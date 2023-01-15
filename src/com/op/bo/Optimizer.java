package com.op.bo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.util.CombinatoricsUtils;

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
import com.op.bo.targetstop.TargetStopStrategyStats;
import com.op.bo.targetstop.TargetStopStrategyResult;

public class Optimizer {
	
	private static final int MAX_THREADS = 10;
	private static final int MAX_STAT_COUNT = 1000;
	private static final int MAX_TARGET_STOP_STAT_COUNT = 1000; //The number of Target Stop strategies to consider.
	
	private static final int TARGET_TO_STOP_RATIO_ANOMALY_PERCENT = 5; //Percent of original total trades accepted to go against targetToStop restriction.
	
	private Set<Integer> lookup = Collections.synchronizedSet(new HashSet<Integer>());
	
	private int minTrades;
	private int minWinRate;
	private double minPf;
	private int minPercentOfDaysTraded;
	
	private double tsMinPf;
	private double tsMinWinRate;
	private int minPercentOfTargetsHit;
	private int minPercentOfStopsHit;
	private double targetToStop;
	private double minStopLoss;
	private double minProfitTarget;
	
	private boolean dollarAmountTargetStrategy;
	private boolean percentOfPriceTargetStrategy;
	private boolean percentOfATRTargetStrategy;
	private boolean dollarAmountStopLossStrategy;
	private boolean percentOfPriceStopLossStrategy;
	private boolean percentOfATRStopLossStrategy;
	private boolean smartStopLossStrategy;
	
	private int optimizeCount;
	private int discreteCount;

	private List<TradeRecord> inSampleTrades;
	private int percentOfDaysForInSampleTest;
	
	//Strategy Optimization
	private ConcurrentSkipListMap<Double, StratStats> maxPf = new ConcurrentSkipListMap<Double, StratStats>();
	private ConcurrentSkipListMap<Double, StratStats> maxPfWin = new ConcurrentSkipListMap<Double, StratStats>();
	private ConcurrentSkipListMap<Double, StratStats> maxPfWinTrades = new ConcurrentSkipListMap<Double, StratStats>();
	private ConcurrentSkipListMap<Double, StratStats> maxWin = new ConcurrentSkipListMap<Double, StratStats>();
	private ConcurrentSkipListMap<Double, StratStats> maxWinTrades = new ConcurrentSkipListMap<Double, StratStats>();

	//Target Stop Optimization
	private ConcurrentSkipListMap<Double, TargetStopStrategyStats> maxTargetStopPf = new ConcurrentSkipListMap<>();
	private ConcurrentSkipListMap<Double, TargetStopStrategyStats> maxTargetStopWin = new ConcurrentSkipListMap<>();
	private ConcurrentSkipListMap<Double, TargetStopStrategyStats> maxTargetStopPfWin = new ConcurrentSkipListMap<>();
		
	private boolean optimizeTargetStop;
	private int counter = 0;
	private long startMillis;
	
	private synchronized void addToTargetStopStatsMap(ConcurrentSkipListMap<Double, TargetStopStrategyStats> maxMap, TargetStopStrategyStats stats, double param) {
		if (maxMap.size() < MAX_TARGET_STOP_STAT_COUNT) {
			maxMap.put(param, stats);
		} else if (param > maxMap.firstKey()) {
			maxMap.remove(maxMap.firstKey());
			maxMap.put(param, stats);
		}
	}
	
	private synchronized void addToStatsMap(ConcurrentSkipListMap<Double, StratStats> maxMap, StratStats stats, double param) {
		if (maxMap.size() < MAX_STAT_COUNT) {
			maxMap.put(param, stats);
		} else if (param > maxMap.firstKey()) {
			maxMap.remove(maxMap.firstKey());
			maxMap.put(param, stats);
		}
	}
	
	public Optimizer(RunConfiguration param) {
		super();
		this.minTrades = param.getMinTrades();
		this.minPf = param.getMinPf();
		this.minWinRate = param.getMinWinRate();
		this.optimizeCount = param.getOptimizeCount();
		this.discreteCount = param.getDiscreteCount();
		this.minPercentOfDaysTraded = param.getMinPercentOfDaysTraded();
		this.minPercentOfTargetsHit = param.getTsRunConfigurations().getMinPercentOfTargetsHit();
		this.minPercentOfStopsHit = param.getTsRunConfigurations().getMinPercentOfStopsHit();
		this.tsMinPf = param.getTsRunConfigurations().getTsMinPf();
		this.tsMinWinRate = param.getTsRunConfigurations().getTsMinWinRate();
		this.targetToStop = param.getTsRunConfigurations().getTargetToStop();
		this.minStopLoss = param.getTsRunConfigurations().getMinStopLoss();
		this.minProfitTarget = param.getTsRunConfigurations().getMinProfitTarget();
		this.percentOfDaysForInSampleTest = param.getPercentOfDaysForInSampleTest();
		this.dollarAmountTargetStrategy = param.getTsRunConfigurations().isDollarAmountTargetStrategy();
		this.percentOfPriceTargetStrategy = param.getTsRunConfigurations().isPercentOfPriceTargetStrategy();
		this.percentOfATRTargetStrategy = param.getTsRunConfigurations().isPercentOfATRTargetStrategy();
		this.dollarAmountStopLossStrategy = param.getTsRunConfigurations().isDollarAmountStopLossStrategy();
		this.percentOfPriceStopLossStrategy = param.getTsRunConfigurations().isPercentOfPriceStopLossStrategy();
		this.percentOfATRStopLossStrategy = param.getTsRunConfigurations().isPercentOfATRStopLossStrategy();
		this.smartStopLossStrategy = param.getTsRunConfigurations().isSmartStopLossStrategy();
	}

