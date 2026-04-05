package com.samsung.ocs.operation.mode;

import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.TrCmdConstant;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.PassDoor;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.VehicleData;
import com.samsung.ocs.operation.Operation;
import com.samsung.ocs.operation.constant.MessageItem.EVENT_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant;
import com.samsung.ocs.operation.constant.OperationConstant.COMMAND_STATE;
import com.samsung.ocs.operation.constant.OperationConstant.OPERATION_MODE;

/**
 * GoMode Class, OCS 3.0 for Unified FAB
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

public class GoMode extends OperationModeImpl {
	private String stopNode = "";
	
	/**
	 * Constructor of GoMode class.
	 */
	public GoMode(Operation operation) {
		super(operation);
	}
	
	@Override
	public OPERATION_MODE getOperationMode() {
		return OPERATION_MODE.GO;
	}

	/**
	 * Control Vehicle in GoMode
	 */
	@Override
	public boolean controlVehicle() {
		trCmd = getTrCmd();
	
		// 1. SLEEP 조건  체크.
		if (checkSleepMode()) {
			return false;
		}
		
		// 2013.09.10 by MYM : 주행 중 재하 이상시 E-Stop 전송
		if (isGoModeCarrierStatusCheckUsed() && checkAbnormalCarrierStatus()) {
			return false;
		}
		
		// 2023.01.04 by JJW : 주행 중 Carrier Type이 다른 경우 E-Stop 전송
		if (vehicleData.isCarrierExist() && ocsInfoManager.isCarrierTypeUsage() && checkCarrierTypeMismatch()) {
			return false;
		}
		
		// 2. Auto Position 상태 처리
		if (checkAutoPositioning() == false) {
			return false;
		}
		
		// 3. Vehicle ActionHold 체크.
		if (vehicleData.isActionHold()) {
			return true;
		}
		
		// 4. Alarm 해제.
		if (isAlarmRegistered()) {
			unregisterAlarmInGoMode();
		}
		
		// 5. Vehicle 제어
		// 2013.02.01 by KYK
		if (hasDrivenToTarget()) {
			// 2012.02.06 by PMM
			// Park 요청받았던 호기에 대한  reset.
			// 도착, Manual->Auto 전환 호기는 모두 reset 되도록 하기 위해 IdleMode에서 reset.
			// reset이 제대로 안되면 YieldSearch 진행 안 됨.
			//
			// IdleMode에서만 하면, 양보 요청 받으면 IdleMode로 가지 않아 reset이 안되는 케이스 생김.
			resetLocateRequest();
			
			if (vehicleData.getYieldState() == 'Y') {
				vehicleData.setYieldState('N');
				vehicleData.setDriveFailedNode(null);
			}
			
			// 2012.01.05 by PMM
			// SourceNode로 업데이트하였으나 아직 G로 보고하면서 정위치 중, 전방 호기를 대차 감지하여 A로 보고하지 못해
			// Unload 작업을 수행하지 못하는 케이스 발생함.
			// if (hasArrivedAtStopNode() {

			// 2013.02.01 by KYK
			if (hasArrivedAtTarget() && vehicleData.getState() != 'G') {
				// 5-1. TargetNode에 도착한 경우.
				return controlVehicleArrivedAtTarget();
			} else {
				// 5-2. TargetNode에 Drive만 하고, 도착 전.
				return controlVehicleDrivedToButNotYetArrivedAtTarget();
			}
		} else {
				// 5-3. TargetNode까지 Drive하지 못한 경우.
			return controlVehicleNotYetDrivedToTarget();
		}
	}
	
	/**
	 * Check AutoPositioning Vehicle
	 * 
	 * @return
	 */
	private boolean checkAutoPositioning() {
		// 2011.12.02 by MYM : PauseType 3 추가
    // Auto Position 상태를 받았을 때 처리
		// ※ Vehicle Pause Type?
		//   0: 정상 주행
		//   1: 대차 센서에 의한 일시 정지상태
		//   2: 대차가  감지되어 OCS에서 Pause 명령 전송에 의한  일시 정지상태
		//   3: OCS에서 Pause 명령을 전송하였고 OHT가 전방 대차가 감지되지 않은 상태에서 일시 정지상태
		//      . 2 -> 3으로 될 수 있음.
    //   ex1. 정상 주행
    //        $tAG0065400910000[1,315,302,7,-1692651.33,1.75,0]
    //   ex2. 대차 센서에 의한 일시 정지상태
    //        $tAG0065400910000[1,315,302,7,-1692651.33,1.75,1]
    //   ex3. Pause 명령에 의한 일시 정지상태(전방 대차센서 감지중)
    //        $tAG0065400910000[1,315,302,7,-1692651.33,1.75,2]
    //   ex4. Pause 명령에 의한 일시 정지중 전방 대차센서 감지 안된 상태
    //        $tAG0065400910000[1,315,302,7,-1692651.33,1.75,3]
		if (vehicleData.isAutoPositioning()) {
			// Auto Position 상태이면 후방에 진입하는 Vehicle을 확인하여 Pause Flag를 설정
			
			// 2012.01.19 by PMM
//			vehicleData.checkPauseRequestToBackwardVehicle();
			try {
				vehicleData.checkPauseRequestToBackwardVehicle();
			} catch (Exception e) {
				traceOperationException("GoMode - checkAutoPositioning()", e);
			}
			
			return false; // 2011.10.24 by MYM : AutoPosition인 경우 Go, GoMore 명령 미전송
		} else {
			// 1. PauseType이 3인 경우 무조건 Resume 명령 전송
			if (vehicleData.getPauseType() == 3) {
				StringBuffer log = new StringBuffer("CheckPause ");
				log.append("[Pause:").append(vehicleData.getPauseType());
				log.append(" CurrNode:").append(vehicleData.getDriveCurrNode()).append("], ");
				log.append("[SendResume(PauseType:").append(vehicleData.getPauseType()).append(")]");
				
				sendResumeCommand();
				vehicleData.setPauseRequestVehicle(null);
				vehicleData.setPauseReason("");
				traceOperation(log.toString());
				return false;
			}
			
			// 2. Pause/Resume 대상 호기 확인 후 Pause/Resume 명령 전송 
			VehicleData pauseRequestVehicle = vehicleData.getPauseRequestVehicle();
			if (pauseRequestVehicle != null) {
				StringBuffer log = new StringBuffer("CheckPause ");
				log.append("[Pause:").append(vehicleData.getPauseType());
				log.append(" CurrNode:").append(vehicleData.getDriveCurrNode());
				log.append("], [ReqVehicle:").append(pauseRequestVehicle.getVehicleId());
				log.append("(").append(pauseRequestVehicle.getDriveCurrNode()).append(")");
				log.append(" Mode:").append(pauseRequestVehicle.getVehicleMode());
				log.append(" State:").append(pauseRequestVehicle.getState());
				log.append(" Pause:").append(pauseRequestVehicle.getPauseType()).append("]");
				
				// 2011.12.12 by MYM : Pause 3 추가로 전방 Vehicle 존재시 Resume 조건 제거
				if (pauseRequestVehicle.isAutoPositioning() == false) {
					// Pause 요청 호기가 Auto Position 중이 아닌 경우 Resume 명령 전송
					if (vehicleData.getPauseType() == 2) {
						log.append(", [SendResume(PauseType:").append(vehicleData.getPauseType()).append("), Reset]");
						sendResumeCommand();
					} else {
						log.append(", [Reset]");
					}
					vehicleData.setPauseRequestVehicle(null);
				} else if (vehicleData.hasArrivedAtStopNode()) {
					// 2011.12.12 by MYM : StopNode에 도착시 Resume 및 Reset
					if (vehicleData.getPauseType() == 2) {
						log.append(", [SendResume(PauseType:").append(vehicleData.getPauseType()).append("), Reset by arrived at stopNode]");
						sendResumeCommand();
					} else {
						log.append(", [Reset by arrived at stopNode]");
					}
					vehicleData.setPauseRequestVehicle(null);
				} else if (vehicleData.getPauseType() == 0 || vehicleData.getPauseType() == 1) {
    			// Pause 요청 호기가 Auto Position 중이고 대상 호기가 아직 Pause되지 않았으면 Pause 명령 전송
					sendPauseCommand();
					log.append(", [SendPause(PauseType:").append(vehicleData.getPauseType()).append(")]");
				} else {
					log.append(", [Pausing]");
				}
				if (vehicleData.getPauseReason().equals(log.toString()) == false) {
					traceOperation(log.toString());
					vehicleData.setPauseReason(log.toString());
				}
				return false;
			} 
			vehicleData.setPauseReason("");
			
//			else if (vehicleData.getPauseType() == 2) {
//				// Pause 요청이 없는 상태에서 Pausing이면 Resume 명령 전송
//				sendResumeCommand();
//				StringBuffer log = new StringBuffer("CheckPause ");
//				log.append("[Pause:").append(vehicleData.getPauseType());
//				log.append(" CurrNode:").append(vehicleData.getDriveCurrNode()).append("], SendResume");
//				if (vehicleData.getPauseReason().equals(log.toString()) == false) {
//					traceOperation(log.toString());
//					vehicleData.setPauseReason(log.toString());
//				}
//				return false;
//			} 
//			else if (vehicleData.getPauseType() == 1) {
//				// 대차 센서 감지시 전방 7초 or 2개의 노드 위치한 Vehicle에게 양보 요청함.
//				// ※ 전방 대차 센서가 감지가 되어도 주행명령은 전송
//				String yieldVehicleId = vehicleData.checkYieldRequestForForwardVehicleDetection(ocsInfoManager.getYieldRequestLimitTime());
//				if (yieldVehicleId.length() > 0) {
//					traceOperation("Yield Request(Forward vehicle) : " + yieldVehicleId);
//				}
//			}
//			vehicleData.setPauseReason("");
		}
		
		return true;
	}
	
	private void yieldRequestForPathDrive() {
//		if (isNearByDrive) {
			String yieldCheckResult;
			// 2012.01.19 by PMM
			// 대차 감지로 인한 전방 양보 요청은 Operation의 checkVehicleDetection()에서 수행.
			
//			if (vehicleData.getPauseType() == 1) {
//				// 2011.12.21 by MYM : TargetNode Drive 완료 조건 제거
//				// 배경 : 분기 후 ByPass로 주행할 Vehicle이 직진 전방 IDLE Vehicle이 존재할 경우 대차 감지하여 ByPass로 진입하지 않는 현상 발생하여 양보하도록 함
//				// 3. TargetNode까지 Drive(Go명령 전송)한 상태에서 대차 센서 감지시 전방 4초 or 2개의 노드 위치한 Vehicle에게 양보 요청함.
//				yieldVehicleId = vehicleData.checkYieldRequestForForwardVehicleDetection(ocsInfoManager.getYieldRequestLimitTime());
//				if (yieldVehicleId.length() > 0) {
//					traceOperation("Yield Request(Forward vehicle) : " + yieldVehicleId);
//				}
//			} else {
//				// CurrNode ~ StopNode 사이의 Vehicle에게 양보 요청
//				yieldVehicleId = vehicleData.checkYieldRequestForPathDrive(ocsInfoManager.getYieldRequestLimitTime());
//				if (yieldVehicleId.length() > 0) {
//					traceOperation("Yield Request(Forward vehicle) : " + yieldVehicleId);
//				}
//			}
			try {
				// CurrNode ~ StopNode 사이의 Vehicle에게 양보 요청
//				yieldCheckResult = vehicleData.checkYieldRequestForPathDrive(ocsInfoManager.getYieldRequestLimitTime());
				
				// 전방 YieldRequestLimitTime 이내 VHL에 양보 요청.
				yieldCheckResult = vehicleData.checkYieldRequestForPathDrive(ocsInfoManager.getYieldRequestLimitTime(), isNearByDrive);
				if (yieldCheckResult != null && yieldCheckResult.length() > 0) {
					traceOperation("Yield Request(Forward vehicle) : " + yieldCheckResult);
				}
			} catch (Exception e) {
				traceOperationException("GoMode - yieldRequestForPathDrive()", e);
			}
//		}
	}
	
	/**
	 * 2013.02.01 by KYK
	 * Target 에 도착한 경우 Vehicle 제어
	 * @return
	 */
	private boolean controlVehicleArrivedAtTarget(){
		assert hasArrivedAtTarget();
		
		if (trCmd == null) {
			// 2012.01.05 by PMM
			vehicleData.resetVehicleLocusList();
			operation.updateVehicleLocusToDB();
			
			// 2012.02.06 by PMM
			// 연속 양보로 인한 Park 기능 간섭 문제로 Park 요청받아 이동 중인 Vehicle은 YieldSearch 제외함.
			// 도착하면 reset해줘야 YieldSearch 진행함.
			vehicleData.setLocateRequested(false);
			
			// OperationMode(G->I) by NoJob
			changeOperationMode(OPERATION_MODE.IDLE, "NoJob");
		} else {
			// 2013.02.22 by KYK
//			if (isWorkNode(vehicleData.getTargetNode())) {
			if (isWorkStation()) {
				// WorkNode: SourceNode or DestNode
				// 2011.10.20 by PMM
				// VehicleArrived 보고 조건 수정
				if (trCmd.getPauseCount() == 0 && trCmd.isPause() == false) {
					if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
						switch (trCmd.getDetailState()) {
							case UNLOAD_ASSIGNED:
							case LOAD_ASSIGNED:
//							case STAGE_ASSIGNED:
							{
								sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ARRIVED, 0);
								break;
							}
							case STAGE_ASSIGNED:
							{
								changeOperationMode(OPERATION_MODE.IDLE, "STAGE - SourceNode Arrive");
								return true;
							}
							case STAGE_WAITING:
							{
								// 2011.10.20 by PMM
								// STAGE_WAITING 추가. -> Idle 모드로 전환.
								
								// OperationMode(G->I) by STAGE Wait.
								changeOperationMode(OPERATION_MODE.IDLE, "STAGE Wait - SourceNode Arrive");
								return true;
							}
							default:
								break;
						}
					} else {
						traceOperation("VehicleArrived Event is not reported. - CMD_ABORTED (TrCmdId: " + trCmd.getTrCmdId() + ") ");
						
						// 2012.01.11 by PMM
						changeOperationMode(OPERATION_MODE.IDLE, "WorkNode Arrived But Command Aborted");
						return true;
					}
				} else {
					traceOperation("VehicleArrived Event is not reported. - JobPause (Type:" + trCmd.getPauseType() + ", Count:" + trCmd.getPauseCount() + ")");
					
					// 2012.01.11 by PMM
					changeOperationMode(OPERATION_MODE.IDLE, "WorkNode Arrived But JobPaused");
					return true;
				}
				// 2021.04.02 by JDH : Transfer Premove 사양 추가
//				// OperationMode(G->W) by WorkNode Arrive.
//				changeOperationMode(OPERATION_MODE.WORK, "WorkNode Arrive");
				if(trCmd.getRemoteCmd()==TRCMD_REMOTECMD.PREMOVE) {
					// 2022.03.14 dahye : Premove Logic Improve
					//	1) DWT Logging
					//	2) State Change
//					changeOperationMode(OPERATION_MODE.IDLE,"WorkNode Arrive for PREMOVE");
//					// 21.09.03 dahye : DEST 지점 도착과 동시에 LOAD_WAITING 상태로 변경
//					trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_WAITING);
//					vehicleData.setPremoveWaitTime(System.currentTimeMillis());
					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ASSIGNED) {
						
						StringBuffer log = new StringBuffer("PREMOVE Arrived at DestPort. ");
						log.append("RemainingTime:").append(trCmd.getRemainingDuration());
						log.append("(DWT:").append(trCmd.getDeliveryWaitTimeOut()).append(")");
						traceOperation(log.toString());
						
						trCmd.setState(TRCMD_STATE.CMD_PREMOVE);
						
						trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_WAITING);	// 21.09.03 dahye : DEST 지점 도착과 동시에 LOAD_WAITING 상태로 변경
						changeOperationMode(OPERATION_MODE.IDLE,"WorkNode Arrive for PREMOVE");
						
						addVehicleToUpdateList();
						addTrCmdToStateUpdateList();
					} else if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_WAITING) {
						// 2022.05.05 dahye : 양보요청받아 도착 후 이동했다 다시 복귀하는 경우
						StringBuffer log = new StringBuffer("PREMOVE Arrived at DestPort. ");
						log.append("RemainingTime:").append(trCmd.getRemainingDuration());
						log.append("(DWT:").append(trCmd.getDeliveryWaitTimeOut()).append(")");
						traceOperation(log.toString());
						
						changeOperationMode(OPERATION_MODE.IDLE,"WorkNode Arrive for PREMOVE");
					} else {
						// OperationMode(G->W) by WorkNode Arrive.
						changeOperationMode(OPERATION_MODE.WORK, "WorkNode Arrive");
					}
				} else{
					// OperationMode(G->W) by WorkNode Arrive.
					changeOperationMode(OPERATION_MODE.WORK, "WorkNode Arrive");
				}
			} else {
				// TrPaused상태이거나 CMD_ABORTED상태인 작업을 들고 있는 경우만 가능하도록 조건추가
				// 작업이 있으면 이동이나 양보 이후에 작업위치로 이동하도록 처리
//				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
				// 2022.05.05 dahye : PREMOVE 양보요청 이후 다시 작업위치 복귀 필요. 일단 모든 경우에 대해 동작하도록 하자...
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER || (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE)) {
					if (trCmd.getCarrierLoc().equals(trCmd.getSourceLoc())) {
						// 2013.02.15 by KYK
						String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//						vehicleData.setTargetNode(trCmd.getSourceNode());
						vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
					} else {
						// 2013.02.15 by KYK
						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//						vehicleData.setTargetNode(trCmd.getDestNode());
						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
					}
					addVehicleToUpdateList();
				} else {
					// Tr이 Pause된 상태에서 양보를 하다가 Arrived되었을 때 RemoteCmd가 Transfer가 아닌 경우 같은데... 흠...
					// 여기가 Paused??
					// OperationMode(G->I) by Paused
					changeOperationMode(OPERATION_MODE.IDLE, "Paused");
				}
			}
		}
		return true;
	}
	
	/**
	 * 2013.02.01 by KYK
	 * Taget 에 Drive함, 그러나 아직 도착전 Vehicle 제어
	 * @return
	 */
	private boolean controlVehicleDrivedToButNotYetArrivedAtTarget(){
		assert hasDrivenToButNotYetArrivedAtTarget();
		if (getCommandState() == COMMAND_STATE.TIMEOUT) {
			if (trCmd == null) {
				// 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정 (TimeOut 처리)
				char eqOptionForGoMode = getEqOptionForDriveToPassDoor();
				
				if (eqOptionForGoMode == 'N' && vehicleData.isExistPortofPark()
						&& vehicleData.getStopNode().equals(vehicleData.getTargetNode())){
					eqOptionForGoMode = 'P';
				}
				
				// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] 
				Station station = stationManager.getStation(vehicleData.getTargetStation());		
				
				// 2013.02.22 by KYK : station, shift 옵션 추가
				// EQ Option (T:STOCKERPORT, E:EQPORT, S:STBPORT, U:UTBPORT, N:NONE)
				if (vehicleData.getCurrCmd() == 0) {
					// Next Command (N:Next Command, X:No use NextCmd)
					// 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정 (TimeOut 처리)
//					sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), 'N', 'X', null, 0);
					// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] - Station 사양 추가
//					sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X', null, 0);
					sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X', station, 0);
				} else {
					// 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정 (TimeOut 처리)
//					sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), 'N', null, 0);
					// 2021.06.08 by YSJ [OCS3.7_FAB-QRTAG 추가] - Station 사양 추가
//					sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, null, 0);
					sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, station, 0);
				}
			} else {
				switch (trCmd.getDetailState()) {
					// 2014.07.25 by MYM : UNLOADED 상태 추가
					// 배경 : UNLOADED -> HID Capa가 Limit Over인 경우 -> IDLE에서 EscapeSearch -> LoadSearch -> 목적지 도착 마지막 Go 명령 DatalogicError 시 재전송 안함.
				  //       상태가 LOAD_ASSGINED로 변경되지 않아서 재전송 못하는 현상 발생(M1B).
					case UNLOADED:
					case UNLOAD_ASSIGNED:
					case LOAD_ASSIGNED:
					case SCAN_ASSIGNED:
					case MAPMAKE_ASSIGNED:
					case MAPMAKING:
					case PATROL_ASSIGNED:
					case PATROLLING:
					case VIBRATION_MONITORING:
						// 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정 (TimeOut 처리)
						char eqOptionForGoMode = getEqOptionForDriveToPassDoor();
						if (eqOptionForGoMode == 'N') {
							eqOptionForGoMode = getEqOptionForGoMode();
						}
						
						if (eqOptionForGoMode == 'N' && vehicleData.isExistPortofPark()){
							eqOptionForGoMode = 'P';
						}
						
						// 2013.02.22 by KYK
						Station station = stationManager.getStation(vehicleData.getTargetStation());					
						int shiftPosition = getShiftPositionOfTarget(vehicleData.getTargetStation());
						if (vehicleData.getCurrCmd() == 0) {
							// 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정 (TimeOut 처리)
//							sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), getEqOptionForGoMode(), 'X', station, shiftPosition);
							sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X', station, shiftPosition);
						} else {
							// 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정 (TimeOut 처리)
//							sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), getEqOptionForGoMode(), station, shiftPosition);
							sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, station, shiftPosition);
						}
						break;
					case UNLOAD_SENT:
						if (checkDestPortDuplicate()) {
							controlDestPortDuplicated();
						} else if (checkNextCommandSendCondition()) {
							sendUnloadCommand(getVehicleCommCommand().getCommandId(), 'N');
						}
						break;
					case LOAD_SENT:
						if (checkNextCommandSendCondition()) {
							sendLoadCommand(getVehicleCommCommand().getCommandId(), 'N');
						}
						break;
					case SCAN_SENT:
						if (checkNextCommandSendCondition()) {
							sendScanCommand(getVehicleCommCommand().getCommandId(), 'N');
						}
						break;
					default:
						break;
				}
			}
		} else {
			if (trCmd == null) {
				// LocalOHT로 지정 이전에 Going 중, 지정되면 ComebackBayPathSearch가 늦어지는 케이스 발견됨.
				if (searchLocalVehicleComebackBayPath() == false) {
					// 반송명령 미수행 시(양보를 하는중) 목적지(TargetNode) 도착시까지 추가적인 양보 필요한지 체크
					searchVehicleYieldPath();
				}
			} else {
				if (checkDestPortDuplicate()) {
					// 1. 목적지(TargetNode) 도착시까지 Port Duplicate 발생 여부를 확인
					controlDestPortDuplicated();					
				} else if (checkNextCommandSendCondition()) {
					// 2. Next Unload/Load 전송 체크 후 Next Unload/Load Command 전송
					//    Next Command(N:Next Command, X:No use NextCmd)
					switch (trCmd.getDetailState()) {
						case UNLOAD_ASSIGNED:
							makeVehicleCommandId();
							sendUnloadCommand(getVehicleCommCommand().getCommandId(), 'N');
							trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_SENT);
							addTrCmdToStateUpdateList();
							break;
						case LOAD_ASSIGNED:
							makeVehicleCommandId();
							sendLoadCommand(getVehicleCommCommand().getCommandId(), 'N');
							trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_SENT);
							addTrCmdToStateUpdateList();
							break;
						case SCAN_ASSIGNED:
							makeVehicleCommandId();
							sendScanCommand(getVehicleCommCommand().getCommandId(), 'N');
							trCmd.setState(TRCMD_STATE.CMD_SCANNING);
							trCmd.setDetailState(TRCMD_DETAILSTATE.SCAN_SENT);
							addTrCmdToStateUpdateList();
							break;
						default:
							break;
					}
				} else {
					// wait for arriving
					; /*NULL*/
				}
			}
			
			// 근접제어시
			// 목적지(TargetNode) 도착시까지 CurrNode ~ StopNode(=TargetNode) 사이의 Vehicle에게 양보 요청 
