package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.TrCmd;

/**
 * TrCmdManager Class, OCS 3.0 for Unified FAB
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

public class TrCmdManager extends AbstractManager {
	private static TrCmdManager manager = null;
	private static final String TRCMDID = "TRCMDID";
	private static final String REMOTECMD = "REMOTECMD";
	// 추후 STATUS -> STATE로 수정해야 함.
	private static final String STATE = "STATUS";
	// 추후 DETAILSTATUS -> DETAILSTATE로 수정해야 함.
	private static final String DETAILSTATE = "DETAILSTATUS";
	// 추후 STATUSCHANGEDTIME -> STATECHANGEDTIME로 수정해야 함.
	private static final String STATECHANGEDTIME = "STATUSCHANGEDTIME";
	private static final String CARRIERID = "CARRIERID";
	private static final String SOURCELOC = "SOURCELOC";
	private static final String DESTLOC = "DESTLOC";
	private static final String CARRIERLOC = "CARRIERLOC";
	private static final String SOURCENODE = "SOURCENODE";
	private static final String DESTNODE = "DESTNODE";
	private static final String VEHICLE = "VEHICLE";
	private static final String PRIORITY = "PRIORITY";
	private static final String REPLACE = "REPLACE";
	private static final String TRQUEUEDTIME = "TRQUEUEDTIME";
	private static final String UNLOADASSIGNEDTIME = "UNLOADASSIGNEDTIME";
	private static final String UNLOADINGTIME = "UNLOADINGTIME";
	private static final String UNLOADEDTIME = "UNLOADEDTIME";
	private static final String LOADASSIGNEDTIME = "LOADASSIGNEDTIME";
	private static final String LOADINGTIME = "LOADINGTIME";
	private static final String LOADEDTIME = "LOADEDTIME";
	private static final String PAUSE = "PAUSE";
	private static final String PAUSEDTIME = "PAUSEDTIME";
	private static final String PAUSETYPE = "PAUSETYPE";
	private static final String PAUSECOUNT = "PAUSECOUNT";
	private static final String REMOVE = "REMOVE";
	private static final String DELETEDTIME = "DELETEDTIME";
	private static final String LOADINGBYPASS = "LOADINGBYPASS";
	private static final String REASON = "REASON";
	private static final String EXPECTEDDURATION = "EXPECTEDDURATION";
	private static final String NOBLOCKINGTIME = "NOBLOCKINGTIME";
	private static final String WAITTIMEOUT = "WAITTIMEOUT";
	private static final String OCSREGISTERED = "OCSREGISTERED";
	private static final String FOUPID = "FOUPID";	// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
	private static final String PATROLID = "PATROLID";
	private static final String PATROLMODE = "PATROLMODE";
	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	private static final String DELIVERYTYPE = "DELIVERYTYPE";
	private static final String EXPECTEDDELIVERYTIME = "EXPECTEDDELIVERYTIME";
	private static final String DELIVERYWAITTIMEOUT = "DELIVERYWAITTIMEOUT";
	private static final String WAITSTARTEDTIME = "WAITSTARTEDTIME";	// 2022.03.14 dahye : Premove Logic Improve
	// 2021.08.10 by JJW : Transfer Update 사양 추가
	private static final String OLDDESTLOC = "OLDDESTLOC";
	private static final String OLDDESTNODE = "OLDDESTNODE";
	private static final String OLDPRIORITY = "OLDPRIORITY";
	private static final String OLDCARRIERID = "OLDCARRIERID";
		
	
	private static final String TRUE = "TRUE";
	private static final String FALSE = "FALSE";

	// 2011.09.15 by MYM : [Vehicle Request 정리]
	private static final String CHANGEDREMOTECMD = "CHANGEDREMOTECMD";
	private static final String CHANGEDTRCMDID = "CHANGEDTRCMDID";
	private static final String ASSIGNEDVEHICLE = "ASSIGNEDVEHICLE";
	
	private static final String CMD_QUEUED = "CMD_QUEUED";
	
	private Vector<TrCmd> updateStateList = new Vector<TrCmd>();
	private Vector<TrCmd> updatePauseList = new Vector<TrCmd>();
	private Vector<TrCmd> updateVehicleList = new Vector<TrCmd>();
	private Vector<TrCmd> updateChangedInfoList = new Vector<TrCmd>();
	private Vector<TrCmd> updateAssignedVehicleList = new Vector<TrCmd>();	
	private Vector<TrCmd> abortedTrCmdList = new Vector<TrCmd>(); // 2015.06.11 by KYK 

	// 2011.11.01 by PMM
	// 직접 DB 입력 방식으로 변경
//	private Vector<TrCmd> insertTrCmdList = new Vector<TrCmd>();
	private Vector<String> deleteTrCmdList = new Vector<String>();
	private Vector<String> deleteStageCmdList = new Vector<String>();
	
	private ConcurrentHashMap<String, TrCmd> vehicleAssignedTrCmdList = null;
	private ConcurrentHashMap<String, TrCmd> vehicleAssignRequestedTrCmdList = null;
	private ConcurrentHashMap<String, TrCmd> changedRemoteCmdRequestedNotAssignedTrCmdList = null;
	private ConcurrentHashMap<String, TrCmd> notAssignedPremoveTrCmdList = null;	// 2022.03.14 dahye : Premove Logic Improve
	
	private boolean isOperation;
	
	/**
	 * Constructor of TrCmdManager class.
	 */
	private TrCmdManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		isOperation = false;

		if (vOType != null && vOType.getClass().isInstance(TrCmd.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		vehicleAssignedTrCmdList = new ConcurrentHashMap<String, TrCmd>();
		vehicleAssignRequestedTrCmdList = new ConcurrentHashMap<String, TrCmd>();
		changedRemoteCmdRequestedNotAssignedTrCmdList = new ConcurrentHashMap<String, TrCmd>();
		notAssignedPremoveTrCmdList = new ConcurrentHashMap<String, TrCmd>();	// 2022.03.14 dahye : Premove Logic Improve
	}
	
	/**
	 * Constructor of TrCmdManager class. (Singleton)
	 */
	public static synchronized TrCmdManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new TrCmdManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	@Override
	protected void init() {
		// super에서 호출하기 때문에 Member Variables들은 초기화되지 않음.
		updateFromDB();
		isInitialized = true;
	}
	
	/**
	 * 
	 */
	@Override
	protected boolean updateToDB() {
		updateTrCmdPause();
		updateTrCmdVehicle();
		updateTrCmdState();
		deleteTrCmdFromDB();
		deleteStageCmdFromDB();
		
		// 2011.11.01 by PMM
		// 직접 DB 입력 방식으로 변경
//		registerUnknownTrCmd();
		return true;
	}
	
	private static final String SELECT_SQL = "SELECT * FROM TRCMD";
//	private static final String selectOperationSql = "SELECT * FROM TRCMD WHERE ASSIGNEDVEHICLE IS NOT NULL OR VEHICLE IS NOT NULL";
	// 2011.10.21 by PMM
	// NOT_ASSIGNED TrCmd에 대한 CANCEL 처리.
	// 2012.03.21 by PMM
//	private static final String SELECT_OPERATION_SQL = "SELECT * FROM TRCMD WHERE ASSIGNEDVEHICLE IS NOT NULL OR CHANGEDREMOTECMD IS NOT NULL OR VEHICLE IS NOT NULL";
	private static final String SELECT_OPERATION_SQL = "SELECT * FROM TRCMD WHERE ASSIGNEDVEHICLE IS NOT NULL OR CHANGEDREMOTECMD IS NOT NULL OR VEHICLE IS NOT NULL OR UNLOADASSIGNEDTIME IS NOT NULL";

	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		MODULE_STATE serviceState = this.serviceState;
		
		// super에서 호출하기 때문에 Member Variables들은 초기화되지 않음.
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		
		Set<String> removeAssignedKeys = null;
		Set<String> removeAssignRequestedKeys = null;
		Set<String> removeChangedRemoteCmdKeys = null;
		Set<String> removePremoveCmdKeys = null;	// 2022.03.14 dahye : Premove Logic Improve
		Set<TrCmd> removeAbortedTrCmdKeys = null;

		try {
			if (isInitialized &&
					vehicleAssignedTrCmdList != null &&
					vehicleAssignRequestedTrCmdList != null &&
//					changedRemoteCmdRequestedNotAssignedTrCmdList != null) {
					changedRemoteCmdRequestedNotAssignedTrCmdList != null &&
					notAssignedPremoveTrCmdList != null) {	// 2022.03.14 dahye : Premove Logic Improve
				removeAssignedKeys = new HashSet<String>(vehicleAssignedTrCmdList.keySet());
				removeAssignRequestedKeys = new HashSet<String>(vehicleAssignRequestedTrCmdList.keySet());
				removeChangedRemoteCmdKeys = new HashSet<String>(changedRemoteCmdRequestedNotAssignedTrCmdList.keySet());
				removePremoveCmdKeys = new HashSet<String>(notAssignedPremoveTrCmdList.keySet());	// 2022.03.14 dahye : Premove Logic Improve
				removeAbortedTrCmdKeys = new HashSet<TrCmd>(abortedTrCmdList);
			}
			
			conn = dbAccessManager.getConnection();
			// 2014.02.06 by KYK
//			if (isOperation) {
//				pstmt = conn.prepareStatement(SELECT_OPERATION_SQL);
//			} else {
//				pstmt = conn.prepareStatement(SELECT_SQL);
//			}
			pstmt = conn.prepareStatement(SELECT_SQL);
			
			rs = pstmt.executeQuery();
			String trCmdId = null;
			TrCmd trCmd = null;
			while (rs.next()) {
				trCmdId = rs.getString(TRCMDID);
				trCmd = (TrCmd)data.get(trCmdId);
				if (trCmd == null) {
					trCmd = (TrCmd) vOType.newInstance();
					data.put(trCmdId, trCmd);
					setTrCmdInfo(trCmd, rs);
				} else {
					// 2015.04.09 by MYM : OutOfService → InService로 전환될 때만 DB의 전체 정보를 가져옴.
					if (serviceState == MODULE_STATE.REQINSERVICE) {
						setTrCmdInfo(trCmd, rs);
					} else {
						updateTrCmdInfo(trCmd, rs);
					}
				}
				removeKeys.remove(trCmdId);
				if (isInitialized &&
						vehicleAssignedTrCmdList != null &&
						vehicleAssignRequestedTrCmdList != null &&
//						changedRemoteCmdRequestedNotAssignedTrCmdList != null) {
						changedRemoteCmdRequestedNotAssignedTrCmdList != null &&
						notAssignedPremoveTrCmdList != null) {	// 2022.03.14 dahye : Premove Logic Improve
					if (updateAssignedVehicleList(trCmd)) {
						removeAssignedKeys.remove(trCmd.getVehicle());
					}
					if (updateAssignRequestedVehicleList(trCmd)) {
						removeAssignRequestedKeys.remove(trCmd.getAssignedVehicleId());
					}
					
					// 2011.10.24 by PMM/
					// CANCEL 처리를 위해 별도 관리: 전체 검색 -> 리스트 관리
					if (updateChangedRemoteCmdNotAssignedTrCmdList(trCmd)) {
						removeChangedRemoteCmdKeys.remove(trCmd.getTrCmdId());
					}
					
					// 2022.03.14 dahye : Premove Logic Improve
					// NOT_ASSIGNED PREMOVE 처리를 위한 리스트
					if (updateNotAssignedPremoveTrCmdList(trCmd)) {
						removePremoveCmdKeys.remove(trCmd.getTrCmdId());
					}
					
					// 2015.06.11 by KYK
					if (updateAbortedTrCmdList(trCmd)) {
						removeAbortedTrCmdKeys.remove(trCmd);						
					}
				}
			}
			// DB에서 지워지더라도 유지. 별도의 정리 절차가 있을 때, data에서 정리
//			if (isOperation == false ||
//					serviceState == MODULE_STATE.OUTOFSERVICE ||
//					serviceState == MODULE_STATE.REQOUTOFSERVICE) {
//				for (String rmKey : removeKeys) {
//					data.remove(rmKey);
//				}
//			}
			for (String rmKey : removeKeys) {
				data.remove(rmKey);
			}
			if (isInitialized &&
					vehicleAssignedTrCmdList != null &&
					vehicleAssignRequestedTrCmdList != null &&
//					changedRemoteCmdRequestedNotAssignedTrCmdList != null) {
					changedRemoteCmdRequestedNotAssignedTrCmdList != null &&
					notAssignedPremoveTrCmdList != null) {	// 2022.03.14 dahye : Premove Logic Improve
				for (String rmKey : removeAssignedKeys) {
					vehicleAssignedTrCmdList.remove(rmKey);
				}
				for (String rmKey : removeAssignRequestedKeys) {
					vehicleAssignRequestedTrCmdList.remove(rmKey);
				}
				for (String rmKey : removeChangedRemoteCmdKeys) {
					changedRemoteCmdRequestedNotAssignedTrCmdList.remove(rmKey);
				}
				// 2022.03.14 dahye : Premove Logic Improve
				for (String rmKey : removePremoveCmdKeys) {
					notAssignedPremoveTrCmdList.remove(rmKey);
				}
				// 2015.06.11 by KYK
				for (TrCmd rmTrCmd : removeAbortedTrCmdKeys) {
					abortedTrCmdList.remove(rmTrCmd);
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			writeExceptionLog(LOGFILENAME, ie);
			ie.printStackTrace();
		} catch (InstantiationException e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * 
	 * @param trCmd
	 * @param rs
	 * @exception SQLException
	 */
	private void updateTrCmdInfo(TrCmd trCmd, ResultSet rs) throws SQLException {
		if (trCmd != null && rs != null) {
			trCmd.setStateChangedTime(getString(rs.getString(STATECHANGEDTIME)));
			
//		// MCS에서 Abort, Cancel을 내려왔을 때는 RemoteCmd를 업데이트 함.
//		TRCMD_REMOTECMD remoteCmd = TRCMD_REMOTECMD.toRemoteCmd(rs.getString(REMOTECMD));
//		if ((TRCMD_REMOTECMD.ABORT == remoteCmd && TRCMD_REMOTECMD.ABORT != trCmd.getRemoteCmd()) ||
//				(TRCMD_REMOTECMD.CANCEL == remoteCmd && TRCMD_REMOTECMD.CANCEL != trCmd.getRemoteCmd())) {
//			trCmd.setRemoteCmd(remoteCmd);
//		}
//		
//		// 동일한 TrCmdID로 DestChange가 내려온 경우는 DestLoc을 업데이트 함. 
//		String destLoc = getString(rs.getString(DESTLOC));
//		if (TRCMD_REMOTECMD.ABORT == trCmd.getRemoteCmd() &&
//				(destLoc != null && destLoc.length() > 0) &&
//				destLoc.equals(trCmd.getDestLoc()) == false) {
//			trCmd.setDestLoc(destLoc);
//		}	
			
			// 2015.01.05 by KYK : 정적분석 반영
			// 2011.09.15 by MYM : [Vehicle Request 정리]
//			if (updateAssignedVehicleList != null && updateAssignedVehicleList.contains(trCmd.getTrCmdId()) == false) {
			if (updateAssignedVehicleList != null && updateAssignedVehicleList.contains(trCmd) == false) {
				trCmd.setAssignedVehicleId(getString(rs.getString(ASSIGNEDVEHICLE)));
			}
//			if (updateChangedInfoList != null && updateChangedInfoList.contains(trCmd.getTrCmdId()) == false) {
			if (updateChangedInfoList != null && updateChangedInfoList.contains(trCmd) == false) {
				trCmd.setChangedTrCmdId(getString(rs.getString(CHANGEDTRCMDID)));
				if (trCmd.getTrCmdId().equals(trCmd.getChangedTrCmdId())) {
					trCmd.setDestLoc(getString(rs.getString(DESTLOC)));
					// 2014.02.28 by KYK
					trCmd.setDestNode(getString(rs.getString(DESTNODE)));
				}
				trCmd.setChangedRemoteCmd(TRCMD_REMOTECMD.toRemoteCmd(rs.getString(CHANGEDREMOTECMD)));
				
				// 2021.09.03 dahye : TRANSFER_EX4 사양 데이터 반영
				trCmd.setDeliveryType(getString(rs.getString(DELIVERYTYPE)));
				trCmd.setExpectedDuration(rs.getInt(EXPECTEDDURATION));
				trCmd.setDeliveryWaitTimeOut(rs.getInt(DELIVERYWAITTIMEOUT));
				trCmd.setWaitStartedTime(rs.getString(WAITSTARTEDTIME));	// 2022.03.14 dahye : Premove Logic Improve
			}
			
			// 2021.06.11 by JJW Transfer Update 관련  개선 및 STAGE 개선
			String newDestLoc = rs.getString(DESTLOC);
			String newCarrierId = rs.getString(CARRIERID);
			int newPriority = rs.getInt(PRIORITY);
			
			if(newDestLoc != null && newDestLoc.length() > 0){
				if(!trCmd.getDestLoc().equals(newDestLoc) || trCmd.getPriority() != newPriority || !trCmd.getCarrierId().equals(newCarrierId)){
					trCmd.setOldDestLoc(getString(rs.getString(OLDDESTLOC)));
					trCmd.setOldDestNode(getString(rs.getString(OLDDESTNODE)));
					trCmd.setOldPriority(rs.getInt(OLDPRIORITY));
					trCmd.setOldCarrierId(rs.getString(OLDCARRIERID));
					trCmd.setDestLoc(getString(rs.getString(DESTLOC)));
					trCmd.setDestNode(getString(rs.getString(DESTNODE)));
					trCmd.setPriority(rs.getInt(PRIORITY));
					trCmd.setCarrierId(rs.getString(CARRIERID));
				}
			}
		}
	}
	
	/**
	 * 
	 * @param trCmd
	 * @param rs
	 * @exception SQLException
	 */
	private void setTrCmdInfo(TrCmd trCmd, ResultSet rs) throws SQLException {
		if (trCmd != null && rs != null) {
			trCmd.setTrCmdId(getString(rs.getString(TRCMDID)));
			trCmd.setRemoteCmd(TRCMD_REMOTECMD.toRemoteCmd(rs.getString(REMOTECMD)));
			trCmd.setState(TRCMD_STATE.toTrCmdState(rs.getString(STATE)));
			trCmd.setDetailState(TRCMD_DETAILSTATE.toTrCmdDetailState(rs.getString(DETAILSTATE)));
			trCmd.setStateChangedTime(getString(rs.getString(STATECHANGEDTIME)));
			trCmd.setCarrierId(getString(rs.getString(CARRIERID)));
			trCmd.setSourceLoc(getString(rs.getString(SOURCELOC)));
			trCmd.setDestLoc(getString(rs.getString(DESTLOC)));
			trCmd.setCarrierLoc(getString(rs.getString(CARRIERLOC)));
			trCmd.setSourceNode(getString(rs.getString(SOURCENODE)));
			trCmd.setDestNode(getString(rs.getString(DESTNODE)));
			trCmd.setVehicle(getString(rs.getString(VEHICLE)));
			trCmd.setPriority(rs.getInt(PRIORITY));
			trCmd.setReplace(rs.getInt(REPLACE));
			trCmd.setTrQueuedTime(getString(rs.getString(TRQUEUEDTIME)));
			trCmd.setUnloadAssignedTime(getString(rs.getString(UNLOADASSIGNEDTIME)));
			trCmd.setUnloadingTime(getString(rs.getString(UNLOADINGTIME)));
			trCmd.setUnloadedTime(getString(rs.getString(UNLOADEDTIME)));
			trCmd.setLoadAssignedTime(getString(rs.getString(LOADASSIGNEDTIME)));
			trCmd.setLoadingTime(getString(rs.getString(LOADINGTIME)));
			trCmd.setLoadedTime(getString(rs.getString(LOADEDTIME)));
			trCmd.setPause(getBoolean(rs.getString(PAUSE)));
			trCmd.setPausedTime(getString(rs.getString(PAUSEDTIME)));
			trCmd.setPauseType(getString(rs.getString(PAUSETYPE)));
			trCmd.setPauseCount(rs.getInt(PAUSECOUNT));
			trCmd.setRemove(getBoolean(rs.getString(REMOVE)));
			trCmd.setDeletedTime(getString(rs.getString(DELETEDTIME)));
			trCmd.setLoadingByPass(getBoolean(rs.getString(LOADINGBYPASS)));
			trCmd.setReason(getString(rs.getString(REASON)));
			trCmd.setExpectedDuration(rs.getLong(EXPECTEDDURATION));
			trCmd.setNoBlockingTime(rs.getLong(NOBLOCKINGTIME));
			trCmd.setWaitTimeout(rs.getLong(WAITTIMEOUT));
			trCmd.setOcsRegistered(getBoolean(rs.getString(OCSREGISTERED)));
			
			// 2011.09.15 by MYM : [Vehicle Request 정리]
			trCmd.setChangedRemoteCmd(TRCMD_REMOTECMD.toRemoteCmd(rs.getString(CHANGEDREMOTECMD)));
			trCmd.setChangedTrCmdId(getString(rs.getString(CHANGEDTRCMDID)));
			trCmd.setAssignedVehicleId(getString(rs.getString(ASSIGNEDVEHICLE)));
			// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
			trCmd.setFoupId(getString(rs.getString(FOUPID)));
			
			// 2015.12.21 by KBS : Patrol VHL 기능 추가
			trCmd.setPatrolId(getString(rs.getString(PATROLID)));
			trCmd.setPatrolMode(rs.getInt(PATROLMODE));
			
			// 2021.04.02 by JDH : Transfer Premove 사양 추가
			trCmd.setDeliveryType(getString(rs.getString(DELIVERYTYPE)));
			trCmd.setExpectedDeliveryTime((rs.getInt(EXPECTEDDELIVERYTIME)));
			trCmd.setDeliveryWaitTimeOut((rs.getInt(DELIVERYWAITTIMEOUT)));
			trCmd.setWaitStartedTime(getString(rs.getString(WAITSTARTEDTIME)));		// 2022.03.14 dahye : Premove Logic Improve
		}
	}
	
	/**
	 * 
	 * @param trCmd
	 * @return
	 */
	private boolean updateAssignedVehicleList(TrCmd trCmd) {
		try {
			if (trCmd != null) {
				if (vehicleAssignedTrCmdList != null) {
					String vehicle = trCmd.getVehicle();
					String assignedVehicle = trCmd.getAssignedVehicleId();
					
					if (vehicle != null && vehicle.length() > 0) {
						// Operation에서 할당된 작업을 가져와서 TRCMD 테이블의 Vehicle에 자신의 VehicleId를 등록한 TrCmd.
						if (vehicle.equals(assignedVehicle) == false &&
								serviceState == MODULE_STATE.REQINSERVICE) {
							StringBuilder message = new StringBuilder();
							message.append("Abnormal Case - VehicleId and AssignedVehicleId are different! TrCmdId:").append(trCmd.getTrCmdId());
							message.append(", Vehicle:").append(vehicle);
							message.append(", AssignedVehicle:").append(assignedVehicle);
							writeExceptionLog(LOGFILENAME, message.toString());
							
							// 2012.02.06 by PMM
							trCmd.setAssignedVehicleId(vehicle);
							addTrCmdToVehicleUpdateList(trCmd);
						}
						
						vehicleAssignedTrCmdList.put(vehicle, trCmd);
						return true;
					}
				}
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		return false;
	}
	
	/**
	 * 
	 * @param trCmd
	 * @return
	 */
	private boolean updateAssignRequestedVehicleList(TrCmd trCmd) {
		try {
			if (trCmd != null) {
				if (vehicleAssignRequestedTrCmdList != null) {
					String vehicle = trCmd.getVehicle();
					String assignedVehicle = trCmd.getAssignedVehicleId();
					
					if (vehicle != null && vehicle.length() == 0 &&
							assignedVehicle != null && assignedVehicle.length() > 0 && trCmd.getState() == TRCMD_STATE.CMD_QUEUED) {
						// JobAssign에서 Vehicle에 작업을 할당했지만, 아직 Operation에서 처리하지 않은 TrCmd.
						vehicleAssignRequestedTrCmdList.put(assignedVehicle, trCmd);
						return true;
					}
				}
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		return false;
	}
	
	// 2015.06.11 by KYK
	public Vector<TrCmd> getAbortedTrCmdList() {
		return abortedTrCmdList;
	}
	
	private boolean updateAbortedTrCmdList(TrCmd trCmd) {
		if (trCmd != null) {
			if (abortedTrCmdList != null) {
				if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT) {
					if (abortedTrCmdList.contains(trCmd) == false) {
						abortedTrCmdList.add(trCmd);						
					}
					return true;
				}
			}
		}
		return false;		
	}

	private boolean updateChangedRemoteCmdNotAssignedTrCmdList(TrCmd trCmd) {
		if (trCmd != null) {
			try {
				if (changedRemoteCmdRequestedNotAssignedTrCmdList != null) {
					if (trCmd.getChangedRemoteCmd() != TRCMD_REMOTECMD.NULL) {
						if (trCmd.getState() == TRCMD_STATE.CMD_QUEUED || trCmd.getDetailState() == TRCMD_DETAILSTATE.NOT_ASSIGNED) {
							changedRemoteCmdRequestedNotAssignedTrCmdList.put(trCmd.getTrCmdId(), trCmd);
							return true;
						}
					}
				}
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
			}
		}
		return false;
	}
	
	/**
	 * 2022.03.14 dahye : Premove Logic Improve
	 * Update NotAssigned Premove TrCmd List
	 */
	private boolean updateNotAssignedPremoveTrCmdList(TrCmd trCmd) {
		if (trCmd != null) {
			try {
				if (notAssignedPremoveTrCmdList != null) {
					if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.PREMOVE && trCmd.getDeliveryType().equals("PREMOVE")) {
						if (trCmd.getState() == TRCMD_STATE.CMD_QUEUED && trCmd.getDetailState() == TRCMD_DETAILSTATE.NOT_ASSIGNED) {
							notAssignedPremoveTrCmdList.put(trCmd.getTrCmdId(), trCmd);
							return true;
						}
					}
				}
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
			}
		}
		return false;
	}
	
	// 2011.10.24 by PMM
	/**
	 * 
	 */
	public ConcurrentHashMap<String, TrCmd> getChangedRemoteCmdRequestedNotAssignedTrCmdList() {
		return changedRemoteCmdRequestedNotAssignedTrCmdList;
	}
	
	/**
	 * 2022.03.14 dahye : Premove Logic Improve
	 * Return NotAssigned Premove TrCmd List
	 */
	public ConcurrentHashMap<String, TrCmd> getNotAssignedPremoveTrCmdList() {
		return notAssignedPremoveTrCmdList;
	}

	/**
	 * 
	 * @param trCmd
	 */
	public void addTrCmdToStateUpdateList(TrCmd trCmd) {
		if (trCmd != null) {
			try {
				if (updateStateList != null) {
					if (updateStateList.contains(trCmd) == false) {
						updateStateList.add(trCmd);
					}
				}
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
			}
		}
	}
	
//	private static final String UPDATE_TRCMD_STATE_SQL = "UPDATE TRCMD SET REMOTECMD=?, STATUS=?, DETAILSTATUS=?, CARRIERLOC=?, TRQUEUEDTIME=?, UNLOADASSIGNEDTIME=?, UNLOADINGTIME=?, UNLOADEDTIME=?, LOADASSIGNEDTIME=?, LOADINGTIME=?, LOADEDTIME=?, DELETEDTIME=?, STATUSCHANGEDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE TRCMDID=?";
	// 21.08.25 dahye : DeliveryType Update
	private static final String UPDATE_TRCMD_STATE_SQL = "UPDATE TRCMD SET REMOTECMD=?, STATUS=?, DETAILSTATUS=?, CARRIERLOC=?, DELIVERYTYPE=?, TRQUEUEDTIME=?, UNLOADASSIGNEDTIME=?, UNLOADINGTIME=?, UNLOADEDTIME=?, LOADASSIGNEDTIME=?, LOADINGTIME=?, LOADEDTIME=?, DELETEDTIME=?, STATUSCHANGEDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE TRCMDID=?";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean updateTrCmdState() {
		if (updateStateList == null || updateStateList.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<TrCmd> updateStateListClone = null;
		Vector<TrCmd> updateStateListTemp = null;
		TrCmd trCmd = null;
		String state = null;
		String vehicleId = null;
		try {
			updateStateListTemp = new Vector<TrCmd>();
			updateStateListClone = (Vector<TrCmd>)updateStateList.clone();
			ListIterator<TrCmd> iteratorClone = updateStateListClone.listIterator();
			while (iteratorClone.hasNext()) {
				trCmd = iteratorClone.next();
				if (trCmd != null) {
					if (updateStateListTemp.contains(trCmd) == false) {
						updateStateListTemp.add(trCmd);
					}
				} else {
					StringBuffer tempList = new StringBuffer();
					for (TrCmd tempKey : updateStateListClone) {
						if (tempKey != null) {
							tempList.append(tempKey.getTrCmdId()).append("/");
						} else {
							tempList.append("null/");
						}
					}
					writeExceptionLog(LOGFILENAME, "   Temp List: " + tempList.toString());
				}
				// 2012.03.14 by MYM : ArrayIndexOutOfBoundsException 발생할 수 있어 보완
				// 배경 : Manager에서 DB로 업데이트 하는 것 이외에 updateTrCmdStateToDB에서 바로 처리하게 될 때 Sync가 맞지 않아 Exception 발생 가능함.
//				updateStateList.remove(0);
				updateStateList.remove(trCmd);
			}
			ListIterator<TrCmd> iterator = updateStateListTemp.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_TRCMD_STATE_SQL);
			while (iterator.hasNext()) {
				trCmd = iterator.next();
				if (trCmd != null) {
					// 2012.03.21 by PMM
					synchronized (trCmd) {
						state = trCmd.getState().toConstString();
						pstmt.setString(1, trCmd.getRemoteCmd().toConstString());
						pstmt.setString(2, state);
						pstmt.setString(3, trCmd.getDetailState().toConstString());
						pstmt.setString(4, trCmd.getCarrierLoc());
						pstmt.setString(5, trCmd.getDeliveryType());	// 21.08.25 dahye : DeliveryType Update
						pstmt.setString(6, trCmd.getTrQueuedTime());
						pstmt.setString(7, trCmd.getUnloadAssignedTime());
						pstmt.setString(8, trCmd.getUnloadingTime());
						pstmt.setString(9, trCmd.getUnloadedTime());
						pstmt.setString(10, trCmd.getLoadAssignedTime());
						pstmt.setString(11, trCmd.getLoadingTime());
						pstmt.setString(12, trCmd.getLoadedTime());
						pstmt.setString(13, trCmd.getDeletedTime());
						pstmt.setString(14, trCmd.getTrCmdId());

						vehicleId = trCmd.getVehicle();
						if (CMD_QUEUED.equals(state) == false) {
							if (vehicleId != null && vehicleId.length() > 0) {
								pstmt.executeUpdate();
							}
						} else {
							pstmt.executeUpdate();
						}
					}
				}
				updateStateListClone.remove(trCmd);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
			// 2011.11.08 by PMM
			// updateStateListClone에 대한 not null 조건 추가
			if (updateStateListClone != null && updateStateListClone.size() > 0) {
				for (TrCmd tempKey : updateStateListClone) {
					updateStateList.add(tempKey);
				}
			}
		}
		return result;
	}
	
	/**
	 * 
	 * 
	 * @param trCmd
	 * @return
	 */
	public boolean updateTrCmdStateToDB(TrCmd trCmd) {
		if (trCmd != null) {
			Connection conn = null;
			PreparedStatement pstmt = null;
			boolean result = false;
			try {
//				if (updateStateList.contains(trCmd)) {
//					updateStateList.remove(trCmd);
//				}
				
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(UPDATE_TRCMD_STATE_SQL);
				// 2012.03.21 by PMM
				synchronized (trCmd) {
					pstmt.setString(1, trCmd.getRemoteCmd().toConstString());
					pstmt.setString(2, trCmd.getState().toConstString());
					pstmt.setString(3, trCmd.getDetailState().toConstString());
					pstmt.setString(4, trCmd.getCarrierLoc());
					pstmt.setString(5, trCmd.getDeliveryType());	// 21.08.25 dahye : DeliveryType Update
					pstmt.setString(6, trCmd.getTrQueuedTime());
					pstmt.setString(7, trCmd.getUnloadAssignedTime());
					pstmt.setString(8, trCmd.getUnloadingTime());
					pstmt.setString(9, trCmd.getUnloadedTime());
					pstmt.setString(10, trCmd.getLoadAssignedTime());
					pstmt.setString(11, trCmd.getLoadingTime());
					pstmt.setString(12, trCmd.getLoadedTime());
					pstmt.setString(13, trCmd.getDeletedTime());
					pstmt.setString(14, trCmd.getTrCmdId());
					pstmt.executeUpdate();
				}
				result = true;
			} catch (SQLException se) {
				result = false;
				se.printStackTrace();
				writeExceptionLog(LOGFILENAME, se);
			} catch (Exception e) {
				result = false;
				e.printStackTrace();
				writeExceptionLog(LOGFILENAME, e);
			}
			finally {
				if (pstmt != null) {
					try {
						pstmt.close();
					} catch (Exception e) {}
					pstmt = null;
				}
			}
			return result;
		} else {
			return false;
		}
	}
	
	/**
	 * 
	 * @param trCmd
	 */
	public void addTrCmdToPauseUpdateList(TrCmd trCmd) {
		if (trCmd != null) {
			try {
				if (updatePauseList != null) {
					if (updatePauseList.contains(trCmd) == false) {
						updatePauseList.add(trCmd);
					}
				}
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
			}
		}
	}
	
	private static final String UPDATE_TRCMD_PAUSE_SQL = "UPDATE TRCMD SET PAUSE='TRUE', PAUSEDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), PAUSECOUNT= ?, PAUSETYPE= ?, STATUSCHANGEDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE TRCMDID= ? ";
	private static final String UPDATE_TRCMD_RESUME_SQL = "UPDATE TRCMD SET PAUSE='FALSE', PAUSEDTIME='', PAUSECOUNT= ?, PAUSETYPE= ?, STATUSCHANGEDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE TRCMDID= ? ";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean updateTrCmdPause() {
		if (updatePauseList == null || updatePauseList.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<TrCmd> updatePauseListClone = null;
		try {
			conn = dbAccessManager.getConnection();
			updatePauseListClone = (Vector<TrCmd>)updatePauseList.clone();
			ListIterator<TrCmd> iterator = updatePauseListClone.listIterator();
			TrCmd trCmd;
			while (iterator.hasNext()) {
				trCmd = iterator.next();
				if (trCmd != null) {
					if (trCmd.isPause()) {
						pstmt = conn.prepareStatement(UPDATE_TRCMD_PAUSE_SQL);
					} else {
						pstmt = conn.prepareStatement(UPDATE_TRCMD_RESUME_SQL);
					}
					// 2012.03.21 by PMM
					synchronized (trCmd) {
						pstmt.setInt(1, trCmd.getPauseCount());
						pstmt.setString(2, trCmd.getPauseType());
						pstmt.setString(3, trCmd.getTrCmdId());
						pstmt.executeUpdate();
						
						// 2012.03.21 by PMM
						// 여기서 close() 해줘야 함.
						pstmt.close();
					}
				}
				updatePauseList.remove(0);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
			
		}
		return result;
	}
	
	/**
	 * 
	 * @param trCmd
	 */
	public void addTrCmdToVehicleUpdateList(TrCmd trCmd) {
		assert trCmd != null;
		
		try {
			if (updateVehicleList != null) {
				if (updateVehicleList.contains(trCmd) == false) {
					updateVehicleList.add(trCmd);
				}
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private static final String UPDATE_TRCMD_VEHICLE_SQL = "UPDATE TRCMD SET VEHICLE= ?, ASSIGNEDVEHICLE = ?, STATUSCHANGEDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE TRCMDID= ? ";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean updateTrCmdVehicle() {
		if (updateVehicleList == null || updateVehicleList.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<TrCmd> updateVehicleListClone = null;
		try {
			conn = dbAccessManager.getConnection();
			updateVehicleListClone = (Vector<TrCmd>)updateVehicleList.clone();
			ListIterator<TrCmd> iterator = updateVehicleListClone.listIterator();
			TrCmd trCmd;
			pstmt = conn.prepareStatement(UPDATE_TRCMD_VEHICLE_SQL);
			while (iterator.hasNext()) {
				trCmd = iterator.next();
				if (trCmd != null) {
					// 2012.03.21 by PMM
					synchronized (trCmd) {
						pstmt.setString(1, trCmd.getVehicle());
						pstmt.setString(2, trCmd.getVehicle());
						pstmt.setString(3, trCmd.getTrCmdId());
						pstmt.executeUpdate();
					}
				} 
				updateVehicleList.remove(0);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	// 2012.03.21 by PMM
//	private static final String UNASSIGNVEHICLE_TRCMD_SQL = "UPDATE TRCMD SET VEHICLE='', ASSIGNEDVEHICLE='', STATUSCHANGEDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE TRCMDID=?";
	private static final String UNASSIGNVEHICLE_TRCMD_SQL = "UPDATE TRCMD SET STATUS = 'CMD_QUEUED', DETAILSTATUS = 'NOT_ASSIGNED', VEHICLE='', ASSIGNEDVEHICLE='', STATUSCHANGEDTIME=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE TRCMDID=?";
	/**
	 * 
	 * 
	 * @param trCmd
	 * @return
	 */
	public boolean unassignVehicleFromTrCmdToDB(TrCmd trCmd) {
		// 2011.12.01 by PMM
		if (trCmd == null) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UNASSIGNVEHICLE_TRCMD_SQL);
			pstmt.setString(1, trCmd.getTrCmdId());
			pstmt.executeUpdate();
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		return result;
	}
	
	private static final String UPDATE_TRCMD_CHANGEDINFO_RESET_SQL = "UPDATE TRCMD SET CHANGEDREMOTECMD = '', CHANGEDTRCMDID = '' WHERE TRCMDID= ? ";
	/**
	 * 
	 * @param trCmd
	 * @return
	 */
	public boolean resetChangedInfoFromDB(TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_TRCMD_CHANGEDINFO_RESET_SQL);
			if (trCmd != null) {
				pstmt.setString(1, trCmd.getTrCmdId());
				pstmt.executeUpdate();
			} 
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
//	private static final String UPDATE_TRCMD_DEST_RESET_SQL = "UPDATE TRCMD SET CHANGEDREMOTECMD = '', CHANGEDTRCMDID = '', DESTLOC = ?, DESTNODE = ? WHERE TRCMDID= ? ";
//	private static final String UPDATE_TRCMD_DEST_RESET_SQL = "UPDATE TRCMD SET CHANGEDREMOTECMD = '', CHANGEDTRCMDID = '' WHERE TRCMDID= ? ";
	private static final String UPDATE_TRCMD_OLD_DEST_RESET_SQL = "UPDATE TRCMD SET CHANGEDREMOTECMD = '', CHANGEDTRCMDID = '', OLDDESTLOC = '', OLDDESTNODE = '',"
			+ "OLDCARRIERID = '', OLDPRIORITY = '' WHERE TRCMDID= ? ";
	
	/**
	* @author : Jongwon Jung
	* @date : 2021. 4. 8.
	* @description :
	* @param trCmd
	* @return
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 4. 8. Jongwon 최초 생성 */
	public boolean resetChangedTargetInfoFromDB(TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_TRCMD_OLD_DEST_RESET_SQL);
			if (trCmd != null) {
				pstmt.setString(1, trCmd.getTrCmdId());
				pstmt.executeUpdate();
			} 
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	public TrCmd getNextTrCmdAndCancelAssignmentList(String vehicleId) {
		TrCmd trCmd = null;
		if (vehicleAssignRequestedTrCmdList != null) {
			trCmd = vehicleAssignRequestedTrCmdList.get(vehicleId);
			if (trCmd != null) {
				// 2012.02.06 by PMM
				vehicleAssignRequestedTrCmdList.remove(vehicleId,trCmd);
				vehicleAssignedTrCmdList.remove(vehicleId, trCmd);
				cancelNextTrCmd(vehicleId);
			}
		}
		return trCmd;
	}
	
	// 2012.02.06 by PMM
//	private static final String CANCEL_NEXT_TRCMD_SQL = "UPDATE TRCMD SET ASSIGNEDVEHICLE = '' WHERE TRCMDID= ?";
	// 2012.08.28 by PMM
	// CancelNextTrCmd에 대한 처리 방식 변경.
	// AssignedVehicle은 vehicleId이고, Vehicle은 ''인 모든 TrCmd에 대해 할당 해제
//	private static final String CANCEL_NEXT_TRCMD_SQL = "UPDATE TRCMD SET ASSIGNEDVEHICLE = '', VEHICLE = '' WHERE TRCMDID= ?";
	private static final String CANCEL_NEXT_TRCMD_SQL = "UPDATE TRCMD SET ASSIGNEDVEHICLE = '' WHERE VEHICLE IS NULL AND ASSIGNEDVEHICLE=?";
	/**
	 * 
	 * @return
	 */
	public boolean cancelNextTrCmd(String vehicleId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(CANCEL_NEXT_TRCMD_SQL);
			pstmt.setString(1, vehicleId);
			pstmt.executeUpdate();
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * 
	 * @param trCmd
	 */
	public void addTrCmdToAssignedVehicleUpdateList(TrCmd trCmd) {
		assert trCmd != null;
		
		try {
			if (updateAssignedVehicleList != null) {
				updateAssignedVehicleList.add(trCmd);
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	
	// 2011.10.21 by PMM
	// NOT_ASSIGNED TrCmd에 대한 CANCEL 처리로 인해 JobAssign에서는 CANCEL 된 작업에 대해서는 VHL을 Assign하지 않도록 수정.
	//	private static final String updateTrCmdAssignedVehicleSql = "UPDATE TRCMD SET ASSIGNEDVEHICLE = ? WHERE TRCMDID = ?";
	private static final String UPDATE_TRCMD_ASSIGNEDVEHICLE_SQL = "UPDATE TRCMD SET ASSIGNEDVEHICLE = ? WHERE TRCMDID = ? AND CHANGEDREMOTECMD IS NULL";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean updateTrCmdAssignedVehicle() {
		if (updateAssignedVehicleList == null || updateAssignedVehicleList.size() == 0) {
			return false;
		}

		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<TrCmd> updateAssignedVehicleListClone = null;
		try {
			conn = dbAccessManager.getConnection();
			updateAssignedVehicleListClone = (Vector<TrCmd>)updateAssignedVehicleList.clone();
			ListIterator<TrCmd> iterator = updateAssignedVehicleListClone.listIterator();
			TrCmd trCmd = null;
			pstmt = conn.prepareStatement(UPDATE_TRCMD_ASSIGNEDVEHICLE_SQL);
			while (iterator.hasNext()) {
				trCmd = iterator.next();
				if (trCmd != null) {
					// 2012.03.21 by PMM
					synchronized (trCmd) {
						pstmt.setString(1, trCmd.getAssignedVehicleId());
						pstmt.setString(2, trCmd.getTrCmdId());
						pstmt.executeUpdate();
					}
				} 
				updateAssignedVehicleList.remove(0);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * 
	 * @param trCmdId
	 */
	public void resetTrCmd(String trCmdId) {
		if (data.containsKey(trCmdId)) {
			data.remove(trCmdId);
		}
	}

	/**
	 * 
	 * @param trCmdId
	 */
	public void deleteTrCmdFromDB(String trCmdId) {
		if (deleteTrCmdList != null && deleteTrCmdList.contains(trCmdId) == false) {
			deleteTrCmdList.add(trCmdId);
		}
	}
	
	private static final String DELETE_TRCMD_SQL = "DELETE FROM TRCMD WHERE TRCMDID=?";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean deleteTrCmdFromDB() {
		if (deleteTrCmdList == null || deleteTrCmdList.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<String> deleteTrCmdListClone = null;
		try {
			conn = dbAccessManager.getConnection();
			deleteTrCmdListClone = (Vector<String>)deleteTrCmdList.clone();
			ListIterator<String> iterator = deleteTrCmdListClone.listIterator();
			pstmt = conn.prepareStatement(DELETE_TRCMD_SQL);
			String trCmdId;
			while (iterator.hasNext()) {
				trCmdId = iterator.next();
				pstmt.setString(1, trCmdId);
				pstmt.executeUpdate();
				data.remove(trCmdId);
				deleteTrCmdList.remove(trCmdId);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * 
	 * @param trCmdId
	 */
	public void deleteStageCmdFromDB(String trCmdId) {
		if (deleteStageCmdList != null && deleteStageCmdList.contains(trCmdId) == false) {
			deleteStageCmdList.add(trCmdId);
		}
	}
	
	private static final String DELETE_STATE_SQL = "DELETE FROM TRCMD WHERE REMOTECMD='STAGE' AND TRCMDID=?";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	// 2014.02.16 by MYM : [Stage Locate 기능]
//	private boolean deleteStageCmdFromDB() {
	public boolean deleteStageCmdFromDB() {
		if (deleteStageCmdList == null || deleteStageCmdList.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<String> deleteStageCmdListTemp = null;
		try {
			conn = dbAccessManager.getConnection();
			deleteStageCmdListTemp = (Vector<String>)deleteStageCmdList.clone();
			ListIterator<String> iterator = deleteStageCmdListTemp.listIterator();
			pstmt = conn.prepareStatement(DELETE_STATE_SQL);
			String trCmdId;
			while (iterator.hasNext()) {
				trCmdId = iterator.next();
				pstmt.setString(1, trCmdId);
				pstmt.executeUpdate();
				data.remove(trCmdId);
				deleteStageCmdList.remove(trCmdId);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	// 18.03.12 LSH: STB/UTB Port 작업인 경우, A 반송만 삭제 (B 반송은 ABORT 상태로 유지=후속 반송 기준으로 정리)
	private static final String DELETE_STBUTBMISSEDCARRIER_TRCMDS_SQL = "DELETE FROM TRCMD WHERE TRCMDID=? AND SOURCELOC=? AND REMOTECMD='TRANSFER'";
	
	public boolean deleteSTBUTBMissedCarrierTrCmd(String missedTrCmdId, String sourceLoc) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getNewConnection();
			if (conn != null) {
				conn.setAutoCommit(false);

				pstmt = conn.prepareStatement(DELETE_STBUTBMISSEDCARRIER_TRCMDS_SQL);
				pstmt.setString(1, missedTrCmdId);
				pstmt.setString(2, sourceLoc);
				pstmt.executeUpdate();
				pstmt.close();

				conn.commit();
				result = true;
			}
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
			try {
				if (conn != null) {
					conn.rollback();
				}
			} catch (Exception e1) {}
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.commit();
				} catch (Exception e) {}
				try {
					conn.close();
				} catch (Exception e) {}
				conn = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	private static final String DELETE_MISSEDCARRIER_TRCMDS_SQL = "DELETE FROM TRCMD WHERE (TRCMDID=? OR TRCMDID=?) AND SOURCELOC=? AND REMOTECMD='TRANSFER'";
//	/**
//	 * 
//	 * @param trCmd
//	 * @return
//	 */
//	public boolean deleteMissedCarrierTrCmds(String missedTrCmdId, String duplicatedTrCmdId, String sourceLoc) {
//		Connection conn = null;
//		PreparedStatement pstmt = null;
//		boolean result = false;
//		try {
//			conn = dbAccessManager.getConnection();
//			pstmt = conn.prepareStatement(DELETE_MISSEDCARRIER_TRCMDS_SQL);
//			pstmt.setString(1, missedTrCmdId);
//			pstmt.setString(2, duplicatedTrCmdId);
//			pstmt.setString(3, sourceLoc);
//			pstmt.executeUpdate();
//			pstmt.close();
//			result = true;
//		} catch (SQLException se) {
//			result = false;
//			se.printStackTrace();
//			writeExceptionLog(LOGFILENAME, se);
//		} catch (Exception e) {
//			result = false;
//			e.printStackTrace();
//			writeExceptionLog(LOGFILENAME, e);
//		}
//		finally {
//			if (pstmt != null) {
//				try {
//					pstmt.close();
//				} catch (Exception e) {}
//				pstmt = null;
//			}
//		}
//		if (result == false) {
//			dbAccessManager.requestDBReconnect();
//		}
//		return result;
//	}
	
	public boolean deleteMissedCarrierTrCmdsAndRegisterUnknownTrCmd(TrCmd unknownTrCmd, String missedTrCmdId, String duplicatedTrCmdId, String sourceLoc) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			if (unknownTrCmd != null) {
				conn = dbAccessManager.getNewConnection();
				if (conn != null) {
					conn.setAutoCommit(false);

					pstmt = conn.prepareStatement(DELETE_MISSEDCARRIER_TRCMDS_SQL);
					pstmt.setString(1, missedTrCmdId);
					pstmt.setString(2, duplicatedTrCmdId);
					pstmt.setString(3, sourceLoc);
					pstmt.executeUpdate();
					pstmt.close();

					pstmt = conn.prepareStatement(INSERT_TRCMD_SQL);
					pstmt.setString(1, unknownTrCmd.getTrCmdId());
					pstmt.setString(2, unknownTrCmd.getCarrierId());
					pstmt.setString(3, unknownTrCmd.getCarrierLoc());
					pstmt.setString(4, unknownTrCmd.getVehicle());
					pstmt.setString(5, unknownTrCmd.getAssignedVehicleId());
					pstmt.setString(6, unknownTrCmd.getRemoteCmd().toConstString());
					pstmt.setString(7, unknownTrCmd.getState().toConstString());
					pstmt.setString(8, unknownTrCmd.getDetailState().toConstString());
					if (unknownTrCmd.isOcsRegistered()) {
						pstmt.setString(9, OcsConstant.TRUE);
					} else {
						pstmt.setString(9, OcsConstant.FALSE);
					}	
					pstmt.setString(10, unknownTrCmd.getTrQueuedTime());
					pstmt.setString(11, unknownTrCmd.isPause() ? OcsConstant.TRUE : OcsConstant.FALSE);
					pstmt.setString(12, unknownTrCmd.getPauseType());
					pstmt.setInt(13, unknownTrCmd.getPauseCount());
					pstmt.executeUpdate();
					data.put(unknownTrCmd.getTrCmdId(), unknownTrCmd);
					pstmt.close();

					conn.commit();
					result = true;
				}
			}
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
			try {
				if (conn != null) {
					conn.rollback();
				}
			} catch (Exception e1) {}
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.commit();
				} catch (Exception e) {}
				try {
					conn.close();
				} catch (Exception e) {}
				conn = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	

	// 2011.11.01 by PMM
	// 직접 DB 입력 방식으로 변경
//	public void registerUnknownTrCmd(TrCmd trCmd) {
//		if (insertTrCmdList.contains(trCmd) == false) {
//			insertTrCmdList.add(trCmd);
//		}
//	}
	
	private static final String INSERT_TRCMD_SQL = "INSERT INTO TRCMD (TRCMDID, CARRIERID, CARRIERLOC, VEHICLE, ASSIGNEDVEHICLE, REMOTECMD, STATUS, DETAILSTATUS, OCSREGISTERED, TRQUEUEDTIME, PAUSE, PAUSETYPE, PAUSECOUNT, STATUSCHANGEDTIME, LOADINGBYPASS) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'FALSE')";
//	private static final String INSERT_TRCMD_SQL = "INSERT INTO TRCMD (TRCMDID, CARRIERID, CARRIERLOC, VEHICLE, ASSIGNEDVEHICLE, REMOTECMD, STATUS, DETAILSTATUS, OCSREGISTERED, TRQUEUEDTIME, STATUSCHANGEDTIME, PAUSE, PAUSETYPE, PAUSECOUNT, LOADINGBYPASS) VALUES (?,?,?,?,?,?,?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'FALSE','NOT ACTIVE',0,'FALSE')";
//	private static final String INSERT_TRCMD_SQL = "INSERT INTO TRCMD (TRCMDID, CARRIERID, CARRIERLOC, VEHICLE, ASSIGNEDVEHICLE, REMOTECMD, STATUS, DETAILSTATUS, OCSREGISTERED, TRQUEUEDTIME, STATUSCHANGEDTIME, PAUSE, PAUSETYPE, PAUSECOUNT, LOADINGBYPASS) VALUES (?,?,?,?,?,?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'FALSE','NOT ACTIVE',0,'FALSE')";
	/**
	 * 
	 * @param trCmd
	 * @return
	 */
	public boolean registerUnknownTrCmd(TrCmd trCmd) {
		// 2011.11.01 by PMM
		// 직접 DB 입력 방식으로 변경
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			if (trCmd != null) {
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(INSERT_TRCMD_SQL);
				pstmt.setString(1, trCmd.getTrCmdId());
				pstmt.setString(2, trCmd.getCarrierId());
				pstmt.setString(3, trCmd.getCarrierLoc());
				pstmt.setString(4, trCmd.getVehicle());
				pstmt.setString(5, trCmd.getAssignedVehicleId());
				pstmt.setString(6, trCmd.getRemoteCmd().toConstString());
				pstmt.setString(7, trCmd.getState().toConstString());
				pstmt.setString(8, trCmd.getDetailState().toConstString());
				if (trCmd.isOcsRegistered()) {
					pstmt.setString(9, OcsConstant.TRUE);
				} else {
					pstmt.setString(9, OcsConstant.FALSE);
				}	
				pstmt.setString(10, trCmd.getTrQueuedTime());
				pstmt.setString(11, trCmd.isPause() ? OcsConstant.TRUE : OcsConstant.FALSE);
				pstmt.setString(12, trCmd.getPauseType());
				pstmt.setInt(13, trCmd.getPauseCount());
				pstmt.executeUpdate();
				data.put(trCmd.getTrCmdId(), trCmd);
				pstmt.close();
				result = true;
			}
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}	
		
	/**
	 * 
	 * @param trCmdId
	 * @return
	 */
	public TrCmd getTrCmd(String trCmdId) {
		return (TrCmd)data.get(trCmdId);
	}
	
	/**
	 * 
	 * @param carrierId
	 * @return
	 */
	public TrCmd getTrCmdByCarrierId(String carrierId) {
		Set<String> dataKeys = new HashSet<String>(data.keySet());
		TrCmd tempTrCmd = null;
		for (String searchKey : dataKeys) {
			tempTrCmd = (TrCmd)data.get(searchKey);
			if (tempTrCmd != null) {
				if (carrierId.equals(tempTrCmd.getCarrierId())) {
					return (TrCmd) data.get(searchKey);
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public boolean isVehicleRegistered(String vehicleId) {
		if (vehicleAssignedTrCmdList != null && vehicleAssignedTrCmdList.containsKey(vehicleId)) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public TrCmd getAssignRequestedTrCmd(String vehicleId) {
		if (vehicleAssignRequestedTrCmdList != null) {
			return (TrCmd)vehicleAssignRequestedTrCmdList.get(vehicleId);
		}
		return null;
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public TrCmd getAssignedTrCmd(String vehicleId) {
		if (vehicleAssignedTrCmdList != null) {
			return (TrCmd)vehicleAssignedTrCmdList.get(vehicleId);
		}
		return null;
	}

	/**
	 * 
	 * @param destLoc
	 * @return
	 */
	public boolean checkUnloadTrCmdExistOnDestPort(String destLoc) {
		if (destLoc == null) return false;
		Set<String> checkKeys = new HashSet<String>(data.keySet());
		TrCmd trCmd = null;
		for (String ckKey : checkKeys) {
			trCmd = (TrCmd) data.get(ckKey);
			if (trCmd != null) {
				if (destLoc.equals(trCmd.getSourceLoc())) {
					switch (trCmd.getDetailState()) {
						case NOT_ASSIGNED:
						case UNLOAD_ASSIGNED:
						case UNLOAD_SENT:
						case UNLOAD_ACCEPTED:
						case UNLOADING:
							return true;
						default:
							break;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param destLoc
	 * @return
	 */
	public TrCmd getUnloadTrCmdExistOnDestPort(String destLoc) {
		Set<String> checkKeys = new HashSet<String>(data.keySet());
		TrCmd trCmd = null;
		for (String ckKey : checkKeys) {
			trCmd = (TrCmd) data.get(ckKey);
			if (trCmd != null) {
				if (destLoc.equals(trCmd.getSourceLoc())) {
					switch (trCmd.getDetailState()) {
						case NOT_ASSIGNED:
						case UNLOAD_ASSIGNED:
						case UNLOAD_SENT:
						case UNLOAD_ACCEPTED:
						case UNLOADING:
							return trCmd;
						default:
							break;
					}
				}
			}
		}
		trCmd = null;
		return trCmd;
	}
	
	private static final String SELECT_SOURCELOC_DUPLICATED_TRCMD_SQL = "SELECT * FROM TRCMD WHERE SOURCELOC =? AND TRCMDID != ? AND STATUS='CMD_QUEUED' AND REMOTECMD = 'TRANSFER' ORDER BY TRQUEUEDTIME";
	/**
	 * 
	 * @param carrierId
	 * @return
	 */
	public TrCmd getSourceLocDuplicatedTrCmdFromDB(TrCmd trCmd) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		TrCmd duplicatedTrCmd = null;
		try {
			if (trCmd != null) {
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(SELECT_SOURCELOC_DUPLICATED_TRCMD_SQL);
				pstmt.setString(1, trCmd.getSourceLoc());
				pstmt.setString(2, trCmd.getTrCmdId());
				rs = pstmt.executeQuery();
				while (rs.next()) {
					duplicatedTrCmd = new TrCmd();
					setTrCmdInfo(duplicatedTrCmd, rs);
					break;
				}
			}
		} catch (SQLException se) {
			duplicatedTrCmd = null;
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}				
		return duplicatedTrCmd;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean updateFromDBForJobAssign() {
		// super에서 호출하기 때문에 Member Variables들은 초기화되지 않음.
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		String trCmdId = "";
		TrCmd trCmd = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				trCmdId = rs.getString(TRCMDID);
				trCmd = (TrCmd)data.get(trCmdId);
				if (trCmd == null) {
					trCmd = (TrCmd) vOType.newInstance();
					data.put(trCmdId, trCmd);
				}
				setTrCmdInfo(trCmd, rs);
				removeKeys.remove(trCmdId);
			}
			for (String rmKey : removeKeys) {
				data.remove(rmKey);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * Add Methods for IBSEM ONLY
	 * 
	 * SELECT * FROM TRCMD
	 * SELECT * FROM TRCMD WHERE CARRIERID IS NOT NULL
	 * SELECT * FROM TRCMD WHERE TRCMDID IS NOT NULL
	 * SELECT * FROM TRCMD WHERE TRCMDID IS NOT NULL AND REMOTECMD='TRANSFER'
	 * 
	 * SELECT * FROM TRCMD WHERE CARRIERID=?
	 * SELECT * FROM TRCMD WHERE TRCMDID=?
	 * 
	 * SELECT * FROM TRCMD WHERE REMOTECMD='STAGE' AND TRCMDID=?
	 * SELECT * FROM TRCMD WHERE REMOTECMD='ABORT' AND CARRIERID=?  	  
	 */
	
	private static final String SELECT_ALL_SQL = "SELECT * FROM TRCMD";
	private static final String SELECT_ALL_BUT_TRCMDID_NOTNULL_SQL = "SELECT * FROM TRCMD WHERE TRCMDID IS NOT NULL AND OCSREGISTERED='FALSE'";
	private static final String SELECT_ALL_BUT_CARRIERID_NOTNULL_SQL = "SELECT * FROM TRCMD WHERE CARRIERID IS NOT NULL AND OCSREGISTERED='FALSE'";
	private static final String SELECT_ENHANCED_TRANSFER_SQL = "SELECT * FROM TRCMD WHERE TRCMDID IS NOT NULL AND REMOTECMD='TRANSFER' AND OCSREGISTERED='FALSE'";
	
	public HashMap<String, TrCmd> getTrCmdFromDB() {
		return getTrCmdFromDB(SELECT_ALL_SQL);
	}

	public HashMap<String, TrCmd> getTrCmdFromDBCarrierIdNotNull() {
		return getTrCmdFromDB(SELECT_ALL_BUT_CARRIERID_NOTNULL_SQL);

	}
	public HashMap<String, TrCmd> getTrCmdFromDBTrCmdIdNotNull() {
		return getTrCmdFromDB(SELECT_ALL_BUT_TRCMDID_NOTNULL_SQL);				
	}

	public HashMap<String, TrCmd> getEnhancedTransfersFromDB() {
		return getTrCmdFromDB(SELECT_ENHANCED_TRANSFER_SQL);				
	}

	private HashMap<String, TrCmd> getTrCmdFromDB(String sql) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		HashMap<String, TrCmd> trCmdTable = new HashMap<String, TrCmd>();
		//Set<String> removeKeys = new HashSet<String>(data.keySet());
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			String trCmdId;
			TrCmd trCmd;
			while (rs.next()) {
				trCmdId = rs.getString(TRCMDID);
				trCmd = new TrCmd();
				setTrCmdInfo(trCmd, rs);
				trCmdTable.put(trCmdId, trCmd);
			}
		} catch (SQLException se) {
			trCmdTable = null;
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}		
		return trCmdTable;
	}

	private static final String SELECT_ONE_BY_TRCMDID_SQL = "SELECT * FROM TRCMD WHERE TRCMDID=?";
	/**
	 * 
	 * @param trCmdId
	 * @return
	 */
	public TrCmd getTrCmdFromDBWhereTrCmdId(String trCmdId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		TrCmd trCmd = null;
		int count = 0;

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_ONE_BY_TRCMDID_SQL);
			pstmt.setString(1, trCmdId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				count++;
				trCmd = new TrCmd();
				setTrCmdInfo(trCmd, rs);
			}
			if (count > 1) trCmd = null;			
		} catch (SQLException se) {
			trCmd = null;
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}				
		return trCmd;
	}

	private static final String SELECT_ONE_BY_CARRIERID_SQL = "SELECT * FROM TRCMD WHERE CARRIERID=?";
	/**
	 * 
	 * @param carrierId
	 * @return
	 */
	public TrCmd getTrCmdFromDBWhereCarrierId(String carrierId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		TrCmd trCmd = null;
		int count = 0;

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_ONE_BY_CARRIERID_SQL);
			pstmt.setString(1, carrierId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				count++;
				trCmd = new TrCmd();
				setTrCmdInfo(trCmd, rs);
			}
			if (count > 1) trCmd = null;
		} catch (SQLException se) {
			trCmd = null;
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}				
		return trCmd;
	}

	private static final String SELECT_STAGE_CMD_SQL = "SELECT * FROM TRCMD WHERE CARRIERID=? AND REMOTECMD='STAGE' AND (CHANGEDREMOTECMD IS NULL OR CHANGEDREMOTECMD NOT LIKE 'STAGECHANGE')";
	private static final String SELECT_ALL_STAGE_CMD_SQL = "SELECT * FROM TRCMD WHERE REMOTECMD='STAGE' AND (CHANGEDREMOTECMD IS NULL OR CHANGEDREMOTECMD NOT LIKE 'STAGECHANGE')";
	/**
	 * 
	 * @param stageCommandId
	 * @return
	 */
	public HashMap<String, TrCmd> getRegisteredStageCmd(String stageCommandId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		HashMap<String, TrCmd> trCmdTable = new HashMap<String, TrCmd>();
		try {
			conn = dbAccessManager.getConnection();
			if (stageCommandId != null) {
				pstmt = conn.prepareStatement(SELECT_STAGE_CMD_SQL);
				pstmt.setString(1, stageCommandId);
			} else {
				pstmt = conn.prepareStatement(SELECT_ALL_STAGE_CMD_SQL);
			}
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String trCmdId = rs.getString(TRCMDID);
				TrCmd trCmd = new TrCmd();
				setTrCmdInfo(trCmd, rs);
				trCmdTable.put(trCmdId, trCmd);
			}
		} catch (SQLException se) {
			trCmdTable = null;
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}		
		return trCmdTable;
	}
	
//	private void setTrCmdInfo(TrCmd trcmd, ResultSet rs) throws SQLException {
//		trcmd.setTrCmdId(makeString(rs.getString(TRCMDID)));
//		trcmd.setRemoteCmd(TRCMD_REMOTECMD.toRemoteCmd(rs.getString(REMOTECMD)));
//		trcmd.setState(TRCMD_STATE.toTrCmdState(rs.getString(STATE)));
//		trcmd.setDetailState(TRCMD_DETAILSTATE.toTrCmdDetailState(rs.getString(DETAILSTATE)));
//		trcmd.setStateChangedTime(makeString(rs.getString(STATECHANGEDTIME)));
//		trcmd.setCarrierId(makeString(rs.getString(CARRIERID)));
//		trcmd.setSourceLoc(makeString(rs.getString(SOURCELOC)));
//		trcmd.setDestLoc(makeString(rs.getString(DESTLOC)));
//		trcmd.setCarrierLoc(makeString(rs.getString(CARRIERLOC)));
//		trcmd.setSourceNode(makeString(rs.getString(SOURCENODE)));
//		trcmd.setDestNode(makeString(rs.getString(DESTNODE)));
//		trcmd.setVehicle(makeString(rs.getString(VEHICLE)));
//		trcmd.setPriority(rs.getInt(PRIORITY));
//		trcmd.setReplace(rs.getInt(REPLACE));
//		trcmd.setTrQueuedTime(makeString(rs.getString(TRQUEUEDTIME)));
//		trcmd.setUnloadAssignedTime(makeString(rs.getString(UNLOADASSIGNEDTIME)));
//		trcmd.setUnloadingTime(makeString(rs.getString(UNLOADINGTIME)));
//		trcmd.setUnloadingTime(makeString(rs.getString(UNLOADEDTIME)));
//		trcmd.setLoadAssignedTime(makeString(rs.getString(LOADASSIGNEDTIME)));
//		trcmd.setLoadingTime(makeString(rs.getString(LOADINGTIME)));
//		trcmd.setLoadedTime(makeString(rs.getString(LOADEDTIME)));
//		trcmd.setPause(getBoolean(rs.getString(PAUSE)));
//		trcmd.setPausedTime(makeString(rs.getString(PAUSEDTIME)));
//		trcmd.setPauseType(makeString(rs.getString(PAUSETYPE)));
//		trcmd.setPauseCount(rs.getInt(PAUSECOUNT));
//		trcmd.setRemove(getBoolean(rs.getString(REMOVE)));
//		trcmd.setDeletedTime(makeString(rs.getString(DELETEDTIME)));
//		trcmd.setLoadingByPass(getBoolean(rs.getString(LOADINGBYPASS)));
//		trcmd.setReason(makeString(rs.getString(REASON)));
//		trcmd.setExpectedDuration(rs.getLong(EXPECTEDDURATION));
//		trcmd.setNoBlockingTime(rs.getLong(NOBLOCKINGTIME));
//		trcmd.setWaitTimeout(rs.getLong(WAITTIMEOUT));
//		
//		// DB Column 추가 필요.
//		//trcmd.setOcsRegistered(getBoolean(rs.getString("OCSREGISTERED")));
//	}
	

	private static final String SELECT_TRCMD_DUPCHECK1_SQL = "SELECT * FROM TRCMD WHERE TRCMDID=?";
	private static final String SELECT_TRCMD_DUPCHECK2_SQL = "SELECT * FROM TRCMD WHERE TRCMDID=? AND REMOTECMD=?";

	/**
	 * 2013.07.01 by KYK
	 * @param trCmdId
	 * @param remoteCmd
	 * @param carrierId
	 * @return
	 */
	public boolean checkDuplicatedTrCmdFromDB(String trCmdId, String remoteCmd, String carrierId) {
		
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = dbAccessManager.getConnection();
			if (carrierId == null) {
				if (remoteCmd == null) {
					// STAGE, SCAN
					pstmt = conn.prepareStatement(SELECT_TRCMD_DUPCHECK1_SQL);
					pstmt.setString(1, trCmdId);
				} else {
					// CANCEL, ABORT
					pstmt = conn.prepareStatement(SELECT_TRCMD_DUPCHECK2_SQL);
					pstmt.setString(1, trCmdId);
					pstmt.setString(2, remoteCmd);
				}
				rs = pstmt.executeQuery();
				if (rs != null) {
					while (rs.next()) {
						// Duplicated TrCmd exists !
						return true;
					}
				}
			} else {
				// TRANSFER (Normal, StageChange, DestChange)
				pstmt = conn.prepareStatement(SELECT_TRCMD_DUPCHECK1_SQL);
				pstmt.setString(1, trCmdId);
				rs = pstmt.executeQuery();
				ArrayList<TrCmd> dupTrCmdList = new ArrayList<TrCmd>();
				if (rs != null) {
					while (rs.next()) {
						TrCmd trCmd = new TrCmd();
						trCmd.setTrCmdId(getString(rs.getString(TRCMDID)));
						trCmd.setRemoteCmd(TRCMD_REMOTECMD.toRemoteCmd(rs.getString(REMOTECMD)));
						trCmd.setCarrierId(getString(rs.getString(CARRIERID)));
						dupTrCmdList.add(trCmd);
					}
				}
				if (dupTrCmdList.size() > 0) {
					for (TrCmd trCmd: dupTrCmdList) {
						if (trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT
								|| trCmd.getRemoteCmd() == TRCMD_REMOTECMD.STAGE) {
							if (carrierId.equals(trCmd.getCarrierId()) == false) {
								return true;
							}
						} else {
							return true;
						}
					}
				}
			}			
		} catch (SQLException e) {
			e.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, e);
			return true;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		return false;
	}
	
	private static final String SELECT_TRCMD_STAGE_SOURCE_DUPCHECK_SQL = "SELECT * FROM TRCMD WHERE SOURCELOC =? AND REMOTECMD='TRANSFER'";
	
	/**
	 * 2022.05.05 by JJW
	 * STAGE 대기중 동일 Source Trcmd가 있을 경우 Stage Cancel
	 * 
	 * Check TRANSFER/STAGE Duplicated SourcePort
	 * 
	 * @param sourceLoc
	 * @return
	 */
	public boolean checkDuplicatedSourceLocFromDB(String sourceLoc) {
		
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_TRCMD_STAGE_SOURCE_DUPCHECK_SQL);
			pstmt.setString(1, sourceLoc);
			rs = pstmt.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					// Duplicated TrCmd exists !
					return true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, e);
			return true;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		return false;
	}

//	/**
//	 * 
//	 * @param trCmdId
//	 * @param remoteCmd
//	 * @return
//	 */
//	public String getTrCmdFromDBWhere(String trCmdId, String remoteCmd) {		
//		Connection conn = null;
//		ResultSet rs = null;
//		PreparedStatement pstmt = null;
//		String rCmdInDB = null;
//		
//		try {
//			conn = dbAccessManager.getConnection();
//			if (remoteCmd == null){
//				pstmt = conn.prepareStatement(SELECT_TRCMD_DUPCHECK1_SQL);				
//				pstmt.setString(1, trCmdId);
//			} else {
//				pstmt = conn.prepareStatement(SELECT_TRCMD_DUPCHECK2_SQL);		
//				pstmt.setString(1, trCmdId);
//				pstmt.setString(2, remoteCmd);
//			}
//			rs = pstmt.executeQuery();			
//			while (rs.next()){
//				rCmdInDB = rs.getString(REMOTECMD);
//			}			
//			
//		} catch (SQLException se) {
//			se.printStackTrace();
//			dbAccessManager.requestDBReconnect();
//			writeExceptionLog(LOGFILENAME, se);
//		} finally {
//			if (rs != null) {
//				try {
//					rs.close();
//				} catch (Exception e) {}
//				rs = null;
//			}
//			if (pstmt != null) {
//				try {
//					pstmt.close();
//				} catch (Exception e) {}
//				pstmt = null;
//			}
//		}				
//		return rCmdInDB;
//	}

//	private static final String SELECT_TRANSFER_DUPCARRIER_SQL = "SELECT * FROM TRCMD WHERE CARRIERID=? AND (REMOTECMD=? OR STATUS='CMD_CANCELLING' OR STATUS='CMD_ABORTING')";

	//	private static final String SELECT_STAGE_DUPCARRIER_SQL = "SELECT * FROM TRCMD WHERE CARRIERID=? AND (REMOTECMD='TRANSFER' OR REMOTECMD='STAGE')";

	// 21.08.25 dahye : Transfer Premove 사양 추가
	private static final String SELECT_TRANSFER_DUPCARRIER_SQL = "SELECT * FROM TRCMD WHERE CARRIERID=? AND ((REMOTECMD='TRANSFER' OR REMOTECMD='PREMOVE') OR STATUS='CMD_CANCELLING' OR STATUS='CMD_ABORTING')";
	private static final String SELECT_STAGE_DUPCARRIER_SQL = "SELECT * FROM TRCMD WHERE CARRIERID=? AND (REMOTECMD='TRANSFER' OR REMOTECMD='STAGE' OR REMOTECMD='PREMOVE')";
	
	private static final String SELECT_SCAN_DUPCARRIER_SQL = "SELECT * FROM TRCMD WHERE CARRIERID=?";
	private static final String SELECT_TRANSFER_UPDATE_DUPCARRIER_SQL = "SELECT * FROM TRCMD WHERE CARRIERID=? AND (REMOTECMD=? OR STATUS='CMD_CANCELLING' OR STATUS='CMD_ABORTING') AND TRCMDID!=?";
	/**
	 * 
	 * @param carrierId
	 * @param remoteCmd
	 * @return
	 */
	public boolean isCarrierIdDuplicated(String carrierId, String remoteCmd){
		/*
		 * REMOTECMD = 'TRANSFER' 인 반송존재할 경우, CARRIER 중복(O)처리
		 * REMOTECMD = 'STAGE' or 'SCAN' 인 반송존재할 경우, CARRIER 중복(X)아님
		 * REMOTECMD = 'CANCEL' 인 반송존재할 경우, STATUS='CMD_CANCELLING' 인 경우, CARRIER 중복(O)처리
		 * REMOTECMD = 'ABORT' 인 반송존재할 경우, STATUS='CMD_ABORTING' 인 경우, CARRIER 중복(O)처리		 * 
		 */
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean result = false;
		int count = 0;
		
		try {
			conn = dbAccessManager.getConnection();
			if ("TRANSFER".equals(remoteCmd)) {
				pstmt = conn.prepareStatement(SELECT_TRANSFER_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
//				pstmt.setString(2, remoteCmd);	// 21.08.25 dahye : TRANSFER/PREMOVE 모두 DUP 확인 대상
			}else if("PREMOVE".equals(remoteCmd)){	// 2021.04.02 by JDH : Transfer Premove 사양 추가
				pstmt = conn.prepareStatement(SELECT_TRANSFER_DUPCARRIER_SQL);	// 21.08.25 dahye : Premove는 Transfer와 동일하게 DUP 판단
				pstmt.setString(1, carrierId);
			} else if ("STAGE".equals(remoteCmd)) {
				pstmt = conn.prepareStatement(SELECT_STAGE_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
			} else if ("SCAN".equals(remoteCmd)) {
				pstmt = conn.prepareStatement(SELECT_SCAN_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
			} else {
				pstmt = conn.prepareStatement(SELECT_SCAN_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
			}

			rs = pstmt.executeQuery();
			while (rs.next()) {
				count++;
			}			
			if (count == 0) result = false;
			else result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = true;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		return result;
	}

	/**
	* @author : Jongwon Jung
	* @date : 2021. 3. 3.
	* @description : Transfer Update CommandId 필요로 메소드 오버로딩
	* @param commandId
	* @param carrierId
	* @param remoteCmd
	* @return
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 3. 3. Jongwon 최초 생성 */
	public boolean isCarrierIdDuplicated(String commandId, String carrierId, String remoteCmd){
		/*
		 * REMOTECMD = 'TRANSFER' 인 반송존재할 경우, CARRIER 중복(O)처리
		 * REMOTECMD = 'STAGE' or 'SCAN' 인 반송존재할 경우, CARRIER 중복(X)아님
		 * REMOTECMD = 'CANCEL' 인 반송존재할 경우, STATUS='CMD_CANCELLING' 인 경우, CARRIER 중복(O)처리
		 * REMOTECMD = 'ABORT' 인 반송존재할 경우, STATUS='CMD_ABORTING' 인 경우, CARRIER 중복(O)처리
		 * TRCMD != 현재 commandId 제외 검색 
		 */
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean result = false;
		int count = 0;
		
		try {
			conn = dbAccessManager.getConnection();
			if ("TRANSFER".equals(remoteCmd)) {
				pstmt = conn.prepareStatement(SELECT_TRANSFER_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
				pstmt.setString(2, remoteCmd);				
			} else if ("STAGE".equals(remoteCmd)) {
				pstmt = conn.prepareStatement(SELECT_STAGE_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
			} else if ("SCAN".equals(remoteCmd)) {
				pstmt = conn.prepareStatement(SELECT_SCAN_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
			} else if ("TRANSFERUPDATE".equals(remoteCmd)) {
				pstmt = conn.prepareStatement(SELECT_TRANSFER_UPDATE_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
				pstmt.setString(2, "TRANSFER");				
				pstmt.setString(3, commandId);
			} else {
				pstmt = conn.prepareStatement(SELECT_SCAN_DUPCARRIER_SQL);
				pstmt.setString(1, carrierId);
			}

			rs = pstmt.executeQuery();
			while (rs.next()) {
				count++;
			}			
			if (count == 0) result = false;
			else result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = true;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		return result;
	}

	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	/*
	private static final String INSERT_TRANSFER_CMD_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
		+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, VEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
		+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, FOUPID) VALUES (?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),"
		+ "?,?,?,?,?,?,'',?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE','FALSE',?)";
		*/
	private static final String INSERT_TRANSFER_CMD_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
			+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, VEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
			+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
//			+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, FOUPID,DELIVERYTYPE, EXPECTEDDELIVERYTIME, DELIVERYWAITTIMEOUT) "
			+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, FOUPID,DELIVERYTYPE, EXPECTEDDELIVERYTIME, DELIVERYWAITTIMEOUT, WAITSTARTEDTIME) "		// 2022.03.14 dahye : Premove Logic Improve
			+ "VALUES (?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),"
//			+ "?,?,?,?,?,?,'',?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE','FALSE',?,?,?,?)";
			+ "?,?,?,?,?,?,'',?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE','FALSE',?,?,?,?,?)";	// 2022.03.14 dahye : Premove Logic Improve

	/**
	 * 
	 * @param trCmd
	 * @return
	 */
	synchronized public boolean createTRANSFERCmdToDB(TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			// 2011.11.01 by PMM
			// not null 추가
			if (trCmd != null) {
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(INSERT_TRANSFER_CMD_SQL);
				
				pstmt.setString(1, trCmd.getTrCmdId());
				// 2021.04.02 by JDH : Transfer Premove 사양 추가
				//pstmt.setString(2, TRCMD_REMOTECMD.TRANSFER.toConstString());
				if(trCmd.getRemoteCmd().equals(TRCMD_REMOTECMD.TRANSFER)) {
					pstmt.setString(2, TRCMD_REMOTECMD.TRANSFER.toConstString());
				} else if(trCmd.getRemoteCmd().equals(TRCMD_REMOTECMD.PREMOVE)) {
					pstmt.setString(2, TRCMD_REMOTECMD.PREMOVE.toConstString());
				}
				pstmt.setString(3, TRCMD_STATE.CMD_QUEUED.toConstString());
				pstmt.setString(4, TRCMD_DETAILSTATE.NOT_ASSIGNED.toConstString());
				pstmt.setString(5, trCmd.getCarrierId());
				pstmt.setString(6, trCmd.getSourceLoc());
				pstmt.setString(7, trCmd.getDestLoc());
				pstmt.setString(8, trCmd.getSourceLoc());
				pstmt.setString(9, trCmd.getSourceNode());
				pstmt.setString(10, trCmd.getDestNode());
				pstmt.setInt(11, trCmd.getPriority());
				pstmt.setInt(12, trCmd.getReplace());
				pstmt.setString(13, trCmd.getFoupId());
				pstmt.setString(14, trCmd.getDeliveryType());	// 2021.04.02 by JDH : Transfer Premove 사양 추가
				pstmt.setInt(15, trCmd.getExpectedDeliveryTime());	// 2021.04.02 by JDH : Transfer Premove 사양 추가
				pstmt.setInt(16, trCmd.getDeliveryWaitTimeOut());	// 2021.04.02 by JDH : Transfer Premove 사양 추가
				pstmt.setString(17, trCmd.getWaitStartedTime());	// 2022.03.14 dahye : Premove Logic Improve
				
				pstmt.execute();
				result = true;
			}
//			conn.commit();
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}		

	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	/*
	private static final String INSERT_DESTCHANGE_CMD_COPYINGDB_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
		+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, ASSIGNEDVEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
		+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, FOUPID) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,"
		+ "?,?,?,?,?,?,?,?,?,?,?,?,?,?,'FALSE',?)";
	 */
	private static final String INSERT_DESTCHANGE_CMD_COPYINGDB_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
			+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, ASSIGNEDVEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
			+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
//			+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, FOUPID ,DELIVERYTYPE, EXPECTEDDELIVERYTIME, DELIVERYWAITTIMEOUT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,"
			+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, FOUPID ,DELIVERYTYPE, EXPECTEDDELIVERYTIME, DELIVERYWAITTIMEOUT, WAITSTARTEDTIME) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,"	// 2022.03.14 dahye : Premove Logic Improve
//			+ "?,?,?,?,?,?,?,?,?,?,?,?,?,?,'FALSE',?,?,?,?)";
			+ "?,?,?,?,?,?,?,?,?,?,?,?,?,?,'FALSE',?,?,?,?,?)";	// 2022.03.14 dahye : Premove Logic Improve
	/**
	 * 
	 * @param registeredTrCmd
	 * @param changedTrCmd
	 * @return
	 */
	synchronized public boolean createDESTCHANGECmdCopyingToDB(TrCmd registeredTrCmd, TrCmd changedTrCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			// 2011.11.01 by PMM
			// not null 추가
			if (registeredTrCmd != null && changedTrCmd != null) {
				conn = dbAccessManager.getConnection();
				/* DestChange */
				pstmt = conn.prepareStatement(INSERT_DESTCHANGE_CMD_COPYINGDB_SQL);
				pstmt.setString(1, changedTrCmd.getTrCmdId());
				// 2021.04.02 by JDH : Transfer Premove 사양 추가
				//pstmt.setString(2, TRCMD_REMOTECMD.TRANSFER.toConstString());
				pstmt.setString(2, changedTrCmd.getRemoteCmd().toConstString());
				pstmt.setString(3, TRCMD_STATE.CMD_TRANSFERRING.toConstString());
				pstmt.setString(4, registeredTrCmd.getDetailState().toConstString());
				pstmt.setString(5, registeredTrCmd.getStateChangedTime());
				pstmt.setString(6, registeredTrCmd.getCarrierId());
				pstmt.setString(7, registeredTrCmd.getSourceLoc());
				pstmt.setString(8, changedTrCmd.getDestLoc());
				pstmt.setString(9, registeredTrCmd.getCarrierLoc());
				pstmt.setString(10, registeredTrCmd.getSourceNode());
				pstmt.setString(11, changedTrCmd.getDestNode());
				pstmt.setString(12, registeredTrCmd.getVehicle());
				pstmt.setInt(13, changedTrCmd.getPriority());
				pstmt.setInt(14, changedTrCmd.getReplace() );
				pstmt.setString(15, registeredTrCmd.getTrQueuedTime());
				pstmt.setString(16, registeredTrCmd.getUnloadAssignedTime());
				pstmt.setString(17, registeredTrCmd.getUnloadingTime());
				pstmt.setString(18, registeredTrCmd.getUnloadedTime());
				pstmt.setString(19, registeredTrCmd.getLoadAssignedTime());
				pstmt.setString(20, registeredTrCmd.getLoadingTime());
				pstmt.setString(21, registeredTrCmd.getLoadedTime());
				if (registeredTrCmd.isPause()) {
					pstmt.setString(22, OcsConstant.TRUE);
				} else {
					pstmt.setString(22, OcsConstant.FALSE);
				}
				pstmt.setString(23, registeredTrCmd.getPausedTime());
				pstmt.setString(24, registeredTrCmd.getPauseType());
				pstmt.setInt(25, registeredTrCmd.getPauseCount());
				if (registeredTrCmd.isRemove()) {
					pstmt.setString(26, OcsConstant.TRUE);
				} else {
					pstmt.setString(26, OcsConstant.FALSE);
				}
				pstmt.setString(27, registeredTrCmd.getDeletedTime());
				pstmt.setString(28, OcsConstant.FALSE);
				pstmt.setString(29, registeredTrCmd.getFoupId());
				pstmt.setString(30, changedTrCmd.getDeliveryType());	// 2021.04.02 by JDH : Transfer Premove 사양 추가
				pstmt.setInt(31, changedTrCmd.getExpectedDeliveryTime());	// 2021.04.02 by JDH : Transfer Premove 사양 추가
				pstmt.setInt(32, changedTrCmd.getDeliveryWaitTimeOut());	// 2021.04.02 by JDH : Transfer Premove 사양 추가
				pstmt.setString(33, changedTrCmd.getWaitStartedTime());		// 2022.03.14 dahye : Premove Logic Improve
				pstmt.execute();
				result = true;
			}
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}		


	private static final String INSERT_STAGECHANGE_CMD_COPYINGDB_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
		+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, VEHICLE, ASSIGNEDVEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
		+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
//		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, FOUPID) VALUES (?,?,?,?,TO_CHAR(SYSDATE,'YYYYMMDDHH24MISS'),?,?,?,?,?,?,'',?,?,?,"
		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, FOUPID, "
		+ "DELIVERYTYPE, EXPECTEDDELIVERYTIME, DELIVERYWAITTIMEOUT, WAITSTARTEDTIME) VALUES (?,?,?,?,TO_CHAR(SYSDATE,'YYYYMMDDHH24MISS'),?,?,?,?,?,?,'',?,?,?,"
//		+ "TO_CHAR(SYSDATE,'YYYYMMDDHH24MISS'),'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE','FALSE',?)";
		+ "TO_CHAR(SYSDATE,'YYYYMMDDHH24MISS'),'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE','FALSE',?,"
		+ "?,?,?,?)";
	/**
	 * 
	 * @param registeredTrCmd
	 * @param changedTrCmd
	 * @return
	 */
	synchronized public boolean createSTAGECHANGECmdCopyingToDB(TrCmd registeredTrCmd, TrCmd changedTrCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			// 2011.11.01 by PMM
			// not null 추가
			if (registeredTrCmd != null && changedTrCmd != null) {
				conn = dbAccessManager.getConnection();
				/* StageChange */
				pstmt = conn.prepareStatement(INSERT_STAGECHANGE_CMD_COPYINGDB_SQL);
				pstmt.setString(1, changedTrCmd.getTrCmdId());
				pstmt.setString(2, TRCMD_REMOTECMD.TRANSFER.toConstString());
				pstmt.setString(3, TRCMD_STATE.CMD_QUEUED.toConstString());
				pstmt.setString(4, TRCMD_DETAILSTATE.NOT_ASSIGNED.toConstString());
				pstmt.setString(5, changedTrCmd.getCarrierId());
				pstmt.setString(6, changedTrCmd.getSourceLoc());
				pstmt.setString(7, changedTrCmd.getDestLoc());
				pstmt.setString(8, registeredTrCmd.getCarrierLoc());
				pstmt.setString(9, changedTrCmd.getSourceNode());
				pstmt.setString(10, changedTrCmd.getDestNode());
				pstmt.setString(11, registeredTrCmd.getVehicle());
				pstmt.setInt(12, changedTrCmd.getPriority());
				pstmt.setInt(13, changedTrCmd.getReplace());
				pstmt.setString(14, changedTrCmd.getFoupId());
				pstmt.setString(15, changedTrCmd.getDeliveryType());		// 2022.03.14 dahye : TRANSFER_EX4
				pstmt.setInt(16, changedTrCmd.getExpectedDeliveryTime());	// 2022.03.14 dahye : TRANSFER_EX4
				pstmt.setInt(17, changedTrCmd.getDeliveryWaitTimeOut());	// 2022.03.14 dahye : TRANSFER_EX4
				pstmt.setString(18, changedTrCmd.getWaitStartedTime());		// 2022.03.14 dahye : TRASNFER_EX4
				
				pstmt.execute();
				result = true;
			}
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}		

	private static final String INSERT_STAGE_CMD_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
		+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, VEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
		+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, EXPECTEDDURATION, NOBLOCKINGTIME, WAITTIMEOUT, OCSREGISTERED) VALUES "
		+ "(?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),?,?,?,?,?,?,'',?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')," 
		+ "'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE',?,?,?,'FALSE')";

	/**
	 * 
	 * @param trCmd
	 * @return
	 */
	public boolean createSTAGECmdToDB(TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			// 2011.11.01 by PMM
			// not null 추가
			if (trCmd != null) {
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(INSERT_STAGE_CMD_SQL);
				
				pstmt.setString(1, trCmd.getTrCmdId() );
				pstmt.setString(2, TRCMD_REMOTECMD.STAGE.toConstString());
				pstmt.setString(3, TRCMD_STATE.CMD_QUEUED.toConstString());
				pstmt.setString(4, TRCMD_DETAILSTATE.NOT_ASSIGNED.toConstString());
				pstmt.setString(5, trCmd.getCarrierId());
				pstmt.setString(6, trCmd.getSourceLoc());
				pstmt.setString(7, trCmd.getDestLoc());
				pstmt.setString(8, trCmd.getSourceLoc());
				pstmt.setString(9, trCmd.getSourceNode());
				pstmt.setString(10, trCmd.getDestNode());
				pstmt.setInt(11, trCmd.getPriority());
				pstmt.setInt(12, trCmd.getReplace());
				pstmt.setLong(13, trCmd.getExpectedDuration());
				pstmt.setLong(14, trCmd.getNoBlockingTime());
				pstmt.setLong(15, trCmd.getWaitTimeout() );
				pstmt.execute();
				result = true;
			}
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}		
	
	private static final String INSERT_SCAN_CMD_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
		+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, VEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
		+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED) VALUES (?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')," 
		+ "?,?,?,?,?,?,'',?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')," 
		+ "'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE','FALSE')";
	/**
	 * 	  
	 * @param trCmd
	 * @return
	 */
	public boolean createSCANCmdToDB(TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			// 2011.11.01 by PMM
			// not null 추가
			if (trCmd != null) {
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(INSERT_SCAN_CMD_SQL);
				pstmt.setString(1, trCmd.getTrCmdId());
				pstmt.setString(2, TRCMD_REMOTECMD.SCAN.toConstString());
				pstmt.setString(3, TRCMD_STATE.CMD_QUEUED.toConstString());
				pstmt.setString(4, TRCMD_DETAILSTATE.NOT_ASSIGNED.toConstString());
				pstmt.setString(5, trCmd.getCarrierId());
				pstmt.setString(6, trCmd.getSourceLoc());
				pstmt.setString(7, trCmd.getDestLoc());
				pstmt.setString(8, trCmd.getSourceLoc());
				pstmt.setString(9, trCmd.getSourceNode());
				pstmt.setString(10, trCmd.getDestNode());
				pstmt.setInt(11, trCmd.getPriority());
				pstmt.setInt(12, trCmd.getReplace());
				pstmt.execute();
				result = true;
			}
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
	private static String DELETE_STAGE_CMD = "DELETE FROM TRCMD WHERE REMOTECMD='STAGE'";
	private static String DELETE_STAGE_CMD2 = "DELETE FROM TRCMD WHERE REMOTECMD='STAGE' AND TRCMDID=?";
	/**
	 * 
	 * @param trCmdId
	 * @return
	 */
	public boolean deleteSTAGECmdFromDB(String trCmdId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();			
			if (trCmdId == null) {
				pstmt = conn.prepareStatement(DELETE_STAGE_CMD);				
			} else {
				pstmt = conn.prepareStatement(DELETE_STAGE_CMD2);				
				pstmt.setString(1, trCmdId); 				
			}
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}

	private static String SELECT_VEHICLE_AT_SOURCE_SQL = "SELECT A.* FROM TRCMD A, VEHICLE B WHERE A.TRCMDID=? AND A.VEHICLE=B.VEHICLEID AND A.SOURCENODE=B.CURRNODE";
	private static String SELECT_VEHICLE_AT_DEST_SQL = "SELECT A.* FROM TRCMD A, VEHICLE B WHERE A.TRCMDID=? AND A.VEHICLE=B.VEHICLEID AND DESTNODE=B.CURRNODE";
	/**
	 * 
	 * @param trCmdId
	 * @return
	 */
	public boolean isVehicleArrivedAtSourceNode(String trCmdId) {
		return isVehicleArrivedAtTargetNode(trCmdId, SELECT_VEHICLE_AT_SOURCE_SQL);		
	}

	/**
	 * 
	 * @param trCmdId
	 * @return
	 */
	public boolean isVehicleArrivedAtDestNode(String trCmdId) {
		return isVehicleArrivedAtTargetNode(trCmdId, SELECT_VEHICLE_AT_DEST_SQL);		
	}
	
	/**
	 * 
	 * @param trCmdId
	 * @param sql
	 * @return
	 */
	public boolean isVehicleArrivedAtTargetNode(String trCmdId, String sql) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int count = 0;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, trCmdId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				count++;
			}
			if (count == 0) {
				result = true;
			}
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}		
		return result;
	}

	private static String INSERT_TRCOMPLETIONHISTORY_SQL = "INSERT INTO TRCOMPLETIONHISTORY (TRCMDID, REMOTECMD, " 
		+ "CARRIERID, PRIORITY, SOURCELOC, DESTLOC, SOURCENODE, DESTNODE, VEHICLE, TRQUEUEDTIME,DELETEDTIME, " 
		+ "EXPECTEDDURATION, NOBLOCKINGTIME, WAITTIMEOUT, OCSREGISTERED, FOUPID) VALUES (?,?,?,?,?,?,?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),?,?,?,'FALSE',?)";
	/**
	 * 
	 * @param trCmd
	 * @param remoteCmd
	 */
	public void updateTrCompletionHistoryDB(TrCmd trCmd, String remoteCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(INSERT_TRCOMPLETIONHISTORY_SQL);
			pstmt.setString(1, trCmd.getTrCmdId() );
			pstmt.setString(2, remoteCmd);
			pstmt.setString(3, trCmd.getCarrierId() );
			pstmt.setInt(4, trCmd.getPriority());
			pstmt.setString(5, trCmd.getSourceLoc() );
			pstmt.setString(6, trCmd.getDestLoc() );
			pstmt.setString(7, trCmd.getSourceNode() );
			pstmt.setString(8, trCmd.getDestNode() );
			pstmt.setString(9, trCmd.getVehicle() );
			pstmt.setString(10, trCmd.getTrQueuedTime() );
			pstmt.setLong(11, trCmd.getExpectedDuration());
			pstmt.setLong(12, trCmd.getNoBlockingTime() );
			pstmt.setLong(13, trCmd.getWaitTimeout() );
			pstmt.setString(14, trCmd.getFoupId() );
			pstmt.execute();
		} catch (SQLException se) {
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}		
	}
	
	private static String INSERT_STAGE_TRCOMPLETIONHISTORY_SQL = "INSERT INTO TRCOMPLETIONHISTORY ( TRCMDID, REMOTECMD, " 
		+ "CARRIERID, PRIORITY, SOURCELOC, DESTLOC, SOURCENODE, DESTNODE, VEHICLE, TRQUEUEDTIME, DELETEDTIME, "
		+ "EXPECTEDDURATION, NOBLOCKINGTIME, WAITTIMEOUT, OCSREGISTERED) VALUES (?,'STAGEDELETE',?,?,?,?,?,?,?,?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),?,?,?,'FALSE')";
	/**
	 * 
	 * @param trCmd
	 * @return
	 */
	public boolean updateSTAGEDELETETrCompletionHistoryDB(TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INSERT_STAGE_TRCOMPLETIONHISTORY_SQL);
			pstmt.setString(1, trCmd.getTrCmdId());
			pstmt.setString(2, trCmd.getCarrierId());
			pstmt.setInt(3, trCmd.getPriority());
			pstmt.setString(4, trCmd.getSourceLoc());
			pstmt.setString(5, trCmd.getDestLoc());
			pstmt.setString(6, trCmd.getSourceNode());
			pstmt.setString(7, trCmd.getDestNode());
			pstmt.setString(8, trCmd.getVehicle());
			pstmt.setString(9, trCmd.getTrQueuedTime());
			pstmt.setFloat(10, trCmd.getExpectedDuration());
			pstmt.setFloat(11, trCmd.getNoBlockingTime());
			pstmt.setFloat(12, trCmd.getWaitTimeout());
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;		
	}
	
	/**
	 * 
	 * @param carrierId
	 * @param trCmdId
	 * @param destLoc
	 * @param destNode
	 * @return
	 */
	public boolean updateTrCmdToDB(String carrierId, String trCmdId, String destLoc, String destNode) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		StringBuilder sb = new StringBuilder("UPDATE TRCMD SET ");
		if (trCmdId != null) {
			sb.append("TRCMDID='").append(trCmdId).append("', ");
		}
		if (destLoc != null) {
			sb.append("DESTLOC='").append(destLoc).append("', ");
		}
		if (destNode != null) {
			sb.append("DESTNODE='").append(destNode).append("', ");
		}

		String str = sb.toString();
		int length = str.length();
		str = str.substring(0, length-2);
		StringBuilder sb2 = new StringBuilder(str);		
		sb2.append(" WHERE CARRIERID='").append(carrierId).append("'");
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(sb2.toString());
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;				
	}

	/**
	 * 
	 * @param trCmdId
	 * @param state
	 * @param detailState
	 * @param remoteCmd
	 * @param jobPause
	 * @param pauseType
	 * @param pauseCount
	 * @param stateChangedTime
	 * @param deliveryType	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	 * @param expectedDeliveryTime	// 2021.09.03 dahye : EX4 사양 데이터 추가반영
	 * @param deliveryWaitTimeOut	// 2021.09.03 dahye : EX4 사양 데이터 추가반영
	 * @param waitStartedTime		// 2022.03.14 dahye : EX4 사양 데이터 추가반영 (Premove Logic Improve)
	 * @return
	 */
	public boolean updateTrCmdStatusToDB(String trCmdId, String state,
		String detailState, String remoteCmd, String jobPause,
		String pauseType, int pauseCount, boolean stateChangedTime ,
		String deliveryType, int expectedDeliveryTime, int deliveryWaitTimeOut, String waitStartedTime) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		StringBuilder sb = new StringBuilder("UPDATE TRCMD SET ");		
		if (state != null) {
			sb.append("STATUS='").append(state).append("', ");
			if ("CMD_COMPLETED".equals(state)) {
				sb.append("LOADEDTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			} else if ("CMD_ABORT_COMPLETED".equals(state)) {
				sb.append("DELETEDTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			}
		}		
		if (detailState != null) {
			sb.append("DETAILSTATUS='").append(detailState).append("', ");
			if ("UNLOAD_ASSIGNED".equals(detailState)) {
				sb.append("UNLOADASSIGNEDTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			} else if ("UNLOADING".equals(detailState)) {
				sb.append("UNLOADINGTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			} else if ("UNLOADED".equals(detailState)) {
				sb.append("UNLOADEDTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			} else if ("LOAD_ASSIGNED".equals(detailState)) {
				sb.append("LOADASSIGNEDTime = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			} else if ("LOADING".equals(detailState)) {
				sb.append("LOADINGTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			} else if ("LOADED".equals(detailState)) {
				sb.append("LOADEDTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			}				
		}
		if (remoteCmd != null) {
			sb.append("REMOTECMD = '").append(remoteCmd).append("', ");
		}
		if (jobPause != null) {
			sb.append("PAUSE = '").append(jobPause).append("', ");
			if (jobPause.equals("TRUE")) {
				sb.append("PAUSEDTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
			}
		}
		if (pauseType != null) {
			sb.append("PAUSETYPE = '").append(pauseType).append("', ");
		}
		if (pauseCount >= 0) {
			sb.append("PAUSECOUNT = ").append(pauseCount).append(", ");
		}
		if (stateChangedTime == true) {
			sb.append("STATUSCHANGEDTIME = TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), ");
		}
		
//		// 2021.04.02 by JDH : Transfer Premove 사양 추가
//		sb.append("DELIVERYTYPE = '").append(deliveryType).append("', ");		
//		sb.append("EXPECTEDDELIVERYTIME = '").append(expectedDeliveryTime).append("', ");	// 2021.09.03 dahye : EX4 사양 데이터 추가반영
//		sb.append("DELIVERYWAITTIMEOUT = '").append(deliveryWaitTimeOut).append("'");	// 2021.09.03 dahye : EX4 사양 데이터 추가반영
		// 2022.03.14 dahye : TRANSFER_EX4 사양 데이터 추가반영
		if (deliveryType != null) {
			sb.append("DELIVERYTYPE = '").append(deliveryType).append("', ");
		}
		if (expectedDeliveryTime >= 0) {
			sb.append("EXPECTEDDELIVERYTIME = '").append(expectedDeliveryTime).append("', ");
		}
		if (deliveryWaitTimeOut >= 0) {
			sb.append("DELIVERYWAITTIMEOUT = '").append(deliveryWaitTimeOut).append("', ");
		}
		if (waitStartedTime != null) {
			sb.append("WAITSTARTEDTIME = '").append(waitStartedTime).append("', ");
		}

		String str = sb.toString();
		int length = str.length();
		str = str.substring(0, length-2);
		StringBuilder sb2 = new StringBuilder(str);
		sb2.append(" WHERE TRCMDID='").append(trCmdId).append("'");
		
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(sb2.toString());

			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;				
	}
	
	private static final String UPDATE_TRCMD_CHANGEDINFO_SQL = "UPDATE TRCMD SET CHANGEDREMOTECMD = ?, changedtrcmdid = ? WHERE TRCMDID = ?";
	/**
	 * 
	 * @param trCmdId
	 * @param changedRemoteCmd
	 * @param changedTrCmdId
	 * @return
	 */
	public boolean updateTrCmdChangedInfoToDB(String trCmdId, String changedRemoteCmd, String changedTrCmdId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_TRCMD_CHANGEDINFO_SQL);
			pstmt.setString(1, changedRemoteCmd);
			pstmt.setString(2, changedTrCmdId);			
			pstmt.setString(3, trCmdId);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;		
	}	
	
	private static final String UPDATE_TRCMD_UPDATECHANGEDINFO_SQL = "UPDATE TRCMD SET CHANGEDREMOTECMD = ?, DESTLOC = ?, DESTNODE = ?, CARRIERID = ?, PRIORITY = ? WHERE TRCMDID = ?";
	/**
	 * 
	 * @param trCmdId
	 * @param changedRemoteCmd
	 * @param changedTrCmd
	 * @return
	 */
	public boolean updateTrCmdChangedInfoToDB(String trCmdId, String changedRemoteCmd, TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_TRCMD_UPDATECHANGEDINFO_SQL);
			pstmt.setString(1, changedRemoteCmd);
			pstmt.setString(2, trCmd.getDestLoc());			
			pstmt.setString(3, trCmd.getDestNode());
			pstmt.setString(4, trCmd.getCarrierId());
			pstmt.setInt(5, trCmd.getPriority());
			pstmt.setString(6, trCmdId);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;		
	}
	
	private static final String UPDATE_OLD_TRCMD_UPDATECHANGEDINFO_SQL = "UPDATE TRCMD SET OLDDESTLOC = ?, OLDDESTNODE = ?, OLDCARRIERID = ?, OLDPRIORITY = ? WHERE TRCMDID = ?";
	/**
	 * @param trCmdId
	 * @param trCmd
	 * @return
	 */
	public boolean updateOldTrCmdChangedInfoToDB(String trCmdId, TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_OLD_TRCMD_UPDATECHANGEDINFO_SQL);
			pstmt.setString(1, trCmd.getOldDestLoc());			
			pstmt.setString(2, trCmd.getOldDestNode());
			pstmt.setString(3, trCmd.getOldCarrierId());
			pstmt.setInt(4, trCmd.getOldPriority());
			pstmt.setString(5, trCmdId);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;		
	}
	
	private static final String UPDATE_TRCMD_NOTASSIGN_UPDATECHANGEDINFO_SQL = "UPDATE TRCMD SET DESTLOC = ?, DESTNODE = ?, CARRIERID = ?, PRIORITY = ? WHERE TRCMDID = ?";
	/**
	 * 
	 * @param trCmdId
	 * @param changedRemoteCmd
	 * @param changedTrCmd
	 * @return
	 */
	public boolean updateTrCmdNotAssignChangedInfoToDB(String trCmdId, TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_TRCMD_NOTASSIGN_UPDATECHANGEDINFO_SQL);
			pstmt.setString(1, trCmd.getDestLoc());			
			pstmt.setString(2, trCmd.getDestNode());
			pstmt.setString(3, trCmd.getCarrierId());
			pstmt.setInt(4, trCmd.getPriority());
			pstmt.setString(5, trCmdId);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;		
	}
	
	private static final String UPDATE_TRCMD_RECOVERY_UPDATECHANGEDINFO_SQL = "UPDATE TRCMD SET DESTLOC = OLDDESTLOC, DESTNODE = OLDDESTNODE,"
			+ " CARRIERID = OLDCARRIERID, PRIORITY = OLDPRIORITY WHERE TRCMDID = ?";
	/**
	 * 
	 * @param trCmdId
	 * @param changedRemoteCmd
	 * @param changedTrCmd
	 * @return
	 */
	public boolean updateTrCmdRecoveryChangedInfoToDB(String trCmdId, TrCmd trCmd) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_TRCMD_RECOVERY_UPDATECHANGEDINFO_SQL);
			pstmt.setString(1, trCmdId);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;		
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isNothingUpdateToDB() {
		if (updateStateList.size() > 0) {
			writeLog(LOGFILENAME, "updateStateList: " + updateStateList.size());
			return false;
		}
		if (updatePauseList.size() > 0) {
			writeLog(LOGFILENAME, "updatePauseList: " + updatePauseList.size());
			return false;
		}
		if (updateVehicleList.size() > 0) {
			writeLog(LOGFILENAME, "updateVehicleList: " + updateVehicleList.size());
			return false;
		}
		if (deleteTrCmdList.size() > 0) {
			writeLog(LOGFILENAME, "deleteTrCmdList: " + deleteTrCmdList.size());
			return false;
		}
		if (deleteStageCmdList.size() > 0) {
			writeLog(LOGFILENAME, "deleteStageCmdList: " + deleteStageCmdList.size());
			return false;
		}
		if (updateChangedInfoList.size() > 0) {
			writeLog(LOGFILENAME, "updateChangedInfoList: " + updateChangedInfoList.size());
			return false;
		}
		if (updateAssignedVehicleList.size() > 0) {
			writeLog(LOGFILENAME, "updateAssignedVehicleList: " + updateAssignedVehicleList.size());
			return false;
		}
		
		// 2011.11.01 by PMM
		// 직접 DB 입력 방식으로 변경
//		if (insertTrCmdList.size() > 0) {
//			return false;
//		}
		
		return true;
	}
	
	public void setOperation(boolean isOperation) {
		this.isOperation = isOperation;
	}
	
	private static final String INSERT_PATROL_REQUEST_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
		+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, VEHICLE, ASSIGNEDVEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
		+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED, PATROLID, PATROLMODE) "
		+ "VALUES (CONCAT(?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')),'PATROL','CMD_QUEUED','NOT_ASSIGNED',TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),"
		+ "CONCAT(?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')),'Patrol_B1','Patrol_B2','Patrol_B1',?,?,'',?,10,0,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE','TRUE',?,?)";

	synchronized public boolean createPatrolRequestToDB(String trCmdId, String vehicleId, String carrierId, String startNodeId, String endNodeId, String patrolId, int patrolMode) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INSERT_PATROL_REQUEST_SQL);
			
			pstmt.setString(1, trCmdId);
			pstmt.setString(2, carrierId);
			pstmt.setString(3, startNodeId);
			pstmt.setString(4, endNodeId);
			pstmt.setString(5, vehicleId);
			pstmt.setString(6, patrolId);
			pstmt.setInt(7, patrolMode);
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
	private static final String INSERT_VIBRATION_REQUEST_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
		+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, VEHICLE, ASSIGNEDVEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
		+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED) VALUES (CONCAT(?,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')),'VIBRATION','CMD_QUEUED','NOT_ASSIGNED',TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),"
		+ "?,?,?,?,?,?,'',?,10,0,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','','FALSE','TRUE')";

	synchronized public boolean createVibrationRequestToDB(String trCmdId, String vehicleId, String carrierId, CarrierLoc dockingStation) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			if (dockingStation != null) {
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(INSERT_VIBRATION_REQUEST_SQL);
				
				pstmt.setString(1, trCmdId);
				pstmt.setString(2, carrierId);
				pstmt.setString(3, dockingStation.getCarrierLocId());
				pstmt.setString(4, dockingStation.getCarrierLocId());
				pstmt.setString(5, dockingStation.getCarrierLocId());
				pstmt.setString(6, dockingStation.getNode());
				pstmt.setString(7, dockingStation.getNode());
				pstmt.setString(8, vehicleId);
				pstmt.execute();
				result = true;
			}
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
	private static final String INSERT_TRANSFER_REQUEST_SQL = "INSERT INTO TRCMD (TRCMDID, REMOTECMD, STATUS, DETAILSTATUS, STATUSCHANGEDTIME, "
		+ "CARRIERID, SOURCELOC, DESTLOC, CARRIERLOC, SOURCENODE, DESTNODE, VEHICLE, ASSIGNEDVEHICLE, PRIORITY, REPLACE, TRQUEUEDTIME, "
		+ "UNLOADASSIGNEDTIME, UNLOADINGTIME, UNLOADEDTIME, LOADASSIGNEDTIME, LOADINGTIME, LOADEDTIME, PAUSE, PAUSEDTIME, "
		+ "PAUSETYPE, PAUSECOUNT, REMOVE, DELETEDTIME, LOADINGBYPASS, OCSREGISTERED) VALUES (CONCAT(?,TO_CHAR(SYSDATE, 'HH24MISS')),'TRANSFER','CMD_QUEUED','NOT_ASSIGNED',TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),"
		+ "?,?,?,?,?,?,'',?,10,0,TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'),'','','','','','','FALSE','','NOT ACTIVE','0','FALSE','',?,'TRUE')";
	
	synchronized public boolean createTransferRequestToDB(String trCmdIdPrefix, String vehicleId, String carrierId, String sourceLocId, String destLocId, String sourceNodeId, String destNodeId, boolean isLoadingByPass) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INSERT_TRANSFER_REQUEST_SQL);
			
			pstmt.setString(1, trCmdIdPrefix);
			pstmt.setString(2, carrierId);
			pstmt.setString(3, sourceLocId);
			pstmt.setString(4, destLocId);
			pstmt.setString(5, sourceLocId);
			pstmt.setString(6, sourceNodeId);
			pstmt.setString(7, destNodeId);
			pstmt.setString(8, vehicleId);
			if (isLoadingByPass) {
				pstmt.setString(9, TRUE);
			} else {
				pstmt.setString(9, FALSE);
			}
			pstmt.execute();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}
	
	public boolean checkDBStatus() {
		if (super.checkDBStatus()) {
			return true;
		} else {
			return super.checkDBStatus();
		}
	}

	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}
	
}