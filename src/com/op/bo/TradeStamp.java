package com.op.bo;

import java.util.Date;

public class TradeStamp {
	private String symbol;
	private Date timestamp;
	private double avgFillPrice;
	private double marketPrice;
	private double pnl;
	
	public double getAvgFillPrice() {
		return avgFillPrice;
	}
	public void setAvgFillPrice(double avgFillPrice) {
		this.avgFillPrice = avgFillPrice;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public double getMarketPrice() {
		return marketPrice;
	}
	public void setMarketPrice(double marketPrice) {
		this.marketPrice = marketPrice;
	}
	public double getPnl() {
		return pnl;
	}
	public void setPnl(double pnl) {
		this.pnl = pnl;
	}
}
