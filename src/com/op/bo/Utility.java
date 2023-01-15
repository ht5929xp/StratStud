package com.op.bo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Stream;
import java.util.Map.Entry;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;

import com.op.bo.GemParameters.GemReviewResult;
import com.op.bo.targetstop.TargetStopComboStrategyResult;
import com.op.bo.targetstop.TargetStopInfo;
import com.op.bo.targetstop.TargetStopStrategyResult;

public class Utility {
	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		try {
			BigDecimal bd = BigDecimal.valueOf(value);
			bd = bd.setScale(places, RoundingMode.HALF_UP);
			return bd.doubleValue();
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0.0;
		}
	}
	
	public static double roundUp(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		try {
			BigDecimal bd = BigDecimal.valueOf(value);
			bd = bd.setScale(places, RoundingMode.UP);
			return bd.doubleValue();
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0.0;
		}
	}
	
	public static double roundDown(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		try {
			BigDecimal bd = BigDecimal.valueOf(value);
			bd = bd.setScale(places, RoundingMode.DOWN);
			return bd.doubleValue();
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 0.0;
		}
	}
	
	public static String roundStr(double value, int places) {
		if (places < 0)	throw new IllegalArgumentException();

		if (Double.isInfinite(value)) {
			return "infinite";
		} else if (Double.isNaN(value)) {
			return "NaN";
		}

		BigDecimal bd = BigDecimal.valueOf(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return String.valueOf(bd.doubleValue());
	}
	
	public static <T> List<T> lastX(List<T> list, int x) {
		return list.subList(list.size() - Math.min(list.size(), x), list.size());
	}
	
	public static String getAvgWinnerLoserStr(double avgWinner, double avgLoser) {
		return "%" + roundStr(avgWinner, 2) + " / %" + roundStr(avgLoser, 2);
	}
	
	public static String getColumnCode(String column) {
		return column.split("\\[")[1].split("\\]")[0];
	}
	
	public static TargetStopInfo getTargetStopInfo(TradeRecord rec, double target, double stop, TargetStopComboStrategyResult targetStopComboResult, boolean isShort) {
		boolean stopHit = false;
		boolean targetHit = false;
		Date estimatedExitDate = null;

		Date minMaeHitTime = rec.getMinDateByStopPrice() != null ? rec.getMinDateByStopPrice().get(stop) : null;
		Date minMfeHitTime = rec.getMinDateByTargetPrice() != null ? rec.getMinDateByTargetPrice().get(target): null;

		double exitPrice = 0.0;
		if (minMaeHitTime == null && minMfeHitTime == null) { // Timeout
			exitPrice = rec.getTimeoutPrice();
			estimatedExitDate = rec.getExitTime();
		} else if (minMfeHitTime != null && minMaeHitTime == null) { // Target Hit
			exitPrice = target;
			targetHit = true;
			estimatedExitDate = minMfeHitTime;
		} else if (minMfeHitTime == null && minMaeHitTime != null) { // Stop Hit
			exitPrice = stop;
			stopHit = true;
			estimatedExitDate = minMaeHitTime;
		} else if (minMfeHitTime != null && minMaeHitTime != null && (minMaeHitTime.before(minMfeHitTime) || minMaeHitTime.equals(minMfeHitTime))) { // Stop Hit (Rough Estimate)
			exitPrice = stop;
			stopHit = true;
			estimatedExitDate = minMaeHitTime;
		} else if (minMfeHitTime != null && minMaeHitTime != null && minMfeHitTime.before(minMaeHitTime)) { // Target Hit (Rough Estimate)
			exitPrice = target;
			targetHit = true;
			estimatedExitDate = minMfeHitTime;
		} else {
			System.err.println("INVALID DATA !!!!");
		}

		if (targetHit == true) {
			long seconds = (estimatedExitDate.getTime() - rec.getEntryTime().getTime()) / 1000;
			if (seconds >= 0 && seconds <= 60) {
				Double changeInOneMinuteObj = rec.getData().get("Change 1 Minute ($) [DUp1]");
				double changeInOneMinute = changeInOneMinuteObj != null ? Math.abs(changeInOneMinuteObj) : 0;
				double targetDiff = Math.abs(target - rec.getEntryPrice());
				 // 60% chance to stop hit or timeout if 1 minute trade or if 1 minute change is more than target.
				if (changeInOneMinute > targetDiff || Math.random() < 0.6) {
					if (minMaeHitTime != null) {
						exitPrice = stop;
						stopHit = true;
						targetHit = false;
						estimatedExitDate = minMaeHitTime;
					} else {
						exitPrice = rec.getTimeoutPrice();
						estimatedExitDate = rec.getExitTime();
						stopHit = false;
						targetHit = false;
					}
				}
			}
		}
	
		boolean win = !isShort ? exitPrice > rec.getEntryPrice() : rec.getEntryPrice() > exitPrice;
		
		TargetStopInfo info = new TargetStopInfo();
		info.setTradeId(rec.getId());
		info.setSymbol(rec.getSymbol());
		info.setEntryDate(rec.getEntryTime());
		info.setEntryDay(rec.getEntryDay());
		info.setEntryPrice(rec.getEntryPrice());
		info.setExitPrice(exitPrice);
		info.setTargetStopComboResult(targetStopComboResult);
		info.setTargetPrice(target);
		info.setStopPrice(stop);
		info.setWin(win);
		info.setStopHit(stopHit);
		info.setTargetHit(targetHit);
		info.setEstimatedExitDate(estimatedExitDate);
		info.setPriceDiff(!isShort ? info.getExitPrice() - info.getEntryPrice() : info.getEntryPrice() - info.getExitPrice());
		if (info.getPriceDiff() > 0.0) {
			info.setProfitPercent((info.getPriceDiff() / info.getEntryPrice()) * 100.0);
		} else {
			info.setLossPercent((-info.getPriceDiff() / info.getEntryPrice()) * 100.0);
		}

		return info;
	}

	public static List<GemParameters> readGemParameters(File parentDir, File file) {
		return readGemParameters(parentDir, file, 0, null);
	}
	
	/**
	 * Reads the parameters from the gem results Excel file provided. Based on which have the ID highlighted yellow.
	 * @param file
	 * @return
	 */
	public static List<GemParameters> readGemParameters(File parentDir, File file, int gemId, String strategyName) {
		List<GemParameters> gemParam = new ArrayList<>();

		if (!file.exists()) {
			return gemParam;
		}

		final Set<String> RESULT_TYPES = new HashSet<>(Arrays.asList("maxPf", "maxPfWin", "maxPfWinTrades", "maxWin", "maxWinTrades",
				"maxQIndex", "maxQIndex-ByCol", "maxPf-ByCol", "maxPfWin-ByCol", "maxPfWinTrades-ByCol", "maxWin-ByCol",
				"maxWinTrades-ByCol"));

		String resultType = null;
		String fileName = file.getName();
		for(String rType : RESULT_TYPES) {
			int indexOfResultType = fileName.indexOf(rType);
			if (indexOfResultType > -1) {
				strategyName = fileName.substring(0, indexOfResultType - 1);
				resultType = rType;
				break;
			}
		}
		
		Date creationDateTime = null;
		Date lastAccessedDateTime = null;
		Date lastModifiedDateTime = null;
		try {
			BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

			FileTime creationTime = attr.creationTime();
			FileTime lastAccessedTime = attr.lastAccessTime();
			FileTime lastModifiedTime = attr.lastModifiedTime();

			creationDateTime = new Date(creationTime.toMillis());
			lastAccessedDateTime = new Date(lastAccessedTime.toMillis());
			lastModifiedDateTime = new Date(lastModifiedTime.toMillis());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		String runConfig = null;
		if (parentDir.getName().contains("[") && parentDir.getName().contains("]")) {
			runConfig = parentDir.getName().substring(parentDir.getName().indexOf("[") + 1, parentDir.getName().indexOf("]"));
		}
		
		if (strategyName == null) {
			System.err.println("Could not read strategy name for file: " + fileName);
			return gemParam;
		}
		
		try (XSSFWorkbook wb = XSSFWorkbookFactory.createWorkbook(file, true);) {
			XSSFSheet sheet = wb.getSheet("Stats");
			
			//For Smart Stop cases where min stop was not entered into strategy sheet
			double minStopLossDollar = readMinAllowedStopLossFromRunParamSheet(wb.getSheet("Run Param"));

			GemParameters param = null;
			boolean readingFilters = false;
			boolean readingGem = false;
			for(Row r : sheet) {
				Cell cell = r.getCell(0);
				String cellContent = cell != null ? cell.getCellType() == CellType.NUMERIC ? String.valueOf((int)cell.getNumericCellValue()) : cell.getStringCellValue() : "";
				
				if (cell != null) {
					CellStyle style = cell.getCellStyle();
					Color fillColor = style.getFillForegroundColorColor();

					if (isNumeric(cellContent)) {
						if ((Integer.parseInt(cellContent) == gemId) || 
								(fillColor != null && 
								("FFFFFF00".equals(((XSSFColor) fillColor).getARGBHex()) // Yellow
								|| "FF00B050".equals(((XSSFColor) fillColor).getARGBHex()) // Green
								|| "FFFFC000".equals(((XSSFColor) fillColor).getARGBHex()) // Orange
								|| "FF7030A0".equals(((XSSFColor) fillColor).getARGBHex())))) { // Purple
							
							
							
							boolean pendingReview = false;
							boolean reviewed = false;
							boolean successful = false;
							boolean paperTrading = false;
							
							if (fillColor != null) {
								if ("FFFFFF00".equals(((XSSFColor) fillColor).getARGBHex())) {
									pendingReview = true;
								} else if ("FF00B050".equals(((XSSFColor) fillColor).getARGBHex())) {
									successful = true;
								} else if ("FFFFC000".equals(((XSSFColor) fillColor).getARGBHex())) {
									reviewed = true;
								} else if ("FF7030A0".equals(((XSSFColor) fillColor).getARGBHex())) {
									paperTrading = true;
								}
							}
							
							readingGem = true;
							readingFilters = false;

							param = new GemParameters();
							param.setGemFileLastAccessedDate(lastAccessedDateTime);
							param.setGemFileCreatedDate(creationDateTime);
							param.setGemFileLastModifiedDate(lastModifiedDateTime);
							param.setStrategyName(strategyName);
							param.setGemDirName(parentDir.getName());
							param.setFileName(fileName);
							param.setStrategyId(Integer.parseInt(cellContent));
							param.setResultType(resultType);
							param.setRunConfigName(runConfig);
							param.setReviewResult(pendingReview ? GemReviewResult.PendingReview
									: reviewed ? GemReviewResult.Reviewed
											: successful ? GemReviewResult.Successful
													: paperTrading ? GemReviewResult.PaperTrading : null);
							gemParam.add(param);
						} else {
							readingGem = false;
							readingFilters = false;
						}
					}
				}

				if (readingGem) {
					if ("Target Strategy".equalsIgnoreCase(cellContent.trim())) {
						String targetDesc = r.getCell(1).getStringCellValue();
						String[] twoParts = targetDesc.split("\\(");
						if(twoParts.length > 2) {
							twoParts = targetDesc.split("\\(min");
						}
						
						String part1 = twoParts[0]; // target/stop
						String numberPart1Str = part1.replaceAll("[^\\d.]", "");
						
						if (twoParts.length == 2) {
							String part2 = twoParts[1]; // min target/stop
							String numberPart2Str = part2.replaceAll("[^\\d.]", "");
							double minTarget = Double.parseDouble(numberPart2Str);
							param.setMinTargetDollar(minTarget);
						}
						
						if (targetDesc.contains("Specific Dollar Amount Strategy:")) {
							double targetDollar = Double.parseDouble(numberPart1Str);
							param.setTargetDollar(targetDollar);
						} else if (targetDesc.contains("Percent of Price Strategy:")) {
							double targetPercent = Double.parseDouble(numberPart1Str);
							param.setTargetPercent(targetPercent);
						} else if (targetDesc.contains("Percent of ATR Strategy:")) {
							double targetPercent = Double.parseDouble(numberPart1Str);
							param.setTargetPercentATR(targetPercent);
						}
					} else if ("Stop Loss Strategy".equalsIgnoreCase(cellContent.trim())) {
						String stopDesc = r.getCell(1).getStringCellValue();
						String[] twoParts = stopDesc.split("\\(");
						if(twoParts.length > 2) {
							twoParts = stopDesc.split("\\(min");
						}
						
						String part1 = twoParts[0]; // target/stop
						String numberPart1Str = part1.replaceAll("[^\\d.]", "");
						
						if (twoParts.length == 2) {
							String part2 = twoParts[1]; // min target/stop
							String numberPart2Str = part2.replaceAll("[^\\d.]", "");
							
							if (!numberPart2Str.trim().isEmpty()) {
								double minStop = Double.parseDouble(numberPart2Str);
								param.setMinStopLossDollar(minStop);
							}
						}
						
						if (stopDesc.toUpperCase().contains("SMART STOP")) {
							param.setSmartStop(true);
							if (param.getMinStopLossDollar() == 0.0 && minStopLossDollar != 0.0) {
								param.setMinStopLossDollar(minStopLossDollar);
							}
						}
						
						if (stopDesc.contains("Specific Dollar Amount Strategy:")) {
							double stopLossDollar = Double.parseDouble(numberPart1Str);
							param.setStopLossDollar(stopLossDollar);
						} else if (stopDesc.contains("Percent of Price Strategy:")) {
							double stopLossPercent = Double.parseDouble(numberPart1Str);
							param.setStopLossPercent(stopLossPercent);
						} else if (stopDesc.contains("Percent of ATR Strategy:")) {
							double stopLossPercent = Double.parseDouble(numberPart1Str);
							param.setStopLossPercentATR(stopLossPercent);
						}
					} else if ("Trade Count".equalsIgnoreCase(cellContent.trim())) {
						Cell inSampleCountCell = r.getCell(1);
						if (inSampleCountCell != null) {
							int inSampleCount = (int) Double.parseDouble(inSampleCountCell.getStringCellValue());
							param.setGemInSampleTradeCount(inSampleCount);
						}
						Cell overallCountCell = r.getCell(3);
						if (overallCountCell != null) {
							int overallCount = (int) Double.parseDouble(overallCountCell.getStringCellValue());
							param.setGemOverallTradeCount(overallCount);
						}
						Cell finalCountCell = r.getCell(4);
						if (finalCountCell != null) {
							int finalCount = (int) Double.parseDouble(finalCountCell.getStringCellValue());
							param.setGemFinalTradeCount(finalCount);
						}
					} else if ("Filter(s)".equalsIgnoreCase(cellContent.trim())) {
						readingFilters = true;
					} else if (readingFilters) {
						Cell minCell = r.getCell(1);
						Cell maxCell = r.getCell(2);
						
						if (minCell != null && maxCell != null) {
							double min = r.getCell(1).getNumericCellValue();
							double max = r.getCell(2).getNumericCellValue();
							FilterRange filter = new FilterRange(cellContent.trim(), min, max);
							param.getFilters().add(filter);
						}
					}
				}
			}
			
		} catch (InvalidFormatException | IOException | NotOfficeXmlFileException e) {
			System.err.println("Could not read Excel file: " + file.getName());
		}
		
		for(GemParameters param : gemParam) {
			System.out.println("!!!!   " + param);
		}
		
		return gemParam;
	}
	
	private static boolean isNumeric(String strNum) {
		if (strNum == null) {
	        return false;
	    }
	    try {
	        Double.parseDouble(strNum);
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	    return true;
	}

	private static double readMinAllowedStopLossFromRunParamSheet(XSSFSheet sheet) {
		double minStopLoss = 0.0;
		
		for (Row r : sheet) {
			Cell cell = r.getCell(0);
			String cellContent = cell != null
					? cell.getCellType() == CellType.NUMERIC ? String.valueOf((int) cell.getNumericCellValue())	: cell.getStringCellValue()	: "";

			if (cell != null) {
				if ("Min Allowed Stop Loss".equalsIgnoreCase(cellContent.trim())) {
					Cell minStopLossCell = r.getCell(1);
					if (minStopLossCell != null) {
						minStopLoss = Double.parseDouble(minStopLossCell.getStringCellValue());
					}
				}
			}
		}

		return minStopLoss;
	}
	
	public static List<TradeData> loadTradeData(File dataDir, int percentOfDaysForInSampleTest, int percentOfDaysForFinalTest, Map<String, PriceLevels> priceLevelsData) throws IOException, ParseException {
		return loadTradeData(dataDir, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, priceLevelsData, null);
	}
	
	public static List<TradeData> loadTradeData(File dataDir, int percentOfDaysForInSampleTest, int percentOfDaysForFinalTest, Map<String, PriceLevels> priceLevelsData, List<FilterRange> preFilter) throws IOException, ParseException {		
		List<TradeData> tradeData = new ArrayList<>();
		File wtiFile = null;
		
		File[] allFiles = dataDir.listFiles();

		boolean multipleGroups = isMultipleGroups(allFiles);

		System.out.println("\r\nMultiple Groups Flag = " + multipleGroups + "\n");

		Map<String, List<File>> runGroups = new HashMap<String, List<File>>();
		if (multipleGroups) {
			for (File f : allFiles) {
				if (isDataFile(f) && !isTargetStopFile(f)) {
					String fileName = f.getName();
					String[] array = fileName.split(PathResources.FILE_NAME_SEPARATOR);
					
					String groupName = array[0];
		
					if (runGroups.containsKey(groupName)) {
						runGroups.get(groupName).add(f);
					} else {
						List<File> list = new ArrayList<File>();
						list.add(f);
						runGroups.put(groupName, list);
					}
				} else if(isWTIFile(f)) {
					wtiFile = f;
				}
			}
		} else {
			List<File> allFilesList = new ArrayList<File>();
			for (File f : allFiles) {
				if (isDataFile(f) && !isTargetStopFile(f)) {
					allFilesList.add(f);
				} else if(isWTIFile(f)) {
					wtiFile = f;
				}
			}

			String fileName = allFilesList.get(0).getName();
			String[] array = fileName.split(PathResources.FILE_NAME_SEPARATOR);
			
			String groupName = array[0];
			
			runGroups.put(groupName, allFilesList);
		}
				
		for (Entry<String, List<File>> entry : runGroups.entrySet()) {
			System.out.println("Loading data for trade data group: " + entry.getKey() + "\n");
			List<File> dataFiles = entry.getValue();
			TradeData data = new TradeData(dataFiles, percentOfDaysForInSampleTest, percentOfDaysForFinalTest, preFilter);
			data.setWtiFile(wtiFile);
			tradeData.add(data);
		}

		for (TradeData data : tradeData) {
			for (TradeRecord rec : data.getTrades()) {
				rec.setPriceLevels(getPriceLevelsInRange(priceLevelsData, rec.getSymbol(), rec.getEntryTime(), rec.getTimeoutTime()));
			}
		}

		if (tradeData.size() > 1) {
			String groupName = getGroupNameFromUser();
			for (TradeData data : tradeData) {
				data.setGroupName(groupName);
			}
		}
		
		return tradeData;
	}

	private static String getGroupNameFromUser() {
		Scanner in = new Scanner(System.in);
	    System.out.println("Enter Group Name: ");

	    String groupName = in.nextLine();
	    in.close();
	    
	    return groupName;
	}
	
	public static boolean isDataFile(File f) {
		return !f.isDirectory() && f.getName().endsWith(".csv");
	}
	
	public static boolean isWTIFile(File f) {
		return !f.isDirectory() && f.getName().endsWith(".WTI");
	}
	
	public static List<PriceLevel> getPriceLevelsInRange(Map<String, PriceLevels> priceLevelsData, String symbol, Date entryTime, Date timeoutTime) {
		PriceLevels priceLevels = priceLevelsData.get(symbol);

		List<PriceLevel> result = new ArrayList<>();
		if (priceLevels != null) {
			for (PriceLevel pLevel : priceLevels.getPriceLevels()) {
				if ((pLevel.getDate().after(entryTime) || pLevel.getDate().equals(entryTime))
						&& (pLevel.getDate().before(timeoutTime) || pLevel.getDate().equals(timeoutTime))) {
					result.add(new PriceLevel(pLevel.getSymbol(), pLevel.getDate(), pLevel.getPrice()));
				}
			}
		}

		return result;
	}
	
	public static boolean isMultipleGroups(File[] allFiles) {
		Set<String> names = new HashSet<>();
		for (File f : allFiles) {
			if (isDataFile(f)) {
				String fileName = f.getName();
				String[] array = fileName.split(PathResources.FILE_NAME_SEPARATOR);

				if (!Utility.isTargetStopFile(array[0])) {
					names.add(array[0]);
				}
			}
		}
		
		return names.size() > 1;
	}

	public static boolean tradeDataContainsTargetStop(List<TradeData> tradeData) {
		boolean containsTargetStop = false;
		for(TradeData data : tradeData) {
			if(data.getRunParam().getProfitTarget() > 0 || data.getRunParam().getStopLoss() > 0) {
				containsTargetStop = true;
			}
		}
		return containsTargetStop;
	}
	
	public static String getStrategyName(List<TradeData> tradeData) {
		if (tradeData.get(0).getGroupName() != null) {
			return tradeData.get(0).getGroupName();
		} else {
			return tradeData.get(0).getStrategyName();
		}
	}
	
	public static int getTotalDataRowCount(List<TradeData> allData) {
		int totalCount = 0;
		for (TradeData data : allData) {
			totalCount += data.getTrades().size();
		}
		return totalCount;
	}
	
	/**
	 * Retrieves a list of directories for each of the strategy data to run optimization for. These are sub directories of the main data directory.
	 * It excludes any directory named backup.
	 * @return
	 */
	public static List<File> getDataDirectories() {
		List<File> dataDirs = new ArrayList<>();

		File dataDir = new File(PathResources.DATA_PATH);
		File[] allFiles = dataDir.listFiles();

		for (File f : allFiles) {
			if (f.isDirectory()) {
				if (!f.getName().equalsIgnoreCase(PathResources.BACK_UP_DATA_FOLDER_NAME)
						&& !f.getName().equalsIgnoreCase(PathResources.SINGLE_RUN_DATA_FOLDER_NAME)
							&& !f.getName().equalsIgnoreCase(PathResources.SUPPLEMENT_DATA_FOLDER_NAME)) {
					dataDirs.add(f);
				}
			}
		}

		return dataDirs;
	}

	public static List<File> getDataDirectoriesWithSupplementData() {
		List<File> dataDirs = new ArrayList<>();

		File dataDir = new File(PathResources.DATA_PATH);
		File[] allFiles = dataDir.listFiles();

		for (File f : allFiles) {
			if (f.isDirectory()) {
				if (!f.getName().equalsIgnoreCase(PathResources.BACK_UP_DATA_FOLDER_NAME)
						&& !f.getName().equalsIgnoreCase(PathResources.SINGLE_RUN_DATA_FOLDER_NAME)) {
					dataDirs.add(f);
				}
			}
		}

		return dataDirs;
	}
	
	public static boolean matchFilter(TradeRecord t, FilterRange filter) {
		Double value = t.getData().get(filter.getVar());
		return value != null && value >= filter.getMin() && value <= filter.getMax();
	}
	
	public static TradeData getSubTradeData(TradeData data, FilterRange filter) {
		List<TradeRecord> rangeTrades = new ArrayList<TradeRecord>();

		for (TradeRecord t : data.getTrades()) {
			if (matchFilter(t, filter)) {
				rangeTrades.add(t);
			}
		}

		List<FilterRange> filters = new ArrayList<FilterRange>();
		if (data.getFilters() != null) {
			filters.addAll(data.getFilters());
		}
		filters.add(filter);

		TradeData d = new TradeData(rangeTrades, data.getRunParam(), filters, data.getRanges(), data.getGroupName(), data.getStrategyName(), data.isShort());

		return d;
	}
	
	public static void updateTradeDataWithTargetStopMinPriceLevelDates(List<TargetStopStrategyResult> allTargetResults,
			List<TargetStopStrategyResult> allStopLossResults, Collection<TradeRecord> trades, boolean isShort) {
		for (TargetStopStrategyResult targetResult : allTargetResults) {
			Map<Integer, Double> targets = targetResult.getTargetStopsMap();

			for (TradeRecord rec : trades) {
				double target = targets.get(rec.getId());
				Date minTargetHitTime = getMinTargetHitTime(rec.getPriceLevels(), target, isShort);

				if (minTargetHitTime != null) {
					if (rec.getMinDateByTargetPrice() != null
							&& (!rec.getMinDateByTargetPrice().containsKey(target)
									|| rec.getMinDateByTargetPrice().get(target).after(minTargetHitTime))) {
						rec.getMinDateByTargetPrice().put(target, minTargetHitTime);
					} else if (rec.getMinDateByTargetPrice() == null) {
						Map<Double, Date> map = new HashMap<>();
						map.put(target, minTargetHitTime);
						rec.setMinDateByTargetPrice(map);
					}
				}
			}
		}

		for (TargetStopStrategyResult stopLossResult : allStopLossResults) {
			Map<Integer, Double> stops = stopLossResult.getTargetStopsMap();

			for (TradeRecord rec : trades) {
				double stop = stops.get(rec.getId());
				Date minStopHitTime = getMinStopHitTime(rec.getPriceLevels(), stop, isShort);

				if (minStopHitTime != null) {
					if (rec.getMinDateByStopPrice() != null && (!rec.getMinDateByStopPrice().containsKey(stop)
							|| rec.getMinDateByStopPrice().get(stop).after(minStopHitTime))) {
						rec.getMinDateByStopPrice().put(stop, minStopHitTime);
					} else if (rec.getMinDateByStopPrice() == null) {
						Map<Double, Date> map = new HashMap<>();
						map.put(stop, minStopHitTime);
						rec.setMinDateByStopPrice(map);
					}
				}
			}
		}
	}
	
	
	/**
	 * Finds the earliest time that the stop loss was met by MAE.
	 * @param maeMap
	 * @param stopLoss
	 * @return
	 */
	private static Date getMinStopHitTime(List<PriceLevel> priceLevels, double stopLoss, boolean isShort) {
		Date minMaeTime = null;
		
		for (PriceLevel pLevel : priceLevels) {
			if (!isShort) {
				if (stopLoss >= pLevel.getPrice()) {
					if (minMaeTime == null || pLevel.getDate().before(minMaeTime)) {
						minMaeTime = pLevel.getDate();
					}
				}
			} else {
				if (stopLoss <= pLevel.getPrice()) {
					if (minMaeTime == null || pLevel.getDate().before(minMaeTime)) {
						minMaeTime = pLevel.getDate();
					}
				}
			}
		}
		
		return minMaeTime;
	}
	
	/**
	 * Finds the earliest time that the target was met by MFE.
	 * @param mfeMap
	 * @param target
	 * @return
	 */
	private static Date getMinTargetHitTime(List<PriceLevel> priceLevels, double target, boolean isShort) {
		Date minMfeTime = null;
		
		for (PriceLevel pLevel : priceLevels) {
			if (!isShort) {
				if (target < pLevel.getPrice()) {
					if (minMfeTime == null || pLevel.getDate().before(minMfeTime)) {
						minMfeTime = pLevel.getDate();
					}
				}
			} else {
				if (target > pLevel.getPrice()) {
					if (minMfeTime == null || pLevel.getDate().before(minMfeTime)) {
						minMfeTime = pLevel.getDate();
					}
				}
			}
		}
		
		return minMfeTime;
	}
	
	public static boolean isTargetStopFile(String strategyNameOrGroupName) {
		return strategyNameOrGroupName.equals(PathResources.TARGET_STOP_MAE_MFE_FILE_NAME_1) || strategyNameOrGroupName.equals(PathResources.TARGET_STOP_MAE_MFE_FILE_NAME_2);
	}
	
	public static boolean isTargetStopFile(File file) {
		String[] fileNameArray = file.getName().replace(".csv", "").split(PathResources.FILE_NAME_SEPARATOR);
		String fileName = fileNameArray[0];
				
		return Utility.isTargetStopFile(fileName);
	}
	
	/**
	 * Reads all target stop data to get the price levels for each symbol.
	 * @param dataDir
	 * @return
	 */
	public static Map<String, PriceLevels> extractPriceLevelData(File dataDir) {
		Map<String, PriceLevels> priceLevelsData = new HashMap<>();
		
		File[] allFiles = dataDir.listFiles();
		
		List<File> allFilesList = new ArrayList<File>();
		for (File f : allFiles) {
			if (isDataFile(f)) {
				allFilesList.add(f);
			}
		}
		
		final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		
		int count = 0;
		for (File f : allFilesList) {
			count++;

			System.out.println("Extracting Price Level Data from data file [" + f.getName() + "] (" + count + "/" + allFilesList.size() + ")...");

			String[] fileNameArray = f.getName().replace(".csv", "").split(PathResources.FILE_NAME_SEPARATOR);
			String fileName = fileNameArray[0];
			
			boolean isTargetStopFile = Utility.isTargetStopFile(fileName);

			List<String> cols = new ArrayList<String>();
			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {

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
						} else if ("Entry Price".equals(column)) {
							rec.setEntryPrice(Double.parseDouble(val));
						} else if ("Entry Time".equals(column)) {
							rec.setEntryTime(df.parse(val));
						} else if ("Timeout Price".equals(column)) {
							rec.setTimeoutPrice(Double.parseDouble(val));
						} else if ("Timeout Time".equals(column)) {
							rec.setTimeoutTime(df.parse(val));
						} else if ("MFE".equals(column)) {
							rec.setMfe(val.isEmpty() ? null : Double.parseDouble(val));
						} else if ("MFE Time".equals(column)) {
							rec.setMfeTime(val.isEmpty() ? null : df.parse(val));
						} else if ("MAE".equals(column)) {
							rec.setMae(val.isEmpty() ? null : Double.parseDouble(val));
						} else if ("MAE Time".equals(column)) {
							rec.setMaeTime(val.isEmpty() ? null : df.parse(val));
						}
					}

					collectPriceLevelsFromTradeRecord(priceLevelsData, rec, isTargetStopFile);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return priceLevelsData;
	}
	
	/**
	 * Reads all target stop data to get the price levels for each symbol.
	 * @param dataDir
	 * @return
	 */
	public static Map<Date, MarketCondition> extractMarketConditionsData() {
		Map<Date, MarketCondition> marketConditionsData = new HashMap<>();
				
		List<File> allDataDirs = Utility.getDataDirectoriesWithSupplementData();
		
		List<File> allFilesList = allDataDirs.stream().filter((d) -> d.isDirectory()).flatMap((d) -> Arrays.stream(d.listFiles())).filter((f) -> isDataFile(f)).toList();

		allFilesList.forEach((f) -> System.out.println("!!! " + f.getName()));
		
		final SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		
		int count = 0;
		for (File f : allFilesList) {
			count++;

			System.out.println("Extracting Market Conditions Data from data file [" + f.getName() + "] (" + count + "/" + allFilesList.size() + ")...");
			
			List<String> cols = new ArrayList<String>();
			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {

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
					
					Date date = null;
					
					double dowChange5 = 0.0;
					double dowChange10 = 0.0;
					double dowChange15 = 0.0;
					double dowChange30 = 0.0;
					double dowChangeToday = 0.0;
					
					double spyChange5 = 0.0;
					double spyChange10 = 0.0;
					double spyChange15 = 0.0;
					double spyChange30 = 0.0;
					double spyChangeToday = 0.0;
					
					double nasdaqChange5 = 0.0;
					double nasdaqChange10 = 0.0;
					double nasdaqChange15 = 0.0;
					double nasdaqChange30 = 0.0;
					double nasdaqChangeToday = 0.0;
					
					for (int i = 0; i < array.length; i++) {
						String column = cols.get(i);
						
						if (column.isEmpty()) {
							continue;
						}
						
						if ((date != null && marketConditionsData.containsKey(date))) {
							break;
						}
						
						String val = array[i];
						
						if ("Entry Time".equals(column)) {
							date = df.parse(val);
						} else if ("Dow Change 10 Minute (%) [Dia10]".equals(column)) {
							dowChange10 = Double.parseDouble(val);
						} else if ("Dow Change 15 Minute (%) [Dia15]".equals(column)) {
							dowChange15 = Double.parseDouble(val);
						} else if ("Dow Change 30 Minute (%) [Dia30]".equals(column)) {
							dowChange30 = Double.parseDouble(val);
						} else if ("Dow Change 5 Minute (%) [Dia5]".equals(column)) {
							dowChange5 = Double.parseDouble(val);
						} else if ("Dow Change Today (%) [DiaD]".equals(column)) {
							dowChangeToday = Double.parseDouble(val);
						} else if ("S&P Change 10 Minute (%) [Spy10]".equals(column)) {
							spyChange10 = Double.parseDouble(val);
						} else if ("S&P Change 5 Minute (%) [Spy5]".equals(column)) {
							spyChange5 = Double.parseDouble(val);
						} else if ("S&P Change 15 Minute (%) [Spy15]".equals(column)) {
							spyChange15 = Double.parseDouble(val);
						} else if ("S&P Change 30 Minute (%) [Spy30]".equals(column)) {
							spyChange30 = Double.parseDouble(val);
						} else if ("S&P Change Today (%) [SpyD]".equals(column)) {
							spyChangeToday = Double.parseDouble(val);
						} else if ("NASDAQ Change 10 Minute (%) [Qqqq10]".equals(column)) {
							nasdaqChange10 = Double.parseDouble(val);
						} else if ("NASDAQ Change 15 Minute (%) [Qqqq15]".equals(column)) {
							nasdaqChange15 = Double.parseDouble(val);
						} else if ("NASDAQ Change 30 Minute (%) [Qqqq30]".equals(column)) {
							nasdaqChange30 = Double.parseDouble(val);
						} else if ("NASDAQ Change 5 Minute (%) [Qqqq5]".equals(column)) {
							nasdaqChange5 = Double.parseDouble(val);
						} else if ("NASDAQ Change Today (%) [QqqqD]".equals(column)) {
							nasdaqChangeToday = Double.parseDouble(val);
						}
					}
					
					if (!marketConditionsData.containsKey(date)) {
						MarketCondition mkt = new MarketCondition(date, dowChange5, dowChange10, dowChange15,
								dowChange30, dowChangeToday, spyChange5, spyChange10, spyChange15, spyChange30,
								spyChangeToday, nasdaqChange5, nasdaqChange10, nasdaqChange15, nasdaqChange30,
								nasdaqChangeToday);
						marketConditionsData.put(date, mkt);
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		SimpleDateFormat dff = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
		marketConditionsData.forEach((a, b) -> System.out.println(dff.format(a) + "\t" + b.getDowChangeToday()));

		return marketConditionsData;
	}
	
	/**
	 * Goes through all trade records (And target/stop) and collects the price
	 * levels at various MAE, MFE, Timeout points.
	 */
	private static void collectPriceLevelsFromTradeRecord(Map<String, PriceLevels> priceLevelsData, TradeRecord rec, boolean isTargetStopFile) {
		if (priceLevelsData.containsKey(rec.getSymbol())) {
			PriceLevels priceLevels = priceLevelsData.get(rec.getSymbol());
			if (priceLevels.getPriceLevels() == null) {
				priceLevels.setPriceLevels(new HashSet<>());
			}

			addPriceLevel(priceLevels.getPriceLevels(), rec.getSymbol(), rec.getMaeTime(), rec.getMae());
			addPriceLevel(priceLevels.getPriceLevels(), rec.getSymbol(), rec.getMfeTime(), rec.getMfe());
			addPriceLevel(priceLevels.getPriceLevels(), rec.getSymbol(), rec.getTimeoutTime(), rec.getTimeoutPrice());
			addPriceLevel(priceLevels.getPriceLevels(), rec.getSymbol(), rec.getEntryTime(), rec.getEntryPrice());
		} else {
			Set<PriceLevel> priceLevelsSet = new HashSet<>();

			addPriceLevel(priceLevelsSet, rec.getSymbol(), rec.getMaeTime(), rec.getMae());
			addPriceLevel(priceLevelsSet, rec.getSymbol(), rec.getMfeTime(), rec.getMfe());
			addPriceLevel(priceLevelsSet, rec.getSymbol(), rec.getTimeoutTime(), rec.getTimeoutPrice());
			addPriceLevel(priceLevelsSet, rec.getSymbol(), rec.getEntryTime(), rec.getEntryPrice());

			PriceLevels priceLevels = new PriceLevels();
			priceLevels.setSymbol(rec.getSymbol());
			priceLevels.setPriceLevels(priceLevelsSet);

			priceLevelsData.put(priceLevels.getSymbol(), priceLevels);
		}
	}
	
	private static void addPriceLevel(Set<PriceLevel> priceLevelsSet, String symbol, Date time, Double price) {
		if (time != null && price > 0.0) {
			priceLevelsSet.add(new PriceLevel(symbol, time, price));
		}
	}
	
	/**
	 * TODO: Improve the efficiency
	 * 
	 * Calculates Spline Interpolation function for percent of profit/loss curve,
	 * then integrates the positive and negative parts and divides the positive by
	 * the negative to get a ratio.
	 * 
	 */
	public static double getProfitToLossVolumeRatio(TradeRecord rec) {
		double ratio = 0.0;
		
		List<PriceLevel> orderedPriceLevels = new ArrayList<>();
		rec.getPriceLevels().forEach((l) -> orderedPriceLevels.add(l));
		orderedPriceLevels.sort(new Comparator<PriceLevel>() {
			@Override
			public int compare(PriceLevel o1, PriceLevel o2) {
				return o1.getDate().compareTo(o2.getDate());
			}
		});
		
		Calendar cal = Calendar.getInstance();
		
		Set<Double> uniqueMinutes = new HashSet<>();

		List<PriceLevel> orderedDistinctPriceLevels = new ArrayList<>();
		for (PriceLevel l : orderedPriceLevels) {
			cal.setTime(l.getDate());
			
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int minute = cal.get(Calendar.MINUTE);

			double minutesFromTheOpen = (hour - 9) * 60 + minute - 30;

			if (uniqueMinutes.contains(minutesFromTheOpen)) {
				continue;
			} else {
				uniqueMinutes.add(minutesFromTheOpen);
				orderedDistinctPriceLevels.add(l);
			}
		}

		double[] x = new double[orderedDistinctPriceLevels.size()];
		double[] y = new double[orderedDistinctPriceLevels.size()];

		double minMinutes = -1;
		double maxMinutes = -1;

		for(int i = 0; i < orderedDistinctPriceLevels.size(); i++) {
			PriceLevel l = orderedDistinctPriceLevels.get(i);
			
			cal.setTime(l.getDate());
			
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int minute = cal.get(Calendar.MINUTE);
			
			double minutesFromTheOpen = (hour - 9)*60 + minute - 30;

			x[i] = minutesFromTheOpen;
			y[i] = (l.getPrice() - rec.getEntryPrice())/rec.getEntryPrice(); // percent profit (positive or negative)
						
			if (minutesFromTheOpen < minMinutes || minMinutes == -1) {
				minMinutes = minutesFromTheOpen;
			}
			if (minutesFromTheOpen > maxMinutes || maxMinutes == -1) {
				maxMinutes = minutesFromTheOpen;
			}
		}

		UnivariateInterpolator interpolator = new SplineInterpolator();
		UnivariateFunction function = interpolator.interpolate(x, y);

		List<Interval> positiveIntervals = new ArrayList<>();
		List<Interval> negativeIntervals = new ArrayList<>();
		
		Interval interval = null;
		boolean positive = false;
		for (double i = minMinutes; i <= maxMinutes; i++) {
			double interpolatedY = function.value(i);
						
			if (interval == null) {
				interval = new Interval();
				interval.min = i;

				if (interpolatedY > 0.0) {
					positive = true;
				} else {
					positive = false;
				}
				
				continue;
			}
			
			if ((positive && interpolatedY < 0.0) || (!positive && interpolatedY > 0.0) || i == maxMinutes) {
				interval.max = i;

				if (positive) {
					positiveIntervals.add(interval);
				} else {
					negativeIntervals.add(interval);
				}

				if (i != maxMinutes) {
					interval = new Interval();
					interval.min = i;

					positive = interpolatedY > 0.0;
				}
			} else {
				continue;
			}
		}
				
		RombergIntegrator integrator = new RombergIntegrator();

		double positiveVolume = 0.0;
		for (Interval i : positiveIntervals) {
			positiveVolume += Math.abs(integrator.integrate(Integer.MAX_VALUE, function, i.min, i.max));
		}

		double negativeVolume = 0.0;
		for (Interval i : negativeIntervals) {
			negativeVolume += Math.abs(integrator.integrate(Integer.MAX_VALUE, function, i.min, i.max));
		}
				
		ratio = negativeVolume > 0 ? positiveVolume / negativeVolume : 9999999;
		
		System.out.println("=====> Ratio = " + ratio);
		
		return ratio;
	}
	
	private static class Interval {
		double min;
		double max;
	}
}
