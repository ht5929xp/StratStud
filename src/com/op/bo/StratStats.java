package com.op.bo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.op.bo.targetstop.TargetStopComboStrategyResult;
import com.op.bo.targetstop.TargetStopInfo;

public class StratStats {
	private static final AtomicInteger atomicInteger = new AtomicInteger(0);
		
	private int statId;
	
	private boolean detailedStatsCalculated;

	private int tradeCount;
	private double pf;
	private double winRate;
	private double winCount;
	private double loseCount;
	private double avgWinner;
	private double avgLoser;
	private double percentDaysTraded; // Percent of the days having atleast one trade
	private List<FilterRange> filters;
	private double rmse;
	private double drawDown;

	private TradeData tradeData; // All Trade Data (In Sample + Out of Sample + Final Sample)
	private Map<Integer, Double> cumulativeProfitPercentMap; // ID - Cumulative Profit Percent

	private int osTradeCount;
	private double osPf;
	private double osWinRate;
	private int osWinCount;
	private int osLoseCount;
	private double osAvgWinner;
	private double osAvgLoser;
	private double osPercentDaysTraded;
	private double osRMSE;
	private double osDrawDown;

	// In-Sample & Out-of-Sample
	private int overallTradeCount;
	private double overallPf;
	private double overallWinRate;
	private int overallWinCount;
	private int overallLoseCount;
	private double overallAvgWinner;
	private double overallAvgLoser;
	private double overallPercentDaysTraded;
	private double overallDrawDown;
	private double overallRMSE;
	private double overallPercentOfTargetsHit;
	private double overallPercentOfStopsHit;

	private double qualityIndex;

	// Must always be excluded from all optimizations and controls
	private int finalTradeCount;
	private double finalPf;
	private double finalWinRate;
	private int finalWinCount;
	private int finalLoseCount;
	private double finalAvgWinner;
	private double finalAvgLoser;
	private double finalPercentDaysTraded;
	private double finalDrawDown;
	private double finalRMSE;
	private double finalPercentOfTargetsHit;
	private double finalPercentOfStopsHit;
	private double finalCumulativeProfitPercent;
	
	//For Gem Analysis
	private GemParameters gemParam;
	private boolean reviewed;
	private Date maxTradeDate = null;
	
	private TargetStopComboStrategyResult targetStopComboStrategyResult;
	
	public String getFinalAvgWinnerLoser() {
		return Utility.getAvgWinnerLoserStr(finalAvgWinner, finalAvgLoser);
	}
	
	public String getOverallAvgWinnerLoser() {
		return Utility.getAvgWinnerLoserStr(overallAvgWinner, overallAvgLoser);
	}
	
	public String getAvgWinnerLoser() {
		return Utility.getAvgWinnerLoserStr(avgWinner, avgLoser);
	}
	
	public String getOsAvgWinnerLoser() {
		return Utility.getAvgWinnerLoserStr(osAvgWinner, osAvgLoser);
	}
	
	public double getCumulativeProfitPercent(int id) {
		return cumulativeProfitPercentMap.get(id);
	}

	public StratStats() {
		super();
		this.statId = atomicInteger.getAndIncrement();
	}

