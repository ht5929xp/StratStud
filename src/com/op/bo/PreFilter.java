package com.op.bo;

import java.util.List;

public class PreFilter {
	private String strategyName;
	private String filterName;
	private List<FilterRange> filter;

	public PreFilter(String strategyName, String filterName, List<FilterRange> filter) {
		super();
		this.strategyName = strategyName;
		this.filterName = filterName;
		this.filter = filter;
	}
	
	public String getStrategyName() {
		return strategyName;
	}
	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}
	public String getFilterName() {
		return filterName;
	}
	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}
	public List<FilterRange> getFilter() {
		return filter;
	}
	public void setFilter(List<FilterRange> filter) {
		this.filter = filter;
	}
}
