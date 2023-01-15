package com.op;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;

import java.util.Map.Entry;

import com.op.bo.ExcelBuilder;
import com.op.bo.FilterRange;
import com.op.bo.GemParameters;
import com.op.bo.PathResources;
import com.op.bo.PriceLevels;
import com.op.bo.StratStats;
import com.op.bo.TIRunParameters;
import com.op.bo.TradeData;
import com.op.bo.TradeRecord;
import com.op.bo.Utility;
import com.op.bo.stoploss.strategy.DollarAmountStopLossStrategy;
import com.op.bo.stoploss.strategy.PercentOfATRStopLossStrategy;
import com.op.bo.stoploss.strategy.PercentOfPriceStopLossStrategy;
import com.op.bo.stoploss.strategy.SmartStopStrategy;
import com.op.bo.stoploss.strategy.StopLossStrategy;
import com.op.bo.target.strategy.DollarAmountTargetStrategy;
import com.op.bo.target.strategy.PercentOfATRTargetStrategy;
import com.op.bo.target.strategy.PercentOfPriceTargetStrategy;
import com.op.bo.target.strategy.TargetStrategy;
import com.op.bo.targetstop.TargetStopComboStrategyResult;
import com.op.bo.targetstop.TargetStopInfo;
import com.op.bo.targetstop.TargetStopStrategyResult;
import com.op.bo.targetstop.TargetStopStrategyStats;

public class GemAnalysis {
	
	private static Map<String, Integer> gemAnalysisMaxSequencesMap = new HashMap<>(); //Map of gem dir Name and the new analysis sequence
	private static final String RUN_FOR_STRATEGY = "crossed-resistance"; //Run only for this strategy
	private static final int NEW_TRADES_REVIEW_THRESHOLD = 2; //Atleast this many new trades to highlight to review.
	
	public static void main (String[] args) {
		Map<String, List<GemParameters>> gemParamMap = readGemParameters(RUN_FOR_STRATEGY);

		buildGemAnalysisNewSequencesMap(gemParamMap);
		
		List<StratStats> allStats = new ArrayList<>();
		for (Entry<String, List<GemParameters>> entry : gemParamMap.entrySet()) {
			File dataDir = new File(PathResources.DATA_PATH + File.separator + entry.getKey());
			
			if (dataDir.exists()) {
				try {
					Map<String, PriceLevels> priceLevelsData = Utility.extractPriceLevelData(dataDir);
					
					List<TradeData> tradeData = Utility.loadTradeData(dataDir, 100, 0, priceLevelsData);

					if (tradeData.size() > 1) {
						System.err.println("Cannot analyze gem for multi-group data.");
						continue;
					}
	
					for (GemParameters gemParam : entry.getValue()) {
						TradeData clonedData = (TradeData)SerializationUtils.clone(tradeData.get(0));
						StratStats stats = analyzeGem(clonedData, gemParam);
						allStats.add(stats);
					}
				} catch (IOException | ParseException e) {
					e.printStackTrace();
				}
			}
		}
		
		ExcelBuilder.buildGemAnalysisResultsExcel(allStats, RUN_FOR_STRATEGY);
	}

	private static Map<String, List<GemParameters>> readGemParameters(String readForStrategy) {
		Map<String, List<GemParameters>> allGemParamMap = new HashMap<>();

		File gemBaseDir = new File(PathResources.GEMS_DIR_PATH);
		for (File gemStrategyDir : gemBaseDir.listFiles()) {
			if (gemStrategyDir.isDirectory()) {
				for (File f : gemStrategyDir.listFiles()) {
					if (isExcelFile(f)) { // Gem Strategy Excel
						List<GemParameters> gemParam = Utility.readGemParameters(gemStrategyDir, f);
						if (gemParam != null && !gemParam.isEmpty()) {
							String strategyName = gemParam.get(0).getStrategyName();
							if (RUN_FOR_STRATEGY == null || RUN_FOR_STRATEGY.trim().isEmpty() || (RUN_FOR_STRATEGY.equals(strategyName))) {
								if (allGemParamMap.containsKey(strategyName)) {
									allGemParamMap.get(strategyName).addAll(gemParam);
								} else {
									allGemParamMap.put(strategyName, gemParam);
								}
							}
						}
					}
				}
			}
		}

		return allGemParamMap;
	}
	
