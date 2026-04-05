package com.samsung.ocs.operation.mode;

import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;
import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.TrCmdConstant;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.manager.impl.model.EventHistory;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.operation.Operation;
import com.samsung.ocs.operation.constant.MessageItem.EVENT_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant;
import com.samsung.ocs.operation.constant.OperationConstant.OPERATION_MODE;
import com.samsung.ocs.operation.constant.ResultCode;

/**
 * IdleMode Class, OCS 3.0 for Unified FAB
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

public class IdleMode extends OperationModeImpl {
	/**
	 * Constructor of IdleMode class.
	 */
	public IdleMode(Operation operation) {
		super(operation);
	}
	
	@Override
	public OPERATION_MODE getOperationMode() {
		return OPERATION_MODE.IDLE;
	}
	
	/**
	 * Control Vehicle in IdleMode
	 */
	@Override
	public boolean controlVehicle(){
		trCmd = getTrCmd();
		
		/**
		 * Step1. SLEEP 조건  체크.
		 */
		if (checkSleepMode()) {
			return false;
		}
		
		// 2012.01.19 by PMM
		/**
		 * Step2. Vehicle 위치 체크.
		 */
		if (checkVehicleLocation()) {
			return false;
		}
		
		/**
		 * Step3. Vehicle ActionHold 체크.
		 */
		if (vehicleData.isActionHold())
			return true;
		
		/**
		 * Step4. CarrierExist 체크.
		 */
		if (checkAbnormalCarrierStatus()) {
			return false;
		}
		
		/**
		 * Step5. Alarm 해제.
		 */
		if (isAlarmRegistered()) {
			unregisterAlarmInIdleMode();
//			ungisterAlarmInIdleMode(); // 2012.12.03 by KYK : 오타수정
		}
		
		if (searchEscapeForAbnormalHid()) {
			return true;
		}
		
		// 2012.01.03 by PMM
		if (trCmd == null || trCmd.isPause() == false) {
			isPortDuplicateClearEventRegistered = false;
			isPortDuplicateEventRegistered = false;
		}
		
		// 2012.02.06 by PMM
		// Park 요청받았던 호기에 대한  reset.
		// 도착, Manual->Auto 전환 호기는 모두 reset 되도록 하기 위해 IdleMode에서 reset.
		// reset이 제대로 안되면 YieldSearch 진행 안 됨.
		resetLocateRequest();
		
		if (vehicleData.getYieldState() == 'Y') {
			vehicleData.setYieldState('N');
			vehicleData.setDriveFailedNode(null);
		}
		
		/**
		 * Step6. Vehicle 제어
		 */
		// 6-1. TrCmd가 없는 경우
		if (trCmd == null) {
			return controlVehicleWithNoCommand();
		} else {
		// 6-2. TrCmd가 있는 경우
			// 6-2-1. TrCmd Paused가 아닌 경우
			if (trCmd.isPause() == false) {
				switch (trCmd.getState()) {
					case CMD_QUEUED:
						// 6-2-1-1. TrCmd State: CMD_QUEUED
						return controlVehicleWithQueuedCommand();
						
					case CMD_WAITING:
						// 6-2-1-2. TrCmd State: CMD_WAITING
						return controlVehicleWithWaitingCommand();
					
					case CMD_TRANSFERRING:
						// 6-2-1-3. TrCmd State: CMD_TRANSFERRING
						return controlVehicleWithTransferringCommand();
						
					case CMD_ABORTING:
					case CMD_ABORTED:
						// 6-2-1-4. TrCmd State: CMD_ABORTING/CMD_ABORTED 
						return controlVehicleWithAbortCommand();
					
					case CMD_STAGING:
						// 6-2-1-5. TrCmd State: CMD_STAGING
						return controlVehicleWithStagingCommand();
						
					case CMD_MAPMAKING:
						// 6-2-1-6. TrCmd State: CMD_MAPMAKING 
						return controlVehicleWithMapMakingCommand();
						
					case CMD_PATROLLING:
						// 6-2-1-7. TrCmd State: CMD_PATROLLING 
						return controlVehicleWithPatrollingCommand();
						
					case CMD_MONITORING:
						// 6-2-1-8. TrCmd State: CMD_MONITORING 
						return controlVehicleWithMonitoringCommand();
					
					case CMD_PREMOVE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
						// 6-2-1-9. TrCmd State : CMD_PREMOVE
						return controlVehicleWithPremoveCommand();
						
					case CMD_COMPLETED:
						deleteTrCmdFromDB();
						break;
						
					// 2012.03.16 by PMM
					// CMD_PAUSED인 상태로 재시작 시, trCmd.isPause()가 true가 아닌 케이스에 대한 조치
					case CMD_PAUSED:
						pauseTrCmd(true, trCmd.getPauseType(), trCmd.getPauseCount());
						traceOperationException("Abnormal Case: IdleMode#001-2");
						return controlVehicleWithPausedCommand();
					
					case CMD_SCANNING:
					// case CMD_PAUSED:
					case CMD_CANCELLING:
					case CMD_CANCELED:
					default:
						// 6-2-1-8. Abnormal Case.
						traceOperation("Abnormal Case. (TrCmdId:" + trCmd.getTrCmdId() + ", State:" + trCmd.getState() + ")");
						// IdleMode#001
						traceOperationException("Abnormal Case: IdleMode#001");
						break;
				}
			} else {
			// 6-2-2. TrCmd Paused인 경우
				return controlVehicleWithPausedCommand();
			}
		}
		return false;
	}
	
	// 2012.01.19 by PMM
	private boolean checkVehicleLocation() {
//		if (vehicleData.getCurrCmd() != 0)
		// Failover 시, Idle이 DefaultMode임. (WorkMode에서 Failover 시 처리 문제)
		
		// 2011.10.27 by PMM
		// VehicleData.getSourceNode가 실제 DriveStopNode와 다를 경우
		Node stopNode = vehicleData.getDriveStopNode();
		if (stopNode != null) {
			if (vehicleData.getStopNode().equals(stopNode.getNodeId()) == false) {
				traceOperation("Abnormal Case. StopNode:" + vehicleData.getStopNode() + " is updated to DriveStopNode:" + stopNode.getNodeId());
				// 2013.02.15 by KYK : ??
//				vehicleData.setStopNode(stopNode.getNodeId());
				vehicleData.setStop(stopNode.getNodeId(), "");
				addVehicleToUpdateList();
			}
		}
//		if (vehicleData.getCurrNode().equals(vehicleData.getStopNode()) == false) {
			// 2011.10.26 by PMM
			// 2011.10.27 by PMM
			// NoTrCmd 상태에서 양보 중 (G) 작업 할당받아 (I)로 전환 시 여기로 들어옴.
			// 아래 코드로 StopNode가 잘못 reset되고 MOVE PathSearch가 일어나서 탈선이 일어남.
			// 도착 시까지 return해서 Waiting??
			
//			if (vehicleData.getState() == 'A' || vehicleData.getState() == 'I') {
//				vehicleData.setStopNode(vehicleData.getCurrNode());
//				vehicleData.resetDriveNodeList(getOCSInfoManager().isNearByDrive());
//				addVehicleToUpdateList();
////				if (vehicleData.getRoutedNodeCount() == 0) {
////					return searchVehiclePath(vehicleData.getTargetNode(), TrCmdConstant.MOVE, false);
////				}
//			}
//			changeOperationMode(OPERATION_MODE.GO, "Not Arrived");
//		}
		return false;
	}
	
	/**
	 * 작업이 없는 Vehicle 제어 (Idle Mode)
	 * 
	 * @author mokmin.park
	 * @return 정상 or 비정상 상태 (boolean).
	 */
	private boolean controlVehicleWithNoCommand() {
		// 2013.02.01 by KYK
		if (hasDrivenToTarget()) {
			// 2014.02.18 by MYM : [Stage Locate 기능] Stage로 위치한 경우 대기
			if (vehicleData.getRequestedType() == REQUESTEDTYPE.STAGEWAIT) {
				if (vehicleData.getStageWaitTime() > 0) {
					return true;
				} else {
					resetStageRequest("StageWait Timeout");
				}
			} else if (vehicleData.getRequestedType() == REQUESTEDTYPE.STAGENOBLOCK) {
				if (vehicleData.getStageWaitTime() <= 0) {
					resetStageRequest("StageWait Timeout");
				}
			}
			
			// 2011.10.12 by PMM
			// 위치 이동.
//			if (searchEscapeForAbnormalHid()) {
//				return true;
//			}
			if (searchLocalVehicleComebackBayPath()) {
				return true;
			}
			
			if (searchVehicleComebackZonePath()) {
				return true;
			}
			
			if (searchVehicleYieldPath()) {
				return true;
			}
			
			// 근접제어인 경우 합류 이전 노드(DrivingQueue의 첫번째 Node) 및 분기 노드에 있을 때
			// 후방의 Vehicle이 위의 노드까지 Drive를 하다가 FailOver한 경우
			// 후방 Vehicle의 OperationControlMode가 READY인 경우는 양보를 하도록 함.
			
			// 2011.11.15 by PMM
//			if (ocsInfoManager.isNearByDrive() && vehicleData.checkYieldForBackwardVehicle()) {
			
			// 2012.01.19 by PMM
			// Exception 처리 추가
			try {
				// 2012.01.26 by PMM
//				if (isNearByDrive && vehicleData.checkYieldForBackwardVehicle()) { 
//					if (searchVehicleYieldPath()) {
//						return true;
//					}
//				}
				if (isNearByDrive) {
					if (isFailoverCompleted() == false &&
							vehicleData.checkYieldForBackwardVehicle()) {
						if (searchVehicleYieldPath()) {
							return true;
						}
					}
				}
				
				// 2012.04.04 by PMM
				// 비근접제어에서도 Target에 도착하지 않았는데 IdleMode여서 정리가 안된 케이스 발생함.
				// GoMore Command가 Timeout 처리되어야 하는데, IdleMode여서 안됨.
//				00:22:19:297 OHT086> [G] Mode:A Status:G Node(57031,57037,57037) Carrier:0 CmdStatus:S(P:2 C:3 N:0 V:4) Error:0 LocalGroup:
//				00:22:19:706 OHT086> UpdateRequestedCmd : TRANSFER_RESET(TRANSFER)                                                         
//				00:22:19:710 OHT086> Job Cancel: 15422784                                                                                  
//				00:22:20:940 OHT086> [G] Mode:A Status:G Node(57031,57037,57037) Carrier:0 CmdStatus:S(P:2 C:3 N:0 V:4) Error:0 LocalGroup:
//				00:22:20:941 OHT086> STATUS(I) by TRANSFER                                                                                 
//				00:22:20:941 OHT086> JobAssign 15422803 From PTAA09_B2 To ELOA09_B3                                                        
//				00:22:21:555 OHT086> [I] Mode:A Status:G Node(57032,57037,57037) Carrier:0 CmdStatus:S(P:2 C:3 N:0 V:4) Error:0 LocalGroup:
//				00:22:21:968 OHT086> [I] Mode:A Status:A Node(57032,57037,57037) Carrier:0 CmdStatus:S(P:3 C:0 N:0 V:4) Error:0 LocalGroup:

				if (vehicleData.getState() == 'G' || 
						(vehicleData.getState() == 'A' && hasArrivedAtStopNode() == false)) {
					changeOperationMode(OPERATION_MODE.GO, "Not Yet Arrived at TargetNode.");
					return false;
				}
			} catch (Exception e) {
				traceOperationException("IdleMode - controlVehicleWithNoCommand()", e);
			}
		} else {
			// 2011.10.12 by PMM. User Takeover 시, MOVE VHL을 TargetNode까지 보내기 위해 아래 Comment 해제.
			return searchVehiclePath(vehicleData.getTargetNode(), TrCmdConstant.MOVE, false);
		}
		return false;
	}
	
	/**
	 * STATE가 CMD_QUEUED인 경우에 대한 Vehicle 제어 (Idle Mode)
	 * 
	 * @author mokmin.park
	 * @return 정상 or 비정상 상태 (boolean).
	 */
	private boolean controlVehicleWithQueuedCommand() {
		if (trCmd != null) {
			if (trCmd.getState() == TRCMD_STATE.CMD_QUEUED && trCmd.isPause() == false) {
				switch (trCmd.getRemoteCmd()) {
					case TRANSFER: {
						trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_INITIATED, 0);
						break;
					}
					case SCAN: {
						trCmd.setDetailState(TRCMD_DETAILSTATE.SCAN_ASSIGNED);
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCAN_INITIATED, 0);
						break;
					}
					case STAGE: {
						trCmd.setDetailState(TRCMD_DETAILSTATE.STAGE_ASSIGNED);
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.STAGE_INITIATED, 0);
						break;
					}
					case MAPMAKE: {
						trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMAKE_ASSIGNED);
						// OCSRegistered TrCmd의 경우에는 MCS에 Report하지 않음.
						break;
					}
					case PATROL: {
						trCmd.setDetailState(TRCMD_DETAILSTATE.PATROL_ASSIGNED);
						// OCSRegistered TrCmd의 경우에는 MCS에 Report하지 않음.
						break;
					}
					case VIBRATION: {
						trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
						// OCSRegistered TrCmd의 경우에는 MCS에 Report하지 않음.
						break;
					}
					default:
						break;
				}
				// RemoteCmd 별로 STATE를 CMD_WAITING으로 전환.
				// 2013.02.15 by KYK
				String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//				vehicleData.setTargetNode(trCmd.getSourceNode());
				vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
				addVehicleToUpdateList();
				trCmd.setCarrierLoc(trCmd.getSourceLoc());
				trCmd.setState(TRCMD_STATE.CMD_WAITING);
				addTrCmdToStateUpdateList();
				
				// 2012.03.20 by PMM
				traceOperation("State of TrCmd is updated as CMD_WAITING in IdleMode.");
				
				if ((trCmd.getVehicle() == null || trCmd.getVehicle().length() == 0)) {
					trCmd.setVehicle(vehicleData.getVehicleId());
					addTrCmdToVehicleUpdateList();
				}
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ASSIGNED, 0);
				// 2013.02.01 by KYK
