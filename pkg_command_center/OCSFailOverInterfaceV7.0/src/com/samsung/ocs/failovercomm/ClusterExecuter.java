package com.samsung.ocs.failovercomm;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.OCSMain;
import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.failovercomm.config.ModuleConfig;
import com.samsung.ocs.failovercomm.dao.OcsProcessStateDAO;
import com.samsung.ocs.failovercomm.dao.OcsProcessStateVO;
import com.samsung.ocs.failovercomm.dao.OcsProcessVersionDAO;
import com.samsung.ocs.failovercomm.dao.OcsProcessVersionVO;
import com.samsung.ocs.failovercomm.thread.ConditionCheckThread;

/**
 * ClusterExecuter Class, OCS 3.0 for Unified FAB
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

public class ClusterExecuter {
	private OCSMain main;
	private ModuleConfig mc = null;

	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTEREXECUTER);
	
	private DBAccessManager dbAccessManager = null;
	private CommonConfig commonConfig = null;
	
	// 2019.09.02 by JJW : HostType 변수 선언
	private String hostServiceType = null;
	
	/**
	 * Constructor of ClusterExecuter class.
	 */
	public ClusterExecuter(OCSMain main) {
		this.main = main;
		log("FAILOVER INTERFACE STARTUP.");
		// config read
		
		if (initializeConfig()) {
			// 잘 읽어와야지 실행한다..
			initializeDBState(); // DB를 읽어올때까지 block
			
			// 2011.11.08 by PMM
			// initializeConfig()가 true이면 mc는 not null이지만, prevent 예방용.
			if (mc != null) {
				log("  - FAILOVER USE : " + mc.isFailoverUse());
				log("  - FAILOVER POLICY : " + mc.getFailoverPolicy());
				if (mc.isFailoverUse()) {
					try {
						ConditionCheckThread srt = new ConditionCheckThread(mc, dbAccessManager, commonConfig, main);
						srt.start();
						log("  - StateReportThread Started.");
					} catch (Exception ignore) {
						log(ignore);
					}
				}
				
				Logger startupLogger = Logger.getLogger(CommonLogFileName.STARTUPHISTORY);
				startupLogger.info(main.getModuleName() + "-ClusterExecuter start up.");
				
				// 2011.11.02 by PMM
				// StartupHistory.log에 Version, BuildId, Include 정보 출력.
				startupLogger.info(" Version:" + main.getVersion());
				startupLogger.info("   Include:" + main.getIncludeInfo());
				startupLogger.info(" BuildID:" + main.getBuildId());
			} else {
				log("  - CONFIGURATION ERROR.");
				System.out.println("  - CONFIGURATION ERROR.");
			}
		} else {
			log("  - CONFIGURATION ERROR.");
			System.out.println("  - CONFIGURATION ERROR.");
		}
	}

	/**
	 * 
	 * @return
	 */
	private boolean initializeConfig() {
		if (mc == null) {
			try {
				mc = new ModuleConfig(main);
			} catch (Exception ignore) { 
				log(ignore); 
			}
			if (mc == null) {
				return false;
			}
		}
		if (commonConfig == null) {
			commonConfig = CommonConfig.getInstance();
			if (commonConfig == null) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 무조건 커넥션 받아보고 dbState 받아보고 시작함! 안그럼 정상동작을 보장하지 못한다.
	 */
	private void initializeDBState() {
		// 2019.09.02 by JJW : hostType 호출
		this.hostServiceType = commonConfig.getHostServiceType();
		OcsProcessVersionVO dbVersion = null;
		String oldVersion = null;
		String [] buildDateArray = null;
		String buildDate = null;
		buildDateArray = main.getBuildId().split("_");
		buildDate = buildDateArray[1];
		String moduleName = main.getModuleName().toLowerCase();
		String newVersion = moduleName+ "_" +main.getVersion() +"_"+ buildDate +"_"+ main.getIncludeInfo();
		
		while (true) {
			if (dbAccessManager == null) {
				dbAccessManager = new DBAccessManager();
				log("DBAccessManager instance created.");
				continue;
			} 
			if (dbAccessManager.isDBConnected() == false) {
				log("dbAccessManager.isDBConnected()==false");
			} else {
				// 빈거 아무거나 가져오겠지..
				OcsProcessStateVO dbState = OcsProcessStateDAO.retrieveProcessState(main.getModuleName().toLowerCase(), dbAccessManager, false);
				// 2019.09.02 by JJW : Version 클래스 실행
				dbVersion = OcsProcessVersionDAO.retrieveProcessVersion(moduleName, dbAccessManager);
				if(hostServiceType.equalsIgnoreCase("Primary")){
					oldVersion = moduleName+"_"+dbVersion.getPrimary_Version()+"_"+dbVersion.getPrimary_Bulid_Date()+"_"+dbVersion.getPrimary_Include_Version();
				}else{
					oldVersion = moduleName+"_"+dbVersion.getSecondary_Version()+"_"+dbVersion.getSecondary_Bulid_Date()+"_"+dbVersion.getSecondary_Include_Version();
				}
				if(!oldVersion.equalsIgnoreCase(newVersion)){
					OcsProcessVersionDAO.registerVersion(main, dbAccessManager, hostServiceType);
					OcsProcessVersionDAO.historyVersion(main, dbAccessManager, hostServiceType);
				}
				if (dbState == null) {
					log("retrieve dbState fail. MODULENAME["+main.getModuleName()+"]");
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
	}
	
	private void log(String log) {
		logger.debug(log);
	}
	private void log(Throwable w) {
		logger.debug(w);
	}
}
