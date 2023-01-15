package com.op.bo.stoploss.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.op.bo.TradeRecord;
import com.op.bo.Utility;
import com.op.bo.targetstop.TargetStopStrategyResult;

public class PercentOfATRStopLossStrategy extends StopLossStrategy {

	private static final String STRATEGY_NAME = "Percent of ATR Strategy";
	private static final double INCREMENT = 1; //Percent increment
	
	private double minStopLoss; //Dollar amount
	
	private static final double MIN_STOP_LOSS_PERCENT_OF_ATR = 1; //The minimum allowed percent stop loss for this strategy
	
	private double maxStopLossPercentOfATR = 1000;
	
	public PercentOfATRStopLossStrategy(Collection<TradeRecord> allTrades, double minStopLoss) {
		this.minStopLoss = minStopLoss;
	}
	
	@Override
	public List<TargetStopStrategyResult> calculate(Collection<TradeRecord> allTrades, boolean isShort) {
		List<TargetStopStrategyResult> results = new ArrayList<>();
		
		for (double i = MIN_STOP_LOSS_PERCENT_OF_ATR; i <= maxStopLossPercentOfATR; i += INCREMENT + INCREMENT*(i/25)) {
			double roundedI = Utility.round(i, 2);
			results.add(getTargetStopStrategyResult(roundedI, allTrades, isShort));
		}
		
		return results;
	}

	private TargetStopStrategyResult getTargetStopStrategyResult(double roundedI, Collection<TradeRecord> allTrades, boolean isShort) {
		Map<Integer, Double> stopsMap = new HashMap<>();
		int numberOfStopsHit = 0;

		for (TradeRecord rec : allTrades) {
			Double atrObj = rec.getData().get("Average True Range ($) [ATR]");
			double atr = atrObj != null ? atrObj : minStopLoss;
			double stopLossMove = atr*(roundedI/100.0);

			if (stopLossMove < minStopLoss) {
				stopLossMove = minStopLoss;
			}
			
			double stopLoss = Utility.round(!isShort ? rec.getEntryPrice() - stopLossMove : rec.getEntryPrice() + stopLossMove, 2);
			
			if (stopLoss < 0) {
				stopLoss = 0;
			}
			
			stopsMap.put(rec.getId(), stopLoss);

			if (!isShort) {
				if (rec.getMae() != null && rec.getMae() <= stopLoss) {
					numberOfStopsHit++;
				}
			} else {
				if (rec.getMae() != null && rec.getMae() >= stopLoss) {
					numberOfStopsHit++;
				}
			}
		}

		int resultId = TargetStopStrategyResult.maxResultId++;
		TargetStopStrategyResult result = new TargetStopStrategyResult(resultId, getStrategyProperties(roundedI), numberOfStopsHit,
				numberOfStopsHit / allTrades.size(), stopsMap, 0.0, 0.0, 0.0, 0.0);
		
		return result;
	}
	
	private String getStrategyProperties(double percent) {
		return getStrategyName() + ":\n" + "Percent: " + percent + " (min stop = " + minStopLoss + ")";
	}
	
	@Override
	public String getStrategyName() {
		return STRATEGY_NAME;
	}
	
	@Override
	public TargetStopStrategyResult calculate(Collection<TradeRecord> allTrades, double stopLoss, boolean isShort) {
		return getTargetStopStrategyResult(stopLoss, allTrades, isShort);
	}
}
