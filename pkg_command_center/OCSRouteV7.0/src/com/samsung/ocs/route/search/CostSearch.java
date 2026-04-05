package com.samsung.ocs.route.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsInfoConstant.COSTSEARCH_OPTION;
import com.samsung.ocs.common.constant.OcsInfoConstant.VEHICLECOMM_TYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.Vehicle;

/**
 * CostSearch Class, OCS 3.0 for Unified FAB
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

public class CostSearch extends Search {
	private HashSet<String> abnormalNodeIdList = null;
	private HashMap<String, Object> enabledVehicleList = null;
	private HashMap<String, ArrayList<Vehicle>> currNodeVehicleMap = null;
	
	/**
	 * Constructor of YieldSearch class.
	 */
	public CostSearch() {
		super();
		abnormalNodeIdList = nodeManager.getAbnormalNodeIdList();
		currNodeVehicleMap = new HashMap<String, ArrayList<Vehicle>>();
	}
	
	/**
	 * search a cost of vehicle to drive from a fromNode to a toNode
	 * 
	 * @param vehicle Vehicle
	 * @param fromNode Node
	 * @param toNode Node
	 * @param searchLimit double
	 * @param costSearchOption COSTSEARCH_OPTION
	 */
	public double costSearch(Vehicle vehicle, Node fromNode, Node toNode, double searchLimit, COSTSEARCH_OPTION costSearchOption) {
		if (vehicle == null || fromNode == null || toNode == null) {
			return MAXCOST_TIMEBASE;
		}
		
		double arrivedTime = 0;
		boolean nodeFound = false;
		long key = System.nanoTime();
		
		// toNode까지 경로 탐색
//		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addCostQueueNode(key, fromNode, queueNodeList);
		PriorityQueue<Node> queueNodeList = new PriorityQueue<Node> (1024, new NodeComparator1(key));
		queueNodeList.add(fromNode);
		Node searchNode = null;
		fromNode.setCostArrivedTime(key, arrivedTime);
		
		String vehicleZone = vehicle.getZone();
		String nodeZone = "";
		
		while (true) {			
			if (fromNode == toNode) {
				nodeFound = true;
				break;
			}
//			searchNode = getQueuedNode(queueNodeList);
			searchNode = queueNodeList.poll();
			if (searchNode == null)
				break;
			
			for (int i = 0; i < searchNode.getSectionCount(); i++) {
				arrivedTime = searchNode.getCostArrivedTime(key);
				Section section = searchNode.getSection(i);
				Node prevNode = searchNode;
				int pos = section.getNodeIndex(searchNode);
				for (int j = pos + 1; j < section.getNodeCount(); j++) {
					Node node = section.getNode(j);
					nodeZone = node.getZone();
					if (prevNode.getZoneIndex() != node.getZoneIndex()) {
						if (isDriveAllowed(vehicleZone, nodeZone) == false) {
							break;
						}
						arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
					}
					
					// HID Status
					if (node.getHid() != prevNode.getHid() &&
							node.getHid() != null &&
							node.getHid().isAbnormalState()) {
						break;
					}
					
					// 장애 OHT 체크.
					if (abnormalNodeIdList.contains(node.getNodeId()))	{
						break;
					}
					
					arrivedTime += node.getMoveInTime(prevNode);
					
					// 2012.07.31 by PMM
					// Curr ~ Stop, Stop ~ Target에 대한 Cost 계산에서는 Penalty 고려할 필요 없음.
					// JobAssign 시, LOCATE -> Assign 전환 기준 등에서 문제의 소지가 있음.
					if (arrivedTime >= MAXCOST_TIMEBASE) {
						arrivedTime = MAXCOST_TIMEBASE;
						break;
					}
					
					if (node.setCostArrivedTime(key, prevNode, arrivedTime)) {					
						prevNode = node;
						arrivedTime = node.getCostArrivedTime(key);
						
						if (node == toNode) {
							nodeFound = true;
							break;
						} else if (arrivedTime > searchLimit) {
							// search Limit 보다 큰 경우 추가 search를 하지 않음
							break;
						} else if (arrivedTime >= MAXCOST_TIMEBASE) { 
							break;
						}

						if (j == section.getNodeCount()-1) {
//							addCostQueueNode(key, node, queueNodeList);
							queueNodeList.add(node);
						}
					} else {
						break;
					}
				}
				if (nodeFound) {
					break;
				}
			}
			if (nodeFound) {
				break;
			}
		}
		if (nodeFound && arrivedTime < MAXCOST_TIMEBASE) {
			return arrivedTime;
		} else {
			return MAXCOST_TIMEBASE;
		}
	}
	
	/**
	 * search a cost of Load Transfer Time from a sourceNode to a destNode
	 * 
	 * @param trCmd TrCmd 
	 * @param assignableVehicleZone String
	 * @param sourceNode Node
	 * @param destNode Node
	 * @param costSearchOption COSTSEARCH_OPTION
	 */
	public double costSearchForLoadTransferTime(TrCmd trCmd, String assignableVehicleZone, Node sourceNode, Node destNode, COSTSEARCH_OPTION costSearchOption) {
		double arrivedTime = 0;
		boolean nodeFound = false;
		long key = System.nanoTime();
		
		// toNode까지 경로 탐색
//		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addCostQueueNode(key, sourceNode, queueNodeList);
		PriorityQueue<Node> queueNodeList = new PriorityQueue<Node> (1024, new NodeComparator1(key));
		queueNodeList.add(sourceNode);
		Node searchNode = null;
		sourceNode.setCostArrivedTime(key, arrivedTime);
		String nodeZone = "";
		
		while (true) {			
			if (sourceNode == destNode) {
				nodeFound = true;
				break;
			}
//			searchNode = getQueuedNode(queueNodeList);
			searchNode = queueNodeList.poll();
			if (searchNode == null)
				break;
			
			for (int i = 0; i < searchNode.getSectionCount(); i++) {
				arrivedTime = searchNode.getCostArrivedTime(key);
				Section section = searchNode.getSection(i);
				Node prevNode = searchNode;
				int pos = section.getNodeIndex(searchNode);
				for (int j = pos + 1; j < section.getNodeCount(); j++) {
					Node node = section.getNode(j);
					nodeZone = node.getZone();
					if (prevNode.getZoneIndex() != node.getZoneIndex()) {
						if (isDriveAllowed(assignableVehicleZone, nodeZone) == false) {
							break;
						}
						arrivedTime += getZoneOverPenalty(assignableVehicleZone, nodeZone);
					}
					
					// HID Status
					if (node.getHid() != prevNode.getHid() &&
							node.getHid() != null &&
							node.getHid().isAbnormalState()) {
						break;
					}
					
					// 장애 OHT 체크.
					if (abnormalNodeIdList.contains(node.getNodeId()))	{
						break;
					}
					
					arrivedTime += node.getMoveInTime(prevNode);
					
					if (arrivedTime >= MAXCOST_TIMEBASE) {
						arrivedTime = MAXCOST_TIMEBASE;
						break;
					}
					
					if (node.setCostArrivedTime(key, prevNode, arrivedTime)) {					
						prevNode = node;
						arrivedTime = node.getCostArrivedTime(key);
						
						if (node == destNode) {
							nodeFound = true;
							break;
						} else if (arrivedTime >= MAXCOST_TIMEBASE) { 
							break;
						}

						if (j == section.getNodeCount()-1) {
//							addCostQueueNode(key, node, queueNodeList);
							queueNodeList.add(node);
						}
					} else {
						break;
					}
				}
				if (nodeFound) {
					break;
				}
			}
			if (nodeFound) {
				break;
			}
		}
		if (nodeFound && arrivedTime < MAXCOST_TIMEBASE) {
			return arrivedTime;
		} else {
			return MAXCOST_TIMEBASE;
		}
	}
	
	/**
	 * search a cost of vehicle to drive from a stopNode to toNodes
	 * 
	 * @param vehicle Vehicle
	 * @param stopNode Node
	 * @param costMapToNodes HashMap<String, Double>
	 * @param isCostSearchFromStopNode boolean
	 * @param searchLimit double
	 * @param costSearchOption COSTSEARCH_OPTION
	 */
	public boolean costSearch(Vehicle vehicle, Node stopNode, HashMap<String, Double> costMapToNodes, boolean isCostSearchFromStopNode, double searchLimit, COSTSEARCH_OPTION costSearchOption) {
		assert vehicle != null;
		assert stopNode != null;
		
		double arrivedTime = 0;
		Node fromNode = stopNode;
		Node currNode = (Node)nodeManager.getNode(vehicle.getCurrNode());
		Node targetNode = (Node)nodeManager.getNode(vehicle.getTargetNode());
		
		String vehicleZone = vehicle.getZone();
		String nodeZone = "";
		
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();
		
		if (currNode != null && stopNode != null && currNode != stopNode) {
			arrivedTime += costSearch(vehicle, currNode, stopNode, searchLimit, costSearchOption);
		}
		
		if (isCostSearchFromStopNode == false) {
			if (stopNode != null && targetNode != null && stopNode != targetNode) {
				arrivedTime += costSearch(vehicle, stopNode, targetNode, searchLimit, costSearchOption);
			}
			fromNode = targetNode;
		}
		
		int nodeCount = costMapToNodes.size();
		// toNode까지 경로 탐색
//		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addCostQueueNode(vehicle, fromNode, queueNodeList);
		PriorityQueue<Node> queueNodeList = new PriorityQueue<Node> (1024, new NodeComparator2(vehicle));
		queueNodeList.add(fromNode);
		Node searchNode = null;
		fromNode.setCostArrivedTime(vehicle, arrivedTime);
		
		if (costMapToNodes.containsKey(fromNode.getNodeId())) {
			nodeCount--;
		}
		int sectionCount = 0;
		while (true) {			
			if (nodeCount == 0) {
				break;
			}
			
//			searchNode = getQueuedNode(queueNodeList);
			searchNode = queueNodeList.poll();
			if (searchNode == null) {
				break;
			}
			
			for (int i = 0; i < searchNode.getSectionCount(); i++) {
				sectionCount++;
				arrivedTime = searchNode.getCostArrivedTime(vehicle);
				
				Section section = searchNode.getSection(i);
				Node prevNode = searchNode;
				int pos = section.getNodeIndex(searchNode);
				
				for (int j = pos + 1; j < section.getNodeCount(); j++) {
					Node node = section.getNode(j);
					if (fromNode == node) {
						break;
					}
					
					nodeZone = node.getZone();
					if (prevNode.getZoneIndex() != node.getZoneIndex()) {
						if (isDriveAllowed(vehicleZone, nodeZone) == false) {
							break;
						}
						arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
					}
					
					// HID Status
					if (node.getHid() != prevNode.getHid() &&
							node.getHid() != null &&
							node.getHid().isAbnormalState()) {
						break;
					}
					
					// 장애 OHT 체크.
					if (abnormalNodeIdList.contains(node.getNodeId()))	{
						break;
					}
					
					arrivedTime += node.getMoveInTime(prevNode);
					
					switch (costSearchOption) {
						case NODE_PENALTY:
						{
							arrivedTime += node.getTrafficPenalty();
							break;
						}
						case VEHICLE_PENALTY:
						{
							arrivedTime += getVehiclePenaltyForCostSearch(node, sectionCount);
							break;
						}
						case HYBRID:
						{
							arrivedTime += node.getTrafficPenalty();
							arrivedTime += getVehiclePenaltyForCostSearch(node, sectionCount);
							break;
						}
						default:
							break;
					}
					
					// 2015.02.24 by MYM : 장애 지역 우회 기능
					arrivedTime += section.getDetourPenalty(prevNode, prevNode.getCostPrevNode(vehicle));
					if (arrivedTime >= MAXCOST_TIMEBASE) {
						arrivedTime = MAXCOST_TIMEBASE;
						break;
					}

					if (arrivedTime > searchLimit) {
						break;
					}
					
					// 2014.02.03 by MYM : Disabled Link 처리
//					if (node.setCostArrivedTime(vehicle, prevNode, arrivedTime)) {
					if (node.setCostArrivedTime(vehicle, prevNode, arrivedTime, section)) {
						prevNode = node;
						arrivedTime = node.getCostArrivedTime(vehicle);
						if (costMapToNodes.containsKey(node.getNodeId())) {
							nodeMap.put(node.getNodeId(), node);
							nodeCount--;
							if (arrivedTime > 0) {
								arrivedTime += 3;
							}
						}
						if (j == section.getNodeCount()-1) {
//							addCostQueueNode(vehicle, node, queueNodeList);
							queueNodeList.add(node);
						}
						if (nodeCount == 0) {
							break;
						}
					} else {
						break;
					}
				}
				
				if (nodeCount == 0) {
					break;
				}
			}
		}
		
		try {
			if (nodeCount < costMapToNodes.size()) {
				Set<String> searchKeys = new HashSet<String>(costMapToNodes.keySet());
				Node node;
				for (String searchKey : searchKeys) {
					node = nodeMap.get(searchKey);
					if (node == null) {
						node = nodeManager.getNode(searchKey);
					}
					arrivedTime = node.getCostArrivedTime(vehicle);
					if (arrivedTime < MAXCOST_TIMEBASE) {
						costMapToNodes.put(searchKey, arrivedTime);
					} else {
						costMapToNodes.put(searchKey, MAXCOST_TIMEBASE);
					}
				}
			}
		} catch (Exception e) {
			traceException("costSearch()", e);
		}
		return true;
	}

	//20221208 by y.won qr station 검증 절차 추가 메소드 구현 부분
	private boolean nodeStationValidation(Node currNode, Node stopNode,  Node targetNode, Station currStation, Station stopStation, Station targetStation, Vehicle vehicle) {
		try{
			// Node 및 Station 객체 확인
			if (currNode == null || stopNode == null || targetNode == null) {
				return false;
			}
			
			// Station 객체의 데이터 유효성 검사 1. parent, next Station 객체 및 offset 값 확인
			if (currStation != null) {
				if (currStation.getParentNode() == null || currStation.getNextNode() == null || currStation.getOffset() < 0) {
					return false;
				}
				
				// Station 객체의 데이터 유효성 검사 2. parent, next Station 객체의 유효성 검사
				String parentNodeID_currStation = currStation.getParentNodeId();
				String nextNodeID_currStation = currStation.getNextNodeId();
				if (nodeManager.isValidNode(parentNodeID_currStation) == false || nodeManager.isValidNode(nextNodeID_currStation) == false) {
					return false;
				}
			}
			
			if (stopStation != null) {
				if (stopStation.getParentNode() == null || stopStation.getNextNode() == null || stopStation.getOffset() < 0) {
					return false;
				}

				String parentNodeID_stopStation = stopStation.getParentNodeId();
				String nextNodeID_stopStation = stopStation.getNextNodeId();
				if (nodeManager.isValidNode(parentNodeID_stopStation) == false || nodeManager.isValidNode(nextNodeID_stopStation) == false) {
					return false;
				}
			}
			
			if (targetStation != null) {
				if (targetStation.getParentNode() == null || targetStation.getNextNode() == null || targetStation.getOffset() < 0) {
					return false;
				}

				String parentNodeID_targetStation = targetStation.getParentNodeId();
				String nextNodeID_targetStation = targetStation.getNextNodeId();
				if (nodeManager.isValidNode(parentNodeID_targetStation) == false || nodeManager.isValidNode(nextNodeID_targetStation) == false) {
					return false;
				}
			}
			
			return true;
		
		} catch (Exception e){
			// 예외 상황 발생 시 return 은 false 로 해서 다음 차량의 cost search 지속할 수 있도록 유도 
			return false;
		}
	}
	
	
	/**
	 * 2013.05.16 by KYK
	 * @param vehicle
	 * @param stopNode
	 * @param costMap
	 * @param targetStationMap
	 * @param isCostSearchFromStopNode
	 * @param searchLimit
	 * @param costSearchOption
	 * @return
	 */
	// 2023.02.20 add by YSJ for FAB QR Tag 부모Node 로의 Load -> Unload 불가로 NextNode 부터 CostSearch 하게 추가 (VehicleCommType = CHAR)
	public boolean costSearch(Vehicle vehicle, Node stopNode, HashMap<String, Double> costMap, HashMap<String, String> targetStationMap, boolean isCostSearchFromStopNode, double searchLimit, COSTSEARCH_OPTION costSearchOption, VEHICLECOMM_TYPE vehicleCommType) {
		assert vehicle != null;
		assert stopNode != null;
		
		double arrivedTime = 0;
		Node fromNode = stopNode;
		Node currNode = (Node)nodeManager.getNode(vehicle.getCurrNode());
		Node targetNode = (Node)nodeManager.getNode(vehicle.getTargetNode());		

		// 2013.05.03 by KYK
		Station fromStation = null;
		Station currStation = stationManager.getStation(vehicle.getCurrStation());
		Station stopStation = stationManager.getStation(vehicle.getStopStation());
		Station targetStation = stationManager.getStation(vehicle.getTargetStation());	
		
		// 20221208 by y.won qr station 검증 절차 추가
		if (nodeStationValidation(currNode, stopNode, targetNode, currStation, stopStation, targetStation, vehicle) == false) {
			 return false;
		}
		boolean isStationType = false;
		Node from = currNode;
		double currOffset = 0;
		double stopOffset = 0;
		double targetOffset = 0;
		if (stopStation != null && stopStation.getOffset() >= 0) { // 23.02.10 by ysj qr 직후 bcr 수행 못하도록
			if (vehicleCommType == VEHICLECOMM_TYPE.VEHICLECOMM_CHAR) {
				stopOffset = stopStation.getOffset();
				fromNode = stopStation.getNextNode();
				fromStation = stopStation;
				isStationType = true;
				// 2014.12.24 by KYK
				// 2014.04.25 by KYK
//				arrivedTime += fromNode.getMoveInTime(currNode) - stopOffset / 2500;
				arrivedTime += fromNode.getMoveInTime(stopNode) - stopOffset / 2500;
			} else if (vehicleCommType != VEHICLECOMM_TYPE.VEHICLECOMM_CHAR && stopStation.getOffset() > 0) {
				stopOffset = stopStation.getOffset();
				fromNode = stopStation.getNextNode();
				fromStation = stopStation;
				isStationType = true;
				// 2014.12.24 by KYK
				// 2014.04.25 by KYK
//				arrivedTime += fromNode.getMoveInTime(currNode) - stopOffset / 2500;
				arrivedTime += fromNode.getMoveInTime(stopNode) - stopOffset / 2500;
			}
		}
		// 1. calculate cost [Current ~ Stop]
		boolean isCurrStation = false;
		if (currStation != null && currStation.getOffset() >= 0) { // 23.02.10 by ysj qr 직후 bcr 수행 못하도록
			if (vehicleCommType == VEHICLECOMM_TYPE.VEHICLECOMM_CHAR) {
				currOffset = currStation.getOffset();
				isCurrStation = true;  // 23.02.10 by ysj qr 직후 bcr 수행 못하도록
			} else if (vehicleCommType != VEHICLECOMM_TYPE.VEHICLECOMM_CHAR && currStation.getOffset() > 0) {
				currOffset = currStation.getOffset();
				isCurrStation = true;  // 23.02.10 by ysj qr 직후 bcr 수행 못하도록
			}
		}
		if (currNode.equals(stopNode) == false) {
			if (isCurrStation) {
				from = currStation.getNextNode();				
			}
			arrivedTime += costSearch(vehicle, from, stopNode, searchLimit, costSearchOption);
		}		
		arrivedTime += ((stopOffset - currOffset) / 2500.0);
		// 2. calculate cost [Stop ~ Target] (loadVHL : nextJob reserved)
		if (isCostSearchFromStopNode == false) {
			fromNode = targetNode;
			fromStation = null;
			boolean isTargetStation = false;
			if (targetStation != null && targetStation.getOffset() > 0) {
				targetOffset = targetStation.getOffset();
				fromNode = targetStation.getNextNode();
				fromStation = targetStation;
				isTargetStation = true;
			}
			if (stopNode != targetNode || targetOffset < stopOffset) {
				from = stopNode;
				if (isStationType) {
					from = stopStation.getNextNode();
				}
				arrivedTime += costSearch(vehicle, from, targetNode, searchLimit, costSearchOption);
				arrivedTime += ((targetOffset - stopOffset) / 2500);
			}			
			isStationType = isTargetStation;
		}		
		// 3. calculate cost [From ~ TargetList] (Search from nextnode if From Station
		boolean isFromStation = false;
		if (fromStation != null && fromStation.getOffset() >= 0) {
			isFromStation = true;
		}
		ArrayList<String> sameNodeList = new ArrayList<String>();
		HashMap<String, Double> costMapToNodes = new HashMap<String, Double>(costMap);
		String targetNodeId = null;
		if (targetStationMap.size() > 0) {
			for (String targetId : targetStationMap.keySet()) {
				costMapToNodes.remove(targetId);
				targetNodeId = targetStationMap.get(targetId);
				if (targetNodeId != null) {
					costMapToNodes.put(targetNodeId, MAXCOST_TIMEBASE);
					if (isFromStation) {
						if (targetNodeId.equals(fromStation.getParentNodeId())) {
							sameNodeList.add(targetId);
						}						
					}
				}
			}
		}
		costSearchToTargetList(vehicle, fromNode, costMapToNodes, searchLimit, arrivedTime, costSearchOption);
		
		// 4. add cost : station offset
		Station station = null;
		double newArrivedTime = 0.0;
		ArrayList<String> keySet = new ArrayList<String>(costMap.keySet());
		if (targetStationMap.size() > 0) {
			for (String targetId : targetStationMap.keySet()) {
				targetNodeId = targetStationMap.get(targetId);
				if (costMapToNodes.containsKey(targetNodeId)) {
					newArrivedTime = costMapToNodes.get(targetNodeId);
					if (newArrivedTime < MAXCOST_TIMEBASE) {
						station = stationManager.getStation(targetId);
						if (station != null) {
							newArrivedTime += (station.getOffset() / 2500.0);
						}
					}
					costMap.put(targetId, newArrivedTime);						
				}
				keySet.remove(targetId);
			}
		}
		for (String nodeId: keySet) {
			if (costMapToNodes.containsKey(nodeId)) {
				costMap.put(nodeId, costMapToNodes.get(nodeId));				
			}
		}
		
		// 5. recalculate targetNodes same as Vhl's fromNode 
		if (isFromStation) {
			double gap = 0.0;
			for (String targetId : sameNodeList) {
				newArrivedTime = 0.0;
				station = stationManager.getStation(targetId);
				// 2013.07.30 by KYK : parentNode 같지만 nextNode 다른 경우 고려
				if (station != null) {
					if (station.getNextNode() == fromStation.getNextNode()) {
						gap = station.getOffset() - fromStation.getOffset();
						if (gap >= 0) {
							newArrivedTime = arrivedTime + (gap / 2500);
							costMap.put(targetId, newArrivedTime);
						}
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * 2013.05.16 by KYK
	 * @param vehicle
	 * @param fromNode
	 * @param costMapToNodes
	 * @param searchLimit
	 * @param costSearchOption
	 */
	private void costSearchToTargetList(Vehicle vehicle, Node fromNode, HashMap<String, Double> costMapToNodes, double searchLimit,	double arrivedTime, COSTSEARCH_OPTION costSearchOption) {
		String vehicleZone = vehicle.getZone();
		String nodeZone = "";
		HashMap<String, Node> nodeMap = new HashMap<String, Node>();

		int nodeCount = costMapToNodes.size();
		// toNode까지 경로 탐색
//		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addCostQueueNode(vehicle, fromNode, queueNodeList);
		PriorityQueue<Node> queueNodeList = new PriorityQueue<Node> (1024, new NodeComparator2(vehicle));
		queueNodeList.add(fromNode);
		Node searchNode = null;
		fromNode.setCostArrivedTime(vehicle, arrivedTime);
		
		if (costMapToNodes.containsKey(fromNode.getNodeId())) {
			nodeCount--;
		}
		int sectionCount = 0;
		while (true) {			
			if (nodeCount == 0) {
				break;
			}
			
//			searchNode = getQueuedNode(queueNodeList);
			searchNode = queueNodeList.poll();
			if (searchNode == null) {
				break;
			}
			
			for (int i = 0; i < searchNode.getSectionCount(); i++) {
				sectionCount++;
				arrivedTime = searchNode.getCostArrivedTime(vehicle);
				
				Section section = searchNode.getSection(i);
				Node prevNode = searchNode;
				int pos = section.getNodeIndex(searchNode);
				
				for (int j = pos + 1; j < section.getNodeCount(); j++) {
					Node node = section.getNode(j);
					if (fromNode == node) {
						break;
					}
					
					nodeZone = node.getZone();
					if (prevNode.getZoneIndex() != node.getZoneIndex()) {
						if (isDriveAllowed(vehicleZone, nodeZone) == false) {
							break;
						}
						arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
					}
					
					// HID Status
					if (node.getHid() != prevNode.getHid() &&
							node.getHid() != null &&
							node.getHid().isAbnormalState()) {
						break;
					}
					
					// 장애 OHT 체크.
					if (abnormalNodeIdList.contains(node.getNodeId()))	{
						break;
					}
					
					arrivedTime += node.getMoveInTime(prevNode);
					
					switch (costSearchOption) {
						case NODE_PENALTY:
						{
							arrivedTime += node.getTrafficPenalty();
							break;
						}
						case VEHICLE_PENALTY:
						{
							arrivedTime += getVehiclePenaltyForCostSearch(node, sectionCount);
							break;
						}
						case HYBRID:
						{
							arrivedTime += node.getTrafficPenalty();
							arrivedTime += getVehiclePenaltyForCostSearch(node, sectionCount);
							break;
						}
						default:
							break;
					}

					// 2015.02.24 by MYM : 장애 지역 우회 기능
					arrivedTime += section.getDetourPenalty(prevNode, prevNode.getCostPrevNode(vehicle));
					if (arrivedTime >= MAXCOST_TIMEBASE) {
						arrivedTime = MAXCOST_TIMEBASE;
						break;
					}

					if (arrivedTime > searchLimit) {
						break;
					}

					// 2014.02.03 by MYM : Disabled Link 처리
//					if (node.setCostArrivedTime(vehicle, prevNode, arrivedTime)) {
					if (node.setCostArrivedTime(vehicle, prevNode, arrivedTime, section)) {
						prevNode = node;
						arrivedTime = node.getCostArrivedTime(vehicle);
						if (costMapToNodes.containsKey(node.getNodeId())) {
							nodeMap.put(node.getNodeId(), node);
							nodeCount--;
							if (arrivedTime > 0) {
								arrivedTime += 3;
							}
						}
						if (j == section.getNodeCount()-1) {
//							addCostQueueNode(vehicle, node, queueNodeList);
							queueNodeList.add(node);
						}
						if (nodeCount == 0) {
							break;
						}
					} else {
						break;
					}
				}
				
				if (nodeCount == 0) {
					break;
				}
			}
		}
		
		try {
			if (nodeCount < costMapToNodes.size()) {
				Set<String> searchKeys = new HashSet<String>(costMapToNodes.keySet());
				Node node;
				for (String searchKey : searchKeys) {
					node = nodeMap.get(searchKey);
					if (node == null) {
						node = nodeManager.getNode(searchKey);
					}
					arrivedTime = node.getCostArrivedTime(vehicle);
					if (arrivedTime < MAXCOST_TIMEBASE) {
						costMapToNodes.put(searchKey, arrivedTime);
					} else {
						costMapToNodes.put(searchKey, MAXCOST_TIMEBASE);
					}
				}
			}
		} catch (Exception e) {
			traceException("costSearch()", e);
		}
	}

	/**
	 * search a cost of local vehicle to drive from a fromNode to bays
	 * 
	 * @param vehicle Vehicle
	 * @param fromNode Node
	 * @param costMapToBays HashMap<String, Double>
	 * @param searchLimit double
	 * @return true if successfully search a cost
	 */
	public boolean costSearchForLocalOHT(Vehicle vehicle, Node stopNode, HashMap<String, Double> costMapToBays, boolean isCostSearchFromStopNode, double searchLimit, COSTSEARCH_OPTION costSearchOption) {
		assert vehicle != null;
		assert stopNode != null;
		
		double arrivedTime = 0;
		Node fromNode = stopNode;
		Node currNode = (Node)nodeManager.getNode(vehicle.getCurrNode());
		Node targetNode = (Node)nodeManager.getNode(vehicle.getTargetNode());
		
		String vehicleZone = vehicle.getZone();
		String nodeZone = "";
		
		if (currNode != null && stopNode != null && currNode != stopNode) {
			arrivedTime += costSearch(vehicle, currNode, stopNode, searchLimit, costSearchOption);
		}
		
		if (isCostSearchFromStopNode == false) {
			if (targetNode != null && stopNode != null && targetNode != stopNode) {
				arrivedTime += costSearch(vehicle, stopNode, targetNode, searchLimit, costSearchOption);
			}
			fromNode = targetNode;
		}
		
		// to Bay까지 경로 탐색
//		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addCostQueueNode(vehicle, fromNode, queueNodeList);
		PriorityQueue<Node> queueNodeList = new PriorityQueue<Node> (1024, new NodeComparator2(vehicle));
		queueNodeList.add(fromNode);
		Node searchNode = null;
		fromNode.setCostArrivedTime(vehicle, arrivedTime);
		int bayCount = costMapToBays.size();
		
		while (true) {
//			searchNode = getQueuedNode(queueNodeList);
			searchNode = queueNodeList.poll();
			if (searchNode == null) {
				break;
			}
			
			for (int i = 0; i < searchNode.getSectionCount(); i++) {
				arrivedTime = searchNode.getCostArrivedTime(vehicle);
				Section section = searchNode.getSection(i);
				Node prevNode = searchNode;
				int pos = section.getNodeIndex(searchNode);
				for (int j = pos + 1; j < section.getNodeCount(); j++) {
					Node node = section.getNode(j);
					if (fromNode == node) {
						break;
					}
					
					nodeZone = node.getZone();
					if (prevNode.getZoneIndex() != node.getZoneIndex()) {
						if (isDriveAllowed(vehicleZone, nodeZone) == false) {
							break;
						}
						arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
					}
					
					// HID Status
					if (node.getHid() != prevNode.getHid() &&
							node.getHid() != null &&
							node.getHid().isAbnormalState()) {
						break;
					}
					
					// 장애 OHT 체크.
					if (abnormalNodeIdList.contains(node.getNodeId()))	{
						break;
					}
					
					arrivedTime += node.getMoveInTime(prevNode);
					
					switch (costSearchOption) {
						case NODE_PENALTY:
						{
							arrivedTime += node.getTrafficPenalty();
							break;
						}
						case VEHICLE_PENALTY:
						{
							arrivedTime += getVehiclePenaltyForCostSearch(node, i);
							break;
						}
						case HYBRID:
						{
							arrivedTime += node.getTrafficPenalty();
							arrivedTime += getVehiclePenaltyForCostSearch(node, i);
							break;
						}
						default:
							break;
					}
					
					// 2015.02.24 by MYM : 장애 지역 우회 기능
					arrivedTime += section.getDetourPenalty(prevNode, prevNode.getCostPrevNode(vehicle));
					if (arrivedTime >= MAXCOST_TIMEBASE) {
						arrivedTime = MAXCOST_TIMEBASE;
						break;
					}
					
					if (arrivedTime > searchLimit) {
						break;
					}
						
					// 2014.02.03 by MYM : Disabled Link 처리
//					if (node.setCostArrivedTime(vehicle, prevNode, arrivedTime)) {					
					if (node.setCostArrivedTime(vehicle, prevNode, arrivedTime, section)) {					
						prevNode = node;
						arrivedTime = node.getCostArrivedTime(vehicle);
						
						if (costMapToBays.containsKey(node.getBay())) {
							costMapToBays.put(node.getBay(), arrivedTime);
							bayCount--;
						}
						if (bayCount == 0) {
							break;
						}

						if (j == section.getNodeCount()-1) {
//							addCostQueueNode(vehicle, node, queueNodeList);
							queueNodeList.add(node);
						}
					} else {
						break;
					}
				}
				
				if (bayCount == 0) {
					break;
				}
			}
		}
		return true;
	}
	
	/**
	 * search a cost of idle vehicle to escape from current area
	 * 
	 * @param vehicle Vehicle
	 * @return time to escape from current area
	 */
	public double costSearchToEscapeArea(Vehicle vehicle) {
		assert vehicle != null;
		
		double arrivedTime = 0;
		double escapingTime = MAXCOST_TIMEBASE;
		Node currNode = (Node)nodeManager.getNode(vehicle.getCurrNode());
		Node stopNode = (Node)nodeManager.getNode(vehicle.getStopNode());
		Node fromNode = stopNode;
		
		String vehicleZone = vehicle.getZone();
		String nodeZone = "";
		
		if (currNode != null && stopNode != null && currNode != stopNode) {
			arrivedTime += costSearch(vehicle, currNode, stopNode, MAXCOST_TIMEBASE, COSTSEARCH_OPTION.HYBRID);
		}
		
		// to Bay까지 경로 탐색
//		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addCostQueueNode(vehicle, fromNode, queueNodeList);
		PriorityQueue<Node> queueNodeList = new PriorityQueue<Node> (1024, new NodeComparator2(vehicle));
		queueNodeList.add(fromNode);
		
		Node searchNode = null;
		fromNode.setCostArrivedTime(vehicle, arrivedTime);
		while (true) {
//			searchNode = getQueuedNode(queueNodeList);
			searchNode = queueNodeList.poll();
			if (searchNode == null) {
				break;
			}
			
			for (int i = 0; i < searchNode.getSectionCount(); i++) {
				arrivedTime = searchNode.getCostArrivedTime(vehicle);
				Section section = searchNode.getSection(i);
				Node prevNode = searchNode;
				int pos = section.getNodeIndex(searchNode);
				for (int j = pos + 1; j < section.getNodeCount(); j++) {
					Node node = section.getNode(j);
					if (fromNode == node) {
						break;
					}
					
					nodeZone = node.getZone();
					if (prevNode.getZoneIndex() != node.getZoneIndex()) {
						if (isDriveAllowed(vehicleZone, nodeZone) == false) {
							break;
						}
						arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
					}
					
					// HID Status
					if (node.getHid() != prevNode.getHid() &&
							node.getHid() != null &&
							node.getHid().isAbnormalState()) {
						break;
					}
					
					// 장애 OHT 체크.
					if (abnormalNodeIdList.contains(node.getNodeId()))	{
						break;
					}
					
					arrivedTime += node.getMoveInTime(prevNode);
					arrivedTime += node.getTrafficPenalty();
					arrivedTime += getVehiclePenaltyForCostSearch(node, i);
					
					if (arrivedTime >= MAXCOST_TIMEBASE) {
						arrivedTime = MAXCOST_TIMEBASE;
						break;
					}
					
					// 2015.06.02 by MYM : Disabled Link 처리
//					if (node.setCostArrivedTime(vehicle, prevNode, arrivedTime)) {
					if (node.setCostArrivedTime(vehicle, prevNode, arrivedTime, section)) {
						prevNode = node;
						arrivedTime = node.getCostArrivedTime(vehicle);
						if (stopNode.getArea() != node.getArea()) {
							if (arrivedTime < escapingTime) {
								escapingTime = arrivedTime;
							}
							break;
						}
						if (j == section.getNodeCount()-1) {
//							addCostQueueNode(vehicle, node, queueNodeList);
							queueNodeList.add(node);
						}
					} else {
						break;
					}
				}
			}
		}
		return escapingTime;
	}
	
	/**
	 * get Vehicle Penalty
	 * 
	 * @param vehicle Vehicle
	 * @param toNode Node
	 * @return VehiclePenalty double
	 */
	private double getVehiclePenaltyForCostSearch(Node node, int index) {
		double vehiclePenalty = 0.0;
		if (currNodeVehicleMap != null) {
			if (index < 15) {
				ArrayList<Vehicle> drivedVehicleList = currNodeVehicleMap.get(node.getNodeId());
				if (drivedVehicleList != null) {
					Vehicle vehicle = null;
					for (int i = 0; i < drivedVehicleList.size(); i++) {
						vehicle = drivedVehicleList.get(i);
						if (vehicle != null) {
							if (vehicle.getVehicleMode() == 'A') {
								if (vehicle.getErrorCode() == OcsConstant.COMMUNICATION_FAIL || vehicle.getState() == 'E') {
									vehiclePenalty += errorVehiclePenalty;
								} else if (vehicle.getRequestedType() == REQUESTEDTYPE.STAGEWAIT) {
									// 2014.06.03 by MYM : [Stage Locate 기능] Stage 위치에서 대기 중인 Vehicle Penalty 부여
									double stageWaitVehiclePenalty = 0;
									try {
										stageWaitVehiclePenalty = Double.valueOf(vehicle.getRequestedData());
										if (stageWaitVehiclePenalty < 0) {
											stageWaitVehiclePenalty = 0;
										} else if (stageWaitVehiclePenalty > 50) {
											stageWaitVehiclePenalty = 50;
										}
									} catch (Exception e) {
										traceException("getVehiclePenaltyForCostSearch()", e);
									}
									vehiclePenalty += stageWaitVehiclePenalty;
								} else {
									switch (vehicle.getState()) {
										case 'U':
										case 'L':
											vehiclePenalty += workingVehiclePenalty;
											break;
										case 'G':
											if (trCmdManager.isVehicleRegistered(vehicle.getVehicleId()) &&
													vehicle.getStopNode().equals(vehicle.getTargetNode()) == true) {
												vehiclePenalty += workingVehiclePenalty * 2;
											} else {
												vehiclePenalty += goingVehiclePenalty;
											}
											break;
										default:
											if (vehicle.getState() == 'A' &&
													vehicle.getStopNode().equals(vehicle.getTargetNode()) == false) {
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
						}
					}
				}
			}
		}
		return vehiclePenalty;
	}
	
	public void setEnabledVehicleList(HashMap<String, Object> enabledVehicleList) {
		this.enabledVehicleList = enabledVehicleList;
	}
	
	public void updateCurrNodeVehicleMap() {
		if (enabledVehicleList != null) {
			currNodeVehicleMap.clear();
			Set<String> searchKeys = new HashSet<String>(enabledVehicleList.keySet());
			Vehicle vehicle = null;
			ArrayList<Vehicle> currNodeVehicleList = null;
			for (String searchKey : searchKeys) {
				vehicle = (Vehicle)enabledVehicleList.get(searchKey);
				if (vehicle != null) {
					if (currNodeVehicleMap.containsKey(vehicle.getCurrNode())) {
						currNodeVehicleList = currNodeVehicleMap.get(vehicle.getCurrNode());
						if (currNodeVehicleList != null) {
							if (currNodeVehicleList.contains(vehicle) == false) {
								currNodeVehicleList.add(vehicle);
							}
						}
					} else {
						currNodeVehicleList = new ArrayList<Vehicle>();
						currNodeVehicleList.add(vehicle);
						currNodeVehicleMap.put(vehicle.getCurrNode(), currNodeVehicleList);
					}
				}
			}
		}
	}
	
	public LinkedList<String> searchVehicleDrive(Vehicle vehicle) {
		LinkedList<String> driveNodeList = new LinkedList<String>();
		
		String currNodeId = vehicle.getCurrNode();
		String stopNodeId = vehicle.getStopNode();
		Node fromNode = nodeManager.getNode(currNodeId);
		Node toNode = nodeManager.getNode(stopNodeId);
		
		// 2015.06.12 by KYK : null check 먼저하도록 if -else 순서 바꿈
		if (fromNode == null || toNode == null) {
			driveNodeList.add(0, stopNodeId);
			driveNodeList.add(0, currNodeId);
			return driveNodeList;
		} else if (fromNode.equals(toNode)) {
			driveNodeList.add(0, currNodeId);
			return driveNodeList;
		}
		
		double arrivedTime = 0;
		boolean nodeFound = false;
		long key = System.nanoTime();
		
		// toNode까지 경로 탐색
//		LinkedList<Node> queueNodeList = new LinkedList<Node>();
//		addCostQueueNode(key, fromNode, queueNodeList);
		PriorityQueue<Node> queueNodeList = new PriorityQueue<Node> (1024, new NodeComparator1(key));
		queueNodeList.add(fromNode);
		Node searchNode = null;
		fromNode.setCostArrivedTime(key, arrivedTime);
		
		String vehicleZone = vehicle.getZone();
		String nodeZone = "";
		
		while (true) {			
			if (fromNode == toNode) {
				nodeFound = true;
				break;
			}
//			searchNode = getQueuedNode(queueNodeList);
			searchNode = queueNodeList.poll();
			if (searchNode == null)
				break;
			
			for (int i = 0; i < searchNode.getSectionCount(); i++) {
				arrivedTime = searchNode.getCostArrivedTime(key);
				Section section = searchNode.getSection(i);
				Node prevNode = searchNode;
				int pos = section.getNodeIndex(searchNode);
				for (int j = pos + 1; j < section.getNodeCount(); j++) {
					Node node = section.getNode(j);
					nodeZone = node.getZone();
					if (prevNode.getZoneIndex() != node.getZoneIndex()) {
						if (isDriveAllowed(vehicleZone, nodeZone) == false) {
							break;
						}
						arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
					}
					
					if (prevNode.getHid() != node.getHid() &&
							node.getHid() != null &&
							node.getHid().isAbnormalState()) {
						break;
					}
					if (abnormalNodeIdList.contains(node.getNodeId()))	{
						break;
					}
					
					arrivedTime += node.getMoveInTime(prevNode);
					
					// 2012.07.31 by PMM
					// Curr ~ Stop, Stop ~ Target에 대한 Cost 계산에서는 Penalty 고려할 필요 없음.
					// JobAssign 시, LOCATE -> Assign 전환 기준 등에서 문제의 소지가 있음.
					if (arrivedTime >= MAXCOST_TIMEBASE) {
						arrivedTime = MAXCOST_TIMEBASE;
						break;
					}
					
					if (node.setCostArrivedTime(key, prevNode, arrivedTime)) {					
						prevNode = node;
						arrivedTime = node.getCostArrivedTime(key);
						
						if (node == toNode) {
							nodeFound = true;
							break;
						} else if (arrivedTime >= MAXCOST_TIMEBASE) { 
							break;
						}

						if (j == section.getNodeCount()-1) {
//							addCostQueueNode(key, node, queueNodeList);
							queueNodeList.add(node);
						}
					} else {
						break;
					}
				}
				if (nodeFound) {
					break;
				}
			}
			if (nodeFound) {
				break;
			}
		}
		if (nodeFound && arrivedTime < MAXCOST_TIMEBASE) {
			if (nodeFound) {
				Node node = toNode;
				while (fromNode.equals(node) == false) {
					driveNodeList.add(0, node.getNodeId());
					node = node.getCostPrevNode(key);
				}
				driveNodeList.add(0, node.getNodeId());
				return driveNodeList;
			}
		}
		driveNodeList.add(0, stopNodeId);
		driveNodeList.add(0, currNodeId);
		return driveNodeList;
	}
	

	class NodeComparator1 implements Comparator<Node> {
		long key;
		
		public NodeComparator1(long key) {
			this.key = key;
		}
		
		public int compare(Node n1, Node n2) {
			double arrivalTime1 = n1.getCostArrivedTime(key);
			double arrivalTime2 = n2.getCostArrivedTime(key);
			if (arrivalTime1 < arrivalTime2) {
				return -1;
			} else if (arrivalTime1 > arrivalTime2) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	class NodeComparator2 implements Comparator<Node> {
		Vehicle key;
		
		public NodeComparator2(Vehicle key) {
			this.key = key;
		}
		
		public int compare(Node n1, Node n2) {
			double arrivalTime1 = n1.getCostArrivedTime(key);
			double arrivalTime2 = n2.getCostArrivedTime(key);
			if (arrivalTime1 < arrivalTime2) {
				return -1;
			} else if (arrivalTime1 > arrivalTime2) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
}
