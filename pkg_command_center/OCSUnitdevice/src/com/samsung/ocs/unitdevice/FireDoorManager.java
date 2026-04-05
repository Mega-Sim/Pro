package com.samsung.ocs.unitdevice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.FireDoor;

public class FireDoorManager {
	private static FireDoorManager manager = null;
	private ConcurrentHashMap<String, FireDoor> data = new ConcurrentHashMap<String, FireDoor>();
	private Vector<String> disabledNode = new Vector<String>();
	private Vector<FireDoor> updateStatusFireDoor = new Vector<FireDoor>();
	private Vector<String> updateCommfailFireDoor = new Vector<String>();
	
	private static String FIREDOORID = "FIREDOORID";
	private static String ENABLED = "ENABLED";
	private static String STATUS = "STATUS";
	private static String IPADDRESS = "IPADDRESS";
	private static String NODELIST = "NODELIST";
	private static String ERRORCODE = "ERRORCODE";
	
	private static String NODEID = "NODEID";
	
	private static String NAME = "NAME";
	private static String VALUE = "VALUE";
	
	private static String FIREDOOR_MONITOR_CONTROL_USAGE = "FIREDOOR_MONITOR_CONTROL_USAGE";
	private static String FIREDOOR_COMMFAIL_CONTROL_USAGE = "FIREDOOR_COMMFAIL_CONTROL_USAGE";
	private static String FIREDOOR_REPORT_CHECKTIME = "FIREDOOR_REPORT_CHECKTIME";
	private static String FIREDOOR_ALL_NODE_CONTROL_USAGE = "FIREDOOR_ALL_NODE_CONTROL_USAGE";
	private static String FIREDOOR_SOCKET_RECONNECT_TIME = "FIREDOOR_SOCKET_RECONNECT_TIME";
	
	private boolean isFireDoorCommfailControlUsed = false;
	private boolean isFireDoorMonitoringControlUsed = false;
	private long lFireDoorReportChecktime = 10000;
	private boolean isFireDoorAllNodeControlUsage = false;
	private long lFireDoorSorcketReconnectTime = 2000;
	
	private static final String FIREDOORMANAGER_TRACE = "FireDoorManagerDebug";
	private static Logger fireDoorManagerTraceLog = Logger.getLogger(FIREDOORMANAGER_TRACE);
	
	DBAccessManager dbAccessManager = null;

	private FireDoorManager() {
		this.dbAccessManager = new DBAccessManager();
		for (int i = 0; i < 5; i++) {
			if (dbAccessManager.IsDBConnected() == true)
				break;
			dbAccessManager.ReconnectToDB();
		}
		
		initilize();
	}
	
