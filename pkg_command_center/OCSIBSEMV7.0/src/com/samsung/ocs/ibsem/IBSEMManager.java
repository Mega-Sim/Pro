package com.samsung.ocs.ibsem;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.config.CarrierTypeConfig;
import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.CARRIERLOC_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.RUNTIME_UPDATE;
import com.samsung.ocs.common.message.Message;
import com.samsung.ocs.common.message.MsgVector;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.ibsem.stbc.STBReportComm;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.EventHistoryManager;
import com.samsung.ocs.manager.impl.IBSEMHistoryManager;
import com.samsung.ocs.manager.impl.IBSEMReportManager;
import com.samsung.ocs.manager.impl.MaterialControlManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.RailDownControlManager;
import com.samsung.ocs.manager.impl.StationManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.VehicleErrorManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.model.Alarm;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.IBSEMHistory;
import com.samsung.ocs.manager.impl.model.IBSEMReport;
import com.samsung.ocs.manager.impl.model.MaterialControl;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.OCSInfo;
import com.samsung.ocs.manager.impl.model.RailDownControl;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.manager.impl.model.VehicleError;

/**
 * IBSEMManager Class, OCS 3.0 for Unified FAB
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

public class IBSEMManager extends AbstractOcsThread {
	private boolean isReportStarted;
	private boolean isIBSEMUsed;
	private MODULE_STATE serviceState = MODULE_STATE.OUTOFSERVICE;
	private MODULE_STATE requestedServiceState = MODULE_STATE.REQOUTOFSERVICE;

	private static String SendS6F11 ="SendS6F11";
	private static String EventName ="EventName";
	private static String EventType ="EventType";
	private static String TrCmd ="TrCmd";
	private static String CommandID ="CommandID";
	private static String SourcePort ="SourcePort";
	private static String DestPort ="DestPort";
	private static String CarrierLoc ="CarrierLoc";
	private static String CarrierID ="CarrierID";
	private static String Priority ="Priority";
	private static String Replace ="Replace";
	private static String ResultCode ="ResultCode";
	private static String Vehicle ="Vehicle";
	private static String VehicleID ="VehicleID";
	private static String TransferPort ="TransferPort";
	private static String Carrier ="Carrier";
	private static String TSC ="TSC";
	private static String SetAlarmReport ="SetAlarmReport";
	private static String ALID ="ALID";
	private static String VehicleState ="VehicleState";
	private static String ClearAlarmReport ="ClearAlarmReport";
	private static String VehicleType ="VehicleType";
	private static String VehicleCurrentDomain ="VehicleCurrentDomain";
	private static String VehicleCurrentPosition ="VehicleCurrentPosition";
	private static String CarrierInstalled = "CarrierInstalled";
	private static String CarrierRemoved = "CarrierRemoved";
	// 2014.01.02 by KBS : FoupID 추가  (for A-PJT EDS)
	private static String FoupID ="FoupID";
	
	private static final String IBSEM = "IBSEM";
	private static final String RUNTIMEUPDATE_START_INSERVICE = "Runtime Layout Update Started! - INSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_INSERVICE = "Runtime Layout Update Completed! - INSERVICE";
	private static final String RUNTIMEUPDATE_START_OUTOFSERVICE = "Runtime Layout Update Started! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE = "Runtime Layout Update Completed! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_FAILED = "Runtime Layout Update Failed!";
	private static final String INITIALIZING_CARRIERLOCMANAGER = "Initializing CarrierLocManager ...";
	private static final String INITIALIZING_NODEMANAGER = "Initializing NodeManager ...";
	private static final String INITIALIZING_VEHICLEERRORMANAGER = "Initializing VehicleErrorManager ...";	
	private static final String INITIALIZING_STATIONMANAGER = "Initializing StationManager ...";
	private static final String INITIALIZING_MATERIALCONTROLMANAGER = "Initializing MaterialControlManager ...";
	
	private IBSEM ibsem;
	// DataManager
	private VehicleManager vehicleManager;
	private TrCmdManager trCmdManager;
	private OCSInfoManager ocsInfoManager;
	private CarrierLocManager carrierLocManager;
	private IBSEMReportManager ibsemReportManager;
	private AlarmManager alarmManager;
	private EventHistoryManager eventHistoryManager;
	private NodeManager nodeManager;
	// 2013.06.28 by KYK
	private VehicleErrorManager vehicleErrorManager;
	// 2014.12.22 by KYK
	private StationManager stationManager;
	// 2015.09.02 by MYM : PortService 별도 Thread 분리 (Report 지연 및 DB Deadlock 방지)
	private PortServiceManager portServiceManager;
	private MaterialControlManager materialControlManager;

	private static IBSEMManager instance = null;
	
	// 2014.11.17 by KBS : STBC 이상감지
	private int stbReportSocketPort = 6001;
	private static STBReportComm stbReportComm = null;
	
	// Singleton : clusterManager 에서 INSERVICE REQ (인스턴스 중복 생성방지)
	public static synchronized IBSEMManager getInstance() {
		if (instance == null) {
			instance = new IBSEMManager();
		}
		return instance;
	}

	/**
	 * Constructor of IBSEMManager class.
	 */
	private IBSEMManager() {		
		super();
		initializeManager();
		
		// 2014.11.17 by KBS : STBC 이상감지 Port 설정
		stbReportSocketPort = CommonConfig.getInstance().getSTBReportSocketPort();
		
		// 2015.07.01 by MYM : CarrierType 설정 (Material - CarrierType)
		CarrierTypeConfig.getInstance();

		new LogManager(OcsConstant.IBSEM);
		new HistoryManager();
	}
	
	/**
	 * Initialize Managers
	 */
	public void initializeManager() {
		DBAccessManager dbAccessManager = new DBAccessManager();
		// 2015.09.02 by MYM : Report 지연 및 DB Deadlock 방지
//		TrCmdManager.getInstance(TrCmd.class, dbAccessManager, false, false, 0);
		trCmdManager = TrCmdManager.getInstance(TrCmd.class, new DBAccessManager(), false, false, 0);
		vehicleManager = VehicleManager.getInstance(Vehicle.class, dbAccessManager, false, false, 0);
		ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, dbAccessManager, true, true, 500);
		// 2013.10.22 by KYK
