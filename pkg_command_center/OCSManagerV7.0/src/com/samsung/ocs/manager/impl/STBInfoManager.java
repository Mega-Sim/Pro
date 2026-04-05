package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.STBInfo;

/**
 * STBInfoManager Class, OCS 3.0 for Unified FAB
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

public class STBInfoManager extends AbstractManager {
	private static STBInfoManager manager = null;
	private static final String NAME = "NAME";
	private static final String VALUE = "VALUE";
	
	private Vector<String> update = new Vector<String>();

	/**
	 * Constructor of STBInfoManager class.
	 */
	private STBInfoManager(Class<?> vOType, DBAccessManager dbAccessManager, 
			boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(STBInfo.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of STBInfoManager class. (Singleton)
	 */
	public static synchronized STBInfoManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new STBInfoManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		updateFromDB();
	}

	@Override
	protected boolean updateToDB() {
		updateSTBInfoToDB();
		return true;		
	}
	
	private static final String SELECT_SQL = "SELECT * FROM STBINFO";

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
			String name;
			STBInfo stbInfo;
			while (rs.next()) {
				name = rs.getString(NAME);
				stbInfo = (STBInfo)data.get(name);
				if (stbInfo == null) {
					stbInfo = (STBInfo) vOType.newInstance();
					stbInfo.setName(name);
					data.put(name, stbInfo);
				}
				stbInfo.setValue(rs.getString(VALUE));
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
		} finally {
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
	 * @param name
	 */
	public void updateStbcInfo(String name) {
		if (update.contains(name) == false) {
			update.add(name);			
		}
	}
	
	private static final String UPDATE_STBINFO_SQL = "UPDATE STBINFO SET VALUE=? WHERE NAME=? ";
	/**
	 * 
	 * @return
	 */
	private boolean updateSTBInfoToDB() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			Vector<String> updateTemp = new Vector<String>(update);
			Iterator<String> iterator = updateTemp.iterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_STBINFO_SQL);
			String name;
			STBInfo stbInfo;
			while (iterator.hasNext()) {
				name = iterator.next();
				stbInfo = (STBInfo)data.get(name);
				if (stbInfo != null) {
					pstmt.setString(1, stbInfo.getValue());
					pstmt.setString(2, stbInfo.getName());
					pstmt.execute();
					update.remove(name);
				}
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
		return result;
	}
	
	/**
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public boolean updateSTBInfoToDB(String name, String value) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_STBINFO_SQL);
			pstmt.setString(1, value);
			pstmt.setString(2, name);
			pstmt.executeUpdate();
			result = true;			
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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

	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}	
	
}