//			checkYieldRequestForPathDrive();
			yieldRequestForPathDrive();
		}
		return false;
	}

	/**
	 * 2013.02.01 by KYK
	 * TargetNode까지 Drive하지 못한 경우에 대한 Vehicle 제어
	 * @return
	 */
	private boolean controlVehicleNotYetDrivedToTarget(){
		if (availableDriveVehiclePath()) {
			stopNode = driveVehiclePath();
			
			// 2013.02.22 by KYK
			if (isGoCommandSendable(stopNode)){
				String stopStation = "";
				if (stopNode.equals(vehicleData.getTargetNode()) && vehicleData.isStationDriveAllowed()) {
					stopStation = vehicleData.getTargetStation();
				}
				vehicleData.setStop(stopNode, stopStation);
				addVehicleToUpdateList();
				
				makeVehicleCommandId();
				// 2013.02.22 by KYK
				Station station = null;
				int shiftPosition = 0;
				char eqOptionForGoMode = 'N';
				
				if (isLastGoCommandToTarget()) {
					String targetStationId = vehicleData.getTargetStation();
					if (targetStationId != null && targetStationId.length() > 0) {
						station = stationManager.getStation(targetStationId);
						// 2013.07.03 by KYK
						if (station == null) {
							String log = "Station is null. Check StationInfo / stationId:" + targetStationId;
							traceOperationException(log);
						}
					}
					if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
						eqOptionForGoMode = getEqOptionForGoMode();
						shiftPosition = getShiftPositionOfTarget(targetStationId);
					}
				}
				
				// 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정
				// 2015.03.16 by zzang9un : PassDoor 관련 flag가 설정될 필요가 있는 경우에만 PassDoor flag 적용, 그 외 경우는 설정된대로 전송
				char tempEqOptionForGoMode = getEqOptionForDriveToPassDoor(); 
				if (tempEqOptionForGoMode != 'N') {
					eqOptionForGoMode = tempEqOptionForGoMode;
				}
				
				if (eqOptionForGoMode == 'N' && vehicleData.isExistPortofPark()
						&& vehicleData.getStopNode().equals(vehicleData.getTargetNode())){
					eqOptionForGoMode = 'P';
				}
								
				if (vehicleData.getCurrCmd() == 0) {
					sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X', station, shiftPosition);
				} else {
					sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, station, shiftPosition);
				}
			}
			// 근접제어시 양보요청
			yieldRequestForPathDrive();
		} else if (getCommandState() == COMMAND_STATE.TIMEOUT) {
			// 2013.02.22 by KYK
//			char eqOptionForGoMode;
//			if (trCmd != null &&
//					vehicleData.getTargetNode().equals(vehicleData.getStopNode())&&
//					TRCMD_REMOTECMD.TRANSFER.equals(trCmd.getRemoteCmd())) {
//			if (isLastGoCommandToTargetByTransfer()) {
//				// 주행명령을 작업위치인 곳으로 보내는 경우
//				eqOptionForGoMode = getEqOptionForGoMode();
//			} else {
//				// 주행명령을 작업위치가 아닌 곳으로 보내는 경우
//				eqOptionForGoMode = 'N';
//			}
			Station station = null;
			int shiftPosition = 0;
			char eqOptionForGoMode = 'N';
			
			if (isLastGoCommandToTarget()) {
				String targetStationId = vehicleData.getTargetStation();
				if (targetStationId != null && targetStationId.length() > 0) {
					station = stationManager.getStation(targetStationId);						
				}
				if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
					eqOptionForGoMode = getEqOptionForGoMode();
					shiftPosition = getShiftPositionOfTarget(targetStationId);
				}
			}
			
			// 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정
			// 2015.03.16 by zzang9un : PassDoor 관련 flag가 설정될 필요가 있는 경우에만 PassDoor flag 적용, 그 외 경우는 설정된대로 전송
			char tempEqOptionForGoMode = getEqOptionForDriveToPassDoor(); 
			if (tempEqOptionForGoMode != 'N') {
				eqOptionForGoMode = tempEqOptionForGoMode;
			}
			
			if (eqOptionForGoMode == 'N' && vehicleData.isExistPortofPark()){
				eqOptionForGoMode = 'P';
			}
			
			// 2013.02.01 by KYK
			if (vehicleData.getCurrCmd() == 0) {
				sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X', station, shiftPosition);
			} else {
				sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, station, shiftPosition);
			}
		} else {
			// Normal.
			; /*NULL*/
			// 2013.07.16 by KYK
			StringBuilder sb = new StringBuilder();
			sb.append("Not Available to drive Vehicle / ").append("CommandState:").append(getCommandState());
			traceOperation(sb.toString());
		}
		return false;
	}

	/**
	 * 2015.03.11 by zzang9un : PassDoor에서 출발/도착 시 FinalPortType 설정
	 * @return
	 */
	private char getEqOptionForDriveToPassDoor() {
		if (ocsInfoManager.isPassDoorControlUsage()) {
			PassDoor currPassDoor = null;
			PassDoor stopPassDoor = null;
			currPassDoor = vehicleData.getDriveCurrNode().getPassDoor();
			stopPassDoor = vehicleData.getDriveStopNode().getPassDoor();
		
			if ((currPassDoor != null) && currPassDoor.isEnabled() && (vehicleData.getCurrCmd() == 0)) { // 첫 Go 명령일때만 'O'를 전송
				// 출발 위치가 PassDoor인 경우 FinalPortType을 변경
				return 'O'; // 'O'(0x07)
			} else if (stopPassDoor != null && stopPassDoor.isEnabled()) {
				// 도착 위치가 PassDoor인 경우 FinalPortType을 변경
				return 'I'; // 'I'(0x06)
			}
		}
		return 'N';
	}
	
	/**
	 * 2013.02.22 by KYK
	 * @param targetStationId
	 * @return
	 */
	private int getShiftPositionOfTarget(String targetStationId) {

		int shiftPosition = 0;
		String carrierLocId = null;
		if (targetStationId == null || targetStationId.length() == 0) {
			return 0;
		}
		if (targetStationId.equals(getStationIdAtPort(trCmd.getSourceLoc()))) {
			carrierLocId = trCmd.getSourceLoc();
		} else if (targetStationId.equals(getStationIdAtPort(trCmd.getDestLoc()))) {
			carrierLocId = trCmd.getDestLoc();
		} else {
			return 0;
		}
		
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(carrierLocId);
		if (carrierLoc != null) {
			shiftPosition = carrierLoc.getShiftPosition();
		}		
		return shiftPosition;		
	}

	// 2013.02.01 by KYK
