package com.samsung.ocs.failover.thread;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.constant.ClusterConstant.HEARTBEATTYPE;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.HeartBeat;

/**
 * HeartBeatCheckThread Class, OCS 3.0 for Unified FAB
 * 
 * HeartBeat상태를 주기적으로 체크하여 그 결과로 ClusterState 객체를 갱신하는 Thread
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

public class HeartBeatCheckThread extends AbstractOcsThread {
	private ClusterState state;
	private List<HeartBeat> heartBeatList;
	private int listenInterval = 2;
	private boolean[] checkArray;
	private int checkArraySize=1;
	private int checkArrayIndex=0;
	private int historyInsertCount = 1;
	
	private boolean lastRemotePublicNetState = true;
	private boolean lastRemoteLocalNetState = true;
	private boolean lastRemoteDBState = true;
	private boolean lastRemoteAvailable = true;
	private int delayInformMillis = 3000;
	
	private boolean isPrimary = true;
	private OcsHistoryManager historyManager = null;
	
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	
	private static final String TRUE = "TRUE";
	private String lastRemoteReportTime = "";
	
	/**
	 * Constructor of HeartBeatCheckThread class.
	 * 
	 * @param config
	 * @param state
	 * @param historyManager
	 * @exception SocketException
	 * @exception UnknownHostException
	 */
	public HeartBeatCheckThread(FailoverConfig config, ClusterState state, OcsHistoryManager historyManager) throws SocketException, UnknownHostException {
		super();
		this.state = state;
		// 16.08.04 by LWG
		this.heartBeatList = new ArrayList<HeartBeat>(config.getHeartBeatList());
		if(config.getTcpHeartBeat() != null) {
			heartBeatList.add(config.getTcpHeartBeat());
		}
		this.listenInterval = config.getHeartBeatListenInterval();
		this.historyInsertCount = config.getheartBeatHistoryInsertCount();
		this.delayInformMillis = config.getHeartBeatDelayInformMillis();
		this.checkArraySize = config.getHeartBeatTimeoutCount();
		this.checkArray = new boolean[this.checkArraySize];
		for (int i = 0; i < this.checkArray.length; i++) {
			this.checkArray[i] = true;
		}
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(CommonConfig.getInstance().getHostServiceType());
		this.historyManager = historyManager;
	}
	
	/**
	 * Initialize
	 */
	protected void initialize() {
		interval = FailoverConfig.getInstance().getHeartBeatInterval();
	}

	/**
	 * Stop Processing
	 */
	protected void stopProcessing() {
		
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	/**
	 * Main Processing Method
	 */
	protected void mainProcessing() {
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		long startTime = System.currentTimeMillis();
		log(String.format("HeartBeatCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		//heart beat
		long currentTimeMillis = System.currentTimeMillis();
		if (sleepCount % listenInterval == 0) {
			boolean resentlyListen = false;
			/* Heart Beat Check 로직 개선.
			for (HeartBeat hb : heartBeatList) {
				if (hb.isReported()) {
					resentlyListen = true;
					state.setResentHeartBeatTime(hb.getLastReportedTime());
					//true,true,true,operation:InService:$,jobassign:InService:$
					String message = hb.getLastReportedString();
					if (message != null && message.length() > 13) {
						String[] msg = message.split(",");
						
						boolean remoteInitialized = TRUE.equalsIgnoreCase(msg[3]);
						boolean remotePublicConnected = TRUE.equalsIgnoreCase(msg[0]);
						boolean remoteLocalConnected = TRUE.equalsIgnoreCase(msg[1]);
						boolean remoteDBConnected = TRUE.equalsIgnoreCase(msg[2]);
						
						if (remoteInitialized == true) {
							if(msg.length > 3) {
								if(msg[4] != null) {
									if(lastRemoteReportTime.compareTo(msg[4])<0) {
										state.setRemotePublicConnected(remotePublicConnected);
										state.setRemoteLocalConnected(remoteLocalConnected);
										state.setRemoteDBConnected(remoteDBConnected);
										lastRemoteReportTime = msg[4];
									}
								} else {
									state.setRemotePublicConnected(remotePublicConnected);
									state.setRemoteLocalConnected(remoteLocalConnected);
									state.setRemoteDBConnected(remoteDBConnected);
								}
							} else {
								state.setRemotePublicConnected(remotePublicConnected);
								state.setRemoteLocalConnected(remoteLocalConnected);
								state.setRemoteDBConnected(remoteDBConnected);
							}
						}
						state.setRemoteInitialized(remoteInitialized);
					}
					break;
				}
			}
			*/
			
			for (HeartBeat hb : heartBeatList) {
				if (hb.isReported()) {
					resentlyListen = true;
					state.setRecentHeartBeatTime(currentTimeMillis);
					if(hb.getType() == HEARTBEATTYPE.PUBLIC) {
						state.setRecentPublicHeartBeatTime(currentTimeMillis);
					} else if(hb.getType() == HEARTBEATTYPE.LOCAL) {
						state.setRecentLocalHeartBeatTime(currentTimeMillis);
					} else {
						state.setRecentDirectHeartBeatTime(currentTimeMillis);
					}
					//true,true,true,operation:InService:$,jobassign:InService:$
					String message = hb.getLastReportedString();
					if (message != null && message.length() > 13) {
						String[] msg = message.split(",");
						
						boolean remoteInitialized = TRUE.equalsIgnoreCase(msg[3]);
						boolean remotePublicConnected = TRUE.equalsIgnoreCase(msg[0]);
						boolean remoteLocalConnected = TRUE.equalsIgnoreCase(msg[1]);
						boolean remoteDBConnected = TRUE.equalsIgnoreCase(msg[2]);
						
						if (remoteInitialized == true) {
							if(msg.length > 3) {
								if(msg[4] != null) {
									if(lastRemoteReportTime.compareTo(msg[4])<0) {
										state.setRemotePublicConnected(remotePublicConnected);
										state.setRemoteLocalConnected(remoteLocalConnected);
										state.setRemoteDBConnected(remoteDBConnected);
										lastRemoteReportTime = msg[4];
									}
								} else {
									state.setRemotePublicConnected(remotePublicConnected);
									state.setRemoteLocalConnected(remoteLocalConnected);
									state.setRemoteDBConnected(remoteDBConnected);
								}
							} else {
								state.setRemotePublicConnected(remotePublicConnected);
								state.setRemoteLocalConnected(remoteLocalConnected);
								state.setRemoteDBConnected(remoteDBConnected);
							}
						}
						state.setRemoteInitialized(remoteInitialized);
					}
					hb.setReported(false);
				} else {
					if(hb.getLastReportedTime() > 0) {
						if(currentTimeMillis - hb.getLastReportedTime() > delayInformMillis) {
							if(isPrimary) {
								//14.11.18 LSH
								historyManager.addCheckDupClusterHistory(EVENT_TYPE.HEARTBEATDELAY, ClusterConstant.CLUSTERMANAGER, isPrimary, "(Secondary>Primary) HeartBeat Timeout ["+hb.getIpAddress()+":"+hb.getPort()+"]");
							} else {
								//14.11.18 LSH
								historyManager.addCheckDupClusterHistory(EVENT_TYPE.HEARTBEATDELAY, ClusterConstant.CLUSTERMANAGER, isPrimary, "(Primary>Secondary) HeartBeat Timeout ["+hb.getIpAddress()+":"+hb.getPort()+"]");
							}
						}
					}
				}
			}
			
			this.checkArray[checkArrayIndex] = resentlyListen;
			if (resentlyListen) {
				for (int i = 0; i < checkArraySize; i++) {
					checkArray[i] = true;
				}
			}

			int timeoutCount = 0;
			for (int i = 0; i < checkArraySize; i++) {
				if (checkArray[i] == false) {
					timeoutCount++;
				}
			}
			
			if (checkArray[checkArrayIndex] == false && timeoutCount < checkArraySize) {
				if(timeoutCount >= historyInsertCount) {
					String s = String.format("HeartBeat fail detected. [%d/%d] ", timeoutCount, checkArraySize);
					log(s);
					historyManager.addClusterHistory(EVENT_TYPE.DETECT, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
				}
			} else {
				if (state.isRemoteInitialized()) {
					/*14.11.18 LSH
					CM State Change Case 별 분류
					if (state.isRemoteDaemonAlive() != lastRemoteAvailable || 
							state.isRemoteDBConnected() != lastRemoteDBState ||
							state.isRemoteLocalConnected() != lastRemoteLocalNetState ||
							state.isRemotePublicConnected() != lastRemotePublicNetState ) {
						StringBuffer sb = new StringBuffer("RemoteCM[");
						sb.append(lastRemoteAvailable).append("->").append(state.isRemoteDaemonAlive()).append("] PublicNet[").append(lastRemotePublicNetState).append("->").append(state.isRemotePublicConnected());
						sb.append("] LocalNet[").append(lastRemoteLocalNetState).append("->").append(state.isRemoteLocalConnected());
						sb.append("] DB[").append(lastRemoteDBState).append("->").append(state.isRemoteDBConnected()).append("].");
						log(sb.toString());
						lastRemotePublicNetState = state.isRemotePublicConnected();
						lastRemoteLocalNetState = state.isRemoteLocalConnected();
						lastRemoteDBState = state.isRemoteDBConnected();
						historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, sb.toString());
					}
					
					Remote CM State는 Connect/Disconnect로 구분
					if (state.isRemoteDaemonAlive() != lastRemoteAvailable){
						if(isPrimary){
							String s = String.format("[HeartBeat] Secondary Cluster Manager State [%s->%s]", lastRemoteAvailable, state.isRemoteDaemonAlive());
							log(s);
							historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, s);							
						} else {
							String s = String.format("[HeartBeat] Primary Cluster Manager State [%s->%s]", lastRemoteAvailable, state.isRemoteDaemonAlive());
							log(s);
							historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
						}
					}
					*/
					if (state.isRemotePublicConnected() != lastRemotePublicNetState){
						if(isPrimary){
							String s = String.format("[HeartBeat] Secondary MCS Network State [%s->%s]", lastRemotePublicNetState, state.isRemotePublicConnected());
							log(s);
							historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, s);							
						} else {
							String s = String.format("[HeartBeat] Primary MCS Network State [%s->%s]", lastRemotePublicNetState, state.isRemotePublicConnected());
							log(s);
							historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
						}
						lastRemotePublicNetState = state.isRemotePublicConnected();
					}
					if (state.isRemoteLocalConnected() != lastRemoteLocalNetState){
						if(isPrimary){
							String s = String.format("[HeartBeat] Secondary OHT Network State [%s->%s]", lastRemoteLocalNetState, state.isRemoteLocalConnected());
							log(s);
							historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
						} else {
							String s = String.format("[HeartBeat] Primary OHT Network State [%s->%s]", lastRemoteLocalNetState, state.isRemoteLocalConnected());
							log(s);
							historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
						}
						lastRemoteLocalNetState = state.isRemoteLocalConnected();
					}
					if (state.isRemoteDBConnected() != lastRemoteDBState){
						if(isPrimary){
							String s = String.format("[HeartBeat] Secondary DB State [%s->%s]", lastRemoteDBState, state.isRemoteDBConnected());
							log(s);
							historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
						} else {
							String s = String.format("[HeartBeat] Primary DB State [%s->%s]", lastRemoteDBState, state.isRemoteDBConnected());
							log(s);
							historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, s);
						}						
						lastRemoteDBState = state.isRemoteDBConnected();
					}
				}
			}
			
			if (timeoutCount == checkArraySize) {
				state.setRemoteDaemonAlive(false);
				if (lastRemoteAvailable) {
					log("ClusterState Change. [Remote CM disconnected.]");					
					lastRemoteAvailable = false;
					if(isPrimary){
						historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, "[Secondary ClusterManager] DISCONNECTED.");
					} else {
						historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, "[Primary ClusterManager] DISCONNECTED.");
					}					
				}
			} else {
				state.setRemoteDaemonAlive(true);
				if (lastRemoteAvailable == false) {
					log("ClusterState Change. [Remote CM connected.]");
					lastRemoteAvailable = true;
					if(isPrimary){
						historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, "[Secondary ClusterManager] CONNECTED.");
					} else{
						historyManager.addClusterHistory(EVENT_TYPE.REMOTESTATECHANGE, ClusterConstant.CLUSTERMANAGER, isPrimary, "[Primary ClusterManager] CONNECTED.");
					}
				}
			}
			checkArrayIndex = (checkArrayIndex+1) % checkArraySize;
			sleepCount = 0;
		}
		
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("HeartBeatCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
	}
	
	public String getThreadId() {
		return this.getClass().getName();
	}
	
	protected void log(String log) {
		logger.debug(log);
	}
	
	protected void log(Throwable w) {
		logger.debug(w.getMessage(), w);
	}
}
