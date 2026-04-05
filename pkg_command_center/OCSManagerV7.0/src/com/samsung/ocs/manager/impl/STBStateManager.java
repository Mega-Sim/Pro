package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.STBState;

/**
 * STBStateManager Class, OCS 3.0 for Unified FAB
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

public class STBStateManager extends AbstractManager {
	private static STBStateManager manager;
	private static final String STATENAME = "STATENAME";
	private static final String STATE = "STATE";
	private static final String VALUE = "VALUE";

	/**
	 * Constructor of STBStateManager class.
	 */
	private STBStateManager(Class<?> vOType, DBAccessManager dbAccessManager, 
			boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(STBState.class)){
			if (managerThread != null){
				managerThread.setRunFlag(true);
			}			
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of STBStateManager class. (Singleton)
	 */
	public static synchronized STBStateManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null){
			manager = new STBStateManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	@Override
	protected void init() {
		updateFromDB();
	}
	@Override
	protected boolean updateToDB() {
		return false;
	}	
	
	private static final String SELECT_SQL = "SELECT * FROM STBSTATE";
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
			while (rs.next()){
				String stateName = rs.getString(STATENAME);
				STBState stbState = (STBState) data.get(stateName);
				if (stbState == null){
					stbState = (STBState) vOType.newInstance();
					data.put(stateName, stbState);
					stbState.setStateName(stateName);
				}
				stbState.setState(rs.getString(STATE));
				stbState.setValue(rs.getString(VALUE));
				removeKeys.remove(stateName);
			}
			for (String rmKey: removeKeys){
				data.remove(rmKey);
			}
			result = true;
			
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (InstantiationException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (IllegalAccessException e) {
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
		if (result == false){
			dbAccessManager.requestDBReconnect();
		}		
		return result;
	}

	/**
	 * 
	 * @param stateName
	 * @return
	 */
	public STBState getStbStateData(String stateName) {
		STBState stbState = (STBState) data.get(stateName);
		return stbState;
	}
}