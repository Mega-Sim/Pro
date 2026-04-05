package com.samsung.ocs.operation;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.config.CarrierTypeConfig;
import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.DEADLOCK_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.OCS_CONTROL_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.RUNTIME_UPDATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.TSC_STATE;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.common.message.Message;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.AutoRetryGroupInfoManager;
import com.samsung.ocs.manager.impl.BlockManager;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.CloseLoopManager;
import com.samsung.ocs.manager.impl.CollisionManager;
import com.samsung.ocs.manager.impl.DetourControlManager;
import com.samsung.ocs.manager.impl.EventHistoryManager;
import com.samsung.ocs.manager.impl.HIDManager;
import com.samsung.ocs.manager.impl.IBSEMReportManager;
import com.samsung.ocs.manager.impl.LocalGroupInfoManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.RailDownControlManager;
import com.samsung.ocs.manager.impl.STBCarrierLocManager;
import com.samsung.ocs.manager.impl.SectionManager;
import com.samsung.ocs.manager.impl.StationManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.TrCompletionHistoryManager;
import com.samsung.ocs.manager.impl.TrafficManager;
import com.samsung.ocs.manager.impl.UserDefinedPathManager;
import com.samsung.ocs.manager.impl.UserOperationManager;
import com.samsung.ocs.manager.impl.UserRequestManager;
import com.samsung.ocs.manager.impl.VehicleErrorHistoryManager;
import com.samsung.ocs.manager.impl.VehicleErrorManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.ZoneControlManager;
import com.samsung.ocs.manager.impl.model.Alarm;
import com.samsung.ocs.manager.impl.model.AutoRetryGroupInfo;
import com.samsung.ocs.manager.impl.model.Block;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.CloseLoop;
import com.samsung.ocs.manager.impl.model.Collision;
import com.samsung.ocs.manager.impl.model.DetourControl;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.IBSEMReport;
import com.samsung.ocs.manager.impl.model.Link;
import com.samsung.ocs.manager.impl.model.LocalGroupInfo;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.OCSInfo;
import com.samsung.ocs.manager.impl.model.RailDownControl;
import com.samsung.ocs.manager.impl.model.STBCarrierLoc;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.TrCompletionHistory;
import com.samsung.ocs.manager.impl.model.Traffic;
import com.samsung.ocs.manager.impl.model.UserDefinedPath;
import com.samsung.ocs.manager.impl.model.UserOperation;
import com.samsung.ocs.manager.impl.model.UserRequest;
import com.samsung.ocs.manager.impl.model.VehicleData;
import com.samsung.ocs.manager.impl.model.VehicleError;
import com.samsung.ocs.manager.impl.model.VehicleErrorHistory;
import com.samsung.ocs.manager.impl.model.ZoneControl;
import com.samsung.ocs.operation.comm.VehicleComm;
import com.samsung.ocs.operation.constant.MessageItem;
import com.samsung.ocs.operation.constant.MessageItem.EVENT_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant;
import com.samsung.ocs.operation.constant.ResultCode;
import com.samsung.ocs.operation.model.VehicleCommData;

/**
 * OperationManager Class, OCS 3.0 for Unified FAB
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

public class OperationManager extends AbstractOcsThread {
	private MODULE_STATE requestedServiceState = MODULE_STATE.REQOUTOFSERVICE;
	private MODULE_STATE serviceState = MODULE_STATE.OUTOFSERVICE;

	private ConcurrentHashMap<String, Operation> managerMap;
	private OCSInfoManager ocsInfoManager;
	private HIDManager hidManager;
	private NodeManager nodeManager;
	private SectionManager sectionManager;
	private CollisionManager collisionManager;
	private CloseLoopManager closeLoopManager;
	private CarrierLocManager carrierLocManager;
	private STBCarrierLocManager stbCarrierLocManager;
	private BlockManager blockInfoManager;
	private TrCmdManager trCmdManager;
	private VehicleManager vehicleManager;
	private ZoneControlManager zoneControlManager;
	private VehicleErrorManager vehicleErrorManager;
	private IBSEMReportManager ibsemReportManager;
	private TrCompletionHistoryManager trCompletionHistoryManager;
	private AlarmManager alarmManager;
	private EventHistoryManager eventHistoryManager;
	private DetourControlManager detourControlManager;
	// 2013.02.15 by KYK
	private StationManager stationManager;
//	private PassDoorManager passDoorManager; // 2015.02.09 by zzang9un : PassDoorManager 추가
	private TrafficManager trafficManager;
	
	// 2014.11.13 by zzang9un : ThreadMonitorManager 추가
	private ThreadMonitorManager threadMonitoringManager;
	
	// Setup Parameters
	private boolean isIBSEMUsed = true;
	
	// 2012.01.10 by PMM
	private boolean isNearByDrive = true;
	
	private boolean isSystemPauseRequested;
	private boolean isAllVehicleStop;
	private boolean isAllOperationReady;
	private boolean isFailoverCompleted;
	
	// 2011.10.12 by PMM OCSINFO update 지연으로  TSCAutoCompleted 중복 보고 방지
	private boolean isTSCPauseInitiatedReported;
	private boolean isTSCAutoCompletedReported;
	private boolean isTSCPauseCompletedReported;
	private boolean isAbnormalStateChanged = false;
	
	private long startTime;
	private static final long START_TIMEOUT = 30000;

	private long systemPauseRequestedTime;

	private int vehicleSocketPort = 5001;
	
	private int outOfServiceCheckCount = 0;
	
	private static final String OPERATIONTIME = "OPERATIONTIME";
	private static final String OCSCONTROL = "OCSCONTROL";
	private static final String TSCSTATUS = "TSCSTATUS";
	
	// 2011.11.04 by PMM
	private static final String OPERATION = "Operation";
	private static final String RUNTIMEUPDATE_START_INSERVICE = "Runtime Layout Update Started! - INSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_INSERVICE = "Runtime Layout Update Completed! - INSERVICE";
	// 2017.10.24 by LSH : Port ID 변경용 Runtime Update 기능
	private static final String RUNTIMEPORTUPDATE_START_INSERVICE = "Runtime Port Update Started! - INSERVICE";
	private static final String RUNTIMEPORTUPDATE_COMPLETED_INSERVICE = "Runtime Port Update Completed! - INSERVICE";
	
	private static final String ALL_VEHICLE_INITIATED = "All Vehicles Initiated!";
	private static final String SYSTEM_PAUSE_REQUESTED = "System Pause Requested!";
	private static final String SYSTEM_PAUSE_REQUEST_TIMEOUT = "System Pause Request Timeout!";
	private static final String SYSTEM_PAUSED = "System Paused!";
	private static final String SYSTEM_PAUSE_RESUMED = "System Pause Resumed!";
	private static final String RUNTIMEUPDATE_START_OUTOFSERVICE = "Runtime Layout Update Started! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE = "Runtime Layout Update Completed! - OUTOFSERVICE";
	private static final String RUNTIMEUPDATE_CANCELED = "Runtime Layout Update Canceled!";
	private static final String RUNTIMEUPDATE_FAILED = "Runtime Layout Update Failed!";
	// 2017.10.24 by LSH : Port ID 변경용 Runtime Update 기능
	private static final String RUNTIMEPORTUPDATE_CANCELED = "Runtime Port Update Canceled!";
	private static final String RUNTIMEPORTUPDATE_FAILED = "Runtime Port Update Failed!";
	private static final String INITIALIZING_HIDMANAGER = "Initializing HIDManager ...";
	private static final String INITIALIZING_NODEMANAGER = "Initializing NodeManager ...";
	private static final String INITIALIZING_SECTIONMANAGER = "Initializing SectionManager ...";
	private static final String INITIALIZING_COLLISIONMANAGER = "Initializing CollisionManager ...";
	private static final String INITIALIZING_CLOSELOOPMANAGER = "Initializing CloseLoopManager ...";
	private static final String INITIALIZING_CARRIERLOCMANAGER = "Initializing CarrierLocManager ...";
	private static final String INITIALIZING_STBCARRIERLOCMANAGER = "Initializing STBCarrierLocManager ...";
	private static final String INITIALIZING_BLOCKINFOMANAGER = "Initializing BlockInfoManager ...";
	private static final String INITIALIZING_ZONECONTROLMANAGER = "Initializing ZoneControlManager ...";
	private static final String INITIALIZING_VEHICLEERRORMANAGER = "Initializing VehicleErrorManager ...";
	private static final String INITIALIZING_STATIONMANAGER = "Initializing StationManager ..."; // 2013.02.15 by KYK
	
	private static final String OPERATION_TRACE = "OperationDebug";
	private static Logger operationTraceLog = Logger.getLogger(OPERATION_TRACE);
	
	private static final String OPERATION_EXCEPTION_TRACE = "OperationException";
	private static Logger operationExceptionLog = Logger.getLogger(OPERATION_EXCEPTION_TRACE);
	
	// 2011.11.04 by PMM
	private static final String RUNTIMEUPDATEHISTORY = "RuntimeUpdateHistory";
	private static Logger runtimeUpdateHistoryLog = Logger.getLogger(RUNTIMEUPDATEHISTORY);
	
	private static final String HOSTREPORT_TRACE = "HostReport";
	private static Logger hostReportLog = Logger.getLogger(HOSTREPORT_TRACE);
	
	// 2011.12.27 by PMM
	private static final String DEADLOCKBREAK = "DeadlockBreak";
	private static Logger deadlockBreakLog = Logger.getLogger(DEADLOCKBREAK);
	
	// 2012.02.20 by PMM
	private static final String FORMAT_TRCOMPLETIONHISTORY_TRACE = "TrCompletionHistoryLog";
	private static Logger trCompletionHistoryTraceFormatLog = Logger.getLogger(FORMAT_TRCOMPLETIONHISTORY_TRACE);
	private static final String FOUR_ZEROS = "0000";
	private SimpleDateFormat sdf;	// 2022.03.14 dahye : Premove Logic Improve
	private SimpleDateFormat sdf2;
	
	// 2012.11.30 by KYK : ResultCode 세분화
	public static final String UNLOADED_BUT_CARRIERNOTEXIST = "UNLOADED_BUT_CARRIERNOTEXIST";
	public static final String STBUNLOAD_CARRIERMISMATCH = "STBUNLOAD_CARRIERMISMATCH";
	public static final String STB_LOADFAIL = "STB_LOADFAIL"; 
	public static final String EQ_LOADFAIL = "EQ_LOADFAIL";
	public static final String STB_UNLOADFAIL = "STB_UNLOADFAIL"; 
	public static final String EQ_UNLOADFAIL = "EQ_UNLOADFAIL";
	public static final String TRDELETED_BY_VEHICLEREMOVE = "TRDELETED_BY_VEHICLEREMOVE";
	public static final String TRDELETED_BY_USER = "TRDELETED_BY_USER";
	public static final String SCANDELETED_BY_USER = "SCANDELETED_BY_USER";
	public static final String STBPORT_OUTOFSERVICE = "STBPORT_OUTOFSERVICE";
	public static final String RAILDOWN = "RAILDOWN";	
	public static final String UNLOADING_VHL_ERROR_CARRIER_EXIST = "UNLOADING_VHL_ERROR_CARRIER_EXIST";	
	public static final String UNLOADING_VHL_ERROR_CARRIER_NOT_EXIST = "UNLOADING_VHL_ERROR_CARRIER_NOT_EXIST";
	public static final String MISSED_CARRIER = "MISSED_CARRIER";	
	public static final String DETOUR = "DETOUR";

	public static final String HOMEDIR = "user.dir";
	public static final String FILESEPARATOR = "file.separator";

	public static final String RESULTCODE = "RESULTCODE";	
	public static final String NAME = "NAME";
	public static final String ITEM = "ITEM";	

	// 2011.10.24 by PMM
	private ConcurrentHashMap<String, TrCmd> changedRemoteCmdRequestedNotAssignedTrCmdList;
	
	// 2011.12.27 by PMM
	private int deadlockCheckCount = 0;
	
	// 2014.09.24 by zzang9un : Deadlock 체크 주기를 10초로 변경
	// 약 2초 주기로 체크 (Thread sleep: 200 ms)
//	private static final int DEADLOCK_CHECK_PERIOD = 10;
	private static final int DEADLOCK_CHECK_PERIOD = 50;
	private boolean isDeadlockCheckNeeded = false;
	
	private ArrayList<VehicleData> driveFailedVehicleList;
	private ArrayList<VehicleData> deadlockDetectedVehicleList;
	private ArrayList<VehicleData> deadlockBrokenVehicleList;
	private HashMap<VehicleData, Integer> deadlockCheckedMap;
	private ArrayList<VehicleData> previousAbnormalVehicleList;
	private ArrayList<Hid> previousAbnormalHidList;
	// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//	private HashSet<Node> abnormalVehiclesCollisionNodeSet;
	private ConcurrentHashMap<Node, VehicleData> abnormalVehiclesOnCollisionMap;
	
	// 2014.08.18 by zzang9un : 대차 감지된 Vehicle List
	private ArrayList<VehicleData> detectedVehicleList;
	
	// 2014.09.29 by zzang9un : Deadlock이 감지된 loop 내 vehicle을 저장하는 list
	private HashMap<String, Long> deadlockDetectedLoopMap;
	
	// 2015.06.11 by KYK
	HashSet<TrCmd> prevAbortedTrCmdList;
	
	// 2022.03.14 dahye : Premove Logic Improve
	private ConcurrentHashMap<String, TrCmd> premoveTrCmdList;

	/**
	 * Constructor of Operation class.
	 */
	public OperationManager() {
		// 각 Manager 생성
		initializeManager();
		
		// 2012.11.30 by KYK : ResultCode 세분화
		loadResultCodeDetail();

		// Setup Parameters 설정
		isIBSEMUsed = ocsInfoManager.isIBSEMUsed();
		
		// 2012.01.10 by PMM
		isNearByDrive = ocsInfoManager.isNearByDrive();
		
		isSystemPauseRequested = false;
		isAllVehicleStop = false;
		isAllOperationReady = false;
		isFailoverCompleted = false;
		
		// 2011.10.12 by PMM OCSINFO update 지연으로  TSCAutoCompleted 중복 보고 방지
		isTSCPauseInitiatedReported = false;
		isTSCAutoCompletedReported = false;
		isTSCPauseCompletedReported = false;
		
		// 2015.09.16 by MYM : Manager의 멤버 변수는 Operation 생성 전에 생성(초기화)하도록 위로 올림  
		// 2011.12.27 by PMM
		driveFailedVehicleList = new ArrayList<VehicleData>();
		deadlockDetectedVehicleList = new ArrayList<VehicleData>();
		deadlockBrokenVehicleList = new ArrayList<VehicleData>();
		deadlockCheckedMap = new HashMap<VehicleData, Integer>();
		previousAbnormalVehicleList = new ArrayList<VehicleData>();
		previousAbnormalHidList = new ArrayList<Hid>();
		// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//		abnormalVehiclesCollisionNodeSet = nodeManager.getAbnormalVehiclesCollisionNodeSet();
		abnormalVehiclesOnCollisionMap = new ConcurrentHashMap<Node, VehicleData>();
		// 2014.08.18 by zzang9un : list 초기화
		detectedVehicleList = new ArrayList<VehicleData>();
		// 2014.09.29 by zzang9un
		deadlockDetectedLoopMap = new HashMap<String, Long>();
		// 2015.06.11 by KYK
		prevAbortedTrCmdList = new HashSet<TrCmd>();
		// 2012.02.20 by PMM
		sdf = new SimpleDateFormat("yyyyMMddHHmmss");	// 2022.03.14 dahye : Premove Logic Improve
		sdf2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
		
		// Vehicle Socket Port 설정
		vehicleSocketPort = CommonConfig.getInstance().getVehicleSocketPort();
		
		// 2015.07.01 by MYM : CarrierType 설정 (Material - CarrierType)
		CarrierTypeConfig.getInstance();
		
		// Operation Instance 생성
		managerMap = new ConcurrentHashMap<String, Operation>();
		createOperationInstance();
		startTime = System.currentTimeMillis();
		
		new LogManager(OcsConstant.OPERATION);
		new HistoryManager();
		
		// 2011.11.04 by PMM
		// OperationUpdate 초기화
		if (ocsInfoManager.getOperationUpdate() != RUNTIME_UPDATE.NO) {
			ocsInfoManager.setOperationUpdate(RUNTIME_UPDATE.NO);
		}
		
		// 2014.11.13 by zzang9un : ThreadMonitoringManager 초기화
		threadMonitoringManager = ThreadMonitorManager.getInstance(this.managerMap, 5000);
	}
	
	/**
	 * 2012.11.30 by KYK
	 * ResultCode 세분화
	 */
	private void loadResultCodeDetail() {
		// file route
		String homePath = System.getProperty(HOMEDIR);
		String fileSeperator = System.getProperty(FILESEPARATOR);
		String fileName = "ResultCode.xml";
		String configFile = homePath + fileSeperator + fileName;
		
		// file reading from xml
		String name;
		String strResultCode;
		int resultCode;
		
		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		
		try {
			doc = saxb.build(configFile);
			Element root = doc.getRootElement();
			@SuppressWarnings("unchecked")
			List<Element> list = root.getChildren(ITEM);
			for (Element element: list) {
				name = element.getChildTextTrim(NAME);
				strResultCode = element.getChildTextTrim(RESULTCODE);
				if (strResultCode != null && strResultCode.length() > 0) {
					resultCode = Integer.parseInt(strResultCode);
					if (resultCode != 0) { // zero means ok
						if (UNLOADED_BUT_CARRIERNOTEXIST.equals(name)) {
							ResultCode.RESULTCODE_UNLOADED_BUT_CARRIERNOTEXIST = resultCode;
						} else if (STBUNLOAD_CARRIERMISMATCH.equals(name)) {
							ResultCode.RESULTCODE_STBUNLOAD_CARRIERMISMATCH = resultCode;
						} else if (STB_LOADFAIL.equals(name)) {
							ResultCode.RESULTCODE_STB_LOADFAIL = resultCode;
						} else if (EQ_LOADFAIL.equals(name)) {
							ResultCode.RESULTCODE_EQ_LOADFAIL = resultCode;						
						} else if (STB_UNLOADFAIL.equals(name)) {
							ResultCode.RESULTCODE_STB_UNLOADFAIL = resultCode;
						} else if (EQ_UNLOADFAIL.equals(name)) {
							ResultCode.RESULTCODE_EQ_UNLOADFAIL = resultCode;
						} else if (TRDELETED_BY_VEHICLEREMOVE.equals(name)) {
							ResultCode.RESULTCODE_TRDELETED_BY_VEHICLEREMOVE = resultCode;
						} else if (TRDELETED_BY_USER.equals(name)) {
							ResultCode.RESULTCODE_TRDELETED_BY_USER = resultCode;
						} else if (SCANDELETED_BY_USER.equals(name)) {
							ResultCode.RESULTCODE_SCANDELETED_BY_USER = resultCode;
						} else if (STBPORT_OUTOFSERVICE.equals(name)) {
							ResultCode.RESULTCODE_STBPORT_OUTOFSERVICE = resultCode;
						} else if (RAILDOWN.equals(name)) {
							ResultCode.RESULTCODE_RAILDOWN = resultCode;
						} else if (UNLOADING_VHL_ERROR_CARRIER_EXIST.equals(name)) {
							ResultCode.RESULTCODE_UNLOADING_VHL_ERROR_CARRIER_EXIST = resultCode;
						} else if (UNLOADING_VHL_ERROR_CARRIER_NOT_EXIST.equals(name)) {
							ResultCode.RESULTCODE_UNLOADING_VHL_ERROR_CARRIER_NOT_EXIST = resultCode;
						} else if (MISSED_CARRIER.equals(name)) {
							ResultCode.RESULTCODE_MISSED_CARRIER = resultCode;
						} else if (DETOUR.equals(name)) {
							ResultCode.RESULTCODE_DETOUR = resultCode;
						}
					}
					System.out.println("resultcode:" + resultCode);
				}
			}
		} catch (JDOMException e) {
			traceOperationException("loadResultCodeDetail()", e);
		} catch (IOException e) {
			traceOperationException("loadResultCodeDetail()", e);
		} catch (Exception e) {
			traceOperationException("loadResultCodeDetail()", e);			
		}		
	}

	/**
	 * Initialize Managers
	 */
	public void initializeManager() {
		// DBAccessManager 용도별 설정
		// dbAccessManager1 : 기능 ON/OF   [Thread:(O), UpdateFromDB (O), UpdateToDB (X)]
		// 										(RailDownControlManager, AutoRetryGroupInfoManager) + AlarmManager(UpdateToDB용 1개만 추가)
		// dbAccessManager2 : 기능 관련     [Thread:(O), UpdateFromDB (O), UpdateToDB (X)]
		//										(CloseLoopManager, LocalGroupInfoManager, UserDefinedPathManager, UserOperationManager)
		// dbAccessManager3 : 쓰레드 미사용 [Thread:(X), UpdateFromDB (O), UpdateToDB (X)]
		//										(ZoneControlManager, VehicleErrorManager) + CarrierLocManager(UpdateToDB용 1개만 추가)
		// new DBAccessManager() : UpdateToDB 사용 빈도가 높은 경우 [Thread:(O), UpdateFromDB (O), UpdateToDB (O)]
		DBAccessManager dbAccessManager1 = new DBAccessManager();
		DBAccessManager dbAccessManager2 = new DBAccessManager();
		DBAccessManager dbAccessManager3 = new DBAccessManager();
		
		// 2012.02.03 by MYM : 타 Manager에서 알람 등록을 할 수 있도록 AlarmManager 생성을 가정 먼저 하도록 함. 
		traceOperation("AlarmManager is initializing...");
		alarmManager = AlarmManager.getInstance(Alarm.class, dbAccessManager1, true, true, 200);
		
		traceOperation("OCSInfoManager is initializing...");
		ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, new DBAccessManager(), true, true, 200);
		
		traceOperation("VehicleErrorManager is initializing...");
		vehicleErrorManager = VehicleErrorManager.getInstance(VehicleError.class, dbAccessManager3, true, false, 500);
		
		// 2015.02.10 by MYM : 장애 지역 우회 기능
		traceOperation("DetourManager is initializing...");
		detourControlManager = DetourControlManager.getInstance(DetourControl.class,  new DBAccessManager(), true, true, 500);

		traceOperation("HIDManager is initializing...");
		hidManager = HIDManager.getInstance(Hid.class, new DBAccessManager(), true, true, 200);
		// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
		hidManager.setOperation(true);
		
		traceOperation("NodeManager is initializing...");
		nodeManager = NodeManager.getInstance(Node.class, new DBAccessManager(), true, true, 200);
		// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
		nodeManager.setOperation(true);
		
		traceOperation("SectionManager is initializing...");
		// 2014.02.03 by MYM : Disabled Link 처리 (실시간 Enabled 업데이트 - 쓰레드 사용)
