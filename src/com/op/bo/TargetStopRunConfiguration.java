package com.op.bo;

public class TargetStopRunConfiguration {
	public static enum TargetStopRunStrategies {
		Dollar_And_Percent_TS,
		Any_Target_With_Smart_Stop,
		Percent_TS,
		Dollar_TS,
		ATR_TS,
		ALL
	}
	
	private double tsMinPf;
	private double tsMinWinRate;
	private int minPercentOfTargetsHit;
	private int minPercentOfStopsHit;
	
	private double targetToStop; // How much target is greater/less than stop. Value of 1 means target is greater than or equal to stop, Ex. 2 means target is atleast twice stop. 0 Ignores this property.
	private double minStopLoss; // Minimum stop loss in $
	private double minProfitTarget; // Minimum target in $
	
	private boolean dollarAmountTargetStrategy;
	private boolean percentOfPriceTargetStrategy;
	private boolean percentOfATRTargetStrategy;
	private boolean dollarAmountStopLossStrategy;
	private boolean percentOfPriceStopLossStrategy;
	private boolean percentOfATRStopLossStrategy;
	private boolean smartStopLossStrategy;
	
	public TargetStopRunConfiguration(double targetToStop, double minStopLoss, double minProfitTarget, TargetStopRunStrategies tsStrategy) {
		this.targetToStop = targetToStop;
		this.minStopLoss = minStopLoss;
		this.minProfitTarget = minProfitTarget;
		setBooleansByStrategy(tsStrategy);
	}
	
	public TargetStopRunConfiguration(double tsMinPf, double tsMinWinRate, int minPercentOfTargetsHit,
			int minPercentOfStopsHit, double targetToStop, double minStopLoss, double minProfitTarget,
			TargetStopRunStrategies tsStrategy) {
		this.tsMinPf = tsMinPf;
		this.tsMinWinRate = tsMinWinRate;
		this.minPercentOfTargetsHit = minPercentOfTargetsHit;
		this.minPercentOfStopsHit = minPercentOfStopsHit;
		this.targetToStop = targetToStop;
		this.minStopLoss = minStopLoss;
		this.minProfitTarget = minProfitTarget;
		setBooleansByStrategy(tsStrategy);
	}

	private void setBooleansByStrategy(TargetStopRunStrategies tsStrategy) {
		switch (tsStrategy) {
		case ALL:
			this.dollarAmountTargetStrategy = true;
			this.percentOfPriceTargetStrategy = true;
			this.percentOfATRTargetStrategy = true;
			this.dollarAmountStopLossStrategy = true;
			this.percentOfPriceStopLossStrategy = true;
			this.percentOfATRStopLossStrategy = true;
			this.smartStopLossStrategy = true;
			break;
		case ATR_TS:
			this.dollarAmountTargetStrategy = false;
			this.percentOfPriceTargetStrategy = false;
			this.percentOfATRTargetStrategy = true;
			this.dollarAmountStopLossStrategy = false;
			this.percentOfPriceStopLossStrategy = false;
			this.percentOfATRStopLossStrategy = true;
			this.smartStopLossStrategy = false;
			break;
		case Dollar_And_Percent_TS:
			this.dollarAmountTargetStrategy = true;
			this.percentOfPriceTargetStrategy = true;
			this.percentOfATRTargetStrategy = false;
			this.dollarAmountStopLossStrategy = true;
			this.percentOfPriceStopLossStrategy = true;
			this.percentOfATRStopLossStrategy = false;
			this.smartStopLossStrategy = false;
			break;
		case Any_Target_With_Smart_Stop:
			this.dollarAmountTargetStrategy = true;
			this.percentOfPriceTargetStrategy = true;
			this.percentOfATRTargetStrategy = true;
			this.dollarAmountStopLossStrategy = false;
			this.percentOfPriceStopLossStrategy = false;
			this.percentOfATRStopLossStrategy = false;
			this.smartStopLossStrategy = true;
			break;
		case Dollar_TS:
			this.dollarAmountTargetStrategy = true;
			this.percentOfPriceTargetStrategy = false;
			this.percentOfATRTargetStrategy = false;
			this.dollarAmountStopLossStrategy = true;
			this.percentOfPriceStopLossStrategy = false;
			this.percentOfATRStopLossStrategy = false;
			this.smartStopLossStrategy = false;
			break;
		case Percent_TS:
			this.dollarAmountTargetStrategy = false;
			this.percentOfPriceTargetStrategy = true;
			this.percentOfATRTargetStrategy = false;
			this.dollarAmountStopLossStrategy = false;
			this.percentOfPriceStopLossStrategy = true;
			this.percentOfATRStopLossStrategy = false;
			this.smartStopLossStrategy = false;
			break;
		default:
			break;
		}
	}
	
