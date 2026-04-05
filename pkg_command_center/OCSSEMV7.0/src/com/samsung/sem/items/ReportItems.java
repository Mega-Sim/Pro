package com.samsung.sem.items;

/**
 * ReportItems Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date 2011. 6. 21.
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

public class ReportItems {
	String commandId;
	String vehicleId;
	String carrierId;
	String carrierLoc;
	String sourcePort;
	String destPort;
	String transferPort;
	int alarmId;
	int vehicleState;
	int priority;
	int replace;
	int resultCode;
	// STBC
	int idReadStatus;
	int stbState;
	int carrierState;
	// 2012.05.16 by MYM : VehicleType 추가
	// Rail-Down - S1a Foup, Reticle 통합 반송시 사양(IBSEM Spec for Conveyor usage in one OHT) 대응
	int vehicleType;
	// 2012.08.08 by KYK : alarmText추가 (TP VOC)
	String alarmText;
	
	// 2013.10.01 by MYM : SetUnitAlarm, ClearUnitAlarm 추가
	String vehicleCurrentDomain;
	int vehicleCurrentPosition;
	// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
	String foupId;

	// 2020.12.30 by JJW : FloorID 추가 (for TRUpdate)
	String floorId;

	/**
	 * Constructor of ReportItems class.
	 */
	public ReportItems() {
		commandId = null;
		vehicleId = null;
		carrierId = null;
		carrierLoc = null;
		sourcePort = null;
		destPort = null;
		transferPort = null;
		alarmText = null; // 2012.08.08 by KYK : alarmText추가 (TP VOC)

		alarmId = 0;
		vehicleState = 0;
		priority = 0;
		replace = 0;
		resultCode = 0;
		//
		idReadStatus = 0;
		stbState = 0;
		carrierState = 0;
		
		vehicleType = -1; // 2012.01.04 by KYK
		
		// 2013.10.01 by MYM : SetUnitAlarm, ClearUnitAlarm 추가
		vehicleCurrentDomain = null;
		vehicleCurrentPosition = 0;
		// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
		foupId = null;
	}

	public String getCommandId() {
		return commandId;
	}
	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}
	public String getVehicleId() {
		return vehicleId;
	}
	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}
	public String getCarrierId() {
		return carrierId;
	}
	public void setCarrierId(String carrierId) {
		this.carrierId = carrierId;
	}
	public String getCarrierLoc() {
		return carrierLoc;
	}
	public void setCarrierLoc(String carrierLoc) {
		this.carrierLoc = carrierLoc;
	}
	public String getSourcePort() {
		return sourcePort;
	}
	public void setSourcePort(String sourcePort) {
		this.sourcePort = sourcePort;
	}
	public String getDestPort() {
		return destPort;
	}
	public void setDestPort(String destPort) {
		this.destPort = destPort;
	}
	public String getTransferPort() {
		return transferPort;
	}
	public void setTransferPort(String transferPort) {
		this.transferPort = transferPort;
	}
	public int getAlarmId() {
		return alarmId;
	}
	public void setAlarmId(int alarmId) {
		this.alarmId = alarmId;
	}
	public int getVehicleState() {
		return vehicleState;
	}
	public void setVehicleState(int vehicleState) {
		this.vehicleState = vehicleState;
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
	public int getResultCode() {
		return resultCode;
	}
	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}
	public int getIdReadStatus() {
		return idReadStatus;
	}
	public void setIdReadStatus(int idReadStatus) {
		this.idReadStatus = idReadStatus;
	}
	public int getStbState() {
		return stbState;
	}
	public void setStbState(int stbState) {
		this.stbState = stbState;
	}
	public int getCarrierState() {
		return carrierState;
	}
	public void setCarrierState(int carrierState) {
		this.carrierState = carrierState;
	}
	public void setVehicleType(int vehicleType) {
		this.vehicleType = vehicleType;
	}
	public int getVehicleType() {
		return this.vehicleType;
	}
	
	// 2012.08.08 by KYK : alarmText추가 (TP VOC)
	public String getAlarmText() {
		return alarmText;
	}

	public void setAlarmText(String alarmText) {
		this.alarmText = alarmText;
	}

	public String getVehicleCurrentDomain() {
		return vehicleCurrentDomain;
	}

	public void setVehicleCurrentDomain(String vehicleCurrentDomain) {
		this.vehicleCurrentDomain = vehicleCurrentDomain;
	}

	public int getVehicleCurrentPosition() {
		return vehicleCurrentPosition;
	}

	public void setVehicleCurrentPosition(int vehicleCurrentPosition) {
		this.vehicleCurrentPosition = vehicleCurrentPosition;
	}

	public String getFoupId() {
		return foupId;
	}

	public void setFoupId(String foupId) {
		this.foupId = foupId;
	}

	public String getFloorId() {
		return floorId;
	}

	public void setFloorId(String floorId) {
		this.floorId = floorId;
	}
	
}
