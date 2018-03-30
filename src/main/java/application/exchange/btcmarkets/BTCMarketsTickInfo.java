package application.exchange.btcmarkets;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JSON annotated POJO for the GET response from BTCMarkets.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BTCMarketsTickInfo {

	private BigDecimal bestBid;

	private BigDecimal bestAsk;

	private Boolean success;

	private String instrument;

	private String currency;

	public BigDecimal getBestBid() {
		return bestBid;
	}

	public void setBestBid(BigDecimal bestBid) {
		this.bestBid = bestBid;
	}

	public BigDecimal getBestAsk() {
		return bestAsk;
	}

	public void setBestAsk(BigDecimal bestAsk) {
		this.bestAsk = bestAsk;
	}

	public Boolean getSuccess() {
		return success;
	}

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getInstrument() {
		return instrument;
	}

	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
}
