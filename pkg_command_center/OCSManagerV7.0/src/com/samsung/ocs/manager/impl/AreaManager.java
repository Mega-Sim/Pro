package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.Area;

/**
 * AreaManager Class, OCS 3.0 for Unified FAB
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

public class AreaManager extends AbstractManager {
	private static AreaManager manager = null;
	private static final String AREAID = "AREAID";
	private static final String MINVHLCOUNT = "MINVHLCOUNT";
	private static final String MAXVHLCOUNT = "MAXVHLCOUNT";
	private static final String MINIDLEVHLCOUNT = "MINIDLEVHLCOUNT";
	private static final String MAXIDLEVHLCOUNT = "MAXIDLEVHLCOUNT";
	private static final String MINVHLCOUNTLOWESTLIMIT = "MINVHLCOUNTLOWESTLIMIT";
	private static final String MINVHLCOUNTUPPERLIMIT = "MINVHLCOUNTUPPERLIMIT";
	private static final String MAXVHLCOUNTLOWESTLIMIT = "MAXVHLCOUNTLOWESTLIMIT";
	private static final String MAXVHLCOUNTUPPERLIMIT = "MAXVHLCOUNTUPPERLIMIT";
	private static final String MINIDLEVHLCOUNTLOWESTLIMIT = "MINIDLEVHLCOUNTLOWESTLIMIT";
	private static final String MINIDLEVHLCOUNTUPPERLIMIT = "MINIDLEVHLCOUNTUPPERLIMIT";
	private static final String MAXIDLEVHLCOUNTLOWESTLIMIT = "MAXIDLEVHLCOUNTLOWESTLIMIT";
	private static final String MAXIDLEVHLCOUNTUPPERLIMIT = "MAXIDLEVHLCOUNTUPPERLIMIT";
	private static final String MINVHLASSIGNALLOWANCE = "MINVHLASSIGNALLOWANCE";
	private static final String MINVHLPARKALLOWANCE = "MINVHLPARKALLOWANCE";
	
	private static final String NODEID = "NODEID";
	/**
	 * Constructor of AreaManager class.
	 */
	private AreaManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(Area.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		}
	}
	
	/**
	 * Constructor of AreaManager class. (Singleton)
	 */
	public static synchronized AreaManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new AreaManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
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
	
	private static final String SELECT_AREA_SQL = "SELECT * FROM AREA";
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	
	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		String areaId;
		Area area;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_AREA_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				areaId = rs.getString(AREAID);
				area = (Area)data.get(areaId);
				if (area == null) {
					area = (Area) vOType.newInstance();
					area.setAreaId(areaId);
					setArea(area, rs);
					data.put(areaId, area);
				} else {
					setArea(area, rs);
				}
				removeKeys.remove(areaId);
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
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
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
	 * @param area
	 * @param rs
	 * @exception SQLException
	 */
	private void setArea(Area area, ResultSet rs) {
		if (area != null && rs != null) {
			try {
				area.setMinVehicleCountLowestLimit(rs.getInt(MINVHLCOUNTLOWESTLIMIT));
			} catch (Exception e) {
				area.setMinVehicleCountLowestLimit(0);
			}
			try {
				area.setMinVehicleCountUpperLimit(rs.getInt(MINVHLCOUNTUPPERLIMIT));
			} catch (Exception e) {
				area.setMinVehicleCountUpperLimit(100);
			}
			try {
				area.setMaxVehicleCountLowestLimit(rs.getInt(MAXVHLCOUNTLOWESTLIMIT));
			} catch (Exception e) {
				area.setMaxVehicleCountLowestLimit(0);
			}
			try {
				area.setMaxVehicleCountUpperLimit(rs.getInt(MAXVHLCOUNTUPPERLIMIT));
			} catch (Exception e) {
				area.setMaxVehicleCountUpperLimit(500);
			}
			try {
				area.setMinIdleVehicleCountLowestLimit(rs.getInt(MINIDLEVHLCOUNTLOWESTLIMIT));
			} catch (Exception e) {
				area.setMinIdleVehicleCountLowestLimit(0);
			}
			try {
				area.setMinIdleVehicleCountUpperLimit(rs.getInt(MINIDLEVHLCOUNTUPPERLIMIT));
			} catch (Exception e) {
				area.setMinIdleVehicleCountUpperLimit(100);
			}
			try {
				area.setMaxIdleVehicleCountLowestLimit(rs.getInt(MAXIDLEVHLCOUNTLOWESTLIMIT));
			} catch (Exception e) {
				area.setMaxIdleVehicleCountLowestLimit(0);
			}
			try {
				area.setMaxIdleVehicleCountUpperLimit(rs.getInt(MAXIDLEVHLCOUNTUPPERLIMIT));
			} catch (Exception e) {
				area.setMaxIdleVehicleCountUpperLimit(500);
			}
			try {
				area.setMinVehicleCount(rs.getInt(MINVHLCOUNT));
			} catch (Exception e) {
				area.setMinVehicleCount(0);
			}
			try {
				area.setMaxVehicleCount(rs.getInt(MAXVHLCOUNT));
			} catch (Exception e) {
				area.setMaxVehicleCount(200);
			}
			try {
				area.setMinIdleVehicleCount(rs.getInt(MINIDLEVHLCOUNT));
			} catch (Exception e) {
				area.setMinIdleVehicleCount(0);
			}
			try {
				area.setMaxIdleVehicleCount(rs.getInt(MAXIDLEVHLCOUNT));
			} catch (Exception e) {
				area.setMaxIdleVehicleCount(100);
			}
			try {
				area.setMinVehicleAssignAllowed(getBoolean(rs.getString(MINVHLASSIGNALLOWANCE)));
			} catch (Exception e) {
				area.setMinVehicleAssignAllowed(false);
			}
			try {
				area.setMinVehicleParkAllowed(getBoolean(rs.getString(MINVHLPARKALLOWANCE)));
			} catch (Exception e) {
				area.setMinVehicleParkAllowed(false);
			}
		}
	}
	
	/**
	 * 
	 * @param areaId
	 * @return
	 */
	public Area getArea(String areaId) {
		if (areaId == null || areaId.length() == 0) {
			return null;
		}
		return (Area)data.get(areaId);
	}
	
	private static final String SELECT_TARGETNODE_SQL = "SELECT N2.NODEID AS NODEID FROM NODE N1, NODE N2, LINK L WHERE L.ENABLED='TRUE' AND  L.FROMNODE = N1.NODEID AND L.TONODE = N2.NODEID AND N1.AREA != N2.AREA AND N2.AREA = ? AND N1.ENABLED='TRUE' AND N2.ENABLED='TRUE' AND N2.TYPE='COMMON'";
	/**
	 * 
	 */
	public ArrayList<String> getTargetNodeList(String areaId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		String nodeId;
		ArrayList<String> targetNodeList = new ArrayList<String>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_TARGETNODE_SQL);
			pstmt.setString(1, areaId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				nodeId = rs.getString(NODEID);
				if (nodeId != null && nodeId.length() > 0) {
					targetNodeList.add(nodeId);
				}
			}
			rs.close();
			pstmt.close();
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
		return targetNodeList;
	}
	
	
	public void initializeAreaData() {
		Area area = null;
		Set<String> areakeys = new HashSet<String>(data.keySet());
		for (String key : areakeys) {
			area = (Area)data.get(key);
			if (area != null) {
				area.initVehicleList();
			}
		}
	}
}
