package com.samsung.ocs.unitdevice;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.DockingStation;

public class DockingStationOperation {	
	
	DockingStationManager dockingStationManager = null;	
	private DockingStation dockingStationData = null;	
	private DockingStationComm dockingStationComm = null;
	private DockingStationOperationThread dockingStationOperationThread = null;
	private String lastReceivedStatusMsg = "";

	private int socketPort = 6000;
	private int threadInterval = 1000; // OperationThread의 Interval	
	
	private static final String DOCKINGSTATION_OPERATION_TRACE = "DokcingStationOperationDebug";
	private static final String DOCKINGSTATION_OPERATION_EXCEPTION_TRACE = "HIDServerOperationException";
	private static Logger operationTraceLog = Logger.getLogger(DOCKINGSTATION_OPERATION_TRACE);
	private static Logger operationExceptionTraceLog = Logger.getLogger(DOCKINGSTATION_OPERATION_EXCEPTION_TRACE);
	
	
	public DockingStationOperation(DockingStationManager m_dockingStationManager) {
		this.dockingStationManager = m_dockingStationManager;
		this.dockingStationComm = new DockingStationComm(this);		
	}
	
	class DockingStationOperationThread extends Thread {
		public boolean m_bRun = true;

		public void run() {
			while (m_bRun) {
				try {
					operationProcess();
					sleep(threadInterval);
				} catch (Exception e) {
					operationTrace("DockingStationOperationThread ", e);
				}
			}
			operationTrace("DockingStationOprationThread is dead.", null);
		}
	}	
	
	/**
	 * Operation을 관리하는 Timer를 시작한다.
	 */
	public void operationStart() {
		
		// DockingStation Alarm ID를 1000으로 초기화
		updateCommFail();
		// Main Operation Timer Thread 생성 및 실행		
		dockingStationOperationThread = new DockingStationOperationThread();
		dockingStationOperationThread.start();		
		dockingStationComm.dockingStationCommStart();
	}
	
	public void operationStop() {
		if (dockingStationOperationThread != null) {
			// Operation 정리
			dockingStationOperationThread.m_bRun = false;
			dockingStationComm.dockingStationCommStop();
			dockingStationOperationThread = null;
			dockingStationComm = null;
		}
	}
	
	void operationProcess() {
		// 주기적으로 상태 정보에 대한 request 명령을 Comm class의 m_strSendString에 저장
		setStatusRequest();
	}
	
	private boolean setStatusRequest() {		
		return dockingStationComm.setStatusRequestCommand(dockingStationData.getDockingStationID());
	}
		
	public void setOperationData(DockingStation dockingStation) {
		this.dockingStationData = dockingStation;
		dockingStationComm.setTargetInfo(dockingStation.getDockingStationID(), dockingStation.getIpAddress(), socketPort);
	}
	public void receivedStatusMsg(String strMsg, String dockingStationID, String strMode, int carrierExist, int carrierCharged, int alarmID, String carrierID) {
		if(lastReceivedStatusMsg.equals(strMsg)) {
			return;
		}
		if(dockingStationID.equals(dockingStationData.getDockingStationID())) {
			dockingStationData.setDockingStation(dockingStationID, strMode, carrierExist, carrierCharged, alarmID, carrierID);
			dockingStationManager.updateCommStatus(dockingStationData);			
		}		
		lastReceivedStatusMsg = strMsg;
				
	}
	
	private void operationTrace(String message, Throwable e) {
		if (e == null) {
			operationTraceLog.debug(message);
		} else {
			operationExceptionTraceLog.error(message, e);
		}
	}

	public void updateCommFail() {
		// TODO Auto-generated method stub
		dockingStationManager.addCommFailToUpdateList(dockingStationData.getIpAddress());
		lastReceivedStatusMsg = "";
		
	}
	
}
