package com.samsung.ocs.unitdevice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.FFU;

public class FFUManager {
	private static FFUManager manager = null;
	private ConcurrentHashMap<String, FFU> data = new ConcurrentHashMap<String, FFU>();
	private Vector<String> disabledNode = new Vector<String>();
	private Vector<FFU> updateStatusFFU = new Vector<FFU>();
	private Vector<String> updateCommfailFFU = new Vector<String>();
	
	private static String FFUGROUPID = "FFUGROUPID";
	private static String STATUS = "STATUS";
	private static String TOTAL_FFU = "TOTAL_FFU";
	private static String ABNORMAL_FFU = "ABNORMAL_FFU";
	private static String COMMFAIL_FFU = "COMMFAIL_FFU";
	private static String NODEID = "NODEID";
	private static String IPADDRESS = "IPADDRESS";
	private static String ENABLED = "ENABLED";
	private static String ERRORCODE = "ERRORCODE";
	
	private static String NAME = "NAME";
	private static String VALUE = "VALUE";
	private static String FFUSERVER_COMMFAIL_CONTROL_USAGE = "FFUSERVER_COMMFAIL_CONTROL_USAGE";
	private static String FFU_MONITORING_CONTROL_USAGE = "FFU_MONITORING_CONTROL_USAGE";
	private static String FFUGROUP_REPORT_CHECKTIME = "FFUGROUP_REPORT_CHECKTIME";
	
	private boolean isFFUServerCommfailControlUsed = false;
	private boolean isFFUMonitoringControlUsed = false;
	private long lFFUGroupReportChecktime = 10000;
	
	private static final String FFUMANAGER_TRACE = "FFUManagerDebug";
	private static Logger ffuManagerTraceLog = Logger.getLogger(FFUMANAGER_TRACE);
	
	DBAccessManager dbAccessManager = null;

	private FFUManager() {
		this.dbAccessManager = new DBAccessManager();
		for (int i = 0; i < 5; i++) {
			if (dbAccessManager.IsDBConnected() == true)
				break;
			dbAccessManager.ReconnectToDB();
		}
		
		initilize();
	}
	
	synchronized static public FFUManager getInstance() {
		if(manager == null) {
			manager = new FFUManager();
		}
		
		return manager;
	}
	
	private String MakeString(String strValue) {
		if (strValue == null)
			return "";
		else
			return strValue;
	}
	
	private void initilize() {
		updateFFUFromDB();
		updateFFUParamFromDB();
	}
	
	public boolean isFFUServerCommfailControlUsed() {
		return isFFUServerCommfailControlUsed;
	}
	
	public boolean isFFUMonitoringControlUsed() {
		return isFFUMonitoringControlUsed;
	}
	
	public long getFFUGroupReportChecktime() {
		return lFFUGroupReportChecktime;
	}
	
	synchronized public boolean update() {
		updateFFUFromDB();
		updateDisableNodeFromDB();
		updateFFUToDB();
		updateFFUParamFromDB();
		return true;
	}

	private static String SELECT_FFU_SQL = "SELECT * FROM FFU ORDER BY FFUGROUPID";