//		sectionManager = SectionManager.getInstance(Section.class, dbAccessManager2, true, false, 500);
		sectionManager = SectionManager.getInstance(Section.class, new DBAccessManager(), true, true, 200);
		// 2015.03.18 by MYM : operation 모듈만 기능 동작 하도록 조건 추가
		sectionManager.setOperation(true);
		
		// 2015.05.27 by MYM : Vehicle Traffic 분산
		traceOperation("TrafficManager is initializing...");
		trafficManager = TrafficManager.getInstance(Traffic.class, new DBAccessManager(), true, true, 2000);

		// 2015.12.21 by KBS : Patrol VHL 기능 추가
		traceOperation("UserRequestManager is initializing...");
		UserRequestManager.getInstance(UserRequest.class, dbAccessManager3, true, false, 500);
		
		// 2012.05.16 by MYM : Rail-Down
		traceOperation("RailDownControlManager is initializing...");
//		RailDownControlManager.getInstance(RailDownControl.class, dbAccessManager3, true, true, 200);
		RailDownControlManager.getInstance(RailDownControl.class, dbAccessManager1, true, true, 200);

		// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
		traceOperation("AutoRetryGroupInfoManager is initializing...");
		AutoRetryGroupInfoManager.getInstance(AutoRetryGroupInfo.class, dbAccessManager1, true, true, 200);
		
		traceOperation("CollisionManager is initializing...");
		collisionManager = CollisionManager.getInstance(Collision.class, new DBAccessManager(), true, true, 200);
		
		traceOperation("CloseLoopManager is initializing...");
//		closeLoopManager = CloseLoopManager.getInstance(CloseLoop.class, dbAccessManager3, true, true, 200);
		closeLoopManager = CloseLoopManager.getInstance(CloseLoop.class, dbAccessManager2, true, true, 200);
		
		traceOperation("CarrierLocManager is initializing...");
//		carrierLocManager = CarrierLocManager.getInstance(CarrierLoc.class, dbAccessManager1, true, true, 200);
		carrierLocManager = CarrierLocManager.getInstance(CarrierLoc.class, dbAccessManager3, true, true, 200);
		traceOperation("STBCarrierLocManager is initializing...");
		stbCarrierLocManager = STBCarrierLocManager.getInstance(STBCarrierLoc.class, new DBAccessManager(), true, true, 500);
		
		traceOperation("BlockInfoManager is initializing...");
		blockInfoManager = BlockManager.getInstance(Block.class, new DBAccessManager(), true, true, 200);
		
		traceOperation("TrCmdManager is initializing...");
		trCmdManager = TrCmdManager.getInstance(TrCmd.class, new DBAccessManager(), true, true, 100);
		trCmdManager.setOperation(true);
		
		traceOperation("VehicleManager is initializing...");
		// 2014.03.14 by KYK
//		vehicleManager = VehicleManager.getInstance(VehicleData.class, new DBAccessManager(), true, true, 50);
		vehicleManager = VehicleManager.getInstance(VehicleData.class, new DBAccessManager(), true, true, 100);
		
		traceOperation("LocalGroupInfoManager is initializing...");
//		LocalGroupInfoManager.getInstance(LocalGroupInfo.class, dbAccessManager3, true, true, 200);
		LocalGroupInfoManager.getInstance(LocalGroupInfo.class, dbAccessManager2, true, true, 200);
		
		traceOperation("EventHistoryManager is initializing...");
//		eventHistoryManager = EventHistoryManager.getInstance(EventHistory.class, dbAccessManager2, false, true, 200);
		eventHistoryManager = EventHistoryManager.getInstance(EventHistory.class, new DBAccessManager(), false, true, 200);
		
		traceOperation("TrCompletionHistoryManager is initializing...");
//		trCompletionHistoryManager = TrCompletionHistoryManager.getInstance(TrCompletionHistory.class, dbAccessManager3, false, true, 200);
		trCompletionHistoryManager = TrCompletionHistoryManager.getInstance(TrCompletionHistory.class, new DBAccessManager(), false, true, 200);
		
		traceOperation("ZoneControlManager is initializing...");
//		zoneControlManager = ZoneControlManager.getInstance(ZoneControl.class, dbAccessManager1, true, false, 500);
		zoneControlManager = ZoneControlManager.getInstance(ZoneControl.class, dbAccessManager3, true, false, 500);
		
		traceOperation("VehicleErrorHistoryManager is initializing...");
//		VehicleErrorHistoryManager.getInstance(VehicleErrorHistory.class, dbAccessManager1, false, true, 200);
		VehicleErrorHistoryManager.getInstance(VehicleErrorHistory.class, new DBAccessManager(), false, true, 200);
		
		traceOperation("IBSEMReportManager is initializing...");
		ibsemReportManager = IBSEMReportManager.getInstance(IBSEMReport.class, new DBAccessManager(), false, true, 100);

		traceOperation("UserDefiedPathManager is initializing...");
		UserDefinedPathManager.getInstance(UserDefinedPath.class, dbAccessManager2, true, true, 200);
		
		traceOperation("UserOperationManager is initializing...");
