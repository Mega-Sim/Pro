package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.OcsConstant.DETOUR_CONTROL_LEVEL;

/**
 * UnitDevice Class, OCS 3.0 for Unified FAB
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

public class UnitDevice {
	protected String unitId;
	protected String type;
	protected String ipAddress;
	protected String state;
	protected double voltage;
	protected double electricCurrent;
	protected double temperature;
	protected double frequency;
	protected int errorcode;
	protected long left;
	protected long top;
	protected long width;
	protected long height;
	protected String remoteCmd;
	protected String altHidName;
	protected String backupHidName;
	protected DETOUR_CONTROL_LEVEL detourControlLevel;
	
	public String getUnitId() {
		return unitId;
	}
	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public String getState() {
		return state;
	}
	public void setState(String status) {
		this.state = status;
	}
	public double getVoltage() {
		return voltage;
	}
	public void setVoltage(double voltage) {
		this.voltage = voltage;
	}
	public double getElectricCurrent() {
		return electricCurrent;
	}
	public void setElectricCurrent(double electricCurrent) {
		this.electricCurrent = electricCurrent;
	}
	public double getTemperature() {
		return temperature;
	}
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
	public double getFrequency() {
		return frequency;
	}
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	public int getErrorcode() {
		return errorcode;
	}
	public void setErrorcode(int errorcode) {
		this.errorcode = errorcode;
	}
	public long getLeft() {
		return left;
	}
	public void setLeft(long left) {
		this.left = left;
	}
	public long getTop() {
		return top;
	}
	public void setTop(long top) {
		this.top = top;
	}
	public long getWidth() {
		return width;
	}
	public void setWidth(long width) {
		this.width = width;
	}
	public long getHeight() {
		return height;
	}
	public void setHeight(long height) {
		this.height = height;
	}
	public String getRemoteCmd() {
		return remoteCmd;
	}
	public void setRemoteCmd(String remoteCmd) {
		this.remoteCmd = remoteCmd;
	}
	public String getAltHidName() {
		return altHidName;
	}
	public void setAltHidName(String altHidName) {
		this.altHidName = altHidName;
	}
	public String getBackupHidName() {
		return backupHidName;
	}
	public void setBackupHidName(String backupHidName) {
		this.backupHidName = backupHidName;
	}
	public DETOUR_CONTROL_LEVEL getDetourControlLevel() {
		return detourControlLevel;
	}
	public void setDetourControlLevel(DETOUR_CONTROL_LEVEL detourControlLevel) {
		this.detourControlLevel = detourControlLevel;
	}
}
