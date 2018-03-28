package application.exchange.acx;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import application.NetTickPrice;
import application.configuration.ExchangeConfig;
import application.exchange.BaseExchangeConnector;

public class ACXConnector extends BaseExchangeConnector {

	private final String TICK_URL_PATTERN = "https://acx.io/api/v2/order_book.json?market=%s%s&asks_limit=1&bids_limit=1";

	public ACXConnector(ExchangeConfig exchangeConfig) {
		super(exchangeConfig);
	}

	@Override
	protected Optional<NetTickPrice> fetchNetPrices(String baseCurrency, String quoteCurrency) throws IOException {

		final String url = String.format(TICK_URL_PATTERN, baseCurrency.toLowerCase(), quoteCurrency.toLowerCase());
		final ACXTickInfo acxTickInfo = getJson(url, ACXTickInfo.class);

		final BigDecimal fee = getExchangeConfig().getFee();

		final BigDecimal netAskPrice = acxTickInfo.getAsks().get(0).getPrice().multiply(fee);
		final BigDecimal netBidPrice = acxTickInfo.getBids().get(0).getPrice().multiply(fee);

		final String exchangeId = getExchangeConfig().getId();
		final String ccyPair = baseCurrency + "-" + quoteCurrency;
		final NetTickPrice priceInfo = new NetTickPrice(exchangeId, ccyPair, netAskPrice, netBidPrice);
		return Optional.of(priceInfo);
	}

}