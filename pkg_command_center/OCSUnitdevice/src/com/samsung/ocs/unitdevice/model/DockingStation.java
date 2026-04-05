package com.samsung.ocs.unitdevice.model;

public class DockingStation {
	private String dockingStationID = "";
	private String ipAddress = "127.0.0.1";
	private String enabled = "TRUE";
	private String stationMode = "A";
	private int carrierExist = 0;
	private int carrierCharge = 0;
	private int alarmID = 0;
	private String carrierID = "";
	
	public DockingStation(String dockingStationID) {
		this.dockingStationID = dockingStationID;
	}
	public String getDockingStationID() {
		return dockingStationID;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public String getEnabled() {
		return enabled;
	}
	public String getStationMode() {
		return stationMode;
	}
	public int getCarrierExist() {
		return carrierExist;
	}
	public int getCarrierCharged() {
		return carrierCharge;
	}
	public int getAlarmID() {
		return alarmID;
	}
	public String getCarrierID() {
		return carrierID;
	}
	public void setDockingStation(String dockingStationID, String stationMode, int carrierExist, int carrierCharge, int alarmID, String carrierID) {
		this.dockingStationID = dockingStationID;
		this.stationMode = stationMode;
		this.carrierExist = carrierExist;
		this.carrierCharge = carrierCharge;
		this.alarmID = alarmID;
		this.carrierID = carrierID;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public void setEnabled(String enabled) {
		this.enabled = enabled;
	}
	public void setStationMode(String mode) {
		this.stationMode = mode;
	}
	public void setCarrierExist(int carrierExist) {
		this.carrierExist = carrierExist;
	}
	public void setCarrierCharge(int carrierCharge) {
		this.carrierCharge = carrierCharge;
	}
	public void setAlarmID(int alarmID) {
		this.alarmID = alarmID;		
	}
	public void setCarrierID(String carrierID) {
		this.carrierID = carrierID;
	}
	public String toString() {
		return "[" + dockingStationID + "," + ipAddress + "," + enabled + "," + stationMode + "," + carrierExist + "," + carrierCharge + "," + alarmID + "," + carrierID + "]";
	}
}