	private class OptimizerThread extends Thread {
		public OptimizerThread(int count, int combinationsSize, List<String> columns, TradeData data) {
			super();
			this.combinationsSize = combinationsSize;
			this.columns = columns;
			this.data = data;
		}

		int combinationsSize;
		List<String> columns;
		TradeData data;

		public void run() {
			optimizeStrat(0, null, null, columns, data);
			counter++;
			long millis = System.currentTimeMillis();
			long elapsed = millis - startMillis;
			long elapsedSeconds = elapsed/1000;
			long estSecondsTotal = (((combinationsSize * elapsed) / counter) / 1000);
			long remainingSeconds = estSecondsTotal - elapsedSeconds;
			long remainingMinutes = remainingSeconds/60;
			System.out.println("Completed " + counter + " of " + combinationsSize + " combinations ... lookup.size() = "
					+ lookup.size() + " ... remaining = "
					+ (remainingMinutes > 120 ? (remainingMinutes/60) + " hours ..." : (remainingSeconds > 120 ? (remainingSeconds / 60 + " minutes ...") : remainingSeconds + " seconds ..."))
					+ " atleast " + maxPf.size() + " strategies found.");
		}
	}
	
	private class TargetStopOptimizerThread extends Thread {
		public TargetStopOptimizerThread(TradeData data, TargetStopComboStrategyResult targetStopComboResult, 
				int targetStopResultsSize, double minPf, double minWinRate,	double minPercentOfTargetsHit, double minPercentOfStopsHit) {
			super();
			this.targetStopComboResult = targetStopComboResult;
			this.data = data;
			this.allTargetStopResultsSize = targetStopResultsSize;
			this.minPf = minPf;
			this.minWinRate = minWinRate;
			this.minPercentOfTargetsHit = minPercentOfTargetsHit;
			this.minPercentOfStopsHit = minPercentOfStopsHit;
		}

		private TargetStopComboStrategyResult targetStopComboResult;
		private TradeData data;
		private int allTargetStopResultsSize;
		
		private double minPf;
		private double minWinRate;
		private double minPercentOfTargetsHit;
		private double minPercentOfStopsHit;
				
		public void run() {
			optimizeTargetStop(data, targetStopComboResult, minPf, minWinRate, minPercentOfTargetsHit, minPercentOfStopsHit);
			counter++;
			long millis = System.currentTimeMillis();
			long elapsed = millis - startMillis;
			long elapsedSeconds = elapsed/1000;
			long estSecondsTotal = (((allTargetStopResultsSize * elapsed) / counter) / 1000);
			long remainingSeconds = estSecondsTotal - elapsedSeconds;
			long remainingMinutes = remainingSeconds/60;
			
			if (counter % 10000 == 0 || (counter == allTargetStopResultsSize)) {
				System.out.println("Completed processing " + counter + " of " + allTargetStopResultsSize + " Target/Stop Strategy results ... remaining = "
						+ (remainingMinutes > 120 ? (remainingMinutes/60) + " hours ..." : (remainingSeconds > 120 ? (remainingSeconds / 60 + " minutes ...") : remainingSeconds + " seconds ..."))
						+ " atleast " + maxTargetStopPf.size() + " strategies found.");
				System.gc();
			}
		}
	}

	/**
	 * If ignoreMinimums is true, this means all target/Stop combinations will be considered regardless of PF, Win Rate, etc.
	 * If ignoreMinimums is false, this means the parameters in the Optimizer class will be used to filter out unwanted target/stop results.
	 * @param data
	 * @param ignoreMinimums
	 * @return
	 * @throws InterruptedException
	 */
	public OptimizationResults optimizeTargetStop(TradeData data) throws InterruptedException {
		double minPf = this.tsMinPf;
		double minWinRate = this.tsMinWinRate;
		double minPercentOfTargetsHit = this.minPercentOfTargetsHit;
		double minPercentOfStopsHit = this.minPercentOfStopsHit;
		
		System.out.println("Optimizing Target Stop for provided Strategy.");
		
		List<TargetStopStrategyResult> allTargetResults = new ArrayList<>();
		List<TargetStopStrategyResult> allStopLossResults = new ArrayList<>();
		
		if (this.dollarAmountTargetStrategy) {
			TargetStrategy dollarTargetStrategy = new DollarAmountTargetStrategy(data.getTrades(), this.minProfitTarget);
			List<TargetStopStrategyResult> dollarAmountTargetStrategyResults = dollarTargetStrategy.calculate(data.getTrades(), data.isShort());
			allTargetResults.addAll(dollarAmountTargetStrategyResults);
		}
		
		if (this.percentOfPriceTargetStrategy) {
			TargetStrategy percentTargetStrategy = new PercentOfPriceTargetStrategy(data.getTrades(), this.minProfitTarget);
			List<TargetStopStrategyResult> percentTargetStrategyResults = percentTargetStrategy.calculate(data.getTrades(), data.isShort());
			allTargetResults.addAll(percentTargetStrategyResults);
		}
		
		if (this.percentOfATRTargetStrategy) {
			TargetStrategy percentOfATRTargetStrategy = new PercentOfATRTargetStrategy(data.getTrades(), this.minProfitTarget);
			List<TargetStopStrategyResult> percentOfATRTargetStrategyResults = percentOfATRTargetStrategy.calculate(data.getTrades(), data.isShort());
			allTargetResults.addAll(percentOfATRTargetStrategyResults);
		}
		
		if (this.dollarAmountStopLossStrategy) {
			StopLossStrategy dollarStopLossStrategy = new DollarAmountStopLossStrategy(data.getTrades(), this.minStopLoss);
			List<TargetStopStrategyResult> dollarAmountStopLossStrategyResults = dollarStopLossStrategy.calculate(data.getTrades(), data.isShort());
			allStopLossResults.addAll(dollarAmountStopLossStrategyResults);
		}
		
		if (this.percentOfPriceStopLossStrategy) {
			StopLossStrategy percentStopLossStrategy = new PercentOfPriceStopLossStrategy(data.getTrades(), this.minStopLoss);
			List<TargetStopStrategyResult> percentStopLossStrategyResults = percentStopLossStrategy.calculate(data.getTrades(), data.isShort());
			allStopLossResults.addAll(percentStopLossStrategyResults);
		}
		
		if (this.percentOfATRStopLossStrategy) {
			StopLossStrategy percentOfATRStopLossStrategy = new PercentOfATRStopLossStrategy(data.getTrades(), this.minStopLoss);
			List<TargetStopStrategyResult> percentOfATRStopLossStrategyResults = percentOfATRStopLossStrategy.calculate(data.getTrades(), data.isShort());
			allStopLossResults.addAll(percentOfATRStopLossStrategyResults);
		}
		
		if (this.smartStopLossStrategy) {
			StopLossStrategy smartStopStrategy = new SmartStopStrategy(this.minStopLoss);
			List<TargetStopStrategyResult> smartStopStrategyResults = smartStopStrategy.calculate(data.getTrades(), data.isShort());
			allStopLossResults.addAll(smartStopStrategyResults);
		}

		System.out.println("\nAnalyzing trades using calculated targets and stops and calculating statisitics. Using parameters: \n"
				+ "- Minimum PF: " + minPf + "\n"
				+ "- Minimum Win%: " + minWinRate + "\n"
				+ "- Minimum Targets Hit: " + minPercentOfTargetsHit + "%\n"
				+ "- Minimum Stops Hit: " + minPercentOfStopsHit + "%\n"
				+ "- Target To Stop: " + targetToStop + "\n");
		
		System.out.println("Target counts: " + allTargetResults.size());
		System.out.println("Stop Loss counts: " + allStopLossResults.size());
				
		Utility.updateTradeDataWithTargetStopMinPriceLevelDates(allTargetResults, allStopLossResults, data.getTrades(), data.isShort());

		int targetStopResultsSize = allTargetResults.size() * allStopLossResults.size();
		
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);
		
