package com.samsung.ocs.common.constant;

/**
 * EventHistoryConstant Interface, OCS 3.0 for Unified FAB
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

public interface EventHistoryConstant {
	public static String NULL = "";
	
	// Event Name
	public static String CURRENT_JOB_CANCEL = "CURRENT JOB CANCEL";
	public static String CURRENT_STAGE_CANCEL = "CURRENT STAGE CANCEL";
	public static String CURRENT_STAGE_CHANGE = "CURRENT STAGE CHANGE";
	public static String CURRENT_STAGE_DELETE = "CURRENT STAGE DELETE";
	public static String CURRENT_VIBRATION_DELETE = "CURRENT VIBRATION DELETE";
	public static String NEXT_JOB_CANCEL = "NEXT JOB CANCEL";
	public static String PORT_DUPLICATE_CLEAR = "PORT DUPLICATE CLEAR";
	public static String UNLOAD_CARRIER_MISMATCH = "UNLOAD CARRIER MISMATCH";
	public static String MISSED_CARRIER = "MISSED CARRIER";
	public static String OPERATION_PROCESS_TIMEOVER = "OPERATION PROCESS TIMEOVER";
	public static String AUTO_ZONE_MOVE = "AUTO ZONE MOVE";
	public static String MANUAL_ZONE_MOVE = "MANUAL ZONE MOVE";
	public static String RUNTIME_UPDATE = "RUNTIME UPDATE";
	public static String RUNTIME_PORT_UPDATE = "RUNTIME_PORT_UPDATE";
	// 2013.01.04 by KYK
	public static String ERROR_STB = "ERROR_STB";
	public static String ERROR_STB_DUP = "ERROR_STB_DUP";
	public static String UNUSED_STBPORT = "UNUSED_STBPORT";
	public static String UNUSED_RFC = "UNUSED_RFC";
	
	// Event Type
	public static String SYSTEM = "SYSTEM";
	
	// RemoteId
	public static String OPERATION = "Operation";
	public static String JOBASSIGN = "JobAssign";
	public static String IBSEM = "IBSEM";
	public static String STBC = "STBC";
	public static String OPTIMIZER = "Optimizer";
	public static String LONGRUN = "LongRun";
	
	// Reason
	public static String CURRENT_JOB_ABORTED = "CURRENT JOB ABORTED";
	// 2018.08.31 by LSH
	public static String BEFORE_JOB_IN_PROGRESS = "BEFORE JOB IN PROGRESS";
	public static String PORT_DUPLICATE = "PORT DUPLICATE";
	public static String VEHICLE_AUTO_RECOVERY = "VEHICLE AUTO RECOVERY";
	public static String VEHICLE_MANUAL = "VEHICLE MANUAL";
	public static String VEHICLE_MANUAL_ERROR = "VEHICLE MANUAL ERROR";
	public static String VEHICLE_COMMFAIL = "VEHICLE COMMFAIL";
	public static String VEHICLE_NOT_RESPONDING = "VEHICLE NOT RESPONDING";
	public static String VEHICLE_REMOVE = "VEHICLE REMOVE";
	public static String VEHICLE_NOT_ASSIGNHOLD = "VEHICLE NOT ASSIGNHOLD";
	public static String ABNORMAL_VEHICLE = "ABNORMAL VEHICLE";
	public static String HIDDOWN_AT_SOURCENODE = "HIDDOWN AT SOURCENODE";
	public static String HIDDOWN_AT_DESTNODE = "HIDDOWN AT DESTNODE";
	public static String UNLOAD_PATHSEARCH_FAIL = "UNLOAD PATHSEARCH FAIL";
	public static String LOAD_PATHSEARCH_FAIL = "LOAD PATHSEARCH FAIL";
	public static String MAPMAKE_PATHSEARCH_FAIL = "MAPMAKE PATHSEARCH FAIL";
	public static String PATROL_PATHSEARCH_FAIL = "PATROL PATHSEARCH FAIL";
	public static String STAGECHANGE = "STAGECHANGE";
	public static String STAGEDELETE = "STAGEDELETE";
	public static String EXPECTEDDURATION_IS_ZERO = "EXPECTEDDURATION IS ZERO";
	public static String EXPECTEDDURATION_TIMEOVER = "EXPECTEDDURATION TIMEOVER";
	public static String WAITTIMEOUT_TIMEOVER = "WAITTIMEOUT TIMEOVER";
	public static String UNKNOWN_TRCMD_REGISTERED = "UNKNOWN_TRCMD_REGISTERED";
	// 2013.01.04 by KYK
	public static String CARRIER_DUPLICATE_BY_VERIFY = "CARRIER_DUPLICATE_BY_VERIFY";
	public static String CARRIER_DUPLICATE_BY_IDREAD = "CARRIER_DUPLICATE_BY_IDREAD";
	public static String ERRORSTB_CARRIER_REMOVED = "ERRORSTB_CARRIER_REMOVED";
	public static String IDREAD_TIMEOUT = "IDREAD_TIMEOUT";
	public static String RFC_ABNORMAL = "RFC_ABNORMAL";
	public static String UNLOAD_VERIFY_TIMEOUT = "UNLOAD_VERIFY_TIMEOUT";
	public static String LOAD_VERIFY_TIMEOUT = "LOAD_VERIFY_TIMEOUT";
	public static String UNLOAD_VERIFY_NG = "UNLOAD_VERIFY_NG";
	public static String LOAD_VERIFY_NG = "LOAD_VERIFY_NG";
	// 2022.03.14 dahye : Premove Logic Improve
	public static String DELIVERYWAITTIMEOUT_TIMEOVER = "DELIVERYWAITTIMEOUT_TIMEOVER";
	// 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
	public static String SOURCE_DUPLICATE_BY_STAGE = "SOURCE_DUPLICATE_BY_STAGE";
	
	
	public static enum EVENTHISTORY_NAME {CURRENT_JOB_CANCEL, NEXT_JOB_CANCEL,
		CURRENT_STAGE_CANCEL, CURRENT_STAGE_CHANGE, CURRENT_STAGE_DELETE, CURRENT_VIBRATION_DELETE,
		PORT_DUPLICATE, PORT_DUPLICATE_CLEAR, UNLOAD_CARRIER_MISMATCH, MISSED_CARRIER,
		OPERATION_PROCESS_TIMEOVER, AUTO_ZONE_MOVE, MANUAL_ZONE_MOVE,
		RUNTIME_UPDATE,
		ERROR_STB, ERROR_STB_DUP, UNUSED_STBPORT, UNUSED_RFC,	// 2013.01.04 by KYK
		RUNTIME_PORT_UPDATE,	// 2017.10.24 by LSH
		NULL;
		
		/**
		 * 
		 * @return eventHistoryName String
		 */
		public String toConstString() {
			switch(this) {
				case CURRENT_JOB_CANCEL:
					return EventHistoryConstant.CURRENT_JOB_CANCEL;
				case NEXT_JOB_CANCEL :
					return EventHistoryConstant.NEXT_JOB_CANCEL;
				case CURRENT_STAGE_CANCEL:
					return EventHistoryConstant.CURRENT_STAGE_CANCEL;
				case CURRENT_STAGE_CHANGE:
					return EventHistoryConstant.CURRENT_STAGE_CHANGE;
				case CURRENT_STAGE_DELETE:
					return EventHistoryConstant.CURRENT_STAGE_DELETE;
				case CURRENT_VIBRATION_DELETE:
					return EventHistoryConstant.CURRENT_VIBRATION_DELETE;
				case PORT_DUPLICATE:
					return EventHistoryConstant.PORT_DUPLICATE;
				case PORT_DUPLICATE_CLEAR:
					return EventHistoryConstant.PORT_DUPLICATE_CLEAR;
				case UNLOAD_CARRIER_MISMATCH :
					return EventHistoryConstant.UNLOAD_CARRIER_MISMATCH;
				case MISSED_CARRIER :
					return EventHistoryConstant.MISSED_CARRIER;
				case OPERATION_PROCESS_TIMEOVER :
					return EventHistoryConstant.OPERATION_PROCESS_TIMEOVER;
				case AUTO_ZONE_MOVE :
					return EventHistoryConstant.AUTO_ZONE_MOVE;
				case MANUAL_ZONE_MOVE:
					return EventHistoryConstant.MANUAL_ZONE_MOVE;
				case RUNTIME_UPDATE:
					return EventHistoryConstant.RUNTIME_UPDATE;
				case RUNTIME_PORT_UPDATE:
					return EventHistoryConstant.RUNTIME_PORT_UPDATE;
				case ERROR_STB: 
					return EventHistoryConstant.ERROR_STB;
				case ERROR_STB_DUP:
					return EventHistoryConstant.ERROR_STB_DUP;
				case UNUSED_STBPORT:
					return EventHistoryConstant.UNUSED_STBPORT;
				case UNUSED_RFC:
					return EventHistoryConstant.UNUSED_RFC;
				case NULL:
				default:
					return EventHistoryConstant.NULL;
			}
		};
	};
	
	public static enum EVENTHISTORY_TYPE {SYSTEM, NULL;
		/**
		 * 
		 * @return eventHistoryType String
		 */
		public String toConstString() {
			switch(this) {
				case SYSTEM:
					return EventHistoryConstant.SYSTEM;
				case NULL:
				default:
					return EventHistoryConstant.NULL;
			}
		};
	};
	
	public static enum EVENTHISTORY_REMOTEID {OPERATION, JOBASSIGN, IBSEM, STBC, OPTIMIZER, LONGRUN, NULL;
		/**
		 * 
		 * @return eventHistoryRemoteId String
		 */
		public String toConstString() {
			switch(this) {
				case OPERATION:
					return EventHistoryConstant.OPERATION;
				case JOBASSIGN:
					return EventHistoryConstant.JOBASSIGN;
				case IBSEM:
					return EventHistoryConstant.IBSEM;
				case STBC:
					return EventHistoryConstant.STBC;
				case OPTIMIZER:
					return EventHistoryConstant.OPTIMIZER;
				case LONGRUN:
					return EventHistoryConstant.LONGRUN;
				case NULL:
				default:
					return EventHistoryConstant.NULL;
			}
		};
	};
	
	public static enum EVENTHISTORY_REASON {CURRENT_JOB_ABORTED, BEFORE_JOB_IN_PROGRESS, PORT_DUPLICATE, 
		VEHICLE_AUTO_RECOVERY, VEHICLE_MANUAL, VEHICLE_MANUAL_ERROR, VEHICLE_COMMFAIL, VEHICLE_NOT_RESPONDING, VEHICLE_REMOVE, VEHICLE_NOT_ASSIGNHOLD,
		UNLOAD_PATHSEARCH_FAIL, LOAD_PATHSEARCH_FAIL, MAPMAKE_PATHSEARCH_FAIL, PATROL_PATHSEARCH_FAIL,
		ABNORMAL_VEHICLE, HIDDOWN_AT_SOURCENODE, HIDDOWN_AT_DESTNODE,
		STAGECHANGE, STAGEDELETE,
		EXPECTEDDURATION_IS_ZERO,
		EXPECTEDDURATION_TIMEOVER, WAITTIMEOUT_TIMEOVER, UNKNOWN_TRCMD_REGISTERED,
		RUNTIME_UPDATE,
		// 2013.01.04 by KYK
		CARRIER_DUPLICATE_BY_VERIFY, CARRIER_DUPLICATE_BY_IDREAD, ERRORSTB_CARRIER_REMOVED, RFC_ABNORMAL, 
		UNLOAD_VERIFY_NG, LOAD_VERIFY_NG, UNLOAD_VERIFY_TIMEOUT, LOAD_VERIFY_TIMEOUT, IDREAD_TIMEOUT, 
		// 2017.10.24 by LSH
		RUNTIME_PORT_UPDATE,
		// 2022.03.14 dahye : Prmove Logic Improve
		DELIVERYWAITTIMEOUT_TIMEOVER,
		// 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
		SOURCE_DUPLICATE_BY_STAGE,
		NULL;
	
		/**
		 * 
		 * @return eventHistoryReason String
		 */
		public String toConstString() {
			switch (this) {
				case CURRENT_JOB_ABORTED:
					return EventHistoryConstant.CURRENT_JOB_ABORTED;
				case BEFORE_JOB_IN_PROGRESS:
					return EventHistoryConstant.BEFORE_JOB_IN_PROGRESS;
				case PORT_DUPLICATE:
					return EventHistoryConstant.PORT_DUPLICATE;
				case VEHICLE_AUTO_RECOVERY:
					return EventHistoryConstant.VEHICLE_AUTO_RECOVERY;
				case VEHICLE_MANUAL:
					return EventHistoryConstant.VEHICLE_MANUAL;
				case VEHICLE_MANUAL_ERROR :
					return EventHistoryConstant.VEHICLE_MANUAL_ERROR;
				case VEHICLE_COMMFAIL:
					return EventHistoryConstant.VEHICLE_COMMFAIL;
				case VEHICLE_NOT_RESPONDING:
					return EventHistoryConstant.VEHICLE_NOT_RESPONDING;
				case VEHICLE_REMOVE :
					return EventHistoryConstant.VEHICLE_REMOVE;
				case VEHICLE_NOT_ASSIGNHOLD :
					return EventHistoryConstant.VEHICLE_NOT_ASSIGNHOLD;
				case ABNORMAL_VEHICLE :
					return EventHistoryConstant.ABNORMAL_VEHICLE;
				case HIDDOWN_AT_SOURCENODE :
					return EventHistoryConstant.HIDDOWN_AT_SOURCENODE;
				case HIDDOWN_AT_DESTNODE :
					return EventHistoryConstant.HIDDOWN_AT_DESTNODE;
				case UNLOAD_PATHSEARCH_FAIL :
					return EventHistoryConstant.UNLOAD_PATHSEARCH_FAIL;
				case LOAD_PATHSEARCH_FAIL :
					return EventHistoryConstant.LOAD_PATHSEARCH_FAIL;
				case MAPMAKE_PATHSEARCH_FAIL :
					return EventHistoryConstant.MAPMAKE_PATHSEARCH_FAIL;
				case PATROL_PATHSEARCH_FAIL :
					return EventHistoryConstant.PATROL_PATHSEARCH_FAIL;
				case STAGECHANGE:
					return EventHistoryConstant.STAGECHANGE;
				case STAGEDELETE:
					return EventHistoryConstant.STAGEDELETE;
				case EXPECTEDDURATION_IS_ZERO:
					return EventHistoryConstant.EXPECTEDDURATION_IS_ZERO;
				case EXPECTEDDURATION_TIMEOVER:
					return EventHistoryConstant.EXPECTEDDURATION_TIMEOVER;
				case WAITTIMEOUT_TIMEOVER:
					return EventHistoryConstant.WAITTIMEOUT_TIMEOVER;
				case UNKNOWN_TRCMD_REGISTERED:
					return EventHistoryConstant.UNKNOWN_TRCMD_REGISTERED;
				case RUNTIME_UPDATE:
					return EventHistoryConstant.RUNTIME_UPDATE;
				case RUNTIME_PORT_UPDATE:
					return EventHistoryConstant.RUNTIME_PORT_UPDATE;
				case CARRIER_DUPLICATE_BY_VERIFY: 
					return EventHistoryConstant.CARRIER_DUPLICATE_BY_VERIFY;
				case CARRIER_DUPLICATE_BY_IDREAD: 
					return EventHistoryConstant.CARRIER_DUPLICATE_BY_IDREAD;
				case ERRORSTB_CARRIER_REMOVED:
					return EventHistoryConstant.ERRORSTB_CARRIER_REMOVED;
				case RFC_ABNORMAL: 
					return EventHistoryConstant.RFC_ABNORMAL;
				case UNLOAD_VERIFY_NG: 
					return EventHistoryConstant.UNLOAD_VERIFY_NG;
				case LOAD_VERIFY_NG: 
					return EventHistoryConstant.LOAD_VERIFY_NG;
				case UNLOAD_VERIFY_TIMEOUT: 
					return EventHistoryConstant.UNLOAD_VERIFY_TIMEOUT;
				case LOAD_VERIFY_TIMEOUT: 
					return EventHistoryConstant.LOAD_VERIFY_TIMEOUT;
				case IDREAD_TIMEOUT:
					return EventHistoryConstant.IDREAD_TIMEOUT;
				case DELIVERYWAITTIMEOUT_TIMEOVER:
					return EventHistoryConstant.DELIVERYWAITTIMEOUT_TIMEOVER;
				case SOURCE_DUPLICATE_BY_STAGE:
					return EventHistoryConstant.SOURCE_DUPLICATE_BY_STAGE;
				case NULL:
				default:
					return EventHistoryConstant.NULL;
			}
		};
	};
}
