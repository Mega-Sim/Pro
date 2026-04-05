package com.samsung.ocs.common.constant;

/**
 * TrCmdConstant Interface, OCS 3.0 for Unified FAB
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

public interface TrCmdConstant {
	public static String NULL = "";
	public static int NULL_HASHCODE = 0;
	
	// TrCmd RemoteCmd, Vehicle RequestedType 관련
	public static String TRANSFER = "TRANSFER";
	public static String PREMOVE = "PREMOVE";	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	public static String STAGE = "STAGE";
	public static String STAGEWAIT = "STAGEWAIT";
	public static String STAGENOBLOCK = "STAGENOBLOCK";
	public static String STAGECANCEL = "STAGECANCEL";
	public static String SCAN = "SCAN";
	public static String MAPMAKE = "MAPMAKE";
	public static String PATROL = "PATROL";
	public static String VIBRATION = "VIBRATION";
	public static String VIBRATIONCHANGE = "VIBRATIONCHANGE";
	public static String CANCEL = "CANCEL";
	public static String ABORT = "ABORT";
	public static String REMOVE = "REMOVE";
	public static String PAUSE = "PAUSE";
	public static String RESUME = "RESUME";
	public static String TRANSFERUPDATE = "TRANSFERUPDATE"; // 2021.01.21 by JJW
	// 2020.05.11 by YSJ (OHT Auto Change)/
	public static String VEHICLEAUTO = "VEHICLEAUTO";
	public static String VEHICLEAUTO_RESET = "VEHICLEAUTO_RESET";

	// Vehicle RequestedType 관련
	public static String STAGECHANGE = "STAGECHANGE";
	public static String STAGEDELETE = "STAGEDELETE";
	public static String UNASSIGN = "UNASSIGN";
	public static String DESTCHANGE = "DESTCHANGE";
	public static String MOVE = "MOVE";
	public static String PMOVE = "PMOVE";
	public static String ZONEMOVE = "ZONEMOVE";
	public static String YIELD = "YIELD";
	public static String RESET = "RESET";
	public static String LOCATE = "LOCATE";
	public static String PLOCATE = "PLOCATE";
	public static String LOCATECANCEL = "LOCATECANCEL";
	public static String TRANSFERCHANGE = "TRANSFERCHANGE"; // 2021.01.21 by JJW
	public static String PLOCATECANCEL = "PLOCATECANCEL";
//	public static String PARK = "PARK";
//	public static String PARKCANCEL = "PARKCANCEL";
	public static String TRANSFER_RESET = "TRANSFER_RESET";
	public static String STAGE_RESET = "STAGE_RESET";
	public static String STAGECHANGE_RESET = "STAGECHANGE_RESET";
	public static String SCAN_RESET = "SCAN_RESET";
	public static String MAPMAKE_RESET = "MAPMAKE_RESET";
	public static String PATROL_RESET = "PATROL_RESET";
	public static String VIBRATION_RESET = "VIBRATION_RESET";
	public static String UNASSIGN_RESET = "UNASSIGN_RESET";
	public static String DESTCHANGE_RESET = "DESTCHANGE_RESET";
	public static String MOVE_RESET = "MOVE_RESET";
	public static String PMOVE_RESET = "PMOVE_RESET";
	public static String ZONEMOVE_RESET = "ZONEMOVE_RESET";
	public static String YIELD_RESET = "YIELD_RESET";
	public static String LOCATE_RESET = "LOCATE_RESET";
	public static String PLOCATE_RESET = "PLOCATE_RESET";
//	public static String PARK_RESET = "PARK_RESET";
	
	public static int TRANSFER_HASHCODE = 2063509483;
	public static int STAGE_HASHCODE = 79219422;
	public static int STAGECANCEL_HASHCODE = -1691496744;
	public static int SCAN_HASHCODE = 2539133;
	public static int MAPMAKE_HASHCODE = 1555441962;
	public static int PATROL_HASHCODE = -1942022580;
	public static int VIBRATION_HASHCODE = -1590230414;
	public static int VIBRATIONCHANGE_HASHCODE = 1834871970;
	public static int CANCEL_HASHCODE = 1980572282;
	public static int ABORT_HASHCODE = 62073616;
	public static int REMOVE_HASHCODE = -1881281404;
	public static int PAUSE_HASHCODE = 75902422;
	public static int RESUME_HASHCODE = -1881097171;
	
	public static int STAGECHANGE_HASHCODE = -1685408754;
	public static int STAGEDELETE_HASHCODE = -1659230711;
	public static int UNASSIGN_HASHCODE = 260925640;
	public static int DESTCHANGE_HASHCODE = 1508572370;
	public static int MOVE_HASHCODE = 2372561;
	public static int PMOVE_HASHCODE = 76254241;
	public static int ZONEMOVE_HASHCODE = 1631623549;
	public static int YIELD_HASHCODE = 84436845;
	public static int RESET_HASHCODE = 77866287;
	public static int LOCATE_HASHCODE = -2044132526;
	public static int PLOCATE_HASHCODE = 236685218;
	public static int LOCATECANCEL_HASHCODE = -1645813684;
	public static int TRANSFERUPDATE_HASHCODE = 437319796;
	public static int PLOCATECANCEL_HASHCODE = 651749532;
//	public static int PARK_HASHCODE = 2448362;
//	public static int PARKCANCEL_HASHCODE = 938764004;
	public static int TRANSFER_RESET_HASHCODE = 725504987;
	public static int STAGE_RESET_HASHCODE = -874433394;
	public static int STAGECHANGE_RESET_HASHCODE = -1190430274;
	public static int SCAN_RESET_HASHCODE = 650884333;
	public static int MAPMAKE_RESET_HASHCODE = -1338398246;
	public static int PATROL_RESET_HASHCODE = 1160820092;
	public static int VIBRATION_RESET_HASHCODE = -1649119966;
	public static int UNASSIGN_RESET_HASHCODE = -193053960;
	public static int DESTCHANGE_RESET_HASHCODE = 1885020034;
	public static int MOVE_RESET_HASHCODE = 162061121;
	public static int PMOVE_RESET_HASHCODE = -1860126831;
	public static int ZONEMOVE_RESET_HASHCODE = -2049617939;
	public static int YIELD_RESET_HASHCODE = -307828259;
	public static int LOCATE_RESET_HASHCODE = -828750334;
	public static int PLOCATE_RESET_HASHCODE = 1468812882;
	public static int STAGEWAIT_HASHCODE = 329561427;
	public static int STAGENOBLOCK_HASHCODE = 665817774;
	public static int TRANSFERUPDATE_RESET_HASHCODE = 437319796;  // 2021.01.21 by JJW
	public static int PREMOVE_HASHCODE = 399536340;	// 2021.04.02 by JDH : Transfer Premove 사양 추가
//	public static int PARK_RESET_HASHCODE = 1755827354;
	// 2020.05.11 by YSJ (OHT Auto Change)
	public static int VEHICLEAUTO_HASHCODE = -1930327365;
	public static int VEHICLEAUTO_RESET_HASHCODE = -1156588885;

	// Command State 관련
	public static String CMD_QUEUED = "CMD_QUEUED";
	public static String CMD_WAITING = "CMD_WAITING";
	public static String CMD_TRANSFERRING = "CMD_TRANSFERRING";
	public static String CMD_COMPLETED = "CMD_COMPLETED";
	public static String CMD_STAGING = "CMD_STAGING";
	public static String CMD_SCANNING = "CMD_SCANNING";
	public static String CMD_MAPMAKING = "CMD_MAPMAKING";
	public static String CMD_PATROLLING = "CMD_PATROLLING";
	public static String CMD_MONITORING = "CMD_MONITORING";
	public static String CMD_PAUSED = "CMD_PAUSED";
	public static String CMD_CANCELLING = "CMD_CANCELLING";
	public static String CMD_CANCELED = "CMD_CANCELED";
	public static String CMD_CANCELFAILED = "CMD_CANCELFAILED";
	public static String CMD_ABORTING = "CMD_ABORTING";
	public static String CMD_ABORTED = "CMD_ABORTED";
	public static String CMD_ABORTFAILED = "CMD_ABORTFAILED";
	public static String CMD_PREMOVE = "CMD_PREMOVE";	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	
	public static int CMD_QUEUED_HASHCODE = 1343546424;
	public static int CMD_WAITING_HASHCODE = -838593528;
	public static int CMD_TRANSFERRING_HASHCODE = -1675748448;
	public static int CMD_COMPLETED_HASHCODE = 1127411014;
	public static int CMD_STAGING_HASHCODE = 442537462;
	public static int CMD_SCANNING_HASHCODE = -1362292170;
	public static int CMD_MAPMAKING_HASHCODE = -125139326;
	public static int CMD_PATROLLING_HASHCODE = -1248217977;
	public static int CMD_MONITORING_HASHCODE = -1311667059;
	public static int CMD_PAUSED_HASHCODE = 1296921587;
	public static int CMD_CANCELLING_HASHCODE = -1274414667;
	public static int CMD_CANCELED_HASHCODE = -595739106;
	public static int CMD_CANCELFAILED_HASHCODE = -826120644;
	public static int CMD_PREMOVE_HASHCODE = 2021614255;	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	public static int CMD_ABORTING_HASHCODE = 1144037431;
	public static int CMD_ABORTED_HASHCODE = 1145282954;
	public static int CMD_ABORTFAILED_HASHCODE = 1355854760;

	// Command DetailState 관련
	public static String NOT_ASSIGNED = "NOT_ASSIGNED";
	public static String UNLOAD_ASSIGNED = "UNLOAD_ASSIGNED";
	public static String UNLOAD_SENT = "UNLOAD_SENT";
	public static String UNLOAD_ACCEPTED = "UNLOAD_ACCEPTED";
	public static String UNLOADING = "UNLOADING";
	public static String UNLOADED = "UNLOADED";
	public static String LOAD_ASSIGNED = "LOAD_ASSIGNED";
	public static String LOAD_SENT = "LOAD_SENT";
	public static String LOAD_ACCEPTED = "LOAD_ACCEPTED";
	public static String LOADING = "LOADING";
	public static String LOADED = "LOADED";
	public static String SCAN_ASSIGNED = "SCAN_ASSIGNED";
	public static String SCAN_SENT = "SCAN_SENT";
	public static String SCAN_ACCEPTED = "SCAN_ACCEPTED";
	public static String SCANNING = "SCANNING";
	public static String SCANNED = "SCANNED";
	public static String STAGE_ASSIGNED = "STAGE_ASSIGNED";
	public static String STAGE_NOBLOCKING = "STAGE_NOBLOCKING";
	public static String STAGE_WAITING = "STAGE_WAITING";
	public static String MAPMAKE_ASSIGNED = "MAPMAKE_ASSIGNED";
	public static String MAPMAKE_SENT = "MAPMAKE_SENT";
	public static String MAPMAKE_ACCEPTED = "MAPMAKE_ACCEPTED";
	public static String MAPMAKING = "MAPMAKING";
	public static String MAPMADE = "MAPMADE";
	public static String PATROL_ASSIGNED = "PATROL_ASSIGNED";
	public static String PATROL_SENT = "PATROL_SENT";
	public static String PATROL_ACCEPTED = "PATROL_ACCEPTED";
	public static String PATROLLING = "PATROLLING";
	public static String PATROLLED = "PATROLLED";
	public static String PATROL_CANCELED = "PATROL_CANCELED";
	public static String VIBRATION_MONITORING = "VIBRATION_MONITORING";
	public static String LOAD_WAITING = "LOAD_WAITING";	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	
	public static int NOT_ASSIGNED_HASHCODE = -1865688614;
	public static int UNLOAD_ASSIGNED_HASHCODE = -1514648818;
	public static int UNLOAD_SENT_HASHCODE = -654461736;
	public static int UNLOAD_ACCEPTED_HASHCODE = 1003674855;
	public static int UNLOADING_HASHCODE = 540079203;
	public static int UNLOADED_HASHCODE = 571611102;
	public static int LOAD_ASSIGNED_HASHCODE = -28666617;
	public static int LOAD_SENT_HASHCODE = -89267375;
	public static int LOAD_ACCEPTED_HASHCODE = -1805310240;
	public static int LOADING_HASHCODE = 1054633244;
	public static int LOADED_HASHCODE = -2044189691;
	public static int SCAN_ASSIGNED_HASHCODE = -1301648048;
	public static int SCAN_SENT_HASHCODE = 990857242;
	public static int SCAN_ACCEPTED_HASHCODE = 1216675625;
	public static int SCANNING_HASHCODE = -107099983;
	public static int SCANNED_HASHCODE = -1666022960;
	public static int STAGE_ASSIGNED_HASHCODE = -1288061425;
	public static int STAGE_NOBLOCKING_HASHCODE = -1472673545;
	public static int STAGE_WAITING_HASHCODE = 1502336460;
	public static int MAPMAKE_ASSIGNED_HASHCODE = -2060208829;
	public static int MAPMAKE_SENT_HASHCODE = 1757970829;
	public static int MAPMAKE_ACCEPTED_HASHCODE = 458114844;
	public static int MAPMAKING_HASHCODE = 131112807;
	public static int MAPMADE_HASHCODE = 1555441745;
	public static int PATROL_ASSIGNED_HASHCODE = -1104777631;
	public static int PATROL_SENT_HASHCODE = 1422948779;
	public static int PATROL_ACCEPTED_HASHCODE = 1413546042;
	public static int PATROLLING_HASHCODE = -1894336446;
	public static int PATROLLED_HASHCODE = -1585128417;
	public static int PATROL_CANCELED_HASHCODE = -858069716;
	public static int VIBRATION_MONITORING_HASHCODE = -486782923;
	public static int LOAD_WAITING_HASHCODE = -1643626540;	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	
	// TRCMD PauseType 관련. MAX: 16
	public static String USER = "USER";
	public static String NOT_ACTIVE = "NOT ACTIVE";
	public static String PATH_SEARCH = "PATH SEARCH";
	public static String PORT_DUPLICATE = "PORT DUPLICATE";
	public static String AUTO_ERROR = "AUTO ERROR";
	public static String CARRIER_MISMATCH = "CARRIER MISMATCH";
	public static String STAGE_WAIT = "STAGE WAIT";
	public static String ABORTED_BY_MCS = "ABORTED BY MCS";
	public static String ABORTED_BY_DETOUR = "ABORTED BY DETOUR";
	public static String UNKNOWN_TRCMD = "UNKNOWN TRCMD";
	public static String UNLOADING_VHL_ERROR = "UNLOADING ERROR";
	public static String VEHICLE_MANUAL_ERROR = "VHL MANUAL ERR";
	public static String DW_TIMEOUT = "DW_TIMEOUT";	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	
	// PathSearch Options
	public static String UNLOAD = "UNLOAD";
	public static String LOAD = "LOAD";
	// public static String PATROL = "PATROL";
	// public static String MOVE = "MOVE";
	public static enum TRCMD_REMOTECMD {TRANSFER, STAGE, STAGECANCEL, STAGECHANGE, STAGEDELETE, DESTCHANGE, SCAN, MAPMAKE, PATROL, VIBRATION, VIBRATIONCHANGE, CANCEL, ABORT, REMOVE, PAUSE, RESUME, TRANSFERUPDATE, PREMOVE, VEHICLEAUTO, NULL;
		/**
		 * 
		 * @return requestedType REQUESTEDTYPE
		 */
		public REQUESTEDTYPE toRequestedType() {
			switch (this) {
				case TRANSFER:
					return REQUESTEDTYPE.TRANSFER;
				case STAGE:
					return REQUESTEDTYPE.STAGE;
				case STAGECANCEL:
					return REQUESTEDTYPE.STAGECANCEL;
				case STAGECHANGE:
					return REQUESTEDTYPE.STAGECHANGE;
				case STAGEDELETE:
					return REQUESTEDTYPE.STAGEDELETE;
				case DESTCHANGE:
					return REQUESTEDTYPE.DESTCHANGE;
				case SCAN:
					return REQUESTEDTYPE.SCAN;
				case MAPMAKE:
					return REQUESTEDTYPE.MAPMAKE;
				case PATROL:
					return REQUESTEDTYPE.PATROL;	
				case VIBRATION:
					return REQUESTEDTYPE.VIBRATION;	
				case VIBRATIONCHANGE:
					return REQUESTEDTYPE.VIBRATIONCHANGE;	
				case CANCEL:
					return REQUESTEDTYPE.CANCEL;	
				case ABORT:
					return REQUESTEDTYPE.ABORT;
				case REMOVE:
					return REQUESTEDTYPE.REMOVE;
				case PAUSE:
					return REQUESTEDTYPE.PAUSE;
				case RESUME:
					return REQUESTEDTYPE.RESUME;
				case TRANSFERUPDATE:	// 2021.01.21 by JJW
					return REQUESTEDTYPE.TRANSFERUPDATE;
				case PREMOVE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
					return REQUESTEDTYPE.PREMOVE;
				case VEHICLEAUTO:	// 2020.05.11 by YSJ (OHT Auto Change)
					return REQUESTEDTYPE.VEHICLEAUTO;
				case NULL:
				default:
					return REQUESTEDTYPE.NULL;
			}
		}
		
		/**
		 * 
		 * @return requestedType String
		 */
		public String toConstString() {
			switch (this) {
				case TRANSFER:
					return TrCmdConstant.TRANSFER;
				case PREMOVE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
					return TrCmdConstant.PREMOVE;
				case STAGE:
					return TrCmdConstant.STAGE;
				case STAGECANCEL:
					return TrCmdConstant.STAGECANCEL;
				case STAGECHANGE:
					return TrCmdConstant.STAGECHANGE;
				case STAGEDELETE:
					return TrCmdConstant.STAGEDELETE;
				case DESTCHANGE:
					return TrCmdConstant.DESTCHANGE;
				case SCAN:
					return TrCmdConstant.SCAN;
				case MAPMAKE:
					return TrCmdConstant.MAPMAKE;
				case PATROL:
					return TrCmdConstant.PATROL;	
				case VIBRATION:
					return TrCmdConstant.VIBRATION;	
				case VIBRATIONCHANGE:
					return TrCmdConstant.VIBRATIONCHANGE;	
				case CANCEL:
					return TrCmdConstant.CANCEL;	
				case ABORT:
					return TrCmdConstant.ABORT;
				case REMOVE:
					return TrCmdConstant.REMOVE;
				case PAUSE:
					return TrCmdConstant.PAUSE;
				case RESUME:
					return TrCmdConstant.RESUME;
				case TRANSFERUPDATE:
					return TrCmdConstant.TRANSFERUPDATE;
				case NULL:
				default:
					return TrCmdConstant.NULL;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return remoteCmd TRCMD_REMOTECMD
		 */
		public static TRCMD_REMOTECMD toRemoteCmd(String str) {
			if (str == null) {
				return TRCMD_REMOTECMD.NULL;
			} else {
				switch (str.hashCode()) {
					case TRANSFER_HASHCODE:
						return TRCMD_REMOTECMD.TRANSFER;
					case STAGE_HASHCODE:
						return TRCMD_REMOTECMD.STAGE;
					case STAGECANCEL_HASHCODE:
						return TRCMD_REMOTECMD.STAGECANCEL;
					case STAGECHANGE_HASHCODE:
						return TRCMD_REMOTECMD.STAGECHANGE;
					case STAGEDELETE_HASHCODE:
						return TRCMD_REMOTECMD.STAGEDELETE;
					case DESTCHANGE_HASHCODE:
						return TRCMD_REMOTECMD.DESTCHANGE;
					case SCAN_HASHCODE:
						return TRCMD_REMOTECMD.SCAN;
					case MAPMAKE_HASHCODE:
						return TRCMD_REMOTECMD.MAPMAKE;
					case PATROL_HASHCODE:
						return TRCMD_REMOTECMD.PATROL;
					case VIBRATION_HASHCODE:
						return TRCMD_REMOTECMD.VIBRATION;
					case VIBRATIONCHANGE_HASHCODE:
						return TRCMD_REMOTECMD.VIBRATIONCHANGE;
					case PREMOVE_HASHCODE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
						return TRCMD_REMOTECMD.PREMOVE;
					case CANCEL_HASHCODE:
						return TRCMD_REMOTECMD.CANCEL;
					case ABORT_HASHCODE:
						return TRCMD_REMOTECMD.ABORT;
					case REMOVE_HASHCODE:
						return TRCMD_REMOTECMD.REMOVE;
					case PAUSE_HASHCODE:
						return TRCMD_REMOTECMD.PAUSE;
					case RESUME_HASHCODE:
						return TRCMD_REMOTECMD.RESUME;
					case TRANSFERUPDATE_RESET_HASHCODE:
						return TRCMD_REMOTECMD.TRANSFERUPDATE;
					// 2020.05.11 by YSJ (OHT Auto Change)
					case VEHICLEAUTO_HASHCODE:
						return TRCMD_REMOTECMD.VEHICLEAUTO;
					case NULL_HASHCODE:
					default:
						return TRCMD_REMOTECMD.NULL;
				}				
			}
		}
	};
	
	public static enum REQUESTEDTYPE {TRANSFER, STAGE, STAGEWAIT, STAGENOBLOCK, STAGECANCEL, STAGECHANGE, SCAN, MAPMAKE, PATROL, VIBRATION, VIBRATIONCHANGE, CANCEL, ABORT, REMOVE, PAUSE, RESUME, PREMOVE,
		STAGEDELETE, UNASSIGN, DESTCHANGE, MOVE, PMOVE, ZONEMOVE, YIELD, LOCATE, LOCATECANCEL, PLOCATE, PLOCATECANCEL, // 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
		RESET, TRANSFER_RESET, STAGE_RESET, SCAN_RESET, MAPMAKE_RESET, PATROL_RESET, VIBRATION_RESET,
		STAGECHANGE_RESET, UNASSIGN_RESET, DESTCHANGE_RESET, MOVE_RESET, PMOVE_RESET, ZONEMOVE_RESET, YIELD_RESET, LOCATE_RESET, TRANSFERUPDATE, PLOCATE_RESET, // 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
//		PARK, PARKCANCEL, PARK_RESET,
		VEHICLEAUTO, VEHICLEAUTO_RESET, // 2020.05.11 by YSJ (OHT Auto Change)
		NULL;

		/**
		 * 
		 * @return requestedType String
		 */
		public String toConstString() {
			switch (this) {
				case TRANSFER:
					return TrCmdConstant.TRANSFER;
				case STAGE:
					return TrCmdConstant.STAGE;
				case STAGEWAIT:
					return TrCmdConstant.STAGEWAIT;
				case STAGENOBLOCK:
					return TrCmdConstant.STAGENOBLOCK;
				case STAGECANCEL:
					return TrCmdConstant.STAGECANCEL;
				case STAGECHANGE:
					return TrCmdConstant.STAGECHANGE;
				case SCAN:
					return TrCmdConstant.SCAN;
				case MAPMAKE:
					return TrCmdConstant.MAPMAKE;
				case PATROL:
					return TrCmdConstant.PATROL;	
				case VIBRATION:
					return TrCmdConstant.VIBRATION;	
				case VIBRATIONCHANGE:
					return TrCmdConstant.VIBRATIONCHANGE;	
				case CANCEL:
					return TrCmdConstant.CANCEL;	
				case ABORT:
					return TrCmdConstant.ABORT;
				case REMOVE:
					return TrCmdConstant.REMOVE;
				case PAUSE:
					return TrCmdConstant.PAUSE;
				case RESUME:
					return TrCmdConstant.RESUME;
				case PREMOVE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
					return TrCmdConstant.PREMOVE;
				case STAGEDELETE:
					return TrCmdConstant.STAGEDELETE;
				case UNASSIGN:
					return TrCmdConstant.UNASSIGN;
				case DESTCHANGE:
					return TrCmdConstant.DESTCHANGE;
				case MOVE:
					return TrCmdConstant.MOVE;
				// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
				case PMOVE:
					return TrCmdConstant.PMOVE;
				case ZONEMOVE:
					return TrCmdConstant.ZONEMOVE;
				case YIELD:
					return TrCmdConstant.YIELD;
				case LOCATE:
					return TrCmdConstant.LOCATE;
				case LOCATECANCEL:
					return TrCmdConstant.LOCATECANCEL;
				// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
				case PLOCATE:
					return TrCmdConstant.PLOCATE;
				case PLOCATECANCEL:
					return TrCmdConstant.PLOCATECANCEL;
//				case PARK:
//					return TrCmdConstant.PARK;
//				case PARKCANCEL:
//					return TrCmdConstant.PARKCANCEL;
				case RESET:
					return TrCmdConstant.RESET;
				case TRANSFER_RESET:
					return TrCmdConstant.TRANSFER_RESET;
				case STAGE_RESET:
					return TrCmdConstant.STAGE_RESET;
				case SCAN_RESET:
					return TrCmdConstant.SCAN_RESET;
				case MAPMAKE_RESET:
					return TrCmdConstant.MAPMAKE_RESET;
				case PATROL_RESET:
					return TrCmdConstant.PATROL_RESET;
				case VIBRATION_RESET:
					return TrCmdConstant.VIBRATION_RESET;
				case STAGECHANGE_RESET:
					return TrCmdConstant.STAGECHANGE_RESET;
				case UNASSIGN_RESET:
					return TrCmdConstant.UNASSIGN_RESET;
				case DESTCHANGE_RESET:
					return TrCmdConstant.DESTCHANGE_RESET;
				case MOVE_RESET:
					return TrCmdConstant.MOVE_RESET;
				// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
				case PMOVE_RESET:
					return TrCmdConstant.PMOVE_RESET;
				case ZONEMOVE_RESET:
					return TrCmdConstant.ZONEMOVE_RESET;
				case YIELD_RESET:
					return TrCmdConstant.YIELD_RESET;
				case LOCATE_RESET:
					return TrCmdConstant.LOCATE_RESET;
				// 2020.05.11 by YSJ (OHT Auto Change)
				case VEHICLEAUTO:
					return TrCmdConstant.VEHICLEAUTO;
				case VEHICLEAUTO_RESET:
					return TrCmdConstant.VEHICLEAUTO_RESET;
				// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
				case PLOCATE_RESET:
					return TrCmdConstant.PLOCATE_RESET;
				case NULL:
				default:
					return TrCmdConstant.NULL;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return requestedType REQUESTEDTYPE
		 */
		public static REQUESTEDTYPE toVehicleRequestedType(String str) {
			if (str == null) {
				return REQUESTEDTYPE.NULL;
			} else {
				switch (str.hashCode()) {
					case TRANSFER_HASHCODE:
						return REQUESTEDTYPE.TRANSFER;
					case STAGE_HASHCODE:
						return REQUESTEDTYPE.STAGE;
					case STAGECANCEL_HASHCODE:
						return REQUESTEDTYPE.STAGECANCEL;
					case STAGECHANGE_HASHCODE:
						return REQUESTEDTYPE.STAGECHANGE;
					case SCAN_HASHCODE:
						return REQUESTEDTYPE.SCAN;
					case MAPMAKE_HASHCODE:
						return REQUESTEDTYPE.MAPMAKE;
					case PATROL_HASHCODE:
						return REQUESTEDTYPE.PATROL;
					case VIBRATION_HASHCODE:
						return REQUESTEDTYPE.VIBRATION;
					case PREMOVE_HASHCODE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
						return REQUESTEDTYPE.PREMOVE;
					case VIBRATIONCHANGE_HASHCODE:
						return REQUESTEDTYPE.VIBRATIONCHANGE;
					case CANCEL_HASHCODE:
						return REQUESTEDTYPE.CANCEL;
					case ABORT_HASHCODE:
						return REQUESTEDTYPE.ABORT;
					case REMOVE_HASHCODE:
						return REQUESTEDTYPE.REMOVE;
					case PAUSE_HASHCODE:
						return REQUESTEDTYPE.PAUSE;
					case RESUME_HASHCODE:
						return REQUESTEDTYPE.RESUME;
					case STAGEDELETE_HASHCODE:
						return REQUESTEDTYPE.STAGEDELETE;
					case UNASSIGN_HASHCODE:
						return REQUESTEDTYPE.UNASSIGN;
					case DESTCHANGE_HASHCODE:
						return REQUESTEDTYPE.DESTCHANGE;
					case MOVE_HASHCODE:
						return REQUESTEDTYPE.MOVE;
					// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
					case PMOVE_HASHCODE:
						return REQUESTEDTYPE.PMOVE;
					case ZONEMOVE_HASHCODE:
						return REQUESTEDTYPE.ZONEMOVE;
					case YIELD_HASHCODE:
						return REQUESTEDTYPE.YIELD;
					case LOCATE_HASHCODE:
						return REQUESTEDTYPE.LOCATE;
					case LOCATECANCEL_HASHCODE:
						return REQUESTEDTYPE.LOCATECANCEL;
					// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
					case PLOCATE_HASHCODE:
						return REQUESTEDTYPE.PLOCATE;
					case PLOCATECANCEL_HASHCODE:
						return REQUESTEDTYPE.PLOCATECANCEL;
//					case PARK_HASHCODE:
//						return REQUESTEDTYPE.PARK;
//					case PARKCANCEL_HASHCODE:
//						return REQUESTEDTYPE.PARKCANCEL;
					case RESET_HASHCODE:
						return REQUESTEDTYPE.RESET;
					case TRANSFER_RESET_HASHCODE:
						return REQUESTEDTYPE.TRANSFER_RESET;
					case STAGE_RESET_HASHCODE:
						return REQUESTEDTYPE.STAGE_RESET;
					case SCAN_RESET_HASHCODE:
						return REQUESTEDTYPE.SCAN_RESET;
					case MAPMAKE_RESET_HASHCODE:
						return REQUESTEDTYPE.MAPMAKE_RESET;
					case PATROL_RESET_HASHCODE:
						return REQUESTEDTYPE.PATROL_RESET;
					case VIBRATION_RESET_HASHCODE:
						return REQUESTEDTYPE.VIBRATION_RESET;
					case STAGECHANGE_RESET_HASHCODE:
						return REQUESTEDTYPE.STAGECHANGE_RESET;
					case UNASSIGN_RESET_HASHCODE:
						return REQUESTEDTYPE.UNASSIGN_RESET;
					case DESTCHANGE_RESET_HASHCODE:
						return REQUESTEDTYPE.DESTCHANGE_RESET;
					case MOVE_RESET_HASHCODE:
						return REQUESTEDTYPE.MOVE_RESET;
					// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
					case PMOVE_RESET_HASHCODE:
						return REQUESTEDTYPE.PMOVE_RESET;
					case ZONEMOVE_RESET_HASHCODE:
						return REQUESTEDTYPE.ZONEMOVE_RESET;
					case YIELD_RESET_HASHCODE:
						return REQUESTEDTYPE.YIELD_RESET;
					case LOCATE_RESET_HASHCODE:
						return REQUESTEDTYPE.LOCATE_RESET;
					// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
					case PLOCATE_RESET_HASHCODE:
						return REQUESTEDTYPE.PLOCATE_RESET;
//					case PARK_RESET_HASHCODE:
//						return REQUESTEDTYPE.PARK_RESET;
					case STAGEWAIT_HASHCODE:
						return REQUESTEDTYPE.STAGEWAIT;
					case STAGENOBLOCK_HASHCODE:
						return REQUESTEDTYPE.STAGENOBLOCK;
					// 2020.05.11 by YSJ (OHT Auto Change)
					case VEHICLEAUTO_HASHCODE:
						return REQUESTEDTYPE.VEHICLEAUTO;
					case VEHICLEAUTO_RESET_HASHCODE:
						return REQUESTEDTYPE.VEHICLEAUTO_RESET;
					case NULL_HASHCODE:
					default:
						return REQUESTEDTYPE.NULL;
				}
			}
		}
	};
	
	public static enum TRCMD_STATE { CMD_QUEUED, CMD_WAITING, CMD_TRANSFERRING, CMD_PREMOVE, CMD_COMPLETED, 
		CMD_STAGING, CMD_SCANNING, CMD_MAPMAKING, CMD_PATROLLING, CMD_MONITORING,  
		CMD_PAUSED, 
		CMD_CANCELLING, CMD_CANCELED, CMD_CANCELFAILED, 
		CMD_ABORTING, CMD_ABORTED, CMD_ABORTFAILED,
		NULL;
		/**
		 * 
		 * @return state String
		 */
		public String toConstString() {
			switch (this) {
				case CMD_QUEUED :
					return TrCmdConstant.CMD_QUEUED;
				case CMD_WAITING :
					return TrCmdConstant.CMD_WAITING;
				case CMD_TRANSFERRING :
					return TrCmdConstant.CMD_TRANSFERRING;
				case CMD_COMPLETED:
					return TrCmdConstant.CMD_COMPLETED;
				case CMD_STAGING:
					return TrCmdConstant.CMD_STAGING;	
				case CMD_SCANNING:
					return TrCmdConstant.CMD_SCANNING;	
				case CMD_MAPMAKING:
					return TrCmdConstant.CMD_MAPMAKING;	
				case CMD_PATROLLING:
					return TrCmdConstant.CMD_PATROLLING;	
				case CMD_MONITORING:
					return TrCmdConstant.CMD_MONITORING;	
				case CMD_PAUSED:
					return TrCmdConstant.CMD_PAUSED;	
				case CMD_CANCELLING:
					return TrCmdConstant.CMD_CANCELLING;	
				case CMD_CANCELED:
					return TrCmdConstant.CMD_CANCELED;	
				case CMD_CANCELFAILED:
					return TrCmdConstant.CMD_CANCELFAILED;	
				case CMD_ABORTING:
					return TrCmdConstant.CMD_ABORTING;	
				case CMD_ABORTED:
					return TrCmdConstant.CMD_ABORTED;
				case CMD_ABORTFAILED:
					return TrCmdConstant.CMD_ABORTFAILED;
				case CMD_PREMOVE :	// 2021.04.02 by JDH : Transfer Premove 사양 추가
					return TrCmdConstant.CMD_PREMOVE;
				case NULL:
				default:
					return TrCmdConstant.NULL;
			}
		};
	
		/**
		 * 
		 * @param str
		 * @return state TRCMD_STATE
		 */
		public static TRCMD_STATE toTrCmdState(String str) {
			switch (str.hashCode()) {
				case CMD_QUEUED_HASHCODE:
					return TRCMD_STATE.CMD_QUEUED;
				case CMD_WAITING_HASHCODE:
					return TRCMD_STATE.CMD_WAITING;
				case CMD_TRANSFERRING_HASHCODE:
					return TRCMD_STATE.CMD_TRANSFERRING;
				case CMD_COMPLETED_HASHCODE:
					return TRCMD_STATE.CMD_COMPLETED;
				case CMD_STAGING_HASHCODE:
					return TRCMD_STATE.CMD_STAGING;
				case CMD_SCANNING_HASHCODE:
					return TRCMD_STATE.CMD_SCANNING;
				case CMD_MAPMAKING_HASHCODE:
					return TRCMD_STATE.CMD_MAPMAKING;
				case CMD_PATROLLING_HASHCODE:
					return TRCMD_STATE.CMD_PATROLLING;
				case CMD_MONITORING_HASHCODE:
					return TRCMD_STATE.CMD_MONITORING;
				case CMD_PAUSED_HASHCODE:
					return TRCMD_STATE.CMD_PAUSED;
				case CMD_CANCELLING_HASHCODE:
					return TRCMD_STATE.CMD_CANCELLING;
				case CMD_CANCELED_HASHCODE:
					return TRCMD_STATE.CMD_CANCELED;
				case CMD_CANCELFAILED_HASHCODE:
					return TRCMD_STATE.CMD_CANCELFAILED;
				case CMD_ABORTING_HASHCODE:
					return TRCMD_STATE.CMD_ABORTING;
				case CMD_ABORTED_HASHCODE:
					return TRCMD_STATE.CMD_ABORTED;
				case CMD_ABORTFAILED_HASHCODE:
					return TRCMD_STATE.CMD_ABORTFAILED;
				case CMD_PREMOVE_HASHCODE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
					return TRCMD_STATE.CMD_PREMOVE;
				case NULL_HASHCODE:
				default:
					return TRCMD_STATE.NULL;
			}
		}
	};

	public static enum TRCMD_DETAILSTATE {NOT_ASSIGNED, UNLOAD_ASSIGNED, UNLOAD_SENT, UNLOAD_ACCEPTED, UNLOADING, UNLOADED, 
		LOAD_ASSIGNED, LOAD_SENT, LOAD_ACCEPTED, LOADING, LOADED, LOAD_WAITING , 
		SCAN_ASSIGNED, SCAN_SENT, SCAN_ACCEPTED, SCANNING, SCANNED,
		STAGE_ASSIGNED, STAGE_NOBLOCKING, STAGE_WAITING, 
		MAPMAKE_ASSIGNED, MAPMAKE_SENT, MAPMAKE_ACCEPTED, MAPMAKING, MAPMADE,
		PATROL_ASSIGNED, PATROL_SENT, PATROL_ACCEPTED, PATROLLING, PATROLLED, PATROL_CANCELED,
		VIBRATION_MONITORING,
		NULL;
		/**
		 * 
		 * @return detailState String
		 */
		public String toConstString() {
			switch (this) {
				case NOT_ASSIGNED:
					return TrCmdConstant.NOT_ASSIGNED;
				case UNLOAD_ASSIGNED:
					return TrCmdConstant.UNLOAD_ASSIGNED;
				case UNLOAD_SENT:
					return TrCmdConstant.UNLOAD_SENT;
				case UNLOAD_ACCEPTED:
					return TrCmdConstant.UNLOAD_ACCEPTED;
				case UNLOADING:
					return TrCmdConstant.UNLOADING;	
				case UNLOADED:
					return TrCmdConstant.UNLOADED;	
				case LOAD_ASSIGNED:
					return TrCmdConstant.LOAD_ASSIGNED;	
				case LOAD_SENT:
					return TrCmdConstant.LOAD_SENT;	
				case LOAD_ACCEPTED:
					return TrCmdConstant.LOAD_ACCEPTED;	
				case LOADING:
					return TrCmdConstant.LOADING;	
				case LOADED:
					return TrCmdConstant.LOADED;
				case SCAN_ASSIGNED:
					return TrCmdConstant.SCAN_ASSIGNED;
				case SCAN_SENT:
					return TrCmdConstant.SCAN_SENT;
				case SCAN_ACCEPTED:
					return TrCmdConstant.SCAN_ACCEPTED;
				case SCANNING:
					return TrCmdConstant.SCANNING;
				case SCANNED:
					return TrCmdConstant.SCANNED;
				case STAGE_ASSIGNED:
					return TrCmdConstant.STAGE_ASSIGNED;
				case STAGE_NOBLOCKING:
					return TrCmdConstant.STAGE_NOBLOCKING;
				case STAGE_WAITING:
					return TrCmdConstant.STAGE_WAITING;
				case MAPMAKE_ASSIGNED:
					return TrCmdConstant.MAPMAKE_ASSIGNED;
				case MAPMAKE_SENT:
					return TrCmdConstant.MAPMAKE_SENT;
				case MAPMAKE_ACCEPTED:
					return TrCmdConstant.MAPMAKE_ACCEPTED;
				case MAPMAKING:
					return TrCmdConstant.MAPMAKING;
				case MAPMADE:
					return TrCmdConstant.MAPMADE;
				case PATROL_ASSIGNED:
					return TrCmdConstant.PATROL_ASSIGNED;
				case PATROL_SENT:
					return TrCmdConstant.PATROL_SENT;
				case PATROL_ACCEPTED:
					return TrCmdConstant.PATROL_ACCEPTED;
				case PATROLLING:
					return TrCmdConstant.PATROLLING;
				case PATROLLED:
					return TrCmdConstant.PATROLLED;
				case PATROL_CANCELED:
					return TrCmdConstant.PATROL_CANCELED;
				case VIBRATION_MONITORING:
					return TrCmdConstant.VIBRATION_MONITORING;
				case LOAD_WAITING:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
					return TrCmdConstant.LOAD_WAITING;
				case NULL:
				default:
					return TrCmdConstant.NULL;
			}
		};
	
		/**
		 * 
		 * @param str
		 * @return detailState TRCMD_DETAILSTATE
		 */
		public static TRCMD_DETAILSTATE toTrCmdDetailState(String str) {
			if (str == null) {
				return TRCMD_DETAILSTATE.NULL;
			} else {
				switch (str.hashCode()) {
					case NOT_ASSIGNED_HASHCODE:
						return TRCMD_DETAILSTATE.NOT_ASSIGNED;
					case UNLOAD_ASSIGNED_HASHCODE:
						return TRCMD_DETAILSTATE.UNLOAD_ASSIGNED;
					case UNLOAD_SENT_HASHCODE:
						return TRCMD_DETAILSTATE.UNLOAD_SENT;
					case UNLOAD_ACCEPTED_HASHCODE:
						return TRCMD_DETAILSTATE.UNLOAD_ACCEPTED;
					case UNLOADING_HASHCODE:
						return TRCMD_DETAILSTATE.UNLOADING;
					case UNLOADED_HASHCODE:
						return TRCMD_DETAILSTATE.UNLOADED;
					case LOAD_ASSIGNED_HASHCODE:
						return TRCMD_DETAILSTATE.LOAD_ASSIGNED;
					case LOAD_SENT_HASHCODE:
						return TRCMD_DETAILSTATE.LOAD_SENT;
					case LOAD_ACCEPTED_HASHCODE:
						return TRCMD_DETAILSTATE.LOAD_ACCEPTED;
					case LOADING_HASHCODE:
						return TRCMD_DETAILSTATE.LOADING;
					case LOADED_HASHCODE:
						return TRCMD_DETAILSTATE.LOADED;
					case SCAN_ASSIGNED_HASHCODE:
						return TRCMD_DETAILSTATE.SCAN_ASSIGNED;
					case SCAN_SENT_HASHCODE:
						return TRCMD_DETAILSTATE.SCAN_SENT;
					case SCAN_ACCEPTED_HASHCODE:
						return TRCMD_DETAILSTATE.SCAN_ACCEPTED;
					case SCANNING_HASHCODE:
						return TRCMD_DETAILSTATE.SCANNING;
					case SCANNED_HASHCODE:
						return TRCMD_DETAILSTATE.SCANNED;
					case STAGE_ASSIGNED_HASHCODE:
						return TRCMD_DETAILSTATE.STAGE_ASSIGNED;
					case STAGE_NOBLOCKING_HASHCODE:
						return TRCMD_DETAILSTATE.STAGE_NOBLOCKING;
					case STAGE_WAITING_HASHCODE:
						return TRCMD_DETAILSTATE.STAGE_WAITING;
					case MAPMAKE_ASSIGNED_HASHCODE:
						return TRCMD_DETAILSTATE.MAPMAKE_ASSIGNED;
					case MAPMAKE_SENT_HASHCODE:
						return TRCMD_DETAILSTATE.MAPMAKE_SENT;
					case MAPMAKE_ACCEPTED_HASHCODE:
						return TRCMD_DETAILSTATE.MAPMAKE_ACCEPTED;
					case MAPMAKING_HASHCODE:
						return TRCMD_DETAILSTATE.MAPMAKING;
					case MAPMADE_HASHCODE:
						return TRCMD_DETAILSTATE.MAPMADE;
					case PATROL_ASSIGNED_HASHCODE:
						return TRCMD_DETAILSTATE.PATROL_ASSIGNED;
					case PATROL_SENT_HASHCODE:
						return TRCMD_DETAILSTATE.PATROL_SENT;
					case PATROL_ACCEPTED_HASHCODE:
						return TRCMD_DETAILSTATE.PATROL_ACCEPTED;
					case PATROLLING_HASHCODE:
						return TRCMD_DETAILSTATE.PATROLLING;
					case PATROLLED_HASHCODE:
						return TRCMD_DETAILSTATE.PATROLLED;
					case PATROL_CANCELED_HASHCODE:
						return TRCMD_DETAILSTATE.PATROL_CANCELED;
					case VIBRATION_MONITORING_HASHCODE:
						return TRCMD_DETAILSTATE.VIBRATION_MONITORING;
					case LOAD_WAITING_HASHCODE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
						return TRCMD_DETAILSTATE.LOAD_WAITING;
					case NULL_HASHCODE:
					default:
						return TRCMD_DETAILSTATE.NULL;
				}
			}
		}
	};
}
