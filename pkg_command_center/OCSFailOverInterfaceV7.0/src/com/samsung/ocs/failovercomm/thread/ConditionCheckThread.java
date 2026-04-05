package com.samsung.ocs.failovercomm.thread;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.OCSMain;
import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.FAILOVER_POLICY;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.failovercomm.config.ModuleConfig;
import com.samsung.ocs.failovercomm.dao.OcsProcessStateDAO;
import com.samsung.ocs.failovercomm.dao.OcsProcessStateVO;

/**
 * ConditionCheckThread Class, OCS 3.0 for Unified FAB
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

public class ConditionCheckThread extends AbstractOcsThread {
	private OCSMain main;
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTEREXECUTER);
	private DBAccessManager dbAccessManager;
	private boolean isPrimary = false;
	private String hostType;
	private int unknownCount = 0;
	private int autoTakeOverTimeoutCount = 0;
	private FAILOVER_POLICY policy = FAILOVER_POLICY.NETDBBASE;
	
	/**
	 * Constructor of ConditionCheckThread class.
	 * 
	 * @exception SocketException
	 * @exception UnknownHostException
	 */
	public ConditionCheckThread(ModuleConfig config, DBAccessManager dbAccessManager, CommonConfig commonConfig, OCSMain main) throws SocketException, UnknownHostException {
		super(500);
		this.main = main;
		this.dbAccessManager = dbAccessManager;
		this.hostType = commonConfig.getHostServiceType();
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(commonConfig.getHostServiceType());
		this.autoTakeOverTimeoutCount = config.getAutoTakeOverTimeoutCount();
		this.policy = config.getFailoverPolicy();
	}
	
	protected void initialize() {
	}

	protected void stopProcessing() {
		
	}

	/**
	 * 
	 */
	protected void mainProcessing() {
		MODULE_STATE hostState = main.getModuleState();
		String processName = main.getModuleName().toLowerCase();
		// ÀÏ´Ü ÇöÀç»óÅÂ DB¿¡ ¾²°í!! ÇÏÁö¸»°í ³»»óÅÂ¶û db¶û ´Ù¸¦¶§¸¸ ½áº¸ÀÚ ÈåÈåÈå
		
		OcsProcessStateVO ops = null;
		switch (policy) {
			case VIPBASE : 
			case NETDBBASE :
				ops = OcsProcessStateDAO.retrieveProcessState(processName, dbAccessManager, isPrimary);
				break;
			case ANYHOST : 
			default :
				break;
		}
		
		if (ops != null && hostState != ops.getHostState()) {
			OcsProcessStateDAO.updateProcessState(dbAccessManager, isPrimary, hostState, processName);
		}
		
		switch (policy) {
			case ANYHOST : 
				if (hostState != MODULE_STATE.INSERVICE ) {
					main.activate();
				}
				break;
			case VIPBASE : 
			case NETDBBASE :
			default :
				if (ops != null) {
					MODULE_STATE host = ops.getHostState();
					MODULE_STATE remote = ops.getRemoteState();
					log(ops.toString());
					log("  - REMOTE UNKNOWN COUNT ["+unknownCount+"]");
					if (hostType.equalsIgnoreCase(ops.getInserviceHost())) {
						//host == inservice_host
						switch (host) {
							case INSERVICE :
								unknownCount = 0;
								// À¯Áö
								if (remote == MODULE_STATE.INSERVICE) {
									log("ERROR!! HOST and REMOTE is INSERVICE at same time..!! ");
								}
								break;
							case OUTOFSERVICE :
								if (remote == MODULE_STATE.OUTOFSERVICE ) {
									unknownCount=0;
									main.activate();
								} else if (remote == MODULE_STATE.UNKNOWN) {
									unknownCount++;
									if (unknownCount > autoTakeOverTimeoutCount) {
										main.activate();
										unknownCount = 0;
									}
								}
								break;
							case UNKNOWN :
							default :
								unknownCount=0;
								// ÀÌ·²¶© ¸ô ÇØ¾ßÇÒ±î..
								break;
						}
					} else {
						unknownCount = 0;
						//host != inservice_host
						switch (host) {
							case INSERVICE :
//							case REQINSERVICE :
							case REQOUTOFSERVICE :
								main.deactivate();
								break;
							case OUTOFSERVICE :
								break;
							case UNKNOWN :
							default :
								break;
						}
					}
				} else {
					log("retrieveProcessState IS NULL");
				}
				break;
		}
	}
	
	/**
	 * 
	 */
	public String getThreadId() {
		return this.getClass().getName();
	}

	private void log(String log) {
		logger.debug(log);
	}
}
