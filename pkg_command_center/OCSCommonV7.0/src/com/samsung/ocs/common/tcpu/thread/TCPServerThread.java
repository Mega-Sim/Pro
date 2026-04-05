package com.samsung.ocs.common.tcpu.thread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.samsung.ocs.common.tcpu.TCPServerJob;

/**
 * TCPServerThread Class, OCS 3.0 for Unified FAB
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

public class TCPServerThread extends Thread {
	private boolean activeState = true;
	private ServerSocket ss = null;
	private TCPServerJob job;
	
	/**
	 * Constructor of TCPServerThread class.
	 * 
	 * @exception IOException
	 */
	public TCPServerThread(TCPServerJob job, int port) throws IOException {
		ss = new ServerSocket(port);
		this.job = job;
	}
	
	/**
	 * 
	 */
	public void run() {
		TCPServerJobThread t = null;
		while (activeState) {
			try {
				Socket socket = ss.accept();
				t = new TCPServerJobThread(socket, job);
				t.start();
			} catch (Exception ignore) {
				
			}
		}
		if (t != null) {
			t.setRunFlag(false);
		}
	}
	
	/**
	 * 
	 */
	public void stopTcpServer() {
		this.activeState = false;
	}
	
	/**
	 * 
	 * @param activeState
	 */
	public void setActiveState(boolean activeState) {
		this.activeState = activeState;
	}
}
