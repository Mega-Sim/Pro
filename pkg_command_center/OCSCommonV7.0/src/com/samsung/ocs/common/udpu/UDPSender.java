package com.samsung.ocs.common.udpu;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.CommonLogFileName;

/**
 * UDPSender Class, OCS 3.0 for Unified FAB
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

public class UDPSender {
	private static Logger log = Logger.getLogger(CommonLogFileName.UDPSENDER);
	private String url;
	private int port;
	private DatagramSocket socket = null;
	private DatagramPacket packet;
	
	/**
	 * Constructor of UDPSender class.
	 * 
	 * @exception SocketException
	 * @exception UnknownHostException
	 */
	public UDPSender(String url, int port) throws SocketException, UnknownHostException {
		this.url = url;
		this.port = port;
		// DatagramSocket 积己
		this.socket = new DatagramSocket();
		packet = new DatagramPacket(" ".getBytes(), 1, InetAddress.getByName(url), port);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public boolean send(byte[] data) {
		boolean result = false;
		try {
			packet.setData(data);
			packet.setLength(data.length);
			// DatagramPacket 傈价
			socket.send(packet);
			result = true;
			log.debug("[SEND:"+url+":"+port+"]" + new String(data));
		} catch (UnknownHostException e) {
			traceException(e);
		} catch (SocketException e) {
			traceException(e);
		} catch (IOException e) {
			traceException(e);
		} finally {
			
		}
		return result;
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public boolean send2(byte[] data) {
		boolean result = false;
		try {
			packet.setData(data);
			packet.setLength(data.length);
			// DatagramPacket 傈价
			socket.send(packet);
			result = true;
		} catch (UnknownHostException e) {
			traceException(e);
		} catch (SocketException e) {
			traceException(e);
		} catch (IOException e) {
			traceException(e);
		} finally {
			
		}
		return result;
	}
	
	/**
	 * 
	 * @param data
	 * @param recieveBufferMaxSize
	 * @param timeOutMillies
	 * @return
	 */
	public byte[] sendAndRecieve(byte[] data, int recieveBufferMaxSize, int timeOutMillies) {
		byte[] result = null;
		try {
			// 傈价且 DatagramPacket 积己
//			DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(url), port);
			// DatagramPacket 傈价
			packet.setData(data);
			packet.setLength(data.length);
			
			socket.send(packet);
			
			log.debug("[SEND:"+url+":"+port+"]" + new String(data));
			
			//recieve timeout set
			socket.setSoTimeout(timeOutMillies);
			
			byte[] buf = new byte[recieveBufferMaxSize];
			
			DatagramPacket receivePacket = new DatagramPacket(buf, buf.length); 

			packet = null;
			
			socket.receive(receivePacket); 

			log.debug("[SEND:"+receivePacket.getAddress()+":"+receivePacket.getPort()+"]" + new String(receivePacket.getData()).trim());
			
			result = receivePacket.getData();
			
			receivePacket = null;
			
		} catch (UnknownHostException e) {
			traceException(e);
		} catch (SocketException e) {
			traceException(e);
		} catch (IOException e) {
			traceException(e);
		} finally {
			
		}
		return result;
	}
	
	/**
	 * 
	 */
	public void close() {
		try {socket.close();} catch (Exception ignore) {}
	}
	
	private static Logger commonExceptionLog = Logger.getLogger(CommonLogFileName.COMMONEXCEPTION);
	
	private void traceException(Exception e) {
		commonExceptionLog.error(String.format("[UDPSender] %s", e.getMessage()), e);
	}
}
