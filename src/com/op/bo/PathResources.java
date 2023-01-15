package com.op.bo;

import java.io.File;

public class PathResources {
	private static final String HOME = System.getProperty("user.home");
	
	public static final String PROJECT_NAME = "StratStud";
	public static final String TEMPLATES_DIR = "templates";
	public static final String RESULTS_DIR = "results";
	public static final String GEMS_DIR = "gems";
	public static final String GEMS_ANALYSIS_DIR = "gems_analysis";
	public static final String ANALYSIS_DIR_NAME = "Analysis";
	public static final String PERFORMANCE_DIR = "performance";
	public static final String FILE_NAME_SEPARATOR = "_";
	public static final String DATA_SEPARATOR = ",";
	public static final String TARGET_STOP_MAE_MFE_FILE_NAME_1 = "targetstop";
	public static final String TARGET_STOP_MAE_MFE_FILE_NAME_2 = "target-stop";
	public static final String BACK_UP_DATA_FOLDER_NAME = "backup";
	public static final String SUPPLEMENT_DATA_FOLDER_NAME = "supplement-data";
	public static final String SINGLE_RUN_DATA_FOLDER_NAME = "single-run";
	
	/**
	 * Location of the data exported from Trade Ideas to be optimized.
	 */
	public static final String DATA_PATH = HOME + File.separator + PROJECT_NAME + File.separator + "data";
	public static final String SINGLE_RUN_DATA_PATH = HOME + File.separator + PROJECT_NAME + File.separator + "data" + File.separator + SINGLE_RUN_DATA_FOLDER_NAME;
	
	/**
	 * Location of the Trade Ideas robotlogs files.
	 */
	public static final String ROBOT_LOGS_PATH = HOME + File.separator + "Documents" + File.separator + "TradeIdeasPro" + File.separator + "robotlogs";

	//Excel Export
	public static final String STRATEGY_STATS_TEMPLATE = "STRATEGY_STATS_TEMPLATE.xlsx";
	public static final String TEMPLATE_PATH = HOME + File.separator + PROJECT_NAME + File.separator + TEMPLATES_DIR + File.separator + STRATEGY_STATS_TEMPLATE;
	public static final String RESULTS_DIR_PATH =  HOME + File.separator + PROJECT_NAME + File.separator + RESULTS_DIR + File.separator;
	public static final String GEMS_DIR_PATH = HOME + File.separator + PROJECT_NAME + File.separator + GEMS_DIR + File.separator;
	
	public static final String GEMS_ANALYSIS_PATH =  HOME + File.separator + PROJECT_NAME + File.separator + GEMS_ANALYSIS_DIR + File.separator;
	public static final String GEMS_ANALYSIS_RESULTS_TEMPLATE = "GEMS_ANALYSIS_RESULTS.xlsx";
	public static final String GEMS_ANALYSIS_RESULTS_TEMPLATE_PATH = HOME + File.separator + PROJECT_NAME + File.separator + TEMPLATES_DIR + File.separator + GEMS_ANALYSIS_RESULTS_TEMPLATE;
	
	public static final String PERFORMANCE_ANALYSIS_FILE_NAME = "Performance Analysis.xlsx";
	public static final String PERFORMANCE_ANALYSIS_DIR_PATH = HOME + File.separator + PROJECT_NAME + File.separator + PERFORMANCE_DIR + File.separator;
}
