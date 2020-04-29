package com.template.states

import com.template.contracts.QuoteContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(QuoteContract::class)
data class QuoteState(
        val rfqBank: Party,
        val customer: Party,
//        val transactionID: String,
//        val time: String,
        val currencyPair: String,
        val quantity: Double,
        val buySell: String,
        val price: Double?,     //"?" means price can be null
        val paidAmount: Double?,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {
    fun withNewPrice(newPrice: Double) = copy(price = newPrice)
    fun withNewAmount(newAmount: Double) = copy(paidAmount = newAmount)
    fun withoutPrice() = copy(price = null)
    override val participants: List<AbstractParty> = listOf(rfqBank, customer)
}

