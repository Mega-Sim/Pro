package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.IBSEMHistory;

/**
 * IBSEMHistoryManager Class, OCS 3.0 for Unified FAB
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

public class IBSEMHistoryManager extends AbstractManager {
	private static IBSEMHistoryManager manager;
	private Vector<IBSEMHistory> register;
	
	/**
	 * Constructor of IBSEMHistoryManager class.
	 */
	private IBSEMHistoryManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(IBSEMHistory.class)){
			this.register = new Vector<IBSEMHistory>();
			if (managerThread != null){
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of IBSEMHistoryManager class. (Singleton)
	 */
	public static synchronized IBSEMHistoryManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart,	boolean makeManagerThread, long interval){
		if (manager == null){
			manager = new IBSEMHistoryManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, interval);
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
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	@Override
	protected boolean updateFromDB() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	/**
	 * 
	 */
	@Override
	protected boolean updateToDB() {
		if (register.size() > 0){
			registerIBSEMHistory();
		}
		return true;
	}
	
	/**
	 * 
	 * @param ibsemHistory
	 */
	public void addIBSEMHistoryToRegisterDB(IBSEMHistory ibsemHistory){
		if (register.contains(ibsemHistory) == false){
			register.add(ibsemHistory);
		}
	}

	private static final String REGISER_SQL = "INSERT INTO IBSEMHISTORY (MSGDIRECTION, MSGTYPE, MSG, EVENTTIME) VALUES (?,?,?,?)";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean registerIBSEMHistory() {
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<IBSEMHistory> registerClone = null;
		try {
			registerClone = (Vector<IBSEMHistory>)register.clone();
			IBSEMHistory ibsemHistory;
			conn = dbAccessManager.getConnection();
			ListIterator<IBSEMHistory> iter = registerClone.listIterator();
			while (iter.hasNext()){
				ibsemHistory = iter.next();
				if (ibsemHistory != null){
					pstmt = conn.prepareStatement(REGISER_SQL);
					pstmt.setString(1, ibsemHistory.getMsgDirection());
					pstmt.setString(2, ibsemHistory.getMsgType());
					pstmt.setString(3, ibsemHistory.getMsg());
					pstmt.setString(4, ibsemHistory.getEventTime());		
					pstmt.execute();
					pstmt.close();
				}
				register.remove(0);
			}
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}

	private static final String DELETE_SQL = "DELETE FROM IBSEMHISTORY WHERE EVENTTIME < ?";
	/**
	 * 
	 * @param timeBefore
	 * @return
	 */
	public boolean deleteIBSEMHistoryFromDB(String timeBefore) {
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;		
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(DELETE_SQL);
			pstmt.setString(1, timeBefore);
			pstmt.executeUpdate();
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
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