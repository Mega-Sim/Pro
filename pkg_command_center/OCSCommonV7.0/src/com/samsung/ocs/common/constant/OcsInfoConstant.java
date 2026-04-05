package com.samsung.ocs.common.constant;

/**
 * OcsInfoConstant Interface, OCS 3.0 for Unified FAB
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

public interface OcsInfoConstant {
	
	public static String YES = "YES";
	public static String NO = "NO";
	public static String DONE = "DONE";
	
	public static int YES_HASHCODE = 87751;
	public static int NO_HASHCODE = 2497;
	public static int DONE_HASHCODE = 2104194;
	
	public static String NEARBY = "NEARBY";
	public static String NORMAL = "NORMAL";

	// 2012.01.11 by KYK
	public static String INSTALLED = "INSTALLED";
	public static String NOT_INSTALLED = "NOT_INSTALLED";	
	
	//OCSCONTROL
	public static String PAUSE = "PAUSE";
	public static String RESUME = "RESUME";
	
	// TSCSTATUS
	public static String TSC_NONE = "TSC_NONE";
	public static String TSC_PAUSING = "TSC_PAUSING";
	public static String TSC_PAUSED = "TSC_PAUSED";
	public static String TSC_AUTO = "TSC_AUTO";
	
	public static String MANUAL = "MANUAL";
	public static String AUTO = "AUTO";
	
	public static String CONTROLLABLE = "CONTROLLABLE";
	
	public static String DEFAULT_MISMATCH_UNLOAD_APPLIED_PORT = "STOCKERPORT";
	public static String DEFAULT_AVLOADAPPLIED_PORT = "EQPORT/STOCKERPORT/LOADERPORT";
	public static String DEFAULT_AVUNLOADAPPLIED_PORT = "EQPORT";
	public static String DEFAULT_RFREAD_DEVICE = "STBPORT/UTBPORT";
	public static String DEFAULT_YIELDSEARCH_RULE = "YSR4";
	
	public static String UNLOADED_VHL = "UNLOADED_VHL";
	public static String UNLOADING_VHL = "UNLOADING_VHL";
	public static String UNLOAD_ASSIGNED_VHL = "UNLOAD_ASSIGNED_VHL";
	public static String MANUAL_VHL = "MANUAL_VHL";
	public static String REMOVE_VHL = "REMOVE_VHL";
	public static String PATHSEARCH_FAIL = "PATHSEARCH_FAIL";
	public static String UNKNOWN = "UNKNOWN";
	public static int UNLOADED_VHL_HASHCODE = 429093881;
	public static int UNLOADING_VHL_HASHCODE = -63535618;
	public static int UNLOAD_ASSIGNED_VHL_HASHCODE = 730631977;
	public static int MANUAL_VHL_HASHCODE = -909442079;
	public static int REMOVE_VHL_HASHCODE = 1584956831;
	public static int PATHSEARCH_FAIL_HASHCODE = 1019347472;
	public static int UNKNOWN_HASHCODE = 433141802;
	
	public static String NULL = "";
	public static String STT = "STT";
	public static String LWT = "LWT";
	public static String HPF = "HPF";
	public static String HYBRID = "HYBRID";
	public static String HYBRID2 = "HYBRID2";
	public static String HYBRID3 = "HYBRID3";
	
	public static String NONE = "NONE";
	public static String NODE_PENALTY = "NODE_PENALTY";
	public static String VEHICLE_PENALTY = "VEHICLE_PENALTY";
	
	public static String COORD = "COORD";
	public static String DISTANCE = "DISTANCE";
	public static int COORD_HASHCODE = 64307925;
	public static int DISTANCE_HASHCODE = 1071086581;
	
	// 2015.05.27 by MYM : TRAFFIC_UPDATE_RULE
	public static String PULL = "PULL";
	public static String PUSH = "PUSH";
	public static int PULL_HASHCODE = 2467397;
	public static int PUSH_HASHCODE = 2467610;
	
	// 2015.03.17 by KYK [Job Reservation Option]
	public static String JR0 = "JR0";
	public static String JR1 = "JR1";
	public static String JR2 = "JR2";
	
	public static int JR0_HASHCODE = 73704;
	public static int JR1_HASHCODE = 73705;
	public static int JR2_HASHCODE = 73706;

	public static int NULL_HASHCODE = 0;
	public static int STT_HASHCODE = 82451;
	public static int LWT_HASHCODE = 75817;
	public static int HPF_HASHCODE = 71742;
	public static int HYBRID_HASHCODE = 2145539580;
	public static int HYBRID2_HASHCODE = 2087217590;
	public static int HYBRID3_HASHCODE = 2087217591;
	
	public static int NONE_HASHCODE = 2402104;
	public static int NODE_PENALTY_HASHCODE = -1286475988;
	public static int VEHICLE_PENALTY_HASHCODE = 1505999414;
	
	// OCS_CONTROL_STATE
	public static int PAUSE_HASHCODE = 75902422;
	public static int RESUME_HASHCODE = -1881097171;
	
	// TSC_STATE
	public static int TSC_AUTO_HASHCODE = -1271517334;
	public static int TSC_PAUSING_HASHCODE = 1694839160;
	public static int TSC_PAUSED_HASHCODE = -1746443223;

	// 2013.08.09 by KYK
//	public static String VEHICLECOMM_V1 = "V1";
//	public static String VEHICLECOMM_V7 = "V7";
//	public static int VEHICLECOMM_V1_HASHCODE = 2715;
//	public static int VEHICLECOMM_V7_HASHCODE = 2721;
	public static String VEHICLECOMM_CHAR = "CHAR";
	public static String VEHICLECOMM_BYTE = "BYTE";
	public static int VEHICLECOMM_CHAR_HASHCODE = 2067286;
	public static int VEHICLECOMM_BYTE_HASHCODE = 2054408;
	
	// 2013.04.05 by KYK
	public static String NEARBY_V3 = "V3";
	public static String NEARBY_V7 = "V7";
	public static int NEARBY_V3_HASHCODE = 2717;
	public static int NEARBY_V7_HASHCODE = 2721;
	// 2013.04.19 by KYK
	public static String DQRANGE = "DQRANGE";
	public static String FIFO_DQ1 = "FIFO_DQ1";
	public static String FIFO_DQ2 = "FIFO_DQ2";
	public static int DQRANGE_HASHCODE = -1677555536;
	public static int FIFO_DQ1_HASHCODE = -114172079;
	public static int FIFO_DQ2_HASHCODE = -114172078;	
	
	public static int PRIORITY_HASHCODE = -382834268;
	public static int WAITINGTIME_HASHCODE = -1667920262;
	public static int EQPRIORITY_HASHCODE = -1432287664;

	/**
	 * 2015.03.17 by KYK [Job Reservation Option]
	 */
	public static enum JOB_RESERVATION_OPTION {NONE, JR0, JR1, JR2;
	
	public String toConstString() {
		switch (this) {
		case NONE:
			return OcsInfoConstant.NONE;
		case JR0:
			return OcsInfoConstant.JR0;
		case JR1:
			return OcsInfoConstant.JR1;
		case JR2:
			return OcsInfoConstant.JR2;
		default:
			return OcsInfoConstant.JR1;
		}
	};
	 
	public static JOB_RESERVATION_OPTION toJobReservationOption(String str) {
		switch (str.hashCode()) {
		case JR0_HASHCODE:
			return JOB_RESERVATION_OPTION.JR0;
		case JR1_HASHCODE:
			return JOB_RESERVATION_OPTION.JR1;
		case JR2_HASHCODE:
			return JOB_RESERVATION_OPTION.JR2;
		default:
			return JOB_RESERVATION_OPTION.JR1;
		}
	}
	};
	
	public static enum DISPATCHING_RULES {STT, LWT, HPF, HYBRID, HYBRID2, HYBRID3;
		/**
		 * 
		 * @return dispatchingRule String
		 */
		public String toConstString() {
			switch (this) {
				case STT:
					return OcsInfoConstant.STT;
				case LWT:
					return OcsInfoConstant.LWT;
				case HPF:
					return OcsInfoConstant.HPF;
				case HYBRID2:
					return OcsInfoConstant.HYBRID2;
				case HYBRID3:
					return OcsInfoConstant.HYBRID3;
				case HYBRID:
				default:
					return OcsInfoConstant.HYBRID;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return dispatchingRule DISPATCHING_RULES
		 */
		public static DISPATCHING_RULES toDispatchingRules(String str) {
			switch (str.hashCode()) {
				case OcsInfoConstant.STT_HASHCODE:
					return DISPATCHING_RULES.STT;
				case OcsInfoConstant.LWT_HASHCODE:
					return DISPATCHING_RULES.LWT;
				case OcsInfoConstant.HPF_HASHCODE:
					return DISPATCHING_RULES.HPF;
				case OcsInfoConstant.HYBRID2_HASHCODE:
					return DISPATCHING_RULES.HYBRID2;
				case OcsInfoConstant.HYBRID3_HASHCODE:
					return DISPATCHING_RULES.HYBRID3;
				case OcsInfoConstant.HYBRID_HASHCODE:
				default:
					return DISPATCHING_RULES.HYBRID;
			}
		}
	};
	
	public static enum OCS_CONTROL_STATE {PAUSE, RESUME, NULL;
		/**
		 * 
		 * @return ocsControlState String
		 */
		public String toConstString() {
			switch (this) {
				case PAUSE:
					return OcsInfoConstant.PAUSE;
				case RESUME:
					return OcsInfoConstant.RESUME;
				case NULL:
				default:
					return OcsInfoConstant.NULL;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return ocsControlState OCS_CONTROL_STATE
		 */
		public static OCS_CONTROL_STATE toOcsControlState(String str) {
			switch (str.hashCode()) {
				case OcsInfoConstant.PAUSE_HASHCODE:
					return OCS_CONTROL_STATE.PAUSE;
				case OcsInfoConstant.RESUME_HASHCODE:
					return OCS_CONTROL_STATE.RESUME;
				case OcsInfoConstant.NULL_HASHCODE:
				default:
					return OCS_CONTROL_STATE.NULL;
			}
		}
	};
	
	public static enum TSC_STATE {TSC_AUTO, TSC_PAUSING, TSC_PAUSED, NULL;
		/**
		 * 
		 * @return tscState String
		 */
		public String toConstString() {
			switch (this) {
				case TSC_AUTO:
					return OcsInfoConstant.TSC_AUTO;
				case TSC_PAUSING:
					return OcsInfoConstant.TSC_PAUSING;
				case TSC_PAUSED:
					return OcsInfoConstant.TSC_PAUSED;
				case NULL:
				default:
					return OcsInfoConstant.NULL;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return tscState TSC_STATE
		 */
		public static TSC_STATE toTscState(String str) {
			switch (str.hashCode()) {
				case OcsInfoConstant.TSC_AUTO_HASHCODE:
					return TSC_STATE.TSC_AUTO;
				case OcsInfoConstant.TSC_PAUSING_HASHCODE:
					return TSC_STATE.TSC_PAUSING;
				case OcsInfoConstant.TSC_PAUSED_HASHCODE:
					return TSC_STATE.TSC_PAUSED;
				case OcsInfoConstant.NULL_HASHCODE:
				default:
					return TSC_STATE.NULL;
			}
		}
	};
	
	public static enum LOCALGROUP_CLEAROPTION {UNLOADED_VHL, UNLOADING_VHL, UNLOAD_ASSIGNED_VHL, MANUAL_VHL, REMOVE_VHL, PATHSEARCH_FAIL, UNKNOWN;
		/**
		 * 
		 * @return localGroupClearOption String
		 */
		public String toConstString() {
			switch (this) {
				case UNLOADED_VHL:
					return OcsInfoConstant.UNLOADED_VHL;
				case UNLOADING_VHL:
					return OcsInfoConstant.UNLOADING_VHL;
				case UNLOAD_ASSIGNED_VHL:
					return OcsInfoConstant.UNLOAD_ASSIGNED_VHL;
				case MANUAL_VHL:
					return OcsInfoConstant.MANUAL_VHL;
				case REMOVE_VHL:
					return OcsInfoConstant.REMOVE_VHL;
				case PATHSEARCH_FAIL:
					return OcsInfoConstant.PATHSEARCH_FAIL;
				case UNKNOWN:
				default:
					return OcsConstant.UNKNOWN;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return localGroupClearOption LOCALGROUP_CLEAROPTION
		 */
		public static LOCALGROUP_CLEAROPTION toLocalGroupClearOption(String str) {
			switch (str.hashCode()) {
				case UNLOADED_VHL_HASHCODE:
					return LOCALGROUP_CLEAROPTION.UNLOADED_VHL;
				case UNLOADING_VHL_HASHCODE:
					return LOCALGROUP_CLEAROPTION.UNLOADING_VHL;
				case UNLOAD_ASSIGNED_VHL_HASHCODE:
					return LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL;
				case MANUAL_VHL_HASHCODE:
					return LOCALGROUP_CLEAROPTION.MANUAL_VHL;
				case REMOVE_VHL_HASHCODE:
					return LOCALGROUP_CLEAROPTION.REMOVE_VHL;
				case PATHSEARCH_FAIL_HASHCODE:
					return LOCALGROUP_CLEAROPTION.PATHSEARCH_FAIL;
				case UNKNOWN_HASHCODE:
				default:
					return LOCALGROUP_CLEAROPTION.UNKNOWN;
			}
		}
	};
	
	public static enum RUNTIME_UPDATE {YES, DONE, NO, UNKNOWN;
		/**
		 * 
		 * @return runtimeUpdate String
		 */
		public String toConstString() {
			switch (this) {
				case YES:
					return OcsInfoConstant.YES;
				case DONE:
					return OcsInfoConstant.DONE;
				case NO:
					return OcsInfoConstant.NO;
				case UNKNOWN:
				default:
					return OcsInfoConstant.UNKNOWN;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return runtimeUpdate RUNTIME_UPDATE
		 */
		public static RUNTIME_UPDATE toRuntimeUpdate(String str) {
			switch (str.hashCode()) {
				case OcsInfoConstant.YES_HASHCODE:
					return RUNTIME_UPDATE.YES;
				case OcsInfoConstant.DONE_HASHCODE:
					return RUNTIME_UPDATE.DONE;
				case OcsInfoConstant.NO_HASHCODE:
					return RUNTIME_UPDATE.NO;
				case OcsInfoConstant.UNKNOWN_HASHCODE:
				default:
					return RUNTIME_UPDATE.UNKNOWN;
			}
		}
	};
	
	public static enum COSTSEARCH_OPTION {NONE, NODE_PENALTY, VEHICLE_PENALTY, HYBRID;
		/**
		 * 
		 * @return CostSearchOption String
		 */
		public String toConstString() {
			switch (this) {
				case NODE_PENALTY:
					return OcsInfoConstant.NODE_PENALTY;
				case VEHICLE_PENALTY:
					return OcsInfoConstant.VEHICLE_PENALTY;
				case HYBRID:
					return OcsInfoConstant.HYBRID;
				case NONE:
				default:
					return OcsInfoConstant.NONE;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return CostSearchOption COST_SEARCH_OPTION
		 */
		public static COSTSEARCH_OPTION toCostSearchOption(String str) {
			switch (str.hashCode()) {
				case OcsInfoConstant.NODE_PENALTY_HASHCODE:
					return COSTSEARCH_OPTION.NODE_PENALTY;
				case OcsInfoConstant.VEHICLE_PENALTY_HASHCODE:
					return COSTSEARCH_OPTION.VEHICLE_PENALTY;
				case OcsInfoConstant.HYBRID_HASHCODE:
					return COSTSEARCH_OPTION.HYBRID;
				case OcsInfoConstant.NONE_HASHCODE:
				default:
					return COSTSEARCH_OPTION.NONE;
			}
		}
	};
	
	public static enum VEHICLECOMM_TYPE {
//		NONE, VEHICLECOMM_V1, VEHICLECOMM_V7;
		NONE, VEHICLECOMM_CHAR, VEHICLECOMM_BYTE;
		
		/**
		 * 
		 * @return
		 */
		public String toConstString() {
			switch (this) {
				case VEHICLECOMM_CHAR:
					return OcsInfoConstant.VEHICLECOMM_CHAR;
				case VEHICLECOMM_BYTE:
					return OcsInfoConstant.VEHICLECOMM_BYTE;
				default:
					return OcsInfoConstant.NONE;
			}
		};
		
		/**
		 * 
		 * @param str
		 * @return
		 */
		public static VEHICLECOMM_TYPE toVehicleCommType(String str) {
			switch (str.hashCode()) {
				case OcsInfoConstant.VEHICLECOMM_CHAR_HASHCODE:
					return VEHICLECOMM_TYPE.VEHICLECOMM_CHAR;
				case OcsInfoConstant.VEHICLECOMM_BYTE_HASHCODE:
					return VEHICLECOMM_TYPE.VEHICLECOMM_BYTE;
				case OcsInfoConstant.NONE_HASHCODE:
				default:
					return VEHICLECOMM_TYPE.NONE;
			}
		}
	}
	
	// 2013.04.05 by KYK
	public static enum NEARBY_TYPE {NONE, NEARBY_V3, NEARBY_V7;	

		public String toConstString() {
			switch (this) {
			case NEARBY_V3:
				return OcsInfoConstant.NEARBY_V3;
			case NEARBY_V7:
				return OcsInfoConstant.NEARBY_V7;
			default:
				return OcsInfoConstant.NONE;				
			}
		};

		public static NEARBY_TYPE toNearbyType(String str) {
			switch (str.hashCode()) {
			case OcsInfoConstant.NEARBY_V3_HASHCODE:
				return NEARBY_TYPE.NEARBY_V3;
			case OcsInfoConstant.NEARBY_V7_HASHCODE:
				return NEARBY_TYPE.NEARBY_V7;
			case OcsInfoConstant.NONE_HASHCODE:
			default:
				return NEARBY_TYPE.NONE;		
			}
		}
	};
	
	// 2013.04.19 by KYK
	public static enum FLOW_CONTROL_TYPE {DQRANGE, FIFO_DQ1, FIFO_DQ2, NONE;	
	
	public String toConstString() {
		switch (this) {
		case DQRANGE:
			return OcsInfoConstant.DQRANGE;
		case FIFO_DQ1:
			return OcsInfoConstant.FIFO_DQ1;
		case FIFO_DQ2:
			return OcsInfoConstant.FIFO_DQ2;
		default:
			return OcsInfoConstant.NONE;				
		}
	};
	
	public static FLOW_CONTROL_TYPE toFlowControlType(String str) {
		switch (str.hashCode()) {
		case OcsInfoConstant.DQRANGE_HASHCODE:
			return FLOW_CONTROL_TYPE.DQRANGE;
		case OcsInfoConstant.FIFO_DQ1_HASHCODE:
			return FLOW_CONTROL_TYPE.FIFO_DQ1;
		case OcsInfoConstant.FIFO_DQ2_HASHCODE:
			return FLOW_CONTROL_TYPE.FIFO_DQ2;
		case OcsInfoConstant.NONE_HASHCODE:
		default:
			return FLOW_CONTROL_TYPE.NONE;		
		}
	}
	};
	
	public static enum SYSTEM_COLLISION_CRITERION {COORD, DISTANCE;
	/**
	 * 
	 * @return
	 */
	public String toConstString() {
		switch (this) {
		case COORD:
			return OcsInfoConstant.COORD;
		case DISTANCE:
			return OcsInfoConstant.DISTANCE;
		default:
			return OcsInfoConstant.DISTANCE;
		}
	};
	
	/**
	 * 
	 * @param str
	 * @return
	 */
	public static SYSTEM_COLLISION_CRITERION toSystemCollisionCriterion(String str) {
		switch (str.hashCode()) {
		case OcsInfoConstant.COORD_HASHCODE:
			return SYSTEM_COLLISION_CRITERION.COORD;
		case OcsInfoConstant.DISTANCE_HASHCODE:
			return SYSTEM_COLLISION_CRITERION.DISTANCE;
		default:
			return SYSTEM_COLLISION_CRITERION.DISTANCE;
		}
	}
	};
	
	
	public static enum TRAFFIC_UPDATE_RULE {PULL, PUSH, HYBRID;
	/**
	 * 
	 * @return
	 */
	public String toConstString() {
		switch (this) {
		case PULL:
			return OcsInfoConstant.PULL;
		case PUSH:
			return OcsInfoConstant.PUSH;
		case HYBRID:
			return OcsInfoConstant.HYBRID;
		default:
			return OcsInfoConstant.PUSH;
		}
	};
	
	/**
	 * 
	 * @param str
	 * @return
	 */
	public static TRAFFIC_UPDATE_RULE toTrafficUpdateRule(String str) {
		switch (str.hashCode()) {
		case OcsInfoConstant.PULL_HASHCODE:
			return TRAFFIC_UPDATE_RULE.PULL;
		case OcsInfoConstant.PUSH_HASHCODE:
			return TRAFFIC_UPDATE_RULE.PUSH;
		case OcsInfoConstant.HYBRID_HASHCODE:
			return TRAFFIC_UPDATE_RULE.HYBRID;
		default:
			return TRAFFIC_UPDATE_RULE.PUSH;
		}
	}
	};
	
	// 2015.10.01 by KYK
	public static enum PRIORJOB_DISPATCHING_RULE { PRIORITY, WAITINGTIME, EQPRIORITY, HYBRID;
	
	public static PRIORJOB_DISPATCHING_RULE toPriorJobDispatchingRule(String str) {
		switch (str.hashCode()) {
		case EQPRIORITY_HASHCODE:
			return PRIORJOB_DISPATCHING_RULE.EQPRIORITY;
		case PRIORITY_HASHCODE:
			return PRIORJOB_DISPATCHING_RULE.PRIORITY;
		case WAITINGTIME_HASHCODE:
			return PRIORJOB_DISPATCHING_RULE.WAITINGTIME;
		case HYBRID_HASHCODE:
			return PRIORJOB_DISPATCHING_RULE.HYBRID;			
		default:
			return PRIORJOB_DISPATCHING_RULE.EQPRIORITY;
		}
	};
	}

}
