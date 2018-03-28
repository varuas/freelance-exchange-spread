package application.configuration;

import java.util.List;

public class AppConfig {

	private List<String> currencyPairs;
	private List<ExchangeConfig> exchanges;
	private Long refreshInterval;
	private Integer ioThreads;

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
	public Integer getIoThreads() {
		return ioThreads;
	}
	public void setIoThreads(Integer ioThreads) {
		this.ioThreads = ioThreads;
	}

}