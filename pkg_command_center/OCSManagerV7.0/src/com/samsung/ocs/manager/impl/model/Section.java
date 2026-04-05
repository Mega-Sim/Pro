package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;
import com.samsung.ocs.manager.impl.DetourControlManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;

/**
 * Section Class, OCS 3.0 for Unified FAB
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

public class Section {
	private static final String UNDERBAR = "_";
	
	protected String sectionId;
	protected String type;	
	protected String direction;
	protected String firstNodeId;
	protected String lastNodeId;
	protected boolean enabled;
	protected boolean twoWay;
	protected int sectionSpeed;
	protected char steerPosition;
	
	protected ArrayList<Node> nodeList;
	protected HashMap<Node, Integer> nodeIndexMap;// 2014.11.05 by MYM : SectionSearch
	protected double distance = 0.0;
	protected double moveInSectionTime = 0.0;
	protected double trafficPenalty = 0;
	protected ConcurrentHashMap<String, SectionArrivedTimeInfo> arrivedTimeTable = new ConcurrentHashMap<String, SectionArrivedTimeInfo>();
	
	// 2014.02.03 by MYM : Disabled Link 처리
	protected ConcurrentHashMap<String, Link> linkMap = new ConcurrentHashMap<String, Link>();
	private HashMap<Object, String> abnormalItemMap = new HashMap<Object, String>();
	private HashSet<Section> detourSectionSet = new HashSet<Section>();
	private HashSet<Section> detourUserSectionSet = new HashSet<Section>();
	private double detourPenalty = 0.0;
	
	// 2015.05.27 by MYM : Vehicle Traffic 분산
	private Traffic traffic = null;
	
	// 2015.08.08 by MYM : 장애 회피 오류 자동 체크 
	private long detourStatusChangedTime = System.currentTimeMillis();
	
	// 2015.07.25 by MYM : 장애 회피 기능 로그 기록
	private static final String DETOUR_TRACE = "DetourDebug";
	private static Logger detourTraceLog = Logger.getLogger(DETOUR_TRACE);
	public void traceDetour(String message) {
		detourTraceLog.debug(String.format("%s> %s", sectionId, message));
	}
	
	/**
	 * Constructor of Section class.
	 */
	public Section() {
		nodeList = new ArrayList<Node>();
		nodeIndexMap = new HashMap<Node, Integer>();// 2014.11.05 by MYM : SectionSearch
		
		// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
		this.detourSectionSet.add(this);
	}
	
	public double getTrafficPenalty() {
		return trafficPenalty;
	}

	public void setTrafficPenalty(double trafficPenalty) {
		this.trafficPenalty = trafficPenalty;
	}

	public double getMoveInSectionTime() {
		return moveInSectionTime;
	}

	public void setMoveInSectionTime(double moveInSectionTime) {
		this.moveInSectionTime = moveInSectionTime;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public String getSectionId() {
		return sectionId;
	}

	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isTwoWay() {
		return twoWay;
	}

	public void setTwoWay(boolean twoWay) {
		this.twoWay = twoWay;
	}

	public int getSectionSpeed() {
		return sectionSpeed;
	}

	public void setSectionSpeed(int sectionSpeed) {
		this.sectionSpeed = sectionSpeed;
	}

	public void setFirstNodeId(String fromNode) {
		this.firstNodeId = fromNode;
	}

	public String getFirstNodeId() {
		return firstNodeId;
	}

	public void setLastNodeId(String toNode) {
		this.lastNodeId = toNode;
	}

	public String getLastNodeId() {
		return lastNodeId;
	}
	
	public char getSteerPosition() {
		return steerPosition;
	}

	public void setSteerPosition(char steerPosition) {
		this.steerPosition = steerPosition;
	}
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리 
	 */
	public void addLink(Link link) {
		link.setSection(this);
		StringBuffer key = new StringBuffer();
		key.append(link.getFromNode()).append(UNDERBAR).append(link.getToNode());
		linkMap.put(key.toString(), link);
	}
	
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 */
	private void addAllLink(Map<? extends String, ? extends Link> m) {
		@SuppressWarnings("unchecked")
		Iterator<Link> iter = (Iterator<Link>) m.values().iterator();
		while (iter.hasNext()) {
			iter.next().setSection(this);
		}
		linkMap.putAll(m);
	}
	
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 */
	public void removeLink(Link link) {
		StringBuffer key = new StringBuffer();
		key.append(link.getFromNode()).append(UNDERBAR).append(link.getToNode());
		linkMap.remove(key.toString());
	}
	
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 */
	public boolean isLinkEnabled(Node fromNode, Node toNode) {
		StringBuffer key = new StringBuffer();
		key.append(fromNode).append(UNDERBAR).append(toNode);
		Link link = linkMap.get(key.toString());
		if (link != null) {
			return link.isEnabled();
		}
		return true;
	}
	
	/**
	 * 2015.03.10 by KYK
	 * @param fromNode
	 * @param toNode
	 * @return
	 */
	public Link getLink(Node fromNode, Node toNode) {
		StringBuffer key = new StringBuffer();
		key.append(fromNode).append(UNDERBAR).append(toNode);
		return linkMap.get(key.toString());
	}	

	/**
	 * 
	 * @param node
	 */
	public void addNode(Node node) {
		nodeList.add(node);
		nodeIndexMap.put(node, Integer.valueOf(nodeList.size() - 1)); // 2014.11.05 by MYM : SectionSearch		
		if (OcsConstant.CURVE.equals(type) == false) {
			int angle;
			if (nodeList.size() >= 2) {
				angle = (int) Math.toDegrees(Math.atan2(node.getTop() - getFirstNode().getTop(), node.getLeft() - getFirstNode().getLeft()));
				if (angle < 0)
					angle += 180;
				if (nodeList.size() == 2)
					getFirstNode().setAngle(angle);
				node.setAngle(angle);
			}
		}
	}
	
	public Node getFirstNode() {
		return nodeList.get(0);
	}
	
	public Node getLastNode() {
		return nodeList.get(nodeList.size()-1);
	}
	
	public ArrayList<Node> getNodeList() {
		return nodeList;
	}
	
	/**
	 * 
	 * @param section
	 * @return
	 */
	public Section mergeSection(Section section) {
		int i;
		Node node;
		
		// 2014.10.01 by MYM : 장애 지역 우회 기능 (주석처리, 분기/합류로만 구분)
//		if (type.equals(section.getType()) == false)
//			return null;

		node = getFirstNode();
		if (node == section.getLastNode()) {
			node.removeSection(this);
			if (node.getHid() != null) {
				node.getHid().removeSection(this);
			}
			for (i = 1; i < nodeList.size(); i++) {
				node = nodeList.get(i);
				node.removeSection(this);
				if (node.getHid() != null) {
					node.getHid().removeSection(this);
					node.getHid().addSection(section);
				}
				node.addSection(section);
				section.addNode(node);
				section.setLastNodeId(node.getNodeId());
			}
			// 2014.02.03 by MYM : Disabled Link 처리, 장애 지역 우회 기능
			section.addAllLink(linkMap);
			linkMap.clear();
			nodeList.clear();
			section.setDistance(section.getDistance() + this.distance);
			section.setMoveInSectionTime(section.getMoveInSectionTime() + this.moveInSectionTime);
			section.setTrafficPenalty(section.getTrafficPenalty() + this.trafficPenalty);
			section.setFirstNodeId(section.getFirstNode().getNodeId());
			return this;
		} else if (getLastNode() == section.getFirstNode()) {
			return section.mergeSection(this);
		}
		return null;
	}
	
	public int getNodeCount() { 
		return nodeList.size();
	}
	
	public Node getNode(int index) {
		if (index < 0 || index >= nodeList.size()) {
			return null;
		}
		return nodeList.get(index);
	}

	public int getNodeIndex(Node node) {
		// 2014.11.05 by MYM : SectionSearch
//		return nodeList.indexOf(node);
		Integer index = nodeIndexMap.get(node);
		if (index == null) {
			return -1;
		}
		return index.intValue();
	}
	
	/**
	 * 
	 */
	public String toString() {
		return this.sectionId;
	}
	
	/**
	 * 2015.01.05 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public String getSectionInfo() {
		StringBuffer sb = new StringBuffer();
		// 2014.02.03 by MYM : Disabled Link 처리 (로그 보완)
		sb.append("N:").append(nodeList.size()).append("[");
		for (int i=0; i < nodeList.size(); i++) {
			Node node = nodeList.get(i);		
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			// node의 toString이 호출되도록함. node가 null이면 null로 기록됨.
			if (i == 0) {
				sb.append(node);
			} else {
				sb.append(",").append(node);
			}			
		}
		
		sb.append("]	L:").append(linkMap.size()).append("[");
		int length = sb.length();
		for (Enumeration<Link> linkEnum = linkMap.elements(); linkEnum.hasMoreElements();) {
			Link link = linkEnum.nextElement();
			if (sb.length() == length) {
				sb.append(link.getLinkId());
			} else {
				sb.append(",").append(link.getLinkId());
			}
			if (link.getSection() == this) {
				sb.append("(true)");
			} else {
				sb.append("(false)");
			}
		}
		sb.append("]");
		
		return sb.toString();
	}
	
	public double getHeuristicCost(String vehicleId) {
		SectionArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			return OcsConstant.MAXCOST_TIMEBASE;
		}
		
		return arrivedTimeInfo.getHeuristicCost();
	}
	
	public int getIndex(String vehicleId) {
		SectionArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			return 999999;
		}
		return arrivedTimeInfo.getSectionIndex();
	}
	
	public SectionArrivedTimeInfo getArrivedTimeInfo(String vehicleId) {
		return arrivedTimeTable.get(vehicleId);
	}
	
	public void setArrivedTime(String vehicleId, double arrivedTime, double heuristicCost, double moveTime, long distance) {
		SectionArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			arrivedTimeInfo = new SectionArrivedTimeInfo();
			arrivedTimeTable.put(vehicleId, arrivedTimeInfo);
		}
		arrivedTimeInfo.setPrevSection(null);
		arrivedTimeInfo.setArrivedTime(arrivedTime);
		arrivedTimeInfo.setHeuristicCost(heuristicCost);
		arrivedTimeInfo.setMoveTime(moveTime);
		arrivedTimeInfo.setDistance(distance);
		arrivedTimeInfo.setSectionIndex(0);
	}
	
	public void removeArrivedTimeInfo(String vehicleId) {
		arrivedTimeTable.remove(vehicleId);		
	}
	
	public boolean setArrivedTime(String vehicleId, Section prevSection, double arrivedTime, double heuristicCost, double moveTime, long distance, int sectionIndex) {
		if (enabled == false)
			return false;
		
		SectionArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);
		if (arrivedTimeInfo == null) {
			arrivedTimeInfo = new SectionArrivedTimeInfo();
			arrivedTimeTable.put(vehicleId, arrivedTimeInfo);
		}
		
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (arrivedTime < arrivedTimeInfo.getArrivedTime()) {
			arrivedTimeInfo.setPrevSection(prevSection);
			arrivedTimeInfo.setArrivedTime(arrivedTime);
			arrivedTimeInfo.setHeuristicCost(heuristicCost);
			arrivedTimeInfo.setMoveTime(moveTime);
			arrivedTimeInfo.setDistance(distance);
			arrivedTimeInfo.setSectionIndex(sectionIndex);
			return true;
		}
		
		return false;
	}
	
	public double getArrivedTime(String vehicleId) {
		SectionArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);		
		if (arrivedTimeInfo == null)
			return OcsConstant.MAXCOST_TIMEBASE;
		
		return arrivedTimeInfo.getArrivedTime();
	}
	
	public Section getPrevSection(String vehicleId) {
		SectionArrivedTimeInfo arrivedTimeInfo = arrivedTimeTable.get(vehicleId);		
		if (arrivedTimeInfo == null)
			return null;
		
		return arrivedTimeInfo.getPrevSection();
	}
	
	//2011.12.27 by PMM
	public boolean contains(String nodeId) {
		Node node;
		ListIterator<Node> it = nodeList.listIterator();
		while (it.hasNext()) {
			node = it.next();
			if (node != null) {
				if (node.getNodeId().equals(nodeId)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 2014.10.10 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public double getDetourPenalty() {
		if (abnormalItemMap.size() > 0) {
			return detourPenalty;
		}
		return 0.0;
	}
	
	/**
	 * 2014.10.10 by MYM : 장애 지역 우회 기능
	 * @param prevSection
	 * @return
	 */
	public double getDetourPenalty(Node prevNode, Node prePrevNode) {
//		if (abnormalItemMap.size() > 0) {
		if (this.enabled == false) {
			// Abnormal Section 내에서의 경로 탐색은 가능하도록 함.
			// PrevSection(X) -> CurrSection(X) : OK
			// PrevSection(O) -> CurrSection(X) : NG
			Section prevSection = null;
			if (prePrevNode != null) {
				if (prePrevNode.getSectionCount() == 1) {
					// 이전 Node가 Section이 1개인 경우
					prevSection = prePrevNode.getSection(0);
				} else {
					// 이전 Node가 Section이 1개인 이상인 경우
					for (Section section : prePrevNode.getSection()) {
						if (prePrevNode.equals(section.getFirstNode())
								&& prevNode.equals(section.getLastNode())) {
							prevSection = section;
							break;
						}
					}
				}				
			} else {
				if (prevNode.isDiverge()) {
					// 분기 위치에서 경로 탐색시
					for (Section section : prevNode.getSection()) {
						if (prevNode.equals(section.getLastNode())) {
							prevSection = section;
							break;
						}
					}
				}
			}
			if (prevSection != null
					&& getFirstNode().equals(prevSection.getLastNode())
					&& prevSection.getEnabled()) {
				return detourPenalty;
			}
		}
		return 0.0;
	}
	
	/**
	 * 2014.10.10 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public String getAbnormalItemString() {
		StringBuffer reason = new StringBuffer();
		try {
			for (Object item : abnormalItemMap.keySet()) {
				reason.append(abnormalItemMap.get(item));
				reason.append("(").append(item).append(") ");
			}
		} catch (Exception e) {
		}
		return reason.toString();
	}
	
	/**
	 * 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
	 * 2014.10.10 by MYM : 장애 지역 우회 기능
	 * @param item
	 */
	synchronized public boolean addAbnormalItem(Object item, DETOUR_REASON reason) {
		detourStatusChangedTime = System.currentTimeMillis();
		
		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		if (ocsInfoManager == null || ocsInfoManager.isDetourControlUsed() == false) {
			return false;
		}
		DetourControlManager detourManager = DetourControlManager.getInstance(null, null, false, false, 0);
		if (detourManager == null || detourManager.isDetourUsed(item, reason) == false) {
			return false;
		}
		
		// Step1. Section Disable 
		this.setEnabled(false);
		double detourPenalty = detourManager.getDetourPenalty(reason);
		if (this.detourPenalty < detourPenalty) {
			this.setDetourPenalty(detourPenalty);
		}
		
		// Step2. 장애 Item(HID, Vehicle, Node, Link) 등록 
		abnormalItemMap.put(item, reason.toConstString());
		
		StringBuilder log = new StringBuilder();
		log.append("Set by ").append(item).append("(").append(reason.toConstString()).append(")");
		traceDetour(log.toString());

		// Step3. Section을 경유하는 Vehicle RepathSearch 요청
		checkRepathSearch();

		// Step4. DetourSection Disabled 요청
		detourManager.addUpdateSectionInfoList(this);
		
		// Step5. Section에 위치한 Port를 OutOfService 요청
		if (detourManager.isPortServiceUsed(item, reason)) {
			detourManager.addDetourPortService(sectionId, OcsConstant.PORT_OUTOFSERVICE);
		}
		
		return true;
	}
	
	/**
	 * 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
	 * 2014.10.10 by MYM : 장애 지역 우회 기능
	 * @param item
	 */
	synchronized public void removeAbnormalItem(Object item) {
		detourStatusChangedTime = System.currentTimeMillis();
		
		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		if (ocsInfoManager == null || ocsInfoManager.isDetourControlUsed() == false) {
			return;
		}
		
		DetourControlManager detourManager = DetourControlManager.getInstance(null, null, false, false, 0);
		DETOUR_REASON reason = DETOUR_REASON.toReasonType(abnormalItemMap.get(item));
		if (detourManager == null || detourManager.isDetourUsed(item, reason) == false) {
			return;
		}
		
		// 경로탐색 불가 → 가능 , OutOfService → InService 처리
		abnormalItemMap.remove(item);
		
		StringBuilder log = new StringBuilder();
		log.append("Released by ").append(item).append("(").append(reason).append(")");
		traceDetour(log.toString());

		if (abnormalItemMap.size() == 0) {
			this.setEnabled(true);
			this.setDetourPenalty(0);
			// 2015.09.02 by MYM : 장애 해제시 기능 off인 경우 미동작 조건 추가
//			if (detourManager != null) {
			if (detourManager != null && detourManager.isPortServiceUsed(item, reason)) {
				// Section에 위치한 Port의 Inservice 요청
				detourManager.addDetourPortService(sectionId, OcsConstant.PORT_INSERVICE);
			}
		} else if ((item instanceof Hid) == false && abnormalItemMap.size() == 1) {
			// 2015.03.12 by MYM : HID Capacity Full 중에 장애 발생 후 해제시 PortInService 보고 처리
			Iterator<Object> iter = abnormalItemMap.keySet().iterator();
			while (iter.hasNext()) {
				if (iter.next() instanceof Hid) {
					// 2015.09.02 by MYM : 장애 해제시 기능 off인 경우 미동작 조건 추가
//					if (detourManager != null) {
					if (detourManager != null && detourManager.isPortServiceUsed(item, reason)) {
						// Section에 위치한 Port의 Inservice 요청
						detourManager.addDetourPortService(sectionId, OcsConstant.PORT_INSERVICE);
					}
				}
			}
		}
		// DetourSection Enabled 요청
		if (detourManager != null) {
			detourManager.addUpdateSectionInfoList(this);
		}
	}
	
	/**
	 * 2015.03.03 by MYM : 장애 지역 우회 기능
	 * @param reason
	 */
	public void clearAllAbnormalItem(DETOUR_REASON reason) {
		if (reason == DETOUR_REASON.NONE) {
			clearAllAbnormalItem();
		} else {
			// 2015.03.12 by MYM : ConcurrentModificationException 발생 수정
//			Iterator<Object> iter = abnormalItemMap.keySet().iterator();
//			while (iter.hasNext()) {
//				Object item = iter.next();
//				DETOUR_REASON tmpReason = DETOUR_REASON.toReasonType(abnormalItemMap.get(item));
//				if (reason == tmpReason) {
//					removeAbnormalItem(item);
//				}
//			}
			@SuppressWarnings("unchecked")
			HashMap<Object, String> tmpAbnormalItemMap = (HashMap<Object, String>) abnormalItemMap.clone(); 
			Iterator<Object> iter = tmpAbnormalItemMap.keySet().iterator();
			while (iter.hasNext()) {
				Object item = iter.next();
				DETOUR_REASON tmpReason = DETOUR_REASON.toReasonType(tmpAbnormalItemMap.get(item));
				if (reason == tmpReason) {
					// 2015.11.10 by MYM : 각 항목별 기능 OFF시 동작해야 하나 removeAbnormalItem에서 Usage를 다시 보기 때문에 동작 안됨. 
//					removeAbnormalItem(item);
					clearAbnormalItem(item);
				}
			}
		}
	}
	
	/**
	 * 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
	 *                     다음 경우에 호출
	 *                      1) UserRequest 처리
	 *                      2) Detour 기능 Off시
	 * 2015.02.06 by MYM : 장애 지역 우회 기능
	 */
	synchronized public void clearAllAbnormalItem() {
		detourStatusChangedTime = System.currentTimeMillis();
		
		// 사용자 Enabled 요청 처리
		// 경로탐색 불가 → 가능 , OutOfService → InService 처리
		if (abnormalItemMap.size() > 0) {
			StringBuilder log = new StringBuilder();
			log.append("AllClear ").append(abnormalItemMap.toString());
			traceDetour(log.toString());
		}
		abnormalItemMap.clear();
		this.setEnabled(true);
		this.setDetourPenalty(0);

		DetourControlManager detourManager = DetourControlManager.getInstance(null, null, false, false, 0);
		if (detourManager != null) {
			// DetourSection Enabled 요청
			detourManager.addUpdateSectionInfoList(this);
			// Section에 위치한 Port의 Inservice 요청
			detourManager.addDetourPortService(sectionId, OcsConstant.PORT_INSERVICE);
		}
	}
	
	/**
	 * 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
	 *                     다음 경우에 호출 됨
	 *                      1) FailOver(Inservice → Outofservice)
	 *                      2) Mismatch 체크
	 *                      3) 항목별 기능 Off시
	 * 2015.02.06 by MYM : 장애 지역 우회 기능
	 * @param item
	 */
	synchronized public void clearAbnormalItem(Object item) {
		detourStatusChangedTime = System.currentTimeMillis();
		
		// Operation InServiceHost가 InService → OutOfService가 될 때 Clear 처리
		// 경로탐색 불가 → 가능 처리
		String reason = abnormalItemMap.remove(item);
		
		StringBuilder log = new StringBuilder();
		log.append("Clear by ").append(item).append("(").append(reason).append(")");
		traceDetour(log.toString());
		
		if (abnormalItemMap.size() == 0) {
			this.setEnabled(true);
			this.setDetourPenalty(0);
			
			DetourControlManager detourManager = DetourControlManager.getInstance(null, null, false, false, 0);
			if (detourManager != null) {
				detourManager.addUpdateSectionInfoList(this);
				// Section에 위치한 Port의 Inservice 요청
				detourManager.addDetourPortService(sectionId, OcsConstant.PORT_INSERVICE);
			}
		}
	}
	
	/**
	 * 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
	 * 2015.02.10 by MYM : 장애 지역 우회 기능
	 * @param item
	 */
	synchronized public void clearPortService(Object item) {
		// OutOfService → InService 처리
		DetourControlManager detourManager = DetourControlManager.getInstance(null, null, false, false, 0);
		if (detourManager != null) {
			// Section에 위치한 Port의 Inservice 요청
			detourManager.addDetourPortService(sectionId, OcsConstant.PORT_INSERVICE);
		}
	}
	
	/**
	 * 2015.03.03 by MYM : 장애 지역 우회 기능
	 * @param reason
	 */
	public void clearPortService(DETOUR_REASON reason) {
		Iterator<Object> iter = abnormalItemMap.keySet().iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			DETOUR_REASON tmpReason = DETOUR_REASON.toReasonType(abnormalItemMap.get(item));
			if (reason == tmpReason) {
				clearPortService(item);
			}
		}
	}
	
	/**
	 * 2015.11.10 by MYM : 모든 진입 Section 설정
	 * @param detourSectionSet
	 */
	public void addDetourSectionSet(HashSet<Section> detourSectionSet) {
		this.detourSectionSet.addAll(detourSectionSet);
	}
	
	/**
	 * 2015.11.10 by MYM : 모든 진입 Section 설정
	 * @return
	 */
	public HashSet<Section> getDetourSectionSet() {
		if (detourUserSectionSet.size() > 0) {
			HashSet<Section> detourAllSectionSet = new HashSet<Section>(this.detourSectionSet); 
			detourAllSectionSet.addAll(detourUserSectionSet);
			return detourAllSectionSet;
		}
		return this.detourSectionSet;
	}
	
	/**
	 * 2015.11.10 by MYM : detourUser Section 설정 변경
	 * @param detourUserSectionSet
	 */
	public void addDetourUserSectionSet(HashSet<Section> detourUserSectionSet) {
		for (Section section : detourUserSectionSet) {
			if (!detourSectionSet.contains(section)) {
				this.detourUserSectionSet.add(section);
			}
		}
	}
	
	/**
	 * 2015.11.10 by MYM : detourUser Section 설정 변경
	 */
	public void clearDetourUserSectionSet() {
		this.detourUserSectionSet.clear();
	}

	/**
	 * 2014.10.10 by MYM : 장애 지역 우회 기능
	 */
	public void checkRepathSearch() {
		if (nodeList.size() == 2) {
			Node fromNode = nodeList.get(0);
			Node toNode = nodeList.get(1);
			toNode.checkRepathSearch(fromNode);
		} else if (nodeList.size() > 2) {
			Node node = nodeList.get(1);
			node.checkRepathSearch();
		}
	}
	
	/**
	 * 2015.02.03 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public ConcurrentHashMap<String, Link> getLinkMap() {
		return linkMap;
	}

	/**
	 * 2015.02.03 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public HashMap<Object, String> getAbnormalItemMap() {
		return abnormalItemMap;
	}
	
	/**
	 * 2015.02.15 by MYM : 장애 지역 우회 기능
	 * @param item
	 * @return
	 */
	public DETOUR_REASON getDetourReason(Object item) {
		return DETOUR_REASON.toReasonType(this.abnormalItemMap.get(item));
	}

	/**
	 * 2015.02.15 by MYM : 장애 지역 우회 기능
	 * @param detourPenalty
	 */
	public void setDetourPenalty(double detourPenalty) {
		this.detourPenalty = detourPenalty;
	}
	
	/**
	 * 2015.02.15 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public String getDetourReason() {
		String detourReason = "";
		try {
			HashSet<String> detourReasonSet = new HashSet<String>(); 
			detourReasonSet.addAll(abnormalItemMap.values());
			detourReason = detourReasonSet.toString().replace("[", "").replace("]", "").replaceAll("\\p{Z}", "");
		} catch (Exception e) {
		}
		return detourReason;
	}

	/**
	 * 2015.05.27 by MYM : Vehicle Traffic 분산
	 * @param traffic
	 */
	public void setTraffic(Traffic traffic) {
		this.traffic = traffic;
	}

	/**
	 * 2015.05.27 by MYM : Vehicle Traffic 분산
	 * @return
	 */
	public Traffic getTraffic() {
		return traffic;
	}
	
	/**
	 * 2015.08.08 by MYM : 장애 회피 오류 자동 체크
	 * @return
	 */
	public void checkDetourMismatch() {
		if (Math.abs(System.currentTimeMillis() - detourStatusChangedTime) > 5000) {
			checkAnomralItemMismatch();
		}
	}
	
	/**
	 * 2015.08.08 by MYM : 장애 회피 오류 자동 체크
	 * 
	 * 타이밍상 장애 발생 후 바로 해제되었지만 Section이 Disabled로 유지되는 경우 자동으로 Enabled 처리
	 */
	synchronized private void checkAnomralItemMismatch() {
		boolean isRemoved = false;
		@SuppressWarnings("unchecked")
		HashMap<Object, String> tmpAbnormalItemMap = (HashMap<Object, String>) abnormalItemMap.clone(); 
		Iterator<Object> iter = tmpAbnormalItemMap.keySet().iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			DETOUR_REASON reason = DETOUR_REASON.toReasonType(tmpAbnormalItemMap.get(item));
			if (reason == DETOUR_REASON.NODE_DISABLED) {
				Node node = (Node) item;
				if (node.isEnabled() == true) {
					abnormalItemMap.remove(item);
					isRemoved = true;
				}
			} else if (reason == DETOUR_REASON.LINK_DISABLED) {
				Link link = (Link) item;
				if (link.isEnabled() == true) {
					abnormalItemMap.remove(item);
					isRemoved = true;
				}
			} else if (reason == DETOUR_REASON.VEHICLE_MANUAL
					|| reason == DETOUR_REASON.VEHICLE_ERROR
					|| reason == DETOUR_REASON.VEHICLE_COMMFAIL
					|| reason == DETOUR_REASON.VEHICLE_NOTRESPOND) {
				boolean reallyAbnormalVehicle = false;
				VehicleData vehicle = (VehicleData) item;
				if (vehicle.getVehicleMode() == 'M') {
					reallyAbnormalVehicle = true;
				}
				if (vehicle.getState() == 'E') {
					reallyAbnormalVehicle = true;
				}
				if (vehicle.getErrorCode() == OcsConstant.COMMUNICATION_FAIL) {
					reallyAbnormalVehicle = true;
				}
				if (vehicle.getAlarmCode() == OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS
						|| vehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER
						|| vehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER
						|| vehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
					reallyAbnormalVehicle = true;
				}
				if (reallyAbnormalVehicle == false || vehicle.isEnabled() == false) {
					abnormalItemMap.remove(item);
					vehicle.clearAbnormalSectionSet(true);
					isRemoved = true;
				}
			} else if (reason == DETOUR_REASON.HID_DOWN
					|| reason == DETOUR_REASON.HID_CAPACITY_FULL) {
				boolean reallyAbnormalHid = false;
				Hid hid = (Hid) item;
				// 2015.09.02 by MYM : hid 장애 해제 자동 체크 조건 오류 수정
				if (hid.isAbnormalState()) {
					reallyAbnormalHid = true;
				}
				if (hid.isHidCapacityReleased() == false) {
					reallyAbnormalHid = true;
				}
				if (reallyAbnormalHid == false) {
					abnormalItemMap.remove(item);
					hid.releaseAbnormalSection(true);
					isRemoved = true;
				}
			} else {
				abnormalItemMap.remove(item);
				isRemoved = true;
			}
			
			if (isRemoved == true) {
				StringBuilder log = new StringBuilder();
				log.append("Auto Check Released by ").append(item).append("(").append(reason).append(")");
				traceDetour(log.toString());
			}
		}

		DetourControlManager detourManager = DetourControlManager.getInstance(null, null, false, false, 0);
		if (isRemoved == true && abnormalItemMap.size() == 0) {
			this.setEnabled(true);
			this.setDetourPenalty(0);
			if (detourManager != null) {
				// Section에 위치한 Port의 Inservice 요청
				detourManager.addDetourPortService(sectionId, OcsConstant.PORT_INSERVICE);
				detourManager.addUpdateSectionInfoList(this);
			}
			traceDetour("Auto Check Released(Disabled → Enabled)");
		}

		detourStatusChangedTime = System.currentTimeMillis();
	}
}
