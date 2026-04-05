package com.samsung.ocs.stbc.rfc.thread;

import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.stbc.rfc.RFCManager;

/**
 * RFCManagerThread Class, OCS 3.0 for Unified FAB
 * 
 * RFCManager에서 주기적으로 처리해야 하는 로직을 구현한 Thread
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

public class RFCManagerThread extends AbstractOcsThread {
	private RFCManager manager;
	
	/**
	 * Constructor of RFCManagerThread class.
	 * 
	 * @param manager
	 */
	public RFCManagerThread(RFCManager manager) {
		super(300);
		this.manager = manager;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#getThreadId()
	 */
	@Override
	public String getThreadId() {
		return this.getClass().getName();
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

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#mainProcessing()
	 */
	/**
	 * Main Processing Method
	 */
	@Override
	protected void mainProcessing() {
		manager.processingEvent();
	}
}
