package com.op.bo.targetstop;

import java.util.List;
import com.op.bo.StratStats;

public class TargetStopStrategyStats extends StratStats {	
	private List<TargetStopInfo> inSampleTargetStopInfo;
	private List<TargetStopInfo> outOfSampleTargetStopInfo;
	private List<TargetStopInfo> finalTargetStopInfo; //Used in Gem Analysis only
	
	private double percentTargetsHit;
	private double percentStopsHit;

	@Override
	public String toString() {
		return "Target - " + getTargetStopComboStrategyResult().getTargetResult().getStrategyProperties() + "\n\n"
				+ "Stop Loss - " + getTargetStopComboStrategyResult().getStopLossResult().getStrategyProperties()
				+ "\n\n" + "PF: " + getPf() + "\n" + "Win Rate: " + getWinRate() + "\nAvg Winner/Loser = " + getAvgWinnerLoser() 
				+ "\nTargets Hit: " + percentTargetsHit * 100.0 + "%\n" + "Stops Hit: " + percentStopsHit * 100.0 + "%";
	}

	public String getOneRowToString() {
		return this.getPf() + "   " + this.getWinRate() + "   T$="
				+ getTargetStopComboStrategyResult().getTargetResult().getTargetDollar() + ", T%="
				+ getTargetStopComboStrategyResult().getTargetResult().getTargetPercent() + ", S$="
				+ getTargetStopComboStrategyResult().getStopLossResult().getStopLossDollar() + ", S%="
				+ getTargetStopComboStrategyResult().getStopLossResult().getStopLossPercent() + ", %T hit="
				+ percentTargetsHit + ", %S Hit=" + percentStopsHit;
	}
	
	public List<TargetStopInfo> getFinalTargetStopInfo() {
		return finalTargetStopInfo;
	}
	public void setFinalTargetStopInfo(List<TargetStopInfo> finalTargetStopInfo) {
		this.finalTargetStopInfo = finalTargetStopInfo;
	}
	public List<TargetStopInfo> getInSampleTargetStopInfo() {
		return inSampleTargetStopInfo;
	}
	public void setInSampleTargetStopInfo(List<TargetStopInfo> inSampleTargetStopInfo) {
		this.inSampleTargetStopInfo = inSampleTargetStopInfo;
	}
	public List<TargetStopInfo> getOutOfSampleTargetStopInfo() {
		return outOfSampleTargetStopInfo;
	}
	public void setOutOfSampleTargetStopInfo(List<TargetStopInfo> outOfSampleTargetStopInfo) {
		this.outOfSampleTargetStopInfo = outOfSampleTargetStopInfo;
	}
	public double getPercentTargetsHit() {
		return percentTargetsHit;
	}
	public void setPercentTargetsHit(double percentTargetsHit) {
		this.percentTargetsHit = percentTargetsHit;
	}
	public double getPercentStopsHit() {
		return percentStopsHit;
	}
	public void setPercentStopsHit(double percentStopsHit) {
		this.percentStopsHit = percentStopsHit;
	}
}