//		carrierLocManager = CarrierLocManager.getInstance(CarrierLoc.class, dbAccessManager, true, false, 0);
		carrierLocManager = CarrierLocManager.getInstance(CarrierLoc.class, dbAccessManager, true, true, 500);
		carrierLocManager.setIBSEM(true);
		// 2015.09.02 by MYM : Report 지연 및 DB Deadlock 방지
//		ibsemReportManager = IBSEMReportManager.getInstance(IBSEMReport.class, dbAccessManager, false, false, 0);		
		ibsemReportManager = IBSEMReportManager.getInstance(IBSEMReport.class, new DBAccessManager(), false, false, 0);		

		// 2012.05.16 by MYM : Rail-Down
		RailDownControlManager.getInstance(RailDownControl.class, dbAccessManager, true, true, 200);
		nodeManager = NodeManager.getInstance(Node.class, dbAccessManager, true, true, 200);

		IBSEMHistoryManager.getInstance(IBSEMHistory.class, dbAccessManager, false, true, 500);
		alarmManager = AlarmManager.getInstance(Alarm.class, dbAccessManager, false, false, 500);
		
		eventHistoryManager = EventHistoryManager.getInstance(EventHistory.class, dbAccessManager, false, true, 200);
		
		// 2013.06.28 by KYK
		vehicleErrorManager = VehicleErrorManager.getInstance(VehicleError.class, dbAccessManager, true, false, 0);
		// 2014.12.22 by KYK
