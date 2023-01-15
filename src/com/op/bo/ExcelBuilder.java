package com.op.bo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.op.bo.GemParameters.GemReviewResult;
import com.op.bo.RunConfiguration.RunStrategies;
import com.op.bo.TIRunParameters.PriceExitType;
import com.op.bo.TIRunParameters.TimeExitType;
import com.op.bo.targetstop.TargetStopInfo;

public class ExcelBuilder {

	private static final int STRATEGY_EXPORT_LIMIT = 10;
	
	private static CellStyle labelStyle;
	private static CellStyle valueStyle;
	private static CellStyle dateMinuteSecondStyle;
	private static CellStyle dateDayStyle;
	
	private static String outputDirPath;
		
	private static final String ORDERS_SHEET_NAME = "Orders";
	private static final String TRADES_SHEET_NAME = "Trades";
	
	public static void buildPerformanceAnalysisExcel(PerformanceData performanceData) {
		SimpleDateFormat df = new SimpleDateFormat("MMddyyyyhhmmss");
		try {
			System.out.println("\nBuilding Performance Analysis Excel: ");
			
			File outFile = new File(PathResources.PERFORMANCE_ANALYSIS_DIR_PATH, PathResources.PERFORMANCE_ANALYSIS_FILE_NAME);
			
			File originalFile = new File(PathResources.PERFORMANCE_ANALYSIS_DIR_PATH, PathResources.PERFORMANCE_ANALYSIS_FILE_NAME);
			
			String originalFileName = originalFile.getName();
			
			File tempFile = new File(PathResources.PERFORMANCE_ANALYSIS_DIR_PATH, "temp.xlsx");
			File backupFile = new File(PathResources.PERFORMANCE_ANALYSIS_DIR_PATH, originalFileName.replace(".xlsx", "") + "_" + df.format(new Date()) + ".xlsx");
			FileUtils.copyFile(originalFile, tempFile);
			FileUtils.copyFile(originalFile, backupFile);
			
			originalFile.delete();

			XSSFWorkbook wb = new XSSFWorkbook(tempFile);

			int ordersSheetIndex = wb.getSheetIndex(ORDERS_SHEET_NAME);
			int tradesSheetIndex = wb.getSheetIndex(TRADES_SHEET_NAME);

			if (ordersSheetIndex != -1) {
				wb.removeSheetAt(ordersSheetIndex);
			}
			if (tradesSheetIndex != -1) {
				wb.removeSheetAt(tradesSheetIndex);
			}

			XSSFSheet tradesSheet = wb.createSheet(TRADES_SHEET_NAME);
			XSSFSheet ordersSheet = wb.createSheet(ORDERS_SHEET_NAME);

			Font font = wb.createFont();
			font.setBold(true);
			
			labelStyle = wb.createCellStyle();
			labelStyle.setBorderBottom(BorderStyle.THIN);
			labelStyle.setBorderTop(BorderStyle.THIN);
			labelStyle.setBorderLeft(BorderStyle.THIN);
			labelStyle.setBorderRight(BorderStyle.THIN);
			labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			labelStyle.setFont(font);
			labelStyle.setWrapText(true);
	        
	        valueStyle = wb.createCellStyle();
	        valueStyle.setBorderBottom(BorderStyle.THIN);
	        valueStyle.setBorderTop(BorderStyle.THIN);
	        valueStyle.setBorderLeft(BorderStyle.THIN);
	        valueStyle.setBorderRight(BorderStyle.THIN);
	        valueStyle.setAlignment(HorizontalAlignment.CENTER);
			
	        CreationHelper createHelper = wb.getCreationHelper();
	        dateMinuteSecondStyle = wb.createCellStyle();
	        dateMinuteSecondStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yyyy hh:mm:ss \\a"));
	        
	        dateDayStyle = wb.createCellStyle();
	        dateDayStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d"));
	        
	        writeOrdersTable(performanceData.getOrders(), wb, ordersSheet);
	        writeTradesTable(performanceData.getTrades(), wb, tradesSheet);
	        
	        FileOutputStream fs = new FileOutputStream(outFile);
			wb.write(fs);
			wb.close();
			
			tempFile.delete();
		} catch (IOException | InvalidFormatException e) {
			e.printStackTrace();
		}
	}

	private static String getTypeOfRun(RunConfiguration runParam) {
		return runParam.getRunStrategy() != null && !runParam.getRunStrategy().equals(RunStrategies.Adhoc)
				? String.valueOf(runParam.getRunStrategy())
				: runParam.isOnlyOptimizeTargetStop() ? "TS"
						: runParam.isOptimizeTargetStop() && runParam.getOptimizeCount() > 1
								? "S & TS Super Heavy"
								: runParam.isOptimizeTargetStop() ? "S & TS"
										: runParam.getOptimizeCount() > 1 ? "S Heavy" : "S";
	}
	
	public static void buildOptimizationResultsExcel(String groupName, String strategyName, OptimizationResults results,
			List<TIRunParameters> tiRunParam, RunConfiguration runParam, File wtiFile, boolean buildAllExcels, long startTimeMillis) {
		outputDirPath = PathResources.RESULTS_DIR_PATH + (groupName != null ? groupName : strategyName);
		File outputDir = new File(outputDirPath);

		int seq = getMaxSeq(outputDir, true) + 1;
		
		long endTimeMillis = System.currentTimeMillis();
		int duration = (int)((endTimeMillis - startTimeMillis) / 1000); // seconds
		
		outputDirPath += File.separator + String.valueOf(seq) + ") [" + getTypeOfRun(runParam) + "] Q="
				+ (Utility.roundStr(results.getAbsMaxQIndex(), 1)) 
				+ ", PF=" + (Utility.roundStr(results.getAbsMaxPf(), 1))
				+ ", W%=" + (Utility.roundStr(results.getAbsMaxWinRate(), 1)
				+ ", Duration=" + duration);
		outputDir = new File(outputDirPath);
		
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		if (buildAllExcels) {
			if (results.getMaxPf() != null && !results.getMaxPf().isEmpty()) {
				System.out.println("Exporting Max PF Excel ... ");
				buildOptimizationResultsExcel(outputDirPath, strategyName, "maxPf", results.getMaxPf(), tiRunParam, runParam);
			}
			
			if (results.getMaxPfWin() != null && !results.getMaxPfWin().isEmpty()) {
				System.out.println("Exporting Max PF & Win Rate Excel ... ");
				buildOptimizationResultsExcel(outputDirPath, strategyName, "maxPfWin", results.getMaxPfWin(), tiRunParam, runParam);
			}
			
			if (results.getMaxPfWinTrades() != null && !results.getMaxPfWinTrades().isEmpty()) {
				System.out.println("Exporting Max PF, Win Rate & Trades Excel ... ");
				buildOptimizationResultsExcel(outputDirPath, strategyName, "maxPfWinTrades", results.getMaxPfWinTrades(), tiRunParam, runParam);
			}
			
			if (results.getMaxWin() != null && !results.getMaxWin().isEmpty()) {
				System.out.println("Exporting Max Win Rate Excel ... ");
				buildOptimizationResultsExcel(outputDirPath, strategyName, "maxWin", results.getMaxWin(), tiRunParam, runParam);
			}
			
			if (results.getMaxWinTrades() != null && !results.getMaxWinTrades().isEmpty()) {
				System.out.println("Exporting Max Win Rate & Trades Excel ... ");
				buildOptimizationResultsExcel(outputDirPath, strategyName, "maxWinTrades", results.getMaxWinTrades(), tiRunParam, runParam);
			}
			
			if (results.getMaxQIndex() != null && !results.getMaxQIndex().isEmpty()) {
				System.out.println("Exporting Max Quality Index Excel ... ");
				buildOptimizationResultsExcel(outputDirPath, strategyName, "maxQIndex", results.getMaxQIndex(), tiRunParam, runParam);
			}
		}
		
		if (results.getMaxQIndxByCol() != null && !results.getMaxQIndxByCol().isEmpty()) {
			System.out.println("Exporting Max Quality Index Trades By Col Excel ... ");
			buildOptimizationResultsExcel(outputDirPath, strategyName, "maxQIndex-ByCol", results.getMaxQIndxByCol(), tiRunParam, runParam);
		}
		
		if (results.getMaxPfByCol() != null && !results.getMaxPfByCol().isEmpty()) {
			System.out.println("Exporting Max PF By Col Excel ... ");
			buildOptimizationResultsExcel(outputDirPath, strategyName, "maxPf-ByCol", results.getMaxPfByCol(), tiRunParam, runParam);
		}
		
		if (results.getMaxPfWinByCol() != null && !results.getMaxPfWinByCol().isEmpty()) {
			System.out.println("Exporting Max PF & Win Rate By Col Excel ... ");
			buildOptimizationResultsExcel(outputDirPath, strategyName, "maxPfWin-ByCol", results.getMaxPfWinByCol(), tiRunParam, runParam);
		}
		
		if (results.getMaxPfWinTradesByCol() != null && !results.getMaxPfWinTradesByCol().isEmpty()) {
			System.out.println("Exporting Max PF, Win Rate & Trades By Col Excel ... ");
			buildOptimizationResultsExcel(outputDirPath, strategyName, "maxPfWinTrades-ByCol", results.getMaxPfWinTradesByCol(), tiRunParam, runParam);
		}
		
		if (results.getMaxWinByCol() != null && !results.getMaxWinByCol().isEmpty()) {
			System.out.println("Exporting Max Win Rate By Col Excel ... ");
			buildOptimizationResultsExcel(outputDirPath, strategyName, "maxWin-ByCol", results.getMaxWinByCol(), tiRunParam, runParam);
		}
		
		if (results.getMaxWinTradesByCol() != null && !results.getMaxWinTradesByCol().isEmpty()) {
			System.out.println("Exporting Max Win Rate & Trades By Col Excel ... ");
			buildOptimizationResultsExcel(outputDirPath, strategyName, "maxWinTrades-ByCol", results.getMaxWinTradesByCol(), tiRunParam, runParam);
		}
		
		createTradeDataRowCountTextFile(outputDirPath, runParam.getTotalTradeDataRowCount());
		
		if (wtiFile != null) {
			copyWTIFile(wtiFile);
			buildStrategyWTIFiles(wtiFile, results, buildAllExcels);
		}
	}
	
