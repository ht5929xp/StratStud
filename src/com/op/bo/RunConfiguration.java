package com.op.bo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.op.bo.TargetStopRunConfiguration.TargetStopRunStrategies;

public class RunConfiguration {

	public static enum RunStrategies {
		Adhoc,
		Ideal_With_Target_Stop_V4,
		Ideal_With_Smart_Stop_V2,
		Ideal_Without_Target_Stop_V2,
		High_Win_Rate_High_Frequency_V1,
		High_Win_Rate_Low_Frequency_V4,
		High_Win_Rate_No_Restrictions_V1,
		High_Profit_Factor_Low_Win_Rate_Loose_Stop_V4,
		Few_High_Quality_Trades_V4,
		Optimize_Two_Columns_Without_Target_Stop_V2,
		Few_Good_Days_V1,
		Extreme_Scalper_V1
	}

	private String onlyForThisStrategy; //Only run this configuration for this strategy
	private RunStrategies runStrategy;
	
	private double minAvgNumberOfTradesPerDay;
	private int minWinRate;
	private double minPf;
	private int minPercentOfDaysTraded; // At least this % of days have trades. (Avoid strategies
															// where all trades are concentrated in few days -
															// circumstantial)

	private int optimizeCount; // Number of columns for the filter to optimize for, the larger the
												// number of columns the more time consuming it will be.
	private int discreteCount; // The number of discrete values for each column to use, the lower the
													// faster.
	private int percentOfDaysForInSampleTest;
	private int percentOfDaysForFinalTest;

	private boolean optimizeTargetStop; // Run strategy optimization while optimizing Targets and
																// Stops. If true, strategy input should not have
																// target/stops.
	private boolean onlyOptimizeTargetStop;// Do NOT run strategy optimization and only run target
																// stop optimization on provided strategy data.
	private int minTrades;
	
	private TargetStopRunConfiguration tsRunConfigurations;

	private int totalTradeDataRowCount;

	private List<FilterRange> preFilter;
	
	public void setOptimizeTargetStop(boolean optimizeTargetStop) {
		this.optimizeTargetStop = optimizeTargetStop;
	}

	public void setOnlyOptimizeTargetStop(boolean onlyOptimizeTargetStop) {
		this.onlyOptimizeTargetStop = onlyOptimizeTargetStop;
	}

	public RunConfiguration(RunStrategies runStrategy, double minAvgNumberOfTradesPerDay, int minWinRate, double minPf,
			int minPercentOfDaysTraded, int optimizeCount, int discreteCount, int percentOfDaysForInSampleTest, int percentOfDaysForFinalTest,
			boolean optimizeTargetStop, boolean onlyOptimizeTargetStop, TargetStopRunConfiguration tsRunConfigurations) {
		super();
		this.runStrategy = runStrategy;
		this.minAvgNumberOfTradesPerDay = minAvgNumberOfTradesPerDay;
		this.minWinRate = minWinRate;
		this.minPf = minPf;
		this.minPercentOfDaysTraded = minPercentOfDaysTraded;
		this.optimizeCount = optimizeCount;
		this.discreteCount = discreteCount;
		this.percentOfDaysForInSampleTest = percentOfDaysForInSampleTest;
		this.percentOfDaysForFinalTest = percentOfDaysForFinalTest;
		this.optimizeTargetStop = optimizeTargetStop;
		this.onlyOptimizeTargetStop = onlyOptimizeTargetStop;
		this.tsRunConfigurations = tsRunConfigurations;
	}
	
