package com.samsung.ocs.manager.impl.model;

/**
 * TrCompletionHistory Class, OCS 3.0 for Unified FAB
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

public class TrCompletionHistory {
	protected String trCmdId;
	protected int priority;
	protected String carrierId;
	protected String sourceLoc;
	protected String destLoc;
	protected String vehicle;
	protected String trQueuedTime;
	protected String unloadAssignedTime;
	protected String unloadingTime;
	protected String unloadedTime;
	protected String loadAssignedTime;
	protected String loadingTime;
	protected String loadedTime;
	protected String deletedTime;
	protected String sourceNode;
	protected String destNode;
	protected String remoteCmd;
	protected int unloadAutoRetryCount;
	protected long unloadBT;
	protected int loadAutoRetryCount;
	protected long loadBT;
	protected long noBlockingTime;
	protected long waitTimeout;
	protected String destChangedTrCmdId;
	protected long expectedDuration;
	protected boolean ocsRegistered;
	protected String vehicleLocus;
	// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
	protected String foupId;
	// 2022.03.14 dahye : TRANSFER_EX4
	protected String deliveryType;
	protected long expectedDeliveryTime;
	protected long deliveryTimeout;
	
	/**
	 * Constructor of TrCompletionHistory class.
	 */
	public TrCompletionHistory(String trCmdId, int priority, String carrierId, String sourceLoc, String destLoc, String vehicle, String trQueuedTime, String unloadAssignedTime, String unloadingTime, String unloadedTime, 
			String loadAssignedTime, String loadingTime, String loadedTime, String deletedTime, String sourceNode, String destNode, String remoteCmd, int unloadAutoRetryCount, long unloadBT, 
//			int loadAutoRetryCount, long loadBT, long noBlockingTime, long waitTimeout, String destChangedTrCmdId, long expectedDuration, boolean ocsRegistered, String vehicleLocus, String foupId) {
			// 2022.03.14 dahye : TRANSFER_EX4
			int loadAutoRetryCount, long loadBT, long noBlockingTime, long waitTimeout, String destChangedTrCmdId, long expectedDuration, boolean ocsRegistered, String vehicleLocus, String foupId,
			String deliveryType, long expectedDeliveryTime, long deliveryTimeout) {
		this.trCmdId = trCmdId;
		this.priority = priority;
		this.carrierId = carrierId;
		this.sourceLoc = sourceLoc;
		this.destLoc = destLoc;
		this.vehicle = vehicle;
		this.trQueuedTime = trQueuedTime;
		this.unloadAssignedTime = unloadAssignedTime;
		this.unloadingTime = unloadingTime;
		this.unloadedTime = unloadedTime;
		this.loadAssignedTime = loadAssignedTime;
		this.loadingTime = loadingTime;
		this.loadedTime = loadedTime;
		this.deletedTime = deletedTime;
		this.sourceNode = sourceNode;
		this.destNode = destNode;
		this.remoteCmd = remoteCmd;
		this.unloadAutoRetryCount = unloadAutoRetryCount;
		this.unloadBT = unloadBT;
		this.loadAutoRetryCount = loadAutoRetryCount;
		this.loadBT = loadBT;
		this.noBlockingTime = noBlockingTime;
		this.waitTimeout = waitTimeout;
		this.destChangedTrCmdId = destChangedTrCmdId;
		this.expectedDuration = expectedDuration;
		this.ocsRegistered = ocsRegistered;
		this.vehicleLocus = vehicleLocus;
		// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
		this.foupId = foupId ;
		// 2022.03.14 dahye : TRANSFER_EX4
		this.deliveryType = deliveryType;
		this.expectedDeliveryTime = expectedDeliveryTime;
		this.deliveryTimeout = deliveryTimeout;
	}
	
	// 2012.02.20 by PMM
	public TrCompletionHistory(TrCmd trCmd, String remoteCmd, String vehicleLocus) {
		if (trCmd != null) {
			this.trCmdId = trCmd.getTrCmdId();
			this.priority = trCmd.getPriority();
			this.carrierId = trCmd.getCarrierId();
			this.sourceLoc = trCmd.getSourceLoc();
			this.destLoc = trCmd.getDestLoc();
			this.vehicle = trCmd.getVehicle();
			this.trQueuedTime = trCmd.getTrQueuedTime();
			this.unloadAssignedTime = trCmd.getUnloadAssignedTime();
			this.unloadingTime = trCmd.getUnloadingTime();
			this.unloadedTime = trCmd.getUnloadedTime();
			this.loadAssignedTime = trCmd.getLoadAssignedTime();
			this.loadingTime = trCmd.getLoadingTime();
			this.loadedTime = trCmd.getLoadedTime();
			this.deletedTime = trCmd.getDeletedTime();
			this.sourceNode = trCmd.getSourceNode();
			this.destNode = trCmd.getDestNode();
			this.remoteCmd = remoteCmd;
			this.unloadAutoRetryCount = trCmd.getUnloadAutoRetryCount();
			this.unloadBT = trCmd.getUnloadBT();
			this.loadAutoRetryCount = trCmd.getLoadAutoRetryCount();
			this.loadBT = trCmd.getLoadBT();
			this.noBlockingTime = trCmd.getNoBlockingTime();
			this.waitTimeout = trCmd.getWaitTimeout();
			this.destChangedTrCmdId = trCmd.getChangedTrCmdId();
			this.expectedDuration = trCmd.getExpectedDuration();
			this.ocsRegistered = trCmd.isOcsRegistered();
			this.vehicleLocus = vehicleLocus;
			this.foupId = trCmd.getFoupId();
			// 2022.03.14 dahye : TRANSFER_EX4
			this.deliveryType = trCmd.getDeliveryType();
			this.expectedDeliveryTime = trCmd.getExpectedDeliveryTime();
			this.deliveryTimeout = trCmd.getDeliveryWaitTimeOut();
		} else {
			this.trCmdId = "NULL";
			this.priority = 0;
			this.carrierId = "NULL";
			this.sourceLoc = "NULL";
			this.destLoc = "NULL";
			this.vehicle = "NULL";
			this.trQueuedTime = "NULL";
			this.unloadAssignedTime = "NULL";
			this.unloadingTime = "NULL";
			this.unloadedTime = "NULL";
			this.loadAssignedTime = "NULL";
			this.loadingTime = "NULL";
			this.loadedTime = "NULL";
			this.deletedTime = "NULL";
			this.sourceNode = "NULL";
			this.destNode = "NULL";
			this.remoteCmd = remoteCmd;
			this.unloadAutoRetryCount = 0;
			this.unloadBT = 0;
			this.loadAutoRetryCount = 0;
			this.loadBT = 0;
			this.noBlockingTime = 0;
			this.waitTimeout = 0;
			this.destChangedTrCmdId = "NULL";
			this.expectedDuration = 0;
			this.ocsRegistered = true;
			this.vehicleLocus = vehicleLocus;
			this.foupId = "NULL";
			// 2022.03.14 dahye : TRANSFER_EX4
			this.deliveryType = "NULL";
			this.expectedDeliveryTime = 0;
			this.deliveryTimeout = 0;
		}
	}
	
	public String getTrCmdId() {
		return trCmdId;
	}
	public void setTrCmdId(String trCmdId) {
		this.trCmdId = trCmdId;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public String getCarrierId() {
		return carrierId;
	}
	public void setCarrierId(String carrierId) {
		this.carrierId = carrierId;
	}
	public String getSourceLoc() {
		return sourceLoc;
	}
	public void setSourceLoc(String sourceLoc) {
		this.sourceLoc = sourceLoc;
	}
	public String getDestLoc() {
		return destLoc;
	}
	public void setDestLoc(String destLoc) {
		this.destLoc = destLoc;
	}
	public String getVehicle() {
		return vehicle;
	}
	public void setVehicle(String vehicle) {
		this.vehicle = vehicle;
	}
	public String getTrQueuedTime() {
		return trQueuedTime;
	}
	public void setTrQueuedTime(String trQueuedTime) {
		this.trQueuedTime = trQueuedTime;
	}
	public String getUnloadAssignedTime() {
		return unloadAssignedTime;
	}
	public void setUnloadAssignedTime(String unloadAssignedTime) {
		this.unloadAssignedTime = unloadAssignedTime;
	}
	public String getUnloadingTime() {
		return unloadingTime;
	}
	public void setUnloadingTime(String unloadingTime) {
		this.unloadingTime = unloadingTime;
	}
	public String getUnloadedTime() {
		return unloadedTime;
	}
	public void setUnloadedTime(String unloadedTime) {
		this.unloadedTime = unloadedTime;
	}
	public String getLoadAssignedTime() {
		return loadAssignedTime;
	}
	public void setLoadAssignedTime(String loadAssignedTime) {
		this.loadAssignedTime = loadAssignedTime;
	}
	public String getLoadingTime() {
		return loadingTime;
	}
	public void setLoadingTime(String loadingTime) {
		this.loadingTime = loadingTime;
	}
	public String getLoadedTime() {
		return loadedTime;
	}
	public void setLoadedTime(String loadedTime) {
		this.loadedTime = loadedTime;
	}
	public String getDeletedTime() {
		return deletedTime;
	}
	public void setDeletedTime(String deletedTime) {
		this.deletedTime = deletedTime;
	}
	public String getSourceNode() {
		return sourceNode;
	}
	public void setSourceNode(String sourceNode) {
		this.sourceNode = sourceNode;
	}
	public String getDestNode() {
		return destNode;
	}
	public void setDestNode(String destNode) {
		this.destNode = destNode;
	}
	public String getRemoteCmd() {
		return remoteCmd;
	}
	public void setRemoteCmd(String remoteCmd) {
		this.remoteCmd = remoteCmd;
	}
	public int getUnloadAutoRetryCount() {
		return unloadAutoRetryCount;
	}
	public void setUnloadAutoRetryCount(int unloadAutoRetryCount) {
		this.unloadAutoRetryCount = unloadAutoRetryCount;
	}
	public long getUnloadBT() {
		return unloadBT;
	}
	public void setUnloadBT(long unloadBT) {
		this.unloadBT = unloadBT;
	}
	public int getLoadAutoRetryCount() {
		return loadAutoRetryCount;
	}
	public void setLoadAutoRetryCount(int loadAutoRetryCount) {
		this.loadAutoRetryCount = loadAutoRetryCount;
	}
	public long getLoadBT() {
		return loadBT;
	}
	public void setLoadBT(long loadBT) {
		this.loadBT = loadBT;
	}
	public long getNoBlockingTime() {
		return noBlockingTime;
	}
	public void setNoBlockingTime(long noBlockingTime) {
		this.noBlockingTime = noBlockingTime;
	}
	public long getWaitTimeout() {
		return waitTimeout;
	}
	public void setWaitTimeout(long waitTimeout) {
		this.waitTimeout = waitTimeout;
	}
	public String getDestChangedTrCmdId() {
		return destChangedTrCmdId;
	}
	public void setDestChangedTrCmdId(String destChangedTrCmdId) {
		this.destChangedTrCmdId = destChangedTrCmdId;
	}
	public long getExpectedDuration() {
		return expectedDuration;
	}
	public void setExpectedDuration(long expectedDuration) {
		this.expectedDuration = expectedDuration;
	}
	public boolean isOcsRegistered() {
		return ocsRegistered;
	}
	public void setOcsRegistered(boolean ocsRegistered) {
		this.ocsRegistered = ocsRegistered;
	}
	public String getVehicleLocus() {
		return vehicleLocus;
	}
	public void setVehicleLocus(String vehicleLocus) {
		this.vehicleLocus = vehicleLocus;
	}

	public String getFoupId() {
		return foupId;
	}

	public void setFoupId(String foupId) {
		this.foupId = foupId;
	}

	public String getDeliveryType() {
		return deliveryType;
	}

	public void setDeliveryType(String deliveryType) {
		this.deliveryType = deliveryType;
	}

	public long getExpectedDeliveryTime() {
		return expectedDeliveryTime;
	}

	public void setExpectedDeliveryTime(long expectedDeliveryTime) {
		this.expectedDeliveryTime = expectedDeliveryTime;
	}

	public long getDeliveryTimeout() {
		return deliveryTimeout;
	}

	public void setDeliveryTimeout(long deliveryTimeout) {
		this.deliveryTimeout = deliveryTimeout;
	}
	
}
