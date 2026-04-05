package com.samsung.ocs.stbc.rfc.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.RFC_COND;

/**
 * RFCData Class, OCS 3.0 for Unified FAB
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

public class RFCData {
	private String rfcId;
	private String ipAddress;
	private long lastSendTime = 0L;
	private long lastRecievedTime = 0L;
	private long lastStatusSendTime = 0L;
	private String machineCode;
	private String machineId;
	private RFC_COND condition;
	private byte[] status;
	private boolean enabled;
	private int ready;
	private String errorCode;
	private int error;
	private UDPSender sender;
	
	/**
	 * Constructor of RFCData class.
	 * 
	 * @param rfcId
	 */
	public RFCData(String rfcId) {
		this.rfcId = rfcId;
		status = new byte[256];
		this.lastSendTime = System.currentTimeMillis();
		this.lastStatusSendTime = System.currentTimeMillis();
	}
	
	public String getRfcId() {
		return rfcId;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	public UDPSender getSender() {
		return sender;
	}
	public void setSender(UDPSender sender) {
		this.sender = sender;
	}
	
	public long getLastSendTime() {
		return lastSendTime;
	}

	public void setLastSendTime(long lastSendTime) {
		this.lastSendTime = lastSendTime;
	}
	
	public boolean setStatusAndReturnEquals(byte [] newStatus) {
		boolean equals = true;
		for (int i = 0; i < status.length; i++) {
			if (status[i] != newStatus[i]) {
				equals = false;
			}
			status[i] = newStatus[i];
		}
		return equals;
	}

	public long getLastRecievedTime() {
		return lastRecievedTime;
	}

	public void setLastRecievedTime(long lastRecievedTime) {
		this.lastRecievedTime = lastRecievedTime;
	}

	public String getMachineCode() {
		return machineCode;
	}

	public void setMachineCode(String machineCode) {
		this.machineCode = machineCode;
	}

	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(String machineId) {
		this.machineId = machineId;
	}

	public RFC_COND getCondition() {
		return condition;
	}

	public void setCondition(RFC_COND condition) {
		this.condition = condition;
	}

	public byte[] getStatus() {
		return status;
	}

	public void setStatus(byte[] status) {
		this.status = status;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public void setRfcId(String rfcId) {
		this.rfcId = rfcId;
	}
	
	private Map<String, STBData> stbMap = new HashMap<String, STBData>();
	private Set<String> indexSet = new HashSet<String>();
	
	public void addSTB(STBData stb) {
		stb.setOwner(this);
		stbMap.put(stb.getRfcIndex() + "", stb);
		indexSet.add(stb.getRfcIndex() + "");
	}
	
	public Set<String> getNewIndexSet() {
		return new HashSet<String>(indexSet);
	}

	public STBData getStb(int rfcIndex) {
		return stbMap.get(rfcIndex + "");
	}

	public int getReady() {
		return ready;
	}

	public void setReady(int ready) {
		this.ready = ready;
	}

	public int getError() {
		return error;
	}

	public void setError(int error) {
		this.error = error;
	}

	public long getLastStatusSendTime() {
		return lastStatusSendTime;
	}

	public void setLastStatusSendTime(long lastStatusSendTime) {
		this.lastStatusSendTime = lastStatusSendTime;
	}
}
