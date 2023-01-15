package com.op.bo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.op.bo.TIRunParameters.PriceExitType;
import com.op.bo.TIRunParameters.TimeExitType;
import com.op.bo.targetstop.TargetStopComboStrategyResult;
import com.op.bo.targetstop.TargetStopInfo;

public class TradeData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private TradeDataRanges ranges = new TradeDataRanges();

	private String groupName = null;
	private File wtiFile;
	
	private String strategyName;
	private TIRunParameters runParam;
	private Collection<TradeRecord> trades;
	private List<FilterRange> filters;
	private boolean isShort;

	private enum Result {Winner, Loser};
	
	public int getUniqueId() {
		List<Integer> order = new ArrayList<Integer>();
		for (TradeRecord trade : trades) {
			order.add(trade.getId());
		}

		Collections.sort(order);

		StringBuilder builder = new StringBuilder();
		for (Integer id : order) {
			builder.append(id).append("|");
		}

		return builder.toString().hashCode();
	}

	public StratStats getStatistics() {
		StratStats stats = new StratStats();
		
		stats.setFilters(this.filters);
				
		double grossProfitPercent = 0.0;
		double grossLossPercent = 0.0;
		
		int winCount = 0;
		int loseCount = 0;

		Set<Integer> distinctDaysOfTrading = new HashSet<Integer>();
		for (TradeRecord trade : trades) {
			if (trade.isInSample()) {
				if (trade.getPriceDiff() > 0.0) {
					grossProfitPercent += (trade.getPriceDiff()/trade.getEntryPrice())*100.0;
					winCount++;
				} else {
					grossLossPercent += (-trade.getPriceDiff()/trade.getEntryPrice())*100.0;
					loseCount++;
				}

				distinctDaysOfTrading.add(trade.getEntryDay());
			}
		}
		
		stats.setTradeCount(winCount + loseCount);
		stats.setPf(grossProfitPercent/grossLossPercent);
		stats.setWinCount(winCount);
		stats.setLoseCount(loseCount);
		stats.setWinRate((Double.valueOf(winCount)/stats.getTradeCount()*1.0)*100.0);
		stats.setAvgWinner(grossProfitPercent/winCount*1.0);
		stats.setAvgLoser(grossLossPercent/loseCount*1.0);
		stats.setPercentDaysTraded((distinctDaysOfTrading.size()*1.0/this.ranges.inSampleDataDaysRange*1.0)*100);
		stats.setTradeData(this);
		
		return stats;
	}

	public List<StratStats> getStatisticsWithTargetStopOptimization() {
		Map<Integer, Double> grossProfitPercentMap = new HashMap<>();
		Map<Integer, Double> grossLossPercentMap = new HashMap<>();
		
		Map<Integer, Integer> winCountMap = new HashMap<>();
		Map<Integer, Integer> loseCountMap = new HashMap<>();
		
		Set<TargetStopComboStrategyResult> targetStopComboResultSet = new HashSet<>();

		boolean firstTrade = true;
		Set<Integer> distinctDaysOfTrading = new HashSet<Integer>();
		for (TradeRecord trade : trades) {
			if (trade.isInSample()) {
				for (TargetStopInfo info : trade.getTargetStopInfo()) {
					
					int targetStopComboResultId = info.getTargetStopComboResult().getTargetStopComboResultId();
					double priceDiff = info.getPriceDiff();
					double profitPercent = info.getProfitPercent();
					double lossPercent = info.getLossPercent();
					
					if (firstTrade) {
						grossProfitPercentMap.put(targetStopComboResultId, 0.0);
						grossLossPercentMap.put(targetStopComboResultId, 0.0);
						winCountMap.put(targetStopComboResultId, 0);
						loseCountMap.put(targetStopComboResultId, 0);
					}
					
					Double grossProfitPercent = grossProfitPercentMap.get(targetStopComboResultId);
					Double grossLossPercent = grossLossPercentMap.get(targetStopComboResultId);
					
					Integer winCount = winCountMap.get(targetStopComboResultId);
					Integer loseCount = loseCountMap.get(targetStopComboResultId);
	
					if (priceDiff > 0.0) {
						grossProfitPercent = grossProfitPercent == null ? profitPercent : grossProfitPercent + profitPercent;
						winCount = winCount == null ? 1 : winCount + 1;
						grossProfitPercentMap.put(targetStopComboResultId, grossProfitPercent);
						winCountMap.put(targetStopComboResultId, winCount);
					} else {
						grossLossPercent = grossLossPercent == null ? lossPercent : grossLossPercent + lossPercent;
						loseCount = loseCount == null ? 1 : loseCount + 1;
						grossLossPercentMap.put(targetStopComboResultId, grossLossPercent);
						loseCountMap.put(targetStopComboResultId, loseCount);
					}
					
					targetStopComboResultSet.add(info.getTargetStopComboResult());
				}
			
				distinctDaysOfTrading.add(trade.getEntryDay());
				
				firstTrade = false;
			}
		}
		
		List<StratStats> statsList = new ArrayList<>();
		for (TargetStopComboStrategyResult targetStopComboResult : targetStopComboResultSet) {
			StratStats stats = new StratStats();
			
			stats.setFilters(this.filters);
			
			int targetStopComboResultId = targetStopComboResult.getTargetStopComboResultId();
			
			int winCount = winCountMap.get(targetStopComboResultId);
			int loseCount = loseCountMap.get(targetStopComboResultId);
			double grossProfitPercent = grossProfitPercentMap.get(targetStopComboResultId);
			double grossLossPercent = grossLossPercentMap.get(targetStopComboResultId);
			
			stats.setTradeCount(winCount + loseCount);
			stats.setPf(grossProfitPercent/grossLossPercent);
			stats.setWinCount(winCount);
			stats.setLoseCount(loseCount);
			stats.setWinRate((Double.valueOf(winCount)/stats.getTradeCount()*1.0)*100.0);
			stats.setAvgWinner(grossProfitPercent/winCount*1.0);
			stats.setAvgLoser(grossLossPercent/loseCount*1.0);
			stats.setPercentDaysTraded((distinctDaysOfTrading.size()*1.0/this.ranges.inSampleDataDaysRange*1.0)*100);

			stats.setTradeData(this);
			stats.setTargetStopComboStrategyResult(targetStopComboResult);
			
			statsList.add(stats);
		}

		return statsList;
	}
	
	public TradeData(Collection<TradeRecord> trades, TIRunParameters runParam, List<FilterRange> filters, TradeDataRanges ranges, boolean isShort) {
		this.trades = trades;
		this.runParam = runParam;
		this.filters = filters;
		this.ranges = ranges;
		this.isShort = isShort;
	}

	public TradeData(Collection<TradeRecord> trades, TIRunParameters runParam, List<FilterRange> filters, TradeDataRanges ranges, String groupName, String strategyName, boolean isShort) {
		this.trades = trades;
		this.runParam = runParam;
		this.filters = filters;
		this.ranges = ranges;
		this.groupName = groupName;
		this.strategyName = strategyName;
		this.isShort = isShort;
	}
	
	public TradeData(List<File> files, int percentOfDaysForInSampleTest, int percentOfDaysForFinalTest, List<FilterRange> preFilter) throws IOException, ParseException {
		this.filters = preFilter;
		
		final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		final SimpleDateFormat df2 = new SimpleDateFormat("HH:mm");
		final SimpleDateFormat df3 = new SimpleDateFormat("hh:mm a");

		trades = new ArrayList<TradeRecord>();
		
		String[] fileNameArray = files.get(0).getName().replace(".csv", "").split(PathResources.FILE_NAME_SEPARATOR);
		this.strategyName = fileNameArray[0];
		
		boolean isTargetStopFile = Utility.isTargetStopFile(this.strategyName);

		int id = 0;
		int count = 0;
		for (File f : files) {
			count++;

			System.out.println("Reading data file [" + f.getName() + "] (" + count + "/" + files.size() + ")...");

			List<String> cols = new ArrayList<String>();
			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {

				Set<String> failedCols = new HashSet<String>();
				
				TreeSet<Double> profitTargetDiff = new TreeSet<Double>();
				TreeSet<Double> profitTargetPercDiff = new TreeSet<Double>();
				
				TreeSet<Double> stopLossDiff = new TreeSet<Double>();
				TreeSet<Double> stopLossPercDiff = new TreeSet<Double>();
				
				List<Integer> secondsFromOpenList = new ArrayList<>();
				TreeSet<String> timeoutTimeStr = new TreeSet<>();
				TreeSet<Integer> timeoutMinutes = new TreeSet<Integer>();
	
				boolean first = true;
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
	
					String[] array = line.split(PathResources.DATA_SEPARATOR);
					if (first) {
						for (String col : array) {
							cols.add(col);
						}

						first = false;
						continue;
					}
					
					TradeRecord rec = new TradeRecord();
					for (int i = 0; i < array.length; i++) {
						String column = cols.get(i);
						
						if (column.isEmpty()) {
							continue;
						}
						
						String val = array[i];
						
						if ("Symbol".equals(column)) {
							rec.setSymbol(val);
						} else if ("Entry Time".equals(column)) {
							rec.setEntryTime(df.parse(val));
							if (this.ranges.overallMinTradeDate == null || rec.getEntryTime().before(this.ranges.overallMinTradeDate)) {
								this.ranges.overallMinTradeDate = rec.getEntryTime();
							}
							if (this.ranges.overallMaxTradeDate == null || rec.getEntryTime().after(this.ranges.overallMaxTradeDate)) {
								this.ranges.overallMaxTradeDate = rec.getEntryTime();
							}
						} else if ("Entry Time (time_t)".equals(column)) {
							rec.setEntryTimeT(Long.parseLong(val));
						} else if ("Entry Price".equals(column)) {
							rec.setEntryPrice(Double.parseDouble(val));
						} else if ("Timeout Price".equals(column)) {
							rec.setTimeoutPrice(Double.parseDouble(val));
						} else if ("Timeout Time".equals(column)) {
							rec.setTimeoutTime(df.parse(val));
						} else if ("Timeout Time (time_t)".equals(column)) {
							rec.setTimeoutTimeT(Long.parseLong(val));
						}else if ("Exit Alert Price".equals(column)) {
							if (!"".equals(val.trim())) {
								rec.setExitAlertPrice(Double.parseDouble(val));
							}
						} else if ("Exit Alert Time".equals(column)) {
							if (!"".equals(val.trim())) {
								rec.setExitAlertTime(df.parse(val));
							}
						} else if ("Exit Alert Time (time_t)".equals(column)) {
							if (!"".equals(val.trim())) {
								rec.setExitAlertTimeT(Long.parseLong(val));
							}
						} else if ("Profit Target Price".equals(column)) {
							rec.setProfitTargetPrice(Double.parseDouble(val));
						} else if ("Stop Loss Price".equals(column)) {
							rec.setStopLossPrice(val.isEmpty() ? null : Double.parseDouble(val));
						} else if ("Reason For Exit".equals(column)) {
							rec.setReasonForExit(val);
						} else if ("Exit Price".equals(column)) {
							rec.setExitPrice(Double.parseDouble(val));
						} else if ("Result".equals(column)) {
							rec.setResultStr(val);
						} else if ("Moved".equals(column)) {
							rec.setMoved(Double.parseDouble(val));
						} else if ("Exchange".equals(column)) {
							rec.setExchange(val);
						} else if ("Entry Alert".equals(column)) {
							rec.setEntryAlert(val);
						} else if ("Stop Loss Time".equals(column)) {
							rec.setStopLossTime(val.isEmpty() ? null : df.parse(val));
						} else if ("Stop Loss Time (time_t)".equals(column)) {
							rec.setStopLossTimeT(val.isEmpty() ? null : Long.parseLong(val));
						} else if ("Profit Target Time".equals(column)) {
							rec.setProfitTargetTime(val.isEmpty() ? null : df.parse(val));
						} else if ("Profit Target Time (time_t)".equals(column)) {
							rec.setProfitTargetTimeT(val.isEmpty() ? null : Long.parseLong(val));
						}  else if ("MFE".equals(column)) {
							rec.setMfe(val.isEmpty() ? null : Double.parseDouble(val));
						} else if ("MFE Time".equals(column)) {
							rec.setMfeTime(val.isEmpty() ? null : df.parse(val));
						} else if ("MFE Time (time_t)".equals(column)) {
							rec.setMfeTimeT(val.isEmpty() ? null : Long.parseLong(val));
						} else if ("MAE".equals(column)) {
							rec.setMae(val.isEmpty() ? null : Double.parseDouble(val));
						} else if ("MAE Time".equals(column)) {
							rec.setMaeTime(val.isEmpty() ? null : df.parse(val));
						} else if ("MAE Time (time_t)".equals(column)) {
							rec.setMaeTimeT(val.isEmpty() ? null : Long.parseLong(val));
						} else {
							try {
								Double value = val.isEmpty() ? null : Double.parseDouble(val);
								rec.getData().put(column, value);
							} catch (Exception e) {
								failedCols.add(column);
							}
						}
					}
					
					if (!isShort) {
						if (Result.Winner.toString().equals(rec.getResultStr())
								&& rec.getExitPrice() < rec.getEntryPrice() - 0.02) {
							isShort = true;
						} else if (Result.Loser.toString().equals(rec.getResultStr())
								&& rec.getExitPrice() > rec.getEntryPrice() + 0.02) {
							isShort = true;
						}
					}
					
					//Filter the data if a preFilter is provided
					if (!isTargetStopFile && preFilter != null && !preFilter.isEmpty()) {
						boolean filterNotMatched = false;
						for (FilterRange filter : preFilter) {
							if (!Utility.matchFilter(rec, filter)) {
								filterNotMatched = true;
								break;
							}
						}
						if (filterNotMatched) {
							continue;
						}
					}
					
					rec.setId(id++);
					
					//TODO: Potentially change to just moved
					if (!isShort) {
						rec.setPriceDiff(rec.getExitPrice() - rec.getEntryPrice());
					} else {
						rec.setPriceDiff(rec.getEntryPrice() - rec.getExitPrice());
					}
					
					double profitPercent = (rec.getPriceDiff())/rec.getEntryPrice();
					rec.setProfitPercent(profitPercent);
					
					if ("Profit Target".equals(rec.getReasonForExit())) {
						profitTargetDiff.add(Math.round(Math.abs(rec.getPriceDiff())*10000.0) / 10000.0);
						profitTargetPercDiff.add(Math.round((Math.abs(rec.getPriceDiff()) / rec.getEntryPrice())*10000.0) / 100.0);
						rec.setExitTime(rec.getProfitTargetTime());
						rec.setExitTimeT(rec.getProfitTargetTimeT());
					} else if ("Stop Loss".equals(rec.getReasonForExit())) {
						stopLossDiff.add(Math.round(Math.abs(rec.getPriceDiff())*10000.0) / 10000.0);
						stopLossPercDiff.add(Math.round((Math.abs(rec.getPriceDiff()) / rec.getEntryPrice())*10000.0) / 100.0);
						rec.setExitTime(rec.getStopLossTime());
						rec.setExitTimeT(rec.getStopLossTimeT());
					} else if("Timeout".equals(rec.getReasonForExit())) {
						Calendar cal = Calendar.getInstance();
						cal.setTime(rec.getTimeoutTime());
						
						int hour = cal.get(Calendar.HOUR_OF_DAY);
						int minute = cal.get(Calendar.MINUTE);
						int second = cal.get(Calendar.SECOND);
						
						int secondsFromOpen = (hour - 9)*60*60 + (minute - 30)*60 + second;
						
						secondsFromOpenList.add(secondsFromOpen);
						timeoutTimeStr.add(df2.format(rec.getTimeoutTime()));
						
						cal = Calendar.getInstance();
						cal.setTime(rec.getEntryTime());
						int entryMinutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.HOUR_OF_DAY)*60;
						
						cal.setTime(rec.getTimeoutTime());
						int exitMinutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.HOUR_OF_DAY)*60;

						timeoutMinutes.add(exitMinutes - entryMinutes);
						rec.setExitTime(rec.getTimeoutTime());
						rec.setExitTimeT(rec.getTimeoutTimeT());
					} else if("Exit Alert".equals(rec.getReasonForExit())) {
						rec.setExitTime(rec.getExitAlertTime());
						rec.setExitTimeT(rec.getExitAlertTimeT());
					}

					if (isTargetStopFile || !trades.contains(rec)) {
						trades.add(rec);
					}
				}

				runParam = new TIRunParameters();
				if (profitTargetDiff.size() == 1) {
					runParam.setPriceExitType(PriceExitType.dollar);
					runParam.setProfitTarget(profitTargetDiff.last());
				} else if(profitTargetPercDiff.size() == 1) {
					runParam.setPriceExitType(PriceExitType.percent);
					runParam.setProfitTarget(profitTargetPercDiff.last());
				}
				
				if (stopLossDiff.size() == 1) {
					runParam.setPriceExitType(PriceExitType.dollar);
					runParam.setStopLoss(stopLossDiff.last());
				} else if(stopLossPercDiff.size() == 1) {
					runParam.setPriceExitType(PriceExitType.percent);
					runParam.setStopLoss(stopLossPercDiff.last());
				}
				
				double percentOfMostValue = secondsFromOpenList.size() != 0 ? percentOfMostValue(secondsFromOpenList) : 999999.0;
				
				if (secondsFromOpenList.size() != 0 && percentOfMostValue > 90) { //Majority of trades timed out at the same time - indicating a time out time.
					runParam.setTimeExitType(TimeExitType.time);
					runParam.setExitTimeOfDay(df2.parse(timeoutTimeStr.last()));
					
					Calendar cal = Calendar.getInstance();
					cal.setTime(runParam.getExitTimeOfDay());
					int hour = cal.get(Calendar.HOUR_OF_DAY);
					int minutes = cal.get(Calendar.MINUTE);
					
					int minutesFromOpen = 0;
					if (hour <= 12 && hour >= 9) {
						minutesFromOpen = (int) ((hour - 9.5) * 60 + minutes);
						runParam.setMinutesFromOpen(minutesFromOpen);
					} else {
						minutesFromOpen = (int) ((12 - 9.5) * 60 + (hour * 60) + minutes);
					}
					
					runParam.setMinutesFromOpen(minutesFromOpen);
				} else if (timeoutMinutes.size() != 0 && timeoutMinutes.size()*1.0/trades.size()*1.0 < 0.1) {
					runParam.setTimeExitType(TimeExitType.minutes);
					runParam.setMinutesFromEntry(timeoutMinutes.last());
				}

				for (String col : failedCols) {
					System.out.println("ISSUE PROCESSING COLUMN DATA: " + col);
				}
			}
		}

		this.ranges.overallDataDaysRange = getWorkingDaysBetweenTwoDates(this.ranges.overallMinTradeDate, this.ranges.overallMaxTradeDate);

		Collection<TradeRecord> trades = this.getTrades();
		
		List<TradeRecord> sortedTrades = new ArrayList<TradeRecord>();
		for (TradeRecord trade : trades) {
			sortedTrades.add(trade);
		}

		Collections.sort(sortedTrades, new Comparator<TradeRecord>() {
			public int compare(TradeRecord obj1, TradeRecord obj2) {
				return obj1.getEntryTime().compareTo(obj2.getEntryTime());
			}
		});

		this.setTrades(sortedTrades);
		
		if (percentOfDaysForInSampleTest < 100) {
			updateDataWithInSampleFlag(percentOfDaysForInSampleTest,
					percentOfDaysForFinalTest > (100 - percentOfDaysForInSampleTest)
							? (100 - percentOfDaysForInSampleTest)
							: percentOfDaysForFinalTest);
		}

		Date inSampleMinTradeDate = null;
		Date inSampleMaxTradeDate = null;
		
		Date outOfSampleMinTradeDate = null;
		Date outOfSampleMaxTradeDate = null;

		Date finalMinTradeDate = null;
		Date finalMaxTradeDate = null;
		
		for (TradeRecord rec : this.trades) {
			if (rec.isInSample()) {
				if (inSampleMinTradeDate == null) {
					inSampleMinTradeDate = rec.getEntryTime();
				}

				inSampleMaxTradeDate = rec.getEntryTime();
			} else if (rec.isOutOfSample()) {
				if (outOfSampleMinTradeDate == null) {
					outOfSampleMinTradeDate = rec.getEntryTime();
				}

				outOfSampleMaxTradeDate = rec.getEntryTime();
			} else if(rec.isFinalTestSample()) {
				if (finalMinTradeDate == null) {
					finalMinTradeDate = rec.getEntryTime();
				}

				finalMaxTradeDate = rec.getEntryTime();
			}
		}

		this.ranges.inSampleMinTradeDate = inSampleMinTradeDate;
		this.ranges.inSampleMaxTradeDate = inSampleMaxTradeDate;
		
		this.ranges.outOfSampleMinTradeDate = outOfSampleMinTradeDate;
		this.ranges.outOfSampleMaxTradeDate = outOfSampleMaxTradeDate;

		this.ranges.finalMinTradeDate = finalMinTradeDate;
		this.ranges.finalMaxTradeDate = finalMaxTradeDate;
		
		this.ranges.inSampleDataDaysRange = getWorkingDaysBetweenTwoDates(inSampleMinTradeDate, inSampleMaxTradeDate);
		
		if (percentOfDaysForInSampleTest < 100 && outOfSampleMinTradeDate != null && outOfSampleMaxTradeDate != null) {
			this.ranges.outOfSampleDataDaysRange = getWorkingDaysBetweenTwoDates(outOfSampleMinTradeDate, outOfSampleMaxTradeDate);
		}
		
		if (percentOfDaysForFinalTest > 0 && finalMinTradeDate != null && finalMaxTradeDate != null) {
			this.ranges.finalDataDaysRange = getWorkingDaysBetweenTwoDates(finalMinTradeDate, finalMaxTradeDate);
		}

		System.out.println();
		System.out.println("Successfully read all trade data\n");
		System.out.println("Trade Count = " + trades.size() + " ... spanning: In Sample =  " + this.ranges.inSampleDataDaysRange + " days, Out of Sample = " + this.ranges.outOfSampleDataDaysRange + " days.\n");
		System.out.println("Detected Run Parameters: ");
		System.out.println("Exit Type = " + (runParam.getTimeExitType() == null ? "None Detected"
				: runParam.getTimeExitType().equals(TimeExitType.time) ? "Time of Day: " + df3.format(runParam.getExitTimeOfDay())
						: "Minutes from Entry: " + runParam.getMinutesFromEntry()));
		System.out.println("Profit Target = " + (runParam.getPriceExitType() == null || runParam.getProfitTarget() == 0.0 ? "None Detected"
				: runParam.getPriceExitType().equals(PriceExitType.dollar) ? "$" + runParam.getProfitTarget()
						: runParam.getProfitTarget() + "%"));
		System.out.println("Stop Loss = " + (runParam.getPriceExitType() == null ? "None Detected"
				: runParam.getPriceExitType().equals(PriceExitType.dollar) ? "$" + runParam.getStopLoss()
						: runParam.getStopLoss() + "%"));
		System.out.println();
	}

	private void updateDataWithInSampleFlag(int percentOfDaysForInSampleTest, int percentOfDaysForFinalTest) {
		int outOfSampleDaysMark = (int) (this.ranges.overallDataDaysRange * (percentOfDaysForInSampleTest * 1.0 / 100.0));
		int finalTestDaysMark = (int) (this.ranges.overallDataDaysRange * ((100 - percentOfDaysForFinalTest) * 1.0 / 100.0));

		Date outOfSampleMark = addWorkingDays(this.ranges.overallMinTradeDate, outOfSampleDaysMark);
		Date finalTestMark = addWorkingDays(this.ranges.overallMinTradeDate, finalTestDaysMark);

		for (TradeRecord rec : this.getTrades()) {
			if (rec.getEntryTime().after(finalTestMark)) {
				rec.setFinalTestSample(true);
			} else if (rec.getEntryTime().after(outOfSampleMark)) {
				rec.setOutOfSample(true);
			}
		}
	}
	
	private double percentOfMostValue(List<Integer> values) {
		Map<Integer, Integer> map = new HashMap<>();
		
		for (Integer i : values) {
			if (map.containsKey(i)) {
				int count = map.get(i);
				map.put(i, count + 1);
			} else {
				map.put(i, 1);
			}
		}

		Optional<Entry<Integer, Integer>> maxEntry = map.entrySet().stream()
				.max(Comparator.comparing(Map.Entry::getValue));
		int maxCount = maxEntry.get().getValue();
		
		return (maxCount*1.0 / values.size())*100.0;
	}
	
	/*private double getStandardDeviation(List<Integer> values) {
		int sum = 0;
		int avg = 0;
		for (int val : values) {
			sum += val;
		}

		avg = sum / values.size();
		List<Double> squareList = new ArrayList<>();
		for (int val : values) {
			double square = Math.pow(val - avg, 2);
			squareList.add(square);
		}

		sum = 0;
		avg = 0;
		for (double val : squareList) {
			sum += val;
		}

		avg = sum / squareList.size();

		double stdDev = Math.sqrt(avg);

		return stdDev;
	}*/
	
	public static int getWorkingDaysBetweenTwoDates(Date startDate, Date endDate) {
	    Calendar startCal = Calendar.getInstance();
	    startCal.setTime(startDate);

	    Calendar endCal = Calendar.getInstance();
	    endCal.setTime(endDate);

	    int workDays = 0;

	    if (startCal.getTimeInMillis() == endCal.getTimeInMillis()) {
	        return 0;
	    }

	    if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
	        startCal.setTime(endDate);
	        endCal.setTime(startDate);
	    }

	    do {
	        startCal.add(Calendar.DAY_OF_MONTH, 1);
	        if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
	            ++workDays;
	        }
	    } while (startCal.getTimeInMillis() < endCal.getTimeInMillis()); //excluding end date

	    return workDays;
	}
	
	public static Date addWorkingDays(Date startDate, int workingDays) {
	    Calendar startCal = Calendar.getInstance();
	    startCal.setTime(startDate);        

	    int workDays = 0;

	    do {
	        startCal.add(Calendar.DAY_OF_MONTH, 1);
	        if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
	            ++workDays;
	        }
	    } while (workDays < workingDays);

	    return startCal.getTime();
	}
	
	public Set<String> getDistinctColumns() {
		Set<String> distinctColumns = new HashSet<>();
		
		Pattern pattern = Pattern.compile("^[U][0-9]{1,2}$");
		
		for (TradeRecord rec : trades) {
			for (String col : rec.getData().keySet()) {
				if (!distinctColumns.contains(col)) {
					String code = col.split("\\[")[1].split("\\]")[0];

					Matcher matcher = pattern.matcher(code);
					boolean matchFound = matcher.find();

					if (!matchFound) {
						distinctColumns.add(col);
					}
				}
			}
		}

		return distinctColumns;
	}

	public static class TradeDataRanges implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Date overallMinTradeDate;
		private Date overallMaxTradeDate;
		
		private Date inSampleMinTradeDate;
		private Date inSampleMaxTradeDate;
		
		private Date outOfSampleMinTradeDate;
		private Date outOfSampleMaxTradeDate;
		
		private Date finalMinTradeDate;
		private Date finalMaxTradeDate;
		
		private int overallDataDaysRange;
		
		protected int inSampleDataDaysRange;
		
		private int outOfSampleDataDaysRange;
		
		private int finalDataDaysRange;

		public Date getOverallMinTradeDate() {
			return overallMinTradeDate;
		}

		public void setOverallMinTradeDate(Date overallMinTradeDate) {
			this.overallMinTradeDate = overallMinTradeDate;
		}

		public Date getOverallMaxTradeDate() {
			return overallMaxTradeDate;
		}

		public void setOverallMaxTradeDate(Date overallMaxTradeDate) {
			this.overallMaxTradeDate = overallMaxTradeDate;
		}

		public Date getInSampleMinTradeDate() {
			return inSampleMinTradeDate;
		}

		public void setInSampleMinTradeDate(Date inSampleMinTradeDate) {
			this.inSampleMinTradeDate = inSampleMinTradeDate;
		}

		public Date getInSampleMaxTradeDate() {
			return inSampleMaxTradeDate;
		}

		public void setInSampleMaxTradeDate(Date inSampleMaxTradeDate) {
			this.inSampleMaxTradeDate = inSampleMaxTradeDate;
		}

		public Date getOutOfSampleMinTradeDate() {
			return outOfSampleMinTradeDate;
		}

		public void setOutOfSampleMinTradeDate(Date outOfSampleMinTradeDate) {
			this.outOfSampleMinTradeDate = outOfSampleMinTradeDate;
		}

		public Date getOutOfSampleMaxTradeDate() {
			return outOfSampleMaxTradeDate;
		}

		public void setOutOfSampleMaxTradeDate(Date outOfSampleMaxTradeDate) {
			this.outOfSampleMaxTradeDate = outOfSampleMaxTradeDate;
		}

		public Date getFinalMinTradeDate() {
			return finalMinTradeDate;
		}

		public void setFinalMinTradeDate(Date finalMinTradeDate) {
			this.finalMinTradeDate = finalMinTradeDate;
		}

		public Date getFinalMaxTradeDate() {
			return finalMaxTradeDate;
		}

		public void setFinalMaxTradeDate(Date finalMaxTradeDate) {
			this.finalMaxTradeDate = finalMaxTradeDate;
		}

		public int getOverallDataDaysRange() {
			return overallDataDaysRange;
		}

		public void setOverallDataDaysRange(int overallDataDaysRange) {
			this.overallDataDaysRange = overallDataDaysRange;
		}

		public int getInSampleDataDaysRange() {
			return inSampleDataDaysRange;
		}

		public void setInSampleDataDaysRange(int inSampleDataDaysRange) {
			this.inSampleDataDaysRange = inSampleDataDaysRange;
		}

		public int getOutOfSampleDataDaysRange() {
			return outOfSampleDataDaysRange;
		}

		public void setOutOfSampleDataDaysRange(int outOfSampleDataDaysRange) {
			this.outOfSampleDataDaysRange = outOfSampleDataDaysRange;
		}

		public int getFinalDataDaysRange() {
			return finalDataDaysRange;
		}

		public void setFinalDataDaysRange(int finalDataDaysRange) {
			this.finalDataDaysRange = finalDataDaysRange;
		}
	}
	
	public Collection<TradeRecord> getTrades() {
		return trades;
	}

	public void setTrades(Collection<TradeRecord> trades) {
		this.trades = trades;
	}

	public TIRunParameters getRunParam() {
		return runParam;
	}

	public void setRunParam(TIRunParameters runParam) {
		this.runParam = runParam;
	}

	public List<FilterRange> getFilters() {
		return filters;
	}

	public void setFilters(List<FilterRange> filters) {
		this.filters = filters;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public File getWtiFile() {
		return wtiFile;
	}

	public void setWtiFile(File wtiFile) {
		this.wtiFile = wtiFile;
	}

	public TradeDataRanges getRanges() {
		return ranges;
	}

	public void setRanges(TradeDataRanges ranges) {
		this.ranges = ranges;
	}

	public boolean isShort() {
		return isShort;
	}
}
