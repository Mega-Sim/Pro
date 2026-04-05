package com.samsung.ocs.operation.mode;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.TrCmdConstant;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.OcsInfoConstant.LOCALGROUP_CLEAROPTION;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.manager.impl.VehicleErrorManager;
import com.samsung.ocs.manager.impl.model.Block;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.VehicleData;
import com.samsung.ocs.operation.Operation;
import com.samsung.ocs.operation.constant.OperationConstant;
import com.samsung.ocs.operation.constant.ResultCode;
import com.samsung.ocs.operation.constant.MessageItem.EVENT_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant.AUTORECOVERY_BUT_NOTAVEXIST_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant.COMMAND_STATE;
import com.samsung.ocs.operation.constant.OperationConstant.JOB_TYPE;
import com.samsung.ocs.operation.constant.OperationConstant.OPERATION_MODE;

/**
 * SleepMode Class, OCS 3.0 for Unified FAB
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

public class SleepMode extends OperationModeImpl {
	private VehicleErrorManager vehicleErrorManager = null;
	
	private static final String VEHICLEAVHISTORY_TRACE = "VehicleAVHistory";
	
	/**
	 * Constructor of SleepMode class.
	 */
	public SleepMode(Operation operation) {
		super(operation);
		this.vehicleErrorManager = VehicleErrorManager.getInstance(null, null, false, false, 0);
	}
	
	@Override
	public OPERATION_MODE getOperationMode() {
		return OPERATION_MODE.SLEEP;
	}
	
	/**
	 * Control Vehicle in SleepMode
	 */
	@Override
	public boolean controlVehicle() {
		trCmd = getTrCmd();
		
		if (ocsInfoManager.isLocalOHTUsed()) {
			clearVehicleLocalGroupInfo(LOCALGROUP_CLEAROPTION.MANUAL_VHL);
		}
		
		// 2020.05.11 by YSJ (OHT Auto Change)
		if (vehicleData.getRequestedType() == REQUESTEDTYPE.VEHICLEAUTO) {
			sendVehicleAutoCommand();
		}
		
		// 2012.11.28 by MYM : SleepMode인 경우 Vehicle Requested 정보 Reset 추가
		if (vehicleData.getRequestedType() != REQUESTEDTYPE.NULL) {
			resetVehicleRequestedInfo();
		}
		
		if (checkCarrierStatus() == false) {
			return false;
		}
		
		if (vehicleData.getVehicleMode() == 'A' && vehicleData.getState() == 'V') {
			// 2011.11.01 by PMM
			// 기 전송 여부를 sendIDResetCommand() 내부에서 체크
			sendIDResetCommand();
		}
		
		// 2011.11.21 by PMM
		if (isAlarmRegistered()) {
			unregisterAlarmInSleepMode();
		}
		
		if (isMapMakeCommand()) {
			return controlVehicleWithMapMakeCommand();
		} else if (isPatrolCommand()) {
			return controlVehicleWithPatrolCommand();
		// 2013.10.07 by KYK	
//		} else if (isManualInit()) {
//			return controlManualVehicleInInitState();
		} else if (isManualNotError()) {
			return controlManualVehicleInNotErrorState();
		} else if (isManualError()) {
			return controlManualVehicleInErrorState();
		} else if (isAutoButNotAutoRecovery()) {
			return controlAutoVehicleNotInAutoRecoveryState();
		} else if (isAutoAutoRecoveryButNotAVExist()) {
			return controlAutoVehicleInAutoRecoveryState();
		} else {
			//return controlVehicleWithAbnormalState();
			return false;
		}
	}
	
	/**
	 * 2012.11.28 by MYM : SleepMode인 경우 Vehicle Requested 정보 Reset 추가
	 */
	private void resetVehicleRequestedInfo() {
		switch (vehicleData.getRequestedType()) {
		case LOCATE:
			updateRequestedCommandReset(REQUESTEDTYPE.LOCATE_RESET, "SleepMode");
			break;
		// 2014.02.21 by MYM : [Stage Locate 기능]
		case STAGE:
		case STAGENOBLOCK:
		case STAGEWAIT:
			resetStageRequest("SleepMode");
			break;
		case MOVE:
			updateRequestedCommandReset(REQUESTEDTYPE.MOVE_RESET, "SleepMode");
			break;
		case ZONEMOVE:
			updateRequestedCommandReset(REQUESTEDTYPE.ZONEMOVE_RESET, "SleepMode");
			break;
		case YIELD:
			updateRequestedCommandReset(REQUESTEDTYPE.YIELD_RESET, "SleepMode");
			break;
		case VEHICLEAUTO:
			updateRequestedCommandReset(REQUESTEDTYPE.VEHICLEAUTO_RESET, "SleepMode");
			break;
		}
	}
	
	/**
	 * Check CarrierStatus
	 * 
	 * @return
	 */
	private boolean checkCarrierStatus() {
		// OHT Manual 상태에서
		if (vehicleData.getVehicleMode() == 'M') {
			if (trCmd == null) {
				// 반송명령이 없는 경우.
				if (vehicleData.isCarrierExist()) {
					// Abnormal.
					//	-> Auto로 전환 & 작업 할당 시, 해당 OHT에 E-Stop으로 처리.
					//	-> IdleMode에서 처리.
				} else {
					// Normal.
					; /*NULL*/
				}
			} else {
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER ||
						// 2011.11.01 by PMM
						// ABORTed TrCmd를 가진 VHL이 Manual 상태에서 Carrier가 없어진 경우에도 비정상 완료 보고 후 TrCmd 삭제
						trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT) {
					
					// 2011.12.09 by PMM
					if (isCarrierUnexpected()) {
						// UNLOAD 이전
						if (vehicleData.isCarrierExist()) {
							// Abnormal.
							//	-> Auto로 전환 시, 해당 OHT에 E-Stop으로 처리.
							//	-> IdleMode에서 처리.
						} else {
							// Normal.
							; /*NULL*/
						}
					}
					// 2011.12.09 by PMM
					if (isCarrierExpectedForAbnormalStatus()) {
						// UNLOAD 이후
						if (vehicleData.isCarrierExist()) {
							// Normal.
						} else {
							// Abnormal.
							// 2013.02.08 by KYK : ?
//							if (vehicleData.getCurrNode().equals(trCmd.getDestNode())) {
							if (hasArrivedAtDest()) {
								// CarrierLoc은 DestLoc으로
								// 정상 반송 완료 보고 및 TrCmd 삭제.
								// LoadedTime, DeletedTime 등록.
								trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
								trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
								trCmd.setCarrierLoc(trCmd.getDestLoc());
								trCmd.setDeletedTime(getCurrDBTimeStr());
								addTrCmdToStateUpdateList();
								
								sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
								
								// 2011.11.02 by PMM
								// 비정상적으로 Carrier가 사라진 경우, DEPOSIT관련 보고하지 않음.
//								sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
								sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
								// 정상 반송 완료 보고
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 0);
								
								registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
								
								if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
									if (isSTBCUsed()) {
										updateCarrierStateInSTB(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
									} else {
										updateCarrierStateInSTBWithoutSTBC(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
									}
								}
								
								deleteTrCmdFromDB();
								traceOperation("TrCmd Completed by CarrierRemoved (Manual VHL) at DestNode.");
							} else {
								// CarrierLoc은 DestLoc으로
								// 비정상 반송 완료 보고 및 TrCmd 삭제.
								// LoadedTime, DeletedTime 등록.
								trCmd.setCarrierLoc(trCmd.getDestLoc());
								trCmd.setDeletedTime(getCurrDBTimeStr());
								addTrCmdToStateUpdateList();
								
								sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
								// 2011.11.02 by PMM
								// 비정상적으로 Carrier가 사라진 경우, DEPOSIT관련 보고하지 않음.
//								sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
								sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
								// 비정상 반송 완료 보고
								// 2012.11.30 by KYK : ResultCode 세분화
//								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_UNLOADED_BUT_CARRIERNOTEXIST);
								
								registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
								deleteTrCmdFromDB();
								traceOperation("TrCmd Deleted by CarrierRemoved (Manual VHL) at Non-DestNode. " + vehicleData.getCurrNode());
							}
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * Unregister Alarm in Sleep Mode
	 */
	// 2011.11.21 by PMM
	private void unregisterAlarmInSleepMode() {
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
			case OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER:
				unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_WITHSENSED_GOCOMMAND_TIMEOVER);
				break;
			case OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER:
				unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER);
				break;
			case OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER:
				unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER);
				break;
			case OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER:
				unregisterAlarm(OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER);
				break;
			default:
				break;
		}
	}
	
	/**
	 * Control MapMaking Vehicle in SleepMode
	 * 
	 * @return
	 */
	private boolean controlVehicleWithMapMakeCommand() {
		assert isMapMakeCommand();
		registerTrCompletionHistory(REQUESTEDTYPE.MAPMAKE.toConstString());
		deleteTrCmdFromDB();
		traceOperation("MapMake Command Deleted by Vehicle ManualMode.");
		return true;
	}
	
	/**
	 * Control Patrollinging Vehicle in SleepMode
	 * 
	 * @return
	 */
	private boolean controlVehicleWithPatrolCommand() {
		assert isPatrolCommand();

		registerTrCompletionHistory(REQUESTEDTYPE.PATROL.toConstString());
		deleteTrCmdFromDB();
		traceOperation("Patrol Command Deleted by Vehicle ManualMode.");
		return true;
	}
	
	/**
	 * Control Manual Vehicle in Init State
	 * 
	 * @return
	 */
	// 2013.10.07 by KYK
//	private boolean controlManualVehicleInInitState() {
//		assert isManualInit();
	private boolean controlManualVehicleInNotErrorState() {
		assert isManualNotError();
		
		// 2011.10.12 by PMM
		// 반송 테스트 중 MI에서 StopNode Update 안되어 MI에서 StopNode를 CurrNode로 reset 요청받음.
		// TargetNode는 유지.
		if (isValidNodeUpdated()) {
			// 2012.07.11 by MYM : DriveNodeList의 StopNode로 업데이트 하도록 함. 
			// 배경 : DriveNodeList는 정리 안된 상태에서 currNode를 StopNode로 업데이트 하면 안됨. -> Operation 메모리와 GUI 보여지는게 다를 수 있음.
//			vehicleData.setStopNode(vehicleData.getCurrNode());
////			vehicleData.reset(isNearByDrive, nodeManager.getNode(vehicleData.getCurrNode()));
//			addVehicleToUpdateList();
			Node stopNode = vehicleData.getDriveStopNode();
			if (stopNode != null && vehicleData.getStopNode().equals(stopNode.getNodeId()) == false) {
				// 2013.02.15 by KYK : ??
//				vehicleData.setStopNode(stopNode.getNodeId());
				vehicleData.setStop(stopNode.getNodeId(), "");
				addVehicleToUpdateList();
			}
		} else {
			// StopNode 및 Drive reset 하지 않음.
			// 합류에서 충돌 위험.
			; /*NULL*/
		}
		
		if (trCmd != null) {
			// 2014.06.05 By MYM : Stage 도착 후 대기시 Error 발생시 Stage 명령 삭제 처리하도록 함.
			// MI에서 삭제처리를 하나 ME -> MI -> AI 가 순차적으로 처리되지 않을 경우 
			// 즉, ME -> AI일 때는 CMD_PAUSED로 계속 남아서 처리 안되는 경우가 발생할 수 있음.
			// ※ STAGE 할당 방식에서 ERROR 발생시 처리
			//   1) STAGE를 할당 받아 Unload 위치로 이동 중 Error : STAGE 할당 해제
			//   2) STAGE 위치에 도착하여 대기 중 Error : STAGE 삭제 처리
//			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
//				trCmd.setDeletedTime(getCurrDBTimeStr());
//				addTrCmdToStateUpdateList();
//				registerTrCompletionHistory(TRCMD_REMOTECMD.STAGECANCEL.toConstString());
//				cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_MANUAL);
//				deleteStageCmdFromDB();
//				
////				StringBuilder message = new StringBuilder();
////				message.append("Vehicle:").append(vehicleData.getVehicleId());
////				if (trCmd != null) {
////					message.append(", TrCmdId:").append(trCmd.getTrCmdId());
////					message.append(", CarrierId:").append(trCmd.getCarrierId());
////					message.append(", SourceLoc:").append(trCmd.getSourceLoc());
////					message.append(", DestLoc:").append(trCmd.getDestLoc());
////				}
////				registerEventHistory(new EventHistory(EVENTHISTORY_NAME.CURRENT_STAGE_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
////						"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.VEHICLE_MANUAL), false);
//				
//				traceOperation("StageCmd Cancel by Vehicle Manual Mode.");
//				return true;
//			} else 
			if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
				if (vehicleData.isCarrierExist()) {
					switch (trCmd.getDetailState()) {
						case VIBRATION_MONITORING:
						case LOAD_ASSIGNED:
						{
							trCmd.setState(TRCMD_STATE.CMD_PAUSED);
							trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
							addTrCmdToStateUpdateList();
							break;
						}
						default:
							break;
					}
				} else {
					traceOperation("Vibration Cancel by Vehicle Manual Mode.");
					cancelVibrationCommand(EVENTHISTORY_REASON.VEHICLE_MANUAL);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Control Manual Vehicle In Error State
	 * 
	 * @return
	 */
	private boolean controlManualVehicleInErrorState() {
		assert isManualError();
		
		// 2013.09.04 by KYK : Request E-Stop to VHL driving in BlockNode
		Node currNode = vehicleData.getDriveCurrNode();
		if (currNode != null) {
			for (int i = 0; i < currNode.getSectionCount(); i++) {
				Section section = currNode.getSection(i);
				if (section != null) {
					int index = section.getNodeIndex(currNode);
					if (section.getNodeCount() - 1 == index + 1) {
						Node node = section.getNode(index + 1);
						if (node != null && node.isConverge()) {
							Block block = node.getBlock(null);
							if (block != null) {
								VehicleData vehicle = block.getDrivingVehicle();
								if (vehicle != null && vehicle.equals(vehicleData) == false) {
									vehicle.setEStopRequested(true);							
								}
							}
						}
					}
				}
			}
		}
		
		if (vehicleData.isVehicleError() == false) {
			if (trCmd != null) {
				 if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
					if (vehicleData.isCarrierExist()) {
						switch (trCmd.getDetailState()) {
							case VIBRATION_MONITORING:
							case LOAD_ASSIGNED:
							{
								trCmd.setState(TRCMD_STATE.CMD_PAUSED);
								trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
								addTrCmdToStateUpdateList();
								break;
							}
							default:
								break;
						}
					} else {
						traceOperation("Vibration Cancel by Vehicle Manual Mode.");
						cancelVibrationCommand(EVENTHISTORY_REASON.VEHICLE_MANUAL);
					}
					return true;
				} else if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED || 
						trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_ASSIGNED) {
					cancelAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_MANUAL_ERROR, true);
				} else if (trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_NOBLOCKING || 
						trCmd.getDetailState() == TRCMD_DETAILSTATE.STAGE_WAITING) {
					// 2014.06.05 By MYM : Stage 도착 후 대기시 Error 발생시 Stage 명령 삭제 처리하도록 함.
					// MI에서 삭제처리를 하나 ME -> MI -> AI 가 순차적으로 처리되지 않을 경우 
					// 즉, ME -> AI일 때는 CMD_PAUSED로 계속 남아서 처리 안되는 경우가 발생할 수 있음.
					// ※ STAGE 할당 방식에서 ERROR 발생시 처리
					//   1) STAGE를 할당 받아 Unload 위치로 이동 중 Error : STAGE 할당 해제
					//   2) STAGE 위치에 도착하여 대기 중 Error : STAGE 삭제 처리
					trCmd.setDeletedTime(getCurrDBTimeStr());
					addTrCmdToStateUpdateList();
					registerTrCompletionHistory(TRCMD_REMOTECMD.STAGECANCEL.toConstString());
					cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_MANUAL);
					deleteStageCmdFromDB();
					traceOperation("StageCmd Deleted by Vehicle Manual Mode.");
				} else {
					// 2012.11.30 by KYK : 반송ABORT, 작업자ME, 대체반송수신 경우 DestChange 안됨 처리
					// 반송Abort후 ME : ABORT/CMD_PAUSED/UNLOAED 경우, DestChange 안됨
					// 해결 : ABORT 상태에서는 ME처리안함, 대체반송 후 처리하도록함
					if (TRCMD_STATE.CMD_ABORTED == trCmd.getState()) {
						return false;
					}

					trCmd.setState(TRCMD_STATE.CMD_PAUSED);
					// 2012.07.23 by MYM : pauseTrCmd 주석 해제 -> Auto가 된 상태로 재시작시 이전 상태가 Manual Error인지 판단하여 CMD_TRANSFERRING으로 변경 
					// 배경 : 반송명령 수행중 Manual Error 발생 -> Auto Init 상태로 재시작 되었을 때 반송 미수행 현상 발생
					// 2012.03.22 by MYM : ME일때는 Pause 걸지 않도록 수정
					// 배경 : ME 발생시 -> MI, AI 되었을 때 Pause를 풀어주지 않아 Load PathSearch가 안되는 현상 발생
					// 2012.03.16 by PMM
					// CMD_PAUSED인 상태로 재시작 시, trCmd.isPause()가 true가 아닌 케이스에 대한 조치
					pauseTrCmd(true, TrCmdConstant.VEHICLE_MANUAL_ERROR, trCmd.getPauseCount());
					
					addTrCmdToStateUpdateList();
					if (trCmd.getDetailState() != TRCMD_DETAILSTATE.UNLOADING) {
//						if (vehicleData.getRequestedData() != null && vehicleData.getRequestedData().length() > 0) {
//							StringBuilder message = new StringBuilder();
//							message.append("Vehicle:").append(vehicleData.getVehicleId());
//							message.append("RequestedData:").append(vehicleData.getRequestedData());
//							
//							registerEventHistory(new EventHistory(EVENTHISTORY_NAME.NEXT_JOB_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
//									"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.VEHICLE_MANUAL_ERROR), false);
//						}
//						updateRequestedCommandReset(REQUESTEDTYPE.TRANSFER_RESET, "Manual Error");
						cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_MANUAL_ERROR);
					}
					
					// 2012.08.30 by PMM: trCmd != null 일때만 보고해야 함.
					//					  Unload/StageAssigned 상태에서는  TRANSFER_PAUSED 보고 필요없음.
					//					  Manual Error일 때만 TRANSFER_PAUSED 보고하고, 다른 pauseTrCmd()는 OCS 자체 제어용.
					// 2012.08.08 by KYK : [TransferPaused] 
					// 변경전 : Operation TransferPaused 별도요청없음 (IBSEM setAlarmReport 처리시 TransferPaused 같이 보고함) -> 보고 아이템 누락 발생함
					// 변경후 : Operation TransferPaused 개별요청처리 (IBSEM setAlarmReport 처리시 TransferPaused 처리 부분도 제거함)
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_PAUSED, 0);
				}
			} else {
//				if (vehicleData.getRequestedData() != null && vehicleData.getRequestedData().length() > 0) {
//					StringBuilder message = new StringBuilder();
//					message.append("Vehicle:").append(vehicleData.getVehicleId());
//					message.append("RequestedData:").append(vehicleData.getRequestedData());
//					
//					registerEventHistory(new EventHistory(EVENTHISTORY_NAME.NEXT_JOB_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
//							"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.VEHICLE_MANUAL_ERROR), false);
//				}
//				updateRequestedCommandReset(REQUESTEDTYPE.TRANSFER_RESET, "Manual Error");
				cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_MANUAL_ERROR);
			}

			setAlarmReport(vehicleData.getErrorCode());
			vehicleData.setVehicleError(true);
			
			traceOperation("Send SetAlarmReport...< AlarmID:" + vehicleData.getErrorCode() + " >");
			
			//resetVehicleDeadLockInfo(m_strVehicleID);
			
			// 2012.01.02 by PMM
