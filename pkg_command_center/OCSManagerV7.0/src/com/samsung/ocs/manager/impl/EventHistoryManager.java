package com.samsung.ocs.manager.impl;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.EventHistory;

/**
 * EventHistoryManager Class, OCS 3.0 for Unified FAB
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

public class EventHistoryManager extends AbstractManager {
	private static EventHistoryManager manager = null;
	private Vector<EventHistory> registerEventHistoryWithDuplicateCheck = new Vector<EventHistory>();
	private Vector<EventHistory> registerEventHistoryWithoutDuplicateCheck = new Vector<EventHistory>();
	
	/**
	 * Constructor of EventHistoryManager class.
	 */
	private EventHistoryManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(EventHistory.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of EventHistoryManager class. (Singleton)
	 */
	public static synchronized EventHistoryManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new EventHistoryManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
//		updateFromDB();
		isInitialized = true;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		registerEventHistoryWithoutDuplicateCheck();
		registerEventHistoryWithDuplicateCheck();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	@Override
	protected boolean updateFromDB() {
		return true;
	}

	/**
	 * 
	 * @param eventHistory
	 * @param duplicateCheck
	 */
	public void addEventHistoryToRegisterList(EventHistory eventHistory, boolean duplicateCheck) {
		if (duplicateCheck) {
			if (registerEventHistoryWithDuplicateCheck.contains(eventHistory) == false) {
				registerEventHistoryWithDuplicateCheck.add(eventHistory);
			}
		} else {
			if (registerEventHistoryWithoutDuplicateCheck.contains(eventHistory) == false) {
				registerEventHistoryWithoutDuplicateCheck.add(eventHistory);
			}
		}
	}
	
	private static final String REGISTER_WITHOUT_DUPLICATECHECK_SQL = "INSERT INTO EVENTHISTORY(NAME,TYPE,SUBTYPE,EVENT,SETTIME,CLEARTIME,REMOTEID,REMOTEIP,REASON) VALUES(?, ?, ?, ?, TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISS'), '', ?, ?, ?)";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean registerEventHistoryWithoutDuplicateCheck() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		EventHistory eventHistory;
		ListIterator<EventHistory> iterator;
		Vector<EventHistory> registerEventHistoryWithoutDuplicateCheckClone = null;
		boolean result = false;
		try {
			registerEventHistoryWithoutDuplicateCheckClone = (Vector<EventHistory>)registerEventHistoryWithoutDuplicateCheck.clone();
			iterator = registerEventHistoryWithoutDuplicateCheckClone.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(REGISTER_WITHOUT_DUPLICATECHECK_SQL);
			while (iterator.hasNext()) {
				eventHistory = iterator.next();
				if (eventHistory != null) {
					if (eventHistory.getRemoteIp() == null || (eventHistory.getRemoteIp() != null && eventHistory.getRemoteIp().length() == 0)) {
						InetAddress ipAddress = InetAddress.getLocalHost();
						eventHistory.setRemoteIp(ipAddress.getHostAddress());
					}
					pstmt.setString(1, eventHistory.getName().toConstString());
					pstmt.setString(2, eventHistory.getType().toConstString());
					pstmt.setString(3, eventHistory.getSubType());
					pstmt.setString(4, eventHistory.getEvent());
					pstmt.setString(5, eventHistory.getRemoteId().toConstString());
					pstmt.setString(6, eventHistory.getRemoteIp());
					pstmt.setString(7, eventHistory.getReason().toConstString());
					pstmt.execute();
				}
				registerEventHistoryWithoutDuplicateCheck.remove(0);
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
	
	private static final String REGISTER_WITH_DUPLICATECHECK_SQL = "MERGE INTO " +
			"EVENTHISTORY USING DUAL ON (EVENTHISTORY.NAME=? AND EVENTHISTORY.EVENT=? AND EVENTHISTORY.SETTIME >= TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MI')-500) WHEN NOT MATCHED THEN INSERT (NAME, TYPE, SUBTYPE, EVENT, SETTIME, REMOTEID, REMOTEIP, REASON) VALUES (?, ?, ?, ?, TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISS'), ?, ?, ?)";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean registerEventHistoryWithDuplicateCheck() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		EventHistory eventHistory;
		ListIterator<EventHistory> iterator;
		Vector<EventHistory> registerEventHistoryWithDuplicateCheckClone = null;
		boolean result = false;
		try {
			registerEventHistoryWithDuplicateCheckClone = (Vector<EventHistory>)registerEventHistoryWithDuplicateCheck.clone();
			iterator = registerEventHistoryWithDuplicateCheckClone.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(REGISTER_WITH_DUPLICATECHECK_SQL);
			while (iterator.hasNext()) {
				eventHistory = iterator.next();
				if (eventHistory != null) {
					if (eventHistory.getRemoteIp() == null || (eventHistory.getRemoteIp() != null && eventHistory.getRemoteIp().length() == 0)) {
						InetAddress ipAddress = InetAddress.getLocalHost();
						eventHistory.setRemoteIp(ipAddress.getHostAddress());
					}
					pstmt.setString(1, eventHistory.getName().toConstString());
					pstmt.setString(2, eventHistory.getEvent());
					pstmt.setString(3, eventHistory.getName().toConstString());
					pstmt.setString(4, eventHistory.getType().toConstString());
					pstmt.setString(5, eventHistory.getSubType());
					pstmt.setString(6, eventHistory.getEvent());
					pstmt.setString(7, eventHistory.getRemoteId().toConstString());
					pstmt.setString(8, eventHistory.getRemoteIp());
					pstmt.setString(9, eventHistory.getReason().toConstString());
					pstmt.execute();
				}
				registerEventHistoryWithDuplicateCheck.remove(0);
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
	
	static String deleteHistorySql = "DELETE FROM EVENTHISTORY WHERE SETTIME < ?";
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
			pstmt = conn.prepareStatement(deleteHistorySql);
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
