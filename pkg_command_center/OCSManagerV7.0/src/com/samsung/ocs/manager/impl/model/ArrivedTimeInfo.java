package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.OcsConstant;

/**
 * ArrivedTimeInfo Class, OCS 3.0 for Unified FAB
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

public class ArrivedTimeInfo {
	private double arrivedTime = OcsConstant.MAXCOST_TIMEBASE;
	private double moveTime = OcsConstant.MAXCOST_TIMEBASE;
	private long distance = OcsConstant.MAXCOST_DISTANCEBASE;
	private double heuristicCost = 0;
	private int sectionIndex = 0;

	public double getArrivedTime() {
		return arrivedTime;
	}

	public void setArrivedTime(double arrivedTime) {
		this.arrivedTime = arrivedTime;
	}
	
	public double getHeuristicCost() {
		return heuristicCost;
	}

	public void setHeuristicCost(double heuristicCost) {
		this.heuristicCost = heuristicCost;
	}
	
	public double getMoveTime() {
		return moveTime;
	}
	
	public void setMoveTime(double moveTime) {
		this.moveTime = moveTime;
	}
	
	public long getDistance() {
		return distance;
	}
	
	public void setDistance(long distance) {
		this.distance = distance;
	}
	
	public int getSectionIndex() {
		return sectionIndex;
	}

	public void setSectionIndex(int sectionIndex) {
		this.sectionIndex = sectionIndex;
	}
}
