package com.samsung.ocs.failover.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.util.StringUtil;
import com.samsung.ocs.failover.model.OcsDBStateVO;

/**
 * OcsDBHistoryDAO Class, OCS 3.0 for Unified FAB
 * 
 * ocs_cluster_state 테이블에 대한 처리를 수행하는 DAO 클래스
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

public class OcsDBStateDAO {
	private static final String INSERVICE_HOST = "inservice_host";
	private static final String PRIMARY_HOST = "primary_state";
	private static final String SECONDARY_HOST = "secondary_state";
	private static final String FAILOVER_ACCEPTANCE = "failover_acceptance";
	private static final String PRIMARY_UPDATION_TIME = "primary_updation_time";
	private static final String SECONDARY_UPDATION_TIME = "secondary_updation_time";
	private static final String CT = "CT";
	private static final String USER_REQUEST = "user_request";
	
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	private static final String RETRIEVE_PROCESS_STATE_SQL = "SELECT inservice_host, primary_state, secondary_state, failover_acceptance,"
				+" primary_updation_time, secondary_updation_time, SYSDATE CT, user_request FROM ocs_cluster_state where process_name = ? and rownum=1";
	
	/**
	 * ocs_cluster_state 테이블에서 processName을 키로하여 데이터를 조회하여 OcsDBStateVO객체에 받아오는 메서드
	 * @param processName : 조회할 Process명
	 * @param dbAccessManager : 접속할 Database의 Connection을 가지는 DBAccessManager 객체.
	 * @return OcsDBStateVO 객체
	 */
	public static OcsDBStateVO retrieveProcessState(String processName, DBAccessManager dbAccessManager) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		OcsDBStateVO result = null;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(RETRIEVE_PROCESS_STATE_SQL);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, processName);
			
			rs = pstmt.executeQuery();
			
			result = new OcsDBStateVO();
			while (rs.next()) {
				result.setInserviceHost(rs.getString(INSERVICE_HOST));
				result.setPrimaryState(rs.getString(PRIMARY_HOST));
				result.setSecondaryState(rs.getString(SECONDARY_HOST));
//				result.setFailoverAcceptance(OcsConstant.TRUE.equalsIgnoreCase(rs.getString("failover_acceptance")));
				result.setFailoverAcceptance(rs.getString(FAILOVER_ACCEPTANCE));
				result.setPrimaryUpdationTime(StringUtil.getDate(rs, PRIMARY_UPDATION_TIME));
				result.setSecondaryUpdationTime(StringUtil.getDate(rs, SECONDARY_UPDATION_TIME));
//				result.setLastUpdationTime(StringUtil.getDate(rs, "last_updation_time"));
				result.setCurrentTime(StringUtil.getDate(rs, CT));
				result.setUserRequest(rs.getString(USER_REQUEST));
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
	
	private static final String UPDATE_USER_REQUEST_SQL = "UPDATE ocs_cluster_state SET user_request = ? WHERE process_name = ?";
	
	/**
	 * ocs_cluster_state 테이블에 processName을 키로하여 user_request column 데이터를 String userRequest 업데이트 하는 메서드
	 * @param dbAccessManager : 접속할 Database의 Connection을 가지는 DBAccessManager 객체.
	 * @param userRequest : 갱신할 userRequest 데이터
	 * @param processName : 갱신할 Process명
	 * @return boolean update 성공여부
	 */
	public static boolean updateUserRequest(DBAccessManager dbAccessManager, String userRequest, String processName) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_USER_REQUEST_SQL);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, userRequest );
			pstmt.setString(2, processName );
			pstmt.execute();
			result = true;
		} catch (Exception ignore) {
			log(ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}

	private static final String UPDATE_FAILOVER_ACCEPTANCE_SQL = "UPDATE ocs_cluster_state SET failover_acceptance=? WHERE process_name = ? ";

	/**
	 * ocs_cluster_state 테이블에 processName을 키로하여 failover_acceptace column 데이터를 String failoverAcceptance로 업데이트 하는 메서드
	 * @param dbAccessManager : 접속할 Database의 Connection을 가지는 DBAccessManager 객체.
	 * @param failoverAcceptance : 갱신할 failover_acceptance 데이터
	 * @param processName : 갱신할 Process명
	 * @return boolean update 성공여부
	 */
	public static boolean updateFailoverAcceptance(DBAccessManager dbAccessManager, String failoverAcceptance, String processName) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_FAILOVER_ACCEPTANCE_SQL);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, failoverAcceptance );
			pstmt.setString(2, processName );
			pstmt.execute();
			result = true;
		} catch (Exception ignore) {
			log(ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
	// Primary 상태 업데이트
	private static String UPDATE_PRIMARY_INSERVICE_HOST_SQL = "UPDATE ocs_cluster_state SET inservice_host='Primary', secondary_state='Unknown', failover_acceptance=? WHERE process_name = ? ";
	private static String UPDATE_SECONDARY_INSERVICE_HOST = "UPDATE ocs_cluster_state SET inservice_host='Secondary', primary_state='Unknown', failover_acceptance=?  WHERE process_name = ? ";
	
	/**
	 * ocs_cluster_state 테이블에 processName을 키로하여 inservice_host column 데이터를  업데이트 하고, filover_acceptance column 데이터를  String failoverAcceptance로 갱신하는 메서드.
	 * 이때 REMOTE_HOST의 상태는 'Unknown'으로 갱신하며, 해당 HOST의 updation_time도 함께 갱신하여 준다.
	 * @param dbAccessManager : 접속할 Database의 Connection을 가지는 DBAccessManager 객체.
	 * @param isPrimary : 업데이트를 해당할 호스트가 Primary 인지 Secondary인지 여부.
	 * @param failoverAcceptance : 갱신할 failover_acceptance 데이터.
	 * @param processName : 갱신할 Process명
	 * @return boolean update 성공여부
	 */
	public static boolean updateInserviceHost(DBAccessManager dbAccessManager, boolean isPrimary, String failoverAcceptance, String processName) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (isPrimary) {
				pstmt = conn.prepareStatement(UPDATE_PRIMARY_INSERVICE_HOST_SQL);
			} else {
				pstmt = conn.prepareStatement(UPDATE_SECONDARY_INSERVICE_HOST);
			}
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, failoverAcceptance );
			pstmt.setString(2, processName );
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
	
	// Primary 상태 업데이트
	private static String UPDATE_PRIMARY_PROCESS_UNKNOWN_SQL = "UPDATE ocs_cluster_state SET primary_state='Unknown' WHERE process_name = ? ";
	private static String UPDATE_SECONDARY_PROCESS_UNKNOWN_SQL = "UPDATE ocs_cluster_state SET secondary_state='Unknown' WHERE process_name = ? ";
	
	/**
	 * ocs_cluster_state 테이블에 processName을 키로하여 해당 HOST의 state column 데이터를 'Unknown'로 갱신하는 메서드.
	 * @param dbAccessManager : 접속할 Database의 Connection을 가지는 DBAccessManager 객체.
	 * @param isPrimary : 업데이트를 해당할 호스트가 Primary 인지 Secondary인지 여부.
	 * @param processName : 갱신할 Process명
	 * @return boolean update 성공여부
	 */
	public static boolean updateProcessUnknown(DBAccessManager dbAccessManager, boolean isPrimary, String processName) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (isPrimary) {
				pstmt = conn.prepareStatement(UPDATE_PRIMARY_PROCESS_UNKNOWN_SQL);
			} else {
				pstmt = conn.prepareStatement(UPDATE_SECONDARY_PROCESS_UNKNOWN_SQL);
			}
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, processName );
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
	
	/*create table ocs_cluster_info (
	host			varchar2(100),
	info_item 		varchar2(64),
	state			varchar2(32) default 'DEAD',
	updation_time	timestamp default SYSDATE
);*/
	
/*	
	
	private static String updatePrimaryForIBSEMSql = "UPDATE ocs_cluster_state SET inservice_host='Primary', primary_state='InService', secondary_state='OutOfService' WHERE process_name = 'ibsem'";
	private static String updateSecondaryForIBSEMSql = "UPDATE ocs_cluster_state SET inservice_host='Secondary', primary_state='OutOfService', secondary_state='InService' WHERE process_name = 'ibsem'";
	
	public static boolean updateIBSEM(DBAccessManager dbAccessManager, boolean isPrimary) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if(isPrimary) {
				pstmt = conn.prepareStatement(updatePrimaryForIBSEMSql);
			} else {
				pstmt = conn.prepareStatement(updateSecondaryForIBSEMSql);
			}
			pstmt.execute();
			result = true;
//			conn.commit();
		} catch(Exception ignore) {
			log(ignore);
//			try {conn.rollback();} catch(Exception ignore1) {}
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if(pstmt!=null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	*/
}
