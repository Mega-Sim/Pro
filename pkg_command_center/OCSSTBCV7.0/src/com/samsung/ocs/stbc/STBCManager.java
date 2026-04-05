package com.samsung.ocs.stbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.RUNTIME_UPDATE;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.EventHistoryManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.STBCarrierLocManager;
import com.samsung.ocs.manager.impl.STBInfoManager;
import com.samsung.ocs.manager.impl.STBRfcDataManager;
import com.samsung.ocs.manager.impl.STBStateManager;
import com.samsung.ocs.manager.impl.model.Alarm;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.OCSInfo;
import com.samsung.ocs.manager.impl.model.STBCarrierLoc;
import com.samsung.ocs.manager.impl.model.STBInfo;
import com.samsung.ocs.manager.impl.model.STBRfcData;
import com.samsung.ocs.manager.impl.model.STBState;
import com.samsung.ocs.stbc.rfc.RFCManager;
import com.samsung.ocs.stbc.rfc.model.EventEntry;
import com.samsung.ocs.stbc.rfc.model.commmsg.Read;
import com.samsung.ocs.stbc.rfc.model.commmsg.Read2;
import com.samsung.ocs.stbc.rfc.model.commmsg.ReadAll;
import com.samsung.ocs.stbc.rfc.model.commmsg.ReadAll2;
import com.samsung.ocs.stbc.rfc.model.commmsg.Verify;
import com.samsung.ocs.stbc.rfc.model.commmsg.Verify2;
import com.samsung.sem.items.ReportItems;

/**
 * STBCManager Class, OCS 3.0 for Unified FAB
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

public class STBCManager extends AbstractOcsThread implements STBCConstant{
	private boolean isSTBCUsed;
	private boolean isRFCUsed; // 2012.04.05 by KYK	
	private int verifyTimeout = 15;
	private int stbDataSavePeriod = 300;
	private long stbDataSaveTime = 0;
	private long checkTime = 0;
	private long processingTime = 0;

	private STBC stbc;
	private RFCManager rfcManager;
	// DataManager
	private AlarmManager alarmManager;
	private EventHistoryManager eventHistoryManager; // 2013.01.04 by KYK
	private OCSInfoManager ocsInfoManager;
	private STBInfoManager stbInfoManager;
	private STBStateManager stbStateManager;
//	private STBCHistoryManager stbcHistoryManager;
	private STBRfcDataManager stbRfcDataManager;	
	private STBCarrierLocManager stbCarrierLocManager;

	private MODULE_STATE serviceState = MODULE_STATE.OUTOFSERVICE;
	private MODULE_STATE requestedServiceState = MODULE_STATE.REQOUTOFSERVICE;

//	private static int DELAY_TIME_MILLSECOND = 100; // 2012.04.06 by KYK (ms)	
	private static final String RUNTIMEUPDATE_START_INSERVICE = "Runtime Layout Update Started! - INSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_INSERVICE = "Runtime Layout Update Completed! - INSERVICE";
	private static final String RUNTIMEUPDATE_START_OUTOFSERVICE = "Runtime Layout Update Started! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE = "Runtime Layout Update Completed! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_FAILED = "Runtime Layout Update Failed!";
	private static final String INITIALIZING_STBCARRIERLOCMANAGER = "Initializing STBCarrierLocManager ...";
	// 2013.07.26 by KYK
	private static final String INITIALIZING_STBRFCDATAMANAGER = "Initializing STBRfcDataManager ...";

	// 2013.01.04 by KYK : 포트비사용 이유기록
	private static final String UNUSED_BY_SYSTEM_CARRIER_REMOVED_IN_ERRORSTB = "[UNUSED BY SYSTEM] CARRIER REMOVED IN ERRORSTB";
	private static final String UNUSED_BY_SYSTEM_UNLOAD_VERIFY_TIMEOUT = "[UNUSED BY SYSTEM] UNLOAD VERIFY TIMEOUT";
	private static final String UNUSED_BY_SYSTEM_RFC_ABNORMAL_STATE = "[UNUSED BY SYSTEM] RFC ABNORMAL STATE";	
	// 2013.01.04 by KYK : errorSTB 이유기록
	private static final String ERRORSTB_LOAD_VERIFY_NG = "[ERRORSTB] LOAD VERIFY NG";	
	private static final String ERRORSTB_UNLOAD_VERIFY_NG = "[ERRORSTB] UNLOAD VERIFY NG";	
	private static final String ERRORSTB_RFC_IDREAD_TIMEOUT = "[ERRORSTB] IDREAD TIMEOUT";	
	private static final String ERRORSTB_LOAD_VERIFY_TIMEOUT = "[ERRORSTB] LOAD VERIFY TIMEOUT";	
	private static final String ERRORSTBDUP_CARRIER_DUPLICATE_BY_VERIFY = "[ERRORSTBDUP] CARRIER DUPLICATE BY VERIFY";		
	private static final String ERRORSTBDUP_CARRIER_DUPLICATE_BY_IDREAD = "[ERRORSTBDUP] CARRIER DUPLICATE BY IDREAD";	
	// 2013.10.11 by KYK
	private static final String ERRORSTB_IDREAD_FAIL = "[ERRORSTB] IDREAD FAIL";
	
	// Singleton : clusterManager 에서 InService REQ (인스턴스 중복 생성방지)
	private static STBCManager instance;
	public static synchronized STBCManager getInstance(){
		if (instance == null) {
			instance = new STBCManager();
		}
		return instance;
	}

	/**
	 * Constructor of STBCManager class.
	 */
	private STBCManager(){
		super();
		initializeSTBCManager();
		new LogManager(OcsConstant.STBC);
		// 2013.01.29 by KYK : DBdelay 발생 (delete 시) -> 사용안하도록함 (향후 formattedLog로 대체함)
//		new HistoryManager();
	}	

	/**
	 * Initialize STBC Manager
	 */
	private void initializeSTBCManager() {
		DBAccessManager dbAccessManager = new DBAccessManager();
		stbStateManager = STBStateManager.getInstance(STBState.class, dbAccessManager, true, false, 0);
		ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, dbAccessManager, true, true, 500);
		stbInfoManager = STBInfoManager.getInstance(STBInfo.class, dbAccessManager, true, true, 500);
		// 2014.12.19 by KYK TODO
//		stbCarrierLocManager = STBCarrierLocManager.getInstance(STBCarrierLoc.class, dbAccessManager, true, true, 500);	
		stbCarrierLocManager = STBCarrierLocManager.getInstance(STBCarrierLoc.class, dbAccessManager, true, false, 0);	
		// 2013.01.25 by KYK : db delete 시 delay발생 -> 사용안하도록함
	//	stbcHistoryManager = STBCHistoryManager.getInstance(STBCHistory.class, dbAccessManager, true, true, 500);
		stbRfcDataManager = STBRfcDataManager.getInstance(STBRfcData.class, dbAccessManager, true, true, 500);
		alarmManager = AlarmManager.getInstance(Alarm.class, dbAccessManager, false, false, 500);
		eventHistoryManager = EventHistoryManager.getInstance(EventHistory.class, dbAccessManager, true, true, 500); // 2013.01.04 by KYK
		
		getParametersFromDB();
		// 2012.07.04 by KYK : runtime data backup 은 미사용하기로 함 (너무많아;)
		// 2012.04.06 by KYK : file writing -> log4j 방식으로 변경
