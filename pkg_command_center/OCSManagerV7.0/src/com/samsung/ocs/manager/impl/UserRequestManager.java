package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant.CYCLE_UNIT;
import com.samsung.ocs.common.constant.OcsConstant.USERREQUEST_TYPE;
import com.samsung.ocs.manager.impl.model.UserRequest;

/**
 * UserRequestManager Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 3. 7.
 * @version 3.0
 * 
 * Copyright 2013 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class UserRequestManager extends AbstractManager {
	private static UserRequestManager manager = null;
	private static final String USERREQUESTID = "USERREQUESTID";
	private static final String TYPE = "TYPE";
	private static final String VEHICLEID = "VEHICLEID";
	private static final String LOADINGBYPASS = "LOADINGBYPASS";
//	private static final String ENABLED = "ENABLED";
	private static final String TOURLIST = "TOURLIST";
	private static final String CYCLE = "CYCLE";
	private static final String CYCLEUNIT = "CYCLEUNIT";
	private static final String DOCKINGSTATIONID = "DOCKINGSTATIONID";
	private static final String LASTTOUREDTIME = "LASTTOUREDTIME";
	private static final String PATROLMODE = "PATROLMODE";
	
	/**
	 * Constructor of UserRequestManager class.
	 */
	private UserRequestManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(UserRequest.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of UserRequestManager class. (Singleton)
	 */
	public static synchronized UserRequestManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new UserRequestManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
		return true;
	}
	
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB() {
		data.clear();
		init();
	}
	
	private static final String SELECT_SQL = "SELECT * FROM USERREQUEST WHERE ENABLED = 'TRUE'";
	/**
	 * 
	 */
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
			
			String userRequestId = null;
			UserRequest userRequest = null;
			while (rs.next()) {
				userRequestId = rs.getString(USERREQUESTID);
				userRequest = (UserRequest) data.get(userRequestId);
				if (userRequest == null) {
					userRequest = (UserRequest) vOType.newInstance();
					userRequest.setUserRequestId(userRequestId);
					userRequest.setEnabled(true);
					if (setUserRequest(userRequest, rs)) {
						data.put(userRequestId, userRequest);
					}
				} else {
					updateUserRequest(userRequest, rs);
				}
				removeKeys.remove(userRequestId);
			}
			for (String rmKey : removeKeys) {
				if (rmKey != null) {
					userRequest = (UserRequest) data.remove(rmKey);
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			writeExceptionLog(LOGFILENAME, ie);
			ie.printStackTrace();
		} catch (InstantiationException e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		} catch (NullPointerException e) {			
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		} catch (Exception e) {			
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
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
		return result;
	}

	/**
	 * 
	 * @param userRequest
	 * @param rs
	 */
	private void updateUserRequest(UserRequest userRequest, ResultSet rs) {
		try {
			if (userRequest != null && rs != null) {
				userRequest.setCycle(rs.getInt(CYCLE));
				updateCycleUnit(userRequest, getString(rs.getString(CYCLEUNIT)));
				userRequest.setLastTouredTime(getString(rs.getString(LASTTOUREDTIME)));
				userRequest.setPatrolMode(rs.getInt(PATROLMODE));
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	/**
	 * 
	 * @param userRequest
	 * @param rs
	 */
	private boolean setUserRequest(UserRequest userRequest, ResultSet rs) {
		try {
			if (userRequest != null && rs != null) {
				userRequest.setTypeString(getString(rs.getString(TYPE)));
				userRequest.setType(getUserRequestType(getString(rs.getString(TYPE))));
				userRequest.setVehicleId(getString(rs.getString(VEHICLEID)));
				userRequest.setCycle(rs.getInt(CYCLE));
				updateCycleUnit(userRequest, getString(rs.getString(CYCLEUNIT)));
				userRequest.setDockingStationId(getString(rs.getString(DOCKINGSTATIONID)));
				userRequest.setLoadingByPass(getBoolean(rs.getString(LOADINGBYPASS)));
				userRequest.setLastTouredTime(getString(rs.getString(LASTTOUREDTIME)));
				userRequest.setTourListString(getString(rs.getString(TOURLIST)));
				userRequest.setPatrolMode(rs.getInt(PATROLMODE));
				return true;
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		return false;
	}
	
	private void updateCycleUnit(UserRequest userRequest, String cycleUnitString) {
		if (userRequest != null && cycleUnitString != null) {
			userRequest.setCycleUnitString(cycleUnitString);
			userRequest.setCycleUnit(getCycleUnit(cycleUnitString));
		}
	}
	
	private USERREQUEST_TYPE getUserRequestType(String typeString) {
		if (typeString != null && typeString.length() > 0) {
//				case 'P':	// Patrol
//				case 'V':	// Vibration
//				case 'R':	// RandomTransfer
//				case 'F':	// FixedTransfer
//				case 'M':	// Move (Default)
			return USERREQUEST_TYPE.toUserRequestType(typeString.charAt(0));
		}
		return USERREQUEST_TYPE.UNDEFINED;			// Undefined
	}
	
	private CYCLE_UNIT getCycleUnit(String cycleUnitString) {
		if (cycleUnitString != null && cycleUnitString.length() > 0) {
//				case 'D':	// Day (Default)
//				case 'H':	// Hour
//				case 'M':	// Minute
//				case 'S':	// Second
			return CYCLE_UNIT.toCycleUnit(cycleUnitString.charAt(0));
		}
		return CYCLE_UNIT.UNDEFINED;			// Undefined
	}
	
	private static final String RESET_PATROL_REQUEST_SQL = "UPDATE USERREQUEST SET ENABLED='FALSE' WHERE USERREQUESTID=?";
	/**
	 * 
	 * @param userRequestId
	 * @return
	 */
	synchronized public boolean disableUserRequest(String userRequestId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(RESET_PATROL_REQUEST_SQL);
			pstmt.setString(1, userRequestId);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
	private static final String UPDATE_LASTTOUREDTIME_SQL = "UPDATE USERREQUEST SET LASTTOUREDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE USERREQUESTID=?";
	/**
	 * 
	 * @param userRequestId
	 * @return
	 */
	synchronized public boolean updateLastTouredTime(String userRequestId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_LASTTOUREDTIME_SQL);
			pstmt.setString(1, userRequestId);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
}
