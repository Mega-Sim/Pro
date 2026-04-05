package com.samsung.ocs.failover;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.INIT_STATE;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.common.udpu.UDPServer;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.dao.OcsDBStateDAO;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.dao.OcsProcessVersionDAO;
import com.samsung.ocs.failover.job.HeartBeatListenJob;
import com.samsung.ocs.failover.job.RemoteCMCommandExecuteJob;
import com.samsung.ocs.failover.model.ClusterInfoItem.MODE;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.DatabaseConnCheckConfig;
import com.samsung.ocs.failover.model.HeartBeat;
import com.samsung.ocs.failover.model.LocalNetCheckConfig;
import com.samsung.ocs.failover.model.LocalProcess;
import com.samsung.ocs.failover.model.NetworkPingCheckConfig;
import com.samsung.ocs.failover.model.OcsDBStateVO;
import com.samsung.ocs.failover.model.OcsProcessVersionVO;
import com.samsung.ocs.failover.model.PublicNetCheckConfig;
import com.samsung.ocs.failover.policy.AbstractPolicyThread;
import com.samsung.ocs.failover.policy.AnyHostBaseCheckThread;
import com.samsung.ocs.failover.policy.NetDBBaseCheckThread;
import com.samsung.ocs.failover.policy.VIPBaseCheckThread;
import com.samsung.ocs.failover.thread.DatabaseConnCheckThread;
import com.samsung.ocs.failover.thread.FileSizeTraceThread;
import com.samsung.ocs.failover.thread.HeartBeatCheckThread;
import com.samsung.ocs.failover.thread.LocalNetCheckThread;
import com.samsung.ocs.failover.thread.LogAndHistoryManageThread;
import com.samsung.ocs.failover.thread.NetworkPingCheckThread;
import com.samsung.ocs.failover.thread.ProcessWatchdogThread;
import com.samsung.ocs.failover.thread.PublicNetCheckThread;
import com.samsung.ocs.failover.thread.TCPClientHeartBeatThread;
import com.samsung.ocs.failover.thread.TCPServerHeartBeatThread;
import com.samsung.ocs.failover.thread.UDPHeartBeatSendThread;
import com.samsung.ocs.failover.thread.VipCheckThread;