//		UserOperationManager.getInstance(UserOperation.class,  dbAccessManager3, true, true, 200);
		UserOperationManager.getInstance(UserOperation.class,  dbAccessManager2, true, true, 200);
		
		// 2013.02.15 by KYK
		traceOperation("StationManager is initializing...");
		stationManager = StationManager.getInstance(Station.class, dbAccessManager3, true, true, 200);
		
		setManagerServiceState(MODULE_STATE.REQOUTOFSERVICE);
		traceOperation("Initialized Managers!!");
	}
	
	/**
	 * 초기 구동시 Enabled된 Vehicle을 관리하는 Operation Instance를 생성한다.
	 */
	private void createOperationInstance() {
		assert vehicleManager != null;
		
		ConcurrentHashMap<String, Object> mapData = vehicleManager.getData();
		Set<String> vehicleIdList = mapData.keySet();
		VehicleData vehicle = null;
		Operation operation = null;
		for (String vehicleId : vehicleIdList) {
			vehicle = (VehicleData) mapData.get(vehicleId);
			if (vehicle != null && vehicle.isEnabled()) {
				if (vehicle.getCurrNode() != null && vehicle.getStopNode() != null) {
					if (vehicle.getCurrNode().length() > 0 && vehicle.getStopNode().length() > 0) {
						operation = managerMap.get(vehicleId);
						if (operation == null) {
							operation = new Operation(vehicle);
							operation.startOperation(this, false, vehicleSocketPort);
							managerMap.put(vehicleId, operation);
						}
					} else {
						traceOperationException(vehicle.getVehicleId() + ": CurrNode/StopNode is null.");
					}
				} else {
					traceOperationException(vehicle.getVehicleId() + ": CurrNode/StopNode is null.");
				}
			}
		}
	}
	
	private boolean setManagerServiceState(MODULE_STATE STATE) {
		assert ocsInfoManager != null;
		assert nodeManager != null;
		assert sectionManager != null;
		assert collisionManager != null;
		assert carrierLocManager != null;
		assert blockInfoManager != null;
		assert trCmdManager != null;
		assert vehicleManager != null;
		assert zoneControlManager != null;
		
		ocsInfoManager.setReqServiceState(STATE);
		HIDManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		nodeManager.setReqServiceState(STATE);
		sectionManager.setReqServiceState(STATE);		
		collisionManager.setReqServiceState(STATE);
		closeLoopManager.setReqServiceState(STATE);
		carrierLocManager.setReqServiceState(STATE);
		detourControlManager.setReqServiceState(STATE);
		
		blockInfoManager.setReqServiceState(STATE);
		trCmdManager.setReqServiceState(STATE);
		vehicleManager.setReqServiceState(STATE);
		LocalGroupInfoManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		AlarmManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		EventHistoryManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		TrCompletionHistoryManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);

		zoneControlManager.setReqServiceState(STATE);
		VehicleErrorManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		VehicleErrorHistoryManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		IBSEMReportManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		
		// 2015.04.10 by MYM : 누락된 Manager 추가
		RailDownControlManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		AutoRetryGroupInfoManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		stbCarrierLocManager.setReqServiceState(STATE);
		UserDefinedPathManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		UserOperationManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		
		// 2015.05.27 by MYM : Vehicle Traffic 분산
		trafficManager.setReqServiceState(STATE);
		
		// 2013.02.15 by KYK
		stationManager.setReqServiceState(STATE);
		
		// 2015.12.21 by KBS : Patrol VHL 기능 추가
		UserRequestManager.getInstance(null, null, false, false, 0).setReqServiceState(STATE);
		
		if (trCmdManager != null &&
				trCmdManager.getServiceState() == MODULE_STATE.INSERVICE &&
				vehicleManager != null &&
				vehicleManager.getServiceState() == MODULE_STATE.INSERVICE) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * 2015.04.09 by MYM : RuntimeUpdate시 관련 Manager만 ServiceState를 변경
	 * @param STATE
	 */
	private void setManagerServiceStateForRuntimeUpdate(MODULE_STATE STATE) {
		// updateManager에서 initializeFromDB를 하는 Manager만 ServiceState를 변경
		hidManager.setReqServiceState(STATE);
		nodeManager.setReqServiceState(STATE);
		sectionManager.setReqServiceState(STATE);
		collisionManager.setReqServiceState(STATE);
		closeLoopManager.setReqServiceState(STATE);
		carrierLocManager.setReqServiceState(STATE);
		stbCarrierLocManager.setReqServiceState(STATE);
		blockInfoManager.setReqServiceState(STATE);
		zoneControlManager.setReqServiceState(STATE);
		vehicleErrorManager.setReqServiceState(STATE);
		stationManager.setReqServiceState(STATE);
		// 2015.09.21 by MYM : RuntimeUpdate시 Traffic정보 재 갱신 
		trafficManager.setReqServiceState(STATE);
	}

	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = 200;
	}

	@Override
	protected void stopProcessing() {
		
	}

	/**
	 * Main Processing Method
	 */
	long prevTimeMillis = System.currentTimeMillis();
	long termTimeMillis = 0;
	@Override
	protected void mainProcessing() {
		operationTimeLogging(0);
		
		checkDeadlockCheckPeriod();
		manageTSCState();
		manageServiceState();
		checkAbnormalStateChanged();
		
		operationTimeLogging(1);
		
		manageOperationInstance();
		
		operationTimeLogging(2);
		
		manageAllOperationReady();
		checkFailoverCompleted();
		
		operationTimeLogging(3);
		
		manageDeadlock();
		
		operationTimeLogging(4);
		
		manageRuntimeUpdate();
		
		operationTimeLogging(5);
		
		processChangedRemoteCmdNotAssignedTrCmd();
		checkDelayedDestChange(); // 2015.06.11 by KYK
		updateOperationTime();
		
		operationTimeLogging(6);
		
		// 2022.03.14 dahye : Premove Logic Improve
		checkNotAssignedPremoveCommand();
		operationTimeLogging(7);
	}
	
	private void operationTimeLogging(int index) {
		if(ocsInfoManager.isOperationManagerLoggingUsage()){
			long currentTimeMillis = System.currentTimeMillis();
			termTimeMillis = currentTimeMillis - prevTimeMillis;
			
			StringBuilder sb = new StringBuilder("mainProcessing Running(");
			sb.append(index).append("):").append(termTimeMillis);
			traceOperation(sb.toString());
			
			prevTimeMillis = currentTimeMillis;
		}
	}
	
	/**
	 * 2015.06.11 by KYK
	 */
	private void checkDelayedDestChange() {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			HashSet<TrCmd> removeAbortedTrCmdList = (HashSet<TrCmd>) prevAbortedTrCmdList.clone();		
			try {
				Vector<TrCmd> abortedTrCmdListClone = (Vector<TrCmd>) trCmdManager.getAbortedTrCmdList().clone();
				for (TrCmd trCmd : abortedTrCmdListClone) {
					if (trCmd.isDestChangeDelayed() == false) {
						if (trCmd.getLastAbortedTime() == 0) {
							trCmd.setLastAbortedTime(System.currentTimeMillis());
						}
						double abortCheckTime = ocsInfoManager.getAbortCheckTime();
						if (System.currentTimeMillis() - trCmd.getLastAbortedTime() > abortCheckTime * 1000) {
							trCmd.setDestChangeDelayed(true);
							
							StringBuffer msg = new StringBuffer();
							msg.append("MCS Abort후 DestChange 처리지연 - ");
							msg.append(abortCheckTime / 60);
							msg.append("분 초과. CarrierId:").append(trCmd.getCarrierId());
							msg.append(", TrCmdId:").append(trCmd.getTrCmdId());
							msg.append(" - ").append(trCmd.getVehicle());
							traceOperation(trCmd.getVehicle(), msg.toString());
							registerAlarm(trCmd.getTrCmdId(), OcsAlarmConstant.DELAYED_DESTCHANGE, msg.toString());
						}
					}
					if (prevAbortedTrCmdList.contains(trCmd) == false) {
						prevAbortedTrCmdList.add(trCmd);					
					}
					removeAbortedTrCmdList.remove(trCmd);
				}
				for (TrCmd trCmd : removeAbortedTrCmdList) {
					prevAbortedTrCmdList.remove(trCmd);
					trCmd.setDestChangeDelayed(false);
					unregisterAlarm(trCmd.getTrCmdId());
				}
			} catch (Exception e) {
				traceOperationException("checkDelayedDestChange()", e);
			}
		}
	}
	
	public void registerAlarm(String key, int alarmCode, String alarmText) {
		if (key.length() > 30) {
			key = key.substring(0, 30);
		}
		if (alarmText.length() > 160) {
			alarmText = alarmText.substring(0, 160);
		}
		
		alarmManager.registerAlarm(key, alarmCode, alarmText, ALARMLEVEL.ERROR);				
	}

	public void unregisterAlarm(String key) {
		if (key.length() > 30) {
			key = key.substring(0,30);
		}
		alarmManager.unregisterAllAlarm(key);
	}

	private void checkDeadlockCheckPeriod() {
		// 2014.08.13 by zzang9un : 근접제어 Deadlock 처리 되도록 수정
		// 2014.09.13 by zzang9un : 근접제어 Deadlock Break Usage로 deadlock 기능 on/off 하도록 수정
		if ((isNearByDrive == false || ocsInfoManager.isDeadlockBreakNearbyDriveUsage() == true) &&
				this.serviceState == MODULE_STATE.INSERVICE) {
			if (isAllOperationReady) {
				if (++deadlockCheckCount >= DEADLOCK_CHECK_PERIOD) {
					isDeadlockCheckNeeded = true;
					deadlockCheckCount = 0;
					return;
				}
			}
		}
		isDeadlockCheckNeeded = false;
	}
	
	private void checkAbnormalStateChanged() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				// Abnormal State
				// 1. Node: Disabled -> Enabled
				// 2. HID: S, F, W -> o.w.
				// 3. VHL: CommFail, Manual, NotResponding -> Normal
				
				isAbnormalStateChanged = false;
				checkAbnormalVehicleStateChanged();
				checkAbnormalNodeStateChanged();
				checkAbnormalHidStateChanged();
				// 2014.02.03 by MYM : Disabled Link 처리
				checkAbnormalLinkChanged();
			}
		} catch (Exception e) {
			traceOperationException("checkAbnormalStateChanged()", e);
		}
	}
	
	private void checkAbnormalVehicleStateChanged() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				ArrayList<VehicleData> tempAbnormalVehicleList = new ArrayList<VehicleData>();
				ConcurrentHashMap<String, Object> mapData = vehicleManager.getData();
				Set<String> vehicleIdList = mapData.keySet();
				VehicleData vehicle = null;
				for (String vehicleId : vehicleIdList) {
					vehicle = (VehicleData) mapData.get(vehicleId);
					if (vehicle != null && vehicle.isEnabled()) {
						// 2015.09.16 by MYM : vehicle의 isAbnormalvehicle로 조건 변경
//						if (vehicle.getVehicleMode() == 'M' ||
//								vehicle.getErrorCode() == OcsConstant.COMMUNICATION_FAIL ||
//								vehicle.getAlarmCode() > 5000) {
						if (vehicle.isAbnormalVehicle()) {
							tempAbnormalVehicleList.add(vehicle);
							previousAbnormalVehicleList.remove(vehicle);
						}
					}
				}
				
				if (previousAbnormalVehicleList.size() > 0) {
					isAbnormalStateChanged = true;
					StringBuilder message = new StringBuilder();
					message.append("AbnormalState Changed. Recovered VHL:");
					message.append(previousAbnormalVehicleList.toString());
					traceOperation(message.toString());
				}
				updateAbnormalVehicleNodesCollisions(tempAbnormalVehicleList);
				previousAbnormalVehicleList = tempAbnormalVehicleList;
			}
		} catch (Exception e) {
			traceOperationException("checkAbnormalVehicleStateChanged()", e);
		}
	}
	
	private void updateAbnormalVehicleNodesCollisions(ArrayList<VehicleData> abnormalVehicleList) {
		try {
			if (abnormalVehicleList != null) {
				// 2015.09.16 by MYM : 비근접은 Collision, 근접은 Block 위치의 장애 Vehicle을 보도록 변경
//				if (this.serviceState == MODULE_STATE.INSERVICE &&
//						ocsInfoManager.isCollisionNodeCheckUsed() &&
//						ocsInfoManager.isNearByDrive() == false) {
				if (this.serviceState == MODULE_STATE.INSERVICE &&
						ocsInfoManager.isCollisionNodeCheckUsed()) {
					if (ocsInfoManager.isNearByDrive()) {
						// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
						updateAbnormalVehicleNodesBlockNodes(abnormalVehicleList);
					} else {
						updateAbnormalVehicleNodesCollisionNodes(abnormalVehicleList);
					}
				} else {
					abnormalVehiclesOnCollisionMap.clear();
				}
			}
		} catch (Exception e) {
			traceOperationException("updateAbnormalVehicleNodesCollisions()", e);
		}
	}
	
	/**
	 * 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
	 * 
	 * @return
	 */
	public ConcurrentHashMap<Node, VehicleData> getAbnormalVehiclesOnCollisionMap() {
		return abnormalVehiclesOnCollisionMap;
	}
	
	/**
	 * 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
	 * 
	 * @param abnormalVehicleList
	 */
	private void updateAbnormalVehicleNodesCollisionNodes(ArrayList<VehicleData> abnormalVehicleList) {
		HashSet<Node> removeBlockNodeSet = new HashSet<Node>(abnormalVehiclesOnCollisionMap.keySet());
		for (VehicleData abnormalVehicle : abnormalVehicleList) {
			if (abnormalVehicle != null) {
				for (int i = 0; i < abnormalVehicle.getDriveNodeCount(); i++) {
					Node node = abnormalVehicle.getDriveNode(i);
					if (node != null) {
						for (Collision collision : node.getCollisions()) {
							if (collision != null) {
								Node collisionNode = collision.getCollisionNode(node);
								if (collisionNode != null) {
									abnormalVehiclesOnCollisionMap.put(collisionNode, abnormalVehicle);
									removeBlockNodeSet.remove(collisionNode);
								}
							}
						}
					}
				}
			}
		}
		
		for (Node node : removeBlockNodeSet) {
			abnormalVehiclesOnCollisionMap.remove(node);
		}
	}
	
	/**
	 * 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
	 * 
	 * @param abnormalVehicleList
	 */
	private void updateAbnormalVehicleNodesBlockNodes(ArrayList<VehicleData> abnormalVehicleList) {
		HashSet<Node> removeBlockNodeSet = new HashSet<Node>(abnormalVehiclesOnCollisionMap.keySet());
		for (VehicleData abnormalVehicle : abnormalVehicleList) {
			if (abnormalVehicle != null) {
				for (int i = 0; i < abnormalVehicle.getDriveNodeCount(); i++) {
					Node node = abnormalVehicle.getDriveNode(i);
					if (node != null) {
						for (Block block : node.getCollisionBlockSet()) {
							if (block != null) {
								for (Node blockNode : block.getNodeList()) {
									abnormalVehiclesOnCollisionMap.put(blockNode, abnormalVehicle);
									removeBlockNodeSet.remove(blockNode);
								}
							}
						}
					}
				}
			}
		}
		
		for (Node node : removeBlockNodeSet) {
			abnormalVehiclesOnCollisionMap.remove(node);
		}
	}
	
	private void checkAbnormalNodeStateChanged() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				ArrayList<Node> tempEnableChangedNodeList = nodeManager.getEnableChangedNodeList();
				if (tempEnableChangedNodeList != null && tempEnableChangedNodeList.size() > 0) {
					isAbnormalStateChanged = true;
					
					StringBuilder message = new StringBuilder();
					message.append("AbnormalState Changed. Enabled Node");
					if (tempEnableChangedNodeList.size() > 1) {
						message.append("s");
					}
					message.append(":");
					int count = 0;
					for (Node enabledNode : tempEnableChangedNodeList) {
						if (enabledNode != null) {
							if (count > 0) {
								message.append(",");
							}
							message.append(enabledNode.getNodeId());
							count++;
						}
					}
					traceOperation(message.toString());
					nodeManager.resetEnableChangedNodeList();
				}
			}
		} catch (Exception e) {
			traceOperationException("checkAbnormalNodeStateChanged()", e);
		}
	}

	private void checkAbnormalHidStateChanged() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				ArrayList<Hid> tempAbnormalHidList = new ArrayList<Hid>();
				ConcurrentHashMap<String, Object> hidData = hidManager.getData();
				Set<String> hidIdList = hidData.keySet();
				Hid hid = null;
				for (String hidId : hidIdList) {
					hid = (Hid) hidData.get(hidId);
					if (hid != null && hid.isAbnormalState()) {
						tempAbnormalHidList.add(hid);
						previousAbnormalHidList.remove(hid);
					}
				}
				
				if (previousAbnormalHidList.size() > 0) {
					isAbnormalStateChanged = true;
					
					StringBuilder message = new StringBuilder();
					message.append("AbnormalState Changed. Recovered HID:");
					int count = 0;
					Hid altHid = null;
					for (Hid recoveredHid : previousAbnormalHidList) {
						if (recoveredHid != null) {
							if (count > 0) {
								message.append(", ");
							}
							message.append(recoveredHid.getUnitId());
							message.append("(");
							message.append(recoveredHid.getState());
							if (recoveredHid.isHidDown()) {
								message.append(",AltHid:");
								message.append(recoveredHid.getAltHidName());
								altHid = recoveredHid.getAltHid();
								if (altHid != null) {
									message.append("(");
									message.append(altHid.getState());
									message.append(",AH:");
									message.append(altHid.getAltHidName());
									message.append(",BH:");
									message.append(altHid.getBackupHidName());
									message.append(")");
								}
							}
							message.append(")");
						}
						count++;
					}
					traceOperation(message.toString());
				}
				previousAbnormalHidList = tempAbnormalHidList;
			}
		} catch (Exception e) {
			traceOperationException("checkAbnormalHidStateChanged()", e);
		}
	}
	
	/**
	 * 2014.02.03 by MYM : Disabled Link 처리
	 */
	private void checkAbnormalLinkChanged() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				Vector<Link> tempChangedLinkList = sectionManager.getChangedLinkList();
				if (tempChangedLinkList != null && tempChangedLinkList.size() > 0) {
					isAbnormalStateChanged = true;
					
					StringBuilder message = new StringBuilder();
					message.append("AbnormalState Changed. Enabled Link");
					if (tempChangedLinkList.size() > 1) {
						message.append("s");
					}
					message.append(":");
					int count = 0;
					for (Link link : tempChangedLinkList) {
						if (link != null) {
							if (count > 0) {
								message.append(",");
							}
							message.append(link.getLinkId());
							count++;
						}
					}
					traceOperation(message.toString());
					sectionManager.resetChangedLinkList();
				}
			}
		} catch (Exception e) {
			traceOperationException("checkAbnormalLinkChanged()", e);
		}
	}
	
	/**
	 * 2022.03.14 dahye : Premove Logic Improve
	 * Check NotAssigned Premove Command
	 */
	private void checkNotAssignedPremoveCommand() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				// QUEUED NOT ASSIGNED 된 TRCMD 확인 --> DWT 확인 --> CANCEL 처리
				
				Set<String> searchKeys;
				TrCmd trCmd;
				
				// 1. Queued 'PREMOVE' TrCmd 중 NOT_ASSIGNED 인 반송 리스트 GET
				premoveTrCmdList = trCmdManager.getNotAssignedPremoveTrCmdList();
				searchKeys = new HashSet<String>(premoveTrCmdList.keySet());
				for (String searchKey : searchKeys) {
					trCmd = (TrCmd)premoveTrCmdList.get(searchKey);
					if (trCmd != null) {
						// 2. DWT Timeout 확인
						long waitTime = getWaitingTime(trCmd.getWaitStartedTime());
						if (waitTime >= trCmd.getDeliveryWaitTimeOut()) {
							StringBuffer log = new StringBuffer();
							log.append("PREMOVE DeliveryWaitTime TimeOver. ");
							log.append("ElapseTime:").append(waitTime).append(",RemainingTime:").append(trCmd.getDeliveryWaitTimeOut() - waitTime);
							log.append("(DWT:").append(trCmd.getDeliveryWaitTimeOut()).append(")");
							traceOperation(log.toString());
							
							// 3. DWT Timeout 처리 - CANCEL
							notAssignedPremoveCancel(trCmd);
						}
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("checkNotAssignedPremoveCommand()", e);
		}
	}
	
	/**
	 * 초기 구동 이후 실시간으로 Vehicle이 Enabled or Disabled시 Operation Instance를 생성 및 제거를 담당한다.
	 */
	private void manageOperationInstance() {
		try {
			ConcurrentHashMap<String, Object> mapData = vehicleManager.getData();
			driveFailedVehicleList.clear();
			Set<String> vehicleIdList = mapData.keySet();
			VehicleData vehicle = null;
			Operation operation = null;
			
			// 2014.08.18 by zzang9un : 대차 감지 list 초기화
			detectedVehicleList.clear();

			for (String vehicleId : vehicleIdList) {
				vehicle = (VehicleData) mapData.get(vehicleId);
				operation = managerMap.get(vehicleId);

				if (vehicle != null && vehicle.isEnabled()) {
					if (operation == null) {
						operation = new Operation(vehicle);
						operation.startOperation(this, true, vehicleSocketPort);
						operation.setAllOperationReady(isAllOperationReady);
						operation.setFailoverCompleted(isFailoverCompleted);
						managerMap.put(vehicleId, operation);
					}
					
					// 2012.03.13 by PMM : Deadlock Check Period 확인 부분을 별도로 뺌.
					// 2011.12.27 by PMM
					// Deadlock Detection을 위한 
					// All VHL이 Ready 상태 이후, INSERVICE Host에서만 Deadlock Break가 동작해야 함.
					if (isDeadlockCheckNeeded) {
						if (vehicle.isDriveFailed()) {
							// 정지한(I, A, N, O) Auto Enabled VHL 중 driveLimitTime Over외의 Drive Failed VHL
							if (driveFailedVehicleList.contains(vehicle) == false) {
								driveFailedVehicleList.add(vehicle);
							}
						}
						
						// 2014.08.18 by zzang9un : 대차감지한 vehicle을 list에 add
						if (isNearByDrive) {
							if (vehicle.isForwardVehicleDetected()) {
								if (detectedVehicleList.contains(vehicle) == false) {
									detectedVehicleList.add(vehicle);
									
									// 해당 vehicle의 caused vehicle을 search한다.
									searchForwardVehicleOfDetectedVehicle(vehicle);
								}
							} else {
								// 대차 감지 상태가 아닌 vehicle은 list 초기화
								vehicle.clearDetectedCausedVehicleList();
							}
						}							
					}
					
					if (this.serviceState == MODULE_STATE.INSERVICE) {
						if (isAbnormalStateChanged) {
							if (operation != null) {
								operation.setAbnormalStateChanged(true);
							}
						}
					}
				} else {
					if (operation != null) {
						managerMap.remove(vehicleId);
						operation.stopOperation();
						operation = null;
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("manageOperationInstance()", e);
		}
	}
	
	/**
	 * TSC 상태(PAUSE, RESUME) 변경에 따른 Operation의 서비스 시작 및 일시중지를 관리한다.
	 */
	private void manageTSCState() {
		assert ocsInfoManager != null;
		
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			if (ocsInfoManager.getOcsControlState() == OCS_CONTROL_STATE.PAUSE) {
				ocsInfoManager.addOCSInfoToOCSInfoUpdateList(OCSCONTROL, OCS_CONTROL_STATE.NULL.toConstString());
				ocsInfoManager.addOCSInfoToOCSInfoUpdateList(TSCSTATUS, TSC_STATE.TSC_PAUSING.toConstString());
				
				// Report (TSCPauseInitiated) Msg to MCS
				// 2011.10.12 by PMM OCSINFO update 지연으로  TSCAutoCompleted 중복 보고 방지
				if (isTSCPauseInitiatedReported == false) {
					sendS6F11_TSC(OperationConstant.TSC_PAUSE_INITIATED, 0);
					isTSCPauseInitiatedReported = true;
				}
			} else if (ocsInfoManager.getOcsControlState() == OCS_CONTROL_STATE.RESUME) {
				ocsInfoManager.addOCSInfoToOCSInfoUpdateList(OCSCONTROL, OCS_CONTROL_STATE.NULL.toConstString());
				
				isAllVehicleStop = false;
				ocsInfoManager.addOCSInfoToOCSInfoUpdateList(TSCSTATUS, TSC_STATE.TSC_AUTO.toConstString());
				
				// 2011.11.02 by PMM
				int checkCount = 0;
				while (true) {
					if (vehicleManager.resumeAllVehicleActionHold() || checkCount > 5) {
						break;
					}
					checkCount++;
				}
				
				// Report (TSCAutoCompleted) Msg to MCS
				// 2011.10.12 by PMM OCSINFO update 지연으로  TSCAutoCompleted 중복 보고 방지
				if (isTSCAutoCompletedReported == false) {
					sendS6F11_TSC(OperationConstant.TSC_AUTO_COMPLETED, 0);
					isTSCAutoCompletedReported = true;
				}
			} else {
				isTSCPauseInitiatedReported = false;
				isTSCAutoCompletedReported = false;
			}
			
			if (ocsInfoManager.getTscState() == TSC_STATE.TSC_PAUSING && isAllVehicleStop == false) {
				if (checkAllVehicleStop()) {
					isAllVehicleStop = true;
					ocsInfoManager.addOCSInfoToOCSInfoUpdateList(TSCSTATUS, TSC_STATE.TSC_PAUSED.toConstString());
					
					// Report (TSCPauseCompleted) Msg to MCS
					// 2011.10.12 by PMM OCSINFO update 지연으로  TSCAutoCompleted 중복 보고 방지
					if (isTSCPauseCompletedReported == false) {
						sendS6F11_TSC(OperationConstant.TSC_PAUSE_COMPLETED, 0);
						isTSCPauseCompletedReported = true;
					}
				}
			} else {
				isTSCPauseCompletedReported = false;
			}
		} else {
			isTSCPauseInitiatedReported = false;
			isTSCAutoCompletedReported = false;
			isTSCPauseCompletedReported = false;
		}
	}
	
	/**
	 * 초기 구동시 모든 Vehicle과 통신이 완료되어 Operation Instance가 초기화 되었으면 Operation 서비스를 실행한다.
	 * 한대라도 통신이 되고있지 않으면 startTimeout동안 대기 후 Operation 서비스를 실행한다.
	 */
	private void manageAllOperationReady() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				if (isAllOperationReady == false) {
					if (checkAllOperationReadyCompleted() || ((System.currentTimeMillis() - startTime) >= START_TIMEOUT)) {
						isAllOperationReady = true;
						Operation operation = null;
						for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
							operation = e.nextElement();
							if (operation != null) {
								operation.setAllOperationReady(isAllOperationReady);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("manageAllOperationReady()", e);
		}
	}
	
	/**
	 * 모든 Vehicle과 통신이 완료되어 Operation Instance가 초기화되었는지 확인한다.
	 * 
	 * @return
	 */
	private boolean checkAllOperationReadyCompleted() {
		try {
			Operation operation = null;
			boolean result = true;
			int notReadyCount = 0;
			StringBuffer notReadyVehicleList = new StringBuffer();
			for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
				operation = e.nextElement();
				if (operation != null) {
					if (operation.isOperationReady() == false) {
						result = false;
						notReadyCount++;
						notReadyVehicleList.append(operation.getVehicleData().getVehicleId());
						notReadyVehicleList.append("/");
					}
				}
			}
			if (result == false && this.serviceState == MODULE_STATE.INSERVICE) {
				traceOperation(notReadyCount + " vehicle(s) are not ready.");
				if (notReadyCount <= 10) traceOperation(notReadyCount + " Not Ready Vehicle List:" + notReadyVehicleList.toString());
			}
			return result;
		} catch (Exception e) {
			traceOperationException("checkAllOperationReadyCompleted()", e);
			return false;
		}
	}
	
	private void checkFailoverCompleted() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				if (isFailoverCompleted == false) {
					if (checkAllOperationStarted()) {
						Operation operation = null;
						for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
							operation = e.nextElement();
							if (operation != null) {
								operation.setFailoverCompleted(true);
							}
						}
						isFailoverCompleted = true;
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("checkFailoverCompleted()", e);
		}
	}
	
	private boolean checkAllOperationStarted() {
		try {
			Operation operation = null;
			for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
				operation = e.nextElement();
				if (operation != null) {
					if (operation.isOperationStarted() == false) {
						return false;
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("checkAllOperationStarted()", e);
		}
		return true;
	}
	
	// 2011.12.27 by PMM
	/**
	 * Deadlock Detection
	 */
	private void manageDeadlock() {
		try {
			if (isDeadlockCheckNeeded) {
				// 2012.01.10 by PMM
				// if (this.serviceState == MODULE_STATE.INSERVICE) {
				// 2014.08.13 by zzang9un : 근접제어 Deadlock 처리 되도록 수정
				//if (isNearByDrive == false && this.serviceState == MODULE_STATE.INSERVICE) {
				if (this.serviceState == MODULE_STATE.INSERVICE) {
					// 약 10초 주기로 체크
					if (isAllOperationReady) {
						// All VHL이 Ready 상태 이후, INSERVICE Host에서만 Deadlock Break가 동작해야 함.
						long deadlockDetectStartedTime = System.nanoTime();
						
						@SuppressWarnings("unchecked")
						ArrayList<VehicleData> driveFailVehicleListClone = (ArrayList<VehicleData>)(driveFailedVehicleList.clone());
						if (driveFailVehicleListClone.size() > 0) {
							// DriveFailed VHL이 있어야 Deadlock Detection 동작.
							VehicleData driveFailedVehicle = null;
							
							// Deadlock이 감지된 전체 VHL List 초기화
							deadlockBrokenVehicleList.clear();
							
							// 2014.09.30 by zzang9un : Deadlock이 감지된 Loop를 저장하는 임시 변수
							ArrayList<String> currentDeadlockDetectedLoopList;
							currentDeadlockDetectedLoopList = new ArrayList<String>();
														
							for (int i = 0; i < driveFailVehicleListClone.size(); i++) {
								// Cycle (Strongly Connected Component)을 확인하기 위한 Depth 기반 검색 리스트 초기화
								deadlockCheckedMap.clear();
								// 개별 Deadlock이 감지된  VHL List 초기화
								deadlockDetectedVehicleList.clear();
								
								driveFailedVehicle = driveFailVehicleListClone.get(i);
								if (driveFailedVehicle != null &&
										deadlockBrokenVehicleList.contains(driveFailedVehicle) == false) {
									
									// Depth First Search
									if (checkDeadlockRecursive(driveFailedVehicle, driveFailedVehicle, 0)) {
										if (deadlockDetectedVehicleList.contains(driveFailedVehicle) == false) {
											deadlockDetectedVehicleList.add(driveFailedVehicle);
										}
										
										// 개별 Deadlock에 대해 Deadlock Break
										breakDetectedDeadlock();
										
										// 2014.09.30 by zzang9un : 감지된 DeadlockDetectedVehicleList를 Map에 추가
										// 이미 있는 Deadlock Loop인 경우 경과 시간을 보고 알람 메세지 발생
										if (deadlockDetectedVehicleList.size() > 0) {
											String deadlockDetectedLoopKey = null;
											deadlockDetectedLoopKey = makeDeadlockAlarmText(deadlockDetectedVehicleList);
											
											if (deadlockDetectedLoopKey != null) {
												currentDeadlockDetectedLoopList.add(deadlockDetectedLoopKey);
											}
										}
									} 
								}
							}
							
							registerDeadlockAlarm(currentDeadlockDetectedLoopList);
						}
						
						// 2012.03.13 by PMM
//						traceDeadlockBreak("DeadlockDetectionTime: " + (System.nanoTime() - deadlockDetectStartedTime)/1000000 + "(ms)");
						double elapsedTime = ((double)(System.nanoTime() - deadlockDetectStartedTime)/1000000);
						if (elapsedTime > 10) {
							traceDeadlockBreak("DeadlockDetectionTime: " + elapsedTime + "(ms) (over 10 ms), DriveFailedVehicle:" + driveFailVehicleListClone.size());
						}
					}
				}
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			// Known Exception.
		} catch (Exception e) {
			traceOperationException("manageDeadlock()", e);
		}
	}
	
	// Depth First Search를 위한 Recursive Check Method.
	private boolean checkDeadlockRecursive(VehicleData driveFailedVehicle, VehicleData checkVehicle, int depth) {
		// 2014.08.13 by zzang9un : 근접제어 Deadlock 처리 되도록 수정
		//assert isNearByDrive == false;
		
		assert this.serviceState == MODULE_STATE.INSERVICE;
		assert isAllOperationReady;
		assert driveFailedVehicle != null;
		assert driveFailedVehicleList != null;
		
		try {
			if (depth > driveFailedVehicleList.size()) {
				// 무한 Loop 방지 안전 코드
				return false;
			}
			
			boolean result = false;
			if (checkVehicle != null) {
				if (deadlockCheckedMap.containsKey(checkVehicle)) {
					if (deadlockCheckedMap.get(checkVehicle) != depth) {
						// Cycle (Strongly Connected Component) 확인.
						return true;
					}
				} else {
					deadlockCheckedMap.put(checkVehicle, depth);
				}
				
				@SuppressWarnings("unchecked")
				ArrayList<VehicleData> driveFailCausedVehicleListClone = (ArrayList<VehicleData>)(checkVehicle.getDriveFailCausedVehicleList().clone());
				
				// 2014.08.26 by zzang9un : 대차 감지된 vehicle을 causedvehicle에 추가해서 deadlock을 감지하도록 수정
				// driveFailCausedVehicleList가 없지만 대차 감지된 vehicle list가 있는 경우
				// 앞차량을 driveFailCausedVehicleListClone에 추가 		
				// 2014.09.15 by zzang9un : 근접제어만 대차 감지를 체크하도록 수정
				if (this.isNearByDrive) {
					if ((driveFailCausedVehicleListClone.size() <= 0) && (checkVehicle.getReason().isEmpty())) {
						if (checkVehicle.getDetectedCausedVehicleList().size() > 0) {
							driveFailCausedVehicleListClone.add(checkVehicle.getDetectedCausedVehicleList().get(0));
						}
					}
				}
				
				VehicleData driveFailCausedVehicle = null;
				
				depth++;
				for (int i = 0; i < driveFailCausedVehicleListClone.size(); i++) {
					driveFailCausedVehicle = driveFailCausedVehicleListClone.get(i);
					if (driveFailCausedVehicle != null &&
							deadlockBrokenVehicleList.contains(driveFailCausedVehicle) == false) {
						StringBuffer message = new StringBuffer();
						message.append("[").append(String.format("%02d", depth)).append("] ");
						message.append("StartVehicle:").append(driveFailedVehicle.getVehicleId());
						message.append(" - CheckVehicle:").append(checkVehicle.getVehicleId());
						message.append(", CausedVehicle:").append(driveFailCausedVehicle.getVehicleId());
						message.append("[").append(driveFailCausedVehicle.getState()).append("] ");
						
						message.append("- ").append(checkVehicle.getVehicleId()).append(" ");
						message.append(checkVehicle.getReason());
						
						// 2014.08.26 : 대차감지인 경우 driveFailed가 아니므로 조건 추가
						//if (driveFailCausedVehicle.isDriveFailed()) {						
						if (driveFailCausedVehicle.isDriveFailed() || driveFailCausedVehicle.getDetectedCausedVehicleList().size() > 0) {								
							if (checkDeadlockRecursive(driveFailedVehicle, driveFailCausedVehicle, depth)) {
								if (deadlockDetectedVehicleList.contains(driveFailCausedVehicle) == false) {
									deadlockDetectedVehicleList.add(driveFailCausedVehicle);
								}
								if (deadlockBrokenVehicleList.contains(driveFailCausedVehicle) == false) {
									deadlockBrokenVehicleList.add(driveFailCausedVehicle);
								}
								result = true;
								traceDeadlockBreak(message.toString());
							}
						}
					}
				}
				deadlockCheckedMap.remove(checkVehicle);
			}
			return result;
		} catch (ArrayIndexOutOfBoundsException e) {
			// Known Exception.
			traceOperationException("checkDeadlockRecursive()", e);
		} catch (Exception e) {
			traceOperationException("checkDeadlockRecursive()", e);
		}
		return false;
	}
	
	/**
	 * @deprecated 다른 함수로 대체 사용
	 * @see registerDeadlockAlarm()
	 */
	private void registerDeadlockDetected() {
		assert isNearByDrive == false;
		assert this.serviceState == MODULE_STATE.INSERVICE;
		assert isAllOperationReady;
		assert deadlockDetectedVehicleList != null;

		try {
			VehicleData deadlockDetectedVehicle = null;
			if (deadlockDetectedVehicleList.size() > 2) {
				long deadlockDetectedTimeout = ocsInfoManager.getDeadlockDetectedTimeout();
				for (int i = 0; i < deadlockDetectedVehicleList.size(); i++) {
					deadlockDetectedVehicle = deadlockDetectedVehicleList.get(i);
					if (deadlockDetectedVehicle != null) {
						if (deadlockDetectedVehicle.getStopNode().equals(deadlockDetectedVehicle.getCurrNode()) == false) {
							return;
						}
						if (deadlockDetectedVehicle.getState() == 'U' || deadlockDetectedVehicle.getState() == 'L' || deadlockDetectedVehicle.getState() == 'G') {
							return;
						}
						
						//ocsInfoManager.getDeadlockDetectedTimeout(): 120000 (120sec)
						if (System.currentTimeMillis() - deadlockDetectedVehicle.getStateChangedTime() < deadlockDetectedTimeout) {
							return;
						}
					}
				}
				StringBuffer message = new StringBuffer();
				message.append("Deadlock Detected. ");
				for (int i = 0; i < deadlockDetectedVehicleList.size(); i++) {
					deadlockDetectedVehicle = deadlockDetectedVehicleList.get(i);
					if (deadlockDetectedVehicle != null) {
						if (i > 0) {
							message.append(",");
						}
						if (i < 3) {
							message.append(deadlockDetectedVehicle.getVehicleId());
						} else {
							message.append("...");
							break;
						}
					}
				}
				registerAlarmText(message.toString());
			}
		} catch (Exception e) {
			traceOperationException("registerDeadlockDetected()", e);
		}
	}

	// Request for Breaking Detected Deadlock
	private void breakDetectedDeadlock() {
		// 2014.08.13 by zzang9un : 근접제어 Deadlock 처리 되도록 수정
		//assert isNearByDrive == false;
		
		assert this.serviceState == MODULE_STATE.INSERVICE;
		assert isAllOperationReady;
		assert deadlockDetectedVehicleList != null;

		try {
			VehicleData deadlockDetectedVehicle = null;
			Node currNode;
			StringBuffer message = new StringBuffer();
			message.append("Deadlock Break:");
			for (int i = 0; i < deadlockDetectedVehicleList.size(); i++) {
				deadlockDetectedVehicle = deadlockDetectedVehicleList.get(i);
				if (deadlockDetectedVehicle != null) {
					currNode = deadlockDetectedVehicle.getDriveCurrNode();
					message.append(" [");
					if (deadlockDetectedVehicle.getReason() != null &&
							deadlockDetectedVehicle.getReason().indexOf("Hid") >= 0) {
						// HID Capacity Full로 인한 Deadlock.
						// Capacity 초과하여 진입 허용.
						deadlockDetectedVehicle.setDeadlockType(DEADLOCK_TYPE.HID);
						message.append("(KEY:HID)");
					} else if (deadlockDetectedVehicle.getReason() != null &&
							deadlockDetectedVehicle.getReason().indexOf("Reserved") >= 0) {
						deadlockDetectedVehicle.setDeadlockType(DEADLOCK_TYPE.RESERVED);
						message.append("(KEY:RESERVE)");
					} else if (currNode != null && currNode.isDiverge()) {
						// 분기 노드에 위치한 VHL을 DriveFailed Node가 아닌 Node로 우회 탐색 요청.
						deadlockDetectedVehicle.setDeadlockType(DEADLOCK_TYPE.NODE);
						message.append("(KEY:NODE)");
					}
					message.append(deadlockDetectedVehicle.getVehicleId()).append(":");
					message.append(deadlockDetectedVehicle.getCurrNode()).append("]");
				}
			}
			traceDeadlockBreak(message.toString());
			
			// 2014.09.29 by zzang9un : deadlock 알람 메세지 등록 함수 새로 구현한 것으로 대체
			//registerDeadlockDetected();
		} catch (Exception e) {
			traceOperationException("breakDetectedDeadlock()", e);
		}
	}
	
	/**
	 * Manage ServiceState 
	 */
	private void manageServiceState() {
		try {
			if (this.requestedServiceState == MODULE_STATE.REQOUTOFSERVICE
					&& this.serviceState != MODULE_STATE.OUTOFSERVICE) {
				outOfService();
				
				// 2011.11.04 by PMM
				alarmManager.unregisterAllAlarm(OPERATION);
			} else if (this.requestedServiceState == MODULE_STATE.REQINSERVICE
					&& this.serviceState != MODULE_STATE.INSERVICE) {
				inService();
				
				// 2011.11.04 by PMM
				alarmManager.unregisterAllAlarm(OPERATION);
			}
			
			if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
				if (++outOfServiceCheckCount >= 5) {
					traceOperation("      [OUTOFSERVICE]");
					outOfServiceCheckCount = 0;
				}
			}
		} catch (Exception e) {
			traceOperationException("manageServiceState()", e);
		}
	}
	
	/**
	 * Make INSERVICE
	 */
	private void inService() {
		traceOperation("   FailOver Start. [" + this.serviceState.toConstString() + "][INSERVICE]");
		
		Operation operation = null;
		int checkServiceCount = 0;
		int currSerivceCount = managerMap.size();
		
		traceOperation("   Request to Change Manager ServiceState (INSERVICE).");
		// 1. Manager의 ServiceState를 INSERVICE로 변경한다.
		if (setManagerServiceState(MODULE_STATE.REQINSERVICE) == false) {
			traceOperation("      Failed to Change Manager ServiceState (INSERVICE).");
			return;
		}
		traceOperation("   Manager ServiceState Changed (INSERVICE).");
		
		// 2. 모든 Operation Thread에게 INSERVICE를 요청한다.
		for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
			operation = e.nextElement();
			if (operation != null) {
				operation.setRequestedServiceState(MODULE_STATE.INSERVICE);
				operation.setAllOperationReady(false);
				operation.setFailoverCompleted(false);
			}
		}
		
		// 3. 모든 Operation Thread의 INSERVICE인지 확인한다.
		boolean check = true;
		while (check) {
			checkServiceCount = 0;
			for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
				operation = e.nextElement();
				if (operation != null) {
					if (operation.getServiceState() == MODULE_STATE.INSERVICE) {
						checkServiceCount++;
					}
				}
			}
			if (checkServiceCount < currSerivceCount) {
				try {
					sleep(3);
				} catch (Exception e) {
					
				}
			} else {
				check = false;
			}
		}
		
		// 2015.01.28 by MYM : 장애 지역 우회 기능
		detourControlManager.requestUpdateSectionToDB();
		
		// 4. Block 정보를 DB에 저장한다.
		// 2014.10.15 by KYK : blockManager 의 update 와 동시참조 이슈발생, 요청방식으로 변경
//		// 2011.11.07 by PMM
//		blockInfoManager.insertBlockInfoToDB();
		blockInfoManager.requestBlockUpdateToDB();
		
		// 2015.04.20 by MYM : CloseLoop Validation 체크는 요청에 의해서 실행하도록 변경		
		closeLoopManager.requestValidationCheck();
		
		// 5. SERVICE_STATE를 INSERVICE로 변경한다.
		this.serviceState = MODULE_STATE.INSERVICE;
		isAllOperationReady = false;
		isFailoverCompleted = false;
		startTime = System.currentTimeMillis();
		traceOperation("   FailOver End.   [" + this.serviceState.toConstString() + "][INSERVICE]");
	}
	
	/**
	 * Make OUTOFSERVICE
	 */
	private void outOfService() {
		traceOperation("   FailOver Start. [" + this.serviceState.toConstString() + "][OUTOFSERVICE]");
		Operation operation = null;
		int checkServiceCount = 0;
		int currSerivceCount = managerMap.size();
		
		long startTime = System.currentTimeMillis();
		long elapseTime = 0;
		// 1. 모든 Operation Thread에게 OUTOFSERVICE를 요청한다.
		for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
			operation = e.nextElement();
			if (operation != null) {
				operation.setRequestedServiceState(MODULE_STATE.OUTOFSERVICE);
			}
		}
		
//		// 2. 모든 Operation Thread의 OUTOFSERVICE인지 확인한다.
//		for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
//			operation = e.nextElement();
//			if (operation.getServiceState() == MODULE_STATE.OUTOFSERVICE) {
//				checkServiceCount++;
//			}
//		}
//		
//		// 3. 모든 OperationThread가 OUTOFSERVICE 완료인지 확인
//		if (checkServiceCount < currSerivceCount) {
//			return;
//		}
//		
//		// 4. 주요 Manager(VehicleManager, TrCmdManager)의 DB 업데이트 완료 확인
//		if (isNothingUpdateToDB() == false) {
//			return;
//		}
		
		// 2. 모든 Operation Thread의 OUTOFSERVICE인지 확인한다.
		boolean check = true;
		ArrayList<String> stillInServiceList = new ArrayList<String>();
		VehicleData tempVehicleData = null;
		while (check) {
			checkServiceCount = 0;
			currSerivceCount = managerMap.size();
			stillInServiceList.clear();
			for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
				operation = e.nextElement();
				if (operation != null) {
					if (operation.getServiceState() == MODULE_STATE.OUTOFSERVICE) {
						checkServiceCount++;
					} else {
						tempVehicleData = operation.getVehicleData();
						if (tempVehicleData != null) {
							stillInServiceList.add(tempVehicleData.getVehicleId());
						}
						operation.setRequestedServiceState(MODULE_STATE.OUTOFSERVICE);
					}
				}
			}
			// 3. 모든 OperationThread가 OUTOFSERVICE 완료인지 확인
			// 4. 주요 Manager(VehicleManager, TrCmdManager)의 DB 업데이트 완료 확인
			
			if (checkServiceCount < currSerivceCount) {
				try {
					if (stillInServiceList.size() < 10) {
						traceOperation("Still InService: " + stillInServiceList.toString());
					} else {
						traceOperation("Still InService: " + stillInServiceList.size());
					}
					elapseTime = System.currentTimeMillis() - startTime;
					if (elapseTime > 5000) {
						traceOperation("   FailOver Failed.   [" + this.serviceState.toConstString() + "][OUTOFSERVICE] [" + elapseTime + " (msec)]");
						return;
					}
					sleep(3);
				} catch (Exception e) {
					traceException(e);
				}
			} else {
				check = false;
			}
			
			try {
				elapseTime = System.currentTimeMillis() - startTime;
				if (elapseTime > 10000) {
					traceOperation("   isNothingUpdateToDB() is skipped. ElapseTime:" + elapseTime + " (msec)]");
					break;
				}
				if (isNothingUpdateToDB() == false) {
					traceOperation("   FailOver Failed.   [" + this.serviceState.toConstString() + "][OUTOFSERVICE] - isNothingUpdateToDB() is false.");
					return;
				}
			} catch (Exception e) {
				traceException(e);
			}
		}
		
		// 5. 모든 Operation Thread가 OUTOFSERVICE이면 SERVICE_STATE를 OUTOFSERVICE로 변경한다.
		this.serviceState = MODULE_STATE.OUTOFSERVICE;
		traceOperation("   FailOver End.   [" + this.serviceState.toConstString() + "][OUTOFSERVICE]");
		
		// 6. Manager의 ServiceState를 OUTOFSERVICE로 변경한다.
		setManagerServiceState(MODULE_STATE.REQOUTOFSERVICE);
		
		isAbnormalStateChanged = false;
		previousAbnormalVehicleList.clear();
		previousAbnormalHidList.clear();
		nodeManager.resetEnableChangedNodeList();
		// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//		abnormalVehiclesCollisionNodeSet.clear();
		abnormalVehiclesOnCollisionMap.clear();
	}
	
	/**
	 * Manage RuntimeUpdate of Operation Module
	 */
	private void manageRuntimeUpdate() {
		assert ocsInfoManager != null;
		
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			if (ocsInfoManager.getOperationUpdate() == RUNTIME_UPDATE.NO) {
				// 2012.04.18 by PMM
				// RuntimeUpdate 요청 후, SystemPause 요청에 대한 Cancel을 위해.
				if (isSystemPauseRequested) {
					manageSystemPaused(false);
					unregisterAlarmText(SYSTEM_PAUSE_REQUESTED);
					
					// 아래 알람은 사용자가 제거해야 함.
//					registerAlarmText(RUNTIMEUPDATE_CANCELED);
					registerAlarmTextWithLevel(RUNTIMEUPDATE_CANCELED, ALARMLEVEL.INFORMATION);
					
					traceRuntimeUpdate(RUNTIMEUPDATE_CANCELED);
					
					traceRuntimeUpdate(SYSTEM_PAUSE_RESUMED);
					
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_CANCELED, 
							"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
				}
			} else if (ocsInfoManager.getOperationUpdate() == RUNTIME_UPDATE.YES) {
				// OPERATIONUPDATE, OPERATIONPORTUPDATE 둘다 YES면 OPERATIONUPDATE 진행
				if (isSystemPauseRequested) {
					if (checkAllVehicleStop()) {
						// 2011.11.04 by PMM
						unregisterAlarmText(SYSTEM_PAUSE_REQUESTED);
						isAllVehicleStop = true;
						
//						registerAlarmText(SYSTEM_PAUSED);
						registerAlarmTextWithLevel(SYSTEM_PAUSED, ALARMLEVEL.WARNING);
						traceRuntimeUpdate(SYSTEM_PAUSED);
						
//						registerAlarmText(RUNTIMEUPDATE_START_INSERVICE);
						registerAlarmTextWithLevel(RUNTIMEUPDATE_START_INSERVICE, ALARMLEVEL.INFORMATION);
						traceRuntimeUpdate(RUNTIMEUPDATE_START_INSERVICE);
						
						// 2011.11.21 by PMM
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_START_INSERVICE, 
								"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
						
						// Manager Update!
						updateManager();
						
						unregisterAlarmText(RUNTIMEUPDATE_START_INSERVICE);
						traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_INSERVICE);
						
						// 2011.11.21 by PMM
						registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_COMPLETED_INSERVICE, 
								"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
						
						setAllOperationInitForRuntimeUpdate();
						traceRuntimeUpdate(ALL_VEHICLE_INITIATED);
						
						manageSystemPaused(false);
						unregisterAlarmText(SYSTEM_PAUSED);
						traceRuntimeUpdate(SYSTEM_PAUSE_RESUMED);
						ocsInfoManager.setOperationUpdate(RUNTIME_UPDATE.DONE);
						ocsInfoManager.setOperationPortUpdate(RUNTIME_UPDATE.NO);
					} else {
						// 2012.04.18 by PMM
						// RuntimeUpdate 중 SystemPause에 대한 요청에 대한 Timeout 추가 필요.
						// VHL이 작업 중 AV로 인해 AllVehicleStop이 안된 케이스 발생함.
//						if (isSystemPauseRequested == false) {
//							registerAlarmText(SYSTEM_PAUSE_REQUESTED);
//							traceRuntimeUpdate(SYSTEM_PAUSE_REQUESTED);
//							manageSystemPaused(true);
//						}
						if (System.currentTimeMillis() - systemPauseRequestedTime > ocsInfoManager.getSystemPauseRequestTimeout()) {
							unregisterAlarmText(SYSTEM_PAUSE_REQUESTED);
//								registerAlarmText(SYSTEM_PAUSE_REQUEST_TIMEOUT);
							registerAlarmTextWithLevel(SYSTEM_PAUSE_REQUEST_TIMEOUT, ALARMLEVEL.WARNING);
							
							traceRuntimeUpdate(SYSTEM_PAUSE_REQUEST_TIMEOUT);
//								registerAlarmText(RUNTIMEUPDATE_FAILED);
							registerAlarmTextWithLevel(RUNTIMEUPDATE_FAILED, ALARMLEVEL.WARNING);
							
							traceRuntimeUpdate(RUNTIMEUPDATE_FAILED);
							
							manageSystemPaused(false);
							
							traceRuntimeUpdate(SYSTEM_PAUSE_RESUMED);
							ocsInfoManager.setOperationUpdate(RUNTIME_UPDATE.NO);
							ocsInfoManager.setOperationPortUpdate(RUNTIME_UPDATE.NO);
							
							registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_FAILED, 
									"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
						}
					}
				} else {
					manageSystemPaused(true);
					registerAlarmTextWithLevel(SYSTEM_PAUSE_REQUESTED, ALARMLEVEL.WARNING);
					traceRuntimeUpdate(SYSTEM_PAUSE_REQUESTED);
				}
			} else if (ocsInfoManager.getOperationUpdate() != RUNTIME_UPDATE.DONE) {
				ocsInfoManager.setOperationUpdate(RUNTIME_UPDATE.NO);
				ocsInfoManager.setOperationPortUpdate(RUNTIME_UPDATE.NO);
			}
			
			// 2017.10.24 by LSH : Port ID 변경용 Runtime Update 기능
			if (ocsInfoManager.getOperationPortUpdate() == RUNTIME_UPDATE.YES
					&& (ocsInfoManager.getOperationUpdate() == RUNTIME_UPDATE.NO || ocsInfoManager.getOperationUpdate() == RUNTIME_UPDATE.DONE)) {
				registerAlarmTextWithLevel(RUNTIMEPORTUPDATE_START_INSERVICE, ALARMLEVEL.INFORMATION);
				traceRuntimeUpdate(RUNTIMEPORTUPDATE_START_INSERVICE);

				registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_PORT_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEPORTUPDATE_START_INSERVICE, 
						"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.RUNTIME_PORT_UPDATE), false);

				// Manager Update!
				updatePortManager();
				unregisterAlarmText(RUNTIMEPORTUPDATE_START_INSERVICE);

				traceRuntimeUpdate(RUNTIMEPORTUPDATE_COMPLETED_INSERVICE);

				registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_PORT_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEPORTUPDATE_COMPLETED_INSERVICE, 
						"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.RUNTIME_PORT_UPDATE), false);

				ocsInfoManager.setOperationPortUpdate(RUNTIME_UPDATE.DONE);
			} else if (ocsInfoManager.getOperationPortUpdate() == RUNTIME_UPDATE.NO) {
				// nothing
			} else if (ocsInfoManager.getOperationPortUpdate() != RUNTIME_UPDATE.DONE) {
				ocsInfoManager.setOperationPortUpdate(RUNTIME_UPDATE.NO);
			}
			
		} else {
			// 2017.10.24 by LSH : Port ID 변경용 Runtime Update 기능
//			if (ocsInfoManager.getOperationUpdate() == RUNTIME_UPDATE.DONE) {
			if (ocsInfoManager.getOperationUpdate() == RUNTIME_UPDATE.DONE || ocsInfoManager.getOperationPortUpdate() == RUNTIME_UPDATE.DONE) {
//				registerAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
				registerAlarmTextWithLevel(RUNTIMEUPDATE_START_OUTOFSERVICE, ALARMLEVEL.INFORMATION);
				traceRuntimeUpdate(RUNTIMEUPDATE_START_OUTOFSERVICE);
				
				// 2011.11.21 by PMM
				registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_START_OUTOFSERVICE, 
						"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
				
				updateManager();
				
				unregisterAlarmText(RUNTIMEUPDATE_START_OUTOFSERVICE);
				traceRuntimeUpdate(RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE);
				ocsInfoManager.setOperationUpdate(RUNTIME_UPDATE.NO);
				ocsInfoManager.setOperationPortUpdate(RUNTIME_UPDATE.NO);
				
				// 2011.11.21 by PMM
				registerEventHistory(new EventHistory(EVENTHISTORY_NAME.RUNTIME_UPDATE, EVENTHISTORY_TYPE.SYSTEM, "", RUNTIMEUPDATE_COMPLETED_OUTOFSERVICE, 
						"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.RUNTIME_UPDATE), false);
			}
		}
	}
	
	/**
	 * RuntimeUpdate Managers
	 */
	private void updateManager() {
		assert hidManager != null;
		assert nodeManager != null;
		assert sectionManager != null;
		assert collisionManager != null;
		assert carrierLocManager != null;
		assert stbCarrierLocManager != null;
		assert blockInfoManager != null;
		assert zoneControlManager != null;
		assert vehicleErrorManager != null;
		
		try {
			// 2015.04.09 by MYM : RuntimeUpdate시 관련 Manager만 Service State를 변경
//			setManagerServiceState(MODULE_STATE.REQOUTOFSERVICE);
			setManagerServiceStateForRuntimeUpdate(MODULE_STATE.REQOUTOFSERVICE);
			
//			registerAlarmText(INITIALIZING_HIDMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_HIDMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_HIDMANAGER);
			hidManager.initializeFromDB();		// data reset.
			unregisterAlarmText(INITIALIZING_HIDMANAGER);
			
//			registerAlarmText(INITIALIZING_NODEMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_NODEMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_NODEMANAGER);
			// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
			this.abnormalVehiclesOnCollisionMap.clear();
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
			
			// 2013.09.25 by MYM : CloseLoop 추가
//			registerAlarmText(INITIALIZING_CLOSELOOPMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_CLOSELOOPMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_CLOSELOOPMANAGER);
			closeLoopManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_CLOSELOOPMANAGER);
			
//			registerAlarmText(INITIALIZING_CARRIERLOCMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_CARRIERLOCMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_CARRIERLOCMANAGER);
			carrierLocManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_CARRIERLOCMANAGER);
			
			// 2011.11.04 by PMM
//			registerAlarmText(INITIALIZING_STBCARRIERLOCMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_STBCARRIERLOCMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_STBCARRIERLOCMANAGER);
			stbCarrierLocManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_STBCARRIERLOCMANAGER);
			
