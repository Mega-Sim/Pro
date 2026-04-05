package com.samsung.ocs.unitdevice.model;

/**
 * PassDoor Model Class
 * 
 * @author zzang9un
 */
public class PassDoor {
	private String passDoorId = "";
	private String nodeId = "";
	private String ipAddress = "127.0.0.1";
	private String enabled = "FALSE";
	private String mode = "M";
	private String status = "I";
	private String sensorData = "";
	private int errorCode = 0;
	private String pioData = "";
	
	private final static String PASSDOOR_ENABLED = "TRUE";
	private final static String PASSDOOR_DISABLED = "FALSE";
	private final static String PASSDOOR_STATUS_IDLE = "I";
	private final static String PASSDOOR_STATUS_BUSY = "B";
	private final static String PASSDOOR_STATUS_ERROR = "E";

	public PassDoor(String passDoorId) {
		setPassDoorId(passDoorId);
	}

	/**
	 * PassDoor 설정 함수
	 * @author zzang9un
	 * @date 2015. 1. 17.
	 * @param passDoorID
	 * @param nodeId
	 * @param ipAddress
	 * @param enabled
	 */
	public void setPassDoor(String passDoorID, String nodeId, String ipAddress, String enabled) {
		this.passDoorId = passDoorID;
		this.nodeId = nodeId;
		this.ipAddress = ipAddress;
		this.enabled = enabled;
	}

	public String getPassDoorId() {
		return passDoorId;
	}

	public void setPassDoorId(String passDoorId) {
		this.passDoorId = passDoorId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getEnabled() {
		return enabled;
	}

	public void setEnabled(String enabled) {
		this.enabled = enabled;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		if (status.equals("1"))
			this.status = PASSDOOR_STATUS_IDLE;
		else if (status.equals("2"))
			this.status = PASSDOOR_STATUS_BUSY;
		else if (status.equals("3"))
			this.status = PASSDOOR_STATUS_ERROR;
	}

	public String getSensorData() {
		return sensorData;
	}

	public void setSensorData(String sensorData) {
		this.sensorData = sensorData;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = Integer.parseInt(errorCode);
	}

	public String getPioData() {
		return pioData;
	}

	public void setPioData(String pioData) {
		this.pioData = pioData;
	}
	
	public boolean isEnabled() {
		if (PASSDOOR_ENABLED.equalsIgnoreCase(getEnabled()))
			return true;
		else
			return false;
	}	
}
