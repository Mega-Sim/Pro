package com.samsung.ocs.operation.mode;

import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.OcsConstant.CARRIERLOC_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.LOCALGROUP_CLEAROPTION;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.StationManager;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.VehicleData;
import com.samsung.ocs.operation.Operation;
import com.samsung.ocs.operation.comm.VehicleComm;
import com.samsung.ocs.operation.comm.VehicleCommV7;
import com.samsung.ocs.operation.constant.MessageItem.EVENT_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant;
import com.samsung.ocs.operation.constant.OperationConstant.COMMAND_STATE;
import com.samsung.ocs.operation.constant.OperationConstant.COMMAND_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant.JOB_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant.OPERATION_MODE;
import com.samsung.ocs.operation.model.VehicleCommCommand;

/**
 * OperationModeImpl Class, OCS 3.0 for Unified FAB
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

public class OperationModeImpl implements OperationMode {
	protected Operation operation;
	protected TrCmd trCmd;
	protected VehicleData vehicleData;
	protected OPERATION_MODE previousOperationMode;
	protected OCSInfoManager ocsInfoManager = null;
	
	// 2012.03.06 by PMM
	protected NodeManager nodeManager = null;
	// 2013.02.22 by KYK
	protected StationManager stationManager = null;
	protected CarrierLocManager carrierLocManager = null;
	
	//2011.11.15 by PMM
	protected boolean isNearByDrive = true;
//	protected boolean isSTBCUsed = true;
	
	// 2012.01.03 by PMM
	// PortDup 관련 Event 중복 등록 및 로그 방지
	protected boolean isPortDuplicateEventRegistered = false;
	protected boolean isPortDuplicateClearEventRegistered = false;
	
	// 2013.09.06 by KYK
	private static final String FOUP = "Foup";
	private static final String RETICLE = "Reticle";
	private static final String MAC = "Mac";
	
	/**
	 * Constructor of OperationModeImpl class.
	 */
	public OperationModeImpl(Operation operation) {
		this.operation = operation;
		this.vehicleData = operation.getVehicleData();
		this.ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		
		// 2012.03.06 by PMM
		this.nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		// 2013.02.22 by KYK
		this.stationManager = StationManager.getInstance(null, null, false, false, 0);
		this.carrierLocManager = CarrierLocManager.getInstance(null, null, false, false, 0);
		
		this.isNearByDrive = operation.isNearByDrive();
//		this.isSTBCUsed = operation.isSTBCUsed();
	}
	
	public OPERATION_MODE getOperationMode() {
		return OPERATION_MODE.IDLE;
	}
	
	public void setPreviousOperationMode(OPERATION_MODE mode) {
		previousOperationMode = mode;
	}
	
	public boolean controlVehicle() {
		return false;
	}
	
	/**
	 * Check Sleep Condition
	 * 
	 * @return
	 */
	protected boolean checkSleepMode() {
		if (vehicleData.getVehicleMode() == 'M') {
			// OperationMode(x->S) by Manual.
			changeOperationMode(OPERATION_MODE.SLEEP, "VehicleMode: Manual");
			return true;
		} else if (vehicleData.getState() == 'E' || vehicleData.getState() == 'V') {
			// OperationMode(x->S) by Vehicle Error (E, V)
			changeOperationMode(OPERATION_MODE.SLEEP, "VehicleState: " + vehicleData.getState());
			return true;
		}
		return false;
	}
	
	protected void setAlarmReport(int alarmId) {
		this.operation.setAlarmReport(alarmId);
	}
	
	protected void clearAlarmReport(int alarmId) {
		this.operation.clearAlarmReport(alarmId);
	}
	
	// 2012.03.08 by KYK  
	protected boolean isSTBPortAvailable(String carrierLoc) {
		return this.operation.isSTBPortAvailable(carrierLoc);
	}
	
	protected boolean isSTBOrUTBPort(CARRIERLOC_TYPE carrierLocType) {
		return this.operation.isSTBOrUTBPort(carrierLocType);
	}
	protected boolean updateCarrierStateInSTB(String state, String carrierLoc, String carrierId, String rfData, String foupId) {
		return this.operation.updateCarrierStateInSTB(state, carrierLoc, carrierId, rfData, foupId);
	}
	
	protected void updateCarrierStateInSTBWithoutSTBC(String state, String carrierLoc, String carrierId, String rfData, String foupId) {
		this.operation.updateCarrierStateInSTBWithoutSTBC(state, carrierLoc, carrierId, rfData, foupId);
	}
	
	// 2011.10.26 by PMM
	// Operation으로 옮김.
	protected boolean isLoadingByPass() {
		return this.operation.isLoadingByPass();
	}
	
	protected boolean isSTBCUsed() {
		return this.operation.isSTBCUsed();
	}
	
	protected boolean isUnloadErrorReportUsed() {
		return this.operation.isUnloadErrorReportUsed();
	}
		
	protected boolean isAutoMismatchRecoveryMode() {
		return this.operation.isAutoMismatchRecoveryMode();
	}
	
	protected void registerUnknownTrCmd() {
		this.operation.registerUnknownTrCmd();
	}
	
	protected boolean isGoModeCarrierStatusCheckUsed() {		
		return this.operation.isGoModeCarrierStatusCheckUsed();
	}
	
	protected boolean checkMissedCarrierOnUnloadPort() {		
		return this.operation.checkMissedCarrierOnUnloadPort();
	}
	
	protected boolean checkSTBUTBCarrierMismatchOnUnloadPort() {		
		return this.operation.checkSTBUTBCarrierMismatchOnUnloadPort();
	}
	
	protected boolean isRfReadPort(CARRIERLOC_TYPE carrierLocType) {
		// OCSINFO에서 설정된 값으로 RF Read를 진행할 Port Type이 지정된다. ex.)STBPORT/STKPORT/UTBPORT
		if (ocsInfoManager.getRfReadDevice().indexOf(carrierLocType.toConstString()) >= 0) {
			return true;
		} else {
			return false;
		}
	}
	
	// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정 [주석처리]
