package com.samsung.ocs.operation.comm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Vector;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.util.StringUtil;
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
public class VehicleCommV7 extends VehicleComm {
	// Define Command & Ack
	protected final static int GO_COMMAND = Integer.valueOf("39111000", 16);
	protected final static long GO_ACK = Long.valueOf("93A11000", 16);
	protected final static int GOMORE_COMMAND = Integer.valueOf("39111010", 16);
	protected final static long GOMORE_ACK = Long.valueOf("93A11010", 16);
	protected final static int LOAD_COMMAND = Integer.valueOf("39133000", 16);
	protected final static long LOAD_ACK = Long.valueOf("93A33000", 16);
	protected final static int UNLOAD_COMMAND = Integer.valueOf("39133010", 16);
	protected final static long UNLOAD_ACK = Long.valueOf("93A33010", 16);
	protected final static int SCAN_COMMAND = Integer.valueOf("39133020", 16);
	protected final static long SCAN_ACK = Long.valueOf("93A33020", 16);	
	protected final static int MTL_COMMAND = Integer.valueOf("39122000", 16);
	protected final static long MTL_ACK = Long.valueOf("93A22000", 16);
	protected final static int MAPMAKER_COMMAND = Integer.valueOf("39122010", 16);
	protected final static long MAPMAKER_ACK = Long.valueOf("93A22010", 16);
	protected final static int PATROL_COMMAND = Integer.valueOf("39122020", 16);	
	protected final static long PATROL_ACK = Long.valueOf("93A22020", 16);
	protected final static int PATROLCANCEL_COMMAND = Integer.valueOf("39122021", 16);
	protected final static long PATROLCANCEL_ACK = Long.valueOf("93A22021", 16);
	
	protected final static int CANCEL_COMMAND = Integer.valueOf("39144020", 16);
	protected final static long CANCEL_ACK = Long.valueOf("93A44020", 16);
	protected final static int IDRESET_COMMAND = Integer.valueOf("39144030", 16);
	protected final static long IDRESET_ACK = Long.valueOf("93A44030", 16);
	protected final static int PAUSE_COMMAND = Integer.valueOf("39144040", 16);
	protected final static long PAUSE_ACK = Long.valueOf("93A44040", 16);
	protected final static int RESUME_COMMAND = Integer.valueOf("39144050", 16);
	protected final static long RESUME_ACK = Long.valueOf("93A44050", 16);
	protected final static int ESTOP_COMMAND = Integer.valueOf("39144060", 16);
	protected final static long ESTOP_ACK = Long.valueOf("93A44060", 16);
	
	// 2020.05.11 by YSJ (OHT Auto Change)
	protected final static int VEHICLEAUTO_COMMAND = Integer.valueOf("391E4070", 16);
	protected final static long VEHICLEAUTO_ACK = Long.valueOf("93AE4070", 16);
	
	protected final static long VEHICLE_STATUS = Long.valueOf("931AA000", 16);
	protected final static int VEHICLE_STATUS_ACK = Integer.valueOf("3919A000", 16);
	
	protected final static long H_VEHICLE_STATUS = Long.valueOf("A31AA000", 16);
	protected final static int H_VEHICLE_STATUS_ACK = Integer.valueOf("3A19A000", 16);
	
	protected final static int ROUTE_INFO_COMMAND = Integer.valueOf("391F1000", 16);
	protected final static long ROUTE_INFO_ACK = Long.valueOf("93AF1000", 16);
	
	protected final static long HEADER_MASK = Long.valueOf("FFFFFFFF", 16);
	protected final static long SUBTRACTION_NUM = Long.valueOf("5A900000", 16);
	
	// Ack Error 정의
	protected final static int ACK 									= Integer.valueOf("00", 16); // 0x00
	protected final static int BUSY 								= Integer.valueOf("01", 16); // 0x01
	protected final static int ERROR 								= Integer.valueOf("02", 16); // 0x02
	protected final static int DATALOGIC 							= Integer.valueOf("03", 16); // 0x03
	
	protected final static int VEHICLE_MANUAL_MODE		= Integer.valueOf("01", 16); // 0x01
	protected final static int VEHICLE_AUTO_MODE		= Integer.valueOf("10", 16); // 0x10

	protected final static int VEHICLE_INIT				= Integer.valueOf("00", 16); // 0x00
	protected final static int VEHICLE_GOING			= Integer.valueOf("01", 16); // 0x01
	protected final static int VEHICLE_ARRIVED			= Integer.valueOf("02", 16); // 0x02
	protected final static int VEHICLE_UNLOADING		= Integer.valueOf("03", 16); // 0x03
	protected final static int VEHICLE_UNLOADED			= Integer.valueOf("04", 16); // 0x04
	protected final static int VEHICLE_LOADING			= Integer.valueOf("05", 16); // 0x05
	protected final static int VEHICLE_LOADED			= Integer.valueOf("06", 16); // 0x06
	protected final static int VEHICLE_AUTOPOSITION		= Integer.valueOf("07", 16); // 0x07
	protected final static int VEHICLE_MANUAL_ING 		= Integer.valueOf("08", 16); // 0x08
	protected final static int VEHICLE_MANUAL_COM 		= Integer.valueOf("09", 16); // 0x09
	protected final static int VEHICLE_AUTORECOVERY 	= Integer.valueOf("0A", 16); // 0x0A
	protected final static int VEHICLE_COMMAND_CANCEL 	= Integer.valueOf("0C", 16); // 0x0C
	protected final static int VEHICLE_ERROR 			= Integer.valueOf("0E", 16); // 0x0E
	
	protected final static String ZERO_STRING = "0";
	
	protected BufferedInputStream readBuffer;
	protected BufferedOutputStream writeBuffer;
//	protected VehicleCommData vehicleCommData;
	
	protected int currReceiveDataOffset = 0;
	protected byte[] emptyData = new byte[2048];
	protected byte[] receiveBuffer = new byte[1024];
	protected byte[] currReceiveData = new byte[2048];
	protected byte[] lastReceivedData;
	protected byte[] sendMessage;
	protected static int TAG_LENGTH = 4; 
	protected static int HEADER_LENGTH = 6;
	protected static int TAIL_LENGTH = 2;
	
