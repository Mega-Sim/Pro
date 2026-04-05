package com.samsung.ocs.manager.impl.model;

/**
 * ZoneControl Class, OCS 3.1 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2012. 5. 24.
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

public class ZoneControl {
	protected String vehicleZone;
	protected String nodeZone;
	protected boolean isAssignAllowed;
	protected boolean isDriveAllowed;
	protected boolean isComebackZoneAllowed;
	protected double penalty;
	
	public String getVehicleZone() {
		return vehicleZone;
	}
	public void setVehicleZone(String vehicleZone) {
		this.vehicleZone = vehicleZone;
	}
	public String getNodeZone() {
		return nodeZone;
	}
	public void setNodeZone(String nodeZone) {
		this.nodeZone = nodeZone;
	}
	public boolean isAssignAllowed() {
		return isAssignAllowed;
	}
	public void setAssignAllowed(boolean isAssignAllowed) {
		this.isAssignAllowed = isAssignAllowed;
	}
	public boolean isDriveAllowed() {
		return isDriveAllowed;
	}
	public void setDriveAllowed(boolean isDriveAllowed) {
		this.isDriveAllowed = isDriveAllowed;
	}
	public boolean isComebackZoneAllowed() {
		return isComebackZoneAllowed;
	}
	public void setComebackZoneAllowed(boolean isComebackZoneAllowed) {
		this.isComebackZoneAllowed = isComebackZoneAllowed;
	}
	public double getPenalty() {
		return penalty;
	}
	public void setPenalty(double penalty) {
		this.penalty = penalty;
	}
}