	public RunConfiguration(RunStrategies runStrategy, double minAvgNumberOfTradesPerDay, int minWinRate, double minPf,
			int minPercentOfDaysTraded, int optimizeCount, int discreteCount, int percentOfDaysForInSampleTest,
			int percentOfDaysForFinalTest, boolean optimizeTargetStop, boolean onlyOptimizeTargetStop,
			TargetStopRunConfiguration tsRunConfigurations, List<FilterRange> preFilter) {
		super();
		this.runStrategy = runStrategy;
		this.minAvgNumberOfTradesPerDay = minAvgNumberOfTradesPerDay;
		this.minWinRate = minWinRate;
		this.minPf = minPf;
		this.minPercentOfDaysTraded = minPercentOfDaysTraded;
		this.optimizeCount = optimizeCount;
		this.discreteCount = discreteCount;
		this.percentOfDaysForInSampleTest = percentOfDaysForInSampleTest;
		this.percentOfDaysForFinalTest = percentOfDaysForFinalTest;
		this.optimizeTargetStop = optimizeTargetStop;
		this.onlyOptimizeTargetStop = onlyOptimizeTargetStop;
		this.tsRunConfigurations = tsRunConfigurations;
		this.preFilter = preFilter;
	}
	
	public RunConfiguration(RunStrategies runStrategy, double minAvgNumberOfTradesPerDay, int minWinRate, double minPf,
			int minPercentOfDaysTraded, int optimizeCount, int discreteCount, int percentOfDaysForInSampleTest,
			int percentOfDaysForFinalTest, boolean optimizeTargetStop, boolean onlyOptimizeTargetStop,
			TargetStopRunConfiguration tsRunConfigurations, List<FilterRange> preFilter, String onlyForThisStrategy) {
		super();
		this.runStrategy = runStrategy;
		this.minAvgNumberOfTradesPerDay = minAvgNumberOfTradesPerDay;
		this.minWinRate = minWinRate;
		this.minPf = minPf;
		this.minPercentOfDaysTraded = minPercentOfDaysTraded;
		this.optimizeCount = optimizeCount;
		this.discreteCount = discreteCount;
		this.percentOfDaysForInSampleTest = percentOfDaysForInSampleTest;
		this.percentOfDaysForFinalTest = percentOfDaysForFinalTest;
		this.optimizeTargetStop = optimizeTargetStop;
		this.onlyOptimizeTargetStop = onlyOptimizeTargetStop;
		this.tsRunConfigurations = tsRunConfigurations;
		this.preFilter = preFilter;
		this.onlyForThisStrategy = onlyForThisStrategy;
	}

	public static List<RunConfiguration> getAllRunConfigurations() {
		List<RunConfiguration> runConfigs = new ArrayList<>();
		
		double minAvgNumberOfTradesPerDay = 1;
		int minWinRate = 50;
		double minPf = 2.0;
		int minPercentOfDaysTraded = 40; 
		
		int optimizeCount = 1;
		int discreteCount = 100;
		int percentOfDaysForInSampleTest = 50;
		int percentOfDaysForFinalTest = 15;
		
		boolean optimizeTargetStop = true; 
		boolean onlyOptimizeTargetStop = false;

		double targetToStop = 3;
		double minStopLoss = 0.03;
		double minProfitTarget = 0.02;

		TargetStopRunConfiguration tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss,	minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);

		RunConfiguration config1 = new RunConfiguration(RunStrategies.Ideal_With_Target_Stop_V4, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		minAvgNumberOfTradesPerDay = 1;
		minWinRate = 50;
		minPf = 2.0;
		minPercentOfDaysTraded = 40; 
		
		optimizeCount = 1;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = false; 
		onlyOptimizeTargetStop = false;
		
		targetToStop = 3;
		minStopLoss = 0.03;
		minProfitTarget = 0.02;
		
		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);
		
		RunConfiguration config2 = new RunConfiguration(RunStrategies.Ideal_Without_Target_Stop_V2, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		minAvgNumberOfTradesPerDay = 0.5;
		minWinRate = 75;
		minPf = 2.0;
		minPercentOfDaysTraded = 30; 
		
		optimizeCount = 1;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = true; 
		onlyOptimizeTargetStop = false;
		
		targetToStop = 3;
		minStopLoss = 0.03;
		minProfitTarget = 0.02;
		
		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);
		
		RunConfiguration config3 = new RunConfiguration(RunStrategies.High_Win_Rate_Low_Frequency_V4, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		minAvgNumberOfTradesPerDay = 2;
		minWinRate = 65;
		minPf = 2.0;
		minPercentOfDaysTraded = 25; 
		
		optimizeCount = 1;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = true; 
		onlyOptimizeTargetStop = false;
		
		targetToStop = 1;
		minStopLoss = 0.03;
		minProfitTarget = 0.02;
		
		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);
		