//	/**
//	 * '5-1. TargetNode에 도착한 경우'에 대한 Vehicle 제어
//	 * 
//	 * @author mokmin.park
//	 * @return boolean value.
//	 */
//	private boolean controlVehicleArrivedAtTargetNode(){
//		assert hasArrivedAtTargetNode();
//		
//		if (trCmd == null) {
//			// 2012.01.05 by PMM
//			vehicleData.resetVehicleLocusList();
//			
//			// 2012.02.06 by PMM
//			// 연속 양보로 인한 Park 기능 간섭 문제로 Park 요청받아 이동 중인 Vehicle은 YieldSearch 제외함.
//			// 도착하면 reset해줘야 YieldSearch 진행함.
//			vehicleData.setLocateRequested(false);
//			
//			// OperationMode(G->I) by NoJob
//			changeOperationMode(OPERATION_MODE.IDLE, "NoJob");
//		} else {
//			if (isWorkNode(vehicleData.getTargetNode())) {
//				// WorkNode: SourceNode or DestNode
//				// 2011.10.20 by PMM
//				// VehicleArrived 보고 조건 수정
//				if (trCmd.getPauseCount() == 0 && trCmd.isPause() == false) {
//					if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
//						switch (trCmd.getDetailState()) {
//						case UNLOAD_ASSIGNED:
//						case LOAD_ASSIGNED:
//						case STAGE_ASSIGNED:
//						{
//							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ARRIVED, 0);
//							break;
//						}
//						case STAGE_WAITING:
//						{
//							// 2011.10.20 by PMM
//							// STAGE_WAITING 추가. -> Idle 모드로 전환.
//							
//							// OperationMode(G->I) by STAGE Wait.
//							changeOperationMode(OPERATION_MODE.IDLE, "STAGE Wait - SourceNode Arrive");
//							return true;
//						}
//						default:
//							break;
//						}
//					} else {
//						traceOperation("VehicleArrived Event is not reported. - CMD_ABORTED (TrCmdId: " + trCmd.getTrCmdId() + ") ");
//						
//						// 2012.01.11 by PMM
//						changeOperationMode(OPERATION_MODE.IDLE, "WorkNode Arrived But Command Aborted");
//						return true;
//					}
//				} else {
//					traceOperation("VehicleArrived Event is not reported. - JobPause (Type:" + trCmd.getPauseType() + ", Count:" + trCmd.getPauseCount() + ")");
//					
//					// 2012.01.11 by PMM
//					changeOperationMode(OPERATION_MODE.IDLE, "WorkNode Arrived But JobPaused");
//					return true;
//				}
//				// OperationMode(G->W) by WorkNode Arrive.
//				changeOperationMode(OPERATION_MODE.WORK, "WorkNode Arrive");
//			} else {
//				// TrPaused상태이거나 CMD_ABORTED상태인 작업을 들고 있는 경우만 가능하도록 조건추가
//				// 작업이 있으면 이동이나 양보 이후에 작업위치로 이동하도록 처리
//				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
//					if (trCmd.getCarrierLoc().equals(trCmd.getSourceLoc())) {
//						vehicleData.setTargetNode(trCmd.getSourceNode());
//					} else {
//						vehicleData.setTargetNode(trCmd.getDestNode());
//					}
//					addVehicleToUpdateList();
//				} else {
//					// Tr이 Pause된 상태에서 양보를 하다가 Arrived되었을 때 RemoteCmd가 Transfer가 아닌 경우 같은데... 흠...
//					// 여기가 Paused??
//					// OperationMode(G->I) by Paused
//					changeOperationMode(OPERATION_MODE.IDLE, "Paused");
//				}
//			}
//		}
//		return true;
//	}
//	
////	private void checkYieldRequestForPathDrive() {
////		// 2011.11.15 by PMM
//////		if (ocsInfoManager.isNearByDrive()) {
////		if (isNearByDrive) {
////			String yieldVehicleId = vehicleData.checkYieldRequestForPathDrive(ocsInfoManager.getYieldRequestLimitTime());
////			if (yieldVehicleId.length() > 0) {
////				traceOperation("Yield Request(Forward vehicle) : " + yieldVehicleId);
////			}
////		}
////	}
//
//	/**
//	 * '5-2. TargetNode에 Drive만 하고, 도착 전'에 대한 Vehicle 제어
//	 * 
//	 * @author mokmin.park
//	 * @return boolean value.
//	 */
//	private boolean controlVehicleDrivedToButNotYetArrivedAtTargetNode(){
//		assert hasDrivenToButNotYetArrivedAtTargetNode();
//		if (getCommandState() == COMMAND_STATE.TIMEOUT) {
//			if (trCmd == null) {
//				if (vehicleData.getCurrCmd() == 0) {
//					// EQ Option for Go Mode (T:STOCKERPORT, E:EQPORT, S:STBPORT, U:UTBPORT, N:NONE)
//					// Next Command (N:Next Command, X:No use NextCmd)
//					sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), 'N', 'X');
//				} else {
//					// 2012.12.04 by MYM : PatrolVehicle 대응 - 주행하면서 Patrolling이면 'C' 상태를 보고함.
//					// 2012.11.01 by MYM : CurrCmd가 0이 아닌 경우에 Vehicle State에 따라 Go or GoMore 전송
////					sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), 'N');
//					if (vehicleData.getState() == 'G' || vehicleData.getState() == 'C') {
//						sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), 'N');
//					} else {
//						traceOperationException("Abnormal Case: GoMode#001");
//						sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), 'N', 'X');
//					}
//				}
//			} else {
//				switch (trCmd.getDetailState()) {
//				case UNLOAD_ASSIGNED:
//				case LOAD_ASSIGNED:
//				case SCAN_ASSIGNED:
//				case MAPMAKE_ASSIGNED:
//				case MAPMAKING:
//				case PATROL_ASSIGNED:
//				case PATROLLING:
//					if (vehicleData.getCurrCmd() == 0) {
//						sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), getEqOptionForGoMode(), 'X');
//					} else {
//						// 2012.12.04 by MYM : PatrolVehicle 대응 - 주행하면서 Patrolling이면 'C' 상태를 보고함.
//						// 2012.11.01 by MYM : CurrCmd가 0이 아닌 경우에 Vehicle State에 따라 Go or GoMore 전송
////							sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), getEqOptionForGoMode());
//						if (vehicleData.getState() == 'G' || vehicleData.getState() == 'C') {
//							sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), getEqOptionForGoMode());
//						} else {
//							traceOperationException("Abnormal Case: GoMode#002");
//							sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), getEqOptionForGoMode(), 'X');
//						}						
//					}
//					break;
//				case UNLOAD_SENT:
//					if (checkDestPortDuplicate()) {
//						controlDestPortDuplicated();
//					} else if (checkNextCommandSendCondition()) {
//						sendUnloadCommand(getVehicleCommCommand().getCommandId(), vehicleData.getTargetNode(), 'N');
//					}
//					break;
//				case LOAD_SENT:
//					if (checkNextCommandSendCondition()) {
//						sendLoadCommand(getVehicleCommCommand().getCommandId(), vehicleData.getTargetNode(), 'N');
//					}
//					break;
//				case SCAN_SENT:
//					if (checkNextCommandSendCondition()) {
//						sendScanCommand(getVehicleCommCommand().getCommandId(), vehicleData.getTargetNode(), 'N');
//					}
//					break;
//				default:
//					break;
//				}
//			}
//		} else {
//			if (trCmd == null) {
//				// LocalOHT로 지정 이전에 Going 중, 지정되면 ComebackBayPathSearch가 늦어지는 케이스 발견됨.
//				if (searchLocalVehicleComebackBayPath() == false) {
//					// 반송명령 미수행 시(양보를 하는중) 목적지(TargetNode) 도착시까지 추가적인 양보 필요한지 체크
//					searchVehicleYieldPath();
//				}
//			} else {
//				if (checkDestPortDuplicate()) {
//					// 1. 목적지(TargetNode) 도착시까지 Port Duplicate 발생 여부를 확인
//					controlDestPortDuplicated();					
//				} else if (checkNextCommandSendCondition()) {
//					// 2. Next Unload/Load 전송 체크 후 Next Unload/Load Command 전송
//					//    Next Command(N:Next Command, X:No use NextCmd)
//					switch (trCmd.getDetailState()) {
//					case UNLOAD_ASSIGNED:
//						makeVehicleCommandId();
//						sendUnloadCommand(getVehicleCommCommand().getCommandId(), vehicleData.getTargetNode(), 'N');
//						trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
//						trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_SENT);
//						addTrCmdToStateUpdateList();
//						break;
//					case LOAD_ASSIGNED:
//						makeVehicleCommandId();
//						sendLoadCommand(getVehicleCommCommand().getCommandId(), vehicleData.getTargetNode(), 'N');
//						trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_SENT);
//						addTrCmdToStateUpdateList();
//						break;
//					case SCAN_ASSIGNED:
//						makeVehicleCommandId();
//						sendScanCommand(getVehicleCommCommand().getCommandId(), vehicleData.getTargetNode(), 'N');
//						trCmd.setState(TRCMD_STATE.CMD_SCANNING);
//						trCmd.setDetailState(TRCMD_DETAILSTATE.SCAN_SENT);
//						addTrCmdToStateUpdateList();
//						break;
//					default:
//						break;
//					}
//				} else {
//					// wait for arriving
//					; /*NULL*/
//				}
//			}
//			
//			// 근접제어시
//			// 목적지(TargetNode) 도착시까지 CurrNode ~ StopNode(=TargetNode) 사이의 Vehicle에게 양보 요청 
////			checkYieldRequestForPathDrive();
//			yieldRequestForPathDrive();
//		}
//		return false;
//	}
//	
//	
//	/**
//	 * '5-3. TargetNode까지 Drive하지 못한 경우'에 대한 Vehicle 제어
//	 * 
//	 * @author mokmin.park
//	 * @return boolean value.
//	 */
//	private boolean controlVehicleNotYetDrivedToTargetNode(){
//		if (availableDriveVehiclePath()) {
//			stopNode = driveVehiclePath();
//			
//			// 주행명령을 보낼수 있는 Node가 있는 경우에 GoCmd 생성 후 전송
//			if (vehicleData.getStopNode().equals(stopNode) == false && (stopNode != null && stopNode.length() > 0)) {
//				vehicleData.setStopNode(stopNode);
//				addVehicleToUpdateList();
//				
//				makeVehicleCommandId();
//				
//				// 2012.11.01 by MYM : CurrCmd가 0이 아닌 경우에 Vehicle State에 따라 Go or GoMore 전송
//				//                     주행명령을 작업위치 or 아닌 곳으로 보내는 경우의 코드를 아래와 같이 정리
//				char eqOptionForGoMode;
//				if (trCmd != null &&
//						trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER &&
//						vehicleData.getTargetNode().equals(vehicleData.getStopNode())) {
//					// 주행명령을 작업위치인 곳으로 보내는 경우
//					eqOptionForGoMode = getEqOptionForGoMode();
//				} else {
//					// 주행명령을 작업위치가 아닌 곳으로 보내는 경우
//					eqOptionForGoMode = 'N';
//				}
//				if (vehicleData.getCurrCmd() == 0) {
//					sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X');
//				} else {
					// 2012.12.04 by MYM : PatrolVehicle 대응 - 주행하면서 Patrolling이면 'C' 상태를 보고함.
