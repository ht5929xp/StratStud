package com.op.bo;

import java.util.Date;

public class Order {
	private String symbol;
	private Integer orderId;
	private Integer parentId;
	private String status;
	private Integer shares;
	private Boolean buy;
	private String type;
	private Date gtd;
	private Double stop;
	private Double limit;
	private String strategy;
	private Integer filled;
	private Integer remaining;
	private Double lastFillPrice;
	private Double avgFillPrice;
	private Integer position;
	private String ocaGroup;
	private String logFileName;

	private Date submittedTs;
	private Date partialFilledTs; //First partial fill TS
	private Date filledTs;
	private Date cancelledTs;
	
	public Date getPartialFilledTs() {
		return partialFilledTs;
	}
	public void setPartialFilledTs(Date partialFilledTs) {
		this.partialFilledTs = partialFilledTs;
	}
	public String getLogFileName() {
		return logFileName;
	}
	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Integer getOrderId() {
		return orderId;
	}
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}
	public Integer getParentId() {
		return parentId;
	}
	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Integer getShares() {
		return shares;
	}
	public void setShares(Integer shares) {
		this.shares = shares;
	}
	public Boolean getBuy() {
		return buy;
	}
	public void setBuy(Boolean buy) {
		this.buy = buy;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Date getGtd() {
		return gtd;
	}
	public void setGtd(Date gtd) {
		this.gtd = gtd;
	}
	public Double getStop() {
		return stop;
	}
	public void setStop(Double stop) {
		this.stop = stop;
	}
	public Double getLimit() {
		return limit;
	}
	public void setLimit(Double limit) {
		this.limit = limit;
	}
	public String getStrategy() {
		return strategy;
	}
	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}
	public Integer getFilled() {
		return filled;
	}
	public void setFilled(Integer filled) {
		this.filled = filled;
	}
	public Integer getRemaining() {
		return remaining;
	}
	public void setRemaining(Integer remaining) {
		this.remaining = remaining;
	}
	public Double getLastFillPrice() {
		return lastFillPrice;
	}
	public void setLastFillPrice(Double lastFillPrice) {
		this.lastFillPrice = lastFillPrice;
	}
	public Double getAvgFillPrice() {
		return avgFillPrice;
	}
	public void setAvgFillPrice(Double avgFillPrice) {
		this.avgFillPrice = avgFillPrice;
	}
	public Integer getPosition() {
		return position;
	}
	public void setPosition(Integer position) {
		this.position = position;
	}
	public String getOcaGroup() {
		return ocaGroup;
	}
	public void setOcaGroup(String ocaGroup) {
		this.ocaGroup = ocaGroup;
	}
	public Date getSubmittedTs() {
		return submittedTs;
	}
	public void setSubmittedTs(Date submittedTs) {
		this.submittedTs = submittedTs;
	}
	public Date getFilledTs() {
		return filledTs;
	}
	public void setFilledTs(Date filledTs) {
		this.filledTs = filledTs;
	}
	public Date getCancelledTs() {
		return cancelledTs;
	}
	public void setCancelledTs(Date cancelledTs) {
		this.cancelledTs = cancelledTs;
	}
	@Override
	public String toString() {
		return "symbol = " + symbol + ", orderId = " + orderId + ", parentId = " + parentId + ", status = " + status
				+ ", shares = " + shares + ", buy = " + buy + ", type = " + type + ", gtd = " + gtd + ", stop = "
				+ stop + ", limit = " + limit + ", strategy = " + strategy + ", filled = " + filled
				+ ", remaining = " + remaining + ", lastFillPrice = " + lastFillPrice + ", avgFillPrice = "
				+ avgFillPrice + ", position = " + position + ", ocaGroup = " + ocaGroup + ", logFileName = "
				+ logFileName + ", submittedTs = " + submittedTs + ", filledTs = " + filledTs + ", cancelledTs = "
				+ cancelledTs;
	}
}
