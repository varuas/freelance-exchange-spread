# Exchange Spread Calculator

### About

This application calculates the differences in the bid & ask price for different currency pairs across different exchanges.

The sorted output of this 'spread' is displayed on the console at periodic intervals.

```
Time : 2018-03-31T18:00:58.563
--------------------------------------------------------------------------------
  Sr Spread% Currency         Ask       Ask-Exch         Bid       Bid-Exch
--------------------------------------------------------------------------------
   1  0.1158  BCH-AUD    943.4601            ACX    845.5190     BTCMarkets
   2  0.0548  ETH-AUD    520.5717            ACX    493.5440     BTCMarkets
   3  0.0501  BTC-AUD   9176.2803            ACX   8738.0715     BTCMarkets
```

### Program Execution
Launch class : application.ApplicationRunner.main(String[])

### Configuration : File *config.json*
```
{
	"currencyPairs" : ["BTC-AUD", "ETH-AUD", "BCH-AUD"],
	"refreshInterval" : 5000,
	"exchanges" : [
		{
			"id" : "BTCMarkets",
			"fee" : "0.95",
			"pollingLimit" : 2.5,
			"ioThreads" : 4,
			"connectorClass" : "application.exchange.btcmarkets.BTCMarketsConnector"
		},
		{
			"id" : "ACX",
			"fee" : "0.99",
			"pollingLimit" : 2,
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

### Features

#### 1. Scalability
 - Additional currency pairs : Easy to add more currency pairs. It simply involves configuring them in the JSON configuration file (property 'currencyPairs').
 - The number of threads to use for polling is configurable for each exchange separately (property 'ioThreads').

#### 2. Design
 - Event driven reactive approach has been used to design the application.
 - Polling limit : it is configurable for each exchange (property 'pollingLimit'). The IO requests are throttled at this rate.
 - Resilient to IO failures : the failure of one web request won't affect spread calculation for the remaining currency pairs.
 - Responsive : the spread is always displayed at 5 second intervals. If any server response takes too much time, the last available cached data is used to display the results.

#### 3. Good code quality
 - The code is easy to maintain and all configurations are externalized in a structured JSON format.
 - Modular approach to the addition of more exchanges (i.e. apart from ACX and BTCMarkets) - no modification of existing code needs to be done.
 - Exchange specific optimizations are done (e.g. for ACX, a single GET request polls the order book for all currency-pairs)

### ToDos
 - [Optimization] Only poll those ccy pairs which are active on the BTC market
 - [Optimization] Alternative approach of BTC markets connector using semaphores