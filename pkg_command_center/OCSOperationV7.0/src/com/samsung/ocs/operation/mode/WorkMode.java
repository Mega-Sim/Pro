package com.samsung.ocs.operation.mode;

import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.TrCmdConstant;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.operation.Operation;
import com.samsung.ocs.operation.constant.MessageItem.EVENT_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant;
import com.samsung.ocs.operation.constant.OperationConstant.COMMAND_STATE;
import com.samsung.ocs.operation.constant.OperationConstant.OPERATION_MODE;
import com.samsung.ocs.operation.constant.ResultCode;

/**
 * WorkMode Class, OCS 3.0 for Unified FAB
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

public class WorkMode extends OperationModeImpl {
	/**
	 * Constructor of WorkMode class.
	 */
	public WorkMode(Operation operation) {
		super(operation);
	}
	
	@Override
	public OPERATION_MODE getOperationMode() {
		return OPERATION_MODE.WORK;
	}
	
	/**
	 * Control Vehicle in WorkMode 
	 */
	@Override
	public boolean controlVehicle() {
		trCmd = getTrCmd();
		
		// 1. SLEEP 조건  체크.
		if (checkSleepMode()) {
			return false;
		}
		
		// 2011.12.09 by PMM
		if (checkAbnormalCarrierStatus()) {
			return false;
		}
		
		if (isAbnormalHidCheckCondition()) {
			if (searchEscapeForAbnormalHid()) {
				if (trCmd != null && trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED) {
					cancelAssignedTrCmd(EVENTHISTORY_REASON.HIDDOWN_AT_SOURCENODE, true);
				}
				return false;
			}
		}

		// 2012.01.19 by PMM
		// 2012.03.09 by PMM
		// if (vehicleData.getState() == 'G') {
		// 2013.02.01 by KYK
//		if (vehicleData.getState() == 'G' ||
//				(trCmd != null && hasArrivedAtSourceNode() == false && hasArrivedAtDestNode() == false)) {
		if (vehicleData.getState() == 'G' ||
				(trCmd != null && hasArrivedAtSource() == false && hasArrivedAtDest() == false)) {
			traceOperationException("vehicleData.getCurrNode() : "+vehicleData.getCurrNode());
			traceOperationException("trCmd.getDestLoc() : "+ trCmd.getDestLoc());
			traceOperationException("trCmd.getDestNode() : "+ trCmd.getDestNode());
			changeOperationMode(OPERATION_MODE.GO, "Not Yet Arrived at WorkNode.");
			traceOperationException("Abnormal Case: WorkMode#100");
			return false;
		}
		
		// 2. Vehicle 제어 
		if (trCmd == null) {
			// 2-1. 작업이 없는 경우 
			return controlVehicleWithNoCommand();
		} else {
			// 2012.07.31 by PMM
			// UNLOAD 이전에 받은 양보 요청을 LOAD 이후에 수행하는 케이스 발생함.
			vehicleData.resetYieldRequested();
			
			switch (trCmd.getRemoteCmd()) {
				case TRANSFER:
				case VIBRATION:
				case PREMOVE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
					// 2-2. TRANSFER 작업인 경우
					return controlVehicleWithTransferCommand();
				case SCAN:
					// 2-3. SCAN 작업인 경우
					return controlVehicleWithScanCommand();
				case STAGE:
					// 2-4. STAGE 작업인 경우
					return controlVehicleWithStageCommand();
				case MAPMAKE:
					// 2-5. MAPMAKE 작업인 경우
					return controlVehicleWithMapMakeCommand();
				case PATROL:
					// 2-6. PATROL 작업인 경우
					return controlVehicleWithPatrolCommand();
				case ABORT:
					return controlVehicleWithAbortCommand();
				default:
					traceOperationException("Abnormal Case: WorkMode#001");
					return false;
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	private boolean controlVehicleWithNoCommand() {
		// Work Mode에서 TrCmd가 없으면 Idle로 전환.
		// OperationMode(W->I) by NoJob
		if (vehicleData.getState() == 'U' ||
				vehicleData.getState() == 'L' ||
				vehicleData.getState() == 'R') {
			traceOperationException("No TrCmd, But CurCmd is Executing...");
		} else {
			changeOperationMode(OPERATION_MODE.IDLE, "NoJob");
		}
		return true;
	}

	/**
	 * Control Vehicle Having TRANSFER Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferCommand() {
		assert (trCmd != null);
		switch (trCmd.getDetailState()) {
			case UNLOAD_ASSIGNED:
				return controlVehicleWithTransferUnloadAssignedCommand();
			case UNLOAD_SENT:
				return controlVehicleWithTransferUnloadSentCommand();
			case UNLOAD_ACCEPTED:
				return controlVehicleWithTransferUnloadAcceptedCommand();
			case UNLOADING:
				return controlVehicleWithTransferUnloadingCommand();
			case UNLOADED:
				return controlVehicleWithTransferUnloadedCommand();
			case LOAD_ASSIGNED:
				return controlVehicleWithTransferLoadAssignedCommand();
			case LOAD_WAITING:	// 2022.03.14 dahye : Premove Logic Improve
				return controlVehicleWithTransferLoadWaitingCommand();
			case LOAD_SENT:
				return controlVehicleWithTransferLoadSentCommand();
			case LOAD_ACCEPTED:
				return controlVehicleWithTransferLoadAcceptedCommand();
			case LOADING:
				return controlVehicleWithTransferLoadingCommand();
//			case LOADED:
//				return controlVehicleWithTransferLoadedCommand();
			default :
				changeOperationMode(OPERATION_MODE.IDLE, "Abnormal");
				return false;
		}
	}
	
	/**
	 * Control Vehicle Having UNLOAD_ASSIGNED Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferUnloadAssignedCommand() {
		// 2013.02.01 by KYK
//		assert hasArrivedAtSourceNode();
		assert hasArrivedAtSource();
		if (isLoadingByPass()) {
			// 2013.02.01 by KYK
//			if (hasArrivedAtSourceNode()) {
			if (hasArrivedAtSource()) {
				trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
				trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
				addTrCmdToStateUpdateList();
				
				// OperationMode(W->I) by LoadingByPass
				changeOperationMode(OPERATION_MODE.IDLE, "LoadingByPass");
				return true;
			} else {
				changeOperationMode(OPERATION_MODE.IDLE, "Not Yet Arrived at the Source Node");
				return false;
			}
		} else {
			// 2013.02.01 by KYK
//			if (hasArrivedAtSourceNode()) {
			if (hasArrivedAtSource()) {
				if ((vehicleData.isCarrierExist() == false && vehicleData.getState() == 'U') ||
						(vehicleData.isCarrierExist() && vehicleData.getState() == 'N')) {
					// 2012.01.30 by PMM
					trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADING);
					addTrCmdToStateUpdateList();
					return true;
				}
				// 2011.11.02 by PMM
				// Vehicle ActionHold이면 UNLOAD 명령 전송하지 않음.
				if (vehicleData.isActionHold()) {
					return false;
				}
				
				// 2012.03.08 by KYK  
				// STB(orUTB) PortOutOfService 일 경우, Unload 전 확인하여 Cancel (비정상 완료 보고)
				if (isSTBCUsed()) {
					if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
						if (isSTBPortAvailable(trCmd.getDestLoc()) == false) {
							trCmd.setRemoteCmd(TRCMD_REMOTECMD.CANCEL);
							trCmd.setState(TRCMD_STATE.CMD_CANCELED);
							trCmd.setCarrierLoc(trCmd.getSourceLoc());
							trCmd.setDeletedTime(ocsInfoManager.getCurrDBTimeStr());
							addTrCmdToStateUpdateList();
							
							registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());

							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
							// 2012.11.30 by KYK : ResultCode 세분화
//							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_STBPORT_OUTOFSERVICE);
							deleteTrCmdFromDB();

							// OperationMode(W->I) by TrCmd's Aborted by ocs : STBPort is not Available
							changeOperationMode(OPERATION_MODE.IDLE, "TrCmd's Canceled by OCS: Dest STBPort is not Available");
							return true;						
						}
					}
				}
			}
//			if (vehicleData.isCarrierExist() && hasArrivedAtSourceNode()) {
//				if (vehicleData.getState() == 'N' || (vehicleData.getState() == 'A' && getOCSInfoManager().isEmulatorMode())) {
//					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADING);
//					addTrCmdToStateUpdateList();
//					return true;
//				}
//			}
			
			if (checkExceptionalConditionForTransferCommand(trCmd.getSourceNode())) {
				return false;
			}
			
			if (isUnloadCommandSendable()) {
				makeVehicleCommandId();
				sendUnloadCommand(getVehicleCommCommand().getCommandId(), 'X');
				trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_SENT);
				addTrCmdToStateUpdateList();
				return true;				
			} else if (vehicleData.getState() == 'U' || vehicleData.getState() == 'N') {
				trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
				// 2012.01.30 by PMM
//				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_SENT);
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADING);
				addTrCmdToStateUpdateList();
				return true;
			// 2012.03.13 by PMM
//			} else if (vehicleData.getState() == 'A') {
			} else if (vehicleData.getState() == 'A' || vehicleData.getState() == 'I') {
				if (getCommandState() == COMMAND_STATE.SENT) {
					trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_SENT);
					addTrCmdToStateUpdateList();
					return true;
				} else {
					// Waiting
					; /*NULL*/
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#002");
			}
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having UNLOAD_SENT Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferUnloadSentCommand() {
		// 2013.02.01 by KYK
//		assert hasArrivedAtSourceNode();
		assert hasArrivedAtSource();
		
		if (vehicleData.isCarrierExist()) {
			if (vehicleData.getState() == 'U' || vehicleData.getState() == 'N') {
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADING);
				addTrCmdToStateUpdateList();
				
				// 2012.01.30 by PMM
				if (vehicleData.getState() == 'N') {
					return controlVehicleWithTransferUnloadingCommand();
				} else {
					return true;
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#003");
			}
		} else {
			if (vehicleData.getState() == 'U') {
				// 2016.08.16 by KBS : Unload/Load 보고 시점 개선
				if (vehicleData.getTransStatus() == 0x01) {
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADING);
					addTrCmdToStateUpdateList();
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFERRING, 0);
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRESTARTED, 0);

					return true;
				} else {
					// Waiting. (Hoist does not moved yet)
					; /*NULL*/
				}
			} else if (vehicleData.getState() == 'I' || vehicleData.getState() == 'A' ||
					vehicleData.getState() == 'O' || vehicleData.getState() == 'F') {
				if (getCommandState() == COMMAND_STATE.TIMEOUT) {
					if (vehicleData.isAvRetryWait() == false) {
						sendUnloadCommand(getVehicleCommCommand().getCommandId(), 'X');
						return true;
					} else {
						traceOperationException("Abnormal Case: WorkMode#004");
					}
				} else {
					// Waiting!!
					; /*NULL*/
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#005");
			}
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having UNLOAD_ACCEPTED Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferUnloadAcceptedCommand() {
		// 2013.02.01 by KYK
//		assert hasArrivedAtSourceNode();
		assert hasArrivedAtSource();
		
		if (vehicleData.isCarrierExist()) {
			if (vehicleData.getState() == 'U' || vehicleData.getState() == 'N') {
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADING);
				addTrCmdToStateUpdateList();
				
				// 2012.01.30 by PMM
				if (vehicleData.getState() == 'N') {
					return controlVehicleWithTransferUnloadingCommand();
				} else {
					return true;
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#006");
			}
		} else {
			if (vehicleData.getState() == 'U') {
				if (getCommandState() == COMMAND_STATE.EXECUTING) {
					// 2016.08.16 by KBS : Unload/Load 보고 시점 개선
					if (vehicleData.getTransStatus() == 0x01) {
						trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADING);
						addTrCmdToStateUpdateList();
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFERRING, 0);
						sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRESTARTED, 0);

						return true;
					} else {
						// Waiting. (Hoist does not moved yet)
						; /*NULL*/
					}
				} else if (getCommandState() == COMMAND_STATE.RESPONDED) {
					// Waiting.
					; /*NULL*/
					
				} else {
					traceOperationException("Abnormal Case: WorkMode#007");
				}
			} else if (vehicleData.getState() == 'A' || vehicleData.getState() == 'I' || vehicleData.getState() == 'O') {
				// Waiting!!
				// A (Arrived), I (Init), O (Loaded) 상태일 수 있음.
				if (getCommandState() == COMMAND_STATE.RESPONDED) {
					// Waiting
					return true;
				} else if (getCommandState() == COMMAND_STATE.READY || getCommandState() == COMMAND_STATE.EXECUTED) {
					// 2013.08.20 by PMM : CMD_TRANSFERRING/UNLOAD_ASSIGNED 인 케이스가 발생함.
					trCmd.setState(TRCMD_STATE.CMD_WAITING);
					
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
					addTrCmdToStateUpdateList();
				} else {
					traceOperationException("Abnormal Case: WorkMode#008");
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#009");
			}
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having UNLOADING Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferUnloadingCommand() {
		// 2013.02.01 by KYK
//		assert hasArrivedAtSourceNode();
		assert hasArrivedAtSource();
		
		if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
			if (vehicleData.isCarrierExist()) {
				if (vehicleData.getState() == 'N') {
					if (checkCarrierMismatchedOnUnloadPort()) {
						return true;
					}
					// 2013.09.06 by KYK
					if (checkCarrierTypeMismatch()) {
						return true;
					}
					
					if (checkMissedCarrierOnUnloadPort()) {
						changeOperationMode(OPERATION_MODE.IDLE, "Missed Carrier");
						return true;
					}
					
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
					trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
					trCmd.setUnloadedTime(getCurrDBTimeStr());
					addTrCmdToStateUpdateList();
					pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, 0);
					
					sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRECOMPLETED, 0);
					
					if (checkSTBUTBCarrierMismatchOnUnloadPort()) {
						return true;
					}
					
					// OperationMode(W->I) by Unload Completion
					changeOperationMode(OPERATION_MODE.IDLE, "Unload Completion");
				} else {
					// Waiting!!
					; /*NULL*/
				}
			} else {
				// Waiting!!
				; /*NULL*/
			}
		} else {
			// Do Nothing??
			traceOperationException("Abnormal Case: WorkMode#012");
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having UNLOADED Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferUnloadedCommand() {
		// 2013.02.01 by KYK
//		assert hasArrivedAtSourceNode();
		assert hasArrivedAtSource();

		// 2022.02.14 dahye : Premove Logic Improve
		//	State:CMD_PREMOVE, DetailState:LOAD_WAITING
		// 2021.04.02 by JDH : Transfer Premove 사양 추가
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
//		if(trCmd.getRemoteCmd()==TRCMD_REMOTECMD.PREMOVE){
//			trCmd.setState(TRCMD_STATE.CMD_PREMOVE);
//			trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
//			trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
//			addTrCmdToStateUpdateList();
//			vehicleData.resetLocusData();
//		} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
			trCmd.setState(TRCMD_STATE.CMD_MONITORING);
			trCmd.setDetailState(TRCMD_DETAILSTATE.VIBRATION_MONITORING);
			trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
			addTrCmdToStateUpdateList();
			vehicleData.resetLocusData();
		} else {
			trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
			trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
			trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
			addTrCmdToStateUpdateList();
			sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPARTED, 0);
		}
		
		// OperationMode(W->I) by Unload Completion
		changeOperationMode(OPERATION_MODE.IDLE, "Unload Completion");
		
		return true;
	}
	
	/**
	 * Control Vehicle Having LOAD_ASSIGNED Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferLoadAssignedCommand() {
		if (isLoadingByPass()) {
			// 2013.02.01 by KYK
//			if (hasArrivedAtDestNode()) {
			if (hasArrivedAtDest()) {
				trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
				trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
				trCmd.setCarrierLoc(trCmd.getDestLoc());
				addTrCmdToStateUpdateList();
				registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
				
				// OperationMode(W->I) by Load Completion (LoadingByPass)
				changeOperationMode(OPERATION_MODE.IDLE, "Load Completion (LoadingByPass)");
				deleteTrCmdFromDB();
				
				return true;
			} else {
				changeOperationMode(OPERATION_MODE.IDLE, "Not Yet Arrived at the Dest Node");
				return false;
			}
		} else {
			// 2013.02.01 by KYK
//			if (hasArrivedAtDestNode()) {
			if (hasArrivedAtDest()) {
				if ((vehicleData.isCarrierExist() && vehicleData.getState() == 'L') ||
						(vehicleData.isCarrierExist() == false && vehicleData.getState() == 'O')) {
					trCmd.setDetailState(TRCMD_DETAILSTATE.LOADING);
					addTrCmdToStateUpdateList();
					return true;
				}			
				
				// 2011.11.02 by PMM
				// Vehicle ActionHold이면 LOAD 명령 전송하지 않음.
				if (vehicleData.isActionHold()) {
					return false;
				}
				
				// 2022.03.14 dahye : Premove Logic Improve
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE) {					
					StringBuffer log = new StringBuffer("PREMOVE Arrived at DestPort. ");
					log.append("RemainingTime:").append(trCmd.getRemainingDuration());
					log.append("(DWT:").append(trCmd.getDeliveryWaitTimeOut()).append(")");
					traceOperation(log.toString());
					
					trCmd.setState(TRCMD_STATE.CMD_PREMOVE);
					trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_WAITING);					
					changeOperationMode(OPERATION_MODE.IDLE,"WorkNode Arrive for PREMOVE");
					
					addVehicleToUpdateList();
					addTrCmdToStateUpdateList();
					
					return true;
				}
				
				// 2012.03.08 by KYK  
				// STB(orUTB) PortOutOfService 일 경우, Load 전 확인하여 ABORT (이후 DestChange 처리)
				if (isSTBCUsed()) {
					if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
						if (isSTBPortAvailable(trCmd.getDestLoc()) == false) {
							trCmd.setState(TRCMD_STATE.CMD_PAUSED);
							// alarmSet, clear 는 하지 않도록함 (요청) // 2012.03.15 by KYK
							pauseTrCmd(true, TrCmdConstant.AUTO_ERROR, trCmd.getPauseCount());
							trCmd.setLastAbortedTime(System.currentTimeMillis());
							trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
							trCmd.setState(TRCMD_STATE.CMD_ABORTED);
							addTrCmdToStateUpdateList();
							// 2012.11.30 by KYK : ResultCode 세분화
//							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_STBPORT_OUTOFSERVICE);

							// OperationMode(W->I) by TrCmd's Aborted by ocs : STBPort is not Available
							changeOperationMode(OPERATION_MODE.IDLE, "TrCmd's Aborted by OCS: Dest STBPort is not Available");
							return true;						
						}
					}
				}
			}
			
			if (checkExceptionalConditionForTransferCommand(trCmd.getDestNode())) {
				return false;
			}
			
			if (isLoadCommandSendable()) {
				trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_SENT);
				addTrCmdToStateUpdateList();
				makeVehicleCommandId();
				// 2013.02.22 by KYK
				sendLoadCommand(getVehicleCommCommand().getCommandId(), 'X');
//				sendLoadCommand(getVehicleCommCommand().getCommandId(), vehicleData.getCurrNode(), 'X');
				return true;
			// 2012.03.13 by PMM
//			} else if (vehicleData.getState() == 'A') {
			} else if (vehicleData.getState() == 'A' || vehicleData.getState() == 'I') {
				if (getCommandState() == COMMAND_STATE.SENT) {
					trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_SENT);
					addTrCmdToStateUpdateList();
					return true;
				} else {
					// Waiting
					; /*NULL*/
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#013");
			}
		}
		return false;
	}
	
	// 2012.01.11 by PMM