		int targetStopComboResultId = 0;
		startMillis = System.currentTimeMillis();
		for (TargetStopStrategyResult targetResult : allTargetResults) {
			for (TargetStopStrategyResult stopLossResult : allStopLossResults) {
				targetStopComboResultId++;
				TargetStopComboStrategyResult targetStopComboResult = new TargetStopComboStrategyResult(targetStopComboResultId, targetResult, stopLossResult);
				TargetStopOptimizerThread thread = new TargetStopOptimizerThread(data, targetStopComboResult, 
						targetStopResultsSize, minPf, minWinRate, minPercentOfTargetsHit, minPercentOfStopsHit);
				executor.execute(thread);
			}
		}
		
		executor.shutdown();
		executor.awaitTermination(1000, TimeUnit.MINUTES);
		executor.shutdownNow();
		
		Thread.sleep(500);

		updateTradeDataWithTargetStopOptimizationData(data.getTrades());
		
		System.out.println("\nTarget/Stop Optimization Results: \n"
				+ "- Maximize Win% count: " + maxTargetStopWin.size() + "\n"
				+ "- Maximize PF count: " + maxTargetStopPf.size() + "\n");
		
		System.out.println("Max Win% ===> ");
		int count = 0;
		for (TargetStopStrategyStats stat : maxTargetStopWin.descendingMap().values()) {
			System.out.println("\n" + stat);
			System.out.println("------------------------------");

			if (count++ > 10) {
				break;
			}
		}
		
		System.out.println("Max PF ===> ");
		count = 0;
		for (TargetStopStrategyStats stat : maxTargetStopPf.descendingMap().values()) {
			System.out.println("\n" + stat);
			System.out.println("------------------------------");
			
			if (count++ > 10) {
				break;
			}
		}

		System.out.println("Max PF Win===> ");
		count = 0;
		for (TargetStopStrategyStats stat : maxTargetStopPfWin.descendingMap().values()) {
			System.out.println("\n" + stat);
			System.out.println("------------------------------");
			
			if (count++ > 10) {
				break;
			}
		}
		
		Map<Integer, StratStats> allStratsMap = new HashMap<Integer, StratStats>();
		
		List<StratStats> maxPfList = new ArrayList<StratStats>();
		List<StratStats> maxPfWinList = new ArrayList<StratStats>();
		List<StratStats> maxWinList = new ArrayList<StratStats>();
		
		for (Entry<Double, TargetStopStrategyStats> entry : maxTargetStopWin.entrySet()) {
			StratStats stats = entry.getValue();
			maxWinList.add(stats);
			allStratsMap.put(stats.getTargetStopComboStrategyResult().getTargetStopComboResultId(), stats);
		}
		
		for (Entry<Double, TargetStopStrategyStats> entry : maxTargetStopPf.entrySet()) {
			StratStats stats = entry.getValue();
			maxPfList.add(stats);
			allStratsMap.put(stats.getTargetStopComboStrategyResult().getTargetStopComboResultId(), stats);
		}
		
		for (Entry<Double, TargetStopStrategyStats> entry : maxTargetStopPfWin.entrySet()) {
			StratStats stats = entry.getValue();
			maxPfWinList.add(stats);
			allStratsMap.put(stats.getTargetStopComboStrategyResult().getTargetStopComboResultId(), stats);
		}
		
