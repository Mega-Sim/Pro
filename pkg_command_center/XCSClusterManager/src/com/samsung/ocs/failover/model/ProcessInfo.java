package com.samsung.ocs.failover.model;

/**
 * ProcessInfo Class, OCS 3.0 for Unified FAB
 * 
 * Process의 pid와 processName을 가지는 Bean객체
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

public class ProcessInfo {
	private String processId;
	private String moduleName;
	
	/**
	 * Constructor of ProcessInfo class.
	 * 
	 * @param processId
	 * @param moduleName
	 */
	public ProcessInfo(String processId, String moduleName) {
		this.processId = processId;
		this.moduleName = moduleName;
	}
	
	/**
	 * processId getter
	 * @return String processId
	 */
	public String getProcessId() {
		return processId;
	}
	
	/**
	 * processId setter
	 * @param processId
	 */
	public void setProcessId(String processId) {
		this.processId = processId;
	}
	
	/**
	 * moduleName getter
	 * @return String moduleName
	 */
	public String getModuleName() {
		return moduleName;
	}
	
	/**
	 * moduleName setter
	 * @param moduleName
	 */
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
}
