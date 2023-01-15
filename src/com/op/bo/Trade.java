package com.op.bo;

import java.util.Date;

public class Trade {
	public static final double COMMISSION = 0.005; 
	
	private String symbol;
	private String strategy;
	
	private Date entryDate;
	private Date exitDate;
	
	private int filledShares;
	private Double entryPrice; //Avg Fill
	
	private boolean isShort;
	
	private int exitShares;
	private Double exitPrice; //Avg Fill 
	
	private String entryOrderType;
	private Double entryLimit;
	
	private String targetOrderType;
	private Double targetLimitPrice;
	
	private String stopOrderType;
	private Double stopPrice;
	private Double stopLimitPrice;
	
	private String timeoutOrderType;
	private Double timeoutLimitPrice;
	
	private Order sell; //short
	private Order buy; //long
	private Order stopLoss;
	private Order target;
	private Order timeout;
	
	public double getProfitWithCommission() {
		double profit = 0.0;
		
		if (entryPrice != null && exitPrice != null) {
			profit = (exitPrice - entryPrice) * filledShares - (COMMISSION * filledShares * 2);
		}
		
		return profit;
	}
	
	public double getProfit() {
		double profit = 0.0;
		
		if (entryPrice != null && exitPrice != null) {
			profit = (exitPrice - entryPrice) * filledShares;
		}
		
		return profit;
	}
	
	public double getPercentProfit() {
		double percProfit = 0.0;
		
		if (entryPrice != null && exitPrice != null) {
			percProfit = (exitPrice - entryPrice) / entryPrice;
		}
		
		return percProfit;
	}

	public double getExposure() {
		return filledShares * (entryPrice != null ? entryPrice : 0);
	}
	
	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public Date getEntryDate() {
		return entryDate;
	}

	public void setEntryDate(Date entryDate) {
		this.entryDate = entryDate;
	}

	public Date getExitDate() {
		return exitDate;
	}

	public void setExitDate(Date exitDate) {
		this.exitDate = exitDate;
	}

	public int getFilledShares() {
		return filledShares;
	}

	public void setFilledShares(int filledShares) {
		this.filledShares = filledShares;
	}

	public Double getEntryPrice() {
		return entryPrice;
	}

	public void setEntryPrice(Double entryPrice) {
		this.entryPrice = entryPrice;
	}

	public boolean isShort() {
		return isShort;
	}

	public void setShort(boolean isShort) {
		this.isShort = isShort;
	}

	public int getExitShares() {
		return exitShares;
	}

	public void setExitShares(int exitShares) {
		this.exitShares = exitShares;
	}

	public Double getExitPrice() {
		return exitPrice;
	}

	public void setExitPrice(Double exitPrice) {
		this.exitPrice = exitPrice;
	}

	public String getEntryOrderType() {
		return entryOrderType;
	}

	public void setEntryOrderType(String entryOrderType) {
		this.entryOrderType = entryOrderType;
	}

	public Double getEntryLimit() {
		return entryLimit;
	}

	public void setEntryLimit(Double entryLimit) {
		this.entryLimit = entryLimit;
	}

	public String getTargetOrderType() {
		return targetOrderType;
	}

	public void setTargetOrderType(String targetOrderType) {
		this.targetOrderType = targetOrderType;
	}

	public Double getTargetLimitPrice() {
		return targetLimitPrice;
	}

	public void setTargetLimitPrice(Double targetLimitPrice) {
		this.targetLimitPrice = targetLimitPrice;
	}

	public String getStopOrderType() {
		return stopOrderType;
	}

	public void setStopOrderType(String stopOrderType) {
		this.stopOrderType = stopOrderType;
	}

	public Double getStopPrice() {
		return stopPrice;
	}

	public void setStopPrice(Double stopPrice) {
		this.stopPrice = stopPrice;
	}

	public Double getStopLimitPrice() {
		return stopLimitPrice;
	}

	public void setStopLimitPrice(Double stopLimitPrice) {
		this.stopLimitPrice = stopLimitPrice;
	}

	public String getTimeoutOrderType() {
		return timeoutOrderType;
	}

	public void setTimeoutOrderType(String timeoutOrderType) {
		this.timeoutOrderType = timeoutOrderType;
	}

	public Double getTimeoutLimitPrice() {
		return timeoutLimitPrice;
	}

	public void setTimeoutLimitPrice(Double timeoutLimitPrice) {
		this.timeoutLimitPrice = timeoutLimitPrice;
	}

	public Order getSell() {
		return sell;
	}

	public void setSell(Order sell) {
		this.sell = sell;
	}

	public Order getBuy() {
		return buy;
	}

	public void setBuy(Order buy) {
		this.buy = buy;
	}

	public Order getStopLoss() {
		return stopLoss;
	}

	public void setStopLoss(Order stopLoss) {
		this.stopLoss = stopLoss;
	}

	public Order getTarget() {
		return target;
	}

	public void setTarget(Order target) {
		this.target = target;
	}

	public Order getTimeout() {
		return timeout;
	}

	public void setTimeout(Order timeout) {
		this.timeout = timeout;
	}
}
