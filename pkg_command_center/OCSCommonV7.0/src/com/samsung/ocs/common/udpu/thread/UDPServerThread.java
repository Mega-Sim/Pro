package com.samsung.ocs.common.udpu.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.udpu.UDPServerJob;

/**
 * UDPServerThread Class, OCS 3.0 for Unified FAB
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

public class UDPServerThread extends Thread {
	private DatagramSocket socket;
	private int recieveBufferSize = 256;
	private boolean activeState = false;
	private static Logger log = Logger.getLogger(CommonLogFileName.UDPSERVERTHREAD);
	private UDPServerJob job;
	
	/**
	 * Constructor of UDPServerThread class.
	 * 
	 * @exception SocketException
	 */
	public UDPServerThread(int port, UDPServerJob job, int recieveBufferSize) throws SocketException {
		// DatagramPacket을 받기 위한 Socket 생성
		// 9999 : Listen할 Port
		socket = new DatagramSocket(port);
		// 수신 버퍼 사이즈
		this.recieveBufferSize = recieveBufferSize;
		
		activeState = true;
		this.job = job;
	}

	/**
	 * 
	 */
	public void run() {
		while (activeState) {
			try {
				// 데이터를 받을 버퍼
				byte[] inbuf = new byte[recieveBufferSize];

				// 데이터를 받을 Packet 생성
				DatagramPacket packet = new DatagramPacket(inbuf, inbuf.length);

				// 데이터 수신
				// 데이터가 수신될 때까지 대기됨
				socket.receive(packet);
				
				//처리함.
				InetAddress address = packet.getAddress(); 
		        int port = packet.getPort();
		        log.debug("[RECV:"+address+":"+port+"]"+new String(packet.getData()).trim());
		        job.operation(packet);
		        byte[] replyMessage = job.getRelpyMessage();
		        if (replyMessage != null) {
			        DatagramPacket sendPacket = new DatagramPacket(replyMessage, replyMessage.length, address, port);
		        	try {
						socket.send(sendPacket);
						log.debug("[SEND:"+address+":"+port+"]"+new String(replyMessage));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				traceException(e);
			}
		}
		if (socket != null) {
			try { socket.close(); } catch (Exception ignore) {}
		}
	}
	
	/**
	 * 
	 * @param activeState
	 */
	public void setActiveState(boolean activeState) {
		this.activeState = activeState;
	}
	
	/**
	 * 
	 */
	public void stopUdpServer() {
		this.activeState = false;
	}
	
	/**
	 * 
	 */
	public void closeSocket() {
		if (socket != null) {
			try { socket.close(); } catch (Exception ignore) {}
		}
		socket = null;
	}
	
	private static Logger commonExceptionLog = Logger.getLogger(CommonLogFileName.COMMONEXCEPTION);
	
	private void traceException(Exception e) {
		commonExceptionLog.error(String.format("[UDPServerThread] %s", e.getMessage()), e);
	}
}
