package application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.configuration.AppConfig;
import application.exchange.BaseExchangeConnector;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

public class SpreadCalculatorTask implements Runnable {

	private static Logger LOGGER = LoggerFactory.getLogger(SpreadCalculatorTask.class);

	private final AppConfig appConfig;

	private final Map<String, BaseExchangeConnector> exchangeConnectors;

	private final Scheduler pooledIOScheduler;

	private final ExecutorService pooledIOExec;

	public SpreadCalculatorTask(AppConfig appConfig) {
		this.appConfig = appConfig;
		this.exchangeConnectors = Utils.createExchangeConnectorInstances(appConfig);
		this.pooledIOExec = Executors.newFixedThreadPool(appConfig.getIoThreads());
		this.pooledIOScheduler = Schedulers.from(pooledIOExec);
	}

	@Override
	public void run() {

		final List<Observable<Optional<SpreadInfo>>> currencyPairEvents = new ArrayList<>();

		//  Iterate for each currency pair that is configured
		for(final String currencyPair : appConfig.getCurrencyPairs()) {

			final String[] splitCurrPair = currencyPair.split("-");
			final String baseCurrency = splitCurrPair[0];
			final String quoteCurrency = splitCurrPair[1];

			// Event type #1 : Query each exchange for the given currency pair
			final List<Observable<Optional<NetTickPrice>>> exchangeQueryEvents =
					getEventsForExchangeQuery(baseCurrency, quoteCurrency);

			// Event type #2 : After all exchanges have responded for the currency pair, calculate the spread
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
					sortedInfos.sort((spread1, spread2) ->
							spread2.getSpreadPercent().compareTo(spread1.getSpreadPercent()));
					return Utils.formatConsolePrint(sortedInfos);
		});

		// There is only one output : the sorted array of spreads, this task waitser till this result is found
		final String output = zippedCurrencyPairEvents.blockingFirst();

		System.out.println(output);
	}

	private Observable<Optional<SpreadInfo>> zipExchangeQueriesAndCalcSpread(final List<Observable<Optional<NetTickPrice>>> exchObservables) {
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

						LOGGER.info(netTickPrice.toString());
					}

					// Ignore invalid permutations
					if(noOfActiveExchanges <= 1 || spread.getBestAskPrice() == null || spread.getBestBidPrice() == null) {
						return Optional.empty();
					}

					final BigDecimal spreadPercent = spread.getBestAskPrice().subtract(spread.getBestBidPrice())
							.divide(spread.getBestBidPrice(), 4, RoundingMode.HALF_UP);
					spread.setSpreadPercent(spreadPercent);

					LOGGER.info(spread.toString());
					return Optional.of(spread);
				});
		return spreadForCcyPair;
	}

	private List<Observable<Optional<NetTickPrice>>> getEventsForExchangeQuery(final String baseCurrency,
			final String quoteCurrency) {
		final List<Observable<Optional<NetTickPrice>>> exchObservables = new ArrayList<>(appConfig.getExchanges().size());
		for (final Entry<String, BaseExchangeConnector> connectorEntry : exchangeConnectors.entrySet()) {
			final BaseExchangeConnector connector = connectorEntry.getValue();
			final Observable<Optional<NetTickPrice>> exchObservable =
					connector.getTickInfo(baseCurrency, quoteCurrency)
						.subscribeOn(pooledIOScheduler)
						.observeOn(Schedulers.computation())
						.onErrorReturn( (t) -> {
							LOGGER.warn("Failed to get tick info", t);
							return Optional.empty();
						});
			exchObservables.add(exchObservable);
		}
		return exchObservables;
	}

	public void shutdown() {
		this.pooledIOScheduler.shutdown();
		this.pooledIOExec.shutdownNow();
	}

}