//			if (trCmd == null) {
//				registerVehicleErrorHistory(new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(), 
//						vehicleData.getErrorCode(), vehicleErrorManager.getVehicleErrorText(vehicleData.getErrorCode()), OperationConstant.VEHICLEERROR_ERROR, 
//						 "", "", "", 
//						 "", "", "", 
//						 getCurrDBTimeStr(), "", ""));
//			} else {
//				registerVehicleErrorHistory(new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(), 
//						vehicleData.getErrorCode(), vehicleErrorManager.getVehicleErrorText(vehicleData.getErrorCode()), OperationConstant.VEHICLEERROR_ERROR, 
//						trCmd.getTrCmdId(), trCmd.getDetailState().toConstString(), trCmd.getCarrierId(), 
//						trCmd.getCarrierLoc(), trCmd.getSourceLoc(), trCmd.getDestLoc(), 
//						getCurrDBTimeStr(), "", ""));
//			}
			registerVehicleErrorHistory(vehicleData.getErrorCode(), vehicleErrorManager.getVehicleErrorText(vehicleData.getErrorCode()), OperationConstant.VEHICLEERROR_ERROR);
			
			addVehicleToUpdateList();
			return true;
		}
		
		// 2012.06.11 by PMM
		// ME에서는 CurrNode로부터 5개 노드 혹은 5000 초과인 노드는 정리함.
		Node stopNode = vehicleData.getDriveStopNode();
		if (stopNode != null) {
			if (vehicleData.getStopNode().equals(stopNode.getNodeId()) == false) {
				// 2013.02.15 by KYK : ??
//				vehicleData.setStopNode(stopNode.getNodeId());
				vehicleData.setStop(stopNode.getNodeId(), "");
				addVehicleToUpdateList();
			}
		}
		
		return false;
	}
	
	/**
	 * Control Auto Vehicle Not In AutoRecovery State
	 * 
	 * @return
	 */
	private boolean controlAutoVehicleNotInAutoRecoveryState() {
		assert isAutoButNotAutoRecovery();
		
		setCommandState(COMMAND_STATE.READY);
		vehicleData.setAvExist(false);

		// 에러 조치 시점에 Vehicle의 Stop, TargetNode 변경.
		// 2013.02.15 by KYK
//		vehicleData.setStopNode(vehicleData.getCurrNode());
//		vehicleData.setTargetNode(vehicleData.getCurrNode());
		vehicleData.setStop(vehicleData.getCurrNode(), vehicleData.getCurrStation());
		vehicleData.setTarget(vehicleData.getCurrNode(), vehicleData.getCurrStation());

		// 2012.08.27 by MYM : 위치 변경 
		// 배경 : Unload/Load 위치에서 PIO Error or Error 발생(TrCmd Paused) 후 Auto 전환시 Pause 해제되지 않는 현상 수정
		//       isAutoInitVehicleTriedToUnload, controlAutoInitVehicleTriedToLoad가 먼저 수행한 경우
		//       resetAlarmOfAutoVehicleNotInAutoRecoveryState에서 TrCmd의 STATE가 맞지 않아 Pause 해제가 안될 수 있음.
		if (vehicleData.isVehicleError()) {
			resetAlarmOfAutoVehicleNotInAutoRecoveryState();
		}
		
		// 2014.10.13 by MYM : 장애 지역 우회 기능
		vehicleData.releaseAbnormalSection();
		
		// 2012.06.11
		// Operation의 updateVehicleData 내, updateDriveNode()에서 정리함.
//		vehicleData.reset(isNearByDrive, nodeManager.getNode(vehicleData.getCurrNode()));
		
		if (isAutoInitVehicleTriedToUnload()) {
			controlAutoInitVehicleTriedToUnload();
		} else if (isAutoInitVehicleTriedToLoad()) {
			controlAutoInitVehicleTriedToLoad();
		}
		
		if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
			if (trCmd.getState() == TRCMD_STATE.CMD_PAUSED) {
				if (vehicleData.isCarrierExist()) {
					trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
					trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
					addTrCmdToStateUpdateList();
				}
			}
		}
		
		// OperationMode(S->I) by Auto Init
		changeOperationMode(OPERATION_MODE.IDLE, "Auto Init");
		addVehicleToUpdateList();
		return true;
	}
	
	/**
	 * Control Auto Vehicle in Init State Tried to Unload
	 */
	private void controlAutoInitVehicleTriedToUnload() {
		assert isAutoInitVehicleTriedToUnload();
		
		if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.VIBRATION) {
			if (vehicleData.isCarrierExist()) {
				trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
				if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
					trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
				}
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
			} else {
				trCmd.setState(TRCMD_STATE.CMD_WAITING);
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
			}
			addTrCmdToStateUpdateList();
			return;
		}
		
		if (isUnloadErrorReportUsed()) {
			if (vehicleData.isCarrierExist()) {
				trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
				addTrCmdToStateUpdateList();
				
				sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRECOMPLETED, 0);
				
				if (isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc()))) {
					if (isSTBCUsed()) {
						if (updateCarrierStateInSTB(OperationConstant.REMOVE, trCmd.getSourceLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), "") == false) {
							trCmd.setState(TRCMD_STATE.CMD_PAUSED);
							// 2012.08.08 by KYK : [TransferPaused] 
//							// 변경전 : Operation TransferPaused 별도요청없음 (IBSEM setAlarmReport 처리시 TransferPaused 같이 보고함) -> 보고 아이템 누락 발생함
//							// 변경후 : Operation TransferPaused 개별요청처리 (IBSEM setAlarmReport 처리시 TransferPaused 처리 부분도 제거함)
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_PAUSED, 0);									
							
							setAlarmReport(999);
							clearAlarmReport(999);
							pauseTrCmd(true, TrCmdConstant.CARRIER_MISMATCH, -1);
							
							trCmd.setLastAbortedTime(System.currentTimeMillis());
							trCmd.setState(TRCMD_STATE.CMD_ABORTED);
							trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
							sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
							sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_STBUNLOAD_CARRIERMISMATCH);
							
							// 2013.01.07 by MYM : STB Unload 후 Carrier Mismatch 발생시 알람 표시
							registerAlarm(OcsAlarmConstant.UNLOAD_CARRIER_MISMATCH);
							addTrCmdToStateUpdateList();
							return;
						}
					} else {
						updateCarrierStateInSTBWithoutSTBC(OperationConstant.REMOVE, trCmd.getSourceLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), "");
					}
				}
				trCmd.setLastAbortedTime(System.currentTimeMillis());
				trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
				trCmd.setState(TRCMD_STATE.CMD_ABORTED);
				addTrCmdToStateUpdateList();
				pauseTrCmd(true, TrCmdConstant.UNLOADING_VHL_ERROR, 0);
				
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_UNLOADING_VHL_ERROR_CARRIER_EXIST);
			} else {
				trCmd.setCarrierLoc(trCmd.getSourceLoc());
				addTrCmdToStateUpdateList();
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_UNLOADING_VHL_ERROR_CARRIER_NOT_EXIST);
				
				trCmd.setDeletedTime(getCurrDBTimeStr());
				registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
				deleteTrCmdFromDB();
			}
		} else {
			// 기존 처리
			if (vehicleData.isCarrierExist() == false) {
				trCmd.setState(TRCMD_STATE.CMD_WAITING);
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
				addTrCmdToStateUpdateList();
			} else {
				trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
				
				if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
					trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
				}
				trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
				
				if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.VIBRATION) {
					sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
					sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRECOMPLETED, 0);
					
					if (isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc()))) {
						if (isSTBCUsed()) {
							if (updateCarrierStateInSTB(OperationConstant.REMOVE, trCmd.getSourceLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), "") == false) {
								trCmd.setState(TRCMD_STATE.CMD_PAUSED);
								// 2012.08.08 by KYK : [TransferPaused] 
								// 변경전 : Operation TransferPaused 별도요청없음 (IBSEM setAlarmReport 처리시 TransferPaused 같이 보고함) -> 보고 아이템 누락 발생함
								// 변경후 : Operation TransferPaused 개별요청처리 (IBSEM setAlarmReport 처리시 TransferPaused 처리 부분도 제거함)
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_PAUSED, 0);									
								
								setAlarmReport(999);
								clearAlarmReport(999);
								pauseTrCmd(true, TrCmdConstant.CARRIER_MISMATCH, -1);
								
								trCmd.setLastAbortedTime(System.currentTimeMillis());
								trCmd.setState(TRCMD_STATE.CMD_ABORTED);
								trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
								// 2012.11.30 by KYK : ResultCode 세분화
//								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
								sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_STBUNLOAD_CARRIERMISMATCH);
								
								// 2013.01.07 by MYM : STB Unload 후 Carrier Mismatch 발생시 알람 표시
								registerAlarm(OcsAlarmConstant.UNLOAD_CARRIER_MISMATCH);
							}
						} else {
							updateCarrierStateInSTBWithoutSTBC(OperationConstant.REMOVE, trCmd.getSourceLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), "");
						}
					}
				}
				addTrCmdToStateUpdateList();
			}
		}
	}
	
	/**
	 * Control Auto Vehicle in Init State Tried to Load
	 */
	private void controlAutoInitVehicleTriedToLoad() {
		assert isAutoInitVehicleTriedToLoad();
		
		if (vehicleData.isCarrierExist()) {
			// 2013.02.15 by KYK
//			vehicleData.setTargetNode(trCmd.getDestNode());
			String targetStation = getStationIdAtPort(trCmd.getDestLoc());
			vehicleData.setTarget(trCmd.getDestNode(), targetStation);
			addVehicleToUpdateList();
			trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
			trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
			addTrCmdToStateUpdateList();
		} else {
			trCmd.setCarrierLoc(trCmd.getDestLoc());
			trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
			trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
			addTrCmdToStateUpdateList();
			registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
			
			if (trCmd.getRemoteCmd() != TRCMD_REMOTECMD.VIBRATION) {
				sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 0);
				
				if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
					if (isSTBCUsed()) {
						updateCarrierStateInSTB(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
					} else {
						updateCarrierStateInSTBWithoutSTBC(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), trCmd.getFoupId());
					}
				}
			}
			deleteTrCmdFromDB();

