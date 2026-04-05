package com.samsung.ocs.stbc;

import com.samsung.ocs.common.OCSMain;
import com.samsung.ocs.common.constant.OcsConstant.INIT_STATE;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.failovercomm.ClusterExecuter;

/**
 * STBCMain Class, OCS 3.0 for Unified FAB
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

public class STBCMain  implements OCSMain{
	STBCManager stbcManager;
	private static final String VERSION = "VERSION";
	private static final String BUILDID = "BUILDID";
	
	/**
	 * Main Method of STBCMain class.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {		
		new STBCMain();
	}
	
	/**
	 * Constructor of STBCMain class.
	 */
	public STBCMain(){
		initialize();	
		new ClusterExecuter(this);
	}

	/*******************************************
	 * method for interface implementation
	 *******************************************/
	
	private void initialize() {
		stbcManager = STBCManager.getInstance();
		stbcManager.start();
	}

	/**********************************
	 * Implements methods for OCSMain
	 ***********************************/
	
	public boolean activate() {
		stbcManager.changeRequestedServiceState(MODULE_STATE.REQINSERVICE);
		return true;
	}

	public boolean deactivate() {
		stbcManager.changeRequestedServiceState(MODULE_STATE.REQOUTOFSERVICE);
		return true;	
	}

	public String getModuleName() {
		return "stbc";
	}

	public MODULE_STATE getModuleState() {
		return stbcManager.getServiceState();
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
		include.append("[").append(com.samsung.sem.VersionInfo.getString(BUILDID)).append("]");
		include.append("[").append(com.samsung.ocs.failovercomm.VersionInfo.getString(BUILDID)).append("]");
		return include.toString();
	}
}
