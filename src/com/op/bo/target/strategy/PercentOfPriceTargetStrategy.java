package com.op.bo.target.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.op.bo.TradeRecord;
import com.op.bo.Utility;
import com.op.bo.targetstop.TargetStopStrategyResult;

public class PercentOfPriceTargetStrategy extends TargetStrategy {

	private static final String STRATEGY_NAME = "Percent of Price Strategy";
	private static final double INCREMENT = 0.1; //Percent increment
	
	private double minTarget; //Dollar amount
	
	private static final double MIN_TARGET_PERCENT = 0.1; //The minimum allowed percent target for this strategy
	
	private double maxTargetPercent = 0.0;
	
	public PercentOfPriceTargetStrategy(Collection<TradeRecord> inSampleTrades, double minProfitTarget) {
		this.minTarget = minProfitTarget;
		
		double maxPercentMove = 0.0;
		
		for (TradeRecord trade : inSampleTrades) {
			if (trade.getMfe() != null) {
				double move = Math.abs(trade.getMfe() - trade.getEntryPrice());
				double percentMove = (move / trade.getEntryPrice())*100.0;
				if (percentMove > maxPercentMove) {
					maxPercentMove = percentMove;
				}
			}
		}
		
		this.maxTargetPercent = maxPercentMove + INCREMENT;
		
		System.out.println("Initialized PercentOfPriceTargetStrategy with maxTargetPercent: " + maxTargetPercent);
	}
	
	@Override
	public List<TargetStopStrategyResult> calculate(Collection<TradeRecord> allTrades, boolean isShort) {		
		List<TargetStopStrategyResult> results = new ArrayList<>();
		
		for (double i = MIN_TARGET_PERCENT; i <= maxTargetPercent; i += INCREMENT*(i + 1)) {
			double roundedI = Utility.round(i, 2);
			results.add(getTargetStopStrategyResult(roundedI, allTrades, isShort));
		}
		
		return results;
	}

	private TargetStopStrategyResult getTargetStopStrategyResult(double roundedI, Collection<TradeRecord> allTrades, boolean isShort) {
		Map<Integer, Double> targetsMap = new HashMap<>();
		int numberOfTargetsHit = 0;

		for (TradeRecord rec : allTrades) {
			double targetMove = rec.getEntryPrice()*(roundedI/100.0);
			
			if (targetMove < minTarget) {
				targetMove = minTarget;
			}
			
			double target = Utility.round(!isShort ? rec.getEntryPrice() + targetMove : rec.getEntryPrice() - targetMove, 2);
			
			if (target < 0) {
				target = 0;
			}
			
			targetsMap.put(rec.getId(), target);

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
				numberOfTargetsHit / allTrades.size(), targetsMap, roundedI, 0.0, 0.0, 0.0);
		
		return result;
	}
	
	private String getStrategyProperties(double percent) {
		return getStrategyName() + ":\n" + "Percent: " + percent + " (min target = " + minTarget + ")";
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
