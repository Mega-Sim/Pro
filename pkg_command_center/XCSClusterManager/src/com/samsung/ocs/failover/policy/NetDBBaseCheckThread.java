package com.samsung.ocs.failover.policy;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.dao.OcsDBStateDAO;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.LocalProcess;
import com.samsung.ocs.failover.model.OcsDBStateVO;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * NetDBBaseCheckThread Class, OCS 3.0 for Unified FAB
 * 
 * Network상태와 DB상태에 따라 Inservice host를 판단하여 
 * Primary,Secondary HOST 중 한곳에서만 서비스 되는 프로세스를 위한 Policy Thread.
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

public class NetDBBaseCheckThread extends AbstractPolicyThread {
	private ClusterState clusterState;
	private DBAccessManager dbAccessManager;
	private String serviceType;
	private String processName;
	private LocalProcess process;
	private boolean isPrimary = false;
	private boolean initialized = false;
	
	private String currentInserviceHost = "";
	
	private int remoteInitTimeOutCount = 0;
	private int remoteInitWaitCnt = 0;
	
	private int autoTakeOverCount = 0;
	private int autoTakeOverTimeoutCount = 0;
	
	private int inserviceRequestTimeoutCount = 0;
	private int inserviceRequestCount = 0; 
	private OcsHistoryManager historyManager = null;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of NetDBBaseCheckThread class.
	 */
	public NetDBBaseCheckThread(DBAccessManager dbAccessManager, ClusterState clusterState, LocalProcess process, UDPSender sender, OcsHistoryManager historyManager) {
		super(500, process, sender);
		this.dbAccessManager = dbAccessManager;
		this.clusterState = clusterState;
		CommonConfig cc = CommonConfig.getInstance();
		this.processName = process.getProcessName();
		this.process = process;
		this.serviceType = cc.getHostServiceType();
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(serviceType);
		
		FailoverConfig fc = FailoverConfig.getInstance();
		this.remoteInitTimeOutCount = fc.getRemoteInitTimeOutCount();
		this.autoTakeOverTimeoutCount = fc.getAutoTakeOverCount();
		this.inserviceRequestTimeoutCount = fc.getInserviceRequestTimeoutCount();
		
		this.historyManager = historyManager;
	}
	
	/**
	 * 
	 */
	protected void initialize() {
	}

	/**
	 * 
	 */
	protected void stopProcessing() {
	}
	
	// 초기화 담당입니다. ㅎㅎㅎ
	/**
	 * Initialize Module
	 */
	private boolean initModule() {
		OcsDBStateVO vo = OcsDBStateDAO.retrieveProcessState(processName, dbAccessManager);
		if (vo != null) {
			currentInserviceHost = vo.getInserviceHost();
			
			//초기화?
			if (OcsConstant.VIAVIP.equalsIgnoreCase(vo.getFailoverAcceptance())) {
				OcsDBStateDAO.updateFailoverAcceptance(dbAccessManager, OcsConstant.FALSE, processName);
			}
		} else {
			return false;
		}
		return true;
	}

