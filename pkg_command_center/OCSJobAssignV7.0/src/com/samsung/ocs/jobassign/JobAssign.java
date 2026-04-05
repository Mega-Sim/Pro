package com.samsung.ocs.jobassign;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.common.constant.OcsConstant.CARRIERLOC_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.SEARCH_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.COSTSEARCH_OPTION;
import com.samsung.ocs.common.constant.OcsInfoConstant.DISPATCHING_RULES;
import com.samsung.ocs.common.constant.OcsInfoConstant.JOB_RESERVATION_OPTION;
import com.samsung.ocs.common.constant.OcsInfoConstant.PRIORJOB_DISPATCHING_RULE;
import com.samsung.ocs.common.constant.OcsInfoConstant.VEHICLECOMM_TYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.jobassign.model.BackupVehicle;
import com.samsung.ocs.jobassign.model.JobAssignResult;
import com.samsung.ocs.jobassign.model.PriorJob;
import com.samsung.ocs.jobassign.model.ReservedAsNextTrCmd;
import com.samsung.ocs.jobassign.model.StageTarget;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.AreaManager;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.CollisionManager;
import com.samsung.ocs.manager.impl.EventHistoryManager;
import com.samsung.ocs.manager.impl.LocalGroupInfoManager;
import com.samsung.ocs.manager.impl.MaterialControlManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.ParkManager;
import com.samsung.ocs.manager.impl.SectionManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.TrCompletionHistoryManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.ZoneControlManager;
import com.samsung.ocs.manager.impl.model.Area;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.Collision;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.LocalGroupInfo;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Park;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.TrCompletionHistory;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.route.search.CostSearch;

