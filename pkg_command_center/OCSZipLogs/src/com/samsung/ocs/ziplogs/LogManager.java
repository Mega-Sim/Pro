package com.samsung.ocs.ziplogs;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.thread.AbstractOcsThread;

/**
 * LogManager Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2015. 6. 17.
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

public class LogManager extends AbstractOcsThread {
	private String module = "";
	private String homePath = "";
	private String fileSeparator = File.separator;
	private String logPath = "";
	private static Logger exceptionLog = null;
	private SimpleDateFormat formatter = null;
	
	public LogManager(String module) {
		this.module = module;
		this.homePath = System.getProperty(OcsConstant.HOMEDIR);
		this.fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		this.logPath = homePath + fileSeparator + OcsConstant.LOG;
		this.exceptionLog = Logger.getLogger(module + OcsConstant.EXCEPTION);
		this.formatter = new SimpleDateFormat("yyyy-MM-dd");
	}

	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = 1000;
		this.start();
	}

	/**
	 * Main Processing Method.
	 */
	@Override
	protected void mainProcessing() {
		// TODO : logHoldingPeriod는 OCSINFO에서 가져오나 DB 사용을 하지 않기 위해 Max 15로 지정
		int logHoldingPeriod = 15;
		Calendar calendar = Calendar.getInstance();
		if (calendar.get(Calendar.MINUTE) == 0 && calendar.get(Calendar.SECOND) == 0) {
			calendar.add(Calendar.DATE, (-1) * logHoldingPeriod);
			String timeBefore = formatter.format(calendar.getTime());
			deleteLog(logPath, logHoldingPeriod, timeBefore);
		}
	}

	@Override
	protected void stopProcessing() {
		
	}
	
	/**
	 * Delete Old Logs Except Exception Logs
	 * 
	 * @param path
	 * @param logHoldingPeriod
	 * @param timeBefore
	 */
	private void deleteLog(String path, int logHoldingPeriod, String timeBefore) {
		File dir = new File(path);
		String[] files = dir.list();
		long lastModifiedTime;
		long storedPeriod;
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = new File(dir, files[i]);
				lastModifiedTime = file.lastModified();
				storedPeriod = (System.currentTimeMillis() - lastModifiedTime) / 24 / 3600 / 1000;
				try {
					if (file.isDirectory()) {
						StringBuilder logPathInfo = new StringBuilder();
						logPathInfo.append(path).append(fileSeparator).append(file.getName());
						deleteLog(logPathInfo.toString(), logHoldingPeriod, timeBefore);
					}
					if (file.isDirectory()) {
						if ((storedPeriod >= logHoldingPeriod || timeBefore.compareTo(file.getName()) > 0) && OcsConstant.EXCEPTION.equals(file.getName()) == false) {
							deleteLog(file);
						}
					} else {
						if (storedPeriod >= logHoldingPeriod) {
							deleteLog(file);
						}
					}
				} catch (Exception e) {
					traceException("deleteLog()", e);
				}
			}
		}
	}

	private static final String INITFILE_PATTERN = "^(([a-zA-Z0-9]|_|-)*[a-zA-Z]|([a-zA-Z0-9]|_|-)*[a-zA-Z][0-9]).log$";
	private static final Pattern initFilePattern = Pattern.compile(INITFILE_PATTERN);

	private boolean isInitFile(String fileName) {
		Matcher matcher = initFilePattern.matcher(fileName);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}

	private void deleteLog(File file) {
		if (isInitFile(file.getName()) == false) {
			file.delete();
		}
	}

	private void traceException(String message, Throwable t) {
		exceptionLog.error(String.format("%s", message), t);
	}
}
