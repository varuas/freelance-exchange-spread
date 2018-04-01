package application.exchange.btcmarkets;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import application.NetTickPrice;
import application.configuration.AppConfig;
import application.configuration.ExchangeConfig;
import application.exchange.BaseExchangeConnector;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

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
	public BTCMarketsConnector(AppConfig appConfig, ExchangeConfig exchangeConfig, Scheduler scheduler) {
		super(appConfig, exchangeConfig, scheduler);
		createQueryEventLoop();
	}

	/**
	 * Starts the event loop for fetching BTCMarkets data.
	 * For each currency-pair, one observable-observer pair is created.
	 */
	private void createQueryEventLoop() {

		final Scheduler ioScheduler = getIOScheduler();
		final RateLimiter rateLimiter = getRateLimiter();
		final List<String> currencyPairs = getAppConfig().getCurrencyPairs();

		for (final String currencyPair : currencyPairs) {
			observeCurrencyPair(currencyPair, rateLimiter, ioScheduler);
		}
	}

	/**
	 * Creates the observable-observer pair for the given currency to fetch
	 * market data. A recursive event-loop ensures that the query is done in an
	 * event-driven non-blocking manner.
	 *
	 * The total rate (shared across all currency-pairs) is throttled by the
	 * rate limiter.
	 *
	 * All the observables share the same IO scheduler (i.e. thread pool).
	 *
	 * @param currencyPair
	 *            the currency pair for which market data is required
	 * @param rateLimiter
	 *            the web requests are limited by this rate limiter
	 * @param ioScheduler
	 *            the IO scheduler to use for the event
	 */
	private void observeCurrencyPair(String currencyPair, RateLimiter rateLimiter, Scheduler ioScheduler) {

		Observable.fromCallable(() -> queryTickInfo(currencyPair, rateLimiter))
			.subscribeOn(ioScheduler)
			.onErrorReturn(err -> {
				LOGGER.warn("Failed to get BTCMarkets data for " + currencyPair, err);
				return Optional.empty();
			})
			.doOnNext(this::updateCache)
			.subscribe((data) ->
				observeCurrencyPair(currencyPair, rateLimiter, ioScheduler));
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
	 * @param rateLimiter the rate limiter used to throttle the requests
	 * @return the ticker data (if found)
	 * @throws IOException if an error occurs during the web request
	 */
	private Optional<BTCMarketsTickInfo> queryTickInfo(String ccyPair, RateLimiter rateLimiter) throws IOException {

		final String[] splitCcyPair = ccyPair.split("-");
		final String baseCurrency = splitCcyPair[0];
		final String quoteCurrency = splitCcyPair[1];
		final String url = String.format(TICK_URL_PATTERN, baseCurrency.toUpperCase(), quoteCurrency.toUpperCase());

		// Throttle
		rateLimiter.acquire(1);

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