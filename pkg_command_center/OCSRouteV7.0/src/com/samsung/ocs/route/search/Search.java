package com.samsung.ocs.route.search;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.SEARCH_TYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.manager.impl.LocalGroupInfoManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.SectionManager;
import com.samsung.ocs.manager.impl.StationManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.ZoneControlManager;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.PassDoor;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.manager.impl.model.VehicleData;

/**
 * Search Abstract, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date 2011. 6. 21.
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

public abstract class Search {
	protected NodeManager nodeManager;
	protected SectionManager sectionManager;
	protected VehicleManager vehicleManager;
	protected OCSInfoManager ocsInfoManager;
	protected TrCmdManager trCmdManager;
	protected LocalGroupInfoManager localGroupInfoManager;
	protected ZoneControlManager zoneControlManager;
	protected StationManager stationManager;
//	protected PassDoorManager passDoorManager; // 2015.02.10 by zzang9un
	
	private static final String ROUTE_TRACE = "RouteDebug";
	private static Logger traceLog = Logger.getLogger(ROUTE_TRACE);
	private static final String ROUTE_EXCEPTION = "RouteException";
	private static Logger traceExceptionLog = Logger.getLogger(ROUTE_EXCEPTION);
	
	protected boolean isNearByDrive = false;
	
	protected HashSet<String> driveAllowedSet;
	protected HashMap<String, Double> penaltyMap;
	
	protected static final double MAXCOST_TIMEBASE = 9999.0;
	protected static final long MAXCOST_DISTANCEBASE = 99999999;
	
	protected double goingVehiclePenalty = 0.1;
	protected double stoppingVehiclePenalty = 1;
	protected double workingVehiclePenalty = 2;
	protected double cleaningVehiclePenalty = 10;
	protected double manualVehiclePenalty = 300;
	protected double errorVehiclePenalty = 300;
	
	protected boolean isDynamicRoutingUsed = true;
	protected double congestionCountThreshold = 20;
	protected double congestionIndexThreshold = 10;
	protected long congestionTimeThreshold = 120000;
	// 2015.05.27 by MYM : Vehicle Traffic 분산
	protected boolean isTrafficUpdateUsed = true;
	
	// 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
	protected boolean isFailureOHTDetourSearchUsed = true; 
	protected boolean isCollisionNodeCheckUsed = true; 
	protected boolean isCollisionNodeCheckNeeded = true;
	protected boolean isPassDoorControlUsed = false;	// 2015.03.13 by zzang9un : OCS 파라미터 변수
	// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//	protected HashSet<Node> abnormalVehiclesCollisionNodeSet;
	protected ConcurrentHashMap<Node, VehicleData> abnormalVehiclesOnCollisionMap;
	protected static final int MAX_SECTION_COUNT_FOR_VEHICLEPENALTY = 10;
	
	/**
	 * Constructor of Search class.
	 */
	protected Search() {
		this.nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		this.sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		this.ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		this.vehicleManager = VehicleManager.getInstance(null, null, false, false, 0);
		this.localGroupInfoManager = LocalGroupInfoManager.getInstance(null, null, false, false, 0);
		this.zoneControlManager = ZoneControlManager.getInstance(null, null, false, false, 0);
		this.trCmdManager = TrCmdManager.getInstance(null, null, false, false, 0);
		this.stationManager = StationManager.getInstance(Station.class, null, false, false, 0);
		
		// 2015.02.10 by zzang9un
//		this.passDoorManager = PassDoorManager.getInstance(PassDoor.class, null, false, false, 0);
		
		isNearByDrive = ocsInfoManager.isNearByDrive();
		
		driveAllowedSet = zoneControlManager.getDriveAllowedSet();
		penaltyMap = zoneControlManager.getPenaltyMap();
		updateOperationalParameters();
	}
	
	/**
	 * 
	 * 
	 * @param vehicleId String
	 * @param serchType serchType
	 */
	protected void initRouteSearchInfo(String vehicleId, SEARCH_TYPE serchType) {
		for (Enumeration<Object> e = nodeManager.getData().elements(); e.hasMoreElements();) {
			Node node = (Node) e.nextElement();
			node.setArrivedTime(vehicleId, MAXCOST_TIMEBASE);
		}

		if (SEARCH_TYPE.SECTION == serchType) {
			for (Enumeration<Object> e = sectionManager.getData().elements(); e.hasMoreElements();) {
				Section section = (Section) e.nextElement();
//				section.setArrivedTime(vehicleId, MAXCOST_TIMEBASE);
				section.setArrivedTime(vehicleId, MAXCOST_TIMEBASE, 0, 0, 0);
			}
		}
	}
	
	/**
	 * 
	 * @param serchType
	 */
	public void initCostSearchInfo(SEARCH_TYPE serchType) {
		for (Enumeration<Object> e = nodeManager.getData().elements(); e.hasMoreElements();) {
			Node node = (Node) e.nextElement();
			node.initCostArrivedTime();
		}

		if (SEARCH_TYPE.SECTION == serchType) {
			for (Enumeration<Object> e = sectionManager.getData().elements(); e.hasMoreElements();) {
				Section section = (Section) e.nextElement();
				section.setArrivedTime("", MAXCOST_TIMEBASE, 0, 0, 0);
			}
		}		
	}
	
	/**
	 * 
	 * 
	 * @param vehicleId String
	 * @param serchType serchType
	 */
	protected void removeRouteSearchInfo(String vehicleId, SEARCH_TYPE serchType) {
		for (Enumeration<Object> e = nodeManager.getData().elements(); e.hasMoreElements();) {
			Node node = (Node) e.nextElement();
			node.removeArrivedTimeInfo(vehicleId);
		}

		if (SEARCH_TYPE.SECTION == serchType) {
			for (Enumeration<Object> e = sectionManager.getData().elements(); e.hasMoreElements();) {
				Section section = (Section) e.nextElement();
				section.removeArrivedTimeInfo(vehicleId);
			}
		}
	}
	
	/**
	 * 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
	 * 
	 * @param vehicleId
	 * @param serchType
	 * @param nodeList
	 */
	protected void removeRouteSearchInfo(String vehicleId, SEARCH_TYPE serchType, LinkedList<Node> nodeList) {
		try {
			boolean isFirstNode = true;
			for (Node node : nodeList) {
				for (Section section : node.getSection()) {
					if (SEARCH_TYPE.SECTION == serchType) {
						section.removeArrivedTimeInfo(vehicleId);
					}
					if (isFirstNode == false && node != section.getFirstNode()) {
						continue;
					}
					isFirstNode = false;
					ArrayList<Node> sectionNodeList = section.getNodeList();
					for (Node removeNode : sectionNodeList) {
						removeNode.removeArrivedTimeInfo(vehicleId);
					}					
				}
			}
		} catch (Exception e) {
			traceException("resetRouteSearchInfo", e);
		}
	}
	
	/**
	 * 
	 * 
	 * @param vehicleId String
	 * @param node Node
	 * @param queueNodeList LinkedList<Node>
	 */
	protected void addQueueNode(String vehicleId, Node node, LinkedList<Node> queueNodeList) {
		for (int i = 0; i < queueNodeList.size(); i++) {
			if (node.getArrivedTime(vehicleId) < (queueNodeList.get(i)).getArrivedTime(vehicleId)) {
				queueNodeList.add(i, node);
				return;
			}
		}
		queueNodeList.add(node);
	}
	
	@Deprecated
	protected void addCostQueueNode(Vehicle vehicle, Node node, LinkedList<Node> queueNodeList) {
		double arrivalTime = node.getCostArrivedTime(vehicle);
		for (int i = 0; i < queueNodeList.size(); i++) {
			if (arrivalTime < (queueNodeList.get(i)).getCostArrivedTime(vehicle)) {
				queueNodeList.add(i, node);
				return;
			}
		}
		queueNodeList.add(node);
	}
	
	@Deprecated
	protected void addCostQueueNode(long key, Node node, LinkedList<Node> queueNodeList) {
		double arrivalTime = node.getCostArrivedTime(key);
		for (int i = 0; i < queueNodeList.size(); i++) {
			if (arrivalTime < (queueNodeList.get(i)).getCostArrivedTime(key)) {
				queueNodeList.add(i, node);
				return;
			}
		}
		queueNodeList.add(node);
	}
	
	/**
	 * get QueuedNode from queueNodeList
	 * 
	 * @param queueNodeList LinkedList<Node>
	 * @return queuedNode Node
	 */
	protected Node getQueuedNode(LinkedList<Node> queueNodeList) {
		if (queueNodeList.size() == 0)
			return null;
		return queueNodeList.remove(0);
	}
	
	/**
	 * 
	 * 
	 * @param vehicleId String
	 * @param section Section
	 * @param queueSectionList LinkedList<Section>
	 */
	protected void addQueueSection(String vehicleId, Section section, LinkedList<Section> queueSectionList) {
		for (int i = 0; i < queueSectionList.size(); i++) {
			if (section.getArrivedTime(vehicleId) < (queueSectionList.get(i)).getArrivedTime(vehicleId)) {
				queueSectionList.add(i, section);
				return;
			}
		}
		queueSectionList.add(section);
	}
	
	/**
	 * get QueuedSection from queueSectionList
	 * 
	 * @param queueSectionList LinkedList<Section>
	 * @return queuedSection Section
	 */
	protected Section getQueuedSection(LinkedList<Section> queueSectionList) {
		if (queueSectionList.size() == 0)
			return null;
		return queueSectionList.remove(0);
	}
	
	/**
	 * check DriveAllowance
	 * 
	 * @param vehicleZone String
	 * @param nodeZone String
	 * @return true, if Drive Allowed
	 */
	protected boolean isDriveAllowed(String vehicleZone, String nodeZone) {
		if (vehicleZone.equals(nodeZone)) {
			return true;
		} else {
			if (driveAllowedSet.contains(vehicleZone + "_" + nodeZone)) {
				return true;
			}
		}
		return false;
	}	
	
	/**
	 * get ZoneOver Penalty
	 * 
	 * @param vehicleZone String
	 * @param nodeZone String
	 * @return ZoneOverPenalty double
	 */
	protected double getZoneOverPenalty(String vehicleZone, String nodeZone) {
		if (vehicleZone.equals(nodeZone)) {
			return 0;
		} else {
			String key = vehicleZone + "_" + nodeZone;
			if (penaltyMap.containsKey(key)) {
				return penaltyMap.get(key);
			}
		}
		return 9999;
	}
	
	protected double getCleanPenalty(Node node) {
		double cleanPenalty = 0.0;
		VehicleData vehicleData = null;
		for (int i = 0; i < node.getDriveVehicleCount(); i++) {
			vehicleData = node.getDriveVehicle(i);
			if (vehicleData != null) {
				if (node == vehicleData.getDriveCurrNode()) {
					if (vehicleData.getVehicleMode() == 'A') {
						if (vehicleData.getPatrolStatus() == '1') {
							cleanPenalty += cleaningVehiclePenalty;
						}
					}
				}
			}
		}
		return cleanPenalty;
	}
	
	/**
	 * get Vehicle Penalty
	 * 
	 * @param vehicle Vehicle
	 * @param toNode Node
	 * @return VehiclePenalty double
	 */
	protected double getVehiclePenalty(Node node) {
		double vehiclePenalty = 0.0;
		VehicleData vehicleData = null;
		for (int i = 0; i < node.getDriveVehicleCount(); i++) {
			vehicleData = node.getDriveVehicle(i);
			if (vehicleData != null) {
				if (node == vehicleData.getDriveCurrNode()) {
					if (vehicleData.getVehicleMode() == 'A') {
						if (vehicleData.isAbnormalVehicle()) {
							vehiclePenalty += errorVehiclePenalty;
						} else if (vehicleData.getRequestedType() == REQUESTEDTYPE.STAGEWAIT) {
							// 2014.06.03 by MYM : [Stage Locate 기능] Stage 위치에서 대기 중인 Vehicle Penalty 부여
							double stageWaitVehiclePenalty = 0;
							try {
								stageWaitVehiclePenalty = Double.valueOf(vehicleData.getRequestedData());
								if (stageWaitVehiclePenalty < 0) {
									stageWaitVehiclePenalty = 0;
								} else if (stageWaitVehiclePenalty > 50) {
									stageWaitVehiclePenalty = 50;
								}
							} catch (Exception e) {
							}
							vehiclePenalty += stageWaitVehiclePenalty;
						} else {
							switch (vehicleData.getState()) {
								case 'U':
								case 'L':
									vehiclePenalty += workingVehiclePenalty;
									break;
								case 'G':
									vehiclePenalty += goingVehiclePenalty;
									break;
								default:
									if (vehicleData.getState() == 'A' &&
											vehicleData.getStopNode().equals(vehicleData.getTargetNode()) == false) {
										vehiclePenalty += stoppingVehiclePenalty;
									} else {
										vehiclePenalty += goingVehiclePenalty;
									}
									break;
							}
						}
					} else {
						vehiclePenalty += manualVehiclePenalty;
					}
				} else if (node == vehicleData.getDriveTargetNode()) {
					// Source/Dest Node에 도착할 VHL
					if (trCmdManager.isVehicleRegistered(vehicleData.getVehicleId()) &&
							vehicleData.getDriveStopNode() == vehicleData.getDriveTargetNode()) {
						vehiclePenalty += workingVehiclePenalty * 2;
					}
				}
			}
		}
		return vehiclePenalty;
	}
	
	protected double getCongestionPenalty(Node node, double arrivedTime, int index) {
		double penalty = 0;
		if (node != null) {
			VehicleData vehicle = node.getDriveVehicle();
			if (vehicle != null) {
				if (node.equals(vehicle.getDriveStopNode())) {
					if (vehicle.getTargetNode().equals(vehicle.getStopNode()) == false) {
						if (vehicle.getState() == 'A') {
							if (vehicle.getReason().indexOf("DriveFail") > 0) {
								long waitedTime = System.currentTimeMillis() - vehicle.getStateChangedTime();
								if (waitedTime > congestionTimeThreshold) {
									trace("WaitLong: " + vehicle.getVehicleId() + " at N" + node.getNodeId() + " during " + ((waitedTime) / 1000) + " sec.");
									return 300;
								} else {
									penalty += 3 * (waitedTime / congestionTimeThreshold);
								}
							}
						} else {
							double tempPenalty = 0;
							if (node.getNodeId().equals(vehicle.getStopNode())) {
								if (isNearByDrive) {
									if (vehicle.getPauseType() == 1 && vehicle.getState() == 'G') {
										tempPenalty = 1;
									}
								} else {
									int driveNodeCount = vehicle.getDriveNodeCount();
									if (driveNodeCount < 4) {
										tempPenalty = 1;
									} else {
										tempPenalty = 0.2;
									}
								}
							}
							if (index < 40) {
								penalty += tempPenalty;
							} else if (index < 80) {
								penalty += tempPenalty / 2;
							} else {
								penalty += tempPenalty / 4;
							}
						}
					}
				}
			}
			if (node.isConverge() || node.isDiverge()) {
				double vehicleIndex = node.getApproachingVehicleIndex(index);
				if (vehicleIndex > congestionIndexThreshold) {
					penalty += vehicleIndex * vehicleIndex;
				} else { 
					if (node.getRoutedVehicleCount() > congestionCountThreshold) {
						penalty += node.getRoutedVehicleCount() * 2;
					} else if (vehicleIndex > congestionIndexThreshold * 0.3) {
						penalty += vehicleIndex * 0.5;
					}
				}
			}
		}
		return penalty;
	}
	
	/**
	 * update updateOperationalParameters
	 * 
	 */
	public void updateOperationalParameters() {
		goingVehiclePenalty = ocsInfoManager.getGoingVehiclePenalty();
		stoppingVehiclePenalty = ocsInfoManager.getStoppingVehiclePenalty();
		workingVehiclePenalty = ocsInfoManager.getWorkingVehiclePenalty();
		cleaningVehiclePenalty = ocsInfoManager.getCleaningVehiclePenalty();
		manualVehiclePenalty = ocsInfoManager.getManualVehiclePenalty();
		errorVehiclePenalty = ocsInfoManager.getErrorVehiclePenalty();
		
		isDynamicRoutingUsed = ocsInfoManager.isDynamicRoutingUsed();
		congestionCountThreshold = ocsInfoManager.getCongestionCountThreshold();
		congestionIndexThreshold = ocsInfoManager.getCongestionIndexThreshold();
		congestionTimeThreshold = ocsInfoManager.getCongestionTimeThreshold();
		
		// 2015.05.27 by MYM : Vehicle Traffic 분산
		isTrafficUpdateUsed = ocsInfoManager.isTrafficUpdateUsed();
		
		// 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
		isFailureOHTDetourSearchUsed = ocsInfoManager.isFailureOHTDetourSearchUsed();
		isCollisionNodeCheckUsed = ocsInfoManager.isCollisionNodeCheckUsed();
		isPassDoorControlUsed = ocsInfoManager.isPassDoorControlUsage();
		
		// 2015.09.16 by MYM : 비근접은 Collision, 근접은 Block 위치의 장애 Vehicle을 보도록 변경
//		if (isCollisionNodeCheckUsed &&
//				isNearByDrive == false &&
//				abnormalVehiclesCollisionNodeSet != null) {
		if (isCollisionNodeCheckUsed &&
				abnormalVehiclesOnCollisionMap != null) {
			isCollisionNodeCheckNeeded = true;
		} else {
			isCollisionNodeCheckNeeded = false;
		}
	}
	
	/**
	 * log a message
	 * 
	 * @param message String
	 */
	public void trace(String message) {
		traceLog.debug(String.format("%s", message));
	}
	
	/**
	 * log a message with a specific modifier
	 * 
	 * @param name String
	 * @param message String
	 */
	public void trace(String name, String message) {
		traceLog.debug(String.format("%s> %s", name, message));
	}
	
	/**
	 * log an exception message
	 * 
	 * @param name String
	 * @param message String
	 */
	public void traceException(String message) {
		traceExceptionLog.error(String.format("%s", message));
	}
	
	/**
	 * log an exception with a specific modifier
	 * 
	 * @param name String
	 * @param message String
	 */
	public void traceException(String name, Throwable e) {
		// 2012.02.20 by PMM
		//traceExceptionLog.error(String.format("%s> ", e);
		traceExceptionLog.error(String.format("%s> ", name), e);
	}
	
	/**
	 * log an exception message with a specific modifier
	 * 
	 * @param name String
	 * @param message String
	 */
	public void traceException(String name, String message) {
		traceExceptionLog.error(String.format("%s> %s", name, message));
	}
	
	public void setNearByDrive (boolean isNearByDrive) {
		this.isNearByDrive = isNearByDrive;
	}
	
	/**
	 * 2014.02.08 by KYK
	 * @param vehicle
	 * @param prevNode
	 * @param node
	 * @param arrivedTime
	 * @return
	 */
	protected double checkZoneAllowed(VehicleData vehicle, Node prevNode, Node node, double arrivedTime) {
		if (node.getZoneIndex() != prevNode.getZoneIndex()) {
			String vehicleZone = vehicle.getZone();
			// 2014.07.29 by MYM : prevNode가 아닌 node의 Zone으로 비교해야 함.
//			String nodeZone = prevNode.getZone();
			String nodeZone = node.getZone();
			if (isDriveAllowed(vehicleZone, nodeZone) == false) {
				return MAXCOST_TIMEBASE;
			} else {
				arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
			}
		}
		return arrivedTime;
	}
	
	/**
	 * 2013.02.08 by KYK
	 * @param tagId
	 * @return
	 */
	public Node getNextNodeOfStation(String stationId) {
		Node nextNode = null;
		Station station = (Station) stationManager.getStation(stationId);
		if (station != null) {
			String nextNodeId = station.getNextNodeId();
			nextNode = nodeManager.getNode(nextNodeId);
		}
		return nextNode;
	}
	
