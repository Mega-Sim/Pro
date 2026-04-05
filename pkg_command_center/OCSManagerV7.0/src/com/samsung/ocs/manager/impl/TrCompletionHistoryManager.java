package com.samsung.ocs.manager.impl;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.TrCompletionHistory;

/**
 * TrCompletionHistoryManager Class, OCS 3.0 for Unified FAB
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

public class TrCompletionHistoryManager extends AbstractManager {
	private static TrCompletionHistoryManager manager = null;
	private Vector<TrCompletionHistory> register;
	
	private static final String TRUE = "TRUE";
	private static final String FALSE = "FALSE";
	
	/**
	 * Constructor of TrCompletionHistoryManager class.
	 */
	private TrCompletionHistoryManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(TrCompletionHistory.class)) {
			this.register = new Vector<TrCompletionHistory>();
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of TrCompletionHistoryManager class. (Singleton)
	 */
	public static synchronized TrCompletionHistoryManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new TrCompletionHistoryManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
			registerTrCompletionHistory();
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
	
	/**
	 * 
	 * @param trCompletionHistory
	 */
	public void addTrCmdToRegisterTrCompletionHistory(TrCompletionHistory trCompletionHistory) {
		if (register.contains(trCompletionHistory) == false) {
			register.add(trCompletionHistory);
		}
	}
	
	private static final String REGISTER_SQL = 
//		"INSERT INTO TRCOMPLETIONHISTORY (TRCMDID, PRIORITY, CARRIERID, SOURCELOC, DESTLOC, VEHICLE, TRQUEUEDTIME, UNLOADASSIGNTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNTIME, LOADINGTIME, LOADEDTIME, DELETEDTIME, SOURCENODE, DESTNODE, REMOTECMD, NOBLOCKINGTIME, WAITTIMEOUT, DESTCHANGEDTRCMDID, EXPECTEDDURATION, OCSREGISTERED, VEHICLELOCUS, FOUPID) " +
		"INSERT INTO TRCOMPLETIONHISTORY (TRCMDID, PRIORITY, CARRIERID, SOURCELOC, DESTLOC, VEHICLE, TRQUEUEDTIME, UNLOADASSIGNTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNTIME, LOADINGTIME, LOADEDTIME, DELETEDTIME, SOURCENODE, DESTNODE, REMOTECMD, NOBLOCKINGTIME, WAITTIMEOUT, DESTCHANGEDTRCMDID, EXPECTEDDURATION, OCSREGISTERED, VEHICLELOCUS, FOUPID, " +
		"DELIVERYTYPE, EXPECTEDDELIVERYTIME, DELIVERYWAITTIMEOUT) " +	// 2022.03.14 dahye : Premove Logic Improve
//		"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
		"?, ?, ?)";	// 2022.03.14 dahye : Premove Logic Improve
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean registerTrCompletionHistory() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<TrCompletionHistory> registerClone = null;
		
		try {
			registerClone = (Vector<TrCompletionHistory>)register.clone();
			ListIterator<TrCompletionHistory> iterator = registerClone.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(REGISTER_SQL);
			TrCompletionHistory trCompletionHistory;
			while (iterator.hasNext()) {
				trCompletionHistory = iterator.next();
				if (trCompletionHistory != null) {
					pstmt.setString(1, trCompletionHistory.getTrCmdId());
					pstmt.setInt(2, trCompletionHistory.getPriority());
					pstmt.setString(3, trCompletionHistory.getCarrierId());
					pstmt.setString(4, trCompletionHistory.getSourceLoc());
					pstmt.setString(5, trCompletionHistory.getDestLoc());
					pstmt.setString(6, trCompletionHistory.getVehicle());
					pstmt.setString(7, trCompletionHistory.getTrQueuedTime());
					pstmt.setString(8, trCompletionHistory.getUnloadAssignedTime());
					pstmt.setString(9, trCompletionHistory.getUnloadingTime());
					pstmt.setString(10, trCompletionHistory.getUnloadedTime());
					pstmt.setString(11, trCompletionHistory.getLoadAssignedTime());
					pstmt.setString(12, trCompletionHistory.getLoadingTime());
					pstmt.setString(13, trCompletionHistory.getLoadedTime());
					pstmt.setString(14, trCompletionHistory.getDeletedTime());
					pstmt.setString(15, trCompletionHistory.getSourceNode());
					pstmt.setString(16, trCompletionHistory.getDestNode());
					pstmt.setString(17, trCompletionHistory.getRemoteCmd());
					pstmt.setLong(18, trCompletionHistory.getNoBlockingTime());
					pstmt.setLong(19, trCompletionHistory.getWaitTimeout());
					pstmt.setString(20, trCompletionHistory.getDestChangedTrCmdId());
					pstmt.setLong(21, trCompletionHistory.getExpectedDuration());
					if (trCompletionHistory.isOcsRegistered()) {
						pstmt.setString(22, TRUE);
					} else {
						pstmt.setString(22, FALSE);
					}
					// 2011.10.24 by PMM
					// VehicleLocus 길이 확인.
					
					// 2011.10.29 by PMM
					if (trCompletionHistory.getVehicleLocus() != null) {
						if (trCompletionHistory.getVehicleLocus().length() < 2000) {
							pstmt.setString(23, trCompletionHistory.getVehicleLocus());
						} else if (trCompletionHistory.getVehicleLocus().length() < 4000) {
//							pstmt.setString(23, trCompletionHistory.getVehicleLocus());
							StringReader reader = new StringReader(trCompletionHistory.getVehicleLocus());
							pstmt.setCharacterStream(23, reader, trCompletionHistory.getVehicleLocus().length());
						} else {
//							pstmt.setString(23, trCompletionHistory.getVehicleLocus().substring(0, 3998));
							StringReader reader = new StringReader(trCompletionHistory.getVehicleLocus().substring(0, 4000));
							pstmt.setCharacterStream(23, reader, 4000);
						}
					} else {
						pstmt.setString(23, "");
					}
					// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
					pstmt.setString(24,trCompletionHistory.getFoupId());
					pstmt.setString(25, trCompletionHistory.getDeliveryType());	// 2022.03.14 dahye : TRANSFER_EX4
					pstmt.setLong(26, trCompletionHistory.getExpectedDeliveryTime());	// 2022.03.14 dahye : TRANSFER_EX4
					pstmt.setLong(27, trCompletionHistory.getDeliveryTimeout());	// 2022.03.14 dahye : TRANSFER_EX4
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
	
	// 2018.03.12 by LSH : TRCOMPLETIONHISTORY 테이블 PARTITIONNIG 적용으로 불필요한 조건 삭제 (Partition Key=TRQUEUEDTIME)
	static String DELETE_HISTORY_SQL = "DELETE /*+ INDEX(A IDX_TR_HIST_TRQUEUEDTIME) */ FROM TRCOMPLETIONHISTORY A WHERE TRQUEUEDTIME < ?";
	// 2011.10.29 by PMM
//	static String DELETE_HISTORY_SQL = "DELETE FROM TRCOMPLETIONHISTORY WHERE TRQUEUEDTIME < ? OR (TRQUEUEDTIME IS NULL AND DELETEDTIME < ?)";
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
//			2018.03.12 by LSH : TRCOMPLETIONHISTORY 테이블 PARTITIONNIG 적용으로 불필요한 조건 삭제 (Partition Key=TRQUEUEDTIME)
//			pstmt.setString(2, timeBefore);
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
