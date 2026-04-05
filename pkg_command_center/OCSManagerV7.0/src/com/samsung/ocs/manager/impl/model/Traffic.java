package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsInfoConstant.TRAFFIC_UPDATE_RULE;

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
public class Traffic {
	private String trafficId;
	private boolean enabled = false;
	TRAFFIC_UPDATE_RULE type;
	private ArrayList<Section> sectionList = new ArrayList<Section>();
	private ArrayList<Section> prevSectionList = new ArrayList<Section>();
	
	// 실시간 변경 및 DB 업데이트 항목
	private int currentVehicleCount;
	private int expectedVehicleCount;
	private double pullTrafficRatio = 1;	// PULL 적용 값
	private double pushTrafficCost = 0;		// PUSH 적용 값
	
	private int minPriority = 0;
	private int maxPriority = 80;
	
	// PUSH, PULL 공통 설정
	private double secondTrafficCostRatio = 0.5;
	
	// PULL 관련 설정
	private int pullGreenLimit = 2;
	private int pullBlueLimit = 1;
	private double pullGreenRatio = 0.95;
	private double pullBlueRatio = 0.8;
	
	// PUSH 관련 설정
	private double pushTrafficWeight = 1;
	private int pushYellowLimit = 5;
	private int pushRedLimit = 7;
	
	private static final String TRAFFIC_TRACE = "TrafficDebug";
	private static Logger trafficTraceLog = Logger.getLogger(TRAFFIC_TRACE);
	public void traceTraffic(String message) {
		trafficTraceLog.debug(String.format("%s> %s", trafficId, message));
	}
	
