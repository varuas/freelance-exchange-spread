package application;

import java.math.BigDecimal;

/**
 * POJO for storing the top level bid & ask price for a particular currency-pair
 * for a particular exchange. The prices for bid and ask are adjusted with the
 * fees for that exchange.
 */
public class NetTickPrice {

	private final String exchangeId;
	private final String ccyPair;
	private final BigDecimal netAskPrice;
	private final BigDecimal netBidPrice;

	public NetTickPrice(String exchangeId, String ccyPair, BigDecimal netAskPrice, BigDecimal netBidPrice) {
		super();
		this.exchangeId = exchangeId;
		this.ccyPair = ccyPair;
		this.netAskPrice = netAskPrice;
		this.netBidPrice = netBidPrice;
	}

	public BigDecimal getNetAskPrice() {
		return netAskPrice;
	}
	public BigDecimal getNetBidPrice() {
		return netBidPrice;
	}
	public String getCcyPair() {
		return ccyPair;
	}
	public String getExchangeId() {
		return exchangeId;
	}

	@Override
	public String toString() {
		return "NetTickPrice [exchangeId=" + exchangeId + ", ccyPair=" + ccyPair + ", netAskPrice=" + netAskPrice
				+ ", netBidPrice=" + netBidPrice + "]";
	}
}
