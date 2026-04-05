package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsInfoConstant.COSTSEARCH_OPTION;
import com.samsung.ocs.common.constant.OcsInfoConstant.DISPATCHING_RULES;
import com.samsung.ocs.common.constant.OcsInfoConstant.FLOW_CONTROL_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.JOB_RESERVATION_OPTION;
import com.samsung.ocs.common.constant.OcsInfoConstant.LOCALGROUP_CLEAROPTION;
import com.samsung.ocs.common.constant.OcsInfoConstant.NEARBY_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.OCS_CONTROL_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.PRIORJOB_DISPATCHING_RULE;
import com.samsung.ocs.common.constant.OcsInfoConstant.RUNTIME_UPDATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.SYSTEM_COLLISION_CRITERION;
import com.samsung.ocs.common.constant.OcsInfoConstant.TRAFFIC_UPDATE_RULE;
import com.samsung.ocs.common.constant.OcsInfoConstant.TSC_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.VEHICLECOMM_TYPE;
import com.samsung.ocs.manager.impl.model.OCSInfo;

/**
 * OCSInfoManager Class, OCS 3.0 for Unified FAB
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

public class OCSInfoManager extends AbstractManager {
	private static OCSInfoManager manager = null;
	private final String NAME = "NAME";
	private final String TYPE = "TYPE";
	private final String VALUE = "VALUE";
	private final String CONSTRAINT = "CONSTRAINT";
	private String currDBTimeStr = "";
	private Map<String, String> updateOCSInfo;
	
	private static final String AUTO = "AUTO";
	private static final String YES = "YES";
	private static final String NO = "NO";
	private static final String NOT_INSTALLED = "NOT_INSTALLED";
	private static final String NEARBY = "NEARBY";
	
	private static final String IBSEM_UPDATE = "IBSEMUPDATE";
	private static final String JOBASSIGN_UPDATE = "JOBASSIGNUPDATE";
	private static final String OPERATION_UPDATE = "OPERATIONUPDATE";
	private static final String OPERATION_PORT_UPDATE = "OPERATIONPORTUPDATE";
	private static final String STBC_UPDATE = "STBCUPDATE";
	private static final String OPTIMIZER_UPDATE = "OPTIMIZERUPDATE";
	private static final String LONGRUN_UPDATE = "LONGRUNUPDATE";
	private static final String USERBLOCK_UPDATE = "USERBLOCK_UPDATE";
	
	private static final String DEFAULT_MISMATCH_UNLOAD_APPLIED_PORT = "STOCKERPORT";

	private static final String DEFAULT_RFREAD_DEVICE = "STBPORT/UTBPORT";
	private static final String DEFAULT_YIELDSEARCH_RULE = "YSR4";
	// 2013.01.04 by KYK
	private static final String STBC = "STBC";
	private static final String OCS = "OCS";
	private static final String MCS = "MCS";	
	
	// Parameters' HashCodes
	private static final int ABORT_CHECKTIME_HASHCODE = 1567495494;
	private static final int AREA_BALANCING_LIMIT_HASHCODE = -1334808413;
	private static final int AREA_BALANCING_INTERVAL_HASHCODE = 1230584509;
	private static final int AREA_BALANCING_USAGE_HASHCODE = -1326210423;
	private static final int AUTO_RETRY_USAGE_HASHCODE = 782381690;
	private static final int BLOCK_RESET_TIMEOUT_HASHCODE = 431284703;
	private static final int BIDIRECTIONALSTB_HASHCODE = 1925116414;
	private static final int PATROL_CONTROL_USAGE_HASHCODE = 1160325964;
	private static final int COLLISIONNODE_CHECK_USAGE_HASHCODE = 2023001023;
	private static final int COMMFAIL_CHECKTIME_HASHCODE = 1394139616;
	private static final int CONGESTION_COUNT_THRESHOLD_HASHCODE = 553882851;
	private static final int CONGESTION_INDEX_THRESHOLD_HASHCODE = -1846881050;
	private static final int CONGESTION_PENALTY_RANGELIMIT_HASHCODE = -1820766900;
	private static final int CONGESTION_PENALTY_THRESHOLD_HASHCODE = -2100226595;
	private static final int CONGESTION_TIME_THRESHOLD_HASHCODE = -901688207;
	private static final int CONVEYOR_HOTPRORITY_USAGE_HASHCODE = 1440018610;
	private static final int COST_SEARCH_OPTION_HASHCODE = 1831911930;
	private static final int DEADLOCK_DETECTED_TIMEOUT_HASHCODE = 932943668;
	private static final int DELAY_LIMIT_JOBASSIGN_HASHCODE = 118580300;
	private static final int DELAY_LIMIT_OPERATION_HASHCODE = 251698887;
	private static final int DELAY_LIMIT_STBC_HASHCODE = 1678464226;
	private static final int DISPATCHING_RULE_HASHCODE = -72527917;
	private static final int DRIVE_FAIL_LIMITTIME_HASHCODE = 533651580;
	private static final int DRIVE_LIMIT_TIME_HASHCODE = -1649049432;
	private static final int DRIVE_METHOD_HASHCODE = 1557763414;
	private static final int DRIVINGQUEUE_CURVE_DISTANCE_HASHCODE = 843970714;
	private static final int DRIVINGQUEUE_LINE_DISTANCE_HASHCODE = -1909186037;
	private static final int DYNAMIC_ROUTING_USAGE_HASHCODE = -50275192;
	private static final int DYNAMICROUTING_HOLD_TIMEOUT_HASHCODE = -939221159;
	private static final int EMULATORMODE_HASHCODE = 1133331000;
	private static final int ESTABLISH_COMMUNICATIONS_TIMEOUT_HASHCODE = 290845765;
	private static final int FAILUREOHT_DETOURSEARCH_USAGE_HASHCODE = 1194957841;
	private static final int FORMATTED_LOG_USAGE_HASHCODE = 1910099139;
	private static final int GOMODE_CHECKTIME_HASHCODE = 1716294401;
	private static final int GOMODE_CARRIERSTATUS_CHECK_USAGE_HASHCODE = -1177900351;
	private static final int GOMODE_VHLDETECTED_CHECKTIME_HASHCODE = -1283125026;
	private static final int GOMODE_VHLDETECTED_RESET_TIMEOUT_HASHCODE = 857331418;
	private static final int HID_CONTROL_USAGE_HASHCODE = -343148509;
	private static final int HID_LIMIT_OVER_PASS_HASHCODE = 144888860;
	private static final int HISTORY_DELETE_CHECK_PERIOD_HASHCODE = -445833535;
	private static final int EVENT_HISTORY_DELETE_USAGE_HASHCODE = 1394105469;
	private static final int TRCOM_HISTORY_DELETE_USAGE_HASHCODE = 43608180;
	private static final int VHLERROR_HISTORY_DELETE_USAGE_HASHCODE = -347906775;
	private static final int IBSEM_HISTORY_DELETE_USAGE_HASHCODE = -2056849547;
	private static final int STBC_HISTORY_DELETE_USAGE_HASHCODE = -1579755115;
	private static final int HISTORY_HOLDINGPERIOD_HASHCODE = -1400599143;
	private static final int IBSEM_USAGE_HASHCODE = 1524568639;
	private static final int IBSEM_UPDATE_HASHCODE = 14301131;
	private static final int IDREADER_ON_VEHICLE_HASHCODE = -1425543699;
	private static final int JOBASSIGN_DETAILRESULT_USAGE_HASHCODE = -1173182973;
	private static final int JOBASSIGN_UPDATE_HASHCODE = 1667665557;
	private static final int JOBASSIGN_SEARCH_LIMIT_HASHCODE = 113761495;
	private static final int JOBASSIGN_THRESHOLD_HASHCODE = -5207400;
	private static final int JOBASSIGN_LOCATE_THRESHOLD_HASHCODE = -1207133007;
	private static final int JOBASSIGN_PRIORITY_THRESHOLD_HASHCODE = -1037710397;
	private static final int JOBASSIGN_PRIORITY_WEIGHT_HASHCODE = 230445568;
	private static final int JOBASSIGN_URGENT_THRESHOLD_HASHCODE = -2139127328;
	private static final int JOBASSIGN_WAITINGTIME_THRESHOLD_HASHCODE = -1079855213;
	private static final int JOBASSIGN_WAITINGTIME_WEIGHT_HASHCODE = 2101049392;
	private static final int LOADED_VEHICLE_PENALTY_HASHCODE = -88630494;
	private static final int LOCALOHT_CLEAR_OPTION_HASHCODE = -1878672330;
	private static final int LOCALOHT_USAGE_HASHCODE = -91007246;
	private static final int LONGRUN_UPDATE_HASHCODE = -64128008;
	private static final int LONGRUN_MOVE_USAGE_HASHCODE = 325740387;
	private static final int LONGRUN_TRANSFER_USAGE_HASHCODE = 1843755005;
	private static final int LOG_HOLDINGPERIOD_DEFAULT_HASHCODE = 390037547;
	private static final int LOG_HOLDINGPERIOD_JOBASSIGN_HASHCODE = -99324330;
	private static final int LOG_HOLDINGPERIOD_OPERATION_HASHCODE = 33794257;
	private static final int LOG_HOLDINGPERIOD_IBSEM_HASHCODE = -987230964;
	private static final int MAP_DISTANCE_USAGE_HASHCODE = 425393690;
	private static final int MAP_VEHICLESPEED_USAGE_HASHCODE = 2070378688;
	private static final int MISMATCH_RECOVERY_MODE_HASHCODE = -1248787620;
	private static final int MISMATCH_UNLOAD_APPLIED_PORT_HASHCODE = 1827899506;
	private static final int MISSED_CARRIER_CHECK_USAGE_HASHCODE = 549311711;
	private static final int MISSED_CARRIER_CHECK_SLEEP_HASHCODE = 547259925;
	private static final int NEXT_CMD_USAGE_HASHCODE = 50125776;
	private static final int OCSCONTROL_HASHCODE = 1108833118;
	private static final int OHT_COUNT_LIMIT_PER_HID_HASHCODE = -1616243003;
	private static final int OPERATION_UPDATE_HASHCODE = 270581968;
	private static final int OPERATION_PORT_UPDATE_HASHCODE = -514404079;
	private static final int OPTIMIZER_UPDATE_HASHCODE = -579313122;
	private static final int PARK_SEARCH_INTERVAL_HASHCODE = -199645401;
	private static final int PARK_SEARCH_LIMIT_HASHCODE = -840209287;
	private static final int PARK_NODE_USAGE_HASHCODE = 101763193;
	private static final int PORT_DUP_USAGE_HASHCODE = 935685347;
	private static final int PRIORJOB_CRITERIA_PRIORITY_HASHCODE = 526276088;
	private static final int PRIORJOB_CRITERIA_WAITINGTIME_HASHCODE = 1869894054;
	private static final int PRIORJOB_CRITERIA_TRANSFERTIME_HASHCODE = -1822185588;
	private static final int RAILDOWN_CHECK_USAGE_HASHCODE = 1377107679;
	private static final int REFINE_PORTDUP_TRCMD_HASHCODE = -169938487;
	private static final int REPATHSEARCH_HOLD_TIMEOUT_HASHCODE = 1131645728;
	private static final int RF_READ_DEVICE_HASHCODE = 1645567028;
	private static final int RFC_USAGE_HASHCODE = 876464722;
	private static final int RFC_VERIFYING_TIMEOUT_HASHCODE = -1961580549; // 2012.12.05 by KYK	
	private static final int SOCKET_RECONNECTION_TIMEOUT_HASHCODE = -1203835489;
	private static final int SOCKET_CLOSE_CHECKTIME_HASHCODE = -1155589822;
	private static final int STATION_USAGE_HASHCODE = -335063402; // 2013.02.28 by KYK
	private static final int STBC_USAGE_HASHCODE = 1117205599;
	private static final int STBC_UPDATE_HASHCODE = 270948779;	
	private static final int STBDATA_SAVE_USAGE_HASHCODE = -1308095533;
	private static final int STBDATA_SAVE_PERIOD_HASHCODE = -2051822161; // 2012.12.05 by KYK
	private static final int STBDATA_RECOVERY_OPTION_HASHCODE = -91018933; // 2012.12.05 by KYK	
	private static final int STEERING_READY_USAGE_HASHCODE = -757813001;
	private static final int SYSTEM_COLLISION_UPDATE_HASHCODE = -952608090;
	private static final int SYSTEMPAUSE_REQUEST_TIMEOUT_HASHCODE = -2034641863;
	private static final int TSCSTATUS_HASHCODE = -1088930154;
	private static final int UNLOADERROR_REPORT_USAGE_HASHCODE = 770643180;
	private static final int USER_PASSTHROUGH_USAGE_HASHCODE = -331626622;
	private static final int USERBLOCK_UPDATE_HASHCODE = 1418822694;
	private static final int VEHICLE_CURVE_SPEED_HASHCODE = 434169028;
	private static final int VEHICLE_LINE_SPEED_HASHCODE = 305853595;
	private static final int VEHICLE_LENGTH_HASHCODE = 681234674;
	private static final int VEHICLE_WIDTH_HASHCODE = 309338650;
	private static final int VEHICLE_PENALTY_ERROR_HASHCODE = -785385825;
	private static final int VEHICLE_PENALTY_GOING_HASHCODE = -783636847;
	private static final int VEHICLE_PENALTY_MANUAL_HASHCODE = 1636062703;
	private static final int VEHICLE_PENALTY_STOPPING_HASHCODE = 1872313437;
	private static final int VEHICLE_PENALTY_WORKING_HASHCODE = -132349432;
	private static final int VEHICLE_PENALTY_CLEANING_HASHCODE = 946853090;
	private static final int VEHICLETRAFFIC_LOG_USAGE_HASHCODE = 2019736376;
	private static final int VIBRATION_CONTROL_USAGE_HASHCODE = 2053496050;
	private static final int VIBRATION_MONITORING_TIMEOUT_HASHCODE = -2123775177;
	private static final int WORKMODE_CHECKTIME_HASHCODE = 1646447978;
	private static final int YIELD_MIN_LIMITTIME_HASHCODE = -190702135;
	private static final int YIELD_REQUEST_LIMITTIME_HASHCODE = 1277821638;
	private static final int YIELD_SEARCH_RULE_HASHCODE = 994951521;
	private static final int YIELD_SEARCH_USAGE_HASHCODE = 781426620;
	private static final int NEARBY_NORMAL_DRIVE_USAGE_HASHCODE = 799277268;
	private static final int STAGE_LOCATE_VEHICLE_COUNT_HASHCODE = -143826288;
	private static final int STAGE_LOCATE_USAGE_HASHCODE = -1759617515;
	private static final int DEADLOCKBREAK_NEARBYDRIVE_USAGE_HASHCODE = 1798438590; // 2014.09.13 by zzang9un : 근접제어 deadlock break on/off 
	private static final int RESENDCMD_FOR_ABNORMAL_REPLY_HASHCODE = 415896489;
	private static final int BLOCK_PREEMPTION_UPDATE_USAGE_HASHCODE = 1774232415;
	private static final int STB_REPORT_USAGE_HASHCODE = 1431350324; // 2015.02.25 by KBS : STBC 이상감지 on/off
	private static final int DETOUR_CONTROL_USAGE_HASHCODE = 315538585;
	private static final int DETOUR_PORTSERVICE_LIMIT_COUNT_HASHCODE = 2030414490;
	private static final int FOUPID_USAGE_HASHCODE = -767946111;	// 2014.01.02 by KBS : FoupID 사용모드 추가

	private static final int VEHICLECOMM_TYPE_HASHCODE = -427969791;
	// 2013.04.05 by KYK
	private static final int NEARBY_TYPE_HASHCODE = 1637308666;
	private static final int FLOW_CONTROL_TYPE_HASHCODE = 815454317;
	private static final int DRIVINGQUEUE_CONVERGE_LIMITTIME_HASHCODE = 374145245;
	private static final int DRIVINGQUEUE_DIVERGE_LIMITTIME_HASHCODE = 184424272;
	private static final int DRIVINGQUEUE_CURVE_LIMITTIME_HASHCODE = 1786278115;
	private static final int DRIVINGQUEUE_LINE_LIMITTIME_HASHCODE = -1957202542;
	private static final int HOIST_SPEED_LEVEL_HASHCODE = -1963054832 ;	
	private static final int SHIFT_SPEED_LEVEL_HASHCODE = 1594516719;
	// 2013.10.22 by KYK
	private static final int STATIONDATA_REVISION_HASHCODE = 1910363289;
	private static final int TEACHINGDATA_REVISION_HASHCODE = 1138583890;
	
	// 2015.02.07 by zzang9un : PassDoor Control Usage 사용 유무 파라미터 추가
	private static final int PASSDOOR_CONTROL_USAGE_HASHCODE = -1900873985;
	private static final int JOB_RESERVATION_OPTION_HASHCODE = 1684940874; // 2015.03.17 by KYK
	private static final int JOB_RESERVATION_LIMITTIME_HASHCODE = 690725107; // 2015.04.06 by KYK
	private static final int SYSTEM_COLLISION_CRITERION_HASHCODE = 34490244;
	private static final int RFC_ERROR_PORTOUTOFSERVICE_USAGE_HASHCODE = -674787814;	// 2012.03.08 by KYK
	private static final int BLOCK_ARRIVAL_REARRANGETIME_HASHCODE = -351052786;
	private static final int COMMFAIL_ALARMREPORT_USAGE_HASHCODE = -1069334894; // 2015.05.01 by KYK
	private static final int CARRIERTYPE_MISMATCH_USAGE_HASHCODE = 1197201821;
	private static final int CONSECUTIVE_YIELD_LIMITTIME_HASHCODE = -1929843743;
	private static final int TRAFFIC_UPDATE_USAGE_HASHCODE = 1669432749;
	private static final int TRAFFIC_UPDATE_RULE_HASHCODE = 1023596880;
	private static final int PRIORJOB_DISPATCHING_USAGE_HASHCODE = 1431736062; // 2015.10.01 by KYK
	private static final int PRIORJOB_DISPATCHING_RULE_HASHCODE = -1062280737;	
	private static final int PRIORJOB_PRIORITY_THRESHOLD_HASHCODE = 1939987068;
	private static final int PRIORJOB_WAITINGTIME_THRESHOLD_HASHCODE = -749206982;
	private static final int IGNORE_MATERIAL_DIFFERENCE_HASHCODE = 636556648;
	private static final int CARRIER_TYPE_USAGE_HASHCODE = 1927663619;
	private static final int OPERATION_MANAGER_LOGGING_USAGE_HASHCODE = -2102776233;
	private static final int PARK_COMMAND_EQOPTION_USAGE_HASHCODE = -123239572;
	private static final int STAGE_SOURCE_DUP_CANCEL_USAGE_HASHCODE = -1858445537;
	private static final int AREA_BALANCING_MANUAL_EXCLUDE_HASHCODE = 897176121; // 2022.12.14 by JJW : Area 내  Max VHL 초과시  Manual VHL 제외 여부 
	


	/**
	 * Constructor of OCSInfoManager class.
	 */
	private OCSInfoManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(OCSInfo.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		// 2021.02.25 by kw3711.kim : ThreadSafe Map으로 수정
		updateOCSInfo = new ConcurrentHashMap<String, String>();
		// 2012.04.18 by KYK
		init();
	}
	
	/**
	 * Constructor of OCSInfoManager class. (Singleton)
	 */
	public static synchronized OCSInfoManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new OCSInfoManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	@Override
	protected void init() {
		updateFromDB();
		isInitialized = true;
	}
	
	@Override
	protected boolean updateToDB() {
		updateOCSInfo();
		return true;
	}

	private static final String SELECT_SQL = "SELECT * FROM OCSINFO";

	@Override
	protected boolean updateFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			String name;
			OCSInfo ocsInfo;
			while (rs.next()) {
				name = rs.getString(NAME);
				ocsInfo = (OCSInfo) data.get(name);
				if (ocsInfo == null) {
					ocsInfo = (OCSInfo) vOType.newInstance();
					data.put(name, ocsInfo);
				}
				setOCSInfo(ocsInfo, rs);
				removeKeys.remove(name);
			}
			for (String rmKey : removeKeys) {
				data.remove(rmKey);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		currDBTimeStr = getCurrDBTimeFromDB();
		return result;
	}

	/**
	 * 
	 * @param ocsInfo
	 * @param rs
	 * @exception SQLException
	 */
	private void setOCSInfo(OCSInfo ocsInfo, ResultSet rs) throws SQLException {
		if (ocsInfo != null && rs != null) {
			ocsInfo.setName(getString(rs.getString(NAME)));
			ocsInfo.setValue(getString(rs.getString(VALUE)));
			ocsInfo.setType(getString(rs.getString(TYPE)));
			ocsInfo.setConstraint(getString(rs.getString(CONSTRAINT)));
			
			setOCSInfoParameters(getString(rs.getString(NAME)), getString(rs.getString(VALUE)));
		} else {
			writeExceptionLog(LOGFILENAME, "setOCSInfo(OCSInfo ocsInfo, ResultSet rs) - one of parameters is null.");
		}
	}

	private static final String UPDATE_OCSINFO_SQL = "UPDATE OCSINFO SET VALUE=? WHERE NAME=?";
	/**
	 * 
	 * @return
	 */
	private boolean updateOCSInfo() {
		boolean result = false;
		if (updateOCSInfo != null) {
			Connection conn = null;
			PreparedStatement pstmt = null;
			try {
				conn = dbAccessManager.getConnection();
				Set<String> updateKeys = new HashSet<String>(updateOCSInfo.keySet());
				pstmt = conn.prepareStatement(UPDATE_OCSINFO_SQL);
				
				// 2021.02.26 by kw3711.kim : Batch 처리 및 DB 전체 적용후 메모리정리하도록 수정 
				conn.setAutoCommit(false);
				
				for (String updateKey : updateKeys) {
					pstmt.setString(1, (String) updateOCSInfo.get(updateKey));
					pstmt.setString(2, updateKey);
					pstmt.executeUpdate();
//					updateOCSInfo.remove(updateKey);					
				}
				conn.commit();
				conn.setAutoCommit(true);
				
				for (String updateKey : updateKeys) {
					updateOCSInfo.remove(updateKey);
				}
				
				result = true;
			} catch (SQLException se) {
				result = false;
				se.printStackTrace();
				writeExceptionLog(LOGFILENAME, se);
			} catch (Exception e) {
				result = false;
				e.printStackTrace();
				writeExceptionLog(LOGFILENAME, e);
			}
			finally {
				if (pstmt != null) {
					try {
						pstmt.close();
					} catch (Exception e) {}
					pstmt = null;
				}
			}
			if (result == false) {
				dbAccessManager.requestDBReconnect();
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getCurrDBTimeStr() {
		return currDBTimeStr;
	}
	
	private static final String CURR_TIME_SQL = "SELECT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') FROM DUAL";
	private String getCurrDBTimeFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String result = "";
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(CURR_TIME_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				if (rs != null) {
					result = rs.getString(1);
				}
			}
		} catch (SQLException se) {
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} 
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		return result;
	}
	
	public void addOCSInfoToOCSInfoUpdateList(OCSInfo ocsInfo) {
		try {
			updateOCSInfo.put(ocsInfo.getName(), ocsInfo.getValue());
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	public void addOCSInfoToOCSInfoUpdateList(String name, String value) {
		try {
			updateOCSInfo.put(name, value);
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	public OCSInfo getOCSInfoData(String name) {
		return (OCSInfo) data.get(name);
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public String getOCSInfoValue(String name) {
		if ((OCSInfo) data.get(name) == null) {
			return "";
		} else if ((((OCSInfo) data.get(name)).getValue()) == null) {
			return "";
		} else {
			return ((OCSInfo) data.get(name)).getValue();
		}
	}
		
	public void setIBSEMUpdate(RUNTIME_UPDATE runtimeUpdate) {
		this.ibsemUpdate = runtimeUpdate;
		addOCSInfoToOCSInfoUpdateList(IBSEM_UPDATE, runtimeUpdate.toConstString());
	}
	
	public void setJobAssignUpdate(RUNTIME_UPDATE runtimeUpdate) {
		this.jobAssignUpdate = runtimeUpdate;
		addOCSInfoToOCSInfoUpdateList(JOBASSIGN_UPDATE, runtimeUpdate.toConstString());
	}
	
	public void setOperationUpdate(RUNTIME_UPDATE runtimeUpdate) {
		this.operationUpdate = runtimeUpdate;
		addOCSInfoToOCSInfoUpdateList(OPERATION_UPDATE, runtimeUpdate.toConstString());
	}
	
	public void setOperationPortUpdate(RUNTIME_UPDATE runtimeUpdate) {
		this.operationPortUpdate = runtimeUpdate;
		addOCSInfoToOCSInfoUpdateList(OPERATION_PORT_UPDATE, runtimeUpdate.toConstString());
	}
	
	public void setOptimizerUpdate(RUNTIME_UPDATE runtimeUpdate) {
		this.optimizerUpdate = runtimeUpdate;
		addOCSInfoToOCSInfoUpdateList(OPTIMIZER_UPDATE, runtimeUpdate.toConstString());
	}
	
	public void setLongRunUpdate(RUNTIME_UPDATE runtimeUpdate) {
		this.longRunUpdate = runtimeUpdate;
		addOCSInfoToOCSInfoUpdateList(LONGRUN_UPDATE, runtimeUpdate.toConstString());
	}

	public void setSTBCUpdate(RUNTIME_UPDATE runtimeUpdate) {
		this.stbcUpdate = runtimeUpdate;
		addOCSInfoToOCSInfoUpdateList(STBC_UPDATE, runtimeUpdate.toConstString());
	}
	
	public void setUserBlockUpdate(RUNTIME_UPDATE runtimeUpdate) {
		this.userBlockUpdate = runtimeUpdate;
		addOCSInfoToOCSInfoUpdateList(USERBLOCK_UPDATE, runtimeUpdate.toConstString());
	}
	
	private void setOCSInfoParameters(String name, String value) {
		if (updateOCSInfo != null && updateOCSInfo.containsKey(name)) {
			return;
		}
		
		switch (name.hashCode()) {
			case ABORT_CHECKTIME_HASHCODE:
				setAbortCheckTime(value);
				break;
			case AREA_BALANCING_USAGE_HASHCODE:
				setAreaBalancingUsed(value);
				break;
			case AREA_BALANCING_LIMIT_HASHCODE:
				setAreaBalancingLimit(value);
				break;
			case AREA_BALANCING_INTERVAL_HASHCODE:
				setAreaBalancingInterval(value);
				break;
			case AUTO_RETRY_USAGE_HASHCODE:
				setAutoRetryUsed(value);
				break;
			case BIDIRECTIONALSTB_HASHCODE:
				setBidirectionalSTB(value);
				break;
			case BLOCK_RESET_TIMEOUT_HASHCODE:
				setBlockResetTimeout(value);
				break;
			case PATROL_CONTROL_USAGE_HASHCODE:
				setPatrolControlUsed(value);
				break;
			case COLLISIONNODE_CHECK_USAGE_HASHCODE:
				setCollisionNodeCheckUsed(value);
				break;
			case COMMFAIL_CHECKTIME_HASHCODE:
				setCommFailCheckTime(value);
				break;
			case CONGESTION_COUNT_THRESHOLD_HASHCODE:
				setCongestionCountThreshold(value);
				break;
			case CONGESTION_INDEX_THRESHOLD_HASHCODE:
				setCongestionIndexThreshold(value);
				break;
			case CONGESTION_PENALTY_RANGELIMIT_HASHCODE:
				setCongestionPenaltyRangeLimit(value);
				break;
			case CONGESTION_PENALTY_THRESHOLD_HASHCODE:
				setCongestionPenaltyThreshold(value);
				break;
			case CONGESTION_TIME_THRESHOLD_HASHCODE:
				setCongestionTimeThreshold(value);
				break;
			case CONVEYOR_HOTPRORITY_USAGE_HASHCODE:
				setConveyorHotPriorityUsed(value);
				break;
			case COST_SEARCH_OPTION_HASHCODE:
				setCostSearchOption(value);
				break;
			case DEADLOCK_DETECTED_TIMEOUT_HASHCODE:
				setDeadlockDetectedTimeout(value);
				break;
			case DELAY_LIMIT_JOBASSIGN_HASHCODE:
				setJobAssignDelayLimit(value);
				break;
			case DELAY_LIMIT_OPERATION_HASHCODE:
				setOperationDelayLimit(value);
				break;
			case DELAY_LIMIT_STBC_HASHCODE:
				setStbcDelayLimit(value);
				break;
			case DISPATCHING_RULE_HASHCODE:
				setDispatchingRule(value);
				break;
			case DRIVE_FAIL_LIMITTIME_HASHCODE: // for reroute on diverge node
				setDriveFailLimitTime(value);
				break;
			case DRIVE_LIMIT_TIME_HASHCODE:
				setDriveLimitTime(value);
				break;
			case DRIVINGQUEUE_CURVE_DISTANCE_HASHCODE:
				setDrivingQueueCurveDistance(value);
				break;
			case DRIVINGQUEUE_LINE_DISTANCE_HASHCODE:
				setDrivingQueueLineDistance(value);
				break;
			case DRIVE_METHOD_HASHCODE:
				setDriveMethod(value);
				break;
			case DYNAMIC_ROUTING_USAGE_HASHCODE:
				setDynamicRoutingUsed(value);
				break;
			case DYNAMICROUTING_HOLD_TIMEOUT_HASHCODE:
				setDynamicRoutingHoldTimeout(value);
				break;
			case EMULATORMODE_HASHCODE:
				setEmulatorMode(value);
				break;
			case ESTABLISH_COMMUNICATIONS_TIMEOUT_HASHCODE:
				setEstablishCommunicationsTimeout(value);
				break;
			case FAILUREOHT_DETOURSEARCH_USAGE_HASHCODE:
				setFailureOHTDetourSearchUsed(value);
				break;
			case FORMATTED_LOG_USAGE_HASHCODE:
				setFormattedLogUsed(value);
				break;
			case GOMODE_CHECKTIME_HASHCODE:
				setGoModeCheckTime(value);
				break;
			case GOMODE_VHLDETECTED_CHECKTIME_HASHCODE:
				setGoModeVehicleDetectedCheckTime(value);
				break;
			case GOMODE_VHLDETECTED_RESET_TIMEOUT_HASHCODE:
				setGoModeVehicleDetectedResetTimeout(value);
				break;
			case GOMODE_CARRIERSTATUS_CHECK_USAGE_HASHCODE:
				setGoModeCarrierStatusCheckUsed(value);
				break;
			case HID_CONTROL_USAGE_HASHCODE:
				setHidControlUsed(value);
				break;
			case HID_LIMIT_OVER_PASS_HASHCODE:
				setHidLimitOverPass(value);
				break;
			case HISTORY_DELETE_CHECK_PERIOD_HASHCODE:
				setHistoryDeleteCheckPeriod(value);
				break;
			case EVENT_HISTORY_DELETE_USAGE_HASHCODE:
				setEventHistoryDeleteUsage(value);
				break;
			case TRCOM_HISTORY_DELETE_USAGE_HASHCODE:
				setTrCompletionHistoryDeleteUsage(value);
				break;
			case VHLERROR_HISTORY_DELETE_USAGE_HASHCODE:
				setVehicleErrorHistoryDeleteUsage(value);
				break;
			case IBSEM_HISTORY_DELETE_USAGE_HASHCODE:
				setIBSEMHistoryDeleteUsage(value);
				break;
			case STBC_HISTORY_DELETE_USAGE_HASHCODE:
				setSTBCHistoryDeleteUsage(value);
				break;
			case HISTORY_HOLDINGPERIOD_HASHCODE:
				setHistoryHoldingPeriod(value);
				break;
			case IBSEM_UPDATE_HASHCODE:
				setIBSEMUpdate(value);
				break;
			case IBSEM_USAGE_HASHCODE:
				setIBSEMUsed(value);
				break;
			case IDREADER_ON_VEHICLE_HASHCODE:
				setIdReaderOnVehicle(value);
				break;
			case JOBASSIGN_DETAILRESULT_USAGE_HASHCODE:
				setJobAssignDetailResultUsage(value);
				break;
			case JOBASSIGN_SEARCH_LIMIT_HASHCODE:
				setJobAssignSearchLimit(value);
				break;
			case JOBASSIGN_THRESHOLD_HASHCODE:
				setJobAssignThreshold(value);
				break;
			case JOBASSIGN_LOCATE_THRESHOLD_HASHCODE:
				setJobAssignLocateThreshold(value);
				break;
			case JOBASSIGN_PRIORITY_THRESHOLD_HASHCODE:
				setJobAssignPriorityThreshold(value);
				break;
			case JOBASSIGN_PRIORITY_WEIGHT_HASHCODE:
				setJobAssignPriorityWeight(value);
				break;
			case JOBASSIGN_UPDATE_HASHCODE:
				setJobAssignUpdate(value);
				break;
			case JOBASSIGN_URGENT_THRESHOLD_HASHCODE:
				setJobAssignUrgentThreshold(value);
				break;
			case JOBASSIGN_WAITINGTIME_THRESHOLD_HASHCODE:
				setJobAssignWaitingTimeThreshold(value);
				break;
			case JOBASSIGN_WAITINGTIME_WEIGHT_HASHCODE:
				setJobAssignWaitingTimeWeight(value);
				break;
			case LOADED_VEHICLE_PENALTY_HASHCODE:
				setLoadedVehiclePenalty(value);
				break;
			case REPATHSEARCH_HOLD_TIMEOUT_HASHCODE:
				setRepathSearchHoldTimeout(value);
				break;
			case LOCALOHT_CLEAR_OPTION_HASHCODE:
				setLocalOHTClearOption(value);
				break;
			case LOCALOHT_USAGE_HASHCODE:
				setLocalOHTUsed(value);
				break;
			case LONGRUN_UPDATE_HASHCODE:
				setLongRunUpdate(value);
				break;
			case LONGRUN_MOVE_USAGE_HASHCODE:
				setLongRunMoveUsed(value);
				break;
			case LONGRUN_TRANSFER_USAGE_HASHCODE:
				setLongRunTransferUsed(value);
				break;
			case LOG_HOLDINGPERIOD_JOBASSIGN_HASHCODE:
				setLogHoldingPeriod_JobAssign(value);
				break;
			case LOG_HOLDINGPERIOD_OPERATION_HASHCODE:
				setLogHoldingPeriod_Operation(value);
				break;
			case LOG_HOLDINGPERIOD_IBSEM_HASHCODE:
				setLogHoldingPeriod_IBSEM(value);
				break;
			case LOG_HOLDINGPERIOD_DEFAULT_HASHCODE:
				setLogHoldingPeriod_Default(value);
				break;
			case MAP_DISTANCE_USAGE_HASHCODE:
				setMapDistanceUsed(value);
				break;
			case MAP_VEHICLESPEED_USAGE_HASHCODE:
				setMapVehicleSpeedUsed(value);
				break;
			case MISMATCH_RECOVERY_MODE_HASHCODE:
				setAutoMismatchRecoveryMode(value);
				break;
			case MISMATCH_UNLOAD_APPLIED_PORT_HASHCODE:
				setMismatchUnloadAppliedPort(value);
				break;
			case MISSED_CARRIER_CHECK_USAGE_HASHCODE:
				setMissedCarrierCheckUsed(value);
				break;
			case MISSED_CARRIER_CHECK_SLEEP_HASHCODE:
				setMissedCarrierCheckSleep(value);
				break;
			case NEXT_CMD_USAGE_HASHCODE:
				setNextCommandUsed(value);
				break;
			case OCSCONTROL_HASHCODE:
				setOcsControlState(value);
				break;
			case OHT_COUNT_LIMIT_PER_HID_HASHCODE:
				setVehicleCountPerHid(value);
				break;
			case OPERATION_UPDATE_HASHCODE:
				setOperationUpdate(value);
				break;
			case OPERATION_PORT_UPDATE_HASHCODE:
				setOperationPortUpdate(value);
				break;
			case OPTIMIZER_UPDATE_HASHCODE:
				setOptimizerUpdate(value);
				break;
			case PARK_NODE_USAGE_HASHCODE:
				setParkNodeUsed(value);
				break;
			case PARK_SEARCH_INTERVAL_HASHCODE:
				setParkSearchInterval(value);
				break;
			case PARK_SEARCH_LIMIT_HASHCODE:
				setParkSearchLimit(value);
				break;
			case PORT_DUP_USAGE_HASHCODE:
				setPortDuplicationUsed(value);
				break;
			case RAILDOWN_CHECK_USAGE_HASHCODE:
				setRailDownCheckUsed(value);
				break;
			case REFINE_PORTDUP_TRCMD_HASHCODE:
				setRefinePortDupTrCmdUsed(value);
				break;
			case RF_READ_DEVICE_HASHCODE:
				setRfReadDevice(value);
				break;
			case RFC_ERROR_PORTOUTOFSERVICE_USAGE_HASHCODE: // 2015.03.19 by KYK 이름 변경
				setRFCErrorPortOutOfServiceUsed(value);
				break;
			case RFC_USAGE_HASHCODE:
				setRFCUsed(value);
				break;
			case RFC_VERIFYING_TIMEOUT_HASHCODE: // 2013.01.04 by KYK
				setRfcVerifyingTimeout(value);
				break;				
			case STBC_UPDATE_HASHCODE:
				setStbcUpdate(value);
				break;
			case STBC_USAGE_HASHCODE:
				setSTBCUsed(value);
				break;
			case STBDATA_RECOVERY_OPTION_HASHCODE: // 2013.01.04 by KYK
				setStbDataRecoveryOption(value);
				break;
			case STBDATA_SAVE_PERIOD_HASHCODE: // 2013.01.04 by KYK
				setStbDataSavePeriod(value);
				break;
			case STBDATA_SAVE_USAGE_HASHCODE:
				setSTBDataSaveUsed(value);
				break;
			case STATION_USAGE_HASHCODE: // 2013.02.28 by KYK
				setStationUsed(value);
				break;				
			case STEERING_READY_USAGE_HASHCODE:
				setSteeringReadyUsed(value);
				break;
			case SYSTEM_COLLISION_UPDATE_HASHCODE:
				setSystemCollisionUpdate(value);
				break;
			case SYSTEMPAUSE_REQUEST_TIMEOUT_HASHCODE:
				setSystemPauseRequestTimeout(value);
				break;
			case PRIORJOB_CRITERIA_PRIORITY_HASHCODE:
				setPriorJobCriteriaOfPriority(value);
				break;
			case PRIORJOB_CRITERIA_WAITINGTIME_HASHCODE:
				setPriorJobCriteriaOfWaitingTime(value);
				break;
			case PRIORJOB_CRITERIA_TRANSFERTIME_HASHCODE:
				setPriorJobCriteriaOfTransferTime(value);
				break;
			case SOCKET_RECONNECTION_TIMEOUT_HASHCODE:
				setSocketReconnectionTimeout(value);
				break;
			case SOCKET_CLOSE_CHECKTIME_HASHCODE:
				setSocketCloseCheckTime(value);
				break;
			case TSCSTATUS_HASHCODE:
				setTscState(value);
				break;
			case UNLOADERROR_REPORT_USAGE_HASHCODE:
				setUnloadErrorReportUsed(value);
				break;
			case USER_PASSTHROUGH_USAGE_HASHCODE:
				setUserPassThroughUsed(value);
				break;
			case USERBLOCK_UPDATE_HASHCODE:
				setUserBlockUpdate(value);
				break;
			case VEHICLE_CURVE_SPEED_HASHCODE:
				setVehicleCurveSpeed(value);
				break;
			case VEHICLE_LINE_SPEED_HASHCODE:
				setVehicleLineSpeed(value);
				break;
			case VEHICLE_LENGTH_HASHCODE:
				setVehicleLength(value);
				break;
			case VEHICLE_PENALTY_ERROR_HASHCODE:
				setErrorVehiclePenalty(value);
				break;
			case VEHICLE_PENALTY_GOING_HASHCODE:
				setGoingVehiclePenalty(value);
				break;
			case VEHICLE_PENALTY_MANUAL_HASHCODE:
				setManualVehiclePenalty(value);
				break;
			case VEHICLE_PENALTY_STOPPING_HASHCODE:
				setStoppingVehiclePenalty(value);
				break;
			case VEHICLE_PENALTY_WORKING_HASHCODE:
				setWorkingVehiclePenalty(value);
				break;
			case VEHICLE_PENALTY_CLEANING_HASHCODE:
				setCleaningVehiclePenalty(value);
				break;
			case VEHICLE_WIDTH_HASHCODE:
				setVehicleWidth(value);
				break;
			case VEHICLETRAFFIC_LOG_USAGE_HASHCODE:
				setVehicleTrafficLogUsed(value);
				break;
			case VIBRATION_CONTROL_USAGE_HASHCODE:
				setVibrationControlUsed(value);
				break;
			case VIBRATION_MONITORING_TIMEOUT_HASHCODE:
				setVibrationMonitoringTimeout(value);
				break;
			case WORKMODE_CHECKTIME_HASHCODE:
				setWorkModeCheckTime(value);
				break;
			case YIELD_MIN_LIMITTIME_HASHCODE:
				setYieldMinLimitTime(value);
				break;
			case YIELD_REQUEST_LIMITTIME_HASHCODE:
				setYieldRequestLimitTime(value);
				break;
			case YIELD_SEARCH_RULE_HASHCODE:
				setYieldSearchRule(value);
				break;
			case YIELD_SEARCH_USAGE_HASHCODE:
				setYieldSearchUsed(value);
				break;
			case CONSECUTIVE_YIELD_LIMITTIME_HASHCODE:
				setConsecutiveYieldLimitTime(value);
				break;				
			case NEARBY_NORMAL_DRIVE_USAGE_HASHCODE:
				setNearByNormalDrive(value);
				break;
			case STAGE_LOCATE_VEHICLE_COUNT_HASHCODE:
				setStageLocateVehicleCount(value);
				break;
			case STAGE_LOCATE_USAGE_HASHCODE:
				setStageLocateUsage(value);
				break;
			case DEADLOCKBREAK_NEARBYDRIVE_USAGE_HASHCODE: // 2014.09.13 by zzang9un : 근접제어 deadlock break on/off
				setDeadlockBreakNearbyDriveUsage(value);
				break;
			case RESENDCMD_FOR_ABNORMAL_REPLY_HASHCODE:
				setResendCmdForAbnormalReply(value);
				break;
			case BLOCK_PREEMPTION_UPDATE_USAGE_HASHCODE:
				setBlockPreemptionUpdateUsed(value);
				break;
			case STB_REPORT_USAGE_HASHCODE: // 2015.02.25 by KBS : STBC 이상감지 on/off
				setSTBReportUsed(value);
				break;
			case DETOUR_CONTROL_USAGE_HASHCODE:
				setDetourControlUsed(value);
				break;
			case DETOUR_PORTSERVICE_LIMIT_COUNT_HASHCODE:
				setDetourPortServiceLimitCount(value);
				break;
			case FOUPID_USAGE_HASHCODE:
				setFoupIdUsed(value);
				break;
			case VEHICLECOMM_TYPE_HASHCODE:
				setVehicleCommType(value);
				break;
				// 2013.04.05 by KYK
			case NEARBY_TYPE_HASHCODE: 
				setNearbyType(value);
				break;
			case FLOW_CONTROL_TYPE_HASHCODE:
				setFlowControlType(value);
				break;
			case DRIVINGQUEUE_CONVERGE_LIMITTIME_HASHCODE:
				setDrivingQueueConvergeLimitTime(value);
				break;
			case DRIVINGQUEUE_DIVERGE_LIMITTIME_HASHCODE:
				setDrivingQueueDivergeLimitTime(value);
				break;
			case DRIVINGQUEUE_CURVE_LIMITTIME_HASHCODE:
				setDrivingQueueCurveLimitTime(value);
				break;
			case DRIVINGQUEUE_LINE_LIMITTIME_HASHCODE:
				setDrivingQueueLineLimitTime(value);
				break;
			case HOIST_SPEED_LEVEL_HASHCODE:
				setHoistSpeedLevel(value);
				break;
			case SHIFT_SPEED_LEVEL_HASHCODE:
				setShiftSpeedLevel(value);
				break;
				// 2013.10.22 by KYK
			case STATIONDATA_REVISION_HASHCODE:
				setStationDataRevision(value);
				break;
			case TEACHINGDATA_REVISION_HASHCODE:
				setTeachingDataRevision(value);
				break;
			case PASSDOOR_CONTROL_USAGE_HASHCODE:
				setPassDoorControlUsage(value);
				break;
			case JOB_RESERVATION_OPTION_HASHCODE: // 2015.03.17 by KYK : Job Reservation Option
				setJobReservationOption(value);
				break;
			case JOB_RESERVATION_LIMITTIME_HASHCODE:
				setJobReservationLimitTime(value);
				break;
			case SYSTEM_COLLISION_CRITERION_HASHCODE:
				setSystemCollisionCriterion(value);
				break;
			case BLOCK_ARRIVAL_REARRANGETIME_HASHCODE:
				setBlockArrivalRearrangeTime(value);
				break;
				// 2015.05.01 by KYK
			case COMMFAIL_ALARMREPORT_USAGE_HASHCODE:
				setCommfailAlarmReportUsed(value);
				break;
			case CARRIERTYPE_MISMATCH_USAGE_HASHCODE:
				setCarrierTypeMismatchUsed(value);
				break;
			case TRAFFIC_UPDATE_USAGE_HASHCODE:	// 2015.05.27 by MYM : Vehicle Traffic 분산
				setTrafficUpdateUsed(value);
				break;
			case TRAFFIC_UPDATE_RULE_HASHCODE:	// 2015.05.27 by MYM : Vehicle Traffic 분산
				setTrafficUpdateRule(value);
				break;
			case PRIORJOB_DISPATCHING_USAGE_HASHCODE: // 2015.10.01 by KYK
				setPriorJobDispatchingUsed(value);
				break;
			case PRIORJOB_DISPATCHING_RULE_HASHCODE:
				setPriorJobDispatchingRule(value);
				break;
			case PRIORJOB_PRIORITY_THRESHOLD_HASHCODE:
				setPriorJobPriorityThreshold(value);
				break;
			case PRIORJOB_WAITINGTIME_THRESHOLD_HASHCODE:
				setPriorJobWaitingTimeThreshold(value);
				break;
			case IGNORE_MATERIAL_DIFFERENCE_HASHCODE:
				setIgnoreMaterialDifference(value);
				break;
			case CARRIER_TYPE_USAGE_HASHCODE:
				setCarrierTypeUsage(value);
				break;
			case OPERATION_MANAGER_LOGGING_USAGE_HASHCODE:
				setOperationManagerLoggingUsage(value);
				break;
			case PARK_COMMAND_EQOPTION_USAGE_HASHCODE:
				setIsParkCmdEqoption(value);
				break;
			case STAGE_SOURCE_DUP_CANCEL_USAGE_HASHCODE:
				setStageSourceDupCancelUsage(value);
				break;
			case AREA_BALANCING_MANUAL_EXCLUDE_HASHCODE: // 2022.12.14 by JJW : Area 내  Max VHL 초과시  Manual VHL 제외 여부
				setAreaBalancingManualExclude(value);
				break;
			default:
				break;
		}
	}
	
	private boolean isCollisionNodeCheckUsed = true;
	private boolean isAreaBalancingUsed = false;
	private boolean isAutoMismatchRecoveryMode = false;
	private boolean isAutoRetryUsed = false;
	private boolean isBidirectionalSTB = true;
	private boolean isPatrolControlUsed = false;
	private boolean isConveyorHotPriorityUsed = false;
	private boolean isDynamicRoutingUsed = true;
	private boolean isEmulatorMode = false;
	private boolean isFailureOHTDetourSearchUsed = true;
	private boolean isFormattedLogUsed = false;
	private boolean isGoModeCarrierStatusCheckUsed = false;
	private boolean isHidControlUsed = true;
	private boolean isHidLimitOverPass = false;
	private boolean isEventHistoryDeleteUsed = true;
	private boolean isTrCompletionHistoryDeleteUsed = true;
	private boolean isVehicleErrorHistoryDeleteUsed = true;
	private boolean isIBSEMHistoryDeleteUsed = true;
	private boolean isSTBCHistoryDeleteUsed = true;
	private boolean isIBSEMUsed = true;
	private boolean isIdReaderInstalledOnVehicle = false;
	private boolean isJobAssignDetailResultUsed = false;
	private boolean isLocalOHTUsed = false;
	private boolean isLongRunMoveUsed = false;
	private boolean isLongRunTransferUsed = false;
	private boolean isMapDistanceUsed = false;
	private boolean isMapVehicleSpeedUsed = false;
	private boolean isMissedCarrierCheckUsed = true;
	private boolean isNearByDrive = false;
	private boolean isNearByNormalDrive = false;
	private boolean isNextCommandUsed = false;
	private boolean isParkNodeUsed = false;
	private boolean isPortDuplicationUsed = false;
	private boolean isRailDownCheckUsed = false;
	private boolean isRefinePortDupTrCmdUsed = true;
	private boolean isRFCErrorPortOutOfServiceUsed;	// 2015.03.19 by KYK (RFC Error Control Usage 에서 변경)
	private boolean isRFCUsed;  
	private boolean isSTBCUsed = false;
	private boolean isSTBDataSaveUsed = false;	
	private boolean isStationUsed = false; // 2013.02.28 by KYK
	private boolean isSteeringReadyUsed = false;	
	private boolean isSystemCollisionUpdate = true;
	private boolean isUnloadErrorReportUsed = true;
	private boolean isUserBlockUpdate = false;
	private boolean isUserPassThroughUsed = false;
	private boolean isVehicleTrafficLogUsed = false;
	private boolean isVibrationControlUsed = false;
	private boolean isYieldSearchUsed = true;
	private boolean isStageLocateUsage = true;
	private boolean isDeadlockBreakNearbyDriveUsage = true; // 2014.09.13 by zzang9un : 근접제어 deadlock break 기능 on/off
	private boolean isResendCmdForAbnormalReply = true;
	private boolean isBlockPreemptionUpdateUsed = false;
	private boolean isSTBReportUsed = false; // 2015.02.25 by KBS : STBC 이상감지 on/off
	private boolean isDetourControlUsed = true;
	private boolean isFoupIdUsed = false;	// 2014.01.02 by KBS : FoupID 사용 모드 (for A-PJT EDS)
	private boolean isPassDoorControlUsage = false; // 2015.02.07 by zzang9un : PassDoor Control Usage 플래그
	// 2015.05.01 by KYK
	public boolean isCommfailAlarmReportUsed = false;
	private boolean isCarrierTypeMismatchUsed = true; // default:True
	
	private boolean isTrafficUpdateUsed = false; // 2015.05.27 by MYM : Vehicle Traffic 분산
	private boolean isPriorJobDispatchingUsed = false; // 2015.10.01 by KYK : priorJob dispatching
	
	private boolean isIgnoreMaterialDifference = false;	// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
	private boolean isCarrierTypeUsage = false;	// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가

	private boolean isOperationManagerLoggingUsage = false;	// 2020.08.06 by kw3711.kim : OperationManager Logging Usage 추가 (Operation Timeover 모니터링)

	private boolean isParkCmdEqoption = false;
	
	private boolean isStageSourceDupCancelUsage = false; // 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
	
	private boolean isAreaBalancingManualExclude = false; // 2022.12.14 by JJW : Area 내  Max VHL 초과시  Manual VHL 제외 여부

	private int areaBalancingLimit = 1;
	private int areaBalancingInterval = 5;
	private int commFailCheckTime = 10000;
	private int delayLimitOfJobAssign = 6000;
	private int delayLimitOfOperation = 100;
	private int delayLimitOfStbc = 100;
	private int driveFailLimitTime = 120000;
	private int drivingQueueCurveDistance = 4500;
	private int drivingQueueLineDistance = 6000;
	private int establishCommunicationsTimeout = 5;
	private int goModeCheckTime = 60;
	private int goModeVehicleDetectedCheckTime = 300;
	private int goModeVehicleDetectedResetTimeout = 600;
	private int historyHoldingPeriod = 14;
	private int loadRetryLimit = 0;
	private int logHoldingPeriod_JobAssign = 7;
	private int logHoldingPeriod_Operation = 7;
	private int logHoldingPeriod_IBSEM = 7;
	private int logHoldingPeriod_Default = 7;
	private int parkSearchInterval = 5;
	private int priorJobCriteriaOfPriority = 100;
	private int priorJobCriteriaOfWaitingTime = 240;
	private int priorJobCriteriaOfTransferTime = 300;
	private int unloadRetryLimit = 0;
	private int vehicleCountPerHid = 20;
	private int vehicleLength = 1500;
	private int vehicleWidth = 100;
	private int workModeCheckTime = 60;
	private int yieldMinLimitTime = 2;
	private int yieldRequestLimitTime = 7;
	private int consecutiveYieldLimitTime = 4; // 2015.07.08 by KYK
	private int blockResetTimeout = 20;
	private int stbDataSavePeriod = 300; // 2013.01.04 by KYK
	private int rfcVerifyingTimeout = 15; // 2013.01.04 by KYK
	private int stageLocateVehicleCount = 3; // 2014.03.14 by MYM [Stage Locate 기능]
	private int detourPortServiceLimitCount = 50000;
	private int jobReservationLimitTime = 300; // 2015.04.06 by KYK
	private int blockArrivalRearrangTime = 10; // 2015.05.27 by MYM
	
	private double congestionCountThreshold = 20;
	private double congestionIndexThreshold = 10;
	private double congestionPenaltyRangeLimit = 30;
	private double congestionPenaltyThreshold = 10;
	private long congestionTimeThreshold = 120000;
	
	private double abortCheckTime = 60;
	private double driveLimitTime = 7;
	private double errorVehiclePenalty = 300;
	private double goingVehiclePenalty = 1;
	private double jobAssignSearchLimit = 300;
	private double jobAssignThreshold = 9999;
	private double jobAssignLocateThreshold = 9999;
	private double jobAssignPriorityThreshold = 90;
	private double jobAssignPriorityWeight = 2;
	private double jobAssignUrgentThreshold = 300;
	private double jobAssignWaitingTimeThreshold = 150;
	private double jobAssignWaitingTimeWeight = 1;
	private double loadedVehiclePenalty = 0;
	private double manualVehiclePenalty = 300;
	private double parkSearchLimit = 9999;
	private double vehicleCurveSpeed = 1000;
	private double vehicleLineSpeed = 2500;
	private double stoppingVehiclePenalty = 1;
	private double workingVehiclePenalty = 10;
	private double cleaningVehiclePenalty = 10;

	private long deadlockDetectedTimeout = 120000;
	private long dynamicRoutingHoldTimeout = 50000;
	private long historyDeleteCheckPeriod = 3600000;
	private long missedCarrierCheckSleep = 1000;
	private long repathSearchHoldTimeout = 30000;
	private long socketReconnectionTimeout = 5000;
	private long socketCloseCheckTime = 5000;
	private long systemPauseRequestTimeout = 60000;
	private long vibrationMonitoringTimeout = 10800000L;

	// 2013.04.12 by KYK
	private double drivingQueueConvergeLimitTime = 2;
	private double drivingQueueDivergeLimitTime = 2;
	private double drivingQueueCurveLimitTime = 1.5;
	private double drivingQueueLineLimitTime = 2;
	
	private int hoistSpeedLevel = 100;
	private int shiftSpeedLevel = 100;
	// 2013.10.22 by KYK
	private int stationDataRevision = 0;
	private int teachingDataRevision = 0;
		
	private String mismatchUnloadAppliedPort = DEFAULT_MISMATCH_UNLOAD_APPLIED_PORT;
	private String rfReadDevice = DEFAULT_RFREAD_DEVICE;
	private String yieldSearchRule = DEFAULT_YIELDSEARCH_RULE;
	private String stbDataRecoveryOption = STBC; // 2013.01.04 by KYK
	
	private DISPATCHING_RULES dispatchingRule = DISPATCHING_RULES.HYBRID2;
	private LOCALGROUP_CLEAROPTION localOHTClearOption = LOCALGROUP_CLEAROPTION.UNLOADING_VHL;
	private OCS_CONTROL_STATE ocsControlState = OCS_CONTROL_STATE.NULL;
	private RUNTIME_UPDATE ibsemUpdate = RUNTIME_UPDATE.NO;
	private RUNTIME_UPDATE stbcUpdate = RUNTIME_UPDATE.NO;
	private RUNTIME_UPDATE jobAssignUpdate = RUNTIME_UPDATE.NO;
	private RUNTIME_UPDATE operationUpdate = RUNTIME_UPDATE.NO;
	private RUNTIME_UPDATE operationPortUpdate = RUNTIME_UPDATE.NO;
	private RUNTIME_UPDATE optimizerUpdate = RUNTIME_UPDATE.NO;
	private RUNTIME_UPDATE longRunUpdate = RUNTIME_UPDATE.NO;
	private RUNTIME_UPDATE userBlockUpdate = RUNTIME_UPDATE.NO;
	private TSC_STATE tscState = TSC_STATE.NULL;
	private COSTSEARCH_OPTION costSearchOption = COSTSEARCH_OPTION.NONE;
	private SYSTEM_COLLISION_CRITERION systemCollisionCriterion = SYSTEM_COLLISION_CRITERION.DISTANCE;
	
	private VEHICLECOMM_TYPE vehicleCommType = VEHICLECOMM_TYPE.VEHICLECOMM_CHAR;
	private NEARBY_TYPE nearbyType = NEARBY_TYPE.NEARBY_V3;
	private FLOW_CONTROL_TYPE flowControlType = FLOW_CONTROL_TYPE.DQRANGE;
	private JOB_RESERVATION_OPTION jobReservationOption = JOB_RESERVATION_OPTION.JR1; // 2015.03.17 by KYK [Job Reservation Option]
	private TRAFFIC_UPDATE_RULE trafficUpdateRule = TRAFFIC_UPDATE_RULE.PUSH; // 2015.05.27 by MYM
	private PRIORJOB_DISPATCHING_RULE priorJobDispatchingRule = PRIORJOB_DISPATCHING_RULE.EQPRIORITY; // 2015.10.01 by KYK
	private int priorJobPriorityThreshold = 90;
	private int priorJobWaitingTimeThreshold = 600;

	// Setter
	
	private void setCollisionNodeCheckUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isCollisionNodeCheckUsed = false;
		} else {
			isCollisionNodeCheckUsed = true;
		}
	}
	
	private void setAreaBalancingUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isAreaBalancingUsed = true;
		} else {
			isAreaBalancingUsed = false;
		}
//		writeLog(LOGFILENAME, "setAreaBalancingUsed() - Value:" + value + ", Result:" + isAreaBalancingUsed);
	}
	
	private void setAutoMismatchRecoveryMode(String value) {
		//default: MANUAL
		if (AUTO.equals(value)) {
			isAutoMismatchRecoveryMode = true;
		} else {
			isAutoMismatchRecoveryMode = false;
		}
//		writeLog(LOGFILENAME, "setAutoMismatchRecoveryMode() - Value:" + value + ", Result:" + isAutoMismatchRecoveryMode);
	}
	
	private void setBidirectionalSTB(String value) {
		//default: YES
		if (NO.equals(value)) {
			isBidirectionalSTB = false;
		} else {
			isBidirectionalSTB = true;
		}
//		writeLog(LOGFILENAME, "setBidirectionalSTB() - Value:" + value + ", Result:" + isBidirectionalSTB);
	}
	
	private void setPatrolControlUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isPatrolControlUsed = true;
		} else {
			isPatrolControlUsed = false;
		}
//		writeLog(LOGFILENAME, "setPatrolControlUsed() - Value:" + value + ", Result:" + isPatrolControlUsed);
	}
	
	private void setConveyorHotPriorityUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isConveyorHotPriorityUsed = true;
		} else {
			isConveyorHotPriorityUsed = false;
		}
