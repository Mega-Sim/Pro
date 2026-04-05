package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.DEADLOCK_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.NEARBY_TYPE;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;
import com.samsung.ocs.common.message.Message;
import com.samsung.ocs.manager.impl.OCSInfoManager;

/**
 * VehicleData Class, OCS 3.0 for Unified FAB
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

public class VehicleData extends Vehicle {
	private int prevCmd;
	private int currCmd;
	private int nextCmd;
	private int commandId;
	private char command;
//	private String rfData;
	private char reply;	
	private String vehicleLoc;
	private boolean isAvRetryWait;
	private double lastAvTime;
	private boolean iaAvExist;
	private boolean isVehicleError;
	private boolean isNearByDrive = false;
	private boolean isAssignedVehicle = false;

//	private int pauseType = 0;
	private int alarmCode = OcsAlarmConstant.NO_ALARM;
	private char prevState = 'I';
	private char prevVehicleMode = 'A';
	private VehicleData yieldRequestedVehicle = null;
	private Node prevCurrNode = null;
	private VehicleData pauseRequestVehicle = null;
	private String pauseReason = "";

//	private boolean isYieldRequested = false;
	private boolean isPathSearchRequested = false;
	private boolean isFailureOHTDetourSearchUsed = true;
	private boolean isBlockPreemptionUpdateUsed = false;

	private char yieldState = 'N';	// N (None), R (Requested), Y (Yielding)
	private Node driveFailedNode = null;
	private boolean isDetourYieldRequested = false; // 2015.06.08
	
	// 2012.02.06 by PMM
	private boolean isLocateRequested = false;
	
	// 2014.02.18 by MYM : [Stage Locate 기능]
	private boolean isStageRequested = false;
	private long stageWaitTime = 0;
	private long stageArrivedTime = 0;
	
	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	private long premoveWaitingTime =0;
	
	// 2012.03.05 by PMM
	private boolean isEStopRequested = false;

	// 2013.07.31 by KYK
	private Vector<Block> occupiedBlockList = new Vector<Block>();
	// 2014.02.20 by KYK
	private HashSet<Block> enteringBlockList = new HashSet<Block>();
	private HashSet<Block> tempBlockList = new HashSet<Block>();	
	private ConcurrentHashMap<Block, String> occupiedBlockStationMap = new ConcurrentHashMap<Block, String>();
	
	private List<Node> routedNodeList = Collections.synchronizedList(new ArrayList<Node>());
	private List<Node> driveNodeList = Collections.synchronizedList(new ArrayList<Node>());
	private Vector<Hid> routedHidList = new Vector<Hid>();
	private Message locusData = new Message("LocusData");
	
	// 2011.12.27 by PMM
	private ArrayList<VehicleData> driveFailCausedVehicleList = new ArrayList<VehicleData>();
	private ArrayList<VehicleData> yieldRequestedVehicleList = new ArrayList<VehicleData>();
	
	// 2014.08.18 by zzang9un
	private ArrayList<VehicleData> detectedCausedVehicleList = new ArrayList<VehicleData>();
	
	private Node detourNode = null;
	private DEADLOCK_TYPE deadlockType = DEADLOCK_TYPE.NONE;
	private long lastDeadlockBrokenTime = 0;
	
	// 2012.01.05 by PMM
	private ArrayList<Node> vehicleLocusList = new ArrayList<Node>();

	private HashSet<Node> redirectedNodeSet = new HashSet<Node>();
	private HashSet<Node> repathSearchCheckNodeSet = new HashSet<Node>();
	private boolean isRepathSearchNeeded = false;
	private long lastPathSearchedTime = 0;
	
	// 2014.10.13 by MYM : 장애 지역 우회 기능
	private HashSet<Section> abnormalSectionSet = new HashSet<Section>();
	private DETOUR_REASON abnormalReason = DETOUR_REASON.NONE;
	private int detourErrorCode = 0;  // 2015.11.10 by MYM : 장애 회피 기능 동작시 ErroCode 저장 
	private String searchFailReason = "";
	
	// 2012.02.15 by PMM
	private static final String OPERATION_EXCEPTION_TRACE = "OperationException";
	private static Logger operationExceptionTraceLog = Logger.getLogger(OPERATION_EXCEPTION_TRACE);
	
	// 2015.06.01 by MYM : OperationDebug Logger 추가(Hid Limit Over Pass 로그 기록시 사용)
	private static final String OPERATION_TRACE = "OperationDebug";
	private static Logger operationTraceLog = Logger.getLogger(OPERATION_TRACE);
	
	private boolean isRepathSearchNeededByPatrolVHL = true;
	
	// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
	private boolean isExistPortofPark = false;

	// 2022.08.17 by Y.Won : HID capa full 인 경우 idle vehicle escape 동작 시 path search fail 표시용
	private boolean isHIDEscapePathSearchFailed = false;
	
	/**
	 * Constructor of VehicleData class.
	 */
	public VehicleData() {
		super();
		prevCmd = 0;
		currCmd= 0;
		nextCmd= 0;
		commandId = 0;
		command = ' ';
		rfData = "";
		reply = ' ';
		stopNode = "";
		targetNode = "";
		registerVehicleLoc();
	}

	public String getPauseReason() {
		return pauseReason;
	}

	public void setPauseReason(String pauseReason) {
		this.pauseReason = pauseReason;
	}
	
	public void setNearByDrive(boolean isNearByDrive) {
		this.isNearByDrive = isNearByDrive;
	}
	
	private void registerVehicleLoc() {
		if (super.getVehicleId() != null || (super.getVehicleId() != null && super.getVehicleId().length() == 0)) {
			vehicleLoc = super.getVehicleId() + "_1";
		} else {
			vehicleLoc = "";
		}
	}
	
	public int getPrevCmd() {
		return prevCmd;
	}
	
	public void setPrevCmd(int prevCmd) {
		this.prevCmd = prevCmd;
	}
	
	public int getCurrCmd() {
		return currCmd;
	}
	
	public void setCurrCmd(int currCmd) {
		this.currCmd = currCmd;
	}
	
	public int getNextCmd() {
		return nextCmd;
	}
	
	public void setNextCmd(int nextCmd) {
		this.nextCmd = nextCmd;
	}
	
	public int getCommandId() {
		return commandId;
	}
	
	public void setCommandId(int commandId) {
		this.commandId = commandId;
	}
	
	public char getCommand() {
		return command;
	}
	
	public void setCommand(char command) {
		this.command = command;
	}
	
	// 2013.05.29 by MYM : Vehicle로 이동(DB에 저장하기 위해서 이동)
