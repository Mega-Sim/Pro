package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;

/**
 * Detour Class, OCS 3.1 for Unified FAB
 * 
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * 
 * @date   2015. 1. 23.
 * @version 3.1
 * 
 * Copyright 2012 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class DetourControl {
	private String detourId;
	private boolean detourUsed = false;
	private boolean transferCancelUsed = false;
	private boolean transferAbortUsed = false;
	private int transferAbortTimeout = 0;
	private boolean portServiceUsed = false;
	private double penalty = 9999;
	private DETOUR_REASON detourReason = DETOUR_REASON.NONE; 
	
	public DETOUR_REASON getDetourReason() {
		return detourReason;
	}
	public void setDetourReason(DETOUR_REASON detourReason) {
		this.detourReason = detourReason;
	}
	public String getDetourId() {
		return detourId;
	}
	public void setDetourId(String detourId) {
		this.detourId = detourId;
		this.detourReason = DETOUR_REASON.toReasonType(detourId);
	}
	public boolean isDetourUsed() {
		return detourUsed;
	}
	public void setDetourUsed(boolean detourUsed) {
		this.detourUsed = detourUsed;
	}
	public boolean isTransferCancelUsed() {
		return transferCancelUsed;
	}
	public void setTransferCancelUsed(boolean transferCancelUsed) {
		this.transferCancelUsed = transferCancelUsed;
	}
	public boolean isTransferAbortUsed() {
		return transferAbortUsed;
	}
	public void setTransferAbortUsed(boolean transferAbortUsed) {
		this.transferAbortUsed = transferAbortUsed;
	}
	public int getTransferAbortTimeout() {
		return transferAbortTimeout;
	}
	public void setTransferAbortTimeout(int transferAbortTimeout) {
		this.transferAbortTimeout = transferAbortTimeout;
	}
	public boolean isPortServiceUsed() {
		return portServiceUsed;
	}
	public void setPortServiceUsed(boolean portServiceUsed) {
		this.portServiceUsed = portServiceUsed;
	}
	public double getPenalty() {
		return penalty;
	}
	public void setPenalty(double penalty) {
		this.penalty = penalty;
	}
}
