package com.op.bo.stoploss.strategy;

import java.util.Collection;
import java.util.List;

import com.op.bo.TradeRecord;
import com.op.bo.targetstop.TargetStopStrategyResult;

public abstract class StopLossStrategy {

	public static int maxResultId = 0;
	
	/**
	 * For each stop loss configuration for a specific stop loss strategy, this calculates the results for applying the stop loss strategy all trades in sample and out. Returns a result
	 * for each configuration, each TargetStopStrategyResult contains the stop loss configuration settings, result ID, and for each trade the stop loss price for the strategy and trade.
	 * @param inSampleTrades
	 * @return
	 */
	public abstract List<TargetStopStrategyResult> calculate(Collection<TradeRecord> allTrades, boolean isShort);

	public abstract TargetStopStrategyResult calculate(Collection<TradeRecord> allTrades, double stopLoss, boolean isShort);
	
	public abstract String getStrategyName();

}
