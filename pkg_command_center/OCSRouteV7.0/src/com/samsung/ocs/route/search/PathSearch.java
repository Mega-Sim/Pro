package com.samsung.ocs.route.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.constant.OcsConstant.SEARCH_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.TRAFFIC_UPDATE_RULE;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.NodeArrivedTimeInfo;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.Traffic;
import com.samsung.ocs.manager.impl.model.VehicleData;

/**
 * PathSearch Class, OCS 3.0 for Unified FAB
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

public class PathSearch extends Search {
	// 2011.11.15 by PMM
	private boolean isNearByDrive = true;
	private static final String NEARBY = "NEARBY";
	private static final String NORMAL = "NORMAL";
	private static final String INSERVICE = "InService";
	
	// 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
//	 2011.12.02 by PMM
//	private static final int MAX_SECTION_COUNT_FOR_VEHICLEPENALTY = 10;
//	
//	private boolean isFailureOHTDetourSearchUsed = true;
//	private boolean isCollisionNodeCheckUsed = true;
//	private boolean isCollisionNodeCheckNeeded = true;
//	
//	// CostSearch 때문에 private
//	private HashSet<Node> abnormalVehiclesCollisionNodeSet;
	
	/**
	 * Constructor of PathSearch class.
	 */
	// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//	public PathSearch(boolean isNearByDrive) {