	private static StratStats analyzeGem(TradeData tradeData, GemParameters param) {
		if (param.getFilters() != null && !param.getFilters().isEmpty()) {
			TradeData overallSubData = null;
			for (FilterRange range : param.getFilters()) {
				if (overallSubData == null) {
					overallSubData = Utility.getSubTradeData(tradeData, range);
				} else {
					overallSubData = Utility.getSubTradeData(overallSubData, range);
				}
			}
			
			tradeData = overallSubData;
		}

		double percentOfDaysForInSampleTest = updateTradeDataWithOutOfSampleFlagBasedOnNumberOfTrades(tradeData, param);
		
		TargetStopStrategyResult targetResult = null;
		TargetStopStrategyResult stopLossResult = null;
		
		if (param.getTargetDollar() > 0.0) {
			TargetStrategy targetStrategy = new DollarAmountTargetStrategy(tradeData.getTrades(), param.getMinTargetDollar());
			targetResult = targetStrategy.calculate(tradeData.getTrades(), param.getTargetDollar(), tradeData.isShort());
		} else if (param.getTargetPercent() > 0.0) {
			TargetStrategy targetStrategy = new PercentOfPriceTargetStrategy(tradeData.getTrades(), param.getMinTargetDollar());
			targetResult = targetStrategy.calculate(tradeData.getTrades(), param.getTargetPercent(), tradeData.isShort());
		} else if (param.getTargetPercentATR() > 0.0) {
			TargetStrategy targetStrategy = new PercentOfATRTargetStrategy(tradeData.getTrades(), param.getMinTargetDollar());
			targetResult = targetStrategy.calculate(tradeData.getTrades(), param.getTargetPercentATR(), tradeData.isShort());
		}

		if (param.getStopLossDollar() > 0.0) {
			StopLossStrategy stopStrategy = new DollarAmountStopLossStrategy(tradeData.getTrades(), param.getMinStopLossDollar());
			stopLossResult = stopStrategy.calculate(tradeData.getTrades(), param.getStopLossDollar(), tradeData.isShort());
		} else if (param.getStopLossPercent() > 0.0) {
			StopLossStrategy stopStrategy = new PercentOfPriceStopLossStrategy(tradeData.getTrades(), param.getMinStopLossDollar());
			stopLossResult = stopStrategy.calculate(tradeData.getTrades(), param.getStopLossPercent(), tradeData.isShort());
		} else if (param.getStopLossPercentATR() > 0.0) {
			StopLossStrategy stopStrategy = new PercentOfATRStopLossStrategy(tradeData.getTrades(), param.getMinStopLossDollar());
			stopLossResult = stopStrategy.calculate(tradeData.getTrades(), param.getStopLossPercentATR(), tradeData.isShort());
		} else if (param.isSmartStop()) {
			StopLossStrategy stopStrategy = new SmartStopStrategy(param.getMinStopLossDollar());
			stopLossResult = stopStrategy.calculate(tradeData.getTrades(), param.getStopLossPercentATR(), tradeData.isShort());
		}

		TargetStopComboStrategyResult targetStopComboResult = null;
		
		if (targetResult != null && stopLossResult != null) {
			List<TargetStopStrategyResult> targetResults = new ArrayList<>();
			targetResults.add(targetResult);
			List<TargetStopStrategyResult> stopLossResults = new ArrayList<>();
			stopLossResults.add(stopLossResult);

			Utility.updateTradeDataWithTargetStopMinPriceLevelDates(targetResults, stopLossResults, tradeData.getTrades(), tradeData.isShort());
			
			targetStopComboResult = new TargetStopComboStrategyResult(0, targetResult, stopLossResult);
			TargetStopStrategyStats targetStopStats = getTargetStopStrategyStats(tradeData, targetStopComboResult);
			updateTradeDataWithTargetStopOptimizationData(tradeData.getTrades(), targetStopStats);
		}

		StratStats stats = tradeData.getStatistics();
		stats.setTargetStopComboStrategyResult(targetStopComboResult);
		stats.calculateDetailedStatistics((int)percentOfDaysForInSampleTest);
		stats.setStatId(param.getStrategyId());
		stats.setGemParam(param);
		
		List<TIRunParameters> tiRunParam = new ArrayList<>();
		tiRunParam.add(tradeData.getRunParam());
		
		List<StratStats> statsList = new ArrayList<>();
		statsList.add(stats);

		int maxSeq = gemAnalysisMaxSequencesMap.get(param.getGemDirName());
		int newSeq = maxSeq + 1;

		String analysisFileName = getAnalysisFileName(param);
		
		String analysisDirBasePath = getAnalysisDirectoryBasePath(param);
		
		String prevAnalysisDirPath = analysisDirBasePath + File.separator + maxSeq;
		
		File prevAnalysisDir = new File(prevAnalysisDirPath);

		int prevAnalysisTradeCount = 0;

		List<GemParameters> prevParamList = Utility.readGemParameters(prevAnalysisDir, new File(prevAnalysisDir, analysisFileName), param.getStrategyId(), param.getStrategyName());
		if (prevParamList != null && prevParamList.size() == 1) {
			GemParameters prevParam = prevParamList.get(0);
			prevAnalysisTradeCount = prevParam.getGemOverallTradeCount() + prevParam.getGemFinalTradeCount();
		}
		
		int newAnalysisTradeCount = stats.getOverallTradeCount() + stats.getFinalTradeCount();
		
		int originalGemTradeCount = param.getGemOverallTradeCount() + param.getGemFinalTradeCount();
		int newTradeCount = tradeData.getTrades().size();
		int newTrades = newTradeCount - originalGemTradeCount;

		if ((prevAnalysisTradeCount != newAnalysisTradeCount || (prevAnalysisTradeCount == 0 && newAnalysisTradeCount == 0)) && newTrades >= NEW_TRADES_REVIEW_THRESHOLD) {
			String newAnalysisDirPath = analysisDirBasePath + File.separator + newSeq;
			File newAnalysisDir = new File(newAnalysisDirPath);
			if (!newAnalysisDir.exists()) {
				newAnalysisDir.mkdirs();
			}
			ExcelBuilder.buildOptimizationResultsExcel(newAnalysisDirPath, analysisFileName, "", statsList, tiRunParam,	null);
			System.out.println("Created new analysis file for Gem: " + param);
		} else if (prevParamList != null && prevParamList.size() == 1) {
			GemParameters prevParam = prevParamList.get(0);
			if (param.getGemFileLastModifiedDate().after(prevParam.getGemFileCreatedDate()) || newTrades < NEW_TRADES_REVIEW_THRESHOLD) {
				stats.setReviewed(true);
			}
		}

		return stats;
	}

