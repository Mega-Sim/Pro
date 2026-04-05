package com.samsung.ocs.failover.policy;

import com.samsung.ocs.common.thread.AbstractUsingScriptOcsThread;
import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.failover.model.LocalProcess;

/**
 * AbstractPolicyThread Abstract Class, OCS 3.0 for Unified FAB
 * 
 * Policy를 Check하는 Thread에서 공통적으로 쓸 메서드를 정의한 Thread
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

public abstract class AbstractPolicyThread extends AbstractUsingScriptOcsThread {
	private String uocsScript;
	protected UDPSender remoteCommandSender;
	
	private static final String UOCS = "UOCS";
	private static final String NG = "NG";
	
	/**
	 * Constructor of AbstractPolicyThread class.
	 * 
	 * @param interval
	 * @param process
	 * @param sender
	 */
	public AbstractPolicyThread(long interval, LocalProcess process, UDPSender sender ) {
		super(interval, process.getProcessName());
		this.uocsScript = System.getProperty(UOCS).trim();
		this.remoteCommandSender = sender;
	}
	
	/**
	 * processName을 받아서 'uocs down processName' 스크립트를 실행해주는 스크립트
	 * 
	 * @param processName : uocs down 할 프로세스 명
	 * @return
	 */
	@Deprecated
	protected boolean uocsDown(String processName) {
		String rs = execScript(String.format("%s down %s", uocsScript, processName));
		if (NG.equals(rs)) {
			return false;
		}
		return true;
	}
	
	/**
	 * processName을 받아서 'uocs up processName' 스크립트를 실행해주는 스크립트
	 * 
	 * @param processName : uocs up 할 프로세스 명
	 * @return
	 */
	@Deprecated
	protected boolean uocsUp(String processName) {
		String rs = execScript(String.format("%s up %s", uocsScript, processName));
		if (NG.equals(rs)) {
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * 
	 * @param processName
	 */
	protected void remoteUocsUP(String processName) {
		if (remoteCommandSender != null) {
			remoteCommandSender.send(String.format("UOCSCMD: up %s", processName).getBytes());
		}
	}
	
	/**
	 * 
	 * 
	 * @param processName
	 */
	protected void remoteUocsDown(String processName) {
		if (remoteCommandSender != null) {
			remoteCommandSender.send(String.format("UOCSCMD: down %s", processName).getBytes());
		}
	}
}