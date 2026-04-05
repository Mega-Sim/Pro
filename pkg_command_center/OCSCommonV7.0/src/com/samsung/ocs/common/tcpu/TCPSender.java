package com.samsung.ocs.common.tcpu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * TCPSender Class, OCS 3.0 for Unified FAB
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

public class TCPSender {
	private Socket socket = null;
	private InputStreamReader isr;
	private BufferedReader reader ;
	private PrintWriter writer;
	
	/**
	 * Constructor of TCPSender class.
	 * 
	 * @exception UnknownHostException
	 * @exception IOException
	 */
	public TCPSender(String url, int port) throws UnknownHostException, IOException {
		this.socket = new Socket(InetAddress.getByName(url), port);
		isr = new InputStreamReader(socket.getInputStream());
		reader = new BufferedReader(isr);
		writer = new PrintWriter(socket.getOutputStream());
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public String sendAndRecieve(String data) {
		String result = "";
		try {
			writer.println(data);
			writer.flush();
			result = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 
	 */
	public void close() {
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
}
