package application.configuration;

import java.math.BigDecimal;

/**
 * Configuration for the individual exchanges.
 */
public class ExchangeConfig {

	/**
	 * A unique identifier given to the exchange
	 */
	private String id;

	/**
	 * The bid & ask price fees (ratio from 0 to 1.0)
	 */
	private BigDecimal fee;

	/**
	 * The fully qualified class name corresponding to the exchange connector
	 */
	private String connectorClass;

	/**
	 * The thread pool identifier that will be used for this exchange pollling
	 */
	private String threadPool;

	/**
	 * The polling limit of the exchange (requests per second)
	 */
	private Double pollingLimit;

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
	public Double getPollingLimit() {
		return pollingLimit;
	}
	public void setPollingLimit(Double pollingLimit) {
		this.pollingLimit = pollingLimit;
	}
	public String getThreadPool() {
		return threadPool;
	}
	public void setThreadPool(String threadPool) {
		this.threadPool = threadPool;
	}
}
