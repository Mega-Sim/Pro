package com.samsung.ocs.route.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.SEARCH_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.TRAFFIC_UPDATE_RULE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.manager.impl.model.Collision;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.NodeArrivedTimeInfo;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.Traffic;
import com.samsung.ocs.manager.impl.model.VehicleData;

/**
 * YieldSearch Class, OCS 3.0 for Unified FAB 
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

public class YieldSearch extends Search {
	private static final String YSR4 = "YSR4";
	private static final String YSR5 = "YSR5";
	private static final double CONSECUTIVE_YIELD_TIME = 4.0;
	
	private static final String PATROL_VEHICLEZONE = "Patrol";
	
	// 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
//	private boolean isFailureOHTDetourSearchUsed = true; 
//	private boolean isCollisionNodeCheckUsed = true; 
//	private boolean isCollisionNodeCheckNeeded = true;
	private int yieldMinLimitTime = 4;
	private int consecutiveYieldLimitTime = 4;
	private HashSet<String> yieldAllowedSet;
	private HashSet<Node> inTheWayNodeSet = null;
	
	// 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
	// CostSearch 때문에 private
//	private HashSet<Node> abnormalVehiclesCollisionNodeSet;
		
    /**
	 * Constructor of YieldSearch class.
	 */
	// 2015.09.16 by MYM : Map(abnormalVehiclesOnCollisionMap)으로 대체 및 OperationManager에서 생성으로 변경
//	public YieldSearch() {
//		super();
//		yieldAllowedSet = zoneControlManager.getYieldAllowedSet();
//		inTheWayNodeSet = new HashSet<Node>();
//		abnormalVehiclesCollisionNodeSet = nodeManager.getAbnormalVehiclesCollisionNodeSet();
//	}
	public YieldSearch(ConcurrentHashMap<Node, VehicleData> abnormalVehiclesOnCollisionMap) {
		super();
		yieldAllowedSet = zoneControlManager.getYieldAllowedSet();
		inTheWayNodeSet = new HashSet<Node>();
		this.abnormalVehiclesOnCollisionMap = abnormalVehiclesOnCollisionMap;
	}

	/**
	 * 양보 요청이 있을 때 설정된 양보룰에 맞는 양보 로직을 호출한다.
	 * 
	 * ※ 고려사항
	 *   . Zone Over
	 * 
	 * @param vehicleId
	 * @return
	 */
	public boolean searchVehicleYieldPath(VehicleData vehicle) {
		assert vehicle != null;
		
		long processTime = System.currentTimeMillis();
		if (vehicle == null) {
			return false;
		}
		
		// PathSearch 요청 초기화 
		vehicle.setPathSearchRequest(false);
		
		boolean retVal = false;
		try {
			
			// 목적지까지 4.0초 이내 도착인 경우 추가적인 양보 노드를 검색(consecutive)
			// 2013.02.01 by KYK
//			if (vehicle.checkArrivedTimeAtTargetNode(CONSECUTIVE_YIELD_TIME)) {
			Station targetStation = stationManager.getStation(vehicle.getTargetStation());
			// 2015.07.08 by KYK : 연속양보 시간 파라미터화
//			if (vehicle.checkArrivedTimeAtTarget(CONSECUTIVE_YIELD_TIME, targetStation)) {
			if (vehicle.checkArrivedTimeAtTarget(consecutiveYieldLimitTime, targetStation)) {
				if (YSR4.equals(ocsInfoManager.getYieldSearchRule())) {
					retVal = searchYieldPath(vehicle);
				} else if (YSR5.equals(ocsInfoManager.getYieldSearchRule())) {
					retVal = searchYieldPathMin(vehicle);
				} else {
					retVal = searchYieldPath(vehicle);
				}
			} else {
				retVal = true;
			}			
		} catch (Exception e) {
			traceException("searchVehicleYieldPath()", e);
		} finally {
			if ((System.currentTimeMillis()-processTime) >= 100) {
				StringBuffer log = new StringBuffer();
//				log.append(" searchVehicleYieldPath() TimeDelay=").append(System.currentTimeMillis()-processTime).append(" ").append(vehicle.getVehicleId());
//				trace(log.toString());
				log.append("searchVehicleYieldPath() TimeDelay=").append(System.currentTimeMillis()-processTime);
				trace(vehicle.getVehicleId(), log.toString());
			}
		}
		return retVal;
	}
	
