package com.samsung.ocs.ibsem.stbc.thread;

import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.ibsem.stbc.STBReportComm;

public class STBReportCommSocketReadThread extends AbstractOcsThread {
	
	private STBReportComm stbReportComm = null;
	
	/**
	 * Constructor of STBCReportCommSocketReadThread class.
	 */
	public STBReportCommSocketReadThread(STBReportComm stbcReportComm) {
		super();
		this.stbReportComm = stbcReportComm;
	}

	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = 100;
	}

	@Override
	protected void mainProcessing() {
		stbReportComm.readSocketProcess();
	}

	@Override
	protected void stopProcessing() {
		stbReportComm.closeClientSocket();
	}
	
}