	private static void buildStrategyWTIFiles(File wtiFile, OptimizationResults results, boolean buildAllExcels) {
		String strategyWTIDirPath = outputDirPath + File.separator + "Strategy WTI Files";
		File strategyWTIDir = new File(strategyWTIDirPath);

		if (!strategyWTIDir.exists()) {
			strategyWTIDir.mkdirs();
		}
		
		Set<StratStats> stats = new HashSet<>();
		if (buildAllExcels) {
			if (results.getMaxPf() != null) {
				stats.addAll(Utility.lastX(results.getMaxPf(), STRATEGY_EXPORT_LIMIT));
			}
			if (results.getMaxPfWin() != null) {
				stats.addAll(Utility.lastX(results.getMaxPfWin(), STRATEGY_EXPORT_LIMIT));
			}
			if (results.getMaxPfWinTrades() != null) {
				stats.addAll(Utility.lastX(results.getMaxPfWinTrades(), STRATEGY_EXPORT_LIMIT));
			}
			if (results.getMaxWin() != null) {
				stats.addAll(Utility.lastX(results.getMaxWin(), STRATEGY_EXPORT_LIMIT));
			}
			if (results.getMaxWinTrades() != null) {
				stats.addAll(Utility.lastX(results.getMaxWinTrades(), STRATEGY_EXPORT_LIMIT));
			}
			if (results.getMaxQIndex() != null) {
				stats.addAll(Utility.lastX(results.getMaxQIndex(), STRATEGY_EXPORT_LIMIT));
			}
		}
		if (results.getMaxQIndxByCol() != null) {
			stats.addAll(Utility.lastX(results.getMaxQIndxByCol(), STRATEGY_EXPORT_LIMIT));
		}
		if (results.getMaxPfByCol() != null) {
			stats.addAll(Utility.lastX(results.getMaxPfByCol(), STRATEGY_EXPORT_LIMIT));
		}
		if (results.getMaxPfWinByCol() != null) {
			stats.addAll(Utility.lastX(results.getMaxPfWinByCol(), STRATEGY_EXPORT_LIMIT));
		}
		if (results.getMaxPfWinTradesByCol() != null) {
			stats.addAll(Utility.lastX(results.getMaxPfWinTradesByCol(), STRATEGY_EXPORT_LIMIT));
		}
		if (results.getMaxWinByCol() != null) {
			stats.addAll(Utility.lastX(results.getMaxWinByCol(), STRATEGY_EXPORT_LIMIT));
		}
		if (results.getMaxWinTradesByCol() != null) {
			stats.addAll(Utility.lastX(results.getMaxWinTradesByCol(), STRATEGY_EXPORT_LIMIT));
		}
		
		String originalWtiXML = readWTIFileXML(wtiFile);
	
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();

			for(StratStats stat : stats) {
				InputSource is = new InputSource(new StringReader(originalWtiXML));
				Document doc = builder.parse(is);
				
				createStrategtWTIFromOriginalWTIUsingStrategy(doc, stat);
				
				DOMSource source = new DOMSource(doc);

				FileWriter writer = new FileWriter(new File(strategyWTIDirPath + File.separator + stat.getStatId() + ".WTI"));
				StreamResult result = new StreamResult(writer);
				transformer.transform(source, result);		
			}
		} catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
			e.printStackTrace();
		}
	}
	
	private static void createStrategtWTIFromOriginalWTIUsingStrategy(Document wtiDoc, StratStats stat) {
		Element window = (Element) wtiDoc.getElementsByTagName("WINDOW").item(0);
		StringBuilder configAttr = new StringBuilder(window.getAttribute("CONFIG"));
		
		int windowNameStartIndex = configAttr.indexOf("WN=");
		int indexOfShow0 = configAttr.indexOf("&show0=", windowNameStartIndex);
		int indexOfSL = configAttr.indexOf("&SL=", windowNameStartIndex);
		int windowNameEndIndex = indexOfShow0 > -1 && indexOfSL > -1 ? Math.min(indexOfShow0, indexOfSL) : indexOfShow0;
		
		StringBuilder name = new StringBuilder(configAttr.substring(windowNameStartIndex + 3, windowNameEndIndex));
		
		StringBuilder builder = new StringBuilder();
		if (stat.getFilters() != null) {
			for (FilterRange filter : stat.getFilters()) {
				String code = Utility.getColumnCode(filter.getVar());
				builder.append("Min" + code + "=" + Utility.roundDown(filter.getMin(), 3) + "&" + "Max" + code + "=" + Utility.roundUp(filter.getMax(), 3) + "&");
				
				name.append(" " + code);
			}
		}
		
		builder.append("WN=" + name.toString().replace(" ", "+"));

		configAttr = configAttr.replace(windowNameStartIndex, windowNameEndIndex, builder.toString());
		window.setAttribute("CONFIG", configAttr.toString());
		
		if (stat.getTargetStopComboStrategyResult() != null) {
			Element oddsmaker = (Element) wtiDoc.getElementsByTagName("ODDSMAKER").item(0);
			oddsmaker.setAttribute("TXT_PROFIT_TARGET", String.valueOf(stat.getTargetStopComboStrategyResult().getTargetResult().getProfitTarget()));
			oddsmaker.setAttribute("TXT_STOP_LOSS", String.valueOf(stat.getTargetStopComboStrategyResult().getStopLossResult().getStopLoss()));
			oddsmaker.setAttribute("RDO_BY_AT_LEAST_DOLLAR", String.valueOf(stat.getTargetStopComboStrategyResult().getTargetResult().isDollar() ? "True" : "False"));
			oddsmaker.setAttribute("RDO_BY_AT_LEAST_PERCENT", String.valueOf(stat.getTargetStopComboStrategyResult().getTargetResult().isDollar() ? "False" : "True"));
			oddsmaker.setAttribute("CHK_PROFIT_TARGET", "True");
			oddsmaker.setAttribute("CHK_STOP_LOSS", "True");
		}
	}
	
	private static String readWTIFileXML(File wtiFile) {
		StringBuilder builder = new StringBuilder();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(wtiFile));) {
			String line = null;
			do {
				line = reader.readLine();
				if (line != null) {
					builder.append(line);
				}
			} while (line != null);

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		return builder.toString();
	}
	
	private static void createTradeDataRowCountTextFile(String outputDirPath, int totalTradeDataRowCount) {
		String outputFilePath = outputDirPath + File.separator + String.valueOf(totalTradeDataRowCount);
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(new File(outputFilePath)));
			writer.write("");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void copyWTIFile(File wtiFile) {
		if (wtiFile != null) {
			File outputFile = new File(outputDirPath + File.separator + wtiFile.getName());
			try {
				FileUtils.copyFile(wtiFile, outputFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void buildOptimizationResultsExcel(String outputDirPath, String fileName, String optimizationLabel, List<StratStats> stats, List<TIRunParameters> tiRunParam, RunConfiguration runParam) {
		try {
			if (!fileName.endsWith(".xlsx") && optimizationLabel != null && !optimizationLabel.trim().isEmpty()) {
				fileName += "-" + optimizationLabel + ".xlsx";
			}
			
			File outFile = new File(outputDirPath, fileName) ;
			File tempFile = new File(PathResources.RESULTS_DIR_PATH, "temp.xlsx");
			FileUtils.copyFile(new File(PathResources.TEMPLATE_PATH), tempFile);
			
			XSSFWorkbook wb = new XSSFWorkbook(tempFile);
			XSSFSheet sheet = wb.getSheet("Stats");
			XSSFSheet dataSheet = wb.getSheet("Stats Data");
			XSSFSheet dayDataSheet = wb.getSheet("Stats Day Data");
			XSSFSheet timeDataSheet = wb.getSheet("Stats Time Data");
			XSSFSheet tiRunParamSheet = wb.getSheet("TI Run Param");
			XSSFSheet runParamSheet = wb.getSheet("Run Param");

			Font font = wb.createFont();
			font.setBold(true);
			
			labelStyle = wb.createCellStyle();
			labelStyle.setBorderBottom(BorderStyle.THIN);
			labelStyle.setBorderTop(BorderStyle.THIN);
			labelStyle.setBorderLeft(BorderStyle.THIN);
			labelStyle.setBorderRight(BorderStyle.THIN);
			labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			labelStyle.setFont(font);
			labelStyle.setWrapText(true);
	        
	        valueStyle = wb.createCellStyle();
	        valueStyle.setBorderBottom(BorderStyle.THIN);
	        valueStyle.setBorderTop(BorderStyle.THIN);
	        valueStyle.setBorderLeft(BorderStyle.THIN);
	        valueStyle.setBorderRight(BorderStyle.THIN);
	        valueStyle.setAlignment(HorizontalAlignment.CENTER);
	        valueStyle.setWrapText(true);
			
	        CreationHelper createHelper = wb.getCreationHelper();
	        dateMinuteSecondStyle = wb.createCellStyle();
	        dateMinuteSecondStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yyyy hh:mm:ss \\a"));
	        
	        dateDayStyle = wb.createCellStyle();
	        dateDayStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d"));
	        
			if (tiRunParam != null && !tiRunParam.isEmpty()) {
				writeTIRunParamSection(tiRunParam, wb, tiRunParamSheet);
			}
			if (runParam != null) {
				writeRunParamSection(runParam, wb, runParamSheet);
			}
	        
			int rowPos = 0;
			int dataRowPos = 0;
			int dayDataRowPos = 0;
			int timeDataRowPos = 0;
			for (int i = 0; i < STRATEGY_EXPORT_LIMIT; i++) {
				StratStats stat = null;
				try {
					stat = stats.get(stats.size() - i - 1);
				} catch (IndexOutOfBoundsException e) {
					continue;
				}

				writeStatSection(stat, wb, sheet, rowPos);
				writeStatDataSection(stat, wb, dataSheet, dataRowPos);
				writeStatDayDataSection(stat.getDayData(), wb, dayDataSheet, dayDataRowPos);
				writeStatTimeDataSection(getTimeData(stat), wb, timeDataSheet, timeDataRowPos);
				
				rowPos += 20;
				dataRowPos += 10000;
				dayDataRowPos += 10000;
				timeDataRowPos += 10000;
			}
		
			FileOutputStream fs = new FileOutputStream(outFile);
			wb.write(fs);
			wb.close();
			
			tempFile.delete();
		} catch (IOException | InvalidFormatException e) {
			e.printStackTrace();
		}
	}
	
	private static int getMaxSeq(File outputDir, boolean isDirectory) {
		int maxSeq = 0;
		if (outputDir.listFiles() != null) {
			for (File f : outputDir.listFiles()) {
				if ((isDirectory && f.isDirectory()) || (!isDirectory && !f.isDirectory())) {
					String seqStr = f.getName().split("\\)")[0];
					try {
						int seq = Integer.parseInt(seqStr);
						if (seq > maxSeq) {
							maxSeq = seq;
						}
					} catch (NumberFormatException e) {
						// Continue
					}
				}
			}
		}
		return maxSeq;
	}
	
	private static void writeStatSection(StratStats stat, XSSFWorkbook wb, XSSFSheet sheet, int rowPos) {
		sheet.createRow(rowPos);
		XSSFRow headerRow = sheet.getRow(rowPos);
		
		headerRow.createCell(0);
		headerRow.createCell(1);
		headerRow.createCell(2);
		headerRow.createCell(3);
		headerRow.createCell(4);
		XSSFCell label0Cell = headerRow.getCell(0);
		label0Cell.setCellStyle(valueStyle);
		XSSFCell label1Cell = headerRow.getCell(1);
		label1Cell.setCellStyle(labelStyle);
		XSSFCell label2Cell = headerRow.getCell(2);
		label2Cell.setCellStyle(labelStyle);
		XSSFCell label3Cell = headerRow.getCell(3);
		label3Cell.setCellStyle(labelStyle);
		XSSFCell label4Cell = headerRow.getCell(4);
		label4Cell.setCellStyle(labelStyle);

		label0Cell.setCellValue(String.valueOf(stat.getStatId()));
		label1Cell.setCellValue("In Sample");
		label2Cell.setCellValue("Out Of Sample");
		label3Cell.setCellValue("Overall");
		label4Cell.setCellValue("Final");
		
		rowPos++;
		
		writeLabelValue(rowPos++, "Trade Count", stat.getTradeCount(), stat.getOsTradeCount(), stat.getOverallTradeCount(), stat.getFinalTradeCount(), sheet, labelStyle, valueStyle);		
		writeLabelValue(rowPos++, "Profit Factor", stat.getPf(), stat.getOsPf(), stat.getOverallPf(), stat.getFinalPf(), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Win Rate", stat.getWinRate(), stat.getOsWinRate(), stat.getOverallWinRate(), stat.getFinalWinRate(), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Win Count", stat.getWinCount(), stat.getOsWinCount(), stat.getOverallWinCount(), stat.getFinalWinCount(), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Lose Count", stat.getLoseCount(), stat.getOsLoseCount(), stat.getOverallLoseCount(), stat.getFinalLoseCount(), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Average Winner", stat.getAvgWinner(), stat.getOsAvgWinner(), stat.getOverallAvgWinner(), stat.getFinalAvgWinner(), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Average Loser", stat.getAvgLoser(), stat.getOsAvgLoser(), stat.getOverallAvgLoser(), stat.getFinalAvgLoser(), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Average Winner/Loser", stat.getAvgWinnerLoser(), stat.getOsAvgWinnerLoser(), stat.getOverallAvgWinnerLoser(), stat.getFinalAvgWinnerLoser(), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Percent Days Traded", stat.getPercentDaysTraded(), stat.getOsPercentDaysTraded(), stat.getOverallPercentDaysTraded(), stat.getFinalPercentDaysTraded(), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Draw Down Percent", stat.getDrawDown()*100.0, stat.getOsDrawDown()*100.0, stat.getOverallDrawDown()*100.0, stat.getFinalDrawDown()*100.0, sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "RMSE", Utility.roundStr(stat.getRmse(), 2), Utility.roundStr(stat.getOsRMSE(), 2), Utility.roundStr(stat.getOverallRMSE(), 2), Utility.roundStr(stat.getFinalRMSE(), 2), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Quality Index", "", "", Utility.roundStr(stat.getQualityIndex(), 2), "", sheet, labelStyle, valueStyle);
		
		if (stat.getTargetStopComboStrategyResult() != null) {
			writeLabelValue(rowPos++, "Target Strategy", stat.getTargetStopComboStrategyResult().getTargetResult().getStrategyProperties(), "", "", Utility.roundStr(stat.getOverallPercentOfTargetsHit(), 0), sheet, labelStyle, valueStyle);
			writeLabelValue(rowPos++, "Stop Loss Strategy", stat.getTargetStopComboStrategyResult().getStopLossResult().getStrategyProperties(), "", "", Utility.roundStr(stat.getOverallPercentOfStopsHit(), 0), sheet, labelStyle, valueStyle);
		}
		
		writeLabelValue(rowPos++, "Run Param ID", String.valueOf(stat.getTradeData().getRunParam().getRunParamId()), sheet, labelStyle, valueStyle);
		
		sheet.setColumnWidth(1, 30*256);
		
		if (stat.getFilters() != null && !stat.getFilters().isEmpty()) {
			sheet.createRow(rowPos);
			XSSFRow row = sheet.getRow(rowPos);
			
			row.createCell(0);
			row.createCell(1);
			row.createCell(2);
			
			XSSFCell filterLabelCell = row.getCell(0);
			filterLabelCell.setCellStyle(labelStyle);
			
			XSSFCell minLabelCell = row.getCell(1);
			minLabelCell.setCellStyle(labelStyle);
			
			XSSFCell maxLabelCell = row.getCell(2);
			maxLabelCell.setCellStyle(labelStyle);
			
			filterLabelCell.setCellValue("Filter(s)");
			minLabelCell.setCellValue("Min");
			maxLabelCell.setCellValue("Max");
			
			for (FilterRange fr : stat.getFilters()) {
				writeFilterRow(++rowPos, fr.getVar(), fr.getMin(), fr.getMax(), sheet, labelStyle, valueStyle);
			}
			
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
			sheet.autoSizeColumn(3);
			sheet.autoSizeColumn(4);
		}
	}
	
	private static void writeTIRunParamSection(List<TIRunParameters> runParam, XSSFWorkbook wb, XSSFSheet sheet) {
        final SimpleDateFormat df3 = new SimpleDateFormat("hh:mm a");
        
        int rowPos = 0;
        
		for (TIRunParameters param : runParam) {
			writeLabelValue(rowPos++, "ID", String.valueOf(param.getRunParamId()), sheet, labelStyle, valueStyle);
	        writeLabelValue(rowPos++, "Exit Type", (param.getTimeExitType() == null ? "None Detected"
					: param.getTimeExitType().equals(TimeExitType.time) ? "Time of Day: " + df3.format(param.getExitTimeOfDay())
					: "Minutes from Entry: " + param.getMinutesFromEntry()), sheet, labelStyle, valueStyle);
			writeLabelValue(rowPos++, "Profit Target", (param.getPriceExitType() == null || param.getProfitTarget() == 0.0 ? "None Detected"
					: param.getPriceExitType().equals(PriceExitType.dollar) ? "$" + param.getProfitTarget()
					: param.getProfitTarget() + "%"), sheet, labelStyle, valueStyle);
			writeLabelValue(rowPos++, "Stop Loss", (param.getPriceExitType() == null ? "None Detected"
					: param.getPriceExitType().equals(PriceExitType.dollar) ? "$" + param.getStopLoss()
					: param.getStopLoss() + "%"), sheet, labelStyle, valueStyle);
			rowPos++;
        }
		
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
	}
	
	private static void writeRunParamSection(RunConfiguration param, XSSFWorkbook wb, XSSFSheet sheet) {        
        int rowPos = 0;
        
		writeLabelValue(rowPos++, "Run Configuration ID", String.valueOf(param.getRunStrategy()), sheet, labelStyle, valueStyle);
        writeLabelValue(rowPos++, "Min Avg Number of Trades Per Day", String.valueOf(param.getMinAvgNumberOfTradesPerDay()), sheet, labelStyle, valueStyle);
        writeLabelValue(rowPos++, "Min Trades", String.valueOf(param.getMinTrades()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Min Win Rate", String.valueOf(param.getMinWinRate()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Min PF", String.valueOf(param.getMinPf()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Min % of Days Traded", String.valueOf(param.getMinPercentOfDaysTraded()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "% of Days for In-Sample Test", String.valueOf(param.getPercentOfDaysForInSampleTest()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "% of Days for Final Test", String.valueOf(param.getPercentOfDaysForFinalTest()), sheet, labelStyle, valueStyle);
		rowPos++;
		writeLabelValue(rowPos++, "Optimize Count", String.valueOf(param.getOptimizeCount()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Discrete Count", String.valueOf(param.getDiscreteCount()), sheet, labelStyle, valueStyle);
		rowPos++;
		writeLabelValue(rowPos++, "Optimize Target/Stop", String.valueOf(param.isOptimizeTargetStop()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Only Optimize Target Stop", String.valueOf(param.isOnlyOptimizeTargetStop()), sheet, labelStyle, valueStyle);
		rowPos++;
		writeLabelValue(rowPos++, "Target/Stop Min PF", String.valueOf(param.getTsRunConfigurations().getTsMinPf()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Target/Stop Min Win Rate", String.valueOf(param.getTsRunConfigurations().getTsMinWinRate()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Target/Stop Min % of Targets Hit", String.valueOf(param.getTsRunConfigurations().getMinPercentOfTargetsHit()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Target/Stop Min % of Stops Hit", String.valueOf(param.getTsRunConfigurations().getMinPercentOfStopsHit()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Target to Stop Ratio", String.valueOf(param.getTsRunConfigurations().getTargetToStop()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Min Allowed Stop Loss", String.valueOf(param.getTsRunConfigurations().getMinStopLoss()), sheet, labelStyle, valueStyle);
		writeLabelValue(rowPos++, "Min Allowed Target", String.valueOf(param.getTsRunConfigurations().getMinProfitTarget()), sheet, labelStyle, valueStyle);
		rowPos++;
        
		sheet.setColumnWidth(0, 30*256);
		sheet.setColumnWidth(1, 30*256);
		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
	}
	
	private static void writeLabelValue(int rowPos, String label, double inSampleValue, double outOfSampleValue, double overallValue, double finalValue, XSSFSheet sheet, CellStyle labelStyle, CellStyle valueStyle) {
		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);

		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		row.createCell(3);
		row.createCell(4);
		XSSFCell labelCell = row.getCell(0);
		labelCell.setCellStyle(labelStyle);
		XSSFCell inSampleValueCell = row.getCell(1);
		XSSFCell outOfSampleValueCell = row.getCell(2);
		XSSFCell overallValueCell = row.getCell(3);
		XSSFCell finalValueCell = row.getCell(4);
		inSampleValueCell.setCellStyle(valueStyle);
		outOfSampleValueCell.setCellStyle(valueStyle);
		overallValueCell.setCellStyle(valueStyle);
		finalValueCell.setCellStyle(valueStyle);
		labelCell.setCellValue(label);
		inSampleValueCell.setCellValue(Utility.roundStr(inSampleValue, 2));
		outOfSampleValueCell.setCellValue(Utility.roundStr(outOfSampleValue, 2));
		overallValueCell.setCellValue(Utility.roundStr(overallValue, 2));
		finalValueCell.setCellValue(Utility.roundStr(finalValue, 2));
	}
	
	private static void writeLabelValue(int rowPos, String label, String inSampleValue, String outOfSampleValue, String overallValue, String finalValue, XSSFSheet sheet, CellStyle labelStyle, CellStyle valueStyle) {
		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		row.createCell(3);
		row.createCell(4);
		XSSFCell labelCell = row.getCell(0);
		labelCell.setCellStyle(labelStyle);
		XSSFCell inSampleValueCell = row.getCell(1);
		XSSFCell outOfSampleValueCell = row.getCell(2);
		XSSFCell overallValueCell = row.getCell(3);
		XSSFCell finalValueCell = row.getCell(4);
		if (inSampleValue != null && !inSampleValue.equals("")) {
			inSampleValueCell.setCellStyle(valueStyle);
		}
		if (outOfSampleValue != null && !outOfSampleValue.equals("")) {
			outOfSampleValueCell.setCellStyle(valueStyle);
		}
		if (overallValue != null && !overallValue.equals("")) {
			overallValueCell.setCellStyle(valueStyle);
		}
		if (finalValue != null && !finalValue.equals("")) {
			finalValueCell.setCellStyle(valueStyle);
		}
		labelCell.setCellValue(label);
		inSampleValueCell.setCellValue(inSampleValue);
		outOfSampleValueCell.setCellValue(outOfSampleValue);
		overallValueCell.setCellValue(overallValue);
		finalValueCell.setCellValue(finalValue);
	}
	
	private static void writeLabelValue(int rowPos, String label, String value, XSSFSheet sheet, CellStyle labelStyle, CellStyle valueStyle) {
		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		XSSFCell labelCell = row.getCell(0);
		labelCell.setCellStyle(labelStyle);
		XSSFCell inSampleValueCell = row.getCell(1);
		if (value != null && !value.equals("")) {
			inSampleValueCell.setCellStyle(valueStyle);
		}
		labelCell.setCellValue(label);
		inSampleValueCell.setCellValue(value);
	}
	
	private static void writeFilterRow(int rowPos, String var, double min, double max, XSSFSheet sheet, CellStyle labelStyle, CellStyle valueStyle) {
		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		
		XSSFCell varCell = row.getCell(0);
		varCell.setCellStyle(valueStyle);
		
		XSSFCell minCell = row.getCell(1);
		minCell.setCellStyle(valueStyle);
		
		XSSFCell maxCell = row.getCell(2);
		maxCell.setCellStyle(valueStyle);
		
		varCell.setCellValue(var);
		minCell.setCellValue(Utility.roundDown(min, 3));
		maxCell.setCellValue(Utility.roundUp(max, 3));
	}
	
	private static void writeStatDataSection(StratStats stat, XSSFWorkbook wb, XSSFSheet sheet, int rowPos) {
		sheet.createRow(rowPos);
		
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		row.createCell(3);
		row.createCell(4);
		row.createCell(5);
		row.createCell(6);
		row.createCell(7);
		row.createCell(8);
		
		XSSFCell headerCell = row.getCell(0);
		headerCell.setCellValue("Count");
		
		headerCell = row.getCell(1);
		headerCell.setCellValue("Symbol");
		
		headerCell = row.getCell(2);
		headerCell.setCellValue("Entry Date");
		
		headerCell = row.getCell(3);
		headerCell.setCellValue("Entry Price");
		
		headerCell = row.getCell(4);
		headerCell.setCellValue("Exit Date");
		
		headerCell = row.getCell(5);
		headerCell.setCellValue("Exit Price");
		
		headerCell = row.getCell(6);
		headerCell.setCellValue("Profit Percent");
		
		headerCell = row.getCell(7);
		headerCell.setCellValue("Cumulative Profit Percent");
		
		headerCell = row.getCell(8);
		headerCell.setCellValue("Out Of Sample");
		
		int count = 0;
		for (TradeRecord trade : stat.getTradeData().getTrades()) {
			writeDataRow(++rowPos, ++count, wb, sheet, stat, trade);
		}
		
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
	}

	private static Collection<TimeResults> getTimeData(StratStats stat) {
		Map<String, TimeResults> _0_1 = new HashMap<>();
		Map<String, TimeResults> _1_3 = new HashMap<>();
		Map<String, TimeResults> _3_10 = new HashMap<>();
		Map<String, TimeResults> _10_30 = new HashMap<>();
		Map<String, TimeResults> _30_1 = new HashMap<>();
		Map<String, TimeResults> _1_2 = new HashMap<>();
		Map<String, TimeResults> _2_3 = new HashMap<>();
		Map<String, TimeResults> _3_5 = new HashMap<>();
		Map<String, TimeResults> _1 = new HashMap<>();
		Map<String, TimeResults> _2 = new HashMap<>();
		Map<String, TimeResults> _3 = new HashMap<>();
		Map<String, TimeResults> _4 = new HashMap<>();
		Map<String, TimeResults> _5 = new HashMap<>();
		Map<String, TimeResults> _6 = new HashMap<>();
		Map<String, TimeResults> _7_max = new HashMap<>();
		
		String _0_1_key = "0 - 1 (Bad)";
		String _1_3_key = "1 - 3 (Acceptable)";
		String _3_10_key = "3 - 10 (Good)";
		String _10_30_key = "10 - 30 (Very Good)";
		String _30_1_key = "30 - 1";
		String _1_2_key = "1 - 2";
		String _2_3_key = "2 - 3";
		String _3_5_key = "3 - 5";
		String _1_key = "Same Day";
		String _2_key = "Next Day";
		String _3_key = "Day 3";
		String _4_key = "Day 4";
		String _5_key = "Day 5";
		String _6_key = "Day 6";
		String _7_max_key = "Day 7+";
		
		Calendar entryCal = Calendar.getInstance();
		Calendar exitCal = Calendar.getInstance();
		
		for (TradeRecord trade : stat.getTradeData().getTrades()) {
			TargetStopInfo targetStopInfo = stat.getTargetStopComboStrategyResult() != null
					? trade.getTargetStopInfoMap().get(stat.getTargetStopComboStrategyResult().getTargetStopComboResultId()) : null;
			
			Date exitTime = trade.getExitTime();
			
			if (targetStopInfo != null) {
				exitTime = targetStopInfo.getEstimatedExitDate();
			}

			long seconds = (exitTime.getTime() - trade.getEntryTime().getTime())/1000;
			
			if (seconds >= 0 && seconds < 60) {
				appendToMap(_0_1, _0_1_key, trade, targetStopInfo);
			} else if (seconds >= 60 && seconds < 180) {
				appendToMap(_1_3, _1_3_key, trade, targetStopInfo);
			} else if (seconds >= 180 && seconds < 600) {
				appendToMap(_3_10, _3_10_key, trade, targetStopInfo);
			} else if (seconds >= 600 && seconds < 1800) {
				appendToMap(_10_30, _10_30_key, trade, targetStopInfo);
			} else if (seconds >= 1800 && seconds < 3600) {
				appendToMap(_30_1, _30_1_key, trade, targetStopInfo);
			} else if (seconds >= 3600 && seconds < 7200) {
				appendToMap(_1_2, _1_2_key, trade, targetStopInfo);
			} else if (seconds >= 7200 && seconds < 10800) {
				appendToMap(_2_3, _2_3_key, trade, targetStopInfo);
			} else if (seconds >= 10800 && seconds < 18000) {
				appendToMap(_3_5, _3_5_key, trade, targetStopInfo);
			} else {
				entryCal.setTime(trade.getEntryTime());
				exitCal.setTime(exitTime);
				
				int entryDay = entryCal.get(Calendar.DATE);
				int exitDay = exitCal.get(Calendar.DATE);
				int diff = exitDay - entryDay;
				
				if (diff == 0) {
					appendToMap(_1, _1_key, trade, targetStopInfo);
				} else if (diff == 1) {
					appendToMap(_2, _2_key, trade, targetStopInfo);
				} else if (diff == 2) {
					appendToMap(_3, _3_key, trade, targetStopInfo);
				} else if (diff == 3) {
					appendToMap(_4, _4_key, trade, targetStopInfo);
				} else if (diff == 5) {
					appendToMap(_5, _5_key, trade, targetStopInfo);
				} else if (diff == 6) {
					appendToMap(_6, _6_key, trade, targetStopInfo);
				} else if (diff >= 7) {
					appendToMap(_7_max, _7_max_key, trade, targetStopInfo);
				}
			}
		}
		
		List<TimeResults> list = new ArrayList<>();
		list.addAll(new ArrayList<TimeResults>(_0_1.values()));
		list.addAll(new ArrayList<TimeResults>(_1_3.values()));
		list.addAll(new ArrayList<TimeResults>(_3_10.values()));
		list.addAll(new ArrayList<TimeResults>(_10_30.values()));
		list.addAll(new ArrayList<TimeResults>(_30_1.values()));
		list.addAll(new ArrayList<TimeResults>(_1_2.values()));
		list.addAll(new ArrayList<TimeResults>(_2_3.values()));
		list.addAll(new ArrayList<TimeResults>(_3_5.values()));
		list.addAll(new ArrayList<TimeResults>(_1.values()));
		list.addAll(new ArrayList<TimeResults>(_2.values()));
		list.addAll(new ArrayList<TimeResults>(_3.values()));
		list.addAll(new ArrayList<TimeResults>(_4.values()));
		list.addAll(new ArrayList<TimeResults>(_5.values()));
		list.addAll(new ArrayList<TimeResults>(_6.values()));
		list.addAll(new ArrayList<TimeResults>(_7_max.values()));
		
		return list;
	}
	
	private static void appendToMap(Map<String, TimeResults> map, String key, TradeRecord trade, TargetStopInfo targetStopInfo) {		
		double profitPercent = trade.getProfitPercent();
		
		if (targetStopInfo != null) {
			profitPercent = targetStopInfo.getPriceDiff() / targetStopInfo.getEntryPrice();
		}
		
		if (map.get(key) != null) {
			TimeResults results = map.get(key);
			results.setTradeCount(results.getTradeCount() + 1);
			results.setProfitPercent(results.getProfitPercent() + profitPercent);
		} else {
			TimeResults results = new TimeResults();
			results.setTimeCateg(key);
			results.setTradeCount(1);
			results.setProfitPercent(profitPercent);

			map.put(key, results);
		}
	}
	
	private static void writeOrdersTable(List<Order> orders, XSSFWorkbook wb, XSSFSheet sheet) {
		System.out.println("Writing Orders Table ...");
		
		sheet.createRow(0);
		
		XSSFRow row = sheet.getRow(0);
		
		final int COL_COUNT = 15;
		for (int i = 0; i < COL_COUNT; i++) {
			row.createCell(i);
		}

		int cellPos = 0;
		XSSFCell headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Submitted");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Order ID");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Parent ID");

		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Filled");

		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Partial Filled");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Cancelled");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Symbol");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Status");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Shares");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Strategy");

		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Shares Filled");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Avg Fill");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Order Type");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Limit");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Buy/Sell");

		int rowPos = 1;
		for (Order order : orders) {
			writeOrderRow(rowPos++, wb, sheet, order);
		}
		
		for (int i = 0; i < COL_COUNT; i++) {
			sheet.autoSizeColumn(i);
		}
		
		System.out.println("Completed Orders Table ...");
	}
	
	private static void writeOrderRow(int rowPos, XSSFWorkbook wb, XSSFSheet sheet, Order order) {
		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);
		
		final int COL_COUNT = 15;
		for (int i = 0; i < COL_COUNT; i++) {
			row.createCell(i);
		}
		
		int cellPos = 0;
		XSSFCell valueCell = row.getCell(cellPos++);
		valueCell.setCellStyle(dateMinuteSecondStyle);
		valueCell.setCellValue(order.getSubmittedTs());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getOrderId());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getParentId() == null ? 0 : order.getParentId());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellStyle(dateMinuteSecondStyle);
		valueCell.setCellValue(order.getFilledTs());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellStyle(dateMinuteSecondStyle);
		valueCell.setCellValue(order.getPartialFilledTs());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellStyle(dateMinuteSecondStyle);
		valueCell.setCellValue(order.getCancelledTs());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getSymbol());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getStatus());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getShares() == null ? 0 : order.getShares());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getStrategy());

		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getFilled() == null ? 0 : order.getFilled());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getAvgFillPrice() == null || order.getAvgFillPrice() == 0.0 ? "" : String.valueOf(order.getAvgFillPrice()));
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getType());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getLimit() == null || order.getLimit() == 0.0 ? "" : String.valueOf(order.getLimit()));
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(order.getBuy() ? "Buy" : "Sell");
	}
	
	private static void writeTradesTable(List<Trade> trades, XSSFWorkbook wb, XSSFSheet sheet) {
		System.out.println("Writing Trades Table ...");
		
		sheet.createRow(0);
		
		XSSFRow row = sheet.getRow(0);
		
		final int COL_COUNT = 22;
		for (int i = 0; i < COL_COUNT; i++) {
			row.createCell(i);
		}

		int cellPos = 0;
		XSSFCell headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Entry Date");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Exit Date");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Strategy Name");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Symbol");

		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("L/S");

		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Filled Shares");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Entry Price");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Exposure");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Exit Shares");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Exit Price");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Profit ($)");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Profit w/ Commission ($)");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Profit (%)");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Entry Type");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Entry Limit");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Target Type");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Target Limit");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Stop Order Type");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Stop Price");

		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Stop Limit");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Timeout Order Type");
		
		headerCell = row.getCell(cellPos++);
		headerCell.setCellStyle(labelStyle);
		headerCell.setCellValue("Timeout Limit");
		
		int rowPos = 1;
		for (Trade trade : trades) {
			writeTradeRow(rowPos++, wb, sheet, trade);
		}
		
		for (int i = 0; i < COL_COUNT; i++) {
			sheet.autoSizeColumn(i);
		}
		
		System.out.println("Completed Trades Table ...");
	}
	
	private static void writeTradeRow(int rowPos, XSSFWorkbook wb, XSSFSheet sheet, Trade trade) {
		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);
		
		final int COL_COUNT = 22;
		for (int i = 0; i < COL_COUNT; i++) {
			row.createCell(i);
		}
		
		int cellPos = 0;
		XSSFCell valueCell = row.getCell(cellPos++);
		valueCell.setCellStyle(dateMinuteSecondStyle);
		valueCell.setCellValue(trade.getEntryDate());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellStyle(dateMinuteSecondStyle);
		valueCell.setCellValue(trade.getExitDate());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getStrategy());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getSymbol());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.isShort() ? "S" : "L");
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getFilledShares());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getEntryPrice() == null || trade.getEntryPrice() == 0.0 ? "" : String.valueOf(trade.getEntryPrice()));
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getExposure());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getExitShares());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getExitPrice() == null || trade.getExitPrice() == 0.0 ? "" : String.valueOf(trade.getExitPrice()));
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getProfit());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getProfitWithCommission());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getPercentProfit()*100);
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getEntryOrderType());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getEntryLimit() == null || trade.getEntryLimit() == 0.0 ? "" : String.valueOf(trade.getEntryLimit()));
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getTargetOrderType());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getTargetLimitPrice() == null || trade.getTargetLimitPrice() == 0.0 ? "" : String.valueOf(trade.getTargetLimitPrice()));
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getStopOrderType());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getStopPrice() == null || trade.getStopPrice() == 0.0 ? "" : String.valueOf(trade.getStopPrice()));
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getStopLimitPrice() == null || trade.getStopLimitPrice() == 0.0 ? "" : String.valueOf(trade.getStopLimitPrice()));
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getTimeoutOrderType());
		
		valueCell = row.getCell(cellPos++);
		valueCell.setCellValue(trade.getTimeoutLimitPrice() == null || trade.getTimeoutLimitPrice() == 0.0 ? "" : String.valueOf(trade.getTimeoutLimitPrice()));
	}
	
	private static void writeStatDayDataSection(Collection<DailyResults> dailyResults, XSSFWorkbook wb, XSSFSheet sheet, int rowPos) {
		sheet.createRow(rowPos);
		
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		row.createCell(3);
		row.createCell(4);

		XSSFCell headerCell = row.getCell(0);
		headerCell.setCellValue("Date");
		
		headerCell = row.getCell(1);
		headerCell.setCellValue("Count");
		
		headerCell = row.getCell(2);
		headerCell.setCellValue("Cumulative Count");
		
		headerCell = row.getCell(3);
		headerCell.setCellValue("Profit Percent");
		
		headerCell = row.getCell(4);
		headerCell.setCellValue("Cumulative Profit Percent");

		int count = 0;
		for (DailyResults dayData : dailyResults) {
			writeDayDataRow(++rowPos, ++count, wb, sheet, dayData);
		}
	}
	
	private static void writeStatTimeDataSection(Collection<TimeResults> timeResults, XSSFWorkbook wb, XSSFSheet sheet, int rowPos) {
		sheet.createRow(rowPos);
		
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		row.createCell(3);
		row.createCell(4);

		XSSFCell headerCell = row.getCell(0);
		headerCell.setCellValue("Category");
		
		headerCell = row.getCell(1);
		headerCell.setCellValue("Count");
		
		headerCell = row.getCell(2);
		headerCell.setCellValue("Profit Percent");

		int count = 0;
		for (TimeResults timeData : timeResults) {
			writeTimeDataRow(++rowPos, ++count, wb, sheet, timeData);
		}
	}
	
	private static void writeDataRow(int rowPos, int count, XSSFWorkbook wb, XSSFSheet sheet, StratStats stat, TradeRecord trade) {
		TargetStopInfo targetStopInfo = stat.getTargetStopComboStrategyResult() != null
				? trade.getTargetStopInfoMap().get(stat.getTargetStopComboStrategyResult().getTargetStopComboResultId())
				: null;

		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		row.createCell(3);
		row.createCell(4);
		row.createCell(5);
		row.createCell(6);
		row.createCell(7);
		row.createCell(8);
		
		XSSFCell valueCell = row.getCell(0);
		valueCell.setCellValue(count);
		
		valueCell = row.getCell(1);
		valueCell.setCellValue(trade.getSymbol());
		
		valueCell = row.getCell(2);
		valueCell.setCellStyle(dateMinuteSecondStyle);
		valueCell.setCellValue(trade.getEntryTime());
		
		valueCell = row.getCell(3);
		valueCell.setCellValue(trade.getEntryPrice());
		
		valueCell = row.getCell(4);
		valueCell.setCellStyle(dateMinuteSecondStyle);
		valueCell.setCellValue(targetStopInfo != null ? targetStopInfo.getEstimatedExitDate() : trade.getExitTime());
		
		valueCell = row.getCell(5);
		valueCell.setCellValue(targetStopInfo != null ? targetStopInfo.getExitPrice() : trade.getExitPrice());
		
		valueCell = row.getCell(6);
		valueCell.setCellValue(targetStopInfo != null ? (targetStopInfo.getPriceDiff()/targetStopInfo.getEntryPrice()) * 100.0 : trade.getProfitPercent() * 100.0);
		
		valueCell = row.getCell(7);
		valueCell.setCellValue(stat.getCumulativeProfitPercent(trade.getId()) * 100.0);
		
		valueCell = row.getCell(8);
		if (trade.isOutOfSample()) {
			valueCell.setCellValue(1);
		}
	}
	
	private static void writeDayDataRow(int rowPos, int count, XSSFWorkbook wb, XSSFSheet sheet, DailyResults dayData) {
		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		row.createCell(3);
		row.createCell(4);
		
		XSSFCell valueCell = row.getCell(0);
		valueCell.setCellStyle(dateDayStyle);
		valueCell.setCellValue(dayData.getDate());
		
		valueCell = row.getCell(1);
		valueCell.setCellValue(dayData.getTradeCount());
		
		valueCell = row.getCell(2);
		valueCell.setCellValue(dayData.getCumulativeTradeCount());
		
		valueCell = row.getCell(3);
		valueCell.setCellValue(dayData.getProfitPercent() * 100.0);
		
		valueCell = row.getCell(4);
		valueCell.setCellValue(dayData.getCumulativeProfitPercent() * 100.0);
	}
	
	private static void writeTimeDataRow(int rowPos, int count, XSSFWorkbook wb, XSSFSheet sheet, TimeResults timeData) {
		sheet.createRow(rowPos);
		XSSFRow row = sheet.getRow(rowPos);
		
		row.createCell(0);
		row.createCell(1);
		row.createCell(2);
		
		XSSFCell valueCell = row.getCell(0);
		valueCell.setCellValue(timeData.getTimeCateg());
		
		valueCell = row.getCell(1);
		valueCell.setCellValue(timeData.getTradeCount());
		
		valueCell = row.getCell(2);
		valueCell.setCellValue(timeData.getProfitPercent() * 100.0);
	}
	
	public static void buildGemAnalysisResultsExcel(List<StratStats> allStats, String strategyName) {
		try {
			Set<GemParameters> uniqueGems = new HashSet<>();
			
			File outputDir = new File(PathResources.GEMS_ANALYSIS_PATH);
			
			int maxSeq = getMaxSeq(outputDir, false);
			
			File outFile = new File(outputDir, (maxSeq + 1) + ") Gem Analysis Results" + (strategyName != null && !strategyName.trim().isEmpty() ? " [" + strategyName + "]" : "") + ".xlsx") ;
			File tempFile = new File(PathResources.GEMS_ANALYSIS_PATH, "temp.xlsx");
			FileUtils.copyFile(new File(PathResources.GEMS_ANALYSIS_RESULTS_TEMPLATE_PATH), tempFile);
			
			XSSFWorkbook wb = new XSSFWorkbook(tempFile);
			XSSFSheet sheet = wb.getSheet("Gems Analysis Results");

			Font font = wb.createFont();
			font.setBold(true);
			
			Font whiteFont = wb.createFont();
			whiteFont.setColor(IndexedColors.WHITE.index);
			
	        valueStyle = wb.createCellStyle();
	        valueStyle.setBorderBottom(BorderStyle.THIN);
	        valueStyle.setBorderTop(BorderStyle.THIN);
	        valueStyle.setBorderLeft(BorderStyle.THIN);
	        valueStyle.setBorderRight(BorderStyle.THIN);
	        valueStyle.setAlignment(HorizontalAlignment.CENTER);
	        valueStyle.setWrapText(false);
			
	        CellStyle dateValueStyle = wb.createCellStyle();
	        dateValueStyle.setBorderBottom(BorderStyle.THIN);
	        dateValueStyle.setBorderTop(BorderStyle.THIN);
	        dateValueStyle.setBorderLeft(BorderStyle.THIN);
	        dateValueStyle.setBorderRight(BorderStyle.THIN);
	        dateValueStyle.setAlignment(HorizontalAlignment.CENTER);
	        dateValueStyle.setWrapText(false);
	        dateValueStyle.setDataFormat(wb.createDataFormat().getFormat("mm/dd/yyyy"));
	        
	        CellStyle yellowValueStyle = wb.createCellStyle();
	        yellowValueStyle.setBorderBottom(BorderStyle.THIN);
	        yellowValueStyle.setBorderTop(BorderStyle.THIN);
	        yellowValueStyle.setBorderLeft(BorderStyle.THIN);
	        yellowValueStyle.setBorderRight(BorderStyle.THIN);
	        yellowValueStyle.setAlignment(HorizontalAlignment.CENTER);
	        yellowValueStyle.setWrapText(false);
	        yellowValueStyle.setFillForegroundColor(IndexedColors.YELLOW.index);
	        yellowValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        
	        CellStyle greenValueStyle = wb.createCellStyle();
	        greenValueStyle.setBorderBottom(BorderStyle.THIN);
	        greenValueStyle.setBorderTop(BorderStyle.THIN);
	        greenValueStyle.setBorderLeft(BorderStyle.THIN);
	        greenValueStyle.setBorderRight(BorderStyle.THIN);
	        greenValueStyle.setAlignment(HorizontalAlignment.CENTER);
	        greenValueStyle.setWrapText(false);
	        greenValueStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.index);
	        greenValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        
	        CellStyle orangeValueStyle = wb.createCellStyle();
	        orangeValueStyle.setBorderBottom(BorderStyle.THIN);
	        orangeValueStyle.setBorderTop(BorderStyle.THIN);
	        orangeValueStyle.setBorderLeft(BorderStyle.THIN);
	        orangeValueStyle.setBorderRight(BorderStyle.THIN);
	        orangeValueStyle.setAlignment(HorizontalAlignment.CENTER);
	        orangeValueStyle.setWrapText(false);
	        orangeValueStyle.setFillForegroundColor(IndexedColors.ORANGE.index);
	        orangeValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        
	        CellStyle purpleValueStyle = wb.createCellStyle();
	        purpleValueStyle.setBorderBottom(BorderStyle.THIN);
	        purpleValueStyle.setBorderTop(BorderStyle.THIN);
	        purpleValueStyle.setBorderLeft(BorderStyle.THIN);
	        purpleValueStyle.setBorderRight(BorderStyle.THIN);
	        purpleValueStyle.setAlignment(HorizontalAlignment.CENTER);
	        purpleValueStyle.setWrapText(false);
	        purpleValueStyle.setFillForegroundColor(IndexedColors.VIOLET.index);
	        purpleValueStyle.setFont(whiteFont);
	        purpleValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        
	        CellStyle percentStyle = wb.createCellStyle();
	        percentStyle.setBorderBottom(BorderStyle.THIN);
	        percentStyle.setBorderTop(BorderStyle.THIN);
	        percentStyle.setBorderLeft(BorderStyle.THIN);
	        percentStyle.setBorderRight(BorderStyle.THIN);
	        percentStyle.setAlignment(HorizontalAlignment.CENTER);
	        percentStyle.setWrapText(false);
	        percentStyle.setDataFormat(wb.createDataFormat().getFormat("0.0#%"));
	        
	        CellStyle intPercentStyle = wb.createCellStyle();
	        intPercentStyle.setBorderBottom(BorderStyle.THIN);
	        intPercentStyle.setBorderTop(BorderStyle.THIN);
	        intPercentStyle.setBorderLeft(BorderStyle.THIN);
	        intPercentStyle.setBorderRight(BorderStyle.THIN);
	        intPercentStyle.setAlignment(HorizontalAlignment.CENTER);
	        intPercentStyle.setWrapText(false);
	        intPercentStyle.setDataFormat(wb.createDataFormat().getFormat("0%"));
	        
	        CellStyle doubleValueStyle = wb.createCellStyle();
	        doubleValueStyle.setBorderBottom(BorderStyle.THIN);
	        doubleValueStyle.setBorderTop(BorderStyle.THIN);
	        doubleValueStyle.setBorderLeft(BorderStyle.THIN);
	        doubleValueStyle.setBorderRight(BorderStyle.THIN);
	        doubleValueStyle.setAlignment(HorizontalAlignment.CENTER);
	        doubleValueStyle.setWrapText(false);
	        doubleValueStyle.setDataFormat(wb.createDataFormat().getFormat("0.0##"));
	        
	        CellStyle doubleValueOneDecimalStyle = wb.createCellStyle();
	        doubleValueOneDecimalStyle.setBorderBottom(BorderStyle.THIN);
	        doubleValueOneDecimalStyle.setBorderTop(BorderStyle.THIN);
	        doubleValueOneDecimalStyle.setBorderLeft(BorderStyle.THIN);
	        doubleValueOneDecimalStyle.setBorderRight(BorderStyle.THIN);
	        doubleValueOneDecimalStyle.setAlignment(HorizontalAlignment.CENTER);
	        doubleValueOneDecimalStyle.setWrapText(false);
	        doubleValueOneDecimalStyle.setDataFormat(wb.createDataFormat().getFormat("0.0"));
	        
	        CellStyle doubleValueTwoDecimalStyle = wb.createCellStyle();
	        doubleValueTwoDecimalStyle.setBorderBottom(BorderStyle.THIN);
	        doubleValueTwoDecimalStyle.setBorderTop(BorderStyle.THIN);
	        doubleValueTwoDecimalStyle.setBorderLeft(BorderStyle.THIN);
	        doubleValueTwoDecimalStyle.setBorderRight(BorderStyle.THIN);
	        doubleValueTwoDecimalStyle.setAlignment(HorizontalAlignment.CENTER);
	        doubleValueTwoDecimalStyle.setWrapText(false);
	        doubleValueTwoDecimalStyle.setDataFormat(wb.createDataFormat().getFormat("0.0#"));
	        
	        CellStyle intValueStyle = wb.createCellStyle();
	        intValueStyle.setBorderBottom(BorderStyle.THIN);
	        intValueStyle.setBorderTop(BorderStyle.THIN);
	        intValueStyle.setBorderLeft(BorderStyle.THIN);
	        intValueStyle.setBorderRight(BorderStyle.THIN);
	        intValueStyle.setAlignment(HorizontalAlignment.CENTER);
	        intValueStyle.setWrapText(false);
	        intValueStyle.setDataFormat(wb.createDataFormat().getFormat("0"));

	        int rowPos = 1;
			for (StratStats stat : allStats) {
				if (uniqueGems.contains(stat.getGemParam())) {
					continue;
				} else {
					uniqueGems.add(stat.getGemParam());
				}
				
				XSSFRow row = sheet.createRow(rowPos);

				GemParameters gemParam = stat.getGemParam();
				
				Collection<TimeResults> timeData = getTimeData(stat);
				
				int _1MinCount = 0;
				double _1MinProfitPercent = 0.0;
				for (TimeResults r : timeData) {
					if ("0 - 1 (Bad)".equals(r.getTimeCateg())) {
						_1MinCount = r.getTradeCount();
						_1MinProfitPercent += r.getProfitPercent();
					}
				}

				int originalGemTradeCount = stat.getGemParam().getGemOverallTradeCount() + stat.getGemParam().getGemFinalTradeCount();
				
				boolean yellowStrategy = GemReviewResult.PendingReview.equals(gemParam.getReviewResult()) && stat.getTradeData().getTrades().size() > originalGemTradeCount && !stat.isReviewed();
				boolean greenStrategy = GemReviewResult.Successful.equals(gemParam.getReviewResult());
				boolean orangeStrategy = GemReviewResult.Reviewed.equals(gemParam.getReviewResult());
				boolean paperTradingStrategy = GemReviewResult.PaperTrading.equals(gemParam.getReviewResult());
				
				int colPos = 0;
				XSSFCell pendingReviewCell = row.createCell(colPos++);
				if ((yellowStrategy || greenStrategy || orangeStrategy)
						&& stat.getMaxTradeDate().after(gemParam.getGemFileLastModifiedDate())) {
					pendingReviewCell.setCellStyle(yellowValueStyle);
				} else {
					pendingReviewCell.setCellStyle(valueStyle);
				}
				
				XSSFCell strategyIdCell = row.createCell(colPos++);
				if (yellowStrategy) {
					strategyIdCell.setCellStyle(yellowValueStyle);
				} else if (greenStrategy) {
					strategyIdCell.setCellStyle(greenValueStyle);
				} else if (orangeStrategy) {
					strategyIdCell.setCellStyle(orangeValueStyle);
				} else if (paperTradingStrategy) {
					strategyIdCell.setCellStyle(purpleValueStyle);
				} else {
					strategyIdCell.setCellStyle(valueStyle);
				}
				strategyIdCell.setCellValue(stat.getStatId());
				
				XSSFCell maxTradeDateCell = row.createCell(colPos++);
				maxTradeDateCell.setCellStyle(dateValueStyle);
				maxTradeDateCell.setCellValue(stat.getMaxTradeDate());
				
				XSSFCell strategyNameCell = row.createCell(colPos++);
				strategyNameCell.setCellStyle(valueStyle);
				strategyNameCell.setCellValue(gemParam.getStrategyName());
				
				XSSFCell runConfigCell = row.createCell(colPos++);
				runConfigCell.setCellStyle(valueStyle);
				runConfigCell.setCellValue(gemParam.getRunConfigName());
				
				XSSFCell resultTypeCell = row.createCell(colPos++);
				resultTypeCell.setCellStyle(valueStyle);
				resultTypeCell.setCellValue(gemParam.getResultType());
				
				if (stat.getFilters() != null && stat.getFilters().size() > 1) {
					StringBuilder filterVars = new StringBuilder("");
					StringBuilder filterMin = new StringBuilder("");
					StringBuilder filterMax = new StringBuilder("");
					if (stat.getFilters() != null && !stat.getFilters().isEmpty()) {
						for (FilterRange filter : stat.getFilters()) {
							filterVars.append((filterVars.toString().length() > 0 ? "\r\n" : "") + filter.getVar());
							filterMin.append((filterMin.toString().length() > 0 ? "\r\n" : "") + filter.getMin());
							filterMax.append((filterMax.toString().length() > 0 ? "\r\n" : "") + filter.getMax());
						}
						
						XSSFCell filterVarCell = row.createCell(colPos++);
						filterVarCell.setCellStyle(valueStyle);
						filterVarCell.setCellValue(filterVars.toString());
						
						XSSFCell filterMinCell = row.createCell(colPos++);
						filterMinCell.setCellStyle(valueStyle);
						filterMinCell.setCellValue(filterMin.toString());
						
						XSSFCell filterMaxCell = row.createCell(colPos++);
						filterMaxCell.setCellStyle(valueStyle);
						filterMaxCell.setCellValue(filterMax.toString());
					}
				} else if (stat.getFilters() != null && !stat.getFilters().isEmpty()) {
					FilterRange f = stat.getFilters().get(0);

					XSSFCell filterVarCell = row.createCell(colPos++);
					filterVarCell.setCellStyle(doubleValueStyle);
					filterVarCell.setCellValue(f.getVar());

					XSSFCell filterMinCell = row.createCell(colPos++);
					filterMinCell.setCellStyle(doubleValueStyle);
					filterMinCell.setCellValue(f.getMin());

					XSSFCell filterMaxCell = row.createCell(colPos++);
					filterMaxCell.setCellStyle(doubleValueStyle);
					filterMaxCell.setCellValue(f.getMax());
				}

				
				boolean isTargetDollar = gemParam.getTargetDollar() > 0;
				boolean isTargetPercent = gemParam.getTargetPercent() > 0;
				boolean isTargetPercentATR = gemParam.getTargetPercentATR() > 0;
				
				String targetType = "";
				Double target = null; 
				if (isTargetDollar) {
					targetType = "$";
					target = gemParam.getTargetDollar();
				} else if (isTargetPercent) {
					targetType = "%";
					target = gemParam.getTargetPercent();
				} else if (isTargetPercentATR) {
					targetType = "ATR%";
					target = gemParam.getTargetPercentATR();
				}
				
				XSSFCell targetTypeCell = row.createCell(colPos++);
				targetTypeCell.setCellStyle(valueStyle);
				targetTypeCell.setCellValue(targetType);
				
				XSSFCell targetCell = row.createCell(colPos++);
				targetCell.setCellStyle(doubleValueTwoDecimalStyle);
				if (target == null) {
					targetCell.setCellValue("");
				} else {
					targetCell.setCellValue(target);
				}
				
				boolean isStopDollar = gemParam.getStopLossDollar() > 0;
				boolean isStopPercent = gemParam.getStopLossPercent() > 0;
				boolean isStopPercentATR = gemParam.getStopLossPercentATR() > 0;

				String stopType = "";
				Double stop = null;
				if (isStopDollar) {
					stopType = "$";
					stop = gemParam.getStopLossDollar();
				} else if (isStopPercent) {
					stopType = "%";
					stop = gemParam.getStopLossPercent();
				} else if (isStopPercentATR) {
					stopType = "ATR%";
					stop = gemParam.getStopLossPercentATR();
				} else if(gemParam.isSmartStop()) {
					stopType = "Smart";
				}

				XSSFCell stopTypeCell = row.createCell(colPos++);
				stopTypeCell.setCellStyle(valueStyle);
				stopTypeCell.setCellValue(stopType);

				XSSFCell stopCell = row.createCell(colPos++);
				stopCell.setCellStyle(doubleValueTwoDecimalStyle);
				if (stop == null) {
					stopCell.setCellValue("");
				} else {
					stopCell.setCellValue(stop);
				}

				XSSFCell overallPfCell = row.createCell(colPos++);
				overallPfCell.setCellStyle(doubleValueOneDecimalStyle);
				overallPfCell.setCellValue(stat.getOverallPf());

				XSSFCell overallWinRateCell = row.createCell(colPos++);
				overallWinRateCell.setCellStyle(percentStyle);
				overallWinRateCell.setCellValue(stat.getOverallWinRate()/100);
				
				XSSFCell overallAvgWinCell = row.createCell(colPos++);
				overallAvgWinCell.setCellStyle(doubleValueTwoDecimalStyle);
				overallAvgWinCell.setCellValue(stat.getAvgWinner());
				
				XSSFCell overallAvgLoseCell = row.createCell(colPos++);
				overallAvgLoseCell.setCellStyle(doubleValueTwoDecimalStyle);
				overallAvgLoseCell.setCellValue(stat.getAvgLoser());
				
				XSSFCell overallAvgWinLoseCell = row.createCell(colPos++);
				overallAvgWinLoseCell.setCellStyle(doubleValueTwoDecimalStyle);
				overallAvgWinLoseCell.setCellValue(stat.getAvgWinner() / stat.getAvgLoser());
				
				XSSFCell overallDrawDownCell = row.createCell(colPos++);
				overallDrawDownCell.setCellStyle(intPercentStyle);
				overallDrawDownCell.setCellValue(stat.getOverallDrawDown()/100);
				
				XSSFCell overallPercDaysTradedCell = row.createCell(colPos++);
				overallPercDaysTradedCell.setCellStyle(percentStyle);
				overallPercDaysTradedCell.setCellValue(stat.getOverallPercentDaysTraded()/100);
				
				XSSFCell finalPfCell = row.createCell(colPos++);
				finalPfCell.setCellStyle(doubleValueOneDecimalStyle);
				finalPfCell.setCellValue(stat.getFinalPf());

				XSSFCell finalWinRateCell = row.createCell(colPos++);
				finalWinRateCell.setCellStyle(percentStyle);
				finalWinRateCell.setCellValue(stat.getFinalWinRate()/100);
				
				XSSFCell finalAvgWinCell = row.createCell(colPos++);
				finalAvgWinCell.setCellStyle(doubleValueTwoDecimalStyle);
				finalAvgWinCell.setCellValue(stat.getAvgWinner());
				
				XSSFCell finalAvgLoseCell = row.createCell(colPos++);
				finalAvgLoseCell.setCellStyle(doubleValueTwoDecimalStyle);
				finalAvgLoseCell.setCellValue(stat.getAvgLoser());
				
				XSSFCell finalDrawDownCell = row.createCell(colPos++);
				finalDrawDownCell.setCellStyle(intPercentStyle);
				finalDrawDownCell.setCellValue(stat.getFinalDrawDown()/100);
				
				XSSFCell finalPercDaysTradedCell = row.createCell(colPos++);
				finalPercDaysTradedCell.setCellStyle(intPercentStyle);
				finalPercDaysTradedCell.setCellValue(stat.getFinalPercentDaysTraded()/100);
				
				XSSFCell finalTradeCountCell = row.createCell(colPos++);
				finalTradeCountCell.setCellStyle(intValueStyle);
				finalTradeCountCell.setCellValue(stat.getFinalTradeCount());

				XSSFCell percTargetsHitCell = row.createCell(colPos++);
				percTargetsHitCell.setCellStyle(intPercentStyle);
				percTargetsHitCell.setCellValue(stat.getOverallPercentOfTargetsHit()/100);
				
				XSSFCell percStopsHitCell = row.createCell(colPos++);
				percStopsHitCell.setCellStyle(intPercentStyle);
				percStopsHitCell.setCellValue(stat.getOverallPercentOfStopsHit()/100);
				
				XSSFCell _1MinCountCell = row.createCell(colPos++);
				_1MinCountCell.setCellStyle(intValueStyle);
				_1MinCountCell.setCellValue(_1MinCount);
				
				XSSFCell _1MinProfitPercCell = row.createCell(colPos++);
				_1MinProfitPercCell.setCellStyle(percentStyle);
				_1MinProfitPercCell.setCellValue(_1MinProfitPercent);
				
				XSSFCell finalCumulProfitPercCell = row.createCell(colPos++);
				finalCumulProfitPercCell.setCellStyle(percentStyle);
				finalCumulProfitPercCell.setCellValue(stat.getFinalCumulativeProfitPercent()/100);
				
				rowPos++;
			}
		
			sheet.autoSizeColumn(0);
			
			FileOutputStream fs = new FileOutputStream(outFile);
			wb.write(fs);
			wb.close();
			
			tempFile.delete();
		} catch (IOException | InvalidFormatException e) {
			e.printStackTrace();
		}
	}
}
