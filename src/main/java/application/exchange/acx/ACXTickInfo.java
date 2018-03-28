package application.exchange.acx;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ACXTickInfo {

	private List<ACXTickDetailInfo> asks;

	private List<ACXTickDetailInfo> bids;

	public List<ACXTickDetailInfo> getAsks() {
		return asks;
	}

	public void setAsks(List<ACXTickDetailInfo> asks) {
		this.asks = asks;
	}

	public List<ACXTickDetailInfo> getBids() {
		return bids;
	}

	public void setBids(List<ACXTickDetailInfo> bids) {
		this.bids = bids;
	}

}
