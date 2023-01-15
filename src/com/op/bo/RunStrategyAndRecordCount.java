package com.op.bo;

public class RunStrategyAndRecordCount {
	private String strategyName;
	private String runStrategy;
	private int totalTradeDataRowCount;

	public RunStrategyAndRecordCount(String strategyName, String runStrategy, int totalTradeDataRowCount) {
		super();
		this.strategyName = strategyName;
		this.runStrategy = runStrategy;
		this.totalTradeDataRowCount = totalTradeDataRowCount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((runStrategy == null) ? 0 : runStrategy.hashCode());
		result = prime * result + ((strategyName == null) ? 0 : strategyName.hashCode());
		result = prime * result + totalTradeDataRowCount;
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
		RunStrategyAndRecordCount other = (RunStrategyAndRecordCount) obj;
		if (runStrategy == null) {
			if (other.runStrategy != null)
				return false;
		} else if (!runStrategy.equals(other.runStrategy))
			return false;
		if (strategyName == null) {
			if (other.strategyName != null)
				return false;
		} else if (!strategyName.equals(other.strategyName))
			return false;
		if (totalTradeDataRowCount != other.totalTradeDataRowCount)
			return false;
		return true;
	}
	
	public String getStrategyName() {
		return strategyName;
	}
	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}
	public String getRunStrategy() {
		return runStrategy;
	}
	public void setRunStrategy(String runStrategy) {
		this.runStrategy = runStrategy;
	}
	public int getTotalTradeDataRowCount() {
		return totalTradeDataRowCount;
	}
	public void setTotalTradeDataRowCount(int totalTradeDataRowCount) {
		this.totalTradeDataRowCount = totalTradeDataRowCount;
	}
}
