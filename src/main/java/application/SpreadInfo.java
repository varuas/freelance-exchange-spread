package application;

import java.math.BigDecimal;

/**
 * POJO for storing the spread along with best ask & bid price for a particular
 * currency pair.
 */
public class SpreadInfo {

	private String ccyPair;
	private String bestAskExchange;
	private String bestBidExchange;
	private BigDecimal bestAskPrice;
	private BigDecimal bestBidPrice;
	private BigDecimal spreadPercent;

	public BigDecimal getSpreadPercent() {
		return spreadPercent;
	}

	public void setSpreadPercent(BigDecimal spreadPercent) {
		this.spreadPercent = spreadPercent;
	}

	public String getCcyPair() {
		return ccyPair;
	}

	public void setCcyPair(String ccyPair) {
		this.ccyPair = ccyPair;
	}

	public String getBestAskExchange() {
		return bestAskExchange;
	}

	public String getBestBidExchange() {
		return bestBidExchange;
	}

	public BigDecimal getBestAskPrice() {
		return bestAskPrice;
	}

	public BigDecimal getBestBidPrice() {
		return bestBidPrice;
	}

	public void setBestAskExchange(String bestAskExchange) {
		this.bestAskExchange = bestAskExchange;
	}

	public void setBestBidExchange(String bestBidExchange) {
		this.bestBidExchange = bestBidExchange;
	}

	public void setBestAskPrice(BigDecimal bestAskPrice) {
		this.bestAskPrice = bestAskPrice;
	}

	public void setBestBidPrice(BigDecimal bestBidPrice) {
		this.bestBidPrice = bestBidPrice;
	}

	@Override
	public String toString() {
		return "SpreadInfo [ccyPair=" + ccyPair + ", spreadPercent=" + spreadPercent + ", bestAskExchange="
				+ bestAskExchange + ", bestAskPrice=" + bestAskPrice + ", bestBidExchange=" + bestBidExchange
				+ ", bestBidPrice=" + bestBidPrice + "]";
	}
}
