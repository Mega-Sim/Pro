package com.samsung.ocs.common.thread;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.CommonLogFileName;

/**
 * AbstractOcsWorkThread Abstract Class, OCS 3.0 for Unified FAB
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

public abstract class AbstractOcsWorkThread extends Thread {
	public abstract String getThreadId();
	protected abstract void initialize();
	protected abstract void stopProcessing();
	protected abstract void mainProcessing();
	
	protected long interval = 1000;
	protected long sleepCount = 0;
	private boolean stopFlag = false;
	public boolean runFlag = false;
	
	private boolean hasInterval = false;

	/**
	 * Constructor of AbstractOcsWorkThread abstract class.
	 */
	protected AbstractOcsWorkThread() {
		initialize();
	}
	
	protected AbstractOcsWorkThread(long interval) {
		initialize();
		this.interval = interval;
	}
	
	/**
	 * 
	 */
	public void run() {
		while (true) {
			hasInterval = false;
			while (runFlag) {
				try {
					mainProcessing();
				} catch (Exception e) {
					traceException(e);
				} catch (Throwable t) {
					traceException(t);
				}
				
				try {
					sleep(interval);
					hasInterval = true;
					sleepCount++;
				} catch (InterruptedException e) {
					traceException(e);
				}
				if (stopFlag) {
					break;
				}
			}
			if (stopFlag) {
				stopProcessing();
				break;
			}
			if (hasInterval == false) {
				try {
					sleep(interval);
					hasInterval = true;
				} catch (InterruptedException e) {
					traceException(e);
				}
			}
		}
	}
	
	/**
	 * 
	 * @param runFlag
	 */
	public void setRunFlag(boolean runFlag) {
		this.runFlag = runFlag;
	}
	
	/**
	 * 
	 */
	public void stopThread() {
		this.stopFlag = true;
	}
	
	protected static Logger commonExceptionLog = Logger.getLogger(CommonLogFileName.COMMONEXCEPTION);
	
	protected void traceException(Throwable t) {
		commonExceptionLog.error(String.format("[%s]", getThreadId()), t);
	}
}
