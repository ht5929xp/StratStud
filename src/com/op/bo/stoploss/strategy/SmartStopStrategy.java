package com.op.bo.stoploss.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.op.bo.TradeRecord;
import com.op.bo.Utility;
import com.op.bo.targetstop.TargetStopStrategyResult;

public class SmartStopStrategy extends StopLossStrategy {

	private static final String STRATEGY_NAME = "Smart Stop $";
	
	private double minStopLoss; //The minimum allowed stop loss for this strategy
	
	public SmartStopStrategy(double minStopLoss) {
		this.minStopLoss = minStopLoss;

		System.out.println("Initialized SmartStopStrategy with minStopLoss: " + minStopLoss);
	}
	
	@Override
	public List<TargetStopStrategyResult> calculate(Collection<TradeRecord> allTrades, boolean isShort) {
		List<TargetStopStrategyResult> results = new ArrayList<>();

		results.add(getTargetStopStrategyResult(allTrades, isShort));
		
		return results;
	}
	
	private TargetStopStrategyResult getTargetStopStrategyResult(Collection<TradeRecord> allTrades, boolean isShort) {
		Map<Integer, Double> stopsMap = new HashMap<>();
		int numberOfStopsHit = 0;

		for (TradeRecord rec : allTrades) {
			Double smartStop = rec.getData().get("Smart Stop ($) [SmartStopD]");
			Double wiggle = rec.getData().get("Wiggle ($) [Wiggle]");
			
			double stop = (smartStop != null ? smartStop : wiggle);
			
			double stopLoss = Utility.round(!isShort ? rec.getEntryPrice() - stop : rec.getEntryPrice() + stop, 2);

			if (stop < minStopLoss) {
				stopLoss = minStopLoss;
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
		TargetStopStrategyResult result = new TargetStopStrategyResult(resultId, getStrategyProperties(), numberOfStopsHit,
				numberOfStopsHit / allTrades.size(), stopsMap, 0.0, 0.0, 0.0, 0.0);
		
		return result;
	}

	private String getStrategyProperties() {
		return getStrategyName();
	}
	
	@Override
	public String getStrategyName() {
		return STRATEGY_NAME + ": (min stop = " + minStopLoss + ")";
	}

	@Override
	public TargetStopStrategyResult calculate(Collection<TradeRecord> allTrades, double stopLoss, boolean isShort) {
		return getTargetStopStrategyResult(allTrades, isShort);
	}
}
