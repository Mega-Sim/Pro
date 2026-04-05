package com.samsung.ocs.manager.impl.model;

/**
 * VehicleErrorHistory Class, OCS 3.0 for Unified FAB
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

public class VehicleErrorHistory {
	protected String vehicle;
	protected String node;
	protected int alarmCode;
	protected String alarmText;
	protected String type;
	protected String trCmdId;
	protected String trStatus;
	protected String carrierId;
	protected String carrierLoc;
	protected String sourceLoc;
	protected String destLoc;
	protected String setTime;
	protected String clearTime;
	protected String showMsg;
	
	/**
	 * Constructor of VehicleErrorHistory class.
	 */
	public VehicleErrorHistory() {
		this.vehicle = "";
		this.node = "";
		this.alarmCode = 0;
		this.alarmText = "";
		this.type = "";
		this.trCmdId = "";
		this.trStatus = "";
		this.carrierId = "";
		this.carrierLoc = "";
		this.sourceLoc = "";
		this.destLoc = "";
		this.setTime = "";
		this.clearTime = "";
		this.showMsg = "";
	}
	
	/**
	 * Constructor of VehicleErrorHistory class.
	 */
	public VehicleErrorHistory(String vehicle, String clearTime) {
		this.vehicle = vehicle;
		this.node = "";
		this.alarmCode = 0;
		this.alarmText = "";
		this.type = "";
		this.trCmdId = "";
		this.trStatus = "";
		this.carrierId = "";
		this.carrierLoc = "";
		this.sourceLoc = "";
		this.destLoc = "";
		this.setTime = "";
		this.clearTime = clearTime;
		this.showMsg = "";
	}
	
	/**
	 * Constructor of VehicleErrorHistory class.
	 */
	public VehicleErrorHistory(String vehicle, String node, int alarmCode, String alarmText, String type, String trCmdId, String trStatus, String carrierId, String carrierLoc, String sourceLoc, String destLoc, String setTime, String clearTime, String showMsg) {
		this.vehicle = vehicle;
		this.node = node;
		this.alarmCode = alarmCode;
		this.alarmText = alarmText;
		this.type = type;
		this.trCmdId = trCmdId;
		this.trStatus = trStatus;
		this.carrierId = carrierId;
		this.carrierLoc = carrierLoc;
		this.sourceLoc = sourceLoc;
		this.destLoc = destLoc;
		this.setTime = setTime;
		this.clearTime = clearTime;
		this.showMsg = showMsg;
	}
	
	public String getVehicle() {
		return vehicle;
	}
	public void setVehicle(String vehicle) {
		this.vehicle = vehicle;
	}
	public String getNode() {
		return node;
	}
	public void setNode(String node) {
		this.node = node;
	}
	public String getCarrierLoc() {
		return carrierLoc;
	}
	public void setCarrierLoc(String carrierLoc) {
		this.carrierLoc = carrierLoc;
	}
	public int getAlarmCode() {
		return alarmCode;
	}
	public void setAlarmCode(int alarmCode) {
		this.alarmCode = alarmCode;
	}
	public String getAlarmText() {
		return alarmText;
	}
	public void setAlarmText(String alarmText) {
		this.alarmText = alarmText;
	}
	public String getSetTime() {
		return setTime;
	}
	public void setSetTime(String setTime) {
		this.setTime = setTime;
	}
	public String getClearTime() {
		return clearTime;
	}
	public void setClearTime(String clearTime) {
		this.clearTime = clearTime;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTrStatus() {
		return trStatus;
	}
	public void setTrStatus(String trStatus) {
		this.trStatus = trStatus;
	}
	public String getTrCmdId() {
		return trCmdId;
	}
	public void setTrCmdId(String trCmdId) {
		this.trCmdId = trCmdId;
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
	public String getShowMsg() {
		return showMsg;
	}
	public void setShowMsg(String showMsg) {
		this.showMsg = showMsg;
	}
}
