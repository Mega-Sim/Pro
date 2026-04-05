package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ListIterator;

import com.samsung.ocs.common.constant.OcsConstant.DEADLOCK_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;
import com.samsung.ocs.manager.impl.OCSInfoManager;

/**
 * Hid Class, OCS 3.0 for Unified FAB
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

public class Hid extends UnitDevice {
	private Hid altHid;
	private Hid backupHid;
	private Hashtable<VehicleData, Object> vehicleTable = new Hashtable<VehicleData, Object>();
	private HashSet<Section> sectionList = new HashSet<Section>(); 
	private OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, null, true, true, 500);
	
	private final static String HID_RUN = "R";
	private final  static String HID_FAILOVER = "O";
//	private final  static String HID_TIMEOUT = "T";

	// Abnormal
	private final  static String HID_STOP = "S";
	private final  static String HID_FAULT = "F";
	private final  static String HID_WARNING = "W";

	// 2014.02.04 by KYK
	private final static String HID_CAPA_FULL = " Hid Capacity Full";
	private final static String HID_CAPA_FULL_HIDLIMITOVERPASS_X = " Hid Capacity Full, HidLimitOverPass(x)";
	private final static String FORWARD_VEHICLE_EXISTS = " Other Vehicle Exists on Entering Hid Border";
	private final static String NO_ALT_HID = " No Alt Hid Exist";
	
	private DETOUR_REASON abnormalReason = DETOUR_REASON.NONE;
	
	public String toString() {
//		return getUnitId();
		return "HID" + getUnitId();
	}
	
	public Hid getAltHid() {
		return altHid;
	}
	
	public void setAltHid(Hid altHid) {
		this.altHid = altHid;
	}
	
	public Hid getBackupHid() {
		return backupHid;
	}
	
	public void setBackupHid(Hid backupHid) {
		this.backupHid = backupHid;
	}
	
	public void setUnregisteredHid(String unitId) {
		this.unitId = unitId;
		this.state = HID_RUN;
		this.altHidName = "";
		this.backupHidName = "";
		this.type = "HID";
		this.ipAddress = "";
		this.voltage = 400;
		this.electricCurrent = 80;
		this.temperature = 11;
		this.frequency = 999;
		this.errorcode = 0;
		this.left = 0;
		this.top = 0;
		this.width = 0;
		this.height = 0;
		this.remoteCmd = "";
		this.altHid = null;
		this.backupHid = null;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isHidDown() {
		if (ocsInfoManager.isHidControlUsed()) {
			if (HID_STOP.equals(state) || HID_FAULT.equals(state) || HID_WARNING.equals(state)) {
				// S or F or W
				return true;
			}
		}
		// R (Run) or O (Failover) or T (Timeout)
		// HID_CONTROL_USED가 NO이면, Hid의 State에 상관없이 Hid가 Available한 것으로 판단함.
		return false;
	}
	
	/**
	 * 현재 이 HID가 FAILOVER중인지 체크해서 넘겨준다.
	 *  
	 * @return
	 */
	public boolean isAltHidCoveringForMainHid() {
		if (ocsInfoManager.isHidControlUsed()) {
			if (altHid != null){
				if (altHid.getAltHid() != null) {
					// Failover HID (M1 Case)
					if (altHid.getAltHid().equals(this)) {
						if (HID_FAILOVER.equals(altHid.getState())) {
							return true;
						}
					}
				} else {
					// Multi-Backup HID (S1 Case)
					if (altHid.getBackupHid() != null) {
						if (altHid.getBackupHid().equals(this)) {
							if (HID_FAILOVER.equals(altHid.getState())) {
								return true;
							}
						}
					}
				}
			}
			return false;
		} else {
			// HID_CONTROL_USED가 NO이면, Hid의 State에 상관없이 Hid가 Available한 것으로 판단함.
			return true;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isAbnormalState() {
		if (isHidDown()) {
			if (isAltHidCoveringForMainHid() == false) {
				return true;
			}
		}
		return false;
	}
	
	public int getVehicleCount() {
		return vehicleTable.size();
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param node
	 * @param prevNode
	 * @param isVehicleInitialize
	 * @param forwardVehicleCount
	 * @return
	 */
//	public String checkMoveIn(VehicleData vehicle, Node node, Node prevNode, boolean isVehicleInitialize, int forwardVehicleCount) {
//		int vehicleCountPerHid = ocsInfoManager.getVehicleCountPerHid();
//		
//		@SuppressWarnings("unchecked")
//		Hashtable<Node, VehicleData> nodeTable = (Hashtable<Node, VehicleData>) vehicleTable.get(vehicle);
//		if (nodeTable != null) {
//			// 동일한 HID상에서 Drive 대상 Vehicle이 이동하려고 할때 이 루틴이 실행됨.
//			nodeTable.put(node, vehicle);
//			return "";
//		} else if (isVehicleInitialize) {
//			// 초기 구동 후 Vehicle이 처음으로 인지된 CurrentNode에 대해 아래의 코드가 실행됨.
//			nodeTable = new Hashtable<Node, VehicleData>();
//			nodeTable.put(node, vehicle);
//			vehicleTable.put(vehicle, nodeTable);
//			return "";
//		} else if (vehicle.getDeadlockType() == DEADLOCK_TYPE.HID) {
//			nodeTable = new Hashtable<Node, VehicleData>();
//			nodeTable.put(node, vehicle);
//			vehicleTable.put(vehicle, nodeTable);
//			return "";
//		} else {
//			if (isHidDown()) {
//				// S (Stop) or F (Fail) or W (Warning)
//				
//				if (isAltHidCoveringForMainHid()) {
//					// altHid != null
//					if (vehicleTable.size() + altHid.getVehicleCount() < vehicleCountPerHid && forwardVehicleCount == 0) {
//						nodeTable = new Hashtable<Node, VehicleData>();
//						nodeTable.put(node, vehicle);
//						vehicleTable.put(vehicle, nodeTable);
//						return "";
//					} else {
//						if (prevNode != null && altHid.equals(prevNode.getHid())) {
//							nodeTable = new Hashtable<Node, VehicleData>();
//							nodeTable.put(node, vehicle);
//							vehicleTable.put(vehicle, nodeTable);
//							return "";
//						} else if (ocsInfoManager.isHidLimitOverPass() && forwardVehicleCount == 0) {
//							if (vehicle.checkHidLimitOverPass(node)) {
//								nodeTable = new Hashtable<Node, VehicleData>();
//								nodeTable.put(node, vehicle);
//								vehicleTable.put(vehicle, nodeTable);
//								return "";
//							} else {
//								registerDriveFailCausedVehicleList(vehicle);
//								StringBuilder message = new StringBuilder();
//								message.append("[").append(unitId).append("] ");
//								message.append("MainHid Status(").append(state).append(",").append(vehicleTable.size()).append(",").append(forwardVehicleCount).append(")");
//								message.append(", AltHid Status(").append(altHid.getState()).append(",").append(altHid.getVehicleCount()).append(")");
//								message.append(", HID Capacity Full, HIDLimitOverPass(X)");
//								return message.toString();
//							}
//						} else {
//							registerDriveFailCausedVehicleList(vehicle);
//							StringBuilder message = new StringBuilder();
//							message.append("[").append(unitId).append("] ");
//							message.append("MainHid Status(").append(state).append(",").append(vehicleTable.size()).append(",").append(forwardVehicleCount).append(")");
//							message.append(", AltHid Status(").append(altHid.getState()).append(",").append(altHid.getVehicleCount()).append(")");
//							message.append(", HID Capacity Full");
//							return message.toString();
//						}
//					}
//				} else {
//					// Main Hid Down, altHid == null or R (Run) or S (Stop) or F (Fail) or W (Warning)
//					StringBuilder message = new StringBuilder();
//					message.append("[").append(unitId).append("] ");
//					message.append("MainHid Status(").append(state).append(")");
//					if (altHid != null) {
//						message.append(", AltHid Status(").append(altHid.getState()).append(")");
//					} else {
//						message.append(", No AltHid");
//					}
//					return message.toString();
//				}
//			} else {
//				if (HID_FAILOVER.equals(state)) {
//					// O (Failover)
//					int vehicleCountsInCoveringHid = 0;
//					if (altHid != null) {
//						vehicleCountsInCoveringHid = altHid.getVehicleCount();
//					} else if (backupHid != null) {
//						vehicleCountsInCoveringHid = backupHid.getVehicleCount();
//					}
//					
//					if (vehicleTable.size() + vehicleCountsInCoveringHid < vehicleCountPerHid && forwardVehicleCount == 0) {
//						nodeTable = new Hashtable<Node, VehicleData>();
//						nodeTable.put(node, vehicle);
//						vehicleTable.put(vehicle, nodeTable);
//						return "";
//					} else if (ocsInfoManager.isHidLimitOverPass() && forwardVehicleCount == 0) {
//						if (vehicle.checkHidLimitOverPass(node)) {
//							nodeTable = new Hashtable<Node, VehicleData>();
//							nodeTable.put(node, vehicle);
//							vehicleTable.put(vehicle, nodeTable);
//							return "";
//						} else {
//							registerDriveFailCausedVehicleList(vehicle);
//							StringBuilder message = new StringBuilder();
//							message.append("[").append(unitId).append("] ");
//							message.append("MainHid Status(").append(state).append(",").append(vehicleTable.size()).append(",").append(forwardVehicleCount).append(")");
//							if (altHid != null) {
//								message.append(", CoveringHid Status(").append(altHid.getState()).append(",").append(vehicleCountsInCoveringHid).append(")");
//							} else if (backupHid != null) {
//								message.append(", CoveringHid Status(").append(backupHid.getState()).append(",").append(vehicleCountsInCoveringHid).append(")");
//							}
//							message.append(", HID Capacity Full, HIDLimitOverPass(X)");
//							return message.toString();
//						}
//					} else {
//						registerDriveFailCausedVehicleList(vehicle);
//						StringBuilder message = new StringBuilder();
//						message.append("[").append(unitId).append("] ");
//						message.append("MainHid Status(").append(state).append(",").append(vehicleTable.size()).append(",").append(forwardVehicleCount).append(")");
//						if (altHid != null) {
//							message.append(", CoveringHid Status(").append(altHid.getState()).append(",").append(vehicleCountsInCoveringHid).append(")");
//						} else if (backupHid != null) {
//							message.append(", CoveringHid Status(").append(backupHid.getState()).append(",").append(vehicleCountsInCoveringHid).append(")");
//						}
//						message.append(", HID Capacity Full");
//						return message.toString();
//					}
//				} else {
//					// R (Run) or T (Timeout)
//					if (vehicleTable.size() < vehicleCountPerHid && forwardVehicleCount == 0) {
//						nodeTable = new Hashtable<Node, VehicleData>();
//						nodeTable.put(node, vehicle);
//						vehicleTable.put(vehicle, nodeTable);
//						return "";
//					} else if (ocsInfoManager.isHidLimitOverPass() && forwardVehicleCount == 0) {
//						if (vehicle.checkHidLimitOverPass(node)) {
//							nodeTable = new Hashtable<Node, VehicleData>();
//							nodeTable.put(node, vehicle);
//							vehicleTable.put(vehicle, nodeTable);
//							return "";
//						} else {
//							registerDriveFailCausedVehicleList(vehicle);
//							StringBuilder message = new StringBuilder();
//							message.append("[").append(unitId).append("] ");
//							message.append("MainHid Status(").append(state).append(",").append(vehicleTable.size()).append(",").append(forwardVehicleCount).append(")");
//							message.append(", HID Capacity Full, HIDLimitOverPass(X)");
//							return message.toString();
//						}
//					} else {
//						registerDriveFailCausedVehicleList(vehicle);
//						StringBuilder message = new StringBuilder();
//						message.append("[").append(unitId).append("] ");
//						message.append("MainHid Status(").append(state).append(",").append(vehicleTable.size()).append(",").append(forwardVehicleCount).append(")");
//						message.append(", HID Capacity Full");
//						return message.toString();
//					}
//				}
//			}
//		}
//	}

	/**
	 * 2014.02.04 by KYK
	 * @param node
	 * @param prevNode
	 * @return
	 */
	public boolean checkHidEqual(Node node, Node prevNode) {
		if (node != null) {
			if (prevNode == null ||
					node.getHid().equals(prevNode.getHid())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 2014.02.04 by KYK
	 * @param node
	 * @param prevNode
	 * @param nodeTable
	 * @return
	 */
	public boolean isComingBackCurrHid(Node node, Node prevNode, Hashtable<Node, VehicleData> nodeTable) {
		if (nodeTable != null && checkHidEqual(node, prevNode) == false) {
			return true;
		}
		return false;
	}
	
	/**
	 * 2014.02.04 by KYK
	 * @param vehicle
	 * @param node
	 * @param nodeTable
	 */
	public void putVehicleInHid(VehicleData vehicle, Node node, Hashtable<Node, VehicleData> nodeTable){
		if (nodeTable == null) {
			nodeTable = new Hashtable<Node, VehicleData>();
		}
		nodeTable.put(node, vehicle);
		vehicleTable.put(vehicle, nodeTable);
	}

	/**
	 * 2014.02.04 by KYK
	 * @param reason
	 * @param forwardVehicleCount
	 * @param isAltHidState
	 * @return
	 */
	public String makeHidDriveFailLog (String reason, int forwardVehicleCount, boolean isAltHidState) {
		StringBuilder message = new StringBuilder();
		message.append("[").append(unitId).append("] ");
		message.append("MainHid Status(").append(state).append(",").append(vehicleTable.size()).append(",").append(forwardVehicleCount).append(")");
		if (isAltHidState) {
			if (altHid != null) {
				message.append(", AltHid Status(").append(altHid.getState()).append(",").append(altHid.getVehicleCount()).append(")");
			} else if (backupHid != null) {
				message.append(", BackupHid Status(").append(backupHid.getState()).append(",").append(backupHid.getVehicleCount()).append(")");				
			}
		}
		message.append(reason);
		return message.toString();
	}
	
	public String checkMoveIn(VehicleData vehicle, Node node, Node prevNode, boolean isVehicleInitialize, int forwardVehicleCount) {
		int vehicleCountPerHid = ocsInfoManager.getVehicleCountPerHid();
		
		@SuppressWarnings("unchecked")
		Hashtable<Node, VehicleData> nodeTable = (Hashtable<Node, VehicleData>) vehicleTable.get(vehicle);
		// 2014.02.04 by KYK : hid ((currnode)1 -> 2 -> 1 (stopnode))
		boolean isComingBackCurrHid = isComingBackCurrHid(node, prevNode, nodeTable);

		if (nodeTable != null && isComingBackCurrHid == false) { // OK : 이미 등록됨 (나갔다돌아옴 제외)
			putVehicleInHid(vehicle, node, nodeTable);
			return "";
		} else if (isVehicleInitialize) { // OK : VHL 초기화 시
			putVehicleInHid(vehicle, node, nodeTable);
			return "";
//		} else if (vehicle.getDeadlockType() == DEADLOCK_TYPE.HID) { // OK : DeadLock Break 시
//			putVehicleInHid(vehicle, node, nodeTable);
//			return "";
		} else { // Check : 신규HID진입 or 나갔다돌아옴			
			boolean isAltHidLog = false;
			int vehicleCountOnCoveringHid = 0;
			if (isHidDown()) {	// S (Stop) or F (Fail) or W (Warning)
				if (isAltHidCoveringForMainHid()) {
					if (prevNode != null && altHid.equals(prevNode.getHid())) {
						putVehicleInHid(vehicle, node, nodeTable);
						return "";
					}
					isAltHidLog = true;
					vehicleCountOnCoveringHid = altHid.getVehicleCount();
				} else {
					// Main Hid Down, altHid == null or R (Run) or S (Stop) or F (Fail) or W (Warning)
					if (altHid != null) {
						return makeHidDriveFailLog("", forwardVehicleCount, true);						
					} else {
						return makeHidDriveFailLog(NO_ALT_HID, forwardVehicleCount, false);						
					}
				}
			} else { // Normal
				if (HID_FAILOVER.equals(state)) {
					isAltHidLog = true;
					if (altHid != null) {
						if (prevNode != null && altHid.equals(prevNode.getHid())) {
							putVehicleInHid(vehicle, node, nodeTable);
							return "";
						}
						vehicleCountOnCoveringHid = altHid.getVehicleCount();
					} else if (backupHid != null) {
						if (prevNode != null && backupHid.equals(prevNode.getHid())) {
							putVehicleInHid(vehicle, node, nodeTable);
							return "";
						}
						vehicleCountOnCoveringHid = backupHid.getVehicleCount();
					}
				}				
			}
			// FAIL : HID변경후 전방정지호기
			if (forwardVehicleCount > 0) { 
				registerDriveFailCausedVehicleList(vehicle);
				return makeHidDriveFailLog(FORWARD_VEHICLE_EXISTS, forwardVehicleCount, false);
			}
			// Check : VHLCount & HidLimitOverPass
			if (vehicleTable.size() + vehicleCountOnCoveringHid < vehicleCountPerHid) {
				putVehicleInHid(vehicle, node, nodeTable);
				return "";
			} else if (vehicle.getDeadlockType() == DEADLOCK_TYPE.HID 
//				&& vehicleTable.size() + vehicleCountOnCoveringHid < vehicleCountPerHid + 1) { // OK : DeadLock Break 시
				&& vehicleTable.size() + vehicleCountOnCoveringHid < vehicleCountPerHid) { // OK : DeadLock Break 시
				putVehicleInHid(vehicle, node, nodeTable);
				return "";
			} else if (ocsInfoManager.isHidLimitOverPass()) {
				if (isComingBackCurrHid == false && vehicle.checkHidLimitOverPass(node)) {
					putVehicleInHid(vehicle, node, nodeTable);
					return "";
				} else {
					// 2015.02.11 by MYM : 장애 지역 우회 기능
					setAbnormalSection(DETOUR_REASON.HID_CAPACITY_FULL);
					registerDriveFailCausedVehicleList(vehicle);
					return makeHidDriveFailLog(HID_CAPA_FULL_HIDLIMITOVERPASS_X, forwardVehicleCount, isAltHidLog);
				}
			} else {
				// 2015.02.11 by MYM : 장애 지역 우회 기능
				setAbnormalSection(DETOUR_REASON.HID_CAPACITY_FULL);
				registerDriveFailCausedVehicleList(vehicle);
				return makeHidDriveFailLog(HID_CAPA_FULL, forwardVehicleCount, isAltHidLog);
			}
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param node
	 * @return
	 */
	public boolean checkMoveOut(VehicleData vehicle, Node node) {
		@SuppressWarnings("unchecked")
		Hashtable<Node, VehicleData> NodeTable = (Hashtable<Node, VehicleData>) vehicleTable.get(vehicle);
		if (NodeTable != null) {
			NodeTable.remove(node);
			if (NodeTable.size() == 0) {
				vehicleTable.remove(vehicle);
				
				// 2015.02.15 by MYM : 장애 지역 우회 기능
				if (abnormalReason == DETOUR_REASON.HID_CAPACITY_FULL
						&& isHidCapacityReleased()) {
					releaseAbnormalSection(false);
				}
			}
		}
		return true;
	}
	
	/**
	 * 2015.08.08 by MYM : 장애 회피 오류 자동 체크
	 * @return
	 */
	public boolean isHidCapacityReleased() {
		if (vehicleTable.size() < ocsInfoManager.getVehicleCountPerHid() - 2) {
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 */
	private void registerDriveFailCausedVehicleList(VehicleData vehicle) {
		if (vehicle != null &&
				vehicle.getDriveFailCausedVehicleList() != null) {
			VehicleData driveFailCausedVehicle;
			@SuppressWarnings("unchecked")
			ArrayList<VehicleData> removeList = (ArrayList<VehicleData>)vehicle.getDriveFailCausedVehicleList().clone(); 
			for (Enumeration<VehicleData> e = vehicleTable.keys(); e.hasMoreElements();) {
				driveFailCausedVehicle = e.nextElement();
				if (driveFailCausedVehicle != null) {
					driveFailCausedVehicle.requestYield(vehicle);
					vehicle.addVehicleToDriveFailCausedVehicleList(driveFailCausedVehicle);
					if (removeList.contains(driveFailCausedVehicle)) {
						removeList.remove(driveFailCausedVehicle);
					}
				}
			}
			ListIterator<VehicleData> it = removeList.listIterator();
			while (it.hasNext()) {
				vehicle.removeVehicleFromDriveFailCausedVehicleList(it.next());
			}
		}
	}
	
	
	/**
	 * 2014.10.17 by MYM : 장애 지역 우회 기능
	 * @param section
	 */
	public void addSection(Section section) {
		this.sectionList.add(section);
	}
	
	/**
	 * 2014.10.17 by MYM : 장애 지역 우회 기능
	 * @param section
	 */
	public void removeSection(Section section) {
		this.sectionList.remove(section);
	}
	
	/**
	 * 2014.10.11 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public void releaseAbnormalSection(boolean resetOnly) {
		if (this.abnormalReason == DETOUR_REASON.NONE) {
			return;
		}
		
		// 경로탐색 불가 → 가능 , OutOfService → InService 처리
		if (resetOnly == false) {
			// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
//			for (Section section : sectionList) {
//				section.removeAbnormalItem(this);
//			}
			HashSet<Section> detourSectionSet = new HashSet<Section>();
			for (Section section : sectionList) {
				detourSectionSet.addAll(section.getDetourSectionSet());
			}
			for (Section section : detourSectionSet) {
				section.removeAbnormalItem(this);
			}
		}
		this.abnormalReason = DETOUR_REASON.NONE;
	}

	/**
	 * 2014.10.11 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public void setAbnormalSection(DETOUR_REASON reason) {
		if (this.abnormalReason == reason) {
			return;
		}
		// HID Down 상태에서 → Capacity Full로 변경 불가
		if (this.abnormalReason == DETOUR_REASON.HID_DOWN
				&& reason == DETOUR_REASON.HID_CAPACITY_FULL) {
			return;
		}
		this.abnormalReason = reason;
		
		// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
//		for (Section section : sectionList) {
//			section.addAbnormalItem(this, true, reason);
//		}
		HashSet<Section> detourSectionSet = new HashSet<Section>();
		for (Section section : sectionList) {
			detourSectionSet.addAll(section.getDetourSectionSet());
		}
		for (Section section : detourSectionSet) {
			section.addAbnormalItem(this, reason);
		}
	}
	
	/**
	 * 2014.10.11 by MYM : 장애 지역 우회 기능
	 */
	public void checkRepathSearch() {
		for (Section section : sectionList) {
			section.checkRepathSearch();
		}
	}
}
