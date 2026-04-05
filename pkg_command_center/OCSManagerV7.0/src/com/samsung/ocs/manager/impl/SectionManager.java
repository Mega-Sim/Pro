package com.samsung.ocs.manager.impl;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.manager.impl.model.Link;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Section;

/**
 * SectionManager Class, OCS 3.0 for Unified FAB
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

public class SectionManager extends AbstractManager {
	private static SectionManager manager = null;
	private static final String LINKID = "LINKID";
	private static final String TYPE = "TYPE";
	private static final String TONODE = "TONODE";
	private static final String FROMNODE = "FROMNODE";
	private static final String DIRECTION = "DIRECTION";
	private static final String ENABLED = "ENABLED";
	private static final String VEHICLESPEED = "VEHICLESPEED";
	private static final String DISTANCE = "DISTANCE";
	private static final String SECTIONID = "SECTIONID";
	private static final String PREVIOUSSECTION = "PREVIOUSSECTION";
	private static final String DETOURPENALTY = "DETOURPENALTY";
	private static final String STEERPOSITION = "STEERPOSITION";
	private static final String TRUE = "TRUE";
	
	// 2014.02.03 by MYM : Disabled Link 처리
	private ConcurrentHashMap<String, Link> linkData;
	private boolean isRuntimeUpdateRequested = false;
	private boolean isRuntimeUpdatable = false;
	private Vector<Link> changedLinkList = new Vector<Link>();
	private Vector<Link> disabledLinkList = new Vector<Link>();
	private HashSet<Section> disabledSectionList = new HashSet<Section>();
	
	// 2014.10.13 by MYM : 장애 지역 우회 기능
	private boolean requestClearDetour = false;
	private boolean requestClearDetourPortService = false;
	private DETOUR_REASON requestClearDetourReason = DETOUR_REASON.NONE;
	// 2015.11.10 by MYM : DetourUserSection 설정 방식 변경
	private HashSet<Section> lastDetourUserSectionSet = new HashSet<Section>();
	private boolean isOperation = false;
	private long lastDetourMismatchCheckTime = System.currentTimeMillis(); 
	
	/**
	 * Constructor of SectionManager class.
	 */
	private SectionManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(Section.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of SectionManager class. (Singleton)
	 */
	public static synchronized SectionManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new SectionManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
	@Override
	protected boolean updateFromDB() {
		// 2014.02.03 by MYM : Disabled Link 처리
		if (isRuntimeUpdateRequested == false) {
			isRuntimeUpdatable = false;
			if (isInitialized) {
				updateChangedLinkInfo();

				// 2015.10.31 by MYM : 장애 지역 우회 기능
				updateDetourUserSectionFromDB();
				
				// 2015.08.08 by MYM : updateChangedLinkInfo에서 여기로 위치 변경
				checkDetourUsed();
				
				// 2015.08.08 by MYM : 장애 회피 오류 자동 체크
				checkDetourMismatch();
			} else {
				// 어떤 경우?? 예방 차원
				init();
			}
		} 
		isRuntimeUpdatable = true;
		return true;
	}
	
	// 2011.11.09 by PMM
	public void initializeFromDB() {
		isInitialized = false;
		// 2014.02.03 by MYM : Disabled Link 처리 - 주석처리
//		data.clear();
//		init();
		
		// 2014.02.03 by MYM : Disabled Link 처리
		isRuntimeUpdateRequested = true;
		int count = 0;
		while (true) {
			if (isRuntimeUpdatable || count > 200) {
				break;
			}
			count++;
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
				break;
			}
		}
		
		data.clear();
		init();
		isRuntimeUpdateRequested = false;
	}
	
	// 2011.11.07 by PMM
	// 중복 link 예방
//	private static final String selectSql = "SELECT * FROM LINK";
	private static final String SELECT_SQL = "SELECT * FROM LINK WHERE LINKID IN (SELECT MIN(LINKID) AS LINKID FROM LINK GROUP BY FROMNODE, TONODE) ORDER BY LINKID";
	/**
	 * 
	 */
	private void initialize() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		// 2011.11.09 by PMM