//			// 2013.08.30 by KYK : AL -> ME -> MI (다른위치로 이동하여 재하내려놓음) -> AI 인 경우 비정상완료보고 해야함 (STB에 carrierInstall 하면 안됨)
//			if (hasArrivedAtDest()) {
//				// Normal TransferCompleted
//				trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
//				trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
//				trCmd.setCarrierLoc(trCmd.getDestLoc());
//				trCmd.setDeletedTime(getCurrDBTimeStr());
//				addTrCmdToStateUpdateList();
//				
//				sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);				
//				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
//				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
//				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 0);
//				
//				registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
//				
//				if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
//					if (isSTBCUsed()) {
//						updateCarrierStateInSTB(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData());
//					} else {
//						updateCarrierStateInSTBWithoutSTBC(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData());
//					}
//				}
//				deleteTrCmdFromDB();
//			} else {
//				// Abnormal TransferCompleted
//				trCmd.setCarrierLoc(trCmd.getDestLoc());
//				trCmd.setDeletedTime(getCurrDBTimeStr());
//				addTrCmdToStateUpdateList();
//				
//				sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
//				// 2011.11.02 by PMM
//				// 비정상적으로 Carrier가 사라진 경우, DEPOSIT관련 보고하지 않음.
////				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
//				sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
//				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_UNLOADED_BUT_CARRIERNOTEXIST);
//				
//				registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
//				deleteTrCmdFromDB();
//				traceOperation("TrCmd Deleted by CarrierRemoved (Manual VHL) at Non-DestNode. " + vehicleData.getCurrNode());
//			}
		}
	}
	
	/**
	 * Control Auto Vehicle in AutoRecovery Stage
	 * 
	 * @return
	 */
	private boolean controlAutoVehicleInAutoRecoveryState() {
		assert isAutoAutoRecoveryButNotAVExist();
		
		vehicleData.setAvExist(true);
		addVehicleToUpdateList();

//		sendIDResetCommand();
		
		if (vehicleData.getErrorCode() != 0) {
			// 2012.01.02 by PMM
			traceVehicleAVHistory();
			
			registerVehicleErrorInAutoModeAutoRecoveryState();
		}
		
		if (trCmd != null) {
			if (vehicleData.getNextCmd() != 0) {
				return true;
			}
			
			// 2012.01.27 by PMM
//			if (isAutoAutoRecoveryVehicleTriedToUnload()) {
//				return controlAutoAutoRecoveryVehicleTriedToUnload();
//				
//			} else if (isAutoAutoRecoveryVehicleUnloaded()) {
//				return controlAutoAutoRecoveryVehicleUnloaded();
//				
//			} else if (isAutoAutoRecoveryVehicleTriedToUnloadButToBeAborted()) {
//				return controlAutoAutoRecoveryVehicleTriedToUnloadButToBeAborted();
//				
//			} else if (isAutoAutoRecoveryVehicleTriedToLoad()) {
//				return controlAutoAutoRecoveryVehicleTriedToLoad();
//				
//			} else if (isAutoAutoRecoveryVehicleLoaded()) {
//				return controlAutoAutoRecoveryVehicleLoaded();
//				
//			} else if (isAutoAutoRecoveryVehicleTriedToLoadButToBeAborted()) {
//				return controlAutoAutoRecoveryVehicleTriedToLoadButToBeAborted();
//				
//			} else {
//				traceOperation("Vehicle reported <AV> in wrong state.");
//			}
			switch (getAutoRecoveryButNotAVExistType()) {
				case TRIED_TO_UNLOAD:
					return controlAutoAutoRecoveryVehicleTriedToUnload();
				case UNLOADED:
					return controlAutoAutoRecoveryVehicleUnloaded();
				case TRIED_TO_UNLOAD_BUT_TO_BE_ABORTED:
					return controlAutoAutoRecoveryVehicleTriedToUnloadButToBeAborted();
				case TRIED_TO_LOAD:
					return controlAutoAutoRecoveryVehicleTriedToLoad();
				case TRIED_TO_LOAD_BUT_TO_BE_ABORTED:
					return controlAutoAutoRecoveryVehicleTriedToLoadButToBeAborted();
				case ABNORMAL:
					traceOperation("Vehicle reported <AV> in wrong state.");
					break;
				// 2012.01.27 by PMM
				// Loaded 처리를 하지 못하고 Manual 전환되어 MapUpdate 중 AV 보고한 경우, STB 데이터 유실 발생.
				// Loaded에 대한 처리는 AutoInit 보고 후, Idle 모드 전환 전에 처리함.
//				case LOADED:
//					return controlAutoAutoRecoveryVehicleLoaded();
				case LOADED:
				case NONE:
				default:
					break;
			}
		}
		return false;
	}
	
	/**
	 * Control Auto Vehicle in AutoRecovery State Tried to Unload
	 * 
	 * @return
	 */
	private boolean controlAutoAutoRecoveryVehicleTriedToUnload() {
		pauseTrCmd(true, TrCmdConstant.AUTO_ERROR, trCmd.getPauseCount() + 1);
		vehicleData.setAvRetryWait(true);
		// 2013.02.15 by KYK
//		vehicleData.setTargetNode(trCmd.getSourceNode());
		String targetStation = getStationIdAtPort(trCmd.getSourceLoc());
		vehicleData.setTarget(trCmd.getSourceNode(), targetStation);
		addVehicleToUpdateList();
		
		trCmd.setState(TRCMD_STATE.CMD_WAITING);
		trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOAD_ASSIGNED);
		trCmd.setUnloadAutoRetryCount(trCmd.getUnloadAutoRetryCount() + 1);
		trCmd.setLastAbortedTime(System.currentTimeMillis());
		addTrCmdToStateUpdateList();
		return true;
	}
	
	/**
	 * Control Auto Vehicle in AutoRecovery State Unloaded
	 * 
	 * @return
	 */
	private boolean controlAutoAutoRecoveryVehicleUnloaded() {
		trCmd.setCarrierLoc(vehicleData.getVehicleLoc());
		
		if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
			trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
		}
		
		trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
		
		sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_INSTALLED, 0);
		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_ACQUIRECOMPLETED, 0);
		
		if (isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc()))) {
			if (isSTBCUsed()) {
				if (updateCarrierStateInSTB(OperationConstant.REMOVE, trCmd.getSourceLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), "") == false) {
					trCmd.setState(TRCMD_STATE.CMD_PAUSED);
					// 2012.08.08 by KYK : [TransferPaused] 
					// 변경전 : Operation TransferPaused 별도요청없음 (IBSEM setAlarmReport 처리시 TransferPaused 같이 보고함) -> 보고 아이템 누락 발생함
					// 변경후 : Operation TransferPaused 개별요청처리 (IBSEM setAlarmReport 처리시 TransferPaused 처리 부분도 제거함)
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_PAUSED, 0);									

					setAlarmReport(999);
					clearAlarmReport(999);
					pauseTrCmd(true, TrCmdConstant.CARRIER_MISMATCH, -1);
					
					trCmd.setLastAbortedTime(System.currentTimeMillis());
					trCmd.setState(TRCMD_STATE.CMD_ABORTED);
					trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
					//updateRemoteCmd(TRCMD_REMOTECMD.ABORT);
					
					// 2012.11.30 by KYK : ResultCode 세분화
//					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
					sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_STBUNLOAD_CARRIERMISMATCH);
					
					// 2013.01.07 by MYM : STB Unload 후 Carrier Mismatch 발생시 알람 표시
					registerAlarm(OcsAlarmConstant.UNLOAD_CARRIER_MISMATCH);
				}
			} else {
				updateCarrierStateInSTBWithoutSTBC(OperationConstant.REMOVE, trCmd.getSourceLoc(), trCmd.getCarrierId(), vehicleData.getRfData(), "");
			}
		}

		addTrCmdToStateUpdateList();
		return true;
	}
	
	/**
	 * Control Auto Vehicle in AutoRecovery State Tried to Unload But Aborted
	 * 
	 * @return
	 */
	private boolean controlAutoAutoRecoveryVehicleTriedToUnloadButToBeAborted() {
		if (trCmd.isPause() == false) {
			trCmd.setLastAbortedTime(System.currentTimeMillis());
			trCmd.setState(TRCMD_STATE.CMD_ABORTED);
			trCmd.setDeletedTime(getCurrDBTimeStr());
			addTrCmdToStateUpdateList();
			registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
			
			sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
			
			// 2013.01.03 by MYM : STB or UTB Port인 경우 ErrorCode(851,856)를 추가 확인하여 Double Storage 처리
			// 2012.11.30 by KYK : ResultCode 세분화
//			if (isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc()))) {
			if (isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc())) && vehicleErrorManager.isEmptyRetrievalInSTBorUTBPort(vehicleData.getErrorCode())) {
//				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 22);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_STB_UNLOADFAIL);
			} else {
//				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_EQ_UNLOADFAIL);
			}
			
			StringBuilder message = new StringBuilder();
			message.append("TrCmd is deleted because of EQ PIO Problem: <<");
			message.append(" CommandID: ").append(trCmd.getTrCmdId());
			message.append(", CarrierID:").append(trCmd.getCarrierId());
			message.append(" >>");
			traceOperation(message.toString());
			
			deleteTrCmdFromDB();
		}
		return true;
	}
	
	/**
	 * Control Auto Vehicle in AutoRecovery State Tried to Load
	 * 
	 * @return
	 */
	private boolean controlAutoAutoRecoveryVehicleTriedToLoad() {
		pauseTrCmd(true, TrCmdConstant.AUTO_ERROR, trCmd.getPauseCount() + 1);
		vehicleData.setAvRetryWait(true);
		// 2013.02.15 by KYK
//		vehicleData.setTargetNode(trCmd.getDestNode());
		String targetStation = getStationIdAtPort(trCmd.getDestLoc());
		vehicleData.setTarget(trCmd.getDestNode(), targetStation);
		addVehicleToUpdateList();
		
		if (trCmd.getState() != TRCMD_STATE.CMD_ABORTED) {
			trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
		}
		trCmd.setDetailState(TRCMD_DETAILSTATE.LOAD_ASSIGNED);
		trCmd.setLoadAutoRetryCount(trCmd.getLoadAutoRetryCount() + 1);
		trCmd.setLastAbortedTime(System.currentTimeMillis());
		addTrCmdToStateUpdateList();
		
//		if (vehicleData.getRequestedData() != null && vehicleData.getRequestedData().length() > 0) {
//			StringBuilder message = new StringBuilder();
//			message.append("Vehicle:").append(vehicleData.getVehicleId());
//			message.append("RequestedData:").append(vehicleData.getRequestedData());
//			
//			registerEventHistory(new EventHistory(EVENTHISTORY_NAME.NEXT_JOB_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
//					"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.VEHICLE_AUTO_RECOVERY), false);
//		}
//		updateRequestedCommandReset(REQUESTEDTYPE.TRANSFER_RESET, "Load AutoRetry");
		cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_AUTO_RECOVERY);
		return true;
	}
	
	/**
	 * Control Auto Vehicle in AutoRecovery State Loaded
	 * 
	 * @return
	 */
	// 2012.01.27 by PMM
	// AutoRecovery 중 Loaded 처리는 Auto Init 후에 처리함.
	//