	private static void buildGemAnalysisNewSequencesMap(Map<String, List<GemParameters>> allGemParamMap) {
		for (Entry<String, List<GemParameters>> entry : allGemParamMap.entrySet()) {
			for (GemParameters param : entry.getValue()) {
				if (!gemAnalysisMaxSequencesMap.containsKey(param.getGemDirName())) {
					gemAnalysisMaxSequencesMap.put(param.getGemDirName(), getMaxAnalysisSeq(param.getGemDirName(), param));
				}
			}
		}
	}
	
	private static String getAnalysisDirectoryBasePath(GemParameters gemParam) {
		return PathResources.GEMS_DIR_PATH + gemParam.getGemDirName() + File.separator + PathResources.ANALYSIS_DIR_NAME;
	}
	
	private static String getAnalysisFileName(GemParameters gemParam) {
		return gemParam.getStrategyName() + " [" + gemParam.getStrategyId() + "].xlsx";
	}
	
	private static int getMaxAnalysisSeq(String gemDir, GemParameters param) {
		int maxSeq = 0;
		
		String analysisDirPath = getAnalysisDirectoryBasePath(param);
		File analysisDir = new File(analysisDirPath);
		
		if (analysisDir.exists()) {
			if (analysisDir.listFiles() != null) {
				for (File f : analysisDir.listFiles()) {
					if (f.isDirectory()) {
						String seqStr = f.getName();
						try {
							int seq = Integer.parseInt(seqStr);
							if (seq > maxSeq) {
								maxSeq = seq;
							}
						} catch (NumberFormatException e) {
							// Continue
						}
					}
				}
			}
		}

		return maxSeq;
	}
	