//		Set<String> removeKeys = new HashSet<String>(data.keySet());
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				// 2014.10.13 by MYM : 장애 지역 우회 기능(Link → Section 로 변환)
				String sectionId = getString(rs.getString(LINKID)).replace("Link", "Section");
				Section section = (Section)data.get(sectionId);
				if (section == null) {
					section = (Section) vOType.newInstance();
					data.put(sectionId, section);
				}
				setSecitonInfo(section, rs);
//				removeKeys.remove(sectionId);
			}
//			for (String rmKey : removeKeys) {
//				data.remove(rmKey);
//			}
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
		
		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);

		if (nodeManager == null) {
			return;
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("\n").append("Initialize...").append("\n");		
		long processTime = System.currentTimeMillis();
		
		boolean isNearByDrive = false;
		boolean isMapDistanceUsed = false;
		boolean isMapVehicleSpeedUsed = false;
		double vehicleLineSpeed = 2500;
		double vehicleCurveSpeed = 1000;
		
		if (ocsInfoManager != null) {
			isNearByDrive = ocsInfoManager.isNearByDrive();
			isMapDistanceUsed = ocsInfoManager.isMapDistanceUsed();
			isMapVehicleSpeedUsed = ocsInfoManager.isMapVehicleSpeedUsed();
			vehicleLineSpeed = ocsInfoManager.getVehicleLineSpeed();
			vehicleCurveSpeed = ocsInfoManager.getVehicleCurveSpeed();
		}
		
		// 2014.02.03 by MYM : Disabled Link 처리
		if (linkData != null) {
			linkData.clear();
		}
		ConcurrentHashMap<String, Link> linkData = new ConcurrentHashMap<String, Link>();
		this.linkData = linkData;
		
		// Section(MoveInTime, Section) 설정
		for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
			Section section = (Section) e.nextElement();
			// 2012.03.02 by PMM
			// [NotNullCheck]
			if (section != null) {
				Node fromNode = nodeManager.getNode(section.getLastNodeId());
				Node toNode = nodeManager.getNode(section.getFirstNodeId());
				if (fromNode != null && toNode != null) {
					// 2015.07.08 by KYK,JYH 위치이동, 두 값 link 에 추가
					double speed = section.getSectionSpeed();
					// 2011. 11.02 by MYM : DB(Link Table)에서 Distance 값을 가져옴. 0이면 거리 계산하여 사용
					double distance = section.getDistance(); 
					if (distance <= 0 || (isNearByDrive == false && isMapDistanceUsed == false)) {
						distance = fromNode.getLength(toNode);
					}
					if (speed <= 0 || (isNearByDrive == false && isMapVehicleSpeedUsed == false)) {
						if (OcsConstant.LINE.equals(section.getType())) {
							speed = vehicleLineSpeed;
						} else {
							speed = vehicleCurveSpeed;
						}
					}
					// 2014.02.03 by MYM : Disabled Link 처리
					Link link = new Link();
					link.setLinkId(section.getSectionId().replace("Section", "Link"));
					link.setType(section.getType());
					link.setFromNode(fromNode);
					link.setToNode(toNode);
					link.setEnabled(section.getEnabled());
					link.setSteerPosition(section.getSteerPosition());
					// 추가
					link.setSpeed(speed);
					link.setDistance(distance);
					
					linkData.put(link.getLinkId(), link);
					section.addLink(link);
					
					fromNode.addSection(section);
					section.addNode(fromNode);
					toNode.addSection(section);
					section.addNode(toNode);
					section.setTrafficPenalty(toNode.getTrafficPenalty());
					
					// 2014.10.17 by MYM : 장애 지역 우회 기능
					if (fromNode.getHid() != null) {
						fromNode.getHid().addSection(section);
					}
					if (toNode.getHid() != null) {
						toNode.getHid().addSection(section);
					}
					
					double time = 0.0;
					time = distance / speed;
					toNode.setMoveInTime(time, distance, fromNode, true);
					fromNode.setMoveInTime(time, distance, toNode, false);
					section.setDistance(distance);
					section.setMoveInSectionTime(time);
				} else {
					StringBuilder message = new StringBuilder();
					message.append("initialize() - node is null. ");
					message.append("FromNodeId:").append(section.getLastNodeId());
					message.append(", ToNodeId:").append(section.getFirstNodeId());
					writeExceptionLog(LOGFILENAME, message.toString());
				}
			} else {
				writeExceptionLog(LOGFILENAME, "initialize() - section is null.");
			}
		}

		// Section 구성
		for (Enumeration<Object> e = nodeManager.getData().elements(); e.hasMoreElements();) {
			Node node = (Node) e.nextElement();
			// 2012.03.02 by PMM
			// [NotNullCheck]
			if (node != null) {
				if (node.getSectionCount() == 2) {
					Section firstSection = node.getSection(0);
					Section secondSection = node.getSection(1);
					Section section = firstSection.mergeSection(secondSection);
					if (section != null) {
						removeSection(section.getSectionId());
					}
				} else if (node.getSectionCount() > 2) {
					node.checkMergeType();
				}
			} else {
				writeExceptionLog(LOGFILENAME, "initialize() - node is null.");
			}
		}
		sb.append("  Make Section : ").append((System.currentTimeMillis() - processTime)).append(" ms").append("\n"); 
		writeLog(LOGFILENAME, sb.toString());
		
		// 2015.01.05 by MYM : 장애 지역 우회 기능
		setConnectedSectionInfo();
	}
	
	/**
	 * 2015.01.05 by MYM : 장애 지역 우회 기능
	 * 
	 */
	private void setConnectedSectionInfo() {
		StringBuffer log = new StringBuffer();
		log.append("Section Count : ").append(data.size()).append("\n");
		for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
			Section section = (Section) e.nextElement();
			Node node = section.getFirstNode();
			// Previous Section 정보 설정
			HashSet<Section> detourSectionSet = new HashSet<Section>();
			if (node.isConverge() && node.isDiverge() == false) {
				// 2015.11.10 by MYM : Section의 이전 진입 가능한 모든 Section을 가지고 있는 것으로 변경 
				//										 (Recursive Call 미사용 하기 위함, Manual Vehicle 위치 이동시 장애 구간 업데이트시 중복 체크시도 사용)
				//                     장애 발생시 해당 Section에서 모든 진입 구간을 미리 확인 후 순차적으로 Section Disabled 적용하도록 로직 변경
//				Iterator<Section> iterator = node.getSection().iterator();
//				while (iterator.hasNext()) {
//					Section prevSection = iterator.next();
//					if (section != prevSection && node.equals(prevSection.getLastNode())) {
//						section.addPreviousSectionSet(prevSection);
//					}
//				}
				addPreviousSectionSet(section, node, detourSectionSet);
				section.addDetourSectionSet(detourSectionSet);
			}
			
			// 설정된 정보를 로그로 확인
			log.append(" [").append(section.getSectionId()).append("]	");
			if (section.getDetourSectionSet().size() > 0) {
				log.append(section.getDetourSectionSet()).append(",	");
			}
			log.append(section.getSectionInfo()).append("\n");
		}
		
		registerSectionInfoToFile(log.toString());		
	}
	
	/**
	 * 2015.11.10 by MYM : 진입하는 모든 Section을 찾아 Section에 설정
	 * @param section
	 * @param node
	 * @param previousSectionSet
	 */
	private void addPreviousSectionSet(Section section, Node node, HashSet<Section> previousSectionSet) {
		if (node.isDiverge()) {
			return;
		}
		
		Iterator<Section> iterator = node.getSection().iterator();
		while (iterator.hasNext()) {
			Section prevSection = iterator.next();
			if (section != prevSection && node.equals(prevSection.getLastNode())) {
				addPreviousSectionSet(prevSection, prevSection.getFirstNode(), previousSectionSet);
				previousSectionSet.add(prevSection);
			}
		}
	}
	
	/**
	 * 2015.02.05 by MYM : 장애 지역 우회 기능
	 */
	public void checkChangedSectionEnabled() {
		for (Link link : disabledLinkList) {
			link.setAbnormalSection();
		}
	}
	
	private static final String SELECT_CHANGED_SQL = "SELECT * FROM LINK WHERE ENABLED = 'FALSE'";
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 */
	public boolean updateChangedLinkInfo() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<Link> lastDisabledLinkList = new Vector<Link>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_CHANGED_SQL);
			rs = pstmt.executeQuery();
			String key;
			boolean enabled;
			while (rs.next()) {
				if (isRuntimeUpdateRequested) {
					return false;
				}
				key = getString(rs.getString(LINKID));
				enabled = getBoolean(rs.getString(ENABLED));
				Link link = linkData.get(key);				
				if (link != null && enabled == false) {
					link.setEnabled(enabled);
					lastDisabledLinkList.add(link);
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
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
			return result;
		}
		
		if (result) {
			// disable -> enable
			for (int i = disabledLinkList.size() - 1; i >= 0; i--) {
				Link link = disabledLinkList.get(i);
				if (lastDisabledLinkList.remove(link) == false) {
					link.setEnabled(true);
					// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
					if (isOperation) {
						link.releaseAbnormalSection();
					}
					disabledLinkList.remove(i);
					
					if (changedLinkList.contains(link) == false) {
						changedLinkList.add(link);
					}
				}			
			}
			
			// enable -> disable
			for (int i = 0; i < lastDisabledLinkList.size(); i++) {
				Link link = lastDisabledLinkList.get(i);
				if (link != null) {
					// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
					if (isOperation) {
						link.setAbnormalSection();
						link.checkRepathSearch();
					}
					disabledLinkList.add(link);
				}
			}
		}
		return result;
	}
	
	private static final String SELECT_DETOUR_USER_SECTION_SQL = "SELECT * FROM DETOURUSERSECTION WHERE ENABLED='TRUE'";
	/**
	 * 2015.02.05 by MYM : 장애 지역 우회 기능
	 */
	private boolean updateDetourUserSectionFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_DETOUR_USER_SECTION_SQL);
			rs = pstmt.executeQuery();
			
			// 2015.11.10 by MYM : DetourUserSection 설정 방식 변경
			HashSet<Section> tmpLastDetourUserSectionSet = new HashSet<Section>(lastDetourUserSectionSet);
			while (rs.next()) {
				if (isRuntimeUpdateRequested) {
					return false;
				}
				String sectionId = getString(rs.getString(SECTIONID));
				Section section = (Section) data.get(sectionId);
				if (section == null) {
					continue;
				}
				String prevSectionIds = getString(rs.getString(PREVIOUSSECTION));
				boolean enabled = getBoolean(rs.getString(ENABLED));
				if (enabled) {
					String[] prevSectionIdList = prevSectionIds.split(",");
					HashSet<Section> detourUserSectionSet = new HashSet<Section>();
					boolean isValid = true;
					for (int i = 0; i < prevSectionIdList.length; i++) {
						Section detourSection = (Section) data.get(prevSectionIdList[i]);
						if (detourSection != null) {
							detourUserSectionSet.add(detourSection);
						} else {
							isValid = false;
							break;
						}
					}
					if (isValid) {
						section.addDetourUserSectionSet(detourUserSectionSet);
					} else {
						section.clearDetourUserSectionSet();
					}
				} else {
					section.clearDetourUserSectionSet();
				}
				lastDetourUserSectionSet.add(section);
				tmpLastDetourUserSectionSet.remove(section);
			}
			
			for (Section section : tmpLastDetourUserSectionSet) {
				section.clearDetourUserSectionSet();
				lastDetourUserSectionSet.remove(section);
			}
			
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
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
			return result;
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param section
	 * @param rs
	 * @exception SQLException
	 */
	private void setSecitonInfo(Section section, ResultSet rs) throws SQLException {
		if (section != null && rs != null) {
			section.setSectionId(getString(rs.getString(LINKID)).replace("Link", "Section"));
			section.setType(rs.getString(TYPE));
			section.setFirstNodeId(rs.getString(TONODE));
			section.setLastNodeId(rs.getString(FROMNODE));
			section.setDirection(rs.getString(DIRECTION));
			// 16.12.15 LSH: Link Disable 후, 모듈 재시작 or RuntimeUpdate 시, Section Disable 되는 문제 수정
//			section.setEnabled(getBoolean(rs.getString(ENABLED)));
			section.setEnabled(true);
			section.setSectionSpeed(rs.getInt(VEHICLESPEED));
			section.setDistance(rs.getInt(DISTANCE));
			String steerPos = getString(rs.getString(STEERPOSITION));
			if (steerPos.length() > 0) {
				section.setSteerPosition(steerPos.charAt(0));
			}
		} else {
			writeExceptionLog(LOGFILENAME, "setSecitonInfo(Section section, ResultSet rs) - one of parameters is null.");
		}
	}
	
	private static final String SELECT_SECTION_SQL = "SELECT SECTIONID, ENABLED, DETOURPENALTY FROM SECTION WHERE ENABLED='FALSE'";
	/**
	 * 2015.02.24 by MYM : 장애 지역 우회 기능
	 */
	public void updateSectionFromDBForJobAssign() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SECTION_SQL);
			rs = pstmt.executeQuery();
			
			HashSet<Section> tmpDisabledSectionList = new HashSet<Section>(disabledSectionList);  
			while (rs.next()) {
				String sectionId = getString(rs.getString(SECTIONID));
				Section section = (Section)data.get(sectionId);
				if (section != null) {
					section.setEnabled(getString(rs.getString(ENABLED)).equals(TRUE));
					section.setDetourPenalty(rs.getDouble(DETOURPENALTY));
					tmpDisabledSectionList.remove(section);
					disabledSectionList.add(section);
				}
			}
			
			for (Section section : tmpDisabledSectionList) {
				disabledSectionList.remove(section);
				section.setEnabled(true);
				section.setDetourPenalty(0);
			}
			tmpDisabledSectionList.clear();
			
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
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
	}
	
	private void registerSectionInfoToFile(String log) {
		if (log.length() <= 0) {
			return;
		}

		File file;
		FileWriter out = null;
		try {
			file = new File("SectionData.txt");
			out = new FileWriter(file, false);
			out.write(log);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			try {
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
				writeExceptionLog(LOGFILENAME, e);
			}
		}
	}
	
	/**
	 * 
	 * @param sectionId
	 */
	public void removeSection(String sectionId) {
		data.remove(sectionId);
	}
	
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 */
	public Vector<Link> getChangedLinkList() {
		return changedLinkList;
	}
	
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 */
	public void resetChangedLinkList() {
		changedLinkList.clear();
	}
	
	/**
	 * 2015.02.10 by MYM : 장애 지역 우회 기능
	 */
	private void checkDetourUsed() {
		if (requestClearDetour) {
			Iterator<Object> iter = data.values().iterator();
			while (iter.hasNext()) {
				Section section = (Section) iter.next();
				if (section != null) {
					section.clearAllAbnormalItem(requestClearDetourReason);
				}
			}
		} else if (requestClearDetourPortService) {
			Iterator<Object> iter = data.values().iterator();
			while (iter.hasNext()) {
				Section section = (Section) iter.next();
				if (section != null) {
					section.clearPortService(requestClearDetourReason);
				}
			}
		}
		requestClearDetour = false;
		requestClearDetourPortService = false;
		requestClearDetourReason = DETOUR_REASON.NONE;
	}
	
	/**
	 * 2015.02.10 by MYM : 장애 지역 우회 기능
	 * @param reason
	 */
	public void requestClearDetour(DETOUR_REASON reason) {
		this.requestClearDetour = true;
		this.requestClearDetourReason = reason;
	}
	
	/**
	 * 2015.02.10 by MYM : 장애 지역 우회 기능
	 * @param reason
	 */
	public void requestClearDetourPortService(DETOUR_REASON reason) {
		this.requestClearDetourPortService = true;
		this.requestClearDetourReason = reason;
	}
	
	/**
	 * 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
	 * @param isOperation
	 */
	public void setOperation(boolean isOperation) {
		this.isOperation = isOperation;
	}
	
	/**
	 * 2015.08.08 by MYM : 장애 회피 오류 자동 체크
	 */
	private void checkDetourMismatch() {
		if (serviceState == MODULE_STATE.INSERVICE) {
			if (Math.abs(System.currentTimeMillis() - lastDetourMismatchCheckTime) > 5000) {
				Iterator<Object> iter = data.values().iterator();
				while (iter.hasNext()) {
					Section section = (Section) iter.next();
					if (section != null) {
						section.checkDetourMismatch();
					}
				}
				lastDetourMismatchCheckTime = System.currentTimeMillis();
			}
		} else {
			lastDetourMismatchCheckTime = System.currentTimeMillis();
		}
	}
	

	public boolean isRuntimeUpdateRequested() {
		return isRuntimeUpdateRequested;
	}

}
