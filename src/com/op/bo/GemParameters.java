package com.op.bo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GemParameters {
	public enum GemReviewResult {PendingReview, Reviewed, Successful, PaperTrading};
	
	private GemReviewResult reviewResult;
	private int strategyId;
	private String gemDirName;
	private String fileName;
	private String runConfigName;
	private String resultType;
	private String strategyName;
	private double targetPercent;
	private double stopLossPercent;
	private double targetDollar;
	private double stopLossDollar;
	private double targetPercentATR;
	private double stopLossPercentATR;
	private double minTargetDollar;
	private double minStopLossDollar;
	private boolean isSmartStop;
	private List<FilterRange> filters = new ArrayList<>();
	private Date gemFileLastModifiedDate;
	private Date gemFileLastAccessedDate;
	private Date gemFileCreatedDate;
	
	private int gemInSampleTradeCount; // In-Sample Trade Count used to generate Gem
	private int gemOverallTradeCount; // Overall Trade Count used to generate Gem
	private int gemFinalTradeCount; // Overall Trade Count used to generate Gem
	
	@Override
	public String toString() {
		return "fileName = " + fileName + ", strategyName = " + strategyName + ", strategyId = " + strategyId
				+ (targetPercent > 0.0 ? ", targetPercent = " + targetPercent : "")
				+ (stopLossPercent > 0.0 ? ", stopLossPercent = " + stopLossPercent : "")
				+ (targetDollar > 0.0 ? ", targetDollar = " + targetDollar : "")
				+ (stopLossDollar > 0.0 ? ", stopLossDollar = " + stopLossDollar : "")
				+ (targetPercentATR > 0.0 ? ", targetPercentATR = " + targetPercentATR : "")
				+ (stopLossPercentATR > 0.0 ? ", stopLossPercentATR = " + stopLossPercentATR : "")
				+ ", minTargetDollar = " + minTargetDollar + ", minStopLossDollar = " + minStopLossDollar
				+ ", filters = " + filters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + gemOverallTradeCount;
		result = prime * result + strategyId;
		result = prime * result + ((strategyName == null) ? 0 : strategyName.hashCode());
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
		GemParameters other = (GemParameters) obj;
		if (gemOverallTradeCount != other.gemOverallTradeCount)
			return false;
		if (strategyId != other.strategyId)
			return false;
		if (strategyName == null) {
			if (other.strategyName != null)
				return false;
		} else if (!strategyName.equals(other.strategyName))
			return false;
		return true;
	}

	public Date getGemFileLastModifiedDate() {
		return gemFileLastModifiedDate;
	}
	public void setGemFileLastModifiedDate(Date gemFileLastModifiedDate) {
		this.gemFileLastModifiedDate = gemFileLastModifiedDate;
	}
	public Date getGemFileLastAccessedDate() {
		return gemFileLastAccessedDate;
	}
	public void setGemFileLastAccessedDate(Date gemFileLastAccessedDate) {
		this.gemFileLastAccessedDate = gemFileLastAccessedDate;
	}
	public Date getGemFileCreatedDate() {
		return gemFileCreatedDate;
	}
	public void setGemFileCreatedDate(Date gemFileCreatedDate) {
		this.gemFileCreatedDate = gemFileCreatedDate;
	}
	public GemReviewResult getReviewResult() {
		return reviewResult;
	}
	public void setReviewResult(GemReviewResult reviewResult) {
		this.reviewResult = reviewResult;
	}
	public String getResultType() {
		return resultType;
	}
	public void setResultType(String resultType) {
		this.resultType = resultType;
	}
	public String getRunConfigName() {
		return runConfigName;
	}
	public void setRunConfigName(String runConfigName) {
		this.runConfigName = runConfigName;
	}
	public String getGemDirName() {
		return gemDirName;
	}
	public void setGemDirName(String gemDirName) {
		this.gemDirName = gemDirName;
	}
	public int getStrategyId() {
		return strategyId;
	}
	public void setStrategyId(int strategyId) {
		this.strategyId = strategyId;
	}
	public int getGemInSampleTradeCount() {
		return gemInSampleTradeCount;
	}
	public void setGemInSampleTradeCount(int gemInSampleTradeCount) {
		this.gemInSampleTradeCount = gemInSampleTradeCount;
	}
	public int getGemOverallTradeCount() {
		return gemOverallTradeCount;
	}
	public void setGemOverallTradeCount(int gemOverallTradeCount) {
		this.gemOverallTradeCount = gemOverallTradeCount;
	}
	public int getGemFinalTradeCount() {
		return gemFinalTradeCount;
	}
	public void setGemFinalTradeCount(int gemFinalTradeCount) {
		this.gemFinalTradeCount = gemFinalTradeCount;
	}
	public double getTargetPercentATR() {
		return targetPercentATR;
	}
	public void setTargetPercentATR(double targetPercentATR) {
		this.targetPercentATR = targetPercentATR;
	}
	public double getStopLossPercentATR() {
		return stopLossPercentATR;
	}
	public void setStopLossPercentATR(double stopLossPercentATR) {
		this.stopLossPercentATR = stopLossPercentATR;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public double getMinTargetDollar() {
		return minTargetDollar;
	}
	public void setMinTargetDollar(double minTargetDollar) {
		this.minTargetDollar = minTargetDollar;
	}
	public double getMinStopLossDollar() {
		return minStopLossDollar;
	}
	public void setMinStopLossDollar(double minStopLossDollar) {
		this.minStopLossDollar = minStopLossDollar;
	}
	public String getStrategyName() {
		return strategyName;
	}
	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
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
	public List<FilterRange> getFilters() {
		return filters;
	}
	public void setFilters(List<FilterRange> filters) {
		this.filters = filters;
	}
	public boolean isSmartStop() {
		return isSmartStop;
	}
	public void setSmartStop(boolean isSmartStop) {
		this.isSmartStop = isSmartStop;
	}
}