//				if (hasArrivedAtSourceNode()) {
				if (hasArrivedAtSource()) {
					return controlVehicleWithWaitingCommand();
				}
			} else {
				traceOperationException("controlVehicleWithQueuedCommand()");
			}
		} else {
			traceOperationException("controlVehicleWithQueuedCommand() - trCmd is null.");
		}
		return true;
	}
	
	/**
	 * RemoteCmd가 TRANSFER이고, STATE가 CMD_WAITING경우에 대한 Vehicle 제어 (Idle Mode)
	 * 
	 * @author mokmin.park
	 * @return 정상 or 비정상 상태 (boolean).
	 */
	private boolean controlVehicleWithWaitingCommand() {
		assert (trCmd != null);
		assert (trCmd.isPause() == false) && (trCmd.getState() == TRCMD_STATE.CMD_WAITING);
		
		// CMD_WAITING 상태에서는 TargetNode가 SourceNode.
		// 2013.02.01 by KYK
//		if (hasArrivedAtSourceNode()) {
		if (hasArrivedAtSource()) {
//			if (hasArrivedAtTargetPort(trCmd.getSourceLoc()) == false) {
//				if (hasPassedbyTargetPort(trCmd.getSourceLoc())) {
//					// 동일노드 후방포인트로 이동 위해 RouteSearch
//					return searchVehiclePath(trCmd.getSourceNode(), TrCmdConstant.UNLOAD, false);
//				} else {
//					// 동일노드 전방포인트로 이동
//					changeOperationMode(OPERATION_MODE.GO, "Go To Point At Same Node(SourceLoc)");
//					return true;
//				}
//			}
			
			// 근접 제어 시, CurrNode를 SourceNode로 업데이트하더라도 실제 도착 (Auto Arrived)이 아닐 수 있음.
			// WorkMode로 전환 전에  vehicleData.getState() != 'G' 조건이 필요할 수 있음.
			// 그러나 IdleMode -> WorkMode이고, Mode에 상관없이 대차 감지 시 전방 양보 요청함. 
			switch (trCmd.getDetailState()) {
				case UNLOAD_ASSIGNED:
				case STAGE_ASSIGNED:
				{
					// 현재 위치가 TargetNode(SourceNode)인 경우.
					// JobAssign을 받을 당시 CurrNode == SourceNode 인 경우.
					// Idle -> Go -> Work 일 경우, Go Mode에서 도착 시 보고.
					if (trCmd.getPauseCount() == 0 && trCmd.isPause() == false) {
						sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ARRIVED, 0);
					} else {
						traceOperation("VehicleArrived Event is not reported by JobPause(" + trCmd.getPausedTime() + ", " + trCmd.getPauseCount() + ", " + trCmd.getPauseCount() + ")");
					}
					
					// OperationMode(I->W) by SourceNode Arrive.
					changeOperationMode(OPERATION_MODE.WORK, "SourceNode Arrive");
					
					break;
				}
				case MAPMAKE_ASSIGNED:
				case PATROL_ASSIGNED:
				{
					// OperationMode(I->W) by SourceNode Arrive.
					changeOperationMode(OPERATION_MODE.WORK, "SourceNode Arrive");
					break;
				}
				default:
				{
					traceOperation("Failover Case.");
					// OperationMode(I->W) by Failover.
					changeOperationMode(OPERATION_MODE.WORK, "Failover");
					break;
				}
			}
		} else {
			// 2013.02.15 by KYK
			String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//			vehicleData.setTargetNode(trCmd.getSourceNode());
			vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
			addVehicleToUpdateList();
			return searchVehiclePath(trCmd.getSourceNode(), TrCmdConstant.UNLOAD, false);
		}

		return true;
	}
	
	/**
	 * RemoteCmd가 TRANSFER이고, STATE가 CMD_TRANSFERRING인 경우에 대한 Vehicle 제어 (Idle Mode)
	 * 
	 * @author mokmin.park
	 * @return 정상 or 비정상 상태 (boolean).
	 */
	private boolean controlVehicleWithTransferringCommand() {
		assert (trCmd != null);
		assert (trCmd.isPause() == false) && (trCmd.getState() == TRCMD_STATE.CMD_TRANSFERRING);

		// CMD_TRANSFERRING은 UNLOADED 이후. (Carrier를 들고 있음)
		// 2021.04.02 by JDH : Transfer Premove 사양 추가
//		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER || trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE) {
			
			if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADED) {
				// 2022.02.14 dahye : Premove Logic Improve
				//	State:CMD_PREMOVE, DetailState:LOAD_WAITING
//				// 2021.04.02 by JDH : Transfer Premove 사양 추가
//				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE) {
//					trCmd.setState(TRCMD_STATE.CMD_PREMOVE);
//				}
				
				// UNLOADED인 경우, LOAD_ASSIGNED로 전환.
				trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
				trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
				addTrCmdToStateUpdateList();
				// 2013.02.15 by KYK
				String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//				vehicleData.setTargetNode(trCmd.getDestNode());
				vehicleData.setTarget(trCmd.getDestNode(), targetStation);
				addVehicleToUpdateList();
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPARTED, 0);
			}
			
			switch (trCmd.getDetailState()) {
				case LOAD_ASSIGNED:
				{
					// 2013.02.01 by KYK
//					if (hasArrivedAtDestNode()) {
					if (hasArrivedAtDest()) {
//						if (hasArrivedAtTargetPort(trCmd.getDestLoc()) == false) {
//							if (hasPassedbyTargetPort(trCmd.getDestLoc())) {
//								// 동일노드 후방포인트로 이동 위해 Drive
//								return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
//							} else {
//								// 동일노드 전방포인트로 이동
//								changeOperationMode(OPERATION_MODE.GO, "Go To Point At Same Node(DestLoc)");
//								return true;
//							}
//						}

						// 현재 위치가 TargetNode(DestNode)인 경우.
						if (trCmd.getPauseCount() == 0 && trCmd.isPause() == false) {
							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ARRIVED, 0);
						} else {
							traceOperation("VehicleArrived Event is not reported by JobPause(" + trCmd.getPausedTime() + ", " + trCmd.getPauseCount() + ", " + trCmd.getPauseCount() + ")");
						}
//						// 2021.09.03 dahye : Transfer Premove 처리 위치 변경
						//	기존 : RCMD 확인 후, Arrive 및 TR_STATE 변경. IdleMode에서 Dest 도착 시 Arrived 미보고 현상 존재
						//	변경 : Arrived 는 기존과 동일하게 처리. 이후 RCMD 확인하여 TR_STATE 변경
//						// OperationMode(I->W) by DestNode Arrive.
//						changeOperationMode(OPERATION_MODE.WORK, "DestNode Arrive");
						if(trCmd.getRemoteCmd().equals(TRCMD_REMOTECMD.PREMOVE)){
							// 2022.03.14 dahye : Premove Logic Improve (TimeCheck Logging)
							//	1) DWT Logging
							//	2) State Change
							//	3) Timeout Check -> at Operation MainProcessing
//							// 21.08.25 dahye : DeliveryType 확인할 필요 없음
//							// 2021.04.02 by JDH : Transfer Premove 사양 추가
//							trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_WAITING);
//							vehicleData.setPremoveWaitTime(System.currentTimeMillis());
							StringBuffer log = new StringBuffer("PREMOVE Arrived at DestPort. ");
							log.append("RemainingTime:").append(trCmd.getRemainingDuration());
							log.append("(DWT:").append(trCmd.getDeliveryWaitTimeOut()).append(")");
							traceOperation(log.toString());
							
							trCmd.setState(TRCMD_STATE.CMD_PREMOVE);
							trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_WAITING);

							addVehicleToUpdateList();
							addTrCmdToStateUpdateList();
						} else {
							// OperationMode(I->W) by DestNode Arrive.
							changeOperationMode(OPERATION_MODE.WORK, "DestNode Arrive");
						}
					} else {
						// TargetNode까지 Drive를 못한 경우, TargetNode까지의 경로 탐색 후, GO 모드로 전환.
						// Search VehiclePath to DestNode & Change to GO Mode.
						// 2013.02.15 by KYK
						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//						vehicleData.setTargetNode(trCmd.getDestNode());
						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
						addVehicleToUpdateList();
						return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
					}
					break;
				}
				// 2022.03.14 dahye : Prmove Logic Improve
				//	State:CMD_PREMOVE, DetailState:LOAD_WAITING