/**
 * XCSClusterManager Class, OCS 3.0 for Unified FAB
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

public class XCSClusterManager {
	private FailoverConfig fc = null;
	private HashMap<String, AbstractOcsThread> mainThreadMap;
	private ClusterState state;
	private DBAccessManager dbAccessManager = null;
	private OcsHistoryManager historyManager = null;
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	
	// 2011.11.14 by PMM
	private static final String VERSION = "VERSION";
	private static final String BUILDID = "BUILDID";
	// 2019.09.02 by JJW Process name 정의
	private static final String xcsclustermanager = "xcsclustermanager";
	/**
	 * Constructor of XCSClusterManager class.
	 */
	public XCSClusterManager() {
		// Step 0. thread map 및 ClusterState Instance를 생성함. 
		logger.debug("FailOver Launcher Init Start");
		System.out.println("FAILOVER LAUNCHER START");
		mainThreadMap = new HashMap<String, AbstractOcsThread>();
		state = new ClusterState();
		historyManager = new OcsHistoryManager();

		// Step 1. 설정파일을 읽어옴 ( 안되면 될때까지 계속 시도함 ) 
		logger.debug("Loading Config start....");
		state.setInitState(INIT_STATE.LOAD_CONFIG);
		initializeConfiguration();
		logger.debug("Loading Config end....");
		
		// stdout/stderr 설정을 확인한다.
		String writeOut = System.getProperty(ClusterConstant.WRITEOUT);
		if(writeOut != null) {
			String wo = writeOut.trim();
			if("TRUE".equalsIgnoreCase(wo)) {
				FailoverConfig.getInstance().writeStdOut();
			}
		}
		String writeErr = System.getProperty(ClusterConstant.WRITEERR);
		if(writeErr != null) {
			String we = writeErr.trim();
			if("TRUE".equalsIgnoreCase(we)) {
				FailoverConfig.getInstance().writeStdErr();
			}
		}
		
		// Step 2. Heart Beat Listen Server와 처리Thread를 시작한다. 
		state.setInitState(INIT_STATE.THREAD_INIT);
		logger.debug("Heart Beat Process Init start....");
		initializeHeartBeatProcess();
		logger.debug("Heart Beat Process Init end....");
		
		// Step 3. db 접속을 한다. ( 안되면 될때까지 계속 시도함 ) 
		logger.debug("DB Connection start....");
		state.setInitState(INIT_STATE.DB_CONNECTION_INIT);
		initializeDBState();
		logger.debug("DB Connection end....");
		
		// Step 4. 필요한 Thread를 구성하고 start 해준다.
		state.setInitState(INIT_STATE.THREAD_INIT);
		logger.debug("Thread Init start....");
		initializeThreads();
		runThreads();
		logger.debug("Thread Init end....");
		
		logger.debug("FailOver Launcher Init End");
		System.out.println("FAILOVER LAUNCHER END");
		
		// Step 5. STARTUPHISTORY파일에 시작된 시간을 기록해준다.
		Logger startupLogger = Logger.getLogger(CommonLogFileName.STARTUPHISTORY);
		startupLogger.info("ClusterManager start up.");
		
		startupLogger.info(" Version:" + getVersion());
		startupLogger.info("   Include:" + getIncludeInfo());
		startupLogger.info(" BuildId:" + getBuildId());
	}

	/**
	 * 필요한 Thread들 map에서 시작하기
	 */
	private void runThreads() {
		for (AbstractOcsThread thread : mainThreadMap.values()) {
			thread.start();
		}
	}
	
	private void initializeHeartBeatProcess() {
		// 상대방 heart beat 리모트 받기 & 보내기
		List<HeartBeat> heartBeatList = fc.getHeartBeatList();
		for (HeartBeat hb : heartBeatList) {
			UDPServer server = new UDPServer(new HeartBeatListenJob(hb), hb.getPort());
			// server.start()
			if (!server.start()){
				logger.debug("System Exit. by UDPHeartBeatServer Initialize Fail");
				System.exit(0);
			}
		}

		try {
			HeartBeatCheckThread hrt = new HeartBeatCheckThread(fc, state, historyManager);
			hrt.start();
		} catch (Exception ignore) {}
		
		// 2016.08.04 by LWG
		try {
			UDPHeartBeatSendThread uhbst = new UDPHeartBeatSendThread(fc, state);
			uhbst.start();
		} catch (Exception ignore) {
		}
		
		HeartBeat tcpHeartBeat = fc.getTcpHeartBeat();
		if(tcpHeartBeat != null) {
			boolean isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(CommonConfig.getInstance().getHostServiceType());
			if(isPrimary) {
				TCPClientHeartBeatThread tcp = new TCPClientHeartBeatThread(tcpHeartBeat, state, fc.getTcpClientTimeoutMillis());
				tcp.start();
			} else {
				TCPServerHeartBeatThread tcp = new TCPServerHeartBeatThread(this, tcpHeartBeat, state);
				tcp.start();
			}
		}
	}
	
	
	/**
	 * 필요한 Thread들 map에 등록하기
	 */
	private void initializeThreads() {
		
		// db connection test
		DatabaseConnCheckConfig databaseConnCheckConfig = fc.getDatabaseConnCheckConfig();
		DatabaseConnCheckThread dct = new DatabaseConnCheckThread(databaseConnCheckConfig, dbAccessManager, state);
		mainThreadMap.put(dct.getThreadId(), dct);
		
		VipCheckThread vct = new VipCheckThread(state, dbAccessManager, historyManager);
		mainThreadMap.put(vct.getThreadId(), vct);
		
		// watch dog 
		ProcessWatchdogThread pwt = new ProcessWatchdogThread(fc, dbAccessManager, state, historyManager);
		mainThreadMap.put(pwt.getThreadId(), pwt);
		
		// 퍼블릭 넷 체크
		PublicNetCheckConfig publicNetCheckConfig = fc.getPublicNetCheckConfig();
		if (publicNetCheckConfig.isUse()) {
			PublicNetCheckThread pnct = new PublicNetCheckThread(publicNetCheckConfig, state, dbAccessManager, historyManager);
			mainThreadMap.put(pnct.getThreadId(), pnct);
		} else {
			state.setPublicNetCheckFail(false);
			state.setPublicNetCheckInitialized(true);
		}
		
		// 로컬 넷 체크
		LocalNetCheckConfig localNetCheckConfig = fc.getLocalNetCheckConfig();
		if (localNetCheckConfig.isUse()) {
			LocalNetCheckThread lnct = new LocalNetCheckThread(localNetCheckConfig, state, dbAccessManager, historyManager);
			mainThreadMap.put(lnct.getThreadId(), lnct);
		} else {
			state.setLocalNetCheckFail(false);
			state.setLocalNetCheckInitialized(true);
		}
		
		// 넷 핑 체크
		NetworkPingCheckConfig networkPingCheckConfig = fc.getNetworkPingCheckConfig();
		if (networkPingCheckConfig.isUse()) {
			NetworkPingCheckThread npct = new NetworkPingCheckThread(networkPingCheckConfig, state, dbAccessManager, historyManager);
			mainThreadMap.put(npct.getThreadId(), npct);
		}
		
		// REMOTE CM COMMAND 처리 서버 수행.
		UDPServer server = new UDPServer(new RemoteCMCommandExecuteJob(), fc.getDirectHeartBeatPort());
		// server.start();
		if (!server.start()){
			logger.debug("System Exit. by RemoteCMCommandExecuteJob Initialize Fail");
			System.exit(0);
		}
		
		// log file Check
		if(fc.getFileSizeTraceItemMap().size() > 0) {
			FileSizeTraceThread fstt = new FileSizeTraceThread(fc, state, historyManager);
			mainThreadMap.put(fstt.getThreadId(), fstt);
		}
		
		// logAndHistoryManager
		LogAndHistoryManageThread lahmt = new LogAndHistoryManageThread(fc);
		mainThreadMap.put(lahmt.getThreadId(), lahmt);
		
		// failover를 사용해야 해준다.... 안쓰면 켜지도 말등가.
		if (fc.isFailoverUse()) { 
			UDPSender sender = null;
			try {
				sender = new UDPSender(fc.getDirectHeartBeatIpaddress(), fc.getDirectHeartBeatPort());
			} catch (Exception e) {
				
			}
			
			for (LocalProcess lp : fc.getLocalProcessMap().values()) {
				// local process에 대한 상태체크 쓰레드. (failover use를 해야 thread를 돌려준다)
				if (lp.isFailoverUse()) {
					AbstractPolicyThread apt = null;
					switch (lp.getPolicy()) {
						case VIPBASE:
							apt = new VIPBaseCheckThread(dbAccessManager, state, lp, sender, historyManager);
							break;
						case ANYHOST :
							apt = new AnyHostBaseCheckThread(dbAccessManager, state, lp, sender, historyManager);
							break;
						case NETDBBASE :
						default :
							apt = new NetDBBaseCheckThread(dbAccessManager, state, lp, sender, historyManager);
							break;
					}
					mainThreadMap.put(apt.getThreadId(), apt);
				}
			}
		}
	}

	/**
	 * 무조건 시킨다. 안시키면 켜지는 의미가 없다.. 근데 안시켜질일이 별로 없다..
	 */
	private void initializeConfiguration() {
		while (true) {
			if (fc == null) {
				fc = FailoverConfig.getInstance();
				try { Thread.sleep(500); } catch (Exception ignore) {}
			} else {
				break;
			}
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		state.setLocalNetCheckUse(fc.getLocalNetCheckConfig().isUse());
		state.setPublicNetCheckUse(fc.getPublicNetCheckConfig().isUse());
	}
	
	/**
	 * 무조건 커넥션 받아보고 dbState 받아보고 시작함! 안그럼 정상동작을 보장하지 못한다.
	 */
	private void initializeDBState() {
		// 2019.09.02 by JJW : hostType 호출
		String hostServiceType = CommonConfig.getInstance().getHostServiceType();
		OcsProcessVersionVO dbVersion = null;
		String oldVersion = null;
		String[] buildDateArray = null;
		String buildDate = null;
		buildDateArray = getBuildId().split("_");
		buildDate = buildDateArray[1];
		String moduleName = xcsclustermanager;
		String newVersion = moduleName + "_" + getVersion() + "_" + buildDate
				+ "_" + getIncludeInfo();
		while (true) {
			if (dbAccessManager == null) {
				dbAccessManager = new DBAccessManager();
				System.out.println("DBAccessManager instance created.");
				continue;
			} 
			if (dbAccessManager.isDBConnected() == false) {
				System.out.println("dbAccessManager.isDBConnected()==false");
			} else {
				// 빈거 아무거나 가져오겠지..
				OcsDBStateVO dbState = OcsDBStateDAO.retrieveProcessState(OcsConstant.INSERVICE, dbAccessManager);
				// 2019.09.02 by JJW : Version 클래스 실행
				dbVersion = OcsProcessVersionDAO.retrieveProcessVersion(xcsclustermanager, dbAccessManager);
				if(hostServiceType.equalsIgnoreCase("Primary")){
					oldVersion = moduleName+"_"+dbVersion.getPrimary_Version()+"_"+dbVersion.getPrimary_Bulid_Date()+"_"+dbVersion.getPrimary_Include_Version();
				}else{
					oldVersion = moduleName+"_"+dbVersion.getSecondary_Version()+"_"+dbVersion.getSecondary_Bulid_Date()+"_"+dbVersion.getSecondary_Include_Version();
				}
				if(!oldVersion.equalsIgnoreCase(newVersion)){
					OcsProcessVersionDAO.registerVersion(dbAccessManager, hostServiceType, moduleName, getVersion(), buildDate, getIncludeInfo());
					OcsProcessVersionDAO.historyVersion(dbAccessManager, moduleName, hostServiceType, getVersion(), buildDate, getIncludeInfo());
				}
				if (dbState == null) {
					System.out.println("retrieve dbState fail.");
				} else {
					break;
				}
			}
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		boolean isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(CommonConfig.getInstance().getHostServiceType());
		
		historyManager.addClusterInfo(MODE.INIT, isPrimary);
		historyManager.addClusterHistory(EVENT_TYPE.CM_STARTUP, ClusterConstant.CLUSTERMANAGER, isPrimary, "OCS ClusterManager STARTUP.");
	}
	
	/**
	 * Initialize 상태를 알려준다.
	 * @return INIT_STATE
	 */
	public INIT_STATE getInitializeState() {
		return state.getInitState();
	}
	
	/**
	 * Main Method of XCSClusterManager Class.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new XCSClusterManager();
	}
	
	private String getVersion() {
		return com.samsung.ocs.VersionInfo.getString(VERSION);
	}

	private String getBuildId() {
		return com.samsung.ocs.VersionInfo.getString(BUILDID);
	}

	private String getIncludeInfo() {
		StringBuilder include = new StringBuilder();
		include.append("[").append(com.samsung.ocs.common.VersionInfo.getString(BUILDID)).append("]");
		return include.toString();
	}
	
	private static final String CLUSTERMANAGER_EXCEPTION = "ClusterManagerException";
	private static Logger clusterManagerExceptionLog = Logger.getLogger(CLUSTERMANAGER_EXCEPTION);
	public void traceException(String message) {
		clusterManagerExceptionLog.error(String.format("%s", message));
	}
	public void traceException(String message, Throwable t) {
		clusterManagerExceptionLog.error(String.format("%s", message), t);
	}

}
