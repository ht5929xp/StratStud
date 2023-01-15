package com.op.bo;

import java.util.Date;
import java.util.List;

public class PortfolioStamp {
	private Date timestamp;
	private List<PositionStatus> status;
	
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public List<PositionStatus> getStatus() {
		return status;
	}
	public void setStatus(List<PositionStatus> status) {
		this.status = status;
	}
}
