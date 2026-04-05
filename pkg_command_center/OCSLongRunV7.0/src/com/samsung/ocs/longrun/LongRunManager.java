package com.samsung.ocs.longrun;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.RUNTIME_UPDATE;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.CollisionManager;
import com.samsung.ocs.manager.impl.DockingStationManager;
import com.samsung.ocs.manager.impl.EventHistoryManager;
import com.samsung.ocs.manager.impl.HIDManager;
import com.samsung.ocs.manager.impl.LocalGroupInfoManager;
import com.samsung.ocs.manager.impl.MaterialControlManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.SectionManager;
import com.samsung.ocs.manager.impl.TourManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.UserRequestManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.ZoneControlManager;
import com.samsung.ocs.manager.impl.model.Alarm;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.Collision;
import com.samsung.ocs.manager.impl.model.DockingStation;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.LocalGroupInfo;
import com.samsung.ocs.manager.impl.model.MaterialControl;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.OCSInfo;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.Tour;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.UserRequest;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.manager.impl.model.ZoneControl;

/**
 * LongRunManager Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 3. 7.
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

public class LongRunManager extends AbstractOcsThread {
	private MODULE_STATE serviceState = MODULE_STATE.OUTOFSERVICE;
	private MODULE_STATE requestedSerivceState = MODULE_STATE.REQOUTOFSERVICE;
	private int deactivationCheckCount = 0;
	private OCSInfoManager ocsInfoManager;
	private CarrierLocManager carrierLocManager;
	private HIDManager hidManager;
	private NodeManager nodeManager;
	private SectionManager sectionManager;
	private CollisionManager collisionManager;
	private ZoneControlManager zoneControlManager;
	private AlarmManager alarmManager;
	private UserRequestManager userRequestManager;
	private TourManager tourManager;
	private DockingStationManager dockingStationManager;
	private EventHistoryManager eventHistoryManager;
	// 2013.08.30 by KYK
	private MaterialControlManager materialControlManager;
	
	private LongRun longRun;
	
	private int outOfServiceCheckCount;
	
	private static final String LONGRUN = "LongRun";
	
	private static final String RUNTIMEUPDATE_START_INSERVICE = "Runtime Layout Update Started! - INSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_INSERVICE = "Runtime Layout Update Completed! - INSERVICE";
	private static final String RUNTIMEUPDATE_START_OUTOFSERVICE = "Runtime Layout Update Started! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE = "Runtime Layout Update Completed! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_FAILED = "Runtime Layout Update Failed!";
	private static final String INITIALIZING_HIDMANAGER = "Initializing HIDManager ...";
	private static final String INITIALIZING_NODEMANAGER = "Initializing NodeManager ...";
	private static final String INITIALIZING_SECTIONMANAGER = "Initializing SectionManager ...";
	private static final String INITIALIZING_COLLISIONMANAGER = "Initializing CollisionManager ...";
	private static final String INITIALIZING_CARRIERLOCMANAGER = "Initializing CarrierLocManager ...";
	private static final String INITIALIZING_ZONECONTROLMANAGER = "Initializing ZoneControlManager ...";
	private static final String INITIALIZING_TOURMANAGER = "Initializing TourManager ...";
	private static final String INITIALIZING_USERREQUESTMANAGER = "Initializing UserRequestManager ...";
	// 2013.08.30 by KYK
	private static final String INITIALIZING_MATERIALCONTROLMANAGER = "Initializing MaterialControlManager ...";

	private static final String RUNTIMEUPDATEHISTORY = "RuntimeUpdateHistory";
	private static Logger runtimeUpdateHistoryLog = Logger.getLogger(RUNTIMEUPDATEHISTORY);
	
	/**
	 * Constructor of LongRunManager class.
	 */
	public LongRunManager() {
		initializeManager();
		longRun = new LongRun();
		new LogManager(LONGRUN);
	}
	
	/**
	 * 
	 */
	private void initializeManager() {
		DBAccessManager dbAccessManager = new DBAccessManager();

		ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, new DBAccessManager(), true, true, 500);
		userRequestManager = UserRequestManager.getInstance(UserRequest.class, dbAccessManager, true, false, 300);
		tourManager = TourManager.getInstance(Tour.class, dbAccessManager, true, false, 300);
		dockingStationManager = DockingStationManager.getInstance(DockingStation.class, dbAccessManager, true, true, 300);
		
		VehicleManager.getInstance(Vehicle.class, dbAccessManager, true, false, 500);
		LocalGroupInfoManager.getInstance(LocalGroupInfo.class, dbAccessManager, false, false, 700);
		TrCmdManager.getInstance(TrCmd.class, dbAccessManager, true, false, 200);
		
		nodeManager = NodeManager.getInstance(Node.class, dbAccessManager, true, true, 500);
		nodeManager.updateCarrierLocMaterialOfBay();
		sectionManager = SectionManager.getInstance(Section.class, dbAccessManager, true, false, 500);
		collisionManager = CollisionManager.getInstance(Collision.class, dbAccessManager, true, false, 500);
		zoneControlManager = ZoneControlManager.getInstance(ZoneControl.class, dbAccessManager, true, false, 500);

		carrierLocManager = CarrierLocManager.getInstance(CarrierLoc.class, dbAccessManager, true, true, 500);
		hidManager = HIDManager.getInstance(Hid.class, dbAccessManager, true, true, 300);
		alarmManager = AlarmManager.getInstance(Alarm.class, dbAccessManager, true, true, 300);
		eventHistoryManager = EventHistoryManager.getInstance(EventHistory.class, dbAccessManager, false, false, 500);
		// 2013.08.30 by KYK
		materialControlManager = MaterialControlManager.getInstance(MaterialControl.class, dbAccessManager, true, false, 0);
	}
	
	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = 1000;
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
			// Ownership을 가지고 있는 경우에 한해 작업 할당을 진행.
			longRun.mainProcessing();
			
		} else if (serviceState == MODULE_STATE.OUTOFSERVICE) {
			if (deactivationCheckCount > 9) {
				longRun.traceLongRunMain("Deactivated.");
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
		if (this.requestedSerivceState == MODULE_STATE.REQOUTOFSERVICE) {
			changeServiceState(MODULE_STATE.OUTOFSERVICE);
			longRun.traceLongRunMain("Deactivated!");
		} else if (this.serviceState != MODULE_STATE.INSERVICE && 
				this.requestedSerivceState == MODULE_STATE.REQINSERVICE) {
			changeServiceState(MODULE_STATE.INSERVICE);
			longRun.traceLongRunMain("Activated!");
		}
		
		if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
			if (++outOfServiceCheckCount >= 5) {
				longRun.traceLongRunMain("      [OUTOFSERVICE]");
				outOfServiceCheckCount = 0;
			}
		}
	}
	
	private void changeServiceState(MODULE_STATE state) {
		this.requestedSerivceState = state;
		this.serviceState = state;
	}
	
	public void traceLongrunMain(String message) {
		longRun.traceLongRunMain(message);
	}
	
	private void manageRuntimeUpdate() {
		assert ocsInfoManager != null;
		
		// 2011.11.04 by PMM
		if (ocsInfoManager.getLongRunUpdate() != RUNTIME_UPDATE.NO) {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				if (ocsInfoManager.getLongRunUpdate() == RUNTIME_UPDATE.YES) {
					registerAlarmText(RUNTIMEUPDATE_START_INSERVICE);
					traceRuntimeUpdate(RUNTIMEUPDATE_START_INSERVICE);
					
					// 2011.11.21 by PMM
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_START_INSERVICE, 
							"", "", EVENTHISTORY_REMOTEID.LONGRUN, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
					
					// Manager Update!
					updateManager();
					unregisterAlarmText(RUNTIMEUPDATE_START_INSERVICE);
					
					traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_INSERVICE);
					
					// 2011.11.21 by PMM
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_COMPLETED_INSERVICE, 
							"", "", EVENTHISTORY_REMOTEID.LONGRUN, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
					
					ocsInfoManager.setLongRunUpdate(RUNTIME_UPDATE.DONE);
				} else if (ocsInfoManager.getLongRunUpdate() != RUNTIME_UPDATE.DONE) {
					ocsInfoManager.setLongRunUpdate(RUNTIME_UPDATE.NO);
				}
			} else if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
				if (ocsInfoManager.getLongRunUpdate() == RUNTIME_UPDATE.DONE) {
					registerAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
					traceRuntimeUpdate(RUNTIMEUPDATE_START_OUTOFSERVICE);
					
					// 2011.11.21 by PMM
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_START_OUTOFSERVICE, 
							"", "", EVENTHISTORY_REMOTEID.LONGRUN, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
					
					updateManager();
					unregisterAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
					traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE);
					
					// 2011.11.21 by PMM
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE, 
							"", "", EVENTHISTORY_REMOTEID.LONGRUN, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
					
					ocsInfoManager.setLongRunUpdate(RUNTIME_UPDATE.NO);
				}
			}
		}
	}
	
	private void updateManager() {
		assert hidManager != null;
		assert nodeManager != null;
		assert sectionManager != null;
		assert collisionManager != null;
		assert carrierLocManager != null;
		assert zoneControlManager != null;
		
		try {
			registerAlarmText(INITIALIZING_HIDMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_HIDMANAGER);
			hidManager.initializeFromDB();		// data reset.
			unregisterAlarmText(INITIALIZING_HIDMANAGER);
			
			registerAlarmText(INITIALIZING_NODEMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_NODEMANAGER);
			nodeManager.initializeFromDB();
			unregisterAlarmText(INITIALIZING_NODEMANAGER);
			
			registerAlarmText(INITIALIZING_SECTIONMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_SECTIONMANAGER);
			sectionManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_SECTIONMANAGER);
			
			registerAlarmText(INITIALIZING_COLLISIONMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_COLLISIONMANAGER);
			collisionManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_COLLISIONMANAGER);
			
			registerAlarmText(INITIALIZING_CARRIERLOCMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_CARRIERLOCMANAGER);
			carrierLocManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_CARRIERLOCMANAGER);
			
			registerAlarmText(INITIALIZING_ZONECONTROLMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_ZONECONTROLMANAGER);
			zoneControlManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_ZONECONTROLMANAGER);
			
			registerAlarmText(INITIALIZING_TOURMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_TOURMANAGER);
			tourManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_TOURMANAGER);
			
			registerAlarmText(INITIALIZING_USERREQUESTMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_USERREQUESTMANAGER);
			userRequestManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_USERREQUESTMANAGER);
			
			// 2013.08.30 by KYK
			registerAlarmText(INITIALIZING_MATERIALCONTROLMANAGER);
			traceRuntimeUpdate("   " + INITIALIZING_MATERIALCONTROLMANAGER);
			materialControlManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_MATERIALCONTROLMANAGER);

			longRun.initializeForRuntimeUpdate();
			
		} catch (Exception e) {
			traceRuntimeUpdate(RUNTIMEUPDATE_FAILED);
			longRun.traceLongRunException("updateManager()", e);
		}
	}
	
	// 2011.11.04 by PMM
	private void registerAlarmText(String alarmText) {
		alarmManager.registerAlarmText(LONGRUN, alarmText);
	}
	
	private void unregisterAlarmText(String alarmText) {
		alarmManager.unregisterAlarmText(LONGRUN, alarmText);
	}
	
	// 2011.11.21 by PMM
	private void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
		eventHistoryManager.addEventHistoryToRegisterList(eventHistory, duplicateCheck);
	}
	
	private void traceRuntimeUpdate(String message) {
		runtimeUpdateHistoryLog.info(message);
	}
}
