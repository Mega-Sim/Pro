package com.samsung.ocs.common.thread;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.CommonLogFileName;

/**
 * AbstractOcsThread Abstract Class, OCS 3.0 for Unified FAB
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

public abstract class AbstractOcsThread extends Thread {
	public abstract String getThreadId();
	protected abstract void initialize();
	protected abstract void stopProcessing();
	protected abstract void mainProcessing();
	
	protected long interval = 1000;
	protected long sleepCount = 0;
	private boolean stopFlag = false;
	
	protected boolean initialized = false;
	
	protected static Logger commonExceptionLog = Logger.getLogger(CommonLogFileName.COMMONEXCEPTION);
	
	/**
	 * Constructor of AbstractOcsThread abstract class.
	 */
	protected AbstractOcsThread() {
		// 2014.10.23 by MYM : Thread Name을 설정
		setName(this.getClass().getSimpleName());
		initialize();
	}
	
	protected AbstractOcsThread(long interval) {
		this();
		this.interval = interval;
	}
	
	/**
	 * 
	 */
	public void run() {
		while (true) {
			try {
				lastExecutedTime = System.currentTimeMillis();
				mainProcessing();
			} catch (Exception e) {
				traceException(e);
			} catch (Throwable t) {
				traceException(t);
			}
			
			try {
				sleep(interval);
				sleepCount++;
			} catch (InterruptedException e) {
				traceException(e);
			}
			if (stopFlag) {
				break;
			}
		}
		stopProcessing();
	}
	
	public void stopThread() {
		this.stopFlag = true;
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	protected void traceException(Throwable t) {
		commonExceptionLog.error(String.format("[%s]", getThreadId()), t);
	}
	
	private long lastExecutedTime = System.currentTimeMillis();
	public long getLastExecutedTime() {
		return lastExecutedTime;
	}
	
	// 2014.11.13 by zzang9un : Thread 수행 예상 시간 limit(지연시간 측정을 위해)
	protected long elapsedTimeLimit = 10000;

	public long getElapsedTimeLimit() {
		return elapsedTimeLimit;
	}
	
	public void setElapsedTimeLimit(long elapsedTimeLimit) {
		this.elapsedTimeLimit = elapsedTimeLimit;
	}
}