package com.op.bo;

public class OptimizationMinimums {
	private double minPf;
	private double minWinRate;
	private double minPercentOfTargetsHit;
	private double minPercentOfStopsHit;
	
	public OptimizationMinimums(double minPf, double minWinRate, double minPercentOfTargetsHit,
			double minPercentOfStopsHit) {
		super();
		this.minPf = minPf;
		this.minWinRate = minWinRate;
		this.minPercentOfTargetsHit = minPercentOfTargetsHit;
		this.minPercentOfStopsHit = minPercentOfStopsHit;
	}
	
	public double getMinPf() {
		return minPf;
	}
	public void setMinPf(double minPf) {
		this.minPf = minPf;
	}
	public double getMinWinRate() {
		return minWinRate;
	}
	public void setMinWinRate(double minWinRate) {
		this.minWinRate = minWinRate;
	}
	public double getMinPercentOfTargetsHit() {
		return minPercentOfTargetsHit;
	}
	public void setMinPercentOfTargetsHit(double minPercentOfTargetsHit) {
		this.minPercentOfTargetsHit = minPercentOfTargetsHit;
	}
	public double getMinPercentOfStopsHit() {
		return minPercentOfStopsHit;
	}
	public void setMinPercentOfStopsHit(double minPercentOfStopsHit) {
		this.minPercentOfStopsHit = minPercentOfStopsHit;
	}
}
