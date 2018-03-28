package application;

import java.math.BigDecimal;

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
