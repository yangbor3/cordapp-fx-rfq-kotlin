# cordapp-fx-rfq-kotlin

# Business Story & Design
# What Is a Request for Quote (RFQ)?
A request for quote is a standard business process that an organization will use when they want to buy a specific product or service. Typically, a company creates and issues an RFQ, and vendors offer price quotes. 

https://www.smartsheet.com/rfq-process

# Purpose
1)FX RFQ OTC(over the counter) is usually huge amount transaction, the digital assets must be secured. 
2)The business process NewQuote, responde, settle quite match Corda design.
3)Compliance/regulatory requirement from central bank: 
  FX RFQ data must store for long time, nomarly 10 years, 
  avoid front trader using this information for personal benifits.

# How to run?
# build
Go to project root folder, e.g. /Users/boyang/Documents/workspace/cordapp-fx-rfq-kotlin-v0.1
gradlew clean deployNodes

# start the nodes on Mac/Linux run the following command: 
build/nodes/runnodes

# run
flow start NewQuoteFlow$Initiator rfqBank: BankA/BankB, customer: CompanyA, currencyPair: "xxx/yyy", quantity: xxxx, buySell: "xxx"
flow start RespondQuoteFlow$Initiator linearId: "xxxxxxxxxx", price: 98.765
flow start SettleQuoteFlow$Initiator linearId: "xxxxxxxxxx"
e.g.
flow start NewQuoteFlow$Initiator rfqBank: BankA, customer: CompanyA, currencyPair: "AUD/USD", quantity: 126339806, buySell: "BUY"
flow start NewQuoteFlow$Initiator rfqBank: BankB, customer: CompanyA, currencyPair: "EUR/USD", quantity: 120000000, buySell: "BUY"
flow start RespondQuoteFlow$Initiator linearId: "abb22c73-62f0-4d10-8b67-77b1984ff8a7", price: 500.000
flow start SettleQuoteFlow$Initiator linearId: "abb22c73-62f0-4d10-8b67-77b1984ff8a7"

# check
run vaultQuery contractStateType: com.template.states.QuoteState
    
    lookfor linearId

# problems:
1.Intellij IDEA运行报Command line is too long 解法:
修改项目下 .idea\workspace.xml，找到标签 <component name="PropertiesComponent"> ， 在标签里加一行  <property name="dynamic.classpath" value="true" />

2.java.lang.IllegalStateException: Missing the '-javaagent' JVM argument. Make sure you run the tests with the Quasar java agent attached to your JVM.
  See https://docs.corda.net/head/testing.html#running-tests-in-intellij - 'Fiber classes not instrumented' for more details.
  Setup test env : https://docs.corda.net/docs/corda-os/4.4/testing.html#running-tests-in-intellij  

3.** search by id doesn't work, block process:
Sun Apr 26 10:49:30 SGT 2020>>> flow start RespondQuoteFlow$Initiator linearId: "1a95df17-f4fa-4cc8-9ef3-8117f7df68d3", price: 98.765
 -- Searching RFQ with linerID fun2: 1a95df17-f4fa-4cc8-9ef3-8117f7df68d3 --
Starting
Obtaining RFQ from vault.
** resolve : change the ContractState to LinearState

[Pending]
4.set Amount SU, but the QuoteState was not saved into network, when query after settlement, amount is still null  
# Thinking:
1.println only print out in the 4 popup windows but NOT the console
2.processTracker msg will be printed into the 4 popup windows 
  
