package com.samsung.ocs.operation.model;

import java.util.ArrayList;

/**
 * VehicleCommCommand Class, OCS 3.0 for Unified FAB
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

public class VehicleCommCommand {
	protected int commandId;
	protected String nodeId;
	protected char errorReportOption;
	protected char eqOption;
	protected char rfOption;
	protected char commandOption;
	protected char carrierlocDirectionOption;
	protected String routeInfoData;
	
	// 2011.11.30 by PMM
	protected String targetNode;
	protected ArrayList<String> routedIntersectionNodeList;
	
	// 2013.05.24 by MYM
	// OHT7.0
	protected String nextNodeId;
	protected String preSteeringNodeId;
	protected int mapMakeSpeed;
	protected int patrolSpeed;
	protected String stationId;
	protected int stationOffset;
	protected byte stationType; 		// 0x01:StopTag, 0x02:QRTag(Left), 0x03:QRTag(Right)
	protected byte portType;
	protected byte carrierType;
	protected int hoistPosition;
	protected int shiftPosition;
	protected int rotatePosition;	
	protected byte extraOption;			// AutoRecovery(7), RFReader(6), StationSound(5), Oscillation(4), HandDetectEQ(3), Reserved(2~0)
	protected int hoistSpeedLevel;
	protected int shiftSpeedLevel;	
	protected int lineSpeedLevel;
	protected int curveSpeedLevel;
	protected int accelationLevel;	
	protected int pioDirection;			// 0x01:Left, 0x02:Right
	protected int pioTimeLevel;
	protected int lookDownLevel;
	
	// 2014.10.30 by KYK
	protected char carrierPosition;
	private String rfPioId;
	private int rfPioCS;

	// 2015.12.21 by KBS : Patrol VHL 기능 추가
	private int patrolMode;			// 0x03:Clean+Vision, 0x04:Vibration, 0x08:Slope
	
	/**
	 * Constructor of VehicleCommCommand class.
	 */
	public VehicleCommCommand() {
		initialize();
	}
	
	/**
	 * Initialize
	 */
	public void initialize() {
		this.commandId = 0;
		this.nodeId = "";
		this.errorReportOption = ' ';
		this.eqOption = ' ';
		this.rfOption = ' ';
		this.commandOption = ' ';
		this.carrierlocDirectionOption = ' ';
		this.routeInfoData = "";
		
		// 2011.11.30 by PMM
		this.targetNode = "";
		this.routedIntersectionNodeList = new ArrayList<String>();
		
		// 2013.05.24 by MYM
		// OHT7.0
		nextNodeId = "";
		preSteeringNodeId = "";
		mapMakeSpeed = 0;
		patrolSpeed = 0;
		stationId = "";
		stationOffset = 0;
		stationType = 0;		
		portType = 0;		
		hoistPosition = 0;
		shiftPosition = 0;
		rotatePosition = 0;
		hoistSpeedLevel = 0;
		shiftSpeedLevel = 0;
		pioDirection = 0;
		pioTimeLevel = 0;
		lookDownLevel = 0;
		// 2014.10.30 by KYK [DualOHT]
		carrierPosition = '0';
		rfPioId = "";
		rfPioCS = 0;
		patrolMode = 0;
	}

	public int getCommandId() {
		return commandId;
	}

	public void setCommandId(int commandId) {
		this.commandId = commandId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public char getErrorReportOption() {
		return errorReportOption;
	}

	public void setErrorReportOption(char errorReportOption) {
		this.errorReportOption = errorReportOption;
	}

	public char getEqOption() {
		return eqOption;
	}

	public void setEqOption(char eqOption) {
		this.eqOption = eqOption;
	}

	public char getRfOption() {
		return rfOption;
	}

	public void setRfOption(char rfOption) {
		this.rfOption = rfOption;
	}

	public char getCommandOption() {
		return commandOption;
	}

	public void setCommandOption(char commandOption) {
		this.commandOption = commandOption;
	}

	public char getCarrierlocDirectionOption() {
		return carrierlocDirectionOption;
	}

	public void setCarrierlocDirectionOption(char carrierlocDirectionOption) {
		this.carrierlocDirectionOption = carrierlocDirectionOption;
	}

	public String getRouteInfoData() {
		return routeInfoData;
	}

	public void setRouteInfoData(String routeInfoData) {
		this.routeInfoData = routeInfoData;
	}
	
	// 2011.11.30 by PMM
	public String getTargetNode() {
		return targetNode;
	}
	public void setTargetNode(String targetNode) {
		this.targetNode = targetNode;
	}
	public ArrayList<String> getRoutedIntersectionNodeList() {
		return routedIntersectionNodeList;
	}
	public void setRoutedIntersectionNodeList(ArrayList<String> routedIntersectionNodeList) {
		this.routedIntersectionNodeList = routedIntersectionNodeList;
	}

	/**
	 * Make Command Id
	 * 
	 * @param prevCmd
	 * @param currCmd
	 * @param nextCmd
	 */
	public void makeCommandId(int prevCmd, int currCmd, int nextCmd) {
		// CmdID를 1~9사이의 값으로 설정한다.
		if (commandId < 9) { 
			commandId++;
		} else {
			commandId = 1;
		}
		
		if (prevCmd == commandId) {
			if (commandId < 9) {
				commandId++;
			} else {
				commandId = 1;
			}
		}
		
		if (currCmd == commandId) {
			if (commandId < 9) {
				commandId++;
			} else {
				commandId = 1;
			}
		}
		
		if (nextCmd == commandId) {
			if (commandId < 9) {
				commandId++;
			} else {
				commandId = 1;
			}
		}
	}
	
	public String getNextNodeId() {
		return nextNodeId;
	}

	public void setNextNodeId(String nextNodeId) {
		this.nextNodeId = nextNodeId;
	}
	
	public String getPreSteeringNodeId() {
		return preSteeringNodeId;
	}

	public void setPreSteeringNodeId(String preSteeringNodeId) {
		this.preSteeringNodeId = preSteeringNodeId;
	}
	
	public int getMapMakeSpeed() {
		return mapMakeSpeed;
	}

	public void setMapMakeSpeed(int mapMakeSpeed) {
		this.mapMakeSpeed = mapMakeSpeed;
	}

	public int getPatrolSpeed() {
		return patrolSpeed;
	}

	public void setPatrolSpeed(int patrolSpeed) {
		this.patrolSpeed = patrolSpeed;
	}

	public void setStationId(String stationId) {
		this.stationId = stationId;
	}
	
	public String getStationId() {
		return stationId;
	}

	public void setStationOffset(int stationOffset) {
		this.stationOffset = stationOffset;
	}
	
	public int getStationOffset() {
		return stationOffset;
	}

	public void setStationType(int stationType) {
		this.stationType = (byte) stationType;
	}
	
	public int getStationType() {
		return stationType;
	}
	
	public void setTPosition(int hoistPosition, int shiftPosition, int rotatePosition) {
		this.hoistPosition = hoistPosition;
		this.shiftPosition = shiftPosition;
		this.rotatePosition = rotatePosition;
	}

	public int getHoistPosition() {
		return hoistPosition;
	}

	public void setHoistPosition(int hoistPosition) {
		this.hoistPosition = hoistPosition;
	}

	public int getShiftPosition() {
		return shiftPosition;
	}

	public void setShiftPosition(int shiftPosition) {
		this.shiftPosition = shiftPosition;
	}

	public int getRotatePosition() {
		return rotatePosition;
	}

	public void setRotatePosition(int rotatePosition) {
		this.rotatePosition = rotatePosition;
	}

	public int getLookDownLevel() {
		return lookDownLevel;
	}

	public void setLookDownLevel(int lookDownLevel) {
		this.lookDownLevel = lookDownLevel;
	}
	
	public byte getExtraOption() {
		return extraOption;
	}

	public void setExtraOption(int extraOption) {
		this.extraOption = (byte) extraOption;
	}

	public int getHoistSpeedLevel() {
		return hoistSpeedLevel;
	}

	public void setHoistSpeedLevel(int hoistSpeedLevel) {
		this.hoistSpeedLevel = hoistSpeedLevel;
	}

	public int getShiftSpeedLevel() {
		return shiftSpeedLevel;
	}

	public void setShiftSpeedLevel(int shiftSpeedLevel) {
		this.shiftSpeedLevel = shiftSpeedLevel;
	}
	
	public int getLineSpeedLevel() {
		return lineSpeedLevel;
	}

	public void setLineSpeedLevel(int lineSpeedLevel) {
		this.lineSpeedLevel = lineSpeedLevel;
	}

	public int getCurveSpeedLevel() {
		return curveSpeedLevel;
	}

	public void setCurveSpeedLevel(int curveSpeedLevel) {
		this.curveSpeedLevel = curveSpeedLevel;
	}

	public int getAccelationLevel() {
		return accelationLevel;
	}

	public void setAccelationLevel(int accelationLevel) {
		this.accelationLevel = accelationLevel;
	}

	public int getPIODirection() {
		return pioDirection;
	}

	public void setPIODirection(int pioDirection) {
		this.pioDirection = pioDirection;
	}

	public int getPioTimeLevel() {
		return pioTimeLevel;
	}

	public void setPioTimeLevel(int pioTimeLevel) {
		this.pioTimeLevel = pioTimeLevel;
	}

	/**
	 * FinalPortType을 얻어오는 함수<br>
	 * 0x00,'N' : None<br>
	 * 0x01,'T' : Stocker 작업<br>
	 * 0x02,'E' : EQ 작업<br>
	 * 0x03,'S' : STB 작업<br>
	 * 0x04,'U' : UTB 작업<br>
	 * 0x05,'L' : Loader 작업<br>
	 * 0x06,'I' : PassDoor In 작업<br>
	 * 0x07,'O' : PassDoor Out 작업<br>
	 * @date 2015. 1. 10.
	 * @return Final Port Type
	 */
	public byte getFinalPortType() {
		byte rtnFinalPortType = 0x00;
		
		/* 2015.01.10 by zzang9un : 기존 코드
		if (eqOption == 'T') {
			return 0x01;
		} else if (eqOption == 'E') {
			return 0x02;
		} else if (eqOption == 'S') {
			return 0x03;
		} else if (eqOption == 'U') {
			return 0x04;
		} else if (eqOption == 'L') {
			return 0x05;
		}
		*/
		
		// 2014.01.10 by zzang9un : PassDoor In, Out 추가
		// 기존 if문을 switch문으로 변경
		switch(eqOption) {
		case 'T':
			rtnFinalPortType = 0x01;
			break;
		case 'E':
			rtnFinalPortType = 0x02;
			break;
		case 'S':
			rtnFinalPortType = 0x03;
			break;
		case 'U':
			rtnFinalPortType = 0x04;
			break;
		case 'L':
			rtnFinalPortType = 0x05;
			break;
		case 'I':
			rtnFinalPortType = 0x06;
			break;
		case 'O':
			rtnFinalPortType = 0x07;
			break;
		default:
			rtnFinalPortType = 0x00;
			break;
		}
		
		return rtnFinalPortType;
	}
	
	public byte getCancelCommandType() {
		if (commandOption == 'C') {
			return 0x01;
		}
		
		return 0x02;
	}
	
	public void setPortType(int portType) {
		this.portType = (byte) portType;
	}

	public byte getPortType() {
		// 0x01 : EQ 작업
		// 0x02 : Multi EQ 작업
		// 0x03 : Stocker 작업
		// 0x04 : Loader 작업
		// 0x05 : Left STB 작업
		// 0x06 : Right STB 작업
		// 0x07 : UTB 작업
		return portType;
	}
	
	public void setCarrierType(int carrierType) {
		this.carrierType = (byte) carrierType;
	}
	
	public byte getCarrierType() {
		// 0x64 : Not Defined(Default)
		// 0x00 : FOUP 0x01 : POD 0x03 : MAC 0x04 : FOSB
		return carrierType;
	}	
	
	public byte getExtraOptionForWorkCommand() {
		// AutoRecovery(7),RFReader(6),StationSound(5),Oscillation(4),HandDetectEQ(3),Reserved(2~0)
		byte option = this.extraOption;
		option |= getAutoRecoveryFlag() << 7;
		option |= getRFReaderFlag() << 6;
		
		return option;
	}
	
	private byte getAutoRecoveryFlag() {
		if (errorReportOption == 'F') {
			return 1;
		} else {
			return 0;
		}
	}
	
	private byte getRFReaderFlag() {
		if (rfOption == '1') {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * 2014.10.30 by KYK [DualOHT] TODO check
	 * @return
	 */
	public byte getCarrierPosition() {
		switch (carrierPosition) {
		case '1':
			return 1;
		case '2':
			return 2;
		default:
			return 0;				
		}
	}

	public void setCarrierPosition(char carrierPosition) {
		this.carrierPosition = carrierPosition;
	}

	public String getRfPioId() {
		return rfPioId;
	}

	public void setRfPioId(String rfPioId) {
		this.rfPioId = rfPioId;
	}

	public int getRfPioCS() {
		return rfPioCS;
	}

	public void setRfPioCS(int rfPioCS) {
		this.rfPioCS = rfPioCS;
	}

	public int getPatrolMode() {
		return patrolMode;
	}

	public void setPatrolMode(int patrolMode) {
		this.patrolMode = patrolMode;
	}
	
}
