package com.samsung.ocs.unitdevice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.DockingStation;


public class DockingStationManager {
	private static DockingStationManager manager = null;
	private Vector<DockingStation> updateStatus = new Vector<DockingStation>();
	private ConcurrentHashMap<String, DockingStation> data = new ConcurrentHashMap<String, DockingStation>();
	private Vector<String> updateCommfail = new Vector<String>();
	
	private static String DOCKINGSTATIONID = "DOCKINGSTATIONID";
	private static String ENABLED = "ENABLED";
	private static String IPADDRESS = "IPADDRESS";
	private static String STATIONMODE = "STATIONMODE";
	private static String CARRIEREXIST = "CARRIEREXIST";
	private static String CARRIERCHARGED = "CARRIERCHARGED";
	private static String ALARMID = "ALARMID";
	private static String CARRIERID = "CARRIERID";
	
	private static String NAME = "NAME";
	private static String VALUE = "VALUE";
	private static String LOG_HOLDINGPERIOD_DEFAULT = "LOG_HOLDINGPERIOD_DEFAULT";
	private static String LOG_HOLDINGPERIOD_UNITDEVICE = "LOG_HOLDINGPERIOD_UNITDEVICE";
	private static String DOCKINGSTATION_USAGE = "DOCKINGSTATION_USAGE";
	
	private int logHoldingPeriod = -1;
	private int defaultLogHoldingPeriod = 7;
	private String dockingStationUsage = null;
	
	DBAccessManager dbAccessManager = null;
	
	
	private static final String DOCKINGSTATIONMANAGER_TRACE = "DockingStationManagerDebug";
	private static Logger dockingStationManagerTraceLog = Logger.getLogger(DOCKINGSTATIONMANAGER_TRACE);
	
	private DockingStationManager() {
		this.dbAccessManager = new DBAccessManager();
		for (int i = 0; i < 5; i++) {
			if (dbAccessManager.IsDBConnected() == true)
				break;
			dbAccessManager.ReconnectToDB();
		}
		initilize();
	}
	synchronized static public DockingStationManager getInstance() {
		if(manager == null) {
			manager = new DockingStationManager();
		}
		return manager;
	}
	private void initilize() {
		updateDockingStationFromDB();
		updateDockingStationParamFromDB();			
	}
	
	public boolean getDockingStationUsage() {
		if("YES".equals(dockingStationUsage)) {
			return true;
		} else {
			return false;
		}
	}
	
	public int getLogHoldingPeriod() {
		if (logHoldingPeriod == -1) {
			return defaultLogHoldingPeriod;
		}
		return logHoldingPeriod;
	}
	
	synchronized public boolean update() {		
		updateDockingStationFromDB();
		// 현재 docking station 정보를 DB에 update
		updateDockingStationToDB();
		updateDockingStationParamFromDB();		
		return true;
	}
	
	public ConcurrentHashMap<String, DockingStation> getData() {
		return data;
	}	
	
