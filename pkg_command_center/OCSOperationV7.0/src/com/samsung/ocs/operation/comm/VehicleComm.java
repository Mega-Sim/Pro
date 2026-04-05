package com.samsung.ocs.operation.comm;

import java.net.Socket;

import org.apache.log4j.Logger;

import com.samsung.ocs.operation.constant.OperationConstant.COMMAND_TYPE;
import com.samsung.ocs.operation.model.VehicleCommCommand;
import com.samsung.ocs.operation.model.VehicleCommData;
import com.samsung.ocs.operation.thread.VehicleCommSocketCheckThread;
import com.samsung.ocs.operation.thread.VehicleCommSocketReadThread;

/**
 * VehicleComm Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 3. 18.
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
public abstract class VehicleComm {
	protected boolean sendFlag;
	protected boolean isBidirectionalSTB;
	
	protected int targetPort;
	protected int replyTimeoutCount;
	protected int reconnectionCount;
	
	protected long socketReconnectionTimeout;
	protected long lastReceivedTime;
	protected long sendRequestedTime;
	protected long socketDisconnectedTime;
//	protected long lastConnectedTime;
	protected long lastCommLogUpdatedTime;
	protected COMMAND_TYPE lastSentCommand;
	
	  // 2014.06.21 by MYM : [Commfail 체크 개선]
	protected long socketCloseCheckTime; // Vehicle로 상태 미수신시 Socket을 Close 후 재접속을 하기 위한 체크 시간
	protected long commFailCheckTime;		// Vehicle과 통신이 안되는 상태에서 통신 Fail로 간주하는 시간

	protected String targetName;
	protected String targetIPAddress;
	protected Socket clientSocket;
	protected VehicleCommData vehicleCommData;
	
	protected VehicleCommSocketReadThread socketReadThread;
	protected VehicleCommSocketCheckThread socketCheckThread;
	
	protected static final int REPLY_TIMEOUT = 2000;
	
	protected static final String VEHICLE_COMM_TRACE = "VehicleCommDebug";
	protected static final String VEHICLE_COMM_EXCEPTION = "VehicleCommException";
	protected static final String VEHICLETARGETINFO = "VehicleCommTargetInfo";
	protected static final Logger log = Logger.getLogger(VEHICLE_COMM_TRACE);
	protected static final Logger exceptionLog = Logger.getLogger(VEHICLE_COMM_EXCEPTION);
	protected static final Logger targetInfoLog = Logger.getLogger(VEHICLETARGETINFO);

	public VehicleComm() {
		targetName = "";		
		targetIPAddress = "127.0.0.1";
		clientSocket = null;
		// 2014.06.21 by MYM : [Commfail 체크 개선] 초기값 설정
		socketReconnectionTimeout = 5000; // 5초마다 재접속 시도함.
		socketCloseCheckTime = 5000; // Vehicle 상태 미수신이 5초 초과시 재접속 시도함.
		commFailCheckTime = 10000;   // 통신이 안되고 있는 시간이 10초 초과시 통신 Fail로 간주함.
	}
	
	protected void initialize() {
		sendFlag = false;
		replyTimeoutCount = 0;
		reconnectionCount = 0;
		lastReceivedTime = System.currentTimeMillis();
		sendRequestedTime = System.currentTimeMillis();
		socketDisconnectedTime = System.currentTimeMillis() - socketReconnectionTimeout;
//		lastConnectedTime = System.currentTimeMillis();
		lastCommLogUpdatedTime = System.currentTimeMillis();
		lastSentCommand = COMMAND_TYPE.UNKNOWN;
	}
	
	public void startVehicleComm() {
		if (socketCheckThread == null) {
			initialize();
			
			socketCheckThread = new VehicleCommSocketCheckThread(this);
			socketCheckThread.start();
		}
	}

	public void stopVehicleComm() {
		if (socketCheckThread != null) {
			socketCheckThread.stopThread();
			socketCheckThread = null;
		}
	}
	
	public void setTargetInfo(String targetName, String targetIPAddress, int targetPort) {
		this.targetName = targetName;
		this.targetIPAddress = targetIPAddress;
		this.targetPort = targetPort;
		
		StringBuilder logMessage = new StringBuilder();
		logMessage.append("Name:").append(targetName).append(", ");
		logMessage.append("IPAddress:").append(targetIPAddress).append(", ");
		logMessage.append("Port:").append(targetPort);
		
		targetInfoLog.debug(logMessage.toString());
	}
	
	public void setBidirectionalSTB(boolean isBidirectionalSTB) {
		this.isBidirectionalSTB = isBidirectionalSTB;
	}
	
	/**
	 * Is CommFail?
	 * 
	 * @param commFailCheckTime
	 * @return
	 */
	//	public boolean isCommFail(long commFailCheckTime) {
	//	if (clientSocket == null) {
	//		return true;
	//	} else if (clientSocket.isConnected() == false) {
	//		if (System.currentTimeMillis() - lastConnectedTime > commFailCheckTime) {
	//			return true;
	//		}
	//	}
	//	
	//	return false;
	//}
	//
	public boolean isCommFail() {
		// 2014.06.21 by MYM : [Commfail 체크 개선] 통신 Fail로 간주하는 조건 변경
		// 상태 미수신시 Socket을 Close하고 재접속하는 동안에도 commFailCheckTime 초과시에만 통신 Fail로 간주함.
		if (clientSocket == null || clientSocket.isConnected() == false) {
			if (System.currentTimeMillis() - lastReceivedTime > commFailCheckTime) {
				return true;
			}
		}

		return false;
	}

	public VehicleCommData getVehicleCommData() {
		return vehicleCommData;
	}
	
	/**
	 * Get Last Sent Command
	 * 
	 * @return
	 */
	public COMMAND_TYPE getLastSentCommand() {
		return lastSentCommand;
	}

	/**
	 * Set Last Sent Command
	 * 
	 * @param lastSentCommand
	 */
	public void setLastSentCommand(COMMAND_TYPE lastSentCommand) {
		this.lastSentCommand = lastSentCommand;
	}

	/**
	 * Trace VehicleComm
	 */
	protected void traceVehicleComm(String message) {
		log.debug(String.format("%s> %s", targetName, message));
	}
	
	// 2012.02.20 by PMM
	protected void traceVehicleComm(String message, Throwable e) {
		log.error(String.format("%s> %s ", targetName, message), e);
	}
	
	/**
	 * Trace VehicleComm Exception
	 * 
	 * @param message
	 * @param e
	 */
	protected void traceVehicleCommException(String message, Throwable e) {
		exceptionLog.error(String.format("%s> %s ", targetName, message), e);
	}
	
	protected void traceVehicleCommException(String message) {
		exceptionLog.error(String.format("%s> %s ", targetName, message));
	}
	/**
	 * Get Target Name
	 * 
	 * @return
	 */
	public String getTargetName() {
		return targetName;
	}

	/**
	 * Get Target IPAddress
	 * 
	 * @return
	 */
	public String getTargetIPAddress() {
		return targetIPAddress;
	}

	public void setSocketReconnectionTimeout(long socketReconnectionTimeout) {
		this.socketReconnectionTimeout = socketReconnectionTimeout;
	}
	public void setSocketCloseCheckTime(long socketCloseCheckTime) {
		this.socketCloseCheckTime = socketCloseCheckTime;
	}
	
	public void setCommFailCheckTime(long commFailCheckTime) {
		this.commFailCheckTime = commFailCheckTime;
	}	

	public boolean readSocketProcess() {
		return false;
	}
	public boolean checkSocketProcess() {
		return false;
	}
	public void closeClientSocket(String reason) {
	}
	
	abstract protected void openClientSocket();
	abstract public boolean sendGoCommand(VehicleCommCommand command);
	abstract public boolean sendGoMoreCommand(VehicleCommCommand command);
	abstract public boolean sendUnloadCommand(VehicleCommCommand command);
	abstract public boolean sendLoadCommand(VehicleCommCommand command);
	abstract public boolean sendScanCommand(VehicleCommCommand command);
	abstract public boolean sendRouteInfoData(VehicleCommCommand command);
	abstract public boolean sendIntersectionNodes(VehicleCommCommand command);
	abstract public boolean sendRemoveCommand(VehicleCommCommand command);
	abstract public boolean sendMapMakeCommand(VehicleCommCommand command);
	abstract public boolean sendCancelCommand(VehicleCommCommand command);
	abstract public boolean sendPatrolCommand(VehicleCommCommand command);
	abstract public boolean sendPatrolCancelCommand(VehicleCommCommand command);
	abstract public boolean sendIDResetCommand();
	abstract public boolean sendPauseCommand();
	abstract public boolean sendResumeCommand();
	abstract public boolean sendEStopCommand(int type);
	// 2020.05.11 by YSJ (OHT Auto Change)
	abstract public boolean sendAutoCommand();
}
