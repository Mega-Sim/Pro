package com.samsung.ocs.unitdevice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.HID;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class HIDManager {
	private static HIDManager manager = null;
	private ConcurrentHashMap<String, HID> data = new ConcurrentHashMap<String, HID>();
	private Vector<HID> updateStatus = new Vector<HID>();
	private Vector<HID> updateStatusWithBackupHid = new Vector<HID>();
	private Vector<HID> updateRemoteCmd = new Vector<HID>();
	private Vector<String> updateCommfail = new Vector<String>();
	
	private static String UNITID = "UNITID";
	private static String ENABLED = "ENABLED";
	private static String IPADDRESS = "IPADDRESS";
	private static String REMOTECMD = "REMOTECMD";
	private static String NAME = "NAME";
	private static String VALUE = "VALUE";
	private static String HID_CIPHER = "HID_CIPHER";
	private static String HID_POWER_CONTROL_USAGE = "HID_POWER_CONTROL_USAGE";
	private static String LOG_HOLDINGPERIOD_DEFAULT = "LOG_HOLDINGPERIOD_DEFAULT";
	private static String LOG_HOLDINGPERIOD_UNITDEVICE = "LOG_HOLDINGPERIOD_UNITDEVICE";
	
	private boolean isHIDPowerControlUsed = false;
	private int hidCipher = 3;
	private int logHoldingPeriod = -1;
	private int defaultLogHoldingPeriod = 7;
	
	private static final String HIDMANAGER_TRACE = "HIDManagerDebug";
	private static Logger hidManagerTraceLog = Logger.getLogger(HIDMANAGER_TRACE);
	
	DBAccessManager dbAccessManager = null;

	private HIDManager() {
		this.dbAccessManager = new DBAccessManager();
		for (int i = 0; i < 5; i++) {
			if (dbAccessManager.IsDBConnected() == true)
				break;
			dbAccessManager.ReconnectToDB();
		}
		
		initilize();
	}
	
	synchronized static public HIDManager getInstance() {
		if(manager == null) {
			manager = new HIDManager();
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
		updateHidFromDB();
		updateHidParamFromDB();
	}
	
	public int getHIDCipher() {
		return hidCipher;
	}
	
	public boolean isHIDPowerControlUsed() {
		return isHIDPowerControlUsed;
	}
	
	public int getLogHoldingPeriod() {
		if (logHoldingPeriod == -1) {
			return defaultLogHoldingPeriod;
		}
		return logHoldingPeriod;
	}	
	
	synchronized public boolean update() {
		updateHidFromDB();
		updateHidToDB();
		updateHidParamFromDB();
		return true;
	}

	private static String SELECT_HID_SQL = "SELECT * FROM UNITDEVICE WHERE TYPE='HID' ORDER BY IPADDRESS, UNITID";
	private boolean updateHidFromDB() {
		// 1. Vehicle이 HID_down 관련 Error 발생시 해당 위치의 hid Down 명령 요청
		checkRemoteCmd();
		
		// 2. DB의 HID 최신 정보를 Manager에 업데이트 
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_HID_SQL);
			if (rs != null) {
				Vector<String> removeHid = new Vector<String>(data.keySet()); 
				while (rs.next()) {
					String unitId = rs.getString(UNITID);
					HID hid = data.get(unitId);
					if (hid == null) {
						hid = new HID(unitId);
						hid.setIpAddress(MakeString(rs.getString(IPADDRESS)));
						data.put(unitId, hid);
					}
					hid.setEnabled(MakeString(rs.getString(ENABLED)));
					hid.setRemoteCmd(MakeString(rs.getString(REMOTECMD)));
					removeHid.remove(unitId);
				}
				
				for (String removeKey : removeHid) {
					data.remove(removeKey);
				}
			}
		} catch (SQLException e) {
			hidManagerTrace("Exception updateHIDFromDB", e);
			
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}

		return true;
	}
	
	private boolean updateHidToDB() {
		updateCommfailToDB();
		updateStatusToDB();
		// 2012.11.07 by MYM : BackupHid 업데이트 안되는 문제 수정
		updateStatusWithBackupHidToDB();
		updateRemoteCmdToDB();		
		return true;
	}
	
	public boolean addStatusToUpdateList(HID hid) {
		synchronized (updateStatus) {
			if (updateStatus.contains(hid) == false) {
				return updateStatus.add(hid);
			}
		}
		return false;
	}
	
	private static String UPDATE_STATUS_SQL = "UPDATE UNITDEVICE SET STATUS=?,VOLTAGE=?,ELECTRICCURRENT=?,TEMPERATURE=?,FREQUENCY=?,ERRORCODE=? WHERE TYPE='HID' AND UNITID=?";
	private boolean updateStatusToDB() {
		if (updateStatus.size() == 0) {
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
			
			Vector<HID> updateClone = (Vector<HID>) updateStatus.clone();
			ListIterator<HID> iterator = updateClone.listIterator();
			HID hid = null;
			while (iterator.hasNext()) {
				hid = iterator.next();
				if (hid != null) {
					// STATUS=?,VOLTAGE=?,ELECTRICCURRENT=?,TEMPERATURE=?,FREQUENCY=?,ERRORCODE=?,REMOTECMD=?
					pstmt.setString(1, hid.getStatus());
					pstmt.setString(2, hid.getVoltage());
					pstmt.setString(3, hid.getElectricCurrent());
					pstmt.setString(4, hid.getTemperature());
					pstmt.setString(5, hid.getFrequency());
					pstmt.setInt(6, hid.getErrorCode());
					pstmt.setString(7, hid.getHid());
					pstmt.addBatch();
					updateStatus.remove(hid);
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			// 2012.11.07 by MYM : Exception인 경우 업데이트 요청 모두 삭제
			updateStatus.clear();
			hidManagerTrace("Exception updateStatusToDB", e);
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
	
	/**
	 * 2012.11.07 by MYM : BackupHid 업데이트 안되는 문제 수정
	 * 
	 * @param hid
	 * @return
	 */
	public boolean addStatusWithBackupHidToUpdateList(HID hid) {
		synchronized (updateStatusWithBackupHid) {
			if (updateStatusWithBackupHid.contains(hid) == false) {
				return updateStatusWithBackupHid.add(hid);
			}
		}
		return false;
	}
	
	private static String UPDATE_STATUS_WITHBACKUPHID_SQL = "UPDATE UNITDEVICE SET STATUS=?,VOLTAGE=?,ELECTRICCURRENT=?,TEMPERATURE=?,FREQUENCY=?,ERRORCODE=?,BACKUPHID=? WHERE TYPE='HID' AND UNITID=?";
	private boolean updateStatusWithBackupHidToDB() {
		if (updateStatusWithBackupHid.size() == 0) {
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
			pstmt = conn.prepareStatement(UPDATE_STATUS_WITHBACKUPHID_SQL);
			
			Vector<HID> updateClone = (Vector<HID>) updateStatusWithBackupHid.clone();
			ListIterator<HID> iterator = updateClone.listIterator();
			HID hid = null;
			while (iterator.hasNext()) {
				hid = iterator.next();
				if (hid != null) {
					// STATUS=?,VOLTAGE=?,ELECTRICCURRENT=?,TEMPERATURE=?,FREQUENCY=?,ERRORCODE=?,REMOTECMD=?
					pstmt.setString(1, hid.getStatus());
					pstmt.setString(2, hid.getVoltage());
					pstmt.setString(3, hid.getElectricCurrent());
					pstmt.setString(4, hid.getTemperature());
					pstmt.setString(5, hid.getFrequency());
					pstmt.setInt(6, hid.getErrorCode());
					pstmt.setString(7, hid.getBackupHID());
					pstmt.setString(8, hid.getHid());
					pstmt.addBatch();
					updateStatusWithBackupHid.remove(hid);
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			// 2012.11.07 by MYM : Exception인 경우 업데이트 요청 모두 삭제
			updateStatusWithBackupHid.clear();
			hidManagerTrace("Exception updateStatusWithBackupHidToDB", e);
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
	
	public boolean addRemoteCmdToUpdateList(HID hid) {
		synchronized (updateRemoteCmd) {
			if (updateRemoteCmd.contains(hid) == false) {
				return updateRemoteCmd.add(hid);
			}
		}
		return false;
	}
	
	private static String UPDATE_REMOTECMD_SQL = "UPDATE UNITDEVICE SET REMOTECMD='' WHERE TYPE='HID' AND UNITID=?";
	private boolean updateRemoteCmdToDB() {
		if (updateRemoteCmd.size() == 0) {
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
			pstmt = conn.prepareStatement(UPDATE_REMOTECMD_SQL);
			
			Vector<HID> updateClone = (Vector<HID>) updateRemoteCmd.clone();
			ListIterator<HID> iterator = updateClone.listIterator();
			HID hid = null;
			while (iterator.hasNext()) {
				hid = iterator.next();
				if (hid != null) {
					pstmt.setString(1, hid.getHid());
					pstmt.addBatch();
					updateRemoteCmd.remove(hid);
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			hidManagerTrace("Exception updateRemoteCmdToDB", e);
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
		synchronized (updateCommfail) {
			if (updateCommfail.contains(ipaddress) == false) {
				return updateCommfail.add(ipaddress);
			}
		}
		return false;
	}
	
	private static String UPDATE_COMMFAIL_SQL = "UPDATE UNITDEVICE SET ERRORCODE=1000 WHERE TYPE='HID' AND IPADDRESS=?";
	private boolean updateCommfailToDB() {
		if (updateCommfail.size() == 0) {
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
			
			Vector<String> updateClone = (Vector<String>) updateCommfail.clone();
			ListIterator<String> iterator = updateClone.listIterator();
			String ipaddress = null;
			while (iterator.hasNext()) {
				ipaddress = iterator.next();
				if (ipaddress != null) {
					pstmt.setString(1, ipaddress);
					pstmt.addBatch();
					updateCommfail.remove(ipaddress);
				}
			}
			pstmt.executeBatch();
			conn.commit();			
			result = true;
		} catch (Exception e) {
			result = false;
			hidManagerTrace("Exception updateCommfailToDB", e);
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
	
	private static String SELECT_PARAM_SQL = "SELECT NAME, VALUE FROM OCSINFO WHERE NAME='HID_CIPHER' OR NAME='HID_POWER_CONTROL_USAGE' OR NAME='LOG_HOLDINGPERIOD_UNITDEVICE' OR NAME='LOG_HOLDINGPERIOD_DEFAULT'";
	private boolean updateHidParamFromDB() {
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_PARAM_SQL);
			if (rs != null) {
				while (rs.next()) {
					String name = rs.getString(NAME);
					if (HID_CIPHER.equals(name)) {
						hidCipher = rs.getInt(VALUE);
					} else if (HID_POWER_CONTROL_USAGE.equals(name)) {
						String value = rs.getString(VALUE);
						if (value != null && "YES".equals(value)) {
							isHIDPowerControlUsed = true;
						} else {
							isHIDPowerControlUsed = false;
						}
					} else if (LOG_HOLDINGPERIOD_UNITDEVICE.equals(name)) {
						long value = rs.getLong(VALUE);
						logHoldingPeriod = (int) value;
					} else if (LOG_HOLDINGPERIOD_DEFAULT.equals(name)) {
						long value = rs.getLong(VALUE);
						defaultLogHoldingPeriod = (int) value;
					}
				}				
			}
		} catch (Exception e) {
			hidManagerTrace("Exception updateHidParamFromDB", e);
			return false;
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}

		return true;
	}
	
	// 2018.08.31 by LSH : SQL Cartesian Product 발생 방지를 위한 HINT 추가
//	private static String CHECKREMOTECMD_SQL = "UPDATE UNITDEVICE SET REMOTECMD = 'PAUSE' WHERE TYPE='HID' AND UNITID IN (" +
//	"SELECT B.HID FROM VEHICLE A, NODE B, UNITDEVICE C, VEHICLEERROR D WHERE " +
//	"A.VEHICLEMODE='M' AND A.STATUS='E' AND A.ENABLED='TRUE' AND A.CURRNODE=B.NODEID AND B.HID=C.UNITID AND C.TYPE='HID' AND C.STATUS='R' AND A.ERRORCODE=D.ERRORCODE AND D.ACTIONTYPE='HID_Down')";
	private static String CHECKREMOTECMD_SQL = "UPDATE UNITDEVICE SET REMOTECMD = 'PAUSE' WHERE TYPE='HID' AND UNITID IN (" +
			"SELECT /*+ LEADING(D A B C) */ B.HID FROM VEHICLE A, NODE B, UNITDEVICE C, VEHICLEERROR D WHERE " +
			"A.VEHICLEMODE='M' AND A.STATUS='E' AND A.ENABLED='TRUE' AND A.CURRNODE=B.NODEID AND B.HID=C.UNITID AND C.TYPE='HID' AND C.STATUS='R' AND A.ERRORCODE=D.ERRORCODE AND D.ACTIONTYPE='HID_Down')";
	synchronized private void checkRemoteCmd() {
		try {
			dbAccessManager.ExecSQL(CHECKREMOTECMD_SQL);
		} catch (SQLException e) {
			hidManagerTrace("Exception checkRemoteCmd", e);
		}
	}

	private static String RESET_REMOTECMD_SQL = "UPDATE UNITDEVICE SET REMOTECMD='' WHERE TYPE='HID'";
	synchronized public void resetAllRemoteCmd() {
		try {
			dbAccessManager.ExecSQL(RESET_REMOTECMD_SQL);
		} catch (SQLException e) {
			hidManagerTrace("Exception resetAllRemoteCmd", e);
		}
	}
	
	public ConcurrentHashMap<String, HID> getData() {
		return data;
	}
	
	public void hidManagerTrace(String message, Throwable e) {
		hidManagerTraceLog.error(message, e);
	}
}
