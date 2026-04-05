package com.samsung.ocs.ziplogs;

import com.samsung.ocs.common.OCSMain;
import com.samsung.ocs.common.constant.OcsConstant.INIT_STATE;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.failovercomm.ClusterExecuter;

/**
 * CommonConfig Class, OCS 3.0 for Unified FAB
 * 
 * @author Byoungsoo.Kim
 * 
 * @date   2014. 7. 01.
 * @version 3.0
 * 
 * Copyright 2014 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class ZipLogsMain implements OCSMain {
	private ZipLogsManager zipLogsManager;

	private static final String VERSION = "VERSION";
	private static final String BUILDID = "BUILDID";
	private static final String MODULE_NAME = "ziplogs";
	
	public ZipLogsMain() {
		initialize();
		new ClusterExecuter(this);
	}
	
	private void initialize() {
		zipLogsManager = new ZipLogsManager();
		zipLogsManager.start();
	}

	/**
	 * ZipLogs ¸ðµâÀÇ main Method.
	 * @param args
	 */
	public static void main(String[] args) {
		new ZipLogsMain();
	}

	public boolean activate() {
		if (zipLogsManager == null) {
			return false;
		}

		zipLogsManager.requestChangeServiceState(MODULE_STATE.REQINSERVICE);
		return true;
	}

	public boolean deactivate() {
		if (zipLogsManager == null) {
			return false;
		}

		zipLogsManager.requestChangeServiceState(MODULE_STATE.REQOUTOFSERVICE);
		return true;
	}

	public String getModuleName() {
		return MODULE_NAME;
	}
	
	public INIT_STATE getModuleInitState() {
		return INIT_STATE.INIT_BEGINED;
	}

	public MODULE_STATE getModuleState() {
		return zipLogsManager.getServiceState();
	}
	
	public String getVersion() {
		return com.samsung.ocs.VersionInfo.getString(VERSION);
	}

	public String getBuildId() {
		return com.samsung.ocs.VersionInfo.getString(BUILDID);
	}

	public String getIncludeInfo() {
		StringBuilder include = new StringBuilder();
		include.append("[").append(com.samsung.ocs.common.VersionInfo.getString(BUILDID)).append("]");
		include.append("[").append(com.samsung.ocs.failovercomm.VersionInfo.getString(BUILDID)).append("]");
		return include.toString();
	}
	
}
