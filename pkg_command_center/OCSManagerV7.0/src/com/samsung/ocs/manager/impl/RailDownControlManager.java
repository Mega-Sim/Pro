package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.RailDownControl;

/**
 * RailDownControlManager Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2012. 5. 24.
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

public class RailDownControlManager extends AbstractManager {
	private static RailDownControlManager manager = null;
	
	private static final String AREA = "AREA";
	private static final String NODEID = "NODEID";
	private static final String DIRECTION = "DIRECTION";
	private static final String ENABLED = "ENABLED";
	private static final String DELIMITER = "_";
	
	private ConcurrentHashMap<String, String> nodeList = new ConcurrentHashMap<String, String>();
	

	/**
	 * Constructor of NodeManager class.
	 */
	private RailDownControlManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval );
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(RailDownControl.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of NodeManager class. (Singleton)
	 */
	public static synchronized RailDownControlManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new RailDownControlManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	@Override
	protected void init() {
		updateFromDB();
		isInitialized = true;
	}

	private static final String SELECT_SQL = "SELECT A.*, B.ENABLED FROM RAILDOWNCONTROL A, NODE B WHERE A.NODEID = B.NODEID";
	
	@Override
	protected boolean updateFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			
			ConcurrentHashMap<String, String> removeNodeList = null;
			if (isInitialized) {
				removeNodeList = new ConcurrentHashMap<String, String>(nodeList); 
			}
			
			String nodeId = "";
			RailDownControl railDownControl = null;
			while (rs.next()) {
				StringBuffer key = new StringBuffer();
				key.append(getString(rs.getString(AREA))).append(DELIMITER);
				key.append(getString(rs.getString(DIRECTION)));
				railDownControl = (RailDownControl) data.get(key.toString());
				if (railDownControl == null) {
					railDownControl = (RailDownControl) vOType.newInstance();					
					data.put(key.toString(), railDownControl);
				}
				setRailDownControlInfo(railDownControl, rs);
				
				if (isInitialized) {
					nodeId = rs.getString(NODEID);
					key.append(nodeId);
					nodeList.put(key.toString(), nodeId);
					removeNodeList.remove(key.toString());
				}
			}
			
			if (isInitialized) {
				for (Enumeration<String> e = removeNodeList.keys(); e.hasMoreElements();) {
					String key = e.nextElement();
					nodeId = removeNodeList.get(key);
					String removeKey = key.substring(0, key.indexOf(nodeId));
					railDownControl = (RailDownControl) data.get(removeKey);
					if (railDownControl != null) {
						railDownControl.removeNode(nodeId);
						if (railDownControl.size() == 0) {
							data.remove(removeKey);
						}
					}
					nodeList.remove(key);
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
	
	private void setRailDownControlInfo(RailDownControl railDownControl, ResultSet rs) throws SQLException {
		railDownControl.setArea(getString(rs.getString(AREA)));
		railDownControl.setDirection(getString(rs.getString(DIRECTION)));
		railDownControl.addNode(getString(rs.getString(NODEID)), getString(rs.getString(ENABLED)));
	}

	public boolean isRailAvailable(String area, String direction) {
		StringBuffer key = new StringBuffer(); 
		key.append(area).append(DELIMITER).append(direction);
		RailDownControl railDownControl = (RailDownControl) data.get(key.toString());
		if (railDownControl == null) {
			return true;
		}
		
		return railDownControl.isRailAvailable();
	}
}
