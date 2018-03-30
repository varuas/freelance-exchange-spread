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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Base class for the connectors that are responsible for fetching market data
 * from the exchanges.
 */
public abstract class BaseExchangeConnector {

	private static Logger LOGGER = LoggerFactory.getLogger(BaseExchangeConnector.class);

	private final AppConfig appConfig;

	private final ExchangeConfig exchangeConfig;

	private final OkHttpClient client;

	private final ObjectMapper objectMapper;

	private final RateLimiter rateLimiter;

	private final Map<String, NetTickPrice> tickCache;

	public BaseExchangeConnector(AppConfig appConfig, ExchangeConfig exchangeConfig) {
		this.exchangeConfig = exchangeConfig;
		this.appConfig = appConfig;
		this.client = new OkHttpClient();
		this.objectMapper = new ObjectMapper();
		this.tickCache = new ConcurrentHashMap<>();
		this.rateLimiter = RateLimiter.create(exchangeConfig.getPollingLimit(), 1,  TimeUnit.SECONDS);
	}

	public abstract Observable<Optional<NetTickPrice>> getTickInfo(String baseCurrency, String quoteCurrency);

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

	protected ExchangeConfig getExchangeConfig() {
		return exchangeConfig;
	}

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

	public AppConfig getAppConfig() {
		return appConfig;
	}

	public RateLimiter getRateLimiter() {
		return rateLimiter;
	}

	public Map<String, NetTickPrice> getTickCache() {
		return tickCache;
	}

}