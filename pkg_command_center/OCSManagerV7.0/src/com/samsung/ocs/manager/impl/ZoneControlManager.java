package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.ZoneControl;

/**
 * ZoneControlManager Class, OCS 3.1 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2012. 5. 24.
 * @version 3.1
 * 
 * Copyright 2012 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class ZoneControlManager extends AbstractManager {
	private static ZoneControlManager manager = null;
	private static final String VEHICLEZONE = "VEHICLEZONE";
	private static final String NODEZONE = "NODEZONE";
	private static final String ASSIGNALLOWANCE = "ASSIGNALLOWANCE";
	private static final String DRIVEALLOWANCE = "DRIVEALLOWANCE";
	private static final String YIELDALLOWANCE = "YIELDALLOWANCE";
	private static final String PARKALLOWANCE = "PARKALLOWANCE";
	private static final String COMEBACKZONEALLOWANCE = "COMEBACKZONEALLOWANCE";
	private static final String PENALTY = "PENALTY";
	
	private HashSet<String> assignAllowedSet;
	private HashSet<String> driveAllowedSet;
	private HashSet<String> yieldAllowedSet;
	private HashSet<String> parkAllowedSet;
	private HashSet<String> comebackZoneAllowedSet;
	private HashMap<String, Double> penaltyMap;
	
	/**
	 * Constructor of ZoneControlManager class.
	 */
	private ZoneControlManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(ZoneControl.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		assignAllowedSet = new HashSet<String>();
		driveAllowedSet = new HashSet<String>();
		yieldAllowedSet = new HashSet<String>();
		parkAllowedSet = new HashSet<String>();
		comebackZoneAllowedSet = new HashSet<String>();
		penaltyMap = new HashMap<String, Double>();
		isInitialized = true;
		init();
	}
	
	/**
	 * Constructor of ZoneControlManager class. (Singleton)
	 */
	public static synchronized ZoneControlManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new ZoneControlManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		initialize();
		isInitialized = true;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		return true;
	}
	
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB() {
		data.clear();
		init();
	}
	
	private static final String SELECT_SQL = "SELECT * FROM ZONECONTROL";
	/**
	 * 
	 * @return
	 */
	private boolean initialize() {
		boolean result = false;
		if (isInitialized) {
			Connection conn = null;
			ResultSet rs = null;
			PreparedStatement pstmt = null;
			String key = "";
			
			try {
				HashSet<String> tempAssignAllowedSet = new HashSet<String>();
				for (String assignAllowed : assignAllowedSet) {
					if (assignAllowed != null) {
						tempAssignAllowedSet.add(assignAllowed);
					}
				}
				
				HashSet<String> tempDriveAllowedSet = new HashSet<String>();
				for (String driveAllowed : driveAllowedSet) {
					if (driveAllowed != null) {
						tempDriveAllowedSet.add(driveAllowed);
					}
				}
				
				HashSet<String> tempYieldAllowedSet = new HashSet<String>();
				for (String yieldAllowed : yieldAllowedSet) {
					if (yieldAllowed != null) {
						tempYieldAllowedSet.add(yieldAllowed);
					}
				}
				
				HashSet<String> tempParkAllowedSet = new HashSet<String>();
				for (String parkAllowed : parkAllowedSet) {
					if (parkAllowed != null) {
						tempParkAllowedSet.add(parkAllowed);
					}
				}
				
				HashSet<String> tempComebackZoneAllowedSet = new HashSet<String>();
				for (String comebackZoneAllowed : comebackZoneAllowedSet) {
					if (comebackZoneAllowed != null) {
						tempComebackZoneAllowedSet.add(comebackZoneAllowed);
					}
				}
				
				Set<String> removePenaltyKeys = new HashSet<String>(penaltyMap.keySet());
				
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(SELECT_SQL);
				rs = pstmt.executeQuery();
				while (rs.next()) {
					key = rs.getString(VEHICLEZONE) + "_" + rs.getString(NODEZONE);
					if (isAssignAllowed(rs)) {
						assignAllowedSet.add(key);
						tempAssignAllowedSet.remove(key);
					}
					if (isDriveAllowed(rs)) {
						driveAllowedSet.add(key);
						tempDriveAllowedSet.remove(key);
					}
					if (isYieldAllowed(rs)) {
						yieldAllowedSet.add(key);
						tempYieldAllowedSet.remove(key);
					}
					if (isParkAllowed(rs)) {
						parkAllowedSet.add(key);
						tempParkAllowedSet.remove(key);
					}
					if (isComebackZoneAllowed(rs)) {
						comebackZoneAllowedSet.add(key);
						tempComebackZoneAllowedSet.remove(key);
					}
					penaltyMap.put(key, getPenalty(rs));
					removePenaltyKeys.remove(key);
				}
				for (String removeAssignAllowed : tempAssignAllowedSet) {
					if (removeAssignAllowed != null) {
						assignAllowedSet.remove(removeAssignAllowed);
					}
				}
				
				for (String removeDriveAllowed : tempDriveAllowedSet) {
					if (removeDriveAllowed != null) {
						driveAllowedSet.remove(removeDriveAllowed);
					}
				}
				
				for (String removeYieldAllowed : tempYieldAllowedSet) {
					if (removeYieldAllowed != null) {
						yieldAllowedSet.remove(removeYieldAllowed);
					}
				}
				
				for (String removeParkAllowed : tempParkAllowedSet) {
					if (removeParkAllowed != null) {
						parkAllowedSet.remove(removeParkAllowed);
					}
				}
				
				for (String removeComebackZoneAllowed : tempComebackZoneAllowedSet) {
					if (removeComebackZoneAllowed != null) {
						comebackZoneAllowedSet.remove(removeComebackZoneAllowed);
					}
				}
				
				for (String rmKey : removePenaltyKeys) {
					penaltyMap.remove(rmKey);
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
			} catch (Throwable t) {
				result = false;
				t.printStackTrace();
				writeExceptionLog(LOGFILENAME, t);
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
		}
		return result;
	}
	
	private boolean isAssignAllowed (ResultSet rs) {
		try {
			return getBoolean(rs.getString(ASSIGNALLOWANCE));
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isDriveAllowed (ResultSet rs) {
		try {
			return getBoolean(rs.getString(DRIVEALLOWANCE));
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isYieldAllowed (ResultSet rs) {
		try {
			return getBoolean(rs.getString(YIELDALLOWANCE));
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isParkAllowed (ResultSet rs) {
		try {
			return getBoolean(rs.getString(PARKALLOWANCE));
		} catch (Exception e) {
			return false;
		}
	}
	
	private boolean isComebackZoneAllowed (ResultSet rs) {
		try {
			return getBoolean(rs.getString(COMEBACKZONEALLOWANCE));
		} catch (Exception e) {
			return false;
		}
	}
	
	private double getPenalty (ResultSet rs) {
		try {
			return rs.getDouble(PENALTY);
		} catch (Exception e) {
			return 9999;
		}
	}
	
	public HashSet<String> getAssignAllowedSet() {
		return assignAllowedSet;
	}
	
	public HashSet<String> getDriveAllowedSet() {
		return driveAllowedSet;
	}
	
	public HashSet<String> getYieldAllowedSet() {
		return yieldAllowedSet;
	}
	
	public HashSet<String> getParkAllowedSet() {
		return parkAllowedSet;
	}
	
	public HashSet<String> getComebackZoneAllowedSet() {
		return comebackZoneAllowedSet;
	}
	
	public HashMap<String, Double> getPenaltyMap() {
		return penaltyMap;
	}
	
	private static final String SELECT_NODEZONE_SQL = "SELECT NODEZONE FROM ZONECONTROL WHERE VEHICLEZONE=? AND DRIVEALLOWANCE='TRUE' AND ASSIGNALLOWANCE='TRUE' AND COMEBACKZONEALLOWANCE='FALSE'";
	/**
	 * 
	 * @return
	 */
	public ArrayList<String> getAssignableNodeZoneList(String vehicleZone) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String zone = null;
		ArrayList<String> assignableNodeZoneList = new ArrayList<String>();
		assignableNodeZoneList.add(vehicleZone);
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_NODEZONE_SQL);
			pstmt.setString(1, vehicleZone);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				zone = getString(rs.getString(NODEZONE));
				if (assignableNodeZoneList.contains(zone) == false) {
					assignableNodeZoneList.add(zone);
				}
			}
		} catch (SQLException se) {
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
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
		return assignableNodeZoneList;
	}
}
