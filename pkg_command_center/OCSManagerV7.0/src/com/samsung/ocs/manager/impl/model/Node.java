package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.DEADLOCK_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;

/**
 * Node Class, OCS 3.0 for Unified FAB
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

public class Node {
	protected String nodeId;
	protected String collisionType; // 2011.11.07 by MYM
	protected boolean isEnabled;
	protected long left;
	protected long top;
	protected String zone;
	protected int zoneIndex;
	protected String rail;
	protected String areaId;
	protected Area area;
	protected String bay;
	protected String statusChangedTime;
	protected double trafficPenalty;	
	protected Hid hid;
	protected boolean checkCollision;
	
	protected boolean isVirtual = false;
	protected int angle = -1;
	protected boolean isDiverge = false;
	protected boolean isConverge = false;
	
	protected PassDoor passDoor; // 2015.03.03 by zzang9un : PassDoor를 Node에 포함하도록 변경
	
	protected HashMap<Node, Double> moveInNodeTimeTable = new HashMap<Node, Double>();
	protected HashMap<Node, Double> moveInNodeDistanceTable = new HashMap<Node, Double>();
//	protected HashMap<Node, Boolean> moveInNodeDirectionTable = new HashMap<Node, Boolean>();  // 2011.11.02 by MYM : 현재는 사용하지 않아 주석처리(향후 양방향 주행 기능시 사용)
	
	protected ArrayList<Section> sectionList = new ArrayList<Section>();
	protected ArrayList<Collision> collisionList = new ArrayList<Collision>();
	protected ArrayList<CloseLoop> closeLoopList = new ArrayList<CloseLoop>();
	protected ArrayList<Block> blockList = new ArrayList<Block>();
	// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
	protected HashSet<Block> collisionBlockSet = new HashSet<Block>();
	
	protected ArrayList<VehicleData> routedVehicleList = new ArrayList<VehicleData>();
	protected ArrayList<VehicleData> driveVehicleList = new ArrayList<VehicleData>();
	// 2013.02.08 by KYK
	protected ArrayList<VehicleData> driveReservedVehicleList = new ArrayList<VehicleData>();	
	
	//2011.12.27 by PMM
	protected ArrayList<VehicleData> reservedVehicleList = new ArrayList<VehicleData>();
	protected HashMap<VehicleData, Long> reservedTimeTable = new HashMap<VehicleData, Long>();
	
	protected ConcurrentHashMap<String, NodeArrivedTimeInfo> arrivedTimeTable = new ConcurrentHashMap<String, NodeArrivedTimeInfo>();
	NodeArrivedTimeInfo costArrivedTime = new NodeArrivedTimeInfo(); 
	
	private static final String NO_VEHICLE = "NoVehicle";
	private static final String NULL = "";
	private static final String LINE = "LINE";
//	private static final String CURVE = "CURVE";

	protected boolean isStopAllowed = true;
	
	private List<VehicleData> arrivedVehicleList = Collections.synchronizedList(new ArrayList<VehicleData>());
	private ConcurrentHashMap<String, Long> vehicleArrivedTimeTable = new ConcurrentHashMap<String, Long>();

	public boolean setDriveVehicleInfo(VehicleData vehicle, long arrivedTime) {
		synchronized (arrivedVehicleList) {
			if (vehicleArrivedTimeTable.putIfAbsent(vehicle.getVehicleId(), new Long(arrivedTime)) == null) {
				arrivedVehicleList.add(vehicle);
				return true;
			}
			return false;
		}
	}
	
	/**
	 * 2015.05.27 by MYM : Vehicle 도착 시간 업데이트 
	 * 배경 : Block 통과시 도착시간이 늦어서 후순위인 경우 
	 *       상대 Path Vehicle이 Drive Fail로 인해 계속 못가는 경우는
	 *       나의 도착시간을 업데이트하여 먼저갈 수 있도록 함.
	 */
	public boolean updateDriveVehicleInfo(VehicleData vehicle, long arrivedTime) {
		synchronized (arrivedVehicleList) {
			if (vehicleArrivedTimeTable.containsKey(vehicle.getVehicleId())) {
				vehicleArrivedTimeTable.put(vehicle.getVehicleId(), new Long(arrivedTime));
				return true;
			}
			return false;
		}
	}

	public boolean resetDriveVehicleInfo(VehicleData vehicle) {
		synchronized (arrivedVehicleList) {
			vehicleArrivedTimeTable.remove(vehicle.getVehicleId());
			return arrivedVehicleList.remove(vehicle);
		}
	}
	
	public VehicleData getArrivedVehicle() {
		synchronized (arrivedVehicleList) {
			for (int i = 0; i < arrivedVehicleList.size(); i++) {
				VehicleData occupiedVehicle = arrivedVehicleList.get(i);
				if (occupiedVehicle != null) {
					return occupiedVehicle;
				}
			}
			return null;
		}
	}
	
	public boolean containsVehicle(VehicleData vehicle) {
		synchronized (arrivedVehicleList) {
			return arrivedVehicleList.contains(vehicle);
		}
	}

	public int getArrivedVehicleCount() {
		synchronized (arrivedVehicleList) {
			return arrivedVehicleList.size();
		}
	}

	public VehicleData getArrivedVehicle(int index) {
		synchronized (arrivedVehicleList) {
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (index < 0 || index >= arrivedVehicleList.size()) {
				return null;
			}
			
			return arrivedVehicleList.get(index);
		}
	}

	public long getVehicleArrivedTime(String vehicleName) {
		synchronized (vehicleArrivedTimeTable) {
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			Long arrivedTime = vehicleArrivedTimeTable.get(vehicleName);
			if(arrivedTime != null) {
				return arrivedTime.longValue();
			} else {
				return System.currentTimeMillis(); 
			}
//			return (vehicleArrivedTimeTable.get(vehicleName)).longValue();
		}
	}

	public long getFirstVehicleArrivedTime() {
		synchronized (arrivedVehicleList) {
			VehicleData vehicle = (VehicleData) arrivedVehicleList.get(0);
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (vehicle != null) {
				Long arrivedTime = vehicleArrivedTimeTable.get(vehicle.getVehicleId());
				if (arrivedTime != null) {
					return arrivedTime.longValue();
				} else {
					return System.currentTimeMillis();
				}
			} else {
				return System.currentTimeMillis();
			}
//			return (vehicleArrivedTimeTable.get(vehicle.getVehicleId())).longValue();
		}
	}

	/**
	 * 
	 * @return driveVehicleList
	 */
	public String toVehicleString() {
		StringBuffer vehicles = new StringBuffer();
		synchronized (arrivedVehicleList) {
			for (int i = 0; i < arrivedVehicleList.size(); i++) {
				VehicleData vehicle = arrivedVehicleList.get(i);
				if (vehicle != null) {
					if (vehicles.length() > 0) {
						vehicles.append(",");
					}
					vehicles.append(vehicle.getVehicleId());
				}			
			}		
		}
		return vehicles.toString();
	}
	
	// 2011.11.04 by PMM
	// RuntimeUpdate 시, node의 reference는 유지하면서,
	// 관련  data는 초기화해야 함.
	/**
	 * Node 초기화
	 */
	public void initialize() {
		hid = null;
		isVirtual = false;
		angle = -1;
		isDiverge = false;
		isConverge = false;
		passDoor = null;
		
		if (moveInNodeTimeTable != null) {
			moveInNodeTimeTable.clear();
		}
		if (moveInNodeDistanceTable != null) {
			moveInNodeDistanceTable.clear();
		}
		if (sectionList != null) {
			sectionList.clear();
		}
		if (collisionList != null) {
			collisionList.clear();
		}
		if (closeLoopList != null) {
			closeLoopList.clear();
		}
		if (blockList != null) {
			blockList.clear();
		}
		if (routedVehicleList != null) {
			routedVehicleList.clear();
		}
		if (driveVehicleList != null) {
			driveVehicleList.clear();
		}
		if (arrivedTimeTable != null) {
			arrivedTimeTable.clear();
		}
	}
	
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public String getCollisionType() {
		return collisionType;
	}
	public void setCollisionType(String collisionType) {
		this.collisionType = collisionType;
	}
	public Hid getHid() {
		return hid;
	}
	public void setHid(Hid hid) {
		this.hid = hid;
	}
	public PassDoor getPassDoor() {
		return passDoor;
	}
	public void setPassDoor(PassDoor passDoor) {
		this.passDoor = passDoor;
	}
	public boolean isEnabled() {
		return isEnabled;
	}
	public void setEnabled(boolean enabled) {
		this.isEnabled = enabled;
	}
	public long getLeft() {
		return left;
	}
	public void setLeft(long left) {
		this.left = left;
	}
	public long getTop() {
		return top;
	}
	public void setTop(long top) {
		this.top = top;
	}
	public String getBay() {
		return bay;
	}
	public void setBay(String bay) {
		this.bay = bay;
	}
	public String getZone() {
		return zone;
	}
	public void setZone(String zone) {
		this.zone = zone;
	}
	public int getZoneIndex() {
		return zoneIndex;
	}
	public void setZoneIndex(int zoneIndex) {
		this.zoneIndex = zoneIndex;
	}
	public String getRail() {
		return rail;
	}
	public void setRail(String rail) {
		this.rail = rail;
	}
	public String getAreaId() {
		return areaId;
	}
	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}
	public Area getArea() {
		return area;
	}
	public void setArea(Area area) {
		this.area = area;
	}
	public String getStatusChangedTime() {
		return statusChangedTime;
	}
	public void setStatusChangedTime(String statusChangedTime) {
		this.statusChangedTime = statusChangedTime;
	}
	public double getTrafficPenalty() {
		return trafficPenalty;
	}
	public void setTrafficPenalty(double trafficPenalty) {
		this.trafficPenalty = trafficPenalty;
	}
	
	public void setAngle(int angle) {
		this.angle = angle;
	}
	
	public int getAngle() {
		return angle;
	}
	
	public void setVirtual(boolean isVirtual) {
		this.isVirtual = isVirtual;
	}
	
	public boolean isVirtual() {
		return isVirtual;
	}
	
	public boolean isDiverge() {
		return isDiverge;
	}
	
	public boolean isConverge() {
		return isConverge;
	}
	
	public void addSection(Section section) {
		sectionList.add(section);
	}	
	public int getSectionCount() {
		return sectionList.size();
	}	
	public Section getSection(int index) {
		return sectionList.get(index);
	}
	public ArrayList<Section> getSection() {
		return sectionList;
	}	
	
	/**
	 * 
	 * @param time
	 * @param distance
	 * @param prevNode
	 * @param forward
	 */
	public void setMoveInTime(double time, double distance, Node prevNode, boolean forward) {
		moveInNodeTimeTable.put(prevNode, new Double(time));
		moveInNodeDistanceTable.put(prevNode, new Double(distance));
//		moveInNodeDirectionTable.put(prevNode, new Boolean(forward)); // 2011.11.02 by MYM : 현재는 사용하지 않아 주석처리(향후 양방향 주행 기능시 사용)
	}
	
