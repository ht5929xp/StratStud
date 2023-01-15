package com.op.bo.targetstop;

import java.util.Date;

public class TargetStopInfo {
	private int tradeId;
	
	private TargetStopComboStrategyResult targetStopComboResult;
	
	private String symbol;
	private Date entryDate;
	private int entryDay;
	
	private double entryPrice;
	private double targetPrice;
	private double stopPrice;
	private double exitPrice;
	private double priceDiff;
	private double profitPercent;
	private double lossPercent;
	private boolean win;
	private boolean targetHit;
	private boolean stopHit;
	private Date estimatedExitDate; //MAE Date or MFE Date
	
	public int getEntryDay() {
		return entryDay;
	}
	public void setEntryDay(int entryDay) {
		this.entryDay = entryDay;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Date getEntryDate() {
		return entryDate;
	}
	public void setEntryDate(Date entryDate) {
		this.entryDate = entryDate;
	}
	public double getPriceDiff() {
		return priceDiff;
	}
	public void setPriceDiff(double priceDiff) {
		this.priceDiff = priceDiff;
	}
	public double getEntryPrice() {
		return entryPrice;
	}
	public void setEntryPrice(double entryPrice) {
		this.entryPrice = entryPrice;
	}
	public int getTradeId() {
		return tradeId;
	}
	public void setTradeId(int tradeId) {
		this.tradeId = tradeId;
	}
	public TargetStopComboStrategyResult getTargetStopComboResult() {
		return targetStopComboResult;
	}
	public void setTargetStopComboResult(TargetStopComboStrategyResult targetStopComboResult) {
		this.targetStopComboResult = targetStopComboResult;
	}
	public double getTargetPrice() {
		return targetPrice;
	}
	public void setTargetPrice(double targetPrice) {
		this.targetPrice = targetPrice;
	}
	public double getStopPrice() {
		return stopPrice;
	}
	public void setStopPrice(double stopPrice) {
		this.stopPrice = stopPrice;
	}
	public double getExitPrice() {
		return exitPrice;
	}
	public void setExitPrice(double exitPrice) {
		this.exitPrice = exitPrice;
	}
	public boolean isWin() {
		return win;
	}
	public void setWin(boolean win) {
		this.win = win;
	}
	public boolean isTargetHit() {
		return targetHit;
	}
	public void setTargetHit(boolean targetHit) {
		this.targetHit = targetHit;
	}
	public boolean isStopHit() {
		return stopHit;
	}
	public void setStopHit(boolean stopHit) {
		this.stopHit = stopHit;
	}
	public Date getEstimatedExitDate() {
		return estimatedExitDate;
	}
	public void setEstimatedExitDate(Date estimatedExitDate) {
		this.estimatedExitDate = estimatedExitDate;
	}
	public double getProfitPercent() {
		return profitPercent;
	}
	public void setProfitPercent(double profitPercent) {
		this.profitPercent = profitPercent;
	}
	public double getLossPercent() {
		return lossPercent;
	}
	public void setLossPercent(double lossPercent) {
		this.lossPercent = lossPercent;
	}
}