	synchronized static public FireDoorManager getInstance() {
		if(manager == null) {
			manager = new FireDoorManager();
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
		updateFireDoorFromDB();
		updateFireDoorParamFromDB();
	}
	
	public boolean isFireDoorCommfailControlUsed() {
		return isFireDoorCommfailControlUsed;
	}
	
	public boolean isFireDoorMonitoringControlUsed() {
		return isFireDoorMonitoringControlUsed;
	}
	
	public long getFireDoorReportChecktime() {
		return lFireDoorReportChecktime;
	}
	
	public boolean isFireDoorAllNodeControlUsed() {
		return isFireDoorAllNodeControlUsage;
	}
	
	public long getFireDoorSorcketReconnectTime() {
		return lFireDoorSorcketReconnectTime;
	}
	
	synchronized public boolean update() {
		updateFireDoorFromDB();
		updateDisableNodeFromDB();
		updateFireDoorToDB();
		updateFireDoorParamFromDB();
		
		return true;
	}

	private static String SELECT_FIREDOOR_SQL = "SELECT * FROM FIREDOOR ORDER BY FIREDOORID";

	private void updateFireDoorFromDB() {
		// 1. DB의 FIREDOOR 최신 정보를 Manager에 업데이트 
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_FIREDOOR_SQL);
			if (rs != null) {
				Vector<String> removeFireDoor = new Vector<String>(data.keySet());
				while (rs.next()) {
					String fireDoorId = rs.getString(FIREDOORID);
					FireDoor fireDoor = data.get(fireDoorId);
					if (fireDoor == null) {
						fireDoor = new FireDoor(fireDoorId);
						fireDoor.setIpAddress(MakeString(rs.getString(IPADDRESS)));
						fireDoor.setStatus(MakeString(rs.getString(STATUS)));
						fireDoor.setErrorCode(MakeString(rs.getString(ERRORCODE)));
						data.put(fireDoorId, fireDoor);
					}
					
					//2018.11.22 by kw3711.kim: UI에서 NodeList 수정가능하여 NodeList를 주기로 업데이트함.
					String tempNodeList = MakeString(rs.getString(NODELIST));
					String[] nodes = tempNodeList.split(",");
					List<String> nodeList = new ArrayList<String>();
					for(String node: nodes) {
						nodeList.add(node.trim());
					}
					fireDoor.setNodeList(nodeList);
					
					fireDoor.setEnabled(MakeString(rs.getString(ENABLED)));
					removeFireDoor.remove(fireDoorId);
				}
				
				for (String removeKey : removeFireDoor) {
					data.remove(removeKey);
				}
			}
		} catch (SQLException e) {
			fireDoorManagerTrace("Exception updateFireDoorFromDB", e);
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
				
				Vector<String> removeDisabledNode = new Vector<String>();
				removeDisabledNode.addAll(disabledNode);
				 
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
			fireDoorManagerTrace("Exception updateDisableNodeFromDB", e);
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}
	}
	
	private boolean updateFireDoorToDB() {
		// 16.09.06 LSH: FIREDOOR_MONITORING_CONTROL_USAGE가 YES일 때만, Commfail 처리하도록 수정
		if (isFireDoorMonitoringControlUsed){
			updateCommfailToDB();
		}
		updateStatusToDB();
		return true;
	}
	
	public boolean addStatusToUpdateList(FireDoor fireDoor) {
		synchronized (updateStatusFireDoor) {
			if (updateStatusFireDoor.contains(fireDoor) == false) {
				return updateStatusFireDoor.add(fireDoor);
			}
		}
		return false;
	}
	
	private static String UPDATE_STATUS_SQL = "UPDATE FIREDOOR SET STATUS=?,ERRORCODE=? WHERE FIREDOORID=?";

	private boolean updateStatusToDB() {
		if (updateStatusFireDoor.size() == 0) {
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
			
			Vector<FireDoor> updateClone = new Vector<FireDoor>();
			updateClone.addAll(updateStatusFireDoor);
			
			ListIterator<FireDoor> iterator = updateClone.listIterator();
			FireDoor fireDoor = null;
			while (iterator.hasNext()) {
				fireDoor = iterator.next();
				if (fireDoor != null) {
					// STATUS=?,ERRORCODE=? WHERE FIREDOORID=?
					pstmt.setString(1, fireDoor.getStatus());
					pstmt.setInt(2, fireDoor.getErrorCode());
					pstmt.setString(3, fireDoor.getFireDoorId());
					pstmt.addBatch();
					updateStatusFireDoor.remove(fireDoor);
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			// 2012.11.07 by MYM : Exception인 경우 업데이트 요청 모두 삭제
			updateStatusFireDoor.clear();
			fireDoorManagerTrace("Exception updateStatusToDB", e);
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

	public boolean addCommfailToUpdateList(String fireDoorId) {
		synchronized (updateCommfailFireDoor) {
			if (updateCommfailFireDoor.contains(fireDoorId) == false) {
				return updateCommfailFireDoor.add(fireDoorId);
			}
		}
		return false;
	}
	
	private static String UPDATE_COMMFAIL_SQL = "UPDATE FIREDOOR SET ERRORCODE=? WHERE FIREDOORID=?";
	private static String ERRORCODE_FIREDOOR_COMFAIL = "9999";

	private boolean updateCommfailToDB() {
		if (updateCommfailFireDoor.size() == 0) {
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
			
			Vector<String> updateClone = new Vector<String>();
			updateClone.addAll(updateCommfailFireDoor);
			
			ListIterator<String> iterator = updateClone.listIterator();
			String fireDoorId = null;
			while (iterator.hasNext()) {
				fireDoorId = iterator.next();
				if (fireDoorId != null) {
					pstmt.setString(1, ERRORCODE_FIREDOOR_COMFAIL);
					pstmt.setString(2, fireDoorId);
					pstmt.addBatch();
					updateCommfailFireDoor.remove(fireDoorId);
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			fireDoorManagerTrace("Exception updateCommfailToDB", e);
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
	
	public boolean setNodeEnable(List<String> nodeIdList, boolean enable) {
		Connection conn = null;
		boolean result = false;
		String userId = "FIREDOOR";
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			String databaseTime = getDatabaseTime(conn);

			// 2018.11.12 by kw3711.kim : Firedoor에 의해 Diable된 Node는 사용자의 조작에 의해서만 Enable
			if (enable) {
//				Iterator<String> iter = nodeIdList.iterator();
//				while(iter.hasNext()) {
//					String nodeId = iter.next();
//					if (disabledNode.contains(nodeId)){
//						setNodeEnabledTrue(conn, databaseTime, userId, nodeId);
//					}
//				}
			} else {
				
				Iterator<String> iter = null;
				if(isFireDoorAllNodeControlUsage) {
					List<String> allNodeIdList = new Vector<String>();
					for(FireDoor fireDoorData : data.values()) {
						allNodeIdList.addAll(fireDoorData.getNodeList());
					}
					iter = allNodeIdList.iterator();
				} else {
					iter = nodeIdList.iterator();
				}
				while(iter.hasNext()) {
					String nodeId = iter.next();
					if (!disabledNode.contains(nodeId)){
						setNodeEnabledFalse(conn, databaseTime, userId, nodeId);
						
						System.out.println("#### FireDoor diable Node : " + nodeId);
					}
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
	
	// 2018.11.12 by kw3711.kim : Firedoor에 의해 Diable된 Node는 사용자의 조작에 의해서만 Enable
//	private static final String SET_NODE_ENALBED_TRUE = "UPDATE NODE SET ENABLED='TRUE',STATUSCHANGEDTIME=?, LASTOPENEDTIME=?,LASTOPENUSER=?,COMMENTS='' WHERE NODEID=? AND LASTCLOSEUSER='FIREDOOR' AND ENABLED='FALSE'";

//	private boolean setNodeEnabledTrue(Connection conn, String databaseTime, String userId, String nodeId) throws Exception {
//		PreparedStatement pstmt = null;
//		boolean result = false;
//
//		try {
//			pstmt = conn.prepareStatement(SET_NODE_ENALBED_TRUE);
//			pstmt.setString(1, databaseTime);
//			pstmt.setString(2, databaseTime);
//			pstmt.setString(3, userId);
//			pstmt.setString(4, nodeId);
//			pstmt.execute();
//			result = true;
//		} catch (Exception e) {
//			result = false;
//			throw e;
//		} finally {
//			if (pstmt != null) {
//				try { pstmt.close(); } catch (Exception e) { }
//			}
//		}
//		return result;
//	}

	private static final String SET_NODE_ENALBED_FALSE = "UPDATE NODE SET ENABLED='FALSE',STATUSCHANGEDTIME=?,LASTCLOSEDTIME=?,LASTCLOSEUSER=?,COMMENTS=? WHERE NODEID=? AND ENABLED='TRUE'";

	private boolean setNodeEnabledFalse(Connection conn, String databaseTime, String userId, String nodeId) throws Exception {
		PreparedStatement pstmt = null;
		boolean result = false;
		String reason = "FIREDOOR is abnormal status now.";

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
	
	private static String SELECT_PARAM_SQL = "SELECT NAME, VALUE FROM OCSINFO WHERE NAME='"
			+ FIREDOOR_COMMFAIL_CONTROL_USAGE
			+ "' OR NAME='"
			+ FIREDOOR_MONITOR_CONTROL_USAGE
			+ "' OR NAME='"
			+ FIREDOOR_REPORT_CHECKTIME
			+ "' OR NAME='"
			+ FIREDOOR_ALL_NODE_CONTROL_USAGE + "'";
	private boolean updateFireDoorParamFromDB() {
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_PARAM_SQL);
			if (rs != null) {
				while (rs.next()) {
					String name = rs.getString(NAME);
					if (FIREDOOR_COMMFAIL_CONTROL_USAGE.equals(name)) {
						String value = rs.getString(VALUE);
						if (value != null && "YES".equals(value)) {
							isFireDoorCommfailControlUsed = true;
						} else {
							isFireDoorCommfailControlUsed = false;
						}
					} else if (FIREDOOR_MONITOR_CONTROL_USAGE.equals(name)) {
						String value = rs.getString(VALUE);
						if (value != null && "YES".equals(value)) {
							isFireDoorMonitoringControlUsed = true;
						} else {
							isFireDoorMonitoringControlUsed = false;
						}
					} else if (FIREDOOR_REPORT_CHECKTIME.equals(name)) {
						long value = rs.getLong(VALUE);
						lFireDoorReportChecktime = (int) value * 1000;
						if (lFireDoorReportChecktime < 1000) lFireDoorReportChecktime = 1000;
						else if (lFireDoorReportChecktime > 60000) lFireDoorReportChecktime = 60000;
					} else if (FIREDOOR_ALL_NODE_CONTROL_USAGE.equals(name)) {
						String value = rs.getString(VALUE);
						if (value != null && "YES".equals(value)) {
							isFireDoorAllNodeControlUsage = true;
						} else {
							isFireDoorAllNodeControlUsage = false;
						}
					} else if (FIREDOOR_SOCKET_RECONNECT_TIME.equals(name)) {
						long value = rs.getLong(VALUE);
						lFireDoorSorcketReconnectTime = (int) value * 1000;
						if (lFireDoorSorcketReconnectTime < 1000) lFireDoorReportChecktime = 1000;
						else if (lFireDoorSorcketReconnectTime > 60000) lFireDoorReportChecktime = 60000;
					}
				}
			}
		} catch (Exception e) {
			fireDoorManagerTrace("Exception updateHidParamFromDB", e);
			return false;
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}

		return true;
	}
	
	
	private static String UPDATE_ERRORCODE_CLEAR_SQL = "UPDATE FIREDOOR SET ERRORCODE='0'";

	// 16.09.06 LSH: FIREDOOR_MONITORING_CONTROL_USAGE가 NO로 변경 될 때, ErrorCode 정리
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
			fireDoorManagerTrace("Exception updateErrorCodeClearToDB", e);
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
	
	public ConcurrentHashMap<String, FireDoor> getData() {
		return data;
	}
	
	public void fireDoorManagerTrace(String message, Throwable e) {
		fireDoorManagerTraceLog.error(message, e);
	}
}
