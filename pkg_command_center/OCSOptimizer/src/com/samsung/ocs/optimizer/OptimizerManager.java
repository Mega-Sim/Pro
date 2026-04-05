package com.samsung.ocs.optimizer;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.RUNTIME_UPDATE;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.AreaManager;
import com.samsung.ocs.manager.impl.CollisionManager;
import com.samsung.ocs.manager.impl.EventHistoryManager;
import com.samsung.ocs.manager.impl.HIDManager;
import com.samsung.ocs.manager.impl.LocalGroupInfoManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.ParkManager;
import com.samsung.ocs.manager.impl.SectionManager;
import com.samsung.ocs.manager.impl.StationManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.ZoneControlManager;
import com.samsung.ocs.manager.impl.model.Alarm;
import com.samsung.ocs.manager.impl.model.Area;
import com.samsung.ocs.manager.impl.model.Collision;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.LocalGroupInfo;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.OCSInfo;
import com.samsung.ocs.manager.impl.model.Park;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.manager.impl.model.ZoneControl;

/**
 * OptimizerManager Class, OCS 3.0 for Unified FAB
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

public class OptimizerManager extends AbstractOcsThread {
	private Optimizer optimizer;
	
	private MODULE_STATE serviceState = MODULE_STATE.OUTOFSERVICE;
	private MODULE_STATE requestedSerivceState = MODULE_STATE.REQOUTOFSERVICE;
//	private MODULE_STATE serviceState = MODULE_STATE.INSERVICE;
//	private MODULE_STATE requestedSerivceState = MODULE_STATE.REQINSERVICE;
	private int deactivationCheckCount = 0;
	private OCSInfoManager ocsInfoManager;
	private HIDManager hidManager;
	private NodeManager nodeManager;
	private SectionManager sectionManager;
	private CollisionManager collisionManager;
	private ZoneControlManager zoneControlManager;
	private AlarmManager alarmManager;
	private EventHistoryManager eventHistoryManager;
	private ParkManager parkManager;
	
	private int outOfServiceCheckCount;
	
	private static final String OPTIMIZER = "Optimizer";
	private static final String RUNTIMEUPDATE_START_INSERVICE = "Runtime Layout Update Started! - INSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_INSERVICE = "Runtime Layout Update Completed! - INSERVICE";
	private static final String RUNTIMEUPDATE_START_OUTOFSERVICE = "Runtime Layout Update Started! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE = "Runtime Layout Update Completed! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_FAILED = "Runtime Layout Update Failed!";
	private static final String INITIALIZING_HIDMANAGER = "Initializing HIDManager ...";
	private static final String INITIALIZING_NODEMANAGER = "Initializing NodeManager ...";
	private static final String INITIALIZING_SECTIONMANAGER = "Initializing SectionManager ...";
	private static final String INITIALIZING_COLLISIONMANAGER = "Initializing CollisionManager ...";
	private static final String INITIALIZING_ZONECONTROLMANAGER = "Initializing ZoneControlManager ...";
	
	private static final String OPTIMIZER_MAIN = "OptimizerMain";
	private static Logger optimizerMainLog = Logger.getLogger(OPTIMIZER_MAIN);
	
	private static final String OPTIMIZER_EXCEPTION = "OptimizerException";
	private static Logger optimizerExceptionLog = Logger.getLogger(OPTIMIZER_EXCEPTION);
	
	private static final String RUNTIMEUPDATEHISTORY = "RuntimeUpdateHistory";
	private static Logger runtimeUpdateHistoryLog = Logger.getLogger(RUNTIMEUPDATEHISTORY);
	
	/**
	 * Constructor of OptimizerManager class.
	 */
	public OptimizerManager() {
		initializeManager();
		optimizer = new Optimizer();
		new LogManager(OPTIMIZER);
	}
	
	/**
	 * 
	 */
	private void initializeManager() {
		DBAccessManager dbAccessManager = new DBAccessManager();
		DBAccessManager dbAccessManager2 = new DBAccessManager();
		
		ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, dbAccessManager, true, true, 500);
		hidManager = HIDManager.getInstance(Hid.class, dbAccessManager2, true, true, 200);
		nodeManager = NodeManager.getInstance(Node.class, dbAccessManager, true, true, 500);
		nodeManager.updateCarrierLocMaterialOfBay();
		sectionManager = SectionManager.getInstance(Section.class, dbAccessManager, true, false, 500);
		collisionManager = CollisionManager.getInstance(Collision.class, dbAccessManager2, true, false, 500);
		AreaManager.getInstance(Area.class, dbAccessManager, true, true, 500);
		zoneControlManager = ZoneControlManager.getInstance(ZoneControl.class, dbAccessManager2, true, false, 500);
		VehicleManager.getInstance(Vehicle.class, dbAccessManager, true, false, 500);
		alarmManager = AlarmManager.getInstance(Alarm.class, dbAccessManager2, true, true, 500);
		LocalGroupInfoManager.getInstance(LocalGroupInfo.class, dbAccessManager, true, true, 500);
		TrCmdManager.getInstance(TrCmd.class, dbAccessManager2, true, false, 200);
		eventHistoryManager = EventHistoryManager.getInstance(EventHistory.class, dbAccessManager, false, true, 200);
		StationManager.getInstance(Station.class, dbAccessManager2, true, true, 500); // 2020.06.12 by JJW : Station Manager Object Type Not Supported 처리
		parkManager = ParkManager.getInstance(Park.class, dbAccessManager2, true, true, 300);
	}
	
	@Override
	/**
	 * 
	 */
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = 5000;
		outOfServiceCheckCount = 0;
	}

	/**
	 * Main Processing Method
	 */
	@Override
	protected void mainProcessing() {
		manageServiceState();
		
		if (serviceState != requestedSerivceState) {
			if (requestedSerivceState == MODULE_STATE.INSERVICE)
				return;
			
			serviceState = requestedSerivceState;
		}
		
		manageRuntimeUpdate();
		
		if (serviceState == MODULE_STATE.INSERVICE)	{
			// Ownership을 가지고 있는 경우에 한해 진행.
			optimizer.mainProcessing();
			
		} else if (serviceState == MODULE_STATE.OUTOFSERVICE) {
			if (deactivationCheckCount > 9) {
				traceOptimizerMain("Deactivated.");
				deactivationCheckCount = 0;
			} else {
				deactivationCheckCount++;
			}
		}
	}

	@Override
	protected void stopProcessing() {
		
	}
	
	/**
	 * 
	 * @param requestedState
	 */
	public void requestChangeServiceState(MODULE_STATE requestedState) {
		this.requestedSerivceState = requestedState;
	}
	
	/**
	 * 
	 * @return
	 */
	public MODULE_STATE getServiceState() {
		return this.serviceState;
	}
	
	/**
	 * 
	 */
	private void manageServiceState() {
		try {
			if (this.requestedSerivceState == MODULE_STATE.REQOUTOFSERVICE) {
				changeServiceState(MODULE_STATE.OUTOFSERVICE);
				traceOptimizerMain("Deactivated!");
			} else if (this.serviceState != MODULE_STATE.INSERVICE && 
					this.requestedSerivceState == MODULE_STATE.REQINSERVICE) {
				changeServiceState(MODULE_STATE.INSERVICE);
				traceOptimizerMain("Activated!");
			}
			
			if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
				if (++outOfServiceCheckCount >= 5) {
					traceOptimizerMain("      [OUTOFSERVICE]");
					outOfServiceCheckCount = 0;
				}
			}
		} catch (Exception e) {
			traceOptimizerException("manageServiceState()", e);
		}
	}
	
	private void changeServiceState(MODULE_STATE state) {
		this.requestedSerivceState = state;
		this.serviceState = state;
	}
	
	/**
	 * 
	 */
	private void manageRuntimeUpdate() {
		assert ocsInfoManager != null;
		
		try {
			if (ocsInfoManager.getOptimizerUpdate() != RUNTIME_UPDATE.NO) {
				if (this.serviceState == MODULE_STATE.INSERVICE) {
					if (ocsInfoManager.getOptimizerUpdate() == RUNTIME_UPDATE.YES) {
//						registerAlarmText(RUNTIMEUPDATE_START_INSERVICE);
						registerAlarmTextWithLevel(RUNTIMEUPDATE_START_INSERVICE, ALARMLEVEL.INFORMATION);
						traceRuntimeUpdate(RUNTIMEUPDATE_START_INSERVICE);
						
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_START_INSERVICE, 
								"", "", EVENTHISTORY_REMOTEID.OPTIMIZER, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
						
						// Manager Update!
						updateManager();
						unregisterAlarmText(RUNTIMEUPDATE_START_INSERVICE);
						
						traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_INSERVICE);
						
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_COMPLETED_INSERVICE, 
								"", "", EVENTHISTORY_REMOTEID.OPTIMIZER, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
						
						ocsInfoManager.setOptimizerUpdate(RUNTIME_UPDATE.DONE);
					} else if (ocsInfoManager.getOptimizerUpdate() != RUNTIME_UPDATE.DONE) {
						ocsInfoManager.setOptimizerUpdate(RUNTIME_UPDATE.NO);
					}
				} else if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
					if (ocsInfoManager.getOptimizerUpdate() == RUNTIME_UPDATE.DONE) {
//						registerAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
						registerAlarmTextWithLevel(RUNTIMEUPDATE_START_OUTOFSERVICE, ALARMLEVEL.INFORMATION);
						traceRuntimeUpdate(RUNTIMEUPDATE_START_OUTOFSERVICE);
						
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_START_OUTOFSERVICE, 
								"", "", EVENTHISTORY_REMOTEID.OPTIMIZER, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
						
						updateManager();
						unregisterAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
						traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE);
						
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE, 
								"", "", EVENTHISTORY_REMOTEID.OPTIMIZER, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
						
						ocsInfoManager.setOptimizerUpdate(RUNTIME_UPDATE.NO);
					}
				}
			}
		} catch (Exception e) {
			traceOptimizerException("manageRuntimeUpdate()", e);
		}
	}
	
	/**
	 * 
	 */
	private void updateManager() {
		assert hidManager != null;
		assert nodeManager != null;
		assert sectionManager != null;
		assert collisionManager != null;
		assert zoneControlManager != null;
		
		try {
//			registerAlarmText(INITIALIZING_HIDMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_HIDMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_HIDMANAGER);
			hidManager.initializeFromDB();		// data reset.
			unregisterAlarmText(INITIALIZING_HIDMANAGER);
			
//			registerAlarmText(INITIALIZING_NODEMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_NODEMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_NODEMANAGER);
			nodeManager.initializeFromDB();
			unregisterAlarmText(INITIALIZING_NODEMANAGER);
			
//			registerAlarmText(INITIALIZING_SECTIONMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_SECTIONMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_SECTIONMANAGER);
			sectionManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_SECTIONMANAGER);
			
//			registerAlarmText(INITIALIZING_COLLISIONMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_COLLISIONMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_COLLISIONMANAGER);
			collisionManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_COLLISIONMANAGER);
			
