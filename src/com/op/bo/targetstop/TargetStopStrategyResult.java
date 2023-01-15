package com.op.bo.targetstop;

import java.util.Map;

public class TargetStopStrategyResult {
	public static int maxResultId = 0;
	
	private int resultId;
	private String strategyProperties;
	private int numberOfTargetStopsHit; //# targets or Stops hit
	private double percentOfTargetStopsHit; //% targets or Stops hit
	
	private double targetPercent;
	private double stopLossPercent;
	private double targetDollar;
	private double stopLossDollar;
	
	/**
	 * Contains the Trade ID and the Target/Stop price (Not the delta).
	 */
	private Map<Integer, Double> targetStopsMap;

	public TargetStopStrategyResult(int resultId, String strategyProperties, int numberOfTargetStopsHit,
			double percentOfTargetStopsHit, Map<Integer, Double> targetStopsMap, double targetPercent,
			double stopLossPercent, double targetDollar, double stopLossDollar) {
		super();
		this.resultId = resultId;
		this.strategyProperties = strategyProperties;
		this.numberOfTargetStopsHit = numberOfTargetStopsHit;
		this.percentOfTargetStopsHit = percentOfTargetStopsHit;
		this.targetStopsMap = targetStopsMap;
		
		this.targetPercent = targetPercent;
		this.stopLossPercent = stopLossPercent;
		this.targetDollar = targetDollar;
		this.stopLossDollar = stopLossDollar;
	}
	
	public double getProfitTarget() {
		return this.targetPercent > 0.0 ? this.targetPercent : this.targetDollar;
	}
	
	public double getStopLoss() {
		return this.stopLossPercent > 0.0 ? this.stopLossPercent : this.stopLossDollar;
	}

	public boolean isDollar() {
		return targetDollar > 0.0;
	}
	
	public int getResultId() {
		return resultId;
	}
	public void setResultId(int resultId) {
		this.resultId = resultId;
	}
	public String getStrategyProperties() {
		return strategyProperties;
	}
	public void setStrategyProperties(String strategyProperties) {
		this.strategyProperties = strategyProperties;
	}
	public int getNumberOfTargetStopsHit() {
		return numberOfTargetStopsHit;
	}
	public void setNumberOfTargetStopsHit(int numberOfTargetStopsHit) {
		this.numberOfTargetStopsHit = numberOfTargetStopsHit;
	}
	public double getPercentOfTargetStopsHit() {
		return percentOfTargetStopsHit;
	}
	public void setPercentOfTargetStopsHit(double percentOfTargetStopsHit) {
		this.percentOfTargetStopsHit = percentOfTargetStopsHit;
	}
	public Map<Integer, Double> getTargetStopsMap() {
		return targetStopsMap;
	}
	public void setTargetStopsMap(Map<Integer, Double> targetStopsMap) {
		this.targetStopsMap = targetStopsMap;
	}
	public double getTargetPercent() {
		return targetPercent;
	}
	public void setTargetPercent(double targetPercent) {
		this.targetPercent = targetPercent;
	}
	public double getStopLossPercent() {
		return stopLossPercent;
	}
	public void setStopLossPercent(double stopLossPercent) {
		this.stopLossPercent = stopLossPercent;
	}
	public double getTargetDollar() {
		return targetDollar;
	}
	public void setTargetDollar(double targetDollar) {
		this.targetDollar = targetDollar;
	}
	public double getStopLossDollar() {
		return stopLossDollar;
	}
	public void setStopLossDollar(double stopLossDollar) {
		this.stopLossDollar = stopLossDollar;
	}
}