//		StationManager.getInstance(Station.class, dbAccessManager, true, true, 500);
		stationManager = StationManager.getInstance(Station.class, dbAccessManager, true, true, 500);
		
		// 2015.09.04 by MYM : PortService 별도 Thread 처리 (Report 지연 및 DB Deadlock 방지)
		portServiceManager = new PortServiceManager(this, ibsem, carrierLocManager, ocsInfoManager);
		portServiceManager.start();
		
		// 2018.03.12 by LSH : Active IBSEM만 IBSEMREPORT 테이블 정리하도록 변경
//		ibsemReportManager.clearIBSEMReport();
		
		materialControlManager = MaterialControlManager.getInstance(MaterialControl.class, dbAccessManager, true, false, 0);
	}
	
	private void checkCurrentDBStatus() {
		trCmdManager.checkCurrentDBStatus();
		vehicleManager.checkCurrentDBStatus();
		carrierLocManager.checkCurrentDBStatus();
		ocsInfoManager.checkCurrentDBStatus();
		alarmManager.checkCurrentDBStatus();
		vehicleErrorManager.checkCurrentDBStatus();
	}
	
	/**
	 * Start IBSEM
	 * 
	 * @return
	 */
	private boolean startService() {
		checkCurrentDBStatus();
		if (ibsem == null && ocsInfoManager.isIBSEMUsed()) {
			ibsem = new IBSEM(this);
		}
		return true;
	}

	/**
	 * Stop IBSEM
	 * 
	 * @return
	 */
	private boolean stopService() {
		if (ibsem != null) {
			if (ibsem.stopIBSEM()) {
				stopReport();
				ibsem = null;
			} else {
				return false;				
			}
		}
		return true;
	}

	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = 500;
	}

	/**
	 * IBSEMManager의 mainProcessing
	 */
	@Override
	protected void mainProcessing() {
		// 1. IBSEMUSAGE & OWNERSHIP 에 따라 SERVICE START/STOP
		manageServiceState();

		// 2. MANAGE REPORT (SEND MSG TO MCS)
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			manageReport();
		}

		// 3. RUNTIME LAYOUT UPDATE
		manageRuntimeUpdate();
	}

	@Override
	protected void stopProcessing() {
		traceIBSEMMain("IBSEMManager Thread is stopped!");
	}
	
	
	public void changeRequestedServiceState(MODULE_STATE requestedState) {
		this.requestedServiceState = requestedState;
	}
	
	public MODULE_STATE getServiceState() {
		return this.serviceState;
	}
	
	/**
	 * IBSEMUSAGE (OCSINFO) 와 SERVICESTATE (OCS_CLUSTER_STATE) 를 체크하여 IBSEM Service 실행여부 결정
	 * IBSEMUSAGE=YES, INSERVICE (or REQINSERVICE) 일때 동작 (IBSEM 인스턴스생성) / 정지일 경우, 인스턴스제거 
	 */
	void manageServiceState() {
		// IBSEMUSAGE : YES
		if (ocsInfoManager.isIBSEMUsed()) {
			// IBSEMUSAGE : NO -> YES 로 변경됨
			if (isIBSEMUsed != ocsInfoManager.isIBSEMUsed()) {
				if (this.requestedServiceState == MODULE_STATE.REQINSERVICE
						|| this.serviceState == MODULE_STATE.INSERVICE) {
					changeServiceState(MODULE_STATE.INSERVICE);
					// 2018.03.12 by LSH : Active IBSEM만 IBSEMREPORT 테이블 정리하도록 변경
					ibsemReportManager.clearIBSEMReport();
					startService();
					traceIBSEMMain("Activated!");					
				}
				isIBSEMUsed = ocsInfoManager.isIBSEMUsed();
			}
			else {
				// OUTOFSERVICE 요청받음
				if (this.requestedServiceState == MODULE_STATE.REQOUTOFSERVICE) {
					traceIBSEMMain("Deactivated!");
					changeServiceState(MODULE_STATE.OUTOFSERVICE);
					stopService();
				}
				// INSERVICE 요청받음 (현재 INSERVICE 아닐 경우 동작)
				else if (this.serviceState != MODULE_STATE.INSERVICE && 
						this.requestedServiceState == MODULE_STATE.REQINSERVICE) {
					changeServiceState(MODULE_STATE.INSERVICE);
					// 2018.03.12 by LSH : Active IBSEM만 IBSEMREPORT 테이블 정리하도록 변경
					ibsemReportManager.clearIBSEMReport();
					startService();
					traceIBSEMMain("Activated!");
				}				
			}
			
			// 2012.01.06 by PMM
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				if (ibsem != null) {
					ibsem.setFormattedLogUsed(ocsInfoManager.isFormattedLogUsed());
				}
			}
		} 
		// IBSEMUSAGE : NO
		else {
			// IBSEMUSAGE : YES -> NO 로 변경됨
			if (isIBSEMUsed != ocsInfoManager.isIBSEMUsed()) {
				traceIBSEMMain("Deactivated!");
				changeServiceState(MODULE_STATE.OUTOFSERVICE);
				stopService();
				
				isIBSEMUsed = ocsInfoManager.isIBSEMUsed();
			}			
		}
		
		// 2015.02.25 by KBS : STBC 이상감지 on/off
		// 2015.03.04 by KBS : InService일 때 동작하도록
		if (this.serviceState == MODULE_STATE.INSERVICE && ocsInfoManager.isSTBReportUsed()) {
			// STBC_REPORT_USAGE : YES
			if (stbReportComm == null) {
				stbReportComm = new STBReportComm(stbReportSocketPort);
				stbReportComm.setStbReportCommUse(true);
				stbReportComm.startSTBReport();
			}
		} else {
			// STBC_REPORT_USAGE : NO
			if (stbReportComm != null) {
				stbReportComm.setStbReportCommUse(false);
				stbReportComm.closeServerSocket();
				stbReportComm.stopSTBReport();
				stbReportComm = null;
			}
		}
	}

	/**
	 * change ServiceState
	 * @param state
	 */
	private void changeServiceState(MODULE_STATE state) {
		this.requestedServiceState = state;
		this.serviceState = state;
	}
	
	/**
	 * Manager IBSEM RuntimeUpdate
	 */
	private void manageRuntimeUpdate() {
		assert ocsInfoManager != null;

		if (ocsInfoManager.getIBSEMUpdate() != RUNTIME_UPDATE.NO) {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				if (ocsInfoManager.getIBSEMUpdate() == RUNTIME_UPDATE.YES) {
					registerAlarmTextWithLevel(RUNTIMEUPDATE_START_INSERVICE, ALARMLEVEL.INFORMATION);
					traceRuntimeUpdate(RUNTIMEUPDATE_START_INSERVICE);
					
					// 2011.12.05 by PMM
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_START_INSERVICE, 
							"", "", EVENTHISTORY_REMOTEID.IBSEM, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
					
					updateManager();
					
					unregisterAlarmText(RUNTIMEUPDATE_START_INSERVICE);
					traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_INSERVICE);
					
					// 2011.12.05 by PMM
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_COMPLETED_INSERVICE, 
							"", "", EVENTHISTORY_REMOTEID.IBSEM, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
					
					ocsInfoManager.setIBSEMUpdate(RUNTIME_UPDATE.DONE);
				} else if (ocsInfoManager.getIBSEMUpdate() != RUNTIME_UPDATE.DONE) {
					ocsInfoManager.setIBSEMUpdate(RUNTIME_UPDATE.NO);
				}
			} else if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
				if (ocsInfoManager.getIBSEMUpdate() == RUNTIME_UPDATE.DONE) {
					registerAlarmTextWithLevel(RUNTIMEUPDATE_START_OUTOFSERVICE, ALARMLEVEL.INFORMATION);
					traceRuntimeUpdate(RUNTIMEUPDATE_START_OUTOFSERVICE);
					
					// 2011.12.05 by PMM
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_START_OUTOFSERVICE, 
							"", "", EVENTHISTORY_REMOTEID.IBSEM, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
					
					updateManager();
					
					unregisterAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
					traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE);
					
					// 2011.12.05 by PMM
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE, 
							"", "", EVENTHISTORY_REMOTEID.IBSEM, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
					
					ocsInfoManager.setIBSEMUpdate(RUNTIME_UPDATE.NO);
				}
			}
		}
	}
	
	/**
	 * RuntimeUpdate Managers
	 */
	private void updateManager() {
		assert carrierLocManager != null;
		try {
			registerAlarmTextWithLevel(INITIALIZING_CARRIERLOCMANAGER, ALARMLEVEL.INFORMATION);
			
			traceRuntimeUpdate("   " + INITIALIZING_CARRIERLOCMANAGER);
			carrierLocManager.initializeFromDB();
			unregisterAlarmText(INITIALIZING_CARRIERLOCMANAGER);
			
			// 2012.05.16 by MYM : Rail-Down
			registerAlarmTextWithLevel(INITIALIZING_NODEMANAGER, ALARMLEVEL.INFORMATION);
			
			traceRuntimeUpdate("   " + INITIALIZING_NODEMANAGER);
			nodeManager.initializeFromDB();
			unregisterAlarmText(INITIALIZING_NODEMANAGER);
			
			// 2014.12.22 by KYK 
			registerAlarmTextWithLevel(INITIALIZING_STATIONMANAGER, ALARMLEVEL.INFORMATION);			
			traceRuntimeUpdate("   " + INITIALIZING_STATIONMANAGER);
			stationManager.initializeFromDB();
			unregisterAlarmText(INITIALIZING_STATIONMANAGER);			
			
			// 2013.06.28 by KYK
			registerAlarmTextWithLevel(INITIALIZING_VEHICLEERRORMANAGER, ALARMLEVEL.INFORMATION);
			
			traceRuntimeUpdate("   " + INITIALIZING_VEHICLEERRORMANAGER);
			vehicleErrorManager.initializeFromDB();
			if (ibsem != null) {
				ibsem.loadAlarmDataFromDB();
				ibsem.enableAlarm();				
			}
			unregisterAlarmText(INITIALIZING_VEHICLEERRORMANAGER);
			
			registerAlarmText(INITIALIZING_MATERIALCONTROLMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_MATERIALCONTROLMANAGER);
			materialControlManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_MATERIALCONTROLMANAGER);
		
			
			// 2015.07.01 by MYM : CarrierTypeConfig Reload
			CarrierTypeConfig.getInstance().loadCarrierTypeConfig();
		} catch (Exception e) {
			traceRuntimeUpdate(RUNTIMEUPDATE_FAILED);
			traceException("updateManager()", e);
		}
	}

	/**
	 * 주기적으로 DB IBSEMREPORT 테이블로부터 데이터 (CMessage 형태로 저장된 msg)를 가져와서
	 * SECS메시지 형태로 MCS 에 보고(report) 한다.  
	 */
	void manageReport(){			
		if (isReportStarted) {
			/* [AS-WAS(IS) Version]
			 *  : Operation 동작에 따른 event CMessage 형태로 DB IBSEMREPORT 에 저장 
			 *  IBSEM 은 주기적으로 IBSEMREPORT 읽어와 MCS 에 report
			 **/
			
			String lastReportTime = null;
			ArrayList<String> msgList = new ArrayList<String>();
			// Step 1 : select * from IBSEMREPORT
			msgList = ibsemReportManager.getReportToMCSDataFromDB();
			lastReportTime = ibsemReportManager.getLastReportTime();
			
			// Step 2 : CMessage 형태로 저장된 데이터 파싱하여 SECS메시지 형태로 MCS 보고
			if (msgList.size() > 0) {
				String strMsgName;
				Message msg = new Message();
				
				String eventName;
				String eventType;
				String commandId;
				String sourceLoc;
				String destLoc;
				String carrierLoc;
				String carrierId;
				int priority;
				int replace;
				int resultCode;
				
				String vehicleId;
				String transferPort;
				long alarmId;
				int vehicleState;
				int vehicletype;
				String foupId;
				
				for (String strMsg:msgList) {
					msg.reset();
					msg.setMessage(strMsg);
					strMsgName = msg.getMessageName();
					traceIBSEMReport("Msg : " + strMsg);
					if (SendS6F11.equalsIgnoreCase(strMsgName)) {
						MsgVector value = new MsgVector();
						msg.getMessageItem(EventName, value, 0, false);
						eventName = value.toString(0);

						msg.getMessageItem(EventType, value, 0, false);
						eventType = value.toString(0);
						
						if (TrCmd.equalsIgnoreCase(eventType)) {
							msg.getMessageItem(CommandID, value, 0, false);
							commandId = value.toString(0); // ? null 처리
							msg.getMessageItem(SourcePort, value, 0, false);
							sourceLoc = value.toString(0);
							msg.getMessageItem(DestPort, value, 0, false);
							destLoc = value.toString(0);
							msg.getMessageItem(CarrierLoc, value, 0, false);
							carrierLoc = value.toString(0);
							msg.getMessageItem(CarrierID, value, 0, false);
							carrierId = value.toString(0);
							msg.getMessageItem(Priority, value, 0, false);
							priority = value.toInt(0);
							msg.getMessageItem(Replace, value, 0, false);
							replace = value.toInt(0);
							msg.getMessageItem(ResultCode, value, 0, false);
							resultCode = value.toInt(0);
							ibsem.sendS6F11TrCmd(eventName, commandId, sourceLoc, destLoc, carrierLoc, carrierId, priority, replace, resultCode);						
						} else if (Vehicle.equalsIgnoreCase(eventType)) {
							msg.getMessageItem(CommandID, value, 0, false);
							commandId =  value.toString(0);
							msg.getMessageItem(VehicleID, value, 0, false);
							vehicleId =  value.toString(0);
							msg.getMessageItem(TransferPort, value, 0, false);
							transferPort =  value.toString(0);
							msg.getMessageItem(CarrierID, value, 0, false);
							carrierId = value.toString(0);		
							ibsem.sendS6F11Vehicle(eventName, commandId, vehicleId, transferPort, carrierId);
						} else if (Carrier.equalsIgnoreCase(eventType)) {
							msg.getMessageItem(CommandID, value, 0, false);
							commandId = value.toString(0);
							msg.getMessageItem(VehicleID, value, 0, false);
							vehicleId = value.toString(0);
							msg.getMessageItem(CarrierID, value, 0, false);
							carrierId = value.toString(0);
							msg.getMessageItem(CarrierLoc, value, 0, false);
							carrierLoc = value.toString(0);
							// 2012.05.16 by MYM : VehicleType 추가
							// Rail-Down - S1a Foup, Reticle 통합 반송시 사양(IBSEM Spec for Conveyor usage in one OHT) 대응
							msg.getMessageItem(VehicleType, value, 0, false);
							vehicletype = value.toInt(0);
							// 2014.01.02 by KBS : FoupID 추가  (for A-PJT EDS)
							msg.getMessageItem(FoupID, value, 0, false);
							// 2015.06.11 by KYK
//							foupId = value.toString(0);
							if (value != null && value.size() > 0) {
								foupId = value.toString(0);
							} else {
								foupId = "";
							}
							ibsem.sendS6F11Carrier(eventName, commandId, vehicleId, carrierId, carrierLoc, vehicletype, foupId);							
							
							// 2015.02.25 by KBS : STBC 이상감지 on/off
							if (ocsInfoManager.isSTBReportUsed() && stbReportComm != null) {
								// 2014.11.14 by KBS : STBC 이상감지 (TCP/IP)
								msg.getMessageItem(TransferPort, value, 0, false);
								// 2015.06.11 by KYK
//								transferPort =  value.toString(0);
								if (value != null && value.size() > 0) {
									transferPort = value.toString(0);
								} else {
									transferPort = "";
								}
								// 2015.05.27 by MYM : transferPort가 Null인 경우 조건 추가
//								if ((CarrierInstalled.equals(eventName) || CarrierRemoved.equals(eventName)) && 
//										(CARRIERLOC_TYPE.STBPORT == carrierLocManager.getCarrierLocData(transferPort).getType() || 
//										CARRIERLOC_TYPE.UTBPORT == carrierLocManager.getCarrierLocData(transferPort).getType())) {
//									stbReportComm.registerSendData(eventName, vehicleId, transferPort, carrierId);
//								}
								CARRIERLOC_TYPE portType = getPortType(transferPort);
								if ((CarrierInstalled.equals(eventName) || CarrierRemoved.equals(eventName)) && 
										(CARRIERLOC_TYPE.STBPORT == portType || CARRIERLOC_TYPE.UTBPORT == portType)) {
									stbReportComm.registerSendData(eventName, vehicleId, transferPort, carrierId);
								}
							}
						} else if (TSC.equalsIgnoreCase(eventType)) {
							ibsem.sendS6F11(eventName, "", "", 0);
						}						
					} else if (SetAlarmReport.equalsIgnoreCase(strMsgName)) {
						MsgVector value = new MsgVector();
						msg.getMessageItem(ALID, value, 0, false);
//						alarmId = (long) value.toDouble(0);
						alarmId = value.toInt(0);
						msg.getMessageItem(VehicleID, value, 0, false);
						vehicleId = value.toString(0);
						msg.getMessageItem(VehicleState, value, 0, false);
						vehicleState = value.toInt(0);
						msg.getMessageItem(CommandID, value, 0, false);
						commandId = value.toString(0);
						
						// 2013.10.01 by MYM : UnitAlarmSet Event 추가
						String vehicleCurrentDomain = "";
						int vehicleCurrentPosition = 0;
						sourceLoc = "";
						destLoc = "";
						try {
							msg.getMessageItem(SourcePort, value, 0, false);
							sourceLoc = value.toString(0);
							msg.getMessageItem(DestPort, value, 0, false);
							destLoc = value.toString(0);
							msg.getMessageItem(VehicleCurrentDomain, value, 0, false);
							vehicleCurrentDomain = value.toString(0);
							msg.getMessageItem(VehicleCurrentPosition, value, 0, false);
							vehicleCurrentPosition = Integer.parseInt(value.toString(0));
						} catch (Exception e) {}
						ibsem.setAlarmReport(alarmId, vehicleId, vehicleState, commandId, vehicleCurrentDomain, vehicleCurrentPosition, sourceLoc, destLoc);
						System.out.println("SetAlarmReport Called");
					} else if (ClearAlarmReport.equalsIgnoreCase(strMsgName)) {
						MsgVector value = new MsgVector();
						msg.getMessageItem(ALID, value, 0, false);
						// 2015.05.01 by KYK [Commfail AlarmReport]
//						alarmId = 0;// (long) value.toDouble(0);
						alarmId = value.toInt(0);
						msg.getMessageItem(VehicleID, value, 0, false);
						vehicleId = value.toString(0);
						msg.getMessageItem(VehicleState, value, 0, false);
						vehicleState = value.toInt(0);
						msg.getMessageItem(CommandID, value, 0, false);
						commandId = value.toString(0);
						
						// 2013.10.01 by MYM : UnitAlarmCleared Event 추가
						String vehicleCurrentDomain = "";
						int vehicleCurrentPosition = 0;
						sourceLoc = "";
						destLoc = "";
						try {
							msg.getMessageItem(SourcePort, value, 0, false);
							sourceLoc = value.toString(0);
							msg.getMessageItem(DestPort, value, 0, false);
							destLoc = value.toString(0);
							msg.getMessageItem(VehicleCurrentDomain, value, 0, false);
							vehicleCurrentDomain = value.toString(0);
							msg.getMessageItem(VehicleCurrentPosition, value, 0, false);
							vehicleCurrentPosition = Integer.parseInt(value.toString(0));
						} catch (Exception e) {}
						ibsem.clearAlarmReport(alarmId, vehicleId, vehicleState, commandId, vehicleCurrentDomain, vehicleCurrentPosition, sourceLoc, destLoc);						
						System.out.println("ClearAlarmReport Called");
					}
				}
				
				// Delete Data reported to MCS
				if (lastReportTime != null) {					
					ibsemReportManager.deleteIBSEMReportData(msgList, lastReportTime);					
				}
			}
		}
	}
	
	/**
	 * 2015.05.27 by MYM : PortType 추가
	 * 배경 : STBC 이상감지 기능 On시 transferPort 가져올 때 사용함
	 * @param carrierLocId
	 * @return
	 */
	public CARRIERLOC_TYPE getPortType(String carrierLocId) {
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(carrierLocId);		
		if (carrierLoc != null) {
			return carrierLoc.getType();
		} else {
			return CARRIERLOC_TYPE.NULL;
		}
	}

	public boolean isReportStarted() {
		return isReportStarted;
	}
	
	public IBSEM getIbsem() {
		return ibsem;
	}
	
	public void startReport() {
		isReportStarted = true;
	}
	
	public void stopReport() {
		isReportStarted = false;
	}	
	
	private static final String IBSEM_MAIN = "IBSEMMain";
	private static Logger ibsemMainLog = Logger.getLogger(IBSEM_MAIN);
	public void traceIBSEMMain(String message) {
		ibsemMainLog.debug(String.format("%s", message));
	}
	
	private static final String IBSEM_REPORT = "IBSEMReport";
	private static Logger ibsemReportLog = Logger.getLogger(IBSEM_REPORT);
	public void traceIBSEMReport(String message) {
		ibsemReportLog.debug(String.format("%s", message));
	}
	
	private static final String IBSEM_EXCEPTION = "IBSEMException";
	private static Logger ibsemExceptionLog = Logger.getLogger(IBSEM_EXCEPTION);
	public void traceException(String message) {
		ibsemExceptionLog.error(String.format("%s", message));
	}
	public void traceException(String message, Throwable t) {
		ibsemExceptionLog.error(String.format("%s", message), t);
	}
	
	// 2011.11.05 by PMM
	private static final String RUNTIMEUPDATE = "RuntimeUpdateHistory";
	private static Logger runtimeUpdateHistoryLog = Logger.getLogger(RUNTIMEUPDATE);
	public void traceRuntimeUpdate(String message) {
		runtimeUpdateHistoryLog.info(String.format("%s", message));
	}
	
	public void registerAlarmText(String alarmText) {
		alarmManager.registerAlarmText(IBSEM, alarmText);
	}
	
	private void registerAlarmTextWithLevel(String alarmText, ALARMLEVEL alarmLevel) {
		if (alarmManager != null) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.registerAlarmTextWithLevel(IBSEM, alarmText, alarmLevel.toConstString());
		}
	}
	
	public void unregisterAlarmText(String alarmText) {
		alarmManager.unregisterAlarmText(IBSEM, alarmText);
	}
	
	// 2011.12.05 by PMM
	private void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
		eventHistoryManager.addEventHistoryToRegisterList(eventHistory, duplicateCheck);
	}
}
