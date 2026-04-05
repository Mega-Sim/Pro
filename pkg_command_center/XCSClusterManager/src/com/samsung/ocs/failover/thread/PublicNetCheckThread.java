package com.samsung.ocs.failover.thread;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterInfoItem.MODE;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.PublicNetCheckConfig;

/**
 * PublicNetCheckThread Class, OCS 3.0 for Unified FAB
 * 
 * Public Network의 상태를 주기적으로 체크하여 그 결과로 ClusterState 객체를 갱신하는 Thread
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

public class PublicNetCheckThread extends AbstractPingCheckThread {
	private ClusterState state;
	private boolean[] checkArray;
	private int checkArrayCount = 1;
	private int checkArrayIndex = 0;
	
	private String gatewayIp;
	private String remoteIp;
	private int pingTimeout = 2000;
	private boolean lastPublicNetFail = true;
	private boolean isPrimary = true;
	private DBAccessManager dbam;
	private int initCount = 0;
	private OcsHistoryManager historyManager = null;
	private int heartBeatTimeout = 0;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of PublicNetCheckThread class.
	 * 
	 * @param config
	 * @param state
	 * @param dbam
	 * @param historyManager
	 */
	public PublicNetCheckThread(PublicNetCheckConfig config, ClusterState state, DBAccessManager dbam, OcsHistoryManager historyManager) {
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
		this.dbam=dbam;
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
		interval = FailoverConfig.getInstance().getPublicNetCheckConfig().getIntervalMillis();
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
		log(String.format("PublicNetCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		if (initialized == false) {
			if (initCount >= checkArrayCount) {
				initialized = true;
				state.setPublicNetCheckInitialized(true);
			}
		}
		
		//gateway ping : 연속 false면 내 퍼블릭이 안되는거지
//		checkArray[checkArrayIndex] = (pingCheck(remoteIp, pingTimeout) || pingCheck(gatewayIp, pingTimeout) || heartBeatCheck() );
		boolean pingResult = pingCheck(remoteIp, pingTimeout) || pingCheck(gatewayIp, pingTimeout);
		if(pingResult == false) {
			String s = String.format("MCS Network Ping(Reachable) Check Timeout. [%s,%s] [%d]ms", remoteIp, gatewayIp, pingTimeout *2 );
			log(s);
			// 14.12.03 LSH
			// Modify: addCheckDupClusterHistory -> addClusterHistory
			historyManager.addClusterHistory(EVENT_TYPE.DETECT, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
		}
		if(state.getRecentPublicHeartBeatTime() > 0) {
			checkArray[checkArrayIndex] = ( pingResult || heartBeatCheck() );
		} else {
			checkArray[checkArrayIndex] = pingResult;
		}
		
		int hostNetFailCnt=0;
		for (int i = 0; i < checkArrayCount; i++) {
			if (checkArray[i] == false) {
				hostNetFailCnt++;
			}
		}
		
		if (checkArray[checkArrayIndex] == false && hostNetFailCnt < checkArrayCount) {
			String s = String.format("MCS Network fail detected. [%d/%d] ", hostNetFailCnt, checkArrayCount );
			log(s);
			historyManager.addClusterHistory(EVENT_TYPE.DETECT, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
		}
		
		if (hostNetFailCnt == checkArrayCount) {
			// gateway랑 통신안된다!!!!!!
			state.setPublicNetCheckFail(true);
			if (lastPublicNetFail == false) {
				lastPublicNetFail = true;
				log("ClusterState Change. [PublicNetwork DOWN.]");
				//14.11.18 LSH
				//EVENT_TYPE.STATECHANGE -> EVENT_TYPE.PUBLICNET_DOWN
				historyManager.addClusterHistory(EVENT_TYPE.PUBLICNET_DOWN, ClusterConstant.CLUSTERMANAGER, isPrimary, "[MCS NETWORK] DOWN");
			}
		} else {
			state.setPublicNetCheckFail(false);
			if (lastPublicNetFail) {
				lastPublicNetFail = false;
				log("ClusterState Change. [PublicNetwork UP.]");
				//14.11.18 LSH
				//EVENT_TYPE.STATECHANGE -> EVENT_TYPE.PUBLICNET_UP
				historyManager.addClusterHistory(EVENT_TYPE.PUBLICNET_UP, ClusterConstant.CLUSTERMANAGER, isPrimary, "[MCS NETWORK] UP");
			}
		}
		// 속편하게 계속 DB에 쓰자 에잇
		historyManager.addClusterInfo(MODE.UPDATE, isPrimary, ClusterConstant.PUBLICNET, lastPublicNetFail == false);
		//remote ping : 연속 false면 상대방 퍼블릭이 안되는거지? 근데 내 퍼블릭이 안되는데 상대방께 되는게 말이되는가?
		checkArrayIndex = (checkArrayIndex+1) % checkArrayCount;
		
		initCount++;
		
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("PublicNetCheckThread process completed. publicNetFail[%s] ElapsedTime[%d] (initialized[%s])", lastPublicNetFail, elapsedTime, initialized));
	}
	
	private boolean heartBeatCheck() {
		if(state.getRecentPublicHeartBeatTime() > 0 
				&& (System.currentTimeMillis() - state.getRecentPublicHeartBeatTime()) > heartBeatTimeout ) {
			return false;
		}
		return true;
	}
}
