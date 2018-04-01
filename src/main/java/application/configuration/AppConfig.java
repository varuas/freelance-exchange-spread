package application.configuration;

import java.util.List;
import java.util.Map;

/**
 * Configuration for the application.
 */
public class AppConfig {

	/**
	 * The list of currency pairs to poll
	 */
	private List<String> currencyPairs;

	/**
	 * The list of exchanges to poll from
	 */
	private List<ExchangeConfig> exchanges;

	/**
	 * The thread pool configurations (key is pool ID and value is pool size)
	 */
	private Map<String, Integer> threadPools;

	/**
	 * Output display rate (in seconds)
	 */
	private Long refreshInterval;

	public List<String> getCurrencyPairs() {
		return currencyPairs;
	}
	public void setCurrencyPairs(List<String> currencyPairs) {
		this.currencyPairs = currencyPairs;
	}
	public List<ExchangeConfig> getExchanges() {
		return exchanges;
	}
	public void setExchanges(List<ExchangeConfig> exchanges) {
		this.exchanges = exchanges;
	}
	public Long getRefreshInterval() {
		return refreshInterval;
	}
	public void setRefreshInterval(Long refreshInterval) {
		this.refreshInterval = refreshInterval;
	}
	public Map<String, Integer> getThreadPools() {
		return threadPools;
	}
	public void setThreadPools(Map<String, Integer> threadPools) {
		this.threadPools = threadPools;
	}
}