package com.op.bo;

import java.util.Date;

public class DailyResults {
	private Date date;
	private double profitPercent;
	private double cumulativeProfitPercent;
	private int tradeCount;
	private int cumulativeTradeCount;
	
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public double getProfitPercent() {
		return profitPercent;
	}
	public void setProfitPercent(double profitPercent) {
		this.profitPercent = profitPercent;
	}
	public double getCumulativeProfitPercent() {
		return cumulativeProfitPercent;
	}
	public void setCumulativeProfitPercent(double cumulativeProfitPercent) {
		this.cumulativeProfitPercent = cumulativeProfitPercent;
	}
	public int getTradeCount() {
		return tradeCount;
	}
	public void setTradeCount(int tradeCount) {
		this.tradeCount = tradeCount;
	}
	public int getCumulativeTradeCount() {
		return cumulativeTradeCount;
	}
	public void setCumulativeTradeCount(int cumulativeTradeCount) {
		this.cumulativeTradeCount = cumulativeTradeCount;
	}
}
