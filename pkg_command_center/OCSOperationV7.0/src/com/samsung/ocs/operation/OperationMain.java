package com.samsung.ocs.operation;

import com.samsung.ocs.common.OCSMain;
import com.samsung.ocs.common.constant.OcsConstant.INIT_STATE;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.failovercomm.ClusterExecuter;

/**
 * OperationMain Class, OCS 3.0 for Unified FAB
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

public class OperationMain implements OCSMain {
	private OperationManager operationManager;
	
	private static final String MODULE_NAME = "operation";
	private static final String VERSION = "VERSION";
	private static final String BUILDID = "BUILDID";

	/**
	 * Constructor of OperationMain class.
	 */
	public OperationMain() {
		initialize();
	}
	
	/**
	 * Initialize OperationMain
	 */
	private void initialize() {
		// FailOverLuncher 생성 후 리턴값을 받아 InService Node인 경우 Service 되도록 고려
		new ClusterExecuter(this);
		
		// OperationMangerControl 생성
		operationManager = new OperationManager();
		operationManager.start();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new OperationMain();
	}
	
	/**
	 * Request to Change to INSERVICE
	 */
	public boolean activate() {
		if (operationManager == null) { 
			return false;
		}
		operationManager.setRequestedServiceState(MODULE_STATE.REQINSERVICE);
		return true;
	}

	/**
	 * Request to Change to OUTOFSERVICE
	 */
	public boolean deactivate() {
		if (operationManager == null) { 
			return false;
		}
		
		operationManager.setRequestedServiceState(MODULE_STATE.REQOUTOFSERVICE);
		return true;
	}
	
	public MODULE_STATE getModuleState() {
		if (operationManager == null) { 
			return MODULE_STATE.OUTOFSERVICE;
		}
		return operationManager.getServiceState();
	}

	public String getModuleName() {
		return MODULE_NAME;
	}
	
	public INIT_STATE getModuleInitState() {
		return INIT_STATE.INIT_COMPLETED;
	}
	
	// 2011.11.02 by PMM
	// StartupHistory.log에 Version, BuildId, Include 정보 출력.
	public String getVersion() {
		return com.samsung.ocs.VersionInfo.getString(VERSION);
	}
	public String getBuildId(){
		return com.samsung.ocs.VersionInfo.getString(BUILDID);
	}
	public String getIncludeInfo(){
		StringBuilder include = new StringBuilder();
		include.append("[").append(com.samsung.ocs.common.VersionInfo.getString(BUILDID)).append("]");
		include.append("[").append(com.samsung.ocs.manager.VersionInfo.getString(BUILDID)).append("]");
		include.append("[").append(com.samsung.ocs.route.VersionInfo.getString(BUILDID)).append("]");
		include.append("[").append(com.samsung.ocs.failovercomm.VersionInfo.getString(BUILDID)).append("]");
		return include.toString();
	}
}