//		super();
//		this.isNearByDrive = isNearByDrive;
//		abnormalVehiclesCollisionNodeSet = nodeManager.getAbnormalVehiclesCollisionNodeSet();
//	}
	public PathSearch(boolean isNearByDrive, ConcurrentHashMap<Node, VehicleData> abnormalVehiclesOnCollisionMap) {
		super();
		this.isNearByDrive = isNearByDrive;
		this.abnormalVehiclesOnCollisionMap = abnormalVehiclesOnCollisionMap;
	}
	
	// 2011.10.21 by PMM
	// public boolean initializeVehiclePath(String vehicleId) {
	/**
	 * initialize path of a vehicle
	 * 
	 * @param vehicle VehicleData
	 * @return true if successfully initialize path of a vehicle
	 */
	public boolean initializeVehiclePath(VehicleData vehicle, String type) {
		if (vehicle == null) {
			return false;
		}
		
		Node currNode = nodeManager.getNode(vehicle.getCurrNode());
		Node stopNode = nodeManager.getNode(vehicle.getStopNode());
		
		// 2011.10.27 by PMM 확인용 로그 추가
		if (currNode == null) {
			StringBuffer log = new StringBuffer();
			log.append("initializeVehiclePath() - ");
//			log.append("VehicleId:").append(vehicle.getVehicleId());
			log.append(" CurrNode:").append(vehicle.getCurrNode());
			log.append(" StopNode:").append(vehicle.getStopNode());
//			trace(log.toString());
			trace(vehicle.getVehicleId(), log.toString());
			return false;
		}
		
		StringBuffer log = new StringBuffer("initializeVehiclePath - ");
		log.append(type).append("/").append(currNode.getNodeId()).append("/").append(stopNode.getNodeId()).append("/");
		if (isNearByDrive) {
			log.append(NEARBY);
		} else {
			log.append(NORMAL);
		}
		
		char vehicleMode = ' ';
		char vehicleState = ' ';
		if (INSERVICE.equalsIgnoreCase(type) == false) {
			vehicleMode = vehicle.getVehicleMode();
			vehicleState = vehicle.getState();
		}
		vehicle.updateDriveNode(vehicleMode, vehicleState, currNode, isNearByDrive);
		vehicle.resetRoutedNodeList();

		if (stopNode != null && currNode.equals(stopNode) == false) {
			if (searchInitializeVehicleDrivePath(vehicle, currNode, stopNode)) {
//				if (vehicle.initializePathDrive(currNode, isNearByDrive)) {
				if (vehicle.initializePathDrive(currNode)) {
					log.append("/").append("init1/").append(vehicle.getDriveNodeInfo());					
					trace(vehicle.getVehicleId(), log.toString());
					return true;
				}
			} else {
				log.append("/").append("SearchFail");					
			}
		} else {
			vehicle.resetDriveNodeList(isNearByDrive, currNode);
			log.append("/").append("init2/").append(vehicle.getDriveNodeInfo());
			trace(vehicle.getVehicleId(), log.toString());
			return true;
		}
		
		log.append("/").append("false").append("/").append(vehicle.getReason()).append("/").append(vehicle.getDriveNodeInfo());
		trace(vehicle.getVehicleId(), log.toString());
		return false;
	}
	
	// 2011.10.21 by PMM
	// public boolean searchVehiclePath(String vehicleId, String targetNode) {
	/**
	 * search path of a vehicle to a targetNode
	 * 
	 * @param vehicle VehicleData
	 * @param targetNode String
	 * @return true if successfully search path of a vehicle to a targetNode
	 */
	public boolean searchVehiclePath(VehicleData vehicle, String targetNode, int priority) {
//		VehicleData vehicle = (VehicleData)vehicleManager.getVehicle(vehicleId);
		if (vehicle == null) {
			return false;
		}

		// PathSearch 요청 초기화
		vehicle.setPathSearchRequest(false);
		
		// 2011.10.26 by PMM
		// 로그 추가
		StringBuffer request = new StringBuffer();
//		request.append(" searchVehiclePath() requested. Vehicle:").append(vehicle.getVehicleId()).append(", DriveStopNode:").append(vehicle.getDriveStopNode()).append(" -> TargetNode:").append(targetNode);
//		trace(request.toString());
		request.append("searchVehiclePath() requested. DriveStopNode:").append(vehicle.getDriveStopNode()).append(" -> TargetNode:").append(targetNode);
		// 2013.02.15 by KYK
		String stationId = vehicle.getTargetStation();
		if (stationId != null && stationId.length() > 0) {
			request.append(", TargetStation:" + stationId);
		}
		trace(vehicle.getVehicleId(), request.toString());
		
		long processTime = System.currentTimeMillis();
		boolean retVal = false;
		try {
			// 2013.10.17 by KYK
			retVal = searchVehiclePathToTarget(vehicle, vehicle.getDriveStopNode(), nodeManager.getNode(targetNode), priority);
		} catch (Exception e) {
			traceException("searchVehiclePath() - " + vehicle.getVehicleId(), e);
		} finally {
			if ((System.currentTimeMillis()-processTime) >= 100) {
				StringBuffer log = new StringBuffer();
//				log.append(" searchVehiclePath() TimeDelay=").append(System.currentTimeMillis()-processTime).append(" ").append(vehicle.getVehicleId()).append(" ").append(targetNode);
//				trace(log.toString());
				log.append("searchVehiclePath() TimeDelay=").append(System.currentTimeMillis()-processTime);
				trace(vehicle.getVehicleId(), log.toString());
			}
		}
		
		return retVal;
	}

	/**
	 * 2013.02.01 by KYK
	 * @param vehicle
	 * @param targetNode
	 * @return
	 */
	private boolean searchVehiclePathToTarget(VehicleData vehicle,  Node fromNode, Node toNode, int priority) {
		long pathSearchStartedTime = System.nanoTime();
		
		// fromNode, toNode 확인
		if (fromNode == null || toNode == null) {
			return false;
		}
		// 2013.02.15 by KYK
		if (isSearchedFromNextNodeToTarget(vehicle)) {
			Node currNode = fromNode;
			fromNode = getNextNodeOfStation(vehicle.getStopStation());
			// 2013.10.03 by KYK : check node abnormal
			if (checkAbnormalState(fromNode,currNode,vehicle)) {
				return false;
			}
		}
		if (fromNode.equals(toNode)) {
			LinkedList<Node> routedNodeList = new LinkedList<Node>();
			routedNodeList.add(0, fromNode);
			return vehicle.setRoutedNodeList(routedNodeList);
		}
		
		// vehicle route 초기화
		String vehicleId = vehicle.getVehicleId();
		Node searchNode = null;
		boolean arrived = false;
		double arrivedTime = 0;
		double moveTime = 0;
		long distance = 0;
		double targetArrivedTime = MAXCOST_TIMEBASE;
		fromNode.setArrivedTime(vehicleId, arrivedTime, getMetropolitanTimeCost(fromNode, toNode));
		
		int loopCount = 0;
		int checkedNodeCount = 0;
		int sectionIndex = 0;
		
		// toNode까지 경로 탐색
		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addQueueNode(vehicleId, fromNode, queueNodeList);
		addQueueNodeWithHeuristicCost(vehicleId, fromNode, queueNodeList);
		
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
		LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
		resetRouteNodeList.add(fromNode);
		
		// 2011.10.26 by PMM
		// 로그 추가
		StringBuffer route = new StringBuffer();
		route.append("[" + toNode.getNodeId() + "] ");
		
		// 2012.07.03 by MYM : PathSearch시 Abnormal 케이스 함수 일원화로 미사용 변수 주석 처리
//		String vehicleZone = vehicle.getZone();
//		String prevNodeZone = "";
//		String nodeZone = "";
		boolean isException = false;
		try {
			while (true) {
				loopCount++;
				
				searchNode = getQueuedNode(queueNodeList);
				if (searchNode == null)
					break;
				
				for (int i = 0; i < searchNode.getSectionCount(); i++) {
					loopCount++;
					
//					arrivedTime = searchNode.getArrivedTime(vehicleId);
					NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
					if (arrivedTimeInfo != null) {
						arrivedTime = arrivedTimeInfo.getArrivedTime();
						moveTime = arrivedTimeInfo.getMoveTime();
						distance = arrivedTimeInfo.getDistance();
						sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
					} else {
						arrivedTime = MAXCOST_TIMEBASE;
						moveTime = MAXCOST_TIMEBASE;
						distance = MAXCOST_DISTANCEBASE;
					}
					
					Section section = searchNode.getSection(i);
					Node prevNode = searchNode;
					int pos = section.getNodeIndex(searchNode);
					
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						loopCount++;
						checkedNodeCount++;
						Node node = section.getNode(j);
//						arrivedTime += node.getMoveInTime(prevNode);
						double moveInTime = node.getMoveInTime(prevNode);
//						arrivedTime += moveInTime + node.getTrafficPenalty();
						// 2015.05.27 by MYM : Vehicle Traffic 분산
						if (isTrafficUpdateUsed) {
							double trafficCost = 0, trafficRatio = 1;
							Traffic traffic = section.getTraffic();
							if (traffic != null) {
								if (traffic.getType() == TRAFFIC_UPDATE_RULE.PULL) {
									trafficRatio = traffic.getPullTrafficRatio();
								} else if (traffic.getType() == TRAFFIC_UPDATE_RULE.PUSH) {
									trafficCost = traffic.getPushTrafficCost(section, priority);
								}
							}
							arrivedTime += (moveInTime * trafficRatio) + node.getTrafficPenalty() + trafficCost;
						} else {
							arrivedTime += moveInTime + node.getTrafficPenalty();
						}
						moveTime += moveInTime;
						distance += node.getMoveInDistance(prevNode);
						
						if (isDynamicRoutingUsed) {
							arrivedTime += getCongestionPenalty(node, arrivedTime, prevNode.getIndex(vehicleId) + 1);
							
							if (vehicle.containsRedirectedNodeSet(node)) {
								arrivedTime += 300;
							}
						}
						
						// 2012.07.03 by MYM : PathSearch시 Abnormal 케이스 함수 일원화
//						double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, i, route);
						double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, sectionIndex, route, section, j == 1);
						if (time == MAXCOST_TIMEBASE) {
							break;
						} else {
							arrivedTime = time;
						}
						
						// 2014.02.03 by MYM : Disabled Link 처리
//						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, toNode))) {
						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, toNode), moveTime, distance, section, sectionIndex)) {
							prevNode = node;
//							arrivedTime = node.getArrivedTime(vehicleId);
							arrivedTimeInfo = node.getArrivedTimeInfo(vehicleId);
							if (arrivedTimeInfo != null) {
								arrivedTime = arrivedTimeInfo.getArrivedTime();
								moveTime = arrivedTimeInfo.getMoveTime();
								distance = arrivedTimeInfo.getDistance();
							}
							
							if (toNode.equals(node)) {
								if (arrivedTime < targetArrivedTime) {
									targetArrivedTime = arrivedTime;
								}
								arrived = true;
								break;
							}
//							if (arrivedTime >= targetArrivedTime) {
							if (arrivedTime + node.getHeuristicCost(vehicleId) >= targetArrivedTime) {
								break;
							}
							if (j == section.getNodeCount()-1) {
//								addQueueNode(vehicleId, node, queueNodeList);
								addQueueNodeWithHeuristicCost(vehicleId, node, queueNodeList);
								// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
								resetRouteNodeList.add(node);
							}
						} else {
							break;
						}
					}
				}
			}
			
			if (arrived == true) {
				StringBuffer log = new StringBuffer();
				NodeArrivedTimeInfo arrivedTimeInfo = toNode.getArrivedTimeInfo(vehicleId);
				log.append("Path Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId());
				log.append(", Cost:").append(targetArrivedTime).append(", Time:").append(arrivedTimeInfo.getMoveTime()).append(", Distance:").append(arrivedTimeInfo.getDistance());
				log.append(", loopCnt:").append(loopCount);
				log.append(", CheckedNode:").append(checkedNodeCount);
				log.append(", Time:").append((double)((System.nanoTime() - pathSearchStartedTime)/1000)/1000).append(" ms");
				trace(vehicleId, log.toString());

				LinkedList<Node> routedNodeList = new LinkedList<Node>();
				Node node = toNode;
				while (fromNode.equals(node) == false) {
					routedNodeList.add(0, node);
					node = node.getPrevNode(vehicleId);
				}
				routedNodeList.add(0, node);
				return vehicle.setRoutedNodeList(routedNodeList);
			} else {
				// 2011.10.26 by PMM
				// 로그 추가
//				trace(route.toString());
				trace(vehicleId, route.toString());
			}
		} catch (Exception e) {
			traceException("searchVehiclePathToTargetNode() - " + vehicleId, e);
			isException = true;
		} finally {
			vehicle.resetDetourNode();
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
			if (isException) {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
			} else {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
			}
		}
		return false;
	}
	
	private boolean searchInitializeVehicleDrivePath(VehicleData vehicle, Node currNode, Node stopNode) {
		long pathSearchStartedTime = System.nanoTime();
		
		// fromNode, toNode 확인
		if (currNode == null || stopNode == null) {
			return false;
		} else if (currNode.equals(stopNode)) {
			LinkedList<Node> routedNodeList = new LinkedList<Node>();
			routedNodeList.add(0, currNode);
			return vehicle.setInitializeRoutedNodeList(routedNodeList);
		}
		
		// vehicle route 초기화
		String vehicleId = vehicle.getVehicleId();
		Node searchNode = null;
		boolean arrived = false;
		double arrivedTime = 0;
		double moveTime = 0;
		long distance = 0;
		double targetArrivedTime = MAXCOST_TIMEBASE;
		currNode.setArrivedTime(vehicleId, arrivedTime, getMetropolitanTimeCost(currNode, stopNode));
		int loopCount = 0;
		int checkedNodeCount = 0;
		int sectionIndex = 0;
		
		// toNode까지 경로 탐색
		LinkedList<Node> queueNodeList = new LinkedList<Node>();
		addQueueNodeWithHeuristicCost(vehicleId, currNode, queueNodeList);
		
		LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
		resetRouteNodeList.add(currNode);
		
		// 로그 추가
		StringBuffer route = new StringBuffer();
		route.append("[" + stopNode.getNodeId() + "] ");
		boolean isException = false;
		try {
			while (true) {
				loopCount++;
				
				searchNode = getQueuedNode(queueNodeList);
				if (searchNode == null)
					break;
				
				for (int i = 0; i < searchNode.getSectionCount(); i++) {
					loopCount++;
					
//					arrivedTime = searchNode.getArrivedTime(vehicleId);
					NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
					if (arrivedTimeInfo != null) {
						arrivedTime = arrivedTimeInfo.getArrivedTime();
						moveTime = arrivedTimeInfo.getMoveTime();
						distance = arrivedTimeInfo.getDistance();
						sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
					} else {
						arrivedTime = MAXCOST_TIMEBASE;
						moveTime = MAXCOST_TIMEBASE;
						distance = MAXCOST_DISTANCEBASE;
					}
					
					Section section = searchNode.getSection(i);
					Node prevNode = searchNode;
					int pos = section.getNodeIndex(searchNode);
					
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						loopCount++;
						checkedNodeCount++;
						Node node = section.getNode(j);
//						arrivedTime += node.getMoveInTime(prevNode);
//						index = prevNode.getIndex(vehicleId) + 1;
//						arrivedTime += node.getTrafficPenalty() + getCongestionPenalty(node, arrivedTime, index);
						double moveInTime = node.getMoveInTime(prevNode);
						arrivedTime += moveInTime + node.getTrafficPenalty();
						moveTime += moveInTime;
						distance += node.getMoveInDistance(prevNode);
						
						if (isDynamicRoutingUsed) {
							arrivedTime += getCongestionPenalty(node, arrivedTime, prevNode.getIndex(vehicleId) + 1);
							
							if (vehicle.containsRedirectedNodeSet(node)) {
								arrivedTime += 300;
							}
						}
						
						if (currNode.equals(node)) {
							arrivedTime = MAXCOST_TIMEBASE;
							break;
						}
						
						// 2014.02.03 by MYM : Disabled Link 처리
//						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, stopNode))) {
						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, stopNode), moveTime, distance, section, sectionIndex)) {
							prevNode = node;
//							arrivedTime = node.getArrivedTime(vehicleId);
							arrivedTimeInfo = node.getArrivedTimeInfo(vehicleId);
							if (arrivedTimeInfo != null) {
								arrivedTime = arrivedTimeInfo.getArrivedTime();
								moveTime = arrivedTimeInfo.getMoveTime();
								distance = arrivedTimeInfo.getDistance();
							}
							
							if (stopNode.equals(node)) {
								if (arrivedTime < targetArrivedTime) {
									targetArrivedTime = arrivedTime;
								}
								arrived = true;
								break;
							}
							if (arrivedTime + node.getHeuristicCost(vehicleId) >= targetArrivedTime) {
								break;
							}
							if (j == section.getNodeCount()-1) {
								addQueueNodeWithHeuristicCost(vehicleId, node, queueNodeList);
								resetRouteNodeList.add(node);
							}
						} else {
							break;
						}
					}
				}
			}
			
			if (arrived == true) {
				StringBuffer log = new StringBuffer();
				NodeArrivedTimeInfo arrivedTimeInfo = stopNode.getArrivedTimeInfo(vehicleId);
				log.append("Initialize Vehicle DrivePath Searched From ").append(currNode.getNodeId()).append(" To ").append(stopNode.getNodeId());
				log.append(", Cost:").append(targetArrivedTime).append(", Time:").append(arrivedTimeInfo.getMoveTime()).append(", Distance:").append(arrivedTimeInfo.getDistance());
				log.append(", loopCnt:").append(loopCount);
				log.append(", CheckedNode:").append(checkedNodeCount);
				log.append(", Time:").append((double)((System.nanoTime() - pathSearchStartedTime)/1000)/1000).append(" ms");
				trace(vehicleId, log.toString());

				LinkedList<Node> routedNodeList = new LinkedList<Node>();
				Node node = stopNode;
				while (currNode.equals(node) == false) {
					routedNodeList.add(0, node);
					node = node.getPrevNode(vehicleId);
				}
				routedNodeList.add(0, node);
				return vehicle.setInitializeRoutedNodeList(routedNodeList);
			} else {
				// 2011.10.26 by PMM
				// 로그 추가
//				trace(route.toString());
				trace(vehicleId, route.toString());
			}
		} catch (Exception e) {
			traceException("searchInitializeVehicleDrivePath() - " + vehicleId, e);
			isException = true;
		} finally {
			vehicle.resetDetourNode();
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
			if (isException) {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
			} else {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
			}
		}
		return false;
	}
	
	/**
	 * search UserDefinedPath of a vehicle to a targetNode
	 * 
	 * @param vehicle VehicleData
	 * @param targetNode String
	 * @return true if successfully search path of a vehicle to a targetNode
	 */
	public boolean searchVehiclePathOnUserDefinedRoutes(VehicleData vehicle, Vector<String> nodeList, String targetNode, int vehicleLimit) {
		if (vehicle == null) {
			return false;
		}
		if (nodeList.size() == 0) {
			return false;
		}
		nodeList.add(targetNode);
		
		// PathSearch 요청 초기화
		vehicle.setPathSearchRequest(false);
		
		// 2014.05.13 by MYM : 로그 추가
		StringBuffer request = new StringBuffer();
		request.append("searchVehiclePathOnUserDefinedRoutes() requested. DriveStopNode:").append(vehicle.getDriveStopNode()).append(" -> TargetNode:").append(targetNode);
		trace(vehicle.getVehicleId(), request.toString());
		
		long processTime = System.currentTimeMillis();
		boolean retVal = false;
		try {
			retVal = searchVehiclePathOnUserDefinedRoutes(vehicle, nodeList, vehicleLimit);
		} catch (Exception e) {
			traceException("searchVehiclePathOnUserDefinedRoutes() - " + vehicle.getVehicleId(), e);
		} finally {
			if ((System.currentTimeMillis() - processTime) >= 100) {
				StringBuffer log = new StringBuffer();
				log.append("searchVehiclePathOnUserDefinedRoutes() TimeDelay=").append(System.currentTimeMillis()-processTime);
				trace(vehicle.getVehicleId(), log.toString());
			}
		}
		
		return retVal;
	}
	
	/**
	 * search UserDefinedPath of a vehicle to a targetNode
	 * 
	 * @param vehicle VehicleData
	 * @param nodeList Vector<String>
	 * @param vehicleLimit int
	 * @return true if successfully search path of a vehicle to a targetNode
	 */
	private boolean searchVehiclePathOnUserDefinedRoutes(VehicleData vehicle, Vector<String> nodeList, int vehicleLimit) {
		long pathSearchStartedTime = System.nanoTime();
		Node fromNode = null;
		fromNode = vehicle.getDriveStopNode();
		Node toNode = nodeManager.getNode(nodeList.lastElement());
		
		// fromNode, toNode 확인
		if (fromNode == null || toNode == null) {
			return false;
		}
		// 2013.02.15 by KYK
		if (isSearchedFromNextNodeToTarget(vehicle)) {
			Node currNode = fromNode;
			fromNode = getNextNodeOfStation(vehicle.getStopStation());
			// 2013.10.03 by KYK : check node abnormal
			if (checkAbnormalState(fromNode,currNode,vehicle)) {
				return false;
			}
		}
//		// 2013.02.08 by KYK
//		if (isAtWorkTagPassedbyParentNode(vehicle)) {
//			if (checkAvailableToMoveForward(vehicle) == false) {
//				fromNode = getNextNodeOfStation(vehicle.getStopStation());
//				if (fromNode == null) {
//					return false;
//				}
//			}
//		}		
		if (fromNode.equals(toNode)) {
			// calculate ArrivedTime ?
			LinkedList<Node> routedNodeList = new LinkedList<Node>();
			routedNodeList.add(0, fromNode);
			return vehicle.setRoutedNodeList(routedNodeList);
		}
		
		// vehicle route 초기화
		String vehicleId = vehicle.getVehicleId();
		LinkedList<Node> routedNodeList = new LinkedList<Node>();
		
		StringBuffer route = new StringBuffer();
		route.append("[" + toNode + "] ");
		
		// 2012.07.03 by MYM : PathSearch시 Abnormal 케이스 함수 일원화로 미사용 변수 주석 처리
//		String vehicleZone = vehicle.getZone();
//		String prevNodeZone = "";
//		String nodeZone = "";

		for (int i = 0; i < nodeList.size(); i++) {
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//			initRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);

			toNode = nodeManager.getNode(nodeList.get(i));
			if (toNode == null) {
				return false;
			}

			if (i != 0) {
				fromNode = routedNodeList.getLast();
			}
			
			Node searchNode = null;
			boolean arrived = false;
			double arrivedTime = 0;
			double moveTime = 0;
			long distance = 0;
			double targetArrivedTime = MAXCOST_TIMEBASE;
			int sectionIndex = 0;
			fromNode.setArrivedTime(vehicleId, arrivedTime, getMetropolitanTimeCost(fromNode, toNode));
			
			// toNode까지 경로 탐색
			LinkedList<Node> queueNodeList = new LinkedList<Node>();
			addQueueNodeWithHeuristicCost(vehicleId, fromNode, queueNodeList);
			
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
			LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
			resetRouteNodeList.add(fromNode);
			
			boolean isException = false;
			try {
				while (true) {
					searchNode = getQueuedNode(queueNodeList);
					if (searchNode == null)
						break;
					
					for (int j = 0; j < searchNode.getSectionCount(); j++) {
//						arrivedTime = searchNode.getArrivedTime(vehicleId);
						NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
						if (arrivedTimeInfo != null) {
							arrivedTime = arrivedTimeInfo.getArrivedTime();
							moveTime = arrivedTimeInfo.getMoveTime();
							distance = arrivedTimeInfo.getDistance();
							sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
						} else {
							arrivedTime = MAXCOST_TIMEBASE;
							moveTime = MAXCOST_TIMEBASE;
							distance = MAXCOST_DISTANCEBASE;
						}
						
						Section section = searchNode.getSection(j);
						Node prevNode = searchNode;
						int pos = section.getNodeIndex(searchNode);
						for (int k = pos + 1; k < section.getNodeCount(); k++) {
							Node node = section.getNode(k);
//							arrivedTime += node.getMoveInTime(prevNode) + node.getTrafficPenalty();
							double moveInTime = node.getMoveInTime(prevNode);
							arrivedTime += moveInTime + node.getTrafficPenalty();
							moveTime += moveInTime;
							distance += node.getMoveInDistance(prevNode);
							
							// 2012.07.03 by MYM : PathSearch시 Abnormal 케이스 함수 일원화
							double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, sectionIndex, route, section, k == 1);
							if (time == MAXCOST_TIMEBASE) {
								break;
							} else {
								arrivedTime = time;
							}
							
							// 2014.02.03 by MYM : Disabled Link 처리
//							if (node.setArrivedTime(vehicleId, prevNode, arrivedTime)) {
							if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, toNode), moveTime, distance, section, sectionIndex)) {
								prevNode = node;
//								arrivedTime = node.getArrivedTime(vehicleId);
								arrivedTimeInfo = node.getArrivedTimeInfo(vehicleId);
								if (arrivedTimeInfo != null) {
									arrivedTime = arrivedTimeInfo.getArrivedTime();
									moveTime = arrivedTimeInfo.getMoveTime();
									distance = arrivedTimeInfo.getDistance();
								}
								
								// 2015.01.13 by MYM : A* 알고리즘으로 변경(사용자 정의 경로)
//								if (toNode.equals(node)) {
//									arrived = true;
//									StringBuffer log = new StringBuffer();
//									log.append(vehicleId).append("'s Path Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId()).append(" : ").append(arrivedTime);
//									trace(log.toString());
//									log.append("Path Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId()).append(" : ").append(arrivedTime);
//									trace(vehicleId, log.toString());
//									break;
//								}
//								
//								if (k == section.getNodeCount()-1) {
//									addQueueNode(vehicleId, node, queueNodeList);
//									// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//									resetRouteNodeList.add(node);
//								}
								if (toNode.equals(node)) {
									if (arrivedTime < targetArrivedTime) {
										targetArrivedTime = arrivedTime;
									}
									arrived = true;
									break;
								}
								if (arrivedTime + node.getHeuristicCost(vehicleId) >= targetArrivedTime) {
									break;
								}
								if (k == section.getNodeCount()-1) {
									addQueueNodeWithHeuristicCost(vehicleId, node, queueNodeList);
									resetRouteNodeList.add(node);
								}
							} else {
								break;
							}
						}
						
						// 2015.01.13 by MYM : A* 알고리즘으로 변경(사용자 정의 경로)
//						if (arrived == true) {
//							break;
//						}
					}
				}
				
				if (arrived == true) {
					Node node = toNode;
					int pos = routedNodeList.size();
					while (fromNode.equals(node) == false) {
						routedNodeList.add(pos, node);
						node = node.getPrevNode(vehicleId);
					}
					// 2013.11.04 by KYK
//					if (node.equals(vehicle.getDriveStopNode())) {
					if (pos == 0) {
						routedNodeList.add(pos, node);
					}
					
					// 2015.01.13 by MYM : A* 알고리즘으로 변경으로 로그 위치 변경(사용자 정의 경로)
					StringBuffer log = new StringBuffer();
					NodeArrivedTimeInfo arrivedTimeInfo = toNode.getArrivedTimeInfo(vehicleId);
					log.append("UserDefined Path Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId());
					log.append(", Cost:").append(arrivedTimeInfo.getArrivedTime()).append(", Time:").append(arrivedTimeInfo.getMoveTime()).append(", Distance:").append(arrivedTimeInfo.getDistance());
					log.append(", Time:").append((double)((System.nanoTime() - pathSearchStartedTime)/1000)/1000).append(" ms");
					trace(vehicleId, log.toString());
				} else {
					trace(vehicleId, route.toString());
					return false;
				}
			} catch (Exception e) {
				traceException("searchVehiclePathOnUserDefinedRoutes() - " + vehicleId, e);
				isException = true;
			} finally {
				// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
				if (isException) {
					removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
				} else {
					removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
				}
			}
		}
		
		ArrayList<VehicleData> driveVehicleList = new ArrayList<VehicleData>(); 
		for (int i = 0; i < routedNodeList.size(); i++) {
			Node node = routedNodeList.get(i);
			VehicleData driveVehicle = node.getDriveVehicle();
			if (vehicle.equals(driveVehicle)) {
				continue;
			}
			
			if (driveVehicle != null && driveVehicleList.contains(driveVehicle) == false) {
				driveVehicleList.add(driveVehicle);
			}
		}
		
		if (driveVehicleList.size() < vehicleLimit) {
			return vehicle.setRoutedNodeList(routedNodeList);
		}
		
		return false;
	}
	
	// 2015.03.04 by MYM : PathSercha, YieldSearch 공통 부분으로 Search로 이동