//				case LOAD_WAITING:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
//				{
//					if((System.currentTimeMillis() - vehicleData.getPremoveWaitTime())/1000 >= trCmd.getDeliveryWaitTimeOut()){
//						if (trCmd.isPause() == false) {
//							trCmd.setLastAbortedTime(System.currentTimeMillis());
//							trCmd.setState(TRCMD_STATE.CMD_ABORTED);
//							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
//							trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);	// 21.08.25 dahye : Timeout일 경우 DeliveryType 없이 일반 대체반송 수신 필요
//							trCmd.setDeliveryType("");
//							pauseTrCmd(true, TrCmdConstant.DW_TIMEOUT, trCmd.getPauseCount() + 1);
//							addTrCmdToStateUpdateList();
//							
//							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
//							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_PREMOVE_WAIT_TIMEOUT);
//							
//							addVehicleToUpdateList();
//						}
//					}
//					
//					if (hasArrivedAtDest()) {
//						//Do Nothing
//						
//					} else {
//						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
//
//						addVehicleToUpdateList();
//						return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
//					}
//					
//					if(vehicleData.isYieldRequested()){
//						searchVehicleYieldPath();
//						changeOperationMode(OPERATION_MODE.GO,"Yield Move while Load Waiting by Premove");
//						break;
//					}
//					break;
//				}
				case LOAD_SENT:
				case LOAD_ACCEPTED:
				case LOADING:
				{
					traceOperation("Failover/CommFail Case. (TrCmdId:" + trCmd.getTrCmdId() + ", DetailState:" + trCmd.getDetailState() + ")");
					// 2013.02.01 by KYK
//					if (hasArrivedAtDestNode()) {
					if (hasArrivedAtDest()) {
						changeOperationMode(OPERATION_MODE.WORK, "Failover/CommFail");
					} else {
						// IdleMode#002
						traceOperationException("Abnormal Case: IdleMode#002");
						// 2013.02.15 by KYK
						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//						vehicleData.setTargetNode(trCmd.getDestNode());
						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
						addVehicleToUpdateList();
						return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
					}
					break;
				}
				case UNLOAD_ASSIGNED:
				case UNLOAD_SENT:
				case UNLOAD_ACCEPTED:
				case UNLOADING:
				{
					// IdleMode#003
					traceOperationException("Abnormal Case: IdleMode#003");
					trCmd.setState(TRCMD_STATE.CMD_WAITING);
					addTrCmdToStateUpdateList();
					break;
				}
				default:
				{
					traceOperationException("Abnormal Case: IdleMode#004");
					
					// 2011.10.26 by PMM

					// 2013.02.01 by KYK
//					if (hasArrivedAtDestNode()) {
					if (hasArrivedAtDest()) {
						// 2013.02.15 by KYK
						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//						vehicleData.setTargetNode(trCmd.getDestNode());
						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
						addVehicleToUpdateList();
						return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
					}
					
//					if (isWorkNode(vehicleData.getCurrNode())) {
//						// OperationMode(I->W) by Abnormal.
//						changeOperationMode(OPERATION_MODE.WORK, "Abnormal");
//					}
					break;
				}
			}
		} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
			//trCmd.isPause() == false && trCmd.getState() == TRCMD_STATE.CMD_TRANSFERRING
			if (hasArrivedAtDest()) {
				// OperationMode(I->W) by DestNode Arrive.
				changeOperationMode(OPERATION_MODE.WORK, "DestNode Arrive");
			} else {
				// TargetNode까지 Drive를 못한 경우, TargetNode까지의 경로 탐색 후, GO 모드로 전환.
				// Search VehiclePath to DestNode & Change to GO Mode.
				String targetStation = getStationIdAtPort(trCmd.getDestLoc());
				vehicleData.setTarget(trCmd.getDestNode(),targetStation);
				addVehicleToUpdateList();
				return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
			}
		} else {
			traceOperationException("Abnormal Case: IdleMode#005");
			return false;
		}
		return true;
	}

	/**
	 * RemoteCmd가 ABORT인 경우에 대한 Vehicle 제어 (Idle Mode)
	 * 
	 * @author mokmin.park
	 * @return 정상 or 비정상 상태 (boolean).
	 */
	private boolean controlVehicleWithAbortCommand() {
		assert (trCmd != null);
		assert (trCmd.isPause() == false);
		assert (trCmd.getState() == TRCMD_STATE.CMD_ABORTING) || (trCmd.getState() == TRCMD_STATE.CMD_ABORTED);
		
		// ABORT인 경우, TargetNode가 작업 노드가 아닌 경우, TargetNode로 MOVE.
		// 2011.11.01 by PMM
		// UnknownTrCmd 등록 시, SourceNode & DestNode는 "" 임.
		if (trCmd.getSourceNode() != null && trCmd.getSourceNode().length() > 0 &&
				trCmd.getDestNode() != null && trCmd.getDestNode().length() > 0) {
			if (vehicleData.getTargetNode().equals(trCmd.getSourceNode()) == false &&
					vehicleData.getTargetNode().equals(trCmd.getDestNode()) == false) {

				// 2013.02.01 by KYK
//				if (hasDrivenToTargetNode() == false) {
				if (hasDrivenToTarget() == false) {
					if (searchVehiclePath(vehicleData.getTargetNode(), TrCmdConstant.MOVE, false)) {
						return true;
					} else {
						return false;
					}
				}
			}
		}
		
		// HID장애지역 탈출, YieldSearch.
		if (searchEscapeForAbnormalHid()) {
			return true;
		}
    
		//VehiclePathSearchForLocalOHT() ??
		
		searchVehicleYieldPath();
		return true;
	}
	
	// 2011.10.20 by PMM
	// controlVehicleWithStagingCommand() 추가
	/**
	 * Control Staging Vehicle 
	 */
	private boolean controlVehicleWithStagingCommand() {
		assert (trCmd != null);
		assert (trCmd.getState() == TRCMD_STATE.CMD_STAGING);
		
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
			switch (trCmd.getDetailState()) {
				// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//				case STAGE_NOBLOCKING:
				case STAGE_WAITING:
				{
					// Failover Case ??
					// Abnormal Case.
					// 2013.02.01 by KYK
//					if (hasArrivedAtSourceNode()) {
					if (hasArrivedAtSource()) {
						// OperationMode(I->W) by DestNode Arrive.
						// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//						changeOperationMode(OPERATION_MODE.WORK, "STAGE NoBlocking - SourceNode Arrive");
						changeOperationMode(OPERATION_MODE.WORK, "STAGE WaitingTime - SourceNode Arrive");
					} else {
						// Abnormal Case.
						traceOperationException("Abnormal Case: IdleMode#006");
						// 2013.02.15 by KYK
						String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//						vehicleData.setTargetNode(trCmd.getSourceNode());
						vehicleData.setTarget(trCmd.getSourceNode(), targetStation);

						addVehicleToUpdateList();
						return searchVehiclePath(trCmd.getSourceNode(), TrCmdConstant.UNLOAD, false);
					}
					break;
				}
				// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//				case STAGE_WAITING:
				case STAGE_NOBLOCKING:
				{
					// 2013.02.01 by KYK
//					if (hasArrivedAtSourceNode()) {
					if (hasArrivedAtSource()) {
						if (searchEscapeForAbnormalHid()) {
							return true;
						}
						if (searchVehicleYieldPath()) {
							return true;
						}
						
						// 2012.01.19 by PMM
						// Exception 처리 추가
						try {
							if (isNearByDrive &&
									isFailoverCompleted() == false &&
									vehicleData.checkYieldForBackwardVehicle()) {
								searchVehicleYieldPath();
							}
						} catch (Exception e) {
							traceOperationException("IdleMode - controlVehicleWithStagingCommand()", e);
						}
					} else {
						// 2013.02.15 by KYK
						String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//						vehicleData.setTargetNode(trCmd.getSourceNode());
						vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
						addVehicleToUpdateList();
						return searchVehiclePath(trCmd.getSourceNode(), TrCmdConstant.UNLOAD, false);
					}
					break;
				}
				default:
					traceOperationException("Abnormal Case: IdleMode#007");
					break;
			}
		}
		return true;
	}
	
	/**
	 * Control MapMaking Vehicle
	 * 
	 * @return
	 */
	private boolean controlVehicleWithMapMakingCommand() {
		assert (trCmd != null);
		assert (trCmd.isPause() == false);
		assert (trCmd.getState() == TRCMD_STATE.CMD_MAPMAKING);
		
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.MAPMAKE) {
			if (trCmd.getDetailState() == TRCMD_DETAILSTATE.MAPMAKING) {
				if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
					// 2013.02.01 by KYK
//					if (hasDrivenToTargetNode()) {
//						if (hasArrivedAtTargetNode()) {
					if (hasDrivenToTarget()) {
						if (hasArrivedAtTarget()) {
							// OperationMode(I->W) by DestNode Arrive.
							changeOperationMode(OPERATION_MODE.WORK, "DestNode Arrive");
						} else {
							// Do Nothing. Waiting...
							// OperationMode(I->G) by LOAD.
							changeOperationMode(OPERATION_MODE.GO, "LOAD (Drived to TargetNode)");
						}
					} else {
						return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
					}
				} else {
					// 2013.02.15 by KYK
					String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//					vehicleData.setTargetNode(trCmd.getDestNode());
					vehicleData.setTarget(trCmd.getDestNode(), targetStation);
					addVehicleToUpdateList();
				}
			} else {
				trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMAKING);
				addTrCmdToStateUpdateList();
			}
		}
		return true;
	}
	
	/**
	 * Control Patrolling Vehicle
	 * 
	 * @return
	 */
	private boolean controlVehicleWithPatrollingCommand() {
		assert (trCmd != null);
		assert (trCmd.isPause() == false);
		assert (trCmd.getState() == TRCMD_STATE.CMD_PATROLLING);
		
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
			if (trCmd.getDetailState() == TRCMD_DETAILSTATE.PATROLLING) {
				if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
					// 2013.02.01 by KYK
//					if (hasDrivenToTargetNode()) {
//						if (hasArrivedAtTargetNode()) {
					if (hasDrivenToTarget()) {
						if (hasArrivedAtTarget()) {
							// OperationMode(I->W) by DestNode Arrive.
							changeOperationMode(OPERATION_MODE.WORK, "DestNode Arrive");
						} else {
							// Do Nothing. Waiting...
							// OperationMode(I->G) by LOAD.
							changeOperationMode(OPERATION_MODE.GO, "LOAD (Drived to TargetNode)");
						}
					} else {
						return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
					}
				} else {
					// 2013.02.15 by KYK
					String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//					vehicleData.setTargetNode(trCmd.getDestNode());
					vehicleData.setTarget(trCmd.getDestNode(), targetStation);
					addVehicleToUpdateList();
				}
			} else {
				trCmd.setDetailState(TRCMD_DETAILSTATE.PATROLLING);
				addTrCmdToStateUpdateList();
			}
		}
		return true;
	}
	
	/**
	 * Control Monitoring Vehicle
	 * 
	 * @return
	 */
	private boolean controlVehicleWithMonitoringCommand() {
		if (trCmd != null) {
			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
				if (trCmd.getState() != TRCMD_STATE.CMD_MONITORING) {
					traceOperationException("Abnormal Case: IdleMode#030");
				}
				// TrCmd State: CMD_MONITORING
				pauseTrCmd(true, TrCmdConstant.VIBRATION, 0);
				if (vehicleData.getStopNode().equals(vehicleData.getTargetNode()) == false) {
					return searchVehiclePath(vehicleData.getTargetNode(), TrCmdConstant.MOVE, false);
				}
				
				if (searchEscapeForAbnormalHid()) {
					return true;
				}
				
				if (searchVehicleYieldPath()) {
					return true;
				}
				
				try {
					if (isNearByDrive &&
							isFailoverCompleted() == false &&
							vehicleData.checkYieldForBackwardVehicle()) {
						searchVehicleYieldPath();
					}
				} catch (Exception e) {
					traceOperationException("IdleMode - controlVehicleWithPausedCommand()", e);
				}
			}
		}
		return true;
	}
	
	/**
	 * Control PREMOVE Vehicle
	 * 2021.04.02 by JDH : Transfer Premove 사양 추가
	 */
	private boolean controlVehicleWithPremoveCommand() {
		assert (trCmd != null);
		assert (trCmd.getState() == TRCMD_STATE.CMD_PREMOVE);
		
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE) {
			switch (trCmd.getDetailState()) {
			// 2022.03.14 dahye : Premove Logic Improve
			// trCmd 의 State-DetailState 변경. DestLoc에서 대기중인 상태만 CMD_PREMOVE 상태로 정의
			//   기존 : CMD_PREMOVE = LOAD_ASSIGNED, LOAD_WAITING
			//   변경 : CMD_PREMOVE = LOAD_WAITING
//				case LOAD_ASSIGNED:{
//
//					if (hasArrivedAtDest()) {
//						
//						// 21.09.03 dahye : Dest Arrive 미보고 현상 보완
//						// 현재위치가 TargetNode(DestNode)인 경우.
//						if (trCmd.getPauseCount() == 0 && trCmd.isPause() == false) {
//							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ARRIVED, 0);
//						} else {
//							traceOperation("VehicleArrived Event is not reported by JobPause(" + trCmd.getPausedTime() + ", " + trCmd.getPauseCount() + ", " + trCmd.getPauseCount() + ")");
//						}
//						
//						// 21.08.25 dahye : DeliveryType 확인할 필요 없음
//						trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_WAITING);
//						vehicleData.setPremoveWaitTime(System.currentTimeMillis());
//						addVehicleToUpdateList();
//						addTrCmdToStateUpdateList();
//					} else {
//						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
//						addVehicleToUpdateList();
//						return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
//					}
//					break;
//					
//					
//				}
				case LOAD_WAITING:
				{
					// 2022.03.14 dahye : Premove Logic Improve
					// Premove Timeout Check -> at Operation MainProcessing
//					if((System.currentTimeMillis() - vehicleData.getPremoveWaitTime())/1000 >= trCmd.getDeliveryWaitTimeOut()){	//(DW Timeout: LOAD_WAITING 상태 유지시간)
//						if (trCmd.isPause() == false) {
//							trCmd.setLastAbortedTime(System.currentTimeMillis());
//							trCmd.setState(TRCMD_STATE.CMD_ABORTED);
//							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
//							trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);	// 21.08.25 dahye : Timeout일 경우 DeliveryType 없이 일반 대체반송 수신 필요
//							trCmd.setDeliveryType("");
//							pauseTrCmd(true, TrCmdConstant.DW_TIMEOUT, trCmd.getPauseCount() + 1);
//							addTrCmdToStateUpdateList();
//							
//							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
//							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_PREMOVE_WAIT_TIMEOUT);
//							
//							addVehicleToUpdateList();
//						}
//					}
					
					// 2022.03.14 dahye : Premove Logic Improve
					// LOAD_WAITING : Always Already Arrived at DestLoc
//					if (hasArrivedAtDest()) {
//						//Do Nothing
//					} else {
//						String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//						vehicleData.setTarget(trCmd.getDestNode(), targetStation);
//
//						addVehicleToUpdateList();
//						return searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false);
//					}
					
					if(vehicleData.isYieldRequested()){
						searchVehicleYieldPath();
						changeOperationMode(OPERATION_MODE.GO,"Yield Move while Load Waiting by Premove");
						break;
					}
					break;
				}
				
				default:
					traceOperationException("Abnormal Case: IdleMode#112");
					break;
			}
		}
		return true;
	}
	
	/**
	 * RemoteCmd가 PAUSE인 경우에 대한 Vehicle 제어 (Idle Mode)
	 * 
	 * @author mokmin.park
	 * @return 정상 or 비정상 상태 (boolean).
	 */
	private boolean controlVehicleWithPausedCommand() {
		assert (trCmd != null);
		assert (trCmd.isPause());
		
		// Pause Type별 처리.
		if (TrCmdConstant.PATH_SEARCH.equals(trCmd.getPauseType())) {
			// 2011.10.26 by PMM
			// 2013.02.15 by KYK
			String targetStation = getStationIdAtPort(trCmd.getDestLoc());
//			vehicleData.setTargetNode(trCmd.getDestNode());
			vehicleData.setTarget(trCmd.getDestNode(), targetStation);
			addVehicleToUpdateList();
			if (searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false)) {
				if (vehicleData.getAlarmCode() == OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH) {
					unregisterAlarm(OcsAlarmConstant.SEARCH_FAIL_BY_LOAD_PATH);
				}
				pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, trCmd.getPauseCount());
				return true;
			}
		} else if (TrCmdConstant.PORT_DUPLICATE.equals(trCmd.getPauseType())) {
			if (ocsInfoManager.isPortDuplicationUsed() && checkDestPortDuplicate() == false) {
				StringBuilder message = new StringBuilder();
				message.append("[Load Job] Vehicle:").append(vehicleData.getVehicleId());
				if (trCmd != null) {
					message.append(", TrCmdId:").append(trCmd.getTrCmdId());
					message.append(", CarrierId:").append(trCmd.getCarrierId());
					message.append(", SourceLoc:").append(trCmd.getSourceLoc());
					message.append(", DestLoc:").append(trCmd.getDestLoc());
					message.append(", TrQueuedTime:").append(trCmd.getTrQueuedTime());
				}
				if (isPortDuplicateClearEventRegistered == false) {
					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.PORT_DUPLICATE_CLEAR, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
							"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.PORT_DUPLICATE), true);
					isPortDuplicateClearEventRegistered = true;
				}
				if (searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false)) {
					pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
					return true;
				}
			}
		} else {
			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
				// Vehicle이 Manual이 되면, CMD_PAUSED가 될 수 있음.
				if (trCmd.getState() == TRCMD_STATE.CMD_WAITING) {
					if (searchVehiclePath(trCmd.getSourceNode(), TrCmdConstant.UNLOAD, false)) {
						pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
						return true;
					}
				} else if (trCmd.getState() == TRCMD_STATE.CMD_TRANSFERRING) {
					if (searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false)) {
						pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
						return true;
					}
				} else if (trCmd.getState() == TRCMD_STATE.CMD_PAUSED) {
					if (vehicleData.isCarrierExist()) {
						trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
						addTrCmdToStateUpdateList();
						if (searchVehiclePath(trCmd.getDestNode(), TrCmdConstant.LOAD, false)) {
							pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
							return true;
						}
					} else {
						trCmd.setState(TRCMD_STATE.CMD_WAITING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
						addTrCmdToStateUpdateList();
						if (searchVehiclePath(trCmd.getSourceNode(), TrCmdConstant.UNLOAD, false)) {
							pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
							return true;
						}
					}
					return true;
				} else {
					// TrCmd State: CMD_MONITORING
					return controlVehicleWithMonitoringCommand();
				}
			}
		}
		
		if (searchEscapeForAbnormalHid()) {
			return true;
		}

		if (searchVehicleYieldPath()) {
			return true;
		}
		
		// 2011.11.15 by PMM
