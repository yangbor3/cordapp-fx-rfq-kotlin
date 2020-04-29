package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.QuoteContract
import com.template.states.QuoteState
//import net.corda.confidential.IdentitySyncFlow
//import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker.Step
//import com.google.common.collect.ImmutableList
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

object RespondQuoteFlow {
// *********
// * Flows *
// *********

    @StartableByRPC
    @InitiatingFlow
    class Initiator(
            private val linearId: UniqueIdentifier,
            val price: Double
    ) : FlowLogic<SignedTransaction>() {

        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object GET_RFQ : Step("Obtaining RFQ from vault.")

            object BUILD_RESPONSE : Step("Building and verifying response.")
            object SIGN_RESPONSE : Step("Signing response.")
            object VERIFYING : ProgressTracker.Step("-- Verifying the response! --")
            object FINALISING : ProgressTracker.Step("-- Sending the response! --") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(GET_RFQ, BUILD_RESPONSE, SIGN_RESPONSE, VERIFYING, FINALISING)
        }

        fun getRFQByLinearId2(linearId: UniqueIdentifier): StateAndRef<QuoteState> {
            println(" -- Searching RFQ with linerID fun2: $linearId -- ")
            // Retrieving the input from the vault.
            val inputCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
            val inputStateAndRef = serviceHub.vaultService.queryBy<QuoteState>(inputCriteria).states.single()
            //val input = inputStateAndRef.state.data
            return inputStateAndRef
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
            // Stage 1. Retrieve obligation with the correct linear ID from the vault.
            progressTracker.currentStep = GET_RFQ
            //val rfqPendingRespond = getRFQByLinearId(linearId)
            //println(" -- find by linearId, response: $rfqPendingRespond")
            // here here here
//        val inputRFQ = rfqPendingRespond.state.data
            val rfqPendingRespond = getRFQByLinearId2(linearId)
            val inputRFQ = rfqPendingRespond.state.data
            println(" -- !find by linearId, inputRFQ: $inputRFQ")
            // Stage 2. Create the new quote state reflecting response price.
            progressTracker.currentStep = BUILD_RESPONSE
            println(" -- !set the new price $price --")
            val outputRFQ = inputRFQ.withNewPrice(price)

            // Stage 3. Create the transfer command.
            val command = Command(QuoteContract.Commands.Respond(), listOf(outputRFQ.rfqBank, outputRFQ.customer).map { it.owningKey })

            // Stage 4. Create a transaction builder, add the states and commands, and verify the output.
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(rfqPendingRespond)
                    .addOutputState(outputRFQ, QuoteContract.ID)
                    .addCommand(command)
            println(" - yb VERIFYING RESPOND - ")
            progressTracker.currentStep = VERIFYING
            txBuilder.verify(serviceHub)
            progressTracker.currentStep = SIGN_RESPONSE
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
                    val output = stx.tx.outputs.single().data
                    "The output must be a QuoteState" using (output is QuoteState)
                }
            }

            val txWeJustSignedId = subFlow(signedTransactionFlow)
            return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSignedId.id))
        }
    }
}