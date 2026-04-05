package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.CARRIERLOC_TYPE;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.index.MaterialTable;

/**
 * CarrierLocManager Class, OCS 3.0 for Unified FAB
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

public class CarrierLocManager extends AbstractManager {
	private static CarrierLocManager manager = null;
	private static final String CARRIERLOCID = "CARRIERLOCID";
	private static final String TYPE = "TYPE";
	private static final String OWNER = "OWNER";
	private static final String NODE = "NODE";
	private static final String ENABLED = "ENABLED";
	private static final String LEFT = "LEFT";
	private static final String TOP = "TOP";
	private static final String ENGINEERINFO = "ENGINEERINFO";
	private static final String USERGROUPID = "USERGROUPID";
	private static final String LOCALGROUPID = "LOCALGROUPID";
	private static final String MATERIAL = "MATERIAL";
	private static final String MULTIPORT = "MULTIPORT";
	private static final String USERREQUEST = "USERREQUEST";
	// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정	
	private static final String AUTORETRYGROUPID = "AUTORETRYGROUPID";
	
	// 2017.10.24 by LSH : Port ID 변경
	private static final String CHANGEIDREQUEST = "CHANGEIDREQUEST";
	private static final String CHANGEOWNERREQUEST = "CHANGEOWNERREQUEST";
	
	// 2022.03.30 by JJW : Port Material 변경
	private static final String CHANGEMATERIALREQUEST = "CHANGEMATERIALREQUEST";
	
	// 2013.02.01 by KYK
	private static final String STATIONID = "STATIONID";
	private static final String PIODIRECTION = "PIODIRECTION";
	private static final String HOISTPOSITION = "HOISTPOSITION";
	private static final String SHIFTPOSITION = "SHIFTPOSITION";
	private static final String ROTATEPOSITION = "ROTATEPOSITION";	
	private static final String PIOTIMELEVEL = "PIOTIMELEVEL";
	private static final String LOOKDOWNLEVEL = "LOOKDOWNLEVEL";
	private static final String SUBTYPE = "SUBTYPE";
	private static final String EXTRAOPTION = "EXTRAOPTION";
	// 2014.10.30 by KYK [DualOHT]
	private static final String RFPIOID = "RFPIOID";	
	private static final String RFPIOCS = "RFPIOCS";	
	
	private boolean isAllLocalGroupIdCleared = false;
	private boolean isRuntimeUpdateRequested = false;
	private boolean isRuntimeUpdatable = false;
	private HashMap<String, Integer> materialIndexMapOfLocalGroup = new HashMap<String, Integer>();
	private Vector<String> clearLocalGroupIdList = new Vector<String>();
	private Vector<CarrierLoc> localGroupIdRegisteredCarrierLocIdList = new Vector<CarrierLoc>();
	private Vector<CarrierLoc> userGroupIdRegisteredCarrierLocIdList = new Vector<CarrierLoc>();
	private Vector<CarrierLoc> autoRetryGroupRegisteredCarrierLocIdList = new Vector<CarrierLoc>();
	
	private Map<String, String> changeIdRequestList = new HashMap<String, String>();
	private Map<String, String> changeOwnerRequestList = new HashMap<String, String>();
	private Map<String, String> changeMaterialRequestList = new HashMap<String, String>(); // 2022.04.20 by JJW Material 변경 기능 추가
	
	private MaterialTable materialTable = null;
	
	// 2013.10.22 by KYK
	private OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
	private int dataRevision = ocsInfoManager.getTeachingDataRevision();
	private boolean isIBSEM = false;
	
	private static final int INVALID_TEACHING = -9999;
	
	/**
	 * Constructor of CarrierLocManager class.
	 */
	private CarrierLocManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(CarrierLoc.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		materialTable = MaterialTable.getInstance();
	}
	
	/**
	 * Constructor of CarrierLocManager class. (Singleton)
	 */
	public static synchronized CarrierLocManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new CarrierLocManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		if (isRuntimeUpdateRequested == false) {
			isRuntimeUpdatable = false;
			if (isInitialized) {
				// 2013.10.22 by KYK
				if (hasRevisionChanged()) {
					setDataRevision(ocsInfoManager.getTeachingDataRevision());
					updateTeachingDataFromDB();
				}
				if (isIBSEM == false) {
					updateGroupInfo();
				}
			} else {
				// 어떤 경우?? 예방 차원
				init();
			}
		} 
		isRuntimeUpdatable = true;
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	/**
	 * 
	 */
	@Override
	public boolean updateToDB() {
		if (clearLocalGroupIdList.size() > 0) {
			clearLocalGroupId();
		}
		return true;
	}
	
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
		materialIndexMapOfLocalGroup.clear();
		clearLocalGroupIdList.clear();
		userGroupIdRegisteredCarrierLocIdList.clear();
		localGroupIdRegisteredCarrierLocIdList.clear();
		autoRetryGroupRegisteredCarrierLocIdList.clear();
		
		init();
		isRuntimeUpdateRequested = false;
	}
	
	private static final String INITIALIZE_SQL = "SELECT * FROM CARRIERLOC";
	/**
	 * 
	 * @return
	 */
	private boolean initialize() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		materialTable = MaterialTable.getInstance();
		
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			pstmt = conn.prepareStatement(INITIALIZE_SQL);
			rs = pstmt.executeQuery();
			String carrierLocId = null;
			CarrierLoc carrierLoc = null;
			while (rs.next()) {
				carrierLocId = rs.getString(CARRIERLOCID);
				carrierLoc = (CarrierLoc) data.get(carrierLocId);
				if (carrierLoc == null) {
					carrierLoc = (CarrierLoc) vOType.newInstance();
					data.put(carrierLocId, carrierLoc);
					setCarrierLoc(carrierLoc, rs);
				} else {
					setCarrierLoc(carrierLoc, rs);
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
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
	
	/**
	 * Request to Port ID RuntimeUpdate
	 */
	public boolean changePortIDFromDB() {
		isRuntimeUpdateRequested = true;
		isInitialized = false;
		boolean result = false;
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
		changeOwnerRequestList.clear();
		
		result = changePortID();

		isInitialized = true;
		isRuntimeUpdateRequested = false;
		
		return result;
	}
	
	/**
	 * Request to Port ID RuntimeUpdate
	 */
	// 2022.04.20 by JJW Material 변경 기능 추가
	public boolean changePortMaterialFromDB() {
		isRuntimeUpdateRequested = true;
		isInitialized = false;
		boolean result = false;
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
		
		changeMaterialRequestList.clear();
		
		result = changePortMaterial();

		isInitialized = true;
		isRuntimeUpdateRequested = false;
		
		return result;
	}
	
	private static final String SELECT_CHANGEIDREQUEST_SQL = "SELECT * FROM CARRIERLOC A WHERE CHANGEIDREQUEST IS NOT NULL AND CHANGEOWNERREQUEST IS NOT NULL AND NOT EXISTS (SELECT CARRIERLOCID FROM CARRIERLOC B WHERE A.CHANGEIDREQUEST=B.CARRIERLOCID) AND NOT EXISTS (SELECT OWNER FROM CARRIERLOC B WHERE A.CHANGEOWNERREQUEST=B.OWNER AND A.CHANGEOWNERREQUEST!=A.OWNER) AND CHANGEIDREQUEST IN (SELECT CHANGEIDREQUEST FROM CARRIERLOC GROUP BY CHANGEIDREQUEST HAVING COUNT(*) = 1) AND 1 = (SELECT COUNT(DISTINCT(OWNER)) from CARRIERLOC B WHERE B.CHANGEOWNERREQUEST=A.CHANGEOWNERREQUEST) AND 1 = (SELECT COUNT(DISTINCT(CHANGEOWNERREQUEST)) from CARRIERLOC B WHERE B.OWNER=A.OWNER) AND (SELECT COUNT(CHANGEOWNERREQUEST) from CARRIERLOC B WHERE B.OWNER=A.OWNER AND B.OWNER!=A.CHANGEOWNERREQUEST) = (SELECT COUNT(OWNER) from CARRIERLOC B WHERE B.OWNER=A.OWNER AND B.OWNER!=A.CHANGEOWNERREQUEST)";
	private static final String UPDATE_CHANGEIDREQUEST_SQL = "UPDATE CARRIERLOC SET CARRIERLOCID=?, HOISTPOSITION=-9999, SHIFTPOSITION=-9999, ROTATEPOSITION=-9999, PIODIRECTION='1', PIOTIMELEVEL=100 , LOOKDOWNLEVEL=15, EXTRAOPTION=56, CHANGEIDREQUEST='' WHERE CARRIERLOCID=? AND CHANGEIDREQUEST=?";
	private static final String UPDATE_CHANGEOWNERREQUEST_SQL = "UPDATE CARRIERLOC SET OWNER=?, CHANGEOWNERREQUEST='' WHERE CHANGEOWNERREQUEST=?";
	private static final String UPDATE_CHANGEDEVICEIDREQUEST_SQL = "UPDATE DEVICE SET DEVICEID=? WHERE DEVICEID=?";

	private boolean changePortID() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		materialTable = MaterialTable.getInstance();
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
			String owner = null;
			String changeOwnerRequest = null;
			CarrierLoc carrierLoc = null;
			
			while (rs.next()) {
				carrierLocId = rs.getString(CARRIERLOCID);
				changeIdRequest = rs.getString(CHANGEIDREQUEST);
				changeIdRequestList.put(carrierLocId, changeIdRequest);
				
				owner = rs.getString(OWNER);
				changeOwnerRequest = rs.getString(CHANGEOWNERREQUEST);
				changeOwnerRequestList.put(owner, changeOwnerRequest);
			}
			
			pstmt = conn.prepareStatement(UPDATE_CHANGEIDREQUEST_SQL);
			Iterator<String> iter = changeIdRequestList.keySet().iterator();
			while (iter.hasNext()) {
				carrierLocId = iter.next();
				carrierLoc = (CarrierLoc) data.get(carrierLocId);
				if (carrierLoc != null) {
					changeIdRequest = changeIdRequestList.get(carrierLocId);
					pstmt.setString(1, changeIdRequest);
					pstmt.setString(2, carrierLocId);
					pstmt.setString(3, changeIdRequest);
					pstmt.executeUpdate();
				}
			}
			
			String changeRequestId = null;
			pstmt = conn.prepareStatement(UPDATE_CHANGEOWNERREQUEST_SQL);
			Iterator<String> iter2 = changeOwnerRequestList.keySet().iterator();
			while (iter2.hasNext()) {
				owner = iter2.next();
				changeRequestId = changeOwnerRequestList.get(owner);
				pstmt.setString(1, changeRequestId);
				pstmt.setString(2, changeRequestId);
				pstmt.executeUpdate();
			}
			
			changeRequestId = null;
			pstmt = conn.prepareStatement(UPDATE_CHANGEDEVICEIDREQUEST_SQL);
			Iterator<String> iter3 = changeOwnerRequestList.keySet().iterator();
			while (iter3.hasNext()) {
				owner = iter3.next();
				changeRequestId = changeOwnerRequestList.get(owner);
				pstmt.setString(1, changeRequestId);
				pstmt.setString(2, owner);
				pstmt.executeUpdate();
			}
			
			carrierLocId = null;
			changeIdRequest = null;
			owner = null;
			changeOwnerRequest = null;
			rs.beforeFirst();
			while (rs.next()) {
				carrierLocId = rs.getString(CARRIERLOCID);
				carrierLoc = (CarrierLoc) data.get(carrierLocId);
				if (carrierLoc != null) {
					owner = carrierLoc.getOwner();
					changeOwnerRequest = changeOwnerRequestList.get(owner);
					localGroupIdRegisteredCarrierLocIdList.remove(carrierLoc);
					userGroupIdRegisteredCarrierLocIdList.remove(carrierLoc);
					autoRetryGroupRegisteredCarrierLocIdList.remove(carrierLoc);
					data.remove(carrierLocId);
				}
				changeIdRequest = rs.getString(CHANGEIDREQUEST);
				carrierLoc = (CarrierLoc) data.get(changeIdRequest);
				if (carrierLoc == null) {
					carrierLoc = (CarrierLoc) vOType.newInstance();
					setCarrierLocforChangeID(carrierLoc, changeIdRequest, changeOwnerRequest, rs);
					data.put(changeIdRequest, carrierLoc);
				}
			}
			
			conn.commit();
			conn.setAutoCommit(true);
			result = true;
		} catch (SQLException se) {
			try { if (conn != null) conn.rollback(); } catch (SQLException ignore) {}
			try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			try { if (conn != null) conn.rollback(); } catch (SQLException ignore) {}
			try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
			try { if (conn != null) conn.rollback(); } catch (SQLException ignore) {}
			try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
			try { if (conn!=null) conn.rollback(); } catch (SQLException ignore) {}
			try { if (conn!=null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
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

	// 2022.04.20 by JJW Material 변경 기능 추가
	private static final String SELECT_CHANGEMATERIALREQUEST_SQL = "SELECT * FROM CARRIERLOC WHERE CHANGEMATERIALREQUEST IS NOT NULL";
	private static final String UPDATE_CHANGEMATERIALREQUEST_SQL = "UPDATE CARRIERLOC SET MATERIAL=?, CHANGEMATERIALREQUEST='' WHERE CARRIERLOCID=? AND CHANGEMATERIALREQUEST=?";
	
	// 2022.04.20 by JJW Material 변경 기능 추가
	private boolean changePortMaterial() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		materialTable = MaterialTable.getInstance();
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(SELECT_CHANGEMATERIALREQUEST_SQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = pstmt.executeQuery();
			String carrierLocId = null;
			String material = null;
			String changeMaterialRequest = null;
			CarrierLoc carrierLoc = null;
			
			while (rs.next()) {
				carrierLocId = rs.getString(CARRIERLOCID);
				changeMaterialRequest = rs.getString(CHANGEMATERIALREQUEST);
				changeMaterialRequestList.put(carrierLocId, changeMaterialRequest);
			}
			
			pstmt = conn.prepareStatement(UPDATE_CHANGEMATERIALREQUEST_SQL);
			Iterator<String> iter = changeMaterialRequestList.keySet().iterator();
			while (iter.hasNext()) {
				carrierLocId = iter.next();
				carrierLoc = (CarrierLoc) data.get(carrierLocId);
				if (carrierLoc != null) {
					changeMaterialRequest = changeMaterialRequestList.get(carrierLocId);
					pstmt.setString(1, changeMaterialRequest);
					pstmt.setString(2, carrierLocId);
					pstmt.setString(3, changeMaterialRequest);
					pstmt.executeUpdate();
				}
			}
			
			
			carrierLocId = null;
			material = null;
			changeMaterialRequest = null;
			rs.beforeFirst();
			while (rs.next()) {
				carrierLocId = rs.getString(CARRIERLOCID);
				carrierLoc = (CarrierLoc) data.get(carrierLocId);
				if (carrierLoc != null) {
					material = carrierLoc.getMaterial();
					changeMaterialRequest = changeMaterialRequestList.get(carrierLocId);
					data.remove(carrierLocId);
				}
				changeMaterialRequest = rs.getString(CHANGEMATERIALREQUEST);
				carrierLoc = (CarrierLoc) data.get(carrierLocId);
				if (carrierLoc == null) {
					carrierLoc = (CarrierLoc) vOType.newInstance();
					setCarrierLocforChangeMaterial(carrierLoc, changeMaterialRequest, rs);
					data.put(carrierLocId, carrierLoc);
				}
			}
			
			conn.commit();
			conn.setAutoCommit(true);
			result = true;
		} catch (SQLException se) {
			try { if (conn != null) conn.rollback(); } catch (SQLException ignore) {}
			try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			try { if (conn != null) conn.rollback(); } catch (SQLException ignore) {}
			try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
			try { if (conn != null) conn.rollback(); } catch (SQLException ignore) {}
			try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
			try { if (conn!=null) conn.rollback(); } catch (SQLException ignore) {}
			try { if (conn!=null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
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

//	private static final String UPDATE_SQL = "SELECT * FROM CARRIERLOC WHERE LOCALGROUPID IS NOT NULL OR USERGROUPID IS NOT NULL";
	private static final String UPDATE_SQL = "SELECT * FROM CARRIERLOC WHERE LOCALGROUPID IS NOT NULL OR USERGROUPID IS NOT NULL OR AUTORETRYGROUPID IS NOT NULL";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean updateGroupInfo() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			pstmt = conn.prepareStatement(UPDATE_SQL);
			rs = pstmt.executeQuery();
			Set<String> removeLocalGroupKeys = new HashSet<String>(materialIndexMapOfLocalGroup.keySet());
			Vector<CarrierLoc> localGroupIdRegisteredCarrierLocIdListClone = (Vector<CarrierLoc>) localGroupIdRegisteredCarrierLocIdList.clone();
			Vector<CarrierLoc> userGroupIdRegisteredCarrierLocIdListClone = (Vector<CarrierLoc>) userGroupIdRegisteredCarrierLocIdList.clone();
			Vector<CarrierLoc> autoRetryGroupRegisteredCarrierLocIdListClone = (Vector<CarrierLoc>) autoRetryGroupRegisteredCarrierLocIdList.clone();
			localGroupIdRegisteredCarrierLocIdList.clear();
			userGroupIdRegisteredCarrierLocIdList.clear();
			autoRetryGroupRegisteredCarrierLocIdList.clear();
			String carrierLocId = null;
			CarrierLoc carrierLoc = null;
			while (rs.next()) {
				if (isRuntimeUpdateRequested) {
					return false;
				}
				carrierLocId = rs.getString(CARRIERLOCID);
				carrierLoc = (CarrierLoc) data.get(carrierLocId);
				if (carrierLoc != null) {
					setGroupInfo(carrierLoc, rs);
					
					if (carrierLoc.getLocalGroupId() != null && carrierLoc.getLocalGroupId().length() > 0) {
						localGroupIdRegisteredCarrierLocIdList.add(carrierLoc);
						localGroupIdRegisteredCarrierLocIdListClone.remove(carrierLoc);
					}
					if (carrierLoc.getUserGroupId() != null && carrierLoc.getUserGroupId().length() > 0) {
						userGroupIdRegisteredCarrierLocIdList.add(carrierLoc);
						userGroupIdRegisteredCarrierLocIdListClone.remove(carrierLoc);
					}
					// 2013.09.10 by MYM : AutoRetryGroup 실시간 반영
					if (carrierLoc.getAutoRetryGroupId() != null && carrierLoc.getAutoRetryGroupId().length() > 0) {
						autoRetryGroupRegisteredCarrierLocIdList.add(carrierLoc);
						autoRetryGroupRegisteredCarrierLocIdListClone.remove(carrierLoc);
					}
					if (carrierLoc.getLocalGroupId() != null && carrierLoc.getLocalGroupId().length() > 0) {
						materialIndexMapOfLocalGroup.put(carrierLoc.getLocalGroupId(), Integer.valueOf(carrierLoc.getMaterialIndex()));
						removeLocalGroupKeys.remove(carrierLoc.getLocalGroupId());
					}
				}
			}
			if (localGroupIdRegisteredCarrierLocIdListClone != null &&
					localGroupIdRegisteredCarrierLocIdListClone.size() > 0) {
				for (CarrierLoc clearLocalGroupCarrierLoc : localGroupIdRegisteredCarrierLocIdListClone) {
					clearLocalGroupCarrierLoc.setLocalGroupId("");
				}
			}
			if (userGroupIdRegisteredCarrierLocIdListClone != null &&
					userGroupIdRegisteredCarrierLocIdListClone.size() > 0) {
				for (CarrierLoc clearUserGroupCarrierLoc : userGroupIdRegisteredCarrierLocIdListClone) {
					clearUserGroupCarrierLoc.setLocalGroupId("");
				}
			}
			// 2013.09.10 by MYM : AutoRetryGroup 실시간 반영
			if (autoRetryGroupRegisteredCarrierLocIdListClone != null &&
					autoRetryGroupRegisteredCarrierLocIdListClone.size() > 0) {
				for (CarrierLoc clearAutoRetryGroupCarrierLoc : autoRetryGroupRegisteredCarrierLocIdListClone) {
					clearAutoRetryGroupCarrierLoc.setAutoRetryGroupId("");
				}
			}
			for (String rmKey : removeLocalGroupKeys) {
				materialIndexMapOfLocalGroup.remove(rmKey);
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
	 * @param carrierLoc
	 * @param rs
	 * @exception SQLException
	 */
	private void setCarrierLoc(CarrierLoc carrierLoc, ResultSet rs ) throws SQLException {
		carrierLoc.setCarrierLocId(getString(rs.getString(CARRIERLOCID)));
		carrierLoc.setType(CARRIERLOC_TYPE.toCarrierlocType(rs.getString(TYPE)));
		carrierLoc.setOwner(getString(rs.getString(OWNER)));
		carrierLoc.setNode(getString(rs.getString(NODE)));
		carrierLoc.setEnabled(getBoolean(rs.getString(ENABLED)));
		carrierLoc.setLeft(rs.getLong(LEFT));
		carrierLoc.setTop(rs.getLong(TOP));
		carrierLoc.setEngineerInfo(getString(rs.getString(ENGINEERINFO)));
		carrierLoc.setUserGroupId(getString(rs.getString(USERGROUPID)));
		carrierLoc.setLocalGroupId(getString(rs.getString(LOCALGROUPID)));
		setCarrierLocMaterial(carrierLoc, getString(rs.getString(MATERIAL)));
		carrierLoc.setMultiPort(getBoolean(rs.getString(MULTIPORT)));
		// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
		carrierLoc.setAutoRetryGroupId(getString(rs.getString(AUTORETRYGROUPID)));
		// 2013.02.15 by KYK
		carrierLoc.setStationId(getString(rs.getString(STATIONID)));
		carrierLoc.setSubType(Integer.valueOf(rs.getString(SUBTYPE), 16));
		// 2013.11.12 by KYK
		setCarrierLocTeachingValue(carrierLoc, rs);
//		carrierLoc.setPioDirection(Integer.valueOf(rs.getString(PIODIRECTION), 16));
//		carrierLoc.setHoistPosition(rs.getInt(HOISTPOSITION));
//		carrierLoc.setShiftPosition(rs.getInt(SHIFTPOSITION));
//		carrierLoc.setRotatePosition(rs.getInt(ROTATEPOSITION));
//		carrierLoc.setPioTimeLevel(rs.getInt(PIOTIMELEVEL));
//		carrierLoc.setLookDownLevel(rs.getInt(LOOKDOWNLEVEL));
//		carrierLoc.setExtraOption(Integer.valueOf(rs.getString(EXTRAOPTION)));
		
		// 2014.10.30 by KYK [DualOHT] TODO check
		carrierLoc.setRfPioId(getString(rs.getString(RFPIOID)));
		carrierLoc.setRfPioCS(rs.getInt(RFPIOCS));
		
		if (carrierLoc.getLocalGroupId() != null && carrierLoc.getLocalGroupId().length() > 0) {
			isAllLocalGroupIdCleared = false;
		}
	}
	
	/**
	 * 
	 * @param carrierLoc
	 * @param changeIdRequest
	 * @param changeOwnerRequest
	 * @param rs
	 * @exception SQLException
	 */
	private void setCarrierLocforChangeID(CarrierLoc carrierLoc, String changeIdRequest, String changeOwnerRequest, ResultSet rs) throws SQLException {
		carrierLoc.setCarrierLocId(changeIdRequest);
		carrierLoc.setType(CARRIERLOC_TYPE.toCarrierlocType(rs.getString(TYPE)));
		carrierLoc.setOwner(changeOwnerRequest);
		carrierLoc.setNode(getString(rs.getString(NODE)));
		carrierLoc.setEnabled(getBoolean(rs.getString(ENABLED)));
		carrierLoc.setLeft(rs.getLong(LEFT));
		carrierLoc.setTop(rs.getLong(TOP));
		carrierLoc.setEngineerInfo(getString(rs.getString(ENGINEERINFO)));
		carrierLoc.setUserGroupId(getString(rs.getString(USERGROUPID)));
		carrierLoc.setLocalGroupId(getString(rs.getString(LOCALGROUPID)));
		setCarrierLocMaterial(carrierLoc, getString(rs.getString(MATERIAL)));
		carrierLoc.setMultiPort(getBoolean(rs.getString(MULTIPORT)));
		// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
		carrierLoc.setAutoRetryGroupId(getString(rs.getString(AUTORETRYGROUPID)));
		// 2013.02.15 by KYK
		carrierLoc.setStationId(getString(rs.getString(STATIONID)));
		carrierLoc.setSubType(Integer.valueOf(rs.getString(SUBTYPE), 16));
		
		carrierLoc.setHoistPosition(-9999);
		carrierLoc.setShiftPosition(-9999);
		carrierLoc.setRotatePosition(-9999);
		carrierLoc.setPioDirection(1);
		carrierLoc.setPioTimeLevel(100);
		carrierLoc.setLookDownLevel(15);
		carrierLoc.setExtraOption(56);
		carrierLoc.setValid(false);
		
		// 2014.10.30 by KYK [DualOHT] TODO check
		carrierLoc.setRfPioId(getString(rs.getString(RFPIOID)));
		carrierLoc.setRfPioCS(rs.getInt(RFPIOCS));
		
		if (carrierLoc.getLocalGroupId() != null && carrierLoc.getLocalGroupId().length() > 0) {
			isAllLocalGroupIdCleared = false;
		}
	}
	
	/**
	 * @param carrierLoc
	 * @param changeIdRequest
	 * @param rs
	 * @throws SQLException
	 */
	// 2022.04.20 by JJW Material 변경 기능 추가
	private void setCarrierLocforChangeMaterial(CarrierLoc carrierLoc, String changeMaterialRequest, ResultSet rs) throws SQLException {
		carrierLoc.setCarrierLocId(getString(rs.getString(CARRIERLOCID)));
		carrierLoc.setType(CARRIERLOC_TYPE.toCarrierlocType(rs.getString(TYPE)));
		carrierLoc.setNode(getString(rs.getString(NODE)));
		carrierLoc.setEnabled(getBoolean(rs.getString(ENABLED)));
		carrierLoc.setLeft(rs.getLong(LEFT));
		carrierLoc.setTop(rs.getLong(TOP));
		carrierLoc.setEngineerInfo(getString(rs.getString(ENGINEERINFO)));
		carrierLoc.setUserGroupId(getString(rs.getString(USERGROUPID)));
		carrierLoc.setLocalGroupId(getString(rs.getString(LOCALGROUPID)));
		setCarrierLocMaterial(carrierLoc, changeMaterialRequest);
		carrierLoc.setMultiPort(getBoolean(rs.getString(MULTIPORT)));
		// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
		carrierLoc.setAutoRetryGroupId(getString(rs.getString(AUTORETRYGROUPID)));
		// 2013.02.15 by KYK
		carrierLoc.setStationId(getString(rs.getString(STATIONID)));
		carrierLoc.setSubType(Integer.valueOf(rs.getString(SUBTYPE), 16));
		
		// 2014.10.30 by KYK [DualOHT] TODO check
		carrierLoc.setRfPioId(getString(rs.getString(RFPIOID)));
		carrierLoc.setRfPioCS(rs.getInt(RFPIOCS));
		
		if (carrierLoc.getLocalGroupId() != null && carrierLoc.getLocalGroupId().length() > 0) {
			isAllLocalGroupIdCleared = false;
		}
	}
	
	private void setCarrierLocMaterial(CarrierLoc carrierLoc, String material) {
		carrierLoc.setMaterial(material);
		carrierLoc.setMaterialIndex(materialTable.getMaterialIndex(material));
	}
	
	/**
	 * 
	 * @param carrierLoc
	 * @param rs
	 * @exception SQLException
	 */
	private void setGroupInfo(CarrierLoc carrierLoc, ResultSet rs ) throws SQLException {
		carrierLoc.setUserGroupId(getString(rs.getString(USERGROUPID)));
		carrierLoc.setLocalGroupId(getString(rs.getString(LOCALGROUPID)));
		carrierLoc.setAutoRetryGroupId(getString(rs.getString(AUTORETRYGROUPID)));
		
		if (carrierLoc.getLocalGroupId() != null && carrierLoc.getLocalGroupId().length() > 0) {
			isAllLocalGroupIdCleared = false;
		}
	}

	/**
	 * 
	 * @param carrierLocId
	 * @return
	 */
	public CarrierLoc getCarrierLocData(String carrierLocId) {
		if (carrierLocId == null || data.containsKey(carrierLocId) == false) {
			return null;
		} 
		return (CarrierLoc) data.get(carrierLocId);
	}
	
	/**
	 * 
	 * @param carrierLocId
	 * @return
	 */
	public boolean isValidCarrierLoc(String carrierLocId) {
		if (data.containsKey(carrierLocId)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param localGroupId
	 * @return
	 */
	public boolean clearLocalGroupId(String localGroupId) {
		if (localGroupId != null && localGroupId.length() > 0) {
			addLocalGroupIdClearList(localGroupId);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param localGroupId
	 */
	private void addLocalGroupIdClearList(String localGroupId) {
		if (clearLocalGroupIdList.contains(localGroupId) == false) {
			clearLocalGroupIdList.add(localGroupId);
		}
	}
	
	private static final String CLEAR_LOCALGROUPID_SQL = "UPDATE CARRIERLOC SET LOCALGROUPID='' WHERE LOCALGROUPID=?";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean clearLocalGroupId() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<String> clearLocalGroupInfoListClone = null;
		try {
			conn = dbAccessManager.getConnection();
			clearLocalGroupInfoListClone = (Vector<String>)clearLocalGroupIdList.clone();
			ListIterator<String> iterator = clearLocalGroupInfoListClone.listIterator();
			String localGroupId;
			pstmt = conn.prepareStatement(CLEAR_LOCALGROUPID_SQL);
			
			while (iterator.hasNext()) {
				localGroupId = iterator.next();
				if (localGroupId != null) {
					pstmt.setString(1, localGroupId);
					pstmt.executeUpdate();
				}
				clearLocalGroupIdList.remove(0);
			}
		
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
	
	private static final String CLEAR_ALL_LOCALGROUPID_SQL = "UPDATE CARRIERLOC SET LOCALGROUPID=''";
	/**
	 * 
	 * @return
	 */
	public boolean clearAllLocalGroupId() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(CLEAR_ALL_LOCALGROUPID_SQL);
			pstmt.execute();
			result = true;
			isAllLocalGroupIdCleared = true;
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
	 * @return
	 */
	public boolean isAllLocalGroupIdCleared() {
		return isAllLocalGroupIdCleared;
	}
	
	/*
	 * Add Methods For IBSEM ONLY
	 */
	private static String SELECT_STBCARRIERLOC_SQL = "SELECT * FROM CARRIERLOC WHERE (TYPE = 'STBPORT' OR TYPE = 'UTBPORT')";
	/**
	 * 
	 * @return
	 */
	public HashMap<String, CarrierLoc> getCurrentPortStateFromCarrierLocDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		CarrierLoc carrierLoc;
		HashMap<String, CarrierLoc> currentPortTable = new HashMap<String, CarrierLoc>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_STBCARRIERLOC_SQL);
			rs = pstmt.executeQuery();
			String carrierLocId = null;
			while (rs.next()) {
				carrierLoc = (CarrierLoc) vOType.newInstance();
				carrierLocId = rs.getString(CARRIERLOCID);
				setCarrierLoc(carrierLoc, rs);
				currentPortTable.put(carrierLocId, carrierLoc);
			}
		} catch (SQLException se) {
			carrierLoc = null;
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			carrierLoc = null;
			e.printStackTrace();
			dbAccessManager.requestDBReconnect();
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
		return currentPortTable;
	}
	
	// 2015.09.02 by MYM : batch Update → 개별 Update로 변경 (DB Deadlock 방지)
//	private static final String UPDATE_PORT_USERREQUEST_RESET_SQL = "UPDATE CARRIERLOC SET USERREQUEST='' WHERE ((USERREQUEST='PortInService' AND ENABLED='TRUE') OR (USERREQUEST='PortOutOfService' AND ENABLED='FALSE'))";
	private static final String RESET_MISMATCH_USERREQUEST_SQL = "UPDATE CARRIERLOC SET USERREQUEST='' WHERE ((USERREQUEST='PortInService' AND ENABLED='TRUE') OR (USERREQUEST='PortOutOfService' AND ENABLED='FALSE'))";
	/**
	 * 2015.03.18 by MYM : 장애 지역 우회 기능 (UserRequest와 Enabled가 불일치한 경우 UserRequest Reset 분리)
	 * @return
	 */
	public boolean resetUserRequestForMismatchWithEnabled() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			// (TRUE,PortInService), (FALSE,PortOutOfService)인 경우는 USERREQUEST Reset
			pstmt = conn.prepareStatement(RESET_MISMATCH_USERREQUEST_SQL);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
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
	
	private static String SELECT_USERREQUEST_PORT_SQL = "SELECT * FROM CARRIERLOC WHERE TYPE<>'VEHICLEPORT' AND ((USERREQUEST='PortOutOfService' AND ENABLED='TRUE') OR (USERREQUEST='PortInService' AND ENABLED='FALSE'))";
	/**
	 * 2015.02.04 by MYM : 장애 지역 우회 기능 (PortInService/PortOutOfservice 대응)
	 * @return
	 */
	public HashMap<String, CarrierLoc> getRequestedPortFromCarrierLocDB() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		CarrierLoc carrierLoc;
		HashMap<String, CarrierLoc> requestedPortTable = new HashMap<String, CarrierLoc>();
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_USERREQUEST_PORT_SQL);
			rs = pstmt.executeQuery();
			String carrierLocId = null;
			while (rs.next()) {
				carrierLoc = (CarrierLoc) vOType.newInstance();
				carrierLocId = rs.getString(CARRIERLOCID);
				setCarrierLoc(carrierLoc, rs);
				carrierLoc.setUserRequest(getString(rs.getString(USERREQUEST)));
				requestedPortTable.put(carrierLocId, carrierLoc);
			}
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
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
		return requestedPortTable;
	}
	
	// 2015.09.02 by MYM : batch Update → 개별 Update로 변경 (DB Deadlock 방지)
//	private static final String UPDATE_PORT_USERREQUEST_RESET_BATCH_SQL = "UPDATE CARRIERLOC SET ENABLED=?, USERREQUEST='' WHERE CARRIERLOCID=?";
	private static final String RESET_USERREQUEST_SQL = "UPDATE CARRIERLOC SET ENABLED=?, USERREQUEST='' WHERE CARRIERLOCID=?";
	/**
	 * 2015.02.04 by MYM : 2015.02.04 by MYM : 장애 지역 우회 기능 (PortInService/PortOutOfservice 대응)
	 * @param carrierLocMap
	 * @return
	 */
	public boolean resetUserRequestFromDB(HashMap<String, Boolean> carrierLocMap) {
		if (carrierLocMap.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			// 2015.09.02 by MYM : batch Update → 개별 Update로 변경 (DB Deadlock 방지)
			pstmt = conn.prepareStatement(RESET_USERREQUEST_SQL);
			Iterator<String> iter = carrierLocMap.keySet().iterator();
			while (iter.hasNext()) {
				String carrierlocId = iter.next();
				boolean isEnabled = carrierLocMap.get(carrierlocId);
				pstmt.setString(1, isEnabled ? OcsConstant.TRUE : OcsConstant.FALSE);
				pstmt.setString(2, carrierlocId);
				pstmt.executeUpdate();
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
	
	/**
	 * 
	 * @param localGroupId
	 * @return
	 */
	public int getMaterialIndexOfLocalGroup(String localGroupId) {
		try {
			Integer materialIndex = (Integer)materialIndexMapOfLocalGroup.get(localGroupId);
			if (materialIndex != null) {
				return materialIndex.intValue();
			}
		} catch (Exception e) {}
		return 9999;
	}
	
	/**
	 * 
	 * @return materialIndexMapOfLocalGroup in HashMap<String, Integer>
	 */
	public HashMap<String, Integer> getMaterialIndexMapOfLocalGroup() {
		return materialIndexMapOfLocalGroup;
	}
	
	private static final String UPDATE_TEACHINGDATA_SQL = "UPDATE CARRIERLOC SET HOISTPOSITION=?, SHIFTPOSITION=?, ROTATEPOSITION=?, PIODIRECTION=?, PIOTIMELEVEL=? , LOOKDOWNLEVEL=?  WHERE CARRIERLOCID=?";
	
	/**
	 * 2013.04.22 by MYM : Teacing 데이터 업데이트
	 * 
	 * @param stationId
	 * @param locationType
	 * @param multiPort
	 * @param hoistPosition
	 * @param shiftPosition
	 * @param rotatePosition
	 * @param pioDirection
	 * @return
	 */
	public boolean updateTeachingDataToDB(CarrierLoc carrierloc,
			int hoistPosition, int shiftPosition, int rotatePosition, int pioDirection, int pioTimeLevel, int lookDownLevel) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_TEACHINGDATA_SQL);
			pstmt.setInt(1, hoistPosition);
			pstmt.setInt(2, shiftPosition);
			pstmt.setInt(3, rotatePosition);
			pstmt.setInt(4, pioDirection);
			pstmt.setInt(5, pioTimeLevel);
			pstmt.setInt(6, lookDownLevel);
			pstmt.setString(7, carrierloc.getCarrierLocId());
			pstmt.executeUpdate();
			
			carrierloc.setHoistPosition(hoistPosition);
			carrierloc.setShiftPosition(shiftPosition);
			carrierloc.setRotatePosition(rotatePosition);
			carrierloc.setPioDirection(pioDirection);
			carrierloc.setPioTimeLevel(pioTimeLevel);
			carrierloc.setLookDownLevel(lookDownLevel);
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;				
	}
	
	/**
	 * 2013.10.22 by KYK
	 * @return
	 */
	private boolean updateTeachingDataFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INITIALIZE_SQL);
			rs = pstmt.executeQuery();
			String carrierLocId = null;
			CarrierLoc carrierLoc = null;
			while (rs.next()) {
				carrierLocId = rs.getString(CARRIERLOCID);
				carrierLoc = (CarrierLoc) data.get(carrierLocId);
				if (carrierLoc != null) {
					setCarrierLocTeachingValue(carrierLoc, rs);
				}
			}
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
	/**
	 * 2013.10.22 by KYK
	 * @param carrierLoc
	 * @param rs
	 * @throws SQLException
	 */
	private void setCarrierLocTeachingValue(CarrierLoc carrierLoc, ResultSet rs ) throws SQLException {
		int hoistPosition = rs.getInt(HOISTPOSITION);
		int shiftPosition = rs.getInt(SHIFTPOSITION);
		int ratatePosition = rs.getInt(ROTATEPOSITION);
		
		carrierLoc.setHoistPosition(hoistPosition);
		carrierLoc.setShiftPosition(shiftPosition);
		carrierLoc.setRotatePosition(ratatePosition);
		carrierLoc.setPioDirection(Integer.valueOf(rs.getString(PIODIRECTION), 16));
		carrierLoc.setPioTimeLevel(rs.getInt(PIOTIMELEVEL));
		carrierLoc.setLookDownLevel(rs.getInt(LOOKDOWNLEVEL));
		carrierLoc.setExtraOption(Integer.valueOf(rs.getString(EXTRAOPTION)));
		
		if (hoistPosition == INVALID_TEACHING || 
				shiftPosition == INVALID_TEACHING || ratatePosition == INVALID_TEACHING) {
			carrierLoc.setValid(false);
		} else {
			carrierLoc.setValid(true);
		}
	}	
	
	/**
	 * 2013.10.22 by KYK
	 * @return
	 */
	private boolean hasRevisionChanged() {
		if (dataRevision != ocsInfoManager.getTeachingDataRevision()) {
			return true;
		}
		return false;
	}
	
	private void setDataRevision(int dataRevision) {
		this.dataRevision = dataRevision;
	}
	
	public void setIBSEM(boolean isIBSEM) {
		this.isIBSEM = isIBSEM;
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
