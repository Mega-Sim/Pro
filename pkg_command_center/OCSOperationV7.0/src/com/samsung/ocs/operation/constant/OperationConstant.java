package com.samsung.ocs.operation.constant;

/**
 * OperationConstant Interface, OCS 3.0 for Unified FAB
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

public interface OperationConstant {
	public static String AUTO = "Auto";
	public static String MANUAL = "Manual";
	
	// CommandState 包访
	public static String COMMAND_READY = "R";
	public static String COMMAND_SENT = "S";
	public static String COMMAND_RESPONDED = "N";
	public static String COMMAND_EXECUTING = "E";
	public static String COMMAND_EXECUTED = "D";
	public static String COMMAND_TIMEOUT = "T";
	public static String COMMAND_WAITFORRESPONSE = "t";
	public static String COMMAND_UNKNOWN = "F";
	
	// OperationMode 包访
	public static String IDLE = "I";
	public static String GO = "G";
	public static String WORK = "W";
	public static String SLEEP = "S";
	
	// ReplyMsg 包访
	public static char ACK = 'A';
	public static char BUSY = 'B';
	public static char DATA_LOGIC_ERROR = 'D';
	public static char ERROR = 'E';
	public static char PROTOCOL_ERROR = 'P';
	public static char TIMEOUT = 'T';
	
	public static String TSC_PAUSE_INITIATED = "TSCPauseInitiated";
	public static String TSC_PAUSE_COMPLETED = "TSCPauseCompleted";
	public static String TSC_AUTO_COMPLETED = "TSCAutoCompleted";
	
	public static String TRANSFER_INITIATED = "TransferInitiated";
	public static String TRANSFERRING = "Transferring";
	public static String TRANSFER_COMPLETED = "TransferCompleted";
	// 2012.08.08 by KYK : [TransferPaused]
	public static String TRANSFER_PAUSED = "TransferPaused";
	
	public static String TRANSFER_CANCELINITIATED = "TransferCancelInitiated";
	public static String TRANSFER_CANCELCOMPLETED = "TransferCancelCompleted";
	public static String TRANSFER_CANCELFAILED = "TransferCancelFailed";
	public static String TRANSFER_ABORTINITIATED = "TransferAbortInitiated";
	public static String TRANSFER_ABORTCOMPLETED = "TransferAbortCompleted";
	public static String TRANSFER_ABORTFAILED = "TransferAbortFailed";
	
	public static String TRANSFER_UPDATECOMPLETED = "TransferUpdateCompleted"; // 2021.01.21 by JJW
	public static String TRANSFER_UPDATEFAILED = "TransferUpdateFailed"; // 2021.01.21 by JJW
	
	public static String SCAN_INITIATED = "ScanInitiated";
	public static String SCANNING = "Scanning";
	public static String SCAN_COMPLETED = "ScanCompleted";
	
	public static String STAGE_INITIATED = "StageInitiated";
	public static String STAGING = "Staging";
	public static String STAGE_COMPLETED = "StageCompleted";
	
	public static String MAPMAKE_INITIATED = "MapmakeInitiated";
	public static String MAPMAKING = "Mapmaking";
	public static String MAPMAKE_COMPLETED = "MapmakeCompleted";
	
	public static String PATROL_INITIATED = "PatrolInitiated";
	public static String PATROLLING = "Patrolling";
	public static String PATROL_COMPLETED = "PatrolCompleted";
	
	public static String VEHICLE_ASSIGNED = "VehicleAssigned";
	public static String VEHICLE_ARRIVED = "VehicleArrived";
	public static String VEHICLE_ACQUIRESTARTED = "VehicleAcquireStarted";
	public static String VEHICLE_ACQUIRECOMPLETED = "VehicleAcquireCompleted";
	public static String VEHICLE_DEPARTED = "VehicleDeparted";
	public static String VEHICLE_DEPOSITSTARTED = "VehicleDepositStarted";
	public static String VEHICLE_DEPOSITCOMPLETED = "VehicleDepositCompleted";
	public static String VEHICLE_INSTALLED = "VehicleInstalled";
	public static String VEHICLE_REMOVED = "VehicleRemoved";
	public static String VEHICLE_UNASSIGNED = "VehicleUnassigned";
	
	public static String CARRIER_INSTALLED = "CarrierInstalled";
	public static String CARRIER_REMOVED = "CarrierRemoved";
	
	// Carrier State
	public static String INSTALL = "INSTALL";
	public static String INSTALLED = "INSTALLED";
	public static String REMOVE = "REMOVE";
	
	// RF Data 包访
	public static String RF_ERROR = "ERROR";
	public static String RF_EMPTY = "EMPTY";
	
	// VehicleErrorHistory Type 包访
	public static String VEHICLEERROR_ERROR = "ERROR";
	public static String VEHICLEERROR_RFREAD_ERROR = "RFREAD_ERROR";
	public static String VEHICLEERROR_AUTO_RETRY = "AUTO_RETRY";
	public static String VEHICLEERROR_AUTO_POSITION = "AUTO_POSITION";
	
	//
	public static final long GOMODE_NOTRESPONDING_LIMIT = 300000;
	
	public static enum OPERATION_MODE {IDLE, GO, WORK, SLEEP;
		/**
		 * Get OperationMode in String
		 * 
		 * @return
		 */
		public String toConstString() {
			switch(this) {
				case IDLE:
					return OperationConstant.IDLE;
				case GO:
					return OperationConstant.GO;
				case WORK:
					return OperationConstant.WORK;
				case SLEEP:
				default:
					return OperationConstant.SLEEP;						
			}
		};
	};
	
	// 'R':CommandReady, 'S':CommandSent, 'N': CommandResponsed,
	// 'E':CommandExecuting, 'D':CommandExecuted, 'T':CommandTimeout
	public static enum COMMAND_STATE {READY, SENT, RESPONDED, EXECUTING, EXECUTED, TIMEOUT, WAITFORRESPONSE, UNKNOWN;
		/**
		 * Get CommandState in String
		 * 
		 * @return
		 */
		public String toConstString() {
			switch(this) {
				case READY :
					return OperationConstant.COMMAND_READY;
				case SENT :
					return OperationConstant.COMMAND_SENT;
				case RESPONDED :
					return OperationConstant.COMMAND_RESPONDED;
				case EXECUTING:
					return OperationConstant.COMMAND_EXECUTING;
				case EXECUTED:
					return OperationConstant.COMMAND_EXECUTED;
				case TIMEOUT:
					return OperationConstant.COMMAND_TIMEOUT;
				case WAITFORRESPONSE:
					return OperationConstant.COMMAND_WAITFORRESPONSE;
				case UNKNOWN:
				default:
					return OperationConstant.COMMAND_UNKNOWN;
			}
		};
		
		/**
		 * Get CommandState in char
		 * 
		 * @return
		 */
		public char toConstChar() {
			switch(this) {
				case READY :
					return 'R';
				case SENT :
					return 'S';
				case RESPONDED :
					return 'N';
				case EXECUTING:
					return 'E';
				case EXECUTED:
					return 'D';
				case TIMEOUT:
					return 'T';
				case WAITFORRESPONSE:
					return 't';
				case UNKNOWN:
				default:
					return 'F';
			}
		};
	};
	
	public static enum JOB_TYPE {UNLOAD, LOAD};
	
	// 2020.05.11 by YSJ (OHT Auto Change)
	//   - add "VEHICLEAUTO"
	public static enum COMMAND_TYPE {GO, GOMORE, UNLOAD, LOAD, SCAN, MAPMAKE, PATROL, ESTOP, CANCEL, IDRESET, PAUSE, RESUME, ROUTEINFODATA, INTERSECTIONNODES, PATROLCANCEL, VEHICLEAUTO, UNKNOWN};
	
	
	//public static enum VEHICLE_STATE {INITIALIZED, GOING, ARRIVED, UNLOADING, UNLOADED, SCANNING, SCANNED, LOADING, LOADED, AUTO_POSITIONING, AUTO_RECOVERY};
	
	// 2012.01.27 by PMM
	public static enum AUTORECOVERY_BUT_NOTAVEXIST_TYPE {TRIED_TO_UNLOAD, UNLOADED, TRIED_TO_UNLOAD_BUT_TO_BE_ABORTED, TRIED_TO_LOAD, LOADED, TRIED_TO_LOAD_BUT_TO_BE_ABORTED, ABNORMAL, NONE};
}