//			registerAlarmText(INITIALIZING_BLOCKINFOMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_BLOCKINFOMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_BLOCKINFOMANAGER);
			blockInfoManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_BLOCKINFOMANAGER);

//			registerAlarmText(INITIALIZING_ZONECONTROLMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_ZONECONTROLMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_ZONECONTROLMANAGER);
			zoneControlManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_ZONECONTROLMANAGER);
			
//			registerAlarmText(INITIALIZING_VEHICLEERRORMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_VEHICLEERRORMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_VEHICLEERRORMANAGER);
			vehicleErrorManager.initializeFromDB();	// data reset.
			unregisterAlarmText(INITIALIZING_VEHICLEERRORMANAGER);
			
			// 2013.02.15 by KYK
			registerAlarmTextWithLevel(INITIALIZING_STATIONMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_STATIONMANAGER);
			stationManager.initializeFromDB(); // data reset.
			unregisterAlarmText(INITIALIZING_STATIONMANAGER);
			
			// 2015.02.09 by zzang9un : PassDoorManager 추가
//			registerAlarmText(INITIALIZING_PASSDOORMANAGER);
//			traceRuntimeUpdate("   " + INITIALIZING_PASSDOORMANAGER);
//			passDoorManager.initializeFromDB(); // data reset.
//			unregisterAlarmText(INITIALIZING_PASSDOORMANAGER);
			
			// 2015.07.01 by MYM : CarrierTypeConfig Reload
			CarrierTypeConfig.getInstance().loadCarrierTypeConfig();
			
			// 2015.09.21 by MYM : RuntimeUpdate시 Traffic정보 재 갱신
			trafficManager.requestRuntimeUpdate();

			// 2015.02.06 by MYM : INSERVICE인 경우에만 INSERVICE 변경 요청(RuntimeUpdate 후 Manager가 OUTOFSERVICE로 남아 있는 문제가 있음)
			// Manager는 REQOUTOFSERVICE 요청 -> Manager 쓰레드에서 OUTOFSERIVCE로 변경
			//           REQINSERVICE 요청 -> Manager 쓰레드에서 INSERVICE로 변경 처리됨
