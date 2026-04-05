package com.samsung.ocs.failover.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.samsung.ocs.failover.model.NetworkPingCheckConfig;

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

public class NetworkPingCheckThread extends AbstractPingCheckThread {
	private ClusterState state;
	
	private int pingTimeout = 2000;
	private boolean isPrimary = false;
	private DBAccessManager dbam;
	private OcsHistoryManager historyManager = null;
	private Map<String, String> ipListMap;
	
	private List<String> oldAliveList = new ArrayList<String>();
	private List<String> oldDeadList = new ArrayList<String>();
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of LocalNetCheckThread class.
	 * 
	 * @param config
	 * @param state
	 * @param dbam
	 * @param historyManager
	 */
	public NetworkPingCheckThread(NetworkPingCheckConfig config, ClusterState state, DBAccessManager dbam, OcsHistoryManager historyManager) {
		this.state = state;
		this.pingTimeout = (int) config.getPingTimeoutMillis();
		this.ipListMap = config.getIpListMap();
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(CommonConfig.getInstance().getHostServiceType());
		this.dbam = dbam;
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
		interval = FailoverConfig.getInstance().getNetworkPingCheckConfig().getIntervalMillis();
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
		log(String.format("NetworkPingCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		List<String> aliveList = new ArrayList<String>();
		List<String> deadList = new ArrayList<String>();
		
		Set<String> keySet = ipListMap.keySet();
		for(String name : keySet) {
			String ipAddress = ipListMap.get(name);
			boolean pingCheck = pingCheck(ipAddress, pingTimeout);
			if (pingCheck) {
				aliveList.add(name);
			} else {
				deadList.add(name);
			}
		}
		String result = String.format("ALIVE%s:DEAD%s", aliveList.toString(), deadList.toString());
		
		// 속편하게 계속 DB에 쓰자 에잇
		historyManager.addClusterInfo(MODE.UPDATE, isPrimary, ClusterConstant.PINGCHECK, result);
		
		if ( compareResultList(aliveList, oldAliveList, deadList, oldDeadList) == false ) {
			// 14.11.18 LSH
			// Modify: addClusterHistory -> addCheckDupClusterHistory
			historyManager.addCheckDupClusterHistory(EVENT_TYPE.STATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, String.format("%s%s", "[HUBCHK] ",result.replaceAll(":", " ")) );
			oldAliveList.clear();
			oldDeadList.clear();
			oldAliveList.addAll(aliveList);
			oldDeadList.addAll(deadList);
		}
		
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("NetworkPingCheckThread process completed. result[%s] ElapsedTime[%d]", result, elapsedTime));
	}

	/**
	 * @param aliveList
	 * @param oldAliveList2
	 * @param deadList
	 * @param oldDeadList2
	 * @return
	 */
	private boolean compareResultList(List<String> aliveList, List<String> oldAliveList2, List<String> deadList, List<String> oldDeadList2) {
		boolean result = false;
		if( equalListContent(aliveList, oldAliveList2) ) {
			if( equalListContent(deadList, oldDeadList2) ) {
				result = true;
			}
		}
		return result;
	}
	
	private boolean equalListContent(List<String> list1, List<String> list2) {
		boolean result = true;
		if(list1.size() == list2.size()) {
			for (String s : list1) {
				int index = list2.indexOf(s);
				if(index < 0 ) {
					result = false;
					break;
				}
			}
		} else {
			result = false;
		}
		return result;
	}
}