//		writeLog(LOGFILENAME, "setConveyorHotPriorityUsed() - Value:" + value + ", Result:" + isConveyorHotPriorityUsed);
	}
	
	private void setDynamicRoutingUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isDynamicRoutingUsed = false;
		} else {
			isDynamicRoutingUsed = true;
		}
	}
	
	private void setEmulatorMode(String value) {
		//default: NO
		if (YES.equals(value)) {
			isEmulatorMode = true;
		} else {
			isEmulatorMode = false;
		}
//		writeLog(LOGFILENAME, "setEmulatorMode() - Value:" + value + ", Result:" + isEmulatorMode);
	}
	
	private void setFailureOHTDetourSearchUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isFailureOHTDetourSearchUsed = false;
		} else {
			isFailureOHTDetourSearchUsed = true;
		}
//		writeLog(LOGFILENAME, "setFailureOHTDetourSearchUsed() - Value:" + value + ", Result:" + isFailureOHTDetourSearchUsed);
	}
	
	private void setBlockPreemptionUpdateUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isBlockPreemptionUpdateUsed = true;
		} else {
			isBlockPreemptionUpdateUsed = false;
		}
	}
	
	private void setDetourControlUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isDetourControlUsed = false;
		} else {
			isDetourControlUsed = true;
		}
	}
	
	private void setBlockArrivalRearrangeTime(String value) {
		try {
			blockArrivalRearrangTime = Integer.parseInt(value);
			if (blockArrivalRearrangTime < 1) blockArrivalRearrangTime = 1;
		} catch (Exception e) {
			blockArrivalRearrangTime = 10;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setTrafficUpdateUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isTrafficUpdateUsed = true;
		} else {
			isTrafficUpdateUsed = false;
		}
	}
	
	private void setTrafficUpdateRule(String value) {
		try {
			trafficUpdateRule = TRAFFIC_UPDATE_RULE.toTrafficUpdateRule(value);
		} catch (Exception e) {
			trafficUpdateRule = TRAFFIC_UPDATE_RULE.PUSH;
			writeExceptionLog(LOGFILENAME, e);
		}
	}

	private void setDetourPortServiceLimitCount(String value) {
		try {
			detourPortServiceLimitCount = Integer.parseInt(value);
			if (detourPortServiceLimitCount < 50) {
				detourPortServiceLimitCount = 50;
			}
		} catch (Exception e) {
			detourPortServiceLimitCount = 50000;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setSystemCollisionCriterion(String value) {
		try {
			systemCollisionCriterion = SYSTEM_COLLISION_CRITERION.toSystemCollisionCriterion(value);
		} catch (Exception e) {
			systemCollisionCriterion = SYSTEM_COLLISION_CRITERION.DISTANCE;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setSTBReportUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isSTBReportUsed = true;
		} else {
			isSTBReportUsed = false;
		}
	}
	
	private void setFoupIdUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isFoupIdUsed = true;
		} else {
			isFoupIdUsed = false;
		}
	}

	private void setFormattedLogUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isFormattedLogUsed = true;
		} else {
			isFormattedLogUsed = false;
		}
