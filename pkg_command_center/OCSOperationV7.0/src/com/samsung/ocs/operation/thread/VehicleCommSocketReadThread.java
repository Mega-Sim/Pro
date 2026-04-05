package com.samsung.ocs.operation.thread;

import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.operation.comm.VehicleComm;

/**
 * VehicleCommSocketReadThread Class, OCS 3.0 for Unified FAB
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

public class VehicleCommSocketReadThread extends AbstractOcsThread {
	private VehicleComm vehicleComm;
	
	/**
	 * Constructor of VehicleCommSocketReadThread class.
	 */
	public VehicleCommSocketReadThread(VehicleComm vehicleComm) {
		super();
		this.vehicleComm = vehicleComm;
		// 2014.10.23 by MYM : Thread Name¿ª º≥¡§
		setName(this.getClass().getSimpleName() + "_" + vehicleComm.getTargetName());
	}

	/**
	 * 
	 */
	@Override
	public String getThreadId() {
		// 2012.03.09 by PMM
//		return VehicleCommSocketReadThread.class.getName();
		return this.getClass().getName();
	}

	/**
	 * 
	 */
	@Override
	protected void initialize() {
		interval = 10;
	}

	/**
	 * 
	 */
	@Override
	protected void mainProcessing() {
		vehicleComm.readSocketProcess();
	}

	/**
	 * 
	 */
	@Override
	protected void stopProcessing() {
		
	}
}
