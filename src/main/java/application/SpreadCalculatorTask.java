package application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.configuration.AppConfig;
import application.exchange.BaseExchangeConnector;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Executes the logic for calculation of currency-pair spreads.
 *
 * The logic can be broken down into 3 events : </br>
 *
 * 1. Fetches data for each exchange & currency-pair combination </br>
 *
 * 2. Zips all the above to calculate the best spread for a particular currency.
 * This is repeated for all currencies. </br>
 *
 * 3. All the best spreads for the currencies are zipped and then sorted in
 * descending order. </br>
 */
public class SpreadCalculatorTask implements Runnable {

	private static Logger LOGGER = LoggerFactory.getLogger(SpreadCalculatorTask.class);

	/**
	 * Configuration that is stored in 'config.json'
	 */
	private final AppConfig appConfig;

	/**
	 * Each exchange has an associated 'connector' class defined in 'config.json'
	 */
	private final Map<String, BaseExchangeConnector> exchangeConnectors;

	/**
	 * Initializes the connectors for each exchange
	 */
	public SpreadCalculatorTask(AppConfig appConfig) {
		this.appConfig = appConfig;
		this.exchangeConnectors = Utils.createExchangeConnectorInstances(appConfig);
	}

	/**
	 * Calculates the best spread and displays the output every time this method is run.
	 */
	@Override
	public void run() {

		final List<Observable<Optional<SpreadInfo>>> currencyPairEvents = new ArrayList<>();

		//  Iterate for each currency pair that is configured
		for(final String currencyPair : appConfig.getCurrencyPairs()) {

			final String[] splitCurrPair = currencyPair.split("-");
			final String baseCurrency = splitCurrPair[0];
			final String quoteCurrency = splitCurrPair[1];

			// Event type #1 : Fetch data for the exchange + currency-pair combination
			final List<Observable<Optional<NetTickPrice>>> exchangeQueryEvents =
					getEventsForExchangeQuery(baseCurrency, quoteCurrency);

			// Event type #2 : Zips all the above to calculate the best spread for a particular currency.
			final Observable<Optional<SpreadInfo>> currencyPairEvent = zipExchangeQueriesAndCalcSpread(exchangeQueryEvents);
			currencyPairEvents.add(currencyPairEvent);
		}

		// Event type #3 : After all currency-pair spreads have been calculated, this event sorts them and generates output
		final Observable<String> zippedCurrencyPairEvents = Observable.zip(
				currencyPairEvents, (spreadArr) -> {
					final List<SpreadInfo> sortedInfos = new ArrayList<>(spreadArr.length);
					for (final Object element : spreadArr) {
						@SuppressWarnings("unchecked")
						final Optional<SpreadInfo> opt = (Optional<SpreadInfo>) element;
						if(opt.isPresent()) {
							sortedInfos.add(opt.get());
						}
					}
					// Descending order sorting
					sortedInfos.sort((spread1, spread2) ->
							spread2.getSpreadPercent().compareTo(spread1.getSpreadPercent()));
					return Utils.formatConsolePrint(sortedInfos);
		});

		// There is only one output : the sorted array of spreads, this task waits till this result is found
		final String output = zippedCurrencyPairEvents.blockingFirst();

		System.out.println(output);
	}

	/**
	 * Zips all the exchange+currency-pair combinations to calculate the best
	 * spread for a particular currency.
	 */
	private Observable<Optional<SpreadInfo>> zipExchangeQueriesAndCalcSpread(
			List<Observable<Optional<NetTickPrice>>> exchObservables) {

		final Observable<Optional<SpreadInfo>> spreadForCcyPair = Observable
			.zip(exchObservables, (obsArr) -> {

				// calculate spread for each currency pair
				final SpreadInfo spread = new SpreadInfo();

				int noOfActiveExchanges = 0;
				for (final Object obsElement : obsArr) {

					@SuppressWarnings("unchecked")
					final Optional<NetTickPrice> opNetTickPrice = (Optional<NetTickPrice>) obsElement;
					if(!opNetTickPrice.isPresent()) {
						continue;
					}
					noOfActiveExchanges++;
					final NetTickPrice netTickPrice = opNetTickPrice.get();
					if(spread.getBestAskPrice() == null || netTickPrice.getNetAskPrice().compareTo(spread.getBestAskPrice()) > 0) {
						spread.setCcyPair(netTickPrice.getCcyPair());
						spread.setBestAskPrice(netTickPrice.getNetAskPrice());
						spread.setBestAskExchange(netTickPrice.getExchangeId());
					}
					if(spread.getBestBidPrice() == null || netTickPrice.getNetBidPrice().compareTo(spread.getBestBidPrice()) < 0) {
						spread.setCcyPair(netTickPrice.getCcyPair());
						spread.setBestBidPrice(netTickPrice.getNetBidPrice());
						spread.setBestBidExchange(netTickPrice.getExchangeId());
					}

					if(LOGGER.isDebugEnabled()) {
						LOGGER.debug(netTickPrice.toString());
					}
				}

				// Ignore invalid permutations
				if(noOfActiveExchanges <= 1 || spread.getBestAskPrice() == null || spread.getBestBidPrice() == null) {
					return Optional.empty();
				}

				final BigDecimal spreadPercent = spread.getBestAskPrice().subtract(spread.getBestBidPrice())
						.divide(spread.getBestBidPrice(), 4, RoundingMode.HALF_UP);
				spread.setSpreadPercent(spreadPercent);

				if(LOGGER.isDebugEnabled()) {
					LOGGER.debug(spread.toString());
				}

				return Optional.of(spread);
			});
		return spreadForCcyPair;
	}

	/**
	 * Fetches data for the exchange + currency-pair combination
	 */
	private List<Observable<Optional<NetTickPrice>>> getEventsForExchangeQuery(
			final String baseCurrency, final String quoteCurrency) {

		final List<Observable<Optional<NetTickPrice>>> exchObservables = new ArrayList<>(appConfig.getExchanges().size());
		for (final Entry<String, BaseExchangeConnector> connectorEntry : exchangeConnectors.entrySet()) {
			final BaseExchangeConnector connector = connectorEntry.getValue();
			final Observable<Optional<NetTickPrice>> exchObservable =
					connector.getTickInfo(baseCurrency, quoteCurrency)
						.subscribeOn(Schedulers.computation())
						.observeOn(Schedulers.computation())
						.onErrorReturn( (t) -> {
							LOGGER.warn("Failed to get tick info", t);
							return Optional.empty();
						});
			exchObservables.add(exchObservable);
		}
		return exchObservables;
	}
}