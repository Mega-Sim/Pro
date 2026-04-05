package com.samsung.ocs.manager.impl.model;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RailDownControl Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2012. 5. 24.
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

public class RailDownControl {
	private static String FALSE = "FALSE";
	private String area;
	private String direction;
	private ConcurrentHashMap<String, String> nodeList = new ConcurrentHashMap<String, String>();
	
	public RailDownControl() {
	}
	
	public String getArea() {
		return area;
	}
	
	public void setArea(String area) {
		this.area = area;
	}
	
	public String getDirection() {
		return direction;
	}
	
	public void setDirection(String direction) {
		this.direction = direction;
	}
	
	public void addNode(String nodeId, String enabled) {
		nodeList.put(nodeId, enabled);
	}
	
	public void removeNode(String nodeId) {
		nodeList.remove(nodeId);
	}
	
	public int size() {
		return nodeList.size();
	}
	
	public boolean isRailAvailable() {
		for (Enumeration<String> e = nodeList.elements(); e.hasMoreElements();) {
			String enabled = e.nextElement();
			if (FALSE.equals(enabled)) {
				return false;
			}
		}
		
		return true;
	}
}
