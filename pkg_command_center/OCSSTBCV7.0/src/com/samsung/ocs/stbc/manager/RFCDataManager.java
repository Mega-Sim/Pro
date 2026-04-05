package com.samsung.ocs.stbc.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.manager.impl.AbstractManager;
import com.samsung.ocs.stbc.rfc.config.RfcConfig;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.RFC_COND;
import com.samsung.ocs.stbc.rfc.model.RFCData;
import com.samsung.ocs.stbc.rfc.model.STBData;

/**
 * RFCDataManager Class, OCS 3.0 for Unified FAB
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

public class RFCDataManager extends AbstractManager {
	private Map<String, RFCData> rfcMap;
	private Map<String, STBData> stbMap; 
	private Vector<RFCData> updateRfcMetaDataList = new Vector<RFCData>();
	private Vector<RFCData> updateRfcStatusDataList = new Vector<RFCData>();
	private Vector<RFCData> updateRfcConditionList = new Vector<RFCData>();
	private Vector<STBData> updateStbStatusDataList = new Vector<STBData>();
	private Vector<STBData> updateStbResultDataList = new Vector<STBData>();
	
	private static Logger rfcLogger = Logger.getLogger(RfcConstant.RFCMANAGERLOGGER);
	
	private RfcConfig config;
	
	private static final String RFCID = "RFCID";
	private static final String IPADDRESS = "IPADDRESS";
	private static final String MACHINECODE = "MACHINECODE";
	private static final String MACHINEID = "MACHINEID";
	private static final String CONDITION = "CONDITION";
	private static final String ENABLED = "ENABLED";
	private static final String READY = "READY";
	private static final String ERRORCODE = "ERRORCODE";
	private static final String ERROR = "ERROR";
	
	private static final String CARRIERLOCID = "CARRIERLOCID";
	private static final String RFCINDEX = "RFCINDEX";
	private static final String CARRIERSENSOR = "CARRIERSENSOR";
	private static final String STBHOMESENSOR = "STBHOMESENSOR";
	private static final String ECAT1CONN = "ECAT1CONN";
	private static final String ECAT2CONN = "ECAT2CONN";
	private static final String CARRIERDETECT = "CARRIERDETECT";
	private static final String READRESULT = "READRESULT";
	private static final String VERIFYRESULT = "VERIFYRESULT";
	private static final String IDDATA = "IDDATA";

	/**
	 * Constructor of RFCDataManager class.
	 * 
	 * @param dbAccessManager
	 * @param vOType
	 * @param initializeAtStart
	 * @param makeManagerThread
	 * @param interval
	 * @exception Exception 
	 */
	public RFCDataManager(DBAccessManager dbAccessManager, Class<?> vOType,
			boolean initializeAtStart, boolean makeManagerThread, long interval, Map<String, RFCData> rfcMap, Map<String, STBData> stbMap, RfcConfig config) throws Exception {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		this.rfcMap = rfcMap;
		this.stbMap = stbMap;
		this.config = config;
		if (vOType.getClass().isInstance(RFCData.class)) {
			LOGFILENAME = this.getClass().getName();
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			throw new Exception("Object Type Not Supported");
		}
		initRfcDbData();
		initRfcMap();
		initStbMap();
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	/**
	 * Init
	 */
	@Override
	protected void init() {
//		updateFromDB();
//		initRFCData();
		
//		RFCData rfc = new RFCData();
//		rfc.setRfcId("RF001");
//		rfc.addSTB("TEST01_B0", "1");
//		rfc.addSTB("TEST01_B1", "2");
//		rfc.addSTB("TEST02_B0", "3");
//		rfc.addSTB("TEST02_B1", "4");
//		
//		data.put("RF001", rfc);
//		
		isInitialized = true;
		
	}
//	CREATE TABLE STBRFCDATA (
//			RFCID varchar2(15),
//			MACHINECODE varchar2(10),
//			MACHINEID varchar2(10),
//			CONDITION varchar2(10),
//			IPADDRESS varchar2(20),
//			ENABLED varchar2(10),
//			READY varchar2(10),
//			ERRORCODE varchar2(10),
//			ERROR varchar2(10),
//			UPDATE_TIME TIMESTAMP
//		);

	
	public static String initRfcDbDataSql = "update STBRFCDATA set CONDITION=?, READY=0";
	
	private void initRfcDbData() {

		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(initRfcDbDataSql);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1,RFC_COND.OFFLINE.toConditionString());
			pstmt.execute();
			conn.commit();
		} catch (Exception ignore) {
			try { conn.rollback(); } catch (Exception ii) {}
			writeExceptionLog(LOGFILENAME, ignore);
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("initRfcDbData elapsedTime[%d]millis", elapsedTime));
	}
	
	
	public static String initRfcMapSql = "select RFCID, MACHINECODE, MACHINEID, IPADDRESS, ENABLED, CONDITION, READY, ERRORCODE, ERROR from STBRFCDATA";
	
	/**
	 * Init RFC Map
	 */
	private void initRfcMap() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(initRfcMapSql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String rfcId = rs.getString(RFCID);
				RFCData data = new RFCData(rfcId);
				
				String ipAddress = rs.getString(IPADDRESS);
				data.setMachineCode(rs.getString(MACHINECODE));
				data.setMachineId(rs.getString(MACHINEID));
				data.setCondition(RFC_COND.toRfcCondition(rs.getString(CONDITION)));
				data.setIpAddress(ipAddress);
				data.setEnabled(OcsConstant.TRUE.equalsIgnoreCase(rs.getString(ENABLED)));
				data.setReady(rs.getInt(READY));
				data.setErrorCode(rs.getString(ERRORCODE));
				data.setError(rs.getInt(ERROR));
				
				UDPSender sender = null;
				try {
					sender = new UDPSender(ipAddress, config.getNormalCommPort());
				} catch (Exception ignore) {}
				data.setSender(sender);
				
				rfcMap.put(rfcId, data);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try { rs.close(); } catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) { 
				try { pstmt.close(); } catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
	}
	
