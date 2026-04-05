package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.OcsConstant.DETOUR_CONTROL_LEVEL;

/**
 * VehicleError Class, OCS 3.0 for Unified FAB
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

public class VehicleError {
	protected String classification;
	protected int errorCode;
	protected String errorText;
	protected int troubleCost;
	protected String actionType;
	protected int actionTypeHashCode;
	protected DETOUR_CONTROL_LEVEL detourControlLevel;
	
	public String getClassification() {
		return classification;
	}
	public void setClassification(String classification) {
		this.classification = classification;
	}
	public String getActionType() {
		return actionType;
	}
	public int getActionTypeHashCode() {
		return actionTypeHashCode;
	}
	public void setActionType(String actionType) {
		this.actionType = actionType;
		this.actionTypeHashCode = actionType.hashCode(); 
	}
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getErrorText() {
		return errorText;
	}
	public void setErrorText(String errorText) {
		this.errorText = errorText;
	}
	public int getTroubleCost() {
		return troubleCost;
	}
	public void setTroubleCost(int troubleCost) {
		this.troubleCost = troubleCost;
	}
	public DETOUR_CONTROL_LEVEL getDetourControlLevel() {
		return detourControlLevel;
	}
	public void setDetourControlLevel(DETOUR_CONTROL_LEVEL detourControlLevel) {
		this.detourControlLevel = detourControlLevel;
	}
}
