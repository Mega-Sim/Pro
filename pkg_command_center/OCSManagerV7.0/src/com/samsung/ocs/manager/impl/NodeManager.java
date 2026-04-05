package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.Area;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.PassDoor;
import com.samsung.ocs.manager.index.ZoneTable;

/**
 * NodeManager Class, OCS 3.0 for Unified FAB
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

public class NodeManager extends AbstractManager {
	private static NodeManager manager = null;
	private static final String ENABLED = "ENABLED";
	private static final String HID = "HID";
	private static final String LEFT = "LEFT";
	private static final String NODEID = "NODEID";
	private static final String STATUSCHANGEDTIME = "STATUSCHANGEDTIME";
	private static final String TOP = "TOP";
	private static final String TRAFFICPENALTY = "TRAFFICPENALTY";
	private static final String TYPE = "TYPE";
	private static final String VIRTUAL = "VIRTUAL";
	private static final String ZONE = "ZONE";
	private static final String RAIL = "RAIL";
	private static final String AREA = "AREA";
	private static final String BAY = "BAY";
	private static final String COLLISIONTYPE = "COLLISIONTYPE";
	private static final String CHECKCOLLISION = "CHECKCOLLISION";
	private static final String TRUE = "TRUE";
	private static final String MATERIAL = "MATERIAL";
	private static final String PASSDOORID = "PASSDOORID";
	private static final String PASSDOORMODE = "PASSDOORMODE";
	private static final String STATUS = "STATUS";
	private static final String ERRORCODE = "ERRORCODE";
	
	// 2011.11.09 by PMM
	private boolean isRuntimeUpdateRequested = false;
	private boolean isRuntimeUpdatable = false;
	
	// 2014.10.07 by KYK : list 동시참조에 따른 이슈발생(가상노드 무시됨), thread-safe Vector 로 변경
//	private LinkedList<Node> disabledNodeList = new LinkedList<Node>();
//	private LinkedList<Node> virtualNodeList = new LinkedList<Node>();
	private Vector<Node> disabledNodeList = new Vector<Node>();
	private Vector<Node> virtualNodeList = new Vector<Node>();
	private HashSet<Node> abnormalPassDoorNodeSet = new HashSet<Node>();
	private HashSet<String> abnormalNodeIdList = new HashSet<String>();							// JobAssign 용.
	private HashMap<String, String> carrierLocMaterialOfBay = new HashMap<String, String>();	// JobAssign 용.
	private ArrayList<Node> enableChangedNodeList = new ArrayList<Node>();
	// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//	private HashSet<Node> abnormalVehiclesCollisionNodeSet = new HashSet<Node>();
	
	private AreaManager areaManager = null;
	private ZoneTable zoneTable = null;
	
	private boolean isOperation = false;
	
	/**
	 * Constructor of NodeManager class.
	 */
	private NodeManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long interval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, interval );
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(Node.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		areaManager = AreaManager.getInstance(Area.class, dbAccessManager, true, true, 500);
	}
	
	/**
	 * Constructor of NodeManager class. (Singleton)
	 */
	public static synchronized NodeManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new NodeManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
		// 2011.11.09 by PMM
		if (isRuntimeUpdateRequested == false) {
			isRuntimeUpdatable = false;
			if (isInitialized) {
				updateChangedNodeInfo();
				
				// 2015.03.03 by zzang9un : PassDoor 정보 업데이트
				updatePassDoorInfo();
			} else {
				// 어떤 경우?? 예방 차원
				init();
			}
		} 
		isRuntimeUpdatable = true;
		return true;
	}
	
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB() {
		isRuntimeUpdateRequested = true;
		isInitialized = false;
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
		
		// NodeManager는 data.clear()를 하지 않음.
		
		disabledNodeList.clear();
		virtualNodeList.clear();
		abnormalNodeIdList.clear();
		carrierLocMaterialOfBay.clear();
		// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//		abnormalVehiclesCollisionNodeSet.clear();
		
		init();
		isRuntimeUpdateRequested = false;

		// 2014.10.07 by KYK : method 동시호출에 따른 이슈발생 (가상노드 무시됨), 여기서는 호출 안하도록 주석처리
//		updateChangedNodeInfo();
	}
	
	// 2011.11.09 by PMM
	private static final String INIT_SELECT_SQL = "SELECT * FROM NODE ORDER BY NODEID";
	/**
	 * 2015.03.18 by KYK : select node 후 select passdoor 처리로 변경
	 * 2015.03.03 by zzang9un : PassDoor 테이블을 함께 select 하도록 수정
	 * @return
	 */
	private boolean initialize() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		HIDManager hidManager = HIDManager.getInstance(Hid.class, dbAccessManager, true, true, 500);
		areaManager = AreaManager.getInstance(Area.class, dbAccessManager, true, true, 500);
		zoneTable = ZoneTable.getInstance();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INIT_SELECT_SQL);
			rs = pstmt.executeQuery();
			String nodeId = "";
			Node node = null;
			while (rs.next()) {
				nodeId = rs.getString(NODEID);
				node = (Node) data.get(nodeId);
				if (node == null) {
					node = (Node) vOType.newInstance();
					node.setNodeId(nodeId);
					data.put(nodeId, node);
				}
				setNodeInfo(node, rs, hidManager);
				removeKeys.remove(nodeId);
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
		// PassDoor 
		updatePassDoorInfo();
		
		return result;
	}
	
	// 2011.11.09 by PMM
	private static final String SELECT_CHANGED_SQL = "SELECT * FROM NODE WHERE ENABLED='FALSE' OR TYPE='VIRTUAL'";
	/**
	 * 
	 * @return
	 */
	private boolean updateChangedNodeInfo() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		LinkedList<Node> lastDisabledNodeList = new LinkedList<Node>();
		LinkedList<Node> lastVirtualNodeList = new LinkedList<Node>();
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_CHANGED_SQL);
			rs = pstmt.executeQuery();
			String nodeId = "";
			Node node = null;
			while (rs.next()) {
				if (isRuntimeUpdateRequested) {
					return false;
				}
				nodeId = rs.getString(NODEID);
				node = (Node) data.get(nodeId);
				if (node != null) {
					setChangedNodeInfo(node, rs, lastDisabledNodeList, lastVirtualNodeList);
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
			
			// 2012.03.02 by PMM
			// Clean Room에서 DB Archive Log 삭제 중 DB가 끊어짐.
			// 가상 노드, Disabled Node가 reset되면서 주행 차단 구간으로 VHL이 진입한 경우가 발생함.
			// Update from DB 시, 비정상인 경우에는 아래 부분을 수행하면 안됨.
			return result;
		}
		
		// 2011.11.09 by PMM
		
		// Enabled <-> Disabled Node 처리
		Node node = null;
		for (int i = this.disabledNodeList.size()-1; i >= 0; i--) {
			node = this.disabledNodeList.get(i);
			
			// 2012.03.02 by PMM
			// [NotNullCheck]
			if (node != null) {
				if (lastDisabledNodeList.contains(node)) {
					// Disabled -> Disabled
					lastDisabledNodeList.remove(node);
				} else {
					// Disabled -> Enabled
					node.setEnabled(true);
					// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
					// 2014.10.13 by MYM : 장애 지역 우회 기능
					if (isOperation) {
						node.releaseAbnormalSection();
					}
					this.disabledNodeList.remove(i);
					
					if (enableChangedNodeList != null && enableChangedNodeList.contains(node) == false) {
						enableChangedNodeList.add(node);
					}
				}
			} else {
				StringBuilder message = new StringBuilder();
				message.append("node is null");
				message.append(" - disabledNodeList Size:").append(this.disabledNodeList.size());
				message.append(", i:").append(i);
				writeExceptionLog(LOGFILENAME, message.toString());
			}
		}
		for (int i = 0; i < lastDisabledNodeList.size(); i++) {
			// Enabled -> Disabled 
			node = lastDisabledNodeList.get(i);
			
			// 2012.03.02 by PMM
			// [NotNullCheck]
			if (node != null) {
				node.setEnabled(false);
				// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가 (RepathSearch 요청 포함)
				// 2014.10.13 by MYM : 장애 지역 우회 기능
				if (isOperation) {
					node.checkRepathSearch();
					node.setAbnormalSection();
				}
				this.disabledNodeList.add(node);
			} else {
				StringBuilder message = new StringBuilder();
				message.append("node is null");
				message.append(" - lastDisabledNodeList Size:").append(lastDisabledNodeList.size());
				message.append(", i:").append(i);
				writeExceptionLog(LOGFILENAME, message.toString());
			}
		}
		
		// Common <-> Virtual Node 처리
		for (int i = this.virtualNodeList.size()-1; i >= 0; i--) {
			node = this.virtualNodeList.get(i);
			
			// 2012.03.02 by PMM
			// [NotNullCheck]
			if (node != null) {
				if (lastVirtualNodeList.contains(node)) {
					// Virtual -> Virtual
					lastVirtualNodeList.remove(node);
				} else {
					// Virtual -> Common
					node.setVirtual(false);
					this.virtualNodeList.remove(i);
				}
			} else {
				StringBuilder message = new StringBuilder();
				message.append("node is null");
				message.append(" - virtualNodeList Size:").append(this.virtualNodeList.size());
				message.append(", i:").append(i);
				writeExceptionLog(LOGFILENAME, message.toString());
			}
		}
		for (int i = 0; i < lastVirtualNodeList.size(); i++) {
			// Common -> Virtual
			node = lastVirtualNodeList.get(i);
			
			// 2012.03.02 by PMM
			// [NotNullCheck]
			if (node != null) {
				node.setVirtual(true);
				this.virtualNodeList.add(node);
			} else {
				StringBuilder message = new StringBuilder();
				message.append("node is null");
				message.append(" - lastVirtualNodeList Size:").append(lastVirtualNodeList.size());
				message.append(", i:").append(i);
				writeExceptionLog(LOGFILENAME, message.toString());
			}
		}
		
		return result;
	}
	
	private static final String SELECT_PASSDOOR_SQL = "SELECT * FROM PASSDOOR";	
	/**
	 * 2015.03.03 by zzang9un : PassDoor 정보 업데이트(Node Enabled와 관계없이 업데이트)
	 * @return
	 */
	private boolean updatePassDoorInfo() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		HashSet<Node> lastAbnormalPassDoorNodeSet = new HashSet<Node>(); 
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_PASSDOOR_SQL);
			rs = pstmt.executeQuery();
			Node node = null;
			PassDoor passDoor = null;
			while (rs.next()) {
				if (isRuntimeUpdateRequested) {
					return false;
				}
				
				node = (Node) data.get(getString(rs.getString(NODEID)));
				if (node != null) {
					passDoor = node.getPassDoor();
					if (passDoor == null) {
						passDoor = new PassDoor();
						passDoor.setNodeId(node.getNodeId());
						node.setPassDoor(passDoor);
					}
					setPassDoorInfo(passDoor, rs);
					
					if (passDoor.checkPassable() == false) {
						// 신규로 Abnormal인 PassDoor 등록
						lastAbnormalPassDoorNodeSet.add(node);
					}
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
		
		// 2015.03.19 by MYM : isInitialized 조건 추가(init 단계에서는 abnormalPassDoorNodeSet가 null임) 
		if (isInitialized) {
			// PassDoor가 정상 → 비정상 전환시 경유 Vehicle RePathSearch 요청
			boolean isPassDoorControlUsed = OCSInfoManager.getInstance(null, null, false, false, 0).isPassDoorControlUsage();
			HashSet<Node> removeAbnormalPassDoorNodeSet = new HashSet<Node>(abnormalPassDoorNodeSet); 
			for (Node node : lastAbnormalPassDoorNodeSet) {
				// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
				if (isOperation && isPassDoorControlUsed && abnormalPassDoorNodeSet.contains(node) == false) {
					node.checkRepathSearch();
				}
				abnormalPassDoorNodeSet.add(node);
				removeAbnormalPassDoorNodeSet.remove(node);
			}
			
			// PassDoor가 비정상 → 정상 전환시 list에서 제거
			for (Node node : removeAbnormalPassDoorNodeSet) {
				abnormalPassDoorNodeSet.remove(node);
			}
			
			lastAbnormalPassDoorNodeSet.clear();
			removeAbnormalPassDoorNodeSet.clear();
		}
		return result;
	}
	
	/**
	 * 
	 * @param node
	 * @param rs
	 * @param hidManager
	 * @exception SQLException
	 */
	private void setNodeInfo(Node node, ResultSet rs, HIDManager hidManager) throws SQLException {
		// 2012.03.02 by PMM
		// [NotNullCheck]
		if (node != null && rs != null && hidManager != null) {
			// 2011.11.04 by PMM
			node.initialize();
			
			// 2014.10.07 by KYK : 방식변경 virtual, enabled 설정을 initialize 후 별도 update 하였으나 db 값으로 한번에 설정되도록 함
			// 2011.11.09 by PMM
			// 방식 변경, initialize()에서는 All enabled, common, updateFromDB()에서 disabled, virtual 가져와서 Update.
			node.setVirtual(rs.getString(TYPE).equals(VIRTUAL));		
			node.setEnabled(getBoolean(rs.getString(ENABLED)));
//			node.setVirtual(false);		
//			node.setEnabled(true);
			
			node.setLeft(rs.getLong(LEFT));
			node.setTop(rs.getLong(TOP));
			setNodeZone(node, getString(rs.getString(ZONE)));
			
			node.setRail(getString(rs.getString(RAIL))); // 2012.06.22 by MYM : Node의 Rail 추가
			
			String areaId = getString(rs.getString(AREA));
			node.setAreaId(areaId);
			node.setArea(areaManager.getArea(areaId));
			
			node.setBay(getString(rs.getString(BAY)));
			node.setCollisionType(getString(rs.getString(COLLISIONTYPE)));
			node.setStatusChangedTime(getString(rs.getString(STATUSCHANGEDTIME)));
			node.setTrafficPenalty(rs.getDouble(TRAFFICPENALTY));

			// 2014.03.25 by MYM : Hybrid 주행 제어 (Node의 CheckCollision이 TRUE인 경우는 Vehicle 간섭 및 충돌설정 고려 주행)
			node.setCheckCollision(getString(rs.getString(CHECKCOLLISION)).equals(TRUE));
			
			// 2011.11.07 by PMM
			Hid hid = (Hid) hidManager.getHid(getString(rs.getString(HID)));
			node.setHid(hid);
			if (hid == null) {
				writeExceptionLog(LOGFILENAME, "HID is null. UnitId:" + getString(rs.getString(HID)));
			}
		} else {
			writeExceptionLog(LOGFILENAME, "setNodeInfo(Node node, ResultSet rs, HIDManager hidManager) - one of parameters is null.");
		}
	}

	/**
	 * 2015.03.03 by zzang9un : PassDoor 초기화
	 * @param passDoor
	 * @param rs
	 * @throws SQLException
	 */
	private void setPassDoorInfo(PassDoor passDoor, ResultSet rs) throws SQLException {
		passDoor.setPassDoorId(getString(rs.getString(PASSDOORID)));
		passDoor.setEnabled(getString(rs.getString(ENABLED)));
		passDoor.setMode(rs.getString(PASSDOORMODE));
		passDoor.setStatus(getString(rs.getString(STATUS)));
		passDoor.setErrorCode(rs.getInt(ERRORCODE));
	}
	
	private void setNodeZone(Node node, String zone) {
		node.setZone(zone);
		if (zoneTable != null) {
			node.setZoneIndex(zoneTable.getZoneIndex(zone));
		}
	}

	// 2011.11.09 by PMM
//	private void setChangedNodeInfo(Node node, ResultSet rs, LinkedList<Node> disabledNodeList, LinkedList<Node> virtualNodeList) throws SQLException {
	/**
	 * 
	 * @param node
	 * @param rs
	 * @param lastDisabledNodeList
	 * @param lastVirtualNodeList
	 * @exception SQLException
	 */
	private void setChangedNodeInfo(Node node, ResultSet rs, LinkedList<Node> lastDisabledNodeList, LinkedList<Node> lastVirtualNodeList) throws SQLException {
		// 2012.03.02 by PMM
		// [NotNullCheck]
		if (node != null && rs != null && lastDisabledNodeList != null && lastVirtualNodeList != null) {
			node.setEnabled(getBoolean(rs.getString(ENABLED)));
			if (node.isEnabled() == false) {
				lastDisabledNodeList.add(node);
			}
			node.setVirtual(VIRTUAL.equals(rs.getString(TYPE)));
			if (node.isVirtual()) {
				lastVirtualNodeList.add(node);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "setChangedNodeInfo(Node node, ResultSet rs, LinkedList<Node> lastDisabledNodeList, LinkedList<Node> lastVirtualNodeList) - one of parameters is null.");
		}
	}
	
	/**
	 * 
	 */
	public ConcurrentHashMap<String, Object> getData() {
		return data;
	}

	/**
	 * 
	 * @param nodeId
	 * @return
	 */
	public Node getNode(String nodeId) {
		return (Node) data.get(nodeId);
	}
	
	/**
	 * 
	 * @param nodeId
	 * @return
	 */
	public boolean isValidNode(String nodeId) {
		if (data.containsKey(nodeId)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param nodeId
	 * @return
	 */
	public String getBayByNodeId(String nodeId) {
		Node node = (Node) data.get(nodeId);
		if (node != null) {
			return node.getBay();
		} else {
			writeExceptionLog(LOGFILENAME, "getBayByNodeId(String nodeId) - node is null. NodeId:" + nodeId);
			return "";
		}
	}
	
	private static final String SELECT_BAY_AND_CARRIERLOCMATERIAL_SQL = "SELECT N.BAY AS BAY, C.MATERIAL AS MATERIAL, COUNT(C.CARRIERLOCID) AS COUNT FROM CARRIERLOC C, NODE N WHERE C.NODE = N.NODEID GROUP BY N.BAY, C.MATERIAL ORDER BY N.BAY, 2, 3 DESC";
	/**
	 * 
	 * @return
	 */
	public boolean updateCarrierLocMaterialOfBay() {
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		String bay;
		String material;
		boolean result = false;
		try {
			carrierLocMaterialOfBay.clear();
			conn = dbAccessManager.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SELECT_BAY_AND_CARRIERLOCMATERIAL_SQL);
			while (rs.next()) {
				bay = rs.getString(BAY);
				material = rs.getString(MATERIAL);
				carrierLocMaterialOfBay.put(bay, material);
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
	
	/**
	 * 
	 * @param bay
	 * @return
	 */
	public String getCarrierLocMaterialOfBay(String bay) {
		if (carrierLocMaterialOfBay.containsKey(bay)) {
			return carrierLocMaterialOfBay.get(bay);
		}
		return "";
	}
	
	/**
	 * 
	 */
	public void resetAbnormalNodeIdList() {
		abnormalNodeIdList.clear();
	}
	
	/**
	 * 
	 * @param nodeId
	 */
	public void addAbnormalNodeToList(String nodeId) {
		if (abnormalNodeIdList.contains(nodeId) == false) {
			abnormalNodeIdList.add(nodeId);
		}
	}
	
	/**
	 * 
	 * @param nodeId
	 */
	public void removeAbnormalNodeFromList(String nodeId) {
		if (abnormalNodeIdList.contains(nodeId)) {
			abnormalNodeIdList.remove(nodeId);
		}
	}
	
	/**
	 * 
	 * @param nodeId
	 * @return
	 */
	public boolean containsAbnormalNode(String nodeId) {
		if (nodeId == null || abnormalNodeIdList == null) {
			return false;
		}
		return abnormalNodeIdList.contains(nodeId);
	}
	
	public HashSet<String> getAbnormalNodeIdList() {
		return abnormalNodeIdList;
	}
	
	private static final String SELECT_AREA_IN_ZONE_SQL = "SELECT DISTINCT AREA FROM NODE WHERE ZONE=? AND ENABLED='TRUE' ORDER BY AREA";
	/**
	 * 
	 * @return
	 */
	public ArrayList<String> getAreaList(String nodeZone) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String area = null;
		ArrayList<String> areaList = new ArrayList<String>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_AREA_IN_ZONE_SQL);
			pstmt.setString(1, nodeZone);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				area = getString(rs.getString(AREA));
				areaList.add(area);
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
		return areaList;
	}
	
	public ArrayList<Node> getEnableChangedNodeList() {
		return enableChangedNodeList;
	}
	
	public void resetEnableChangedNodeList() {
		if (enableChangedNodeList != null) {
			enableChangedNodeList.clear();
		} else {
			enableChangedNodeList = new ArrayList<Node>();
		}
	}
	
	// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//	public HashSet<Node> getAbnormalVehiclesCollisionNodeSet() {
//		return this.abnormalVehiclesCollisionNodeSet;
//	}
	
	/**
	 * 2015.02.05 by MYM : 장애 지역 우회 기능
	 */
	public void checkChangedSectionEnabled() {
		for (Node node : disabledNodeList) {
			node.setAbnormalSection();
		}
	}
	
	/**
	 * 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
	 * @param isOperation
	 */
	public void setOperation(boolean isOperation) {
		this.isOperation = isOperation;
	}
}