package com.template

import com.template.flows.*
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

//Must right click here and run the debug / run the test case !!
class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(NewQuoteFlow.Responder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    //The rfq flow should not have any input
    //This test will check if the input list is empty

    //    val rfqBank: Party,
    //    val customer: Party,
    //    val transactionID: String,
    //    val time: String,
    //    val currencyPair: String,
    //    val quantity: String,
    //    val buySell: String
    @Test
    fun `dummy test_001`() {
        println(" -------------- dummy test_001 -------------- ")
        val future = a.startFlow(NewQuoteFlow.Initiator(b.info.legalIdentities.first(),a.info.legalIdentities.first(),
                "USD/CNH",182693450.00,"SELL"))
        network.runNetwork()
        val ptx = future.get()
        println(" -------------- check ptx input empty -------------- ")
        assert(ptx.tx.inputs.isEmpty())
        println(" -------------- Done -------------- ")
    }
}