package com.samsung.ocs.unitdevice;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.HID;

/**
 * <p>Title: UnifiedOCS 1.0 for JAVA</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: SAMSUNG ELECTRONICS</p>
 * @author 조영일 책임
 * @version 1.0
 */

/**
 * HIDServerOperation.java는 HID를 관리하는 모듈로 HID의 전원 On/Off명령을 보내거나 상태정보를
 * DB에 갱신하고 HID를 통해 통신을 담당하는 모듈이다.
 */
public class HIDServerOperation
{
	private static String RUN = "RUN";
	private static String PAUSE = "PAUSE";
	private static String RUNSTATUS = "R";
	
	HIDManager hidManager = null;

	private HIDServerComm hidServerComm = null;
	private ConcurrentHashMap<String, HID> hidData = null;
	private OperationThread hidOperationThread = null;

	private String hidServerID = ""; // HIDServer Name
	private String ipAddress = "127.0.0.1";
	private int socketPort = 2100;
	private int threadInterval = 1000; // OperationThread의 Interval

	private int hidCipher = 2; // 2010.01.07 by MYM : HID 자릿수
//	private boolean isPowerControlUsed = false;
	private int statusSendCount = 0;
	
	private static final String HID_SERVER_OPERATION_TRACE = "HIDServerOperationDebug";
	private static final String HID_SERVER_OPERATION_EXCEPTION_TRACE = "HIDServerOperationException";
	private static Logger operationTraceLog = Logger.getLogger(HID_SERVER_OPERATION_TRACE);
	private static Logger operationExceptionTraceLog = Logger.getLogger(HID_SERVER_OPERATION_EXCEPTION_TRACE);
	/**
	 * HIDServerOperation의 생성자이다.
	 */
	public HIDServerOperation(HIDManager hidManager, int hidCipher) {
		this.hidManager = hidManager;

		this.hidServerComm = new HIDServerComm(this);
		this.hidData = new ConcurrentHashMap<String, HID>(); 

		// 2010.01.07 by MYM : HID 자릿수
		// 전체 또는 일부 HID의 상태값을 요청할 때의 자릿수 값을 설정한다.
		// ex) - 자릿수가 2일 때
		//       . 전체 : $T00
		//       . 특정HID : $T01
		//     - 자릿수가 3일 때
		//       . 전체 : $T000
		//       . 특정HID : $T001
		this.hidCipher = hidCipher;
	}

	/**
	 * HIDServer Operation Timer - 1초 Timer
	 * 정상 및 비정상 Case 검색 및 처리
	 * <p>Title: </p>
	 * <p>Description: </p>
	 * <p>Copyright: Copyright (c) 2005</p>
	 * <p>Company: </p>
	 * @author not attributable
	 * @version 1.0
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
	 * HID로부터 수신된 상태정보를 운영모듈에 알려준다.
	 */
//	public void ReceivedStatusMsg(String name, int hidCnt, String id[],
//			String status[], String errorCode[]) {
//
//		// HID Server 정보가 유효하지 않은 경우
//		if (name.equals(hidServerID) == false) {
//			StringBuffer log = new StringBuffer();
//			log.append("[ReceivedStatusMsg] HIDServerName mismatch: ").append(name);
//			operationTrace(log.toString(), null);
//			return;
//		}
//
//		for (int i = 0; i < hidCnt; i++) {
//			StringBuffer log = new StringBuffer();
//			log.append("HID").append(id[i]).append(" > Status:").append(status[i]).append(", ErrorCode:").append(errorCode[i]);			
//			operationTrace(log.toString(), null);
//
//			try {
//				HID hid = hidData.get(Integer.parseInt(id[i]) + "");
//				if (hid != null) {
//					hid.setStatus(status[i]);
//					hid.setErrorCode(errorCode[i]);
//					hidManager.addStatusToUpdateList(hid);
//				}
//			} catch (Exception e) {
//				operationTrace("ReceivedStatusMsg ", e);
//			}
//		}
//	}

