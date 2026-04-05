package com.samsung.ocs.unitdevice.model;

public class NetworkDevice {

	private String unitId;
	private String ipAddress;
	private boolean enabled;
	private String status;
	private String type;
	
	public NetworkDevice(String unitId, String ipAddress, boolean enabled, String status , String type ) {
		this.unitId = unitId;
		this.ipAddress = ipAddress;
		this.enabled = enabled;
		this.status = status;
		this.type = type;
	}
	
	public NetworkDevice(String unitId) {
		this.unitId = unitId;
	}
	
	public void setDetailInfo(String ipAddress, boolean enabled, String status , String type ) {
		this.ipAddress = ipAddress;
		this.enabled = enabled;
		this.status = status;
		this.type = type;
	}
	
	public String getUnitId() {
		return unitId;
	}
	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	
	
}