//		writeLog(LOGFILENAME, "setFormattedLogUsed() - Value:" + value + ", Result:" + isFormattedLogUsed);
	}
	
	private void setHidControlUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isHidControlUsed = false;
		} else {
			isHidControlUsed = true;
		}
//		writeLog(LOGFILENAME, "setHidControlUsed() - Value:" + value + ", Result:" + isHidControlUsed);
	}
	
	private void setHidLimitOverPass(String value) {
		//default: NO
		if (YES.equals(value)) {
			isHidLimitOverPass = true;
		} else {
			isHidLimitOverPass = false;
		}
//		writeLog(LOGFILENAME, "setHidLimitOverPass() - Value:" + value + ", Result:" + isHidLimitOverPass);
	}
	
	private void setIBSEMUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isIBSEMUsed = false;
		} else {
			isIBSEMUsed = true;
		}
//		writeLog(LOGFILENAME, "setIBSEMUsed() - Value:" + value + ", Result:" + isIBSEMUsed);
	}
	
	private void setIdReaderOnVehicle(String value) {
		//default: INSTALLED
		if (NOT_INSTALLED.equals(value)){
			isIdReaderInstalledOnVehicle = false;
		} else {
			isIdReaderInstalledOnVehicle = true;
		}
	}
	
	private void setJobAssignDetailResultUsage(String value) {
		//default: NO
		if (YES.equals(value)) {
			isJobAssignDetailResultUsed = true;
		} else {
			isJobAssignDetailResultUsed = false;
		}
//		writeLog(LOGFILENAME, "setJobAssignDetailResultUsage() - Value:" + value + ", Result:" + isJobAssignDetailResultUsed);
	}
	
	private void setLocalOHTUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isLocalOHTUsed = true;
		} else {
			isLocalOHTUsed = false;
		}