	/**
	 * HID로부터 수신된 상태정보를 운영모듈에 알려준다. BackUPHIDInfo 포함
	 * 2011.02.28 by LWG [Backup HID 관련]
	 */
	public void ReceivedStatusMsg(String name, int hidCnt, String id[],
			String status[], String errorCode[], String backupHID[]) {

		// HID Server 정보가 유효하지 않은 경우
		if (name.equals(hidServerID) == false) {
			StringBuffer log = new StringBuffer(); 
			log.append("[ReceivedStatusMsg] HIDServerName mismatch: ").append(name);
			operationTrace(log.toString(), null);
			return;
		}

		for (int i = 0; i < hidCnt; i++) {
			StringBuffer log = new StringBuffer(); 
			log.append("HID").append(id[i]).append(" > Status:").append(status[i]).append(", ErrorCode:").append(errorCode[i]);
			// 2012.11.07 by MYM : BackupHid 업데이트 안되는 문제 수정
			if (backupHID != null) {
				log.append(", Backup HID:").append(backupHID[i]);
			}
			operationTrace(log.toString(), null);

			try {
				HID hid = hidData.get(Integer.parseInt(id[i]) + "");			
				if (hid != null) {
					hid.setStatus(status[i]);
					hid.setErrorCode(errorCode[i]);
					// 2012.11.07 by MYM : BackupHid 업데이트 안되는 문제 수정
					if (backupHID != null) {
						hid.setBackupHID(backupHID[i]);
						hidManager.addStatusWithBackupHidToUpdateList(hid);
					} else {
						hidManager.addStatusToUpdateList(hid);
					}
				}
			} catch (Exception e) {
				operationTrace("ReceivedStatusMsg ", e);
			}
		}
	}

	/**
	 * HID로부터 수신된 상태정보를 운영모듈에 알려준다.
	 */
//	public void ReceivedStatusMsg(String name, int HIDCnt, String id[],
//			String status[], String voltage[], String current[],
//			String temperature[], String frequency[], String errorCode[]) {
//
//		// HID Server 정보가 유효하지 않은 경우
//		if (name.equals(hidServerID) == false) {
//			StringBuffer log = new StringBuffer(); 
//			log.append("[ReceivedStatusMsg] HIDServerName mismatch: ").append(name);
//			operationTrace(log.toString(), null);
//			return;
//		}
//
//		for (int i = 0; i < HIDCnt; i++) {
//			StringBuffer log = new StringBuffer(); 
//			log.append("HID").append(id[i]).append(" > Status:").append(status[i]).append(", Voltage:").append(voltage[i]);
//			log.append(", Current:").append(current[i]).append(", Temperature:").append(temperature[i]).append(", Frequency:").append(frequency[i]);
//			log.append(", ErrorCode:").append(errorCode[i]);
//			operationTrace(log.toString(), null);
//			
//			try {
//				HID hid = hidData.get(Integer.parseInt(id[i]) + "");
//				if (hid != null) {
//					hid.setStatus(status[i]);
//					hid.setErrorCode(errorCode[i]);
//					hid.setDetailStatus(voltage[i], current[i], temperature[i], frequency[i]);
//					hidManager.addStatusToUpdateList(hid);
//				}
//			} catch (Exception e) {
//				operationTrace("ReceivedStatusMsg ", e);
//			}
//		}
//	}

