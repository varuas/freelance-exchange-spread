package application.configuration;

import java.math.BigDecimal;

public class ExchangeConfig {

	private String id;
	private BigDecimal fee;
	private String connectorClass;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public BigDecimal getFee() {
		return fee;
	}
	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}
	public String getConnectorClass() {
		return connectorClass;
	}
	public void setConnectorClass(String connectorClass) {
		this.connectorClass = connectorClass;
	}
}
