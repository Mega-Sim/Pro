package com.samsung.ocs.unitdevice;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.PassDoor;

/**
 * PassDoor의 상태정보를 DB에 갱신하고 통신을 담당하는 Class
 * @author zzang9un
 *
 */
public class PassDoorOperation
{
	PassDoorManager passDoorManager = null;

	private PassDoorClientComm passDoorServerComm = null;
	private PassDoor passDoor = null;
	private OperationThread PassDoorOperationThread = null;

	private String passDoorId = ""; // PassDoor Id
	private String ipAddress = "127.0.0.1";
	
	private static final int PASSDOOR_PORT = 3030; // PassDoor Port Number
	private int socketPort = PASSDOOR_PORT;
	private int threadInterval = 1000; // OperationThread의 Interval

	private static final String PASSDOOR_OPERATION_TRACE = "PassDoorOperationDebug";
	private static final String PASSDOOR_OPERATION_EXCEPTION_TRACE = "PassDoorOperationException";
	private static Logger operationTraceLog = Logger.getLogger(PASSDOOR_OPERATION_TRACE);
	private static Logger operationExceptionTraceLog = Logger.getLogger(PASSDOOR_OPERATION_EXCEPTION_TRACE);
	
	
	public PassDoorOperation(PassDoorManager passDoorManager) {
		this.passDoorManager = passDoorManager;

		this.passDoorServerComm = new PassDoorClientComm(this);
		this.passDoor = null; 
	}

	/**
	 * PassDoor Operation Thread Class
	 * @author zzang9un
	 */
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
			System.out.println("OprationThread is dead.");
			operationTrace("OprationThread is dead.", null);
		}
	}

	/**
	 * Socket로부터 수신된 상태정보를 Manager에 반영한다.
	 * @author zzang9un
	 * @date 2015. 2. 4.
	 * @param strPassDoorID
	 * @param strMode
	 * @param strStatus
	 * @param strSensorData
	 * @param strErrorCode
	 * @param strPIOData
	 */
	public void ReceivedStatusMsg(String strPassDoorID, String strMode, String strStatus, String strSensorData, String strErrorCode, String strPIOData) {
		// PassDoor 정보가 유효하지 않은 경우
		if (strPassDoorID.equals(passDoorId) == false) {
			StringBuffer log = new StringBuffer(); 
			log.append("[ReceivedStatusMsg] PassDoorId mismatch: ").append(strPassDoorID);
			operationTrace(log.toString(), null);
			return;
		}

		// log 기록
		StringBuffer log = new StringBuffer(); 
		log.append("PASSDOOR").append(strPassDoorID).append(" > Mode:").append(strMode);
		log.append(",Status:").append(strStatus);
		log.append(",SensorData:").append(strSensorData);
		log.append(",ErrorCode:").append(strErrorCode);
		log.append(",PIOData:").append(strPIOData);
		operationTrace(log.toString(), null);

		try {
			if (this.passDoor.getPassDoorId().equals(strPassDoorID)) {
				this.passDoor.setMode(strMode);
				this.passDoor.setStatus(strStatus);
				this.passDoor.setSensorData(strSensorData);
				this.passDoor.setErrorCode(strErrorCode);
				this.passDoor.setPioData(strPIOData);

				// PassDoorManager로 update해야할 PassDoor를 add
				passDoorManager.addStatusToUpdateList(this.passDoor);
			}
		} catch (Exception e) {
			operationTrace("ReceivedStatusMsg ", e);
		}
	}
	
	public String getPassDoorId() {
		return passDoorId;
	}

	public void setPassDoorId(String passDoorId) {
		this.passDoorId = passDoorId;
	}

	public boolean setPassDoor(PassDoor passDoor) {
		if (passDoor != null) {
			this.passDoor = passDoor;
			return true;
		} else
			return false;
	}
	
	public PassDoor getPassDoor() {
		return this.passDoor;
	}
	
	public boolean removePassDoor(String passDoorId) {
		if (this.passDoor.getPassDoorId().equals(passDoorId)) {
			this.passDoor = null;
			return true;
		} else
			return false;		
	}

	public String getIpAddress() {
		return ipAddress;
	}
	
	/**
	 * Operation하는 PassDoor의 정보를 설정한다.
	 */
	public void SetOperationInfo(String strPassDoorId, String strIPAddress, PassDoor passDoor) {
		// Operation 모듈에서 VehicleComm 모듈의 Vehicle정보를 설정
		this.passDoorId = strPassDoorId;		
		this.ipAddress = strIPAddress;
		setPassDoor(passDoor);
		passDoorServerComm.SetTargetInfo(strPassDoorId, strIPAddress, socketPort);
	}

	/**
	 * Operation을 관리하는 Timer를 시작한다.
	 */
	public void OperationStart()
	{
		// 2009.03.11 by MYM : HID 마스터 통신 상태를 1000으로 초기화
		UpdateCommFail();
		
		// Main Operation Timer Thread 생성 및 실행
		PassDoorOperationThread = new OperationThread();
		PassDoorOperationThread.start();
		
		passDoorServerComm.passDoorClientCommStart();
	}

	/**
	 * Operation을 관리하는 Timer를 종료한다.
	 */
	public void OperationStop()
	{
		if (PassDoorOperationThread != null)
		{
			// Operation 정리
			PassDoorOperationThread.m_bRun = false;
			passDoorServerComm.passDoorClientCommStop();

			PassDoorOperationThread = null;
			passDoorServerComm = null;
		}
	}

	/**
	 * Operation에서 처리할 Process를 관리한다.
	 */
	void OperationProcess()
	{
		manageRequestPassDoorStatus();
	}
	
	private void manageRequestPassDoorStatus() {
		
	}
	
	public void UpdateCommFail() {
		passDoorManager.addCommfailToUpdateList(ipAddress);
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
}