//	private boolean controlVehicleWithTransferLoadAssignedCommand() {
//		if (hasArrivedAtDestNode()) {
//			if (isLoadingByPass()) {
//				trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
//				trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
//				trCmd.setCarrierLoc(trCmd.getDestLoc());
//				addTrCmdToStateUpdateList();
//				registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
//				
//				// OperationMode(W->I) by Load Completion (LoadingByPass)
//				changeOperationMode(OPERATION_MODE.IDLE, "Load Completion (LoadingByPass)");
//				deleteTrCmdFromDB();
//				return true;
//			} else {
//				if ((vehicleData.isCarrierExist() && vehicleData.getState() == 'L') ||
//						(vehicleData.isCarrierExist() == false && vehicleData.getState() == 'O')) {
//					trCmd.setDetailState(TRCMD_DETAILSTATE.LOADING);
//					addTrCmdToStateUpdateList();
//					return true;
//				}
//				
//				// 2011.11.02 by PMM
//				// Vehicle ActionHold이면 LOAD 명령 전송하지 않음.
//				if (vehicleData.isActionHold()) {
//					return false;
//				}
//				
//				if (checkExceptionalConditionForTransferCommand(trCmd.getDestNode())) {
//					return false;
//				}
//				
//				if (isLoadCommandSendable()) {
//					trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_SENT);
//					addTrCmdToStateUpdateList();
//					makeVehicleCommandId();
//					sendLoadCommand(getVehicleCommCommand().getCommandId(), vehicleData.getCurrNode(), 'X');
//					return true;
//				}
//			}
//		} else if (vehicleData.getTargetNode().equals(trCmd.getDestNode()) == false) {
//			// 2011.10.27 by PMM
//			vehicleData.setTargetNode(trCmd.getDestNode());
//			addVehicleToUpdateList();
//			changeOperationMode(OPERATION_MODE.IDLE, "TargetNode Changed.");
//		} else {
//			traceOperationException("Abnormal Case: WorkMode#013");
//		}
//		return false;
//	}	
	
	/**
	 * Control Vehicle Having LOAD_WAITING Command
	 * 2022.03.14 dahye : Premove Logic Improve
	 */
	private boolean controlVehicleWithTransferLoadWaitingCommand() {
		assert hasArrivedAtDest();
		// Dest 도착 후 대기상태이면 IdleMode로 변경
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE && vehicleData.getState() == 'A') {
			changeOperationMode(OPERATION_MODE.IDLE, "Arrived at DestLoc (PREMOVE)");
		}
		return true;
	}
	
	
	/**
	 * Control Vehicle Having LOAD_SENT Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferLoadSentCommand() {
		// 2013.02.01 by KYK
//		assert hasArrivedAtDestNode();
		assert hasArrivedAtDest();
		
		if (vehicleData.isCarrierExist()) {
			if (vehicleData.getState() == 'I' || vehicleData.getState() == 'A' || vehicleData.getState() == 'F') {
				if (getCommandState() == COMMAND_STATE.TIMEOUT) {
					if (vehicleData.isAvRetryWait() == false) {
						// 2013.02.22 by KYK
						sendLoadCommand(getVehicleCommCommand().getCommandId(), 'X');
//						sendLoadCommand(getVehicleCommCommand().getCommandId(), vehicleData.getCurrNode(), 'X');
						return true;
					} else {
						// Do Nothing?
						traceOperationException("Abnormal Case: WorkMode#014");
					}
				} else {
					// Waiting!!
					; /*NULL*/
				}
			} else if (vehicleData.getState() == 'L') {
				// 2016.08.16 by KBS : Unload/Load 보고 시점 개선
				if (vehicleData.getTransStatus() == 0x01) {
					trCmd.setDetailState(TRCMD_DETAILSTATE.LOADING);
					addTrCmdToStateUpdateList();
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITSTARTED, 0);

					return true;
				} else {
					// Waiting. (Hoist does not moved yet)
					; /*NULL*/
				}
			} else if (vehicleData.getState() == 'N') {
				// Source와 Dest가 동일 Node인 경우, Unloaded 후 바로 LoadSent.
				// Waiting!!
				; /*NULL*/
			} else {
				// Do Nothing?
				traceOperationException("Abnormal Case: WorkMode#015");
			}
		} else {
			if (vehicleData.getState() == 'L' || vehicleData.getState() == 'O') {
				trCmd.setDetailState(TRCMD_DETAILSTATE.LOADING);
				addTrCmdToStateUpdateList();
				
				// 2012.01.30 by PMM
				if (vehicleData.getState() == 'O') {
					return controlVehicleWithTransferLoadingCommand();
				} else {
					return true;
				}
			} else {
				// Do Nothing?
				traceOperationException("Abnormal Case: WorkMode#016");
			}
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having LOAD_ACCEPTED Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferLoadAcceptedCommand() {
		// 2013.02.01 by KYK
//		assert hasArrivedAtDestNode();
		assert hasArrivedAtDest();
		
		if (vehicleData.isCarrierExist() == false) {
			if (vehicleData.getState() == 'L' || vehicleData.getState() == 'O') {
				trCmd.setDetailState(TRCMD_DETAILSTATE.LOADING);
				addTrCmdToStateUpdateList();
				
				// 2012.01.30 by PMM
				if (vehicleData.getState() == 'O') {
					return controlVehicleWithTransferLoadingCommand();
				} else {
					return true;
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#017");
			}
		} else {
			if (vehicleData.getState() == 'L') {
				// 2012.01.30 by PMM
//				if (getCommandState() == COMMAND_STATE.EXECUTING) {
				// 2016.08.16 by KBS : Unload/Load 보고 시점 개선
				if (vehicleData.getTransStatus() == 0x01) {
					trCmd.setDetailState(TRCMD_DETAILSTATE.LOADING);
					addTrCmdToStateUpdateList();
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITSTARTED, 0);

					return true;
				} else {
					// Waiting. (Hoist does not moved yet)
					; /*NULL*/
				} 
//				} else {
//					traceOperationException("Abnormal Case: WorkMode#018");
//				}
			
			// 2012.03.08 by PMM
			// DestNode Arrive 중, AV -> IDReset이면, Auto Init이고, 이후 LOAD 명령을 전송하고 l을 받으면 LoadAccepted일 수 있음.
//			} else if (vehicleData.getState() == 'A') {
			} else if (vehicleData.getState() == 'A' || vehicleData.getState() == 'I') {
				// Waiting!!
				// A (Arrived), I (Init) 상태일 수 있음. 
				if (getCommandState() == COMMAND_STATE.RESPONDED) {
					// Waiting
					return true;
				} else if (getCommandState() == COMMAND_STATE.READY || getCommandState() == COMMAND_STATE.EXECUTED) {
					trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
					addTrCmdToStateUpdateList();
				} else {
					traceOperationException("Abnormal Case: WorkMode#019-1");
				}
			} else if (vehicleData.getState() == 'N') {
				// Waiting!!
				// N (Unloaded) 상태일 수 있음. (Source와 Dest가 동일 Node인 경우) 
				return true;
			} else {
				traceOperationException("Abnormal Case: WorkMode#019-2");
			}
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having LOADING Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithTransferLoadingCommand() {
		// 2013.02.01 by KYK
//		assert hasArrivedAtDestNode();
		assert hasArrivedAtDest();
		
		if (vehicleData.getState() == 'O') {
			if (vehicleData.isCarrierExist() == false) {
				// 2012.01.30 by PMM
				// Loading 이후 OHT MapUpdate 시,
				// O (Loaded) 보고에도 currCmd가 정리안되는 경우가 발생함.
				// 15:08:45:945 OHTB230> [W] Mode:A Status:O Node(128028,128028,128028) Carrier:0 CmdStatus:E(P:1 C:2 N:0 V:2) Error:0 LocalGroup: PauseType:0 ControlState:START
//				if (getCommandState() == COMMAND_STATE.EXECUTED || getCommandState() == COMMAND_STATE.READY) {
				trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
				trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
				trCmd.setCarrierLoc(trCmd.getDestLoc());
				addTrCmdToStateUpdateList();
				sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 0);
				
				registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
				
				if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
					if (isSTBCUsed()) {
						updateCarrierStateInSTB(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
					} else {
						updateCarrierStateInSTBWithoutSTBC(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
					}
				}
			
				// OperationMode(W->I) by Load Completion
				changeOperationMode(OPERATION_MODE.IDLE, "Load Completion");
				deleteTrCmdFromDB();
				return true;
//				} else if (getCommandState() == COMMAND_STATE.EXECUTING){
//					// 2011.10.21 by PMM Abnormal Case에서 제외.
//					// Waiting.
//					; /*NULL*/
//					
//				} else {
//					// Impossible
//					traceOperationException("Abnormal Case: WorkMode#020");
//				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#021");
			}
		} else {
			// Waiting!!
			; /*NULL*/
		}
		return false;
	}
	
//	private boolean controlVehicleWithTransferLoadedCommand() {
//		if (vehicleData.getState() == 'O') {
//			if (vehicleData.isCarrierExist() == false) {
//				if (getCommandState() == COMMAND_STATE.EXECUTED || getCommandState() == COMMAND_STATE.READY) {
//					trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
//					trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
//					trCmd.setCarrierLoc(trCmd.getDestLoc());
//					addTrCmdToStateUpdateList();
//					sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
//					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
//					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
//					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 0);
//					
//					registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
//					
//					if (isSTBCUsed() && 
//							(getCarrierLocType(trCmd.getDestLoc()) == CARRIERLOC_TYPE.STBPORT || getCarrierLocType(trCmd.getDestLoc()) == CARRIERLOC_TYPE.UTBPORT)) {
//						updateCarrierStateInSTB(OperationConstant.INSTALL, trCmd.getCarrierLoc(), trCmd.getCarrierId(), vehicleData.getRfData());
//					}
//				
//					// OperationMode(W->I) by Load Completion
//					changeOperationMode(OPERATION_MODE.IDLE, "Load Completion");
//					deleteTrCmdFromDB();
//					return true;
//				} else {
//					// Impossible
//				}
//			} else {
//				// Do Nothing?
//			}
//		} else {
//			// Do Nothing?
//		}
//		return false;
//	}
	
	/**
	 * Control Vehicle Having SCAN Command
	 */
	private boolean controlVehicleWithScanCommand() {
		switch (trCmd.getDetailState()) {
			case SCAN_ASSIGNED:
				return controlVehicleWithScanAssignedCommand();
			case SCAN_SENT:
				return controlVehicleWithScanSentCommand();
			case SCAN_ACCEPTED:
				return controlVehicleWithScanAcceptedCommand();
			case SCANNING:
				return controlVehicleWithScanningCommand();
			default:
				//Impossible
				traceOperationException("Abnormal Case: WorkMode#023");
				return false;
		}
	}
	
	/**
	 * Control Vehicle Having SCAN_ASSIGNED Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithScanAssignedCommand() {
		if (vehicleData.getCurrNode().equals(vehicleData.getStopNode()) == false) {
			return true;
		}
		
		if (vehicleData.isCarrierExist() == false) {
			if (vehicleData.getState() == 'I' || vehicleData.getState() == 'A' || vehicleData.getState() == 'O' || vehicleData.getState() == 'F') {
				if (getCommandState() == COMMAND_STATE.READY || getCommandState() == COMMAND_STATE.EXECUTED) {
					if (vehicleData.isAvRetryWait() == false) {
						trCmd.setState(TRCMD_STATE.CMD_SCANNING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.SCAN_SENT);
						addTrCmdToStateUpdateList();
						makeVehicleCommandId();
						// 2013.02.22 by KYK
						sendScanCommand(getVehicleCommCommand().getCommandId(), 'X');
//						sendScanCommand(getVehicleCommCommand().getCommandId(), vehicleData.getCurrNode(), 'X');
						return true;
					} else {
						traceOperationException("Abnormal Case: WorkMode#024");
					}
				} else {
					traceOperationException("Abnormal Case: WorkMode#025");
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#026");
			}
		} else {
			traceOperationException("Abnormal Case: WorkMode#027");
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having SCAN_SENT Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithScanSentCommand() {
		if (vehicleData.isCarrierExist() == false) {
			if (vehicleData.getState() == 'I' || vehicleData.getState() == 'A' || vehicleData.getState() == 'O' || vehicleData.getState() == 'F') {
				if (getCommandState() == COMMAND_STATE.TIMEOUT) {
					if (vehicleData.isAvRetryWait() == false) {
						// 2013.02.22 by KYK
						sendScanCommand(getVehicleCommCommand().getCommandId(), 'X');
//						sendScanCommand(getVehicleCommCommand().getCommandId(), vehicleData.getCurrNode(), 'X');
						return true;
					} else {
						//Impossible
						traceOperationException("Abnormal Case: WorkMode#028");
					}
				} else {
					traceOperationException("Abnormal Case: WorkMode#029");
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#030");
			}
		} else {
			traceOperationException("Abnormal Case: WorkMode#031");
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having SCAN_ACCEPTED Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithScanAcceptedCommand() {
		if (vehicleData.isCarrierExist()) {
			if (vehicleData.getState() == 'R') {
				trCmd.setDetailState(TRCMD_DETAILSTATE.SCANNING);
				addTrCmdToStateUpdateList();
				return true;
			} else {
				traceOperationException("Abnormal Case: WorkMode#032");
			}
		} else {
			if (vehicleData.getState() == 'R') {
				if (getCommandState() == COMMAND_STATE.EXECUTING) {
					trCmd.setDetailState(TRCMD_DETAILSTATE.SCANNING);
					addTrCmdToStateUpdateList();
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCANNING, 0);
					return true;
				} else {
					traceOperationException("Abnormal Case: WorkMode#033");
				}
			} else if (vehicleData.getState() == 'F') {
				if (getCommandState() == COMMAND_STATE.EXECUTED) {
					if ((vehicleData.getRfData() != null && vehicleData.getRfData().length() == 0) ||
							"EMPTY".equals(vehicleData.getRfData())) {
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCANNING, 0);
						trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
						trCmd.setDetailState(TRCMD_DETAILSTATE.SCANNED);
						trCmd.setCarrierLoc(trCmd.getDestLoc());
						addTrCmdToStateUpdateList();
						sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCAN_COMPLETED, 22);
						
						// OperationMode(W->I) by Abnormal Scan Completion
						changeOperationMode(OPERATION_MODE.IDLE, "Abnormal Scan Completion");
						return true;
						
					} else {
						traceOperationException("Abnormal Case: WorkMode#034");
					}
				} else {
					traceOperationException("Abnormal Case: WorkMode#035");
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#036");
			}
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having SCANNING Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithScanningCommand() {
		if (vehicleData.isCarrierExist()) {
			if (vehicleData.getState() == 'R') {
				trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
				addTrCmdToStateUpdateList();
				return true;
			} else {
				// Impossible
				traceOperationException("Abnormal Case: WorkMode#037. VehicleState:" + vehicleData.getState());
			}
		} else {
			switch (vehicleData.getState()) {
				case 'R':
					// Scanning
					; /*NULL*/
					break;
				case 'F':
					if (getCommandState() == COMMAND_STATE.READY || getCommandState() == COMMAND_STATE.EXECUTED) {
						trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
						trCmd.setDetailState(TRCMD_DETAILSTATE.SCANNED);
						trCmd.setCarrierLoc(trCmd.getDestLoc());
						addTrCmdToStateUpdateList();
						if (trCmd.getCarrierId().equals(vehicleData.getRfData())) {
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCAN_COMPLETED, 0);
							// OperationMode(W->I) by Scan Completion
							changeOperationMode(OPERATION_MODE.IDLE, "Scan Completion");
							
						} else if ((vehicleData.getRfData() != null && vehicleData.getRfData().length() == 0) ||
								"EMPTY".equals(vehicleData.getRfData())) {
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCAN_COMPLETED, 22);
							// OperationMode(W->I) by Abnormal Scan Completion (EMPTY)
							changeOperationMode(OPERATION_MODE.IDLE, "Abnormal Scan Completion (EMPTY)");
							
						} else if ("ERROR".equals(vehicleData.getRfData())) {
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCAN_COMPLETED, 1);
							String unknownCarrierID = "";
							
							// OperationMode(W->I) by Abnormal Scan Completion (ERROR)
							changeOperationMode(OPERATION_MODE.IDLE, "Abnormal Scan Completion (ERROR)");
							trCmd.setCarrierId(unknownCarrierID);
							addTrCmdToStateUpdateList();
							
						} else {
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.SCAN_COMPLETED, 24);
							trCmd.setCarrierId(vehicleData.getRfData());
							addTrCmdToStateUpdateList();
							// OperationMode(W->I) by Abnormal Scan Completion
							changeOperationMode(OPERATION_MODE.IDLE, "Abnormal Scan Completion (" + vehicleData.getRfData() + ")");
						}
						return true;
					} else {
						traceOperationException("Abnormal Case: WorkMode#038");
					}
					break;
				default:
					// Impossible
					traceOperationException("Abnormal Case: WorkMode#039. VehicleState:" + vehicleData.getState());
					break;
			}
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having STAGE Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithStageCommand() {
		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE);
		
		// 2011.10.20 by PMM
		// STAGE_NOBLOCKING 처리 부분 수정.
		// 2013.02.01 by KYK
