package com.samsung.ocs.common.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;

/**
 * AbstractUsingScriptOcsThrad Abstract Class, OCS 3.0 for Unified FAB
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

public abstract class AbstractUsingScriptOcsThread extends AbstractOcsThread {
	private String osName;
	protected Logger logger;
	
	protected static final String NG = "NG";
	protected static final String WINDOWS = "WINDOWS";
	
	/**
	 * Constructor of AbstractUsingScriptOcsThread abstract class.
	 */
	public AbstractUsingScriptOcsThread(long interval, String logfileName) {
		super(interval);
		this.logger = Logger.getLogger(logfileName);
		this.osName = System.getProperty("os.name").toUpperCase();
	}

	/**
	 * 
	 * @param script
	 * @return
	 */
	protected String execScript(String script) {
		String result = "";
		String command = "";
		if (script == null || script.length() == 0) {
			log("Script is null");
			return NG;
		}

		log("Script : " + script);
		if (osName.indexOf(WINDOWS) >= 0) {
			log("    Windows is not Supported.");
			return NG;
		}
		command = script;
		Runtime rt = Runtime.getRuntime();
		Process ps = null;
		BufferedReader reader = null;
		try {
			ps = rt.exec(command);
			reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
			StringWriter writer = new StringWriter();
			String cl = null;
			while ((cl = reader.readLine()) != null) {
				writer.write(cl.trim() + "\n");
			}
			result = writer.toString();
			reader.close();
			ps.destroy();
		} catch (Exception e) {
			log("    Could Not Execute The Command.");
			log(e);
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			if(ps != null) {
				ps.destroy();
			}
			return NG;
		} catch (Throwable t) {
			log("    Could Not Execute The Command.");
			log(t);
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			if(ps != null) {
				ps.destroy();
			}
			return NG;
		}
		return result;
	}
	
	protected void log(String string) {
//		logger.writeLog(logFileName, string, true);
		logger.debug(string);
	}
	
	protected void log(Throwable t) {
//		logger.writeLog(logFileName, t, true);
//		logger.error(t.getMessage(), t);
		logger.error(String.format("[%s]", getThreadId()), t);
	}
}