	private void updateFFUFromDB() {
		// 1. DB의 FFU 최신 정보를 Manager에 업데이트 
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_FFU_SQL);
			if (rs != null) {
				Vector<String> removeFFU = new Vector<String>(data.keySet()); 
				while (rs.next()) {
					String ffuGroupId = rs.getString(FFUGROUPID);
					FFU ffu = data.get(ffuGroupId);
					if (ffu == null) {
						ffu = new FFU(ffuGroupId);
						ffu.setIpAddress(MakeString(rs.getString(IPADDRESS)));
						ffu.setStatus(MakeString(rs.getString(STATUS)));
						ffu.setNodeId(MakeString(rs.getString(NODEID)));
						ffu.setErrorCode(MakeString(rs.getString(ERRORCODE)));
						data.put(ffuGroupId, ffu);
					}
					ffu.setEnabled(MakeString(rs.getString(ENABLED)));
					removeFFU.remove(ffuGroupId);
				}
				
				for (String removeKey : removeFFU) {
					data.remove(removeKey);
				}
			}
		} catch (SQLException e) {
			ffuManagerTrace("Exception updateFFUFromDB", e);
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}
	}
	
	private static String SELECT_DISABLENODE_SQL = "SELECT NODEID, ENABLED FROM NODE WHERE ENABLED='FALSE' ORDER BY NODEID";

	private void updateDisableNodeFromDB() {
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_DISABLENODE_SQL);
			if (rs != null) {
				Vector<String> removeDisabledNode = (Vector<String>) disabledNode.clone(); 
				while (rs.next()) {
					String nodeId = rs.getString(NODEID);
					if (!disabledNode.contains(nodeId)){
						disabledNode.add(nodeId);
					}
					removeDisabledNode.remove(nodeId);
				}
				
				for (String removeKey : removeDisabledNode) {
					disabledNode.remove(removeKey);
				}
			}
		} catch (SQLException e) {
			ffuManagerTrace("Exception updateDisableNodeFromDB", e);
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}
	}
	
	private boolean updateFFUToDB() {
		// 16.09.06 LSH: FFU_MONITORING_CONTROL_USAGE가 YES일 때만, Commfail 처리하도록 수정
		if (isFFUMonitoringControlUsed){
			updateCommfailToDB();
		}
		updateStatusToDB();
		return true;
	}
	
	public boolean addStatusToUpdateList(FFU ffu) {
		synchronized (updateStatusFFU) {
			if (updateStatusFFU.contains(ffu) == false) {
				return updateStatusFFU.add(ffu);
			}
		}
		return false;
	}
	
	private static String UPDATE_STATUS_SQL = "UPDATE FFU SET STATUS=?,TOTAL_FFU=?,ABNORMAL_FFU=?,COMMFAIL_FFU=?,ERRORCODE=? WHERE FFUGROUPID=?";

	private boolean updateStatusToDB() {
		if (updateStatusFFU.size() == 0) {
			return false;
		}
		
		boolean result = true;
		PreparedStatement pstmt = null;
		Connection conn = null;
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_STATUS_SQL);
			
			Vector<FFU> updateClone = (Vector<FFU>) updateStatusFFU.clone();
			ListIterator<FFU> iterator = updateClone.listIterator();
			FFU ffu = null;
			while (iterator.hasNext()) {
				ffu = iterator.next();
				if (ffu != null) {
					// STATUS=?,TOTAL_FFU=?,ABNORMAL_FFU=? WHERE FFUGROUPID=?
					pstmt.setString(1, ffu.getStatus());
					pstmt.setInt(2, ffu.getTotalFFU());
					pstmt.setInt(3, ffu.getAbnormalFFU());
					pstmt.setInt(4, ffu.getCommfailFFU());
					pstmt.setInt(5, ffu.getErrorCode());
					pstmt.setString(6, ffu.getFFUGroupId());
					pstmt.addBatch();
					updateStatusFFU.remove(ffu);
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			// 2012.11.07 by MYM : Exception인 경우 업데이트 요청 모두 삭제
			updateStatusFFU.clear();
			ffuManagerTrace("Exception updateStatusToDB", e);
			e.printStackTrace();
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
				pstmt = null;
			}
		}
		return result;
	}

	public boolean addCommfailToUpdateList(String ipaddress) {
		synchronized (updateCommfailFFU) {
			if (updateCommfailFFU.contains(ipaddress) == false) {
				return updateCommfailFFU.add(ipaddress);
			}
		}
		return false;
	}
	
	private static String UPDATE_COMMFAIL_SQL = "UPDATE FFU SET ERRORCODE=? WHERE IPADDRESS=?";
	private static String ERRORCODE_FFUSERVER_COMFAIL = "1000";

	private boolean updateCommfailToDB() {
		if (updateCommfailFFU.size() == 0) {
			return false;
		}
		
		boolean result = true;
		PreparedStatement pstmt = null;
		Connection conn = null;
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_COMMFAIL_SQL);
			
			Vector<String> updateClone = (Vector<String>) updateCommfailFFU.clone();
			ListIterator<String> iterator = updateClone.listIterator();
			String ipaddress = null;
			while (iterator.hasNext()) {
				ipaddress = iterator.next();
				if (ipaddress != null) {
					pstmt.setString(1, ERRORCODE_FFUSERVER_COMFAIL);
					pstmt.setString(2, ipaddress);
					pstmt.addBatch();
					updateCommfailFFU.remove(ipaddress);
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			ffuManagerTrace("Exception updateCommfailToDB", e);
			e.printStackTrace();
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
				pstmt = null;
			}
		}
		return result;
	}
	
	public boolean setNodeEnable(String nodeId, boolean enable) {
		Connection conn = null;
		boolean result = false;
		String userId = "FFU";
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			String databaseTime = getDatabaseTime(conn);

			if (enable) {
				if (disabledNode.contains(nodeId)){
					setNodeEnabledTrue(conn, databaseTime, userId, nodeId);
				}
			} else {
				if (!disabledNode.contains(nodeId)){
					setNodeEnabledFalse(conn, databaseTime, userId, nodeId);
				}
			}
			conn.commit();
			conn.setAutoCommit(true);
			result = true;
		} catch (Exception e) {
			try { if(conn!=null) conn.rollback(); } catch (SQLException ignore) {}
			try { if(conn!=null) conn.setAutoCommit(true); } catch (SQLException ignore) {}
			e.printStackTrace();
		} finally {
			// conn close 하면 안됨
//			if (conn != null) {
//				try { conn.close(); } catch (Exception e) { }
//			}
		}
		return result;
	}
	
	// 16.04.26 LSH: FFU로 막은 것만 Enable 하도록 수정 (사용자가 막은 Node는 FFU 상태변화로 Enable 시키지 않음)
