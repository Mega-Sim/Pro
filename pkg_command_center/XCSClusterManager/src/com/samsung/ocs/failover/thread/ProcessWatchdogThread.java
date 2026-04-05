package com.samsung.ocs.failover.thread;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.thread.AbstractUsingScriptOcsThread;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.dao.OcsDBStateDAO;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterInfoItem.MODE;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.LocalProcess;
import com.samsung.ocs.failover.model.ProcessInfo;

/**
 * ProcessWatchdogThread Class, OCS 3.0 for Unified FAB
 * 
 * 현재 관리중인 Local Process들이 실행되고 있는지를 Watchdog하여 프로세스가 내려가있을경우 local retry를 시켜주는 thread.
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

public class ProcessWatchdogThread extends AbstractUsingScriptOcsThread {
	private Map<String, LocalProcess> localProcessMap;
	private String uocsScript;
	private String psDataScript;
	private Map<String, ProcessInfo> pInfoMap;
	private DBAccessManager dbAccessManager;
	private boolean isPrimary = false;
	private ClusterState state = null;
	private boolean lastRemoteDaemonAlive = true;
	private OcsHistoryManager historyManager = null;
	
	private static int retryCount = 0;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
 	
	/**
	 * Constructor of ProcessWatchdogThread class.
	 * 
	 * @param fc
	 * @param dbAccessManager
	 * @param state
	 * @param historyManager
	 */
	public ProcessWatchdogThread(FailoverConfig fc, DBAccessManager dbAccessManager, ClusterState state, OcsHistoryManager historyManager ) {
		super(fc.getWatchdogInterval(), OcsConstant.PROCMANAGE);
		this.localProcessMap = fc.getLocalProcessMap();
		this.uocsScript = System.getProperty("UOCS").trim();
		this.psDataScript = System.getProperty("PSDATA").trim();
		this.pInfoMap = new HashMap<String, ProcessInfo>();
		this.dbAccessManager = dbAccessManager;
		CommonConfig cc = CommonConfig.getInstance();
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(cc.getHostServiceType());
		this.state = state;
		this.lastRemoteDaemonAlive = state.isRemoteDaemonAlive();
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
		log(String.format("ProcessWatchdogThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		// VIP관련한 내용이 한번 돌때까지, 기다려야제
		// Step 0.1 내 상태를 체크하고 준비가 될 경우 시작함.
		if (state.isVipCheckInitialized() == false) {
			try { sleep(500); } catch (Exception ignore) {}
			log("NOT START : VIP CEHCK NOT INITIALIZE.");
			long elapsedTime = System.currentTimeMillis() - startTime;
			log(String.format("ProcessWatchdogThread process completed. elapsedTime[%d]millis", elapsedTime));
			return;
		}
		
		pInfoMap.clear();
		
		String result = execScript(psDataScript);
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

		String[] pInfo = result.split("[|]");
		for (int i = 0; i < pInfo.length; i++) {
			if (pInfo[i] != null && pInfo[i].indexOf("PID:") > -1) {
				String pid = pInfo[i].substring(pInfo[i].indexOf(":") + 2).trim();
				i++;
				String moduleName = pInfo[i].substring(pInfo[i].indexOf("=") + 1).trim();
				pInfoMap.put(moduleName, new ProcessInfo(pid, moduleName));
			}
		}
		
		// 이게 돌고있음 CM은 살아있는거겠지, 그리고 remoteCM이랑 통신이 끊기면 챙겨주자.
		
		historyManager.addClusterInfo(MODE.UPDATE, isPrimary, ClusterConstant.CLUSTERMANAGER, true);
		if (lastRemoteDaemonAlive != state.isRemoteDaemonAlive()) {
			if (state.isRemoteDaemonAlive() == false) {
				historyManager.addClusterInfo(MODE.FORCEUNKNOWN, !isPrimary);
			}
			lastRemoteDaemonAlive = state.isRemoteDaemonAlive();
		}
		
		// 관리대상중에 켜진 프로세스가 없으면 켜기
		for (LocalProcess lp : localProcessMap.values()) {
			if (lp.isWatchdogRunControlUse()) {
				ProcessInfo processInfo = pInfoMap.get(lp.getProcessName());

				// process 관련해서 db에 정보를 남기자.
				historyManager.addClusterInfo(MODE.UPDATE, isPrimary, lp.getProcessName(), processInfo != null);
				
				switch (lp.getWatchdogPolicy()) {
					case ALWAYS_DOWN :
						if (processInfo != null) {
							String script = uocsScript + " down " + lp.getProcessName();
							String r = execScript(script);
							log("Result : " + r);
							historyManager.addClusterHistory(EVENT_TYPE.PROCESS_DOWN, lp.getProcessName(), isPrimary, "[PROCESS_DOWN] Policy : " + lp.getWatchdogPolicy());
						} 
						break;
					case ALWAYS_UP :
						if (processInfo == null) {
							String script = uocsScript + " up " + lp.getProcessName();
							String r = execScript(script);
							log("Result : " + r);
							OcsDBStateDAO.updateProcessUnknown(dbAccessManager, isPrimary, lp.getProcessName());
							log("Result : " + r);
							historyManager.addClusterHistory(EVENT_TYPE.PROCESS_STARTUP, lp.getProcessName(), isPrimary, "[PROCESS_STARTUP] Policy : " + lp.getWatchdogPolicy());
						} 
						break;
					case BALANCING :
						break;
					case VIP_STRICT :
						if (state.isVipAssigned()) {
							if (processInfo == null) {
								String script = uocsScript + " up " + lp.getProcessName();
								String r = execScript(script);
								log("Result : " + r);
								historyManager.addClusterHistory(EVENT_TYPE.PROCESS_STARTUP, lp.getProcessName(), isPrimary, "[PROCESS_STARTUP] Policy : " + lp.getWatchdogPolicy());
							} 
						} else {
							if (processInfo != null) {
								String script = uocsScript + " down " + lp.getProcessName();
								String r = execScript(script);
								log("Result : " + r);
								historyManager.addClusterHistory(EVENT_TYPE.PROCESS_DOWN, lp.getProcessName(), isPrimary, "[PROCESS_DOWN] Policy : " + lp.getWatchdogPolicy());
							} 
						}
						break;
					case VIP_BASE_DOWN :
						if (state.isVipAssigned() == false) {
							if (processInfo != null) {
								String script = uocsScript + " down " + lp.getProcessName();
								String r = execScript(script);
								log("Result : " + r);
								historyManager.addClusterHistory(EVENT_TYPE.PROCESS_DOWN, lp.getProcessName(), isPrimary, "[PROCESS_DOWN] Policy : " + lp.getWatchdogPolicy());
							} 
						}
						break;
					case VIP_BASE_UP :
						if (state.isVipAssigned()) {
							if (processInfo == null) {
								String script = uocsScript + " up " + lp.getProcessName();
								String r = execScript(script);
								log("Result : " + r);
								historyManager.addClusterHistory(EVENT_TYPE.PROCESS_STARTUP, lp.getProcessName(), isPrimary, "[PROCESS_STARTUP] Policy : " + lp.getWatchdogPolicy());
							} 
						}
						break;
					default :
						break;
				}
			}
		}
		
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("ProcessWatchdogThread process completed. elapsedTime[%d]millis", elapsedTime));
	}
}
