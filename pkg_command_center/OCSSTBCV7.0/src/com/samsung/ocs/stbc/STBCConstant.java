package com.samsung.ocs.stbc;

/**
 * STBCConstant Interface, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2011. 6. 21.
 * @version 3.0
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public interface STBCConstant {

	// RFC Verify Result
	public static int OK = 0;
	public static int CARRIER_DETECT_MISMATCH = 1;
	public static int DISABLED_STB = 9;
	public static int NOT_USE_STB = 3;
	// RFC Read Result
	public static int IDREAD_ERROR = 1;
	public static int NOT_USE_RFC = 2;
	public static int IDREAD_NULL_ERROR = 4;
	// RFC Carrier Detect
	public static int NOT_DETECT_CARRIER = 0;
	public static int DETECT_CARRIER = 1;
	// IDRead Status
	public static int SUCCESS = 0; 
	public static int FAILURE = 1; 
	public static int DUPLICATE = 2; 
	public static int MISMATCH = 3; 
	public static int NO_LOAD = 4;

	public static String HOMEDIR = "user.dir";
	public static String FILESEPARATOR = "file.separator";
	public static String STBC_NONE = "STBC_NONE";
	public static String STBC_INIT = "STBC_INIT";
	public static String STBC_START = "STBC_START";
	public static String STBC_STOP = "STBC_STOP";

	public static String STBCUSAGE = "STBCUSAGE"; // 2013.01.04 by KYK
	public static String STBC = "STBC";
	public static String OCS = "OCS";
	public static String MCS = "MCS"; // 2013.01.04 by KYK
	public static String INSTALLED = "INSTALLED";
	public static String CarrierState = "CarrierState";
	public static String CarrierIDRead = "CarrierIDRead";
	public static String CarrierIDReadMulti = "CarrierIDReadMulti";	
	public static String CarrierInstalled = "CarrierInstalled";
	public static String CarrierRemoved = "CarrierRemoved";
	public static String CarrierDataListInstalled = "CarrierDataListInstalled";
	public static String STBAutoCompleted = "STBAutoCompleted";
	public static String STBAutoInitiated = "STBAutoInitiated";
	public static String STBPauseCompleted = "STBPauseCompleted";
	public static String STBPauseInitiated = "STBPauseInitiated";
	public static String STBPaused = "STBPaused";
	public static String PortInService = "PortInService";
	public static String PortOutOfService = "PortOutOfService";		
}