	public double getTsMinPf() {
		return tsMinPf;
	}

	public void setTsMinPf(double tsMinPf) {
		this.tsMinPf = tsMinPf;
	}

	public double getTsMinWinRate() {
		return tsMinWinRate;
	}

	public void setTsMinWinRate(double tsMinWinRate) {
		this.tsMinWinRate = tsMinWinRate;
	}

	public int getMinPercentOfTargetsHit() {
		return minPercentOfTargetsHit;
	}

	public void setMinPercentOfTargetsHit(int minPercentOfTargetsHit) {
		this.minPercentOfTargetsHit = minPercentOfTargetsHit;
	}

	public int getMinPercentOfStopsHit() {
		return minPercentOfStopsHit;
	}

	public void setMinPercentOfStopsHit(int minPercentOfStopsHit) {
		this.minPercentOfStopsHit = minPercentOfStopsHit;
	}

	public double getTargetToStop() {
		return targetToStop;
	}

	public void setTargetToStop(double targetToStop) {
		this.targetToStop = targetToStop;
	}

	public double getMinStopLoss() {
		return minStopLoss;
	}

	public void setMinStopLoss(double minStopLoss) {
		this.minStopLoss = minStopLoss;
	}

	public double getMinProfitTarget() {
		return minProfitTarget;
	}

	public void setMinProfitTarget(double minProfitTarget) {
		this.minProfitTarget = minProfitTarget;
	}

	public boolean isDollarAmountTargetStrategy() {
		return dollarAmountTargetStrategy;
	}

	public void setDollarAmountTargetStrategy(boolean dollarAmountTargetStrategy) {
		this.dollarAmountTargetStrategy = dollarAmountTargetStrategy;
	}

	public boolean isPercentOfPriceTargetStrategy() {
		return percentOfPriceTargetStrategy;
	}

	public void setPercentOfPriceTargetStrategy(boolean percentOfPriceTargetStrategy) {
		this.percentOfPriceTargetStrategy = percentOfPriceTargetStrategy;
	}

	public boolean isPercentOfATRTargetStrategy() {
		return percentOfATRTargetStrategy;
	}

	public void setPercentOfATRTargetStrategy(boolean percentOfATRTargetStrategy) {
		this.percentOfATRTargetStrategy = percentOfATRTargetStrategy;
	}

	public boolean isDollarAmountStopLossStrategy() {
		return dollarAmountStopLossStrategy;
	}

	public void setDollarAmountStopLossStrategy(boolean dollarAmountStopLossStrategy) {
		this.dollarAmountStopLossStrategy = dollarAmountStopLossStrategy;
	}

	public boolean isPercentOfPriceStopLossStrategy() {
		return percentOfPriceStopLossStrategy;
	}

	public void setPercentOfPriceStopLossStrategy(boolean percentOfPriceStopLossStrategy) {
		this.percentOfPriceStopLossStrategy = percentOfPriceStopLossStrategy;
	}

	public boolean isPercentOfATRStopLossStrategy() {
		return percentOfATRStopLossStrategy;
	}

	public void setPercentOfATRStopLossStrategy(boolean percentOfATRStopLossStrategy) {
		this.percentOfATRStopLossStrategy = percentOfATRStopLossStrategy;
	}

	public boolean isSmartStopLossStrategy() {
		return smartStopLossStrategy;
	}

	public void setSmartStopLossStrategy(boolean smartStopLossStrategy) {
		this.smartStopLossStrategy = smartStopLossStrategy;
	}
}