//			setManagerServiceState(this.serviceState);
			// 2011.11.07 by PMM
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				// 2015.04.09 by MYM : RuntimeUpdate시 관련 Manager만 Service State를 변경
//				setManagerServiceState(MODULE_STATE.REQINSERVICE);
				setManagerServiceStateForRuntimeUpdate(MODULE_STATE.REQINSERVICE);
				
				// 2015.01.28 by MYM : 장애 지역 우회 기능
				detourControlManager.requestUpdateSectionToDB();

				// 2014.10.15 by KYK : blockManager 의 update 와 동시참조 이슈발생, 요청방식으로 변경
//				blockInfoManager.insertBlockInfoToDB();
				blockInfoManager.requestBlockUpdateToDB();
				
				// 2015.04.20 by MYM : Runtime Update 중 CloseLoop Validation 체크가 늦어지는 경우가 발생
				//                     Runtime Update 완료시 체크하도록 변경
				closeLoopManager.requestValidationCheck();
			}
		} catch (Exception e) {
			traceRuntimeUpdate(RUNTIMEUPDATE_FAILED);
			traceOperationException("updateManager()", e);
		}
	}
	
	
	// 2017.10.24 by LSH : Port ID 변경용 Runtime Update 기능
	/**
	 * RuntimeUpdate Managers
	 */
	private void updatePortManager() {
		assert carrierLocManager != null;
		assert stbCarrierLocManager != null;
		
		boolean result = false;
		boolean materialResult = false; // 2022.04.20 by JJW Material 변경 기능 추가
		
		try {
			// 2017.10.24 by LSH : Port ID 변경 땐 Service State 변경 불필요 (non-down 변경)
						
//			registerAlarmText(INITIALIZING_CARRIERLOCMANAGER);
			registerAlarmTextWithLevel(INITIALIZING_CARRIERLOCMANAGER, ALARMLEVEL.INFORMATION);
			traceRuntimeUpdate("   " + INITIALIZING_CARRIERLOCMANAGER);
			result = carrierLocManager.changePortIDFromDB();	// data reset.
			
			materialResult = carrierLocManager.changePortMaterialFromDB(); // 2022.04.20 by JJW Material 변경 기능 추가
			
			if (result) {
				unregisterAlarmText(INITIALIZING_CARRIERLOCMANAGER);
				
				// 2011.11.04 by PMM
//				registerAlarmText(INITIALIZING_STBCARRIERLOCMANAGER);
				registerAlarmTextWithLevel(INITIALIZING_STBCARRIERLOCMANAGER, ALARMLEVEL.INFORMATION);
				traceRuntimeUpdate("   " + INITIALIZING_STBCARRIERLOCMANAGER);
				stbCarrierLocManager.changePortIDFromDB();	// data reset.
				unregisterAlarmText(INITIALIZING_STBCARRIERLOCMANAGER);
			} else if (materialResult) { // 2022.04.20 by JJW Material 변경 기능 추가
				unregisterAlarmText(INITIALIZING_CARRIERLOCMANAGER);
			} else {
				unregisterAlarmText(INITIALIZING_CARRIERLOCMANAGER);
				registerAlarmTextWithLevel(RUNTIMEPORTUPDATE_CANCELED, ALARMLEVEL.INFORMATION);
				traceRuntimeUpdate(RUNTIMEPORTUPDATE_CANCELED);
			}
		} catch (Exception e) {
			traceRuntimeUpdate(RUNTIMEPORTUPDATE_FAILED);
			traceOperationException("updatePortManager()", e);
		}
	}
	
	// 2011.10.28 by PMM
	// SystemPause 추가 (RuntimeUpdate)
	/**
	 * Manage SystemPaused
	 */
	private void manageSystemPaused(boolean isSystemPauseRequested) {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				Operation operation = null;
				
				for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
					operation = e.nextElement();
					if (operation != null) {
						operation.setSystemPaused(isSystemPauseRequested);
					}
				}
				this.isSystemPauseRequested = isSystemPauseRequested;
				
				if (isSystemPauseRequested) {
					systemPauseRequestedTime = System.currentTimeMillis();
				} else {
					isAllVehicleStop = false;
				}
			}
		} catch (Exception e) {
			traceOperationException("manageSystemPaused()", e);
		}
	}
	
	// 2011.10.28 by PMM
	// RuntimeUpdate 후 OperationInit
	private void setAllOperationInitForRuntimeUpdate() {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				Operation operation = null;
				
				for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
					operation = e.nextElement();
					if (operation != null) {
						operation.setOperationInitForRuntimeUpdate();
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("setAllOperationInit()", e);
		}
	}
	
	/**
	 * MCS로부터 PAUSE 명령 수신시 PAUSING 상태에서 모든 Vehicle이 정지하였는지 확인한다.
	 * ※ 모든 Vehicle이 정지하면 이후에 TSC_PAUSE를 보고함.
	 * @return
	 */
	private boolean checkAllVehicleStop() {
		try {
			Operation operation = null;
			VehicleData vehicleData = null;
			VehicleComm vehicleComm = null;
			VehicleCommData commData = null;
			for (Enumeration<Operation> e = managerMap.elements(); e.hasMoreElements();) {
				operation = e.nextElement();
				if (operation != null) {
					// 2011.10.28 by PMM
					// 장애 VHL은 정지한 것으로 간주
					vehicleData = operation.getVehicleData();
//					if (vehicleData != null &&
//							vehicleData.isAbnormalVehicle()) {
//						continue;
//					}
					
					// 2012.04.18 by PMM
					// VHL이 AV (AutoPosition or Auto Recovery) 인 경우, RuntimeUpdate가 진행이 안되는 케이스가 발생함.
					if (vehicleData != null) {
						if (vehicleData.isAbnormalVehicle() || vehicleData.getState() == 'V') {
							continue;
						}
					}
					
					// 2011.10.28 by PMM
					// vehicleComm NOT NULL 체크 추가.
					vehicleComm = operation.getVehicleComm();
					if (vehicleComm != null) {
						commData = vehicleComm.getVehicleCommData();
						if (commData != null && commData.getCurrCmd() != 0) {
							// 2011.10.28 by PMM
							// PauseType에 따른 예외 추가 (작업 중인 VHL을 제외)
							switch (commData.getState()) {
								case 'U':
								case 'L':
								{
									return false;
								}
								default:
								{
									if (commData.getPauseType() == 0) {
										return false;
									}
									break;
								}
							}
						}
					}
				}
			}
			return true;
		} catch (Exception e) {
			traceOperationException("checkAllVehicleStop()", e);
			return false;
		}
	}
	
	/**
	 * process NotAssigned TrCmd's ChangedRemoteCmd 
	 */
	private void processChangedRemoteCmdNotAssignedTrCmd() {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			Set<String> searchKeys;
			TrCmd trCmd;
		
			try {
				changedRemoteCmdRequestedNotAssignedTrCmdList = trCmdManager.getChangedRemoteCmdRequestedNotAssignedTrCmdList();
				searchKeys = new HashSet<String>(changedRemoteCmdRequestedNotAssignedTrCmdList.keySet());
				trCmd = null;
				for (String searchKey : searchKeys) {
					trCmd = (TrCmd)changedRemoteCmdRequestedNotAssignedTrCmdList.get(searchKey);
					if (trCmd != null) {
						switch (trCmd.getChangedRemoteCmd()) {
							case CANCEL:
								// NOT_ASSIGNED TrCmd에 대한 CANCEL 처리
								processCancel(trCmd);
								break;
							case REMOVE:
								processRemove(trCmd);
								break;
							case STAGECHANGE:
							case STAGEDELETE:
								processStageDelete(trCmd);
								break;
							case TRANSFERUPDATE: // 2021.01.21 by JJW
								processTransferUpdate(trCmd);
								break;
							default:
								traceOperationException("OperationManager #001 - NotAssigned TrCmd's ChangedRemoteCmd is NOT NULL!");
								break;
						}
					}
				}
				trCmdManager.deleteTrCmdFromDB();
			} catch (Exception e) {
				traceOperationException("processChangedRemoteCmdNotAssignedTrCmd()", e);
			}
		}
	}
	
	private void processCancel(TrCmd trCmd) {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			TrCompletionHistory trCompletionHistory;
			
			if (trCmd != null) {
				if (trCmd.getChangedRemoteCmd() == TRCMD_REMOTECMD.CANCEL) {
					if (trCmd.getState() == TRCMD_STATE.CMD_QUEUED || trCmd.getDetailState() == TRCMD_DETAILSTATE.NOT_ASSIGNED) {
						sendS6F11_TRCMD(OperationConstant.TRANSFER_CANCELINITIATED, 0, trCmd);
						
						trCmd.setRemoteCmd(TRCMD_REMOTECMD.CANCEL);
						trCmd.setState(TRCMD_STATE.CMD_CANCELED);
						trCmd.setDeletedTime(ocsInfoManager.getCurrDBTimeStr());
						
						trCompletionHistory = new TrCompletionHistory(trCmd, TRCMD_REMOTECMD.CANCEL.toConstString(), "");
						trCompletionHistoryManager.addTrCmdToRegisterTrCompletionHistory(trCompletionHistory);
						if (ocsInfoManager.isFormattedLogUsed()) {
							traceFormattedTrCompletionHistory(trCompletionHistory);
						}
						sendS6F11_TRCMD(OperationConstant.TRANSFER_CANCELCOMPLETED, 0, trCmd);
						traceOperation("Job Cancel: " + trCmd.getTrCmdId());
						trCmdManager.deleteTrCmdFromDB(trCmd.getTrCmdId());
					}
				}
			}
		}
	}
	
	private void processRemove(TrCmd trCmd) {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			TrCompletionHistory trCompletionHistory;
			
			if (trCmd != null) {
				if (trCmd.getChangedRemoteCmd() == TRCMD_REMOTECMD.REMOVE) {
					if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
						if (trCmd.getState() == TRCMD_STATE.CMD_QUEUED) {
							trCmd.setDeletedTime(ocsInfoManager.getCurrDBTimeStr());
							trCompletionHistory = new TrCompletionHistory(trCmd, trCmd.getRemoteCmd().toConstString(), "");
							trCompletionHistoryManager.addTrCmdToRegisterTrCompletionHistory(trCompletionHistory);
							
							if (ocsInfoManager.isFormattedLogUsed()) {
								traceFormattedTrCompletionHistory(trCompletionHistory);
							}
							resetChangedInfo(trCmd, trCmd.getChangedRemoteCmd().toConstString());
							trCmdManager.deleteTrCmdFromDB(trCmd.getTrCmdId());
							
							StringBuilder message = new StringBuilder();
							message.append("REMOVE Requested by LongRun(VIBRATION).");
							message.append(" TrCmdId:").append(trCmd.getTrCmdId());
							message.append(", RemoteCmd:").append(trCmd.getRemoteCmd().toConstString());
							message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
							message.append(", CarrierId:").append(trCmd.getCarrierId());
							
							traceOperation(message.toString());
						} else {
							traceOperationException("OperationManager #004 - RemoveRequested VIBRATION TrCmd's State is NOT CMD_QUEUED!");
						}
					} else {
						traceOperationException("OperationManager #005 - ChangedRemoteCmd is REMOVE, but RemoteCmd is NOT VIBARATION!");
					}
				}
			}
		}
	}
	
	private void processStageDelete(TrCmd trCmd) {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			String trCmdId;
			StringBuilder event;
			TrCompletionHistory trCompletionHistory;
			
			if (trCmd != null) {
				if (trCmd.getChangedRemoteCmd() == TRCMD_REMOTECMD.STAGECHANGE || trCmd.getChangedRemoteCmd() == TRCMD_REMOTECMD.STAGEDELETE) {
					if (trCmd.getState() == TRCMD_STATE.CMD_QUEUED || trCmd.getDetailState() == TRCMD_DETAILSTATE.NOT_ASSIGNED) {
						if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
							trCmdId = trCmd.getTrCmdId();
							event = new StringBuilder();
							event.append("TrCmdId:").append(trCmdId);
							event.append(", CarrierId:").append(trCmd.getCarrierId());
							event.append(", SourceLoc:").append(trCmd.getSourceLoc());
							event.append(", DestLoc:").append(trCmd.getDestLoc());
							registerEventHistory(new EventHistory(
									EVENTHISTORY_NAME.CURRENT_STAGE_DELETE, EVENTHISTORY_TYPE.SYSTEM, "",
									event.toString(), "", "", EVENTHISTORY_REMOTEID.OPERATION, "",
									EVENTHISTORY_REASON.STAGEDELETE), false);
							
							trCompletionHistory = new TrCompletionHistory(trCmd, REQUESTEDTYPE.STAGEDELETE.toConstString(), "");
							trCompletionHistoryManager.addTrCmdToRegisterTrCompletionHistory(trCompletionHistory);
							if (ocsInfoManager.isFormattedLogUsed()) {
								traceFormattedTrCompletionHistory(trCompletionHistory);
							}
							
							resetChangedInfo(trCmd, trCmd.getChangedRemoteCmd().toConstString());
							
							this.trCmdManager.deleteStageCmdFromDB(trCmdId);
						} else {
							traceOperationException("OperationManager #003 - STAGECHANGE/STAGEDELETE Requested TrCmd's RemoteCmd is NOT STAGE!");
						}
					}
				}
			}
		}
	}
	
	/**
	* @author : Jongwon Jung
	* @date : 2021. 1. 27.
	* @description : NotAssign Trcmd ChangedRemoteCmd 가 TRANSFERUPDATE 일 경우 현재 정보 Reset 및 S6F11 보고시 사용
	* @param trCmd
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 1. 27. Jongwon 최초 생성 */
	private void processTransferUpdate(TrCmd trCmd) {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			if (trCmd != null) {
				if (trCmd.getChangedRemoteCmd() == TRCMD_REMOTECMD.TRANSFERUPDATE) {
					if (trCmd.getState() == TRCMD_STATE.CMD_QUEUED || trCmd.getDetailState() == TRCMD_DETAILSTATE.NOT_ASSIGNED) {
						resetChangedInfo(trCmd, trCmd.getChangedRemoteCmd().toConstString());
						sendS6F11_TRCMD(OperationConstant.TRANSFER_UPDATECOMPLETED, 0, trCmd);
						StringBuffer log = new StringBuffer("Transfer Update");
						log.append("Command ID: " + trCmd.getTrCmdId());
						traceOperation(log.toString());
					}
				}
			}
		}
	}
	
	private void resetChangedInfo(TrCmd trCmd, String reason) {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			StringBuilder log;
			
			if (trCmd != null) {
				log = new StringBuilder("CHANGEDINFO_RESET");
				log.append(" by ").append(reason).append(".");
				log.append("(Requested:").append(trCmd.getChangedRemoteCmd()).append("/").append(trCmd.getChangedTrCmdId());
				log.append(", TrCmd:").append(trCmd.getRemoteCmd()).append("/").append(trCmd.getTrCmdId()).append("/").append(trCmd.getCarrierId());
				log.append("/").append(trCmd.getState()).append("/").append(trCmd.getDetailState()).append(")");
				traceOperation(log.toString());
				
				trCmd.setChangedRemoteCmd(TRCMD_REMOTECMD.NULL);
				trCmd.setChangedTrCmdId("");
				this.trCmdManager.resetChangedInfoFromDB(trCmd);
			}
		}
	}
	
	/**
	 * Update OperationTime to DB
	 */
	private String lastUpdateOperationTime = null; 
	private void updateOperationTime() {
		if (this.serviceState == MODULE_STATE.INSERVICE) {
			try {
				if(null==lastUpdateOperationTime){
					lastUpdateOperationTime = ocsInfoManager.getCurrDBTimeStr();
					ocsInfoManager.addOCSInfoToOCSInfoUpdateList(OPERATIONTIME, lastUpdateOperationTime);
				} else {
					if(false == lastUpdateOperationTime.equals(ocsInfoManager.getCurrDBTimeStr())) {
						lastUpdateOperationTime = ocsInfoManager.getCurrDBTimeStr();
						ocsInfoManager.addOCSInfoToOCSInfoUpdateList(OPERATIONTIME, lastUpdateOperationTime);
						
						if(ocsInfoManager.isOperationManagerLoggingUsage()){
							StringBuilder sb = new StringBuilder("[DEBUG]update OpeationTime:");
							sb.append(lastUpdateOperationTime);
							traceOperation(sb.toString());
						}
					}
				}
			} catch (Exception e) {
				traceOperationException("updateOperationTime()", e);
			}
		}
	}
	
	/**
	 * TSC Event 발생하면 MCS로 보고하기 위해 DB에 등록한다.
	 * @param eventName
	 * @param nAlarmID
	 */
