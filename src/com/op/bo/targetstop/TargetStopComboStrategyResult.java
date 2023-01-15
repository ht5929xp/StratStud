package com.op.bo.targetstop;

public class TargetStopComboStrategyResult {
	private int targetStopComboResultId;
	private TargetStopStrategyResult targetResult;
	private TargetStopStrategyResult stopLossResult;

	public TargetStopComboStrategyResult(int targetStopComboResultId, TargetStopStrategyResult targetResult,
			TargetStopStrategyResult stopLossResult) {
		super();
		this.targetStopComboResultId = targetStopComboResultId;
		this.targetResult = targetResult;
		this.stopLossResult = stopLossResult;
	}
	
	public int getTargetStopComboResultId() {
		return targetStopComboResultId;
	}
	public void setTargetStopComboResultId(int targetStopComboResultId) {
		this.targetStopComboResultId = targetStopComboResultId;
	}
	public TargetStopStrategyResult getTargetResult() {
		return targetResult;
	}
	public void setTargetResult(TargetStopStrategyResult targetResult) {
		this.targetResult = targetResult;
	}
	public TargetStopStrategyResult getStopLossResult() {
		return stopLossResult;
	}
	public void setStopLossResult(TargetStopStrategyResult stopLossResult) {
		this.stopLossResult = stopLossResult;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + targetStopComboResultId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TargetStopComboStrategyResult other = (TargetStopComboStrategyResult) obj;
		if (targetStopComboResultId != other.targetStopComboResultId)
			return false;
		return true;
	}
}
