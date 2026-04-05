package com.samsung.ocs.common.constant;

/**
 * OcsConstant Interface, OCS 3.0 for Unified FAB
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

public interface OcsConstant {
	public static String NULL = "";
	public static int NULL_HASHCODE = 0; 

	public static String HOMEDIR = "user.dir";
	public static String FILESEPARATOR = "file.separator";
	public static String LINESEPARATOR = "line.separator";
	public static String MODULENAME = "moduleName";
	public static String DBCONNECTION = "dbConnection";
	public static String URL = "url";
	public static String PRIMARYURL = "PrimaryUrl";
	public static String SECONDARYURL = "SecondaryUrl";
	public static String USERNAME = "userName";
	public static String PASSWORD = "passWord";
	public static String RECONNECTINTERVALMILLIS = "reconnectIntervalMillis";
	public static String LOGINTIMEOUTSECONDS = "logInTimeoutSeconds";
	
	
	public static String LOG = "log";
	public static String EXCEPTION = "Exception";
	public static String OPERATION = "Operation";
	public static String JOBASSIGN = "JobAssign";
	public static String IBSEM = "IBSEM";
	public static String STBC = "STBC";
	public static int OPERATION_HASHCODE = -628296377;
	public static int JOBASSIGN_HASHCODE = -1677547796;
	public static int IBSEM_HASHCODE = 69465218;

	public static String USAGE="usage";

	public static String HOSTNAME = "hostName";
	public static String HOSTSERVICETYPE = "hostServiceType";
	public static String REMOTEHOSTIPADDRESS = "remoteHostIpAddress";
	public static String VEHICLESOCKETPORT = "vehicleSocketPort";
	public static String STBREPORTSOCKETPORT = "stbReportSocketPort";

	// Route 관련
	public static final double MAXCOST_TIMEBASE = 9999.0;
	public static final long MAXCOST_DISTANCEBASE = 99999999;
	public static enum SEARCH_TYPE {NODE, SECTION};
	
	public static final int COMMUNICATION_FAIL = 1000;

	// heart beat
	public static String HEARTBEATLIST = "heartBeatList";
	public static String HEARTBEAT = "heartBeat";
	public static String IPADDRESS = "ipAddress";
	public static String PORT = "port";
	public static String INTERVALMILLIS = "intervalMillis";
	public static String TIMEOUTCOUNT = "timeoutCount";
	public static String LISTENINTERVALCOUNT = "listenIntervalCount";


	public static String PINGTIMEOUTMILLIS = "pingTimeoutMillis";
	// public net check
	public static String PUBLICNETCHECK = "publicNetCheck";
	public static String PUBLICNETCHECKGTIP = "gatewayIpAddress";
	public static String PUBLICNETCHECKREMOTEIP = "remotePublicIpAddress";

	// local net check
	public static String LOCALNETCHECK = "localNetCheck";
	public static String LOCALNETCHECKREMOTEIP = "remoteLocalIpAddress";

	// db conn check
	public static String DBCONNCHECK = "databaseConnCheck";

	// UNKNOWN STATE
	public static String FAILOVERUSE = "failoverUse";
	public static String DELETELOGDAY = "deleteLogDay";
	public static String DELETEHISTORYDAY = "deleteHistoryDay";
	// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화
	public static String DELETEHISTORYCHECKINTERVAL = "deleteHistoryCheckIntervalSec";


	public static String COMMONLOGFILE = "OcsModuleCommon";
	public static String UDPCOMMLOGFILE = "OcsUdpComm";
	public static String DBPROCESSLOGFILE = "OcsDBProcess";
	public static String FAILOVERLOGFILE = "OcsFailOver";
	public static String FAILOVERINTERFACEFILE = "OcsFailOverModule";
	public static String PROCMANAGE = "OcsWatchdogManage";

	// FailOver관련
	public static String INSERVICE = "InService";
	public static String OUTOFSERVICE = "OutOfService";
	public static String REQOUTOFSERVICE = "ReqOutOfService";
	public static String REQINSERVICE = "ReqInService";
	public static String UNKNOWN = "Unknown";
	public static String TRUE = "TRUE";
	public static String FALSE = "FALSE";

	public static String PRIMARY = "Primary";
	public static String SECONDARY= "Secondary";

	public static String DISCONNECTED = "Disconnected";
	public static String CONNECTED = "Connected";

	public static String DOWNREQUESTCMD = "YOUDIE!";
	public static String ACTIVATECMD = "activated!";
	public static String DEACTIVATECMD = "deactivated!";

	// localProcess 관련
	public static String WATCHDOGINTERVAL = "watchdogIntervalMillis";
	public static String DAEMONLISTENPORT = "daemonListenPort";
	public static String REPORTINTERVALMILLIS = "reportIntervalMillis";
	public static String LOCALPROCESSLIST = "localProcessList";
	public static String LISTENPORT = "listenPort";
	public static String WATCHDOGCONTROL = "watchdogControl";
	public static String RUN = "run";
	public static String STOP = "stop";

	// Block 관련
	public static String OK = "OK";
	public static String ABNORMAL_OK = "AbnormalOK";
	public static String CONVERGE = "CONVERGE";
	public static String DIVERGE = "DIVERGE";
	public static String MULTI = "MULTI"; // 2013.07.30 by KYK
	public static String RESET = "RESET";
	public static String SET = "SET";
	public static String LINE = "LINE";
	public static String CURVE = "CURVE";
	
	public static String REQUEST_PATH_SEARCH = "ReqPathSearch";
	
	// HID 관련
	// Normal
	public static String HID_RUN = "R";
	public static String HID_FAILOVER = "O";
	
	// Abnormal
	public static String HID_FAULT = "F";
	public static String HID_WARNING = "W";
	public static String HID_STOP = "S";
	public static String HID_TIMEOUT = "T";
	
	public static String HID_NOTUSE = "N";
	public static String HID_UNKNOWN = "U";

	// Carrierloc Type 관련
	public static String STBPORT = "STBPORT";
	public static String EQPORT = "EQPORT";
	public static String UTBPORT = "UTBPORT";
	public static String STOCKERPORT = "STOCKERPORT";
	public static String LOADERPORT = "LOADERPORT";
	public static String VEHICLEPORT = "VEHICLEPORT";
	public static int STBPORT_HASHCODE = -1178342910;
	public static int EQPORT_HASHCODE = 2052678445;
	public static int UTBPORT_HASHCODE = 596664452;
	public static int STOCKERPORT_HASHCODE = 1495722276;
	public static int LOADERPORT_HASHCODE = 782725908;
	public static int VEHICLEPORT_HASHCODE = -1929886323;
	
	// PortInservice/PortOutOfService 관련
	public static String PORT_INSERVICE = "PortInService";	
	public static String PORT_OUTOFSERVICE = "PortOutOfService";	
	
	// Operation Control Mode
	public static String INIT = "INIT";
	public static String READY = "READY";
	public static String START = "START";

	public static String DOWNREQUESTTIMEOUTCOUNT = "downRequestTimeoutCount";
	public static String INSERVICEREQUESTTIMEOUTCOUNT = "inserviceRequestTimeoutCount";

	public static enum INIT_STATE { INIT_BEGINED, LOAD_CONFIG, DB_CONNECTION_INIT, THREAD_INIT, INIT_COMPLETED };
	
	public static String POLICY = "policy";
	
	public static String ALWAYS_UP = "ALWAYS_UP";
	public static String ALWAYS_DOWN = "ALWAYS_DOWN";
	public static String BALANCING = "BALANCING";
	public static String VIP_STRICT = "VIP_STRICT";
	public static String VIP_BASE_UP = "VIP_BASE_UP";
	public static String VIP_BASE_DOWN = "VIP_BASE_DOWN";
	
	// Rail-Down Control 관련
	public static String IN = "IN";
	public static String OUT = "OUT";
	
	public static char USERREQUEST_TYPE_PATROL = 'P';
	public static char USERREQUEST_TYPE_VIBRATION = 'V';
	public static char USERREQUEST_TYPE_RANDOM_TRANSFER = 'R';
	public static char USERREQUEST_TYPE_FIXED_TRANSFER = 'F';
	public static char USERREQUEST_TYPE_MOVE = 'M';
	public static char USERREQUEST_TYPE_UNDEFINED = '0';
	
	public static char CYCLE_UNIT_DAY = 'D';
	public static char CYCLE_UNIT_HOUR = 'H';
	public static char CYCLE_UNIT_MINUTE = 'M';
	public static char CYCLE_UNIT_SECOND = 'S';
	public static char CYCLE_UNIT_UNDEFINED = '0';
	
	public static char TOUR_TYPE_NODE = 'N';
	public static char TOUR_TYPE_PAIR = 'P';
	public static char TOUR_TYPE_CARRIERLOC = 'C';
	public static char TOUR_TYPE_UNDEFINED = '0';
	
	public static enum WATCHDOC_POLICY { ALWAYS_UP , ALWAYS_DOWN, VIP_STRICT, VIP_BASE_UP, VIP_BASE_DOWN, BALANCING;
		/**
		 * 
		 * @param string
		 * @return watchDocPolicy WATCHDOC_POLICY
		 */
		public static WATCHDOC_POLICY toWatchdogPolicy(String string) {
			if (OcsConstant.ALWAYS_UP.equalsIgnoreCase(string)) {
				return WATCHDOC_POLICY.ALWAYS_UP;
			} else if (OcsConstant.ALWAYS_DOWN.equalsIgnoreCase(string)) {
				return WATCHDOC_POLICY.ALWAYS_DOWN;
			} else if (OcsConstant.VIP_STRICT.equalsIgnoreCase(string)) {
				return WATCHDOC_POLICY.VIP_STRICT;
			} else if (OcsConstant.VIP_BASE_UP.equalsIgnoreCase(string)) {
				return WATCHDOC_POLICY.VIP_BASE_UP;
			} else if (OcsConstant.VIP_BASE_DOWN.equalsIgnoreCase(string)) {
				return WATCHDOC_POLICY.VIP_BASE_DOWN;
			} else if (OcsConstant.BALANCING.equalsIgnoreCase(string)) {
				return WATCHDOC_POLICY.BALANCING;
			} else {
				return WATCHDOC_POLICY.ALWAYS_DOWN;
			}
		}
	}
	
	public static enum MODULE_STATE { INSERVICE, REQOUTOFSERVICE, OUTOFSERVICE, REQINSERVICE, UNKNOWN;
		/**
		 * 
		 * @return moduleState String
		 */
		public String toConstString() {
			switch(this) {
				case INSERVICE :
					return OcsConstant.INSERVICE;
				case REQINSERVICE :
					return OcsConstant.REQINSERVICE;
				case REQOUTOFSERVICE :
					return OcsConstant.REQOUTOFSERVICE;
				case OUTOFSERVICE:
					return OcsConstant.OUTOFSERVICE;
				case UNKNOWN:
				default:
					return OcsConstant.UNKNOWN;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return moduleState MODULE_STATE
		 */
		public static MODULE_STATE toModuleState(String str) {
			if (OcsConstant.OUTOFSERVICE.equalsIgnoreCase(str)) {
				return MODULE_STATE.OUTOFSERVICE;
			} else if (OcsConstant.INSERVICE.equalsIgnoreCase(str)) {
				return MODULE_STATE.INSERVICE;
			} else if (OcsConstant.REQINSERVICE.equalsIgnoreCase(str)) {
				return MODULE_STATE.REQINSERVICE;
			} else if (OcsConstant.REQOUTOFSERVICE.equalsIgnoreCase(str)) {
				return MODULE_STATE.REQOUTOFSERVICE;
			} else {
				return MODULE_STATE.UNKNOWN;
			}
		}
	};
	
	// Operation Control Mode
	public static enum OPERAION_CONTROL_STATE { INIT, READY, START ;
		/**
		 * 
		 * @return operationControlState String
		 */
		public String toConstString() {
			switch(this) {
				case READY :
					return OcsConstant.READY;
				case START :
					return OcsConstant.START;
				case INIT :
				default:
					return OcsConstant.INIT;
			}
		};
	};

	public static String FAILOVERPOLICY = "failoverPolicy";
	public static String VIPBASE = "VIPBASE";
	public static String NETDBBASE = "NETDBBASE";
	public static String ANYHOST = "ANYHOST";
	public static enum FAILOVER_POLICY { VIPBASE, NETDBBASE, ANYHOST ;
		/**
		 * 
		 * @return failoverPolicy String
		 */
		public String toConstString() {
			switch(this) {
				case VIPBASE :
					return OcsConstant.VIPBASE;
				case ANYHOST :
					return OcsConstant.ANYHOST;
				case NETDBBASE :
				default:
					return OcsConstant.NETDBBASE;
			}
		};
		
		/**
		 *
		 * @param str
		 * @return failoverPolicy FAILOVER_POLICY
		 */
		public static FAILOVER_POLICY toFailoverPolicy(String str) {
			if (OcsConstant.VIPBASE.equalsIgnoreCase(str)) {
				return FAILOVER_POLICY.VIPBASE;
			} else if (OcsConstant.NETDBBASE.equalsIgnoreCase(str)) {
				return FAILOVER_POLICY.NETDBBASE;
			} else if (OcsConstant.ANYHOST.equalsIgnoreCase(str)) {
				return FAILOVER_POLICY.ANYHOST;
			} else {
				return FAILOVER_POLICY.NETDBBASE;
			}
		}
	};

	public static enum FAILOVER_STATE {INSERVICE, REQOUTOFSERVICE, OUTOFSERVICE, REQINSERVICE };

	public static enum CARRIERLOC_TYPE {STBPORT, EQPORT, UTBPORT, STOCKERPORT, LOADERPORT, VEHICLEPORT, NULL;
		/**
		 * 
		 * @return carrierLocType String
		 */
		public String toConstString() {
			switch(this) {
				case STBPORT :
					return OcsConstant.STBPORT;
				case EQPORT :
					return OcsConstant.EQPORT;
				case UTBPORT :
					return OcsConstant.UTBPORT;
				case STOCKERPORT:
					return OcsConstant.STOCKERPORT;
				case LOADERPORT:
					return OcsConstant.LOADERPORT;	
				case VEHICLEPORT:
					return OcsConstant.VEHICLEPORT;
				case NULL:
				default:
					return OcsConstant.NULL;
			}
		};
		
		/**
		 * 
		 * @return eqOptionForWorkMode char
		 */
		public char toEqOptionForWorkMode() {
			switch(this) {
				case STBPORT :
					return '1';
				case UTBPORT :
					return '2';
				case LOADERPORT:
					return '3';	
				default:
					return '0';
			}
		};
		
		/**
		 * 
		 * @return eqOptionForScanMode char
		 */
		public char toEqOptionForScanMode() {
			switch(this) {
				case UTBPORT :
					return 'U';
				case STBPORT :
				default:
					return 'S';
			}
		};
		
		/**
		 * 
		 * @return eqOptionForGoMode char
		 */
		public char toEqOptionForGoMode() {
			switch(this) {
				case STBPORT :
					return 'S';
				case EQPORT :
					return 'E';
				case UTBPORT :
					return 'U';
				case STOCKERPORT:
					return 'T';
				case LOADERPORT:
					return 'L';	
				default:
					return 'N';
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return carrierLocType CARRIERLOC_TYPE
		 */
		public static CARRIERLOC_TYPE toCarrierlocType(String str) {
			switch (str.hashCode()) {
				case STBPORT_HASHCODE:
					return CARRIERLOC_TYPE.STBPORT;
				case EQPORT_HASHCODE:
					return CARRIERLOC_TYPE.EQPORT;
				case UTBPORT_HASHCODE:
					return CARRIERLOC_TYPE.UTBPORT;
				case STOCKERPORT_HASHCODE:
					return CARRIERLOC_TYPE.STOCKERPORT;
				case LOADERPORT_HASHCODE:
					return CARRIERLOC_TYPE.LOADERPORT;
				case VEHICLEPORT_HASHCODE:
					return CARRIERLOC_TYPE.VEHICLEPORT;
				case NULL_HASHCODE:
				default:
					return CARRIERLOC_TYPE.NULL;
			}
		}
	};
	
	public static String AUTOTAKEOVERCOUNT = "autoTakeOverCount";
	public static String REMOTEINITTIMEOUTCOUNT = "remoteInitTimeOutCount";
	
	public static String NETCHECKUSAGE = "netCheckUsage";
	public static String PUBLIC = "public";
	public static String LOCAL = "local";
	public static String VIAVIP = "VIA VIP";
	public static String UNKNOWNWAITCOUNT = "unknownWaitCount";
	public static String DIRECTHEARTBEAT = "directHeartBeat";
	// 2018.01.24 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화
	public static String DELETEHISTORY = "deleteHistory";

	
	public static enum DEADLOCK_TYPE {NONE, HID, RESERVED, NODE};
	
	public static enum USERREQUEST_TYPE {PATROL, VIBRATION, RANDOM_TRANSFER, FIXED_TRANSFER, MOVE, UNDEFINED;
		/**
		 * 
		 * @return UserRequestType char
		 */
		public char toChar() {
			switch(this) {
				case PATROL :
					return OcsConstant.USERREQUEST_TYPE_PATROL;
				case VIBRATION :
					return OcsConstant.USERREQUEST_TYPE_VIBRATION;
				case RANDOM_TRANSFER :
					return OcsConstant.USERREQUEST_TYPE_RANDOM_TRANSFER;
				case FIXED_TRANSFER:
					return OcsConstant.USERREQUEST_TYPE_FIXED_TRANSFER;
				case MOVE:
					return OcsConstant.USERREQUEST_TYPE_MOVE;	
				case UNDEFINED:
				default:
					return OcsConstant.USERREQUEST_TYPE_UNDEFINED;
			}
		};
		
		/**
		 * 
		 * @param userRequestType
		 * @return UserRequestType USERREQUEST_TYPE
		 */
		public static USERREQUEST_TYPE toUserRequestType(char userRequestType) {
			switch (userRequestType) {
				case USERREQUEST_TYPE_PATROL:
					return USERREQUEST_TYPE.PATROL;
				case USERREQUEST_TYPE_VIBRATION:
					return USERREQUEST_TYPE.VIBRATION;
				case USERREQUEST_TYPE_RANDOM_TRANSFER:
					return USERREQUEST_TYPE.RANDOM_TRANSFER;
				case USERREQUEST_TYPE_FIXED_TRANSFER:
					return USERREQUEST_TYPE.FIXED_TRANSFER;
				case USERREQUEST_TYPE_MOVE:
					return USERREQUEST_TYPE.MOVE;
				case USERREQUEST_TYPE_UNDEFINED:
				default:
					return USERREQUEST_TYPE.UNDEFINED;
			}
		}
	};
	
	public static enum CYCLE_UNIT {DAY, HOUR, MINUTE, SECOND, UNDEFINED;
		/**
		 * 
		 * @return CycleUnit char
		 */
		public char toChar() {
			switch(this) {
				case DAY :
					return OcsConstant.CYCLE_UNIT_DAY;
				case HOUR :
					return OcsConstant.CYCLE_UNIT_HOUR;
				case MINUTE :
					return OcsConstant.CYCLE_UNIT_MINUTE;
				case SECOND:
					return OcsConstant.CYCLE_UNIT_SECOND;
				case UNDEFINED:
				default:
					return OcsConstant.CYCLE_UNIT_UNDEFINED;
			}
		};
		
		/**
		 * 
		 * @param cycleUnit
		 * @return UserRequestType CYCLE_UNIT
		 */
		public static CYCLE_UNIT toCycleUnit(char cycleUnit) {
			switch (cycleUnit) {
				case CYCLE_UNIT_DAY:
					return CYCLE_UNIT.DAY;
				case CYCLE_UNIT_HOUR:
					return CYCLE_UNIT.HOUR;
				case CYCLE_UNIT_MINUTE:
					return CYCLE_UNIT.MINUTE;
				case CYCLE_UNIT_SECOND:
					return CYCLE_UNIT.SECOND;
				case CYCLE_UNIT_UNDEFINED:
				default:
					return CYCLE_UNIT.UNDEFINED;
			}
		}
	};
	
	public static enum TOUR_TYPE {NODE, PAIR, CARRIERLOC, UNDEFINED;
		/**
		 * 
		 * @return TourType char
		 */
		public char toChar() {
			switch(this) {
				case NODE :
					return OcsConstant.TOUR_TYPE_NODE;
				case PAIR :
					return OcsConstant.TOUR_TYPE_PAIR;
				case CARRIERLOC :
					return OcsConstant.TOUR_TYPE_CARRIERLOC;
				case UNDEFINED:
				default:
					return OcsConstant.TOUR_TYPE_UNDEFINED;
			}
		};
		
		/**
		 * 
		 * @param tourType
		 * @return TourType TOUR_TYPE
		 */
		public static TOUR_TYPE toTourType(char tourType) {
			switch (tourType) {
				case TOUR_TYPE_NODE:
					return TOUR_TYPE.NODE;
				case TOUR_TYPE_PAIR:
					return TOUR_TYPE.PAIR;
				case TOUR_TYPE_CARRIERLOC:
					return TOUR_TYPE.CARRIERLOC;
				case TOUR_TYPE_UNDEFINED:
				default:
					return TOUR_TYPE.UNDEFINED;
			}
		}
	};
	
	// 2015.02.13 by MYM : 장애 지역 우회 기능
	public static String DETOUR_NONE = "NONE";
	public static String DETOUR_NODE_DISABLED = "NODE_DISABLED";
	public static String DETOUR_LINK_DISABLED = "LINK_DISABLED";
	public static String DETOUR_VEHICLE_MANUAL = "VEHICLE_MANUAL";
	public static String DETOUR_VEHICLE_ERROR = "VEHICLE_ERROR";
	public static String DETOUR_VEHICLE_COMMFAIL = "VEHICLE_COMMFAIL";
	public static String DETOUR_VEHICLE_NOTRESPOND = "VEHICLE_NOTRESPOND";
	public static String DETOUR_HID_DOWN = "HID_DOWN";
	public static String DETOUR_HID_CAPACITY_FULL = "HID_CAPACITY_FULL";
	public static int DETOUR_NONE_HASHCODE = 2402104;
	public static int DETOUR_NODE_DISABLED_HASHCODE = -1267050471;
	public static int DETOUR_LINK_DISABLED_HASHCODE = 871501089;
	public static int DETOUR_VEHICLE_MANUAL_HASHCODE = -456624039;
	public static int DETOUR_VEHICLE_ERROR_HASHCODE = 1225318133;
	public static int DETOUR_VEHICLE_COMMFAIL_HASHCODE = -1472998403;
	public static int DETOUR_VEHICLE_NOTRESPOND_HASHCODE = -928376459;
	public static int DETOUR_HID_DOWN_HASHCODE = -966794242;
	public static int DETOUR_HID_CAPACITY_FULL_HASHCODE = 106787608;
	public static enum DETOUR_REASON {NONE, NODE_DISABLED, LINK_DISABLED, VEHICLE_MANUAL, VEHICLE_ERROR, VEHICLE_COMMFAIL, VEHICLE_NOTRESPOND, HID_DOWN, HID_CAPACITY_FULL;
		/**
		 * 
		 * @return
		 */
		public String toConstString() {
			switch (this) {
			case NONE :
				return OcsConstant.DETOUR_NONE;
			case NODE_DISABLED :
				return OcsConstant.DETOUR_NODE_DISABLED;
			case LINK_DISABLED :
				return OcsConstant.DETOUR_LINK_DISABLED;
			case VEHICLE_MANUAL:
				return OcsConstant.DETOUR_VEHICLE_MANUAL;
			case VEHICLE_ERROR:
				return OcsConstant.DETOUR_VEHICLE_ERROR;
			case VEHICLE_COMMFAIL:
				return OcsConstant.DETOUR_VEHICLE_COMMFAIL;
			case VEHICLE_NOTRESPOND:
				return OcsConstant.DETOUR_VEHICLE_NOTRESPOND;
			case HID_DOWN:
				return OcsConstant.DETOUR_HID_DOWN;
			case HID_CAPACITY_FULL:
				return OcsConstant.DETOUR_HID_CAPACITY_FULL;
			default:
				return OcsConstant.DETOUR_NONE;
			}
		}
		
		public static DETOUR_REASON toReasonType(String str) {
			if (str == null) {
				return DETOUR_REASON.NONE;
			}
			switch (str.hashCode()) {
				case DETOUR_NONE_HASHCODE:
					return DETOUR_REASON.NONE;
				case DETOUR_NODE_DISABLED_HASHCODE:
					return DETOUR_REASON.NODE_DISABLED;
				case DETOUR_LINK_DISABLED_HASHCODE:
					return DETOUR_REASON.LINK_DISABLED;
				case DETOUR_VEHICLE_MANUAL_HASHCODE:
					return DETOUR_REASON.VEHICLE_MANUAL;
				case DETOUR_VEHICLE_ERROR_HASHCODE:
					return DETOUR_REASON.VEHICLE_ERROR;
				case DETOUR_VEHICLE_COMMFAIL_HASHCODE:
					return DETOUR_REASON.VEHICLE_COMMFAIL;
				case DETOUR_VEHICLE_NOTRESPOND_HASHCODE:
					return DETOUR_REASON.VEHICLE_NOTRESPOND;
				case DETOUR_HID_DOWN_HASHCODE:
					return DETOUR_REASON.HID_DOWN;
				case DETOUR_HID_CAPACITY_FULL_HASHCODE:
					return DETOUR_REASON.HID_CAPACITY_FULL;
				default:
					return DETOUR_REASON.NONE;
			}
		}
	};
	
	public static String DETOUR_LEVEL0 = "L0";
	public static String DETOUR_LEVEL1 = "L1";
	public static String DETOUR_LEVEL2 = "L2";
	public static int DETOUR_LEVEL0_HASHCODE = 2404;
	public static int DETOUR_LEVEL1_HASHCODE = 2405;
	public static int DETOUR_LEVEL2_HASHCODE = 2406;
	public static enum DETOUR_CONTROL_LEVEL {LEVEL0, LEVEL1, LEVEL2;
		/**
		 * 
		 * @return
		 */
		public String toConstString() {
			switch (this) {
			case LEVEL0 :
				return OcsConstant.DETOUR_LEVEL0;
			case LEVEL1 :
				return OcsConstant.DETOUR_LEVEL1;
			case LEVEL2 :
				return OcsConstant.DETOUR_LEVEL2;
			default:
				return OcsConstant.DETOUR_LEVEL0;
			}
		}
		
		public static DETOUR_CONTROL_LEVEL toReasonType(String str) {
			if (str == null) {
				return DETOUR_CONTROL_LEVEL.LEVEL0;
			}
			switch (str.hashCode()) {
			case DETOUR_LEVEL0_HASHCODE:
				return DETOUR_CONTROL_LEVEL.LEVEL0;
			case DETOUR_LEVEL1_HASHCODE:
				return DETOUR_CONTROL_LEVEL.LEVEL1;
			case DETOUR_LEVEL2_HASHCODE:
				return DETOUR_CONTROL_LEVEL.LEVEL2;
			default:
				return DETOUR_CONTROL_LEVEL.LEVEL0;
			}
		}
	};
}
