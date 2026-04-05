package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.UnitDevice;

/**
 * UnitDeviceManager Class, OCS 3.0 for Unified FAB
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

public class UnitDeviceManager extends AbstractManager {
	private static UnitDeviceManager manager = null;
	public static final String ALTHID = "ALTHID";
	public static final String BACKUPHID = "BACKUPHID";
	public static final String ELECTRICCURRENT = "ELECTRICCURRENT";
	public static final String ERRORCODE = "ERRORCODE";
	public static final String FREQUENCY = "FREQUENCY";
	public static final String HEIGHT = "HEIGHT";
	public static final String IPADDRESS = "IPADDRESS";
	public static final String LEFT = "LEFT";
	public static final String REMOTECMD = "REMOTECMD";
	public static final String STATUS = "STATUS";
	public static final String TEMPERATURE = "TEMPERATURE";
	public static final String TOP = "TOP";
	public static final String TYPE = "TYPE";
	public static final String UNITID = "UNITID";
	public static final String VOLTAGE = "VOLTAGE";
	public static final String WIDTH = "WIDTH";
	public static final String HID = "HID";
	private ConcurrentHashMap<String, Object> hid = new ConcurrentHashMap<String, Object>();
	
	/**
	 * Constructor of UnitDeviceManager class.
	 */
	private UnitDeviceManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(UnitDevice.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of UnitDeviceManager class. (Singleton)
	 */
	public static synchronized UnitDeviceManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new UnitDeviceManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
	
	private static final String SELECT_SQL = "SELECT * FROM UNITDEVICE";
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
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
			String unitId;
			UnitDevice unitDevice;
			while (rs.next()) {
				unitId = rs.getString(UNITID);
				unitDevice = (UnitDevice) data.get(unitId);
				if (unitDevice == null) {
					unitDevice = (UnitDevice) vOType.newInstance();
					data.put(unitId, unitDevice);
				}
				setUnitDevice(unitDevice, rs);
				removeKeys.remove(unitId);
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
	 * @param unitDevice
	 * @param rs
	 * @exception SQLException
	 */
	private void setUnitDevice(UnitDevice unitDevice, ResultSet rs ) throws SQLException {
		unitDevice.setAltHidName(getString(rs.getString(ALTHID)));
		unitDevice.setBackupHidName(getString(rs.getString(BACKUPHID)));
		unitDevice.setElectricCurrent(rs.getDouble(ELECTRICCURRENT));
		unitDevice.setErrorcode(rs.getInt(ERRORCODE));
		unitDevice.setFrequency(rs.getDouble(FREQUENCY));
		unitDevice.setHeight(rs.getLong(HEIGHT));
		unitDevice.setIpAddress(getString(rs.getString(IPADDRESS)));
		unitDevice.setLeft(rs.getLong(LEFT));
		unitDevice.setRemoteCmd(getString(rs.getString(REMOTECMD)));
		unitDevice.setState(getString(rs.getString(STATUS)));
		unitDevice.setTemperature(rs.getDouble(TEMPERATURE));
		unitDevice.setTop(rs.getLong(TOP));
		unitDevice.setType(getString(rs.getString(TYPE)));
		unitDevice.setUnitId(getString(rs.getString(UNITID)));
		unitDevice.setVoltage(rs.getDouble(VOLTAGE));
		unitDevice.setWidth(rs.getLong(WIDTH));
	}
	
	/**
	 * 
	 * @return
	 */
	public ConcurrentHashMap<String, Object> getHid() {
		hid.clear();
		Set<String> searchKeys = new HashSet<String>(data.keySet());
		UnitDevice unitDevice = null;
		for (String searchKey : searchKeys) {
			unitDevice = (UnitDevice) data.get(searchKey);
			if (HID.equals(unitDevice.getType())) {
				hid.put(unitDevice.getUnitId(), unitDevice);
			}
		}
		return hid;
	}
	
	/**
	 * 
	 * @param unitId
	 * @return
	 */
	public UnitDevice getUnitDevice(String unitId) {
		return (UnitDevice)data.get(unitId);
	}
}