//	2014.07.26 by MYM : 미사용 주석처리
//	/**
//	 * search path for a local vehicle to come back to it's bay
//	 * 
//	 * @param vehicleId String
//	 * @param localGroupBay String
//	 * @return result boolean
//	 */
//	public boolean searchLocalVehicleComebackBayPath(String vehicleId, String localGroupBay) {
//		VehicleData vehicle = (VehicleData)vehicleManager.getVehicle(vehicleId);
//		if (vehicle == null) {
//			return false;
//		}
//		
//		if (vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() == 0) {
//			return false;
//		}
//		
//		// PathSearch 요청 초기화 
//		vehicle.setPathSearchRequest(false);
//		
//		long processTime = System.currentTimeMillis();
//		boolean retVal = false;
//		try {
//			retVal = searchLocalVehicleComebackBayPath(vehicle, localGroupBay);
//		} catch (Exception e) {
//			traceException("searchLocalVehicleComebackBayPath()", e);
//		} finally {
//			if ((System.currentTimeMillis()-processTime) >= 100) {
//				StringBuffer log = new StringBuffer();
////				log.append(" searchVehicleComebackZonePath() TimeDelay=").append(System.currentTimeMillis()-processTime).append(" ").append(vehicleId);
////				trace(log.toString());
//				log.append(" searchVehicleComebackZonePath() TimeDelay=").append(System.currentTimeMillis()-processTime).append(", To:").append(localGroupBay);
//				trace(vehicle.getVehicleId(), log.toString());
//			}
//		}
//		
//		return retVal;
//	}
	
	/**
	 * 2014.02.06 by KYK
	 * @param vehicle
	 * @param localGroupBay
	 * @return
	 */
	public boolean searchLocalVehicleComebackBay(VehicleData vehicle, String localGroupBay) {
		if (vehicle == null) {
			return false;
		}
		
		// PathSearch 요청 초기화 
		vehicle.setPathSearchRequest(false);
		
		long processTime = System.currentTimeMillis();
		boolean retVal = false;
		try {
			retVal = searchLocalVehicleComebackBayPath(vehicle, localGroupBay);
		} catch (Exception e) {
			traceException("searchLocalVehicleComebackBay()", e);
		} finally {
			if ((System.currentTimeMillis()-processTime) >= 100) {
				StringBuffer log = new StringBuffer();
//				log.append(" searchVehicleComebackZonePath() TimeDelay=").append(System.currentTimeMillis()-processTime).append(" ").append(vehicle.getVehicleId());
//				trace(log.toString());
				log.append("searchLocalVehicleComebackBay() TimeDelay=").append(System.currentTimeMillis()-processTime).append(", To:").append(localGroupBay);
				trace(vehicle.getVehicleId(), log.toString());
			}
		}
		
		return retVal;
	}

	/**
	 * search path for a vehicle to escape from abnormal HID
	 * 
	 * @param vehicle VehicleData
	 * @param vehicleCountPerHid int
	 * @param isIdle boolean
	 * @return result boolean
	 */
	public boolean searchEscapeForAbnormalHid(VehicleData vehicle, int vehicleCountPerHid, boolean isIdle) {
		assert vehicle != null;
		
		if (vehicle == null) {
			return false;
		}

		Hid mainHid = null;
		try {
			mainHid = vehicle.getDriveCurrNode().getHid();
		} catch (Exception e) {
			traceException("searchEscapeForAbnormalHid()", e);
			traceException("searchEscapeForAbnormalHid()", "Vehicle:" + vehicle.getVehicleId() + ", CurrNode:" + vehicle.getCurrNode());
		}
		if (mainHid == null || (mainHid.getState() != null && mainHid.getState().length() == 0)) {
			return false;
		}
		
		// PathSearch 요청 초기화 
		vehicle.setPathSearchRequest(false);
		
		long processTime = System.currentTimeMillis();
		boolean retVal = false;
		try {
			// HID Failover 기능 사용 시.
			Hid altHid = mainHid.getAltHid();
			Hid backupHid = mainHid.getBackupHid();
			
			if (mainHid.isHidDown()) {
				// S (Stop) or F (Fail) or W (Warning)
				
				if (mainHid.isAltHidCoveringForMainHid() == false) {
					// Main Down, Alt Failover X
					StringBuilder log = new StringBuilder();
					log.append("[Main(X), Alt FailOver(X), All VHL Escape] ");
					log.append("MainHID:").append(mainHid.getUnitId());
					log.append(", State:").append(mainHid.getState());
					log.append(", VehicleCount:").append(mainHid.getVehicleCount());
					
					log.append(", AltHID:");
					if (altHid != null) {
						log.append(altHid.getUnitId());
						log.append(", State:").append(altHid.getState());
						log.append(", VehicleCount:").append(altHid.getVehicleCount());
					} else {
						log.append("(null)");
					}
					log.append(", Limit:").append(vehicleCountPerHid);
					log.append(", isIdle:").append(isIdle);
					log.append(", VHLReq:").append(vehicle.getRequestedType());
//					trace(log.toString());
					trace(vehicle.getVehicleId(), log.toString());
					return searchEscapeForAbnormalHid(vehicle, mainHid, altHid);
				} else {
					// altHid != null
					// Main Down, Alt Failover O
					if ((mainHid.getVehicleCount() + altHid.getVehicleCount()) > vehicleCountPerHid) {
						// Main(X) + Alt(O) Capacity Full
						StringBuilder log = new StringBuilder();
						log.append("[Main(X) <- Alt FailOver(O), Capacity Over, All VHL Escape] ");
						log.append("MainHID:").append(mainHid.getUnitId());
						log.append(", State:").append(mainHid.getState());
						log.append(", VehicleCount:").append(mainHid.getVehicleCount());
						log.append(", AltHID:").append(altHid.getUnitId());
						log.append(", State:").append(altHid.getState());
						log.append(", VehicleCount:").append(altHid.getVehicleCount());
						log.append(", Limit:").append(vehicleCountPerHid);
						log.append(", isIdle:").append(isIdle);
						log.append(", VHLReq:").append(vehicle.getRequestedType());
//						trace(log.toString());
						trace(vehicle.getVehicleId(), log.toString());
						return searchEscapeForAbnormalHid(vehicle, mainHid, altHid);
					} else if (isIdle && (mainHid.getVehicleCount() + altHid.getVehicleCount()) >= vehicleCountPerHid) {
						// Main(X) + Alt(O) Capacity Full이 우려되는 경우, Idle은 해당 HID 영역에서 배출.
						StringBuilder log = new StringBuilder();
						log.append("[Main(X) <- Alt FailOver(O), Capacity Full, Idle VHL Escape] ");
						log.append("MainHID:").append(mainHid.getUnitId());
						log.append(", State:").append(mainHid.getState());
						log.append(", VehicleCount:").append(mainHid.getVehicleCount());
						log.append(", AltHID:").append(altHid.getUnitId());
						log.append(", State:").append(altHid.getState());
						log.append(", VehicleCount:").append(altHid.getVehicleCount());
						log.append(", Limit:").append(vehicleCountPerHid);
						log.append(", isIdle:").append(isIdle);
						log.append(", VHLReq:").append(vehicle.getRequestedType());
//						trace(log.toString());
						trace(vehicle.getVehicleId(), log.toString());
						return searchEscapeForAbnormalHid(vehicle, mainHid, altHid);
					}
				}
			} else {
				if (OcsConstant.HID_FAILOVER.equals(mainHid.getState())) {
					if (altHid != null && altHid.isHidDown()) {
						// Failover HID Case
						if ((mainHid.getVehicleCount() + altHid.getVehicleCount()) > vehicleCountPerHid) {
							// Main(O) + Alt(X) Capacity Full
							StringBuilder log = new StringBuilder();
							log.append("[Main(O) -> Alt(X), Capacity Over, All VHL Escape] ");
							log.append("MainHID:").append(mainHid.getUnitId());
							log.append(", State:").append(mainHid.getState());
							log.append(", VehicleCount:").append(mainHid.getVehicleCount());
							log.append(", AltHID:").append(altHid.getUnitId());
							log.append(", State:").append(altHid.getState());
							log.append(", VehicleCount:").append(altHid.getVehicleCount());
							log.append(", Limit:").append(vehicleCountPerHid);
							log.append(", isIdle:").append(isIdle);
							log.append(", VHLReq:").append(vehicle.getRequestedType());
//							trace(log.toString());
							trace(vehicle.getVehicleId(), log.toString());
							return searchEscapeForAbnormalHid(vehicle, mainHid, altHid);
						} else if (isIdle && (mainHid.getVehicleCount() + altHid.getVehicleCount()) >= vehicleCountPerHid) {
							// Main(O) + Alt(X) Capacity Full이 우려되는 경우, Idle은 해당 HID 영역에서 배출.
							StringBuilder log = new StringBuilder();
							log.append("[Main(O) -> Alt(X), Capacity Full, Idle VHL Escape] ");
							log.append("MainHID:").append(mainHid.getUnitId());
							log.append(", State:").append(mainHid.getState());
							log.append(", VehicleCount:").append(mainHid.getVehicleCount());
							log.append(", AltHID:").append(altHid.getUnitId());
							log.append(", State:").append(altHid.getState());
							log.append(", VehicleCount:").append(altHid.getVehicleCount());
							log.append(", Limit:").append(vehicleCountPerHid);
							log.append(", isIdle:").append(isIdle);
							log.append(", VHLReq:").append(vehicle.getRequestedType());
//							trace(log.toString());
							trace(vehicle.getVehicleId(), log.toString());
							return searchEscapeForAbnormalHid(vehicle, mainHid, altHid);
						}
					} else if (backupHid != null && backupHid.isHidDown()) {
						// MultiBackup HID Case
						if ((mainHid.getVehicleCount() + backupHid.getVehicleCount()) > vehicleCountPerHid) {
							// Main(O) + Backup(X) Capacity Full
							StringBuilder log = new StringBuilder();
							log.append("[Main(O) -> Backup(X), Capacity Over, All VHL Escape] ");
							log.append("MainHID:").append(mainHid.getUnitId());
							log.append(", State:").append(mainHid.getState());
							log.append(", VehicleCount:").append(mainHid.getVehicleCount());
							log.append(", BackupHID:").append(backupHid.getUnitId());
							log.append(", State:").append(backupHid.getState());
							log.append(", VehicleCount:").append(backupHid.getVehicleCount());
							log.append(", Limit:").append(vehicleCountPerHid);
							log.append(", isIdle:").append(isIdle);
							log.append(", VHLReq:").append(vehicle.getRequestedType());
//							trace(log.toString());
							trace(vehicle.getVehicleId(), log.toString());
							return searchEscapeForAbnormalHid(vehicle, mainHid, backupHid);
						} else if (isIdle && (mainHid.getVehicleCount() + backupHid.getVehicleCount()) >= vehicleCountPerHid) {
							// Main(O) + Backup(X) Capacity Full이 우려되는 경우, Idle은 해당 HID 영역에서 배출.
							StringBuilder log = new StringBuilder();
							log.append("[Main(O) -> Backup(X), Capacity Full, Idle VHL Escape] ");
							log.append("MainHID:").append(mainHid.getUnitId());
							log.append(", State:").append(mainHid.getState());
							log.append(", VehicleCount:").append(mainHid.getVehicleCount());
							log.append(", BackupHID:").append(backupHid.getUnitId());
							log.append(", State:").append(backupHid.getState());
							log.append(", VehicleCount:").append(backupHid.getVehicleCount());
							log.append(", Limit:").append(vehicleCountPerHid);
							log.append(", isIdle:").append(isIdle);
							log.append(", VHLReq:").append(vehicle.getRequestedType());
//							trace(log.toString());
							trace(vehicle.getVehicleId(), log.toString());
							return searchEscapeForAbnormalHid(vehicle, mainHid, backupHid);
						}
					} else {
						if (mainHid.getVehicleCount() > vehicleCountPerHid) {
							// Main Capacity Full.
							StringBuilder log = new StringBuilder();
							log.append("[Main(O) -> Unkown(X), Capacity Over, All VHL Escape] ");
							log.append("MainHID:").append(mainHid.getUnitId());
							log.append(", State:").append(mainHid.getState());
							log.append(", VehicleCount:").append(mainHid.getVehicleCount());
							log.append(", Limit:").append(vehicleCountPerHid);
							log.append(", isIdle:").append(isIdle);
							log.append(", VHLReq:").append(vehicle.getRequestedType());
//							trace(log.toString());
							trace(vehicle.getVehicleId(), log.toString());
							return searchEscapeForAbnormalHid(vehicle, mainHid, null);
						} else if (isIdle && mainHid.getVehicleCount() >= vehicleCountPerHid) {
							// Main Capacity Full이 우려되는 경우, Idle은 해당 HID 영역에서 배출.
							StringBuilder log = new StringBuilder();
							log.append("[Main(O) -> Unkown(X), Capacity Full, Idle VHL Escape] ");
							log.append("MainHID:").append(mainHid.getUnitId());
							log.append(", State:").append(mainHid.getState());
							log.append(", VehicleCount:").append(mainHid.getVehicleCount());
							log.append(", Limit:").append(vehicleCountPerHid);
							log.append(", isIdle:").append(isIdle);
							log.append(", VHLReq:").append(vehicle.getRequestedType());
//							trace(log.toString());
							trace(vehicle.getVehicleId(), log.toString());
							return searchEscapeForAbnormalHid(vehicle, mainHid, null);
						}
					}
				} else {
					// Main 정상 (Run or Timeout)
					// 2014.07.25 by MYM : HIDLimitOverPass로 Limit 초과시 작업 OHT는 탈출하지 않도록 주석 처리
					// 배경 : Unloaded -> IDLE -> Limit 초과되어 EscapeSearch 된 경우 LOAD_ASSIGNED로 상태 변경 안함.
					//       GoMode에서 탈출 → LoadSearch → 마지막 목적지 주행 명령 전송시 DatalogicError 발생하면
					//       재전을 하지 못하는 현상 발생
//					if (mainHid.getVehicleCount() > vehicleCountPerHid) {
//						StringBuilder log = new StringBuilder();
//						log.append("[Main Capacity Over, All VHL Escape] ");
//						log.append("MainHID:").append(mainHid.getUnitId());
//						log.append(", State:").append(mainHid.getState());
//						log.append(", VehicleCount:").append(mainHid.getVehicleCount());
//						log.append(", Limit:").append(vehicleCountPerHid);
//						log.append(", isIdle:").append(isIdle);
////						trace(log.toString());
//						trace(vehicle.getVehicleId(), log.toString());
//						// Main Capacity Full.
//						return searchEscapeForAbnormalHid(vehicle, mainHid, null);
//					} else if (isIdle && mainHid.getVehicleCount() >= vehicleCountPerHid) {
					if (isIdle && mainHid.getVehicleCount() >= vehicleCountPerHid) {
						StringBuilder log = new StringBuilder();
						log.append("[Main Capacity Full, Idle VHL Escape] ");
						log.append("MainHID:").append(mainHid.getUnitId());
						log.append(", State:").append(mainHid.getState());
						log.append(", VehicleCount:").append(mainHid.getVehicleCount());
						log.append(", Limit:").append(vehicleCountPerHid);
						log.append(", isIdle:").append(isIdle);
						log.append(", VHLReq:").append(vehicle.getRequestedType());
//						trace(log.toString());
						trace(vehicle.getVehicleId(), log.toString());
						// Main Capacity Full이 우려되는 경우, Idle은 해당 HID 영역에서 배출.
						return searchEscapeForAbnormalHid(vehicle, mainHid, null);
					} else { // 2022.08.17 by Y.Won : HID 영역에서 배출할 필요 없는 경우
						vehicle.setHIDEscapePathSearchFailed(false);
					}
				}
			}
		} catch (Exception e) {
			traceException("searchEscapeForAbnormalHid()", e);
		} finally {
			if ((System.currentTimeMillis()-processTime) >= 100) {
				StringBuffer log = new StringBuffer();
				log.append(" searchEscapeForAbnormalHid() TimeDelay=").append(System.currentTimeMillis()-processTime).append(" ").append(vehicle.getVehicleId());
//				trace(log.toString());
				trace(vehicle.getVehicleId(), log.toString());
			}
		}
		
		return retVal;
	}
	
	/**
	 * search path for a vehicle to come back to it's zone
	 * 
	 * @param vehicle VehicleData
	 * @return result boolean
	 */
	public boolean searchVehicleComebackZonePath(VehicleData vehicle) {
		if (vehicle == null) {
			return false;
		}
		
		// PathSearch 요청 초기화 
		vehicle.setPathSearchRequest(false);
		
		long processTime = System.currentTimeMillis();
		boolean retVal = false;
		try {
			retVal = searchComebackZonePath(vehicle);
		} catch (Exception e) {
			traceException("searchVehicleComebackZonePath()", e);
		} finally {
			if ((System.currentTimeMillis()-processTime) >= 100) {
				StringBuffer log = new StringBuffer();
//				log.append(" searchVehicleComebackZonePath() TimeDelay=").append(System.currentTimeMillis()-processTime).append(" ").append(vehicle.getVehicleId());
//				trace(log.toString());
				log.append("searchVehicleComebackZonePath() TimeDelay=").append(System.currentTimeMillis()-processTime);
				trace(vehicle.getVehicleId(), log.toString());
			}
		}
		return retVal;
	}
	
	/**
	 * 
	 * 
	 * @param vehicle VehicleData
	 * @return result boolean
	 */
	private boolean searchYieldPathMin(VehicleData vehicle) {
		Node fromNode = null;
		fromNode = vehicle.getDriveStopNode();
		// fromNode, toNode 확인
		if (fromNode == null) {
			return false;
		}
		// 2013.02.22 by KYK
		double arrivedTimeToNext = 0.0;
		if (isSearchedFromNextNode(vehicle)) {
			Node prevNode = fromNode;
			fromNode = getNextNodeOfStation(vehicle.getStopStation());
			// 2013.10.03 by KYK : check node abnormal
			if (checkAbnormalState(fromNode,prevNode,vehicle)) {
				return false;
			}
			double time = prevNode.getMoveInTime(fromNode);
			double distance = prevNode.getMoveInDistance(fromNode);
			double speed = distance / time;
			if (time > 0 && distance > 0 && speed > 0) {
				arrivedTimeToNext = (distance - vehicle.getCurrNodeOffset()) / speed;				
			}
		}	

		// vehicle route 초기화
		String vehicleId = vehicle.getVehicleId();
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//		initRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
		TrCmd trCmd = trCmdManager.getAssignedTrCmd(vehicleId);
		
		// toNode까지 경로 탐색
		LinkedList<Node> queueNodeList = new LinkedList<Node>();
		TreeMap<Double, Node> candidateNode = new TreeMap<Double, Node>();
		TreeMap<Double, Node> tempCandidateNode = new TreeMap<Double, Node>();
		Vector<Node> tempSectionCandidateNode = new Vector<Node>();
		addQueueNode(vehicleId, fromNode, queueNodeList);
		
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
		LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
		resetRouteNodeList.add(fromNode);
		
		Node prevNode = null;
		Node searchNode = null;
		boolean found = false;
		
		// 2013.02.22 by KYK
//		double arrivedTime = 0;
		double arrivedTime = arrivedTimeToNext;
		double moveTime = 0;
		long distance = 0;
		fromNode.setArrivedTime(vehicleId, arrivedTime);
		int loopCount = 0;
		int sectionIndex = 0;
		
		VehicleData yieldRequestedVehicle = vehicle.getYieldRequestedVehicle();
		String vehicleZone = vehicle.getZone();
//		String prevNodeZone = "";
//		String nodeZone = "";
		StringBuffer route = new StringBuffer();
		
		boolean isException = false;
		try {
			updateInTheWayNodeList(yieldRequestedVehicle);
			while (true) {
				loopCount++;
				searchNode = getQueuedNode(queueNodeList);
				if (searchNode == null)
					break;
				
				for (int i = 0; i < searchNode.getSectionCount(); i++) {
					loopCount++;
//					arrivedTime = searchNode.getArrivedTime(vehicleId);
					NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
					if (arrivedTimeInfo != null) {
						arrivedTime = arrivedTimeInfo.getArrivedTime();
						moveTime = arrivedTimeInfo.getMoveTime();
						distance = arrivedTimeInfo.getDistance();
						sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
					} else {
						arrivedTime = MAXCOST_TIMEBASE;
						moveTime = MAXCOST_TIMEBASE;
						distance = MAXCOST_DISTANCEBASE;
					}
					
					Section section = searchNode.getSection(i);
					prevNode = searchNode;
					int pos = section.getNodeIndex(searchNode);
					tempCandidateNode.clear();
					tempSectionCandidateNode.clear();
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						loopCount++;
						Node node = section.getNode(j);
						
						// 2015.03.04 by MYM : checkAbnormalSearch로 일원화
//						if (node.isEnabled() == false) {
//							tempSectionCandidateNode.clear();
//							break;
//						}
//						
//						prevNodeZone = prevNode.getZone();
//						nodeZone = node.getZone();
//						if (prevNodeZone.equals(nodeZone) == false) {
//							if (isDriveAllowed(vehicleZone, nodeZone) == false ||
//									isYieldAllowed(vehicleZone, nodeZone) == false) {
//								tempSectionCandidateNode.clear();
//								break;
//							}
//							arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
//						}
//						
//						if (fromNode.equals(node) ||
//								node.hasAlreadyDrived(vehicle)) {
//							tempSectionCandidateNode.clear();
//							break;
//						}
//						
//						// 2012.07.09 by PMM
//						// 같은 Section 내에서 Abnormal State 발생 시, 양보 안되는 케이스 발생.
//						if (node.isAbnormalState(vehicle, isFailureOHTDetourSearchUsed) || containsAbnormalCollision(node)) {
//							double cost = 0.0;
//							Node tempPrevNode = null;
//							for (Node candidate : tempSectionCandidateNode) {
//								cost = candidate.getArrivedTime(vehicleId) + 3000;
//								tempPrevNode = candidate.getPrevNode(vehicleId);
//								candidate.changeArrivedTime(vehicleId, tempPrevNode, cost);
//							}
//							break;
//						}
//						
//						// 2014.09.24 by KYK : 근접제어에서도 DeadLock Break 우회주행하도록
//						if (vehicle.getDetourNode() != null &&
//								vehicle.getDetourNode() == node) {
//							break;
//						}
						if (isNearByDrive == false) {
							if (node.isCloseloopSet()) {
								arrivedTime += 50;
							}
						}
						
						// 2015.03.04 by MYM : 순수이동시간(moveTime), 순수 이동 거리(distance) 로그 기록
//						arrivedTime += node.getMoveInTime(prevNode) + node.getTrafficPenalty();
//						
//						if (arrivedTime >= OcsConstant.MAXCOST_TIMEBASE) {
//							arrivedTime = OcsConstant.MAXCOST_TIMEBASE;
//							tempSectionCandidateNode.clear();
//							break;
//						}
						double moveInTime = node.getMoveInTime(prevNode);
						arrivedTime += moveInTime + node.getTrafficPenalty();
						moveTime += moveInTime;
						distance += node.getMoveInDistance(prevNode);
						
						double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, sectionIndex, route, section, j == 1);
						if (time == MAXCOST_TIMEBASE || isYieldAllowed(vehicleZone, node.getZone()) == false) {
							if (route.indexOf("AbnormalState") >= 0) {
								double cost = 0.0;
								Node tempPrevNode = null;
								for (Node candidate : tempSectionCandidateNode) {
									cost = candidate.getArrivedTime(vehicleId) + 3000;
									tempPrevNode = candidate.getPrevNode(vehicleId);
									candidate.changeArrivedTime(vehicleId, tempPrevNode, cost);
								}
							} else {
								tempSectionCandidateNode.clear();
							}
							break;
						} else {
							arrivedTime = time;
						}
						
						// 2014.02.03 by MYM : Disabled Link 처리
//						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime)) {
						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, 0, moveTime, distance, section, sectionIndex)) {
							prevNode = node;
							arrivedTime = node.getArrivedTime(vehicleId);
							
							if (node.isConverge() == false && node.isDiverge() == false &&
									arrivedTime > yieldMinLimitTime && 
									arrivedTime < OcsConstant.MAXCOST_TIMEBASE &&
									node.isStopAllowed()) {
								tempSectionCandidateNode.add(node);
							}
							
							if (j == section.getNodeCount() - 1) {
								addQueueNode(vehicleId, node, queueNodeList);
								// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
								resetRouteNodeList.add(node);
							}
						} else {
							break;
						}
					}
					if (tempSectionCandidateNode.size() > 0) {
						double cost = 0.0;
						for (Node candidate : tempSectionCandidateNode) {
							cost = candidate.getArrivedTime(vehicleId);
							
							if (PATROL_VEHICLEZONE.equals(vehicle.getZone())) {
								if (isNotInTheWay(candidate)) {
									cost += 50;
								} else {
									cost += 500;
								}
								if (fromNode.getBay().equals(candidate.getBay()) == false) {
									cost += 3000;
								}
							} else {
								if (isNotInTheWay(candidate)) {
									cost += 50 * (candidate.getDriveVehicleCount() + 20 * candidate.getApproachingVehicleIndex()); 
								} else {
									cost += 1000 + 50 * (candidate.getDriveVehicleCount() + 20 * candidate.getApproachingVehicleIndex()); 
								}
							}
							
							if (vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0 ||
									(trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE)) {
								// LocalOHT는 Bay 내 양보, Bay내 양보 불가 시 Bay외 양보.
								if (fromNode.getBay().equals(candidate.getBay()) == false) {
									cost += 2000;
								}
							}
							if (isNearByDrive == false) {
								if (candidate.isCloseloopSet()) {
									cost += 1000;
								}
							}
							if (section.getNodeCount() < 4) {
								cost += 500;
							}
							
							tempCandidateNode.put(cost, candidate);
						}
						candidateNode.put(tempCandidateNode.firstKey(), tempCandidateNode.get(tempCandidateNode.firstKey()));
					}
					if (candidateNode.size() > 1) {
						found = true;
						break;
					}
				}
				if (found) {
					break;
				}
			}
			
			if (candidateNode.size() > 0) {
				LinkedList<Node> routedNodeList = new LinkedList<Node>();
				Node node = (Node)candidateNode.get(candidateNode.firstKey());
				arrivedTime = node.getArrivedTime(vehicleId);
				
				StringBuffer log = new StringBuffer();
//				log.append(vehicleId).append("'s Yield Searched(Min) From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(" : ").append(arrivedTime).append(", loopCnt : ").append(loopCount);
//				trace(log.toString());
				log.append("Yield Searched(Min) From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(", Cost:").append(arrivedTime).append(", loopCnt : ").append(loopCount);
				trace(vehicleId, log.toString());
				
				while (fromNode.equals(node) == false) {
					routedNodeList.add(0, node);
					node = node.getPrevNode(vehicleId);
					if (node == null) {
						break;
					}
				}
				if (node != null) {
					routedNodeList.add(0, node);
				}
				return vehicle.setRoutedNodeList(routedNodeList);
			}
		} catch (Exception e) {
			traceException("searchYieldPathMin()", e);
			isException = true;
		} finally {
			vehicle.resetDetourNode();
			inTheWayNodeSet.clear();
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
			if (isException) {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
			} else {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
			}
		}
		return false;
	}
	
	/**
	 * 
	 * 
	 * @param node Node
	 * @param yieldRequestedVehicle VehicleData
	 * @return result boolean
	 */