		System.out.println("\nIn Sample Optimization counts matching minimum criteria: ");
		System.out.println("maxTargetStopPf strategy count: " + maxTargetStopPf.size());
		System.out.println("maxTargetStopPfWin strategy count: " + maxTargetStopPfWin.size());
		System.out.println("maxTargetStopWin strategy count: " + maxTargetStopWin.size());
		
		/*
		 * Calculate detailed statistics for all strategies for all data.
		 */
		for (StratStats stat : allStratsMap.values()) {
			stat.calculateDetailedStatistics(percentOfDaysForInSampleTest);
		}
		
		OptimizationResults results = new OptimizationResults();
		
		/*
		 * The lists will now be ordered based on the overall data and not just in sample.
		 */
		sortBasedOnOverallStat(maxPfList, "PF");
		sortBasedOnOverallStat(maxPfWinList, "PF_WIN");
		sortBasedOnOverallStat(maxWinList, "WIN");
		
		results.setMaxPf(maxPfList);
		results.setMaxPfWin(maxPfWinList);
		results.setMaxWin(maxWinList);
		
		List<StratStats> list = new ArrayList<>(allStratsMap.values());
		sortBasedOnOverallStat(list, "Q_INDEX");
		results.setMaxQIndex(list);
		
		List<TIRunParameters> runParam = new ArrayList<TIRunParameters>();
		runParam.add(data.getRunParam());
		
		results.setRunParam(runParam);
		results.setStrategyName(data.getStrategyName());
		
		counter = 0;
		
		results.calculateAbsMaximums();
		