/**
 * JobAssign Class, OCS 3.0 for Unified FAB
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

public class JobAssign {
	private CarrierLocManager carrierLocManager = null;
	private LocalGroupInfoManager localGroupInfoManager = null;
	private NodeManager nodeManager = null;
	private SectionManager sectionManager = null;
	private CollisionManager collisionManager = null;
	private OCSInfoManager ocsInfoManager = null;
	private TrCmdManager trCmdManager = null;
	private VehicleManager vehicleManager = null;
	private AreaManager areaManager = null;
	private ZoneControlManager zoneControlManager = null;
	private AlarmManager alarmManager = null;
	private ParkManager parkManager = null;
	private EventHistoryManager eventHistoryManager = null;
	private TrCompletionHistoryManager trCompletionHistoryManager = null;
	private MaterialControlManager materialControlManager = null;
	
	private HashMap<String, Object> vehicleList;						// Normal Auto Enabled Vehicles
	private HashMap<String, Object> enabledVehicleList;					// All Enabled Vehicles
	
	private ConcurrentHashMap<String, Object> trCmdList;				// 전체 TrCmd
	private ConcurrentHashMap<String, Object> areaList;					// 전체 Area
	private ConcurrentHashMap<String, Object> localGroupInfoList;		// 전체 LocalGroupInfo
	private ConcurrentHashMap<String, Object> parkList;					// 전체 Park
	
	private HashMap<TrCmd, Vehicle> reservedTrCmdMap;					// Reserved TrCmd's LOCATE Requested Vehicle.
	private HashMap<Vehicle, TrCmd> reservedVehicleMap;					// Reserved Vehicle's TrCmd (Next TrCmd & LOCATE).
	
	// 2014.02.27 by MYM : [Stage Locate 기능]
	private TreeSet<TrCmd> unassignedStageList;
	private HashMap<String, ArrayList<TrCmd>> sortedStageAtSamePortMap;
	private HashMap<TrCmd, Node> stageTargetNodeMap;
	private HashMap<String, StageTarget> unavailableStageTargetMap;
	private HashMap<Vehicle, TrCmd> reservedStageVehicleMap;
	private ArrayList<Vehicle> stageLocateVehicleList;
	private ArrayList<Vehicle> newStageLocateVehicleList;
	
	private TreeSet<TrCmd> localTrCmdList;								// LocalGroup TrCmd
	private HashMap<TrCmd, String> localTrCmdGroupIdMap;				// LocalGroupId of LocalTrCmd
	
	private TreeSet<TrCmd> unassignedTrCmdList;							// 미할당된 TrCmd (AssignedVehicle == '')
	private TreeSet<TrCmd> assignedTrCmdList;							// 할당된 TrCmd (AssignedVehicle is not null)
	private TreeSet<Vehicle> idleVehicleList;							// 작업을 수행하지 않고 있는 Idle Vehicle
	private TreeSet<Vehicle> loadVehicleList;							// Unloaded 작업부터 Loading 구간내 작업을 수행중인 Vehicle
	private TreeSet<Vehicle> localVehicleList;							// 가변형 LocalOHT
	private TreeSet<Vehicle> idleLocalVehicleList;						// Idle인 가변형 LocalOHT
	private TreeSet<Vehicle> tempCandidateVehicleList;
	private ArrayList<Vehicle> candidateVehicleList;					// 작업할당 후보 Vehicle (LocalOHT + GeneralVehicle)
	private ArrayList<Vehicle> locateVehicleList;						// Idle이지만 지정 노드로 LOCATE 요청받아 이동 중인 Vehicle (Curr-Stop-Target-SourceNode)
	private ArrayList<LocalGroupInfo> targetLocalGroupList;
	private ArrayList<TrCmd> targetTrCmdList;
	private ArrayList<Park> targetParkList;
	private ArrayList<TrCmd> sourceDupTrCmdList;
	
	private ArrayList<Vehicle> candidateVehicleListForJobAllocation;
	private ArrayList<Vehicle> candidateVehicleListForParkAllocation;
	
	private ArrayList<TrCmd> targetTrCmdListForJobAllocation;
	private ArrayList<Park> targetParkListForParkAllocation;
	
	private ArrayList<String> targetNodeList;
	// 2013.05.16 by KYK
	private ArrayList<String> targetList;	// node & station
	private ArrayList<String> targetStationList;	// station		
	private HashMap<String,String> targetStationMap;	// station		
	
	private ArrayList<String> assignedVehicleIdList;					// 작업을 할당받은 Vehicle
	private ArrayList<String> assignRequestedVehicleIdList;				// Next 작업을 할당받은 Vehicle
	private ArrayList<String> localGroupIdList;
	private HashSet<String> assignAllowedList;
	// 2013.07.10 by KYK
	private ArrayList<String> materialAssignAllowedList;

	private HashSet<String> parkAllowedList;
	private HashSet<String> driveAllowedList;
	private ArrayList<String> releaseLocalGroupIdList;
	
	private ArrayList<Vehicle> resultVehicleList;
	private ArrayList<Object> resultTargetList;
	
	private ArrayList<Vehicle> reservedVehicleList;
	private ArrayList<Vehicle> tempReservedVehicleList;
	
	private HashMap<Vehicle, Park> requestedParkMap;
	private HashMap<Vehicle, Park> tempRequestedParkMap;
	
	private ArrayList<LocalGroupInfo> setLocalGroupInfoList;
	
	private ArrayList<String> idleVehicleLocalGroupIdList;
	
	private ArrayList<String> destLocOfUnloadedVehicleList;
	private HashMap<String, TrCmd> unassignedTrCmdMapBySourceLoc;
	private HashMap<String, Double> costMap;
	private HashMap<String, Integer> localGroupCurVHL;
	private HashMap<String, Integer> materialIndexMapOfLocalGroup;
	
	private HashMap<TrCmd, Long> waitingTimeMap;
	
	private TreeSet<JobAssignResult> jobAssignResultSet;
	private TreeSet<JobAssignResult> parkResultSet;
	
	private TreeSet<ReservedAsNextTrCmd> reservedAsNextTrCmdSet;
	private TreeSet<BackupVehicle> backupVehicleSet;
	
	private static final String JOBASSIGN_MAIN = "JobAssignMain";
	private static final String JOBASSIGN_DELAY = "JobAssignDelay";
	private static final String JOBASSIGN_EXCEPTION = "JobAssignException";
	private static final String MOVE = "MOVE";
	private static final String LOCATE = "LOCATE";
	private static final String PMOVE = "PMOVE";
	private static final String PLOCATE = "PLOCATE";
	private static final String LOCATECANCEL = "LOCATECANCEL";
	private static final String STAGE = "STAGE";
	private static final String STAGEWAIT = "STAGEWAIT";
	private static final String STAGENOBLOCK = "STAGENOBLOCK";
	private static final String STAGECANCEL = "STAGECANCEL";
	
	// LocalGroupInfo의 AssignOption. (Default: DISTANCE)
//	private static final String DISTANCE = "DISTANCE";
	private static final String LOCAL = "LOCAL";
	
	private static final String CONVEYOR = "CV_";
	
	private static final String BLANK = " ";
	
	private static final String FORMAT = "%04.2f";
	private static final String FORMAT0 = "%,.0f";
	private static final String FORMAT1 = "%.1f";
	private static final String FORMAT2 = "%.2f";
	private static final String FORMAT3 = "%.3f";
	private static final String FORMAT4 = "%.4f";
	private static final String FORMAT03D = "%03d";
	private static final String FORMAT06D = "%06d";
	
	private static final double MAXCOST_TIMEBASE = 9999.0;
	private static final int MAX_SEARCH_LIMIT_CHECK_COUNT = 5;
	
	private CostSearch costSearch;
	
	private boolean isJobAssignDetailResultUsed = false;
	private boolean isLocalOHTUsed = false;
	private boolean isRefinePortDupTrCmdUsed = false;
	private boolean isAreaBalancingUsed = false;
	private boolean isParkNodeUsed = false;
	private boolean isConveyorHotPriorityUsed = false;
	private boolean isHidControlUsed = true;
	private boolean isNearByDrive = false;
	private boolean isStageLocateUsage = true;
	
  // 2014.02.27 by MYM : [Stage Locate 기능]
	private int stageLocateVehicleCount = 3;
	
	private int idleLocalCandidateVehicleCount = 0;
	private int idleGeneralCandidateVehicleCount = 0;
	private int localTrCmdCount = 0;
	
	private int maxLocalGroupIdLength = 3;
	
	private int parkCheckCount = 0;
	private boolean isParkSearchNeeded = false;
	
//	private int distanceLimit = 0;
	private int jobAssignDelayLimit = 6000;
	private int priorJobCriteriaOfPriority = 100;
	private int priorJobCriteriaOfWaitingTime = 240;
	private int priorJobCriteriaOfTransferTime = 300;
	private int searchLimitCheckCount = 0;
	private int parkSearchInterval = 5;
	private int size;
	private int[] result;
	private double[][] cost;
	private double[][] costBackup;
	private double[][] costNodes;
	private double[][] costNodesBackup;
	
	private long currentDBTime = 0;
	private long costSearchStartTime = 0;
	private long costSearchedTime = 0;
	private long allocateStartedTime = 0;
	private long allocateCompletedTime = 0;
	
	private SimpleDateFormat sdf;
	
	private double loadedVehiclePenalty = 0;
	private double jobAssignSearchLimit = 9999;
	private double jobAssignThreshold = 9999;
	private double jobAssignLocateThreshold = 9999;
	private double jobAssignPriorityThreshold = 90;
	private double jobAssignPriorityWeight = 2;
	private double jobAssignUrgentThreshold = 300;
	private double jobAssignWaitingTimeThreshold = 150;
	private double jobAssignWaitingTimeWeight = 1;
	private double parkSearchLimit = 9999;
	private double searchLimit = 9999;
	
	private APSolver solver;
	private COSTSEARCH_OPTION costSearchOption = COSTSEARCH_OPTION.NONE;
	protected DISPATCHING_RULES dispatchingRule = DISPATCHING_RULES.HYBRID;
	private JOB_RESERVATION_OPTION jobReservationOption = JOB_RESERVATION_OPTION.JR1; // 2015.03.17 by KYK
	private int jobReservationLimitTime = 300;
	// 2015.10.01 by KYK : priorJob dispatching
	private boolean isPriorJobDispatchingUsed = false;
	private PRIORJOB_DISPATCHING_RULE priorJobDispatchingRule = PRIORJOB_DISPATCHING_RULE.EQPRIORITY;
	private int priorJobPriorityThreshold = 90;
	private int priorJobWaitingTimeThreshold = 600;
	private boolean isIgnoreMaterialDifference = false;	// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
	private boolean isParkCmdEqoption = false;
	private boolean isStageSourceDupCancelUsage = false; // 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
	private boolean isAreaBalancingManualExclude = false; // 2022.12.14 by JJW : Area 내  Max VHL 초과시  Manual VHL 제외 여부
	private VEHICLECOMM_TYPE vehicleCommType = VEHICLECOMM_TYPE.VEHICLECOMM_CHAR; // add by YSJ for FAB QR Tag 부모Node 로의 Load -> Unload 불가로 NextNode 부터 CostSearch 하게 추가

	/**
	 * Constructor of JobAssign class.
	 */
	public JobAssign() {
		this.carrierLocManager = CarrierLocManager.getInstance(null, null, false, false, 0);
		this.localGroupInfoManager = LocalGroupInfoManager.getInstance(null, null, false, false, 0);
		this.nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		this.sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		this.collisionManager = CollisionManager.getInstance(null, null, false, false, 0);
		this.ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		this.trCmdManager = TrCmdManager.getInstance(null, null, false, false, 0);
		this.vehicleManager = VehicleManager.getInstance(null, null, false, false, 0);
		this.areaManager = AreaManager.getInstance(null, null, false, false, 0);
		this.zoneControlManager = ZoneControlManager.getInstance(null, null, false, false, 0);
		this.alarmManager = AlarmManager.getInstance(null, null, false, false, 0);
		this.parkManager = ParkManager.getInstance(null, null, false, false, 0);
		this.eventHistoryManager = EventHistoryManager.getInstance(null, null, false, false, 0);
		this.trCompletionHistoryManager = TrCompletionHistoryManager.getInstance(null, null, false, false, 0);
		this.materialControlManager = MaterialControlManager.getInstance(null, null, false, false, 0);
		
		vehicleList = new HashMap<String, Object>();
		enabledVehicleList = new HashMap<String, Object>();
		trCmdList = new ConcurrentHashMap<String, Object>();
		parkList = new ConcurrentHashMap<String, Object>();
		localGroupCurVHL = new HashMap<String, Integer>();
		unassignedTrCmdMapBySourceLoc = new HashMap<String, TrCmd>();
		costMap = new HashMap<String, Double>();
		
		localTrCmdGroupIdMap = new HashMap<TrCmd, String>();
		waitingTimeMap = new HashMap<TrCmd, Long>();
		
		idleVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		loadVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		localVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		idleLocalVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		tempCandidateVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		sourceDupTrCmdList = new ArrayList<TrCmd>();
		
		locateVehicleList = new ArrayList<Vehicle>();
		candidateVehicleList = new ArrayList<Vehicle>();
		targetTrCmdList = new ArrayList<TrCmd>();
		targetLocalGroupList = new ArrayList<LocalGroupInfo>();
		targetParkList = new ArrayList<Park>();
		targetNodeList = new ArrayList<String>();
		targetList = new ArrayList<String>();	// 2013.05.16 by KYK
		targetStationList = new ArrayList<String>();		
		targetStationMap = new HashMap<String, String>();
		
		destLocOfUnloadedVehicleList = new ArrayList<String>();
		idleVehicleLocalGroupIdList = new ArrayList<String>();
		
		candidateVehicleListForJobAllocation = new ArrayList<Vehicle>();
		candidateVehicleListForParkAllocation = new ArrayList<Vehicle>();
		
		targetTrCmdListForJobAllocation = new ArrayList<TrCmd>();
		targetParkListForParkAllocation = new ArrayList<Park>();
		
		assignedVehicleIdList = new ArrayList<String>();
		assignRequestedVehicleIdList = new ArrayList<String>();
		
		resultVehicleList = new ArrayList<Vehicle>();
		resultTargetList = new ArrayList<Object>();
		
		reservedVehicleList = new ArrayList<Vehicle>();
		tempReservedVehicleList = new ArrayList<Vehicle>();
		
		reservedTrCmdMap = new HashMap<TrCmd, Vehicle>();
		reservedVehicleMap = new HashMap<Vehicle, TrCmd>();
		
		// 2014.02.26 by MYM : [Stage Locate 기능]
		stageTargetNodeMap = new HashMap<TrCmd, Node>();
		unassignedStageList = new TreeSet<TrCmd>(new TrCmdComparator<TrCmd>());
		sortedStageAtSamePortMap = new HashMap<String, ArrayList<TrCmd>>();
		reservedStageVehicleMap = new HashMap<Vehicle, TrCmd>();
		stageLocateVehicleList = new ArrayList<Vehicle>();
		newStageLocateVehicleList = new ArrayList<Vehicle>();
		unavailableStageTargetMap = new HashMap<String, StageTarget>();
		
		requestedParkMap = new HashMap<Vehicle, Park>();
		tempRequestedParkMap = new HashMap<Vehicle, Park>();

		assignedTrCmdList = new TreeSet<TrCmd>(new TrCmdComparator<TrCmd>());
		unassignedTrCmdList = new TreeSet<TrCmd>(new TrCmdComparator<TrCmd>());
		localTrCmdList = new TreeSet<TrCmd>(new TrCmdComparator<TrCmd>());
		localGroupIdList = new ArrayList<String>();
		
		jobAssignResultSet = new TreeSet<JobAssignResult>(new JobAssignResultComparator<JobAssignResult>());
		parkResultSet = new TreeSet<JobAssignResult>(new JobAssignResultComparator<JobAssignResult>());
		
		reservedAsNextTrCmdSet = new TreeSet<ReservedAsNextTrCmd>(new ReservedAsNextTrCmdComparator<ReservedAsNextTrCmd>());
		backupVehicleSet = new TreeSet<BackupVehicle>(new BackupVehicleComparator<BackupVehicle>());
		
		releaseLocalGroupIdList = new ArrayList<String>();
		setLocalGroupInfoList = new ArrayList<LocalGroupInfo>();
		
		sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		costSearch = new CostSearch();
		costSearch.setEnabledVehicleList(enabledVehicleList);
		
		assignAllowedList = zoneControlManager.getAssignAllowedSet();
		parkAllowedList = zoneControlManager.getParkAllowedSet();
		driveAllowedList = zoneControlManager.getDriveAllowedSet();

		// 2013.07.10 by KYK
		materialAssignAllowedList = materialControlManager.getMaterialAssignAllowedList();
		
		solver = new APSolver();
	}
	
	/**
	 * MainProcessing
	 */
	public void mainProcessing() {
		try {
			// Update Operational Parameters for JobAssign
			updateJobAssignParameters();
			
			// Step 2 Manage LocalOHT
			manageLocalOHT();
			
			// Step 3 CostSearch for Job & Park
			costSearchForVehicles();
			
			// Step 4 Allocate LocalJob to InBay LocalOHT
			allocateLocalJobToInBayLocalOHT();
			
			// Step 5 Allocate Job to Candidate Vehicle
			allocateJobToCandidateVehicle();
			
			// Step 6 Allocate Park to IdleVehicle
			allocateParkToIdleVehicle();
			
			// Step 7 Move Idle Vehicle to Reserved as Next
			backupVehicleToReservedAsNext();
			
			// Step 8 Update to DB for JobAssign
			updateToDBForJobAssign();
			
			// Step 9 Check Stage Duration
			// 2014.02.27 by MYM : [Stage Locate 기능]
			checkStageExpectedDurationTimeOver();
		} catch (Exception e) {
			traceJobAssignException("mainProcessing()", e);
		}
	}
	
	/**
	 * Step 1 Update Operational Parameters for JobAssign
	 */
	private void updateJobAssignParameters() {
		// Step 1-1 Clear Lists
		initialize();
		
		// Step 1-2 Update Operational Parameters From OCSINFO
		updateOperationalParameters();
		
		// Step 1-3 Update VEHICLE and TRCMD from DB
		updateVehicleAndTrCmdFromDB();
		
		// Step 1-4 Get Vehicles and TrCmds [IdleVehicle, LoadVehicle, LocalVehicle, IdleLocalVehicle, LocateVehicle / UnassignedTrCmd, AssignedTrCmd]
		getVehiclesAndTrCmds();
		
		// 2014.02.28 by MYM : [Stage Locate 기능]
		// Step 1-4-1 find TargetNode for stage TrCmds at same Port [stageTargetNodeMap]
		searchStageTargetNode();
		
		// Step 1-5 작업 할당을 위해 Vehicle과 TrCmd List를 정리. (PortDuplicatedTrCmd 정리)
		// UnassignedTrCmd와 AssignedTrCmd를 구분한 뒤에 호출해야 함.
		refinePortDuplicatedTrCmds();
		
		// Step 1-6 Update Vehicle Counts per Area for AreaBalancing 
		updateAreaVehicleCounts();
		
		// Step 1-7 Update SearchLimit Check Count
		updateSearchLimitCheckCount();
		
		// Step 1-8 Update link & Section Enabled
		// 2015.02.24 by MYM : 장애 지역 우회 기능
		updateLinkAndSectionFromDB();
	}
	
	/** 
	 * Step 2 Manage LocalOHT
	 */
	private void manageLocalOHT() {
		if (isLocalOHTUsed) {
			// Step 2-1 Classify LocalGroups into releaseGroupList/setGroupList
			classifyLocalGroup();
			
			// Step 2-2 Release LocalOHT(s) of LocalGroup(s) in releaseLocalGroupList
			releaseLocalOHT();
			
			// Step 2-3 Set LocalOHT(s) of LocalGroup(s) in setLocalGroupList
			setLocalOHT();
		} else {
			// Step 2-4 Clear LocalGroupInfo [LOCARLGROUPINFO, VEHICLE, CARRIERLOC]
			clearLocalGroupInfoFromDB();
		}
	}
	
	/**
	 * Step 3 costSearch For Vehicles (Nodes).
	 */
	private void costSearchForVehicles() {
		// Step 3-1 Select Candidate Vehicles [Idle Local Vehicle / Idle General Vehicle / Load Vehicle] 
		selectCandidateVehicles();
		
		// Step 3-2 Select TargetNodes [Local Job / Job / Park]
		selectTargetNodes();
		
		// Step 3-3 Initialize Cost for Target Nodes [costNodes, costNodesBackup]
		initializeCostForTarget();
		
		// Step 3-4 Calculate Cost for Target Nodes
		calculateCostForTarget();

		if (isJobAssignDetailResultUsed && targetList.size() > 0) {
			StringBuilder nodes = new StringBuilder();
			String target = "";
			nodes.append("      ").append("\t");
			for (int j = 0; j < targetList.size(); j++) {
				target = targetList.get(j);
//				nodes.append(makeNodeString(target));
				nodes.append(target);
				if (j < targetList.size() - 1) {
					nodes.append("\t");
				}
			}
			traceJobAssignMain(nodes.toString());
			
			Vehicle vehicle = null;
			for (int i = 0; i < candidateVehicleList.size(); i++) {
				StringBuilder message = new StringBuilder();
				vehicle = candidateVehicleList.get(i);
				if (vehicle != null) {
					message.append(vehicle.getVehicleId());
					
					if (localVehicleList.contains(vehicle)) {
						message.append("(L)");
					} else if (loadVehicleList.contains(vehicle)) {
						message.append("(O)");
					} else if (locateVehicleList.contains(vehicle)) {
						message.append("(M)");
					} else if (idleVehicleList.contains(vehicle)) {
						message.append("(F)");
					} else {
						message.append("(?)");
					}
					
//					Node currNode = nodeManager.getNode(vehicle.getCurrNode());
//					message.append(currNode.getAreaId());
					message.append("\t");
					
					for (int j = 0; j < targetList.size(); j++) {
//						message.append(getCostInSixDigits(costNodes[i][j]));
						message.append(getCostInSixDigits(costNodes[i][j]));
						if (j < targetList.size() - 1) {
							message.append("\t");
						}
					}
				}
				traceJobAssignMain(message.toString());
			}
		}
	}
	
	/**
	 * Step 4 Allocate LocalJob to InBay LocalOHT 
	 */
	private void allocateLocalJobToInBayLocalOHT() {
		try {
			resultVehicleList.clear();
			resultTargetList.clear();
			
			if (isLocalOHTUsed) {
				traceJobAssignMain("[LocalJob Assign Request-Started]");
				if (localTrCmdCount > 0 && idleLocalCandidateVehicleCount > 0) {
					
					// Step 4-1 Get Cost for LocalJob Allocation
					getCostForLocalJob();
					
					// Step 4-2 Print LocalJob Pattern and Cost
					printPatternAndCostForLocalJob();
					
					// Step 4-3 Allocate LocalJob to InBay LocalOHT
					allocateLocalJob();
					
					traceJobAssignMain("[LocalJob Assign Request-Finished]-END");
				} else {
					if (idleLocalCandidateVehicleCount == 0) {
						traceJobAssignMain("[LocalJob Assign Request-Finished]-No Idle LocalVehicle");
					} else {
						traceJobAssignMain("[LocalJob Assign Request-Finished]-No LocalTrCmd");
					}
				}
			}
		} catch (Exception e) {
			traceJobAssignException("allocateLocalJobToInBayLocalOHT()", e);
		}
	}
	
	/**
	 * Step 5 Allocate Job to Candidate Vehicle 
	 */
	private void allocateJobToCandidateVehicle() {
		try {
			traceJobAssignMain("[Job Assign Request-Started]");
			
			// Unassigned(Target) TrCmd가 있고, Idle VHL이 있는 경우, JobAssign 진행.
			if (targetTrCmdList.size() - resultTargetList.size()> 0 && idleVehicleList.size() > 0) {
				
				// Step 5-1 Get Cost for Job Allocation
				getCostForJob();
				
				// Step 5-2 Print Job Pattern and Cost
				printPatternAndCostForJob();
				
				// Step 5-3 Allocate Job to Candidate Vehicle
				allocateJob();
				
				traceJobAssignMain("[Job Assign Request-Finished]-END");
			} else {
				if (idleVehicleList.size() == 0) {
					traceJobAssignMain("[Job Assign Request-Finished]-No Idle Vehicle");
				} else {
					traceJobAssignMain("[Job Assign Request-Finished]-No TrCmd");
				}
			}
		} catch (Exception e) {
			traceJobAssignException("allocateJobToCandidateVehicle()", e);
		}
	}
	
	/**
	 * Step 6 Allocate Park to IdleVehicle
	 */
	private void allocateParkToIdleVehicle() {
		try {
			if (isParkNodeUsed) {
				if (isParkSearchNeeded) {
					traceJobAssignMain("[Park Request-Started]");
					if (targetParkList.size() > 0) {
						
						// Step 6-1 Get Cost for Park Allocation
						getCostForPark();
						
						// Step 6-2 Print Park Pattern and Cost
						printPatternAndCostForPark();
						
						// Step 6-3 Allocate Park to Idle Vehicle
						allocatePark();
						
						traceJobAssignMain("[Park Request-Finished]-END");
					} else {
						traceJobAssignMain("[Park Request-Finished]-No ParkNode");
					}
				}
			} else {
				manageRequestedPark();
			}
		} catch (Exception e) {
			traceJobAssignException("allocateParkToIdleVehicle()", e);
		}
	}
	
	/**
	 * Step 7 Move Idle Vehicle to Reserved as Next
	 */
	private void backupVehicleToReservedAsNext() {
		if (reservedAsNextTrCmdSet.size() > 0) {
			ArrayList<Vehicle> remainedIdleVehicleList = new ArrayList<Vehicle>();
			for (Vehicle vehicle: candidateVehicleList) {
				if (vehicle != null) {
					if (idleVehicleList.contains(vehicle) &&
							resultVehicleList.contains(vehicle) == false &&
							localVehicleList.contains(vehicle) == false) {
						remainedIdleVehicleList.add(vehicle);
					}
				}
			}
			if (remainedIdleVehicleList.size() == 0) {
				return;
			}
			
			StringBuilder remained = new StringBuilder();
			remained.append("Remained Idle Vehicle:");
			for (Vehicle vehicle: remainedIdleVehicleList) {
				remained.append(vehicle.getVehicleId()).append(" ");
			}
			traceJobAssignMain(remained.toString());
			
			ReservedAsNextTrCmd reservedAsNextTrCmd = null;
			Iterator<ReservedAsNextTrCmd> it = reservedAsNextTrCmdSet.iterator();
			while (it.hasNext()) {
				reservedAsNextTrCmd = it.next();
				if (reservedAsNextTrCmd != null) {
					moveBackupVehicle(reservedAsNextTrCmd, remainedIdleVehicleList);
					if (remainedIdleVehicleList.size() == 0) {
						break;
					}
				}
			}
			backupVehicleSet.clear();
		}
	}
	
	private void moveBackupVehicle(ReservedAsNextTrCmd reservedAsNextTrCmd, ArrayList<Vehicle> remainedIdleVehicleList) {
		// 2022.03.11 dahye : moveBackupVehicle logic exception handling
		try {
			TrCmd trCmd = reservedAsNextTrCmd.getTrCmd();
			if (trCmd != null) {
				// 2013.06.05 by KYK
				String targetId = null;
				String sourceStationId = getStationIdAtPort(trCmd.getSourceLoc());
				if (sourceStationId != null && sourceStationId.length() > 0) {
					targetId = sourceStationId;
				} else {
					targetId = trCmd.getSourceNode();
				}
				int j = targetList.indexOf(targetId);
				int i = 0;
				double cost = 9999;
				backupVehicleSet.clear();
				for (Vehicle vehicle: remainedIdleVehicleList) {
					if (vehicle != null) {
						if (resultVehicleList.contains(vehicle) == false) {
							if (isJobAssignable(vehicle, trCmd)) {
								if (isAreaBalancingUsed == false || checkAreaBalancing(vehicle, trCmd)) {
									i = candidateVehicleList.indexOf(vehicle);
									if (i == -1) {
										break;
									}
									cost = costNodes[i][j];
									if (cost < reservedAsNextTrCmd.getReservingCost() + 50) {
										backupVehicleSet.add(new BackupVehicle(vehicle, cost));
									}
								}
							}
						}
					}
				}
				if (backupVehicleSet.size() > 0) {
					Vehicle vehicle = null;
					BackupVehicle backupVehicle = null;
					Iterator<BackupVehicle> it = backupVehicleSet.iterator();
					while (it.hasNext()) {
						backupVehicle = it.next();
						if (backupVehicle != null) {
							vehicle = backupVehicle.getVehicle();
							if (vehicle != null) {
								if (vehicle.getTargetNode().equals(trCmd.getSourceNode())) {
									traceJobAssignMain("[BackupVehicle] Vehicle:" + vehicle.getVehicleId() + " is moving to N" + trCmd.getSourceNode() + " (for " + trCmd.getTrCmdId() + ")");
								} else {
									traceJobAssignMain("[BackupVehicle] MOVE Vehicle:" + vehicle.getVehicleId() + " to N" + trCmd.getSourceNode() + " (for " + trCmd.getTrCmdId() + ")");
									vehicleManager.updateVehicleRequestedInfoToDB(MOVE, trCmd.getSourceNode(), vehicle.getVehicleId());
								}
								if (resultVehicleList.contains(vehicle) == false) {
									resultVehicleList.add(vehicle);
								}
								remainedIdleVehicleList.remove(vehicle);
								return;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			traceJobAssignException("     [MBV_Exception] (TrCmd:" + reservedAsNextTrCmd.getTrCmd().getTrCmdId() + ")", e);
		}
	}
	
	/**
	 * Step 8 Update to DB for JobAssign
	 * 
	 */
	private void updateToDBForJobAssign() {
		trCmdManager.updateTrCmdAssignedVehicle();
		vehicleManager.updateToDBForJobAssign();
	}
	
	/**
	 * Step 1-1 Clear Lists
	 * 
	 */
	private void initialize() {
		// getVehicleList()
		vehicleList.clear();
		enabledVehicleList.clear();
		
		// getTrCmdList()
		assignedTrCmdList.clear();
		assignedVehicleIdList.clear();
		assignRequestedVehicleIdList.clear();
		
		// classifyVehicles()
		idleVehicleList.clear();
		localVehicleList.clear();
		localGroupIdList.clear();
		
		// classifyTrCmdsAndVehicles()
		unassignedTrCmdList.clear();
		loadVehicleList.clear();
		locateVehicleList.clear();
		idleLocalVehicleList.clear();
		idleVehicleLocalGroupIdList.clear();
		
		// refinePortDuplicatedTrCmds()
		unassignedTrCmdMapBySourceLoc.clear();
		sourceDupTrCmdList.clear();
		
		// classifyLocalGroup()
		releaseLocalGroupIdList.clear();
		setLocalGroupInfoList.clear();
		
		resultVehicleList.clear();
		resultTargetList.clear();
		
		candidateVehicleList.clear();
		targetLocalGroupList.clear();
		targetTrCmdList.clear();
		targetParkList.clear();
		destLocOfUnloadedVehicleList.clear();
		unassignedTrCmdList.clear();
		
		tempRequestedParkMap.clear();
		
		localTrCmdList.clear();
		localTrCmdGroupIdMap.clear();
		localTrCmdCount = 0;
		
		reservedAsNextTrCmdSet.clear();
		backupVehicleSet.clear();
		
		newStageLocateVehicleList.clear();
	}
	
	/**
	 * Step 1-2 Update Operational Parameters From OCSINFO
	 */
	private void updateOperationalParameters() {
		isJobAssignDetailResultUsed = ocsInfoManager.isJobAssignDetailResultUsed();
		isLocalOHTUsed = ocsInfoManager.isLocalOHTUsed();
		isRefinePortDupTrCmdUsed = ocsInfoManager.isRefinePortDupTrCmdUsed();
		isAreaBalancingUsed = ocsInfoManager.isAreaBalancingUsed();
		isParkNodeUsed = ocsInfoManager.isParkNodeUsed();
		isConveyorHotPriorityUsed = ocsInfoManager.isConveyorHotPriorityUsed();
		isHidControlUsed = ocsInfoManager.isHidControlUsed();
		isNearByDrive = ocsInfoManager.isNearByDrive();
		loadedVehiclePenalty = ocsInfoManager.getLoadedVehiclePenalty();
		jobAssignThreshold = ocsInfoManager.getJobAssignThreshold();
		jobAssignLocateThreshold = ocsInfoManager.getJobAssignLocateThreshold();
		jobAssignPriorityThreshold = ocsInfoManager.getJobAssignPriorityThreshold();
		jobAssignPriorityWeight = ocsInfoManager.getJobAssignPriorityWeight();
		jobAssignUrgentThreshold = ocsInfoManager.getJobAssignUrgentThreshold();
		jobAssignWaitingTimeThreshold = ocsInfoManager.getJobAssignWaitingTimeThreshold();
		jobAssignWaitingTimeWeight = ocsInfoManager.getJobAssignWaitingTimeWeight();
		jobAssignDelayLimit = ocsInfoManager.getJobAssignDelayLimit();
		priorJobCriteriaOfPriority = ocsInfoManager.getPriorJobCriteriaOfPriority();
		priorJobCriteriaOfWaitingTime = ocsInfoManager.getPriorJobCriteriaOfWaitingTime();
		priorJobCriteriaOfTransferTime = ocsInfoManager.getPriorJobCriteriaOfTransferTime();
		costSearchOption = ocsInfoManager.getCostSearchOption();
		dispatchingRule = ocsInfoManager.getDispatchingRule();
		parkSearchInterval = ocsInfoManager.getParkSearchInterval();
		// 2014.02.27 by MYM : [Stage Locate 기능]
		stageLocateVehicleCount = ocsInfoManager.getStageLocateVehicleCount();
		isStageLocateUsage = ocsInfoManager.isStageLocateUsage();
		// 2015.03.17 by KYK : [Reserved Job Priority]
		jobReservationOption = ocsInfoManager.getJobReservationOption();
		jobReservationLimitTime = ocsInfoManager.getJobReservationLimitTime();

		if (searchLimitCheckCount > MAX_SEARCH_LIMIT_CHECK_COUNT) {
			jobAssignSearchLimit = MAXCOST_TIMEBASE;
			parkSearchLimit = MAXCOST_TIMEBASE;
		} else {
			jobAssignSearchLimit = ocsInfoManager.getJobAssignSearchLimit();
			parkSearchLimit = ocsInfoManager.getParkSearchLimit();
		}
		
		if (++parkCheckCount >= parkSearchInterval)  {
			parkCheckCount = 0;
			isParkSearchNeeded = true;
			searchLimit = (parkSearchLimit > jobAssignSearchLimit) ? parkSearchLimit : jobAssignSearchLimit;
		} else {
			isParkSearchNeeded = false;
			searchLimit = jobAssignSearchLimit;
		}
		
		try {
			currentDBTime = sdf.parse(ocsInfoManager.getCurrDBTimeStr()).getTime();
		} catch (Exception e) {
			traceJobAssignException("getCurrentTimeFromDB()", e);
			currentDBTime = System.currentTimeMillis();
		}
		
		// 2015.10.01 by KYK
		isPriorJobDispatchingUsed = ocsInfoManager.isPriorJobDispatchingUsed();
		priorJobDispatchingRule = ocsInfoManager.getPriorJobDispatchingRule();
		priorJobPriorityThreshold = ocsInfoManager.getPriorJobPriorityThreshold();
		priorJobWaitingTimeThreshold = ocsInfoManager.getPriorJobWaitingTimeThreshold();
		// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
		isIgnoreMaterialDifference = ocsInfoManager.isIgnoreMaterialDifference();
		// 2021.04.09 by YSJ : Park Command add Eq Option
		isParkCmdEqoption = ocsInfoManager.isParkCmdEqoption();
		// 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
		isStageSourceDupCancelUsage = ocsInfoManager.isStageSourceDupCancelUsage();
		// 2022.12.14 by JJW : Area 내  Max VHL 초과시  Manual VHL 제외 여부
		isAreaBalancingManualExclude = ocsInfoManager.isAreaBalancingManualExclude();
		costSearch.updateOperationalParameters();
		// 2023.02.20 add by YSJ for FAB QR Tag 부모Node 로의 Load -> Unload 불가로 NextNode 부터 CostSearch 하게 추가
		vehicleCommType = ocsInfoManager.getVehicleCommType();
	}
	
	/**
	 * Step 1-3 Update VEHICLE and TRCMD from DB
	 */
	private void updateVehicleAndTrCmdFromDB() {
		// Step 1-3-1 Update VEHICLE from DB
		vehicleManager.updateFromDBForJobAssign();
		
		// Step 1-3-2 Update TRCMD from DB
		trCmdManager.updateFromDBForJobAssign();
		
		// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
		parkManager.updateParkPortCount();
	}
	
	/**
	 * Step 1-4 Get Vehicles and TrCmds
	 * 			[IdleVehicle, LoadVehicle, LocalVehicle, IdleLocalVehicle, LocateVehicle / UnassignedTrCmd, AssignedTrCmd]
	 * 
	 */
	private void getVehiclesAndTrCmds() {
		// Step 1-4-1 Get VehicleList [enabledVehicleList, vehicleList]
		// 		enabledVehicleList: All Enabled Vehicles
		// 		vehicleList: Normal Auto Enabled Vehicles
		getVehicleList();
		
		// Step 1-4-2 Get TrCmd List [assignedTrCmdList, assignedVehicleIdList, reservedVehicleIdList]
		// 		assignedTrCmdList: TrCmds whose AssignedVehicle is not null in TRCMD
		// 		assignedVehicleIdList: VehicleIds in TRCMD's Vehicle  
		// 		reservedVehicleIdList: VehicleIds in TRCMD's AssignedVehicle and TRCMD's Vehicle is null 
		getTrCmdList();
		
		// Step 1-4-3 Classify Vehicles [idleVehicleList, localVehicleList, localGroupIdList]
		//		idleVehicleList (1)
		//		localVehicleList
		//		localGroupIdList
		classifyVehicles();
		
		// Step 1-4-4 Classify TrCmds and LocatingVehicles [unassignedTrCmdList, idleVehicleList, loadVehicleList, destLocOfUnloadedVehicleList, locateVehicleList, locateNodeIdMap, idleLocalVehicleList]
		//		unassignedTrCmdList
		//		idleVehicleList (2)
		//		loadVehicleList
		//		destLocOfUnloadedVehicleList
		//		locateVehicleList
		//		locateNodeIdMap
		//		idleLocalVehicleList
		classifyTrCmdsAndVehicles();
	}
	
	/**
	 * Step 1-5 작업 할당을 위해 Vehicle과 TrCmd List를 정리. (PortDuplicatedTrCmd 정리)
	 * 
	 */
	private void refinePortDuplicatedTrCmds() {
		try {
			// Unloaded 이전 작업이 있는 경우, 동일 SourceLoc에 대한 작업 할당 제한.
			// 동일 포트의 작업은 가장 먼저 생성된 작업을 제외하고 작업 후보군에서 제거함
			// 실제 현장에서는 발생하지 않으나 Simulation을 수행할 때 발생가능하므로 옵션처리가 가능하도록 함
			if (isRefinePortDupTrCmdUsed) {
				unassignedTrCmdMapBySourceLoc.clear();
				sourceDupTrCmdList.clear();
				
				String trCmdId = "";
				String duplicatedTrCmdId = "";
				String sourceNode = "";
				String sourceLoc = "";
				String trQueuedTime = "";
				String duplicatedTrQueuedTime = "";
				TrCmd trCmd = null;
				TrCmd duplicatedTrCmd = null;
				Iterator<TrCmd> itRequested = assignedTrCmdList.iterator();
				while (itRequested.hasNext()) {
					trCmd = itRequested.next();
					if (trCmd != null) {
						sourceLoc = trCmd.getSourceLoc();
						if (sourceLoc != null && sourceLoc.length() == 0) {
							sourceLoc = trCmd.getSourceNode();
						}
						switch (trCmd.getDetailState()) {
							case NOT_ASSIGNED:
							case UNLOAD_ASSIGNED:
							case UNLOAD_SENT:
							case UNLOAD_ACCEPTED:
							case UNLOADING:
							case SCAN_ASSIGNED:
							case STAGE_ASSIGNED:
							case MAPMAKE_ASSIGNED:
							case PATROL_ASSIGNED:
								// UNLOADED 하지 않은 작업과 동일한 SourceLoc의 작업은 중복 체크 필요.
								// STK에서 순서 지키기.
								unassignedTrCmdMapBySourceLoc.put(sourceLoc, trCmd);
								break;
							default:
								break;
						}
					}
				}
				
				int duplicatedTrCmdCount = 0;
				Iterator<TrCmd> it = unassignedTrCmdList.iterator();
				while (it.hasNext()) {
					trCmd = it.next();
					if (trCmd != null) {
						trCmdId = trCmd.getTrCmdId();
						sourceLoc = trCmd.getSourceLoc();
						sourceNode = trCmd.getSourceNode();
						if (sourceLoc != null && sourceLoc.length() == 0) {
							sourceLoc = sourceNode;
						}
						
						// 2014.02.19 by MYM : [Stage Locate 기능] Stage인 경우는 중복 작업할당 가능하도록 함.
						if (isStageLocateUsage && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
							ArrayList<TrCmd> stageList = sortedStageAtSamePortMap.get(trCmd.getSourceNode());
							if (stageList.indexOf(trCmd) >= stageLocateVehicleCount) {
								duplicatedTrCmd = stageList.get(0);
								if (duplicatedTrCmd != null) {
									sourceDupTrCmdList.add(trCmd);
									duplicatedTrCmdCount++;
									
									StringBuilder message = new StringBuilder();
									message.append("[Existing Job:] Removed Same SourceLoc Job. StageCmd:");
									message.append(duplicatedTrCmd.getTrCmdId()).append("(");
									message.append(duplicatedTrCmd.getTrQueuedTime()).append(") & ");
									message.append(trCmdId).append("(").append(trCmd.getTrQueuedTime()).append(")");
									message.append(", SourceLoc:").append(sourceLoc);
									message.append(", SourceNode:").append(sourceNode).append(", DuplicatedTrCmd:");
									message.append(trCmdId);
									message.append(" Removed.");
									traceJobAssignMain(message.toString());
								}
							}
						} else {
							if (unassignedTrCmdMapBySourceLoc.containsKey(sourceLoc)) {
								trQueuedTime = trCmd.getTrQueuedTime();
								duplicatedTrCmd = unassignedTrCmdMapBySourceLoc.get(sourceLoc);
								if (duplicatedTrCmd != null) {
									duplicatedTrCmdId = duplicatedTrCmd.getTrCmdId();
									duplicatedTrQueuedTime = duplicatedTrCmd.getTrQueuedTime();
									
									StringBuilder message = new StringBuilder();
									message.append("[Existing Job:] Removed Same SourceLoc Job. TrCmd:");
									message.append(duplicatedTrCmdId).append("(");
									if (assignedTrCmdList.contains(duplicatedTrCmd)) {
										message.append(duplicatedTrCmd.getDetailState().toConstString()).append(", ");
									}
									message.append(duplicatedTrQueuedTime).append(") & ");
									message.append(trCmdId).append("(").append(trQueuedTime).append(")");
									message.append(", SourceLoc:").append(sourceLoc);
									message.append(", SourceNode:").append(sourceNode).append(", DuplicatedTrCmd:");
									
									if (trQueuedTime.compareTo(duplicatedTrQueuedTime) < 0
											&& assignedTrCmdList.contains(duplicatedTrCmd) == false) {
										unassignedTrCmdMapBySourceLoc.put(sourceLoc, trCmd);
										sourceDupTrCmdList.add(duplicatedTrCmd);
										message.append(duplicatedTrCmdId);
									} else {
										sourceDupTrCmdList.add(trCmd);
										message.append(trCmdId);
									}
									message.append(" Removed.");
									traceJobAssignMain(message.toString());
									duplicatedTrCmdCount++;
								}
							} else {
								unassignedTrCmdMapBySourceLoc.put(sourceLoc, trCmd);
							}
						}
					}
				}
				for (TrCmd sourceDupTrCmd : sourceDupTrCmdList) {
					unassignedTrCmdList.remove(sourceDupTrCmd);
				}
				if (duplicatedTrCmdCount > 0) {
					traceJobAssignMain("SourceLoc Duplicated TrCmd: " + duplicatedTrCmdCount + " (Removed).");
				}
			}
		} catch (Exception e) {
			traceJobAssignException("", e);
		}
	}
	
	/**
	 * Step 1-6 Update Vehicle Counts per Area for AreaBalancing
	 * 
	 */
	private void updateAreaVehicleCounts(){
		areaList = areaManager.getData();
		if (areaList.size() > 0 && isAreaBalancingUsed) {
			areaManager.initializeAreaData();
			
			Vehicle vehicle = null;
			Node stopNode = null;
			Node targetNode = null;
			Area stopNodeArea = null;
			Area targetNodeArea = null;
			Set<String> searchKeys = new HashSet<String>(enabledVehicleList.keySet());
			for (String searchKey : searchKeys) {
				vehicle = (Vehicle)enabledVehicleList.get(searchKey);
				if (vehicle != null) {
					try {
						stopNode = (Node)nodeManager.getNode(vehicle.getStopNode());
						if (stopNode != null) {
							stopNodeArea = stopNode.getArea();
						} else {
							stopNodeArea = null;
						}
						
						targetNode = (Node)nodeManager.getNode(vehicle.getTargetNode());
						if (targetNode != null) {
							targetNodeArea = targetNode.getArea();
						} else {
							targetNodeArea = null;
						}
						if (stopNodeArea == targetNodeArea) {
							if (stopNodeArea != null) {
								stopNodeArea.addStayingVehicle(vehicle);
								
								if (idleVehicleList.contains(vehicle) &&
										localVehicleList.contains(vehicle) == false) {
									stopNodeArea.addStayingIdleVehicle(vehicle);
								}
							}
						} else {
							if (stopNodeArea != null) {
								stopNodeArea.addOutGoingVehicle(vehicle);
								if (idleVehicleList.contains(vehicle) &&
										localVehicleList.contains(vehicle) == false) {
									stopNodeArea.addOutGoingIdleVehicle(vehicle);
								}
							}
							if (targetNodeArea != null) {
								targetNodeArea.addInComingVehicle(vehicle);
								if (idleVehicleList.contains(vehicle) &&
										localVehicleList.contains(vehicle) == false) {
									targetNodeArea.addInComingIdleVehicle(vehicle);
								}
							}
						}
						if (targetNodeArea != null) {
							if (loadVehicleList.contains(vehicle)) {
								targetNodeArea.addLoadVehicle(vehicle);
							}
						}
					} catch (Exception e) {
						traceJobAssignException("StopNode:" + vehicle.getStopNode() + ", TargetNode:" + vehicle.getTargetNode(), e);
					}
				}
			}
			TrCmd trCmd = null;
			String sourceNodeId = "";
			Node sourceNode = null;
			Area sourceArea = null;
			Iterator<TrCmd> it = unassignedTrCmdList.iterator();
			while (it.hasNext()) {
				trCmd = it.next();
				if (trCmd != null) {
					sourceNodeId = trCmd.getSourceNode();
					if (sourceNodeId != null && sourceNodeId.length() > 0) {
						sourceNode = nodeManager.getNode(sourceNodeId);
						if (sourceNode != null) {
							sourceArea = sourceNode.getArea();
							if (sourceArea != null) {
								sourceArea.addUnassignedTrCmd(trCmd);
							}
						}
					}
				}
			}
			String destNodeId = "";
			Node destNode = null;
			Area destArea = null;
			Set<String> searchKeysTrCmd = new HashSet<String>(trCmdList.keySet());
			for (String searchKey : searchKeysTrCmd) {
				trCmd = (TrCmd)trCmdList.get(searchKey);
				if (trCmd != null) {
					destNodeId = trCmd.getDestNode();
					if (destNodeId != null && destNodeId.length() > 0) {
						destNode = nodeManager.getNode(destNodeId);
						if (destNode != null) {
							destArea = destNode.getArea();
							if (destArea != null) {
								destArea.addDestTrCmd(trCmd);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Step 1-7 Update SearchLimit Check Count
	 * 
	 */
	private void updateSearchLimitCheckCount() {
		if (searchLimitCheckCount > MAX_SEARCH_LIMIT_CHECK_COUNT) {
			searchLimitCheckCount = 0;
		} else {
			searchLimitCheckCount++;
		}
	}
	
	/**
	 * 2015.02.24 by MYM : 장애 지역 우회 기능
	 * Step 1-8 Update link & Section Enabled
	 */
	private void updateLinkAndSectionFromDB() {
		sectionManager.updateChangedLinkInfo();
		sectionManager.updateSectionFromDBForJobAssign();
	}
	
	/**
	 * Step 2-1 Classify LocalGroups into releaseGroupList/setGroupList
	 * 
	 */
	private void classifyLocalGroup() {
		assert isLocalOHTUsed;
		
		releaseLocalGroupIdList.clear();
		setLocalGroupInfoList.clear();
		
		localGroupInfoManager.updateLocalGroupInfo();
		localGroupCurVHL = vehicleManager.getLocalGroupCurrVHL();
		localGroupInfoList = localGroupInfoManager.getData();
		materialIndexMapOfLocalGroup = carrierLocManager.getMaterialIndexMapOfLocalGroup();
		
		if (localGroupCurVHL != null && localGroupInfoList != null) {
			LocalGroupInfo localGroupInfo = null;
			Set<String> searchKeys = new HashSet<String>(localGroupInfoList.keySet());
			for (String searchKey : searchKeys) {
				localGroupInfo = (LocalGroupInfo)localGroupInfoList.get(searchKey);
				if (localGroupInfo != null) {
					if (localGroupCurVHL.containsKey(searchKey)) {
						if (localGroupInfo.getSetVHL() - localGroupCurVHL.get(searchKey) > 0) {
							if (setLocalGroupInfoList.contains(localGroupInfo) == false) {
								setLocalGroupInfoList.add(localGroupInfo);
							}
						} else if (localGroupInfo.getSetVHL() - localGroupCurVHL.get(searchKey) < 0) {
							if (releaseLocalGroupIdList.contains(localGroupInfo.getLocalGroupId()) == false) {
								releaseLocalGroupIdList.add(localGroupInfo.getLocalGroupId());
							}
						}
					} else {
						if (localGroupInfo.getSetVHL() > 0) {
							if (setLocalGroupInfoList.contains(localGroupInfo) == false) {
								setLocalGroupInfoList.add(localGroupInfo);
							}
						}
					}
				}
			}
			
			Set<String> searchReleaseKeys = new HashSet<String>(localGroupCurVHL.keySet());
			for (String searchKey : searchReleaseKeys) {
				if (localGroupInfoList.containsKey(searchKey) == false) {
					if (releaseLocalGroupIdList.contains(searchKey) == false) {
						releaseLocalGroupIdList.add(searchKey);
					}
				}
			}
		}
	}
	
	/**
	 * Step 2-2 Release LocalOHT(s) of LocalGroup(s) in releaseLocalGroupList
	 * 
	 */
	private void releaseLocalOHT() {
		assert isLocalOHTUsed;
		
		try {
			if (releaseLocalGroupIdList.size() > 0) {
				int count = 0;
				StringBuilder message = new StringBuilder();
				String releaseLocalGroupId = "";
				ListIterator<String> it = releaseLocalGroupIdList.listIterator();
				message.append("LocalGroupID is released. LocalGroupID:");
				while (it.hasNext()) {
					releaseLocalGroupId = it.next();
					if (releaseLocalGroupId != null && releaseLocalGroupId.length() > 0) {
						
						// Step 2-2-1 Release LocalOHT(s) in the LocalGroup
						releaseLocalOHTInLocalGroup(releaseLocalGroupId);
						
						message.append(releaseLocalGroupId);
						if (++count < releaseLocalGroupIdList.size()) message.append(", ");
					}
				}
				traceJobAssignMain(message.toString());
			}
		} catch (Exception e) {
			traceJobAssignException("", e);
		}
	}
	
	/**
	 * Step 2-3 Set LocalOHT(s) of LocalGroup(s) in setLocalGroupList
	 * 
	 */
	private void setLocalOHT() {
		assert isLocalOHTUsed;

		try {
			if (setLocalGroupInfoList.size() > 0) {
				traceJobAssignMain("[LocalOHT Assign Request-Started]");
				
				// Step 2-3-1 LocalOHT로 선정할 일반 Idle VHL이 없으면 더 이상 진행하지 않음.
				if (idleVehicleList.size() - idleLocalVehicleList.size() <= 0) {
					traceJobAssignMain("[LocalOHT Assign Request-Finished] - IdleVehicleSize=0");
					return;
				}
				
				// Step 2-3-2 LocalOHT 선정을 위한 후보 Vehicle, 대상 Bay 선택 
				selectVehiclesAndLocalGroups();
				
				// Step 2-3-3 Print Pattern for LocalGroupAllocation
				printPatternForLocalGroupAllocation();
				
				// Step 2-3-4 Initialize Cost Variables for LocalGroupAllocation
				initializeCostForLocalGroupAllocation();
				
				// Step 2-3-5 Calculate Cost for LocalGroupAllocation
				calculateCostForLocalGroupAllocation();
				
				// Step 2-3-6 LocalOHT 선정
				allocateLocalGroup();
				
				traceJobAssignMain("[LocalOHT Assign Request-Finished] - END");
			}
		} catch (Exception e) {
			traceJobAssignException("", e);
		}
	}
	
	/**
	 * Step 2-4 Clear LocalGroupInfo [LOCARLGROUPINFO, VEHICLE, CARRIERLOC]
	 */
	private void clearLocalGroupInfoFromDB() {
		if (localGroupInfoManager.isAllLocalGroupInfoCleared() == false) {
			localGroupInfoManager.clearAllLocalGroupInfo();
		}
		if (vehicleManager.isAllLocalGroupInfoCleared() == false) {
			vehicleManager.clearAllLocalGroupId();
		}
		if (carrierLocManager.isAllLocalGroupIdCleared() == false) {
			carrierLocManager.clearAllLocalGroupId();
		}
	}
	
	
	/**
	 * Step 3-1 Select Candidate Vehicles [Idle Local Vehicle / Idle General Vehicle / Load Vehicle]
	 * 
	 */
	private void selectCandidateVehicles() {
		tempCandidateVehicleList.clear();
		candidateVehicleList.clear();
		targetLocalGroupList.clear();
		
		idleLocalCandidateVehicleCount = 0;
		idleGeneralCandidateVehicleCount = 0;
		
		Vehicle vehicle = null;
		Iterator<Vehicle> itIdleLocal = idleLocalVehicleList.iterator();
		while (itIdleLocal.hasNext()) {
			vehicle = itIdleLocal.next();
			if (vehicle != null) {
				if (candidateVehicleList.contains(vehicle) == false) {
					candidateVehicleList.add(vehicle);
					idleLocalCandidateVehicleCount++;
				}
			}
		}
		
		Iterator<Vehicle> itIdle = idleVehicleList.iterator();
		while (itIdle.hasNext()) {
			vehicle = itIdle.next();
			if (vehicle != null) {
				if (candidateVehicleList.contains(vehicle) == false) {
					candidateVehicleList.add(vehicle);
					idleGeneralCandidateVehicleCount++;
				}
			}
		}
		
		Iterator<Vehicle> itLoad = loadVehicleList.iterator();
		while (itLoad.hasNext()) {
			vehicle = itLoad.next();
			if (vehicle != null) {
				if (candidateVehicleList.contains(vehicle) == false) {
					candidateVehicleList.add(vehicle);
				}
			}
		}
	}
	
	/**
	 * Step 3-2 Select TargetNodes [Local Job / Job / Park]
	 * 
	 */
	private void selectTargetNodes() {
		
		// Step 3-2-1 Select Local TrCmds
		selectLocalTrCmds();
		
		// Step 3-2-2 Select Unassigned TrCmds and Build TargetTrCmdList & TargetNodeList 
		selectUnassignedTrCmds();
		
		// Step 3-2-3 Select Park Nodes and Build TargetNodeList
		selectParkNodes();
	}
	
	/**
	 * 2013.05.16 by KYK
	 * Step 3-3 Initialize Cost for Target [costNodes, costNodesBackup]  
	 * 
	 */
	private void initializeCostForTarget() {
		if (candidateVehicleList.size() > targetList.size()) {
			size = candidateVehicleList.size();
		} else {
			size = targetList.size();
		}
		costNodes = new double[size][size];
		costNodesBackup = new double[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				costNodes[i][j] = MAXCOST_TIMEBASE;
				costNodesBackup[i][j] = 0;
			}
		}
	}
	
	/**
	 * 2013.05.16 by KYK
	 * Step 3-4 Calculate Cost for Target Nodes  
	 */
	private void calculateCostForTarget() {
		costSearchStartTime = System.currentTimeMillis();
		
		costSearch.initCostSearchInfo(SEARCH_TYPE.NODE);
		
		// 2. 각 Vehicle 별  Job/Park (Node)에 대한 Cost 계산.
		Vehicle vehicle = null;
		Node stopNode = null;
		String targetId = "";
		String targetNodeId = "";
		boolean isCostSearchFromStopNode;
		for (int i = 0; i < candidateVehicleList.size(); i++) {
			vehicle = candidateVehicleList.get(i);
			if (vehicle != null) {
				stopNode = (Node)nodeManager.getNode(vehicle.getStopNode());
				if (stopNode != null) {
					// 2014.02.27 by MYM : [Stage Locate 기능]
//					if (loadVehicleList.contains(vehicle) || locateVehicleList.contains(vehicle)) {
//					if (loadVehicleList.contains(vehicle) || locateVehicleList.contains(vehicle) || stageLocateVehicleList.contains(vehicle)) {
					if (loadVehicleList.contains(vehicle) || locateVehicleList.contains(vehicle)) {
						isCostSearchFromStopNode = false;
					} else if (isStageLocateUsage && vehicle.getRequestedType() == REQUESTEDTYPE.STAGE) {
						isCostSearchFromStopNode = false;
					} else {
						isCostSearchFromStopNode = true;
					}
					
					// isCostSearchFromStopNode: CurrNode -> StopNode -> SourceNode/ParkNode와 CurrNode -> StopNode -> TargetNode -> SourceNode/ParkNode 구분.
					// 20221208 by y.won 
					boolean updateAvailableNodeListResult = updateAvailableNodeList(vehicle);
					boolean costSearchReseult = costSearch.costSearch(vehicle, stopNode, costMap, targetStationMap, isCostSearchFromStopNode, searchLimit, costSearchOption, vehicleCommType);
					if (updateAvailableNodeListResult == false || costSearchReseult) {
						for (int j = 0; j < targetList.size(); j++) {
							targetId = targetList.get(j);
							// costNodesBackup[i][j] > 0 이면 Assign or Park 불가 노드임.
							// updateAvailableSourceNodeList(vehicle, i)에서 체크한 결과임.
							if (costMap.containsKey(targetId) && costNodesBackup[i][j] == 0) {
								costNodes[i][j] = (double)costMap.get(targetId);
							} else {
								// costNodes[i][j] = MAXCOST_TIMEBASE;
								;/*NULL*/
							}
							
							// 2014.02.28 by MYM : [Stage Locate 기능]
							if (isStageLocateUsage && j < targetTrCmdList.size() && costNodes[i][j] < MAXCOST_TIMEBASE) {
								TrCmd trCmd = targetTrCmdList.get(j);
								if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
									targetNodeId = targetNodeList.get(j);
									StageTarget item = unavailableStageTargetMap.get(targetNodeId);
									if (item != null && item.isAvailable() == false) {
										item.setAvailable(true);
									}
								}
							}
						}
					} else {
						traceJobAssignMain("     [Cost Search Failed]: " + vehicle.getVehicleId());
					}
					
					if (costSearchReseult == false) {
						// 20221208 by y.won node station 검증 절차 추가 알람 등록 부분
						registerAlarmWithLevel("JobAssign " + vehicle.getVehicleId(), "Abnormal Station Error: curr: " + vehicle.getCurrStation() + " stop: " + vehicle.getStopStation() + " target: " + vehicle.getTargetStation(), ALARMLEVEL.ERROR);
						traceJobAssignMain("     [Cost Search Failed by Node and Station Validation]: " + vehicle.getVehicleId());
						try {
							traceJobAssignMain("     <Node> curr: " + vehicle.getCurrNode() + " stop: " + vehicle.getStopNode() + " target: " + vehicle.getTargetNode());
							traceJobAssignMain("     <Station> curr: " + vehicle.getCurrStation() + " stop: " + vehicle.getStopStation() + " target: " + vehicle.getTargetStation());
						} catch (Exception e) {
							traceJobAssignMain("     one of curr/stop/target Node and Station is null" );
						}
					} else { // 20230126 by y.won 알람 등록 조건 해제 시 알람 삭제
						if (alarmManager.isAlarmRegistered("JobAssign " + vehicle.getVehicleId())) {
							alarmManager.unregisterAllAlarm("JobAssign "+ vehicle.getVehicleId());
						}
					}
					
				}
			}
		}
		
		// 2014.02.28 by MYM : [Stage Locate 기능]
		Iterator<String> iter = unavailableStageTargetMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			StageTarget item = unavailableStageTargetMap.get(key); 
			if (item != null && item.isAvailable()) {
				iter.remove();
			}
		}
		
		costSearchedTime = System.currentTimeMillis();
		long costSearchTime = costSearchedTime - costSearchStartTime;
		if (candidateVehicleList.size() > 0 && targetList.size() > 0) {
			traceJobAssignMain("     [Time-CostSearch] " + costSearchTime + "[msec]");
		}
		
		if (costSearchTime > jobAssignDelayLimit) {
			StringBuilder message = new StringBuilder();
			message.append("     [Cost Search] Vehicle Count:").append(candidateVehicleList.size());
			message.append(", LocalGroup Count:").append(targetLocalGroupList.size());
			message.append(", DelayTime:").append(costSearchTime).append("[msec]");
			traceJobAssignDelay(message.toString());
		}
	}
	
	/**
	 * Step 4-1 Get Cost for LocalJob Allocation 
	 * 
	 */
	private void getCostForLocalJob() {
		assert localTrCmdCount > 0;
		assert idleLocalCandidateVehicleCount > 0;
		assert isLocalOHTUsed;
		
		if (localTrCmdCount > idleLocalCandidateVehicleCount) {
			size = localTrCmdCount;
		} else {
			size = idleLocalCandidateVehicleCount;
		}
		cost = new double[size][size];
		costBackup = new double[size][size];
		result = new int[size];
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				cost[i][j] = MAXCOST_TIMEBASE;
				costBackup[i][j] = MAXCOST_TIMEBASE;
			}
			result[i] = size;
		}
		
		Vehicle vehicle = null;
		Node stopNode = null;
		for (int i = 0; i < idleLocalCandidateVehicleCount; i++) {
			vehicle = candidateVehicleList.get(i);
			if (vehicle != null) {
				stopNode = nodeManager.getNode(vehicle.getStopNode());
				if (stopNode != null) {
					if (stopNode.getBay().equals(localGroupInfoManager.getBay(vehicle.getLocalGroupId()))) {
						for (int j = 0; j < localTrCmdCount; j++) {
							cost[i][j] = costNodes[i][j];
							costBackup[i][j] = cost[i][j];
						}
						continue;
					}
				}
			}
			for (int j = 0; j < localTrCmdCount; j++) {
				cost[i][j] = MAXCOST_TIMEBASE;
				costBackup[i][j] = MAXCOST_TIMEBASE;
			}
		}
	}
	
	/**
	 * Step 4-2 Print LocalJob Pattern and Cost 
	 * 
	 * @return
	 */
	private void printPatternAndCostForLocalJob() {
		assert localTrCmdCount > 0;
		assert idleLocalCandidateVehicleCount > 0;
		assert isLocalOHTUsed;
		
		Vehicle vehicle = null;
		StringBuilder vehiclePattern = new StringBuilder();
		vehiclePattern.append("     [LocalVehicle Pattern] <").append(makeCountString((idleLocalCandidateVehicleCount))).append(" Vehicle>\t");
		Iterator<Vehicle> itVehiclePattern = idleLocalVehicleList.iterator();
		while (itVehiclePattern.hasNext()) {
			vehicle = itVehiclePattern.next();
			if (vehicle != null) {
				vehiclePattern.append(vehicle.getVehicleId());
			}
			if (itVehiclePattern.hasNext()) {
				vehiclePattern.append(" ");
			}
		}
		traceJobAssignMain(vehiclePattern.toString());
		
		TrCmd trCmd = null;
		StringBuilder jobTrCmdIdPattern = new StringBuilder();
		StringBuilder jobLocalGroupIdPattern = new StringBuilder();
		jobTrCmdIdPattern.append("     [LocalJob Pattern]  <").append(makeCountString(localTrCmdCount)).append(" Job>\t");
		jobLocalGroupIdPattern.append("     [LocalJob Pattern]  <").append(makeCountString(localTrCmdCount)).append(" Job>\t");
		Iterator<TrCmd> itJobPattern = localTrCmdList.iterator();
		while (itJobPattern.hasNext()) {
			trCmd = itJobPattern.next();
			if (trCmd != null) {
				jobTrCmdIdPattern.append(trCmd.getTrCmdId());
				jobLocalGroupIdPattern.append(makeStringWithBlanks(String.valueOf(localTrCmdGroupIdMap.get(trCmd)), 6, false));
			}
			if (itJobPattern.hasNext()) {
				jobTrCmdIdPattern.append(" ");
				jobLocalGroupIdPattern.append(" ");
			}
		}
		traceJobAssignMain(jobTrCmdIdPattern.toString());
		traceJobAssignMain(jobLocalGroupIdPattern.toString());
		
		if (isJobAssignDetailResultUsed) {
			for (int i = 0; i < idleLocalCandidateVehicleCount; i++) {
				vehicle = candidateVehicleList.get(i);
				if (vehicle != null) {
					StringBuilder message = new StringBuilder();
					message.append(vehicle.getVehicleId()).append("(F)\t");
					for (int j = 0; j < localTrCmdCount; j++) {
						message.append(getCostInSixDigits(cost[i][j]));
						if (j < localTrCmdCount - 1) {
							message.append(", ");
						}
					}
					traceJobAssignMain("     [Cost for LocalJob] " + message.toString());
				}
			}
		}
	}
	
	/**
	 * Step 4-3 Allocate LocalJob to InBay LocalOHT 
	 * 
	 * @return
	 */
	private void allocateLocalJob() {
		assert localTrCmdCount > 0;
		assert idleLocalCandidateVehicleCount > 0;
		assert isLocalOHTUsed;

		jobAssignResultSet.clear();
		
		allocateStartedTime = System.currentTimeMillis();
		try {
			if (solver.solveAssignment(size, cost, result)) {
				double modelingCost;
				double distanceBasedCost;
				Vehicle vehicle = null;
				TrCmd trCmd = null;
				JobAssignResult jobAssignResult = null;
				for (int i = 0; i < idleLocalCandidateVehicleCount; i++) {
					if (result[i] < localTrCmdCount) {
						vehicle = candidateVehicleList.get(i);
						trCmd = targetTrCmdList.get(result[i]);
						if (vehicle == null || trCmd == null) {
							continue;
						}
						modelingCost = cost[i][result[i]];
						distanceBasedCost = costBackup[i][result[i]];
						if (modelingCost >= OcsConstant.MAXCOST_TIMEBASE) {
							continue;
						}
						
						jobAssignResult = new JobAssignResult(vehicle, trCmd, modelingCost, distanceBasedCost);
						if (jobAssignResultSet.contains(jobAssignResult) == false) {
							jobAssignResultSet.add(jobAssignResult);
						}
					}
				}
				
				Iterator<JobAssignResult> itResult = jobAssignResultSet.iterator();
				while (itResult.hasNext()) {
					jobAssignResult = itResult.next();
					if (jobAssignResult != null) {
						vehicle = jobAssignResult.getVehicle();
						trCmd = (TrCmd)jobAssignResult.getTarget();
						modelingCost = jobAssignResult.getModelingCost();
						distanceBasedCost = jobAssignResult.getDistanceBasedCost();
						
						if (resultVehicleList.contains(vehicle) == false &&
								resultTargetList.contains(trCmd) == false) {
							if (idleLocalVehicleList.contains(vehicle)) {
								if (isStageLocateUsage && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
									// 2014.02.18 by MYM : [Stage Locate 기능]
									reserveVehicleToStage(vehicle, trCmd, modelingCost, distanceBasedCost);
								} else if (locateVehicleList.contains(vehicle) == false) {
									allocateVehicleToJob(vehicle, trCmd, modelingCost, distanceBasedCost);
								} else {
									reserveVehicleToJob(vehicle, trCmd, modelingCost, distanceBasedCost);
								}
							} else {
								StringBuilder message = new StringBuilder();
								message.append("[Abnormal Case] Vehicle:").append(vehicle.getVehicleId());
								message.append(", idleLocalVehicleList:").append(idleLocalVehicleList.toString());
								traceJobAssignException(message.toString());
							} 
							
						} else {
							StringBuilder message = new StringBuilder();
							message.append("[Abnormal Case] Vehicle:").append(vehicle.getVehicleId());
							message.append(", ResultVehicleList:").append(resultVehicleList.toString());
							message.append(", ResultTargetList:").append(resultTargetList.toString());
							traceJobAssignException(message.toString());
						}
					}
				}
			} else {
				traceJobAssignMain("     [AP_Failed] AP_Infinite_Loop");
			}
		} catch (Exception e) {
			traceJobAssignException("     [AP_Exception] ", e);
		}
		
		allocateCompletedTime = System.currentTimeMillis();
		printAllocationCompleted(allocateCompletedTime - allocateStartedTime);
	}
	
	/**
	 * Step 5-1 Get Cost for Job Allocation 
	 * 
	 */
	private void getCostForJob() {
		candidateVehicleListForJobAllocation.clear();
		targetTrCmdListForJobAllocation.clear();
		
		Vehicle vehicle = null;
		TrCmd trCmd = null;
		TrCmd nextReservedTrCmd = null;
		
		int candidateCount = candidateVehicleList.size() - resultVehicleList.size();
		int targetCount = targetTrCmdList.size() - resultTargetList.size();
		boolean isTargetUpdated = false;
		
		if (candidateCount > targetCount) {
			size = candidateCount;
		} else {
			size = targetCount;
		}
		cost = new double[size][size];
		costBackup = new double[size][size];
		result = new int[size];
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				cost[i][j] = MAXCOST_TIMEBASE;
				costBackup[i][j] = MAXCOST_TIMEBASE;
			}
			result[i] = size;
		}
		HashMap<String, Vehicle> reservedStageSourceLocMap = new HashMap<String, Vehicle>();
		candidateCount = 0;		
		for (int i = 0; i < candidateVehicleList.size(); i++) {
			vehicle = candidateVehicleList.get(i);
			if (vehicle != null) {
				if (resultVehicleList.contains(vehicle) == false) {
					if (candidateVehicleListForJobAllocation.contains(vehicle) == false) {
						candidateVehicleListForJobAllocation.add(vehicle);
					}
					targetCount = 0;
					
					// 2015.03.17 by KYK [Job Reservation Option]
					if (jobReservationOption != JOB_RESERVATION_OPTION.JR0 && reservedVehicleMap.containsKey(vehicle)) {
//					if (reservedVehicleMap.containsKey(vehicle)) {
						nextReservedTrCmd = (TrCmd)reservedVehicleMap.get(vehicle);
						// 2015.02.25 by KYK TODO : nextJob 이 targetTrCmdList 있는지 체크
						if (targetTrCmdList.contains(nextReservedTrCmd) == false) {
							reservedVehicleMap.remove(vehicle);
						}
					} else {
						nextReservedTrCmd = null;
					}
					
					// 2014.02.26 by MYM : [Stage Locate 기능] Stage 위치에서 대기하는 Vehicle은 다른 작업(Transfer)를 받지 않도록 함. 
					TrCmd reservedStage = null;
					Node stageTargetNode = null;
					if (isStageLocateUsage) {
						reservedStage = getReservedStageTrCmd(vehicle);
						if (reservedStage != null) {
							stageTargetNode = stageTargetNodeMap.get(reservedStage);
							String sourceLoc = reservedStage.getSourceLoc();
							if (sourceLoc != null) {
								reservedStageSourceLocMap.put(sourceLoc, vehicle);
							}
						}
					}
					
					for (int j = 0; j < targetTrCmdList.size(); j++) {
						trCmd = targetTrCmdList.get(j);
						if (trCmd != null) {
							if (resultTargetList.contains(trCmd) == false) {
								if (isTargetUpdated == false &&
										targetTrCmdListForJobAllocation.contains(trCmd) == false) {
									targetTrCmdListForJobAllocation.add(trCmd);
								}
								cost[candidateCount][targetCount] = costNodes[i][j];
								if (cost[candidateCount][targetCount] > jobAssignSearchLimit) {
									cost[candidateCount][targetCount] = MAXCOST_TIMEBASE;
								}
								costBackup[candidateCount][targetCount] = cost[candidateCount][targetCount];
								if (nextReservedTrCmd != null &&
										trCmd != nextReservedTrCmd &&
										cost[candidateCount][targetCount] < 8000) {
									cost[candidateCount][targetCount] += 1000;
								}
								
								// 2015.03.04 by MYM : 위치 이동 - 작업 할당시(allocateVehicleToJob) costBackup(distance-based cost)의 값 기준으로 jobAssignThreshold 조건을 보는데 디폴트가 10으로 되어 있어서 Locate Vehicle 할당이 처리가 안됨
								// 2014.05.31 by KYK : [Stage Locate 기능] Stage 위치에서 대기하는 Vehicle이 StageChange Transfer를 잡기 위해서
								// Stage Cost보다 낮아야 하기 때문에 모든 반송의 기본 Cost를 10부터 설정하고 아래에서 StageChange Transfer는 10보다 낮게 설정함.
								if (isStageLocateUsage) {
									cost[candidateCount][targetCount] += 10; // default cost 부여
								}
								
								// 2014.02.26 by MYM : [Stage Locate 기능] Stage 위치에서 대기하는 Vehicle은 다른 작업(Transfer)를 받지 않도록 함.
								if (isStageLocateUsage && reservedStage != null && stageTargetNode != null) {
									String targetNodeId = targetList.get(j);
									if (reservedStage.getSourceNode().equals(trCmd.getSourceNode()) == false) {
										if (targetNodeId.equals(stageTargetNode.getNodeId()) == false) {
											cost[candidateCount][targetCount] = MAXCOST_TIMEBASE;
										}
									}
								}
								targetCount++;
							}
						}
					}
					isTargetUpdated = true;
					candidateCount++;
				}
			}
		}
		
		long waitingTimeOfTrCmd = 0;
		int priorityOfTrCmd = 30;
		double weightOfTrCmd = 0;
		double distanceCost = 0;
		double assignCost = 0;
//		double priorCost = 0;
		ArrayList<Vehicle> stageChangedVehicleList = new ArrayList<Vehicle>();
		ArrayList<PriorJob> priorJobList = new ArrayList<PriorJob>();//TODO priorJob
		for (int j= 0; j < targetTrCmdListForJobAllocation.size(); j++) {
			priorityOfTrCmd = 0;
			waitingTimeOfTrCmd = 0;
			trCmd = targetTrCmdListForJobAllocation.get(j);
			if (trCmd != null) {
				if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.STAGE &&
						(isConveyorHotPriorityUsed == false || (isConveyorType(trCmd.getSourceLoc()) == false && isConveyorType(trCmd.getDestLoc()) == false))) {
					priorityOfTrCmd = getPriorityOfTrCmd(trCmd);
					waitingTimeOfTrCmd = getWaitingTimeOfTrCmd(trCmd);
					weightOfTrCmd = (jobAssignPriorityWeight * priorityOfTrCmd + jobAssignWaitingTimeWeight * waitingTimeOfTrCmd) / jobAssignUrgentThreshold;
					assignCost = getAssignCost(priorityOfTrCmd, waitingTimeOfTrCmd, weightOfTrCmd);
//					priorCost = getPriorCost(trCmd, waitingTimeOfTrCmd);

					if (isPriorJobDispatchingUsed) {
						// 2015.10.01 by KYK : priorJob select TODO
						double priorJobCost = getPriorJobCost(trCmd);
						if (priorJobCost > 0) {
							priorJobList.add(new PriorJob(trCmd, j, priorJobCost));
						}
					}

					double minCost = 9999.0;
					int minCostVehicleIndex = -1;
					for (int i = 0; i < candidateVehicleListForJobAllocation.size(); i++) {
						// cost[i][j] >= 0임.
						cost[i][j] = (cost[i][j] < 0) ? 0 : cost[i][j];
						costBackup[i][j] = (costBackup[i][j] < 0) ? 0 : costBackup[i][j];
						if (cost[i][j] < MAXCOST_TIMEBASE) {
							vehicle = candidateVehicleListForJobAllocation.get(i);
							distanceCost = getDistanceCost(vehicle, cost[i][j], waitingTimeOfTrCmd, weightOfTrCmd);
							cost[i][j] = distanceCost + assignCost;
							if (loadedVehiclePenalty > 0) {
								cost[i][j] = cost[i][j] + loadedVehiclePenalty;
							}
							if (cost[i][j] > MAXCOST_TIMEBASE) {
								cost[i][j] = MAXCOST_TIMEBASE;
							}
							
							// 2014.05.31 by KYK : [Stage Locate 기능] 
							// Stage 대기 Vehicle이 StageChange된 Transfer를 반드시 할당될 수 있도록 함.
							if (isStageLocateUsage && cost[i][j] < minCost) {
								// added by KYK : 이전에 stageChange 호기로 지정된 경우, 그 다음으로 가까운 호기를 찾는다.
								if (stageChangedVehicleList.contains(vehicle) == false) {
									minCost = cost[i][j];
									minCostVehicleIndex = i;									
								}
							}
						}
					}
					
					// 2014.05.31 by KYK : [Stage Locate 기능] 
					// 비슷한 위치 포트에 각각 Stage가 있었는데 거의 동시에 Transfer (StageChange) 된 경우, 두 포트입장에서 가장 가까운 호기가 같을 수 있다.
					// Stage 명령이 다른 Transfer 에 호기를 빼앗기지는 않지만 StageChange된 Tranfer 에게는 빼앗길 수 있다.
					// 따라서, StageChange된 Transfer가 Stage를 위해 대기했던 Vehicle이 반드시 잡을 수 있도록 cost를 5로 설정한다.
					// 현재로서는 이때 포트별 호기매칭의 순서까지 맞춰줄 수는 없다. 이걸할려면 구조가 달라져야 함, 근데 일단 그렇게까지 할 필요는 없는듯
					if (isStageLocateUsage
							&& trCmd.getSourceLoc() != null
							&& reservedStageSourceLocMap.containsKey(trCmd.getSourceLoc())
							&& minCostVehicleIndex != -1) {
						for (int i = 0; i < candidateVehicleListForJobAllocation.size(); i++) {
							if (i == minCostVehicleIndex) {
								cost[i][j] = 5;
								// added by KYK
								Vehicle stageChangedVehicle = candidateVehicleListForJobAllocation.get(i);
								stageChangedVehicleList.add(stageChangedVehicle);
							} else {
								cost[i][j] = MAXCOST_TIMEBASE;
							}
						}
					}
				}
			}
		}
		
		if (isPriorJobDispatchingUsed) {
			// 2015.10.01 by KYK : priorJob initiated , nearest Vehicle dispatching (cost-matrix value setting) TODO
			if (priorJobList.size() > 0) {
				Collections.sort(priorJobList, new PriorJobComparator<PriorJob>());
				
				int minCostIndex;
				int jobIndex;
				double minValue;			
				HashSet<Vehicle> priorJobAssignedVehicleSet = new HashSet<Vehicle>();
				for (PriorJob job: priorJobList) {
					minCostIndex = -1;
					jobIndex = job.getIndex();
					minValue = MAXCOST_TIMEBASE;
					for (int i = 0; i < candidateVehicleListForJobAllocation.size(); i++) {					
						vehicle = candidateVehicleListForJobAllocation.get(i);
						if (stageChangedVehicleList.contains(vehicle) == false && 
								priorJobAssignedVehicleSet.contains(vehicle) == false) {
							if (costBackup[i][jobIndex] < minValue) {
								minValue = costBackup[i][jobIndex];
								minCostIndex = i;
							}
						}
					}
					if (minCostIndex > -1) {
						Vehicle selectedVehicle = candidateVehicleListForJobAllocation.get(minCostIndex);
						priorJobAssignedVehicleSet.add(selectedVehicle);
						for (int i = 0; i < candidateVehicleListForJobAllocation.size(); i++) {
							if (i == minCostIndex) {
								cost[i][jobIndex] = 0;
							} else {
								cost[i][jobIndex] = MAXCOST_TIMEBASE;
							}
						}						
						StringBuilder sb = new StringBuilder("[PriorJob selected] ");
						sb.append("PriorCost:").append(job.getCost()).append("/");
						sb.append("TrCmdID:").append(job.getTrCmd().getTrCmdId()).append("/");
						sb.append("Vehicle:").append(selectedVehicle.getVehicleId()).append("/");
						sb.append("Distance-basedCost:").append(String.format(FORMAT, costBackup[minCostIndex][jobIndex],2)).append("/");
						sb.append("SourceNode:").append(job.getTrCmd().getSourceNode()).append("/");
						sb.append("StopNode:").append(selectedVehicle.getStopNode());
						traceJobAssignMain(sb.toString());
					}
				}
			}
		}

		reservedStageSourceLocMap.clear();
		stageChangedVehicleList.clear();
	}
	
	public int getWaitingTime(TrCmd trCmd) {
		int waitingTime = 0;
		Long longValue = waitingTimeMap.get(trCmd);
		if (longValue != null) {
			waitingTime = longValue.intValue();
		}
		return waitingTime;
	}
	
	/**
	 * PRIORJOB DISPATCHINGRULE 占쏙옙 占쏙옙占쏙옙 PRIORJOB 占실븝옙
	 * return priorCost : if priorCost > 0, it is priorJob
	 */
	public double getPriorJobCost(TrCmd trCmd) {
		double priorCost = -1;
		int priority = trCmd.getPriority();
		int waitingTime = getWaitingTime(trCmd);
		if (trCmd != null) {
			switch (priorJobDispatchingRule) {
			case PRIORITY:
				if (priority >= priorJobPriorityThreshold) {
					priorCost = priority + (waitingTime/1000.0);
				}
				break;
				
			case EQPRIORITY:
				if (priority >= priorJobPriorityThreshold) {
					CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(trCmd.getDestLoc());
					if (carrierLoc == null) {
						StringBuilder sb = new StringBuilder("[Unknown CarrierLoc] ");
						sb.append("Unknown DestLoc Job is Registered. ");
						sb.append("TrCmdID:").append(trCmd.getTrCmdId()).append("/");
						sb.append("Unknown DestLoc:").append(trCmd.getDestLoc());
						registerAlarmWithLevel("JobAssign", sb.toString(), ALARMLEVEL.ERROR);
						traceJobAssignMain(sb.toString());
					} else if (CARRIERLOC_TYPE.EQPORT == carrierLoc.getType()) {
						priorCost = priority + (waitingTime/1000.0);
					}
				}
				break;
				
			case WAITINGTIME:
				if (waitingTime >= priorJobWaitingTimeThreshold) {
					priorCost = waitingTime + (priority/100.0);
				}
				break;
				
			case HYBRID:
				if (priority >= priorJobPriorityThreshold || 
						waitingTime >= priorJobWaitingTimeThreshold) {
					priorCost = priority + (waitingTime/1000.0);
				}
				break;
				
			default:
			}
		}
		return priorCost;
	}

	/**
	 * Step 5-2 Print Job Pattern and Cost 
	 * 
	 */
	private void printPatternAndCostForJob() {
		Vehicle vehicle = null;
		StringBuilder vehiclePattern = new StringBuilder();
		vehiclePattern.append("     [Vehicle Pattern] <").append(makeCountString(candidateVehicleListForJobAllocation.size())).append(" Vehicle>\t");
		Iterator<Vehicle> itVehiclePattern = candidateVehicleListForJobAllocation.iterator();
		while (itVehiclePattern.hasNext()) {
			vehicle = itVehiclePattern.next();
			if (vehicle != null) {
				vehiclePattern.append(vehicle.getVehicleId());
			}
			if (itVehiclePattern.hasNext()) {
				vehiclePattern.append(" ");
			}
		}
		traceJobAssignMain(vehiclePattern.toString());
		
		TrCmd trCmd = null;
		long waitingTimeOfTrCmd = 0;
		StringBuilder jobTrCmdIdPattern = new StringBuilder();
		StringBuilder jobSourceNodePattern = new StringBuilder();
		StringBuilder jobPriorityPattern = new StringBuilder();
		StringBuilder jobWaitingTimePattern = new StringBuilder();
		StringBuilder jobLoadTimePattern = new StringBuilder();
		jobTrCmdIdPattern.append("     [Job Pattern]  <").append(makeCountString(targetTrCmdListForJobAllocation.size())).append(" Job>\t");
		jobSourceNodePattern.append("     [Job Pattern:SourceNode]\t");
		jobPriorityPattern.append("     [Job Pattern:  Priority]\t");
		jobWaitingTimePattern.append("     [Job Pattern:  WaitTime]\t");
		jobLoadTimePattern.append("     [Job Pattern:  LoadTime]\t");
		Iterator<TrCmd> itJobPattern = targetTrCmdListForJobAllocation.iterator();
		while (itJobPattern.hasNext()) {
			trCmd = itJobPattern.next();
			waitingTimeOfTrCmd = 0;
			if (trCmd != null) {
				jobTrCmdIdPattern.append(trCmd.getTrCmdId());
//					jobSourceNodePattern.append(makeStringWithBlanks(trCmd.getSourceNode(), 6, false));
				if (isStageLocateUsage && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
					Node targetNode = stageTargetNodeMap.get(trCmd);
					if (targetNode != null) {
						jobSourceNodePattern.append(makeStringWithBlanks(targetNode.getNodeId(), 6, false));
					} else {
						jobSourceNodePattern.append(makeStringWithBlanks(trCmd.getSourceNode(), 6, false));
					}
				} else {
					jobSourceNodePattern.append(makeStringWithBlanks(trCmd.getSourceNode(), 6, false));
				}
				jobPriorityPattern.append(makeStringWithBlanks(String.valueOf(trCmd.getPriority()), 6, false));
				if (waitingTimeMap.containsKey(trCmd)) {
					waitingTimeOfTrCmd = (waitingTimeMap.get(trCmd)).longValue();
				}
				jobWaitingTimePattern.append(makeStringWithBlanks(String.valueOf(waitingTimeOfTrCmd), 6, false));
				jobLoadTimePattern.append(makeStringWithBlanks(getCostInSixDigits(trCmd.getLoadTransferTime()), 6, false));
			}
			if (itJobPattern.hasNext()) {
				jobTrCmdIdPattern.append(" ");
				jobSourceNodePattern.append("  ");
				jobPriorityPattern.append("  ");
				jobWaitingTimePattern.append("  ");
				jobLoadTimePattern.append("  ");
			}
		}
		traceJobAssignMain(jobTrCmdIdPattern.toString());
		traceJobAssignMain(jobSourceNodePattern.toString());
		traceJobAssignMain(jobPriorityPattern.toString());
		traceJobAssignMain(jobWaitingTimePattern.toString());
		traceJobAssignMain(jobLoadTimePattern.toString());
		
		if (isJobAssignDetailResultUsed) {
			for (int i = 0; i < candidateVehicleListForJobAllocation.size(); i++) {
				vehicle = candidateVehicleListForJobAllocation.get(i);
				if (vehicle != null) {
					StringBuilder message = new StringBuilder();
					message.append(vehicle.getVehicleId());
					
					if (localVehicleList.contains(vehicle)) {
						message.append("(L)");
					} else if (locateVehicleList.contains(vehicle)) {
						message.append("(M)");
					} else if (idleVehicleList.contains(vehicle)) {
						message.append("(F)");
					} else if (loadVehicleList.contains(vehicle)) {
						message.append("(O)");
					} else {
						// 확인 용.
						message.append("(?)");
					}
					message.append("\t");
					
					for (int j = 0; j < targetTrCmdListForJobAllocation.size(); j++) {
						message.append(getCostInSixDigits(cost[i][j]));
						if (j < targetTrCmdListForJobAllocation.size() - 1) {
							message.append(", ");
						}
					}
					traceJobAssignMain("     [Cost for Job] " + message.toString());
				}
			}
		}
	}
	
	/**
	 * Step 5-3 Allocate Job to Candidate Vehicle 
	 * 
	 */
	private void allocateJob() {
		tempReservedVehicleList.clear();
		jobAssignResultSet.clear();
		
		reservedAsNextTrCmdSet.clear();
		
		allocateStartedTime = System.currentTimeMillis();
		try {
			if (solver.solveAssignment(size, cost, result)) {
				double modelingCost = 0;
				double distanceBasedCost = 0;
				Vehicle vehicle = null;
				TrCmd trCmd = null;
				JobAssignResult jobAssignResult = null;
				for (int i = 0; i < candidateVehicleListForJobAllocation.size(); i++) {
					if (result[i] < targetTrCmdListForJobAllocation.size()) {
						vehicle = candidateVehicleListForJobAllocation.get(i);
						trCmd = targetTrCmdListForJobAllocation.get(result[i]);
						if (vehicle == null || trCmd == null) {
							continue;
						}
						modelingCost = cost[i][result[i]];
						distanceBasedCost = costBackup[i][result[i]];
						if (distanceBasedCost >= OcsConstant.MAXCOST_TIMEBASE) {
							continue;
						}
						jobAssignResult = new JobAssignResult(vehicle, trCmd, modelingCost, distanceBasedCost);
						if (jobAssignResultSet.contains(jobAssignResult) == false) {
							jobAssignResultSet.add(jobAssignResult);
						}
					}
				}
				
				Iterator<JobAssignResult> itResult = jobAssignResultSet.iterator();
				while (itResult.hasNext()) {
					jobAssignResult = itResult.next();
					if (jobAssignResult != null) {
						vehicle = jobAssignResult.getVehicle();
						trCmd = (TrCmd)jobAssignResult.getTarget();
						modelingCost = jobAssignResult.getModelingCost();
						distanceBasedCost = jobAssignResult.getDistanceBasedCost();
						if (resultVehicleList.contains(vehicle) == false &&
								resultTargetList.contains(trCmd) == false) {
							if (idleVehicleList.contains(vehicle)) {
								if (isAreaBalancingUsed == false || checkAreaBalancing(vehicle, trCmd)) {
									if (isStageLocateUsage && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
										// 2014.02.18 by MYM : [Stage Locate 기능]
										reserveVehicleToStage(vehicle, trCmd, modelingCost, distanceBasedCost);
									} else if (locateVehicleList.contains(vehicle)) {
										// 2015.02.16 by MYM : Locate 작업할당 Threshold 파라미터화
//										if (distanceBasedCost <= 10) {
										if (distanceBasedCost <= jobAssignLocateThreshold) {
											allocateVehicleToJob(vehicle, trCmd, modelingCost, distanceBasedCost);
										} else {
											reserveVehicleToJob(vehicle, trCmd, modelingCost, distanceBasedCost);
										}
									} else {
										if (distanceBasedCost <= jobAssignThreshold) {
											allocateVehicleToJob(vehicle, trCmd, modelingCost, distanceBasedCost);
										} else {
											reserveVehicleToJob(vehicle, trCmd, modelingCost, distanceBasedCost);
										}
									}
								} else {
									canNotAllocateVehicleToJobByAreaBalancing(vehicle, trCmd);
								}
							} else if (loadVehicleList.contains(vehicle)) {
								if (vehicle.getTargetNode().equals(trCmd.getSourceNode()) &&
										destLocOfUnloadedVehicleList.contains(trCmd.getSourceLoc())) {
									canNotAllocateVehicleToNextJob(vehicle, trCmd, modelingCost);
									continue;
								} else {
									reserveVehicleToNextJob(vehicle, trCmd, modelingCost, distanceBasedCost);
								}
							} else {
								StringBuilder message = new StringBuilder();
								message.append("[Abnormal Case1] Vehicle:").append(vehicle.getVehicleId());
								message.append(", TrCmd:").append(trCmd.getTrCmdId());
								message.append(", ResultVehicleList:").append(resultVehicleList.toString());
								message.append(", ResultTargetList:").append(resultTargetList.toString());
								traceJobAssignException(message.toString());
							}
						} else {
							StringBuilder message = new StringBuilder();
							message.append("[Abnormal Case2] Vehicle:").append(vehicle.getVehicleId());
							message.append(", TrCmd:").append(trCmd.getTrCmdId());
							message.append(", ResultVehicleList:").append(resultVehicleList.toString());
							message.append(", ResultTargetList:").append(resultTargetList.toString());
							traceJobAssignException(message.toString());
						}
					}
				}
//				manageJobReserved();
			} else {
				traceJobAssignException("     [AP_Failed] AP_Infinite_Loop");
			}
		} catch (Exception e) {
			traceJobAssignException("     [AP_Exception] " + e.getStackTrace());
		}
		
		allocateCompletedTime = System.currentTimeMillis();
		printAllocationCompleted(allocateCompletedTime - allocateStartedTime);
	}
	
	/**
	 *  Step 6-1 Get Cost for Park Allocation
	 * 
	 */
	private void getCostForPark() {
		assert isParkNodeUsed;
		assert targetParkList.size() > 0;
		
		candidateVehicleListForParkAllocation.clear();
		targetParkListForParkAllocation.clear();
		
		Vehicle vehicle = null;
		Park park = null;
		
		int candidateCount = idleVehicleList.size();
		int targetCount = targetParkList.size();
		int idleCount = idleLocalCandidateVehicleCount + idleGeneralCandidateVehicleCount;
		int targetTrCmdCount = targetTrCmdList.size();
		boolean isTargetUpdated = false;
		
		if (candidateCount > targetCount) {
			size = candidateCount;
		} else {
			size = targetCount;
		}
		cost = new double[size][size];
		costBackup = new double[size][size];
		result = new int[size];
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				cost[i][j] = MAXCOST_TIMEBASE;
				costBackup[i][j] = MAXCOST_TIMEBASE;
			}
			result[i] = size;
		}
		
		candidateCount = 0;
		for (int i = 0; i < idleCount; i++) {
			vehicle = candidateVehicleList.get(i);
			if (vehicle != null) {
				if (idleVehicleList.contains(vehicle) &&
						resultVehicleList.contains(vehicle) == false) {
					if (candidateVehicleListForParkAllocation.contains(vehicle) == false) {
						candidateVehicleListForParkAllocation.add(vehicle);
					}
					targetCount = 0;
					// 2013.06.05 by KYK
					for (int j = targetTrCmdCount; j < targetList.size(); j++) {
						park = targetParkList.get(j - targetTrCmdCount);
						if (park != null) {
							if (isTargetUpdated == false &&
									targetParkListForParkAllocation.contains(park) == false) {
								targetParkListForParkAllocation.add(park);
							}
							cost[candidateCount][targetCount] = costNodes[i][j];
							if (cost[candidateCount][targetCount] > parkSearchLimit) {
								cost[candidateCount][targetCount] = MAXCOST_TIMEBASE;
							}
							costBackup[candidateCount][targetCount] = cost[candidateCount][targetCount];
							targetCount++;
						}
					}
					candidateCount++;
					isTargetUpdated = true;
				}
			}
		}
		
		int rank = 9;
		for (int j= 0; j < targetParkListForParkAllocation.size(); j++) {
			park = targetParkListForParkAllocation.get(j);
			if (park != null) {
				rank = park.getRank();
				for (int i = 0; i < candidateVehicleListForParkAllocation.size(); i++) {
					if (cost[i][j] < MAXCOST_TIMEBASE) {
						cost[i][j] = cost[i][j] + 500 * rank;
					}
					
					if (cost[i][j] > MAXCOST_TIMEBASE) {
						cost[i][j] = MAXCOST_TIMEBASE;
					}
				}
			}
		}
	}
	
	/**
	 * Step 6-2 Print Park Pattern and Cost 
	 * 
	 */
	private void printPatternAndCostForPark() {
		assert isParkNodeUsed;
		
		Vehicle vehicle = null;
		StringBuilder vehiclePattern = new StringBuilder();
		vehiclePattern.append("     [Vehicle Pattern] <").append(makeCountString(candidateVehicleListForParkAllocation.size())).append(" Vehicle>\t");
		Iterator<Vehicle> itVehiclePattern = candidateVehicleListForParkAllocation.iterator();
		while (itVehiclePattern.hasNext()) {
			vehicle = itVehiclePattern.next();
			if (vehicle != null) {
				vehiclePattern.append(vehicle.getVehicleId());
			}
			if (itVehiclePattern.hasNext()) {
				vehiclePattern.append(" ");
			}
		}
		traceJobAssignMain(vehiclePattern.toString());
		
		Park park = null;
		int maxLength = maxLocalGroupIdLength > 0 ? maxLocalGroupIdLength + 2 : 3;
		StringBuilder parkNamePattern = new StringBuilder();
		StringBuilder parkTypePattern = new StringBuilder();
		String parkCount = makeCountString(targetParkListForParkAllocation.size());
		parkNamePattern.append("     [Park Pattern] <").append(parkCount).append(" Pa").append(makeStringWithBlanks("rkNode>", maxLength, true)).append("\t");
		parkTypePattern.append("     [Park Pattern:      Pa").append(makeStringWithBlanks("rkType]", maxLength, true)).append("\t");
		Iterator<Park> itParkPattern = targetParkListForParkAllocation.iterator();
		while (itParkPattern.hasNext()) {
			park = itParkPattern.next();
			if (park != null) {
				parkNamePattern.append(makeNodeString(park.getName()));
				parkTypePattern.append(makeStringWithBlanks(park.getType(), 6, false));
			}
			if (itParkPattern.hasNext()) {
				parkNamePattern.append("  ");
				parkTypePattern.append("  ");
			}
		}
		traceJobAssignMain(parkNamePattern.toString());
		traceJobAssignMain(parkTypePattern.toString());
		
		if (isJobAssignDetailResultUsed) {
			for (int i = 0; i < candidateVehicleListForParkAllocation.size(); i++) {
				vehicle = candidateVehicleListForParkAllocation.get(i);
				if (vehicle != null) {
					StringBuilder message = new StringBuilder();
					message.append(vehicle.getVehicleId());
					
					if (localVehicleList.contains(vehicle)) {
						message.append(makeStringWithBlanks("(" + vehicle.getLocalGroupId() + ")", maxLength, true));
					} else if (locateVehicleList.contains(vehicle)) {
						message.append(makeStringWithBlanks("(M)", maxLength, true));
					} else {
						message.append(makeStringWithBlanks("(F)", maxLength, true));
					}
					message.append("\t");
					
					for (int j = 0; j < targetParkListForParkAllocation.size(); j++) {
						message.append(getCostInSixDigits(cost[i][j]));
						if (j < targetParkListForParkAllocation.size() - 1) {
							message.append(", ");
						}
					}
					traceJobAssignMain("     [Cost for Park] " + message.toString());
				}
			}
		}
	}
	
	/**
	 * Step 6-3 Allocate Park to Idle Vehicle 
	 * 
	 */
	private void allocatePark() {
		assert isParkNodeUsed;
		
		tempRequestedParkMap.clear();
		parkResultSet.clear();
		
		allocateStartedTime = System.currentTimeMillis();
		try {
			if (solver.solveAssignment(size, cost, result)) {
				double modelingCost;
				double distanceBasedCost;
				Vehicle vehicle = null;
				Park park = null;
				
				JobAssignResult parkResult = null;
				for (int i = 0; i < candidateVehicleListForParkAllocation.size(); i++) {
					if (result[i] < targetParkListForParkAllocation.size()) {
						vehicle = candidateVehicleListForParkAllocation.get(i);
						park = targetParkListForParkAllocation.get(result[i]);
						if (vehicle == null || park == null) {
							continue;
						}
						modelingCost = cost[i][result[i]];
						distanceBasedCost = costBackup[i][result[i]];
						if (modelingCost >= OcsConstant.MAXCOST_TIMEBASE) {
							continue;
						}
						
						parkResult = new JobAssignResult(vehicle, park, modelingCost, distanceBasedCost);
						if (parkResultSet.contains(parkResult) == false) {
							parkResultSet.add(parkResult);
						}
					}
				}
				
				Iterator<JobAssignResult> itResult = parkResultSet.iterator();
				while (itResult.hasNext()) {
					parkResult = itResult.next();
					if (parkResult != null) {
						vehicle = parkResult.getVehicle();
						park = (Park)parkResult.getTarget();
						modelingCost = parkResult.getModelingCost();
						distanceBasedCost = parkResult.getDistanceBasedCost();
						
						if (resultVehicleList.contains(vehicle) == false &&
								resultTargetList.contains(park) == false) {
							if (idleVehicleList.contains(vehicle)) {
								if (isAreaBalancingUsed == false || checkAreaBalancing(vehicle, park)) {
									allocateVehicleToPark(vehicle, park, modelingCost, distanceBasedCost);
								} else {
									StringBuilder message = new StringBuilder();
									message.append("     [Can Not Park] ").append(vehicle.getVehicleId());
									message.append(" X/ParkNode=").append(park.getName());
									message.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
									message.append("/Distance-based Cost=").append(String.format(FORMAT, distanceBasedCost));
									message.append(" [Area Balancing]");
									traceJobAssignMain(message.toString());
								}
							}
						} else {
							StringBuilder message = new StringBuilder();
							message.append("[Abnormal Case] Vehicle:").append(vehicle.getVehicleId());
							message.append(", Park:").append(park.getName());
							message.append(", ResultVehicleList:").append(resultVehicleList.toString());
							message.append(", ResultTargetList:").append(resultTargetList.toString());
							traceJobAssignException(message.toString());
						}
					}
				}
				
				manageRequestedPark();
			} else {
				traceJobAssignMain("     [AP_Failed] AP_Infinite_Loop");
			}
		} catch (Exception e) {
			traceJobAssignException("     [AP_Exception] ", e);
		}
		
		allocateCompletedTime = System.currentTimeMillis();
		printAllocationCompleted(allocateCompletedTime - allocateStartedTime);
	}
	
	/**
	 * Step 1-4-1 Get VehicleList [enabledVehicleList, vehicleList]
	 *			enabledVehicleList: All Enabled Vehicles
	 *			vehicleList: Normal Auto Enabled Vehicles
	 * 
	 */
	private void getVehicleList() {
		vehicleList.clear();
		enabledVehicleList.clear();
		
		nodeManager.resetAbnormalNodeIdList();
		
		ConcurrentHashMap<String, Object> tempVehicleList = vehicleManager.getData(); 
		Set<String> searchKeys = new HashSet<String>(tempVehicleList.keySet());
		Vehicle vehicle = null;
		int alarmCode = 0;
		for (String searchKey : searchKeys) {
			vehicle = (Vehicle)tempVehicleList.get(searchKey);
			if (vehicle != null) {
				if (vehicle.isEnabled()) {
					
					// All Enabled Vehicles
					enabledVehicleList.put(searchKey, vehicle);
					
					if (vehicle.getVehicleMode() == 'A') {
						if (vehicle.getState() != 'E' && vehicle.getErrorCode() != OcsConstant.COMMUNICATION_FAIL) {
							if (alarmManager.isAlarmRegistered(vehicle.getVehicleId())) {
								alarmCode = alarmManager.getRegisteredAlarmCode(vehicle.getVehicleId());
								if (alarmCode > 5000) {
									addAbnormalNode(vehicle);
									continue;
								}
							}
							if (vehicle.isActionHold() == false && vehicle.isAssignHold() == false) {
								if ((vehicle.getTargetNode() != null && vehicle.getTargetNode().length() == 0) ||
										(vehicle.getStopNode() != null && vehicle.getStopNode().length() == 0) || 
										(vehicle.getCurrNode() != null && vehicle.getCurrNode().length() == 0)) {
									continue;
								}
								
								// Normal Auto Enabled Vehicles
								vehicleList.put(searchKey, vehicle);
							}
						} else {
							addAbnormalNode(vehicle);
						}
					} else {
						// Manual Vehicles
						addAbnormalNode(vehicle);
					}
				}
			}
		}
		costSearch.updateCurrNodeVehicleMap();
	}
	
	private void addAbnormalNode(Vehicle vehicle) {
		StringBuilder message = new StringBuilder();
		message.append("addAbnormalNode: [");
		message.append(vehicle.getVehicleId());
		message.append("] ");
		
		if (vehicle.getCurrNode().equals(vehicle.getStopNode())) {
			addAbnormalNode(vehicle.getCurrNode());
		} else {
			LinkedList<String> driveNodeList = costSearch.searchVehicleDrive(vehicle);
			if (driveNodeList != null && driveNodeList.size() > 0) {
				String drivedNodeId = null;
				for (int i = 0; i < driveNodeList.size(); i++) {
					drivedNodeId = driveNodeList.get(i);
					if (drivedNodeId != null && drivedNodeId.length() > 0) {
						addAbnormalNode(drivedNodeId);
						message.append(drivedNodeId).append(" ");
					}
				}
			} else {
				addAbnormalNode(vehicle.getCurrNode());
				addAbnormalNode(vehicle.getStopNode());
			}
		}
		traceJobAssignMain(message.toString());
	}
	
	private void addAbnormalNode(String nodeId) {
		try {
			nodeManager.addAbnormalNodeToList(nodeId);
			if (isNearByDrive == false) {
				Node node = nodeManager.getNode(nodeId);
				ListIterator<Collision> iterator = node.getCollisions().listIterator();
				Collision collision;
				while (iterator.hasNext()) {
					collision = (Collision)iterator.next();
					if (collision != null) {
						nodeManager.addAbnormalNodeToList(collision.getCollisionNodeId(nodeId));
					}
				}
			}
		} catch (Exception e) {}
	}
	
	/**
	 * Step 1-4-2 Get TrCmd List [assignedTrCmdList, assignedVehicleIdList, reservedVehicleIdList]
	 * 			assignedTrCmdList: TrCmds whose AssignedVehicle is not null in TRCMD
	 * 			assignedVehicleIdList: VehicleIds in TRCMD's Vehicle  
	 * 			reservedVehicleIdList: VehicleIds in TRCMD's AssignedVehicle and TRCMD's Vehicle is null 
	 * 
	 */
	private void getTrCmdList() {
		assignedTrCmdList.clear();
		assignedVehicleIdList.clear();
		assignRequestedVehicleIdList.clear();
		
		trCmdList = trCmdManager.getData();

		Set<String> searchKeys = new HashSet<String>(trCmdList.keySet());
		TrCmd trCmd = null;
		String assignedVehicleId = null;
		String vehicleId = null;
		
		for (String searchKey : searchKeys) {
			trCmd = (TrCmd)trCmdList.get(searchKey);
			if (trCmd != null) {
				assignedVehicleId = trCmd.getAssignedVehicleId();
				vehicleId = trCmd.getVehicle();
				if ((assignedVehicleId != null && assignedVehicleId.length() > 0)) {
					if (assignedTrCmdList.contains(trCmd) == false) {
						assignedTrCmdList.add(trCmd);
					}
					
					if ((vehicleId != null && vehicleId.length() > 0)) {
						if (assignedVehicleIdList.contains(vehicleId) == false) {
							assignedVehicleIdList.add(vehicleId);
						}
					} else {
						if (assignRequestedVehicleIdList.contains(assignedVehicleId) == false) {
							assignRequestedVehicleIdList.add(assignedVehicleId);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Step 1-4-3 Classify Vehicles [IdleVehicleList, LocalVehicleList, IdleLocalVehicleList, LocalGroupIdList]
	 * 
	 */
	private void classifyVehicles() {
		idleVehicleList.clear();
		localVehicleList.clear();
		localGroupIdList.clear();
		
		Vehicle vehicle = null;
		String localGroupId = "";
		
		Set<String> searchKeys = new HashSet<String>(vehicleList.keySet());
		for (String searchKey : searchKeys) {
			vehicle = (Vehicle)vehicleList.get(searchKey);
			if (vehicle != null) {
				if (assignedVehicleIdList.contains(vehicle.getVehicleId()) == false) {
					// Idle Vehicle.
					if (idleVehicleList.contains(vehicle) == false) {
						idleVehicleList.add(vehicle);
					}
				}
				
				localGroupId = vehicle.getLocalGroupId();
				if ((localGroupId != null && localGroupId.length() > 0)) {
					if (localVehicleList.contains(vehicle) == false) {
						// 가변형 Enabled LocalOHT
						localVehicleList.add(vehicle);
					}
					
					if (localGroupIdList.contains(localGroupId) == false) {
						// LocalGroupIds
						localGroupIdList.add(localGroupId);
					}
				}
			}
		}
	}
	
	/**
	 * Step 1-4-4 Classify TrCmds and LocatingVehicles [unassignedTrCmdList, idleVehicleList, loadVehicleList, destLocOfUnloadedVehicleList, locateVehicleList, idleLocalVehicleList]
	 * 
	 */
	private void classifyTrCmdsAndVehicles() {
		unassignedTrCmdList.clear();
		loadVehicleList.clear();
		locateVehicleList.clear();
		idleLocalVehicleList.clear();
		idleVehicleLocalGroupIdList.clear();
		
		// 2014.02.27 by MYM : [Stage Locate 기능]
		unassignedStageList.clear();
		stageLocateVehicleList.clear();
		Iterator<String> iter = sortedStageAtSamePortMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			ArrayList<TrCmd> list = sortedStageAtSamePortMap.get(key);
			if (list != null) {
				list.clear();
			}
			iter.remove();
		}
		
		TrCmd trCmd = null;
		Vehicle assignedVehicle = null;
		String assignedVehicleId = "";
		Set<String> searchKeys = new HashSet<String>(trCmdList.keySet());
		for (String searchKey : searchKeys) {
			trCmd = (TrCmd)trCmdList.get(searchKey);
			if (trCmd != null) {
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
					// STAGE인 경우, DestLoc과 DestNode가 없음.
					if (trCmd.getDestLoc() != null && trCmd.getDestLoc().length() < 2) {
						trCmd.setDestLoc(trCmd.getSourceLoc());
					}
					if (trCmd.getDestNode() != null && trCmd.getDestNode().length() < 2) {
						trCmd.setDestNode(trCmd.getSourceNode());
					}
				}
				assignedVehicleId = trCmd.getAssignedVehicleId();
				
				if (assignedVehicleId != null && assignedVehicleId.length() == 0) {
					if (trCmd.getVehicle() != null && trCmd.getVehicle().length() > 0) {
						// Unknown TrCmd 등록 시, AssignedVehicle이 빠질 수 있음.
						StringBuilder message = new StringBuilder();
						message.append("Abnormal Case01! trCmdId:").append(trCmd.getTrCmdId());
						message.append(", Vehicle:").append(trCmd.getVehicle());
						message.append(", AssignedVehicle:").append(assignedVehicleId);
						message.append(", State:").append(trCmd.getState().toConstString());
						message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
						traceJobAssignException(message.toString());
						
						continue;
					}
					// Vehicle이 Assign 되지 않은 TrCmd.
					if (trCmd.isPause() == false) {
						if (isHidControlUsed == false || checkHidStatusOfWorkNodes(trCmd)) {
							// 2018.08.31 by LSH : Runtime Update 중 JobAssign 누락 시, 알 수 없는 Port 반송으로 인지되어 Cost 계산 중 Null Point Exception 발생 가능.
							// Tr의 CarrierLoc이 메모리상 정상 등록된 Port 인지 확인하여 Cost 계산 대상 작업으로 등록
							if (nodeManager.isValidNode(trCmd.getSourceNode()) && nodeManager.isValidNode(trCmd.getDestNode())
									&& carrierLocManager.isValidCarrierLoc(trCmd.getSourceLoc()) && carrierLocManager.isValidCarrierLoc(trCmd.getDestLoc())) {
								if (assignedTrCmdList.contains(trCmd) == false) {
									// Not Assigned TrCmd.
									
									if (trCmd.getDetailState() == TRCMD_DETAILSTATE.NOT_ASSIGNED) {
										if (unassignedTrCmdList.contains(trCmd) == false) {
											unassignedTrCmdList.add(trCmd);
										}
										// 2014.02.27 by MYM : [Stage Locate 기능] : 
										// Port별로 TrQueuedTime으로 정렬하여 Map에 저장하고 TargetNode를 찾을때(searchStageTargetNode), 
										// StageLocate Vehicle 수 제한시(refinePortDuplicateTrCmds) 활용
										if (isStageLocateUsage
												&& trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE
												&& unassignedStageList.contains(trCmd) == false) {
											unassignedStageList.add(trCmd);
											ArrayList<TrCmd> stageList = sortedStageAtSamePortMap.get(trCmd.getSourceNode());
											if (stageList != null) {
												trCmd.setRemainingDuration(getRemainingDuration(trCmd));
												int i = 0;
												long trQueuedTime = getTimeFromString(trCmd.getTrQueuedTime());
												for (; i < stageList.size(); i++) {
													TrCmd stageTrCmd = stageList.get(i);
													long tmpTrQueuedTime = getTimeFromString(stageTrCmd.getTrQueuedTime());
													if (trQueuedTime < tmpTrQueuedTime) {
														break;
													}
												}
												stageList.add(i, trCmd);
											} else {
												stageList = new ArrayList<TrCmd>();
												stageList.add(trCmd);
											}
											sortedStageAtSamePortMap.put(trCmd.getSourceNode(), stageList);
										}
									} else {
										StringBuilder message = new StringBuilder();
										message.append("Abnormal Case02! trCmdId:").append(trCmd.getTrCmdId());
										message.append(", Vehicle:").append(trCmd.getVehicle());
										message.append(", AssignedVehicle:").append(assignedVehicleId);
										message.append(", State:").append(trCmd.getState().toConstString());
										message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
										traceJobAssignException(message.toString());
										if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
											trCmd.setState(TRCMD_STATE.CMD_QUEUED);
											trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
											trCmdManager.updateTrCmdStateToDB(trCmd);
										}
										StringBuilder message2 = new StringBuilder();
										message2.append("Recovered! trCmdId:").append(trCmd.getTrCmdId());
										message2.append(", Vehicle:").append(trCmd.getVehicle());
										message2.append(", AssignedVehicle:").append(assignedVehicleId);
										message2.append(", State:").append(trCmd.getState().toConstString());
										message2.append(", DetailState:").append(trCmd.getDetailState().toConstString());
										traceJobAssignException(message2.toString());
									}
								}
							} else {
								StringBuilder message = new StringBuilder();
								message.append("Work Node or CarrierLoc is not valid. TrCmdId:").append(trCmd.getTrCmdId());
								message.append(", SourceNode:").append(trCmd.getSourceNode());
								message.append(", DestNode:").append(trCmd.getDestNode());
								message.append(", SourceCarrierLoc:").append(trCmd.getSourceLoc());
								message.append(", DestCarrierLoc:").append(trCmd.getDestLoc());
								traceJobAssignMain(message.toString());
							}
						} else {
							// HID Down.
							;/*NULL*/
						}
					} else {
						// TrCmd is Paused.
						;/*NULL*/
					}
				} else {
				// Vehicle이 이미 Assign된 TrCmd.
					assignedVehicle = (Vehicle)vehicleList.get(assignedVehicleId);
					if (assignedVehicle != null) {
						idleVehicleList.remove(assignedVehicle);
						
						if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT) {
							// ABORT 인 경우에는 LoadVehicleList에서 제외.
						} else {
							if (trCmd.isPause()) {
								// PAUSED 인 경우에는 LoadVehicleList에서 제외.
							} else {
								switch (trCmd.getDetailState()) {
									case LOADED:
									case LOADING:
									{
										if (assignRequestedVehicleIdList.contains(assignedVehicleId) == false) {
											// 아직  Next Job이 할당되지 않은 경우.
											// 작업 할당 시 IdleVehicle과 동일하게 취급.
											idleVehicleList.add(assignedVehicle);
										} else {
											// 이미 Next Job이 할당되어 있는 경우.
											;/*NULL*/
										}
										break;
									}
									// 2012.04.20 by PMM
//									case UNLOADED:
									case LOAD_ACCEPTED: 
									case LOAD_SENT:
									case LOAD_ASSIGNED:
									{
										if ((assignRequestedVehicleIdList.contains(assignedVehicleId) == false) &&
												(loadVehicleList.contains(assignedVehicle) == false)) {
											loadVehicleList.add(assignedVehicle);
										}
										if (destLocOfUnloadedVehicleList.contains(trCmd.getDestLoc()) == false) {
											destLocOfUnloadedVehicleList.add(trCmd.getDestLoc());
										}
										break;
									}
									default:
										break;
								}
							}
						}
					}
				}
			}
		}
		
		String localGroupId = "";
		maxLocalGroupIdLength = 3;
		for (Vehicle idleVehicle : idleVehicleList) {
			if (idleVehicle.getRequestedType() == REQUESTEDTYPE.LOCATE) {
				if (locateVehicleList.contains(idleVehicle) == false) {
					locateVehicleList.add(idleVehicle);
				}
			} else if (idleVehicle.getRequestedType() == REQUESTEDTYPE.STAGE
					|| idleVehicle.getRequestedType() == REQUESTEDTYPE.STAGEWAIT
					|| idleVehicle.getRequestedType() == REQUESTEDTYPE.STAGENOBLOCK) {
				// 2014.03.05 by MYM : [Stage Locate 기능]
				if (stageLocateVehicleList.contains(idleVehicle) == false) {
					stageLocateVehicleList.add(idleVehicle);
				}
			} else {
				// 2015.04.06 by KYK TODO loading 상태에서 idleVHL 로 간주됨, 이때 nextJob 에 대한 우선권 사라짐 (유지하는 옵션 추가)
				if (jobReservationOption == JOB_RESERVATION_OPTION.JR2) {
					TrCmd tempTrCmd = reservedVehicleMap.get(idleVehicle);
					if (tempTrCmd != null) {
						long waitingTime = getWaitingTime(tempTrCmd.getTrQueuedTime());
						if (waitingTime < jobReservationLimitTime) {
							reservedVehicleMap.remove(idleVehicle);
						} else {
							StringBuilder message = new StringBuilder();
							message.append("[Option:JR2] Limit:").append(jobReservationLimitTime);
							message.append(", WT:").append(waitingTime).append(" - ");
							message.append(idleVehicle.getVehicleId()).append(",").append(tempTrCmd.getTrCmdId());					
							message.append(" VHL state's changed LOAD to IDLE, but reservation advantage still valid.");
							traceJobAssignMain(message.toString());
						}
					}
				} else {
					if (reservedVehicleMap.containsKey(idleVehicle)) {
						reservedVehicleMap.remove(idleVehicle);
					}
				}
				
				// 2014.03.05 by MYM : [Stage Locate 기능]
				if (reservedStageVehicleMap.containsKey(idleVehicle)) {
					reservedStageVehicleMap.remove(idleVehicle);
				}
			}
			if (localVehicleList.contains(idleVehicle)) {
				// Idle인 LocalVehicle.
				idleLocalVehicleList.add(idleVehicle);
				
				localGroupId = idleVehicle.getLocalGroupId();
				if (localGroupId != null && localGroupId.length() > 0) {
					if (idleVehicleLocalGroupIdList.contains(localGroupId) == false) {
						idleVehicleLocalGroupIdList.add(localGroupId);
						if (localGroupId.length() > maxLocalGroupIdLength) {
							maxLocalGroupIdLength = localGroupId.length();
						}
					}
				}
			}
		}
	}
	
	private String getAssignableVehicleZone(TrCmd trCmd) {
		Vehicle vehicle = null;
		Set<String> searchKeys = new HashSet<String>(vehicleList.keySet());
		for (String searchKey : searchKeys) {
			vehicle = (Vehicle)vehicleList.get(searchKey);
			if (vehicle != null) {
				if (isJobAssignable(vehicle, trCmd)) {
					return vehicle.getZone();
				}
			}
		}
		return "";
	}
	
	/**
	 * Step 2-2-1 Release LocalOHT(s) in the LocalGroup
	 * 
	 * @param localGroupId
	 */
	private void releaseLocalOHTInLocalGroup(String localGroupId) {
		assert localGroupId != null;
		
		try {
			if (localGroupId != null && localGroupId.length() > 0) {
				int releaseCount = 0;
				LocalGroupInfo localGroupInfo = (LocalGroupInfo)localGroupInfoList.get(localGroupId);
				if (localGroupInfo != null) {
					releaseCount = localGroupCurVHL.get(localGroupId) - localGroupInfo.getSetVHL();
				} else {
					releaseCount = localGroupCurVHL.get(localGroupId);
				}
				ArrayList<Vehicle> releasedLocalVehicleList = new ArrayList<Vehicle>();
				if (releaseCount > 0) {
					int releasedCount = 0;
					for (Vehicle localVehicle : localVehicleList) {
						if (localVehicle != null) {
							if (localGroupId.equals(localVehicle.getLocalGroupId())) {
								
								// 해당 LocalGroup에 속하는 LocalVehicle에 대해 releaseCount만큼 release.
								vehicleManager.addVehicleToReleaseLocalGroupList(localVehicle.getVehicleId());
								traceJobAssignMain("     [Released] " + localVehicle.getVehicleId() + " /GroupId:" + localGroupId);
								releasedLocalVehicleList.add(localVehicle);
								releasedCount++;
							}
						}
						if (releasedCount >= releaseCount) {
							break;
						}
					}
					
					for (Vehicle releasedLocalVehicle : releasedLocalVehicleList) {
						// 해제한 LocalVehicle에 대해 LocalGroupId, localVehicleList, idleLocalVehicleList 정리.
						if (releasedLocalVehicle != null) {
							releasedLocalVehicle.setLocalGroupId("");
							localVehicleList.remove(releasedLocalVehicle);
							if (idleLocalVehicleList.contains(releasedLocalVehicle)) {
								idleLocalVehicleList.remove(releasedLocalVehicle);
							}
						}
					}
				}			
			}
		} catch (Exception e) {
			traceJobAssignException("", e);
		}
	}
	

	
	/**
	 * Step 2-3-2 LocalOHT 선정을 위한 후보 Vehicle, 대상 Bay 선택
	 * 
	 */
	private void selectVehiclesAndLocalGroups() {
		tempCandidateVehicleList.clear();
		candidateVehicleList.clear();
		targetLocalGroupList.clear();
		
		Vehicle vehicle;
		Iterator<Vehicle> itIdle = idleVehicleList.iterator();
		while (itIdle.hasNext()) {
			vehicle = itIdle.next();
			if (vehicle != null) {
				if (idleLocalVehicleList.contains(vehicle) == false &&
						tempCandidateVehicleList.contains(vehicle) == false) {
					tempCandidateVehicleList.add(vehicle);
				}
			}
		}
		
		Iterator<Vehicle> itLoad = loadVehicleList.iterator();
		while (itLoad.hasNext()) {
			vehicle = itLoad.next();
			if (vehicle != null) {
				if (localVehicleList.contains(vehicle) == false &&
						tempCandidateVehicleList.contains(vehicle) == false) {
					tempCandidateVehicleList.add(vehicle);
				}
			}
		}
		Iterator<Vehicle> itCandidate = tempCandidateVehicleList.iterator();
		while (itCandidate.hasNext()) {
			vehicle = itCandidate.next();
			if (vehicle != null) {
				if (candidateVehicleList.contains(vehicle) == false) {
					candidateVehicleList.add(vehicle);
				}
			}
		}
		
		LocalGroupInfo localGroupInfo = null;
		ListIterator<LocalGroupInfo> it = setLocalGroupInfoList.listIterator();
		while (it.hasNext()) {
			localGroupInfo = it.next();
			if (localGroupInfo != null) {
				if ((localGroupInfo.getBay() != null && localGroupInfo.getBay().length() > 0) &&
						targetLocalGroupList.contains(localGroupInfo) == false) {
					targetLocalGroupList.add(localGroupInfo);
				}
			}
		}
	}
	
	/**
	 * Step 2-3-3 Print Pattern for LocalGroupAllocation 
	 * 
	 */
	private void printPatternForLocalGroupAllocation() {
		StringBuilder candidateVehiclePattern = new StringBuilder();
		candidateVehiclePattern.append("     [LocalGroup Search Pattern] <").append(makeCountString(candidateVehicleList.size())).append(" Free Vehicle>\t");
		Iterator<Vehicle> itCandidateVehiclePattern = candidateVehicleList.iterator();
		while (itCandidateVehiclePattern.hasNext()) {
			candidateVehiclePattern.append(" ").append(itCandidateVehiclePattern.next().getVehicleId());
		}
		traceJobAssignMain(candidateVehiclePattern.toString());
		
		StringBuilder localGroupPattern = new StringBuilder();
		localGroupPattern.append("     [LocalGroup Pattern] <").append(makeCountString(targetLocalGroupList.size())).append(" LocalGroup>\t");
		
		LocalGroupInfo localGroupInfo = null;
		Iterator<LocalGroupInfo> itLocalGroupPattern = targetLocalGroupList.iterator();
		while (itLocalGroupPattern.hasNext()) {
			localGroupInfo = (LocalGroupInfo)itLocalGroupPattern.next();
			if (localGroupInfo != null) {
				StringBuilder localGroupIdAndBay = new StringBuilder();
				localGroupIdAndBay.append(localGroupInfo.getLocalGroupId());
				localGroupIdAndBay.append("(");
				localGroupIdAndBay.append(localGroupInfo.getBay());
				localGroupIdAndBay.append(")");
				localGroupPattern.append("  ").append(localGroupIdAndBay.toString());
			}
		}
		traceJobAssignMain(localGroupPattern.toString());
	}

	/**
	 * Step 2-3-4 Initialize Cost Variables for LocalGroupAllocation 
	 * 
	 */
	private void initializeCostForLocalGroupAllocation() {
		if (candidateVehicleList.size() > targetLocalGroupList.size()) {
			size = candidateVehicleList.size();
		} else {
			size = targetLocalGroupList.size();
		}
		cost = new double[size][size];
		costBackup = new double[size][size];
		result = new int[size];
		
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				cost[i][j] = MAXCOST_TIMEBASE;
				costBackup[i][j] = MAXCOST_TIMEBASE;
			}
		}
	}
	
	/**
	 * Step 2-3-5 Calculate Cost for LocalGroupAllocation 
	 * 
	 */
	private void calculateCostForLocalGroupAllocation() {
		costSearchStartTime = System.currentTimeMillis();
		
		costSearch.initCostSearchInfo(SEARCH_TYPE.NODE);
		
		Vehicle vehicle = null;
		String stopNodeId = "";
		Node fromNode = null;
		String bay = "";
		LocalGroupInfo localGroupInfo = null;
		double searchLimit = 100;
		boolean isCostSearchFromStopNode;
		
		for (int i = 0; i < candidateVehicleList.size(); i++) {
			result[i] = size;
			vehicle = candidateVehicleList.get(i);
			if (vehicle != null) {
				stopNodeId = vehicle.getStopNode();
				fromNode = (Node)nodeManager.getNode(stopNodeId);
				if (fromNode != null) {
					costMap.clear();
					ListIterator<LocalGroupInfo> itLocalGroup = targetLocalGroupList.listIterator();
					while (itLocalGroup.hasNext()) {
						localGroupInfo = itLocalGroup.next();
						if (localGroupInfo != null) {
							if (isLocalOHTAssignable(vehicle, localGroupInfo)) {
								costMap.put(localGroupInfo.getBay(), MAXCOST_TIMEBASE);
							}
							if (searchLimit < localGroupInfo.getDistance()) {
								searchLimit = localGroupInfo.getDistance();
							}
						}
					}
					StringBuilder message = new StringBuilder();
					message.append(vehicle.getVehicleId());
					if (locateVehicleList.contains(vehicle)) {
						isCostSearchFromStopNode = false;
						message.append("(M)\t");
					} else if (loadVehicleList.contains(vehicle)) {
						isCostSearchFromStopNode = false;
						message.append("(O)\t");
					} else {
						isCostSearchFromStopNode = true;
						message.append("(F)\t");
					}
					if (costSearch.costSearchForLocalOHT(vehicle, fromNode, costMap, isCostSearchFromStopNode, searchLimit, costSearchOption)) {
						for (int j = 0; j < targetLocalGroupList.size(); j++) {
							localGroupInfo = targetLocalGroupList.get(j);
							if (localGroupInfo != null) {
								bay = localGroupInfo.getBay();
								/* 2018.07.23 by LSH : 동일 Bay를 대상으로 한 서로 다른 Material 속성의 Local OHT 설정 보완
								 * (예: 동일 Bay에 Foup, Reticle Local OHT & Port 각각 설정)
								 */
//								if (costMap.containsKey(bay)) {
								if (costMap.containsKey(bay) && isLocalOHTAssignable(vehicle, localGroupInfo)) {
									cost[i][j] = (double)costMap.get(bay);
									if (cost[i][j] > localGroupInfo.getDistance()) {
										cost[i][j] = MAXCOST_TIMEBASE;
									}
									costBackup[i][j] = cost[i][j];
									message.append(getCostInSixDigits(cost[i][j]));
								} else {
									message.append(MAXCOST_TIMEBASE);
								}
							}
							if (j < targetLocalGroupList.size() - 1) {
								message.append(", ");
							}
						}
					}
					if (isJobAssignDetailResultUsed) {
						traceJobAssignMain("     [Cost for LocalGroup] " + message.toString());
					}
				}
			}
		}
		
		costSearchedTime = System.currentTimeMillis();
		long costSearchTime = costSearchedTime - costSearchStartTime;
		traceJobAssignMain("     [Time-CostSearch] " + costSearchTime + "[msec]" );
		
		if (costSearchTime > jobAssignDelayLimit) {
			StringBuilder message = new StringBuilder();
			message.append("     [Cost Search] Vehicle Count:").append(candidateVehicleList.size());
			message.append(", LocalGroup Count:").append(targetLocalGroupList.size());
			message.append(", DelayTime:").append(costSearchTime).append("[msec]");
			traceJobAssignDelay(message.toString());
		}
	}
	
	/**
	 * Step 2-3-6 LocalOHT 선정 
	 * 
	 */
	private void allocateLocalGroup() {
		try {
			resultVehicleList.clear();
			resultTargetList.clear();
			
			allocateStartedTime = System.currentTimeMillis();
			
			if (solver.solveAssignment(size, cost, result)) {
				double jobAssignCost;
				Vehicle vehicle = null;
				LocalGroupInfo localGroupInfo = null;
				for (int i = 0; i < candidateVehicleList.size(); i++) {
					if (result[i] < targetLocalGroupList.size()) {
						vehicle = candidateVehicleList.get(i);
						localGroupInfo = targetLocalGroupList.get(result[i]);
						if (vehicle != null && localGroupInfo != null) {
							jobAssignCost = costBackup[i][result[i]];
							if (jobAssignCost >= OcsConstant.MAXCOST_TIMEBASE) {
								continue;
							}
							
							if (resultVehicleList.contains(vehicle) == false &&
									resultTargetList.contains(localGroupInfo) == false) {
								
								resultVehicleList.add(vehicle);
								resultTargetList.add(localGroupInfo);
								
								registerLocalGroupOfVehicle(vehicle, localGroupInfo.getLocalGroupId());
								
								StringBuilder message = new StringBuilder();
								message.append("     [Assigned] ").append(vehicle.getVehicleId());
								message.append(" C/LocalGroupId=").append(localGroupInfo.getLocalGroupId());
								message.append("/Modeling Cost=").append(String.format("%,.2f", jobAssignCost));
								traceJobAssignMain(message.toString());
							} else {
								StringBuilder message = new StringBuilder();
								message.append("[Abnormal Case] Vehicle:").append(vehicle.getVehicleId());
								message.append(", LocalGroup:").append(localGroupInfo.getLocalGroupId());
								message.append(", ResultVehicleList:").append(resultVehicleList.toString());
								message.append(", ResultTargetList:").append(resultTargetList.toString());
								traceJobAssignException(message.toString());
							}
						} else {
							StringBuilder message = new StringBuilder();
							message.append("[Abnormal Case] Vehicle:").append(vehicle);
							message.append(", LocalGroup:").append(localGroupInfo);
							traceJobAssignException(message.toString());
						}
					}
				}
			} else {
				traceJobAssignMain("     [AP_Failed] AP_Infinite_Loop");
			}
			
			allocateCompletedTime = System.currentTimeMillis();
			printAllocationCompleted(allocateCompletedTime - allocateStartedTime);
			
			vehicleManager.updateVehicleLocationFromDBForJobAssign();
		} catch (Exception e) {
			traceJobAssignException("     [AP_Exception] ", e);
		}
	}
	
	/**
	 * Step 3-2-1 Select LocalTrCmds
	 * 
	 */
	private void selectLocalTrCmds() {
		try {
			localTrCmdList.clear();
			localTrCmdGroupIdMap.clear();
			localTrCmdCount = 0;
			
			if (isLocalOHTUsed && idleLocalVehicleList.size() > 0) {
				TrCmd trCmd = null;
				Node sourceNode = null;
				Node destNode = null;
				CarrierLoc sourceLoc = null;
				CarrierLoc destLoc = null;
				String sourceLocId = "";
				String destLocId = "";
				String sourceLocLocalGroupId = "";
				String destLocLocalGroupId = "";
				LocalGroupInfo sourceLocLocalGroupInfo = null;
				LocalGroupInfo destLocLocalGroupInfo = null;
				String sourceBay = "";
				String destBay = "";
				Iterator<TrCmd> it = unassignedTrCmdList.iterator();
				while (it.hasNext()) {
					trCmd = it.next();
					if (trCmd != null) {
						sourceLocId = trCmd.getSourceLoc();
						sourceLocLocalGroupId = "";
						sourceLocLocalGroupInfo = null;
						if (sourceLocId != null && sourceLocId.length() > 0) {
							sourceLoc = (CarrierLoc)carrierLocManager.getCarrierLocData(sourceLocId);
							if (sourceLoc != null) {
								sourceLocLocalGroupId = sourceLoc.getLocalGroupId();
								if (sourceLocLocalGroupId != null && sourceLocLocalGroupId.length() > 0) {
									sourceLocLocalGroupInfo = localGroupInfoManager.getLocalGroupInfo(sourceLocLocalGroupId);
								}
							}
						}
						destLocId = trCmd.getDestLoc();
						destLocLocalGroupId = "";
						destLocLocalGroupInfo = null;
						if (destLocId != null && destLocId.length() > 0) {
							destLoc = (CarrierLoc)carrierLocManager.getCarrierLocData(destLocId);
							if (destLoc != null) {
								destLocLocalGroupId = destLoc.getLocalGroupId();
								if (destLocLocalGroupId != null && destLocLocalGroupId.length() > 0) {
									destLocLocalGroupInfo = localGroupInfoManager.getLocalGroupInfo(destLocLocalGroupId);
								}
							}
						}
						sourceNode = nodeManager.getNode(trCmd.getSourceNode());
						sourceBay = "";
						if (sourceNode != null) {
							sourceBay = sourceNode.getBay();
						}
						destNode = nodeManager.getNode(trCmd.getDestNode());
						destBay = "";
						if (destNode != null) {
							destBay = destNode.getBay();
						}
						
						if (sourceLocLocalGroupInfo != null &&
								LOCAL.equals(sourceLocLocalGroupInfo.getAssignOption()) &&
								localGroupIdList.contains(sourceLocLocalGroupId)) {
							if (localTrCmdList.contains(trCmd) == false) {
								localTrCmdList.add(trCmd);
							}
							localTrCmdGroupIdMap.put(trCmd, sourceLocLocalGroupId);
						} else if (destLocLocalGroupInfo != null &&
								sourceBay.equals(destBay) &&
								LOCAL.equals(destLocLocalGroupInfo.getAssignOption()) &&
								localGroupIdList.contains(destLocLocalGroupId)) {
							if (localTrCmdList.contains(trCmd) == false) {
								localTrCmdList.add(trCmd);
							}
							localTrCmdGroupIdMap.put(trCmd, destLocLocalGroupId);
						}
					}
				}
				localTrCmdCount = localTrCmdList.size();
			}
		} catch (Exception e) {
			traceJobAssignException("", e);
		}
	}
	
	/**
	 * Step 3-2-2 Select Unassigned TrCmds and Build TargetTrCmdList & TargetNodeList
	 * 
	 */
	private void selectUnassignedTrCmds() {
		targetTrCmdList.clear();
		targetNodeList.clear();
		waitingTimeMap.clear();
		// added by KYK
		targetList.clear();
		targetStationList.clear();
		targetStationMap.clear();
		
		TrCmd trCmd = null;
		if (isLocalOHTUsed && localTrCmdCount > 0) {
			Iterator<TrCmd> itLocalTrCmd = localTrCmdList.iterator();
			while (itLocalTrCmd.hasNext()) {
				trCmd = itLocalTrCmd.next();
				registerToTargetList(trCmd);
			}
		}
		
		Iterator<TrCmd> itTrCmd = unassignedTrCmdList.iterator();
		while (itTrCmd.hasNext()) {
			trCmd = itTrCmd.next();
			if (trCmd != null) {
				registerToTargetList(trCmd);
			}
		}
	}
	
	private void registerToTargetList(TrCmd trCmd) {
		if (trCmd != null) {
			try {
				long queuedTime = 0;
				long waitingTime = 0;
				double loadTransferTime = 0;
				String trQueuedTime = trCmd.getTrQueuedTime();
				String sourceNodeId = "";
				String assignableVehicleZone = "";
				Node sourceNode = null;
				Node destNode = null;
				String sourceStationId = "";
				
				if (targetTrCmdList.contains(trCmd) == false) {
					sourceNodeId = trCmd.getSourceNode();
					targetTrCmdList.add(trCmd);
					
					// 2014.02.28 by MYM : [Stage Locate 기능]
					if (isStageLocateUsage && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
						Node targetNode = stageTargetNodeMap.get(trCmd);
						if (targetNode != null) {
							sourceNodeId = targetNode.getNodeId();
							// 2015.03.20 by MYM : sourceNodeId는 무조건 add하도록 수정 
							// 배경 : targetNode가 Null인 경우 targetList와 targetTrCmdList의 사이즈가 달라질 수 있음.(getCostJob에서 Exception 발생 가능)
//							targetNodeList.add(sourceNodeId);
//							targetList.add(sourceNodeId);
						}
						targetNodeList.add(sourceNodeId);
						targetList.add(sourceNodeId);
					} else {
						targetNodeList.add(sourceNodeId);
						// 2013.05.16 by KYK
						sourceStationId = getStationIdAtPort(trCmd.getSourceLoc());
						if (sourceStationId != null && sourceStationId.length() > 0) {
							targetList.add(sourceStationId);
							targetStationList.add(sourceStationId);
							targetStationMap.put(sourceStationId, sourceNodeId);
						} else {
							targetList.add(sourceNodeId);						
						}
					}
										
					if (trQueuedTime != null && trQueuedTime.length() == 14) {
						queuedTime = sdf.parse(trCmd.getTrQueuedTime()).getTime();
						waitingTime = (long) (currentDBTime - queuedTime)/1000;
						if (waitingTime < 0) {
							waitingTime = 0;
						}
						waitingTimeMap.put(trCmd, waitingTime);
					}
					
					if (trCmd.getLoadTransferTime() == MAXCOST_TIMEBASE) {
						sourceNode = nodeManager.getNode(trCmd.getSourceNode());
						destNode = nodeManager.getNode(trCmd.getDestNode());
						if (sourceNode == null || destNode == null) {
							return;
						}
						assignableVehicleZone = getAssignableVehicleZone(trCmd);
						if (assignableVehicleZone != null && assignableVehicleZone.length() > 0) {
							loadTransferTime = costSearch.costSearchForLoadTransferTime(trCmd, assignableVehicleZone, sourceNode, destNode, costSearchOption);
							trCmd.setLoadTransferTime(loadTransferTime);
						}
					}
				}
			} catch (Exception e) {
				traceJobAssignException("", e);
			}
		}
	}
	
	/**
	 * Step 3-2-3 Select Park Nodes and Build TargetNodeList
	 * 
	 */
	private void selectParkNodes() {
		parkList.clear();
		targetParkList.clear();
		
		if (isParkNodeUsed) {
			if (isParkSearchNeeded) {
				Vehicle vehicle = null;
				Park park = null;
				String parkName = "";
				ConcurrentHashMap<String, Object> tempParkList = parkManager.getData();
				Set<String> searchKeys = new HashSet<String>(tempParkList.keySet());
				for (String searchKey : searchKeys) {
					park = (Park)tempParkList.get(searchKey);
					if (park != null) {
						parkList.put(searchKey, park);
					}
				}
				ArrayList<String> tempTargetParkList = new ArrayList<String>();
				Set<String> searchParkKeys = new HashSet<String>(parkList.keySet());
				for (String searchKey : searchParkKeys) {
					park = (Park)parkList.get(searchKey);
					if (park != null) {
						if (park.isNodeType() ||
								idleVehicleLocalGroupIdList.contains(park.getType())) {
							manageParkCapacity(park);
						} else {
							StringBuilder message = new StringBuilder();
							message.append("     [Ignored ParkNode] ").append(park.getName()).append("(").append(park.getType()).append(") has been ignored. (No Idle Local Vehicle)");
							traceJobAssignMain(message.toString());
						}
					}
				}
				
				Set<Vehicle> searchRequestedKeys = new HashSet<Vehicle>(requestedParkMap.keySet());
				for (Vehicle searchRequestedKey : searchRequestedKeys) {
					park = (Park)requestedParkMap.get(searchRequestedKey);
					if (park != null) {
						if (targetParkList.contains(park) == false) {
							cancelRequestedPark(searchRequestedKey, park);
						}
					}
				}
				
				Iterator<Park> itPark = targetParkList.iterator();
				while (itPark.hasNext()) {
					park = itPark.next();
					if (park != null) {
						parkName = park.getName();
						if (tempTargetParkList.contains(parkName) == false) {
							tempTargetParkList.add(parkName);
						}
					}
				}
				
				Iterator<Vehicle> itCandidate = candidateVehicleList.iterator();
				String localGroupId = "";
				while (itCandidate.hasNext()) {
					vehicle = itCandidate.next();
					if (vehicle != null) {
						if (vehicle.getStopNode().equals(vehicle.getTargetNode())) {
							localGroupId = vehicle.getLocalGroupId();
							if (localGroupId != null && localGroupId.length() > 0) {
								if (parkList.containsKey(vehicle.getStopNode())) {
									park = (Park) parkList.get(vehicle.getStopNode());
									if (localGroupId.equals(park.getType())) {
										tempTargetParkList.remove(vehicle.getStopNode());
									}
								}
							} else {
								if (parkList.containsKey(vehicle.getStopNode())) {
									park = (Park) parkList.get(vehicle.getStopNode());
									if (park.isNodeType()) {
										tempTargetParkList.remove(vehicle.getStopNode());
									}
								}
							}
						}
					}
				}
				
				if (tempTargetParkList.size() == 0) {
					targetParkList.clear();
				}
				
				Iterator<Park> it = targetParkList.iterator();
				while (it.hasNext()) {
					park = it.next();
					if (park != null) {
						targetList.add(park.getName());
					}
				}
			}
		}
	}
	
	/**
	 * 2013.05.16 by KYK
	 * @param vehicle
	 * @return
	 */
	private boolean updateAvailableNodeList(Vehicle vehicle) {
		assert vehicle != null;
		
		costMap.clear();
		TrCmd trCmd = null;
		Park park = null;
		String targetId = "";
		String targetNodeId = "";
		
		for (int j = 0; j < targetList.size(); j++) {
			targetId = targetList.get(j);
			if (targetStationMap.containsKey(targetId)) {
				targetNodeId = targetStationMap.get(targetId);
			} else {
				targetNodeId = targetId;
			}
			
			if (j < targetTrCmdList.size()) {
				trCmd = targetTrCmdList.get(j);
				if (trCmd != null) {
					// 2014.02.28 by MYM : [Stage Locate 기능]
					Node stageTargetNode = stageTargetNodeMap.get(trCmd);
					if (targetNodeId.equals(trCmd.getSourceNode())
							|| (stageTargetNode != null && targetNodeId.equals(stageTargetNode.getNodeId()))) {
						if (isJobAssignable(vehicle, trCmd)) {
							if (vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0) {
								if (isLocalOHTJobAssignable(vehicle, trCmd)) {
									costMap.put(targetId, MAXCOST_TIMEBASE);
									continue;
								}
							} else {
								if (isAreaBalancingUsed == false || checkAreaBalancing(vehicle, trCmd)) {
									// 2022.03.11 dahye : Stage Assign(Reserve) Logic Improve
									// STAGE - ONLY Assign (NOT Reserve)
									// 	AS-IS : IDLE(idle + loading) + LOAD(loadAssigned~)
									// 	TO-BE : IDLE(idle + loading)
//									costMap.put(targetId, MAXCOST_TIMEBASE);
//									continue;
									if ((trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE && loadVehicleList.contains(vehicle)) == false) {
										costMap.put(targetId, MAXCOST_TIMEBASE);
										continue;
									}
								}
							}
						}	
					}
				}
			} else if (targetParkList.size() > 0) {
				park = targetParkList.get(j - targetTrCmdList.size());
				if (park != null) {
					if (targetNodeId.equals(park.getName())) {
						if (isParkAllowed(vehicle, park)) {
							if (vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0) {
								if (vehicle.getLocalGroupId().equals(park.getType())) {
									costMap.put(targetId, MAXCOST_TIMEBASE);
									continue;
								}
							} else {
								if (park.isNodeType()) {
									if (isAreaBalancingUsed == false || checkAreaBalancing(vehicle, park)) {
										costMap.put(targetId, MAXCOST_TIMEBASE);
										continue;
									}
								}
							}
						}
					}
				}
			}
			costNodesBackup[candidateVehicleList.indexOf(vehicle)][j] = MAXCOST_TIMEBASE;
		}
		return (costMap.size() > 0);
	}
	
	
	/**
	 * 
	 * @param vehicle
	 * @param trCmd
	 * @param modelingCost
	 */
	private void allocateVehicleToJob(Vehicle vehicle, TrCmd trCmd, double modelingCost, double distanceBasedCost) {
		assert vehicle != null;
		assert trCmd != null;
		
		if (locateVehicleList.contains(vehicle)) {
			StringBuilder message = new StringBuilder();
			message.append("     [LocateCancel] ").append(vehicle.getVehicleId());
			message.append(" Node=").append(vehicle.getTargetNode());
			if (vehicleManager.updateVehicleRequestedInfoToDB(LOCATECANCEL, vehicle.getTargetNode(), vehicle.getVehicleId())) {
				locateVehicleList.remove(vehicle);
			} else {
				message.append(" Failed.");
			}
			traceJobAssignMain(message.toString());
		}
		if (reservedTrCmdMap.containsKey(trCmd)) {
			Vehicle locatingVehicle = (Vehicle)reservedTrCmdMap.get(trCmd);
			if (vehicle != locatingVehicle) {
				String sourceNodeId = trCmd.getSourceNode();
				StringBuilder message = new StringBuilder();
				message.append("     [LocateCancel] ").append(locatingVehicle.getVehicleId());
				message.append(" Node=").append(sourceNodeId);
				if (vehicleManager.updateVehicleRequestedInfoToDB(LOCATECANCEL, sourceNodeId, locatingVehicle.getVehicleId())) {
					locateVehicleList.remove(locatingVehicle);
				} else {
					message.append(" Failed.");
				}
				traceJobAssignMain(message.toString());
			}
			reservedTrCmdMap.remove(trCmd);
		}
		if (reservedVehicleMap.containsKey(vehicle)) {
			reservedVehicleMap.remove(vehicle);
		} else {
			cancelReservationFromAnotherVehicle(vehicle, trCmd);
		}
		
		// 2014.02.27 by MYM : [Stage Locate 기능]
		cancelReservedStageInfo(vehicle, trCmd);
		
		registerAssignedVehicleToTrCmd(vehicle, trCmd);
		updateResultToList(vehicle, trCmd);
		
		if (isAreaBalancingUsed) {
			updateAreaVehicleCount(vehicle, trCmd.getSourceNode());
		}
		
		StringBuilder message = new StringBuilder();
		message.append("     [Assigned] ").append(vehicle.getVehicleId());
		message.append(" C/TrCMDID=").append(trCmd.getTrCmdId());
		message.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
		message.append("/Distance-based Cost=").append(String.format(FORMAT, distanceBasedCost));
		message.append("/SourceNode=").append(trCmd.getSourceNode());
		message.append("/StopNode=").append(vehicle.getStopNode());
		traceJobAssignMain(message.toString());
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param trCmd
	 * @param modelingCost
	 */
	private void reserveVehicleToJob(Vehicle vehicle, TrCmd trCmd, double modelingCost, double distanceBasedCost) {
		assert vehicle != null;
		assert trCmd != null;
		
		reserveJob(vehicle, trCmd);
		updateResultToList(vehicle, trCmd);
		
		if (isAreaBalancingUsed) {
			updateAreaVehicleCount(vehicle, trCmd.getSourceNode());
		}
		
		StringBuilder message = new StringBuilder();
		message.append("     [Reserved] ").append(vehicle.getVehicleId());
		message.append(" M/TrCMDID=").append(trCmd.getTrCmdId());
		message.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
		message.append("/Distance-based Cost=").append(String.format(FORMAT, distanceBasedCost));
		message.append("/SourceNode=").append(trCmd.getSourceNode());
		message.append("/StopNode=").append(vehicle.getStopNode());
		traceJobAssignMain(message.toString());
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param trCmd
	 */
	private void canNotAllocateVehicleToJobByAreaBalancing(Vehicle vehicle, TrCmd trCmd) {
		assert vehicle != null;
		assert trCmd != null;
		
		StringBuilder message = new StringBuilder();
		message.append("     [Cannot be assigned]").append(vehicle.getVehicleId());
		message.append(" C/TrCMDID=").append(trCmd.getTrCmdId());
		message.append(" [AreaBalancing]");
		traceJobAssignMain(message.toString());
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param trCmd
	 * @param modelingCost
	 */
	private void canNotAllocateVehicleToNextJob(Vehicle vehicle, TrCmd trCmd, double modelingCost) {
		assert vehicle != null;
		assert trCmd != null;
		
		StringBuilder message = new StringBuilder();
		message.append("     [Cannot be assigned to NextJob]").append(vehicle.getVehicleId());
		message.append(" N/TrCMDID=").append(trCmd.getTrCmdId());
		message.append("/SourceLoc=").append(trCmd.getSourceLoc());
		message.append(" [Another VHL should unload from the sourceloc.]");
		traceJobAssignMain(message.toString());
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param trCmd
	 * @param modelingCost
	 */
	private void reserveVehicleToNextJob(Vehicle vehicle, TrCmd trCmd, double modelingCost, double distanceBasedCost) {
		assert vehicle != null;
		assert trCmd != null;
		
		if (reservedTrCmdMap.containsKey(trCmd)) {
			Vehicle locatingVehicle = (Vehicle)reservedTrCmdMap.get(trCmd);
			if (vehicle != locatingVehicle) {
				// vehicle은 Loading 중인 VHL이기 때문에, locatingVehicle과는 다름.
				String sourceNodeId = trCmd.getSourceNode();
				StringBuilder message = new StringBuilder();
				message.append("     [LocateCancel] ").append(locatingVehicle.getVehicleId());
				message.append(" Node=").append(sourceNodeId);
				if (vehicleManager.updateVehicleRequestedInfoToDB(LOCATECANCEL, sourceNodeId, locatingVehicle.getVehicleId())) {
					locateVehicleList.remove(locatingVehicle);
				} else {
					message.append(" Failed.");
				}
				traceJobAssignMain(message.toString());
			}
			reservedTrCmdMap.remove(trCmd);
		}
		
		reservedVehicleMap.put(vehicle, trCmd);
		cancelReservationFromAnotherVehicle(vehicle, trCmd);
		
		updateResultToList(vehicle, trCmd);
		
		StringBuilder message = new StringBuilder();
		message.append("     [Reserved] ").append(vehicle.getVehicleId());
		message.append(" N/TrCMDID=").append(trCmd.getTrCmdId());
		message.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
		message.append("/Distance-based Cost=").append(String.format(FORMAT, distanceBasedCost));
		message.append("/SourceNode=").append(trCmd.getSourceNode());
		message.append("/StopNode=").append(vehicle.getStopNode());
		traceJobAssignMain(message.toString());
		
		if (waitingTimeMap.containsKey(trCmd)) {
			long waitingTime = (waitingTimeMap.get(trCmd)).longValue();
			if (distanceBasedCost > 20 ||
					(waitingTime > 30 && distanceBasedCost > 10)) {
				reservedAsNextTrCmdSet.add(new ReservedAsNextTrCmd(trCmd, waitingTime, distanceBasedCost));
			}
		}
	}
	
	private void allocateVehicleToPark(Vehicle vehicle, Park park, double modelingCost, double distanceBasedCost) {
		assert vehicle != null;
		assert park != null;
		
		resultVehicleList.add(vehicle);
		resultTargetList.add(park);
		reservedVehicleMap.remove(vehicle);
		tempRequestedParkMap.put(vehicle, park);
		
		String parkNodeId = park.getName();
		if (vehicle.getTargetNode().equals(parkNodeId) == false) {
			requestPark(vehicle, park);
			if (isAreaBalancingUsed) {
				updateAreaVehicleCount(vehicle, parkNodeId);
			}
			StringBuilder message = new StringBuilder();
			message.append("     [Requested] ").append(vehicle.getVehicleId());
			message.append(" C/ParkNode=").append(parkNodeId);
			message.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
			message.append("/Distance-based Cost=").append(String.format(FORMAT, distanceBasedCost));
			message.append("/StopNode=").append(vehicle.getStopNode());
			message.append("/TargetNode=").append(vehicle.getTargetNode());
			traceJobAssignMain(message.toString());
		} else {
			if (vehicle.getStopNode().equals(parkNodeId)) {
				if (vehicle.getCurrNode().equals(parkNodeId)) {
					StringBuilder message = new StringBuilder();
					message.append("     [Parked] ").append(vehicle.getVehicleId());
					message.append(" P/ParkNode=").append(parkNodeId);
					traceJobAssignMain(message.toString());
					return;
				}
			}
			StringBuilder message = new StringBuilder();
			message.append("     [Requested] ").append(vehicle.getVehicleId());
			message.append(" M/ParkNode=").append(parkNodeId);
			message.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
			message.append("/Distance-based Cost=").append(String.format(FORMAT, distanceBasedCost));
			message.append("/StopNode=").append(vehicle.getStopNode());
			message.append("/TargetNode=").append(vehicle.getTargetNode());
			traceJobAssignMain(message.toString());
		}
	}
	
	private void manageRequestedPark() {
		Park requestedPark = null;
		Set<Vehicle> searchTempKeys = new HashSet<Vehicle>(tempRequestedParkMap.keySet());
		for (Vehicle searchKey : searchTempKeys) {
			if (searchKey != null && requestedParkMap.containsKey(searchKey)) {
				requestedParkMap.remove(searchKey);
			}
		}
		
		Set<Vehicle> searchKeys = new HashSet<Vehicle>(requestedParkMap.keySet());
		for (Vehicle searchKey : searchKeys) {
			if (searchKey != null) {
				requestedPark = (Park)requestedParkMap.get(searchKey);
				if (requestedPark != null) {
					cancelRequestedPark(searchKey, requestedPark);
				}
			}
		}
		
		requestedParkMap.clear();
		Set<Vehicle> searchParkKeys = new HashSet<Vehicle>(tempRequestedParkMap.keySet());
		for (Vehicle searchKey : searchParkKeys) {
			requestedPark = (Park)tempRequestedParkMap.get(searchKey);
			if (searchKey != null && requestedPark != null) {
				requestedParkMap.put(searchKey, requestedPark);
			}
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param target
	 */
	private void updateResultToList(Vehicle vehicle, Object target) {
		assert vehicle != null;
		assert target != null;
		
		if (resultVehicleList.contains(vehicle) == false) {
			resultVehicleList.add(vehicle);
		}
		if (resultTargetList.contains(target) == false) {
			resultTargetList.add(target);
		}
		
		if (idleLocalVehicleList.contains(vehicle)) {
			idleLocalVehicleList.remove(vehicle);
		}
		if (idleVehicleList.contains(vehicle)) {
			idleVehicleList.remove(vehicle);
		}
		if (unassignedTrCmdList.contains((TrCmd)target)) {
			unassignedTrCmdList.remove((TrCmd)target);
		}
		// 2014.02.27 by MYM : [Stage Locate 기능]
		if (unassignedStageList.contains((TrCmd)target)) {
			unassignedStageList.remove((TrCmd)target);
		}
	}
	
	/**
	 * Register LOCATE Request to Vehicle
	 * 
	 * @param vehicle
	 * @param trCmd
	 */
	private void reserveJob(Vehicle vehicle, TrCmd trCmd) {
		if (vehicle != null && trCmd != null) {
			String nodeId = trCmd.getSourceNode();
			if (nodeId.equals(vehicle.getTargetNode()) == false) {
				vehicleManager.updateVehicleRequestedInfoToDB(LOCATE, nodeId, vehicle.getVehicleId());
			}
			reservedTrCmdMap.put(trCmd, vehicle);
			reservedVehicleMap.put(vehicle, trCmd);
			cancelReservationFromAnotherVehicle(vehicle, trCmd);
		}
	}
	
	private void cancelReservationFromAnotherVehicle(Vehicle vehicle, TrCmd trCmd) {
		if (vehicle != null && trCmd != null) {
			TrCmd reservedTrCmd = null;
			Set<Vehicle> searchKeys = new HashSet<Vehicle>(reservedVehicleMap.keySet());
			for (Vehicle searchKey : searchKeys) {
				reservedTrCmd = (TrCmd)reservedVehicleMap.get(searchKey);
				if (trCmd == reservedTrCmd && vehicle != searchKey) {
					reservedVehicleMap.remove(searchKey);
					break;
				}
			}
		}
	}
	
	/**
	 * Request Park to Vehicle
	 * 
	 * @param vehicle
	 * @param parkNodeId
	 */
	private void requestPark(Vehicle vehicle, Park park) {
		if (vehicle != null && park != null) {
			String parkNodeId = park.getName();
			if (parkNodeId.equals(vehicle.getTargetNode()) == false) {
				if (park.getRank() < 6) {
					// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
					if(isParkCmdEqoption && parkManager.isExistPortofPark(parkNodeId))
						vehicleManager.updateVehicleRequestedInfoToDB(PLOCATE, parkNodeId, vehicle.getVehicleId());
					else
						vehicleManager.updateVehicleRequestedInfoToDB(LOCATE, parkNodeId, vehicle.getVehicleId());
				} else {
					// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
					if(isParkCmdEqoption && parkManager.isExistPortofPark(parkNodeId))
						vehicleManager.updateVehicleRequestedInfoToDB(PMOVE, parkNodeId, vehicle.getVehicleId());
					else
						vehicleManager.updateVehicleRequestedInfoToDB(MOVE, parkNodeId, vehicle.getVehicleId());
				}	
			}
		}
	}
	
	/**
	 * Cancel Requested Park to Vehicle
	 * 
	 * @param vehicle
	 * @param park
	 */
	private void cancelRequestedPark(Vehicle vehicle, Park park) {
		StringBuilder message = new StringBuilder();
		if (vehicle != null && park != null) {
			if (vehicle.getTargetNode().equals(vehicle.getStopNode()) == false &&
					vehicle.getTargetNode().equals(park.getName())) {
				message.append("     [ParkCancel] ").append(vehicle.getVehicleId());
				message.append(" ParkNode=").append(park.getName());
				if (reservedVehicleList.contains(vehicle) == false) {
					if (vehicleManager.updateVehicleRequestedInfoToDB(LOCATECANCEL, vehicle.getTargetNode(), vehicle.getVehicleId())) {
						if (locateVehicleList.contains(vehicle)) {
							locateVehicleList.remove(vehicle);
						}
						message.append(" Requested.");
					} else {
						message.append(" Failed.");
					}
				} else {
					message.append(" (JobReserved).");
				}
				traceJobAssignMain(message.toString());
			}
		} else {
			message.append("cancelRequestedPark() -");
			if (vehicle == null) {
				message.append(" vehicle is null");
			}
			if (park == null) {
				message.append(" park is null");
			}
			traceJobAssignException(message.toString());
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param nodeId
	 */
	private void updateAreaVehicleCount(Vehicle vehicle, String nodeId) {
		assert vehicle != null;
		
		try {
			Area stopArea = null;
			Node stopNode = nodeManager.getNode(vehicle.getStopNode());
			if (stopNode != null) {
				stopArea = stopNode.getArea();
			} else {
				return;
			}
			
			Area targetArea = null;
			Node targetNode = nodeManager.getNode(vehicle.getTargetNode());
			if (targetNode != null) {
				targetArea = targetNode.getArea();
			} else {
				return;
			}
			
			Area sourceArea = null;
			Node sourceNode = nodeManager.getNode(nodeId);
			if (sourceNode != null) {
				sourceArea = sourceNode.getArea();
			} else {
				return;
			}
			
			if (stopArea == targetArea) {
				if (stopArea != null) {
					stopArea.removeStayingIdleVehicle(vehicle);
				}
				if (stopArea != sourceArea) {
					if (stopArea != null) {
						stopArea.addOutGoingVehicle(vehicle);
						stopArea.removeStayingVehicle(vehicle);
					}
					if (sourceArea != null) {
						sourceArea.addInComingVehicle(vehicle);
					}
				}
			} else {
				if (stopArea != null) {
					stopArea.removeOutGoingIdleVehicle(vehicle);
				}
				if (targetArea != null) {
					targetArea.removeInComingIdleVehicle(vehicle);
				}
				if (stopArea == sourceArea) {
					// targetArea != sourceArea
					if (stopArea != null) {
						stopArea.addStayingVehicle(vehicle);
					}
					if (targetArea != null) {
						targetArea.removeInComingVehicle(vehicle);
					}
				} else {
					if (targetArea != sourceArea) {
						if (targetArea != null) {
							targetArea.removeInComingVehicle(vehicle);
						}
						if (sourceArea != null) {
							sourceArea.addInComingVehicle(vehicle);
						}
					}
				}
			}
		} catch (Exception e) {
			traceJobAssignException("", e);
		}
	}
	
	/**
	 * Register LocalGroup to a Vehicle
	 * 
	 * @param vehicleId
	 * @param localGroupId
	 */
	private void registerLocalGroupOfVehicle(Vehicle vehicle, String localGroupId) {
		if (vehicle != null && localGroupId != null && localGroupId.length() > 0) {
			vehicleManager.addVehicleToRegisterLocalGroupList(vehicle.getVehicleId(), localGroupId);
			vehicle.setLocalGroupId(localGroupId);
			
			if (localVehicleList.contains(vehicle) == false) {
				localVehicleList.add(vehicle);
			}
			if (idleVehicleList.contains(vehicle) &&
					idleLocalVehicleList.contains(vehicle) == false) {
				idleLocalVehicleList.add(vehicle);
			}
		}
	}
	
	/**
	 * 
	 */
	private void printAllocationCompleted(long allocationTime) {
		StringBuilder message = new StringBuilder();
		message.append("     [Time-Allocation] ").append(allocationTime);
		message.append("[msec] DispatchingRule:").append(dispatchingRule.toConstString());
		traceJobAssignMain(message.toString());
	}
	
	/**
	 * double 형식의 cost 값을 6자리 String으로 변환.
	 * (ex. 1234.6, 123.56, 12.456, 1.3456, -2.456, ...)
	 * 
	 * @param value cost
	 * @return cost in six digits
	 */
	private String getCostInSixDigits(double value) {
		String result = null;
		try {
			if (value < -1000) {
				result = String.format(FORMAT0, value);
			} else if (value < -100) {
				result = String.format(FORMAT1, value);
			} else if (value < -10) {
				result = String.format(FORMAT2, value);
			} else if (value < 0) {
				result = String.format(FORMAT3, value);
			} else if (value < 10) {
				result = String.format(FORMAT4, value);
			} else if (value < 100) {
				result = String.format(FORMAT3, value);
			} else if (value < 1000) {
				result = String.format(FORMAT2, value);
			} else if (value < 10000) {
				result = String.format(FORMAT1, value);
			} else {
				result = String.format(FORMAT0, value);
			}
		} catch (Exception e) {
			traceJobAssignException("", e);
		}
		return result;
	}
	
	/** 019.10.15 by kw3711.kim	: MaterialControl Dest 추가
	 * 해당 Vehicle에 대상 TrCmd의 작업할당 가능여부를 확인한다. 
	 * VehicleMaterial, SourceLocMaterial, DestLocMaterial에 대한 동등 비교
	 * VehicleZone과 SourceNode에 대한 AssignAllowance
	 * 
	 * @param vehicle
	 * @param trCmd
	 * @return true if JobAssignable. 
	 */
	private boolean isJobAssignable(Vehicle vehicle, TrCmd trCmd) {
		assert vehicle != null;
		assert trCmd != null;
		
		CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(trCmd.getSourceLoc());
		CarrierLoc destLoc = carrierLocManager.getCarrierLocData(trCmd.getDestLoc());	// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
		Node sourceNode = nodeManager.getNode(trCmd.getSourceNode());
		Node destNode = nodeManager.getNode(trCmd.getDestNode());

		if (sourceLoc != null) {
			// Material AssignAllowance Check
			// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
			// 2013.07.10 by KYK
//			if (isMaterialAssignAllowed(vehicle, sourceLoc) == false) {
			if (isMaterialAssignAllowed(vehicle, sourceLoc, destLoc) == false) {
				return false;
			}
//			if (vehicle.getMaterialIndex() == sourceLoc.getMaterialIndex()) {
			if (sourceNode != null) {
				// Zone AssignAllowance Check
				if (isAssignAllowed(vehicle, sourceNode)) {
					if (loadVehicleList.contains(vehicle)) {
						if (vehicle.getTargetNode().equals(trCmd.getSourceNode()) &&
								destLocOfUnloadedVehicleList.contains(trCmd.getSourceLoc())) {
							// LoadVHL의 DestLoc(TargetNode)과 Unassigned TrCmd의 SourceLoc (SourceNode)가 같은 경우,
							// LoadVHL은 다른 VHL이 Unassigned TrCmd에 대해 Unload 한 뒤에야 LOAD 할 수 있음. 
							return false;
						}
					}
					// 2015.05.08 by KYK : 작업할당(허용)조건추가, destnode로 주행불가(설정)시 할당 안 함
					if (destNode != null) {
						return isDriveAllowed(vehicle, destNode);
					}
					return true;
				}
			}
		}
		
		return false;
	}
	
	// 2015.05.08 by KYK : 작업할당(허용)조건추가, destnode로 주행불가(설정)시 할당 안 함
	private boolean isDriveAllowed(Vehicle vehicle, Node node) {
		if (vehicle.getZoneIndex() == node.getZoneIndex() ||
				driveAllowedList.contains(vehicle.getZone() + "_" + node.getZone())) {
			return true;
		}
		return false;
	}	

	/**
	 * 2013.07.10 by KYK
	 * @param vehicle
	 * @param sourceLoc
	 * @return
	 */
	@Deprecated
	private boolean isMaterialAssignAllowed(Vehicle vehicle, CarrierLoc sourceLoc) {
		if (vehicle.getMaterialIndex() == sourceLoc.getMaterialIndex() ||
			materialAssignAllowedList.contains(vehicle.getMaterial() + "_" + sourceLoc.getMaterial())) {
			return true;
		}
		return false;
	}
	
	/**
	 * 2019.10.15 by kw3711.kim
	 * @param vehicle
	 * @param sourceLoc
	 * @param destLoc
	 * @return	is MaterialAssign Allowed 
	 */
	private boolean isMaterialAssignAllowed(Vehicle vehicle, CarrierLoc sourceLoc, CarrierLoc destLoc) {
		String materialControl = vehicle.getMaterial() + "_" + sourceLoc.getMaterial() + "_" + destLoc.getMaterial();
		if (isIgnoreMaterialDifference) {
			if ((vehicle.getMaterialIndex() == sourceLoc.getMaterialIndex() && sourceLoc.getMaterialIndex() == destLoc.getMaterialIndex()) 
					|| materialAssignAllowedList.contains(materialControl)) {
				return true;
			}
		} else {
			if (vehicle.getMaterialIndex() == sourceLoc.getMaterialIndex() || materialAssignAllowedList.contains(materialControl)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAssignAllowed(Vehicle vehicle, Node node) {
		if (vehicle.getZoneIndex() == node.getZoneIndex() ||
				assignAllowedList.contains(vehicle.getZone() + "_" + node.getZone())) {
			return true;
		}
		return false;
	}
	
	private boolean isParkAllowed(Vehicle vehicle, Park park) {
		if (vehicle.getZoneIndex() == park.getVehicleZoneIndex()) {
			Node parkNode = park.getNode();
			if (parkNode != null) {
				if (vehicle.getZoneIndex() == parkNode.getZoneIndex() ||
						parkAllowedList.contains(vehicle.getZone() + "_" + parkNode.getZone())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Is JobAssignable for the localOHT?
	 * 
	 * @param vehicle
	 * @param trCmd
	 * @return
	 */
	private boolean isLocalOHTJobAssignable(Vehicle vehicle, TrCmd trCmd) {
		assert vehicle != null;
		assert trCmd != null;
		
		if (vehicle != null && trCmd != null &&
				(vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0)) {
			Node sourceNode = nodeManager.getNode(trCmd.getSourceNode());
			Node destNode = nodeManager.getNode(trCmd.getDestNode());
			CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(trCmd.getSourceLoc());
			CarrierLoc destLoc = carrierLocManager.getCarrierLocData(trCmd.getDestLoc());
			LocalGroupInfo localGroupInfo = localGroupInfoManager.getLocalGroupInfo(vehicle.getLocalGroupId());
			
			if (sourceNode == null || destNode == null || sourceLoc == null || destLoc == null || localGroupInfo == null) {
				return false;
			}
			
			if (isJobAssignable(vehicle, trCmd) == false) {
				return false;
			}
			
			if (vehicle.getLocalGroupId().equals(sourceLoc.getLocalGroupId()) == false) {
				if (vehicle.getLocalGroupId().equals(destLoc.getLocalGroupId()) == false) {
					return false;
				} else {
					if (sourceNode.getBay().equals(destNode.getBay()) == false) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
	
	private boolean isLocalOHTAssignable(Vehicle vehicle, LocalGroupInfo localGroupInfo) {
		try {
			if (vehicle != null) {
				if (localGroupInfo != null) {
					Integer materialIndex = (Integer)materialIndexMapOfLocalGroup.get(localGroupInfo.getLocalGroupId());
					if (materialIndex != null && vehicle.getMaterialIndex() == materialIndex.intValue()) {
						return true;
					}
				}
			}
		} catch (Exception e) {}
		return false;
	}
	
	/**
	 * Check HID Status of Work Nodes (SourceNode or DestNode)
	 * @param trCmd
	 * @return
	 */
	private boolean checkHidStatusOfWorkNodes(TrCmd trCmd) {
		assert (trCmd != null);

		Node sourceNode = null;
		Node destNode = null;
		Hid sourceHid = null;
		Hid destHid = null;
		String sourceHidState = OcsConstant.HID_UNKNOWN;
		String destHidState = OcsConstant.HID_UNKNOWN;
		try {
			sourceNode = nodeManager.getNode(trCmd.getSourceNode());
			if (sourceNode != null) {
				sourceHid = (Hid) sourceNode.getHid();
				if (sourceHid != null) {
					sourceHidState = sourceHid.getState(); 
				} else {
					traceJobAssignException("Source HID is not registered. SourceNode:" + trCmd.getSourceNode());
				}
			} else {
				return false;
			}
			destNode = nodeManager.getNode(trCmd.getDestNode());
			if (destNode != null) {
				destHid = (Hid) destNode.getHid();
				if (destHid != null) {
					destHidState = destHid.getState(); 
				} else {
					traceJobAssignException("Dest HID is not registered. DestNode:" + trCmd.getDestNode());
				}
			} else {
				return false;
			}	
		} catch (Exception e) {
			traceJobAssignException("SourceNode:" + trCmd.getSourceNode() + ", DestNode:" + trCmd.getDestNode(), e);
		}
		
		if ((sourceHid != null && sourceHid.isAbnormalState()) || (destHid != null && destHid.isAbnormalState())) {
			StringBuilder message = new StringBuilder();
			message.append("[Job on Abnormal HID] Job Removed. ");
			message.append("TrCmdId:").append(trCmd.getTrCmdId());
			message.append(", SourceNode:").append(trCmd.getSourceNode());
			if (sourceHid != null) {
				message.append("(HID:").append(sourceHid.getUnitId());
				message.append(",Status:").append(sourceHidState);
				message.append(",AltHID:").append(sourceHid.getAltHidName());
				Hid sourceAltHid = sourceHid.getAltHid();
				if (sourceAltHid != null) {
					message.append("(").append(sourceAltHid.getState()).append(")");
				}
			} else {
				message.append("(HID: NOT AVAILABLE");
			}
			message.append("), DestNode:").append(trCmd.getDestNode());
			if (destHid != null) {
				message.append("(HID:").append(destHid.getUnitId());
				message.append(",Status:").append(destHidState);
				message.append(",AltHID:").append(destHid.getAltHidName());
				Hid destAltHid = destHid.getAltHid();
				if (destAltHid != null) {
					message.append("(").append(destAltHid.getState()).append(")");
				}
			} else {
				message.append("(HID: NOT AVAILABLE");
			}
			traceJobAssignMain(message.toString());
			return false;
		} else {
			return true;
		}
	}
	
	private boolean isConveyorType(String carrierLoc) {
		if (carrierLoc != null && carrierLoc.indexOf(CONVEYOR) >= 0) {
			return true;
		} else {
			return false;
		}
	}
	
	// 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
	private boolean checkDupSourceLoc(String sourceLoc) {
		for (TrCmd duplicatedTrCmd : assignedTrCmdList) {
			if (duplicatedTrCmd != null && duplicatedTrCmd.getSourceLoc().equals(sourceLoc)) {
				return true;
			}
		}
		return false;
	}	
	
	private int getPriorityOfTrCmd(TrCmd trCmd) {
		int priorityOfTrCmd = trCmd.getPriority();
		switch (dispatchingRule) {
			case HYBRID2:
			case HYBRID3:
				return priorityOfTrCmd = (priorityOfTrCmd >= jobAssignPriorityThreshold) ? priorityOfTrCmd : 0;
			default:
				return priorityOfTrCmd;
		}
	}
	
	private long getWaitingTimeOfTrCmd(TrCmd trCmd) {
		long waitingTimeOfTrCmd = 0;
		if (waitingTimeMap.containsKey(trCmd)) {
			waitingTimeOfTrCmd = (waitingTimeMap.get(trCmd)).longValue();
		}
		switch (dispatchingRule) {
			case HYBRID2:
			case HYBRID3:
				return waitingTimeOfTrCmd = (waitingTimeOfTrCmd >= jobAssignWaitingTimeThreshold) ? waitingTimeOfTrCmd : 0;
			default:
				return waitingTimeOfTrCmd;
		}
	}
	
	private double getAssignCost(int priority, long waitingTime, double weightOfTrCmd) {
		double cost = 3000;
		switch (dispatchingRule) {
			case STT:
				return 0;
			case HPF:
				cost = 3000 - (jobAssignPriorityWeight * priority);
				break;
			case LWT:
				cost = 3000 - (jobAssignWaitingTimeWeight * waitingTime);
				break;
			case HYBRID2:
			case HYBRID3:
				if (dispatchingRule == DISPATCHING_RULES.HYBRID3 &&
						waitingTime > jobAssignUrgentThreshold) {
					cost = 1000 - (jobAssignWaitingTimeWeight * waitingTime);
				} else {
					if (weightOfTrCmd < 1) {
						cost = 3000 - (jobAssignPriorityWeight * priority + jobAssignWaitingTimeWeight * waitingTime);
					} else {
						cost = 2000 - (jobAssignWaitingTimeWeight * waitingTime);
					}
				}
				break;
			case HYBRID:
			default:
				cost = 3000 - (jobAssignPriorityWeight * priority + jobAssignWaitingTimeWeight * waitingTime);
				break;
		}
		return (cost > 0) ? cost : 0;
	}
	
	private double getPriorCost(TrCmd trCmd, long waitingTimeOfTrCmd) {
		if (destLocOfUnloadedVehicleList.contains(trCmd.getSourceLoc())) {
			return 0;
		} else {
			double loadTransferTime = trCmd.getLoadTransferTime();
			if (waitingTimeOfTrCmd + loadTransferTime >= priorJobCriteriaOfTransferTime &&
					loadTransferTime < MAXCOST_TIMEBASE) {
				return ((waitingTimeOfTrCmd + loadTransferTime < 1000) ? (1000 - waitingTimeOfTrCmd - loadTransferTime) : 0);
			} else if (waitingTimeOfTrCmd >= priorJobCriteriaOfWaitingTime) {
				return ((waitingTimeOfTrCmd < 1000) ? (1000 - waitingTimeOfTrCmd) : 0);
			} else {
				return ((trCmd.getPriority() >= priorJobCriteriaOfPriority) ? 300 - trCmd.getPriority() : 1000);
			}
		}
	}
	
	private double getDistanceCost(Vehicle vehicle, double distanceCost, long waitingTimeOfTrCmd, double weightOfTrCmd) {
		switch (dispatchingRule) {
			case LWT:
			case HPF:
				return distanceCost/100;
			case HYBRID2:
			case HYBRID3:
				if (dispatchingRule == DISPATCHING_RULES.HYBRID3 &&
						waitingTimeOfTrCmd > jobAssignUrgentThreshold) {
					if (idleVehicleList.contains(vehicle)) {
						return distanceCost/100;
					} else {
						return MAXCOST_TIMEBASE;
					}
				} else {
					if (weightOfTrCmd < 1) {
						return distanceCost;
					} else {
						return distanceCost/100;
					}
				}
			case STT:
			case HYBRID:
			default:
				return distanceCost;
		}
	}
	
	private boolean checkAreaBalancing(Vehicle vehicle, TrCmd trCmd) {
		assert vehicle != null;
		assert trCmd != null;
		
		try {
			if (idleVehicleList.contains(vehicle) == false ||
					(vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0)) {
				return true;
			}
			if (areaList.size() == 0) {
				// AREA 테이블이 등록이 안되어 있는 경우.
				return true;
			}
			Node stopNode = nodeManager.getNode(vehicle.getStopNode());
			Node targetNode = nodeManager.getNode(vehicle.getTargetNode());
			Node sourceNode = nodeManager.getNode(trCmd.getSourceNode());
			Node destNode = nodeManager.getNode(trCmd.getDestNode());
			
			if (stopNode == null || sourceNode == null || destNode == null) {
				StringBuilder message = new StringBuilder();
				message.append(" StopNode:").append(vehicle.getStopNode());
				message.append(" SourceNode:").append(trCmd.getSourceNode());
				message.append(" DestNode:").append(trCmd.getDestNode());
				traceJobAssignException(message.toString());
				return false;
			}
			
			String stopAreaId = stopNode.getAreaId();
			String targetAreaId = targetNode.getAreaId();
			String sourceAreaId = sourceNode.getAreaId();
			String destAreaId = destNode.getAreaId();
			Area stopArea = stopNode.getArea();
			Area targetArea = targetNode.getArea();
			Area sourceArea = sourceNode.getArea();
			Area destArea = destNode.getArea();
			if (sourceArea != null) {
				if (sourceArea == stopArea) {
					if (sourceArea.getMinVehicleCount() > 0 &&
							sourceArea.getStayingVehicleCount() <= sourceArea.getMinVehicleCount() &&
								!(sourceArea.isMinVehicleAssignAllowed())) {
						if (sourceArea != destArea) {
							// S1A Reticle Zone의 경우, MinVHL 유지. Zone별 1~2대만 SendFab으로 나가는 반송 담당. -> Min 제약을 깨는 유출 차단.
							StringBuilder message = new StringBuilder();
							message.append(" Vehicle:").append(vehicle.getVehicleId());
							message.append(" TrCmd:").append(trCmd.getTrCmdId());
							message.append(" StopArea:").append(stopAreaId);
							message.append(" SourceArea:").append(sourceAreaId);
							message.append(" DestArea:").append(destAreaId);
							message.append(" SourceStayingV:").append(sourceArea.getStayingVehicleCount());
							message.append(" SourceMinV:").append(sourceArea.getMinVehicleCount());
							traceJobAssignMain(message.toString());
							return false;
						}
					}
				} else {
					if(isAreaBalancingManualExclude){
						if (sourceArea.getCurrentAutoVehicleCount() >= sourceArea.getMaxVehicleCount() ||
								sourceArea.getExpectedAutoVehicleCount() >= sourceArea.getMaxVehicleCount()) {
							// 정체 지역 내의 반송(Source기준)에 대해서는 외부 VHL에 할당 제한 -> 추가 유입 차단
							StringBuilder message = new StringBuilder();
							message.append(" Vehicle:").append(vehicle.getVehicleId());
							message.append(" TrCmd:").append(trCmd.getTrCmdId());
							message.append(" StopArea:").append(stopAreaId);
							message.append(" SourceArea:").append(sourceAreaId);
							message.append(" SourceCurrentAutoV:").append(sourceArea.getCurrentAutoVehicleCount());
							message.append(" SourceExpectedAutoV:").append(sourceArea.getExpectedAutoVehicleCount());
							message.append(" SourceMaxV:").append(sourceArea.getMaxVehicleCount());
							traceJobAssignMain(message.toString());
							return false;
						}
					} else{
						if (sourceArea.getCurrentVehicleCount() >= sourceArea.getMaxVehicleCount() ||
								sourceArea.getExpectedVehicleCount() >= sourceArea.getMaxVehicleCount()) {
							// 정체 지역 내의 반송(Source기준)에 대해서는 외부 VHL에 할당 제한 -> 추가 유입 차단
							StringBuilder message = new StringBuilder();
							message.append(" Vehicle:").append(vehicle.getVehicleId());
							message.append(" TrCmd:").append(trCmd.getTrCmdId());
							message.append(" StopArea:").append(stopAreaId);
							message.append(" SourceArea:").append(sourceAreaId);
							message.append(" SourceCurrentV:").append(sourceArea.getCurrentVehicleCount());
							message.append(" SourceExpectedV:").append(sourceArea.getExpectedVehicleCount());
							message.append(" SourceMaxV:").append(sourceArea.getMaxVehicleCount());
							traceJobAssignMain(message.toString());
							return false;
						}
					}
				}
			}
			
			if (targetArea != null) {
				if(targetArea == stopArea) {
					if (targetArea.getMinVehicleCount() > 0 &&
							targetArea.getStayingVehicleCount() <= targetArea.getMinVehicleCount() &&
							!(targetArea.isMinVehicleAssignAllowed())) {
						if (targetArea != destArea) {
							StringBuilder message = new StringBuilder();
							message.append(" Vehicle:").append(vehicle.getVehicleId());
							message.append(" TrCmd:").append(trCmd.getTrCmdId());
							message.append(" StopArea:").append(stopAreaId);
							message.append(" TargetArea:").append(targetAreaId);
							message.append(" SourceArea:").append(sourceAreaId);
							message.append(" DestArea:").append(destAreaId);
							message.append(" TargetStayingV:").append(targetArea.getStayingVehicleCount());
							message.append(" TargetMinV:").append(targetArea.getMinVehicleCount());
							traceJobAssignMain(message.toString());
							return false;
						}
					}
				}
			}
			
			if (destArea != null) {
				if (destArea != stopArea) {
					if(isAreaBalancingManualExclude){
						if (destArea.getCurrentAutoVehicleCount() >= destArea.getMaxVehicleCount()) {
							// 정체 지역으로의 반송(Dest기준)에 대해서는 외부 VHL에 할당 제한 -> 추가 유입 차단
							StringBuilder message = new StringBuilder();
							message.append(" Vehicle:").append(vehicle.getVehicleId());
							message.append(" TrCmd:").append(trCmd.getTrCmdId());
							message.append(" StopArea:").append(stopAreaId);
							message.append(" DestArea:").append(destAreaId);
							message.append(" DestCurrentAutoV:").append(destArea.getCurrentAutoVehicleCount());
							message.append(" DestExpectedAutoV:").append(destArea.getExpectedAutoVehicleCount());
							message.append(" DestMaxV:").append(destArea.getMaxVehicleCount());
							traceJobAssignMain(message.toString());
							return false;
						}
						if (destArea.getExpectedAutoVehicleCount() >= destArea.getMaxVehicleCount()) {
							// 정체 지역으로의 반송(Dest기준)에 대해서는 외부 VHL에 할당 제한 -> 추가 유입 차단
							if (destArea != targetArea) {
								StringBuilder message = new StringBuilder();
								message.append(" Vehicle:").append(vehicle.getVehicleId());
								message.append(" TrCmd:").append(trCmd.getTrCmdId());
								message.append(" StopArea:").append(stopAreaId);
								message.append(" DestArea:").append(destAreaId);
								message.append(" DestCurrentAutoV:").append(destArea.getCurrentAutoVehicleCount());
								message.append(" DestExpectedAutoV:").append(destArea.getExpectedAutoVehicleCount());
								message.append(" DestMaxV:").append(destArea.getMaxVehicleCount());
								traceJobAssignMain(message.toString());
								return false;
							}
						}
					} else{
						if (destArea.getCurrentVehicleCount() >= destArea.getMaxVehicleCount()) {
							// 정체 지역으로의 반송(Dest기준)에 대해서는 외부 VHL에 할당 제한 -> 추가 유입 차단
							StringBuilder message = new StringBuilder();
							message.append(" Vehicle:").append(vehicle.getVehicleId());
							message.append(" TrCmd:").append(trCmd.getTrCmdId());
							message.append(" StopArea:").append(stopAreaId);
							message.append(" DestArea:").append(destAreaId);
							message.append(" DestCurrentV:").append(destArea.getCurrentVehicleCount());
							message.append(" DestExpectedV:").append(destArea.getExpectedVehicleCount());
							message.append(" DestMaxV:").append(destArea.getMaxVehicleCount());
							traceJobAssignMain(message.toString());
							return false;
						}
						if (destArea.getExpectedVehicleCount() >= destArea.getMaxVehicleCount()) {
							// 정체 지역으로의 반송(Dest기준)에 대해서는 외부 VHL에 할당 제한 -> 추가 유입 차단
							if (destArea != targetArea) {
								StringBuilder message = new StringBuilder();
								message.append(" Vehicle:").append(vehicle.getVehicleId());
								message.append(" TrCmd:").append(trCmd.getTrCmdId());
								message.append(" StopArea:").append(stopAreaId);
								message.append(" DestArea:").append(destAreaId);
								message.append(" DestCurrentV:").append(destArea.getCurrentVehicleCount());
								message.append(" DestExpectedV:").append(destArea.getExpectedVehicleCount());
								message.append(" DestMaxV:").append(destArea.getMaxVehicleCount());
								traceJobAssignMain(message.toString());
								return false;
							}
						}
					}
				}
			}
			return true;
		} catch (Exception e) {
			traceJobAssignException("", e);
			return false;
		}
	}
	
	private boolean checkAreaBalancing(Vehicle vehicle, Park park) {
		assert vehicle != null;
		assert park != null;
		
		try {
			if (vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0) {
				return true;
			}
			if (locateVehicleList.contains(vehicle)) {
				return true;
			}
			
			if (areaList.size() == 0) {
				// AREA 테이블이 등록이 안되어 있는 경우.
				return true;
			}
			Node stopNode = nodeManager.getNode(vehicle.getStopNode());
			Node parkNode = park.getNode();
			
			if (stopNode == null || parkNode == null) {
				StringBuilder message = new StringBuilder();
				message.append(" StopNode:").append(vehicle.getStopNode());
				message.append(" ParkNode:").append(park.getName());
				traceJobAssignException(message.toString());
				return false;
			}
			
			Area stopArea = stopNode.getArea();
			Area parkArea = parkNode.getArea();
			if (stopArea != parkArea) {
				if (stopArea != null) {
					if (!(stopArea.isMinVehicleParkAllowed())){
						if (stopArea.getStayingIdleVehicleCount() <= stopArea.getTargetIdleVehicleCount() ||
								stopArea.getStayingVehicleCount() <= stopArea.getMinVehicleCount()) {
							return false;
						}
					}
				}
				if (parkArea != null) {
					if (parkArea.getCurrentVehicleCount() >= parkArea.getMaxVehicleCount() ||
							parkArea.getExpectedVehicleCount() >= parkArea.getMaxVehicleCount()) {
						return false;
					}
				}
			}
			return true;
		} catch (Exception e) {
			traceJobAssignException("", e);
			return false;
		}
	}
	
	private void manageParkCapacity(Park park) {
		assert park != null;
		
		int parkCapacity = park.getCapacity();
		int registeredParkNodeCount = 1;
		
		Node lastRegisteredParkNode = park.getNode();
		Section section = lastRegisteredParkNode.getSection(0);
		int pos = section.getNodeIndex(lastRegisteredParkNode);
		Node prevNode = null;
		
		if (targetParkList.contains(park) == false) {
			targetParkList.add(park);
		}
		
		if (lastRegisteredParkNode != null) {
			if (parkCapacity > 1) {
				while (pos > 0 &&
						registeredParkNodeCount < parkCapacity) {
					prevNode = section.getNode(--pos);
					if (prevNode != null) {
						if (prevNode.isVirtual() == false &&
								collisionManager.checkCollsion(lastRegisteredParkNode.getNodeId(), prevNode.getNodeId()) == false) {
							Park newPark = new Park(prevNode.getNodeId(), prevNode, park.getType(), park.getVehicleZone(), park.getVehicleZoneIndex(), 1, park.getRank(), true);
							lastRegisteredParkNode = prevNode;
							parkList.put(newPark.getName(), newPark);
							if (targetParkList.contains(newPark) == false) {
								targetParkList.add(newPark);
							}
							registeredParkNodeCount++;
						}
					}
				}
			}
		}
	}
	
	private String makeCountString(int count) {
		try {
			return String.format(FORMAT03D, count);
		} catch (Exception e) {
			traceJobAssignException("makeCountString()", e);
			return "000";
		}
	}
	
	private String makeNodeString(String node) {
		try {
			return String.format(FORMAT06D, Integer.parseInt(node));
		} catch (Exception e) {
			traceJobAssignException("makeNodeString() - Exception: A node (ID: " + node + ") is not number.", e);
			return node;
		}
	}
	
	private String makeStringWithBlanks(String value, int maxLength, boolean isLeftAlign) {
		try {
			if (value != null) {
				int required = maxLength - value.length();
				if (required > 0) {
					StringBuilder modified = new StringBuilder();
					if (isLeftAlign) {
						modified.append(value);
					}
					for (int i = 0; i < required; i++) {
						modified.append(BLANK);
					}
					if (isLeftAlign == false) {
						modified.append(value);
					}
					return modified.toString();
				}
			}
		} catch (Exception e) {
			traceJobAssignException("Value:" + value + ", MaxLength:" + maxLength, e);
		}
		return value;
	}
	
	private static Logger jobAssignMainLog = Logger.getLogger(JOBASSIGN_MAIN);
	public void traceJobAssignMain(String message) {
		jobAssignMainLog.debug(message);
	}
	
	private static Logger jobAssignDelayLog = Logger.getLogger(JOBASSIGN_DELAY);
	public void traceJobAssignDelay(String message) {
		jobAssignDelayLog.debug(message);
	}
	
	private static Logger jobAssignExceptionLog = Logger.getLogger(JOBASSIGN_EXCEPTION);
	public void traceJobAssignException(String message) {
		jobAssignExceptionLog.error(message);
	}
	public void traceJobAssignException(String message, Throwable e) {
		jobAssignExceptionLog.error(message, e);
	}
	
	public void initializeForRuntimeUpdate() {
		costSearch.setNearByDrive(ocsInfoManager.isNearByDrive());
	}
	
	/**
	 * Register Assigned Vehicle to a TrCmd
	 * 
	 * @param vehicle
	 * @param trCmd
	 */
	private void registerAssignedVehicleToTrCmd(Vehicle vehicle, TrCmd trCmd) {
		assert vehicle != null;
		assert trCmd != null;
		
		trCmd.setAssignedVehicleId(vehicle.getVehicleId());
		trCmdManager.addTrCmdToAssignedVehicleUpdateList(trCmd);
		
		// 2011.10.21 by PMM
		// NOT_ASSIGNED TrCmd에 대한 CANCEL 처리 변경.
		// OCS v2.0: IBSEM에서 삭제
		// OCS v3.0: 	IBSEM에서는 CANCEL 가능한 TrCmd의 CHANGEDREMOTECMD에 CANCEL 업데이트
		//				JobAssign에서는 CHANGEDREMOTECMD가 CANCEL인 TrCmd에는 AssignedVehicle에 VHL을 UPDATE하지 않음.
		//				Operation에서 CANCEL 처리. 
	}

	/**
	 * 2014.02.26 by MYM : [Stage Locate 기능]
	 * 
	 * @param vehicle
	 * @return
	 */
	private TrCmd getReservedStageTrCmd(Vehicle vehicle) {
		if (vehicle.getRequestedType() == REQUESTEDTYPE.STAGEWAIT
				|| vehicle.getRequestedType() == REQUESTEDTYPE.STAGENOBLOCK) {
			if (vehicle.getCurrNode().equals(vehicle.getTargetNode())
					&& vehicle.getStopNode().equals(vehicle.getTargetNode())) {
				return reservedStageVehicleMap.get(vehicle);
			}
		}
		return null;
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param eventHistory
	 * @param duplicateCheck
	 */
	public void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
		eventHistoryManager.addEventHistoryToRegisterList(eventHistory, duplicateCheck);
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param remoteCmd
	 * @param trCmd
	 */
	public void registerTrCompletionHistory(String remoteCmd, TrCmd trCmd) {
		if (trCmd != null) {
			TrCompletionHistory trCompletionHistory = new TrCompletionHistory(trCmd, remoteCmd, "");
			trCompletionHistoryManager.addTrCmdToRegisterTrCompletionHistory(trCompletionHistory);
		}
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param vehicle
	 * @param trCmd
	 */
	private void cancelReservedStageInfo(Vehicle vehicle, TrCmd trCmd) {
		if (stageLocateVehicleList.contains(vehicle)) {
			StringBuilder log = new StringBuilder();
			log.append("     [CancelS1] ").append(vehicle.getVehicleId());
			log.append(" StageLocate TargetNode=").append(vehicle.getTargetNode());
			if (vehicleManager.updateVehicleRequestedInfoToDB(STAGECANCEL, vehicle.getTargetNode(), vehicle.getVehicleId())) {
				stageLocateVehicleList.remove(vehicle);
				reservedStageVehicleMap.remove(vehicle);
			} else {
				log.append(" Failed.");
			}
			traceJobAssignMain(log.toString());
		}
		
		String anOtherVehicles = cancelStageReservationFromAnotherVehicle(vehicle, trCmd);
		if (anOtherVehicles.length() > 0) {
			StringBuilder log = new StringBuilder();
			log.append("     [CancelS2] ").append(anOtherVehicles).append("(P)");
			log.append(" StageLocate TargetNode=").append(vehicle.getTargetNode());
			log.append("/TrCmd=").append(trCmd.getTrCmdId());
			traceJobAssignMain(log.toString());
		}
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param vehicle
	 * @param trCmd
	 */
	private String cancelStageReservationFromAnotherVehicle(Vehicle vehicle, TrCmd trCmd) {
		StringBuilder log = new StringBuilder(); 
		if (vehicle != null && trCmd != null) {
			TrCmd reservedTrCmd = null;
			Set<Vehicle> searchKeys = new HashSet<Vehicle>(reservedStageVehicleMap.keySet());
			for (Vehicle searchKey : searchKeys) {
				reservedTrCmd = reservedStageVehicleMap.get(searchKey);
				if (trCmd == reservedTrCmd && vehicle != searchKey) {
					vehicleManager.updateVehicleRequestedInfoToDB(STAGECANCEL, "", searchKey.getVehicleId());
					reservedStageVehicleMap.remove(searchKey);
					if (log.length() > 0) {
						log.append(",");
					}
					log.append(searchKey);
				}
			}
		}
		return log.toString();
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 */
	private void searchStageTargetNode() {
		try {
			stageTargetNodeMap.clear();
			Iterator<String> iter = unavailableStageTargetMap.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				StageTarget item = unavailableStageTargetMap.get(key);
				if (item != null && item.isAvailable() == false) {
					if (unassignedTrCmdList.contains(item.getTrCmd()) == false) {
						iter.remove();
					}
				}
			}
			
			if (isStageLocateUsage) {
				// 2014.05.30 by KYK : check abnormalNodeIdList  
				HashSet<String> abnormalNodeIdList = nodeManager.getAbnormalNodeIdList();
				HashMap<String, StageTarget> tmpUnavailableStageTargetMap = new HashMap<String, StageTarget>();
				for (String sourceNode : sortedStageAtSamePortMap.keySet()) {
					ArrayList<TrCmd> stageList = sortedStageAtSamePortMap.get(sourceNode);
					Node targetNode = this.nodeManager.getNode(sourceNode);
//					if (targetNode == null || targetNode.isEnabled() == false) {
					if (targetNode == null || targetNode.isEnabled() == false
							|| (abnormalNodeIdList != null && abnormalNodeIdList.contains(targetNode.getNodeId()))) {
//						return;
						continue;
					}
					ArrayList<Node> stageTargetNodeList = new ArrayList<Node>(); 
					ArrayList<Node> queueNodes = new ArrayList<Node>();
					stageTargetNodeList.add(targetNode);
					queueNodes.add(targetNode);
					Node queueNode = null;

					if (stageList == null || stageList.isEmpty()) {
						continue;
					}
					int stageCount = stageList.size();
					if (stageCount > stageLocateVehicleCount) {
						stageCount = stageLocateVehicleCount;
					}

					while (stageTargetNodeList.size() < stageCount && queueNodes.size() > 0) {
						queueNode = queueNodes.remove(0);
						if (queueNode == null) {
							break;
						}
						ArrayList<Section> sectionList = queueNode.getSection();
						for (Section section : sectionList) {
							if (section == null) {
								continue;
							}
							int pos = section.getNodeIndex(queueNode) - 1;
							if (pos < 0) {
								continue;
							}
							Node node = null;
							for (int i = pos; i >= 0; i--) {
								node = section.getNode(i);
//								if (node == null || node.isEnabled() == false) {
								if (node == null || node.isEnabled() == false
										|| (abnormalNodeIdList != null && abnormalNodeIdList.contains(node.getNodeId()))) {
									node = null;
									break;
								}
								if (node.isVirtual()) {
									continue;
								}
								StageTarget item = unavailableStageTargetMap.get(node.getNodeId());
								if (item != null && item.isAvailable() == false) {
									node = null;
									break;
								}
								boolean isCollision = false;
								for (Node chkNode : stageTargetNodeList) {
									if (collisionManager.checkCollsion(chkNode.getNodeId(), node.getNodeId())) {
										isCollision = true;
									}
								}
								if (isCollision == false) {
									stageTargetNodeList.add(node);
									if (stageTargetNodeList.size() >= stageCount) {
										break;
									}
								}
							}
							if (stageTargetNodeList.size() >= stageCount) {
								break;
							}
							if (node != null) {
								queueNodes.add(node);
							}
						}
					}

					for (int i = 0; i < stageCount; i++) {
						TrCmd trCmd = stageList.get(i);
						if (i < stageTargetNodeList.size()) {
							Node node = stageTargetNodeList.get(i);
							if (node != null) {
								stageTargetNodeMap.put(trCmd, node);
								tmpUnavailableStageTargetMap.put(node.getNodeId(), new StageTarget(node.getNodeId(), false, trCmd));
							}
						}
					}
				}
				unavailableStageTargetMap.putAll(tmpUnavailableStageTargetMap);
				tmpUnavailableStageTargetMap.clear();
			}
		} catch (Exception e) {
			traceJobAssignException("searchStageTargetNode()", e);
		}
	}
	
	/**
	 * 2014.02.18 by MYM : [Stage Locate 기능]
	 * 
	 * @param vehicle
	 * @param trCmd
	 * @param modelingCost
	 * @param distanceBasedCost
	 */
	private void reserveVehicleToStage(Vehicle vehicle, TrCmd trCmd, double modelingCost, double distanceBasedCost) {
		assert isStageLocateUsage && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE;
		
		if (vehicle != null && trCmd != null) {
			String targetNodeId = trCmd.getSourceNode();
			Node targetNode = stageTargetNodeMap.get(trCmd);
			if (targetNode != null) {
				targetNodeId = targetNode.getNodeId();				
			}
			StringBuilder stageLog = new StringBuilder(); 
			if (targetNodeId.equals(vehicle.getTargetNode()) == false
					|| vehicle.getRequestedType() == REQUESTEDTYPE.NULL) {
				// TargetNode까지 주행 명령 요청
				vehicleManager.updateVehicleRequestedInfoToDB(STAGE, targetNodeId, vehicle.getVehicleId());
				stageLog.append("ReqStageLocate");
			} else {
				trCmd.setRemainingDuration(getRemainingDuration(trCmd));
				long waitingTime = getWaitingTime(trCmd.getTrQueuedTime());
				if (vehicle.getCurrNode().equals(vehicle.getTargetNode()) &&
						vehicle.getStopNode().equals(vehicle.getTargetNode()) &&
						vehicle.getTargetNode().equals(targetNodeId)) {
					// TargetNode에 도착하면 NoBlocking, Waiting, Cancel(TimeOver) 확인
					if (trCmd.getStageInitTime() == 0) {
						trCmd.setStageInitTime(waitingTime);
					}
					if (trCmd.getSourceNode().equals(vehicle.getCurrNode()) == false) {
						// Stage Source가 아닌 TargetNode에서 도착 대기시에는 양보 가능
						if (waitingTime > (trCmd.getExpectedDuration() + trCmd.getWaitTimeout())) {
							vehicleManager.updateVehicleRequestedInfoToDB(STAGECANCEL, "", vehicle.getVehicleId());
							unassignedStageList.remove(trCmd);
							reservedStageVehicleMap.remove(vehicle);
							cancelStageCommand(EVENTHISTORY_REASON.WAITTIMEOUT_TIMEOVER, trCmd, vehicle);
							return;
						} else {
							long remainingWaitTime = (trCmd.getExpectedDuration() + trCmd.getWaitTimeout()) - waitingTime;
							vehicleManager.updateVehicleRequestedInfoToDB(STAGENOBLOCK, Long.toString(remainingWaitTime), vehicle.getVehicleId());
							stageLog.append("NoBlocking");							
						}
					} else if (waitingTime <= trCmd.getNoBlockingTime()) {
						// NoBlocking 확인
						long remainingNoBlockingTime = trCmd.getNoBlockingTime() - waitingTime;
						vehicleManager.updateVehicleRequestedInfoToDB(STAGENOBLOCK, Long.toString(remainingNoBlockingTime), vehicle.getVehicleId());
						stageLog.append("NoBlocking:").append(remainingNoBlockingTime);
					} else if (waitingTime <= (trCmd.getExpectedDuration() + trCmd.getWaitTimeout())) {
						// Waiting 확인
						long remainingWaitTime = (trCmd.getExpectedDuration() + trCmd.getWaitTimeout()) - waitingTime;
						vehicleManager.updateVehicleRequestedInfoToDB(STAGEWAIT, Long.toString(remainingWaitTime), vehicle.getVehicleId());
						stageLog.append("Waiting:").append(remainingWaitTime);
						// 2022.05.05 by JJW : STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
						if(isStageSourceDupCancelUsage){
							if(checkDupSourceLoc(trCmd.getSourceLoc())){
								vehicleManager.updateVehicleRequestedInfoToDB(STAGECANCEL, "", vehicle.getVehicleId());
								unassignedStageList.remove(trCmd);
								reservedStageVehicleMap.remove(vehicle);
								cancelStageCommand(EVENTHISTORY_REASON.SOURCE_DUPLICATE_BY_STAGE, trCmd, vehicle);
								return;
							}
						}
					} else if (waitingTime > (trCmd.getExpectedDuration() + trCmd.getWaitTimeout())) {
						// Cancel(TimeOver) 확인
						vehicleManager.updateVehicleRequestedInfoToDB(STAGECANCEL, "", vehicle.getVehicleId());
						unassignedStageList.remove(trCmd);
						reservedStageVehicleMap.remove(vehicle);
						cancelStageCommand(EVENTHISTORY_REASON.WAITTIMEOUT_TIMEOVER, trCmd, vehicle);
						return;
					} else {
						stageLog.append("Abnormal");
					}
				} else if (waitingTime > (trCmd.getExpectedDuration() + trCmd.getWaitTimeout())) {
					// TargetNode로 이동중 - RemainingTime 확인 후 Cancel
					vehicleManager.updateVehicleRequestedInfoToDB(STAGECANCEL, "", vehicle.getVehicleId());
					unassignedStageList.remove(trCmd);
					reservedStageVehicleMap.remove(vehicle);
					cancelStageCommand(EVENTHISTORY_REASON.EXPECTEDDURATION_TIMEOVER, trCmd, vehicle);
					return;
				} else {
					stageLog.append("Going");
				}
				stageLog.append("(EDT:").append(trCmd.getExpectedDuration());
				stageLog.append(",NBT:").append(trCmd.getNoBlockingTime());
				stageLog.append(",WTO:").append(trCmd.getWaitTimeout());
				stageLog.append(",INT:").append(trCmd.getStageInitTime());
				stageLog.append(",RDT:").append(trCmd.getRemainingDuration() + trCmd.getWaitTimeout()).append(")");
			}
			String anOtherVehicles = cancelStageReservationFromAnotherVehicle(vehicle, trCmd);
			if (anOtherVehicles.length() > 0) {
				stageLog.append("/PrevVHL=").append(anOtherVehicles);
			}
			reservedStageVehicleMap.put(vehicle, trCmd);
			updateResultToList(vehicle, trCmd);
			newStageLocateVehicleList.add(vehicle);

			if (isAreaBalancingUsed) {
				updateAreaVehicleCount(vehicle, targetNodeId);
			}

			StringBuilder log = new StringBuilder();
			log.append("     [Reserved] ").append(vehicle.getVehicleId());
			log.append(" S/TrCMDID=").append(trCmd.getTrCmdId());
			log.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
			log.append("/Distance-based Cost=").append(String.format(FORMAT, distanceBasedCost));
			log.append("/SourceNode=").append(trCmd.getSourceNode());
			log.append("/StopNode=").append(vehicle.getStopNode());
			log.append("/StageTargetNode=").append(targetNodeId);
			log.append("/VehicleTargetNode=").append(vehicle.getTargetNode());
			log.append("/").append(stageLog.toString());
			traceJobAssignMain(log.toString());
		}
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 */
	public void deleteStageCmdFromDB() {
		this.trCmdManager.deleteStageCmdFromDB();
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param trCmd
	 */
	public void deleteStageCmdFromDB(TrCmd trCmd) {
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
			this.trCmdManager.deleteStageCmdFromDB(trCmd.getTrCmdId());
			this.trCmdManager.deleteStageCmdFromDB();
		}
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param reason
	 * @param trCmd
	 * @param vehicle
	 */
	private void cancelStageCommand(EVENTHISTORY_REASON reason, TrCmd trCmd, Vehicle vehicle) {
		// VHL:OHT201(AA), CMDID:234423, CARRIERID:OYB0123, SRCLOC:EFB01_1233, DESTLOC:EFB03_2233
		StringBuilder message = new StringBuilder();
		if (vehicle == null) {
			message.append("Vehicle:NotAssigned");
		} else {
			message.append("Vehicle:").append(vehicle.getVehicleId());
		}
		message.append(", TrCmdId:").append(trCmd.getTrCmdId());
		message.append(", CarrierId:").append(trCmd.getCarrierId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		message.append(", QueuedTime:").append(trCmd.getTrQueuedTime());
		message.append(", EDT:").append(trCmd.getExpectedDuration());
		message.append(", NBT:").append(trCmd.getNoBlockingTime());
		message.append(", WTO:").append(trCmd.getWaitTimeout());
		registerEventHistory(new EventHistory(
				EVENTHISTORY_NAME.CURRENT_STAGE_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "",
				message.toString(), "", "", EVENTHISTORY_REMOTEID.JOBASSIGN, "",
				reason), false);

		if (trCmd.getDeletedTime() != null && trCmd.getDeletedTime().length() < 2) {
			trCmd.setDeletedTime(ocsInfoManager.getCurrDBTimeStr());
		}
		registerTrCompletionHistory(REQUESTEDTYPE.STAGECANCEL.toConstString(), trCmd);
		deleteStageCmdFromDB(trCmd);
		
		StringBuilder log = new StringBuilder();
		log.append("     [CancelS3] StageLocate ");
		log.append(reason).append(" ").append(message);
		traceJobAssignMain(log.toString());
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param time
	 * @return
	 */
	private long getTimeFromString(String time) {
		try {
			return sdf.parse(time).getTime();
		} catch (Exception e) {
			traceJobAssignException("getTimeFromString()", e);
			return System.currentTimeMillis();
		}
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param time
	 * @return
	 */
	private long getWaitingTime(String time) {
		try {
			return (long) ((getTimeFromString(ocsInfoManager.getCurrDBTimeStr()) - getTimeFromString(time)) / 1000);
		} catch (Exception e) {
			traceJobAssignException("getTimeFromString()", e);
			return System.currentTimeMillis();
		}
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 * 
	 * @param trCmd
	 * @return
	 */
	private long getRemainingDuration(TrCmd trCmd) {
		assert trCmd != null;
		
		try {
			if (trCmd == null) {
				return 0;
			} else {
				return trCmd.getExpectedDuration() - getWaitingTime(trCmd.getTrQueuedTime());
			}
		} catch (Exception e) {
			traceJobAssignException("getRemainingDuration()", e);
			return 0;
		}
	}
	
	/**
	 * 2014.02.27 by MYM : [Stage Locate 기능]
	 */
	private void checkStageExpectedDurationTimeOver() {
		for (TrCmd trCmd : unassignedStageList) {
			if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
				long waitingTime = getWaitingTime(trCmd.getTrQueuedTime());
//				if (getRemainingDuration(trCmd) <= 0) {
				if (waitingTime >= (trCmd.getExpectedDuration() + trCmd.getWaitTimeout())) {
					cancelStageCommand(EVENTHISTORY_REASON.EXPECTEDDURATION_TIMEOVER, trCmd, null);					
				}
			}
		}
		for (Vehicle vehicle : stageLocateVehicleList) {
			if (newStageLocateVehicleList.contains(vehicle) == false) {
				StringBuilder log = new StringBuilder();
				log.append("     [CancelS4] ").append(vehicle.getVehicleId());
				log.append(" StageLocate TargetNode=").append(vehicle.getTargetNode());
				if (vehicleManager.updateVehicleRequestedInfoToDB(STAGECANCEL, "", vehicle.getVehicleId())) {
					reservedStageVehicleMap.remove(vehicle);
				} else {
					log.append(" Failed.");
				}
				traceJobAssignMain(log.toString());
			}
		}
		
//		System.out.println("                     [RESERVED INFO] ReservedVHL:" + reservedStageVehicleMap.size() + ", StageLocateVHL:" + stageLocateVehicleList.size() + ", unavailableStageTarget:" + unavailableStageTargetMap.size());
	}

	
	/**
	 * Register Alarm with Level
	 * 
	 * @param type
	 * @param alarmText
	 * @param alarmLevel
	 */
	public void registerAlarmWithLevel(String type, String alarmText, ALARMLEVEL alarmLevel) {
		if (alarmManager != null) {
			if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}
			alarmManager.registerAlarmTextWithLevel(type, alarmText, alarmLevel.toConstString());
		}
	}
	
	/**
	 * 2013.05.16 by KYK
	 * @param portId
	 * @return
	 */
	private String getStationIdAtPort(String portId) {
		String stationId = null;
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(portId);
		if (carrierLoc != null) {
			stationId = carrierLoc.getStationId();
		}
		return stationId;
	}
}

/**
 * VehicleComparator Class, OCS 3.0 for Unified FAB
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
class VehicleComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		Vehicle v1 = (Vehicle)o1;
		Vehicle v2 = (Vehicle)o2;
		return v1.getVehicleId().compareTo(v2.getVehicleId());
	}
}

/**
 * TrCmdComparator Class, OCS 3.0 for Unified FAB
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
class TrCmdComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		TrCmd t1 = (TrCmd)o1;
		TrCmd t2 = (TrCmd)o2;
		return t1.getTrCmdId().compareTo(t2.getTrCmdId());
	}
}

/**
 * JobAssignResultComparator Class, OCS 3.0 for Unified FAB
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
class JobAssignResultComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		JobAssignResult t1 = (JobAssignResult)o1;
		JobAssignResult t2 = (JobAssignResult)o2;
		if (t1.getModelingCost() < t2.getModelingCost()) {
			return -1;
		} else if (t1.getModelingCost() == t2.getModelingCost()) {
			if (t1.getDistanceBasedCost() < t2.getDistanceBasedCost()) {
				return -1;
			} else if (t1.getDistanceBasedCost() == t2.getDistanceBasedCost()) {
				return t1.getVehicle().getVehicleId().compareTo(t2.getVehicle().getVehicleId());
			} else  {
				return 1;
			}
		} else  {
			return 1;
		}
	}
}

/**
 * ReservedAsNextTrCmdComparator Class, OCS 3.0 for Unified FAB
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
class ReservedAsNextTrCmdComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		ReservedAsNextTrCmd t1 = (ReservedAsNextTrCmd)o1;
		ReservedAsNextTrCmd t2 = (ReservedAsNextTrCmd)o2;
		if (t1.getWaitingTime() < t2.getWaitingTime()) {
			return -1;
		} else if (t1.getWaitingTime() == t2.getWaitingTime()) {
			if (t1.getReservingCost() > t2.getReservingCost()) {
				return -1;
			} else if (t1.getReservingCost() == t2.getReservingCost()) {
				return 0;
			} else  {
				return 1;
			}
		} else  {
			return 1;
		}
	}
}

/**
 * BackupVehicleComparator Class, OCS 3.0 for Unified FAB
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
class BackupVehicleComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		BackupVehicle t1 = (BackupVehicle)o1;
		BackupVehicle t2 = (BackupVehicle)o2;
		if (t1.getDistanceCost() < t2.getDistanceCost()) {
			return -1;
		} else if (t1.getDistanceCost() == t2.getDistanceCost()) {
			return 0;
		} else  {
			return 1;
		}
	}
}

class PriorJobComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		PriorJob j1 = (PriorJob) o1;
		PriorJob j2 = (PriorJob) o2;		
		
		if (j2.getCost() - j1.getCost() > 0) {
			return 1;
		} else {
			return -1;
		}
	}	
}

