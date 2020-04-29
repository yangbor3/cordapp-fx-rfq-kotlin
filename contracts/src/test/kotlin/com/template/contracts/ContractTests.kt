package com.template.contracts

import com.template.states.QuoteState
//import net.corda.core.contracts.BelongsToContract
//import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import net.corda.testing.core.TestIdentity
//import net.corda.testing.node.makeTestIdentityService
import org.junit.Test

class ContractTests {

    private class DummyState : ContractState {
        override val participants: List<AbstractParty> get() = listOf()
    }

    private val ledgerServices = MockServices(
            listOf("com.template.states", "com.template.contracts"));

    private val companyA = TestIdentity(CordaX500Name("companyA","New York","US"))
    private val bankA = TestIdentity(CordaX500Name("bankA", "London", "GB"))

    @Test
    fun `NewQuote transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                output(QuoteContract.ID, QuoteState(companyA.party,bankA.party,"AUD/USD",16339806.00, "BUY", null,null))
                command(listOf(companyA.publicKey, bankA.publicKey), QuoteContract.Commands.New())
                input(QuoteContract.ID, DummyState())
                fails()
            }
            transaction {
                output(QuoteContract.ID, QuoteState(companyA.party,bankA.party,  "AUD/USD", 16339806.00, "BUY", null,null))
                command(listOf(companyA.publicKey, bankA.publicKey), QuoteContract.Commands.New())
                verifies() // As there are no input states.
            }
        }
    }

    @Test
    fun `New transaction must have only one output quote`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(companyA.publicKey, bankA.publicKey), QuoteContract.Commands.New())
                output(QuoteContract.ID, QuoteState(companyA.party,bankA.party,"AUD/USD",16339806.00, "BUY", null,null))
                output(QuoteContract.ID, QuoteState(companyA.party,bankA.party,"AUD/USD",16339806.00, "BUY", null,null))
                fails()
            }
            transaction {
                command(listOf(companyA.publicKey, bankA.publicKey), QuoteContract.Commands.New())
                // One output passes.
                output(QuoteContract.ID, QuoteState(companyA.party,bankA.party,"AUD/USD",16339806.00, "BUY", null,null))
                verifies()
            }
        }
    }
}