		RunConfiguration config7 = new RunConfiguration(RunStrategies.High_Win_Rate_High_Frequency_V1, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		minAvgNumberOfTradesPerDay = 0;
		minWinRate = 80;
		minPf = 2.0;
		minPercentOfDaysTraded = 30; 
		
		optimizeCount = 1;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = true; 
		onlyOptimizeTargetStop = false;
		
		targetToStop = 2;
		minStopLoss = 0.03;
		minProfitTarget = 0.02;
		
		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);
		
		RunConfiguration config5 = new RunConfiguration(RunStrategies.Few_High_Quality_Trades_V4, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		minAvgNumberOfTradesPerDay = 1;
		minWinRate = 40;
		minPf = 4.0;
		minPercentOfDaysTraded = 40; 
		
		optimizeCount = 1;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = true; 
		onlyOptimizeTargetStop = false;
		
		targetToStop = 3;
		minStopLoss = 0.05;
		minProfitTarget = 0.02;
		
		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);
		
		RunConfiguration config4 = new RunConfiguration(RunStrategies.High_Profit_Factor_Low_Win_Rate_Loose_Stop_V4, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		minAvgNumberOfTradesPerDay = 1;
		minWinRate = 40;
		minPf = 2.0;
		minPercentOfDaysTraded = 40; 
		
		optimizeCount = 2;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = false; 
		onlyOptimizeTargetStop = false;
		
		targetToStop = 2;
		minStopLoss = 0.05;
		minProfitTarget = 0.02;
		
		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);
		
		RunConfiguration config6 = new RunConfiguration(RunStrategies.Optimize_Two_Columns_Without_Target_Stop_V2, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);

		minAvgNumberOfTradesPerDay = 0.25;
		minWinRate = 60;
		minPf = 2.0;
		minPercentOfDaysTraded = 15; 
		
		optimizeCount = 1;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = true; 
		onlyOptimizeTargetStop = false;
		