//					if (vehicleData.getState() == 'G' || vehicleData.getState() == 'C') {
//						sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode);
//					} else {
//						traceOperationException("Abnormal Case: GoMode#003");
//						sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X');
//					}
//				}
//			}
//			
//			// 근접제어시
//			// 목적지(TargetNode)까지 StopNode를 전송하지 않은 경우 아직 주행중인경우 
//			// CurrNode ~ StopNode(!=TargetNode) 사이의 Vehicle에게 양보 요청
////			checkYieldRequestForPathDrive();
//			
//			yieldRequestForPathDrive();
//		} else if (getCommandState() == COMMAND_STATE.TIMEOUT) {
//			// 2012.11.01 by MYM : CurrCmd가 0이 아닌 경우에 Vehicle State에 따라 Go or GoMore 전송
//			//                     주행명령을 작업위치 or 아닌 곳으로 보내는 경우의 코드를 아래와 같이 정리
//			char eqOptionForGoMode;
//			if (trCmd != null &&
//					vehicleData.getTargetNode().equals(vehicleData.getStopNode())&&
//					TRCMD_REMOTECMD.TRANSFER.equals(trCmd.getRemoteCmd())) {
//				// 주행명령을 작업위치인 곳으로 보내는 경우
//				eqOptionForGoMode = getEqOptionForGoMode();
//			} else {
//				// 주행명령을 작업위치가 아닌 곳으로 보내는 경우
//				eqOptionForGoMode = 'N';
//			}
//			if (vehicleData.getCurrCmd() == 0) {
//				sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X');
//			} else {
				// 2012.12.04 by MYM : PatrolVehicle 대응 - 주행하면서 Patrolling이면 'C' 상태를 보고함.