	/**
	 * HID로부터 수신된 상태정보를 운영모듈에 알려준다.
	 * 2011.02.28 by LWG [Backup HID 관련]
	 */
	public void ReceivedStatusMsg(String name, int HIDCnt,
			String id[], String status[], String voltage[], String current[],
			String temperature[], String frequency[], String errorCode[],
			String backupHID[]) {

		// HID Server 정보가 유효하지 않은 경우
		if (name.equals(hidServerID) == false) {
			StringBuffer log = new StringBuffer(); 
			log.append("[ReceivedStatusMsg] HIDServerName mismatch: ").append(name);
			operationTrace(log.toString(), null);
			return;
		}

		for (int i = 0; i < HIDCnt; i++) {
			StringBuffer log = new StringBuffer(); 
			log.append("HID").append(id[i]).append(" > Status:").append(status[i]).append(", Voltage:").append(voltage[i]);
			log.append(", Current:").append(current[i]).append(", Temperature:").append(temperature[i]).append(", Frequency:").append(frequency[i]);
			log.append(", ErrorCode:").append(errorCode[i]);
			// 2012.11.07 by MYM : BackupHid 업데이트 안되는 문제 수정
			if (backupHID != null) {
				log.append(", Backup HID:").append(backupHID[i]);
			}
			operationTrace(log.toString(), null);

			try {
				HID hid = hidData.get(Integer.parseInt(id[i]) + "");
				if (hid != null) {
					hid.setStatus(status[i]);
					hid.setErrorCode(errorCode[i]);
					hid.setDetailStatus(voltage[i], current[i], temperature[i], frequency[i]);
					// 2012.11.07 by MYM : BackupHid 업데이트 안되는 문제 수정
					if (backupHID != null) {
						hid.setBackupHID(backupHID[i]);
						hidManager.addStatusWithBackupHidToUpdateList(hid);
					} else {
						hidManager.addStatusToUpdateList(hid);
					}
				}
			} catch (Exception e) {
				operationTrace("ReceivedStatusMsg ", e);
			}
		}
	}

	/**
	 * HID로부터 수신된 응답정보를 운영모듈에 알려준다.
	 */
	public void ReceivedCommandReplyMsg(String strName, String strHID,
			String strCmd, char cReply) {
		HID hid = null;
		try {
			hid = hidData.get(Integer.parseInt(strHID) + "");
			if (hid == null) {
				return;
			}
		} catch (Exception e) {
			operationTrace("ReceivedCommandReplyMsg ", e);
		}

		switch (cReply) {
		case 'A':
			if (strCmd.equals("p")) {
				// 2012.06.12 by MYM : 상태 수신되었을 때만 업데이트 하도록 함.
//				hid.setStatus("S");
//				hidManager.addStatusToUpdateList(hid);
			} else if (strCmd.equals("r")) {
				// 2012.06.12 by MYM : 상태 수신되었을 때만 업데이트 하도록 함.
//				hid.setStatus("R");
//				hidManager.addStatusToUpdateList(hid);
			} else {
				operationTrace("[ReceivedCommandReplyMsg] Not Defined Status Error from the HID.", null);
			}
			break;
		case 'E':
			operationTrace("[ReceivedCommandReplyMsg] Reply Error from the HID.", null);
			break;
		default:
			break;
		}
	}

	/**
	 * 전체 또는 일부 HID의 상태값을 요청할 때의 자릿수 값을 반환한다.
	 *
	 * @return int
	 * @version created by MYM 2010.01.07
	 */
	public int getHIDCipher() {
		return hidCipher;
	}

	public boolean addHID(HID hid) {
		if (hidData.get(hid.getHid()) == null) {
			hidData.put(hid.getHid(), hid);
			return true;
		}
		return false;
	}
	
	public HID removeHID(String hidName) {
		return hidData.remove(hidName);
	}

	public String getIpAddress() {
		return ipAddress;
	}
	/**
	 * Operation하는 HID의 정보를 설정한다.
	 */
	public void SetOperationInfo(String strName, String strIPAddress) {
		// Operation 모듈에서 VehicleComm 모듈의 Vehicle정보를 설정
		this.hidServerID = strName;
		this.ipAddress = strIPAddress;
		hidServerComm.SetTargetInfo(strName, strIPAddress, socketPort);
	}

	/**
	 * Operation을 관리하는 Timer를 시작한다.
	 */
	public void OperationStart()
	{
		// 2009.03.11 by MYM : HID 마스터 통신 상태를 1000으로 초기화
		updateCommFail();
		
		// Main Operation Timer Thread 생성 및 실행
		hidOperationThread = new OperationThread();
		hidOperationThread.start();
		
		hidServerComm.HIDServerCommStart();
	}

