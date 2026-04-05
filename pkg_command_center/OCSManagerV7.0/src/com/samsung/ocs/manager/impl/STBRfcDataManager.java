package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.STBRfcData;

/**
 * STBRfcDataManager Class, OCS 3.0 for Unified FAB
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

public class STBRfcDataManager extends AbstractManager{

	private static STBRfcDataManager manager = null;
	private static final String RFCID = "RFCID";
	private static final String MACHINECODE = "MACHINECODE";
	private static final String MACHINEID = "MACHINEID";
	private static final String CONDITION = "CONDITION";
	private static final String IPADDRESS = "IPADDRESS";
	private static final String ENABLED = "ENABLED";
	private static final String READY = "READY";
	private static final String ERROR = "ERROR";
	private static final String ERRORCODE = "ERRORCODE";
	private static final String UPDATE_TIME = "UPDATE_TIME";
	
	private HashSet<String> enabledRfcIdSet = new HashSet<String>();
	private HashSet<String> errorRfcIdSet = new HashSet<String>();	
	// 2012.03.09 by KYK
	private ArrayList<String> enabledRfcList = new ArrayList<String>();
	private ArrayList<String> disabledRfcList = new ArrayList<String>();
	// 2013.07.26 by KYK
	private boolean isRuntimeUpdateRequested;
	private boolean isRuntimeUpdatable;
	
	/**
	 * Constructor of STBRfcDataManager class.
	 */
	public STBRfcDataManager(Class<?> vOType, DBAccessManager dbAccessManager, 
			boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(STBRfcData.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}		
	}
	
	/**
	 * Constructor of STBRfcDataManager class. (Singleton)
	 */
	public static synchronized STBRfcDataManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, 
			boolean initializeAtStart, boolean makeManagerThread, long interval){
		if (manager == null) {
			manager = new STBRfcDataManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, interval);
		}
		return manager;
	}

	@Override
	protected void init() {
//		updateFromDB();
		initialize();
		isInitialized = true;
	}
	
	public void initializeFromDB() {
		isRuntimeUpdateRequested = true;
		isInitialized = false;
		int count = 0;
		while (true) {
			if (isRuntimeUpdatable || count++ > 200) {
				break;
			}
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

	private static String SELECT_SQL = "SELECT * FROM STBRFCDATA";
	
	public boolean initialize() {
		boolean result = false;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			String rfcId = null;
			STBRfcData rfcData = null;
			if (rs != null) {
				while (rs.next()) {
					rfcId = getString(rs.getString(RFCID));
					rfcData = (STBRfcData) data.get(rfcId);
					if (rfcData == null) {
						rfcData = (STBRfcData) vOType.newInstance();
						data.put(rfcId, rfcData);
					}
					setRfcDataInfo(rfcData, rs);
				}
			}
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (InstantiationException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (IllegalAccessException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}		
		return result;
	}

	
	@Override
	protected boolean updateFromDB() {
		if (isRuntimeUpdateRequested == false) {
			isRuntimeUpdatable = false;
			if (isInitialized) {
				updateFromDBImpl();
			}
		}
		isRuntimeUpdatable = true;
		return true;
	}
	
	/**
	 * 2013.07.26 by KYK
	 * @return
	 */
	protected boolean updateFromDBImpl() {
		if (isInitialized == false) {
			return false;
		}
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;		
		HashSet<String> tempSet = (HashSet<String>) errorRfcIdSet.clone();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					String rfcId = getString(rs.getString(RFCID));
					// 기존 데이터만 갱신함 (추가 runtimeUpdate 로 진행)
					if (data.containsKey(rfcId)) {
						STBRfcData rfcData = (STBRfcData) data.get(rfcId);
						setRfcDataInfo(rfcData, rs);
						if (rfcData.isEnabled()){
							if ("0".equals(rfcData.getError()) == false) {
								if (errorRfcIdSet.contains(rfcId) == false) {
									errorRfcIdSet.add(rfcId);
								}
								tempSet.remove(rfcId);
							}
						}
					}
				}
			}
			for (String rfcId: tempSet) {
				errorRfcIdSet.remove(rfcId);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
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

//	protected boolean updateFromDB() {
//		Connection conn = null;
//		ResultSet rs = null;
//		PreparedStatement pstmt = null;
//		boolean result = false;
//		
//		if (isInitialized) {
//			errorRfcIdSet.clear();
//		}
//		try {
//			conn = dbAccessManager.getConnection();
//			pstmt = conn.prepareStatement(SELECT_SQL);
//			rs = pstmt.executeQuery();
//			while (rs.next()) {
//				String rfcId = rs.getString(RFCID);
//				STBRfcData rfcData = (STBRfcData) data.get(rfcId);
//				if (isInitialized == false) {
//					if (rfcData == null) {
//						rfcData = (STBRfcData) vOType.newInstance();
//						rfcData.setRfcId(rfcId);
//						data.put(rfcId, rfcData);
//					}					
//				}
//				setRfcDataInfo(rfcData, rs);		
//				
//				if (isInitialized) {
//					if (rfcData.isEnabled()){
//						if ("0".equals(rfcData.getError()) == false) {
//							errorRfcIdSet.add(rfcId);
//						}
//					}
//				}
//			}
//			result = true;
//		} catch (SQLException se) {
//			result = false;
//			se.printStackTrace();
//			writeExceptionLog(LOGFILENAME, se);
//		} catch (Exception e) {
//			result = false;
//			writeExceptionLog(LOGFILENAME, e);
//			e.printStackTrace();
//		}
//		finally {
//			if (rs != null) {
//				try {
//					rs.close();
//				} catch (Exception e) {}
//				rs = null;
//			}
//			if (pstmt != null) {
//				try {
//					pstmt.close();
//				} catch (Exception e) {}
//				pstmt = null;
//			}
//		}
//		if (result == false) {
//			dbAccessManager.requestDBReconnect();
//		}		
//		return result;
//	}

	/**
	 * 
	 * @param rfcData
	 * @param rs
	 * @exception SQLException
	 */
	private void setRfcDataInfo(STBRfcData rfcData, ResultSet rs) throws SQLException {
		rfcData.setRfcId(rs.getString("RFCID")); // 2013.08.22 by KYK 주석제거
		rfcData.setMachineCode(rs.getString(MACHINECODE));
		rfcData.setMachineId(rs.getString(MACHINEID));
		rfcData.setCondition(rs.getString(CONDITION));
		rfcData.setIpAddress(rs.getString(IPADDRESS));
		rfcData.setEnabled(getBoolean(rs.getString(ENABLED)));
		rfcData.setReady(rs.getString(READY));
		rfcData.setError(rs.getString(ERROR));
		rfcData.setErrorCode(rs.getString(ERRORCODE));
		rfcData.setUpdate_time(rs.getString(UPDATE_TIME));
	}

	@Override
	protected boolean updateToDB() {
		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	public HashSet<String> getEnabledRfcIdSet(){
		enabledRfcIdSet.clear();
		STBRfcData stbRfcData;
		Iterator<Object> it = getData().values().iterator();
		while (it.hasNext()) {
			stbRfcData = (STBRfcData) it.next();
			if (stbRfcData.isEnabled()){
				enabledRfcIdSet.add(stbRfcData.getRfcId());
			}
		}
		return enabledRfcIdSet;
	}
	
	/**
	 * 2012.03.09 by KYK
	 * Enabled RFC List 를 가져온다.
	 * @return
	 */
	public ArrayList<String> getEnabledRfcList() {
		enabledRfcList.clear();
		STBRfcData stbRfcData;
		String rfcId;
		Iterator<Object> it = getData().values().iterator();
		while (it.hasNext()) {
			stbRfcData = (STBRfcData) it.next();
			if (stbRfcData.isEnabled()){
				rfcId = stbRfcData.getRfcId();
				if (enabledRfcList.contains(rfcId) == false) {
					enabledRfcList.add(rfcId);
				}
			}			
		}		
		return enabledRfcList;
	}
	
	/**
	 * 2012.03.09 by KYK
	 * Disabled RFC List 를 가져온다.
	 * @return
	 */
	public ArrayList<String> getDisabledRfcList() {
		disabledRfcList.clear();
		STBRfcData stbRfcData;
		String rfcId;
		Iterator<Object> it = getData().values().iterator();
		while (it.hasNext()) {
			stbRfcData = (STBRfcData) it.next();
			if (stbRfcData.isEnabled() == false){
				rfcId = stbRfcData.getRfcId();
				if (disabledRfcList.contains(rfcId) == false) {
					disabledRfcList.add(rfcId);
				}
			}			
		}		
		return disabledRfcList;
	}
	
	/**
	 * 
	 * @return
	 */
	public HashSet<String> getErrorRfcIdSet(){
		return errorRfcIdSet;
	}

	private static final String UPDATE_RFC_STATE_SQL = "UPDATE STBRFCDATA SET ENABLED=? WHERE RFCID=?";
	/**
	 * 
	 * @param rfcId
	 * @param enabled
	 * @return
	 */
	public boolean updateRfcState(String rfcId, boolean enabled) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_RFC_STATE_SQL);
			if (enabled) {
				pstmt.setString(1, "TRUE");				
			} else {
				pstmt.setString(1, "FALSE");
			}
			pstmt.setString(2, rfcId);
			pstmt.execute();
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}				
		if (result == false) {
			dbAccessManager.requestDBReconnect();			
		}				
		return result;		
	}

	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}

}