//		if (hasArrivedAtSourceNode()) {
		if (hasArrivedAtSource()) {
			switch (trCmd.getState()) {
				case CMD_STAGING: {
					// 2014.03.21 by MYM : [Stage NBT,WTO 기준 변경]
//					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_WAITING) {
//						changeOperationMode(OPERATION_MODE.IDLE, "STAGE Wait");
					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_NOBLOCKING) {
						changeOperationMode(OPERATION_MODE.IDLE, "STAGE Noblocking");
					}
					break;
				}
				case CMD_WAITING: {
					trCmd.setState(TRCMD_STATE.CMD_STAGING);
					trCmd.setDetailState(TRCMD_DETAILSTATE.STAGE_NOBLOCKING);
					addTrCmdToStateUpdateList();
					traceOperationException("Abnormal Case: WorkMode#040");
					break;
				}
				default:
					traceOperationException("Abnormal Case: WorkMode#041");
					break;
			}
		} else {
			traceOperationException("Abnormal Case: WorkMode#042");
			// 2013.02.15 by KYK
			String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
//			vehicleData.setTargetNode(trCmd.getSourceNode());
			vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
			addVehicleToUpdateList();
			return searchVehiclePath(trCmd.getSourceNode(), TrCmdConstant.UNLOAD, false);
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having MAPMAKE Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithMapMakeCommand() {
		switch (trCmd.getDetailState()) {
			case MAPMAKE_ASSIGNED:
			{
				if (vehicleData.getState() == 'I' || vehicleData.getState() == 'A' || vehicleData.getState() == 'N' || 
						vehicleData.getState() == 'O' || vehicleData.getState() == 'F') {
					if (getCommandState() == COMMAND_STATE.READY || getCommandState() == COMMAND_STATE.EXECUTED) {
						makeVehicleCommandId();
						sendMapMakeCommand(getVehicleCommCommand().getCommandId(), trCmd.getDestNode());
						trCmd.setState(TRCMD_STATE.CMD_MAPMAKING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMAKE_SENT);
						addTrCmdToStateUpdateList();
						return true;
					} else if (getCommandState() == COMMAND_STATE.TIMEOUT) {
						sendMapMakeCommand(getVehicleCommCommand().getCommandId(), trCmd.getDestNode());
						trCmd.setState(TRCMD_STATE.CMD_MAPMAKING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMAKE_SENT);
						addTrCmdToStateUpdateList();
						return true;
					} else {
						// ??
						traceOperationException("Abnormal Case: WorkMode#043");
					}
				} else {
					// ??
					traceOperationException("Abnormal Case: WorkMode#044");
				}
				break;
			}	
			case MAPMAKE_SENT:
			{
				if (getCommandState() == COMMAND_STATE.EXECUTING) {
					trCmd.setState(TRCMD_STATE.CMD_MAPMAKING);
					trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMAKING);
					addTrCmdToStateUpdateList();
					return true;
				}
				break;
			}
			case MAPMAKE_ACCEPTED:
			{
				trCmd.setState(TRCMD_STATE.CMD_MAPMAKING);
				trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMAKING);
				addTrCmdToStateUpdateList();
				return true;
			}
			case MAPMAKING:
			case MAPMADE:
			{
				// 2013.02.01 by KYK
//				if (hasArrivedAtDestNode()) {
				if (hasArrivedAtDest()) {
					if (getCommandState() == COMMAND_STATE.EXECUTED) {
						trCmd.setState(TRCMD_STATE.CMD_MAPMAKING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.MAPMADE);
						addTrCmdToStateUpdateList();
					}
					
					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.MAPMADE) {
						// 2012.01.03 by PMM
						registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
						
						// OperationMode(W->I) by MapMake Completed
						changeOperationMode(OPERATION_MODE.IDLE, "MapMake Completed");
						deleteTrCmdFromDB();
						return true;
					}
				} else {
					// OperationMode(W->I) by Mapmaking Started.
					changeOperationMode(OPERATION_MODE.IDLE, "Mapmaking Started.");
				}
				break;
			}
			default:
				break;
		}
		return false;
	}
	
	/**
	 * Control Vehicle Having PATROL Command
	 * 
	 * @return
	 */
	private boolean controlVehicleWithPatrolCommand() {
		switch (trCmd.getDetailState()) {
			case PATROL_ASSIGNED:
			{
				if (vehicleData.getState() == 'I' || vehicleData.getState() == 'A' || vehicleData.getState() == 'N' || 
						vehicleData.getState() == 'O' || vehicleData.getState() == 'F') {
					vehicleData.resetVehicleLocusList();
					operation.updateVehicleLocusToDB();
					if (getCommandState() == COMMAND_STATE.READY || getCommandState() == COMMAND_STATE.EXECUTED) {
						makeVehicleCommandId();
						sendPatrolCommand(getVehicleCommCommand().getCommandId(), trCmd.getDestNode(), trCmd.getPatrolMode());
						trCmd.setState(TRCMD_STATE.CMD_PATROLLING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.PATROL_SENT);
						addTrCmdToStateUpdateList();
						return true;
					} else {
						traceOperationException("Abnormal Case: WorkMode#045");
					}
				} else {
					traceOperationException("Abnormal Case: WorkMode#046");
				}
				break;
			}	
			case PATROL_SENT:
			{
				if (getCommandState() == COMMAND_STATE.EXECUTING) {
					trCmd.setState(TRCMD_STATE.CMD_PATROLLING);
					trCmd.setDetailState(TRCMD_DETAILSTATE.PATROLLING);
					addTrCmdToStateUpdateList();
					
					// OperationMode(W->I) by Patrolling Started.
					changeOperationMode(OPERATION_MODE.IDLE, "Patrolling Started.");
					return true;
				} else if (getCommandState() == COMMAND_STATE.TIMEOUT) {
					sendPatrolCommand(getVehicleCommCommand().getCommandId(), trCmd.getDestNode(), trCmd.getPatrolMode());
					return true;
				}
				break;
			}
			case PATROL_ACCEPTED:
			{
				trCmd.setState(TRCMD_STATE.CMD_PATROLLING);
				trCmd.setDetailState(TRCMD_DETAILSTATE.PATROLLING);
				addTrCmdToStateUpdateList();
				
				// OperationMode(W->I) by Patrolling Started.
				changeOperationMode(OPERATION_MODE.IDLE, "Patrolling Started.");
				return true;
			}
			case PATROLLING:
			case PATROLLED:
			{
				// 2013.02.01 by KYK
//				if (hasArrivedAtDestNode()) {
				if (hasArrivedAtDest()) {
					if (getCommandState() == COMMAND_STATE.EXECUTED) {
						trCmd.setState(TRCMD_STATE.CMD_PATROLLING);
						trCmd.setDetailState(TRCMD_DETAILSTATE.PATROLLED);
						addTrCmdToStateUpdateList();
					}
					
					if (trCmd.getDetailState() == TRCMD_DETAILSTATE.PATROLLED) {
						// 2012.01.03 by PMM
						registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
						
						// 2015.12.21 by KBS : Patrol VHL 기능 추가
						vehicleData.setRepathSearchNeededByPatrolVHL(true);
			
						// OperationMode(W->I) by Patrol Completed
						changeOperationMode(OPERATION_MODE.IDLE, "Patrol Completed.");
						deleteTrCmdFromDB();
						return true;
					}
				} else {
					// 2011.12.30 by PMM
					// OperationMode(W->I) by Patrolling Started.
					changeOperationMode(OPERATION_MODE.IDLE, "Patrolling Started.");
				}
				break;
			}
			default:
				break;
		}
		return false;
	}
	
	/**
	 * 
	 * @return
	 */
	private boolean controlVehicleWithAbortCommand() {
		// Work Mode에서 ABORT이면 Idle로 전환.
		// OperationMode(W->I) by ABORT
		changeOperationMode(OPERATION_MODE.IDLE, "ABORT");
		return true;
	}
	

	/**
	 * Check Exceptional Condition for TRANSFER Command
	 * 
	 * @param node
	 * @return
	 */
	private boolean checkExceptionalConditionForTransferCommand(String node) {
		if (hasArrivedAtStopNode() == false) {
			return true;
		}
		
		// StopNode should be SourceNode or DestNode.
		if (vehicleData.getStopNode().equals(node) == false) {
			// OperationMode(W->I) by Wrong StopNode
			changeOperationMode(OPERATION_MODE.IDLE, "Wrong StopNode");
			return true;
		}

		if (vehicleData.isActionHold()) {
			// OperationMode(W->I) by Vehicle ActionPaused
			changeOperationMode(OPERATION_MODE.IDLE, "Vehicle ActionHold");
			return true;
		}

		if (trCmd.isPause()) {
			// OperationMode(W->I) by TrCmd Paused
			changeOperationMode(OPERATION_MODE.IDLE, "TrCmd Paused");
			return true;
		}

		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT) {
			// OperationMode(W->I) by ABORT Command
			changeOperationMode(OPERATION_MODE.IDLE, "ABORT Command");
			return true;
		}
		
		return false;
	}
	
	/**
	 * Is UNLOAD Command Sendable?
	 * 
	 * @return
	 */
	private boolean isUnloadCommandSendable() {
		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER);
		
		// 2011.11.02 by PMM
		// Vehicle ActionHold이면 UNLOAD 명령 전송하지 않음.
		if (vehicleData.isActionHold() == false) {
			if (vehicleData.isCarrierExist() == false) {
				if (vehicleData.getState() == 'I' || vehicleData.getState() == 'A' || 
						vehicleData.getState() == 'O' || vehicleData.getState() == 'F') {
					if (getCommandState() == COMMAND_STATE.READY || getCommandState() == COMMAND_STATE.EXECUTED) {
						if (vehicleData.isAvRetryWait() == false) {
							return true;
						}
					} else if (getCommandState() == COMMAND_STATE.TIMEOUT) {
						// 2013.02.22 by KYK : ??
						if (vehicleData.getCurrNode().equals(vehicleData.getTargetNode())) {
							if (vehicleData.isAvRetryWait() == false) {
								return true;
							}
						}
					} else if (getCommandState() == COMMAND_STATE.SENT || getCommandState() == COMMAND_STATE.RESPONDED) {
						// Waiting
					} else {
						traceOperationException("Abnormal Case: WorkMode#047");
					}
				} else {
					traceOperationException("Abnormal Case: WorkMode#048");
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#049");
			}
		} else {
			// Vehicle ActionHold!!
			; /*NULL*/
		}
		return false;
	}
	
	/**
	 * Is LOAD Command Sendable?
	 * 
	 * @return
	 */
	private boolean isLoadCommandSendable() {
		assert (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER);
		// 2011.11.02 by PMM
		// Vehicle ActionHold이면 LOAD 명령 전송하지 않음.
		if (vehicleData.isActionHold() == false) {
			if (vehicleData.isCarrierExist()) {
				if (vehicleData.getState() == 'A' || vehicleData.getState() == 'N' || 
						vehicleData.getState() == 'I' || vehicleData.getState() == 'O' || vehicleData.getState() == 'F') {
					if (getCommandState() == COMMAND_STATE.EXECUTED || getCommandState() == COMMAND_STATE.READY) {
						if (vehicleData.isAvRetryWait() == false) {
							return true;
						}
					} else if (getCommandState() == COMMAND_STATE.TIMEOUT) {
						// 2013.02.22 by KYK : ??
						if (vehicleData.getCurrNode().equals(vehicleData.getTargetNode())) {
							if (vehicleData.isAvRetryWait() == false) {
								return true;
							}
						}
					} else if (getCommandState() == COMMAND_STATE.SENT || getCommandState() == COMMAND_STATE.RESPONDED) {
						// Waiting
					} else {
						traceOperationException("Abnormal Case: WorkMode#050");
					}
				} else {
					traceOperationException("Abnormal Case: WorkMode#051");
				}
			} else {
				traceOperationException("Abnormal Case: WorkMode#052");
			}
		} else {
			// Vehicle ActionHold!!
			; /*NULL*/
		}
		return false;
	}
}
