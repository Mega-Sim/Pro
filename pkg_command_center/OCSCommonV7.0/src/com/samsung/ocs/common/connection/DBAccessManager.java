package com.samsung.ocs.common.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.thread.DBAccessManagerThread;
import com.samsung.ocs.common.constant.CommonLogFileName;

/**
 * DBAccessManager Class, OCS 3.0 for Unified FAB
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

public class DBAccessManager {
	private boolean currConnIsPrimary = false;
	private Vector<Connection> connectionList = null;

	private static Logger log = Logger.getLogger(CommonLogFileName.DBACCESSMANAGER);
	private static Logger logException = Logger.getLogger(CommonLogFileName.OCSMANAGEREXCEPTION);
	private CommonConfig cc = null;

	private boolean isInitialized = false;
	private boolean reconnectRequested = true;
	private int reconnectCount = 0;
	private DBAccessManagerThread thread = null;
	private int logInTimeoutSeconds = 10;

	/**
	 * Constructor of DBAccessManager class.
	 */
	public DBAccessManager() {
		this.connectionList = new Vector<Connection>();
		this.lastSwitchingConnectionTime = System.currentTimeMillis();
		this.logInTimeoutSeconds = CommonConfig.getInstance().getLogInTimeoutSeconds();
		reconnectToDB();
		thread = new DBAccessManagerThread(this);
		thread.start();
	}

	/**
	 * initialize method.
	 * 
	 * @return true if successfully initialized.
	 */
	private boolean initialize() {
		if (cc == null) {
			cc = CommonConfig.getInstance();
		}
		if (cc != null) {
			try {
				DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
			} catch (Throwable e) {
				traceException(e);
				return false;
			}
			isInitialized = true;
			return true;
		}
		exceptionLog("CommonConfig File doesnot exist.");
		return false;
	}
	
	/**
	 * primary database instance에 connection 연결을 시도하는 매서드
	 * @return Connection 
	 */
	private Connection getPrimaryConnection() {
		Connection conn = null;
		try {
			log("Try to connect Primary DB...");
			DriverManager.setLoginTimeout(logInTimeoutSeconds);
			conn = DriverManager.getConnection(cc.getPrimaryUrl(), cc.getDbUserName(), cc.getDbPassWord());
			conn.setAutoCommit(true);
		} catch (Throwable e) {
			traceException(e);
			reconnectCount++;
		}
		reconnectCount = 0;
		return conn;
	}

	/**
	 * secondary database instance에 connection 연결을 시도하는 매서드
	 * @return Connection 
	 */
	private Connection getSecondaryConnection() {
		Connection conn = null;
		try {
			log("Try to connect Secondary DB...");
			DriverManager.setLoginTimeout(logInTimeoutSeconds);
			conn = DriverManager.getConnection(cc.getSecondaryUrl(), cc.getDbUserName(), cc.getDbPassWord());
			conn.setAutoCommit(true);
		} catch (Throwable e) {
			traceException(e);
			reconnectCount++;
		}
		reconnectCount = 0;
		return conn;
	}

	private long lastSwitchingConnectionTime = 0L;
	
	/**
	 * 스위칭하면서 변경해서 추가를 우선으로 한다.
	 * @return
	 */
	private boolean addFirstConnToConnList() {
		Connection conn = null;
		boolean result = false;
		if (currConnIsPrimary == true) {
			conn = getSecondaryConnection();
		} else {
			conn = getPrimaryConnection();
		}
		currConnIsPrimary = !currConnIsPrimary;
		if (conn != null) {
			connectionList.add(conn);
			result = true;
		}
		return result;
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean addNewConnToConnList() {
		Connection conn = null;
		boolean result = false;
		if (currConnIsPrimary == true) {
			conn = getSecondaryConnection();
		} else {
			conn = getPrimaryConnection();
		}
		if (conn != null) {
			currConnIsPrimary = !currConnIsPrimary;
			connectionList.add(conn);
			result = true;
		}
		return result;
	}
	
	/**
	 * database에 재접속을 시도하는 메서드
	 */
	public void reconnectToDB() {
		if (isInitialized == false) {
			initialize();
			return;
		}

		if (connectionList.size() == 0) {
			if (reconnectRequested) {
				synchronized (connectionList) {
					boolean result = addFirstConnToConnList();
					if (result) {
						lastSwitchingConnectionTime = System.currentTimeMillis();
						reconnectRequested = false;
					}
					log(String.format("connListSize 0 : [ReconnectRequested] GetConnectionResult[%s] - afterConnListSize[%d]", result, connectionList.size()));
				}
			} else {
				boolean result = addFirstConnToConnList();
				log(String.format("connListSize 0 : GetConnectionResult[%s] - afterConnListSize[%d]", result, connectionList.size()));
			}
		} else if (connectionList.size() == 1) {
			if (reconnectRequested) {
				synchronized (connectionList) {
					long switchingTimeGap = System.currentTimeMillis() - lastSwitchingConnectionTime;
					if (switchingTimeGap > 2000) {
						Connection oldConnection = connectionList.get(0);
						connectionList.remove(0);
						closeConnection(oldConnection);
						boolean result = addNewConnToConnList();
						if (result) {
							lastSwitchingConnectionTime = System.currentTimeMillis();
							reconnectRequested = false;
						}
						log(String.format("connListSize 1 : [ReconnectRequested] GetConnectionResult[%s] - afterConnListSize[%d]", result, connectionList.size()));
					} else {
						// 2초에 한번씩만...
						log(String.format("connListSize 1 : [ReconnectRequested] SwitchingConnectionTime is enough[%d/2000]", switchingTimeGap));
					}
				}
			} else {
				// 그냥 한개다. 2개로 만들어준다.
				boolean result = addNewConnToConnList();
				log(String.format("connListSize 1 : GetConnectionResult[%s] - afterConnListSize[%d]", result, connectionList.size()));
			}
		} else if (connectionList.size() == 2) {
			if (reconnectRequested) {
				synchronized (connectionList) {
					Connection oldConnection = connectionList.get(0);
					connectionList.remove(0);
					closeConnection(oldConnection);
					lastSwitchingConnectionTime = System.currentTimeMillis();
					reconnectRequested = false;
					log("connListSize 2 : Switching Connection completed.");
				}
			} 
		} else {
			// 뭔가 무슨 에러가 있지않고서야 이럴순 없다고 본다.
			log(String.format("Connection List size error. [%d]", connectionList.size()));
			for (Connection conn : connectionList) {
				closeConnection(conn);
			}
			connectionList.clear();
			reconnectRequested = false;
		}
	}
	
	/**
	 * 
	 * @param conn
	 */
	private synchronized void closeConnection(Connection conn) {
		try {
			if (conn != null) {
				if (conn.isClosed() == false) {
					conn.close();
					log("connection closed...");
				}
				conn = null;
			}
		} catch (Throwable e) {
			traceException(e);
		}
	}

	/**
	 * 현재 db Connection 상태가 양호한지 반환하는 메서드
	 * @return boolean 연결상태
	 */
	public synchronized boolean isDBConnected() {
		if (connectionList.size() > 0 ) {
			Connection conn = connectionList.get(0);
			try {
				boolean closed = conn.isClosed();
				if (closed == false) {
					return true;
				}
			} catch (Throwable e) {
				traceException(e);
			}
		}
		return false;
	}

	/**
	 * reconnectRequested getter
	 * @return boolean reconnectRequested
	 */
	public boolean isReconnectRequested() {
		return reconnectRequested;
	}
	
	/**
	 * dbAccessManager에 reconnectRequested값을 true로 변경하여 reconnection가 일어나도록 하는 매서드
	 */
	public void requestDBReconnect() {
		this.reconnectRequested = true;
		reconnectToDB();
	}

	/**
	 * 현재 접속 유지되고 있는 Connection을 리턴하는 매서드
	 * @return
	 * @exception SQLException
	 */
	public Connection getConnection() throws SQLException {
		Connection conn = null; 
		synchronized (connectionList) {
			if (connectionList.size() > 0 ) {
				conn = connectionList.get(0);
			} 
		}
		if (conn == null) {
			throw new SQLException("Connection is not yet prepared.");
		}
		return conn;
	}
	
	/**
	 * 관리되지 않는 새로운 Connection을 리턴하는 매서드.
	 * 이 매서드로 받은 Connection은 반드시 close까지 해주어야 한다.
	 * @return
	 * @exception SQLException
	 */
	public Connection getNewConnection() throws SQLException {
		return DriverManager.getConnection(cc.getPrimaryUrl(), cc.getDbUserName(), cc.getDbPassWord());
	}
	
	/**
	 * stop thread and close connections
	 */
	public void close() {
		thread.stopThread();
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
			traceException(e);
		}
		for (Connection conn : connectionList) {
			closeConnection(conn);
		}
	}
	
	private void traceException(Throwable w) {
		logException.error(w.getMessage(), w);
	}
	private void exceptionLog(String message) {
		logException.error(message);
	}
	private void log(String message) {
		log.debug(message);
	}
}
