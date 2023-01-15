package com.op.bo.target.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.op.bo.TradeRecord;
import com.op.bo.Utility;
import com.op.bo.targetstop.TargetStopStrategyResult;

public class DollarAmountTargetStrategy extends TargetStrategy {

	private static final String STRATEGY_NAME = "Specific Dollar Amount Strategy";
	private static final double INCREMENT = 0.01;
	
	private double minTarget; //The minimum allowed target for this strategy
	
	private double maxTarget = 0.0;
	
	public DollarAmountTargetStrategy(Collection<TradeRecord> allTrades, double minProfitTarget) {
		this.minTarget = minProfitTarget;
		
		double maxTarget = 0.0;
		
		for (TradeRecord trade : allTrades) {
			if (trade.getMfe() != null) {
				double move = Math.abs(trade.getMfe() - trade.getEntryPrice());
				if (maxTarget < move) {
					maxTarget = move;
				}
			}
		}
		
		this.maxTarget = Math.abs(maxTarget + INCREMENT);
		
		System.out.println("Initialized DollarAmountTargetStrategy with maxTarget: " + maxTarget);
	}
	
	@Override
	public List<TargetStopStrategyResult> calculate(Collection<TradeRecord> allTrades, boolean isShort) {
		List<TargetStopStrategyResult> results = new ArrayList<>();
		
		for (double i = minTarget; i <= maxTarget; i += INCREMENT*(i + 1)) {
			double roundedI = Utility.round(i, 2);
			results.add(getTargetStopStrategyResult(roundedI, allTrades, isShort));
		}
		
		return results;
	}

	private TargetStopStrategyResult getTargetStopStrategyResult(double roundedI, Collection<TradeRecord> allTrades, boolean isShort) {
		Map<Integer, Double> targetsMap = new HashMap<>();
		int numberOfTargetsHit = 0;

		for (TradeRecord rec : allTrades) {
			double target = Utility.round(!isShort ? rec.getEntryPrice() + roundedI : rec.getEntryPrice() - roundedI, 2);
			targetsMap.put(rec.getId(), target);

			if (target < 0) {
				target = 0;
			}
			
			if (!isShort) {
				if (rec.getMfe() != null && rec.getMfe() >= target) {
					numberOfTargetsHit++;
				}
			}else {
				if (rec.getMfe() != null && rec.getMfe() <= target) {
					numberOfTargetsHit++;
				}
			}
		}

		int resultId = TargetStopStrategyResult.maxResultId++;
		TargetStopStrategyResult result = new TargetStopStrategyResult(resultId, getStrategyProperties(roundedI), numberOfTargetsHit,
				numberOfTargetsHit / allTrades.size(), targetsMap, 0.0, 0.0, roundedI, 0.0);
		
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
	public TargetStopStrategyResult calculate(Collection<TradeRecord> allTrades, double target, boolean isShort) {
		return getTargetStopStrategyResult(target, allTrades, isShort);
	}
}
