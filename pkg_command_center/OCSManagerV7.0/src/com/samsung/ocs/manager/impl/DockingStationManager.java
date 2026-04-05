package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.DockingStation;

/**
 * DockingStationManager Class, OCS 3.0 for Unified FAB
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
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class DockingStationManager extends AbstractManager {
	private static DockingStationManager manager = null;
	private static final String DOCKINGSTATIONID = "DOCKINGSTATIONID";
	private static final String ENABLED = "ENABLED";
	private static final String IPADDRESS = "IPADDRESS";
	private static final String STATIONMODE = "STATIONMODE";
	private static final String CARRIEREXIST = "CARRIEREXIST";
	private static final String CARRIERCHARGED = "CARRIERCHARGED";
	private static final String ALARMID = "ALARMID";
	private static final String CARRIERID = "CARRIERID";

	/**
	 * Constructor of DockingStationManager class.
	 */
	private DockingStationManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(DockingStation.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of DockingStationManager class. (Singleton)
	 */
	public static synchronized DockingStationManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new DockingStationManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
	
	private static final String SELECT_SQL = "SELECT * FROM DOCKINGSTATION";
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
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
			
			String dockingStationId = null;
			DockingStation dockingStation = null;
			while (rs.next()) {
				dockingStationId = rs.getString(DOCKINGSTATIONID);
				dockingStation = (DockingStation) data.get(dockingStationId);
				if (dockingStation == null) {
					dockingStation = (DockingStation) vOType.newInstance();
					dockingStation.setDockingStationId(dockingStationId);
					if (setDockingStation(dockingStation, rs)) {
						data.put(dockingStationId, dockingStation);
					}
				} else {
					setDockingStation(dockingStation, rs);
				}
				removeKeys.remove(dockingStationId);
			}
			for (String rmKey : removeKeys) {
				if (rmKey != null) {
					dockingStation = (DockingStation) data.remove(rmKey);
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
	 * @param dockingStation
	 * @param rs
	 */
	private boolean setDockingStation(DockingStation dockingStation, ResultSet rs) {
		try {
			if (dockingStation != null && rs != null) {
				dockingStation.setEnabled(getBoolean(rs.getString(ENABLED)));
				dockingStation.setIPAddress(getString(rs.getString(IPADDRESS)));
				dockingStation.setStationMode(getString(rs.getString(STATIONMODE)).charAt(0));
				dockingStation.setCarrierExist(rs.getInt(CARRIEREXIST));
				dockingStation.setCarrierCharged(rs.getInt(CARRIERCHARGED));
				dockingStation.setAlarmId(rs.getInt(ALARMID));
				dockingStation.setCarrierId(getString(rs.getString(CARRIERID)));
				return true;
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		return false;
	}
	
	public boolean updateManuallyFromDB() {
		return updateFromDB();
	}
	
	/**
	 * 
	 * @param dockingStationId
	 * @return
	 */
	public DockingStation getDockingStationData(String dockingStationId) {
		try {
			if (dockingStationId != null) {
				return (DockingStation) data.get(dockingStationId);
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		return null;
	}
}
