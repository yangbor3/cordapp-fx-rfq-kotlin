package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.QuoteContract
import com.template.states.QuoteState
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.contracts.Amount
import java.util.*
object SettleQuoteFlow {
// *********
// * Flows *
// *********

    @StartableByRPC
    @InitiatingFlow
    class Initiator(
            private val linearId: UniqueIdentifier
    ) : FlowLogic<SignedTransaction>() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object GET_RESPONSE : Step("Obtaining Respond RFQ from vault.")

            object BUILD_SETTLEMENT : Step("Building and verifying settlement.")
            object SIGN_SETTLEMENT : Step("Signing settlement.")
            object VERIFYING : ProgressTracker.Step("-- Verifying the settlement! --")
            object FINALISING : ProgressTracker.Step("-- Sending the settlement! --") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(GET_RESPONSE, BUILD_SETTLEMENT, SIGN_SETTLEMENT, VERIFYING, FINALISING)
        }

        fun getRFQByLinearId(linearId: UniqueIdentifier): StateAndRef<QuoteState> {
            println(" -- Searching RFQ with linerID: $linearId -- ")
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                    null,
                    listOf(linearId),
                    Vault.StateStatus.UNCONSUMED, null)
            println(" -- queryCriteria: $queryCriteria -- ")
            return serviceHub.vaultService.queryBy<QuoteState>(queryCriteria).states.singleOrNull()
                    ?: throw FlowException("RFQ with id $linearId not found.")
        }

        @Suspendable
        override fun call(): SignedTransaction {
            // Stage 1. Retrieve specified by linearId from the vault.
            progressTracker.currentStep = GET_RESPONSE
            val rfqPendingSettle = getRFQByLinearId(linearId)
            println(" -- find by linearId, response: $rfqPendingSettle")
            val inputRFQ = rfqPendingSettle.state.data

            // Stage 2. Create the new quote state reflecting amount.
            progressTracker.currentStep = BUILD_SETTLEMENT
            //TODO:calculate amount from price x quantity

            var paidAmount = inputRFQ.price!!.times(inputRFQ.quantity)
            val outputRFQ = inputRFQ.withNewAmount(paidAmount)

            println(" -- set the amount ${outputRFQ.paidAmount} --")


            // Stage 3. Create the settle command.
            val command = Command(QuoteContract.Commands.Settle(), listOf(outputRFQ.rfqBank, outputRFQ.customer).map { it.owningKey })

            // Stage 4. Create a transaction builder, add the states and commands, and verify the output.
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(rfqPendingSettle)
                    .addOutputState(outputRFQ, QuoteContract.ID)
                    .addCommand(command)
            println(" - yb VERIFYING SETTLEMENT - ")
            progressTracker.currentStep = VERIFYING
            txBuilder.verify(serviceHub)
            progressTracker.currentStep = SIGN_SETTLEMENT
            val tx = serviceHub.signInitialTransaction(txBuilder)
            progressTracker.currentStep = FINALISING

            val sessions = (outputRFQ.participants - ourIdentity).map { initiateFlow(it as Party) }
            val stx = subFlow(CollectSignaturesFlow(tx, sessions))
            //return subFlow(FinalityFlow(stx, sessions))
            return subFlow(FinalityFlow(stx, sessions, FINALISING.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs?.single().data
                    "The output must be a QuoteState" using (output is QuoteState)
                }
            }
            val txWeJustSignedId = subFlow(signedTransactionFlow)
            return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
        }
    }
}
