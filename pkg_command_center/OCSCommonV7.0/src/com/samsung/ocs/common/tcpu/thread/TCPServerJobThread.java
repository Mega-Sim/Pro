package com.samsung.ocs.common.tcpu.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.samsung.ocs.common.tcpu.TCPServerJob;

/**
 * TCPServerJobThread Class, OCS 3.0 for Unified FAB
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

public class TCPServerJobThread extends Thread{
	private Socket socket;
	private TCPServerJob job;
	private InputStreamReader isr;
	private BufferedReader reader ;
	private PrintWriter writer;
	
	private boolean runFlag = true;
	
	/**
	 * Constructor of TCPServerJobThread class.
	 * 
	 * @exception IOException
	 */
	public TCPServerJobThread(Socket socket, TCPServerJob job) throws IOException {
		this.socket = socket;
		this.job = job;
		
		isr = new InputStreamReader(socket.getInputStream());
		reader = new BufferedReader(isr);
		writer = new PrintWriter(socket.getOutputStream());
	}
	
	/**
	 * 
	 */
	public void run() {
		while (runFlag) {
			try {
				String str = reader.readLine();
				if (str == null) {
					break;
				}
				String ret = job.operation(str);
				writer.println(ret);
				writer.flush();
				
				//?
				sleep(10);
			} catch (Exception ignore) {
				ignore.printStackTrace();
				break;
			}
			
		}
		
		if (reader != null) {
			try { reader.close(); } catch (Exception ignore) {}
		}
		if (writer != null) {
			try { writer.close(); } catch (Exception ignore) {}
		}
		if (isr != null) {
			try { isr.close(); } catch (Exception ignore) {}
		}
		if (socket != null) {
			try { socket.close(); } catch (Exception ignore) {}
		}
	}

	/**
	 * 
	 * @param runFlag
	 */
	public void setRunFlag(boolean runFlag) {
		this.runFlag = runFlag;
	}
}
