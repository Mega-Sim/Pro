package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant.TOUR_TYPE;
import com.samsung.ocs.manager.impl.model.Tour;

/**
 * TourManager Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 3. 7.
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

public class TourManager extends AbstractManager {
	private static TourManager manager = null;
	private static final String TOURID = "TOURID";
	private static final String TYPE = "TYPE";
	private static final String TOUR = "TOUR";
	
	/**
	 * Constructor of TourManager class.
	 */
	private TourManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(Tour.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of TourManager class. (Singleton)
	 */
	public static synchronized TourManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new TourManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
	
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB() {
		data.clear();
		init();
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		return true;
	}
	
	private static final String SELECT_SQL = "SELECT * FROM TOUR";
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
		boolean result = false;
		Set<String> removeKeys = new HashSet<String>(data.keySet());

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			
			String tourId = null;
			Tour tour = null;
			while (rs.next()) {
				tourId = rs.getString(TOURID);
				tour = (Tour) data.get(tourId);
				if (tour == null) {
					tour = (Tour) vOType.newInstance();
					tour.setTourId(tourId);
					data.put(tourId, tour);
				}
				setTour(tour, rs);
				removeKeys.remove(tourId);
			}
			for (String rmKey : removeKeys) {
				if (rmKey != null) {
					tour = (Tour) data.remove(rmKey);
				}
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
		} catch (NullPointerException e) {			
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		} catch (Exception e) {			
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
	 * @param tour
	 * @param rs
	 * @exception SQLException
	 */
	private void setTour(Tour tour, ResultSet rs ) throws SQLException {
		if (tour != null && rs != null) {
			tour.setTypeString(getString(rs.getString(TYPE)));
			tour.setType(getTourType(getString(rs.getString(TYPE))));
			tour.setTour(getString(rs.getString(TOUR)));
		}
	}
	
	private TOUR_TYPE getTourType(String typeString) {
		if (typeString != null && typeString.length() > 0) {
//				case 'N':	// Node (ex. '100000,100001,100002,100003')
//				case 'P':	// Pair (ex. '100000:100001,100002:100003')
//				case 'C':	// CarrierLoc (ex. 'ABCD_B1,ABCD_B2')
			return TOUR_TYPE.toTourType(typeString.charAt(0));
		}
		return TOUR_TYPE.UNDEFINED;			// Undefined
	}
	
	public boolean isValidTour(String tourId) {
		if (data.containsKey(tourId)) {
			return true;
		}
		return false;
	}
	
	public Tour getTour(String tourId) {
		if (tourId != null) {
			return (Tour)data.get(tourId);
		}
		return null;
	}
}
