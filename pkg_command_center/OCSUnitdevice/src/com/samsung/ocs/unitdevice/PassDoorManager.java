package com.samsung.ocs.unitdevice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.PassDoor;

/**
 * PassDoor Manager Class
 * @author zzang9un
 * @date	2015.01.19
 */
public class PassDoorManager {
	private static PassDoorManager manager = null;
	private ConcurrentHashMap<String, PassDoor> data = new ConcurrentHashMap<String, PassDoor>();
	private Vector<PassDoor> updateStatusPassDoor = new Vector<PassDoor>();
	private Vector<String> updateCommfailPassDoor = new Vector<String>();
	
	private static String PASSDOORID = "PASSDOORID";
	private static String ENABLED = "ENABLED";
	private static String IPADDRESS = "IPADDRESS";
	private static String NODEID = "NODEID";
	private static String MODE = "PASSDOORMODE";
	private static String ERRORCODE = "ERRORCODE";
	private static String DATA = "DATA";
	
	private static final String PASSDOORMANAGER_TRACE = "PassDoorManagerDebug";
	private static Logger passDoorManagerTraceLog = Logger.getLogger(PASSDOORMANAGER_TRACE);
	
	DBAccessManager dbAccessManager = null;

	private PassDoorManager() {
		this.dbAccessManager = new DBAccessManager();
		for (int i = 0; i < 5; i++) {
			if (dbAccessManager.IsDBConnected() == true)
				break;
			dbAccessManager.ReconnectToDB();
		}
		
		initilize();
	}
	
	synchronized static public PassDoorManager getInstance() {
		if(manager == null) {
			manager = new PassDoorManager();
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
		updatePassDoorFromDB();
	}
	
	synchronized public boolean update() {
		updatePassDoorFromDB();
		updatePassDoorToDB();
		return true;
	}

	private static String SELECT_PASSDOOR_SQL = "SELECT * FROM PASSDOOR ORDER BY IPADDRESS, PASSDOORID";
	/**
	 * DB 정보를 이용하여 PassDoor 업데이트
	 * @author zzang9un
	 * @date 2015. 2. 4.
	 */
	private void updatePassDoorFromDB() {
		// 1. DB의 HID 최신 정보를 Manager에 업데이트 
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_PASSDOOR_SQL);
			if (rs != null) {
				Vector<String> removePassDoor = new Vector<String>(data.keySet()); 
				while (rs.next()) {
					String passDoorId = rs.getString(PASSDOORID);
					PassDoor passDoor = data.get(passDoorId);
					if (passDoor == null) {
						passDoor = new PassDoor(passDoorId);
						passDoor.setIpAddress(MakeString(rs.getString(IPADDRESS)));
						data.put(passDoorId, passDoor);
					}
					passDoor.setEnabled(MakeString(rs.getString(ENABLED)));
					removePassDoor.remove(passDoorId);
				}
				
				for (String removeKey : removePassDoor) {
					data.remove(removeKey);
				}
			}
		} catch (SQLException e) {
			passDoorManagerTrace("Exception updatePassDoorFromDB", e);
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}
	}
	
	private boolean updatePassDoorToDB() {
		updateCommfailToDB();
		updateStatusToDB();
		return true;
	}
	
	public boolean addStatusToUpdateList(PassDoor passDoor) {
		synchronized (updateStatusPassDoor) {
			if (updateStatusPassDoor.contains(passDoor) == false) {
				return updateStatusPassDoor.add(passDoor);
			}
		}
		return false;
	}
	
	private static String UPDATE_STATUS_SQL = "UPDATE PASSDOOR SET PASSDOORMODE=?,STATUS=?,SENSORDATA=?,ERRORCODE=?,PIODATA=? WHERE PASSDOORID=?";
	/**
	 * status를 DB에 update한다.
	 * @author zzang9un
	 * @date 2015. 2. 4.
	 * @return
	 */
	private boolean updateStatusToDB() {
		if (updateStatusPassDoor.size() == 0) {
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
			
			Vector<PassDoor> updateClone = (Vector<PassDoor>) updateStatusPassDoor.clone();
			ListIterator<PassDoor> iterator = updateClone.listIterator();
			PassDoor passDoor = null;
			while (iterator.hasNext()) {
				passDoor = iterator.next();
				if (passDoor != null) {
					// PASSDOORMODE=?,STATUS=?,SENSORDATA=?,ERRORCODE=?,PIODATA=? WHERE PASSDOORID=?
					pstmt.setString(1, passDoor.getMode());
					pstmt.setString(2, passDoor.getStatus());
					pstmt.setString(3, passDoor.getSensorData());
					pstmt.setInt(4, passDoor.getErrorCode());
					pstmt.setString(5, passDoor.getPioData());
					pstmt.setString(6, passDoor.getPassDoorId());
					pstmt.addBatch();
					updateStatusPassDoor.remove(passDoor);
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			// 2012.11.07 by MYM : Exception인 경우 업데이트 요청 모두 삭제
			updateStatusPassDoor.clear();
			passDoorManagerTrace("Exception updateStatusToDB", e);
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
		synchronized (updateCommfailPassDoor) {
			if (updateCommfailPassDoor.contains(ipaddress) == false) {
				return updateCommfailPassDoor.add(ipaddress);
			}
		}
		return false;
	}
	
	private static String UPDATE_COMMFAIL_SQL = "UPDATE PASSDOOR SET ERRORCODE=? WHERE IPADDRESS=?";
	private static String ERRORCODE_PASSDOOR_COMFAIL = "9999";
	/**
	 * 통신 장애인 PassDoor 정보를 DB에 update한다.
	 * @author zzang9un
	 * @date 2015. 2. 4.
	 * @return
	 */
	private boolean updateCommfailToDB() {
		if (updateCommfailPassDoor.size() == 0) {
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
			
			Vector<String> updateClone = (Vector<String>) updateCommfailPassDoor.clone();
			ListIterator<String> iterator = updateClone.listIterator();
			String ipaddress = null;
			while (iterator.hasNext()) {
				ipaddress = iterator.next();
				if (ipaddress != null) {
					pstmt.setString(1, ERRORCODE_PASSDOOR_COMFAIL);
					pstmt.setString(2, ipaddress);
					pstmt.addBatch();
					updateCommfailPassDoor.remove(ipaddress);
				}
			}
			pstmt.executeBatch();
			conn.commit();			
			result = true;
		} catch (Exception e) {
			result = false;
			passDoorManagerTrace("Exception updateCommfailToDB", e);
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
	
	public ConcurrentHashMap<String, PassDoor> getData() {
		return data;
	}
	
	public void passDoorManagerTrace(String message, Throwable e) {
		passDoorManagerTraceLog.error(message, e);
	}
}