//	private boolean isNotInTheWay(Node node, VehicleData yieldRequestedVehicle) {
//		if (yieldRequestedVehicle != null) {
//			if (yieldRequestedVehicle.containsRoutedNode(node) == false &&
//					yieldRequestedVehicle.containsDriveNode(node) == false) {
//				if (isNearByDrive == false) {
//					ArrayList<Collision> collisionList = node.getCollisions();
//					if (collisionList != null && collisionList.size() > 0) {
//						Collision collision;
//						Node collisionNode = null;
//						Iterator<Collision> iterator = collisionList.iterator();
//						while (iterator.hasNext()) {
//							collision = (Collision)iterator.next();
//							if (collision != null) {
//								collisionNode = collision.getCollisionNode(node);
//								if (collisionNode != null) {
//									if (yieldRequestedVehicle.containsRoutedNode(collisionNode) ||
//											yieldRequestedVehicle.containsDriveNode(collisionNode)) {
//										return false;
//									}
//								}
//							}
//						}
//					}
//				}
//				return true;
//			}
//		}
//		return false;
//	}
	
	private boolean isNotInTheWay(Node node) {
		if (node != null) {
			if (inTheWayNodeSet.contains(node)) {
				return false;
			}
		}
		return true;
	}
	
	private HashSet<Node> tempNodeSet = new HashSet<Node>();
	private void updateInTheWayNodeList(VehicleData yieldRequestedVehicle) {
		inTheWayNodeSet.clear();
		tempNodeSet.clear();
		if (yieldRequestedVehicle != null) {
			yieldRequestedVehicle.getInTheWay(inTheWayNodeSet);
			if (isNearByDrive == false) {
				for (Node inTheWayNode : inTheWayNodeSet) {
					if (inTheWayNode != null) {
						ArrayList<Collision> collisionList = inTheWayNode.getCollisions();
						if (collisionList != null && collisionList.size() > 0) {
							Collision collision;
							Node collisionNode = null;
							Iterator<Collision> iterator = collisionList.iterator();
							while (iterator.hasNext()) {
								collision = (Collision)iterator.next();
								if (collision != null) {
									collisionNode = collision.getCollisionNode(inTheWayNode);
									if (collisionNode != null) {
										tempNodeSet.add(collisionNode);
									}
								}
							}
						}
					}
				}
				for (Node tempNode : tempNodeSet) {
					if (tempNode != null) {
						inTheWayNodeSet.add(tempNode);
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * 
	 * @param vehicle VehicleData
	 * @return result boolean
	 */
	private boolean searchYieldPath(VehicleData vehicle) {
		Node fromNode = null;
		fromNode = vehicle.getDriveStopNode();
		// fromNode, toNode 확인
		if (fromNode == null) {
			return false;
		}
		// 2013.02.22 by KYK
		double arrivedTimeToNext = 0.0;
		if (isSearchedFromNextNode(vehicle)) {
			Node prevNode = fromNode;
			fromNode = getNextNodeOfStation(vehicle.getStopStation());
			// 2013.10.03 by KYK : check node abnormal
			if (checkAbnormalState(fromNode,prevNode,vehicle)) {
				return false;
			}
			double time = prevNode.getMoveInTime(fromNode);
			double distance = prevNode.getMoveInDistance(fromNode);
			double speed = distance / time;
			if (time > 0 && distance > 0 && speed > 0) {
				arrivedTimeToNext = (distance - vehicle.getCurrNodeOffset()) / speed;				
			}
		}	

		// vehicle route 초기화
		String vehicleId = vehicle.getVehicleId();
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//		initRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
		TrCmd trCmd = trCmdManager.getAssignedTrCmd(vehicleId);
		
		// toNode까지 경로 탐색
		LinkedList<Node> queueNodeList = new LinkedList<Node>();
		TreeMap<Double, Node> candidateNodeMap = new TreeMap<Double, Node>();
		TreeMap<Double, Node> tempCandidateNodeMap = new TreeMap<Double, Node>();
		Vector<Node> tempSectionCandidateNodeList = new Vector<Node>();
		addQueueNode(vehicleId, fromNode, queueNodeList);
		
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
		LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
		resetRouteNodeList.add(fromNode);
		
		Node prevNode = null;
		Node searchNode = null;
		boolean found = false;

		// 2013.02.22 by KYK
//		double arrivedTime = 0;
		double arrivedTime = arrivedTimeToNext;
		double moveTime = 0;
		long distance = 0;
		fromNode.setArrivedTime(vehicleId, arrivedTime);
		int loopCount = 0;
		int sectionCount = 0;
		int sectionIndex = 0;
		
		VehicleData yieldRequestedVehicle = vehicle.getYieldRequestedVehicle();
		String vehicleZone = vehicle.getZone();
//		String prevNodeZone = "";
//		String nodeZone = "";
		StringBuffer route = new StringBuffer();
		
		boolean isException = false;
		try {
			updateInTheWayNodeList(yieldRequestedVehicle);
			while (true) {
				loopCount++;
				searchNode = getQueuedNode(queueNodeList);
				if (searchNode == null)
					break;
				
				for (int i = 0; i < searchNode.getSectionCount(); i++) {
					sectionCount++;
					loopCount++;
//					arrivedTime = searchNode.getArrivedTime(vehicleId);
					NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
					if (arrivedTimeInfo != null) {
						arrivedTime = arrivedTimeInfo.getArrivedTime();
						moveTime = arrivedTimeInfo.getMoveTime();
						distance = arrivedTimeInfo.getDistance();
						sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
					} else {
						arrivedTime = MAXCOST_TIMEBASE;
						moveTime = MAXCOST_TIMEBASE;
						distance = MAXCOST_DISTANCEBASE;
					}
					
					Section section = searchNode.getSection(i);
					prevNode = searchNode;
					int pos = section.getNodeIndex(searchNode);
					tempCandidateNodeMap.clear();
					tempSectionCandidateNodeList.clear();
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						loopCount++;
						Node node = section.getNode(j);
						
						// 2015.03.04 by MYM : checkAbnormalSearch로 일원화
//						if (node.isEnabled() == false) {
//							tempSectionCandidateNodeList.clear();
//							break;
//						}
//
//						// 2015.02.10 by zzang9un : PassDoor가 Disable된 node는 Search 안되도록 함
//						PassDoor pb = null;
////						pb = passDoorManager.findPassDoor(node.getNodeId()); 
//						pb = node.getPassDoor(); 
//						if (pb != null && pb.checkPassable() == false) {
//							trace(vehicle.getVehicleId(), "Can't pass PassDoor(" + pb.getPassDoorId() + ")");
//							tempSectionCandidateNodeList.clear();
//							break;
//						}
//						
////						prevNodeZone = prevNode.getZone();
//						nodeZone = node.getZone();
//						
////						if (prevNodeZone.equals(nodeZone) == false) {
//						if (prevNode.getZoneIndex() != node.getZoneIndex()) {
//							if (isDriveAllowed(vehicleZone, nodeZone) == false ||
//									isYieldAllowed(vehicleZone, nodeZone) == false) {
//								tempSectionCandidateNodeList.clear();
//								break;
//							}
//							arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
//						}
//						
//						if (fromNode.equals(node) ||
//								node.hasAlreadyDrived(vehicle)) {
//							tempSectionCandidateNodeList.clear();
//							break;
//						}
//						
//						// 2012.07.09 by PMM
//						if (node.isAbnormalState(vehicle, isFailureOHTDetourSearchUsed) || containsAbnormalCollision(node)) {
//							double cost = 0.0;
//							Node tempPrevNode = null;
//							for (Node candidate : tempSectionCandidateNodeList) {
//								cost = candidate.getArrivedTime(vehicleId) + 3000;
//								tempPrevNode = candidate.getPrevNode(vehicleId);
//								candidate.changeArrivedTime(vehicleId, tempPrevNode, cost);
//							}
//							break;
//						} 
//						// 2014.09.24 by KYK : 근접제어에서도 DeadLock Break 우회주행하도록
////						if (isNearByDrive == false) {
////							if (vehicle.getDetourNode() != null &&
////									vehicle.getDetourNode() == node) {
////								break;
////							}
////							if (node.isCloseloopSet()) {
////								arrivedTime += 50;
////							}
////						}
//						if (vehicle.getDetourNode() != null &&
//								vehicle.getDetourNode() == node) {
//							break;
//						}
						if (isNearByDrive == false) {
							if (node.isCloseloopSet()) {
								arrivedTime += 50;
							}
						}
						
						// 2015.03.04 by MYM : 순수이동시간(moveTime), 순수 이동 거리(distance) 로그 기록
//						arrivedTime += node.getMoveInTime(prevNode) + node.getTrafficPenalty();
//						
//						index = prevNode.getIndex(vehicleId) + 1;
//						arrivedTime += getCongestionPenalty(node, arrivedTime, index);
//						
//						if (arrivedTime >= OcsConstant.MAXCOST_TIMEBASE) {
//							arrivedTime = OcsConstant.MAXCOST_TIMEBASE;
//							tempSectionCandidateNodeList.clear();
//							break;
//						}
						double moveInTime = node.getMoveInTime(prevNode);
//						arrivedTime += moveInTime + node.getTrafficPenalty();
						// 2015.05.27 by MYM : Vehicle Traffic 분산
						if (isTrafficUpdateUsed) {
							double trafficCost = 0, trafficRatio = 1;
							Traffic traffic = section.getTraffic();
							if (traffic != null) {
								if (traffic.getType() == TRAFFIC_UPDATE_RULE.PULL) {
									trafficRatio = traffic.getPullTrafficRatio();
								} else if (traffic.getType() == TRAFFIC_UPDATE_RULE.PUSH) {
									trafficCost = traffic.getPushTrafficCost(section);
								}
							}
							arrivedTime += (moveInTime * trafficRatio) + node.getTrafficPenalty() + trafficCost;
						} else {
							arrivedTime += moveInTime + node.getTrafficPenalty();
						}
						moveTime += moveInTime;
						distance += node.getMoveInDistance(prevNode);
						
						if (isDynamicRoutingUsed) {
							arrivedTime += getCongestionPenalty(node, arrivedTime, prevNode.getIndex(vehicleId) + 1);
						}
						
						double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, sectionIndex, route, section, j == 1);
						if (time == MAXCOST_TIMEBASE || isYieldAllowed(vehicleZone, node.getZone()) == false) {
							if (route.indexOf("AbnormalState") >= 0) {
								double cost = 0.0;
								Node tempPrevNode = null;
								for (Node candidate : tempSectionCandidateNodeList) {
									cost = candidate.getArrivedTime(vehicleId) + 3000;
									tempPrevNode = candidate.getPrevNode(vehicleId);
									candidate.changeArrivedTime(vehicleId, tempPrevNode, cost);
								}
							} else {
								tempSectionCandidateNodeList.clear();
							}
							break;
						} else {
							arrivedTime = time;
						}
						
						// 2015.06.08 by MYM : 위치이동(arrivedTime set 안 됨), 우회 양보인 경우는 부가 Penalty 부여
						if (vehicle.isDetourYieldRequested() && node == vehicle.getDriveFailedNode()) {
							arrivedTime += 3000;
						}
						
						// 2014.02.03 by MYM : Disabled Link 처리
//						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime)) {
						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, 0, moveTime, distance, section, sectionIndex)) {
							prevNode = node;
							arrivedTime = node.getArrivedTime(vehicleId);
							
							if ((section.getNodeCount() == 2 || (node.isConverge() == false && node.isDiverge() == false)) &&
									arrivedTime > yieldMinLimitTime && 
									arrivedTime < OcsConstant.MAXCOST_TIMEBASE &&
									node.isStopAllowed()) {
								tempSectionCandidateNodeList.add(node);
							}
							
							if (j == section.getNodeCount() - 1) {
								addQueueNode(vehicleId, node, queueNodeList);
								// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
								resetRouteNodeList.add(node);
							}
						} else {
							break;
						}
					}
					if (tempSectionCandidateNodeList.size() > 0) {
						double cost = 0.0;
						double minCost = 9999;
						for (Node candidate : tempSectionCandidateNodeList) {
							cost = candidate.getArrivedTime(vehicleId);
							
							// 2015.06.08 by MYM,KYK,zzang9un : 우회 양보인 경우는 부가 Penalty 미부여(driveFailedNode로 양보하는 경우가 있음)
							if (vehicle.isDetourYieldRequested() == false) {
								if (PATROL_VEHICLEZONE.equals(vehicle.getZone())) {
									if (isNotInTheWay(candidate)) {
										cost += 50;
									} else {
										cost += 500;
									}
									if (fromNode.getBay().equals(candidate.getBay()) == false) {
										cost += 3000;
									}
								} else {
									if (isNotInTheWay(candidate)) {
										cost += 50 * (candidate.getDriveVehicleCount() + 30 * candidate.getApproachingVehicleIndex());
									} else {
										cost += 1000 + 50 * (candidate.getDriveVehicleCount() + 30 * candidate.getApproachingVehicleIndex()); 
									}
								}

								if (vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0 ||
										(trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE)) {
									// LocalOHT는 Bay 내 양보, Bay내 양보 불가 시 Bay외 양보.
									if (fromNode.getBay().equals(candidate.getBay()) == false) {
										cost += 2000;
									}
								}
							}
							
							if (isNearByDrive == false) {
								if (candidate.isCloseloopSet()) {
									cost += 1000;
								}
							}
							if (section.getNodeCount() < 4) {
								cost += 500;
							}
							tempCandidateNodeMap.put(cost, candidate);
							if (cost < minCost) {
								minCost = cost;
							}
						}
						if (candidateNodeMap.size() == 0 || minCost < 1000) {
							candidateNodeMap.put(tempCandidateNodeMap.firstKey(), tempCandidateNodeMap.get(tempCandidateNodeMap.firstKey()));
						}
					}
					if (candidateNodeMap.size() >= 10 || (candidateNodeMap.size() > 0 && sectionCount > 20)) {
						found = true;
						break;
					}
				}
				if (found) {
					break;
				}
			}
			
			if (candidateNodeMap.size() > 0) {
				LinkedList<Node> routedNodeList = new LinkedList<Node>();
				Node node = (Node)candidateNodeMap.get(candidateNodeMap.firstKey());
				arrivedTime = node.getArrivedTime(vehicleId);
				
				// 2011.10.26 by PMM
				// 검색 결과 로그 위치 이동
				StringBuffer log = new StringBuffer();
//				log.append(vehicleId).append("'s Yield Searched From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(" : ").append(arrivedTime).append(", loopCnt : ").append(loopCount);
//				trace(log.toString());
				log.append("Yield Searched From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(", Cost:").append(arrivedTime).append(", loopCnt : ").append(loopCount);
				trace(vehicleId, log.toString());
				
				while (fromNode.equals(node) == false) {
					routedNodeList.add(0, node);
					node = node.getPrevNode(vehicleId);
					if (node == null) {
						break;
					}
				}
				if (node != null) {
					routedNodeList.add(0, node);
				}
				
				return vehicle.setRoutedNodeList(routedNodeList);
			}
		} catch (Exception e) {
			traceException("searchYieldPath()", e);
			isException = true;
		} finally {
			vehicle.resetDetourNode();
			inTheWayNodeSet.clear();
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
			if (isException) {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
			} else {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
			}
		}
		return false;
	}
	
	/**
	 * 
	 * 
	 * @param vehicle VehicleData
	 * @return result boolean
	 */
	private boolean searchComebackZonePath(VehicleData vehicle) {
		long pathSearchStartedTime = System.nanoTime();
		
		Node fromNode = null;
		fromNode = vehicle.getDriveStopNode();
		
		// fromNode, toNode 확인
		if (fromNode == null) {
			return false;
		}
		// 2013.02.22 by KYK
		if (isSearchedFromNextNode(vehicle)) {
			Node currNode = fromNode;
			fromNode = getNextNodeOfStation(vehicle.getStopStation());
			// 2013.10.03 by KYK : check node abnormal
			if (checkAbnormalState(fromNode,currNode,vehicle)) {
				return false;
			}
		}	

		// vehicle route 초기화
		String vehicleId = vehicle.getVehicleId();
//		String vehicleZone = vehicle.getZone();
		int vehicleZoneIndex = vehicle.getZoneIndex();
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//		initRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
		
		// toNode까지 경로 탐색
		LinkedList<Node> queueNodeList = new LinkedList<Node>();
		addQueueNode(vehicleId, fromNode, queueNodeList);
		
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
		LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
		resetRouteNodeList.add(fromNode);
		
		Node prevNode = null;
		Node searchNode = null;
		Node toNode = null;
		boolean arrived = false;
		double arrivedTime = 0;
		double moveTime = 0;
		long distance = 0;
		int sectionIndex = 0;
		fromNode.setArrivedTime(vehicleId, arrivedTime);

//		String prevNodeZone = "";
//		String nodeZone = "";
		StringBuffer route = new StringBuffer();
		
		boolean isException = false;
		try {
			while (true) {			
				searchNode = getQueuedNode(queueNodeList);
				if (searchNode == null)
					break;
				
				for (int i = 0; i < searchNode.getSectionCount(); i++) {
//					arrivedTime = searchNode.getArrivedTime(vehicleId);
					NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
					if (arrivedTimeInfo != null) {
						arrivedTime = arrivedTimeInfo.getArrivedTime();
						moveTime = arrivedTimeInfo.getMoveTime();
						distance = arrivedTimeInfo.getDistance();
						sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
					} else {
						arrivedTime = MAXCOST_TIMEBASE;
						moveTime = MAXCOST_TIMEBASE;
						distance = MAXCOST_DISTANCEBASE;
					}
					
					Section section = searchNode.getSection(i);
					prevNode = searchNode;
					int pos = section.getNodeIndex(searchNode);
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						Node node = section.getNode(j);
//						if (fromNode.equals(node)) {
//							break;
//						}
//						
//						// 2014.02.08 by KYK
//						if (node.isAbnormalState(vehicle, isFailureOHTDetourSearchUsed)) {
//							break;
//						}
//						prevNodeZone = prevNode.getZone();
//						nodeZone = node.getZone();
//						if (prevNodeZone.equals(nodeZone) == false) {
//							if (isDriveAllowed(vehicleZone, nodeZone) == false) {
//								break;
//							}
//							arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
//						}
						
//						arrivedTime += node.getMoveInTime(prevNode) + node.getTrafficPenalty();
						double moveInTime = node.getMoveInTime(prevNode);
						arrivedTime += moveInTime + node.getTrafficPenalty();
						moveTime += moveInTime;
						distance += node.getMoveInDistance(prevNode);
						
						// 2015.01.12 by MYM : PathSearch시 Abnormal 케이스 함수 일원화
						double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, sectionIndex, route, section, j == 1);
						if (time == MAXCOST_TIMEBASE) {
							break;
						} else {
							arrivedTime = time;
						}
						
						// 2014.02.03 by MYM : Disabled Link 처리
//						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime)) {
						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, toNode), moveTime, distance, section, sectionIndex)) {
							prevNode = node;
//							arrivedTime = node.getArrivedTime(vehicleId);
							arrivedTimeInfo = node.getArrivedTimeInfo(vehicleId);
							if (arrivedTimeInfo != null) {
								arrivedTime = arrivedTimeInfo.getArrivedTime();
								moveTime = arrivedTimeInfo.getMoveTime();
								distance = arrivedTimeInfo.getDistance();
							}
							
							// 2014.07.26 by MYM : VehicleZone과 NodeZone 비교 조건 추가
							// 배경 : 위에서 nodeZone을 설정하는 부분을 주석처리하여 vehicleZone.equals(nodeZone) 조건이 맞지 않아 ComebackZone 안됨.
							// 2012.03.12 by PMM
//							if (node.isVirtual() == false && vehicleZone.equals(node.getZone())) {
//							if (vehicleZone.equals(nodeZone) && node.isStopAllowed()) {
							if (vehicleZoneIndex == node.getZoneIndex() && node.isStopAllowed()) {
								arrived = true;
								toNode = node;
								StringBuffer log = new StringBuffer();
								log.append("ComebackZone Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId());
								log.append(", Cost:").append(arrivedTime).append(", Time:").append(moveTime).append(", Distance:").append(distance);
								log.append(", Time:").append((double)((System.nanoTime() - pathSearchStartedTime)/1000)/1000).append(" ms");
								trace(vehicleId, log.toString());
								break;
							}
							
							if (j == section.getNodeCount()-1) {
								addQueueNode(vehicleId, node, queueNodeList);
								// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
								resetRouteNodeList.add(node);
							}
						} else {
							break;
						}
					}
					
					if (arrived == true) {
						break;
					}
				}
			}
			
			if (arrived == true) {
				LinkedList<Node> routedNodeList = new LinkedList<Node>();
				
				Node node = toNode;
				while (fromNode.equals(node) == false) {
					routedNodeList.add(0, node);
					node = node.getPrevNode(vehicleId);
				}
				routedNodeList.add(0, node);
				return vehicle.setRoutedNodeList(routedNodeList);
			}
		} catch (Exception e) {
			traceException("searchVehicleComebackZonePath()", e);
			isException = true;
		} finally {
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
			if (isException) {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
			} else {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
			}
		}
		return false;
	}
	
	/**
	 * 
	 * 
	 * @param vehicle VehicleData
	 * @param localGroupBay String
	 * @return result boolean
	 */
	private boolean searchLocalVehicleComebackBayPath(VehicleData vehicle, String localGroupBay) {
		long pathSearchStartedTime = System.nanoTime();
		
		Node fromNode = null;
		fromNode = vehicle.getDriveStopNode();
		
		// fromNode, toNode 확인
		if (fromNode == null) {
			return false;
		}
		// 2013.02.22 by KYK
		if (isSearchedFromNextNode(vehicle)) {
			Node currNode = fromNode;
			fromNode = getNextNodeOfStation(vehicle.getStopStation());
			// 2013.10.03 by KYK : check node abnormal
			if (checkAbnormalState(fromNode,currNode,vehicle)) {
				return false;
			}
		}	

		// vehicle route 초기화
		String vehicleId = vehicle.getVehicleId();
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//		initRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
		
		// toNode까지 경로 탐색
		LinkedList<Node> queueNodeList = new LinkedList<Node>();
		addQueueNode(vehicleId, fromNode, queueNodeList);
		
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
		LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
		resetRouteNodeList.add(fromNode);
		
		Node prevNode = null;
		Node searchNode = null;
		Node toNode = null;
		boolean arrived = false;
		double arrivedTime = 0;
		double moveTime = 0;
		long distance = 0;
		int sectionIndex = 0;
		fromNode.setArrivedTime(vehicleId, arrivedTime);
		
//		String vehicleZone = vehicle.getZone();
//		String prevNodeZone = "";
//		String nodeZone = "";
		StringBuffer route = new StringBuffer();
		
		boolean isException = false;
		try {
			while (true) {			
				searchNode = getQueuedNode(queueNodeList);
				if (searchNode == null)
					break;
				
				for (int i = 0; i < searchNode.getSectionCount(); i++) {
//					arrivedTime = searchNode.getArrivedTime(vehicleId);
					NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
					if (arrivedTimeInfo != null) {
						arrivedTime = arrivedTimeInfo.getArrivedTime();
						moveTime = arrivedTimeInfo.getMoveTime();
						distance = arrivedTimeInfo.getDistance();
						sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
					} else {
						arrivedTime = MAXCOST_TIMEBASE;
						moveTime = MAXCOST_TIMEBASE;
						distance = MAXCOST_DISTANCEBASE;
					}
					
					Section section = searchNode.getSection(i);
					prevNode = searchNode;
					int pos = section.getNodeIndex(searchNode);
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						Node node = section.getNode(j);
//						if (fromNode.equals(node)) {
//							break;
//						}
//						
//						// 2014.02.08 by KYK
////						prevNodeZone = prevNode.getZone();
////						nodeZone = node.getZone();
////						if (prevNodeZone.equals(nodeZone) == false) {
////							if (isDriveAllowed(vehicleZone, nodeZone) == false) {
////								break;
////							}
////							arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
////						}
//						if (node.isAbnormalState(vehicle, isFailureOHTDetourSearchUsed)) {
//							break;
//						}
//						arrivedTime = checkZoneAllowed(vehicle, prevNode, node, arrivedTime);
//						if (arrivedTime >= MAXCOST_TIMEBASE) {
//							break;
//						}
						
//						arrivedTime += node.getMoveInTime(prevNode) + node.getTrafficPenalty();
						double moveInTime = node.getMoveInTime(prevNode);
						arrivedTime += moveInTime + node.getTrafficPenalty();
						moveTime += moveInTime;
						distance += node.getMoveInDistance(prevNode);
						
						// 2015.01.12 by MYM : PathSearch시 Abnormal 케이스 함수 일원화
						double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, sectionIndex, route, section, j == 1);
						if (time == MAXCOST_TIMEBASE) {
							break;
						} else {
							arrivedTime = time;
						}
						
						// 2014.02.03 by MYM : Disabled Link 처리
//						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime)) {
						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, toNode), moveTime, distance, section, sectionIndex)) {
							prevNode = node;
//							arrivedTime = node.getArrivedTime(vehicleId);
							arrivedTimeInfo = node.getArrivedTimeInfo(vehicleId);
							if (arrivedTimeInfo != null) {
								arrivedTime = arrivedTimeInfo.getArrivedTime();
								moveTime = arrivedTimeInfo.getMoveTime();
								distance = arrivedTimeInfo.getDistance();
							}
							
							if (localGroupBay.equals(node.getBay()) && node.isStopAllowed()) {
								arrived = true;
								toNode = node;
								StringBuffer log = new StringBuffer();
//								log.append("[LocalOht]").append(vehicleId).append("'s ComebackBay Searched From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(" : ").append(arrivedTime);
//								trace(log.toString());
//								log.append("LocalOHT ComebackBay Searched From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append("(").append(localGroupBay).append("Bay) : ").append(arrivedTime);
								log.append("LocalOHT ComebackBay Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId());
								log.append(", Cost:").append(arrivedTime).append(", Time:").append(moveTime).append(", Distance:").append(distance);
								log.append(", Time:").append((double)((System.nanoTime() - pathSearchStartedTime)/1000)/1000).append(" ms");
								trace(vehicleId, log.toString());
								break;
							}
							
							if (j == section.getNodeCount()-1) {
								addQueueNode(vehicleId, node, queueNodeList);
								// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
								resetRouteNodeList.add(node);
							}
						} else {
							break;
						}
					}
					
					if (arrived == true) {
						break;
					}
				}
			}
			
			if (arrived == true) {
				LinkedList<Node> routedNodeList = new LinkedList<Node>();
				
				Node node = toNode;
				while (fromNode.equals(node) == false) {
					routedNodeList.add(0, node);
					node = node.getPrevNode(vehicleId);
				}
				routedNodeList.add(0, node);
				return vehicle.setRoutedNodeList(routedNodeList);
			}
		} catch (Exception e) {
			traceException("searchLocalVehicleComebackBayPath()", e);
			isException = true;
		}
		finally {
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
			if (isException) {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
			} else {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
			}
		}
		return false;
	}
	
	/**
	 * 
	 * 
	 * @param vehicle VehicleData
	 * @param mainHID Hid
	 * @param altHID Hid
	 * @return result boolean
	 */
	private boolean searchEscapeForAbnormalHid(VehicleData vehicle, Hid mainHID, Hid altHID) {
		long pathSearchStartedTime = System.nanoTime();
		
		Node fromNode = null;
		fromNode = vehicle.getDriveStopNode();
		
		// fromNode, toNode 확인
		if (fromNode == null) {
			return false;
		}
		// 2013.02.22 by KYK
		if (isSearchedFromNextNode(vehicle)) {
			// 2022.08.31 Y.Won
			Node currNode = fromNode;
			fromNode = getNextNodeOfStation(vehicle.getStopStation());
			// 2022.08.31 Y.Won: 길찾기 실패 시 vehicleData 의 isHIDEscapePathSearchFailed 를 true 로 설정
			if (checkAbnormalState(fromNode,currNode,vehicle)) {
				vehicle.setHIDEscapePathSearchFailed(true);
				return false;
			}
		}	

		// vehicle route 초기화
		String vehicleId = vehicle.getVehicleId();
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
//		initRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
		
		// toNode까지 경로 탐색
		LinkedList<Node> queueNodeList = new LinkedList<Node>();
		addQueueNode(vehicleId, fromNode, queueNodeList);
		
		// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
		LinkedList<Node> resetRouteNodeList = new LinkedList<Node>();
		resetRouteNodeList.add(fromNode);
		
		Node prevNode = null;
		Node searchNode = null;
		Node toNode = null;
		boolean arrived = false;
		double arrivedTime = vehicle.getArrivedTimeAtTargetNode();
		double moveTime = 0;
		long distance = 0;
		int sectionIndex = 0;
		fromNode.setArrivedTime(vehicleId, arrivedTime);
		Hid hid = null;
		
//		String vehicleZone = vehicle.getZone();
//		String prevNodeZone = "";
//		String nodeZone = "";
		StringBuffer route = new StringBuffer();
		
		boolean isException = false;
		try {
			while (true) {
				searchNode = getQueuedNode(queueNodeList);
				if (searchNode == null)
					break;
				
				for (int i = 0; i < searchNode.getSectionCount(); i++) {
//					arrivedTime = searchNode.getArrivedTime(vehicleId);
					NodeArrivedTimeInfo arrivedTimeInfo = searchNode.getArrivedTimeInfo(vehicleId);
					if (arrivedTimeInfo != null) {
						arrivedTime = arrivedTimeInfo.getArrivedTime();
						moveTime = arrivedTimeInfo.getMoveTime();
						distance = arrivedTimeInfo.getDistance();
						sectionIndex = arrivedTimeInfo.getSectionIndex() + 1;
					} else {
						arrivedTime = MAXCOST_TIMEBASE;
						moveTime = MAXCOST_TIMEBASE;
						distance = MAXCOST_DISTANCEBASE;
					}
					
					Section section = searchNode.getSection(i);
					prevNode = searchNode;
					int pos = section.getNodeIndex(searchNode);
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						Node node = section.getNode(j);
//						if (fromNode.equals(node)) {
//							break;
//						}
//
//						// 2014.02.08 by KYK
////						prevNodeZone = prevNode.getZone();
////						nodeZone = node.getZone();
////						if (prevNodeZone.equals(nodeZone) == false) {
////							if (isDriveAllowed(vehicleZone, nodeZone) == false) {
////								break;
////							}
////							arrivedTime += getZoneOverPenalty(vehicleZone, nodeZone);
////						}
//						if (node.isAbnormalState(vehicle, isFailureOHTDetourSearchUsed)) {
//							break;
//						}
//						arrivedTime = checkZoneAllowed(vehicle, prevNode, node, arrivedTime);
//						if (arrivedTime >= MAXCOST_TIMEBASE) {
//							break;
//						}
						
//						arrivedTime += node.getMoveInTime(prevNode) + node.getTrafficPenalty();
						double moveInTime = node.getMoveInTime(prevNode);
						arrivedTime += moveInTime + node.getTrafficPenalty();
						moveTime += moveInTime;
						distance += node.getMoveInDistance(prevNode);
						
						// 2015.01.12 by MYM : PathSearch시 Abnormal 케이스 함수 일원화
						double time = checkAbnormalSearch(vehicle, fromNode, prevNode, node, arrivedTime, sectionIndex, route, section, false);
						if (time == MAXCOST_TIMEBASE) {
							break;
						} else {
							arrivedTime = time;
						}
						
						// 2014.02.03 by MYM : Disabled Link 처리
//						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime)) {
						if (node.setArrivedTime(vehicleId, prevNode, arrivedTime, getMetropolitanTimeCost(node, toNode), moveTime, distance, section, sectionIndex)) {
							prevNode = node;
							hid = node.getHid();
//							arrivedTime = node.getArrivedTime(vehicleId);
							arrivedTimeInfo = node.getArrivedTimeInfo(vehicleId);
							if (arrivedTimeInfo != null) {
								arrivedTime = arrivedTimeInfo.getArrivedTime();
								moveTime = arrivedTimeInfo.getMoveTime();
								distance = arrivedTimeInfo.getDistance();
							}
							
							if ((hid.isHidDown() == false) &&
									(hid.getUnitId().equals(mainHID.getUnitId()) == false) &&
									node.isStopAllowed()) {
								if (altHID != null) {
									if (hid.getUnitId().equals(altHID.getUnitId()) == false) {
										toNode = node;
										arrived = true;
										
										StringBuffer log = new StringBuffer();
//										log.append(vehicleId).append("'s Escape Searched From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(" : ").append(arrivedTime);
//										trace(log.toString());
//										log.append("Escape Searched From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(" : ").append(arrivedTime);
										log.append("Escape Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId());
										log.append(", Cost:").append(arrivedTime).append(", Time:").append(moveTime).append(", Distance:").append(distance);
										log.append(", Time:").append((double)((System.nanoTime() - pathSearchStartedTime)/1000)/1000).append(" ms");
										trace(vehicleId, log.toString());
										break;
									}
								} else {
									toNode = node;
									arrived = true;
									
									StringBuffer log = new StringBuffer();
//									log.append(vehicleId).append("'s Escape Searched From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(" : ").append(arrivedTime);
//									trace(log.toString());
//									log.append("Escape Searched From ").append(fromNode.getNodeId()).append(" To ").append(node.getNodeId()).append(" : ").append(arrivedTime);
									log.append("Escape Searched From ").append(fromNode.getNodeId()).append(" To ").append(toNode.getNodeId());
									log.append(", Cost:").append(arrivedTime).append(", Time:").append(moveTime).append(", Distance:").append(distance);
									log.append(", Time:").append((double)((System.nanoTime() - pathSearchStartedTime)/1000)/1000).append(" ms");
									trace(vehicleId, log.toString());
									break;
								}
							}
							
							if (j == section.getNodeCount()-1) {
								addQueueNode(vehicleId, node, queueNodeList);
								// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
								resetRouteNodeList.add(node);
							}
						} else {
							break;
						}
					}
					if (arrived == true) {
						break;
					}
				}
			}
			
			if (arrived == true) {
				LinkedList<Node> routedNodeList = new LinkedList<Node>();
				
				Node node = toNode;
				while (fromNode.equals(node) == false) {
					routedNodeList.add(0, node);
					node = node.getPrevNode(vehicleId);
				}
				routedNodeList.add(0, node);
				return vehicle.setRoutedNodeList(routedNodeList);
			}
		} catch (Exception e) {
			traceException("searchEscapeForAbnormalHid()", e);
			isException = true;
		} finally {
			// 2012.07.03 by MYM : Search시 ArrivedTime 설정 및 초기화 개선
			if (isException) {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE);
			} else {
				removeRouteSearchInfo(vehicleId, SEARCH_TYPE.NODE, resetRouteNodeList);
			}
		}
		return false;
	}
	
	/**
	 * check YieldAllowance
	 * 
	 * @param vehicleZone String
	 * @param nodeZone String
	 * @return true, if Yield Allowed
	 */
	private boolean isYieldAllowed(String vehicleZone, String nodeZone) {
		if (vehicleZone.equals(nodeZone)) {
			return true;
		} else {
			if (yieldAllowedSet.contains(vehicleZone + "_" + nodeZone)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * update updateOperationalParameters
	 * 
	 */
	public void updateOperationalParameters() {
		super.updateOperationalParameters();
		
		yieldMinLimitTime = ocsInfoManager.getYieldMinLimitTime();
		// 2015.07.08 by KYK : 파라미터화 (CONSECUTE_YIELD_TIME)
		consecutiveYieldLimitTime = ocsInfoManager.getConsecutiveYieldLimitTime();
		
		// 2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
//		isFailureOHTDetourSearchUsed = ocsInfoManager.isFailureOHTDetourSearchUsed();
//		isCollisionNodeCheckUsed = ocsInfoManager.isCollisionNodeCheckUsed();
//		
//		if (isCollisionNodeCheckUsed &&
//				isNearByDrive == false &&
//				abnormalVehiclesCollisionNodeSet != null) {
//			isCollisionNodeCheckNeeded = true;
//		} else {
//			isCollisionNodeCheckNeeded = false;
//		}
	}
	
//	2015.01.12 by MYM : YieldSearch, PathSearch 공통 부분으로 Search 로 이동
//	private boolean containsAbnormalCollision(Node node) {
//		if (isCollisionNodeCheckNeeded && node != null) {
//			if (abnormalVehiclesCollisionNodeSet.contains(node)) {
//				return true;
//			}
//		}
//		return false;
//	}
}
