package com.samsung.ocs.unitdevice;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.unitdevice.model.FFU;
import com.samsung.ocs.unitdevice.model.FireDoor;
import com.sun.org.apache.bcel.internal.generic.FDIV;

import org.apache.log4j.Logger;

public class FireDoorOperation
{
	FireDoorManager fireDoorManager = null;

	private FireDoorClientComm fireDoorComm = null;
	private FireDoor fireDoorData = null;
	
	private OperationThread fireDoorOperationThread = null;

	private String fireDoorId = ""; // FIREDOOR Server Id
	private String ipAddress = "127.0.0.1";
	
	private long lFireDoorReportTimeout = 10000;
	private long lFireDoorSocketReconnectTime = 2000;
	
	private static final int FIREDOOR_PORT = 8001; // FIREDOOR Port Number
	private int socketPort = FIREDOOR_PORT;
	private int threadInterval = 1000; // OperationThread의 Interval
	
	private static final String FIREDOOR_STATUS_RUN = "RUN";
	private static final String FIREDOOR_STATUS_STOP = "STOP";
	private static final String FIREDOOR_STATUS_WARNING = "WARNING";
	
	private static final int ERRORCODE_CLEAR = 0;
	private static final int ERRORCODE_FIREDOOR_COMMFAIL = 9999;

	private static final String FIREDOOR_OPERATION_TRACE = "FireDoorOperationDebug";
	private static final String FIREDOOR_OPERATION_EXCEPTION_TRACE = "FireDoorOperationException";
	private static Logger operationTraceLog = Logger.getLogger(FIREDOOR_OPERATION_TRACE);
	private static Logger operationExceptionTraceLog = Logger.getLogger(FIREDOOR_OPERATION_EXCEPTION_TRACE);
	
	
	public FireDoorOperation(FireDoorManager fireDoorManager) {
		this.fireDoorManager = fireDoorManager;

		this.fireDoorComm = new FireDoorClientComm(this);
	}

	class OperationThread extends Thread {
		public boolean m_bRun = true;

		public void run() {
			while (m_bRun) {
				try {
					OperationProcess();
					sleep(threadInterval);
				} catch (Exception e) {
					operationTrace("OperationThread ", e);
				}
			}
			System.out.println(ipAddress + ": OprationThread is dead.");
			operationTrace(ipAddress + ": OprationThread is dead.", null);
		}
	}

	public void ReceivedStatusMsg(String name, String fireDoorId, String strStatus) {
		
		lFireDoorReportTimeout = fireDoorManager.getFireDoorReportChecktime();
		lFireDoorSocketReconnectTime = fireDoorManager.getFireDoorSorcketReconnectTime();
		
		// FIREDOOR 정보가 유효하지 않은 경우
		if (name.equals(fireDoorId) == false) {
			StringBuffer log = new StringBuffer(); 
			log.append("[ReceivedStatusMsg] ForeDoorID mismatch: ").append(name);
			operationTrace(log.toString(), null);
			return;
		}
		
		
		StringBuffer log = new StringBuffer(); 
		log.append(fireDoorId).append(",Status:").append(strStatus);
		operationTrace(log.toString(), null);

		try {
			this.fireDoorData.setPrevStatus(this.fireDoorData.getStatus());
			this.fireDoorData.setStatus(strStatus);
			this.fireDoorData.setPrevErrorCode(this.fireDoorData.getErrorCode());
			this.fireDoorData.setErrorCode(ERRORCODE_CLEAR);
			this.fireDoorData.setLastReportedTime(System.currentTimeMillis());
			fireDoorManager.addStatusToUpdateList(this.fireDoorData);
			
			if (FIREDOOR_STATUS_STOP.equals(fireDoorData.getStatus())) {
				fireDoorManager.setNodeEnable(fireDoorData.getNodeList(), false);
			}
		} catch (Exception e) {
			operationTrace("ReceivedStatusMsg ", e);
		}
	}
	
	
	public void setFireDoor(FireDoor fireDoor) {
		this.fireDoorData = fireDoor;
	}
	
