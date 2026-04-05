package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Park;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.index.ZoneTable;

/**
 * ParkManager Class, OCS 3.0 for Unified FAB
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

public class ParkManager extends AbstractManager {
	private static ParkManager manager = null;	
	private static final String NAME = "NAME";
	private static final String TYPE = "TYPE";
	private static final String VEHICLEZONE = "VEHICLEZONE";
	private static final String CAPACITY = "CAPACITY";
	private static final String RANK = "RANK";
	private static final String ENABLED = "ENABLED";
	
	private static final String NODE = "NODE";
	private static final String STATION = "STATION";
	
	// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
	private static final String PARKNODEID = "PARKNODEID";
	private static final String PORTCOUNT = "PORTCOUNT";
	
	private ZoneTable zoneTable = null;
	
	private HashMap<String, String> carrierlocOfPark = new HashMap<String, String>();	// Park Move / Park Locate 용.
	
	/**
	 * Constructor of ParkManager class.
	 */
	private ParkManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(Park.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		zoneTable = ZoneTable.getInstance();
	}
	
	/**
	 * Constructor of ParkManager class. (Singleton)
	 */
	public static synchronized ParkManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new ParkManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		zoneTable = ZoneTable.getInstance();
		
		updateFromDB();
		isInitialized = true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		return true;
	}
	
	private static final String SELECT_SQL7 = "SELECT * FROM PARK WHERE ENABLED='TRUE' AND CAPACITY > 0";

	@Override
	protected boolean updateFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		String parkName;
		String parkType;
		Park park;
		boolean result = false;
		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		StationManager stationManager = StationManager.getInstance(null, null, false, false, 0);
		
		Node parkNode = null;
		Station parkStation = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL7);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				parkName = rs.getString(NAME);
				parkType = rs.getString(TYPE);
				park = (Park)data.get(parkName);
				if (park == null) {
					park = (Park) vOType.newInstance();
					if (STATION.equals(parkType)) {
						parkStation = stationManager.getStation(parkName);
						park.setStation(parkStation);
						parkNode = parkStation.getParentNode();
					} else {
						parkNode = nodeManager.getNode(parkName);
					}
					if (parkNode != null) {
						park.setName(parkName);
						park.setNode(parkNode);
						setPark(park, rs);
						data.put(parkName, park);
					}
				} else {
					setPark(park, rs);
				}
				removeKeys.remove(parkName);
			}
			for (String rmKey : removeKeys) {
				data.remove(rmKey);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			writeExceptionLog(LOGFILENAME, ie);
			ie.printStackTrace();
		} catch (InstantiationException e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
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
	 * @param park
	 * @param rs
	 * @exception SQLException
	 */
	private void setPark(Park park, ResultSet rs) throws SQLException {
		if (park != null && rs != null) {
			park.setType(getString(rs.getString(TYPE)));
			setVehicleZone(park, getString(rs.getString(VEHICLEZONE)));
			park.setCapacity(rs.getInt(CAPACITY));
			park.setRank(rs.getInt(RANK));
			park.setEnabled(getBoolean(rs.getString(ENABLED)));
		} else {
			writeExceptionLog(LOGFILENAME, "setPark(Park park, ResultSet rs) - one of parameters is null.");
		}
	}
	
	private void setVehicleZone(Park park, String zone) {
		park.setVehicleZone(zone);
		park.setVehicleZoneIndex(zoneTable.getZoneIndex(zone));
	}
	
	/**
	 * 
	 * @param parkName
	 * @return
	 */
	public Park getPark(String parkName) {
		return (Park) data.get(parkName);
	}

	// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
	private static final String SELECT_PARKNODE_AND_CARRIERLOC_SQL = "SELECT P.NAME AS PARKNODEID, COUNT(C.CARRIERLOCID) AS PORTCOUNT FROM CARRIERLOC C, PARK P WHERE P.NAME = C.NODE AND (C.TYPE = 'EQPORT' or C.TYPE = 'STOCKERPORT') GROUP BY P.NAME ORDER BY 1 DESC";
	
	// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
		public boolean updateParkPortCount() { 
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		String parkNodeID;
		String portCount;
		boolean result = false;
		try {
			
			carrierlocOfPark.clear();
			
			conn = dbAccessManager.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SELECT_PARKNODE_AND_CARRIERLOC_SQL);
			
			while (rs.next()) {
				parkNodeID = rs.getString(PARKNODEID);
				portCount = rs.getString(PORTCOUNT);
				
				carrierlocOfPark.put(parkNodeID, portCount);
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
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Exception e) {}
				stmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	public boolean isExistPortofPark(String nodeID){
		
		if(carrierlocOfPark.containsKey(nodeID))
			return true;
		return false;
	}
}