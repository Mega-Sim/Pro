package com.samsung.ocs.optimizer;

import com.samsung.ocs.common.OCSMain;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant.INIT_STATE;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.failovercomm.ClusterExecuter;

/**
 * OptimizerMain Class, OCS 3.0 for Unified FAB
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

public class OptimizerMain implements OCSMain {
	private OptimizerManager optimizerManager;
	private static final String MODULE_NAME = "optimizer";
	
	private static final String VERSION = "VERSION";
	private static final String BUILDID = "BUILDID";
	
	/**
	 * Constructor of OptimizerMain class.
	 */
	public OptimizerMain() {
		initialize();
		new ClusterExecuter(this);
	}
	
	/**
	 * 
	 */
	private void initialize() {
		// DB 생성
		DBAccessManager dbAccessManager = new DBAccessManager();
		// DB 접속이 안되는 경우 대기를 하다가 DB 접속이 OK일 때 아래가 실행이 되어야 함. --> 추후 처리하도록 함.
		
		optimizerManager = new OptimizerManager();
		optimizerManager.start();
	}

	/**
	 * Optimizer 모듈의 main Method.
	 * @param args
	 */
	public static void main(String[] args) {
		new OptimizerMain();
	}

	/**
	 * 
	 */
	public boolean activate() {
		optimizerManager.requestChangeServiceState(MODULE_STATE.REQINSERVICE);
		return true;
	}

	/**
	 * 
	 */
	public boolean deactivate() {
		optimizerManager.requestChangeServiceState(MODULE_STATE.REQOUTOFSERVICE);
		return true;
	}

	/**
	 * 
	 */
	public String getModuleName() {
		return MODULE_NAME;
	}
	
	/**
	 * 
	 */
	public INIT_STATE getModuleInitState() {
		return INIT_STATE.INIT_BEGINED;
	}

	/**
	 * 
	 */
	public MODULE_STATE getModuleState() {
		return optimizerManager.getServiceState();
	}
	
	/**
	 * 
	 */
	public String getVersion() {
		return com.samsung.ocs.VersionInfo.getString(VERSION);
	}
	
	/**
	 * 
	 */
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
