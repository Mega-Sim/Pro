package com.samsung.ocs.failover.thread;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.failover.dao.OcsHistoryManager;

/**
 * OcsHistoryManagerThread Class, OCS 3.0 for Unified FAB
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

public class OcsHistoryManagerThread extends AbstractOcsThread {
	private OcsHistoryManager manager;
	
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of OcsHistoryManagerThread class.
	 */
	public OcsHistoryManagerThread(OcsHistoryManager manager) {
		super(1000);
		this.manager = manager;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#getThreadId()
	 */
	@Override
	public String getThreadId() {
		return "OCSEVENTHISTORYMANAGER_THREAD";
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#initialize()
	 */
	@Override
	protected void initialize() {
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#stopProcessing()
	 */
	@Override
	protected void stopProcessing() {
	}

	/**
	 * Main Processing Method
	 */
	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#mainProcessing()
	 */
	@Override
	protected void mainProcessing() {
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		long startTime = System.currentTimeMillis();
		log(String.format("OcsHistoryManagerThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		if(manager.isInitialized()) {
			manager.processClusterHistory();
			manager.processClusterInfo();
			manager.processCheckDupClusterHistory();
		} else {
			manager.setInitialized(manager.init());
		}

		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("OcsHistoryManagerThread process completed. elapsedTime[%d]millis", elapsedTime));
	}
	
	protected void log(String log) {
		logger.debug(log);
	}
	
	protected void log(Throwable w) {
		logger.debug(w.getMessage(), w);
	}
}
