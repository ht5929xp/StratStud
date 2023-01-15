package com.op.bo.stoploss.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.op.bo.TradeRecord;
import com.op.bo.Utility;
import com.op.bo.targetstop.TargetStopStrategyResult;

public class DollarAmountStopLossStrategy extends StopLossStrategy {

	private static final String STRATEGY_NAME = "Specific Dollar Amount Strategy";
	private static final double INCREMENT = 0.01;
	
	private double minStopLoss; //The minimum allowed stop loss for this strategy
	
	private double maxStopLoss = 0.0;
	
	public DollarAmountStopLossStrategy(Collection<TradeRecord> allTrades, double minStopLoss) {
		this.minStopLoss = minStopLoss;
		
		double maxStopLoss = 0.0;
		
		for (TradeRecord trade : allTrades) {
			if (trade.getMae() != null) {
				double move = Math.abs(trade.getEntryPrice() - trade.getMae());
				if (maxStopLoss < move) {
					maxStopLoss = move;
				}
			}
		}
		
		this.maxStopLoss = Math.abs(maxStopLoss + INCREMENT);
		
		System.out.println("Initialized DollarAmountStopLossStrategy with maxStopLoss: " + maxStopLoss);
	}
	
	@Override
	public List<TargetStopStrategyResult> calculate(Collection<TradeRecord> allTrades, boolean isShort) {
		List<TargetStopStrategyResult> results = new ArrayList<>();
		
		for (double i = minStopLoss; i <= maxStopLoss; i += INCREMENT*(i + 1)) {
			double roundedI = Utility.round(i, 2);
			results.add(getTargetStopStrategyResult(roundedI, allTrades, isShort));
		}
		
		return results;
	}
	
	private TargetStopStrategyResult getTargetStopStrategyResult(double roundedI, Collection<TradeRecord> allTrades, boolean isShort) {
		Map<Integer, Double> stopsMap = new HashMap<>();
		int numberOfStopsHit = 0;

		for (TradeRecord rec : allTrades) {
			double stopLoss = Utility.round(!isShort ? rec.getEntryPrice() - roundedI : rec.getEntryPrice() + roundedI, 2);

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
				numberOfStopsHit / allTrades.size(), stopsMap, 0.0, 0.0, 0.0, roundedI);
		
		return result;
	}

	private String getStrategyProperties(double dollarAmount) {
		return getStrategyName() + ":\n" + "Dollar Amount: " + dollarAmount;
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