//	public String getRfData() {
//		return rfData;
//	}
//	
//	public void setRfData(String rfData) {
//		if (rfData == null) {
//			this.rfData = "";
//		} else {
//			this.rfData = rfData;
//		}
//	}
	
	public char getReply() {
		return reply;
	}
	
	public void setReply(char reply) {
		this.reply = reply;
	}
	
	public String getVehicleLoc() {
		if (vehicleLoc != null && vehicleLoc.length() == 0) {
			registerVehicleLoc();
		} 
		return this.vehicleLoc;
	}	
	
	public boolean isAvRetryWait() {
		return isAvRetryWait;
	}

	public void setAvRetryWait(boolean isAvRetryWait) {
		this.isAvRetryWait = isAvRetryWait;
		if (isAvRetryWait) {
			this.lastAvTime = System.currentTimeMillis();
		}
	}
	
	public double getLastAvTime() {
		return lastAvTime;
	}
	
	public boolean isCarrierExist() {
		return (carrierExist == '1') ? true : false;
	}

	public boolean isAvExist() {
		return iaAvExist;
	}

	public void setAvExist(boolean iaAvExist) {
		this.iaAvExist = iaAvExist;
	}
	
	public boolean isVehicleError() {
		return isVehicleError;
	}

	public void setVehicleError(boolean isVehicleError) {
		this.isVehicleError = isVehicleError;
	}

	/**
	 * 
	 * @param currNode
	 * @param vehicleMode
	 * @param state
	 * @param vehicleSpeed
	 * @param errorCode
	 * @param rfData
	 * @param mapVersion
	 * @param carrierExist
	 * @return
	 */
	public boolean equalVehicleData(String currNode, char vehicleMode,
			char state, double vehicleSpeed, int errorCode, String rfData,
			String mapVersion, char carrierExist) {
		if (this.currNode.equals(currNode) == false || this.state != state ||
				this.vehicleSpeed != vehicleSpeed || this.rfData.equals(rfData) == false ||
				this.vehicleMode != vehicleMode || this.errorCode != errorCode ||
				this.mapVersion.equals(mapVersion) == false || this.carrierExist != carrierExist) {
			return false;
		}
		return true;
	}
	
	/**
	 * 2013.02.15 by KYK
	 * @param currNode
	 * @param vehicleMode
	 * @param state
	 * @param vehicleSpeed
	 * @param errorCode
	 * @param rfData
	 * @param mapVersion
	 * @param carrierExist
	 * @param currStation
	 * @return
	 */
	public boolean equalVehicleData(String currNode, char vehicleMode,
			char state, double vehicleSpeed, int errorCode, String rfData,
			String mapVersion, char carrierExist, String currStation) {
		if (this.currNode.equals(currNode) == false || this.state != state ||
				this.vehicleSpeed != vehicleSpeed || this.rfData.equals(rfData) == false ||
				this.vehicleMode != vehicleMode || this.errorCode != errorCode ||
				this.mapVersion.equals(mapVersion) == false || this.carrierExist != carrierExist || 
//				this.currStation != currStation) {					
				this.currStation.equals(currStation) == false) { // 2015.01.05 by KYK : 정적분석 반영
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param vehicleMode
	 * @param state
	 * @return
	 */
	public boolean equalVehicleData(char vehicleMode, char state) {
		if (this.vehicleMode != vehicleMode || this.state != state) {
			return false;
		}
		return true;
	}
	
	public boolean isPathSearchRequested() {
		return isPathSearchRequested;
	}

	public void setPathSearchRequest(boolean isPathSearchRequested) {
		this.isPathSearchRequested = isPathSearchRequested;
	}
	
	public VehicleData getYieldRequestedVehicle() {
		return yieldRequestedVehicle;
	}

	// 2012.02.06 by PMM
//	public void setYieldRequestedVehicle(VehicleData yieldRequestedVehicle) {
//		this.yieldRequestedVehicle = yieldRequestedVehicle;
//	}

	public boolean isYieldRequested() {
		return this.yieldState == 'R';
//		return isYieldRequested;
	}
	
	public char getYieldState() {
		return yieldState;
	}
	
	public void setYieldState(char yieldState) {
		this.yieldState = yieldState;
	}
	
	public Node getDriveFailedNode() {
		return driveFailedNode;
	}
	
	public void setDriveFailedNode(Node driveFailedNode) {
		this.driveFailedNode = driveFailedNode;
	}

	// 2012.02.06 by PMM
//	public void setYieldRequest(boolean yieldRequest) {
//		this.yieldRequest = yieldRequest;
//	}
	
	// 2012.02.06 by PMM
	public boolean requestYield(VehicleData yieldRequestedVehicle) {
		if (yieldRequestedVehicle != null) {
			// 2015.06.07 by MYM : Locate Vehicle도 양보 요청 설정은 가능하도록 함.(실제 YieldSearch시에는 Locate 호기인지 체크하고 있음)
			// 배경 : Locate Vehicle도 분기에서 DriveFail시 후방 호기의 양보가 있는 경우 양보 해야함. 
//			if (isLocateRequested == false) {
				boolean isYieldNeeded = true;
				if (currNode.equals(targetNode) == false && state == 'G') {
					Node driveTargetNode = getDriveTargetNode();
					if (driveTargetNode != null) {
						if (yieldRequestedVehicle.containsDriveNode(driveTargetNode) == false &&
								yieldRequestedVehicle.containsRoutedNode(driveTargetNode) == false) {
							// TargetNode가 양보 요청 호기의 Route에 없는 경우, 연속 양보 하지 않음.
							if (isNearByDrive) {
								if (yieldRequestedVehicle.getPauseType() == 0) {
									// 근접 제어의 경우, 양보 요청 호기가 대차 감지가 되지 않은 경우.
									return false;
								}
							} else {
								isYieldNeeded = false;
								ArrayList<Collision> collisionList = driveTargetNode.getCollisions();
								if (collisionList != null && collisionList.size() > 0) {
									Collision collision;
									Node collisionNode = null;
									Iterator<Collision> iterator = collisionList.iterator();
									while (iterator.hasNext()) {
										collision = (Collision)iterator.next();
										if (collision != null) {
											collisionNode = collision.getCollisionNode(driveTargetNode);
											if (collisionNode != null) {
												if (yieldRequestedVehicle.containsRoutedNode(collisionNode) ||
														yieldRequestedVehicle.containsDriveNode(collisionNode)) {
													// 비근접제어의 경우, TargetNode의 CollisionNode도 양보 요청 호기의 Route에 있는지 확인.
													isYieldNeeded = true;
												}
											}
										}
									}
								}
							}
						}
					}
				}
				if (isYieldNeeded) {
//					this.isYieldRequested = true;
					this.yieldState = 'R';
					this.yieldRequestedVehicle = yieldRequestedVehicle;
					return true;
				}
//			}
		}
		return false;
	}
	
	public void resetYieldRequested() {
//		this.isYieldRequested = false;
		this.yieldState = 'N';
		this.yieldRequestedVehicle = null;
	}

	public String getLocusData() {
		return locusData.toMessage();
	}
	
	public long getPremoveWaitTime(){
		return this.premoveWaitingTime;
	}
	
	public void setPremoveWaitTime(long time){
		this.premoveWaitingTime = time;
	}
	
	// 2011.12.27 by PMM
	public String getLocusDataString() {
		return locusData.toString();
	}
	
	public void resetLocusData() {
		locusData.reset();
	}
	
	// 2013.05.29 by MYM : Vehicle로 이동(DB에 저장하기 위해서 이동)
//	public int getPauseType() {
//		return pauseType;
//	}
//
//	public void setPauseType(int pauseType) {
//		this.pauseType = pauseType;
//	}
	
	public int getAlarmCode() {
		return alarmCode;
	}

	public void setAlarmCode(int alarmCode) {
		this.alarmCode = alarmCode;
	}

	/**
	 * 
	 */
	public void resetRoutedNodeList() {
		synchronized (routedNodeList) {
			for (int i = 0; i < routedNodeList.size(); i++) {
				Node node = routedNodeList.get(i);
				if (node != null) {
					node.removeRoutedVehicle(this);
					
					// 2012.01.07 by MYM : 비정상 Drive시 초기화
					// 배경 : StopNode - VirtualNode - CommonNode(타 호기 점유)일 때
					//      Vehicle이 Drive시도시 VirtualNode는 DriveIn OK, CommonNode에서 타호기 점유로 인해
					//      DriveFail시 VirtualNode까지 DriveOut을 해야하는데 CommonNode에서 비정상적으로 Exception 발생하여
					//      VirtualNode가 DriveOut되지 않은 상태에서 경로가 바뀌었을 때에 대한 기존 Node DriveOut 해제 처리 필요
					
					// 2013.07.22 by PMM : Drived Node를 지나는 Route에 대해 RepathSearch 시, 초기화되는 문제 발생. U1 CMP
					if (node.hasAlreadyDrived(this) && driveNodeList.contains(node) == false) {
						node.vehicleDriveOut(this);
					}
					
					// 2012.07.11 by MYM : 중복된 코드 주석처리 - node.removeRoutedVehicle에서 cancelReservationForVehicleDriveIn 호출하고 있음. 
					// 2011.12.27 by PMM
					// Route가 바뀌는 경우 (Yield 중 JobAssign 받는 경우 등), 
					// 기존 Route에 등록한 Reservation을 해제해줘야 함.
//					node.cancelReservationForVehicleDriveIn(this);
				}
			}
			routedNodeList.clear();
			
			// resetRoutedNodeList이면, DriveFailedNode도 reset되어야 함.
			// yieldSearch에서 참조함.
			setDriveFailedNode(null);
		}
		routedHidList.clear();
	}
	
	/**
	 * 2015.06.06 by KYK : routeNodeList 를 정리할 때, driveFailNode 는 유지해야할 경우 발생함
	 * DriveFail 지속시 해당 노드를 회피하는 양보주행경로 탐색 필요
	 * @param isDriveFailNodeReset
	 */
	public void resetRoutedNodeList(boolean isDriveFailNodeReset) {
		synchronized (routedNodeList) {
			for (int i = 0; i < routedNodeList.size(); i++) {
				Node node = routedNodeList.get(i);
				if (node != null) {
					node.removeRoutedVehicle(this);
					
					if (node.hasAlreadyDrived(this) && driveNodeList.contains(node) == false) {
						node.vehicleDriveOut(this);
					}					
				}
			}
			routedNodeList.clear();
			if (isDriveFailNodeReset) {
				setDriveFailedNode(null);
			}
		}
		routedHidList.clear();
	}
	
	/**
	 * 
	 * @param isNearByDrive
	 */
	public void clearDriveNodeList(boolean isNearByDrive) {
		synchronized (driveNodeList) {
			Node node = null;
			// 2013.12.13 by KYK
			Block block = null;
			ArrayList<Block> blockList = new ArrayList<Block>(); 
			for (int i = 0; i < driveNodeList.size(); i++) {
				node = driveNodeList.get(i);
				if (node != null) {
					node.vehicleDriveOut(this);
					node.removeRoutedVehicle(this);
					
					if (isNearByDrive) {
						updateDrivingQueue(node, null);
						// 2013.12.13 by KYK : Block Reset
						blockList.clear();
						node.getBlocks(blockList);
						for (int j = 0; j < blockList.size(); j++) {
							block = blockList.get(j);
							if (block.containsDrivingVehicle(this)) {
								block.resetDrivingVehicle(this);
							}
						}
					}
				}
			}
			driveNodeList.clear();
		}
	}
	
	/**
	 * 
	 * @param isNearByDrive
	 */
	public boolean resetDriveNodeList(boolean isNearByDrive, Node currNode) {
		if (currNode != null) {
			synchronized (driveNodeList) {
				boolean result = currNode.vehicleInitialize(this, isNearByDrive);
				Node node = null;
				for (int i = driveNodeList.size() - 1; i >= 0 ; i--) {
					node = driveNodeList.get(i);
					if (node != null && node != currNode) {
						node.vehicleDriveOut(this);
						node.removeRoutedVehicle(this);
						if (isNearByDrive) {
							updateDrivingQueue(node, null);
						}
					}
				}
				driveNodeList.clear();
				if (driveNodeList.contains(currNode) == false) {
					driveNodeList.add(0, currNode);
				}
				// 2013.04.12 by KYK : 재시작 or 초기화시 합류전간섭노드(Station)에서 Block점유 재설정
				if (isNearByDrive) {
					setDrivingVehicle7(currNode);
					// 2013.07.31 by KYK
					resetAbnormalOccupiedBlockList();
				}
				return result;
			}
		} else {
			StringBuilder message = new StringBuilder();
			message.append("resetDriveNodeList() - currNode is null. CurrNodeId:");
			message.append(this.currNode);
			traceOperationException(message.toString());
		}
		return false;
	}

	/**
	 * 2014.02.20 by KYK
	 * @param block
	 */
	public void setEnteringBlockList(Block block) {
		synchronized (enteringBlockList) {
			enteringBlockList.add(block);			
		}
	}
	
	public void resetEnteringBlockList(Block block) {
		synchronized (enteringBlockList) {
			enteringBlockList.remove(block);			
		}
	}
	
	/**
	 * 2013.07.31 by KYK
	 */
	public void setOccupiedBlockList(Block block, String stationId) {
		if (occupiedBlockList.contains(block) == false) {
			occupiedBlockList.add(block);
		}
		if (stationId != null) {
			if (occupiedBlockStationMap.containsKey(block) == false) {
				occupiedBlockStationMap.put(block, stationId);
			}
		}
	}
	
	public void resetOccupiedBlockList(Block block) {
		synchronized (occupiedBlockList) {
			occupiedBlockList.remove(block);
			occupiedBlockStationMap.remove(block);			
		}
	}
	
	/**
	 * 2013.07.31 by KYK
	 */
	public void resetAbnormalOccupiedBlockList() {
		Node node;
		String stationId;
		boolean isOccupied;
		Vector<Block> blockList = new Vector<Block>(occupiedBlockList);
		for (Block block: blockList) {
			isOccupied = false;
			for (int i = 0; i < block.getBlockNodeCount(); i++) {
				node = block.getBlockNode(i);
				if (containsDriveNode(node)) {
					isOccupied = true;
					break;
				}
			}
			if (isOccupied == false) {
				if (occupiedBlockStationMap.containsKey(block)) {
					stationId = occupiedBlockStationMap.get(block);
					if (stationId != null && stationId.length() > 0) {
						if (stationId.equals(getCurrStation())) {
							isOccupied = true;
						} else {
							if (vehicleMode == 'M' && state == 'E' && 
									stationId.equals(getStopStation())) {
								isOccupied = true;
							}
						}
					}
				}
				if (isOccupied == false) {
					block.resetDrivingVehicle(this);
					block.addBlockToUpdatedVehicleList(this);
				}
			}
		}
	}
	
	
	/**
	 * 2013.04.12 by KYK
	 * 재시작 or 초기화시 합류전간섭노드(Station)에서 Block점유 재설정
	 * @param currNode
	 */
	private void setDrivingVehicle7(Node currNode) {
		if (currNode != null) {
			Block block = currNode.getConvergeBlock();
			if (block != null) {
				if (currNode.equals(block.getMainBlockNode()) == false) {
					String currStation = getCurrStation();
					// 2018.03.14 by LSH : 합류 구간 VHL 비정상 정지 중 Operation 초기화 시, Block 점유 해제 현상 개선
//					if (currStation != null && currStation.length() > 0) {
//						block.setDrivingVehicle(this);
//						// 2013.07.31 by KYK
//						setOccupiedBlockList(block, currStation);
//					}
					block.setDrivingVehicle(this);
					setOccupiedBlockList(block, currStation);
				}
			}
		}
//		if (currNode != null) {
//			Block block = currNode.getBlock(null);
//			if (block != null) {
//				if (currNode.equals(block.getMainBlockNode()) == false) {
//					if (OcsConstant.CONVERGE.equals(block.getBlockType())) {
//						String currStation = getCurrStation();
//						if (currStation != null && currStation.length() > 0) {
//							block.setDrivingVehicle(this);
//						}
//					}
//				}
//			}			
//		}
	}

	// 2012.03.05 by PMM
//	public void resetDriveNodeList(boolean isNearByDrive) {
//	synchronized (driveNodeList) {
//		Node tempCurrNode = null;
//		Node node = null;
//		for (int i = 0; i < driveNodeList.size(); i++) {
//			node = driveNodeList.get(i);
//			if (node != null) {
//				if (node.getNodeId().equals(this.getCurrNode())) {
//					tempCurrNode = node;
//				} else {
//					node.vehicleDriveOut(this);
//					node.removeRoutedVehicle(this);
//					if (isNearByDrive) {
//						updateDrivingQueue(node, null);
//					}
//				}
//			}
//		}
//		driveNodeList.clear();
//		if (tempCurrNode != null) {
//			driveNodeList.add(tempCurrNode);
//		}
//	}
//}
	
	// 2012.03.05 by PMM
//	public void reset(boolean isNearByDrive) {
//		resetRoutedNodeList();
//		resetDriveNodeList(isNearByDrive);
//	}
	public boolean reset(boolean isNearByDrive, Node currNode) {
		resetRoutedNodeList();
		return resetDriveNodeList(isNearByDrive, currNode);
	}

	public void clear(boolean isNearByDrive) {
		resetRoutedNodeList();
		clearDriveNodeList(isNearByDrive);
	}

	/**
	 * 
	 * @param currNode
	 * @param driveMethod
	 * @return
	 */
//	public boolean initializePathDrive(Node currNode, boolean isNearByDrive) {
	public boolean initializePathDrive(Node currNode) {
		boolean retVal = true;
		List<Node> prevDriveNodeList = new ArrayList<Node>();
		for (Node node : driveNodeList) {
			if (node != null) {
				if (prevDriveNodeList.contains(node) == false) {
					prevDriveNodeList.add(node);
				}
			}
		}
		
		synchronized (driveNodeList) {
			driveNodeList.clear();
			driveNodeList.add(0, currNode);
			// 2018.03.14 by LSH : 합류 구간 VHL 비정상 정지 중 Operation 초기화 시, Block 점유 해제 현상 개선
			if (isNearByDrive) {
				setDrivingVehicle7(currNode);
			}
			prevDriveNodeList.remove(currNode);
		}
		
		Node node;
		Node prevNode = currNode;
		String reason;
		while (routedNodeList.size() > 0) {
			node = routedNodeList.remove(0);
			if (node != null && prevNode != null) {
				if (prevDriveNodeList.contains(node) == false) {
					if (node.vehicleInitialize(this, isNearByDrive)) {
						reason = OcsConstant.OK;
					} else {
						reason = "Vehicle Initialize Failed";
					}
					// 2013.04.12 by KYK : 재시작 or 초기화시 합류전간섭노드(Station) Block 점유
					if (isNearByDrive) {
						setDrivingVehicle7(node);
					}
					if (OcsConstant.OK.equals(reason) == false) {
						// isNearByDrive == false인데, reason != OK이면??
						retVal = false;
						this.reason = reason;
						
						StringBuilder message = new StringBuilder();
						message.append("initializePathDrive() - DriveFail:");
						message.append(vehicleId);
						message.append(", from:").append(prevNode.getNodeId());
						message.append(" -> to:").append(node.getNodeId());
						message.append(", Reason:").append(reason);
						traceOperationException(message.toString());
					}
				}
				driveNodeList.add(node);
				prevDriveNodeList.remove(node);
				prevNode = node;
			} else {
				StringBuilder message = new StringBuilder();
				message.append("initializePathDrive() - node is null.");
				if (node != null) {
					message.append(" node:").append(node.getNodeId());
				}
				if (prevNode != null) {
					message.append(" prevNode:").append(prevNode.getNodeId());
				}
				traceOperationException(message.toString());
			}
		}
		
		synchronized (driveNodeList) {
			for (Node tempNode : prevDriveNodeList) {
				if (tempNode != null && driveNodeList.contains(tempNode) == false) {
					tempNode.vehicleDriveOut(this);
					tempNode.removeRoutedVehicle(this);
					if (isNearByDrive) {
						updateDrivingQueue(tempNode, null);
					}
				}
			}
		}
		// 2014.07.26 by MYM : 초기 구동시 설정하도록 변경
//		this.isNearByDrive = isNearByDrive;
		return retVal;
	}

	/**
	 * 
	 * @param driveMethod
	 * @param driveLimitTime
	 * @return
	 */
	public String driveVehiclePath(boolean isNearByDrive, double driveLimitTime, int driveMinNodeCount, NEARBY_TYPE nearbyType, OCSInfoManager ocsInfoManager, boolean isNearByNormalDrive) throws Exception {
//	public String driveVehiclePath(boolean isNearByDrive, double driveLimitTime) {
// 2012.01.19 by PMM
		// PathSearch 요청 확인
//		if (isPathSearchRequested || isAbnormalStatusOnRoutedHid()) {
		if (isPathSearchRequested() || isAbnormalStatusOnRoutedHid()) {
			resetRoutedNodeList();
			return OcsConstant.REQUEST_PATH_SEARCH;
		}

		try {
			// 2013.02.08 by KYK
			setStationDriveAllowed(true);
			if (isNearByDrive) {
				// 2013.04.05 by KYK
				if (nearbyType == NEARBY_TYPE.NEARBY_V7) {
					return driveVehiclePathForNearby7(driveLimitTime, ocsInfoManager, isNearByNormalDrive);
				} else {
					return driveVehiclePathForNearby(driveLimitTime, isNearByNormalDrive);
				}
			} else {
				return driveVehiclePath(driveLimitTime, driveMinNodeCount);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "DriveFail : " + e.getStackTrace();
		}
	}
	
	public boolean isAbnormalVehicle() {
		// 2015.04.15 by MYM : Vehicle Detect 무언정지인 경우는 Abnormal로 인식하지 않도록 함.
		// 배경 : Operation Cancel 후 JobAssign 할당 반복현상 때문에 NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER AlarmCode를 
		//       3101 → 5005으로 변경하면서 경로 탐색시 기존에는 Vehicle Detect라도 경로 탐색이 가능하였지만
		//       변경(2015.02.10) 후에는 경로 탐색이 되지 않는 문제가 발생 (17L EDS - 2015.04.14)
		//      ＊ NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER 처리?
		//        1) Unload 작업 할당 해제 (Operation)
		//        2) 해당 Vehicle의 경로는 탐색 가능 (Operation)
		//        3) 해당 Vehicle에게 작업 할당 불가 (JobAssign)
		//       
//		if (vehicleMode == 'M' || state == 'E' ||
//				errorCode == OcsConstant.COMMUNICATION_FAIL || alarmCode > 5000) {
//			return true;
//		}
		if (vehicleMode == 'M' || state == 'E' ||
				errorCode == OcsConstant.COMMUNICATION_FAIL || 
				(alarmCode > 5000 && alarmCode != OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER)) {
			return true;
		}
		return false;
	}
	
	private boolean isAbnormalStatusOnRoutedHid() {
		ListIterator<Hid> iterator = routedHidList.listIterator();
		Hid routedHid = null;
		Hid drivedHid = this.getDriveCurrNode().getHid();
		
		// 2011.10.26 by PMM
		if (drivedHid != null && drivedHid.isAbnormalState()) {
			return false;
		}
		while (iterator.hasNext()) {
			routedHid = iterator.next();
			if (routedHid != null) {
				if (routedHid.isAbnormalState()) {
					if (drivedHid != null && routedHid.equals(drivedHid) == false) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * 2013.04.05 by KYK
	 * @return
	 */
	public String getPreSteeringNode() {
		
		Node stopNode = getDriveStopNode();
		if (stopNode == null) {
			return "";
		}		
		if (getRoutedNodeCount() == 0) {
			return targetNode;
		}		
		Node nextNode = getRoutedNode(0);
		if (nextNode == null) {
			return "";
		}
		// 지금 교차로노드면 다음노드 리턴
		if (stopNode.isConverge || stopNode.isDiverge) {
			return nextNode.getNodeId();
		}
		// 교차로노드 찾아서 그 다음노드 리턴
		for (int i = 0; i < getRoutedNodeCount(); i++) {
			nextNode = getRoutedNode(i);
			if (nextNode.isConverge || nextNode.isDiverge) {
				if (i + 1 < getRoutedNodeCount()) {
					nextNode = getRoutedNode(i + 1);
					if (nextNode == null) {
						return "";
					} else {
						return nextNode.getNodeId();
					}
				}
			}
		}
		return targetNode;
	}

	/**
	 * 
	 * @param driveLimitTime
	 * @param driveMinNodeCount
	 * @return
	 */
	private String driveVehiclePath(double driveLimitTime, int driveMinNodeCount) throws Exception {
//	private String driveVehiclePath(double driveLimitTime) {
// 2012.01.19 by PMM
		assert driveNodeList != null;
		assert routedNodeList != null;
		
		String stopNodeId = "";
		Node node;
		Node prevNode = getDriveCurrNode();
		if (prevNode == null) {
			return stopNodeId;
		}

		if (driveNodeList.size() > 0) {
			Node stopNode = getDriveStopNode();
			if (stopNode != null) {
				stopNodeId = stopNode.getNodeId();
			}
		}
		if (routedNodeList.size() == 0) {
			setPathSearchRequest(true);
			return stopNodeId;
		}

		// 2012.08.02 by MYM : 최소 1구간은 Drive 가능하도록 조건 추가(StopAllowed를 체크하여 0이면 추가 Drive)
		int stopAllowedCount = 0;
		// Vehicle CurrNode ~ StopNode까지의 시간을 계산한다.
		double arrivedTime = 0;
		for (int i = 1; i < driveNodeList.size(); i++) {
			node = driveNodeList.get(i);
			if (node != null) {
				arrivedTime += node.getMoveInTime(prevNode);
				prevNode = node;
				stopNodeId = node.getNodeId();
				
				// 2012.08.02 by MYM : 최소 1구간은 Drive 가능하도록 조건 추가(StopAllowed를 체크하여 0이면 추가 Drive)
				if (node.isStopAllowed()) {
					stopAllowedCount++;
				}
			}
		}
		
		// 2012.03.08 by MYM : [분기 이후 Stop 명령 전송] 분기를 지날 때는 이후 분기 통과 후 첫노드(가상노드 제외)까지만 주행 명령 전송
		boolean isPassedDivergeNode = false;
		if (prevNode.isDiverge()) {
			isPassedDivergeNode = true;
		}
		
		// Vehicle의 추가 Drive 가능한 StopNode를 찾는다.
		int driveCount = 0;
		for (int i = 0; i < routedNodeList.size(); i++) {
			node = routedNodeList.get(i);
			if (node != null) {
				// 2012.07.13 by MYM : 주석처리 - setArrivedTime 사용하지 않도록 수정(node의 NodeArrivedTimeInfo 미생성)
//				node.setArrivedTime(vehicleId, driveLimitTime);
//				arrivedTime += node.getMoveInTime(prevNode);
//				if (node.setArrivedTime(vehicleId, prevNode, arrivedTime)) {
				arrivedTime += node.getMoveInTime(prevNode);
				
				// 2012.08.02 by MYM : 최소 1구간은 Drive 가능하도록 조건 추가(StopAllowed를 체크하여 0이면 추가 Drive)
//				if (node.isEnabled() && arrivedTime < driveLimitTime) {
				if (node.isEnabled() && (arrivedTime < driveLimitTime || stopAllowedCount < driveMinNodeCount)) {
					reason = node.vehicleDriveIn(this, prevNode);
					
					if (OcsConstant.OK.equals(reason)) {
						prevNode = node;
						// 2012.07.13 by MYM : 주석처리 - setArrivedTime,getArrivedTime 사용하지 않도록 수정(불필요)
//						arrivedTime = node.getArrivedTime(vehicleId);
						driveCount++;
						
						// 2012.08.02 by MYM : 최소 1구간은 Drive 가능하도록 조건 추가(StopAllowed를 체크하여 0이면 추가 Drive)
						if (node.isStopAllowed()) {
							stopAllowedCount++;
						}
						
						// 2012.03.08 by MYM : [분기 이후 Stop 명령 전송] 분기를 지날 때는 이후 분기 통과 후 첫노드(가상노드 제외)까지만 주행 명령 전송
						// 배경 : 분기 이후 합류를 통과할 때 Vehicle에게 한번에 합류 통과 주행명령을 전송하게 되면 OCS의 Route와 Vehicle의 Route가 다를 수 있음.
						//       -> OCS에서는 Route를 이탈하면 E-Stop을 전송함.
						//       루프의 커브구간의 노드와 진입하는 직선의 노드가 충돌설정된 경우 직선 노드는 지나갈 수 있는 경우에만 Drive하도록 조건 체크(isInappropriateForStopNode)
						//2012.03.12 by PMM
//						if (isPassedDivergeNode == true && node.isVirtual() == false && node.isInappropriateForStopNode() == false) {								
						if (isPassedDivergeNode == true && node.isStopAllowed()) {								
							break;
						} else if (node.isDiverge()) {
							isPassedDivergeNode = true;
						}
					} else {
						if (driveNodeList.contains(node)) {
							// 2013.06.13
							// 이미 Drived한 구간을 다시 지나가는 경로가 선정될 경우,
							// Duplication Drive로 DriveFail되어 여기로 옴.
							// vehicleDriveOut을 하면, 다음 번에 Drive가 됨.
							// Vehicle이 drive한 구간을 지나가고 나서 다시 해당 노드에 drive해야 함.
							break;
						}
						// drive 시도 실패시 해당 노드에 대해서 driveOut
						node.vehicleDriveOut(this);
						
						// 2011.12.27 by PMM
						if (reason.indexOf("Capacity Full") < 0) {
							node.makeReservationForVehicleDriveIn(this);
						}

						// 2014.09.23 by zzang9un : Deadlock type이 None이 아닌 경우 분기인지를 모두 확인
						//if (deadlockType == DEADLOCK_TYPE.NODE) {
						if (deadlockType != DEADLOCK_TYPE.NONE) {
							if (getDriveCurrNode() != null &&
									getDriveCurrNode().isDiverge() &&
									System.currentTimeMillis() - lastDeadlockBrokenTime > 5000) {
								setPathSearchRequest(true);
								node.cancelReservationForVehicleDriveIn(this);
								if (detourNode == null) {
									detourNode = node;
								}
								lastDeadlockBrokenTime = System.currentTimeMillis();
							}
						}
						
						setDriveFailedNode(node);
						
						break;
					}
				} else {
					if (node.isEnabled() == false) {
						StringBuffer log = new StringBuffer();
						log.append("DriveFail : ").append(prevNode.getNodeId()).append(">").append(node.getNodeId()).append(" Disabled");
						this.reason = log.toString();
					} else {
						StringBuffer log = new StringBuffer();
						log.append("DriveFail : ").append(prevNode.getNodeId()).append(">").append(node.getNodeId()).append(" driveLimitTime Over(").append(driveLimitTime).append("sec)");
						this.reason = log.toString();
					}
					break;
				}
			} else {
				StringBuilder message = new StringBuilder();
				message.append("driveVehiclePath(double driveLimitTime) - node is null. (1)");
				traceOperationException(message.toString());
			}
		}
		
		// StopNode의 Virtual 유무를 확인한다.
		while (driveCount > 0) {
			node = routedNodeList.get(driveCount - 1);
			if (node != null) {
				// 2011.12.27 by PMM
//				if (node.isVirtual() && driveCount != routedNodeList.size()) {
//					node.vehicleDriveOut(this);
//					driveCount--;
//				} else {
//					break;
//				}
				if (node.equals(routedNodeList.get(routedNodeList.size() - 1)) == false) {
					// 2012.03.12 by PMM
//					if (node.isVirtual() || node.isInappropriateForStopNode()) {
					if (node.isStopAllowed() == false) {
						node.vehicleDriveOut(this);
						
						setDriveFailedNode(node);
						
						if (reason.indexOf("Capacity Full") < 0) {
							// Closeloop/HID Capacity Full이 아닌 경우에만 Make Reservation.
							// Capacity Full로 진입을 못하는데 Reserve하면, 해당 지역 내 VHL이 Drive 못해 Capacity Full이 해결안될 수 있음.
							node.makeReservationForVehicleDriveIn(this);
						}
						
						// CurrNode - 가상노드 - 실 노드 에서
						// 실 노드에서 Drive Fail 시, detourNode를 실노드가 아닌 가상노드로 해야 하는 경우가 생김. Shortcut에서 곡선부가 가상노드.
						if (detourNode != null) {
							detourNode = node;
						}
						
						driveCount--;
						continue;
					}
				}
				if (repathSearchCheckNodeSet.contains(node)) {
					setRepathSearchNeeded(true);
				}
				break;
			} else {
				StringBuilder message = new StringBuilder();
				message.append("driveVehiclePath(double driveLimitTime) - node is null. (2)");
				traceOperationException(message.toString());
			}
		}
		
		// DriveNode를 설정한다.
		stopNodeId = setDriveNodeList(driveCount);
		
		if (stopNodeId.equals(stopNode) && (this.reason != null && this.reason.length() > 0 && OcsConstant.OK.equals(reason) == false)) {
			return this.reason;
		} else {
			this.reason = "";
			
			// 2011.12.27 by PMM
			clearDriveFailCausedVehicleList();
			deadlockType = DEADLOCK_TYPE.NONE;
			lastDeadlockBrokenTime = 0;
			
			return stopNodeId;
		}
	}

	/**
	 * @param driveLimitTime - Drive Limit Time(사용하지 않음, 불필요)
	 * @param isNearByNormalDrive - 근접제어 flag
	 * @return
	 * @throws Exception
	 */
	private String driveVehiclePathForNearby(double driveLimitTime, boolean isNearByNormalDrive) throws Exception {
		assert driveNodeList != null;
		assert routedNodeList != null;
		
		String stopNodeId = "";
		Node node;
		Node prevNode = getDriveCurrNode();
		if (prevNode == null) {
			return stopNodeId;
		}

//		Hid vehicleCurrHID = null;
		if (driveNodeList.size() > 0) {
			Node stopNode = getDriveStopNode();
			if (stopNode != null) {
				stopNodeId = stopNode.getNodeId();
//				vehicleCurrHID = stopNode.getHid();
			}
		}
		if (routedNodeList.size() == 0) {
			setPathSearchRequest(true);
			return stopNodeId;
		}

		// Vehicle CurrNode ~ StopNode까지의 시간을 계산한다.
//		Vector<VehicleData> forwardVehicleList = new Vector<VehicleData>();
		HashSet<VehicleData> forwardVehicleList = new HashSet<VehicleData>();
		for (int i = 1; i < driveNodeList.size(); i++) {
			node = driveNodeList.get(i);
			if (node != null) {
				// 2015.05.31 by MYM : vehicle의 HID 영역과 다른 선행 Vehicle도 카운트하도록 수정
				// 배경 : vehicle의 Curr HID와 다른 선행 Vehicle이 카운트되지 않아 HID Capa를 Over해서 진입하는 경우가 발생
				// 새로운 HID로 진입할 때 Capacity 체크를 위한 선행 Vehicle의 수 카운트
//				if (node.getHid() == vehicleCurrHID) {
//					checkForwardVehicle(forwardVehicleList, node, vehicleCurrHID);
//				}
				if (i < driveNodeList.size() - 1) { // StopNode는 아래에서 체크
					checkForwardVehicle(forwardVehicleList, node);
				}
				
				prevNode = node;
				stopNodeId = node.getNodeId();
			}
		}

		// 1. 마지막 StopNode가 분기 Block이면 첫번째 Vehicle인지 확인
		// ※ 마지막 StopNode가 분기 Block인 경우 첫번째 Vehicle만 통과 시킨다.
		Block stopNodeBlock = prevNode.getBlock(null);
		if (stopNodeBlock != null && OcsConstant.DIVERGE.equals(stopNodeBlock.getBlockType())) {
			// 2012.06.01 by MYM : 분기 통과 체크시 Sync 하도록 수정
			synchronized (stopNodeBlock) {
				String checkFirstVehicleResult = stopNodeBlock.checkFirstVehicleInDiverge(this);
				if (OcsConstant.OK.equals(checkFirstVehicleResult) == false) {
					StringBuffer log = new StringBuffer("DriveFail : ");
					log.append(stopNodeId).append(">").append(routedNodeList.get(0).getNodeId()).append(" ").append(checkFirstVehicleResult);
					if (reason.equals(log.toString()) == false) {
						reason = log.toString();
						return reason;
					} else {
						return stopNodeId;
					}
				} else {
					reason = "";
				}
			}
		}
		
		// 2012.03.08 by MYM : [분기 이후 Stop 명령 전송] 분기를 지날 때는 이후 분기 통과 후 첫노드(가상노드 제외)까지만 주행 명령 전송
		boolean isPassedDivergeNode = false;
		if (prevNode.isDiverge()) {
			isPassedDivergeNode = true;
		}

		// Vehicle의 추가 Drive 가능한 StopNode를 찾는다.
		int driveCount = 0;
		try {
			for (int i = 0; i < routedNodeList.size(); i++) {
				node = routedNodeList.get(i);
				if (node != null) {
					// 2-1. DriveNode가 Loop를 형성하는지 확인
					// ※ 2회 동일한 Node로 Drive되는 현상을 방지
					if (driveNodeList.contains(node)) {
						break;
					}
					
					// 2015.06.03 by MYM : 전방 Forward Vehicle 카운트는 Drive 할 노드 전까지만 체크해야 함. (prevNode로 변경)
					// 2015.05.31 by MYM : vehicle의 HID 영역과 다른 선행 Vehicle도 카운트하도록 수정
					// 배경 : vehicle의 Curr HID와 다른 선행 Vehicle이 카운트되지 않아 HID Capa를 Over해서 진입하는 경우가 발생
					// 새로운 HID로 진입할 때 Capacity 체크를 위한 선행 Vehicle의 수 카운트
//					if (node.getHid() == vehicleCurrHID) {
//						checkForwardVehicle(forwardVehicleList, node, vehicleCurrHID);
//					}
					checkForwardVehicle(forwardVehicleList, prevNode);
					
					// 2-2. 후보 StopNode가 Block 노드인지 확인한다.					
					// 2011.12.30 by MYM : 합류 Block에서 blockNode에 정지해 있다가 출발시 Block을 가져오지 못하는 문제 수정
					// 배경 : 분기 ~ 합류 사이의 Node가 두 Block의 blockNode로 포함된 경우 분기는 통과 했는데 다음 합류 블럭 통과시 
					//       분기 Block을 가져와 조건을 판단하여 합류의 blockNode로 주행 명령을 전송하는 문제를 개선한 코드에 버그가 존재
//					BlockInfo block = node.getBlock(stopNodeBlock);
					Block block = null;
					if (stopNodeBlock != null && OcsConstant.CONVERGE.equals(stopNodeBlock.getBlockType())) {
						block = node.getBlock(null);
					} else {
						block = node.getBlock(stopNodeBlock);
					}
					
					if (block == null) {
						// 2-2-1. Block 노드가 아니면 계속 Drive 가능 여부를 확인한다.
						String driveResult = node.vehicleDriveIn(this, prevNode, forwardVehicleList.size(), isNearByNormalDrive);
						if (OcsConstant.OK.equals(driveResult)) {
							reason = "";
							driveCount++;
						} else {
							node.vehicleDriveOut(this);
							reason = driveResult;
							
							setDriveFailedNode(node);
							
							break;
						}
					} else {
						// 2012.06.01 by MYM : Block 통과 체크시 Sync 하도록 수정
						// 배경 : 동일 Block에서 vehicle이 node로 drivein할 때까지는 동기화(synchronized)되어야 함. 
						synchronized (block) {
							// 2-2-2. 합류 Block 노드인 경우 Block을 통과 여부를 확인한다.
							if (OcsConstant.CONVERGE.equals(block.getBlockType())) {
								// 1. 첫번째 통과 Vehicle이 아니면 현재 노드까지만 Drive 하도록 한다.
								String checkFirstVehicleResult = block.checkFirstVehicleInConverge(this);
								if (OcsConstant.OK.equals(checkFirstVehicleResult)) {
									// 다음 노드로 Drive 가능 여부를 확인한다.
									String driveResult = node.vehicleDriveIn(this, prevNode, forwardVehicleList.size(), isNearByNormalDrive);
									if (OcsConstant.OK.equals(driveResult)) {
										driveCount++;
										// 2011.10.27 by PMM & MYM
//									block.setDrivingVehicle(this);
										reason = "";
									} else {
										// 2011.10.27 by PMM & MYM
										block.resetDrivingVehicle(this);
										node.vehicleDriveOut(this);
										setDriveFailedNode(node);
										reason = driveResult + forwardVehicleList; // Forward Vehicle Reason 추가
										break;
									}
									// 2015.06.03 by MYM : 미사용 주석 처리
//								} else if (OcsConstant.ABNORMAL_OK.equals(checkFirstVehicleResult)) {
//									// Block 점유 Vehicle이 존재한 상태에서 First Vehicle인 경우 통과 시킨다.
//									String driveResult = node.vehicleDriveIn(this, prevNode, forwardVehicleList.size(), isNearByNormalDrive);
//									if (OcsConstant.OK.equals(driveResult)) {
//										reason = "";
//										driveCount++;
//									} else {
//										node.vehicleDriveOut(this);
//										setDriveFailedNode(node);
//										reason = driveResult;
//										break;
//									}
								} else {
									reason = "DriveFail : " + checkFirstVehicleResult;
									// 2015.06.04 by MYM : Block 통과 실패시 DriveFailedNode 설정 추가 (DriveFail시 우회 기능 동작) 
									setDriveFailedNode(node);
									break;
								}
								
								// 2012.03.08 by MYM : [분기 이후 Stop 명령 전송] 합류를 지난 경우에는 passedDivergeNode를 초기화 
								isPassedDivergeNode = false;
							} else if (OcsConstant.DIVERGE.equals(block.getBlockType())) {
								// 2-2-3. 분기 Block 노드인 경우 계속 Drive 가능 여부를 확인한다.
								String driveResult = node.vehicleDriveIn(this, prevNode, forwardVehicleList.size(), isNearByNormalDrive);
								if (OcsConstant.OK.equals(driveResult)) {
									driveCount++;
									block.setDrivingVehicle(this);
									reason = "";
									
									// 분기는 분기 노드까지 주행명령을 전송하고 분기 통과 여부는 다음 Drive 시도시 위의 1에서 체크함.
									// 배경 : 합류노드 다음 분기노드가 바로 오는 경우 합류를 통과하고 합류 노드가 분기의 DQ에 포함된 경우 분기도 연속으로 통과하기 때문에 분기 노드까지면 주행명령 전송
//								break;
									// MainBlock노드까지 주행명령 전송하도록 조건 추가
									// 배경 : MainBlock 노드까지 주행명령을 주고 직진 전방 1300mm 이내에 노드가 있고, 가상 노드이면 Drive를 하지 못하는 현상이 발생
//								if (node.equals(block.getMainBlockNode())) {
//										break;
//								}	
									// 2011.11.25 by MYM 
									// 분기 노드가 가상노드인 경우 여기서 분기 통과 여부를 결정함.
									// 배경 : 분기 노드가 가상노드인 경우 분기 전 노드까지 주행명령을 보내고 다음 Drive시 위의 1번에서 Block 노드가 아니기 때문에 여기로 내려와서 분기 노드라 
									//       break 조건으로 빠지고 하여 분기를 통과하지 못하는 현상이 발생.
									if (node.equals(block.getMainBlockNode())) {
										if (node.isVirtual()) {
											String checkFirstVehicleResult = block.checkFirstVehicleInDiverge(this);
											if (OcsConstant.OK.equals(checkFirstVehicleResult) == false) {
												StringBuffer log = new StringBuffer("DriveFail : ");
												log.append(stopNodeId).append(">").append(routedNodeList.get(0).getNodeId()).append(" ").append(checkFirstVehicleResult);
												if (reason.equals(log.toString()) == false) {
													reason = log.toString();
												}
												break;
											}
										} else {
											break;
										}
									}
								} else {
									node.vehicleDriveOut(this);
									setDriveFailedNode(node);
									reason = driveResult + forwardVehicleList; // Forward Vehicle Reason 추가
									break;
								}
							} else {
								reason = "";
								break;
							}
						}
					}
					
					// 2012.03.08 by MYM : [분기 이후 Stop 명령 전송] 분기를 지날 때는 이후 분기 통과 후 첫노드(가상노드 제외)까지만 주행 명령 전송
					// 배경 : 분기 이후 합류를 통과할 때 Vehicle에게 한번에 합류 통과 주행명령을 전송하게 되면 OCS의 Route와 Vehicle의 Route가 다를 수 있음.
					//       -> OCS에서는 Route를 이탈하면 E-Stop을 전송함.
					if (isPassedDivergeNode == true && node.isVirtual() == false) {												
						break;
					} else if (node.isDiverge()) {
						isPassedDivergeNode = true;
					}
					
					prevNode = node;
				}

			}
		} catch (Exception e) {
			while (driveCount > 0) {
				node = routedNodeList.get(driveCount - 1);
				node.vehicleDriveOut(this);
				setDriveFailedNode(node);
				driveCount--;
				Block block = node.getBlock(stopNodeBlock);
				if (block != null) {
					block.resetDrivingVehicle(this);
				}
			}
			return stopNodeId;
		}

		// StopNode의 Virtual 유무를 확인한다.
		while (driveCount > 0) {
			node = routedNodeList.get(driveCount - 1);
			// 2015.06.03 by MYM : Converge Block 통과를 못하는 경우 합류 전 UserBlock Node도 DriveOut 추가
//			if (node.isVirtual() && driveCount != routedNodeList.size()) {
//				node.vehicleDriveOut(this);
//				setDriveFailedNode(node);
//				driveCount--;
//				Block block = node.getBlock(stopNodeBlock);
//				if (block != null) {
//					block.resetDrivingVehicle(this);
//				}
//			} else {
//				break;
//			}
			// 최종 목적지인 경우 Drive 가능
			if (driveCount == routedNodeList.size()) {
				break;
			}
			
			// 가상 노드인 경우, 합류 Block에서 UserBlock인 경우 주행 불가
			Block block = node.getBlock(stopNodeBlock);
			if (node.isVirtual()
					|| (block != null
							&& OcsConstant.CONVERGE.equals(block.getBlockType())
							&& block.getMainBlockNode() != node)) {
				node.vehicleDriveOut(this);
				setDriveFailedNode(node);
				driveCount--;
				if (block != null) {
					block.resetDrivingVehicle(this);
				}
			} else {
				break;
			}
		}
		
		// DriveNode를 설정한다.
		stopNodeId = setDriveNodeList(driveCount);
		if (stopNodeId.equals(stopNode) && (reason != null && reason.length() > 0)) {
			// 2014.09.02 by zzang9un : deadlock break를 위해 추가
//			if (deadlockType == DEADLOCK_TYPE.NODE) {
			if (deadlockType != DEADLOCK_TYPE.NONE) {
				if (getDriveCurrNode() != null &&
						getDriveCurrNode().isDiverge() &&
						System.currentTimeMillis() - lastDeadlockBrokenTime > 5000) {
					
					node = routedNodeList.get(0);
					setPathSearchRequest(true);
					node.cancelReservationForVehicleDriveIn(this);
					if (detourNode == null) {
						detourNode = node;
					}
					lastDeadlockBrokenTime = System.currentTimeMillis();
					
					setDriveFailedNode(node);
					return "";
				}
			}
			return reason;
		} else {
			reason = "";
			
			// 2013.11.11 by MYM : Hybrid 주행 제어(교차로 근접, Vehicle 간섭 및 충돌설정 고려 주행)
			clearDriveFailCausedVehicleList();
			deadlockType = DEADLOCK_TYPE.NONE;
			lastDeadlockBrokenTime = 0;
			
			return stopNodeId;
		}
	}

	/**
	 * 2013.02.22 by KYK
	 * @param driveLimitTime
	 * @param station
	 * @return
	 * @throws Exception
	 */
	private String driveVehiclePathForNearby7(double driveLimitTime, OCSInfoManager ocsInfoManager, boolean isNearByNormalDrive) throws Exception {
		assert driveNodeList != null;
		assert routedNodeList != null;
		
		Node prevNode = getDriveCurrNode();
		if (prevNode == null) {
			return "";
		}		
		Node stopNode = getDriveStopNode();
		if (stopNode == null) {
			return "";
		}
		String stopNodeId = stopNode.getNodeId();
//		Hid vehicleCurrHID = stopNode.getHid();
		
		if (routedNodeList.size() == 0) {
			// 2013.04.05 by KYK
			if (hasDrivenToTargetNodeButNotStation()) {
				checkCommandSendableToTarget(stopNode, ocsInfoManager);
				return stopNodeId;
			}
			isPathSearchRequested = true;
			return stopNodeId;				
		}
		
		Node node;		
//		Vector<VehicleData> forwardVehicleList = new Vector<VehicleData>();
		HashSet<VehicleData> forwardVehicleList = new HashSet<VehicleData>();
		for (int i = 1; i < driveNodeList.size(); i++) {
			node = driveNodeList.get(i);
			if (node != null) {
				// 2015.05.31 by MYM : vehicle의 HID 영역과 다른 선행 Vehicle도 카운트하도록 수정
				// 배경 : vehicle의 Curr HID와 다른 선행 Vehicle이 카운트되지 않아 HID Capa를 Over해서 진입하는 경우가 발생
				// 새로운 HID로 진입할 때 Capacity 체크를 위한 선행 Vehicle의 수 카운트
//				if (node.getHid() == vehicleCurrHID) {
//					checkForwardVehicle(forwardVehicleList, node, vehicleCurrHID);
//				}
				if (i < driveNodeList.size() - 1) { // StopNode는 아래에서 체크
					checkForwardVehicle(forwardVehicleList, node);
				}
				
				prevNode = node;
				stopNodeId = node.getNodeId();
			}
		}
		// 2013.05.10 by KYK
		Block stopNodeBlock = prevNode.getBlock(null);
		
		// Check if stopnode is Diverge, (Only drive first vehicle .)
		double dQDivergeLimitTime = ocsInfoManager.getDrivingQueueDivergeLimitTime();
		String result = checkStopNodeInDivergeCase(prevNode,dQDivergeLimitTime);
		if (result != null) {
			return result;
		}
		
		// Check if prevnode is Diverge, drive just nextnode of diverge.
		boolean isPassedDivergeNode = false;
		if (prevNode.isDiverge()) {
			isPassedDivergeNode = true;
		}
		
		// Search drivable node(as a stopnode) in routedNodeList
		int driveCount = 0;
		try {
			for (int i = 0; i < routedNodeList.size(); i++) {
				node = routedNodeList.get(i);
				if (node != null) {
					// Already driven node ?
					if (driveNodeList.contains(node)) {
						break;
					}
					
					// 2015.06.03 by MYM : 전방 Forward Vehicle 카운트는 Drive 할 노드 전까지만 체크해야 함. (prevNode로 변경)
					// 2015.05.31 by MYM : vehicle의 HID 영역과 다른 선행 Vehicle도 카운트하도록 수정
					// 배경 : vehicle의 Curr HID와 다른 선행 Vehicle이 카운트되지 않아 HID Capa를 Over해서 진입하는 경우가 발생
					// Check Vehicle count (HID Capacity) entering new Hid
//					if (node.getHid() == vehicleCurrHID) {
//						checkForwardVehicle(forwardVehicleList, node, vehicleCurrHID);
//					}
					checkForwardVehicle(forwardVehicleList, prevNode);
					
					// Check if BlockNode
					Block block = null;
					// 2013.07.30 by KYK
//					if (stopNodeBlock != null && OcsConstant.DIVERGE.equals(stopNodeBlock.getBlockType())) {
					if (stopNodeBlock != null && stopNodeBlock.isDivergeType()) {
						block = node.getBlock(stopNodeBlock);
					} else {
						block = node.getBlock(null);
					}
					
					if (block == null) {
						// Check if node is drivable
						String driveResult = node.vehicleDriveIn(this, prevNode, forwardVehicleList.size(), isNearByNormalDrive);
						if (OcsConstant.OK.equals(driveResult)) {
							// 2013.04.05 by KYK : 합류전노드 전방 Station 을 target 으로 이동시 주행가능여부 체크
							if (isLastGoCommandToTargetStation(node.getNodeId())) {
								checkCommandSendableToTarget(node, ocsInfoManager);
							}
							reason = "";
							driveCount++;
						} else {
							node.vehicleDriveOut(this);
							reason = driveResult + forwardVehicleList; // Forward Vehicle Reason 추가
							setDriveFailedNode(node);
							break;
						}
					} else {
						synchronized (block) {
							// Converge Block Case.
							// 2013.07.30 by KYK
//							if (OcsConstant.CONVERGE.equals(block.getBlockType())) {
							if (block.isConvergeOrMultiType()) {
								// Check if FirstVehicle in Converge.
								// 2013.07.31 by KYK
//								String checkFirstVehicleResult = block.checkFirstVehicleInConverge7(this, ocsInfoManager);
								String checkFirstVehicleResult = block.checkFirstVehicleInConverge7(this, ocsInfoManager, null);
								if (OcsConstant.OK.equals(checkFirstVehicleResult)) {
									String driveResult = node.vehicleDriveIn(this, prevNode, forwardVehicleList.size(), isNearByNormalDrive);
									if (OcsConstant.OK.equals(driveResult)) {
										driveCount++;
										reason = "";
									} else {
										block.resetDrivingVehicle(this);
										node.vehicleDriveOut(this);
										setDriveFailedNode(node);
										reason = driveResult + forwardVehicleList; // Forward Vehicle Reason 추가
										break;
									}
								} else {
									reason = "DriveFail : " + checkFirstVehicleResult;
									// 2015.06.04 by MYM : Block 통과 실패시 DriveFailedNode 설정 추가 (DriveFail시 우회 기능 동작) 
									setDriveFailedNode(node);
									break;
								}
								
								// 2015.01.30 by KYK : 분기 다음노드에 끊어주기 위해 주석처리.(이슈:분기노드를 지나고 바로 합류이면 무시됨)
//								// 2012.03.08 by MYM : [분기 이후 Stop 명령 전송] 합류를 지난 경우에는 passedDivergeNode를 초기화 
//								isPassedDivergeNode = false;
							// 2013.07.30 by KYK
//							} else if (OcsConstant.DIVERGE.equals(block.getBlockType())) {
							} else if (block.isDivergeType()) {
								// Check Diverge Case
								String driveResult = node.vehicleDriveIn(this, prevNode, forwardVehicleList.size(), isNearByNormalDrive);
								if (OcsConstant.OK.equals(driveResult)) {
									driveCount++;
									block.setDrivingVehicle(this);
									// 2013.07.31 by KYK
									setOccupiedBlockList(block, null);
									reason = "";

									// Check if diverge, send GoCommand just here. 
									if (node.equals(block.getMainBlockNode())) {
										// Check if FirstVehicle in Diverge (Only if Diverge and Virtual node)
										// 2013.10.22 by KYK : 분기노드 일단 명령 끊어서 줌 -> 주행가능하면 추가 주행 하도록 함
//										if (node.isVirtual()) {
//										} else {
//											break;
//										}
										String checkFirstVehicleResult = block.checkFirstVehicleInDiverge7(this, dQDivergeLimitTime);
										if (OcsConstant.OK.equals(checkFirstVehicleResult) == false) {
											StringBuffer log = new StringBuffer("DriveFail : ");
											log.append(stopNodeId).append(">").append(routedNodeList.get(0).getNodeId()).append(" ").append(checkFirstVehicleResult);
											if (reason.equals(log.toString()) == false) {
												reason = log.toString();
											}
											break;
										}
									}
								} else {
									node.vehicleDriveOut(this);
									setDriveFailedNode(node);
									reason = driveResult + forwardVehicleList; // Forward Vehicle Reason 추가
									break;
								}
							} else {
								reason = "";
								break;
							}
						}
					}

					// Check if prevnode is Diverge, drive just nextnode of diverge.
					// 끊어주지 않으면, 분기이후 합류통과시 OCS Route 와 OHT Route 다를 수 있음 (E-Stop발생)
					if (isPassedDivergeNode == true && node.isVirtual() == false) {
						break;
					} else if (node.isDiverge()) {
						isPassedDivergeNode = true;
					}
					prevNode = node;
				}
				
			}
		} catch (Exception e) {
			// 2013.07.05 by KYK 
			e.printStackTrace();
			traceOperationException("driveVehiclePathForNearby7()", e);
			
			while (driveCount > 0) {
				node = routedNodeList.get(driveCount - 1);
				node.vehicleDriveOut(this);
				setDriveFailedNode(node);
				driveCount--;
				Block block = node.getBlock(stopNodeBlock);
				if (block != null) {
					block.resetDrivingVehicle(this);
				}
			}
			return stopNodeId;
		}
		
		// Check stopnode is Virtual. 
		while (driveCount > 0) {
			node = routedNodeList.get(driveCount - 1);
			if (node.isVirtual() && driveCount != routedNodeList.size()) {
				node.vehicleDriveOut(this);
				setDriveFailedNode(node);
				driveCount--;
				Block block = node.getBlock(stopNodeBlock);
				if (block != null) {
					block.resetDrivingVehicle(this);
				}
			} else {
				break;
			}
		}
		
		// Set driveNodeList 
		stopNodeId = setDriveNodeList(driveCount);
		// 2013.07.05 by KYK
//		if (stopNodeId.equals(stopNode) && (reason != null && reason.length() > 0)) {
		if (stopNodeId.equals(stopNode.getNodeId()) && (reason != null && reason.length() > 0)) {
			// 2014.09.02 by zzang9un : deadlock break를 위해 추가
//			if (deadlockType == DEADLOCK_TYPE.NODE) {
			if (deadlockType != DEADLOCK_TYPE.NONE) {
				if (getDriveCurrNode() != null &&
						getDriveCurrNode().isDiverge() &&
						System.currentTimeMillis() - lastDeadlockBrokenTime > 5000) {
					
					node = routedNodeList.get(0);
					setPathSearchRequest(true);
					node.cancelReservationForVehicleDriveIn(this);
					if (detourNode == null) {
						detourNode = node;
					}
					lastDeadlockBrokenTime = System.currentTimeMillis();
					
					setDriveFailedNode(node);
					return "";
				}
			}
			return reason;
		} else {
//			reason = "";
			
			// 2013.11.11 by MYM : Hybrid 주행 제어(교차로 근접, Vehicle 간섭 및 충돌설정 고려 주행)
			// 2015.01.07 by zzang9un : v3.1에 있던 코드가 누락되어 추가함
			// reason은 계속 남겨두고 caused vehicle만 clear함(강연국 선임과 협의)
			clearDriveFailCausedVehicleList();
			deadlockType = DEADLOCK_TYPE.NONE;
			lastDeadlockBrokenTime = 0;
			
			return stopNodeId;
		}
	}
	
	/**
	 * 2013.04.12 by KYK
	 * @param prevNode
	 * @param dQLimitTime
	 * @return
	 */
	private String checkStopNodeInDivergeCase(Node prevNode, double dQLimitTime) {
		if (prevNode != null) {
			String stopNodeId = prevNode.getNodeId();
			Block stopNodeBlock = prevNode.getBlock(null);
			// 2013.07.30 by KYK
//			if (stopNodeBlock != null && OcsConstant.DIVERGE.equals(stopNodeBlock.getBlockType())) {
			if (stopNodeBlock != null && stopNodeBlock.isDivergeType()) {
				synchronized (stopNodeBlock) {
					String checkFirstVehicleResult = stopNodeBlock.checkFirstVehicleInDiverge7(this, dQLimitTime);
					if (OcsConstant.OK.equals(checkFirstVehicleResult) == false) {
						StringBuffer log = new StringBuffer("DriveFail : ");
						log.append(stopNodeId).append(">").append(routedNodeList.get(0).getNodeId()).append(" ").append(checkFirstVehicleResult);
						if (reason.equals(log.toString()) == false) {
							reason = log.toString();
							return reason;
						} else {
							return stopNodeId;
						}
					} else {
						reason = "";
					}
				}
			}
		}
		return null;
	}

	/**
	 * 2013.04.05 by KYK
	 * @return
	 */
	private boolean hasDrivenToTargetNodeButNotStation() {
		if (stopNode != null) {
			if (stopNode.equals(targetNode)) {
				if (targetStation != null && targetStation.length() > 0) {
					if (targetStation.equals(stopStation) == false) {
						return true;
					}
				}
			}			
		}
		return false;
	}

	/**
	 * 2013.04.05 by KYK
	 * @param node
	 */
	private String checkCommandSendableToTarget(Node node, OCSInfoManager ocsInfoManager) {
		String result = "";
		for (int j = 0; j < node.getSectionCount(); j++) {
			Section section = node.getSection(j);
			int index = section.getNodeIndex(node);
			Node nextNode = section.getNode(index + 1);
			// 2013.04.05 by KYK
			if (nextNode != null && nextNode.isConverge()) {
				// synchronized ??
				Block nextBlock = nextNode.getBlock(null);
				// 2013.07.30 by KYK
//				if (nextBlock != null && OcsConstant.CONVERGE.equals(nextBlock.getBlockType())) {
				if (nextBlock != null && nextBlock.isConvergeOrMultiType()) {
					// 합류통과가능여부확인
					// 2013.07.31 by KYK
//					result = nextBlock.checkFirstVehicleInConverge7(this, ocsInfoManager);
					result = nextBlock.checkFirstVehicleInConverge7(this, ocsInfoManager, targetStation);
					if (OcsConstant.OK.equals(result) == false) {
						setDriveFailedNode(node); // ??
						setStationDriveAllowed(false);
						break;
					}
				}
			}										
		}
		return result;
	}

	/**
	 * 2013.04.05 by KYK
	 * @param nodeId
	 * @return
	 */
	private boolean isLastGoCommandToTargetStation(String nodeId) {
		if (nodeId != null) {
			if (nodeId.equals(targetNode)) {
				if (targetStation != null && targetStation.length() > 0) {
					return true;
				}
			}			
		}
		return false;
	}

//	/**
//	 *
//	 * @param forwardVehicleList
//	 * @param node
//	 * @param hid
//	 */
//	private void checkForwardVehicle(Vector<VehicleData> forwardVehicleList, Node node, Hid hid) {
//		for (int i = 0; i < node.getDriveVehicleCount(); i++) {
//			VehicleData occupiedVehicle = node.getDriveVehicle(i);
//
//			// 2012.07.13 by MYM : [NotNullCheck] 추가
//			// Drive Vehicle과 노드에 점유한 Vehicle이 같은지 확인
//			if (occupiedVehicle == null || this.equals(occupiedVehicle)) {
//				continue;
//			}
//
//			// 선행 Vehicle이 아니면 제외
//			if (node.equals(occupiedVehicle.getDriveCurrNode()) == false) {
//				continue;
//			}
//
//			// 중복 확인
//			if (forwardVehicleList.contains(occupiedVehicle)) {
//				continue;
//			}
//
//			Node driveVehicleStopNode = occupiedVehicle.getDriveStopNode();
//			if (occupiedVehicle.getDriveCurrNode().equals(driveVehicleStopNode)) {
//				forwardVehicleList.add(occupiedVehicle); // 정지 호기 추가
//			} else if (driveVehicleStopNode != null &&
//					driveVehicleStopNode.getHid() == hid) {
//				forwardVehicleList.add(occupiedVehicle); // 주행중인 호기중 동일 HID영역인 호기 추가
//			}
//		}
//	}
	
	/**
	 * 2015.07.08 by KYK : 동일노드 후방 Vehicle 은 카운트하지 않도록 수정 (offset 으로 판단)
	 * 
	 * 2015.05.31 by MYM : forward Vehicle 계산 방식 변경
	 * 배경 : vehicle의 Curr HID와 다른 선행 Vehicle이 카운트되지 않아 HID Capa를 Over해서 진입하는 경우가 발생하여
	 *       vehicle의 HID 영역과 다른 선행 Vehicle도 카운트하도록 수정
	 * 
	 * @param forwardVehicleSet
	 * @param node
	 */
//	private void checkForwardVehicle(HashSet<VehicleData> forwardVehicleSet, Node node) {
//		@SuppressWarnings("unchecked")
//		ArrayList<VehicleData> driveVehicleListClone = (ArrayList<VehicleData>) node.getDriveVehicleListClone();
//		for (VehicleData occupiedVehicle : driveVehicleListClone) {
//			// Drive Vehicle과 노드에 점유한 Vehicle이 같은지 확인
//			if (occupiedVehicle == null || this.equals(occupiedVehicle)) {
//				continue;
//			}
//			
//			// node에 위치한 Vehicle 추가(AG, AA 모두) 
//			if (node.equals(occupiedVehicle.getDriveCurrNode())) {
//				forwardVehicleSet.add(occupiedVehicle);
//			}
//		}
//	}
	private void checkForwardVehicle(HashSet<VehicleData> forwardVehicleSet, Node node) {
		@SuppressWarnings("unchecked")
		ArrayList<VehicleData> driveVehicleListClone = (ArrayList<VehicleData>) node.getDriveVehicleListClone();
		if (driveVehicleListClone != null) {
			boolean isNodeLocated = false;
			if (node != null && node.equals(this.getDriveCurrNode())) {
				isNodeLocated = true;
			}
			for (VehicleData occupiedVehicle : driveVehicleListClone) {
				// Drive Vehicle과 노드에 점유한 Vehicle이 같은지 확인
				if (occupiedVehicle == null || this.equals(occupiedVehicle)) {
					continue;
				}
				// node에 위치한 Vehicle 추가(AG, AA 모두) 
				if (node.equals(occupiedVehicle.getDriveCurrNode())) {
					if (isNodeLocated) {
						if (occupiedVehicle.getCurrNodeOffset() > this.getCurrNodeOffset()) {
							forwardVehicleSet.add(occupiedVehicle);
						}
					} else {
						forwardVehicleSet.add(occupiedVehicle);
					}
				}
			}
		}
	}
	
	public boolean setInitializeRoutedNodeList(LinkedList<Node> routedNodeList) {
		if (driveNodeList.size() == 0 || routedNodeList.size() == 0) {
			return false;
		}
		Node node;
		locusData.reset();
		synchronized (this.routedNodeList) {
			for (int i = 0; i < this.routedNodeList.size(); i++) {
				node = this.routedNodeList.get(i);
				if (node != null) {
					node.removeRoutedVehicle(this);
				}
			}
			this.routedNodeList.clear();
			for (int i = 0; i < routedNodeList.size(); i++) {
				node = routedNodeList.get(i);
				if (i > 0) {
					this.routedNodeList.add(node);
				}
			}
		}
		
		try {
//			repathSearchCheckNodeList.clear();
			setRepathSearchNeeded(false);
		} catch (Exception e) {
		}
		lastPathSearchedTime = System.currentTimeMillis();
		return true;
	}

	/**
	 * 2013.02.08 by KYK
	 * @param routedNodeList
	 * @return
	 */
	public boolean setRoutedNodeList(LinkedList<Node> routedNodeList) {
		if (driveNodeList.size() == 0 || routedNodeList.size() == 0) {
			return false;
		}

		int startIndex = 0;
		String drivedNodeId = null;
		
		Node node;
		Node startNode = routedNodeList.get(0);
		synchronized (driveNodeList) {
			node = driveNodeList.get(driveNodeList.size() - 1);
			drivedNodeId = node.getNodeId();
			if (node != null) {
				// driveNodeList 의 마지막위치와 routedNodeList 시작위치가 같은가?
				if (node.equals(startNode) == false) {
					// 포인트에 위치한 경우 NextNode 위치부터 경로찾음
					// 다만, 이때 두노드가 인접노드인지 확인필요할듯
					startIndex = 0;
				} else {
					startIndex = 1;
				}
			}
		}
		
		// 2015.08.08 by MYM : batch 업데이트 하도록 변경 (Vehicle 상태 변경 업데이트시 함께 업데이트 함)
		StringBuilder locus = new StringBuilder();
		for (Node drivedNode : driveNodeList) {
			if (drivedNode != null) {
				if (locus.length() > 0) {
					locus.append(",");
				}
				locus.append(drivedNode);
			}
		}
		
		locusData.reset();
		synchronized (this.routedNodeList) {
			for (int i = 0; i < this.routedNodeList.size(); i++) {
				node = this.routedNodeList.get(i);
				if (node != null) {
					node.removeRoutedVehicle(this);
				}
			}
			this.routedNodeList.clear();

			locusData.setMessageItem("Locus", drivedNodeId, true);			
			for (int i = startIndex; i < routedNodeList.size(); i++) {
				node = routedNodeList.get(i);
				this.routedNodeList.add(node);
				node.addRoutedVehicle(this);
				if (routedHidList.contains(node.getHid()) == false) {
					routedHidList.add(node.getHid());
				}
				
				// 2015.08.08 by MYM : batch 업데이트 하도록 변경 (Vehicle 상태 변경 업데이트시 함께 업데이트 함)
				if (locus.length() > 0) {
					locus.append(",");
				}
				locus.append(node);
				
				locusData.setMessageItem("Locus", node.getNodeId(), true);
			}
		}
		
		// 2015.08.08 by MYM : batch 업데이트 하도록 변경 (Vehicle 상태 변경 업데이트시 함께 업데이트 함)
		this.locus = locus.toString();
		
		try {
			repathSearchCheckNodeSet.clear();
			setRepathSearchNeeded(false);
			Node prevIntersection = null;
			for (Node routedNode : routedNodeList) {
				if (routedNode != null) {
					if (routedNode.isDiverge()) {
						if (prevIntersection != null) {
							repathSearchCheckNodeSet.add(prevIntersection);
						}
						prevIntersection = routedNode;
					} else if (routedNode.isConverge()) {
						prevIntersection = routedNode;
					}
				}
			}
		} catch (Exception e) {
		}
		
		// 2015.01.20 by MYM : 장애 지역 우회 기능
		this.searchFailReason = "";
		lastPathSearchedTime = System.currentTimeMillis();
		
		return true;
	}

	/**
	 * 
	 * @param driveCount
	 * @return
	 */
	private String setDriveNodeList(int driveCount) {
		if (driveCount <= 0 || driveCount > this.routedNodeList.size()) {
			return getDriveStopNode().getNodeId();
		}

		Node node;
		String stopNode = "";
		synchronized (this.routedNodeList) {
			while (driveCount > 0) {
				node = this.routedNodeList.remove(0);
				if (node != null) {
					this.driveNodeList.add(node);

					// 2011.12.27 by PMM
					node.cancelReservationForVehicleDriveIn(this);
					
					// 2012.01.05 by PMM
					vehicleLocusList.add(node);
					
					node.removeArrivedTimeInfo(vehicleId);
					stopNode = node.getNodeId();
					driveCount--;
				} else {
					traceOperationException("setDriveNodeList(int driveCount) - node is null.");
				}
			}
		}
		return stopNode;
	}

	public int getRoutedNodeCount() {
		assert routedNodeList != null;
		
		return routedNodeList.size();
	}
	
	public int getDriveNodeCount() {
		assert driveNodeList != null;
		
		return driveNodeList.size();
	}
	
	public void getInTheWay(HashSet<Node> inTheWayNodeSet) {
		synchronized (driveNodeList) {
			for (Node node : driveNodeList) {
				inTheWayNodeSet.add(node);
			}
		}
		synchronized (routedNodeList) {
			for (Node node : routedNodeList) {
				inTheWayNodeSet.add(node);
			}
		}
	}

	public Node getDriveCurrNode() {
		assert driveNodeList != null;
		
		synchronized (driveNodeList) {
			if (driveNodeList.size() > 0) {
				return driveNodeList.get(0);
			} else {
				return null;
			}
		}
	}

	public Node getDriveStopNode() {
		assert driveNodeList != null;
		
		synchronized (driveNodeList) {
			if (driveNodeList.size() > 0) {
				return driveNodeList.get(driveNodeList.size() - 1);
			} else {
				return getDriveCurrNode();
			}
		}
	}

	public Node getDriveTargetNode() {
		assert routedNodeList != null;
		
		synchronized (routedNodeList) {
			if (routedNodeList.size() > 0) {
				return routedNodeList.get(routedNodeList.size() - 1);
			} else {
				return getDriveStopNode();
			}
		}
	}

	public boolean hasArrivedAtTargetNode() {
		if (getDriveCurrNode() == getDriveStopNode()
				&& getDriveCurrNode() == getDriveTargetNode()) {
			return true;
		}

		return false;
	}
	
	// 2013.03.22 by KYK
	public boolean hasArrivedAtTarget() {
		if (hasArrivedAtTargetNode()) {
			if (currStation == null) {
				currStation = "";
			}
			if (stopStation == null) {
				stopStation = "";
			}
			if (targetStation == null) {
				targetStation = "";
			}
			
			if (stopStation.equals(targetStation) && currStation.equals(stopStation)) {
				return true;
			}
			// ??
			if (currStation.equals(stopStation) && currNodeOffset == 0) {
				return true;
			}
			
//			// 보완코드 : offset=0 인 station 위치해 있는데 해당 노드로 이동시
//			if (targetStation.length() == 0) {
//				if (stopStation.length() > 0) {
//					Station stopStation = stationManager.getStation(stopStation);
//					if (stopStation == null || stopStation.getOffset() > 0) {
//						return false;
//					}
//				}
//				if (currStation.length() > 0) {
//					Station currStation = stationManager.getStation(currStation);
//					if (currStation == null || currStation.getOffset() > 0) {
//						return false;
//					}
//				}
//				return true;
//			}
		}
		return false;
	}
	
	/**
	 * 2011.12.12 by MYM : StopNode에 도착 여부
	 * @return
	 */
	public boolean hasArrivedAtStopNode() {
		if (getDriveCurrNode() == getDriveStopNode()) {
			return true;
		}
		
		return false;
	}

	public Node getDriveNode(int index) {
		assert driveNodeList != null;
		
		return driveNodeList.get(index);
	}

	public Node getRoutedNode(int index) {
		assert routedNodeList != null;
		
		return routedNodeList.get(index);
	}

	public int getDriveNodeIndex(Node node) {
		assert driveNodeList != null;
		
		return driveNodeList.indexOf(node);
	}
	
	public int getRoutedNodeIndex(Node node) {
		assert routedNodeList != null;
		
		return routedNodeList.indexOf(node);
	}

	public boolean containsRoutedNode(Node node) {
		assert routedNodeList != null;
		
		return routedNodeList.contains(node);
	}

	public boolean containsDriveNode(Node node) {
		assert driveNodeList != null;
		
		return driveNodeList.contains(node);
	}

	/**
	 * 2011.12.30 by MYM : 
   * DrivingQueue에 Vehicle 도착 정보 업데이트 -> Node에 Vehicle 도착 정보 업데이트하도록 변경
   * UserBlock, DrivingQueue 설정시 Flexible하게 대응하기 위함
	 */
	private void updateDrivingQueue(Node prevNode, Node currNode) {
		// 2012.07.13 by MYM : try catch 추가
		try {
			// 2012.06.01 by MYM : Block의 DrivingVehicle을 먼저 Reset하고 PrevNode에 Vehicle 정보 제거하도록 순서 변경
			// 배경 : PrevNode에 Vehicle 정보 제거 ~ Block의 DrivingVehicle을 Reset하는 중간에 동일 Path에서
			//       후행하는 Vehicle이 Block 점유 조건을 판단할 때 Vehicle 순서 뒤바뀜 현상으로 오판단 할 수 있음.
			// 1. Block의 DrivingVehicle 정보 Reset
			if (currNode != null) {
				for (int i = 0; i <= driveNodeList.indexOf(currNode); i++) {
					Node drivingQueueNode = driveNodeList.get(i);
					if (drivingQueueNode != null) {
						// 2011.11.29 by MYM : 한 노드에 2개 이상의 Block에 BlockNode가 될 수 있음. 모든 Block에 대해서 Reset하도록 수정 
						ArrayList<Block> blockList = new ArrayList<Block>();
						drivingQueueNode.getBlocks(blockList);
						for (int j = 0; j < blockList.size(); j++) {
							Block block = blockList.get(j);
							// 2012.06.09 by MYM : Block의 drivingVehicle를 List로 변경
							if (block != null && block.containsDrivingVehicle(this)
									&& drivingQueueNode.equals(block.getMainBlockNode())) {
								block.resetDrivingVehicle(this);
							}
						}
					}
				}
			}
			// 2014.02.20 by KYK : Reset DQ Entered Info in Block
			Node node = null;
			tempBlockList.clear();
			tempBlockList = (HashSet<Block>) enteringBlockList.clone();
			for (Block block: tempBlockList) {
				node = block.getMainBlockNode();
				if (driveNodeList.contains(node) == false && routedNodeList.contains(node) == false) {
					block.resetDQEnteringInfo(this);
				}
			}
			
			// 2. CurrNode, PrevNode에 DriveVehicle를 업데이트 한다.
			if (currNode != null) {
				currNode.setDriveVehicleInfo(this, System.currentTimeMillis());
			}
			if (prevNode != null && prevNode.equals(currNode) == false) {
				prevNode.resetDriveVehicleInfo(this);
			}
			
			// 3. CurrNode, PrevNode의 Block을 가져온다.
			ArrayList<Block> blockList = new ArrayList<Block>();
			if (currNode != null) {
				currNode.getAllBlocks(blockList);
			}
			if (prevNode != null && prevNode.equals(currNode) == false) {
				prevNode.getAllBlocks(blockList);			
			}
			
			// 4. Block의 DrivingQueue에 Vehicle 위치를 업데이트 한다.
			for (int i = 0; i < blockList.size(); i++) {
				Block block = (Block)blockList.get(i);
				// 2011.10.25 by PMM Not Null 체크 추가
				if (block != null && currNode != null) {
					block.writeUpdatedVehicle(currNode, isBlockPreemptionUpdateUsed);
				}
			}
		} catch (Exception e) {
			traceOperationException("updateDrivingQueue()", e);
		}
	}
	
	/**
	 * 
	 * @param vehicleMode
	 * @param vehicleState
	 * @param currNode
	 * @param isNearByDrive
	 * @return
	 */
	public boolean updateDriveNode(char vehicleMode, char vehicleState, Node currNode, boolean isNearByDrive) {
		if (currNode == null) {
			return false;
		}

		if (prevVehicleMode == vehicleMode && prevState == vehicleState && currNode.equals(prevCurrNode)) {
			return true;
		}
		
		// DrivingQueue 업데이트
		if (isNearByDrive) {
			updateDrivingQueue(prevCurrNode, currNode);
		}

		try {
			if (driveNodeList.size() == 0) {
				driveNodeList.add(currNode);
				return currNode.vehicleInitialize(this, isNearByDrive);
			} else {
				if (isAbnormalVehicle()) {
					requestRepathSearch(isNearByDrive);
				}
				if (vehicleMode == 'A' && vehicleState == 'I') {
//					isPathSearchRequested = true;
					setPathSearchRequest(true);

					// 2012.06.11
					boolean result = reset(isNearByDrive, currNode);
					
					// 2016.05.24 by KBS : 비근접 라인에서 Z 처리 보완
					if (isNearByDrive == false && prevVehicleMode == 'A' && (prevState != 'V' && prevState != 'Z')) {
						// 이전 Mode가 Auto인 경우는 통신 Fail이 발생한 경우에만 가능하며, 확인이 필요할 수 있어 false를 return
						// 이전 Mode가 Auto이고 이전 Status가 ErrorRecovery가 아닌 경우에는 무조건 false를 return
						// 이전 Mode가 Auto이고 이전 Status가 PatrolCancel이 아닌 경우에는 무조건 false를 return
						return false;
					} else {
						return result;
					}
				} else {
					int currNodeIndex = driveNodeList.indexOf(currNode);
					if (currNodeIndex >= 0) {
						// 2014.03.27 by MYM : [Hybrid 주행 제어]
						// currNode의 Collision 정보에 Vehicle 업데이트
						if (isNearByDrive == false || currNode.isCheckCollision()) {
							currNode.updateCollision(this);
						}
						
						// DriveNodeList에서 현재위치 이전까지 DriveOut 처리
						Node node;
						for (int i = 0; i < currNodeIndex; i++) {
							node = driveNodeList.remove(0);
							if (node != null) {
								node.vehicleDriveOut(this);
								node.removeRoutedVehicle(this);
								// 2015.08.08 by MYM : locus는 경로 탐색 결과로 사용하도록 변경
//								if (locus.length() < 3990) {
//									locus.append(node.getNodeId()).append(" ");
//								}
							}
						}

						if (vehicleMode == 'M') {
							// 2012.07.11 by MYM : Manual일 때는 무조건 Route 정보를 초기화
							resetRoutedNodeList();
							
							if (vehicleState == 'E') {
								// Vehicle이 ManualError인 경우에는 5Node, 5m까지로 StopNode위치를 설정
								Node prevNode = currNode;
								double length = 0.;
								for (int i = 1; i < driveNodeList.size(); i++) {
									node = driveNodeList.get(i);
									if (node != null) {
										
										// 2012.06.11
//										length += prevNode.getLength(node);
										length += node.getMoveInDistance(prevNode);
										
										if (length > 5000 || i >= 5) {
											for (int j = driveNodeList.size() - 1; j > i; j--) {
												node = driveNodeList.remove(j);
												if (node != null) {
													node.vehicleDriveOut(this);
													node.removeRoutedVehicle(this);
												}
											}
											break;
										}
										prevNode = node;
									}
								}
							} else {
								// 2013.10.07 by KYK : MI처리 위치이동 Init 이외에 Not Error 도 처리함
								// Vehicle이 ManualInit에서 Node가 이동한 경우에 DriveNodeList상에서 CurrNode를 StopNode 위치로 설정
								for (int j = driveNodeList.size() - 1; j > 0; j--) {
									node = driveNodeList.remove(j);
									if (node != null) {
										node.vehicleDriveOut(this);
										node.removeRoutedVehicle(this);
									}
								}
							}
							
							// 2013.07.31 by KYK
							if (isNearByDrive) {
								resetAbnormalOccupiedBlockList();
							}
							
							// 2015.02.04 by MYM : 장애 지역 우회 기능
							if (vehicleMode == 'M' && prevVehicleMode == vehicleMode) {
								updateAbnormalSectionForManualMoving(currNode, true);
							}
						} else {
							// 2015.12.21 by KBS : Patrol VHL 기능 추가
							if (vehicleState == 'Z') {
								resetRoutedNodeList();	
								
								for (int j = driveNodeList.size() - 1; j > 0; j--) {
									node = driveNodeList.remove(j);
									if (node != null) {
										node.vehicleDriveOut(this);
										node.removeRoutedVehicle(this);
									}
								}
								
								if (isNearByDrive) {
									resetAbnormalOccupiedBlockList();
								}
							}

						}
						return true;
					} else {
						if (vehicleMode == 'M') {
							// 2012.07.11 by MYM : Manual일 때는 무조건 Route 정보를 초기화
							resetRoutedNodeList();
						}
						resetDriveNodeList(isNearByDrive, currNode);

						// 2015.11.10 by MYM : Manual인 경우만 동작하도록 조건문 위에서 여기로 위치 이동
						//					   Vehicle을 Manaul로 위치 이동시켰을 때 장애 영역을 갱신하는 코드임. 
						// 2015.02.04 by MYM : 장애 지역 우회 기능
						if (vehicleMode == 'M' && prevVehicleMode == vehicleMode) {
							updateAbnormalSectionForManualMoving(currNode, false);
						}
						return false;
					}
				}
			}
		} catch (Exception e) {
			traceOperationException("updateDriveNode()", e);
		} finally {
			prevVehicleMode = vehicleMode;
			prevState = vehicleState;
			prevCurrNode = currNode;
		}

		return false;
	}

	public boolean isNotInTheSameZone() {
		if (zone.equals(getDriveStopNode().getZone())) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param driveNode
	 * @return
	 */
	public boolean checkHidLimitOverPass(Node driveNode) {
		if (driveNode == null) {
			return false;
		}

		Node prevNode = driveNode;
		boolean checkStart = false;
		String driveNodeHidName = driveNode.getHid().getUnitId();
		String hidName = "";
		for (int i = 0; i < this.routedNodeList.size(); i++) {
			Node node = this.routedNodeList.get(i);
			if (node != null) {
				if (checkStart == false
						&& (node.getNodeId().equals(driveNode.getNodeId()))) {
					checkStart = true;
				}
				
				if (checkStart == true) {
					hidName = node.getHid().getUnitId();
					String collisionOccupiedVehicle = node.getCollisionOccupiedVehicle(this);
					if (driveNodeHidName.equals(hidName) == false &&
							node.getDriveVehicleCount() == 0 &&
							(collisionOccupiedVehicle != null && collisionOccupiedVehicle.length() == 0)) {
						// DriveNode 시작에서부터 계속적인 검색을 진행하면서 HIDName이 다른 Node를 만났으면 HID 경유 가능으로 판단.
						StringBuffer log = new StringBuffer();
						// 2015.06.01 by MYM : 로그 기록 추가
//						log.append(this.vehicleId).append("> HIDLimit Over Pass (CurrNode-").append(this.currNode).append(", DriveNode-").append(driveNode.getNodeId());
						log.append("HIDLimit Over Pass (CurrNode:").append(this.currNode).append(", DriveNode:").append(driveNode.getNodeId());
						log.append(") : ").append(driveNode.getNodeId()).append(" ~ ").append(prevNode.getNodeId()).append(", ").append(node.getNodeId());
						traceOperation(log.toString());
						return true;
					} else if (node.getDriveVehicleCount() > 0) {
						// DriveNode 시작에서부터 계속적인 검색을 진행하면서 충돌/간섭 등으로 인해 Drive Fail이 발생되면 HID
						// 경유 가능 불가 판단.
						return false;
					}
				}
				prevNode = node;
			} else {
				StringBuilder message = new StringBuilder();
				message.append("checkHidLimitOverPass(Node driveNode) - node is null.");
				traceOperationException(message.toString());
			}
		}
		return false;
	}

	/**
	 * 
	 */
	public void checkPauseRequestToBackwardVehicle() throws Exception {
//	public void checkPauseRequestToBackwardVehicle() {
// 2012.01.19 by PMM
		Node currNode = getDriveCurrNode();
		if (currNode == null) {
			return;
		}

		int nodeCountRange = 2;
		double distanceLimitRange = 1500;
		for (int i = 0; i < currNode.getSectionCount(); i++) {
			Section section = currNode.getSection(i);
			int j = section.getNodeIndex(currNode);
			double distance = 0;
			int count = 0;
			for (; j >= 0; j--) {
				Node node = section.getNode(j);
				if (node != null) {
					distance = currNode.getLength(node);
					if (distance > distanceLimitRange && count > nodeCountRange) {
						break;
					}
					
					for (int k = 0; k < node.getDriveVehicleCount(); k++) {
						VehicleData vehicle = node.getDriveVehicle(k);
						if (vehicle != null) {
							if (this.equals(vehicle) == true) {
								continue;
							}
							
							// 2011.11.07 by MYM : 버그 수정
//							if (vehicle.getPauseRequestVehicle().equals(vehicle.getVehicleId()) == false
							if (vehicle.getPauseRequestVehicle() == null &&
									vehicle.containsDriveNode(currNode) &&
									node.equals(vehicle.getDriveCurrNode()) &&
									vehicle.hasArrivedAtStopNode() == false) {
								vehicle.setPauseRequestVehicle(this);
							}
						}
					}
					count++;
				}
			}
		}
	}

	/**
	 * 
	 * @param yieldRequestLimitTime
	 * @return
	 */
	public String checkYieldRequestForForwardVehicleDetection(int yieldRequestLimitTime) throws Exception {
//	public String checkYieldRequestForForwardVehicleDetection(int yieldRequestLimitTime) {
// 2012.01.19 by PMM
		assert this.currNode != null;
		
		// StopNode에 도착하기 전 StopNode 보다 앞에 Vehicle이 존재할 경우에 양보 요청함.
		// 2013.04.12 by KYK
//		if (this.currNode.equals(this.stopNode)) {
//			return "";
//		}
		if (currStation == null) {
			currStation = "";
		}
		if (stopStation == null) {
			stopStation = "";
		}
		if (currNode.equals(stopNode) && currStation.equals(stopStation)) {
			return "";
		}

		// 2011.12.21 by MYM : currNode부터 검색하는 것으로 변경
		// 배경 : 분기 후 ByPass로 주행할 Vehicle이 직진 전방 IDLE Vehicle이 존재할 경우 대차 감지하여 ByPass로 진입하지 않는 현상 발생하여 양보하도록 함
		
		// 2011.12.01 by MYM : Section이 짧은 구간은 이후 Section에 위치하고 있는 Vehicle에게 양보를 하지 못하는 현상 발생
		// 1) currNode -> stopNode를 가져오는 것으로 수정
		// 2) 기존 StopNode의 Section만 검색 대상에서 -> 전방으로 계속 검색으로 변경 
		Node stopNode = getDriveCurrNode();
//		Node stopNode = getDriveStopNode();
		if (stopNode == null) {
			return "";
		}

		StringBuffer log = new StringBuffer();
		double arrivedTime = 0.0;
		int nodeCount = 0;

		Vector<Node> nodeQueue = new Vector<Node>();
		Vector<Double> costQueue = new Vector<Double>();
		Vector<Integer> nodeCountQueue = new Vector<Integer>();
		nodeQueue.add(stopNode);
		costQueue.add(new Double(arrivedTime));
		nodeCountQueue.add(new Integer(nodeCount));
		
		VehicleData vehicle = null;
		Node currNode = null;
		Node nextNode = null;
		Section section = null;
		int pos = 0;
		int count = 0;
		double timeCost = 0;
		boolean existVehicle = false;
		while (true) {
			if (nodeQueue.size() <= 0) {
				break;
			}
			stopNode = nodeQueue.remove(0);
			if (stopNode != null) {
				arrivedTime = costQueue.remove(0).doubleValue();
				nodeCount = nodeCountQueue.remove(0).intValue();
				for (int i = 0; i < stopNode.getSectionCount(); i++) {
					timeCost = arrivedTime;
					count = nodeCount;
					section = stopNode.getSection(i);
					if (section != null) {
						currNode = stopNode;
						if (currNode != null) {
							pos = section.getNodeIndex(currNode);
							for (int j = pos + 1; j < section.getNodeCount(); j++) {
								nextNode = section.getNode(j);
								if (nextNode != null) {
									timeCost += nextNode.getMoveInTime(currNode);
									if (timeCost > yieldRequestLimitTime && count >= 3) {
										break;
									}
									existVehicle = false;
									for (int k = 0; k < nextNode.getDriveVehicleCount(); k++) {
										vehicle = nextNode.getDriveVehicle(k);
										// 2012.07.13 by MYM : [NotNullCheck] 추가
										if (vehicle != null && this.equals(vehicle) == false) {
											if (nextNode.equals(vehicle.getDriveCurrNode())) {
												if (nextNode.equals(vehicle.getDriveStopNode())
														&& nextNode.equals(vehicle.getDriveTargetNode())) {
													if (vehicle.isYieldRequested() == false) {
														vehicle.requestYield(this);
														// 2016.03.18 by KYK : 다른쪽 패스도 체크해서 양보요청
														log.append(vehicle.getVehicleId()).append("(").append(nextNode.getNodeId()).append(")");
//														return log.toString();
													}
												}
												existVehicle = true;
												break;
											}
										}
									}
									if (existVehicle) {
										break;
									}
									currNode = nextNode;
									count++;
									if (j == section.getNodeCount() - 1) {
										int k = 0;
										for (k = 0; k < costQueue.size(); k++) {
											if (timeCost < costQueue.get(k).doubleValue()) {
												break;
											}
										}
										nodeQueue.add(k, currNode);
										costQueue.add(k, new Double(timeCost));
										nodeCountQueue.add(new Integer(count));
									}
								}
							}
						}
					}
				}
			}
		}
		return log.toString();
	}

	/**
	 * 
	 * @param time
	 * @return
	 */
	public boolean checkArrivedTimeAtTargetNode(double time) {
		assert routedNodeList != null;
		assert driveNodeList != null;
		
		if (routedNodeList.size() > 0) {
			return false;
		}
		if (driveNodeList.size() <= 1) {
			return true;
		}

		Node prevNode = getDriveCurrNode();
		if (prevNode == null) {
			return false;
		}

		Node node;
		double arrivedTime = 0.0;
		for (int i = 1; i < driveNodeList.size(); i++) {
			node = driveNodeList.get(i);
			if (node != null) {
				arrivedTime += node.getMoveInTime(prevNode);
				prevNode = node;
			}
		}

		if (arrivedTime > time) {
			return false;
		}
		return true;
	}
	
	/**
	 * 2013.02.22 by KYK 
	 * 노드간격이 멀수있기때문에 현재노드에서 지나온 offset 과 목적지노드에서 목적지까지의 offset 을 고려함
	 * @param time
	 * @return
	 */
	public boolean checkArrivedTimeAtTarget(double time, Station targetStation) {
		assert routedNodeList != null;
		assert driveNodeList != null;
		
		// 2013.02.22 by KYK
		double movedTimeFromParentNode = 0.0;
		double arrivedTimeToOffset = 0.0;
		double lastPathSpeed = 0.0;
		if (routedNodeList.size() > 0) {
			return false;
		}
		// 2015.07.08 by KYK : 어차피 서있으면 양보해야 한다. 안하면 뒤에서 못간다.
		if (driveNodeList.size() <= 1) {
			return true;
		}
		if (targetStation != null && targetStation.getOffset() > 0) {
			if (driveNodeList.size() > 0) {
				Node targetNode = targetStation.getParentNode();
				Node nextNode = targetStation.getNextNode();
				if (targetNode != null && nextNode != null) {
					for (int i = 0; i < targetNode.getSectionCount(); i++) {
						Section section = targetNode.getSection(i);
						if (section != null) {
							// link speed 를 참조한다. (section speed 를 쓰면 이상해진다, section 이 변경되었다.)
							Link link = section.getLink(targetNode, nextNode);
							if (link != null) {
								lastPathSpeed = link.getSpeed();
								if (lastPathSpeed > 0) {
									arrivedTimeToOffset = targetStation.getOffset() / lastPathSpeed;
									break;
								}
							}
						}
					}
				}
			}
		}
		
		double arrivedTime = 0.0;
		
		Node currNode = getDriveCurrNode();
		if (currNode != null) {
			Node prevNode = currNode;
			Node node;
			for (int i = 1; i < driveNodeList.size(); i++) {
				node = driveNodeList.get(i);
				if (node != null) {
					arrivedTime += node.getMoveInTime(prevNode);
					prevNode = node;
					
					if (i == 1 && currNodeOffset > 0) {
						for (int j = 0; j < node.getSectionCount(); j++) {
							Section section = node.getSection(j);
							if (section != null) {
								Link link = section.getLink(currNode, node);
								if (link != null) {
									double currPathSpeed = link.getSpeed();
									if (currPathSpeed > 0) {
										movedTimeFromParentNode = currNodeOffset / currPathSpeed;
										break;
									}
								}
							}
						}						
					}
				}
			}
		}
		// A-----현재위치------B---------C------목적지--------D
		// 노드간이동시간 : A~C
		// 이미지나간거리 : A~currOffset
		// 목적지에서더갈거리 : C~targetOffset
		arrivedTime += arrivedTimeToOffset - movedTimeFromParentNode;  
		if (arrivedTime > time) {
			return false;
		}
		return true;
	}

	public double getArrivedTimeAtTargetNode() {
		assert routedNodeList != null;
		assert driveNodeList != null;
		
		double arrivedTime = 0.0;
		if (routedNodeList.size() > 0) {
			return arrivedTime;
		}
		if (driveNodeList.size() <= 1) {
			return arrivedTime;
		}

		Node prevNode = getDriveCurrNode();
		if (prevNode == null) {
			return arrivedTime;
		}

		Node node;
		for (int i = 1; i < driveNodeList.size(); i++) {
			node = driveNodeList.get(i);
			if (node != null) {
				arrivedTime += node.getMoveInTime(prevNode);
				prevNode = node;
			}
		}

		return arrivedTime;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean checkYieldForBackwardVehicle() throws Exception {
//	public boolean checkYieldForBackwardVehicle() {
// 2012.01.19 by PMM
		if (hasArrivedAtTargetNode() == false) {
			return false;
		}
		
		Node node = getDriveCurrNode();
		if (node != null) {
			VehicleData vehicle;
			for (int k = 0; k < node.getDriveVehicleCount(); k++) {
				vehicle = node.getDriveVehicle(k);
				if (this == vehicle) {
					continue;
				}
				if (vehicle != null) {
					// 2011.11.04 by PMM
					// 타이밍 상 READY를 조건에 넣으면 양보가 안되는 경우가 생김.
//			if (vehicle.getVehicleMode() == 'A' && vehicle.getCurrCmd() != 0 &&
//					vehicle.getOperationControlState() == OPERAION_CONTROL_STATE.READY) {
					if (vehicle.getCurrCmd() != 0 && vehicle.getVehicleMode() == 'A') {
						// 2012.02.06 by PMM
//				this.setYieldRequest(true);
//				this.setYieldRequestedVehicle(vehicle);
						this.requestYield(vehicle);
						return true;
					}
				} else {
					StringBuilder message = new StringBuilder();
					message.append("checkYieldForBackwardVehicle() - vehicle is null. k:").append(k);
					traceOperationException(message.toString());
				}
			}
		} else {
			StringBuilder message = new StringBuilder();
			message.append("checkYieldForBackwardVehicle() - node(driveCurrNode) is null.");
			traceOperationException(message.toString());
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param yieldRequestLimitTime
	 * @return
	 */
	public String checkYieldRequestForPathDrive(int yieldRequestLimitTime, boolean isNearByDrive) throws Exception {
		yieldRequestedVehicleList.clear();
		
		Node prevNode = getDriveCurrNode();
		if (prevNode == null) {
			return "";
		}
		
		Node node = null;
		double time = 0.0;
		StringBuffer log = new StringBuffer();
		int nCount = 0;
		String result = "";

		// 2013.07.22 by KYK : 동일노드 전방호기에 양보요청 필요 (다른 station)
//		for (int i = 1; i < driveNodeList.size(); i++) {
		for (int i = 0; i < driveNodeList.size(); i++) {
			node = driveNodeList.get(i);
			if (node != null) {
				if (i > 0) {
					time += node.getMoveInTime(prevNode);
				}
				prevNode = node;
				if (time <= yieldRequestLimitTime || nCount < 3) {
					if (isNearByDrive) {
						result = yieldRequestForPathDriveForNearBy(node);
					} else {
						result = yieldRequestForPathDriveForNormal(node);
					}
					if (result != null && result.length() > 0) {
						log.append(result);
					}
				} else {
					return log.toString();
				}
				if (node.isVirtual() == false) {
					nCount++;
				}
			}
		}
		
		if (time <= yieldRequestLimitTime) {
			for (int i = 0; i < routedNodeList.size(); i++) {
				node = routedNodeList.get(i);
				if (node != null) {
					time += node.getMoveInTime(prevNode);
					prevNode = node;
					if (time <= yieldRequestLimitTime) {
						if (isNearByDrive) {
							result = yieldRequestForPathDriveForNearBy(node);
						} else {
							result = yieldRequestForPathDriveForNormal(node);
						}
						if (result != null && result.length() > 0) {
							log.append(result);
						}
					} else {
						break;
					}
				}
			}
		}
		yieldRequestedVehicleList.clear();

		return log.toString();
	}
	
	private String yieldRequestForPathDriveForNormal(Node node) {
		StringBuffer log = new StringBuffer();
		log.append(yieldRequestForPathDriveForNormalRecursive(node));
		
		ArrayList<Collision> collisionList = node.getCollisions();
		if (collisionList != null && collisionList.size() > 0) {
			Collision collision;
			Node collisionNode = null;
			Iterator<Collision> iterator = collisionList.iterator();
			while (iterator.hasNext()) {
				collision = (Collision)iterator.next();
				if (collision != null) {
					collisionNode = collision.getCollisionNode(node);
					if (collisionNode != null) {
						log.append(yieldRequestForPathDriveForNormalRecursive(collisionNode));
					}
				}
			}
		}
		return log.toString();
	}
	
	private String yieldRequestForPathDriveForNormalRecursive(Node node) {
		StringBuffer log = new StringBuffer();
		if (node != null) {
			VehicleData yieldVehicle = null;
			for (int j = 0; j < node.getDriveVehicleCount(); j++) {
				yieldVehicle = node.getDriveVehicle(j);
				
				// 2012.07.13 by MYM : [NotNullCheck] 추가
				if (yieldVehicle == null || this.equals(yieldVehicle)) {
					continue;
				}
				
				if (yieldVehicle != null) {
					if (node.equals(yieldVehicle.getDriveCurrNode())) {
						if (yieldVehicle.isYieldRequested() == false) {
							if (yieldVehicle.requestYield(this)) {
								if (yieldRequestedVehicleList.contains(yieldVehicle) == false) {
									yieldRequestedVehicleList.add(yieldVehicle);
									log.append(yieldVehicle.getVehicleId()).append("(").append(node.getNodeId()).append(") ");
								}
							} else {
								if (yieldRequestedVehicleList.contains(yieldVehicle) == false) {
									yieldRequestedVehicleList.add(yieldVehicle);
								}
							}
						}
						break;
					}
				}
			}
		}
		return log.toString();
	}
	
	private String yieldRequestForPathDriveForNearBy(Node node) {
		StringBuffer log = new StringBuffer();
		if (node != null) {
			Block block = node.getBlock(null);
			VehicleData yieldVehicle = null;
			if (block != null) {
				for (int j = 0; j < block.getBlockNodeCount(); j++) {
					Node blockNode = block.getBlockNode(j);
					StringBuilder state = new StringBuilder();
					state.append("Node:").append(node.getNodeId());
					state.append(", BlockId:").append(block.getBlockId());
					state.append(", BlockNodeCount:").append(block.getBlockNodeCount());
					state.append(", BlockNodeList:").append(block.getBlockNodeListString());
					state.append(", j:").append(j);
					
					if (blockNode != null) {
						for (int k = 0; k < blockNode.getDriveVehicleCount(); k++) {
							yieldVehicle = blockNode.getDriveVehicle(k);
							
							// 2012.07.13 by MYM : [NotNullCheck] 추가
							if (yieldVehicle == null || this.equals(yieldVehicle)) {
								continue;
							}
							if (yieldVehicle != null) {
								if (blockNode.equals(yieldVehicle.getDriveCurrNode())) {
									// 2013.10.25 by KYK : 동일노드(parentnode) 후방호기 양보대상 제외
									if (blockNode.equals(this.getDriveCurrNode())) {
										if (currNodeOffset > yieldVehicle.getCurrNodeOffset()) {
											continue;
										}
									}
									if (yieldVehicle.isYieldRequested() == false) {
										if (yieldVehicle.requestYield(this)) {
											if (yieldRequestedVehicleList.contains(yieldVehicle) == false) {
												yieldRequestedVehicleList.add(yieldVehicle);
												log.append(yieldVehicle.getVehicleId()).append("(").append(blockNode.getNodeId()).append(") ");
											}
										} else {
											if (yieldRequestedVehicleList.contains(yieldVehicle) == false) {
												yieldRequestedVehicleList.add(yieldVehicle);
											}
										}
									}
									break;
								}
							}
						}
					} else {
						StringBuilder message = new StringBuilder();
						message.append("checkYieldRequestForPathDrive() - blockNode is null. ");
						message.append(state.toString());
						traceOperationException(message.toString());
					}
				}
			} else {
				for (int j = 0; j < node.getDriveVehicleCount(); j++) {
					yieldVehicle = node.getDriveVehicle(j);
					
					// 2012.07.13 by MYM : [NotNullCheck] 추가
					if (yieldVehicle == null || this.equals(yieldVehicle)) {
						continue;
					}
					
					if (yieldVehicle != null) {
						if (node.equals(yieldVehicle.getDriveCurrNode())) {
							// 2013.10.25 by KYK : 동일노드(parentnode) 후방호기 양보대상 제외
							if (node.equals(this.getDriveCurrNode())) {
								if (currNodeOffset > yieldVehicle.getCurrNodeOffset()) {
									continue;
								}										
							}
							if (yieldVehicle.isYieldRequested() == false) {
								if (yieldVehicle.requestYield(this)) {
									if (yieldRequestedVehicleList.contains(yieldVehicle) == false) {
										yieldRequestedVehicleList.add(yieldVehicle);
										log.append(yieldVehicle.getVehicleId()).append("(").append(node.getNodeId()).append(") ");
									}
								} else {
									if (yieldRequestedVehicleList.contains(yieldVehicle) == false) {
										yieldRequestedVehicleList.add(yieldVehicle);
									}
								}
							}
							break;
						}
					}
				}
			}
		}
		return log.toString();
	}
	
	/**
	 * 
	 * @param isNearyByDrive
	 */
	public void requestRepathSearch(boolean isNearyByDrive) {
		// 2014.02.06 by KYK
//		if (isFailureOHTDetourSearchUsed()) {
//			Node drivedNode = null;
//			for (int i = 0; i < driveNodeList.size(); i++) {
//				drivedNode = driveNodeList.get(i);
//				if (drivedNode != null) {
//					drivedNode.checkRepathSearch(this, isNearyByDrive);
//				}
//			}
//		}
		Node drivedNode = null;
		for (int i = 0; i < driveNodeList.size(); i++) {
			drivedNode = driveNodeList.get(i);
			if (drivedNode != null) {
				drivedNode.checkRepathSearch(this, isNearyByDrive);
			}
		}
	}

	public boolean isAutoPositioning() {
		// 2012.01.19 by PMM
//		if (vehicleMode == 'A' && state == 'P') {
		if (state == 'P' && vehicleMode == 'A') {
			return true;
		}
		return false;
	}

	public VehicleData getPauseRequestVehicle() {
		return pauseRequestVehicle;
	}

	public void setPauseRequestVehicle(VehicleData pauseRequestVehicle) {
		this.pauseRequestVehicle = pauseRequestVehicle;
	}
	
	public boolean isLocatedInDriveNodeList(VehicleData vehicle) {
		Node drivedNode = null;
		// 2015.04.16 by KYK : 동일 노드에 Vehicle 위치시 offset 참조하여 전후 판단
//		for (int i = 1; i < driveNodeList.size(); i++) {
//			drivedNode = driveNodeList.get(i);
//			if (drivedNode != null) {
//				if (drivedNode.equals(vehicle.getDriveCurrNode())) {
//					return true;
//				}
//			}
//		}
		for (int i = 0; i < driveNodeList.size(); i++) {
			drivedNode = driveNodeList.get(i);
			if (drivedNode != null) {
				if (drivedNode.equals(vehicle.getDriveCurrNode())) {
					// i=0 일때 currNodeOffset 비교 (OCS3.1은 offset=0임)
					if (i == 0) {
						if (vehicle.getCurrNodeOffset() > currNodeOffset) {
							return true;
						}
					} else {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean containsDriveFailCausedVehicleList(VehicleData vehicle) {
		if (driveFailCausedVehicleList != null) {
			return driveFailCausedVehicleList.contains(vehicle);
		}
		return false;
	}
	
	public void clearDriveFailCausedVehicleList() {
		if (driveFailCausedVehicleList != null) {
			driveFailCausedVehicleList.clear();
		}
	}
	
	/**
	 * 비근접제어인 경우 driveFailCausedVehicleList에 vehicle만 add한다.
	 * @param vehicle - element to be appended to this list 
	 */
	public void addVehicleToDriveFailCausedVehicleList(VehicleData vehicle) {
		if (driveFailCausedVehicleList != null &&
				driveFailCausedVehicleList.contains(vehicle) == false) {
			driveFailCausedVehicleList.add(vehicle);
		}
	}	
	
	/**
	 * 근접제어인 경우 driveFailCausedVehicleList에 최신 vehicle만 add하고, 기존 vehicle은 삭제한다.</br>
	 * (isNearby가 false인 경우는 비근접제어에 사용하는 함수로 대체)
	 * @author zzang9un
	 * @since	2014. 9. 11.
	 * @param vehicle - element to be appended to this list
	 * @param isNearby - 근접제어 방식 flag(근접제어:true, 비근접제어:false)
	 * @see #addVehicleToDriveFailCausedVehicleList(VehicleData)
	 */
	public void addVehicleToDriveFailCausedVehicleList(VehicleData vehicle, boolean isNearby) {
		if (isNearby) {
			// by zzang9un 2015.09.11 : 자기 자신은 caused에 등록되지 않도록 변경
			if (driveFailCausedVehicleList != null && !this.equals(vehicle)) {
				driveFailCausedVehicleList.clear();
				driveFailCausedVehicleList.add(vehicle);
			}
		}
		else{
			addVehicleToDriveFailCausedVehicleList(vehicle);
		}
	}
	
	public void removeVehicleFromDriveFailCausedVehicleList(VehicleData vehicle) {
		if (driveFailCausedVehicleList != null &&
				driveFailCausedVehicleList.contains(vehicle)) {
			driveFailCausedVehicleList.remove(vehicle);
		}
	}
	
	public ArrayList<VehicleData> getDriveFailCausedVehicleList() {
		return driveFailCausedVehicleList;
	}
	
	public int getDriveFailCausedVehicleListSize() {
		if (driveFailCausedVehicleList != null) {
			return driveFailCausedVehicleList.size();
		}
		return 0;
	}
	
	public VehicleData getDriveFailCausedVehicle(int index) {
		VehicleData driveFailCausedVehicle = null;
		try {
			if (driveFailCausedVehicleList.size() > index) {
				driveFailCausedVehicle = driveFailCausedVehicleList.get(index);
			}
		} catch (Exception e) {
			// 단순 참조용이므로 해당 Exception 무시.
			driveFailCausedVehicle = null;
		}
		return driveFailCausedVehicle;
	}
	
	public Node getDetourNode() {
		return this.detourNode;
	}
	
	public void resetDetourNode(){
		this.detourNode = null;
	}
	
	public boolean isDriveFailed() {
		// 2014.09.22 by zzang9un 근접제어 Deadlock을 위해 조건 추가
		if ((state == 'A' || state == 'N' || state == 'O' || state == 'I') || (isNearByDrive && (state == 'G'))) {
			if (reason != null && reason.length() > 2) {
				if (reason.indexOf("driveLimitTime Over") < 0) {
					if (vehicleMode == 'A') {
						if (enabled) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Check if the forward vehicle is detected
	 * @author zzang9un
	 * @since	2014. 8. 18.
	 * @return	true if forward vehicle is detected, otherwise false
	 */
	public boolean isForwardVehicleDetected()  {
		// 2014.08.18 by zzang9un : 근접제어 vehicle이 Pause Type이 1인 경우 true를 리턴
		if (isNearByDrive && (getPauseType() == 1 && getState() == 'G'))
			return true;
		else
			return false;
	}
	
	public void setDeadlockType(DEADLOCK_TYPE deadlockType) {
		this.deadlockType = deadlockType;
	}
	
	public DEADLOCK_TYPE getDeadlockType() {
		return deadlockType;
	}
	
	// 2012.01.05 by PMM
	public void resetVehicleLocusList() {
		if (vehicleLocusList.size() > 1) {
			Node drivedNode = vehicleLocusList.get(vehicleLocusList.size() - 1);
			vehicleLocusList.clear();
			if (drivedNode != null) {
				vehicleLocusList.add(drivedNode);
			}
		}
	}
	
	// 2012.01.05 by PMM
	public String getVehicleLocus() {
		StringBuffer vehicleLocus = new StringBuffer();
		Node drivedNode;
		int drivedNodeSize = vehicleLocusList.size();
		for (int i = 0; i < drivedNodeSize; i++) {
			drivedNode = vehicleLocusList.get(i);
			if (drivedNode != null) {
				vehicleLocus.append(drivedNode.getNodeId());
				if (i < drivedNodeSize - 1) {
					vehicleLocus.append(" ");
				}
			}
		}
		return vehicleLocus.toString();
	}
	
	// 2012.02.06 by PMM
	public void setLocateRequested(boolean isLocateRequested) {
		this.isLocateRequested = isLocateRequested;
	}
	
	// 2012.02.06 by PMM
	public boolean isLocateRequested() {
		return isLocateRequested;
	}
	
	// 2014.02.21 by MYM : [Stage Locate 기능]
	public void setStageRequested(boolean isStageRequested, long stageWaitTime) {
		this.isStageRequested = isStageRequested;
		this.stageWaitTime = stageWaitTime;
	}
	// 2014.02.21 by MYM : [Stage Locate 기능]
	public boolean isStageRequested() {
		return isStageRequested;
	}
	// 2014.02.21 by MYM : [Stage Locate 기능]
	public long getStageWaitTime() {
		return stageWaitTime;
	}
	/**
	 * 2015.04.03 by MYM : Stage 도착 시간 설정
	 * @param stageArrivedTime
	 */
	public void setStageArrivedTime(long stageArrivedTime) {
		this.stageArrivedTime = stageArrivedTime;
	}
	/**
	 * 2015.04.03 by MYM : Stage 도착 시간 설정
	 * @return
	 */
	public long getStageArrivedTime() {
		return this.stageArrivedTime;
	}
	
	// 2012.03.05 by PMM
	public void setEStopRequested(boolean isEStopRequested) {
		this.isEStopRequested = isEStopRequested;
	}
	
	// 2012.03.05 by PMM
	public boolean isEStopRequested() {
		return isEStopRequested;
	}
	
	// 2012.02.15 by PMM
	public void traceOperationException(String message, Throwable e) {
		operationExceptionTraceLog.error(String.format("%s> [%s] ", vehicleId, message), e);
		operationExceptionTraceLog.error(String.format("%s>      %s", vehicleId, getJournalOfVehicle()));
	}
	
	/**
	 * Trace OperationException
	 * 
	 * @param message
	 */
	public void traceOperationException(String message) {
		operationExceptionTraceLog.error(String.format("%s> %s", vehicleId, message));
		operationExceptionTraceLog.error(String.format("%s>      %s", vehicleId, getJournalOfVehicle()));
	}

	public void traceOperation(String message) {
		operationTraceLog.debug(String.format("%s> %s", vehicleId, message));
	}

	// 2012.02.15 by PMM
	private String getJournalOfVehicle() {
		StringBuffer journal = new StringBuffer();
		journal.append("Vehicle: ");
		journal.append(" Mode:").append(vehicleMode);
		journal.append(", State:").append(state);
		journal.append(", Node(").append(currNode).append(",").append(stopNode).append(",").append(targetNode).append(")");
		journal.append(", Carrier:").append(carrierExist);
		journal.append(", CmdState:(P:").append(prevCmd).append(" C:").append(currCmd).append(" N:").append(nextCmd).append(")");
		journal.append(", Error:").append(errorCode);
		journal.append(", RF:").append(rfData);
		if (localGroupId != null &&
				localGroupId.length() > 0) {
			journal.append(", LocalGroup:").append(localGroupId);
		}
		journal.append(", PauseType:").append(pauseType);
		return journal.toString();
	}

	public boolean isFailureOHTDetourSearchUsed() {
		return isFailureOHTDetourSearchUsed;
	}

	public void setFailureOHTDetourSearchUsed(boolean isFailureOHTDetourSearchUsed) {
		this.isFailureOHTDetourSearchUsed = isFailureOHTDetourSearchUsed;
	}
	
	public void setBlockPreemptionUpdateUsed(boolean isBlockPreemptionUpdateUsed) {
		this.isBlockPreemptionUpdateUsed = isBlockPreemptionUpdateUsed;
	}
	
	public void resetRedirectedNodeSet() {
		redirectedNodeSet.clear();
	}
	
	public void addToRedirectedNodeSet(Node node) {
		if (node != null) {
			redirectedNodeSet.add(node);
		}
	}
	
	public boolean containsRedirectedNodeSet(Node node) {
		if (node != null) {
			return redirectedNodeSet.contains(node);
		}
		return false;
	}
	
	public boolean isRepathSearchNeeded(long dynamicRoutingHoldTimeout) {
		// by PMM.
		// RepathSearch를 위한 조건:
		// 1. 마지막 PathSearch한 후, 최소 시간 (20 sec) 경과. -> 해당 Search 결과를 유지하기 위함.
		// 2. 분기 이전 교차로 노드 통과 확인
		// 3. 분기 노드에서 대기 시, 시간 경과만으로도 RepathSearch 가능.
		// 4. RoutedNodeList를 Backward로 체크하여, 우회 경로 존재 필요 조건을 체크함.
		//		우회 경로 존재 필요 조건: TargetNode로부터 Backward로 체크해서 합류 노드 이전에 분기 노드가 있어야 함.
		if (isRepathSearchNeeded &&
				System.currentTimeMillis() - lastPathSearchedTime > dynamicRoutingHoldTimeout) {
			if (isRepathSearchNeeded || (state != 'G' && getDriveCurrNode() != null && getDriveCurrNode().isDiverge())) {
				Node node = null;
				boolean isConvergeNodePassed = false;
				synchronized (routedNodeList) {
					for (int i = routedNodeList.size() - 1; i >= 0; i--) {
						node = routedNodeList.get(i);
						if (node != null) {
							if (isConvergeNodePassed == false) {
								if (node.isConverge()) {
									isConvergeNodePassed = true;
								}
							} else {
								if (node.isDiverge()) {
									return true;
								}
							}
						}
					}
				}
				
				// 남은 RoutedNodeList에서 우회 경로가 없으면, RepathSearch가 불필요함.
				setRepathSearchNeeded(false);
			}
		}
		return false;
	}
	
	public void setRepathSearchNeeded(boolean isRepathSearchNeeded) {
		this.isRepathSearchNeeded = isRepathSearchNeeded;
	}
	
	public boolean isAssignedVehicle() {
		return isAssignedVehicle;
	}

	public void setAssignedVehicle(boolean isAssignedVehicle) {
		this.isAssignedVehicle = isAssignedVehicle;
	}

	public String getDriveNodeInfo() {
		return driveNodeList.toString();
	}
		
	/**
	 * 2013.04.05 by KYK
	 * @param offset
	 * @return
	 */
	public double getMoveTimeToStopNode(Node dQNode) {
		double arrivedTime = 0.0;
		double offsetTime = 0.0;
		double speed = 0.0;
		int realNodeCount = 0;

		if (dQNode == null) {
			return 0;
		}
		Node currNode = getDriveCurrNode();
		if (currNode == null) {
			return 9999;
		}
		Node prevNode = currNode;
		Node node = null;
		int startIndex = getDriveNodeIndex(prevNode);
		// DQNode 까지 Drive 한 경우
		if (containsDriveNode(dQNode)) {
			int endIndex = getDriveNodeIndex(dQNode);			
			for (int i = startIndex + 1; i <= endIndex; i++) {
				node = driveNodeList.get(i);
				if (node != null) { // 2014.02.24 by KYK : null check
					arrivedTime += node.getMoveInTime(prevNode);
					prevNode = node;
				}
			}			
		} else if (containsRoutedNode(dQNode)) {
			if (getDriveNodeCount() > 1) {
				for (int i = startIndex + 1; i < getDriveNodeCount(); i++) {
					node = driveNodeList.get(i);
					if (node != null) { // 2014.02.24 by KYK : null check
						arrivedTime += node.getMoveInTime(prevNode);
						prevNode = node;
					}
				}
			}
			int endIndex = getRoutedNodeIndex(dQNode);
			for (int i = 0; i <= endIndex; i++) {
				node = routedNodeList.get(i);
				if (node != null) { // 2014.02.24 by KYK : null check
					arrivedTime += node.getMoveInTime(prevNode);
					prevNode = node;
					// check realNodeCount
					// 2015.02.05 by KYK : block 조건은 없어야 할듯...
//					if (node.isVirtual() == false && node.getBlock(null) == null) {
					if (node.isVirtual() == false) {
						Block block = node.getBlock(null);
						if (block == null || node.equals(block.getMainBlockNode())) {
							realNodeCount++;
						}
					}
				}
			}
		}
		// exceptional case
		if (realNodeCount == 0 && driveNodeList.size() <= 1) {
			arrivedTime = 0;
		}
		
		if (currNodeOffset > 0) {
			Node nextNode = null;
			if (startIndex + 1 < getDriveNodeCount()) {
				nextNode = getDriveNode(startIndex + 1);
			} else if (getRoutedNodeCount() > 0){
				nextNode = getRoutedNode(0);
			}
			if (nextNode != null) {
				int index = 0;
				for (int i = 0; i < currNode.getSectionCount(); i++) {
					Section section = currNode.getSection(i);
					index = section.getNodeIndex(currNode);
					if (nextNode.equals(section.getNode(index + 1))) {
						speed = section.getSectionSpeed();
						offsetTime = currNodeOffset / speed;
						break;
					}
				}				
			}
		}
		
		arrivedTime = arrivedTime - offsetTime;
		if (arrivedTime < 0) {
			arrivedTime = 0.0;
		}
		return arrivedTime;
	}
	
	/**
	 * 2013.02.08 by KYK
	 * @param offset
	 * @return
	 */
	public double getMoveTimeToStopNode(double offset) {
		double arrivedTime = 0.0;
		double offsetTime = 0.0;
		double speed = 0.0;
		
		if (driveNodeList.size() > 1) {
			int index = driveNodeList.indexOf(getDriveCurrNode());
			if (index >= 0 && index < driveNodeList.size()-1) {
				Node prevNode = driveNodeList.get(index);
				Node node = driveNodeList.get(index + 1);
				for (int i = 0; i < prevNode.getSectionCount(); i++) {
					Section section = prevNode.getSection(i);
					int sIndex = section.getNodeIndex(prevNode);
					if (node.equals(section.getNode(sIndex + 1))) {
						speed = section.getSectionSpeed();
						offsetTime = offset / speed;
						break;
					}
				}
				for (int i = index + 1; i < driveNodeList.size(); i++) {
					node = driveNodeList.get(i);
					arrivedTime += node.getMoveInTime(prevNode);
					prevNode = node;
				}
			}
		}
		arrivedTime = arrivedTime - offsetTime;
		if (arrivedTime < 0) {
			arrivedTime = 0.0;
		}
		return arrivedTime;
	}
	
	public synchronized List<Node> getDriveNodeList() {
		return driveNodeList;
	}
	
	/**
	 * 2013.05.10 by KYK
	 * @return
	 */
	public synchronized List<Node> getRoutedNodeList() {
		return routedNodeList;
	}

	/**
	 * 2013.04.12 by KYK
	 * @return
	 */
	public boolean isAbnormalInBlock() {
		
		if (vehicleMode == 'M') {
			return true;
		}
		if (errorCode == OcsConstant.COMMUNICATION_FAIL) {
			return true;
		}
		if (alarmCode == OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER
				|| alarmCode == OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER
				|| alarmCode == OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
			return true;
		}
		return false;
	}

	/**
	 * 2013.05.10 by KYK (2014.02.14 Revised)
	 * @param vehicle
	 * @param dQNode
	 * @return
	 */
	public VehicleData getDQFirstVehicle(Node dQNode) {
		if (dQNode == null) {
			return null;
		}
		VehicleData firstVehicle = null;
		VehicleData tempVehicle = null;
		Node node = null;		
		List<Node> nodeList = null;
		// search from currNode to dQNode (driveNodeList or routedNodeList)
		nodeList = new ArrayList<Node>(driveNodeList);
		if (nodeList.contains(dQNode) == false) {
			int index = routedNodeList.indexOf(dQNode);
			for (int i = 0; i <= index; i++) {
				nodeList.add(routedNodeList.get(i));
			}
		}
		for (int i = nodeList.size() - 1; i >= 0; i--) {
			node = nodeList.get(i);
			for (int j = 0; j < node.getArrivedVehicleCount(); j++) {
				tempVehicle = node.getArrivedVehicle(j);
				// 2014.02.14 by KYK
				if (tempVehicle != null) {
					if (firstVehicle == null) {
						firstVehicle = tempVehicle;
					} else {
						if (firstVehicle.getCurrNodeOffset() < tempVehicle.getCurrNodeOffset()) {
							firstVehicle = tempVehicle;
						}
					}					
				}
			}
			if (firstVehicle != null) {
				break;
			}
		}
		return firstVehicle;		
	}
	
	/**
	 * 2014.07.15 by KYK
	 */
	public void initializeDriveVehicleInNode() {
		for (Node node : driveNodeList) {
			node.vehicleInitialize(this, isNearByDrive);
		}
	}
	
	/**
	 * @author zzang9un
	 * @since	2014. 8. 18.
	 * @return	detectedCausedVehicleList
	 */
	public ArrayList<VehicleData> getDetectedCausedVehicleList() {
		return detectedCausedVehicleList;
	}
	
	/**
	 * Add vehicle to detectedCausedVehicleList
	 * @author zzang9un
	 * @since	2014. 8. 18.
	 * @param 	vehicle
	 */
	public void addDetectedCausedVehicleList(VehicleData vehicle) {
		if (detectedCausedVehicleList != null &&
				detectedCausedVehicleList.contains(vehicle) == false) {
			detectedCausedVehicleList.add(vehicle);
		}
	}
	
	/**
	 * Clear detectedCausedVehicleList
	 * @author zzang9un
	 * @since	2014. 8. 18.
	 */
	public void clearDetectedCausedVehicleList() {
		detectedCausedVehicleList.clear();
	}
	
	/**
	 * 2014.10.13 by KYK
	 */
	public void cancelReservationForVehicleDriveIn() {
		if (routedNodeList != null && routedNodeList.size() > 0) {
			for (Node node : routedNodeList) {
				if (node != null) {
					if (node.cancelReservationForVehicleDriveIn(this) == false) {
						break;
					}
				}
			}
		}
	}
	
	/**
	 * 2014.10.13 by MYM : 장애 지역 우회 기능
	 */
	public void setAbnormalSection(DETOUR_REASON reason) {
		if (this.abnormalReason == reason) {
			return;
		}
		this.abnormalReason = reason;
		// 2015.11.10 by MYM : 장애 발생시 ErrorCode 저장 (release할 때 기능 유무 체크시 참조 필요)
		this.detourErrorCode = this.errorCode;
		
		HashSet<Section> detourSectionSet = new HashSet<Section>();
		// 장애 발생시 CurrNode ~ StopNode 및 충돌 간섭 Node의 Section 차단 처리
		for (Node node : driveNodeList) {
			detourSectionSet.addAll(node.getSection());
			
			if (isNearByDrive == false || node.isCheckCollision()) {
				Iterator<Collision> iterator = node.getCollisions().iterator();
				while (iterator.hasNext()) {
					Node collisionNode = iterator.next().getCollisionNode(node);
					detourSectionSet.addAll(collisionNode.getSection());
				}
			} else if (isNearByDrive) {
				// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
				Iterator<Block> iterator = node.getCollisionBlockSet().iterator();
				while (iterator.hasNext()) {
					for (Node blockNode : iterator.next().getNodeList()) {
						detourSectionSet.addAll(blockNode.getSection());
					}
				}
			}
		}
		
		// 2015.11.10 by MYM : 진입 가능한 Section을 모두 검색하여 Disable 설정으로 변경 (Recursive Section Disable 미사용)
		for (Section section : detourSectionSet) {
			abnormalSectionSet.addAll(section.getDetourSectionSet());
		}
		
		Iterator<Section> iterator = abnormalSectionSet.iterator();
		HashSet<Section> removeSection = new HashSet<Section>(); 
		while (iterator.hasNext()) {
			// 2016.04.12 by MYM : Disable 처리 실패한 Section은 차단 대상 Set(abnormalSectionSet)에서 제외
//			iterator.next().addAbnormalItem(this, reason);
			Section next = iterator.next();
			if (!next.addAbnormalItem(this, reason)) {
				removeSection.add(next);
			}
		}
		for (Section section : removeSection) {
			abnormalSectionSet.remove(section);
		}
	}
	
	/**
	 * 2015.11.10 by MYM : 장애 발생시 ErrorCode 저장 (release할 때 기능 유무 체크시 참조 필요)
	 */
	public int getDetourErrorCode() {
		return detourErrorCode;
	}	
	
	/**
	 * 2014.10.13 by MYM : 장애 지역 우회 기능
	 */
	public void releaseAbnormalSection() {
		// 경로 탐색 허용 처리, OutOfService → InService 처리
		Iterator<Section> iterator = abnormalSectionSet.iterator();
		while (iterator.hasNext()) {
			iterator.next().removeAbnormalItem(this);
		}
		abnormalSectionSet.clear();
		abnormalReason = DETOUR_REASON.NONE;
		detourErrorCode = 0;
	}
	
	/**
	 * 2014.10.13 by MYM : 장애 지역 우회 기능
	 * @param reason
	 */
	public void releaseAbnormalSection(DETOUR_REASON reason) {
		if (this.abnormalReason == reason) {
			releaseAbnormalSection();
		}
	}
	
	/**
	 * 2015.02.11 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public DETOUR_REASON getAbnormalReason() {
		return this.abnormalReason;
	}
	
	/**
	 * 2015.02.04 by MYM : 장애 지역 우회 기능
	 * @param node
	 */
	public void updateAbnormalSectionForManualMoving(Node currNode, boolean isInTheDriveNodeList) {
		// Vehicle 위치가 바뀔 때 점유 해제된 Node 및 충돌 간섭 Node 구간 해제 처리
		if (currNode != null) {
			HashSet<Section> baseSectionSet = new HashSet<Section>();
			// Step1. driveNodeList 기준으로 Section 수집
			if (isInTheDriveNodeList) {
				for (Node node : driveNodeList) {
					baseSectionSet.addAll(node.getSection());
					if (isNearByDrive == false || node.isCheckCollision()) {
						Iterator<Collision> iter = node.getCollisions().iterator();
						while (iter.hasNext()) {
							Node collisionNode = iter.next().getCollisionNode(node);
							baseSectionSet.addAll(collisionNode.getSection());
						}
					} else if (isNearByDrive) {
						// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
						Iterator<Block> iterator = node.getCollisionBlockSet().iterator();
						while (iterator.hasNext()) {
							for (Node blockNode : iterator.next().getNodeList()) {
								baseSectionSet.addAll(blockNode.getSection());
							}
						}
					}
				}
			} else {
				baseSectionSet.addAll(currNode.getSection());
				if (isNearByDrive == false || currNode.isCheckCollision()) {
					Iterator<Collision> iter = currNode.getCollisions().iterator();
					while (iter.hasNext()) {
						Node collisionNode = iter.next().getCollisionNode(currNode);
						baseSectionSet.addAll(collisionNode.getSection());
					}
				} else if (isNearByDrive) {
					// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
					Iterator<Block> iterator = currNode.getCollisionBlockSet().iterator();
					while (iterator.hasNext()) {
						for (Node blockNode : iterator.next().getNodeList()) {
							baseSectionSet.addAll(blockNode.getSection());
						}
					}
				}
			}
			
			HashSet<Section> detourAllSectionSet = new HashSet<Section>();
			for (Section section : baseSectionSet) {
				detourAllSectionSet.addAll(section.getDetourSectionSet());
			}
			
			HashSet<Section> tmpSectionSet = (HashSet<Section>) abnormalSectionSet.clone(); 
			// Step2. driveNodeList 기준에 포함되지 않은 Section은 차단 해제
			for (Section section : tmpSectionSet) {
				if (detourAllSectionSet.contains(section) == false) {
					if (abnormalSectionSet.remove(section)) {
						section.removeAbnormalItem(this);
					}
				} else {
					detourAllSectionSet.remove(section);
				}
			}
			
			// Step3. 신규로 추가된 Section은 차단
			for (Section section : detourAllSectionSet) {
				if (section.addAbnormalItem(this, abnormalReason)) {
					abnormalSectionSet.add(section);
				}
			}
		}
	}
	
	/**
	 * 2015.02.06 by MYM : 장애 지역 우회 기능
	 */
	public void clearAbnormalSectionSet(boolean resetOnly) {
		// InService → OutOfService가 될 때 Clear 처리
		if (resetOnly == false) {
			for (Section section : abnormalSectionSet) {
				section.clearAbnormalItem(this);
			}
		}
		abnormalSectionSet.clear();
		abnormalReason = DETOUR_REASON.NONE;
		detourErrorCode = 0;
	}
	
	/**
	 * 2015.02.10 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public boolean isInAbnormalSection() {
		if (abnormalSectionSet.size() > 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * 2015.01.20 by MYM : 장애 지역 우회 기능
	 * @return
	 */
	public String getSearchFailReason() {
		return searchFailReason;
	}

	/**
	 * 2015.01.20 by MYM : 장애 지역 우회 기능
	 * @param searchFailReason
	 */
	public void setSearchFailReason(String searchFailReason) {
		this.searchFailReason = searchFailReason;
	}
	
	// 2015.06.08
	public boolean isDetourYieldRequested() {
		return isDetourYieldRequested;
	}
	
	public void setDetourYieldRequested(boolean isDetourYieldRequested) {
		this.isDetourYieldRequested = isDetourYieldRequested;
	}

	public boolean isRepathSearchNeededByPatrolVHL() {
		return isRepathSearchNeededByPatrolVHL;
	}

	public void setRepathSearchNeededByPatrolVHL(boolean isRepathSearchNeededByPatrolVHL) {
		this.isRepathSearchNeededByPatrolVHL = isRepathSearchNeededByPatrolVHL;
	}
	
	// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
	public boolean isExistPortofPark(){
		return isExistPortofPark;
	}
	
	// 2020.06.22 by YSJ : 이적재 위치의 ParkNode 구분
	public void setisExistPortofPark(boolean isExistPortofPark){
		this.isExistPortofPark = isExistPortofPark;
	}
	
	// 2022.08.17 by Y.Won : HID capa full 인 경우 idle vehicle escape 동작 시 path search fail 표시
	public boolean isHIDEscapePathSearchFailed() {
		return isHIDEscapePathSearchFailed;
	}
	// 2022.08.17 by Y.Won : HID capa full 인 경우 idle vehicle escape 동작 시 path search fail 표시
	public void setHIDEscapePathSearchFailed(boolean isEscapePathSearchFailed) {
		this.isHIDEscapePathSearchFailed = isEscapePathSearchFailed;
	}
}