	private Vector<byte[]> sendMessageList;
	
	public VehicleCommV7() {
		super();
		
		sendMessageList = new Vector<byte[]>();
		vehicleCommData = new VehicleCommData();
		
		initialize();
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		
		sendMessageList.clear();
	}
	
	@Override
	protected void openClientSocket() {
		long startTime = System.currentTimeMillis();
		try {
			traceVehicleComm("openClientSocket: Try to Connect to " + targetName + " ClientSocketOpen : " + targetPort);
//			lastReceivedTime = System.currentTimeMillis();

			clientSocket = new Socket(targetIPAddress, targetPort);
			readBuffer = new BufferedInputStream(clientSocket.getInputStream());
			writeBuffer = new BufferedOutputStream(clientSocket.getOutputStream());

			// 2014.06.21 by MYM : [Commfail 체크 개선] reconnectionCount, lastReceivedTime 초기화
			// 0은 초기 Vehicle 투입시 통신 시도할 때, 투입 이후 통신이 끊겼다가 다시 접속하는 경우는 1부터 Count함.
			reconnectionCount = 1;
			lastReceivedTime = System.currentTimeMillis();			

			currReceiveDataOffset = 0;
			System.arraycopy(emptyData, 0, currReceiveData, 0, emptyData.length);
			traceVehicleComm("openClientSocket: Client Socket Connected...");

			socketReadThread = new VehicleCommSocketReadThread(this);
			socketReadThread.start();
		} catch (Exception e) {
			socketDisconnectedTime = startTime;
//			socketDisconnectedTime = System.currentTimeMillis();

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
		vehicleCommData.setReceivedState(false);
		
		try {
			socketDisconnectedTime = System.currentTimeMillis() - socketReconnectionTimeout;
			sendFlag = false;			
			currReceiveDataOffset = 0;
			System.arraycopy(emptyData, 0, currReceiveData, 0, emptyData.length);
			sendMessageList.clear();
			
			if (socketReadThread != null) {
				socketReadThread.stopThread();
				socketReadThread = null;
			}
			
			if (writeBuffer != null) {
				try {
					writeBuffer.close();
				} catch (IOException e) {}
				writeBuffer = null;
			}

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
		
		if (clientSocket == null) {
			if (Math.abs(System.currentTimeMillis() - socketDisconnectedTime) > socketReconnectionTimeout) {
				logMessage.append("Retry Connect[").append(reconnectionCount++).append("]...");
				traceVehicleComm("checkSocketProcess: " + logMessage.toString());
				openClientSocket();
				sendFlag = false;
			}
			
			// 2014.06.21 by MYM : [Commfail 체크 개선] 통신 Fail로 간주된 경우 ErrorCode 업데이트 및 로그 기록.  
			if (isCommFail() && vehicleCommData.getErrorCode() != OcsConstant.COMMUNICATION_FAIL) {
				vehicleCommData.setErrorCode(OcsConstant.COMMUNICATION_FAIL);
				traceVehicleComm("Update Vehicle Communication Fail.");
			}
		} else {
//			if (clientSocket.isConnected()) {
//				lastConnectedTime = System.currentTimeMillis();
//			}
			
			if (vehicleCommData.getVehicleMode() == 'A') {
				if (sendFlag == false) {
					if (sendMessageList.size() > 0) {
						sendFlag = true;
						sendMessage = sendMessageList.get(0);						
						sendMessage(sendMessage);
						sendRequestedTime = System.currentTimeMillis();
					}
				} else if (isTimeoutCheckUnnecessary(sendMessage)) {
					sendFlag = false;
					sendMessageList.remove(sendMessage);					
				} else if (Math.abs(System.currentTimeMillis() - sendRequestedTime) > REPLY_TIMEOUT) {
					byte[] commandTimeoutMessage = makeTimeoutMessage(sendMessage);
					logMessage.append("RCV> ").append(getHexString(commandTimeoutMessage, 0, commandTimeoutMessage.length)).append(" Timeout by Operation ");
					traceVehicleComm(logMessage.toString());
					processReceivedMessage(commandTimeoutMessage);
					sendRequestedTime = System.currentTimeMillis();
					
					if (replyTimeoutCount++ > 20) {
						replyTimeoutCount = 0;
						closeClientSocket("ReplyTimeout count over 20");
					}
				}
			} else {
				sendFlag = false;
				sendMessageList.clear();
			}
			
			// 2014.06.21 by MYM : [Commfail 체크 개선] 상태 미수신 체크는 별도의 파라미터(socketCloseCheckTime)로 확인
			// socketReconnectionTimeout 파라미터는 재접속 Interval로 사용함.
//			if (Math.abs(System.currentTimeMillis() - lastReceivedTime) > socketReconnectionTimeout) {
//				closeClientSocket("Vehicle state has not received during " + socketReconnectionTimeout/1000 + "sec");
			if (Math.abs(System.currentTimeMillis() - lastReceivedTime) > socketCloseCheckTime) {
				closeClientSocket("Vehicle state has not received during " + socketCloseCheckTime/1000 + "sec");
			}
		}
		
		return true;
	}

	@Override
	public boolean readSocketProcess() {
		try {
			if (readBuffer != null) {
				int length = readBuffer.read(receiveBuffer);
				if (length > 0) {
					long startTime  = System.currentTimeMillis();

					readSocket(receiveBuffer, length);
					
					// 2013. 10.04 by MYM : 1) readSocket 처리 시간이 50ms 이상인 경우 로그 기록, 
					//                      2) readBuffer에 message가 꽉 찬 경우 로그 기록 → Network 지연 등으로 밀려 차서 들어올 경우 체크 
					long elapsedTime = System.currentTimeMillis() - startTime;
					if (elapsedTime >= 50) {
						StringBuilder log = new StringBuilder();
						log.append("readSocket elapsedTime : ").append(elapsedTime).append("ms");
						traceVehicleCommException(log.toString());
					}
					if (length == receiveBuffer.length) {
						StringBuilder log = new StringBuilder();
						log.append("readBuffer is full of messages(").append(length).append(")");
						traceVehicleCommException(log.toString());
					}
				}
			} else {
				traceVehicleComm("ReadSocketThread() - readBuffer is null.");
			}
		} catch (Throwable e) {
			closeClientSocket("ReadSocketProcess exception");
			traceVehicleComm("ReadSocketThread()", e);
		}
		
		return true;
	}
	
	protected void readSocket(byte[] buffer, int length) {
		try {
			System.arraycopy(buffer, 0, currReceiveData, currReceiveDataOffset, length);
			currReceiveDataOffset += length;
			
			lastReceivedTime = System.currentTimeMillis();
			replyTimeoutCount = 0;
			
			int offset = 0;
			while ((offset + HEADER_LENGTH) < currReceiveDataOffset) {
				long headerType = getHeader(currReceiveData, offset);
				if (isValidHeader(headerType)) {
					int bodyLength = getLegth(currReceiveData, offset + TAG_LENGTH);
					int totalMessageLength = HEADER_LENGTH + bodyLength + TAIL_LENGTH;
					if ((offset + totalMessageLength) <= currReceiveDataOffset) {
						int tail = getLegth(currReceiveData, offset + HEADER_LENGTH + bodyLength);
						if (bodyLength == tail) {
							byte[] receivedData = new byte[totalMessageLength];
							System.arraycopy(currReceiveData, offset, receivedData, 0, totalMessageLength);
							
							// 2013.10.04 by MYM : Parsing 후 바로 업데이트 처리
							// 기존에는 List에 Parsing한 데이터를 넣고 이후에 처리하였으나 Parsing 후 바로 업데이트 처리로 변경
							if ((Arrays.equals(receivedData, lastReceivedData) == false) ||
									(Math.abs(System.currentTimeMillis() - lastCommLogUpdatedTime) > 5000)) {
								StringBuilder receiveHexString = new StringBuilder();
								receiveHexString.append("RCV> ").append(getHexString(receivedData, 0, receivedData.length));
								traceVehicleComm(receiveHexString.toString());
								lastCommLogUpdatedTime = System.currentTimeMillis();
								processReceivedMessage(receivedData);
							}
						} else {
							StringBuilder log = new StringBuilder();
							log.append("mismatch bodyLength:").append(bodyLength).append(", tail:").append(tail);
							log.append(", message:").append(getHexString(currReceiveData, offset, totalMessageLength));
							traceVehicleCommException(log.toString());
						}
						offset += totalMessageLength;
					} else {
						break;
					}
				} else {
					headerType = 0;
					offset++;
				}
			}
			
			// 2013.10.04 by MYM : currReceiveDataOffset 초기화 안되어 Exception 발생 문제 수정
			//                     while 루프에서 처리하던 것을 제일 마지막에 처리하도록 변경
			currReceiveDataOffset = currReceiveDataOffset - offset;
			if (currReceiveDataOffset < 0) {
				StringBuilder log = new StringBuilder();
				log.append("abnormal offset - currReceiveDataOffset : ").append(currReceiveDataOffset).append(", offset : ").append(offset);
				traceVehicleCommException(log.toString());
				currReceiveDataOffset = 0;
			}
			System.arraycopy(currReceiveData, offset, currReceiveData, 0, currReceiveDataOffset);
		} catch (Exception e) {
			StringBuilder log = new StringBuilder();
			log.append("currReceiveDataOffset:").append(currReceiveDataOffset).append(", length:").append(length);
			currReceiveDataOffset = 0;
			traceVehicleCommException("readSocket()", e);
			traceVehicleCommException(log.toString());
		}
	}
	
	protected void processStatusForOHT(byte[] receiveData) {
		ByteBuffer data = ByteBuffer.wrap(receiveData, HEADER_LENGTH, receiveData.length - HEADER_LENGTH);
		vehicleCommData.setVehicleMode(getVehicleMode(data.get()));
		
		// 2015.12.21 by KBS : Patrol VHL 기능 추가
		byte b = data.get();
		int patrolStatus = (int) ((b & 0x80) >> 7);
		int state = (int) (b & 0x0F);
		vehicleCommData.setPatrolStatus(getPatrolStatus(patrolStatus));
		vehicleCommData.setState(getVehicleStatus(state));

		vehicleCommData.setPrevCmd(data.get());
		vehicleCommData.setCurrCmd(data.get());
		vehicleCommData.setNextCmd(data.get());

		vehicleCommData.setCurrNode(getNodeData(data.getInt()));
		vehicleCommData.setCurrNodeOffset(data.getInt());				
		vehicleCommData.setCurrStationId(getStationData(data.getInt()));
		
		vehicleCommData.setStopNode(getNodeData(data.getInt()));
		vehicleCommData.setStopStationId(getStationData(data.getInt()));
		vehicleCommData.setStopStationOffset(data.getInt());
		
		// 2014.11.04 by KYK [DualOHT] TODO
//		vehicleCommData.setCarrierExist(data.get() == 1 ? '1' : '0');
		vehicleCommData.setCarrierExist(getCarrierExist(data.get()));
		vehicleCommData.setCarrierType(data.get());
		vehicleCommData.setPauseType(data.get());
		vehicleCommData.setSteerPosition(data.get());
		vehicleCommData.setErrorCode(data.getInt());

		vehicleCommData.setOrigin(data.getShort());
		byte[] rfTagData = new byte[10];
		data.get(rfTagData, 0, rfTagData.length);
		vehicleCommData.setRfData(new String(rfTagData));
		vehicleCommData.setAPSignal(data.getInt());

		StringBuilder apMacAddress = new StringBuilder(); 
		for (int i = 0; i < 6; i++) {
			if (apMacAddress.length() > 0) {
				apMacAddress.append(':');
			}
			apMacAddress.append(String.format("%02x", (data.get() & 0xFF)).toUpperCase());
		}
		vehicleCommData.setAPMacAddress(apMacAddress.toString());

		vehicleCommData.setMotorDrvFPosition(data.getInt());
		vehicleCommData.setSpeed(data.getInt());
		vehicleCommData.setMotorHoistPosition(data.getInt());
		vehicleCommData.setMotorShiftPosition(data.getInt());
		vehicleCommData.setMotorRotate(data.getInt());
		
		vehicleCommData.setVehicleType(data.get());
		
		StringBuilder mapVersion = new StringBuilder(); 
		for (int i = 0; i < 4; i++) {
			mapVersion.append(String.format("%02x", (data.get() & 0xFF)).toUpperCase());
		}
		vehicleCommData.setMapVersion(mapVersion.toString());
		StringBuilder stationMapVersion = new StringBuilder(); 
		for (int i = 0; i < 4; i++) {
			stationMapVersion.append(String.format("%02x", (data.get() & 0xFF)).toUpperCase());
		}
		vehicleCommData.setStationMapVersion(stationMapVersion.toString());
		StringBuilder teachingMapVersion = new StringBuilder(); 
		for (int i = 0; i < 4; i++) {
			teachingMapVersion.append(String.format("%02x", (data.get() & 0xFF)).toUpperCase());
		}
		vehicleCommData.setTeachingMapVersion(teachingMapVersion.toString());
		
		byte[] hidData = new byte[16];
		for (int i = 0; i < 16; i++) {
			hidData[i] = data.get(); 
		}
		vehicleCommData.setHidData(StringUtil.encodeBase64(hidData));
		byte[] ioData = new byte[12];
		for (int i = 0; i < 12; i++) {
			ioData[i] = data.get(); 
		}
		vehicleCommData.setInputData(StringUtil.encodeBase64(ioData));
		for (int i = 0; i < 12; i++) {
			ioData[i] = data.get();
		}
		vehicleCommData.setOutputData(StringUtil.encodeBase64(ioData));
		vehicleCommData.setReceivedState(true);

		if (receiveData.length - HEADER_LENGTH > 134) {
			// 2016.08.16 by KBS : Unload/Load 보고 시점 개선
			vehicleCommData.setTransStatus(data.get());
		} else {
			vehicleCommData.setTransStatus(1);
		}
	}
	
	protected void processStatusForHOHT(byte[] receiveData) {
		ByteBuffer data = ByteBuffer.wrap(receiveData, HEADER_LENGTH, receiveData.length - HEADER_LENGTH);
		vehicleCommData.setVehicleMode(getVehicleMode(data.get()));
		vehicleCommData.setState(getVehicleStatus(data.get()));

		vehicleCommData.setPrevCmd(data.get());
		vehicleCommData.setCurrCmd(data.get());
		vehicleCommData.setNextCmd(data.get());

		vehicleCommData.setCurrNode(getNodeData(data.getInt()));
		vehicleCommData.setCurrNodeOffset(data.getInt());				
		vehicleCommData.setCurrStationId(getStationData(data.getInt()));
		
		vehicleCommData.setStopNode(getNodeData(data.getInt()));
		vehicleCommData.setStopStationId(getStationData(data.getInt()));
		vehicleCommData.setStopStationOffset(data.getInt());
		
		vehicleCommData.setCarrierExist(data.get() == 1 ? '1' : '0');
		vehicleCommData.setPauseType(data.get());
		vehicleCommData.setSteerPosition(data.get());
		vehicleCommData.setErrorCode(data.getInt());

		vehicleCommData.setOrigin(data.getShort());
		byte[] rfTagData = new byte[10];
		data.get(rfTagData, 0, rfTagData.length);
		vehicleCommData.setRfData(new String(rfTagData));
		vehicleCommData.setAPSignal(data.getInt());

		StringBuilder apMacAddress = new StringBuilder(); 
		for (int i = 0; i < 6; i++) {
			if (apMacAddress.length() > 0) {
				apMacAddress.append(':');
			}
			apMacAddress.append(String.format("%02x", (data.get() & 0xFF)).toUpperCase());
		}
		vehicleCommData.setAPMacAddress(apMacAddress.toString());

		vehicleCommData.setMotorDrvFPosition(data.getInt());
		vehicleCommData.setSpeed(data.getInt());
		vehicleCommData.setMotorHoistPosition(data.getInt());
		vehicleCommData.setMotorShiftPosition(data.getInt());
		vehicleCommData.setMotorRotate(data.getInt());
		
		vehicleCommData.setVehicleType(data.get());
		
		byte[] hidData = new byte[50];
		for (int i = 0; i < 50; i++) {
			hidData[i] = data.get(); 
		}
		vehicleCommData.setHidData(StringUtil.encodeBase64(hidData));
		byte[] ioData = new byte[32];
		for (int i = 0; i < 32; i++) {
			ioData[i] = data.get();
		}
		vehicleCommData.setInputData(StringUtil.encodeBase64(ioData));
		for (int i = 0; i < 32; i++) {
			ioData[i] = data.get();
		}
		vehicleCommData.setOutputData(StringUtil.encodeBase64(ioData));
		vehicleCommData.setReceivedState(true);
	}
//	protected void processStationData(byte[] receiveData) {
//		ByteBuffer data = ByteBuffer.wrap(receiveData, HEADER_LENGTH, receiveData.length - HEADER_LENGTH);
//		// CmdID(1), StopNodeID(4), StopStationID(4), StopStationOffset(4), StopStationType(1), NextNodeID(4)
//		VehicleCommMapData mapData = new VehicleCommMapData();				
//		mapData.setCmdId(data.get());
//		mapData.setStationType(data.get());
//		mapData.setStationId(getStationData(data.getInt()));
//		mapData.setFirstNodeId(getNodeData(data.getInt()));
//		mapData.setFirstStationOffset(data.getInt());
//		mapData.setFirstNextNodeId(getNodeData(data.getInt()));
//		mapData.setSecondNodeId(getNodeData(data.getInt()));
//		mapData.setSecondStationOffset(data.getInt());
//		mapData.setSecondNextNodeId(getNodeData(data.getInt()));
//		vehicleCommData.addMapData(mapData);				
//	}
//	
//	protected void processTeachingData(byte[] receiveData) {
//		ByteBuffer data = ByteBuffer.wrap(receiveData, HEADER_LENGTH, receiveData.length - HEADER_LENGTH);
//		// CmdID(1), CurrNodeID(4), CurrStationID(4), PortType(1), EQPIODir(1)
//		// HoistPosition(4), ShiftPosition(4), RotatePosition(4)
//		VehicleCommTeachingData teachingData = new VehicleCommTeachingData();
//		teachingData.setCmdId(data.get());
//		teachingData.setNodeId(getNodeData(data.getInt()));
//		teachingData.setStationId(getStationData(data.getInt()));
//		teachingData.setPortType(data.get());
//		teachingData.setPIODirection(data.get());
//		teachingData.setPIOTimeLevel(data.get());
//		teachingData.setLookDownLevel(data.get());
//		teachingData.setHoistPosition(data.getInt());
//		teachingData.setShiftPosition(data.getInt());
//		teachingData.setRotatePosition(data.getInt());
//		vehicleCommData.addTeachingData(teachingData);
//	}
	
	protected void processResponseMessage(long headerType, byte[] receiveData) {
		ByteBuffer data = ByteBuffer.wrap(receiveData, HEADER_LENGTH, receiveData.length - HEADER_LENGTH);
		vehicleCommData.setCommand(getCmdType(headerType));
		vehicleCommData.setCommandId(data.get());				
		vehicleCommData.setReply(getAckType(data.get()));				
		if (sendMessageList.size() > 0) {
			long header = getHeader(sendMessage, 0);
			int cmdId = ByteBuffer.wrap(sendMessage, 6, 1).get();
			if (header == (headerType - SUBTRACTION_NUM)
					&& vehicleCommData.getCommandId() == cmdId) {
				sendMessageList.remove(sendMessage);
				sendRequestedTime = System.currentTimeMillis();
				sendFlag = false;
			}
		} else {
			sendFlag = false;
		}
	
	}
	protected void processReceivedMessage(byte[] receiveData) {
		try {
			long headerType = getHeader(receiveData, 0);
			if (headerType == VEHICLE_STATUS) {
				processStatusForOHT(receiveData);
			} else if (headerType == H_VEHICLE_STATUS) {
				processStatusForHOHT(receiveData);
			} else if (isCommandAck(headerType)) {
				processResponseMessage(headerType, receiveData);
			} else {
				traceVehicleCommException("processReceivedMessage() - Unknown Message:" + headerType);
			}
			lastReceivedData = receiveData;
		} catch (Throwable e) {
			traceVehicleCommException("processReceivedMessage() - Message:" + receiveData, e);
		}
	}
	
	public VehicleCommData getVehicleCommData() {
		return vehicleCommData;
	}
	
	protected boolean isTimeoutCheckUnnecessary(byte[] message) {
		long header = getHeader(message, 0);
		if (isCommandHeader(header)) {
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param headerType
	 * @return
	 */
	protected boolean isValidHeader(long headerType) {
		if (headerType == VEHICLE_STATUS || headerType == H_VEHICLE_STATUS
				|| isCommandAck(headerType)) {
			return true;
		}

		return false;
	}
	
	/**
	 * 
	 * @param headerType
	 * @return
	 */
	protected boolean isCommandAck(long headerType) {
		if (headerType == GO_ACK || headerType == GOMORE_ACK
				|| headerType == MTL_ACK || headerType == MAPMAKER_ACK
				|| headerType == PATROL_ACK || headerType == LOAD_ACK
				|| headerType == UNLOAD_ACK ||headerType == SCAN_ACK 
				|| headerType == CANCEL_ACK || headerType == IDRESET_ACK 
				|| headerType == PAUSE_ACK || headerType == RESUME_ACK 
				|| headerType == ESTOP_ACK || headerType == ROUTE_INFO_ACK
				|| headerType == PATROLCANCEL_ACK || headerType == VEHICLEAUTO_ACK) {
			return true;
		}
		
		return false;
	}
	
	protected boolean isCommandHeader(long headerType) {
		if (headerType == GO_COMMAND || headerType == GOMORE_COMMAND
				|| headerType == LOAD_COMMAND || headerType == UNLOAD_COMMAND
				|| headerType == MTL_COMMAND || headerType == MAPMAKER_COMMAND
				|| headerType == PATROL_COMMAND || headerType == PATROLCANCEL_COMMAND
				|| headerType == CANCEL_COMMAND || headerType == IDRESET_COMMAND
				|| headerType == PAUSE_COMMAND || headerType == RESUME_COMMAND
				|| headerType == ESTOP_COMMAND || headerType == SCAN_COMMAND 
				|| headerType == ROUTE_INFO_COMMAND || headerType == VEHICLEAUTO_COMMAND) {
			return true;
		}
		
		return false;
	}

	/**
	 * Byte Array에서 offset부터 length만큼 가져와 Hex String으로 변환하여 반환
	 * 
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return
	 */
	protected String getHexString(byte[] buffer, int offset, int length) {
		try {
			length = offset + length;
			if (length > buffer.length) {
				return ZERO_STRING;
			}

			StringBuffer sb = new StringBuffer(length);
			try {
				for (int i = offset; i < length; i++) {
					sb.append(String.format("%02x", (buffer[i] & 0xFF)).toUpperCase());
				}
			} catch (Exception e) {
				return ZERO_STRING;
			}
			return sb.toString();
			
		} catch (Exception e) {
			traceVehicleCommException(e.toString());
		}
		return "";
	}
	
	protected long getHeader(byte[] buffer, int offset) {
		return ByteBuffer.wrap(buffer, offset, 4).getInt() & HEADER_MASK;
	}
	
	protected int getLegth(byte[] buffer, int offset) {
		return ByteBuffer.wrap(buffer, offset, 2).getShort();
	}
	
	/**
	 * 2014.11.04 by KYK [DualOHT] TODO
	 * @param carrierExist
	 * @return
	 */
	protected char getCarrierExist(byte carrierExist) {
		if (carrierExist == 0) {
			return '0';
		} else if (carrierExist == 1) {
			return '1';
		} else if (carrierExist == 2) {
			return '2';			
		} else if (carrierExist == 3) {
			return '3';
		} else {
			return '0';			
		}
	}
	
	protected char getVehicleMode(int mode) {		
		if (mode == VEHICLE_AUTO_MODE) {
			return 'A';
		} else if (mode == VEHICLE_MANUAL_MODE) {
			return 'M';
		} else {
			return ' ';
		}
	}
	protected char getPatrolStatus(int status) {
		if (status == 1) {
			return '1';
		} else {
			return '0';
		}
	}
		
	protected char getVehicleStatus(int status) {
		if (status == VEHICLE_INIT) {
			return 'I';
		} else if (status == VEHICLE_GOING) {
			return 'G';
		} else if (status == VEHICLE_ARRIVED) {
			return 'A';
		} else if (status == VEHICLE_UNLOADING) {
			return 'U';
		} else if (status == VEHICLE_UNLOADED) {
			return 'N';
		} else if (status == VEHICLE_LOADING) {
			return 'L';
		} else if (status == VEHICLE_LOADED) {
			return 'O';
		} else if (status == VEHICLE_AUTOPOSITION) {
			return 'P';
		} else if (status == VEHICLE_MANUAL_ING) {
			return 'I'; // 2013.11.14 by KYK
		} else if (status == VEHICLE_MANUAL_COM) {
			return 'I'; // 2013.11.14 by KYK
		} else if (status == VEHICLE_ERROR) {
			return 'E';
		} else if (status == VEHICLE_AUTORECOVERY) {
			return 'V';
		} else if (status == VEHICLE_COMMAND_CANCEL) {
			// 2015.12.21 by KBS : Patrol VHL 기능 추가
			return 'Z';
		} else {
			return ' ';
		}
	}
	
	protected String getNodeData(int node) {
		return Integer.toHexString(node).toUpperCase();
	}
	
	protected String getStationData(int station) {
		if (station == 0) {
			return "";
		}
		return Integer.toHexString(station).toUpperCase();
	}
	
	/**
	 * 
	 * 
	 * @param cmdType
	 * @return
	 */
	protected char getCmdType(long cmdType) {
		if (cmdType == GO_ACK) {
			return 'g';
		} else if (cmdType == GOMORE_ACK) {
			return 'm';
		} else if (cmdType == MTL_ACK) {
			return 'f';
		} else if (cmdType == MAPMAKER_ACK) {
			return 'k';
		} else if (cmdType == PATROL_ACK) {
			return 'c';
		} else if (cmdType == PATROLCANCEL_ACK) {
			// 2015.12.21 by KBS : Patrol VHL 기능 추가
			return 'z';
		} else if (cmdType == LOAD_ACK) {
			return 'l';
		} else if (cmdType == UNLOAD_ACK) {
			return 'u';
		} else if (cmdType == SCAN_ACK) {
			return 'r';
		} else if (cmdType == CANCEL_ACK) {
			return 'x';
		} else if (cmdType == IDRESET_ACK) {
			return 'i';
		} else if (cmdType == PAUSE_ACK) {
			return 'h';
		} else if (cmdType == RESUME_ACK) {
			return 'e';
		} else if (cmdType == ESTOP_ACK) {
			return 'p';
		} else if (cmdType == ROUTE_INFO_ACK) {
			return 'a';
		} else if (cmdType == VEHICLEAUTO_ACK) {
			return 'j';
		} else {
			return ' ';
		}
	}
	
	/**
	 * 
	 * 
	 * @param ackType
	 * @return
	 */
	protected char getAckType(int ackType) {
		if (ackType == ACK) {
			return 'A';
		} else if (ackType == BUSY) {
			return 'B';
		} else if (ackType == ERROR) {
			return 'E';
		} else if (ackType == DATALOGIC) {
			return 'D';
		} else {
			return 'T';
		}
	}
	
	protected byte[] makeTimeoutMessage(byte[] sendMessage) {
		long header = getHeader(sendMessage, 0) + SUBTRACTION_NUM;
		ByteBuffer timeoutMessage = ByteBuffer.allocate(14); 
		ByteBuffer data = ByteBuffer.wrap(sendMessage);
		data.position(6);
		// Header(Tag:4/Length:2)
		timeoutMessage.putInt((int) header);
		timeoutMessage.putShort((short) 6);
		// Body(Value)
		// CmdID(1),AckType(1),AckCode(4),
		timeoutMessage.put(data.get());
		timeoutMessage.put((byte) 7); // 2013.09.16 by KYK : 0 -> 7 (임의), 0이면 ack 임 timeout 처리안됨;
		timeoutMessage.putInt(0);
		timeoutMessage.putShort((short) 6);
				
		return timeoutMessage.array();
	}
	
	protected boolean sendMessage(byte[] sendMessage) {
		if (clientSocket == null) {
			closeClientSocket("clientSocket is null");
			return false;
		} else {
			try {
				if (ByteBuffer.wrap(sendMessage, 0, 4).getInt() == ROUTE_INFO_COMMAND) {
					traceVehicleComm("SND> " + getHexString(sendMessage, 0, 7) + new String(sendMessage, 7, sendMessage.length - 9) + getHexString(sendMessage, sendMessage.length - 2, 2));
				} else {
					traceVehicleComm("SND> " + getHexString(sendMessage, 0, sendMessage.length));
				}
				writeBuffer.write(sendMessage);
				writeBuffer.flush();
			} catch (Exception e) {
			}
			return true;
		}
	}

	@Override
	public boolean sendCancelCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		return sendControlCommand(CANCEL_COMMAND, 0, (byte) command.getCommandId(), command.getCancelCommandType());
	}

	@Override
	public boolean sendPatrolCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		ByteBuffer message = ByteBuffer.allocate(34);

		// Header(Tag:4/Length:2)
		message.putInt(PATROL_COMMAND);	// Header Tag
		message.putShort((short) 26);	// Header Length

		// Body(Value)
		// CmdID(1), EndNodeId(4), PatrolSpeed(4), PatrolMode(1), Reserved1(4), Reserved2(4), Reserved3(4), Reserved4(4)
		message.put((byte) command.getCommandId());					// CmdID
		message.putInt(Integer.valueOf(command.getNodeId(), 16));	// EndNodeId
		message.putInt(command.getPatrolSpeed());					// PatrolSpeed
		// 2015.11.23 by KBS : Patrol VHL 기능 개발
		message.put((byte) command.getPatrolMode());				// PatrolMode
		message.putInt(0);											// Reserve1
		message.putInt(0);											// Reserve2
		message.putInt(0);											// Reserve3
		message.putInt(0);											// Reserve4

		message.putShort((short) 26);	// Tail(Length)

		return registerSendData(message.array());
	}

	@Override
	public boolean sendPatrolCancelCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		ByteBuffer message = ByteBuffer.allocate(17);

		// Header(Tag:4/Length:2)
		message.putInt(PATROLCANCEL_COMMAND);	// Header Tag
		message.putShort((short) 9); 			// Header Length

		// Body(Value)
		// CmdID(1), Param1(4), Param2(4)
		message.put((byte) 0);					// CmdID
		message.putInt(0);						// Param1
		message.putInt(0);						// Param2

		message.putShort((short) 9);			// Tail(Length)

		return registerSendData(message.array());
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
		 * - $P6 / $p6A : currStation의 ParenNode와 Vehicle의 CurrNode가 다른 경우
		 * - $P7 / $p7A : sourceloc materialType 과 Vehicle 로부터 올라온 carrierType 이 다른 경우	
		 */
		if (clientSocket == null) {
			return false;
		}
		return sendControlCommand(ESTOP_COMMAND, type, 0, 0);
	}

	@Override
	public boolean sendGoCommand(VehicleCommCommand command) {
		return sendGoCommand(GO_COMMAND, command);
	}

	@Override
	public boolean sendGoMoreCommand(VehicleCommCommand command) {
		return sendGoCommand(GO_COMMAND, command);
	}
	
	protected boolean sendGoCommand(int type, VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

//		ByteBuffer message = ByteBuffer.allocate(38);
//		ByteBuffer message = ByteBuffer.allocate(39); // 2014.11.14 by KYK : carrierPostion 추가
		ByteBuffer message = ByteBuffer.allocate(43); // 2014.12.05 by KYK : rotatePosition 추가

		// Header(Tag:4/Length:2)
		message.putInt(type);									// Header Tag
//		message.putShort((short) 30);					// Header Length
//		message.putShort((short) 31);					// Header Length
		message.putShort((short) 35);					// Header Length

		// Body(Value)
		// CmdID(1),StopNodeID(4),StopStationID(4),StopStationOffset(4),StopStationType(1),
		// NextNodeId(4),LineSpeedLevel(1),CurveSpeedLevel(1),AccelationLevel(1)
		// FinalStationType(1),ShiftPosition(4), PreSteeringNode(4)
		message.put((byte) command.getCommandId());											// CmdID
		message.putInt(Integer.valueOf(command.getNodeId(), 16));				// StopNodeID
		if (command.getStationId().length() > 0) {
			message.putInt(Integer.valueOf(command.getStationId(), 16));	// StopStationID
		} else {
			message.putInt(0);
		}
		message.putInt(command.getStationOffset());											// StopStationOffset
		message.put((byte) command.getStationType());										// StopStationType
		if (command.getNextNodeId().length() > 0) {
			message.putInt(Integer.parseInt(command.getNextNodeId(), 16));// NextNodeId
		} else {
			message.putInt(0);
		}
		message.put((byte) command.getLineSpeedLevel());								// LineSpeedLevel
		message.put((byte) command.getCurveSpeedLevel());								// CurveSpeedLevel
		message.put((byte) command.getAccelationLevel());								// AccelationLevel
		
		message.put(command.getFinalPortType());												// FinalStationType
		message.putInt(command.getShiftPosition());											// ShiftPosition
		// 2014.12.05 by KYK [DualOHT] TODO 티칭 사전 전송
		message.putInt(command.getRotatePosition());										// rotatePosition
		if (command.getPreSteeringNodeId().length() > 0) {
			message.putInt(Integer.parseInt(command.getPreSteeringNodeId(), 16));// PreSteeringNode
		} else {
			message.putInt(0);
		}
		// 2014.11.14 by KYK [DualOHT]
		message.put((byte) command.getCarrierPosition());
//		message.putShort((short) 30);																			// Tail(Length)
//		message.putShort((short) 31);																			// Tail(Length)
		message.putShort((short) 35);																			// Tail(Length)
		
		return registerSendData(message.array());
	}
	
	@Override
	public boolean sendIDResetCommand() {
		if (clientSocket == null) {
			return false;
		}

		return sendControlCommand(IDRESET_COMMAND, 0, 0, 0);
	}

	@Override
	public boolean sendIntersectionNodes(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}
//		
//		ArrayList<String> routedIntersectionNodeList = command.getRoutedIntersectionNodeList();
//		int nodeCount = routedIntersectionNodeList.size();
//		ByteBuffer message = ByteBuffer.allocate(17 + (nodeCount * 4));
//		
//		// Header(Tag:4/Length:2)
//		message.putInt(INTERSECTION_INFO_COMMAND);	// Header Tag
//		message.putShort((short) (nodeCount * 4)); 	// Header Length
//		
//		message.put((byte) command.getCommandId());											// CmdID(1)
//		message.putInt(Integer.parseInt(command.getTargetNode(), 16));	// TargetNodeID(4)
//		message.putInt(nodeCount);																			// NodeCount(4)
//		
//		for (int i = 0; i < routedIntersectionNodeList.size(); i++) {
//			message.putInt(Integer.parseInt(routedIntersectionNodeList.get(i), 16));	// NodeID(4)
//		}
//		
//		message.putShort((short) (nodeCount * 4)); 											// Header Length
		return true;
	}