//			registerAlarmText(INITIALIZING_ZONECONTROLMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_ZONECONTROLMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_ZONECONTROLMANAGER);
			zoneControlManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_ZONECONTROLMANAGER);
		} catch (Exception e) {
			traceRuntimeUpdate(RUNTIMEUPDATE_FAILED);
			traceOptimizerException("updateManager()", e);
		}
	}
	
	private void registerAlarmText(String alarmText) {
		alarmManager.registerAlarmText(OPTIMIZER, alarmText);
	}
	
	private void registerAlarmTextWithLevel(String alarmText, ALARMLEVEL alarmLevel) {
		if (alarmManager != null) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.registerAlarmTextWithLevel(OPTIMIZER, alarmText, alarmLevel.toConstString());
		}
	}
	
	private void unregisterAlarmText(String alarmText) {
		alarmManager.unregisterAlarmText(OPTIMIZER, alarmText);
	}
	
	private void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
		eventHistoryManager.addEventHistoryToRegisterList(eventHistory, duplicateCheck);
	}
	
	private void traceOptimizerMain(String message) {
		optimizerMainLog.debug(message);
	}
	
	private void traceOptimizerException(String message, Throwable e) {
		optimizerExceptionLog.error(message, e);
	}
	
	private void traceRuntimeUpdate(String message) {
		runtimeUpdateHistoryLog.info(message);
	}
}
