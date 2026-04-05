package com.samsung.ocs.unitdevice.model;

public class HID {
	private String hid = "";
	private String ipAddress = "127.0.0.1";
	private String enabled = "TRUE";
	private String status = "R";
	private String voltage = "";
	private String electricCurrent = "";
	private String temperature = "";
	private String frequency = "";
	private int errorCode = 0;
	private String remoteCmd = "";
	// 2011.02.28 by LWG [Backup HID ฐüทร]
	private String backupHID = "";
	
	public HID(String hid) {
		this.hid = hid;
	}
	
	public String getEnabled() {
		return enabled;
	}

	public String getStatus() {
		return status;
	}

	public String getVoltage() {
		return voltage;
	}

	public void setVoltage(String voltage) {
		this.voltage = voltage;
	}

	public String getElectricCurrent() {
		return electricCurrent;
	}

	public void setElectricCurrent(String electricCurrent) {
		this.electricCurrent = electricCurrent;
	}

	public String getTemperature() {
		return temperature;
	}

	public void setTemperature(String temperature) {
		this.temperature = temperature;
	}

	public String getFrequency() {
		return frequency;
	}

	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public String getHid() {
		return hid;
	}

	public String getRemoteCmd() {
		return remoteCmd;
	}

	public void setHid(String hid, String enabled, String status) {
		this.hid = hid;
		this.enabled = enabled;
		this.status = status;
	}

	public void setEnabled(String enabled) {
		this.enabled = enabled;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setErrorCode(String errorCode) {
		try {
			this.errorCode = Integer.parseInt(errorCode);
		} catch (Exception e) {
		}
	}

	public void setDetailStatus(String voltage, String electricCurrent,
			String temperature, String frequency) {
		this.voltage = voltage;
		this.electricCurrent = electricCurrent;
		this.temperature = temperature;
		this.frequency = frequency;
	}

	public void setRemoteCmd(String remoteCmd) {
		this.remoteCmd = remoteCmd;
	}

	public String getBackupHID() {
		return backupHID;
	}

	public void setBackupHID(String backupHID) {
		this.backupHID = backupHID;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getIpAddress() {
		return ipAddress;
	}

}
