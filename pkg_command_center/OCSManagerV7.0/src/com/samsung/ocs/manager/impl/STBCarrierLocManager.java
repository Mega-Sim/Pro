package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.STBCarrierLoc;

/**
 * STBCarrierLocManager Class, OCS 3.0 for Unified FAB
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

public class STBCarrierLocManager extends AbstractManager{
	private static final String CARRIERLOCID = "CARRIERLOCID";
	private static final String ENABLED = "ENABLED";
	private static final String CARRIERID = "CARRIERID";
	private static final String INSTALLTIME = "INSTALLTIME";
	private static final String REMOVETIME = "REMOVETIME";
	private static final String CARRIERSTATE = "CARRIERSTATE";
	private static final String IDREADER = "IDREADER";
	private static final String COMMANDNAME = "COMMANDNAME";
	private static final String MCSCARRIERID = "MCSCARRIERID";
	private static final String OCSCARRIERID = "OCSCARRIERID";
	private static final String RFCID = "RFCID";
	private static final String RFCINDEX = "RFCINDEX";
	private static final String READY = "READY";
	private static final String CARRIERSENSOR = "CARRIERSENSOR";
	private static final String STBHOMESENSOR = "STBHOMESENSOR";
	private static final String ECAT1CONN = "ECAT1CONN";
	private static final String ECAT2CONN = "ECAT2CONN";
	private static final String CARRIERDETECT = "CARRIERDETECT";
	private static final String READRESULT = "READRESULT";
	private static final String VERIFYRESULT = "VERIFYRESULT";
	private static final String IDDATA = "IDDATA";
	private static final String RFCREADERID = "RFCREADERID";
	private static final String MISMATCHCOUNT = "MISMATCHCOUNT"; // 2012.09.10 by KYK
	// 2014.01.02 by KBS : FoupID 사용 모드 (for A-PJT EDS)
	private static final String FOUPID = "FOUPID";
	private static final String MCSFOUPID = "MCSFOUPID";
	
	private static final String INSTALL = "INSTALL";
	private static final String REMOVE = "REMOVE";
	
	private static final String TRUE = "TRUE";
	private static final String FALSE = "FALSE";
	
	private static final String CHANGEIDREQUEST = "CHANGEIDREQUEST";
	
	// 2011.11.09 by PMM
	private boolean isRuntimeUpdateRequested = false;
	private boolean isRuntimeUpdatable = false;
	
	// new
	private HashMap<String, STBCarrierLoc> carrierInfoTable = new HashMap<String, STBCarrierLoc>();
	private HashMap<String, STBCarrierLoc> ocsCommandTable = new HashMap<String, STBCarrierLoc>();
//	private HashMap<String, Boolean> portStateChangeTable = new HashMap<String, Boolean>();	
	// 2012.03.15 by KYK : Thread 동시 참조에 대한 예외상황 예방
	private ConcurrentHashMap<String, Boolean> portStateChangeTable = new ConcurrentHashMap<String, Boolean>();	
	private Vector<String> prevDisabledPortList = new Vector<String>();
	private Vector<STBCarrierLoc> updatePortServiceStateList = new Vector<STBCarrierLoc>();
	private Vector<STBCarrierLoc> updateSTBCarrierLocStateListForOperation = new Vector<STBCarrierLoc>();
	private HashSet<String> rfcIdSet;
	
	private Map<String, String> changeIdRequestList = new HashMap<String, String>();
	
	private DBAccessManager dbAccessManagerForBatch = null;

	private STBCarrierLocManager(Class<?> vOType, DBAccessManager dbAccessManager,
			boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);

		LOGFILENAME = this.getClass().getName();
		
		dbAccessManagerForBatch = new DBAccessManager();
		
		if (vOType != null && vOType.getClass().isInstance(STBCarrierLoc.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}

	private static STBCarrierLocManager manager;
	public static synchronized STBCarrierLocManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval){
		if (manager == null) {
			manager = new STBCarrierLocManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}	

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		initialize();
		isInitialized = true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		updateSTBCarrierStateForOperation();	// For Operation
		updateSTBPortServiceState(); // For STBC
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		// 2011.11.09 by PMM
		if (isRuntimeUpdateRequested == false) {
			isRuntimeUpdatable = false;
			if (isInitialized) {
				updateFromDBImpl();
			} else {
				// 어떤 경우?? 예방 차원
				init();
			}
		} 
		isRuntimeUpdatable = true;
		return true;
	}
	
	// 2011.11.09 by PMM
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB() {
		isRuntimeUpdateRequested = true;
		isInitialized = false;
		int count = 0;
		while (true) {
			if (isRuntimeUpdatable || count > 200) {
				break;
			}
			count++;
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
	
	private static final String SELECT_ALL_SQL = "SELECT * FROM STBCARRIERLOC";
	/**
	 * 
	 * @return
	 */
	private boolean initialize() {
//		MODULE_STATE serviceState = this.serviceState;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		STBCarrierLoc stbCarrierLoc = null;
		String carrierLocId = null;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		try {
			conn = dbAccessManager.getConnection();
			if (conn.isClosed() == false) {
				pstmt = conn.prepareStatement(SELECT_ALL_SQL);
				rs = pstmt.executeQuery();
				while (rs.next()) {
					carrierLocId = rs.getString(CARRIERLOCID);
					stbCarrierLoc = (STBCarrierLoc) data.get(carrierLocId);
					if (stbCarrierLoc == null) {
						stbCarrierLoc = (STBCarrierLoc) vOType.newInstance();
						stbCarrierLoc.setCarrierLocId(carrierLocId);
						setStbCarrierLoc(stbCarrierLoc, rs);
						data.put(carrierLocId, stbCarrierLoc);
					} else {
						setStbCarrierLoc(stbCarrierLoc, rs);
					}
					removeKeys.remove(carrierLocId);
				}
				for (String rmKey : removeKeys) {
					data.remove(rmKey);
				}
				result = true;
			}
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (InstantiationException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
	
	/**
	 * Request to Port ID RuntimeUpdate
	 */
	public void changePortIDFromDB() {
		isRuntimeUpdateRequested = true;
		isInitialized = false;
		int count = 0;
		while (true) {
			if (isRuntimeUpdatable || count > 200) {
				break;
			}
			count++;
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
				break;
			}
		}
		
		changeIdRequestList.clear();
		
		changePortID();

		isInitialized = true;
		isRuntimeUpdateRequested = false;
	}
	
	private static final String SELECT_CHANGEIDREQUEST_SQL = "SELECT * FROM STBCARRIERLOC A WHERE CHANGEIDREQUEST IS NOT NULL AND NOT EXISTS (SELECT CARRIERLOCID FROM STBCARRIERLOC B WHERE A.CHANGEIDREQUEST=B.CARRIERLOCID) AND CHANGEIDREQUEST IN (SELECT CHANGEIDREQUEST FROM STBCARRIERLOC GROUP BY CHANGEIDREQUEST HAVING COUNT(*) = 1)";
	private static final String UPDATE_CHANGEIDREQUEST_SQL = "UPDATE STBCARRIERLOC SET CARRIERLOCID=?, CHANGEIDREQUEST='' WHERE CARRIERLOCID=? AND CHANGEIDREQUEST=?";
	/**
	 * 
	 * @return
	 */
	private boolean changePortID() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(SELECT_CHANGEIDREQUEST_SQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = pstmt.executeQuery();
			String carrierLocId = null;
			String changeIdRequest = null;
			STBCarrierLoc stbCarrierLoc = null;
			
			while (rs.next()) {
				carrierLocId = rs.getString(CARRIERLOCID);
				changeIdRequest = rs.getString(CHANGEIDREQUEST);
				changeIdRequestList.put(carrierLocId, changeIdRequest);
			}
			
			pstmt = conn.prepareStatement(UPDATE_CHANGEIDREQUEST_SQL);
			Iterator<String> iter = changeIdRequestList.keySet().iterator();
			while (iter.hasNext()) {
				carrierLocId = iter.next();
				stbCarrierLoc = (STBCarrierLoc) data.get(carrierLocId);
				if (stbCarrierLoc != null) {
					changeIdRequest = changeIdRequestList.get(carrierLocId);
					pstmt.setString(1, changeIdRequest);
					pstmt.setString(2, carrierLocId);
					pstmt.setString(3, changeIdRequest);
					pstmt.executeUpdate();
				}
			}
			
			carrierLocId = null;
			changeIdRequest = null;
			rs.beforeFirst();
			while (rs.next()) {
				carrierLocId = rs.getString(CARRIERLOCID);
				stbCarrierLoc = (STBCarrierLoc) data.get(carrierLocId);
				if (stbCarrierLoc != null) {
					data.remove(carrierLocId);
				}
				changeIdRequest = rs.getString(CHANGEIDREQUEST);
				stbCarrierLoc = (STBCarrierLoc) data.get(changeIdRequest);
				if (stbCarrierLoc == null) {
					stbCarrierLoc = (STBCarrierLoc) vOType.newInstance();
					setStbCarrierLocforChangeID(stbCarrierLoc, changeIdRequest, rs);
					data.put(changeIdRequest, stbCarrierLoc);
				}
			}
			
			conn.commit();
			conn.setAutoCommit(true);
			
			result = true;
		} catch (SQLException se) {
			try { if(conn!=null) conn.rollback(); } catch (SQLException ignore) {}
			try { if(conn!=null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			try { if(conn!=null) conn.rollback(); } catch (SQLException ignore) {}
			try { if(conn!=null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
			try { if(conn!=null) conn.rollback(); } catch (SQLException ignore) {}
			try { if(conn!=null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
			try { if(conn!=null) conn.rollback(); } catch (SQLException ignore) {}
			try { if(conn!=null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			e.printStackTrace();
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
		
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	
	// 2011.11.09 by PMM
	private static final String SELECT_SQL = "SELECT * FROM STBCARRIERLOC WHERE ENABLED='FALSE' OR (ENABLED='TRUE' AND COMMANDNAME IS NOT NULL) OR CARRIERID IS NOT NULL";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	// 2014.12.19 by KYK
//	private boolean updateFromDBImpl() {
	public boolean updateFromDBImpl() {
		if (isInitialized == false) {
			return false;
		} 		
//		MODULE_STATE serviceState = this.serviceState;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		String carrierLocId = null;
		String carrierId = null;
		STBCarrierLoc stbCarrierLoc;
		Vector<String> disabledPortList = new Vector<String>();	
		Vector<String> enabledPortList = null;
		try {
			// map initialize
//			ocsCommandTable.clear();
			conn = dbAccessManager.getConnection();
			if (conn.isClosed() == false) {
				pstmt = conn.prepareStatement(SELECT_SQL);
				rs = pstmt.executeQuery();
				
				while (rs.next()) {
					if (isRuntimeUpdateRequested && isInitialized) {
						return false;
					}
					
					carrierLocId = rs.getString(CARRIERLOCID);	
					stbCarrierLoc = (STBCarrierLoc)data.get(carrierLocId);
					if (stbCarrierLoc == null) {
						stbCarrierLoc = (STBCarrierLoc) vOType.newInstance();
						stbCarrierLoc.setCarrierLocId(carrierLocId);
						setStbCarrierLoc(stbCarrierLoc, rs);
						data.put(carrierLocId, stbCarrierLoc);
					} else {
						setStbCarrierLoc(stbCarrierLoc, rs);
					}
					
					if (stbCarrierLoc.isEnabled() == false) {
						disabledPortList.add(carrierLocId);
//						// 기존 disabledCarrierLocList 에서 변경된 부분만 report 함
//						if (prevDisabledPortList.contains(carrierLocId)){
//							// 변경없는 항목은 제거
//							prevDisabledPortList.remove(carrierLocId);						
//						} else {// 기존없던 FALSE 항목은 추가
//							portStateChangeTable.put(carrierLocId, false);
//						}
						if (prevDisabledPortList.remove(carrierLocId) == false){
							portStateChangeTable.put(carrierLocId, false);
						}
					}				
					
//					if (stbCarrierLoc.getCommandName().length() > 0) {
//						ocsCommandTable.put(carrierLocId, stbCarrierLoc);
//					}
//					carrierId = stbCarrierLoc.getCarrierId();
//					if (stbCarrierLoc.getCarrierId().length() > 0) {
//						carrierInfoTable.put(carrierId, stbCarrierLoc);
//					}				
					if (stbCarrierLoc.getCommandName().length() > 0) {
						if (ocsCommandTable.containsKey(carrierLocId) == false) {
							ocsCommandTable.put(carrierLocId, stbCarrierLoc);							
						}
					}
				}	
				// 기존 항목(FALSE)중 TRUE로 변경된 것
				enabledPortList = (Vector<String>)prevDisabledPortList.clone();
				for (String portId : enabledPortList) {
					portStateChangeTable.put(portId, true);
					prevDisabledPortList.remove(portId);
					// 2013.08.21 by KYK
					STBCarrierLoc enabledCarrierLoc = (STBCarrierLoc)data.get(portId);
					if (enabledCarrierLoc != null) {
						enabledCarrierLoc.setEnabled(true);
					}
				}
				prevDisabledPortList.clear();
				
				// 현재 disabledPortList 를 prevDisabledPortList 로 갱신
				for (String portId2 : disabledPortList) {
					prevDisabledPortList.add(portId2);
				}
				result = true;
			}
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (InstantiationException ie) {
			result = false;
			writeExceptionLog(LOGFILENAME, ie);
			ie.printStackTrace();
		} catch (IllegalAccessException e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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

	/**
	 * 
	 * @param stbCarrierLoc
	 * @param rs
	 * @exception SQLException
	 */
	private void setStbCarrierLoc(STBCarrierLoc stbCarrierLoc, ResultSet rs) throws SQLException {
		stbCarrierLoc.setCarrierLocId(getString(rs.getString(CARRIERLOCID)));
		stbCarrierLoc.setEnabled(getBoolean(rs.getString(ENABLED)));
		stbCarrierLoc.setCarrierId(getString(rs.getString(CARRIERID)));
		stbCarrierLoc.setInstalledTime(getString(rs.getString(INSTALLTIME)));	// INSTALLTIME -> INSTALLEDTIME
		stbCarrierLoc.setRemovedTime(getString(rs.getString(REMOVETIME)));	//REMOVETIME -> REMOVEDTIME
		stbCarrierLoc.setCarrierState(getString(rs.getString(CARRIERSTATE)));
		stbCarrierLoc.setIdReader(getString(rs.getString(IDREADER)));
		stbCarrierLoc.setCommandName(getString(rs.getString(COMMANDNAME)));
		stbCarrierLoc.setMcsCarrierId(getString(rs.getString(MCSCARRIERID)));
		stbCarrierLoc.setOcsCarrierId(getString(rs.getString(OCSCARRIERID)));

		stbCarrierLoc.setRfcId(getString(rs.getString(RFCID)));
		stbCarrierLoc.setRfcIndex(getString(rs.getString(RFCINDEX)));
		stbCarrierLoc.setReady(getString(rs.getString(READY)));
		stbCarrierLoc.setCarrierSensor(getString(rs.getString(CARRIERSENSOR)));
		stbCarrierLoc.setStbHomeSensor(getString(rs.getString(STBHOMESENSOR)));
		stbCarrierLoc.setEcat1Conn(getString(rs.getString(ECAT1CONN)));
		stbCarrierLoc.setEcat2Conn(getString(rs.getString(ECAT2CONN)));
		stbCarrierLoc.setCarrierDetect(getString(rs.getString(CARRIERDETECT)));
		stbCarrierLoc.setReadResult(getString(rs.getString(READRESULT)));
		stbCarrierLoc.setVerifyResult(getString(rs.getString(VERIFYRESULT)));
		stbCarrierLoc.setIdData(getString(rs.getString(IDDATA)));
		stbCarrierLoc.setRfcReaderId(getString(rs.getString(RFCREADERID)));
		// 2012.09.10 by KYK
		stbCarrierLoc.setMismatchCount(rs.getInt(MISMATCHCOUNT));

		stbCarrierLoc.setFoupId(getString(rs.getString(FOUPID)));
		stbCarrierLoc.setMcsFoupId(getString(rs.getString(MCSFOUPID)));
	}
	
	/**
	 * 
	 * @param stbCarrierLoc
	 * @param changeIdRequest
	 * @param rs
	 * @exception SQLException
	 */
	private void setStbCarrierLocforChangeID(STBCarrierLoc stbCarrierLoc, String changeIdRequest, ResultSet rs) throws SQLException {
		stbCarrierLoc.setCarrierLocId(changeIdRequest);
		stbCarrierLoc.setEnabled(getBoolean(rs.getString(ENABLED)));
		stbCarrierLoc.setCarrierId(getString(rs.getString(CARRIERID)));
		stbCarrierLoc.setInstalledTime(getString(rs.getString(INSTALLTIME)));	// INSTALLTIME -> INSTALLEDTIME
		stbCarrierLoc.setRemovedTime(getString(rs.getString(REMOVETIME)));	//REMOVETIME -> REMOVEDTIME
		stbCarrierLoc.setCarrierState(getString(rs.getString(CARRIERSTATE)));
		stbCarrierLoc.setIdReader(getString(rs.getString(IDREADER)));
		stbCarrierLoc.setCommandName(getString(rs.getString(COMMANDNAME)));
		stbCarrierLoc.setMcsCarrierId(getString(rs.getString(MCSCARRIERID)));
		stbCarrierLoc.setOcsCarrierId(getString(rs.getString(OCSCARRIERID)));

		stbCarrierLoc.setRfcId(getString(rs.getString(RFCID)));
		stbCarrierLoc.setRfcIndex(getString(rs.getString(RFCINDEX)));
		stbCarrierLoc.setReady(getString(rs.getString(READY)));
		stbCarrierLoc.setCarrierSensor(getString(rs.getString(CARRIERSENSOR)));
		stbCarrierLoc.setStbHomeSensor(getString(rs.getString(STBHOMESENSOR)));
		stbCarrierLoc.setEcat1Conn(getString(rs.getString(ECAT1CONN)));
		stbCarrierLoc.setEcat2Conn(getString(rs.getString(ECAT2CONN)));
		stbCarrierLoc.setCarrierDetect(getString(rs.getString(CARRIERDETECT)));
		stbCarrierLoc.setReadResult(getString(rs.getString(READRESULT)));
		stbCarrierLoc.setVerifyResult(getString(rs.getString(VERIFYRESULT)));
		stbCarrierLoc.setIdData(getString(rs.getString(IDDATA)));
		stbCarrierLoc.setRfcReaderId(getString(rs.getString(RFCREADERID)));
		// 2012.09.10 by KYK
		stbCarrierLoc.setMismatchCount(rs.getInt(MISMATCHCOUNT));

		stbCarrierLoc.setFoupId(getString(rs.getString(FOUPID)));
		stbCarrierLoc.setMcsFoupId(getString(rs.getString(MCSFOUPID)));
	}

	public HashMap<String, STBCarrierLoc> getCarrierInfoTable(){
		return carrierInfoTable;
	}

	public HashMap<String, STBCarrierLoc> getOcsCommandTable(){
		return ocsCommandTable;
	}
	public ConcurrentHashMap<String, Boolean> getPortStateChangeTable(){
		return portStateChangeTable;
	}	

	private static final String SELECT_STBCARRIERINFO_SQL = "SELECT * FROM STBCARRIERLOC WHERE CARRIERLOCID=? AND CARRIERID=?";
	/**
	 * 
	 * @param carrierLoc
	 * @param carrierId
	 * @return
	 */
	public boolean getCarrierInfo(String carrierLoc, String carrierId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean result = false;
		int count = 0;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_STBCARRIERINFO_SQL);
			pstmt.setString(1, carrierLoc);
			pstmt.setString(2, carrierId);

			rs = pstmt.executeQuery();
			while (rs.next()) {
				count++;
			}
			if (count > 0) {
				result =  true;
			}
		} catch (SQLException se) {
			carrierLoc = null;
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			carrierLoc = null;
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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

	// 2013.01.04 by KYK : Invalid CarrierLocId Check
	public boolean checkValidationOfCarrierLoc(HashMap<String, String> carrierDataTable) {
		for (String carrierLocId : carrierDataTable.keySet()) {
			// OCS에 등록되지 않은 carrierLoc 이 있으면 return false
			if (data.containsKey(carrierLocId) == false) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 2013.01.04 by KYK : OCS에 등록여부 체크부분은 별도 분리함
	 * @param carrierDataTable
	 * @return
	 */
	public boolean updateActiveCarriersToDB2(HashMap<String, String> carrierDataTable) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int count = 1;
		String carrierId;
		long batchStartTime = System.currentTimeMillis();
		
		try {
			conn = dbAccessManagerForBatch.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_ACTIVECARRIERS_ONEMESSAGE_SQL);
			
			for (String carrierLocId : carrierDataTable.keySet()) {				
				carrierId = carrierDataTable.get(carrierLocId);				
				count++;
				pstmt.setString(1, carrierId);
				pstmt.setString(2, carrierLocId);
				pstmt.addBatch();
				pstmt.clearParameters();					
				
				if (count % 200 == 0) {
					pstmt.executeBatch();
					count = 1;
				}
			}
			if (count > 1) {
				pstmt.executeBatch();
			}
			conn.commit();
			result = true;
			long batchDurationTime = System.currentTimeMillis() - batchStartTime;
			System.out.println("BatchUpdate Time:"+batchDurationTime);
		} catch (SQLException se) {
			result = false;
			writeExceptionLog(LOGFILENAME, se);
			try {
				conn.rollback();
			} catch (Exception ignore) {
			}
		} catch (Exception e) {
			result = false;
			try {
				conn.rollback();
			} catch (Exception ignore) {
			}
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}		
		
		if (result == false) {
			dbAccessManagerForBatch.requestDBReconnect();			
		}
		return result;
	}
	
	private static final String UPDATE_ACTIVECARRIERS_ONEMESSAGE_SQL = "UPDATE STBCARRIERLOC SET CARRIERID=?, " +
		"INSTALLTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), CARRIERSTATE='INSTALLED' , COMMANDNAME=''" +
		", MCSCARRIERID='', OCSCARRIERID='' WHERE CARRIERLOCID=?";
	/**
	 * 
	 * @param carrierDataTable
	 * @return
	 */
	public boolean updateActiveCarriersToDB(HashMap<String, String> carrierDataTable) {		
		if (!checkDBStatus()) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int count = 1;
		String carrierId;
		long batchStartTime = System.currentTimeMillis();
		
		// 2013.01.04 by KYK
		if (carrierDataTable == null || carrierDataTable.isEmpty()) {
			return true;
		}
		try {
			conn = dbAccessManagerForBatch.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_ACTIVECARRIERS_ONEMESSAGE_SQL);
			
			for (String carrierLocId : carrierDataTable.keySet()) {				
				carrierId = carrierDataTable.get(carrierLocId);				
//				if(getCarrierLocTable().containsKey(carrierLoc)){
				if (data.containsKey(carrierLocId)) {
					count++;
					pstmt.setString(1, carrierId);
					pstmt.setString(2, carrierLocId);
					pstmt.addBatch();
					pstmt.clearParameters();					
				} else {
					// 2012.05.18 by KYK : carrierLocId 가 없으면 전체 처리를 무효화함
					return false;
				}
				if (count % 200 == 0) {
					pstmt.executeBatch();
					count = 1;
				}
			}
			if (count > 1) {
				pstmt.executeBatch();
			}
			conn.commit();
			result = true;
			long batchDurationTime = System.currentTimeMillis() - batchStartTime;
			System.out.println("BatchUpdate Time:"+batchDurationTime);
		} catch (SQLException se) {
			result = false;
			writeExceptionLog(LOGFILENAME, se);
			try {
				conn.rollback();
			} catch (Exception ignore) {
			}
		} catch (Exception e) {
			result = false;
			try {
				conn.rollback();
			} catch (Exception ignore) {
			}
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}		
		
		if (result == false) {
			dbAccessManagerForBatch.requestDBReconnect();			
		}
		return result;
	}

	private static final String UPDATE_ALL_SQL = "UPDATE STBCARRIERLOC SET CARRIERID='', FOUPID='', INSTALLTIME='',REMOVETIME=''," 
		+ "CARRIERSTATE='',COMMANDNAME='', MCSCARRIERID='', OCSCARRIERID='', MCSFOUPID=''";
	/**
	 * 
	 */
	public void clearAllSTBData() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_ALL_SQL);
			pstmt.execute();			
			result = true;
			
		} catch (SQLException se) {
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}		
		
		if (result == false) {
			dbAccessManager.requestDBReconnect();			
		}		
	}
	
	private static final String UPDATE_ALL_CMD_SQL = "UPDATE STBCARRIERLOC SET CARRIERID=(CASE WHEN COMMANDNAME='INSTALL' THEN MCSCARRIERID ELSE '' END), "
			+ "FOUPID=(CASE WHEN COMMANDNAME='INSTALL' THEN MCSFOUPID ELSE '' END), "
			+ "CARRIERSTATE=(CASE WHEN COMMANDNAME='INSTALL' THEN 'INSTALLED' ELSE 'EMPTY' END), COMMANDNAME='', MCSCARRIERID='', OCSCARRIERID='', MCSFOUPID = '' WHERE COMMANDNAME IS NOT NULL";
	/**
	 * 
	 */
	public int updateAllCarrierLocStatus() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int updateCount = 0;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_ALL_CMD_SQL);
			updateCount = pstmt.executeUpdate();
			result = true;
		} catch (SQLException se) {
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}		
		if (result == false) {
			dbAccessManager.requestDBReconnect();			
		}
		return updateCount;
	}

	private static final String UPDATE_CMD_INSTALL_SQL = "UPDATE STBCARRIERLOC SET CARRIERID=?,FOUPID=?,INSTALLTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') " +
		",REMOVETIME='',CARRIERSTATE='INSTALLED',COMMANDNAME='',MCSCARRIERID='',MCSFOUPID='',OCSCARRIERID='' WHERE CARRIERLOCID=?";

	private static final String UPDATE_CMD_REMOVE_SQL = "UPDATE STBCARRIERLOC SET CARRIERID='',FOUPID='',REMOVETIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') " +
	",CARRIERSTATE='EMPTY',COMMANDNAME='',MCSCARRIERID='',MCSFOUPID='',OCSCARRIERID='' WHERE CARRIERLOCID=?";
	
	// MCSCARRIERID 를 CARRIERID 에 업데이트 함 (COMMANDNAME,MCSCARRIERID,OCSCARRIERID='')	
	// isRemoveOcsCmd 확인할것
	/**
	 * 
	 */
	public boolean updateCarrierLocStatus(String carrierLocId, String carrierId, String foupId, String command, boolean isRemoveOcsCmd) {
		// Memory Data Update
		STBCarrierLoc carrierLoc = (STBCarrierLoc) getData().get(carrierLocId);
		if (INSTALL.equalsIgnoreCase(command)) {
			carrierLoc.setCarrierId(carrierId);
			carrierLoc.setFoupId(foupId);			
		} else if (REMOVE.equalsIgnoreCase(command)) {
			carrierLoc.setCarrierId("");	
			carrierLoc.setFoupId("");			
		}
		
		// 2012.11.05 by KYK
		if (ocsCommandTable.containsKey(carrierLocId)) {
			ocsCommandTable.remove(carrierLocId);			
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();			
			if (INSTALL.equalsIgnoreCase(command)){
				pstmt = conn.prepareStatement(UPDATE_CMD_INSTALL_SQL);
				pstmt.setString(1, carrierId);
				pstmt.setString(2, foupId);
				pstmt.setString(3, carrierLocId);				
			} else if (REMOVE.equalsIgnoreCase(command)){
				pstmt = conn.prepareStatement(UPDATE_CMD_REMOVE_SQL);
				pstmt.setString(1, carrierLocId);								
			} else {
				return false;
			}
			pstmt.execute();			
			result = true;
			
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
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
	
	// 2013.01.04 by KYK
	private static final String UPDATE_INSTALLCMD_WITH_REASON_SQL = "UPDATE STBCARRIERLOC SET CARRIERID=?,FOUPID=?,REASON=?,INSTALLTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') " +
	",REMOVETIME='',CARRIERSTATE='INSTALLED',COMMANDNAME='',MCSCARRIERID='',MCSFOUPID='',OCSCARRIERID='' WHERE CARRIERLOCID=?";

	private static final String UPDATE_REMOVECMD_WITH_REASON_SQL = "UPDATE STBCARRIERLOC SET CARRIERID='',REASON=?,REMOVETIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') " +
	",CARRIERSTATE='EMPTY',COMMANDNAME='',MCSCARRIERID='',OCSCARRIERID='' WHERE CARRIERLOCID=?";
	
	/**
	 * 2013.01.04 by KYK
	 * @param carrierLocId
	 * @param carrierId
	 * @param foupId
	 * @param command
	 * @param reason
	 * @param isRemoveOcsCmd
	 * @return
	 */
	public boolean updateCarrierLocStatus(String carrierLocId, String carrierId, String foupId, String command, String reason, boolean isRemoveOcsCmd) {
		// Memory Data Update
		STBCarrierLoc carrierLoc = (STBCarrierLoc) getData().get(carrierLocId);
		if (INSTALL.equalsIgnoreCase(command)) {
			carrierLoc.setCarrierId(carrierId);			
			carrierLoc.setFoupId(foupId);
		} else if (REMOVE.equalsIgnoreCase(command)) {
			carrierLoc.setCarrierId("");	
		}		
		// 2012.11.05 by KYK
		if (ocsCommandTable.containsKey(carrierLocId)) {
			ocsCommandTable.remove(carrierLocId);			
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();			
			if (INSTALL.equalsIgnoreCase(command)){
				pstmt = conn.prepareStatement(UPDATE_INSTALLCMD_WITH_REASON_SQL);
				pstmt.setString(1, carrierId);				
				pstmt.setString(2, foupId);
				pstmt.setString(3, reason);				
				pstmt.setString(4, carrierLocId);
			} else if (REMOVE.equalsIgnoreCase(command)){
				pstmt = conn.prepareStatement(UPDATE_REMOVECMD_WITH_REASON_SQL);
				pstmt.setString(1, reason);								
				pstmt.setString(2, carrierLocId);								
			} else {
				return false;
			}
			pstmt.execute();			
			result = true;
			
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
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
	
	// 2012.09.10 by KYK : (RFC Verify 에 의한) carrier mismatch 발생 확인용  
	private static final String UPDATE_ABNORMAL_STATUS = "UPDATE STBCARRIERLOC SET MISMATCHCOUNT=? WHERE CARRIERLOCID=?";	
	public boolean updateAbnormalCarrierLocStatus(String carrierLocId, int mismatchCount) {
		// memory Update ?
		if (carrierLocId == null || carrierLocId.length() == 0) {
			return false;
		}
		STBCarrierLoc carrierLoc = (STBCarrierLoc) getData().get(carrierLocId);
		if (carrierLoc == null) {
			return false;
		}
		carrierLoc.setMismatchCount(mismatchCount);
		//
		boolean result = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_ABNORMAL_STATUS);
			pstmt.setInt(1, mismatchCount);
			pstmt.setString(2, carrierLocId);
			pstmt.execute();
			result = true;
			
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);		
		}		
		return result;
	}

	private static final String SELECT_STBCARRIERLOC_BY_CARRIERID_SQL = "SELECT * FROM STBCARRIERLOC WHERE CARRIERID=?";
	/**
	 * 
	 * @param carrierId
	 * @return
	 */
	public STBCarrierLoc getCarrierLocFromDBWhereCarrierId(String carrierId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean result = false;
		STBCarrierLoc carrierLoc = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_STBCARRIERLOC_BY_CARRIERID_SQL);
			pstmt.setString(1, carrierId);

			rs = pstmt.executeQuery();
			while (rs.next()) {
				carrierLoc = new STBCarrierLoc();
				setStbCarrierLoc(carrierLoc, rs);
			}
			result = true;
			
		} catch (SQLException se) {
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
		return carrierLoc;
	}
	
	private static final String SELECT_STBCARRIERLOC_BY_CARRIERLOCID_SQL = "SELECT * FROM STBCARRIERLOC WHERE CARRIERLOCID=? ";
	/**
	 * 
	 * @param carrierLocId
	 * @return
	 */
	public STBCarrierLoc getCarrierLocFromDBWhereCarrierLocId(String carrierLocId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		STBCarrierLoc carrierLoc = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_STBCARRIERLOC_BY_CARRIERLOCID_SQL);
			pstmt.setString(1, carrierLocId);

			rs = pstmt.executeQuery();
			while (rs.next()) {
				carrierLoc = new STBCarrierLoc();
				setStbCarrierLoc(carrierLoc, rs);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
		return carrierLoc;
	}
	
	private static final String SELECT_PORTLIST_BY_RFCID_SQL = "SELECT * FROM STBCARRIERLOC WHERE RFCID=? ";
	/**
	 * 
	 * @param rfcId
	 * @return
	 */
	public ArrayList<STBCarrierLoc> getPortListFromDBWhereRfcId(String rfcId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		ArrayList<STBCarrierLoc> portList = new ArrayList<STBCarrierLoc>();
		STBCarrierLoc carrierLoc = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_PORTLIST_BY_RFCID_SQL);
			pstmt.setString(1, rfcId);

			rs = pstmt.executeQuery();
			while (rs.next()) {
				carrierLoc = new STBCarrierLoc();
				setStbCarrierLoc(carrierLoc, rs);
				portList.add(carrierLoc);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
		return portList;
	}

	private static final String SELECT_ACTIVECARRIERS_SQL = "SELECT CARRIERID,CARRIERLOCID,CARRIERSTATE,INSTALLTIME FROM STBCARRIERLOC WHERE CARRIERID IS NOT NULL";
	/**
	 * 
	 * @return
	 */
	public HashMap<String, STBCarrierLoc> uploadActiveCarriersFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		STBCarrierLoc carrierLoc;
		String carrierLocId;
		HashMap<String, STBCarrierLoc> activeCarriersTable = new HashMap<String, STBCarrierLoc>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_ACTIVECARRIERS_SQL);

			rs = pstmt.executeQuery();
			while (rs.next()) {
				carrierLoc = new STBCarrierLoc();
				carrierLocId = getString(rs.getString(CARRIERLOCID));
				carrierLoc.setCarrierLocId(carrierLocId);
				carrierLoc.setCarrierId(getString((rs.getString(CARRIERID))));				
				carrierLoc.setInstalledTime(getString(rs.getString(INSTALLTIME)));
				carrierLoc.setCarrierState(getString(rs.getString(CARRIERSTATE)));
				
				activeCarriersTable.put(carrierLocId, carrierLoc);			
			}	
			result = true;
		} catch (SQLException se) {
			activeCarriersTable = null;
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			activeCarriersTable = null;
			result = false;
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
		return activeCarriersTable;
	}
	
	private static final String SELECT_ACTIVECARRIERS2_SQL = "SELECT CARRIERID,CARRIERLOCID,CARRIERSTATE,INSTALLTIME,FOUPID FROM STBCARRIERLOC WHERE CARRIERID IS NOT NULL";
	/**
	 * 
	 * @return
	 */
	public HashMap<String, STBCarrierLoc> uploadActiveCarriers2FromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		STBCarrierLoc carrierLoc;
		String carrierLocId;
		HashMap<String, STBCarrierLoc> activeCarriers2Table = new HashMap<String, STBCarrierLoc>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_ACTIVECARRIERS2_SQL);

			rs = pstmt.executeQuery();
			while (rs.next()) {
				carrierLoc = new STBCarrierLoc();
				carrierLocId = getString(rs.getString(CARRIERLOCID));
				carrierLoc.setCarrierLocId(carrierLocId);
				carrierLoc.setCarrierId(getString((rs.getString(CARRIERID))));				
				carrierLoc.setInstalledTime(getString(rs.getString(INSTALLTIME)));
				carrierLoc.setCarrierState(getString(rs.getString(CARRIERSTATE)));
				carrierLoc.setFoupId(getString(rs.getString(FOUPID)));
				
				activeCarriers2Table.put(carrierLocId, carrierLoc);			
			}	
			result = true;
		} catch (SQLException se) {
			activeCarriers2Table = null;
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			activeCarriers2Table = null;
			result = false;
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
		return activeCarriers2Table;
	}

	// 2018.03.12 by LSH: STBDATA_RECOVERY_OPTION=MCS 인 경우(MCS→STBC) COMMANDNAME 초기화 안하면, Reconcile 이후 REMOVE 수행하면서 Unknown Carrier 다발
//		private static final String UPDATE_CARRIERID_CLEARED_SQL = "UPDATE STBCARRIERLOC SET CARRIERID='', FOUPID='' WHERE CARRIERID IS NOT NULL";
		private static final String UPDATE_CARRIERID_CLEARED_SQL = "UPDATE STBCARRIERLOC SET CARRIERID='', FOUPID='', CARRIERSTATE='EMPTY', COMMANDNAME='', MCSCARRIERID='', OCSCARRIERID='', MCSFOUPID=''";
	/**
	 * 
	 */
	public void updateCarrierIdCleared() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_CARRIERID_CLEARED_SQL);
			pstmt.execute();			
			result = true;
		} catch (SQLException se) {
			result = false;
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}		
		if (result == false) {
			dbAccessManager.requestDBReconnect();			
		}		
	}

	/**
	 * 
	 * @return
	 */
	public HashSet<String> getRfcIdSet() {
		if (rfcIdSet == null) {
			rfcIdSet = new HashSet<String>();
			
			Iterator<Object> it = data.values().iterator();
			STBCarrierLoc carrierLoc;
			while (it.hasNext()) {
				carrierLoc = (STBCarrierLoc) it.next();
				if (carrierLoc.getRfcId() != null && carrierLoc.getRfcId().length() > 0) {
					rfcIdSet.add(carrierLoc.getRfcId());					
				}
			}
		}		
		return rfcIdSet;
	}
	
	/**
	 * 
	 * @param stbCarrierLoc
	 */
	public void updateSTBCarrierStateForOperation(STBCarrierLoc stbCarrierLoc) {
		if (updateSTBCarrierLocStateListForOperation.contains(stbCarrierLoc) == false) {
			updateSTBCarrierLocStateListForOperation.add(stbCarrierLoc);
		}
	}
	
	private static final String UPDATE_REMOVE_CARRIERSTATE_SQL = "UPDATE STBCARRIERLOC SET COMMANDNAME='REMOVE', MCSCARRIERID=?, OCSCARRIERID=?, MCSFOUPID=?, REMOVETIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE CARRIERLOCID=?";
	private static final String UPDATE_INSTALL_CARRIERSTATE_SQL = "UPDATE STBCARRIERLOC SET COMMANDNAME='INSTALL', MCSCARRIERID=?, OCSCARRIERID=?, MCSFOUPID=?, INSTALLTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), REMOVETIME='' WHERE CARRIERLOCID=?";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean updateSTBCarrierStateForOperation() {
		if (updateSTBCarrierLocStateListForOperation.size() == 0){
			return true;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<STBCarrierLoc> updateCarrierStateListClone = null;
		try {
			conn = dbAccessManager.getConnection();
			updateCarrierStateListClone = (Vector<STBCarrierLoc>)updateSTBCarrierLocStateListForOperation.clone();
			ListIterator<STBCarrierLoc> iterator = updateCarrierStateListClone.listIterator();
			STBCarrierLoc stbCarrierLoc;
			while (iterator.hasNext()) {
				stbCarrierLoc = iterator.next();
				if (stbCarrierLoc != null) {
					if (REMOVE.equals(stbCarrierLoc.getCommandName())) {
						pstmt = conn.prepareStatement(UPDATE_REMOVE_CARRIERSTATE_SQL);
					} else if (INSTALL.equals(stbCarrierLoc.getCommandName())) {
						pstmt = conn.prepareStatement(UPDATE_INSTALL_CARRIERSTATE_SQL);
					} else {
						updateSTBCarrierLocStateListForOperation.remove(stbCarrierLoc);
						continue;
					}
					pstmt.setString(1, stbCarrierLoc.getMcsCarrierId());
					pstmt.setString(2, stbCarrierLoc.getOcsCarrierId());
					pstmt.setString(3, stbCarrierLoc.getMcsFoupId());
					pstmt.setString(4, stbCarrierLoc.getCarrierLocId());
					pstmt.executeUpdate();
					pstmt.close();
				}
				updateSTBCarrierLocStateListForOperation.remove(0);
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
		} else {
			result = updateFromDB();
		}
		return result;
	}

	/**
	 * 2013.01.04 by KYK : 포트비사용전환시 이유(Reason)기록추가
	 * @param carrierLocId
	 * @param enabled
	 * @param reason
	 */
	public void updatePortServiceState(String carrierLocId, boolean enabled, String reason) {
		STBCarrierLoc stbCarrierLoc = new STBCarrierLoc();
		stbCarrierLoc.setCarrierLocId(carrierLocId);
		stbCarrierLoc.setEnabled(enabled);
		stbCarrierLoc.setReason(reason); // 2013.01.04 by KYK
		updatePortServiceStateList.add(stbCarrierLoc);
	}	
	
	// 2013.01.03 by KYK : 포트비사용전환시 이유(Reason)기록추가
//	private static final String UPDATE_PORT_SERVICESTATE_SQL = "UPDATE STBCARRIERLOC SET ENABLED=? WHERE CARRIERLOCID=?";
	private static final String UPDATE_PORT_SERVICESTATE_SQL = "UPDATE STBCARRIERLOC SET ENABLED=?,REASON=? WHERE CARRIERLOCID=?";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	// 2014.12.19 by KYK
//	private boolean updateSTBPortServiceState() {
	public boolean updateSTBPortServiceState() {
		if (updatePortServiceStateList.size() == 0) {
			return true;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<STBCarrierLoc> updatePortServiceStateListClone = null;
		try {
			conn = dbAccessManager.getConnection();
			updatePortServiceStateListClone = (Vector<STBCarrierLoc>)updatePortServiceStateList.clone();
			ListIterator<STBCarrierLoc> iterator = updatePortServiceStateListClone.listIterator();
			STBCarrierLoc stbCarrierLoc;
			while (iterator.hasNext()) {
				stbCarrierLoc = iterator.next();
				if (stbCarrierLoc != null) {
					pstmt = conn.prepareStatement(UPDATE_PORT_SERVICESTATE_SQL);
					if (stbCarrierLoc.isEnabled()) {
						pstmt.setString(1, TRUE);
					} else {
						pstmt.setString(1, FALSE);
					}
					// 2013.01.04 by KYK
//					pstmt.setString(2, stbCarrierLoc.getCarrierLocId());					
					pstmt.setString(2, stbCarrierLoc.getReason());
					pstmt.setString(3, stbCarrierLoc.getCarrierLocId());
					pstmt.executeUpdate();
					pstmt.close();
				}
				updatePortServiceStateList.remove(0);
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
		return result;
	}

	// 2012.03.10 by KYK
	public void setReadAllRequestMap(HashMap<String, ArrayList> requestMap) {
		String rfcId;
		STBCarrierLoc carrierLoc;
		ArrayList<String> portList;
		Set<String> keySet = new HashSet<String>(data.keySet());
		for (String carrierLocId: keySet) {
			carrierLoc = (STBCarrierLoc) data.get(carrierLocId);
			// 2013.08.22 by KYK
//			if (carrierLoc != null) {
			if (carrierLoc != null && carrierLoc.isEnabled()) {
				rfcId = carrierLoc.getRfcId();
				// NullCheck : rfcId 가 없으면 보낼수가 없으니깐
				if (rfcId != null && rfcId.length() > 0) {
					portList = requestMap.get(rfcId);
					if (portList == null) {
						portList = new ArrayList<String>();
						requestMap.put(rfcId, portList);
					}
					portList.add(carrierLocId);					
				}
			}
		}
	}
	
	public boolean checkDBStatus() {
		if (super.checkDBStatus()) {
			return true;
		} else {
			return super.checkDBStatus();
		}
	}

	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}
	
}