		return results;
	}
		
	private List<TradeRecord> getInSampleTrades(TradeData data) {
		if (this.inSampleTrades == null) {
			List<TradeRecord> inSampleTrades = new ArrayList<>();
			for (TradeRecord rec : data.getTrades()) {
				if (rec.isInSample()) {
					inSampleTrades.add(rec);
				}
			}

			this.inSampleTrades = inSampleTrades;
		}

		return inSampleTrades;
	}
	
	private void updateTradeDataWithTargetStopOptimizationData(Collection<TradeRecord> allTrades) {
		System.out.println("Updating In Sample Trade data with Target/Stop optimization results data...");

		Map<Integer, TradeRecord> tradeMap = new HashMap<>();
		for (TradeRecord rec : allTrades) {
			tradeMap.put(rec.getId(), rec);
		}
		
		Set<TargetStopStrategyStats> allStats = new HashSet<>();
		allStats.addAll(maxTargetStopPf.values());
		allStats.addAll(maxTargetStopWin.values());
		allStats.addAll(maxTargetStopPfWin.values());
		
		for (TargetStopStrategyStats stat : allStats) {
			for (TargetStopInfo info : stat.getInSampleTargetStopInfo()) {
				tradeMap.get(info.getTradeId()).getTargetStopInfo().add(info);
				tradeMap.get(info.getTradeId()).getTargetStopInfoMap().put(info.getTargetStopComboResult().getTargetStopComboResultId(), info);
			}
			for (TargetStopInfo info : stat.getOutOfSampleTargetStopInfo()) {
				tradeMap.get(info.getTradeId()).getTargetStopInfo().add(info);
				tradeMap.get(info.getTradeId()).getTargetStopInfoMap().put(info.getTargetStopComboResult().getTargetStopComboResultId(), info);
			}
			for (TargetStopInfo info : stat.getFinalTargetStopInfo()) {
				tradeMap.get(info.getTradeId()).getTargetStopInfo().add(info);
				tradeMap.get(info.getTradeId()).getTargetStopInfoMap().put(info.getTargetStopComboResult().getTargetStopComboResultId(), info);
			}
		}
		
		System.out.println("Finished updating In Sample Trade data with Target/Stop optimization results data.");
	}
	
	public OptimizationResults optimizeStrat(TradeData data, boolean optimizeTargetStop, Set<String> preOptimizationColumns) throws InterruptedException {
		List<TradeRecord> inSampleTrades = getInSampleTrades(data);
		
		//////testing
		//for(TradeRecord r : inSampleTrades) {
		//	Utility.getProfitToLossVolumeRatio(r);
		//}
		///////////
		
		this.optimizeTargetStop = optimizeTargetStop;
		
		if (optimizeTargetStop) {
			optimizeTargetStop(data);
		}

		TradeData inSampleData = new TradeData(inSampleTrades, data.getRunParam(), data.getFilters(), data.getRanges(), data.isShort());
		inSampleData.setStrategyName(data.getStrategyName());
		
		List<String> allColumns = new ArrayList<String>(preOptimizationColumns != null && !preOptimizationColumns.isEmpty() ? preOptimizationColumns : inSampleData.getDistinctColumns());
		Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(allColumns.size(), optimizeCount);
		int combinationsSize = (int)CombinatoricsUtils.binomialCoefficientDouble(allColumns.size(), optimizeCount);
		
		System.out.println("combinationsSize = " + combinationsSize);
		if (preOptimizationColumns != null) {
			System.out.println("Pre-Optimization columns size: " + preOptimizationColumns.size());
		}
		
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREADS);
		
		startMillis = System.currentTimeMillis();
		int count = 0;
		while (iterator.hasNext()) {
			final int[] combination = iterator.next();
			
			List<String> columns = new ArrayList<String>();
			for (int i : combination) {
				columns.add(allColumns.get(i));
			}
			
			count++;
			OptimizerThread thread = new OptimizerThread(count, combinationsSize, columns, inSampleData);
			executor.execute(thread);
		}
		
		executor.shutdown();
		executor.awaitTermination(1000, TimeUnit.MINUTES);
		executor.shutdownNow();
		
		Thread.sleep(500);
		
		List<StratStats> allStratsList = new ArrayList<StratStats>();
		
		List<StratStats> maxPfList = new ArrayList<StratStats>();
		List<StratStats> maxPfWinList = new ArrayList<StratStats>();
		List<StratStats> maxPfWinTradesList = new ArrayList<StratStats>();
		List<StratStats> maxWinList = new ArrayList<StratStats>();
		List<StratStats> maxWinTradesList = new ArrayList<StratStats>();
		
		for (Entry<Double, StratStats> entry : maxPf.entrySet()) {
			maxPfList.add(entry.getValue());
			allStratsList.add(entry.getValue());
		}
		
		for (Entry<Double, StratStats> entry : maxPfWin.entrySet()) {
			maxPfWinList.add(entry.getValue());
			allStratsList.add(entry.getValue());
		}
		
		for (Entry<Double, StratStats> entry : maxPfWinTrades.entrySet()) {
			maxPfWinTradesList.add(entry.getValue());
			allStratsList.add(entry.getValue());
		}
		
		for (Entry<Double, StratStats> entry : maxWin.entrySet()) {
			maxWinList.add(entry.getValue());
			allStratsList.add(entry.getValue());
		}
		
		for (Entry<Double, StratStats> entry : maxWinTrades.entrySet()) {
			maxWinTradesList.add(entry.getValue());
			allStratsList.add(entry.getValue());
		}
		
		System.out.println("\nIn Sample Optimization counts matching minimum criteria: ");
		System.out.println("maxPf strategy count: " + maxPf.size());
		System.out.println("maxPfWin strategy count: " + maxPfWin.size());
		System.out.println("maxPfWinTrades strategy count: " + maxPfWinTrades.size());
		System.out.println("maxWin strategy count: " + maxWin.size());
		System.out.println("maxWinTrades strategy count: " + maxWinTrades.size() + "\n");
		
		/*
		 * After optimizing over In Sample range. Apply the optimization parameters
		 * to the overall range (In Sample & Out of Sample) to get the full trade list,
		 * and set in the strategy object. Then calculate detailed statistics on overall data.
		 */
		for (StratStats stat : allStratsList) {
			TradeData overallSubData = null;
			for (FilterRange range : stat.getFilters()) {
				if (overallSubData == null) {
					overallSubData = Utility.getSubTradeData(data, range);
				} else {
					overallSubData = Utility.getSubTradeData(overallSubData, range);
				}
			}

			stat.setTradeData(overallSubData);
			stat.calculateDetailedStatistics(percentOfDaysForInSampleTest);
		}
		
		OptimizationResults results = new OptimizationResults();
		
		/*
		 * The lists will now be ordered based on the overall data and not just in sample.
		 */
		sortBasedOnOverallStat(maxPfList, "PF");
		sortBasedOnOverallStat(maxPfWinList, "PF_WIN");
		sortBasedOnOverallStat(maxPfWinTradesList, "PF_WIN_TRADE");
		sortBasedOnOverallStat(maxWinList, "WIN");
		sortBasedOnOverallStat(maxWinTradesList, "WIN_TRADE");
		
		results.setMaxPf(maxPfList);
		results.setMaxPfWin(maxPfWinList);
		results.setMaxPfWinTrades(maxPfWinTradesList);
		results.setMaxWin(maxWinList);
		results.setMaxWinTrades(maxWinTradesList);
		
		results.setMaxPfByCol(getOverallMaxStatByCol("PF", maxPfList));
		results.setMaxWinByCol(getOverallMaxStatByCol("WIN", maxWinList));
		results.setMaxPfWinByCol(getOverallMaxStatByCol("PF_WIN", maxPfWinList));
		results.setMaxPfWinTradesByCol(getOverallMaxStatByCol("PF_WIN_TRADE", maxPfWinTradesList));
		results.setMaxWinTradesByCol(getOverallMaxStatByCol("WIN_TRADE", maxWinTradesList));
		
		results.setMaxQIndxByCol(getOverallMaxStatByCol("Q_INDEX", allStratsList));
		
		List<TIRunParameters> runParam = new ArrayList<TIRunParameters>();
		runParam.add(data.getRunParam());
		
		results.setRunParam(runParam);
		results.setStrategyName(data.getStrategyName());
		
		results.calculateAbsMaximums();
		
		counter = 0;

		return results;
	}
	
	private static List<StratStats> getOverallMaxStatByCol(String type, List<StratStats> stats) {
		Map<String, StratStats> map = new HashMap<>();

		for (StratStats stat : stats) {
			List<FilterRange> filters = stat.getFilters();
			
			List<String> cols = new ArrayList<>();
			if (filters.size() > 1) {
				for (FilterRange r : filters) {
					cols.add(r.getVar());
				}
				Collections.sort(cols);
			} else {
				cols.add(filters.get(0).getVar());
			}
			
			StringBuilder builder = new StringBuilder();
			for (String col : cols) {
				if (builder.toString().isEmpty()) {
					builder.append(col);
				} else {
					builder.append(", ").append(col);
				}
			}
			
			String key = builder.toString();

			if (map.get(key) != null) {
				StratStats s = map.get(key);
				if (("PF".equals(type) && s.getOverallPf() < stat.getOverallPf())
						|| ("WIN".equals(type) && s.getOverallWinRate() < stat.getOverallWinRate())
						|| ("PF_WIN".equals(type) && s.getOverallPf() * s.getOverallWinRate() < stat.getOverallPf() * stat.getOverallWinRate())
						|| ("PF_WIN_TRADE".equals(type) && s.getOverallPf() * s.getOverallWinRate() * s.getOverallTradeCount() < stat.getOverallPf()
								* stat.getOverallWinRate() * stat.getOverallTradeCount())
						|| ("WIN_TRADE".equals(type)
								&& s.getOverallWinRate() * s.getOverallTradeCount() < stat.getOverallWinRate() * stat.getOverallTradeCount())
						|| ("Q_INDEX".equals(type) && s.getQualityIndex() < stat.getQualityIndex())) {
					map.put(key, stat);
				}
			} else {
				map.put(key, stat);
			}
		}
		
		List<StratStats> list = new ArrayList<>(map.values());
		sortBasedOnOverallStat(list, type);

		return list;
	}
	
	private static void sortBasedOnOverallStat(List<StratStats> list, String type) {		
		Collections.sort(list, new Comparator<StratStats>() {
			public int compare(StratStats obj1, StratStats obj2) {
				double val1 = 0.0;
				double val2 = 0.0;

				if ("PF".equals(type)) {
					val1 = obj1.getOverallPf();
					val2 = obj2.getOverallPf();
				} else if ("WIN".equals(type)) {
					val1 = obj1.getOverallWinRate();
					val2 = obj2.getOverallWinRate();
				} else if ("PF_WIN".equals(type)) {
					val1 = obj1.getOverallPf() * obj1.getOverallWinRate();
					val2 = obj2.getOverallPf() * obj2.getOverallWinRate();
				} else if ("PF_WIN_TRADE".equals(type)) {
					val1 = obj1.getOverallPf() * obj1.getOverallWinRate() * obj1.getOverallTradeCount();
					val2 = obj2.getOverallPf() * obj2.getOverallWinRate() * obj2.getOverallTradeCount();
				} else if ("WIN_TRADE".equals(type)) {
					val1 = obj1.getOverallWinRate() * obj1.getOverallTradeCount();
					val2 = obj2.getOverallWinRate() * obj2.getOverallTradeCount();
				} else if ("Q_INDEX".equals(type)) {
					val1 = obj1.getQualityIndex();
					val2 = obj2.getQualityIndex();
				}

				val1 = Double.isNaN(val1) ? 0 : val1;
				val2 = Double.isNaN(val2) ? 0 : val2;
				
				return (val1 > val2 ? 1 : val1 == val2 ? 0 : -1);
			}
		});
	}
	
	private void optimizeStrat(int currentColumn, String nextColumn, TreeSet<Double> nextDiscreteValues, List<String> columns, TradeData data) {
		String column = nextColumn;
		TreeSet<Double> values = nextDiscreteValues;
		
		if (values == null) {
			column = columns.get(currentColumn);
			values = getDiscreteValues(discreteCount, column, data);
		}
		
		TreeSet<Double> nextNextValues = null;
		String nextNextColumn = null;
		if (currentColumn < optimizeCount - 1) {
			nextNextColumn = columns.get(currentColumn + 1);
			nextNextValues = getDiscreteValues(discreteCount, nextNextColumn, data);
		}
		
		Iterator<Double> aIter = values.iterator();
		Iterator<Double> dIter = values.descendingIterator();
		
		List<Double> dList = new ArrayList<Double>();
		while (dIter.hasNext()) {
			dList.add(dIter.next());
		}

		while (aIter.hasNext()) {
			Double min = aIter.next();
			boolean firstLoop = true;
			boolean tooFewTrades = false;
			for (Double max : dList) {
				if (max > min) {
					FilterRange filter = new FilterRange(column, min, max);
					
					TradeData sub = Utility.getSubTradeData(data, filter);

					if (sub.getTrades().size() <= minTrades) {
						tooFewTrades = true;
						break;
					}

					firstLoop = false;
					
					int id = sub.getUniqueId();
					if (!lookup.contains(id)) {
						lookup.add(id);
					} else {
						continue;
					}
					
					if (this.optimizeTargetStop) {
						updateStatsWithTargetStops(sub);
					} else {
						updateStats(sub);
					}

					if (nextNextValues != null) {
						optimizeStrat(currentColumn + 1, nextNextColumn, nextNextValues, columns, sub);
					}
				}
			}
			
			if (firstLoop && tooFewTrades) {
				break;
			}
		}
	}
	
	private TreeSet<Double> getDiscreteValues(int limit, String column, TradeData data) {
		TreeSet<Double> discreteValues = new TreeSet<>();
		for (TradeRecord t : data.getTrades()) {
			Double value = t.getData().get(column);
			if (value != null) {
				discreteValues.add(value);
			}
		}

		if (discreteValues.size() <= limit) {
			return discreteValues;
		}

		TreeSet<Double> limitedValues = new TreeSet<Double>();
		
		double min = discreteValues.first();
		double max = discreteValues.last();
		double diff = max - min;
		double incr = diff / limit;

		for (int i = 0; i < limit; i++) {
			if (i == limit - 1) {
				limitedValues.add(max);
			} else {
				limitedValues.add(min + (incr * i));
			}
		}

		return limitedValues;
	}

	private void updateStats(TradeData sub) {
		StratStats stats = sub.getStatistics();

		if (stats.getWinRate() <= minWinRate || stats.getPf() <= minPf || stats.getPercentDaysTraded() <= minPercentOfDaysTraded) {
			return;
		}

		addToStatsMap(maxPf, stats, stats.getPf());
		
		double pfWin = stats.getPf()*stats.getWinRate();
		addToStatsMap(maxPfWin, stats, pfWin);

		double pfWinTrades = stats.getPf()*stats.getWinRate()*stats.getTradeCount();
		addToStatsMap(maxPfWinTrades, stats, pfWinTrades);
		
		addToStatsMap(maxWin, stats, stats.getWinRate());

		double winTrades = stats.getWinRate()*stats.getTradeCount();
		addToStatsMap(maxWinTrades, stats, winTrades);
	}
	
	private void updateStatsWithTargetStops(TradeData sub) {
		List<StratStats> statsList = sub.getStatisticsWithTargetStopOptimization();

		for (StratStats stats : statsList) {
			if (stats.getWinRate() <= minWinRate || stats.getPf() <= minPf || stats.getPercentDaysTraded() <= minPercentOfDaysTraded) {
				continue;
			}
	
			addToStatsMap(maxPf, stats, stats.getPf());
			
			double pfWin = stats.getPf()*stats.getWinRate();
			addToStatsMap(maxPfWin, stats, pfWin);

			double pfWinTrades = stats.getPf()*stats.getWinRate()*stats.getTradeCount();
			addToStatsMap(maxPfWinTrades, stats, pfWinTrades);
			
			addToStatsMap(maxWin, stats, stats.getWinRate());

			double winTrades = stats.getWinRate()*stats.getTradeCount();
			addToStatsMap(maxWinTrades, stats, winTrades);
		}
	}
	
	public static OptimizationResults bestOfTheBest(List<OptimizationResults> results) {		
		ConcurrentSkipListMap<Double, StratStats> maxPf = new ConcurrentSkipListMap<Double, StratStats>();
		ConcurrentSkipListMap<Double, StratStats> maxPfWin = new ConcurrentSkipListMap<Double, StratStats>();
		ConcurrentSkipListMap<Double, StratStats> maxPfWinTrades = new ConcurrentSkipListMap<Double, StratStats>();
		ConcurrentSkipListMap<Double, StratStats> maxWin = new ConcurrentSkipListMap<Double, StratStats>();
		ConcurrentSkipListMap<Double, StratStats> maxWinTrades = new ConcurrentSkipListMap<Double, StratStats>();
		
		List<TIRunParameters> runParam = new ArrayList<>();
		List<StratStats> allStats = new ArrayList<>();
		for (OptimizationResults r : results) {
			for (StratStats stats : r.getMaxPf()) {
				allStats.add(stats);
			}
			for (StratStats stats : r.getMaxPfWin()) {
				allStats.add(stats);
			}
			for (StratStats stats : r.getMaxPfWinTrades()) {
				allStats.add(stats);
			}
			for (StratStats stats : r.getMaxWin()) {
				allStats.add(stats);
			}
			for (StratStats stats : r.getMaxWinTrades()) {
				allStats.add(stats);
			}
			
			runParam.addAll(r.getRunParam());
		}
		
		for (StratStats stats : allStats) {
			if (maxPf.size() < MAX_STAT_COUNT) {
				maxPf.put(stats.getOverallPf(), stats);
			} else if (stats.getOverallPf() > maxPf.firstKey()) {
				maxPf.remove(maxPf.firstKey());
				maxPf.put(stats.getOverallPf(), stats);
			}
			
			double pfWin = stats.getOverallPf()*stats.getOverallWinRate();
			
			if (maxPfWin.size() < MAX_STAT_COUNT) {
				maxPfWin.put(pfWin, stats);
			} else if (pfWin > maxPfWin.firstKey()) {
				maxPfWin.remove(maxPfWin.firstKey());
				maxPfWin.put(pfWin, stats);
			}
			
			double pfWinTrades = stats.getPf()*stats.getOverallWinRate()*stats.getOverallTradeCount();
			
			if (maxPfWinTrades.size() < MAX_STAT_COUNT) {
				maxPfWinTrades.put(pfWinTrades, stats);
			} else if (pfWinTrades > maxPfWinTrades.firstKey()) {
				maxPfWinTrades.remove(maxPfWinTrades.firstKey());
				maxPfWinTrades.put(pfWinTrades, stats);
			}
			
			if (maxWin.size() < MAX_STAT_COUNT) {
				maxWin.put(stats.getOverallWinRate(), stats);
			} else if (stats.getOverallWinRate() > maxWin.firstKey()) {
				maxWin.remove(maxWin.firstKey());
				maxWin.put(stats.getOverallWinRate(), stats);
			}
			
			double winTrades = stats.getOverallWinRate()*stats.getOverallTradeCount();
			
			if (maxWinTrades.size() < MAX_STAT_COUNT) {
				maxWinTrades.put(winTrades, stats);
			} else if (winTrades > maxWinTrades.firstKey()) {
				maxWinTrades.remove(maxWinTrades.firstKey());
				maxWinTrades.put(winTrades, stats);
			}
		}
		
		List<StratStats> maxPfList = new ArrayList<StratStats>();
		List<StratStats> maxPfWinList = new ArrayList<StratStats>();
		List<StratStats> maxPfWinTradesList = new ArrayList<StratStats>();
		List<StratStats> maxWinList = new ArrayList<StratStats>();
		List<StratStats> maxWinTradesList = new ArrayList<StratStats>();
		
		for (Entry<Double, StratStats> entry : maxPf.entrySet()) {
			maxPfList.add(entry.getValue());
		}
		
		for (Entry<Double, StratStats> entry : maxPfWin.entrySet()) {
			maxPfWinList.add(entry.getValue());
		}
		
		for (Entry<Double, StratStats> entry : maxPfWinTrades.entrySet()) {
			maxPfWinTradesList.add(entry.getValue());
		}
		
		for (Entry<Double, StratStats> entry : maxWin.entrySet()) {
			maxWinList.add(entry.getValue());
		}
		
		for (Entry<Double, StratStats> entry : maxWinTrades.entrySet()) {
			maxWinTradesList.add(entry.getValue());
		}
		
		OptimizationResults bestOfTheBest = new OptimizationResults();
		
		bestOfTheBest.setMaxPf(maxPfList);
		bestOfTheBest.setMaxPfWin(maxPfWinList);
		bestOfTheBest.setMaxPfWinTrades(maxPfWinTradesList);
		bestOfTheBest.setMaxWin(maxWinList);
		bestOfTheBest.setMaxWinTrades(maxWinTradesList);
		
		bestOfTheBest.setMaxPfByCol(getOverallMaxStatByCol("PF", maxPfList));
		bestOfTheBest.setMaxWinByCol(getOverallMaxStatByCol("WIN", maxWinList));
		bestOfTheBest.setMaxPfWinByCol(getOverallMaxStatByCol("PF_WIN", maxPfWinList));
		bestOfTheBest.setMaxPfWinTradesByCol(getOverallMaxStatByCol("PF_WIN_TRADE", maxPfWinTradesList));
		bestOfTheBest.setMaxWinTradesByCol(getOverallMaxStatByCol("WIN_TRADE", maxWinTradesList));

		bestOfTheBest.setRunParam(runParam);
		
		return bestOfTheBest;
	}

	private void optimizeTargetStop(TradeData data, TargetStopComboStrategyResult targetStopComboResult, 
			double minPf, double minWinRate, double minPercentOfTargetsHit,	double minPercentOfStopsHit) {
					
		Map<Integer, Double> targets = targetStopComboResult.getTargetResult().getTargetStopsMap();
		Map<Integer, Double> stops = targetStopComboResult.getStopLossResult().getTargetStopsMap();
		
		int targetToStopNotMetCount = 0;
		
		List<TargetStopInfo> inSampleTargetStopInfo = new ArrayList<>();
		List<TargetStopInfo> outOfSampleTargetStopInfo = new ArrayList<>();
		List<TargetStopInfo> finalSampleTargetStopInfo = new ArrayList<>();
		for (TradeRecord rec : data.getTrades()) {
			double target = targets.get(rec.getId());
			double stop = stops.get(rec.getId());

			double targetDiff = Math.abs(target - rec.getEntryPrice());
			double stopDiff = Math.abs(rec.getEntryPrice() - stop);
			double targetToStop = targetDiff/stopDiff;
			
			if (this.targetToStop != 0.0 && targetToStop < this.targetToStop) {
				targetToStopNotMetCount++;
				double percentNotMet = targetToStopNotMetCount * 100.0 / data.getTrades().size();
				// Do not accept if TARGET_TO_STOP_RATIO_ANOMALY_PERCENT or more of trades did not meet target to stop ratio criteria.
				if (percentNotMet > TARGET_TO_STOP_RATIO_ANOMALY_PERCENT) {
					return;
				}
			}
			
			TargetStopInfo info = Utility.getTargetStopInfo(rec, target, stop, targetStopComboResult, data.isShort());
			
			if (rec.isOutOfSample()) {
				outOfSampleTargetStopInfo.add(info);
			} else if (rec.isInSample()) {
				inSampleTargetStopInfo.add(info);
			} else if(rec.isFinalTestSample()) {
				finalSampleTargetStopInfo.add(info);
			}
		}

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

		double pf = grossProfitPercent/grossLossPercent;
		double winRate = (Double.valueOf(winCount)/inSampleTargetStopInfo.size()*1.0)*100.0;
		double percentTargetsHit = targetsHit*1.0/inSampleTargetStopInfo.size();
		double percentStopsHit = stopsHit*1.0/inSampleTargetStopInfo.size();
		
		if (pf >= minPf && winRate >= minWinRate
				&& percentTargetsHit * 100.0 >= minPercentOfTargetsHit
				&& percentStopsHit * 100.0 >= minPercentOfStopsHit) {
			
			TargetStopStrategyStats stats = new TargetStopStrategyStats();
			stats.setTargetStopComboStrategyResult(targetStopComboResult);
			stats.setTradeData(data);
			stats.setInSampleTargetStopInfo(inSampleTargetStopInfo);
			stats.setOutOfSampleTargetStopInfo(outOfSampleTargetStopInfo);
			stats.setFinalTargetStopInfo(finalSampleTargetStopInfo);
			stats.setFilters(data.getFilters());
			stats.setTradeCount(winCount + loseCount);
			stats.setWinCount(winCount);
			stats.setLoseCount(loseCount);
			stats.setPf(pf);
			stats.setAvgWinner(grossProfitPercent/winCount*1.0);
			stats.setAvgLoser(grossLossPercent/loseCount*1.0);
			stats.setWinRate(winRate);
			stats.setPercentTargetsHit(percentTargetsHit);
			stats.setPercentStopsHit(percentStopsHit);
			stats.setPercentDaysTraded((distinctDaysOfTrading.size()*1.0/data.getRanges().getInSampleDataDaysRange()*1.0)*100);
			
			addToTargetStopStatsMap(maxTargetStopPf, stats, stats.getPf());
			addToTargetStopStatsMap(maxTargetStopWin, stats, stats.getWinRate());

			double pfWin = stats.getWinRate()*stats.getPf();

			addToTargetStopStatsMap(maxTargetStopPfWin, stats, pfWin);
		}
	}

	
	public int getMinTrades() {
		return minTrades;
	}


	public void setMinTrades(int minTrades) {
		this.minTrades = minTrades;
	}


	public int getOptimizeCount() {
		return optimizeCount;
	}


	public void setOptimizeCount(int optimizeCount) {
		this.optimizeCount = optimizeCount;
	}


	public int getMinWinRate() {
		return minWinRate;
	}


	public void setMinWinRate(int minWinRate) {
		this.minWinRate = minWinRate;
	}
}
