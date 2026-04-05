package com.samsung.ocs.longrun;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.CYCLE_UNIT;
import com.samsung.ocs.common.constant.OcsConstant.TOUR_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.USERREQUEST_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.COSTSEARCH_OPTION;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.longrun.model.Path;
import com.samsung.ocs.longrun.model.PreviousPatrolRequest;
import com.samsung.ocs.longrun.model.PreviousMoveRequest;
import com.samsung.ocs.longrun.model.PreviousTransferRequest;
import com.samsung.ocs.longrun.model.PreviousVibrationRequest;
import com.samsung.ocs.longrun.model.VibrationRequest;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.CollisionManager;
import com.samsung.ocs.manager.impl.DockingStationManager;
import com.samsung.ocs.manager.impl.MaterialControlManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.TourManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.UserRequestManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.ZoneControlManager;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.Collision;
import com.samsung.ocs.manager.impl.model.DockingStation;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Tour;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.UserRequest;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.route.search.CostSearch;

public class LongRun {
	private CarrierLocManager carrierLocManager = null;
	private NodeManager nodeManager = null;
	private CollisionManager collisionManager = null;
	private OCSInfoManager ocsInfoManager = null;
	private TrCmdManager trCmdManager = null;
	private VehicleManager vehicleManager = null;
	private ZoneControlManager zoneControlManager = null;
	private AlarmManager alarmManager = null;
	private UserRequestManager userRequestManager = null;
	private TourManager tourManager = null;
	private DockingStationManager dockingStationManager = null;
	// 2013.08.30 by KYK
	private MaterialControlManager materialControlManager = null;
	
	private ConcurrentHashMap<String, Object> userRequestMap = null;	// Enabled
	private ConcurrentHashMap<String, Object> trCmdMap;					// 전체 TrCmd
	
	private HashMap<String, Vehicle> vehicleMap;						// Normal Auto Enabled Vehicles
	private HashMap<Vehicle, TrCmd> assignedTrCmdMap;					// Assigned or AssignRequested TrCmds
	private HashMap<String, TrCmd> trCmdWithCarrierIdMap;
	
	private HashMap<String, VibrationRequest> vibrationRequestWithDockingStationMap;
	
	private HashMap<UserRequest, Object> previousRequestedMap;
	
	private ArrayList<Vehicle> idleVehicleList;							// 작업을 수행하지 않고 있는 Idle Vehicle
	private ArrayList<Vehicle> assignedVehicleList;						// Unloaded 작업부터 Loading 구간내 작업을 수행중인 Vehicle
	private ArrayList<Vehicle> tempVehicleList;
	
	private ArrayList<TrCmd> controledVibrationTrCmdList;
	
	private ArrayList<UserRequest> patrolRequestList; 
	private ArrayList<UserRequest> vibrationRequestList; 
	private ArrayList<UserRequest> fixedTransferRequestList; 
	private ArrayList<UserRequest> randomTransferRequestList; 
	private ArrayList<UserRequest> moveRequestList;
	
	private ArrayList<UserRequest> userRequestList;
	private ArrayList<UserRequest> checkRandomTransferRequestList;
	
	private ArrayList<UserRequest> validRequestList;
	
	private ArrayList<CarrierLoc> occupiedCarrierLocList;
	private HashMap<CarrierLoc, String> carrierWithCarrierLocMap;
	
	// 2013.08.30 by KYK
	private ArrayList<String> materialAssignAllowedList;
	
	private COSTSEARCH_OPTION costSearchOption = COSTSEARCH_OPTION.NONE;
	private CostSearch costSearch;
	
	private SimpleDateFormat sdf;
	
	private boolean isNearByDrive = false;
	private boolean isPatrolControlUsed = false;
	private boolean isLongRunMoveUsed = false;
	private boolean isLongRunTransferUsed = false;
	private boolean isVibrationControlUsed = false;
	private boolean isYieldSearchUsed = true;
	private static final String MOVE = "MOVE";
	private static final String LOCATE = "LOCATE";
	private static final String VIBRATIONCHANGE = "VIBRATIONCHANGE";
	private static final String CANCEL = "CANCEL";
	private static final String REMOVE = "REMOVE";
	
	private static final String COMMA = ",";
	private static final String COLON = ":";
	
	private static final String PATROL_VEHICLEZONE = "Patrol";
	private static final String CARRIERID_PREFIX = "Carrier_";
	private static final String TRCMDID_PREFIX = "Request_";
	
	private static final long UNIT_SECOND 					=       1000L;
	private static final long UNIT_MINUTE 					=      60000L;
	private static final long UNIT_HOUR 					=    3600000L;
	private static final long UNIT_DAY 						=   86400000L;
	private static final long DEFAULT_CYCLETIME_LIMIT		= 604800000L; // 7 Days
	
	private static final long TOURLIST_CHECKEDTIME_LIMIT	=   86400000L; // 1 Day
	private static final double MAXCOST_TIMEBASE = 9999.0;
	
	private long currentDBTime = 0;
	
	/**
	 * Constructor of LongRun class.
	 */
	public LongRun() {
		this.ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		this.userRequestManager = UserRequestManager.getInstance(null, null, false, false, 0);
		this.tourManager = TourManager.getInstance(null, null, false, false, 0);
		this.dockingStationManager = DockingStationManager.getInstance(null, null, false, false, 0);
		this.vehicleManager = VehicleManager.getInstance(null, null, false, false, 0);
		this.trCmdManager = TrCmdManager.getInstance(null, null, false, false, 0);
		this.nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		this.collisionManager = CollisionManager.getInstance(null, null, false, false, 0);
		this.carrierLocManager = CarrierLocManager.getInstance(null, null, false, false, 0);
		this.zoneControlManager = ZoneControlManager.getInstance(null, null, false, false, 0);
		this.alarmManager = AlarmManager.getInstance(null, null, false, false, 0);
		// 2013.08.30 by KYK
		this.materialControlManager = MaterialControlManager.getInstance(null, null, false, false, 0);
		
		vehicleMap = new HashMap<String, Vehicle>();
		assignedTrCmdMap = new HashMap<Vehicle, TrCmd>();
		trCmdWithCarrierIdMap = new HashMap<String, TrCmd>();
		
		previousRequestedMap = new HashMap<UserRequest, Object>();
		vibrationRequestWithDockingStationMap = new HashMap<String, VibrationRequest>();
		
		idleVehicleList = new ArrayList<Vehicle>();
		assignedVehicleList = new ArrayList<Vehicle>();
		tempVehicleList = new ArrayList<Vehicle>();
		
		controledVibrationTrCmdList = new ArrayList<TrCmd>();
		
		patrolRequestList = new ArrayList<UserRequest>();
		vibrationRequestList = new ArrayList<UserRequest>();
		fixedTransferRequestList = new ArrayList<UserRequest>();
		randomTransferRequestList = new ArrayList<UserRequest>();
		moveRequestList = new ArrayList<UserRequest>();
		
		userRequestList = new ArrayList<UserRequest>();
		checkRandomTransferRequestList = new ArrayList<UserRequest>();
		
		validRequestList = new ArrayList<UserRequest>();
		
		occupiedCarrierLocList = new ArrayList<CarrierLoc>();
		carrierWithCarrierLocMap = new HashMap<CarrierLoc, String>();
		
		// 2013.08.30 by KYK
		materialAssignAllowedList = new ArrayList<String>();
		
		costSearch = new CostSearch();
		
		sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		
		isNearByDrive = ocsInfoManager.isNearByDrive();
		isPatrolControlUsed = ocsInfoManager.isPatrolControlUsed();
		isLongRunMoveUsed = ocsInfoManager.isLongRunMoveUsed();
		isLongRunTransferUsed = ocsInfoManager.isLongRunTransferUsed();
		isVibrationControlUsed = ocsInfoManager.isVibrationControlUsed();
		isYieldSearchUsed = ocsInfoManager.isYieldSearchUsed();
		costSearchOption = ocsInfoManager.getCostSearchOption();
		
		// 2013.08.30 by KYK
		materialAssignAllowedList = materialControlManager.getMaterialAssignAllowedList();
	}
	
	/**
	 * MainProcessing
	 */
	public void mainProcessing() {
		try {
			if (ocsInfoManager.isPatrolControlUsed() ||
					ocsInfoManager.isVibrationControlUsed() ||
					ocsInfoManager.isLongRunMoveUsed() ||
					ocsInfoManager.isLongRunTransferUsed()) {
				updateOperationalParameters();
				getVehiclesAndTrCmds();
				getUserRequests();
				processUserRequests();
				disposeUncontrolled();
			} else {
				clearRequestLists();
			}
		} catch (Exception e) {
			traceLongRunException("mainProcessing()", e);
		}
	}
	
	private void updateOperationalParameters() {
		try {
			isNearByDrive = ocsInfoManager.isNearByDrive();
			isPatrolControlUsed = ocsInfoManager.isPatrolControlUsed();
			isLongRunMoveUsed = ocsInfoManager.isLongRunMoveUsed();
			isLongRunTransferUsed = ocsInfoManager.isLongRunTransferUsed();
			isVibrationControlUsed = ocsInfoManager.isVibrationControlUsed();
			isYieldSearchUsed = ocsInfoManager.isYieldSearchUsed();
			costSearchOption = ocsInfoManager.getCostSearchOption();
			
			dockingStationManager.updateManuallyFromDB();
			vehicleManager.updateFromDBForJobAssign();
			trCmdManager.updateFromDBForJobAssign();
		} catch (Exception e) {
			traceLongRunException("updateOperationalParameters()", e);
		}
		
		try {
			currentDBTime = sdf.parse(ocsInfoManager.getCurrDBTimeStr()).getTime();
		} catch (Exception e) {
			traceLongRunException("updateOperationalParameters()", e);
			currentDBTime = System.currentTimeMillis();
		}
	}
	
	private void processUserRequests() {
		Set<UserRequest> removeKeys = new HashSet<UserRequest>(previousRequestedMap.keySet());
		for (UserRequest userRequest : userRequestList) {
			if (userRequest != null) {
				switch (userRequest.getType()) {
					case PATROL:
						processPatrol(userRequest, removeKeys);
						break;
					case VIBRATION:
						processVibration(userRequest, removeKeys);
						break;
					case RANDOM_TRANSFER:
						processRandomTransfer(userRequest, removeKeys);
						break;
					case FIXED_TRANSFER:
						processFixedTransfer(userRequest, removeKeys);
						break;
					case MOVE:
						processMove(userRequest, removeKeys);
						break;
					default:
						StringBuilder message = new StringBuilder();
						message.append("UserRequest Id:").append(userRequest.getUserRequestId());
						message.append("(Type:").append(userRequest.getTypeString()).append(") is NOT allowed.");
						traceLongRunAbnormal(message.toString());
						disableUserRequest(userRequest);
						break;
				}
			} else {
				traceLongRunAbnormal("processUserRequests() - userRequest is null.");
			}
		}
		
		for (UserRequest rmKey : removeKeys) {
			if (rmKey != null) {
				previousRequestedMap.remove(rmKey);
				if (rmKey.getType() == USERREQUEST_TYPE.RANDOM_TRANSFER) {
					checkRandomTransferRequestList.remove(rmKey);
				}
			}
		}
	}
	
