package com.samsung.ocs.operation.constant;

/**
 * MessageItem Interface, OCS 3.0 for Unified FAB
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

public interface MessageItem {
	// MessageName 관련
	public static String SEND_S6F11 = "SendS6F11";
	public static String SET_ALARM_REPORT = "SetAlarmReport";
	public static String CLEAR_ALARM_REPORT = "ClearAlarmReport";
	
	// MessageItem 관련
	public static String ALARM_ID = "ALID";
	public static String CARRIER_ID = "CarrierID";
	public static String CARRIER_LOC = "CarrierLoc";
	public static String COMMAND_ID = "CommandID";
	public static String DEST_PORT = "DestPort";
	public static String EVENT_TYPE = "EventType";
	public static String EVENT_NAME = "EventName";
	public static String PRIORITY = "Priority";
	public static String REPLACE = "Replace";
	public static String RESULT_CODE = "ResultCode";
	public static String SOURCE_PORT = "SourcePort";
	public static String TRANSFER_PORT = "TransferPort";
	public static String VEHICLE_ID = "VehicleID";
	public static String VEHICLE_STATE = "VehicleState";	
	public static String VEHICLE_TYPE = "VehicleType";
	public static String CARRIER_TYPE = "CarrierType"; // 2013.08.26 by KYK
	public static String VEHICLE_CURR_DOMAIN = "VehicleCurrentDomain";
	public static String VEHICLE_CURR_POSITION = "VehicleCurrentPosition";
	public static String FOUPID = "FoupID"; // 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
	
	public static enum EVENT_TYPE {TRCMD, VEHICLE, CARRIER, TSC;
		// StateType 관련
		private static String TRCMD_STRING = "TrCmd";
		private static String VEHICLE_STRING = "Vehicle";
		private static String CARRIER_STRING = "Carrier";
		private static String TSC_STRING = "TSC";
		/**
		 * Get Event Type in String
		 * 
		 * @return
		 */
		public String toConstString() {
			switch(this) {
				case VEHICLE :
					return VEHICLE_STRING;
				case CARRIER :
					return CARRIER_STRING;
				case TSC :
					return TSC_STRING;
				case TRCMD :
				default:
					return TRCMD_STRING;
			}
		};
	};
}