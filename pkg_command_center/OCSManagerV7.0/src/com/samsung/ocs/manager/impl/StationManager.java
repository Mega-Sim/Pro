package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.Station;

/**
 * Alarm Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 2. 1.
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

public class StationManager extends AbstractManager {
	
	private static StationManager manager;
	
	private static final String STATIONID = "STATIONID";
	private static final String TAGTYPE = "TAGTYPE";
	private static final String PARENTNODE = "PARENTNODE";
	private static final String NEXTNODE = "NEXTNODE";
	private static final String OFFSET = "OFFSET";
	
	// 2013.10.22 by KYK
	private OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
	private int dataRevision = ocsInfoManager.getStationDataRevision();
	
	private StationManager(Class<?> vOType, DBAccessManager dbAccessManager, 
			boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval);
		LOGFILENAME = this.getClass().getName();
		if (vOType != null && vOType.getClass().isInstance(Station.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			} 
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	public static synchronized StationManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, 
			boolean initializeAtStart, boolean makeManagerThread, long interval){
		
		if (manager == null) {
			manager = new StationManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, interval);
		}
		return manager;
	}

	@Override
	protected void init() {
		initialize();
		isInitialized = true;
	}

	private static final String INITIALIZE_SQL = "SELECT * FROM STATION";

	private boolean initialize() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			pstmt = conn.prepareStatement(INITIALIZE_SQL);
			rs = pstmt.executeQuery();
			Station station = null;
			String stationId = null;
			if (rs != null) {
				while (rs.next()) {
					stationId = rs.getString(STATIONID);
					station = (Station) data.get(stationId);
					if (station == null) {
						station = (Station) vOType.newInstance();
						data.put(stationId, station);
					}
					setStation(station, rs);					
				}
			}
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (InstantiationException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (IllegalAccessException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * 2013.10.22 by KYK
	 * @return
	 */
	private boolean updateStationDataFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INITIALIZE_SQL);
			rs = pstmt.executeQuery();
			String stationId = null;
			Station station = null;
			while (rs.next()) {
				stationId = rs.getString(STATIONID);
				station = (Station) data.get(stationId);
				if (station != null) {
					setStationValue(station, rs);
				}
			}
			result = true;
		} catch (SQLException se){
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}		
		return result;
	}

	/**
	 * 2013.10.22 by KYK
	 * @return
	 */
	private boolean hasRevisionChanged() {
		if (dataRevision != ocsInfoManager.getStationDataRevision()) {
			return true;
		}
		return false;
	}
	
	private void setDataRevision(int dataRevision) {
		this.dataRevision = dataRevision;
	}

	/**
	 * 2013.10.22 by KYK
	 * @param station
	 * @param rs
	 * @throws SQLException
	 */
	private void setStationValue(Station station, ResultSet rs) throws SQLException {
		station.setTagType(rs.getInt(TAGTYPE));
		station.setOffset(rs.getInt(OFFSET));
	}

	private void setStation(Station station, ResultSet rs) throws SQLException {
		station.setStationId((getString(rs.getString(STATIONID))));
		station.setTagType(rs.getInt(TAGTYPE));
		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		if (nodeManager != null) {
			station.setParentNode(nodeManager.getNode(getString(rs.getString(PARENTNODE))));
			station.setNextNode(nodeManager.getNode(getString(rs.getString(NEXTNODE))));
		}
		station.setOffset(rs.getInt(OFFSET));
	}

	public void initializeFromDB() {
		data.clear();
		init();
	}

	@Override
	protected boolean updateFromDB() {
		if (isInitialized) {
			// 2013.10.22 by KYK
			if (hasRevisionChanged()) {
				setDataRevision(ocsInfoManager.getTeachingDataRevision());
				updateStationDataFromDB();
			}
		}
		return true;
	}

	@Override
	protected boolean updateToDB() {
		return true;
	}
	
	private static final String UPDATE_STATIONOFFSET_SQL = "UPDATE STATION SET OFFSET=? WHERE STATIONID=?";
	
	/**
	 * 2013.04.22 by MYM : Station Offset 업데이트
	 * @param stationId
	 * @param locus
	 * @return
	 */
	public boolean updateStationOffsetToDB(Station station, int offset) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_STATIONOFFSET_SQL);
			pstmt.setInt(1, offset);
			pstmt.setString(2, station.getStationId());
			pstmt.executeUpdate();
			
			station.setOffset(offset);
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
	public boolean isValidStation(String stationId) {
		if (stationId != null && stationId.length() > 0) {
			Station station = getStation(stationId);
			if (station != null && station.getOffset() >= 0) {
				return true;
			}
		}
		return false;
	}
	
	public Station getStation(String stationId) {
		if (stationId != null) {
			return (Station) data.get(stationId);			
		}
		return null;
	}
	
//	private static final String SELECT_UNREADY_SQL = "SELECT * FROM STATION WHERE OFFSET < 0";
//	
//	/**
//	 * 2013.07.12 by KYK :
//	 * @return
//	 */
//	private boolean updateUnreadyStationFromDB() {
//		if (unreadyStationList == null) {
//			return false;
//		}
//		ArrayList<String> tempList = new ArrayList<String>();
//		
//		boolean result = false;
//		String stationId = null;
//		Connection conn = null;
//		ResultSet rs = null;
//		PreparedStatement pstmt = null;
//		
//		try {
//			conn = dbAccessManager.getConnection();
//			pstmt = conn.prepareStatement(SELECT_UNREADY_SQL);
//			rs = pstmt.executeQuery();			
//			if (rs != null) {
//				while (rs.next()) {
//					stationId = getString(rs.getString(STATIONID));
//					if (unreadyStationList.contains(stationId) == false) {
//						unreadyStationList.add(stationId);
//					}
//					tempList.add(stationId);
//				}
//			}
//			for (int i = unreadyStationList.size(); i > 0; i--) {
//				stationId = unreadyStationList.get(i - 1);
//				if (tempList.contains(stationId) == false) {
//					unreadyStationList.remove(stationId);
//				}
//			}
//			result = true;
//		} catch (SQLException e) {
//			result = false;
//			e.printStackTrace();
//			writeExceptionLog(LOGFILENAME, e);
//		} finally {
//			if (rs != null) {
//				try {
//					rs.close();
//				} catch (SQLException e) {}
//				rs = null;
//			}
//			if (pstmt != null) {
//				try {
//					pstmt.close();
//				} catch (SQLException e) {}
//				pstmt = null;
//			}
//		}		
//		return result;
//	}
//	
//	/**
//	 * 2013.07.12 by KYK
//	 * @return
//	 */
//	public ArrayList<String> getUnreadyStationList() {
//		return unreadyStationList;
//	}

}
