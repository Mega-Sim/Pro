package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_CONTROL_LEVEL;
import com.samsung.ocs.manager.impl.model.VehicleError;

/**
 * VehicleErrorManager Class, OCS 3.0 for Unified FAB
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

public class VehicleErrorManager extends AbstractManager {
	private static VehicleErrorManager manager = null;
	private static final String ERROR_CODE = "ERRORCODE";
	private static final String ERROR_TEXT = "ERRORTEXT";
	private static final String CLASSIFICATION = "CLASSIFICATION";
	private static final String TROUBLE_COST = "TROUBLECOST";
	private static final String ACTIONTYPE = "ACTIONTYPE";
	private static final String DETOURCONTROLLEVEL = "DETOURCONTROLLEVEL";
	private static final int DOUBLE_STORAGE_HASHCODE = -85301235; // "Double_Storage":-85301235
	private static final int EMPTY_RETRIEVAL_HASHCODE = 2069013122; // "Empty_Retrieval":2069013122

	// 2011.11.09 by PMM
	private boolean isRuntimeUpdateRequested = false;
	
	// 2011.12.05 by PMM
	// private boolean isRuntimeUpdatable = false;
	private boolean isRuntimeUpdatable = true;
	
	/**
	 * Constructor of VehicleErrorManager class.
	 */
	private VehicleErrorManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(VehicleError.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of VehicleErrorManager class. (Singleton)
	 */
	public static synchronized VehicleErrorManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new VehicleErrorManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		initialize();
		isInitialized = true;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		// 2011.11.09 by PMM
		if (isRuntimeUpdateRequested == false) {
			isRuntimeUpdatable = false;
			if (isInitialized) {
				updateFromDBImpl();
			} else {
				// 어떤 경우?? 예방 차원
				init();
			}
		} 
		isRuntimeUpdatable = true;
		return true;
	}
	
	// 2011.11.09 by PMM
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB() {
		isRuntimeUpdateRequested = true;
		isInitialized = false;
		int count = 0;
		while (true) {
			if (isRuntimeUpdatable || count > 200) {
				break;
			}
			count++;
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
				break;
			}
		}
		data.clear();
		
		init();
		isRuntimeUpdateRequested = false;
	}
	
	// 2011.11.09 by PMM
	private static String SELECT_SQL = "SELECT * FROM VEHICLEERROR ORDER BY ERRORCODE";
	/**
	 * 
	 * @return
	 */
	private boolean initialize() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			String errorCode = "";
			VehicleError vehicleError = null;
			while (rs.next()) {
				errorCode = rs.getString(ERROR_CODE);
				vehicleError = (VehicleError) data.get(errorCode);
				if (vehicleError == null) {
					vehicleError = (VehicleError) vOType.newInstance();
					data.put(errorCode, vehicleError);
				}
				setVehicleError(vehicleError, rs);
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
	
	// 2011.11.09 by PMM
	/**
	 * 
	 */
	private boolean updateFromDBImpl() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			String errorCode = "";
			VehicleError vehicleError = null;
			while (rs.next()) {
				if (isRuntimeUpdateRequested) {
					return false;
				}
				errorCode = rs.getString(ERROR_CODE);
				vehicleError = (VehicleError) data.get(errorCode);
				if (vehicleError == null) {
					vehicleError = (VehicleError) vOType.newInstance();
					data.put(errorCode, vehicleError);
				}
				setVehicleError(vehicleError, rs);
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
	 * @param vehicleError
	 * @param rs
	 * @exception SQLException
	 */
	private void setVehicleError(VehicleError vehicleError, ResultSet rs ) throws SQLException {
		vehicleError.setErrorCode(rs.getInt(ERROR_CODE));
		vehicleError.setErrorText(getString(rs.getString(ERROR_TEXT)));
		vehicleError.setClassification(getString(rs.getString(CLASSIFICATION)));
		vehicleError.setTroubleCost(rs.getInt(TROUBLE_COST));
		
		// 2012.01.03 by MYM : ActionType 추가
		vehicleError.setActionType(getString(rs.getString(ACTIONTYPE)));
		// 2014.02.15 by MYM : 장애 지역 우회 기능
		vehicleError.setDetourControlLevel(DETOUR_CONTROL_LEVEL.toReasonType(rs.getString(DETOURCONTROLLEVEL)));
	}

	/**
	 * 
	 * @param errorCode
	 * @return errorText
	 */
	public String getVehicleErrorText(int errorCode) {
		// 2011.12.05 by PMM
//		if (data.containsKey(errorCode)) {
//			return ((VehicleError)data.get(errorCode)).getErrorText();
//		} else {
//			return "";
//		}
		if (data.containsKey(String.valueOf(errorCode))) {
			return ((VehicleError)data.get(String.valueOf(errorCode))).getErrorText();
		} else {
			return "";
		}
	}
	
	/**
	 * 
	 * @param errorCode
	 * @return
	 */
	public String getVehicleErrorType(int errorCode) {
		// 2011.12.05 by PMM
		if (data.containsKey(String.valueOf(errorCode))) {
			return ((VehicleError)data.get(String.valueOf(errorCode))).getClassification();
		} else {
			return "";
		}
	}
	
	/**
	 * 
	 * @param errorCode
	 * @return
	 */
	public boolean isDoubleStorageInSTBorUTBPort(int errorCode) {
		if (data.containsKey(String.valueOf(errorCode))) {
			return ((VehicleError)data.get(String.valueOf(errorCode))).getActionTypeHashCode() == DOUBLE_STORAGE_HASHCODE;
		} else if (errorCode == 850 || errorCode == 855){
			// 850 : STB에 Foup이 존재함[Loading시 STB 감지센서 OFF상태(반사판감지안됨)]
			// 855 : UTB에 Foup이 존재함[Loading시 UTB 감지센서 OFF상태(반사판감지안됨)]
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param errorCode
	 * @return
	 */
	public boolean isEmptyRetrievalInSTBorUTBPort(int errorCode) {
		if (data.containsKey(String.valueOf(errorCode))) {
			return ((VehicleError)data.get(String.valueOf(errorCode))).getActionTypeHashCode() == EMPTY_RETRIEVAL_HASHCODE;
		} else if (errorCode == 851 || errorCode == 856){
			// 851 : STB에 Foup이 존재하지 않음[Unloading시 STB 감지센서 ON상태(반사판감지됨)]
			// 856 : UTB에 Foup이 존재하지 않음[Unloading시 UTB 감지센서 ON상태(반사판감지됨)]
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 2015.02.15 by MYM : 장애 지역 우회 기능
	 * @param errorCode
	 * @return
	 */
	public DETOUR_CONTROL_LEVEL getDetourControlLevel(int errorCode) {
		if (data.containsKey(String.valueOf(errorCode))) {
			return ((VehicleError)data.get(String.valueOf(errorCode))).getDetourControlLevel();
		}
		return DETOUR_CONTROL_LEVEL.LEVEL0;
	}

	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}

}
