package com.op.bo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.op.bo.targetstop.TargetStopComboStrategyResult;
import com.op.bo.targetstop.TargetStopInfo;

public class TradeRecord implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int id;
	private String symbol;
	private int entryDay; //Day of the year
	private Date entryTime;
	private long entryTimeT;
	private double entryPrice;
	private double timeoutPrice;
	private Date timeoutTime;
	private long timeoutTimeT;
	private double exitAlertPrice;
	private Date exitAlertTime;
	private long exitAlertTimeT;
	private double profitTargetPrice;
	private Date profitTargetTime;
	private Long profitTargetTimeT;
	private Double stopLossPrice;
	private Date stopLossTime;
	private Long stopLossTimeT;
	private String reasonForExit;
	private double exitPrice;
	private Date exitTime;
	private long exitTimeT;
	private double priceDiff;
	private String resultStr;
	private boolean winner;
	private double moved;
	private String exchange;
	private String entryAlert;
	private Double mfe;
	private Date mfeTime;
	private Long mfeTimeT;
	private Double mae;
	private Date maeTime;
	private Long maeTimeT;
	private double profitPercent;
	private boolean outOfSample;
	private boolean finalTestSample;
	
	/**
	 * All Price Levels within the trade duration, using supplemental data.
	 * 
	 * Supplemental data is data named targetstop_# provided with the strategy data which has various entry/timeout ranges to obtain the MAE and MFE
	 * for each trade more accurately.
	 */
	private List<PriceLevel> priceLevels;

	/**
	 * This is a map of Stop Loss price and minimum date hitting the stop.
	 * Used to improve performance for Target/Stop optimization. This is filled with values from target stop strategies.
	 */
	private Map<Double, Date> minDateByStopPrice;
	
	/**
	 * This is a map of Target price and minimum date hitting the target.
	 * Used to improve performance for Target/Stop optimization. This is filled with values from target stop strategies.
	 */
	private Map<Double, Date> minDateByTargetPrice;
	
	private Map<String, Double> data = new HashMap<String, Double>();
	
	/**
	 * This is a list of trade information based on top target/stop optimization results.
	 */
	private List<TargetStopInfo> targetStopInfo = new ArrayList<>();
	private Map<Integer, TargetStopInfo> targetStopInfoMap = new HashMap<>();
	
	protected TargetStopInfo getTargetStopInfo(TargetStopComboStrategyResult targetStopComboStrategyResult) {
		return targetStopComboStrategyResult != null ? this.getTargetStopInfoMap().get(targetStopComboStrategyResult.getTargetStopComboResultId()) : null;
	}
	
	public boolean isInSample() {
		return !isFinalTestSample() && !isOutOfSample();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entryAlert == null) ? 0 : entryAlert.hashCode());
		result = prime * result + ((entryTime == null) ? 0 : entryTime.hashCode());
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TradeRecord other = (TradeRecord) obj;
		if (entryAlert == null) {
			if (other.entryAlert != null)
				return false;
		} else if (!entryAlert.equals(other.entryAlert))
			return false;
		if (entryTime == null) {
			if (other.entryTime != null)
				return false;
		} else if (!entryTime.equals(other.entryTime))
			return false;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		return true;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void setProfitTargetTimeT(Long profitTargetTimeT) {
		this.profitTargetTimeT = profitTargetTimeT;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Date getEntryTime() {
		return entryTime;
	}
	public void setEntryTime(Date entryTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(entryTime);
		this.entryDay = cal.get(Calendar.DAY_OF_YEAR);
		this.entryTime = entryTime;
	}
	public long getEntryTimeT() {
		return entryTimeT;
	}
	public void setEntryTimeT(long entryTimeT) {
		this.entryTimeT = entryTimeT;
	}
	public double getEntryPrice() {
		return entryPrice;
	}
	public void setEntryPrice(double entryPrice) {
		this.entryPrice = entryPrice;
	}
	public double getTimeoutPrice() {
		return timeoutPrice;
	}
	public void setTimeoutPrice(double timeoutPrice) {
		this.timeoutPrice = timeoutPrice;
	}
	public Date getTimeoutTime() {
		return timeoutTime;
	}
	public void setTimeoutTime(Date timeoutTime) {
		this.timeoutTime = timeoutTime;
	}
	public long getTimeoutTimeT() {
		return timeoutTimeT;
	}
	public void setTimeoutTimeT(long timeoutTimeT) {
		this.timeoutTimeT = timeoutTimeT;
	}
	public double getProfitTargetPrice() {
		return profitTargetPrice;
	}
	public void setProfitTargetPrice(double profitTargetPrice) {
		this.profitTargetPrice = profitTargetPrice;
	}
	public Date getProfitTargetTime() {
		return profitTargetTime;
	}
	public void setProfitTargetTime(Date profitTargetTime) {
		this.profitTargetTime = profitTargetTime;
	}
	public long getProfitTargetTimeT() {
		return profitTargetTimeT;
	}
	public void setProfitTargetTimeT(long profitTargetTimeT) {
		this.profitTargetTimeT = profitTargetTimeT;
	}
	public Double getStopLossPrice() {
		return stopLossPrice;
	}
	public void setStopLossPrice(Double stopLossPrice) {
		this.stopLossPrice = stopLossPrice;
	}
	public Date getStopLossTime() {
		return stopLossTime;
	}
	public void setStopLossTime(Date stopLossTime) {
		this.stopLossTime = stopLossTime;
	}
	public Long getStopLossTimeT() {
		return stopLossTimeT;
	}
	public void setStopLossTimeT(Long stopLossTimeT) {
		this.stopLossTimeT = stopLossTimeT;
	}
	public String getReasonForExit() {
		return reasonForExit;
	}
	public void setReasonForExit(String reasonForExit) {
		this.reasonForExit = reasonForExit;
	}
	public double getExitPrice() {
		return exitPrice;
	}
	public void setExitPrice(double exitPrice) {
		this.exitPrice = exitPrice;
	}
	public String getResultStr() {
		return resultStr;
	}
	public boolean isWinner() {
		return winner;
	}
	public void setResultStr(String resultStr) {
		this.resultStr = resultStr;
		this.winner = "Winner".equals(this.resultStr);
	}
	public double getMoved() {
		return moved;
	}
	public void setMoved(double moved) {
		this.moved = moved;
	}
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
	public String getEntryAlert() {
		return entryAlert;
	}
	public void setEntryAlert(String entryAlert) {
		this.entryAlert = entryAlert;
	}
	public Map<String, Double> getData() {
		return data;
	}
	public void setData(Map<String, Double> data) {
		this.data = data;
	}	
	public double getPriceDiff() {
		return priceDiff;
	}
	public void setPriceDiff(double priceDiff) {
		this.priceDiff = priceDiff;
	}
	public void setWinner(boolean winner) {
		this.winner = winner;
	}
	public int getEntryDay() {
		return entryDay;
	}
	public void setEntryDay(int entryDay) {
		this.entryDay = entryDay;
	}
	public Double getMfe() {
		return mfe;
	}
	public void setMfe(Double mfe) {
		this.mfe = mfe;
	}
	public Date getMfeTime() {
		return mfeTime;
	}
	public void setMfeTime(Date mfeTime) {
		this.mfeTime = mfeTime;
	}
	public Long getMfeTimeT() {
		return mfeTimeT;
	}
	public void setMfeTimeT(Long mfeTimeT) {
		this.mfeTimeT = mfeTimeT;
	}
	public Double getMae() {
		return mae;
	}
	public void setMae(Double mae) {
		this.mae = mae;
	}
	public Date getMaeTime() {
		return maeTime;
	}
	public void setMaeTime(Date maeTime) {
		this.maeTime = maeTime;
	}
	public Long getMaeTimeT() {
		return maeTimeT;
	}
	public void setMaeTimeT(Long maeTimeT) {
		this.maeTimeT = maeTimeT;
	}	
	public double getProfitPercent() {
		return profitPercent;
	}
	public void setProfitPercent(double profitPercent) {
		this.profitPercent = profitPercent;
	}
	public Date getExitTime() {
		return exitTime;
	}
	public void setExitTime(Date exitTime) {
		this.exitTime = exitTime;
	}
	public boolean isOutOfSample() {
		return outOfSample;
	}
	public void setOutOfSample(boolean outOfSample) {
		this.outOfSample = outOfSample;
	}	
	public long getExitTimeT() {
		return exitTimeT;
	}
	public void setExitTimeT(long exitTimeT) {
		this.exitTimeT = exitTimeT;
	}
	public double getExitAlertPrice() {
		return exitAlertPrice;
	}
	public void setExitAlertPrice(double exitAlertPrice) {
		this.exitAlertPrice = exitAlertPrice;
	}
	public Date getExitAlertTime() {
		return exitAlertTime;
	}
	public void setExitAlertTime(Date exitAlertTime) {
		this.exitAlertTime = exitAlertTime;
	}
	public long getExitAlertTimeT() {
		return exitAlertTimeT;
	}
	public void setExitAlertTimeT(long exitAlertTimeT) {
		this.exitAlertTimeT = exitAlertTimeT;
	}
	public List<PriceLevel> getPriceLevels() {
		return priceLevels;
	}
	public void setPriceLevels(List<PriceLevel> priceLevels) {
		this.priceLevels = priceLevels;
	}
	public Map<Double, Date> getMinDateByStopPrice() {
		return minDateByStopPrice;
	}
	public void setMinDateByStopPrice(Map<Double, Date> minDateByStopPrice) {
		this.minDateByStopPrice = minDateByStopPrice;
	}
	public Map<Double, Date> getMinDateByTargetPrice() {
		return minDateByTargetPrice;
	}
	public void setMinDateByTargetPrice(Map<Double, Date> minDateByTargetPrice) {
		this.minDateByTargetPrice = minDateByTargetPrice;
	}
	public List<TargetStopInfo> getTargetStopInfo() {
		return targetStopInfo;
	}
	public void setTargetStopInfo(List<TargetStopInfo> targetStopInfo) {
		this.targetStopInfo = targetStopInfo;
	}
	public Map<Integer, TargetStopInfo> getTargetStopInfoMap() {
		return targetStopInfoMap;
	}
	public void setTargetStopInfoMap(Map<Integer, TargetStopInfo> targetStopInfoMap) {
		this.targetStopInfoMap = targetStopInfoMap;
	}
	public boolean isFinalTestSample() {
		return finalTestSample;
	}
	public void setFinalTestSample(boolean finalTestSample) {
		this.finalTestSample = finalTestSample;
	}

	@Override
	public String toString() {
		return "TradeRecord [symbol=" + symbol + ", entryTime=" + entryTime + ", entryTimeT=" + entryTimeT
				+ ", entryPrice=" + entryPrice + ", timeoutPrice=" + timeoutPrice + ", timeoutTime=" + timeoutTime
				+ ", timeoutTimeT=" + timeoutTimeT + ", profitTargetPrice=" + profitTargetPrice + ", profitTargetTime="
				+ profitTargetTime + ", profitTargetTimeT=" + profitTargetTimeT + ", stopLossPrice=" + stopLossPrice
				+ ", stopLossTime=" + stopLossTime + ", stopLossTimeT=" + stopLossTimeT + ", reasonForExit="
				+ reasonForExit + ", exitPrice=" + exitPrice + ", resultStr=" + resultStr + ", winner=" + winner
				+ ", moved=" + moved + ", exchange=" + exchange + ", entryAlert=" + entryAlert + ", data=" + data + "]";
	}
}
