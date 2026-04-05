package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.TRAFFIC_UPDATE_RULE;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.Traffic;

/**
 * DetourManager Class, OCS 3.1 for Unified FAB
 * 
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * 
 * @date   2015. 5. 27.
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
public class TrafficManager extends AbstractManager {
	private static TrafficManager manager = null;
	
	private static final String TRAFFICID = "TRAFFICID";
	private static final String ENABLED = "ENABLED";
	private static final String TRUE = "TRUE";
	private static final String TYPE = "TYPE";
	private static final String SECTIONLIST = "SECTIONLIST";
	private static final String PULL_GREEN_LIMIT = "PULL_GREENLIMIT";
	private static final String PULL_BLUE_LIMIT = "PULL_BLUELIMIT";
	private static final String PULL_GREEN_RATIO = "PULL_GREENRATIO";
	private static final String PULL_BLUE_RATIO = "PULL_BLUERATIO";
	private static final String PUSH_TRAFFICWEIGHT = "PUSH_TRAFFICWEIGHT";
	private static final String PUSH_YELLOW_LIMIT = "PUSH_YELLOWLIMIT";
	private static final String PUSH_RED_LIMIT = "PUSH_REDLIMIT";
	private static final String PUSH_SECOND_TRAFFICCOST_RATIO = "PUSH_SECONDTRAFFICCOSTRATIO";
	private static final String MIN_PRIORITY = "MIN_PRIORITY";
	private static final String MAX_PRIORITY = "MAX_PRIORITY";
	
	private Vector<String> invalidTrafficList;
	
	private SectionManager sectionManager;
	private OCSInfoManager ocsInfoManager;
	
	// 2015.09.21 by MYM : RuntimeUpdate시 Traffic정보 재 갱신
	private boolean isRuntimeUpdateRequested = false;
	public void requestRuntimeUpdate() {
		this.isRuntimeUpdateRequested = true;
	}

	/**
	 * Constructor of TrafficManager class.
	 */
	private TrafficManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		invalidTrafficList = new Vector<String>();
		
		if (vOType != null && vOType.getClass().isInstance(Traffic.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		}
		sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
	}
	
	/**
	 * Constructor of AreaManager class. (Singleton)
	 */
	public static synchronized TrafficManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new TrafficManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	@Override
	protected void init() {
	}

	private static final String SELECT_TRAFFIC_SQL = "SELECT * FROM TRAFFIC";
	
	@Override
	protected boolean updateFromDB() {
		if (ocsInfoManager.isTrafficUpdateUsed()) {
			// 2016.12.23 by KBS : SectionManager 초기화 전에 section 정보 참조로 traffic 설정 disable 발생 개선
			if (sectionManager.isRuntimeUpdateRequested() == false) {
				// step1. DB에서 Traffic 정보 가져오기
				updateTrafficInfo();
				// step2. Update Traffic Cost
				updateTrafficCost();
			}
		} else {
			clearTrafficCost();
		}
		
		// 2015.09.21 by MYM : RuntimeUpdate시 Traffic정보 재 갱신
		if (isRuntimeUpdateRequested) {
			clearTrafficCost();
			isRuntimeUpdateRequested = false;
		}
		
		return true;
	}
	
	@Override
	protected boolean updateToDB() {
		updateTrafficToDB();
		updateInvalidTrafficToDB();
		
		return true;
	}
	
	private boolean updateTrafficInfo() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		String trafficId;
		Traffic traffic;
		boolean checkInvalid = false;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_TRAFFIC_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				trafficId = rs.getString(TRAFFICID);
				checkInvalid = getString(rs.getString(ENABLED)).equals(TRUE);
				traffic = (Traffic) data.get(trafficId);
				if (traffic == null) {
					traffic = (Traffic) vOType.newInstance();
					traffic.setTrafficId(trafficId);
					data.put(trafficId, traffic);
					checkInvalid = true;
				}
				if (setTraffic(traffic, rs) == false && checkInvalid) {
					invalidTrafficList.add(trafficId);
				}
				removeKeys.remove(trafficId);
			}
			
			for (String rmKey : removeKeys) {
				traffic = (Traffic) data.remove(rmKey);
				if (traffic != null) {
					traffic.clear();
				}
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

	private boolean setTraffic(Traffic traffic, ResultSet rs) throws SQLException {
		if (traffic != null) {
			String[] sectionIdList = getString(rs.getString(SECTIONLIST)).split(",");
			if (sectionIdList.length > 0) {
				ArrayList<Section> addSectionSet = new ArrayList<Section>();
				
				// 신규 설정된 SectionId 검증
				for (String sectionId : sectionIdList) {
					Section section = (Section) sectionManager.getData().get(sectionId);
					if (section != null) {
						addSectionSet.add(section);
					} else {
						return false;
					}
				}
				
				// 파라미터 값 설정
				traffic.setType(TRAFFIC_UPDATE_RULE.toTrafficUpdateRule(getString(rs.getString(TYPE))));
				traffic.setEnabled(getString(rs.getString(ENABLED)).equals(TRUE));
				traffic.setPushTrafficWeight(rs.getDouble(PUSH_TRAFFICWEIGHT));
				traffic.setPushYellowLimit(rs.getInt(PUSH_YELLOW_LIMIT));
				traffic.setPushRedLimit(rs.getInt(PUSH_RED_LIMIT));
				traffic.setPullGreenLimit(rs.getInt(PULL_GREEN_LIMIT));
				traffic.setPullBlueLimit(rs.getInt(PULL_BLUE_LIMIT));
				traffic.setPullGreenRatio(rs.getDouble(PULL_GREEN_RATIO));
				traffic.setPullBlueRatio(rs.getDouble(PULL_BLUE_RATIO));
				traffic.setSecondTrafficCostRatio(rs.getDouble(PUSH_SECOND_TRAFFICCOST_RATIO));
				traffic.setMinPriority(rs.getInt(MIN_PRIORITY));
				traffic.setMaxPriority(rs.getInt(MAX_PRIORITY));
				
				// 기존과 동일 Section 구성 체크 및 등록
				String addSectionInfo = addSectionSet.toString();
				String preSectionInfo = traffic.getSectionList().toString();
				if (addSectionInfo.equals(preSectionInfo) == false) {
					traffic.clear();
					traffic.setSection(addSectionSet);
				}
				
				return true;
			} else {
				traffic.clear();
			}
		}
		return false;
	}
	
	private void updateTrafficCost() {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			TRAFFIC_UPDATE_RULE trafficUpdateRule = ocsInfoManager.getTrafficUpdateRule();
			for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
				try {
					Traffic traffic = (Traffic) e.nextElement();
					if (traffic != null) {
						if (traffic.isEnabled()) {
							// trafficUpdateRule(OCSINFO) 값
							// - HYBRID : PULL, PUSH 모두 동작
							// - PULL   : PULL만 동작
							// - PUSH   : PUSH만 동작
							if (trafficUpdateRule == TRAFFIC_UPDATE_RULE.HYBRID
									|| traffic.getType() == trafficUpdateRule) {
								traffic.updateTrafficCost();
							} else {
								traffic.resetTrafficCost();
							}
						} else {
							traffic.resetTrafficCost();
						}
					}
				} catch (Exception exception) {
					writeExceptionLog(LOGFILENAME, exception);
				}
			}
		}
	}
	
	private void clearTrafficCost() {
		for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
			Traffic traffic = (Traffic) e.nextElement();
			if (traffic != null) {
				traffic.clear();
			}
		}
	}
	
	private static final String UPDATE_TRAFFICINFO_SQL = "UPDATE TRAFFIC SET PUSH_TRAFFICCOST=?, PULL_TRAFFICRATIO=?, CURRENTVEHICLECOUNT=?, EXPECTEDVEHICLECOUNT=? WHERE TRAFFICID=?";
	
	protected boolean updateTrafficToDB() {
		if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
			return true;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_TRAFFICINFO_SQL);
			
			int count = 0;
			for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
				Traffic traffic = (Traffic) e.nextElement();
				if (traffic == null) {
					continue;
				}
				pstmt.setDouble(1, traffic.getPushTrafficCost());
				pstmt.setDouble(2, traffic.getPullTrafficRatio());
				pstmt.setInt(3, traffic.getCurrentVehicleCount());
				pstmt.setInt(4, traffic.getExpectedVehicleCount());
				pstmt.setString(5, traffic.getTrafficId());
				pstmt.addBatch();
				count++;

				if (count > 1000) {
					pstmt.executeBatch();
					count = 0;
				}
			}
			
			if (count > 0) {
				pstmt.executeBatch();
			}
			conn.commit();
			conn.setAutoCommit(true);
			result = true;
		} catch (SQLException se) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
	
	private static final String UPDATE_INVALID_TRAFFIC_SQL = "UPDATE TRAFFIC SET ENABLED='FALSE', PUSH_TRAFFICCOST=0, CURRENTVEHICLECOUNT=0, EXPECTEDVEHICLECOUNT=0 WHERE TRAFFICID=?";
	
	protected boolean updateInvalidTrafficToDB() {
		if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
			return true;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_INVALID_TRAFFIC_SQL);
			
			int count = 0;
			for (String trafficId : invalidTrafficList) {
				pstmt.setString(1, trafficId);
				pstmt.addBatch();
				count++;
				
				if (count > 1000) {
					pstmt.executeBatch();
					count = 0;
				}
				
				writeExceptionLog(LOGFILENAME, new StringBuilder("Invalid Traffic : ").append(trafficId).append(" is Disabled").toString());
			}
			invalidTrafficList.clear();
			
			if (count > 0) {
				pstmt.executeBatch();
			}
			conn.commit();
			conn.setAutoCommit(true);
			result = true;
		} catch (SQLException se) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
}
