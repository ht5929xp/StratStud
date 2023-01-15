package com.op.bo;

import java.io.Serializable;

public class FilterRange implements Comparable<FilterRange>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String var;
	private double min;
	private double max;
	
	public FilterRange(String var, double min, double max) {
		super();
		this.var = var;
		this.min = min;
		this.max = max;
	}

	@Override
	public String toString() {
		return "var = " + var + ": min = " + min + "\tmax = " + max;
	}

	public String getVar() {
		return var;
	}
	public void setVar(String var) {
		this.var = var;
	}
	public double getMin() {
		return min;
	}
	public void setMin(double min) {
		this.min = min;
	}
	public double getMax() {
		return max;
	}
	public void setMax(double max) {
		this.max = max;
	}

	@Override
	public int compareTo(FilterRange arg0) {
		return max - min < arg0.max - arg0.min ? 1 : -1;
	}
}
