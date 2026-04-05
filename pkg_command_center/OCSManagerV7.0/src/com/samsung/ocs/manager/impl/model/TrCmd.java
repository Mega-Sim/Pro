package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;

/**
 * TrCmd Class, OCS 3.0 for Unified FAB
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

public class TrCmd {
	private String trCmdId;
	private TRCMD_REMOTECMD remoteCmd;
	private TRCMD_STATE state;
	private TRCMD_DETAILSTATE detailState;
	private String stateChangedTime;
	private String carrierId;
	private String sourceLoc;
	private String destLoc;
	private String carrierLoc;
	private String sourceNode;
	private String destNode;
	private String vehicle;
	private int priority;
	private int replace;
	private String trQueuedTime;
	private String unloadAssignedTime;
	private String unloadingTime;
	private String unloadedTime;
	private String loadAssignedTime;
	private String loadingTime;
	private String loadedTime;
	private boolean pause;
	private String pausedTime;
	private String pauseType;
	private int pauseCount;
	private boolean remove;
	private String deletedTime;
	private boolean loadingByPass;
	private String reason;
	private long expectedDuration;
	private long noBlockingTime;
	private long waitTimeout;
	private boolean ocsRegistered;
	
	private long lastAbortedTime;		//DB에 저장하지 않음.
	private long remainingDuration;		//DB에 저장하지 않음.
	private long stageInitTime;			//DB에 저장하지 않음.
	
	private int unloadAutoRetryCount;
	private long unloadBT;				// 이동 중 정체 시간
	private int loadAutoRetryCount;
	private long loadBT;				// 이동 중 정체 시간
	
	// 2011.09.15 by MYM : [Vehicle Request 정리]
	private TRCMD_REMOTECMD changedRemoteCmd;
	private String changedTrCmdId;
	private String assignedVehicleId;
	
	// 2012.08.01 by PMM
	private boolean isPriorJob;
	private double loadTransferTime;			//DB에 저장하지 않음. for JobAssign.
	
	// 2012.08.21 by MYM : 사용자 경로 지정 개선 - TrCmd에서 사용자 경로 정보 설정
	private UserDefinedPath userDefinedPath;

	// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
	private String foupId;

	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	private String deliveryType;
	private int expectedDeliveryTime;
	private int deliveryWaitTimeOut;
	private String waitStartedTime;	// 2022.03.14 dahye : Premove Logic Improve
	
	
	// 2015.06.11 by KYK
	private boolean isDestChangeDelayed;

	private String patrolId;
	private int patrolMode;

	// 2021.01.21 by JJW
	private String oldDestLoc;
	private String oldDestNode;
	private String oldCarrierId;
	private int oldPriority;

	/**
	 * Constructor of TrCmd class.
	 */
	public TrCmd() {
		// 2012.01.04 by PMM
		// unloadBT, loadBT: String -> long
		unloadBT = 0;
		loadBT = 0;
		unloadAutoRetryCount = 0;
		loadAutoRetryCount = 0;
		loadTransferTime = 9999;
	}
	
	/**
	 * Constructor of TrCmd class.
	 */
	public TrCmd(TrCmd trCmd) {
		this.carrierId = trCmd.getCarrierId();
		this.carrierLoc = trCmd.getCarrierLoc();
		this.deletedTime = trCmd.getDeletedTime();
		this.destLoc = trCmd.getDestLoc();
		this.destNode = trCmd.getDestNode();
		this.detailState = trCmd.getDetailState();
		this.expectedDuration = trCmd.getExpectedDuration();
		this.loadAssignedTime = trCmd.getLoadAssignedTime();
		this.loadedTime = trCmd.getLoadedTime();
		this.loadingByPass = trCmd.isLoadingByPass();
		this.loadingTime = trCmd.getLoadingTime();
		this.noBlockingTime = trCmd.getNoBlockingTime();
		this.ocsRegistered = trCmd.isOcsRegistered();
		this.pause = trCmd.isPause();
		this.pauseCount = trCmd.getPauseCount();
		this.pausedTime = trCmd.getPausedTime();
		this.pauseType = trCmd.getPauseType();
		this.priority = trCmd.getPriority();
		this.reason = trCmd.getReason();
		this.remainingDuration = trCmd.getRemainingDuration();
		this.remoteCmd = trCmd.getRemoteCmd();
		this.remove = trCmd.isRemove();
		this.replace = trCmd.getReplace();
		this.sourceLoc = trCmd.getSourceLoc();
		this.sourceNode = trCmd.getSourceNode();
		this.stageInitTime = trCmd.getStageInitTime();
		this.state = trCmd.getState();
		this.stateChangedTime = trCmd.getStateChangedTime();
		this.trCmdId = trCmd.getTrCmdId();
		this.trQueuedTime = trCmd.getTrQueuedTime();
		this.unloadAssignedTime = trCmd.getUnloadAssignedTime();
		this.unloadedTime = trCmd.getUnloadedTime();
		this.unloadingTime = trCmd.getUnloadingTime();
		this.vehicle = trCmd.getVehicle();
		this.waitTimeout = trCmd.getWaitTimeout();
		
		this.changedRemoteCmd = trCmd.getChangedRemoteCmd();
		this.changedTrCmdId = trCmd.getChangedTrCmdId();
		this.assignedVehicleId = trCmd.getAssignedVehicleId();
		
		// 2012.01.04 by PMM
		this.unloadAutoRetryCount = trCmd.getUnloadAutoRetryCount();
		this.unloadBT = trCmd.getUnloadBT();
		this.loadAutoRetryCount = trCmd.getLoadAutoRetryCount();
		this.loadBT = trCmd.getLoadBT();
		
		this.isPriorJob = trCmd.isPriorJob();
		this.loadTransferTime = trCmd.getLoadTransferTime();
		
		this.userDefinedPath = trCmd.getUserDefinedPath();
		this.foupId = trCmd.getFoupId();
		this.patrolId = trCmd.getPatrolId();
		this.patrolMode = trCmd.getPatrolMode();
		
		// 2021.04.02 by JDH : Transfer Premove 사양 추가
		this.deliveryType = trCmd.getDeliveryType();
		this.expectedDeliveryTime = trCmd.getExpectedDeliveryTime();
		this.deliveryWaitTimeOut = trCmd.getDeliveryWaitTimeOut();
		this.waitStartedTime = trCmd.getWaitStartedTime();	// 2022.03.14 dahye : Premove Logic Improve
		
	}
	
	public String toString() {
		return trCmdId;
	}
	
	public TRCMD_REMOTECMD getChangedRemoteCmd() {
		return changedRemoteCmd;
	}

	public void setChangedRemoteCmd(TRCMD_REMOTECMD changedRemoteCmd) {
		this.changedRemoteCmd = changedRemoteCmd;
	}

	public String getChangedTrCmdId() {
		return changedTrCmdId;
	}

	public void setChangedTrCmdId(String changedTrCmdId) {
		this.changedTrCmdId = changedTrCmdId;
	}

	public String getAssignedVehicleId() {
		return assignedVehicleId;
	}

	public void setAssignedVehicleId(String assignedVehicleId) {
		this.assignedVehicleId = assignedVehicleId;
	}
	
	public String getTrCmdId() {
		return trCmdId;
	}
	public void setTrCmdId(String trCmdId) {
		this.trCmdId = trCmdId;
	}
	public TRCMD_REMOTECMD getRemoteCmd() {
		return remoteCmd;
	}
	public void setRemoteCmd(TRCMD_REMOTECMD remoteCmd) {
		this.remoteCmd = remoteCmd;
	}
	public TRCMD_STATE getState() {
		return state;
	}
	public void setState(TRCMD_STATE state) {
		this.state = state;
	}
	public TRCMD_DETAILSTATE getDetailState() {
		return detailState;
	}
	public void setDetailState(TRCMD_DETAILSTATE detailState) {
		this.detailState = detailState;
	}
	public String getStateChangedTime() {
		return stateChangedTime;
	}
	public void setStateChangedTime(String stateChangedTime) {
		this.stateChangedTime = stateChangedTime;
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
	public String getCarrierLoc() {
		return carrierLoc;
	}
	public void setCarrierLoc(String carrierLoc) {
		this.carrierLoc = carrierLoc;
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
	public String getVehicle() {
		return vehicle;
	}
	public void setVehicle(String vehicle) {
		this.vehicle = vehicle;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public int getReplace() {
		return replace;
	}
	public void setReplace(int replace) {
		this.replace = replace;
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
	public boolean isPause() {
		return pause;
	}
	public void setPause(boolean pause) {
		this.pause = pause;
	}
	public String getPausedTime() {
		return pausedTime;
	}
	public void setPausedTime(String pausedTime) {
		this.pausedTime = pausedTime;
	}
	public String getPauseType() {
		return pauseType;
	}
	public void setPauseType(String pauseType) {
		// 2012.03.21 by PMM
		// TRCMD 테이블의 PauseType Size가 16임.
//		this.pauseType = pauseType;
		if (pauseType != null && pauseType.length() > 16) {
			this.pauseType = pauseType.substring(0, 16);
		} else {
			this.pauseType = pauseType;
		}
	}
	public int getPauseCount() {
		return pauseCount;
	}
	public void setPauseCount(int pauseCount) {
		this.pauseCount = pauseCount;
	}
	public boolean isRemove() {
		return remove;
	}
	public void setRemove(boolean remove) {
		this.remove = remove;
	}
	public String getDeletedTime() {
		return deletedTime;
	}
	public void setDeletedTime(String deletedTime) {
		this.deletedTime = deletedTime;
	}
	public boolean isLoadingByPass() {
		return loadingByPass;
	}
	public void setLoadingByPass(boolean loadingByPass) {
		this.loadingByPass = loadingByPass;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public long getExpectedDuration() {
		return expectedDuration;
	}
	public void setExpectedDuration(long expectedDuration) {
		this.expectedDuration = expectedDuration;
	}
	public long getNoBlockingTime() {
		return noBlockingTime;
	}
	public void setNoBlockingTime(long noblockingTime) {
		this.noBlockingTime = noblockingTime;
	}
	public long getWaitTimeout() {
		return waitTimeout;
	}
	public void setWaitTimeout(long waitTimeout) {
		this.waitTimeout = waitTimeout;
	}
	public boolean isOcsRegistered() {
		return ocsRegistered;
	}
	public void setOcsRegistered(boolean ocsRegistered) {
		this.ocsRegistered = ocsRegistered;
	}
	public long getRemainingDuration() {
		return remainingDuration;
	}
	public void setRemainingDuration(long remainingDuration) {
		this.remainingDuration = remainingDuration;
	}
	public long getStageInitTime() {
		return stageInitTime;
	}
	public void setStageInitTime(long stageInitTime) {
		this.stageInitTime = stageInitTime;
	}

	public long getLastAbortedTime() {
		return lastAbortedTime;
	}

	public void setLastAbortedTime(long lastAbortedTime) {
		this.lastAbortedTime = lastAbortedTime;
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

	public boolean isPriorJob() {
		return isPriorJob;
	}

	public void setPriorJob(boolean isPriorJob) {
		this.isPriorJob = isPriorJob;
	}

	public double getLoadTransferTime() {
		return loadTransferTime;
	}

	public void setLoadTransferTime(double loadTransferTime) {
		this.loadTransferTime = loadTransferTime;
	}
	
	/**
	 * 2012.08.21 by MYM : 사용자 경로 지정 개선
	 * @return
	 */
	public UserDefinedPath getUserDefinedPath() {
		return userDefinedPath;
	}
	
	/**
	 * 2012.08.21 by MYM : 사용자 경로 지정 개선
	 * @param userDefinedPath
	 */
	public void setUserDefinedPath(UserDefinedPath userDefinedPath) {
		this.userDefinedPath = userDefinedPath;
	}

	public String getFoupId() {
		return foupId;
	}

	public void setFoupId(String foupId) {
		this.foupId = foupId;
	}

	public boolean isDestChangeDelayed() {
		return isDestChangeDelayed;
	}

	public void setDestChangeDelayed(boolean isDestChangeDelayed) {
		this.isDestChangeDelayed = isDestChangeDelayed;
	}

	public String getPatrolId() {
		return patrolId;
	}

	public void setPatrolId(String patrolId) {
		this.patrolId= patrolId;
	}

	public int getPatrolMode() {
		return patrolMode;
	}

	public void setPatrolMode(int patrolMode) {
		this.patrolMode = patrolMode;
	}
	public String getDeliveryType() {
		return deliveryType;
	}

	public void setDeliveryType(String deliveryType) {
		this.deliveryType = deliveryType;
	}

	public int getExpectedDeliveryTime() {
		return expectedDeliveryTime;
	}

	public void setExpectedDeliveryTime(int expectedDeliveryTime) {
		this.expectedDeliveryTime = expectedDeliveryTime;
	}

	public int getDeliveryWaitTimeOut() {
		return deliveryWaitTimeOut;
	}

	public void setDeliveryWaitTimeOut(int deliveryWaitTimeOut) {
		this.deliveryWaitTimeOut = deliveryWaitTimeOut;
	}

	public String getWaitStartedTime() {
		return waitStartedTime;
	}

	public void setWaitStartedTime(String waitStartedTime) {
		this.waitStartedTime = waitStartedTime;
	}

	public String getOldDestLoc() {
		return oldDestLoc;
	}

	public void setOldDestLoc(String oldDestLoc) {
		this.oldDestLoc = oldDestLoc;
	}

	public String getOldDestNode() {
		return oldDestNode;
	}

	public void setOldDestNode(String oldDestNode) {
		this.oldDestNode = oldDestNode;
	}

	public String getOldCarrierId() {
		return oldCarrierId;
	}

	public void setOldCarrierId(String oldCarrierId) {
		this.oldCarrierId = oldCarrierId;
	}

	public int getOldPriority() {
		return oldPriority;
	}

	public void setOldPriority(int oldPriority) {
		this.oldPriority = oldPriority;
	}
}