package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.LocalGroupInfo;

/**
 * LocalGroupInfoManager Class, OCS 3.0 for Unified FAB
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

public class LocalGroupInfoManager extends AbstractManager {
	private static LocalGroupInfoManager manager = null;
	private static final String LOCALGROUPID = "LOCALGROUPID";
	private static final String BAY = "BAY";
	private static final String ENABLED = "ENABLED";
	private static final String MINVHL = "MINVHL";
	private static final String MAXVHL = "MAXVHL";
	private static final String SETVHL = "SETVHL";
//	private static final String CURVHL = "CURVHL";
	private static final String BAYVHL = "BAYVHL";
	private static final String DISTANCE = "DISTANCE";
	private static final String EXPIREDTIME = "EXPIREDTIME";
	private static final String UPDATETIME = "UPDATETIME";
	private static final String ASSIGNOPTION = "ASSIGNOPTION";
	private static final String OCSREGISTERED = "OCSREGISTERED";
	private static final String TRUE = "TRUE";
	private boolean isAllLocalGroupInfoCleared = false;
	
	/**
	 * Constructor of LocalGroupInfoManager class.
	 */
	private LocalGroupInfoManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(LocalGroupInfo.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}

	/**
	 * Constructor of LocalGroupInfoManager class. (Singleton)
	 */
	public static synchronized LocalGroupInfoManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new LocalGroupInfoManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		updateFromDB();
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
		updateLocalGroupInfoFromDB();
		return true;
	}
	
	private static final String SELECT_SQL = "SELECT * FROM LOCALGROUPINFO WHERE ENABLED='TRUE' ORDER BY LOCALGROUPID";
	/**
	 * 
	 * @return
	 */
	private boolean updateLocalGroupInfoFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		String localGroupId;
		LocalGroupInfo localGroupInfo;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				localGroupId = rs.getString(LOCALGROUPID);
				localGroupInfo = (LocalGroupInfo)data.get(localGroupId);
				if (localGroupInfo == null) {
					localGroupInfo = (LocalGroupInfo) LocalGroupInfo.class.newInstance();
					data.put(localGroupId, localGroupInfo);
				}
				setLocalGroupInfo(localGroupInfo, rs);
				removeKeys.remove(localGroupId);
				
				// 2012.03.02 by PMM
				isAllLocalGroupInfoCleared = false;
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
	 * @param localGroupInfo
	 * @param rs
	 * @exception SQLException
	 */
	private void setLocalGroupInfo(LocalGroupInfo localGroupInfo, ResultSet rs) throws SQLException {
		// 2012.03.02 by PMM
		// [NotNullCheck] 추가
		if (localGroupInfo != null && rs != null) {
			localGroupInfo.setLocalGroupId(getString(rs.getString(LOCALGROUPID)));
			localGroupInfo.setBay(getString(rs.getString(BAY)));
			localGroupInfo.setEnabled(getBoolean(rs.getString(ENABLED)));
			localGroupInfo.setMinVHL(rs.getInt(MINVHL));
			localGroupInfo.setMaxVHL(rs.getInt(MAXVHL));
			localGroupInfo.setSetVHL(rs.getInt(SETVHL));
			//localGroupInfo.setCurVHL(rs.getInt(CURVHL));
			localGroupInfo.setBayVHL(rs.getInt(BAYVHL));
			localGroupInfo.setDistance(rs.getInt(DISTANCE));
			localGroupInfo.setExpiredTime(getString(rs.getString(EXPIREDTIME)));
			localGroupInfo.setUpdateTime(getString(rs.getString(UPDATETIME)));
			localGroupInfo.setAssignOption(getString(rs.getString(ASSIGNOPTION)));
			localGroupInfo.setOcsRegistered(getString(rs.getString(OCSREGISTERED)).equals(TRUE));
		} else {
			writeExceptionLog(LOGFILENAME, "setLocalGroupInfo(LocalGroupInfo localGroupInfo, ResultSet rs) - one of parameters is null.");
		}
	}
	
	private static final String UPDATE_EXPIREDTIME_SQL = "UPDATE LOCALGROUPINFO SET EXPIREDTIME=(CASE WHEN (NVL(EXPIREDTIME,0)-(SYSDATE - NVL(UPDATETIME,SYSDATE))*86400) < 0 THEN 0 ELSE (NVL(EXPIREDTIME,0)-(SYSDATE - NVL(UPDATETIME,SYSDATE))*86400) END), UPDATETIME=SYSDATE";
	// 2014.03.28 by MYM : [사용자 정의 LocalGroup 기능] OCS에서 생성한 LocalGroup은 ExpiredTime 미적용
//	private static final String UPDATE_SETVHL_SQL = "UPDATE LOCALGROUPINFO SET SETVHL=CASE WHEN NVL(EXPIREDTIME, 0) = 0 THEN MINVHL ELSE SETVHL END";
	private static final String UPDATE_SETVHL_SQL = "UPDATE LOCALGROUPINFO SET SETVHL=CASE WHEN NVL(EXPIREDTIME, 0) = 0 AND OCSREGISTERED <> 'TRUE' THEN MINVHL ELSE SETVHL END";
	/**
	 * 
	 * @return
	 */
	public boolean updateLocalGroupInfo() {
		boolean result = false;
		Connection conn = null;
		Statement statement = null;
		try {
			conn = dbAccessManager.getConnection();
			statement = conn.createStatement();
			statement.execute(UPDATE_EXPIREDTIME_SQL);
			statement.close();
			statement = conn.createStatement();
			statement.execute(UPDATE_SETVHL_SQL);
			statement.close();
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
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) {}
				statement = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	// 2014.03.28 by MYM : [사용자 정의 LocalGroup 기능] OCS에서 생성한 LocalGroup은 미삭제
//	private static final String DELETE_LOCALGROUPINFO_SQL = "DELETE FROM LOCALGROUPINFO";
	private static final String DELETE_LOCALGROUPINFO_SQL = "DELETE FROM LOCALGROUPINFO WHERE OCSREGISTERED <> 'TRUE'";
	/**
	 * 
	 * @return
	 */
//	public boolean deleteLocalOhtExceptFixed() {
	public boolean clearAllLocalGroupInfo() {
		boolean result = false;
		Connection conn = null;
		Statement statement = null;
		try {
			conn = dbAccessManager.getConnection();
			statement = conn.createStatement();
			statement.execute(DELETE_LOCALGROUPINFO_SQL);
			statement.close();
			isAllLocalGroupInfoCleared = true;
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
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) {}
				statement = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * 
	 * @param localGroupId
	 * @return
	 */
	public LocalGroupInfo getLocalGroupInfo (String localGroupId) {
		return (LocalGroupInfo)data.get(localGroupId);
	}
	
	/**
	 * 
	 * @param localGroupId
	 * @return
	 */
	public String getBay (String localGroupId) {
		if (data.containsKey(localGroupId)) {
			return ((LocalGroupInfo)data.get(localGroupId)).getBay();
		} else {
			return ""; 
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isAllLocalGroupInfoCleared() {
		return isAllLocalGroupInfoCleared;
	}
}
