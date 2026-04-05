package com.samsung.ocs.manager.impl.model;

/**
 * TrafficInfo Class, OCS 3.0 for Unified FAB
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

public class TrafficInfo {
	protected String name;
	protected String trafficNode;
	protected String fromNode;
	protected boolean enabled;
	protected String controlType;
	protected String trafficLight;
	protected String waitLimitTime;
	protected String releaseLimitTime;
	protected String waitingVehicle;
	protected String arrivedTime;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTrafficNode() {
		return trafficNode;
	}
	public void setTrafficNode(String trafficNode) {
		this.trafficNode = trafficNode;
	}
	public String getFromNode() {
		return fromNode;
	}
	public void setFromNode(String fromNode) {
		this.fromNode = fromNode;
	}
	public boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getControlType() {
		return controlType;
	}
	public void setControlType(String controlType) {
		this.controlType = controlType;
	}
	public String getTrafficLight() {
		return trafficLight;
	}
	public void setTrafficLight(String trafficLight) {
		this.trafficLight = trafficLight;
	}
	public String getWaitLimitTime() {
		return waitLimitTime;
	}
	public void setWaitLimitTime(String waitLimitTime) {
		this.waitLimitTime = waitLimitTime;
	}
	public String getReleaseLimitTime() {
		return releaseLimitTime;
	}
	public void setReleaseLimitTime(String releaseLimitTime) {
		this.releaseLimitTime = releaseLimitTime;
	}
	public String getWaitingVehicle() {
		return waitingVehicle;
	}
	public void setWaitingVehicle(String waitingVehicle) {
		this.waitingVehicle = waitingVehicle;
	}
	public String getArrivedTime() {
		return arrivedTime;
	}
	public void setArrivedTime(String arrivedTime) {
		this.arrivedTime = arrivedTime;
	}
}
