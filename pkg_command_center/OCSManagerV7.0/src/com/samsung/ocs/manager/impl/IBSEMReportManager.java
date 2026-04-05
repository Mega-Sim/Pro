package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.IBSEMReport;

/**
 * IBSEMReportManager Class, OCS 3.0 for Unified FAB
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

public class IBSEMReportManager extends AbstractManager {
	private static IBSEMReportManager manager = null;
	private Vector<String> register;
	private static final String MSG = "MSG";
	private static final String REPORTTIME = "REPORTTIME";
	
	/**
	 * Constructor of IBSEMReportManager class.
	 */
	private IBSEMReportManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		register = new Vector<String>();

		if (vOType != null && vOType.getClass().isInstance(IBSEMReport.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of IBSEMReportManager class. (Singleton)
	 */
	public static synchronized IBSEMReportManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new IBSEMReportManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	@Override
	protected void init() {
		updateFromDB();
		isInitialized = true;
	}

	@Override
	protected boolean updateToDB() {
		if (register.size() > 0) {
			registerReportToDB();
		}
		return true;
	}
	
	@Override
	protected boolean updateFromDB() {
		return true;
	}
	
	public void registerReport(String message) {
		if (register.contains(message) == false) {
			register.add(message);
		}
	}

	private static final String REGISTER_SQL = "INSERT INTO IBSEMREPORT (MSG, REPORTTIME) VALUES (?, TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3'))";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean registerReportToDB() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<String> registerClone = null;
		try {
			registerClone = (Vector<String>)register.clone();
			ListIterator<String> iterator = registerClone.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(REGISTER_SQL);
			String message;
			while (iterator.hasNext()) {
				message = (String) iterator.next();
				if (message != null) {
					pstmt.setString(1, message);
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
	
	String lastReportTime = null;
	
	public String getLastReportTime() {
		return lastReportTime;
	}

	public void setLastReportTime(String lastReportTime) {
		this.lastReportTime = lastReportTime;
	}
	
	private static String SELECT_SQL = "SELECT * FROM IBSEMREPORT ORDER BY REPORTTIME ASC, ROWID";
	/**
	 * 
	 * @return
	 */
	public ArrayList<String> getReportToMCSDataFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		ArrayList<String> msgList = new ArrayList<String>();
		String reportTime = null;
		setLastReportTime(reportTime); // initialize
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				msgList.add(rs.getString(MSG));
				reportTime = rs.getString(REPORTTIME);
			}
			setLastReportTime(reportTime);
		} catch (SQLException se) {
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			e.printStackTrace();
			dbAccessManager.requestDBReconnect();
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
		return msgList;
	}

	private static String CLEAR_SQL = "DELETE FROM IBSEMREPORT";
	/**
	 * 
	 * @return
	 */
	public boolean clearIBSEMReport() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		checkCurrentDBStatus();
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(CLEAR_SQL);							
			pstmt.execute();
			result = true;
//			conn.commit();
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
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
		return result;		
	}
	
	// 2018.04.04 by LSH: IBSEMREPORT 테이블 데이터 삭제 로직 변경 롤백 (조건문 "or"로 이어붙이던 예전 방식 사용)
	public boolean deleteIBSEMReportData(ArrayList<String> msgList, String lastReportTime) {
		// 내용 : IBSEMReport 삭제시 보고한 메시지만 삭제하도록 조건 추가
		// 이유 : Operation이 같은 시간(ms단위)에 DB에 저장하였지만 미묘한 시간차이로 IBSEM이 Select시 전부 못가져와
		//       IBSEMReport 삭제시 보고하지 않은 메시지 삭제되는 문제가 발생할 수 있음.

		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;

		String strMsg;
		StringBuffer sb = new StringBuffer();
		sb.append("DELETE FROM IBSEMREPORT WHERE REPORTTIME <='");
		sb.append(lastReportTime); sb.append("'");		
		if (msgList.size() > 0) {
			sb.append(" AND (");
			for (int i=0; i < msgList.size(); i++) {
				strMsg = msgList.get(i);
				sb.append("MSG='").append(strMsg).append("'");
				if ( i < msgList.size()-1) {
					sb.append(" or ");
				} else {
					sb.append(")");																
				}
			}
		}		
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(sb.toString());							
			pstmt.execute();
			result = true;
//			conn.commit();
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		}  catch (Exception e) {
			e.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
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
		return result;		
	}
	
	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}
}