	/**
	 * Operation을 관리하는 Timer를 종료한다.
	 */
	public void OperationStop()
	{
		if (hidOperationThread != null)
		{
			// Operation 정리
			hidOperationThread.m_bRun = false;
			hidServerComm.HIDServerCommStop();

			hidOperationThread = null;
			hidServerComm = null;
		}
	}

	/**
	 * Operation에서 처리할 Process를 관리한다.
	 */
	void OperationProcess()
	{
		// 2012.06.12 by MYM : 통신 단에서 하는 것을 여기로 이동
		manageRequestHIDStatus();
		
		// 2012.05.30 by MYM : HID On/Off 기능 사용
		manageHIDPowerControl();		
	}
	
	private void manageRequestHIDStatus() {
		// 2012.06.12 by MYM : 통신 단에서 하는 것을 여기로 이동
		boolean result = true;
		if (statusSendCount % 5 != 0)	{
			// 2010.01.07 by MYM : 기존라인의 경우 "00"로 요청
			//                     MFAB Can통신 유닛은 "000"로 요청한다.
			if (hidCipher == 2) {
				result = SendStatusCommand("00");
			} else if (hidCipher == 3) {
				result = SendStatusCommand("000");
			}
		} else {
			// 2010.01.07 by MYM : 기존라인의 경우 "00"로 요청
			//                     MFAB Can통신 유닛은 "000"로 요청한다.
			if (hidCipher == 2) {
				result = SendDetailStatusCommand("00");
			} else if (hidCipher == 3) {
				result = SendDetailStatusCommand("000");
			}
			if (result) {
				statusSendCount = 0;
			}
		}
		if (result) {
			statusSendCount++;
		}	
	}

	private void manageHIDPowerControl() {
		// 2012.08.05 by MYM : HID Power Control 실시간 반영하도록 변경
		boolean isPowerControlUsed = hidManager.isHIDPowerControlUsed();
		
		// 2012.05.30 by MYM : OnOff 기능 사용
		for (Enumeration<HID> e = hidData.elements(); e.hasMoreElements();) {
			HID hid = e.nextElement();
			if (hid != null) {
				String remoteCmd = hid.getRemoteCmd();
				if (isPowerControlUsed) {
					if (RUN.equals(remoteCmd)) {
//						if (RUNSTATUS.equals(hid.getStatus()) == false) {
//							SendResumeCommand(hid.getHid());
//						}
					} else if (PAUSE.equals(remoteCmd)) {
						if (RUNSTATUS.equals(hid.getStatus())) {
							SendPauseCommand(hid.getHid());
						}
					}
				}
				if (remoteCmd.length() > 0) {
					resetRemoteCmd(hid);
				}
				hid.setRemoteCmd("");
			}
		}
	}

	private void resetRemoteCmd(HID hid) {
		if (hid != null) {
			hidManager.addRemoteCmdToUpdateList(hid);
		}
	}

	/**
	 * HID Server로 Pause Command 메시지를 생성해서 전송한다.
	 */
	public boolean SendPauseCommand(String strHID)
	{
		return hidServerComm.SendPauseCommand(strHID);
	}

	/**
	 * HID Server로 Resume Command 메시지를 생성해서 전송한다.
	 */
	public boolean SendResumeCommand(String strHID)
	{
		return hidServerComm.SendResumeCommand(strHID);
	}

	/**
	 * HID Server로 Status Command 메시지를 생성해서 전송한다.
	 */
	public boolean SendStatusCommand(String strHID)
	{
		return hidServerComm.SendStatusCommand(strHID);
	}

	/**
	 * HID Server로 DetailStatus Command 메시지를 생성해서 전송한다.
	 */
	public boolean SendDetailStatusCommand(String strHID)
	{
		return hidServerComm.SendDetailStatusCommand(strHID);
	}

	/**
	 * HID 객체의 갯수를 얻는다.
	 */
	int getHIDCount() {
		return hidData.size();
	}
	
	public ConcurrentHashMap<String, HID> getHidList() {
		return hidData;
	}
	
	public void updateCommFail() {
		hidManager.addCommfailToUpdateList(ipAddress);
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