		targetToStop = 1.5;
		minStopLoss = 0.05;
		minProfitTarget = 0.05;

		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);
		
		RunConfiguration config8 = new RunConfiguration(RunStrategies.Few_Good_Days_V1, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		minAvgNumberOfTradesPerDay = 1;
		minWinRate = 50;
		minPf = 2.0;
		minPercentOfDaysTraded = 40; 
		
		optimizeCount = 1;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = true; 
		onlyOptimizeTargetStop = false;

		targetToStop = 0;
		minStopLoss = 0.05;
		minProfitTarget = 0.05;

		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Any_Target_With_Smart_Stop);

		RunConfiguration config9 = new RunConfiguration(RunStrategies.Ideal_With_Smart_Stop_V2, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		minAvgNumberOfTradesPerDay = 0.1;
		minWinRate = 70;
		minPf = 2.0;
		minPercentOfDaysTraded = 0; 
		
		optimizeCount = 1;
		discreteCount = 100;
		percentOfDaysForInSampleTest = 50;
		percentOfDaysForFinalTest = 15;
		
		optimizeTargetStop = true; 
		onlyOptimizeTargetStop = false;
		
		targetToStop = 1;
		minStopLoss = 0.03;
		minProfitTarget = 0.03;
		
		tsRunConfigs = new TargetStopRunConfiguration(targetToStop, minStopLoss, minProfitTarget, TargetStopRunStrategies.Dollar_And_Percent_TS);
		
		RunConfiguration config10 = new RunConfiguration(RunStrategies.High_Win_Rate_No_Restrictions_V1, minAvgNumberOfTradesPerDay, minWinRate, minPf,
				minPercentOfDaysTraded, optimizeCount, discreteCount, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, optimizeTargetStop,
				onlyOptimizeTargetStop, tsRunConfigs);
		
		runConfigs.add(config1);
		runConfigs.add(config2);
		runConfigs.add(config3);
		runConfigs.add(config4);
		runConfigs.add(config5);
		runConfigs.add(config6);
		runConfigs.add(config7);
		runConfigs.add(config8);
		runConfigs.add(config9);
		runConfigs.add(config10);
		
		return runConfigs;
	}
	
	public static Map<String, List<PreFilter>> getPreFilterMap() {
		Map<String, List<PreFilter>> preFilterMap = new HashMap<>();
		
		//"bottoms-pattern" Pre-Filter List
		/*List<PreFilter> preFilterList = new ArrayList<>();
		
		List<FilterRange> filterList = new ArrayList<>();
		filterList.add(new FilterRange("Position in 60 minute range (%) [R60M]", 0, 27));
		preFilterList.add(new PreFilter("bottoms-pattern", "Position in 60 minute range (%) [R60M]", filterList));

		filterList = new ArrayList<>();
		filterList.add(new FilterRange("NASDAQ Change 30 Minute (%) [Qqqq30]", -0.03, 0.64));
		preFilterList.add(new PreFilter("bottoms-pattern", "NASDAQ Change 30 Minute (%) [Qqqq30]", filterList));
				
		filterList = new ArrayList<>();
		filterList.add(new FilterRange("Average Directional Index (%) [ADX]", 21.9, 33.83));
		preFilterList.add(new PreFilter("bottoms-pattern", "Average Directional Index (%) [ADX]", filterList));
		
		filterList = new ArrayList<>();
		filterList.add(new FilterRange("Position in 60 minute range (%) [R60M]", 0, 31));
		preFilterList.add(new PreFilter("bottoms-pattern", "Position in 60 minute range (%) [R60M]", filterList));
		
		preFilterMap.put("bottoms-pattern", preFilterList);*/

		//"crossed-resistance" Pre-Filter List
		List<PreFilter> preFilterList = new ArrayList<>();
		
		List<FilterRange> filterList = new ArrayList<>();
		filterList.add(new FilterRange("Dow Change 30 Minute (%) [Dia30]", -0.031, 0.397));
		preFilterList.add(new PreFilter("crossed-resistance", "Dow Change 30 Minute (%) [Dia30]", filterList));

		preFilterMap.put("crossed-resistance", preFilterList);
		
		return preFilterMap;
	}
	
	public void updateRunConditionsAfterDataLoad(int inSampleDataDaysRange, int totalTradeDataRowCount) {
		this.minTrades = (int)Math.round(this.minAvgNumberOfTradesPerDay*inSampleDataDaysRange);
		this.totalTradeDataRowCount = totalTradeDataRowCount;
	}
	
	public double getMinAvgNumberOfTradesPerDay() {
		return minAvgNumberOfTradesPerDay;
	}

	public int getMinWinRate() {
		return minWinRate;
	}

	public double getMinPf() {
		return minPf;
	}

	public int getMinPercentOfDaysTraded() {
		return minPercentOfDaysTraded;
	}

	public int getOptimizeCount() {
		return optimizeCount;
	}

	public int getDiscreteCount() {
		return discreteCount;
	}

	public int getPercentOfDaysForInSampleTest() {
		return percentOfDaysForInSampleTest;
	}

	public boolean isOptimizeTargetStop() {
		return optimizeTargetStop;
	}

	public boolean isOnlyOptimizeTargetStop() {
		return onlyOptimizeTargetStop;
	}

	public int getMinTrades() {
		return minTrades;
	}

	public int getTotalTradeDataRowCount() {
		return totalTradeDataRowCount;
	}

	public RunStrategies getRunStrategy() {
		return runStrategy;
	}

	public int getPercentOfDaysForFinalTest() {
		return percentOfDaysForFinalTest;
	}

	public TargetStopRunConfiguration getTsRunConfigurations() {
		return tsRunConfigurations;
	}

	public void setTsRunConfigurations(TargetStopRunConfiguration tsRunConfigurations) {
		this.tsRunConfigurations = tsRunConfigurations;
	}

	public List<FilterRange> getPreFilter() {
		return preFilter;
	}

	public void setPreFilter(List<FilterRange> preFilter) {
		this.preFilter = preFilter;
	}

	public String getOnlyForThisStrategy() {
		return onlyForThisStrategy;
	}
}
