package com.op.bo;

import java.util.Set;

public class PriceLevels {
	private String symbol;
	
	private Set<PriceLevel> priceLevels;
	
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public Set<PriceLevel> getPriceLevels() {
		return priceLevels;
	}
	public void setPriceLevels(Set<PriceLevel> priceLevels) {
		this.priceLevels = priceLevels;
	}
}