	public String getTrafficId() {
		return trafficId;
	}
	public void setTrafficId(String trafficId) {
		this.trafficId = trafficId;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public TRAFFIC_UPDATE_RULE getType() {
		return type;
	}
	public void setType(TRAFFIC_UPDATE_RULE rule) {
		try {
			type = rule;
		} catch (Exception e) {
			type = TRAFFIC_UPDATE_RULE.HYBRID;
		}
	}
	public ArrayList<Section> getSectionList() {
		return sectionList;
	}
	public void setPushTrafficWeight(double trafficWeight) {
		this.pushTrafficWeight = trafficWeight;
	}
	public void setSecondTrafficCostRatio(double secondTrafficCostRatio) {
		this.secondTrafficCostRatio = secondTrafficCostRatio;
	}
	public double getPushTrafficCost() {
		return pushTrafficCost;
	}
	public double getPullTrafficRatio() {
		return pullTrafficRatio;
	}
	public int getCurrentVehicleCount() {
		return currentVehicleCount;
	}
	public int getExpectedVehicleCount() {
		return expectedVehicleCount;
	}
	public void setPullGreenLimit(int pullGreenLimit) {
		this.pullGreenLimit = pullGreenLimit;
	}
	public void setPullBlueLimit(int pullBlueLimit) {
		this.pullBlueLimit = pullBlueLimit;
	}
	public void setPullGreenRatio(double pullGreenRatio) {
		this.pullGreenRatio = pullGreenRatio;
	}
	public void setPullBlueRatio(double pullBlueRatio) {
		this.pullBlueRatio = pullBlueRatio;
	}
	public void setPushYellowLimit(int pushYellowLimit) {
		this.pushYellowLimit = pushYellowLimit;
	}
	public void setPushRedLimit(int pushRedLimit) {
		this.pushRedLimit = pushRedLimit;
	}
	public void setSection(ArrayList<Section> addSectionList) {
		sectionList.addAll(addSectionList);
		for (Section section : sectionList) {
			section.setTraffic(this);
			
			if (type == TRAFFIC_UPDATE_RULE.PUSH) {
				Node firstNode = section.getFirstNode();
				ArrayList<Section> tmpPrevSectionSet = firstNode.getSection();
				for (Section prevSetion : tmpPrevSectionSet) {
					if (firstNode == prevSetion.getLastNode()) {
						if (sectionList.contains(prevSetion) == false) {
							prevSetion.setTraffic(this);
							prevSectionList.add(prevSetion);
						}
					}
				}
			}
		}
	}
	public void clear() {
		resetTrafficCost();
		
		for (Section section : sectionList) {
			section.setTraffic(null);
		}
		for (Section section : prevSectionList) {
			section.setTraffic(null);
		}
		sectionList.clear();
		prevSectionList.clear();
	}
	public void resetTrafficCost() {
		pushTrafficCost = 0;
		pullTrafficRatio = 1;
		currentVehicleCount = 0;
		expectedVehicleCount = 0;
	}
	
	public double getPushTrafficCost(Section section) {
		if (prevSectionList.contains(section)) {
			return pushTrafficCost * secondTrafficCostRatio;
		}
		return pushTrafficCost;
	}
	
	public double getPushTrafficCost(Section section, int priority) {
		// 2016.02.20 by MYM : Dynamic Traffic 반영시 반송 Priority 고려(Min <= priority <= Max 인 반송만 Traffic Cost 반영)
		if (priority >= minPriority && priority <= maxPriority) {
			return getPushTrafficCost(section);
		}
		return 0.0;
	}
	
	public void setMinPriority(int minPriority) {
		this.minPriority = minPriority;
	}

	public void setMaxPriority(int maxPriority) {
		this.maxPriority = maxPriority;
	}

	@SuppressWarnings("unchecked")
	public void updateTrafficCost() throws Exception {
		if (type != TRAFFIC_UPDATE_RULE.PULL
				&& type != TRAFFIC_UPDATE_RULE.PUSH) {
			resetTrafficCost();
			return;
		}
				
		double jamScore = 0;
		HashSet<VehicleData> currentVehicleSet = new HashSet<VehicleData>();
		HashSet<VehicleData> expectedVehicleSet = new HashSet<VehicleData>();

		for (Section section : sectionList) {
			ArrayList<Node> nodeList = section.getNodeList();
			for (Node node : nodeList) {
				currentVehicleSet.addAll((ArrayList<VehicleData>) node.getDriveVehicleListClone());
				ArrayList<VehicleData> routedVehicleListClone = (ArrayList<VehicleData>) node.getRoutedVehicleListClone();
				for (VehicleData vehicle : routedVehicleListClone) {
					if (currentVehicleSet.contains(vehicle) == false) {
						expectedVehicleSet.add(vehicle);
					}
				}
			}
		}
		currentVehicleCount = currentVehicleSet.size();
		expectedVehicleCount = expectedVehicleSet.size();
		
		StringBuilder log = new StringBuilder();
		log.append(type.toConstString());
		
		if (type == TRAFFIC_UPDATE_RULE.PULL) {
			jamScore = currentVehicleCount + (expectedVehicleCount * 2);
			if (pullBlueLimit > jamScore) {
				pullTrafficRatio = pullBlueRatio;
			} else if (pullGreenLimit > jamScore) {
				pullTrafficRatio = pullGreenRatio;
			} else {
				pullTrafficRatio = 1;
			}
			if (pullTrafficRatio > 1) {
				pullTrafficRatio = 1;
			}
			pushTrafficCost = 0;
			log.append(" Ratio:").append(pullTrafficRatio);
		} else if(type == TRAFFIC_UPDATE_RULE.PUSH) {
			jamScore = (2 * currentVehicleCount) + expectedVehicleCount;
			if ((sectionList.size() * pushRedLimit) < jamScore) {
				pushTrafficCost = 2 * pushTrafficWeight;
			} else if ((sectionList.size() * pushYellowLimit) < jamScore) {
				pushTrafficCost = 1 * pushTrafficWeight;
			} else {
				pushTrafficCost = 0;
			}
			pullTrafficRatio = 1;
			log.append(" Cost:").append(pushTrafficCost);
		} else {
			pushTrafficCost = 0;
			pullTrafficRatio = 1;
		}
		
		currentVehicleSet.clear();
		expectedVehicleSet.clear();
		
		log.append(", Curr:").append(currentVehicleCount);
		log.append(", Exp:").append(expectedVehicleCount);
		traceTraffic(log.toString());
	}
}
