package com.samsung.ocs.operation.comm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.operation.model.VehicleCommCommand;
import com.samsung.ocs.operation.model.VehicleCommData;
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
public class VehicleCommV1 extends VehicleComm {
	protected String sendString;
	protected String lastReceivedString;
	protected String currentReceivedString;
	protected String carriageReturnAndLineFeed;
	
	protected char[] characterBuffer;

	protected Vector<String> sendStringList;
	protected PrintWriter writeBuffer;
	protected BufferedReader readBuffer;
//	protected VehicleCommData vehicleCommData;
	
	protected boolean isCarrierTypeUsage = false;	// 18.10.02 by JJW isCarrierTypeUsage
	
	protected static final char LINE_FEED = 0x0A;
	protected static final char CARRIAGE_RETURN = 0x0D;
	protected static final int MAX_BUFFER_SIZE = 1024;
	
	// Node가 6자리인 경우
	protected static final String STATE_MESSAGE_PREFIX = "$t";
	protected static final String MESSAGE_START = "$";
	protected static final String LEFT_BRACKET = "[";
	protected static final String RIGHT_BRACKET = "]";
	protected static final String LEFT_BRACE = "{";
	protected static final String RIGHT_BRACE = "}";
	// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가]
	protected static final String LEFT_ANGLE_BRACKET = "<";
	protected static final String RIGHT_ANGLE_BRACKET = ">";
	protected static final String NEXT_COMMAND_PREFIX = "N";
	protected static final String TIMEOUT_COMMAND_PREFIX = "T";
	protected static final String UNLOAD_COMMAND_PREFIX = "U";
	protected static final String LOAD_COMMAND_PREFIX = "L";
	protected static final String SCAN_COMMAND_PREFIX = "R";
	protected static final String GO_COMMAND_PREFIX = "G";
	protected static final String GOMORE_COMMAND_PREFIX_WITH_START = "$M";
	protected static final String MAPMAKE_COMMAND_PREFIX_WITH_START = "$K";
	private static final String PATROL_COMMAND_PREFIX_WITH_START = "$C";
	private static final String PATROL_CANCEL_COMMAND_PREFIX_WITH_START = "$Z";
	
	protected static final String REMOVE_COMMAND_PREFIX_WITH_START = "$F";
	protected static final String ROUTE_INFO_DATA_PREFIX_WITH_START = "$A";		// 예전의 sendLayoutData
	
	// 2011.11.30 by PMM
	protected static final String INTERSECTION_NODE_LIST_PREFIX_WITH_START = "$Y";
	protected static final int MAX_INTERSECTION_NODE_COUNT = 128;
	
	protected static final String CANCEL_COMMAND_PREFIX_WITH_START = "$X0";
	protected static final String IDRESET_COMMAND = "$I0";
	protected static final String RESUME_COMMAND = "$E0";
	protected static final String PAUSE_COMMAND = "$H0";
	protected static final String ESTOP_COMMAND_0 = "$P0";
	protected static final String ESTOP_COMMAND_1 = "$P1";
	protected static final String ESTOP_COMMAND_2 = "$P2";
	protected static final String ESTOP_COMMAND_3 = "$P3";
	protected static final String ESTOP_COMMAND_4 = "$P4";
	protected static final String ESTOP_COMMAND_5 = "$P5";	

	// 2020.05.11 by YSJ (OHT Auto Change)
	protected static final String AUTO_COMMAND_PREFIX = "$J0";
	
	private VehicleManager vehicleManager = null;	// 2021.03.10 by JDH : PatrolStatus update


//	public VehicleCommV1() {
//		super();
//		
//		sendStringList = new Vector<String>();
//		CharArrayWriter arrayWriter = new CharArrayWriter(2);
//		arrayWriter.write(CARRIAGE_RETURN);
//		arrayWriter.write(LINE_FEED);		
//		carriageReturnAndLineFeed = arrayWriter.toString();
//		characterBuffer = new char[MAX_BUFFER_SIZE];
//		vehicleCommData = new VehicleCommData();
//		
//		initialize();
//	}
	
	// 2018.09.28 by JJW : Carrier Type 기능 사용
	public VehicleCommV1(boolean isCarrierTypeUsage) {
		super();
		
		sendStringList = new Vector<String>();
		CharArrayWriter arrayWriter = new CharArrayWriter(2);
		arrayWriter.write(CARRIAGE_RETURN);
		arrayWriter.write(LINE_FEED);		
		carriageReturnAndLineFeed = arrayWriter.toString();
		characterBuffer = new char[MAX_BUFFER_SIZE];
		vehicleCommData = new VehicleCommData();
		
		this.isCarrierTypeUsage = isCarrierTypeUsage;	// 18.10.02 by JJW isCarrierTypeUsage
		vehicleManager = VehicleManager.getInstance(null, null, false, false, 0);	// 2021.03.10 by JDH : PatrolStatus update
		
		initialize();
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		
		sendStringList.clear();
		sendString = "";
		lastReceivedString = "";
		currentReceivedString = "";
	}
	