//	private boolean controlAutoAutoRecoveryVehicleLoaded() {
//		trCmd.setState(TRCMD_STATE.CMD_COMPLETED);
//		trCmd.setDetailState(TRCMD_DETAILSTATE.LOADED);
//		trCmd.setCarrierLoc(trCmd.getDestLoc());
//		addTrCmdToStateUpdateList();
//		registerTrCompletionHistory(trCmd.getRemoteCmd().toConstString());
//		
//		sendS6F11(EVENT_TYPE.CARRIER, OperationConstant.CARRIER_REMOVED, 0);
//		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_DEPOSITCOMPLETED, 0);
//		sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
//		sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 0);
//		
//		// 2012.01.26 by KYK
//		// 1월 18일 15:08 HYB00370 케리어 S1BC06L 정상 반송완료 이후 1월 19일 10:00 전산 데이터삭제 
//		// 실물은 S1BC06L 에 존재하고 있으나 전산에서는 데이터가 삭제된 현상이 발생함.
//		if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
//			if (isSTBCUsed()) {
//				updateCarrierStateInSTB(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData());
//			} else {
//				updateCarrierStateInSTBWithoutSTBC(OperationConstant.INSTALL, trCmd.getDestLoc(), trCmd.getCarrierId(), vehicleData.getRfData());
//			}
//		}
//		
//		traceOperation("TrCmd is deleted because of EQ PIO Error.: <<CommandID: " + trCmd.getTrCmdId() + ">>");
//		deleteTrCmdFromDB();
		
