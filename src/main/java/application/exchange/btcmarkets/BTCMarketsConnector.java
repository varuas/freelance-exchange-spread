package application.exchange.btcmarkets;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import application.NetTickPrice;
import application.configuration.AppConfig;
import application.configuration.ExchangeConfig;
import application.exchange.BaseExchangeConnector;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

/**
 * Connector class responsible for the BTCMarkets Exchange.
 * Fetches live market data and updates the cache.
 * Observes periodic requests for spread data and retrieves values from cache.
 */
public class BTCMarketsConnector extends BaseExchangeConnector {

	/**
	 * This URL returns only the ticker for a particular currency-pair
	 */
	private static final String TICK_URL_PATTERN = "https://api.btcmarkets.net/market/%s/%s/tick";

	private static Logger LOGGER = LoggerFactory.getLogger(BTCMarketsConnector.class);

	/**
	 * Initializes this connector during program startup and fires up the event
	 * loop for fetching market data.
	 */
	public BTCMarketsConnector(AppConfig appConfig, ExchangeConfig exchangeConfig) {
		super(appConfig, exchangeConfig);
		createQueryEventLoop();
	}

	/**
	 * Starts the event loop for fetching BTCMarkets data
	 */
	private void createQueryEventLoop() {

		final Integer ioThreads = getExchangeConfig().getIoThreads();
		final ExecutorService ioExecutor = Executors.newFixedThreadPool(ioThreads);
		final Scheduler ioScheduler = Schedulers.from(ioExecutor);

		final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
		final Scheduler singleScheduler = Schedulers.from(singleExecutor);

		Observable.interval(1, TimeUnit.MILLISECONDS, singleScheduler)
			.doOnNext(t -> LOGGER.info("New tick started for BTC : " + t))
			.subscribe((t) -> {

				final List<String> ccyPairs = getAppConfig().getCurrencyPairs();
				final List<Observable<Optional<BTCMarketsTickInfo>>> urlReqs =
					ccyPairs
						.stream()
						.map(ccyPair ->
							Observable.fromCallable(() -> queryTickInfo(ccyPair))
							.subscribeOn(ioScheduler)
							.onErrorReturnItem(Optional.empty()))
						.collect(Collectors.toList());

				Observable.merge(urlReqs)
					.subscribeOn(Schedulers.computation())
					.doOnNext(this::updateCache)
					.blockingSubscribe();
			});
	}

	/**
	 * Updates the ticker cache for a particular currency-pair (if present).
	 */
	private void updateCache(Optional<BTCMarketsTickInfo> optTickInfo) {

		if(!optTickInfo.isPresent()) {
			return;
		}

		final BTCMarketsTickInfo btcTickInfo = optTickInfo.get();

		final BigDecimal fee = getExchangeConfig().getFee();

		final BigDecimal netAskPrice = btcTickInfo.getBestAsk().multiply(fee);
		final BigDecimal netBidPrice = btcTickInfo.getBestBid().multiply(fee);

		final String exchangeId = getExchangeConfig().getId();

		final String ccyPair = btcTickInfo.getInstrument() + "-" + btcTickInfo.getCurrency();
		final NetTickPrice priceInfo = new NetTickPrice(exchangeId, ccyPair, netAskPrice, netBidPrice);

		getTickCache().put(ccyPair, priceInfo);
	}

	/**
	 * Initiates a web request to fetch the ticker data for the given currency pair.
	 *
	 * @param ccyPair the currency pair in <base_currency>-<quote_currency> format
	 * @return the ticker data (if found)
	 * @throws IOException if an error occurs during the web request
	 */
	private Optional<BTCMarketsTickInfo> queryTickInfo(String ccyPair) throws IOException {

		final String[] splitCcyPair = ccyPair.split("-");
		final String baseCurrency = splitCcyPair[0];
		final String quoteCurrency = splitCcyPair[1];
		final String url = String.format(TICK_URL_PATTERN, baseCurrency.toUpperCase(), quoteCurrency.toUpperCase());

		// Throttle
		getRateLimiter().acquire(1);

		final BTCMarketsTickInfo btcTickInfo = getJson(url, BTCMarketsTickInfo.class);

		if(btcTickInfo.getSuccess() != null && btcTickInfo.getSuccess() == false) {
			throw new IOException("Failed to get tick info for BTCMarkets using url : " + url);
		}

		return Optional.of(btcTickInfo);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Retrieves the tick data from the cache.
	 */
	@Override
	public Observable<Optional<NetTickPrice>> getTickInfo(String baseCurrency, String quoteCurrency) {
		return Observable.fromCallable(() -> {
			final NetTickPrice tickData = getTickCache().get(baseCurrency + "-" + quoteCurrency);
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("[BTC] Fetched from cache : " + tickData);
			}
			return Optional.ofNullable(tickData);
		});
	}

}