//	CREATE TABLE STBCARRIERLOC (
//			CARRIERLOCID varchar2(64),
//			CARRIERID varchar2(64),
//			CARRIERSTATE varchar2(16),
//			IDREADER varchar2(20),
//			COMMANDNAME varchar2(32),
//			MCSCARRIERID varchar2(64),
//			OCSCARRIERID varchar2(64),
//			INSTALLTIME varchar2(16),
//			REMOVETIME varchar2(16),
//			ENABLED varchar2(6),
//			
//			RFCID varchar2(15),
//			RFCINDEX varchar2(10),	
//			READY varchar2(10),
//			CARRIERSENSOR varchar2(10),
//			STBHOMESENSOR varchar2(10),
//			ECAT1CONN	varchar2(10),
//			ECAT2CONN	varchar2(10),
//			CARRIERDETECT varchar2(10),
//			READRESULT varchar2(10),
//			VERIFYRESULT varchar2(10),
//			IDDATA varchar2(64),
//			STBENABLED varchar2(10)
//		);
	
	public static String initStbMapSql = "select RFCID, CARRIERLOCID, RFCINDEX,READY,CARRIERSENSOR,STBHOMESENSOR ,ECAT1CONN,ECAT2CONN,CARRIERDETECT,READRESULT,VERIFYRESULT,IDDATA from STBCARRIERLOC";
	
	/**
	 * Init STB Map
	 */
	private void initStbMap() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(initStbMapSql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String rfcId = rs.getString(RFCID);
				
				RFCData rfcData = rfcMap.get(rfcId);
				String carrierlocId = rs.getString(CARRIERLOCID);
				if (rfcData != null) {
					STBData data = new STBData(rfcData);
					
					data.setCarrierLocId(carrierlocId);
					data.setRfcIndex(rs.getInt(RFCINDEX));
					data.setReady(rs.getInt(READY));
					data.setCarrierSensor(rs.getInt(CARRIERSENSOR));
					data.setStbHomeSensor(rs.getInt(STBHOMESENSOR));
					data.setECAT1Conn(rs.getInt(ECAT1CONN));
					data.setECAT2Conn(rs.getInt(ECAT2CONN));
					data.setCarrierDetect(rs.getInt(CARRIERDETECT));
					data.setReadResult(rs.getInt(READRESULT));
					data.setVerifyResult(rs.getInt(VERIFYRESULT));
					data.setIdData(rs.getString(IDDATA));
//					data.setEnabled(OcsConstant.TRUE.equalsIgnoreCase(rs.getString("STBENABLED")));
					
					stbMap.put(carrierlocId, data);
					rfcData.addSTB(data);
					
				} else {
					log(String.format("CarrierLog[%s] has no RFC[%s]. ", carrierlocId, rfcId));
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try { rs.close(); } catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) { 
				try { pstmt.close(); } catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
	}
	
	/**
	CREATE TABLE STBCARRIERLOC (
	RFCID varchar2(15),
	RFCINDEX varchar2(10),
	CARRIERLOCID varchar2(64)
);
	 */
	// 서비스 스테이트가 바뀐걸 select 하는걸로 하자..
	
	@Override
	public boolean updateFromDB() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	/**
	 * Update to DB
	 */
	@Override
	protected boolean updateToDB() {
		updateRfcMetaData();      // MACHINECODE, MACHINEID, IPADDRESS
		updateRfcStatusData();    // READY, ERRORCODE, ERROR
		updateRfcCondition();     // CONDITION
		updateStbStatusData();    // READY,CARRIERSENSOR,STBHOMESENSOR,ECAT1CONN,ECAT2CONN,CARRIERDETECT
		updateStbResultData();    // CARRIERDETECT,READRESULT,VERIFYRESULT,IDDATA,FOUPIDDATA
		return true;
	}
	
	private static String updateRfcMetaDataSql = "UPDATE STBRFCDATA SET MACHINECODE=?, MACHINEID=?, IPADDRESS=? WHERE RFCID = ?";
	
	/**
	 * Update RFC Meta Data
	 * @return
	 */
	private boolean updateRfcMetaData() {
		if (updateRfcMetaDataList.size() == 0) {
			return true;
		}
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int index = 0;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(updateRfcMetaDataSql);
			pstmt.setQueryTimeout(3);
			
			for (int i = 0; i < updateRfcMetaDataList.size(); i++) {
				RFCData rfc = updateRfcMetaDataList.get(i);
				pstmt.setString(1, rfc.getMachineCode());
				pstmt.setString(2, rfc.getMachineId());
				pstmt.setString(3, rfc.getIpAddress());
				pstmt.setString(4, rfc.getRfcId());
				pstmt.addBatch();
				index++;
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception ignore) {
			try { conn.rollback(); } catch (Exception ii) {}
			writeExceptionLog(LOGFILENAME, ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		if (result == true) {
			for (int i = 0; i < index; i++) {
				updateRfcMetaDataList.remove(0);
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("updateRfcMetaData UPDATE-CNT[%d] elapsedTime[%d]millis", index, elapsedTime));
		return result;
	}
	
	private static String updateRfcStatusDataSql = "UPDATE STBRFCDATA SET READY=?, ERRORCODE=?, ERROR=? WHERE RFCID = ?";
	
	/**
	 * Update RFC Status Data
	 * @return
	 */
	private boolean updateRfcStatusData() {
		if (updateRfcStatusDataList.size() == 0) {
			return true;
		}
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int index = 0;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(updateRfcStatusDataSql);
			pstmt.setQueryTimeout(3);
			
			for (int i = 0; i < updateRfcStatusDataList.size(); i++) {
				RFCData rfc = updateRfcStatusDataList.get(i);
				pstmt.setInt(1, rfc.getReady());
				pstmt.setString(2, rfc.getErrorCode());
				pstmt.setInt(3, rfc.getError());
				pstmt.setString(4, rfc.getRfcId());
				pstmt.addBatch();
				index++;
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception ignore) {
			try { conn.rollback(); } catch (Exception ii) {}
			writeExceptionLog(LOGFILENAME, ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		if (result == true) {
			for (int i = 0; i < index; i++) {
				updateRfcStatusDataList.remove(0);
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("updateRfcStatusData UPDATE-CNT[%d] elapsedTime[%d]millis", index, elapsedTime));
		return result;
	}
	
	private static String updateStbStatusDataSql = "UPDATE STBCARRIERLOC SET READY=?,CARRIERSENSOR=?,STBHOMESENSOR=? ,ECAT1CONN=?,ECAT2CONN=? WHERE RFCID=? AND RFCINDEX=?";
	
	/**
	 * Update STB Status Data
	 * 
	 * @return
	 */
	private boolean updateStbStatusData() {
		if (updateStbStatusDataList.size() == 0) {
			return true;
		}
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int index = 0;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(updateStbStatusDataSql);
			pstmt.setQueryTimeout(3);
			
			for (int i = 0; i < updateStbStatusDataList.size(); i++) {
				STBData stb = updateStbStatusDataList.get(i);
				pstmt.setInt(1, stb.getReady());
				pstmt.setInt(2, stb.getCarrierSensor());
				pstmt.setInt(3, stb.getStbHomeSensor());
				pstmt.setInt(4, stb.getECAT1Conn());
				pstmt.setInt(5, stb.getECAT2Conn());
				pstmt.setString(6, stb.getOwner().getRfcId());
				pstmt.setInt(7, stb.getRfcIndex());
				pstmt.addBatch();
				index++;
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception ignore) {
			try { conn.rollback(); } catch (Exception ii) {}
			writeExceptionLog(LOGFILENAME, ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		if (result == true) {
			for (int i = 0; i < index; i++) {
				updateStbStatusDataList.remove(0);
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("updateStbStatusData UPDATE-CNT[%d] elapsedTime[%d]millis", index, elapsedTime));
		return result;
	
	}
	
	private static String updateStbResultDataSql = "UPDATE STBCARRIERLOC SET CARRIERDETECT=?, READRESULT=?, VERIFYRESULT=?, IDDATA=?, FOUPIDDATA=? WHERE RFCID=? AND RFCINDEX=?";
	
	/**
	 * Update STB Result Data
	 * 
	 * @return
	 */
	private boolean updateStbResultData() {
		if (updateStbResultDataList.size() == 0) {
			return true;
		}
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int index = 0;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(updateStbResultDataSql);
			pstmt.setQueryTimeout(3);
			
			for (int i = 0; i < updateStbResultDataList.size(); i++) {
				STBData stb = updateStbResultDataList.get(i);
				pstmt.setInt(1, stb.getCarrierDetect());
				pstmt.setInt(2, stb.getReadResult());
				pstmt.setInt(3, stb.getVerifyResult());
				pstmt.setString(4, stb.getIdData());
				pstmt.setString(5, stb.getFoupIdData());
				pstmt.setString(6, stb.getOwner().getRfcId());
				pstmt.setInt(7, stb.getRfcIndex());
				pstmt.addBatch();
				index++;
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception ignore) {
			try { conn.rollback(); } catch (Exception ii) {}
			writeExceptionLog(LOGFILENAME, ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		if (result == true) {
			for (int i = 0; i < index; i++) {
				updateStbResultDataList.remove(0);
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("updateStbResultData UPDATE-CNT[%d] elapsedTime[%d]millis", index, elapsedTime));
		return result;
	}
	
	private static String updateRfcConditionSql = "UPDATE STBRFCDATA SET CONDITION=? WHERE RFCID = ?";
	
	/**
	 * Update RFC Condition
	 * 
	 * @return
	 */
	private boolean updateRfcCondition() {
		if (updateRfcConditionList.size() == 0) {
			return true;
		}
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int index = 0;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(updateRfcConditionSql);
			pstmt.setQueryTimeout(3);
			
			for (int i = 0; i < updateRfcConditionList.size(); i++) {
				RFCData rfc = updateRfcConditionList.get(i);
				pstmt.setString(1, rfc.getCondition().toConditionString());
				pstmt.setString(2, rfc.getRfcId());
				pstmt.addBatch();
				index++;
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception ignore) {
			try { conn.rollback(); } catch (Exception ii) {}
			writeExceptionLog(LOGFILENAME, ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		if (result == true) {
			for (int i =0; i < index; i++) {
				updateRfcConditionList.remove(0);
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("updateRfcCondition UPDATE-CNT[%d] elapsedTime[%d]millis", index, elapsedTime));
		return result;
	}
	
	public void addUpdateRfcMetaDataList(RFCData rfc) {
		updateRfcMetaDataList.add(rfc);
	}

	public void addUpdateRfcStatusDataList(RFCData rfc) {
		updateRfcStatusDataList.add(rfc);
	}
	
	public void addUpdateRfcCondition(RFCData rfc) {
		updateRfcConditionList.add(rfc);
	}

	public void addUpdateStbStatusDataList(STBData stb) {
		updateStbStatusDataList.add(stb);
	}

	public void addUpdateStbResultDataList(STBData stb) {
		updateStbResultDataList.add(stb);
	}

	private void log(String s) {
		rfcLogger.debug(s);
	}
}
