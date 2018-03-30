# Exchange Spread Calculator

### Sample output
```
Spread print time : 2018-03-29T00:37:45.454
--------------------------------------------------------------------------------
  Sr Spread% Currency         Ask       Ask-Exch         Bid       Bid-Exch
--------------------------------------------------------------------------------
   1  0.1975  BCH-AUD   1274.1300            ACX   1064.0000     BTCMarkets
   2  0.0606  ETH-AUD    587.9016            ACX    554.3345     BTCMarkets
   3  0.0573  BTC-AUD  10320.9183            ACX   9761.2500     BTCMarkets

```

### Program Execution
Launch class : application.ApplicationRunner.main(String[])

### Configuration : File *config.json*
```
{
	"currencyPairs" : ["BTC-AUD", "ETH-AUD", "BCH-AUD"],
	"refreshInterval" : 5000,
	"ioThreads" : 20,
	"exchanges" : [
		{
			"id" : "BTCMarkets",
			"fee" : "0.95",
			"connectorClass" : "application.exchange.btcmarkets.BTCMarketsConnector"
		},
		{
			"id" : "ACX",
			"fee" : "0.99",
			"connectorClass" : "application.exchange.acx.ACXConnector"
		}
	]
}
```

### Approach
Observable - observer pattern has been used.
There are three layers :
 - **Event layer #1** : Query each exchange for the all currency pairs
 - **Event layer #2** : After all exchanges have responded for any currency pair, calculate the spread for that currency pair
 -  **Event layer #3** : After all currency-pair spreads have been calculated, sort all of them and generate the output

 ___(Logic implemented in the class : application.SpreadCalculatorTask)___

### ToDos
 - [Optimization] Only poll those ccy pairs which are active on the BTC market
 - [Optimization] Alternative approach of BTC markets connector using semaphores