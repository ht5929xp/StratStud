package com.op;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.op.bo.PriceLevel;
import com.op.bo.PriceLevels;
import com.op.bo.TradeData;
import com.op.bo.TradeRecord;
import com.op.bo.Utility;

public class CorrectBouncers {
	public static void main(String args[]) {
		final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		File dir = new File("C:\\Users\\t8005ht\\StratStud\\correctbouncers");
		Map<String, PriceLevels> priceLevelsData = Utility.extractPriceLevelData(dir);
		try {
			List<TradeData> tradeData = Utility.loadTradeData(dir, 100, 0, priceLevelsData, null);
			
			System.out.println(tradeData.size());
			
			TradeData data = tradeData.get(0);
			
			PrintWriter writer = new PrintWriter(new FileWriter(new File("C:\\Users\\t8005ht\\StratStud\\correctbouncers\\export_1.csv")));
			
			boolean first = true;
			
			int count = 1;
			List<String> dataColumns = new ArrayList<>();
			for(TradeRecord rec : data.getTrades()) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(rec.getEntryTime());
				cal.add(Calendar.MINUTE, 45);
				
				Date timeoutTime = cal.getTime();
				long timeoutMillis = cal.getTime().toInstant().getEpochSecond();
				
				double estimatedPrice = getClosestPrice(timeoutTime, rec.getPriceLevels());

				String result = estimatedPrice > rec.getEntryPrice() ? "Winner" : "Loser";
				
				DatePrice mfe = getEstimatedMFE(rec.getEntryTime(), timeoutTime, rec.getPriceLevels());
				DatePrice mae = getEstimatedMAE(rec.getEntryTime(), timeoutTime, rec.getPriceLevels());

				StringBuilder builder = new StringBuilder();
				if (first) {
					builder.append(",Symbol,Entry Time,Entry Time (time_t),Entry Price,Timeout Price,Timeout Time,Timeout Time (time_t),Reason For Exit,Exit Price,Result,Moved,MFE,MFE Time,MFE Time (time_t),MAE,MAE Time,MAE Time (time_t),Exchange,Entry Alert");
					for (java.util.Map.Entry<String, Double> entry : rec.getData().entrySet()) {
						dataColumns.add(entry.getKey());
						builder.append("," + entry.getKey());
					}
					writer.println(builder.toString());
					first = false;
				} else {
					builder.append(count++ + "," + rec.getSymbol() + "," + df.format(rec.getEntryTime()) + ","
							+ rec.getEntryTimeT() + "," + rec.getEntryPrice() + "," + estimatedPrice + ","
							+ df.format(timeoutTime) + "," + timeoutMillis + "," + rec.getReasonForExit() + ","
							+ estimatedPrice + "," + result + "," + (estimatedPrice - rec.getEntryPrice()) + ","
							+ mfe.price + "," + df.format(mfe.date) + "," + mfe.date.toInstant().getEpochSecond() + ","
							+ mae.price + "," + df.format(mae.date) + "," + mae.date.toInstant().getEpochSecond() + ","
							+ rec.getExchange() + "," + rec.getEntryAlert());
					for (String col : dataColumns) {
						Double value = rec.getData().get(col);
						builder.append("," + (value != null ? value : ""));
					}
					writer.println(builder.toString());
				}
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static double getClosestPrice(Date timeoutTime, List<PriceLevel> priceLevels) {		
		Double priceClosestAfter = null;
		Date closestAfter = null;
		Date closestBefore = null;
		Double priceClosestBefore = null;
		
		for(PriceLevel pl : priceLevels) {
			if(pl.getDate().after(timeoutTime)) {
				if(closestAfter == null || pl.getDate().before(closestAfter)) {
					closestAfter = pl.getDate();
					priceClosestAfter = pl.getPrice();
				}
			} else if (pl.getDate().before(timeoutTime)) {
				if(closestBefore == null || pl.getDate().after(closestBefore)) {
					closestBefore = pl.getDate();
					priceClosestBefore = pl.getPrice();
				}
			} else {
				priceClosestAfter = pl.getPrice();
				priceClosestBefore = pl.getPrice();
				closestAfter = pl.getDate();
				closestBefore = pl.getDate();
			}
		}
		
		long millisDiffAfter = closestAfter.getTime() - timeoutTime.getTime();
		long afterDiff = TimeUnit.MILLISECONDS.toMinutes(millisDiffAfter);
		
		long millisDiffBefore = closestBefore.getTime() - timeoutTime.getTime();
		long beforeDiff = TimeUnit.MILLISECONDS.toMinutes(millisDiffBefore);

		double estimatedPrice = 0.0;
		if (afterDiff == beforeDiff && afterDiff == 0) {
			if(priceClosestAfter != priceClosestBefore) {
				if(priceClosestAfter > priceClosestBefore) {
					estimatedPrice = (priceClosestAfter - priceClosestBefore)/2 + priceClosestBefore;
				}else {
					estimatedPrice = (priceClosestBefore - priceClosestAfter)/2 + priceClosestAfter;
				}
			}else {
				estimatedPrice = priceClosestAfter;
			}
		} else {
			double slope = (priceClosestAfter - priceClosestBefore) / (afterDiff - beforeDiff);
			estimatedPrice = priceClosestBefore - beforeDiff * slope;
		}
		//System.out.println(afterDiff + "   " + beforeDiff + "   " + priceClosestBefore + "    " + priceClosestAfter + "    " + estimatedPrice);
		
		return estimatedPrice;
	}

	private static DatePrice getEstimatedMFE(Date entryTime, Date timeoutTime, List<PriceLevel> priceLevels) {		
		Date mfeDate = null;
		Double mfePrice = null;
		
		for (PriceLevel pl : priceLevels) {
			if (pl.getDate().after(entryTime) && pl.getDate().before(timeoutTime)) {
				if (mfeDate == null || pl.getPrice() > mfePrice) {
					mfeDate = pl.getDate();
					mfePrice = pl.getPrice();
				}
			}
		}
		
		DatePrice dp = new DatePrice();
		dp.date = mfeDate;
		dp.price = mfePrice;
		
		return dp;
	}
	
	private static DatePrice getEstimatedMAE(Date entryTime, Date timeoutTime, List<PriceLevel> priceLevels) {		
		Date maeDate = null;
		Double maePrice = null;
		
		for (PriceLevel pl : priceLevels) {
			if (pl.getDate().after(entryTime) && pl.getDate().before(timeoutTime)) {
				if (maeDate == null || pl.getPrice() < maePrice) {
					maeDate = pl.getDate();
					maePrice = pl.getPrice();
				}
			}
		}
		
		DatePrice dp = new DatePrice();
		dp.date = maeDate;
		dp.price = maePrice;
		
		return dp;
	}
	
	private static class DatePrice{
		Date date;
		double price;
	}
}
