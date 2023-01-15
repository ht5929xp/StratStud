package com.op.bo;

public class PositionStatus {
	private String strategy;
	private String symbol;
	private double avgFillPrice;
	private double marketPrice;
	private int shares;
	private boolean shortPos;
	private double realizedPnl;
	private double unrealizedPnl;
	
	public double getMarketPrice() {
		return marketPrice;
	}
	public void setMarketPrice(double marketPrice) {
		this.marketPrice = marketPrice;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public double getAvgFillPrice() {
		return avgFillPrice;
	}
	public void setAvgFillPrice(double avgFillPrice) {
		this.avgFillPrice = avgFillPrice;
	}
	public int getShares() {
		return shares;
	}
	public void setShares(int shares) {
		this.shares = shares;
	}
	public boolean isShortPos() {
		return shortPos;
	}
	public void setShortPos(boolean shortPos) {
		this.shortPos = shortPos;
	}
	public double getRealizedPnl() {
		return realizedPnl;
	}
	public void setRealizedPnl(double realizedPnl) {
		this.realizedPnl = realizedPnl;
	}
	public double getUnrealizedPnl() {
		return unrealizedPnl;
	}
	public void setUnrealizedPnl(double unrealizedPnl) {
		this.unrealizedPnl = unrealizedPnl;
	}
	public String getStrategy() {
		return strategy;
	}
	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}
}