//		if (ocsInfoManager.isNearByDrive() && vehicleData.checkYieldForBackwardVehicle()) {
		
		// 2012.01.19 by PMM
		// Exception 처리 추가
		try {
			if (isNearByDrive &&
					isFailoverCompleted() == false &&
					vehicleData.checkYieldForBackwardVehicle()) {
				searchVehicleYieldPath();
			}
		} catch (Exception e) {
			traceOperationException("IdleMode - controlVehicleWithPausedCommand()", e);
		}
		
		return true;
	}
	
	/**
	 * Idle Mode에서 Alarm 메시지 삭제 처리
	 * 
	 * @author mokmin.park
	 */
	private void unregisterAlarmInIdleMode() {
		if (isAlarmRegistered()) {
			if (trCmd != null && TRCMD_STATE.CMD_ABORTED.equals(trCmd.getState()) == false) {
				if (vehicleData.getAlarmCode() == OcsAlarmConstant.DELAYED_DESTCHANGE) {
					unregisterAlarm(OcsAlarmConstant.DELAYED_DESTCHANGE);
				}
			}
			switch (vehicleData.getAlarmCode()) {
				case OcsAlarmConstant.ESTOP_BY_VEHICLE_INIT_FAIL:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_VEHICLE_INIT_FAIL);
					break;
				case OcsAlarmConstant.ESTOP_BY_VEHICLE_DRIVE_FAIL:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_VEHICLE_DRIVE_FAIL);
					break;
				case OcsAlarmConstant.ESTOP_BY_VEHICLE_COLLISION:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_VEHICLE_COLLISION);
					break;
				case OcsAlarmConstant.ESTOP_BY_VEHICLE_ON_PROHIBITED_ZONE:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_VEHICLE_ON_PROHIBITED_ZONE);
					break;
				case OcsAlarmConstant.ESTOP_BY_UNLOAD_CARRIER_MISMATCH:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_UNLOAD_CARRIER_MISMATCH);
					break;
				case OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_UNLOAD:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_UNLOAD);
					break;
				case OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_LOAD:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_CARRIER_STATUS_ERROR_LOAD);
					break;
				case OcsAlarmConstant.ESTOP_BY_NODE_STATION_MISMATCH:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_NODE_STATION_MISMATCH);
					break;
				case OcsAlarmConstant.ESTOP_BY_CARRIER_TYPE_MISMATCH:
					unregisterAlarm(OcsAlarmConstant.ESTOP_BY_CARRIER_TYPE_MISMATCH);
					break;
				default:
					break;
			}
		}
	}
}
