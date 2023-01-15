package com.op;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.op.bo.ExcelBuilder;
import com.op.bo.PathResources;
import com.op.bo.PerformanceData;
import com.op.bo.RobotLogsReader;
import com.op.bo.TradeData;

public class PerformanceAnalysis {

	private static List<TradeData> simulatedTradeData = new ArrayList<>();
	
	public static void main (String[] args) throws IOException, ParseException, InterruptedException {
		loadTradeData();
		PerformanceData performanceData = RobotLogsReader.readLogData();
		ExcelBuilder.buildPerformanceAnalysisExcel(performanceData);
	}
	
	private static void loadTradeData() throws IOException, ParseException {
		File dataDir = new File(PathResources.DATA_PATH);
		File[] allFiles = dataDir.listFiles();

		boolean multipleGroups = isMultipleGroups(allFiles);

		System.out.println("Multiple Groups Flag = " + multipleGroups + "\n");

		Map<String, List<File>> runGroups = new HashMap<String, List<File>>();
		if (multipleGroups) {
			for (File f : allFiles) {
				if (!f.isDirectory()) {
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
				}
			}
		} else {
			List<File> allFilesList = new ArrayList<File>();
			for (File f : allFiles) {
				if (!f.isDirectory()) {
					allFilesList.add(f);
				}
			}

			runGroups.put("NoGroups", allFilesList);
		}
		
		for (Entry<String, List<File>> entry : runGroups.entrySet()) {
			System.out.println("Loading data for trade data group: " + entry.getKey() + "\n");
			TradeData data = new TradeData(entry.getValue(), 100, 0, null);
			simulatedTradeData.add(data);
		}
	}
	
	private static boolean isMultipleGroups(File[] allFiles) {
		Set<String> names = new HashSet<>();
		for (File f : allFiles) {
			if (!f.isDirectory()) {
				String fileName = f.getName();
				String[] array = fileName.split(PathResources.FILE_NAME_SEPARATOR);

				names.add(array[0]);
			}
		}
		
		return names.size() > 1;
	}
}