//		saveSTBDataToFile(); // saveRuntimeDataToFile 이어야하지않나?				
//		saveRuntimeSTBDataToLog();

		traceSTBCMain("***************************************");
		traceSTBCMain("* STBCManager's initialized           *");
		traceSTBCMain("***************************************");
	}
	
	private boolean getParametersFromDB() {
		// 2013.01.04 by KYK : 파라미터 일원화
		stbDataSavePeriod = ocsInfoManager.getStbDataSavePeriod();
		verifyTimeout = ocsInfoManager.getRfcVerifyingTimeout();
		isRFCUsed = ocsInfoManager.isRFCUsed();
		return true;
	}
	
	private void checkCurrentDBStatus() {
		ocsInfoManager.checkCurrentDBStatus();
		stbInfoManager.checkCurrentDBStatus();
		stbCarrierLocManager.checkCurrentDBStatus();
		stbRfcDataManager.checkCurrentDBStatus();
		alarmManager.checkCurrentDBStatus();
	}

	public int getVerifyTimeout() {
		return verifyTimeout;
	}
	
	public boolean isRFCUsed() {
		return isRFCUsed;
	}

	/**
	 * Start Service
	 * 
	 * @return
	 */
	private boolean startService(){
		checkCurrentDBStatus();
		if (stbc == null && ocsInfoManager.isSTBCUsed()) {
			if (rfcManager == null) {
				rfcManager = new RFCManager();
			}
			stbc = new STBC(this, rfcManager);
			traceSTBCMain("***************************************");
			traceSTBCMain("* STBC Service is Activated!          *");
			traceSTBCMain("***************************************");
		}
		return true;
	}

	/**
	 * Stop Service
	 * 
	 * @return
	 */
	private boolean stopService(){
		if (stbc != null) {
			if (stbc.stopSTBC()) {
				stbc = null;
				rfcManager = null;
			} else {
				return false;
			}			
		}
		return true;
	}

	@Override
	public String getThreadId() {
		// 2012.03.05 by PMM
//		return null;
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = 500;
	}

	@Override
	protected void stopProcessing() {
		traceSTBCMain("STBCManager Thread is stopped!");
	}

	/**
	 * Main Processing Method
	 */
	@Override
	protected void mainProcessing() {
		stbcProcess();
	}

	/**
	 * STBCManager 의 Main Process
	 */
	private void stbcProcess() {
		// 2012.04.06 by KYK : Check DelayTime
		checkTime = Math.abs(System.currentTimeMillis());
		// ------------------------------------------------------
		// 0. STBCUSAGE & ServiceOwnership 체크하여 서비스 start/stop 
		manageServiceState();

		// 1. Runtime 으로 변경된 기준정보 업데이트
		manageRuntimeUpdate();

		// INSERVICE && TSC_AUTO
		if (isInWorkingCondition()) {
			
			// 2014.12.19 by KYK TODO
			updateDataFromDB();
			
			// 2. report PortState(Enable/Disable) To MCS
			reportPortStateChange();

			// 3. manage carrier Transferring by OCSOperation
			manageOCSOperation();

			// ■ RFC 기능사용시에만 처리
			if (isRFCUsed) {
				// 4. manage Data from RFC
				manageResultDataFromRFC();

				// 5. manage RFC state (Error RFC Handling)
				// 2012.03.12 by KYK 기능적용
				manageRFCErrorOcurred();
			}

			// 2014.12.19 by KYK TODO
			updateDataToDB();

			// 6. save STB Backup Data To file
			// 2012.04.06 by KYK
//			saveSTBDataToFile();				
			saveSTBDataToLog();
		}		
		// -----------------------------------------------
		// 2012.04.06 by KYK : DelayTime Check
		processingTime = Math.abs(System.currentTimeMillis()) - checkTime;
		if (processingTime > ocsInfoManager.getStbcDelayLimit()) {
			traceSTBCDelay("[1cycle-time] mainProcessing(), processingTime(ms): " + processingTime);
		}
		
	}

	/**
	 * 2014.12.19 by KYK TODO
	 */
	private void updateDataToDB() {
		stbCarrierLocManager.updateSTBPortServiceState();
	}

	/**
	 * 2014.12.19 by KYK TODO
	 */
	private void updateDataFromDB() {
		stbCarrierLocManager.updateFromDBImpl();
	}

	private void testRfc() {
//		if (true) return;		
		// CarrierDetect : 0 [Not detect Carrier], 1 [Detect Carrier]
		// Read result : 0 [OK], 1 [NG(ID Read Error)], 2 [NotUse]
		// verifyIdReadingResult(int readResult, int carrierDetect, String idData, String carrierLocId) {
		
//		// NORMAL
//		System.out.println("NORMAL -----------------------------");
//		stbc.sendS6F11(CarrierIDRead, verifyIdReadingResult(0, 0, "", "S1GB102R_004011"));
//
//		// NORMAL
//		System.out.println("NORMAL -----------------------------");
//		stbc.sendS6F11(CarrierIDRead, verifyIdReadingResult(0, 1, "AA", "S1GB102R_004012"));
//
//		// MISMATCH
//		System.out.println("MISMATCH -----------------------------");
//		stbc.sendS6F11(CarrierIDRead, verifyIdReadingResult(0, 1, "BB", "S1GB102R_004012"));
//
//		// DUPLICATE
//		System.out.println("DUPLICATE -----------------------------");
//		stbc.sendS6F11(CarrierIDRead, verifyIdReadingResult(0, 1, "BB", "S1GB102R_004011"));
//
//		// MISMATCH & DUPLICATE
//		System.out.println("MISMATCH & DUPLICATE -----------------------------");
//		stbc.sendS6F11(CarrierIDRead, verifyIdReadingResult(0, 1, "CC", "S1GB102R_004012"));
//
//		// READ FAIL
//		System.out.println("READ FAIL -----------------------------");
//		stbc.sendS6F11(CarrierIDRead, verifyIdReadingResult(1, 1, "CC", "S1GB102R_004012"));

		// CarrierDetect : 0 [Not detect Carrier] 1 [Detect Carrier]
		// Verify result : 0 [OK(Match)] 1 [NG(Carrier Detect Mismatch)] 9 [NG(STB Disable)] 3 [NotUse]
		// Read result : 0 [OK] 1 [NG(ID Read Error)] 2 [NotUse]
		// int verifyResult, int readResult, int carrierDetect, String idData, String carrierLocId		
		// manageVerifyingResult(verifyResult, readResult, carrierDetect, idData, carrierLocId)
		
//		try {
//			// NORMAL UNLOADED
//			System.out.println("NORMAL UNLOADED-----------------------------");
//			manageVerifyingResult(0, 0, 0, "", "S1GB102R_004012");
//			sleep(2000);
//			// NORMAL LOADED		
//			System.out.println("NORMAL LOADED-----------------------------");
//			manageVerifyingResult(0, 0, 1, "AA", "S1GB102R_004013");
//			sleep(2000);		
//			// LOAD CARRIER MISMATCH
//			System.out.println("LOAD CARRIER MISMATCH-----------------------------");
//			manageVerifyingResult(0, 0, 1, "CC", "S1GB102R_004013");
//			sleep(2000);		
//			// LOAD CARRIER NOT FOUND & DUPLICATE
//			System.out.println("LOAD CARRIER NOT FOUND & DUPLICATE-----------------------------");
//			manageVerifyingResult(0, 0, 1, "CC", "S1GB102R_004012");
//			sleep(2000);		
//			// LOAD CARRIER MISMATCH & DUPLICATE
//			System.out.println("LOAD CARRIER MISMATCH & DUPLICATE-----------------------------");
//			manageVerifyingResult(0, 0, 1, "DD", "S1GB102R_004012");
//			// LOAD VERIFY NG
//			System.out.println("LOAD VERIFY NG-----------------------------");
//			manageVerifyingResult(1, 0, 0, "", "S1GB102R_004012");
//			sleep(2000);
//			// UNLOAD VERIFY NG
//			System.out.println("UNLOAD VERIFY NG-----------------------------");
//			manageVerifyingResult(1, 0, 1, "A", "S1GB102R_004014");
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}				
	}

	/**
	 * STBCUSAGE (OCSINFO) 와 SERVICESTATE (OCS_CLUSTER_STATE) 를 체크하여 STBC Service 실행여부 결정
	 * STBCUSAGE=YES, INSERVICE (or REQINSERVICE) 일때 동작 (STBC 인스턴스생성) / 정지일 경우, 인스턴스제거 
	 */
	private void manageServiceState() {
		try {
			// STBCUSAGE : YES
			if (ocsInfoManager.isSTBCUsed()) {
				// STBCUSAGE : NO -> YES 로 변경됨
				if (isSTBCUsed != ocsInfoManager.isSTBCUsed()) {
					if (this.requestedServiceState == MODULE_STATE.REQINSERVICE ||
							this.serviceState == MODULE_STATE.INSERVICE) {
						changeServiceState(MODULE_STATE.INSERVICE);
						startService();					
					}
					isSTBCUsed = ocsInfoManager.isSTBCUsed();
				} else {
					// OUTOFSERVICE 요청받음
					if (this.requestedServiceState == MODULE_STATE.REQOUTOFSERVICE) {
						changeServiceState(MODULE_STATE.OUTOFSERVICE);
						stopService();
						traceSTBCMain("STBC Service is Deactivated!");
					} else if (this.requestedServiceState == MODULE_STATE.REQINSERVICE &&
							this.serviceState != MODULE_STATE.INSERVICE) {
						// INSERVICE 요청받음 (현재 INSERVICE 아닐 경우 동작)
						changeServiceState(MODULE_STATE.INSERVICE);
						startService();
					}
				}
				
				// 2012.01.06 by PMM
				if (this.serviceState == MODULE_STATE.INSERVICE) {
					if (stbc != null) {
						stbc.setFormattedLogUsed(ocsInfoManager.isFormattedLogUsed());
					}
				}
			} else {
				// STBCUSAGE : NO
				// STBCUSAGE : YES -> NO 로 변경됨
				if (isSTBCUsed != ocsInfoManager.isSTBCUsed()) {
					changeServiceState(MODULE_STATE.OUTOFSERVICE);
					stopService();
					traceSTBCMain("STBC Service is Deactivated!");				
					isSTBCUsed = ocsInfoManager.isSTBCUsed();
				}
			}			
		} catch (Exception e) {
			traceException("manageServiceState()", e);
		}
	}

	/**
	 * STBCUDATE (OCSINFO) 를 확인하여 RuntimeUpdate 를 진행한다.
	 * 1. InServiceHost 에서 RuntimeUpdate 를 진행 : YES -> DONE
	 * 2. OutOfService 인 모듈에서는 Primary Update 완료후 진행 : DONE -> NO
	 */
	private void manageRuntimeUpdate() {
		assert ocsInfoManager != null;
		
		if (ocsInfoManager.getStbcUpdate() != RUNTIME_UPDATE.NO) {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				if (ocsInfoManager.getStbcUpdate() == RUNTIME_UPDATE.YES) {
//					registerAlarmText(RUNTIMEUPDATE_START_INSERVICE);
					registerAlarmTextWithLevel(RUNTIMEUPDATE_START_INSERVICE, ALARMLEVEL.INFORMATION);
					traceRuntimeUpdate(RUNTIMEUPDATE_START_INSERVICE);
					
					// Manager Update!
					updateManager();
					restartRFC();
					
					unregisterAlarmText(RUNTIMEUPDATE_START_INSERVICE);
					traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_INSERVICE);
					ocsInfoManager.setSTBCUpdate(RUNTIME_UPDATE.DONE);
				} else if (ocsInfoManager.getStbcUpdate() != RUNTIME_UPDATE.DONE) {
					ocsInfoManager.setSTBCUpdate(RUNTIME_UPDATE.NO);
				}
			} else if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
				if (ocsInfoManager.getStbcUpdate() == RUNTIME_UPDATE.DONE) {
//					registerAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
					registerAlarmTextWithLevel(RUNTIMEUPDATE_START_OUTOFSERVICE, ALARMLEVEL.INFORMATION);
					traceRuntimeUpdate(RUNTIMEUPDATE_START_OUTOFSERVICE);
					
					updateManager();
					
					unregisterAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
					traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE);
					ocsInfoManager.setSTBCUpdate(RUNTIME_UPDATE.NO);
				}
			}
		}
	}

	// 2014.12.19 by KYK
