package com.samsung.ocs.unitdevice.model;

public class FFU {
	private String ffuGroupId = "";
	private String status = "R";
	private String prevStatus = "R";
	private int totalFFU = 0;
	private int abnormalFFU = 0;
	private int commfailFFU = 0;
	private String nodeId = "";
	private String ipAddress = "127.0.0.1";
	private String enabled = "FALSE";
	private int errorCode = 0;
	private int prevErrorCode = 0;
	private long lLastReportedTime = 0;
	
	private final static String FFU_ENABLED = "TRUE";
	private final static String FFU_DISABLED = "FALSE";
	private final static String FFU_STATUS_RUN = "R";
	private final static String FFU_STATUS_STOP = "S";
	private final static String FFU_STATUS_WARNING = "W";

	public FFU(String ffuGroupId) {
		setFFUGroupId(ffuGroupId);
	}

	public void setFFU(String ffuGroupId, String nodeId, String ipAddress, String enabled) {
		this.ffuGroupId = ffuGroupId;
		this.nodeId = nodeId;
		this.ipAddress = ipAddress;
		this.enabled = enabled;
	}

	public String getFFUGroupId() {
		return ffuGroupId;
	}

	public void setFFUGroupId(String ffuGroupId) {
		this.ffuGroupId = ffuGroupId;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		if ("RUN".equals(status) || FFU_STATUS_RUN.equals(status))
			this.status = FFU_STATUS_RUN;
		else if ("STOP".equals(status) || FFU_STATUS_STOP.equals(status))
			this.status = FFU_STATUS_STOP;
		else if ("WARNING".equals(status) || FFU_STATUS_WARNING.equals(status))
			this.status = FFU_STATUS_WARNING;
	}
	
	public String getPrevStatus() {
		return prevStatus;
	}

	public void setPrevStatus(String prevStatus) {
		this.prevStatus = prevStatus;
	}
	
	public int getTotalFFU() {
		return totalFFU;
	}

	public void setTotalFFU(int totalFFU) {
		this.totalFFU = totalFFU;
	}
	
	public int getAbnormalFFU() {
		return abnormalFFU;
	}

	public void setAbnormalFFU(int abnormalFFU) {
		this.abnormalFFU = abnormalFFU;
	}
	
	public int getCommfailFFU() {
		return commfailFFU;
	}

	public void setCommfailFFU(int commfailFFU) {
		this.commfailFFU = commfailFFU;
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

	public boolean isEnabled() {
		if (FFU_ENABLED.equalsIgnoreCase(getEnabled()))
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
}