//	/**
//	 * 2012.07.03 by MYM : PathSearch시 Abnormal 케이스 함수 일원화
//	 * 
//	 * @param vehicle
//	 * @param fromNode
//	 * @param prevNode
//	 * @param node
//	 * @param arrivedTime
//	 * @param sectionLevelCount
//	 * @param routeDebug
//	 * @return
//	 */
//	private double checkAbnormalSearch(VehicleData vehicle,
//			Node fromNode, Node prevNode, Node node, double arrivedTime,
//			int sectionLevelCount, StringBuffer routeDebug) {
//		
//		// Vehicle StopNode 위치로 Search된 경우 체크
//		if (fromNode.equals(node)) {
//			routeDebug.append("fromNode:").append(fromNode.getNodeId()).append(", node:").append(node.getNodeId()).append(" ");
//			return MAXCOST_TIMEBASE;
//		}
//
//		// 2012.10.19 by PMM
//		// PathSearch에서 아래 부분 체크할 경우, 불합리 발생함.
////		// Vehicle이 Drive를 하는 구간으로 Search가 된 경우 체크
////		if (node.hasAlreadyDrived(vehicle)) {
////			routeDebug.append("AlreadyDrived at node:").append(node.getNodeId()).append(" ");
////			return MAXCOST_TIMEBASE;
////		}
//
//		// Vehicle의 DetourNode가 있는 경우 체크
//		if (vehicle.getDetourNode() != null && vehicle.getDetourNode() == node) {
//			routeDebug.append("DetourNode:").append(vehicle.getDetourNode().getNodeId()).append(", node:").append(node.getNodeId()).append(" ");
//			return MAXCOST_TIMEBASE;
//		}
//		
//		// HID 이상 상태 구간인 경우 체크
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
//
//		// 2014.09.04 by MYM : checkZoneAllowed 함수 사용
//		// Zone Over시 Allow 체크 및 Penalty 적용 
////		if (prevNode.getZoneIndex() != node.getZoneIndex()) {
////			String vehicleZone = vehicle.getZone();
////			String nodeZone = node.getZone();
////			if (isDriveAllowed(vehicleZone, nodeZone) == false) {
////				routeDebug.append("PathSearch Not Allowed at node:").append(node.getNodeId()).append(", ").append(nodeZone).append(" ");
////				return MAXCOST_TIMEBASE;
////			}
////			arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
////		}
//		double time = checkZoneAllowed(vehicle, prevNode, node, arrivedTime);
//		if (time >= MAXCOST_TIMEBASE) {
//			routeDebug.append("PathSearch Not Allowed at node:").append(node.getNodeId()).append(", ").append(node.getZone()).append(" ");
//			return MAXCOST_TIMEBASE;
//		} else {
//			arrivedTime = time;
//		}
//
//		// Section 레벨에 따른 Vehicle Penalty 부여
//		if (sectionLevelCount < MAX_SECTION_COUNT_FOR_VEHICLEPENALTY) {
//			arrivedTime += getVehiclePenalty(node);
//		}
//
//		// arrivedTime이 Max 값에 도달한 경우(주행 불가)
//		if (arrivedTime >= MAXCOST_TIMEBASE) {
//			arrivedTime = MAXCOST_TIMEBASE;
//			routeDebug.append(" arrivedTime is overed MAX_COST. ");
//		}
//
//		// 2015.03.04 by zzang9un : From과 Next를 함께 보도록 수정(PassDoor Node에서 출발할 경우 PrevNode를 봐야함)
//		PassDoor pbFrom = null;
//		PassDoor pbPrev = null;
////		pb = passDoorManager.findPassDoor(node.getNodeId());
//		pbFrom = node.getPassDoor();
//		pbPrev = prevNode.getPassDoor();
//		if (pbFrom != null && pbFrom.checkPassable() == false) {
//			routeDebug.append("Can't pass PassDoor at node:" + pbFrom.getNodeId() + "(" + pbFrom.getPassDoorId() + ")");
//			return MAXCOST_TIMEBASE;
//		} else if (pbPrev != null && pbPrev.checkPassable() == false) {
//			routeDebug.append("Can't pass PassDoor at node:" + pbPrev.getNodeId() + "(" + pbPrev.getPassDoorId() + ")");
//			return MAXCOST_TIMEBASE;
//		}
//		
//		return arrivedTime;
//	}
	
	/**
	 * search Shortest Path of a vehicle to a targetNode
	 * 
	 * @param vehicle VehicleData
	 * @param targetNode String
	 * @return true if successfully search path of a vehicle to a targetNode
	 */
	public boolean searchShortestVehiclePath(VehicleData vehicle, String targetNode) {
		if (vehicle == null) {
			return false;
		}

		// PathSearch 요청 초기화
		vehicle.setPathSearchRequest(false);
		
		StringBuffer request = new StringBuffer();
//		request.append(" searchShortestVehiclePath() requested. Vehicle:").append(vehicle.getVehicleId()).append(", DriveStopNode:").append(vehicle.getDriveStopNode()).append(" -> TargetNode:").append(targetNode);
//		trace(request.toString());
		request.append("searchShortestVehiclePath() requested. DriveStopNode:").append(vehicle.getDriveStopNode()).append(" -> TargetNode:").append(targetNode);
		trace(vehicle.getVehicleId(), request.toString());
		
		long processTime = System.currentTimeMillis();
		boolean retVal = false;
		try {
			retVal = searchShortestVehiclePathToTargetNode(vehicle, vehicle.getDriveStopNode(), nodeManager.getNode(targetNode));
		} catch (Exception e) {
			traceException("searchShortestVehiclePath() - " + vehicle.getVehicleId(), e);
		} finally {
			if ((System.currentTimeMillis()-processTime) >= 100) {
				StringBuffer log = new StringBuffer();
//				log.append(" searchVehiclePath() TimeDelay=").append(System.currentTimeMillis()-processTime).append(" ").append(vehicle.getVehicleId()).append(" ").append(targetNode);
//				trace(log.toString());
				log.append("searchShortestVehiclePath() TimeDelay=").append(System.currentTimeMillis()-processTime);
				trace(vehicle.getVehicleId(), log.toString());
			}
		}
		
		return retVal;
	}
	
	/**
	 * search Shortest Path of a vehicle to a targetNode
	 * 
	 * @param vehicle VehicleData
	 * @param targetNode String
	 * @return true if successfully search path of a vehicle to a targetNode
	 */
	private boolean searchShortestVehiclePathToTargetNode(VehicleData vehicle, Node fromNode, Node toNode) {
		long pathSearchStartedTime = System.nanoTime();
		
		// fromNode, toNode 확인
		if (fromNode == null || toNode == null) {
			return false;
		} else if (fromNode.equals(toNode)) {
			LinkedList<Node> routedNodeList = new LinkedList<Node>();
			routedNodeList.add(0, fromNode);
			return vehicle.setRoutedNodeList(routedNodeList);
		}
		
		String vehicleId = vehicle.getVehicleId();
		Node searchNode = null;
		boolean arrived = false;
		double arrivedTime = 0;
		double moveTime = 0;
		long distance = 0;
		double targetArrivedTime = MAXCOST_TIMEBASE;
		fromNode.setArrivedTime(vehicleId, arrivedTime, getMetropolitanTimeCost(fromNode, toNode));
		
		int loopCount = 0;
		int checkedNodeCount = 0;
		int sectionIndex = 0;
		
		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addQueueNode(vehicleId, fromNode, queueNodeList);
		addQueueNodeWithHeuristicCost(vehicleId, fromNode, queueNodeList);
		
		LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
		resetRouteNodeList.add(fromNode);
		
		StringBuffer route = new StringBuffer();
		route.append("[" + toNode.getNodeId() + "] ");
		
		boolean isException = false;
		try {
			while (true) {
				loopCount++;
				
				searchNode = getQueuedNode(queueNodeList);
				if (searchNode == null)
					break;
				
				for (int i = 0; i < searchNode.getSectionCount(); i++) {
					loopCount++;
					
//					arrivedTime = searchNode.getArrivedTime(vehicleId);
					NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
					if (arrivedTimeInfo != null) {
						arrivedTime = arrivedTimeInfo.getArrivedTime();
						moveTime = arrivedTimeInfo.getMoveTime();
						distance = arrivedTimeInfo.getDistance();
						sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
					} else {
						arrivedTime = MAXCOST_TIMEBASE;
						moveTime = MAXCOST_TIMEBASE;
						distance = MAXCOST_DISTANCEBASE;
					}
					
					Section section = searchNode.getSection(i);
					Node prevNode = searchNode;
					int pos = section.getNodeIndex(searchNode);
					
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						loopCount++;
						checkedNodeCount++;
						Node node = section.getNode(j);
//						arrivedTime += node.getMoveInTime(prevNode) + node.getTrafficPenalty();
						double moveInTime = node.getMoveInTime(prevNode);
						arrivedTime += moveInTime + node.getTrafficPenalty();
						moveTime += moveInTime;
						distance += node.getMoveInDistance(prevNode);
						
//						if (fromNode.equals(node)) {
//							arrivedTime = MAXCOST_TIMEBASE;
//						}
//						
//						// 2014.09.04 by MYM : checkZoneAlllowed 함수 사용
////						String prevNodeZone = prevNode.getZone();
////						String nodeZone = node.getZone();
////						if (prevNodeZone.equals(nodeZone) == false) {
////							String vehicleZone = vehicle.getZone();
////							if (isDriveAllowed(vehicleZone, nodeZone) == false) {
////								arrivedTime = MAXCOST_TIMEBASE;
////							}
////							arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
////						}
//						// HID 이상 상태 구간인 경우 체크
//						if (node.isAbnormalState(vehicle, false)) {
////							arrivedTime = MAXCOST_TIMEBASE;
//							break;
//						}
//						arrivedTime = checkZoneAllowed(vehicle, prevNode, node, arrivedTime);
//						if (arrivedTime >= MAXCOST_TIMEBASE) {
//							break;
//						}
						
						double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, sectionIndex, route, section, j == 1);
						if (time == MAXCOST_TIMEBASE) {
							break;
						} else {
							arrivedTime = time;
						}
						
						// 2014.02.03 by MYM : Disabled Link 처리
//						if (node.setShortestArrivedTime(vehicleId, prevNode, arrivedTime)) {
						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, toNode), moveTime, distance, section, sectionIndex)) {
							prevNode = node;
//							arrivedTime = node.getArrivedTime(vehicleId);
							arrivedTimeInfo = node.getArrivedTimeInfo(vehicleId);
							if (arrivedTimeInfo != null) {
								arrivedTime = arrivedTimeInfo.getArrivedTime();
								moveTime = arrivedTimeInfo.getMoveTime();
								distance = arrivedTimeInfo.getDistance();
							}
							
							if (toNode.equals(node)) {
								if (arrivedTime < targetArrivedTime) {
									targetArrivedTime = arrivedTime;
								}
								arrived = true;
								break;
							}
//							if (arrivedTime >= targetArrivedTime) {
							if (arrivedTime + node.getHeuristicCost(vehicleId) >= targetArrivedTime) {
								break;
							}
							if (j == section.getNodeCount()-1) {
//								addQueueNode(vehicleId, node, queueNodeList);
								addQueueNodeWithHeuristicCost(vehicleId, node, queueNodeList);
								resetRouteNodeList.add(node);
							}
						} else {
							break;
						}
					}
				}
			}
			
			if (arrived == true) {
				StringBuffer log = new StringBuffer();
				NodeArrivedTimeInfo arrivedTimeInfo = toNode.getArrivedTimeInfo(vehicleId);
				log.append("Shortest Path Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId());
				log.append(", Cost:").append(targetArrivedTime).append(", Time:").append(arrivedTimeInfo.getMoveTime()).append(", Distance:").append(arrivedTimeInfo.getDistance());
				log.append(", loopCnt:").append(loopCount);
				log.append(", CheckedNode:").append(checkedNodeCount);
				log.append(", Time:").append((double)((System.nanoTime() - pathSearchStartedTime)/1000)/1000).append(" ms");
				trace(vehicleId, log.toString());
				
				LinkedList<Node> routedNodeList = new LinkedList<Node>();
				Node node = toNode;
				while (fromNode.equals(node) == false) {
					routedNodeList.add(0, node);
					node = node.getPrevNode(vehicleId);
				}
				routedNodeList.add(0, node);
				return vehicle.setRoutedNodeList(routedNodeList);
			} else {
				// 2011.10.26 by PMM
				// 로그 추가
//				trace(route.toString());
				trace(vehicleId, route.toString());
			}
		} catch (Exception e) {
			traceException("searchShortestVehiclePathToTargetNode() - " + vehicleId, e);
			isException = true;
		} finally {
			vehicle.resetDetourNode();
			if (isException) {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
			} else {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
			}
		}
		return false;
	}
	
	// 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
