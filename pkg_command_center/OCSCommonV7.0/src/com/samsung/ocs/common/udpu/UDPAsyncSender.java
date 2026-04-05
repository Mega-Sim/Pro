package com.samsung.ocs.common.udpu;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Vector;

import com.samsung.ocs.common.udpu.thread.UDPAsyncSenderThread;

/**
 * UDPAsyncSender Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2014. 6. 18.
 * @version 3.1
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class UDPAsyncSender {
	
	private DatagramSocket socket = null;
	private DatagramPacket packet;
	private Vector<byte[]> sendDataList = null;
	private UDPAsyncSenderThread thread = null;
	
	/**
	 * Constructor of UDPAsyncSender class.
	 * 
	 * @exception SocketException
	 * @exception UnknownHostException
	 */
	public UDPAsyncSender(String url, int port) throws SocketException, UnknownHostException {
		this.socket = new DatagramSocket();
		packet = new DatagramPacket(" ".getBytes(), 1, InetAddress.getByName(url), port);
		sendDataList = new Vector<byte[]>();
		thread = new UDPAsyncSenderThread(socket, packet, url, port, sendDataList, 500);
		thread.start();
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public void send(byte[] data) {
		sendDataList.add(data);
	}
	
	/**
	 * 
	 */
	public void close() {
		thread.stopThread();
		try {socket.close();} catch (Exception ignore) {}
	}
	
}
