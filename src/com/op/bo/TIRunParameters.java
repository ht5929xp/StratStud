package com.op.bo;

import java.io.Serializable;
import java.util.Date;

public class TIRunParameters implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static enum TimeExitType {minutes, time}
	public static enum PriceExitType {percent, dollar};
	private static int maxRunParamId;
	
	private int runParamId;
	
	private TimeExitType timeExitType;
	private PriceExitType priceExitType;
	
	private Date exitTimeOfDay;
	private int minutesFromOpen; // For time of day exits
	private int minutesFromEntry; // For minutes from entry exit
	
	private double profitTarget;
	private double stopLoss;

	public TIRunParameters() {
		super();
		runParamId = maxRunParamId + 1;
		maxRunParamId = runParamId;
	}
	
	public TimeExitType getTimeExitType() {
		return timeExitType;
	}
	public void setTimeExitType(TimeExitType timeExitType) {
		this.timeExitType = timeExitType;
	}
	public PriceExitType getPriceExitType() {
		return priceExitType;
	}
	public void setPriceExitType(PriceExitType priceExitType) {
		this.priceExitType = priceExitType;
	}
	public int getMinutesFromOpen() {
		return minutesFromOpen;
	}
	public void setMinutesFromOpen(int minutesFromOpen) {
		this.minutesFromOpen = minutesFromOpen;
	}
	public int getMinutesFromEntry() {
		return minutesFromEntry;
	}
	public void setMinutesFromEntry(int minutesFromEntry) {
		this.minutesFromEntry = minutesFromEntry;
	}
	public double getProfitTarget() {
		return profitTarget;
	}
	public void setProfitTarget(double profitTarget) {
		this.profitTarget = profitTarget;
	}
	public double getStopLoss() {
		return stopLoss;
	}
	public void setStopLoss(double stopLoss) {
		this.stopLoss = stopLoss;
	}
	public Date getExitTimeOfDay() {
		return exitTimeOfDay;
	}
	public void setExitTimeOfDay(Date exitTimeOfDay) {
		this.exitTimeOfDay = exitTimeOfDay;
	}
	public int getRunParamId() {
		return runParamId;
	}
	public void setRunParamId(int runParamId) {
		this.runParamId = runParamId;
	}
}
