package com.samsung.ocs.operation.model;

/**
 * VehicleCommData Class, OCS 3.0 for Unified FAB
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

public class VehicleCommData {
	protected int prevCmd;
	protected int currCmd;
	protected int nextCmd;
	protected int errorCode;
	protected int commandId;
	protected double speed;
	protected char state;
	protected char command;
	protected char vehicleMode;
	protected String currNode;
	protected String stopNode;
	protected char carrierExist;	
	protected String mapVersion;
	protected String rfData;
	protected int pauseType;
	protected char reply;
	protected boolean receivedReply;
	protected boolean receivedState;
	protected int replyErrorCode;
	
	// 2012.06.05 by PMM
	protected int aPSignal;
	protected String aPMacAddress;
	
	// 2015.11.23 by KBS : Patrol VHL 기능 개발
	protected char patrolStatus = '0';
	protected char temperatureLevel = '0';
	
	// OHT7.0
	protected int vehicleType;
	protected int currNodeOffset;
	protected String currStationId;
	protected String stopStationId;
	protected int stopStationOffset;
	protected String hidData;
	protected String inputData;
	protected String outputData;	
	protected int origin;	
	protected int steerPosition;
	protected int motorDrvFPosition;
	protected int motorHoistPosition;
	protected int motorShiftPosition;
	protected int motorRotate;
	protected String stationMapVersion;
	protected String teachingMapVersion;
	protected int carrierType;

	// 2016.08.16 by KBS : Unload/Load 보고 시점 개선
	protected int transStatus;
	protected int reserved;

	/**
	 * Constructor of VehicleCommData class.
	 */
	public VehicleCommData() {
		prevCmd = 0;
		currCmd = 0;
		nextCmd = 0;
		errorCode = 0;
		commandId = 0;
		speed = 0;
		
		command = ' ';
		vehicleMode = ' ';
		state = ' ';
		currNode = "";
		stopNode = "";
		carrierExist = '0';		//'0': Carrier Not Existed, '1': CarrierExisted
		mapVersion = "";
		rfData = "";
		pauseType = 0;
		
		reply = ' ';
		
		// 2012.06.05 by PMM
		aPSignal = 0;
		aPMacAddress = "";
		
		patrolStatus = '0';		//'0': Default, '1': Patrol
		temperatureLevel = '0';		//'0': Default, '1': Warning
		
		// 2013.07.30 by KYK
		vehicleType = 0;
		currNodeOffset = 0;
		currStationId = "";
		stopStationId = "";
		stopStationOffset = 0;
		hidData = "";
		inputData = "";
		outputData = "";
		origin = 0;
		steerPosition = 0;
		motorDrvFPosition = 0;
		motorHoistPosition = 0;
		motorShiftPosition = 0;
		motorRotate = 0;
		
		transStatus = 0;
		reserved = 0;
	}
	
	public int getCommandId() {
		return commandId;
	}

	public void setCommandId(int commandId) {
		this.commandId = commandId;
	}

	public char getCommand() {
		return command;
	}

	public void setCommand(char command) {
		this.command = command;
	}

	public char getReply() {
		return reply;
	}

	public void setReply(char reply) {
		this.reply = reply;
		this.receivedReply = true;
	}

	public int getPrevCmd() {
		return prevCmd;
	}

	public void setPrevCmd(int prevCmd) {
		this.prevCmd = prevCmd;
	}

	public int getCurrCmd() {
		return currCmd;
	}

	public void setCurrCmd(int currCmd) {
		this.currCmd = currCmd;
	}

	public int getNextCmd() {
		return nextCmd;
	}

	public void setNextCmd(int nextCmd) {
		this.nextCmd = nextCmd;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
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

		// 2012.11.01 by MYM : Status 수신 Flag 설정을 모든 Status 정보가 Parsing이 완료된 후에 설정하도록 변경
		//                     VehicleComm.java의 processReceivedMessage로 이동
		// 2012.03.06 by PMM
		// setReceivedState(); 로 해당 변수 수정하는 곳 추적하기 편하도록
//		this.receivedState = true;
//		setReceivedState(true);
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
	
	public char getCarrierExist() {
		return carrierExist;
	}
	
	public void setCarrierExist(char carrierExsit) {
		this.carrierExist = carrierExsit;
	}
	
	public double getSpeed() {
		return speed;
	}
	
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	public String getMapVersion() {
		return mapVersion;
	}
	
	public void setMapVersion(String mapVersion) {
		this.mapVersion = mapVersion;
	}
	
	public String getRfData() {
		return rfData;
	}
	
	public void setRfData(String rfData) {
		this.rfData = rfData;
	}
	
	public int getPauseType() {
		return pauseType;
	}
	
	// PauseType
	// 0: 정상 주행, 1: 대차 센서에 의한 일시 정지, 2: Pause 명령에 의한 일시 정지
	public void setPauseType(int pauseType) {
		this.pauseType = pauseType;
	}
	
	public boolean isReceivedReply() {
		return receivedReply;
	}

	public void setReceivedReply(boolean receivedCmdRep) {
		this.receivedReply = receivedCmdRep;
	}

	public boolean isReceivedState() {
		return receivedState;
	}

	public void setReceivedState(boolean receivedStatus) {
		this.receivedState = receivedStatus;
	}

	// 2012.06.05 by PMM
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

	public int getReplyErrorCode() {
		return replyErrorCode;
	}

	public void setReplyErrorCode(int replyErrorCode) {
		this.replyErrorCode = replyErrorCode;
	}
	
	public int getCurrNodeOffset() {
		return currNodeOffset;
	}

	public void setCurrNodeOffset(int currNodeOffset) {
		this.currNodeOffset = currNodeOffset;
	}

	public String getCurrStationId() {
		return currStationId;
	}

	public void setCurrStationId(String currStationId) {
		this.currStationId = currStationId;
	}

	public String getStopStationId() {
		return stopStationId;
	}

	public void setStopStationId(String stopStationId) {
		this.stopStationId = stopStationId;
	}

	public long getStopStationOffset() {
		return stopStationOffset;
	}

	public void setStopStationOffset(int stopStationOffset) {
		this.stopStationOffset = stopStationOffset;
	}

	public String getHidData() {
		return hidData;
	}

	public void setHidData(String hidData) {
		this.hidData = hidData;
	}

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	public int getSteerPosition() {
		return steerPosition;
	}

	public void setSteerPosition(int steerPosition) {
		this.steerPosition = steerPosition;
	}

	// 2016.06.01 by KBS : CommV1의 SteeringPosition 처리 함수
	public void setSteerPosition(char steerPosition) {
		if (steerPosition == 'L') {
			this.steerPosition = 1;
		} else if (steerPosition == 'R') {
			this.steerPosition = 2;
		} else {
			this.steerPosition = 5;
		}
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

	public int getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(int vehicleType) {
		this.vehicleType = vehicleType;
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