//		return true;
//	}
	
	/**
	 * Control Auto Vehicle in AutoRecovery State Tried to Load But Aborted
	 * 
	 * @return
	 */
	private boolean controlAutoAutoRecoveryVehicleTriedToLoadButToBeAborted() {
		if (trCmd.isPause() == false) {
			trCmd.setLastAbortedTime(System.currentTimeMillis());
			trCmd.setState(TRCMD_STATE.CMD_ABORTED);
			trCmd.setDetailState(TRCMD_DETAILSTATE.UNLOADED);
			trCmd.setRemoteCmd(TRCMD_REMOTECMD.ABORT);
			pauseTrCmd(true, TrCmdConstant.AUTO_ERROR, trCmd.getPauseCount() + 1);
			addTrCmdToStateUpdateList();
//			updateRequestedCommandReset(REQUESTEDTYPE.TRANSFER_RESET, "Aborted");
			cancelNextAssignedTrCmd(EVENTHISTORY_REASON.VEHICLE_AUTO_RECOVERY);
			
			// 2012.11.30 by KYK : ABORT시 VehicleUnassigned 보고 추가
			sendS6F11(EVENT_TYPE.VEHICLE, OperationConstant.VEHICLE_UNASSIGNED, 0);
			
			// 2013.01.03 by MYM : STB or UTB Port인 경우 ErrorCode(850,855)를 추가 확인하여 Double Storage 처리
			// 2012.11.30 by KYK : ResultCode 세분화
//			if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
			if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc())) && vehicleErrorManager.isDoubleStorageInSTBorUTBPort(vehicleData.getErrorCode())) {
//				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 21);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_STB_LOADFAIL);
			} else {
//				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, 1);
				sendS6F11(EVENT_TYPE.TRCMD, OperationConstant.TRANSFER_COMPLETED, ResultCode.RESULTCODE_EQ_LOADFAIL);
			}