//	private void registerAlarmTextWithLevel(String alarmText, ALARMLEVEL alarmLevel) {
	public void registerAlarmTextWithLevel(String alarmText, ALARMLEVEL alarmLevel) {
		if (alarmManager != null) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.registerAlarmTextWithLevel(STBC, alarmText, alarmLevel.toConstString());
		}
	}

	public void registerAlarmText(String alarmText) {
		alarmManager.registerAlarmText(STBC, alarmText);		
	}

	public void unregisterAlarmText(String alarmText) {
		alarmManager.unregisterAlarmText(STBC, alarmText);				
	}

	private void restartRFC() {
		// 2012.04.05 by KYK
		rfcManager.stopRFCManager();			
		if (isRFCUsed) {
			rfcManager.startRFCManager();
		}
	}

	/**
	 * Runtime Update Managers
	 */
	private void updateManager() {
		assert stbCarrierLocManager != null;
		try {
			// 2013.07.26 by KYK
			getParametersFromDB();
//			registerAlarmText(INITIALIZING_STBCARRIERLOCMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_STBCARRIERLOCMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_STBCARRIERLOCMANAGER);
			// data reset.
//			initializeFromDB();
			stbCarrierLocManager.initializeFromDB();
			unregisterAlarmText(INITIALIZING_STBCARRIERLOCMANAGER);
			
//			registerAlarmText(INITIALIZING_STBRFCDATAMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_STBRFCDATAMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_STBRFCDATAMANAGER);
			stbRfcDataManager.initializeFromDB();
			unregisterAlarmText(INITIALIZING_STBRFCDATAMANAGER);
			
		} catch (Exception e) {
			traceRuntimeUpdate(RUNTIMEUPDATE_FAILED);
			traceException("updateManager()", e);
		}
	}

	/**
	 * Initialize From DB
	 * 
	 * @return
	 */
	private boolean initializeFromDB() {		
		getParametersFromDB();
		
		// 2011.11.09 by PMM
//		stbCarrierLocManager.updateFromDBAll();
		stbCarrierLocManager.initializeFromDB();
		return true;
	}

	/**
	 * PortState (ENABLED : TRUE,FALSE) 변경내역을 MCS에 Report 한다.
	 */
	public void reportPortStateChange() {

		// INSERVICE && TSC_AUTO
		if (isInWorkingCondition() == false) {
			return;
		}
		boolean isEnabled;
		ReportItems rItems;		
		ConcurrentHashMap<String, Boolean> portStateChangeTable = stbCarrierLocManager.getPortStateChangeTable();		
		for (Iterator<String> iter = portStateChangeTable.keySet().iterator(); iter.hasNext();) {
			String carrierLocId = iter.next();
			isEnabled = portStateChangeTable.get(carrierLocId);
			rItems = new ReportItems();
			rItems.setCarrierLoc(carrierLocId);
			if (isEnabled) {
				stbc.sendS6F11(PortInService, rItems);
			} else {
				stbc.sendS6F11(PortOutOfService, rItems);				
			}
			portStateChangeTable.remove(carrierLocId);
		}

//		boolean isEnabled;
//		ReportItems reportItems;
//		ArrayList<String> portStateChangedList = new ArrayList<String>();
//		ConcurrentHashMap<String, Boolean> portStateChangeMap = new ConcurrentHashMap<String, Boolean>();
//		stbCarrierLocManager.getPortStateChangeMapClone(portStateChangeMap);		
//		
//		for (String carrierLocId: portStateChangeMap.keySet()) {
//			isEnabled = portStateChangeMap.get(carrierLocId);
//			reportItems = new ReportItems();
//			reportItems.setCarrierLoc(carrierLocId);
//			if (isEnabled) {
//				stbc.sendS6F11(PortInService, reportItems);
//			} else {
//				stbc.sendS6F11(PortOutOfService, reportItems);				
//			}
//			portStateChangedList.add(carrierLocId);
//		}
//		if (portStateChangedList.size() > 0) {
//			stbCarrierLocManager.removePortStateChangedData(portStateChangedList);			
//		}
		
	}

	/**
	 * Carrier Transfer 에 따른 STB(UTB) 재하의 변동을 처리한다.
	 *  OHT가 carrier 반송 중 , carrier를 SourcePort에서 뜨고, DestPort에 내려놓을 때 이벤트 생성
	 *  CARRIERLOC 테이블 commandName, carrierLocId, mcsCarrierId, ocsCarrierId, idReader 에 기록
	 *  이 기록을 STBC 쓰레드에서 주기적으로 읽어와 재하를 관리 (처리) 
	 *  
	 * 1. STB가 SourceLoc 인 경우 : OHT Unload 시 (OHT <- STB) / STBC : CarrierRemoved 이벤트발생
	 * 2. STB가 DestLoc 인 경우 : OHT Load 시 (OHT -> STB) / STBC : CarrierInstalled 이벤트발생  
	 */
	@SuppressWarnings("unchecked")
	public void manageOCSOperation() {
		
		// INSERVICE && TSC_AUTO
		if (isInWorkingCondition() == false) {
			return;
		}
		// 2012.04.06 by KYK : Check DelayTime
		long checkTime = Math.abs(System.currentTimeMillis());
		
		int count = 0;
		String commandName;
		// 2012.05.25 by KYK
		if (stbCarrierLocManager.getOcsCommandTable() == null || stbCarrierLocManager.getOcsCommandTable().isEmpty()) {
			return;
		}
		// 다른 쓰레드에서 동시참조 (ConcurrentModificationException)
//		HashMap<String, STBCarrierLoc> ocsCommandTable = stbCarrierLocManager.getOcsCommandTable();			
		HashMap<String, STBCarrierLoc> ocsCommandTable = (HashMap<String, STBCarrierLoc>) stbCarrierLocManager.getOcsCommandTable().clone();
		for (STBCarrierLoc carrierLoc:ocsCommandTable.values()) {
			commandName = carrierLoc.getCommandName();
			if ("INSTALL".equals(commandName)) {		
				manageOcsInstallCmd(carrierLoc);
				count++;
			} else if ("REMOVE".equals(commandName)) {				
				manageOcsRemoveCmd(carrierLoc);				
				count++;
			} else if ("IDCHECK".equals(commandName)) {
				// 현재 이런 commandName 에대한 처리는 없음 Operation 도 없음 ??
				; /*NULL*/
			}			
		}		
		// RuntimeSTBData
		if (count > 0) {
			// 2012.07.04 by KYK : runtime data backup 은 미사용하기로 함 (너무많아;)
			// 2012.04.06 by KYK : file writing -> log4j 방식으로 변경
//			saveRuntimeSTBDataToFile();
//			saveRuntimeSTBDataToLog();
		}		
		// 2012.04.06 by KYK : DelayTime Check
		long processingTime = System.currentTimeMillis() - checkTime;
		if (processingTime > ocsInfoManager.getStbcDelayLimit()) {
			StringBuilder sb = new StringBuilder("[3].manageOCSOperation(), commandCount: ");
			sb.append(count).append(" / processingTime(ms):").append(processingTime);
			traceSTBCDelay(sb.toString());
		}

		return;
	}

	private boolean isInWorkingCondition() {
		if (stbc == null ||
				("TSC_AUTO".equals(stbc.getTscStatus()) == false) ||
				(this.serviceState != MODULE_STATE.INSERVICE)) {
			return false;
		}
		return true;
	}

	/**
	 * [When Carrier Transfer from a STB]
	 * 1. carrier Validation Check
	 * 2. commandName 에 따른 carrierId (INSTALL or REMOVE)진행
	 * 3. sendS6F11 (CarrierInstalled or CarrierRemoved)	 
	 * 
	 * □ Normal Case : UNLOADED FROM STB (or UTB) As SourceLoc
	 * ■ Abnormal Case 
	 *  1) Vehicle Acquired : Carrier Mismatch 
	 *  2) Vehicle Acquired : Carrier Mismatch & Duplicate 
	 *  3) Vehicle Acquired : Carrier Not Found 
	 *  4) Vehicle Acquired : Carrier Not Found & Duplicate	

	 * ■ + ID read via RFC
	 *  Carrier Transfer from a STB(with RFC) : invalid load exist
	 *  Carrier Transfer from a STB(with RFC) : invalid load exist & Duplicate
	 *  Carrier Transfer from a STB(with RFC) : invalid load exist & read fail
	 *  
	 * @param carrierLoc
	 */
	private void manageOcsRemoveCmd(STBCarrierLoc carrierLoc) {
		String carrierLocId = carrierLoc.getCarrierLocId();
		String mcsCarrierId = carrierLoc.getMcsCarrierId();
		String mcsFoupId = carrierLoc.getMcsFoupId();

		StringBuilder sb = new StringBuilder("[OCS->STBC] REMOVE ");
		sb.append("CarrierLoc:").append(carrierLocId).append(", mcsCarrierId: ").append(mcsCarrierId).append(", mcsFoupId: ").append(mcsFoupId);
		traceSTBCMain(sb.toString());

		String log = null; // 2013.01.04 by KYK
		
		// check abnormal			
		STBCarrierLoc dbCarrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(mcsCarrierId);
		STBCarrierLoc carrierLocOnPort = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
		String carrierIdOnPort = carrierLocOnPort.getCarrierId();

		// Q1 : carrierId data 가 DB 에 존재하는가? (REMOVE 해야하기때문에 이미 존재해 있어야 정상)
		if (dbCarrierLoc != null) { // OK (Carrier Exist) 
			// Q2 : carrier 위치는 바로 여기가 맞나?
			if (carrierLocId.equals(dbCarrierLoc.getCarrierLocId())) { // YES
				// OK ------------------------------------------------------------------
				manageCarrierRemoved(carrierLocId, mcsCarrierId);		
				// ---------------------------------------------------------------------
				// □ Abnormal carrierId : ErrorSTB(DUP) or UnknownSTB 인 경우,
				// ■ Action : 포트를 ENABLED='FALSE' & MCS 에 PortOutOfService 를 보고
				if (isAbnormalCarrierId(mcsCarrierId)){
					// 2013.01.04 by KYK : 포트비사용 이유기록 
					String reason = UNUSED_BY_SYSTEM_CARRIER_REMOVED_IN_ERRORSTB;
					String event = " Port:" + carrierLocId;					
					traceSTBCAbnormal(reason + event);
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.UNUSED_STBPORT,
							EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
							EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.ERRORSTB_CARRIER_REMOVED), false);
//					stbCarrierLocManager.updatePortServiceState(carrierLocId, false);
					stbCarrierLocManager.updatePortServiceState(carrierLocId, false, reason);
					// S6F11(PortOutOfService) 보고는 reportPortState() 에서  처리함 
				}				
				
			} else { // [Carrier NotFound & Duplicate] : That carrier is located at different Port.	
				String unknownCarrierId = stbc.makeUnknownCarrierName(mcsCarrierId);
				String unknownFoupId = stbc.makeUnknownFoupName(mcsFoupId);
				// Q3 : 현재 carrierLocId 위치에 다른 carrier 가 존재하는가?
				if ("".equals(carrierIdOnPort)) { // 아니오 : Carrier Not Found & Duplicate	
					// 2013.01.04 by KYK
					log = "		 Carrier NotFound & Duplicate : Carrier is located at different Port. /carrierId:" + mcsCarrierId;
//					traceSTBCMain("		 Carrier NotFound & Duplicate : Carrier is located at different Port. /carrierId:" + mcsCarrierId);
					traceSTBCMain(log);
					manageCarrierRemoved(dbCarrierLoc.getCarrierLocId(), mcsCarrierId);
					manageCarrierInstalled(dbCarrierLoc.getCarrierLocId(), unknownCarrierId, unknownFoupId);
					// data 정리를 위해 일단 없는 것도 그냥 지우는 걸로 업데이트 하는데, 보고도 해야하는지 좀 애매함
					stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, mcsCarrierId, "", "REMOVE", true);							
					//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, mcsCarrierId, "REMOVE");

				} else { // [Carrier Mismatch & Duplicate] (현재 carrierLocId 위치에 다른 carrier 존재함 ) 
					// 2013.01.04 by KYK
					log = "		 Carrier Mismatch & Duplicate : carrierId:"+mcsCarrierId+" /carrierLocId:"+carrierLocId;	
//					traceSTBCMain("		 Carrier Mismatch & Duplicate : carrierId:"+mcsCarrierId+" /carrierLocId:"+carrierLocId);
					traceSTBCMain(log);

					manageCarrierRemoved(dbCarrierLoc.getCarrierLocId(), mcsCarrierId);
					manageCarrierInstalled(dbCarrierLoc.getCarrierLocId(), unknownCarrierId, unknownFoupId);

					manageCarrierRemoved(carrierLocId, carrierIdOnPort);					
					if (isAbnormalCarrierId(carrierIdOnPort)) {
						// 2013.01.04 by KYK : 포트비사용 이유기록 
						String reason = UNUSED_BY_SYSTEM_CARRIER_REMOVED_IN_ERRORSTB;
						String event = " Port:" + carrierLocId;
						traceSTBCAbnormal(reason + event);
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.UNUSED_STBPORT,
								EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
								EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.ERRORSTB_CARRIER_REMOVED), false);
//						stbCarrierLocManager.updatePortServiceState(carrierLocId, false);
						stbCarrierLocManager.updatePortServiceState(carrierLocId, false, reason);
					}				
				}
			}
		} else {
			// Q1 : carrierId data 가 DB 에 존재하는가? (REMOVE 해야하기때문에 이미 존재해 있어야 정상)
			// [Carrier Not Found] (존재해야 할 carrier 가 없음)

			// Q3 : 현재 carrierLocId 위치에 다른 carrier 가 존재하는가?
			// 2011.11.14 by PMM
//			if ("".equals(carrierIdOnPort)) {
			if (carrierIdOnPort != null && carrierIdOnPort.length() == 0) {
				// [Carrier Not Found] (현재 위치도 비어있음) 
				// 2013.01.04 by KYK
				log = "		 Carrier Not Found (Anywhere) : carrierId:"+mcsCarrierId;
//				traceSTBCMain("		 Carrier Not Found (Anywhere) : carrierId:"+mcsCarrierId);
				traceSTBCMain(log);
				// data 정리를 위해 일단 없는 것도 그냥 지우는 걸로 업데이트 하는데, 보고도 해야하는지 좀 애매함
				stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, mcsCarrierId, "", "REMOVE", true);				
				//				stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, mcsCarrierId, "REMOVE");

			} else { // [Carrier Mismatch] (현재 위치에는 다른 carrier 가 존재함)
				// 2013.01.04 by KYK
				log = "		 Carrier Mismatch : carrierId:"+mcsCarrierId+" /carrierLocId:"+carrierLocId;	
//				traceSTBCMain("		 Carrier Mismatch : carrierId:"+mcsCarrierId+" /carrierLocId:"+carrierLocId);
				traceSTBCMain(log);
				manageCarrierRemoved(carrierLocId, carrierIdOnPort);				
				if (isAbnormalCarrierId(carrierIdOnPort)) {
					// 2013.01.04 by KYK : 포트비사용 이유기록 
					String reason = UNUSED_BY_SYSTEM_CARRIER_REMOVED_IN_ERRORSTB;
					String event = " Port:" + carrierLocId;
					traceSTBCAbnormal(event);
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.UNUSED_STBPORT,
							EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
							EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.ERRORSTB_CARRIER_REMOVED), false);
//					stbCarrierLocManager.updatePortServiceState(carrierLocId, false);
					stbCarrierLocManager.updatePortServiceState(carrierLocId, false, reason);
				}				
			}					
		}				

		// 2013.01.04 by KYK : Abnormal Log
		if (log != null) {
			traceSTBCAbnormal(sb.toString());
			traceSTBCAbnormal(log);
		}
		
		if ((carrierIdOnPort != null && carrierIdOnPort.indexOf("ErrorSTB") >= 0) ||
				(mcsCarrierId != null && mcsCarrierId.indexOf("ErrorSTB") >= 0)) {
			return;
		}
		// RFC Reader 설치 사용
		// 2012.04.05 by KYK : RFCUSAGE Setup Parameter 로 변경