	private void disposeUncontrolled() {
		if (trCmdMap.size() > 0) {
			Set<String> trCmdIdKeys = new HashSet<String>(trCmdMap.keySet());
			TrCmd trCmd = null;
			for (String trCmdId : trCmdIdKeys) {
				trCmd = (TrCmd)trCmdMap.get(trCmdId);
				if (trCmd != null) {
					if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
						switch (trCmd.getState()) {
							case CMD_QUEUED:
							case CMD_WAITING:
							case CMD_MONITORING:
							{
								if (controledVibrationTrCmdList.contains(trCmd) == false) {
									StringBuilder message = new StringBuilder();
									message.append(trCmd.getTrCmdId()).append("> ");
									message.append("Uncontrolled TrCmd (RemoteCmd:VIBRATION).");
									traceLongRunMain(message.toString());
									
									cancelVibrationTrCmd(trCmd);
								}
								break;
							}
							default:
							{
								break;
							}
						}
					}
				}
			}
		}
	}
	
	private void clearRequestLists() {
		userRequestManager.update();
		userRequestMap = userRequestManager.getData();
		
		UserRequest userRequest = null;
		Set<String> searchKeys = new HashSet<String>(userRequestMap.keySet());
		for (String searchKey : searchKeys) {
			userRequest = (UserRequest)userRequestMap.get(searchKey);
			if (userRequest != null) {
				if (userRequest.isEnabled()) {
					disableUserRequest(userRequest);
				}
			}
		}
		
		previousRequestedMap.clear();
		userRequestList.clear();
		occupiedCarrierLocList.clear();
		carrierWithCarrierLocMap.clear();
		checkRandomTransferRequestList.clear();
		traceLongRunMain("PatrolControlUsage, VibrationControlUsage, LongRunTransferUsage, and LongRunMoveUsage are ALL Unused.");
	}
	
	private void processMove(UserRequest userRequest, Set<UserRequest> removeKeys) {
		try {
			if (userRequest != null) {
				Vehicle vehicle = vehicleMap.get(userRequest.getVehicleId());
				if (vehicle != null) {
					if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
						if (userRequest.getTourListSize() > 1) {
							if (vehicle.isAssignHold() || idleVehicleList.contains(vehicle)) {
								if (previousRequestedMap.containsKey(userRequest)) {
									PreviousMoveRequest previousMoveRequested = (PreviousMoveRequest) previousRequestedMap.get(userRequest);
									if (previousMoveRequested != null) {
										if (vehicle.getTargetNode().equals(previousMoveRequested.getNodeId())) {
											if (vehicle.getStopNode().equals(vehicle.getTargetNode())) {
												if (previousMoveRequested.getIndex() == userRequest.getTourListSize() - 1) {
													userRequestManager.updateLastTouredTime(userRequest.getUserRequestId());
													previousRequestedMap.remove(userRequest);
													
													StringBuilder message = new StringBuilder();
													message.append(userRequest.getUserRequestId()).append("> ");
													message.append("MoveRequest Completed.");
													traceLongRunMain(message.toString());
												} else {
													int index = (previousMoveRequested.getIndex() + 1) % userRequest.getTourListSize();
													String nodeId = (String)userRequest.getTourListItem(index);
													previousMoveRequested.setIndex(index);
													previousMoveRequested.setNodeId(nodeId);
													
													requestMoveToVehicle(userRequest, vehicle, previousMoveRequested);
												}
											} else {
												// Waiting
												; /*NULL*/
											}
										} else {
											if (vehicle.getRequestedData().equals(previousMoveRequested.getNodeId()) == false) {
												if (isMovableTo(vehicle, vehicle.getStopNode(), previousMoveRequested.getNodeId())) {
													StringBuilder message = new StringBuilder();
													message.append(userRequest.getUserRequestId()).append("> ");
													message.append("VehicleId:").append(vehicle.getVehicleId());
													message.append(", TargetNode:").append(vehicle.getTargetNode());
													message.append(", RequestedData:").append(vehicle.getRequestedData());
													message.append(", PrevRequested:").append(previousMoveRequested.getNodeId());
													traceLongRunMain(message.toString());
													
													moveVehicle(vehicle, previousMoveRequested.getNodeId());
												} else {
													// Waiting
													; /*NULL*/
												}
											} else {
												// Waiting
												; /*NULL*/
											}
										}
									}
								} else {
									requestMoveToVehicle(userRequest, vehicle, new PreviousMoveRequest((String)userRequest.getTourListItem(0)));
								}
							} else {
								// Waiting
								; /*NULL*/
							}
						} else {
							traceLongRunAbnormal("processMove() - Empty TourList. TourList:" + userRequest.getTourListString());
							disableUserRequest(userRequest);
						}
					} else {
						// Waiting
						; /*NULL*/
					}
					removeKeys.remove(userRequest);
				} else {
					traceLongRunAbnormal("processMove() - vehicle is null.");
					disableUserRequest(userRequest);
				}
			}
		} catch (Exception e) {
			traceLongRunException("processMove()", e);
		}
	}
	
	private void processFixedTransfer(UserRequest userRequest, Set<UserRequest> removeKeys) {
		try {
			if (userRequest != null) {
				if (userRequest.getTourListSize() > 1) {
					Vehicle vehicle = vehicleMap.get(userRequest.getVehicleId());
					if (vehicle != null) {
						if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
							String carrierId = getCarrierId(userRequest.getUserRequestId());
							TrCmd trCmd = trCmdWithCarrierIdMap.get(carrierId);
							if (previousRequestedMap.containsKey(userRequest)) {
								PreviousTransferRequest previousTransferRequested = (PreviousTransferRequest) previousRequestedMap.get(userRequest);
								if (previousTransferRequested != null) {
									int prevIndex = previousTransferRequested.getIndex();
									CarrierLoc prevSourceLoc = previousTransferRequested.getSourceLoc();
									CarrierLoc prevDestLoc = previousTransferRequested.getDestLoc();
									if (prevSourceLoc != null && prevDestLoc != null) {
										if (trCmd == null) {
											if (vehicle.getTargetNode().equals(prevDestLoc.getNode())) {
												if (prevIndex == userRequest.getTourListSize() - 1) {
													userRequestManager.updateLastTouredTime(userRequest.getUserRequestId());
													previousRequestedMap.remove(userRequest);
													
													StringBuilder message = new StringBuilder();
													message.append(userRequest.getUserRequestId()).append("> ");
													message.append("FixedTransferRequest Completed.");
													traceLongRunMain(message.toString());
												} else {
													int index = (prevIndex + 1) % userRequest.getTourListSize();
													CarrierLoc destLoc = (CarrierLoc)userRequest.getTourListItem((index + 1) % userRequest.getTourListSize());
													if (destLoc != null) {
														// 2013.08.30 by KYK : MaterialControl 고려
//														if (vehicle.getMaterial().equals(prevDestLoc.getMaterial()) &&
//																vehicle.getMaterial().equals(destLoc.getMaterial())) {
														// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
//														if (prevDestLoc.getMaterial().equals(destLoc.getMaterial()) && isMaterialAssignAllowed(vehicle, prevDestLoc)) {
														if (prevDestLoc.getMaterial().equals(destLoc.getMaterial()) && isMaterialAssignAllowed(vehicle, prevDestLoc, destLoc)) {
															previousTransferRequested.setIndex(index);
															previousTransferRequested.setSourceLoc(prevDestLoc);
															previousTransferRequested.setDestLoc(destLoc);
															
															requestTransfer(userRequest, vehicle, carrierId, previousTransferRequested);
														} else {
															traceLongRunAbnormal("processFixedTransfer() - Invalid Materials.");
															disableUserRequest(userRequest);
														}
													} else {
														traceLongRunAbnormal("processFixedTransfer() - newDestLoc is null.");
														disableUserRequest(userRequest);
													}
												}
											} else {
												if (isMovableTo(vehicle, vehicle.getStopNode(), prevDestLoc.getNode())) {
													moveVehicle(vehicle, prevDestLoc.getNode());
												} else {
													// Waiting
													; /*NULL*/
												}
											}
										} else {
											// Waiting
											; /*NULL*/
										}
									} else {
										traceLongRunAbnormal("processFixedTransfer() - sourceLoc or destLoc is null.");
										disableUserRequest(userRequest);
									}
								} else {
									traceLongRunAbnormal("processFixedTransfer() - previousTransferRequested is null.");
								}
							} else {
								if (trCmd == null) {
									if (vehicle.isAssignHold() || idleVehicleList.contains(vehicle)) {
										CarrierLoc sourceLoc = (CarrierLoc)userRequest.getTourListItem(0);
										CarrierLoc destLoc = (CarrierLoc)userRequest.getTourListItem(1);
										if (sourceLoc != null && destLoc != null) {
											// 2013.08.30 by KYK : MaterialControl 고려
//											if (vehicle.getMaterial().equals(sourceLoc.getMaterial()) &&
//													vehicle.getMaterial().equals(destLoc.getMaterial())) {
//											if (sourceLoc.getMaterial().equals(destLoc.getMaterial()) && isMaterialAssignAllowed(vehicle, sourceLoc)) {
											// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
											if (sourceLoc.getMaterial().equals(destLoc.getMaterial()) && isMaterialAssignAllowed(vehicle, sourceLoc, destLoc)) {
												requestTransfer(userRequest, vehicle, carrierId, new PreviousTransferRequest(sourceLoc, destLoc));
											} else {
												traceLongRunAbnormal("processFixedTransfer() - Vehicle's Material is NOT same with carrierLoc's sourceLoc or destLoc is null.");
												disableUserRequest(userRequest);
											}
										} else {
											traceLongRunAbnormal("processFixedTransfer() - sourceLoc or destLoc is null.");
											disableUserRequest(userRequest);
										}
									} else {
										traceLongRunAbnormal("processFixedTransfer() - vehicle is NOT idle.");
									}
								} else {
									// Waiting
									; /*NULL*/
								}
							}
						} else {
							// Waiting
							; /*NULL*/
						}
						removeKeys.remove(userRequest);
					} else {
						traceLongRunAbnormal("processFixedTransfer() - vehicle is null. " + userRequest.getVehicleId() + " is null.");
						disableUserRequest(userRequest);
					}
				} else {
					traceLongRunAbnormal("processFixedTransfer() - ");
					disableUserRequest(userRequest);
				}
			} else {
				traceLongRunAbnormal("processFixedTransfer() - userRequest is null.");
			}
		} catch (Exception e) {
			traceLongRunException("processFixedTransfer()", e);
		}
	}
	
	private void processRandomTransfer(UserRequest userRequest, Set<UserRequest> removeKeys) {
		try {
			if (userRequest != null) {
				String carrierId = getCarrierId(userRequest.getUserRequestId());
				if (carrierId != null && carrierId.length() > 0) {
					PreviousTransferRequest previousTransferRequested = null;
					if (previousRequestedMap.containsKey(userRequest)) {
						previousTransferRequested = (PreviousTransferRequest) previousRequestedMap.get(userRequest);
						if (previousTransferRequested != null) {
							CarrierLoc prevSourceLoc = previousTransferRequested.getSourceLoc();
							CarrierLoc prevDestLoc = previousTransferRequested.getDestLoc();
							if (prevSourceLoc != null && prevDestLoc != null) {
								TrCmd trCmd = trCmdWithCarrierIdMap.get(carrierId);
								if (trCmd == null) {
									int index = getIndexForRandomTransfer(userRequest, prevDestLoc, userRequest.getTourListSize()) % userRequest.getTourListSize();
									CarrierLoc destLoc = (CarrierLoc)userRequest.getTourListItem(index);
									if (destLoc != null) {
										if (occupiedCarrierLocList.contains(destLoc) == false) {
											if (prevDestLoc.getMaterial().equals(destLoc.getMaterial())) {
												previousTransferRequested.setSourceLoc(prevDestLoc);
												previousTransferRequested.setDestLoc(destLoc);
												
												requestTransfer(userRequest, null, carrierId, previousTransferRequested);
											} else {
												
											}
										} else {
											// Waiting
											; /*NULL*/
										}
									} else {
										
									}
								} else {
									switch (trCmd.getDetailState()) {
										case UNLOADED:
										case LOAD_ASSIGNED:
										{
											if (prevSourceLoc.equals(prevDestLoc) == false) {
												if (carrierId.equals(carrierWithCarrierLocMap.get(prevSourceLoc))) {
													releaseFromOccupiedCarrierLoc(prevSourceLoc);
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
					} else {
						TrCmd trCmd = trCmdWithCarrierIdMap.get(carrierId);
						if (trCmd == null) {
							CarrierLoc sourceLoc = (CarrierLoc)userRequest.getTourListItem(0);
							int index = getIndexForRandomTransfer(userRequest, sourceLoc, userRequest.getTourListSize());
							CarrierLoc destLoc = (CarrierLoc)userRequest.getTourListItem(index);
							if (sourceLoc != null && destLoc != null) {
								if (userRequest.getTourListSize() == 1 || occupiedCarrierLocList.contains(destLoc) == false) {
									requestTransfer(userRequest, null, carrierId, new PreviousTransferRequest(sourceLoc, destLoc));
								}
							} else {
								traceLongRunAbnormal("processRandomTransfer() - sourceLoc or destLoc is null.");
								disableUserRequest(userRequest);
							}
						}
					}
				}
				removeKeys.remove(userRequest);
			}
		} catch (Exception e) {
			traceLongRunException("processRandomTransfer()", e);
		}
	}
	
	private int getIndexForRandomTransfer(UserRequest userRequest, CarrierLoc currentLoc, int tourListSize) {
		try {
			if (userRequest != null) {
				ArrayList<CarrierLoc> tempTourList = new ArrayList<CarrierLoc>();
				CarrierLoc carrierLoc = null;
				for (int i = 0; i < tourListSize; i++) {
					carrierLoc = (CarrierLoc)userRequest.getTourListItem(i);
					if (carrierLoc != null && occupiedCarrierLocList.contains(carrierLoc) == false) {
						tempTourList.add(carrierLoc);
					}
				}
				if (tempTourList.contains(currentLoc) == false) {
					tempTourList.add(currentLoc);
				}
				Random random = new Random(System.currentTimeMillis());
				int index = random.nextInt(tempTourList.size());
				carrierLoc = tempTourList.get(index);
				index = userRequest.getTourListIndexOf(carrierLoc);
				if (index > -1) {
					return index;
				}
				if (currentLoc != null) {
					return userRequest.getTourListIndexOf(currentLoc);
				}
			}
		} catch (Exception e) {
			traceLongRunException("getDestLocForRandomTransfer()", e);
		}
		return 0;
	}
	
	private void processPatrol(UserRequest userRequest, Set<UserRequest> removeKeys) {
		try {
			if (userRequest != null) {
				Vehicle vehicle = vehicleMap.get(userRequest.getVehicleId());
				if (vehicle != null) {
					if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
						// 2015.12.21 by KBS : Job Pause 일 경우 Patrol TrCmd가 한번에 생기는 문제 
//						if (vehicle.isAssignHold() || idleVehicleList.contains(vehicle)) {
						if (idleVehicleList.contains(vehicle)) {
							if (previousRequestedMap.containsKey(userRequest)) {
								PreviousPatrolRequest previousPatrolRequested = (PreviousPatrolRequest) previousRequestedMap.get(userRequest);
								if (previousPatrolRequested != null) {
									int prevIndex = previousPatrolRequested.getIndex();
									if (vehicle.getStopNode().equals(vehicle.getTargetNode())) {
										TrCmd trCmd = assignedTrCmdMap.get(vehicle);
										if (prevIndex == userRequest.getTourListSize() - 1) {
											if (trCmd == null) {
												// Tour List의 마지막을 수행 완료 한 경우
												userRequestManager.updateLastTouredTime(userRequest.getUserRequestId());
												
												StringBuilder message = new StringBuilder();
												message.append(userRequest.getUserRequestId()).append("> ");
												message.append("PatrolRequest Completed.");
												traceLongRunMain(message.toString());
												
												disableUserRequest(userRequest);
											}
										} else {
											int index = (prevIndex + 1) % userRequest.getTourListSize();
											Path path = null;
											for (int i = index; i < userRequest.getTourListSize(); i++) {
												path = (Path)userRequest.getTourListItem(i);
												if (path != null) {
													String startNodeId = path.getStartNodeId();
													if (isMovableTo(vehicle, vehicle.getStopNode(), startNodeId)) {
														String endNodeId = path.getEndNodeId();
														if (isMovableTo(vehicle, startNodeId, endNodeId)) {
															previousPatrolRequested.setIndex(i);
															previousPatrolRequested.setPath(path);
															previousPatrolRequested.setCostSearchChecked(false);
															
															requestPatrolToVehicle(userRequest, vehicle, previousPatrolRequested);
															break;
														} else {
															if (previousPatrolRequested.isCostSearchChecked() == false) {
																StringBuilder message = new StringBuilder();
																message.append(userRequest.getUserRequestId()).append("> ");
																message.append("PatrolRequest - SearchFailed from a StartNode (").append(startNodeId).append(")");
																message.append(" to a EndNode (").append(endNodeId).append(").");
																message.append(" (").append(i + 1).append("/").append(userRequest.getTourListSize()).append(")");
																traceLongRunAbnormal(message.toString());
															}
														}
													} else {
														if (previousPatrolRequested.isCostSearchChecked() == false) {
															StringBuilder message = new StringBuilder();
															message.append(userRequest.getUserRequestId()).append("> ");
															message.append("PatrolRequest - SearchFailed from a Vehicle's StopNode (").append(vehicle.getStopNode()).append(")");
															message.append(" to a StartNode (").append(startNodeId).append(").");
															message.append(" (").append(i + 1).append("/").append(userRequest.getTourListSize()).append(")");
															traceLongRunAbnormal(message.toString());
														}
													}
												}
												if (i == userRequest.getTourListSize() - 1) {
													previousPatrolRequested.setCostSearchChecked(true);
												}
											}
										}
									} else {
										// Waiting
										; /*NULL*/
									}
								} else {
									traceLongRunAbnormal("previousPatrolRequested is null.");
									disableUserRequest(userRequest);
								}
							} else {
								if (vehicle.getStopNode().equals(vehicle.getTargetNode())) {
									TrCmd trCmd = assignedTrCmdMap.get(vehicle);
									if (trCmd == null) {
										int index = 0;
										Path path = (Path)userRequest.getTourListItem(0);
										for (int i = index; i < userRequest.getTourListSize(); i++) {
											path = (Path)userRequest.getTourListItem(i);
											if (path != null) {
												String startNodeId = path.getStartNodeId();
												if (isMovableTo(vehicle, vehicle.getStopNode(), startNodeId)) {
													String endNodeId = path.getEndNodeId();
													if (isMovableTo(vehicle, startNodeId, endNodeId)) {
														requestPatrolToVehicle(userRequest, vehicle, new PreviousPatrolRequest(i, path));
														break;
													} else {
														StringBuilder message = new StringBuilder();
														message.append(userRequest.getUserRequestId()).append("> ");
														message.append("PatrolRequest - SearchFailed from a StartNode (").append(startNodeId).append(")");
														message.append(" to a EndNode (").append(endNodeId).append(").");
														message.append(" (").append(i + 1).append("/").append(userRequest.getTourListSize()).append(")");
														traceLongRunAbnormal(message.toString());
													}
												} else {
													StringBuilder message = new StringBuilder();
													message.append(userRequest.getUserRequestId()).append("> ");
													message.append("PatrolRequest - SearchFailed from a Vehicle's StopNode (").append(vehicle.getStopNode()).append(")");
													message.append(" to a StartNode (").append(startNodeId).append(").");
													message.append(" (").append(i + 1).append("/").append(userRequest.getTourListSize()).append(")");
													traceLongRunAbnormal(message.toString());
												}
											}
										}
									}
								}
							}
						} else {
							// Waiting
							; /*NULL*/
						}
					} else {
						// Waiting
						; /*NULL*/
					}
					removeKeys.remove(userRequest);
				} else {
					traceLongRunAbnormal(userRequest.getVehicleId() + " is null.");
					disableUserRequest(userRequest);
				}
			}
		} catch (Exception e) {
			traceLongRunException("processPatrol()", e);
		}
	}
	
	private void processVibration(UserRequest userRequest, Set<UserRequest> removeKeys) {
		try {
			if (userRequest != null) {
				TrCmd trCmd = trCmdWithCarrierIdMap.get(userRequest.getCarrierId());
				if (previousRequestedMap.containsKey(userRequest)) {
					PreviousVibrationRequest previousVibrationRequested = (PreviousVibrationRequest) previousRequestedMap.get(userRequest);
					if (previousVibrationRequested != null) {
						if (trCmd != null) {
							if (controledVibrationTrCmdList.contains(trCmd) == false) {
								controledVibrationTrCmdList.add(trCmd);
							}
							
							if (trCmd.getDetailState() == TRCMD_DETAILSTATE.VIBRATION_MONITORING) {
								Vehicle vehicle = vehicleMap.get(trCmd.getVehicle());
								if (vehicle != null) {
									if (vehicle.isAssignHold()) {
										vehicleManager.setAssignHoldToDB(vehicle, false);
									}
									// 2014.03.05 by KYK : ??
//									if (vehicle.getStopNode().equals(vehicle.getTargetNode())) {
									if (vehicle.getStopNode().equals(vehicle.getTargetNode()) && vehicle.getRequestedType() == REQUESTEDTYPE.NULL) {
										int prevIndex = previousVibrationRequested.getIndex();
										String prevNode = previousVibrationRequested.getNodeId();
										if (prevIndex == userRequest.getTourListSize() - 1) {
											userRequestManager.updateLastTouredTime(userRequest.getUserRequestId());
											previousRequestedMap.remove(userRequest);
											trCmdManager.updateTrCmdChangedInfoToDB(trCmd.getTrCmdId(), VIBRATIONCHANGE, "");
											
											StringBuilder message = new StringBuilder();
											message.append(userRequest.getUserRequestId()).append("> ");
											message.append("VibrationRequest Completed.");
											traceLongRunMain(message.toString());
										} else {
											if (vehicle.getTargetNode().equals(prevNode)) {
												int index = (prevIndex + 1) % userRequest.getTourListSize();
												String nodeId = "";
												for (int i = index; i < userRequest.getTourListSize(); i++) {
													nodeId = (String)userRequest.getTourListItem(i);
													if (isMovableTo(vehicle, vehicle.getStopNode(), nodeId)) {
														previousVibrationRequested.setIndex(i);
														previousVibrationRequested.setNodeId(nodeId);
														previousVibrationRequested.setCostSearchChecked(false);
														requestVibrationMonitorToVehicle(userRequest, vehicle, previousVibrationRequested);
														break;
													} else {
														if (previousVibrationRequested.isCostSearchChecked() == false) {
															StringBuilder message = new StringBuilder();
															message.append(userRequest.getUserRequestId()).append("> ");
															message.append("VibrationRequest - SearchFailed to ");
															message.append(nodeId).append(" (").append(i + 1).append("/").append(userRequest.getTourListSize()).append(")");
															traceLongRunAbnormal(message.toString());
														}
														if (i == userRequest.getTourListSize() - 1) {
															previousVibrationRequested.setCostSearchChecked(true);
														}
													}
												}
											} else {
												if (isMovableTo(vehicle, vehicle.getStopNode(), prevNode)) {
													locateVehicle(vehicle, prevNode);
												}
											}
										}
									} else {
										
									}
								}
							} else {
								
							}
						} else {
							previousRequestedMap.remove(userRequest);
							// Unload PathSearch Fail되는 경우.
//							traceLongrunAbnormal("processVibration() - previousMoveRequested is NOT null. But, trCmd is null.");
						}
					} else {
						previousRequestedMap.remove(userRequest);
						traceLongRunAbnormal("processVibration() - previousMoveRequested is null.");
					}
				} else {
					CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(userRequest.getDockingStationId());
					if (carrierLoc != null) {
						if (trCmd == null) {
							DockingStation dockingStation = dockingStationManager.getDockingStationData(userRequest.getDockingStationId());
							if (dockingStation != null) {
								if (dockingStation.isEnabled()) {
									if (dockingStation.getStationMode() == 'A') {
										if (dockingStation.getAlarmId() == 0) {
											if (dockingStation.getCarrierExist() == 1) {
												if (dockingStation.getCarrierCharged() == 1) {
													String vehicleId = userRequest.getVehicleId();
													
													String carrierId = dockingStation.getCarrierId();
													if (carrierId == null || carrierId.length() == 0) {
														carrierId = getVibrationCarrierId(carrierLoc.getCarrierLocId());
													}
													userRequest.setCarrierId(carrierId);
													
													if (vehicleId == null || vehicleId.length() == 0) {
														// ANY Vehicle Type.
														requestVibrationToVehicle(userRequest, null, carrierLoc, new PreviousVibrationRequest(-1, carrierLoc.getNode()));
													} else {
														// Defined Vehicle Type.
														Vehicle vehicle = vehicleMap.get(userRequest.getVehicleId());
														if (vehicle != null) {
															TrCmd assignedTrCmd = assignedTrCmdMap.get(vehicle);
															if (assignedTrCmd == null) {
																if (vehicle.getStopNode().equals(vehicle.getTargetNode())) {
																	if (isMovableTo(vehicle, vehicle.getStopNode(), carrierLoc.getNode())) {
																		requestVibrationToVehicle(userRequest, vehicle, carrierLoc, new PreviousVibrationRequest(-1, carrierLoc.getNode()));
																	} else {
																		StringBuilder message = new StringBuilder();
																		message.append(userRequest.getUserRequestId()).append("> ");
																		message.append("VibrationRequest - SearchFailed to the DockingStation(");
																		message.append(userRequest.getDockingStationId()).append(":");
																		message.append(carrierLoc.getNode()).append(").");
																		traceLongRunAbnormal(message.toString());
																	}
																} else {
																	// Waiting
																	; /*NULL*/
																}
															} else {
																// 다른 작업을 수행 중인 경우.
																if (vehicle.isAssignHold() == false) {
																	vehicleManager.setAssignHoldToDB(vehicle, true);
																} else {
																	// Waiting
																	; /*NULL*/
																}
															}
														} else {
															// Waiting
															; /*NULL*/
														}
													}
												} else {
													// Waiting
													; /*NULL*/
												}
											} else {
												traceLongRunAbnormal("processVibration() - There is NOT a Carrier on the DockingStation.");
											}
										} else {
											traceLongRunAbnormal("processVibration() - dockingStation has AlarmCode.");
										}
									} else {
										traceLongRunAbnormal("processVibration() - dockingStation is Manual.");
									}
								} else {
									traceLongRunAbnormal("processVibration() - dockingStation is disabled.");
								}
							} else {
								traceLongRunAbnormal("processVibration() - dockingStation is null.");
							}
						} else {
							if (trCmd.getDetailState() == TRCMD_DETAILSTATE.VIBRATION_MONITORING) {
								trCmdManager.updateTrCmdChangedInfoToDB(trCmd.getTrCmdId(), VIBRATIONCHANGE, "");
								Vehicle vehicle = vehicleMap.get(trCmd.getVehicle());
								if (vehicle != null) {
									if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.VIBRATION && vehicle.isAssignHold()) {
										vehicleManager.setAssignHoldToDB(vehicle, false);
									}
								}
								
								StringBuilder message = new StringBuilder();
								message.append(trCmd.getTrCmdId()).append("> ");
								message.append("Vibration Changed.");
								traceLongRunMain(message.toString());
							} else if (trCmd.getState() == TRCMD_STATE.CMD_WAITING) {
								Vehicle vehicle = vehicleMap.get(trCmd.getVehicle());
								if (vehicle != null) {
									requestVibrationToVehicle(userRequest, vehicle, carrierLoc, new PreviousVibrationRequest(-1, carrierLoc.getNode()));
								}
							}
						}
					}
				}
				removeKeys.remove(userRequest);
			}
		} catch (Exception e) {
			traceLongRunException("processVibration()", e);
		}
	}
	
	
	private boolean requestMoveToVehicle(UserRequest userRequest, Vehicle vehicle, PreviousMoveRequest previousMoveRequest) {
		if (userRequest != null) {
			StringBuilder message = new StringBuilder();
			message.append(userRequest.getUserRequestId()).append("> ");
			if (vehicle != null) {
				if (previousMoveRequest != null) {
					if (previousMoveRequest.getIndex() > 0 ||
							(vehicle.getState() == 'A' || vehicle.getState() == 'I' || vehicle.getState() == 'O' || vehicle.getState() == 'F' || vehicle.getState() == 'D')) {
						if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
							if (isMovableTo(vehicle, vehicle.getStopNode(), previousMoveRequest.getNodeId())) {
								moveVehicle(vehicle, previousMoveRequest.getNodeId());
								previousRequestedMap.put(userRequest, previousMoveRequest);
								
								message.append("Move Requested ");
								message.append("(").append(previousMoveRequest.getIndex() + 1).append("/").append(userRequest.getTourListSize()).append("). ");
								message.append("VehicleId:").append(vehicle.getVehicleId()).append(" ");
								message.append("to ").append(previousMoveRequest.getNodeId());
								traceLongRunMain(message.toString());
							} else {
								// Waiting
								; /*NULL*/
							}
						} else {
							// Waiting
							; /*NULL*/
						}
					}
					return true;
				} else {
					traceLongRunAbnormal("requestMoveToVehicle() - previousMoveRequest is null.");
				}
			} else {
				traceLongRunAbnormal("requestMoveToVehicle() - vehicle is null.");
			}
			message.append("Move Request Failed. ");
			message.append("VehicleId:").append(vehicle.getVehicleId()).append(" ");
			traceLongRunMain(message.toString());
		} else {
			traceLongRunAbnormal("requestMoveToVehicle() - userRequest is null.");
		}
		return false;
	}
	
	private boolean requestVibrationMonitorToVehicle(UserRequest userRequest, Vehicle vehicle, PreviousVibrationRequest previousVibrationRequest) {
		if (userRequest != null) {
			StringBuilder message = new StringBuilder();
			message.append(userRequest.getUserRequestId()).append("> ");
			if (vehicle != null) {
				if (previousVibrationRequest != null) {
					if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
						locateVehicle(vehicle, previousVibrationRequest.getNodeId());
						previousRequestedMap.put(userRequest, previousVibrationRequest);
						
						message.append("VibrationMonitor Requested ");
						message.append("(").append(previousVibrationRequest.getIndex() + 1).append("/").append(userRequest.getTourListSize()).append("). ");
						message.append("VehicleId:").append(vehicle.getVehicleId()).append(" ");
						message.append("to ").append(previousVibrationRequest.getNodeId());
						traceLongRunMain(message.toString());
					} else {
						// Waiting
						; /*NULL*/
					}
					return true;
				} else {
					traceLongRunAbnormal("requestVibrationMonitorToVehicle() - previousVibrationRequest is null.");
				}
			} else {
				traceLongRunAbnormal("requestVibrationMonitorToVehicle() - vehicle is null.");
			}
			message.append("VibrationMonitor Request Failed. ");
			message.append("VehicleId:").append(vehicle.getVehicleId()).append(" ");
			traceLongRunMain(message.toString());
		} else {
			traceLongRunAbnormal("requestVibrationMonitorToVehicle() - userRequest is null.");
		}
		return false;
	}
	
	private void disableUserRequest(UserRequest userRequest) {
		try {
			if (userRequest != null) {
				if (userRequest.getType() == USERREQUEST_TYPE.VIBRATION) {
					TrCmd trCmd = trCmdWithCarrierIdMap.get(getVibrationCarrierId(userRequest.getDockingStationId()));
					if (trCmd != null) {
						if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
							cancelVibrationTrCmd(trCmd);
						}
					}
				}
				StringBuilder message = new StringBuilder();
				message.append(userRequest.getUserRequestId()).append("> ");
				message.append("Disabled. Type:").append(userRequest.getTypeString());
				traceLongRunMain(message.toString());
				
				previousRequestedMap.remove(userRequest);
				userRequestManager.disableUserRequest(userRequest.getUserRequestId());
				userRequest.disable();
			} else {
				traceLongRunAbnormal("disableUserRequest() - userRequest is null.");
			}
		} catch (Exception e) {
			traceLongRunException("disableUserRequest()", e);
		}
	}
	
	private void cancelVibrationTrCmd(TrCmd trCmd) {
		try {
			if (trCmd != null) {
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
					Vehicle vehicle = vehicleMap.get(trCmd.getAssignedVehicleId());
					if (vehicle != null) {
						switch (trCmd.getDetailState()) {
							case NOT_ASSIGNED:
							case UNLOAD_ASSIGNED:
							{
								trCmdManager.updateTrCmdChangedInfoToDB(trCmd.getTrCmdId(), REMOVE, "");
								if (vehicle.isAssignHold()) {
									vehicleManager.setAssignHoldToDB(vehicle, false);
								}
								
								StringBuilder message = new StringBuilder();
								message.append(trCmd.getTrCmdId()).append("> ");
								message.append("Canceled.");
								traceLongRunMain(message.toString());
								break;
							}
							case UNLOAD_SENT:
							case UNLOAD_ACCEPTED:
							case UNLOADING:
							case VIBRATION_MONITORING:
							{
								trCmdManager.updateTrCmdChangedInfoToDB(trCmd.getTrCmdId(), VIBRATIONCHANGE, "");
								if (vehicle.isAssignHold()) {
									vehicleManager.setAssignHoldToDB(vehicle, false);
								}
								
								StringBuilder message = new StringBuilder();
								message.append(trCmd.getTrCmdId()).append("> ");
								message.append("VibrationChanged.");
								traceLongRunMain(message.toString());
								break;
							}
							default:
								break;
						}
					}
				}
			}
		} catch (Exception e) {
			traceLongRunException("disableVibrationRequest()", e);
		}
	}
	
	private boolean isMovableTo(Vehicle vehicle, String fromNodeId, String toNodeId) {
		if (vehicle != null && fromNodeId != null && toNodeId != null) {
			Node fromNode = nodeManager.getNode(fromNodeId);
			Node toNode = nodeManager.getNode(toNodeId);
			if (fromNode != null && toNode != null) {
				if (fromNode.equals(toNode) ||
						costSearch.costSearch(vehicle, fromNode, toNode, MAXCOST_TIMEBASE, costSearchOption) < MAXCOST_TIMEBASE){
					Hid toNodeHid = toNode.getHid();
					if (toNodeHid != null && toNodeHid.isAbnormalState() == false) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private boolean moveVehicle(Vehicle vehicle, String nodeId) {
		try {
			if (vehicle != null) {
				if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
					if (vehicle.getTargetNode().equals(nodeId) == false) {
						if (vehicleManager.updateVehicleRequestedInfoToDB(MOVE, nodeId, vehicle.getVehicleId())) {
							return true;
						}
					}
				} 
			} else {
				traceLongRunAbnormal("requestMoveToVehicle(). vehicle is null.");
			}
		} catch (Exception e) {
			traceLongRunException("requestMoveToVehicle()", e);
		}
		return false;
	}
	
	private void locateVehicle(Vehicle vehicle, String nodeId) {
		try {
			if (vehicle != null) {
				if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
					if (vehicle.getTargetNode().equals(nodeId) == false) {
						vehicleManager.updateVehicleRequestedInfoToDB(LOCATE, nodeId, vehicle.getVehicleId());
					}
				}
			} else {
				traceLongRunAbnormal("locateVehicle(). vehicle is null.");
			}
		} catch (Exception e) {
			traceLongRunException("locateVehicle()", e);
		}
	}
	
	private boolean requestTransfer(UserRequest userRequest, Vehicle vehicle, String carrierId, PreviousTransferRequest previousTransferRequest) {
		try {
			if (userRequest != null) {
				if (userRequest.getType() == USERREQUEST_TYPE.RANDOM_TRANSFER || userRequest.getType() == USERREQUEST_TYPE.FIXED_TRANSFER) {
					if (previousTransferRequest != null) {
						if (vehicle == null || (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0)) {
							CarrierLoc sourceLoc = previousTransferRequest.getSourceLoc();
							CarrierLoc destLoc = previousTransferRequest.getDestLoc();
							if (sourceLoc != null && destLoc != null) {
								String vehicleId = "";
								if (vehicle != null && userRequest.getType() == USERREQUEST_TYPE.FIXED_TRANSFER) {
									vehicleId = vehicle.getVehicleId();
								}
								if (trCmdManager.createTransferRequestToDB(getTrCmdPrefix(userRequest), vehicleId, carrierId, sourceLoc.getCarrierLocId(), destLoc.getCarrierLocId(), sourceLoc.getNode(), destLoc.getNode(), userRequest.isLoadingByPass())) {
									if (userRequest.getTourListSize() > 1) {
										previousRequestedMap.put(userRequest, previousTransferRequest);
									}
									
									StringBuilder message = new StringBuilder();
									message.append(userRequest.getUserRequestId()).append("> ");
									
									if (userRequest.getType() == USERREQUEST_TYPE.RANDOM_TRANSFER) {
										message.append("RandomTransfer Requested. ");
										registerToOccupiedCarrierLoc(sourceLoc, carrierId);
										registerToOccupiedCarrierLoc(destLoc, carrierId);
									} else {
										message.append("FixedTransfer Requested ");
										message.append("(").append(previousTransferRequest.getIndex() + 1).append("/").append(userRequest.getTourListSize()).append("). ");
										message.append("VehicleId:").append(vehicle.getVehicleId()).append(" ");
									}
									message.append("(").append(sourceLoc.getCarrierLocId()).append(":").append(sourceLoc.getNode());
									message.append(" -> ");
									message.append(destLoc.getCarrierLocId()).append(":").append(destLoc.getNode()).append(")");
									traceLongRunMain(message.toString());
									return true;
								}
							}
						}
					}
				} else {
					StringBuilder message = new StringBuilder();
					message.append(userRequest.getUserRequestId()).append("> ");
					message.append("TRANSFER is NOT allowed. ");
					message.append(", UserRequestType:").append(userRequest.getTypeString());
					traceLongRunAbnormal(message.toString());
				}
				StringBuilder message = new StringBuilder();
				message.append(userRequest.getUserRequestId()).append("> ");
				
				if (userRequest.getType() == USERREQUEST_TYPE.RANDOM_TRANSFER) {
					message.append("RandomTransfer Request Failed. ");
				} else {
					message.append("FixedTransfer Request Failed. ");
					message.append("VehicleId:").append(vehicle.getVehicleId());
				}
				traceLongRunMain(message.toString());
			} else {
				traceLongRunAbnormal("requestTransfer() - userRequest is null.");
			}
		} catch (Exception e) {
			traceLongRunException("requestTransfer()", e);
		}
		
		
		return false;
	}

	private String getTrCmdPrefix(UserRequest userRequest) {
		if (userRequest != null) {
			return TRCMDID_PREFIX + userRequest.getUserRequestId() + "_";
		} else {
			return TRCMDID_PREFIX;
		}
	}
	
	private String getCarrierId(String requestId) {
		if (requestId != null) {
			return requestId;
		} else {
			return CARRIERID_PREFIX;
		}
	}
	
	private void requestPatrolToVehicle(UserRequest userRequest, Vehicle vehicle, PreviousPatrolRequest previousPatrolRequest) {
		try {
			if (userRequest != null) {
				if (userRequest.getType() == USERREQUEST_TYPE.PATROL) {
					if (previousPatrolRequest != null) {
						if (vehicle != null) {
							if (PATROL_VEHICLEZONE.equals(vehicle.getZone())) {
								if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
									Path path = previousPatrolRequest.getPath();
									if (path != null) {
										String trCmdId = "PatrolCmd_" + vehicle.getVehicleId() + "_";
										String carrierId = "PatrolCarrier_" + vehicle.getVehicleId() + "_";
										String startNodeId = path.getStartNodeId();
										String endNodeId = path.getEndNodeId();
										if (startNodeId != null && startNodeId.length() > 0 &&
												endNodeId != null && endNodeId.length() > 0) {
											
											StringBuilder message = new StringBuilder();
											message.append(userRequest.getUserRequestId()).append("> ");
											if (trCmdManager.createPatrolRequestToDB(trCmdId, vehicle.getVehicleId(), carrierId, startNodeId, endNodeId, userRequest.getUserRequestId(), userRequest.getPatrolMode())) {
												previousRequestedMap.put(userRequest, previousPatrolRequest);
												
												message.append("Patrol Requested ");
												message.append("(").append(previousPatrolRequest.getIndex() + 1).append("/").append(userRequest.getTourListSize()).append("). ");
												message.append("VehicleId:").append(vehicle.getVehicleId()).append(" ");
												message.append("(").append(startNodeId).append(" -> ").append(endNodeId).append(")");
											} else {
												
											}
											traceLongRunMain(message.toString());
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			traceLongRunException("requestPatrolToVehicle()", e);
		}
	}
	
	private void requestVibrationToVehicle(UserRequest userRequest, Vehicle vehicle, CarrierLoc carrierLoc, PreviousVibrationRequest previousVibrationRequest) {
		try {
			if (userRequest != null) {
				if (carrierLoc != null) {
					String trCmdId = "V_" + userRequest.getUserRequestId() + "_";
					String carrierId = userRequest.getCarrierId();
					TrCmd trCmd = trCmdWithCarrierIdMap.get(carrierId);
					
					StringBuilder message = new StringBuilder();
					message.append(userRequest.getUserRequestId()).append("> ");
					
					if (trCmd == null) {
						message.append("Vibration Requested. ");
						if (vehicle == null) {
							trCmdManager.createVibrationRequestToDB(trCmdId, "", carrierId, carrierLoc);
							previousRequestedMap.put(userRequest, previousVibrationRequest);
							
							message.append("ANY Vehicle, ");
							message.append("DockingStationId:").append(carrierLoc.getCarrierLocId()).append(", ");
							message.append("Node:").append(carrierLoc.getNode()).append(" ");
							traceLongRunMain(message.toString());
						} else {
							if (vehicle.getVehicleMode() == 'A' && vehicle.getErrorCode() == 0) {
								trCmdManager.createVibrationRequestToDB(trCmdId, vehicle.getVehicleId(), carrierId, carrierLoc);
								previousRequestedMap.put(userRequest, previousVibrationRequest);
								
								message.append("VehicleId:").append(vehicle.getVehicleId()).append(", ");
								message.append("DockingStationId:").append(carrierLoc.getCarrierLocId()).append(", ");
								message.append("Node:").append(carrierLoc.getNode()).append(" ");
								traceLongRunMain(message.toString());
							}
						}
					} else {
						message.append("Vibration Request Failed. - Duplicated TrCmd. PrevTrCmdId:").append(trCmd.getTrCmdId());
						traceLongRunMain(message.toString());
					}
				}
			}
		} catch (Exception e) {
			traceLongRunException("requestVibrationToVehicle()", e);
		}
	}
	
	private String getVibrationCarrierId(String dockingStation) {
		if (dockingStation != null) {
			return "AVS300_" + dockingStation;
		}
		return "AVS300";
	}
	
	private void getVehiclesAndTrCmds() {
		try {
			getVehicleList();
			getTrCmdList();
		} catch (Exception e) {
			traceLongRunException("getVehiclesAndTrCmds()", e);
		}
	}
	private void getVehicleList() {
		try {
			vehicleMap.clear();
			idleVehicleList.clear();
			
			nodeManager.resetAbnormalNodeIdList();
			
			ConcurrentHashMap<String, Object> tempVehicleList = vehicleManager.getData(); 
			Set<String> searchKeys = new HashSet<String>(tempVehicleList.keySet());
			Vehicle vehicle = null;
			int alarmCode = 0;
			for (String searchKey : searchKeys) {
				vehicle = (Vehicle)tempVehicleList.get(searchKey);
				if (vehicle != null) {
					if (vehicle.isEnabled()) {
						//Enabled Vehicles
						vehicleMap.put(searchKey, vehicle);
						
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
									
									if (idleVehicleList.contains(vehicle) == false) {
										idleVehicleList.add(vehicle);
									}
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
		} catch (Exception e) {
			traceLongRunException("getVehicleList()", e);
		}
	}
	
	private void getTrCmdList() {
		assignedVehicleList.clear();
		assignedTrCmdMap.clear();
		trCmdWithCarrierIdMap.clear();
		
		trCmdMap = trCmdManager.getData();

		Set<String> searchKeys = new HashSet<String>(trCmdMap.keySet());
		TrCmd trCmd = null;
		Vehicle vehicle = null;
		String carrierId = null;
		String assignedVehicleId = null;
		String vehicleId = null;
		
		for (String searchKey : searchKeys) {
			trCmd = (TrCmd)trCmdMap.get(searchKey);
			if (trCmd != null) {
				carrierId = trCmd.getCarrierId();
				if (carrierId != null && carrierId.length() > 0) {
					trCmdWithCarrierIdMap.put(carrierId, trCmd);
				}
				assignedVehicleId = trCmd.getAssignedVehicleId();
				vehicleId = trCmd.getVehicle();
				if (assignedVehicleId != null && assignedVehicleId.length() > 0) {
					if (vehicleId != null && vehicleId.length() > 0) {
						vehicle = vehicleMap.get(vehicleId);
						if (vehicle != null) {
							if (assignedVehicleList.contains(vehicle) == false) {
								assignedVehicleList.add(vehicle);
							}
							assignedTrCmdMap.put(vehicle, trCmd);
							idleVehicleList.remove(vehicle);
						}
					} else {
						vehicle = vehicleMap.get(assignedVehicleId);
						if (vehicle != null) {
							if (assignedVehicleList.contains(vehicle) == false) {
								assignedVehicleList.add(vehicle);
							}
							if (assignedTrCmdMap.containsKey(vehicle) == false) {
								assignedTrCmdMap.put(vehicle, trCmd);
							}
							idleVehicleList.remove(vehicle);
						}
					}
				}
			}
		}
	}
	
	private void getUserRequests() {
		initializeLists();
		
		checkUserRequestValidity();
		
		checkPatrolRequests();
		checkVibrationRequests();
		checkRandomTransferRequests();
		checkFixedTransferRequests();
		checkMoveRequests();
	}
	
	private void initializeLists() {
		userRequestList.clear();
		
		patrolRequestList.clear();
		vibrationRequestList.clear();
		fixedTransferRequestList.clear();
		randomTransferRequestList.clear();
		moveRequestList.clear();
		tempVehicleList.clear();
		vibrationRequestWithDockingStationMap.clear();
		controledVibrationTrCmdList.clear();

		userRequestManager.update();
		userRequestMap = userRequestManager.getData();
		tourManager.update();
	}
	
	private void checkUserRequestValidity() {
		try {
			UserRequest userRequest = null;
			Set<String> searchKeys = new HashSet<String>(userRequestMap.keySet());
			for (String searchKey1 : searchKeys) {
				userRequest = (UserRequest)userRequestMap.get(searchKey1);
				if (userRequest != null) {
					if (userRequest.isEnabled()) {
						switch (userRequest.getType()) {
							case PATROL:
							{
								if (isPatrolControlUsed == false) {
									StringBuilder message2 = new StringBuilder();
									message2.append("UserRequestId:").append(userRequest.getUserRequestId());
									message2.append(", Type:Patrol. PatrolControlUsage:NO");
									traceLongRunMain(message2.toString());
									disableUserRequest(userRequest);
								}
								break;
							}
							case VIBRATION:
							{
								if (isVibrationControlUsed == false) {
									StringBuilder message2 = new StringBuilder();
									message2.append("UserRequestId:").append(userRequest.getUserRequestId());
									message2.append(", Type:Vibration. VibrationControlUsed:NO");
									traceLongRunMain(message2.toString());
									disableUserRequest(userRequest);
								}
								break;
							}
							case RANDOM_TRANSFER:
							{
								if (isLongRunTransferUsed == false) {
									StringBuilder message2 = new StringBuilder();
									message2.append("UserRequestId:").append(userRequest.getUserRequestId());
									message2.append(", Type:RandomTransfer. LongRunTransferUsed:NO");
									traceLongRunMain(message2.toString());
									disableUserRequest(userRequest);
								}
								break;
							}
							case FIXED_TRANSFER:
							{
								if (isLongRunTransferUsed == false) {
									StringBuilder message2 = new StringBuilder();
									message2.append("UserRequestId:").append(userRequest.getUserRequestId());
									message2.append(", Type:FixedTransfer. LongRunTransferUsed:NO");
									traceLongRunMain(message2.toString());
									disableUserRequest(userRequest);
								}
								break;
							}
							case MOVE:
							{
								if (isLongRunMoveUsed == false) {
									StringBuilder message2 = new StringBuilder();
									message2.append("UserRequestId:").append(userRequest.getUserRequestId());
									message2.append(", Type:Move. LongRunMoveUsed:NO");
									traceLongRunMain(message2.toString());
									disableUserRequest(userRequest);
								}
								break;
							}
							default:
								break;
						}
					}
				}
			}
			for (String searchKey : searchKeys) {
				userRequest = (UserRequest)userRequestMap.get(searchKey);
				if (userRequest != null) {
					if (userRequest.isEnabled()) {
						if (validRequestList.contains(userRequest) || isValidUserRequest(userRequest)) {
							if (validRequestList.contains(userRequest) == false) {
								validRequestList.add(userRequest);
							}
							preprocessUserRequest(userRequest);
						} else {
							StringBuilder message = new StringBuilder();
							message.append("Invalid UserRequest. ");
							message.append("UserRequestId:").append(userRequest.getUserRequestId());
							message.append(", Type:").append(userRequest.getTypeString());
							message.append(", VehicleId:").append(userRequest.getVehicleId());
							message.append(", Tourlist:").append(userRequest.getTourListString());
							message.append(", Cycle:").append(userRequest.getCycle());
							message.append(", CycleUnit:").append(userRequest.getCycleUnitString());
							message.append(", DockingStation:").append(userRequest.getDockingStationId());
							if (userRequest.isLoadingByPass()) {
								message.append(", LoadingByPass:TRUE");
							} else {
								message.append(", LoadingByPass:FALSE");
							}
							
							message.append(", LastTouredTime:").append(userRequest.getLastTouredTime());
							traceLongRunAbnormal(message.toString());
							
							disableUserRequest(userRequest);
						}
					}
				} else {
					traceLongRunAbnormal("checkUserRequestValidity() - UserRequest is null. UserRequestId:" + searchKey);
				}
			}
		} catch (Exception e) {
			traceLongRunAbnormal("checkUserRequestValidity()");
		}
	}
	
	private void checkPatrolRequests() {
		try {
			// Enabled UserRequest에 대해 Duplicated Vehicle을 허용하지 않음. -> Disable 시킴.
			Vehicle vehicle = null;
			for (UserRequest userRequest : patrolRequestList) {
				if (userRequest != null) {
					if (isPatrolControlUsed) {
						vehicle = vehicleMap.get(userRequest.getVehicleId());
						if (vehicle != null) {
							if (previousRequestedMap.containsKey(userRequest)) {
								if (userRequestList.contains(userRequest) == false) {
									userRequestList.add(userRequest);
									if (tempVehicleList.contains(vehicle) == false) {
										tempVehicleList.add(vehicle);
									} else {
										traceLongRunAbnormal("checkVehicleForPatrolRequestList() - Duplicated tempVehicleList. VehicleId:" + userRequest.getVehicleId());
									}
								}
							}
						}
					} else {
						traceLongRunAbnormal("checkRandomTransferRequests() - PatrolControl Unused.");
						disableUserRequest(userRequest);
					}
				}
			}
			
			for (UserRequest userRequest : patrolRequestList) {
				if (userRequest != null) {
					vehicle = vehicleMap.get(userRequest.getVehicleId());
					if (vehicle != null) {
						if (userRequestList.contains(userRequest) == false) {
							if (tempVehicleList.contains(vehicle) == false) {
								userRequestList.add(userRequest);
								tempVehicleList.add(vehicle);
							} else {
								traceLongRunAbnormal("checkVehicleForPatrolRequestList() - Duplicated Vehicle. VehicleId:" + userRequest.getVehicleId());
								disableUserRequest(userRequest);
							}
						} else {
							// previousRequested
							; /*NULL*/
						}
					} else {
						traceLongRunAbnormal("checkVehicleForPatrolRequestList() - vehicle is null. VehicleId:" + userRequest.getVehicleId());
						disableUserRequest(userRequest);
					}
				} else {
					traceLongRunAbnormal("checkVehicleForPatrolRequestList() - userRequest is null.");
				}
			}
		} catch (Exception e) {
			traceLongRunException("checkVehicleForPatrolRequestList()", e);
		}
	}
	
	private void checkVibrationRequests() {
		try {
			// Check VIBRATION Request.
			// Enabled UserRequest에 대해 Duplicated Vehicle을 허용함. Check해서 하나만 수행하도록 함.
			Vehicle vehicle = null;
			for (UserRequest userRequest : vibrationRequestList) {
				if (userRequest != null) {
					if (isVibrationControlUsed) {
						if (userRequest.getVehicleId() != null && userRequest.getVehicleId().length() > 0) {
							vehicle = vehicleMap.get(userRequest.getVehicleId());
							if (vehicle != null) {
								if (previousRequestedMap.containsKey(userRequest)) {
									if (userRequestList.contains(userRequest) == false) {
										userRequestList.add(userRequest);
										if (tempVehicleList.contains(vehicle) == false) {
											tempVehicleList.add(vehicle);
										} else {
											traceLongRunAbnormal("checkVehicleForVibrationRequestList() - Duplicated tempVehicleList. VehicleId:" + userRequest.getVehicleId());
										}
									}
								}
							}
						}
					} else {
						traceLongRunAbnormal("checkRandomTransferRequests() - VibrationControl Unused.");
						disableUserRequest(userRequest);
					}
				}
			}
			
			for (UserRequest userRequest : vibrationRequestList) {
				if (userRequest != null) {
					if (userRequest.getVehicleId() == null || userRequest.getVehicleId().length() == 0) {
						// ANY Vehicle Type
						if (userRequestList.contains(userRequest) == false) {
							userRequestList.add(userRequest);
						}
					} else {
						vehicle = vehicleMap.get(userRequest.getVehicleId());
						if (vehicle != null) {
							if (userRequestList.contains(userRequest) == false) {
								if (tempVehicleList.contains(vehicle) == false) {
									userRequestList.add(userRequest);
									tempVehicleList.add(vehicle);
								} else {
									// VIBRATION은 Duplicated Vehicle 허용. Check해서 하나만 수행하도록 함.
									// Waiting.
									; /*NULL*/
								}
							} else {
								// previousRequested
								; /*NULL*/
							}
						} else {
							traceLongRunAbnormal("checkVehicleForVibrationRequestList() - vehicle is null. VehicleId:" + userRequest.getVehicleId());
							disableUserRequest(userRequest);
						}
					}
				} else {
					traceLongRunAbnormal("checkVehicleForVibrationRequestList() - userRequest is null.");
				}
			}
		} catch (Exception e) {
			traceLongRunException("checkVehicleForVibrationRequestList()", e);
		}
	}
	
	private void checkRandomTransferRequests() {
		try {
			// Check RANDOM_TRANSFER Request.
			for (UserRequest userRequest : randomTransferRequestList) {
				if (userRequest != null) {
					if (isLongRunTransferUsed) {
						if (isYieldSearchUsed) {
							if (userRequestList.contains(userRequest) == false) {
								userRequestList.add(userRequest);
							}
						} else {
							traceLongRunAbnormal("checkRandomTransferRequests() - YieldSearch Unused.");
							disableUserRequest(userRequest);
						}
					} else {
						traceLongRunAbnormal("checkRandomTransferRequests() - LongRunTransfer Unused.");
						disableUserRequest(userRequest);
					}
				} else {
					traceLongRunAbnormal("checkRandomTransferRequests() - userRequest is null.");
				}
			}
		} catch (Exception e) {
			traceLongRunException("checkRandomTransferRequests()", e);
		}
	}
	
	private void checkFixedTransferRequests() {
		try {
			// Check FIXED_TRANSFER Request.
			// Enabled UserRequest에 대해 Duplicated Vehicle을 허용하지 않음. -> Disable 시킴.
			Vehicle vehicle = null;
			for (UserRequest userRequest : fixedTransferRequestList) {
				if (userRequest != null) {
					if (isLongRunTransferUsed) {
						vehicle = vehicleMap.get(userRequest.getVehicleId());
						if (vehicle != null) {
							if (previousRequestedMap.containsKey(userRequest)) {
								if (tempVehicleList.contains(vehicle) == false) {
									if (userRequestList.contains(userRequest) == false) {
										userRequestList.add(userRequest);
										tempVehicleList.add(vehicle);
									}
								} else {
									traceLongRunAbnormal("checkVehicleForFixedTransferRequestList() - Duplicated tempVehicleList. VehicleId:" + userRequest.getVehicleId());
									disableUserRequest(userRequest);
								}
							}
						}
					} else {
						traceLongRunAbnormal("checkRandomTransferRequests() - LongRunTransfer Unused.");
						disableUserRequest(userRequest);
					}
				}
			}
			
			for (UserRequest userRequest : fixedTransferRequestList) {
				if (userRequest != null) {
					if (userRequest.isEnabled()) {
						vehicle = vehicleMap.get(userRequest.getVehicleId());
						if (vehicle != null) {
							if (userRequestList.contains(userRequest) == false) {
								if (tempVehicleList.contains(vehicle) == false) {
									userRequestList.add(userRequest);
									tempVehicleList.add(vehicle);
								} else {
									traceLongRunAbnormal("checkVehicleForFixedTransferRequestList() - Duplicated Vehicle. VehicleId:" + userRequest.getVehicleId());
									disableUserRequest(userRequest);
								}
							} else {
								// previousRequested
								; /*NULL*/
							}
						} else {
							traceLongRunAbnormal("checkVehicleForFixedTransferRequestList() - vehicle is null. VehicleId:" + userRequest.getVehicleId());
							disableUserRequest(userRequest);
						}
					}
				} else {
					traceLongRunAbnormal("checkVehicleForFixedTransferRequestList() - userRequest is null.");
				}
			}
		} catch (Exception e) {
			traceLongRunException("checkVehicleForFixedTransferRequestList()", e);
		}
	}
	
	private void checkMoveRequests() {
		try {
			// Check MOVE Request.
			// Enabled UserRequest에 대해 Duplicated Vehicle을 허용하지 않음. -> Disable 시킴.
			Vehicle vehicle = null;
			for (UserRequest userRequest : moveRequestList) {
				if (userRequest != null) {
					vehicle = vehicleMap.get(userRequest.getVehicleId());
					if (vehicle != null) {
						if (previousRequestedMap.containsKey(userRequest)) {
							userRequestList.add(userRequest);
						} else {
							if (tempVehicleList.contains(vehicle) == false) {
								userRequestList.add(userRequest);
								tempVehicleList.add(vehicle);
							} else {
								traceLongRunAbnormal("checkMoveRequests() - Duplicated Vehicle. VehicleId:" + userRequest.getVehicleId());
								disableUserRequest(userRequest);
							}
						}
					} else {
						traceLongRunAbnormal("checkMoveRequests() - vehicle is null. VehicleId:" + userRequest.getVehicleId());
						disableUserRequest(userRequest);
					}
				} else {
					traceLongRunAbnormal("checkMoveRequests() - userRequest is null.");
				}
			}
		} catch (Exception e) {
			traceLongRunException("checkMoveRequests()", e);
		}
	}
	
	
	
	private void preprocessUserRequest(UserRequest userRequest) {
		if (userRequest != null) {
			switch (userRequest.getType()) {
				case PATROL:
					if (patrolRequestList.contains(userRequest) == false) {
						patrolRequestList.add(userRequest);
					}
					break;
				case VIBRATION:
					long elapsedTime = (long) (currentDBTime - getLastTouredTime(userRequest.getLastTouredTime()));
					long overTime = elapsedTime - getCycleTimeLimit(userRequest);
					if (previousRequestedMap.containsKey(userRequest)) {
						if (vibrationRequestList.contains(userRequest) == false) {
							vibrationRequestList.add(userRequest);
						}
						vibrationRequestWithDockingStationMap.put(userRequest.getDockingStationId(), new VibrationRequest(userRequest, overTime, elapsedTime));
					} else {
						if (overTime > 0) {
							VibrationRequest prevVibrationRequest = vibrationRequestWithDockingStationMap.get(userRequest.getDockingStationId());
							if (prevVibrationRequest != null) {
								UserRequest prevUserRequest = prevVibrationRequest.getUserRequest();
								if (prevUserRequest != null) {
									if (previousRequestedMap.containsKey(prevUserRequest) == false) {
										if (overTime > prevVibrationRequest.getOverTime() || elapsedTime > prevVibrationRequest.getElapsedTime()) {
											if (vibrationRequestList.contains(prevUserRequest)) {
												// prevUserRequest 제거, userRequest 추가
												StringBuilder message = new StringBuilder();
												message.append("preprocessUserRequest() - DockingStation:").append(userRequest.getDockingStationId());
												message.append(" New UserRequestId:").append(userRequest.getUserRequestId());
												message.append(", OverTime:").append(overTime);
												message.append(", ElapsedTime:").append(elapsedTime);
												message.append(".  Previous UserRequestId:").append(prevVibrationRequest.getUserRequestId());
												message.append(", OverTime:").append(prevVibrationRequest.getOverTime());
												message.append(", ElapsedTime:").append(prevVibrationRequest.getElapsedTime());
												traceLongRunMain(message.toString());
												
												vibrationRequestList.remove(prevUserRequest);
												prevVibrationRequest.setUserRequest(userRequest);
												prevVibrationRequest.setOverTime(overTime);
												prevVibrationRequest.setElapsedTime(elapsedTime);
												
												if (vibrationRequestList.contains(userRequest) == false) {
													vibrationRequestList.add(userRequest);
												}
											} else {
												traceLongRunAbnormal("preprocessUserRequest() - vibrationRequestList doesn't contain prevUserRequest.");
											}
										} else {
											// prevUserRequest 유지.
										}
									} else {
										// prevUserRequest 유지.
									}
								} else {
									traceLongRunAbnormal("preprocessUserRequest() - prevUserRequest is null.");
								}
							} else {
								// userRequest 추가
								if (vibrationRequestList.contains(userRequest) == false) {
									vibrationRequestList.add(userRequest);
								}
								vibrationRequestWithDockingStationMap.put(userRequest.getDockingStationId(), new VibrationRequest(userRequest, overTime, elapsedTime));
							}
						}
					}
					break;
				case RANDOM_TRANSFER:
					if (randomTransferRequestList.contains(userRequest) == false) {
						randomTransferRequestList.add(userRequest);
					}
					break;
				case FIXED_TRANSFER:
					if (fixedTransferRequestList.contains(userRequest) == false) {
						fixedTransferRequestList.add(userRequest);
					}
					break;
				case MOVE:
					if (moveRequestList.contains(userRequest) == false) {
						moveRequestList.add(userRequest);
					}
					break;
				case UNDEFINED:
				default:
					traceLongRunAbnormal("UserRequest Type:" + userRequest.getTypeString() + " is NOT allowed.");
					disableUserRequest(userRequest);
					break;
			}
		} else {
			traceLongRunAbnormal("preprocessUserRequest() - userRequest is null.");
		}
	}
	
	private boolean isValidUserRequest(UserRequest userRequest) {
		if (userRequest != null) {
			StringBuilder message = new StringBuilder();
			message.append(userRequest.getUserRequestId()).append("> ");
			message.append("ValidationCheck Started. Type:").append(userRequest.getTypeString());
			message.append(", VehicleId:").append(userRequest.getVehicleId());
			message.append(", Tourlist:").append(userRequest.getTourListString());
			if (userRequest.getType() == USERREQUEST_TYPE.VIBRATION) {
				message.append(", Cycle:").append(userRequest.getCycle());
				message.append(", CycleUnit:").append(userRequest.getCycleUnitString());
				message.append(", DockingStation:").append(userRequest.getDockingStationId());
			}
			if (userRequest.isLoadingByPass()) {
				message.append(", LoadingByPass:TRUE");
			} else {
				message.append(", LoadingByPass:FALSE");
			}
			message.append(", LastTouredTime:").append(userRequest.getLastTouredTime());
			traceLongRunMain(message.toString());
			
			switch (userRequest.getType()) {
				case PATROL:
				{
					return isValidPatrolUserRequest(userRequest);
				}
				case VIBRATION:
				{
					return isValidVibrationUserRequest(userRequest);
				}
				case RANDOM_TRANSFER:
				{
					return isValidRandomTransferUserRequest(userRequest);
				}
				case FIXED_TRANSFER:
				case MOVE:
				{
					return isValidDefinedVehicleUserRequest(userRequest);
				}
				case UNDEFINED:
				default:
					StringBuilder message1 = new StringBuilder();
					message1.append("isValidUserRequest() - Invalid UserRequest Type. ");
					message1.append("UserRequestId:").append(userRequest.getUserRequestId());
					message1.append(", Type:").append(userRequest.getTypeString());
					traceLongRunAbnormal(message1.toString());
					break;
			}
		}
		return false;
	}
	
	private boolean checkTourlist(UserRequest userRequest) {
		if (userRequest != null) {
			StringBuilder message = new StringBuilder();
			message.append("checkTourlist() - UserRequestId:").append(userRequest.getUserRequestId()).append(" ");
			if (userRequest.getTourListSize() == 0 || (System.currentTimeMillis() - userRequest.getLastTourListCheckedTime()) > TOURLIST_CHECKEDTIME_LIMIT) {
				String tourListString = userRequest.getTourListString();
				if (tourListString != null && tourListString.length() > 0) {
					tourListString = tourListString.trim();
					String[] tourArray = tourListString.split(COMMA);
					String tourId = null;
					Tour tour = null;
					ArrayList<Object> tempTourList = new ArrayList<Object>();
					for (int i = 0; i < tourArray.length; i++) {
						tourId = tourArray[i];
						if (tourId != null && tourId.length() > 0) {
							tour = tourManager.getTour(tourId);
							if (tour != null) {
								if (checkTour(userRequest.getType(), tour, tempTourList) == false) {
									message.append("UserRequest Type:").append(userRequest.getTypeString());
									message.append(", Tour Type:").append(tour.getTypeString());
									message.append(" tourListString:").append(tourListString);
									traceLongRunAbnormal(message.toString());
									return false;
								}
							} else {
								message.append("- tour is null.");
								message.append(" tourListString:").append(tourListString);
								traceLongRunAbnormal(message.toString());
								return false;
							}
						} else {
							message.append("- tourId is null.");
							message.append(" tourListString:").append(tourListString);
							traceLongRunAbnormal(message.toString());
							return false;
						}
					}
					
					if (tempTourList.size() == 0) {
						message.append("- NO Tours.");
						traceLongRunAbnormal(message.toString());
						return false;
					}
					
					if (userRequest.getType() == USERREQUEST_TYPE.MOVE) {
						if (tempTourList.size() >= 2) {
							Object firstTourItem = tempTourList.get(0);
							if (firstTourItem != null) {
								int lastIndex = tempTourList.size() - 1;
								if (firstTourItem.equals(tempTourList.get(lastIndex))) {
									tempTourList.remove(lastIndex);
								}
							} else {
								message.append("- MOVE Type. firstTourItem is null.");
								traceLongRunAbnormal(message.toString());
								return false;
							}
						}
						
						if (tempTourList.size() < 2) {
							message.append("- MOVE Type. But, Size of Valid Tours is less than two.");
							message.append(" tourListString:").append(tourListString);
							traceLongRunAbnormal(message.toString());
							return false;
						}
					}
					
					if (userRequest.getType() == USERREQUEST_TYPE.FIXED_TRANSFER && tempTourList.size() < 2) {
						message.append("- FixedTransfer Type UserRequest needs at least two CarrierLocs.");
						message.append(" tourListString:").append(tourListString);
						traceLongRunAbnormal(message.toString());
						return false;
					}
					
					for (Object tourItem : tempTourList) {
						if (tourItem != null) {
							userRequest.addToTourList(tourItem);
						} else {
							message.append("- tourItem is null.");
							traceLongRunAbnormal(message.toString());
							return false;
						}
					}
					
					userRequest.setLastTourListCheckedTime(System.currentTimeMillis()); 
					return true;
				} else {
					message.append("- tourListString is null.");
					message.append(" tourListString:").append(tourListString);
					traceLongRunAbnormal(message.toString());
				}
			} else {
				return true;
			}
		} else {
			traceLongRunAbnormal("checkTourlist() - userRequest is null.");
		}
		return false;
	}
	
	private boolean checkTour(USERREQUEST_TYPE userRequestType, Tour tour, ArrayList<Object> tempTourList) {
		if (tour != null) {
			switch (tour.getType()) {
				case PAIR:
				{
					return getPairTypeTours(userRequestType, tour, tempTourList);
				}
				case NODE:
				{
					return getNodeTypeTours(userRequestType, tour, tempTourList);
				}
				case CARRIERLOC:
				{
					return getCarrierLocTypeTours(userRequestType, tour, tempTourList);
				}
				default:
					break;
			}
		}
		return false;
	}
	
	private boolean getPairTypeTours(USERREQUEST_TYPE userRequestType, Tour tour, ArrayList<Object> tempTourList) {
		try {
			if (tour != null && tempTourList != null) {
				if (tour.getType() == TOUR_TYPE.PAIR) {
					if (userRequestType == USERREQUEST_TYPE.PATROL || userRequestType == USERREQUEST_TYPE.VIBRATION || userRequestType == USERREQUEST_TYPE.MOVE) {
						String tourString = tour.getTour();
						if (tourString != null && tourString.length() > 0) {
							tourString = tourString.trim();
							String[] routeArray = tourString.split(COMMA);
							String route = null;
							for (int i = 0; i < routeArray.length; i++) {
								route = routeArray[i];
								if (route != null && route.length() > 0) {
									String[] pathArray = route.split(COLON);
									String startNodeId = null;
									String endNodeId = null;
									if (pathArray.length == 2) {
										startNodeId = pathArray[0];
										endNodeId = pathArray[1];
										if (startNodeId != null && startNodeId.length() > 0 && nodeManager.isValidNode(startNodeId) &&
												endNodeId != null && endNodeId.length() > 0 && nodeManager.isValidNode(endNodeId)) {
											if (userRequestType == USERREQUEST_TYPE.PATROL) {
												tempTourList.add(new Path(startNodeId, endNodeId));
											} else if (userRequestType == USERREQUEST_TYPE.VIBRATION || userRequestType == USERREQUEST_TYPE.MOVE) {
												String prevNodeId = null;
												if (tempTourList.size() > 0) {
													prevNodeId = (String)tempTourList.get(tempTourList.size() - 1);
												}
												if (startNodeId.equals(prevNodeId) == false) {
													tempTourList.add(startNodeId);
													prevNodeId = startNodeId;
												}
												if (endNodeId.equals(prevNodeId) == false) {
													tempTourList.add(endNodeId);
													prevNodeId = endNodeId;
												}
											} else {
												return false;
											}
										} else {
											return false;
										}
									} else {
										return false;
									}
								} else {
									return false;
								}
							}
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			traceLongRunException("getPairTypeTours()", e);
		}
		return false;
	}
	
	private boolean getNodeTypeTours(USERREQUEST_TYPE userRequestType, Tour tour, ArrayList<Object> tempTourList) {
		try {
			if (tour != null && tempTourList != null) {
				if (tour.getType() == TOUR_TYPE.NODE) {
					if (userRequestType == USERREQUEST_TYPE.VIBRATION || userRequestType == USERREQUEST_TYPE.MOVE) {
						String tourString = tour.getTour();
						if (tourString != null && tourString.length() > 0) {
							tourString = tourString.trim();
							String[] routeArray = tourString.split(COMMA);
							String route = null;
							for (int i = 0; i < routeArray.length; i++) {
								route = routeArray[i];
								if (route != null && route.length() > 0) {
									if (route != null && route.length() > 0 && nodeManager.isValidNode(route)) {
										String prevNodeId = null;
										if (tempTourList.size() > 0) {
											prevNodeId = (String)tempTourList.get(tempTourList.size() - 1);
										}
										if (route.equals(prevNodeId) == false) {
											tempTourList.add(route);
										}
									} else {
										return false;
									}
								} else {
									return false;
								}
							}
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			traceLongRunException("getNodeTypeTours()", e);
		}
		return false;
	}
	
	private boolean getCarrierLocTypeTours(USERREQUEST_TYPE userRequestType, Tour tour, ArrayList<Object> tempTourList) {
		try {
			if (tour != null && tempTourList != null) {
				if (tour.getType() == TOUR_TYPE.CARRIERLOC) {
					if (userRequestType == USERREQUEST_TYPE.RANDOM_TRANSFER || userRequestType == USERREQUEST_TYPE.FIXED_TRANSFER) {
						String tourString = tour.getTour();
						if (tourString != null && tourString.length() > 0) {
							tourString = tourString.trim();
							String[] routeArray = tourString.split(COMMA);
							String route = null;
							for (int i = 0; i < routeArray.length; i++) {
								route = routeArray[i];
								if (route != null && route.length() > 0) {
									CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(route);
									if (carrierLoc != null) {
										CarrierLoc prevCarrierLoc = null;
										if (tempTourList.size() > 0) {
											prevCarrierLoc = (CarrierLoc)tempTourList.get(tempTourList.size() - 1);
										}
										if (prevCarrierLoc != null) {
											if (carrierLoc.getMaterial().equals(prevCarrierLoc.getMaterial()) == false) {
												StringBuilder message = new StringBuilder();
												message.append("getCarrierLocTypeTours() - Different Materials.");
												message.append("(").append(prevCarrierLoc.getCarrierLocId());
												message.append(":").append(prevCarrierLoc.getMaterial());
												message.append(") vs. (").append(carrierLoc.getCarrierLocId());
												message.append(":").append(carrierLoc.getMaterial());
												message.append("), Tour:").append(tour.getTourId());
												message.append("(").append(tourString).append(")");
												traceLongRunAbnormal(message.toString());
												return false;
											}
										}
										
										if (carrierLoc.equals(prevCarrierLoc) == false) {
											tempTourList.add(carrierLoc);
										}
									} else {
										return false;
									}
								} else {
									return false;
								}
							}
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			traceLongRunException("getCarrierLocTypeTours()", e);
		}
		return false;
	}
	
	private boolean isValidPatrolUserRequest(UserRequest userRequest) {
		if (userRequest != null) {
			if (checkTourlist(userRequest)) {
				String vehicleId = userRequest.getVehicleId();
				if (vehicleId != null && vehicleId.length() > 0) {
					Vehicle vehicle = vehicleManager.getVehicle(vehicleId);
					if (vehicle != null) {
						if (PATROL_VEHICLEZONE.equals(vehicle.getZone())) {
							return true;
						} else {
							StringBuilder message = new StringBuilder();
							message.append("isValidPatrolUserRequest() - The vehicle's Zone is NOT 'Patrol'. ");
							message.append("UserRequestId:").append(userRequest.getUserRequestId());
							message.append(", VehicleId:").append(userRequest.getVehicleId());
							message.append(", Vehicle's Zone:").append(vehicle.getZone());
							traceLongRunAbnormal(message.toString());
						}
					} else {
						StringBuilder message = new StringBuilder();
						message.append("isValidPatrolUserRequest() - vehicle is null. ");
						message.append("UserRequestId:").append(userRequest.getUserRequestId());
						message.append(", VehicleId:").append(userRequest.getVehicleId());
						traceLongRunAbnormal(message.toString());
					}
				} else {
					StringBuilder message = new StringBuilder();
					message.append("isValidPatrolUserRequest() - vehicleId is null. ");
					message.append("UserRequestId:").append(userRequest.getUserRequestId());
					message.append(", VehicleId:").append(userRequest.getVehicleId());
					traceLongRunAbnormal(message.toString());
				}
			} else {
				StringBuilder message = new StringBuilder();
				message.append("isValidPatrolUserRequest() - Invalid Tourlist. ");
				message.append("UserRequestId:").append(userRequest.getUserRequestId());
				message.append(", Tourlist:").append(userRequest.getTourListString());
				traceLongRunAbnormal(message.toString());
			}
		} else {
			traceLongRunAbnormal("isValidPatrolUserRequest() - userRequest is null.");
		}
		return false;
	}
	
	private boolean isValidVibrationUserRequest(UserRequest userRequest) {
		if (userRequest != null) {
			if (checkTourlist(userRequest)) {
				if (userRequest.getCycleUnit() != CYCLE_UNIT.UNDEFINED) {
					CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(userRequest.getDockingStationId());
					if (carrierLoc != null) {
						DockingStation dockingStation = dockingStationManager.getDockingStationData(userRequest.getDockingStationId());
						if (dockingStation != null) {
							userRequest.setDockingStation(dockingStation);
							
							String vehicleId = userRequest.getVehicleId();
							if (vehicleId == null || vehicleId.length() == 0) {
								// ANY Type
								return true;
							} else {
								// 지정 호기 Type
								Vehicle vehicle = vehicleManager.getVehicle(vehicleId);
								if (vehicle != null) {
									// 2014.03.03 by KYK
//									if (vehicle.getMaterial().equals(carrierLoc.getMaterial())) {
									if (isMaterialAssignAllowed(vehicle, carrierLoc)) {
										return true;
									} else {
										StringBuilder message = new StringBuilder();
										message.append("isValidVibrationUserRequest() - ");
										message.append("UserRequestId:").append(userRequest.getUserRequestId());
										message.append(", VehicleId:").append(userRequest.getVehicleId());
										message.append(", Vehicle's Material:").append(vehicle.getMaterial());
										message.append(", DockingStation:").append(userRequest.getDockingStationId());
										message.append(", DockingStation's Material:").append(carrierLoc.getMaterial());
										traceLongRunAbnormal(message.toString());
									}
								} else {
									StringBuilder message = new StringBuilder();
									message.append("isValidVibrationUserRequest() - vehicle is null. ");
									message.append("UserRequestId:").append(userRequest.getUserRequestId());
									message.append(", VehicleId:").append(userRequest.getVehicleId());
									traceLongRunAbnormal(message.toString());
								}
							}
						} else {
							StringBuilder message = new StringBuilder();
							message.append("isValidVibrationUserRequest() - dockingStation is null or disabled. ");
							message.append("UserRequestId:").append(userRequest.getUserRequestId());
							message.append(", DockingStation:").append(userRequest.getDockingStationId());
							traceLongRunAbnormal(message.toString());
						}
					} else {
						StringBuilder message = new StringBuilder();
						message.append("isValidVibrationUserRequest() - carrierLoc is null. ");
						message.append("UserRequestId:").append(userRequest.getUserRequestId());
						message.append(", DockingStation:").append(userRequest.getDockingStationId());
						traceLongRunAbnormal(message.toString());
					}
				} else {
					StringBuilder message = new StringBuilder();
					message.append("isValidVibrationUserRequest() - Invalid CycleUnit. ");
					message.append("UserRequestId:").append(userRequest.getUserRequestId());
					message.append(", CycleUnit:").append(userRequest.getCycleUnitString());
					traceLongRunAbnormal(message.toString());
				}
			} else {
				StringBuilder message = new StringBuilder();
				message.append("isValidVibrationUserRequest() - Invalid Tourlist. ");
				message.append("UserRequestId:").append(userRequest.getUserRequestId());
				message.append(", Tourlist:").append(userRequest.getTourListString());
				traceLongRunAbnormal(message.toString());
			}
		} else {
			traceLongRunAbnormal("isValidVibrationUserRequest() - userRequest is null.");
		}
		return false;
	}
	
	private boolean isValidRandomTransferUserRequest(UserRequest userRequest) {
		if (userRequest != null) {
			if (checkTourlist(userRequest)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isValidDefinedVehicleUserRequest(UserRequest userRequest) {
		if (userRequest != null) {
			if (checkTourlist(userRequest)) {
				String vehicleId = userRequest.getVehicleId();
				if (vehicleId != null && vehicleId.length() > 0) {
					Vehicle vehicle = vehicleManager.getVehicle(vehicleId);
					if (vehicle != null) {
						return true;
					} else {
						StringBuilder message = new StringBuilder();
						message.append("isValidDefinedVehicleUserRequest() - vehicle is null. ");
						message.append("UserRequestId:").append(userRequest.getUserRequestId());
						message.append(", VehicleId:").append(userRequest.getVehicleId());
						traceLongRunAbnormal(message.toString());
					}
				} else {
					StringBuilder message = new StringBuilder();
					message.append("isValidDefinedVehicleUserRequest() - vehicleId is null. ");
					message.append("UserRequestId:").append(userRequest.getUserRequestId());
					message.append(", VehicleId:").append(userRequest.getVehicleId());
					traceLongRunAbnormal(message.toString());
				}
			} else {
				StringBuilder message = new StringBuilder();
				message.append("isValidDefinedVehicleUserRequest() - Invalid Tourlist. ");
				message.append("UserRequestId:").append(userRequest.getUserRequestId());
				message.append(", Tourlist:").append(userRequest.getTourListString());
				traceLongRunAbnormal(message.toString());
			}
		} else {
			traceLongRunAbnormal("isValidDefinedVehicleUserRequest() - userRequest is null.");
		}
		return false;
	}
	
	private long getLastTouredTime(String lastTouredTime) {
		try {
			if (lastTouredTime != null && lastTouredTime.length() == 14) {
				return sdf.parse(lastTouredTime).getTime();
			}
		} catch (Exception e) {
			traceLongRunAbnormal("getLastTouredTime() - lastTouredTime:" + lastTouredTime);
			traceLongRunException("getLastTouredTime()", e);
		}
		return 0;
	}
	
	private long getCycleTimeLimit(UserRequest userRequest) {
		if (userRequest != null) {
			switch (userRequest.getCycleUnit()) {
				case DAY:
					return UNIT_DAY * userRequest.getCycle();
				case HOUR:
					return UNIT_HOUR * userRequest.getCycle();
				case MINUTE:
					return UNIT_MINUTE * userRequest.getCycle();
				case SECOND:
					return UNIT_SECOND * userRequest.getCycle();
				default:
					break;
			}
		}
		return DEFAULT_CYCLETIME_LIMIT;
	}
	
	private void registerToOccupiedCarrierLoc(CarrierLoc carrierLoc, String carrierId) {
		try {
			if (carrierLoc != null) {
				if (occupiedCarrierLocList.contains(carrierLoc) == false) {
					occupiedCarrierLocList.add(carrierLoc);
					carrierWithCarrierLocMap.put(carrierLoc, carrierId);
				}
			} else {
				traceLongRunAbnormal("registerToOccupiedCarrierLoc(). carrierLoc is null.");
			}
		} catch (Exception e) {
			traceLongRunException("registerToOccupiedCarrierLoc()", e);
		}
	}
	
	private void releaseFromOccupiedCarrierLoc(CarrierLoc carrierLoc) {
		try {
			if (carrierLoc != null) {
				occupiedCarrierLocList.remove(carrierLoc);
				carrierWithCarrierLocMap.remove(carrierLoc);
			} else {
				traceLongRunAbnormal("releaseFromOccupiedCarrierLoc(). carrierLoc is null.");
			}
		} catch (Exception e) {
			traceLongRunException("releaseFromOccupiedCarrierLoc()", e);
		}
		
	}
	
	private void addAbnormalNode(Vehicle vehicle) {
		try {
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
						}
					}
				} else {
					addAbnormalNode(vehicle.getCurrNode());
					addAbnormalNode(vehicle.getStopNode());
				}
			}
		} catch (Exception e) {
			traceLongRunException("addAbnormalNode()", e);
		}
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
	
	public void initializeForRuntimeUpdate() {
		costSearch.setNearByDrive(ocsInfoManager.isNearByDrive());
	}
	
	/**
	 * 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가로 검색방법 변경
	 * 2013.08.30 by KYK
	 * @param vehicle
	 * @param sourceLoc
	 * @return
	 */
	private boolean isMaterialAssignAllowed(Vehicle vehicle, CarrierLoc sourceLoc) {
		String key = vehicle.getMaterial() + "_" + sourceLoc.getMaterial();
		if (vehicle.getMaterialIndex() == sourceLoc.getMaterialIndex()) {
			for(String value : materialAssignAllowedList) {
				if (value.matches(key + ".*")) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
	 * @param vehicle
	 * @param sourceLoc
	 * @param destLoc
	 * @return
	 */
	private boolean isMaterialAssignAllowed(Vehicle vehicle, CarrierLoc sourceLoc, CarrierLoc destLoc) {
		if (vehicle.getMaterialIndex() == sourceLoc.getMaterialIndex() ||
			materialAssignAllowedList.contains(materialControlManager.makeMeterialAssignAllowedKey(vehicle.getMaterial(), sourceLoc.getMaterial(), destLoc.getMaterial())) ) {
			return true;
		}
		return false;
	}
	
	private static final String LONGRUN_MAIN = "LongRunMain";
	private static Logger longRunMainLog = Logger.getLogger(LONGRUN_MAIN);
	public void traceLongRunMain(String message) {
		longRunMainLog.debug(message);
	}
	
	private static final String LONGRUN_ABNORMAL = "LongRunAbnormal";
	private static Logger longRunAbnormalLog = Logger.getLogger(LONGRUN_ABNORMAL);
	public void traceLongRunAbnormal(String message) {
		longRunAbnormalLog.debug(message);
	}
	
	private static final String LONGRUN_EXCEPTION = "LongRunException";
	private static Logger longRunExceptionLog = Logger.getLogger(LONGRUN_EXCEPTION);
	public void traceLongRunException(String message) {
		longRunExceptionLog.error(message);
	}
	public void traceLongRunException(String message, Throwable e) {
		longRunExceptionLog.error(message, e);
	}
}
