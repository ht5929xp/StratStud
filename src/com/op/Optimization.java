package com.op;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.op.bo.ExcelBuilder;
import com.op.bo.ExecutionTimeTracker;
import com.op.bo.FilterRange;
import com.op.bo.OptimizationResults;
import com.op.bo.Optimizer;
import com.op.bo.PathResources;
import com.op.bo.PreFilter;
import com.op.bo.PriceLevels;
import com.op.bo.RunConfiguration;
import com.op.bo.RunStrategyAndRecordCount;
import com.op.bo.TargetStopRunConfiguration;
import com.op.bo.TradeData;
import com.op.bo.Utility;
import com.op.bo.RunConfiguration.RunStrategies;
import com.op.bo.TargetStopRunConfiguration.TargetStopRunStrategies;

public class Optimization {
	
	/*
	 * Notes:
	 * If we set the target profit very low, then the trades which exit near instantly are not quality trades.
	 * Avg time in a trade/chart
	 * what about inverse of the range - for example test to exclude the range
	 * optimize # days, apply to remaining days, calculate metrics of comparison
	 * multiple group comparisons, such as different targets and stops
	 * Compare spread to target profit, if too close, strategy is not accurate
	 * MAE and MFE
	 * Alert type
	 * part of quality is to have avg win much larger than avg loser
	 * part of quality is to have more trades hitting target vs timing out
	 * part of quality stop loss being too far and essentially useless
	 * part of quality not many sold in 1 minute
	 * combine strategies
	 * sellers found at 3:45?
	 * whiplash in the morning possibly find low entry price?
	 * formula editor round price
	 * Buy order control the decimal to 0.01. create formula
	 * Sell/stop/limit orders control decimals to 0.01. create formula
	 * log file seems to update instantly and can read while it writes
	 * issue with price contract does not conform was resolved with formula update to 2 decimals
	 * MAE and MFE show for the duration of the trade - so best to do timeout to see the whole picture, then decide on profit target/stop
	 * Run analysis based on parameters to find best target and stop, ex. using wiggle, ATR, smart stop, 2/5/15/30 minute ranges etc.
	 * Dynamic time to stay in a trade? Based on ATR?? Possibly use advanced exit.
	 * Run target/stop optimization, create quick map by alertID and taregt/stop scheme, while optimizing read target/stop stats from this map and calculate stats
	 * Add supplemental data to the target/stop analysis to get MAE and MFE throughout the day, for example timeout at 12PM and at 3PM, etc.
	 * add # of targets and stops hit to the stats results
	 * For MAE MFE pass data for timeout in 1 minute after
	 * Avoiding stop losses at round numbers
	 * Try testing different markets
	 * Work on improving accuracy when MAE MFE is null and using getMfe or getMae methods.
	 * Need to update to account for single super large winners sckewing results
	 * For target/stop in the data itself need to update get stats to apply same logic for < 1 minute trades as target stop
	 * The min max trade dates for in sample, out of sample, final may not be accurate.
	 * Think about adding 1 sided optimization min or max and not both for improved concept.
	 * Filter optimization before target stop is the way
	 * Get strategy which has most potential if target is set properly - or advanced exit technique used - by considering MFE the exit.
	 * Calculate volume of time in profit vs volume of time in loss with more weight to time closer to entry
	 * 
	 * Strategy ideas:
	 * buy at beginning of day and sell at end ideas
	 * opening range break outs - check email for alert config
	 * gap down and recovered up to the 10 SMA but was not able to go through.
	 * Score composite rating gives strong stocks.
	 * Look for strong stocks, find a crash in the market (Down some percentage), buy strong stocks which also crashed, hold for days to come.
	 * Extreme drop intra day, then buy the dip (Abnormal sell off), example AEHR 7/16/2021 11:45 AM
	 * Bookmap
	 * Big gappers low floaters today, some may gap up tomorrow
	 * For news stocks, try to buy or sell near end of day
	 * Scalability (Is the strategy scalable with more cash)
	*/

	//Entry Variables:
	private static final double minAvgNumberOfTradesPerDay = 1;
	private static final int minWinRate = 50;
	private static final double minPf = 2.0;
	private static final int minPercentOfDaysTraded = 40; // At least this % of days have trades. (Avoid strategies where all trades are concentrated in few days - circumstantial)
	
	private static final int optimizeCount = 1; // Number of columns for the filter to optimize for, the larger the number of columns the more time consuming it will be.
	private static final int discreteCount = 100; // The number of discrete values for each column to use, the lower the faster.
	private static final int percentOfDaysForInSampleTest = 50;
	private static final int percentOfDaysForFinalTest = 15;
	
	private static final boolean optimizeTargetStop = false; // Run strategy optimization while optimizing Targets and Stops. If true, strategy input should not have target/stops.
	private static final boolean onlyOptimizeTargetStop = false;// Do NOT run strategy optimization and only run target stop optimization on provided strategy data.
	
