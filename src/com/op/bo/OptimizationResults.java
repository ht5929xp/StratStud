package com.op.bo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OptimizationResults {
	private String strategyName;
	private List<TIRunParameters> runParam;
	
	private List<StratStats> maxPf;
	private List<StratStats> maxPfWin;
	private List<StratStats> maxPfWinTrades;
	private List<StratStats> maxWin;
	private List<StratStats> maxWinTrades;
	private List<StratStats> maxQIndex; //May only be set for Target Stop Optimization
	
	private List<StratStats> maxPfByCol;
	private List<StratStats> maxPfWinByCol;
	private List<StratStats> maxPfWinTradesByCol;
	private List<StratStats> maxWinByCol;
	private List<StratStats> maxWinTradesByCol;
	
	private List<StratStats> maxQIndxByCol;
	
	private double absMaxQIndex = 0.0;
	private double absMaxPf = 0.0;
	private double absMaxWinRate = 0.0;
	
	protected void calculateAbsMaximums() {
		if (maxPf != null) {
			for (StratStats stats : maxPf) {
				if (stats.getPf() > absMaxPf) {
					absMaxPf = stats.getPf();
				}
			}
		}
		
		if (maxPfByCol != null) {
			for (StratStats stats : maxPfByCol) {
				if (stats.getPf() > absMaxPf) {
					absMaxPf = stats.getPf();
				}
			}
		}
		
		if (maxQIndex != null) {
			for (StratStats stats : maxQIndex) {
				if (stats.getQualityIndex() > absMaxQIndex) {
					absMaxQIndex = stats.getQualityIndex();
				}
			}
		}
		
		if (maxQIndxByCol != null) {
			for (StratStats stats : maxQIndxByCol) {
				if (stats.getQualityIndex() > absMaxQIndex) {
					absMaxQIndex = stats.getQualityIndex();
				}
			}
		}
		
		if (maxWin != null) {
			for (StratStats stats : maxWin) {
				if (stats.getWinRate() > absMaxWinRate) {
					absMaxWinRate = stats.getWinRate();
				}
			}
		}
		
		if (maxWinByCol != null) {
			for (StratStats stats : maxWinByCol) {
				if (stats.getWinRate() > absMaxWinRate) {
					absMaxWinRate = stats.getWinRate();
				}
			}
		}
	}
	
	public Set<String> getUniqueColumnsSet(){
		Set<String> columns = new HashSet<>();
		if (maxPf != null) {
			for (StratStats stats : maxPf) {
				for (FilterRange filter : stats.getFilters()) {
					columns.add(filter.getVar());
				}
			}
		}
		
		if (maxPfByCol != null) {
			for (StratStats stats : maxPfByCol) {
				for (FilterRange filter : stats.getFilters()) {
					columns.add(filter.getVar());
				}
			}
		}
		
		if (maxQIndex != null) {
			for (StratStats stats : maxQIndex) {
				for (FilterRange filter : stats.getFilters()) {
					columns.add(filter.getVar());
				}
			}
		}
		
		if (maxQIndxByCol != null) {
			for (StratStats stats : maxQIndxByCol) {
				for (FilterRange filter : stats.getFilters()) {
					columns.add(filter.getVar());
				}
			}
		}
		
		if (maxWin != null) {
			for (StratStats stats : maxWin) {
				for (FilterRange filter : stats.getFilters()) {
					columns.add(filter.getVar());
				}
			}
		}
		
		if (maxWinByCol != null) {
			for (StratStats stats : maxWinByCol) {
				for (FilterRange filter : stats.getFilters()) {
					columns.add(filter.getVar());
				}
			}
		}
		
		return columns;
	}
	
	public List<TIRunParameters> getRunParam() {
		return runParam;
	}
	public void setRunParam(List<TIRunParameters> runParam) {
		this.runParam = runParam;
	}
	public String getStrategyName() {
		return strategyName;
	}
	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}
	public List<StratStats> getMaxPf() {
		return maxPf;
	}
	public void setMaxPf(List<StratStats> maxPf) {
		this.maxPf = maxPf;
	}
	public List<StratStats> getMaxPfWin() {
		return maxPfWin;
	}
	public void setMaxPfWin(List<StratStats> maxPfWin) {
		this.maxPfWin = maxPfWin;
	}
	public List<StratStats> getMaxPfWinTrades() {
		return maxPfWinTrades;
	}
	public void setMaxPfWinTrades(List<StratStats> maxPfWinTrades) {
		this.maxPfWinTrades = maxPfWinTrades;
	}
	public List<StratStats> getMaxWin() {
		return maxWin;
	}
	public void setMaxWin(List<StratStats> maxWin) {
		this.maxWin = maxWin;
	}
	public List<StratStats> getMaxWinTrades() {
		return maxWinTrades;
	}
	public void setMaxWinTrades(List<StratStats> maxWinTrades) {
		this.maxWinTrades = maxWinTrades;
	}
	public List<StratStats> getMaxPfByCol() {
		return maxPfByCol;
	}
	public void setMaxPfByCol(List<StratStats> maxPfByCol) {
		this.maxPfByCol = maxPfByCol;
	}
	public List<StratStats> getMaxPfWinByCol() {
		return maxPfWinByCol;
	}
	public void setMaxPfWinByCol(List<StratStats> maxPfWinByCol) {
		this.maxPfWinByCol = maxPfWinByCol;
	}
	public List<StratStats> getMaxPfWinTradesByCol() {
		return maxPfWinTradesByCol;
	}
	public void setMaxPfWinTradesByCol(List<StratStats> maxPfWinTradesByCol) {
		this.maxPfWinTradesByCol = maxPfWinTradesByCol;
	}
	public List<StratStats> getMaxWinByCol() {
		return maxWinByCol;
	}
	public void setMaxWinByCol(List<StratStats> maxWinByCol) {
		this.maxWinByCol = maxWinByCol;
	}
	public List<StratStats> getMaxWinTradesByCol() {
		return maxWinTradesByCol;
	}
	public void setMaxWinTradesByCol(List<StratStats> maxWinTradesByCol) {
		this.maxWinTradesByCol = maxWinTradesByCol;
	}
	public List<StratStats> getMaxQIndxByCol() {
		return maxQIndxByCol;
	}
	public void setMaxQIndxByCol(List<StratStats> maxQIndxByCol) {
		this.maxQIndxByCol = maxQIndxByCol;
	}
	public List<StratStats> getMaxQIndex() {
		return maxQIndex;
	}
	public void setMaxQIndex(List<StratStats> maxQIndex) {
		this.maxQIndex = maxQIndex;
	}
	public double getAbsMaxQIndex() {
		return absMaxQIndex;
	}
	public double getAbsMaxPf() {
		return absMaxPf;
	}
	public double getAbsMaxWinRate() {
		return absMaxWinRate;
	}
}