	private static void updateTradeDataWithTargetStopOptimizationData(Collection<TradeRecord> allTrades, TargetStopStrategyStats targetStopStats) {
		System.out.println("Updating ALL Trade data with Target/Stop optimization results data...");

		Map<Integer, TradeRecord> tradeMap = new HashMap<>();
		for (TradeRecord rec : allTrades) {
			tradeMap.put(rec.getId(), rec);
		}

		for (TargetStopInfo info : targetStopStats.getInSampleTargetStopInfo()) {
			tradeMap.get(info.getTradeId()).getTargetStopInfo().add(info);
			tradeMap.get(info.getTradeId()).getTargetStopInfoMap()
					.put(info.getTargetStopComboResult().getTargetStopComboResultId(), info);
		}
		for (TargetStopInfo info : targetStopStats.getOutOfSampleTargetStopInfo()) {
			tradeMap.get(info.getTradeId()).getTargetStopInfo().add(info);
			tradeMap.get(info.getTradeId()).getTargetStopInfoMap()
					.put(info.getTargetStopComboResult().getTargetStopComboResultId(), info);
		}
		for (TargetStopInfo info : targetStopStats.getFinalTargetStopInfo()) {
			tradeMap.get(info.getTradeId()).getTargetStopInfo().add(info);
			tradeMap.get(info.getTradeId()).getTargetStopInfoMap()
					.put(info.getTargetStopComboResult().getTargetStopComboResultId(), info);
		}
		
		System.out.println("Finished updating ALL Trade data with Target/Stop optimization results data.");
	}
	
	private static TargetStopStrategyStats getTargetStopStrategyStats(TradeData data, TargetStopComboStrategyResult targetStopComboResult) {
					
		Map<Integer, Double> targets = targetStopComboResult.getTargetResult().getTargetStopsMap();
		Map<Integer, Double> stops = targetStopComboResult.getStopLossResult().getTargetStopsMap();
				
		List<TargetStopInfo> inSampleTargetStopInfo = new ArrayList<>();
		List<TargetStopInfo> outOfSampleTargetStopInfo = new ArrayList<>();
		List<TargetStopInfo> finalSampleTargetStopInfo = new ArrayList<>();
		for (TradeRecord rec : data.getTrades()) {
			double target = targets.get(rec.getId());
			double stop = stops.get(rec.getId());

			TargetStopInfo info = Utility.getTargetStopInfo(rec, target, stop, targetStopComboResult, data.isShort());

			if (rec.isOutOfSample()) {
				outOfSampleTargetStopInfo.add(info);
			} else if (rec.isInSample()) {
				inSampleTargetStopInfo.add(info);
			} else if(rec.isFinalTestSample()) {
				finalSampleTargetStopInfo.add(info);
			}
		}
		
		TargetStopStrategyStats stats = new TargetStopStrategyStats();
		stats.setTargetStopComboStrategyResult(targetStopComboResult);
		stats.setTradeData(data);
		stats.setInSampleTargetStopInfo(inSampleTargetStopInfo);
		stats.setOutOfSampleTargetStopInfo(outOfSampleTargetStopInfo);
		stats.setFinalTargetStopInfo(finalSampleTargetStopInfo);
		
		double grossProfitPercent = 0.0;
		double grossLossPercent = 0.0;
		
		int winCount = 0;
		int loseCount = 0;
		int targetsHit = 0;
		int stopsHit = 0;
		
		Set<Integer> distinctDaysOfTrading = new HashSet<Integer>();
		for (TargetStopInfo info : inSampleTargetStopInfo) {
			if (info.isWin()) {
				grossProfitPercent += (info.getPriceDiff()/info.getEntryPrice())*100.0;
				winCount++;
			} else {
				grossLossPercent += (-info.getPriceDiff()/info.getEntryPrice())*100.0;
				loseCount++;
			}
			
			if (info.isTargetHit()) {
				targetsHit++;
			} else if (info.isStopHit()) {
				stopsHit++;
			}
			
			distinctDaysOfTrading.add(info.getEntryDay());
		}

		stats.setTradeCount(winCount + loseCount);
		stats.setWinCount(winCount);
		stats.setLoseCount(loseCount);
		stats.setPf(grossProfitPercent/grossLossPercent);
		stats.setAvgWinner(grossProfitPercent/winCount*1.0);
		stats.setAvgLoser(grossLossPercent/loseCount*1.0);
		stats.setWinRate((Double.valueOf(winCount)/inSampleTargetStopInfo.size()*1.0)*100.0);
		stats.setPercentTargetsHit(targetsHit*1.0/inSampleTargetStopInfo.size());
		stats.setPercentStopsHit(stopsHit*1.0/inSampleTargetStopInfo.size());
		stats.setPercentDaysTraded((distinctDaysOfTrading.size()*1.0/data.getRanges().getInSampleDataDaysRange()*1.0)*100);
		
		return stats;
	}
	
