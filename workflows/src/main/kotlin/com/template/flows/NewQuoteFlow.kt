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

object NewQuoteFlow {
    // *********
    // * Flows *
    // *********
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
            val rfqBank: Party,
            val customer: Party,
//            val transactionID: String,
//            val time: String,
            val currencyPair: String,
            val quantity: Double,
            val buySell: String
    ) : FlowLogic<SignedTransaction>() {
        // progress tracker is good to show the progressing ???
        override val progressTracker: ProgressTracker = tracker()

        companion object {
            object CREATING : ProgressTracker.Step("-- Creating a new RFQ! --")
            object SIGNING : ProgressTracker.Step("-- Signing the RFQ! --")
            object VERIFYING : ProgressTracker.Step("-- Verifying the RFQ! --")
            object FINALISING : ProgressTracker.Step("-- Sending the RFQ! --") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
        }
        // progress tracker Done

        @Suspendable
        override fun call(): SignedTransaction {
            progressTracker.currentStep = CREATING
            println(" - yb NEW RFQ - ")
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val command = Command(QuoteContract.Commands.New(), listOf(rfqBank, customer).map { it.owningKey })
            val quoteState = QuoteState(
                    rfqBank,
                    customer,
//                    transactionID,
//                    time,
                    currencyPair,
                    quantity,
                    buySell,
                    null,
                    null,
                    UniqueIdentifier()
            )

            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(quoteState, QuoteContract.ID)
                    .addCommand(command)
            println(" - yb VERIFYING - ")
            progressTracker.currentStep = VERIFYING
            txBuilder.verify(serviceHub)
            progressTracker.currentStep = SIGNING
            val tx = serviceHub.signInitialTransaction(txBuilder)
            progressTracker.currentStep = FINALISING

            val sessions = (quoteState.participants - ourIdentity).map { initiateFlow(it as Party) }
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