//	/**
//	 * 2013.02.08 by KYK 
//	 * @return
//	 */
//	public boolean isAtWorkTagPassedbyParentNode(VehicleData vehicle) {
//		String stopStationId = vehicle.getStopStation();
//		if (stopStationId != null && stopStationId.length() > 0) {
//			Station stopStation = (Station) stationManager.getStation(stopStationId);
//			if (stopStation != null) {
//				if (stopStation.getOffset() > 0.01) {
//					return true;
//				}
//			}
//		}
//		// 불필요해보이나 일단 혹시나 코드
//		if (vehicle.getCurrNode().equals(vehicle.getStopNode())) {
//			String currStationId = vehicle.getCurrStation();		
//			if (currStationId != null && currStationId.length() > 0) {
//				Station currStation = stationManager.getStation(currStationId);
//				if (currStation != null) {
//					if (currStation.getOffset() > 0.01) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;			
//	}
	
//	/**
//	 * 2013.02.08 by KYK
//	 * @return
//	 */
//	public boolean checkAvailableToMoveForward(VehicleData vehicle) {
//		if (vehicle.getStopNode().equals(vehicle.getTargetNode())) {
//			String stopStationId = vehicle.getStopStation();
//			if (stopStationId != null && stopStationId.length() > 0) {
//				Station stopStation = (Station) stationManager.getStation(stopStationId);
//				if (stopStation != null) {
//					if (stopStation.getOffset() > 0.01) {
//						String targetStationId = vehicle.getTargetStation();
//						if (targetStationId != null && targetStationId.length() > 0) {
//							Station targetStation = (Station) stationManager.getStation(targetStationId);
//							if (targetStation != null) {
//								if (targetStation.getOffset() < stopStation.getOffset()) {
//									return false;
//								}
//							}
//						} else {
//							return false;
//						}
//					}						
//				}
//			}
//		}
//		return true;
//	}	
	
	/**
	 * 2013.02.15 by KYK
	 * @param vehicle
	 * @return
	 */
	public boolean isSearchedFromNextNodeToTarget(VehicleData vehicle) {
		String stopStationId = vehicle.getStopStation();
		if (stopStationId != null && stopStationId.length() > 0) {
			Station stopStation = stationManager.getStation(stopStationId);
			if (stopStation != null) {
				if (stopStation.getOffset() > 0) {
					if (vehicle.getStopNode().equals(vehicle.getTargetNode()) == false) {
						return true;
					} else {
						String targetStationId = vehicle.getTargetStation();
						if (targetStationId == null || targetStationId.length() == 0) {
							return true;
						} else {
							Station targetStation = stationManager.getStation(targetStationId);
							if (targetStation == null) {
								traceException("isSearchedFromNext() Vehicle:" + vehicle.getVehicleId() + ", TargetStation:" + targetStationId + " is null");
								return true;
							} else {
								// 2013.07.30 by KYK
								Node stopNextNode = stopStation.getNextNode();
								Node targetNextNode = targetStation.getNextNode();
								if (stopNextNode != targetNextNode) {
									return true;
								}
								if (stopStation.getOffset() > targetStation.getOffset()) {
									return true;
								}
							}
						}		
					}
				}
			} else {
				traceException("isSearchedFromNext() Vehicle:" + vehicle.getVehicleId() + ", StopStation:" + stopStationId + " is null");
			}
		}
		return false;
	}

	/**
	 * 2013.02.15 by KYK
	 * @param vehicle
	 * @return
	 */
	public boolean isSearchedFromNextNode(VehicleData vehicle) {
		String stopStationId = vehicle.getStopStation();
		if (stopStationId != null && stopStationId.length() > 0) {
			Station stopStation = stationManager.getStation(stopStationId);
			if (stopStation != null) {
				if (stopStation.getOffset() > 0) {
					return true;
				}
			} else {
				traceException("isSearchedFromNext() Vehicle:" + vehicle.getVehicleId() + ", StopStation:" + stopStationId + " is null");
			}
		}
		return false;
	}
	
	/**
	 * 2015.01.12 by MYM : YieldSearch, PathSearch 공통 사용을 위해 Search 로 이동
	 * 
	 * @param vehicle
	 * @param fromNode
	 * @param prevNode
	 * @param node
	 * @param arrivedTime
	 * @param sectionLevelCount
	 * @param routeDebug
	 * @param section
	 * @param checkAbnormalSection
	 * @return
	 */
	protected double checkAbnormalSearch(VehicleData vehicle,
			Node fromNode, Node prevNode, Node node, double arrivedTime,
			int sectionLevelCount, StringBuffer routeDebug, Section section, boolean checkAbnormalSection) {
		
		// Vehicle StopNode 위치로 Search된 경우 체크
		if (fromNode.equals(node)) {
			routeDebug.append("fromNode:").append(fromNode.getNodeId()).append(", node:").append(node.getNodeId()).append(" ");
			return MAXCOST_TIMEBASE;
		}

		// 2015.03.04 by zzang9un : prevPassDoor과 currPassDoor를 함께 보도록 수정(PassDoor Node에서 출발할 경우 PrevNode를 봐야함)
		// 2015.03.13 by zzang9un : 파라미터 적용
		if (isPassDoorControlUsed) {
			PassDoor currPassDoor = node.getPassDoor();
			PassDoor prevPassDoor = prevNode.getPassDoor();
			if (currPassDoor != null && currPassDoor.checkPassable() == false) {
				routeDebug.append("Can't pass PassDoor at node:" + currPassDoor.getNodeId() + "(" + currPassDoor.getPassDoorId() + ")");
				return MAXCOST_TIMEBASE;
			} else if (prevPassDoor != null && prevPassDoor.checkPassable() == false) {
				routeDebug.append("Can't pass PassDoor at node:" + prevPassDoor.getNodeId() + "(" + prevPassDoor.getPassDoorId() + ")");
				return MAXCOST_TIMEBASE;
			}
		}
		
		// 2014.10.10 by MYM : 장애 지역 우회 기능
		if (checkAbnormalSection) {
			double detourPenalty = section.getDetourPenalty(prevNode, prevNode.getPrevNode(vehicle.getVehicleId()));
			if (detourPenalty >= MAXCOST_TIMEBASE) {
				vehicle.setSearchFailReason(section.getAbnormalItemString());
				routeDebug.append("AbnormalSection:").append(section.getSectionId()).append(" ");
				return MAXCOST_TIMEBASE;
			} else {
				arrivedTime += detourPenalty;
			}
		}
		
		// 2012.10.19 by PMM
		// PathSearch에서 아래 부분 체크할 경우, 불합리 발생함.
//		// Vehicle이 Drive를 하는 구간으로 Search가 된 경우 체크
//		if (node.hasAlreadyDrived(vehicle)) {
//			routeDebug.append("AlreadyDrived at node:").append(node.getNodeId()).append(" ");
//			return MAXCOST_TIMEBASE;
//		}

		// Vehicle의 DetourNode가 있는 경우 체크
		if (vehicle.getDetourNode() != null && vehicle.getDetourNode() == node) {
			routeDebug.append("DetourNode:").append(vehicle.getDetourNode().getNodeId()).append(", node:").append(node.getNodeId()).append(" ");
			return MAXCOST_TIMEBASE;
		}
		
		// HID 이상 상태 구간인 경우 체크
		// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
//		if (node.isAbnormalState(vehicle, isFailureOHTDetourSearchUsed)) {
//			routeDebug.append("AbnormalState at node:").append(node.getNodeId()).append(", HID:");
//			if (node.getHid() != null) {
//				routeDebug.append(node.getHid().getState());
//			}
//			routeDebug.append(", ").append(node.getAbnormalVehicleState(vehicle)).append(" ");
//			return MAXCOST_TIMEBASE;
//		} else {
//			// Vehicle Manual, CommFail, AlarmCode >= 5000인 Collision Node 체크 : OperationManager에서 정보 수집
//			if (containsAbnormalCollision(node)) {
//				routeDebug.append("AbnormalState at CollisionNode of node:").append(node.getNodeId()).append(" ");
//				return MAXCOST_TIMEBASE;
//			}
//		}
		// step1. node Disabled
		if (node.isEnabled() == false) {
			routeDebug.append("AbnormalState at node:").append(node.getNodeId()).append(", Node Disabled");
			return MAXCOST_TIMEBASE;
		}

		// step2. link disabled
		// 2014.02.03 by MYM : Disabled Link 처리
		if (section.isLinkEnabled(prevNode, node) == false) {
			routeDebug.append("AbnormalState at link(from:").append(prevNode.getNodeId()).append(",to:").append(node.getNodeId()).append("), Link Disabled");
			return MAXCOST_TIMEBASE;
		}

		// step3. hid down 체크
		if (node.isAbnormalHid(vehicle)) {
			routeDebug.append("AbnormalState at node:").append(node.getNodeId()).append(", HID:");
			if (node.getHid() != null) {
				routeDebug.append(node.getHid().getState());
			}
			return MAXCOST_TIMEBASE;
		}

		// step4. 장애 Vehicle 체크
		if (isFailureOHTDetourSearchUsed || vehicle.getCarrierExist() == '0') {
			VehicleData abnormalVehicle = node.getAbnormalVehicle(vehicle);
			if (abnormalVehicle != null) {
				routeDebug.append("AbnormalState at node:").append(node.getNodeId());
				routeDebug.append("(").append(abnormalVehicle.getVehicleId());
				routeDebug.append(",").append(abnormalVehicle.getVehicleMode());
				routeDebug.append("/").append(abnormalVehicle.getState());
				routeDebug.append(",Error:").append(abnormalVehicle.getErrorCode());
				routeDebug.append(",Alarm:").append(abnormalVehicle.getAlarmCode());
				routeDebug.append(") ");
				return MAXCOST_TIMEBASE;
			}
		}

		// step5. Collision 이나 Block에 장애 Vehicle 체크
		// Vehicle Manual, CommFail, AlarmCode >= 5000인 Collision Node 체크 : OperationManager에서 정보 수집
		VehicleData abnormalVehicleOnCollision = getAbnormalVehicleOnCollision(node);
		if (abnormalVehicleOnCollision != null) {
			routeDebug.append("AbnormalState at Collision or Block of node:").append(node.getNodeId()).append("(").append(abnormalVehicleOnCollision.getVehicleId()).append(") ");
			return MAXCOST_TIMEBASE;
		}
		
		// 2014.09.04 by MYM : checkZoneAllowed 함수 사용
		// Zone Over시 Allow 체크 및 Penalty 적용 
//		if (prevNode.getZoneIndex() != node.getZoneIndex()) {
//			String vehicleZone = vehicle.getZone();
//			String nodeZone = node.getZone();
//			if (isDriveAllowed(vehicleZone, nodeZone) == false) {
//				routeDebug.append("PathSearch Not Allowed at node:").append(node.getNodeId()).append(", ").append(nodeZone).append(" ");
//				return MAXCOST_TIMEBASE;
//			}
//			arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
//		}
		double time = checkZoneAllowed(vehicle, prevNode, node, arrivedTime);
		if (time >= MAXCOST_TIMEBASE) {
			routeDebug.append("PathSearch Not Allowed at node:").append(node.getNodeId()).append(", ").append(node.getZone()).append(" ");
			return MAXCOST_TIMEBASE;
		} else {
			arrivedTime = time;
		}

		// Cleaning 중인 vehicle이 있으면 Penalty 부여
		double cleanPenalty = getCleanPenalty(node);
		arrivedTime += cleanPenalty;
			
		// Section 레벨에 따른 Vehicle Penalty 부여
		if (sectionLevelCount < MAX_SECTION_COUNT_FOR_VEHICLEPENALTY) {
			arrivedTime += getVehiclePenalty(node);
		}

		// arrivedTime이 Max 값에 도달한 경우(주행 불가)
		if (arrivedTime >= MAXCOST_TIMEBASE) {
			arrivedTime = MAXCOST_TIMEBASE;
			routeDebug.append(" arrivedTime is overed MAX_COST. ");
		}
		
		return arrivedTime;
	}
	
	/**
	 * 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
	 * @param node
	 * @return
	 */
	// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
