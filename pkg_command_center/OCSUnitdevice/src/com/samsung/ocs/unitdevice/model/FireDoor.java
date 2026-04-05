package com.samsung.ocs.unitdevice.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class FireDoor {
	private String fireDoorId = "";
	private String enabled = "FALSE";
	private String ipAddress = "127.0.0.1";
	private String status = "R";
	private List<String> nodeList = new Vector<String>();
	private int errorCode = 0;
	
	private String prevStatus = "R";
	private int prevErrorCode = 0;
	private long lLastReportedTime = 0;
	
	private final static String FIREDOOR_ENABLED = "TRUE";
//	private final static String FIREDOOR_DISABLED = "FALSE";
	private final static String FIREDOOR_STATUS_RUN = "RUN";
	private final static String FIREDOOR_STATUS_STOP = "STOP";
	private final static String FIREDOOR_STATUS_WARNING = "WARNING";

	public FireDoor(String fireDoorId) {
		setFireDoorId(fireDoorId);
	}

	public void setFireDoor(String ffuGroupId, String ipAddress, String enabled) {
		this.fireDoorId = ffuGroupId;
		this.ipAddress = ipAddress;
		this.enabled = enabled;
	}

	public String getFireDoorId() {
		return fireDoorId;
	}

	public void setFireDoorId(String fireDoorId) {
		this.fireDoorId = fireDoorId;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		if ("RUN".equals(status) || FIREDOOR_STATUS_RUN.equals(status))
			this.status = FIREDOOR_STATUS_RUN;
		else if ("STOP".equals(status) || FIREDOOR_STATUS_STOP.equals(status))
			this.status = FIREDOOR_STATUS_STOP;
		else if ("WARNING".equals(status) || FIREDOOR_STATUS_WARNING.equals(status))
			this.status = FIREDOOR_STATUS_WARNING;
	}
	
	public String getPrevStatus() {
		return prevStatus;
	}

	public void setPrevStatus(String prevStatus) {
		this.prevStatus = prevStatus;
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

	public boolean isEnabled() {
		if (FIREDOOR_ENABLED.equalsIgnoreCase(getEnabled()))
			return true;
		else
			return false;
	}
	
	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = Integer.parseInt(errorCode);
	}
	
	public int getPrevErrorCode() {
		return prevErrorCode;
	}

	public void setPrevErrorCode(int prevErrorCode) {
		this.prevErrorCode = prevErrorCode;
	}
	
	public long getLastReportedTime() {
		return lLastReportedTime;
	}

	public void setLastReportedTime(long lLastReportedTime) {
		this.lLastReportedTime = lLastReportedTime;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public List<String> getNodeList() {
		return nodeList;
	}

	public void setNodeList(List<String> nodeList) {
		this.nodeList = nodeList;
	}

	@Override
	public String toString() {
		return "FireDoor [fireDoorId=" + fireDoorId + ", enabled=" + enabled
				+ ", ipAddress=" + ipAddress + ", status=" + status
				+ ", errorCode=" + errorCode + "]";
	}

	
}