//	/**
//	 * 
//	 * @param prevNode
//	 * @return
//	 */
//	public double getMoveInTime(Node prevNode) {
//		return moveInNodeTimeTable.get(prevNode).doubleValue();
//	}	
//
//	/**
//	 * 
//	 * @param prevNode
//	 * @return
//	 */
//	public double getMoveInDistance(Node prevNode) {
//		return moveInNodeDistanceTable.get(prevNode).doubleValue();
//	}

	/**
	 * 2013.07.11 by KYK
	 * @param prevNode
	 * @return
	 */
	public Double getMoveInTime(Node prevNode) {
		if (moveInNodeTimeTable.get(prevNode) == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("[Invalid reference in getMoveInTime(prevNode)] node:").append(this.getNodeId());
			if (prevNode == null) {
				sb.append(" ,prevNode(input) is null.");
			} else {
				sb.append(" ,prevNode:").append(prevNode.getNodeId());
			}
			traceManagerDebug(sb.toString());
			return 0.0;
		}
		return moveInNodeTimeTable.get(prevNode).doubleValue();
	}
	
	public double getMoveInDistance(Node prevNode) {
		if (moveInNodeDistanceTable.get(prevNode) == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("[Invalid reference in getMoveInDistance(prevNode)] node:").append(this.getNodeId());
			if (prevNode == null) {
				sb.append(" ,prevNode(input) is null.");
			} else {
				sb.append(" ,prevNode:").append(prevNode.getNodeId());
			}
			traceManagerDebug(sb.toString());
			return 0.0;
		}		
		return moveInNodeDistanceTable.get(prevNode).doubleValue();
	}

	// 2016.03.08 by KYK
	private static final String MANAGER_DEBUG_TRACE = "OcsManagerDebug";
	private static Logger managerDebugTraceLog = Logger.getLogger(MANAGER_DEBUG_TRACE);
	public void traceManagerDebug(String message) {
		managerDebugTraceLog.error(String.format("%s", message));
	}
