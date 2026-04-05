package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.UserDefinedPath;

/**
 * UserDefinedPathManager Class, OCS 3.0 for Unified FAB
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

public class UserDefinedPathManager extends AbstractManager {
	private static UserDefinedPathManager manager = null;
	private static final String NAME = "NAME";
	private static final String FROMLOCATION = "FROMLOCATION";
	private static final String TOLOCATION = "TOLOCATION";
	private static final String NODELIST = "NODELIST";
	private static final String VEHICLELIMIT = "VEHICLELIMIT";

	/**
	 * Constructor of UserDefinedPathManager class.
	 */
	private UserDefinedPathManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(UserDefinedPath.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of UserDefinedPathManager class. (Singleton)
	 */
	public static synchronized UserDefinedPathManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new UserDefinedPathManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	@Override
	protected void init() {
		updateFromDB();
		isInitialized = true;
	}

	private static final String SELECT_SQL = "SELECT * FROM USERDEFINEDPATH";
	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		String name;
		UserDefinedPath userDefinedPath;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				name = rs.getString(NAME);
				userDefinedPath = (UserDefinedPath)data.get(name);
				if (userDefinedPath == null) {
					userDefinedPath = (UserDefinedPath) vOType.newInstance();
					userDefinedPath.setName(rs.getString(NAME));
					data.put(name, userDefinedPath);
				}
				setUserDefinedPath(userDefinedPath, rs);
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

	@Override
	protected boolean updateToDB() {
		return true;
	}
	
	/**
	 * 
	 * @param userDefinedPath
	 * @param rs
	 * @exception SQLException
	 */
	private void setUserDefinedPath(UserDefinedPath userDefinedPath, ResultSet rs) throws SQLException {
		userDefinedPath.setFromLocation(getString(rs.getString(FROMLOCATION)));
		userDefinedPath.setToLocation(getString(rs.getString(TOLOCATION)));
		userDefinedPath.setNodeList(getString(rs.getString(NODELIST)));
		userDefinedPath.setVehicleLimit(rs.getInt(VEHICLELIMIT));
	}
	
	/**
	 * 
	 * @param sourceGroupId
	 * @param destGroupId
	 * @return
	 */
	public UserDefinedPath getUserDefinedPath(String sourceGroupId, String destGroupId) {
		for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
			UserDefinedPath userDefinedPath = (UserDefinedPath) e.nextElement();
			// 2012.06.27 by MYM : from, to location을 리스트로 설정
//			if (sourceGroupId.equals(userDefinedPath.getFromLocation()) &&
//					destGroupId.equals(userDefinedPath.getToLocation())) {
//				return userDefinedPath;
//			}
			if (userDefinedPath.containsFromLocation(sourceGroupId)
					&& userDefinedPath.containsToLocation(destGroupId)) {
				return userDefinedPath;
			}
		}
		return null;
	}
}
