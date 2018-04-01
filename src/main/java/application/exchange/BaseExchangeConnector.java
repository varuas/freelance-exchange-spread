package application.exchange;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;

import application.NetTickPrice;
import application.configuration.AppConfig;
import application.configuration.ExchangeConfig;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Base class for the connectors that are responsible for fetching market data
 * from the exchanges.
 */
public abstract class BaseExchangeConnector {

	/**
	 * The logger instance for this class
	 */
	private static Logger LOGGER = LoggerFactory.getLogger(BaseExchangeConnector.class);

	/**
	 * The application JSON configuration
	 */
	private final AppConfig appConfig;

	/**
	 * The exchange JSON configuration
	 */
	private final ExchangeConfig exchangeConfig;

	/**
	 * The client used for the HTTP requests
	 */
	private final OkHttpClient client;

	/**
	 * JSON parser
	 */
	private final ObjectMapper objectMapper;

	/**
	 * Throttles the web requests
	 */
	private final RateLimiter rateLimiter;

	/**
	 * Cache of the tick prices for each currency pair
	 */
	private final Map<String, NetTickPrice> tickCache;

	/**
	 * The scheduler obtained from the thread pool configured in JSON
	 */
	private final Scheduler ioScheduler;

	/**
	 * Initializes the connector
	 */
	public BaseExchangeConnector(AppConfig appConfig, ExchangeConfig exchangeConfig, Scheduler scheduler) {
		this.exchangeConfig = exchangeConfig;
		this.appConfig = appConfig;
		this.ioScheduler = scheduler;
		this.client = new OkHttpClient();
		this.objectMapper = new ObjectMapper();
		this.tickCache = new ConcurrentHashMap<>();
		this.rateLimiter = RateLimiter.create(exchangeConfig.getPollingLimit(), 1,  TimeUnit.SECONDS);
	}

	/**
	 * Obtains the tick information (bid and ask sides) for the given currency pair.
	 */
	public abstract Observable<Optional<NetTickPrice>> getTickInfo(String baseCurrency, String quoteCurrency);

	/**
	 * Initiates a web request to the server and returns the response received
	 * from it.
	 *
	 * @param url the URL of the server API
	 * @return the response received from the server
	 * @throws IOException if the web request fails
	 */
	private Response getResponse(final String url) throws IOException {
		try {
			final Response response = client.newCall(
				new Request.Builder().url(url).build()).execute();
			if(response.code() != 200) {
				throw new IOException(String.format(
						"Invalid response code : %d from : {%s} ",
						response.code(), url));
			}
			LOGGER.info("Got response from : " + url + " = " + response.code());
			return response;
		} catch (final Exception e) {
			throw new IOException("Failed to get tick info from : " + url, e);
		}
	}

	/**
	 * Get the exchange configuration
	 */
	protected ExchangeConfig getExchangeConfig() {
		return exchangeConfig;
	}

	/**
	 * Initiates a web request to the given URL and parses the response to the
	 * given class.
	 */
	protected <T> T getJson(String url, Class<T> clazz) throws IOException {
		final Response response = getResponse(url);
		ResponseBody body = null;
		try {
			body = response.body();
			final String responseTxt = body.string();
			final T parsedObj = objectMapper.readValue(responseTxt, clazz);
			return parsedObj;
		} finally {
			if(body != null) {
				body.close();
			}
		}
	}

	/**
	 * Initiates a web request to the given URL and parses the response to the
	 * given type reference.
	 */
	protected <T> T getJson(String url, TypeReference<T> typeRef) throws IOException {
		final Response response = getResponse(url);
		ResponseBody body = null;
		try {
			body = response.body();
			final String responseTxt = body.string();
			final T parsedObj = objectMapper.readValue(responseTxt, typeRef);
			return parsedObj;
		} finally {
			if(body != null) {
				body.close();
			}
		}
	}

	/**
	 * Returns the application configuration
	 */
	public AppConfig getAppConfig() {
		return appConfig;
	}

	/**
	 * Returns the rate limiter for throttling web requests
	 */
	public RateLimiter getRateLimiter() {
		return rateLimiter;
	}

	/**
	 * Returns the cache storing the tick prices (ask & bid)
	 */
	public Map<String, NetTickPrice> getTickCache() {
		return tickCache;
	}

	/**
	 * Returns the scheduler for IO operations (web requests)
	 */
	public Scheduler getIOScheduler() {
		return ioScheduler;
	}

}