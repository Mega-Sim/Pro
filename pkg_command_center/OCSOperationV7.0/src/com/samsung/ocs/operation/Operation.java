package com.samsung.ocs.operation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.config.CarrierTypeConfig;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.common.constant.OcsConstant.CARRIERLOC_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.DEADLOCK_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.OcsConstant.OPERAION_CONTROL_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant;
import com.samsung.ocs.common.constant.OcsInfoConstant.LOCALGROUP_CLEAROPTION;
import com.samsung.ocs.common.constant.OcsInfoConstant.NEARBY_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.TSC_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.VEHICLECOMM_TYPE;
import com.samsung.ocs.common.constant.TrCmdConstant;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.common.message.Message;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.AutoRetryGroupInfoManager;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.DetourControlManager;
import com.samsung.ocs.manager.impl.EventHistoryManager;
import com.samsung.ocs.manager.impl.IBSEMReportManager;
import com.samsung.ocs.manager.impl.LocalGroupInfoManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.RailDownControlManager;
import com.samsung.ocs.manager.impl.STBCarrierLocManager;
import com.samsung.ocs.manager.impl.StationManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.TrCompletionHistoryManager;
import com.samsung.ocs.manager.impl.UserDefinedPathManager;
import com.samsung.ocs.manager.impl.UserRequestManager;
import com.samsung.ocs.manager.impl.VehicleErrorHistoryManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.ZoneControlManager;
import com.samsung.ocs.manager.impl.model.AutoRetryGroupInfo;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.DetourControl;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.STBCarrierLoc;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.TrCompletionHistory;
import com.samsung.ocs.manager.impl.model.UserDefinedPath;
import com.samsung.ocs.manager.impl.model.VehicleData;
import com.samsung.ocs.manager.impl.model.VehicleErrorHistory;
import com.samsung.ocs.operation.comm.VehicleComm;
import com.samsung.ocs.operation.comm.VehicleCommV1;
import com.samsung.ocs.operation.comm.VehicleCommV7;
import com.samsung.ocs.operation.constant.MessageItem;
import com.samsung.ocs.operation.constant.MessageItem.EVENT_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant;
import com.samsung.ocs.operation.constant.OperationConstant.COMMAND_STATE;
import com.samsung.ocs.operation.constant.OperationConstant.COMMAND_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant.JOB_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant.OPERATION_MODE;
import com.samsung.ocs.operation.constant.ResultCode;
import com.samsung.ocs.operation.mode.GoMode;
import com.samsung.ocs.operation.mode.IdleMode;
import com.samsung.ocs.operation.mode.OperationMode;
import com.samsung.ocs.operation.mode.SleepMode;
import com.samsung.ocs.operation.mode.WorkMode;
import com.samsung.ocs.operation.model.VehicleCommCommand;
import com.samsung.ocs.operation.model.VehicleCommData;
import com.samsung.ocs.route.search.PathSearch;
import com.samsung.ocs.route.search.YieldSearch;

/**
 * Operation Class, OCS 3.0 for Unified FAB
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

public class Operation extends AbstractOcsThread {
	private PathSearch pathSearch = null;
	private YieldSearch yieldSearch = null;
	private TrCmdManager trCmdManager = null;
	private VehicleManager vehicleManager = null;
	private OCSInfoManager ocsInfoManager = null;
	private CarrierLocManager carrierLocManager = null;
	private STBCarrierLocManager stbCarrierLocManager = null;
	private NodeManager nodeManager = null;
	private AlarmManager alarmManager = null;
	private RailDownControlManager railDownControlManager = null;
	private EventHistoryManager eventHistoryManager = null;
	private LocalGroupInfoManager localGroupInfoManager = null;
	private TrCompletionHistoryManager trCompletionHistoryManager = null;
	private ZoneControlManager zoneControlManager = null;
	private VehicleErrorHistoryManager vehicleErrorHistoryManager = null;
	private IBSEMReportManager ibsemReportManager = null;
	private UserDefinedPathManager userDefinedPathManager = null;
	private AutoRetryGroupInfoManager autoRetryControlManager = null;
	private StationManager stationManager = null;
	private UserRequestManager userRequestManager = null;
	
	private NEARBY_TYPE nearbyType = null;
	
	private VehicleCommCommand vehicleCommCommand = null;
	private VehicleData vehicleData = null;
	private VehicleComm vehicleComm = null;
	private TrCmd trCmd = null;

	private COMMAND_STATE cmdState;
	private long lastCommandSentTime;
	private long lastDifferentCommandSentTime;
	private String lastDifferentCommand;

	private static final String OPERATION_TRACE = "OperationDebug";
	private static final String OPERATION_DELAY_TRACE = "OperationDelay";
	private static final String OPERATION_EXCEPTION_TRACE = "OperationException";
	private static final String HOSTREPORT_TRACE = "HostReport";
	private static final String UPDATE_REQUESTEDCMD_TRACE = "UpdateRequestedCmd";
	private static final String PROCESS_TRCMD_TRACE = "ProcessTrCmd";
	private static final String STB_TRACE = "STB";
	private static final String RFREAD_ERROR_TRACE = "RFReadError";
	private static final String VEHICLE_TRAFFIC_TRACE = "VehicleTraffic";
	private static final String VEHICLEERRORHISTORY_TRACE = "VehicleErrorHistory";
	private static final String FORMAT_TRCOMPLETIONHISTORY_TRACE = "TrCompletionHistoryLog";
	private static final String FORMAT_VEHICLEERRORHISTORY_TRACE = "VehicleErrorHistoryLog";
	private static final String FORMAT_EVENTHISTORY_TRACE = "EventHistoryLog";
	private static final String STB_REPORT_DATA_TRACE = "STBReportData";
	
	private static final String ALARM_SET = "ALARM_SET";
	private static final String ALARM_RESET = "ALARM_RESET";
	private static final String NO_CARRIERLOC = "No_CarrierLoc";
	private static final String NO_ERROR = "No_Error";
	private static final String AUTO_ERROR = "AUTO ERROR";

	private static final String NO_TRCMD = "NoTrCmd";
	private OperationMode activeOperationMode = null;
	private OperationMode idleMode = null;
	private OperationMode goMode = null;
	private OperationMode workMode = null;
	private OperationMode sleepMode = null;

//	private long socketReconnectionTimeout = 20000;
	private String lastPathDriveResult = "init";
	private boolean isValidNodeUpdated;
	
	private boolean isIDResetCommandSent;
	private boolean isPatrolCancelCommandSent;
	
	private boolean wasLoadPathSearchFailed = false;
	private long lastLoadPathSearchFailedTime = 0;
	private long firstLoadPathSearchFailedTime = 0;
	private long repathSearchHoldTimeout = 30000;
	
	private String currMode = "";
	private long startedTime = 0;
	private long elapsedTime = 0;
	private long missedCarrierCheckSleep = 1000;
	
	// Operation Start Option
	private boolean isSystemPaused = false;
	private boolean isAllOperationReady = false;
	private boolean isFailoverCompleted = false;
	// Operation Control Mode : INIT(초기 구동시), READY(초기 구동후 Vehicle과 통신 완료), START(통신 완료 후 Vehicle 명령 완료)
	private OPERAION_CONTROL_STATE operationControlState = OPERAION_CONTROL_STATE.INIT;
	
	// Setup Parameters
	private boolean isNearByDrive = false;
	private boolean isEmulatorMode = false;
	private boolean isIBSEMUsed = true;
	private boolean isBidirectionalSTB = true;
	private boolean isRailDownCheckUsed = true;
	private boolean isResendCmdForAbnormalReply = true;
	private VEHICLECOMM_TYPE vehicleCommType = VEHICLECOMM_TYPE.VEHICLECOMM_CHAR;
	
	// Setup Parameter -> Operational Parameter
	private boolean isNearByNormalDrive = false;
	private boolean isSTBCUsed = true;
	private boolean isAutoMismatchRecoveryMode = false;
	private boolean isLocalOHTUsed = false;
	private boolean isSteeringReadyUsed = false;
	private boolean isFormattedLogUsed = false;
	private boolean isUserPassThroughUsed = false;
	private boolean isYieldSearchUsed = true;
	private boolean isVehicleTrafficLogUsed = false;
	private boolean isAutoRetryUsed = false;
	private boolean isAbnormalStateChanged = false;
	private boolean isGoModeCarrierStatusCheckUsed = false;
	private boolean isDynamicRoutingUsed = true;
	private boolean isMissedCarrierCheckUsed = true;
	private boolean isUnloadErrorReportUsed = true;
	// 2015.05.01 by KYK [Commfail Report]
	private boolean isCommfailAlarmReported = false;
	private boolean isCommfailAlarmReportUsed = false;
	
	private boolean isCarrierTypeMismatchUsed = true; // default:true
	
	private boolean isStageSourceDupCancelUsage = false; // 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel

	private int hoistSpeedLevel = 100;
	private int shiftSpeedLevel = 100;
	
	private LOCALGROUP_CLEAROPTION localOHTClearOption = LOCALGROUP_CLEAROPTION.UNLOADING_VHL;
	
	private String rfReadDevice = OcsInfoConstant.DEFAULT_RFREAD_DEVICE;
	private String mismatchUnloadAppliedPort = OcsInfoConstant.DEFAULT_MISMATCH_UNLOAD_APPLIED_PORT;
	
	private double driveLimitTime = 7;
	private double abortCheckTime = 60;
	private int delayLimitOfOperation = 100;
	private int yieldRequestLimitTime = 7;
	private int goModeCheckTime = 60;
	private int workModeCheckTime = 60;
	private int vehicleCountPerHid = 20;
//	private int commFailCheckTime = 5;
	private int driveFailLimitTime = 120000;
	private int driveMinNodeCount = 3;
	private int lastSentEstopType = 0;
	private int goModeVehicleDetectedCheckTime = 300;
	private int goModeVehicleDetectedResetTimeout = 600;
	private long vibrationMonitoringTimeout = 10800000L;	// 3hrs
	private long dynamicRoutingHoldTimeout = 50000;
	
	// failOver
	private MODULE_STATE serviceState = MODULE_STATE.OUTOFSERVICE;
	private MODULE_STATE requestedServiceState = MODULE_STATE.OUTOFSERVICE;
	
	private SimpleDateFormat sdf;
	private SimpleDateFormat sdf2;
	
	// 2011.11.30 by PMM
	private ArrayList<String> routedIntersectionNodeList;
	
	private HashSet<String> comebackZoneAllowedSet;
	
	private static final String PATROL = "Patrol";
	
	private Node yieldCancelledNode = null;
	
	private String prevResetTargetNode = "";
	
	/**
	 * Constructor of Operation class.
	 */
	public Operation(VehicleData vehicle) {
		// 2014.10.23 by MYM : Thread Name을 설정
		setName(this.getClass().getSimpleName() + "_" + vehicle.getVehicleId());
			
		initialize();
		this.vehicleData = vehicle;
		sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		sdf2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		
		// 2011.11.30 by PMM
		routedIntersectionNodeList = new ArrayList<String>();
	}

	@Override
	public String getThreadId() {
		StringBuilder message = new StringBuilder();
		message.append(this.getClass().getName());
		if (vehicleData != null) {
			message.append(":").append(vehicleData.getVehicleId());
		}
		if (trCmd != null) {
			message.append("-").append(trCmd.getTrCmdId());
		} else {
			message.append("-NoTrCmd");
		}
		return message.toString();
	}
	
	/**
	 * Initialize Operation Instance
	 */
	@Override
	protected void initialize() {
		interval = 200;
		cmdState = COMMAND_STATE.READY;
		isValidNodeUpdated = false;
		isIDResetCommandSent = false;
		isPatrolCancelCommandSent = false;
		
		lastCommandSentTime = System.currentTimeMillis();
		lastDifferentCommandSentTime = System.currentTimeMillis();
		lastDifferentCommand = "";
		
		// 2014.11.13 by zzang9un : operation thread 예상 수행 시간 최대값 설정(10초)
		elapsedTimeLimit = 10000;
	}

	/**
	 * Main Processing Method
	 */
	@Override
	protected void mainProcessing() {
		operationProcess();
	}

	@Override
	protected void stopProcessing() {
	}
	
	/**
	 * Start Operation Thread
	 * 
	 * @param reportInstalledEvent
	 * @param state
	 * @param vehicleSocketPort
	 */
	public void startOperation(OperationManager manager, boolean reportInstalledEvent, int vehicleSocketPort) {
		// 주요 Manager 설정
		this.vehicleManager = VehicleManager.getInstance(null, null, false, false, 0);
		this.trCmdManager = TrCmdManager.getInstance(null, null, false, false, 0);
		this.carrierLocManager = CarrierLocManager.getInstance(null, null, false, false, 0);
		this.stbCarrierLocManager = STBCarrierLocManager.getInstance(null, null, false, false, 0);
		this.ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		this.nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		this.alarmManager = AlarmManager.getInstance(null, null, true, false, 0);
		this.eventHistoryManager = EventHistoryManager.getInstance(null, null, false, true, 0);
		this.localGroupInfoManager = LocalGroupInfoManager.getInstance(null, null, true, true, 0);
		this.trCompletionHistoryManager = TrCompletionHistoryManager.getInstance(null, null, false, false, 0);
		this.zoneControlManager = ZoneControlManager.getInstance(null, null, false, false, 0);
//		this.vehicleErrorManager = VehicleErrorManager.getInstance(null, null, false, false, 0);
		this.vehicleErrorHistoryManager = VehicleErrorHistoryManager.getInstance(null, null, false, false, 0);
		this.ibsemReportManager = IBSEMReportManager.getInstance(null, null, false, false, 0);
		this.userDefinedPathManager = UserDefinedPathManager.getInstance(null, null, false, false, 0);
		
		this.comebackZoneAllowedSet = zoneControlManager.getComebackZoneAllowedSet();
		
		// 2012.05.16 by MYM : Rail-Down
		this.railDownControlManager = RailDownControlManager.getInstance(null, null, false, false, 0);
		// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
		this.autoRetryControlManager = AutoRetryGroupInfoManager.getInstance(null, null, false, false, 0);
		// 2013.02.15 by KYK
		this.stationManager = StationManager.getInstance(null, null, false, false, 0);

		this.userRequestManager = UserRequestManager.getInstance(null, null, false, false, 0);
		
		// Service State 설정
//		this.serviceState = state;
		
		this.requestedServiceState = manager.getServiceState();

		// TSC 및 IBSEM 설정
		
		// Setup Parameters 설정
		isNearByDrive = ocsInfoManager.isNearByDrive();
		isEmulatorMode = ocsInfoManager.isEmulatorMode();
		isIBSEMUsed = ocsInfoManager.isIBSEMUsed();
		isBidirectionalSTB = ocsInfoManager.isBidirectionalSTB();
		isRailDownCheckUsed = ocsInfoManager.isRailDownCheckUsed();
		isResendCmdForAbnormalReply = ocsInfoManager.isResendCmdForAbnormalReply();
		// 2013.04.05 by KYK
		nearbyType = ocsInfoManager.getNearbyType();
		vehicleCommType = ocsInfoManager.getVehicleCommType();
		
		if (isAlarmRegistered()) {
			// 2012.04.17 by PMM
			// VHL Disabled -> Enabled 시 알람 정리안되는 경우를 위한 안전조치 추가.
			unregisterAllAlarm();
		}

		// 모드별 Instance 생성 및 초기 ActiveMode 설정(Default: Idle 모드)
		idleMode = new IdleMode(this);
		goMode = new GoMode(this);
		workMode = new WorkMode(this);
		sleepMode = new SleepMode(this);
		activeOperationMode = idleMode;

		// PathSearch, YieldSearch 생성 및 DB의 Vehicle 위치 기준으로 Vehicle Drive 초기화 설정
		vehicleData.setNearByDrive(isNearByDrive);
		// 2014.10.22 by MYM : Block 점유 정보 DB 업데이트를 파라미터화
		vehicleData.setBlockPreemptionUpdateUsed(ocsInfoManager.isBlockPreemptionUpdateUsed());
		// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//		pathSearch = new PathSearch(isNearByDrive);
//		yieldSearch = new YieldSearch();
		pathSearch = new PathSearch(isNearByDrive, manager.getAbnormalVehiclesOnCollisionMap());
		yieldSearch = new YieldSearch(manager.getAbnormalVehiclesOnCollisionMap());
//		pathSearch.initializeVehiclePath(vehicleData);
		
		// 작업정보를 가져오기
//		initializeVehicleAssignData();

		// VehicleComm 관련 생성 및 설정, VehicleComm 쓰레드 실행
		// 2013.03.18 by MYM : 통산 방식별로 VehicleComm 생성
		vehicleCommCommand = new VehicleCommCommand();
		// 2013.08.09 by KYK : V1,V7 -> CHAR,BYTE 로 변경
//		if (ocsInfoManager.getVehicleCommType() == VEHICLECOMM_TYPE.VEHICLECOMM_V1) {
		if (vehicleCommType == VEHICLECOMM_TYPE.VEHICLECOMM_CHAR) {
			vehicleComm = new VehicleCommV1(ocsInfoManager.isCarrierTypeUsage());
//		} else if (ocsInfoManager.getVehicleCommType() == VEHICLECOMM_TYPE.VEHICLECOMM_V7) {
		} else if (vehicleCommType == VEHICLECOMM_TYPE.VEHICLECOMM_BYTE) {
			vehicleComm = new VehicleCommV7();
		} else {
			vehicleComm = new VehicleCommV1(ocsInfoManager.isCarrierTypeUsage());
		}
		vehicleComm.setTargetInfo(vehicleData.getVehicleId(), vehicleData.getIpAddress(), getPortForTargetInfo(vehicleData.getVehicleId(), vehicleSocketPort));
//		vehicleComm.setBidirectionalSTB(ocsInfoManager.isBidirectionalSTB());
		vehicleComm.setBidirectionalSTB(isBidirectionalSTB);
		// 2014.06.21 by MYM : [Commfail 체크 개선] : 통신 체크 관련 파라미터 업데이트 정리 
		vehicleComm.setSocketReconnectionTimeout(ocsInfoManager.getSocketReconnectionTimeout());
		vehicleComm.setSocketCloseCheckTime(ocsInfoManager.getSocketCloseCheckTime());
		vehicleComm.setCommFailCheckTime(ocsInfoManager.getCommFailCheckTime());
		vehicleComm.startVehicleComm();

		// 마지막 명령을 현재 시간으로 설정
		lastCommandSentTime = System.currentTimeMillis();
		
		lastDifferentCommandSentTime = System.currentTimeMillis();
		lastDifferentCommand = "";

		// Operation 쓰레드 실행
		this.start();

		if (reportInstalledEvent) {
			// 2015.07.01 by MYM : Enabled시 VehicleInstalled 이벤트 별도 보고 추가
			// 배경 : Enabled시 serviceState가 OUTOFSTATE라서 보고하지 못함
//			sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_INSTALLED, 0);
			sendS6F11_VehicleInstalled(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_INSTALLED, requestedServiceState);
		}
	}
	
	/**
	 * Stop Operation Thread
	 */
	public void stopOperation() {
		// Vehicle과 통신 연결 해제
		vehicleComm.stopVehicleComm();
		// operationProcess 서비스 중지
		this.stopThread();
		
		// 1. Vehicle 초기화 (CurrNode, StopNode, TargetNode, ErrorCode, Reason, RequestedType, RequestedData, RequestedCost)
		resetVehicleData();
		
		if (serviceState != MODULE_STATE.INSERVICE) {
			// 2012.10.18 by PMM
			// Secondary에서 잘못 Alarm을 등록하는 케이스 발생함.
			return;
		}

		// 2. LocalGroupInfo 해제
		if (isLocalOHTUsed) {
			clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION.REMOVE_VHL);
		}
		// 2. Unload 전 : TrCmd 할당해제(Vehicle 칼럼 Reset) - VehicleUnassigned
		// Unload 후 : 작업삭제 및 비정상 완료 보고 - VehicleUnassigned, CarrierRemoved,
		// TransferCompleted(Result:1)
		if (trCmd != null) {
			switch (trCmd.getRemoteCmd()) {
				case STAGE: {
					cancelStageCommand(EVENTHISTORY_REASON.VEHICLE_REMOVE);
					traceOperation("Stage Command Canceled by Vehicle Removal.");
					break;
				}
				case MAPMAKE: {
					cancelMapMakeCommand(EVENTHISTORY_REASON.VEHICLE_REMOVE);
					traceOperation("MapMake Command Canceled by Vehicle Removal.");
					break;
				}
				case PATROL: {
					cancelPatrolCommand(EVENTHISTORY_REASON.VEHICLE_REMOVE);
					traceOperation("Patrol Command Canceled by Vehicle Removal.");
					break;
				}
				case VIBRATION: {
					cancelVibrationCommand(EVENTHISTORY_REASON.VEHICLE_REMOVE);
					traceOperation("Vibration Command Canceled by Vehicle Removal.");
					break;
				}
				case PREMOVE:	// 2022.05.05 dahye : 비정상상황에 대한 PREMOVE 반송 처리 필요
				case TRANSFER: {
					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED) {						
						cancelAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_REMOVE, true);
						resetTargetNode("stopOperation()");
					} else {
						cancelLoadCommand();
					}
					break;
				}
				// 2013.01.08 by MYM : Abort된 TrCmd를 가지고 있는 Vehicle을 Remove시 작업삭제 처리 추가
				case ABORT:
					cancelLoadCommand();
					break;
				default: {
					// Operation#001
					traceOperationException("Abnormal Case: Operation#001");
				}
			}
		}

		// 3. AlarmReset
		if (isAlarmRegistered()) {
			unregisterAllAlarm();
		}
		
		if (vehicleData.isVehicleError()) {
			clearAlarmReport(OcsAlarmConstant.NO_ALARM);
			traceOperation("Send ClearAlarmReport...");
			vehicleErrorHistoryManager.addVehicleToResetErrorList(new VehicleErrorHistory(vehicleData.getVehicleId(), getCurrDBTimeStr()));
		}
		
		// 2011.10.29 by PMM
		// VHL Disable 시킬 때 Carrier 여부 확인 후 알람 등록.
		if (vehicleData.isCarrierExist()) {
			traceOperation(getJournalOfVehicle());
			traceOperation(getJournalOfTrCmd());
			registerAlarm(OcsAlarmConstant.CARRIER_REMAINEDON_REMOVEDVHL);
		}
		
		// 2015.02.10 by MYM : 장애 지역 우회 기능
		vehicleData.releaseAbnormalSection();

		// 4. Vehicle Remove 보고
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_REMOVED, 0);
	}

	/**
	 * Reset VehicleData
	 */
	private void resetVehicleData() {
		// 2013.02.15 by KYK
//		vehicleData.setStopNode(vehicleData.getCurrNode());
//		vehicleData.setTargetNode(vehicleData.getCurrNode());
		vehicleData.setStop(vehicleData.getCurrNode(), vehicleData.getCurrStation());
		vehicleData.setTarget(vehicleData.getCurrNode(), vehicleData.getCurrStation());
		vehicleData.setErrorCode(0);
		vehicleData.setReason("");
		vehicleData.setRequestedType(REQUESTEDTYPE.NULL);
		vehicleData.setRequestedData("");
		vehicleData.setRequestedCost(0);
		
		// 별도 Alarm 정리함.
//		vehicleData.setAlarmCode(OcsAlarmConstant.NO_ALARM);
		vehicleData.clear(isNearByDrive);
		
		addVehicleToUpdateList();
		
		// 2014.03.07 by MYM : [Stage Locate 기능] Vehicle Request 정보는 별도로 DB 업데이트.		
		resetVehicleRequestedInfo();
	}

	/**
	 * Cancel STAGE Command
	 * 
	 * @param reason
	 */
	private void cancelStageCommand(EVENTHISTORY_REASON reason) {
		// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
		resetTargetNode(reason.toConstString());
		
		// VHL:OHT201(AA), CMDID:234423, CARRIERID:OYB0123, SRCLOC:EFB01_1233, DESTLOC:EFB03_2233
		StringBuilder message = new StringBuilder();
		message.append("Vehicle:").append(vehicleData.getVehicleId());
		message.append(", TrCmdId:").append(trCmd.getTrCmdId());
		message.append(", CarrierId:").append(trCmd.getCarrierId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		registerEventHistory(new EventHistory(
				EVENTHISTORY_NAME.CURRENT_STAGE_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "",
				message.toString(), "", "", EVENTHISTORY_REMOTEID.OPERATION, "",
				reason), false);

		if (trCmd.getDeletedTime() != null && trCmd.getDeletedTime().length() < 2) {
			trCmd.setDeletedTime(getCurrDBTimeStr());
		}
		registerTrCompletionHistory(REQUESTEDTYPE.STAGECANCEL.toConstString());
		deleteStageCmdFromDB();
		traceOperation("Stage Cancel by " + reason);
	}

	/**
	 * Cancel MAPMAKE Command
	 * 
	 * @param reason
	 */
	private void cancelMapMakeCommand(EVENTHISTORY_REASON reason) {
		assert (trCmd != null);
		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.MAPMAKE);

		if (trCmd.getDeletedTime() != null && trCmd.getDeletedTime().length() < 2) {
			trCmd.setDeletedTime(getCurrDBTimeStr());
		}
		registerTrCompletionHistory(REQUESTEDTYPE.MAPMAKE.toConstString());
		deleteTrCmdFromDB();
		traceOperation("MapMake Cancel by " + reason);
	}

	/**
	 * Cancel PATROL Command
	 * 
	 * @param reason
	 */
	private void cancelPatrolCommand(EVENTHISTORY_REASON reason) {
		assert (trCmd != null);
		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL);

//		if (EVENTHISTORY_REASON.UNLOAD_PATHSEARCH_FAIL != reason) {
		if (trCmd.getDetailState() != TRCMD_DETAILSTATE.NOT_ASSIGNED && trCmd.getDetailState() != TRCMD_DETAILSTATE.PATROL_ASSIGNED) {
			sendPatrolCancelCommand();
		} else { 
			// 배경 : 청소 시작전에 not assigned / unload_assigned일 경우는 TrCmd만 정리
			trCmd.setDeletedTime(getCurrDBTimeStr());
			trCmd.setDetailState(TRCMD_DETAILSTATE.PATROL_CANCELED);
			trCmd.setCarrierLoc(trCmd.getDestLoc());
			addTrCmdToStateUpdateList();
			// registerTrCompletionHistory(REQUESTEDTYPE.PATROL.toConstString());

			resetTargetNode("cancelPatrolCommand()");
			deleteTrCmdFromDB();
		}
		vehicleData.setRepathSearchNeededByPatrolVHL(true);

		traceOperation("Patrol Cancel by " + reason);
	}
	
	/**
	 * Cancel VIBRATION Command
	 * 
	 * @param reason
	 */
	public void cancelVibrationCommand(EVENTHISTORY_REASON reason) {
		assert (trCmd != null);
		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION);

		if (trCmd.getDeletedTime() != null && trCmd.getDeletedTime().length() < 2) {
			trCmd.setDeletedTime(getCurrDBTimeStr());
		}
		
		StringBuilder message = new StringBuilder();
		message.append("Vehicle:").append(vehicleData.getVehicleId());
		message.append(", TrCmdId:").append(trCmd.getTrCmdId());
		message.append(", CarrierId:").append(trCmd.getCarrierId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		registerEventHistory(new EventHistory(EVENTHISTORY_NAME.CURRENT_VIBRATION_DELETE, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
				"", "", EVENTHISTORY_REMOTEID.OPERATION, "", reason), false);
		
		registerTrCompletionHistory(REQUESTEDTYPE.VIBRATION.toConstString());
		deleteTrCmdFromDB();
		traceOperation("Vibration Cancel by " + reason);
	}

//	private void cancelUnloadCommand() {
//		assert (trCmd != null);
//		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER);
//		assert (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
//
//		trCmd.setState(TRCMD_STATE.CMD_QUEUED);
//		trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
//		addTrCmdToStateUpdateList();
//
//		trCmd.setVehicle("");
//		addTrCmdToVehicleUpdateList();
//
//		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
//		resetTargetNode();
//		resetVehicleRequestedInfo();
//
//		// 2010.08.05. by MYM(EVENT HISTORY 기능) - Current 작업 할당 해제시 Event History에 기록
//		// VHL:OHT201, CMDID:234423, CARRIERID:OYB0123, SRCLOC:EFB01_1233, DESTLOC:EFB03_2233
//		StringBuilder message = new StringBuilder();
//		message.append("Vehicle:").append(vehicleData.getVehicleId());
//		if (trCmd != null) {
//			message.append(", TrCmdId:").append(trCmd.getTrCmdId());
//			message.append(", CarrierId:").append(trCmd.getCarrierId());
//			message.append(", SourceLoc:").append(trCmd.getSourceLoc());
//			message.append(", DestLoc:").append(trCmd.getDestLoc());
//		}
//		registerEventHistory(
//				new EventHistory(EVENTHISTORY_NAME.CURRENT_JOB_CANCEL,
//						EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), "", "",
//						EVENTHISTORY_REMOTEID.OPERATION, "",
//						EVENTHISTORY_REASON.VEHICLE_REMOVE), false);
//	}

	/**
	 * Cancel LOAD Command
	 */
	private void cancelLoadCommand() {
		assert (trCmd != null);
		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER);
		assert (trCmd.getDetailState() != TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);

		trCmd.setLastAbortedTime(System.currentTimeMillis());
		
		// 2012.01.28 by PMM
		trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
		trCmd.setState(TRCMD_STATE.CMD_ABORTED);
		addTrCmdToStateUpdateList();

		if (trCmd.getDeletedTime() != null && trCmd.getDeletedTime().length() < 2) {
			trCmd.setDeletedTime(getCurrDBTimeStr());
		}
		registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());

		if (vehicleData.getVehicleLoc().equals(trCmd.getCarrierLoc())) {
			trCmd.setCarrierLoc(trCmd.getDestLoc());
			addTrCmdToStateUpdateList();
			sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
		}
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
		// 2012.11.30 by KYK : ResultCode 세분화
//		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_TRDELETED_BY_VEHICLEREMOVE);
		
		// 2011.11.07 by PMM
//		traceOperation("TrCmd is deleted because of OHT Remove Problem: <<CommandID:"
//				+ trCmd.getTrCmdId() + ", CarrierId:" + trCmd.getCarrierId() + ">>");
		StringBuilder message = new StringBuilder();
		message.append("TrCmd is deleted because of OHT Remove Problem: <<CommandID:");
		message.append(trCmd.getTrCmdId());
		message.append(", CarrierId:");
		message.append(trCmd.getCarrierId());
		message.append(">>");
		traceOperation(message.toString());

		deleteTrCmdFromDB();

		// 2012.08.28 by PMM
		// U1에서 Loading 중 Next작업 할당 받았으나 CommFail로 처리 못하는 중, VHL LineOut됨.
		cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_REMOVE);
	}

	/**
	 * Cancel Assigned TrCmd
	 */
	public void cancelAssignedTrCmd(EVENTHISTORY_REASON reason, boolean report) {
		// 2012.02.21 by PMM
		// CancelNextAssignedTrCmd();를 먼저하는 경우,
		// Cancel 처리 (CMD_QUEUED, NOT_ASSIGNED)가 DB에 반영되기 전에 trCmd가 null이 되어
		// Vehicle과 AssignedVehicle은 ""이지만, CMD_WAITING, UNLOAD_ASSIGNED로 남아있는 경우가 Timing 상 발생 가능함.
		
//		StringBuilder message = new StringBuilder();
//		message.append("Vehicle:").append(vehicleData.getVehicleId());
//		message.append(", TrCmdId:").append(trCmd.getTrCmdId());
//		message.append(", CarrierId:").append(trCmd.getCarrierId());
//		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
//		message.append(", DestLoc:").append(trCmd.getDestLoc());
//		registerEventHistory(new EventHistory(EVENTHISTORY_NAME.CURRENT_JOB_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
//				"", "", EVENTHISTORY_REMOTEID.OPERATION, "", reason), false);
//		
//		cancelNextAssignedTrCmd(reason);
//
//		trCmd.setState(TRCMD_STATE.CMD_QUEUED);
//		trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
//		addTrCmdToStateUpdateList();
//
//		trCmd.setVehicle("");
//		trCmd.setAssignedVehicleId("");
//		addTrCmdToVehicleUpdateList();
//		
//		resetTrCmd();
		
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
			cancelVibrationCommand(reason);
			return;
		}
		
		// 2015.12.21 by KBS : Patrol 명령일 경우 TrCmd 삭제
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
			cancelPatrolCommand(reason);
			return;
		}
	
		unassignTrCmd();
		
		// 2012.11.01 by MYM : Cancel시에 Pause를 false로 초기화
		// 배경 : Unloading 중 Manual Error -> OHT 재시작 -> Commfail -> Current Job Cancel -> but Pause 유지 -> Job Assign 안됨
		if (trCmd.isPause()) {
			pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
		}
		
		StringBuilder message = new StringBuilder();
		message.append("Vehicle:").append(vehicleData.getVehicleId());
		message.append(", TrCmdId:").append(trCmd.getTrCmdId());
		message.append(", CarrierId:").append(trCmd.getCarrierId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		registerEventHistory(new EventHistory(EVENTHISTORY_NAME.CURRENT_JOB_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
				"", "", EVENTHISTORY_REMOTEID.OPERATION, "", reason), false);
		
		// 2012.03.20 by PMM
		// Unload 이전이기 때문에 NextAssignedTrCmd가 없음.
//		cancelNextAssignedTrCmd(reason);
		
		// 2011.10.20 by PMM
		// 작업 할당 이후, 장애 발생 시 MCS에 VHL Assign 보고 없이 바로 작업 할당 해제.
		if (report) {
			sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
			traceOperation("Job Cancel by " + reason);
		} else {
			traceOperation("Job Cancel by " + reason + " without report");
		}
		traceOperation(getJournalOfTrCmd());
		
		// 2012.11.01 by MYM : Cancel에 따른 VehicleUnAssigned 보고시 TrCmd의 정보를 참조 후 Reset 하도록 변경 
		// 배경 : Unload PathSearch Fail 발생 후 Cancel -> VehicleUnAssigned 보고시 TrCmdID 누락 발생
		resetTrCmd();
		
		// 2012.02.14 by PMM
		if (vehicleData.getState() == 'G' ||
				(vehicleData.getTargetNode().equals(vehicleData.getCurrNode()) == false || vehicleData.getStopNode().equals(vehicleData.getCurrNode()) == false)) {
			changeOperationMode(OPERATION_MODE.GO, "Job Cancel While Going.");
		}
	}
	
	private void unassignTrCmd() {
		if (trCmd != null) {
			trCmd.setState(TRCMD_STATE.CMD_QUEUED);
			trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
			addTrCmdToStateUpdateList();

			trCmd.setVehicle("");
			trCmd.setAssignedVehicleId("");
			addTrCmdToVehicleUpdateList();
			
			vehicleData.setAssignedVehicle(false);
		} else {
			traceOperationException("unassignTrCmd() - trCmd is null.");
		}
	}
	
	/**
	 * Cancel NextAssigned TrCmd
	 * 
	 * @param reason
	 */
	public void cancelNextAssignedTrCmd(EVENTHISTORY_REASON reason) {
//		String trCmdId = "";
//		if(trCmd != null) {
//			trCmdId = trCmd.getTrCmdId();
//		}
//		TrCmd nextTrCmd = this.trCmdManager.getNextTrCmdAndCancelAssignmentList(vehicleData.getVehicleId(), trCmdId);
		TrCmd nextTrCmd = this.trCmdManager.getNextTrCmdAndCancelAssignmentList(vehicleData.getVehicleId());
		if (nextTrCmd != null) {
			StringBuilder event = new StringBuilder();
			event.append(" Vehicle:").append(vehicleData.getVehicleId());
			event.append(", TrCmdId:").append(nextTrCmd.getTrCmdId());
			event.append(", CarrierId:").append(nextTrCmd.getCarrierId());
			event.append(", SourceLoc:").append(nextTrCmd.getSourceLoc());
			event.append(", DestLoc:").append(nextTrCmd.getDestLoc());
			registerEventHistory(new EventHistory(EVENTHISTORY_NAME.NEXT_JOB_CANCEL,
					EVENTHISTORY_TYPE.SYSTEM, "", event.toString(), "", "",
					EVENTHISTORY_REMOTEID.OPERATION, "", reason), false);
			
			StringBuffer log = new StringBuffer("NextTrCmd_Reset");
			log.append(" by ").append(reason).append(". (NextTrCmdId:").append(nextTrCmd.getTrCmdId()).append(")");
			traceOperation(log.toString());
		}
	}
	
	/**
	 * Set Requested ServiceState
	 * 
	 * @param state
	 * @return
	 */
	public boolean setRequestedServiceState(MODULE_STATE state) {
		this.requestedServiceState = state;
		return true;
	}

	/**
	 * Get ServiceState
	 * 
	 * @return
	 */
	public MODULE_STATE getServiceState() {
		return serviceState;
	}

	/**
	 * Manage ServiceState
	 * 
	 * @return
	 */
	private boolean manageActivationService() {
		if (serviceState != requestedServiceState) {
			// SERVICE_STATE와 REQ_SERVICE_STATE 다른 경우
			
			if (requestedServiceState == MODULE_STATE.INSERVICE) {
				// 1. OutofService -> InService
				// 1) CommData ReceivedReply, ReceivedState 초기화
				VehicleCommData commData = vehicleComm.getVehicleCommData();
				commData.setReceivedReply(false);
				commData.setReceivedState(false);
				
				// 2012.05.31 by PMM
				vehicleData.setEStopRequested(false);
				unregisterAllAlarm();
				
				// 2) ModeChange
				changeOperationMode(OPERATION_MODE.IDLE, "InService");
				
				// 3) DB의 StopNode로 Vehicle Initialize
//				pathSearch.initializeVehiclePath(vehicleData.getVehicleId());
				// 2011.10.21 by PMM
				pathSearch.initializeVehiclePath(vehicleData, "InService");
				
				// 4) 작업정보를 가져오기
				initializeVehicleAssignData();
				
				// 5) SERVICE_STATE 변경
				serviceState = MODULE_STATE.INSERVICE;
				
				// 2011.10.28 by PMM
				// SystemPause 추가 (RuntimeUpdate)
				isSystemPaused = false;
				
				vehicleData.setStateChangedTime(System.currentTimeMillis());
				vehicleData.setYieldState('N');
				vehicleData.setDriveFailedNode(null);
				vehicleData.setAssignedVehicle(trCmd != null);
				vehicleData.resetRedirectedNodeSet();
				
				yieldCancelledNode = null;
				
				lastLoadPathSearchFailedTime = System.currentTimeMillis();
				firstLoadPathSearchFailedTime = System.currentTimeMillis();
				
				lastCommandSentTime = System.currentTimeMillis();
				lastDifferentCommandSentTime = System.currentTimeMillis();
				lastDifferentCommand = "";
				
				return true;
			} else {
				// 2. InService -> OutofService
				serviceState = MODULE_STATE.OUTOFSERVICE;
				operationControlState = OPERAION_CONTROL_STATE.INIT;
				
				// 2012.06.01 by PMM
				// InService -> OutOfService -> InService 시 false로 초기화 되어 있어야 함.
				isAllOperationReady = false;
				
				// 2012.03.06 by PMM
				// resetDriveNodeList 시 currNode 필요.
//				vehicleData.reset(isNearByDrive);
				
				// 2012.05.31 by PMM
//				vehicleData.reset(isNearByDrive, nodeManager.getNode(vehicleData.getCurrNode()));
				vehicleData.clear(isNearByDrive);
				
				// 2015.02.06 by MYM : 장애 지역 우회 기능
				vehicleData.clearAbnormalSectionSet(false);
				return false;
			}			
		} else {
			// SERVICE_STATE와 REQ_SERVICE_STATE 같은 경우
			
			if (requestedServiceState == MODULE_STATE.INSERVICE) {
				// 3. InSerivce -> InService
				return true;
			} else {
				// 4. OutofService -> OutofService
				operationControlState = OPERAION_CONTROL_STATE.INIT;
				
				// 2012.03.06 by PMM
				// resetDriveNodeList 시 currNode 필요.
//				vehicleData.reset(isNearByDrive);
				
				// 2012.05.31 by PMM
//				vehicleData.reset(isNearByDrive, nodeManager.getNode(vehicleData.getCurrNode()));
				vehicleData.clear(isNearByDrive);
				return false;
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	private boolean checkSleepMode() {
		if (activeOperationMode.getOperationMode() != OPERATION_MODE.SLEEP) {
			if (vehicleData.getVehicleMode() == 'M') {
				// OperationMode(x->S) by Manual.
				changeOperationMode(OPERATION_MODE.SLEEP, "VehicleMode: Manual");
				// 2014.10.13 by MYM : 장애 지역 우회 기능
				if (vehicleData.getState() == 'E') {
					vehicleData.setAbnormalSection(DETOUR_REASON.VEHICLE_ERROR);
				} else {
					vehicleData.setAbnormalSection(DETOUR_REASON.VEHICLE_MANUAL);
				}
				return true;
			} else if (vehicleData.getState() == 'E' || vehicleData.getState() == 'V') {
				// OperationMode(x->S) by Vehicle Error (E, V)
				changeOperationMode(OPERATION_MODE.SLEEP, "VehicleState: " + vehicleData.getState());
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Operation Process Method
	 */
	private void operationProcess() {
		// Operational Parameter 변경 사항 확인 및 반영.
		updateOperationalParameters();

		// ThreadPause인 경우, Vehicle에 ActionHold 설정.
		// setActionHoldToAllVehicle(true);
		
		// Active Service(InService, OutofService) 처리
		if (manageActivationService() == false) {
			return;
		}
		
		// Vehicle State 수신(Mode, State, PrevCmd, CurrCmd, NextCmd 등) 처리
		updateVehicleData();
		
		// 2013.09.06 by MYM : [OHT Location Update시 처리 보완] SleepMode가 아닌 상태에서 Manual, Recovery 상태 수신시 바로 SleepMode 전환하여 처리
		// 배경 : GoMode에서 짧은 시간에 AG → AA → MI → AV 가 순차적으로 되었을 때 GoMode에서 SleepMode 전환을 체크하기 때문에
		//       다음 Thread 루틴에서 Sleep 처리가 되어 한템포 늦게 처리됨. 
		//       위의 경우 MI 다음 바로 AV가 올라와서 MI 처리가 안되는 경우가 발생함. 
		checkSleepMode();

		// 2012.03.06 by PMM
		// Check EStop Requested
		checkEStopRequested();
		
		// 2011.10.12 by PMM start가 되지 않으면 CommFail 여부를 알 수 없어 check 위치를 이동
		// CommFail 여부 확인
		checkCommFail();
		
		// 2013.07.16 by MYM RuntimeUpdate시 무언정지 발생할 경우 체크가 안되어 check 위치를 이동
		// Abnormal Case(NotRespond) 처리
		checkAbnormalCase();
		
		if (isAllOperationReady &&
				operationControlState == OPERAION_CONTROL_STATE.START &&
				// 2011.11.15 by PMM
//				(ocsInfoManager.getTscState() == TSC_STATE.TSC_AUTO || ocsInfoManager.isIBSEMUsed() == false) &&
				(ocsInfoManager.getTscState() == TSC_STATE.TSC_AUTO || isIBSEMUsed == false) &&
				isSystemPaused == false) {
			// 2011.10.28 by PMM
			// SystemPause 추가 (RuntimeUpdate)
//			(ocsInfoManager.getTscState() == TSC_STATE.TSC_AUTO || ocsInfoManager.isIBSEMUsed() == false)) {
			startedTime = System.currentTimeMillis();
			currMode = activeOperationMode.getOperationMode().toConstString();

			// RemoteCommand 처리(Transfer, Abort, Cancel, DestChange, Stage, StageChange)
			processRemoteCmd();
			
			checkStageCommand();

			// 2022.03.14 dahye : Premove Logic Improve
			// 반송 진행상황과 관계 없이 PREMOVE 반송에 대한 처리 필요 
			// 기존 : DestLoc 도착 이후 timeCheck
			// 변경 : 반송 생성 이후 timeCheck
			checkPremoveCommand();
			
			// 2012.01.19 by PMM
			checkVehicleDetection();
			
			// 2012.02.13 by PMM
			checkPatrolCancel();
			
			checkVibrationMonitoringTimeout();

			// Operation 모드별 처리
			activeOperationMode.controlVehicle();
			
			// AutoRetry 처리 
			checkAutoRetry();

			elapsedTime = System.currentTimeMillis() - startedTime;
//			if (elapsedTime > ocsInfoManager.getOperationDelayLimit()) {
			if (elapsedTime > delayLimitOfOperation) {
				StringBuffer message = new StringBuffer();
//				message.append("[Mode:").append(activeOperationMode.getOperationMode().toConstString());
				message.append("[Mode:").append(currMode);
				message.append("] ElapsedTime:").append(elapsedTime).append("ms (");
				message.append(delayLimitOfOperation).append("ms over)");
				traceOperationDelay(message.toString());
			}
		}
	}

	/**
	 * Get RemainingDuration
	 * 
	 * @return
	 */
	private long getRemainingDuration() {
		assert trCmd != null;
		
		try {
			if (trCmd == null) {
				// 2011.10.20 by PMM
//				return System.currentTimeMillis();
				return 0;
			} else {
				// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//				return trCmd.getExpectedDuration() - getWaitingTime(trCmd.getTrQueuedTime());
				return (trCmd.getExpectedDuration() + trCmd.getWaitTimeout()) - getWaitingTime(trCmd.getTrQueuedTime());
			}
		} catch (Exception e) {
			traceOperationException("getRemainingDuration()", e);
			// 2011.10.20 by PMM
//			return System.currentTimeMillis();
			return 0;
		}
	}
	
	/**
	 * 2022.03.14 dahye : Premove Logic Improve
	 * Get Premove RemainingDuration
	 * (DWT : DeliveryWaitTime)
	 */
	private long getPremoveRemainingDuration() {
		assert trCmd != null;
		
		try {
			if (trCmd == null) {
				return 0;
			} else {
				return (trCmd.getDeliveryWaitTimeOut() - getWaitingTime(trCmd.getWaitStartedTime()));
			}
		} catch (Exception e) {
			traceOperationException("getPremoveRemainingDuration()", e);
			return 0;
		}
	}
	
	/**
	 * Check AutoRetry
	 */
	private void checkAutoRetry() {
		if (trCmd == null || vehicleData.isAvRetryWait() == false) {
			return;
		}
		
		// 2012.11.28 by MYM : TrCmd의 DetailState에 따라서 Source or Dest 비교 조건 추가
//		if (trCmd.getDetailState().toConstString().startsWith("UNLOAD")) {
		if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED) {
			if (vehicleData.getTargetNode().equals(trCmd.getSourceNode())) {
				// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
				AutoRetryGroupInfo autoRetryGroupInfo = getAutoRetryGroupInfo(trCmd.getSourceLoc());
				if (autoRetryGroupInfo != null && trCmd.getPauseCount() <= autoRetryGroupInfo.getUnloadCount()) {
					if (((Math.abs(System.currentTimeMillis() - vehicleData.getLastAvTime())) > autoRetryGroupInfo.getUnloadPauseTime())) {
						if (isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc())) == false) {
							pauseTrCmd(false, trCmd.getPauseType(), trCmd.getPauseCount());
							vehicleData.setAvRetryWait(false);
						}
					}
				}
			}
//		} else if (trCmd.getDetailState().toConstString().startsWith("LOAD")) {
		} else if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADED
				|| trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ASSIGNED) {
			if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
				// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
				AutoRetryGroupInfo autoRetryGroupInfo = getAutoRetryGroupInfo(trCmd.getDestLoc());
				if (autoRetryGroupInfo != null && trCmd.getPauseCount() <= autoRetryGroupInfo.getLoadCount()) {
					if (((Math.abs(System.currentTimeMillis() - vehicleData.getLastAvTime())) > autoRetryGroupInfo.getLoadPauseTime())) {
						if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc())) == false) {
							pauseTrCmd(false, trCmd.getPauseType(), trCmd.getPauseCount());
							vehicleData.setAvRetryWait(false);
						}
					}
				}
			}
		}
	}

	/**
	 * Check STAGE Command
	 */
	private void checkStageCommand() {
		// 2011.10.20 by PMM
		// STAGE 처리 수정. 기존 STAGE_ASSIGNED -> STAGE_NOBLOCKING/STAGE_WAITING 구분.
		if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
			trCmd.setRemainingDuration(getRemainingDuration());
			// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//			if (trCmd.getRemainingDuration() <= 0) {
			if (trCmd.getRemainingDuration() <= 0 && trCmd.getState() == TRCMD_STATE.CMD_WAITING) {
				long waitingTime = getWaitingTime(trCmd.getTrQueuedTime());
				StringBuffer log = new StringBuffer();
				log.append("STAGE ExpectedDuration TimeOver. ");
				log.append("ElapseTime:").append(waitingTime).append(",RemainigTime:").append((trCmd.getExpectedDuration() + trCmd.getWaitTimeout()) - waitingTime);
				log.append("(EDT:").append(trCmd.getExpectedDuration()).append(", NBT:").append(trCmd.getNoBlockingTime());
				log.append(", WTO:").append(trCmd.getWaitTimeout()).append(")");
				traceOperation(log.toString());
				
				cancelStageCommand(EVENTHISTORY_REASON.EXPECTEDDURATION_TIMEOVER);
				
				// 2012.01.19 by PMM
				// StopNode에 Drive했지만 대차 감지로 도착하지 못한 경우, 양보 요청 못하는 케이스 발생. 
				// GoMode인 경우, 정상적으로 TargetNode에 도착할 때까지 Mode 유지
//				changeOperationMode(OPERATION_MODE.IDLE, "STAGECANCEL (ExpectedDuration Timeover.)");
				if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
					changeOperationMode(OPERATION_MODE.IDLE, "STAGECANCEL (ExpectedDuration Timeover.)");
				} else if (vehicleData.getState() == 'G' && activeOperationMode.getOperationMode() != OPERATION_MODE.GO) {
					// 2012.03.20 by PMM
					// Going 중 STAGECANCEL 되는 케이스 생김. GO -> (JobAssign) -> IDLE -> (STAGECANCEL) -> IDLE (But OHT Going)
					changeOperationMode(OPERATION_MODE.GO, "STAGECANCEL (ExpectedDuration Timeover)");
				}
				
				return;
			} else {
				switch (trCmd.getState()) {
					case CMD_WAITING:
					{
						// 2012.03.08 by PMM
						// (C:149046,S:149047,T:149046) SourceNode:149046인 케이스 발생함.
//						if (vehicleData.getTargetNode().equals(trCmd.getSourceNode()) &&
//								vehicleData.getCurrNode().equals(trCmd.getSourceNode())) {
						if (vehicleData.getCurrNode().equals(vehicleData.getTargetNode()) &&
								vehicleData.getStopNode().equals(vehicleData.getTargetNode()) &&
								vehicleData.getTargetNode().equals(trCmd.getSourceNode())) {
							trCmd.setState(TRCMD_STATE.CMD_STAGING);
							trCmd.setDetailState(TRCMD_DETAILSTATE.STAGE_NOBLOCKING);
							addTrCmdToStateUpdateList();
							
							// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//							if (trCmd.getNoBlockingTime() == 0) {
//								trCmd.setNoBlockingTime(trCmd.getRemainingDuration());
//							}
//							if (trCmd.getWaitTimeout() == 0) {
//								trCmd.setWaitTimeout(trCmd.getRemainingDuration());
//							}
//							StringBuffer log = new StringBuffer("Set StageInitTime by Arrived at SourceNode. ");
							long waitingTime = getWaitingTime(trCmd.getTrQueuedTime());
							StringBuffer log = new StringBuffer("STAGE Arrived at SourceNode. ");
							log.append("ElapseTime:").append(waitingTime).append(",RemainigTime:").append((trCmd.getExpectedDuration() + trCmd.getWaitTimeout()) - waitingTime);
							log.append("(EDT:").append(trCmd.getExpectedDuration()).append(", NBT:").append(trCmd.getNoBlockingTime());
							log.append(", WTO:").append(trCmd.getWaitTimeout()).append(")");
							traceOperation(log.toString());
							
							trCmd.setStageInitTime(System.currentTimeMillis());
							
							// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//							changeOperationMode(OPERATION_MODE.WORK, "STAGE - NoBlocking.");
							changeOperationMode(OPERATION_MODE.IDLE, "STAGE NoBlocking");
						} else {
							// Wait to arrive at SourceNode.
							; /*NULL*/
						}
						break;
					}
					case CMD_STAGING:
					{
						if (trCmd.getStageInitTime() == 0) {
							if (trCmd.getUnloadingTime() != null && trCmd.getUnloadingTime().length() < 2) {
								// Abnormal Case.
								trCmd.setStageInitTime(System.currentTimeMillis());
							} else {
								// Failover Case.
								trCmd.setStageInitTime(System.currentTimeMillis() - getWaitingTimeMillis(trCmd.getUnloadingTime()));
							}
						}
						
						if (trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_NOBLOCKING) {
							// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
							long waitingTime = getWaitingTime(trCmd.getTrQueuedTime());
//							if (Math.abs(System.currentTimeMillis() - trCmd.getStageInitTime()) >= trCmd.getNoBlockingTime() * 1000) {
							if (waitingTime >= trCmd.getNoBlockingTime()) {
								trCmd.setDetailState(TRCMD_DETAILSTATE.STAGE_WAITING);
								addTrCmdToStateUpdateList();

								// 2012.01.19 by PMM
								// GoMode인 경우, 정상적으로 TargetNode에 도착할 때까지 Mode 유지
								// changeOperationMode(OPERATION_MODE.IDLE, "STAGE Wait (Release NoBlockingTime.)");
								// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//								if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
//									changeOperationMode(OPERATION_MODE.IDLE, "STAGE Wait (Release NoBlockingTime.)");
//								}
//								traceOperation("Release NoBlockingTime.");
								StringBuffer log = new StringBuffer();
								log.append("STAGE Release NoBlockingTime. ");
								log.append("ElapseTime:").append(waitingTime).append(",RemainigTime:").append((trCmd.getExpectedDuration() + trCmd.getWaitTimeout()) - waitingTime);
								log.append("(EDT:").append(trCmd.getExpectedDuration()).append(", NBT:").append(trCmd.getNoBlockingTime());
								log.append(", WTO:").append(trCmd.getWaitTimeout()).append(")");
								traceOperation(log.toString());
								
								if (vehicleData.getState() != 'G' && activeOperationMode.getOperationMode() != OPERATION_MODE.WORK) {
									changeOperationMode(OPERATION_MODE.WORK, "STAGE Wait (Release NoBlockingTime)");
								}
							}
						}
						if (trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_WAITING) {
							// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
							// 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
							if(isStageSourceDupCancelUsage){
								if(checkDupSourceLoc(trCmd.getSourceLoc())){
									StringBuffer log = new StringBuffer();
									log.append("Duplicated(Stage) SourceLoc :"+trCmd.getSourceLoc());
									traceOperation(log.toString());
									cancelStageCommand(EVENTHISTORY_REASON.SOURCE_DUPLICATE_BY_STAGE);
									if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
										changeOperationMode(OPERATION_MODE.IDLE, "STAGECANCEL (Duplicated TRCMD SourceLoc)");
									} else if (vehicleData.getState() == 'G' && activeOperationMode.getOperationMode() != OPERATION_MODE.GO) {
										changeOperationMode(OPERATION_MODE.GO, "STAGECANCEL (Duplicated TRCMD SourceLoc)");
									}
									break;
								}
							}
							long waitingTime = getWaitingTime(trCmd.getTrQueuedTime());
//							if (Math.abs(System.currentTimeMillis() - trCmd.getStageInitTime()) >= trCmd.getWaitTimeout() * 1000) {
							if (waitingTime >= (trCmd.getExpectedDuration() + trCmd.getWaitTimeout())) {
								StringBuffer log = new StringBuffer();
								log.append("STAGE WaitTimeout Timeover. ");
								log.append("ElapseTime:").append(waitingTime).append(",RemainigTime:").append((trCmd.getExpectedDuration() + trCmd.getWaitTimeout()) - waitingTime);
								log.append("(EDT:").append(trCmd.getExpectedDuration()).append(", NBT:").append(trCmd.getNoBlockingTime());
								log.append(", WTO:").append(trCmd.getWaitTimeout()).append(")");
								traceOperation(log.toString());
								
								cancelStageCommand(EVENTHISTORY_REASON.WAITTIMEOUT_TIMEOVER);
								
								// 2012.01.19 by PMM
								// GoMode인 경우, 정상적으로 TargetNode에 도착할 때까지 Mode 유지
//								changeOperationMode(OPERATION_MODE.IDLE, "STAGECANCEL (WaitTimeout Timeover.)");
								if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
									changeOperationMode(OPERATION_MODE.IDLE, "STAGECANCEL (WaitTimeout Timeover)");
								} else if (vehicleData.getState() == 'G' && activeOperationMode.getOperationMode() != OPERATION_MODE.GO) {
									// 2012.03.20 by PMM
									changeOperationMode(OPERATION_MODE.GO, "STAGECANCEL (WaitTimeout Timeover)");
								}
//								traceOperation("WaitTimeout Timeover.");
							}
						}
						break;
					}	
					default:
						break;
				}
			}
		}
	}
	
	/**
	 * 2022.03.14 dahye : Premove Logic Improve
	 * 반송생성 이후부터 TimeCheck
	 */
	private void checkPremoveCommand() {
		if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE) {
			if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE) {
				trCmd.setRemainingDuration(getPremoveRemainingDuration());
				
				switch (trCmd.getState()) {
				case CMD_WAITING:
				{
					if (trCmd.getRemainingDuration() <= 0) {
						if (trCmd.isPause() == false) {							
							long waitTime = getWaitingTime(trCmd.getWaitStartedTime());
							StringBuffer log = new StringBuffer();
							log.append("PREMOVE DeliveryWaitTime TimeOver. ");
							log.append("ElapseTime:").append(waitTime).append(",RemainigTime:").append(trCmd.getDeliveryWaitTimeOut() - waitTime);
							log.append("(DWT:").append(trCmd.getDeliveryWaitTimeOut()).append(")");
							traceOperation(log.toString());
							
							trCmd.setLastAbortedTime(System.currentTimeMillis());
							trCmd.setRemoteCmd(TRCMD_REMOTECMD.CANCEL);
							trCmd.setState(TRCMD_STATE.CMD_CANCELED);
							trCmd.setDeletedTime(getCurrDBTimeStr());
							addTrCmdToStateUpdateList();
							registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
							
							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_PREMOVE_WAIT_TIMEOUT);
							
							resetTargetNode("Premove DWT TimeOver");
							if (vehicleData.getState() == 'G' && activeOperationMode.getOperationMode() != OPERATION_MODE.GO) {
								changeOperationMode(OPERATION_MODE.GO, "PREMOVE DWT Timeover");
							}
							
							traceOperation("Job Cancel: " + trCmd.getTrCmdId());
							traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Cancel");
							
							deleteTrCmdFromDB();
						}
					}
					break;
				}
				case CMD_TRANSFERRING:
				case CMD_PAUSED:	// 2022.05.05 dahye : PREMOVE Paused CASE 처리 필요
				{
					switch (trCmd.getDetailState()) {
					case UNLOAD_SENT:
					case UNLOAD_ACCEPTED:
					case UNLOADING:
					{
						// Wait for Unload Completed...						
						break;
					}
					case UNLOADED:
					case LOAD_ASSIGNED:
					{
						if (trCmd.getRemainingDuration() <= 0) {
							if (trCmd.isPause() == false) {
								long waitTime = getWaitingTime(trCmd.getWaitStartedTime());
								StringBuffer log = new StringBuffer();
								log.append("PREMOVE DeliveryWaitTime TimeOver. ");
								log.append("ElapseTime:").append(waitTime).append(",RemainingTime:").append(trCmd.getDeliveryWaitTimeOut() - waitTime);
								log.append("(DWT:").append(trCmd.getDeliveryWaitTimeOut()).append(")");
								traceOperation(log.toString());
								
								trCmd.setLastAbortedTime(System.currentTimeMillis());
								trCmd.setState(TRCMD_STATE.CMD_ABORTED);
								trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
								trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
								trCmd.setDeliveryType("");
								pauseTrCmd(true, TrCmdConstant.DW_TIMEOUT, trCmd.getPauseCount() + 1);
								addTrCmdToStateUpdateList();
								
								sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_PREMOVE_WAIT_TIMEOUT);
								addVehicleToUpdateList();
								
								resetTargetNode("PREMOVE DWT Timeover");
								if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
									changeOperationMode(OPERATION_MODE.IDLE, "PREMOVE DWT Timeover");
								} else if (vehicleData.getState() == 'G' && activeOperationMode.getOperationMode() != OPERATION_MODE.GO) {
									changeOperationMode(OPERATION_MODE.GO, "PREMOVE DWT Timeover");
								}
								
								traceOperation("Job Abort: " + trCmd.getTrCmdId());
								traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Abort");
							}
						}
						break;
					}
					default:
					{
						traceOperationException("Abnormal Case: Operation#015");
						break;
					}
					}
				}
				case CMD_PREMOVE:
				{
					if (trCmd.getRemainingDuration() <= 0 && trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_WAITING) {
						if (trCmd.isPause() == false) {
							long waitTime = getWaitingTime(trCmd.getWaitStartedTime());
							StringBuffer log = new StringBuffer();
							log.append("PREMOVE DeliveryWaitTime TimeOver. ");
							log.append("ElapseTime:").append(waitTime).append(",RemainingTime:").append(trCmd.getDeliveryWaitTimeOut() - waitTime);
							log.append("(DWT:").append(trCmd.getDeliveryWaitTimeOut()).append(")");
							traceOperation(log.toString());
							
							trCmd.setLastAbortedTime(System.currentTimeMillis());
							trCmd.setState(TRCMD_STATE.CMD_ABORTED);
							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
							trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
							trCmd.setDeliveryType("");
							pauseTrCmd(true, TrCmdConstant.DW_TIMEOUT, trCmd.getPauseCount() + 1);
							addTrCmdToStateUpdateList();
							
							cancelNextAssignedTrCmd(EVENTHISTORY_REASON.DELIVERYWAITTIMEOUT_TIMEOVER);
							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_PREMOVE_WAIT_TIMEOUT);
							addVehicleToUpdateList();
							
							// GoMode인 경우, 정상적으로 TargetNode에 도착할 때까지 Mode 유지  (양보 이동중인 호기)
							if (vehicleData.getState() == 'G' && activeOperationMode.getOperationMode() != OPERATION_MODE.GO) {
								changeOperationMode(OPERATION_MODE.GO, "PREMOVE DWT Timeover");
							}
							
							traceOperation("Job Abort: " + trCmd.getTrCmdId());
							traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Abort");
						}
					}
					break;
				}
				default:
					traceOperationException("Abnormal Case: Operation#016");
					break;
				}
			}
		}
	}
	
	// 2012.01.19 by PMM
	private void checkVehicleDetection() {
		if (isNearByDrive) {
			if (vehicleData.getPauseType() == 1 && vehicleData.getState() == 'G') {
				try {
					String yieldVehicleId = vehicleData.checkYieldRequestForForwardVehicleDetection(yieldRequestLimitTime);
					if (yieldVehicleId != null && yieldVehicleId.length() > 0) {
						traceOperation("Yield Request(Vehicle Detected) : " + yieldVehicleId);
					}
					// 2012.01.26 by PMM
					// Idle VHL이 작업할당을 받은 경우, IDLE Mode로 전환 후 처리.
//					if ((activeOperationMode.getOperationMode() == OPERATION_MODE.IDLE && vehicleData.getStopNode().equals(vehicleData.getTargetNode())) ||
					if ((activeOperationMode.getOperationMode() == OPERATION_MODE.IDLE && trCmd == null) ||
							activeOperationMode.getOperationMode() == OPERATION_MODE.WORK ||
							activeOperationMode.getOperationMode() == OPERATION_MODE.SLEEP) {
						StringBuilder message = new StringBuilder();
						message.append("Abnormal Case: Operation#012 - checkVehicleDetection()");
						traceOperationException(message.toString());
					}
				} catch (Exception e) {
					traceOperationException("checkVehicleDetection()", e);
				}
			}
		}
	}
	
	private void checkPatrolCancel() {
		if (vehicleData.getState() == 'Z') {
			// 2015.12.21 by KBS : Patrol VHL 기능 추가
			// 배경 : by OHT : Z 수신  -> Stop 정리 -> IDRESET 송신 -> TrCmd 정리 -> AI
			//      by OCS : PatrolCancel 송신 -> Z 수신 -> Stop 정리 -> IDRESET 송신 -> TrCmd 정리 -> AI
			traceOperation("PatrolCancel Reported by OHT:Z");
			
			// 1. StopNode Reset (CurrentNode -> StopNode)
			Node stopNode = vehicleData.getDriveStopNode();
			if (stopNode != null && vehicleData.getStopNode().equals(stopNode.getNodeId()) == false) {
				vehicleData.setStop(stopNode.getNodeId(), "");
				addVehicleToUpdateList();
			}

			// 2. ID Reset & TrCmd 정리
			sendIDResetCommandByPatrol();
		} else {
			// DataLogic Error 일 경우는 PatrolCancel을 보내지 않음
			if (vehicleData.getPatrolStatus() == '1' && vehicleData.getReply() != 'D') {
				// 배경 : OHT에서 Patroling Status가 보고되는 경우는 없으나 예외 시나리오로 아래와 같이 대응
				// 1-1 TrCmd가 없는 경우 : 무시
				// 1-2 TrCmd가 있으나 Patrolling 상태가 아닌 경우 : TrCmd 정리
				if (trCmd == null) {
//					sendPatrolCancelCommand();
//					traceOperation("PatrolCancel Command Sent - No TrCmd.");
					
					traceOperationException("Vehicle is Abnormal Patrol Status: Patrolling - No TrCmd.");
				} else if (trCmd.getDetailState() != TRCMD_DETAILSTATE.PATROLLING) {
//					sendPatrolCancelCommand();
//					traceOperation("PatrolCancel Command Sent - OHT is cleaning, but DetailState is not PATROLLING.");
					
					trCmd.setDeletedTime(getCurrDBTimeStr());
					trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
					trCmd.setDetailState(TRCMD_DETAILSTATE.PATROL_CANCELED);
					trCmd.setCarrierLoc(trCmd.getDestLoc());
					addTrCmdToStateUpdateList();
//					registerTrCompletionHistory(REQUESTEDTYPE.PATROL.toConstString());
					
					traceOperationException("Vehicle is Abnormal Patrol Status: Patrolling - DetailState is not PATROLLING.");
				}
			} else {
				// OHT Status 보고 Timing 상, 발생할 수 있음.
				; /*NULL*/
			}
		}
	}
	
	private void checkVibrationMonitoringTimeout() {
		if (trCmd != null) {
			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
				if (vehicleData.isCarrierExist()) {
					if (trCmd.getState() == TRCMD_STATE.CMD_MONITORING) {
						long elapsedTime = getWaitingTimeMillis(trCmd.getUnloadedTime());
						if (elapsedTime > vibrationMonitoringTimeout) {
							trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
							trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
							addTrCmdToStateUpdateList();
							
							if (trCmd.isPause()) {
								pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
							}
							traceOperation("VibrationMonitoring Timeout. ElapsedTime:" + (int)(elapsedTime / 60000) + "(min)");
						}
					}
				}
			}
		}
	}
	
	/**
	 * Update Operational Parameters
	 */
	private void updateOperationalParameters() {
		// 2014.06.21 by MYM : [Commfail 체크 개선] : 통신 체크 관련 파라미터 업데이트 정리 
//		if (socketReconnectionTimeout != ocsInfoManager.getSocketReconnectionTimeout()) {
//			socketReconnectionTimeout = ocsInfoManager.getSocketReconnectionTimeout();
//			vehicleComm.setSocketReconnectionTimeout(socketReconnectionTimeout);
//		}
		vehicleComm.setSocketReconnectionTimeout(ocsInfoManager.getSocketReconnectionTimeout());
		vehicleComm.setSocketCloseCheckTime(ocsInfoManager.getSocketCloseCheckTime());
		vehicleComm.setCommFailCheckTime(ocsInfoManager.getCommFailCheckTime());

		// 2012.04.09 by PMM
		// Setup Parameter -> Operational Parameter
		isAutoMismatchRecoveryMode = ocsInfoManager.isAutoMismatchRecoveryMode();
		isDynamicRoutingUsed = ocsInfoManager.isDynamicRoutingUsed();
		isFormattedLogUsed = ocsInfoManager.isFormattedLogUsed();
		isLocalOHTUsed = ocsInfoManager.isLocalOHTUsed();
		isMissedCarrierCheckUsed = ocsInfoManager.isMissedCarrierCheckUsed();
		isUnloadErrorReportUsed = ocsInfoManager.isUnloadErrorReportUsed();
		isSTBCUsed = ocsInfoManager.isSTBCUsed();
		isSteeringReadyUsed = ocsInfoManager.isSteeringReadyUsed();
		isUserPassThroughUsed = ocsInfoManager.isUserPassThroughUsed();
		isVehicleTrafficLogUsed = ocsInfoManager.isVehicleTrafficLogUsed();
		isYieldSearchUsed = ocsInfoManager.isYieldSearchUsed();
		delayLimitOfOperation = ocsInfoManager.getOperationDelayLimit();
		rfReadDevice = ocsInfoManager.getRfReadDevice();
		mismatchUnloadAppliedPort = ocsInfoManager.getMismatchUnloadAppliedPort();
		missedCarrierCheckSleep = ocsInfoManager.getMissedCarrierCheckSleep();
		goModeCheckTime = ocsInfoManager.getGoModeCheckTime();
		goModeVehicleDetectedCheckTime = ocsInfoManager.getGoModeVehicleDetectedCheckTime();
		goModeVehicleDetectedResetTimeout = ocsInfoManager.getGoModeVehicleDetectedResetTimeout();
		workModeCheckTime = ocsInfoManager.getWorkModeCheckTime();
		abortCheckTime = ocsInfoManager.getAbortCheckTime();
		localOHTClearOption = ocsInfoManager.getLocalOHTClearOption();
		driveFailLimitTime = ocsInfoManager.getDriveFailLimitTime();
		driveLimitTime = ocsInfoManager.getDriveLimitTime();
		vehicleCountPerHid = ocsInfoManager.getVehicleCountPerHid();
//		commFailCheckTime = ocsInfoManager.getCommFailCheckTime();
		yieldRequestLimitTime = ocsInfoManager.getYieldRequestLimitTime();
		isAutoRetryUsed = ocsInfoManager.isAutoRetryUsed();
		vibrationMonitoringTimeout = ocsInfoManager.getVibrationMonitoringTimeout();
		repathSearchHoldTimeout = ocsInfoManager.getRepathSearchHoldTimeout();
		isGoModeCarrierStatusCheckUsed = ocsInfoManager.isGoModeCarrierStatusCheckUsed();
		dynamicRoutingHoldTimeout = ocsInfoManager.getDynamicRoutingHoldTimeout();
		isNearByNormalDrive = ocsInfoManager.isNearByNormalDrive();
		
		// 2013.05.10 by MYM : HoistSpeedLevel, ShiftSpeedLevel 설정 추가
		hoistSpeedLevel = ocsInfoManager.getHoistSpeedLevel();
		shiftSpeedLevel = ocsInfoManager.getShiftSpeedLevel();
		
		pathSearch.updateOperationalParameters();
		yieldSearch.updateOperationalParameters();
		
		vehicleData.setFailureOHTDetourSearchUsed(ocsInfoManager.isFailureOHTDetourSearchUsed());
		// 2014.10.22 by MYM : Block 점유 정보 DB 업데이트를 파라미터화
		vehicleData.setBlockPreemptionUpdateUsed(ocsInfoManager.isBlockPreemptionUpdateUsed());
		// 2015.05.01 by KYK [Commfail Report]
		isCommfailAlarmReportUsed = ocsInfoManager.isCommfailAlarmReportUsed();
		isCarrierTypeMismatchUsed = ocsInfoManager.isCarrierTypeMismatchUsed();
		// 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
		isStageSourceDupCancelUsage = ocsInfoManager.isStageSourceDupCancelUsage(); 
	}

	/**
	 * Get Port for TargetInfo
	 * 
	 * @param vehicleId
	 * @param port
	 * @return
	 */
	private int getPortForTargetInfo(String vehicleId, int port) {
		if (isEmulatorMode) {
			int position = 0;
			char character = ' ';
			while (position < vehicleId.length()) {
				character = vehicleId.charAt(position);
				if ((character >= 'a' && character <= 'z') ||
						(character >= 'A' && character <= 'Z')) {
					position++;
					continue;
				} else {
					break;
				}
			}
			
			// Primary: port = 5001
			// Secondary: Port = 6001
			if (vehicleId.charAt(0) == 'R') {
				// ROHT001
				return (50000 + (port - 5001) + Integer.parseInt(vehicleId.substring(position)));
			} else {
				// OHT001
				return (port + Integer.parseInt(vehicleId.substring(position)) - 1);
			}
		} else {
			return port;
		}
	}

	/**
	 * Initialize Vehicle AssignData
	 */
	private void initializeVehicleAssignData() {
		this.trCmd = this.trCmdManager.getAssignedTrCmd(vehicleData.getVehicleId());
		StringBuffer initLog = new StringBuffer();
		if (trCmd != null) {
			initLog.append("initializeVehicleAssignData/").append(trCmd.getTrCmdId()).append("/").append(trCmd.getRemoteCmd()).append("/");
			initLog.append(trCmd.getState()).append("/").append(trCmd.getDetailState()).append("/");
			initLog.append(trCmd.getCarrierId()).append("/").append(trCmd.getSourceLoc()).append("/").append(trCmd.getDestLoc()).append("/").append(trCmd.getCarrierLoc()).append("/");
			initLog.append(trCmd.getSourceNode()).append("/").append(trCmd.getDestNode()).append("/");
			initLog.append(trCmd.getReplace()).append("/").append(trCmd.getPriority()).append("/");
			initLog.append(trCmd.isPause()).append("/").append(trCmd.getPauseType()).append("/").append(trCmd.getPauseCount()).append("/").append(trCmd.getPausedTime()).append("/");
			initLog.append(vehicleData.getVehicleMode()).append("/").append(vehicleData.getState()).append("/").append(vehicleData.getCarrierExist()).append("/");
			initLog.append(vehicleData.getCurrNode()).append("/").append(vehicleData.getStopNode()).append("/").append(vehicleData.getTargetNode()).append("/");
			initLog.append(vehicleData.getErrorCode());
			
			// PauseType이 'AUTO ERROR'인 경우 AvRetryWait 값을 true로 설정
			// 배경 : Auto Retry가 발생한 후 Operation이 재시작한 경우 AvRetryWait 값을 무조건 false로 초기화하여 Retry 진행이 되지 않음.
			if (trCmd.isPause() && AUTO_ERROR.equals(trCmd.getPauseType())) {
				vehicleData.setAvRetryWait(true);
				traceOperation("AvRetryWait is true by AUTORETRY");
			} else {
				vehicleData.setAvRetryWait(false);
			}
			
			// 2012.07.23 by MYM : Manual Error에서 재시작한 경우 vehicleError를 true로 설정 
			if (trCmd.isPause() && TrCmdConstant.VEHICLE_MANUAL_ERROR.equals(trCmd.getPauseType())) {
				vehicleData.setVehicleError(true);
			}
			
			// 2012.03.22 by MYM : Manual인 Vehicle Init인 경우는 Pause 걸지 않도록 수정 
			// 배경 : Manual인 경우는 Pause를 걸지 않음.
			// 2012.03.16 by PMM
			// CMD_PAUSED인 상태로 재시작 시, trCmd.isPause()가 true가 아닌 케이스가 발생함.
			if (trCmd.getState() == TRCMD_STATE.CMD_PAUSED && trCmd.isPause() == false 
					&& vehicleData.getVehicleMode() != 'M') {
				pauseTrCmd(true, trCmd.getPauseType(), trCmd.getPauseCount());
			}
		} else {
			initLog.append("initializeVehicleAssignData/");
			initLog.append(vehicleData.getVehicleMode()).append("/").append(vehicleData.getState()).append("/").append(vehicleData.getCarrierExist()).append("/");
			initLog.append(vehicleData.getCurrNode()).append("/").append(vehicleData.getStopNode()).append("/").append(vehicleData.getTargetNode()).append("/");
			initLog.append(vehicleData.getErrorCode()).append("/NoTrCmd.");
			vehicleData.setAvRetryWait(false);
		}
		traceOperation(initLog.toString());
	}
	
	/**
	 * Check EStop Requested
	 */
	private void checkEStopRequested() {
		if (vehicleData.isEStopRequested()) {
			if (vehicleData.getVehicleMode() == 'A') {
//			  - $P2 / $p2A
//			   : 충돌구간에서 타 OHT가 갑자기 인식되는 경우
				sendEStopCommand(2);
			}
			// Reset E-Stop Requested
			vehicleData.setEStopRequested(false);
		}
	}
	
	private long stoppedTime = System.currentTimeMillis();
	private boolean wasStopped = false;
	private boolean wasStoppedForWork = false;
	
	/**
	 * Update VehicleData
	 */
	private void updateVehicleData() {
		VehicleCommData commData = vehicleComm.getVehicleCommData();

		if (commData.isReceivedReply()) {
			processReceivedCommandReplyMessageFromVehicle(commData);
			commData.setReceivedReply(false);
		}

		if (commData.isReceivedState()) {
			if (isVehicleTrafficLogUsed) {
				useTrafficLog(commData);
			}
			
			if (commData.getCurrNode().equals(vehicleData.getCurrNode()) == false ||
					commData.getCurrStationId().equals(vehicleData.getCurrStation()) == false || // 2013.03.05 by KYK
					commData.getPauseType() != vehicleData.getPauseType() ||
					commData.getState() != vehicleData.getState() ||
					commData.getVehicleMode() != vehicleData.getVehicleMode() ||
					 ocsInfoManager.getTscState() == TSC_STATE.TSC_PAUSED) {
				// 2011.10.20 by PMM. 
				// TSC_PAUSED 상태가 오래 지속될 경우 TSC_AUTO로 바뀔 때, NOT_RESPONDING 문제 발생. -> 해당 시간 동안 notresponding 무시.
				// Mode의 변경 여부도 확인: 장애 OHT가 A -> MI -> AI 시 알람 정리 안되는 경우 생김 (무언정지)
				
				// 2013.01.10 by PMM
				// PauseType 조건 추가.
				// 대차 감지로 인해 60초 이상 Node 업데이트가 없다가 대차 감지 해제 시 Going 중 무언정지로 작업할당 해제가 된 케이스 발생.
				vehicleData.setStateChangedTime(System.currentTimeMillis());
			}

			if (cmdState == COMMAND_STATE.SENT) {
				if (Math.abs((System.currentTimeMillis() - lastCommandSentTime)) > 5000) {
					cmdState = COMMAND_STATE.WAITFORRESPONSE;
				}
			}

			// 2013.09.06 by KYK : Manual 에서 stationId 만 올라오는 경우 정상처리 (node 는 parentnode 로)
			if ('M' == commData.getVehicleMode()) {
				if (isVaildStationButNodeNotExist(commData)) {
					Station station = stationManager.getStation(commData.getCurrStationId());
					commData.setCurrNode(station.getParentNodeId());
				}
			}

			// 추후 CurrNode 검증으로 변경 필요.
			if (nodeManager.isValidNode(commData.getCurrNode()) == false) {
				isValidNodeUpdated = false;
				
				traceOperation("[UpdateVehicleData] NodeID Mismatch: " + commData.getCurrNode());
				if ('M' == commData.getVehicleMode() && 'E' == commData.getState()) {
					// 2013.05.28 by MYM : VehicleComm에서 Status 수신시 동일 메시지 비교를 하여 여기서는 비교하지 않도록함.
//					if (vehicleData.equalVehicleData(commData.getVehicleMode(), commData.getState()) == false) {
//						vehicleData.setVehicleMode(commData.getVehicleMode());
//						vehicleData.setState(commData.getState());
//						vehicleData.setErrorCode(commData.getErrorCode());
//						addVehicleToUpdateList();
//					}
						vehicleData.setVehicleMode(commData.getVehicleMode());
						vehicleData.setState(commData.getState());
						vehicleData.setErrorCode(commData.getErrorCode());
						addVehicleToUpdateList();
				}
				// MI (Manual Init)일 때는 real Node 값이 올라올 때만 정보 업데이트. 
				return;
			} else {
				isValidNodeUpdated = true;
				// 2013.05.28 by MYM : VehicleComm에서 Status 수신시 동일 메시지 비교를 하여 여기서는 비교하지 않도록함.
//				String currStation = getValidCurrStation(commData);
//				boolean equalVehicleData = vehicleData.equalVehicleData(commData.getCurrNode(), commData.getVehicleMode(), commData.getState(),
//						commData.getSpeed(), commData.getErrorCode(), commData.getRfData(),
//						commData.getMapVersion(), commData.getCarrierExist(), currStation);
//				if (equalVehicleData == false) {
//					vehicleData.setVehicleMode(commData.getVehicleMode());
//					vehicleData.setState(commData.getState());
//					vehicleData.setCurrNode(commData.getCurrNode());
//					vehicleData.setVehicleSpeed(commData.getSpeed());
//					vehicleData.setMapVersion(commData.getMapVersion());
//					vehicleData.setErrorCode(commData.getErrorCode());
//					vehicleData.setRfData(commData.getRfData());
//					vehicleData.setCarrierExist(commData.getCarrierExist());
//					
//					// 2012.06.05 by PMM
//					vehicleData.setAPSignal(commData.getAPSignal());
//					vehicleData.setAPMacAddress(commData.getAPMacAddress());
//					
//					vehicleData.setCurrStation(currStation);
//					vehicleData.setCurrNodeOffset(commData.getCurrNodeOffset());
//					vehicleData.setHidData(commData.getHidData());
//					vehicleData.setInputData(commData.getInputData());
//					vehicleData.setOutputData(commData.getOutputData());
//
//					addVehicleToUpdateList();
//				}
				vehicleData.setVehicleMode(commData.getVehicleMode());
				vehicleData.setState(commData.getState());
				vehicleData.setCurrNode(commData.getCurrNode());
				vehicleData.setCurrStation(getValidCurrStation(commData));
				// 2013.09.06 by KYK
				if (commData.getCurrNodeOffset() >= 0) {
					vehicleData.setCurrNodeOffset(commData.getCurrNodeOffset());					
				} else {
					vehicleData.setCurrNodeOffset(0);
				}
				vehicleData.setCarrierExist(commData.getCarrierExist());
				vehicleData.setCarrierType(commData.getCarrierType());
				vehicleData.setPauseType(commData.getPauseType());
				vehicleData.setSteerPosition(commData.getSteerPosition());
				vehicleData.setErrorCode(commData.getErrorCode());
				vehicleData.setOriginInfo(commData.getOrigin());
				vehicleData.setRfData(commData.getRfData());
				vehicleData.setAPSignal(commData.getAPSignal());
				vehicleData.setAPMacAddress(commData.getAPMacAddress());
				vehicleData.setMotorDrvFPosition(commData.getMotorDrvFPosition());
				vehicleData.setVehicleSpeed(commData.getSpeed());
				vehicleData.setMotorHoistPosition(commData.getMotorHoistPosition());
				vehicleData.setMotorShiftPosition(commData.getMotorShiftPosition());
				vehicleData.setMotorRotate(commData.getMotorRotate());
				vehicleData.setHidData(commData.getHidData());
				vehicleData.setInputData(commData.getInputData());
				vehicleData.setOutputData(commData.getOutputData());
				vehicleData.setMapVersion(commData.getMapVersion());
				vehicleData.setStationMapVersion(commData.getStationMapVersion());
				vehicleData.setTeachingMapVersion(commData.getTeachingMapVersion());
				vehicleData.setVehicleType(commData.getVehicleType());
				
				addVehicleToUpdateList();
				
				vehicleData.setPrevCmd(commData.getPrevCmd());
				vehicleData.setCurrCmd(commData.getCurrCmd());
				vehicleData.setNextCmd(commData.getNextCmd());
				vehicleData.setPauseType(commData.getPauseType());
				
				// 2015.12.21 by KBS : Patrol VHL 기능 추가
				vehicleData.setPatrolStatus(commData.getPatrolStatus());
				vehicleData.setTemperatureLevel(commData.getTemperatureLevel());
				
				// 2016.08.16 by KBS : Unload/Load 보고 시점 개선
				vehicleData.setTransStatus(commData.getTransStatus());
				
				// 2013.02.15 by KYK : currStation validation check
				if (checkNodeAndStationMismatch() == false) {
					setAlarmCode(OcsAlarmConstant.ESTOP_BY_NODE_STATION_MISMATCH);
					vehicleCommCommand.setCommandId(0);
					sendEStopCommand(6);
				}
			}
			
			// Vehicle로 부터 수신한 Commnad로 CommandState를 만듦
			manageVehicleCommandId(vehicleData.getPrevCmd(), vehicleData.getCurrCmd(), vehicleData.getNextCmd());

			// vehicle DriveNode 업데이트
			boolean nodeUpdated = vehicleData.updateDriveNode(vehicleData.getVehicleMode(), vehicleData.getState(), nodeManager.getNode(vehicleData.getCurrNode()), isNearByDrive);
			
			// State 로그 기록
			StringBuilder log = new StringBuilder();
			log.append("[").append(activeOperationMode.getOperationMode().toConstString()).append("]");
			log.append(" Mode:").append(vehicleData.getVehicleMode());
			log.append(" Status:").append(vehicleData.getState());
			log.append(" Node(").append(vehicleData.getCurrNode()).append(",").append(vehicleData.getStopNode()).append(",").append(vehicleData.getTargetNode()).append(")");
			log.append(" Carrier:").append(vehicleData.getCarrierExist());
			log.append(" CmdStatus:").append(cmdState.toConstChar()).append("(P:").append(vehicleData.getPrevCmd()).append(" C:").append(vehicleData.getCurrCmd()).append(" N:").append(vehicleData.getNextCmd()).append(" V:").append(vehicleCommCommand.getCommandId()).append(")");
			log.append(" Error:").append(vehicleData.getErrorCode());
			log.append(" LocalGroup:").append(vehicleData.getLocalGroupId());
			log.append(" PauseType:").append(vehicleData.getPauseType());
			
			if (operationControlState != OPERAION_CONTROL_STATE.START) {
				log.append(" ControlState:").append(operationControlState.toConstString());
			}
			
			// 2013.02.15 by KYK
			if (vehicleData.getCurrStation().length() > 0) {
				log.append(" CurrStation:").append(vehicleData.getCurrStation());
			}
			// 2013.04.12 by KYK
			log.append(" CurrNodeOffset:").append(vehicleData.getCurrNodeOffset());
			
			// 2013.09.24 by KYK
			log.append(" Speed:").append(vehicleData.getVehicleSpeed());
			
			// 2011.11.02 by PMM
			if (vehicleData.isActionHold()) {
				log.append(" ActionHold:TRUE");
			}
			
			// 2012.04.09 by PMM
			if (vehicleData.getAlarmCode() > 0) {
				log.append(" AlarmCode:" + vehicleData.getAlarmCode());
			}
			
			// 2012.02.09 by PMM
			if (vehicleData.isLocateRequested()) {
				log.append(" LocateRequested:TRUE");
			}
			
			// 2014.02.21 by MYM : [Stage Locate 기능]
			if (vehicleData.isStageRequested()) {
				log.append(" StageRequested:TRUE");
			}
			
			// 2012.12.06 by MYM
			if (vehicleData.isAvExist()) {
				log.append(" AvExist:TRUE");
			}
			
			if (PATROL.equals(vehicleData.getZone())) {
				if (vehicleData.getPatrolStatus() == '1') {
					// Patrol Mode가 저속 주행인 경우 (Clean+Vision, Slope)
					log.append(" Patrolling");
				}
				if (vehicleData.getTemperatureLevel() == '1') {
					log.append(" TemperatureWarningLevel");
					setAlarmCode(OcsAlarmConstant.WARNING_LEVEL_TEMPERATURE);
				} else {
					if (vehicleData.getAlarmCode() == OcsAlarmConstant.WARNING_LEVEL_TEMPERATURE) {
						unregisterAlarm(OcsAlarmConstant.WARNING_LEVEL_TEMPERATURE);
					}
				}
			}
			
			// 22.12.28 by JJW for CarrierType 로그 추가
			if (vehicleData.getCarrierType() > 0){
//			if (vehicleData.getCarrierExist() == '1' && vehicleData.getCarrierType() != 100){
				log.append(" CarrierType:" + CarrierTypeConfig.getInstance().getMaterialType(vehicleData.getCarrierType()));
			}
			
			// 2016.08.16 by KBS : Unload/Load 보고 시점 개선
			if (vehicleData.getTransStatus() == 0x01) {
				log.append(" TransStatus:1");
			}
			
			if (isSystemPaused) {
				log.append(" SystemPauseRequested");
			}
			
			// 2015.06.07 by MYM : YieldState 정보 출력
			if (vehicleData.getYieldState() != 'N') {
				log.append(" YieldState:").append(vehicleData.getYieldState());
			}
			
			traceOperation(log.toString());

			// RFRead Error시 기록
			if (OperationConstant.RF_ERROR.equals(vehicleData.getRfData())) {
				traceRFReadError(log.toString());
				
				registerVehicleErrorHistory(0, "RF Read Data:" + vehicleData.getRfData(), OperationConstant.VEHICLEERROR_RFREAD_ERROR);
			}

			if (nodeUpdated == false && operationControlState != OPERAION_CONTROL_STATE.INIT &&
					vehicleData.getVehicleMode() == 'A' && vehicleData.getState() != 'P' && vehicleData.getState() != 'V') {
				if ('I' == vehicleData.getState()) {
					setAlarmCode(OcsAlarmConstant.ESTOP_BY_VEHICLE_INIT_FAIL);
				} else {
					setAlarmCode(OcsAlarmConstant.ESTOP_BY_VEHICLE_DRIVE_FAIL);
				}
				vehicleCommCommand.setCommandId(0);
				
				// 2012.03.05 by PMM
//				  - $P1 / $p1A
//				   :OCS 경로에서 벗어난 위치로 OHT가 주행하는 경우 (탈선)
				sendEStopCommand(1);
				
				StringBuilder message = new StringBuilder();
				Node tempNode = null;
				message.append("DrivedNodeList[");
				message.append(vehicleData.getDriveNodeCount());
				message.append("]: ");
				for (int i = 0; i < vehicleData.getDriveNodeCount(); i++) {
					tempNode = vehicleData.getDriveNode(i);
					if (tempNode != null) {
						if (i > 0) {
							message.append(",");
						}
						message.append(tempNode.getNodeId());
					}
				}
				traceOperation(message.toString());
				
			} else {
				switch (vehicleData.getAlarmCode()) {
					case OcsAlarmConstant.ESTOP_BY_VEHICLE_INIT_FAIL:
						unregisterAlarm(OcsAlarmConstant.ESTOP_BY_VEHICLE_INIT_FAIL);
						break;
					case OcsAlarmConstant.ESTOP_BY_VEHICLE_DRIVE_FAIL:
						unregisterAlarm(OcsAlarmConstant.ESTOP_BY_VEHICLE_DRIVE_FAIL);
						break;
					default:
						break;
				}
			}

			if (vehicleData.getVehicleMode() != 'A' || (vehicleData.getState() != 'V' && vehicleData.getState() != 'Z') || cmdState == COMMAND_STATE.TIMEOUT) {
				// 2011.11.01 by PMM : AV상태에서 IDReset Command 중복 전송 방지
				// 2015.12.21 by KBS : AZ상태에서 IDReset Command 중복 전송 방지
				isIDResetCommandSent = false;
			}
			if (vehicleData.getVehicleMode() != 'A' || (vehicleData.getState() == 'I')) {
				// 2016.01.20 by KBS : AI상태에서 false 전환
				isPatrolCancelCommandSent = false;
			}
			
			if (cmdState == COMMAND_STATE.TIMEOUT) {
				if (vehicleComm.getLastSentCommand() == COMMAND_TYPE.ESTOP) {
					vehicleCommCommand.setCommandId(0);
					sendEStopCommand(lastSentEstopType);
				} else if (vehicleComm.getLastSentCommand() == COMMAND_TYPE.IDRESET) {
					vehicleCommCommand.setCommandId(0);
					sendIDResetCommand();
				}
			}

			// 초기 Operation 구동시 Vehicle과 통신 완료시 Drive 정리 처리
			initializeControlState(commData);

			commData.setReceivedState(false);
		}
	}
	
	/**
	 * Initialize ControlState
	 * 
	 * @param commData
	 */
	private void useTrafficLog(VehicleCommData commData) {
		// 2012.07.24 by PMM
		// VehicleTraffic Trace를 위한 로그
		if (isAllOperationReady &&
				(activeOperationMode.getOperationMode() == OPERATION_MODE.GO ||
				activeOperationMode.getOperationMode() == OPERATION_MODE.WORK)) {
			if (commData.getState() == 'A' ||
					commData.getState() == 'U' ||
					commData.getState() == 'L' ||
					commData.getState() == 'I') {
				if (wasStopped == false) {
					stoppedTime = System.currentTimeMillis();
				}
				wasStopped = true;
				if (commData.getState() == 'U' ||
						commData.getState() == 'L') {
					wasStoppedForWork = true;
				} else {
					wasStoppedForWork = false;
				}
			} else if (commData.getState() == 'G') {
				if (wasStopped) {
					long blockTime = (System.currentTimeMillis() - stoppedTime);
					StringBuilder message = new StringBuilder();
					
					if (wasStoppedForWork) {
						message.append("[W] ");
					} else {
						message.append("[G] ");
					}
					message.append("CurrNode=").append(vehicleData.getCurrNode()).append(", ");
					message.append("BlockedTime=").append(blockTime).append(", ");
					if (trCmd != null) {
						message.append("TrCmdId=").append(trCmd.getTrCmdId()).append(", ");
						message.append("DetailStatus=").append(trCmd.getDetailState().toConstString());
					} else {
						message.append("TrCmdId=, ");
						message.append("DetailStatus=");
					}
					traceVehicleTraffic(message.toString());
				}
				wasStopped = false;
				wasStoppedForWork = false;
			}
		} else {
			wasStopped = false;
			stoppedTime = System.currentTimeMillis();
		}
	}
	
	/**
	 * 2013.05.27 by MYM
	 * 
	 * @param commData
	 * @return
	 */
	private String getValidCurrStation(VehicleCommData commData) {
		String currStation = commData.getCurrStationId();
		if (currStation == null || currStation.length() == 0 || currStation.equals("000000") || currStation.equals("0")) {
			return "";
		}
		
		Station station = stationManager.getStation(currStation);
		if (station == null) {
			traceOperationException("Invalid CurrStation(Unregistered or offset<0(Not Teaching Yet)) is Reported but Ignored : " + currStation);
			return "";
		}
		
		return currStation;
	}
	
	/**
	 * 2013.05.27 by MYM 
	 * @return
	 */
	private boolean checkNodeAndStationMismatch() {
		// 2013.04.12 by KYK : currNode - currStation Not Matched : Alarm
		Station station = stationManager.getStation(vehicleData.getCurrStation());
		if (station != null && 'A' == vehicleData.getVehicleMode()) {
			if (vehicleData.getCurrNode().equals(station.getParentNodeId()) == false) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 2013.09.06 by KYK
	 * @param commData
	 * @return
	 */
	private boolean isVaildStationButNodeNotExist(VehicleCommData commData) {
		if ("0".equals(commData.getCurrNode())) {
			String currStationId = getValidCurrStation(commData);
			if (currStationId != null && currStationId.length() > 0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Initialize ControlState
	 * 
	 * @param commData
	 */
	private void initializeControlState(VehicleCommData commData) {
		// 1. 통신으로 올라온 CurrNode, StopNode로 Drive 정리
		if (operationControlState == OPERAION_CONTROL_STATE.INIT) {
			// 2014.10.15 : DB 와 통신으로 올라온 데이터가 다를 경우 잠시 대기
			if (vehicleData.getStopNode().equals(commData.getStopNode())
					|| vehicleData.getCurrCmd() == 0
					|| vehicleData.getVehicleMode() == 'M') {
				initializeControlInitState(commData);
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("Waiting for initializeControlInitState - ");
				sb.append("Stopnode(ocs:").append(vehicleData.getStopNode());
				sb.append(",vehicle:").append(commData.getStopNode()).append(")");
				traceOperation(sb.toString());
			}
			// 2015.05.01 by KYK : commfail mcs 보고
			if (isCommfailAlarmReported) {
				isCommfailAlarmReported = false;
				clearAlarmReport(OcsConstant.COMMUNICATION_FAIL);
				traceOperation("Send ClearAlarmReport...");
				// 2022.03.30 by JJW Commfail Vehicle Error History 기록
				resetFromVehicleErrorHistory();
			}
		}
		
		// 2. 초기 구동시 통신 연결 후  Manaul or Vehicle의 현재 명령(CurrCmd) 완료한 경우  operationControlMode를 Start로 변경
		if (operationControlState == OPERAION_CONTROL_STATE.READY &&
				(vehicleData.getCurrCmd() == 0 || vehicleData.getVehicleMode() == 'M')) {
			initializeControlReadyState();
		}
		
		// 2011.10.25 by PMM
		// INIT/READY 상태일 때도, AV이면 SendIDReset을 하기 위해
		checkAutoRecovery();
	}
	
	/**
	 * Check AutoRecovery
	 */
	private void checkAutoRecovery() {
		if (operationControlState == OPERAION_CONTROL_STATE.INIT ||
				operationControlState == OPERAION_CONTROL_STATE.READY) {
			// InitializeControlState()에서만 호출.
			// AutoRecovery 확인
			if (vehicleData.getVehicleMode() == 'A' && vehicleData.getState() == 'V') {

				sendIDResetCommand();

				// OperationMode(I->S) by Auto Position/Auto Recovery
				if (vehicleData.getCurrNode().equals(vehicleData.getTargetNode())) {
					changeOperationMode(OPERATION_MODE.SLEEP, "Auto Recovery");
				} else {
					changeOperationMode(OPERATION_MODE.SLEEP, "Auto Position");
				}
			}
		}
	}
	
	/**
	 * 2014.12.19 by KYK
	 * @param commData
	 */
	private boolean initializeVehiclePath(VehicleCommData commData, String reason) {
		Node currNode = null;
		Node stopNode = null;
		Station currStation = null;
		Station stopStation = null;
		
//		VehicleCommData commData = vehicleComm.getVehicleCommData();
		currNode = nodeManager.getNode(commData.getCurrNode());
		stopNode = nodeManager.getNode(commData.getStopNode());
		currStation = stationManager.getStation(commData.getCurrStationId());
		stopStation = stationManager.getStation(commData.getStopStationId());
		
		StringBuilder log = new StringBuilder();
		log.append("initializeControlInitState");
		log.append("/currNode:").append(currNode).append("/StopNode:").append(stopNode);
		log.append("/currStation:").append(currStation).append("/StopStation:").append(stopStation);
		log.append("/CurrCmd:").append(vehicleData.getCurrCmd());
		
		if (vehicleData.getVehicleMode() == 'A' && vehicleData.getCurrCmd() != 0) {
			if (stopNode != null) {
				if (stopStation != null) {
					vehicleData.setStop(stopNode.getNodeId(), stopStation.getStationId());
					log.append("/setStop(").append(stopNode).append(",").append(stopStation).append(")");
				} else {
					log.append("/setStop(").append(stopNode).append(",'')");
					vehicleData.setStop(stopNode.getNodeId(), "");
				}				
			} else {
				vehicleCommCommand.setCommandId(0);
				sendEStopCommand(0);
				log.append("/AbnormalCase#1");
				traceOperation(log.toString());
				return false;
			}
		} else {
			if (stopNode != null) {
				if (currStation != null && stopStation != null) {
					vehicleData.setStop(stopNode.getNodeId(), stopStation.getStationId());
					log.append("/setStop(").append(stopNode).append(",").append(stopStation).append(")");
				} else if (currStation != null && stopStation == null) {
					vehicleData.setStop(stopNode.getNodeId(), currStation.getStationId());  // ?
					log.append("/setStop(").append(stopNode).append(",").append(currStation).append(")");
				} else if (currStation == null && stopStation == null) {
					vehicleData.setStop(stopNode.getNodeId(), "");
					log.append("/setStop(").append(stopNode).append(",'')");
				} else {
					vehicleCommCommand.setCommandId(0);
					sendEStopCommand(0);
					log.append("/AbnormalCase#2");
					traceOperation(log.toString());
					return false;
				}
			} else {
				// 2015.03.27 by MYM : currNode가 Null인 조건 추가 
				// 배경 : RuntimeUpdate시 Manual OHT인 경우 node를 0으로 올려줌 → Exception 발생
				if (currNode != null && currStation != null && stopStation != null) {
					vehicleData.setStop(currNode.getNodeId(), currStation.getStationId());
					log.append("/setStop(").append(currNode).append(",").append(currStation).append(")");
				} else if (currNode != null && currStation != null && stopStation == null) {
					vehicleData.setStop(currNode.getNodeId(), currStation.getStationId());
					log.append("/setStop(").append(currNode).append(",").append(currStation).append(")");
				} else if (currNode != null && currStation == null && stopStation == null) {
					vehicleData.setStop(currNode.getNodeId(), "");
					log.append("/setStop(").append(currNode).append(",'')");
				} else {
					vehicleCommCommand.setCommandId(0);
					sendEStopCommand(0);
					log.append("/AbnormalCase#3");
					traceOperation(log.toString());
					return false;
				}
			}
		}
		
		if (vehicleData.getPauseType() == 2 || vehicleData.getPauseType() == 3) {
			sendResumeCommand();
			vehicleData.setPauseRequestVehicle(null);
			this.traceOperation("initializeControlInitState : Send Resume");
		}
		
		addVehicleToUpdateList();
		// 2015.01.08 by MYM : StopNode가 invalid인 경우 추가 log 기록
//		pathSearch.initializeVehiclePath(vehicleData, "ControlInit(Invalid Node)");		
		pathSearch.initializeVehiclePath(vehicleData, reason + (stopNode == null ? "(Invalid Node)" : ""));		
		traceOperation(log.toString());
		return true;		
	}
	
	/**
	 * 2013.10.24 by KYK
	 */
	private void initializeCommandState() {
		// 1) Vehicle Command 정리
		if (vehicleData.getNextCmd() != 0) {
			vehicleCommCommand.setCommandId(vehicleData.getNextCmd());
		} else if (vehicleData.getCurrCmd() != 0) {
			vehicleCommCommand.setCommandId(vehicleData.getCurrCmd());
		} else {
			vehicleCommCommand.setCommandId(vehicleData.getPrevCmd());
		}
		
		// 2) Vehicle로 부터 받은 CommandID로 cmdState를 정리
		if (vehicleData.getCurrCmd() == 0) {
			if (vehicleData.getPrevCmd() == 0) {
				cmdState = COMMAND_STATE.READY;
			} else {
				cmdState = COMMAND_STATE.EXECUTED;
			}
		} else {
			cmdState = COMMAND_STATE.EXECUTING;
		}		
	}
	
	/**
	 * Initialize ControlInitState
	 * 
	 * @param commData
	 */
	private void initializeControlInitState(VehicleCommData commData) {

		// 2013.10.24 by KYK
		initializeCommandState();
//		// 1) Vehicle Command 정리
//		if (vehicleData.getNextCmd() != 0) {
//			vehicleCommCommand.setCommandId(vehicleData.getNextCmd());
//		} else if (vehicleData.getCurrCmd() != 0) {
//			vehicleCommCommand.setCommandId(vehicleData.getCurrCmd());
//		} else {
//			vehicleCommCommand.setCommandId(vehicleData.getPrevCmd());
//		}
//		
//		// 2) Vehicle로 부터 받은 CommandID로 cmdState를 정리
//		if (vehicleData.getCurrCmd() == 0) {
//			if (vehicleData.getPrevCmd() == 0) {
//				cmdState = COMMAND_STATE.READY;
//			} else {
//				cmdState = COMMAND_STATE.EXECUTED;
//			}
//		} else {
//			cmdState = COMMAND_STATE.EXECUTING;
//		}
		
		// 3) AutoRecovery 확인
		// 2011.10.25 by PMM 위치 이동.
//		if (vehicleData.getVehicleMode() == 'A' && vehicleData.getState() == 'V') {
//			vehicleCommCommand.setCommandId(0);
//			sendIDResetCommand();
//		}

		// 4) Vehicle로 부터 올라온 StopNode로 Drive 정리 
		// 2014.12.19 by KYK
//		if (initializeControlInitState() == false) {
		if (initializeVehiclePath(commData, "ControlInit") == false) {
			return;
		}
		
		// 2016.04.22 by LSH : DB(M/E CurrNode != StopNode) → VehicleComm(M/I CurrNode=StopNode) 정보로 Initialize 할 경우,
        //                     이전에 차단했던 Section 해제 조건 추가
		// 2015.02.11 by MYM : 장애 지역 우회 기능
//		if (vehicleData.getAbnormalReason() == DETOUR_REASON.VEHICLE_COMMFAIL) {
		if (vehicleData.getAbnormalReason() == DETOUR_REASON.VEHICLE_COMMFAIL || vehicleData.getAbnormalReason() == DETOUR_REASON.VEHICLE_ERROR) {
			vehicleData.releaseAbnormalSection();
		}
		
		// 상태 변경  INIT -> READY
		operationControlState = OPERAION_CONTROL_STATE.READY;
		
//		// 2011.12.02 by MYM : PauseType 3 추가
//		// 2011.11.07 by MYM : Pause 명령 전송후 재시작을 했을 때 Resume 명령 전송
//		if (vehicleData.getPauseType() == 2 || vehicleData.getPauseType() == 3) {
//			sendResumeCommand();
//			vehicleData.setPauseRequestVehicle(null);
//			this.traceOperation("initializeControlInitState : Send Resume");
//		}
	}
	
	/**
	 * 2013.04.12 by MYM
	 * 재시작 및 통신 연결시 Drive 초기화
	 *  
	 * @return
	 */
	@Deprecated
	private boolean initializeControlInitState() {
		Node currNode = null;
		Node stopNode = null;
		Station currStation = null;
		Station stopStation = null;
		
		VehicleCommData commData = vehicleComm.getVehicleCommData();
		currNode = nodeManager.getNode(commData.getCurrNode());
		stopNode = nodeManager.getNode(commData.getStopNode());
		currStation = stationManager.getStation(commData.getCurrStationId());
		stopStation = stationManager.getStation(commData.getStopStationId());
		
		StringBuilder log = new StringBuilder();
		log.append("initializeControlInitState");
		log.append("/currNode:").append(currNode).append("/StopNode:").append(stopNode);
		log.append("/currStation:").append(currStation).append("/StopStation:").append(stopStation);
		log.append("/CurrCmd:").append(vehicleData.getCurrCmd());
		
		if (vehicleData.getVehicleMode() == 'A' && vehicleData.getCurrCmd() != 0) {
			if (stopNode != null) {
				if (stopStation != null) {
					vehicleData.setStop(stopNode.getNodeId(), stopStation.getStationId());
					log.append("/setStop(").append(stopNode).append(",").append(stopStation).append(")");
				} else {
					log.append("/setStop(").append(stopNode).append(",'')");
					vehicleData.setStop(stopNode.getNodeId(), "");
				}				
			} else {
				vehicleCommCommand.setCommandId(0);
				sendEStopCommand(0);
				log.append("/AbnormalCase#1");
				traceOperation(log.toString());
				return false;
			}
		} else {
			if (stopNode != null) {
				if (currStation != null && stopStation != null) {
					vehicleData.setStop(stopNode.getNodeId(), stopStation.getStationId());
					log.append("/setStop(").append(stopNode).append(",").append(stopStation).append(")");
				} else if (currStation != null && stopStation == null) {
					vehicleData.setStop(stopNode.getNodeId(), currStation.getStationId());  // ?
					log.append("/setStop(").append(stopNode).append(",").append(currStation).append(")");
				} else if (currStation == null && stopStation == null) {
					vehicleData.setStop(stopNode.getNodeId(), "");
					log.append("/setStop(").append(stopNode).append(",'')");
				} else {
					vehicleCommCommand.setCommandId(0);
					sendEStopCommand(0);
					log.append("/AbnormalCase#2");
					traceOperation(log.toString());
					return false;
				}
			} else {
				if (currStation != null && stopStation != null) {
					vehicleData.setStop(currNode.getNodeId(), currStation.getStationId());
					log.append("/setStop(").append(currNode).append(",").append(currStation).append(")");
				} else if (currStation != null && stopStation == null) {
					vehicleData.setStop(currNode.getNodeId(), currStation.getStationId());
					log.append("/setStop(").append(currNode).append(",").append(currStation).append(")");
				} else if (currStation == null && stopStation == null) {
					vehicleData.setStop(currNode.getNodeId(), "");
					log.append("/setStop(").append(currNode).append(",'')");
				} else {
					vehicleCommCommand.setCommandId(0);
					sendEStopCommand(0);
					log.append("/AbnormalCase#3");
					traceOperation(log.toString());
					return false;
				}
			}
		}
		
		addVehicleToUpdateList();							
		pathSearch.initializeVehiclePath(vehicleData, "ControlInit(Invalid Node)");		
		traceOperation(log.toString());
		return true;
	}	
	
	/**
	 * 2013.09.06 by KYK
	 * @return
	 */
	public boolean checkCarrierTypeMismatch() {
		// 2015.03.12 by MYM : OCS3.1 라인은 미 체크 (Vehicle이 CarrierType을 올려주지 않음)
		if (vehicleCommType == VEHICLECOMM_TYPE.VEHICLECOMM_CHAR) {
			return false;
		}
		// 2015.07.07 by KYK : OHT,OCS 동시 패치 필요한 경우 OHT 가동중지를 피하기 위함
		if (isCarrierTypeMismatchUsed) {
			CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(trCmd.getSourceLoc());
			int sourceLocType = getCarrierType(carrierLoc.getMaterial());
			int carrierType = vehicleData.getCarrierType();
			
			if (carrierType != sourceLocType) {
				setAlarmCode(OcsAlarmConstant.ESTOP_BY_CARRIER_TYPE_MISMATCH);
				vehicleCommCommand.setCommandId(0);
				sendEStopCommand(7);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check Carrier Mismatched On Unload Port
	 * 
	 * @return
	 */
	public boolean checkCarrierMismatchedOnUnloadPort() {
		if (isCarrierMismatchedOnUnloadPort(getCarrierLocType(trCmd.getSourceLoc()), trCmd.getCarrierId(), vehicleData.getRfData())) {
			if (isAutoMismatchRecoveryMode) {
				// Case1. [STK Port-1: CarrierA, Port-2: CarrierB]
				//        [MCS Transfer(CarrierA: STK Port-1 -> DestPort)]
				//        [OCS Unload시 ID Read : CarrierC or CarrierB]
				// Case2. [STK Port-1: CarrierA, Port-2: CarrierB]
				//        [MCS Transfer(CarrierA: STK Port-1 -> DestPort)]
				//        [OCS Unload시 ID Read : CarrierC, OCS내 CarrierC가 존재]
				StringBuilder message = new StringBuilder();
				message.append("Unload CarrierMismatch(Auto) - Port:").append(trCmd.getSourceLoc());
				message.append(", CarrierID:").append(trCmd.getCarrierId());
				message.append(", RFData:").append(vehicleData.getRfData());
				traceOperation(message.toString());

				StringBuilder event = new StringBuilder();
				event.append("Vehicle:").append(vehicleData.getVehicleId());
				event.append(", Mode:").append(OcsInfoConstant.AUTO);
				event.append(", Port:").append(trCmd.getSourceLoc());
				event.append(", CarrierId:").append(trCmd.getCarrierId());
				event.append(", RFData:").append(vehicleData.getRfData());

				registerEventHistory(
						new EventHistory(EVENTHISTORY_NAME.UNLOAD_CARRIER_MISMATCH,
								EVENTHISTORY_TYPE.SYSTEM, "", event.toString(), "", "",
								EVENTHISTORY_REMOTEID.OPERATION, "",
								EVENTHISTORY_REASON.NULL), false);
				
				// 1. 현재 반송명령은 비정상 완료 보고 위한 TrCmd 상태 변경
//				m_strTrCmdStatus = "CMD_PAUSED";
//				m_strDetailTrCmdStatus = "UNLOADED";
//				UpdateTrCmdStatus(m_strTrCmdID, null, m_strCarrierLoc, m_strTrCmdStatus, m_strDetailTrCmdStatus);

//				// 2. 현재 반송명령(MCS CarrierID)의 CarrierRemoved 보고(to MSC)
//				m_strCarrierLoc = m_strVehicleLoc;
//				SendS6F11("Carrier", "CarrierRemoved", 0);

//				// 3. 현재 반송명령(MCS CarrierID)의 TransferCompleted(Result=1, CarrierLoc="") 보고
//				//    ※ CarrierLoc를 ""로 보고하는 이유
//				//       STK -> STK 반송인 경우 CarrierLoc을 Dest로 줄 경우 Class MCS에서 반송명령 정리 못함.
//				m_strCarrierLoc = "";
//				SendS6F11("TrCmd", "TransferCompleted", 1);

//				// 4. History를 DB에 저장
//				//    ※ Unloaded까지의 시간은 저장했다가 OHT로 부터 올라온 Carrier에 대한 반송명령에 반영
//				String sTrCmdQueuedTime = m_strTrCmdQueuedTime;
//				String sTrCmdUnloadAssignedTime = m_strTrCmdUnloadAssignedTime;
//				String sTrCmdUnloadingTime = m_strTrCmdUnloadingTime;
//				String sTrCmdUnloadedTime = m_strTrCmdUnloadedTime;
//				UpdateTrCompletionHistoryDB();
//				m_strTrCmdQueuedTime = sTrCmdQueuedTime;
//				m_strTrCmdUnloadAssignedTime = sTrCmdUnloadAssignedTime;
//				m_strTrCmdUnloadingTime = sTrCmdUnloadingTime;
//				m_strTrCmdUnloadedTime = sTrCmdUnloadedTime;

//				// 5. Vehicle로 부터 올라온 RF Data(실제 CarrierID) 반송 진행(Abort)을 위한 TrCmd 상태 변경
//				//    . CarrierLoc를 Vehicle Port로 변경
//				//    . TrCmd 상태를 "CMD_ABORTED"로 변경
//				//    . RemoteCmd를 "ABORT"로 변경
//				m_strCarrierLoc = m_strVehicleLoc;
//				m_lLastAbortedTime = System.currentTimeMillis();
//				m_strTrCmdStatus = "CMD_ABORTED";
//				UpdateTrCmdStatus(m_strTrCmdID, null, m_strCarrierLoc, m_strTrCmdStatus, null);
//				UpdateRequestedCmd("ABORT");

//				// 6. OHT로부터 올라온 RF Data(실제 CarrierID)가 현재 다른 반송명령에 존재(DUP)하는지 체크
//				if (checkDuplicationInTrCmd(strRFData) == true)
//				{
//					// 6-1. CarrierID를 UNKNOWNDUP + strRFReadData(실제 CarrierID) 로 변경
//					UpdateCarrierID("UNKNOWNDUP_" + strRFData);
//				}
//				else
//				{
//					// 6-2. CarrierID를 Vehicle로 부터 올라온 RF Data(실제 CarrierID)로 변경
//					UpdateCarrierID(strRFData);
//				}

//				// 7. TrCmdID 변경(기존 TrCMDID + 날짜) - OHT로부터 올라온 RF Data(실제 CarrierID) 비정상완료 보고시 기존 TrCmdID와 중복이면 안됨.
//				String strTempTrCmdID = m_strTrCmdID;
//				m_strTrCmdID = m_strTrCmdID + "_" + m_DBAccessManager.GetCurrTimeStr();

//				// 8. 실제 CarrierID 반송명령 TransferCompleted(CarrierID=CarrierC or CarrierB or UNKNOWNDUP, Result=1, CarrierLoc=OHT) 보고
//				SendS6F11("TrCmd", "TransferCompleted", 1);

//				// 9. 비정상 완료 보고후 원래의 TrCmdID로 변경
//				m_strTrCmdID = strTempTrCmdID;
			} else {
				trCmd.setState(TRCMD_STATE.CMD_PAUSED);
				StringBuilder event = new StringBuilder();
				event.append("Vehicle:").append(vehicleData.getVehicleId());
				event.append(", Mode:").append(OcsInfoConstant.MANUAL);
				event.append(", Port:").append(trCmd.getSourceLoc());
				event.append(", CarrierId:").append(trCmd.getCarrierId());
				event.append(", RFData:").append(vehicleData.getRfData());

				registerEventHistory( new EventHistory(EVENTHISTORY_NAME.UNLOAD_CARRIER_MISMATCH,
						EVENTHISTORY_TYPE.SYSTEM, "", event.toString(), "", "",
						EVENTHISTORY_REMOTEID.OPERATION, "",
						EVENTHISTORY_REASON.NULL), false);

				setAlarmCode(OcsAlarmConstant.ESTOP_BY_UNLOAD_CARRIER_MISMATCH);

				sendEStopCommand(5);
				traceOperation("OCS sent E-StopCommand to OHT.");
				addVehicleToUpdateList();
				addTrCmdToStateUpdateList();
				pauseTrCmd(true, TrCmdConstant.CARRIER_MISMATCH, -1);
			}
			return true;
		}
		
		return false;
	}
	
	public boolean checkMissedCarrierOnUnloadPort() {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (isUnloadErrorReportUsed &&
					isMissedCarrierCheckUsed && trCmd != null) {
				TrCmd duplicatedTrCmd = trCmdManager.getSourceLocDuplicatedTrCmdFromDB(trCmd);
				if (duplicatedTrCmd != null) {
					// TrCmd 1번 (Carrier A)에 대해 할당 받은 VHL이 Unload하기 전에
					// 작업자가 Manual로 Carrier A를 제거한 뒤, Carrier B가 WaitOut되어 동일 Port에 대해 Unloaded 이전에 중복 TrCmd (TrCmd 1번과 TrCmd 2번) 있는 경우
					// Carrier A에 대한 반송 명령을 비정상 완료 보고 처리
					// Carrier B에 대한 반송 명령을 비정상 완료 보고 처리
					// VHL은 Carrier를 떠서, Unknown Carrier 등록 처리
					String unloadAssignedTime;
					String unloadingTime;
					String unloadedTime;
					if (trCmd.getUnloadAssignedTime().compareTo(duplicatedTrCmd.getTrQueuedTime()) <= 0) {
						unloadAssignedTime = duplicatedTrCmd.getTrQueuedTime();
					} else {
						unloadAssignedTime = trCmd.getUnloadAssignedTime();
					}
					if (trCmd.getUnloadingTime().compareTo(duplicatedTrCmd.getTrQueuedTime()) <= 0) {
						unloadingTime = duplicatedTrCmd.getTrQueuedTime();
					} else {
						unloadingTime = trCmd.getUnloadingTime();
					}
					unloadedTime = getCurrDBTimeStr();
					
					StringBuilder event = new StringBuilder();
					event.append("Vehicle:").append(vehicleData.getVehicleId());
					event.append(", Port:").append(trCmd.getSourceLoc());
					event.append(", Carrier A:").append(trCmd.getCarrierId());
					event.append(", Carrier B:").append(duplicatedTrCmd.getCarrierId());

					registerEventHistory( new EventHistory(EVENTHISTORY_NAME.MISSED_CARRIER,
							EVENTHISTORY_TYPE.SYSTEM, "", event.toString(), "", "",
							EVENTHISTORY_REMOTEID.OPERATION, "",
							EVENTHISTORY_REASON.NULL), false);
					
					String missedTrCmdId = trCmd.getTrCmdId();
					String sourceLoc = trCmd.getSourceLoc();
					String duplicatedTrCmdId = duplicatedTrCmd.getTrCmdId();
					
					StringBuilder message = new StringBuilder();
					message.append("Missed Carrier on ");
					message.append(sourceLoc).append("(N").append(trCmd.getSourceNode()).append(")");
					message.append(". Might be Missed(").append(missedTrCmdId).append("/").append(trCmd.getCarrierId()).append(")");
					message.append(", Duplicated(").append(duplicatedTrCmdId).append("/").append(duplicatedTrCmd.getCarrierId()).append(")");
					traceOperation(message.toString());

					// Carrier A에 대한 비정상 완료 보고 처리
					trCmd.setCarrierLoc(trCmd.getSourceLoc());
					addTrCmdToStateUpdateList();
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_MISSED_CARRIER);
					
					trCmd.setDeletedTime(getCurrDBTimeStr());
					registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
					
					// Carrier B에 대한 비정상 완료 보고 처리
					trCmd = duplicatedTrCmd;
					vehicleData.setAssignedVehicle(trCmd != null);
					trCmd.setVehicle(vehicleData.getVehicleId());
					trCmd.setAssignedVehicleId(vehicleData.getVehicleId());
					
					trCmd.setState(TRCMD_STATE.CMD_WAITING);
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
					trCmd.setCarrierLoc(trCmd.getSourceLoc());
					trCmd.setUnloadAssignedTime(unloadAssignedTime);
					trCmd.setUnloadingTime(unloadingTime);
					trCmd.setUnloadedTime(unloadedTime);
					
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_INITIATED, 0);
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);
					
					try {
						sleep(missedCarrierCheckSleep);
					} catch (Exception e) {}
					
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ARRIVED, 0);
					trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_SENT);
					
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFERRING, 0);
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRESTARTED, 0);
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADING);
					
					if (isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc()))) {
						if (isSTBCUsed()) {
							sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRECOMPLETED, 0);
							// 18.03.12 LSH: trCmd 상태 정리 위치 변경
							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
							trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
							trCmd.setUnloadAssignedTime(unloadAssignedTime);
							trCmd.setUnloadingTime(unloadingTime);
							trCmd.setUnloadedTime(unloadedTime);
							addTrCmdToStateUpdateList();
						}
						if (checkSTBUTBCarrierMismatchOnUnloadPort()) {
							// 18.03.12 LSH: trCmd 상태 정리 위치 변경
//							trCmd.setUnloadAssignedTime(unloadAssignedTime);
//							trCmd.setUnloadingTime(unloadingTime);
//							trCmd.setUnloadedTime(unloadedTime);
//							addTrCmdToStateUpdateList();
							// 18.03.12 LSH: STB/UTB Port 작업인 경우, A 반송만 삭제 (B 반송은 ABORT 상태로 유지=후속 반송 기준으로 정리)
							trCmdManager.deleteSTBUTBMissedCarrierTrCmd(missedTrCmdId, sourceLoc);
							return true;
						}
					}
					
					try {
						sleep(missedCarrierCheckSleep);
					} catch (Exception e) {}
					
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_MISSED_CARRIER);
					
					trCmd.setDeletedTime(getCurrDBTimeStr());
					registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
					resetTrCmd();

					// Unknown Carrier 생성
					createUnknownTrCmd();
					
					// Carrier A와 B에 대한 반송 명령 삭제. (A를 B보다 먼저 삭제하면, B 반송이 할당이 될 수 있음.)
					// Unknown Carrier 등록
					if (trCmdManager.deleteMissedCarrierTrCmdsAndRegisterUnknownTrCmd(trCmd, missedTrCmdId, duplicatedTrCmdId, sourceLoc)) {
						cancelNextAssignedTrCmd(EVENTHISTORY_REASON.UNKNOWN_TRCMD_REGISTERED);
						sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
						
						trCmd.setUnloadAssignedTime(unloadAssignedTime);
						trCmd.setUnloadingTime(unloadingTime);
						trCmd.setUnloadedTime(unloadedTime);
						addTrCmdToStateUpdateList();
					}
					return true;
				}
			}
		}
		return false;
	}
	
	
	public boolean checkSTBUTBCarrierMismatchOnUnloadPort() {
		if (isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc()))) {
			if (isSTBCUsed()) {
				if (updateCarrierStateInSTB(OperationConstant.REMOVE, trCmd.getSourceLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), "") == false) {
					trCmd.setState(TRCMD_STATE.CMD_PAUSED);
					// 2012.08.08 by KYK : [TransferPaused] 
					// 변경전 : Operation TransferPaused 별도요청없음 (IBSEM setAlarmReport 처리시 TransferPaused 같이 보고함) -> 보고 아이템 누락 발생함
					// 변경후 : Operation TransferPaused 개별요청처리 (IBSEM setAlarmReport 처리시 TransferPaused 처리 부분도 제거함)
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_PAUSED, 0);									

					setAlarmReport(999);
					clearAlarmReport(999);
					pauseTrCmd(true, TrCmdConstant.CARRIER_MISMATCH, trCmd.getPauseCount());
					trCmd.setLastAbortedTime(System.currentTimeMillis());
					trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
					trCmd.setState(TRCMD_STATE.CMD_ABORTED);
					addTrCmdToStateUpdateList();
					// 2012.11.30 by KYK : ResultCode 세분화
//					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_STBUNLOAD_CARRIERMISMATCH);
					
					// OperationMode(W->I) by Abnormal Unload Completion
					changeOperationMode(OPERATION_MODE.IDLE, "Abnormal Unload Completion");
					
					// 2013.01.07 by MYM : STB Unload 후 Carrier Mismatch 발생시 알람 표시
					registerAlarm(OcsAlarmConstant.UNLOAD_CARRIER_MISMATCH);
					return true;
				}
			} else {
				updateCarrierStateInSTBWithoutSTBC(OperationConstant.REMOVE, trCmd.getSourceLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), "");
			}
		}
		return false;
	}
	
	/**
	 * Initialize ControlReadyState
	 */
	private void initializeControlReadyState() {
		// 반송명령이 없는 상태에서 Carrier 존재시 UnknownCarrier 보고 처리
		if (trCmd == null) {
			if (vehicleData.isCarrierExist()) {
				if (vehicleData.isAssignHold() == false && isLoadingByPass() == false) {
					registerUnknownTrCmd();
					
					// registerUnknownTrCmd() 내부로 옮김.
//					sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
//					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);
				}
			}
		} else {
			switch (trCmd.getState()) {
				case CMD_CANCELLING: {
					// 2008.10.28 by MYM : OHT Cancel 명령 전송후 OCS Down -> Up하여 Reply 받지 못했을 때
					// Vehicle로 올라온 CmdID와 Status로 판단하여 처리
					// 1. CMD_CANCELLING/A/x,x,3 : OHT Cancel 전송
					// 2. CMD_CANCELLING/A/x,x,3 : TransferCancelFailed
					// 3. CMD_CANCELLING/O/x,x,x : TransferCancelFailed
					// 4. CMD_ABORTING/A/x,x,3 : OHT Cancel 전송
					// 5. CMD_ABORTING/A/x,x,3 : TransferAbortFailed
					// 6. CMD_ABORTING/O/x,x,x : TransferAbortFailed
					if (vehicleData.getState() == 'A' && vehicleData.getNextCmd() != 0) {
						// Step1: StopNode, TargetNode Update
						// 2013.02.15 by KYK
//						vehicleData.setStopNode(vehicleData.getCurrNode());
//						vehicleData.setTargetNode(trCmd.getSourceNode());
						vehicleData.setStop(vehicleData.getCurrNode(), vehicleData.getCurrStation());
						String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
						vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
						
						// Step2: IdleMode로 전환
						if (vehicleData.getVehicleMode() == 'A') {
							// OperationMode(x->I) by Initialize.
							changeOperationMode(OPERATION_MODE.IDLE, "Initialize");
						}
	
						// Step3: Vehicle의 NextCmd 명령이 취소되지 않은 경우, OHT Cancle 전송
						getVehicleCommCommand().setCommandId(0);
						sendCancelCommand(vehicleData.getNextCmd(), 'N');
					} else if ((vehicleData.getState() == 'A' && vehicleData.getNextCmd() == 0)
							|| (vehicleData.getState() == 'O')) {
						// Vehicle의 NextCmd 명령 취소된 경우 : TransferCancelFailed
						// remoteCmdCancelFailed();
					}
					break;
				}
				case CMD_ABORTING: {
					if (vehicleData.getState() == 'A' && vehicleData.getNextCmd() != 0) {
						// Step1: StopNode, TargetNode Update
						// 2013.02.15 by KYK : ??
//						vehicleData.setStopNode(vehicleData.getCurrNode());
//						vehicleData.setTargetNode(trCmd.getDestNode());
						vehicleData.setStop(vehicleData.getCurrNode(), vehicleData.getCurrStation());
						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
						
						// 2011.10.12 by PMM
						// 2011.11.15 by PMM
//						vehicleData.resetDriveNodeList(ocsInfoManager.isNearByDrive());
						
						// 2012.03.06 by PMM
						// resetDriveNodeList 시 currNode 필요.
//						vehicleData.resetDriveNodeList(isNearByDrive);
//						vehicleData.resetDriveNodeList(isNearByDrive, nodeManager.getNode(vehicleData.getCurrNode()));
	
						// Step2: IdleMode로 전환
						if (vehicleData.getVehicleMode() == 'A') {
							// OperationMode(x->I) by Initialize.
							changeOperationMode(OPERATION_MODE.IDLE, "Initialize");
						}
	
						// Step3: Vehicle의 NextCmd 명령이 취소되지 않은 경우, OHT Cancel 전송
						getVehicleCommCommand().setCommandId(0);
						sendCancelCommand(vehicleData.getNextCmd(), 'N');
					} else if ((vehicleData.getState() == 'A' && vehicleData.getNextCmd() == 0) ||
							(vehicleData.getState() == 'O')) {
						// Vehicle의 NextCmd 명령 취소된 경우 : TransferAbortFailed
						// remoteCmdAbortFailed();
					} else {
						trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
						addTrCmdToStateUpdateList();
					}
					break;
				}
				// 2012.07.23 by MYM : Manual Error에서 재시작 보완 처리
				// 배경 : 반송명령 수행중 Manual Error 발생 -> Auto Init 상태로 재시작 되었을 때 반송 미수행 현상 발생
				case CMD_PAUSED: {
					if (TrCmdConstant.VEHICLE_MANUAL_ERROR.equals(trCmd.getPauseType())
							&& vehicleData.getVehicleMode() == 'A') {
						clearAlarmReport(0);
						vehicleData.setVehicleError(false);
						addVehicleToUpdateList();
						traceOperation("Send ClearAlarmReport...");

						if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
							trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
							pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, trCmd.getPauseCount());
							addTrCmdToStateUpdateList();
						} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT) {
							trCmd.setLastAbortedTime(System.currentTimeMillis());
							trCmd.setState(TRCMD_STATE.CMD_ABORTED);
							addTrCmdToStateUpdateList();
						}
						resetFromVehicleErrorHistory();
					}
				}
				default:
					break;
			}

			switch (trCmd.getDetailState()) {
				case NOT_ASSIGNED: {
//					vehicleData.setTargetNode(trCmd.getSourceNode());
//					trCmd.setState(TRCMD_STATE.CMD_WAITING);
//					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
//					trCmd.setCarrierLoc(trCmd.getSourceLoc());
//					addTrCmdToStateUpdateList();
//					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_INITIATED, 0);
//					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);

					// AssignedTrCmd를 가지고 왔는데, NOT_ASSIGNED이면 Abnormal임.
					unassignTrCmd();
					traceOperationException("Abnormal Case: Operation#020");
					break;
				}
				case SCAN_SENT:
				case SCAN_ACCEPTED:
				case SCANNING: {
					// 2013.02.15 by KYK
					String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//					vehicleData.setTargetNode(trCmd.getSourceNode());
					vehicleData.setTarget(trCmd.getSourceNode(), targetStation);

					if (vehicleData.isCarrierExist() == false) {
						trCmd.setDetailState(TRCMD_DETAILSTATE.SCAN_ASSIGNED);
						trCmd.setCarrierLoc(trCmd.getSourceLoc());
						addTrCmdToStateUpdateList();
					}
					break;
				}
				case UNLOAD_ASSIGNED:
				case UNLOAD_SENT:
				case UNLOAD_ACCEPTED:
				case UNLOADING: {
					// 2013.02.15 by KYK
					String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//					vehicleData.setTargetNode(trCmd.getSourceNode());
					vehicleData.setTarget(trCmd.getSourceNode(), targetStation);

					if (vehicleData.isCarrierExist() == false) {
						// 2012.11.01 by MYM, PMM : Init시 Manual인 경우에는 DetailState 계속 유지 필요. 
						// 배경 : Unloading 중 ME -> MI -> Commfail -> Init시 UNLOADING을 UNLOAD_ASSIGNED로 변경 -> MI -> Commfail -> Job Cancel
						if (vehicleData.getVehicleMode() == 'A') {
							// 2012.07.23 by MYM : Manual Error로 CMD_PAUSED된 경우 아직 Manual 상태에서 재시작한 경우 이전 State로 유지되어야 함.
							// trCmd.setState(TRCMD_STATE.CMD_WAITING);

							// 2013.08.20 by PMM : CMD_TRANSFERRING/UNLOAD_ASSIGNED 인 케이스가 발생함.
							if (trCmd.getState() == TRCMD_STATE.CMD_TRANSFERRING) {
								trCmd.setState(TRCMD_STATE.CMD_WAITING);
							}
							
							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
							trCmd.setCarrierLoc(trCmd.getSourceLoc());
							addTrCmdToStateUpdateList();
						}
					} else {
						if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
							if (vehicleData.getState() == 'N') {
								if (checkCarrierMismatchedOnUnloadPort()) {
									return;
								}
								
								if (checkMissedCarrierOnUnloadPort()) {
									changeOperationMode(OPERATION_MODE.IDLE, "Missed Carrier");
									return;
								}
								
								trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
								trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
								trCmd.setUnloadedTime(getCurrDBTimeStr());
								addTrCmdToStateUpdateList();
								pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
								
								sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
								sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRECOMPLETED, 0);
								
								if (checkSTBUTBCarrierMismatchOnUnloadPort()) {
									return;
								}
							}
						}
					}
					break;
				}
				case UNLOADED:
				case LOAD_ASSIGNED: {
					// 2014.07.28 by MYM : CMD_ABORTED 조건 추가(ABORT 상태에서는 TargetNode 업데이트할 필요 없음.)
					// 2013.09.12 by MYM : UnkownTrCmd 등록 후 재시작 or 통신 재접속시 DestNode가 Null인 경우는
					//                     Vehicle의 TargetNode로 등록하지 않도록 조건 추가
//					if (trCmd.getDestNode() != null) {
					if (trCmd.getDestNode() != null && trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
						// 2013.02.15 by KYK
						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//					vehicleData.setTargetNode(trCmd.getDestNode());
						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
					}
					if (vehicleData.isCarrierExist() == false && isLoadingByPass() == false) {
						// 2011.10.26 by PMM
						// LoadingByPass 제외 조건이 누락되어 있었음.
						trCmd.setLastAbortedTime(System.currentTimeMillis());
						
						// 2012.01.28 by PMM
						trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
						trCmd.setState(TRCMD_STATE.CMD_ABORTED);
						trCmd.setDeletedTime(getCurrDBTimeStr());
						if (trCmd.getCarrierLoc().equals(vehicleData.getVehicleLoc())) {
							trCmd.setCarrierLoc(trCmd.getDestLoc());
						}
						addTrCmdToStateUpdateList();
						registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
	
						sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
						sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
						// 2012.11.30 by KYK : ResultCode 세분화
//						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_UNLOADED_BUT_CARRIERNOTEXIST);
	
						StringBuilder message = new StringBuilder();
						message.append("TrCmd is deleted because of Carrier Status Problem: <<CommandID:");
						message.append(trCmd.getTrCmdId()).append(", CarrierID:").append(trCmd.getCarrierId());
						traceOperation(message.toString());
						deleteTrCmdFromDB();
					}
					resetForRerouting();
					break;
				}
				case LOAD_SENT:
				case LOAD_ACCEPTED:
				case LOADING: {
					// 2013.02.15 by KYK
					String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//					vehicleData.setTargetNode(trCmd.getDestNode());
					vehicleData.setTarget(trCmd.getDestNode(), targetStation);

					if (vehicleData.isCarrierExist() == false) {
						trCmd.setCarrierLoc(trCmd.getDestLoc());
						trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
						trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
						trCmd.setLoadedTime(getCurrDBTimeStr());
						trCmd.setDeletedTime(getCurrDBTimeStr());
						addTrCmdToStateUpdateList();
						registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
	
						sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
						sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
						sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 0);
						
						if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
							if (isSTBCUsed) {
								updateCarrierStateInSTB(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
							} else {
								updateCarrierStateInSTBWithoutSTBC(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
							}
						}
						
						deleteTrCmdFromDB();
					} else {
						trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
						trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
						addTrCmdToStateUpdateList();
					}
					break;
				}
				case LOADED: {
					// 2013.02.15 by KYK
					String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//					vehicleData.setTargetNode(trCmd.getDestNode());
					vehicleData.setTarget(trCmd.getDestNode(), targetStation);

					// 2011.10.26 by PMM
//					trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
					trCmd.setCarrierLoc(trCmd.getDestLoc());
					trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
					trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
					addTrCmdToStateUpdateList();
					registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
	
					sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 0);
					
					if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
						if (isSTBCUsed) {
							updateCarrierStateInSTB(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
						} else {
							updateCarrierStateInSTBWithoutSTBC(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
						}
					}
					
					deleteTrCmdFromDB();
					break;
				}
				default:
					break;
			}
			addVehicleToUpdateList();
		}
		
		// 2015.01.28 by MYM : 장애 지역 우회 기능(Manual인 Vehicle이 Operation 재시작 중 Auto로 된 경우 처리)
		if (activeOperationMode.getOperationMode() == OPERATION_MODE.SLEEP
				&& vehicleData.getVehicleMode() == 'A') {
			vehicleData.releaseAbnormalSection();
		}
		// 2012.04.10 by PMM
		changeOperationMode(OPERATION_MODE.IDLE, "initialize");
		
		// 상태 변경  READY -> START
		operationControlState = OPERAION_CONTROL_STATE.START;
	}

	/**
	 * Update CarrierState in STB
	 * 
	 * @param state
	 * @param carrierLocId
	 * @param carrierId
	 * @param rfData
	 * @param foupId
	 * @return
	 */
	public boolean updateCarrierStateInSTB(String state, String carrierLocId, String carrierId, String rfData, String foupId) {
		assert (trCmd != null);

		if (isEmulatorMode) {
			rfData = carrierId;
		}

		if ("ERROR".equals(rfData)) {
			Date time = new Date();
			String date = sdf.format(time);
			
			StringBuffer unknownCarrier = new StringBuffer();
			unknownCarrier.append("UNKNOWN");
			unknownCarrier.append("-");
			unknownCarrier.append(carrierLocId);
			unknownCarrier.append("-");
			unknownCarrier.append(date);
			rfData = unknownCarrier.toString();
		}

		if (trCmd != null && trCmd.isLoadingByPass() == false) {
			STBCarrierLoc stbCarrierLoc = (STBCarrierLoc) stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
			if (stbCarrierLoc != null) {
				String stbData = stbCarrierLoc.getCarrierId();
				stbCarrierLoc.setCommandName(state);
				stbCarrierLoc.setMcsCarrierId(carrierId);
				stbCarrierLoc.setOcsCarrierId(rfData);
				stbCarrierLoc.setMcsFoupId(foupId);
				StringBuilder message = new StringBuilder();
				if (trCmd.isOcsRegistered()) {
					message.append("[OCSRegistered] NOT Reported. ");
				} else {
					stbCarrierLocManager.updateSTBCarrierStateForOperation(stbCarrierLoc);
				}
				
				message.append(state).append(" ");
				message.append(carrierLocId);
				message.append(": MCS CARRIERID=").append(carrierId);
				message.append(", RF=").append(rfData);
				message.append(", MCS FOUPID=").append(foupId);
				message.append(", STB=").append(stbData);
				traceSTB(message.toString());
				// 2013.09.10 by MYM : LongRun에서 생성한 반송명령은 Mismatch 미처리
				// Mismatch Case.
//				if (stbData.equals(carrierId) == false && "REMOVE".equals(state)) {
				if (trCmd.isOcsRegistered() == false && stbData.equals(carrierId) == false && "REMOVE".equals(state)) {
					return false;
				}
			} else {
				StringBuilder message = new StringBuilder();
				message.append("STBCarrierLoc is null. - ");
				message.append(state).append(" ");
				message.append(carrierLocId);
				message.append(": MCS CARRIERID=").append(carrierId);
				message.append(", RF=").append(rfData);
				message.append(": MCS FOUPID=").append(foupId);
				message.append(", STB=(null)");
				traceOperationException(message.toString());
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Update CarrierState in STB without STBC
	 * 
	 * @param state
	 * @param carrierLocId
	 * @param carrierId
	 * @param rfData
	 * @param foupId
	 */
	public void updateCarrierStateInSTBWithoutSTBC(String state, String carrierLocId, String carrierId, String rfData, String foupId) {
		STBCarrierLoc stbCarrierLoc = (STBCarrierLoc) stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
		if (stbCarrierLoc != null) {
			stbCarrierLoc.setCommandName(state);
			stbCarrierLoc.setMcsCarrierId(carrierId);
			stbCarrierLoc.setOcsCarrierId("");
			stbCarrierLoc.setMcsFoupId(foupId);
			StringBuilder message = new StringBuilder();
			if (trCmd.isOcsRegistered()) {
				message.append("[OCSRegistered] NOT Reported. ");
			} else {
				stbCarrierLocManager.updateSTBCarrierStateForOperation(stbCarrierLoc);
			}
			message.append(state).append(" ");
			message.append(carrierLocId);
			message.append(": MCS CARRIERID=").append(carrierId);
			message.append(", RF=").append(rfData);
			message.append(": MCS FOUPID=").append(foupId);
			traceSTB(message.toString());
		} else {
			StringBuilder message = new StringBuilder();
			message.append("STBCarrierLoc is null. - ");
			message.append(state).append(" ");
			message.append(carrierLocId);
			message.append(": MCS CARRIERID=").append(carrierId);
			message.append(", RF=").append(rfData);
			message.append(": MCS FOUPID=").append(foupId);
			traceOperationException(message.toString());
		}
	}

	/**
	 * is STB or UTB Port?
	 * 
	 * @param carrierLocType
	 * @return
	 */
	public boolean isSTBOrUTBPort(CARRIERLOC_TYPE carrierLocType) {
		if (carrierLocType == CARRIERLOC_TYPE.STBPORT ||
				carrierLocType == CARRIERLOC_TYPE.UTBPORT) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * is RF Read Port?
	 * 
	 * @param carrierLocType
	 * @return
	 */
	protected boolean isRfReadPort(CARRIERLOC_TYPE carrierLocType) {
		// OCSINFO에서 설정된 값으로 RF Read를 진행할 Port Type이 지정된다.
		// ex.)STBPORT/STKPORT/UTBPORT
//		if (ocsInfoManager.getRfReadDevice().indexOf(carrierLocType.toConstString()) >= 0) {
		if (rfReadDevice.indexOf(carrierLocType.toConstString()) >= 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 2012.03.08 by KYK  
	 * is STBPort Available? (Enabled='true' or 'false')
	 * @param carrierLocId
	 * @return
	 */
	public boolean isSTBPortAvailable(String carrierLocId) {
		// 둘중에 어느게 맞을까?
		STBCarrierLoc stbCarrierLoc = (STBCarrierLoc) stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
		//STBCarrierLoc stbCarrierLoc = (STBCarrierLoc) stbCarrierLocManager.getData().get(carrierLocId);
		if (stbCarrierLoc != null) {
			if (stbCarrierLoc.isEnabled()) {
				return true;
			}
		}
		return false;
	}

//	private boolean isCarrierMismatchedOnSTKPort(CARRIERLOC_TYPE carrierLocType, String carrierId, String rfData) {
//		if (isRfReadPort(carrierLocType)) {
//			if (carrierLocType == CARRIERLOC_TYPE.STOCKERPORT) {
//				if ((rfData != null && rfData.length() == 0) || "(null)".equals(rfData)) {
//					return false;
//				} else if (carrierId.equals(rfData) == false) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}
	
	/**
	 * is Carrier Mismatched on Unload Port?
	 */
	private boolean isCarrierMismatchedOnUnloadPort(CARRIERLOC_TYPE carrierLocType, String carrierId, String rfData) {
		if (isRfReadPort(carrierLocType)) {
//			String mismatchUnloadAppliedPort = ocsInfoManager.getOCSInfoValue(OcsInfoConstant.MISMATCH_UNLOAD_APPLIED_PORT);
			if (mismatchUnloadAppliedPort.indexOf(carrierLocType.toConstString()) >= 0) {
				if ((rfData != null && rfData.length() == 0) || "(null)".equals(rfData)) {
					return false;
				} else if (carrierId.equals(rfData) == false) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Process Command Reply Message Received from Vehicle
	 * 
	 * @param commData
	 */
	private void processReceivedCommandReplyMessageFromVehicle(
			VehicleCommData commData) {
		// 2011.10.26 by PMM
//		boolean isVehicleCommandChanged = false;
		// Locus 전송에 대한 응답 미처리 - Emulator에서만 동작
		if (commData.getCommand() == 'a')
			return;

		if (vehicleData.getCommand() != commData.getCommand() ||
				vehicleData.getCommandId() != commData.getCommandId() ||
				vehicleData.getReply() != commData.getReply()) {
			vehicleData.setCommand(commData.getCommand());
			vehicleData.setCommandId(commData.getCommandId());
			vehicleData.setReply(commData.getReply());
			
			// 2011.10.26 by PMM
//			isVehicleCommandChanged = true;
		}

		StringBuffer log = new StringBuffer();
		log.append("Cmd:").append(commData.getCommand());
		log.append(" CmdID:").append(commData.getCommandId());
		log.append(" Reply:").append(commData.getReply());

		switch (commData.getReply()) {
			case 'A':
			case 'B': {
				// 2015.02.26 by MYM : Pause(Hold), Resume, PatrolCancel에 대한 응답 수신시 RESPONDED 처리
				// 배경 : M1B 라인 무언정지 발생
				//       Unload 위치로 마지막 주행(Go) 명령 전송 후 이동 중 선행 AutoPosition으로
				//       Pause/Resume 명령 전송. but, 응답 처리(cmdState=Responded)하지 않아 
				//       Unload 위치 도착 후 Unload 명령은 전송하나 이전 cmdId로 전송하여 Datalogic 발생
				//       COMMAND_STATE.SENT 이면 trCmd를 TRCMD_DETAILSTATE.UNLOAD_SENT로 변경
        //       COMMAND_STATE.SENT -> COMMAND_STATE.WAITFORRESPONSE
        //       COMMAND_STATE.WAITFORRESPONSE -> COMMAND_STATE.TIMEOUT
//				if (vehicleCommCommand.getCommandId() == commData.getCommandId()) {
				if (vehicleCommCommand.getCommandId() == commData.getCommandId()
						|| commData.getCommand() == 'h' || commData.getCommand() == 'e'
						|| commData.getCommand() == 'z') {
					cmdState = COMMAND_STATE.RESPONDED;
					log.append(" CmdStatus:N");
				}
	
				if (trCmd != null) {
					if (vehicleData.isCarrierExist() == false) {
						if (commData.getCommand() == 'u') {
	//						if (commData.getCommand() == 'u' || commData.getCommand() == 'n') {
							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ACCEPTED);
							addTrCmdToStateUpdateList();
						} else if (commData.getCommand() == 'x' ||
								trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED) {
							trCmd.setState(TRCMD_STATE.CMD_WAITING);
							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
							addTrCmdToStateUpdateList();
						} else if (commData.getCommand() == 'r') {
							trCmd.setDetailState(TRCMD_DETAILSTATE.SCAN_ACCEPTED);
							addTrCmdToStateUpdateList();
						} else if (commData.getCommand() == 'k') {
							trCmd.setState(TRCMD_STATE.CMD_MAPMAKING);
							trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMAKING);
							addTrCmdToStateUpdateList();
						} else if (commData.getCommand() == 'c') {
							trCmd.setState(TRCMD_STATE.CMD_PATROLLING);
							trCmd.setDetailState(TRCMD_DETAILSTATE.PATROLLING);
							addTrCmdToStateUpdateList();
						}
					} else {
						if (commData.getCommand() == 'l') {
	//						if (commData.getCommand() == 'l' || commData.getCommand() == 'o') {
							trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ACCEPTED);
							addTrCmdToStateUpdateList();
						} else if (commData.getCommand() == 'x' || trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED) {
							trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
							addTrCmdToStateUpdateList();
						}
					}
				}
	
				// MCS Cancel/Abort 후 OHT Cancel 명령인지를 확인하여 CancelComplted or AbortCompleted 보고
				reportRemoteCmdCompleted();
				break;
			}
			case 'E': {
				// Error가 발생한 경우에 상태정보를 기다린다는 Flag셋팅
				if (cmdState == COMMAND_STATE.SENT) {
					// A,B로 응답받은 경우에만 'N'으로 나머지는 'F'로 저장
					cmdState = COMMAND_STATE.UNKNOWN;
				}
				// 2014.08.13 by MYM : Abnormal CmdReply 확인
				if (isResendCmdForAbnormalReply) {
					log.append("[ReceivedCommandReplyMsg] Reply Error from the Vehicle.");
				} else {
					log.append("[ReceivedCommandReplyMsg] Reply Pause from the Vehicle.");
				}
	
				// MCS Cancel/Abort 후 OHT Cancel 명령인지를 확인하여 CancelFailed or AbortFailed 보고
				reportRemoteCmdFailed();
				break;
			}
			case 'P': {
				// Protocol Error가 발생한 경우에 상태정보를 기다린다는 Flag셋팅
				if (cmdState == COMMAND_STATE.SENT) {
					// A,B로 응답받은 경우에만 'N'으로 나머지는 'F'로 저장
					cmdState = COMMAND_STATE.UNKNOWN;
				}
				log.append("[ReceivedCommandReplyMsg] Reply Protocol Error from the Vehicle.");
	
				// MCS Cancel/Abort 후 OHT Cancel 명령인지를 확인하여 CancelFailed or AbortFailed 보고
				reportRemoteCmdFailed();
				break;
			}
			case 'D': {
				// DataLogic Error가 발생한 경우에 상태정보를 기다린다는 Flag셋팅
				if (cmdState == COMMAND_STATE.SENT) {
					// A,B로 응답받은 경우에만 'N'으로 나머지는 'F'로 저장
					cmdState = COMMAND_STATE.UNKNOWN;
				}
				log.append("[ReceivedCommandReplyMsg] Reply DataLogic Error from the Vehicle.");
	
				// MCS Cancel/Abort 후 OHT Cancel 명령인지를 확인하여 CancelFailed or AbortFailed 보고
				reportRemoteCmdFailed();
				break;
			}
			case 'T': {
				// Timeout Error가 발생한 경우에 상태정보를 기다린다는 Flag셋팅
				if (cmdState == COMMAND_STATE.SENT) {
					cmdState = COMMAND_STATE.UNKNOWN;
				}
				log.append(" CmdStatus:Timeout");
	
				// MCS Cancel/Abort 후 OHT Cancel 명령인지를 확인하여 CancelFailed or AbortFailed 보고
				reportRemoteCmdFailed();
				
				if (isEmulatorMode) {
					if (vehicleData.getCommand() == 'G') {
						if (vehicleData.getState() == 'A') {
							if (vehicleData.getDriveNode(vehicleData.getDriveNodeCount()).equals(vehicleData.getRoutedNode(0)) == false) {
								traceOperationException("Abnormal Case: Operation#100 (EmulatorMode Only)");
								operationControlState = OPERAION_CONTROL_STATE.INIT;
								initializeControlState(commData);
								commData.setReceivedState(false);
							}
						}
					}
				}
				
				break;
			}
			default:
				// Operation#003
				traceOperationException("Abnormal Case: Operation#003");
				break;
		}
		
		// 2011.10.26 by PMM
		// 모두 찍도록 변경
//		if (isVehicleCommandChanged) {
//			traceOperation(log.toString());
//		}
		traceOperation(log.toString());
	}

	/**
	 * Check Abnormal Case
	 */
	private void checkAbnormalCase() {
		// 2011.10.12 by PMM start가 되지 않으면 CommFail 여부를 알 수 없어 check 위치를 이동
//		// Step 1: CommFail 여부 확인
//		checkCommFail();
		
		if (vehicleData.getVehicleMode() == 'A' && isCommFail() == false) {
			if (isSystemPaused) {
				vehicleData.setStateChangedTime(System.currentTimeMillis());
				lastDifferentCommandSentTime = System.currentTimeMillis();
			} else {
				switch (vehicleData.getState()) {
					case 'G':
						// Step 2: Going 무언 정지 확인 및 조치
						checkNotRespondingWhileGoing();
						break;
					case 'U':
						// Step 3: Unloading 무언 정지 확인 및 조치
						checkNotRespondingWhileUnloading();
						break;
					case 'L':
						// Step 4: Loading 무언 정지 확인 및 조치
						checkNotRespondingWhileLoading();
						break;
					default:
						checkNotRespondingDefaults();
						break;
				}
				
				// 2014.06.03 by MYM : [Stage Locate 기능] 무언정지시 Stage Reset
				if (vehicleData.isStageRequested()) {
					switch (vehicleData.getAlarmCode()) {
						case OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS:
						case OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER:
						case OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER:
						case OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER: {
							resetStageRequest(EVENTHISTORY_REASON.VEHICLE_NOT_RESPONDING.toConstString());
							break;
						}
					}
				}
			}
		}
		// 2015.06.11 by KYK : operationManager 로 위치 이동
//		// Step 5: DESTCHANGE 처리 지연 확인 및 조치
//		checkDelayedDestChange();

		// 2014.08.13 by MYM : Abnormal CmdReply 확인
		checkAbnormalCmdReply();
	}
	
	/**
	 * 2014.08.13 by MYM : Abnormal CmdReply 확인
	 * 
	 */
	private void checkAbnormalCmdReply() {
		if (isResendCmdForAbnormalReply) {
			return;
		}
		
		if (cmdState == COMMAND_STATE.UNKNOWN) {
			int alarmCode = OcsAlarmConstant.NO_ALARM;
			switch (vehicleData.getReply()) {
				case 'E':
					if (Math.abs(System.currentTimeMillis() - lastDifferentCommandSentTime) > 30000) {
						alarmCode = OcsAlarmConstant.RECEIVED_CMDREPLY_PAUSE;
					}
					break;
				case 'P':
					alarmCode = OcsAlarmConstant.RECEIVED_CMDREPLY_PROTOCOL;
					break;
				case 'D':
					alarmCode = OcsAlarmConstant.RECEIVED_CMDREPLY_DATALOGIC;
					break;				
			}
			if (alarmCode != vehicleData.getAlarmCode()
					&& alarmCode != OcsAlarmConstant.NO_ALARM) {
				if (vehicleData.getAlarmCode() != OcsAlarmConstant.NO_ALARM) {
					unregisterAlarm(vehicleData.getAlarmCode());
				}
				setAlarmCode(alarmCode);
				vehicleData.requestRepathSearch(isNearByDrive);
			}
		} else {
			switch (vehicleData.getAlarmCode()) {
			case OcsAlarmConstant.RECEIVED_CMDREPLY_PAUSE:
				unregisterAlarm(OcsAlarmConstant.RECEIVED_CMDREPLY_PAUSE);
				break;
			case OcsAlarmConstant.RECEIVED_CMDREPLY_PROTOCOL:
				unregisterAlarm(OcsAlarmConstant.RECEIVED_CMDREPLY_PROTOCOL);
				break;
			case OcsAlarmConstant.RECEIVED_CMDREPLY_DATALOGIC:
				unregisterAlarm(OcsAlarmConstant.RECEIVED_CMDREPLY_DATALOGIC);
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * Unregister Alarm in Abnormal Case
	 */
	private void checkNotRespondingDefaults() {
		if (vehicleData.getCurrNode().equals(vehicleData.getStopNode()) == false) {
			// 2015.08.08 by MYM : StateChangedTime도 함게 보도록 조건 추가
			// 배경 : M1B 근접제어에서 Vehicle이 주행 중이면서 추가 Go 명령을 줬을 때 계속 Datalogic을 올리는 경우는
			//       Vehicle이 Arrived 되자마자 가성 무언정지가 발생 및 바로 해제됨 (Arrived 되자마자 Go 명령 수신 및 응답)
			//       → 장애 회피 기능 동작으로 해당 영역 Section Disable 되었다가 타이밍으로 해제 안됨 
			// ※ StateChangedTime? CurrNode, Mode, PauseType, State의 변경이 있거나 TSC_PUASED 상태인 경우 변경됨
			// 2014.08.13 by MYM : Abnormal CmdReply 확인
//			if (Math.abs(System.currentTimeMillis() - lastDifferentCommandSentTime) > goModeCheckTime * 1000) {
			if (Math.abs(System.currentTimeMillis() - vehicleData.getStateChangedTime()) > goModeCheckTime * 1000 &&
					Math.abs(System.currentTimeMillis() - lastDifferentCommandSentTime) > goModeCheckTime * 1000 &&
					(isResendCmdForAbnormalReply || cmdState != COMMAND_STATE.UNKNOWN)) {
				// 2014.07.18 by KYK : Go command 를 재전송하지 못하는 경우 (OCS무언정지)
				int alarmCode = vehicleData.getAlarmCode();
				if (vehicleData.getCommandId() != vehicleData.getCurrCmd() &&
						(Math.abs(System.currentTimeMillis() - lastCommandSentTime) > goModeCheckTime * 1000)) {
					alarmCode = OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS;
				} else {
					alarmCode = OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER;
				}
				
				if (alarmCode != vehicleData.getAlarmCode()) {
					if (vehicleData.getAlarmCode() != 0) {
						unregisterAlarm(vehicleData.getAlarmCode());
					}
					setAlarmCode(alarmCode);
					// 2014.09.26 by KYK : commfail, 무언정지 시 합류주행예약 취소하도록 함
					vehicleData.cancelReservationForVehicleDriveIn();
				}				
//				if (vehicleData.getAlarmCode() != OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER) {
//					setAlarmCode(OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER);
//				}
				cancelCommandOnNotRespondingVehicle();
				vehicleData.requestRepathSearch(isNearByDrive);
				return;
			}
		} else {
			if (trCmd != null) {
				switch (trCmd.getDetailState()) {
					case UNLOAD_SENT:
					case UNLOAD_ACCEPTED:
					{
						if (vehicleData.getState() != 'U') {
							if (Math.abs(System.currentTimeMillis() - lastDifferentCommandSentTime) > workModeCheckTime * 1000) {
								if (vehicleData.getAlarmCode() != OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER) {
									setAlarmCode(OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER);
								}
								vehicleData.requestRepathSearch(isNearByDrive);
								return;
							}
						}
						break;
					}
					case LOAD_SENT:
					case LOAD_ACCEPTED:
					{
						if (vehicleData.getState() != 'L') {
							if (Math.abs(System.currentTimeMillis() - lastDifferentCommandSentTime) > workModeCheckTime * 1000) {
								if (vehicleData.getAlarmCode() != OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
									setAlarmCode(OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER);
									cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_NOT_RESPONDING);
								}
								vehicleData.requestRepathSearch(isNearByDrive);
								return;
							}
						}
						break;
					}
					default:
						break;
				}
			}
		}
		
		switch (vehicleData.getAlarmCode()) {
			// 2014.07.15 by KYK : OCS 무언정지 구분
			case OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS:
				unregisterAlarm(OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS);
				break;
			case OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER:
				unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER);
				break;
			// 2012.12.06 by MYM : 대차 센서 감지 무언정지 알람 제거 추가
			case OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER:
				unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER);
				break;
			case OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER:
				unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER);
				break;
			case OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER:
				unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER);
				break;
			default:
				break;
		}
	}

	/**
	 * Check CommFail
	 */
	private void checkCommFail() {
		// INSERVICE에서만 확인.
		if (isAllOperationReady) {
			if (isCommFail()) {
				if (trCmd != null && trCmd.getChangedRemoteCmd() != TRCMD_REMOTECMD.NULL) {
					// ChangedRemoteCmd가 Null이 아니면, ChangedRemoteCmd를 먼저 처리하고 cancel하도록 수정.
					// Transfer, Stage : 반송명령 할당해제 후 TrCmd Reset
					traceOperation("checkCommFail() - ChangedRemoteCmd is NOT NULL.");
					return;
				}
				
				// 2015.01.28 by MYM : 초기 구동시 OHT와 통신이 안될 때 알람 표시 안되는 현상 개선
//				if (operationControlState != OPERAION_CONTROL_STATE.INIT) {
//					operationControlState = OPERAION_CONTROL_STATE.INIT;
//					if (vehicleData.getErrorCode() != OcsConstant.COMMUNICATION_FAIL) {
//						vehicleData.setErrorCode(OcsConstant.COMMUNICATION_FAIL);
//						addVehicleToUpdateList();
//						// 2014.09.26 by KYK : commfail, 무언정지 시 합류주행예약 취소하도록 함
//						vehicleData.cancelReservationForVehicleDriveIn();
//					}
				if (operationControlState != OPERAION_CONTROL_STATE.INIT
						|| vehicleData.getErrorCode() != OcsConstant.COMMUNICATION_FAIL) {
					// 2015.05.01 by KYK 
					if (isCommfailAlarmReportUsed) {
						isCommfailAlarmReported = true;
						setAlarmReport(OcsConstant.COMMUNICATION_FAIL);
						traceOperation("Send SetAlarmReport...< AlarmID:" + vehicleData.getErrorCode() + " >");
						// 2022.03.30 by JJW Commfail Vehicle Error History 기록
						registerVehicleErrorHistory(OcsConstant.COMMUNICATION_FAIL, "Communication Fail", OperationConstant.VEHICLEERROR_ERROR);
					}

					operationControlState = OPERAION_CONTROL_STATE.INIT;
					vehicleData.setErrorCode(OcsConstant.COMMUNICATION_FAIL);
					addVehicleToUpdateList();
					// 2014.09.26 by KYK : commfail, 무언정지 시 합류주행예약 취소하도록 함
					vehicleData.cancelReservationForVehicleDriveIn();
					
					// 2011.10.27 by PMM
					// Vehicle CommFail 시 작업 할당 해제.
					if (trCmd != null) {
						switch (trCmd.getDetailState()) {
							case UNLOAD_ASSIGNED:
							case STAGE_ASSIGNED:
							case SCAN_ASSIGNED:
							{
								cancelAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_COMMFAIL, true);
								break;
							}
							case MAPMAKE_ASSIGNED:
								// MapMake 중 Go 명령 지연은 어떻게 처리?
								// 작업삭제 하는 것이 좋을 듯 함.
								cancelMapMakeCommand(EVENTHISTORY_REASON.VEHICLE_COMMFAIL);
								break;
							case PATROL_ASSIGNED:
								// 2015.12.21 by KBS : CommFail의 경우는 TrCmd 정리를 하지 않고 재진행
								// 배경 : CommFail로 통신이 끊기면 canclePatrol을 해도 OHT가 수신하지 못함
								break;
							case LOADING:
								// 2012.08.28 by PMM
								// U1에서 Loading 중 Next작업 할당 받았으나 CommFail로 처리 못하는 중, VHL LineOut됨.
								cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_COMMFAIL);
								break;
							default:
								break;
						}
					}
					vehicleData.requestRepathSearch(isNearByDrive);
				}
				
				// 2014.06.03 by MYM : [Stage Locate 기능] Commfail시 Stage 해제
				if (vehicleData.isStageRequested()
						|| vehicleData.getRequestedType() == REQUESTEDTYPE.STAGECANCEL) {
					resetStageRequest(EVENTHISTORY_REASON.VEHICLE_COMMFAIL.toConstString());
				}
				
				// 2015.02.11 by MYM : 장애 지역 우회 기능
				vehicleData.setAbnormalSection(DETOUR_REASON.VEHICLE_COMMFAIL);
			}
		}
	}

	/**
	 * Check NotResponding While Going
	 */
	private void checkNotRespondingWhileGoing() {
		assert (isCommFail() == false);
		assert (vehicleData.getVehicleMode() == 'A');
		assert (vehicleData.getState() == 'G');
		
		long notRespondingTime = Math.abs(System.currentTimeMillis() - vehicleData.getStateChangedTime());

		// 2012.11.28 by MYM : 대차 센서 감지에 의한 Go Mode 무언정지 CheckTime을 파라미터화
		// 2011.10.20 by PMM
		// 장애물 감지 여부에 따른 GoMode 무언정지 알람 구분.
		if (notRespondingTime > goModeCheckTime * 1000 && vehicleData.getPauseType() == 0) {	
			// 전방에 장애물이 없는 경우
			if (vehicleData.getAlarmCode() != OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER) {
				setAlarmCode(OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER);
				// 2014.09.26 by KYK : commfail, 무언정지 시 합류주행예약 취소하도록 함
				vehicleData.cancelReservationForVehicleDriveIn();
			}
			vehicleData.requestRepathSearch(isNearByDrive);
		} else if (vehicleData.getPauseType() == 1 && notRespondingTime > goModeVehicleDetectedCheckTime * 1000) {
			// 대차 센서 감지에 의한 Go Mode 무언정지 -> 장애 OHT 아님.
			if (vehicleData.getAlarmCode() != OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER) {
				setAlarmCode(OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER);
			}
//			vehicleData.requestRepathSearch(isNearByDrive);
		} else {
			switch (vehicleData.getAlarmCode()) {
				case OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER:
					unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER);
					break;
				case OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER:
					unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER);
					break;
				default:
					break;
			}
		}

		cancelCommandOnNotRespondingVehicle();
	}
	
	private void cancelCommandOnNotRespondingVehicle() {
		// 2012.12.04 by MYM : 무언정지뿐만 아니라 대차 센서 감지에 의한 무언정지도 Unload 전 반송명령 Cancel 처리
		if (trCmd != null
				&& (vehicleData.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER ||
						vehicleData.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER ||
						vehicleData.getAlarmCode() == OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS)) {
			switch (trCmd.getDetailState()) {
				case UNLOAD_ASSIGNED:
				case STAGE_ASSIGNED:
				case SCAN_ASSIGNED: {
					if (trCmd.getChangedRemoteCmd() == TRCMD_REMOTECMD.NULL) {
						// ChangedRemoteCmd가 Null이 아니면, ChangedRemoteCmd를 먼저 처리하고 cancel하도록 수정.
						// Transfer, Stage : 반송명령 할당해제 후 TrCmd Reset
						if (vehicleData.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER) {
							long notRespondingTime = Math.abs(System.currentTimeMillis() - vehicleData.getStateChangedTime());
							if (notRespondingTime > goModeVehicleDetectedResetTimeout * 1000) {
								cancelAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_NOT_RESPONDING, true);
							}
						} else {
							cancelAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_NOT_RESPONDING, true);
						}
					}
					break;
				}
				case MAPMAKE_ASSIGNED:
					// MapMake 중 Go 명령 지연은 어떻게 처리?
					// 작업삭제 하는 것이 좋을 듯 함.
					cancelMapMakeCommand(EVENTHISTORY_REASON.VEHICLE_NOT_RESPONDING);
					break;
				case PATROL_ASSIGNED:
					// Patrol 중 Go 명령 지연은 어떻게 처리?
					// 그대로 놔둬도 될 듯.
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Check NotResponding While Unloading
	 */
	private void checkNotRespondingWhileUnloading() {
		assert (isCommFail() == false);
		assert (vehicleData.getVehicleMode() == 'A');
		assert (vehicleData.getState() == 'U');

		if (Math.abs(System.currentTimeMillis() - vehicleData.getStateChangedTime()) > workModeCheckTime * 1000) {
			if (vehicleData.getAlarmCode() != OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER) {
				setAlarmCode(OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER);
			}
			vehicleData.requestRepathSearch(isNearByDrive);
		} else if (vehicleData.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER) {
			unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER);
		}
	}

	/**
	 * Check NotResponding While Loading
	 */
	private void checkNotRespondingWhileLoading() {
		assert (isCommFail() == false);
		assert (vehicleData.getVehicleMode() == 'A');
		assert (vehicleData.getState() == 'L');

		if (Math.abs(System.currentTimeMillis() - vehicleData.getStateChangedTime()) > workModeCheckTime * 1000) {
			if (vehicleData.getAlarmCode() != OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
				setAlarmCode(OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER);
				cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_NOT_RESPONDING);
			}
			vehicleData.requestRepathSearch(isNearByDrive);
		} else if (vehicleData.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
			unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER);
		}
	}

	/**
	 * Check Delayed DestChange
	 */
	private void checkDelayedDestChange() {
		// 2012.01.02 by PMM
//		if (trCmd != null && trCmd.getState() == TRCMD_STATE.CMD_ABORTED) {
//			if (System.currentTimeMillis() - trCmd.getLastAbortedTime() > ocsInfoManager.getAbortCheckTime() * 1000) {
//				if (vehicleData.getAlarmCode() != OcsAlarmConstant.DELAYED_DESTCHANGE) {
//					setAlarmCode(OcsAlarmConstant.DELAYED_DESTCHANGE);
//				}
//			} else if (isAlarmRegistered()) {
//				if (vehicleData.getAlarmCode() == OcsAlarmConstant.DELAYED_DESTCHANGE) {
//					unregisterAlarm(OcsAlarmConstant.DELAYED_DESTCHANGE);
//				}
//			}
//		}
		if (trCmd != null) {
			if (trCmd.getState() == TRCMD_STATE.CMD_ABORTED) {
				if (trCmd.getLastAbortedTime() == 0) {
					trCmd.setLastAbortedTime(System.currentTimeMillis());
				}
//				if (System.currentTimeMillis() - trCmd.getLastAbortedTime() > ocsInfoManager.getAbortCheckTime() * 1000) {
				if (System.currentTimeMillis() - trCmd.getLastAbortedTime() > abortCheckTime * 1000) {
					if (vehicleData.getAlarmCode() != OcsAlarmConstant.DELAYED_DESTCHANGE) {
						setAlarmCode(OcsAlarmConstant.DELAYED_DESTCHANGE);
					}
				}
				return;
			} 
		}
		if (vehicleData.getAlarmCode() == OcsAlarmConstant.DELAYED_DESTCHANGE) {
			unregisterAlarm(OcsAlarmConstant.DELAYED_DESTCHANGE);
		}
	}

	/**
	 * Process RemoteCmd
	 */
	private void processRemoteCmd() {
//		[TrCmd]
//		 . Change Job State
//		   - CHANGEDREMOTECMD : CANCEL(MCS), ABORT(MCS), STAGEDELETE(MCS), DESTCHANGE(IBSEM), STAGECHANGE(IBSEM)
//		   - CHANGEDTRCMDID : TRCMDID
//		 . Assign Job (TRANSFER, STAGE, SCAN)
//		   - ASSIGENDVEHICLE : VEHICLEID
//		   
//		[Vehicle]
//		 . Move Vehicle Location 
//		   - REQUESTEDTYPE : MOVE, PARK, YIELD, ZONEMOVE
//		   - REQUESTEDDATA : NODEID
//
//
//		1. S2F49(TRANSFER, STAGE, SCAN)
//		  1) MCS -> IBSEM 반송명령 수신
//		  2) JobAssign에서 해당 작업을 Vehicle에게 할당
//		     . TrCmd의 AssignVehicle에 VEHICLEID를 적어준다.
//		  3) Operation에서 작업없는 IDLE 호기이면 TrCmdManager에서 해당 Vehicle에게 할당된 반송명령을 찾아서 가져온다.
//
//		2. USER TRCMD(PATROL, MAPMAKE)
//		  1) 근무자 RemoteServer를 통해 반송명령 생성
//		     . TrCmd의 AssignVehicle에 VEHICLEID를 적어준다.
//		  2) Operation에서 작업없는 IDLE 호기이면 TrCmdManager에서 해당 Vehicle에게 할당된 반송명령을 찾아서 가져온다.
//
//		3. CANCEL 명령 수신
//		  1) IBSEM에서 해당 반송명령의 CHANGEDREMOTECMD에 CANCEL로 적어준다.
//		  2) Operation에서 TrCmd의 CHANGEDTRCMDID이 CANCEL이면 Cancel 처리를 한다.
//		  
//		4. ABORT 명령 수신
//		  1) IBSEM에서 해당 반송명령의 CHANGEDREMOTECMD에 ABORT로 적어준다.
//		  2) Operation에서 TrCmd의 CHANGEDTRCMDID이 ABORT이면 Abort 처리를 한다.
//		  
//		5. STAGEDELETE 명령 수신
//		  1) IBSEM에서 해당 반송명령의 CHANGEDREMOTECMD에 STAGEDELETE로 적어준다.
//		  2) Operation에서 TrCmd의 CHANGEDTRCMDID이 STAGEDELETE이면 STAGEDELETE 처리를 한다.
//
//		6. DESTCHANGE
//		  1) IBSEM에서 해당 반송명령의 CHANGEDREMOTECMD에 DESTCHANGE로 적어준다.
//		     . ChangedTrCmdID에는 DestChange받은 TrCmdID를 적어준다.
//		  2) Operation에서 TrCmd의 CHANGEDTRCMDID이 DESTCHANGE이면 DESTCHANGE 처리를 한다.
//		  
//		7. STAGECHANGE
//		  1) IBSEM에서 해당 반송명령의 CHANGEDREMOTECMD에 DESTCHANGE로 적어준다.
//		     . ChangedTrCmdID에는 DestChange받은 TrCmdID를 적어준다.
//		  2) Operation에서 TrCmd의 CHANGEDTRCMDID이 DESTCHANGE이면 DESTCHANGE 처리를 한다.

		
		// 1. DB에서 반송명령 삭제 여부 확인
		checkDeletedTrCmd();
		
		if (trCmd != null) {
			// 2. 반송명령이 있는 경우 CANCEL(MCS), ABORT(MCS), STAGEDELETE(MCS), DESTCHANGE(IBSEM), STAGECHANGE(IBSEM) 처리
			if (trCmd.getChangedRemoteCmd() != TRCMD_REMOTECMD.NULL) {
				switch (trCmd.getChangedRemoteCmd()) {
					case CANCEL:
						processCancel();
						break;
					case ABORT:
						processAbort();
						break;
					case DESTCHANGE:
						processDestChange();
						break;
					case STAGECHANGE:
						processStageChange();
						break;
					case STAGEDELETE:
						processStageDelete();
						break;
					case REMOVE:
						processRemove();
						break;
					case PAUSE:
						processPause();
						break;
					case RESUME:
						processResume();
						break;
					case VIBRATIONCHANGE:
						processVibrationChange();
						break;
					case TRANSFERUPDATE:
						processTransferUpdate();
						break;
					default:
						// TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
						updateChangedInfoReset("Abnormal");
						break;
				}
			} else {
				// 2018.08.31 by LSH : 반송중인데 신규 작업이 할당된 경우, 신규 작업 할당 해제하는 기능 추가
				TrCmd nextTrCmd = trCmdManager.getAssignRequestedTrCmd(vehicleData.getVehicleId());
				String nextTrCmdId = (nextTrCmd == null ? "" : nextTrCmd.getTrCmdId());
				if (nextTrCmdId.equals(trCmd.getTrCmdId()) == false	&&
						(activeOperationMode.getOperationMode() == OPERATION_MODE.IDLE || activeOperationMode.getOperationMode() == OPERATION_MODE.GO)) {
					cancelNextAssignedTrCmd(EVENTHISTORY_REASON.BEFORE_JOB_IN_PROGRESS);
				}
			}
		} else {
			// 2015.04.01 by KYK
			if (vehicleData.isAbnormalVehicle()) {
				trCmd = trCmdManager.getAssignRequestedTrCmd(vehicleData.getVehicleId());
				if (trCmd != null && (trCmd.getState() == TRCMD_STATE.CMD_QUEUED || trCmd.getState() == TRCMD_STATE.CMD_WAITING)) {
					cancelAssignedTrCmd(EVENTHISTORY_REASON.ABNORMAL_VEHICLE, false);
					traceOperationException("Vehicle is in AbnormalState, but TrCmd is assigned -> Cancel Assigned TrCmd.");
				}
			}
			// 3. 반송명령이 없는 경우 TRANSFER, STAGE, SCAN, MAPMAKE, PATROL, VIBRATION 처리
			else if ((activeOperationMode.getOperationMode() == OPERATION_MODE.IDLE || activeOperationMode.getOperationMode() == OPERATION_MODE.GO) &&
					cmdState != COMMAND_STATE.UNKNOWN &&
					cmdState != COMMAND_STATE.TIMEOUT &&
					cmdState != COMMAND_STATE.WAITFORRESPONSE) {
				long startedTime = System.currentTimeMillis();
				trCmd = trCmdManager.getAssignRequestedTrCmd(vehicleData.getVehicleId());
				elapsedTime = System.currentTimeMillis() - startedTime;
				if (elapsedTime >= 10) {
					StringBuffer message = new StringBuffer();
					message.append("[trCmdManager.getAssignRequestedTrCmd] ElapsedTime:").append(elapsedTime).append("ms");
					traceOperationDelay(message.toString());
				}
				if (trCmd != null) {
					resetTargetNode("processRemoteCmd()");
					vehicleData.resetVehicleLocusList();
					// 2021.03.29 by JJW Stage 일 경우 DB trcmd 재갱신
					long stageStartedTime = System.currentTimeMillis();
					if(trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE){
						TrCmd stageTrCmd = trCmdManager.getTrCmdFromDBWhereCarrierId(trCmd.getCarrierId()); 
						if (stageTrCmd != null){
							if(stageTrCmd.getTrCmdId().equals(trCmd.getTrCmdId()) && stageTrCmd.getRemoteCmd() != trCmd.getRemoteCmd()){
								trCmd = stageTrCmd;
							}
						}
					}
					elapsedTime = System.currentTimeMillis() - stageStartedTime;
					if (elapsedTime >= 10) {
						StringBuffer message2 = new StringBuffer();
						message2.append("[trCmdManager.getTrCmdFromDBWhereCarrierId] ElapsedTime:").append(elapsedTime).append("ms");
						traceOperationDelay(message2.toString());
					}
					switch (trCmd.getDetailState()) {
						case NOT_ASSIGNED:
						{
							switch (trCmd.getRemoteCmd()) {
								case TRANSFER:
									processTransfer();
									break;
								case STAGE:
									processStage();
									break;
								case SCAN:
									processScan();
									break;
								case PREMOVE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
									processPremove();
									break;
								case MAPMAKE:
									processMapMake();
									break;
								case PATROL:
									processPatrol();
									break;
								case VIBRATION:
									processVibration();
									break;
								default:
									traceOperationException("Abnormal Case: Operation#013");
									resetTrCmd();
									break;
							}
							break;
						}
						case UNLOAD_ASSIGNED:
						{
							traceOperationException("Abnormal Case: Operation#014-1");
							// AssignRequested TrCmd는 CMD_QUEUED/NOT_ASSIGNED여야 함.
							traceOperationException("Reset the trCmd's status as CMD_QUEUED/NOT_ASSIGNED.");
							unassignTrCmd();
							resetTrCmd();
							break;
						}
						case UNLOAD_SENT:
						case UNLOAD_ACCEPTED:
						case UNLOADING:
						{
							traceOperationException("Abnormal Case: Operation#014-2");
							break;
						}
						case UNLOADED:
						case LOAD_ASSIGNED:
						case LOAD_WAITING:	// 2022.03.14 dahye : Premove Logic Improve
						case LOAD_SENT:
						case LOAD_ACCEPTED:
						case LOADING:
						{
							traceOperationException("Abnormal Case: Operation#014-3");
							break;
						}
						case LOADED:
						{
							traceOperationException("Abnormal Case: Operation#014-4");
							deleteTrCmdFromDB();
							break;
						}
						case STAGE_ASSIGNED:
						case STAGE_NOBLOCKING:
						case STAGE_WAITING:
						case SCAN_ASSIGNED:
						case MAPMAKE_ASSIGNED:
						case PATROL_ASSIGNED:
						{
							traceOperationException("Abnormal Case: Operation#014-5");
							break;
						}
						default:
						{
							traceOperationException("Abnormal Case: Operation#014-6");
							resetTrCmd();
							break;
						}
					}
				}
			}
		}
		
		if (vehicleData.getRequestedType() == REQUESTEDTYPE.LOCATECANCEL) {
			processLocateCancelRequest();
		} 
		// 2014.02.21 by MYM : [Stage Locate 기능]
		else if (vehicleData.getRequestedType() == REQUESTEDTYPE.STAGECANCEL
				|| (vehicleData.getRequestedType() == REQUESTEDTYPE.NULL && vehicleData.isStageRequested())) {
			processStageCancelRequest();
		}
		
		// 2013.09.06 by MYM : [OHT Location Update시 처리 보완] SleepMode에서는 VehicleRequested(Move, Yield, Locate 등) 처리를 하지 않도록 함.
		// 배경 : AV → AI 되었을 때 Move, Yield, Locate 요청을 받고 Search Success가 되면 바로 GoMode로 전환 되어
		//       AI를 처리 못하는 현상 발생 (2013.09.05 M1A 발생)
		// 4. MOVE, YIELD, ZONEMOVE, RESET, PARK(?) 처리
		if (activeOperationMode.getOperationMode() != OPERATION_MODE.SLEEP) {
			if (trCmd == null) {
				// 반송명령이 없는 경우
				switch (vehicleData.getRequestedType()) {
				case MOVE:
					processMoveRequest(false);
					break;
				case PMOVE:
					processMoveRequest(true);
					break;
				case YIELD:
					processYieldRequest();
					break;
				case ZONEMOVE:
					processZoneMoveRequest();
					break;
				case LOCATE:
					processLocateRequest(false);
					break;
				case PLOCATE:
					processLocateRequest(true);
					break;
//				case LOCATECANCEL:
//					processLocateCancelRequest();
//					break;
				// 2014.02.21 by : [Stage Locate 기능]
				case STAGE:
					processStageRequest();
					break;
				case STAGENOBLOCK:
					processStageNoBlockRequest();
					break;
				case STAGEWAIT:
					processStageWaitRequest();
					break;
				case RESET:
					processResetRequest();
					break;
				case VEHICLEAUTO:
					processVehicleAutoRequest();
					break;
				default:
					break;
				}
			} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
				if (trCmd.getState() == TRCMD_STATE.CMD_MONITORING) {
					switch(vehicleData.getRequestedType()) {
					case MOVE:
						processMoveRequest(false);
						break;
					case LOCATE:
						processLocateRequest(false);
						break;
					case PMOVE:
						processMoveRequest(true);
						break;
					case PLOCATE:
						processLocateRequest(true);
						break;
					case YIELD:
						processYieldRequest();
						break;
					case VEHICLEAUTO:
						processVehicleAutoRequest();
						break;
					default:
						break;
					}
				}
				// 2013.09.06 by MYM : 조건을 위로 옮김.
				// 2012.11.28 by MYM : SleepMode에서는 VehicleRequested(Move, Yield) 처리를 하지 않도록 함.
				// 현상 : 첫번째 AV 발생하고 한바퀴 돌고 와서 두번째 AV 발생한 이후 DetailStatus가 LOADING으로 계속 유지되어 반송 처리 못하는 현상 발생(S1A, Retry 1회 설정)
				// 원인 : 첫번째 AV 발생하였을 때 아래의 I번째 단계의 AI 처리 단계 전 Move 요청으로 Move Search 후 SleepMode -> GoMode로 변경되어
				//       두번째 AV 발생 후 G번째 단계 수행시 avExist가 true로 유지되어 AV 처리를 못함. -> 계속 LOADING 상태로 유지됨. 
				//      [AutoRetry 처리 순서]
				//        A. WorkMode → B. Send Load → C. Loading → D. AV 발생 → E. SleepMode 전환 → F. Send ID Reset → 
				//        G. avExist false인 경우에 AV 처리(avExist true 설정) → H. AI 상태 수신 → I. AI 처리(avExist false 설정)
//			} else if (trCmd.isPause() && activeOperationMode.getOperationMode() != OPERATION_MODE.SLEEP){
			} else if (trCmd.isPause()) {
				// 반송명령이 존재하고 Pause된 경우
				switch(vehicleData.getRequestedType()) {
				case MOVE:
					processMoveRequest(false);
					break;
				case LOCATE:
					processLocateRequest(false);
					break;
				case PMOVE:
					processMoveRequest(true);
					break;
				case PLOCATE:
					processLocateRequest(true);
					break;
				case YIELD:
					processYieldRequest();
					break;
				case VEHICLEAUTO:
					processVehicleAutoRequest();
					break;
				default:
					break;
				}
			}
		}
		vehicleData.setAssignedVehicle(trCmd != null);
	}

	/**
	 * Check Deleted TrCmd
	 */
	private void checkDeletedTrCmd() {
		if (trCmd != null) {
			if (trCmdManager.getTrCmd(trCmd.getTrCmdId()) == null && trCmd.isOcsRegistered() == false) {
				// RemoteClient에서 TrCmd를 직접 삭제한 경우. DB에는 없는 경우.
				// 2013.02.15 by KYK
//				vehicleData.setTargetNode(vehicleData.getStopNode());
				vehicleData.setTarget(vehicleData.getStopNode(), vehicleData.getStopStation());
				
				// 2011.10.12 by PMM
				vehicleData.resetRoutedNodeList();
				
				addVehicleToUpdateList();
				trCmd.setDeletedTime(getCurrDBTimeStr());
				registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
				StringBuilder message = new StringBuilder();
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
					message.append("StageCmd is deleted by RemoteClient or IBSEM Request: <<CommandID:").append(trCmd.getTrCmdId());
					message.append(", CarrierID:").append(trCmd.getCarrierId());
					message.append(">>");
				} else {
					message.append("TrCmd is deleted by RemoteClient: <<CommandID:").append(trCmd.getTrCmdId());
					message.append(", CarrierID:").append(trCmd.getCarrierId());
					message.append(", RemoteCmd:").append(trCmd.getRemoteCmd().toConstString());
					message.append(">>");
				}
				traceOperation(message.toString());
				addTrCmdToStateUpdateList();
				resetTrCmd();
			} else if (trCmdManager.getTrCmd(trCmd.getTrCmdId()) != null && trCmd.isOcsRegistered()) {
//				trCmd.setOcsRegistered(false);
			}
		}
	}
	
	/**
	 * Process TRANSFER Command
	 */
	private void processTransfer() {
		if (trCmd == null) {
			traceOperationException("AssignRequested TRANSFER TrCmd exists, BUT trCmd is null.");
			return;
		} else if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.TRANSFER) {
			return;
		} else if (trCmd.getChangedRemoteCmd() == TRCMD_REMOTECMD.CANCEL) {
			// 2012.08.28 by PMM
			// AssignedVehicle에 할당 요청을 받았으나, Vehicle이 LineOut되어 CANCEL 처리 안된 케이스 발생.
			// AssignedVehicle에 값이 있더라도 NOT_ASSIGNED이면 OperationManager에서 Cancel 처리함.
			StringBuilder message = new StringBuilder();
			message.append("CANCEL Requested to AssignRequested TrCmd: <<CommandID:").append(trCmd.getTrCmdId());
			message.append(", CarrierID:").append(trCmd.getCarrierId());
			message.append(">>");
			traceOperation(message.toString());
			
			trCmd.setVehicle("");
			trCmd.setAssignedVehicleId("");
			addTrCmdToVehicleUpdateList();
			
			resetTrCmd();
			return;
		}
		
		// 2011.10.20 by PMM
		// 작업 할당 후 장애 VHL인 경우, 바로 작업 할당 해제.
		if (vehicleData.isAbnormalVehicle()) {
			cancelAssignedTrCmd(EVENTHISTORY_REASON.ABNORMAL_VEHICLE, false);
			
			// 2012.03.06 by PMM
			// cancelAssignedTrCmd() 내부에서 resetTrCmd()를 하기 때문에 trCmd == null임.
			return;
		}
		
		trCmd.setState(TRCMD_STATE.CMD_WAITING);
		trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
		trCmd.setCarrierLoc(trCmd.getSourceLoc());
		// 2013.02.15 by KYK
		String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//		vehicleData.setTargetNode(trCmd.getSourceNode());
		vehicleData.setTarget(trCmd.getSourceNode(), targetStation);

		if (trCmd.isPause()) {
			pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
		}
		addTrCmdToStateUpdateList();
		trCmd.setVehicle(vehicleData.getVehicleId());
		addTrCmdToVehicleUpdateList();
		vehicleData.setAvRetryWait(false);
		
		// 2012.01.31 by PMM
		if (vehicleData.getRequestedType() == REQUESTEDTYPE.MOVE) {
			updateRequestedCommandReset(REQUESTEDTYPE.MOVE_RESET, "JobAssign");
		}
		
		addVehicleToUpdateList();
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_INITIATED, 0);
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);

		StringBuilder message = new StringBuilder();
		message.append("[TRANSFER] JobAssign. TrCmdID:").append(trCmd.getTrCmdId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", SourceNode:").append(trCmd.getSourceNode());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		message.append(", DestNode:").append(trCmd.getDestNode());
		traceOperation(message.toString());
		
		changeOperationMode(OPERATION_MODE.IDLE, trCmd.getRemoteCmd().toConstString());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " " + trCmd.getRemoteCmd().toConstString());
		
		setVehicleUserDefinedRoute();
	}
	
	/**
	 * Process SCAN Command
	 */
	private void processScan() {
		if (trCmd == null) {
			traceOperationException("AssignRequested SCAN TrCmd exists, BUT trCmd is null.");
			return;
		} else if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.SCAN) {
			return;
		}
		
		// 2013.02.15 by KYK
		String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//		vehicleData.setTargetNode(trCmd.getSourceNode());
		vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
		trCmd.setCarrierLoc(trCmd.getSourceLoc());
		trCmd.setState(TRCMD_STATE.CMD_WAITING);
		trCmd.setDetailState(TRCMD_DETAILSTATE.SCAN_ASSIGNED);
		if (trCmd.isPause()) {
			pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
		}
		addTrCmdToStateUpdateList();
		trCmd.setVehicle(vehicleData.getVehicleId());
		addTrCmdToVehicleUpdateList();
		vehicleData.setAvRetryWait(false);
		addVehicleToUpdateList();
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCAN_INITIATED, 0);
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);

		StringBuilder message = new StringBuilder();
		message.append("[SCAN] JobAssign. TrCmdID:").append(trCmd.getTrCmdId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", SourceNode:").append(trCmd.getSourceNode());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		message.append(", DestNode:").append(trCmd.getDestNode());
		traceOperation(message.toString());
		changeOperationMode(OPERATION_MODE.IDLE, trCmd.getRemoteCmd().toConstString());

		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " " + trCmd.getRemoteCmd().toConstString());
	}
	
	/**
	 * Process STAGE Command
	 */
	private void processStage() {
		if (trCmd == null) {
			traceOperationException("AssignRequested STAGE TrCmd exists, BUT trCmd is null.");
			return;
		} else if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.STAGE) {
			return;
		}
		
		// 2011.10.20 by PMM
		if (trCmd.getExpectedDuration() == 0) {
			cancelStageCommand(EVENTHISTORY_REASON.EXPECTEDDURATION_IS_ZERO);
			
			// 2012.01.19 by PMM
			// GoMode인 경우, 정상적으로 TargetNode에 도착할 때까지 Mode 유지.
			if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
				changeOperationMode(OPERATION_MODE.IDLE, "STAGECANCEL (ExpectedDuration is zero.)");
			}
			return;
		}
		// 2013.02.15 by KYK
		String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//		vehicleData.setTargetNode(trCmd.getSourceNode());
		vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
		trCmd.setCarrierLoc(trCmd.getSourceLoc());
		trCmd.setState(TRCMD_STATE.CMD_WAITING);
		trCmd.setDetailState(TRCMD_DETAILSTATE.STAGE_ASSIGNED);
		if (trCmd.isPause()) {
			pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
		}
		addTrCmdToStateUpdateList();
		trCmd.setVehicle(vehicleData.getVehicleId());
		addTrCmdToVehicleUpdateList();
		vehicleData.setAvRetryWait(false);
		addVehicleToUpdateList();
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);

		StringBuilder message = new StringBuilder();
		message.append("[STAGE] JobAssign. TrCmdID:").append(trCmd.getTrCmdId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", SourceNode:").append(trCmd.getSourceNode());
		traceOperation(message.toString());
		
		changeOperationMode(OPERATION_MODE.IDLE, trCmd.getRemoteCmd().toConstString());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " " + trCmd.getRemoteCmd().toConstString());
	}
	
	/**
	 * Process PREMOVE Command
	 * 2021.04.02 by JDH : Transfer Premove 사양 추가
	 */
	private void processPremove() {
		if (trCmd == null) {
			traceOperationException("AssignRequested PREMOVE TrCmd exists, BUT trCmd is null.");
			return;
		} else if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.PREMOVE) {
			return;
//		}
		// 2022.03.14 dahye : Premove Logic Improve
		// PREMOVE 반송에 대한 CANCEL 처리
		} else if (trCmd.getChangedRemoteCmd() == TRCMD_REMOTECMD.CANCEL) {
			// 2012.08.28 by PMM
			// AssignedVehicle에 할당 요청을 받았으나, Vehicle이 LineOut되어 CANCEL 처리 안된 케이스 발생.
			// AssignedVehicle에 값이 있더라도 NOT_ASSIGNED이면 OperationManager에서 Cancel 처리함.
			StringBuilder message = new StringBuilder();
			message.append("CANCEL Requested to AssignRequested TrCmd: <<CommandID:").append(trCmd.getTrCmdId());
			message.append(", CarrierID:").append(trCmd.getCarrierId());
			message.append(">>");
			traceOperation(message.toString());
			
			trCmd.setVehicle("");
			trCmd.setAssignedVehicleId("");
			addTrCmdToVehicleUpdateList();
			
			resetTrCmd();
			return;
		}

		if (vehicleData.isAbnormalVehicle()) {
			cancelAssignedTrCmd(EVENTHISTORY_REASON.ABNORMAL_VEHICLE, false);
			return;
		}
		
		trCmd.setState(TRCMD_STATE.CMD_WAITING);
		trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
		trCmd.setCarrierLoc(trCmd.getSourceLoc());
		
		String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
		vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
		
		if (trCmd.isPause()) {
			pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
		}
		addTrCmdToStateUpdateList();
		trCmd.setVehicle(vehicleData.getVehicleId());
		addTrCmdToVehicleUpdateList();
		vehicleData.setAvRetryWait(false);
		
		if (vehicleData.getRequestedType() == REQUESTEDTYPE.MOVE) {
			updateRequestedCommandReset(REQUESTEDTYPE.MOVE_RESET, "JobAssign");
		}
		
		addVehicleToUpdateList();
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_INITIATED, 0);
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);
		
		StringBuilder message = new StringBuilder();
		message.append("[PREMOVE] JobAssign. TrCmdID:").append(trCmd.getTrCmdId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", SourceNode:").append(trCmd.getSourceNode());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		message.append(", DestNode:").append(trCmd.getDestNode());
		traceOperation(message.toString());
		
		changeOperationMode(OPERATION_MODE.IDLE, trCmd.getRemoteCmd().toConstString());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " " + trCmd.getRemoteCmd().toConstString());
		
		setVehicleUserDefinedRoute();
	}

	/**
	 * Process MAPMAKE Command
	 */
	private void processMapMake() {
		if (trCmd == null) {
			traceOperationException("AssignRequested MAPMAKE TrCmd exists, BUT trCmd is null.");
			return;
		} else if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.MAPMAKE) {
			return;
		}
		
		if (vehicleData.isAssignHold() == false) {
			traceOperation("[MAPMAKE] Canceled - Vehicle is not in AssignHold.");
			cancelMapMakeCommand(EVENTHISTORY_REASON.VEHICLE_NOT_ASSIGNHOLD);
			return;
		}
		
		// 2013.02.15 by KYK
		String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//		vehicleData.setTargetNode(trCmd.getSourceNode());
		vehicleData.setTarget(trCmd.getSourceNode(), targetStation);

		trCmd.setCarrierLoc(trCmd.getSourceLoc());
		trCmd.setState(TRCMD_STATE.CMD_WAITING);
		trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMAKE_ASSIGNED);
		trCmd.setLoadingByPass(true);
		trCmd.setOcsRegistered(true);
		if (trCmd.isPause()) {
			pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
		}
		addTrCmdToStateUpdateList();
		trCmd.setVehicle(vehicleData.getVehicleId());
		addTrCmdToVehicleUpdateList();
		vehicleData.setAvRetryWait(false);
		addVehicleToUpdateList();

		StringBuilder message = new StringBuilder();
		message.append("[MAPMAKE] JobAssign. TrCmdID:").append(trCmd.getTrCmdId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", SourceNode:").append(trCmd.getSourceNode());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		message.append(", DestNode:").append(trCmd.getDestNode());
		traceOperation(message.toString());
		changeOperationMode(OPERATION_MODE.IDLE, trCmd.getRemoteCmd().toConstString());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " " + trCmd.getRemoteCmd().toConstString());
	}
	
	/**
	 * Process PATROL Command
	 */
	private void processPatrol() {
		if (trCmd == null) {
			traceOperationException("AssignRequested PATROL TrCmd exists, BUT trCmd is null.");
			return;
		} else if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.PATROL) {
			return;
		}

		// 2013.02.15 by KYK
		String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//		vehicleData.setTargetNode(trCmd.getSourceNode());
		vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
		trCmd.setCarrierLoc(trCmd.getSourceLoc());
		trCmd.setState(TRCMD_STATE.CMD_WAITING);
		trCmd.setDetailState(TRCMD_DETAILSTATE.PATROL_ASSIGNED);
		trCmd.setLoadingByPass(true);
		trCmd.setOcsRegistered(true);
		if (trCmd.isPause()) {
			pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
		}
		addTrCmdToStateUpdateList();
		trCmd.setVehicle(vehicleData.getVehicleId());
		addTrCmdToVehicleUpdateList();
		vehicleData.setAvRetryWait(false);
		addVehicleToUpdateList();

		StringBuilder message = new StringBuilder();
		message.append("[PATROL] JobAssign. TrCmdID:").append(trCmd.getTrCmdId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", SourceNode:").append(trCmd.getSourceNode());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		message.append(", DestNode:").append(trCmd.getDestNode());
		message.append(", PatrolMode:").append(trCmd.getPatrolMode());
		traceOperation(message.toString());
		changeOperationMode(OPERATION_MODE.IDLE, trCmd.getRemoteCmd().toConstString());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " " + trCmd.getRemoteCmd().toConstString());
	}
	
	/**
	 * Process VIBRATION Command
	 */
	private void processVibration() {
		if (trCmd == null) {
			traceOperationException("AssignRequested VIBRATION TrCmd exists, BUT trCmd is null.");
			return;
		} else if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.VIBRATION) {
			return;
		}
		
		if (vehicleData.isAbnormalVehicle()) {
			cancelVibrationCommand(EVENTHISTORY_REASON.ABNORMAL_VEHICLE);
			return;
		}
		
		if (vehicleData.getLocalGroupId() != null && vehicleData.getLocalGroupId().length() > 0) {
			clearLocalGroupId();
			traceOperation("LocalGroupInfo is cleared by VIBRATION.");
		}
		
		vehicleData.setTargetNode(trCmd.getSourceNode());
		trCmd.setCarrierLoc(trCmd.getSourceLoc());
		trCmd.setState(TRCMD_STATE.CMD_WAITING);
		trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
		trCmd.setOcsRegistered(true);
		if (trCmd.isPause()) {
			pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
		}
		addTrCmdToStateUpdateList();
		trCmd.setVehicle(vehicleData.getVehicleId());
		addTrCmdToVehicleUpdateList();
		vehicleData.setAvRetryWait(false);
		addVehicleToUpdateList();

		StringBuilder message = new StringBuilder();
		message.append("[VIBRATION] JobAssign. TrCmdID:").append(trCmd.getTrCmdId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", SourceNode:").append(trCmd.getSourceNode());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		message.append(", DestNode:").append(trCmd.getDestNode());
		traceOperation(message.toString());
		changeOperationMode(OPERATION_MODE.IDLE, trCmd.getRemoteCmd().toConstString());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " " + trCmd.getRemoteCmd().toConstString());
	}
	
	/**
	 * Process VIBRATIONCHANGE Command
	 */
	private void processVibrationChange() {
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
			if (activeOperationMode.getOperationMode() != OPERATION_MODE.SLEEP) {
				switch (trCmd.getDetailState()) {
					case UNLOAD_ASSIGNED:
					case UNLOAD_SENT:
					case UNLOAD_ACCEPTED:
					case UNLOADING:
					{
						break;
					}
					case VIBRATION_MONITORING:
					{
						trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
						
						if (trCmd.isPause()) {
							pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
						}
						
						// 메모리 값을 DB에 업데이트
						addTrCmdToStateUpdateList();
						
						traceOperation("VibrationChange by Request.");
						updateChangedInfoReset("VibrationChange");
						break;
					}
					case LOAD_ASSIGNED:
					default:
					{
						traceOperation("VibrationChange by Request.");
						updateChangedInfoReset("VibrationChange");
						break;
					}
				}
			}
		}
	}

	/**
	 * Process CANCEL Command
	 */
	private void processCancel() {
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {		
			if (trCmd.getState() != TRCMD_STATE.CMD_CANCELFAILED &&
					trCmd.getState() != TRCMD_STATE.CMD_CANCELED &&
					trCmd.getState() != TRCMD_STATE.CMD_CANCELLING) {
				if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED) {
					trCmd.setRemoteCmd(TRCMD_REMOTECMD.CANCEL);
					trCmd.setState(TRCMD_STATE.CMD_CANCELED);
					addTrCmdToStateUpdateList();

					registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());

					// 2007.01.29 작업을 취소하는 경우에 StopNode까지만 이동
					resetTargetNode("processCancel()");

					// Report (TransferCancelInitiated, TransferCancelCompleted, VehicleUnassigned) Msg to MCS
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELINITIATED, 0);
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELCOMPLETED, 0);
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);

					traceOperation("Job Cancel: " + trCmd.getTrCmdId());
					traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Cancel");

					// 해당 TrCmdInfo 삭제
					deleteTrCmdFromDB();
				} else {
					if (vehicleData.getNextCmd() != 0 &&
							(trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_SENT || trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED)) {
						// Step 1: TrCmdStatus를 CMD_CANCELLING으로 변경(CMD_CANCELLING은 여기서만 사용,
						// CmdReply시 체크하여 CMD_CANCELED로 변경)
						trCmd.setRemoteCmd(TRCMD_REMOTECMD.CANCEL);
						trCmd.setState(TRCMD_STATE.CMD_CANCELLING);
						addTrCmdToStateUpdateList();
						// Step 2: Report (TransferCancelInitiated) Msg to MCS
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELINITIATED, 0);

						// Step 3: 로그 기록
						traceOperation("Job Cancelling: " + trCmd.getTrCmdId());
						traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Cancel");

						// Step 4: Vehicle로 NextCmd Cancel 전송
						vehicleData.setCommandId(0);
						sendCancelCommand(vehicleData.getNextCmd(), 'N');
					} else {
						// 2008.10.28 by MYM : CMD_CANCELFAILED -> CMD_TRANSFERRING 으로 변경
						// 배경 : 기존에는 CMD_CANCELFAILED로 변경하여 MCS에서 다시 CANCEL 명령을 줬을 때 IBSEM에서 NAK를 하였음.
						trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
						trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
						addTrCmdToStateUpdateList();

						// 2008.10.28 by MYM : TransferCancelInitiated 메시지 추가
						// 배경 : IBSEM에서 MCS Cancel 명령을 수용하였으므로 TransferCancelInitiated가 보고되어야 함.
						// Report (TransferCancelInitiated, TransferCancelFailed) Msg to MCS
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELINITIATED, 0);
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELFAILED, 0);

						traceOperation("Job CancelFailed: " + trCmd.getTrCmdId());
					}
				}

				// TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
				updateChangedInfoReset("Cancel");
			}
		}
	}

	/**
	 * Process ABORT Command
	 */
	private void processAbort() {
		if (trCmd != null) {
			// 2021.04.02 by JDH : Transfer Premove 사양 추가
			//if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER || trCmd.getRemoteCmd()== TRCMD_REMOTECMD.PREMOVE) {
				if (trCmd.getState() != TRCMD_STATE.CMD_ABORTFAILED &&
						trCmd.getState() != TRCMD_STATE.CMD_ABORTED &&
						trCmd.getState() != TRCMD_STATE.CMD_ABORTING) {
					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_SENT ||
							trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED ||
							trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING ||
							trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADED ) {
						if (vehicleData.getNextCmd() != 0 &&
								(trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_SENT || trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED)) {
							// Step1 : TrCmdStatus를 CMD_ABORTING로 변경(CMD_ABORTING은 여기서만 사용,
							// CmdReply시 체크하여 CMD_ABORTED로 변경)
							trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
							trCmd.setState(TRCMD_STATE.CMD_ABORTING);
							addTrCmdToStateUpdateList();
							// Step2 : Report (TransferAbortInitiated) Msg to MCS
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTINITIATED, 0);
							
							// Step3 : 로그 기록
							traceOperation("Job Aborting: " + trCmd.getTrCmdId());
							traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Abort");
							
							// Step4 : Vehicle로 NextCmd Cancel 전송
							vehicleData.setCommandId(0);
							sendCancelCommand(vehicleData.getNextCmd(), 'N');
						} else {
							// 2008.10.28 by MYM : CMD_ABORTFAILED -> CMD_TRANSFERRING 으로 변경
							// 배경 : 기존에는 CMD_CANCELFAILED로 변경하여 MCS에서 다시 CANCEL 명령을 줬을 때 IBSEM에서 NAK를 하였음.
							trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
							trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
							addTrCmdToStateUpdateList();
							
							// 2008.10.28 by MYM : TransferAbortInitiated 메시지 추가
							// 배경 : IBSEM에서 MCS Abort 명령을 수용하였으므로 TransferAbortInitiated가 보고되어야 함.
							// Report (TransferAbortInitiated, TransferAbortFailed) Msg to MCS
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTINITIATED, 0);
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTFAILED, 0);
							
							traceOperation("Job AbortFailed: " + trCmd.getTrCmdId());
						}
						updateChangedInfoReset("Abort"); // TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
					} else {
						// 2009.12.10 by MYM, IKY : ABORT는 CMD_PAUSED 유무에 따라 처리를 달리하도록 함.
						if (trCmd.getState() == TRCMD_STATE.CMD_PAUSED) {
							// 2009.12.10 by MYM, IKY :
							// 재시작시 반송명령 無, Carrier 有인 경우 OCS에서 등록한 UNKNOWN 반송명령에 대해서는 Abort보고를 하지 않도록 함.
							// CMD_PAUSED 상태에서 MCS로부터 Abort 명령을 수신한 경우는 Abort Fail 처리함.
							if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER && trCmd.isOcsRegistered() == false) {
								// Report (TransferAbortInitiated, TransferAbortFailed) Msg to MCS
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTINITIATED, 0);
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTFAILED, 0);
							}
							updateChangedInfoReset("Abort"); // TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
						} else {
							// 2009.12.10 by MYM, IKY :
							// 재시작시 반송명령 無, Carrier 有인 경우 OCS에서 등록한 UNKNOWN 반송명령에 대해서는 Abort보고를 하지  않도록 함.
							if (trCmd.isOcsRegistered() == false) {
								trCmd.setLastAbortedTime(System.currentTimeMillis());
								trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
								trCmd.setState(TRCMD_STATE.CMD_ABORTED);
								addTrCmdToStateUpdateList();
								
								// 2007.01.29 작업을 취소하는 경우에 StopNode까지만 이동
								resetTargetNode("processAbort()");
								
								// 2013.09.10 by MYM : Abort 수신시 TrCmd Pause 정보 변경
								// 배경 : PathSearch Fail 발생 후 Abort 된 경우 TargetNode가 Reset되지 않고 DestNode로 계속 PathSearch 시도함.
								//       MCS에서 Abort한 TrCmd는 Pause가 되지 않아 근무자가 Move 요청하여도 처리하지 못함. 
								pauseTrCmd(true, TrCmdConstant.ABORTED_BY_MCS, 0);
								
								// Report (TransferAbortInitiated, TransferAbortCompleted, VehicleUnassigned) Msg to MCS
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTINITIATED, 0);
								updateChangedInfoReset("Abort"); // TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTCOMPLETED, 0);
								sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
								
								traceOperation("Job Abort: " + trCmd.getTrCmdId());
								traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Abort");
							}
						}
					}
					// 2016.12.01 by KBS : updateChangedInfoReset 위치 변경
					// 배경 : AbortCompleted 보고 후 ChangedRemoteCmd reset이 지연되어 DestChange 처리가 꼬임
					// updateChangedInfoReset("Abort"); // TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
				}
			}
		} else {
			traceOperationException("processAbort() - trCmd is null.");
		}
	}
	
	/**
	 * Process DESTCHANGE Command
	 */
	private void processDestChange() {
		// 2012.01.30 by PMM
		// AbortedTrCmdId와 DestChangeTrCmdId가 동일한 경우,
		// IBSEM에서 TRANSFER/CMD_ABORTED/UNLOADED/ 로 DB 업데이트를 하는데, 재시작 시 DestChange 처리가 안 됨.
//		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT) {
//			if (trCmd.getState() == TRCMD_STATE.CMD_ABORTED) {
		if (trCmd.getState() == TRCMD_STATE.CMD_ABORTED) {
			String destChangedTrCmdId = trCmd.getChangedTrCmdId();
			TrCmd requestedTrCmd = trCmdManager.getTrCmd(destChangedTrCmdId);
			
			if (requestedTrCmd == null) {
				return;
			}
			if (requestedTrCmd.getDestLoc() == null) {
				// DestLoc이 ""이면 어떻게 처리를 하는고?
				return;
			}
			
			String oldTrCmdId = trCmd.getTrCmdId();
			String oldDestLoc = trCmd.getDestLoc();
			
			// TrCmdId가 다르면 기존 TrCmd는 History에 기록 및 삭제 처리 
			// 2011.11.22 by KYK  
			String abortedTrCmdId = trCmd.getTrCmdId();
			if (abortedTrCmdId.equals(destChangedTrCmdId) == false) {
//				if (trCmd.getTrCmdId().equals(destChangedTrCmdId) == false) {
				registerTrCompletionHistory(TRCMD_REMOTECMD.ABORT.toConstString());
				deleteTrCmdFromDB();
			}
			
			trCmd = requestedTrCmd;
			vehicleData.setAvRetryWait(false);
			trCmd.setVehicle(vehicleData.getVehicleId());
			// 2022.03.14 dahye : Premove Logic Improve
			//	State:CMD_PREMOVE
			//	DetailState:LOAD_WAITING
//			// 2021.09.03 dahye : onVehicle Premove 반송 처리 로직 변경
//			// 기존 : RCMD & DeliveryType 분리하여 인식
//			// 변경 : DeliveryType으로 PREMOVE 인지
////			trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
////			trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
//			if (trCmd.getDeliveryType().equals("PREMOVE")) {
//				trCmd.setRemoteCmd(TRCMD_REMOTECMD.PREMOVE);
//				trCmd.setState(TRCMD_STATE.CMD_PREMOVE);
//			} else {
//				trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
//				trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
//			}
			if (trCmd.getDeliveryType().equals("PREMOVE")) {
				trCmd.setRemoteCmd(TRCMD_REMOTECMD.PREMOVE);
			} else {
				trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
			}
			trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
			trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
			
			// 2012.12.13 by KYK : 위치변경 하였으나 주석처리함 IBSEM 에서 DestChange 수신시 Trasferring 보고하는 부분과 함께 정리필요
			// 2012.11.30 by KYK : 대체반송에 대한 TransferInit, VehicleAssign, VehicleArrived,Transferring 보고
			sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_INITIATED, 0);
			sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);			
			sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFERRING, 0);

			// DestNode가 없는 경우
			// OCS에서 생성한 Unknown TrCmd는 SourceLoc, DestLoc이 없으므로 -> MCS가 해당 Unknown TrCmd에 대해 DestChange를 줬을 때
			// IBSEM에서 해당 TrCmd를 복사하여 DestNode가 없음. 
			if (trCmd.getDestNode() == null || (trCmd.getDestNode() != null && trCmd.getDestNode().length() > 0)) {
				// 2011.11.08 by PMM
//					CarrierLoc carrierloc = carrierLocManager.getCarrierLocData(trCmd.getDestLoc());
//					trCmd.setDestNode(carrierloc.getNode());
				CarrierLoc destLoc = carrierLocManager.getCarrierLocData(trCmd.getDestLoc());
				if (destLoc != null) {
					trCmd.setDestNode(destLoc.getNode());
				} else {
					StringBuilder message = new StringBuilder();
					message.append("(Abnormal) DestChanged DestLoc is null. DestChangeTrCmd:").append(trCmd.getTrCmdId());
					message.append(" DestLoc:").append(trCmd.getDestLoc());
					message.append(" DestNode:").append(trCmd.getDestNode());
					traceOperation(message.toString());
				}
			}
			
			// CarrierLoc의 위치에 따른 처리... SourceLoc인 경우는 ??
			if (trCmd.getCarrierLoc().equals(trCmd.getSourceLoc())) {
			} else {
				// 2013.02.15 by KYK
				String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//				vehicleData.setTargetNode(trCmd.getDestNode());
				vehicleData.setTarget(trCmd.getDestNode(), targetStation);
				trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPARTED, 0);
			}
			
			// TrCmd가 Pause인 경우 Resume
			if (trCmd.isPause()) {
				pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
			}
			
			// 메모리 값을 DB에 업데이트
			addTrCmdToStateUpdateList();
			addTrCmdToVehicleUpdateList();
			
			// LocalOHT가 DestChange를 받은 경우 LocalGroup 해제
			if (isLocalOHTUsed) {
				clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL);
			}
			
			StringBuffer message = new StringBuffer("Job DestChange ");
			message.append(oldTrCmdId).append(" > ");
			message.append(trCmd.getTrCmdId()).append("  From ").append(oldDestLoc);
			message.append(" To ").append(trCmd.getDestLoc());
			traceOperation(message.toString());
			traceUpdateRequestedCmd(trCmd.getTrCmdId() + " DestChange");
			
			// TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
			
			// 2011.11.22 by KYK : ABORT 와 DESTCHANGE 의 TrCmdId 가 같을 경우에만 Reset
			if (abortedTrCmdId.equals(destChangedTrCmdId)){
				updateChangedInfoReset("DestChange");
			}
//			updateChangedInfoReset("DestChange");
			
			// 2014.02.14 by KYK : search in idle mode
			changeOperationMode(OPERATION_MODE.IDLE, trCmd.getRemoteCmd().toConstString());
		}
	}	
	
	/**
	 * 2021.01.27 by JJW : process Transfer Update 메소드 
	 * Process TRANSFERUPDATE Command
	 */
	private void processTransferUpdate() {
		if (trCmd != null) {
			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
				if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_SENT ||
						trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED ||
						trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING ||
						trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADED ) {
					if (vehicleData.getNextCmd() != 0 &&
							(trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_SENT || trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED)) {
						if (activeOperationMode.getOperationMode() != OPERATION_MODE.SLEEP) {
							String curTrCmdId = trCmd.getTrCmdId(); 
							TrCmd curTrCmd = trCmdManager.getTrCmd(curTrCmdId);
							if (curTrCmd == null) {
								return;
							}
							trCmd = curTrCmd;
							vehicleData.setAvRetryWait(false);
							
							if (trCmd.getState() == TRCMD_STATE.CMD_TRANSFERRING && trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ASSIGNED){
								resetTargetNode("processTransferUpdate()");
								String targetStation = getStationIdAtPort(trCmd.getDestLoc());
								vehicleData.setTarget(trCmd.getDestNode(), targetStation);
								
								// DestNode가 없는 경우
								// OCS에서 생성한 Unknown TrCmd는 SourceLoc, DestLoc이 없으므로 -> MCS가 해당 Unknown TrCmd에 대해 DestChange를 줬을 때
								// IBSEM에서 해당 TrCmd를 복사하여 DestNode가 없음. 
								if (trCmd.getDestNode() == null || (trCmd.getDestNode() != null && trCmd.getDestNode().length() > 0)) {
									CarrierLoc destLoc = carrierLocManager.getCarrierLocData(trCmd.getDestLoc());
									if (destLoc != null) {
										vehicleData.setTargetNode(destLoc.getNode());
									} else {
										StringBuilder message = new StringBuilder();
										message.append("(Abnormal) TransferUpdate DestLoc is null. UpdateTrCmd:").append(trCmd.getTrCmdId());
										message.append(" DestLoc:").append(trCmd.getDestLoc());
										message.append(" DestNode:").append(trCmd.getDestNode());
										traceOperation(message.toString());
									}
								}
							}
							
							if (curTrCmd.getDestLoc() == null || curTrCmd.getDestLoc().length() == 0) {
								traceOperationException("Abnormal Case: Operation#004");
								return;
							}
							
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_UPDATECOMPLETED, 0);
							
							if (trCmd.isPause()) {
								pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
							}
							// 메모리 값을 DB에 업데이트
							addTrCmdToStateUpdateList();
							addTrCmdToVehicleUpdateList();

							if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
								changeOperationMode(OPERATION_MODE.IDLE, "TransferUpdate");
							}
							StringBuffer log = new StringBuffer("Transfer Update");
							log.append("Command ID: " + trCmd.getTrCmdId());
							traceOperation(log.toString());
							updateChangedInfoReset("Transfer Update");
							setVehicleUserDefinedRoute();
						}
					} else {
						StringBuilder message = new StringBuilder();
						message.append("(Abnormal) TransferUpdate Fail. UpdateTrCmd:").append(trCmd.getTrCmdId());
						message.append(" StopNode:").append(vehicleData.getStopNode());
						message.append(" TargetNode:").append(vehicleData.getTargetNode());
						traceOperation(message.toString());
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_UPDATEFAILED, 0);
						
						updateChangedTargetInfoReset("Transfer Update"); // TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보  RESET (원복)
						
						// 메모리 값을 DB에 업데이트
						addTrCmdToStateUpdateList(); // 현재의 trcmd 상태를 변경하여 UpdateList에 넣어주면 TrCmdManager에서 DB 업데이트를 한다.
						addTrCmdToVehicleUpdateList(); // 현재의  vehicle 상태를 변경하여 UpdateList에 넣어주면 TrCmdManager에서 DB 업데이트를 한다.
						
						return;
					}
				} else {
					if (activeOperationMode.getOperationMode() != OPERATION_MODE.SLEEP) {
						String curTrCmdId = trCmd.getTrCmdId(); 
						TrCmd curTrCmd = trCmdManager.getTrCmd(curTrCmdId);
						if (curTrCmd == null) {
							return;
						}
						trCmd = curTrCmd;
						vehicleData.setAvRetryWait(false);
						
						if (trCmd.getState() == TRCMD_STATE.CMD_TRANSFERRING && trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ASSIGNED){
							resetTargetNode("processTransferUpdate()");
							String targetStation = getStationIdAtPort(trCmd.getDestLoc());
							vehicleData.setTarget(trCmd.getDestNode(), targetStation);
							
							// DestNode가 없는 경우
							// OCS에서 생성한 Unknown TrCmd는 SourceLoc, DestLoc이 없으므로 -> MCS가 해당 Unknown TrCmd에 대해 DestChange를 줬을 때
							// IBSEM에서 해당 TrCmd를 복사하여 DestNode가 없음. 
							if (trCmd.getDestNode() == null || (trCmd.getDestNode() != null && trCmd.getDestNode().length() > 0)) {
								CarrierLoc destLoc = carrierLocManager.getCarrierLocData(trCmd.getDestLoc());
								if (destLoc != null) {
									vehicleData.setTargetNode(destLoc.getNode());
								} else {
									StringBuilder message = new StringBuilder();
									message.append("(Abnormal) TransferUpdate DestLoc is null. UpdateTrCmd:").append(trCmd.getTrCmdId());
									message.append(" DestLoc:").append(trCmd.getDestLoc());
									message.append(" DestNode:").append(trCmd.getDestNode());
									traceOperation(message.toString());
								}
							}
						}
						
						if (curTrCmd.getDestLoc() == null || curTrCmd.getDestLoc().length() == 0) {
							traceOperationException("Abnormal Case: Operation#004");
							return;
						}
						
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_UPDATECOMPLETED, 0);
						
						if (trCmd.isPause()) {
							pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
						}
						// 메모리 값을 DB에 업데이트
						addTrCmdToStateUpdateList();
						addTrCmdToVehicleUpdateList();

						if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
							changeOperationMode(OPERATION_MODE.IDLE, "TransferUpdate");
						}
						StringBuffer log = new StringBuffer("Transfer Update");
						log.append("Command ID: " + trCmd.getTrCmdId());
						traceOperation(log.toString());
						updateChangedInfoReset("Transfer Update");
						setVehicleUserDefinedRoute();
					}
				}
			}
		} else {
			traceOperationException("processTransferUpdate() - trCmd is null.");
		}
	}
	
	/**
	 * Process STAGECHANGE Command
	 */
	private void processStageChange() {
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
			// 2012.01.19 by PMM
//			if (activeOperationMode.getOperationMode() == OPERATION_MODE.IDLE ||
//					activeOperationMode.getOperationMode() == OPERATION_MODE.GO ||
//					activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
			if (activeOperationMode.getOperationMode() != OPERATION_MODE.SLEEP) {
				String stageChangedTrCmdId = trCmd.getChangedTrCmdId();
				TrCmd requestedTrCmd = trCmdManager.getTrCmd(stageChangedTrCmdId);
				if (requestedTrCmd == null) {
					return;
				}
				
				// 2012.01.11 by PMM
				//if (requestedTrCmd.getDestLoc() == null) {
				if (requestedTrCmd.getDestLoc() == null || requestedTrCmd.getDestLoc().length() == 0) {
					traceOperationException("Abnormal Case: Operation#004");
					return;
				}
				
				// TrCmdId가 다르면 기존 TrCmd는 History에 기록 및 삭제 처리 
				// 2011.11.22 by KYK  
				String stageCmdId = trCmd.getTrCmdId();
//				if (trCmd.getTrCmdId().equals(stageChangedTrCmdId) == false) {
				if (stageCmdId.equals(stageChangedTrCmdId) == false) {
					registerTrCompletionHistory(REQUESTEDTYPE.STAGECHANGE.toConstString());
					deleteStageCmdFromDB();
				}

				trCmd = requestedTrCmd;
				vehicleData.setAvRetryWait(false);
				trCmd.setVehicle(vehicleData.getVehicleId());
				trCmd.setAssignedVehicleId(vehicleData.getVehicleId());
				// 2022.03.14 dahye : Premove Logic Improve
				// 기존 : StageChange 시 RemoteCmd를 TRANSFER 로 설정
				// 변경 : StageChange 시 RemoteCmd를 상위로부터 받은 RemoteCmd로 설정
//				trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
				if (trCmd.getDeliveryType().equals("PREMOVE")) {
					trCmd.setRemoteCmd(TRCMD_REMOTECMD.PREMOVE);
				} else {
					trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
				}
				
				// 2012.01.11 by PMM
//				trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
				trCmd.setState(TRCMD_STATE.CMD_WAITING);
				
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);

				if (trCmd.isPause()) {
					pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
				}

				// 메모리 값을 DB에 업데이트
				addTrCmdToStateUpdateList();
				addTrCmdToVehicleUpdateList();

				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_INITIATED, 0);
				if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
					// 2012.01.11 by PMM
//					changeOperationMode(OPERATION_MODE.IDLE, vehicleData.getRequestedType().toConstString());
					changeOperationMode(OPERATION_MODE.IDLE, "StageChange");
				}

				StringBuffer log = new StringBuffer("Stage Change ");
				log.append(stageChangedTrCmdId).append(" To ").append(trCmd.getDestLoc());
				traceOperation(log.toString());

				// TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
				// 2011.11.22 by KYK : STAGE 와 STAGECHANGE 의 TrCmdId 가 같을 경우에만 Reset
				if (stageCmdId.equals(stageChangedTrCmdId)){
					updateChangedInfoReset("StageChange");
				}
//				updateChangedInfoReset("StageChange");
				
				setVehicleUserDefinedRoute();
			}
		}
	}

	/**
	 * Process STAGEDELETE Command
	 */
	private void processStageDelete() {
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
			if (activeOperationMode.getOperationMode() != OPERATION_MODE.SLEEP) {
				// Stage Cancel시 Event History에 기록
				// VHL:OHT201(AA), CMDID:234423, CARRIERID:OYB0123, SRCLOC:EFB01_1233, DESTLOC:EFB03_2233
				StringBuilder message = new StringBuilder();
				message.append("Vehicle:").append(vehicleData.getVehicleId());
				message.append(", TrCmdId:").append(trCmd.getTrCmdId());
				message.append(", CarrierId:").append(trCmd.getCarrierId());
				message.append(", SourceLoc:").append(trCmd.getSourceLoc());
				message.append(", DestLoc:").append(trCmd.getDestLoc());
				registerEventHistory(new EventHistory(
						EVENTHISTORY_NAME.CURRENT_STAGE_DELETE, EVENTHISTORY_TYPE.SYSTEM, "",
						message.toString(), "", "", EVENTHISTORY_REMOTEID.OPERATION, "",
						EVENTHISTORY_REASON.STAGEDELETE), false);

				registerTrCompletionHistory(REQUESTEDTYPE.STAGEDELETE.toConstString());
				// TrCmd의 ChangedRemoteCmd, ChangedTrCmdId 정보 RESET
				updateChangedInfoReset("StageDelete");
				deleteStageCmdFromDB();
				
				// 2012.01.19 by PMM
				// GoMode인 경우, 정상적으로 TargetNode에 도착할 때까지 Mode 유지.
//				if (activeOperationMode.getOperationMode() != OPERATION_MODE.IDLE) {
				if (activeOperationMode.getOperationMode() == OPERATION_MODE.WORK) {
					changeOperationMode(OPERATION_MODE.IDLE, vehicleData.getRequestedType().toConstString());
				}
			}
		}
	}
	
	/**
	 * Process REMOVE Command
	 */
	private void processRemove() {
		assert trCmd != null;
		
		StringBuilder message = new StringBuilder();
		
		// 2016.2.23 by KBS : Patrol VHL 기능 추가
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
			if (trCmd.getDetailState() != TRCMD_DETAILSTATE.NOT_ASSIGNED && trCmd.getDetailState() != TRCMD_DETAILSTATE.PATROL_ASSIGNED) {
				if (!isPatrolCancelCommandSent) {
					message.append("REMOVE by user. Vehicle:").append(vehicleData.getVehicleId());
					message.append(", TrCmdId:").append(trCmd.getTrCmdId());
					message.append(", RemoteCmd:").append(trCmd.getRemoteCmd().toConstString());
					message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
					message.append(", CarrierId:").append(trCmd.getCarrierId());
					message.append(", StartNode:").append(trCmd.getSourceNode());
					message.append(", CurrNode:").append(vehicleData.getCurrNode());
					message.append(", StopNode:").append(vehicleData.getStopNode());
					message.append(", EndNode:").append(trCmd.getDestNode());	
					traceOperation(message.toString());

					sendPatrolCancelCommand();
				}
			} else {
				message.append("REMOVE by user. Vehicle:").append(vehicleData.getVehicleId());
				message.append(", TrCmdId:").append(trCmd.getTrCmdId());
				message.append(", RemoteCmd:").append(trCmd.getRemoteCmd().toConstString());
				message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
				message.append(", CarrierId:").append(trCmd.getCarrierId());
				message.append(", StartNode:").append(trCmd.getSourceNode());
				message.append(", CurrNode:").append(vehicleData.getCurrNode());
				message.append(", StopNode:").append(vehicleData.getStopNode());
				message.append(", EndNode:").append(trCmd.getDestNode());	
				traceOperation(message.toString());
				
				// 배경 : 청소 시작전에 not assigned / unload_assigned일 경우는 TrCmd만 정리
				trCmd.setDeletedTime(getCurrDBTimeStr());
				trCmd.setDetailState(TRCMD_DETAILSTATE.PATROL_CANCELED);
				trCmd.setCarrierLoc(trCmd.getDestLoc());
				addTrCmdToStateUpdateList();
				// registerTrCompletionHistory(REQUESTEDTYPE.PATROL.toConstString());

				resetTargetNode("cancelPatrolCommand()");
				deleteTrCmdFromDB();
			}
			
			return;
		}
		
		// 2012.01.20 by PMM
		switch (trCmd.getRemoteCmd()) {
			case TRANSFER: {
				message.append("REMOVE by user. Vehicle:").append(vehicleData.getVehicleId());
				message.append(", TrCmdId:").append(trCmd.getTrCmdId());
				message.append(", CarrierId:").append(trCmd.getCarrierId());
				message.append(", SourceLoc:").append(trCmd.getSourceLoc());
				message.append(", DestLoc:").append(trCmd.getDestLoc());
				
				// 2012.08.22 by PMM
				// UNLOAD_ASSIGNED 중 사용자에 의한 TrCmd Remove 시, CARRIER_REMOVED 보고된 현상 관련 정리.
				if (vehicleData.getVehicleMode() != 'M') {
					message = new StringBuilder();
					message.append("[Failed] REMOVE Requested by user. Vehicle:").append(vehicleData.getVehicleId());
					message.append(", Mode:").append(vehicleData.getVehicleMode());
					message.append(", Carrier:").append(vehicleData.getCarrierExist());
					message.append(", TrCmdId:").append(trCmd.getTrCmdId());
					message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
					message.append(", CarrierId:").append(trCmd.getCarrierId());
					message.append(", SourceLoc:").append(trCmd.getSourceLoc());
					message.append(", DestLoc:").append(trCmd.getDestLoc());
					traceOperation(message.toString());
					updateChangedInfoReset("Remove");
					return;
				}
				
				switch (trCmd.getDetailState()) {
					case UNLOAD_ASSIGNED:
						trCmd.setCarrierLoc(trCmd.getSourceLoc());
						break;
					case UNLOAD_SENT:
					case UNLOAD_ACCEPTED:
					case UNLOADING:
						if (vehicleData.isCarrierExist()) {
							trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
							sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
						} else {
							trCmd.setCarrierLoc(trCmd.getSourceLoc());
						}
						break;
					default:
						if (vehicleData.isCarrierExist()) {
							trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
						} else {
							sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
							trCmd.setCarrierLoc(trCmd.getDestLoc());
						}
						break;
				}
				
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
				// 2012.11.30 by KYK : ResultCode 세분화
//				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_TRDELETED_BY_USER);
				break;
			}
			case SCAN: {
				message.append("REMOVE by user. Vehicle:").append(vehicleData.getVehicleId());
				message.append(", TrCmdId:").append(trCmd.getTrCmdId());
				message.append(", RemoteCmd:").append(trCmd.getRemoteCmd().toConstString());
				message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
				message.append(", CarrierId:").append(trCmd.getCarrierId());
				message.append(", SourceLoc:").append(trCmd.getSourceLoc());
				message.append(", DestLoc:").append(trCmd.getDestLoc());
	
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCAN_COMPLETED, 23);
				break;
			}
			case VIBRATION: {
				message.append("REMOVE Requested by LongRun(VIBRATION). Vehicle:").append(vehicleData.getVehicleId());
				message.append(", TrCmdId:").append(trCmd.getTrCmdId());
				message.append(", RemoteCmd:").append(trCmd.getRemoteCmd().toConstString());
				message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
				message.append(", CarrierId:").append(trCmd.getCarrierId());
				break;
			}
			default: {
				message.append("REMOVE by user. Vehicle:").append(vehicleData.getVehicleId());
				message.append(", TrCmdId:").append(trCmd.getTrCmdId());
				message.append(", RemoteCmd:").append(trCmd.getRemoteCmd().toConstString());
				message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
				message.append(", CarrierId:").append(trCmd.getCarrierId());
				message.append(", SourceNode:").append(trCmd.getSourceNode());
				message.append(", DestNode:").append(trCmd.getDestNode());
				break;
			}
		}
		trCmd.setDeletedTime(getCurrDBTimeStr());
		// 2013.02.15 by KYK
//		vehicleData.setTargetNode(vehicleData.getStopNode());
		vehicleData.setTarget(vehicleData.getStopNode(), vehicleData.getStopStation());

		addVehicleToUpdateList();
		registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
		// 2012.07.19 by MYM : trCmdManager.deleteTrCmdFromDB() -> deleteTrCmdFromDB() 로 변경 
		// 배경 : 사용자가 TrCmd를 삭제 요청한 경우 TrcompletionHistory에 2번 기록되는 현상 발생 - trCmdManager.deleteTrCmdFromDB에서는 resetTrcmd()가 없음. 
//		trCmdManager.deleteTrCmdFromDB(trCmd.getTrCmdId());
		deleteTrCmdFromDB();
		traceOperation(message.toString());
		updateChangedInfoReset("Remove");
	}
	
	/**
	 * Process PAUSE Command
	 */
	private void processPause() {
		assert trCmd != null;
		
		pauseTrCmd(true, TrCmdConstant.USER, trCmd.getPauseCount());
		
		StringBuilder message = new StringBuilder();
		message.append("PAUSE by user. Vehicle:").append(vehicleData.getVehicleId());
		message.append(", TrCmdId:").append(trCmd.getTrCmdId());
		message.append(", CarrierId:").append(trCmd.getCarrierId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		traceOperation(message.toString());
		
		updateChangedInfoReset("Pause");
	}
	
	/**
	 * Process RESUME Command
	 */
	private void processResume() {
		assert trCmd != null;
		
		pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, trCmd.getPauseCount());
		
		StringBuilder message = new StringBuilder();
		message.append("RESUME by user. Vehicle:").append(vehicleData.getVehicleId());
		message.append(", TrCmdId:").append(trCmd.getTrCmdId());
		message.append(", CarrierId:").append(trCmd.getCarrierId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		traceOperation(message.toString());
		
		updateChangedInfoReset("Resume");
	}

	/**
	 * Reset TargetNode
	 */
	private void resetTargetNode(String from) {
		String prevTargetNode;
		
		prevTargetNode = vehicleData.getTargetNode();
		// 2013.02.15 by KYK
//		vehicleData.setTargetNode(vehicleData.getStopNode());
		vehicleData.setTarget(vehicleData.getStopNode(), vehicleData.getStopStation());
		vehicleData.resetRoutedNodeList();
		addVehicleToUpdateList();
		
		// 2013.07.11 by KYK
		if (prevTargetNode == null) {
			prevTargetNode = "";
		}
		if (prevTargetNode.equals(vehicleData.getTargetNode()) == false) {
			if (prevTargetNode.equals(prevResetTargetNode) == false) {
				StringBuilder message = new StringBuilder();
				message.append("Reset TargetNode as StopNode: ");
				message.append(prevTargetNode);
				message.append(" -> ");
				message.append(vehicleData.getTargetNode());
				message.append(" by ");
				message.append(from);
				traceOperation(message.toString());
			}
			prevResetTargetNode = prevTargetNode;
		}
	}
	
	/**
	 * 2015.06.06 by KYK : targetnode 를 리셋할 때 routeNodeList 정리해주는데 그 안에서 driveFailNode 는 유지해야 할 경우 발생함
	 * DriveFail 지속시 해당 노드를 회피하는 양보주행경로 탐색 필요
	 * @param from
	 * @param isDriveFailNodeReset
	 */
	private void resetTargetNode(String from, boolean isDriveFailNodeReset) {
		String prevTargetNode;
		
		prevTargetNode = vehicleData.getTargetNode();
		// 2013.02.15 by KYK
//		vehicleData.setTargetNode(vehicleData.getStopNode());
		vehicleData.setTarget(vehicleData.getStopNode(), vehicleData.getStopStation());
		vehicleData.resetRoutedNodeList(isDriveFailNodeReset);
		addVehicleToUpdateList();
		
		// 2013.07.11 by KYK
		if (prevTargetNode == null) {
			prevTargetNode = "";
		}
		if (prevTargetNode.equals(vehicleData.getTargetNode()) == false) {
			if (prevTargetNode.equals(prevResetTargetNode) == false) {
				StringBuilder message = new StringBuilder();
				message.append("Reset TargetNode as StopNode: ");
				message.append(prevTargetNode);
				message.append(" -> ");
				message.append(vehicleData.getTargetNode());
				message.append(" by ");
				message.append(from);
				traceOperation(message.toString());
			}
			prevResetTargetNode = prevTargetNode;
		}
	}
	
	/**
	 * Clear LocalGroupInfo of Vehicle
	 * 
	 * @param clearOption
	 */
	public void clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION clearOption) {
		assert isLocalOHTUsed;

		StringBuffer message = new StringBuffer();
		if (vehicleData.getLocalGroupId() != null && vehicleData.getLocalGroupId().length() > 0) {
			switch (clearOption) {
				case UNLOADED_VHL:
				case UNLOADING_VHL:
				case UNLOAD_ASSIGNED_VHL: {
					// 2012.03.16 by PMM
					// STAGE는 DestNode가 없음. -> Bay 정보를 받아올 수 없고,
					// STAGE_CHANGE 후 TRANSFER에서 LocalGroup을 해제해야 함.
					if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.STAGE) {
						// 2014.06.09 by MYM : LocalOHT가 UnkownTrCmd가 생성되었다가 Manual로 된 경우 DestNode Null 체크 조건 추가
//						String destBay = nodeManager.getBayByNodeId(trCmd.getDestNode());
						String destBay = "";
						if (trCmd.getDestNode() != null) {
							destBay = nodeManager.getBayByNodeId(trCmd.getDestNode());
						}
						String localGroupBay = localGroupInfoManager.getBay(vehicleData.getLocalGroupId());
						
						if (destBay.equals(localGroupBay) == false) {
							clearLocalGroupId();
							message.append("DetailTrState:").append(trCmd.getDetailState().toConstString());
							message.append(", LocalGroupId is cleared because the bay of destnode is different. ");
							message.append("VHL (LocalGroupId:").append(vehicleData.getLocalGroupId());
							message.append(", Bay:").append(localGroupBay);
							message.append("), DestNode:").append(trCmd.getDestNode());
							message.append(", DestBay:").append(destBay);
						}
					}
					break;
				}
				case MANUAL_VHL: {
					clearLocalGroupId();
					message.append("LocalGroupInfo is cleared by MANUAL.");
					break;
				}
				case PATHSEARCH_FAIL: {
					clearLocalGroupId();
					message.append("LocalGroupInfo is cleared by PATHSEARCH_FAIL.");
					break;
				}
				case REMOVE_VHL: {
					clearLocalGroupId();
					message.append("LocalGroupInfo is cleared by REMOVE_VHL.");
					break;
				}
				default:
					break;
			}
			traceOperation(message.toString());
		}
	}

	/**
	 * Process MOVE Request
	 */
	private void processMoveRequest(boolean isParkNodeforCarrierloc) {
		
		if (isParkNodeforCarrierloc)
			assert vehicleData.getRequestedType() == REQUESTEDTYPE.PMOVE;
		else
			assert vehicleData.getRequestedType() == REQUESTEDTYPE.MOVE;
		
		
		if (isParkNodeforCarrierloc && vehicleData.getRequestedData() != null)
			vehicleData.setisExistPortofPark(true);
		else 
			vehicleData.setisExistPortofPark(false);
		
		// 2011.11.04 by PMM
		// MOVE PathSearch Fail 후, MOVE 재요청 시 업데이트를 위해
		if (vehicleData.getAlarmCode() == OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH) {
			unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH);
		}
		
		// 2012.02.06 by PMM
		// Park 이동 중, MOVE 요청을 받은 IdleVHL에 대한 reset
		vehicleData.setLocateRequested(false);
		
		// 2012.01.18 by PMM
		String requestedData = vehicleData.getRequestedData();
		if (requestedData != null && requestedData.length() > 0) {
			if (nodeManager.isValidNode(requestedData) && vehicleData.isDetourYieldRequested() == false) {
				// 2012.01.30 by PMM
				// 양보 중, MOVE Request가 오는 경우 MOVE를 무시해야 하는 케이스 있음.
				Node moveRequestedNode = nodeManager.getNode(requestedData);
				if (moveRequestedNode != null) {
					if (vehicleData.containsDriveNode(moveRequestedNode) == false
							|| (vehicleData.getCurrNode().equals(vehicleData.getStopNode())
									&& vehicleData.getStopStation() != null && vehicleData.getStopStation().length() > 0)) { //
						if (vehicleData.isActionHold() == false &&
								(trCmd == null || (trCmd != null && (trCmd.isPause() || trCmd.getState() == TRCMD_STATE.CMD_ABORTED)))) {
							// 2013.02.15 by KYK
							String prevTargetNodeId = vehicleData.getTargetNode();
							String prevTargetStationId = vehicleData.getTargetStation();
							vehicleData.setTarget(requestedData, "");
							if (searchVehiclePath(requestedData, TrCmdConstant.MOVE, false)) {
								// 2013.05.09 by MYM : SearchVehiclePath에서 sendRouteInfoData() 호출하고 있음. 중복임.
//								sendRouteInfoData();
//								vehicleData.setTargetNode(requestedData);
								addVehicleToUpdateList();
								traceOperation("Vehicle Request Move:" + vehicleData.getTargetNode());
								traceUpdateRequestedCmd(requestedData + " Move");
							} else {
								vehicleData.setTarget(prevTargetNodeId, prevTargetStationId);
							}
						}
					} else {
						traceOperation("[Exception] Vehicle Request Move:" + requestedData + " in DriveNodeList");
					}
				}
			} else if (stationManager.isValidStation(requestedData) && vehicleData.isDetourYieldRequested() == false) {
				// 2013.02.15 by KYK
				Station station = (Station) stationManager.getStation(requestedData);
				if (station != null) {
					String toNodeId = station.getParentNodeId();
					Node toNode = nodeManager.getNode(toNodeId);
					if (toNode != null) {
						if (vehicleData.containsDriveNode(toNode) == false 
								|| requestedData.equals(vehicleData.getStopStation()) == false) {
							if (vehicleData.isActionHold() == false
									&& (trCmd == null || trCmd.isPause() || trCmd.getState() == TRCMD_STATE.CMD_ABORTED)) {
								// ??
								String prevTargetNodeId = vehicleData.getTargetNode();
								String prevTargetStationId = vehicleData.getTargetStation();
								vehicleData.setTarget(toNodeId, requestedData);
								if (searchVehiclePath(toNodeId, TrCmdConstant.MOVE, false)) {
									// 2013.05.09 by MYM : SearchVehiclePath에서 sendRouteInfoData() 호출하고 있음. 중복임.
//									sendRouteInfoData();
									addVehicleToUpdateList();
									traceOperation("Vehicle Request Move:" + vehicleData.getTargetNode());
									traceUpdateRequestedCmd(requestedData + " Move");
								} else {
									vehicleData.setTarget(prevTargetNodeId, prevTargetStationId);
								}
							}
						}
					}
				}
			}
			
			if (isParkNodeforCarrierloc)
				updateRequestedCommandReset(REQUESTEDTYPE.PMOVE_RESET, "ParkMoveRequest");
			else
				updateRequestedCommandReset(REQUESTEDTYPE.MOVE_RESET, "MoveRequest");
		}
	}
	
	/**
	 * Process ZONEMOVE Request
	 */
	private void processZoneMoveRequest() {
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.ZONEMOVE;

		if (trCmd != null && (vehicleData.getRequestedData() != null && vehicleData.getRequestedData().length() > 0)) {
			if (vehicleData.isActionHold() == false && trCmd.isPause() == false) {
				// updateVehicleZone(vehicleData.getRequestedData());

				traceOperation("Vehicle Request ZoneMove:" + vehicleData.getRequestedData());
				traceUpdateRequestedCmd(vehicleData.getRequestedData() + " ZoneMove");
				updateRequestedCommandReset(REQUESTEDTYPE.ZONEMOVE_RESET, "ZoneMoveRequest");
			}
		}
	}

	/**
	 * Process YIELD Request
	 */
	private void processYieldRequest() {
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.YIELD;

		if (trCmd == null) {
			// 2012.02.06 by PMM
//			vehicleData.setYieldRequest(true);
			vehicleData.requestYield(vehicleData);
			searchVehicleYieldPath();
			traceOperation("Vehicle Request Yield.");
		}
		traceUpdateRequestedCmd("Yield");
		updateRequestedCommandReset(REQUESTEDTYPE.YIELD_RESET, "YieldRequest");
	}
	
	private void processVehicleAutoRequest(){
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.VEHICLEAUTO;
		
		if(vehicleData.getVehicleMode() == 'M'){
			sendVehicleAutoCommand();
		} else {
			updateRequestedCommandReset(REQUESTEDTYPE.VEHICLEAUTO_RESET, "VehicleAutoRequest");
		}
		
	}
	
	/**
	 * 2014.02.18 by MYM : [Stage Locate 기능]
	 */
	private void processStageRequest() {
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.STAGE;
		
		if (vehicleData.getAlarmCode() == OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH) {
			unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH);
		}
		String requestedData = vehicleData.getRequestedData();
		if (requestedData != null && requestedData.length() > 0
				&& vehicleData.isDetourYieldRequested() == false) {
			if (vehicleData.getTargetNode().equals(requestedData) == false) {
				Node moveRequestedNode = nodeManager.getNode(requestedData);
				if (moveRequestedNode != null) {
					Hid targetHid = moveRequestedNode.getHid();
					if (targetHid != null && targetHid.isAbnormalState() == false) {
						// 2014.06.03 by MYM 
						// Stage 도착 대기시 양보 요청으로 Target이 변경된 Vehicle이 다시 Stage 요청 받았을 때 처리 안되는 현상 보완
//						if (vehicleData.containsDriveNode(moveRequestedNode) == false) {
						// 2014.07.08 by KYK
						if (vehicleData.containsDriveNode(moveRequestedNode) == false || moveRequestedNode.equals(vehicleData.getDriveStopNode())) {
							if (vehicleData.isActionHold() == false) {
								if (searchVehiclePath(requestedData, TrCmdConstant.MOVE, false)) {
									vehicleData.setTarget(requestedData,"");
									vehicleData.setStageRequested(true, 0);
									addVehicleToUpdateList();
									traceOperation("Vehicle Request:" + vehicleData.getRequestedType() + ", TargetNode:" + vehicleData.getTargetNode());
									traceUpdateRequestedCmd("StageLocate " + requestedData);
									return;
								}
							}
						} else {
							traceOperation("[Exception] Vehicle Request STAGE:" + requestedData + " in DriveNodeList");
						}
					} else {
						traceOperation("RequestedNode:" + requestedData + " in Abnormal HID State.");
					}
				}
			} else if (vehicleData.hasArrivedAtTargetNode() == false) {
				return;
			} else {
				// 2015.04.03 by MYM : Stage 도착 후 JobAssign에서 이후 요청 명령(NOBLOCK,WAIT)이 일정시간 없으면 RESET
//				traceOperation("Vehicle Arrived and Waiting for STAGEWAIT");
//				return;
				long currTime = System.currentTimeMillis();
				long elapsedTime = currTime - vehicleData.getStageArrivedTime();
				if (vehicleData.getStageArrivedTime() == 0) {
					vehicleData.setStageArrivedTime(currTime);
					traceOperation("Vehicle Arrived and Waiting for STAGEWAIT");
					return;
				} else if (elapsedTime < 10000) {
					if (elapsedTime % 3000 == 0) {
						traceOperation("Vehicle Arrived and Waiting for STAGEWAIT("+ elapsedTime/1000 + "sec)");
					}
					return;
				} else {
					resetStageRequest("No Request Noblock or wait by JobAssign(TimeOver:"+ elapsedTime/1000 + "sec)");
				}
			}
		}
		updateRequestedCommandReset(REQUESTEDTYPE.STAGE_RESET, "StageRequest");
	}
	
	/**
	 * 2014.02.18 by MYM : [Stage Locate 기능]
	 */
	private void processStageWaitRequest() {
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.STAGEWAIT;
		
		String requestedData = vehicleData.getRequestedData();
		if (requestedData != null && requestedData.length() > 0) {
			// 2014.04.01 by KYK
//			if (vehicleData.hasArrivedAtTargetNode()) {
			if (vehicleData.hasArrivedAtTarget()) {
				try {
					int reqWaitTime = Integer.parseInt(requestedData);
					if (vehicleData.getStageWaitTime() != reqWaitTime) {
						vehicleData.setStageRequested(true, reqWaitTime);					
						traceOperation("Vehicle Request:" + vehicleData.getRequestedType() + ", " + requestedData);
						traceUpdateRequestedCmd("STAGE Waiting");
					}
				} catch (Exception e) {
					traceOperation("[Exception] Vehicle Request STAGEWAIT");
				}
			}
		}
	}
	
	/**
	 * 2014.02.18 by MYM : [Stage Locate 기능]
	 */
	private void processStageNoBlockRequest() {
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.STAGENOBLOCK;
		
		String requestedData = vehicleData.getRequestedData();
		if (requestedData != null && requestedData.length() > 0) {
			try {
				int reqNoBlockTime = Integer.parseInt(requestedData);
				if (vehicleData.getStageWaitTime() != reqNoBlockTime) {
					vehicleData.setStageRequested(true, reqNoBlockTime);
					traceOperation("Vehicle Request:" + vehicleData.getRequestedType() + ", " + requestedData);
					traceUpdateRequestedCmd("STAGE NoBlocking");
				}
			} catch (Exception e) {
				traceOperation("[Exception] Vehicle Request STAGENOBLOCK");
			}
		}
	}
	
	/**
	 * 2014.02.18 by MYM : [Stage Locate 기능]
	 */
	private void processStageCancelRequest() {
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.STAGECANCEL
				|| (vehicleData.getRequestedType() == REQUESTEDTYPE.NULL && vehicleData.isStageRequested());
		
		traceOperation("Vehicle Request:" + vehicleData.getRequestedType() + ", TargetNode:" + vehicleData.getTargetNode());
		traceUpdateRequestedCmd("STAGE CANCEL");
		resetStageRequest("StageCancelRequest");
	}
	
	/**
	 * 2014.02.18 by MYM : [Stage Locate 기능]
	 */
	public void resetStageRequest(String reason) {
		if (vehicleData.getRequestedType() == REQUESTEDTYPE.STAGE || 
				vehicleData.getRequestedType() == REQUESTEDTYPE.STAGENOBLOCK || 
				vehicleData.getRequestedType() == REQUESTEDTYPE.STAGEWAIT ||
				vehicleData.getRequestedType() == REQUESTEDTYPE.STAGECANCEL ||
				vehicleData.getRequestedType() == REQUESTEDTYPE.NULL) {
			resetTargetNode(reason);
			updateRequestedCommandReset(REQUESTEDTYPE.STAGE_RESET, reason);
			vehicleData.setStageRequested(false, 0);
			vehicleData.setStageArrivedTime(0); // 2015.04.03 by MYM : Stage 도착 후 JobAssign에서 이후 요청 명령(NOBLOCK,WAIT)이 일정시간 없으면 RESET
			vehicleData.setLocateRequested(false);
		}
	}

	/**
	 * 2015.06.08 by MYM : driveFail시 Reroute를 위해서 Locate, Stage Reset
	 */
	public void resetRequestForDrivefailOnDiverge(REQUESTEDTYPE type) {
		if (REQUESTEDTYPE.LOCATE_RESET == type) {
			updateRequestedCommandReset(REQUESTEDTYPE.LOCATE_RESET, "ResetLocateRequestForDrivefailOnDiverge");
			vehicleData.setLocateRequested(false);
		} else if (REQUESTEDTYPE.STAGE_RESET == type) {
			updateRequestedCommandReset(REQUESTEDTYPE.STAGE_RESET, "ResetStageRequestForDrivefailOnDiverge");
			vehicleData.setStageRequested(false, 0);
			vehicleData.setStageArrivedTime(0);
		}
	}
	
	private void processLocateRequest(boolean isParkNodeforCarrierloc) {
		if (isParkNodeforCarrierloc)
			assert vehicleData.getRequestedType() == REQUESTEDTYPE.PLOCATE;
		else
			assert vehicleData.getRequestedType() == REQUESTEDTYPE.LOCATE;
		
		if (isParkNodeforCarrierloc && vehicleData.getRequestedData() != null)
			vehicleData.setisExistPortofPark(true);
		else 
			vehicleData.setisExistPortofPark(false);
		
		if (vehicleData.getAlarmCode() == OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH) {
			unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH);
		}
		String requestedData = vehicleData.getRequestedData();
		if (requestedData != null && requestedData.length() > 0
				&& vehicleData.isDetourYieldRequested() == false) {
			// 2013.02.15 by KYK
			if (nodeManager.isValidNode(requestedData)) {
				if (vehicleData.getTargetNode().equals(requestedData) == false) {
					Node moveRequestedNode = nodeManager.getNode(requestedData);
					if (moveRequestedNode != null) {
						Hid targetHid = moveRequestedNode.getHid();
						if (targetHid != null && targetHid.isAbnormalState() == false) {
							if (vehicleData.containsDriveNode(moveRequestedNode) == false) {
								if (vehicleData.isActionHold() == false &&
										trCmd == null || trCmd.isPause() || 
										trCmd.getState() == TRCMD_STATE.CMD_ABORTED ||
										trCmd.getState() == TRCMD_STATE.CMD_MONITORING) {
									// 2013.02.15 by KYK
									String prevTargetNodeId = vehicleData.getTargetNode();
									String prevTargetStationId = vehicleData.getTargetStation();
									vehicleData.setTarget(requestedData, "");
									if (searchVehiclePath(requestedData, TrCmdConstant.MOVE, false)) {
										// 2013.05.09 by MYM : SearchVehiclePath에서 sendRouteInfoData() 호출하고 있음. 중복임.
//										sendRouteInfoData();
//									vehicleData.setTargetNode(requestedData);
										vehicleData.setTarget(requestedData, "");
										vehicleData.setLocateRequested(true);
										addVehicleToUpdateList();
										traceOperation("Vehicle Request LOCATE:" + vehicleData.getTargetNode());
										traceUpdateRequestedCmd(requestedData + " Locate");
										return;
									} else {
										vehicleData.setTarget(prevTargetNodeId, prevTargetStationId);
									}
								}
							} else {
								traceOperation("[Exception] Vehicle Request LOCATE:" + requestedData + " in DriveNodeList");
							}
						} else {
							traceOperation("RequestedNode:" + requestedData + " in AbnormalState.");
						}
					}
				} else {
					if (isParkNodeforCarrierloc)
						updateRequestedCommandReset(REQUESTEDTYPE.PLOCATE_RESET, "ParkLocateRequest");
					return;
				}
			} else if (stationManager.isValidStation(requestedData)) {
				if (vehicleData.getTargetStation().equals(requestedData) == false) {
					Station station = stationManager.getStation(requestedData);
					if (station != null) {
						String toNodeId = station.getParentNodeId();
						Node toNode = nodeManager.getNode(toNodeId);
						if (toNode != null) {
							Hid targetHid = toNode.getHid();
							if (targetHid != null && targetHid.isAbnormalState() == false) {
								if (vehicleData.containsDriveNode(toNode) == false
										|| requestedData.equals(vehicleData.getCurrStation()) == false) {
									if (vehicleData.isActionHold() == false &&
											trCmd == null || trCmd.isPause() || 
											trCmd.getState() == TRCMD_STATE.CMD_ABORTED ||
											trCmd.getState() == TRCMD_STATE.CMD_MONITORING) {
										// 2013.02.15 by KYK
										String prevTargetNodeId = vehicleData.getTargetNode();
										String prevTargetStationId = vehicleData.getTargetStation();
										vehicleData.setTarget(toNodeId, requestedData);
										if (searchVehiclePath(toNodeId, TrCmdConstant.MOVE, false)) {
											// 2013.05.09 by MYM : SearchVehiclePath에서 sendRouteInfoData() 호출하고 있음. 중복임.
//											sendRouteInfoData();
											vehicleData.setLocateRequested(true);
											addVehicleToUpdateList();
											traceOperation("Vehicle Request LOCATE:" + vehicleData.getTargetNode());
											traceUpdateRequestedCmd(requestedData + " Locate");
											return;
										} else {
											vehicleData.setTarget(prevTargetNodeId, prevTargetStationId);
											addVehicleToUpdateList();
										}
									}
								} else {
									traceOperation("[Exception] Vehicle Request LOCATE:" + requestedData + " in DriveNodeList");
								}
							} else {
								traceOperation("RequestedStation:" + requestedData + " in AbnormalState.");
							}
						}
					}
				} else {
					return;
				}
			}
		}
		if (isParkNodeforCarrierloc)
			updateRequestedCommandReset(REQUESTEDTYPE.PLOCATE_RESET, "ParkLocateRequest");
		else
			updateRequestedCommandReset(REQUESTEDTYPE.LOCATE_RESET, "LocateRequest");
	}
	
	private void processLocateCancelRequest() {
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.LOCATECANCEL;
		
		String requestedData = vehicleData.getRequestedData();
		traceOperation("Vehicle Request LOCATECANCEL:" + requestedData + ", TargetNode:" + vehicleData.getTargetNode());
		if (trCmd == null) {
			resetTargetNode("processLocateCancelRequest()");
		}
		vehicleData.setLocateRequested(false);
		updateRequestedCommandReset(REQUESTEDTYPE.LOCATE_RESET, "LocateCancelRequest");
	}
	
	public void resetLocateRequest() {
		if (vehicleData.getRequestedType() == REQUESTEDTYPE.LOCATE ) {
			updateRequestedCommandReset(REQUESTEDTYPE.LOCATE_RESET, "ResetLocateRequest");
		}
		vehicleData.setLocateRequested(false);
	}
	
	/**
	 * Process RESET Request
	 */
	private void processResetRequest() {
		assert vehicleData.getRequestedType() == REQUESTEDTYPE.RESET;

		if (vehicleData.getCurrCmd() == 0) {
			vehicleComm.stopVehicleComm();
			changeOperationMode(OPERATION_MODE.IDLE, "VehicleInfo Reset.");
			// vehicleData.reset();
			vehicleComm.startVehicleComm();
			traceOperation("Vehicle Request Reset.");
			traceUpdateRequestedCmd("Reset");
			updateRequestedCommandReset(REQUESTEDTYPE.RESET, "ResetRequest");
		}
	}

	/**
	 * Register Report
	 */
	public void registerReport(String message) {
		if (serviceState == MODULE_STATE.INSERVICE && isIBSEMUsed) {
			// OCSRegistered이면 Report하지 않음. S6F11에서 체크하지만 마지막으로 한 번 더 확인.
			if (trCmd != null && trCmd.isOcsRegistered()) {
				traceHostReport("[OCSRegistered TrCmd] Report: " + message);
				return;
			}
			
			ibsemReportManager.registerReport(message);
			traceHostReport("RegisterReport: " + message);
		}
	}

	/**
	 * Manage Vehicle CommandId
	 * 
	 * @param prevCmd
	 * @param currCmd
	 * @param nextCmd
	 * @return
	 */
	private boolean manageVehicleCommandId(int prevCmd, int currCmd, int nextCmd) {
		if (vehicleCommCommand.getCommandId() != 0) {
			if (((vehicleCommCommand.getCommandId() == currCmd) || (vehicleCommCommand.getCommandId() == nextCmd))
					&& (cmdState == COMMAND_STATE.RESPONDED)) {
				// 전송한 Cmd가 실행상태인 경우
				cmdState = COMMAND_STATE.EXECUTING;
			} else if ((vehicleCommCommand.getCommandId() == prevCmd)
					&& ((cmdState == COMMAND_STATE.RESPONDED)
							|| (cmdState == COMMAND_STATE.EXECUTING) || (cmdState == COMMAND_STATE.TIMEOUT))) {
				// 전송한 Cmd가 완료상태인 경우
				cmdState = COMMAND_STATE.EXECUTED;
			}
			// 2014.08.13 by MYM : Abnormal CmdReply 확인
//			} else if ((cmdState == COMMAND_STATE.UNKNOWN) || ((cmdState == COMMAND_STATE.WAITFORRESPONSE))) {
//				// 응답 수신상태에는 명령 미진행시에는 Retry를 실시하도록 설정함
//				cmdState = COMMAND_STATE.TIMEOUT;
//			}
			else if ((cmdState == COMMAND_STATE.UNKNOWN && (isResendCmdForAbnormalReply || vehicleData.getReply() == 'T'))
					|| (cmdState == COMMAND_STATE.WAITFORRESPONSE)) {
				// 명령 전송 후 CMD_SENT 상태가 5초 이상인 경우 TimeOut 처리
				// UNKOWN 중 TimeOut인 경우, 나머지 Datalogic, Pause, Error는 그대로 유지
				cmdState = COMMAND_STATE.TIMEOUT;
			}
		} else {
			// 초기 시작 or EStop or IDReset의 경우 아래를 경유함
			if (currCmd == 0) {
				if (prevCmd == 0) {
					// Vehicle 초기화 이후 통신 개시상태인 경우
					cmdState = COMMAND_STATE.READY;
				} else {
					// Unload/Load 명령을 전송하고 OCS가 Restart된 경우에 완료처리를 위해 상태변경
					cmdState = COMMAND_STATE.EXECUTED;
				}
			} else {
				// Going중 NU/NL Cancel 전송한 후 CurrCmd가 0이 아니면 Executing으로 관리
				// ex) P:9 C:1 N:0 V:0
				cmdState = COMMAND_STATE.EXECUTING;
			}
		}
		return true;
	}

	/**
	 * Change CommandState to SENT
	 */
	public void changeCommandStateToSent(COMMAND_TYPE commandType) {
		cmdState = COMMAND_STATE.SENT;
		vehicleComm.setLastSentCommand(commandType);
		lastCommandSentTime = System.currentTimeMillis();
	}
	
	public void changeLastDiffentCommandState(String sentMessage) {
		if (sentMessage != null && sentMessage.length() > 0) {
			if (sentMessage.equals(lastDifferentCommand) == false) {
				lastDifferentCommandSentTime = System.currentTimeMillis();
			}
			lastDifferentCommand = sentMessage;
		}
	}

	/**
	 * Vehicle로 Emergency Stop Command 메시지를 전송한다.
	 * 
	 * @return boolean
	 */
	public boolean sendEStopCommand(int type) {
		// 2011.11.01 by PMM
		// INSERVICE에서만 명령 전송
		if (serviceState == MODULE_STATE.INSERVICE) {
			changeCommandStateToSent(COMMAND_TYPE.ESTOP);
			traceOperation("ESTOP / State:Sent / Type:" + type);
			lastSentEstopType = type;
			
			return vehicleComm.sendEStopCommand(type);
		}
		return false;
	}

	/**
	 * Vehicle로 Cancel Command 메시지를 전송한다.
	 * 
	 * @param commandId
	 *          int : Command ID
	 * @param commandOption
	 *          char : Next Command(N:Next Command, X:No use NextCmd)
	 * @return boolean
	 */
	public boolean sendCancelCommand(int commandId, char commandOption) {
		// 2011.11.01 by PMM
		// INSERVICE에서만 명령 전송
		if (serviceState == MODULE_STATE.INSERVICE) {
			vehicleCommCommand.setCommandId(commandId);
			vehicleCommCommand.setCommandOption(commandOption);
			
			changeCommandStateToSent(COMMAND_TYPE.CANCEL);
			
			StringBuilder message = new StringBuilder();
			message.append("CANCEL (ID:0, CancelCmdID:").append(commandId);
			message.append(", StopNode:").append(vehicleData.getStopNode());
			message.append(", NextCmd:").append(commandOption);
			message.append(")");
			traceOperation(message.toString());
			return vehicleComm.sendCancelCommand(vehicleCommCommand);
		}
		return false;
	}

	/**
	 * Vehicle로 ID Reset Command 메시지를 전송한다.
	 */
	public boolean sendIDResetCommand() {
		// 2011.11.01 by PMM
		// INSERVICE에서만 명령 전송
		if (serviceState == MODULE_STATE.INSERVICE) {
			// IDReset Command 중복 전송 방지
			if (isIDResetCommandSent == false) {
				vehicleCommCommand.setCommandId(0);

				changeCommandStateToSent(COMMAND_TYPE.IDRESET);
				traceOperation("IDRESET / State:Sent");
				isIDResetCommandSent = true;
				
				return vehicleComm.sendIDResetCommand();
			}
		}
		return false;
	}
	
	/**
	 * Vehicle로 ID Reset Command 메시지를 전송한다. (Patrol)
	 */
	public boolean sendIDResetCommandByPatrol() {
		if (serviceState == MODULE_STATE.INSERVICE) {
			// IDReset Command 중복 전송 방지
			if (isIDResetCommandSent == false) {
				vehicleCommCommand.setCommandId(0);

				changeCommandStateToSent(COMMAND_TYPE.IDRESET);
				
				isIDResetCommandSent = true;
				
				// 2016.2.23 by KBS : IDReset 처리시 정리
				// 배경 : IDReset 처리가 지연될 경우 LongRun에서 만드는 TrCmd들을 삭제하는 문제 발생
				if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
					// LongRun disable
					if (!isPatrolCancelCommandSent) {
						// OHT 이상으로 PatrolCancel이 시작된 경우 LongRun disable
						disableUserRequest();
					}

					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.PATROLLING) {
						// TrCmd 정리
						trCmd.setDeletedTime(getCurrDBTimeStr());
						trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
						trCmd.setDetailState(TRCMD_DETAILSTATE.PATROL_CANCELED);
						trCmd.setCarrierLoc(trCmd.getDestLoc());
						addTrCmdToStateUpdateList();
//						registerTrCompletionHistory(REQUESTEDTYPE.PATROL.toConstString());
					}
					resetTargetNode("sendIDResetCommandByPatrol()");
					deleteTrCmdFromDB();
				}
				traceOperation("IDRESET / State:Sent");
				traceOperation("PatrolCancel Completed.");
				
				return vehicleComm.sendIDResetCommand();
			}
		}
		return false;
	}

	/**
	 * Send PAUSE Command to Vehicle
	 * 
	 * @return
	 */
	public boolean sendPauseCommand() {
		// 2011.11.01 by PMM
		// INSERVICE에서만 명령 전송
		if (serviceState == MODULE_STATE.INSERVICE) {
			changeCommandStateToSent(COMMAND_TYPE.PAUSE);
			traceOperation("Pause / State:Sent");

			return vehicleComm.sendPauseCommand();
		}
		return false;
	}

	/**
	 * Send RESUME Command to Vehicle
	 * 
	 * @return
	 */
	public boolean sendResumeCommand() {
		// 2011.11.01 by PMM
		// INSERVICE에서만 명령 전송
		if (serviceState == MODULE_STATE.INSERVICE) {
			changeCommandStateToSent(COMMAND_TYPE.RESUME);
			traceOperation("Resume / State:Sent");
			
			return vehicleComm.sendResumeCommand();
		}
		return false;
	}
	
	private boolean sendPatrolCancelCommand() {
		if (serviceState == MODULE_STATE.INSERVICE) {
			if (vehicleComm.getLastSentCommand() != COMMAND_TYPE.PATROLCANCEL ||
					System.currentTimeMillis() - lastCommandSentTime > 5000) {
				// PatrolCancel에 대한 Timeout 처리 없음.
				vehicleComm.setLastSentCommand(COMMAND_TYPE.PATROLCANCEL);
				lastCommandSentTime = System.currentTimeMillis();

				traceOperation("PatrolCancel / State:Sent");
				isPatrolCancelCommandSent = true;
				return vehicleComm.sendPatrolCancelCommand(getVehicleCommCommand());
			} else {
				traceOperation("PatrolCancel Already Sent.");
			}
		}
		return false;
	}
	
	/**
	 * Vehicle로 RouteInfo 메시지를 전송한다.
	 * 
	 * @return boolean
	 */
	public boolean sendRouteInfoData() {
		// 2011.11.30 by PMM
		if (serviceState == MODULE_STATE.INSERVICE) {
			String locusData = vehicleData.getLocusData();
			traceOperation(locusData);
			
			if (isEmulatorMode) {
				vehicleComm.setLastSentCommand(COMMAND_TYPE.ROUTEINFODATA);
				vehicleCommCommand.setRouteInfoData(locusData);
				vehicleComm.sendRouteInfoData(vehicleCommCommand);
			}
			
//			if (ocsInfoManager.isSteeringReadyUsed()) {
			if (isSteeringReadyUsed) {
				if (vehicleData.getRoutedNodeCount() > 0) {
					Node routedNode = null;
					routedIntersectionNodeList.clear();
					for (int i = 0; i < vehicleData.getRoutedNodeCount(); i++) {
						routedNode = vehicleData.getRoutedNode(i);
						if (routedNode != null) {
							if (routedNode.isConverge() || routedNode.isDiverge()) {
								routedIntersectionNodeList.add(routedNode.getNodeId());
							}
						}
					}
					Node targetNode = vehicleData.getRoutedNode(vehicleData.getRoutedNodeCount() - 1);
					if (targetNode != null) {
						vehicleCommCommand.setTargetNode(targetNode.getNodeId());
					} else {
						// 예비용.
						vehicleCommCommand.setTargetNode(vehicleData.getTargetNode());
					}
					vehicleCommCommand.setRoutedIntersectionNodeList(routedIntersectionNodeList);
					vehicleComm.setLastSentCommand(COMMAND_TYPE.INTERSECTIONNODES);
					vehicleComm.sendIntersectionNodes(vehicleCommCommand);
					routedIntersectionNodeList.clear();
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Send VehicleAuto Command to Vehicle
	 *  - 2020.05.11 by YSJ (OHT Auto Change)
	 * @return
	 */
	public boolean sendVehicleAutoCommand() {
		// 2011.11.01 by PMM
		// INSERVICE에서만 명령 전송
		if (serviceState == MODULE_STATE.INSERVICE) {
			changeCommandStateToSent(COMMAND_TYPE.VEHICLEAUTO);
			traceOperation("VehicleAuto / State:Sent");
			
			return vehicleComm.sendAutoCommand();
		}
		return false;
	}

	/**
	 * Change Operation Mode
	 * 
	 * @param mode
	 * @param message
	 */
	public void changeOperationMode(OPERATION_MODE mode, String message) {
		if (this.activeOperationMode.getOperationMode() != mode) {
			traceOperation("OperationMode (" + this.activeOperationMode.getOperationMode().toConstString() + "->" + mode.toConstString() + ") by " + message + ".");
			switch (mode) {
				case GO:
					goMode.setPreviousOperationMode(this.activeOperationMode.getOperationMode());
					this.activeOperationMode = goMode;
					traceOperation("  VehicleData: CurrNode(" + vehicleData.getCurrNode() + "), StopNode(" + vehicleData.getStopNode() + "), TargetNode(" + vehicleData.getTargetNode() + ")");
					break;
				case WORK:
					workMode.setPreviousOperationMode(this.activeOperationMode.getOperationMode());
					this.activeOperationMode = workMode;
					resetForRerouting();
					break;
				case SLEEP:
					sleepMode.setPreviousOperationMode(this.activeOperationMode.getOperationMode());
					this.activeOperationMode = sleepMode;
					break;
				case IDLE:
				default:
					idleMode.setPreviousOperationMode(this.activeOperationMode.getOperationMode());
					this.activeOperationMode = idleMode;
					resetForRerouting();
					break;
			}
		}
	}

	/**
	 * Get VehicleData
	 * 
	 * @return
	 */
	public VehicleData getVehicleData() {
		return vehicleData;
	}

	/**
	 * Get VehicleCommCommand
	 * 
	 * @return
	 */
	public VehicleCommCommand getVehicleCommCommand() {
		return vehicleCommCommand;
	}

	/**
	 * Get VehicleComm
	 * 
	 * @return
	 */
	public VehicleComm getVehicleComm() {
		return vehicleComm;
	}

	/**
	 * Get CarrierLoc Type
	 * 
	 * @param carrierLocId
	 * @return
	 */
	public CARRIERLOC_TYPE getCarrierLocType(String carrierLocId) {
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(carrierLocId);
		if (carrierLoc == null) {
			return CARRIERLOC_TYPE.NULL;
		} else {
			return carrierLoc.getType();
		}
	}
	
	/**
	 * 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
	 * 
	 * @param carrierlocId
	 * @param type
	 * @return
	 */
	public boolean isAutoRetryPort(String carrierlocId, JOB_TYPE jobType) {
		if (isAutoRetryUsed == false) {
			return false;
		}
		
		AutoRetryGroupInfo autoRetryGroupInfo = getAutoRetryGroupInfo(carrierlocId);
		
		if (autoRetryGroupInfo != null) {
			if (jobType == JOB_TYPE.UNLOAD) {
				// 2012.09.20 by KYK : LastUnloadError 적용 , 마지막 retryCount 에는 retry 적용안함 에러발생
				if (autoRetryGroupInfo.isUnloadEnabled() && autoRetryGroupInfo.isLastUnloadErrorEnabled()) {
					if (trCmd.getPauseCount() >= autoRetryGroupInfo.getUnloadCount()) {
						return false;
					}
				}
				return autoRetryGroupInfo.isUnloadEnabled();
			} else {
				// 2012.09.20 by KYK : LastloadError 적용 , 마지막 retryCount 에는 retry 적용안함 에러발생
				if (autoRetryGroupInfo.isLoadEnabled() && autoRetryGroupInfo.isLastLoadErrorEnabled()) {
					if (trCmd.getPauseCount() >= autoRetryGroupInfo.getLoadCount()) {
						return false;
					}
				}
				return autoRetryGroupInfo.isLoadEnabled();
			}
		} 
		return false;
	}
	
	/**
	* @author : Jongwon Jung
	* @date : 2021. 4. 8.
	* @description : Load Assign 상태에서 OHT가 Target에 근접
	* @return
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 4. 8. Jongwon 최초 생성 */
	private boolean checkStopTarget() {
		if(trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ASSIGNED && vehicleData.getStopNode().equals(vehicleData.getTargetNode())){
			return true;
		} else{
			return false;
		}
	}
	
	
	public boolean isStopTarget(String a) {
		return false;
	}
	
	public int getHoistSpeedLevel() {
		return hoistSpeedLevel;
	}
	
	public int getShiftSpeedLevel() {
		return shiftSpeedLevel;
	}
	
	public int getAutoRetryLimitCount(String carrierlocId, JOB_TYPE jobType) {
		if (isAutoRetryUsed == false) {
			return 0;
		}
		
		AutoRetryGroupInfo autoRetryGroupInfo = getAutoRetryGroupInfo(carrierlocId);
		
		if (autoRetryGroupInfo != null) {
			if (jobType == JOB_TYPE.UNLOAD) {
				return autoRetryGroupInfo.getUnloadCount();
			} else {
				return autoRetryGroupInfo.getLoadCount();
			}
		} 
		return 0;
	}
	
	/**
	 * 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
	 * 
	 * @param carrierlocId
	 * @return
	 */
	private AutoRetryGroupInfo getAutoRetryGroupInfo(String carrierlocId) {
		CarrierLoc carrierloc = carrierLocManager.getCarrierLocData(carrierlocId);
		if (carrierloc == null) {
			return null;
		}
		
		AutoRetryGroupInfo autoRetryGroupInfo = autoRetryControlManager.getAutoRetryGroupInfo(carrierloc.getAutoRetryGroupId());
		if (autoRetryGroupInfo == null) {
			autoRetryGroupInfo = autoRetryControlManager.getAutoRetryGroupInfo(carrierloc.getType().toConstString());
		}
		return autoRetryGroupInfo;
	}

	/**
	 * Get CommandState
	 */
	public COMMAND_STATE getCommandState() {
		return cmdState;
	}

	/**
	 * Get TrCmd
	 * 
	 * @return
	 */
	public TrCmd getTrCmd() {
		return trCmd;
	}

	/**
	 * Set TrCmd
	 * 
	 * @param trCmd
	 */
	public void setTrCmd(TrCmd trCmd) {
		this.trCmd = trCmd;
		this.activeOperationMode.setTrCmd(trCmd);
	}

	/**
	 * Set CommandState
	 * 
	 * @param cmdState
	 */
	public void setCommandState(COMMAND_STATE cmdState) {
		this.cmdState = cmdState;
	}
	
	/**
	 * Add Vehicle to Update List 
	 */
	public void addVehicleToUpdateList() {
		assert vehicleData != null;
		if (serviceState == MODULE_STATE.INSERVICE) {
			this.vehicleManager.addVehicleToUpdateList(vehicleData);
		}
	}
	
	/**
	 * 2013.04.02 by MYM 
	 * Update Vehicle Locus to DB
	 */
	public void updateVehicleLocusToDB() {
		assert vehicleData != null;
		if (serviceState == MODULE_STATE.INSERVICE) {
			// 2015.08.13 by KYK : Vehicle 상태 업데이트 메소드와 분리 (큰 데이터 불필요하게 업데이트 됨- 변경시에만 업데이트)
			vehicleManager.addVehicleToLocusUpdateList(vehicleData);
			// 2015.08.08 by MYM : batch 업데이트 하도록 변경 (Vehicle 상태 변경 업데이트시 함께 업데이트 함)
//			this.vehicleManager.updateVehicleLocusToDB(vehicleData.getVehicleId(), vehicleData.getLocusDataString());
//			this.vehicleManager.addVehicleToUpdateList(vehicleData);
		}
	}
	
	/**
	 * Clear LocalGroupId
	 */
	public void clearLocalGroupId() {
		assert vehicleData != null;
		if (serviceState == MODULE_STATE.INSERVICE) {
			this.vehicleManager.clearLocalGroupId(vehicleData.getVehicleId());
		}
	}

	/**
	 * Pause TrCmd
	 * 
	 * @param pause
	 * @param pauseType
	 * @param pauseCount
	 */
	public void pauseTrCmd(boolean pause, String pauseType, int pauseCount) {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			trCmd.setPause(pause);
			trCmd.setPauseType(pauseType);
			trCmd.setPauseCount(pauseCount);
			this.trCmdManager.addTrCmdToPauseUpdateList(trCmd);
		}
	}

	/**
	 * Register Unknown TrCmd
	 */
	public void registerUnknownTrCmd() {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (vehicleData.isAssignHold() == false && isLoadingByPass() == false) {
				createUnknownTrCmd();
				if (this.trCmdManager.registerUnknownTrCmd(trCmd)) {
					sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
					// 2013.09.24 by PMM
					// Unknown TrCmd를 DB에 정상적으로 등록 후, CarrierInstalled 보고.
					// 김재진 선임 확인 후, Unknown Carrier 처리 시, VEHICLE_ASSIGNED 이벤트 불필요
//					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);
					this.cancelNextAssignedTrCmd(EVENTHISTORY_REASON.UNKNOWN_TRCMD_REGISTERED);
					
					// 2014.06.09 by MYM : Stage Vehicle이 UnkownTrCmd 생성된 경우 Stage Reset
					if (vehicleData.isStageRequested()) {
						resetStageRequest(EVENTHISTORY_REASON.UNKNOWN_TRCMD_REGISTERED.toConstString());
					}
				}
			}
		}
	}
	
	private void createUnknownTrCmd() {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (vehicleData.isAssignHold() == false && isLoadingByPass() == false) {
				Date time = new Date();
				String date = sdf.format(time);
				
				StringBuffer unknownCarrier = new StringBuffer();
				unknownCarrier.append("UNKNOWN");
				unknownCarrier.append("-");
				unknownCarrier.append(vehicleData.getVehicleLoc());
				unknownCarrier.append("-");
				unknownCarrier.append(date);
				
				trCmd = new TrCmd();
				trCmd.setCarrierId(unknownCarrier.toString());
				trCmd.setTrCmdId("TR_" + trCmd.getCarrierId());		
				trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
				trCmd.setVehicle(vehicleData.getVehicleId());
				trCmd.setAssignedVehicleId(vehicleData.getVehicleId());
				trCmd.setLastAbortedTime(System.currentTimeMillis());
				trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
				trCmd.setState(TRCMD_STATE.CMD_ABORTED);
				trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
				
				// 2013.09.12 by MYM : UnknownTrCmd는 ABORT이므로 Pause 설정하도록 추가
				trCmd.setPause(true);
				trCmd.setPauseType(TrCmdConstant.UNKNOWN_TRCMD);
				trCmd.setPauseCount(0);
				
				vehicleData.setAssignedVehicle(trCmd != null);
				
				// 2012.09.17 by PMM
				// TrQueuedTime이 없는 경우, Remote에서 Exception 발생 가능.
				trCmd.setTrQueuedTime(date);
				
				// MCS에 Report해야 하기 때문에 OCSRegistered를 FALSE로 생성.
				// Longrun에서 생성한 TrCmd는 MCS에 Report하지 않도록 OCSRegistered를 TRUE로 생성.
				trCmd.setOcsRegistered(false);
				trCmd.setChangedRemoteCmd(TRCMD_REMOTECMD.NULL);
				trCmd.setChangedTrCmdId("");
				
				StringBuffer log = new StringBuffer();
				log.append("Unknown TrCmd Registered. ");
				log.append("(TrCmd:").append(trCmd.getRemoteCmd()).append("/").append(trCmd.getTrCmdId()).append("/").append(trCmd.getCarrierId());
				log.append("/").append(trCmd.getState()).append("/").append(trCmd.getDetailState()).append("/").append(trCmd.getLastAbortedTime()).append(")");
				traceOperation(log.toString());
			}
		}
	}

	/**
	 * Reset TrCmd
	 */
	public void resetTrCmd() {
		setTrCmd(null);
		vehicleData.setAssignedVehicle(false);
	}

	/**
	 * Reset ChangedRemoteCmd
	 * 
	 * @param message
	 */
	public void updateChangedInfoReset(String message) {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			// CHANGEDINFO_RESET by xxxx.(Requested:CANCEL/123456, TrCmd:TRANSFER/123456/GYB0012/UNLOADED)
			StringBuffer log = new StringBuffer("CHANGEDINFO_RESET");
			log.append(" by ").append(message).append(".");
			if (trCmd != null) {
				log.append("(Requested:").append(trCmd.getChangedRemoteCmd()).append("/").append(trCmd.getChangedTrCmdId());
				log.append(", TrCmd:").append(trCmd.getRemoteCmd()).append("/").append(trCmd.getTrCmdId()).append("/").append(trCmd.getCarrierId());
				log.append("/").append(trCmd.getState()).append("/").append(trCmd.getDetailState()).append(")");
				
				resetTrCmdChangedInfo();
			} else {
				log.append(" (No TrCmd)");
			}
			traceOperation(log.toString());
		}
	}

	/**
	* @author : Jongwon Jung
	* @date : 2021. 4. 8.
	* @description : Target 정보  Reset
	* @param message
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 4. 8. Jongwon 최초 생성 */
	public void updateChangedTargetInfoReset(String message) {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			// CHANGEDINFO_RESET by xxxx.(Requested:CANCEL/123456, TrCmd:TRANSFER/123456/GYB0012/UNLOADED)
			StringBuffer log = new StringBuffer("CHANGEDINFO_RESET");
			log.append(" by ").append(message).append(".");
			if (trCmd != null) {
				log.append("(Requested:").append(trCmd.getChangedRemoteCmd()).append("/").append(trCmd.getChangedTrCmdId());
				log.append(", TrCmd:").append(trCmd.getRemoteCmd()).append("/").append(trCmd.getTrCmdId()).append("/").append(trCmd.getCarrierId());
				log.append("/").append(trCmd.getState()).append("/").append(trCmd.getDetailState()).append(")");
				
				resetTrCmdTargetInfo();
			} else {
				log.append(" (No TrCmd)");
			}
			traceOperation(log.toString());
		}
	}

	/**
	 * Add TrCmd to State Update List
	 */
	public void addTrCmdToStateUpdateList() {
		assert trCmd != null;
		
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			String currentTime = getCurrDBTimeStr();
			switch (trCmd.getState()) {
				case CMD_COMPLETED: {
					trCmd.setLoadedTime(currentTime);
					StringBuilder message = new StringBuilder();
					message.append("JobCompleted ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" : ");
					message.append(trCmd.getSourceLoc()).append(" > ").append(trCmd.getDestLoc());
					traceProcessTrCmd(message.toString());
					break;
				}
				case CMD_ABORTED: {
					StringBuilder message = new StringBuilder();
					message.append("JobAborted ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" : ");
					message.append(trCmd.getSourceLoc()).append(" > ").append(trCmd.getDestLoc());
					traceProcessTrCmd(message.toString());
					break;
				}
				case CMD_CANCELED: {
					trCmd.setDeletedTime(currentTime);
					StringBuilder message = new StringBuilder();
					message.append("JobRemoved ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" : ");
					message.append(trCmd.getSourceLoc()).append(" > ").append(trCmd.getDestLoc());
					traceProcessTrCmd(message.toString());
					break;
				}
				case CMD_STAGING: {
					trCmd.setUnloadingTime(currentTime);
					StringBuilder message = new StringBuilder();
					message.append("[STAGE] Arrived at SourceNode. ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" : ");
					message.append(trCmd.getSourceLoc()).append("(").append(trCmd.getSourceNode()).append(")");
					traceProcessTrCmd(message.toString());
					break;
				}
				default:
					break;
			}
			
			switch (trCmd.getDetailState()) {
				case UNLOAD_ASSIGNED:
				case STAGE_ASSIGNED:
				case SCAN_ASSIGNED:
				case MAPMAKE_ASSIGNED:
				case PATROL_ASSIGNED: {
					if (trCmd.getUnloadAssignedTime() != null && trCmd.getUnloadAssignedTime().length() < 2) {
						trCmd.setUnloadAssignedTime(currentTime);
					}
					if (vehicleData.getLocalGroupId() != null && vehicleData.getLocalGroupId().length() > 0) {
						if (isLocalOHTUsed) {
//							if (ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL) {
							if (localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL) {
								clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL);
							}
						}
					}
					// 2014.02.06 by KYK : Retry 시 최신시도 시각업데이트 위함
					if (trCmd.getUnloadingTime() != null && trCmd.getUnloadingTime().length() > 0) {
						trCmd.setUnloadingTime("");
					}
		
					StringBuilder message = new StringBuilder();
					message.append("JobAssigned ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" : ");
					message.append(trCmd.getSourceLoc()).append(" > ").append(trCmd.getDestLoc());
					traceProcessTrCmd(message.toString());
					break;
				}
				case UNLOAD_SENT:
				case UNLOAD_ACCEPTED:
				case UNLOADING:
				case SCANNING:
				case MAPMAKING:
				case PATROLLING: {
					if (trCmd.getUnloadingTime() != null && trCmd.getUnloadingTime().length() < 2) {
						trCmd.setUnloadingTime(currentTime);
					}
					if (vehicleData.getLocalGroupId() != null && vehicleData.getLocalGroupId().length() > 0) {
						if (isLocalOHTUsed) {
							// 2011.12.12 by PMM
//							if (ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOADING_VHL ||
//									ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL) {
							if (localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOADING_VHL ||
									localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL) {
								clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION.UNLOADING_VHL);
							}
						}
					}
					break;
				}
				case UNLOADED: {
					if (trCmd.getUnloadingTime() != null && trCmd.getUnloadingTime().length() < 2) {
						trCmd.setUnloadingTime(currentTime);
					}
					if (trCmd.getUnloadedTime() != null && trCmd.getUnloadedTime().length() < 2) {
						trCmd.setUnloadedTime(currentTime);
					}
					if (vehicleData.getLocalGroupId() != null && vehicleData.getLocalGroupId().length() > 0) {
						if (isLocalOHTUsed) {
							// 2011.12.12 by PMM
//							if (ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOADED_VHL ||
//									ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOADING_VHL ||
//									ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL) {
							if (localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOADED_VHL ||
									localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOADING_VHL ||
									localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL) {
								clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION.UNLOADED_VHL);
							}
						}
					}
//					updateRequestedCommandReset(REQUESTEDTYPE.TRANSFER_RESET, "UNLOADED");
		
					StringBuilder message = new StringBuilder();
					message.append("JobUnloaded ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" : ");
					message.append(trCmd.getSourceLoc()).append(" > ").append(trCmd.getDestLoc());
					traceProcessTrCmd(message.toString());
					break;
				}
				case STAGE_NOBLOCKING: {
					if (trCmd.getUnloadingTime() != null && trCmd.getUnloadingTime().length() < 2) {
						trCmd.setUnloadingTime(currentTime);
					}
					break;
				}
				case STAGE_WAITING: {
					if (trCmd.getUnloadedTime() != null && trCmd.getUnloadedTime().length() < 2) {
						trCmd.setUnloadedTime(currentTime);
					}
					if (trCmd.getLoadAssignedTime() != null && trCmd.getLoadAssignedTime().length() < 2) {
						trCmd.setLoadAssignedTime(currentTime);
					}
					if (trCmd.getLoadingTime() != null && trCmd.getLoadingTime().length() < 2) {
						trCmd.setLoadingTime(currentTime);
					}
					break;
				}
				case SCANNED: {
					if (trCmd.getUnloadedTime() != null && trCmd.getUnloadedTime().length() < 2) {
						trCmd.setUnloadedTime(currentTime);
					}
//					updateRequestedCommandReset(REQUESTEDTYPE.SCAN_RESET, "SCANNED");
		
					StringBuilder message = new StringBuilder();
					message.append("JobScanned ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" : ");
					message.append(trCmd.getSourceLoc());
					traceProcessTrCmd(message.toString());
					break;
				}
				case MAPMADE: {
					if (trCmd.getUnloadedTime() != null && trCmd.getUnloadedTime().length() < 2) {
						trCmd.setUnloadedTime(currentTime);
					}
//					updateRequestedCommandReset(REQUESTEDTYPE.MAPMAKE_RESET, "MAPMADE");
		
					StringBuilder message = new StringBuilder();
					message.append("JobMapMade ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" Node:");
					message.append(trCmd.getSourceNode()).append(" > ").append(trCmd.getDestNode());
					traceProcessTrCmd(message.toString());
					break;
				}
				case PATROLLED: {
					if (trCmd.getUnloadedTime() != null && trCmd.getUnloadedTime().length() < 2) {
						trCmd.setUnloadedTime(currentTime);
					}
//					updateRequestedCommandReset(REQUESTEDTYPE.PATROL_RESET, "PATROLLED");
		
					StringBuilder message = new StringBuilder();
					message.append("JobPatrolled ");
					message.append(trCmd.getTrCmdId()).append("/");
					message.append(trCmd.getDetailState().toConstString()).append(" Node:");
					message.append(trCmd.getSourceNode()).append(" > )").append(trCmd.getDestNode());
					traceProcessTrCmd(message.toString());
					break;
				}
				case LOAD_ASSIGNED: {
					if (trCmd.getUnloadingTime() != null && trCmd.getUnloadingTime().length() < 2) {
						trCmd.setUnloadingTime(currentTime);
					}
					if (trCmd.getUnloadedTime() != null && trCmd.getUnloadedTime().length() < 2) {
						trCmd.setUnloadedTime(currentTime);
					}
					if (trCmd.getLoadAssignedTime() != null && trCmd.getLoadAssignedTime().length() < 2) {
						trCmd.setLoadAssignedTime(currentTime);
					}
					
					// 2011.12.12 by PMM
					if (vehicleData.getLocalGroupId() != null && vehicleData.getLocalGroupId().length() > 0) {
						if (isLocalOHTUsed) {
//							if (ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOADED_VHL ||
//									ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOADING_VHL ||
//									ocsInfoManager.getLocalOHTClearOption() == LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL) {
							if (localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOADED_VHL ||
									localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOADING_VHL ||
									localOHTClearOption == LOCALGROUP_CLEAROPTION.UNLOAD_ASSIGNED_VHL) {
								clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION.UNLOADED_VHL);
							}
						}
					}
					// 2014.02.06 by KYK : Retry 시 최신시도 시각업데이트 위함
					if (trCmd.getLoadingTime() != null && trCmd.getLoadingTime().length() > 0) {
						trCmd.setLoadingTime("");
					}

					break;
				}
				case LOAD_SENT:
				case LOAD_ACCEPTED:
				case LOADING: {
					if (trCmd.getLoadingTime() != null && trCmd.getLoadingTime().length() < 2) {
						trCmd.setLoadingTime(currentTime);
					}
					break;
				}
				case LOADED: {
					if (trCmd.getLoadingTime() != null && trCmd.getLoadingTime().length() < 2) {
						trCmd.setLoadingTime(currentTime);
					}
					if (trCmd.getLoadedTime() != null && trCmd.getLoadedTime().length() < 2) {
						trCmd.setLoadedTime(currentTime);
					}
					break;
				}
				default:
					break;
			}
			
			// 2011.12.01 by PMM
			if (isImmediatelyUpdateTrCmdStateToDBNeeded()) {
				// 아래 코드가 시간이 많이 걸리는 경우가 있음. (1 ~ 50 ms)
				long checkTime = System.currentTimeMillis();
				this.trCmdManager.updateTrCmdStateToDB(trCmd);
				long elapsedTime = System.currentTimeMillis() - checkTime;
				if (elapsedTime > 40) {
					StringBuilder message = new StringBuilder();
					message.append("   [Mode:").append(currMode).append("]");
					message.append(" updateTrCmdStateToDB(trCmd) Time: ");
					message.append(elapsedTime);
					message.append("(ms)");
					traceOperationDelay(message.toString());
				}
			} else {
				// 아래 코드는 시간이 적게 걸림. (0 ~ 1? ms)
				this.trCmdManager.addTrCmdToStateUpdateList(trCmd);
			}
		}
	}
	
	/*
	 * 2011.12.01 by PMM
	 * 
	 * Loading 중 AV에 의해 MCS에 비정상 완료 보고 후, 
	 * trCmdManager에 CMD_ABORTED Update 요청 중, (sleep 200 ms)
	 * MCS에서 DestChange 명령이 내려온 케이스 발생. (65 ms)
	 * IBSEM에서 DestChange 명령을 NACK 처리하여 DestChange 처리 지연 발생함.
	 *  05:10:38:379 [SND S6F11] TransferCompleted(11326/WYB00779//S1BZ107A_OUT06.LP->WSSSM5_B1/TranferPort=/CurrLoc=OHTB110_1/ALARMID=0/VehicleState=0/Priority=80/Replace=0/ResultCode=1)
	 *  05:10:38:444 [RCV S2F49] RCMD:TRANSFER
	 *  05:10:38:446 [RCV S2F49]: Invalid CarrierID or Invalid TrCmdStatus/CarrierID=WYB00779/HCACK=3
	 * 
	 * ABORT, CANCEL, PAUSE의 경우, 즉시 Update 방식으로 변경.
	 */
	private boolean isImmediatelyUpdateTrCmdStateToDBNeeded() {
		assert trCmd != null;
		
		switch (trCmd.getRemoteCmd()) {
			case ABORT:
			case CANCEL:
			{
				return true;
			}
			default:
			{
				switch (trCmd.getState()) {
				// 2012.03.21 by PMM
//					case CMD_QUEUED:
					case CMD_PAUSED:
					case CMD_ABORTED:
					{
						return true;
					}
					default:
					{
						switch (trCmd.getDetailState()) {
							case UNLOAD_SENT:
							case LOAD_SENT:
							{
								return true;
							}
							default:
							{
								return false;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Add TrCmd to Vehicle Update List
	 */
	public void addTrCmdToVehicleUpdateList() {
		assert trCmd != null;
		
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (trCmd.getVehicle() == null || trCmd.getVehicle().length() == 0) {
				// 2012.02.02 by PMM
				// Update 방식에서 Reset 방식으로 수정
				// this.trCmdManager.updateTrCmdVehicleToDB(trCmd);
				this.trCmdManager.unassignVehicleFromTrCmdToDB(trCmd);
			} else {
				this.trCmdManager.addTrCmdToVehicleUpdateList(trCmd);
			}
		}
	}
	
	/**
	 * Delete TrCmd from DB
	 */
	public void deleteTrCmdFromDB() {
		assert trCmd != null;
		
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			// 2015.12.21 by KBS : Patrol VHL 기능 추가
			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL && trCmd.getDetailState() != TRCMD_DETAILSTATE.PATROLLED) {
				StringBuffer alarmMessage = new StringBuffer();
				alarmMessage.append("[Patrol_Canceled] Vehicle:").append(vehicleData.getVehicleId());
				alarmMessage.append(", TrCmd:").append(trCmd.getTrCmdId());
				alarmMessage.append(", Source Node:").append(trCmd.getSourceNode());
				alarmMessage.append(", Dest Node:").append(trCmd.getDestNode());
				registerAlarmWithLevel("PatrolVHL", alarmMessage.toString(), ALARMLEVEL.WARNING);
			}
			
			this.trCmdManager.deleteTrCmdFromDB(trCmd.getTrCmdId());
			resetTrCmd();
		}
	}

	/**
	 * Delete STAGE Command from DB
	 */
	public void deleteStageCmdFromDB() {
		assert trCmd != null;
		
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
				this.trCmdManager.deleteStageCmdFromDB(trCmd.getTrCmdId());
				resetTrCmd();
			}
		}
	}
	
	/**
	 * 2022.05.05 by JJW
	 * STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
	 * 
	 * Check Unload TrCmd Exist on DestPort
	 * 
	 * @param destLoc
	 * @return
	 */
	public boolean checkDupSourceLoc(String sourceLoc) {
		return trCmdManager.checkDuplicatedSourceLocFromDB(sourceLoc);
	}

	/**
	 * Check Unload TrCmd Exist on DestPort
	 * 
	 * @param destLoc
	 * @return
	 */
	public boolean checkUnloadTrCmdExistOnDestPort(String destLoc) {
		return trCmdManager.checkUnloadTrCmdExistOnDestPort(destLoc);
	}

	/**
	 * Get Unload TrCmd Exist on DestPort
	 * 
	 * @param destLoc
	 * @return
	 */
	public TrCmd getUnloadTrCmdExistOnDestPort(String destLoc) {
		return trCmdManager.getUnloadTrCmdExistOnDestPort(destLoc);
	}

	/**
	 * Register TrCompletionHistory
	 * 
	 * @param remoteCmd
	 */
	public void registerTrCompletionHistory(String remoteCmd) {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (trCmd != null) {
				String vehicleLocus = "";
				switch (trCmd.getDetailState()) {
					case PATROL_ASSIGNED:
					case PATROL_SENT:
					case PATROL_ACCEPTED: {
						break;
					}
					default: {
						vehicleLocus = vehicleData.getVehicleLocus();
						break;
					}
				}
				TrCompletionHistory trCompletionHistory = new TrCompletionHistory(trCmd, remoteCmd, vehicleLocus);
				trCompletionHistoryManager.addTrCmdToRegisterTrCompletionHistory(trCompletionHistory);
				if (isFormattedLogUsed) {
					traceFormattedTrCompletionHistory(trCompletionHistory);
				}
			}
			vehicleData.resetLocusData();
		}
	}
	
	/**
	 * Reset RequestedCommand
	 */
	public void updateRequestedCommandReset(REQUESTEDTYPE requestedType, String message) {
		switch (requestedType) {
			case LOCATE_RESET:
			case PLOCATE_RESET:
			case STAGE_RESET: // 2014.02.21 by : [Stage Locate 기능]
			case MOVE_RESET:
			case PMOVE_RESET:
			case RESET:
			case ZONEMOVE_RESET:
			case YIELD_RESET:
			case VEHICLEAUTO_RESET:
			{
				StringBuffer log = new StringBuffer(requestedType.toConstString());
				log.append(" by ").append(message).append(".");
				if (trCmd != null) {
					log.append("(VehicleRequested:").append(vehicleData.getRequestedType()).append("/").append(vehicleData.getRequestedData());
					log.append(", TrCmd:").append(trCmd.getRemoteCmd()).append("/").append(trCmd.getTrCmdId()).append("/").append(trCmd.getCarrierId());
					log.append("/").append(trCmd.getState()).append("/").append(trCmd.getDetailState()).append(")");
				}
				resetVehicleRequestedInfo();
				vehicleData.setDetourYieldRequested(false);
				traceOperation(log.toString());
				break;
			}
			default:
				// Operation#010
				traceOperationException("Abnormal Case: Operation#010");
				traceOperation("Invalid Reset");
				break;
		}
	}

	/**
	 * Get Current DB Time
	 * 
	 * @return
	 */
	public String getCurrDBTimeStr() {
		return ocsInfoManager.getCurrDBTimeStr();
	}

	/**
	 * Set AlarmReport
	 * 
	 * @param alarmId
	 */
	public void setAlarmReport(int alarmId) {
		// INSERVICE에서만 Event 보고
		if (serviceState == MODULE_STATE.INSERVICE && isIBSEMUsed) {
			String trCmdId = (trCmd == null ? "" : trCmd.getTrCmdId());
			Message report = new Message();
			report.setMessageName(MessageItem.SET_ALARM_REPORT);
			report.setMessageItem(MessageItem.ALARM_ID, alarmId, false);
			report.setMessageItem(MessageItem.COMMAND_ID, trCmdId, false);
			report.setMessageItem(MessageItem.VEHICLE_ID, vehicleData.getVehicleId(), false);
			report.setMessageItem(MessageItem.VEHICLE_STATE, getAlarmReportOption(vehicleData.getState()), false);
			
			// 2013.10.01 by MYM : UnitAlarmSet Event 추가
			String sourceLoc = "";
			String destLoc = "";
			String vehicleCurrentDomain = "";
			String vehicleCurrentPosition = vehicleData.getCurrNode();
			if (trCmd != null) {
				sourceLoc = trCmd.getSourceLoc();
				destLoc = trCmd.getDestLoc();				
			}
			report.setMessageItem(MessageItem.SOURCE_PORT, sourceLoc, false);
			report.setMessageItem(MessageItem.DEST_PORT, destLoc, false);
			report.setMessageItem(MessageItem.VEHICLE_CURR_DOMAIN, vehicleCurrentDomain, false);
			report.setMessageItem(MessageItem.VEHICLE_CURR_POSITION, vehicleCurrentPosition, false);
			
			registerReport(report.toMessage());
		}
	}

	/**
	 * Clear AlarmReport
	 * 
	 * @param alarmId
	 */
	public void clearAlarmReport(int alarmId) {
		// INSERVICE에서만 Event 보고
		if (serviceState == MODULE_STATE.INSERVICE && isIBSEMUsed) {
			String trCmdId = (trCmd == null ? "" : trCmd.getTrCmdId());
			Message report = new Message();
			report.setMessageName(MessageItem.CLEAR_ALARM_REPORT);
			report.setMessageItem(MessageItem.ALARM_ID, alarmId, false);
			report.setMessageItem(MessageItem.COMMAND_ID, trCmdId, false);
			report.setMessageItem(MessageItem.VEHICLE_ID, vehicleData.getVehicleId(), false);
			report.setMessageItem(MessageItem.VEHICLE_STATE, getAlarmReportOption(vehicleData.getState()), false);
			
			// 2013.10.01 by MYM : UnitAlarmCleared Event 추가
			String sourceLoc = "";
			String destLoc = "";
			String vehicleCurrentDomain = "";
			String vehicleCurrentPosition = vehicleData.getCurrNode();
			if (trCmd != null) {
				sourceLoc = trCmd.getSourceLoc();
				destLoc = trCmd.getDestLoc();				
			}
			report.setMessageItem(MessageItem.SOURCE_PORT, sourceLoc, false);
			report.setMessageItem(MessageItem.DEST_PORT, destLoc, false);
			report.setMessageItem(MessageItem.VEHICLE_CURR_DOMAIN, vehicleCurrentDomain, false);
			report.setMessageItem(MessageItem.VEHICLE_CURR_POSITION, vehicleCurrentPosition, false);
			
			registerReport(report.toMessage());
		}
	}

	/**
	 * Get AlarmReport Option
	 * 
	 * @param vehicleState
	 * @return
	 */
	private int getAlarmReportOption(char vehicleState) {
		switch (vehicleData.getState()) {
			case 'I':
				return 2;
			case 'G':
				return 3;
			case 'A':
				return 4;
			case 'U':
			case 'N':
				return 5;
			case 'L':
			case 'O':
				return 6;
			default:
				return 1;
		}
	}

	/**
	 * Send S6F11
	 * 
	 * @param eventType
	 * @param eventName
	 * @param alarmId
	 */
	public void sendS6F11(EVENT_TYPE eventType, String eventName, int alarmId) {
		if (serviceState == MODULE_STATE.INSERVICE &&
				isIBSEMUsed) {
			// INSERVICE에서만 Event 보고
			if (trCmd == null) {
				switch (eventType) {
					case VEHICLE: {
						// VEHICLE_INSTALLED, VEHICLE_REMOVED
						Message report = new Message();
						report.setMessageName(MessageItem.SEND_S6F11);
						report.setMessageItem(MessageItem.EVENT_TYPE, eventType.toConstString(), false);
						report.setMessageItem(MessageItem.EVENT_NAME, eventName, false);
						report.setMessageItem(MessageItem.COMMAND_ID, "", false);
						report.setMessageItem(MessageItem.VEHICLE_ID, vehicleData.getVehicleId(), false);
						report.setMessageItem(MessageItem.CARRIER_ID, "", false);
						report.setMessageItem(MessageItem.TRANSFER_PORT, "", false);
						registerReport(report.toMessage());
						break;
					}
					case TRCMD: 
					case CARRIER:
					default: {
						traceOperationException("Abnormal Case: Operation#011 - sendS6F11(): " + eventType.toConstString() + "/" + eventName);
						break;
					}
				}
			} else {
				// OCSRegistered TrCmd: MAPMAKE, PATROL, Longrun (VIBRATION, TRANSFER) -> MCS에 Report하지 않음.
				if (trCmd.isOcsRegistered() == false &&
						trCmd.getRemoteCmd() != TRCMD_REMOTECMD.MAPMAKE &&
						trCmd.getRemoteCmd() != TRCMD_REMOTECMD.PATROL &&
						trCmd.getRemoteCmd() != TRCMD_REMOTECMD.VIBRATION) {
					Message report = new Message();
					switch (eventType) {
						case TRCMD: {
							report.setMessageName(MessageItem.SEND_S6F11);
							report.setMessageItem(MessageItem.EVENT_TYPE, eventType.toConstString(), false);
							report.setMessageItem(MessageItem.EVENT_NAME, eventName, false);
							report.setMessageItem(MessageItem.COMMAND_ID, trCmd.getTrCmdId(), false);
							report.setMessageItem(MessageItem.CARRIER_ID, trCmd.getCarrierId(), false);
							report.setMessageItem(MessageItem.CARRIER_LOC, trCmd.getCarrierLoc(), false);
							report.setMessageItem(MessageItem.REPLACE, 0, false);
							report.setMessageItem(MessageItem.PRIORITY, trCmd.getPriority(), false);
							report.setMessageItem(MessageItem.SOURCE_PORT, trCmd.getSourceLoc(), false);
							report.setMessageItem(MessageItem.DEST_PORT, trCmd.getDestLoc(), false);
							report.setMessageItem(MessageItem.RESULT_CODE, alarmId, false);
							break;
						}
						case VEHICLE: {
							report.setMessageName(MessageItem.SEND_S6F11);
							report.setMessageItem(MessageItem.EVENT_TYPE, eventType.toConstString(), false);
							report.setMessageItem(MessageItem.EVENT_NAME, eventName, false);
							report.setMessageItem(MessageItem.COMMAND_ID, trCmd.getTrCmdId(), false);
							report.setMessageItem(MessageItem.VEHICLE_ID, vehicleData.getVehicleId(), false);
							report.setMessageItem(MessageItem.CARRIER_ID, trCmd.getCarrierId(), false);
							if (eventName.equals(OperationConstant.VEHICLE_DEPOSITSTARTED) ||
									eventName.equals(OperationConstant.VEHICLE_DEPOSITCOMPLETED) ||
									(eventName.equals(OperationConstant.VEHICLE_ARRIVED) && vehicleData.getTargetNode().equals(trCmd.getDestNode()))) {
								report.setMessageItem(MessageItem.TRANSFER_PORT, trCmd.getDestLoc(), false);
							} else if (eventName.equals(OperationConstant.VEHICLE_ACQUIRESTARTED) ||
									eventName.equals(OperationConstant.VEHICLE_ACQUIRECOMPLETED) ||
									eventName.equals(OperationConstant.VEHICLE_DEPARTED) ||
									(eventName.equals(OperationConstant.VEHICLE_ARRIVED) && vehicleData.getTargetNode().equals(trCmd.getSourceNode()))) {
								report.setMessageItem(MessageItem.TRANSFER_PORT, trCmd.getSourceLoc(), false);
							} else {
								report.setMessageItem(MessageItem.TRANSFER_PORT, "", false);
							}
							break;
						}
						case CARRIER: {
							report.setMessageName(MessageItem.SEND_S6F11);
							report.setMessageItem(MessageItem.EVENT_TYPE, eventType.toConstString(), false);
							report.setMessageItem(MessageItem.EVENT_NAME, eventName, false);
							report.setMessageItem(MessageItem.COMMAND_ID, trCmd.getTrCmdId(), false);
							report.setMessageItem(MessageItem.VEHICLE_ID, vehicleData.getVehicleId(), false);
							report.setMessageItem(MessageItem.CARRIER_ID, trCmd.getCarrierId(), false);
							report.setMessageItem(MessageItem.CARRIER_LOC, trCmd.getCarrierLoc(), false);
							// 2015.02.25 by KBS : STBC 이상감지 on/off
							if (ocsInfoManager.isSTBReportUsed()) {
								// 2014.11.14 by KBS : STBC 이상감지
								if (eventName.equals(OperationConstant.CARRIER_INSTALLED)) {
									CARRIERLOC_TYPE type = getCarrierLocType(trCmd.getSourceLoc());
									if (CARRIERLOC_TYPE.STBPORT == type || CARRIERLOC_TYPE.UTBPORT == type) {
										traceSTBReportData(OperationConstant.CARRIER_INSTALLED, trCmd);
									}
									report.setMessageItem(MessageItem.TRANSFER_PORT, trCmd.getSourceLoc(), false);
								} else if (eventName.equals(OperationConstant.CARRIER_REMOVED)) {
									CARRIERLOC_TYPE type = getCarrierLocType(trCmd.getDestLoc());
									if (CARRIERLOC_TYPE.STBPORT == type || CARRIERLOC_TYPE.UTBPORT == type) {
										traceSTBReportData(OperationConstant.CARRIER_REMOVED, trCmd);
									}
									report.setMessageItem(MessageItem.TRANSFER_PORT, trCmd.getDestLoc(), false);
								} else {
									report.setMessageItem(MessageItem.TRANSFER_PORT, "", false);
								}
							}

//							// 2012.05.16 by MYM : Rail-Down - S1a Foup, Reticle 통합 반송시 사양(IBSEM Spec for Conveyor usage in one OHT) 대응
//							if (RETICLE.equalsIgnoreCase(vehicleData.getMaterial())) {
//								report.setMessageItem(MessageItem.VEHICLE_TYPE, 1, false);
//							} else if (FOUP.equalsIgnoreCase(vehicleData.getMaterial())) {
//								report.setMessageItem(MessageItem.VEHICLE_TYPE, 0, false);
//							} else {
//								report.setMessageItem(MessageItem.VEHICLE_TYPE, 0, false);
//							}
							
							// 2013.09.06 by KYK
							report.setMessageItem(MessageItem.VEHICLE_TYPE, getCarrierType(), false);
							// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
							report.setMessageItem(MessageItem.FOUPID, trCmd.getFoupId(), false);
							
							break;
						}
						default:
							break;
					}
					registerReport(report.toMessage());
				}
			}
		}
	}
	
	/**
	 * 2015.07.01 by MYM : VehicleInstalled 별도 처리 (Enabled시 serviceState가 OUTOFSERVICE라서 보고 안되는 현상 존재) 
	 */
	public void sendS6F11_VehicleInstalled(EVENT_TYPE eventType, String eventName, MODULE_STATE state) {
		if ((state == MODULE_STATE.INSERVICE) && isIBSEMUsed) {
			// VEHICLE_INSTALLED
			Message report = new Message();
			report.setMessageName(MessageItem.SEND_S6F11);
			report.setMessageItem(MessageItem.EVENT_TYPE, eventType.toConstString(), false);
			report.setMessageItem(MessageItem.EVENT_NAME, eventName, false);
			report.setMessageItem(MessageItem.COMMAND_ID, "", false);
			report.setMessageItem(MessageItem.VEHICLE_ID, vehicleData.getVehicleId(), false);
			report.setMessageItem(MessageItem.CARRIER_ID, "", false);
			report.setMessageItem(MessageItem.TRANSFER_PORT, "", false);
			registerReport(report.toMessage());
			
			ibsemReportManager.registerReport(report.toMessage());
			traceHostReport("RegisterReport: " + report.toMessage());
		}
	}
	
	/**
	 * 2013.09.06 by KYK
	 * @param materialType
	 * @return
	 */
	public int getCarrierType(String materialType) {
		// 2015.07.01 by MYM : CarrierTypeConfig에서 처리 변경(CarrierTypeConfig.xml 추가 및 관리)
//		int carrierType = 100;
//		// 0 = FOUP, 1 = POD, 3 = MAC 
//		if (FOUP.equalsIgnoreCase(materialType)) {
//			carrierType = 0;
//		} else if (RETICLE.equalsIgnoreCase(materialType)) {
//			carrierType = 1;
//		} else if (MAC.equalsIgnoreCase(materialType)) {
//			carrierType = 3;
//		}
//		return carrierType;
		return CarrierTypeConfig.getInstance().getCarrierType(materialType);
	}

	/**
	 * 2013.09.06 by KYK
	 * @return
	 */
	private int getCarrierType() {
		int carrierType = 0;
		String materialType = "";
		if (trCmd != null) {
			CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(trCmd.getSourceLoc());
			if (sourceLoc != null && CARRIERLOC_TYPE.VEHICLEPORT != sourceLoc.getType()) {
				materialType = sourceLoc.getMaterial();
				carrierType = getCarrierType(materialType);
			} else {
				carrierType = vehicleData.getCarrierType();
			}
		}
		return carrierType;
	}

	/**
	 * Report RemoteCmd Completed
	 */
	private void reportRemoteCmdCompleted() {
		assert trCmd != null;

		if (checkCancelRemoteCmdResultReportCondition()) {
			reportRemoteCmdCancelCompleted();
		} else if (checkAbortRemoteCmdResultReportCondition()) {
			reportRemoteCmdAbortCompleted();
		}
	}

	/**
	 * Report RemoteCmd Failed
	 */
	private void reportRemoteCmdFailed() {
		assert trCmd != null;

		if (checkCancelRemoteCmdResultReportCondition()) {
			reportRemoteCmdCancelFailed();
		} else if (checkAbortRemoteCmdResultReportCondition()) {
			reportRemoteCmdAbortFailed();
		}
	}

	/**
	 * Check CANCEL RemoteCmd Result Report Condition
	 * 
	 * @return
	 */
	private boolean checkCancelRemoteCmdResultReportCondition() {
		if (trCmd != null &&
				trCmd.isOcsRegistered() == false &&
				vehicleData.getNextCmd() != 0 &&
				trCmd.getState() == TRCMD_STATE.CMD_CANCELLING &&
				(trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_SENT || trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED)) {
			return true;
		}
		return false;
	}

	/**
	 * Check ABORT RemoteCmd Result Report Condition
	 * 
	 * @return
	 */
	private boolean checkAbortRemoteCmdResultReportCondition() {
		if (trCmd != null &&
				trCmd.isOcsRegistered() == false &&
				vehicleData.getNextCmd() != 0 &&
				trCmd.getState() == TRCMD_STATE.CMD_ABORTING &&
				(trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_SENT || trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED)) {
			return true;
		}
		return false;
	}

	/**
	 * Report RemoteCmd Cancel Completed
	 */
	private void reportRemoteCmdCancelCompleted() {
		assert checkCancelRemoteCmdResultReportCondition();

		// Step 1: TrCmd State 변경.
		trCmd.setState(TRCMD_STATE.CMD_CANCELED);
		trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
		addTrCmdToStateUpdateList();

		// Step 2: History를 DB에 저장.
		registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());

		// Step 3: 작업을 취소하는 경우에 StopNode까지만 이동
		resetTargetNode("reportRemoteCmdCancelCompleted()");

		// Step 4: Report (TransferCancelCompleted, VehicleUnassigned) Msg to MCS
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELCOMPLETED, 0);
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);

		// Step 5: 해당 TrCmdInfo 삭제
		deleteTrCmdFromDB();

		// Step 6: 로그 기록
		traceOperation("Job Cancel: " + trCmd.getTrCmdId());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Cancel");
	}

	/**
	 * Report RemoteCmd Cancel Failed
	 */
	private void reportRemoteCmdCancelFailed() {
		assert checkCancelRemoteCmdResultReportCondition();

		// Step1 : TrCmdStatus 변경(CMD_CANCELLING -> CMD_TRANSFERRING)
		// 기존에는 CMD_CANCELFAILED로 변경하여 MCS에서 다시 CANCEL 명령을 줬을 때 IBSEM에서 NAK를 하였음.
		trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
		trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
		addTrCmdToStateUpdateList();

		// Step2 : Report (TransferCancelFailed) Msg to MCS
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELFAILED, 0);

		traceOperation("Job CancelFailed: " + trCmd.getTrCmdId());
	}

	/**
	 * Report RemoteCmd Abort Completed
	 */
	private void reportRemoteCmdAbortCompleted() {
		assert checkAbortRemoteCmdResultReportCondition();

		// Step 1: TrCmd State 변경.
		trCmd.setLastAbortedTime(System.currentTimeMillis());
		
		// 2012.01.28 by PMM
		trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
		
		trCmd.setState(TRCMD_STATE.CMD_ABORTED);
		trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
		addTrCmdToStateUpdateList();

		// Step 2: 작업을 취소하는 경우에 StopNode까지만 이동
		resetTargetNode("reportRemoteCmdAbortCompleted()");

		// Step 3: Report (TransferAbortCompleted, VehicleUnassigned) Msg to MCS
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTCOMPLETED, 0);
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);

		// Step 4: 로그 기록
		traceOperation("Job Abort: " + trCmd.getTrCmdId());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Abort");
	}

	/**
	 * Report RemoteCmd Abort Failed
	 */
	private void reportRemoteCmdAbortFailed() {
		assert checkAbortRemoteCmdResultReportCondition();
		// Step1 : TrCmdStatus 변경(CMD_ABORTING -> CMD_TRANSFERRING)
		// 기존에는 CMD_ABORTFAILED로 변경하여 MCS에서 다시 CANCEL 명령을 줬을 때 IBSEM에서 NAK를 하였음.
		trCmd.setRemoteCmd(TRCMD_REMOTECMD.TRANSFER);
		trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
		addTrCmdToStateUpdateList();

		// Step2 : Report (TransferAbortFailed) Msg to MCS
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_ABORTFAILED, 0);

		traceOperation("Job AbortFailed: " + trCmd.getTrCmdId());
	}

	/**
	 * Drive Vehicle Path
	 * 
	 * @return
	 */
	public String driveVehiclePath() {
		String newStopNode = "";
		vehicleData.setDetourYieldRequested(false);
		Node targetNode = vehicleData.getDriveTargetNode();
		Hid hid = targetNode.getHid();
		if (hid != null && hid.isAbnormalState()) {
			resetTargetNode("driveVehiclePath() - #01");
			newStopNode = OcsConstant.REQUEST_PATH_SEARCH;
			traceOperation("Request Path Search by Abnormal HID.");
		} else if (isDynamicRoutingUsed && vehicleData.isRepathSearchNeeded(dynamicRoutingHoldTimeout)) {
			Node drivedNode = null;
			for (int i = 0; i < vehicleData.getDriveNodeCount(); i++) {
				drivedNode = vehicleData.getDriveNode(i);
				if (drivedNode != null) {
					vehicleData.addToRedirectedNodeSet(drivedNode);
				}
			}
			newStopNode = OcsConstant.REQUEST_PATH_SEARCH;
			vehicleData.setRepathSearchNeeded(false);
		} else if (checkRepathSearchNeededByRouteReset()) { // 2014.02.26 by KYK : offset 고려
			newStopNode = OcsConstant.REQUEST_PATH_SEARCH;
		} else {
			try {
//				newStopNode = vehicleData.driveVehiclePath(isNearByDrive, driveLimitTime, driveMinNodeCount);
				newStopNode = vehicleData.driveVehiclePath(isNearByDrive, driveLimitTime, driveMinNodeCount, nearbyType, ocsInfoManager, isNearByNormalDrive);
			} catch (Exception e) {
				traceOperationException("driveVehiclePath()", e);
			}
		}
		if (newStopNode != null) {
			if (newStopNode.length() == 0) {
				if (newStopNode.equals(lastPathDriveResult) == false) {
					traceOperation("DriveFail");
				}
				lastPathDriveResult = newStopNode;
				return "";
			} else if (newStopNode.startsWith("DriveFail")) {
				// Reason Update를 위해.
				addVehicleToUpdateList();
				if (newStopNode.equals(lastPathDriveResult) == false) {
					traceOperation(newStopNode);
				}
				lastPathDriveResult = newStopNode;
				
				// 2015.06.01 by MYM : 정지 상태 조건 추가(Unload, Loaded, Initialized, Scanned)
				// 2014.10.01 by KYK : 근접제어도 Drive Fail 지속 시, 경로 변경 적용 
				// 2012.07.09 by PMM
				// 비근접제어 분기노드에서 DriveFail 지속 시, 경로 변경.
//				if (isNearByDrive == false) {
					// 이동 중.
					// 분기 노드에서 작업 중 Cancel되는 케이스 발생 가능하여 조건 추가
//				if (vehicleData.getState() == 'A' &&
//						vehicleData.getStopNode().equals(vehicleData.getTargetNode()) == false) {
				if ((vehicleData.getState() == 'A' || vehicleData.getState() == 'I'
						|| vehicleData.getState() == 'N' || vehicleData.getState() == 'O' || vehicleData.getState() == 'F')
						&& vehicleData.getStopNode().equals(vehicleData.getTargetNode()) == false) {
					// 분기 위치에서 Drive Fail 지속 시 후방 Vehicle이 양보를 요청한 경우 일단 비켜줌
					if (System.currentTimeMillis() - vehicleData.getStateChangedTime() > driveFailLimitTime) {
						if (vehicleData.isYieldRequested()) {
							Node stopNode = vehicleData.getDriveStopNode();
							if (stopNode.isDiverge()) {
								// 2015.06.08 by MYM : DriveFail시 Locate, Stage Cancel
								// 배경 : Locate Vehicle이 분기에서 DriveFail시에는 우회 동작 안함. 
								//       RESET을 하지 않으면 YieldSearch 했다가 다시 LOCATE, STAGE REQUEST 처리를 반복하면서 양보하지 않음. 
								if (vehicleData.isLocateRequested()) {
									resetRequestForDrivefailOnDiverge(REQUESTEDTYPE.LOCATE_RESET);
								} else if (vehicleData.isStageRequested()) {
									resetRequestForDrivefailOnDiverge(REQUESTEDTYPE.STAGE_RESET);
								}
								// 2015.06.08 by MYM,KYK,zzang9un : DriveFail인 경우에는 resetTargetNode에서 driveFailedNode를 reset하지 않도록 함.
								//                                  driveFailedNode 로그 추가
								//                                  양보탐색 후 drive 전 locate (or stage or move) 요청발생으로 다시 reset target 방지
								StringBuffer log = new StringBuffer();
								log.append("Reset TargetNode by DriveFail (").append(((System.currentTimeMillis() - vehicleData.getStateChangedTime())/1000));
								log.append(" s) on DivergeNode:").append(stopNode).append(", DriveFailedNode:").append(vehicleData.getDriveFailedNode());								
								traceOperation(log.toString());
//								resetTargetNode("driveVehiclePath() - #02");
								resetTargetNode("driveVehiclePath() - #02", false);
								vehicleData.setStateChangedTime(System.currentTimeMillis());
								vehicleData.setDetourYieldRequested(true);									
								searchVehicleYieldPath();
							}
						}
					} else {
						Node stopNode = vehicleData.getDriveStopNode();
						if (stopNode.isDiverge() && stopNode != yieldCancelledNode) {
							if (vehicleData.getYieldState() != 'N' &&
									(trCmd == null || trCmd.isPause()) &&
									vehicleData.getRequestedType() == REQUESTEDTYPE.NULL) {
								if (vehicleData.getYieldState() == 'Y') {
									// Yielding 중이면 yieldRequestedVehicle이 null.
									vehicleData.setYieldState('N');
									traceOperation("Reset TargetNode by DriveFail on DivergeNode:" + stopNode.getNodeId() + " During Yielding");
								} else {
									traceOperation("Reset TargetNode by DriveFail on DivergeNode:" + stopNode.getNodeId() + " During YieldRequested");
								}
								resetTargetNode("driveVehiclePath() - #03");
								yieldCancelledNode = stopNode;
							}
						}
					}
				}
				return "";
			} else if (OcsConstant.REQUEST_PATH_SEARCH.equals(newStopNode)) {
				if (trCmd != null) {
					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED &&
							vehicleData.getTargetNode().equals(trCmd.getSourceNode())) {
						searchVehiclePath(trCmd.getSourceNode(), TrCmdConstant.UNLOAD, true);
					} else if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
						searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, true);
					} else if (searchEscapeForAbnormalHid()) {
					} else if (vehicleData.getTargetNode().equals(vehicleData.getStopNode()) == false) {
						// 작업이 있어도 HID Down으로 TargetNode가 바뀐 경우, 일단 TargetNode까지 가야 함.
						searchVehiclePath(vehicleData.getTargetNode(), TrCmdConstant.MOVE, false);
					} else {
						searchVehicleYieldPath();
					}
				} else {
					if (vehicleData.getTargetNode().equals(vehicleData.getStopNode()) == false &&
							vehicleData.getDeadlockType() != DEADLOCK_TYPE.NODE) {
						searchVehiclePath(vehicleData.getTargetNode(), TrCmdConstant.MOVE, false);
					} else if (searchEscapeForAbnormalHid()) {
					} else if (searchVehicleComebackZonePath()) {
					} else {
						searchVehicleYieldPath();
					}
				}
				lastPathDriveResult = newStopNode;
				if (vehicleData.getDeadlockType() == DEADLOCK_TYPE.NODE) {
					vehicleData.setDeadlockType(DEADLOCK_TYPE.NONE);
				}
				return "";
			}
		} else {
			lastPathDriveResult = newStopNode;
			return "";
		}
		
		vehicleData.setDriveFailedNode(null);
		yieldCancelledNode = null;
		
		lastPathDriveResult = newStopNode;
		return newStopNode;
	}
	
	/**
	 * 2014.02.26 by KYK
	 * @return
	 */
	private boolean checkRepathSearchNeededByRouteReset() {
		if (vehicleData.getRoutedNodeList().size() == 0) {
			if (vehicleData.getStopNode().equals(vehicleData.getTargetNode())) {
				String stopStationId = vehicleData.getStopStation();
				if (stopStationId == null || stopStationId.length() == 0) {
					return false;
				} 
				String targetStationId = vehicleData.getTargetStation();
				if (targetStationId == null || targetStationId.length() == 0) {
					return true;
				}
				if (stopStationId.equals(targetStationId)) {
					return false;
				} else {
					Station stopStation = stationManager.getStation(stopStationId);
					Station targetStation = stationManager.getStation(targetStationId);
					if (stopStation != null && targetStation != null) {
						if (stopStation.getOffset() > targetStation.getOffset()) {
							return true;
						} else {
							return false;
						}					
					} else {
						return true;
					}					
				}
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check Rail-down
	 * 
	 * @return
	 */
	public boolean checkRailDown() {
		// 2012.05.16 by MYM : Rail-Down
		// S1a Foup, Reticle 통합 반송시 사양(IBSEM Spec for Conveyor usage in one OHT) 대응
		// Load PathSearch Fail이 발생했을 때
		// 1. 현재 위치가 SourceArea인 경우 Direction이 OUT 노드 중 Disabled 노드가 존재하면 비정상완료 보고(75)
		// 2. 1번 체크 후 RailDown이 아니고 현재 위치가 DestArea인 아닌 경우 Direction이 IN 노드 중 Disabled 노드가 존재하면 비정상완료 보고(75)
		// 나머지 (TrCmd Paused, Alarm 표시)
		Node currNode = vehicleData.getDriveStopNode();
		Node sourceNode = nodeManager.getNode(trCmd.getSourceNode());
		Node destNode = nodeManager.getNode(trCmd.getDestNode());
		if (currNode == null || sourceNode == null || destNode == null) {
			return false;
		}
		
		// 2012.08.22 by MYM : Zone -> Area로 변경
		String currArea = currNode.getAreaId();
		String sourceArea = sourceNode.getAreaId();
		String destArea = destNode.getAreaId();
		if (currArea.length() > 0 && sourceArea.length() > 0 && destArea.length() > 0
				&& (sourceArea.equals(destArea) == false || destArea.equals(currArea) == false)) {
			boolean isRailAvailable = true;
			if (sourceArea.equals(currArea)) {
				// 현재 위치가 SourceZone 인 경우
				isRailAvailable = railDownControlManager.isRailAvailable(sourceArea, OcsConstant.OUT);
			}
			if (isRailAvailable && destArea.equals(currArea) == false) {
				// 현재 위치가 SourceZone, DestZone이 아닌 경우 
				isRailAvailable = railDownControlManager.isRailAvailable(destArea, OcsConstant.IN);
			}
			
			if (isRailAvailable == false) {
				if (TRCMD_STATE.CMD_TRANSFERRING.equals(trCmd.getState())) {
					// TrCmd 상태 변경
					trCmd.setLastAbortedTime(System.currentTimeMillis());
					trCmd.setState(TRCMD_STATE.CMD_ABORTED);
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
					trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
					pauseTrCmd(true, TrCmdConstant.PATH_SEARCH, trCmd.getPauseCount());
					addTrCmdToStateUpdateList();

					// Event 기록 및 Unsuccessful Complete 보고
					cancelNextAssignedTrCmd(EVENTHISTORY_REASON.LOAD_PATHSEARCH_FAIL);
					// 2012.11.30 by KYK : ResultCode 세분화
//					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 75);
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_RAILDOWN);

					// TargetNode를 StopNode로 변경
					resetTargetNode("checkRailDown()");

					// IDLE Mode 변경
					changeOperationMode(OPERATION_MODE.IDLE, "PathSearch Fail(Rail-down)");
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Search Vehicle Path
	 * 
	 * @param toNode
	 * @param type
	 * @param request
	 * @return
	 */
	public boolean searchVehiclePath(String toNode, String type, boolean request) {
		boolean result = false;
//		if (ocsInfoManager.isUserPassThroughUsed() && TrCmdConstant.LOAD.equals(type)) {
		if (isUserPassThroughUsed && TrCmdConstant.LOAD.equals(type)) {
			// 2012.08.21 by MYM : 사용자 경로 지정 개선
			// AS-IS : 반송명령 가져올 때 해당 Vehicle에 사용자 경로 정보를 설정하고 관리하도록 함.
			//         -> 경유지 Node를 지났을 때마다 경유지 Node 제거, 이동중 Manual -> Auto시 미경유한 경유지 Node를 경유한 경로 탐색 진행
			// TO-BE : 반송명령 가져올 때 TrCmd에 사용자 경로 정보 설정
			//         -> Unload 후 Load Search시 한번 사용자 경로 탐색 진행, 이동중 Manual -> Auto, 반송명령 Pause시에는 일반 경로 탐색 진행
			if (trCmd != null) {
				UserDefinedPath userDefinedPath = trCmd.getUserDefinedPath();
				if (userDefinedPath != null) {
					Vector<String> nodeList = new Vector<String>(userDefinedPath.getNodeList());
					result = pathSearch.searchVehiclePathOnUserDefinedRoutes(vehicleData, nodeList, toNode, userDefinedPath.getVehicleLimit());
					trCmd.setUserDefinedPath(null);
				}
			}
		}
		
		if (result == false) {
			if (TrCmdConstant.PATROL.equals(type)) {
				result = pathSearch.searchShortestVehiclePath(vehicleData, toNode);
			} else {
				if (trCmd != null) {
					if (trCmd.isPause() && TrCmdConstant.LOAD.equals(type)) {
						if (wasLoadPathSearchFailed) {
							long elapsedTime = System.currentTimeMillis() - lastLoadPathSearchFailedTime;
							if (elapsedTime >= repathSearchHoldTimeout) {
								StringBuilder message = new StringBuilder();
								message.append("RepathSearch Hold Timeout (");
								message.append(elapsedTime);
								message.append(" msec, over ");
								message.append(repathSearchHoldTimeout/1000);
								message.append(" sec)");
								traceOperation(message.toString());
							} else {
								if (isAbnormalStateChanged) {
									isAbnormalStateChanged = false;
									traceOperation("Abnormal State Changed.");
								} else {
									StringBuilder message = new StringBuilder();
									message.append("Re-LoadPathSearch Holded. (");
									message.append(elapsedTime);
									message.append(" msec)");
									traceOperation(message.toString());
									return false;
								}
							}
						}
					}
				}
				// 2016.02.20 by MYM : Dynamic Traffic 반영시 반송 Priority 고려(Min <= priority <= Max 인 반송만 Traffic Cost 반영)
//				result = pathSearch.searchVehiclePath(vehicleData, toNode);
				result = pathSearch.searchVehiclePath(vehicleData, toNode, (trCmd != null ? trCmd.getPriority() : 0));
			}
		}
		if (result) {
			wasLoadPathSearchFailed = false;
			isAbnormalStateChanged = false;
			
			sendRouteInfoData();
			// 2013.02.15 by KYK
			traceOperation(type + " Path Search Success. ToNode:" + toNode + ", Station:" + vehicleData.getTargetStation());
			changeOperationMode(OPERATION_MODE.GO, type);
			
			switch (vehicleData.getAlarmCode()) {
				case OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH:
					unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH);
					break;
				case OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH:
					unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH);
					break;
				case OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH:
					unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH);
					break;
				default:
					break;
			}
			
			// 2012.07.09 by PMM
			// LOAD Search 성공 시, Paused
			if (TrCmdConstant.LOAD.equals(type)) {
				if (trCmd != null) {
					if (trCmd.isPause() &&
							TrCmdConstant.PATH_SEARCH.equals(trCmd.getPauseType())) {
						pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, trCmd.getPauseCount());
					}
				}
			}
			
			// 2013.04.02 by MYM : Vehicle Locus DB 업데이트
			updateVehicleLocusToDB();
			return true;
		} else {
			if (TrCmdConstant.LOAD.equals(type)) {
				// 2015.01.21 by MYM : 장애 지역 우회 기능 - TransferAbort 처리
				if (wasLoadPathSearchFailed == false) {
					// 최초 Load Search Fail 시간을 기록
					firstLoadPathSearchFailedTime = System.currentTimeMillis();
				} else if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
					// TransferAbort 확인 및 처리
					if (checkDetourTransferAbort()) {
						if (vehicleData.getAlarmCode() == OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH) {
							unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH);
						}
						wasLoadPathSearchFailed = false;
						isAbnormalStateChanged = false;
						return false;
					}
				}
				wasLoadPathSearchFailed = true;
				lastLoadPathSearchFailedTime = System.currentTimeMillis();
				
				StringBuilder message = new StringBuilder();
				message.append("[Search_Fail] Load Search Fail(");
				if (trCmd != null) {
					message.append(trCmd.getCarrierId()).append(",").append(vehicleData.getCurrNode());
					message.append(">").append(trCmd.getDestNode());
				}
				message.append(")");
				traceOperation(message.toString());
			} else {
				wasLoadPathSearchFailed = false;
				isAbnormalStateChanged = false;
			}
			
			// 2011.10.21 by PMM 위치 이동.
			//resetTargetNode();
			if (TrCmdConstant.UNLOAD.equals(type)) {
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
					cancelStageCommand(EVENTHISTORY_REASON.UNLOAD_PATHSEARCH_FAIL);
				} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.MAPMAKE) {
					cancelMapMakeCommand(EVENTHISTORY_REASON.UNLOAD_PATHSEARCH_FAIL);
				} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
					cancelPatrolCommand(EVENTHISTORY_REASON.UNLOAD_PATHSEARCH_FAIL);
				} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
					cancelVibrationCommand(EVENTHISTORY_REASON.UNLOAD_PATHSEARCH_FAIL);
				} else {
					// 2015.01.20 by MYM : 장애 지역 우회 기능 - TransferCancel 처리
//					cancelAssignedTrCmd(EVENTHISTORY_REASON.UNLOAD_PATHSEARCH_FAIL, true);
					if (checkDetourTransferCancel() == false) {						
						cancelAssignedTrCmd(EVENTHISTORY_REASON.UNLOAD_PATHSEARCH_FAIL, true);
					}
				}
			} else if (TrCmdConstant.LOAD.equals(type)) {
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.MAPMAKE) {
					cancelMapMakeCommand(EVENTHISTORY_REASON.MAPMAKE_PATHSEARCH_FAIL);
				} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
					cancelPatrolCommand(EVENTHISTORY_REASON.PATROL_PATHSEARCH_FAIL);
				} else {
					// 2012.05.16 by MYM : Rail-Down
					if (isRailDownCheckUsed && checkRailDown()) {
						return true;
					} else {
						// trCmd Pause
						pauseTrCmd(true, TrCmdConstant.PATH_SEARCH, trCmd.getPauseCount());
						
						// Alarm 등록
						if (vehicleData.getAlarmCode() != OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH) {
							setAlarmCode(OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH);
						}
					}
				}
			} else if (TrCmdConstant.MOVE.equals(type)) {
				if (vehicleData.getAlarmCode() != OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH) {
					setAlarmCode(OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH);
				}
			} else if (TrCmdConstant.PATROL.equals(type)) {
				cancelPatrolCommand(EVENTHISTORY_REASON.PATROL_PATHSEARCH_FAIL);
			} else {
				// ??
				; /*NULL*/
			}
			// 2011.10.21 by PMM 위치 이동
			// 알람 등록 시 TargetNode 유지가 필요함.
			resetTargetNode("searchVehiclePath()");
			vehicleData.resetVehicleLocusList();
			
			// 2012.03.08 by PMM
			// Going 중 JobAssign이면 Idle Mode.
			// Idle Mode에서 Unload PathSearch Fail 시 (SourceNode가 DriveNodeList 내에 있는 경우),
			// Going 중이면 GoMode로. (이후 Idle Mode에서 GoMode로 바꿔주지만, checkVehicleDetection()에서 AbnormalCase로 Exception Log 남김.
			if (vehicleData.getState() == 'G') {
				if (activeOperationMode.getOperationMode() != OPERATION_MODE.GO) {
					changeOperationMode(OPERATION_MODE.GO, "PathSearch Fail(OHT Going)");
				}
			} else if (vehicleData.getCurrNode().equals(vehicleData.getStopNode())) {
				if (activeOperationMode.getOperationMode() != OPERATION_MODE.IDLE) {
					changeOperationMode(OPERATION_MODE.IDLE, "PathSearch Fail");
				}
			}
			return false;
		}
	}
	
	/**
	 * 2015.01.20 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	private boolean checkDetourTransferCancel() {
		boolean checkResult = false;
		try {
			DetourControlManager detourManager = DetourControlManager.getInstance(null, null, false, false, 0);
			if (detourManager == null) {
				return false;
			}
			
			DetourControl detourControl = detourManager.getHidDownDetour();
			if (checkDetourTransferCancel(detourControl, DETOUR_REASON.HID_DOWN)) {
				checkResult = true;
				return true;
			}
			detourControl = detourManager.getVehicleManualDetour();
			if (checkDetourTransferCancel(detourControl, DETOUR_REASON.VEHICLE_MANUAL)) {
				checkResult = true;
				return true;
			}
			detourControl = detourManager.getVehicleErrorDetour();
			if (checkDetourTransferCancel(detourControl, DETOUR_REASON.VEHICLE_ERROR)) {
				checkResult = true;
				return true;
			}
			detourControl = detourManager.getVehicleCommfailDetour();
			if (checkDetourTransferCancel(detourControl, DETOUR_REASON.VEHICLE_COMMFAIL)) {
				checkResult = true;
				return true;
			}
			detourControl = detourManager.getVehicleNotRespondDetour();
			if (checkDetourTransferCancel(detourControl, DETOUR_REASON.VEHICLE_NOTRESPOND)) {
				checkResult = true;
				return true;
			}
			detourControl = detourManager.getNodeDisabledDetour();
			if (checkDetourTransferCancel(detourControl, DETOUR_REASON.NODE_DISABLED)) {
				checkResult = true;
				return true;
			}
			detourControl = detourManager.getLinkDisabledDetour();
			if (checkDetourTransferCancel(detourControl, DETOUR_REASON.LINK_DISABLED)) {
				checkResult = true;
				return true;
			}
		} catch (Exception e) {
			traceOperationException("checkDetourTransferCancel", e);
		} finally {
			if (checkResult) {
				StringBuffer log = new StringBuffer("[DetourTransferCanceled] Unload Search Fail by ");
				log.append(vehicleData.getSearchFailReason());
				traceOperation(log.toString());
				
				// Transfer Cancel 처리
				detourTransferCancel();
			}
		}
		return false;
	}
	
	/**
	 * 2015.01.20 by MYM : 장애 지역 우회 기능
	 * @param detourControl
	 * @param type
	 * @return
	 */
	private boolean checkDetourTransferCancel(DetourControl detourControl, DETOUR_REASON reason) {
		if (detourControl != null && detourControl.isDetourUsed() && detourControl.isTransferCancelUsed()
				&& vehicleData.getSearchFailReason().indexOf(reason.toConstString()) >= 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * 2015.01.20 by MYM : 장애 지역 우회 기능
	 */
	private void detourTransferCancel() {
		trCmd.setRemoteCmd(TRCMD_REMOTECMD.CANCEL);
		trCmd.setState(TRCMD_STATE.CMD_CANCELED);
		addTrCmdToStateUpdateList();

		registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());

		// 2007.01.29 작업을 취소하는 경우에 StopNode까지만 이동
		resetTargetNode("detourTransferCancel()");

		// Report (TransferCancelInitiated, TransferCancelCompleted, VehicleUnassigned) Msg to MCS
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELINITIATED, 0);
		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_CANCELCOMPLETED, 0);
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);

		traceOperation("Job Cancel: " + trCmd.getTrCmdId());
		traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Cancel");

		// 해당 TrCmdInfo 삭제
		deleteTrCmdFromDB();
	}

	/**
	 * 2015.01.20 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	private boolean checkDetourTransferAbort() {
		boolean checkResult = false;
		long elapsedTime = (System.currentTimeMillis() - firstLoadPathSearchFailedTime)/1000;
		int abortTimeOut = 0;
		try {
			DetourControlManager detourManager = DetourControlManager.getInstance(null, null, false, false, 0);
			if (detourManager == null) {
				return false;
			}
			
			if (wasLoadPathSearchFailed) {
				DetourControl detourControl = detourManager.getHidDownDetour();
				if (checkDetourTransferAbort(detourControl, elapsedTime, DETOUR_REASON.HID_DOWN)) {
					checkResult = true;
					abortTimeOut = detourControl.getTransferAbortTimeout();
					return true;
				}
				detourControl = detourManager.getVehicleManualDetour();
				if (checkDetourTransferAbort(detourControl, elapsedTime, DETOUR_REASON.VEHICLE_MANUAL)) {
					checkResult = true;
					abortTimeOut = detourControl.getTransferAbortTimeout();
					return true;
				}
				detourControl = detourManager.getVehicleErrorDetour();
				if (checkDetourTransferAbort(detourControl, elapsedTime, DETOUR_REASON.VEHICLE_ERROR)) {
					checkResult = true;
					abortTimeOut = detourControl.getTransferAbortTimeout();
					return true;
				}
				detourControl = detourManager.getVehicleCommfailDetour();
				if (checkDetourTransferAbort(detourControl, elapsedTime, DETOUR_REASON.VEHICLE_COMMFAIL)) {
					checkResult = true;
					return true;
				}
				detourControl = detourManager.getVehicleNotRespondDetour();
				if (checkDetourTransferAbort(detourControl, elapsedTime, DETOUR_REASON.VEHICLE_NOTRESPOND)) {
					checkResult = true;
					return true;
				}
				detourControl = detourManager.getNodeDisabledDetour();
				if (checkDetourTransferAbort(detourControl, elapsedTime, DETOUR_REASON.NODE_DISABLED)) {
					checkResult = true;
					abortTimeOut = detourControl.getTransferAbortTimeout();
					return true;
				}
				detourControl = detourManager.getLinkDisabledDetour();
				if (checkDetourTransferAbort(detourControl, elapsedTime, DETOUR_REASON.LINK_DISABLED)) {
					checkResult = true;
					abortTimeOut = detourControl.getTransferAbortTimeout();
					return true;
				}
			} else {
				firstLoadPathSearchFailedTime = System.currentTimeMillis();
			}
		} catch (Exception e) {
			traceOperationException("checkDetourTransferAbort", e);
		} finally {
			if (checkResult) {
				StringBuffer log = new StringBuffer("[DetourTransferAborted] Load Search Fail by ");
				log.append(vehicleData.getSearchFailReason());
				log.append("(").append(elapsedTime).append(" sec / TimeOver ").append(abortTimeOut).append(" sec)");
				traceOperation(log.toString());
				
				// Transfer Abort 처리
				detourTransferAbort();
			}
		}
		return false;
	}

	/**
	 * 2015.01.20 by MYM : 장애 지역 우회 기능
	 * @param detourControl
	 * @param elapsedTime
	 * @param type
	 * @return
	 */
	private boolean checkDetourTransferAbort(DetourControl detourControl, long elapsedTime, DETOUR_REASON reason) {
		if (detourControl != null && detourControl.isDetourUsed() && detourControl.isTransferAbortUsed()
				&& vehicleData.getSearchFailReason().indexOf(reason.toConstString()) >= 0
				&& elapsedTime >= detourControl.getTransferAbortTimeout()) {
			return true;
		}
		return false;
	}

	/**
	 * 2015.01.20 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	private boolean detourTransferAbort() {
		if (trCmd.isOcsRegistered() == false) {
			trCmd.setLastAbortedTime(System.currentTimeMillis());
			trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
			trCmd.setState(TRCMD_STATE.CMD_ABORTED);
			addTrCmdToStateUpdateList();

			// 2007.01.29 작업을 취소하는 경우에 StopNode까지만 이동
			resetTargetNode("detourTransferAbort()");

			// 2013.09.10 by MYM : Abort 수신시 TrCmd Pause 정보 변경
			// 배경 : PathSearch Fail 발생 후 Abort 된 경우 TargetNode가 Reset되지 않고 DestNode로 계속 PathSearch 시도함.
			// MCS에서 Abort한 TrCmd는 Pause가 되지 않아 근무자가 Move 요청하여도 처리하지 못함.
			pauseTrCmd(true, TrCmdConstant.ABORTED_BY_DETOUR, 0);

			// Report (VehicleUnassigned, TransferCompleted) Msg to MCS
			sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
			sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_DETOUR);

			traceOperation("Job Abort: " + trCmd.getTrCmdId());
			traceUpdateRequestedCmd(trCmd.getTrCmdId() + " Abort");
			return true;
		}
		return false;
	}

	/**
	 * Search Vehicle Yield Path
	 * 
	 * @return
	 */
	public boolean searchVehicleYieldPath() {
//		if (ocsInfoManager.isYieldSearchUsed()) {
		if (isYieldSearchUsed) {
			if (vehicleData.isYieldRequested()) {
				// 2014.03.07 by MYM : [Stage Locate 기능] : STAGE, STAGWAIT 중인 경우 양보 안하도록 함.
				// 2012.02.06 by PMM
				// Park 요청을 받아 이동 중인 VHL은  YieldSearch 제외.
				// 연속 양보로 인한 Park 기능과의 간섭 문제
				if (vehicleData.isLocateRequested()
						|| vehicleData.getRequestedType() == REQUESTEDTYPE.STAGE
						|| vehicleData.getRequestedType() == REQUESTEDTYPE.STAGEWAIT) {
					vehicleData.resetYieldRequested();
				} else {
					long checkTime = System.currentTimeMillis();
					if (this.yieldSearch.searchVehicleYieldPath(vehicleData)) {
						if (vehicleData.getRoutedNodeCount() > 0) {
							vehicleData.setYieldState('Y');
							sendRouteInfoData();
							// 2013.02.15 by KYK
//							vehicleData.setTargetNode(vehicleData.getDriveTargetNode().getNodeId());
							vehicleData.setTarget(vehicleData.getDriveTargetNode().getNodeId(), "");
							addVehicleToUpdateList();
							traceOperation("Yield Search Success. ToNode:" + vehicleData.getTargetNode() + " Time:" + (System.currentTimeMillis() - checkTime) + "(ms)");
							changeOperationMode(OPERATION_MODE.GO, "Yield Search");
						}
						
						// 2012.08.31 by PMM
						// 15:11:33:017 OHT023> [Search_Fail] Move Search Fail(317>269006)
						// 15:11:33:018 OHT023> MOVE_RESET by MoveRequest.
						// 15:11:58:301 OHT023> Yield Search Success. ToNode:342 Time:1(ms)
						// Move Search Fail 후, Yield Search Success 시, Move Search Fail 알람 삭제 안되는 케이스 생김.
//						if (isAlarmRegistered()) {
//							if (vehicleData.getAlarmCode() == OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH) {
//								unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH);
//							}
//						}
						switch (vehicleData.getAlarmCode()) {
							case OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH:
								unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH);
								break;
							case OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH:
								unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH);
								break;
							case OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH:
								unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH);
								break;
							default:
								break;
						}
						
						// 2012.07.10 by PMM
						vehicleData.resetYieldRequested();
						
						// 2013.04.02 by MYM : Vehicle Locus DB 업데이트
						updateVehicleLocusToDB();
						return true;
					} else {
						// Alarm 등록
						if (vehicleData.getAlarmCode() != OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH) {
							setAlarmCode(OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH);
						}
						return false;
					}
				}
			} else {
				; /*NULL*/
			}
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Set AlarmCode
	 * 
	 * @param alarmCode
	 */
	public void setAlarmCode(int alarmCode) {
		unregisterAllAlarm();
		ALARMLEVEL alarmLevel = ALARMLEVEL.ERROR;
		StringBuffer alarmMessage = new StringBuffer();

		switch (alarmCode) {
			case OcsAlarmConstant.SEARCH_FAIL_BY_MOVE_PATH:
			{
				alarmMessage.append("[Search_Fail] Move Search Fail(");
				// 2012.02.09 by PMM
//				alarmMessage.append(vehicleData.getCurrNode()).append(">").append(vehicleData.getRequestedData()).append(")");
				if (vehicleData.getRequestedData() != null && vehicleData.getRequestedData().length() > 0) {
					// 처음 MOVE Request 받았을 때
					alarmMessage.append(vehicleData.getCurrNode()).append(">").append(vehicleData.getRequestedData()).append(")");
				} else {
					// MOVE 중 Node Disabled 되었을 때
					alarmMessage.append(vehicleData.getCurrNode()).append(">").append(vehicleData.getTargetNode()).append(")");
				}
				break;
			}
			case OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH:
			{
				alarmMessage.append("[Search_Fail] TrCmd Paused by Load Search Fail(");
				alarmMessage.append(trCmd.getCarrierId()).append(",").append(vehicleData.getCurrNode());
				alarmMessage.append(">").append(trCmd.getDestNode()).append(")");
				break;
			}
			case OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH:
			{
				alarmMessage.append("[Search_Fail] No Yield by Yield Search Fail(").append(vehicleData.getCurrNode()).append(")");
				break;
			}
			case OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER:
			{
				alarmMessage.append("[Not_Responding] Unload Command ");
				alarmMessage.append(workModeCheckTime);
				alarmMessage.append("(sec) TimeOver, CurrNode:").append(vehicleData.getCurrNode());
				alarmMessage.append(", Bay:").append(vehicleData.getDriveCurrNode().getBay());
				break;
			}
			case OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER:
			{
				alarmMessage.append("[Not_Responding] Load Command ");
				alarmMessage.append(workModeCheckTime);
				alarmMessage.append("(sec) TimeOver, CurrNode:").append(vehicleData.getCurrNode());
				alarmMessage.append(", Bay:").append(vehicleData.getDriveCurrNode().getBay());
				break;
			}
			case OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER:
			{
				alarmMessage.append("[Not_Responding] Go Command ");
				alarmMessage.append(goModeCheckTime);
				alarmMessage.append("(sec) TimeOver, CurrNode:").append(vehicleData.getCurrNode());
				alarmMessage.append(", Bay:").append(vehicleData.getDriveCurrNode().getBay());
				break;
			}
			case OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS:
			{
				alarmMessage.append("[Not_Sending] OCS Not Sending Go Command ");
				alarmMessage.append(goModeCheckTime);
				alarmMessage.append("(sec) TimeOver, CurrNode:").append(vehicleData.getCurrNode());
				alarmMessage.append(", Bay:").append(vehicleData.getDriveCurrNode().getBay());
				break;
			}
			case OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER:
			{
				// 2017.02.15 by KBS : S1 요청으로 Error 레벨로 원복
//				alarmLevel = ALARMLEVEL.WARNING;
				alarmMessage.append("[Not_Responding] Go Command (VehicleDetected) ");
				alarmMessage.append(goModeVehicleDetectedCheckTime);
				alarmMessage.append("(sec) TimeOver, CurrNode:").append(vehicleData.getCurrNode());
				alarmMessage.append(", Bay:").append(vehicleData.getDriveCurrNode().getBay());
				break;
			}
			case OcsAlarmConstant.DELAYED_DESTCHANGE:
			{
				alarmMessage.append("MCS Abort후 DestChange 처리지연 - ");
				alarmMessage.append(abortCheckTime / 60);
				alarmMessage.append("분 초과. CarrierId:").append(trCmd.getCarrierId());
				alarmMessage.append(", TrCmdId:").append(trCmd.getTrCmdId());
				break;
			}
			case OcsAlarmConstant.ESTOP_BY_UNLOAD_CARRIER_MISMATCH:
			{
				alarmMessage.append("Unload CarrierMismatch(Manual) - Port:").append(trCmd.getSourceLoc());
				alarmMessage.append(", CarrierID:").append(trCmd.getCarrierId());
				alarmMessage.append(", RFData:").append(vehicleData.getRfData());
				break;
			}
			case OcsAlarmConstant.ESTOP_BY_VEHICLE_INIT_FAIL:
			{
				alarmMessage.append("E-Stop by Vehicle Init Fail - Curr:");
				alarmMessage.append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.ESTOP_BY_VEHICLE_DRIVE_FAIL:
			{
				alarmMessage.append("E-Stop by Vehicle Drive Fail - ");
				alarmMessage.append("Curr:").append(vehicleData.getCurrNode());
				alarmMessage.append(", Stop:").append(vehicleData.getStopNode());
				break;
			}
			case OcsAlarmConstant.CARRIER_STATUS_ERROR_NOTRCMD:
			{
				alarmMessage.append("Carrier Status Error. Node:");
				alarmMessage.append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_UNLOAD:
			{
				alarmMessage.append("E-Stop by Vehicle - Carrier Status Error (UNLOAD). Node:");
				alarmMessage.append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_LOAD:
			{
				alarmMessage.append("E-Stop by Vehicle - Carrier Status Error (LOAD). Node:");
				alarmMessage.append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.WARNING_LEVEL_TEMPERATURE:
			{
				alarmMessage.append("Patrol VHL's Temperature exceeds a specified Warning Level. Node:");
				alarmMessage.append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.RECEIVED_CMDREPLY_PROTOCOL:
			{
				// 2014.08.13 by MYM : Abnormal CmdReply 확인
				alarmMessage.append("[Abnormal Reply] ProtocolError");
				alarmMessage.append(", ErrorCode:").append(vehicleComm.getVehicleCommData().getReplyErrorCode());
				alarmMessage.append(", Node:").append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.RECEIVED_CMDREPLY_DATALOGIC:
			{
				alarmMessage.append("[Abnormal Reply] DataLogic");
				alarmMessage.append(", ErrorCode:").append(vehicleComm.getVehicleCommData().getReplyErrorCode());
				alarmMessage.append(", Node:").append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.RECEIVED_CMDREPLY_PAUSE:
			{
				alarmMessage.append("[Abnormal Reply] Pause");
				alarmMessage.append(", ErrorCode:").append(vehicleComm.getVehicleCommData().getReplyErrorCode());
				alarmMessage.append(", Node:").append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.ESTOP_BY_NODE_STATION_MISMATCH:
			{
				alarmMessage.append("E-Stop by Vehicle - Node&Station Mismatch. Node:");
				alarmMessage.append(vehicleData.getCurrNode());
				alarmMessage.append(" Station:");
				alarmMessage.append(vehicleData.getCurrStation());
				break;
			}
			case OcsAlarmConstant.ESTOP_BY_CARRIER_TYPE_MISMATCH:
			{
				alarmMessage.append("E-Stop by Vehicle - CarrierType Mismatch. carrierType(VHL):");
				alarmMessage.append(vehicleData.getCarrierType());
				break;
			}
			case OcsAlarmConstant.NO_ALARM:
			{
				break;
			}
			default:
			{
				alarmMessage.append("Undefined Alarm. Node:");
				alarmMessage.append(vehicleData.getCurrNode());
				break;
			}
		}
		if (alarmCode != OcsAlarmConstant.NO_ALARM) {
			traceOperation(alarmMessage.toString());
			registerAlarm(alarmCode, alarmMessage.toString(), alarmLevel);
		}
		vehicleData.setAlarmCode(alarmCode);
		
		// 2015.02.11 by MYM : 장애 지역 우회 기능
		switch (vehicleData.getAlarmCode()) {
			case OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS:
			case OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER:
			case OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER:
			case OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER: {
				vehicleData.setAbnormalSection(DETOUR_REASON.VEHICLE_NOTRESPOND);
				break;
			}
		}
	}
	
	/**
	 * 
	 * @param alarmCode
	 */
	public void registerAlarm(int alarmCode) {
		// 2013.01.07 by MYM : Alarm 텍스트만 등록하는 메소드 추가
		StringBuffer alarmMessage = new StringBuffer();
		switch (alarmCode) {
			case OcsAlarmConstant.CARRIER_REMAINEDON_REMOVEDVHL: {
				if (trCmd != null) {
					alarmMessage.append("Carrier:");
					alarmMessage.append(trCmd.getCarrierId());
				} else {
					alarmMessage.append("Unknown Carrier");
				}
				alarmMessage.append(" remained on Removed VHL at Node:");
				alarmMessage.append(vehicleData.getCurrNode());
				break;
			}
			case OcsAlarmConstant.UNLOAD_CARRIER_MISMATCH: {
				// 2013.01.07 by MYM : STB Unload 후 Carrier Mismatch 발생시 알람 표시
				alarmMessage.append("[Unload_Carrier_Mismatch] Carrier:").append(trCmd.getCarrierId());
				alarmMessage.append(", Port:").append(trCmd.getSourceLoc());
				alarmMessage.append(", TrCmd:").append(trCmd.getTrCmdId());
				break;
			}
			case OcsAlarmConstant.NO_ALARM:
			default: {
				break;
			}
		}
		traceOperation(alarmMessage.toString());
		registerAlarm(alarmCode, alarmMessage.toString(), ALARMLEVEL.ERROR);
	}

	/**
	 * Search Escape Path from AbnormalHid
	 * 
	 * @return
	 */
	public boolean searchEscapeForAbnormalHid() {
		switch (vehicleData.getState()) {
			case 'G':
			case 'A':
			case 'N':
			case 'O':
			case 'I':
			case 'F':
				if (trCmd != null) {
					Node stopNode;
					Node targetNode;
					Hid stopHid;
					Hid targetHid;
					
					stopNode = vehicleData.getDriveStopNode();
					if (stopNode == null) {
						return false;
					}
					stopHid = stopNode.getHid();
					if (stopHid == null) {
						return false;
					}
					targetNode = null;
					targetHid = null;
					String targetNodeId = null;
					switch (trCmd.getDetailState()) {
						case UNLOAD_ASSIGNED:
						case STAGE_ASSIGNED:
						case PATROL_ASSIGNED:
							targetNodeId = trCmd.getSourceNode();
							if (targetNodeId != null && targetNodeId.length() > 0) {
								targetNode = nodeManager.getNode(targetNodeId);
								if (targetNode != null) {
									targetHid = targetNode.getHid();
									if (targetHid != null) {
										if (targetHid.isAbnormalState()) {
											if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
												cancelStageCommand(EVENTHISTORY_REASON.HIDDOWN_AT_SOURCENODE);
											} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
												cancelPatrolCommand(EVENTHISTORY_REASON.HIDDOWN_AT_SOURCENODE);
											} else {
												cancelAssignedTrCmd(EVENTHISTORY_REASON.HIDDOWN_AT_SOURCENODE, true);
											}
										} else {
											if (targetHid != stopHid) {
												return false;
											}
										}
									}
								}
							} else {
								// UnknownTrCmd는 SourceLoc, DestLoc이 null 임.
							}
							break;
						case LOAD_ASSIGNED:
							targetNodeId = trCmd.getDestNode();
							if (targetNodeId != null && targetNodeId.length() > 0) {
								targetNode = nodeManager.getNode(targetNodeId);
								if (targetNode != null) {
									targetHid = targetNode.getHid();
									if (targetHid != null && targetHid != stopHid) {
										return false;
									}
								}
							} else {
								// UnknownTrCmd는 SourceLoc, DestLoc이 null 임.
							}
							break;
						case PATROLLING:
							if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
								if (stopHid.isAbnormalState()) {
									cancelPatrolCommand(EVENTHISTORY_REASON.HIDDOWN_AT_SOURCENODE);
								} else {
									targetNodeId = trCmd.getDestNode();
									if (targetNodeId != null && targetNodeId.length() > 0) {
										targetNode = nodeManager.getNode(targetNodeId);
										if (targetNode != null) {
											targetHid = targetNode.getHid();
											if (targetHid != null && targetHid.isAbnormalState()) {
												cancelPatrolCommand(EVENTHISTORY_REASON.HIDDOWN_AT_DESTNODE);
											}
										}
									} else {
										// UnknownTrCmd는 SourceLoc, DestLoc이 null 임.
									}
								}
							}
							break;
						case VIBRATION_MONITORING:
							targetNodeId = vehicleData.getTargetNode();
							if (targetNodeId != null && targetNodeId.length() > 0) {
								targetNode = nodeManager.getNode(targetNodeId);
								if (targetNode != null) {
									targetHid = targetNode.getHid();
									if (targetHid != null && targetHid != stopHid) {
										return false;
									}
								}
							} else {
								// UnknownTrCmd는 SourceLoc, DestLoc이 null 임.
							}
							break;
						default:
							break;
					}
				}
				if (this.yieldSearch.searchEscapeForAbnormalHid(vehicleData, vehicleCountPerHid, trCmd == null)) {
					// HID Down or HID Capacity Full
					if (vehicleData.getRoutedNodeCount() > 0) {
						sendRouteInfoData();
						// 2013.02.15 by KYK
//						vehicleData.setTargetNode(vehicleData.getDriveTargetNode().getNodeId());
						vehicleData.setTarget(vehicleData.getDriveTargetNode().getNodeId(), "");
						addVehicleToUpdateList();
						traceOperation("Escape Search Success. ToNode:" + vehicleData.getTargetNode());
						changeOperationMode(OPERATION_MODE.GO, "Escape Search For Abnormal HID");
						
						// 2013.04.02 by MYM : Vehicle Locus DB 업데이트
						updateVehicleLocusToDB();
						
						// 2022.08.17 by Y.Won : path search 성공하면 알람 삭제
						if (alarmManager.isAlarmRegistered(vehicleData.getVehicleId()) == true) {
							vehicleData.setHIDEscapePathSearchFailed(false);
							unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH);
						}
						
						return true;
					}
				} else {
					// 2022.08.17 by Y.Won : Vehicle이 station에서 출발하는 경우, next node 가 disable 이더라도 진입하는 문제.
					// searchEscapeForAbnormalHid() 에서 next node = disable 이면 false 를 리턴하도록 함
					// 무언정지 상황을 방지하기 위해 alarm 을 warning 레벨로 등록함
					if (vehicleData.isHIDEscapePathSearchFailed() == true) {
						traceOperation("[Search_Fail] HID Limit Over Escape Search Failed (N:" + vehicleData.getCurrNode() + ")");
						vehicleData.setAlarmCode(OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH);
						registerAlarmWithLevel(vehicleData.getVehicleId(), "[Search-Fail] HID Limit Over Escape Search Failed(N:" + vehicleData.getCurrNode() + ")", ALARMLEVEL.ERROR);
					} else { // HID Capa full 상황 풀리면 알람 종료
						if (alarmManager.isAlarmRegistered(vehicleData.getVehicleId()) == true && vehicleData.isHIDEscapePathSearchFailed() == false) {
							unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_YIELD_PATH);
						}
					}
				}
				return false;
			default:
				return false;
		}
	}

	/**
	 * Search Vehicle Comeback Zone Path
	 * 
	 * @return
	 */
	public boolean searchVehicleComebackZonePath() {
		switch (vehicleData.getState()) {
			case 'A':
			case 'O':
			case 'I':
			case 'F':
				if (isComebackZoneNeeded(vehicleData.getZone(), (vehicleData.getDriveStopNode()).getZone())) {
					if (vehicleData.getLocalGroupId() == null || vehicleData.getLocalGroupId().length() == 0) {
						// LocalOHT는 ComebackZone을 하지 않고, ComebackBay를 하기 때문에 ComebackZone Search를 하지 않음.
						if (this.yieldSearch.searchVehicleComebackZonePath(vehicleData)) {
							sendRouteInfoData();
							// 2013.02.15 by KYK
//							vehicleData.setTargetNode(vehicleData.getDriveTargetNode().getNodeId());
							vehicleData.setTarget(vehicleData.getDriveTargetNode().getNodeId(), "");

							addVehicleToUpdateList();
							traceOperation("ComebackZone Search Success. ToNode:" + vehicleData.getTargetNode());
							changeOperationMode(OPERATION_MODE.GO, "ComebackZone Search.");
							
							// 2013.04.02 by MYM : Vehicle Locus DB 업데이트
							updateVehicleLocusToDB();
							return true;
						} else {
							traceOperation("ComebackZone Search Failed.");
							return false;
						}
					}
				}
				return false;
			default:
				return false;
		}
	}
	
	private boolean isComebackZoneNeeded(String vehicleZone, String nodeZone) {
		if (vehicleZone.equals(nodeZone) == false) {
			if (comebackZoneAllowedSet.contains(vehicleZone + "_" + nodeZone)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Search Local Vehicle Comeback Bay Path
	 * 
	 * @return
	 */
	public boolean searchLocalVehicleComebackBayPath() {
		if (isLocalOHTUsed) {
			// 2012.03.26 by PMM
//			if (vehicleData.getLocalGroupId() != null && vehicleData.getLocalGroupId().length() > 0) {
//				String localGroupBay = localGroupInfoManager.getBay(vehicleData.getLocalGroupId());
			String localGroupId = vehicleData.getLocalGroupId();
			if (localGroupId != null && localGroupId.length() > 0) {
				String localGroupBay = localGroupInfoManager.getBay(localGroupId);
				String stopNodeBay = vehicleData.getDriveStopNode().getBay(); 
				
				// 2012.03.26 by PMM
				// DB에서 강제로 LocalGroupInfo를 삭제하면, Bay가 ""인 Node로 ComebackBaySearch가 일어남.
				if (localGroupBay != null && localGroupBay.length() > 0) {
					
					if (localGroupBay.equals(stopNodeBay) == false) {
						// 2014.02.06 by KYK
//						if (this.yieldSearch.searchLocalVehicleComebackBayPath(vehicleData.getVehicleId(), localGroupBay)) {
						if (this.yieldSearch.searchLocalVehicleComebackBay(vehicleData, localGroupBay)) {
							sendRouteInfoData();
							// 2013.02.15 by KYK
//							vehicleData.setTargetNode(vehicleData.getDriveTargetNode().getNodeId());
							vehicleData.setTarget(vehicleData.getDriveTargetNode().getNodeId(), "");
							addVehicleToUpdateList();
							traceOperation("LocalVehicle ComebackBay Search Success. ToNode:" + vehicleData.getTargetNode());
							changeOperationMode(OPERATION_MODE.GO, "LocalVehicle ComebackBay Search.");
							
							// 2013.04.02 by MYM : Vehicle Locus DB 업데이트
							updateVehicleLocusToDB();
							return true;
						} else {
							traceOperation("LocalVehicle ComebackBay Search Failed.");
							return false;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Reset Vehicle RequestedInfo
	 */
	private void resetVehicleRequestedInfo() {
		if (serviceState == MODULE_STATE.INSERVICE) {
			vehicleManager.resetVehicleRequestedInfoToDB(vehicleData);
		}
	}
	
	/**
	 * Reset TrCmd ChangedInfo
	 */
	private void resetTrCmdChangedInfo() {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (trCmd != null) {
				trCmd.setChangedRemoteCmd(TRCMD_REMOTECMD.NULL);
				trCmd.setChangedTrCmdId("");
				this.trCmdManager.resetChangedInfoFromDB(trCmd);
			}
		}
	}
	
	/**
	* @author : Jongwon Jung
	* @date : 2021. 4. 8.
	* @description : TrcmdTarget 정보 reset
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 4. 8. Jongwon 최초 생성 */
	private void resetTrCmdTargetInfo() {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (trCmd != null) {
				this.trCmdManager.updateTrCmdRecoveryChangedInfoToDB(trCmd.getTrCmdId(), trCmd);
				trCmd.setChangedRemoteCmd(TRCMD_REMOTECMD.NULL);
				trCmd.setChangedTrCmdId("");
				if(trCmd.getOldDestLoc() != null){
					trCmd.setDestLoc(trCmd.getOldDestLoc());
					trCmd.setDestNode(trCmd.getOldDestNode());
				}
				if(trCmd.getOldCarrierId() != null){
					trCmd.setCarrierId(trCmd.getOldCarrierId());
				}
				if(trCmd.getPriority() > 0){
					trCmd.setPriority(trCmd.getOldPriority());
				}
				this.trCmdManager.resetChangedTargetInfoFromDB(trCmd);
			}
		}
	}

	/**
	 * Set Vehicle UserDefinedRoute
	 */
	private void setVehicleUserDefinedRoute() {
//		if (ocsInfoManager.isUserPassThroughUsed()) {
		if (isUserPassThroughUsed) {
			String sourceLocId = trCmd.getSourceLoc();
			String destLocId  = trCmd.getDestLoc();
			
			// 2011.11.08 by PMM
//			if (sourceLocId != null && sourceLocId.length() != 0 && destLocId != null || destLocId.length() != 0) {
			if (sourceLocId != null && sourceLocId.length() != 0 && destLocId != null && destLocId.length() != 0) {
				CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(sourceLocId);
				CarrierLoc destLoc = carrierLocManager.getCarrierLocData(destLocId);
				if (sourceLoc == null || destLoc == null) {
					return;
				}
				trCmd.setUserDefinedPath(userDefinedPathManager.getUserDefinedPath(sourceLoc.getUserGroupId(), destLoc.getUserGroupId()));
			}
		}
	}
	
	/**
	 * Is CommFail?
	 * 
	 * @return
	 */
	private boolean isCommFail() {
//		return vehicleComm.isCommFail(ocsInfoManager.getCommFailCheckTime() * 1000);
		// 2014.06.21 by MYM : [Commfail 체크 개선] commFailCheckTime 주기적으로 업데이트 하도록 변경 
//		return vehicleComm.isCommFail(commFailCheckTime * 1000);
		return vehicleComm.isCommFail();
	}
	
	/**
	 * Is NearByDrive?
	 * 
	 * @return
	 */
	public boolean isNearByDrive() {
		return isNearByDrive;
	}
	
	/**
	 * Is STBCUsed?
	 * 
	 * @return
	 */
	public boolean isSTBCUsed() {
		return isSTBCUsed;
	}
	
	public boolean isUnloadErrorReportUsed() {
		return isUnloadErrorReportUsed;
	}
	
	/**
	 * Is AutoMismatchRecoveryMode?
	 * 
	 * @return
	 */
	public boolean isAutoMismatchRecoveryMode() {
		return isAutoMismatchRecoveryMode;
	}
	
	public boolean isGoModeCarrierStatusCheckUsed() {
		return isGoModeCarrierStatusCheckUsed;
	}

	/**
	 * Is AlarmRegistered?
	 * 
	 * @return
	 */
	public boolean isAlarmRegistered() {
		// 2012.04.09 by PMM
		// Alarm 등록 여부를 Memory (vehicleData) 기준으로 단순화 함.
		// DB 동기화 시, 사용자에 의한 Alarm 강제 제거 시, Memory 값이 정리안되는 문제 발생함.
//		if (alarmManager.isAlarmRegistered(vehicleData.getVehicleId())) {
//			vehicleData.setAlarmCode(alarmManager.getRegisteredAlarmCode(vehicleData.getVehicleId()));
//			return true;
//		}
		if (vehicleData.getAlarmCode() > 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * Is Valid Node Updated?
	 * 
	 * @return
	 */
	public boolean isValidNodeUpdated() {
		return isValidNodeUpdated;
	}

	/**
	 * Register Alarm
	 * 
	 * @param alarmCode
	 * @param alarmText
	 */
	public void registerAlarm(int alarmCode, String alarmText, ALARMLEVEL alarmLevel) {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.registerAlarm(vehicleData.getVehicleId(), alarmCode, alarmText, alarmLevel);
		}
	}

	/**
	 * Register Alarm with Level
	 * 
	 * @param type 
	 * @param alarmText
	 * @param alarmLevel
	 */
	public void registerAlarmWithLevel(String type, String alarmText, ALARMLEVEL alarmLevel) {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.registerAlarmTextWithLevel(type, alarmText, alarmLevel.toConstString());
		}
	}

	/**
	 * Unregister All Alarms 
	 */
	public void unregisterAllAlarm() {
		if (alarmManager.unregisterAllAlarm(vehicleData.getVehicleId()) == false) {
			traceOperationException("Operation Abnormal #010 - Failed to unregister all alarm.");
		}
		vehicleData.setAlarmCode(OcsAlarmConstant.NO_ALARM);
	}
	
	/**
	 * Unregister the Alarm with the AlarmCode on the Vehicle
	 */
	public void unregisterAlarm(int alarmCode) {
		if (alarmManager.unregisterAlarm(vehicleData.getVehicleId(), alarmCode)) {
			vehicleData.setAlarmCode(OcsAlarmConstant.NO_ALARM);
		} else {
//			traceOperationException("Operation Abnormal #011 - Failed to unregister an alarm:" + alarmCode);
			unregisterAllAlarm();
		}
		
		// 2015.02.11 by MYM : 장애 지역 우회 기능
		if (vehicleData.getAbnormalReason() == DETOUR_REASON.VEHICLE_NOTRESPOND) {
			switch (alarmCode) {
				case OcsAlarmConstant.NOT_SENDING_GOCOMMAND_TIMEOVER_BY_OCS:
				case OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER:
				case OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER:
				case OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER: {
					vehicleData.releaseAbnormalSection();
					break;
				}
			}
		}
	}

	/**
	 * Register EventHistory
	 * 
	 * @param eventHistory
	 * @param duplicateCheck
	 */
	public void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			eventHistoryManager.addEventHistoryToRegisterList(eventHistory, duplicateCheck);
			if (isFormattedLogUsed) {
				traceFormattedEventHistory(eventHistory);
			}
		}
	}

	/**
	 * Regiser VehicleErrorHistory
	 * 
	 * @param vehicleErrorHistory
	 */
	public void registerVehicleErrorHistory(int alarmCode, String alarmText, String type) {
		if (requestedServiceState == MODULE_STATE.INSERVICE) {
			VehicleErrorHistory vehicleErrorHistory;
			if (trCmd == null) {
				vehicleErrorHistory = new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(),
						alarmCode, alarmText, type,
						"", "", "",
						"", "", "",
						getCurrDBTimeStr(), "", "");
			} else {
				vehicleErrorHistory = new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(),
						alarmCode, alarmText, type,
						trCmd.getTrCmdId(), trCmd.getDetailState().toConstString(), trCmd.getCarrierId(),
						trCmd.getCarrierLoc(), trCmd.getSourceLoc(), trCmd.getDestLoc(),
						getCurrDBTimeStr(), "", "");
			}
			if (vehicleErrorHistory.getCarrierLoc() != null && vehicleErrorHistory.getCarrierLoc().length() == 0) {
				vehicleErrorHistory.setCarrierLoc(NO_CARRIERLOC);
			}
			if (vehicleErrorHistory.getAlarmCode() == 0 &&
					vehicleErrorHistory.getAlarmText() != null && vehicleErrorHistory.getAlarmText().length() == 0) {
				vehicleErrorHistory.setAlarmText(NO_ERROR);
			}
			vehicleErrorHistoryManager.addVehicleToRegisterList(vehicleErrorHistory);
			traceVehicleErrorHistory(vehicleErrorHistory, true);
			if (isFormattedLogUsed) {
				traceFormattedVehicleErrorHistory(vehicleErrorHistory);
			}
		}
	}
	/**
	 * Reset from VehicleErrorHistory
	 * 
	 * @param vehicleErrorHistory
	 */
	public void resetFromVehicleErrorHistory() {
		VehicleErrorHistory vehicleErrorHistory;
		if (trCmd == null) {
			vehicleErrorHistory = new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(),
					0, "", "",
					"", "", "",
					"", "", "",
					"", getCurrDBTimeStr(), "");
		} else {
			vehicleErrorHistory = new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(),
					0, "", "",
					trCmd.getTrCmdId(), trCmd.getDetailState().toConstString(), trCmd.getCarrierId(),
					trCmd.getCarrierLoc(), trCmd.getSourceLoc(), trCmd.getDestLoc(),
					"", getCurrDBTimeStr(), "");
		}
		
		if (vehicleErrorHistory.getCarrierLoc() != null && vehicleErrorHistory.getCarrierLoc().length() == 0) {
			vehicleErrorHistory.setCarrierLoc(NO_CARRIERLOC);
		}
		if (vehicleErrorHistory.getAlarmCode() == 0 &&
				vehicleErrorHistory.getAlarmText() != null && vehicleErrorHistory.getAlarmText().length() == 0) {
			vehicleErrorHistory.setAlarmText(NO_ERROR);
		}
		
		vehicleErrorHistoryManager.addVehicleToResetErrorList(vehicleErrorHistory);
		
		// 2011.12.05 by PMM
		traceVehicleErrorHistory(vehicleErrorHistory, false);
		
		// 2011.12.29 by PMM
//		if (ocsInfoManager.isFormattedLogUsed()) {
		if (isFormattedLogUsed) {
			traceFormattedVehicleErrorHistory(vehicleErrorHistory);
		}
	}

	// 2011.10.28 by PMM
	// RuntimeUpdate 후, VehicleInitialize.
	/**
	 * Set OperationInit
	 */
	public void setOperationInitForRuntimeUpdate() {
		
		VehicleCommData commData = vehicleComm.getVehicleCommData();
		commData.setReceivedReply(false);
		commData.setReceivedState(false);
		// 2013.10.23 by KYK
		initializeCommandState();
		vehicleData.initializeDriveVehicleInNode();
		initializeVehiclePath(commData, "RuntimeUpdate");
		if (vehicleData.getCurrCmd() == 0 || vehicleData.getVehicleMode() == 'M') {
			initializeControlReadyState();
		}

		StringBuffer log = new StringBuffer();
		log.append("RuntimeUpdate/").append(operationControlState).append("/");
		if (trCmd != null) {
			log.append(trCmd.getTrCmdId()).append("/").append(trCmd.getRemoteCmd()).append("/");
			log.append(trCmd.getState()).append("/").append(trCmd.getDetailState()).append("/");
			log.append(trCmd.getCarrierId()).append("/").append(trCmd.getSourceLoc()).append("/").append(trCmd.getDestLoc()).append("/").append(trCmd.getCarrierLoc()).append("/");
			log.append(trCmd.getSourceNode()).append("/").append(trCmd.getDestNode()).append("/");
			log.append(trCmd.getReplace()).append("/").append(trCmd.getPriority()).append("/");
			log.append(trCmd.isPause()).append("/").append(trCmd.getPauseType()).append("/").append(trCmd.getPauseCount()).append("/").append(trCmd.getPausedTime()).append("/");
		} else {
			log.append("NoTrCmd/");
		}
		log.append(vehicleData.getVehicleMode()).append("/").append(vehicleData.getState()).append("/").append(vehicleData.getCarrierExist()).append("/");
		log.append(vehicleData.getCurrNode()).append("/").append(vehicleData.getStopNode()).append("/").append(vehicleData.getTargetNode()).append("/");
		log.append(vehicleData.getErrorCode());
		traceOperation(log.toString());
		traceOperation("RuntimeUpdateInit Completed.");
	}

	/**
	 * Set All OperationReady
	 * 
	 * @param isAllOperationReady
	 */
	public void setAllOperationReady(boolean isAllOperationReady) {
		this.isAllOperationReady = isAllOperationReady;
	}
	
	/**
	 * Set Failover Completed
	 * 
	 * @param isFailoverCompleted
	 */
	public void setFailoverCompleted(boolean isFailoverCompleted) {
		this.isFailoverCompleted = isFailoverCompleted;
	}
	
	// 2011.10.28 by PMM
	// SystemPause 추가 (RuntimeUpdate)
	/**
	 * Is SystemPaused?
	 */
	public boolean isSystemPaused() {
		return isSystemPaused;
	}
	
	/**
	 * Set SystemPaused
	 * 
	 * @param isSystemPaused
	 */
	public void setSystemPaused(boolean isSystemPaused) {
		if (this.isSystemPaused != isSystemPaused) {
			if (isSystemPaused) {
				traceOperation("System Pause Requested.");
			} else {
				traceOperation("System Pause Resumed.");
			}
			this.isSystemPaused = isSystemPaused;
		}
	}

	/**
	 * Is OperationReady?
	 * 
	 * @return
	 */
	public boolean isOperationReady() {
		if (operationControlState == OPERAION_CONTROL_STATE.INIT) {
			return false;
		}
		return true;
	}
	
	/**
	 * Is OperationStarted?
	 * 
	 * @return
	 */
	public boolean isOperationStarted() {
		if (operationControlState == OPERAION_CONTROL_STATE.START) {
			return true;
		}
		return false;
	}
	
	/**
	 * Is Failover Completed?
	 * 
	 * @return
	 */
	public boolean isFailoverCompleted() {
		return isFailoverCompleted;
	}
	
	// 2011.10.26 by PMM
	// OperationModeImpl에서 여기로 옮겨옴.
	/**
	 * Is LoadingByPass?
	 */
	public boolean isLoadingByPass() {
		if (vehicleData.isLoadingByPass()) {
			return true;
		} else if (trCmd != null && trCmd.isLoadingByPass()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Update Vehicle ActionHold
	 * 
	 * @param actionHold
	 */
	public void updateVehicleActionHold(boolean actionHold) {
		vehicleData.setActionHold(actionHold);
		addVehicleToUpdateList();
	}
	
	private static Logger operationTraceLog = Logger.getLogger(OPERATION_TRACE);
	/**
	 * Trace OperationDebug
	 * 
	 * @param message
	 */
	public void traceOperation(String message) {
		operationTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
	}

	private static Logger operationDelayTraceLog = Logger.getLogger(OPERATION_DELAY_TRACE);
	/**
	 * Trace OperationDelay
	 * 
	 * @param message
	 */
	public void traceOperationDelay(String message) {
		operationDelayTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
	}
	
	private static Logger operationExceptionTraceLog = Logger.getLogger(OPERATION_EXCEPTION_TRACE);
	/**
	 * Trace OperationException
	 * 
	 * @param message
	 * @param e
	 */
	public void traceOperationException(String message, Throwable e) {
		operationExceptionTraceLog.error(String.format("%s> [%s] ", vehicleData.getVehicleId(), message), e);
		operationExceptionTraceLog.error(String.format("%s>      %s", vehicleData.getVehicleId(), getJournalOfVehicle()));
		operationExceptionTraceLog.error(String.format("%s>      %s", vehicleData.getVehicleId(), getJournalOfTrCmd()));
	}
	
	/**
	 * Trace OperationException
	 * 
	 * @param message
	 */
	public void traceOperationException(String message) {
		operationExceptionTraceLog.error(String.format("%s> %s", vehicleData.getVehicleId(), message));
		operationExceptionTraceLog.error(String.format("%s>      %s", vehicleData.getVehicleId(), getJournalOfVehicle()));
		operationExceptionTraceLog.error(String.format("%s>      %s", vehicleData.getVehicleId(), getJournalOfTrCmd()));
	}

	private static Logger hostReportTraceLog = Logger.getLogger(HOSTREPORT_TRACE);
	/**
	 * Trace HostReport
	 * 
	 * @param message
	 */
	public void traceHostReport(String message) {
		hostReportTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
	}

	private static Logger updateRequestedCmdTraceLog = Logger.getLogger(UPDATE_REQUESTEDCMD_TRACE);
	/**
	 * Trace Update RequestedCmd
	 * 
	 * @param message
	 */
	public void traceUpdateRequestedCmd(String message) {
		updateRequestedCmdTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
	}

	private static Logger processTrCmdTraceLog = Logger.getLogger(PROCESS_TRCMD_TRACE);
	/**
	 * Trace ProcessTrCmd
	 * 
	 * @param message
	 */
	public void traceProcessTrCmd(String message) {
		processTrCmdTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
	}

	private static Logger stbTraceLog = Logger.getLogger(STB_TRACE);
	/**
	 * Trace STB
	 * 
	 * @param message
	 */
	public void traceSTB(String message) {
		stbTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
	}
	
	private static Logger vehicleTrafficTraceLog = Logger.getLogger(VEHICLE_TRAFFIC_TRACE);
	/**
	 * Trace VehicleTraffic
	 * 
	 * @param message
	 */
	public void traceVehicleTraffic(String message) {
		vehicleTrafficTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
	}

	private static Logger rfReadErrorTraceLog = Logger.getLogger(RFREAD_ERROR_TRACE);
	/**
	 * Trace RFReadError
	 * 
	 * @param message
	 */
	public void traceRFReadError(String message) {
		rfReadErrorTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
	}
	
	private static Logger vehicleErrorHistoryTraceLog = Logger.getLogger(VEHICLEERRORHISTORY_TRACE);
	/**
	 * Trace VehicleErrorHistory
	 * 
	 * @param message
	 */
	public void traceVehicleErrorHistory(VehicleErrorHistory vehicleErrorHistory, boolean isAlarmSet) {
		StringBuffer message = new StringBuffer();
		message.append(vehicleErrorHistory.getVehicle()).append(" ");
		message.append(vehicleErrorHistory.getNode()).append(" ");
		message.append(vehicleErrorHistory.getCarrierLoc()).append(" ");
		message.append(vehicleErrorHistory.getAlarmCode()).append(" ");
		message.append(vehicleErrorHistory.getAlarmText()).append(" ");
		if (isAlarmSet) {
			message.append(ALARM_SET);
		} else {
			message.append(ALARM_RESET);
		}
		vehicleErrorHistoryTraceLog.debug(message.toString());
	}
	
	/**
	 * Get Journal of Vehicle
	 * 
	 * @return
	 */
	private String getJournalOfVehicle() {
		StringBuffer journal = new StringBuffer();
		journal.append("Vehicle: ");
		journal.append("[").append(activeOperationMode.getOperationMode().toConstString()).append("]");
		journal.append(" Mode:").append(vehicleData.getVehicleMode());
		journal.append(", State:").append(vehicleData.getState());
		journal.append(", Node(").append(vehicleData.getCurrNode()).append(",").append(vehicleData.getStopNode()).append(",").append(vehicleData.getTargetNode()).append(")");
		journal.append(", Carrier:").append(vehicleData.getCarrierExist());
		journal.append(", CmdState:").append(cmdState).append("(P:").append(vehicleData.getPrevCmd()).append(" C:").append(vehicleData.getCurrCmd()).append(" N:").append(vehicleData.getNextCmd()).append(" V:").append(vehicleCommCommand.getCommandId()).append(")");
		journal.append(", Error:").append(vehicleData.getErrorCode());
		journal.append(", RF:").append(vehicleData.getRfData());
		
		if (vehicleData.getLocalGroupId() != null && vehicleData.getLocalGroupId().length() > 0) {
			journal.append(", LocalGroup:").append(vehicleData.getLocalGroupId());
		}
		journal.append(", PauseType:").append(vehicleData.getPauseType());
		if (vehicleData.getAlarmCode() > 0) {
			journal.append(", AlarmCode:").append(vehicleData.getAlarmCode());
		}
		return journal.toString();
	}
	
	/**
	 * Get Journal of TrCmd
	 * 
	 * @return
	 */
	private String getJournalOfTrCmd() {
		if (trCmd != null) {
			StringBuffer journal = new StringBuffer();
			journal.append("TrCmd:").append(trCmd.getTrCmdId()).append("/").append(trCmd.getRemoteCmd()).append("/");
			journal.append(trCmd.getState()).append("/").append(trCmd.getDetailState()).append("/");
			journal.append(trCmd.getCarrierId()).append("/").append(trCmd.getSourceLoc()).append("/").append(trCmd.getDestLoc()).append("/").append(trCmd.getCarrierLoc()).append("/");
			journal.append(trCmd.getSourceNode()).append("/").append(trCmd.getDestNode()).append("/");
			journal.append(trCmd.getVehicle()).append("/").append(trCmd.getAssignedVehicleId()).append("/");
			journal.append(trCmd.getReplace()).append("/").append(trCmd.getPriority()).append("/");
			journal.append(trCmd.isPause()).append("/").append(trCmd.getPauseType()).append("/").append(trCmd.getPauseCount()).append("/").append(trCmd.getPausedTime());
			if (trCmd.getChangedRemoteCmd() != TRCMD_REMOTECMD.NULL) {
				journal.append(trCmd.getChangedRemoteCmd()).append("/").append(trCmd.getChangedTrCmdId());
			}
			journal.append(".");
			return journal.toString();
		} else {
			return NO_TRCMD;
		}
	}
	
	/**
	 * Get Time from String
	 * 
	 * @param time
	 * @return
	 */
	private long getTimeFromString(String time) {
		try {
			return sdf.parse(time).getTime();
		} catch (Exception e) {
			traceOperationException("getTimeFromString()", e);
			return System.currentTimeMillis();
		}
	}
	
	/**
	 * Get Waiting Time
	 * 
	 * @param time
	 * @return
	 */
	private long getWaitingTime(String time) {
		try {
			return (long) ((getTimeFromString(ocsInfoManager.getCurrDBTimeStr()) - getTimeFromString(time)) / 1000);
		} catch (Exception e) {
			traceOperationException("getTimeFromString()", e);
			return System.currentTimeMillis();
		}
	}
	
	/**
	 * Get WaitingTime in Millis
	 */
	private long getWaitingTimeMillis(String time) {
		try {
			return (long) (getTimeFromString(ocsInfoManager.getCurrDBTimeStr()) - getTimeFromString(time));
		} catch (Exception e) {
			traceOperationException("getWaitingTimeMillis()", e);
			return System.currentTimeMillis();
		}
	}
	
	private static Logger trCompletionHistoryTraceFormatLog = Logger.getLogger(FORMAT_TRCOMPLETIONHISTORY_TRACE);
	/**
	 * Trace TrCompletionHistoryLog
	 * 
	 * @param message
	 */
	public void traceFormattedTrCompletionHistory(TrCompletionHistory trCompletionHistory) {
		StringBuffer message = new StringBuffer();
		message.append("[\"").append(sdf2.format(new Date())).append("\",");
		message.append("\"").append(trCompletionHistory.getTrCmdId()).append("\",");
		message.append("\"").append(trCompletionHistory.getPriority()).append("\",");
		message.append("\"").append(trCompletionHistory.getCarrierId()).append("\",");
		message.append("\"").append(trCompletionHistory.getSourceLoc()).append("\",");
		message.append("\"").append(trCompletionHistory.getDestLoc()).append("\",");
		message.append("\"").append(trCompletionHistory.getVehicle()).append("\",");
		message.append("\"").append(trCompletionHistory.getTrQueuedTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getUnloadAssignedTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getUnloadingTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getUnloadedTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getLoadAssignedTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getLoadingTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getLoadedTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getDeletedTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getSourceNode()).append("\",");
		message.append("\"").append(trCompletionHistory.getDestNode()).append("\",");
		message.append("\"").append(trCompletionHistory.getRemoteCmd()).append("\",");
		message.append("\"").append(trCompletionHistory.getUnloadAutoRetryCount()).append("\",");
		message.append("\"").append(trCompletionHistory.getUnloadBT()).append("\",");
		message.append("\"").append(trCompletionHistory.getLoadAutoRetryCount()).append("\",");
		message.append("\"").append(trCompletionHistory.getLoadBT()).append("\",");
		message.append("\"").append(trCompletionHistory.getNoBlockingTime()).append("\",");
		message.append("\"").append(trCompletionHistory.getWaitTimeout()).append("\",");
		message.append("\"").append(trCompletionHistory.getExpectedDuration()).append("\",");
		message.append("\"").append(trCompletionHistory.getDestChangedTrCmdId()).append("\",");
		if (trCompletionHistory.isOcsRegistered()) {
			message.append("\"TRUE\",");
		} else {
			message.append("\"FALSE\",");
		}
		message.append("\"").append(trCompletionHistory.getVehicleLocus()).append("\",");
		message.append("\"").append(trCompletionHistory.getFoupId()).append("\"]");
		
		message.insert(0, getMessageSizeInfoForLogServer(message.toString()));
		trCompletionHistoryTraceFormatLog.debug(message.toString());
	}
	
	private static Logger vehicleErrorHistoryTraceFormatLog = Logger.getLogger(FORMAT_VEHICLEERRORHISTORY_TRACE);
	/**
	 * Trace VehicleErrorHistoryLog
	 * 
	 * @param message
	 */
	public void traceFormattedVehicleErrorHistory(VehicleErrorHistory vehicleErrorHistory) {
		StringBuffer message = new StringBuffer();
		message.append("[\"").append(sdf2.format(new Date())).append("\",");
		message.append("\"").append(vehicleErrorHistory.getVehicle()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getNode()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getCarrierLoc()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getAlarmCode()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getAlarmText()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getSetTime()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getClearTime()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getType()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getTrStatus()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getTrCmdId()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getCarrierId()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getSourceLoc()).append("\",");
		message.append("\"").append(vehicleErrorHistory.getDestLoc()).append("\"]");
		
		message.insert(0, getMessageSizeInfoForLogServer(message.toString()));
		vehicleErrorHistoryTraceFormatLog.debug(message.toString());
	}
	
	private static Logger eventHistoryTraceFormatLog = Logger.getLogger(FORMAT_EVENTHISTORY_TRACE);
	/**
	 * Trace EventHistoryLog
	 * 
	 * @param message
	 */
	public void traceFormattedEventHistory(EventHistory eventHistory) {
		StringBuffer message = new StringBuffer();
		message.append("[\"").append(sdf2.format(new Date())).append("\",");
		message.append("\"").append(eventHistory.getName().toConstString()).append("\",");
		message.append("\"").append(eventHistory.getType().toConstString()).append("\",");
		message.append("\"").append(eventHistory.getSubType()).append("\",");
		message.append("\"").append(eventHistory.getEvent()).append("\",");
		message.append("\"").append(eventHistory.getSetTime()).append("\",");
		message.append("\"").append(eventHistory.getClearTime()).append("\",");
		message.append("\"").append(eventHistory.getRemoteId().toConstString()).append("\",");
		message.append("\"").append(eventHistory.getRemoteIp()).append("\",");
		message.append("\"").append(eventHistory.getReason().toConstString()).append("\"]");
		
		message.insert(0, getMessageSizeInfoForLogServer(message.toString()));
		eventHistoryTraceFormatLog.debug(message.toString());
	}
	
	private static Logger stbReportDataTraceLog = Logger.getLogger(STB_REPORT_DATA_TRACE);
	/**
	 * Trace traceSTBReportData
	 * 
	 * @param message
	 */
	public void traceSTBReportData(String eventName, TrCmd trCmd) {
		StringBuffer message = new StringBuffer();
		if (eventName.equals(OperationConstant.CARRIER_INSTALLED)) {
			message.append(trCmd.getUnloadedTime()).append(",");
			message.append("UNLOAD,");  // EQ 관점에서 UNLOAD
			message.append(trCmd.getVehicle()).append(",");
			message.append(trCmd.getSourceLoc()).append(",");
			message.append(trCmd.getCarrierId());
		} else if (eventName.equals(OperationConstant.CARRIER_REMOVED)) {
			message.append(trCmd.getLoadedTime()).append(",");
			message.append("LOAD,");  // EQ 관점에서 LOAD
			message.append(trCmd.getVehicle()).append(",");
			message.append(trCmd.getDestLoc()).append(",");
			message.append(trCmd.getCarrierId());
		}
		stbReportDataTraceLog.debug(String.format("%s", message));
	}
	
	private static final String FOUR_ZEROS = "0000";
	private String getMessageSizeInfoForLogServer(String message) {
		if (message != null) {
			String size = Integer.toString(message.length(), 36);
			return FOUR_ZEROS.substring(0, 4 - size.length()) + size;
		} else {
			return FOUR_ZEROS;
		}
	}
	
//	2013.05.10 by MYM : CarrierLoc의 MultiPort를 PortOption으로 흡수함.
//	/**
//	 * 2012.06.20 by KYK [MultiPort]
//	 * @param port
//	 * @return
//	 */
//	public boolean isMultiPort(String port) {
//		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(port);
//		if (carrierLoc != null) {
//			return carrierLoc.isMultiPort();
//		}
//		return false;
//	}

	/**
	 * 2013.02.08 by KYK
	 * @param carrierLocId
	 * @return
	 */
	public String getStationIdAtPort(String carrierLocId) {
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(carrierLocId);
		String stationId = null;
		if (carrierLoc != null) {
			stationId = carrierLoc.getStationId();
		}
		return stationId;
	}
	
	public void setAbnormalStateChanged(boolean isAbnormalStateChanged) {
		this.isAbnormalStateChanged = isAbnormalStateChanged;
	}
	
	private void resetForRerouting() {
		vehicleData.resetRedirectedNodeSet();
	}
	
	public void disableUserRequest() {
		if (trCmd.getPatrolId() != null && trCmd.getPatrolId().length() > 0) {
			boolean result = userRequestManager.disableUserRequest(trCmd.getPatrolId());
			if (result) {
				traceOperation("LongRun(" + trCmd.getPatrolId() + ") Disabled");

				StringBuilder alarmMessage = new StringBuilder();
				alarmMessage.append("[LongRun_Disabled] Patrol ID:").append(trCmd.getPatrolId());
				alarmMessage.append(", Vehicle:").append(vehicleData.getVehicleId());
				alarmMessage.append(", TrCmd:").append(trCmd.getTrCmdId());
				alarmMessage.append(", Source Node:").append(trCmd.getSourceNode());
				alarmMessage.append(", Dest Node:").append(trCmd.getDestNode());
				registerAlarmWithLevel("PatrolVHL", alarmMessage.toString(), ALARMLEVEL.WARNING);
			}

			vehicleData.setRepathSearchNeededByPatrolVHL(true);
		}
	}

}