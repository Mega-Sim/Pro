package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.manager.IManager;
import com.samsung.ocs.manager.impl.thread.ManagerThread;

/**
 * AbstractManager Class, OCS 3.0 for Unified FAB
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

public abstract class AbstractManager implements IManager {
	protected ConcurrentHashMap<String, Object> data;
	protected MODULE_STATE reqServiceState = MODULE_STATE.REQOUTOFSERVICE;
	protected MODULE_STATE serviceState = MODULE_STATE.OUTOFSERVICE;
	
	public void setReqServiceState(MODULE_STATE STATE) {
		reqServiceState = STATE;
	}
	
	public MODULE_STATE getServiceState() {
		return serviceState;
	}

	/**
	 * Constructor of AbstractManager abstract class.
	 */
	public AbstractManager(DBAccessManager dbAccessManager, Class<?> vOType, boolean initializeAtStart, boolean makeManagerThread, long interval ) {
		this.data = new ConcurrentHashMap<String, Object>();
		this.dbAccessManager = dbAccessManager;
		this.vOType = vOType;
		if (initializeAtStart) {
			while (true) {
				if (dbAccessManager.isDBConnected()) {
					break;
				}
				writeLog(vOType.getName(), "DB Connection Failed.");
				
				try {
					Thread.sleep(5);
				} catch (Exception e) {
					writeExceptionLog(vOType.getName(), e);
				}
			}
			init();
		}
		if (makeManagerThread) {
			managerThread = new ManagerThread(this, interval);
			managerThread.start();
		}
	}
	
	protected ManagerThread managerThread = null;
	
	protected Class<?> vOType;
	protected DBAccessManager dbAccessManager;

	protected static Logger logger = Logger.getLogger(CommonLogFileName.OCSMANAGERDEBUG);
	protected static Logger loggerException = Logger.getLogger(CommonLogFileName.OCSMANAGEREXCEPTION);
	protected String LOGFILENAME;
	
	protected boolean databaseUpdate = true;
	protected boolean isInitialized = false;
	
	// 이닛실행하면 해줄꺼임.
	protected abstract void init();
	
	// update 구성
	protected abstract boolean updateFromDB();
	protected abstract boolean updateToDB();
	
	/**
	 * 
	 */
	public void update() {
		if (reqServiceState == MODULE_STATE.REQINSERVICE
				&& serviceState == MODULE_STATE.OUTOFSERVICE) {
			serviceState = reqServiceState;
		} else if (reqServiceState == MODULE_STATE.REQOUTOFSERVICE
				&& serviceState == MODULE_STATE.INSERVICE) {
			serviceState = reqServiceState;
		}
		
		if (updateToDB() == false) {
			return;
		}
		if (updateFromDB() == false) {
			return;
		}
		
		if (reqServiceState == MODULE_STATE.REQINSERVICE
				&& serviceState == MODULE_STATE.REQINSERVICE) {
			serviceState = MODULE_STATE.INSERVICE;
		} else if (reqServiceState == MODULE_STATE.REQOUTOFSERVICE
				&& serviceState == MODULE_STATE.REQOUTOFSERVICE) {
			serviceState = MODULE_STATE.OUTOFSERVICE;
		}
	}
	
	public boolean isInitialized() {
		return isInitialized;
	}	
	public void setInitialized(boolean isInitialized) {
		this.isInitialized = isInitialized;
	}
	public void setDatabaseUpdate(boolean databaseUpdate) {
		this.databaseUpdate = databaseUpdate;
	}
	
	protected boolean getBoolean(String value) {
		return OcsConstant.TRUE.equalsIgnoreCase(value);
	}
	
	protected String getString(String value) {
		if (value == null) {
			return "";
		} else {
			return value;
		}
	}

	protected void writeLog(String logfileName, String log) {
		logger.debug(String.format("[%s] %s", logfileName, log));
	}

	protected void writeExceptionLog(String logfileName, Throwable e) {
		// 2012.03.19 by PMM
//		loggerException.error(String.format("[%s] %s", logfileName, e.getMessage()), e);
		loggerException.error(String.format("[%s] ", logfileName), e);
	}
	
	protected void writeExceptionLog(String logfileName, String log) {
		loggerException.error(String.format("[%s] %s", logfileName, log));
	}
	
	public ConcurrentHashMap<String, Object> getData() {
		return data;
	}

	public ManagerThread getManagerThread() {
		return managerThread;
	}
	
	public void close() {
		if (managerThread != null) {
			managerThread.stopThread();
		}
	}
	
	public boolean checkDBStatus(String sql) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			result = true;
		} catch (SQLException se) {
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
			result = false;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					writeExceptionLog(LOGFILENAME, e);
					result = false;
				}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
					writeExceptionLog(LOGFILENAME, e);
					result = false;
				}
				pstmt = null;
			}
		}
		return result;
	}
	
	static final String sql = "SELECT * FROM DUAL";
	public boolean checkDBStatus() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			result = true;
		} catch (SQLException se) {
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
			result = false;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					writeExceptionLog(LOGFILENAME, e);
					result = false;
				}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
					writeExceptionLog(LOGFILENAME, e);
					result = false;
				}
				pstmt = null;
			}
		}
		return result;
	}
}