//		if (rfcManager != null && ocsInfoManager.isRFCUsed() && stbc.isIdReaderAvailable(carrierLocId)) {
		if (rfcManager != null && isRFCUsed && stbc.isIdReaderAvailable(carrierLocId)) {
			rfcManager.requestVerify(carrierLocId, verifyTimeout, false);
			traceSTBCMain("[STBC->RFC] REQ_VERIFY Carrier Unloaded[NotExist] On Port:" + carrierLocId);
		}		
	}

	/**
	 * [Carrier Transfer to a STB]
	 * 1. carrier Validation Check
	 * 2. commandName 에 따른 carrierId (INSTALL or REMOVE)진행
	 * 3. sendS6F11 (CarrierInstalled or CarrierRemoved)
	 * 
	 * □ Normal Case : LOADED IN STB (or UTB) As DestLoc
	 * ■ Abnormal Case 
	 * 1. Vehicle Deposited : Carrier Duplicate
	 * 2. Vehicle Deposited : Carrier Mismatch
	 * 3. Vehicle Deposited : Carrier Mismatch & Duplicate
	 * 
	 * ■ + ID read via RFC
	 * Carrier ID Read : Carrier mismatch
	 * Carrier ID Read : Carrier Mismatch & Duplicate
	 * Carrier ID Read : Carrier Duplicate
	 * Carrier Transfer to a STB(with RFC) : no load	 * 
	 * 
	 * @param carrierLoc
	 */
	private void manageOcsInstallCmd(STBCarrierLoc carrierLoc) {
		String carrierLocId = carrierLoc.getCarrierLocId();
		String mcsCarrierId = carrierLoc.getMcsCarrierId();
		String mcsFoupId = carrierLoc.getMcsFoupId();
		
		StringBuilder sb = new StringBuilder("[OCS->STBC] INSTALL ");
		sb.append("CarrierLoc:").append(carrierLocId).append(", mcsCarrierId: ").append(mcsCarrierId).append(", mcsFoupId: ").append(mcsFoupId);
		traceSTBCMain(sb.toString());

		String log = null; // 2013.01.04 by KYK
		
		// check abnormal : 1. 동일 carrierId 가 DB 에 존재하는지 / 2. 현재위치에 어떤 carrie		
		STBCarrierLoc dupCarrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(mcsCarrierId);
		STBCarrierLoc carrierLocOnPort = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);

		// Q1 : 동일한 carrierId 가 DB 다른 곳에 존재하는가?
		if (dupCarrierLoc == null) { // 존재안함, 정상
			// Q2 : 현재 포트는 비어 있는가?
			if ("".equals(carrierLocOnPort.getCarrierId())) { // 비어있음. 정상
				// OK --------------------------------------------------------------------------------
				manageCarrierInstalled(carrierLocId, mcsCarrierId, mcsFoupId);
				// -----------------------------------------------------------------------------------

			} else { // [Carrier Mismatch] 현재 carrierLoc 에 다른 carrier 가 점유
				// 2013.01.04 by KYK
				log = "		 Carrier Mismatch : carrierId:"+mcsCarrierId+",carrierLocId:"+carrierLocId;
//				traceSTBCMain("		 Carrier Mismatch : carrierId:"+mcsCarrierId+",carrierLocId:"+carrierLocId);
				traceSTBCMain(log);
				manageCarrierRemoved(carrierLocId, carrierLocOnPort.getCarrierId());
				manageCarrierInstalled(carrierLocId, mcsCarrierId, mcsFoupId);				
			}
		} else {
			// Q1 : 동일한 carrierId 가 DB 다른 곳에 존재하는가?
			// [Carrier Duplicate] DB 상 동일한 carrier 가 이미 존재함
			String dupCarrierLocId = dupCarrierLoc.getCarrierLocId();
			// Q3 : 동일한 carrier 가 위치한 포트가 바로 여기인가?
			if (carrierLocId.equals(dupCarrierLocId)) {
				// ALREADY REQUESTED
				stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, mcsCarrierId, mcsFoupId, "INSTALL", true);
				//				stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, mcsCarrierId, "INSTALL");
			} else { // [Duplicate] 동일 CarrierId 다른 포트에 존재				
				String unknownCarrierId = stbc.makeUnknownCarrierName(mcsCarrierId);
				String unknownFoupId = stbc.makeUnknownFoupName(mcsFoupId);
				// Q2 : 현재 포트는 비어 있는가?
				if ("".equals(carrierLocOnPort.getCarrierId())) {	// DUPLICATE 
					// 2013.01.04 by KYK
					log = "		 Carrier Duplicate : carrierId:"+mcsCarrierId+",carrierLocId:"+carrierLocId;
//					traceSTBCMain("		 Carrier Duplicate : carrierId:"+mcsCarrierId+",carrierLocId:"+carrierLocId);
					traceSTBCMain(log);
					manageCarrierRemoved(dupCarrierLocId, mcsCarrierId);					
					manageCarrierInstalled(dupCarrierLocId, unknownCarrierId, unknownFoupId);
					manageCarrierInstalled(carrierLocId, mcsCarrierId, mcsFoupId); 
				} else { // [Duplicate & Mismacth] 현재포트에는 다른 carrier 가 존재함
					// 2013.01.04 by KYK
					log = "		 Carrier Duplicate & Mismacth : carrierId:"+mcsCarrierId+",carrierLocId:"+carrierLocId;
//					traceSTBCMain("		 Carrier Duplicate & Mismacth : carrierId:"+mcsCarrierId+",carrierLocId:"+carrierLocId);
					traceSTBCMain(log);
					manageCarrierRemoved(dupCarrierLocId, mcsCarrierId);					
					manageCarrierInstalled(dupCarrierLocId, unknownCarrierId, unknownFoupId);					
					manageCarrierRemoved(carrierLocId, carrierLocOnPort.getCarrierId());					
					manageCarrierInstalled(carrierLocId, mcsCarrierId, mcsFoupId);
				}
			}
		}						
		// 2013.01.04 by KYK : Abnormal Log
		if (log != null) {
			traceSTBCAbnormal(sb.toString());
			traceSTBCAbnormal(log);
		}

		// RFC Reader 설치 사용
		// 2012.04.05 by KYK : RFCUSAGE Setup Parameter 로 변경
