package com.op.bo.target.strategy;

import java.util.Collection;
import java.util.List;

import com.op.bo.TradeRecord;
import com.op.bo.targetstop.TargetStopStrategyResult;

public abstract class TargetStrategy {
	/**
	 * For each Target configuration for a specific target strategy, this calculates the results for applying the target strategy to all trades in sample and out. Returns a result
	 * for each configuration, each TargetStopStrategyResult contains the target configuration settings, result ID, and for each trade the target price for the strategy and trade.
	 * @param allTrades
	 * @return
	 */
	public abstract List<TargetStopStrategyResult> calculate(Collection<TradeRecord> allTrades, boolean isShort);

	public abstract TargetStopStrategyResult calculate(Collection<TradeRecord> allTrades, double target, boolean isShort);
	
	public abstract String getStrategyName();
}
