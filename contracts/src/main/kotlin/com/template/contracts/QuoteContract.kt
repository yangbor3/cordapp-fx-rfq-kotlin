package com.template.contracts

import com.template.states.QuoteState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import java.security.PublicKey

// ************
// * Contract *
// ************
class QuoteContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.QuoteContract"
    }

    interface Commands : CommandData {
        class New : Commands
        class Respond : Commands
        class Settle : Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>().value
        val setOfSigners = tx.commands.requireSingleCommand<Commands>().signers.toSet()
        when (command) {
            is Commands.New -> verifyNewRFQ(tx)
            is Commands.Respond -> verifyRespond(tx, setOfSigners)
            is Commands.Settle -> verifySettle(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    /*
     * 1. No input state before NEW
     * 2. Only one input state
     * 3. Out must by type of QuoteState
     * 4. Currency Pair must be 7 characters long
     * 5. buySell field must be equal to "BUY" or "SELL"
     * TODO: pending check only PartyType == customer can issue new RFQ
     */
    private fun verifyNewRFQ(tx: LedgerTransaction) = requireThat {
        "There should be no input state" using (tx.inputs.isEmpty())
        "There should be one input state" using (tx.outputs.size == 1)
        "The output state must be of type QuoteState" using (tx.outputs.get(0).data is QuoteState)
        val outputState = tx.outputs.get(0).data as QuoteState
        "The currency pair must be seven characters long, e.g. AUD/USD, USD/EUR" using (outputState.currencyPair.length == 7)
        "The buySell must be equal to BUY or SELL" using (outputState.buySell in listOf<String>("BUY", "SELL"))
    }

    /*
     * 1. One respond map to one New QuoteState
     * 2. Only one output QuoteState
     * 3. Response only fill in the price
     * TODO: check the sign
     * TODO: more checking need to be implemented base on FX RFQ Business Nature
     */
    private fun verifyRespond(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "An RFQ response respond should only consume one input state." using (tx.inputs.size == 1)
        "An RFQ response should only create one output state." using (tx.outputs.size == 1)
        val input = tx.inputsOfType<QuoteState>().single()
        val output = tx.outputsOfType<QuoteState>().single()
        "Only the price was filled in" using (input.withoutPrice() == output.withoutPrice())
        "The Price property must change in a respond." using (input.price != output.price)

        // "The borrower, old lender and new lender only must sign an obligation transfer transaction" using (signers == (keysFromParticipants(input) union keysFromParticipants(output)))
    }

    /*
     * 1. One settle map to one QuoteState
     * 2. No output QuoteState
     * 3. Response only fill in the price
     * TODO: check the sign
     * TODO: more checking need to be implemented base on FX RFQ Business Nature
     */
    private fun verifySettle(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Check for the presence of an input obligation state.
        val inputs = tx.inputsOfType<QuoteState>()
        "There must be one input QuoteState." using (inputs.size == 1)

        val outputs = tx.outputsOfType<QuoteState>()

        // Check that the cash is being assigned to us.
        val input = inputs.single()

        // "Both lender and borrower together only must sign obligation settle transaction." using (signers == keysFromParticipants(input))
    }
}