package com.samsung.ocs.common.tcpu;

import com.samsung.ocs.common.tcpu.thread.TCPServerThread;

/**
 * TCPServer Class, OCS 3.0 for Unified FAB
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

public class TCPServer {
	private TCPServerJob job;
	private int port;
	private TCPServerThread thread;
	
	/**
	 * Constructor of TCPServer class.
	 */
	public TCPServer(TCPServerJob job, int port) {
		this.job = job;
		this.port = port;
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized boolean start() {
		if (thread == null) {
			try {
				thread = new TCPServerThread(job, port);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (thread != null) {
				thread.start();
				return true;
			} 
		} else {
			System.out.println("Thread was already started.");
		}
		return false;
	}
	
	/**
	 * 
	 */
	public synchronized void stop() {
		if (thread != null) {
			thread.setActiveState(false);
		}
	}
}