//	private double getMetropolitanTimeCost(Node node1, Node node2) {
//		double metropolitanTimeCost = OcsConstant.MAXCOST_TIMEBASE;
//		if (node1 != null && node2 != null) {
//			metropolitanTimeCost = (Math.abs(node1.getLeft() - node2.getLeft()) + Math.abs(node1.getTop() - node2.getTop())) / 3300; 
//		}
//		return metropolitanTimeCost;
//	}
//	
//	private void addQueueNodeWithHeuristicCost(String vehicleId, Node node, LinkedList<Node> queueNodeList) {
//		if (node != null) {
//			Node tempNode = null;
//			for (int i = 0; i < queueNodeList.size(); i++) {
//				tempNode = queueNodeList.get(i);
//				if (tempNode != null) {
//					if (node.getHeuristicCost(vehicleId) < tempNode.getHeuristicCost(vehicleId)) {
//						queueNodeList.add(i, node);
//						return;
//					} else if (node.getHeuristicCost(vehicleId) == tempNode.getHeuristicCost(vehicleId)) {
//						if (node.getArrivedTime(vehicleId) < tempNode.getArrivedTime(vehicleId)) {
//							queueNodeList.add(i, node);
//							return;
//						}
//					}
//				}
//			}
//			queueNodeList.add(node);
//		}
//	}
//	
//	/**
//	 * update updateOperationalParameters
//	 * 
//	 */
//	public void updateOperationalParameters() {
//		super.updateOperationalParameters();
//		
//		isFailureOHTDetourSearchUsed = ocsInfoManager.isFailureOHTDetourSearchUsed();
//		isCollisionNodeCheckUsed = ocsInfoManager.isCollisionNodeCheckUsed();
//		
//		if (isCollisionNodeCheckUsed &&
//				isNearByDrive == false &&
//				abnormalVehiclesCollisionNodeSet != null) {
//			isCollisionNodeCheckNeeded = true;
//		} else {
//			isCollisionNodeCheckNeeded = false;
//		}
//	}
//	
//	private boolean containsAbnormalCollision(Node node) {
//		if (isCollisionNodeCheckNeeded && node != null) {
//			if (abnormalVehiclesCollisionNodeSet.contains(node)) {
//				return true;
//			}
//		}
//		return false;
//	}
}