//	protected boolean isAVUnloadPort(CARRIERLOC_TYPE carrierLocType) {
//		if (ocsInfoManager.getAVUnloadPortType().indexOf(carrierLocType.toConstString()) >= 0) {
//			return true;
//		} else {
//			return false;
//		}
//	}
//	
//	protected boolean isAVLoadPort(CARRIERLOC_TYPE carrierLocType) {
//		if (ocsInfoManager.getAVLoadPortType().indexOf(carrierLocType.toConstString()) >= 0) {
//			return true;
//		} else {
//			return false;
//		}
//	}
	
	protected CARRIERLOC_TYPE getCarrierLocType(String carrierLocId) {
		return this.operation.getCarrierLocType(carrierLocId);
	}
	
	/**
	 * Is DriveVehiclePath Available?
	 * 
	 * @return
	 */
	protected boolean availableDriveVehiclePath() {
		switch (getCommandState()) {
			case READY:
			case EXECUTED:
				if (vehicleData.getCurrCmd() == 0) {
					return true;
				}
				break;
			case EXECUTING:
				if (vehicleData.getCurrCmd() > 0) {
					// 2015.04.23 by zzang9un : PassDoor 테스트 후 문제점 수정(Out 명령을 AA된 후 줘야 함)
					// Stop이 PassDoor Node이고 CurrNode와 StopNode가 같은데 AA가 아닌 경우 false 리턴
					if (ocsInfoManager.isPassDoorControlUsage() && vehicleData.getDriveStopNode().getPassDoor() != null) {
						return false;
					}					
					return true;
				} else if (vehicleData.getCurrCmd() == 0 &&
						getVehicleCommCommand().getCommandId() > 0) {
					return true;
				}
				break;
			default:
				break;
		}
		return false;
	}
	
	protected void makeVehicleCommandId() {
		getVehicleCommCommand().makeCommandId(vehicleData.getPrevCmd(), vehicleData.getCurrCmd(), vehicleData.getNextCmd());
	}
	
	protected boolean isCarrierMismatchedOnSTKPort(CARRIERLOC_TYPE carrierLocType, String carrierId, String rfData) {
		if (isRfReadPort(carrierLocType))	{
			if (carrierLocType == CARRIERLOC_TYPE.STOCKERPORT) {
				if ((rfData != null && rfData.length() == 0) || "(null)".equals(rfData)) {
					return false;
				} else if (carrierId.equals(rfData) == false) {
					return true;
				}
			}
		}
		return false;
	}
	
	protected boolean isWorkNode(String node) {
		assert (trCmd != null);
		
		// 2012.01.19 by PMM
//		if (node == null || (node != null && node.length() == 0)) {
		if (trCmd == null || node == null || node.length() == 0) {
			return false;
		}
		// 2012.11.28 by MYM : TrCmd의 DetailState에 따라서 SourceNode or DestNode 비교 조건 추가 
//		if (node.equals(trCmd.getSourceNode()) || node.equals(trCmd.getDestNode())) {
//			return true;
//		}
		if (node.equals(trCmd.getSourceNode())
				// STAGE에 대해 VehicleArrived 보고 안되는 현상.
//				&& trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED) {
				&& (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED ||
						trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_ASSIGNED ||
						trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_WAITING ||
						trCmd.getDetailState() == TRCMD_DETAILSTATE.SCAN_ASSIGNED ||
						trCmd.getDetailState() == TRCMD_DETAILSTATE.PATROL_ASSIGNED ||
						trCmd.getDetailState() == TRCMD_DETAILSTATE.MAPMAKE_ASSIGNED)) {
			return true;
		} else if (node.equals(trCmd.getDestNode())
				&& (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADED
						|| trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ASSIGNED)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 2013.02.22 by KYK
	 * @return
	 */
	protected boolean isWorkStation() {
		assert (trCmd != null);
		
		if (trCmd == null) {
			return false;
		}
		String sourceStation = getStationIdAtPort(trCmd.getSourceLoc());
		if (sourceStation == null) {
			sourceStation = "";
		}
		String destStation = getStationIdAtPort(trCmd.getDestLoc());
		if (destStation == null) {
			destStation = "";
		}
		String targetNode = vehicleData.getTargetNode();
		String targetStation = vehicleData.getTargetStation();
		if (targetStation == null) {
			targetStation = "";
		}
		if (targetNode.equals(trCmd.getSourceNode()) && targetStation.equals(sourceStation)
				&& (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED
				// 2013.07.16 by KYK			
				||	trCmd.getDetailState() == TRCMD_DETAILSTATE.SCAN_ASSIGNED
				||	trCmd.getDetailState() == TRCMD_DETAILSTATE.PATROL_ASSIGNED
				||	trCmd.getDetailState() == TRCMD_DETAILSTATE.MAPMAKE_ASSIGNED
				
				||	trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_ASSIGNED
				||  trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_WAITING)) {
			return true;
		} else if (targetNode.equals(trCmd.getDestNode()) && targetStation.equals(destStation)
				&& (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADED
				|| trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ASSIGNED
				|| trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_WAITING	// 2022.05.05 dahye : PREMOVE
				|| trCmd.getDetailState() == TRCMD_DETAILSTATE.PATROLLING)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Has Vehicle Arrived at TargetNode?
	 * 
	 * @return
	 */
	protected boolean hasArrivedAtTargetNode() {
		if (vehicleData.getStopNode().equals(vehicleData.getTargetNode())) {
			if (vehicleData.getCurrNode().equals(vehicleData.getStopNode())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Has Vehicle Arrived at StopNode?
	 * 
	 * @return
	 */
	protected boolean hasArrivedAtStopNode() {
		if (vehicleData.getCurrNode().equals(vehicleData.getStopNode())) {
			return true;
		}
		return false;
	}
	
	/**
	 * Has Vehicle Arrived at SourceNode?
	 * 
	 * @return
	 */
	protected boolean hasArrivedAtSourceNode() {
		if (trCmd != null) {
			if (vehicleData.getCurrNode().equals(trCmd.getSourceNode())) {
				if (hasArrivedAtTargetNode()) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Has Vehicle Arrived at DestNode?
	 * 
	 * @return
	 */
	protected boolean hasArrivedAtDestNode() {
		if (trCmd != null) {
			if (vehicleData.getCurrNode().equals(trCmd.getDestNode())) {
				if (hasArrivedAtTargetNode()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 2013.02.01 by KYK
	 * Has Vehicle Arrived at Source?
	 * 
	 * @return
	 */
	protected boolean hasArrivedAtSource() {
		if (trCmd != null) {
			if (vehicleData.getCurrNode().equals(trCmd.getSourceNode())) {
				// 2013.08.12 by KYK
//				if (hasArrivedAtTarget()) {
				String stationId = getStationIdAtPort(trCmd.getSourceLoc());
				if (hasArrivedAtStation(stationId)) {
					// 2013.08.23 by KYK
					if (hasArrivedAtTarget()) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * 2013.02.01 by KYK
	 * Has Vehicle Arrived at Dest?
	 * 
	 * @return
	 */
	protected boolean hasArrivedAtDest() {
		if (trCmd != null) {
			if (vehicleData.getCurrNode().equals(trCmd.getDestNode())) {
				// 2013.08.12 by KYK
//				if (hasArrivedAtTarget()) {
				String stationId = getStationIdAtPort(trCmd.getDestLoc());
				if (hasArrivedAtStation(stationId)) {
					// 2013.08.23 by KYK
					if (hasArrivedAtTarget()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 2013.08.12 by KYK
	 * @param stationId
	 * @return
	 */
	protected boolean hasArrivedAtStation(String stationId) {
		if (stationId == null) {
			stationId = "";
		}
		String currStationId = vehicleData.getCurrStation();
		if (currStationId == null) {
			currStationId = "";
		}
		
		if (stationId.equals(currStationId)) {
			return true;
		}		
		if (stationId.length() == 0) {
			Station currStation = stationManager.getStation(currStationId);
			if (currStation != null && currStation.getOffset() == 0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 2013.02.01 by KYK
	 * Has Vehicle Arrived at Target (node or port) ?
	 * @return
	 */
	protected boolean hasArrivedAtTarget() {
		// 주행노드로 이동명령보냈는데 이적재태크 읽혀서 올라올 경우?		
		if (vehicleData.getStopNode().equals(vehicleData.getTargetNode())) {
			if (vehicleData.getCurrNode().equals(vehicleData.getStopNode())) {
				String currStationId = vehicleData.getCurrStation();
				String stopStationId = vehicleData.getStopStation();
				String targetStationId = vehicleData.getTargetStation();

				if (currStationId == null) {
					currStationId = "";
				}
				if (stopStationId == null) {
					stopStationId = "";
				}
				if (targetStationId == null) {
					targetStationId = "";
				}
				
				if (stopStationId.equals(targetStationId) && currStationId.equals(stopStationId)) {
					return true;
				}
				// 보완코드 : offset=0 인 station 위치해 있는데 해당 노드로 이동시
				if (targetStationId.length() == 0) {
					if (stopStationId.length() > 0) {
						Station stopStation = stationManager.getStation(stopStationId);
						if (stopStation == null || stopStation.getOffset() > 0) {
							return false;
						}
					}
					if (currStationId.length() > 0) {
						Station currStation = stationManager.getStation(currStationId);
						if (currStation == null || currStation.getOffset() > 0) {
							return false;
						}
					}
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 2013.02.01 by KYK
	 * Has Vehicle Driven to Target (Node or Specific Port) ?
	 * @return
	 */
	protected boolean hasDrivenToTarget() {
		if (vehicleData.getStopNode().equals(vehicleData.getTargetNode())) {
			String targetStationId = vehicleData.getTargetStation();
			String stopStationId = vehicleData.getStopStation();

			if (targetStationId == null) {
				targetStationId = "";
			}
			if (stopStationId == null) {
				stopStationId = "";
			}
			if (targetStationId.equals(stopStationId)) {
				return true;
			}
			// 보완코드 : offset=0 인 station 위치해 있는데 해당 노드로 이동시
			if (targetStationId.length() == 0) {
				Station stopStation = stationManager.getStation(stopStationId);
				if (stopStation != null) {
					if (stopStation.getOffset() == 0) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * 2013.02.01 by KYK
	 * @return
	 */
	protected boolean hasDrivenToButNotYetArrivedAtTarget() {
		return hasDrivenToTarget() && (hasArrivedAtTarget() == false);
	}
	
	/**
	 * 2013.02.01 by KYK
	 * @return
	 */
	protected boolean hasNotYetDrivenToTarget() {
		return (hasDrivenToTarget() == false);
	}

	/**
	 * 2013.02.01 by KYK
	 * @param carrierLocId
	 * @return
	 */
	protected boolean hasArrivedAtTargetPort(String carrierLocId) {
		String currStation = this.vehicleData.getCurrStation();
		if (currStation == null) {
			currStation = "";
		}
		String targetStation = getStationIdAtPort(carrierLocId);
		if (targetStation == null) {
			targetStation = "";
		}

		if (targetStation.equals(currStation)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 2013.02.15 by KYK
	 * @param carrierLocId
	 * @return
	 */
	protected String getStationIdAtPort(String carrierLocId) {
		return this.operation.getStationIdAtPort(carrierLocId);
	}
	
	/**
	 * 2013.02.15 by KYK
	 * @param newStopNode
	 * @return
	 */
	protected boolean isGoCommandSendable(String newStopNode) {
		if (newStopNode != null && newStopNode.length() > 0) {
			if (newStopNode.equals(vehicleData.getStopNode())) {
			// 동일노드 다른 포인트로 이동명령 전송시
				if (newStopNode.equals(vehicleData.getTargetNode())) {
					String targetStation = vehicleData.getTargetStation();
					if (targetStation != null && targetStation.length() > 0) {
						if (targetStation.equals(vehicleData.getStopStation()) == false) {
							if (vehicleData.isStationDriveAllowed()) {
								return true;
							}
						}
					}
				}
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * 2013.02.22 by KYK
	 * @return
	 */
	protected boolean isLastGoCommandToTargetByTransfer() {
		if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
			if (vehicleData.getTargetNode().equals(vehicleData.getStopNode())) {
				String targetStation = vehicleData.getTargetStation();
				String stopStation = vehicleData.getStopStation();
				if (targetStation == null) {
					targetStation = "";
				}
				if (stopStation == null) {
					stopStation = "";
				}
				if (targetStation.equals(stopStation)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 2013.02.22 by KYK
	 * @return
	 */
	protected boolean isLastGoCommandToTarget() {
		if (vehicleData.getTargetNode().equals(vehicleData.getStopNode())) {
			String targetStation = vehicleData.getTargetStation();
			String stopStation = vehicleData.getStopStation();
			if (targetStation == null) {
				targetStation = "";
			}
			if (stopStation == null) {
				stopStation = "";
			}
			if (targetStation.equals(stopStation)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 2013.09.06 by KYK
	 * @param materialType
	 * @return
	 */
	protected int getCarrierType(String materialType) {
		return operation.getCarrierType(materialType);
	}
	
	// 2013.02.01 by KYK
//	/**
//	 * Has Vehicle Driven to TargetNode?
//	 * 
//	 * @return
//	 */
//	protected boolean hasDrivenToTargetNode() {
//		if (vehicleData.getStopNode().equals(vehicleData.getTargetNode())) {
//			return true;
//		} else {
//			return false;
//		}
//	}
//	
//	/**
//	 * Has Vehicle Driven to But Not Yet Arrived At TargetNode?
//	 * 
//	 * @return
//	 */
//	protected boolean hasDrivenToButNotYetArrivedAtTargetNode() {
//		return hasDrivenToTargetNode() && (hasArrivedAtTargetNode() == false);
//	}
//	
//	/**
//	 * Has Vehicle
//	 * 
//	 * @return
//	 */
//	protected boolean hasNotYetDrivenToTargetNode() {
//		return (hasDrivenToTargetNode() == false);
//	}

	// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정 [주석처리]
//	/**
//	 * Error Recovery Option값을 얻는다.
//	 * 
//	 * @param carrierLocType CARRIERLOC_TYPE
//	 * @param jobType JOB_TYPE
//	 * @return char
//	 */
//	protected char getErrorReportOption(CARRIERLOC_TYPE carrierLocType, JOB_TYPE jobType)
//	{
//		// Unload/Load 시 AV에 대한 PortType구분
//		if (jobType == JOB_TYPE.UNLOAD) {
//			if (carrierLocType != CARRIERLOC_TYPE.NULL && isAVUnloadPort(carrierLocType)) {
//				return 'F';
//			}
//		} else {
//			if (carrierLocType != CARRIERLOC_TYPE.NULL && isAVLoadPort(carrierLocType)) {
//				return 'F';
//			}
//		}
//		return 'T';
//	}
	
	/**
	 * 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
	 * @param carrierLocId
	 * @param jobType
	 * @return
	 */
	protected char getErrorReportOption(String carrierLocId, JOB_TYPE jobType) {
		if (this.operation.isAutoRetryPort(carrierLocId, jobType)) {
			return 'F';
		}
		return 'T';
	}
	
	/**
	 * 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
	 * 
	 * @param carrierLocId
	 * @param jobType
	 * @return
	 */
	protected int getAutoRetryLimitCount(String carrierLocId, JOB_TYPE jobType) {
		return this.operation.getAutoRetryLimitCount(carrierLocId, jobType);
	}

	/**
	 * Read에 대한 Option값을 얻는다. <BR>
	 * Unload, Load 명령을 OHT에 전송할 때 RF Read 기능의 수행 여부를 지정하여 전송하도록 한다. <BR>
	 * 호출함수명: SendUnloadCommand(), SendLoadCommand() <BR>
	 * 관련요구사항: RF Read 기능 <BR>
	 * 
	 * @param carrierLocType CARRIERLOC_TYPE: Unload/Load를 수행하는 CarrierLoc Type을 입력한다.
	 * @param jobType JOB_TYPE: "UNLOAD", "LOAD" 중 하나
	 * @return RfOption char : '0', '1'을 Return 한다.
	 */
	protected char getRfOption(CARRIERLOC_TYPE carrierLocType, JOB_TYPE jobType) {
		// IDREADER 위치인 경우에 Load위치에서 RF를 읽도록 처리하고 그 외의 경우에 설정값에 따라서 Unload 위치에서 읽도록 처리
		if (jobType == JOB_TYPE.UNLOAD) {
			if (isRfReadPort(carrierLocType)) {	
				return '1';				
			}
		} else if (jobType == JOB_TYPE.LOAD) {

			// destLocType, idReader를 가져와야 함. -> 현재는 임시로 ""로 설정
			CARRIERLOC_TYPE destLocType = getCarrierLocType(trCmd.getDestLoc());
			String idReaderState = "";

			if ((destLocType == CARRIERLOC_TYPE.STBPORT || destLocType == CARRIERLOC_TYPE.UTBPORT) &&
					OperationConstant.INSTALLED.equals(idReaderState)) {
				return '1';
			}
		}

		return '0';
	}

	/**
	 * 2013.02.22 by KYK
	 * Vehicle로 Go Command 메시지를 전송한다.
	 * 
	 * @param commandId
	 * @param nodeId
	 * @param eqOptionForGoMode
	 * @param station
	 * @param shiftPosition
	 * @param commandOption
	 * @return
	 */
	protected boolean sendGoCommand(int commandId, String nodeId, char eqOptionForGoMode, char commandOption, Station station, int shiftPosition) {
		String preSteeringNodeId = vehicleData.getPreSteeringNode();
		StringBuilder message = new StringBuilder();
		message.append("GO (ID:").append(commandId);
		message.append(", StopNode:").append(nodeId);
		message.append(", TargetNode:").append(vehicleData.getTargetNode());
		message.append(", PreSteeringNode:").append(preSteeringNodeId);
		message.append(")");
		
		VehicleComm vehicleComm = getVehicleComm();
		VehicleCommCommand command = getVehicleCommCommand();
		command.setCommandId(commandId);
		command.setNodeId(nodeId);
		if (vehicleComm instanceof VehicleCommV7 == false) {
			// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
			String stationId = "";
			
			command.setCommandOption(commandOption);
			command.setEqOption(eqOptionForGoMode);
			
			changeCommandStateToSent(COMMAND_TYPE.GO);
			changeLastDiffentCommandState(commandOption + "G" + commandId + nodeId);
			
			// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
			if (station != null) {
				stationId = station.getStationId();
				message.append(", StopStation:").append(station.getStationId());
				message.append(", TagType:").append(station.getTagType());
				message.append(", NextNode:").append(station.getNextNodeId());
				message.append(", Offset:").append(station.getOffset());
			}
			
			operation.traceOperation(message.toString());
			command.setStationId(stationId);
			
			return vehicleComm.sendGoCommand(command);
		} else {
			VehicleCommV7 commV7 = (VehicleCommV7) getVehicleComm();
			String stationId = "";
			String nextNode = "";
			int stationType = 0;
			int offset = 0;
			if (station != null) {
				stationId = station.getStationId();
				stationType = station.getTagType();
				nextNode = station.getNextNodeId();
				offset = station.getOffset();
				message.append(", StopStation:").append(station.getStationId());
				message.append(", TagType:").append(station.getTagType());
				message.append(", NextNode:").append(station.getNextNodeId());
				message.append(", Offset:").append(station.getOffset());
				message.append(", ShiftPosition:").append(shiftPosition);
			}
			command.setNextNodeId(nextNode);
			command.setPreSteeringNodeId(preSteeringNodeId);
			command.setStationId(stationId);
			command.setStationOffset(offset);
			command.setStationType(stationType);
			command.setShiftPosition(shiftPosition);
			
			command.setEqOption(eqOptionForGoMode); // 2015.02.10 by zzang9un : FinalPortType 추가
			// PassDoor 관련 로그 생성
			if (eqOptionForGoMode == 'I') {
				message.append(", PassDoor:In");
			} else if (eqOptionForGoMode == 'O') {
				message.append(", PassDoor:Out");
			} else if (eqOptionForGoMode == 'P') {
				message.append(", MoveForPark");
			}
			
			operation.traceOperation(message.toString());
			changeCommandStateToSent(COMMAND_TYPE.GO);
			changeLastDiffentCommandState(commandOption + "G" + commandId + nodeId);
			
			return commV7.sendGoCommand(command);
		}
	}

	/**
	 * 2013.03.05 by KYK
	 * Vehicle로 GoMore Command 메시지를 전송한다.
	 * 
	 * @param commandId
	 * @param nodeId
	 * @param eqOptionForGoMode
	 * @param station
	 * @param shiftPosition
	 * @return
	 */
	protected boolean sendGoMoreCommand(int commandId, String nodeId, char eqOptionForGoMode, Station station, int shiftPosition) {
		String preSteeringNodeId = vehicleData.getPreSteeringNode();
		StringBuilder message = new StringBuilder();
		message.append("GoMore (ID:").append(commandId);
		message.append(", StopNode:").append(nodeId);
		message.append(", TargetNode:").append(vehicleData.getTargetNode());
		message.append(", PreSteeringNode:").append(preSteeringNodeId);
		message.append(")");
		
		VehicleComm vehicleComm = getVehicleComm();
		VehicleCommCommand command = getVehicleCommCommand();
		command.setCommandId(commandId);
		command.setNodeId(nodeId);
		if (vehicleComm instanceof VehicleCommV7 == false) {
			// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
			String stationId = "";
						
			command.setEqOption(eqOptionForGoMode);
			changeCommandStateToSent(COMMAND_TYPE.GOMORE);
			changeLastDiffentCommandState("M" + commandId + nodeId);

			// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
			if (station != null) {
				stationId = station.getStationId();
				message.append(", StopStation:").append(station.getStationId());
				message.append(", TagType:").append(station.getTagType());
				message.append(", NextNode:").append(station.getNextNodeId());
				message.append(", Offset:").append(station.getOffset());
			}
			
			// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
			command.setStationId(stationId);
			
			operation.traceOperation(message.toString());
			return vehicleComm.sendGoMoreCommand(command);
		} else {
			VehicleCommV7 commV7 = (VehicleCommV7) getVehicleComm();
			String stationId = "";
			String nextNode = "";
			int stationType = 0;
			int offset = 0;
			if (station != null) {
				stationId = station.getStationId();
				stationType = station.getTagType();
				nextNode = station.getNextNodeId();
				offset = station.getOffset();
				message.append(", StopStation:").append(station.getStationId());
				message.append(", TagType:").append(station.getTagType());
				message.append(", NextNode:").append(station.getNextNodeId());
				message.append(", Offset:").append(station.getOffset());
				message.append(", ShiftPosition:").append(shiftPosition);
			}
			command.setNextNodeId(nextNode);
			command.setPreSteeringNodeId(preSteeringNodeId);
			command.setStationId(stationId);
			command.setStationOffset(offset);
			command.setStationType(stationType);
			command.setShiftPosition(shiftPosition);
			
			command.setEqOption(eqOptionForGoMode); // 2015.02.10 by zzang9un : FinalPortType 추가
			// PassDoor 관련 로그 생성
			if (eqOptionForGoMode == 'I') {
				message.append(", PassDoor:In");
			} else if (eqOptionForGoMode == 'O') {
				message.append(", PassDoor:Out");
			}
			
			operation.traceOperation(message.toString());
			changeCommandStateToSent(COMMAND_TYPE.GOMORE);
			changeLastDiffentCommandState("M" + commandId + nodeId);

			return commV7.sendGoMoreCommand(command);
		}
	}
	
	/**
	 * 2013.03.05 by KYK
	 * Vehicle로 Unload Command 메시지를 전송한다.
	 * 
	 * @param commandId
	 * @param commandOption
	 * @return
	 */
	protected boolean sendUnloadCommand(int commandId, char commandOption) {
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(trCmd.getSourceLoc());
		if (carrierLoc == null) {
			StringBuilder message = new StringBuilder();
			message.append("UNLOAD (ID:").append(commandId);
			message.append(") CarrierLoc is null");
			operation.traceOperation(message.toString());
			return false;
		}
		
		VehicleComm vehicleComm = getVehicleComm();
		VehicleCommCommand command = getVehicleCommCommand();
		String nodeId = carrierLoc.getNode();
		command.setCommandId(commandId);
		command.setNodeId(nodeId);
		// 2014.10.30 by KYK [DualOHT] TODO
		command.setRfPioId(carrierLoc.getRfPioId());
		command.setRfPioCS(carrierLoc.getRfPioCS());
		
		// 2013.07.18 by KYK
		CARRIERLOC_TYPE locationType = carrierLoc.getType();
		char rfOption = getRfOption(locationType, JOB_TYPE.UNLOAD);
		char errorReportOption = getErrorReportOption(carrierLoc.getCarrierLocId(), JOB_TYPE.UNLOAD);
		
		if (vehicleComm instanceof VehicleCommV7 == false) {
			char eqOptionForWorkMode = locationType.toEqOptionForWorkMode();
			if ('0' == eqOptionForWorkMode) {
				if (carrierLoc.isMultiPort()) {
					eqOptionForWorkMode = '4';
				}
			}
			char carrierLocDirectionOption = getDirectionOption(carrierLoc.getCarrierLocId());
			
			command.setCommandOption(commandOption);
			command.setErrorReportOption(errorReportOption);
			command.setEqOption(eqOptionForWorkMode);
			command.setRfOption(rfOption);
			command.setCarrierlocDirectionOption(carrierLocDirectionOption);
			
			StringBuilder message = new StringBuilder();
			message.append("UNLOAD (ID:").append(commandId);
			message.append(", StopNode:").append(vehicleData.getStopNode());
			message.append(", TargetNode:").append(nodeId);
			message.append(", NextCmd:").append(commandOption);
			message.append(", ErrorReportOption:").append(errorReportOption);
			message.append(", EQOption:").append(eqOptionForWorkMode);
			message.append(", RFOption:").append(rfOption);
			message.append(", STBDirection:").append(carrierLocDirectionOption);
			// 2018.09.28 by JJW : Carrier Type 기능 사용
			if(ocsInfoManager.isCarrierTypeUsage()){
				message.append(", CarrierType:").append(carrierLoc.getMaterial());
			}
			message.append(")");
			
			// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
			String stationID = getStationIdAtPort(trCmd.getSourceLoc()); // Return StationID
			if (stationID != null && stationID.length() > 0) {
				getVehicleCommCommand().setStationId(stationID);
				message.append(", TargetQRTag:").append(stationID);
			}

			operation.traceOperation(message.toString());
			changeCommandStateToSent(COMMAND_TYPE.UNLOAD);
			
			changeLastDiffentCommandState("U" + commandId + nodeId + eqOptionForWorkMode);

			return vehicleComm.sendUnloadCommand(getVehicleCommCommand());
		} else {
			VehicleCommV7 commV7 = (VehicleCommV7) vehicleComm;
			String stationId = carrierLoc.getStationId();
			int portType = carrierLoc.getSubType();
			int pioDirection = carrierLoc.getPioDirection();
			int hoistPosition = carrierLoc.getHoistPosition();
			int shiftPosition = carrierLoc.getShiftPosition();
			int rotatePosition = carrierLoc.getRotatePosition();
			int hoistSpeedLevel = getHoistSpeedLevel();
			int shiftSpeedLevel = getShiftSpeedLevel();
			int pioTimeLevel = carrierLoc.getPioTimeLevel();
			int lookDownLevel = carrierLoc.getLookDownLevel();
			int extraOption = carrierLoc.getExtraOption();
			
			command.setStationId(stationId);
			command.setPortType(portType);
			command.setCarrierType(getCarrierType(carrierLoc.getMaterial())); // carrierType 추가
			command.setPIODirection(pioDirection);
			command.setTPosition(hoistPosition, shiftPosition, rotatePosition);
			command.setHoistSpeedLevel(hoistSpeedLevel);
			command.setShiftSpeedLevel(shiftSpeedLevel);
			command.setPioTimeLevel(pioTimeLevel);
			command.setLookDownLevel(lookDownLevel);
			command.setExtraOption(extraOption);
			// 2013.07.18 by KYK
			command.setErrorReportOption(errorReportOption);
			command.setRfOption(rfOption);
			
			StringBuilder message = new StringBuilder();			
			message.append("UNLOAD (ID:").append(commandId);
			message.append(", StopNode:").append(vehicleData.getStopNode());
			message.append(", TargetNode:").append(nodeId);
			message.append(", TargetStation:").append(stationId);
			message.append(", Hoist:").append(hoistPosition);
			message.append(", Shift:").append(shiftPosition);
			message.append(", Rotate:").append(rotatePosition);
			message.append(", CarrierType:").append(carrierLoc.getMaterial());
			// 2013.07.18 by KYK
			message.append(", ErrorOption:").append(errorReportOption);
			message.append(", RFOption:").append(rfOption);
			message.append(")");
			
			operation.traceOperation(message.toString());
			changeCommandStateToSent(COMMAND_TYPE.UNLOAD);
			
			changeLastDiffentCommandState("U" + commandId + nodeId + portType);

			return commV7.sendUnloadCommand(command);
		}
	}
	
	/**
	 * 2013.03.05 by KYK
	 * Vehicle로 Load Command 메시지를 전송한다.
	 * 
	 * @param commandId
	 * @param commandOption
	 * @return
	 */
	protected boolean sendLoadCommand(int commandId, char commandOption) {
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(trCmd.getDestLoc());
		if (carrierLoc == null) {
			StringBuilder message = new StringBuilder();
			message.append("LOAD (ID:").append(commandId);
			message.append(") CarrierLoc is null");
			operation.traceOperation(message.toString());
			return false;
		}
		
		VehicleComm vehicleComm = getVehicleComm();
		VehicleCommCommand command = getVehicleCommCommand();
		String nodeId = carrierLoc.getNode();
		command.setCommandId(commandId);
		command.setNodeId(nodeId);
		// 2014.10.30 by KYK [DualOHT] TODO
		command.setRfPioId(carrierLoc.getRfPioId());
		command.setRfPioCS(carrierLoc.getRfPioCS());

		// 2013.07.18 by KYK
		CARRIERLOC_TYPE locationType = carrierLoc.getType();
		char rfOption = getRfOption(locationType, JOB_TYPE.LOAD);
		char errorReportOption = getErrorReportOption(carrierLoc.getCarrierLocId(), JOB_TYPE.LOAD);

		if (vehicleComm instanceof VehicleCommV7 == false) {
			char eqOptionForWorkMode = locationType.toEqOptionForWorkMode();
			if ('0' == eqOptionForWorkMode) {
				if (carrierLoc.isMultiPort()) {
					eqOptionForWorkMode = '4';
				}
			}
			char carrierLocDirectionOption = getDirectionOption(carrierLoc.getCarrierLocId());
			
			command.setCommandOption(commandOption);
			command.setErrorReportOption(errorReportOption);
			command.setEqOption(eqOptionForWorkMode);
			command.setRfOption(rfOption);
			command.setCarrierlocDirectionOption(carrierLocDirectionOption);
			
			StringBuilder message = new StringBuilder();
			message.append("LOAD (ID:").append(commandId);
			message.append(", StopNode:").append(vehicleData.getStopNode());
			message.append(", TargetNode:").append(nodeId);
			message.append(", NextCmd:").append(commandOption);
			message.append(", ErrorReportOption:").append(errorReportOption);
			message.append(", EQOption:").append(eqOptionForWorkMode);
			message.append(", RFOption:").append(rfOption);
			message.append(", STBDirection:").append(carrierLocDirectionOption);
			// 2018.09.28 by JJW : Carrier Type 기능 사용
			if(ocsInfoManager.isCarrierTypeUsage()){
			message.append(", CarrierType:").append(carrierLoc.getMaterial());
			}
			message.append(")");
			
			// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가]
			String stationID = getStationIdAtPort(trCmd.getDestLoc()); // Return StationID
			if (stationID != null && stationID.length() > 0) {
				getVehicleCommCommand().setStationId(stationID);
				message.append(", TargetQRTag:").append(stationID);
			}
			
			operation.traceOperation(message.toString());
			changeCommandStateToSent(COMMAND_TYPE.LOAD);
			
			return vehicleComm.sendLoadCommand(getVehicleCommCommand());
		} else {
			VehicleCommV7 commV7 = (VehicleCommV7) vehicleComm;
			String stationId = carrierLoc.getStationId();
			int portType = carrierLoc.getSubType();
			int pioDirection = carrierLoc.getPioDirection();
			int hoistPosition = carrierLoc.getHoistPosition();
			int shiftPosition = carrierLoc.getShiftPosition();
			int rotatePosition = carrierLoc.getRotatePosition();
			int hoistSpeedLevel = getHoistSpeedLevel();
			int shiftSpeedLevel = getShiftSpeedLevel();
			int pioTimeLevel = carrierLoc.getPioTimeLevel();
			int lookDownLevel = carrierLoc.getLookDownLevel();
			int extraOption = carrierLoc.getExtraOption();
			
			command.setStationId(stationId);
			command.setPortType(portType);
			command.setCarrierType(getCarrierType(carrierLoc.getMaterial())); // carrierType 추가
			command.setPIODirection(pioDirection);
			command.setTPosition(hoistPosition, shiftPosition, rotatePosition);
			command.setHoistSpeedLevel(hoistSpeedLevel);
			command.setShiftSpeedLevel(shiftSpeedLevel);
			command.setPioTimeLevel(pioTimeLevel);
			command.setLookDownLevel(lookDownLevel);
			command.setExtraOption(extraOption);
			// 2013.07.18 by KYK
			command.setErrorReportOption(errorReportOption);
			command.setRfOption(rfOption);

			StringBuilder message = new StringBuilder();			
			message.append("LOAD (ID:").append(commandId);
			message.append(", StopNode:").append(vehicleData.getStopNode());
			message.append(", TargetNode:").append(nodeId);
			message.append(", TargetStation:").append(stationId);
			message.append(", Hoist:").append(hoistPosition);
			message.append(", Shift:").append(shiftPosition);
			message.append(", Rotate:").append(rotatePosition);
			message.append(", CarrierType:").append(carrierLoc.getMaterial());
			// 2013.07.18 by KYK
			message.append(", ErrorOption:").append(errorReportOption);
			message.append(", RFOption:").append(rfOption);
			message.append(")");
			
			operation.traceOperation(message.toString());
			changeCommandStateToSent(COMMAND_TYPE.LOAD);
			
			return commV7.sendLoadCommand(command);
		}
	}
	
	protected boolean sendPauseCommand() {
		return operation.sendPauseCommand();
	}

	protected boolean sendResumeCommand() {
		return operation.sendResumeCommand();
	}
	
	// 2020.05.11 by YSJ (OHT Auto Change)
	protected boolean sendVehicleAutoCommand() {
		return operation.sendVehicleAutoCommand();
	}

	/**
	 * 2013.03.05 by KYK
	 * Vehicle로 Scan Command 메시지를 전송한다.
	 * 
	 * @param commandId
	 * @param commandOption
	 * @return
	 */
	protected boolean sendScanCommand(int commandId, char commandOption) {
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(trCmd.getSourceLoc());
		if (carrierLoc == null) {
			StringBuilder message = new StringBuilder();
			message.append("SCAN (ID:").append(commandId);
			message.append(") CarrierLoc is null");
			operation.traceOperation(message.toString());
			return false;
		}
		
		VehicleComm comm = getVehicleComm();
		VehicleCommCommand command = getVehicleCommCommand();
		String nodeId = carrierLoc.getNode();
		command.setCommandId(commandId);
		command.setNodeId(nodeId);
		
		if (comm instanceof VehicleCommV7 == false) {
			char eqOptionForWorkMode = carrierLoc.getType().toEqOptionForWorkMode();
			char carrierLocDirectionOption = getDirectionOption(carrierLoc.getCarrierLocId());
			
			command.setCommandOption(commandOption);
			command.setEqOption(eqOptionForWorkMode);
			command.setCarrierlocDirectionOption(carrierLocDirectionOption);
			
			StringBuilder message = new StringBuilder();
			message.append("SCAN (ID:").append(commandId);
			message.append(", StopNode:").append(vehicleData.getStopNode());
			message.append(", TargetNode:").append(nodeId);
			message.append(", NextCmd:").append(commandOption);
			message.append(", EQOption:").append(eqOptionForWorkMode);
			message.append(", STBDirection:").append(carrierLocDirectionOption);
			message.append(")");
			
			operation.traceOperation(message.toString());
			changeCommandStateToSent(COMMAND_TYPE.SCAN);
			changeLastDiffentCommandState("R" + commandId + nodeId + eqOptionForWorkMode);
			
			return comm.sendScanCommand(getVehicleCommCommand());
		} else {
			VehicleCommV7 commV7 = (VehicleCommV7) comm;
			String stationId = carrierLoc.getStationId();
			int portType = carrierLoc.getSubType();
			int pioDirection = carrierLoc.getPioDirection();
			int hoistPosition = carrierLoc.getHoistPosition();
			int shiftPosition = carrierLoc.getShiftPosition();
			int rotatePosition = carrierLoc.getRotatePosition();
			int hoistSpeedLevel = getHoistSpeedLevel();
			int shiftSpeedLevel = getShiftSpeedLevel();
			int pioTimeLevel = carrierLoc.getPioTimeLevel();
			int lookDownLevel = carrierLoc.getLookDownLevel();
			int extraOption = carrierLoc.getExtraOption();
			
			command.setStationId(stationId);
			command.setPortType(portType);
			command.setCarrierType(getCarrierType(carrierLoc.getMaterial())); // carrierType 추가
			command.setPIODirection(pioDirection);
			command.setTPosition(hoistPosition, shiftPosition, rotatePosition);
			command.setHoistSpeedLevel(hoistSpeedLevel);
			command.setShiftSpeedLevel(shiftSpeedLevel);
			command.setPioTimeLevel(pioTimeLevel);
			command.setLookDownLevel(lookDownLevel);
			command.setExtraOption(extraOption);
			
			StringBuilder message = new StringBuilder();			
			message.append("SCAN (ID:").append(commandId);
			message.append(", StopNode:").append(vehicleData.getStopNode());
			message.append(", TargetNode:").append(nodeId);
			message.append(", TargetStation:").append(stationId);
			message.append(", Hoist:").append(hoistPosition);
			message.append(", Shift:").append(shiftPosition);
			message.append(", Rotate:").append(rotatePosition);
			message.append(", CarrierType:").append(carrierLoc.getMaterial());
			message.append(")");
			
			operation.traceOperation(message.toString());
			changeCommandStateToSent(COMMAND_TYPE.SCAN);
			changeLastDiffentCommandState("R" + commandId + nodeId + portType);
			
			return commV7.sendScanCommand(command);
		}
	}

	/**
	 * Vehicle로 MapMake Command 메시지를 전송한다.
	 * 
	 * @param commandId int : Command ID
	 * @param nodeId String : Node ID
	 * @return boolean
	 */
	protected boolean sendMapMakeCommand(int commandId, String nodeId) {
		StringBuilder message = new StringBuilder();
		message.append("MAPMAKE (ID:").append(commandId);
		message.append(", StopNode:").append(vehicleData.getStopNode());
		message.append(", TargetNode:").append(nodeId);
		message.append(")");
		operation.traceOperation(message.toString());

		// 2012.02.14 by PMM
//		changeCommandStateToSent();
//		getVehicleComm().setLastSentCommand(COMMAND_TYPE.MAPMAKE);
		changeCommandStateToSent(COMMAND_TYPE.MAPMAKE);
		
		changeLastDiffentCommandState("K" + commandId + nodeId);
		
		getVehicleCommCommand().setCommandId(commandId);
		getVehicleCommCommand().setNodeId(nodeId);

		return getVehicleComm().sendMapMakeCommand(getVehicleCommCommand());
	}
	
	/**
	 * 2013.03.05 by KYK
	 * @param commandId
	 * @param nodeId
	 * @param station
	 * @return
	 */
	protected boolean sendMapMakeCommand(int commandId, String nodeId, Station station) {
		String stationId = "";
		if (station != null) {
			stationId = station.getStationId();
		}
		StringBuilder message = new StringBuilder();
		message.append("MAPMAKE (ID:").append(commandId);
		message.append(", StopNode:").append(vehicleData.getStopNode());
		message.append(", TargetNode:").append(nodeId);
		if (station != null) {
			message.append("TargetStation:").append(stationId);
		}
		message.append(")");
		operation.traceOperation(message.toString());
		
		changeCommandStateToSent(COMMAND_TYPE.MAPMAKE);
		changeLastDiffentCommandState("K" + commandId + nodeId);

		VehicleCommCommand command = getVehicleCommCommand();
		command.setCommandId(commandId);
		command.setNodeId(nodeId);
		
		if (getVehicleComm() instanceof VehicleCommV7) {
			VehicleCommV7 commV7 = (VehicleCommV7) getVehicleComm();
			command.setStationId(stationId);
			command.setStationOffset(0);
			command.setStationType(0);
			return commV7.sendMapMakeCommand(command);
		}
		return getVehicleComm().sendMapMakeCommand(getVehicleCommCommand());
	}
	
	/**
	 * Vehicle로 Patrol Command 메시지를 전송한다.
	 * 
	 * @param int commandId : Command ID
	 * @param String nodeId : Node ID
	 * @param int patrolMode : Patrol Mode
	 * @return boolean
	 */
	protected boolean sendPatrolCommand(int commandId, String nodeId, int patrolMode) {
		StringBuilder message = new StringBuilder();
		message.append("PATROL (ID:").append(commandId);
		message.append(", StopNode:").append(vehicleData.getStopNode());
		message.append(", TargetNode:").append(nodeId);
		message.append(")");
		operation.traceOperation(message.toString());

		// 2012.02.14 by PMM
//		changeCommandStateToSent();
//		getVehicleComm().setLastSentCommand(COMMAND_TYPE.PATROL);
		changeCommandStateToSent(COMMAND_TYPE.PATROL);
		changeLastDiffentCommandState("C" + commandId + nodeId);
		
		getVehicleCommCommand().setCommandId(commandId);
		getVehicleCommCommand().setNodeId(nodeId);
		// 2015.11.23 by KBS : Patrol VHL 기능 개발
		getVehicleCommCommand().setPatrolMode(patrolMode);

		return getVehicleComm().sendPatrolCommand(getVehicleCommCommand());
	}
	
	/**
	 * CarrierLoc의 DirectionOption을 반환한다. (L/R/X)
	 * 
	 * @param carrierLoc String : CarrierLoc
	 * @return DirectionOption char: L (Left), R (Right), X (Non STBPort)
	 */
	protected char getDirectionOption(String carrierLoc) {
		if (getCarrierLocType(carrierLoc) == CARRIERLOC_TYPE.STBPORT) {
			if (carrierLoc.indexOf("L_") > 0) {
				return 'L';
			} else if (carrierLoc.indexOf("R_") > 0) {
				return 'R';
			}   
		} 
		return 'X';
	}
	
	/**
	 * TrCmd의 Pause 설정을 변경한다.
	 * 
	 * @param pause boolean : PAUSE 설정 or 해제 (true/false)
	 * @param pauseType String : PAUSE Type 
	 * @param pauseCount int : PAUSE Count
	 */
	protected void pauseTrCmd(boolean pause, String pauseType, int pauseCount) {
		this.operation.pauseTrCmd(pause, pauseType, pauseCount);
	}
	
	/**
	 * DestLoc의 Port Duplicate 체크
	 * 
	 * @return PortDuplicated boolean: DestLoc의 Port Duplicated 여부
	 */
	protected boolean checkDestPortDuplicate() {
		if (ocsInfoManager.isPortDuplicationUsed()) {
			// Step 1: 현재 VHL이 작업정보가 있는지 확인한다.
			if (trCmd == null) {
				return false;
			}

			// Step 2: DestLoc 정보가 있는지 확인하고, DestLoc Type이 Stocker Port가 아닌지 확인한다.
			if ((trCmd.getDestLoc() != null && trCmd.getDestLoc().length() == 0) ||
					getCarrierLocType(trCmd.getDestLoc()) == CARRIERLOC_TYPE.STOCKERPORT) {
				return false;
			}

			// Step 3: Load 명령 이전 상태인지 확인한다.
			if (trCmd.getDetailState() != TRCMD_DETAILSTATE.LOAD_ASSIGNED) {
				return false;
			}
			
			// Step 4: Source Port가 현재 VHL이 수행하고 있는 작업의 DestPort와 동일한 작업이 있는지 확인한다.
			//         이때 존재하는 작업이 Unloaded 이전상태인지도 같이 확인한다.
			return checkUnloadTrCmdExistOnDestPort(trCmd.getDestLoc());
		}
		return false;
	}
	
	/**
	 * DestPort에 다른 UNLOAD TrCmd 존재 여부를 확인한다.
	 * 
	 * @param destLoc String : DestLoc 정보 
	 * @return UnloadTrCmdExisted boolean: DestPort에 다른 UNLOAD TrCmd 존재 여부
	 */
	protected boolean checkUnloadTrCmdExistOnDestPort(String destLoc) {
		return this.operation.checkUnloadTrCmdExistOnDestPort(destLoc);
	}
	
	/**
	 * DestPort에 중복된 다른 UNLOAD TrCmd를 반환한다.
	 * 
	 * @param destLoc String : DestLoc 정보 
	 * @return DuplicatedUnloadTrCmd TrCmd: DestPort에 중복된 다른 UNLOAD TrCmd
	 */
	protected TrCmd getUnloadTrCmdExistOnDestPort(String destLoc) {
		return this.operation.getUnloadTrCmdExistOnDestPort(destLoc);
	}

	/**
	 * OperationMode를 변경한다.
	 * 
	 * @param operationMode OPERATION_MODE : 변경하고자 하는 OperationMode 
	 * @param message String : OperationMode의 변경 사유 
	 */
	protected void changeOperationMode(OPERATION_MODE operationMode, String message) {
		this.operation.changeOperationMode(operationMode, message);
	}

	protected VehicleComm getVehicleComm() {
		return this.operation.getVehicleComm();
	}

	protected VehicleCommCommand getVehicleCommCommand() {
		return this.operation.getVehicleCommCommand();
	}

	protected VehicleData getVehicleData() {
		return this.operation.getVehicleData();
	}

	protected COMMAND_STATE getCommandState() {
		return this.operation.getCommandState();
	}
	
	protected void setCommandState(COMMAND_STATE cmdState) {
		this.operation.setCommandState(cmdState);
	}

	protected void changeCommandStateToSent(COMMAND_TYPE commandType) {
		this.operation.changeCommandStateToSent(commandType);
	}
	
	protected void changeLastDiffentCommandState(String sentMessage) {
		this.operation.changeLastDiffentCommandState(sentMessage);
	}

	public TrCmd getTrCmd() {
		return this.operation.getTrCmd();
	}
	
	public void setTrCmd(TrCmd trCmd) {
		this.trCmd = trCmd;
	}
	
	protected void resetTrCmd() {
		this.operation.resetTrCmd();
	}
	
//	public void registerUnknownTrCmd() {
//		this.operation.registerUnknownTrCmd();
//	}
	
	protected void traceOperation(String message) {
		this.operation.traceOperation(message);
	}
	
	protected void traceOperationException(String message, Exception e) {
		this.operation.traceOperationException(message, e);
	}
	protected void traceOperationException(String message) {
		this.operation.traceOperationException(message);
	}
	
	protected void traceOperationDelay(String message) {
		this.operation.traceOperationDelay(message);
	}

	protected boolean sendEStopCommand(int type) {
		return this.operation.sendEStopCommand(type);
	}
	
	protected boolean sendIDResetCommand() {
		return this.operation.sendIDResetCommand();
	}
	protected boolean sendCancelCommand(int commandId, char commandOption) {
		return this.operation.sendCancelCommand(commandId, commandOption);
	}
	
	protected void sendS6F11(EVENT_TYPE eventType, String eventName, int alarmId) {
		this.operation.sendS6F11(eventType, eventName, alarmId);
	}
	
	/**
	 * Report 등록을 위해 Operation의 registerReport(message)를 호출
	 * 
	 * @param message String: 등록한 메시지 내용
	 */
	protected void registerReport(String message) {
		this.operation.registerReport(message);
	}
	
	/**
	 * VehicleData를 UPDATE하기 위해 Operation의 addVehicleToUpdateList()를 호출하여 해당 VehicleData를 UpdateList에 등록.
	 * 
	 */
	protected void addVehicleToUpdateList() {
		this.operation.addVehicleToUpdateList();
	}
	
	/**
	 * TrCmd를 UPDATE하기 위해 Operation의 addTrCmdToUpdateList()를 호출하여 해당 TrCmd를 UpdateList에 등록.
	 * 
	 */
	protected void addTrCmdToStateUpdateList() {
		assert (trCmd != null);
		
		this.operation.addTrCmdToStateUpdateList();
	}
	
	/**
	 * Add TrCmd to Vehicle Update List
	 */
	protected void addTrCmdToVehicleUpdateList() {
		assert (trCmd != null);
		
		this.operation.addTrCmdToVehicleUpdateList();
		
	}
	
	protected void cancelAssignedTrCmd(EVENTHISTORY_REASON reason, boolean report) {
		assert (trCmd != null);
		
		this.operation.cancelAssignedTrCmd(reason, report);
	}
	
	/**
	 * TrCmd를 DELETE하기 위해 Operation의 deleteTrCmdFromDB()를 호출한다. (즉시 삭제)
	 * 
	 */
	protected void deleteTrCmdFromDB() {
		assert (trCmd != null);
		
		this.operation.deleteTrCmdFromDB();
	}
	
	/**
	 * Delete STAGE Command From DB
	 */
	protected void deleteStageCmdFromDB() {
		assert (trCmd != null);
		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE);
		
		this.operation.deleteStageCmdFromDB();
	}
	
	protected String getCurrDBTimeStr() {
		return this.operation.getCurrDBTimeStr();
	}

	protected void registerTrCompletionHistory(String remoteCmd) {
		this.operation.registerTrCompletionHistory(remoteCmd);
	}

	protected void cancelNextAssignedTrCmd(EVENTHISTORY_REASON reason) {
		this.operation.cancelNextAssignedTrCmd(reason);
	}
	
	protected void cancelVibrationCommand(EVENTHISTORY_REASON reason) {
		this.operation.cancelVibrationCommand(reason);
	}
	
	protected boolean searchVehiclePath(String toNode, String type, boolean request) {
		return this.operation.searchVehiclePath(toNode, type, request);
	}
	
	protected boolean searchVehicleYieldPath() {
		return this.operation.searchVehicleYieldPath();		
	}
	
	protected boolean searchEscapeForAbnormalHid() {
		return this.operation.searchEscapeForAbnormalHid();		
	}
	
	protected boolean searchLocalVehicleComebackBayPath() {
		return this.operation.searchLocalVehicleComebackBayPath();		
	}
	
	protected boolean searchVehicleComebackZonePath() {
		return this.operation.searchVehicleComebackZonePath();		
	}
	
	protected String driveVehiclePath() {
		return this.operation.driveVehiclePath();
	}

	protected void clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION clearOption) {
		this.operation.clearVehicleLocalGroupInfo(clearOption);
	}
	
	protected void updateRequestedCommandReset(REQUESTEDTYPE requestedType, String message) {
		this.operation.updateRequestedCommandReset(requestedType, message);
	}

	protected void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
		this.operation.registerEventHistory(eventHistory, duplicateCheck);
	}
	
	// 2012.01.02 by PMM
//	protected void registerVehicleErrorHistory(VehicleErrorHistory vehicleErrorHistory) {
//		this.operation.registerVehicleErrorHistory(vehicleErrorHistory);
//	}
//	protected void resetFromVehicleErrorHistory(VehicleErrorHistory vehicleErrorHistory) {
//		this.operation.resetFromVehicleErrorHistory(vehicleErrorHistory);
//	}
	protected void registerVehicleErrorHistory(int alarmCode, String alarmText, String type) {
		this.operation.registerVehicleErrorHistory(alarmCode, alarmText, type);
	}
	protected void resetFromVehicleErrorHistory() {
		this.operation.resetFromVehicleErrorHistory();
	}
	
	protected boolean isAlarmRegistered() {
		return this.operation.isAlarmRegistered();
	}
	
	protected boolean isValidNodeUpdated() {
		return this.operation.isValidNodeUpdated();
	}
	
	protected void setAlarmCode(int alarmCode) {
		this.operation.setAlarmCode(alarmCode);
	}
	
	public void registerAlarm(int alarmCode) {
		this.operation.registerAlarm(alarmCode);
	}
	
	protected void unregisterAlarm(int alarmCode) {
		this.operation.unregisterAlarm(alarmCode);
	}
	
	protected boolean isFailoverCompleted() {
		return this.operation.isFailoverCompleted();
	}
	
	protected boolean checkCarrierMismatchedOnUnloadPort() {
		return this.operation.checkCarrierMismatchedOnUnloadPort();
	}

	/**
	 * 2013.09.06 by KYK
	 * @return
	 */
	protected boolean checkCarrierTypeMismatch() {
		return this.operation.checkCarrierTypeMismatch();
	}
	
	// 2011.12.09 by PMM
	protected boolean isCarrierExpected() {
		if (trCmd != null) {
			// 2012.03.12 by PMM
			if (trCmd.isLoadingByPass() || vehicleData.isLoadingByPass()) {
				return false;
			}
			
			switch (trCmd.getDetailState()) {
			  // 2013.09.13 by MYM : UNLOADED 조건 추가
		    	case UNLOADED:
				case LOAD_ASSIGNED:
				// 2018.03.14 by LSH : LOAD_SENT, LOAD_ACCEPTED 조건 추가 (비정상완료보고 누락 보완)
				// 2018.09.17 by LSH : LOAD_SENT, LOAD_ACCEPTED를 SleepMode일 경우에만 체크하도록 메서드 분리 (기존 메서드는 조건 원복)
				//                    (OHT가 Load Ack 응답을 늦게하면서 Carrier를 내려놓는 경우, E-STOP 발생 가능)
//				case LOAD_SENT:
//				case LOAD_ACCEPTED:
				case VIBRATION_MONITORING:
					return true;
				default:
					return false;
			}
		} else {
			return false;
		}
	}
	
	// 2018.09.17 by LSH
	protected boolean isCarrierExpectedForAbnormalStatus() {
		if (trCmd != null) {
			// 2012.03.12 by PMM
			if (trCmd.isLoadingByPass() || vehicleData.isLoadingByPass()) {
				return false;
			}

			switch (trCmd.getDetailState()) {
			// 2013.09.13 by MYM : UNLOADED 조건 추가
			case UNLOADED:
			case LOAD_ASSIGNED:
				// 2018.03.14 by LSH : LOAD_SENT, LOAD_ACCEPTED 조건 추가 (비정상완료보고 누락 보완)
				// 2018.09.17 by LSH : LOAD_SENT, LOAD_ACCEPTED를 SleepMode일 경우에만 체크하도록 메서드 분리
				//                    (OHT가 Load Ack 응답을 늦게하면서 Carrier를 내려놓는 경우, E-STOP 발생 가능)
			case LOAD_SENT:
			case LOAD_ACCEPTED:
			case VIBRATION_MONITORING:
				return true;
			default:
				return false;
			}
		} else {
			return false;
		}
	}
	
	// 2011.12.09 by PMM
	protected boolean isCarrierUnexpected() {
		if (trCmd != null) {
			// 2012.03.12 by PMM
			if (trCmd.isLoadingByPass() || vehicleData.isLoadingByPass()) {
				return true;
			}
			
			switch (trCmd.getDetailState()) {
				case UNLOAD_ASSIGNED:
					return true;
				default:
					return false;
			}
		} else {
			return true;
		}
	}
	
	// 2011.12.09 by PMM
	protected boolean checkAbnormalCarrierStatus() {
		if (isLoadingByPass() == false) {
			if (trCmd == null) {
				if (vehicleData.isCarrierExist()) {
//					// 2011.12.09 by PMM 체크 조건 추가
//					if (vehicleData.getAlarmCode() != OcsAlarmConstant.CARRIER_STATUS_ERROR_NOTRCMD) {
//						// Abnormal. Carrier Status Error 알람 등록.
//						setAlarmCode(OcsAlarmConstant.CARRIER_STATUS_ERROR_NOTRCMD);
//					}
					registerUnknownTrCmd();
				} else {
					// Normal.
					if (vehicleData.getAlarmCode() == OcsAlarmConstant.CARRIER_STATUS_ERROR_NOTRCMD) {
						unregisterAlarm(OcsAlarmConstant.CARRIER_STATUS_ERROR_NOTRCMD);
					}
				}
			} else {
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER || trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
					if (isCarrierUnexpected()) {
						// UNLOAD 이전
						if (vehicleData.isCarrierExist()) {
							// Abnormal. E-Stop.
							setAlarmCode(OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_UNLOAD);
							sendEStopCommand(3);
							return true;
						} else {
							// Normal.
							if (vehicleData.getAlarmCode() == OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_UNLOAD) {
								unregisterAlarm(OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_UNLOAD);
							}
						}
					}
					
					if (isCarrierExpected()) {
						// UNLOAD 이후
						if (vehicleData.isCarrierExist()) {
							// Normal.
							if (vehicleData.getAlarmCode() == OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_LOAD) {
								unregisterAlarm(OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_LOAD);
							}
						} else {
							// Abnormal. E-Stop.
							setAlarmCode(OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_LOAD);
							sendEStopCommand(4);
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	protected boolean isAbnormalHidCheckCondition() {
		if (trCmd == null) {
			return true;
		} else {
			switch (trCmd.getDetailState()) {
				case UNLOAD_ASSIGNED:
				case LOAD_ASSIGNED:
				case STAGE_ASSIGNED:
				case STAGE_NOBLOCKING:
				case STAGE_WAITING:
				case MAPMAKE_ASSIGNED:
				case MAPMAKING:
				case PATROL_ASSIGNED:
				case PATROLLING:
					return true;
				default:
					return false;
			}
		}
	}
	
	protected void resetLocateRequest() {
		this.operation.resetLocateRequest();
	}
	
	protected void resetStageRequest(String reason) {
		this.operation.resetStageRequest(reason);
	}

	protected int getHoistSpeedLevel() {
		return this.operation.getHoistSpeedLevel();
	}
	
	protected int getShiftSpeedLevel() {
		return this.operation.getShiftSpeedLevel();
	}
}
