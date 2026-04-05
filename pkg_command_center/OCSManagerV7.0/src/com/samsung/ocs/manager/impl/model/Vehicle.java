package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;

/**
 * Vehicle Class, OCS 3.0 for Unified FAB
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

public class Vehicle {
	protected String vehicleId;
	protected REQUESTEDTYPE requestedType;
	protected char vehicleMode;
	protected char state;
	protected char carrierExist;
	protected String currNode;
	protected String stopNode;
	protected String targetNode;
	protected long stateChangedTime;
	protected int errorCode;
	protected boolean assignHold;
	protected boolean actionHold;
	protected boolean loadingByPass;
	protected boolean enabled;
	protected String ipAddress;
	protected String requestedData;
	protected double requestedCost;
	protected String locus = "";
	protected String material;
	protected int materialIndex;
	protected String zone;
	protected int zoneIndex;
	protected boolean semiManual;
	protected String reason;
	protected String localGroupId;
	protected double vehicleSpeed;
	protected String mapVersion;
	
	// 2012.06.05 by PMM
	protected int aPSignal;
	protected String aPMacAddress;

	// 2013.02.08 by KYK
	protected String currStation;
	protected String stopStation;
	protected String targetStation;
	protected int currNodeOffset;
	protected boolean isStationDriveAllowed;
	
	// S-OHT Only
	protected char patrolStatus = '0';
	protected char temperatureLevel = '0';	// S-OHT Cap-Bank Temperature Warning Level
	
	// 2013.05.24 by MYM
	protected int vehicleType;
	protected String rfData;
	protected int pauseType = 0;
	protected int steerPosition;
	protected int originInfo;
	protected int motorDrvFPosition;
	protected int motorHoistPosition;
	protected int motorShiftPosition;
	protected int motorRotate;
	protected String hidData;
	protected String inputData;
	protected String outputData;
	protected String stationMapVersion;
	protected String teachingMapVersion;
	protected int carrierType;

	protected int transStatus;

	public String toString() {
		return vehicleId;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public REQUESTEDTYPE getRequestedType() {
		return requestedType;
	}

	public void setRequestedType(REQUESTEDTYPE requestedType) {
		this.requestedType = requestedType;
	}

	public char getVehicleMode() {
		return vehicleMode;
	}

	public void setVehicleMode(char vehicleMode) {
		this.vehicleMode = vehicleMode;
	}

	public char getState() {
		return state;
	}

	public void setState(char state) {
		this.state = state;
	}
	
	public char getCarrierExist() {
		return carrierExist;
	}

	public void setCarrierExist(char carrierExist) {
		if (carrierExist == '1') {
			this.carrierExist = '1';
		} else {
			this.carrierExist = '0';
		}
	}

	public String getCurrNode() {
		return currNode;
	}

	public void setCurrNode(String currNode) {
		this.currNode = currNode;
	}

	public String getStopNode() {
		return stopNode;
	}

	public void setStopNode(String stopNode) {
		this.stopNode = stopNode;
	}

	public String getTargetNode() {
		return targetNode;
	}

	public void setTargetNode(String targetNode) {
		// 2013.09.12 by MYM : 인자(targetNode)가 null이면 targetNode를 stopNode로 업데이트 하도록 조건 추가
		if (targetNode != null && targetNode.length() > 0) {
			this.targetNode = targetNode;
		} else {
			this.targetNode = this.stopNode;
		}
	}

	public long getStateChangedTime() {
		return stateChangedTime;
	}

	public void setStateChangedTime(long stateChangedTime) {
		this.stateChangedTime = stateChangedTime;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public boolean isAssignHold() {
		return assignHold;
	}

	public void setAssignHold(boolean assignHold) {
		this.assignHold = assignHold;
	}

	public boolean isActionHold() {
		return actionHold;
	}

	public void setActionHold(boolean actionHold) {
		this.actionHold = actionHold;
	}

	public boolean isLoadingByPass() {
		return loadingByPass;
	}

	public void setLoadingByPass(boolean loadingByPass) {
		this.loadingByPass = loadingByPass;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getRequestedData() {
		return requestedData;
	}

	public void setRequestedData(String requestedData) {
		this.requestedData = requestedData;
	}

	public double getRequestedCost() {
		return requestedCost;
	}

	public void setRequestedCost(double requestedCost) {
		this.requestedCost = requestedCost;
	}

	public String getLocus() {
		return locus;
	}

	public void setLocus(String locus) {
		this.locus = locus;
	}
	
	public String getMaterial() {
		return material;
	}
	public void setMaterial(String material) {
		this.material = material;
	}
	public int getMaterialIndex() {
		return materialIndex;
	}
	public void setMaterialIndex(int materialIndex) {
		this.materialIndex = materialIndex;
	}

	public String getZone() {
		return zone;
	}
	public void setZone(String zone) {
		this.zone = zone;
	}

	public int getZoneIndex() {
		return zoneIndex;
	}

	public void setZoneIndex(int zoneIndex) {
		this.zoneIndex = zoneIndex;
	}

	public boolean isSemiManual() {
		return semiManual;
	}

	public void setSemiManual(boolean semiManual) {
		this.semiManual = semiManual;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getLocalGroupId() {
		return localGroupId;
	}

	public void setLocalGroupId(String localGroupId) {
		this.localGroupId = localGroupId;
	}

	public double getVehicleSpeed() {
		return vehicleSpeed;
	}

	public void setVehicleSpeed(double vehicleSpeed) {
		this.vehicleSpeed = vehicleSpeed;
	}

	public String getMapVersion() {
		return mapVersion;
	}

	public void setMapVersion(String mapVersion) {
		this.mapVersion = mapVersion;
	}
	
	public String getStationMapVersion() {
		return stationMapVersion;
	}

	public void setStationMapVersion(String stationMapVersion) {
		this.stationMapVersion = stationMapVersion;
	}

	public String getTeachingMapVersion() {
		return teachingMapVersion;
	}

	public void setTeachingMapVersion(String teachingMapVersion) {
		this.teachingMapVersion = teachingMapVersion;
	}

	public int getAPSignal() {
		return aPSignal;
	}

	public void setAPSignal(int aPSignal) {
		this.aPSignal = aPSignal;
	}

	public String getAPMacAddress() {
		return aPMacAddress;
	}

	public void setAPMacAddress(String aPMacAddress) {
		this.aPMacAddress = aPMacAddress;
	}

	public char getPatrolStatus() {
		return patrolStatus;
	}

	public void setPatrolStatus(char patrolStatus) {
		this.patrolStatus = patrolStatus;
	}

	public char getTemperatureLevel() {
		return temperatureLevel;
	}

	public void setTemperatureLevel(char temperatureLevel) {
		this.temperatureLevel = temperatureLevel;
	}
	
	// 2013.02.08 by KYK
	public String getCurrStation() {
		return currStation;
	}

	public void setCurrStation(String currStation) {
		this.currStation = currStation;
	}

	public String getStopStation() {
		return stopStation;
	}

	public void setStopStation(String stopStation) {
		this.stopStation = stopStation;
	}

	public String getTargetStation() {
		return targetStation;
	}

	public void setTargetStation(String targetStation) {
		this.targetStation = targetStation;
	}

	public int getCurrNodeOffset() {
		return currNodeOffset;
	}

	public void setCurrNodeOffset(int currNodeOffset) {
		this.currNodeOffset = currNodeOffset;
	}

	public boolean isStationDriveAllowed() {
		return isStationDriveAllowed;
	}

	public void setStationDriveAllowed(boolean isStationDriveAllowed) {
		this.isStationDriveAllowed = isStationDriveAllowed;
	}
	
	public void setTarget(String targetNode, String targetStation) {
		setTargetNode(targetNode);
		setTargetStation(targetStation);
	}
	
	public void setStop(String stopNode, String stopStation) {
		setStopNode(stopNode);
		setStopStation(stopStation);
	}

	public String getRfData() {
		return rfData;
	}
	
	public void setRfData(String rfData) {
		if (rfData == null) {
			this.rfData = "";
		} else {
			this.rfData = rfData;
		}
	}
	public int getPauseType() {
		return pauseType;
	}

	public void setPauseType(int pauseType) {
		this.pauseType = pauseType;
	}
	
	public int getSteerPosition() {
		return steerPosition;
	}

	public void setSteerPosition(int steerPosition) {
		this.steerPosition = steerPosition;
	}

	public int getOriginInfo() {
		return originInfo;
	}

	public void setOriginInfo(int originInfo) {
		this.originInfo = originInfo;
	}

	public int getMotorDrvFPosition() {
		return motorDrvFPosition;
	}

	public void setMotorDrvFPosition(int motorDrvFPosition) {
		this.motorDrvFPosition = motorDrvFPosition;
	}

	public int getMotorHoistPosition() {
		return motorHoistPosition;
	}

	public void setMotorHoistPosition(int motorHoistPosition) {
		this.motorHoistPosition = motorHoistPosition;
	}

	public int getMotorShiftPosition() {
		return motorShiftPosition;
	}

	public void setMotorShiftPosition(int motorShiftPosition) {
		this.motorShiftPosition = motorShiftPosition;
	}

	public int getMotorRotate() {
		return motorRotate;
	}

	public void setMotorRotate(int motorRotate) {
		this.motorRotate = motorRotate;
	}
	
	public String getHidData() {
		return hidData;
	}

	public void setHidData(String hidData) {
		this.hidData = hidData;
	}

	public String getInputData() {
		return inputData;
	}

	public void setInputData(String inputData) {
		this.inputData = inputData;
	}

	public String getOutputData() {
		return outputData;
	}

	public void setOutputData(String outputData) {
		this.outputData = outputData;
	}
	
	public int getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(int vehicleType) {
		this.vehicleType = vehicleType;
	}

	public int getCarrierType() {
		return carrierType;
	}

	public void setCarrierType(int carrierType) {
		this.carrierType = carrierType;
	}

	public int getTransStatus() {
		return transStatus;
	}

	public void setTransStatus(int transStatus) {
		this.transStatus = transStatus;
	}
	
}