//	private static final String SET_NODE_ENALBED_TRUE = "UPDATE NODE SET ENABLED='TRUE',STATUSCHANGEDTIME=?, LASTOPENEDTIME=?,LASTOPENUSER=?,COMMENTS='' WHERE NODEID=?";
	private static final String SET_NODE_ENALBED_TRUE = "UPDATE NODE SET ENABLED='TRUE',STATUSCHANGEDTIME=?, LASTOPENEDTIME=?,LASTOPENUSER=?,COMMENTS='' WHERE NODEID=? AND LASTCLOSEUSER='FFU' AND ENABLED='FALSE'";

	private boolean setNodeEnabledTrue(Connection conn, String databaseTime, String userId, String nodeId) throws Exception {
		PreparedStatement pstmt = null;
		boolean result = false;

		try {
			pstmt = conn.prepareStatement(SET_NODE_ENALBED_TRUE);
			pstmt.setString(1, databaseTime);
			pstmt.setString(2, databaseTime);
			pstmt.setString(3, userId);
			pstmt.setString(4, nodeId);
			pstmt.execute();
			result = true;
		} catch (Exception e) {
			result = false;
			throw e;
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception e) { }
			}
		}
		return result;
	}

	private static final String SET_NODE_ENALBED_FALSE = "UPDATE NODE SET ENABLED='FALSE',STATUSCHANGEDTIME=?,LASTCLOSEDTIME=?,LASTCLOSEUSER=?,COMMENTS=? WHERE NODEID=? AND ENABLED='TRUE'";

	private boolean setNodeEnabledFalse(Connection conn, String databaseTime, String userId, String nodeId) throws Exception {
		PreparedStatement pstmt = null;
		boolean result = false;
		String reason = "FFU is abnormal status now.";

		try {
			pstmt = conn.prepareStatement(SET_NODE_ENALBED_FALSE);
			pstmt.setString(1, databaseTime);
			pstmt.setString(2, databaseTime);
			pstmt.setString(3, userId);
			pstmt.setString(4, reason);
			pstmt.setString(5, nodeId);
			pstmt.execute();
			result = true;
		} catch (Exception e) {
			result = false;
			throw e;
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception e) { }
			}
		}
		return result;
	}
	
	private static String SELECT_PARAM_SQL = "SELECT NAME, VALUE FROM OCSINFO WHERE NAME='FFUSERVER_COMMFAIL_CONTROL_USAGE'"
			+ " OR NAME='FFU_MONITORING_CONTROL_USAGE'" + " OR NAME='FFUGROUP_REPORT_CHECKTIME'";
	private boolean updateFFUParamFromDB() {
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_PARAM_SQL);
			if (rs != null) {
				while (rs.next()) {
					String name = rs.getString(NAME);
					if (FFUSERVER_COMMFAIL_CONTROL_USAGE.equals(name)) {
						String value = rs.getString(VALUE);
						if (value != null && "YES".equals(value)) {
							isFFUServerCommfailControlUsed = true;
						} else {
							isFFUServerCommfailControlUsed = false;
						}
					} else if (FFU_MONITORING_CONTROL_USAGE.equals(name)) {
						String value = rs.getString(VALUE);
						if (value != null && "YES".equals(value)) {
							isFFUMonitoringControlUsed = true;
						} else {
							isFFUMonitoringControlUsed = false;
						}
					} else if (FFUGROUP_REPORT_CHECKTIME.equals(name)) {
						long value = rs.getLong(VALUE);
						lFFUGroupReportChecktime = (int) value * 1000;
						if (lFFUGroupReportChecktime < 10000) lFFUGroupReportChecktime = 10000;
						else if (lFFUGroupReportChecktime > 60000) lFFUGroupReportChecktime = 60000;
					}
				}
			}
		} catch (Exception e) {
			ffuManagerTrace("Exception updateHidParamFromDB", e);
			return false;
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}

		return true;
	}
	
	
	private static String UPDATE_ERRORCODE_CLEAR_SQL = "UPDATE FFU SET ERRORCODE='0'";

	// 16.09.06 LSH: FFU_MONITORING_CONTROL_USAGE가 NO로 변경 될 때, ErrorCode 정리
	public boolean updateErrorCodeClearToDB() {

		boolean result = true;
		PreparedStatement pstmt = null;
		Connection conn = null;
		
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_ERRORCODE_CLEAR_SQL);
			pstmt.execute();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			ffuManagerTrace("Exception updateErrorCodeClearToDB", e);
			e.printStackTrace();
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
				pstmt = null;
			}
		}
		return result;
	}
	
	
	private static final String GET_DATABASE_TIME_QUERY = "SELECT TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') T FROM DUAL";

	public synchronized String getDatabaseTime(Connection conn) throws Exception {
		Statement stmt = null;
		ResultSet rs = null;
		String result = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(GET_DATABASE_TIME_QUERY);
			while (rs.next()) {
				result = rs.getString("T");
				break;
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (rs != null) { try { rs.close(); } catch (Exception e) { } }
			if (stmt != null) { try { stmt.close(); } catch (Exception e) { } }
		}
		return result;
	}
	
	public ConcurrentHashMap<String, FFU> getData() {
		return data;
	}
	
	public void ffuManagerTrace(String message, Throwable e) {
		ffuManagerTraceLog.error(message, e);
	}
}