	// DB에 저장된 docking station 정보 추출
	private static String SELECT_DOCKINGSTATION_SQL = "SELECT * FROM DOCKINGSTATION ORDER BY DOCKINGSTATIONID, IPADDRESS";
	private boolean updateDockingStationFromDB() {
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_DOCKINGSTATION_SQL);
			if (rs != null) {
				Vector<String> removeDockingStation = new Vector<String>(data.keySet()); 
				while (rs.next()) {
					String dockingStationId = rs.getString(DOCKINGSTATIONID);
					// DB record에 해당하는 dockingStationID에 해당하는 docking station 할당
					DockingStation dockingStation = data.get(dockingStationId);
					// data ConcurrentHashMap에 해당 docking station이 없는 경우 Map에 추가
					if (dockingStation == null) {
						dockingStation = new DockingStation(dockingStationId);
						data.put(dockingStationId, dockingStation);
					}
					dockingStation.setEnabled(rs.getString(ENABLED));
					dockingStation.setIpAddress(makeString(rs.getString(IPADDRESS)));
					dockingStation.setStationMode(rs.getString(STATIONMODE));
					dockingStation.setCarrierExist(rs.getInt(CARRIEREXIST));
					dockingStation.setCarrierCharge(rs.getInt(CARRIERCHARGED));
					dockingStation.setAlarmID(rs.getInt(ALARMID));
					dockingStation.setCarrierID(rs.getString(CARRIERID));
										
					removeDockingStation.remove(dockingStationId);					
				}				
				for (String removeKey : removeDockingStation) {
					data.remove(removeKey);
				}				
			}			
		} catch (SQLException e) {			
			dockingStationManagerTrace("Exception updateDockingStationFromDB", e);
			
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}
		return true;	
	}
	private boolean updateDockingStationToDB() {
		updateCommFailToDB();
		updateStatusToDB();
		return true;
	}	
	private static String UPDATE_STATUS_SQL = "UPDATE DOCKINGSTATION SET STATIONMODE=?,CARRIEREXIST=?,CARRIERCHARGED=?,ALARMID=?,CARRIERID=? WHERE DOCKINGSTATIONID=?";
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
			Vector<DockingStation> updateClone = (Vector<DockingStation>) updateStatus.clone();
			ListIterator<DockingStation> iterator = updateClone.listIterator();
			DockingStation dockingStation = null;
			while (iterator.hasNext()) {
				dockingStation = iterator.next();
				if (dockingStation != null) {
					// STATIONMODE=?,CARRIEREXIST=?,CARRIERCHARGE=?,ALARMID=?,CARRIERID=?,UNITID=?
					pstmt.setString(1, dockingStation.getStationMode());
					pstmt.setInt(2, dockingStation.getCarrierExist());
					pstmt.setInt(3, dockingStation.getCarrierCharged());
					pstmt.setInt(4, dockingStation.getAlarmID());
					pstmt.setString(5, dockingStation.getCarrierID());
					pstmt.setString(6, dockingStation.getDockingStationID());
					pstmt.addBatch();
					updateStatus.remove(dockingStation);					
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception e) {
			result = false;
			updateStatus.clear();
			dockingStationManagerTrace("Exception updateStatusToDB", e);
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

	public boolean updateCommStatus(DockingStation dockingStationOperationData) {
		if(dockingStationOperationData instanceof DockingStation) {			
			DockingStation dockingStation = new DockingStation(dockingStationOperationData.getDockingStationID());
			dockingStation.setDockingStation(dockingStationOperationData.getDockingStationID(), dockingStationOperationData.getStationMode(), dockingStationOperationData.getCarrierExist(), dockingStationOperationData.getCarrierCharged(), dockingStationOperationData.getAlarmID(), dockingStationOperationData.getCarrierID());
			updateStatus.add(dockingStation);
			return true;
		}
		else {
			return false;
		}
	}
	
	private static String SELECT_PARAM_SQL = "SELECT NAME, VALUE FROM OCSINFO WHERE NAME='LOG_HOLDINGPERIOD_UNITDEVICE' " +
			"OR NAME='LOG_HOLDINGPERIOD_DEFAULT' OR NAME='DOCKINGSTATION_USAGE'";
	private boolean updateDockingStationParamFromDB() {
		ResultSet rs = null;
		try {
			rs = dbAccessManager.GetRecord(SELECT_PARAM_SQL);
			if (rs != null) {
				while (rs.next()) {
					String name = rs.getString(NAME);
					if (LOG_HOLDINGPERIOD_UNITDEVICE.equals(name)) {
						long value = rs.getLong(VALUE);
						logHoldingPeriod = (int) value;
					} else if (LOG_HOLDINGPERIOD_DEFAULT.equals(name)) {
						long value = rs.getLong(VALUE);
						defaultLogHoldingPeriod = (int) value;
					} else if(DOCKINGSTATION_USAGE.equals(name)) {
						dockingStationUsage = rs.getString(VALUE);
					}
				}				
			}			
		} catch (Exception e) {
			dockingStationManagerTrace("Exception updateDockingStationParamFromDB", e);
			return false;
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}
		return true;
		
	}
	
	
	private String makeString(String strValue) {
		if (strValue == null)
			return "";
		else
			return strValue;
	}
	
	public void dockingStationManagerTrace(String message, Throwable e) {
		dockingStationManagerTraceLog.error(message, e);
	}
	
	public DockingStation getDockingStation(String stationId) {
		return data.get(stationId);
	}
	
	public boolean addCommFailToUpdateList(String ipaddress) {
		// TODO Auto-generated method stub
		synchronized (updateCommfail) {
			if (updateCommfail.contains(ipaddress) == false) {
				return updateCommfail.add(ipaddress);
			}
		}
		return false;		
	}
	
	private static String UPDATE_COMMFAIL_SQL = "UPDATE DOCKINGSTATION SET ALARMID=1000 WHERE IPADDRESS=?";
	private boolean updateCommFailToDB() {
		if(updateCommfail.size() == 0 ) {
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
			dockingStationManagerTrace("Exception updateCommfailToDB", e);
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

}
