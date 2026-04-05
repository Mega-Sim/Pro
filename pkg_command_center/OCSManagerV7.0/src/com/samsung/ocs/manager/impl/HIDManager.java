package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_CONTROL_LEVEL;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;
import com.samsung.ocs.manager.impl.model.Hid;

/**
 * HIDManager Class, OCS 3.0 for Unified FAB
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

public class HIDManager extends AbstractManager {
	private static HIDManager manager = null;
	private static final String UNITID = "UNITID";
	private static final String ALTHID = "ALTHID";
	private static final String BACKUPHID = "BACKUPHID";
	private static final String ELECTRICCURRENT = "ELECTRICCURRENT";
	private static final String ERRORCODE = "ERRORCODE";
	private static final String FREQUENCY = "FREQUENCY";
	private static final String HEIGHT = "HEIGHT";
	private static final String IPADDRESS = "IPADDRESS";
	private static final String LEFT = "LEFT";
	private static final String REMOTECMD = "REMOTECMD";
	private static final String STATUS = "STATUS";
	private static final String TEMPERATURE = "TEMPERATURE";
	private static final String TOP = "TOP";
	private static final String TYPE = "TYPE";
	private static final String VOLTAGE = "VOLTAGE";
	private static final String WIDTH = "WIDTH";
	private static final String DETOURCONTROLLEVEL = "DETOURCONTROLLEVEL";
	
	private boolean isRuntimeUpdateRequested = false;
	private boolean isRuntimeUpdatable = false;
	private boolean isUnregisteredHidListJournaled = false;
	// 2014.10.13 by MYM : 장애 지역 우회 기능
	private Vector<Hid> abnormalHidList = null;
	private boolean isOperation = false;
	
	/**
	 * Constructor of HIDManager class.
	 */
	private HIDManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		LOGFILENAME = this.getClass().getName();

		// 2014.10.13 by MYM : 장애 지역 우회 기능
		abnormalHidList = new Vector<Hid>();
		
		if (vOType != null && vOType.getClass().isInstance(Hid.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of HIDManager class. (Singleton)
	 */
	public static synchronized HIDManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new HIDManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	/**
	 * 
	 */
	@Override
	protected void init() {
		// 2011.11.09 by PMM
		initialize();
		configureMultiBackup();
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
	
	// 2011.11.04 by PMM
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
//	private static final String SELECT_SQL = "SELECT A.HID UNITID, B.TYPE, B.ENABLED, B.STATUS, B.VOLTAGE, B.ELECTRICCURRENT, B.TEMPERATURE, B.FREQUENCY, B.ERRORCODE, B.ALTHID, B.BACKUPHID, B.REMOTECMD, B.IPADDRESS, B.LEFT, B.TOP, B.WIDTH, B.HEIGHT FROM (SELECT DISTINCT HID FROM NODE) A, (SELECT * FROM UNITDEVICE WHERE TYPE='HID') B WHERE B.UNITID (+)= A.HID ORDER BY LPAD(A.HID, 3, '0')";
	private static final String SELECT_UNITDEVICE_SQL = "SELECT * FROM UNITDEVICE WHERE TYPE='HID' ORDER BY LPAD(UNITID, 3, '0')";
	private static final String SELECT_NODE_SQL = "SELECT DISTINCT HID AS UNITID FROM NODE ORDER BY LPAD(HID, 3, '0')";
	
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
			pstmt = conn.prepareStatement(SELECT_UNITDEVICE_SQL);
			rs = pstmt.executeQuery();
			String unitId;
			Hid hid;
			while (rs.next()) {
				unitId = rs.getString(UNITID);
				hid = (Hid) data.get(unitId);
				if (hid == null) {
					hid = (Hid) vOType.newInstance();
					data.put(unitId, hid);
				}
				setHid(hid, rs);
			}
			rs.close();
			pstmt.close();
			// 2014.02.06 by KYK
			int unregisteredHidCount = 0;
			StringBuilder unregisteredHid = new StringBuilder();
			
			pstmt = conn.prepareStatement(SELECT_NODE_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				unitId = rs.getString(UNITID);
				if (unitId != null && unitId.length() > 0) {
					if (data.containsKey(unitId) == false) {
						hid = (Hid) vOType.newInstance();
						hid.setUnregisteredHid(unitId);
						data.put(unitId, hid);
						// 2014.02.06 by KYK
						if (unregisteredHidCount > 0) unregisteredHid.append(",");
						unregisteredHid.append(unitId);
						unregisteredHidCount++;
					}
				}
			}
			// 2014.02.06 by KYK
			if (unregisteredHidCount > 0 && isUnregisteredHidListJournaled == false) {
				StringBuilder message = new StringBuilder();
				message.append("Unregistered Hid:");
				message.append(" [").append(unregisteredHidCount).append("] ");
				message.append(unregisteredHid.toString());
				writeExceptionLog(LOGFILENAME, message.toString());
				isUnregisteredHidListJournaled = true;
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
	private void configureMultiBackup() {
		Hid hid;
		for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
			hid = (Hid) e.nextElement();
			hid.setAltHid((Hid) data.get(hid.getAltHidName()));
			hid.setBackupHid((Hid) data.get(hid.getBackupHidName()));
		}
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
		Set<String> removeKeys = new HashSet<String>(data.keySet());
//		int unregisteredHidCount = 0;
//		StringBuilder unregisteredHid = new StringBuilder();
		// 2014.10.13 by MYM : 장애 지역 우회 기능
		Vector<Hid> lastAbnormalHidList = new Vector<Hid>();

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_UNITDEVICE_SQL);
			rs = pstmt.executeQuery();
			String unitId;
			Hid hid;
			while (rs.next()) {
				if (isRuntimeUpdateRequested) {
					return false;
				}
				unitId = rs.getString(UNITID);
				hid = (Hid) data.get(unitId);
				if (hid == null) {
					hid = (Hid) vOType.newInstance();
					data.put(unitId, hid);
				}
				setHid(hid, rs);
				removeKeys.remove(unitId);
				
				// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
				// 2014.10.13 by MYM : 장애 지역 우회 기능
				if (isOperation && hid.isAbnormalState()) {
					lastAbnormalHidList.add(hid);
				}
			}
			rs.close();
			pstmt.close();
			// 2014.02.06 by KYK
//			pstmt = conn.prepareStatement(SELECT_NODE_SQL);
//			rs = pstmt.executeQuery();
//			while (rs.next()) {
//				unitId = rs.getString(UNITID);
//				if (unitId != null && unitId.length() > 0) {
//					if (data.containsKey(unitId) == false) {
//						hid = (Hid) vOType.newInstance();
//						hid.setUnregisteredHid(unitId);
//						data.put(unitId, hid);
//						if (unregisteredHidCount > 0) unregisteredHid.append(",");
//						unregisteredHid.append(unitId);
//						unregisteredHidCount++;
//					}
//					removeKeys.remove(unitId);
//				}
//			}
//			for (String rmKey : removeKeys) {
//				data.remove(rmKey);
//			}
//			result = true;
//			
//			if (unregisteredHidCount > 0 && isUnregisteredHidListJournaled == false) {
//				StringBuilder message = new StringBuilder();
//				message.append("Unregistered Hid:");
//				message.append(" [").append(unregisteredHidCount).append("] ");
//				message.append(unregisteredHid.toString());
//				writeExceptionLog(LOGFILENAME, message.toString());
//				isUnregisteredHidListJournaled = true;
//			}
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
		
		// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
		// 2014.10.13 by MYM : 장애 지역 우회 기능
		if (isOperation && result) {
			// abnormal -> normal
			for (int i = abnormalHidList.size() - 1; i >= 0; i--) {
				Hid hid = abnormalHidList.get(i);
				if (lastAbnormalHidList.remove(hid) == false) {
					hid.releaseAbnormalSection(false);
					abnormalHidList.remove(i);
				}			
			}
			
			// normal -> abnormal 
			for (int i = 0; i < lastAbnormalHidList.size(); i++) {
				Hid hid = lastAbnormalHidList.get(i);
				if (hid != null) {
					hid.setAbnormalSection(DETOUR_REASON.HID_DOWN);
					hid.checkRepathSearch();
					abnormalHidList.add(hid);
				}
			}
		}
//		writeExceptionLog(LOGFILENAME, "abnormalHid " + abnormalHidList.toString());
		
		return result;
	}
	
	/**
	 * 
	 * @param hid
	 * @param rs
	 * @exception SQLException
	 */
	private void setHid(Hid hid, ResultSet rs) throws SQLException {
		hid.setUnitId(getString(rs.getString(UNITID)));
		hid.setType(getString(rs.getString(TYPE)));
		hid.setState(getString(rs.getString(STATUS)));
		hid.setErrorcode(rs.getInt(ERRORCODE));
		hid.setElectricCurrent(rs.getDouble(ELECTRICCURRENT));
		hid.setFrequency(rs.getDouble(FREQUENCY));
		hid.setVoltage(rs.getDouble(VOLTAGE));
		hid.setTemperature(rs.getDouble(TEMPERATURE));
		hid.setAltHidName(getString(rs.getString(ALTHID)));
		hid.setBackupHidName(getString(rs.getString(BACKUPHID)));
		hid.setBackupHid((Hid) data.get(hid.getBackupHidName()));
		hid.setIpAddress(getString(rs.getString(IPADDRESS)));
		hid.setRemoteCmd(getString(rs.getString(REMOTECMD)));
		hid.setHeight(rs.getLong(HEIGHT));
		hid.setLeft(rs.getLong(LEFT));
		hid.setTop(rs.getLong(TOP));
		hid.setWidth(rs.getLong(WIDTH));
		// 2014.02.15 by MYM : 장애 지역 우회 기능
		hid.setDetourControlLevel(DETOUR_CONTROL_LEVEL.toReasonType(rs.getString(DETOURCONTROLLEVEL)));
	}
	
	/**
	 * 
	 * @param unitId
	 * @return
	 */
	public Hid getHid(String unitId) {
		return (Hid)data.get(unitId);
	}

	/**
	 * 2015.02.10 by MYM : 장애 지역 우회 기능
	 */
	public void checkChangedSectionEnabled() {
		for (Hid hid : abnormalHidList) {
			hid.setAbnormalSection(DETOUR_REASON.HID_DOWN);
		}
	}
	
	/**
	 * 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
	 * @param isOperation
	 */
	public void setOperation(boolean isOperation) {
		this.isOperation = isOperation;
	}
}