//			if (vehicleData.getRequestedData() != null && vehicleData.getRequestedData().length() > 0) {
//				StringBuilder message = new StringBuilder();
//				message.append("Vehicle:").append(vehicleData.getVehicleId());
//				message.append("RequestedData:").append(vehicleData.getRequestedData());
//				
//				registerEventHistory(new EventHistory(EVENTHISTORY_NAME.NEXT_JOB_CANCEL, EVENTHISTORY_TYPE.SYSTEM, "", message.toString(), 
//						"", "", EVENTHISTORY_REMOTEID.OPERATION, "", EVENTHISTORY_REASON.VEHICLE_AUTO_RECOVERY), false);
//			}
			addVehicleToUpdateList();
		}
		
		return true;
	}
	
	private boolean isMapMakeCommand() {
		if (trCmd != null &&
				trCmd.getRemoteCmd() == TRCMD_REMOTECMD.MAPMAKE) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isPatrolCommand() {
		if (trCmd != null &&
				trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PATROL) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 2013.10.07 by KYK
	 * @return
	 */
	private boolean isManualNotError() {
		assert !isMapMakeCommand() && !isPatrolCommand();
		if (vehicleData.getVehicleMode() == 'M' && vehicleData.getState() != 'E') {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isManualInit() {
		assert !isMapMakeCommand() && !isPatrolCommand();
		if (vehicleData.getVehicleMode() == 'M' && vehicleData.getState() == 'I') {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isManualError() {
		assert !isMapMakeCommand() && !isPatrolCommand();
		if (vehicleData.getVehicleMode() == 'M' && vehicleData.getState() == 'E') {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isAutoButNotAutoRecovery() {
		assert !isMapMakeCommand() && !isPatrolCommand();
		
		if (vehicleData.getVehicleMode() == 'A' && vehicleData.getState() != 'V') {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isAutoAutoRecoveryButNotAVExist() {
		assert !isMapMakeCommand() && !isPatrolCommand();
		// 2011.10.08 by PMM. Idle로 MOVE 중인 VHL에 대한 AutoPosition 처리를 위해 trCmd NOT NULL 체크 제거
//		if (vehicleData.getVehicleMode() == 'A' && vehicleData.getState() == 'V' && vehicleData.isAvExist() == false && trCmd != null) {
		if (vehicleData.getVehicleMode() == 'A' && vehicleData.getState() == 'V' && vehicleData.isAvExist() == false) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isAutoInitVehicleTriedToUnload() {
		assert isAutoButNotAutoRecovery();
		if (trCmd != null) {
			// 2012.12.06 by MYM : AUTO_ERROR 조건 제거
			// Unloading 중 Carrier O 이 후 AV 발생시 AV 처리(controlAutoVehicleInAutoRecoveryState)에서 Loaded 처리 했던 것을
			// AI 처리(controlAutoVehicleNotInAutoRecoveryState)에서 처리하도록 변경하여 AUTO_ERROR 조건 제거
//			if (TrCmdConstant.AUTO_ERROR.equals(trCmd.getPauseType()) == false) {
//				if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_SENT ||
//						trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED ||
//						trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADING) {
//					return true;
//				}
//			}
			if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_SENT ||
					trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED ||
					trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADING) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isAutoInitVehicleTriedToLoad() {
		assert isAutoButNotAutoRecovery();
		if (trCmd != null) {
			// 2012.12.06 by MYM : AUTO_ERROR 조건 제거
			// Loading 중 Carrier X 이 후 AV 발생시 AV 처리(controlAutoVehicleInAutoRecoveryState)에서 Loaded 처리 했던 것을
			// AI 처리(controlAutoVehicleNotInAutoRecoveryState)에서 처리하도록 변경하여 AUTO_ERROR 조건 제거 
//			if (TrCmdConstant.AUTO_ERROR.equals(trCmd.getPauseType()) == false) {
//				if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_SENT ||
//						trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED ||
//						trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING) {
//					return true;
//				}
//			}
			if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_SENT ||
					trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED ||
					trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING) {
				return true;
			}
		}
		return false;
	}
	
	private AUTORECOVERY_BUT_NOTAVEXIST_TYPE getAutoRecoveryButNotAVExistType() {
		if (trCmd != null) {
			switch (trCmd.getDetailState()) {
			case UNLOAD_SENT:
			case UNLOAD_ACCEPTED:
			case UNLOADING: {
				if (vehicleData.getTargetNode().equals(trCmd.getSourceNode())) {
					// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
//					if (trCmd.getPauseCount() < ocsInfoManager.getUnloadRetryLimit() && 
					if (trCmd.getPauseCount() < getAutoRetryLimitCount(trCmd.getSourceLoc(), JOB_TYPE.UNLOAD) && 
							isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc())) == false) {
						if (vehicleData.isCarrierExist() == false) {
							return AUTORECOVERY_BUT_NOTAVEXIST_TYPE.TRIED_TO_UNLOAD;
						}
					} else {
						if (trCmd.getDetailState() != TRCMD_DETAILSTATE.UNLOAD_SENT) {
							if (vehicleData.isCarrierExist()) {
								return AUTORECOVERY_BUT_NOTAVEXIST_TYPE.UNLOADED;
							} else {
								return AUTORECOVERY_BUT_NOTAVEXIST_TYPE.TRIED_TO_UNLOAD_BUT_TO_BE_ABORTED;
							}
						}
					}
				}
				break;
			}
			case LOAD_SENT:
			case LOAD_ACCEPTED:
			case LOADING: {
				if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
					// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
//					if (trCmd.getPauseCount() < ocsInfoManager.getLoadRetryLimit() &&
					if (trCmd.getPauseCount() < getAutoRetryLimitCount(trCmd.getDestLoc(), JOB_TYPE.LOAD) &&
							isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc())) == false) {
						if (vehicleData.isCarrierExist()) {
							return AUTORECOVERY_BUT_NOTAVEXIST_TYPE.TRIED_TO_LOAD;
						}
					} else {
						if (trCmd.getDetailState() != TRCMD_DETAILSTATE.LOAD_SENT) {
							if (vehicleData.isCarrierExist()) {
								return AUTORECOVERY_BUT_NOTAVEXIST_TYPE.TRIED_TO_LOAD_BUT_TO_BE_ABORTED;
							} else {
								return AUTORECOVERY_BUT_NOTAVEXIST_TYPE.LOADED;
							}
						}
					}
				}
				break;
			}
			default:
				break;
			}
			return AUTORECOVERY_BUT_NOTAVEXIST_TYPE.ABNORMAL;
		}
		return AUTORECOVERY_BUT_NOTAVEXIST_TYPE.NONE;
	}
	
//	private boolean isAutoAutoRecoveryVehicleTriedToUnload() {
//		assert isAutoAutoRecoveryButNotAVExist();
//		assert (trCmd != null && vehicleData.getNextCmd() != 0);
//		
//		if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_SENT ||
//				trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED ||
//				trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADING) {
//			if (vehicleData.getTargetNode().equals(trCmd.getSourceNode())) {
//				if (trCmd.getPauseCount() < ocsInfoManager.getUnloadRetryLimit() && 
//						isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc())) == false) {
//					if (vehicleData.isCarrierExist() == false) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}
//	
//	private boolean isAutoAutoRecoveryVehicleUnloaded() {
//		assert isAutoAutoRecoveryButNotAVExist();
//		assert (trCmd != null && vehicleData.getNextCmd() != 0);
//		
//		if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED ||
//				trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADING) {
//			if (vehicleData.getTargetNode().equals(trCmd.getSourceNode())) {
//				if (trCmd.getPauseCount() >= ocsInfoManager.getUnloadRetryLimit() || 
//						isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc()))) {
//					if (vehicleData.isCarrierExist()) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}
//	
//	private boolean isAutoAutoRecoveryVehicleTriedToUnloadButToBeAborted() {
//		assert isAutoAutoRecoveryButNotAVExist();
//		assert (trCmd != null && vehicleData.getNextCmd() != 0);
//		
//		if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED ||
//				trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADING) {
//			if (vehicleData.getTargetNode().equals(trCmd.getSourceNode())) {
//				if (trCmd.getPauseCount() >= ocsInfoManager.getUnloadRetryLimit() || 
//						isSTBOrUTBPort(getCarrierLocType(trCmd.getSourceLoc()))) {
//					if (vehicleData.isCarrierExist() == false) {
//						if (trCmd.isPause() == false) {
//							return true;
//						}
//					}
//				}
//			}
//		}
//		return false;
//	}
//	
//	private boolean isAutoAutoRecoveryVehicleTriedToLoad() {
//		assert isAutoAutoRecoveryButNotAVExist();
//		assert (trCmd != null && vehicleData.getNextCmd() != 0);
//		
//		if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_SENT ||
//				trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED ||
//				trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING) {
//			if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
//				if (trCmd.getPauseCount() < ocsInfoManager.getLoadRetryLimit()) {
//					if (isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc())) == false) {
//						if (vehicleData.isCarrierExist()) {
//							return true;
//						}
//					}
//				}
//			}
//		}
//		return false;
//	}
//	
//	private boolean isAutoAutoRecoveryVehicleLoaded() {
//		assert isAutoAutoRecoveryButNotAVExist();
//		assert (trCmd != null && vehicleData.getNextCmd() != 0);
//		
//		if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED ||
//				trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING) {
//			if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
//				if (trCmd.getPauseCount() >= ocsInfoManager.getLoadRetryLimit()
//						|| isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
//					if (vehicleData.isCarrierExist() == false) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}
//	
//	private boolean isAutoAutoRecoveryVehicleTriedToLoadButToBeAborted() {
//		assert isAutoAutoRecoveryButNotAVExist();
//		assert (trCmd != null && vehicleData.getNextCmd() != 0);
//
//		if (trCmd.getDetailState() == TRCMD_DETAILSTATE.LOAD_ACCEPTED ||
//				trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING) {
//			if (vehicleData.getTargetNode().equals(trCmd.getDestNode())) {
//				if (trCmd.getPauseCount() >= ocsInfoManager.getLoadRetryLimit()
//						|| isSTBOrUTBPort(getCarrierLocType(trCmd.getDestLoc()))) {
//					if (vehicleData.isCarrierExist()) {
//						return true;
//					}
//				}
//			}
//		}
//		return false;
//	}
	
	/**
	 * Register VehicleError of Auto Vehicle in AutoRecovery State
	 */
	private void registerVehicleErrorInAutoModeAutoRecoveryState(){
		assert isAutoAutoRecoveryButNotAVExist();
		assert (vehicleData.getErrorCode() != 0);
		
		String type = OperationConstant.VEHICLEERROR_AUTO_RETRY;
		if (previousOperationMode == OPERATION_MODE.GO) {
			type = OperationConstant.VEHICLEERROR_AUTO_POSITION;
		}
		
		// 2012.01.02 by PMM
		if (trCmd == null) {
			type = OperationConstant.VEHICLEERROR_AUTO_POSITION;
		} else {
			if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADING ||
					trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING) {
				type = OperationConstant.VEHICLEERROR_AUTO_RETRY;
			}
		}
		registerVehicleErrorHistory(vehicleData.getErrorCode(), vehicleErrorManager.getVehicleErrorText(vehicleData.getErrorCode()), type);
		
//		if (trCmd == null) {
//			type = OperationConstant.VEHICLEERROR_AUTO_POSITION;
//			registerVehicleErrorHistory(new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(), 
//					vehicleData.getErrorCode(), vehicleErrorManager.getVehicleErrorText(vehicleData.getErrorCode()), type, 
//					 "", "", "", 
//					 "", "", "", 
//					 getCurrDBTimeStr(), "", ""));
//		} else {
//			if (trCmd.getDetailState() == TRCMD_DETAILSTATE.UNLOADING ||
//					trCmd.getDetailState() == TRCMD_DETAILSTATE.LOADING) {
//				// 2011.11.04 by PMM
////				type = OperationConstant.VEHICLEERROR_AUTO_POSITION;
//				type = OperationConstant.VEHICLEERROR_AUTO_RETRY;
//			}
//			registerVehicleErrorHistory(new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(), 
//					vehicleData.getErrorCode(), vehicleErrorManager.getVehicleErrorText(vehicleData.getErrorCode()), type, 
//					trCmd.getTrCmdId(), trCmd.getDetailState().toConstString(), trCmd.getCarrierId(), 
//					trCmd.getCarrierLoc(), trCmd.getSourceLoc(), trCmd.getDestLoc(), 
//					getCurrDBTimeStr(), "", ""));
//		}
	}
	
	/**
	 * Reset Alarm of Auto Vehicle Not In AutoRecovery State
	 */
	private void resetAlarmOfAutoVehicleNotInAutoRecoveryState() {
		assert isAutoButNotAutoRecovery();
		assert vehicleData.isVehicleError();
		
		clearAlarmReport(0);
		vehicleData.setVehicleError(false);
		addVehicleToUpdateList();
		traceOperation("Send ClearAlarmReport...");
		
		if (trCmd != null) {
			if (trCmd.getState() == TRCMD_STATE.CMD_PAUSED) {
//				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER) {
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.TRANSFER || trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE) {	// 2022.05.05 dahye : PREMOVE 반송 재개 필요
					trCmd.setState(TRCMD_STATE.CMD_TRANSFERRING);
					pauseTrCmd(false, TrCmdConstant.NOT_ACTIVE, trCmd.getPauseCount());
				} else if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT) {
					trCmd.setLastAbortedTime(System.currentTimeMillis());
					trCmd.setState(TRCMD_STATE.CMD_ABORTED);
				}
				addTrCmdToStateUpdateList();
			}
		}
		
		// 2012.01.02 by PMM
//		// 2011.12.05 by PMM
//		if (trCmd == null) {
//			resetFromVehicleErrorHistory(new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(),
//					0, "", "",
//					"", "", "",
//					"", "", "",
//					// 2011.12.15 by PMM
//					//getCurrDBTimeStr(), "", ""));
//					"", getCurrDBTimeStr(), ""));
//		} else {
//			resetFromVehicleErrorHistory(new VehicleErrorHistory(vehicleData.getVehicleId(), vehicleData.getCurrNode(),
//					0, "", "",
//					trCmd.getTrCmdId(), trCmd.getDetailState().toConstString(), trCmd.getCarrierId(),
//					trCmd.getCarrierLoc(), trCmd.getSourceLoc(), trCmd.getDestLoc(),
//					// 2011.12.15 by PMM
//					//getCurrDBTimeStr(), "", ""));
//					"", getCurrDBTimeStr(), ""));
//		}
		resetFromVehicleErrorHistory();
	}
	
	private static Logger vehicleAVHistoryTraceLog = Logger.getLogger(VEHICLEAVHISTORY_TRACE);
	
	/**
	 * Trace VehicleAVHistory
	 * 
	 * @param message
	 */
	public void traceVehicleAVHistory() {
		if (vehicleData != null && trCmd != null) {
			StringBuffer message = new StringBuffer();
			message.append("TrDetailState:").append(trCmd.getDetailState().toConstString()).append(", ");
			
			// 2012.01.19 by PMM
//			message.append("Port:").append(trCmd.getCarrierLoc()).append(", ");
			switch (trCmd.getDetailState()) {
				case LOAD_ASSIGNED:
				case LOAD_SENT:
				case LOAD_ACCEPTED:
				case LOADING:
					message.append("Port:").append(trCmd.getDestLoc()).append(", ");
					break;
				default:
					message.append("Port:").append(trCmd.getSourceLoc()).append(", ");
					break;
			}
			
			message.append("Node:").append(vehicleData.getCurrNode()).append(", ");
			message.append("ErrorCode:").append(vehicleData.getErrorCode()).append(", ");
			message.append("TrCmdID:").append(trCmd.getTrCmdId()).append(", ");
			message.append("CarrierID:").append(trCmd.getCarrierId()).append("");
			vehicleAVHistoryTraceLog.debug(String.format("%s> %s", vehicleData.getVehicleId(), message));
		}
	}
	
/*	private boolean controlVehicleWithAbnormalState() {
		StringBuilder message = new StringBuilder();
		message.append("Abnormal Case in SLEEP Mode. VehicleMode:").append(vehicleData.getVehicleMode());
		message.append(", VehicleState:").append(vehicleData.getState());
		if (vehicleData.isAvExist()) {
			message.append(", AVExisted");
		}
		if (trCmd != null) {
			message.append(", TrCmdId:").append(trCmd.getTrCmdId());
			message.append(", RemoteCmd:").append(trCmd.getRemoteCmd());
		}
		traceOperation(message.toString());
		return false;
	}*/
}