	@Override
	protected void openClientSocket() {
		long startTime = System.currentTimeMillis();
		try {
			traceVehicleComm("openClientSocket: Try to Connect to " + targetName + " ClientSocketOpen : " + targetPort);
			lastReceivedTime = System.currentTimeMillis();

			clientSocket = new Socket(targetIPAddress, targetPort);
			readBuffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			writeBuffer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));
			//	isActive = true;
			// 2014.06.21 by MYM : [Commfail 체크 개선] reconnectionCount, lastReceivedTime 초기화
			// 0은 초기 Vehicle 투입시 통신 시도할 때, 투입 이후 통신이 끊겼다가 다시 접속하는 경우는 1부터 Count함.
			reconnectionCount = 1;
			lastReceivedTime = System.currentTimeMillis();			

			currentReceivedString = "";
			traceVehicleComm("openClientSocket: Client Socket Connected...");

			socketReadThread = new VehicleCommSocketReadThread(this);
			socketReadThread.start();
		} catch (Exception e) {
			socketDisconnectedTime = startTime;
			//	isActive = false;

			// 2012.02.20 by PMM
			// Vehicle CommFail 관련 Exception은 VehicleCommDebug Log로 이전.
			//	traceVehicleCommException("openClientSocket()", e);
			traceVehicleComm("openClientSocket()", e);
		}
}
	
	@Override
	public void closeClientSocket(String reason) {
		if (clientSocket == null) {
			traceVehicleComm("closeClientSocket Exception: m_ClientSocket is null (" + reason + ")");
			return;
		}
		// 2014.06.21 by MYM : [Commfail 체크 개선] 주석 처리
		// socket을 close 해도 통신이 안되는 시간이 commFailCheckTime이 초과시에만 통신 Fail로 설정함.
//		vehicleCommData.setErrorCode(OcsConstant.COMMUNICATION_FAIL);
		
		// 2012.03.06 by PMM
//		vehicleCommData.setReceivedState(true);
		vehicleCommData.setReceivedState(false);
		
		try {
			socketDisconnectedTime = System.currentTimeMillis() - socketReconnectionTimeout;
//			isActive = false;
			sendFlag = false;
			lastReceivedString = "";
			sendStringList.clear();
			
			if (socketReadThread != null) {
				socketReadThread.stopThread();
				socketReadThread = null;
			}
			
			writeBuffer.close();
			writeBuffer = null;
			
			if (readBuffer != null) {
				try {
					readBuffer.close();
				} catch (IOException e) {}
				readBuffer = null;
			}
			
			if (clientSocket != null) {
				try {
					clientSocket.close();
				} catch (IOException e) {}
				clientSocket = null;
			}
			
			traceVehicleComm("closeClientSocket: Client Socket Disconnected. (" + reason + ")");
		} catch (Exception e) {
			// 2012.02.20 by PMM
			// Vehicle CommFail 관련 Exception은 VehicleCommDebug Log로 이전.
//			traceVehicleCommException("closeClientSocket()", e);
			traceVehicleComm("closeClientSocket()", e);
		} finally {
			clientSocket = null;
			readBuffer = null;
			writeBuffer = null;
		}
	}

	@Override
	public boolean checkSocketProcess() {
		StringBuilder logMessage = new StringBuilder();
		
//	if ((clientSocket == null) || (isActive == false)) {
	if (clientSocket == null) {
		if (Math.abs(System.currentTimeMillis() - socketDisconnectedTime) > socketReconnectionTimeout) {
			logMessage.append("Retry Connect[").append(reconnectionCount++).append("]...");
			traceVehicleComm("checkSocketProcess: " + logMessage.toString());
			openClientSocket();
			sendFlag = false;
//			socketDisconnectedTime = System.currentTimeMillis();
		}
		
		// 2014.06.21 by MYM : [Commfail 체크 개선] 통신 Fail로 간주된 경우 ErrorCode 업데이트 및 로그 기록.  
		if (isCommFail() && vehicleCommData.getErrorCode() != OcsConstant.COMMUNICATION_FAIL) {
			vehicleCommData.setErrorCode(OcsConstant.COMMUNICATION_FAIL);
			traceVehicleComm("Update Vehicle Communication Fail.");
		}
	} else {
//		if (clientSocket.isConnected()) {
//			lastConnectedTime = System.currentTimeMillis();
//		}
		
		if (vehicleCommData.getVehicleMode() == 'A'
				|| (vehicleCommData.getVehicleMode() == 'M' && sendStringList.indexOf(AUTO_COMMAND_PREFIX) > -1)) {
			if (sendFlag == false) {
				if (sendStringList.size() > 0) {
					sendFlag = true;
					sendString = sendStringList.get(0);
					sendMessage(sendString);
					sendRequestedTime = System.currentTimeMillis();
				}
			} else if (isTimeoutCheckUnnecessary(sendString)) {
				// 2011.11.30 by PMM
				sendFlag = false;
				sendStringList.remove(sendString);
				
			} else if (Math.abs(System.currentTimeMillis() - sendRequestedTime) > REPLY_TIMEOUT
					&& (sendString.indexOf(AUTO_COMMAND_PREFIX) > -1) == false) { // AutoCommand Ack nod-Check 
				String commandTimeoutMessage = makeTimeoutString(sendString);					
				logMessage.append("RCV> ");
				logMessage.append(commandTimeoutMessage);
				logMessage.append(" Timeout by Operation ");
				traceVehicleComm(logMessage.toString());					
				processReceivedMessage(targetName, commandTimeoutMessage);
				sendRequestedTime = System.currentTimeMillis();
				
				if (replyTimeoutCount++ > 20) {
					replyTimeoutCount = 0;
					closeClientSocket("ReplyTimeout count over 20");
//					socketDisconnectedTime = System.currentTimeMillis() - REPLY_TIMEOUT;
				}
			}
		} else {
			sendFlag = false;
			sendStringList.clear();
//			while (sendStringList.size() > 0) {
//				sendStringList.removeElementAt(0);
//			}
		}
		
		// 2014.06.21 by MYM : [Commfail 체크 개선] 상태 미수신 체크는 별도의 파라미터(socketCloseCheckTime)로 확인
		// socketReconnectionTimeout 파라미터는 재접속 Interval로 사용함.
//		if (Math.abs(System.currentTimeMillis() - lastReceivedTime) > socketReconnectionTimeout) {
//			closeClientSocket("Vehicle state has not received during " + socketReconnectionTimeout/1000 + "sec");
		if (Math.abs(System.currentTimeMillis() - lastReceivedTime) > socketCloseCheckTime) {
			closeClientSocket("Vehicle state has not received during " + socketCloseCheckTime/1000 + "sec");
//			lastReceivedTime = System.currentTimeMillis();
//			socketDisconnectedTime = System.currentTimeMillis() - socketReconnectionTimeout;
		}
	}
	
	return true;
}

	@Override
	public boolean readSocketProcess() {
		int receivedStringSize;
		String receivedString;
		try {
			// 2012.03.06 by PMM
			// 13:31:14:569 OHT70> closeClientSocket: Client Socket Disconnected. (Vehicle state has not received during 15sec)
			// 13:31:14:569 OHT70> closeClientSocket Exception: m_ClientSocket is null (ReadSocketProcess exception)
			// readBuffer에 대한 Null Check가 없는 경우, readSocketProcess Exception -> closeClientSocket -> readSocketProcess Exception ...
			if (readBuffer != null) {
				receivedStringSize = readBuffer.read(characterBuffer);
				if (receivedStringSize > 0) {
					if (receivedStringSize < MAX_BUFFER_SIZE) {
						receivedString = new String(characterBuffer, 0, receivedStringSize);
					} else {
						receivedString = new String(characterBuffer, 0, MAX_BUFFER_SIZE);
					}
					
					readSocket(receivedString);
				}
			} else {
				traceVehicleComm("ReadSocketThread() - readBuffer is null.");
			}
		} catch (Exception e) {
			closeClientSocket("ReadSocketProcess exception");
			
			// 2012.02.20 by PMM
			// Vehicle CommFail 관련 Exception은 VehicleCommDebug Log로 이전.
//			traceVehicleCommException("ReadSocketThread()", e);
			traceVehicleComm("ReadSocketThread()", e);
		}
		
		return true;
	}
	
	protected void readSocket(String buffer) {
		int nPos = 0;
		String bufferString = "";
		int i;

		try{
			currentReceivedString += buffer;
		} catch (Exception e) {
			// 2012.02.20 by PMM
			// Vehicle CommFail 관련 Exception은 VehicleCommDebug Log로 이전.
//			traceVehicleCommException("readSocket()", e);
			traceVehicleComm("readSocket()", e);
		}

		Vector<String> receivedList = new Vector<String>();
		lastReceivedTime = System.currentTimeMillis();
		replyTimeoutCount = 0;

		while (true) {
			// "$"문자열 이후에 "CRLF"문자열을 포함한 정상적인 문자열이 아닌 경우에 루프 탈출
			nPos = currentReceivedString.indexOf(MESSAGE_START);
			if (nPos == -1) {
				break;
			}
			currentReceivedString = currentReceivedString.substring(nPos);
			nPos = currentReceivedString.indexOf(carriageReturnAndLineFeed);
			if (nPos == -1) {
				break;
			}

			// 정상적으로 수신된 메시지가 있는 경우에 메시지 처리
			bufferString = currentReceivedString.substring(0, nPos);
			currentReceivedString = currentReceivedString.substring(nPos + 2);

			if (bufferString.indexOf(STATE_MESSAGE_PREFIX) == -1) {
				// 명령에 대한 응답이 있는 경우에는 이전의 상태 보고는 모두 삭제
				receivedList.clear();
			} else {
				// 상태 확인 경우에는 이전의 상태 보고는 모두 삭제
				for (i = receivedList.size()-1; i >= 0; i--) {
					String tempReceivedData = receivedList.get(i);
					if (tempReceivedData.indexOf(STATE_MESSAGE_PREFIX) >= 0) {
						receivedList.remove(i);
					}
				}
			}
			receivedList.add(bufferString);
		}

		// 2012.03.06 by PMM
		// try {} catch(){}를 for loop 외부로
		try {
			for (i = 0; i < receivedList.size(); i++) {
				// 중복되지 않은 메시지인 경우 수신정보 처리 또는 중복된 메시지인 경우에는 일정시간(5초) 간격으로 처리
				bufferString = receivedList.get(i);
				if ((lastReceivedString.equals(bufferString) == false) ||
						(Math.abs(System.currentTimeMillis() - lastCommLogUpdatedTime) > 5000)) {
					traceVehicleComm("RCV> " + bufferString);
					lastCommLogUpdatedTime = System.currentTimeMillis();
					
					processReceivedMessage(targetName, bufferString);
				}
			}
		} catch (Exception e) {
			traceVehicleCommException("readSocket()", e);
		}
	}
	
	/**
	 * Process Received Message
	 * 
	 * @param targetName
	 * @param message
	 */
	public void processReceivedMessage(String targetName, String message) {
		try {
			//  OHT100> RCV> $tAI1101071000000[1,306,298,7,0.00,0.00,0,004505]{201108191650,201108162132,000000000000,000000000000,000000000000,201108031837}
			//	OHTG202> RCV> $tAG0190430560000[1,314,301,14,3878100.00,3.31,0,018065,L,0,00:13:a6:23:a0:81]
			if (STATE_MESSAGE_PREFIX.equals(message.substring(0, 2))) {
				vehicleCommData.setVehicleMode(message.substring(2, 3).charAt(0));
				vehicleCommData.setState(message.substring(3, 4).charAt(0));
				vehicleCommData.setCurrNode(getNodeData(message.substring(4, 10)));
				vehicleCommData.setCarrierExist(message.substring(10, 11).charAt(0));
				vehicleCommData.setPrevCmd(Integer.parseInt(message.substring(11, 12)));
				vehicleCommData.setCurrCmd(Integer.parseInt(message.substring(12, 13)));
				vehicleCommData.setNextCmd(Integer.parseInt(message.substring(13, 14)));
				vehicleCommData.setErrorCode(Integer.parseInt(message.substring(14, 17)));

				// profile
				if (message.indexOf(LEFT_BRACKET) >= 0 && message.indexOf(RIGHT_BRACKET) >= 0) {
					vehicleCommData.setRfData(message.substring(17, message.indexOf(LEFT_BRACKET)));

					String[] profileList = message.substring(message.indexOf(LEFT_BRACKET) + 1, message.indexOf(RIGHT_BRACKET)).split(",");
					// Data1: Is Origin Positioned (OHT 이적재축 원점 위치 여부 ,	1 : Hoist, Shift, Rotate축이 모두 원점에 존재, 0 : 하나라도 원점이 아닐때   => Local Loader 적용시 필요) 
					// Data2: HID Input Voltage (HID 2차측 입력전압) 
					// Data3: HID Output Voltage (HID 2차측 출력전압)
					// Data4: HID Output Electric Current (HID 2차측 전류)
					// Data5: Front Driving Axis Encoder Value (OHT 주행 축 Encoder값 [mm])

					// Data6: Vehicle Speed
					if (profileList.length >= 6) {
						// 2012.07.13 by MYM : Vehicle에서 올려준 값 [m/s]
						vehicleCommData.setSpeed(Double.parseDouble(profileList[5]));
					}
					// Data7: PauseType	0: 정상 주행, 1: 대차 센서에 의한 일시 정지, 2: Pause 명령에 의한 일시 정지
					//					1,2인 경우, 무언정지 알람 발생 X -> 일정 시간 초과 시 무언정지 발생 
					if (profileList.length >= 7) {
						vehicleCommData.setPauseType(Integer.parseInt(profileList[6]));
					}
					// Data8: StopNode
					if (profileList.length >= 8) {
						vehicleCommData.setStopNode(getNodeData(profileList[7]));
					} else {
						vehicleCommData.setStopNode("0");
					}

					// Data9: SteerPosition [L, N, R]
					if (profileList.length >= 9) {
						vehicleCommData.setSteerPosition(profileList[8].charAt(0));
					} else {
						vehicleCommData.setSteerPosition(0);
					}

					// Data10: AP Signal Quality Level [0 ~ 9]
					if (profileList.length >= 10) {
						vehicleCommData.setAPSignal(Integer.parseInt(profileList[9]));
					} else {
						vehicleCommData.setAPSignal(0);
					}
					// Data11: APMacAddress
					if (profileList.length >= 11) {
						vehicleCommData.setAPMacAddress(profileList[10]);
					} else {
						vehicleCommData.setAPMacAddress("");
					}
					
					// 2018.09.28 by JJW : Carrier Type 기능 사용
					if(isCarrierTypeUsage){
						// Data12: Carrier Type
						if (profileList.length >= 12) {
							vehicleCommData.setCarrierType(makeCarrierTypeInteger(profileList[11]));
						} else {
							vehicleCommData.setCarrierType(0x64);
						}
						
						// Data13: TransStatus
						if (profileList.length >= 13) {
							vehicleCommData.setTransStatus(Integer.parseInt(profileList[12]));
						} else {
							vehicleCommData.setTransStatus(1);
						}

						// Data14: PatrolStatus
						if (profileList.length >= 14) {
							// 2021.03.10 by JDH : PatrolStatus update
//							vehicleCommData.setPatrolStatus(profileList[13].charAt(0));
							if(isPatrol(targetName)){
								vehicleCommData.setPatrolStatus(profileList[13].charAt(0));
							}
							else {
								vehicleCommData.setPatrolStatus('0');
							}
						} else {
							vehicleCommData.setPatrolStatus('0');
						}
						
						// Data15: Cap-Bank Temperature Warning Level
						if (profileList.length >= 15) {
							vehicleCommData.setTemperatureLevel(profileList[14].charAt(0));
						} else {
							vehicleCommData.setTemperatureLevel('0');
						}
					} else {
						// Data12: PatrolStatus
						if (profileList.length >= 12) {
							// 2021.03.10 by JDH : PatrolStatus update
//							vehicleCommData.setPatrolStatus(profileList[11].charAt(0));
							if(isPatrol(targetName)){
								vehicleCommData.setPatrolStatus(profileList[11].charAt(0));
							}
							else {
								vehicleCommData.setPatrolStatus('0');
							}
						} else {
							vehicleCommData.setPatrolStatus('0');
						}

						// Data13: Cap-Bank Temperature Warning Level
						if (profileList.length >= 13) {
							vehicleCommData.setTemperatureLevel(profileList[12].charAt(0));
						} else {
							vehicleCommData.setTemperatureLevel('0');
						}
						// 2017.04.06 by KBS : Unload/Load 보고 시점 개선 (VehicleCommV1 미적용)
						vehicleCommData.setTransStatus(1);
					}
				} else {
					// 2012.11.01 by MYM : "[,,,,...]" 데이터가 없을 경우 RFData는 초기값("")로 설정
					// 배경 : S1A에서 RFData뒤에 찌꺼기 데이터가 붙어서 [가 없어져서 OCS로 들어온 경우가 발생 -> RFData를 DB 업데이트시 Oracle Error 발생 
					//      $tAN0037201300000GYBG3370? ?1,309,301,12,189471.00,0.00,0,003720,L,7,00:3a:98:68:12:2f]
					vehicleCommData.setRfData("");
					vehicleCommData.setSpeed(0);
					vehicleCommData.setPauseType(0);
					vehicleCommData.setStopNode("0");
					vehicleCommData.setSteerPosition(0);
					vehicleCommData.setAPSignal(0);
					vehicleCommData.setAPMacAddress("");
					vehicleCommData.setPatrolStatus('0');
					vehicleCommData.setTemperatureLevel('0');
				}

				// MapVersion 
				if (message.indexOf(LEFT_BRACE) >= 0 && message.indexOf(RIGHT_BRACE) >= 0) {
					vehicleCommData.setMapVersion(parseMapVersion(message));					
				}
				
				// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] : <5+HIDNUM + 5 + Index>
				if (message.indexOf(LEFT_ANGLE_BRACKET) >= 0 && message.indexOf(RIGHT_ANGLE_BRACKET) >= 0) {
					
					String[] qrDataprofileList = message.substring(message.indexOf(LEFT_ANGLE_BRACKET) + 1, message.indexOf(RIGHT_ANGLE_BRACKET)).split(",");
					
					if (qrDataprofileList.length >= 1) {
						vehicleCommData.setCurrStationId(getQrTagData(qrDataprofileList[0]));
					} else {
						vehicleCommData.setCurrStationId("");
					}
					
					if (qrDataprofileList.length >= 2) {
						vehicleCommData.setStopStationId(getQrTagData(qrDataprofileList[1]));
					} else {
						vehicleCommData.setStopStationId("");
					}
				} else {
					vehicleCommData.setCurrStationId("");
					vehicleCommData.setStopStationId("");
				}
				
				// 2017.04.06 by KBS : Unload/Load 보고 시점 개선 (VehicleCommV1 미적용)
//				vehicleCommData.setTransStatus(1); // 2020.05.19 by JJW : isCarrierTypeUsage 사용 안할 시 조건문으로 이동 

				// 마지막 상태값을 저장, 이후 일정시간 이내의 동일메시지 수신시 중복 처리 
				lastReceivedString = message;

				// 2012.11.01 by MYM : Status 수신 Flag 설정을 모든 Status 정보가 Parsing이 완료된 후에 설정하도록 변경
				//                     VehicleCommData.java의 setState()에서의 설정을 여기로 이동
				vehicleCommData.setReceivedState(true);
			} else if (isTimeoutCheckUnnecessary(message.toUpperCase())) {
				; /*NULL*/
			} else {
				vehicleCommData.setCommand(message.substring(1, 2).toLowerCase().charAt(0));
				vehicleCommData.setCommandId(Integer.parseInt(message.substring(2, 3)));
				// 2014.08.13 by MYM : Abnormal CmdReply 확인 (Reply ErrorCode 저장)
				if (message.length() > 4) {
					vehicleCommData.setReplyErrorCode(Integer.parseInt(message.substring(4)));
				}
				vehicleCommData.setReply(message.charAt(3));
				if (sendStringList.size() > 0) {
					if (NEXT_COMMAND_PREFIX.equals(sendString.substring(1, 2))) {
						if (sendString.substring(3, 4).equals(message.substring(2, 3).toUpperCase())) {
							// 2012.04.02 by PMM
//							sendStringList.removeElementAt(0);
							sendStringList.remove(sendString);
							sendRequestedTime = System.currentTimeMillis();
							sendFlag = false;
						}
					} else {
						if (sendString.substring(1, 3).equals(message.substring(1, 3).toUpperCase())) {
//							sendStringList.removeElementAt(0);
							sendStringList.remove(sendString);
							sendRequestedTime = System.currentTimeMillis();
							sendFlag = false;
						}
					}
				} else {
					sendFlag = false;
				}
			}
		} catch (Exception e) {
			traceVehicleCommException("processReceivedMessage() - Message:" + message, e);
		}
	}
	
	public VehicleCommData getVehicleCommData() {
		return vehicleCommData;
	}
	
	/**
	 * Make Timeout String
	 * 
	 * @param sendMessage
	 * @return
	 */
	protected String makeTimeoutString(String sendMessage) {
		StringBuilder message = new StringBuilder();
		message.append(sendMessage.substring(0, 2).toLowerCase());
		
		if (NEXT_COMMAND_PREFIX.equals(sendMessage.substring(1, 2))) {
			message.append(sendMessage.substring(3, 4));
		} else {
			message.append(sendMessage.substring(2, 3));
		}
		
		message.append(TIMEOUT_COMMAND_PREFIX);
		return message.toString();
	}
	
	protected boolean isTimeoutCheckUnnecessary(String message) {
		if (INTERSECTION_NODE_LIST_PREFIX_WITH_START.equals(message.substring(0, 2))) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get Node Data
	 * 
	 * @param node
	 * @return
	 */
	protected String getNodeData(String node) {
		try {
			return String.valueOf(Integer.parseInt(node));
		} catch (Exception e) {
			traceVehicleCommException("getNodeData() - Exception: A node (ID: " + node + ") is not number.", e);
			return "0";
		}
	}
	
	// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
	protected String getQrTagData(String stationID) {
		try {
			return String.valueOf(Integer.parseInt(stationID));
		} catch (Exception e) {
			traceVehicleCommException("getQRStationData() - Exception: Station (ID: " + stationID + ") is not number.", e);
			return "0";
		}
	}
	
	/**
	 * Parse MapVersion
	 */
	protected String parseMapVersion(String additionalProfile) {
		if (additionalProfile != null && additionalProfile.length() > 0) {
			try {
				String[] additionalProfileList = additionalProfile.substring(additionalProfile.indexOf(LEFT_BRACE) + 1, additionalProfile.indexOf(RIGHT_BRACE)).split(",");
				if (additionalProfileList[0].length() <= 16) {
					return additionalProfileList[0];
				} else {
					return additionalProfileList[0].substring(0, 16);
				}
			} catch (Exception e) {
				traceVehicleCommException("getMapVersion() - " + additionalProfile, e);
				return "";
			}
		} else {
			return "";
		}
	}
	
	// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가]
	protected String parseStationInfo(String stationInfo){
		
		if(stationInfo != null & stationInfo.length() > 0){
			
			if(stationInfo.length() <= 6){
				return stationInfo;
			}
			else {
				return stationInfo.substring(0, 16);
			}
		}
		return "";
	}
	
	protected boolean sendMessage(String sendMessage) {
		if (clientSocket == null) {
			closeClientSocket("clientSocket is null");
			return false;
		} else {
			traceVehicleComm("SND> " + sendMessage);
			writeBuffer.println(sendMessage);
			writeBuffer.flush();
			return true;
		}
	}

	@Override
	public boolean sendCancelCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(CANCEL_COMMAND_PREFIX_WITH_START);
		message.append(command.getCommandId());
		message.append(command.getCommandOption());

		return registerSendData(message.toString());
}

	@Override
	public boolean sendPatrolCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}
		
		StringBuilder message = new StringBuilder();
		message.append(PATROL_COMMAND_PREFIX_WITH_START);
		message.append(command.getCommandId());
		message.append(makeNodeString(command.getNodeId()));
		message.append(command.getPatrolMode());
		
		return registerSendData(message.toString());
	}

	@Override
	public boolean sendPatrolCancelCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}
		
		StringBuilder message = new StringBuilder();
		message.append(PATROL_CANCEL_COMMAND_PREFIX_WITH_START);
		message.append("0");
		message.append(makeNodeString(command.getNodeId()));
		message.append(command.getPatrolMode());
		
		return registerSendData(message.toString());
	}

	@Override
	public boolean sendEStopCommand(int type) {
		/*
		 * - $P0 / $p0A
		 * - $P1 / $p1A : OCS 경로에서 벗어난 위치로 OHT가 주행하는 경우 (탈선)
		 * - $P2 / $p2A : 충돌구간에서 타 OHT가 갑자기 인식되는 경우
		 * - $P3 / $p3A : Unload_Assign 상태에서 Carrier가 있는 경우
		 * - $P4 / $p4A : Load_Assign 상태에서 Carrier가 없는 경우
		 * - $P5 / $p5A : STK 포트에서 Unloading 중 RF Data와 반송명령의 Carrier가 틀린 경우
		 */
		if (clientSocket == null) {
			return false;
		}
		switch (type) {
		case 1:
			return registerSendData(ESTOP_COMMAND_1);
		case 2:
			return registerSendData(ESTOP_COMMAND_2);
		case 3:
			return registerSendData(ESTOP_COMMAND_3);
		case 4:
			return registerSendData(ESTOP_COMMAND_4);
		case 5:
			return registerSendData(ESTOP_COMMAND_5);
		case 0:
		default:
			return registerSendData(ESTOP_COMMAND_0);
		}
	}

	@Override
	public boolean sendGoCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(MESSAGE_START);
		if (command.getCommandOption() == 'N')
			message.append(NEXT_COMMAND_PREFIX);
		message.append(GO_COMMAND_PREFIX);
		message.append(command.getCommandId());
		message.append(makeNodeString(command.getNodeId()));
		message.append(command.getEqOption());
		// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
		if (command.getStationId() != null && command.getStationId().length() > 0) {
			// 2012.06.30 by KYK : 꺽쇄안 자리수 맞춰주기 <1234A> -> <001234A>
			String stationIDString = makeStationString(command.getStationId());
			message.append(LEFT_ANGLE_BRACKET).append(stationIDString)
					.append(RIGHT_ANGLE_BRACKET);
		}

		return registerSendData(message.toString());
	}

	@Override
	public boolean sendGoMoreCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(GOMORE_COMMAND_PREFIX_WITH_START);
		message.append(command.getCommandId());
		message.append(makeNodeString(command.getNodeId()));
		message.append(command.getEqOption());
		// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가]
		if (command.getStationId() != null
				&& command.getStationId().length() > 0) {
			// 2012.06.30 by KYK : 꺽쇄안 자리수 맞춰주기 <1234A> -> <001234A>
			String stationIDString = makeStationString(command.getStationId());
			message.append(LEFT_ANGLE_BRACKET).append(stationIDString)
					.append(RIGHT_ANGLE_BRACKET);
		}

		return registerSendData(message.toString());
	}

	@Override
	public boolean sendIDResetCommand() {
		if (clientSocket == null) {
			return false;
		}

		return registerSendData(IDRESET_COMMAND);
	}

	@Override
	public boolean sendIntersectionNodes(VehicleCommCommand command) {
		// 2011.11.30 by PMM
		if (clientSocket == null) {
			return false;
		}
		
		ArrayList<String> routedIntersectionNodeList = command.getRoutedIntersectionNodeList();
		int sentIntersectionNodeCount = 0;
		int remainedIntersectionNodeCount = 0;
		int sendingIntersectionNodeCount = 0;
		int messageNumber = 1;
		int messageCount = routedIntersectionNodeList.size() / MAX_INTERSECTION_NODE_COUNT + 1;
		while (messageCount - messageNumber >= 0) {
			if (messageNumber > 9) {
				break;
			}
			
			
			remainedIntersectionNodeCount = routedIntersectionNodeList.size() - sentIntersectionNodeCount;
			if (remainedIntersectionNodeCount / MAX_INTERSECTION_NODE_COUNT > 0) {
				sendingIntersectionNodeCount = MAX_INTERSECTION_NODE_COUNT;
			} else {
				sendingIntersectionNodeCount = remainedIntersectionNodeCount;
			}
			StringBuilder message = new StringBuilder();
			message.append(INTERSECTION_NODE_LIST_PREFIX_WITH_START);
			message.append(messageNumber);
			if (messageCount < 10) {
				message.append(messageCount);
			} else {
				message.append(9);
			}
			message.append(String.format("%03d", sendingIntersectionNodeCount));
			message.append(LEFT_BRACE).append(command.getTargetNode()).append(RIGHT_BRACE);
			message.append(LEFT_BRACKET);
			for (int i = 0; i < sendingIntersectionNodeCount; i++) {
				if (i > 0) {
					message.append(',');
				}
				message.append(routedIntersectionNodeList.get(sentIntersectionNodeCount + i));
			}
			message.append(RIGHT_BRACKET);
			registerSendData(message.toString());
			sentIntersectionNodeCount += sendingIntersectionNodeCount;
			messageNumber++;
		}
		return true;
	}

	@Override
	public boolean sendLoadCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(MESSAGE_START);
		if (command.getCommandOption() == 'N') message.append(NEXT_COMMAND_PREFIX);
		message.append(LOAD_COMMAND_PREFIX);
		message.append(command.getCommandId());
		message.append(makeNodeString(command.getNodeId()));
		message.append(command.getErrorReportOption());
		message.append(command.getEqOption());
		message.append(command.getRfOption());

		if (isBidirectionalSTB) {
			message.append(command.getCarrierlocDirectionOption());
		}

		// 2018.09.28 by JJW : Carrier Type 기능 사용
		if(isCarrierTypeUsage){
			// 2017.08.07 by KBS : CarrierType 추가
			message.append(makeCarrierTypeString(command.getCarrierType()));			
		}
		
		// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가]
		if (command.getStationId() != null
				&& command.getStationId().length() > 0) {
			// 2012.06.30 by KYK : 꺽쇄안 자리수 맞춰주기 <1234A> -> <001234A>
			String stationIDString = makeStationString(command.getStationId());
			message.append(LEFT_ANGLE_BRACKET).append(stationIDString)
					.append(RIGHT_ANGLE_BRACKET);
		}
				
		return registerSendData(message.toString());
}

	@Override
	public boolean sendRemoveCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(REMOVE_COMMAND_PREFIX_WITH_START);
		message.append(command.getCommandId());
		message.append(makeNodeString(command.getNodeId()));

		return registerSendData(message.toString());
	}

	@Override
	public boolean sendMapMakeCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(MAPMAKE_COMMAND_PREFIX_WITH_START);
		message.append(command.getCommandId());
		message.append(makeNodeString(command.getNodeId()));

		return registerSendData(message.toString());
	}

	@Override
	public boolean sendPauseCommand() {
		if (clientSocket == null) {
			return false;
		}
		
		return registerSendData(PAUSE_COMMAND);
	}

	@Override
	public boolean sendResumeCommand() {
		if (clientSocket == null) {
			return false;
		}

		return registerSendData(RESUME_COMMAND);
	}

	@Override
	public boolean sendRouteInfoData(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(ROUTE_INFO_DATA_PREFIX_WITH_START);
		message.append(0);
		message.append(command.getRouteInfoData());
		return registerSendData(message.toString());
	}

	@Override
	public boolean sendScanCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(MESSAGE_START);

		if (command.getCommandOption() == 'N') {
			message.append(NEXT_COMMAND_PREFIX);
		}

		message.append(SCAN_COMMAND_PREFIX);
		message.append(command.getCommandId());
		message.append(makeNodeString(command.getNodeId()));
		message.append(command.getEqOption());

		if (isBidirectionalSTB) {
			message.append(command.getCarrierlocDirectionOption());
		}

		return registerSendData(message.toString());
	}

	@Override
	public boolean sendUnloadCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		StringBuilder message = new StringBuilder();
		message.append(MESSAGE_START);
		if (command.getCommandOption() == 'N') message.append(NEXT_COMMAND_PREFIX);
		message.append(UNLOAD_COMMAND_PREFIX);
		message.append(command.getCommandId());
		message.append(makeNodeString(command.getNodeId()));
		message.append(command.getErrorReportOption());
		message.append(command.getEqOption());
		message.append(command.getRfOption());

		if (isBidirectionalSTB) {
			message.append(command.getCarrierlocDirectionOption());
		}
		
		// 2018.09.28 by JJW : Carrier Type 기능 사용
		if(isCarrierTypeUsage){
			// 2017.08.07 by KBS : CarrierType 추가
			message.append(makeCarrierTypeString(command.getCarrierType()));
		}
		
		// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가]
		if (command.getStationId() != null
				&& command.getStationId().length() > 0) {
			// 2012.06.30 by KYK : 꺽쇄안 자리수 맞춰주기 <1234A> -> <001234A>
			String stationIDString = makeStationString(command.getStationId());
			message.append(LEFT_ANGLE_BRACKET).append(stationIDString)
					.append(RIGHT_ANGLE_BRACKET);
		}

		return registerSendData(message.toString());
	}
	
	@Override
	public boolean sendAutoCommand(){
		if (clientSocket == null) {
			return false;
		}
		
		return registerSendData(AUTO_COMMAND_PREFIX);
		
	}
	
	
	protected boolean registerSendData(String sendData) {
		if (sendStringList.contains(sendData)) {
			return false;
		}
		
		sendStringList.add(sendData);
		return true;
	}
	
	protected String makeNodeString(String node) {
		try {
			return String.format("%06d", Integer.parseInt(node));
		} catch (Exception e) {
			traceVehicleCommException("makeNodeString() - Exception: A node (ID: " + node + ") is not number.", e);
			return "000000";
		}
	}
	
	protected String makeCarrierTypeString(byte carrierType) {
		if (carrierType == 0x64) {
			return "64";
		} else {
			return String.format("%02d", (int) carrierType);
		}
	}
	
	protected int makeCarrierTypeInteger(String carrierType) {
		if ("64".equals(carrierType)) {
			return 0x64;
		} else {
			return Integer.parseInt(carrierType);
		}
	}
	
	// 2021.03.10 by JDH : PatrolStatus update
	protected boolean isPatrol(String vhlName){		
		Vehicle vhl = vehicleManager.getVehicle(vhlName);
		if(vhl.getZone().equals("Patrol"))
			return true;
		else
			return false;
	}
	
	// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가]
	private String makeStationString(String stationId) {
		// 2자리 미만 비정상 받은그대로 돌려주자..  
		if (stationId.length() == 6) {
			return stationId;
		}		
		while (stationId.length() < 7) {
			stationId = "0" + stationId; 
		}
		return stationId;
	}
	
}
