package com.samsung.ocs.failover.job;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.DatagramPacket;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.udpu.UDPServerJob;

/**
 * RemoteCMCommandExecuteJob Class, OCS 3.0 for Unified FAB
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

public class RemoteCMCommandExecuteJob implements UDPServerJob {
	private String osName;
	private Logger logger;
	private String uocsScript;
	
	/**
	 * Constructor of RemoteCMCommandExecuteJob class.
	 */
	public RemoteCMCommandExecuteJob() {
		this.osName = System.getProperty("os.name").toUpperCase();
		logger = Logger.getLogger(OcsConstant.PROCMANAGE);
		this.uocsScript = System.getProperty("UOCS").trim();
	}
	
	/**
	 * UDP를 Listen하였을 때, 수행하는 콜백메서드.
	 */
	public void operation(DatagramPacket packet) {
		String recieveString = new String(packet.getData()).trim();
		
		if (recieveString.indexOf("UOCSCMD:") >= 0 && recieveString.length() > 8) {
			execScript(uocsScript+recieveString.substring(8));
		}
	}

	/**
	 * UDP를 Listen하였을때 Replay내용을 반환하는 콜백메서드.
	 */
	public byte[] getRelpyMessage() {
		return null;
	}
	
	/**
	 * 
	 * 
	 * @param script
	 * @return
	 */
	protected String execScript(String script) {
		String result = "";
		String command = "";
//		if (script.equals("") == true) {
		if ("".equals(script)) {
			log("Script is null");
			return "NG";
		}
		// 윈도우즈는 지원 안함.
		if (osName.indexOf("WINDOWS") >= 0) {
			log(" - GetScriptResult(" + script + ") : Windows is not Supported..");
			return "NG";
		}
		log(script);
		command = script;
		Runtime rt = Runtime.getRuntime();
		Process ps = null;
		try {
			ps = rt.exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			StringWriter writer = new StringWriter();
			String cl = null;
			while ((cl = reader.readLine()) != null) {
				writer.write(cl.trim()+"\n");
			}
			result = writer.toString();
			reader.close();
			ps.destroy();
		} catch (Exception e) {
			log("Could Not Execute The Command");
			log(e);
			return "NG";
		}
		log("<" + script + "> : " + result);
		return result;
	}
	
	protected void log(String string) {
//		logger.writeLog(logFileName, string, true);
		logger.debug(string);
	}
	
	protected void log(Throwable t) {
//		logger.writeLog(logFileName, t, true);
		logger.debug(t.getMessage(), t);
	}

}