	/**
	 * Returns percent of days for in-sample.
	 * @param data
	 * @param overallGemTradeCount
	 * @return
	 */
	private static double updateTradeDataWithOutOfSampleFlagBasedOnNumberOfTrades(TradeData data, GemParameters gemParam) {
		int inSampleGemTradeCount = gemParam.getGemInSampleTradeCount();
		int overallGemTradeCount = gemParam.getGemOverallTradeCount();
		
		Date inSampleMinTradeDate = null;
		Date inSampleMaxTradeDate = null;
		Date outOfSampleMinTradeDate = null;
		Date outOfSampleMaxTradeDate = null;
		Date finalMinTradeDate = null;
		Date finalMaxTradeDate = null;
		
		Date latestTradeDate = null;
		
		int count = 1;
		for (TradeRecord rec : data.getTrades()) {
			if (count > 0 && count <= inSampleGemTradeCount) {
				if (inSampleMinTradeDate == null) {
					inSampleMinTradeDate = rec.getEntryTime();
				}
				inSampleMaxTradeDate = rec.getEntryTime();
			} else if (count > inSampleGemTradeCount && count <= overallGemTradeCount) {
				if (outOfSampleMinTradeDate == null) {
					outOfSampleMinTradeDate = rec.getEntryTime();
				}
				rec.setOutOfSample(true);
				outOfSampleMaxTradeDate = rec.getEntryTime();
			} else if (count > overallGemTradeCount) {
				if (finalMinTradeDate == null) {
					finalMinTradeDate = rec.getEntryTime();
				}
				rec.setFinalTestSample(true);
				finalMaxTradeDate = rec.getEntryTime();
			}
					
			latestTradeDate = rec.getEntryTime();
			
			count++;
		}
		count = 0;

		data.getRanges().setInSampleDataDaysRange(TradeData.getWorkingDaysBetweenTwoDates(inSampleMinTradeDate, inSampleMaxTradeDate));
		data.getRanges().setInSampleMinTradeDate(inSampleMinTradeDate);
		data.getRanges().setInSampleMaxTradeDate(inSampleMaxTradeDate);
		data.getRanges().setOutOfSampleMinTradeDate(outOfSampleMinTradeDate);
		data.getRanges().setOutOfSampleMaxTradeDate(outOfSampleMaxTradeDate);
		data.getRanges().setFinalMinTradeDate(finalMinTradeDate);
		data.getRanges().setFinalMaxTradeDate(finalMaxTradeDate);
		
		if (outOfSampleMinTradeDate != null && outOfSampleMaxTradeDate != null) {
			data.getRanges().setOutOfSampleDataDaysRange(TradeData.getWorkingDaysBetweenTwoDates(outOfSampleMinTradeDate, outOfSampleMaxTradeDate));
		}
		
		if (finalMinTradeDate != null && finalMaxTradeDate != null) {
			data.getRanges().setFinalDataDaysRange(TradeData.getWorkingDaysBetweenTwoDates(finalMinTradeDate, finalMaxTradeDate));
		}
		
		int overallTradeDays = TradeData.getWorkingDaysBetweenTwoDates(inSampleMinTradeDate, latestTradeDate);
		
		double percentOfDaysForInSampleTest = data.getRanges().getInSampleDataDaysRange()*1.0 / overallTradeDays*1.0; 
		
		return percentOfDaysForInSampleTest;
	}
	
	private static boolean isExcelFile(File f) {
		return !f.isDirectory() && f.getName().endsWith(".xlsx");
	}
}
