package com.samsung.ocs.optimizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.SEARCH_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.COSTSEARCH_OPTION;
import com.samsung.ocs.common.constant.OcsInfoConstant.DISPATCHING_RULES;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.AreaManager;
import com.samsung.ocs.manager.impl.CollisionManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.ParkManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.ZoneControlManager;
import com.samsung.ocs.manager.impl.model.Area;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.LocalGroupInfo;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Park;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.optimizer.model.Candidate;
import com.samsung.ocs.optimizer.model.EscapeResult;
import com.samsung.ocs.optimizer.model.OptimizerResult;
import com.samsung.ocs.optimizer.model.Target;
import com.samsung.ocs.route.search.CostSearch;

/**
 * Optimizer Class, OCS 3.0 for Unified FAB
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

public class Optimizer {
	private NodeManager nodeManager = null;
//	private SectionManager sectionManager = null;
	private CollisionManager collisionManager = null;
	private OCSInfoManager ocsInfoManager = null;
	private TrCmdManager trCmdManager = null;
	private VehicleManager vehicleManager = null;
	private AreaManager areaManager = null;
	private ZoneControlManager zoneControlManager = null;
	private AlarmManager alarmManager = null;
	private ParkManager parkManager = null;
	
	private HashMap<String, Object> vehicleList;						// Normal Auto Enabled Vehicles
	private HashMap<String, Object> enabledVehicleList;					// All Enabled Vehicles
	private HashMap<String, Object> locateNodeIdMap;
	
	private ConcurrentHashMap<String, Object> trCmdList;				// 전체 TrCmd
	private ConcurrentHashMap<String, Object> areaList;					// 전체 Area
	private ConcurrentHashMap<String, Object> parkList;					// 전체 Park
	
	private TreeSet<TrCmd> localTrCmdList;								// LocalGroup TrCmd
	private HashMap<TrCmd, String> localTrCmdGroupIdMap;				// LocalGroupId of LocalTrCmd
	
	private TreeSet<TrCmd> unassignedTrCmdList;							// 미할당된 TrCmd (AssignedVehicle == '')
	private TreeSet<TrCmd> assignedTrCmdList;							// 할당된 TrCmd (AssignedVehicle is not null)
	private TreeSet<Vehicle> idleVehicleList;							// 작업을 수행하지 않고 있는 Idle Vehicle
	private TreeSet<Vehicle> loadVehicleList;							// Unloaded 작업부터 Loading 구간내 작업을 수행중인 Vehicle
	private TreeSet<Vehicle> localVehicleList;							// 가변형 LocalOHT
	private TreeSet<Vehicle> idleLocalVehicleList;						// Idle인 가변형 LocalOHT
	private ArrayList<Vehicle> locateVehicleList;							// Idle이지만 지정 노드로 LOCATE 요청받아 이동 중인 Vehicle (CostSearch 시 Curr-Stop-TargetNode까지 계산해야 함.) 
	private ArrayList<LocalGroupInfo> targetLocalGroupList;
	private ArrayList<TrCmd> targetTrCmdList;
	private ArrayList<Park> targetParkList;
	private ArrayList<TrCmd> sourceDupTrCmdList;
	
	private ArrayList<String> assignedVehicleIdList;					// 작업을 할당받은 Vehicle
	private ArrayList<String> reservedVehicleIdList;					// Next 작업을 할당받은 Vehicle
	private ArrayList<String> localGroupIdList;
	private ArrayList<String> releaseLocalGroupIdList;
	private ArrayList<String> parkNodeIdList;
	
	private ArrayList<Vehicle> resultVehicleList;
	private ArrayList<Object> resultTargetList;
	
	private ArrayList<LocalGroupInfo> setLocalGroupInfoList;
	private ArrayList<String> idleVehicleLocalGroupIdList;
	private ArrayList<String> vehicleZoneList;
	
	private ArrayList<String> destLocOfUnloadedVehicleList;
	private HashMap<String, TrCmd> unassignedTrCmdMapBySourceLoc;
	private HashMap<String, Double> costMap;
	
	private HashMap<String, Integer> currentEnabledVehicleCounts;
	private HashMap<String, Integer> currentAssignableVehicleCounts;
	private HashMap<String, Integer> expectedEnabledVehicleCounts;
	private HashMap<String, Double> expectedEnabledVehicleWeights;
	
//	private static final String NODE = "NODE";
	private static final String MOVE = "MOVE";
//	private static final String LOCATE = "LOCATE";
//	private static final String LOCATECANCEL = "LOCATECANCEL";
	private static final String FORMAT = "%04.2f";
	
	private static final double MAXCOST_TIMEBASE = 9999.0;
//	private static final int MAX_SEARCH_LIMIT_CHECK_COUNT = 5;
	
	private CostSearch costSearch;
	
	private boolean isAreaBalancingUsed = false;
	private boolean isHidControlUsed = false;
	private int maxLocalGroupIdLength = 3;
	
	private int moveRequestedVehicleCount = 0;
	private int areaBalancingLimit = 1;

	private int intervalCheckCount = 0;
	private int areaBalancingInterval = 5;
	
//	private int priorJobCriteriaOfPriority = 90;
//	private int priorJobCriteriaOfWaitingTime = 450;
//	private int searchLimitCheckCount = 0;
	private int size;
	private int[] result;
	private double[][] cost;
	private double[][] costBackup;
	
//	private SimpleDateFormat sdf;
	
	private APSolver solver;
//	private COSTSEARCH_OPTION costSearchOption = COSTSEARCH_OPTION.NONE;
	protected DISPATCHING_RULES dispatchingRule = DISPATCHING_RULES.HYBRID;
	
	/**
	 * Constructor of Optimizer class.
	 */
	public Optimizer() {
		this.nodeManager = NodeManager.getInstance(null, null, false, false, 0);
//		this.sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		this.collisionManager = CollisionManager.getInstance(null, null, false, false, 0);
		this.ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		this.trCmdManager = TrCmdManager.getInstance(null, null, false, false, 0);
		this.vehicleManager = VehicleManager.getInstance(null, null, false, false, 0);
		this.areaManager = AreaManager.getInstance(null, null, false, false, 0);
		this.zoneControlManager = ZoneControlManager.getInstance(null, null, false, false, 0);
		this.alarmManager = AlarmManager.getInstance(null, null, false, false, 0);
		this.parkManager = ParkManager.getInstance(null, null, false, false, 0);
		
		vehicleList = new HashMap<String, Object>();
		enabledVehicleList = new HashMap<String, Object>();
		locateNodeIdMap = new HashMap<String, Object>();
		trCmdList = new ConcurrentHashMap<String, Object>();
		parkList = new ConcurrentHashMap<String, Object>();
		unassignedTrCmdMapBySourceLoc = new HashMap<String, TrCmd>();
		costMap = new HashMap<String, Double>();
		
		currentEnabledVehicleCounts = new HashMap<String, Integer>();
		currentAssignableVehicleCounts = new HashMap<String, Integer>();
		
		expectedEnabledVehicleCounts = new HashMap<String, Integer>();
		expectedEnabledVehicleWeights = new HashMap<String, Double>();
		
		localTrCmdGroupIdMap = new HashMap<TrCmd, String>();
		idleVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		loadVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		localVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		idleLocalVehicleList = new TreeSet<Vehicle>(new VehicleComparator<Vehicle>());
		sourceDupTrCmdList = new ArrayList<TrCmd>();
		
		locateVehicleList = new ArrayList<Vehicle>();
		targetTrCmdList = new ArrayList<TrCmd>();
		targetLocalGroupList = new ArrayList<LocalGroupInfo>();
		targetParkList = new ArrayList<Park>();
		destLocOfUnloadedVehicleList = new ArrayList<String>();
		idleVehicleLocalGroupIdList = new ArrayList<String>();
		
		assignedVehicleIdList = new ArrayList<String>();
		reservedVehicleIdList = new ArrayList<String>();
		
		vehicleZoneList = new ArrayList<String>();
		parkNodeIdList = new ArrayList<String>();
		
		resultVehicleList = new ArrayList<Vehicle>();
		resultTargetList = new ArrayList<Object>();
		
		assignedTrCmdList = new TreeSet<TrCmd>(new TrCmdComparator<TrCmd>());
		unassignedTrCmdList = new TreeSet<TrCmd>(new TrCmdComparator<TrCmd>());
		localTrCmdList = new TreeSet<TrCmd>(new TrCmdComparator<TrCmd>());
		localGroupIdList = new ArrayList<String>();
		
		releaseLocalGroupIdList = new ArrayList<String>();
		setLocalGroupInfoList = new ArrayList<LocalGroupInfo>();
		
//		sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		costSearch = new CostSearch();
		
		solver = new APSolver();
	}
	
	/**
	 * MainProcessing
	 */
	public void mainProcessing() {
		try {
			if (++intervalCheckCount >= areaBalancingInterval) {
				// Step 1 Update Operational Parameters for Optimizer
				updateOptimizerParameters();
				balanceIdleVehicle();
				
				intervalCheckCount = 0;
			}
		} catch (Exception e) {
			traceOptimizerException("mainProcessing()", e);
		}
	}
	
	/**
	 * Step 1 Update Operational Parameters for Optimizer
	 */
	private void updateOptimizerParameters() {
		// Step 1-1 Clear Lists
		initialize();
		
		// Step 1-2 Update Operational Parameters from OCSINFO
		updateOperationalParameters();
	}
	
	
	/**
	 * Step 
	 */
	private void balanceIdleVehicle() {
		if (isAreaBalancingUsed) {
			// Step 1-3 Update VEHICLE and TRCMD from DB
			updateVehicleAndTrCmdFromDB();
			
			// Step 1-4 Get Vehicles and TrCmds [IdleVehicle, LoadVehicle, LocalVehicle, IdleLocalVehicle, LocateVehicle / UnassignedTrCmd, AssignedTrCmd]
			getVehiclesAndTrCmds();
			
			// Step 1-6 Update Vehicle Counts per Area for AreaBalancing 
			updateAreaData();
			
			balanceIdleVehicleInZone();
		}
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
		reservedVehicleIdList.clear();
		
		// classifyVehicles()
		idleVehicleList.clear();
		localVehicleList.clear();
		localGroupIdList.clear();
		
		// classifyTrCmdsAndVehicles() {
		unassignedTrCmdList.clear();
		loadVehicleList.clear();
		locateVehicleList.clear();
		locateNodeIdMap.clear();
		idleLocalVehicleList.clear();
		idleVehicleLocalGroupIdList.clear();
		
		// refinePortDuplicatedTrCmds() {
		unassignedTrCmdMapBySourceLoc.clear();
		sourceDupTrCmdList.clear();
		
		// classifyLocalGroup() {
		releaseLocalGroupIdList.clear();
		setLocalGroupInfoList.clear();
		
		resultVehicleList.clear();
		resultTargetList.clear();
		
		targetLocalGroupList.clear();
		targetTrCmdList.clear();
		targetParkList.clear();
		destLocOfUnloadedVehicleList.clear();
		unassignedTrCmdList.clear();
		
		localTrCmdList.clear();
		localTrCmdGroupIdMap.clear();
		
		moveRequestedVehicleCount = 0;
	}
	
	/**
	 * Step 1-2 Update Operational Parameters from OCSINFO
	 */
	private void updateOperationalParameters() {
		isHidControlUsed = ocsInfoManager.isHidControlUsed();
		dispatchingRule = ocsInfoManager.getDispatchingRule();
		
		isAreaBalancingUsed = ocsInfoManager.isAreaBalancingUsed();
		areaBalancingLimit = ocsInfoManager.getAreaBalancingLimit();
		areaBalancingInterval = ocsInfoManager.getAreaBalancingInterval();
	}
	
	/**
	 * Step 1-3 Update VEHICLE and TRCMD from DB
	 */
	private void updateVehicleAndTrCmdFromDB() {
		vehicleZoneList.clear();
		
		// Step 1-3-1 Update VEHICLE from DB
		vehicleManager.updateFromDBForJobAssign();
		vehicleZoneList = vehicleManager.getVehicleZoneList();
		
		// Step 1-3-2 Update TRCMD from DB
		trCmdManager.updateFromDBForJobAssign();
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
		
		getParkNodes();
	}
	
	
	/**
	 * Step 1-6 Update AreaInfo for AreaBalancing
	 * 
	 */
	private void updateAreaData(){
		currentEnabledVehicleCounts.clear();
		currentAssignableVehicleCounts.clear();
		expectedEnabledVehicleWeights.clear();
		expectedEnabledVehicleCounts.clear();
		
		areaList = areaManager.getData();
		if (areaList.size() > 0 && isAreaBalancingUsed) {
			areaManager.initializeAreaData();
			Vehicle vehicle = null;
			Node stopNode = null;
			Node targetNode = null;
			String stopNodeAreaId = "";
			String targetNodeAreaId = "";
			Area stopNodeArea = null;
			Area targetNodeArea = null;
			Set<String> searchKeys = new HashSet<String>(enabledVehicleList.keySet());
			for (String searchKey : searchKeys) {
				vehicle = (Vehicle)enabledVehicleList.get(searchKey);
				if (vehicle != null) {
					try {
						stopNode = (Node)nodeManager.getNode(vehicle.getStopNode());
						if (stopNode != null) {
							stopNodeAreaId = stopNode.getAreaId();
							stopNodeArea = stopNode.getArea();
						} else {
							stopNodeAreaId = "";
							stopNodeArea = null;
						}
						
						targetNode = (Node)nodeManager.getNode(vehicle.getTargetNode());
						if (targetNode != null) {
							targetNodeAreaId = targetNode.getAreaId();
							targetNodeArea = targetNode.getArea();
						} else {
							targetNodeAreaId = "";
							targetNodeArea = null;
						}
						
						if (stopNodeArea != null) {
							if (stopNodeAreaId.equals(targetNodeAreaId)) {
								stopNodeArea.addStayingVehicle(vehicle);
								if (idleVehicleList.contains(vehicle) &&
										localVehicleList.contains(vehicle) == false) {
									stopNodeArea.addStayingIdleVehicle(vehicle);
								}
							} else {
								stopNodeArea.addOutGoingVehicle(vehicle);
								if (idleVehicleList.contains(vehicle) &&
										localVehicleList.contains(vehicle) == false) {
									stopNodeArea.addOutGoingIdleVehicle(vehicle);
								}
							}
						}
						if (targetNodeArea != null) {
							if (stopNodeAreaId.equals(targetNodeAreaId) == false) {
								targetNodeArea.addInComingVehicle(vehicle);
								if (idleVehicleList.contains(vehicle) &&
										localVehicleList.contains(vehicle) == false) {
									targetNodeArea.addInComingIdleVehicle(vehicle);
								}
							}
							if (loadVehicleList.contains(vehicle)) {
								targetNodeArea.addLoadVehicle(vehicle);
							}
						}
					} catch (Exception e) {
						traceOptimizerException("StopNode:" + vehicle.getStopNode() + ", TargetNode:" + vehicle.getTargetNode(), e);
					}
				}
			}
			
			TrCmd trCmd = null;
			String sourceNodeId = "";
			Node sourceNode = null;
			Area sourceArea = null;
			int reservedTrCmdCount = 0;
			Iterator<TrCmd> it = unassignedTrCmdList.iterator();
			while (it.hasNext()) {
				trCmd = it.next();
				if (trCmd != null) {
					sourceNodeId = trCmd.getSourceNode();
					if (sourceNodeId != null && sourceNodeId.length() > 0) {
						if (locateNodeIdMap.containsKey(sourceNodeId)) {
							reservedTrCmdCount++;
						} else {
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
			}
			
			traceOptimizerMain("ReservedTrCmdCount:" + reservedTrCmdCount);
		}
	}
	
	private void balanceIdleVehicleInZone() {
		resultVehicleList.clear();
		resultTargetList.clear();
		
		traceOptimizerMain("Local Vehicle:" + localVehicleList.toString());
		traceOptimizerMain("Locate Vehicle:" + locateVehicleList.toString());
		
		Area area = null;
		ArrayList<String> assignableNodeZoneList = null;
		ArrayList<String> areaIdList = null;
		for (String vehicleZone : vehicleZoneList) {
			if (vehicleZone != null) {
				assignableNodeZoneList = zoneControlManager.getAssignableNodeZoneList(vehicleZone);
				if (assignableNodeZoneList != null) {
					ArrayList<Area> targetAreaList = new ArrayList<Area>();
					for (String nodeZone : assignableNodeZoneList) {
						areaIdList = nodeManager.getAreaList(nodeZone);
						for (String areaId : areaIdList) {
							if (areaId != null && areaId.length() > 0) {
								area = (Area)areaList.get(areaId);
								if (area != null) {
									if (targetAreaList.contains(area) == false) {
										targetAreaList.add(area);
									}
								}
							} else {
								StringBuilder message = new StringBuilder();
								message.append("[Abnormal] VehicleZone:").append(vehicleZone);
								message.append(" - Area:").append(areaId);
								traceOptimizerMain(message.toString());
							}
						}
					}
					if (targetAreaList.size() > 1) {
						balanceIdleVehicleInZone(vehicleZone, targetAreaList);
					} else {
						StringBuilder message = new StringBuilder();
						message.append("[Ignored] VehicleZone:").append(vehicleZone);
						if (targetAreaList.size() > 0) {
							message.append(" - AreaList:");
						}
						for (Area targetArea : targetAreaList) {
							message.append(" ").append(targetArea.getAreaId());
						}
						traceOptimizerMain(message.toString());
					}
				}
			}
		}
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private void balanceIdleVehicleInZone(String vehicleZone, ArrayList<Area> targetAreaList) {
		int sumOfMaxPlusMinIdleVehicleCountInThisVehicleZone = getSumOfMaxPlusMinIdleVehicleCount(vehicleZone, targetAreaList);
		int totalIdleVehicleCountInThisVehicleZone = getTotalIdleVehicleCount(vehicleZone);
		
		for (Area area : targetAreaList) {
			if (area != null) {
				updateTargetIdleVehicleCount(area, totalIdleVehicleCountInThisVehicleZone, sumOfMaxPlusMinIdleVehicleCountInThisVehicleZone);
			}
		}
		pullInIdleVehicle(vehicleZone, targetAreaList);
		pushOutIdleVehicle(vehicleZone, targetAreaList);
		
		vehicleManager.registerRequestedDataToDB();
		moveRequestedVehicleCount = 0;
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private int getSumOfMaxPlusMinIdleVehicleCount(String vehicleZone, ArrayList<Area> targetAreaList) {
		int sumOfMaxPlusMinIdleVehicleCount = 0;
		
		if (vehicleZone != null && vehicleZone.length() > 0) {
			for (Area area : targetAreaList) {
				if (area != null) {
					sumOfMaxPlusMinIdleVehicleCount += (area.getMaxIdleVehicleCount() + area.getMinIdleVehicleCount());
				}
			}
		}
		return sumOfMaxPlusMinIdleVehicleCount;
	}
	/**
	 * Step 
	 * 
	 * @return
	 */
	private int getTotalIdleVehicleCount(String vehicleZone) {
		int totalIdleVehicleCount = 0;
		
		if (vehicleZone != null && vehicleZone.length() > 0) {
			Vehicle vehicle = null;
			Iterator<Vehicle> itIdle = idleVehicleList.iterator();
			while (itIdle.hasNext()) {
				vehicle = itIdle.next();
				if (vehicle != null) {
					if (localVehicleList.contains(vehicle) == false) {
						if (vehicleZone.equals(vehicle.getZone())) {
							totalIdleVehicleCount++;
						}
					}
				}
			}
		}
		return totalIdleVehicleCount;
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private void updateTargetIdleVehicleCount(Area area, int totalIdleVehicleCountInThisVehicleZone, int sumOfMaxPlusMinIdleVehicleCountInThisVehicleZone) {
		assert totalIdleVehicleCountInThisVehicleZone >= 0;
		assert sumOfMaxPlusMinIdleVehicleCountInThisVehicleZone >= 0;
		
		int targetIdleVehicleCount = 0;
		int minVehicleCount = area.getMinVehicleCount();
		int maxVehicleCount = area.getMaxVehicleCount();
		int expectedVehicleCount = area.getExpectedVehicleCount();
		int stayingVehicleCount = area.getStayingVehicleCount();
		int minIdleVehicleCount = area.getMinIdleVehicleCount();
		int maxIdleVehicleCount = area.getMaxIdleVehicleCount();
		int stayingIdleVehicleCount = area.getStayingIdleVehicleCount();
		int inComingIdleVehicleCount = area.getInComingIdleVehicleCount();
		int expectedIdleVehicleCount = area.getExpectedIdleVehicleCount();
		int unassignedTrCmdCount = area.getUnassignedTrCmdCount();
		// 작업이 적어 IdleVHL이 많은 경우, IdleVHL 분배
		
		int distributedIdleVehicleCount = stayingIdleVehicleCount;
		if (sumOfMaxPlusMinIdleVehicleCountInThisVehicleZone > 0) {
			distributedIdleVehicleCount = (int) totalIdleVehicleCountInThisVehicleZone * (maxIdleVehicleCount + minIdleVehicleCount) / (sumOfMaxPlusMinIdleVehicleCountInThisVehicleZone);
		}
		distributedIdleVehicleCount = (distributedIdleVehicleCount > maxVehicleCount) ? maxVehicleCount : distributedIdleVehicleCount;
		distributedIdleVehicleCount = (distributedIdleVehicleCount < minVehicleCount) ? minVehicleCount : distributedIdleVehicleCount;
		
		StringBuilder message = new StringBuilder();
		message.append(area.getAreaId()).append("\t");
		message.append(" Min:").append(minVehicleCount);
		message.append(" Staying:").append(stayingVehicleCount);
		message.append(" Max:").append(maxVehicleCount);
		message.append(" MinIdle:").append(minIdleVehicleCount);
		message.append(" StayingIdle:").append(stayingIdleVehicleCount);
		message.append(" MaxIdle:").append(maxIdleVehicleCount);
		message.append(" InComingIdle:").append(inComingIdleVehicleCount);
		message.append(" Distributed:").append(distributedIdleVehicleCount);
		message.append(" ExpectedIdle:").append(expectedIdleVehicleCount);
		message.append(" TrCmd:").append(unassignedTrCmdCount);
		
		targetIdleVehicleCount = stayingIdleVehicleCount;
		
		if (minVehicleCount > 0 && stayingVehicleCount < minVehicleCount) {
			if (expectedVehicleCount < minVehicleCount) {
				targetIdleVehicleCount = stayingIdleVehicleCount + (minVehicleCount - expectedVehicleCount);
			}
		} else {
			if (stayingVehicleCount > maxVehicleCount || expectedVehicleCount > maxVehicleCount) {
				// VHL이 과다인 경우
				if (stayingVehicleCount - stayingIdleVehicleCount > maxVehicleCount) {
					targetIdleVehicleCount = 0;
				} else if (stayingIdleVehicleCount > maxVehicleCount) {
					targetIdleVehicleCount = maxVehicleCount;
				} else {
					targetIdleVehicleCount = minVehicleCount - (stayingVehicleCount - stayingIdleVehicleCount);
				}
			} else {
				if (distributedIdleVehicleCount < minIdleVehicleCount) {
					if (stayingIdleVehicleCount < distributedIdleVehicleCount) {
						targetIdleVehicleCount = distributedIdleVehicleCount;
					} else if (stayingIdleVehicleCount > minIdleVehicleCount) {
						targetIdleVehicleCount = minIdleVehicleCount;
					}
				} else if (distributedIdleVehicleCount > maxIdleVehicleCount) {
					if (stayingIdleVehicleCount < minIdleVehicleCount) {
						targetIdleVehicleCount = minIdleVehicleCount;
					}
				} else {
					if (stayingIdleVehicleCount < minIdleVehicleCount) {
						targetIdleVehicleCount = minIdleVehicleCount;
					} else if (stayingIdleVehicleCount > maxIdleVehicleCount) {
						targetIdleVehicleCount = maxIdleVehicleCount;
					}
				}
			}
		}
		
		message.append(" Target:").append(targetIdleVehicleCount);
		traceOptimizerMain(message.toString());
		
		area.setTargetIdleVehicleCount(targetIdleVehicleCount);
		area.setDistributedIdleVehicleCount(distributedIdleVehicleCount);
	}
	
	/**
	 * Step 
	 * 
	 */
	private void pullInIdleVehicle(String vehicleZone, ArrayList<Area> targetAreaList) {
		costSearch.initCostSearchInfo(SEARCH_TYPE.NODE);
		
		ArrayList<Target> targetList = new ArrayList<Target>();
		for (Area area : targetAreaList) {
			if (area != null) {
				getTargetNodeListForPullIn(vehicleZone, area, targetList);
			}
		}
		if (targetList.size() == 0) {
			return;
		}
		
		StringBuilder messageTarget = new StringBuilder();
		messageTarget.append("TargetNode:");
		for (Target target : targetList) {
			messageTarget.append(" ").append(target.getNodeId());
		}
		traceOptimizerMain(messageTarget.toString());
		
		ArrayList<Candidate> candidateList = new ArrayList<Candidate>();
		for (Area area : targetAreaList) {
			if (area != null) {
				getCandidateVehicleListForPullIn(vehicleZone, area, candidateList);
			}
		}
		if (candidateList.size() == 0) {
			return;
		}
		
		StringBuilder messageCandidate = new StringBuilder();
		messageCandidate.append("CandidateVehicle:");
		for (Candidate candidate : candidateList) {
			messageCandidate.append(" ").append(candidate.getVehicle().getVehicleId());
		}
		traceOptimizerMain(messageCandidate.toString());
		
		calculateCostForIdleVehicleBalancing(candidateList, targetList, true);
		TreeSet<OptimizerResult> optimizerResultSet = new TreeSet<OptimizerResult>(new OptimizerResultComparator<OptimizerResult>());
		if (assignForIdleVehicleBalancing(candidateList, targetList, optimizerResultSet)) {
			moveIdleVehicle(optimizerResultSet);
		}
	}
	
	/**
	 * Step 
	 * 
	 */
	private void pushOutIdleVehicle(String vehicleZone, ArrayList<Area> targetAreaList) {
		if (moveRequestedVehicleCount >= areaBalancingLimit) {
			return;
		}
		costSearch.initCostSearchInfo(SEARCH_TYPE.NODE);
		
		ArrayList<Candidate> candidateList = new ArrayList<Candidate>();
		for (Area area : targetAreaList) {
			if (area != null) {
				getCandidateVehicleListForPushOut(vehicleZone, area, candidateList);
			}
		}
		if (candidateList.size() == 0) {
			return;
		}
		StringBuilder messageCandidate = new StringBuilder();
		messageCandidate.append("CandidateVehicle:");
		for (Candidate candidate : candidateList) {
			messageCandidate.append(" ").append(candidate.getVehicle().getVehicleId());
		}
		traceOptimizerMain(messageCandidate.toString());
		
		ArrayList<Target> targetList = new ArrayList<Target>();
		for (Area area : targetAreaList) {
			if (area != null) {
				getTargetNodeListForPushOut(vehicleZone, area, targetList);
			}
		}
		if (targetList.size() == 0) {
			return;
		}
		StringBuilder messageTarget = new StringBuilder();
		messageTarget.append("TargetNode:");
		for (Target target : targetList) {
			messageTarget.append(" ").append(target.getNodeId());
		}
		traceOptimizerMain(messageTarget.toString());
		
		calculateCostForIdleVehicleBalancing(candidateList, targetList, false);
		TreeSet<OptimizerResult> optimizerResultSet = new TreeSet<OptimizerResult>(new OptimizerResultComparator<OptimizerResult>());
		if (assignForIdleVehicleBalancing(candidateList, targetList, optimizerResultSet)) {
			moveIdleVehicle(optimizerResultSet);
		}
	}
	
	/**
	 * 
	 * 
	 */
	private void calculateCostForIdleVehicleBalancing(ArrayList<Candidate> candidateList, ArrayList<Target> targetList, boolean isPullIn) {
		if (candidateList.size() > targetList.size()) {
			size = candidateList.size();
		} else {
			size = targetList.size();
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
		
		costSearch.initCostSearchInfo(SEARCH_TYPE.NODE);
		
		// 2. 각 Vehicle 별  Target Node에 대한 Cost 계산.
		Vehicle vehicle = null;
		Node stopNode = null;
		Candidate candidate = null;
		Target target = null;
		Area candidateArea = null;
		Area targetArea = null;
		String targetNodeId = "";
		double candidateVehiclePenalty = 0;
		for (int i = 0; i < candidateList.size(); i++) {
			costMap.clear();
			candidate = (Candidate) candidateList.get(i);
			if (candidate != null) {
				vehicle = candidate.getVehicle();
				candidateArea = candidate.getArea();
				if (vehicle != null) {
					stopNode = (Node)nodeManager.getNode(vehicle.getStopNode());
					if (stopNode != null) {
						for (int j = 0; j < targetList.size(); j++) {
							target = targetList.get(j);
							if (target != null) {
								targetNodeId = target.getNodeId();
								costMap.put(targetNodeId, MAXCOST_TIMEBASE);
							}
						}
						candidateVehiclePenalty = getCandidateVehiclePenalty(candidateArea);
						if (costSearch.costSearch(vehicle, stopNode, costMap, true, MAXCOST_TIMEBASE, COSTSEARCH_OPTION.HYBRID)) {
							for (int j = 0; j < targetList.size(); j++) {
								target = targetList.get(j);
								if (target != null) {
									targetNodeId = target.getNodeId();
									targetArea = target.getArea();
									if (costMap.containsKey(targetNodeId)) {
										cost[i][j] = (double)costMap.get(targetNodeId);
										
										if (isPullIn) {
											cost[i][j] += getTargetNodePenalty(targetArea);
										} else {
											cost[i][j] += candidateVehiclePenalty;  
										}
										
										if (cost[i][j] > MAXCOST_TIMEBASE) {
											cost[i][j] = MAXCOST_TIMEBASE;
										}
										costBackup[i][j] = cost[i][j];
									} else {
										// costNodes[i][j] = MAXCOST_TIMEBASE;
										;/*NULL*/
									}
								} else {
									// costNodes[i][j] = MAXCOST_TIMEBASE;
									;/*NULL*/
								}
							}
						} else {
							traceOptimizerMain("     [Cost Search Failed]: " + vehicle.getVehicleId());
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param trCmd
	 * @return
	 */
	private boolean checkAreaBalancing(Vehicle vehicle, String moveRequestedNodeId) {
		assert vehicle != null;
		
		try {
			Node stopNode = nodeManager.getNode(vehicle.getStopNode());
			Node moveRequestedNode = nodeManager.getNode(moveRequestedNodeId);
			
			if (stopNode == null || moveRequestedNode == null) {
				StringBuilder message = new StringBuilder();
				message.append(" StopNode:").append(vehicle.getStopNode());
				message.append(" MoveRequestedNode:").append(moveRequestedNodeId);
				traceOptimizerException(message.toString());
				return false;
			}
			
			String stopAreaId = stopNode.getAreaId();
			String moveRequestedAreaId = moveRequestedNode.getAreaId();
			
			if (stopAreaId.equals(moveRequestedAreaId) == false) {
				Area stopArea = stopNode.getArea();
				if (stopArea != null) {
					if (stopArea.getStayingIdleVehicleCount() <= stopArea.getTargetIdleVehicleCount() ||
							stopArea.getStayingVehicleCount() <= stopArea.getMinVehicleCount()) {
						return false;
					}
				}
				Area moveRequestedArea = moveRequestedNode.getArea();
				if (moveRequestedArea != null) {
					if (moveRequestedArea.getExpectedIdleVehicleCount() >= moveRequestedArea.getMaxIdleVehicleCount() ||
							moveRequestedArea.getCurrentVehicleCount() >= moveRequestedArea.getMaxVehicleCount() ||
							moveRequestedArea.getExpectedVehicleCount() >= moveRequestedArea.getMaxVehicleCount()) {
						return false;
					}
				}
			}
			return true;
		} catch (Exception e) {
			traceOptimizerException("", e);
			return false;
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
			String stopAreaId = "";
			Area stopArea = null;
			Node stopNode = nodeManager.getNode(vehicle.getStopNode());
			if (stopNode != null) {
				stopAreaId = stopNode.getAreaId();
				stopArea = stopNode.getArea();
			} else {
				return;
			}
			
			String targetAreaId = "";
			Area targetArea = null;
			Node targetNode = nodeManager.getNode(vehicle.getTargetNode());
			if (targetNode != null) {
				targetAreaId = targetNode.getAreaId();
				targetArea = targetNode.getArea();
			} else {
				return;
			}
			
			String sourceAreaId = "";
			Area sourceArea = null;
			Node sourceNode = nodeManager.getNode(nodeId);
			if (sourceNode != null) {
				sourceAreaId = sourceNode.getAreaId();
				sourceArea = sourceNode.getArea();
			} else {
				return;
			}
			
			if (stopAreaId.equals(targetAreaId)) {
				if (stopArea != null) {
					stopArea.removeStayingIdleVehicle(vehicle);
				}
				if (stopAreaId.equals(sourceAreaId) == false) {
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
				if (stopAreaId.equals(sourceAreaId)) {
					// targetArea != sourceArea
					if (stopArea != null) {
						stopArea.addStayingVehicle(vehicle);
					}
					if (targetArea != null) {
						targetArea.removeInComingVehicle(vehicle);
					}
				} else {
					if (targetAreaId.equals(sourceAreaId) == false) {
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
			traceOptimizerException("", e);
		}
	}
	
	private int getTargetNodePenalty(Area area) {
		int penalty = 1000;
		if (area != null) {
			if (area.getMinIdleVehicleCount() > 0) {
				if (area.getExpectedIdleVehicleCount() == 0) {
					return 0;
				} else {
					if (area.getExpectedIdleVehicleCount() < area.getMinIdleVehicleCount()) {
						return 100;
					} else if (area.getTargetIdleVehicleCount() > area.getExpectedIdleVehicleCount()) {
						penalty = 100 + 10 * (10 - area.getTargetIdleVehicleCount() + area.getExpectedIdleVehicleCount());
					} else if (area.getMaxIdleVehicleCount() > area.getExpectedIdleVehicleCount()) {
						penalty = 200 + 10 * (10 - area.getMaxIdleVehicleCount() + area.getExpectedIdleVehicleCount());
					} else {
						return 9999;
					}
					penalty = (penalty < 200) ? 200 : penalty;
					penalty = (penalty > 500) ? 500 : penalty;
				}
			} else {
				return 1000;
			}
		}
		return penalty;
	}
	
	private int getCandidateVehiclePenalty(Area area) {
		int penalty = 1000;
		if (area != null) {
			if (area.getStayingIdleVehicleCount() > area.getMinIdleVehicleCount()) {
				if (area.getMaxIdleVehicleCount() < area.getStayingIdleVehicleCount()) {
					penalty = 100 + 10 * (10 + area.getMaxIdleVehicleCount() - area.getStayingIdleVehicleCount());
				} else if (area.getTargetIdleVehicleCount() < area.getExpectedIdleVehicleCount()) {
					penalty = 500 + 10 * (10 + area.getTargetIdleVehicleCount() - area.getStayingIdleVehicleCount());
				}
				penalty = (penalty < 0) ? 0 : penalty;
			} else {
				return 9999;
			}
		}
		return penalty;
	}
	
	/**
	 * 
	 * 
	 */
	private boolean assignForIdleVehicleBalancing(ArrayList<Candidate> candidateList, ArrayList<Target> targetList, TreeSet<OptimizerResult> optimizerResultSet) {
		try {
			Candidate candidate = null;
			Target target = null;
			if (solver.solveAssignment(size, cost, result)) {
				double modelingCost;
				double distanceBasedCost;
				OptimizerResult optimizerResult = null;
				for (int i = 0; i < candidateList.size(); i++) {
					if (result[i] < targetList.size()) {
						candidate = candidateList.get(i);
						target = targetList.get(result[i]);
						if (candidate == null || target == null) {
							continue;
						}
						modelingCost = cost[i][result[i]];
						distanceBasedCost = costBackup[i][result[i]];
						if (modelingCost >= OcsConstant.MAXCOST_TIMEBASE) {
							continue;
						}
						
						optimizerResult = new OptimizerResult(candidate, target, modelingCost, distanceBasedCost);
						if (optimizerResultSet.contains(optimizerResult) == false) {
							optimizerResultSet.add(optimizerResult);
						}
					}
				}
				return true;
			} else {
				traceOptimizerMain("     [AP_Failed] AP_Infinite_Loop");
			}
		} catch (Exception e) {
			traceOptimizerException("     [AP_Exception] ", e);
		}
		return false;
	}
	
	/**
	 * 
	 * 
	 */
	private void moveIdleVehicle(TreeSet<OptimizerResult> optimizerResultSet) {
		double modelingCost;
		Candidate candidate = null;
		Target target = null;
		Vehicle vehicle = null;
		Area candidateArea = null;
		Area targetArea = null;
		String candidateAreaId = "";
		String targetAreaId = "";
		String targetNodeId = "";
		OptimizerResult optimizerResult = null;
		Iterator<OptimizerResult> itResult = optimizerResultSet.iterator();
		while (itResult.hasNext()) {
			if (moveRequestedVehicleCount >= areaBalancingLimit) {
				return;
			}
			
			optimizerResult = itResult.next();
			if (optimizerResult != null) {
				candidate = (Candidate)optimizerResult.getCandidate();
				if (candidate != null) {
					vehicle = candidate.getVehicle();
					candidateArea = candidate.getArea();
					if (candidateArea == null) {
						candidateAreaId = "";
					} else {
						candidateAreaId = candidateArea.getAreaId();
					}
					
					target = (Target)optimizerResult.getTarget();
					if (target != null) {
						targetArea = target.getArea();
						if (targetArea == null) {
							targetAreaId = "";
						} else {
							targetAreaId = targetArea.getAreaId();
						}
						targetNodeId = target.getNodeId();
						modelingCost = optimizerResult.getModelingCost();
						if (resultVehicleList.contains(vehicle) == false &&
								resultTargetList.contains(targetNodeId) == false) {
							if (idleVehicleList.contains(vehicle) &&
									checkAreaBalancing(vehicle, targetNodeId) &&
									localVehicleList.contains(vehicle) == false && locateVehicleList.contains(vehicle) == false) {
								StringBuilder message = new StringBuilder();
								message.append("     [MOVE] ").append(vehicle.getVehicleId());
								message.append(" C/").append(candidateAreaId).append("->").append(targetAreaId);
								message.append("/TargetNode=").append(targetNodeId);
								message.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
								traceOptimizerMain(message.toString());
								
								requestMove(vehicle, targetNodeId);
							} else {
								StringBuilder message = new StringBuilder();
								message.append("     [Can Not Move] ").append(vehicle.getVehicleId());
								message.append(" X/").append(candidateAreaId).append("->").append(targetAreaId);
								message.append("/TargetNode=").append(targetNodeId);
								message.append("/Modeling Cost=").append(String.format(FORMAT, modelingCost));
								traceOptimizerMain(message.toString());
							}
						} else {
							StringBuilder message = new StringBuilder();
							message.append("[Abnormal Case] Vehicle:").append(vehicle.getVehicleId());
							message.append(", ResultVehicleList:").append(resultVehicleList.toString());
							message.append(", ResultTargetList:").append(resultTargetList.toString());
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * 
	 * @param vehicle
	 * @param targetNodeId
	 */
	private void requestMove(Vehicle vehicle, String targetNodeId) {
		if (targetNodeId.equals(vehicle.getTargetNode()) == false) {
			vehicleManager.updateVehicleRequestedInfoToDB(MOVE, targetNodeId, vehicle.getVehicleId());
			resultVehicleList.add(vehicle);
			resultTargetList.add(targetNodeId);
			
			updateAreaVehicleCount(vehicle, targetNodeId);
			
			moveRequestedVehicleCount++;
		}
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private void getTargetNodeListForPullIn(String vehicleZone, Area area, ArrayList<Target> targetList) {
		if (area != null) {
			if (area.getCurrentVehicleCount() + area.getTargetIdleVehicleCount() < area.getMaxVehicleCount() &&
					area.getExpectedVehicleCount() + area.getTargetIdleVehicleCount() < area.getMaxVehicleCount()) {
				// Idle VHL 추가가 가능한 경우
				int pullInCount = 1;
				if (area.getTargetIdleVehicleCount() > area.getExpectedIdleVehicleCount() - area.getUnassignedTrCmdCount()) {
					if (area.getStayingIdleVehicleCount() < area.getMaxIdleVehicleCount() &&
							area.getStayingIdleVehicleCount() < area.getTargetIdleVehicleCount()) {
						// target을 맞추기 위해 Idle이 추가로 필요한 경우
						// pullInCount = targetIdleVehicleCount - (expectedIdleVehicleCount - unassignedTrCmdCount);
						getTargetNode(area, pullInCount, targetList);
						
						StringBuilder message = new StringBuilder();
						message.append(area.getAreaId()).append("\t");
						message.append(" [PullIn]:").append(pullInCount);
						message.append(" Min:").append(area.getMinVehicleCount());
						message.append(" Staying:").append(area.getStayingVehicleCount());
						message.append(" Max:").append(area.getMaxVehicleCount());
						message.append(" MinIdle:").append(area.getMinIdleVehicleCount());
						message.append(" StayingIdle:").append(area.getStayingIdleVehicleCount());
						message.append(" MaxIdle:").append(area.getMaxIdleVehicleCount());
						message.append(" Distributed:").append(area.getDistributedIdleVehicleCount());
						message.append(" ExpectedIdle:").append(area.getExpectedIdleVehicleCount());
						message.append(" TrCmd:").append(area.getUnassignedTrCmdCount());
						message.append(" TargetIdle:").append(area.getTargetIdleVehicleCount());
						traceOptimizerMain(message.toString());
					}
				}
			}
		}
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private void getCandidateVehicleListForPullIn(String vehicleZone, Area area, ArrayList<Candidate> candidateList) {
		if (area != null) {
			if (area.getStayingVehicleCount() > area.getMinVehicleCount()) {
				// Min 제약 보장.
				int candidateVehicleCount = 1;
				
//					candidateVehicleCount = area.getStayingIdleVehicleCount() - area.getTargetIdleVehicleCount();
				candidateVehicleCount = 1;
				getCandidateVehicle(vehicleZone, area, candidateVehicleCount, candidateList);
				
				StringBuilder message = new StringBuilder();
				message.append(area.getAreaId()).append("\t");
				message.append(" [CandidateVehicle]:").append(candidateVehicleCount);
				message.append(" Min:").append(area.getMinVehicleCount());
				message.append(" Staying:").append(area.getStayingVehicleCount());
				message.append(" Max:").append(area.getMaxVehicleCount());
				message.append(" MinIdle:").append(area.getMinIdleVehicleCount());
				message.append(" StayingIdle:").append(area.getStayingIdleVehicleCount());
				message.append(" MaxIdle:").append(area.getMaxIdleVehicleCount());
				message.append(" Distributed:").append(area.getDistributedIdleVehicleCount());
				message.append(" ExpectedIdle:").append(area.getExpectedIdleVehicleCount());
				message.append(" TrCmd:").append(area.getUnassignedTrCmdCount());
				message.append(" TargetIdle:").append(area.getTargetIdleVehicleCount());
				traceOptimizerMain(message.toString());
			}
		}
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private void getCandidateVehicleListForPushOut(String vehicleZone, Area area, ArrayList<Candidate> candidateList) {
		if (area != null) {
			if (area.getStayingVehicleCount() > area.getMinVehicleCount()) {
				int pushOutCount = 1;
				if (area.getStayingIdleVehicleCount() > area.getTargetIdleVehicleCount()) {
					// 탈출 시간이 짧은 VHL부터
//					candidateVehicleCount = area.getStayingIdleVehicleCount() - area.getTargetIdleVehicleCount();
					getCandidateVehicle(vehicleZone, area, pushOutCount, candidateList);
					
					StringBuilder message = new StringBuilder();
					message.append(area.getAreaId()).append("\t");
					message.append(" [PushOut]:").append(pushOutCount);
					message.append(" Min:").append(area.getMinVehicleCount());
					message.append(" Staying:").append(area.getStayingVehicleCount());
					message.append(" Max:").append(area.getMaxVehicleCount());
					message.append(" MinIdle:").append(area.getMinIdleVehicleCount());
					message.append(" StayingIdle:").append(area.getStayingIdleVehicleCount());
					message.append(" MaxIdle:").append(area.getMaxIdleVehicleCount());
					message.append(" Distributed:").append(area.getDistributedIdleVehicleCount());
					message.append(" ExpectedIdle:").append(area.getExpectedIdleVehicleCount());
					message.append(" TrCmd:").append(area.getUnassignedTrCmdCount());
					message.append(" TargetIdle:").append(area.getTargetIdleVehicleCount());
					traceOptimizerMain(message.toString());
				}
			}
		}
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private void getTargetNodeListForPushOut(String vehicleZone, Area area, ArrayList<Target> targetList) {
		if (area != null) {
			if (area.getCurrentVehicleCount() < 0.8 * area.getMaxVehicleCount() &&
					area.getExpectedVehicleCount() < 0.8 * area.getMaxVehicleCount()) {
				int targetNodeCount = 2;
				if (area.getExpectedIdleVehicleCount() - area.getUnassignedTrCmdCount() < area.getMaxIdleVehicleCount()) {
					getTargetNode(area, targetNodeCount, targetList);
					
					StringBuilder message = new StringBuilder();
					message.append(area.getAreaId()).append("\t");
					message.append(" [TargetNode]:").append(targetNodeCount);
					message.append(" Min:").append(area.getMinVehicleCount());
					message.append(" Staying:").append(area.getStayingVehicleCount());
					message.append(" Max:").append(area.getMaxVehicleCount());
					message.append(" MinIdle:").append(area.getMinIdleVehicleCount());
					message.append(" StayingIdle:").append(area.getStayingIdleVehicleCount());
					message.append(" MaxIdle:").append(area.getMaxIdleVehicleCount());
					message.append(" Distributed:").append(area.getDistributedIdleVehicleCount());
					message.append(" ExpectedIdle:").append(area.getExpectedIdleVehicleCount());
					message.append(" TrCmd:").append(area.getUnassignedTrCmdCount());
					message.append(" TargetIdle:").append(area.getTargetIdleVehicleCount());
					traceOptimizerMain(message.toString());
				}
			}
		}
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private void getCandidateVehicle(String vehicleZone, Area area, int candidateCount, ArrayList<Candidate> candidateList) {
		if (area != null && candidateCount > 0 && candidateList != null) {
			int count = 0;
			for (Vehicle inComingIdleVehicle : area.getInComingIdleVehicleList()) {
				if (inComingIdleVehicle != null) {
					if (locateVehicleList.contains(inComingIdleVehicle) == false) {
						if (resultVehicleList.contains(inComingIdleVehicle) == false) {
							if (candidateList.contains(inComingIdleVehicle) == false) {
								candidateList.add(new Candidate(area, inComingIdleVehicle));
								count++;
							}
						}
					}
				}
			}
			if (count >= candidateCount) {
				return;
			}
			TreeSet<EscapeResult> escapeResultList = new TreeSet<EscapeResult>(new EscapeResultComparator<EscapeResult>());
			for (Vehicle stayingIdleVehicle : area.getStayingIdleVehicleList()) {
				if (stayingIdleVehicle != null) {
					if (locateVehicleList.contains(stayingIdleVehicle) == false) {
						if (parkNodeIdList.contains(stayingIdleVehicle.getTargetNode()) == false && 
								parkNodeIdList.contains(stayingIdleVehicle.getCurrNode()) == false) {
							if (resultVehicleList.contains(stayingIdleVehicle) == false) {
								escapeResultList.add(new EscapeResult(stayingIdleVehicle, costSearch.costSearchToEscapeArea(stayingIdleVehicle)));
							}
						}
					}
				}
			}
			
			EscapeResult escapeResult = null;
			Vehicle candidateVehicle = null;
			Iterator<EscapeResult> it = escapeResultList.iterator();
			while (it.hasNext()) {
				escapeResult = it.next();
				if (escapeResult != null) {
					candidateVehicle = escapeResult.getVehicle();
					if (candidateVehicle != null) {
						if (candidateList.contains(candidateVehicle) == false) {
							candidateList.add(new Candidate(area, candidateVehicle));
							count++;
						}
					}
				}
				if (count >= candidateCount) {
					return;
				}
			}
		}
	}
	
	/**
	 * Step 
	 * 
	 * @return
	 */
	private void getTargetNode(Area area, int targetCount, ArrayList<Target> targetList) {
		if (area != null && targetCount > 0 && targetList != null) {
			int count = 0;
			String sourceNodeId;
			if (area.getUnassignedTrCmdCount() > 0) {
				for (TrCmd trCmd : area.getUnassignedTrCmdList()) {
					if (trCmd != null) {
						sourceNodeId = trCmd.getSourceNode();
						if (sourceNodeId != null && sourceNodeId.length() > 0) {
							if (resultTargetList.contains(sourceNodeId) == false) {
								if (targetList.contains(sourceNodeId) == false) {
									targetList.add(new Target(area, sourceNodeId));
									count++;
								}
							}
						}
					}
					if (count >= targetCount) {
						return;
					}
				}
			}
			for (String nodeId : areaManager.getTargetNodeList(area.getAreaId())) {
				if (targetList.contains(nodeId) == false) {
					targetList.add(new Target(area, nodeId));
					count++;
				}
				if (count >= targetCount) {
					return;
				}
			}
		}
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
									nodeManager.addAbnormalNodeToList(vehicle.getCurrNode());
									nodeManager.addAbnormalNodeToList(vehicle.getStopNode());
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
							nodeManager.addAbnormalNodeToList(vehicle.getCurrNode());
							nodeManager.addAbnormalNodeToList(vehicle.getStopNode());
						}
					} else {
						// Manual Vehicles
						nodeManager.addAbnormalNodeToList(vehicle.getCurrNode());
						nodeManager.addAbnormalNodeToList(vehicle.getStopNode());
					}
				}
			}
		}
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
		reservedVehicleIdList.clear();
		
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
						if (reservedVehicleIdList.contains(assignedVehicleId) == false) {
							reservedVehicleIdList.add(assignedVehicleId);
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
	 * Step 1-4-4 Classify TrCmds and LocatingVehicles [unassignedTrCmdList, idleVehicleList, loadVehicleList, destLocOfUnloadedVehicleList, locateVehicleList, locateNodeIdMap, idleLocalVehicleList]
	 * 
	 */
	private void classifyTrCmdsAndVehicles() {
		unassignedTrCmdList.clear();
		loadVehicleList.clear();
		locateVehicleList.clear();
		locateNodeIdMap.clear();
		idleLocalVehicleList.clear();
		idleVehicleLocalGroupIdList.clear();
		
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
						traceOptimizerException(message.toString());
						
						continue;
					}
					// Vehicle이 Assign 되지 않은 TrCmd.
					if (trCmd.isPause() == false) {
						if (isHidControlUsed == false || checkHidStatusOfWorkNodes(trCmd)) {
							if (nodeManager.isValidNode(trCmd.getSourceNode()) && nodeManager.isValidNode(trCmd.getDestNode())) {
								if (assignedTrCmdList.contains(trCmd) == false) {
									// Not Assigned TrCmd.
									
									if (trCmd.getDetailState() == TRCMD_DETAILSTATE.NOT_ASSIGNED) {
										if (unassignedTrCmdList.contains(trCmd) == false) {
											unassignedTrCmdList.add(trCmd);
										}
									} else {
										StringBuilder message = new StringBuilder();
										message.append("Abnormal Case02! trCmdId:").append(trCmd.getTrCmdId());
										message.append(", Vehicle:").append(trCmd.getVehicle());
										message.append(", AssignedVehicle:").append(assignedVehicleId);
										message.append(", State:").append(trCmd.getState().toConstString());
										message.append(", DetailState:").append(trCmd.getDetailState().toConstString());
										traceOptimizerException(message.toString());
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
										traceOptimizerException(message2.toString());
									}
								}
							} else {
								StringBuilder message = new StringBuilder();
								message.append("Work Node is not valid. TrCmdId:").append(trCmd.getTrCmdId());
								message.append(", SourceNode:").append(trCmd.getSourceNode());
								message.append(", DestNode:").append(trCmd.getDestNode());
								traceOptimizerMain(message.toString());
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
										if (reservedVehicleIdList.contains(assignedVehicleId) == false) {
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
										if ((reservedVehicleIdList.contains(assignedVehicleId) == false) &&
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
				locateNodeIdMap.put(idleVehicle.getRequestedData(), idleVehicle);
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
	
	
	/**
	 * Step 3-2-3 Select Park Nodes and Build TargetNodeList
	 * 
	 */
	private void getParkNodes() {
		parkList.clear();
		targetParkList.clear();
		parkNodeIdList.clear();
		
		Park park = null;
		ConcurrentHashMap<String, Object> tempParkList = parkManager.getData();
		Set<String> searchKeys = new HashSet<String>(tempParkList.keySet());
		for (String searchKey : searchKeys) {
			park = (Park)tempParkList.get(searchKey);
			if (park != null) {
				parkList.put(searchKey, park);
			}
		}
		Set<String> searchParkKeys = new HashSet<String>(parkList.keySet());
		for (String searchKey : searchParkKeys) {
			park = (Park)parkList.get(searchKey);
			if (park != null) {
				manageParkCapacity(park);
			}
		}
		
		for (Park targetPark : targetParkList) {
			if (targetPark != null) {
				if (parkNodeIdList.contains(targetPark.getName()) == false) {
					parkNodeIdList.add(targetPark.getName());
				}
			}
		}
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
					traceOptimizerException("Source HID is not registered. SourceNode:" + trCmd.getSourceNode());
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
					traceOptimizerException("Dest HID is not registered. DestNode:" + trCmd.getDestNode());
				}
			} else {
				return false;
			}	
		} catch (Exception e) {
			traceOptimizerException("SourceNode:" + trCmd.getSourceNode() + ", DestNode:" + trCmd.getDestNode(), e);
		}
		
		if ((sourceHid != null && sourceHid.isAbnormalState()) || (destHid != null && destHid.isAbnormalState())) {
			StringBuilder message = new StringBuilder();
			message.append("[Job of Abnormal HID:] Removed Job in the abnormal HID. ");
			message.append("TrCmdId:").append(trCmd.getTrCmdId());
			message.append(", SourceNode:").append(trCmd.getSourceNode());
			if (sourceHid != null) {
				message.append("(HID:").append(sourceHid.getUnitId());
				message.append(",Status:").append(sourceHidState);
				message.append(", AltHID:").append(sourceHid.getAltHidName());
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
				message.append(", AltHID:").append(destHid.getAltHidName());
				Hid destAltHid = destHid.getAltHid();
				if (destAltHid != null) {
					message.append("(").append(destAltHid.getState()).append(")");
				}
			} else {
				message.append("(HID: NOT AVAILABLE");
			}
			traceOptimizerMain(message.toString());
			return false;
		} else {
			return true;
		}
	}
	
	private void manageParkCapacity(Park park) {
		assert park != null;
		
		String parkName = park.getName();
		int parkCapacity = park.getCapacity();
		int registeredParkNodeCount = 1;
		
		Node lastRegisteredParkNode = nodeManager.getNode(parkName);
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
//							Park newPark = new Park(prevNode.getNodeId(), park.getType(), park.getVehicleZone(), 1, park.getRank(), true);
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
	
	private static final String OPTIMIZER_MAIN = "OptimizerMain";
	private static Logger optimizerMainLog = Logger.getLogger(OPTIMIZER_MAIN);
	
	public void traceOptimizerMain(String message) {
		optimizerMainLog.debug(message);
	}
	
	private static final String OPTIMIZER_DELAY = "OptimizerDelay";
	private static Logger optimizerDelayLog = Logger.getLogger(OPTIMIZER_DELAY);
	public void traceOptimizerDelay(String message) {
		optimizerDelayLog.debug(message);
	}
	
	private static final String OPTIMIZER_EXCEPTION = "OptimizerException";
	private static Logger optimizerExceptionLog = Logger.getLogger(OPTIMIZER_EXCEPTION);
	public void traceOptimizerException(String message) {
		optimizerExceptionLog.error(message);
	}
	public void traceOptimizerException(String message, Throwable e) {
		optimizerExceptionLog.error(message, e);
	}
	
	public void initializeForRuntimeUpdate() {
		costSearch.setNearByDrive(ocsInfoManager.isNearByDrive());
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
 * EscapeResultComparator Class, OCS 3.0 for Unified FAB
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
class EscapeResultComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		EscapeResult t1 = (EscapeResult)o1;
		EscapeResult t2 = (EscapeResult)o2;
		if (t1.getCost() < t2.getCost()) {
			return -1;
		} else if (t1.getCost() == t2.getCost()) {
			return t1.getVehicle().getVehicleId().compareTo(t2.getVehicle().getVehicleId());
		} else  {
			return 1;
		}
	}
}

/**
 * OptimizerResultComparator Class, OCS 3.0 for Unified FAB
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
class OptimizerResultComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		OptimizerResult t1 = (OptimizerResult)o1;
		OptimizerResult t2 = (OptimizerResult)o2;
		if (t1.getModelingCost() < t2.getModelingCost()) {
			return -1;
		} else if (t1.getModelingCost() == t2.getModelingCost()) {
			if (t1.getDistanceBasedCost() < t2.getDistanceBasedCost()) {
				return -1;
			} else if (t1.getDistanceBasedCost() == t2.getDistanceBasedCost()) {
				return t1.getCandidate().getVehicle().getVehicleId().compareTo(t2.getCandidate().getVehicle().getVehicleId());
			} else  {
				return 1;
			}
		} else  {
			return 1;
		}
	}
}

/**
 * CandidateAreaComparator Class, OCS 3.0 for Unified FAB
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
class CandidateAreaComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		Area t1 = (Area)o1;
		Area t2 = (Area)o2;
		
		int t1Gap = t1.getTargetIdleVehicleCount() - t1.getMinIdleVehicleCount();
		int t2Gap = t2.getTargetIdleVehicleCount() - t2.getMinIdleVehicleCount();
		
		if (t1Gap > t2Gap) {
			return -1;
		} else if (t1Gap == t2Gap) {
			if (t1.getMinIdleVehicleCount() < t2.getMinIdleVehicleCount()) {
				return -1;
			} else if (t1.getMinIdleVehicleCount() == t2.getMinIdleVehicleCount()) {
				if (t1.getMaxIdleVehicleCount() < t2.getMaxIdleVehicleCount()) {
					return -1;
				} else if (t1.getMaxIdleVehicleCount() == t2.getMaxIdleVehicleCount()) {
					return 0;
				} else {
					return 1;
				}
			} else {
				return 1;
			}
		} else  {
			return 1;
		}
	}
}


/**
 * TargetAreaComparator Class, OCS 3.0 for Unified FAB
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
class TargetAreaComparator<T> implements Comparator<T> {
	public int compare(T o1, T o2) {
		Area t1 = (Area)o1;
		Area t2 = (Area)o2;
		
		if (t1.getMinIdleVehicleCount() < t2.getMinIdleVehicleCount()) {
			return -1;
		} else if (t1.getMinIdleVehicleCount() == t2.getMinIdleVehicleCount()) {
			if (t1.getMaxIdleVehicleCount() < t2.getMaxIdleVehicleCount()) {
				return -1;
			} else if (t1.getMaxIdleVehicleCount() == t2.getMaxIdleVehicleCount()) {
				return 0;
			} else {
				return 1;
			}
		} else  {
			return 1;
		}
	}
}