	public FireDoor getFireDoor() {
		return this.fireDoorData;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public void SetOperationInfo(String strName, String strIPAddress) {
		// Operation에서 FIREDOORComm 모듈의 FIREDOORServer정보를 설정
		this.fireDoorId = strName;		
		this.ipAddress = strIPAddress;
		fireDoorComm.SetTargetInfo(strName, strIPAddress, socketPort);
	}

	/**
	 * Operation을 관리하는 Timer를 시작한다.
	 */
	public void OperationStart()
	{
		// 2018.11.27 by kw3711.kim : Thread 시작시 통신상태 클리어
		fireDoorData.setErrorCode(ERRORCODE_CLEAR);
		
		// Main Operation Timer Thread 생성 및 실행
		fireDoorOperationThread = new OperationThread();
		fireDoorOperationThread.start();
		
		fireDoorComm.fireDoorClientCommStart();
	}

	/**
	 * Operation을 관리하는 Timer를 종료한다.
	 */
	public void OperationStop()
	{
		if (fireDoorOperationThread != null)
		{
			// Operation 정리
			fireDoorOperationThread.m_bRun = false;
			fireDoorComm.fireDoorClientCommStop();

			fireDoorOperationThread = null;
			fireDoorComm = null;
			
			manageFireDoorMonitoringControl();
		}
	}

	/**
	 * Operation에서 처리할 Process를 관리한다.
	 */
	void OperationProcess()
	{
		manageRequestfireDoorStatus();
		manageFireDoorCommfailControl();
	}
	
	private void manageRequestfireDoorStatus() {
		
	}
	
	private void manageFireDoorMonitoringControl() {
		
		boolean isFireDoorMonitoringControlUsed = fireDoorManager.isFireDoorMonitoringControlUsed();
		
		if (!isFireDoorMonitoringControlUsed){
			// 16.09.06 LSH: FIREDOOR_MONITORING_CONTROL_USAGE가 NO로 변경 될 때, ErrorCode 정리
			fireDoorData.setErrorCode(ERRORCODE_CLEAR);					
//			fireDoorManager.setNodeEnable(fireDoorData.getNodeList(), true);
	
			// 16.09.06 LSH: FIREDOOR_MONITORING_CONTROL_USAGE가 NO로 변경 될 때, ErrorCode 정리
			fireDoorManager.updateErrorCodeClearToDB();
		}
	}
	
	private void manageFireDoorCommfailControl() {
		
		lFireDoorReportTimeout = fireDoorManager.getFireDoorReportChecktime();
		lFireDoorSocketReconnectTime = fireDoorManager.getFireDoorSorcketReconnectTime();
		
		
		boolean isCommfailControlUsed = fireDoorManager.isFireDoorCommfailControlUsed();

		if (ERRORCODE_FIREDOOR_COMMFAIL == fireDoorData.getErrorCode()) {
			// FIREDOOR Server와 통신 fail 일 때
			if (isCommfailControlUsed) {
				fireDoorManager.setNodeEnable(fireDoorData.getNodeList(), false);
			} else{
				if (FIREDOOR_STATUS_RUN.equals(fireDoorData.getStatus()) || FIREDOOR_STATUS_WARNING.equals(fireDoorData.getStatus())) {
					// FIREDOORSERVER_COMMFAIL_CONTROL_USAGE가 FALSE 이면서 DB에 저장된 마지막 Status가 STOP이 아닌 경우 Node Enable 전환
					fireDoorManager.setNodeEnable(fireDoorData.getNodeList(), true);
				} else if (FIREDOOR_STATUS_STOP.equals(fireDoorData.getStatus())) {
					// 16.09.06 LSH: FIREDOORSERVER_COMMFAIL_CONTROL_USAGE가 FALSE 이면서 DB에 저장된 마지막 Status가 STOP인 경우 Node Disable 전환
					fireDoorManager.setNodeEnable(fireDoorData.getNodeList(), false);
				}
			}
		}
	}
	
	public void UpdateCommFail() {
		// 16.09.06 LSH: FIREDOOR_MONITORING_CONTROL_USAGE가 YES일 때만, Commfail 처리하도록 수정
		boolean isFireDoorMonitoringControlUsed = fireDoorManager.isFireDoorMonitoringControlUsed();
		
		if (isFireDoorMonitoringControlUsed){
			fireDoorManager.addCommfailToUpdateList(fireDoorId);
			fireDoorData.setErrorCode(ERRORCODE_FIREDOOR_COMMFAIL);
		}
	}

	/**
	 * 통신에 대한 이력을 저장한다.
	 */
	private void operationTrace(String message, Throwable e) {
		if (e == null) {
			operationTraceLog.debug(message);
		} else {
			operationExceptionTraceLog.error(message, e);
		}
	}
	
	public long getFireDoorReportTimeout() { 
		return lFireDoorReportTimeout;
	}
	
	public long getFireDoorSocketReconnectTime() { 
		return lFireDoorSocketReconnectTime;
	}
}