//		if (rfcManager != null && ocsInfoManager.isRFCUsed() && stbc.isIdReaderAvailable(carrierLocId)) {
		if (rfcManager != null && isRFCUsed && stbc.isIdReaderAvailable(carrierLocId)) {
			rfcManager.requestVerify(carrierLocId, verifyTimeout, true);
			traceSTBCMain("[STBC->RFC] REQ_VERIFY Carrier Loaded[Exist] On Port:" + carrierLocId);
		}		
	}

	/**
	 * carrierInstalled 에 대한 처리
	 * 1. DB 업데이트 & 2. sendS6F11(CarrierInstalled) 전송
	 * @param carrierLocId
	 * @param carrierId
	 * @param foupId
	 */
	public void manageCarrierInstalled(String carrierLocId, String carrierId, String foupId) {
		stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, foupId, "INSTALL", true);
		// stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, "INSTALL");
		ReportItems rItems = makeSTBReportItems(carrierLocId, carrierId, foupId);
		stbc.sendS6F11(CarrierInstalled, rItems);
	}

	// 2013.01.04 by KYK : Reason 추가
	public void manageCarrierInstalled(String carrierLocId, String carrierId, String foupId, String reason) {
		stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, foupId, "INSTALL", reason, true);
		ReportItems rItems = makeSTBReportItems(carrierLocId, carrierId, foupId);
		stbc.sendS6F11(CarrierInstalled, rItems);
	}

	/**
	 * carrierRemoved 에 대한 처리
	 * 1. DB 업데이트 & 2. sendS6F11(CarrierRemoved) 전송
	 * @param carrierLocId
	 * @param carrierId
	 */
	public void manageCarrierRemoved(String carrierLocId, String carrierId) {
		stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, "", "REMOVE", false);
		// stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, "REMOVE");
		ReportItems rItems = makeSTBReportItems(carrierLocId, carrierId, "");
		stbc.sendS6F11(CarrierRemoved, rItems);		
	}

	/**
	 * Abnormal carrierId 인지 확인한다. 
	 * 1. Verify or IDRead 에대한 RFC 의 응답이 없을때 : ErrorSTBxx
	 * 2. RFC ReadError (ReadFail) 일때 : UNKNOWNSTBxxx
	 * @param carrierId
	 * @return
	 */
	private boolean isAbnormalCarrierId(String carrierId) {
		if (carrierId != null && (carrierId.indexOf("ErrorSTB") >= 0 ||
				carrierId.indexOf("UNKNOWNSTB") >= 0)) {
			return true;
		}
		return false;
	}

	/**
	 * Make STB ReportItems
	 * 
	 * @param carrierLocId
	 * @param mcsCarrierId
	 * @param mcsFoupId
	 * @return
	 */
	private ReportItems makeSTBReportItems(String carrierLocId, String mcsCarrierId, String mcsFoupId) {		
		ReportItems rItems = new ReportItems();
		rItems.setCarrierLoc(carrierLocId);
		rItems.setCarrierId(mcsCarrierId);
		rItems.setFoupId(mcsFoupId);
		return rItems;
	}

	/**
	 * Make STB ReportItems
	 * 
	 * @param carrierLocId
	 * @param carrierId
	 * @param mcsFoupId
	 * @param idReadStatus
	 * @return
	 */
	private ReportItems makeSTBReportItems(String carrierLocId, String carrierId, String mcsFoupId, int idReadStatus) {		
		ReportItems rItems = new ReportItems();
		rItems.setCarrierLoc(carrierLocId);
		rItems.setCarrierId(carrierId);
		rItems.setFoupId(mcsFoupId);
		rItems.setIdReadStatus(idReadStatus);
		return rItems;		
	}

	/**
	 * IDREAD, VERIFY Request 에 대한 RFC 의 응답 결과를 처리하여
	 * MCS 에 결과를 report 한다. (S6F11 CarrierIDRead)
	 */
	private void manageResultDataFromRFC() {	
		
		// INSERVICE && TSC_AUTO
		// 2012.04.05 by KYK
//		if (isInWorkingCondition() == false || rfcManager == null){
		if (isInWorkingCondition() == false || rfcManager == null || isRFCUsed == false){
			return;
		}		
		// 2012.04.06 by KYK : Check DelayTime
		long checkTime = Math.abs(System.currentTimeMillis());

		List<EventEntry> completedEventList = rfcManager.getCompletedEventList();		
		int index = 0;
		String carrierLocId;
		for (int i = 0; i < completedEventList.size(); i++) {
			EventEntry eventEntry = completedEventList.get(i);
			
			switch (eventEntry.getEventType()) {				
				case VERIFY:
				case VERIFY2:
					Verify v = null;
					carrierLocId = eventEntry.getCarrierlocId();
					Iterator it1 = eventEntry.getResponseMap().values().iterator();
					while (it1.hasNext()) {
						v = (Verify) it1.next(); // 여기는 메시지가 하나이다.
					}
					if (v != null) {
						manageVerifyingResult(v);					
					} else { // RFC Timeout (Verify 에 대한 응답이 없음)					
						if (eventEntry.hasCarrier()) { // [Carrier Exist]
							// 2013.01.04 by KYK
							StringBuilder log = new StringBuilder("[RFC->STBC] VERIFY Timeout : A Carrier's Loaded[Exist] On Port:");
							log.append(carrierLocId).append(", but RFC Not Respond ");
//							traceSTBCMain("[RFC->STBC] VERIFY Timeout : A Carrier's Loaded[Exist] On Port:"+carrierLocId+", but RFC Not Respond ");	
							traceSTBCMain(log.toString());	
							traceSTBCAbnormal(log.toString());

							//  carrierLoc 에 있는 carrierRemoved / ErrorSTBxx 로 carrierInstalled
							STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
							String dbCarrierId = carrierLoc.getCarrierId();
							//String errorCarrierId = stbc.makeErrorSTBCarrierName(carrierLoc.getCarrierId());
							String errorCarrierId = stbc.makeErrorSTBCarrierName();
							String errorFoupId = stbc.makeErrorSTBFoupName();
	
							if (dbCarrierId != null && dbCarrierId.length() > 0) {
								manageCarrierRemoved(carrierLocId, carrierLoc.getCarrierId());	
							}					
							// 2013.01.04 by KYK							
							String reason = ERRORSTB_LOAD_VERIFY_TIMEOUT;
//							manageCarrierInstalled(carrierLocId, errorCarrierId);			
							manageCarrierInstalled(carrierLocId, errorCarrierId, errorFoupId, reason);
							// 누락보고 추가
							ReportItems rItems = makeSTBReportItems(carrierLocId, errorCarrierId, errorFoupId, FAILURE); 
							stbc.sendS6F11(CarrierIDRead, rItems);	

							String event = " Port:" + carrierLocId + ", CarrierId:" + errorCarrierId;
							registerEventHistory(new EventHistory(EVENTHISTORY_NAME.ERROR_STB,
									EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
									EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.LOAD_VERIFY_TIMEOUT), false);
							
						} else { // [Carrier Not Exist]
							// 2013.01.04 by KYK
							StringBuilder log = new StringBuilder("[RFC->STBC] VERIFY Timeout : A Carrier's Unloaded[NotExist] On Port:");
							log.append(carrierLocId).append(", but RFC Not Respond / PortOutOfService");
//							traceSTBCMain("[RFC->STBC] VERIFY Timeout : A Carrier's Unloaded[NotExist] On Port:"+carrierLocId+", but RFC Not Respond ");
							traceSTBCMain(log.toString());
							traceSTBCAbnormal(log.toString());
							
							// 누락보고 추가
							ReportItems rItems = makeSTBReportItems(carrierLocId, "", "", FAILURE); 
							stbc.sendS6F11(CarrierIDRead, rItems);	

							String event = " Port:" + carrierLocId;
							registerEventHistory(new EventHistory(EVENTHISTORY_NAME.UNUSED_STBPORT,
									EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
									EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.UNLOAD_VERIFY_TIMEOUT), false);
							
							// Request PortOutOfService
							String reason = UNUSED_BY_SYSTEM_UNLOAD_VERIFY_TIMEOUT;
//							stbCarrierLocManager.updatePortServiceState(carrierLocId, false);
							stbCarrierLocManager.updatePortServiceState(carrierLocId, false, reason);
						}
					}
					break;
				case READ:
				case READ2:
					Read r = null;
					carrierLocId = eventEntry.getCarrierlocId();
					Iterator it2 = eventEntry.getResponseMap().values().iterator();
					while (it2.hasNext()) {
						r = (Read) it2.next(); // 여기는 메시지가 하나이다.
					}
					if (r != null) {
						manageIdReadingResult(r);
					} else { // RFC Timeout (IdRead 에 대한 응답이 없음)
						// 2013.01.04 by KYK
						StringBuilder log = new StringBuilder("[RFC->STBC] IDREAD Timeout On Port:");
						log.append(carrierLocId).append(" RFC Not Respond");
//						traceSTBCMain("[RFC->STBC] IDREAD Timeout On Port:" + carrierLocId + " RFC Not Respond");
						traceSTBCMain(log.toString());
						traceSTBCAbnormal(log.toString());

						// carrierLoc 에 있는 carrierRemoved / ErrorSTBxx 로 carrierInstalled
						STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
						String dbCarrierId = carrierLoc.getCarrierId();
						//String errorCarrierId = stbc.makeErrorSTBCarrierName(dbCarrierId);
						String errorCarrierId = stbc.makeErrorSTBCarrierName();
						String errorFoupId = stbc.makeErrorSTBFoupName();
	
						if (dbCarrierId != null && dbCarrierId.length() > 0) {
							manageCarrierRemoved(carrierLocId, dbCarrierId);
						}					
						// 2013.01.04 by KYK
						String reason = ERRORSTB_RFC_IDREAD_TIMEOUT;
//						manageCarrierInstalled(carrierLocId, errorCarrierId);			
						manageCarrierInstalled(carrierLocId, errorCarrierId, errorFoupId, reason);
						
						// 누락보고 추가
						ReportItems rItems = makeSTBReportItems(carrierLocId, errorCarrierId, errorFoupId, FAILURE); 
						stbc.sendS6F11(CarrierIDRead, rItems);	

						String event = " Port:" + carrierLocId + ", CarrierId:" + errorCarrierId;
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.ERROR_STB,
								EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
								EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.IDREAD_TIMEOUT), false);						
					}				
					break;
				case READALL:
				case READALL2:
					HashMap<String, ArrayList> requestMap; // rfcId,portList
					HashSet<String> tempPortList = new HashSet<String>();					
					boolean isRequestCompleted = false;

					// 2012.03.13 by KYK
					String rfcId = eventEntry.getRfcId();
					traceSTBCMain(" Received READALL Response of RFC:" + rfcId);
					for (int cnt = 0; cnt < stbc.getReadAllRequestList().size(); cnt++) {
						requestMap = stbc.getReadAllRequestList().get(cnt);
						if (requestMap.containsKey(rfcId)) {
							if (requestMap.get(rfcId) != null) {
								tempPortList.addAll(requestMap.get(rfcId));
								requestMap.remove(rfcId);
								if (requestMap.isEmpty()) {
									stbc.getReadAllRequestList().remove(requestMap);
									isRequestCompleted = true;								
								}
							}
						}
					}
					// 수신된 ReadAll 메시지를 가져온다. 
					Iterator it3 = eventEntry.getResponseMap().values().iterator();		
					// 2013.08.22 by KYK
//					while (it3.hasNext()) {
//						// RfcId 를 갖는 모든 포트에 대한 ReadResult, 즉 여기는 복수개가 존재
//						ReadAll readAll = (ReadAll) it3.next();
//						
//						carrierLocId = readAll.getCarrierLocId();
//						if (tempPortList.contains(carrierLocId)) {
//							manageIdReadingAllResult(readAll);							
//						}
//					}
					int checkCount = 0;
					ReadAll readAll = null;
					while (it3.hasNext()) {
						// RfcId 를 갖는 모든 포트에 대한 ReadResult, 즉 여기는 복수개가 존재
						readAll = (ReadAll) it3.next();
						carrierLocId = readAll.getCarrierLocId();
						if (tempPortList.contains(carrierLocId)) {
							manageIdReadingAllResult(readAll);							
						}
						checkCount++;
					}
					if (checkCount == 0) {
						for (String portId: tempPortList) {
							// Not Respond
							StringBuilder log = new StringBuilder("[RFC->STBC] IDREAD Timeout On Port:");
							log.append(portId).append(" RFC Not Respond");
							traceSTBCMain(log.toString());
							traceSTBCAbnormal(log.toString());
							
							
							// carrierLoc 에 있는 carrierRemoved / ErrorSTBxx 로 carrierInstalled
							STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(portId);
							String dbCarrierId = carrierLoc.getCarrierId();
							String errorCarrierId = stbc.makeErrorSTBCarrierName();
							String errorFoupId = stbc.makeErrorSTBFoupName();
		
							if (dbCarrierId != null && dbCarrierId.length() > 0) {
								manageCarrierRemoved(portId, dbCarrierId);
							}					
							String reason = ERRORSTB_RFC_IDREAD_TIMEOUT;
							manageCarrierInstalled(portId, errorCarrierId, errorFoupId, reason);
							
							ReportItems rItems = makeSTBReportItems(portId, errorCarrierId, errorFoupId, FAILURE); 
							stbc.sendS6F11(CarrierIDRead, rItems);	

							String event = " Port:" + portId + ", CarrierId:" + errorCarrierId;
							registerEventHistory(new EventHistory(EVENTHISTORY_NAME.ERROR_STB,
									EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
									EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.IDREAD_TIMEOUT), false);						

						}
					}
					
					// IDREADLIST 요청완료에 대한 보고 [STBC->MCS]
					if (isRequestCompleted) {
						stbc.sendS6F11(CarrierIDReadMulti, null);		
						traceSTBCMain(" READALL's Completed. State:" + eventEntry.getState());
					}			

					break;
				case STATUS: 
					break;
				case UNKNOWN: 
					break;
				default :
			}
			index++;
		}
		for (int i = 0; i < index; i++) {
			completedEventList.remove(0);
		}			

		// 2012.04.06 by KYK : DelayTime Check
		long processingTime = System.currentTimeMillis() - checkTime;
		if (processingTime > ocsInfoManager.getStbcDelayLimit()) {
//			traceSTBCDelay(" [4].manageResultDataFromRFC(), processingTime(ms): " + processingTime);
			StringBuilder sb = new StringBuilder("[4].manageResultDataFromRFC(), itemCount: ");
			sb.append(index).append(" / processingTime(ms):").append(processingTime);
			traceSTBCDelay(sb.toString());
		}

	}

	/**
	 * IDREAD ALL Request 에 대해 수신된  RFC Data 를 처리한다.
	 * @param r
	 */
	private void manageIdReadingAllResult(ReadAll r) {

		// variables from RFC (r)
		int readResult = r.getReadResultByte();
		int carrierDetect = r.getCarrierDetectByte();
		String idData = r.getIdData();
		String carrierLocId = r.getCarrierLocId();
		// 2014.01.30 by KBS : FoupID 추가 (for A-PJT EDS)
		String foupId = "";
		if (r instanceof ReadAll2) {
			foupId = ((ReadAll2) r).getFoupIdData();
		}

		StringBuilder sb = new StringBuilder("[RFC->STBC] IDREADALL : ");
		sb.append("DATA CNT[").append(r.getDataCount()).append("], DATA MAX[").append(r.getDataMax());
		sb.append("], STBNo.[").append(r.getStbNumber()).append("], CarrierLoc[").append(carrierLocId);
		sb.append("], ReadResult[").append(readResult).append("]:").append(r.getReadResult());
		sb.append(" , CarrierDetect[").append(carrierDetect).append("]:").append("], IDData[").append(idData).append("], FoupIDData[").append(foupId).append("]");
		traceSTBCMain(sb.toString());
		
		// [RFC->STBC] IDREADALL : DATA CNT[3], DATA MAX[3], STBNo.[3], CarrierLoc[S1BR21_11], ReadResult[0]:OK , CarrierDetect[1], IDData[GY012]

		if (carrierLocId == null || carrierLocId.length() == 0) {
			traceSTBCMain("CarrierLocId does not exist in DB. (carrierLocId == null)");
			return;
		}
		// 2012.03.08 by KYK
		STBCarrierLoc carrierLoc = (STBCarrierLoc) stbCarrierLocManager.getData().get(carrierLocId);
		if (carrierLoc == null || carrierLoc.isEnabled() == false) {
			traceSTBCMain("Port is not Available[Enabled=false]. IDReadResult won't be reported.");
		}
		ReportItems rItems = verifyIdReadingResult(readResult, carrierDetect, idData, carrierLocId, foupId);		
	}

	/**
	 * IDREAD Request 에 대해 RFC로부터 수신된 결과를 처리한다.
	 * DB Data와 데이터 검증후 MCS에 결과 보고 (S6F11)
	 * @param r
	 */
	private void manageIdReadingResult(Read r) {

		// RFC 로부터 받은 데이터  ------------------------------------------
		// STBNo.
		// CarrierDetect : 0 [Not detect Carrier], 1 [Detect Carrier]
		// Read result : 0 [OK], 1 [NG(ID Read Error)], 2 [NotUse]
		// ID data : Id data which RFC hava (if no carrier, 'EMPTY' / if read error, 'ERROR')		
		// -------------------------------------------------------------

		// variables from RFC (r)
		int readResult = r.getReadResultByte();
		int carrierDetect = r.getCarrierDetectByte();
		String idData = r.getIdData();
		String carrierLocId = r.getCarrierLocId();
		// 2014.01.30 by KBS : FoupID 추가 (for A-PJT EDS)
		String foupId = "";
		if (r instanceof Read2) {
			foupId = ((Read2) r).getFoupIdData();
		}

		StringBuilder sb = new StringBuilder("[RFC->STBC] IDREAD : ");
		sb.append("STBNo.[").append(r.getStbNumber()).append("], CarrierLoc[").append(carrierLocId);
		sb.append("], ReadResult[").append(readResult).append("]:").append(r.getReadResult());
		sb.append(" , CarrierDetect[").append(carrierDetect).append("], IDData[").append(idData).append("], FoupIDData[").append(foupId).append("]");
		traceSTBCMain(sb.toString());
		
		// [RFC->STBC] IDREAD : STBNo.[3], CarrierLoc[S1BR21_11], ReadResult[1]:NG , CarrierDetect[0], IDData[]

		if (carrierLocId == null || carrierLocId.length() == 0) {
			traceSTBCMain("CarrierLocId does not exist in DB. (carrierLocId == null)");
			return;
		}
		// ■ Verify IdReadResult 
		ReportItems reportItems = verifyIdReadingResult(readResult, carrierDetect, idData, carrierLocId, foupId);
		// ■ Report (S6F11) IdReadResult ToMCS 		
//		reportIdReadingResult(reportItems, carrierLocId);
		stbc.sendS6F11(CarrierIDRead, reportItems);
	}

	/**
	 * IDREAD Request 에 대해 RFC로부터 수신된 결과를 처리한다.
	 * @param readResult
	 * @param carrierDetect
	 * @param idData
	 * @param carrierLocId
	 * @param foupId
	 * @return
	 */
	private ReportItems verifyIdReadingResult(int readResult, int carrierDetect, String idData, String carrierLocId, String foupId) {
		ReportItems rItems = null;		
		STBCarrierLoc carrierLocOnPort = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
		STBCarrierLoc dbCarrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(idData);
		String carrierIdOnPort = carrierLocOnPort.getCarrierId();
		String unknownCarrierId = stbc.makeUnknownCarrierName(idData);
		String unknownFoupId = stbc.makeUnknownFoupName(foupId);
		
		StringBuilder log = new StringBuilder(); // 2013.01.04 by KYK
		/* ---------------------------------------------------------------
		 * Carrier IDREAD Fail
		 * Carrier IDREAD : Carrier mismatch
		 * Carrier IDREAD : Carrier mismatch & Duplicate
		 * Carrier IDREAD : Carrier Duplicate
		 * ---------------------------------------------------------------
		 */
		if (readResult == OK) {
			if (carrierDetect == DETECT_CARRIER) {
				// Q1 : carrierId data 가 DB 에 존재하는가? : // YES, 정상
				if (dbCarrierLoc != null) { 
					// Q2 : carrier 위치는 바로 여기가 맞나?
					if (carrierLocId.equals(dbCarrierLoc.getCarrierLocId())) { // [Normal, OK]
						rItems = makeSTBReportItems(carrierLocId, idData, foupId, SUCCESS);
					} else { // [Duplicate] carrier 다른 곳에 있음
						String errorSTBDupCarrierId;
						String errorSTBDupFoupId;
						// Q3 : 현재 carrierLocId 위치에 다른 carrier 가 존재하는가?
						if (carrierIdOnPort == null || "".equals(carrierIdOnPort) ) {
							// [Carrier Not Found & Duplicate]
							// 2013.01.04 by KYK
							log.append("[RFC-IDREAD] Carrier Not Found & Duplicate :");
							log.append(idData).append("/").append(carrierLocId);
							log.append(", ErrorSTBDUP/").append(dbCarrierLoc.getCarrierLocId());
							traceSTBCMain(log.toString());

							manageCarrierRemoved(dbCarrierLoc.getCarrierLocId(), idData);
							//							String errorSTBDupCarrierId = stbc.makeErrorSTBDUPCarrierName(idData);
							errorSTBDupCarrierId = stbc.makeErrorSTBDUPCarrierName();
							errorSTBDupFoupId = stbc.makeErrorSTBDUPFoupName();
							// 2013.01.04 by KYK
//							manageCarrierInstalled(dbCarrierLoc.getCarrierLocId(), errorSTBDupCarrierId);
							String reason = ERRORSTBDUP_CARRIER_DUPLICATE_BY_IDREAD;
							manageCarrierInstalled(dbCarrierLoc.getCarrierLocId(), errorSTBDupCarrierId, errorSTBDupFoupId, reason);

							manageCarrierInstalled(carrierLocId, idData, foupId);
							rItems = makeSTBReportItems(carrierLocId, idData, foupId, DUPLICATE);

						} else { // [Mismatch & Duplicate]
							// 2013.01.04 by KYK
							log.append("[RFC-IDREAD] Carrier Mismatch & Duplicate :");
							log.append(carrierIdOnPort).append("->").append(idData).append("/").append(carrierLocId);
							log.append(", ErrorSTBDUP/").append(dbCarrierLoc.getCarrierLocId());
							traceSTBCMain(log.toString());

							manageCarrierRemoved(dbCarrierLoc.getCarrierLocId(), idData);
							// String errorSTBDupCarrierId = stbc.makeErrorSTBDUPCarrierName(idData);
							errorSTBDupCarrierId = stbc.makeErrorSTBDUPCarrierName();
							errorSTBDupFoupId = stbc.makeErrorSTBDUPFoupName();
							// 2013.01.04 by KYK
//							manageCarrierInstalled(dbCarrierLoc.getCarrierLocId(), errorSTBDupCarrierId);
							String reason = ERRORSTBDUP_CARRIER_DUPLICATE_BY_IDREAD;
							manageCarrierInstalled(dbCarrierLoc.getCarrierLocId(), errorSTBDupCarrierId, errorSTBDupFoupId, reason);

							manageCarrierRemoved(carrierLocId, carrierIdOnPort);
							manageCarrierInstalled(carrierLocId, idData, foupId);	
							rItems = makeSTBReportItems(carrierLocId, idData, foupId, DUPLICATE);
						}									
						// 2013.01.04 by KYK : eventHistory
						String event = " Port:" + carrierLocId + ", CarrierId:" + errorSTBDupCarrierId;
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.ERROR_STB_DUP,
								EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
								EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.CARRIER_DUPLICATE_BY_IDREAD), false);
						
						// 2012.09.10 by KYK : carrier mismatch 에 대하여 확인을 위해 STBCarrierLoc 에 mismatch count 를 기록
						// 배경 : 설비이동시 노드이동 이때 STB는 아직 이동 안한상태에서 한시적으로 사용하다가 Rfc mapping Index 가 꼬이는 현상발생, 대응용
						registerMismatchCount(carrierLocId, carrierLocOnPort.getMismatchCount() + 1);
					}					
				} else { // Q1 : carrierId data 가 DB 에 존재하는가? : NO, mismatch
					log.append("[RFC-IDREAD] Carrier Mismatch :");
					log.append(carrierIdOnPort).append("->").append(idData).append("/").append(carrierLocId);					
					traceSTBCMain(log.toString());

					// Q3 : 현재 carrierLocId 위치에 다른 carrier 가 존재하는가?
					if (carrierIdOnPort == null || "".equals(carrierIdOnPort)) {
						// [Mismatch]
						manageCarrierInstalled(carrierLocId, idData, foupId);	
						rItems = makeSTBReportItems(carrierLocId, idData, foupId, MISMATCH);

					} else {
						// [Mismatch]
						manageCarrierRemoved(carrierLocId, carrierIdOnPort);
						manageCarrierInstalled(carrierLocId, idData, foupId);	
						rItems = makeSTBReportItems(carrierLocId, idData, foupId, MISMATCH);
					}		
					
					// 2012.09.10 by KYK : carrier mismatch 에 대하여 확인을 위해 STBCarrierLoc 에 mismatch count 를 기록
					// 배경 : 설비이동시 노드이동 이때 STB는 아직 이동 안한상태에서 한시적으로 사용하다가 Rfc mapping Index 가 꼬이는 현상발생, 대응용
					registerMismatchCount(carrierLocId, carrierLocOnPort.getMismatchCount() + 1);
				}
			} else if (carrierDetect == NOT_DETECT_CARRIER) {
				// Q3 : 현재 carrierLocId 위치에 DB상  carrier 가 존재하는가?
				if (carrierIdOnPort == null || "".equals(carrierIdOnPort)) {
					rItems = makeSTBReportItems(carrierLocId, "", "", NO_LOAD);
				} else {
					manageCarrierRemoved(carrierLocId, carrierIdOnPort);
					rItems = makeSTBReportItems(carrierLocId, "", "", MISMATCH); // "" 이게 맞나 ? 아니면 idData 가 맞을까?

					log.append("[RFC-IDREAD] Carrier Mismatch :");
					log.append(carrierIdOnPort).append("->").append(idData).append("/").append(carrierLocId);					
					traceSTBCMain(log.toString());
					// 2012.09.10 by KYK : carrier mismatch 에 대하여 확인을 위해 STBCarrierLoc 에 mismatch count 를 기록
					// 배경 : 설비이동시 노드이동 이때 STB는 아직 이동 안한상태에서 한시적으로 사용하다가 Rfc mapping Index 가 꼬이는 현상발생, 대응용
					registerMismatchCount(carrierLocId, carrierLocOnPort.getMismatchCount() + 1);
				}				
			}			
		} else if (readResult == IDREAD_ERROR || readResult == IDREAD_NULL_ERROR) {  
			// ?? ID Read Fail 일 경우, carrierDetect 는 확인할 수 있나?
			// 2015.02.05 by KBS : Read Result 구분 (ID Read Error / ID Read NULL Error)
			log.append("[RFC-IDREAD] IDREAD Fail : Read result(").append(readResult).append("), ");
			log.append(carrierIdOnPort).append("/").append(carrierLocId);					
			traceSTBCMain(log.toString());
			
			if (carrierDetect == DETECT_CARRIER) {
				unknownCarrierId = stbc.makeRFCUnknownCarrierName();
				unknownFoupId = stbc.makeRFCUnknownFoupName();
				if (carrierIdOnPort == null || "".equals(carrierIdOnPort)) {					
					manageCarrierInstalled(carrierLocId, unknownCarrierId, unknownFoupId);	
					// 2012.03.12 by KYK [IDREAD_ERROR] CarrierIDRead 보고시 UNKNOWNSTBC ID로 보고
//					rItems = makeSTBReportItems(carrierLocId, carrierIdOnPort, FAILURE);
					rItems = makeSTBReportItems(carrierLocId, unknownCarrierId, unknownFoupId, FAILURE);
				} else {
					manageCarrierRemoved(carrierLocId, carrierIdOnPort);					
					manageCarrierInstalled(carrierLocId, unknownCarrierId, unknownFoupId);	
					// 2012.03.12 by KYK [IDREAD_ERROR] CarrierIDRead 보고시 UNKNOWNSTBC ID로 보고
//					rItems = makeSTBReportItems(carrierLocId, carrierIdOnPort, FAILURE);
					rItems = makeSTBReportItems(carrierLocId, unknownCarrierId, unknownFoupId, FAILURE);
				}

			} else if (carrierDetect == NOT_DETECT_CARRIER) {
				if (carrierIdOnPort == null || "".equals(carrierIdOnPort)) {
					rItems = makeSTBReportItems(carrierLocId, "", "", FAILURE);
				} else {
					manageCarrierRemoved(carrierLocId, carrierIdOnPort);
					rItems = makeSTBReportItems(carrierLocId, "", "", FAILURE);
				}
			}			
		} else { // NOT_USE
			// 2013.10.11 by KYK
			log.append("[RFC->STBC] IDREAD Fail On Port:");
			log.append(carrierLocId).append(" (RFC has no Data.)");
			traceSTBCMain(log.toString());

			STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
			String dbCarrierId = carrierLoc.getCarrierId();
			String errorCarrierId = stbc.makeErrorSTBCarrierName();
			String errorFoupId = stbc.makeErrorSTBFoupName();

			if (dbCarrierId != null && dbCarrierId.length() > 0) {
				manageCarrierRemoved(carrierLocId, dbCarrierId);
			}					
			String reason = ERRORSTB_IDREAD_FAIL;
			manageCarrierInstalled(carrierLocId, errorCarrierId, errorFoupId, reason);
			
			rItems = makeSTBReportItems(carrierLocId, errorCarrierId, errorFoupId, FAILURE); 

			String event = " Port:" + carrierLocId + ", CarrierId:" + errorCarrierId;
			registerEventHistory(new EventHistory(EVENTHISTORY_NAME.ERROR_STB,
					EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
					EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.RFC_ABNORMAL), false);						
		}		

		// 2013.01.04 by KYK : Abnormal Log
		if (log != null && log.length() > 0) {
			traceSTBCAbnormal(log.toString());
		}

		return rItems;
	}


	/**
	 * INSTALL/REMOVE 등의 이벤트 발생에 따른 검증요청(VERIFY)에 대한 RFC 결과 데이터를 처리한다.
	 * @param v
	 */
	private void manageVerifyingResult(Verify v) { 
		// RFC 로부터 받는 데이터 -------------------------------------------------------------
		// STBNo.
		// CarrierDetect : 0 [Not detect Carrier] 1 [Detect Carrier]
		// Verify result : 0 [OK(Match)] 1 [NG(Carrier Detect Mismatch)] 9 [NG(STB Disable)] 3 [NotUse]
		// Read result : 0 [OK] 1 [NG(ID Read Error)] 2 [NotUse]
		// ID data : Id data which RFC has (if no carrier, 'EMPTY' / if read error, 'ERROR')		
		// -------------------------------------------------------------------------------

		int verifyResult = v.getVerifyResultByte();
		int readResult = v.getReadResultByte();
		int carrierDetect = v.getCarrierDetectByte();
		String idData = v.getIdData();		
		String carrierLocId = v.getCarrierLocId();
		// 2014.01.30 by KBS : FoupID 추가 (for A-PJT EDS)
		String foupId = "";		
		if (v instanceof Verify2) {
			foupId = ((Verify2) v).getFoupIdData();
		}
		
		StringBuilder log = new StringBuilder(); // 2013.01.04 by KYK
		
		StringBuilder sb = new StringBuilder("[RFC->STBC] VERIFY : ");
		sb.append("STBNo.[").append(v.getStbNumber()).append("], CarrierLoc[").append(carrierLocId);
		sb.append("], VerifyResult[").append(verifyResult).append("]:").append(v.getVerifyResult());
		sb.append(" , ReadResult[").append(readResult).append("]:").append(v.getReadResult());
		sb.append(" , CarrierDetect[").append(carrierDetect).append("], IDData[").append(idData).append("], FoupIDData[").append(foupId).append("]");
		traceSTBCMain(sb.toString());
		
		// [RFC->STBC] VERIFY : STBNo.[2], CarrierLoc[S1BR21_22], VerifyResult[0]:OK , ReadResult[0]:OK , CarrierDetect[1], IDData[GYB01]	
		//displayVerifyInfo(v);		

		if (carrierLocId == null || carrierLocId.length() == 0) {
			traceSTBCMain("CarrierLocId does not exist in DB. (carrierLocId == null)");
			return;
		}

		STBCarrierLoc carrierLocOnPort = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
		STBCarrierLoc dupCarrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(idData);

		ReportItems rItems;
		String unknownCarrierId;		
		String unknownFoupId;		
		String carrierIdInDB = carrierLocOnPort.getCarrierId();

		if (verifyResult == OK) {
			if (carrierDetect == NOT_DETECT_CARRIER) {
				rItems = makeSTBReportItems(carrierLocId, "", "", NO_LOAD);
				stbc.sendS6F11(CarrierIDRead, rItems);					
			} else if (carrierDetect == DETECT_CARRIER) {				
				if (readResult == OK) {
					//
					if (idData.equals(carrierIdInDB)) {
						// normal
						rItems = makeSTBReportItems(carrierLocId, idData, foupId, SUCCESS);
						stbc.sendS6F11(CarrierIDRead, rItems);							
					} else {			
						if (dupCarrierLoc == null) {
							// carrier mismatch
							log.append(" Carrier Mismatch : ");
							log.append(carrierIdInDB).append("->").append(idData).append("/").append(carrierLocId);
							traceSTBCMain(log.toString());
							
							manageCarrierRemoved(carrierLocId, carrierIdInDB);
							manageCarrierInstalled(carrierLocId, idData, foupId);
							rItems = makeSTBReportItems(carrierLocId, idData, foupId, MISMATCH);
							stbc.sendS6F11(CarrierIDRead, rItems);	
						} else {
							// carrier mismatch & duplicate
							// 2013.01.04 by KYK
							log.append(" Carrier Mismatch & Duplicate : ");
							log.append(idData).append("/").append(carrierLocId).append(", ErrorSTBDUP/").append(dupCarrierLoc.getCarrierLocId());
							traceSTBCMain(log.toString());

							manageCarrierRemoved(dupCarrierLoc.getCarrierLocId(), dupCarrierLoc.getCarrierId());
							//							String errorSTBDupCarrierId = stbc.makeErrorSTBDUPCarrierName(dupCarrierLoc.getCarrierId());
							String errorSTBDupCarrierId = stbc.makeErrorSTBDUPCarrierName();
							String errorSTBDupFoupId = stbc.makeErrorSTBDUPFoupName();
							// 2013.01.04 by KYK
//							manageCarrierInstalled(dupCarrierLoc.getCarrierLocId(), errorSTBDupCarrierId);
							String reason = ERRORSTBDUP_CARRIER_DUPLICATE_BY_VERIFY;
							manageCarrierInstalled(dupCarrierLoc.getCarrierLocId(), errorSTBDupCarrierId, errorSTBDupFoupId, reason);
							manageCarrierRemoved(carrierLocId, carrierIdInDB);
							manageCarrierInstalled(carrierLocId, idData, foupId);

							rItems = makeSTBReportItems(carrierLocId, idData, foupId, DUPLICATE); 
							stbc.sendS6F11(CarrierIDRead, rItems);																

							// 2013.01.04 by KYK : eventHistory
							String event = " Port:" + carrierLocId + ", CarrierId:" + errorSTBDupCarrierId;
							registerEventHistory(new EventHistory(EVENTHISTORY_NAME.ERROR_STB_DUP,
									EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
									EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.CARRIER_DUPLICATE_BY_VERIFY), false);
						}							
						
						// 2012.09.10 by KYK : carrier mismatch 에 대하여 확인을 위해 STBCarrierLoc 에 mismatch count 를 기록
						// 배경 : 설비이동시 노드이동 이때 STB는 아직 이동 안한상태에서 한시적으로 사용하다가 Rfc mapping Index 가 꼬이는 현상발생, 대응용
						registerMismatchCount(carrierLocId, carrierLocOnPort.getMismatchCount() + 1);
					}												
				} else {
					// 2013.10.11 by KYK : IDREAD OK 아니면 NG 로 처리
					// 2015.02.05 by KBS : Read Result 구분 로그(ID Read Error / ID Read NULL Error)
					log.append(" Read result(").append(readResult).append("), ");
					log.append(carrierIdInDB).append("->").append(idData).append("/").append(carrierLocId);
					traceSTBCMain(log.toString());
					
					// Read Fail
					manageCarrierRemoved(carrierLocId, carrierIdInDB);
					unknownCarrierId = stbc.makeRFCUnknownCarrierName();
					unknownFoupId = stbc.makeRFCUnknownFoupName();
					manageCarrierInstalled(carrierLocId, unknownCarrierId, unknownFoupId);

					// 2012.03.12 by KYK [IDREAD_ERROR] CarrierIDRead 보고시 UNKNOWNSTBC ID로 보고
					rItems = makeSTBReportItems(carrierLocId, unknownCarrierId, unknownFoupId, FAILURE); 
					stbc.sendS6F11(CarrierIDRead, rItems);	
				}
			} else {				
				; /*NULL*/
			} // invalid data				
		} else if (verifyResult == CARRIER_DETECT_MISMATCH) {
			// 2013.01.04 by KYK
			String errorCarrierId = null;
			String errorFoupId = null;
			// 2012.03.15 by KYK : VERIFYRESULT 가 NG 인 경우, readResult 관계없이 errorSTB로 처리함			
			if (carrierDetect == NOT_DETECT_CARRIER) {
				// 2013.01.04 by KYK
				log.append(" VerifyResult:NG, A Carrier's Loaded On Port:");
				log.append(carrierLocId).append(", but RFC(Sensor) Not Detect.");
				traceSTBCMain(log.toString());
				
				// 2012.03.15 by KYK : [Sensor Error Case] 내려놨는데 감지안되는 케이스				
				manageCarrierRemoved(carrierLocId, carrierIdInDB);
				errorCarrierId = stbc.makeErrorSTBCarrierName();
				errorFoupId = stbc.makeErrorSTBFoupName();
				// 2013.01.04 by KYK
//				manageCarrierInstalled(carrierLocId, errorCarrierId);			
				String reason = ERRORSTB_LOAD_VERIFY_NG;
				manageCarrierInstalled(carrierLocId, errorCarrierId, errorFoupId, reason);			
				rItems = makeSTBReportItems(carrierLocId, errorCarrierId, errorFoupId, FAILURE);
				stbc.sendS6F11(CarrierIDRead, rItems);
				
			} else if (carrierDetect == DETECT_CARRIER) {
				// 2013.01.04 by KYK
				log.append(" VerifyResult:NG, A Carrier's Unloaded On Port:");
				log.append(carrierLocId).append(", but RFC(Sensor) Abnormal Detect.");
				traceSTBCMain(log.toString());
				
				// 2012.03.15 by KYK : [Sensor Error Case] 떴는데 감지되는 케이스 (센서눌려있는 케이스)				
				errorCarrierId = stbc.makeErrorSTBCarrierName();
				errorFoupId = stbc.makeErrorSTBFoupName();
				// 2013.01.04 by KYK
//				manageCarrierInstalled(carrierLocId, errorCarrierId);			
				String reason = ERRORSTB_UNLOAD_VERIFY_NG;
				manageCarrierInstalled(carrierLocId, errorCarrierId, errorFoupId, reason);			
				rItems = makeSTBReportItems(carrierLocId, errorCarrierId, errorFoupId, FAILURE);
				stbc.sendS6F11(CarrierIDRead, rItems);
				
			} else {
				// invalid data
				; /*NULL*/
			}	
			// 2013.01.04 by KYK
			String event = " Port:" + carrierLocId + ", CarrierId:" + errorCarrierId;
			registerEventHistory(new EventHistory(EVENTHISTORY_NAME.ERROR_STB,
					EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
					EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.UNLOAD_VERIFY_NG), false);
			
		} else if (verifyResult == DISABLED_STB) {
			; /*NULL*/
		} else {
			; /*NULL*/
		}
		
		// 2013.01.04 by KYK : Abnormal Log
		if (log != null && log.length() > 0) {
			traceSTBCAbnormal(sb.toString());
			traceSTBCAbnormal(log.toString());
		}		
	}
	

	/**
	 * 2012.09.10 by KYK
	 * 내용 : carrier mismatch 에 대하여 확인을 위해 STBCarrierLoc 에 mismatch count 를 기록
	 * 배경 : 설비이동시 노드이동 이때 STB는 아직 이동 안한상태에서 한시적으로 사용하다가 Rfc mapping Index 가 꼬이는 현상발생, 대응용
	 * @param carrierLocId
	 * @param mismatchCount
	 */
	private void registerMismatchCount(String carrierLocId, int mismatchCount) {
		stbCarrierLocManager.updateAbnormalCarrierLocStatus(carrierLocId, mismatchCount);
	}

