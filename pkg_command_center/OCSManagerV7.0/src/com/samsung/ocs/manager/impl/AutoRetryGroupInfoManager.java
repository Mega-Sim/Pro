package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.AutoRetryGroupInfo;

/**
 * AutoRetryGroupInfo Class, OCS 3.1 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2012. 8. 21.
 * @version 3.1
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class AutoRetryGroupInfoManager extends AbstractManager {
	private static AutoRetryGroupInfoManager manager = null;
	
	private static final String GROUPID = "GROUPID";
	private static final String UNLOADENABLED = "UNLOADENABLED";
	private static final String UNLOADCOUNT = "UNLOADCOUNT";
	private static final String LOADENABLED = "LOADENABLED";
	private static final String LOADCOUNT = "LOADCOUNT";
	private static final String UNLOADPAUSETIME = "UNLOADPAUSETIME";
	private static final String LOADPAUSETIME = "LOADPAUSETIME";
	// 2012.09.20 by KYK
	private static final String LASTUNLOADERRORENABLED = "LASTUNLOADERRORENABLED"; 
	private static final String LASTLOADERRORENABLED = "LASTLOADERRORENABLED"; 

	private static final String TRUE = "TRUE";

	public AutoRetryGroupInfoManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(AutoRetryGroupInfo.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	 /**
	 * Constructor of RetryControlManager class. (Singleton)
	 */
	public static synchronized AutoRetryGroupInfoManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new AutoRetryGroupInfoManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	@Override
	protected void init() {
		updateFromDB();
		isInitialized = true;
	}

	private static final String SELECT_SQL = "SELECT * FROM AUTORETRYGROUPINFO";
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
			while (rs.next()) {
				String groupId = rs.getString(GROUPID);
				AutoRetryGroupInfo autoRetryGroupInfo = (AutoRetryGroupInfo) data.get(groupId);
				if (autoRetryGroupInfo == null) {
					autoRetryGroupInfo = (AutoRetryGroupInfo) vOType.newInstance();
					autoRetryGroupInfo.setGroupId(groupId);
					data.put(groupId, autoRetryGroupInfo);
				}
				setAutoRetryGroupInfo(autoRetryGroupInfo, rs);
				removeKeys.remove(groupId);
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

	@Override
	protected boolean updateToDB() {
		return true;
	}

	private void setAutoRetryGroupInfo(AutoRetryGroupInfo autoRetryGroupInfo, ResultSet rs ) {
		if (autoRetryGroupInfo != null && rs != null) {
			try {
				autoRetryGroupInfo.setUnloadEnabled(TRUE.equals(getString(rs.getString(UNLOADENABLED))));
				autoRetryGroupInfo.setUnloadCount(rs.getInt(UNLOADCOUNT));
				autoRetryGroupInfo.setLoadEnabled(TRUE.equals(getString(rs.getString(LOADENABLED))));
				autoRetryGroupInfo.setLoadCount(rs.getInt(LOADCOUNT));
				autoRetryGroupInfo.setUnloadPauseTime(rs.getInt(UNLOADPAUSETIME));
				autoRetryGroupInfo.setLoadPauseTime(rs.getInt(LOADPAUSETIME));
				// 2012.09.20 by KYK
				autoRetryGroupInfo.setLastUnloadErrorEnabled(TRUE.equals(getString(rs.getString(LASTUNLOADERRORENABLED))));
				autoRetryGroupInfo.setLastLoadErrorEnabled(TRUE.equals(getString(rs.getString(LASTLOADERRORENABLED))));
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
				e.printStackTrace();
			}
		}
	}
	
	public AutoRetryGroupInfo getAutoRetryGroupInfo(String retryGroupId) {
		if (retryGroupId != null) {
			return (AutoRetryGroupInfo) data.get(retryGroupId);
		}
		return null;
	}
}