	private static final double targetToStop = 2; // How much target is greater/less than stop. Value of 1 means target is greater than or equal to stop, Ex. 2 means target is atleast twice stop. 0 Ignores this property.
												  // accepts up to 5% of original trades failing to meet this criteria to account for anomaly cases.
	private static final double minStopLoss = 0.03; // Minimum stop loss in $, 0 for no restriction.
	private static final double minProfitTarget = 0.03; // Minimum target in $, 0 for no restriction.
	
	private static final boolean usePreOptimizationMethod = false; // This runs quick 1 level no TS optimization and gets set of columns, and then limits full TS optimization to only use those columns.
	
	//Target Stop Optimization Parameters:
	private static final double tsMinPf = 0;
	private static final double tsMinWinRate = 0;
	private static final int minPercentOfTargetsHit = 0;
	private static final int minPercentOfStopsHit = 0;
	private static final TargetStopRunStrategies targetStopRunStrategy = TargetStopRunStrategies.ALL;
	
	//Predefined filter
	private static List<FilterRange> preFilter;
	static {
		//FilterRange[] array = {
		//						new FilterRange("Dow Change 30 Minute (%) [Dia30]", 0, 1.073),
		//						new FilterRange("15 Minute RSI (0 - 100) [RSI15]", 72, 78)
		//					  };
		//preFilter = Arrays.asList(array);
	}
	
	private static List<RunStrategyAndRecordCount> completedResultsLookup;
	
	public static void main (String[] args) throws IOException, ParseException, InterruptedException {
		boolean singleRun = isSingleRun();

		List<RunConfiguration> runParamList = null;
		List<File> dataDirs = null;
		if (!singleRun) {
			System.out.println("*** Multi Run ***");
			dataDirs = Utility.getDataDirectories();
			loadResultsLookupMap();
			runParamList = RunConfiguration.getAllRunConfigurations();
		} else {
			System.out.println("*** Single Run ***");
			dataDirs = new ArrayList<>();
			dataDirs.add(new File(PathResources.SINGLE_RUN_DATA_PATH));

			RunConfiguration param = new RunConfiguration(RunStrategies.Adhoc, minAvgNumberOfTradesPerDay, minWinRate,
					minPf, minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest,
					percentOfDaysForFinalTest, optimizeTargetStop, onlyOptimizeTargetStop,
					new TargetStopRunConfiguration(tsMinPf, tsMinWinRate, minPercentOfTargetsHit, minPercentOfStopsHit,
							targetToStop, minStopLoss, minProfitTarget, targetStopRunStrategy), preFilter);
			
			runParamList = new ArrayList<>();
			runParamList.add(param);
		}

		for (File dir : dataDirs) {
			String strategyNameFromDir = dir.getName();
			System.out.println("\n RUNNING RUN CONFIGURATIONS FOR BASE DATA FOR [" + strategyNameFromDir + "] ==> \n");
			
			Map<String, PriceLevels> priceLevelsData = Utility.extractPriceLevelData(dir);
			
			for (RunConfiguration param : runParamList) {
				List<TradeData> tradeData = Utility.loadTradeData(dir, param.getPercentOfDaysForInSampleTest(), param.getPercentOfDaysForFinalTest(), priceLevelsData, param.getPreFilter());
				runOptimization(param, tradeData);
			}
			System.out.println("\n *** COMPLETED *** \n");
		}
		
		if (!singleRun) {
			Map<String, List<PreFilter>> preFilterMap = RunConfiguration.getPreFilterMap();
			
			for (File dir : dataDirs) {
				String strategyNameFromDir = dir.getName();
				System.out.println("\n RUNNING RUN CONFIGURATIONS FOR PRE-FILTERED DATA FOR [" + strategyNameFromDir + "]==> \n");
				
				Map<String, PriceLevels> priceLevelsData = Utility.extractPriceLevelData(dir);
				
				List<PreFilter> preFilterList = preFilterMap.get(strategyNameFromDir);
				if (preFilterList != null && preFilterList.size() > 0) {
					for (PreFilter preFilter : preFilterList) {
						for (RunConfiguration param : runParamList) {
							param.setPreFilter(preFilter.getFilter());
							List<TradeData> tradeData = Utility.loadTradeData(dir, param.getPercentOfDaysForInSampleTest(),	param.getPercentOfDaysForFinalTest(), priceLevelsData, param.getPreFilter());
							runOptimization(param, tradeData);
						}
					}
				}
				System.out.println("\n *** COMPLETED *** \n");
			}
		}
	}

