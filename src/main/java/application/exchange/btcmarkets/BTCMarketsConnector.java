package application.exchange.btcmarkets;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import application.NetTickPrice;
import application.configuration.ExchangeConfig;
import application.exchange.BaseExchangeConnector;

public class BTCMarketsConnector extends BaseExchangeConnector {

	private static final String TICK_URL_PATTERN = "https://api.btcmarkets.net/market/%s/%s/tick";

	public BTCMarketsConnector(ExchangeConfig exchangeConfig) {
		super(exchangeConfig);
	}

	@Override
	protected Optional<NetTickPrice> fetchNetPrices(String baseCurrency, String quoteCurrency) throws IOException {

		final String url = String.format(TICK_URL_PATTERN, baseCurrency.toUpperCase(), quoteCurrency.toUpperCase());
		final BTCMarketsTickInfo btcTickInfo = getJson(url, BTCMarketsTickInfo.class);

		final BigDecimal fee = getExchangeConfig().getFee();

		final BigDecimal netAskPrice = btcTickInfo.getBestAsk().multiply(fee);
		final BigDecimal netBidPrice = btcTickInfo.getBestBid().multiply(fee);

		final String exchangeId = getExchangeConfig().getId();
		final String ccyPair = baseCurrency + "-" + quoteCurrency;
		final NetTickPrice priceInfo = new NetTickPrice(exchangeId, ccyPair, netAskPrice, netBidPrice);
		return Optional.of(priceInfo);
	}
}