//	protected boolean containsAbnormalCollision(Node node) {
//		if (isCollisionNodeCheckNeeded && node != null) {
//			if (abnormalVehiclesCollisionNodeSet.contains(node)) {
//				return true;
//			}
//		}
//		return false;
//	}
	protected VehicleData getAbnormalVehicleOnCollision(Node node) {
		if (isCollisionNodeCheckNeeded && node != null) {
			return abnormalVehiclesOnCollisionMap.get(node);
		}
		return null;
	}
	
	/**
	 * 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
	 * 
	 * @param node1
	 * @param node2
	 * @return
	 */
	protected double getMetropolitanTimeCost(Node node1, Node node2) {
		double metropolitanTimeCost = OcsConstant.MAXCOST_TIMEBASE;
		if (node1 != null && node2 != null) {
			metropolitanTimeCost = (Math.abs(node1.getLeft() - node2.getLeft()) + Math.abs(node1.getTop() - node2.getTop())) / 3300; 
		}
		return metropolitanTimeCost;
	}
	
	/**
	 * 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
	 * 
	 * @param vehicleId
	 * @param node
	 * @param queueNodeList
	 */
	protected void addQueueNodeWithHeuristicCost(String vehicleId, Node node, LinkedList<Node> queueNodeList) {
		if (node != null) {
			Node tempNode = null;
			for (int i = 0; i < queueNodeList.size(); i++) {
				tempNode = queueNodeList.get(i);
				if (tempNode != null) {
					if (node.getHeuristicCost(vehicleId) < tempNode.getHeuristicCost(vehicleId)) {
						queueNodeList.add(i, node);
						return;
					} else if (node.getHeuristicCost(vehicleId) == tempNode.getHeuristicCost(vehicleId)) {
						if (node.getArrivedTime(vehicleId) < tempNode.getArrivedTime(vehicleId)) {
							queueNodeList.add(i, node);
							return;
						}
					}
				}
			}
			queueNodeList.add(node);
		}
	}
	
	/**
	 * 2013.10.03 by KYK : check abnormal state
	 * 1. Node Disable
	 * 2. HID Down
	 * 3. Abnormal Vehicle in Node
	 * 4. Zone not Allowed
	 * 
	 * @param fromNode
	 * @param currNode
	 * @param vehicle
	 * @return
	 */
	protected boolean checkAbnormalState(Node fromNode, Node currNode, VehicleData vehicle) {
		if (fromNode == null || currNode == null || vehicle == null) {
			return true;
		}
		if (fromNode.isEnabled() == false) {
			return true;
		}
		
		// 2015.02.28 by zzang9un : PassDoor 장애 시 Search fail되도록 수정
		// 2015.03.11 by zzang9un : 파라미터 적용
		if (isPassDoorControlUsed) {
			PassDoor passDoor = null;
			passDoor = fromNode.getPassDoor();
			if (passDoor != null && passDoor.checkPassable() == false) {
				StringBuffer routeDebug = new StringBuffer();
				routeDebug.append("PathSearch Not Allowed at node:Cannot pass By PassDoor at node:" + fromNode.getNodeId() + "(" + passDoor.getPassDoorId() + ")");
				trace(routeDebug.toString());
				return true;
			}
		}
		
		if (fromNode.isAbnormalState(vehicle, ocsInfoManager.isFailureOHTDetourSearchUsed())) {
			StringBuffer routeDebug = new StringBuffer();
			routeDebug.append("AbnormalState at node:").append(fromNode.getNodeId()).append(", HID:");
			if (fromNode.getHid() != null) {
				routeDebug.append(fromNode.getHid().getState());
			}
			routeDebug.append(", ").append(fromNode.getAbnormalVehicleState(vehicle)).append(" ");
			trace(routeDebug.toString());
			return true;
		}
		if (currNode.getZoneIndex() != fromNode.getZoneIndex()) {
			StringBuffer routeDebug = new StringBuffer();
			String vehicleZone = vehicle.getZone();
			String nodeZone = fromNode.getZone();
			if (isDriveAllowed(vehicleZone, nodeZone) == false
					|| getZoneOverPenalty(vehicleZone, nodeZone) == MAXCOST_TIMEBASE) {
				routeDebug.append("PathSearch Not Allowed at node:").append(fromNode.getNodeId()).append(", ").append(nodeZone).append(" ");
				trace(routeDebug.toString());
				return true;
			}
		}
				
		return false;
	}

}
