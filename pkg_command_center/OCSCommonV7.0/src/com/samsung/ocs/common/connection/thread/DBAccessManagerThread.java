package com.samsung.ocs.common.connection.thread;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.thread.AbstractOcsThread;

/**
 * DBAccessManagerThread Class, OCS 3.0 for Unified FAB
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

public class DBAccessManagerThread extends AbstractOcsThread {

	private DBAccessManager manager;
	
	/**
	 * Constructor of DBAccessManagerThread class.
	 */
	public DBAccessManagerThread(DBAccessManager manager) {
		this.manager = manager;
	}
	
	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = CommonConfig.getInstance().getReconnectIntervalMillis();
	}

	@Override
	protected void stopProcessing() {
		
	}

	@Override
	protected void mainProcessing() {
		manager.reconnectToDB();
	}
}