//	private void sendS6F11(String eventName, int nAlarmID) {
//		assert ocsInfoManager != null;
//		
//		if (ocsInfoManager.isIBSEMUsed()) {
//			if (this.serviceState == MODULE_STATE.INSERVICE) {
//				Message report = new Message();
//				report.setMessageName(MessageItem.SEND_S6F11);
//				report.setMessageItem(MessageItem.EVENT_NAME, eventName, false);
//				report.setMessageItem(MessageItem.EVENT_TYPE, EVENT_TYPE.TSC.toConstString(), false);
//				
//				registerReport(report.toMessage());
//			}
//		}
//	}
	
	/**
	 * Send TSC Type S6F11
	 */
	private void sendS6F11_TSC(String eventName, int alarmId) {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				Message report;
				
				if (isIBSEMUsed) {
					report = new Message();
					report.setMessageName(MessageItem.SEND_S6F11);
					report.setMessageItem(MessageItem.EVENT_TYPE, EVENT_TYPE.TSC.toConstString(), false);
					report.setMessageItem(MessageItem.EVENT_NAME, eventName, false);
					report.setMessageItem(MessageItem.RESULT_CODE, alarmId, false);
					registerReport(report.toMessage());
				}
			}
		} catch (Exception e) {
			traceOperationException("sendS6F11_TSC()", e);
		}
	}
	
	/**
	 * Send TrCmd Type S6F11
	 */
	private void sendS6F11_TRCMD(String eventName, int alarmId, TrCmd trCmd) {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				Message report;
				
				if (isIBSEMUsed) {
					if (trCmd != null) {
						report = new Message();
						report.setMessageName(MessageItem.SEND_S6F11);
						report.setMessageItem(MessageItem.EVENT_TYPE, EVENT_TYPE.TRCMD.toConstString(), false);
						report.setMessageItem(MessageItem.EVENT_NAME, eventName, false);
						report.setMessageItem(MessageItem.COMMAND_ID, trCmd.getTrCmdId(), false);
						report.setMessageItem(MessageItem.CARRIER_ID, trCmd.getCarrierId(), false);
						report.setMessageItem(MessageItem.CARRIER_LOC, trCmd.getCarrierLoc(), false);
						report.setMessageItem(MessageItem.REPLACE, 0, false);
						report.setMessageItem(MessageItem.PRIORITY, trCmd.getPriority(), false);
						report.setMessageItem(MessageItem.SOURCE_PORT, trCmd.getSourceLoc(), false);
						report.setMessageItem(MessageItem.DEST_PORT, trCmd.getDestLoc(), false);
						report.setMessageItem(MessageItem.RESULT_CODE, alarmId, false);
						registerReport(report.toMessage());
					} else {
						traceOperationException("sendS6F11_TRCMD() - trCmd is null.");
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("sendS6F11_TRCMD()", e);
		}
	}

	/**
	 * Register Report
	 * 
	 * @param message
	 */
	private void registerReport(String message) {
		try {
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				if (isIBSEMUsed) {
					if (ibsemReportManager != null) {
						ibsemReportManager.registerReport(message);
						traceHostReport(message);
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("registerReport()", e);
		}
	}
	
	private void traceFormattedTrCompletionHistory(TrCompletionHistory trCompletionHistory) {
		if (trCompletionHistory != null) {
			StringBuffer message;
			
			message = new StringBuffer();
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
		} else {
			traceOperationException("traceFormattedTrCompletionHistory() - trCompletionHistory is null.");
		}
	}
	
	private String getMessageSizeInfoForLogServer(String message) {
		if (message != null) {
			String size;
			
			size = Integer.toString(message.length(), 36);
			return FOUR_ZEROS.substring(0, 4 - size.length()) + size;
		} else {
			return FOUR_ZEROS;
		}
	}
	
	private boolean isNothingUpdateToDB() {
		try {
			if (trCmdManager == null || vehicleManager == null) {
				return false;
			}
			if (trCmdManager.isNothingUpdateToDB() == false) {
				return false;
			}
			if (vehicleManager.isNothingUpdateToDB() == false) {
				return false;
			}
		} catch (Exception e) {
			traceException(e);
		}
		return true;
	}
	
	private void registerAlarmText(String alarmText) {
		if (alarmManager != null) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.registerAlarmText(OPERATION, alarmText);
		}
	}
	
	private void registerAlarmTextWithLevel(String alarmText, ALARMLEVEL alarmLevel) {
		if (alarmManager != null) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.registerAlarmTextWithLevel(OPERATION, alarmText, alarmLevel.toConstString());
		}
	}
	
	private void unregisterAlarmText(String alarmText) {
		if (alarmManager != null) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.unregisterAlarmText(OPERATION, alarmText);
		}
	}
	
	private void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
		if (eventHistoryManager != null) {
			eventHistoryManager.addEventHistoryToRegisterList(eventHistory, duplicateCheck);
		}
		if (ocsInfoManager != null && ocsInfoManager.isFormattedLogUsed()) {
			traceFormattedEventHistory(eventHistory);
		}
	}
	
	private static final String FORMAT_EVENTHISTORY_TRACE = "EventHistoryLog";
	private static Logger eventHistoryTraceFormatLog = Logger.getLogger(FORMAT_EVENTHISTORY_TRACE);
	/**
	 * Trace EventHistoryLog
	 * 
	 * @param message
	 */
	public void traceFormattedEventHistory(EventHistory eventHistory) {
		if (eventHistory != null) {
			StringBuffer message;
			
			message = new StringBuffer();
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
		} else {
			traceOperationException("traceFormattedEventHistory() - eventHistory is null.");
		}
	}
	
	// 2015.06.11 by KYK
	private void traceOperation(String vehicleId, String message) {
		operationTraceLog.debug(String.format("%s> %s", vehicleId, message));
	}

	private void traceOperation(String message) {
		operationTraceLog.debug(String.format("%s> %s", "Main", message));
	}
	
	private void traceRuntimeUpdate(String message) {
		runtimeUpdateHistoryLog.info(message);
	}
	
	private void traceDeadlockBreak(String message) {
		deadlockBreakLog.debug(message);
	}
	
	private void traceHostReport(String message) {
		hostReportLog.debug(String.format("RegisterReport: %s", message));
	}
	
	private void traceOperationException(String message) {
		operationExceptionLog.error(String.format("%s> [%s] ", "Main", message));
	}
	private void traceOperationException(String message, Throwable t) {
		operationExceptionLog.error(String.format("%s> [%s] ", "Main", message), t);
	}
	
	public void setRequestedServiceState(MODULE_STATE state) {
		this.requestedServiceState = state;
	}
	
	public MODULE_STATE getServiceState() {
		return this.serviceState;
	}
	
	/**
	 * @author zzang9un
	 * @since	2014. 8. 18.
	 * @return	detectedVehicleList
	 */
	public ArrayList<VehicleData> getDetectedVehicleList() {
		return detectedVehicleList;
	}

	/**
	 * @author zzang9un
	 * @since	2014. 8. 18.
	 * @param 	detectedVehicleList
	 */
	public void setDetectedVehicleList(ArrayList<VehicleData> detectedVehicleList) {
		this.detectedVehicleList = detectedVehicleList;
	}
	
	/**
	 * Search forward vehicles in front of certain vehicle. And add vehicle to causedvehicleList.  
	 * @author zzang9un
	 * @since	2014. 8. 18.
	 * @param vehicle : certain vehicle
	 */
	private void searchForwardVehicleOfDetectedVehicle(VehicleData vehicle) {
		Node checkNode;
		VehicleData checkVehicle;
		
		for (int i = 0; i < vehicle.getDriveNodeCount(); i++) {
			checkNode = vehicle.getDriveNode(i);
			
			if (checkNode != null) {
				for (int j = 0; j < checkNode.getDriveVehicleCount(); j++) {
					checkVehicle = checkNode.getDriveVehicle(j);
					
					// 특정 vehicle과 같은 경우 제외
					if (vehicle.equals(checkVehicle))
						continue;
					
					if (checkVehicle != null && 
							checkNode.equals(checkVehicle.getDriveCurrNode()))
						vehicle.addDetectedCausedVehicleList(checkVehicle);
				}				
			}		
			
		}
	}
	
	/**
	 * Deadlock이 감지된 Vehicle list를 이용하여 Alarm Text를 생성하는 함수
	 * @author zzang9un
	 * @since	2014. 9. 30.
	 * @param detectedVehicleList - Deadlock이 감지된 Vehicle List
	 * @return Deadlock Alarm Text(String)
	 */
	private String makeDeadlockAlarmText(ArrayList<VehicleData> detectedVehicleList) {
		if (detectedVehicleList.size() > 0) {
			StringBuffer deadlockAlarmText = new StringBuffer();
			deadlockAlarmText.append("[Deadlock Detected] ");
			
			VehicleData deadlockDetectedVehicle = null;
			for (int i = 0; i < detectedVehicleList.size(); i++) {
				deadlockDetectedVehicle = detectedVehicleList.get(i);
				if (deadlockDetectedVehicle != null) {
					if (i > 0) {
						deadlockAlarmText.append(",");
					}
					if (i < 3) {
						deadlockAlarmText.append(deadlockDetectedVehicle.getVehicleId());
					} else {
						deadlockAlarmText.append("...");
						break;
					}
				}
			}
			
			return deadlockAlarmText.toString();
		} else {
			return null;
		}
	}
	
	/**
	 * deadlock이 감지된 Vehicle List 문자열을 deadlockDetectedLoopMap에 add하는 함수
	 * @author zzang9un
	 * @since	2014. 9. 30.
	 * @param deadlockDetectedVehicles - Deadlock이 감지된 Vehicle List 문자열
	 */
	private void addDeadlockDetectedLoopMap(String deadlockDetectedVehicles) {
		if (deadlockDetectedVehicles != null) {
			if (deadlockDetectedLoopMap.containsKey(deadlockDetectedVehicles) == false) {
				deadlockDetectedLoopMap.put(deadlockDetectedVehicles, System.currentTimeMillis());
			}			
		}
	}
	
	/**
	 * Deadlock이 감지된 Loop를 이용하여 알람 메세지를 등록하거나 해제하는 함수
	 * @author zzang9un
	 * @since	2014. 9. 30.
	 * @param currentDeadlockDetectedLoopList - Deadlock이 감지된 Loop 정보가 저장된 List
	 */
	private void registerDeadlockAlarm(ArrayList<String> currentDeadlockDetectedLoopList) {
		// 2014.09.29 by zzang9un : Deadlock Detect timeout 파라미터 가져오기
		long deadlockDetectedTimeout = ocsInfoManager.getDeadlockDetectedTimeout();
		
		@SuppressWarnings("unchecked")
		HashMap<String, Long> deadlockDetectedLoopMapClone = (HashMap<String, Long>)deadlockDetectedLoopMap.clone();
		
		// 현재 발생한 Deadlock Loop와 기존 Deadlock을 모두 비교
		for (int i = 0; i < currentDeadlockDetectedLoopList.size(); i++) {
			if (deadlockDetectedLoopMapClone.containsKey(currentDeadlockDetectedLoopList.get(i))) {
				// 기존 deadlock loop가 있는 경우 timeout 체크 후 알람 등록
				if ((System.currentTimeMillis() - deadlockDetectedLoopMapClone.get(currentDeadlockDetectedLoopList.get(i))) > deadlockDetectedTimeout) {
					registerAlarmText(currentDeadlockDetectedLoopList.get(i));
				}
				
				// 동일한 deadlock loop는 clone에서 삭제
				deadlockDetectedLoopMapClone.remove(currentDeadlockDetectedLoopList.get(i));
			} else {
				// 없는 경우 새로 등록
				addDeadlockDetectedLoopMap(currentDeadlockDetectedLoopList.get(i));
			}
		}
		
		// 해제된 deadlock loop 삭제(알람도 해제)
		// clone에 남아 있는 해제된 loop이므로 모두 삭제한다.
		Iterator<String> it = deadlockDetectedLoopMapClone.keySet().iterator();
		
		while (it.hasNext()) {
			String deadlockAlarmText = it.next();
			
			deadlockDetectedLoopMap.remove(deadlockAlarmText);
			unregisterAlarmText(deadlockAlarmText);
		}
	}
	
	/**
	 * 2022.03.14 dahye : Premove Logic Improve
	 * String Format의 time으로부터 시간을 가져오기 위한 함수
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
	 * TrCmd의 WaitingTime 시간을 확인하기 위한 함수
	 * time ~ CurrTime
	 * 2022.03.14 dahye : Premove Logic Improve
	 */
	private long getWaitingTime(String time) {
		try {
			return (long) ((getTimeFromString(ocsInfoManager.getCurrDBTimeStr()) - getTimeFromString(time)) / 1000);
		} catch (Exception e) {
			traceOperationException("getWaitingTime()", e);
			return System.currentTimeMillis();
		}
	}
	
	/**
	 * 할당되지 않은 Premove 반송에 대해 DWT Timeover 발생할 경우 해당 반송을 비정상완료 처리하는 함수
	 * 2022.03.14 dahye : Premove Logic Improve
	 */
	private void notAssignedPremoveCancel(TrCmd trCmd) {
		trCmd.setRemoteCmd(TRCMD_REMOTECMD.CANCEL);
		trCmd.setState(TRCMD_STATE.CMD_CANCELED);
		trCmd.setDeletedTime(ocsInfoManager.getCurrDBTimeStr());
		
		TrCompletionHistory trCompletionHistory = new TrCompletionHistory(trCmd, TRCMD_REMOTECMD.CANCEL.toConstString(), "");
		trCompletionHistoryManager.addTrCmdToRegisterTrCompletionHistory(trCompletionHistory);
		if (ocsInfoManager.isFormattedLogUsed()) {
			traceFormattedTrCompletionHistory(trCompletionHistory);
		}
		
//		sendS6F11_TRCMD(OperationConstant.TRANSFER_CANCELCOMPLETED, 0, trCmd);
		sendS6F11_TRCMD(OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_PREMOVE_WAIT_TIMEOUT, trCmd);
		
		traceOperation("Job Cancel: " + trCmd.getTrCmdId());
		
		// 해당 TrCmdInfo 삭제
		this.trCmdManager.deleteTrCmdFromDB(trCmd.getTrCmdId());
	}
}