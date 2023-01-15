package com.op.bo;

import java.io.Serializable;
import java.util.Date;

public class MarketCondition implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Date date;
	private double dowChange5;
	private double dowChange10;
	private double dowChange15;
	private double dowChange30;
	private double dowChangeToday;
	
	private double spyChange5;
	private double spyChange10;
	private double spyChange15;
	private double spyChange30;
	private double spyChangeToday;
	
	private double nasdaqChange5;
	private double nasdaqChange10;
	private double nasdaqChange15;
	private double nasdaqChange30;
	private double nasdaqChangeToday;

	public MarketCondition(Date date, double dowChange5, double dowChange10, double dowChange15, double dowChange30,
			double dowChangeToday, double spyChange5, double spyChange10, double spyChange15, double spyChange30,
			double spyChangeToday, double nasdaqChange5, double nasdaqChange10, double nasdaqChange15,
			double nasdaqChange30, double nasdaqChangeToday) {
		super();
		this.date = date;
		this.dowChange5 = dowChange5;
		this.dowChange10 = dowChange10;
		this.dowChange15 = dowChange15;
		this.dowChange30 = dowChange30;
		this.dowChangeToday = dowChangeToday;
		this.spyChange5 = spyChange5;
		this.spyChange10 = spyChange10;
		this.spyChange15 = spyChange15;
		this.spyChange30 = spyChange30;
		this.spyChangeToday = spyChangeToday;
		this.nasdaqChange5 = nasdaqChange5;
		this.nasdaqChange10 = nasdaqChange10;
		this.nasdaqChange15 = nasdaqChange15;
		this.nasdaqChange30 = nasdaqChange30;
		this.nasdaqChangeToday = nasdaqChangeToday;
	}
	
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public double getDowChange5() {
		return dowChange5;
	}
	public void setDowChange5(double dowChange5) {
		this.dowChange5 = dowChange5;
	}
	public double getDowChange10() {
		return dowChange10;
	}
	public void setDowChange10(double dowChange10) {
		this.dowChange10 = dowChange10;
	}
	public double getDowChange15() {
		return dowChange15;
	}
	public void setDowChange15(double dowChange15) {
		this.dowChange15 = dowChange15;
	}
	public double getDowChange30() {
		return dowChange30;
	}
	public void setDowChange30(double dowChange30) {
		this.dowChange30 = dowChange30;
	}
	public double getDowChangeToday() {
		return dowChangeToday;
	}
	public void setDowChangeToday(double dowChangeToday) {
		this.dowChangeToday = dowChangeToday;
	}
	public double getSpyChange5() {
		return spyChange5;
	}
	public void setSpyChange5(double spyChange5) {
		this.spyChange5 = spyChange5;
	}
	public double getSpyChange10() {
		return spyChange10;
	}
	public void setSpyChange10(double spyChange10) {
		this.spyChange10 = spyChange10;
	}
	public double getSpyChange15() {
		return spyChange15;
	}
	public void setSpyChange15(double spyChange15) {
		this.spyChange15 = spyChange15;
	}
	public double getSpyChange30() {
		return spyChange30;
	}
	public void setSpyChange30(double spyChange30) {
		this.spyChange30 = spyChange30;
	}
	public double getSpyChangeToday() {
		return spyChangeToday;
	}
	public void setSpyChangeToday(double spyChangeToday) {
		this.spyChangeToday = spyChangeToday;
	}
	public double getNasdaqChange5() {
		return nasdaqChange5;
	}
	public void setNasdaqChange5(double nasdaqChange5) {
		this.nasdaqChange5 = nasdaqChange5;
	}
	public double getNasdaqChange10() {
		return nasdaqChange10;
	}
	public void setNasdaqChange10(double nasdaqChange10) {
		this.nasdaqChange10 = nasdaqChange10;
	}
	public double getNasdaqChange15() {
		return nasdaqChange15;
	}
	public void setNasdaqChange15(double nasdaqChange15) {
		this.nasdaqChange15 = nasdaqChange15;
	}
	public double getNasdaqChange30() {
		return nasdaqChange30;
	}
	public void setNasdaqChange30(double nasdaqChange30) {
		this.nasdaqChange30 = nasdaqChange30;
	}
	public double getNasdaqChangeToday() {
		return nasdaqChangeToday;
	}
	public void setNasdaqChangeToday(double nasdaqChangeToday) {
		this.nasdaqChangeToday = nasdaqChangeToday;
	}
}