//		writeLog(LOGFILENAME, "setLocalOHTUsed() - Value:" + value + ", Result:" + isLocalOHTUsed);
	}
	
	private void setLongRunMoveUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isLongRunMoveUsed = true;
		} else {
			isLongRunMoveUsed = false;
		}
//		writeLog(LOGFILENAME, "setLongRunMoveUsed() - Value:" + value + ", Result:" + isLongRunMoveUsed);
	}
	
	private void setLongRunTransferUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isLongRunTransferUsed = true;
		} else {
			isLongRunTransferUsed = false;
		}
//		writeLog(LOGFILENAME, "setLongRunTransferUsed() - Value:" + value + ", Result:" + isLongRunTransferUsed);
	}
	
	private void setMapDistanceUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isMapDistanceUsed = true;
		} else {
			isMapDistanceUsed = false;
		}
	}
	private void setMapVehicleSpeedUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isMapVehicleSpeedUsed = true;
		} else {
			isMapVehicleSpeedUsed = false;
		}
	}
	
	private void setMissedCarrierCheckUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isMissedCarrierCheckUsed = false;
		} else {
			isMissedCarrierCheckUsed = true;
		}
	}
	
	private void setDriveMethod(String value) {
		//default: NORMAL
		if (NEARBY.equals(value)) {
			isNearByDrive = true;
		} else {
			isNearByDrive = false;
		}
//		writeLog(LOGFILENAME, "setDriveMethod() - Value:" + value + ", Result:" + isNearByDrive + " " + driveMethod);
	}
	
	private void setNearByNormalDrive(String value) {
		if (YES.equals(value)) {
			isNearByNormalDrive = true;
		} else {
			isNearByNormalDrive = false;
		}
	}
	
	private void setNextCommandUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isNextCommandUsed = true;
		} else {
			isNextCommandUsed = false;
		}