//				if (vehicleData.getState() == 'G' || vehicleData.getState() == 'C') {
//					sendGoMoreCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode);
//				} else {
//					traceOperationException("Abnormal Case: GoMode#004");
//					sendGoCommand(getVehicleCommCommand().getCommandId(), vehicleData.getStopNode(), eqOptionForGoMode, 'X');
//				}
//			}
//		} else {
//			// Normal.
//			; /*NULL*/
//		}
//		return false;
//	}
	
	/**
	 * Next Command를 전송할 수 있는 상태인지에 대한 조건 판단
	 * 
	 * @author mokmin.park
	 * @return Next Command 전송 가능 여부(boolean).
	 */
	private boolean checkNextCommandSendCondition() {
		/**
		 * Step0. Next 명령 전송 기능 사용 유무 체크
		 */
		if (ocsInfoManager.isNextCommandUsed() == false) {
			return false;
		}

		/**
		 * Step1. 반송명령에 대한 상태가 다음의 조건인 경우 Next 명령 전송 불가
		 *  1. TrCmdID       : 존재 無
		 *  2. JobPause      : TRUE
		 *  3. ActionHold    : TRUE
		 *  4. LoadingByPass : TRUE
		 *  5. RemoteCmd     : TRANSFER 및 SCAN이 아닌 경우(ABORT)
		 */
		if (trCmd == null) {
			return false;
		}

		if (trCmd.isPause()) {
			return false;
		}

		if (vehicleData.isActionHold()) {
			return false;
		}
		
		if (isLoadingByPass()) {
			return false;
		}

		// STAGE, MAPMAKE, PATROL는 어떻게??
		if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.TRANSFER && trCmd.getRemoteCmd() != TRCMD_REMOTECMD.SCAN) {
			return false;
		}

		/**
		 * Step2. CommandID에 대한 상태가 다음의 조건인 경우 Next 명령 전송 불가
		 *  1. m_nNextCmd  : 0이 아닌 경우(0이 아닌경우 이미 전송된 경우임.)
		 *  2. m_nVehCmdID : OCS가 전송한 CmdID와 VHL로부터 Reply받은 CmdID가 다를 때(Reply 응답이 없을 때 Next 명령 전송하지 않도록)
		 */
		if (vehicleData.getNextCmd() != 0) {
			return false;
		}
		
		if (getVehicleCommCommand().getCommandId() != getVehicleComm().getVehicleCommData().getCommandId()) {
			return false;
		}

		/**
		 * Step3. 기타 다음의 조건인 경우 Next 명령 전송 불가
		 *  1. m_bAVRetryWait : true(AVRetry TimouOut을 기다리는 경우)
		 *  2. 작업노드 위치가 아닌 경우
		 *     - ResetTargetNode가 호출되는 경우(Abort, ParkNode 등) TargetNode를 StopNode로 Update를 하게 됨.
		 *       그래서 작업노드 위치를 확인해야 함.
		 */
		if (vehicleData.isAvRetryWait()) {
			return false;
		}
		
		if (isWorkNode(vehicleData.getStopNode()) == false) {
			return false;
		}

		/**
		 * Step4. CmdState 상태 체크
		 * 2008.10.30 :
		 *   현상 : Vehicle 멈춤 현상 발생
		 *   원인 : GoMore에 대한 응답이 'D'(DataLogic Error)로 올라 왔을 때
		 *          조건을 체크하지 않고 Next Unload/Load 명령을 전송
		 *          이에따라 Vehicle은 Next Unload/Load 명령에 대해서 'D'로 보고
		 *          'D' 응답에 대해 TimeOut으로 처리하여 Next Unload/Load를 계속 보냄.(반복)
		 *   개선 : cmdState가 'S'(Sent), 'F'(D,E,P로 올라온 경우), 'T'(TimeOut), 't' (Wait for Response) 일 경우는
		 *          Next Unload/Load 명령 전송하지 않도록 조건 추가
		 */
		switch (getCommandState()) {
			case SENT:
			case UNKNOWN:
			case TIMEOUT:
			case WAITFORRESPONSE:
				return false;
			default:
				return true;
		}
	}
	
	/**
	 * 'Go Mode에서 DestPort에 UNLOAD 작업이 있는 경우'에 대한 Vehicle 제어
	 * 
	 * @author mokmin.park
	 */
	private void controlDestPortDuplicated() {
		assert hasDrivenToButNotYetArrivedAtTarget(); // 2013.02.01 by KYK
		assert checkDestPortDuplicate();
		
		pauseTrCmd(true, TrCmdConstant.PORT_DUPLICATE, 0);
		TrCmd duplicatedTrCmd = getUnloadTrCmdExistOnDestPort(trCmd.getDestLoc());
		
		StringBuilder message = new StringBuilder();
		message.append("[Load Job] Vehicle:").append(vehicleData.getVehicleId());
		message.append(", TrCmdId:").append(trCmd.getTrCmdId());
		message.append(", CarrierId:").append(trCmd.getCarrierId());
		message.append(", SourceLoc:").append(trCmd.getSourceLoc());
		message.append(", DestLoc:").append(trCmd.getDestLoc());
		message.append(", TrQueuedTime:").append(trCmd.getTrQueuedTime());
		
		if (duplicatedTrCmd != null) {
			message.append(", [Unload Job] Vehicle:").append(duplicatedTrCmd.getVehicle());
			message.append(", TrCmdId:").append(duplicatedTrCmd.getTrCmdId());
			message.append(", CarrierId:").append(duplicatedTrCmd.getCarrierId());
			message.append(", SourceLoc:").append(duplicatedTrCmd.getSourceLoc());
			message.append(", DestLoc:").append(duplicatedTrCmd.getDestLoc());
			message.append(", TrQueuedTime:").append(duplicatedTrCmd.getTrQueuedTime());
		} else {
			message.append(", [Unload Job] UNKNOWN.");
		}
		traceOperation(message.toString());
		
		if (isPortDuplicateEventRegistered == false) {
			registerEventHistory(new EventHistory(EVENTHISTORY_NAME.PORT_DUPLICATE, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
					"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.PORT_DUPLICATE), true);
			isPortDuplicateEventRegistered = true;
		}
	}
	
	/**
	 * Go/GoMore Command 전송 시 필요한 CarrierLoc의 Type별 EqOption을 반환
	 * 
	 * @author mokmin.park
	 * @return EqOption for GoMode. (char)
	 * 				'S' for STBPORT.
	 * 				'E' for EQPORT.
	 * 				'U' for UTBPORT.
	 * 				'T' for STOCKERPORT.
	 * 				'L' for LOADERPORT.
	 * 				'N' for else.
	 * @see OcsConstant.CARRIERLOC_TYPE.toEqOptionForGoMode()
	 */
	private char getEqOptionForGoMode() {
		// 2012.01.06 by PMM
//		if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
//			return getCarrierLocType(trCmd.getDestLoc()).toEqOptionForGoMode();
//		} else {
//			return getCarrierLocType(trCmd.getSourceLoc()).toEqOptionForGoMode();
//		}
		if (isLoadingByPass()) {
			return 'N';
		}
		if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
			return getCarrierLocType(trCmd.getDestLoc()).toEqOptionForGoMode();
		} else if (vehicleData.getTargetNode().equals(trCmd.getSourceNode())) {
			return getCarrierLocType(trCmd.getSourceLoc()).toEqOptionForGoMode();
		} else {
			return 'N';
		}
	}
	
	/**
	 * GoMode에서 Alarm 메시지 삭제 처리
	 * 
	 * @author mokmin.park
	 */
	private void unregisterAlarmInGoMode() {
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
	}
}