	@Override
	public boolean sendLoadCommand(VehicleCommCommand command) {
		return sendWorkCommand(LOAD_COMMAND, command);
	}

	@Override
	public boolean sendRemoveCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		ByteBuffer message = ByteBuffer.allocate(13);

		// Header(Tag:4/Length:2)
		message.putInt(MTL_COMMAND); 	// Header Tag
		message.putShort((short) 5);	// Header Length

		// Body(Value)
		// CmdID(1), NodeID(4)
		message.put((byte) command.getCommandId());								// CmdID
		message.putInt(Integer.valueOf(command.getNodeId(), 16));	// StopNodeID
		message.putShort((short) 5);	// Tail(Length)

		return registerSendData(message.array());
	}

	@Override
	public boolean sendMapMakeCommand(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		ByteBuffer message = ByteBuffer.allocate(17);

		// Header(Tag:4/Length:2)
		message.putInt(MAPMAKER_COMMAND);	// Header Tag
		message.putShort((short) 9); 			// Header Length

		// Body(Value)
		// CmdID(1), EndNodeId(4), MapMakeSpeed(4)
		message.put((byte) command.getCommandId());								// CmdID
		message.putInt(Integer.valueOf(command.getNodeId(), 16));	// EndNodeId
		message.putInt(command.getMapMakeSpeed());								// MapMakeSpeed

		message.putShort((short) 9);			// Tail(Length)

		return registerSendData(message.array());
	}

	@Override
	public boolean sendPauseCommand() {
		if (clientSocket == null) {
			return false;
		}

		return sendControlCommand(PAUSE_COMMAND, 0, 0, 0);
	}

	@Override
	public boolean sendResumeCommand() {
		if (clientSocket == null) {
			return false;
		}

		return sendControlCommand(RESUME_COMMAND, 0, 0, 0);
	}

	protected boolean sendControlCommand(int type, int commandId, int param1, int param2) {
		if (clientSocket == null) {
			return false;
		}
		
		ByteBuffer message = ByteBuffer.allocate(11);
		// Header(Tag:4/Length:2)
		message.putInt(type);						// Header Tag
		message.putShort((short) 3);		// Header Length
		
		// Body(Value)
		// CmdID(1)
		message.put((byte) commandId);
		message.put((byte) param1);
		message.put((byte) param2);
		
		message.putShort((short) 3); 	// Tail Length
		
		return registerSendData(message.array());
	}
	
	@Override
	public boolean sendRouteInfoData(VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		String routeInfo = command.getRouteInfoData();
		ByteBuffer message = ByteBuffer.allocate(9 + routeInfo.length());

		// Header(Tag:4/Length:2)
		message.putInt(ROUTE_INFO_COMMAND);									// Header Tag
		message.putShort((short) (routeInfo.length() + 1));	// Header Length
		message.put((byte) 0);															// CmdID
		message.put(routeInfo.getBytes());
		message.putShort((short) (routeInfo.length() + 1));	// Tail Length

		return registerSendData(message.array());
	}

	@Override
	public boolean sendScanCommand(VehicleCommCommand command) {
		return sendWorkCommand(SCAN_COMMAND, command);
	}

	@Override
	public boolean sendUnloadCommand(VehicleCommCommand command) {
		return sendWorkCommand(UNLOAD_COMMAND, command);
	}
	
	@Override
	public boolean sendAutoCommand(){
		if (clientSocket == null) {
			return false;
		}

		return sendControlCommand(VEHICLEAUTO_COMMAND, 0, 0, 0);
	}
	
	protected boolean sendWorkCommand(int cmdType, VehicleCommCommand command) {
		if (clientSocket == null) {
			return false;
		}

		// 2014.10.30 by KYK [DualOHT] TODO
//		ByteBuffer message = ByteBuffer.allocate(37);
		ByteBuffer message = ByteBuffer.allocate(43);

		// Header(Tag:4/Length:2)
		message.putInt(cmdType);							// Header Tag
//		message.putShort((short) 29);					// Header Length
		message.putShort((short) 35);					// Header Length

		// Body(Value)
		// CmdID(1),CurrNodeID(4),CurrStationID(4),PortType(1),PortType(1),PIODirection(1)
		// Tposition_Hoist(4),Tposition_Shift(4),Tposition_Rotate(4)
		// HoistSpeedLevel(1),ShiftSpeedLevel(1),PIOTimeLevel(1),LookDownLevel(1)
		// ExtraOption(1):AutoRecovery(0),RFReader(1),StationSound(2),Oscillation(3),HandDetectEQ(4),Reserved(5~7)

		message.put((byte) command.getCommandId());											// CmdID
		message.putInt(Integer.valueOf(command.getNodeId(), 16));				// CurrNodeID
		if (command.getStationId().length() > 0) {
			message.putInt(Integer.valueOf(command.getStationId(), 16));	// CurrStationID
		} else {
			message.putInt(0);
		}

		message.put(command.getPortType());								// PortType
		message.put(command.getCarrierType());						// CarrierType
		message.put((byte) command.getPIODirection());		// PIODirection
		
		message.putInt(command.getHoistPosition());		 		// Tposition_Hoist
		message.putInt(command.getShiftPosition());		 		// Tposition_Shift
		message.putInt(command.getRotatePosition());			// Tposition_Rotate

		message.put((byte) command.getHoistSpeedLevel());	// HoistSpeedLevel
		message.put((byte) command.getShiftSpeedLevel());	// ShiftSpeedLevel
		message.put((byte) command.getPioTimeLevel());		// PIOTimeLevel
		message.put((byte) command.getLookDownLevel());		// LookDownLevel
		
		// AutoRecovery(0),RFReader(1),StationSound(2),Oscillation(3),HandDetectEQ(4),Reserved(5~7)
		message.put(command.getExtraOptionForWorkCommand());

		// TODO 2014.10.30 by KYK : carrierPosition 추가 및 TODO check
		message.put((byte) command.getCarrierPosition());
		if (command.getRfPioId().length() > 0) {
			message.putInt(Integer.valueOf(command.getRfPioId(), 16));			
		} else {
			message.putInt(0);			
		}
		message.put((byte) command.getRfPioCS());
		
//		message.putShort((short) 29);					// Tail Length
		message.putShort((short) 35);					// Tail Length

		return registerSendData(message.array());
	}
	
	private boolean sendCommandAck(int type, int commandId, int ackType, int ackCode) {
		if (clientSocket == null) {
			return false;
		}
		
		ByteBuffer message = ByteBuffer.allocate(14);
		// Header(Tag:4/Length:2)
		message.putInt(type);						// Header Tag
		message.putShort((short) 3);		// Header Length
		
		// Body(Value)
		// CmdID(1), AckType(1), AckCode(4)
		message.put((byte) commandId);
		message.put((byte) ackType);
		message.putInt(ackCode);
		
		message.putShort((short) 3); 	// Tail Length
		
		return registerSendData(message.array());
	}
	
	public boolean registerSendData(byte[] sendData) {
		if (sendMessageList.contains(sendData)) {
			return false;
		}
		
		sendMessageList.add(sendData);
		return true;
	}

}