	public Date getMaxTradeDate() {
		if (maxTradeDate == null) {

			Collection<TradeRecord> trades = tradeData.getTrades();

			for (TradeRecord rec : trades) {
				if (maxTradeDate == null || rec.getEntryTime().after(maxTradeDate)) {
					maxTradeDate = rec.getEntryTime();
				}
			}

			return maxTradeDate;
		} else {
			return maxTradeDate;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + statId;
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
		StratStats other = (StratStats) obj;
		if (statId != other.statId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		String toString = "Total Trades = " + tradeCount + "\nProfit Factor = " + pf + "\nWin Rate (%) = " + winRate
				+ "\nWin Count = " + winCount + "\nLose Count = " + loseCount + "\nAvg Winner/Loser = " + getAvgWinnerLoser()
				+ "\n% Days Traded = " + percentDaysTraded;

		if (filters != null) {
			toString += "\nFilter(s): \n\n";
			for (FilterRange range : filters) {
				toString += range + "\n";
			}
		}

		return toString;
	}

	public void calculateDetailedStatistics(int percentOfDaysForInSampleTest) {
		if (!detailedStatsCalculated) {
			calculateCumulativeProfitPercent();
			calculateOSStatistics();
			calculateOverallStatistics();
			calculateFinalStatistics();
			calculateMSE();
			calculateDrawDown();
			overallDrawDown = osDrawDown > drawDown ? osDrawDown : drawDown;
			calculateQualityIndex(percentOfDaysForInSampleTest);
			calculateTargetStopUtilizationPercentages();
			
			detailedStatsCalculated = true;
		}
	}

	private void calculateTargetStopUtilizationPercentages() {
		if (this.targetStopComboStrategyResult != null) {
			List<TargetStopInfo> overallTargetStopInfo = new ArrayList<>();
			
			for(TradeRecord rec : this.tradeData.getTrades()) {
				double target = targetStopComboStrategyResult.getTargetResult().getTargetStopsMap().get(rec.getId());
				double stop = targetStopComboStrategyResult.getStopLossResult().getTargetStopsMap().get(rec.getId());
				
				TargetStopInfo info = Utility.getTargetStopInfo(rec, target, stop, targetStopComboStrategyResult, this.tradeData.isShort());
				overallTargetStopInfo.add(info);
			}
			
			int targetsHit = 0;
			int stopsHit = 0;
			
			for (TargetStopInfo info : overallTargetStopInfo) {
				if (info.isWin()) {
					winCount++;
				} else {
					loseCount++;
				}
				
				if (info.isTargetHit()) {
					targetsHit++;
				} else if (info.isStopHit()) {
					stopsHit++;
				}
			}

			this.overallPercentOfTargetsHit = (targetsHit*1.0/tradeData.getTrades().size()) * 100.0;
			this.overallPercentOfStopsHit = (stopsHit*1.0/tradeData.getTrades().size()) * 100.0;
		}
	}
	
	/**
	 * Quality index is based on:
	 * 1) How many out of sample trades compared to in sample and duration
	 * 2) Out of sample PF, Win Rate compared to in sample
	 * 3) Out of sample RMSE compared to in sample
	 * 4) Overall draw-down
	 * 5) Overall profit factor
	 * 6) Overall win rate
	 */
	private void calculateQualityIndex(int percentOfDaysForInSampleTest) {
		if (percentOfDaysForInSampleTest < 100) {
			int osTradeCountInSameDurationAsInSample = (int)(percentOfDaysForInSampleTest*osTradeCount/(100 - percentOfDaysForInSampleTest));
			
			double osPercentOfIs = osTradeCountInSameDurationAsInSample*1.0 / tradeCount*1.0;
			double osPfPercentOfIs = osPf / pf;
			double osWinPercentOfIs = osWinRate / winRate;
	
			qualityIndex = osPercentOfIs*osPfPercentOfIs*osWinPercentOfIs*overallPf*overallWinRate/(overallRMSE*overallDrawDown);
		}
	}
	
	public static double calculateRSD(double numArray[]) {
		double sum = 0.0, standardDeviation = 0.0;
		int length = numArray.length;

		for (double num : numArray) {
			sum += num;
		}

		double mean = sum / length;

		for (double num : numArray) {
			standardDeviation += Math.pow(num - mean, 2);
		}

		return Math.sqrt(standardDeviation / length)/Math.abs(mean);
	}
	
	private void calculateCumulativeProfitPercent() {
		cumulativeProfitPercentMap = new LinkedHashMap<>();

		double cumulativeProfitPercent = 0.0;
		for (TradeRecord t : tradeData.getTrades()) {
			TargetStopInfo targetStopInfo = t.getTargetStopInfo(this.getTargetStopComboStrategyResult());
			
			double profitPercent = t.getProfitPercent();
			if (targetStopInfo != null) {
				profitPercent = targetStopInfo.getPriceDiff() / targetStopInfo.getEntryPrice();
			}
			
			cumulativeProfitPercent += profitPercent;
			cumulativeProfitPercentMap.put(t.getId(), cumulativeProfitPercent);
		}
	}

	private void calculateOSStatistics() {
		double osGrossProfitPercent = 0.0;
		double osGrossLossPercent = 0.0;

		int osWinCount = 0;
		int osLoseCount = 0;

		Set<Integer> osDistinctDaysOfTrading = new HashSet<Integer>();
		for (TradeRecord trade : tradeData.getTrades()) {
			if (trade.isOutOfSample()) {
				TargetStopInfo targetStopInfo = trade.getTargetStopInfo(this.getTargetStopComboStrategyResult());
				
				double priceDiff = trade.getPriceDiff();
				double entryPrice = trade.getEntryPrice();
				if (targetStopInfo != null) {
					priceDiff = targetStopInfo.getPriceDiff();
					entryPrice = targetStopInfo.getEntryPrice();
				}
						
				if (priceDiff > 0.0) {
					osGrossProfitPercent += (priceDiff/entryPrice)*100.0;
					osWinCount++;
				} else {
					osGrossLossPercent += (-priceDiff/entryPrice)*100.0;
					osLoseCount++;
					
				}
				
				osDistinctDaysOfTrading.add(trade.getEntryDay());
			}
		}

		this.setOsTradeCount(osWinCount + osLoseCount);
		this.setOsPf(osGrossProfitPercent / osGrossLossPercent);
		this.setOsWinRate((Double.valueOf(osWinCount) / this.getOsTradeCount() * 1.0) * 100.0);
		this.setOsWinCount(osWinCount);
		this.setOsLoseCount(osLoseCount);
		this.setOsAvgWinner(osGrossProfitPercent / osWinCount * 1.0);
		this.setOsAvgLoser(osGrossLossPercent / osLoseCount * 1.0);
		this.setOsPercentDaysTraded(
				(osDistinctDaysOfTrading.size() * 1.0 / tradeData.getRanges().getOutOfSampleDataDaysRange() * 1.0) * 100);
	}

	private void calculateOverallStatistics() {
		double overallGrossProfitPercent = 0.0;
		double overallGrossLossPercent = 0.0;

		int overallWinCount = 0;
		int overallLoseCount = 0;

		Set<Integer> overallDistinctDaysOfTrading = new HashSet<Integer>();
		for (TradeRecord trade : tradeData.getTrades()) {
			if (trade.isInSample() || trade.isOutOfSample()) {
				TargetStopInfo targetStopInfo = trade.getTargetStopInfo(this.getTargetStopComboStrategyResult());
				
				double priceDiff = trade.getPriceDiff();
				double entryPrice = trade.getEntryPrice();
				if (targetStopInfo != null) {
					priceDiff = targetStopInfo.getPriceDiff();
					entryPrice = targetStopInfo.getEntryPrice();
				}
	
				if (priceDiff > 0.0) {
					overallGrossProfitPercent += (priceDiff/entryPrice)*100.0;
					overallWinCount++;
				} else {
					overallGrossLossPercent += (-priceDiff/entryPrice)*100.0;
					overallLoseCount++;
				}
	
				overallDistinctDaysOfTrading.add(trade.getEntryDay());
			}
		}

		this.setOverallTradeCount(overallWinCount + overallLoseCount);
		this.setOverallPf(overallGrossProfitPercent / overallGrossLossPercent);
		this.setOverallWinRate((Double.valueOf(overallWinCount) / this.getOverallTradeCount() * 1.0) * 100.0);
		this.setOverallWinCount(overallWinCount);
		this.setOverallLoseCount(overallLoseCount);
		this.setOverallAvgWinner(overallGrossProfitPercent / overallWinCount * 1.0);
		this.setOverallAvgLoser(overallGrossLossPercent / overallLoseCount * 1.0);
		this.setOverallPercentDaysTraded(
				(overallDistinctDaysOfTrading.size() * 1.0 / (tradeData.getRanges().getInSampleDataDaysRange() + tradeData.getRanges().getOutOfSampleDataDaysRange()) * 1.0) * 100);
	}
	
	private void calculateFinalStatistics() {
		double finalGrossProfitPercent = 0.0;
		double finalGrossLossPercent = 0.0;

		int finalWinCount = 0;
		int finalLoseCount = 0;

		Set<Integer> finalDistinctDaysOfTrading = new HashSet<Integer>();
		for (TradeRecord trade : tradeData.getTrades()) {
			if (trade.isFinalTestSample()) {
				TargetStopInfo targetStopInfo = trade.getTargetStopInfo(this.getTargetStopComboStrategyResult());
				
				double priceDiff = trade.getPriceDiff();
				double entryPrice = trade.getEntryPrice();
				if (targetStopInfo != null) {
					priceDiff = targetStopInfo.getPriceDiff();
					entryPrice = targetStopInfo.getEntryPrice();
				}
	
				if (priceDiff > 0.0) {
					finalGrossProfitPercent += (priceDiff/entryPrice)*100.0;
					finalWinCount++;
				} else {
					finalGrossLossPercent += (-priceDiff/entryPrice)*100.0;
					finalLoseCount++;
				}
	
				finalDistinctDaysOfTrading.add(trade.getEntryDay());
			}
		}

		this.setFinalCumulativeProfitPercent(finalGrossProfitPercent - finalGrossLossPercent);
		this.setFinalTradeCount(finalWinCount + finalLoseCount);
		this.setFinalPf(finalGrossProfitPercent / finalGrossLossPercent);
		this.setFinalWinRate((Double.valueOf(finalWinCount) / this.getFinalTradeCount() * 1.0) * 100.0);
		this.setFinalWinCount(finalWinCount);
		this.setFinalLoseCount(finalLoseCount);
		this.setFinalAvgWinner(finalGrossProfitPercent / finalWinCount * 1.0);
		this.setFinalAvgLoser(finalGrossLossPercent / finalLoseCount * 1.0);
		this.setFinalPercentDaysTraded(
				(finalDistinctDaysOfTrading.size() * 1.0 / tradeData.getRanges().getFinalDataDaysRange() * 1.0) * 100);
	}
	
	private void calculateMSE() {
		int inSampleCount = 0;
		int outOfSampleCount = 0;
		int overallCount = 0;

		Map<Integer, Double> inSampleActualY = new HashMap<>();
		Map<Integer, Double> outOfSampleActualY = new HashMap<>();
		Map<Integer, Double> overallActualY = new HashMap<>();

		for (TradeRecord rec : tradeData.getTrades()) {
			if (rec.isOutOfSample()) {
				outOfSampleCount++;
				outOfSampleActualY.put(outOfSampleCount, getCumulativeProfitPercent(rec.getId()) * 100.0);
			} else if (rec.isInSample()) {
				inSampleCount++;
				inSampleActualY.put(inSampleCount, getCumulativeProfitPercent(rec.getId()) * 100.0);
			}
			
			overallCount++;
			overallActualY.put(overallCount, getCumulativeProfitPercent(rec.getId()) * 100.0);
		}

		SimpleRegression inSampleSr = new SimpleRegression();
		for (Map.Entry<Integer, Double> entry : inSampleActualY.entrySet()) {
			inSampleSr.addData(entry.getKey(), entry.getValue());
		}
		
		SimpleRegression outOfSampleSr = new SimpleRegression();
		for (Map.Entry<Integer, Double> entry : outOfSampleActualY.entrySet()) {
			outOfSampleSr.addData(entry.getKey(), entry.getValue());
		}
		
		SimpleRegression overallSr = new SimpleRegression();
		for (Map.Entry<Integer, Double> entry : overallActualY.entrySet()) {
			overallSr.addData(entry.getKey(), entry.getValue());
		}

		double inSampleErrorSquareSum = 0.0;
		for (int x : inSampleActualY.keySet()) {
			double yErrorSquare = Math.pow(inSampleSr.predict(x) - inSampleActualY.get(x), 2);
			inSampleErrorSquareSum += yErrorSquare;
		}

		double inSampleMSE = inSampleErrorSquareSum / inSampleCount;

		double outOfSampleErrorSquareSum = 0.0;
		for (int x : outOfSampleActualY.keySet()) {
			double yErrorSquare = Math.pow(outOfSampleSr.predict(x) - outOfSampleActualY.get(x), 2);
			outOfSampleErrorSquareSum += yErrorSquare;
		}

		double outOfSampleMSE = outOfSampleErrorSquareSum / outOfSampleCount;
		
		double overallErrorSquareSum = 0.0;
		for (int x : overallActualY.keySet()) {
			double yErrorSquare = Math.pow(overallSr.predict(x) - overallActualY.get(x), 2);
			overallErrorSquareSum += yErrorSquare;
		}

		double overallMSE = overallErrorSquareSum / overallCount;

		this.rmse = Math.sqrt(inSampleMSE);
		this.osRMSE = Math.sqrt(outOfSampleMSE);
		this.overallRMSE = Math.sqrt(overallMSE);
	}

	private void calculateDrawDown() {
		double maxProfitPercent = 0.0;
		double maxDrawDownPercent = 0.0;

		double osMaxProfitPercent = 0.0;
		double osMaxDrawDownPercent = 0.0;

		double cumulativeProfitPercent = 0.0;
		double osCumulativeProfitPercent = 0.0;
		for (TradeRecord t : tradeData.getTrades()) {
			TargetStopInfo targetStopInfo = t.getTargetStopInfo(this.getTargetStopComboStrategyResult());
			
			double profitPercent = t.getProfitPercent();
			if (targetStopInfo != null) {
				profitPercent = targetStopInfo.getPriceDiff() / targetStopInfo.getEntryPrice();
			}
			
			if (t.isInSample()) { // In-Sample
				cumulativeProfitPercent += profitPercent;

				if (cumulativeProfitPercent < maxProfitPercent) {
					double drawDownPercent = maxProfitPercent - cumulativeProfitPercent;
					if (drawDownPercent > maxDrawDownPercent) {
						maxDrawDownPercent = drawDownPercent;
					}
				} else if (cumulativeProfitPercent > maxProfitPercent) {
					maxProfitPercent = cumulativeProfitPercent;
				}
			} else if(t.isOutOfSample()) { // Out-of-Sample
				osCumulativeProfitPercent += profitPercent;

				if (osCumulativeProfitPercent < osMaxProfitPercent) {
					double drawDownPercent = osMaxProfitPercent - osCumulativeProfitPercent;
					if (drawDownPercent > osMaxDrawDownPercent) {
						osMaxDrawDownPercent = drawDownPercent;
					}
				} else if (osCumulativeProfitPercent > osMaxProfitPercent) {
					osMaxProfitPercent = osCumulativeProfitPercent;
				}
			}
		}

		drawDown = maxDrawDownPercent / maxProfitPercent;
		osDrawDown = osMaxDrawDownPercent / osMaxProfitPercent;
	}

	public Collection<DailyResults> getDayData() {
		SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		Map<String, DailyResults> dayMap = new LinkedHashMap<String, DailyResults>();
		double cumulativeProfitPercent = 0.0;
		int cumulativeTradeCount = 0;
		for (TradeRecord trade : getTradeData().getTrades()) {
			TargetStopInfo targetStopInfo = trade.getTargetStopInfo(this.getTargetStopComboStrategyResult());
			
			double profitPercent = trade.getProfitPercent();
			if (targetStopInfo != null) {
				profitPercent = targetStopInfo.getPriceDiff() / targetStopInfo.getEntryPrice();
			}
			
			String date = df.format(trade.getEntryTime());
			cumulativeProfitPercent += profitPercent;
			cumulativeTradeCount += 1;
			
			if (dayMap.containsKey(date)) {
				DailyResults results = dayMap.get(date);
				results.setCumulativeProfitPercent(cumulativeProfitPercent);
				results.setProfitPercent(results.getProfitPercent() + profitPercent);
				results.setTradeCount(results.getTradeCount() + 1);
				results.setCumulativeTradeCount(cumulativeTradeCount);
			} else {
				DailyResults results = new DailyResults();
				results.setCumulativeProfitPercent(cumulativeProfitPercent);
				try {
					results.setDate(df.parse(date));
				} catch (ParseException e) {
				}
				results.setProfitPercent(profitPercent);
				results.setTradeCount(1);
				results.setCumulativeTradeCount(cumulativeTradeCount);
				
				dayMap.put(date, results);
			}
		}
		
		return dayMap.values();
	}
	
	public int getTradeCount() {
		return tradeCount;
	}

	public void setTradeCount(int tradeCount) {
		this.tradeCount = tradeCount;
	}

	public double getPf() {
		return pf;
	}

	public void setPf(double pf) {
		this.pf = pf;
	}

	public double getWinRate() {
		return winRate;
	}

	public void setWinRate(double winRate) {
		this.winRate = winRate;
	}

	public double getWinCount() {
		return winCount;
	}

	public void setWinCount(double winCount) {
		this.winCount = winCount;
	}

	public double getLoseCount() {
		return loseCount;
	}

	public void setLoseCount(double loseCount) {
		this.loseCount = loseCount;
	}

	public double getAvgWinner() {
		return avgWinner;
	}

	public void setAvgWinner(double avgWinner) {
		this.avgWinner = avgWinner;
	}

	public double getAvgLoser() {
		return avgLoser;
	}

	public void setAvgLoser(double avgLoser) {
		this.avgLoser = avgLoser;
	}

	

	public List<FilterRange> getFilters() {
		return filters;
	}

	public void setFilters(List<FilterRange> filters) {
		this.filters = filters;
	}

	public double getPercentDaysTraded() {
		return percentDaysTraded;
	}

	public void setPercentDaysTraded(double percentDaysTraded) {
		this.percentDaysTraded = percentDaysTraded;
	}

	public TradeData getTradeData() {
		return tradeData;
	}

	public void setTradeData(TradeData tradeData) {
		this.tradeData = tradeData;
	}

	public int getOsTradeCount() {
		return osTradeCount;
	}

	public void setOsTradeCount(int osTradeCount) {
		this.osTradeCount = osTradeCount;
	}

	public double getOsPf() {
		return osPf;
	}

	public void setOsPf(double osPf) {
		this.osPf = osPf;
	}

	public double getOsWinRate() {
		return osWinRate;
	}

	public void setOsWinRate(double osWinRate) {
		this.osWinRate = osWinRate;
	}

	public double getOsAvgWinner() {
		return osAvgWinner;
	}

	public void setOsAvgWinner(double osAvgWinner) {
		this.osAvgWinner = osAvgWinner;
	}

	public double getOsAvgLoser() {
		return osAvgLoser;
	}

	public void setOsAvgLoser(double osAvgLoser) {
		this.osAvgLoser = osAvgLoser;
	}

	public int getOsWinCount() {
		return osWinCount;
	}

	public void setOsWinCount(int osWinCount) {
		this.osWinCount = osWinCount;
	}

	public int getOsLoseCount() {
		return osLoseCount;
	}

	public void setOsLoseCount(int osLoseCount) {
		this.osLoseCount = osLoseCount;
	}

	public double getOsPercentDaysTraded() {
		return osPercentDaysTraded;
	}

	public void setOsPercentDaysTraded(double osPercentDaysTraded) {
		this.osPercentDaysTraded = osPercentDaysTraded;
	}

	public double getDrawDown() {
		return drawDown;
	}

	public void setDrawDown(double drawDown) {
		this.drawDown = drawDown;
	}

	public double getOsDrawDown() {
		return osDrawDown;
	}

	public void setOsDrawDown(double osDrawDown) {
		this.osDrawDown = osDrawDown;
	}

	public Map<Integer, Double> getCumulativeProfitPercentMap() {
		return cumulativeProfitPercentMap;
	}

	public void setCumulativeProfitPercentMap(Map<Integer, Double> cumulativeProfitPercentMap) {
		this.cumulativeProfitPercentMap = cumulativeProfitPercentMap;
	}

	public int getOverallTradeCount() {
		return overallTradeCount;
	}

	public void setOverallTradeCount(int overallTradeCount) {
		this.overallTradeCount = overallTradeCount;
	}

	public double getOverallPf() {
		return overallPf;
	}

	public void setOverallPf(double overallPf) {
		this.overallPf = overallPf;
	}

	public double getOverallWinRate() {
		return overallWinRate;
	}

	public void setOverallWinRate(double overallWinRate) {
		this.overallWinRate = overallWinRate;
	}

	public int getOverallWinCount() {
		return overallWinCount;
	}

	public void setOverallWinCount(int overallWinCount) {
		this.overallWinCount = overallWinCount;
	}

	public int getOverallLoseCount() {
		return overallLoseCount;
	}

	public void setOverallLoseCount(int overallLoseCount) {
		this.overallLoseCount = overallLoseCount;
	}

	public double getOverallAvgWinner() {
		return overallAvgWinner;
	}

	public void setOverallAvgWinner(double overallAvgWinner) {
		this.overallAvgWinner = overallAvgWinner;
	}

	public double getOverallAvgLoser() {
		return overallAvgLoser;
	}

	public void setOverallAvgLoser(double overallAvgLoser) {
		this.overallAvgLoser = overallAvgLoser;
	}

	public double getOverallPercentDaysTraded() {
		return overallPercentDaysTraded;
	}

	public void setOverallPercentDaysTraded(double overallPercentDaysTraded) {
		this.overallPercentDaysTraded = overallPercentDaysTraded;
	}

	public double getOverallDrawDown() {
		return overallDrawDown;
	}

	public void setOverallDrawDown(double overallDrawDown) {
		this.overallDrawDown = overallDrawDown;
	}

	public double getQualityIndex() {
		return qualityIndex;
	}

	public void setQualityIndex(double qualityIndex) {
		this.qualityIndex = qualityIndex;
	}

	public TargetStopComboStrategyResult getTargetStopComboStrategyResult() {
		return targetStopComboStrategyResult;
	}

	public void setTargetStopComboStrategyResult(TargetStopComboStrategyResult targetStopComboStrategyResult) {
		this.targetStopComboStrategyResult = targetStopComboStrategyResult;
	}

	public double getRmse() {
		return rmse;
	}

	public double getOsRMSE() {
		return osRMSE;
	}

	public double getOverallRMSE() {
		return overallRMSE;
	}

	public double getOverallPercentOfTargetsHit() {
		return overallPercentOfTargetsHit;
	}

	public double getOverallPercentOfStopsHit() {
		return overallPercentOfStopsHit;
	}

	public int getStatId() {
		return statId;
	}

	public void setStatId(int statId) {
		this.statId = statId;
	}

	public int getFinalTradeCount() {
		return finalTradeCount;
	}

	public void setFinalTradeCount(int finalTradeCount) {
		this.finalTradeCount = finalTradeCount;
	}

	public double getFinalPf() {
		return finalPf;
	}

	public void setFinalPf(double finalPf) {
		this.finalPf = finalPf;
	}

	public double getFinalWinRate() {
		return finalWinRate;
	}

	public void setFinalWinRate(double finalWinRate) {
		this.finalWinRate = finalWinRate;
	}

	public int getFinalWinCount() {
		return finalWinCount;
	}

	public void setFinalWinCount(int finalWinCount) {
		this.finalWinCount = finalWinCount;
	}

	public int getFinalLoseCount() {
		return finalLoseCount;
	}

	public void setFinalLoseCount(int finalLoseCount) {
		this.finalLoseCount = finalLoseCount;
	}

	public double getFinalAvgWinner() {
		return finalAvgWinner;
	}

	public void setFinalAvgWinner(double finalAvgWinner) {
		this.finalAvgWinner = finalAvgWinner;
	}

	public double getFinalAvgLoser() {
		return finalAvgLoser;
	}

	public void setFinalAvgLoser(double finalAvgLoser) {
		this.finalAvgLoser = finalAvgLoser;
	}

	public double getFinalPercentDaysTraded() {
		return finalPercentDaysTraded;
	}

	public void setFinalPercentDaysTraded(double finalPercentDaysTraded) {
		this.finalPercentDaysTraded = finalPercentDaysTraded;
	}

	public double getFinalDrawDown() {
		return finalDrawDown;
	}

	public void setFinalDrawDown(double finalDrawDown) {
		this.finalDrawDown = finalDrawDown;
	}

	public double getFinalRMSE() {
		return finalRMSE;
	}

	public void setFinalRMSE(double finalRMSE) {
		this.finalRMSE = finalRMSE;
	}

	public double getFinalPercentOfTargetsHit() {
		return finalPercentOfTargetsHit;
	}

	public void setFinalPercentOfTargetsHit(double finalPercentOfTargetsHit) {
		this.finalPercentOfTargetsHit = finalPercentOfTargetsHit;
	}

	public double getFinalPercentOfStopsHit() {
		return finalPercentOfStopsHit;
	}

	public void setFinalPercentOfStopsHit(double finalPercentOfStopsHit) {
		this.finalPercentOfStopsHit = finalPercentOfStopsHit;
	}

	public GemParameters getGemParam() {
		return gemParam;
	}

	public void setGemParam(GemParameters gemParam) {
		this.gemParam = gemParam;
	}

	public double getFinalCumulativeProfitPercent() {
		return finalCumulativeProfitPercent;
	}

	public void setFinalCumulativeProfitPercent(double finalCumulativeProfitPercent) {
		this.finalCumulativeProfitPercent = finalCumulativeProfitPercent;
	}

	public boolean isReviewed() {
		return reviewed;
	}

	public void setReviewed(boolean reviewed) {
		this.reviewed = reviewed;
	}
}
