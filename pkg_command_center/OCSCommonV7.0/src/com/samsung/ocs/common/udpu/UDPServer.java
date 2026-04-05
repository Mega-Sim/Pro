package com.samsung.ocs.common.udpu;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.udpu.thread.UDPServerThread;

/**
 * UDPServer Class, OCS 3.0 for Unified FAB
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

public class UDPServer {
	private UDPServerJob job;
	private int port;
	private UDPServerThread thread;
	
	private int recieveBufferSize = 256;
	
	/**
	 * Constructor of UDPServer class.
	 */
	public UDPServer(UDPServerJob job, int port) {
		this.job = job;
		this.port = port;
	}
	
	/**
	 * 
	 * @param recieveBufferSize
	 */
	public void setRecieveBufferSize(int recieveBufferSize) {
		this.recieveBufferSize = recieveBufferSize;
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized boolean start() {
		if (thread == null) {
			try {
				thread = new UDPServerThread(port, job, recieveBufferSize);
			} catch ( Exception e) {
				traceException(e);
				return false;
			}
			if (thread != null) {
				thread.start();
				return true;
			} 
		} else {
//			System.out.println("Thread was already started.");
			commonExceptionLog.debug("[UDPServer] Thread was already started.");
		}
		return false;
	}
	
	/**
	 * 
	 */
	public synchronized void stop() {
		if (thread != null) {
			thread.closeSocket();
			thread.setActiveState(false);
		}
	}
	
	private static Logger commonExceptionLog = Logger.getLogger(CommonLogFileName.COMMONEXCEPTION);
	
	private void traceException(Exception e) {
		commonExceptionLog.error(String.format("[UDPServer] %s", e.getMessage()), e);
	}
}
