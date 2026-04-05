package com.samsung.ocs.common.constant;


/**
 * OcsAlarmConstant Interface, OCS 3.0 for Unified FAB
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

public interface OcsAlarmConstant {
	public static int NO_ALARM = 0;
	
//	public static int SEARCH_FAIL_BY_UNLOAD_PATH = 3301;			// UNLOAD는 알람 등록하지 않음.
	public static int SEARCH_FAIL_BY_LOAD_PATH = 3004;
	public static int SEARCH_FAIL_BY_MOVE_PATH = 3005;
	public static int SEARCH_FAIL_BY_YIELD_PATH = 3006;
	
	// 2015.02.10 by KYK : alarmCode 변경 why? operation 은 이미 장애로 작업할당해제 처리 -> jobassign도 장애호기로 반영 필요
//	public static int NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER = 3101;
	
	public static int CARRIER_STATUS_ERROR_NOTRCMD = 3201;
	public static int DELAYED_DESTCHANGE = 3202;
	
	public static int WARNING_LEVEL_TEMPERATURE = 3500;
	
	public static int OPERATION_PROCESS_TIMEOUT = 4001;
	
	// 2011.10.29 by PMM
	// 2014.10.20 by zzang9un : user가 삭제해야 하는 알람 코드(단, 장애호기은 아님)
	public static int CARRIER_REMAINEDON_REMOVEDVHL = 4200;
	public static int UNLOAD_CARRIER_MISMATCH = 4201;
	
	// AlarmCode > 5000 이면 장애 OHT로 처리.
	public static int NOTRESPONDING_UNLOADCOMMAND_TIMEOVER = 5001;
	public static int NOTRESPONDING_LOADCOMMAND_TIMEOVER = 5002;
	public static int NOTRESPONDING_GOCOMMAND_TIMEOVER = 5003;
	public static int NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS = 5004; // 2014.07.15 by KYK
	public static int NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER = 5005; // 2015.02.10 by KYK

	public static int ESTOP_BY_VEHICLE_INIT_FAIL = 5101;
	public static int ESTOP_BY_VEHICLE_DRIVE_FAIL = 5102;
	public static int ESTOP_BY_VEHICLE_COLLISION = 5103;
	
	public static int ESTOP_BY_UNLOAD_CARRIER_MISMATCH = 5104;
	public static int ESTOP_BY_VEHICLE_ON_PROHIBITED_ZONE = 5105;
	public static int ESTOP_BY_CARRIER_STATUS_ERROR_UNLOAD = 5106;
	public static int ESTOP_BY_CARRIER_STATUS_ERROR_LOAD = 5107;

	// 2014.08.13 by MYM : Abnormal CmdReply 확인
	public static int RECEIVED_CMDREPLY_DATALOGIC = 5301;
	public static int RECEIVED_CMDREPLY_PROTOCOL = 5302;
	public static int RECEIVED_CMDREPLY_PAUSE = 5303;
	
	// 2013.05.03 by KYK
	public static int ESTOP_BY_NODE_STATION_MISMATCH = 5501;
	// 2013.09.06 by KYK
	public static int ESTOP_BY_CARRIER_TYPE_MISMATCH = 5502;

	public static int UNKNOWN = 9999;
	
	// AlarmLevel 관련
	public static String ALARMLEVEL_ERROR = "E";
	public static String ALARMLEVEL_WARNING = "W";
	public static String ALARMLEVEL_INFORMATION = "I";
	
	public static enum ALARMLEVEL {ERROR, WARNING, INFORMATION;
		/**
		 * Get ALARMLEVEL in String
		 * 
		 * @return
		 */
		public String toConstString() {
			switch(this) {
				case WARNING:
					return OcsAlarmConstant.ALARMLEVEL_WARNING;
				case INFORMATION:
					return OcsAlarmConstant.ALARMLEVEL_INFORMATION;
				case ERROR:
				default:
					return OcsAlarmConstant.ALARMLEVEL_ERROR;						
			}
		};
	};
	
	
}
