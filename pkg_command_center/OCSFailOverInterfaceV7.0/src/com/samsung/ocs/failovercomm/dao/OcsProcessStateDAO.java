package com.samsung.ocs.failovercomm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;

/**
 * OcsProcessStateDAO Class, OCS 3.0 for Unified FAB
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

public class OcsProcessStateDAO {
	
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTEREXECUTER);	
	
	private static String RETRIEVE_STATE_SQL = "SELECT inservice_host, primary_state, secondary_state FROM ocs_cluster_state where process_name = ? and rownum=1";
	
	/**
	 * 
	 * @param processName
	 * @param dbAccessManager
	 * @param isPrimary
	 * @return
	 */
	public static OcsProcessStateVO retrieveProcessState(String processName, DBAccessManager dbAccessManager, boolean isPrimary) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		OcsProcessStateVO result = null;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(RETRIEVE_STATE_SQL);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, processName);
			
			rs = pstmt.executeQuery();
			
			result = new OcsProcessStateVO();
			while (rs.next()) {
				result.setInserviceHost(rs.getString("inservice_host"));
				if (isPrimary) {
					result.setHostState(MODULE_STATE.toModuleState(rs.getString("primary_state")));
					result.setRemoteState(MODULE_STATE.toModuleState(rs.getString("secondary_state")));
				} else {
					result.setHostState(MODULE_STATE.toModuleState(rs.getString("secondary_state")));
					result.setRemoteState(MODULE_STATE.toModuleState(rs.getString("primary_state")));
				}
				break;
			}
		} catch (Exception ignore) {
			result = null;
			dbAccessManager.requestDBReconnect();
			log(ignore);
		} finally {
			if (rs != null) {
				try { rs.close(); } catch (Exception ignore) {}
			}
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore) {}
			}
		}
		return result;
	}
	
	
	// Process 상태 업데이트
	private static String UPDATE_PRIMARY_STATE_SQL = "UPDATE ocs_cluster_state SET primary_state=?, primary_updation_time=sysdate WHERE process_name = ? ";
	private static String UPDATE_SECONDARY_STATE_SQL = "UPDATE ocs_cluster_state SET secondary_state=?, secondary_updation_time=sysdate WHERE process_name = ? ";
	/**
	 * 
	 * @param dbAccessManager
	 * @param isPrimary
	 * @param processState
	 * @param processName
	 * @return
	 */
	public static boolean updateProcessState(DBAccessManager dbAccessManager, boolean isPrimary, MODULE_STATE processState, String processName ) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (isPrimary) {
				pstmt = conn.prepareStatement(UPDATE_PRIMARY_STATE_SQL);
			} else {
				pstmt = conn.prepareStatement(UPDATE_SECONDARY_STATE_SQL);
			}
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, processState.toConstString() );
			pstmt.setString(2, processName.toLowerCase() );
			pstmt.execute();
			result = true;
//			conn.commit();
		} catch (Exception ignore) {
			log(ignore);
//			try {conn.rollback();} catch(Exception ignore1) {}
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
//	private static void log(String log) {
//		logger.debug(log);
//	}
	private static void log(Throwable w) {
		logger.debug(w.getMessage(), w);
	}
}
