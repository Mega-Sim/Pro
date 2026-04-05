package com.samsung.ocs.ziplogs.constant;

/**
 * CommonConfig Class, OCS 3.0 for Unified FAB
 * 
 * @author Byoungsoo.Kim
 * 
 * @date 2014. 7. 01.
 * @version 3.0
 * 
 * Copyright 2014 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information of
 * Samsung Electronics, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Samsung.
 */

public interface ZipLogsConstant {

	public static String ZIPLOGS = "ZipLogs";

	public static final int COMPRESSION_LEVEL = 8;
	public static final int BUFFER_SIZE = 1024 * 2;
	public static final int GIGA_SIZE = 1024 * 1024 * 1024;
	public static final int MSEC_OF_HOUR = 60 * 60 * 1000;
	public static final int MSEC_OF_MINUTE = 60 * 1000;

	// config
	public static String OCSMODULE = "OCSMODULE";
	public static String PATH = "PATH";
	public static String RUNTIME = "RUNTIME";
	public static String TIME = "TIME";
	public static String SLEEPTIME = "SLEEPTIME";
	public static String ZIPTIME = "ZIPTIME";
	public static String DELTIME = "DELTIME";
	public static String ZIPLIMIT = "ZIPLIMIT";

	// log
	public static String ZIPLOGS_MAIN = "ZipLogsMain";
	public static String ZIPLOGS_EXCEPTION = "ZipLogsException";
}