//		writeLog(LOGFILENAME, "setNextCommandUsed() - Value:" + value + ", Result:" + isNextCommandUsed);
	}
	
	private void setPortDuplicationUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isPortDuplicationUsed = true;
		} else {
			isPortDuplicationUsed = false;
		}
//		writeLog(LOGFILENAME, "setPortDuplicationUsed() - Value:" + value + ", Result:" + isPortDuplicationUsed);
	}
	
	private void setParkNodeUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isParkNodeUsed = true;
		} else {
			isParkNodeUsed = false;
		}
//		writeLog(LOGFILENAME, "setParkNodeUsed() - Value:" + value + ", Result:" + isParkNodeUsed);
	}
	
	private void setRefinePortDupTrCmdUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isRefinePortDupTrCmdUsed = false;
		} else {
			isRefinePortDupTrCmdUsed = true;
		}
//		writeLog(LOGFILENAME, "setRefinePortDupTrCmdUsed() - Value:" + value + ", Result:" + isRefinePortDupTrCmdUsed);
	}
	
	private void setSTBCUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isSTBCUsed = true;
		} else {
			isSTBCUsed = false;
		}
//		writeLog(LOGFILENAME, "setSTBCUsed() - Value:" + value + ", Result:" + isSTBCUsed);
	}
	
	// 2015.03.19 by KYK (RFC Error Control Usage 에서 변경)
	private void setRFCErrorPortOutOfServiceUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isRFCErrorPortOutOfServiceUsed = true;
		} else {
			isRFCErrorPortOutOfServiceUsed = false;
		}
	}
	
	private void setRFCUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isRFCUsed = true;
		} else {
			isRFCUsed = false;
		}
//		writeLog(LOGFILENAME, "setRFCUsed() - Value:" + value + ", Result:" + isSTBCUsed);
	}

	private void setSTBDataSaveUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isSTBDataSaveUsed = true;
		} else {
			isSTBDataSaveUsed = false;
		}
//		writeLog(LOGFILENAME, "setSTBDataSaveUsed() - Value:" + value + ", Result:" + isSTBCUsed);
	}
	
	private void setSteeringReadyUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isSteeringReadyUsed = true;
		} else {
			isSteeringReadyUsed = false;
		}
//		writeLog(LOGFILENAME, "setSteeringReadyUsed() - Value:" + value + ", Result:" + isSteeringReadyUsed);
	}

	private void setSystemCollisionUpdate(String value) {
		//default: YES
		if (NO.equals(value)) {
			isSystemCollisionUpdate = false;
		} else {
			isSystemCollisionUpdate = true;
		}
//		writeLog(LOGFILENAME, "setSystemCollisionUpdate() - Value:" + value + ", Result:" + isSystemCollisionUpdate);
	}
	
	private void setUnloadErrorReportUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isUnloadErrorReportUsed = false;
		} else {
			isUnloadErrorReportUsed = true;
		}
	}
	
	private void setUserPassThroughUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isUserPassThroughUsed = true;
		} else {
			isUserPassThroughUsed = false;
		}
//		writeLog(LOGFILENAME, "setUserPassThroughUsed() - Value:" + value + ", Result:" + isUserPassThroughUsed);
	}
	
	private void setVehicleTrafficLogUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isVehicleTrafficLogUsed = true;
		} else {
			isVehicleTrafficLogUsed = false;
		}
//		writeLog(LOGFILENAME, "setVehicleTrafficLogUsed() - Value:" + value + ", Result:" + isVehicleTrafficLogUsed);
	}
	
	private void setVibrationControlUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isVibrationControlUsed = true;
		} else {
			isVibrationControlUsed = false;
		}
//		writeLog(LOGFILENAME, "setVibrationControlUsed() - Value:" + value + ", Result:" + isVibrationControlUsed);
	}
	
	private void setYieldSearchUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isYieldSearchUsed = false;
		} else {
			isYieldSearchUsed = true;
		}
