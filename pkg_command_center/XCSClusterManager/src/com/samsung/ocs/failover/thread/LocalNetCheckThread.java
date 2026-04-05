package com.samsung.ocs.failover.thread;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterInfoItem.MODE;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.LocalNetCheckConfig;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * LocalNetCheckThread Class, OCS 3.0 for Unified FAB
 * 
 * Local Network의 상태를 주기적으로 체크하여 그 결과로 ClusterState 객체를 갱신하는 Thread
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

public class LocalNetCheckThread extends AbstractPingCheckThread {
	private ClusterState state;
	private boolean[] checkArray;
	private int checkArrayCount = 1;
	private int checkArrayIndex = 0;
	
	private String gatewayIp;
	private String remoteIp;
	private int pingTimeout = 2000;
	private boolean lastLocalNetFail = true;
	private boolean isPrimary = false;
	private DBAccessManager dbam;
	private int initCount = 0;
	private OcsHistoryManager historyManager = null;
	private int heartBeatTimeout = 0;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of LocalNetCheckThread class.
	 * 
	 * @param config
	 * @param state
	 * @param dbam
	 * @param historyManager
	 */
	public LocalNetCheckThread(LocalNetCheckConfig config, ClusterState state, DBAccessManager dbam, OcsHistoryManager historyManager) {
		this.state = state;
		this.checkArrayCount = config.getTimeoutCount();
		this.checkArray = new boolean[checkArrayCount];
		for (int i = 0; i < checkArrayCount; i++) {
			checkArray[i] = true;
		}
		this.gatewayIp = config.getGatewayIp();
		this.remoteIp = config.getRemoteIp();
		this.pingTimeout = (int) config.getPingTimeoutMillis();
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(CommonConfig.getInstance().getHostServiceType());
		this.dbam = dbam;
		initCount = 0;
		this.historyManager = historyManager;
		this.heartBeatTimeout = pingTimeout * checkArrayCount;
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
		interval = FailoverConfig.getInstance().getLocalNetCheckConfig().getIntervalMillis();
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
		long startTime = System.currentTimeMillis();
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		log(String.format("LocalNetCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		if (initialized == false) {
			if (initCount >= checkArrayCount) {
				initialized = true;
				state.setLocalNetCheckInitialized(true);
			}
		}
		
		//gateway ping : 연속 false면 내 퍼블릭이 안되는거지
		
		boolean pingResult = pingCheck(remoteIp, pingTimeout) || pingCheck(gatewayIp, pingTimeout);
		if(pingResult == false) {
			String s = String.format("OHT Network Ping(Reachable) Check Timeout. [%s,%s] [%d]ms", remoteIp, gatewayIp, pingTimeout *2 );
			log(s);
			// 14.12.03 LSH
			// Modify: addCheckDupClusterHistory -> addClusterHistory
			historyManager.addClusterHistory(EVENT_TYPE.DETECT, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
		}
		if(state.getRecentLocalHeartBeatTime() > 0) {
			checkArray[checkArrayIndex] = ( pingResult || heartBeatCheck() );
		} else {
			checkArray[checkArrayIndex] = pingResult;
		}

		//remote ping : 연속 false면 상대방 퍼블릭이 안되는거지? 근데 내 퍼블릭이 안되는데 상대방께 되는게 말이되는가?
		int hostNetFailCnt=0;
		for (int i = 0; i < checkArrayCount; i++) {
			if (checkArray[i] == false) {
				hostNetFailCnt++;
			}
		}
		
		if (checkArray[checkArrayIndex] == false && hostNetFailCnt < checkArrayCount) {
			String s = String.format("OHT Network fail detected. [%d/%d] ", hostNetFailCnt, checkArrayCount );
			log(s);
			historyManager.addClusterHistory(EVENT_TYPE.DETECT, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
		}
		
		if (hostNetFailCnt == checkArrayCount) {
			// gateway랑 통신안된다!!!!!!
			state.setLocalNetCheckFail(true);
			if (lastLocalNetFail == false) {
				lastLocalNetFail = true;
				log("ClusterState Change. [LocalNetwork DOWN]");
				//14.11.18 LSH
				//EVENT_TYPE.STATECHANGE -> EVENT_TYPE.LOCALNET_DOWN
				historyManager.addClusterHistory(EVENT_TYPE.LOCALNET_DOWN, ClusterConstant.CLUSTERMANAGER, isPrimary, "[OHT NETWORK] DOWN");
			}
		} else {
			state.setLocalNetCheckFail(false);
			if (lastLocalNetFail) {
				lastLocalNetFail = false;
				log("ClusterState Change. [LocalNetwork UP.]");
				//14.11.18 LSH
				//EVENT_TYPE.STATECHANGE -> EVENT_TYPE.LOCALNET_UP
				historyManager.addClusterHistory(EVENT_TYPE.LOCALNET_UP, ClusterConstant.CLUSTERMANAGER, isPrimary, "[OHT NETWORK] UP");
			}
		}
		// 속편하게 계속 DB에 쓰자 에잇
		historyManager.addClusterInfo(MODE.UPDATE, isPrimary, ClusterConstant.LOCALNET, lastLocalNetFail == false);

		checkArrayIndex = (checkArrayIndex+1) % checkArrayCount;
		
		initCount++;
		
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("LocalNetCheckThread process completed. localNetFail[%s] ElapsedTime[%d] (initialized[%s])", lastLocalNetFail, elapsedTime, initialized));
	}
	
	private boolean heartBeatCheck() {
		if(state.getRecentLocalHeartBeatTime() > 0 
				&& (System.currentTimeMillis() - state.getRecentLocalHeartBeatTime()) > heartBeatTimeout ) {
			return false;
		}
		return true;
	}
}
