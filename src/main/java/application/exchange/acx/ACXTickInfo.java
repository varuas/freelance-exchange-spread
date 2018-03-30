package application.exchange.acx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON annotated POJO for the response received for GET requests from ACX.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ACXTickInfo {

	@JsonProperty("base_unit")
	private String baseCurrency;

	@JsonProperty("quote_unit")
	private String quoteCurrency;

	private ACXTickDetailInfo ticker;

	public String getBaseCurrency() {
		return baseCurrency;
	}

	public void setBaseCurrency(String baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	public String getQuoteCurrency() {
		return quoteCurrency;
	}

	public void setQuoteCurrency(String quoteCurrency) {
		this.quoteCurrency = quoteCurrency;
	}

	public ACXTickDetailInfo getTicker() {
		return ticker;
	}

	public void setTicker(ACXTickDetailInfo ticker) {
		this.ticker = ticker;
	}

}