//		writeLog(LOGFILENAME, "setYieldSearchUsed() - Value:" + value + ", Result:" + isYieldSearchUsed);
	}
	
	private void setAreaBalancingLimit(String value) {
		try {
			areaBalancingLimit = Integer.parseInt(value);
			if (areaBalancingLimit < 1) areaBalancingLimit = 1;
			else if (areaBalancingLimit > 10) areaBalancingLimit = 10;
		} catch (Exception e) {
			areaBalancingLimit = 1;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setAreaBalancingInterval(String value) {
		try {
			areaBalancingInterval = Integer.parseInt(value);
			if (areaBalancingInterval < 1) areaBalancingInterval = 1;
			else if (areaBalancingInterval > 30) areaBalancingInterval = 30;
		} catch (Exception e) {
			areaBalancingInterval = 5;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setCommFailCheckTime(String value) {
		// 2014.06.21 by MYM : [Commfail 체크 개선] millisecond 기준으로 변경, 기준 범위를 10 ~ 30초로 재조정
//		try {
//			commFailCheckTime = Integer.parseInt(value);
//			if (commFailCheckTime < 20) commFailCheckTime = 20;
//			else if (commFailCheckTime > 300) commFailCheckTime = 300;
//		} catch (Exception e) {
//			commFailCheckTime = 5;
//			writeExceptionLog(LOGFILENAME, e);
//		}
		try {
			commFailCheckTime = Integer.parseInt(value) * 1000;
			if (commFailCheckTime < 10000) commFailCheckTime = 10000;
			else if (commFailCheckTime > 30000) commFailCheckTime = 30000;
		} catch (Exception e) {
			commFailCheckTime = 10000;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setCommFailCheckTime() - Value:" + value + ", Result:" + commFailCheckTime);
	}
	
	private void setDeadlockDetectedTimeout(String value) {
		try {
			deadlockDetectedTimeout = Integer.parseInt(value) * 1000;
			if (deadlockDetectedTimeout < 30000) deadlockDetectedTimeout = 30000;
			else if (deadlockDetectedTimeout > 600000) deadlockDetectedTimeout = 600000;
		} catch (Exception e) {
			deadlockDetectedTimeout = 120000;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setDynamicRoutingHoldTimeout(String value) {
		try {
			dynamicRoutingHoldTimeout = Integer.parseInt(value) * 1000;
			if (dynamicRoutingHoldTimeout < 10000) dynamicRoutingHoldTimeout = 10000;
			else if (dynamicRoutingHoldTimeout > 600000) dynamicRoutingHoldTimeout = 600000;
		} catch (Exception e) {
			dynamicRoutingHoldTimeout = 20000;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setEstablishCommunicationsTimeout(String value) {
		try {
			establishCommunicationsTimeout = Integer.parseInt(value);
			if (establishCommunicationsTimeout < 3) establishCommunicationsTimeout = 3;
			else if (establishCommunicationsTimeout > 60) establishCommunicationsTimeout = 60;
		} catch (Exception e) {
			establishCommunicationsTimeout = 5;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setCostSearchOption(String value) {
		try {
			costSearchOption = COSTSEARCH_OPTION.toCostSearchOption(value);
		} catch (Exception e) {
			costSearchOption = COSTSEARCH_OPTION.NONE;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setCostSearchOption() - Value:" + value + ", Result:" + costSearchOption);
	}
	
	private void setJobAssignDelayLimit(String value) {
		try {
			delayLimitOfJobAssign = Integer.parseInt(value);
			if (delayLimitOfJobAssign < 1000) delayLimitOfJobAssign = 1000;
			else if (delayLimitOfJobAssign > 20000) delayLimitOfJobAssign = 20000;
		} catch (Exception e) {
			delayLimitOfJobAssign = 6000;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setJobAssignDelayLimit() - Value:" + value + ", Result:" + delayLimitOfJobAssign);
	}
	
	private void setMissedCarrierCheckSleep(String value) {
		try {
			missedCarrierCheckSleep = Long.parseLong(value);
		} catch (Exception e) {
			missedCarrierCheckSleep = 1000;
		}
	}
	
	private void setOperationDelayLimit(String value) {
		try {
			delayLimitOfOperation = Integer.parseInt(value);
			if (delayLimitOfOperation < 10) delayLimitOfOperation = 10;
			else if (delayLimitOfOperation > 500) delayLimitOfOperation = 500;
		} catch (Exception e) {
			delayLimitOfOperation = 100;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setOperationDelayLimit() - Value:" + value + ", Result:" + delayLimitOfOperation);
	}
	
	// 2013.01.04 by KYK
	private void setRfcVerifyingTimeout(String value) {
		rfcVerifyingTimeout = Integer.parseInt(value);
		try {
			if (rfcVerifyingTimeout < 2) rfcVerifyingTimeout = 2;
		} catch (Exception e) {
			rfcVerifyingTimeout = 15;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	// 2013.01.04 by KYK
	private void setStbDataSavePeriod(String value) {
		try {
			stbDataSavePeriod = Integer.parseInt(value);
			if (stbDataSavePeriod < 60) stbDataSavePeriod = 60;
		} catch (Exception e) {
			stbDataSavePeriod = 300;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	// 2013.01.04 by KYK
	private void setStbDataRecoveryOption(String value) {
		stbDataRecoveryOption = value;
		if (value == null || value.length() == 0) {
			stbDataRecoveryOption = MCS;
		}
	}
	
	private void setVehicleCommType(String value) {
		vehicleCommType = VEHICLECOMM_TYPE.toVehicleCommType(value);
		if (value == null || value.length() == 0) {
			vehicleCommType = VEHICLECOMM_TYPE.VEHICLECOMM_CHAR;
		}
	}
	
	// 2013.04.05 by KYK
	private void setNearbyType(String value) {
		nearbyType = NEARBY_TYPE.toNearbyType(value);
		if (value == null || value.length() == 0) {
			nearbyType = NEARBY_TYPE.NEARBY_V3;
		}
	}
	
	private void setFlowControlType(String value) {
		flowControlType = FLOW_CONTROL_TYPE.toFlowControlType(value);
		if (value == null || value.length() == 0) {
			flowControlType = FLOW_CONTROL_TYPE.DQRANGE;
		}
	}
	
	private void setDrivingQueueConvergeLimitTime(String value) {
		try {
			drivingQueueConvergeLimitTime = Double.parseDouble(value);
			if (drivingQueueConvergeLimitTime > 7) drivingQueueConvergeLimitTime = 7;
			else if (drivingQueueConvergeLimitTime < 1) drivingQueueConvergeLimitTime = 1;
		} catch (Exception e) {
			drivingQueueConvergeLimitTime = 2;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setDrivingQueueDivergeLimitTime(String value) {
		try {
			drivingQueueDivergeLimitTime = Double.parseDouble(value);
			if (drivingQueueDivergeLimitTime > 7) drivingQueueDivergeLimitTime = 7;
			else if (drivingQueueDivergeLimitTime < 1) drivingQueueDivergeLimitTime = 1;
		} catch (Exception e) {
			drivingQueueDivergeLimitTime = 2;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	// 2013.04.12 by KYK
	private void setDrivingQueueCurveLimitTime(String value) {
		try {
			drivingQueueCurveLimitTime = Double.parseDouble(value);
			if (drivingQueueCurveLimitTime > 7) drivingQueueCurveLimitTime = 7;
			else if (drivingQueueCurveLimitTime < 1) drivingQueueCurveLimitTime = 1;
		} catch (Exception e) {
			drivingQueueCurveLimitTime = 1.5;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setDrivingQueueLineLimitTime(String value) {
		try {
			drivingQueueLineLimitTime = Double.parseDouble(value);
			if (drivingQueueLineLimitTime > 7) drivingQueueLineLimitTime = 7;
			else if (drivingQueueLineLimitTime < 1) drivingQueueLineLimitTime = 1;
		} catch (Exception e) {
			drivingQueueLineLimitTime = 2;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	// 2013.10.22 by KYK
	private void setStationDataRevision(String value) {
		try {
			stationDataRevision = Integer.parseInt(value);	
		} catch (Exception e) {
			stationDataRevision = 0;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setTeachingDataRevision(String value) {
		try {
			teachingDataRevision = Integer.parseInt(value);
		} catch (Exception e) {
			teachingDataRevision = 0;
			writeExceptionLog(LOGFILENAME, e);
		}
	}	
	
	private void setStbcDelayLimit(String value) {
		try {
			delayLimitOfStbc = Integer.parseInt(value);
			if (delayLimitOfStbc < 10) delayLimitOfStbc = 10;
			else if (delayLimitOfStbc > 1000) delayLimitOfStbc = 1000;
		} catch (Exception e) {
			delayLimitOfStbc = 100;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setCongestionCountThreshold(String value) {
		try {
			congestionCountThreshold = Double.parseDouble(value);
		} catch (Exception e) {
			congestionCountThreshold = 20;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setCongestionIndexThreshold(String value) {
		try {
			congestionIndexThreshold = Double.parseDouble(value);
		} catch (Exception e) {
			congestionIndexThreshold = 10;
			writeExceptionLog(LOGFILENAME, e);
		}
	}

	private void setCongestionPenaltyRangeLimit(String value) {
		try {
			congestionPenaltyRangeLimit = Double.parseDouble(value);
			if (congestionPenaltyRangeLimit < 10) congestionPenaltyRangeLimit = 10;
		} catch (Exception e) {
			congestionPenaltyRangeLimit = 30;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setCongestionPenaltyThreshold(String value) {
		try {
			congestionPenaltyThreshold = Double.parseDouble(value);
		} catch (Exception e) {
			congestionPenaltyThreshold = 10;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setCongestionTimeThreshold(String value) {
		try {
			congestionTimeThreshold = Integer.parseInt(value) * 1000;
		} catch (Exception e) {
			congestionTimeThreshold = 120000;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setDriveFailLimitTime(String value) {
		try {
			// 2015.06.06 by KYK : 오류 재수정
			int driveFailLimit = Integer.parseInt(value);
			if (driveFailLimit < 60) {
				driveFailLimit = 60;
			} else if (driveFailLimit > 600) {
				driveFailLimit = 600;
			}
			driveFailLimitTime = driveFailLimit * 1000;
			
		} catch (Exception e) {
			driveFailLimitTime = 120000;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setDriveLimitTime(String value) {
		try {
			driveLimitTime = Double.parseDouble(value);
			if (driveLimitTime < 2) driveLimitTime = 2;
			else if (driveLimitTime > 20) driveLimitTime = 20;
		} catch (Exception e) {
			driveLimitTime = 7;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setDriveLimitTime() - Value:" + value + ", Result:" + driveLimitTime);
	}
	
	private void setDrivingQueueCurveDistance(String value) {
		try {
			drivingQueueCurveDistance = Integer.parseInt(value);
			if (drivingQueueCurveDistance < 3000) {
				drivingQueueCurveDistance = 4500;
			}			
		} catch (Exception e) {
			drivingQueueCurveDistance = 4500;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setDrivingQueueLineDistance(String value) {
		try {
			drivingQueueLineDistance = Integer.parseInt(value);
			if (drivingQueueLineDistance < 5000) {
				drivingQueueLineDistance = 6000;
			}			
		} catch (Exception e) {
			drivingQueueLineDistance = 6000;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setGoModeCheckTime(String value) {
		try {
			goModeCheckTime = Integer.parseInt(value);
			if (goModeCheckTime < 5) goModeCheckTime = 5;
			else if (goModeCheckTime > 300) goModeCheckTime = 300;
		} catch (Exception e) {
			goModeCheckTime = 60;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setGoModeVehicleDetectedCheckTime(String value) {
		try {
			goModeVehicleDetectedCheckTime = Integer.parseInt(value);
			if (goModeVehicleDetectedCheckTime < 5) goModeVehicleDetectedCheckTime = 5;
			else if (goModeVehicleDetectedCheckTime > 300) goModeVehicleDetectedCheckTime = 300;
		} catch (Exception e) {
			goModeVehicleDetectedCheckTime = 300;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setGoModeVehicleDetectedResetTimeout(String value) {
		try {
			goModeVehicleDetectedResetTimeout = Integer.parseInt(value);
			if (goModeVehicleDetectedResetTimeout < 5) goModeVehicleDetectedResetTimeout = 5;
			else if (goModeVehicleDetectedResetTimeout > 600) goModeVehicleDetectedResetTimeout = 600;
		} catch (Exception e) {
			goModeVehicleDetectedResetTimeout = 600;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setGoModeCarrierStatusCheckUsed(String value) {
		if (NO.equals(value)) {
			isGoModeCarrierStatusCheckUsed = false;
		} else {
			isGoModeCarrierStatusCheckUsed = true;
		}
	}
	
	private void setHistoryDeleteCheckPeriod(String value) {
		try {
			historyDeleteCheckPeriod = Long.parseLong(value) * 1000;
			if (historyDeleteCheckPeriod < 60000) historyDeleteCheckPeriod = 60000;
			else if (historyDeleteCheckPeriod > 86400000) historyDeleteCheckPeriod = 86400000;
		} catch (Exception e) {
			historyDeleteCheckPeriod = 3600000;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setEventHistoryDeleteUsage(String value) {
		if (NO.equals(value)) {
			isEventHistoryDeleteUsed = false;
		} else {
			isEventHistoryDeleteUsed = true;
		}
	}
	
	private void setTrCompletionHistoryDeleteUsage(String value) {
		if (NO.equals(value)) {
			isTrCompletionHistoryDeleteUsed = false;
		} else {
			isTrCompletionHistoryDeleteUsed = true;
		}
	}
	
	private void setVehicleErrorHistoryDeleteUsage(String value) {
		if (NO.equals(value)) {
			isVehicleErrorHistoryDeleteUsed = false;
		} else {
			isVehicleErrorHistoryDeleteUsed = true;
		}
	}
	
	private void setIBSEMHistoryDeleteUsage(String value) {
		if (NO.equals(value)) {
			isIBSEMHistoryDeleteUsed = false;
		} else {
			isIBSEMHistoryDeleteUsed = true;
		}
	}
	
	private void setSTBCHistoryDeleteUsage(String value) {
		if (NO.equals(value)) {
			isSTBCHistoryDeleteUsed = false;
		} else {
			isSTBCHistoryDeleteUsed = true;
		}
	}
	
	private void setHistoryHoldingPeriod(String value) {
		try {
			historyHoldingPeriod = Integer.parseInt(value);
			if (historyHoldingPeriod < 3) historyHoldingPeriod = 3;
			else if (historyHoldingPeriod > 30) historyHoldingPeriod = 30;
		} catch (Exception e) {
			historyHoldingPeriod = 14;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setHistoryHoldingPeriod() - Value:" + value + ", Result:" + historyHoldingPeriod);
	}
	
	private void setJobAssignSearchLimit(String value) {
		try {
			jobAssignSearchLimit = Double.parseDouble(value);
			if (jobAssignSearchLimit < 50) jobAssignSearchLimit = 50;
			else if (jobAssignSearchLimit > 9999) jobAssignSearchLimit = 9999;
		} catch (Exception e) {
			jobAssignSearchLimit = 9999;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setJobAssignSearchLimit() - Value:" + value + ", Result:" + jobAssignSearchLimit);
	}
	
	private void setJobAssignThreshold(String value) {
		try {
			jobAssignThreshold = Double.parseDouble(value);
			if (jobAssignThreshold < 5) jobAssignThreshold = 5;
			else if (jobAssignThreshold > 9999) jobAssignThreshold = 9999;
		} catch (Exception e) {
			jobAssignThreshold = 9999;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setJobAssignThreshold() - Value:" + value + ", Result:" + jobAssignThreshold);
	}
	
	private void setJobAssignLocateThreshold(String value) {
		try {
			jobAssignLocateThreshold = Double.parseDouble(value);
			if (jobAssignLocateThreshold < 5) jobAssignLocateThreshold = 5;
			else if (jobAssignLocateThreshold > 9999) jobAssignLocateThreshold = 9999;
		} catch (Exception e) {
			jobAssignLocateThreshold = 9999;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setJobAssignLocateThreshold() - Value:" + value + ", Result:" + jobAssignLocateThreshold);
	}
	
	private void setJobAssignPriorityThreshold(String value) {
		try {
			jobAssignPriorityThreshold = Double.parseDouble(value);
			if (jobAssignPriorityThreshold < 30) jobAssignPriorityThreshold = 30;
			else if (jobAssignPriorityThreshold > 100) jobAssignPriorityThreshold = 100;
		} catch (Exception e) {
			jobAssignPriorityThreshold = 90;
			writeExceptionLog(LOGFILENAME, e);
		}
	}

	private void setJobAssignPriorityWeight(String value) {
		try {
			jobAssignPriorityWeight = Double.parseDouble(value);
			if (jobAssignPriorityWeight < 0) jobAssignPriorityWeight = 0;
			else if (jobAssignPriorityWeight > 10) jobAssignPriorityWeight = 10;
		} catch (Exception e) {
			jobAssignPriorityWeight = 2;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	private void setJobAssignUrgentThreshold(String value) {
		try {
			jobAssignUrgentThreshold = Double.parseDouble(value);
			if (jobAssignUrgentThreshold < 60) jobAssignUrgentThreshold = 60;
			else if (jobAssignUrgentThreshold > 1000) jobAssignUrgentThreshold = 1000;
		} catch (Exception e) {
			jobAssignUrgentThreshold = 300;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	private void setJobAssignWaitingTimeThreshold(String value) {
		try {
			jobAssignWaitingTimeThreshold = Double.parseDouble(value);
			if (jobAssignWaitingTimeThreshold < 0) jobAssignWaitingTimeThreshold = 0;
			else if (jobAssignWaitingTimeThreshold > 600) jobAssignWaitingTimeThreshold = 600;
		} catch (Exception e) {
			jobAssignWaitingTimeThreshold = 150;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	private void setJobAssignWaitingTimeWeight(String value) {
		try {
			jobAssignWaitingTimeWeight = Double.parseDouble(value);
			if (jobAssignWaitingTimeWeight < 0) jobAssignWaitingTimeWeight = 0;
			else if (jobAssignWaitingTimeWeight > 10) jobAssignWaitingTimeWeight = 10;
		} catch (Exception e) {
			jobAssignWaitingTimeWeight = 1;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setLogHoldingPeriod_JobAssign(String value) {
		try {
			logHoldingPeriod_JobAssign = Integer.parseInt(value);
			if (logHoldingPeriod_JobAssign < 3) logHoldingPeriod_JobAssign = 3;
//			else if (logHoldingPeriod_JobAssign > 30) logHoldingPeriod_JobAssign = 30;
		} catch (Exception e) {
			logHoldingPeriod_JobAssign = 7;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setLogHoldingPeriod_JobAssign() - Value:" + value + ", Result:" + logHoldingPeriod_JobAssign);
	}
	
	private void setLogHoldingPeriod_Operation(String value) {
		try {
			logHoldingPeriod_Operation = Integer.parseInt(value);
			if (logHoldingPeriod_Operation < 3) logHoldingPeriod_Operation = 3;
//			else if (logHoldingPeriod_Operation > 30) logHoldingPeriod_Operation = 30;
		} catch (Exception e) {
			logHoldingPeriod_Operation = 7;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setLogHoldingPeriod_Operation() - Value:" + value + ", Result:" + logHoldingPeriod_Operation);
	}
	
	private void setLogHoldingPeriod_IBSEM(String value) {
		try {
			logHoldingPeriod_IBSEM = Integer.parseInt(value);
			if (logHoldingPeriod_IBSEM < 3) logHoldingPeriod_IBSEM = 3;
//			else if (logHoldingPeriod_IBSEM > 30) logHoldingPeriod_IBSEM = 30;
		} catch (Exception e) {
			logHoldingPeriod_IBSEM = 7;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setLogHoldingPeriod_IBSEM() - Value:" + value + ", Result:" + logHoldingPeriod_IBSEM);
	}
	
	private void setLogHoldingPeriod_Default(String value) {
		try {
			logHoldingPeriod_Default = Integer.parseInt(value);
			if (logHoldingPeriod_Default < 3) logHoldingPeriod_Default = 3;
//			else if (logHoldingPeriod_Default > 30) logHoldingPeriod_Default = 30;
		} catch (Exception e) {
			logHoldingPeriod_Default = 7;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setLogHoldingPeriod_Default() - Value:" + value + ", Result:" + logHoldingPeriod_Default);
	}
	
	private void setMismatchUnloadAppliedPort(String value) {
		try {
			mismatchUnloadAppliedPort = value;
		} catch (Exception e) {
			mismatchUnloadAppliedPort = DEFAULT_MISMATCH_UNLOAD_APPLIED_PORT;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setParkSearchInterval(String value) {
		try {
			parkSearchInterval = Integer.parseInt(value);
			if (parkSearchInterval < 1) parkSearchInterval = 1;
			else if (parkSearchInterval > 10) parkSearchInterval = 10;
		} catch (Exception e) {
			parkSearchInterval = 5;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setParkSearchLimit(String value) {
		try {
			parkSearchLimit = Double.parseDouble(value);
			if (parkSearchLimit < 50) parkSearchLimit = 50;
			else if (parkSearchLimit > 9999) parkSearchLimit = 9999;
		} catch (Exception e) {
			parkSearchLimit = 9999;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setParkSearchLimit() - Value:" + value + ", Result:" + parkSearchLimit);
	}
	
	private void setPriorJobCriteriaOfPriority(String value) {
		try {
			priorJobCriteriaOfPriority = Integer.parseInt(value);
			if (priorJobCriteriaOfPriority < 50) priorJobCriteriaOfPriority = 50;
			else if (priorJobCriteriaOfPriority > 100) priorJobCriteriaOfPriority = 100;
		} catch (Exception e) {
			priorJobCriteriaOfPriority = 100;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setPriorJobCriteriaOfPriority() - Value:" + value + ", Result:" + priorJobCriteriaOfPriority);
	}
	
	private void setPriorJobCriteriaOfWaitingTime(String value) {
		try {
			priorJobCriteriaOfWaitingTime = Integer.parseInt(value);
			if (priorJobCriteriaOfWaitingTime < 30) priorJobCriteriaOfWaitingTime = 30;
			else if (priorJobCriteriaOfWaitingTime > 450) priorJobCriteriaOfWaitingTime = 450;
		} catch (Exception e) {
			priorJobCriteriaOfWaitingTime = 240;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setPriorJobCriteriaOfTransferTime(String value) {
		try {
			priorJobCriteriaOfTransferTime = Integer.parseInt(value);
			if (priorJobCriteriaOfTransferTime < 60) priorJobCriteriaOfTransferTime = 60;
			else if (priorJobCriteriaOfTransferTime > 9999) priorJobCriteriaOfTransferTime = 9999;
		} catch (Exception e) {
			priorJobCriteriaOfTransferTime = 9999;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setVehicleCountPerHid(String value) {
		try {
			vehicleCountPerHid = Integer.parseInt(value);
		} catch (Exception e) {
			vehicleCountPerHid = 20;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setVehicleCountPerHid() - Value:" + value + ", Result:" + vehicleCountPerHid);
	}
	
	private void setVehicleLength(String value) {
		try {
			vehicleLength = Integer.parseInt(value);
		} catch (Exception e) {
			vehicleLength = 500;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setVehicleLength() - Value:" + value + ", Result:" + vehicleLength);
	}
	
	private void setVehicleWidth(String value) {
		try {
			vehicleWidth = Integer.parseInt(value);
		} catch (Exception e) {
			vehicleWidth = 100;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setVehicleWidth() - Value:" + value + ", Result:" + vehicleWidth);
	}
	
	private void setVibrationMonitoringTimeout(String value) {
		try {
			vibrationMonitoringTimeout = Integer.parseInt(value) * 60000L;
			if (vibrationMonitoringTimeout > 14400000L) vibrationMonitoringTimeout = 14400000L;	//	4 hrs
		} catch (Exception e) {
			vibrationMonitoringTimeout = 10800000L;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setVibrationMonitoringTimeout() - Value:" + value + ", Result:" + vibrationMonitoringTimeout);
	}
	
	public void setHoistSpeedLevel(String value) {
		try {
			this.hoistSpeedLevel = Integer.parseInt(value);
			if (this.hoistSpeedLevel > 100) {
				this.hoistSpeedLevel = 100;
			} else if(this.hoistSpeedLevel < 50) {
				this.hoistSpeedLevel = 50;
			}
		} catch (Exception e) {
			this.hoistSpeedLevel = 100;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	public void setShiftSpeedLevel(String value) {
		try {
			this.shiftSpeedLevel = Integer.parseInt(value);
			if (this.shiftSpeedLevel > 100) {
				this.shiftSpeedLevel = 100;
			} else if(this.shiftSpeedLevel < 50) {
				this.shiftSpeedLevel = 50;
			}
		} catch (Exception e) {
			this.shiftSpeedLevel = 100;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setWorkModeCheckTime(String value) {
		try {
			workModeCheckTime = Integer.parseInt(value);
			if (workModeCheckTime < 30) workModeCheckTime = 30;
			else if (workModeCheckTime > 300) workModeCheckTime = 300;
		} catch (Exception e) {
			workModeCheckTime = 60;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setWorkModeCheckTime() - Value:" + value + ", Result:" + workModeCheckTime);
	}
	
	private void setYieldMinLimitTime(String value) {
		try {
			yieldMinLimitTime = Integer.parseInt(value);
			if (yieldMinLimitTime < 1) yieldMinLimitTime = 1;
//			else if (yieldMinLimitTime > 4) yieldMinLimitTime = 4;
		} catch (Exception e) {
			yieldMinLimitTime = 2;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setYieldMinLimitTime() - Value:" + value + ", Result:" + yieldMinLimitTime);
	}
	
	private void setYieldRequestLimitTime(String value) {
		try {
			yieldRequestLimitTime = Integer.parseInt(value);
			if (yieldRequestLimitTime < 1) yieldRequestLimitTime = 1;
//				else if (yieldRequestLimitTime > 20) yieldRequestLimitTime = 20;
		} catch (Exception e) {
			yieldRequestLimitTime = 7;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setYieldRequestLimitTime() - Value:" + value + ", Result:" + yieldRequestLimitTime);
	}
	
	private void setAbortCheckTime(String value) {
		try {
			abortCheckTime = Double.parseDouble(value);
			if (abortCheckTime < 30) abortCheckTime = 30;
			else if (abortCheckTime > 300) abortCheckTime = 300;
		} catch (Exception e) {
			abortCheckTime = 60;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setAbortCheckTime() - Value:" + value + ", Result:" + abortCheckTime);
	}
	
	private void setLoadedVehiclePenalty(String value) {
		try {
			loadedVehiclePenalty = Double.parseDouble(value);
			if (loadedVehiclePenalty < 0) loadedVehiclePenalty = 0;
			else if (loadedVehiclePenalty > 1000) loadedVehiclePenalty = 1000;
		} catch (Exception e) {
			loadedVehiclePenalty = 0;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setLoadedVehiclePenalty() - Value:" + value + ", Result:" + loadedVehiclePenalty);
	}
	
	private void setRepathSearchHoldTimeout(String value) {
		try {
			repathSearchHoldTimeout = Integer.parseInt(value) * 1000;
			if (repathSearchHoldTimeout < 2000) {
				repathSearchHoldTimeout = 2000;
			} else if (repathSearchHoldTimeout > 60000) {
				repathSearchHoldTimeout = 60000;
			}
		} catch (Exception e) {
			repathSearchHoldTimeout = 30000;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setVehicleCurveSpeed(String value) {
		try {
			vehicleCurveSpeed = Double.parseDouble(value);
			if (vehicleCurveSpeed < 100) vehicleCurveSpeed = 100;
			else if (vehicleCurveSpeed > 5000) vehicleCurveSpeed = 5000;
		} catch (Exception e) {
			vehicleCurveSpeed = 1000;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setVehicleCurveSpeed() - Value:" + value + ", Result:" + vehicleCurveSpeed);
	}
	
	private void setVehicleLineSpeed(String value) {
		try {
			vehicleLineSpeed = Double.parseDouble(value);
			if (vehicleLineSpeed < 100) vehicleLineSpeed = 100;
			else if (vehicleLineSpeed > 5000) vehicleLineSpeed = 5000;
		} catch (Exception e) {
			vehicleLineSpeed = 2500;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setVehicleLineSpeed() - Value:" + value + ", Result:" + vehicleLineSpeed);
	}
	
	private void setErrorVehiclePenalty(String value) {
		try {
			errorVehiclePenalty = Double.parseDouble(value);
			if (errorVehiclePenalty < 100) errorVehiclePenalty = 100;
			else if (errorVehiclePenalty > 9999) errorVehiclePenalty = 9999;
		} catch (Exception e) {
			errorVehiclePenalty = 300;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setErrorVehiclePenalty() - Value:" + value + ", Result:" + errorVehiclePenalty);
	}
	
	private void setGoingVehiclePenalty(String value) {
		try {
			goingVehiclePenalty = Double.parseDouble(value);
			if (goingVehiclePenalty < 0) goingVehiclePenalty = 0;
			else if (goingVehiclePenalty > 10) goingVehiclePenalty = 10;
		} catch (Exception e) {
			goingVehiclePenalty = 1;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setGoingVehiclePenalty() - Value:" + value + ", Result:" + goingVehiclePenalty);
	}
	
	private void setManualVehiclePenalty(String value) {
		try {
			manualVehiclePenalty = Double.parseDouble(value);
			if (manualVehiclePenalty < 100) manualVehiclePenalty = 100;
			else if (manualVehiclePenalty > 9999) manualVehiclePenalty = 9999;
		} catch (Exception e) {
			manualVehiclePenalty = 300;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setManualVehiclePenalty() - Value:" + value + ", Result:" + manualVehiclePenalty);
	}
	
	private void setStoppingVehiclePenalty(String value) {
		try {
			stoppingVehiclePenalty = Double.parseDouble(value);
			if (stoppingVehiclePenalty < 1) stoppingVehiclePenalty = 1;
			else if (stoppingVehiclePenalty > 100) stoppingVehiclePenalty = 100;
		} catch (Exception e) {
			stoppingVehiclePenalty = 1;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setStoppingVehiclePenalty() - Value:" + value + ", Result:" + stoppingVehiclePenalty);
	}
	
	private void setWorkingVehiclePenalty(String value) {
		try {
			workingVehiclePenalty = Double.parseDouble(value);
			if (workingVehiclePenalty < 5) workingVehiclePenalty = 5;
			else if (workingVehiclePenalty > 100) workingVehiclePenalty = 100;
		} catch (Exception e) {
			workingVehiclePenalty = 10;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setWorkingVehiclePenalty() - Value:" + value + ", Result:" + workingVehiclePenalty);
	}
	
	private void setCleaningVehiclePenalty(String value) {
		try {
			cleaningVehiclePenalty = Double.parseDouble(value);
			if (cleaningVehiclePenalty < 10) cleaningVehiclePenalty = 10;
			else if (cleaningVehiclePenalty > 1000) cleaningVehiclePenalty = 1000;
		} catch (Exception e) {
			cleaningVehiclePenalty = 10;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setCleaningVehiclePenalty() - Value:" + value + ", Result:" + cleaningVehiclePenalty);
	}
	
	private void setSocketReconnectionTimeout(String value) {
		try {
			socketReconnectionTimeout = Long.parseLong(value) * 1000;
			// 2014.06.21 by MYM : [Commfail 체크 개선] 통신 재접속 간격으로 기준 시간 범위를 1 ~ 30초로 변경, Default는 1초
			if (socketReconnectionTimeout < 5000) socketReconnectionTimeout = 5000;
			else if (socketReconnectionTimeout > 30000) socketReconnectionTimeout = 30000;
		} catch (Exception e) {
			socketReconnectionTimeout = 5000;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setSocketReconnectionTimeout() - Value:" + value + ", Result:" + socketReconnectionTimeout);
	}
	
	private void setSocketCloseCheckTime(String value) {
		// 2014.06.21 by MYM : [Commfail 체크 개선] 통신 재접속 간격으로 기준 시간 범위를 1 ~ 30초로 변경, Default는 1초
		try {
			socketCloseCheckTime = Long.parseLong(value) * 1000;
			if (socketCloseCheckTime < 5000) socketCloseCheckTime = 5000;
			else if (socketCloseCheckTime > 30000) socketCloseCheckTime = 30000;
		} catch (Exception e) {
			socketCloseCheckTime = 5000;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setSocketCloseCheckTime() - Value:" + value + ", Result:" + socketCloseCheckTime);
	}
	
	private void setSystemPauseRequestTimeout(String value) {
		try {
			systemPauseRequestTimeout = Long.parseLong(value) * 1000;
			if (systemPauseRequestTimeout < 20000) systemPauseRequestTimeout = 20000;
			else if (systemPauseRequestTimeout > 300000) systemPauseRequestTimeout = 300000;
		} catch (Exception e) {
			systemPauseRequestTimeout = 60000;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setSocketReconnectionTimeout() - Value:" + value + ", Result:" + socketReconnectionTimeout);
	}
	
	private void setRfReadDevice(String value) {
		rfReadDevice = value;
//		writeLog(LOGFILENAME, "setRfReadDevice() - Value:" + value + ", Result:" + rfReadDevice);
	}
	
	private void setYieldSearchRule(String value) {
		yieldSearchRule = value;
//		writeLog(LOGFILENAME, "setYieldSearchRule() - Value:" + value + ", Result:" + yieldSearchRule);
	}
	
	private void setDispatchingRule(String value) {
		try {
			dispatchingRule = DISPATCHING_RULES.toDispatchingRules(value);
		} catch (Exception e) {
			dispatchingRule = DISPATCHING_RULES.HYBRID2;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setDispatchingRule() - Value:" + value + ", Result:" + dispatchingRule.toConstString());
	}
	
	private void setLocalOHTClearOption(String value) {
		try {
			localOHTClearOption = LOCALGROUP_CLEAROPTION.toLocalGroupClearOption(value);
		} catch (Exception e) {
			localOHTClearOption = LOCALGROUP_CLEAROPTION.UNLOADING_VHL;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setLocalOHTClearOption() - Value:" + value + ", Result:" + localOHTClearOption.toConstString());
	}
	
	private void setOcsControlState(String value) {
		try {
			ocsControlState = OCS_CONTROL_STATE.toOcsControlState(value);
		} catch (Exception e) {
			ocsControlState = OCS_CONTROL_STATE.NULL;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setOcsControlState() - Value:" + value + ", Result:" + ocsControlState.toConstString());
	}
	
	private void setIBSEMUpdate(String value) {
		try {
			ibsemUpdate = RUNTIME_UPDATE.toRuntimeUpdate(value);
		} catch (Exception e) {
			ibsemUpdate = RUNTIME_UPDATE.UNKNOWN;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setIBSEMUpdate() - Value:" + value + ", Result:" + ibsemUpdate.toConstString());
	}
	
	private void setStbcUpdate(String value) {
		try {
			stbcUpdate = RUNTIME_UPDATE.toRuntimeUpdate(value);
		} catch (Exception e) {
			stbcUpdate = RUNTIME_UPDATE.UNKNOWN;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setIBSEMUpdate() - Value:" + value + ", Result:" + ibsemUpdate.toConstString());
	}
	
	private void setJobAssignUpdate(String value) {
		try {
			jobAssignUpdate = RUNTIME_UPDATE.toRuntimeUpdate(value);
		} catch (Exception e) {
			jobAssignUpdate = RUNTIME_UPDATE.UNKNOWN;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setJobAssignUpdate() - Value:" + value + ", Result:" + jobAssignUpdate.toConstString());
	}
	
	private void setOperationUpdate(String value) {
		try {
			operationUpdate = RUNTIME_UPDATE.toRuntimeUpdate(value);
		} catch (Exception e) {
			operationUpdate = RUNTIME_UPDATE.UNKNOWN;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setOperationUpdate() - Value:" + value + ", Result:" + operationUpdate.toConstString());
	}
	
	private void setOperationPortUpdate(String value) {
		try {
			operationPortUpdate = RUNTIME_UPDATE.toRuntimeUpdate(value);
		} catch (Exception e) {
			operationPortUpdate = RUNTIME_UPDATE.UNKNOWN;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setOperationUpdate() - Value:" + value + ", Result:" + operationUpdate.toConstString());
	}
	
	private void setOptimizerUpdate(String value) {
		try {
			optimizerUpdate = RUNTIME_UPDATE.toRuntimeUpdate(value);
		} catch (Exception e) {
			optimizerUpdate = RUNTIME_UPDATE.UNKNOWN;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setOptimizerUpdate() - Value:" + value + ", Result:" + optimizerUpdate.toConstString());
	}
	
	private void setLongRunUpdate(String value) {
		try {
			longRunUpdate = RUNTIME_UPDATE.toRuntimeUpdate(value);
		} catch (Exception e) {
			longRunUpdate = RUNTIME_UPDATE.UNKNOWN;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setLongRunUpdate() - Value:" + value + ", Result:" + longRunUpdate.toConstString());
	}
	
	private void setTscState(String value) {
		try {
			tscState = TSC_STATE.toTscState(value);
		} catch (Exception e) {
			tscState = TSC_STATE.NULL;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setTscState() - Value:" + value + ", Result:" + tscState.toConstString());
	}

	private void setBlockResetTimeout(String value) {
		try {
			blockResetTimeout = Integer.parseInt(value);
			if (blockResetTimeout < 0) blockResetTimeout = 20;
			else if (blockResetTimeout > 60) blockResetTimeout = 60;
		} catch (Exception e) {
			blockResetTimeout = 20;
			writeExceptionLog(LOGFILENAME, e);
		}
//		writeLog(LOGFILENAME, "setBlockResetTimeout() - Value:" + value + ", Result:" + blockResetTimeout);
	}
	
	private void setUserBlockUpdate(String value) {
		// 2013.11.11 by MYM : UserBlock Update도 RuntimeUpdate처럼 처리하도록 개선 (Primary 적용 후 Secondary 적용)
//		if (YES.equals(value)) {
//			isUserBlockUpdate = true;
//		} else {
//			isUserBlockUpdate = false;
//		}
		try {
			userBlockUpdate = RUNTIME_UPDATE.toRuntimeUpdate(value);
		} catch (Exception e) {
			userBlockUpdate = RUNTIME_UPDATE.UNKNOWN;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setRailDownCheckUsed(String value) {
		if (YES.equals(value)) {
			isRailDownCheckUsed = true;
		} else {
			isRailDownCheckUsed = false;
		}
	}
	
	// 2013.02.28 by KYK
	private void setStationUsed(String value) {
		if (YES.equals(value)) {
			isStationUsed = true;
		} else {
			isStationUsed = false;
		}
	}
	
	private void setAutoRetryUsed(String value) {
		if (YES.equals(value)) {
			isAutoRetryUsed = true;
		} else {
			isAutoRetryUsed = false;
		}
	}
	
	// Getter
	
	public boolean isAutoMismatchRecoveryMode() {
		return isAutoMismatchRecoveryMode;
	}
	
	public boolean isBidirectionalSTB() {
		return isBidirectionalSTB;
	}
	
	public boolean isPatrolControlUsed() {
		return isPatrolControlUsed;
	}
	
	public boolean isCollisionNodeCheckUsed() {
		return isCollisionNodeCheckUsed;
	}
	
	public boolean isConveyorHotPriorityUsed() {
		return isConveyorHotPriorityUsed;
	}
	
	public boolean isDynamicRoutingUsed() {
		return isDynamicRoutingUsed;
	}
	
	public boolean isEmulatorMode() {
		return isEmulatorMode;
	}
	
	public boolean isFailureOHTDetourSearchUsed() {
		return isFailureOHTDetourSearchUsed;
	}
	
	public boolean isBlockPreemptionUpdateUsed() {
		return isBlockPreemptionUpdateUsed;
	}
	
	public boolean isSTBReportUsed() {
		return isSTBReportUsed;
	}
	
	public boolean isFoupIdUsed() {
		return isFoupIdUsed;
	}

	public boolean isFormattedLogUsed() {
		return isFormattedLogUsed;
	}
	
	public boolean isHidControlUsed() {
		return isHidControlUsed;
	}
	
	public boolean isHidLimitOverPass() {
		return isHidLimitOverPass;
	}
	
	public boolean isIBSEMUsed() {
		return isIBSEMUsed;
	}
	
	public boolean isIdReaderInstalledOnVehicle() {
		return isIdReaderInstalledOnVehicle;
	}
	
	public boolean isJobAssignDetailResultUsed() {
		return isJobAssignDetailResultUsed;
	}

	// 2015.03.19 by KYK
	public boolean isRFCErrorPortOutOfServiceUsed() {
		return isRFCErrorPortOutOfServiceUsed;
	}
	
	public boolean isLocalOHTUsed() {
		return isLocalOHTUsed;
	}
	
	public boolean isLongRunMoveUsed() {
		return isLongRunMoveUsed;
	}
	
	public boolean isLongRunTransferUsed() {
		return isLongRunTransferUsed;
	}
	
	public boolean isMapDistanceUsed() {
		return isMapDistanceUsed;
	}
	public boolean isMapVehicleSpeedUsed() {
		return isMapVehicleSpeedUsed;
	}
	
	public boolean isMissedCarrierCheckUsed() {
		return isMissedCarrierCheckUsed;
	}
	
	public boolean isNearByDrive() {
		return isNearByDrive;
	}
	
	public boolean isNearByNormalDrive() {
		return isNearByNormalDrive;
	}
	
	public boolean isNextCommandUsed() {
		return isNextCommandUsed;
	}
	
	public boolean isPortDuplicationUsed() {
		return isPortDuplicationUsed;
	}
	
	public boolean isParkNodeUsed() {
		return isParkNodeUsed;
	}
	
	public boolean isRefinePortDupTrCmdUsed() {
		return isRefinePortDupTrCmdUsed;
	}
	
	public boolean isSTBCUsed() {
		return isSTBCUsed;
	}
	
	public boolean isSTBDataSaveUsed() {
		return isSTBDataSaveUsed;
	}
	// 2013.02.28 by KYK
	public boolean isStationUsed() {
		return isStationUsed;
	}
	
	public boolean isSteeringReadyUsed() {
		return isSteeringReadyUsed;
	}

	public boolean isRFCUsed() {
		return isRFCUsed;
	}
	
	public boolean isSystemCollisionUpdate() {
		return isSystemCollisionUpdate;
	}
	
	public boolean isUnloadErrorReportUsed() {
		return isUnloadErrorReportUsed;
	}
	
	public boolean isUserPassThroughUsed() {
		return isUserPassThroughUsed;
	}
	
	public boolean isVehicleTrafficLogUsed() {
		return isVehicleTrafficLogUsed;
	}
	
	public boolean isVibrationControlUsed() {
		return isVibrationControlUsed;
	}
	
	public boolean isYieldSearchUsed() {
		return isYieldSearchUsed;
	}
	
	public boolean isAreaBalancingUsed() {
		return isAreaBalancingUsed;
	}
	
	public boolean isUserBlockUpdate() {
		return isUserBlockUpdate;
	}
	
	public boolean isRailDownCheckUsed() {
		return isRailDownCheckUsed;
	}
	
	public boolean isAutoRetryUsed() {
		return isAutoRetryUsed;
	}
	
	public boolean isGoModeCarrierStatusCheckUsed() {
		return isGoModeCarrierStatusCheckUsed;
	}
	
	public boolean isEventHistoryDeleteUsed() {
		return isEventHistoryDeleteUsed;
	}
	
	public boolean isTrCompletionHistoryDeleteUsed() {
		return isTrCompletionHistoryDeleteUsed;
	}
	
	public boolean isVehicleErrorHistoryDeleteUsed() {
		return isVehicleErrorHistoryDeleteUsed;
	}
	
	public boolean isIBSEMHistoryDeleteUsed() {
		return isIBSEMHistoryDeleteUsed;
	}
	
	public boolean isSTBCHistoryDeleteUsed() {
		return isSTBCHistoryDeleteUsed;
	}
	
	public int getAreaBalancingLimit() {
		return areaBalancingLimit;
	}
	public int getAreaBalancingInterval() {
		return areaBalancingInterval;
	}
	public int getBlockResetTimeout() {
		return blockResetTimeout;
	}
	public int getBlockArrivalRearrangTime() {
		return blockArrivalRearrangTime;
	}
	public int getCommFailCheckTime() {
		return commFailCheckTime;
	}
	public long getDeadlockDetectedTimeout() {
		return deadlockDetectedTimeout;
	}
	public long getDynamicRoutingHoldTimeout() {
		return dynamicRoutingHoldTimeout;
	}
	public long getHistoryDeleteCheckPeriod() {
		return historyDeleteCheckPeriod;
	}
	public int getEstablishCommunicationsTimeout() {
		return establishCommunicationsTimeout;
	}
	public int getJobAssignDelayLimit() {
		return delayLimitOfJobAssign;
	}
	public int getOperationDelayLimit() {
		return delayLimitOfOperation;
	}	
	// 2013.01.04 by KYK
	public int getRfcVerifyingTimeout() {		
		return rfcVerifyingTimeout;
	}
	// 2013.01.04 by KYK
	public int getStbDataSavePeriod() {		
		return stbDataSavePeriod;
	}
	// 2013.01.04 by KYK
	public String getStbDataRecoveryOption() {		
		return stbDataRecoveryOption;
	}
	public VEHICLECOMM_TYPE getVehicleCommType() {		
		return vehicleCommType;
	}
	// 2013.04.05 by KYK
	public NEARBY_TYPE getNearbyType() {		
		return nearbyType;
	}	
	public FLOW_CONTROL_TYPE getFlowControlType() {
		return flowControlType;
	}
	public double getDrivingQueueConvergeLimitTime() {
		return drivingQueueConvergeLimitTime;
	}
	public double getDrivingQueueDivergeLimitTime() {
		return drivingQueueDivergeLimitTime;
	}
	// 2013.04.12 by KYK
	public double getDrivingQueueCurveLimitTime() {
		return drivingQueueCurveLimitTime;
	}
	public double getDrivingQueueLineLimitTime() {
		return drivingQueueLineLimitTime;
	}
	// 2013.10.22 by KYK
	public int getStationDataRevision() {
		return stationDataRevision;
	}
	public int getTeachingDataRevision() {
		return teachingDataRevision;
	}
	
	public int getStbcDelayLimit() {
		return delayLimitOfStbc;
	}
	
	public double getCongestionCountThreshold() {
		return congestionCountThreshold;
	}
	public double getCongestionIndexThreshold() {
		return congestionIndexThreshold;
	}
	public double getCongestionPenaltyRangeLimit() {
		return congestionPenaltyRangeLimit;
	}
	public double getCongestionPenaltyThreshold() {
		return congestionPenaltyThreshold;
	}
	public long getCongestionTimeThreshold() {
		return congestionTimeThreshold;
	}
	public int getDriveFailLimitTime() {
		return driveFailLimitTime;
	}
	public double getDriveLimitTime() {
		return driveLimitTime;
	}
	public int getDrivingQueueCurveDistance() {
		return drivingQueueCurveDistance;
	}
	public int getDrivingQueueLineDistance() {
		return drivingQueueLineDistance;
	}
	public int getGoModeCheckTime() {
		return goModeCheckTime;
	}
	public int getGoModeVehicleDetectedCheckTime() {
		return goModeVehicleDetectedCheckTime;
	}
	public int getGoModeVehicleDetectedResetTimeout() {
		return goModeVehicleDetectedResetTimeout;
	}
	public int getHistoryHoldingPeriod() {
		return historyHoldingPeriod;
	}
	public double getJobAssignSearchLimit() {
		return jobAssignSearchLimit;
	}
	public double getJobAssignThreshold() {
		return jobAssignThreshold;
	}
	public double getJobAssignLocateThreshold() {
		return jobAssignLocateThreshold;
	}
	public double getJobAssignPriorityThreshold() {
		return jobAssignPriorityThreshold;
	}
	public double getJobAssignPriorityWeight() {
		return jobAssignPriorityWeight;
	}
	public double getJobAssignUrgentThreshold() {
		return jobAssignUrgentThreshold;
	}
	public double getJobAssignWaitingTimeThreshold() {
		return jobAssignWaitingTimeThreshold;
	}
	public double getJobAssignWaitingTimeWeight() {
		return jobAssignWaitingTimeWeight;
	}
	public int getLoadRetryLimit() {
		return loadRetryLimit;
	}
	
	public int getLogHoldingPeriod(String module) {
		switch (module.hashCode()) {
			case OcsConstant.OPERATION_HASHCODE:
				return logHoldingPeriod_Operation;
			case OcsConstant.JOBASSIGN_HASHCODE:
				return logHoldingPeriod_JobAssign;
			case OcsConstant.IBSEM_HASHCODE:
				return logHoldingPeriod_IBSEM;
			default:
				return logHoldingPeriod_Default;
		}
	}
	
	public String getMismatchUnloadAppliedPort() {
		return mismatchUnloadAppliedPort;
	}
	
	public long getMissedCarrierCheckSleep() {
		return missedCarrierCheckSleep;
	}
	
	public int getParkSearchInterval() {
		return parkSearchInterval;
	}
	
	public double getParkSearchLimit() {
		return parkSearchLimit;
	}
	
	public int getPriorJobCriteriaOfPriority() {
		return priorJobCriteriaOfPriority;
	}
	
	public int getPriorJobCriteriaOfWaitingTime() {
		return priorJobCriteriaOfWaitingTime;
	}
	
	public int getPriorJobCriteriaOfTransferTime() {
		return priorJobCriteriaOfTransferTime;
	}
	
	public int getUnloadRetryLimit() {
		return unloadRetryLimit;
	}
	
	public int getVehicleCountPerHid() {
		return vehicleCountPerHid;
	}
	
	public int getVehicleLength() {
		return vehicleLength;
	}
	
	public int getVehicleWidth() {
		return vehicleWidth;
	}
	
	public long getVibrationMonitoringTimeout() {
		return vibrationMonitoringTimeout;
	}
	
	public int getHoistSpeedLevel() {
		return hoistSpeedLevel;
	}

	public int getShiftSpeedLevel() {
		return shiftSpeedLevel;
	}

	public int getWorkModeCheckTime() {
		return workModeCheckTime;
	}
	
	public int getYieldMinLimitTime() {
		return yieldMinLimitTime;
	}
	
	public int getYieldRequestLimitTime() {
		return yieldRequestLimitTime;
	}
	
	public double getAbortCheckTime() {
		return abortCheckTime;
	}
	
	public double getLoadedVehiclePenalty() {
		return loadedVehiclePenalty;
	}
	
	public double getVehicleCurveSpeed() {
		return vehicleCurveSpeed;
	}
	
	public double getVehicleLineSpeed() {
		return vehicleLineSpeed;
	}
	
	public double getErrorVehiclePenalty() {
		return errorVehiclePenalty;
	}
	
	public double getManualVehiclePenalty() {
		return manualVehiclePenalty;
	}
	
	public double getGoingVehiclePenalty() {
		return goingVehiclePenalty;
	}
	
	public double getStoppingVehiclePenalty() {
		return stoppingVehiclePenalty;
	}
	
	public double getWorkingVehiclePenalty() {
		return workingVehiclePenalty;
	}

	public double getCleaningVehiclePenalty() {
		return cleaningVehiclePenalty;
	}

	public long getRepathSearchHoldTimeout() {
		return repathSearchHoldTimeout;
	}
	
	public long getSocketReconnectionTimeout() {
		return socketReconnectionTimeout;
	}
	
	public long getSocketCloseCheckTime() {
		return socketCloseCheckTime;
	}
	
	public long getSystemPauseRequestTimeout() {
		return systemPauseRequestTimeout;
	}
	
	public String getRfReadDevice() {
		return rfReadDevice;
	}
	
	public String getYieldSearchRule() {
		return yieldSearchRule;
	}
	
	public DISPATCHING_RULES getDispatchingRule() {
		return dispatchingRule;
	}
	
	public LOCALGROUP_CLEAROPTION getLocalOHTClearOption() {
		return localOHTClearOption;
	}
	
	public OCS_CONTROL_STATE getOcsControlState() {
		return ocsControlState;
	}
	
	public RUNTIME_UPDATE getIBSEMUpdate() {
		return ibsemUpdate;
	}
	public RUNTIME_UPDATE getStbcUpdate() {
		return stbcUpdate;
	}
	
	public RUNTIME_UPDATE getJobAssignUpdate() {
		return jobAssignUpdate;
	}
	
	public RUNTIME_UPDATE getOperationUpdate() {
		return operationUpdate;
	}
	
	public RUNTIME_UPDATE getOperationPortUpdate() {
		return operationPortUpdate;
	}
	
	public RUNTIME_UPDATE getOptimizerUpdate() {
		return optimizerUpdate;
	}
	
	public RUNTIME_UPDATE getLongRunUpdate() {
		return longRunUpdate;
	}
	
	public RUNTIME_UPDATE getUserBlockUpdate() {
		return userBlockUpdate;
	}
	
	public TSC_STATE getTscState() {
		return tscState;
	}
	
	public COSTSEARCH_OPTION getCostSearchOption() {
		return costSearchOption;
	}
	
	
	public int getStageLocateVehicleCount() {
		return stageLocateVehicleCount;
	}

	public boolean isDetourControlUsed() {
		return isDetourControlUsed;
	}
	
	public boolean isTrafficUpdateUsed() {
		return isTrafficUpdateUsed;
	}
	
	public TRAFFIC_UPDATE_RULE getTrafficUpdateRule() {
		return trafficUpdateRule;
	}
	
	public int getDetourPortServiceLimitCount() {
		return detourPortServiceLimitCount;
	}
	
	public SYSTEM_COLLISION_CRITERION getSystemCollisionCriterion() {
		return systemCollisionCriterion;
	}
	
	public void setStageLocateVehicleCount(String value) {
		try {
			stageLocateVehicleCount = Integer.parseInt(value);
			if (stageLocateVehicleCount < 1) {
				stageLocateVehicleCount = 1;
			} else if (stageLocateVehicleCount > 3) {
				stageLocateVehicleCount = 3;
			}
		} catch (Exception e) {
			stageLocateVehicleCount = 3;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	public boolean isStageLocateUsage() {
		return isStageLocateUsage;
	}
	
	private void setStageLocateUsage(String value) {
		//default: YES
		if (NO.equals(value)) {
			isStageLocateUsage = false;
		} else {
			isStageLocateUsage = true;
		}
	}

	/**
	 * Return parameter value of "DEADLOCKBREAK_NEARBYDRIVE_USAGE"
	 * @author zzang9un
	 * @since	2014. 9. 13.
	 * @return Parameter value of "DEADLOCKBREAK_NEARBYDRIVE_USAGE" 
	 */
	public boolean isDeadlockBreakNearbyDriveUsage() {
		return isDeadlockBreakNearbyDriveUsage;
	}

	/**
	 * Set parameter value of "DEADLOCKBREAK_NEARBYDRIVE_USAGE"
	 * @author zzang9un
	 * @since	2014. 9. 13.
	 * @param value - String "YES" or "NO"
	 */
	private void setDeadlockBreakNearbyDriveUsage(String value) {
		if (NO.equals(value)) {
			this.isDeadlockBreakNearbyDriveUsage = false;
		} else {
			this.isDeadlockBreakNearbyDriveUsage = true;
		}
	}
	
	private void setResendCmdForAbnormalReply(String value) {
		// default : YES
		if (NO.equals(value)) {
			this.isResendCmdForAbnormalReply = false;
		} else {
			this.isResendCmdForAbnormalReply = true;
		}
	}
	
	public boolean isResendCmdForAbnormalReply() {
		return this.isResendCmdForAbnormalReply;
	}
	
	/**
	 * Set parameter value of "PASSDOOR_CONTROL_USAGE"
	 * @author zzang9un
	 * @since	2015. 2. 7
	 * @param value - String "YES" or "NO"
	 */
	private void setPassDoorControlUsage(String value) {
		if (NO.equals(value)) {
			this.isPassDoorControlUsage = false;
		} else {
			this.isPassDoorControlUsage = true;
		}
	}
	
	/**
	 * Return parameter value of "PASSDOOR_CONTROL_USAGE"
	 * @author zzang9un
	 * @since	2015. 2. 7
	 * @return Parameter value of "PASSDOOR_CONTROL_USAGE" 
	 */
	public boolean isPassDoorControlUsage() {
		return isPassDoorControlUsage;
	}
	
	// 2015.03.17 by KYK 
	public JOB_RESERVATION_OPTION getJobReservationOption() {
		return jobReservationOption;
	}
	
	private void setJobReservationOption(String value) {
		try {
			jobReservationOption = JOB_RESERVATION_OPTION.toJobReservationOption(value);
		} catch (Exception e) {
			jobReservationOption = JOB_RESERVATION_OPTION.JR1;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	public int getJobReservationLimitTime() {
		return jobReservationLimitTime;
	}
	
	private void setJobReservationLimitTime(String value) {
		try {
			jobReservationLimitTime = Integer.parseInt(value);
		} catch (Exception e) {
			jobReservationLimitTime = 300;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	// 2015.05.01 by KYK [Commfail Report]
	private void setCommfailAlarmReportUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isCommfailAlarmReportUsed = true;
		} else {
			isCommfailAlarmReportUsed = false;
		}
	}
	
	public boolean isCommfailAlarmReportUsed() {
		return isCommfailAlarmReportUsed;
	}
	
	private void setCarrierTypeMismatchUsed(String value) {
		//default: YES
		if (NO.equals(value)) {
			isCarrierTypeMismatchUsed = false;
		} else {
			isCarrierTypeMismatchUsed = true;
		}
	}
	
	public boolean isCarrierTypeMismatchUsed() {
		return isCarrierTypeMismatchUsed;
	}
	
	// 2015.07.08 by KYK
	public int getConsecutiveYieldLimitTime() {
		return consecutiveYieldLimitTime;
	}
	
	private void setConsecutiveYieldLimitTime(String value) {
		try {
			consecutiveYieldLimitTime = Integer.parseInt(value);
			if (consecutiveYieldLimitTime < 1) {
				consecutiveYieldLimitTime = 1;
			} else if (consecutiveYieldLimitTime > 8) {
				consecutiveYieldLimitTime = 8;
			}
		} catch (Exception e) {
			consecutiveYieldLimitTime = 4;
		}
	}

	private void setPriorJobDispatchingUsed(String value) {
		//default: NO
		if (YES.equals(value)) {
			isPriorJobDispatchingUsed = true;
		} else {
			isPriorJobDispatchingUsed = false;
		}		
	}
	
	public boolean isPriorJobDispatchingUsed() {
		return isPriorJobDispatchingUsed;
	}
	
	public PRIORJOB_DISPATCHING_RULE getPriorJobDispatchingRule() {
		return priorJobDispatchingRule;
	}
	
	public void setPriorJobDispatchingRule(String value) {
		try {
			priorJobDispatchingRule = PRIORJOB_DISPATCHING_RULE.toPriorJobDispatchingRule(value);
		} catch (Exception e) {
			priorJobDispatchingRule = PRIORJOB_DISPATCHING_RULE.EQPRIORITY;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	public int getPriorJobPriorityThreshold() {
		return priorJobPriorityThreshold;
	}
	
	public int getPriorJobWaitingTimeThreshold() {
		return priorJobWaitingTimeThreshold;
	}
	
	private void setPriorJobPriorityThreshold(String value) {
		try {
			priorJobPriorityThreshold = Integer.parseInt(value);
			if (priorJobPriorityThreshold < 10) priorJobPriorityThreshold = 10;
			else if (priorJobPriorityThreshold > 100) priorJobPriorityThreshold = 100;
		} catch (Exception e) {
			priorJobPriorityThreshold = 90;
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void setPriorJobWaitingTimeThreshold(String value) {
		try {
			priorJobWaitingTimeThreshold = Integer.parseInt(value);
			if (priorJobWaitingTimeThreshold < 0) priorJobWaitingTimeThreshold = 0;
			else if (priorJobWaitingTimeThreshold > 3600) priorJobWaitingTimeThreshold = 3600;
		} catch (Exception e) {
			priorJobWaitingTimeThreshold = 600;
			writeExceptionLog(LOGFILENAME, e);
		}		
	}

	
	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}

	private void setIgnoreMaterialDifference(String value) {
		//default: NO
		if (YES.equals(value)) {
			isIgnoreMaterialDifference = true;
		} else {
			isIgnoreMaterialDifference = false;
		}		
	}
	
	public boolean isIgnoreMaterialDifference() {
		return isIgnoreMaterialDifference;
	}

	public boolean isCarrierTypeUsage() {
		return isCarrierTypeUsage;
	}

	public void setCarrierTypeUsage(String value) {
		//default: NO
		if (YES.equals(value)) {
			isCarrierTypeUsage = true;
		} else {
			isCarrierTypeUsage = false;
		}
	}
	
	public boolean isOperationManagerLoggingUsage() {
		return isOperationManagerLoggingUsage;
	}

	public void setOperationManagerLoggingUsage(String value) {
		//default: NO
		if (YES.equals(value)) {
			isOperationManagerLoggingUsage = true;
		} else {
			isOperationManagerLoggingUsage = false;
		}
	}

	private void setIsParkCmdEqoption(String value){
		//default : NO
		if (YES.equals(value)){
			isParkCmdEqoption = true;
		} else {
			isParkCmdEqoption = false;
		}
	}
	
	public boolean isParkCmdEqoption(){
		return isParkCmdEqoption;
	}
	
	private void setStageSourceDupCancelUsage(String value){
		//default : NO
		if (YES.equals(value)){
			isStageSourceDupCancelUsage = true;
		} else {
			isStageSourceDupCancelUsage = false;
		}
	}
	
	public boolean isStageSourceDupCancelUsage(){
		return isStageSourceDupCancelUsage;
	}
	
	private void setAreaBalancingManualExclude(String value){ // 2022.12.14 by JJW : Area 내  Max VHL 초과시  Manual VHL 제외 여부
		//default : NO
		if (YES.equals(value)){
			isAreaBalancingManualExclude = true;
		} else {
			isAreaBalancingManualExclude = false;
		}
	}
	
	public boolean isAreaBalancingManualExclude(){
		return isAreaBalancingManualExclude;
	}
}