//	/**
//	 * Display Verify Info
//	 * 
//	 * @param v
//	 */
//	private void displayVerifyInfo(Verify v) {
//		traceSTBCMain("VERIFY COMPLETED");
//		traceSTBCMain(" =========================================");
//		traceSTBCMain(" - STBNO : [" + v.getStbNumber()+"]");
//		traceSTBCMain(" - CARRIERDETECT : [" + v.getCarrierDetectByte()+"]");
//		traceSTBCMain(" - VERIFY RESULT : [" + v.getVerifyResult()+"]"+(int)v.getVerifyResultByte());
//		traceSTBCMain(" - READ RESULT : [" + v.getReadResult()+"]" + (int)v.getReadResultByte());
//		traceSTBCMain(" - IDDATA : [" + v.getIdData()+"]");
//		traceSTBCMain(" =========================================");		
//	}

	/**
	 * 2012.03.08 by KYK 
	 * RFC 상태가 error 로 올라오면 해당 RFC 에 Port OutOfService
	 * 만약 Port 에 carrier 존재할 경우, carrierRemoved 후, Port OutOfService
	 */
	private void manageRFCErrorOcurred() {
		// INSERVICE && TSC_AUTO && RFCUSAGE='YES' (아니면 return)
		// 2012.04.05 by KYK : RFCUSAGE Setup Parameter 로 변경
//		if (isInWorkingCondition() == false || rfcManager == null || ocsInfoManager.isRFCUsed() == false) {
		if (isInWorkingCondition() == false || rfcManager == null || isRFCUsed == false) {
			return;
		}		
//		// 2012.03.08 by KYK : 기능사용여부 체크 
//		if (ocsInfoManager.isRFCErrorControlUsed() == false) {
//			return;
//		}
		
		// 주기적 Error RFC 체크 (STBRfcDataManager) : errorRfcIdSet 가져옴   		
		if (stbRfcDataManager.getErrorRfcIdSet().size() > 0) {			
			HashSet<String> errorRfcList = new HashSet<String>(stbRfcDataManager.getErrorRfcIdSet());
			STBCarrierLoc carrierLoc;
			String carrierLocId;
			String carrierId;
			for (String rfcId: errorRfcList) {
				// 2015.03.19 by KYK : RFC 비정상 무조건 Disable, but 포트 비사용은 옵션 / 파라미터 이름 변경 TODO 
//				if (ocsInfoManager.isRFCErrorControlUsed()) {
				if (ocsInfoManager.isRFCErrorPortOutOfServiceUsed()) {
					// Error RFC 내의  PortList 가져옴 (STBCarrierLocManager)
					ArrayList<STBCarrierLoc> portList = stbCarrierLocManager.getPortListFromDBWhereRfcId(rfcId);
					for (Iterator it = portList.iterator(); it.hasNext();) {
						carrierLoc = (STBCarrierLoc) it.next();
						carrierId = carrierLoc.getCarrierId();
						carrierLocId = carrierLoc.getCarrierLocId();
						if (carrierId == null || carrierId.length() == 0) {
							// 포트에 carrier 없음 : PortOutOfService
							// 2013.01.04 by KYK : 포트비사용 이유기록 
							String reason = UNUSED_BY_SYSTEM_RFC_ABNORMAL_STATE;
							stbCarrierLocManager.updatePortServiceState(carrierLocId, false, reason);
							
							String event = " Port:" + carrierLocId;
							traceSTBCAbnormal(reason + event);
							registerEventHistory(new EventHistory(EVENTHISTORY_NAME.UNUSED_STBPORT,
									EVENTHISTORY_TYPE.SYSTEM, "", event, "", "",	
									EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.RFC_ABNORMAL), false);						
						}
						// 2012.03.15 by KYK : 정상 carrier 는 그대로 둠
//					} else {
//						// 포트에 carrier 있음 : Wait Until carrierRemoved by OHT
//						if (carrierLoc.isEnabled() && isAbnormalCarrierId(carrierId) == false) {
//							// 그러기 위해 carrierId 를 'ErrorSTBxx' 로 변경함
//							manageCarrierRemoved(carrierLocId, carrierId);
//							manageCarrierInstalled(carrierLocId, stbc.makeErrorSTBCarrierName());
//						}
//					}
					}
				}
				// Port state 보고 후, RFC Disabled 처리 [Enabled=False]
				stbRfcDataManager.updateRfcState(rfcId, false);		
				String log = "	RFC is in Abnormal State. RFC's Updated [Enabled='false'] with RfcId=" + rfcId;
				traceSTBCMain(log);
				traceSTBCAbnormal(log);
				// 2014.12.19 by KYK : alarm 추가
				registerAlarmTextWithLevel("[RFC ERROR] UNUSED BY SYSTEM :" + rfcId, ALARMLEVEL.ERROR);

				// 2013.01.04 by KYK
				registerEventHistory(new EventHistory(EVENTHISTORY_NAME.UNUSED_RFC,
						EVENTHISTORY_TYPE.SYSTEM, "", "RfcId:" + rfcId, "", "",	
						EVENTHISTORY_REMOTEID.STBC, "", EVENTHISTORY_REASON.RFC_ABNORMAL), false);
				
			}
		}
	}

	/**
	 * 2013.01.04 by KYK
	 * @param eventHistory
	 * @param duplicateCheck
	 */
	private void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
		eventHistoryManager.addEventHistoryToRegisterList(eventHistory, duplicateCheck);		
	}

