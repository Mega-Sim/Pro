package com.samsung.ocs.common.udpu.thread;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.thread.AbstractOcsThread;

/**
 * UDPAsyncSenderThread Class, OCS 3.0 for Unified FAB
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

public class UDPAsyncSenderThread extends AbstractOcsThread {

	private static Logger log = Logger.getLogger(CommonLogFileName.UDPSENDER);
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	private DatagramSocket socket = null;
	private DatagramPacket packet = null;
	private Vector<byte[]> sendDataList = null;
	private String url = null;
	private int port = 0;
	
	/**
	 * Constructor of UDPServerThread class.
	 * @param port 
	 * @param url 
	 * 
	 * @exception SocketException
	 */
	public UDPAsyncSenderThread(DatagramSocket socket, DatagramPacket packet, String url, int port, Vector<byte[]> sendDataList, int interval) {
		super(interval);
		this.socket = socket;
		this.packet = packet;
		this.url = url;
		this.port = port;
		this.sendDataList = sendDataList;
	}
	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#getThreadId()
	 */
	@Override
	public String getThreadId() {
		// TODO Auto-generated method stub
		return UDPAsyncSenderThread.class.getName();
	}
	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#initialize()
	 */
	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		
	}
	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#stopProcessing()
	 */
	@Override
	protected void stopProcessing() {
		// TODO Auto-generated method stub
		
	}
	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#mainProcessing()
	 */
	@Override
	protected void mainProcessing() {
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		long startTime = System.currentTimeMillis();
		log.debug(String.format("[ASEND:" + url + ":" + port + "] process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		int sendCnt = 0;
		try {
			int sendSize = sendDataList.size();
			for (int i = 0; i < sendSize; i++) {
				byte[] data = sendDataList.get(i);
				packet.setData(data);
				packet.setLength(data.length);
				// DatagramPacket 전송
				socket.send(packet);
				log.debug("[ASEND:" + url + ":" + port + "]" + new String(data));
				sendCnt++;
			}
		} catch (Exception e) {
			traceException(e);
		} finally {

		}
		for (int i = 0; i < sendCnt; i++) {
			sendDataList.remove(0);
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log.debug(String.format("[ASEND:" + url + ":" + port + "] process completed. elapsedTime[%d]millis", elapsedTime));
	}
	
//	boolean result = false;
//	try {
//		packet.setData(data);
//		packet.setLength(data.length);
//		// DatagramPacket 전송
//		socket.send(packet);
//		result = true;
//		log.debug("[SEND:"+url+":"+port+"]" + new String(data));
//	} catch (UnknownHostException e) {
//		traceException(e);
//	} catch (SocketException e) {
//		traceException(e);
//	} catch (IOException e) {
//		traceException(e);
//	} finally {
//		
//	}
//	return result;
	
	
	private static Logger commonExceptionLog = Logger.getLogger(CommonLogFileName.COMMONEXCEPTION);
	
	private void traceException(Exception e) {
		commonExceptionLog.error(String.format("[UDPAsyncSender] %s", e.getMessage()), e);
	}

}