//	// 2013.07.11 by KYK
//	private static final String OPERATION_EXCEPTION_TRACE = "OperationException";
//	private static Logger operationExceptionTraceLog = Logger.getLogger(OPERATION_EXCEPTION_TRACE);
//	public void traceOperationException(String message) {
//		operationExceptionTraceLog.error(String.format("%s", message));
//	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	public double getLength(Node node) {
		return node.getLength(left, top);
	}
	
	/**
	 * 
	 * @param left
	 * @param top
	 * @return
	 */
	public double getLength(double left, double top) {
		return Math.sqrt((this.left - left) * (this.left - left) + (this.top - top) * (this.top - top));
	}
	
	/**
	 * 
	 * @param section
	 * @return
	 */
	public boolean removeSection(Section section) {
		return sectionList.remove(section);
	}
	
	/**
	 * 
	 */
	public void checkMergeType() {
		int inCount = 0;
		int outCount = 0;
		
		Iterator<Section> iterator = sectionList.iterator();
		Section section;
		Node firstNode;
		Node lastNode;
		while (iterator.hasNext()) {
			section = (Section)iterator.next();
			firstNode = section.getFirstNode();
			lastNode = section.getLastNode();

			if (this.equals(firstNode)) {
				outCount++;
			}
			if (this.equals(lastNode)) {
				inCount++;
			}
		}
		
		if ((inCount == 1) && (outCount > inCount)) {
			isDiverge = true;
		} else if ((outCount == 1) && (inCount > outCount)) {
			isConverge = true;
		} else if (inCount > 1 && outCount > 1) {
			isDiverge = true;
			isConverge = true;
		}
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	public String getLinkType(Node node) {
		if (Math.abs(left - node.getLeft()) <= 50 ||
				Math.abs(top - node.getTop()) <= 50) {
			return OcsConstant.LINE;
		}
		return OcsConstant.CURVE;
	}
	
	/**
	 * 
	 * @param block
	 */
	public void addBlock(Block block) {
		blockList.add(block);
	}

	/**
	 * 2011.12.14 by MYM
	 * @param block
	 */
	public void removeBlock(Block block) {
		blockList.remove(block);
	}
	
	/**
	 * 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
	 * @param block
	 */
	public void addCollisionBlock(Block block) {
		collisionBlockSet.add(block);
	}
	
	/**
	 * 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
	 * @param block
	 */
	public void removeCollisionBlock(Block block) {
		collisionBlockSet.remove(block);
	}
	
	/**
	 * 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
	 * 
	 * @return
	 */
	public HashSet<Block> getCollisionBlockSet() {
		return collisionBlockSet;
	}
	
	/**
	 * 2011.11.29 by MYM : prevBlock 파라미터 추가
	 * @return
	 */
	public Block getBlock(Block prevBlock) {
		Block block;
		Iterator<Block> iterator = blockList.iterator();
		while (iterator.hasNext()) {
			block = (Block) iterator.next();
			if (prevBlock == null) {
				if (block.checkNodeInBlock(this)) {
					return block;
				}
			} else {
				if (block.equals(prevBlock) == false && block.checkNodeInBlock(this)) {
					return block;
				}
			}
		}
		return null;
	}
	
	/**
	 * 2013.04.12 by KYK
	 * @return
	 */
	public Block getConvergeBlock() {
		for (Block block: blockList) {
			// 2013.07.30 by KYK
//			if (OcsConstant.CONVERGE.equals(block.getBlockType())) {
			if (block.isConvergeOrMultiType()) {
				return block;
			}
		}
		return null;
	}
	
	/**
	 * 2011.11.29 by MYM : BlockNode인 모든 Block을 리턴(한 노드가 2개 이상의 BlockNode로 포함될 수 있음) 
	 * @param blockList
	 */
	public void getBlocks(ArrayList<Block> blockList) {
		for (int i = 0; i < this.blockList.size(); i++) {
			Block block = this.blockList.get(i);
			if (block.checkNodeInBlock(this)) {
				blockList.add(block);
			}
		}
	}
	
	/**
	 * 2011.11.29 by MYM : 이름변경 getBlocks -> getAllBlocks 
	 * 
	 * @param blockList
	 */
	public void getAllBlocks(ArrayList<Block> blockList) {
		for (int i = 0; i < this.blockList.size(); i++) {
			Block block = this.blockList.get(i);
			if (blockList.contains(block) == false) {
				blockList.add(block);
			}
		}
	}
	
	/**
	 * 
	 * @param closeLoop
	 */
	public void addCloseLoop(CloseLoop closeLoop) {
		closeLoopList.add(closeLoop);
	}
	
	/**
	 * 
	 * @param closeLoop
	 */
	public void removeCloseLoop(CloseLoop closeLoop) {
		closeLoopList.remove(closeLoop);
	}
	
	/**
	 * 
	 * @return
	 */
	public ArrayList<Collision> getCollisions() {
		return collisionList;
	}
	
	/**
	 * 
	 * @param collision
	 */
	public void removeCollision(Collision collision) {
		collisionList.remove(collision);
	}
	
	/**
	 * 
	 * @param collision
	 */
	public void addCollision(Collision collision) {
		collisionList.add(collision);		
	}
	
	public String toString() {
		return nodeId;
	}
	
	/**
	 * 
	 * @return
	 */
	public String toCollisionString() {
		StringBuffer sb = new StringBuffer();
		int i = 0;
		Collision collision;
		if (collisionList != null) {
			Iterator<Collision> iterator = collisionList.iterator();
			while (iterator.hasNext()) {
				collision = (Collision)iterator.next();
				if (i > 0) {
					sb.append(", ");
				}
				sb.append("(").append(collision.toString()).append(")");
				i++;
			}
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param vehicleId
	 * @param arrivedTime
	 */
	public NodeArrivedTimeInfo setArrivedTime(String vehicleId, double arrivedTime) {
		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			arrivedTimeInfo = new NodeArrivedTimeInfo();
			arrivedTimeTable.put(vehicleId, arrivedTimeInfo);
		}
		arrivedTimeInfo.setPrevNode(null);
		arrivedTimeInfo.setArrivedTime(arrivedTime);
		arrivedTimeInfo.setMoveTime(0);
		arrivedTimeInfo.setDistance(0);
		arrivedTimeInfo.setSectionIndex(0);
		return arrivedTimeInfo;
	}
	
	public NodeArrivedTimeInfo setArrivedTime(String vehicleId, double arrivedTime, double heuristicCost) {
//		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
//		if (arrivedTimeInfo == null) {
//			arrivedTimeInfo = new NodeArrivedTimeInfo();
//			arrivedTimeTable.put(vehicleId, arrivedTimeInfo);
//		}
//		arrivedTimeInfo.setPrevNode(null);
//		arrivedTimeInfo.setArrivedTime(arrivedTime);
		NodeArrivedTimeInfo arrivedTimeInfo = setArrivedTime(vehicleId, arrivedTime);
		arrivedTimeInfo.setHeuristicCost(heuristicCost);
		arrivedTimeInfo.setIndex(0);
		return arrivedTimeInfo;
	}
	
	/**
	 * 
	 * @param vehicleId
	 */
	public void removeArrivedTimeInfo(String vehicleId) {		
		arrivedTimeTable.remove(vehicleId);		
	}
	
//	/**
//	 * 
//	 * @param vehicleId
//	 * @param prevNode
//	 * @param arrivedTime
//	 * @return
//	 */
//	public boolean setArrivedTime(String vehicleId, Node prevNode, double arrivedTime) {
//		if (isEnabled == false) {
//			return false;
//		}
//		
//		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
//		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//		if (arrivedTimeInfo == null) {
//			arrivedTimeInfo = new NodeArrivedTimeInfo();
//			arrivedTimeTable.put(vehicleId, arrivedTimeInfo);
//		}
//		
//		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//		// 2012.03.02 by MYM : [NotNullCheck] 추가
////		if (arrivedTimeInfo != null && arrivedTime < arrivedTimeInfo.getArrivedTime()) {
//		if (arrivedTime < arrivedTimeInfo.getArrivedTime()) {
//			arrivedTimeInfo.setPrevNode(prevNode);
//			arrivedTimeInfo.setArrivedTime(arrivedTime);
//			return true;
//		}
//		
//		return false;
//	}
//
	public boolean setArrivedTime(String vehicleId, Node prevNode, double arrivedTime, double heuristicCost, double moveTime, long distance, Section section, int sectionIndex) {
		// 2015.03.18 by KYK : checkAbnormalSearch (node.isAbnormalState) 로 이동
//		if (isEnabled == false) {
//			return false;
//		}
		
		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			arrivedTimeInfo = new NodeArrivedTimeInfo();
			arrivedTimeTable.put(vehicleId, arrivedTimeInfo);
		}
		
		if (arrivedTime < arrivedTimeInfo.getArrivedTime()) {
			arrivedTimeInfo.setPrevNode(prevNode);
			arrivedTimeInfo.setArrivedTime(arrivedTime);
			arrivedTimeInfo.setHeuristicCost(heuristicCost);
			arrivedTimeInfo.setIndex(prevNode.getIndex(vehicleId) + 1);
			
			arrivedTimeInfo.setMoveTime(moveTime);
			arrivedTimeInfo.setDistance(distance);
			arrivedTimeInfo.setSectionIndex(sectionIndex);
			return true;
		}
		
		return false;
	}
	
//	/**
//	 * 
//	 * @param vehicleId
//	 * @param prevNode
//	 * @param arrivedTime
//	 * @return
//	 */
//	public boolean setShortestArrivedTime(String vehicleId, Node prevNode, double arrivedTime) {
//		if (isEnabled == false) {
//			return false;
//		}
//		
//		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
//		if (arrivedTimeInfo == null) {
//			arrivedTimeInfo = new NodeArrivedTimeInfo();
//			arrivedTimeTable.put(vehicleId, arrivedTimeInfo);
//		}
//		
//		if (arrivedTime < arrivedTimeInfo.getArrivedTime()) {
//			arrivedTimeInfo.setPrevNode(prevNode);
//			arrivedTimeInfo.setArrivedTime(arrivedTime);
//			return true;
//		}
//		
//		return false;
//	}
	
	/**
	 * 
	 * @param vehicleId
	 * @param prevNode
	 * @param arrivedTime
	 * @return
	 */
	public boolean changeArrivedTime(String vehicleId, Node prevNode, double arrivedTime) {
		if (isEnabled == false) {
			return false;
		}

		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (arrivedTimeInfo != null) {
			arrivedTimeInfo.setPrevNode(prevNode);
			arrivedTimeInfo.setArrivedTime(arrivedTime);
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public double getArrivedTime(String vehicleId) {
		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			return OcsConstant.MAXCOST_TIMEBASE;
		}
		
		return arrivedTimeInfo.getArrivedTime();
	}
	
	public NodeArrivedTimeInfo getArrivedTimeInfo(String vehicleId) {
		return arrivedTimeTable.get(vehicleId);
	}
	
	public double getHeuristicCost(String vehicleId) {
		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			return OcsConstant.MAXCOST_TIMEBASE;
		}
		
		return arrivedTimeInfo.getHeuristicCost();
	}
	
	public void setHeuristicCost(String vehicleId, double heuristicCost) {
		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo != null) {
			arrivedTimeInfo.setHeuristicCost(heuristicCost);
		}
	}
	
	public int getIndex(String vehicleId) {
		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			return 999999;
		}
		return arrivedTimeInfo.getIndex();
	}
	
	public int getSectionIndex(String vehicleId) {
		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			return 999999;
		}
		return arrivedTimeInfo.getSectionIndex();
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public Node getPrevNode(String vehicleId) {
		NodeArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			return null;
		}
		
		return arrivedTimeInfo.getPrevNode();
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param arrivalTime
	 */
	public void setCostArrivedTime(Vehicle vehicle, double arrivalTime) {
		costArrivedTime.setVehicle(vehicle);
		costArrivedTime.setArrivedTime(arrivalTime);
		costArrivedTime.setPrevNode(null);
	}
	/**
	 * 
	 * @param key
	 * @param arrivalTime
	 */
	public void setCostArrivedTime(long key, double arrivalTime) {
		costArrivedTime.setKey(key);
		costArrivedTime.setArrivedTime(arrivalTime);
		costArrivedTime.setPrevNode(null);
	}
	
	/**
	 * 
	 */
	public void initCostArrivedTime() {
		costArrivedTime.setVehicle(null);
		costArrivedTime.setKey(0);
		costArrivedTime.setArrivedTime(9999.0);
		costArrivedTime.setPrevNode(null);
	}

	/**
	 * 
	 * @param vehicle
	 * @param prevNode
	 * @param arrivedTime
	 * @return
	 */
	public boolean setCostArrivedTime(Vehicle vehicle, Node prevNode, double arrivedTime, Section section) {
		if (isEnabled == false) {
			return false;
		}
		// 2014.02.03 by MYM : Disabled Link 처리
		if (section.isLinkEnabled(prevNode, this) == false) {
			return false;
		}
		
		// 2015.03.03 by zzang9un : PassDoor 통과를 할 수 없는 경우 false 리턴
		if ((this.passDoor != null) && (this.passDoor.checkPassable() == false)) {
			return false;
		}
		
		if (vehicle == costArrivedTime.getVehicle()) {
			if (arrivedTime < costArrivedTime.getArrivedTime()) {
				costArrivedTime.setPrevNode(prevNode);
				costArrivedTime.setArrivedTime(arrivedTime);
				return true;
			}
		} else {
			costArrivedTime.setVehicle(vehicle);
			costArrivedTime.setPrevNode(prevNode);
			costArrivedTime.setArrivedTime(arrivedTime);
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @param prevNode
	 * @param arrivedTime
	 * @return
	 */
	public boolean setCostArrivedTime(long key, Node prevNode, double arrivedTime) {
		if (isEnabled == false) {
			return false;
		}
		
		// 2015.03.03 by zzang9un : PassDoor 통과를 할 수 없는 경우 false 리턴
		if ((this.passDoor != null) && (this.passDoor.checkPassable() == false)) {
			return false;
		}
		
		if (key == costArrivedTime.getKey()) {
			if (arrivedTime < costArrivedTime.getArrivedTime()) {
				costArrivedTime.setPrevNode(prevNode);
				costArrivedTime.setArrivedTime(arrivedTime);
				return true;
			}
		} else {
			costArrivedTime.setKey(key);
			costArrivedTime.setPrevNode(prevNode);
			costArrivedTime.setArrivedTime(arrivedTime);
			return true;
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @param prevNode
	 * @param arrivedTime
	 * @return
	 */
	public boolean setCostArrivedTime(long key, Node prevNode, double arrivedTime, boolean isCheckNodeEnabled) {
		if (isCheckNodeEnabled && isEnabled == false) {
			return false;
		}
		if (key == costArrivedTime.getKey()) {
			if (arrivedTime < costArrivedTime.getArrivedTime()) {
				costArrivedTime.setPrevNode(prevNode);
				costArrivedTime.setArrivedTime(arrivedTime);
				return true;
			}
		} else {
			costArrivedTime.setKey(key);
			costArrivedTime.setPrevNode(prevNode);
			costArrivedTime.setArrivedTime(arrivedTime);
			return true;
		}
		
		return false;
	}
	
	public Node getCostPrevNode(long key) {
		if (costArrivedTime.getKey() == key) {
			return costArrivedTime.getPrevNode();
		}
		return null;
	}
	
	public Node getCostPrevNode(Vehicle vehicle) {
		if (vehicle == costArrivedTime.getVehicle()) {
			return costArrivedTime.getPrevNode();
		}
		return null;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	public double getCostArrivedTime(Vehicle vehicle) {
		if (vehicle == costArrivedTime.getVehicle()) {
			return costArrivedTime.getArrivedTime();
		}
		return OcsConstant.MAXCOST_TIMEBASE;
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public double getCostArrivedTime(long key) {
		if (key == costArrivedTime.getKey()) {
			return costArrivedTime.getArrivedTime();
		}
		return OcsConstant.MAXCOST_TIMEBASE;
	}

	/**
	 * 
	 * @param vehicle
	 */
	public void addRoutedVehicle(VehicleData vehicle) {
		synchronized (routedVehicleList) {
			routedVehicleList.remove(vehicle);
			routedVehicleList.add(vehicle);
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 */
	public void removeRoutedVehicle(VehicleData vehicle) {
		synchronized (routedVehicleList) {
			routedVehicleList.remove(vehicle);
		}
		
		// 2011.12.27 by PMM
		cancelReservationForVehicleDriveIn(vehicle);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getRoutedVehicleCount() {
		return routedVehicleList.size();
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public VehicleData getRoutedVehicle(int index) {
		synchronized (routedVehicleList) {
			if (index < routedVehicleList.size()) {
				return routedVehicleList.get(index);
			}
		}
		return null;
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void checkRepathSearch() {
		// routedVehicle에게 Re PathSearch
		// 2015.02.15 by MYM : 장애 지역 우회 기능 (deadlock 방지를 위해 routedVehicleList clone 후 사용) 
//		synchronized (routedVehicleList) {
//			Iterator<VehicleData> iterator = routedVehicleList.iterator();
//			VehicleData vehicle;
//			while (iterator.hasNext()) {
//				vehicle = (VehicleData)iterator.next();
//				// 2012.03.02. by MYM : [NotNullCheck] 추가
//				if (vehicle != null) {
//					vehicle.setPathSearchRequest(true);
//				}
//			}
//		}
		ArrayList<VehicleData> routedVehicleListClone = null;
		try {
			routedVehicleListClone = (ArrayList<VehicleData>)routedVehicleList.clone();
			Iterator<VehicleData> iterator = routedVehicleListClone.listIterator();
			VehicleData vehicle;
			while (iterator.hasNext()) {
				vehicle = (VehicleData) iterator.next();
				if (vehicle != null) {
					vehicle.setPathSearchRequest(true);
				}
			}
		} catch (Exception e) {
		} finally {
			routedVehicleListClone = null;
		}
		
		// 충돌 설정된 위치의 Vehicle에게 Re PathSearch
		Iterator<Collision> iterator = collisionList.iterator();
		Collision collision;
		VehicleData vehicle;
		while (iterator.hasNext()) {
			collision = (Collision)iterator.next();
			// 2012.03.02. by MYM : [NotNullCheck] 추가
			if (collision != null) {
				vehicle = collision.getVehicle();
				if (vehicle != null) {
					vehicle.setPathSearchRequest(true);
				}
			}
		}
	}
	
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 * @param fromNode
	 */
	@SuppressWarnings("unchecked")
	public void checkRepathSearch(Node fromNode) {
		// routedVehicle에게 Re PathSearch
		ArrayList<VehicleData> routedVehicleListClone = null;
		try {
			routedVehicleListClone = (ArrayList<VehicleData>)routedVehicleList.clone();
			Iterator<VehicleData> iterator = routedVehicleListClone.listIterator();
			VehicleData vehicle;
			while (iterator.hasNext()) {
				vehicle = (VehicleData) iterator.next();
				if (vehicle != null) {
					if (vehicle.getRoutedNodeIndex(fromNode) >= 0
							|| vehicle.getDriveNodeIndex(fromNode) >= 0) {
						vehicle.setPathSearchRequest(true);
					}
				}
			}
		} catch (Exception e) {
		} finally {
			routedVehicleListClone = null;
		}
	}
	
	/**
	 * 
	 * @param abnormalVehicle
	 * @param isNearByDrive
	 */
	public void checkRepathSearch(VehicleData abnormalVehicle, boolean isNearByDrive) {
		// routedVehicle에게 Re PathSearch
		requestRepathSearchToRoutedVehicles(abnormalVehicle);

		// 충돌 설정된 위치의 Vehicle에게 Re PathSearch
		if (isNearByDrive == false) {
			ListIterator<Collision> iterator = collisionList.listIterator();
			Collision collision;
			VehicleData vehicle;
			while (iterator.hasNext()) {
				collision = (Collision)iterator.next();
				// 2012.03.02. by MYM : [NotNullCheck] 추가
				if (collision != null) {
					vehicle = collision.getVehicle();
					if (vehicle != null && vehicle.equals(abnormalVehicle) == false) {
						vehicle.setPathSearchRequest(true);
					}
				}
			}
		} else {
			// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
			Iterator<Block> iter = collisionBlockSet.iterator();
			while (iter.hasNext()) {
				for (Node node : iter.next().getNodeList()) {
					if (this != node) {
						node.requestRepathSearchToRoutedVehicles(abnormalVehicle);
					}
				}
			}
		}
	}
	
	/**
	 * 2014.10.11 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public void releaseAbnormalSection() {
		if (sectionList.size() == 1) {
			// Normal
			// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
//			sectionList.get(0).removeAbnormalItem(this);
			HashSet<Section> detourSectionSet = new HashSet<Section>();
			detourSectionSet.addAll(sectionList.get(0).getDetourSectionSet());
			for (Section section : detourSectionSet) {
				section.removeAbnormalItem(this);
			}
		} else {
			// Diverge/Converge
			// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
//			for (Section section : sectionList) {
//				if (section.getLastNode() == this) {
//					section.removeAbnormalItem(this);
//				}
//			}
			HashSet<Section> detourSectionSet = new HashSet<Section>();
			for (Section section : sectionList) {
				if (section.getLastNode() == this) {
					detourSectionSet.addAll(section.getDetourSectionSet());
				}
			}
			for (Section section : detourSectionSet) {
				section.removeAbnormalItem(this);
			}
		}
	}
	
	/**
	 * 2014.10.11 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public void setAbnormalSection() {
		if (sectionList.size() == 1) {
			// Normal
			// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
//			sectionList.get(0).addAbnormalItem(this, true, DETOUR_REASON.NODE_DISABLED);
			HashSet<Section> detourSectionSet = new HashSet<Section>();
			detourSectionSet.addAll(sectionList.get(0).getDetourSectionSet());
			for (Section section : detourSectionSet) {
				section.addAbnormalItem(this, DETOUR_REASON.NODE_DISABLED);
			}
		} else {
			// Diverge/Converge
			// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
//			for (Section section : sectionList) {
//				if (section.getLastNode() == this) {
//					section.addAbnormalItem(this, true, DETOUR_REASON.NODE_DISABLED);
//				}
//			}
			HashSet<Section> detourSectionSet = new HashSet<Section>();
			for (Section section : sectionList) {
				if (section.getLastNode() == this) {
					detourSectionSet.addAll(section.getDetourSectionSet());
				}
			}
			for (Section section : detourSectionSet) {
				section.addAbnormalItem(this, DETOUR_REASON.NODE_DISABLED);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void requestRepathSearchToRoutedVehicles(VehicleData abnormalVehicle) {
		if (abnormalVehicle != null) {
			ArrayList<VehicleData> routedVehicleListClone = null;
			try {
				routedVehicleListClone = (ArrayList<VehicleData>)routedVehicleList.clone();
				Iterator<VehicleData> iterator = routedVehicleListClone.listIterator();
				VehicleData vehicle;
				while (iterator.hasNext()) {
					vehicle = (VehicleData) iterator.next();
					if (vehicle != null && vehicle.equals(abnormalVehicle) == false) {
						if (abnormalVehicle.isLocatedInDriveNodeList(vehicle) == false) {
							vehicle.setPathSearchRequest(true);
						}
					}
				}
			} catch (Exception e) {
			} finally {
				routedVehicleListClone = null;
			}
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param isNearByDrive
	 * @return
	 */
	synchronized public boolean vehicleInitialize(VehicleData vehicle, boolean isNearByDrive) {
		if (isNearByDrive) {
			return vehicleInitializeForNearby(vehicle);
		} else {
			return vehicleInitialize(vehicle);
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	synchronized private boolean vehicleInitialize(VehicleData vehicle) {
	// 2012.03.14 by PMM
//	private boolean vehicleInitialize(VehicleData vehicle) {
		// 1. driveVehicle 등록
		if (hasAlreadyDrived(vehicle) == false) {
			driveVehicleList.add(vehicle);
		}
		if (driveVehicleList.size() > 1) {
			vehicle.setEStopRequested(true);
			VehicleData drivedVehicle = null;
			for (int i = 0; i < driveVehicleList.size(); i++) {
				drivedVehicle = driveVehicleList.get(i);
				if (drivedVehicle != null) {
					drivedVehicle.setEStopRequested(true);
				}
			}
			return false;
		}
		
		// 2. HID 등록
		// 2012.02.20 by KYK : [NotNullCheck] hid is null ?
		hid.checkMoveIn(vehicle, this, null, true, 0);
		
		// 3. CloseLoop 등록
		checkMoveInCloseLoop(vehicle, true);
		
		// 4. Collision 등록
		String collisionOccupiedVehicle = checkCollisionOccupiedVehicleWhileVehicleInitialize(vehicle);
		if (collisionOccupiedVehicle != null && collisionOccupiedVehicle.length() > 0) {
			vehicle.setEStopRequested(true);
			StringBuffer log = new StringBuffer("Init Fail : ");
			log.append(this.nodeId).append(" ").append("Already ").append(collisionOccupiedVehicle).append(" occupying collision(setCollision)");
			vehicle.setReason(log.toString());
			return false;
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	synchronized private boolean vehicleInitializeForNearby(VehicleData vehicle) {
		// 1. HID 점유 체크 (HID 이상상태 제어 사용 유무에 따라 처리)
		hid.checkMoveIn(vehicle, this, null, true, 0);
		
		// 2015.10.14 by MYM : 근접제어 CloseLoop 체크
		// 1.1. CloseLoop 등록
		checkMoveInCloseLoop(vehicle, true);
		
		// 2. 동일 Vehicle 중복 Drive 체크 
		if (hasAlreadyDrived(vehicle) == false) {
			// 3. DriveVehicleList 해당 Vehicle 점유
			driveVehicleList.add(vehicle);
		}

		// 2014.03.25 by MYM : Hybrid 주행 제어 (Node의 CheckCollision이 TRUE인 경우는 Vehicle 간섭 및 충돌설정 고려 주행)
		if (isCheckCollision()) {
			if (driveVehicleList.size() > 1) {
				vehicle.setEStopRequested(true);
				VehicleData drivedVehicle = null;
				for (int i = 0; i < driveVehicleList.size(); i++) {
					drivedVehicle = driveVehicleList.get(i);
					if (drivedVehicle != null) {
						drivedVehicle.setEStopRequested(true);
					}
				}
				return false;
			}
			
			// 4. Collision 등록
			String collisionOccupiedVehicle = checkCollisionOccupiedVehicleWhileVehicleInitialize(vehicle);
			if (collisionOccupiedVehicle != null && collisionOccupiedVehicle.length() > 0) {
				vehicle.setEStopRequested(true);
				StringBuffer log = new StringBuffer("Init Fail : ");
				log.append(this.nodeId).append(" ").append("Already ").append(collisionOccupiedVehicle).append(" occupying collision(setCollision)");
				vehicle.setReason(log.toString());
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param prevNode
	 * @return
	 */
	synchronized public String vehicleDriveIn(VehicleData vehicle, Node prevNode) {
		String prevNodeId = "";
		if (prevNode != null) {
			prevNodeId = prevNode.getNodeId();
		}
		
		// 2012.01.03 by MYM : Exception 발생에 따른 예외처리
		// 배경 : vehicleDriveIn에서 Exception이 발생한 경우 이전에 DriveIn 한 Node를 DriveOut하지 않아
		//      타 Vehicle이 주행하지 못하는 현상 발생 (M1L, 2012.12.31 22:25)
		try {
			// 1. 동일 노드 중복 Drive 체크 
			if (hasAlreadyDrived(vehicle)) {
				StringBuffer log = new StringBuffer("DriveFail : ");
				log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
				log.append("Duplication Drive");
				return log.toString();
			}

			// 2015.03.19 by MYM, KYK : Deadlock이 Reserved인 경우는 Reserved 정보 무시하고 Drive 시도하도록 변경
			// 배경 : Deadlock Break를 위해 cancelReservationForDeadlockBreak 함수에서 
			//       충돌 노드의 타 Vehicle을 Reserved 해제하다가 Thread Deadlock 발생
			// 2011.12.27 by PMM
//			if (vehicle.getDeadlockType() == DEADLOCK_TYPE.RESERVED) {
//				cancelReservationForDeadlockBreak(vehicle);
//			}
			if (vehicle.getDeadlockType() != DEADLOCK_TYPE.RESERVED) {
				if (isReservedExcept(vehicle)) {
					StringBuffer log = new StringBuffer("DriveFail : ");
					log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
					log.append("Already Reserved by ");
					if (reservedVehicleList.size() > 0) {
						// 2012.01.03 by MYM : reservedVehicle 참조시 Local 변수로 가져와 참조하도록 함.
	//				if (getReservedVehicle() != null) {
	//					log.append(getReservedVehicle().getVehicleId());
	//					vehicle.addVehicleToDriveFailCausedVehicleList(getReservedVehicle());
	//				}
						VehicleData reservedVehicle = getReservedVehicle(); 
						if (reservedVehicle != null) {
							log.append(reservedVehicle.getVehicleId());
							vehicle.addVehicleToDriveFailCausedVehicleList(reservedVehicle);
						}
					}
					
					return log.toString();
				}
				if (isCollisionNodeReservedExcept(vehicle)) {
					StringBuffer log = new StringBuffer("DriveFail : ");
					log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
					log.append("Already CollisionNode:");
					if (reservedCollisionNode != null) {
						log.append(reservedCollisionNode.getNodeId());
						log.append(" Reserved by ");
						// 2012.01.03 by MYM : reservedVehicle 참조시 Local 변수로 가져와 참조하도록 함.
						// 배경 : reservedCollisionNode의 ReservedVehicle이 Null이 될 때 참조하여 Exception 발생(M1A, 2012.12.31 22:25)
	//				if (reservedCollisionNode.getReservedVehicle() != null) {
	//					log.append(reservedCollisionNode.getReservedVehicle().getVehicleId());
	//					vehicle.addVehicleToDriveFailCausedVehicleList(reservedCollisionNode.getReservedVehicle());
	//				}
						VehicleData reservedVehicleAtCollisionNode = reservedCollisionNode.getReservedVehicle();
						if (reservedVehicleAtCollisionNode != null) {
							log.append(reservedVehicleAtCollisionNode.getVehicleId());
							vehicle.addVehicleToDriveFailCausedVehicleList(reservedVehicleAtCollisionNode);
						}
					}
					return log.toString();
				}
			}
			
			// 2. Vehicle 간섭 체크
			// 2011.12.27 by PMM
//		String nodeOccupiedVehicle = getNodeOccupiedVehicle(vehicle);
//		if (nodeOccupiedVehicle != null && nodeOccupiedVehicle.length() > 0) {
//			StringBuffer log = new StringBuffer("DriveFail : ");
//			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
//			log.append("Exist driving vehicle").append(nodeOccupiedVehicle);
//			return log.toString();
//		}
			if (checkNodeOccupiedVehicle(vehicle)) {
				StringBuffer log = new StringBuffer("DriveFail : ");
				log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
				log.append("Exist driving vehicle: ");
				if (vehicle.getDriveFailCausedVehicleList() != null) {
					log.append(vehicle.getDriveFailCausedVehicleList().toString());
				}
				return log.toString();
			}
			
			// 3. Collision 체크
			// 2011.12.27 by PMM
//		String collisionOccupiedVehicle = getCollisionOccupiedVehicle(vehicle);
//		if (collisionOccupiedVehicle != null && collisionOccupiedVehicle.length() > 0) {
//			StringBuffer log = new StringBuffer("DriveFail : ");
//			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
//			log.append("Already ").append(collisionOccupiedVehicle).append(" occupying collision(checkCollision)");
//			return log.toString();
//		}
			if (checkCollisionOccupiedVehicleWhileCheckingCollsion(vehicle)) {
				StringBuffer log = new StringBuffer("DriveFail : ");
				log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
				log.append("Already occupying collision(checkCollision)");
				if (vehicle.getDriveFailCausedVehicleList() != null) {
					log.append(vehicle.getDriveFailCausedVehicleList().toString());
				}
				return log.toString();
			}
			
			// 4. HID 체크
			String checkMoveIn = hid.checkMoveIn(vehicle, this, prevNode, false, 0);
			if (checkMoveIn != null && checkMoveIn.length() > 0) {
				StringBuffer log = new StringBuffer("DriveFail : ");
				log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
				log.append(checkMoveIn);
				return log.toString();
			}
			
			// 5. CloseLoop 체크
			// 2011.12.27 by PMM
//		String checkMoveInCloseLoop = checkMoveInCloseLoop(vehicle, false);
//		if (checkMoveInCloseLoop != null && checkMoveInCloseLoop.length() > 0) {
//			StringBuffer log = new StringBuffer("DriveFail : ");
//			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
//			log.append(checkMoveInCloseLoop);
//			return log.toString();
//		}
			if (checkMoveInCloseLoop(vehicle, false)) {
				StringBuffer log = new StringBuffer("DriveFail : ");
				log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
				log.append(vehicle.getReason());
				if (vehicle.getDriveFailCausedVehicleList() != null) {
					log.append(vehicle.getDriveFailCausedVehicleList().toString());
				}
				return log.toString();
			}
			
			// 6. Collision 체크 등록
			// 2011.12.27 by PMM
//		collisionOccupiedVehicle = setCollisionOccupied(vehicle);
//		if (collisionOccupiedVehicle != null && collisionOccupiedVehicle.length() > 0) {
//			StringBuffer log = new StringBuffer("DriveFail : ");
//			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
//			log.append("Already ").append(collisionOccupiedVehicle).append(" occupying collision(setCollision)");
//			return log.toString();
//		}
			if (checkCollisionOccupiedVehicleWhileSettingCollsion(vehicle)) {
				StringBuffer log = new StringBuffer("DriveFail : ");
				log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
				log.append("Already occupying collision(setCollision)");
				if (vehicle.getDriveFailCausedVehicleList() != null) {
					log.append(vehicle.getDriveFailCausedVehicleList().toString());
				}
				return log.toString();
			}

			// 2015.12.21 by KBS : Patrol VHL 기능 추가
			// 7. Cleaning 체크
			if (vehicle.getPatrolStatus() == '1') {
				if (vehicle.isRepathSearchNeededByPatrolVHL() == true) {
					// Clean 시작인 경우 repath 요청
					requestRepathSearchToRoutedVehicles(vehicle);
					vehicle.setRepathSearchNeededByPatrolVHL(false);
				} else {
					// Clean 수행 중에 section이 변경되는 경우 repath 요청
					if (this.isConverge() || prevNode.isDiverge()) {
						requestRepathSearchToRoutedVehicles(vehicle);
						vehicle.setRepathSearchNeededByPatrolVHL(false);
					}
				}
			}
		} catch (Exception e) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" Abnormal Exception(");
			log.append(e.toString()).append(")");
			return log.toString();
		}
		
		// 7. driveVehicle 등록
		driveVehicleList.add(vehicle);
		return OcsConstant.OK;
	}
	
	/**
	 * 
	 * @param vehicle - vehicle for Drive
	 * @param prevNode
	 * @param forwardVehicleCount - count of forward vehicle
	 * @param isNearByNormalDrive
	 * @return "OK" if normal, reason(String) otherwise
	 */
	synchronized public String vehicleDriveIn(VehicleData vehicle, Node prevNode, int forwardVehicleCount, boolean isNearByNormalDrive) {
		String prevNodeId = "";
		if (prevNode != null) {
			prevNodeId = prevNode.getNodeId();
		}
		
		// 1. 동일 노드 중복 Drive 체크 
		if (hasAlreadyDrived(vehicle)) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
			log.append("Duplication Drive");
			return log.toString();
		}
		
		// 2013.11.11 by MYM : Hybrid 주행 제어(교차로 근접, Vehicle 간섭 및 충돌설정 고려 주행)
		if (isNearByNormalDrive) {
			String result = checkNearbyNormalDrive(vehicle, prevNode);
			if (OcsConstant.OK.equals(result) == false) {
				return result;
			}
		} else if (isCheckCollision()) {
			// 2014.03.25 by MYM : Hybrid 주행 제어 (Node의 CheckCollision이 TRUE인 경우는 Vehicle 간섭 및 충돌설정 고려 주행)
			// 2015.01.07 by zzang9un : PassDoor 주행을 위해 CheckCollistion 기능을 이용함
			// 							PassDoor 전까지만 주행하기 위해 PassDoor Node를 CheckCollision == TRUE로 설정
			String result = checkCollisionInNearbyDrive(vehicle, prevNode);
			if (OcsConstant.OK.equals(result) == false) {
				return result;
			}
		}
		
		// 2.1 HID 체크
		String checkMoveIn = hid.checkMoveIn(vehicle, this, prevNode, false, forwardVehicleCount);
		if (checkMoveIn != null && checkMoveIn.length() > 0) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
			log.append(checkMoveIn);
			return log.toString();
		}
		
		// 2015.10.14 by MYM : 근접제어 CloseLoop 체크 추가
		// 2.2 CloseLoop 체크
		if (checkMoveInCloseLoop(vehicle, false, forwardVehicleCount)) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
			log.append(vehicle.getReason());
			if (vehicle.getDriveFailCausedVehicleList() != null) {
				log.append(vehicle.getDriveFailCausedVehicleList().toString());
			}
			return log.toString();
		}
		
		// 2015.12.21 by KBS : Patrol VHL 기능 추가
		// 2.3 Cleaning 체크
		if (vehicle.getPatrolStatus() == '1') {
			if (vehicle.isRepathSearchNeededByPatrolVHL() == true) {
				// Clean 시작인 경우 repath 요청
				requestRepathSearchToRoutedVehicles(vehicle);
				vehicle.setRepathSearchNeededByPatrolVHL(false);
			} else {
				// Clean 수행 중에 section이 변경되는 경우 repath 요청
				if (this.isConverge() || prevNode.isDiverge()) {
					requestRepathSearchToRoutedVehicles(vehicle);
					vehicle.setRepathSearchNeededByPatrolVHL(false);
				}
			}
		}
		
		// 3. driveVehicleList 해당 Vehicle 점유		
		driveVehicleList.add(vehicle);
		return OcsConstant.OK;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param prevNode
	 * @return "OK" if normal, reason(String) otherwise
	 */
	private String checkNearbyNormalDrive(VehicleData vehicle, Node prevNode) {
		String prevNodeId = "";
		if (prevNode != null) {
			prevNodeId = prevNode.getNodeId();
		}
		
		// 1. Vehicle 간섭 체크
		if (checkNodeOccupiedVehicleForNearby(vehicle)) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
			log.append("Exist driving vehicle: ");
			if (vehicle.getDriveFailCausedVehicleList() != null) {
				log.append(vehicle.getDriveFailCausedVehicleList().toString());
			}
			return log.toString();
		}

		// 2. Collision 체크
		if (checkCollisionOccupiedVehicleWhileCheckingCollsionForNearby(vehicle)) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
			log.append("Already occupying collision(checkCollision)");
			if (vehicle.getDriveFailCausedVehicleList() != null) {
				log.append(vehicle.getDriveFailCausedVehicleList().toString());
			}
			return log.toString();
		}
		
		return OcsConstant.OK;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param prevNode
	 * @return
	 */
	private String checkCollisionInNearbyDrive(VehicleData vehicle, Node prevNode) {
		String prevNodeId = "";
		if (prevNode != null) {
			prevNodeId = prevNode.getNodeId();
		}
		
		if (prevNode.isCheckCollision() == false) {
			if (vehicle.getDriveCurrNode() != prevNode) {
				StringBuffer log = new StringBuffer("DriveFail : ");
				log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
				log.append("NormalDrive Area");
				return log.toString();
			}
		}
		
		// 1. Vehicle 간섭 체크
		if (checkNodeOccupiedVehicle(vehicle)) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
			log.append("Exist driving vehicle: ");
			if (vehicle.getDriveFailCausedVehicleList() != null) {
				log.append(vehicle.getDriveFailCausedVehicleList().toString());
			}
			return log.toString();
		}

		// 2. Collision 체크
		if (checkCollisionOccupiedVehicleWhileCheckingCollsion(vehicle)) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
			log.append("Already occupying collision(checkCollision)");
			if (vehicle.getDriveFailCausedVehicleList() != null) {
				log.append(vehicle.getDriveFailCausedVehicleList().toString());
			}
			return log.toString();
		}

		// 3. Collision Set
		if (checkCollisionOccupiedVehicleWhileSettingCollsion(vehicle)) {
			StringBuffer log = new StringBuffer("DriveFail : ");
			log.append(prevNodeId).append(">").append(this.nodeId).append(" ");
			log.append("Already occupying collision(setCollision)");
			if (vehicle.getDriveFailCausedVehicleList() != null) {
				log.append(vehicle.getDriveFailCausedVehicleList().toString());
			}
			return log.toString();
		}
		
		return OcsConstant.OK;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	synchronized public boolean vehicleDriveOut(VehicleData vehicle) {
		// 1. driveVehicle 해제 
		driveVehicleList.remove(vehicle);

		// 2. HID 해제
		hid.checkMoveOut(vehicle, this);
		
		// 3. CloseLoop 해제
		Iterator<CloseLoop> itCloseLoop = closeLoopList.iterator();
		CloseLoop closeLoop;
		while (itCloseLoop.hasNext()) {
			closeLoop = (CloseLoop)itCloseLoop.next();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (closeLoop != null) {
				closeLoop.checkMoveOut(vehicle, this);
			}
		}
		
		// 4. Collision 해제
		Iterator<Collision> itCollision = collisionList.iterator();
		Collision collision;
		while (itCollision.hasNext()) {
			collision = (Collision)itCollision.next();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (collision != null) {
				collision.setReleased(vehicle, this);
			}
		}
		
		return true;
	}
	
//	/**
//	 * 
//	 * @param vehicle
//	 * @param initialize
//	 * @return
//	 */
//	private String checkMoveInCloseLoop(VehicleData vehicle, boolean initialize) {
//		for (int i = 0; i < closeLoopList.size(); i++) {
//			CloseLoop closeLoop = closeLoopList.get(i);
//			String checkMoveIn = closeLoop.checkMoveIn(vehicle, this, initialize);
//			closeLoop.debugString(vehicle, this, "MoveIn");
//			if (checkMoveIn != null && checkMoveIn.length() > 0) {
//				for (int j = 0; j < i; j++) {
//					closeLoop = closeLoopList.get(j);
//					closeLoop.checkMoveOut(vehicle, this);
//				}
//				return checkMoveIn;
//			}
//		}
//		return NULL;
//	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	public String getCollisionOccupiedVehicle(VehicleData vehicle) {
		VehicleData occupiedVehicle;
		Iterator<Collision> itCollision = collisionList.iterator();
		Collision collision;
		while (itCollision.hasNext()) {
			collision = (Collision)itCollision.next();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (collision != null && collision.isOccupied(vehicle)) {
				occupiedVehicle = collision.getVehicle();
				if (occupiedVehicle != null) {
					// 2012.02.06 by PMM
//					occupiedVehicle.setYieldRequest(true);
//					occupiedVehicle.setYieldRequestedVehicle(vehicle);
					occupiedVehicle.requestYield(vehicle);
					return occupiedVehicle.getVehicleId();
				} else {
					return NO_VEHICLE;
				}
			}
		}
		return NULL;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	private String checkCollisionOccupiedVehicleWhileVehicleInitialize(VehicleData vehicle) {
		Collision collision;
		VehicleData occupiedVehicle;
		for (int i = 0; i < collisionList.size(); i++) {
			collision = collisionList.get(i);
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (collision != null && collision.setOccupied(vehicle, this) == false) {
				occupiedVehicle = collision.getVehicle();
				if (occupiedVehicle != null) {
					occupiedVehicle.setEStopRequested(true);
					return occupiedVehicle.getVehicleId();
				} else {
					return NO_VEHICLE;
				}
			}
		}
		return NULL;
	}
	
//	/**
//	 * 
//	 * @param vehicle
//	 * @return
//	 */
//	private String getNodeOccupiedVehicle(VehicleData vehicle) {
//		StringBuffer nodeOccupiedVehicle = new StringBuffer();
//		Iterator<VehicleData> iterator = driveVehicleList.iterator();
//		VehicleData occupiedVehicle;
//		while (iterator.hasNext()) {
//			occupiedVehicle = (VehicleData)iterator.next();
//			// 2012.03.02 by MYM : [NotNullCheck] 추가
//			if (occupiedVehicle != null && vehicle.equals(occupiedVehicle) == false) {
//				// 양보 요청
//				// 2012.02.06 by PMM
////				occupiedVehicle.setYieldRequest(true);
////				occupiedVehicle.setYieldRequestedVehicle(vehicle);
//				occupiedVehicle.requestYield(vehicle);
//				nodeOccupiedVehicle.append(" ").append(occupiedVehicle.getVehicleId());
//			}
//		}
//
//		return nodeOccupiedVehicle.toString();
//	}
	
	/**
	 * 
	 * @return
	 */
	public int getDriveVehicleCount() {
		return driveVehicleList.size();
	}
	
	/**
	 * 
	 * @return
	 */
	public VehicleData getDriveVehicle() {
		synchronized (driveVehicleList) {
			if (driveVehicleList.size() > 0) {
				return driveVehicleList.get(0);
			}
		}
		return null;
	}
	
	public Object getDriveVehicleListClone() {
		return driveVehicleList.clone();
	}
	
	/**
	 * 2015.05.27 by MYM : Vehicle Traffic 분산
	 */
	public Object getRoutedVehicleListClone() {
		return routedVehicleList.clone();
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public VehicleData getDriveVehicle(int index) {
		synchronized (driveVehicleList) {
			// 2012.07.13 by MYM : 타이밍상 IndexOutOfBoundsException 발생 가능하여 확률 낮추도록 조건 수정 
//			if (driveVehicleList.size() > 0 && index < driveVehicleList.size()) {
			if (index >= 0 && index < driveVehicleList.size()) {
				return driveVehicleList.get(index);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param vehicle
	 */
	public void updateCollision(VehicleData vehicle) {
		synchronized (collisionList) {
			Iterator<Collision> iterator = collisionList.iterator();
			Collision collision;
			while (iterator.hasNext()) {
				collision = (Collision)iterator.next();
				collision.setOccupied(vehicle, this);
			}
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	public boolean hasAlreadyDrived(VehicleData vehicle) {
		if (driveVehicleList.contains(vehicle)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param isFailureOHTDetourSearchUsed
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public boolean isAbnormalState(VehicleData vehicle, boolean isFailureOHTDetourSearchUsed) {
		// 2014.02.06 by KYK
		if (isEnabled == false) {
			return true;
		}
		// 2012.03.02 by MYM : [NotNullCheck] 추가 - vehicleCurrNode, vehicleHid
		Node vehicleStopNode = vehicle.getDriveStopNode();
		if (vehicleStopNode != null && this.equals(vehicleStopNode) == false) {
			// 장애 지역 탈출을 위해 동일 HID는 제외
			Hid vehicleStopHid = vehicleStopNode.getHid();
			if (vehicleStopHid != null && hid.equals(vehicleStopHid) == false &&
					hid.isAbnormalState()) {
				return true;
			}
			
			// 2014.02.06 by KYK : Unload Search 는 무조건 취소되도록 함
//			if (isFailureOHTDetourSearchUsed) {
			if (isFailureOHTDetourSearchUsed || vehicle.getCarrierExist() == '0') {
				try {
					VehicleData drivedVehicle = null;
					ArrayList<VehicleData> driveVehicleListClone = (ArrayList<VehicleData>)driveVehicleList.clone();
					ListIterator<VehicleData> iterator = driveVehicleListClone.listIterator();
					while (iterator.hasNext()) {
						drivedVehicle = (VehicleData)iterator.next();
						if (drivedVehicle != null) {
							if (drivedVehicle.equals(vehicle) == false) {
								if (drivedVehicle.isAbnormalVehicle()) {
									if (drivedVehicle.isLocatedInDriveNodeList(vehicle) == false) {
										if (driveVehicleList.contains(drivedVehicle)) {
											return true;
										}
									}
								}
							}
						}
					}
				} catch (Exception e) {
					// Known Exception: NoSuchElementException
					;/*NULL*/
				}
			}
		}
		return false;
	}
	
	/**
	 * 2015.09.16 by MYM : 비근접은 Collision, 근접은 Block 위치의 장애 Vehicle을 보도록 변경
	 */
	public boolean isAbnormalHid(VehicleData vehicle) {
		Node vehicleStopNode = vehicle.getDriveStopNode();
		if (vehicleStopNode != null && this.equals(vehicleStopNode) == false) {
			// 장애 지역 탈출을 위해 동일 HID는 제외
			Hid vehicleStopHid = vehicleStopNode.getHid();
			if (vehicleStopHid != null && hid.equals(vehicleStopHid) == false &&
					hid.isAbnormalState()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 2015.09.16 by MYM : 2015.09.16 by MYM : 비근접은 Collision, 근접은 Block 위치의 장애 Vehicle을 보도록 변경
	 */
	public VehicleData getAbnormalVehicle(VehicleData vehicle) {
		try {
			VehicleData drivedVehicle = null;
			ArrayList<VehicleData> driveVehicleListClone = (ArrayList<VehicleData>)driveVehicleList.clone();
			ListIterator<VehicleData> iterator = driveVehicleListClone.listIterator();
			while (iterator.hasNext()) {
				drivedVehicle = (VehicleData)iterator.next();
				if (drivedVehicle != null) {
					if (drivedVehicle.equals(vehicle) == false) {
						if (drivedVehicle.isAbnormalVehicle()) {
							if (drivedVehicle.isLocatedInDriveNodeList(vehicle) == false) {
								if (driveVehicleList.contains(drivedVehicle)) {
									return drivedVehicle;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// Known Exception: NoSuchElementException
			;/*NULL*/
		}
		return null;
	}
	
	// 2011.10.26 by PMM
	// SearchFail 시 trace를 위한 로그 찍기 위해 추가.
	/**
	 * 
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public String getAbnormalVehicleState(VehicleData vehicle) {
		VehicleData drivedVehicle = null;
		StringBuffer message = new StringBuffer();
		// 2012.03.14 by PMM
//		VehicleData drivedVehicle = null;
//		for (int i = 0; i < driveVehicleList.size(); i++) {
//			drivedVehicle = driveVehicleList.get(i);
		ArrayList<VehicleData> driveVehicleListClone = (ArrayList<VehicleData>)driveVehicleList.clone();
		Iterator<VehicleData> iterator = driveVehicleListClone.iterator();
		while (iterator.hasNext()) {
			drivedVehicle = (VehicleData)iterator.next();
			if (drivedVehicle != null) {
				if (drivedVehicle.equals(vehicle) == false) {
					if (drivedVehicle.isAbnormalVehicle()) {
						if (drivedVehicle.isLocatedInDriveNodeList(vehicle) == false) {
							message.append("VehicleId:").append(drivedVehicle.getVehicleId());
							message.append(" Mode:").append(drivedVehicle.getVehicleMode());
							message.append(" State:").append(drivedVehicle.getState());
							message.append(" ErrorCode:").append(drivedVehicle.getErrorCode());
							message.append(" AlarmCode:").append(drivedVehicle.getAlarmCode());
							return message.toString();
						}
					}
				}
			}
		}
		return "";
	}
	
	synchronized public void makeReservationForVehicleDriveIn(VehicleData vehicle) {
		if (isCollisionNodeReservedExcept(vehicle) == false) {
			if (reservedVehicleList.contains(vehicle) == false) {
				reservedVehicleList.add(vehicle);
				reservedTimeTable.put(vehicle, System.currentTimeMillis());
			}
		}
	}
	
		// 2015.03.19 by MYM, KYK : Deadlock이 Reserved인 경우는 Reserved 정보 무시하고 Drive 시도하도록 변경 (미사용 함수 주석 처리)
//	synchronized public void cancelReservationForDeadlockBreak(VehicleData vehicle) {
//	private void cancelReservationForDeadlockBreak(VehicleData vehicle) {
//		if (reservedVehicleList.contains(vehicle) == false) {
//			reservedVehicleList.add(vehicle);
//			reservedTimeTable.put(vehicle, System.currentTimeMillis());
//		}
//		int checkCount = 0;
//		int checkCountLimit = reservedVehicleList.size();
//		while (true) {
//			if (reservedVehicleList.get(0) == vehicle) {
//				break;
//			} else {
//				reservedTimeTable.remove(reservedVehicleList.get(0));
//				reservedVehicleList.remove(0);
//			}
//			if (++checkCount >= checkCountLimit) {
//				break;
//			}
//		}
//		cancelAllCollisionNodeReservationForDeadlockBreak(vehicle);
//	}
//	
////	synchronized public void cancelAllCollisionNodeReservationForDeadlockBreak(VehicleData vehicle) {
//	private void cancelAllCollisionNodeReservationForDeadlockBreak(VehicleData vehicle) {
//		if (containsDriveVehicleList(vehicle) == false) {
//			Iterator<Collision> itCollision = collisionList.iterator();
//			Collision collision;
//			Node collisionNode;
//			while (itCollision.hasNext()) {
//				collision = (Collision)itCollision.next();
//				if (collision != null) {
//					collisionNode = collision.getCollisionNode(this);
//					if (collisionNode != null) {
//						if (collisionNode.isReservedExcept(vehicle)) {
//							if (isCollisionNodeReservationEarlier(vehicle, collisionNode)) {
//								collisionNode.cancelCollisionNodeReservationForDeadlockBreak(vehicle);
//							}
//						}
//					}
//				}
//			}
//		}
//	}
//	
//	synchronized public void cancelCollisionNodeReservationForDeadlockBreak(VehicleData vehicle) {
//		int checkCount = 0;
//		int checkCountLimit = reservedVehicleList.size();
//		while (true) {
//			if (reservedVehicleList.get(0) == vehicle) {
//				break;
//			} else {
//				reservedTimeTable.remove(reservedVehicleList.get(0));
//				reservedVehicleList.remove(0);
//			}
//			if (++checkCount >= checkCountLimit) {
//				break;
//			}
//		}
//	}
//	
//	public void cancelReservationForVehicleDriveIn(VehicleData vehicle) {
//		if (reservedVehicleList.contains(vehicle)) {
//			reservedVehicleList.remove(vehicle);
//			reservedTimeTable.remove(vehicle);
//		}
//	}
	/**
	 * 2014.10.13 : void -> boolean 으로 변경
	 * @param vehicle
	 * @return
	 */
//	synchronized public boolean cancelReservationForVehicleDriveIn(VehicleData vehicle) {
	public boolean cancelReservationForVehicleDriveIn(VehicleData vehicle) {
		reservedTimeTable.remove(vehicle);
		return reservedVehicleList.remove(vehicle);
	}
	
	private Node reservedCollisionNode;
//	public boolean isCollisionNodeReservedExcept(VehicleData vehicle) {
	private boolean isCollisionNodeReservedExcept(VehicleData vehicle) {
		if (containsDriveVehicleList(vehicle) == false) {
			Iterator<Collision> itCollision = collisionList.iterator();
			Collision collision;
			Node collisionNode;
			while (itCollision.hasNext()) {
				collision = (Collision)itCollision.next();
				if (collision != null) {
					collisionNode = collision.getCollisionNode(this);
					if (collisionNode != null) {
						// 2012.05.23 by PMM
						if (vehicle.containsDriveNode(collisionNode) == false) {
							if (collisionNode.isReservedExcept(vehicle)) {
								if (isCollisionNodeReservationEarlier(vehicle, collisionNode)) {
									reservedCollisionNode = collisionNode;
									return true;
								}
							}
						}
					}
				}
			}
		}
		reservedCollisionNode = null;
		return false;
	}
	
	private boolean isCollisionNodeReservationEarlier(VehicleData vehicle, Node collisionNode) {
		synchronized (reservedVehicleList) {
			if (vehicle != null && collisionNode != null) {
				VehicleData collisionReservedVehicle = collisionNode.getReservedVehicle(); 
				if (collisionReservedVehicle != null) {
					long reservedTime = getReservedTime(vehicle);
					long collisionReservedTime = collisionNode.getReservedTime(collisionReservedVehicle);
					if (reservedTime > collisionReservedTime) {
						return true;
					} else if (reservedTime == collisionReservedTime) {
						if (vehicle.getVehicleId().compareTo(collisionReservedVehicle.getVehicleId()) > 0) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean containsDriveVehicleList(VehicleData vehicle) {
		if (driveVehicleList.contains(vehicle) ||
				vehicle.containsDriveNode(this)) {
			return true;
		}
		return false;
	}
	
	public boolean isReservedExcept(VehicleData vehicle) {
		if (vehicle != null) {
			// 2012.07.09 by PMM
			VehicleData reservedVehicle = getReservedVehicle();
			if (reservedVehicle != null) {
				if (reservedVehicle != vehicle) {
					if (reservedVehicle.containsDriveFailCausedVehicleList(vehicle) == false) {
						// 2012.09.04 by PMM
						// 불필요하고, 분기에서 DriveFail된 IdleVHL에 대해 재양보 검색하기 위해 resetTargetNode하는 곳과 충돌되어 FIFO 제어가 안됨.
//						reservedVehicle.requestYield(vehicle);
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public VehicleData getReservedVehicle() {
		if (reservedVehicleList.size() > 0) {
			return reservedVehicleList.get(0);
		}
		return null;
	}
	
	public long getReservedTime(VehicleData vehicle) {
		if (reservedTimeTable.containsKey(vehicle)) {
			return reservedTimeTable.get(vehicle);
		}
		return System.currentTimeMillis();
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean checkNodeOccupiedVehicle(VehicleData vehicle) {
		// 2012.03.14 by PMM
//		Iterator<VehicleData> iterator = driveVehicleList.iterator();
		ArrayList<VehicleData> driveVehicleListClone = (ArrayList<VehicleData>)driveVehicleList.clone();
		Iterator<VehicleData> iterator = driveVehicleListClone.iterator();
		VehicleData occupiedVehicle;
		boolean result = false;
		while (iterator.hasNext()) {
			occupiedVehicle = (VehicleData)iterator.next();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (occupiedVehicle != null && vehicle.equals(occupiedVehicle) == false) {
				// 양보 요청
				// 2012.02.06 by PMM
//				occupiedVehicle.setYieldRequest(true);
//				occupiedVehicle.setYieldRequestedVehicle(vehicle);
				occupiedVehicle.requestYield(vehicle);
				vehicle.addVehicleToDriveFailCausedVehicleList(occupiedVehicle);
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	public boolean checkCollisionOccupiedVehicleWhileCheckingCollsion(VehicleData vehicle) {
		VehicleData occupiedVehicle;
		Iterator<Collision> itCollision = collisionList.iterator();
		Collision collision;
		boolean result = false;
		while (itCollision.hasNext()) {
			collision = (Collision)itCollision.next();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (collision != null && collision.isOccupied(vehicle)) {
				occupiedVehicle = collision.getVehicle();
				if (occupiedVehicle != null) {
					// collsionOccupiedVehicle은 복수가 될 수 없음.
					// 안전 코드 및 통일성 유지를 위해 List 대응.
					// 2012.02.06 by PMM
//					occupiedVehicle.setYieldRequest(true);
//					occupiedVehicle.setYieldRequestedVehicle(vehicle);
					occupiedVehicle.requestYield(vehicle);
					vehicle.addVehicleToDriveFailCausedVehicleList(occupiedVehicle);
					result = true;
				}
			}
		}
		return result;
	}
	
	/**
	 * 2013.11.11 by MYM : Hybrid 주행 제어(교차로 근접, Vehicle 간섭 및 충돌설정 고려 주행)
	 * 
	 * @param vehicle
	 * @return
	 */
	private boolean checkNodeOccupiedVehicleForNearby(VehicleData vehicle) {
		// 2012.03.14 by PMM
		ArrayList<VehicleData> driveVehicleListClone = (ArrayList<VehicleData>)driveVehicleList.clone();
		Iterator<VehicleData> iterator = driveVehicleListClone.iterator();
		VehicleData occupiedVehicle;
		boolean result = false;
		while (iterator.hasNext()) {
			occupiedVehicle = (VehicleData)iterator.next();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (occupiedVehicle != null && vehicle.equals(occupiedVehicle) == false) {
				Node currNode = occupiedVehicle.getDriveCurrNode();
				if (this == currNode) {
					// 양보 요청
					occupiedVehicle.requestYield(vehicle);
					vehicle.addVehicleToDriveFailCausedVehicleList(occupiedVehicle);
					result = true;
				}
			}
		}
		return result;
	}
	
	/**
	 * 2013.11.11 by MYM : Hybrid 주행 제어(교차로 근접, Vehicle 간섭 및 충돌설정 고려 주행)
	 * 
	 * @param vehicle
	 * @return
	 */
	public boolean checkCollisionOccupiedVehicleWhileCheckingCollsionForNearby(VehicleData vehicle) {
		VehicleData occupiedVehicle;
		Iterator<Collision> itCollision = collisionList.iterator();
		Collision collision;
		boolean result = false;
		Node driveStopNode = vehicle.getDriveStopNode();
		while (itCollision.hasNext()) {
			collision = (Collision)itCollision.next();
			if (collision != null) {
				Node node = collision.getCollisionNode(this);
				if (node == null) {
					continue;
				}
				
				ArrayList<VehicleData> driveVehicleListClone = (ArrayList<VehicleData>) node.getDriveVehicleListClone();
				Iterator<VehicleData> iterator = driveVehicleListClone.iterator();
				while (iterator.hasNext()) {
					occupiedVehicle = (VehicleData)iterator.next();
					if (occupiedVehicle != null && vehicle.equals(occupiedVehicle) == false) {
						if (occupiedVehicle.getDriveNodeIndex(driveStopNode) < 0) {
							for (Section section : sectionList) {
								int collisionNodeIndex = section.getNodeIndex(node);
								if (collisionNodeIndex == -1 || collisionNodeIndex > section.getNodeIndex(this)) {
									occupiedVehicle.requestYield(vehicle);
									vehicle.addVehicleToDriveFailCausedVehicleList(occupiedVehicle);
									result = true;
								}
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	public boolean checkCollisionOccupiedVehicleWhileSettingCollsion(VehicleData vehicle) {
		Collision collision;
		Collision releaseCollision;
		VehicleData occupiedVehicle;
		boolean result = false;
		for (int i = 0; i < collisionList.size(); i++) {
			collision = collisionList.get(i);
			// 2012.03.02 by KYK : [NotNullCheck] 추가
			if (collision != null && collision.setOccupied(vehicle, this) == false) {
				// 점유한 Collision 해제
				for (int j = 0; j < i; j++) {
					releaseCollision = collisionList.get(j);
					releaseCollision.setReleased(vehicle, this);
				}
				
				// 양보 요청
				occupiedVehicle = collision.getVehicle();
				if (occupiedVehicle != null) {
					// collsionOccupiedVehicle은 복수가 될 수 없음.
					// 안전 코드 및 통일성 유지를 위해 List 대응.
					// 2012.02.06 by PMM
//					occupiedVehicle.setYieldRequest(true);
//					occupiedVehicle.setYieldRequestedVehicle(vehicle);
					occupiedVehicle.requestYield(vehicle);
					vehicle.addVehicleToDriveFailCausedVehicleList(occupiedVehicle);
					result = true;
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param initialize
	 * @return
	 */
	private boolean checkMoveInCloseLoop(VehicleData vehicle, boolean initialize) {
		for (int i = 0; i < closeLoopList.size(); i++) {
			CloseLoop closeLoop = closeLoopList.get(i);
//			closeLoop.debugString(vehicle, this, "MoveIn");
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (closeLoop != null && closeLoop.checkMoveIn(vehicle, this, initialize, 0)) {
				for (int j = 0; j < i; j++) {
					closeLoop = closeLoopList.get(j);
					closeLoop.checkMoveOut(vehicle, this);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 2015.10.14 by MYM : 근접제어 CloseLoop 체크 추가 (전방 Vehicle 체크)
	 * 
	 * @param vehicle
	 * @param initialize
	 * @param forwardVehicleCount
	 * @return
	 */
	private boolean checkMoveInCloseLoop(VehicleData vehicle, boolean initialize, int forwardVehicleCount) {
		for (int i = 0; i < closeLoopList.size(); i++) {
			CloseLoop closeLoop = closeLoopList.get(i);
			if (closeLoop != null && closeLoop.checkMoveIn(vehicle, this, initialize, forwardVehicleCount)) {
				for (int j = 0; j < i; j++) {
					closeLoop = closeLoopList.get(j);
					closeLoop.checkMoveOut(vehicle, this);
				}
				return true;
			}
		}
		return false;
	}
	
	public boolean isCloseloopSet() {
		if (closeLoopList.size() > 0) {
			return true;
		}
		return false;
	}
	
	public void setStopAllowed(boolean isStopAllowed) {
		this.isStopAllowed = isStopAllowed;
	}
	
	public boolean isStopAllowed() {
		if (isVirtual) {
			return false;
		}
		return isStopAllowed;
	}
	
	// 2011.12.27 by PMM
	public void checkStopAllowed() {
		assert sectionList != null;
		assert collisionList != null;
		
		if (isDiverge() == false && isConverge() == false) {
			if (sectionList.size() > 0) {
				Section section = sectionList.get(0);
				if (section != null &&
						LINE.equals(section.getType()) &&
						section.getLastNode() != null &&
						section.getLastNode().isConverge()) {
					if (collisionList.size() > 0) {
						Collision collision;
						@SuppressWarnings("unchecked")
						// 2012.09.17 by PMM
						// ConcurrentModificationException 방지.
						ArrayList<Collision> collisionListClone = (ArrayList<Collision>)collisionList.clone();
						ListIterator<Collision> it = collisionListClone.listIterator();
						String collisionNodeId;
						while (it.hasNext()) {
							collision = it.next();
							if (collision != null) {
								collisionNodeId = collision.getCollisionNodeId(nodeId);
								if (collisionNodeId != null) {
									if (section.contains(collisionNodeId) == false) {
										isStopAllowed = false;
										return;
									}
								}
							}
						}
					}
				}
			}
		}
		isStopAllowed = true;
	}
	
	@SuppressWarnings("unchecked")
	public double getApproachingVehicleIndex() {
		double vehicleIndex = 0.0;
		ArrayList<VehicleData> routedVehicleListClone = null;
		try {
			routedVehicleListClone = (ArrayList<VehicleData>)routedVehicleList.clone();
			int nodeIndex = 0;
			for (VehicleData vehicle : routedVehicleListClone) {
				if (vehicle != null) {
					nodeIndex = vehicle.getRoutedNodeIndex(this);
					if (nodeIndex < 40) {
						vehicleIndex += 1;
					} else if (nodeIndex < 80) {
						vehicleIndex += 20 / nodeIndex;
					} else {
						vehicleIndex += 0.1;
					}
				}
			}
		} catch (Exception e) {
			return routedVehicleList.size();
		} finally {
			routedVehicleListClone = null;
		}
		return vehicleIndex;
	}
	
	@SuppressWarnings("unchecked")
	public double getApproachingVehicleIndex(int index) {
		double vehicleIndex = 0.0;
		ArrayList<VehicleData> routedVehicleListClone = null;
		try {
			routedVehicleListClone = (ArrayList<VehicleData>)routedVehicleList.clone();
			int nodeIndex = 0;
			for (VehicleData vehicle : routedVehicleListClone) {
				if (vehicle != null) {
					nodeIndex = vehicle.getRoutedNodeIndex(this);
					if (nodeIndex < index - 40) {
						vehicleIndex += 0.1;
					} else if (nodeIndex < index - 20) {
						vehicleIndex += 0.5;
					} else if (nodeIndex < index) {
						vehicleIndex += 1;
					} else if (nodeIndex < index + 10) {
						vehicleIndex += 0.2;
					} else {
						vehicleIndex += 0.1;
					}
				}
			}
		} catch (Exception e) {
			return routedVehicleList.size();
		} finally {
			routedVehicleListClone = null;
		}
		return vehicleIndex;
	}
	
	public boolean isCheckCollision() {
		return checkCollision;
	}

	public void setCheckCollision(boolean checkCollision) {
		this.checkCollision = checkCollision;
	}

}