//	/**
//	 * 2012.04.05 by KYK : [Log4j] 로 기록
//	 * carrier 상태가 변경될 때 Runtime stbData 를 로그로 남김
//	 * 매번 최신데이터로 파일 갱신함 (Overwrite, Not append)
//	 */
//	public void saveRuntimeSTBDataToLog() {
//		if (ocsInfoManager.isSTBDataSaveUsed()) {
//			long checkTime = Math.abs(System.currentTimeMillis());
//			Iterator it = stbCarrierLocManager.getData().values().iterator();
//			STBCarrierLoc carrierLoc;
//			while (it.hasNext()) {
//				carrierLoc = (STBCarrierLoc) it.next();
//				StringBuilder sb = new StringBuilder("UPDATE STBCARRIERLOC SET CARRIERID='");
//				sb.append(carrierLoc.getCarrierId());
//				sb.append("', COMMANDNAME='', MCSCARRIERID='' WHERE CARRIERLOCID='");
//				sb.append(carrierLoc.getCarrierLocId()).append("';");					
//
//				traceSTBRuntimeData(sb.toString());
//			}
//			long processingTime = System.currentTimeMillis() - checkTime;
//			traceSTBRuntimeData(" -- SaveTime : " + processingTime + " ms");
//			// 2012.04.06 by KYK : DelayTime Check
//			if (processingTime > ocsInfoManager.getStbcDelayLimit()) {
//				traceSTBCDelay("  saveRuntimeSTBDataToLog(), processingTime(ms): " + processingTime);
//			}
//		}
//	}
	
	/**
	 * 2012.04.05 by KYK : [Log4j] 로 기록
	 * OCS파라미터로 설정된 interval 마다 현재 carrier 상태를 로그로 남김 (기본설정 : 5분)
	 */
	public void saveSTBDataToLog() {
		if (isInWorkingCondition() == false) {
			return;
		}		
		if (ocsInfoManager.isSTBDataSaveUsed()) {
			long checkTime = Math.abs(System.currentTimeMillis());
			if (checkTime - stbDataSaveTime >= stbDataSavePeriod * 1000) {				
				Iterator it = stbCarrierLocManager.getData().values().iterator();
				STBCarrierLoc carrierLoc;
				while (it.hasNext()) {
					carrierLoc = (STBCarrierLoc) it.next();
					StringBuilder sb = new StringBuilder("UPDATE STBCARRIERLOC SET CARRIERID='");
					sb.append(carrierLoc.getCarrierId());
					sb.append("', COMMANDNAME='', MCSCARRIERID='' WHERE CARRIERLOCID='");
					sb.append(carrierLoc.getCarrierLocId()).append("';");					

					traceSTBData(sb.toString());
				}
				long processingTime = System.currentTimeMillis() - checkTime;
//				traceSTBData(" -- SaveTime : " + processingTime + " ms");
				stbDataSaveTime = System.currentTimeMillis();
				// 2012.04.06 by KYK : DelayTime Check
				if (processingTime > ocsInfoManager.getStbcDelayLimit()) {
					traceSTBCDelay("[6].saveSTBDataToLog(), processingTime(ms): " + processingTime);
				}
			}
		}
	}

