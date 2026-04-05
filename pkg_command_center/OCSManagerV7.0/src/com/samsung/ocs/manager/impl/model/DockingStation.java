package com.samsung.ocs.manager.impl.model;

/**
 * DockingStation Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 4. 8.
 * @version 3.0
 * 
 * Copyright 2013 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class DockingStation {
	protected String dockingStationId = "";
	protected boolean isEnabled = true;
	protected String iPAddress = "";
	protected char stationMode;
	protected int carrierExist = 0;
	protected int carrierCharged = 0;
	protected int alarmId = 0;
	protected String carrierId = "";
	public String getDockingStationId() {
		return dockingStationId;
	}
	public void setDockingStationId(String dockingStationId) {
		this.dockingStationId = dockingStationId;
	}
	public boolean isEnabled() {
		return isEnabled;
	}
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	public String getIPAddress() {
		return iPAddress;
	}
	public void setIPAddress(String iPAddress) {
		this.iPAddress = iPAddress;
	}
	public char getStationMode() {
		return stationMode;
	}
	public void setStationMode(char stationMode) {
		this.stationMode = stationMode;
	}
	public int getCarrierExist() {
		return carrierExist;
	}
	public void setCarrierExist(int carrierExist) {
		this.carrierExist = carrierExist;
	}
	public int getCarrierCharged() {
		return carrierCharged;
	}
	public void setCarrierCharged(int carrierCharged) {
		this.carrierCharged = carrierCharged;
	}
	public int getAlarmId() {
		return alarmId;
	}
	public void setAlarmId(int alarmId) {
		this.alarmId = alarmId;
	}
	public String getCarrierId() {
		return carrierId;
	}
	public void setCarrierId(String carrierId) {
		this.carrierId = carrierId;
	}
}
