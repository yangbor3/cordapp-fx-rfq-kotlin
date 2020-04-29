# **cordapp-fx-rfq-kotlin**

# What is a Request for Quote (RFQ)?
A request for quote is a standard business process that an organization will use when they want to buy a specific product or service. Typically, a company creates and issues an RFQ, and vendors offer price quotes. 

https://www.smartsheet.com/rfq-process

# Purpose
1. FX RFQ OTC(over the counter) is usually huge amount transaction, the digital assets must be secured. 
2. The business process NewQuote, responde, settle quite match Corda design.
3. Compliance/regulatory requirement from central bank: 1) FX RFQ data must store for long time, nomarly 10 years; 2) Avoid front trader using this information for personal benifits.


# How to run?
# build
1. Go to project root folder, e.g. /Users/boyang/Documents/workspace/cordapp-fx-rfq-kotlin-v0.1
2. gradlew clean deployNodes

# start the nodes on Mac/Linux run the following command: 
build/nodes/runnodes

# run
1. flow start NewQuoteFlow$Initiator rfqBank: BankA/BankB, customer: CompanyA, currencyPair: "xxx/yyy", quantity: xxxx, buySell: "xxx"
2. flow start RespondQuoteFlow$Initiator linearId: "xxxxxxxxxx", price: 98.765
3. flow start SettleQuoteFlow$Initiator linearId: "xxxxxxxxxx"
#e.g.
1. flow start NewQuoteFlow$Initiator rfqBank: JPMorgan, customer: Apple, currencyPair: "AUD/USD", quantity: 126339806, buySell: "BUY"
2. flow start RespondQuoteFlow$Initiator linearId: "eea66c0e-a19d-4ef5-80b4-230ecbc629a0", price: 500.000
3. flow start SettleQuoteFlow$Initiator linearId: "eea66c0e-a19d-4ef5-80b4-230ecbc629a0"

# check
run vaultQuery contractStateType: com.template.states.QuoteState
    
# problems:
1.Intellij IDEA error "Command line is too long":

change file .idea\workspace.xml，find <component name="PropertiesComponent"> ， add one more line  <property name="dynamic.classpath" value="true" />

2.java.lang.IllegalStateException: Missing the '-javaagent' JVM argument. Make sure you run the tests with the Quasar java agent attached to your JVM.

  See https://docs.corda.net/head/testing.html#running-tests-in-intellij - 'Fiber classes not instrumented' for more details.
  Setup test env : https://docs.corda.net/docs/corda-os/4.4/testing.html#running-tests-in-intellij  

3.** search by id doesn't work, block process:

Sun Apr 26 10:49:30 SGT 2020>>> flow start RespondQuoteFlow$Initiator linearId: "1a95df17-f4fa-4cc8-9ef3-8117f7df68d3", price: 98.765
 -- Searching RFQ with linerID fun2: 1a95df17-f4fa-4cc8-9ef3-8117f7df68d3 --
Starting
Obtaining RFQ from vault.
** resolve : change the ContractState to LinearState

  