//	//  2012.04.05 by KYK 
//	//	STBData 로그기록을 Log4j로 변경함 (FileWrite 미사용)
//	/**
//	 * STBData (DB insert문 형태) 를 File로 주기적으로 저장 
//	 * 저장주기 : OCSINFO - STBDATA_SAVE_PERIOD 에서 가져옴 
//	 */
//	public void saveSTBDataToFile() {		
//		if (isInWorkingCondition() == false) {
//			return;
//		}		
//		if (ocsInfoManager.isSTBDataSaveUsed()) {
//			//int period = getStbDataSavePeriod();			
//			long checkTime = Math.abs(System.currentTimeMillis());
//			if (checkTime - stbDataSaveTime >= stbDataSavePeriod * 1000) {				
//				String filename = "STBData";
//				String stbDataFileName = makeFileRouteName(filename);		
//				saveSTBDataToFile(stbDataFileName);
//			}
//		}		
//	}
//
//	/**
//	 * Runtime 중 변경된 STBData를 실시간 업데이트 함 
//	 * File : RuntimeSTBData
//	 */
//	public void saveRuntimeSTBDataToFile(){
//		if (ocsInfoManager.isSTBDataSaveUsed()) {
//			if (stbBackupFileName == null || stbBackupFileName.length() == 0) {
//				// make BackupFileName
//				String filename = "RuntimeSTBData";						
//				stbBackupFileName = makeFileRouteName(filename);
//			}
//			saveSTBDataToFile(stbBackupFileName);			
//		}		
//	}
//
//	/**
//	 * STBData(DB insert문 형태)를 File로  저장 
//	 * @param stbDataFileName
//	 */
//	private void saveSTBDataToFile(String stbDataFileName) {
//		long checkTime = Math.abs(System.currentTimeMillis());
//		BufferedWriter out = null;			
//		try {
//			out = new BufferedWriter(new FileWriter(stbDataFileName, false));
//
//			Iterator it = stbCarrierLocManager.getData().values().iterator();
//			STBCarrierLoc carrierLoc;
//			while (it.hasNext()) {
//				carrierLoc = (STBCarrierLoc) it.next();
//				StringBuilder sb = new StringBuilder("UPDATE STBCARRIERLOC SET CARRIERID='");
//				sb.append(carrierLoc.getCarrierId());
//				sb.append("', COMMANDNAME='', MCSCARRIERID='' WHERE CARRIERLOCID='");
//				sb.append(carrierLoc.getCarrierLocId()).append("';");					
//
//				out.write(sb.toString());
//				out.newLine();
//			}
//			out.write(" -- SaveTime : " + (System.currentTimeMillis() - checkTime/1000.));
//			//System.out.println("STB SaveTime : " + (System.currentTimeMillis() - checkTime/1000.));
//			out.newLine();
//			out.flush();
//
//			stbDataSaveTime = System.currentTimeMillis();
//		} catch (IOException e) {
//			e.printStackTrace();
//			traceException("DBException, SaveSTBDataToFile() - SQLException ", e);
//		} finally {
//			try	{
//				if (out != null) out.close();
//			} catch (Exception e){}
//		}					
//	}
//
//	/**
//	 * File 생성시 Directory 경로 및 파일이름을 만들어주는 메소드
//	 * @param filename
//	 * @return
//	 */
//	public String makeFileRouteName(String filename) {
//		String path = System.getProperty("user.dir");
//		String separator = System.getProperty("file.separator");
//
//		SimpleDateFormat dateForm = new SimpleDateFormat("yyyyMMddHHmmss");
//		String dateString = dateForm.format(new Date());
//
//		StringBuilder sb = new StringBuilder(path);
//		sb.append(separator); sb.append("log"); sb.append(separator);
//		sb.append(filename); sb.append(dateString); sb.append(".log");
//
//		return sb.toString();
//	}

	/**
	 * STBDATA_SAVE_PERIOD 를 설정하는 메소드 (OCSINFO DB)
	 * @return
	 */
	private int getStbDataSavePeriodFromDB() {
		String period = ocsInfoManager.getOCSInfoValue("STBDATA_SAVE_PERIOD");
		if (period != null && period.length() > 0) {
			stbDataSavePeriod = Integer.parseInt(period);
		}
		return stbDataSavePeriod;
	}

	public void changeServiceState(MODULE_STATE state) {
		this.requestedServiceState = state;
		this.serviceState = state;		
	}

	public void changeRequestedServiceState(MODULE_STATE reqinservice) {
		this.requestedServiceState = reqinservice;
	}

	public MODULE_STATE getServiceState() {
		return this.serviceState;
	}

	private static final String STBC_ABNORMAL = "STBCAbnormal";
	private static Logger stbcAbnormalLog = Logger.getLogger(STBC_ABNORMAL);
	public void traceSTBCAbnormal(String message) {
		stbcAbnormalLog.debug(String.format("%s", message));
	}
	
	private static final String STBC_MAIN = "STBCMain";
	private static Logger stbcMainLog = Logger.getLogger(STBC_MAIN);
	public void traceSTBCMain(String message) {
		stbcMainLog.debug(String.format("%s", message));
	}

	private static final String STBC_EXCEPTION = "STBCException";
	private static Logger stbcExceptionLog = Logger.getLogger(STBC_EXCEPTION);
	public void traceException(String message) {
		stbcExceptionLog.error(String.format("%s", message));
	}

	public void traceException(String message, Throwable t) {
		stbcExceptionLog.error(String.format("%s", message), t);
	}
	
	// 2011.11.14 by PMM
	private static final String RUNTIMEUPDATE = "RuntimeUpdateHistory";
	private static Logger runtimeUpdateHistoryLog = Logger.getLogger(RUNTIMEUPDATE);
	public void traceRuntimeUpdate(String message) {
		runtimeUpdateHistoryLog.info(String.format("%s", message));
	}
	
//	// 2012.04.05 by KYK
//	private static final String STBRUNTIMEDATA = "STBRuntimeData";
//	private static Logger stbRuntimeDataLog = Logger.getLogger(STBRUNTIMEDATA);
//	public void traceSTBRuntimeData(String message) {
//		stbRuntimeDataLog.info(String.format("%s", message));
//	}
	// 2012.04.05 by KYK
	private static final String STBDATA = "STBData";
	private static Logger stbDataLog = Logger.getLogger(STBDATA);
	public void traceSTBData(String message) {
		stbDataLog.info(String.format("%s", message));
	}
	// 2012.04.06 by KYK
	private static final String STBCDELAY = "STBCDelay";
	private static Logger stbcDelayLog = Logger.getLogger(STBCDELAY);
	public void traceSTBCDelay(String message) {
		stbcDelayLog.info(String.format("%s", message));
	}
	
}