	/**
	 * 
	 */
	protected void mainProcessing() {
		
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		long startTime = System.currentTimeMillis();
		log(String.format("NetDBBaseCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		// Step 0. failOver를 사용하는지 체크를 해보고 안하면 sleep만 하도록 함. 
		if (process.isFailoverUse() == false) {
			try { sleep(1000); } catch (Exception ignore) {}
			long elapsedTime = System.currentTimeMillis() - startTime;
			log(String.format("NetDBBaseCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
			return;
		}
		
		// Step 0.1 내 상태를 체크하고 준비가 될 경우 시작함.
		if (clusterState.isCheckThreadInitialized() == false) {
			try { sleep(1000); } catch (Exception ignore) {}
			log("NOT START : LOCAL CEHCK NOT INITIALIZE.");
			long elapsedTime = System.currentTimeMillis() - startTime;
			log(String.format("NetDBBaseCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
			return;
		}
		
		// Step 1. 상대 Cluster와의 통신이 되는지 체크한다. ( 최초에만 진입을 막도록 하며, remoteInitWaitCnt만큼만 기다려 준다 ) 
		if (initialized == false) {
			if (clusterState.isRemoteDaemonAlive() == false || clusterState.isRemoteInitialized() == false) {
				if (remoteInitWaitCnt < remoteInitTimeOutCount) { 
					remoteInitWaitCnt++;
					log("NOT START : REMOTE CM IS NOT ALIVE.");
					long elapsedTime = System.currentTimeMillis() - startTime;
					log(String.format("NetDBBaseCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
					return;
				}
			}
			initialized = initModule();
		}
		
		// Step 2. DB로부터 OcsDBStateVO를 가져온다.		
		OcsDBStateVO vo = OcsDBStateDAO.retrieveProcessState(processName, dbAccessManager);
		
		if (vo != null) {
			// Step 3. DB로부터 OcsDBStateVO를 가져왔을 경우 	
			// Step 3-1. 현재의 Inservice-Host와 host/remote의 상태를 체크한다. 
			currentInserviceHost = vo.getInserviceHost();
			boolean isHostAvailable = checkHostState(); 
			boolean isRemoteAvailable = checkRemoteState();
			
			StringBuffer stateLog = new StringBuffer("  - DB ROW currentInserviceHost [");
			stateLog.append(currentInserviceHost).append("] isHostAvailable [").append(isHostAvailable).append("] isRemoteAvailable [").append(isRemoteAvailable);
			stateLog.append("] inserviceRequestCount [").append(inserviceRequestCount).append("] autoTakeOverCount [").append(autoTakeOverCount);
			stateLog.append("] failover_accptance [").append(vo.getFailoverAcceptance()).append("]");
			log(stateLog.toString());
			if (isHostAvailable == false) {
				boolean publicNet = process.isPublicNetCheckUse() ? clusterState.isPublicNetCheckFail() == false : true;
				boolean localNet = process.isLocalNetCheckUse() ? clusterState.isLocalNetCheckFail() == false : true;
				boolean dbFail = clusterState.isDbConnCheckFail() == false;
				log(String.format("  - HOST NOT AVAILABLE : publicNet [%s] localNet [%s] dbFail [%s]", publicNet, localNet, dbFail ));
			}
			if (isRemoteAvailable == false) {
				boolean remoteAvaliable = clusterState.isRemoteDaemonAlive();
				boolean publicNet = process.isPublicNetCheckUse() ? clusterState.isRemotePublicConnected() : true;
				boolean localNet = process.isLocalNetCheckUse() ? clusterState.isRemoteLocalConnected() : true;
				boolean dbFail = clusterState.isRemoteDBConnected();
				log(String.format("  - REMOTE NOT AVAILABLE : remoteAvaliable [%s] publicNet [%s] localNet [%s] dbFail [%s]", remoteAvaliable, publicNet, localNet, dbFail ));
			}
			
			// Step 3-1-0. UserRequest를 처리해보자..  요청사항이 있으면 Count 변수를 초기화 하고 처리한다.
			String userRequest = vo.getUserRequest();
			if (userRequest != null && userRequest.length() > 0) {
				// Step 3-1-0-1. Inservicehost가 현재 자기자신이 아닌 경우만! : Primary Secondary 둘다 할경우 피곤해지니까 일단 Secondary에서 처리하는것으로 한다.
				if (currentInserviceHost.equalsIgnoreCase(serviceType) == false) {
					// Step 3-1-0-2. Count 변수 초기화.
					initCountVariable();
					log(String.format("  - USER REQUEST [%s]", userRequest));
					//14.11.18 LSH
					//EVENT_TYPE.DETECT -> EVENT_TYPE.USERTAKEOVERREQ
					historyManager.addClusterHistory(EVENT_TYPE.USERTAKEOVERREQ, processName, isPrimary, String.format("User-Takeover Request Detected. [RequestedHost: %s]", userRequest));
					// Step-3-1-0-3. 현재 서비스 호스트랑 UserRequest랑 같은경우는 무시하고 지워줌.
					if (currentInserviceHost.equalsIgnoreCase(userRequest) == true) {
						log(String.format("  - USER REQUEST IGNORE : CurrentInserviceHost[%s] and UserRequest[%s] is same.", currentInserviceHost, userRequest ));
						OcsDBStateDAO.updateUserRequest(dbAccessManager, "", processName);
						historyManager.addClusterHistory(EVENT_TYPE.USERTAKEOVERIGNORE, processName, isPrimary, String.format("[User Takeover Fail] CurrentInserviceHost and UserRequest is same. [%s->%s]", currentInserviceHost, userRequest));
					} else {
					// Step-3-1-0-4. 현재 서비스 호스트랑 UserRequest랑 다른경우 take over해줌. failover acceptance는 무조건 false로 바꿔준다.. 이건 나중에 바꿀수도.
						// Step-3-1-0-5. 단 그 호스트명이 나랑 같아야 하며, 나의 host 상태도 available 해야한다.
						if (serviceType.equalsIgnoreCase(userRequest) == true) {
							if (isHostAvailable) {
								boolean updateInserviceHost = OcsDBStateDAO.updateInserviceHost(dbAccessManager, isPrimary, OcsConstant.FALSE, processName);
								/*if(updateInserviceHost) {
									remoteUocsDown(processName);
								}*/
								log(String.format("  - USER REQUEST dbUpdate InserviceHost [%s]", updateInserviceHost));
								OcsDBStateDAO.updateUserRequest(dbAccessManager, "", processName);
								historyManager.addClusterHistory(EVENT_TYPE.USERTAKEOVER, processName, isPrimary, String.format("[User Takeover] Result: [%s]", updateInserviceHost));
							} else {
								log("  - USER REQUEST FAIL : Host is not available.");
								OcsDBStateDAO.updateUserRequest(dbAccessManager, "", processName);
								//14.11.18 LSH
								//Primary/Secondary Message 구분
								//historyManager.addClusterHistory(EVENT_TYPE.USERTAKEOVERFAIL, processName, isPrimary, "User-takeover request fail. Host is not available.");
								if(isPrimary) {
									historyManager.addClusterHistory(EVENT_TYPE.USERTAKEOVERFAIL, processName, isPrimary, "[User Takeover Fail] Primary is not OK.");
								} else {
									historyManager.addClusterHistory(EVENT_TYPE.USERTAKEOVERFAIL, processName, isPrimary, "[User Takeover Fail] Secondary is not OK.");
								}
							}
						} else {
							log(String.format("  - USER REQUEST IGNORE : User-request[%s] is not correct.", userRequest));
							OcsDBStateDAO.updateUserRequest(dbAccessManager, "", processName);
							historyManager.addClusterHistory(EVENT_TYPE.USERTAKEOVERIGNORE, processName, isPrimary, String.format("[User Takeover Fail] User Request is not correct. [%s]", userRequest));
						}
					}
					long elapsedTime = System.currentTimeMillis() - startTime;
					log(String.format("NetDBBaseCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
					return;
				}
			}
			
			// Step 3-2. failOverAcceptance가 false일 경우 Count 변수를 초기화 하고 아무것도 하지 않는다.
			if (OcsConstant.TRUE.equalsIgnoreCase(vo.getFailoverAcceptance()) == false) {
				initCountVariable();
				log("  - failover Acceptance is false..");
				try { sleep(500); } catch (Exception ignore) {}
				long elapsedTime = System.currentTimeMillis() - startTime;
				log(String.format("NetDBBaseCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
				return;
			}
			
			if (currentInserviceHost.equalsIgnoreCase(serviceType) == false) {
				// Stop 3-3. Inservicehost가 현재 자기자신이 아닌 경우만!
				if (isRemoteAvailable == false && isHostAvailable == true) {
					// Stop 3-3-1. Remote의 상태가 서비스를 하기에 적합하지 않고, host의 상태가 정상일 경우 autoTakeOverCount가 Timeout만큼 기다려 주다가 뺏어온다.
					inserviceRequestCount = 0;
					log("  - CHECK NET isRemoteAvailable [" + isRemoteAvailable + "] isHostAvailable [" + isHostAvailable + "] autoTakeOverCount ["+autoTakeOverCount + "]");
					autoTakeOverCount++;
					
					if (autoTakeOverCount <= autoTakeOverTimeoutCount) {
						//14.11.18 LSH
						//Primary/Secondary Message 구분
						//String s = String.format("[Remote not available] detected. [%d/%d] ", autoTakeOverCount, autoTakeOverTimeoutCount );
						//historyManager.addClusterHistory(EVENT_TYPE.DETECT, processName, isPrimary, s);
						if(isPrimary){
							String s = String.format("[Secondary State Not Available] Detected. [%d/%d]", autoTakeOverCount, autoTakeOverTimeoutCount );
							historyManager.addClusterHistory(EVENT_TYPE.DETECT, processName, isPrimary, s);
						} else {
							String s = String.format("[Primary State Not Available] Detected. [%d/%d]", autoTakeOverCount, autoTakeOverTimeoutCount );
							historyManager.addClusterHistory(EVENT_TYPE.DETECT, processName, isPrimary, s);
						}
					}
					
					if (autoTakeOverCount > autoTakeOverTimeoutCount) {
						String hostState = "";
						if (isPrimary) {
							hostState = vo.getPrimaryState();
						} else {
							hostState = vo.getSecondaryState();
						}
						if (OcsConstant.UNKNOWN.equalsIgnoreCase(hostState) == false) {
							boolean updateInserviceHost = OcsDBStateDAO.updateInserviceHost(dbAccessManager, isPrimary, OcsConstant.FALSE, processName);
							if (updateInserviceHost) {
								remoteUocsDown(processName);
							}
							//14.11.18 LSH
							//Primary/Secondary Message 구분
							//historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVER, processName, isPrimary, String.format("Auto-takeover(Remote is not available.) result : [%s]", updateInserviceHost));
							if(isPrimary){
								historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVER, processName, isPrimary, String.format("[System Failover] Result: [%s] (by Secondary is not available)", updateInserviceHost));								
							} else {
								historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVER, processName, isPrimary, String.format("[System Failover] Result: [%s] (by Primary is not available)", updateInserviceHost));								
							}
							log("  - dbUpdate InserviceHost ["+updateInserviceHost+"]");
						} else {
							log("  - HOST STATE IS UNKNOWN!!");
							//14.11.18 LSH
							//Primary/Secondary Message 구분
							//historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVERFAIL, processName, isPrimary, "Auto-takeover(Remote is not available.) request fail. Host-process's state is 'UNKNOWN'.");
							if(isPrimary){
								historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVERFAIL, processName, isPrimary, "[System Failover Fail] Primary-process state is 'UNKNOWN' (by Secondary is not available)");
							} else {
								historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVERFAIL, processName, isPrimary, "[System Failover Fail] Secondary-process state is 'UNKNOWN' (by Primary is not available)");
							}
						}
						autoTakeOverCount = 0;
					}
				} else if (isHostAvailable == true) {
					// Stop 3-3-2. 그외 host가 정상일 때, Remote가 Inservice중이여야 하지만, OutOfService혹은 Unknown으로 유지되면 inserviceRequestTimeoutCount만큼 기다려 주다가 뺏어온다. 
					autoTakeOverCount = 0;
					
					String remoteState = "";
					String hostState = "";
					if (isPrimary) {
						remoteState = vo.getSecondaryState();
						hostState = vo.getPrimaryState();
					} else {
						remoteState = vo.getPrimaryState();
						hostState = vo.getSecondaryState();
					}
					if (OcsConstant.OUTOFSERVICE.equalsIgnoreCase(remoteState) || OcsConstant.UNKNOWN.equalsIgnoreCase(remoteState)) {
						inserviceRequestCount++;
						
						if (inserviceRequestCount <= inserviceRequestTimeoutCount) {
							//14.11.18 LSH
							//Primary/Secondary Message 구분
							//String s = String.format("[Remote-process is 'Unknown' or 'OutOfService'] detected. [%d/%d] ", inserviceRequestCount, inserviceRequestTimeoutCount );
							//historyManager.addClusterHistory(EVENT_TYPE.DETECT, processName, isPrimary, s);
							if(isPrimary){
								String s = String.format("[Secondary Process is 'UNKNOWN' or 'OutOfService'] Detected. [%d/%d]", inserviceRequestCount, inserviceRequestTimeoutCount );
								historyManager.addClusterHistory(EVENT_TYPE.DETECT, processName, isPrimary, s);
							} else {
								String s = String.format("[Primary Process is 'UNKNOWN' or 'OutOfService'] Detected. [%d/%d]", inserviceRequestCount, inserviceRequestTimeoutCount );
								historyManager.addClusterHistory(EVENT_TYPE.DETECT, processName, isPrimary, s);
							}
						}
						
						if (inserviceRequestCount > inserviceRequestTimeoutCount) {
							if (OcsConstant.UNKNOWN.equalsIgnoreCase(hostState) == false) {
								log("  - REMOTE INSERVICE TIMEOUT : " + inserviceRequestCount); 
								boolean updateInserviceHost = OcsDBStateDAO.updateInserviceHost(dbAccessManager, isPrimary, OcsConstant.FALSE, processName);
								if (updateInserviceHost) {
									remoteUocsDown(processName);
								}
								//14.11.18 LSH
								//Primary/Secondary Message 구분
								//historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVER, processName, isPrimary, String.format("Auto-takeover(Remote-state timeout.) result : [%s]", updateInserviceHost));
								if(isPrimary){
									historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVER, processName, isPrimary, String.format("[System Failover] Result: [%s] (by Secondary INSERVICE Request Timeout)", updateInserviceHost));
								} else {
									historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVER, processName, isPrimary, String.format("[System Failover] Result: [%s] (by Primary INSERVICE Request Timeout)", updateInserviceHost));
								}
								log("  - dbUpdate InserviceHost ["+updateInserviceHost+"]");
							} else {
								//14.11.18 LSH
								//Primary/Secondary Message 구분
								//historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVERFAIL, processName, isPrimary, "Auto-takeover(Remote-state timeout.) request fail. Host-process's state is 'UNKNOWN'.");
								if(isPrimary){
									historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVERFAIL, processName, isPrimary, "[System Failover Fail] Primary-process state is 'UNKNOWN' (by Secondary INSERVICE Request Timeout)");
								} else {
									historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVERFAIL, processName, isPrimary, "[System Failover Fail] Secondary-process state is 'UNKNOWN' (by Primary INSERVICE Request Timeout)");
								}
								log("  - HOST STATE IS UNKNOWN!!");
							}
							inserviceRequestCount = 0;
						}
					} else {
						inserviceRequestCount = 0;
					}
				} else {
					initCountVariable();
				}
			} else {
				// Stop 3-4. Inservicehost가 현재 자기자신인 경우. do nothing  
				initCountVariable();
			}
		} else {
			// Step 4. DB로부터 OcsDBStateVO를 못가져왔을 경우는 아무것도 하지 않는다. ( Count 변수만 초기화 함. ) 
			log("  - retrieveProcessState IS NULL");
			initCountVariable();
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("NetDBBaseCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
	}
	
	/**
	 * 
	 */
	private void initCountVariable() {
		autoTakeOverCount = 0;
		inserviceRequestCount = 0;
	}

	/**
	 * 
	 */
	public String getThreadId() {
		return "NetDBBaseCheckThread [" + process.getProcessName() + "]";
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean checkHostState() {
		boolean publicNet = process.isPublicNetCheckUse() ? clusterState.isPublicNetCheckFail() == false : true;
		boolean localNet = process.isLocalNetCheckUse() ? clusterState.isLocalNetCheckFail() == false : true;
//		boolean dbFail = clusterState.isDbConnCheckFail() == false;
		return (publicNet && localNet);
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean checkRemoteState() {
		if (clusterState.isRemoteDaemonAlive() == true) {
			boolean publicNet = process.isPublicNetCheckUse() ? clusterState.isRemotePublicConnected() : true;
			boolean localNet = process.isLocalNetCheckUse() ? clusterState.isRemoteLocalConnected() : true;
//			boolean dbFail = clusterState.isRemoteDBConnected();
			return (publicNet && localNet);
		} else {
			return false;
		}
	}
}
