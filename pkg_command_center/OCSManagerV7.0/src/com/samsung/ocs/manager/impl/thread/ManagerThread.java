package com.samsung.ocs.manager.impl.thread;

import com.samsung.ocs.common.thread.AbstractOcsWorkThread;
import com.samsung.ocs.manager.IManager;
import com.samsung.ocs.manager.impl.AbstractManager;

/**
 * ManagerThread Class, OCS 3.0 for Unified FAB
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

public class ManagerThread extends AbstractOcsWorkThread {
	IManager manager;
	
	/**
	 * Constructor of VersionInfo class.
	 */
	public ManagerThread(IManager manager) {
		super(2000);
		this.manager = manager;
	}
	
	/**
	 * Constructor of VersionInfo class.
	 */
	public ManagerThread(AbstractManager manager, long interval) {
		super(interval);
		this.manager = manager;
		// 2014.10.23 by MYM : Thread Name¿ª º≥¡§
		setName(manager.getClass().getSimpleName());
	}
	
	@Override
	public String getThreadId() {
		// 2012.03.09 by PMM
//		return this.getName();
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
	}

	@Override
	protected void mainProcessing() {
		manager.update();
	}

	@Override
	protected void stopProcessing() {
	}
}