	private static void runOptimization(RunConfiguration param, List<TradeData> tradeData) throws InterruptedException {
		long startTimeMillis = System.currentTimeMillis();
		
		int totalDataRowCount = Utility.getTotalDataRowCount(tradeData);

		param.updateRunConditionsAfterDataLoad(tradeData.get(0).getRanges().getInSampleDataDaysRange(),	totalDataRowCount);

		String strategyName = Utility.getStrategyName(tradeData);
		if (!isAlreadyRun(param, strategyName)) {
			if (Utility.tradeDataContainsTargetStop(tradeData)
					&& (param.isOptimizeTargetStop() || param.isOnlyOptimizeTargetStop())) {
				return;
			}

			System.out.println("Minimum # of trades = " + param.getMinTrades() + " \n");
			
			System.out.println("Run Strategy = " + param.getRunStrategy() + " \n");
	
			List<OptimizationResults> results = new ArrayList<>();
			for (TradeData data : tradeData) {
				Set<String> preOptimizationColumns = null;
				if (param.isOptimizeTargetStop() && usePreOptimizationMethod) {
					System.out.println("Runnning Pre-Optimization...\r\n");
					Optimizer preOptimizer = new Optimizer(param);
					preOptimizer.setOptimizeCount(1);
					OptimizationResults preOptimization = preOptimizer.optimizeStrat(data, false, null);
					preOptimizationColumns = preOptimization.getUniqueColumnsSet();
				}
				Optimizer p = new Optimizer(param);
				OptimizationResults r = param.isOnlyOptimizeTargetStop() ? p.optimizeTargetStop(data) : p.optimizeStrat(data, param.isOptimizeTargetStop(), preOptimizationColumns);
				results.add(r);
				ExcelBuilder.buildOptimizationResultsExcel(data.getGroupName(), data.getStrategyName(), r, r.getRunParam(), param, data.getWtiFile(), param.isOnlyOptimizeTargetStop(), startTimeMillis);
			}
			
			if (results.size() > 1) {
				OptimizationResults bestOfTheBest = Optimizer.bestOfTheBest(results);
				ExcelBuilder.buildOptimizationResultsExcel(tradeData.get(0).getGroupName(), tradeData.get(0).getGroupName() + " (Best)", bestOfTheBest, bestOfTheBest.getRunParam(), param, tradeData.get(0).getWtiFile(), param.isOnlyOptimizeTargetStop(), startTimeMillis);
			}
		}
	}

	private static boolean isAlreadyRun(RunConfiguration runParam, String strategyName) {
		if (completedResultsLookup != null && runParam.getTotalTradeDataRowCount() > 0
				&& runParam.getRunStrategy() != null && !runParam.getRunStrategy().equals(RunStrategies.Adhoc)) {
			RunStrategyAndRecordCount rec = new RunStrategyAndRecordCount(strategyName,
					String.valueOf(runParam.getRunStrategy()), runParam.getTotalTradeDataRowCount());
			return completedResultsLookup.contains(rec);
		}

		return false;
	}
	
	/**
	 * Returns a map of all results executed and completed in the past. This includes the run strategy along with the total
	 * row count of the data inwhich it ran on, as well as the strategy name. This is used to prevent the process from executing 
	 * the same optimization for the same data, strategy, and run conditions.
	 * @return
	 */
	private static void loadResultsLookupMap() {
		ExecutionTimeTracker ett = new ExecutionTimeTracker("Load Results Lookup Map");
		
		completedResultsLookup = new ArrayList<>();

		File resultsDir = new File(PathResources.RESULTS_DIR_PATH);

		for (File resultStrategyDir : resultsDir.listFiles()) {
			if (resultStrategyDir.isDirectory()) {
				String strategyName = resultStrategyDir.getName();

				try {
					for (File runResultDir : resultStrategyDir.listFiles()) {
						if (runResultDir.isDirectory()) {
							String dirName = runResultDir.getName();
							if (dirName.contains("[") && dirName.contains("]")) {
								String runStrategy = dirName.split("\\[")[1].split("\\]")[0];

								for (File resultFile : runResultDir.listFiles()) {
									String fileName = resultFile.getName();
									if (!fileName.contains(".") && !resultFile.isDirectory()) {
										int totalTradeDataRowCount = Integer.parseInt(fileName);
										RunStrategyAndRecordCount result = new RunStrategyAndRecordCount(strategyName,
												runStrategy, totalTradeDataRowCount);
										completedResultsLookup.add(result);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Bad Directory Name");
				}
			}
		}
		
		ett.end();
	}

	/**
	 * If any data file is directly available under the DATA_PATH directory then it is a single run, otherwise a multi run.
	 * @return
	 */
	private static boolean isSingleRun() {
		boolean singleRun = false;
		
		File dataDir = new File(PathResources.SINGLE_RUN_DATA_PATH);
		File[] allFiles = dataDir.listFiles();

		for (File f : allFiles) {
			if (Utility.isDataFile(f) || Utility.isWTIFile(f)) {
				singleRun = true;
				break;
			}
		}
		
		return singleRun;
	}
}
