package application.exchange.acx;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;

import application.NetTickPrice;
import application.configuration.AppConfig;
import application.configuration.ExchangeConfig;
import application.exchange.BaseExchangeConnector;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

/**
 * Connector class responsible for the ACX Exchange. Fetches live market data
 * and updates the cache. Observes periodic requests for spread data and
 * retrieves values from cache.
 */
public class ACXConnector extends BaseExchangeConnector {

	/**
	 * This URL gives all currency-pair tickers in a single API call.
	 */
	private final String ALL_TICKERS_URL = "https://acx.io/api/v2/tickers.json";

	/**
	 * The logger instance for this class
	 */
	private static Logger LOGGER = LoggerFactory.getLogger(ACXConnector.class);

	/**
	 * Initializes this connector during program startup and fires up the event
	 * loop for fetching market data.
	 */
	public ACXConnector(AppConfig appConfig, ExchangeConfig exchangeConfig, Scheduler scheduler) {
		super(appConfig, exchangeConfig, scheduler);
		createQueryEventLoop();
	}

	/**
	 * Starts the event loop for fetching ACX market data.
	 */
	private void createQueryEventLoop() {

		final Scheduler ioScheduler = getIOScheduler();
		final RateLimiter rateLimiter = getRateLimiter();

		observeACXMarket(rateLimiter, ioScheduler);
	}

	/**
	 * Creates the observable-observer pair for fetching the market data for the
	 * currency-pairs in ACX exchange. A recursive event-loop ensures that the
	 * query is done in an event-driven non-blocking manner.
	 *
	 * The request rate is throttled by the rate limiter.
	 *
	 * @param rateLimiter
	 *            the web requests are limited by this rate limiter
	 * @param ioScheduler
	 *            the IO scheduler to use for the event
	 */
	private void observeACXMarket(RateLimiter rateLimiter, Scheduler ioScheduler) {

		Observable.fromCallable(() -> fetchAllCurrencies(rateLimiter)).subscribeOn(ioScheduler).onErrorReturn(err -> {
			LOGGER.warn("Failed to fetch ACX market data", err);
			return ImmutableMap.of();
		}).doOnNext(this::updateCache).subscribe((data) -> observeACXMarket(rateLimiter, ioScheduler));
	}

	/**
	 * Updates the ticker cache for all the tickers present in the given
	 * parameter.
	 */
	private void updateCache(Map<String, ACXTickInfo> tickInfo) {
		tickInfo.forEach((k, v) -> {
			final BigDecimal fee = getExchangeConfig().getFee();
			final BigDecimal netAskPrice = v.getTicker().getSell().multiply(fee);
			final BigDecimal netBidPrice = v.getTicker().getBuy().multiply(fee);
			final String ccyPair = v.getBaseCurrency().toUpperCase() + "-" + v.getQuoteCurrency().toUpperCase();
			final NetTickPrice priceInfo = new NetTickPrice(getExchangeConfig().getId(), ccyPair, netAskPrice, netBidPrice);
			getTickCache().put(ccyPair, priceInfo);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Updating cache : " + priceInfo);
			}
		});
	}

	/**
	 * In ACX exchange, there exists an API to fetch all tickers using a single
	 * API call.
	 *
	 * @return the map having key as currency-pair id and the value as the
	 *         ticker info for that pair.
	 * @throws IOException
	 *             if any error occurs during the web request
	 */
	private Map<String, ACXTickInfo> fetchAllCurrencies(RateLimiter rateLimiter) throws IOException {

		// Throttle web requests
		rateLimiter.acquire(1);

		final Map<String, ACXTickInfo> allCurrencyInfo = getJson(ALL_TICKERS_URL,
				new TypeReference<HashMap<String, ACXTickInfo>>() {});
		return allCurrencyInfo;
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
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[ACX] Fetched from cache : " + tickData);
			}
			return Optional.ofNullable(tickData);
		});
	}

}