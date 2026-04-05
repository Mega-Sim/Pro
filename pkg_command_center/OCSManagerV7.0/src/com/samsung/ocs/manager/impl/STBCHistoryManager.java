package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.STBCHistory;

/**
 * STBCHistoryManager Class, OCS 3.0 for Unified FAB
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

public class STBCHistoryManager extends AbstractManager {
	private static STBCHistoryManager manager;
	private Vector<STBCHistory> register;
	
	/**
	 * Constructor of STBCHistoryManager class.
	 */
	private STBCHistoryManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(STBCHistory.class)){
			this.register = new Vector<STBCHistory>();
			if (managerThread != null){
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of STBCHistoryManager class. (Singleton)
	 */
	public static synchronized STBCHistoryManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart,	boolean makeManagerThread, long interval){
		if (manager == null){
			manager = new STBCHistoryManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, interval);
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
			registerSTBCHistory();
		}
		return true;
	}
	
	/**
	 * 
	 * @param stbcHistory
	 */
	public void addSTBCHistoryToRegisterDB(STBCHistory stbcHistory){
		if (register.contains(stbcHistory) == false){
			register.add(stbcHistory);
		}
	}

	private static final String REGISTER_SQL = "INSERT INTO STBCHISTORY (MSGDIRECTION, MSGTYPE, MSG, EVENTTIME) VALUES (?,?,?,?)";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean registerSTBCHistory() {
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<STBCHistory> registerClone = null;
		try {
			registerClone = (Vector<STBCHistory>)register.clone();
			STBCHistory stbcHistory;
			conn = dbAccessManager.getConnection();
			ListIterator<STBCHistory> iter = registerClone.listIterator();
			while (iter.hasNext()){
				stbcHistory = iter.next();
				if (stbcHistory != null){
					pstmt = conn.prepareStatement(REGISTER_SQL);
					pstmt.setString(1, stbcHistory.getMsgDirection());
					pstmt.setString(2, stbcHistory.getMsgType());
					pstmt.setString(3, stbcHistory.getMsg());
					pstmt.setString(4, stbcHistory.getEventTime());		
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

	private static String DELETE_SQL = "DELETE FROM STBCHISTORY WHERE EVENTTIME < ?";
	/**
	 * 
	 * @param timeBefore
	 * @return
	 */
	public boolean deleteSTBCHistoryFromDB(String timeBefore) {
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
		writeLog(LOGFILENAME, "Deletion Time:" + (System.currentTimeMillis() - startTime));
		return result;
	}	
}