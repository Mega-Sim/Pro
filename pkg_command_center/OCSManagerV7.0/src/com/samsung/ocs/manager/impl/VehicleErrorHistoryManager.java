package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.VehicleErrorHistory;

/**
 * VehicleErrorHistoryManager Class, OCS 3.0 for Unified FAB
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

public class VehicleErrorHistoryManager extends AbstractManager {
	private static VehicleErrorHistoryManager manager = null;
	private Vector<VehicleErrorHistory> register = new Vector<VehicleErrorHistory>();
	private Vector<VehicleErrorHistory> resetError = new Vector<VehicleErrorHistory>();
	private Vector<VehicleErrorHistory> resetCommFail = new Vector<VehicleErrorHistory>();
	
	public VehicleErrorHistoryManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(VehicleErrorHistory.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	public static synchronized VehicleErrorHistoryManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new VehicleErrorHistoryManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		isInitialized = true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	/**
	 * 
	 */
	@Override
	protected boolean updateToDB() {
		if (register.size() > 0) {
			registerVehicleErrorHistory();
		}
		if (resetError.size() > 0) {
			resetVehicleErrorHistory();
		}
		if (resetCommFail.size() > 0) {
			resetCommFailVehicleErrorHistory();
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	@Override
	protected boolean updateFromDB() {
		return true;
	}
	
	public void addVehicleToRegisterList(VehicleErrorHistory vehicleErrorHistory) {
		if (register.contains(vehicleErrorHistory) == false) {
			register.add(vehicleErrorHistory);
		}
	}

	private static final String REGISER_SQL = 
		"INSERT INTO VEHICLEERRORHISTORY (VEHICLE, NODE, CARRIERLOC, ALARMCODE, ALARMTEXT, SETTIME, CLEARTIME, TYPE, TRSTATUS, TRCMDID, CARRIERID, SHOWMSG, SOURCELOC, DESTLOC) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean registerVehicleErrorHistory() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<VehicleErrorHistory> registerClone = null;
		try {
			registerClone = (Vector<VehicleErrorHistory>)register.clone();
			ListIterator<VehicleErrorHistory> iterator = registerClone.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(REGISER_SQL);
			VehicleErrorHistory vehicleErrorHistory;
			while (iterator.hasNext()) {
				vehicleErrorHistory = iterator.next();
				if (vehicleErrorHistory != null) {
					pstmt.setString(1, vehicleErrorHistory.getVehicle());
					pstmt.setString(2, vehicleErrorHistory.getNode());
					pstmt.setString(3, vehicleErrorHistory.getCarrierLoc());
					pstmt.setInt(4, vehicleErrorHistory.getAlarmCode());
					pstmt.setString(5, vehicleErrorHistory.getAlarmText());
					pstmt.setString(6, vehicleErrorHistory.getSetTime());
					pstmt.setString(7, vehicleErrorHistory.getClearTime());
					pstmt.setString(8, vehicleErrorHistory.getType());
					pstmt.setString(9, vehicleErrorHistory.getTrStatus());
					pstmt.setString(10, vehicleErrorHistory.getTrCmdId());
					pstmt.setString(11, vehicleErrorHistory.getCarrierId());
					pstmt.setString(12, vehicleErrorHistory.getShowMsg());
					pstmt.setString(13, vehicleErrorHistory.getSourceLoc());
					pstmt.setString(14, vehicleErrorHistory.getDestLoc());
					pstmt.execute();
				}
				register.remove(0);
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
	
	public void addVehicleToResetErrorList(VehicleErrorHistory vehicleErrorHistory) {
		if (resetError.contains(vehicleErrorHistory) == false) {
			resetError.add(vehicleErrorHistory);
		}
	}

	private static final String RESET_SQL = 
		"UPDATE VEHICLEERRORHISTORY SET CLEARTIME=? WHERE VEHICLE=? AND SETTIME= (SELECT MAX(SETTIME) FROM VEHICLEERRORHISTORY WHERE VEHICLE=? AND NVL(ALARMTEXT, ' ') <> 'COMM_FAIL' AND NVL(ALARMTEXT, ' ') <> 'DB_DELAY')";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean resetVehicleErrorHistory() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<VehicleErrorHistory> resetErrorClone = null;
		try {
			resetErrorClone = (Vector<VehicleErrorHistory>)resetError.clone();
			ListIterator<VehicleErrorHistory> iterator = resetErrorClone.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(RESET_SQL);
			VehicleErrorHistory vehicleErrorHistory;
			while (iterator.hasNext()) {
				vehicleErrorHistory = iterator.next();
				if (vehicleErrorHistory != null) {
					pstmt.setString(1, vehicleErrorHistory.getClearTime());
					pstmt.setString(2, vehicleErrorHistory.getVehicle());
					pstmt.setString(3, vehicleErrorHistory.getVehicle());
					pstmt.execute();
				}
				resetError.remove(0);
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
	
	public void addVehicleToResetCommFailList(VehicleErrorHistory vehicleErrorHistory) {
		if (resetCommFail.contains(vehicleErrorHistory) == false) {
			resetCommFail.add(vehicleErrorHistory);
		}
	}

	private static final String RESET_COMMFAIL_SQL = 
		"UPDATE VEHICLEERRORHISTORY SET CLEARTIME=? WHERE VEHICLE=? AND SETTIME= (SELECT MAX(SETTIME) FROM VEHICLEERRORHISTORY WHERE VEHICLE=? AND ALARMTEXT='COMM_FAIL')";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean resetCommFailVehicleErrorHistory() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<VehicleErrorHistory> resetCommFailClone = null;
		try {
			resetCommFailClone = (Vector<VehicleErrorHistory>)resetCommFail.clone();
			ListIterator<VehicleErrorHistory> iterator = resetCommFailClone.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(RESET_COMMFAIL_SQL);
			VehicleErrorHistory vehicleErrorHistory;
			while (iterator.hasNext()) {
				vehicleErrorHistory = iterator.next();
				if (vehicleErrorHistory != null) {
					pstmt.setString(1, vehicleErrorHistory.getClearTime());
					pstmt.setString(2, vehicleErrorHistory.getVehicle());
					pstmt.setString(3, vehicleErrorHistory.getVehicle());
					pstmt.execute();
				}
				resetCommFail.remove(0);
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
	
	static String DELETE_HISTORY_SQL = "DELETE FROM VEHICLEERRORHISTORY WHERE SETTIME < ?";
	/**
	 * 
	 * @param timeBefore
	 * @return
	 */
	public boolean deleteHistoryFromDB(String timeBefore) {
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(DELETE_HISTORY_SQL);
			pstmt.setString(1, timeBefore);
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
		writeLog(LOGFILENAME, "Deletion Time:" + (System.currentTimeMillis() - startTime));
		return result;
	}
}
