package application.exchange;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import application.NetTickPrice;
import application.configuration.ExchangeConfig;
import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public abstract class BaseExchangeConnector {

	private static Logger LOGGER = LoggerFactory.getLogger(BaseExchangeConnector.class);

	private final ExchangeConfig exchangeConfig;

	private final OkHttpClient client;

	private final ObjectMapper objectMapper;

	public BaseExchangeConnector(ExchangeConfig exchangeConfig) {
		this.exchangeConfig = exchangeConfig;
		this.client = new OkHttpClient();
		this.objectMapper = new ObjectMapper();
	}

	public Observable<Optional<NetTickPrice>> getTickInfo(String baseCurrency, String quoteCurrency) {
		return Observable.fromCallable(() -> fetchNetPrices(baseCurrency, quoteCurrency));
	}

	protected abstract Optional<NetTickPrice> fetchNetPrices(String baseCurrency, String quoteCurrency) throws IOException;

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

}