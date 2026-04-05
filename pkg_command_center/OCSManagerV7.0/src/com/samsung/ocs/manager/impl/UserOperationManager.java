package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.message.Message;
import com.samsung.ocs.common.message.MsgVector;
import com.samsung.ocs.manager.impl.model.UserOperation;

/**
 * UserOperationManager Class, OCS 3.0 for Unified FAB
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

public class UserOperationManager extends AbstractManager {
	private static UserOperationManager manager = null;
	private static final String MSGSENDER = "MSGSENDER";
	private static final String MSGRECEIVER = "MSGRECEIVER";
	private static final String MSGSTRING = "MSGSTRING";
	private static final String COMMANDTIME = "COMMANDTIME";
	private static final String TRCMD = "TRCMD";
	private static final String TYPE = "Type";
	private static final String TRCMDID = "TrCmdID";
	private static final String CARRIERID = "CarrierID";
	
	private static final String REMOVE = "REMOVE";
	private static final String PAUSE = "PAUSE";
	private static final String RESUME = "RESUME";
	private static final String CANCEL = "CANCEL"; // 2012.08.08 by KYK
	
	private static final int REMOVE_HASHCODE = -1850743644;	// "Remove"
	private static final int PAUSE_HASHCODE = 76887510;	// "Pause"
	private static final int RESUME_HASHCODE = -1850559411;	// "Resume"
	// 2012.08.08 by KYK
	private static final int CANCEL_HASHCODE = 2011110042; // "Cancel"

	private TrCmdManager trCmdManager = null;
	
	/**
	 * Constructor of UserOperationManager class.
	 */
	private UserOperationManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(UserOperation.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of UserOperationManager class. (Singleton)
	 */
	public static synchronized UserOperationManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new UserOperationManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
	
	private static final String SELECT_SQL = "SELECT * FROM USEROPERATION";
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	/**
	 * 
	 */
	protected boolean updateFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			String message = "";
			String commandTime = "";
			String key = "";
			UserOperation userOperation = null;
			while (rs.next()) {
				message = rs.getString(MSGSTRING);
				commandTime = rs.getString(COMMANDTIME);
				key = message + "_" + commandTime;
				userOperation = (UserOperation) data.get(key);
				if (userOperation == null) {
					userOperation = (UserOperation) vOType.newInstance();
					data.put(key, userOperation);
				}
				setUserOperation(userOperation, rs);
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
		
		processUserOperation();
		return result;
	}

	/**
	 * 
	 * @param userOperation
	 * @param rs
	 * @exception SQLException
	 */
	private void setUserOperation(UserOperation userOperation, ResultSet rs ) throws SQLException {
		userOperation.setMsgSender(getString(rs.getString(MSGSENDER)));
		userOperation.setMsgReceiver(getString(rs.getString(MSGRECEIVER)));
		userOperation.setMsgString(getString(rs.getString(MSGSTRING)));
		userOperation.setCommandTime(getString(rs.getString(COMMANDTIME)));
	}
	
	private static final String DELETE_USEROPERATION_SQL = "DELETE FROM USEROPERATION WHERE COMMANDTIME=? AND MSGSTRING=?";
	/**
	 * 
	 * @return
	 */
	private boolean processUserOperation() {
		if (data.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Set<String> keys = new HashSet<String>(data.keySet());
		UserOperation userOperation = null;
		String messageString = "";
		
		String messageName = "";
		String type = "";
		String trCmdId = "";
		String carrierId = "";
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(DELETE_USEROPERATION_SQL);
			Message message = new Message();
			for (String key : keys) {
				message.reset();
				userOperation = (UserOperation)data.get(key);
				messageString = userOperation.getMsgString();
				message.setMessage(messageString);
				messageName = message.getMessageName();
				if (TRCMD.equals(messageName)) {
					MsgVector value = new MsgVector();
					message.getMessageItem(TYPE, value, 0, false);
					type = value.toString(0);
					message.getMessageItem(TRCMDID, value, 0, false);
					trCmdId = value.toString(0);
					message.getMessageItem(CARRIERID, value, 0, false);
					carrierId = value.toString(0);
					
					processTrCmdUserOperation(type, trCmdId, carrierId);
				}
				pstmt.setString(1, userOperation.getCommandTime());
				pstmt.setString(2, userOperation.getMsgString());
				pstmt.executeUpdate();
				data.remove(key);
			}
			data.clear();
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
	 * @param type
	 * @param trCmdId
	 * @param carrierId
	 */
	private void processTrCmdUserOperation(String type, String trCmdId, String carrierId) {
		if (trCmdManager == null) {
			trCmdManager = TrCmdManager.getInstance(null, null, false, false, 0);
		}
		
		switch (type.hashCode()) {
			case REMOVE_HASHCODE:
				trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, REMOVE, "");
				break;
			case PAUSE_HASHCODE:
				trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, PAUSE, "");
				break;
			case RESUME_HASHCODE:
				trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, RESUME, "");
				break;
				// 2012.08.08 by KYK	
			case CANCEL_HASHCODE:
				trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, CANCEL, "");
				break;
			default:
				break;
		}
	}
}
