package com.samsung.ocs.failover.thread;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.thread.AbstractUsingScriptOcsThread;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterInfoItem.MODE;
import com.samsung.ocs.failover.model.ClusterState;

/**
 * VipCheckThread Class, OCS 3.0 for Unified FAB
 * 
 * VIP의 상태를 주기적으로 체크하여 그 결과로 ClusterState 객체를 갱신하는 Thread
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

public class VipCheckThread extends AbstractUsingScriptOcsThread {
	private ClusterState clusterState;
	private String vipCheckScrip;
	
	private boolean isPrimary = true;
	private DBAccessManager dbam;
	private int initCount = 0;
	private OcsHistoryManager historyManager = null;
	
	private static int retryCount = 0;
	
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of VipCheckThread class.
	 * 
	 * @param clusterState
	 * @param dbam
	 * @param historyManager
	 */
	public VipCheckThread(ClusterState clusterState, DBAccessManager dbam, OcsHistoryManager historyManager) {
		super(1000, OcsConstant.PROCMANAGE);
		this.clusterState = clusterState;
		this.vipCheckScrip = System.getProperty("VIPC").trim();
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(CommonConfig.getInstance().getHostServiceType());
		this.dbam = dbam;
		initCount = 0;
		this.historyManager = historyManager;
	}

	/**
	 * Get Class Name
	 */
	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	/**
	 * Initialize
	 */
	@Override
	protected void initialize() {
	}

	/**
	 * Stop Processing
	 */
	@Override
	protected void stopProcessing() {
	}

	/**
	 * Main Processing Method
	 */
	@Override
	protected void mainProcessing() {
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		long startTime = System.currentTimeMillis();
		log(String.format("VIPCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		if (initialized == false) {
			if (initCount >= 1 ) {
				initialized = true;
				clusterState.setVipCheckInitialized(true);
			}
		}
		
		boolean vipAssined = clusterState.isVipAssigned();
		String result = execScript(vipCheckScrip);
		log("Result : " + result);
		// 2016.07.04 by KBS : script 실행 결과가 없거나 NG일 경우 retry 최대 3회
		if (result != null && result.trim().length() != 0 && !result.equals(NG)) {
			// script 실행 성공
			retryCount = 0;	// reset to 0
		} else {
			// script 실행 실패
			retryCount++;
			log("execScript failed : retryCount[" + retryCount + "]");
			if (retryCount < 3) {
				return;
			} else {
				retryCount = 0;
			}
		}
		
		if (result != null) {
			result = result.trim();
		}
		int vipCount = 0;
		try {
			vipCount = Integer.parseInt(result);
		} catch (NumberFormatException nfe) {
			vipCount = 0;
		}
		long scriptCheckTime = System.currentTimeMillis() - startTime;
		log(String.format("VIP COUNT : [%d]. scriptCheckTime[%d]millis" , vipCount, scriptCheckTime));
		if (vipCount > 0) {
			if (vipAssined == false) {
				clusterState.setVipAssigned(true);
				//14.11.18 LSH
				//EVENT_TYPE.STATECHANGE -> EVENT_TYPE.VIP_UP
				historyManager.addClusterHistory(EVENT_TYPE.VIP_UP, ClusterConstant.CLUSTERMANAGER, isPrimary, "[VIP] UP");
				logCM("ClusterState Change. [VIP UP.]");
			}
		} else {
			if (vipAssined) {
				clusterState.setVipAssigned(false);
				//14.11.18 LSH
				//EVENT_TYPE.STATECHANGE -> EVENT_TYPE.VIP_DOWN
				historyManager.addClusterHistory(EVENT_TYPE.VIP_DOWN, ClusterConstant.CLUSTERMANAGER, isPrimary, "[VIP] DOWN");
				logCM("ClusterState Change. [VIP DOWN.]");
			}
		}
		vipAssined = clusterState.isVipAssigned();
		historyManager.addClusterInfo(MODE.UPDATE, isPrimary, ClusterConstant.VIP, vipAssined);
		initCount++;
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("VIPCheckThread processing Completed. [%s] : elapsedTime[%d]millis, initialized[%s]", vipAssined, elapsedTime, initialized));
	}
	
	/**
	 * 
	 * @param log
	 */
	protected void logCM(String log) {
		logger.debug(log);
	}
	
	/**
	 * 
	 * @param w
	 */
	protected void logCM(Throwable w) {
		logger.debug(w.getMessage(), w);
	}
	
	public static String infoName = "VIP";
}
