package application.exchange.acx;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Nested POJO for ACX market data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ACXTickDetailInfo {

	private BigDecimal buy;
	private BigDecimal sell;

	public BigDecimal getBuy() {
		return buy;
	}
	public void setBuy(BigDecimal buy) {
		this.buy = buy;
	}
	public BigDecimal getSell() {
		return sell;
	}
	public void setSell(BigDecimal sell) {
		this.sell = sell;
	}

}
