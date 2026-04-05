package ocsmanager;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.lang.Object;

import java.sql.ResultSet;
import java.util.Vector;
import java.sql.SQLException;
import java.util.TimerTask;


/**TrCmdOperation
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: </p>
 * @author not attributableTransferCompleted
 * @version 1.0
 */

/*
MCS의 OCS처리 모듈의 시퀀스 및 통신사양 정리
1. System Start
          MCS		: Alarm Clear
   OCS -> MCS 		: TSCControl 상태보고 [RepTSCControlStatus]
   OCS -> MCS 		: TSC 상태보고 [RepTSCStatus]
          MCS -> Host	: DSE 보고 [DSE]
   OCS -> MCS 		: Carrier 정보 동기화보고 [RepEnhancedCarrierInfo]
   OCS -> MCS 		: TrCmd 정보 동기화보고 [RepEnhancedTrCmd]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleInfo]

2. TrCmd 수신시 Normal 반송
OCS <- MCS 		: 반송명령 [SendMicroTC:TRANSFER]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:QUEUED]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferInitiated]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleAssigned]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleArrived]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:Transferring]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleAcquireStarted]
   OCS -> MCS 		: Carrier 상태보고 [RepCarrierStatus:CarrierInstalled]
          MCS -> Host	: PSE 보고 [PSE:EMPTY]
          MCS -> Host	: PSE 보고 [PSE:OCCUPIED]
          MCS -> Host	: PLCE 보고 [PLCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleAcquireCompleted]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDeparted]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleArrived]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositStarted]
   OCS -> MCS 		: Carrier 상태보고 [RepCarrierStatus:CarrierRemoved]
          MCS -> Host	: PSE 보고 [PSE:EMPTY]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositCompleted]
          MCS -> Host	: PSE 보고 [PSE:OCCUPIED]
          MCS -> Host	: PLCE 보고 [PLCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCompleted]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PSE 보고 [PMR_COM]
          MCS -> Host	: 최종 Dest로 반송완료시만 PRE 보고 [PRE]

3. Abnormal 처리: Cancel과 Abort의 경우에는 QUEUED와 Initiated 순서가 뒤바뀌어 수신되나 상관없음
3.1 명령 Cancel처리 : STK에서 PickUp 이전(Transferring 보고 이전)에 Cancel 명령 수신 (By RemoteCmd, STK FULL, STK Down, STK Port Error)
   OCS <- MCS 		: 반송명령 [SendMicroTC:CANCEL]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCancelInitiated]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:CANCELQUEUED]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCancelCompleted]
          MCS -> Host	: PMCE 보고 [PMCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]

3.2 대기후 대체 반송처리 : EQ에서 PickUp 이전(Transferring 보고 이전)에 Cancel 명령 수신 (By RemoteCmd, STK FULL, STK Down, STK Port Error)
   MCS는 PickUP 완료까지 기다린 이후에...
   OCS <- MCS 		: 반송명령 [SendMicroTC:ABORT]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferAbortInitiated]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:ABORTQUEUED]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferAbortCompleted]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]
   OCS <- MCS 		: 대체반송명령 [SendMicroTC:TRANSFER]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:Transferring]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:QUEUED]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositStarted]
   OCS -> MCS 		: Carrier 상태보고 [RepCarrierStatus:CarrierRemoved]
          MCS -> Host	: PSE 보고 [PSE:EMPTY]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositCompleted]
          MCS -> Host	: PSE 보고 [PSE:OCCUPIED]
          MCS -> Host	: PLCE 보고 [PLCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCompleted]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PSE 보고 [PMR_COM]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PRE 보고 [PRE]
   Dest의 대체Port, 대체STK, TSC의 대체STK 순으로 반송
   대체반송 이후 원래 Dest로 반송할 지는 Option임
   대체가 없는 경우에는 반송하지 못하므로 작업자 조치필요

3.3 대체 반송처리 : PickUp 이후(Transferring 보고 이후)에 Cancel 명령 수신 (By RemoteCmd, STK FULL, STK Down, STK Port Error)
   OCS <- MCS 		: 반송명령 [SendMicroTC:ABORT]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferAbortInitiated]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:ABORTQUEUED]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferAbortCompleted]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]
   OCS <- MCS 		: 대체반송명령 [SendMicroTC:TRANSFER]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:Transferring]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:QUEUED]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositStarted]
   OCS -> MCS 		: Carrier 상태보고 [RepCarrierStatus:CarrierRemoved]
          MCS -> Host	: PSE 보고 [PSE:EMPTY]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositCompleted]
          MCS -> Host	: PSE 보고 [PSE:OCCUPIED]
          MCS -> Host	: PLCE 보고 [PLCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCompleted]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PSE 보고 [PMR_COM]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PRE 보고 [PRE]
   Dest의 대체Port, 대체STK, TSC의 대체STK 순으로 반송
   대체반송 이후 원래 Dest로 반송할 지는 Option임
   대체가 없는 경우에는 반송하지 못하므로 작업자 조치필요

3.4 대체 반송처리 : PickUp 작업중 PIO에러에 의해 Cancel 완료보고 수신
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCancelCompleted]
          MCS -> Host	: PMCE 보고 [PMCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]

3.5 대체 반송처리 : Deposit 작업중 PIO에러에 의해 비정상 완료보고 수신
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCompleted ResultCode=1]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]
   OCS <- MCS 		: 대체반송명령 [SendMicroTC:TRANSFER]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:Transferring]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:QUEUED]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositStarted]
   OCS -> MCS 		: Carrier 상태보고 [RepCarrierStatus:CarrierRemoved]
          MCS -> Host	: PSE 보고 [PSE:EMPTY]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositCompleted]
          MCS -> Host	: PSE 보고 [PSE:OCCUPIED]
          MCS -> Host	: PLCE 보고 [PLCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCompleted]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PSE 보고 [PMR_COM]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PRE 보고 [PRE]
   Dest의 대체Port, 대체STK, TSC의 대체STK 순으로 반송
   대체반송 이후 원래 Dest로 반송할 지는 Option임
   대체가 없는 경우에는 반송하지 못하므로 작업자 조치필요

3.6 InTransTimeout 발생
   OCS <- MCS 		: 대체반송명령 [SendMicroTC:TRANSFER]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:Transferring]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:QUEUED]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositStarted]
   OCS -> MCS 		: Carrier 상태보고 [RepCarrierStatus:CarrierRemoved]
          MCS -> Host	: PSE 보고 [PSE:EMPTY]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleDepositCompleted]
          MCS -> Host	: PSE 보고 [PSE:OCCUPIED]
          MCS -> Host	: PLCE 보고 [PLCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCompleted]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PSE 보고 [PMR_COM]
          MCS -> Host	: 최종 Dest로 반송완료시에만 PRE 보고 [PRE]
   TSC의 대체STK로 반송
   대체반송 이후 원래 Dest로 반송할 지는 Option임
   대체가 없는 경우에는 Timeout이후에도 반송하지 못하므로 작업자 조치필요

3.7 PickUp작업중 공반송 에러가 발생하거나 작업자가 작업을 삭제하는 경우
   OCS -> MCS 		: TrCmd 상태보고 [RepMicroTCStatus:TransferCancelCompleted]
          MCS -> Host	: PMCE 보고 [PMCE]
   OCS -> MCS 		: Vehicle 상태보고 [RepVehicleStatus:VehicleUnassigned]

3.8 작업중 PIO외의 에러가 발생한 경우
   반송하지 못하므로 작업자 조치필요

// 2010.02.03 by IKY : Local OHT 기능 추가
*/
public class OCSManagerMain
 {
	// Version ID
	String m_strVersionID = "2021.3.30"; // 이전버전이 OM_1_20_20100203 임.

	// 2015.01.30 by KYK
	String tscType = "OCS";

	// HangCheck 선언
	String m_sHangCheck;

	OCSManagerMainFrame m_Owner = null;

	// DB Access Frame
	DBAccessFrame m_dbFrame = null;

	// IBSEM I/F Dialog
	// 2015.01.30 by KYK
	boolean semInit = false;
	SEMIF semIF = null;
	boolean m_bSendS2F49 = false;
	boolean m_bSendS2F41 = false;
	long m_lLastSendS2F49 = 0;
	long m_lLastSendS2F41 = 0;

	// Utility
	utilLog Util = null; // utility class reference
	String m_sLogPath = "";
	int m_nDeleteLogDay = -1;
	long m_lLastDeleteLogTime = 0;

	// TSC ID : 내 자신이 제어하는 TSC ID : public 전환  20110907 by LWG
	public String m_strTSCID = null;

	// IBSEM Config File Name
	String m_strIBSEMConfigFileName = "";

	// Thread가 정상 가동중인지를 감지하는 Timestamp
	private long m_lMainOperationTimeStamp = System.currentTimeMillis();
	private long m_lAbnormalOperationTimeStamp = System.currentTimeMillis();
	boolean m_bMainOperationTimerActiveFlag = true;
	boolean m_bAbnormalOperationTimerActiveFlag = true;

	// Control Parameter
	private int m_nTempHoldedTimeout = 0;
	private int m_nInTransitTimeout = 0;

	// 동기화 여부 Flag
	boolean m_bSyncCarrierInfo = false;
	boolean m_bOnSyncAll = false;

	// Timer
	java.util.Timer MainTimer = new java.util.Timer();
	MainTimerProc MainTimerTask = new MainTimerProc(this);

	java.util.Timer AbnormalTimer = new java.util.Timer();
	AbnormalTimerProc AbnormalTimerTask = new AbnormalTimerProc(this);

	java.util.Timer WatchDogTimer = new java.util.Timer();
	WatchDogTimerProc WatchDogTimerTask = new WatchDogTimerProc(this);

	// 상수선언 ----------------------
	// Error Code
	final int OK = 0;
	final int ERR_USERDEFINED = -1000;
	final int ERR_LOADCONFIG_FAIL = ERR_USERDEFINED - 1;
	final int ERR_DBCONNECTION_FAIL = ERR_USERDEFINED - 2;

	// Transfer Type
	final int NORMAL_TRANSFER = 0;
	final int ALT_PORT_TRANSFER = 1;
	final int ALT_STK_TRANSFER = 2;
	final int HOLD_TRANSFER = 3;
	final int TEMPHOLD_TRANSFER = 4;

	//2011.09.07 by LWG : [롱런을 읽어서 반송 내리기]
	private FileLongRunManager flrm = null;

	//2012.01.25 by LWG [Cancel/Abort 내리기]
	private LongRunManager lrm = null;

	public OCSManagerMain(OCSManagerMainFrame frame) {
		m_Owner = frame;

		String strLog = "";
		int nRet = Initialize();
		if (nRet != OK) {
			String strErrorText = GetErrorText(nRet);
			strLog = "OCSManager 실행 시작 -----------------------------";
			WriteLog("");
			WriteLog(strLog);
			strLog = "OCSManager 정상실행 실패: " + strErrorText + "(으)로 인한 초기화 실패";
			WriteLog(strLog);

			if (m_Owner != null)
				m_Owner.TerminateProgram();
		}

		strLog = "OCSManager 실행 시작 -----------------------------";
		WriteLog("");
		WriteLog(strLog);

		strLog = "OCSManager 초기화 완료";
		WriteLog(strLog);

		Util.WriteVersionHistory("OCSManager", m_strVersionID);
	}

	/**
	 * 초기화. DB Frame 및 IBSEM I/F 생성
	 * 
	 * @return int
	 */
	int Initialize() {
		// OCSManager.ini로부터의 Configuration 정보(TSCID 등) 얻기
		if (LoadConfig() == false) {
			Util = new utilLog(m_sLogPath, m_nDeleteLogDay);
			return ERR_LOADCONFIG_FAIL;
		}
		Util = new utilLog(m_sLogPath, m_nDeleteLogDay);

		// DB Access Frame 생성
		m_dbFrame = new DBAccessFrame(this);
		//    if (m_dbFrame.IsDBConnected() == false)
		//      m_dbFrame.ReconnectToDB();

		flrm = new FileLongRunManager(this, Util);
		// 2012.01.25 by LWG [Cancel/Abort 내리기]
		lrm = new LongRunManager(this, Util);

		m_lMainOperationTimeStamp = System.currentTimeMillis();
		m_lAbnormalOperationTimeStamp = System.currentTimeMillis();

		// Main Operation Timer Thread 생성 및 실행
		// 주기적인 Timer 동작개시
		MainTimer.schedule(MainTimerTask, 0, 1000);

		// Abnormal Operation Timer Thread 생성 및 실행
		AbnormalTimer.schedule(AbnormalTimerTask, 0, 10000);

		// Thread Monitor Timer Thread 생성 및 실행
		if ((m_sHangCheck != null) && m_sHangCheck.equals("true") == true) {
			Util.ThreadHangCheckLog(".READY", "");
		}
		WatchDogTimer.schedule(WatchDogTimerTask, 0, 1000);

		return OK;
	}

	void Display(String strMsg) {
		m_Owner.DisplayLog(strMsg);

	}

	void InitalizeSEM() {
		// IBSEM Config File Name
		//    m_strIBSEMConfigFileName = GetIBSEMConfigFileName();
		m_strIBSEMConfigFileName = m_strTSCID + ".cfg";

		// 2013.10.08 by MYM : DB 연결 없이 실행
		// TSC 상태(CommunicationStatus/ControlStatus/TSCStatus) 초기화
		//    InitalizeTSCStatus();
		//    RemoveAllAlarm();

		// IBSEM I/F Dlg 생성
		// 2015.01.30 by KYK
		if (semIF == null) {
			if ("STBC".equals(tscType)) {
				semIF = new STBSEMIF(this);
			} else {
				semIF = new IBSEMIF(this);
			}
			semIF.SetTSCName(m_strTSCID);
		}
	}

	/**
	 * OCSManager 초기 실행 시 TSC 상태 정보를 초기화 한다. CommunicationStatus :
	 * NOT_COMMUNICATING ControlStatus : OFFLINE TSCStatus : PAUSED
	 */
	void InitalizeTSCStatus() {
		String strSql = "UPDATE TSC SET CommunicationStatus='NOT_COMMUNICATING', ControlStatus='OFFLINE',";
		strSql += " TSCStatus='NONE', DataUpdatedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE TSCID='" + m_strTSCID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "InitalizeTSCStatus - SQLException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 외부 Dialog 또는 Frame이 호출하는 Function. Msg 변수를 Parsing하여 기능별 Function 재호출
	 * 
	 * @param msg
	 *            MyHashtable
	 * @return int
	 */
	int CallProc(MyHashtable msg) {
		int nRet = 0;

		if (msg.isEmpty() == false) {
			String strMsg = "";
			Enumeration msgEnum = msg.keys();
			strMsg = "[" + msg.toString("MessageName", 0) + "] ";
			while (msgEnum.hasMoreElements() == true) {
				String strKey = (String) msgEnum.nextElement();
				if (strKey.equals("MessageName"))
					continue;
				try {
					strMsg += strKey + ":" + (String) msg.get(strKey) + ", ";
				} catch (Exception e) {
					try {
						strMsg += strKey + ":" + String.valueOf(msg.toInt(strKey, 0)) + " ";
					} catch (Exception e1) {
						Vector vtVal = (Vector) msg.get(strKey);
						for (int i = 0; i < vtVal.size(); i++) {
							try {
								strMsg += strKey + "(" + String.valueOf(i) + "):" + (String) vtVal.get(i) + ", ";
							} catch (Exception e2) {
								strMsg += strKey + "(" + String.valueOf(i) + "):" + String.valueOf((Integer) vtVal.get(i)) + ", ";
							}
						}
					}
				}
			}
			WriteLog(strMsg);
		}

		String strMsgName = msg.toString("MessageName", 0);
		if (strMsgName.equals("RepAlarm")) {
			RepAlarm(msg);
		} else if (strMsgName.equals("RepCarrierStatus")) {
			RepCarrierStatus(msg);
		} else if (strMsgName.equals("RepMicroTCStatus")) {
			RepMicroTCStatus(msg);
		} else if (strMsgName.equals("RepVehicleStatus")) {
			RepVehicleStatus(msg);
		} else if (strMsgName.equals("RepTSCControlStatus")) {
			RepTSCControlStatus(msg);
		} else if (strMsgName.equals("RepTSCStatus")) {
			RepTSCStatus(msg);
		} else if (strMsgName.equals("RepEnhancedTrCmd")) {
			RepEnhancedTrCmd(msg);
		} else if (strMsgName.equals("RepEnhancedCarrierInfo")) {
			RepEnhancedCarrierInfo(msg);
		} else if (strMsgName.equals("RepVehicleInfo")) {
			RepVehicleInfo(msg);
		} else if (strMsgName.equals("RepEnhancedAlarm")) {
			RepEnhancedAlarm(msg);
		}

		return nRet;
	}

	/**
	 * TSC 제어상태 변경 요청 ONLINE/OFFLINE
	 * 
	 * @param strStatus
	 *            String
	 */
	void ReqTSCControlStatusChange(String strStatus) {
		MyHashtable msg = new MyHashtable();
		msg.put("MessageName", "ReqTSCControlStatusChange");
		msg.put("TSC", m_strTSCID);
		msg.put("ControlStatus", strStatus);
		semIF.CallProc(msg);
	}

	/**
	 * TSC 제어상태 변경 보고 처리 ONLINE/OFFLINE
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void RepTSCControlStatus(MyHashtable msg) {
		// TSC ControlStatus 갱신
		String strControlStatus = msg.toString("ControlStatus", 0);
		String strCommunicationStatus = msg.toString("CommunicationStatus", 0);
		int nAck = msg.toInt("ONOFFLINEACK", 0);

		if (strControlStatus.equals("ONLINEFAIL") && (nAck == 2)) // Ack=2(already online)
		{
			strControlStatus = "ONLINE";
		} else if (strControlStatus.equals("OFFLINEFAIL") && (nAck == 2)) // Ack=2(already offline)
		{
			strControlStatus = "OFFLINE";
		}

		UpdateControlStatus(strControlStatus, strCommunicationStatus);
		if (strControlStatus.equals("OFFLINE")) {
			UpdateTSCStatus("NONE");
		}
	}

	/**
	 * TSC 상태 변경 요청 AUTO/INIT/PAUSING/PAUSED
	 * 
	 * @param strStatus
	 *            String
	 */
	void ReqTSCStatusChange(String strStatus) {
		MyHashtable msg = new MyHashtable();
		msg.put("MessageName", "ReqTSCStatusChange");
		msg.put("TSC", m_strTSCID);
		msg.put("TSCStatus", strStatus);
		semIF.CallProc(msg);
	}

	void ReqTSCStatusChange(CMessage msg) {
		MsgVector value = new MsgVector();
		msg.GetMessageItem("TSCStatus", value, 0, false);
		String strTSCStatus = value.toString(0);

		ReqTSCStatusChange(strTSCStatus);
	}

	/**
	 * TSC 상태 요청 AUTO/INIT/PAUSING/PAUSED
	 */
	void GetTSCStatus() {
		MyHashtable msg = new MyHashtable();
		msg.put("MessageName", "GetTSCStatus");
		msg.put("TSC", m_strTSCID);

		semIF.CallProc(msg);
	}

	/**
	 * TSC 상태 변경 보고 처리 AUTO/INIT/PAUSING/PAUSED
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void RepTSCStatus(MyHashtable msg) {
		// TSC Status 갱신
		String strTSCStatus = msg.toString("TSCStatus", 0);

		// 2005.10.10 AlarmClear 기능추가
		MyHashtable tscInfo = new MyHashtable();
		GetTSCInfo(tscInfo);
		if (tscInfo.toString("TSCStatus", 0).equals("AUTO") == false) {
			if (strTSCStatus.equals("AUTO")) {
				RemoveAllAlarm();
			}
		}

		UpdateTSCStatus(strTSCStatus);

		// Host Report : DSE(Device Status Event)
		HostReport_DSE();

		// 2005.08.08 Sync All 요청이 걸려 있을 경우 Enhanced Carrier 정보 요청함.
		if (m_bOnSyncAll) {
			GetEnhancedCarrierInfo();
		}
	}

	boolean SendMicroTC(MyHashtable msg) {
		// 2005.10.11 순차적으로 이전메시지의 Reply받으면 전송
		if (m_bSendS2F49 == true) {
			return false;
		}
		// 반송명령 상태 변경 : Status=IFQueued
		UpdateTrCmdStatus(msg.toString("MicroTrCmdID", 0), msg.toString("MacroTrCmdID", 0), "IFQUEUED");

		// 2006.03.28 반송명령의 상태를 변경한 이후에 메시지를 보내도록 변경
		msg.put("MessageName", "SendMicroTC");
		msg.put("MicroTrCmdType", "TRANSFER");
		m_bSendS2F49 = true;
		m_lLastSendS2F49 = System.currentTimeMillis();
		semIF.CallProc(msg);
		return true;
	}

	boolean SendAbort(MyHashtable msg) {
		// 2005.10.11 순차적으로 이전메시지의 Reply받으면 전송
		if (m_bSendS2F41 == true) {
			return false;
		}
		// 반송명령 상태 변경 : Status=IFQueued
		UpdateTrCmdStatus(msg.toString("MicroTrCmdID", 0), msg.toString("MacroTrCmdID", 0), "IFQUEUED");

		// 2006.03.28 반송명령의 상태를 변경한 이후에 메시지를 보내도록 변경
		msg.put("MessageName", "SendMicroTC");
		msg.put("MicroTrCmdType", "ABORT");
		m_bSendS2F41 = true;
		m_lLastSendS2F41 = System.currentTimeMillis();
		semIF.CallProc(msg);
		return true;
	}

	boolean SendCancel(CMessage msg, String strSender) {
		// 2005.10.11 순차적으로 이전메시지의 Reply받으면 전송
		if (m_bSendS2F41 == true) {
			return false;
		}

		MsgVector value = new MsgVector();
		msg.GetMessageItem("MicroTrCmdID", value, 0, false);
		String strMicroTrCmdID = value.toString(0);
		MyHashtable trCmdInfo = new MyHashtable();
		boolean bRet = GetMicroTCInfo(strMicroTrCmdID, trCmdInfo);
		MyHashtable locInfo = new MyHashtable();

		// 2005.08.12 TOCS에 해당되는 TrCmd만 처리
		if (bRet && trCmdInfo.toString("TSC", 0).equals(m_strTSCID)) {
			String strCarrierID = trCmdInfo.toString("CarrierID", 0);
			String strMacroTrCmdID = trCmdInfo.toString("MacroTrCmdID", 0);
			String strStatus = trCmdInfo.toString("Status", 0);
			String strCarrierLoc = trCmdInfo.toString("SourceLoc", 0);

			//2005.10.28 EQ에서 반송하는 경우에 Abort로 처리되도록 PickUp전까지 Skip하도록 변경
			if (!strSender.equals("HOSTIF") && GetCarrierLocInfo(strCarrierLoc, locInfo) && locInfo.toString("Type", 0).equals("EQPORT")
					&& (strStatus.equals("READY") || strStatus.equals("IFQUEUED") || strStatus.equals("QUEUED") || strStatus.equals("TransferInitiated"))) {
				return false;
			} else if (strStatus.equals("READY") || strStatus.equals("NONE") || strStatus.equals("FAIL") || strStatus.equals("IFQUEUED")) {
				UpdateTrCmdStatus(strMicroTrCmdID, strMacroTrCmdID, "TempHolded");
				// HostReport : PMCE
				HostReport_PMCE(strCarrierID);
			} else if (strStatus.equals("CANCELQUEUED") || strStatus.equals("TransferCancelInitiated") || strStatus.equals("TransferCancelCompleted") || strStatus.equals("ABORTQUEUED")
					|| strStatus.equals("TransferAbortInitiated") || strStatus.equals("TransferAbortCompleted") || strStatus.equals("TransferCompleted")) {
				// 2005.08.12 Cancel, Abort가 진행중이거나 완료된 경우
			} else {
				trCmdInfo.put("MessageName", "SendMicroTC");
				if (strStatus.equals("QUEUED") || strStatus.equals("TransferInitiated")) {
					trCmdInfo.put("MicroTrCmdType", "CANCEL");
				} else {
					trCmdInfo.put("MicroTrCmdType", "ABORT");
				}

				// 반송명령 PrevActiveStatus 갱신
				UpdateTrCmdPrevActiveStatus(strMicroTrCmdID);

				// 반송명령 상태 변경 : Status=IFQueued
				UpdateTrCmdStatus(strMicroTrCmdID, strMacroTrCmdID, "IFQUEUED");

				m_bSendS2F41 = true;
				m_lLastSendS2F41 = System.currentTimeMillis();
				semIF.CallProc(trCmdInfo);
			}
		}
		return true;
	}

	/**
	 * Tr. Cmd 동기화 보고 요청
	 */
	void GetEnhancedTrCmd() {
		MyHashtable msg = new MyHashtable();
		msg.put("MessageName", "GetEnhancedTrCmd");
		msg.put("TSC", m_strTSCID);

		semIF.CallProc(msg);
	}

	/**
	 * Tr. Cmd 동기화 보고 수신 처리
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void RepEnhancedTrCmd(MyHashtable msg) {
		MyHashtable repMsg = new MyHashtable();
		MyHashtable locInfo = new MyHashtable();
		int i = 0;
		String strMicroTrCmdID = "";
		String strCarrierID = "";
		String strCarrierLocID = "";
		String strDest = "";
		String strStatus = "";

		int nCmdCnt = msg.toInt("CommandQty", 0);

		// DB상의 정보와 다르게 보고된 경우
		for (i = 0; i < nCmdCnt; i++) {
			strMicroTrCmdID = msg.toString("MicroTrCmdID", i);
			strCarrierID = msg.toString("CarrierID", i);
			strCarrierLocID = msg.toString("CarrierLocID", i);
			strDest = msg.toString("Dest", i);
			strStatus = GetTrCmdStatusFromStateVal(msg.toInt("TransferState", i));

			MyHashtable trCmdInfo = new MyHashtable();
			if (GetMicroTCInfo(strMicroTrCmdID, trCmdInfo)) {
				if (trCmdInfo.toString("CarrierID", 0).equals(strCarrierID)) {
					if (trCmdInfo.toString("Status", 0).equals(strStatus) == false) {
						UpdateTrCmdStatus(strMicroTrCmdID, trCmdInfo.toString("MacroTrCmdID", 0), strStatus);
					}
					/*
					 * if (trCmdInfo.toString("Dest", 0).equals(strDest) ==
					 * false) { UpdateTrCmdStatus(strMicroTrCmdID,
					 * trCmdInfo.toString("Dest", 0), strDest); } if
					 * (trCmdInfo.toString("CarrierLocID",
					 * 0).equals(strCarrierLocID) == false) {
					 * UpdateTrCmdStatus(strMicroTrCmdID,
					 * trCmdInfo.toString("CarrierLocID", 0), strCarrierLocID);
					 * }
					 */
				}
			}
		}

		// DB상에 등록된 MicroTrCmd가 보고되지 않은 경우
		String strSql = "SELECT * FROM MicroTrCmd WHERE TSC='" + m_strTSCID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if (rs != null) {
				while (rs.next()) {
					strMicroTrCmdID = rs.getString("MicroTrCmdID");
					strCarrierID = rs.getString("CarrierID");
					strCarrierLocID = rs.getString("DestLoc");
					strStatus = rs.getString("Status");

					for (i = 0; i < nCmdCnt; i++) {
						if (msg.toString("MicroTrCmdID", i).equals(strMicroTrCmdID)) {
							break;
						}
					}
					if (i == nCmdCnt) {
						MyHashtable carrierInfo = new MyHashtable();
						// MicroTrCmd가 MacroTrCmd의 최종인 경우
						if (IsLastCmd(strMicroTrCmdID, strCarrierID)) {
							if (!strStatus.equals("READY") && !strStatus.equals("NONE") && !strStatus.equals("FAIL") && !strStatus.equals("IFQUEUED")) {
								// HostReport : PMCE
								HostReport_PMCE(strCarrierID);
							}
						}
						// Pod 정보가 없는 경우
						else if (!GetCarrierInfo(strCarrierID, carrierInfo)) {
							// HostReport : PMCE
							HostReport_PMCE(strCarrierID);
						}
					}
				}
			}
		} catch (SQLException e) {
			String strLog = "SyncTrCmdList() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		// DB상에 없는 TrCmd에 대해서 보고 받은 경우
		for (i = 0; i < nCmdCnt; i++) {
			strMicroTrCmdID = msg.toString("MicroTrCmdID", i);
			MyHashtable trCmdInfo = new MyHashtable();
			MyHashtable carrierInfo = new MyHashtable();
			if (!GetMicroTCInfo(strMicroTrCmdID, trCmdInfo)) {
				strCarrierID = msg.toString("CarrierID", i);
				strCarrierLocID = msg.toString("CarrierLocID", i);
				strDest = msg.toString("Dest", i);
				strStatus = GetTrCmdStatusFromStateVal(msg.toInt("TransferState", i));

				// TrCmd 등록
				trCmdInfo.put("MicroTrCmdID", strMicroTrCmdID);
				trCmdInfo.put("CarrierID", strCarrierID);
				trCmdInfo.put("CarrierLocID", strCarrierLocID);
				trCmdInfo.put("Dest", strDest);
				trCmdInfo.put("TransferState", strStatus);
				if (GetCarrierInfo(strCarrierID, carrierInfo) && GetCarrierLocInfo(carrierInfo.toString("CurrLoc", 0), locInfo) && locInfo.toString("Type", 0).equals("VEHICLEPORT"))
					trCmdInfo.put("Vehicle", locInfo.toString("Owner", 0));
				else
					trCmdInfo.put("Vehicle", "");
				RegisterTrCmd(trCmdInfo);

				carrierInfo.put("SourceLoc", strCarrierLocID);
				carrierInfo.put("DestLoc", strDest);
				carrierInfo.put("PrevLoc", strCarrierLocID);
				carrierInfo.put("NextLoc", strDest);
				UpdateCarrierInfo(carrierInfo);
			}
		}

		// 2005.08.08 Sync All 요청에 대한 종료
		if (m_bOnSyncAll) {
			m_bOnSyncAll = false;
		}
	}

	boolean IsLastCmd(String strMicroTrCmdID, String strCarrierID) {
		boolean bRet = true;
		String strSql = "SELECT * FROM MicroTrCmd WHERE CmdIndex=" + String.valueOf(GetCmdIndex(strMicroTrCmdID) + 1) + " AND CarrierID='" + strCarrierID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = false;
			}
		} catch (SQLException e) {
			String strLog = "IsLastCmd - SQLException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	void GetEnhancedCarrierInfo() {
		MyHashtable msg = new MyHashtable();
		msg.put("MessageName", "GetEnhancedCarrierInfo");
		msg.put("TSC", m_strTSCID);

		semIF.CallProc(msg);
	}

	/**
	 * OCS로부터의 Carrier 정보 동기화 보고 처리
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void RepEnhancedCarrierInfo(MyHashtable msg) {
		int nCarrierCnt = msg.toInt("CarrierQty", 0);

		MyHashtable repMsg = new MyHashtable();
		MyHashtable locInfo = new MyHashtable();
		int i = 0;
		String strCarrierID = "";
		String strCurrLoc = "";
		String strStatus = "";

		// DB상에 Install안된 Carrier가 Vehicle Port에 있는 것으로 보고된 경우 Install
		for (i = 0; i < nCarrierCnt; i++) {
			strCarrierID = msg.toString("CarrierID", i);
			strCurrLoc = msg.toString("CarrierLocID", i);

			MyHashtable carrierInfo = new MyHashtable();
			if (GetCarrierInfo(strCarrierID, carrierInfo)) {
				if (!carrierInfo.toString("Status", 0).equals("CarrierInstalled") && GetCarrierLocInfo(strCurrLoc, locInfo) && locInfo.toString("Type", 0).equals("VEHICLEPORT")) {
					repMsg.clear();
					repMsg.put("CarrierID", strCarrierID);
					repMsg.put("CarrierLocID", strCurrLoc);
					Proc_CarrierInstalled(repMsg);
				}
			}
		}

		// DB상에 Install된 Carrier가 보고되지 않은 경우
		String strSql = "SELECT CarrierID, CurrLoc, Status FROM Carrier WHERE Status= 'CarrierInstalled'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if (rs != null) {
				while (rs.next()) {
					strCarrierID = rs.getString("CarrierID");
					strCurrLoc = rs.getString("CurrLoc");
					strStatus = rs.getString("Status");
					for (i = 0; i < nCarrierCnt; i++) {
						if (msg.toString("CarrierID", i).equals(strCarrierID)) {
							break;
						}
					}
					if (i == nCarrierCnt) {
						repMsg.clear();
						repMsg.put("CarrierID", strCarrierID);
						repMsg.put("CarrierLocID", strCurrLoc);
						Proc_CarrierRemoved(repMsg);

						HostReport_PRE(strCarrierID, strCurrLoc);
						RegisterDeletedCarrier(strCarrierID);
					}
				}
			}
		} catch (SQLException e) {
			String strLog = "SyncCarrierList() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		for (i = 0; i < nCarrierCnt; i++) {
			strCarrierID = msg.toString("CarrierID", i);
			strCurrLoc = msg.toString("CarrierLocID", i);
			MyHashtable carrierInfo = new MyHashtable();
			// DB상에 Carrier의 CurrLoc과 보고받은 Carrier의 CarrierLoc의 Owner가 다른 경우
			if (GetCarrierInfo(strCarrierID, carrierInfo)) {
				if (!GetOwnerDevice(carrierInfo.toString("CurrLoc", 0)).equals(GetOwnerDevice(strCurrLoc))) {
					// DUP처리
					MakeDupCarrier(strCarrierID, strCurrLoc);
				}
			}
			// DB상에 없는 Carrier에 대해서 보고 받은 경우
			else {
				RemoveDeletedCarrier(strCarrierID);
				// Carrier 등록
				carrierInfo.put("CarrierID", strCarrierID);
				carrierInfo.put("CurrLoc", strCurrLoc);
				if (GetCarrierLocInfo(strCurrLoc, locInfo) && locInfo.toString("Type", 0).equals("VEHICLEPORT")) {
					carrierInfo.put("Status", "CarrierInstalled");
					RegisterNewCarrier(carrierInfo);
					UpdateCarrierTimeoutStatus(strCarrierID, "IN_TRANSIT");
				} else {
					carrierInfo.put("Status", "CarrierRemoved");
					RegisterNewCarrier(carrierInfo);
				}
			}
		}

		if (m_bSyncCarrierInfo == false)
			m_bSyncCarrierInfo = true;

		// 2005.08.08 Sync All 요청이 걸려 있을 경우 Enhanced Tr. Cmd 요청함.
		if (m_bOnSyncAll) {
			GetEnhancedTrCmd();
		}
	}

	void GetVehicleInfo() {
		MyHashtable msg = new MyHashtable();
		msg.put("MessageName", "GetVehicleInfo");
		msg.put("TSC", m_strTSCID);

		semIF.CallProc(msg);
	}

	void RepVehicleInfo(MyHashtable msg) {
		int nRTCD = msg.toInt("ReturnCode", 0);
		if (nRTCD == 0) {
		}
	}

	void GetEnhancedAlarm() {
		MyHashtable msg = new MyHashtable();
		msg.put("MessageName", "GetEnhancedAlarm");
		msg.put("TSC", m_strTSCID);

		semIF.CallProc(msg);
	}

	void RepEnhancedAlarm(MyHashtable msg) {
		//
	}

	/**
	 * Alarm Report 수신 처리
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void RepAlarm(MyHashtable msg) {
		String strStatus = msg.toString("AlarmStatus", 0);
		int nAlarmID = msg.toInt("AlarmID", 0);
		String strVehicleID = msg.toString("VehicleID", 0);
		String strAlarmEventType = msg.toString("AlarmEventType", 0);
		String strMicroTrCmdID = "";
		String strErrorID = "";
		String strAlarmText = "";
		String strHostAlarmStatus = "";
		if (strStatus.equals("AlarmSet")) {
			strMicroTrCmdID = msg.toString("MicroTrCmdID", 0);
			strErrorID = msg.toString("ErrorID", 0);
			strAlarmText = msg.toString("AlarmText", 0);
			strHostAlarmStatus = "ALARMS";
		} else {
			strHostAlarmStatus = "NO_ALARMS";
		}

		if (strStatus.equals("AlarmSet")) {
			// Device Alarm 등록
			if (!strVehicleID.equals("")) {
				RegisterAlarm("Vehicle", strVehicleID, nAlarmID, strErrorID, strAlarmText, strAlarmEventType, strMicroTrCmdID);
			} else {
				RegisterAlarm("TSC", m_strTSCID, nAlarmID, strErrorID, strAlarmText, strAlarmEventType, strMicroTrCmdID);
			}
		} else {
			if (!strVehicleID.equals("")) {
				RemoveAlarm("Vehicle", strVehicleID, nAlarmID);
			} else {
				RemoveAlarm("TSC", m_strTSCID, nAlarmID);
			}
		}

		// HostReport : DAE(Device Alarm Event)
		HostReport_DAE(strVehicleID, strStatus, nAlarmID, strAlarmText);
		return;
	}

	/**
	 * Alarm 발생 내용을 Alarm Table 및 Unit(Vehicle/Device/CarrierLoc) Table의
	 * AlarmList 및 AlarmSetTime에 반영한다.
	 * 
	 * @param strUnitType
	 *            String
	 * @param strUnitID
	 *            String
	 * @param nAlarmID
	 *            int
	 * @param strErrorID
	 *            String
	 * @param strAlarmText
	 *            String
	 * @param strAlarmEventType
	 *            String
	 * @param strMicroTrCmdID
	 *            String
	 */
	void RegisterAlarm(String strUnitType, String strUnitID, int nAlarmID, String strErrorID, String strAlarmText, String strAlarmEventType, String strMicroTrCmdID) {
		if (IsAlreadyRegistered(m_strTSCID, strUnitID, nAlarmID))
			return;

		// Alarm Table에 등록
		String strSql = "INSERT INTO Alarm (AlarmID, AlarmText, AlarmSetTime, TSC, ErrorID, UnitID, AlarmEventType, MicroTrCmdID)";
		strSql += " VALUES (" + String.valueOf(nAlarmID) + ", '";
		strSql += strAlarmText + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '";
		strSql += m_strTSCID + "', '";
		strSql += strErrorID + "', '";
		strSql += strUnitID + "', '";
		strSql += strAlarmEventType + "', '";
		strSql += strMicroTrCmdID + "')";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RegisterAlarm - SQLException: " + e.getMessage();
			WriteLog(strLog);
		}

		// 2006.02.10 TSC에 대한 Alarm은 Unit Table에 등록되지 않도록 하기 위해서 조건체크
		if ((strUnitType.equals("Device") == false) && (strUnitType.equals("Vehicle") == false) && (strUnitType.equals("CarrierLoc") == false)) {
			return;
		}

		// Unit(Device/Vehicle/CarrierLoc) Table에 등록
		CMessage alarmList = new CMessage();
		boolean bOnAlarm = false;
		strSql = "SELECT AlarmList FROM " + strUnitType + " WHERE " + strUnitType + "ID='" + strUnitID + "' AND (AlarmList is not null)";
		ResultSet rs = null;

		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bOnAlarm = true;
				alarmList.SetMessage(rs.getString("AlarmList"));
			}
		} catch (SQLException e) {
			String strLog = "RegisterAlarm - SQLException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		int nAlarmCnt = 0;
		if (bOnAlarm) {
			MsgVector value = new MsgVector();
			alarmList.GetMessageItem("AlarmCnt", value, 0, false);
			nAlarmCnt = value.toInt(0);
		}
		nAlarmCnt = nAlarmCnt + 1;
		alarmList.SetMessageItem("AlarmCnt", nAlarmCnt, false);
		alarmList.SetMessageItem("AlarmID", nAlarmID, true);
		String strAlarmList = alarmList.ToMessage();
		strSql = "UPDATE " + strUnitType + " SET AlarmList='" + strAlarmList + "'";
		if (bOnAlarm == false) {
			strSql += ", AlarmSetTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		}
		strSql += " WHERE " + strUnitType + "ID='" + strUnitID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RegisterAlarm - SQLException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	boolean IsAlreadyRegistered(String strTSCID, String strUnitID, int nAlarmID) {
		boolean bRet = false;
		String strSql = "SELECT * FROM Alarm WHERE TSC='" + m_strTSCID + "' AND UnitID='" + strUnitID + "' AND AlarmID=" + String.valueOf(nAlarmID);
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "IsAlreadyRegistered - SQLException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	/**
	 * 시작시점에서 모든 Alarm 정보 제거
	 */
	void RemoveAllAlarm() {
		// Alarm table에서 Alarm 정보 제거
		String strSql = "DELETE FROM Alarm WHERE TSC='" + m_strTSCID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e1) {
			String strLog = "RemoveAllAlarm() <DELETE FROM Alarm> - SQLException : " + e1.getMessage();
			WriteLog(strLog);
		}

		// Vehicle table에서 Alarm 정보 제거
		strSql = "UPDATE Vehicle SET AlarmList='', AlarmSetTime='' WHERE TSC='" + m_strTSCID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e1) {
			String strLog = "RemoveAllAlarm() <UPDATE Vehicle SET AlarmList=> - SQLException : " + e1.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * Alarm 해제 시 Alarm Table 및 Unit Table에서 Alarm 정보 제거
	 * 
	 * @param strUnitType
	 *            String
	 * @param strUnitID
	 *            String
	 * @param nAlarmID
	 *            int
	 */
	void RemoveAlarm(String strUnitType, String strUnitID, int nAlarmID) {
		// Alarm Table에서 해당 Alarm 제거
		String strSql = "";
		if (!strUnitType.equals("Vehicle")) {
			strSql = "DELETE FROM Alarm WHERE UnitID='" + strUnitID + "' AND AlarmID=" + String.valueOf(nAlarmID);
		} else {
			strSql = "DELETE FROM Alarm WHERE UnitID='" + strUnitID + "'";
		}

		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RemoveAlarm - SQLException: " + e.getMessage();
			WriteLog(strLog);
		}

		// Unit Table에서 AlarmList 갱신
		boolean bOnAlarm = false;
		CMessage alarmList = new CMessage();
		strSql = "SELECT AlarmList FROM " + strUnitType + " WHERE " + strUnitType + "ID='" + strUnitID + "' AND (AlarmList is not null)";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bOnAlarm = true;
				alarmList.SetMessage(rs.getString("AlarmList"));
			}
		} catch (SQLException e) {
			String strLog = "RemoveAlarm - SQLException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		if (bOnAlarm) {
			MsgVector value = new MsgVector();
			alarmList.GetMessageItem("AlarmCnt", value, 0, false);
			int nAlarmCnt = value.toInt(0);

			Vector vtAlarmIDList = new Vector();
			int i = 0;
			for (i = 0; i < nAlarmCnt; i++) {
				value.clear();
				alarmList.GetMessageItem("AlarmID", value, i, false);
				int nRegisteredAlarmID = value.toInt(0);
				if (nRegisteredAlarmID != nAlarmID) {
					vtAlarmIDList.add(String.valueOf(nRegisteredAlarmID));
				}
			}

			String strAlarmList = "";
			alarmList.Reset();
			nAlarmCnt = vtAlarmIDList.size();
			if (nAlarmCnt > 0) {
				for (i = 0; i < nAlarmCnt; i++) {
					alarmList.SetMessageItem("AlarmID", Integer.parseInt((String) vtAlarmIDList.get(i)), true);
				}
				alarmList.SetMessageItem("AlarmCnt", nAlarmCnt, false);
				strAlarmList = alarmList.ToMessage();
			}

			strSql = "UPDATE " + strUnitType + " SET AlarmList='" + strAlarmList + "'";
			if (nAlarmCnt == 0) {
				strSql += ", AlarmSetTime=''";
			}
			strSql += " WHERE " + strUnitType + "ID='" + strUnitID + "'";
			try {
				m_dbFrame.ExecSQL(strSql);
			} catch (SQLException e) {
				String strLog = "RemoveAlarm - SQLException: " + e.getMessage();
				WriteLog(strLog);
			}
		}

		// Suspended MicroTrCmd를 Ready 처리함.
		strSql = "UPDATE MicroTrCmd SET Status='READY' WHERE Status='Suspended' AND Vehicle='" + strUnitID + "'";

		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RemoveAlarm - SQLException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	void UpdateTrCmdPrevActiveStatus(String strMicroTrCmdID) {
		MyHashtable trCmdInfo = new MyHashtable();
		if (!GetMicroTCInfo(strMicroTrCmdID, trCmdInfo)) {
			return;
		}

		String strPrevActiveStatus = trCmdInfo.toString("Status", 0);
		String strSql = "UPDATE MicroTrCmd SET PrevActiveStatus='" + strPrevActiveStatus + "' WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateTrCmdPrevActiveStatus - SQLException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * Carrier 상태 보고 수신 처리
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void RepCarrierStatus(MyHashtable msg) {
		String strStatus = msg.toString("CarrierStatus", 0);
		if (strStatus.equals("CarrierInstalled")) {
			Proc_CarrierInstalled(msg);
		} else if (strStatus.equals("CarrierRemoved")) {
			Proc_CarrierRemoved(msg);
		}
	}

	/**
	 * CarrierWaitIn 보고 처리
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void Proc_CarrierInstalled(MyHashtable msg) {
		String strCarrierID = msg.toString("CarrierID", 0);

		// Carrier 정보 얻기
		MyHashtable carrierInfo = new MyHashtable();
		boolean bValidCarrier = GetCarrierInfo(strCarrierID, carrierInfo);

		// Carrier 유효성 평가
		if (bValidCarrier == false) {
			return;
		}

		// Carrier Status 유효성 평가
		String strPrevStatus = carrierInfo.toString("Status", 0);
		if (strPrevStatus.equals("CarrierInstalled")) {
			return;
		}

		// TimeoutStatus 갱신
		UpdateCarrierTimeoutStatus(strCarrierID, "IN_TRANSIT");

		// 기존 carrier loc에 carrier 정보삭제 보고
		String strCarrierLoc = carrierInfo.toString("CurrLoc", 0);
		HostReport_PSE(strCarrierLoc, "EMPTY", strCarrierID);

		// 신규 carrier loc에 carrier 정보등록 보고
		strCarrierLoc = msg.toString("CarrierLocID", 0);
		HostReport_PSE(strCarrierLoc, "OCCUPIED", strCarrierID);

		// Carrier 정보 갱신
		carrierInfo.put("CurrLoc", strCarrierLoc);
		carrierInfo.put("Status", "CarrierInstalled");
		UpdateCarrierInfo(carrierInfo);

		// HostReport :PLCE
		HostReport_PLCE(strCarrierID);
	}

	/**
	 * CarrierRemoved Report 처리
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void Proc_CarrierRemoved(MyHashtable msg) {
		String strCarrierID = msg.toString("CarrierID", 0);
		String strCarrierLoc = msg.toString("CarrierLocID", 0);

		// Carrier 정보 얻기
		MyHashtable carrierInfo = new MyHashtable();
		boolean bValidCarrier = GetCarrierInfo(strCarrierID, carrierInfo);

		// Carrier 유효성 평가
		if (bValidCarrier == false) {
			return;
		}
		if (!carrierInfo.toString("CurrLoc", 0).equals(strCarrierLoc)) {
			return;
		}

		// 2005.10.18 Dest가 EQPort인 경우에 CarrierRemoved, STKPort인 경우에는 CarrierWaitIn으로 저장
		// Carrier Status 유효성 평가
		String strPrevStatus = carrierInfo.toString("Status", 0);
		String strDest = carrierInfo.toString("DestLoc", 0);
		if (GetCarrierLocType(strDest).equals("EQPORT")) {
			if (strPrevStatus.equals("CarrierRemoved")) {
				return;
			}
			// TimeoutStatus 갱신
			UpdateCarrierTimeoutStatus(strCarrierID, "");

			// 기존 carrier loc에 carrier 정보삭제 보고
			strCarrierLoc = carrierInfo.toString("CurrLoc", 0);
			HostReport_PSE(strCarrierLoc, "EMPTY", strCarrierID);

			// Carrier 정보 갱신
			carrierInfo.put("Status", "CarrierRemoved");
			UpdateCarrierInfo(carrierInfo);
		} else {
			if (strPrevStatus.equals("CarrierWaitIn")) {
				return;
			}
			// TimeoutStatus 갱신
			UpdateCarrierTimeoutStatus(strCarrierID, "");

			// 기존 carrier loc에 carrier 정보삭제 보고
			strCarrierLoc = carrierInfo.toString("CurrLoc", 0);
			HostReport_PSE(strCarrierLoc, "EMPTY", strCarrierID);

			// Carrier 정보 갱신
			carrierInfo.put("Status", "CarrierWaitIn");
			UpdateCarrierInfo(carrierInfo);
		}
	}

	/**
	 * 반송명령 보고 수신 처리
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void RepMicroTCStatus(MyHashtable msg) {
		String strMicroTrCmdID = msg.toString("MicroTrCmdID", 0);
		String strStatus = msg.toString("MicroTCStatus", 0);
		String strCarrierID = "";
		if ((strStatus.equals("CANCELFAIL") == false) && (strStatus.equals("ABORTFAIL") == false)) {
			strCarrierID = msg.toString("CarrierID", 0);
		}

		if (strMicroTrCmdID.equals("") || strCarrierID.equals("") || strStatus.equals("")) {
			return;
		}

		// 이미 동일 상태이면 상태보고 무시
		MyHashtable trCmdInfo = new MyHashtable();
		if (!GetMicroTCInfo(strMicroTrCmdID, trCmdInfo)) {
			return;
		}
		String strCurrStatus = trCmdInfo.toString("Status", 0);
		if (strCurrStatus.equals(strStatus)) {
			return;
		}

		// 존재하지 않는 Carrier에 대한 반송명령 상태보고는 무시
		MyHashtable carrierInfo = new MyHashtable();
		if (!GetCarrierInfo(strCarrierID, carrierInfo)) {
			return;
		}

		String strPrevStatus = strCurrStatus;
		String strPrevActiveStatus = trCmdInfo.toString("PrevActiveStatus", 0);
		if (strStatus.equals("CANCELQUEUED") || strStatus.equals("ABORTQUEUED")) {
			if (strPrevStatus.equals("TransferCancelInitiated") || strPrevStatus.equals("TransferAbortInitiated")) {
				strStatus = strPrevStatus;
			}
			// 2006.02.10 SendFlag 초기화
			m_bSendS2F41 = false;
		} else if (strStatus.equals("QUEUED")) {
			if (strPrevStatus.equals("TransferInitiated")) {
				strStatus = strPrevStatus;
			}
			// 2005.07.14 Abort한 이후의 생성된 TrCmd는 Transferring이후에 QUEUED보고됨
			else if (strPrevStatus.equals("Transferring")) {
				strStatus = strPrevStatus;
			}
			// 2006.02.10 SendFlag 초기화
			m_bSendS2F49 = false;
		} else if (strStatus.equals("FAIL")) {
			if (strPrevStatus.equals("IFQUEUED") == false) {
				strStatus = strPrevStatus;
			}
			// 2006.03.28 Fail로 응답수신시 SendFlag 초기화
			m_bSendS2F49 = false;
		} else if (strStatus.equals("TransferAbortFailed")) {
			if ((strPrevStatus.equals("TransferAbortInitiated") == false) && (strPrevStatus.equals(strPrevActiveStatus) == false)) {
				strStatus = strPrevStatus;
			} else {
				strStatus = strPrevActiveStatus;
			}
		} else if (strStatus.equals("TransferCancelFailed")) {
			if ((strPrevStatus.equals("TransferCancelInitiated") == false) && (strPrevStatus.equals(strPrevActiveStatus) == false)) {
				strStatus = strPrevStatus;
			} else {
				strStatus = strPrevActiveStatus;
				SendAbort(msg);
			}
		} else if (strStatus.equals("ABORTFAIL") || strStatus.equals("CANCELFAIL")) {
			strStatus = strPrevActiveStatus;
			// 2006.03.28 Fail로 응답수신시 SendFlag 초기화
			m_bSendS2F41 = false;
		}

		String strMacroTrCmdID = GetMacroTrCmdID(strMicroTrCmdID);

		// 반송명령상태 갱신
		UpdateTrCmdStatus(strMicroTrCmdID, strMacroTrCmdID, strStatus);

		if (strStatus.equals("QUEUED")) {
			// TrQueued Time 갱신
			UpdateTrCmdStatusTime(strMicroTrCmdID, "TrQueued");

			// TimeoutStatus 갱신
			UpdateCarrierTimeoutStatus(strCarrierID, "");
		} else if (strStatus.equals("TransferInitiated")) {
			// TrInitiatedTime 갱신
			UpdateTrCmdStatusTime(strMicroTrCmdID, "TrInitiated");
		} else if (strStatus.equals("Transferring")) {
			if (trCmdInfo.toString("Vehicle", 0).equals("")) {
				UpdateTrCmdVehicle(strMicroTrCmdID, GetOwnerDevice(carrierInfo.toString("CurrLoc", 0)));
			}
			// TrStartTime 갱신
			UpdateTrCmdStatusTime(strMicroTrCmdID, "TrStart");
		} else if (strStatus.equals("TransferCompleted")) {
			int nResultCode = 0;
			nResultCode = msg.toInt("ResultCode", 0);
			String strCarrierLocID = msg.toString("CarrierLocID", 0);
			String strLastLoc = "";

			if ((strCarrierLocID.equals("") == false) && (nResultCode == 0)) {
				strLastLoc = strCarrierLocID;
			} else {
				strLastLoc = trCmdInfo.toString("DestLoc", 0);
			}

			if (nResultCode != 1) {
				// 2005.10.10 VehicleDepositCompleted에서 LastLoc의 정보를 갱신하지 않은 경우에만 처리
				if (strCarrierLocID.equals(trCmdInfo.toString("LastLoc", 0))) {
					return;
				}

				// TrCompletionTime 갱신
				UpdateTrCmdStatusTime(strMicroTrCmdID, "TrCompleted");
				// LastLoc 갱신
				UpdateTrCmdLastLoc(strMicroTrCmdID, strMacroTrCmdID, strLastLoc);

				MyHashtable nextTrCmdInfo = new MyHashtable();
				if (GetNextTrCmdInfo(trCmdInfo, nextTrCmdInfo) == false) {
					// MacroTrCmd Status 갱신
					UpdateMacroTrCmdStatus(strMacroTrCmdID, "COMPLETED");

					// Host Report : PMR_COM(Pod Move Request Completion)
					HostReport_PMR_COM(strCarrierID);

					// 2005.07.05 VehicleDepositCompleted 시점에서 변경
					MyHashtable locInfo = new MyHashtable();
					if (GetCarrierLocInfo(strCarrierLocID, locInfo) && locInfo.toString("Type", 0).equals("EQPORT")) {
						HostReport_PRE(strCarrierID, strCarrierLocID);
						RegisterDeletedCarrier(strCarrierID);
					}
				}
			} else {
				MyHashtable locInfo = new MyHashtable();
				GetCarrierLocInfo(strLastLoc, locInfo);
				String strDevice = locInfo.toString("Owner", 0);
				String strAltLoc = "";
				if (locInfo.toString("Type", 0).equals("STOCKERPORT")) {
					// 대체 Port 설정
					if ((strAltLoc = GetAltSTKPort(strLastLoc)) != null && !strAltLoc.equals("")) {
						ReqRoutePortChange(trCmdInfo, strAltLoc);
					}
					// 대체 STK로 반송명령 설정
					else if ((strAltLoc = GetAltSTK(strLastLoc)) != null && !strAltLoc.equals("")) {
						ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
					}
					// TSC의 대체 STK로 반송명령 설정
					else if ((strAltLoc = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltLoc.equals("")) {
						ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
					} else {
						String strLog = "AltSTK for " + strLastLoc + " Not Found! (RepMicroTCStatus)";
						WriteLog(strLog);
					}
				} else //if (locInfo.toString("Type", 0).equals("EQPORT"))
				{
					// 대체 STK로 반송명령 설정
					if ((strAltLoc = GetAltSTK(strLastLoc)) != null && !strAltLoc.equals("")) {
						ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
					}
					// TSC의 대체 STK로 반송명령 설정
					else if ((strAltLoc = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltLoc.equals("")) {
						ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
					} else {
						String strLog = "AltSTK for " + strLastLoc + " Not Found! (RepMicroTCStatus)";
						WriteLog(strLog);
					}
				}
			}
		} else if (strStatus.equals("TransferAbortCompleted")) {
			String strCarrierLocID = msg.toString("CarrierLocID", 0);
			String strLastLoc = trCmdInfo.toString("DestLoc", 0);

			MyHashtable locInfo = new MyHashtable();
			GetCarrierLocInfo(strLastLoc, locInfo);
			String strDevice = locInfo.toString("Owner", 0);
			String strAltLoc = "";

			// Port나 Device에 에러가 발생한 경우면 Device의 대체STK로 반송(또는 Port)
			//      if (IsPortError(strLastLoc) ||
			//          (GetCarrierLocType(strLastLoc).equals("STOCKERPORT") &&
			//          !IsValidStocker(GetOwnerDevice(strLastLoc)))
			// 2005.10.06 Abort이후 이므로 원래Dest의 상태체크하지 않고 대체반송시도...
			if (IsPortError(strLastLoc) || GetCarrierLocType(strLastLoc).equals("STOCKERPORT")) {
				if (locInfo.toString("Type", 0).equals("STOCKERPORT")) {
					// 대체 Port 설정
					if ((strAltLoc = GetAltSTKPort(strLastLoc)) != null && !strAltLoc.equals("")) {
						ReqRoutePortChange(trCmdInfo, strAltLoc);
					}
					// 대체 STK로 반송명령 설정
					else if ((strAltLoc = GetAltSTK(strLastLoc)) != null && !strAltLoc.equals("")) {
						ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
					}
					// TSC의 대체 STK로 반송명령 설정
					else if ((strAltLoc = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltLoc.equals("")) {
						ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
					} else {
						String strLog = "AltSTK for " + strLastLoc + " Not Found! (RepMicroTCStatus)";
						WriteLog(strLog);
					}
				} else //if (locInfo.toString("Type", 0).equals("EQPORT"))
				{
					// 대체 STK로 반송명령 설정
					if ((strAltLoc = GetAltSTK(strLastLoc)) != null && !strAltLoc.equals("")) {
						ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
					}
					// TSC의 대체 STK로 반송명령 설정
					else if ((strAltLoc = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltLoc.equals("")) {
						ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
					} else {
						String strLog = "AltSTK for " + strLastLoc + " Not Found! (RepMicroTCStatus)";
						WriteLog(strLog);
					}
				}
			}
			// 그외의 경우면 TSC의 대체STK로 반송
			else {
				// HostReport : PMCE
				HostReport_PMCE(strCarrierID);
				// TSC의 대체 STK로 반송명령 설정
				if ((strAltLoc = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltLoc.equals("")) {
					ReqRegisterTrCmd(strCarrierID, strCarrierLocID, strAltLoc);
				} else {
					String strLog = "AltSTK for " + m_strTSCID + " Not Found! (RepMicroTCStatus)";
					WriteLog(strLog);
				}
			}
		} else if (strStatus.equals("TransferCancelCompleted")) {
			String strLastLoc = trCmdInfo.toString("SourceLoc", 0);

			// LastLoc 갱신
			UpdateTrCmdLastLoc(strMicroTrCmdID, strMacroTrCmdID, strLastLoc);

			// HostReport : PMCE
			HostReport_PMCE(strCarrierID);

			MyHashtable locInfo = new MyHashtable();
			String strCurrLoc = carrierInfo.toString("CurrLoc", 0);
			if (GetCarrierLocInfo(strCurrLoc, locInfo) && locInfo.toString("Type", 0).equals("VEHICLEPORT")) {
				HostReport_PRE(strCarrierID, strCurrLoc);
				RegisterDeletedCarrier(strCarrierID);
			}
		}
	}

	boolean IsAlreadyRegisteredUserOperation(String strMsg) {
		boolean bRet = false;
		String strSql = "SELECT * FROM UserOperation WHERE MsgString='" + strMsg + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "IsAlreadyRegisteredUserOperation - SQLException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	void ReqRegisterTrCmd(String strCarrierID, String strCarrierLocID, String strManualOutPort) {
		CMessage RegMsg = new CMessage();
		RegMsg.SetMessageName("ReqRegisterTrCmd");
		RegMsg.SetMessageItem("CarrierID", strCarrierID, false);
		RegMsg.SetMessageItem("SourceLoc", strCarrierLocID, false);
		RegMsg.SetMessageItem("DestLoc", strManualOutPort, false);

		String strRegMsg = RegMsg.ToMessage();
		if (IsAlreadyRegisteredUserOperation(strRegMsg))
			return;

		String strSql = "INSERT INTO UserOperation (MsgSender, MsgReceiver, MsgString, CommandTime)";
		strSql += " VALUES ('" + m_strTSCID + "', 'HostIF', '" + RegMsg.ToMessage() + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'))";
		try {
			m_dbFrame.ExecSQL(strSql);
			String strLog = "ReqRegisterTrCmd - " + strSql;
			WriteLog(strLog);
		} catch (SQLException e) {
			String strLog = "ReqRegisterTrCmd - SQLException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	String GetManualOutputPort(String strStockerID) {
		String strManualPort = "";
		String strSql = "SELECT CarrierLocID FROM CarrierLoc WHERE Owner='" + strStockerID + "' AND InOutMode='MANUAL_OUT' AND CarrierLocID LIKE '%.LP'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strManualPort = rs.getString("CarrierLocID");
			}
		} catch (SQLException e) {
			String strLog = "GetManualOutputPort - SQLException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strManualPort;
	}

	/**
	 * Vehicle(Crane) 상태 보고 처리
	 * 
	 * @param msg
	 *            MyHashtable
	 */
	void RepVehicleStatus(MyHashtable msg) {
		String strMicroTrCmdID = msg.toString("MicroTrCmdID", 0);
		String strVehicleID = msg.toString("VehicleID", 0);
		String strStatus = msg.toString("VehicleStatus", 0);

		UpdateVehicleStatus(strVehicleID, strStatus);
		if (strStatus.equals("VehicleArrived")) {
			String strCarrierLocID = msg.toString("CarrierLocID", 0);
			UpdateVehicleNode(strVehicleID, GetNode(strCarrierLocID));
		} else if (strStatus.equals("VehicleAcquireStarted")) {
		} else if (strStatus.equals("VehicleAcquireCompleted")) {
		} else if (strStatus.equals("VehicleAssigned")) {
			UpdateTrCmdVehicle(strMicroTrCmdID, strVehicleID);
		} else if (strStatus.equals("VehicleDeparted")) {
		} else if (strStatus.equals("VehicleDepositStarted")) {
		} else if (strStatus.equals("VehicleDepositCompleted")) {
			String strCarrierID = msg.toString("CarrierID", 0);
			String strCarrierLocID = msg.toString("CarrierLocID", 0);

			// 2005.09.05 이미 STK에 Stored된 Carrier에 대해 OCS의 Report지연으로 인해 수신받은 경우에는 미처리
			MyHashtable carrierInfo = new MyHashtable();
			MyHashtable locInfo = new MyHashtable();
			if (GetCarrierInfo(strCarrierID, carrierInfo) && GetCarrierLocInfo(carrierInfo.toString("CurrLoc", 0), locInfo) && locInfo.toString("Type", 0).equals("VEHICLEPORT")) {
				// 신규 carrier loc에 carrier 정보등록 보고
				HostReport_PSE(strCarrierLocID, "OCCUPIED", strCarrierID);

				// Carrier 정보 갱신
				carrierInfo = new MyHashtable();
				if (GetCarrierInfo(strCarrierID, carrierInfo)) {
					carrierInfo.put("CurrLoc", strCarrierLocID);
					UpdateCarrierInfo(carrierInfo);

					// HostReport :PLCE
					HostReport_PLCE(strCarrierID);

					// 2005.07.05 TransferCompleted 시점으로 변경
					/*
					 * MyHashtable locInfo = new MyHashtable(); if
					 * (GetCarrierLocInfo(strCarrierLocID, locInfo) &&
					 * locInfo.toString("Type", 0).equals("EQPORT")) {
					 * HostReport_PRE(strCarrierID, strCarrierLocID);
					 * RegisterDeletedCarrier(strCarrierID); }
					 */

					// 2005.10.10 TransferCompleted 시점에서 처리하던 부분을 TransferCompleted 미수신 안전장치 차원에서 이동
					MyHashtable trCmdInfo = new MyHashtable();
					if (!GetMicroTCInfo(strMicroTrCmdID, trCmdInfo)) {
						return;
					}

					// TrCompletionTime 갱신
					UpdateTrCmdStatusTime(strMicroTrCmdID, "TrCompleted");
					// LastLoc 갱신
					String strMacroTrCmdID = trCmdInfo.toString("MacroTrCmdID", 0);
					UpdateTrCmdLastLoc(strMicroTrCmdID, strMacroTrCmdID, strCarrierLocID);

					MyHashtable nextTrCmdInfo = new MyHashtable();
					if (!GetNextTrCmdInfo(trCmdInfo, nextTrCmdInfo)) {
						// MacroTrCmd Status 갱신
						UpdateMacroTrCmdStatus(strMacroTrCmdID, "COMPLETED");

						// Host Report : PMR_COM(Pod Move Request Completion)
						HostReport_PMR_COM(strCarrierID);

						HostReport_PRE(strCarrierID, strCarrierLocID);
						RegisterDeletedCarrier(strCarrierID);
					}
				}
			}
		} else if (strStatus.equals("VehicleInstalled")) {
		} else if (strStatus.equals("VehicleRemoved")) {
		} else if (strStatus.equals("VehicleUnassigned")) {
		}
	}

	/**
	 * 특정 MicroTrCmd에 대한 Vehicle ID 갱신
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @param strVehicleID
	 *            String
	 */
	void UpdateTrCmdVehicle(String strMicroTrCmdID, String strVehicleID) {
		String strSql = "UPDATE MicroTrCmd SET Vehicle='" + strVehicleID + "' WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateTrCmdVehicle() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 특정 Vehicle 대한 CurrentNode 갱신
	 * 
	 * @param strVehicleID
	 *            String
	 * @param strNode
	 *            String
	 */
	void UpdateVehicleNode(String strVehicleID, String strNode) {
		String strSql = "UPDATE Vehicle SET CurrentNode='" + strNode + "' WHERE VehicleID='" + strVehicleID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateVehicleNode() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 특정 Vehicle에 대한 Status 변경 CraneActive/CraneIdle
	 * 
	 * @param strVehicleID
	 *            String
	 * @param strStatus
	 *            String
	 */
	void UpdateVehicleStatus(String strVehicleID, String strStatus) {
		String strSql = "UPDATE Vehicle SET Status='" + strStatus + "', StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE VehicleID='" + strVehicleID + "'";

		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateVehicleStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	void ResetExpectedReceiptTrCmd(String strMacroTrCmdID, String strSourceLoc) {
		String strSql = "UPDATE MacroTrCmd SET ExpectedReceipt=0, SourceLoc='" + strSourceLoc + "' WHERE MacroTrCmdID='" + strMacroTrCmdID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "ResetExpectedReceiptFlag() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	boolean CheckCarrierDuplicationOnInputPort(MyHashtable carrierInfo, String strCarrierLocID) {
		String strCarrierID = carrierInfo.toString("CarrierID", 0);
		MyHashtable locInfo = new MyHashtable();
		GetCarrierLocInfo(strCarrierLocID, locInfo);
		boolean bInputPort = false;
		if (locInfo.toString("InOutMode", 0).indexOf("_IN") > -1)
			bInputPort = true;

		String strLPPort = GetLPPort(strCarrierLocID, bInputPort);
		String strCurrLoc = carrierInfo.toString("CurrLoc", 0);
		String strStatus = carrierInfo.toString("Status", 0);
		if ((strCurrLoc.equals(strLPPort) == false) && (strStatus.equals("CarrierRemoved") == false)) {
			if (IsCarrierOnTransferring(strCarrierID)) {
				// 현재 Carrier를 DUP로 부여
				String strDupID = "DUP_" + strCarrierID;
				carrierInfo.put("CarrierID", strDupID);
				return false; // 현재 Carrier를 dup 처리
			} else {
				MakeDupCarrier(strCarrierID, strCurrLoc); // 기존 carrier를 dup 처리
			}
		}
		return true;
	}

	void MakeDupCarrier(String strCarrierID, String strCarrierLocID) {
		String strDupID = "DUP_" + strCarrierID;
		String strSql = "UPDATE Carrier SET CarrierID='" + strDupID + "' WHERE CarrierID='" + strCarrierID + "'";
		try {
			m_dbFrame.AddBatch(strSql);
		} catch (SQLException e) {
			String strLog = "MakeDupCarrier() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}

		String strDevice = GetOwnerDevice(strCarrierLocID);

		CMessage HostRep = new CMessage();
		HostRep.SetMessageName("ReqSDOperation");
		HostRep.SetMessageItem("OperationType", "INSTALL", false);
		HostRep.SetMessageItem("CarrierID", strDupID, false);
		HostRep.SetMessageItem("CarrierLocID", strCarrierLocID, false);

		String strRegMsg = HostRep.ToMessage();
		if (IsAlreadyRegisteredUserOperation(strRegMsg))
			return;

		strSql = "INSERT INTO UserOperation (MsgSender, MsgReceiver, MsgString, CommandTime) ";
		strSql += "VALUES ('" + m_strTSCID + "', '" + strDevice + "', '";
		strSql += HostRep.ToMessage() + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'))";
		try {
			m_dbFrame.AddBatch(strSql);
		} catch (SQLException e) {
			String strLog = "MakeDupCarrier() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}

		try {
			m_dbFrame.ExecBatch();
		} catch (SQLException e) {
			String strLog = "MakeDupCarrier() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	String GetOwnerDevice(String strCarrierLocID) {
		String strOwner = "";
		String strSql = "SELECT Owner FROM CarrierLoc WHERE CarrierLocID='" + strCarrierLocID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strOwner = rs.getString("Owner");
			}
		} catch (SQLException e) {
			String strLog = "GetOwnerDevice() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strOwner;
	}

	String GetOwnerDevice2(String strCarrierLocID) {
		String strOwner = "";
		String strSql = "SELECT Owner FROM CarrierLoc WHERE CarrierLocID LIKE '%" + strCarrierLocID + "'";

		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strOwner = rs.getString("Owner");
			}
		} catch (SQLException e) {
			String strLog = "GetOwnerDevice2() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strOwner;
	}

	/**
	 * 특정 Carrier에 대한 예약반송여부 확인 및 처리
	 * 
	 * @param strCarrierID
	 *            String
	 * @return String
	 */
	String GetExpectedReceiptMacroTrCmdID(String strCarrierID) {
		String strMacroTrCmdID = null;
		String strSql = "SELECT * FROM MacroTrCmd WHERE CarrierID='" + strCarrierID + "' AND ExpectedReceipt=1";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strMacroTrCmdID = rs.getString("MacroTrCmdID");
			}
		} catch (SQLException e) {
			String strLog = "CheckExpectedReceiptTrCmd() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strMacroTrCmdID;
	}

	/**
	 * 특정 CarrierLoc에 있는 CarrierID 정보 얻기
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return String
	 */
	String GetCarrierOnCarrierLoc(String strCarrierLocID) {
		String strCarrierID = null;
		String strSql = "SELECT CarrierID FROM Carrier WHERE CurrLoc='" + strCarrierLocID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strCarrierID = rs.getString("CarrierID");
			}
		} catch (SQLException e) {
			String strLog = "GetCarrierOnCarrierLoc() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strCarrierID;
	}

	/**
	 * DeleteCarrier table에 carrier의 정보 등록
	 * 
	 * @param strCarrierID
	 *            String
	 */
	void RegisterDeletedCarrier(String strCarrierID) {
		String strSql = "INSERT INTO DeletedCarrier (CarrierID, DeletedTime)";
		strSql += " VALUES ('" + strCarrierID + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'))";

		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RegisterDeletedCarrier() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * DeleteCarrier table에서 특정 carrier의 정보 제거
	 * 
	 * @param strCarrierID
	 *            String
	 */
	void RemoveDeletedCarrier(String strCarrierID) {
		String strSql = "DELETE FROM DeletedCarrier WHERE CarrierID='" + strCarrierID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RemoveDeletedCarrier() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * Stocker의 Crane Port 정보 얻기
	 * 
	 * @param strStockerID
	 *            String
	 * @return String
	 */
	String GetCranePort(String strStockerID) {
		String strPort = "";
		String strSql = "SELECT CarrierLocID FROM CarrierLoc WHERE (Owner LIKE '%" + strStockerID + "%')" + " AND (Type='VEHICLEPORT')";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strPort = rs.getString("CarrierLocID");
			}
		} catch (SQLException e) {
			String strLog = "GetCranePort() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strPort;
	}

	/**
	 * Carrier에 반송명령이 할당되어 있는지 여부
	 * 
	 * @param strCarrierID
	 *            String
	 * @return boolean
	 */
	boolean IsCarrierOnTransferring(String strCarrierID) {
		boolean bRet = false;
		String strSql = "SELECT MacroTrCmdID FROM MacroTrCmd WHERE CarrierID='" + strCarrierID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "IsCarrierOnTransferring() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	/**
	 * Carrier 정보 갱신
	 * 
	 * @param carrierInfo
	 *            MyHashtable
	 */
	void UpdateCarrierInfo(MyHashtable carrierInfo) {
		// Carrier 갱신 정보
		// Location 정보(CurrLoc, PrevLoc, NextLoc)
		// 상태 정보(Status, StatusChangedTime, TimeoutStatus, TimeoutStartTime)

		if (carrierInfo.toString("Status", 0).equals("")) {
			int nTemp = 0;
		}

		String strSql = "UPDATE Carrier SET CurrLoc='" + carrierInfo.toString("CurrLoc", 0) + "', ";
		strSql += "SourceLoc='" + carrierInfo.toString("SourceLoc", 0) + "', ";
		strSql += "DestLoc='" + carrierInfo.toString("DestLoc", 0) + "', ";
		strSql += "PrevLoc='" + carrierInfo.toString("PrevLoc", 0) + "', ";
		strSql += "NextLoc='" + carrierInfo.toString("NextLoc", 0) + "', ";
		strSql += "Status='" + carrierInfo.toString("Status", 0) + "', ";
		strSql += "StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		if (carrierInfo.toString("TimeoutStatus", 0).equals("") == false) {
			strSql += ", TimeoutStatus='" + carrierInfo.toString("TimeoutStatus", 0) + "', ";
			strSql += "TimeoutStartTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		}
		strSql += " WHERE CarrierID='" + carrierInfo.toString("CarrierID", 0) + "'";

		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateCarrierInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 신규 Carrier 등록
	 * 
	 * @param carrierInfo
	 *            MyHashtable
	 */
	void RegisterNewCarrier(MyHashtable carrierInfo) {
		// Carrier 갱신 정보
		// Location 정보(CurrLoc, PrevLoc, NextLoc)
		// 상태 정보(Status, StatusChangedTime, TimeoutStatus, TimeoutStartTime)

		String strSql = "INSERT INTO Carrier (CarrierID, Status, StatusChangedTime,";
		strSql += " TimeoutStatus, TimeoutStartTime, CurrLoc, PrevLoc, NextLoc, SourceLoc,";
		strSql += " DestLoc, EmptyFlag, AlternateFlag, MoveRequestedFlag, MoveRequestedTime)";
		strSql += " VALUES ('" + carrierInfo.toString("CarrierID", 0) + "', '";
		strSql += carrierInfo.toString("Status", 0) + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '";
		strSql += carrierInfo.toString("TimeoutStatus", 0) + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '";
		strSql += carrierInfo.toString("CurrLoc", 0) + "', '";
		strSql += carrierInfo.toString("PrevLoc", 0) + "', '";
		strSql += carrierInfo.toString("NextLoc", 0) + "', '";
		strSql += carrierInfo.toString("SourceLoc", 0) + "', '";
		strSql += carrierInfo.toString("DestLoc", 0) + "', ";
		strSql += carrierInfo.toInt("EmptyFlag", 0) + ", ";
		strSql += carrierInfo.toInt("AlternateFlag", 0) + ", ";
		strSql += 1 + ", TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'))";

		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RegisterNewCarrier() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 신규 Carrier 등록
	 * 
	 * @param trCmdInfo
	 *            MyHashtable
	 */
	void RegisterTrCmd(MyHashtable trCmdInfo) {
		String strMicroTrCmdID = trCmdInfo.toString("MicroTrCmdID", 0);
		// 2005.10.11 수동으로 생성한 Job같이 TrCmd에 "_"가 없는 경우에 대해 고려
		int nPos = strMicroTrCmdID.lastIndexOf("_");
		String strMacroTrCmdID;
		if (nPos == -1) {
			strMacroTrCmdID = strMicroTrCmdID;
		} else {
			strMacroTrCmdID = strMicroTrCmdID.substring(0, nPos);
		}
		String strCarrierID = trCmdInfo.toString("CarrierID", 0);
		String strCarrierLocID = trCmdInfo.toString("CarrierLocID", 0);
		String strDest = trCmdInfo.toString("Dest", 0);
		String strStatus = trCmdInfo.toString("TransferState", 0);
		String strVehicleID = trCmdInfo.toString("Vehicle", 0);

		String strSql = "INSERT INTO MicroTrCmd (MacroTrCmdID, MicroTrCmdID, CarrierID,";
		strSql += " SourceLoc, DestLoc, Status, StatusChangedTime, Vehicle, TSC)";
		strSql += " VALUES ('" + strMacroTrCmdID + "', '" + strMicroTrCmdID + "', '" + strCarrierID + "', '";
		strSql += strCarrierLocID + "', '" + strDest + "', '";
		strSql += strStatus + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '";
		strSql += strVehicleID + "', '" + m_strTSCID + "')";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RegisterTrCmd() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}

		strSql = "INSERT INTO MacroTrCmd (MacroTrCmdID, CarrierID, SourceLoc, DestLoc, Status, StatusChangedTime, InstallTime)";
		strSql += " VALUES ('" + strMacroTrCmdID + "', '" + strCarrierID + "', '";
		strSql += strCarrierLocID + "', '" + strDest + "', 'TRANSFERRING',";
		strSql += "TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'))";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RegisterTrCmd() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * Host Report : PSE(Port Status Event)
	 * 
	 * @param strLoc
	 *            String
	 * @param strPortStatus
	 *            String
	 * @param strCarrierID
	 *            String
	 */
	void HostReport_PSE(String strLoc, String strPortStatus, String strCarrierID) {
		CMessage HostReport = new CMessage();
		HostReport.SetMessageName("PSE");
		HostReport.SetMessageItem("LocationId", strLoc, false);
		HostReport.SetMessageItem("PortStatus", strPortStatus, false);
		HostReport.SetMessageItem("PodId", strCarrierID, false);
		WriteLog("[PSE] " + HostReport.ToMessage());

		RegisterHostReport(HostReport.ToMessage());
	}

	/**
	 * Host Report : PMR_COM(Pod Move Request Completed)
	 * 
	 * @param strCarrierID
	 *            String
	 */
	void HostReport_PMR_COM(String strCarrierID) {
		CMessage HostMsg = new CMessage();
		HostMsg.SetMessageName("PMR_COM");
		HostMsg.SetMessageItem("PodId", strCarrierID, false);
		WriteLog("[PMR_COM] " + HostMsg.ToMessage());
		RegisterHostReport(HostMsg.ToMessage());
	}

	/**
	 * Host Report : PRE(Pod Removed Event)
	 * 
	 * @param strCarrierID
	 *            String
	 * @param strLocationID
	 *            String
	 */
	void HostReport_PRE(String strCarrierID, String strLocationID) {
		CMessage HostMsg = new CMessage();
		HostMsg.SetMessageName("PRE");
		HostMsg.SetMessageItem("PodId", strCarrierID, false);
		HostMsg.SetMessageItem("LocationId", strLocationID, false);
		WriteLog("[PRE] " + HostMsg.ToMessage());
		RegisterHostReport(HostMsg.ToMessage());
	}

	void HostReport_PDQ(String strCarrierID) {
		CMessage HostMsg = new CMessage();
		HostMsg.SetMessageName("PDQ");
		HostMsg.SetMessageItem("PodId", strCarrierID, false);
		WriteLog("[PDQ] " + HostMsg.ToMessage());
		RegisterHostReport(HostMsg.ToMessage());
	}

	/**
	 * Host Report : PLCE(Pod Location Changed Event)
	 * 
	 * @param strCarrierID
	 *            String
	 */
	void HostReport_PLCE(String strCarrierID) {
		CMessage HostMsg = new CMessage();
		HostMsg.SetMessageName("PLCE");
		HostMsg.SetMessageItem("PodId", strCarrierID, false);
		WriteLog("[PLCE] " + HostMsg.ToMessage());

		RegisterHostReport(HostMsg.ToMessage());
	}

	/**
	 * Host Report : PMCE(Pod Move Cancel Event)
	 * 
	 * @param strCarrierID
	 *            String
	 */
	void HostReport_PMCE(String strCarrierID) {
		CMessage HostMsg = new CMessage();
		HostMsg.SetMessageName("PMCE");
		HostMsg.SetMessageItem("PodId", strCarrierID, false);
		WriteLog("[PMCE] " + HostMsg.ToMessage());
		RegisterHostReport(HostMsg.ToMessage());
	}

	/**
	 * Host Report : SDCC(Storage Device Capacity Change)
	 * 
	 * @param strDeviceID
	 *            String
	 */
	void HostReport_SDCC(String strDeviceID) {
		CMessage HostMsg = new CMessage();
		HostMsg.SetMessageName("SDCC");
		HostMsg.SetMessageItem("DeviceId", strDeviceID, false);
		WriteLog("[SDCC] " + HostMsg.ToMessage());
		RegisterHostReport(HostMsg.ToMessage());
	}

	/**
	 * Host Report : DAE(Device Alarm Event)
	 * 
	 * @param strDeviceID
	 *            String
	 * @param strStatus
	 *            String
	 * @param nAlarmID
	 *            int
	 * @param strAlarmText
	 *            String
	 */
	void HostReport_DAE(String strDeviceID, String strStatus, int nAlarmID, String strAlarmText) {
		CMessage HostMsg = new CMessage();
		HostMsg.SetMessageName("DAE");
		HostMsg.SetMessageItem("DeviceId", strDeviceID, false);
		HostMsg.SetMessageItem("AlarmStatus", strStatus, false);
		HostMsg.SetMessageItem("AlarmId", nAlarmID, false);
		HostMsg.SetMessageItem("AlarmText", strAlarmText, false);
		WriteLog("[DAE] " + HostMsg.ToMessage());
		RegisterHostReport(HostMsg.ToMessage());
	}

	/**
	 * 특정 ID의 MacroTrCmd 상태 변경
	 * 
	 * @param strMacroTrCmdID
	 *            String
	 * @param strStatus
	 *            String
	 */
	void UpdateMacroTrCmdStatus(String strMacroTrCmdID, String strStatus) {
		String strSql = "UPDATE MacroTrCmd SET Status='" + strStatus + "', StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE MacroTrCmdID='" + strMacroTrCmdID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateMacroTrCmdStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 특정 MicroTrCmd에 대해 다음 단계의 MicroTrCmd 정보 얻기
	 * 
	 * @param currTrCmdInfo
	 *            MyHashtable
	 * @param nextTrCmdInfo
	 *            MyHashtable
	 * @return boolean
	 */
	boolean GetNextTrCmdInfo(MyHashtable currTrCmdInfo, MyHashtable nextTrCmdInfo) {
		boolean bRet = false;
		String strSql = "SELECT * FROM MicroTrCmd WHERE MacroTrCmdID='" + currTrCmdInfo.toString("MacroTrCmdID", 0) + "' AND CmdIndex=" + String.valueOf(currTrCmdInfo.toInt("CmdIndex", 0) + 1);
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				nextTrCmdInfo.put("MicroTrCmdID", rs.getString("MicroTrCmdID"));
				nextTrCmdInfo.put("CmdIndex", new Integer(rs.getInt("CmdIndex")));
				nextTrCmdInfo.put("SourceLoc", rs.getString("SourceLoc"));
				nextTrCmdInfo.put("DestLoc", rs.getString("DestLoc"));
				nextTrCmdInfo.put("TSC", rs.getString("TSC"));
				nextTrCmdInfo.put("Priority", new Integer(rs.getInt("Priority")));
				nextTrCmdInfo.put("Status", rs.getString("Status"));
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "GetNextTrCmdInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	boolean GetPrevTrCmdInfo(MyHashtable currTrCmdInfo, MyHashtable prevTrCmdInfo) {
		boolean bRet = false;
		int nCmdIndex = currTrCmdInfo.toInt("CmdIndex", 0);
		if (nCmdIndex < 2) {
			return false;
		}

		String strSql = "SELECT * FROM MicroTrCmd WHERE MacroTrCmdID='" + currTrCmdInfo.toString("MacroTrCmdID", 0) + "' AND CmdIndex=" + String.valueOf(currTrCmdInfo.toInt("CmdIndex", 0) - 1);
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				prevTrCmdInfo.put("MicroTrCmdID", rs.getString("MicroTrCmdID"));
				prevTrCmdInfo.put("CmdIndex", new Integer(rs.getInt("CmdIndex")));
				prevTrCmdInfo.put("SourceLoc", rs.getString("SourceLoc"));
				prevTrCmdInfo.put("DestLoc", rs.getString("DestLoc"));
				prevTrCmdInfo.put("TSC", rs.getString("TSC"));
				prevTrCmdInfo.put("Priority", new Integer(rs.getInt("Priority")));
				prevTrCmdInfo.put("Status", rs.getString("Status"));
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "GetNextTrCmdInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	boolean GetNextTrCmdInfo(String strCarrierID, String strSourceLoc, MyHashtable nextTrCmdInfo) {
		boolean bRet = false;
		String strSql = "SELECT * FROM MicroTrCmd WHERE CarrierID='" + strCarrierID + "' AND SourceLoc='" + strSourceLoc + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				nextTrCmdInfo.put("MicroTrCmdID", rs.getString("MicroTrCmdID"));
				nextTrCmdInfo.put("CmdIndex", new Integer(rs.getInt("CmdIndex")));
				nextTrCmdInfo.put("SourceLoc", rs.getString("SourceLoc"));
				nextTrCmdInfo.put("DestLoc", rs.getString("DestLoc"));
				nextTrCmdInfo.put("TSC", rs.getString("TSC"));
				nextTrCmdInfo.put("Priority", new Integer(rs.getInt("Priority")));
				nextTrCmdInfo.put("Status", rs.getString("Status"));
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "GetNextTrCmdInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	boolean GetNextTrCmdInfo(String strMicroTrCmdID, MyHashtable nextTrCmdInfo) {
		String strMacroTrCmdID = "";
		int nCmdIndex = 0;
		boolean bRet = false;
		String strSql = "SELECT * FROM MicroTrCmd WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strMacroTrCmdID = rs.getString("MacroTrCmdID");
				nCmdIndex = rs.getInt("CmdIndex");
				bRet = true;
			} else {
				return bRet;
			}
		} catch (SQLException e) {
			String strLog = "GetNextTrCmdInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		strSql = "SELECT * FROM MicroTrCmd WHERE MacroTrCmdID='" + strMacroTrCmdID + "' AND CmdIndex=" + String.valueOf(nCmdIndex + 1);
		rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				nextTrCmdInfo.put("MicroTrCmdID", rs.getString("MicroTrCmdID"));
				nextTrCmdInfo.put("MacroTrCmdID", rs.getString("MacroTrCmdID"));
				nextTrCmdInfo.put("CarrierID", rs.getString("CarrierID"));
				nextTrCmdInfo.put("Source", rs.getString("SourceLoc"));
				nextTrCmdInfo.put("Dest", rs.getString("DestLoc"));
				nextTrCmdInfo.put("TSC", rs.getString("TSC"));
				nextTrCmdInfo.put("Priority", new Integer(rs.getInt("Priority")));
				nextTrCmdInfo.put("CmdIndex", new Integer(rs.getInt("CmdIndex")));

				bRet = true;
			} else {
				return bRet;
			}
		} catch (SQLException e) {
			String strLog = "GetNextTrCmdInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return bRet;
	}

	/**
	 * 특정 반송명령(Micro/Macro)에 대해 LastLoc 갱신
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @param strMacroTrCmdID
	 *            String
	 * @param strLastLoc
	 *            String
	 */
	void UpdateTrCmdLastLoc(String strMicroTrCmdID, String strMacroTrCmdID, String strLastLoc) {
		String strSql = "UPDATE MicroTrCmd SET LastLoc='" + strLastLoc + "' WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		try {
			m_dbFrame.AddBatch(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateTrCmdLastLoc() AddBatch:MicroTrCmd - IOException: " + e.getMessage();
			WriteLog(strLog);
		}

		strSql = "UPDATE MacroTrCmd SET LastLoc='" + strLastLoc + "' WHERE MacroTrCmdID='" + strMacroTrCmdID + "'";
		try {
			m_dbFrame.AddBatch(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateTrCompletionTime() AddBatch:MacroTrCmd - IOException: " + e.getMessage();
			WriteLog(strLog);
		}

		try {
			m_dbFrame.ExecBatch();
		} catch (SQLException e) {
			String strLog = "UpdateTrCompletionTime() ExecBatch - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * Carrier의 AlternateFlag 갱신
	 * 
	 * @param strCarrierID
	 *            String
	 * @param nAltFlag
	 *            int
	 */
	void SetCarrierAltFlag(String strCarrierID, int nAltFlag) {
		String strSql = "UPDATE Carrier SET AlternateFlag=" + String.valueOf(nAltFlag) + " WHERE CarrierID='" + strCarrierID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "SetCarrierAltFlag() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 특정 ID를 가지는 Carrier 정보 얻기
	 * 
	 * @param strCarrierID
	 *            String
	 * @param carrierInfo
	 *            MyHashtable
	 * @return boolean
	 */
	boolean GetCarrierInfo(String strCarrierID, MyHashtable carrierInfo) {
		boolean bRet = false; // Carrier 존재 여부
		String strSql = "SELECT * FROM Carrier WHERE CarrierID='" + strCarrierID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
				if (rs.getString("CarrierID") != null)
					carrierInfo.put("CarrierID", rs.getString("CarrierID"));
				else
					carrierInfo.put("CarrierID", "");

				if (rs.getString("Status") != null)
					carrierInfo.put("Status", rs.getString("Status"));
				else
					carrierInfo.put("Status", "");

				if (rs.getString("CurrLoc") != null)
					carrierInfo.put("CurrLoc", rs.getString("CurrLoc"));
				else
					carrierInfo.put("CurrLoc", "");

				if (rs.getString("SourceLoc") != null)
					carrierInfo.put("SourceLoc", rs.getString("SourceLoc"));
				else
					carrierInfo.put("SourceLoc", "");

				if (rs.getString("DestLoc") != null)
					carrierInfo.put("DestLoc", rs.getString("DestLoc"));
				else
					carrierInfo.put("DestLoc", "");

				if (rs.getString("PrevLoc") != null)
					carrierInfo.put("PrevLoc", rs.getString("PrevLoc"));
				else
					carrierInfo.put("PrevLoc", "");

				if (rs.getString("NextLoc") != null)
					carrierInfo.put("NextLoc", rs.getString("NextLoc"));
				else
					carrierInfo.put("NextLoc", "");

				carrierInfo.put("AlternateFlag", new Integer(rs.getInt("AlternateFlag")));

				if (rs.getString("TimeoutStatus") != null)
					carrierInfo.put("TimeoutStatus", rs.getString("TimeoutStatus"));
				else
					carrierInfo.put("TimeoutStatus", "");
			}
		} catch (SQLException e) {
			String strLog = "GetCarrierInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	/**
	 * 특정 ID의 CarrierLoc 정보 얻기
	 * 
	 * @param strLocID
	 *            String
	 * @param locInfo
	 *            Myhashtable
	 * @return boolean
	 */
	boolean GetCarrierLocInfo(String strLocID, MyHashtable locInfo) {
		boolean bRet = false;
		String strSql = "SELECT * FROM CarrierLoc WHERE CarrierLocID='" + strLocID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				if (rs.getString("Type") != null)
					locInfo.put("Type", rs.getString("Type"));
				else
					locInfo.put("Type", "");

				if (rs.getString("Owner") != null)
					locInfo.put("Owner", rs.getString("Owner"));
				else
					locInfo.put("Owner", "");

				if (rs.getString("PortType") != null)
					locInfo.put("PortType", rs.getString("PortType"));
				else
					locInfo.put("PortType", "");

				if (rs.getString("PortPriorityType") != null)
					locInfo.put("PortPriorityType", rs.getString("PortPriorityType"));
				else
					locInfo.put("PortPriorityType", "");

				if (rs.getString("Partition") != null)
					locInfo.put("Partition", rs.getString("Partition"));
				else
					locInfo.put("Partition", "");

				if (rs.getString("InOutMode") != null)
					locInfo.put("InOutMode", rs.getString("InOutMode"));
				else
					locInfo.put("InOutMode", "");

				locInfo.put("Enabled", new Integer(rs.getInt("Enabled")));

				if (rs.getString("AlarmList") != null)
					locInfo.put("AlarmList", rs.getString("AlarmList"));
				else
					locInfo.put("AlarmList", "");

				if (rs.getString("AlarmSetTime") != null)
					locInfo.put("AlarmSetTime", rs.getString("AlarmSetTime"));
				else
					locInfo.put("AlarmSetTime", "");

				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "GetCarrierLocInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	/**
	 * 특정 CarrierLoc에 대한 Partition 얻기
	 * 
	 * @param strLoc
	 *            String
	 * @return String
	 */
	String GetPartition(String strLoc) {
		String strPartition = null;
		String strSql = "SELECT Partition FROM CarrierLoc WHERE CarrierLocID='" + strLoc + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strPartition = rs.getString("Partition");
			}
		} catch (SQLException e) {
			String strLog = "GetPartition() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strPartition;
	}

	/**
	 * 특정 Stocker에 대한 Partition 얻기
	 * 
	 * @param strDevice
	 *            String
	 * @return String
	 */
	String GetSTKPartition(String strDevice) {
		String strPartition = null;
		String strSql = "SELECT PartitionID FROM Partition WHERE Owner='" + strDevice + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strPartition = rs.getString("PartitionID");
			}
		} catch (SQLException e) {
			String strLog = "GetSTKPartition() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strPartition;
	}

	/**
	 * 특정 ID의 MicroTrCmd 정보
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @param trCmdInfo
	 *            MyHashtable
	 * @return boolean
	 */
	boolean GetMicroTCInfo(String strMicroTrCmdID, MyHashtable trCmdInfo) {
		boolean bRet = false;
		String strSql = "SELECT * FROM MicroTrCmd WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				trCmdInfo.put("MacroTrCmdID", rs.getString("MacroTrCmdID"));
				trCmdInfo.put("MicroTrCmdID", rs.getString("MicroTrCmdID"));
				trCmdInfo.put("CarrierID", rs.getString("CarrierID"));
				trCmdInfo.put("SourceLoc", rs.getString("SourceLoc"));
				trCmdInfo.put("DestLoc", rs.getString("DestLoc"));
				if (rs.getString("LastLoc") != null)
					trCmdInfo.put("LastLoc", rs.getString("LastLoc"));
				else
					trCmdInfo.put("LastLoc", "");
				trCmdInfo.put("TSC", rs.getString("TSC"));
				trCmdInfo.put("Status", rs.getString("Status"));
				if (rs.getString("Vehicle") != null)
					trCmdInfo.put("Vehicle", rs.getString("Vehicle"));
				else
					trCmdInfo.put("Vehicle", "");
				if (rs.getString("PrevActiveStatus") != null)
					trCmdInfo.put("PrevActiveStatus", rs.getString("PrevActiveStatus"));
				else
					trCmdInfo.put("PrevActiveStatus", "");
				trCmdInfo.put("CmdIndex", new Integer(rs.getInt("CmdIndex")));
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "GetMicroTCInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	/**
	 * 특정 Carrie에 대해 Dest가 특정 위치인 MicroTrCmd 정보
	 * 
	 * @param strCarrierID
	 *            String
	 * @param strDestLoc
	 *            String
	 * @param trCmdInfo
	 *            MyHashtable
	 * @return boolean
	 */
	boolean GetMicroTCInfo(String strCarrierID, String strDestLoc, MyHashtable trCmdInfo) {
		boolean bRet = false; // 조건에 맞는 Tr. Cmd 존재 여부
		String strSql = "SELECT * FROM MicroTrCmd WHERE CarrierID='" + strCarrierID + "'";
		strSql += " AND DestLoc='" + strDestLoc + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				trCmdInfo.put("MacroTrCmdID", rs.getString("MacroTrCmdID"));
				trCmdInfo.put("CarrierID", rs.getString("CarrierID"));
				trCmdInfo.put("SourceLoc", rs.getString("SourceLoc"));
				trCmdInfo.put("DestLoc", rs.getString("DestLoc"));
				trCmdInfo.put("TSC", rs.getString("TSC"));
				trCmdInfo.put("Status", rs.getString("Status"));
				if (rs.getString("PrevActiveStatus") != null)
					trCmdInfo.put("PrevActiveStatus", rs.getString("PrevActiveStatus"));
				else
					trCmdInfo.put("PrevActiveStatus", "");
				trCmdInfo.put("CmdIndex", new Integer(rs.getInt("CmdIndex")));
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "GetMicroTCInfo() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	/**
	 * 특정 Carrier의 Timeout Status 갱신
	 * 
	 * @param strCarrierID
	 *            String
	 * @param strTimeoutStatus
	 *            String
	 */
	void UpdateCarrierTimeoutStatus(String strCarrierID, String strTimeoutStatus) {
		String strSql = "UPDATE Carrier SET TimeoutStatus='" + strTimeoutStatus + "', TimeoutStartTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		strSql += " WHERE CarrierID='" + strCarrierID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateCarrierTimeoutStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * Micro Tr. Cmd의 Status에 대한 Time 갱신
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @param strStatusType
	 *            String
	 */
	void UpdateTrCmdStatusTime(String strMicroTrCmdID, String strStatusType) {
		String strSql = "UPDATE MicroTrCmd SET " + strStatusType + "Time=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateTrCmdStatusTime() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 특정 MicroTrCmdID에 대한 MacroTrCmdID 정보 얻기
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @return String
	 */
	String GetMacroTrCmdID(String strMicroTrCmdID) {
		String strMacroTrCmdID = "";
		String strSql = "SELECT MacroTrCmdID FROM MicroTrCmd WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strMacroTrCmdID = rs.getString("MacroTrCmdID");
			}
		} catch (SQLException e) {
			String strLog = "GetMacroTrCmdID() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strMacroTrCmdID;
	}

	/**
	 * 특정 ID의 Carrier가 시스템 상에 존재하는지 여부
	 * 
	 * @param strCarrierID
	 *            String
	 * @return boolean
	 */
	boolean IsValidCarrier(String strCarrierID) {
		boolean bRet = false;
		String strSql = "SELECT CarrierID FROM Carrier WHERE CarrierID='" + strCarrierID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "IsValidCarrier() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return bRet;
	}

	/**
	 * Micro Tr. Cmd의 현재 상태
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @return String
	 */
	String GetMicroTCStatus(String strMicroTrCmdID) {
		String strStatus = "";
		String strSql = "SELECT Status FROM MicroTrCmd WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strStatus = rs.getString("Status");
			}
		} catch (SQLException e) {
			String strLog = "GetMicroTCStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strStatus;
	}

	/**
	 * 특정 MicroTrCmd의 현재 Status 얻기
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @return String
	 */
	String GetCurrMicroTCStatus(String strMicroTrCmdID) {
		String strCurrStatus = "";
		String strSql = "SELECT Status FROM MicroTrCmd WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strCurrStatus = rs.getString("Status");
			}
		} catch (SQLException e) {
			String strLog = "GetCurrMicroTCStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strCurrStatus;
	}

	/**
	 * ControlStatus 및 CommunicationStatus 갱신 ControlStatus=ONLINE/OFFLINE,
	 * CommunicationStatus=COMMUNICATING/NOT_COMMUNICATING
	 * 
	 * @param strControlStatus
	 *            String
	 * @param strCommunicationStatus
	 *            String
	 */
	void UpdateControlStatus(String strControlStatus, String strCommunicationStatus) {
		String strSql = "UPDATE TSC SET ControlStatus='" + strControlStatus + "', CommunicationStatus='" + strCommunicationStatus + "', DataUpdatedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		strSql += " WHERE TSCID='" + m_strTSCID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateControlStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * HostReport DSE(Device Status Event)
	 */
	void HostReport_DSE() {
		String strControlStatus = GetControlStatus();
		String strLogicalState = "ONLINE";
		String strEquipmentState = "";
		String strCommunicationState = GetCommunicationStatus();
		String strDeviceState = "";
		String strDeviceActivity = "IDLE";

		if (strControlStatus.equals("ONLINE")) {
			strEquipmentState = "REMOTE";
			strDeviceState = "RUN";
		} else {
			strEquipmentState = "OFFLINE";
			strDeviceState = "STOP";
		}

		CMessage HostMsg = new CMessage();
		HostMsg.SetMessageName("DSE");
		HostMsg.SetMessageItem("DeviceId", m_strTSCID, false);
		HostMsg.SetMessageItem("LogicalState", strLogicalState, false);
		HostMsg.SetMessageItem("DeviceState", strDeviceState, false);
		HostMsg.SetMessageItem("DeviceActivity", strDeviceActivity, false);
		HostMsg.SetMessageItem("EquipmentState", strEquipmentState, false);
		HostMsg.SetMessageItem("CommunicationState", strCommunicationState, false);
		HostMsg.SetMessageItem("ErrorState", "CLEARED", false);
		HostMsg.SetMessageItem("ControllerState", "", false);
		WriteLog("[DSE] " + HostMsg.ToMessage());
		RegisterHostReport(HostMsg.ToMessage());
	}

	/**
	 * Host Report Message 등록 (HostReport table)
	 * 
	 * @param strMsg
	 *            String
	 */
	void RegisterHostReport(String strMsg) {
		// 2005.10.05 ReportTime반영
		//String strSql = "INSERT INTO HostReport (Message) VALUES ('" + strMsg + "')";
		String strSql = "INSERT INTO HostReport (Message, ReportTime) VALUES ('" + strMsg + "', TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3'))";

		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RegisterHostReport() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * TSC의 Control Status 정보 얻기 ONLINE/OFFLINE
	 * 
	 * @return String
	 */
	String GetControlStatus() {
		String strControlStatus = "OFFLINE";
		String strSql = "SELECT ControlStatus FROM TSC WHERE TSCID='" + m_strTSCID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strControlStatus = rs.getString("ControlStatus");
			}
		} catch (SQLException e) {
			String strLog = "GetControlStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strControlStatus;
	}

	/**
	 * TSC의 Communication Status 정보 얻기 COMMUNICATING/NOT_COMMUNICATING
	 * 
	 * @return String
	 */
	String GetCommunicationStatus() {
		String strCommunicationStatus = "NOT_COMMUNICATING";
		String strSql = "SELECT CommunicationStatus FROM TSC WHERE TSCID='" + m_strTSCID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strCommunicationStatus = rs.getString("CommunicationStatus");
			}
		} catch (SQLException e) {
			String strLog = "GetCommunicationStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strCommunicationStatus;
	}

	/**
	 * TSC Status 갱신
	 * 
	 * @param strStatus
	 *            String
	 */
	void UpdateTSCStatus(String strStatus) {
		boolean bUpdated = false;
		String strSql = "UPDATE TSC SET TSCStatus='" + strStatus + "', DataUpdatedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		strSql += " WHERE TSCID='" + m_strTSCID + "' AND TSCStatus<>'" + strStatus + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
			bUpdated = true;
		} catch (SQLException e) {
			String strLog = "UpdateTSCStatus() - IOException: " + e.getMessage();
			WriteLog(strLog);
		}

		if (bUpdated && strStatus.equals("AUTO")) {
			// Suspended MicroTrCmd 상태 변경 -> READY
			strSql = "UPDATE MicroTrCmd SET Status='READY' WHERE Status='Suspended' AND TSC='" + m_strTSCID + "' AND Status<>'READY'";
			try {
				m_dbFrame.ExecSQL(strSql);
			} catch (SQLException e) {
				String strLog = "UpdateTSCStatus() - IOException: " + e.getMessage();
				WriteLog(strLog);
			}
		}
	}

	/**
	 * Ini File로부터 Configuration Data 읽기
	 * 
	 * @return boolean
	 */
	boolean LoadConfig() {
		StringBuffer FilePathName = new StringBuffer();
		String strFileName = "OCSManager.ini";

		// get current directory
		String Path = System.getProperty("user.dir");
		String Separator = System.getProperty("file.separator");
		FilePathName.append(Path).append(Separator).append(strFileName);

		File f;
		RandomAccessFile raf;

		int i, nPos;
		String sLine = "";

		try {
			f = new File(FilePathName.toString());
			raf = new RandomAccessFile(f, "r");

			while ((sLine = raf.readLine()) != null) {
				// Database
				if (sLine.indexOf("TSCID") == 0) {
					nPos = sLine.indexOf("=");
					m_strTSCID = sLine.substring(nPos + 1);
					m_strTSCID = m_strTSCID.trim();
				}
				// 2015.01.30 by KYK : OCS, STBC 용 MCS 통합
				else if (sLine.indexOf("TYPE") == 0) {
					nPos = sLine.indexOf("=");
					tscType = sLine.substring(nPos + 1);
					tscType = tscType.trim();
				} else if (sLine.indexOf("HangCheck") == 0) {
					nPos = sLine.indexOf("=");
					m_sHangCheck = sLine.substring(nPos + 1);
					m_sHangCheck = m_sHangCheck.trim();
				} else if (sLine.indexOf("Path") == 0) {
					nPos = sLine.indexOf("=");
					m_sLogPath = sLine.substring(nPos + 1);
					m_sLogPath = m_sLogPath.trim();
				} else if (sLine.indexOf("DeleteLogDay") == 0) {
					nPos = sLine.indexOf("=");
					String strDeleteLogDay = sLine.substring(nPos + 1);
					try {
						m_nDeleteLogDay = Integer.parseInt(strDeleteLogDay);
					} catch (Exception e1) {
					}
				}
			}
			raf.close();
		} catch (IOException e) {
			String strLog = "LoadConfig - IOException: " + e.getMessage();
			//System.out.println(strLog);
			WriteLog(strLog);
			return false;
		}

		return true;
	}

	/**
	 * MainTimerProc : MainTimerTask 수행
	 */
	class MainTimerProc extends TimerTask {
		OCSManagerMain theClass;

		public MainTimerProc(OCSManagerMain instance) {
			theClass = instance;
		}

		public void run() {
			// 2006.01.23 Thread 정상 가동에 대한 Timestamp
			m_lMainOperationTimeStamp = System.currentTimeMillis();
			m_bMainOperationTimerActiveFlag = true;

			try {
				if (semInit == false) {
					InitalizeSEM();
					semInit = true;
				} else {
				}
				if (m_bSyncCarrierInfo == false)
					return;
				//long lInitTime = System.currentTimeMillis();
				// Control Param 값 얻기
				GetControlParam();
				//long lParamTime = System.currentTimeMillis();
				// 실행할 MicroTrCmd 검색
				TrCmdOperation();
				//long lOperationTime = System.currentTimeMillis();
				// User Operation 처리
				ExecUserOperation();
				//long lUserOpTime = System.currentTimeMillis();

				// 2006.02.08 라인에서 5초를 넘는 경우가 발생하여 로그추가
				//String strLog = "MainTimer Timeout(" + (lUserOpTime - m_lMainOperationTimeStamp) + ") ";
				//strLog += (" Init:" + (lInitTime - m_lMainOperationTimeStamp));
				//strLog += (" Param:" + (lParamTime - lInitTime));
				//strLog += (" Operation:" + (lOperationTime - lParamTime));
				//strLog += (" UserOp:" + (lUserOpTime - lOperationTime));
				//if ( (lUserOpTime - m_lMainOperationTimeStamp) > 5000)
				//{
				//  WriteLog(strLog);
				//}
			} catch (Exception e) {
				m_dbFrame.ReconnectToDB();
			} finally {
				// 2006.01.23 Thread 정상 가동에 대한 Timestamp
				m_lMainOperationTimeStamp = System.currentTimeMillis();
				m_bMainOperationTimerActiveFlag = false;
			}
		}
	}

	/**
	 * AbnormalTimerProc : MainTimerTask 수행
	 */
	class AbnormalTimerProc extends TimerTask {
		OCSManagerMain theClass;

		public AbnormalTimerProc(OCSManagerMain instance) {
			theClass = instance;
		}

		public void run() {
			// 2006.01.23 Thread 정상 가동에 대한 Timestamp
			m_lAbnormalOperationTimeStamp = System.currentTimeMillis();
			m_bAbnormalOperationTimerActiveFlag = true;

			try {
				if (semInit == false)
					return;

				if (m_bSyncCarrierInfo == false)
					return;

				// InTransitTimeout 처리
				HandleInTransitTimeout();

				// Abnormal Tr. Cmd 처리
			} catch (Exception e) {
				m_dbFrame.ReconnectToDB();
			} finally {
				// 2006.01.23 Thread 정상 가동에 대한 Timestamp
				m_lAbnormalOperationTimeStamp = System.currentTimeMillis();
				m_bAbnormalOperationTimerActiveFlag = true;
			}
		}
	}

	/**
	 * WatchDogTimerProc : MainTimerTask 수행
	 */
	class WatchDogTimerProc extends TimerTask {
		OCSManagerMain theClass;

		public WatchDogTimerProc(OCSManagerMain instance) {
			theClass = instance;
		}

		public void run() {
			if ((m_sHangCheck != null) && m_sHangCheck.equals("true") == true) {
				Util.ThreadHangCheckLog(".ALIVE", "");
			}

			// Log Delete
			if (m_lLastDeleteLogTime == 0) {
				m_lLastDeleteLogTime = System.currentTimeMillis();
				Util.DeleteAutoLogFiles(m_nDeleteLogDay);
			} else {
				// 1시간에 한번씩 log deletion 실행
				if ((System.currentTimeMillis() - m_lLastDeleteLogTime) > 3600000) {
					m_lLastDeleteLogTime = System.currentTimeMillis();
					Util.DeleteAutoLogFiles(m_nDeleteLogDay);
				}
			}

			// Message Send Flag Check
			if (m_lLastSendS2F49 == 0) {
				m_lLastSendS2F49 = System.currentTimeMillis();
				m_bSendS2F49 = false;
			} else {
				// 10초에 한번씩 Send Flag 초기화
				if ((System.currentTimeMillis() - m_lLastSendS2F49) > 10000) {
					m_lLastSendS2F49 = System.currentTimeMillis();
					m_bSendS2F49 = false;
				}
			}
			// Message Send Flag Check
			if (m_lLastSendS2F41 == 0) {
				m_lLastSendS2F41 = System.currentTimeMillis();
				m_bSendS2F41 = false;
			} else {
				// 10초에 한번씩 Send Flag 초기화
				if ((System.currentTimeMillis() - m_lLastSendS2F41) > 10000) {
					m_lLastSendS2F41 = System.currentTimeMillis();
					m_bSendS2F41 = false;
				}
			}

			long lCurrTime = System.currentTimeMillis();
			// 2006.01.23 Timer를 Reset하는 기능수정
			//if ( (lCurrTime - m_lMainOperationTimeStamp) > 5000)
			//{
			//  ResetTimer_Main();
			//}
			//if ( (lCurrTime - m_lAbnormalOperationTimeStamp) > 20000)
			//{
			//  ResetTimer_Abnormal();
			//}
			if ((lCurrTime - m_lMainOperationTimeStamp) > 5000) {
				WriteLog("MainOperationCheckTimer Delayed : " + m_bMainOperationTimerActiveFlag);

				// 기존에 등록된 Alarm이 없는 경우에 등록
				//if (CheckExistTimerDelayAlarm("MainOperationCheckTimer Delayed") == 0)
				{
					// AlarmID   : -21000
					// AlarmText : MacroTrCheckTimer Delayed
					// AlarmEventType : TimeDelay
					RegisterAlarm("TSC", m_strTSCID, -21000, "1", "MainOperationCheckTimer Delayed", "TimeDelay", "");
				}

				if (m_bMainOperationTimerActiveFlag == false) {
					ResetTimer_Main();
				}
			}
			if ((lCurrTime - m_lAbnormalOperationTimeStamp) > 20000) {
				WriteLog("AbnormalOperationCheckTimer Delayed : " + m_bAbnormalOperationTimerActiveFlag);

				// 기존에 등록된 Alarm이 없는 경우에 등록
				//if (CheckExistTimerDelayAlarm("AbnormalOperationCheckTimer Delayed") == 0)
				{
					// AlarmID   : -21000
					// AlarmText : AbnormalOperationCheckTimer Delayed
					// AlarmEventType : TimeDelay
					RegisterAlarm("TSC", m_strTSCID, -21000, "1", "AbnormalOperationCheckTimer Delayed", "TimeDelay", "");
				}

				if (m_bAbnormalOperationTimerActiveFlag == false) {
					ResetTimer_Abnormal();
				}
			}
		}
	}

	void ResetTimer_Main() {
		// 2006.01.23 Timer를 Reset하는 기능수정
		//MainTimer.cancel();
		//MainTimer = null;
		//MainTimerTask = null;
		//MainTimer = new java.util.Timer();
		//MainTimerTask = new MainTimerProc(this);
		//MainTimer.schedule(MainTimerTask, 0, 1000);
		if (MainTimer != null) {
			MainTimer.cancel();
			MainTimer = null;
			MainTimer = null;
			WriteLog("MainTimer stopped!!");
		} else if (m_bMainOperationTimerActiveFlag == false) {
			MainTimer = new java.util.Timer();
			MainTimerTask = new MainTimerProc(this);
			MainTimer.schedule(MainTimerTask, 0, 1000);
			WriteLog("MainOperationTimer Restart!!");
		}
	}

	void ResetTimer_Abnormal() {
		// 2006.01.23 Timer를 Reset하는 기능수정
		//AbnormalTimer.cancel();
		//AbnormalTimer = null;
		//AbnormalTimerTask = null;
		//AbnormalTimer = new java.util.Timer();
		//AbnormalTimerTask = new AbnormalTimerProc(this);
		//AbnormalTimer.schedule(AbnormalTimerTask, 0, 1000);
		if (AbnormalTimer != null) {
			AbnormalTimer.cancel();
			AbnormalTimer = null;
			AbnormalTimer = null;
			WriteLog("MainTimer stopped!!");
		} else if (m_bAbnormalOperationTimerActiveFlag == false) {
			AbnormalTimer = new java.util.Timer();
			AbnormalTimerTask = new AbnormalTimerProc(this);
			AbnormalTimer.schedule(AbnormalTimerTask, 0, 1000);
			WriteLog("AbnormalOperationTimer Restart!!");
		}
	}

	/**
	 * 비정상 Carrier Manual Output 처리 비정상 Carrier 포함 문자열 : DUP_ / UNKNOWN
	 */
	void HandleInTransitTimeout() {
		if (m_nInTransitTimeout <= 0) {
			return;
		}

		Vector vtMicroTrCmdIDList = new Vector();

		String strTimeout = String.valueOf(m_nInTransitTimeout) + "/24/60/60";
		String strPrevTime = "TO_CHAR(SYSDATE-" + strTimeout + ", 'YYYYMMDDHH24MISS')";
		//String strSql = "SELECT MicroTrCmdID FROM MicroTrCmd WHERE TSC='" + m_strTSCID
		//    + "' AND StatusChangedTime<" + strPrevTime;
		//String strSql = "SELECT MicroTrCmdID FROM MicroTrCmd WHERE CarrierID IN";
		//strSql += " (SELECT CarrierID FROM Carrier WHERE TimeoutStatus='IN_TRANSIT' AND TimeoutStartTime<" + strPrevTime + ")";
		// 2005.12.27 Query 정보 수정
		String strSql = "SELECT MicroTrCmdID FROM MicroTrCmd WHERE TSC='" + m_strTSCID + "' AND Status<>'NONE' AND StatusChangedTime<" + strPrevTime;
		// 2006.02.10 Query 정보 수정
		strSql += " AND CarrierID In (SELECT CarrierID FROM Carrier WHERE TimeoutStatus='IN_TRANSIT')";

		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if (rs != null) {
				while (rs.next()) {
					vtMicroTrCmdIDList.add(rs.getString("MicroTrCmdID"));
				}
			}
		} catch (SQLException e) {
			String strLog = "HandleInTransitTimeout - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		int nMicroTrCmdCnt = vtMicroTrCmdIDList.size();
		if (nMicroTrCmdCnt > 0) {
			MyHashtable trCmdInfo = new MyHashtable();
			// TSC의 대체 STK로 반송명령 설정
			String strAltLoc = GetAltSTK_TSC(m_strTSCID, false);
			if (strAltLoc != null && !strAltLoc.equals("")) {
				for (int i = 0; i < nMicroTrCmdCnt; i++) {
					if (GetMicroTCInfo((String) vtMicroTrCmdIDList.get(i), trCmdInfo)) {
						if (!GetOwnerDevice(trCmdInfo.toString("DestLoc", 0)).equals(strAltLoc)) {
							SendAbort(trCmdInfo);
							String strLog = "HandleInTransitTimeout: " + trCmdInfo.toString("MicroTrCmdID", 0);
							WriteLog(strLog);
						}
					}
				}
			} else {
				String strLog = "AltSTK for " + m_strTSCID + " Not Found! (HandleInTransitTimeout)";
				WriteLog(strLog);
			}
		}
	}

	/**
	 * Control Parameter 값 얻기
	 */
	void GetControlParam() {
		String strSql = "SELECT * FROM SystemConfig";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if (rs != null) {
				while (rs.next()) {
					String strParamName = rs.getString("ParamName");
					if (strParamName.equals("HoldCmdTempTimeout")) {
						m_nTempHoldedTimeout = Integer.parseInt(rs.getString("Value"));
					} else if (strParamName.equals("InTransitTimeout")) {
						m_nInTransitTimeout = Integer.parseInt(rs.getString("Value"));
					}
				}
			}
		} catch (SQLException e) {
			String strLog = "GetControlParam - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
	}

	/**
	 * 반송 명령 처리
	 */
	synchronized void TrCmdOperation() {
		if (IsTSCAuto()) {
			// 정상 반송 처리
			NormalTrCmdOperation();

			// 2005.08.12 TempHold의 경우와 Fail의 경우를 나누어 처리하도록 변경
			// 일정 시간 이상 경과한 TempHolded 상태의 Tr. Cmd 상태를 READY로 전환
			MakeTempHoldedCmdToReady();
			// 일정 시간 이상 FAIL 상태로 유지되는 Tr. Cmd 상태를 READY로 전환
			MakeAbnormalTrCmdToReady();

			// 2011.09.07 by LWG : [롱런을 읽어서 반송 내리기]
			flrm.processTransfer();
		}
	}

	/**
	 * 정상 반송 실행
	 */
	void NormalTrCmdOperation() {
		// 실행할 반송명령 검색
		MyHashtable TrCmdInfo = new MyHashtable();
		while (GetTrCmdInfo_InTurn(TrCmdInfo) == true) {
			// 검색된 반송명령 실행
			ExecuteTrCmd(TrCmdInfo);
		}
	}

	/**
	 * 반송 대상(Status=Ready) Micro Tr. Cmd 검색 우선순위 : Priority -> PortPriority ->
	 * InstallTime
	 * 
	 * @param trCmdInfo
	 *            MyHashtable
	 * @return boolean
	 */
	boolean GetTrCmdInfo_InTurn(MyHashtable trCmdInfo) // 미완성(PortPriority)
	{
		boolean bRet = false;
		String strSql = "SELECT * FROM MicroTrCmd WHERE TSC='" + m_strTSCID + "' AND Status='READY' ORDER BY Priority, InstallTime ASC";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				trCmdInfo.put("MicroTrCmdID", rs.getString("MicroTrCmdID"));
				trCmdInfo.put("MacroTrCmdID", rs.getString("MacroTrCmdID"));
				trCmdInfo.put("CarrierID", rs.getString("CarrierID"));
				trCmdInfo.put("Source", rs.getString("SourceLoc"));
				trCmdInfo.put("Dest", rs.getString("DestLoc"));
				trCmdInfo.put("TSC", rs.getString("TSC"));
				trCmdInfo.put("Priority", new Integer(rs.getInt("Priority")));
				trCmdInfo.put("CmdIndex", new Integer(rs.getInt("CmdIndex")));
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "GetTrCmdInfo_InTurn() - SQLException : " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return bRet;
	}

	boolean GetTrCmdInfo(MyHashtable trCmdInfo) {
		boolean bRet = false;
		String strSql = "SELECT * FROM MicroTrCmd WHERE TSC='" + m_strTSCID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				trCmdInfo.put("MicroTrCmdID", rs.getString("MicroTrCmdID"));
				trCmdInfo.put("MacroTrCmdID", rs.getString("MacroTrCmdID"));
				trCmdInfo.put("CarrierID", rs.getString("CarrierID"));
				trCmdInfo.put("Source", rs.getString("SourceLoc"));
				trCmdInfo.put("Dest", rs.getString("DestLoc"));
				trCmdInfo.put("TSC", rs.getString("TSC"));
				trCmdInfo.put("Priority", new Integer(rs.getInt("Priority")));
				trCmdInfo.put("CmdIndex", new Integer(rs.getInt("CmdIndex")));
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "GetTrCmdInfo() - SQLException : " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return bRet;
	}

	/**
	 * 반송 대상 Micro Tr. Cmd의 실행 반송경로 상태 점검 -> 반송 명령 전송 to IBSEMIF
	 * 
	 * @param trCmdInfo
	 *            MyHashtable
	 */
	void ExecuteTrCmd(MyHashtable trCmdInfo) {
		// MicroTrCmd
		String strMicroTrCmdID = trCmdInfo.toString("MicroTrCmdID", 0);

		// CarrierID
		String strCarrierID = trCmdInfo.toString("CarrierID", 0);

		// Source Type : INOUT, AUTO_IN
		String strSource = trCmdInfo.toString("Source", 0);
		String strSourceType = "";
		strSourceType = GetLocType(strSource);

		// Dest Type : INOUT, AUTO_OUT
		String strDest = trCmdInfo.toString("Dest", 0);
		String strDestType = "";
		strDestType = GetLocType(strDest);

		MyHashtable locInfo = new MyHashtable();
		GetCarrierLocInfo(strDest, locInfo);
		String strDevice = locInfo.toString("Owner", 0);

		String strAltLoc = "";
		// 반송 경로 상태 Check
		int nStatus = 0;
		nStatus = CheckRouteStatus(strMicroTrCmdID, strSource, strSourceType, strDest, strDestType);
		MyHashtable reqRouteInfo;
		switch (nStatus) {
		case NORMAL_TRANSFER:

			// 반송명령 전송 to IBSEMIF
			SendMicroTC(trCmdInfo);
			break;
		case ALT_PORT_TRANSFER:
			// 대체 Port 설정
			if ((strAltLoc = GetAltSTKPort(strDest)) != null && !strAltLoc.equals("")) {
				ReqRoutePortChange(trCmdInfo, strAltLoc);
			}
			// 대체 STK로 반송명령 설정
			else if ((strAltLoc = GetAltSTK(strDest)) != null && !strAltLoc.equals("")) {
				ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
			}
			// TSC의 대체 STK로 반송명령 설정
			else if ((strAltLoc = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltLoc.equals("")) {
				ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
			} else {
				String strLog = "AltSTK for " + strDest + " Not Found! (ExecuteTrCmd)";
				WriteLog(strLog);
			}
			break;
		case ALT_STK_TRANSFER:
			// 대체 STK로 반송명령 설정
			if ((strAltLoc = GetAltSTK(strDest)) != null && !strAltLoc.equals("")) {
				ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
			}
			// TSC의 대체 STK로 반송명령 설정
			else if ((strAltLoc = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltLoc.equals("")) {
				ReqRouteResearch(trCmdInfo, strAltLoc, GetReturnFromAltFlag(strDevice));
			} else {
				String strLog = "AltSTK for " + strDest + " Not Found! (ExecuteTrCmd)";
				WriteLog(strLog);
			}
			break;
		case HOLD_TRANSFER:
			// 반송명령 상태 변경 : Status=TransferPaused
			UpdateTrCmdStatus(trCmdInfo.toString("MicroTrCmdID", 0), trCmdInfo.toString("MacroTrCmdID", 0), "TransferPaused");
			break;
		case TEMPHOLD_TRANSFER:
			// 반송명령 상태 변경 : Status=TempHolded
			UpdateTrCmdStatus(trCmdInfo.toString("MicroTrCmdID", 0), trCmdInfo.toString("MacroTrCmdID", 0), "TempHolded");
			break;
		default:
			break;
		}
	}

	// 2005.10.28 싱크 추가
	synchronized void ExecUserOperation() {
		// 2005.09.09 TSC가 연결되지 않은 상태에서 진행되지 않도록 변경
		if (!IsTSCAuto()) {
			return;
		}
		// 2005.08.08 ReqSyncRealTimeData 관련 기능추가
		Vector vtMsgStringList = new Vector();
		Vector vtMsgSenderList = new Vector();
		Vector vtCommandTimeList = new Vector();
		String strSql = "SELECT * FROM UserOperation WHERE MsgReceiver='" + m_strTSCID + "'";
		strSql += " AND (MsgString NOT LIKE '%ReqTerminalMessage%')";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if (rs != null) {
				while (rs.next()) {
					vtMsgStringList.add(rs.getString("MsgString"));
					vtMsgSenderList.add(rs.getString("MsgSender"));
					vtCommandTimeList.add(rs.getString("CommandTime"));
				}
			}
		} catch (SQLException e) {
			String strLog = "ExecUserOperation() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		if (vtMsgStringList.size() > 0) {
			int i = 0;
			String strMsg = "";
			String strMsgName = "";
			String strMsgSender = "";
			String strCommandTime = "";
			CMessage msg = new CMessage();

			boolean bSuccess;
			for (i = 0; i < vtMsgStringList.size(); i++) {
				strMsg = (String) vtMsgStringList.get(i);
				strMsgSender = (String) vtMsgSenderList.get(i);
				strCommandTime = (String) vtCommandTimeList.get(i);

				WriteLog("UserOperation() Query Result: " + strMsg + ", MsgSender: " + strMsgSender + ", CommandTime: " + strCommandTime);

				msg.Reset();
				msg.SetMessage(strMsg);
				//msg.SetMessage(strMsgSender);
				strMsgName = msg.GetMessageName();
				bSuccess = true;

				if (strMsgName.equals("CancelCmd")) {
					bSuccess = SendCancel(msg, strMsgSender);
				} else if (strMsgName.equals("ReqTSCStatusChange")) {
					ReqTSCStatusChange(msg);
				} else if (strMsgName.equals("ReqSyncRealTimeData")) {
					ReqSyncRealTimeData(msg);
				}

				// 2005.10.11 메시지를 정상처리한 경우에 삭제
				if (bSuccess == true) {
					strSql = "DELETE FROM UserOperation WHERE MsgReceiver='" + m_strTSCID + "'";
					strSql += " AND (MsgString NOT LIKE '%ReqTerminalMessage%')";
					strSql += " AND (MsgString='" + strMsg + "')";
					try {
						m_dbFrame.ExecSQL(strSql);
					} catch (SQLException e) {
						String strLog = "ExecUserOperation() DELETE FROM UserOperation - SQLException: " + e.getMessage();
						WriteLog(strLog);
					}
				}
			}
		}
	}

	void ReqSyncRealTimeData(CMessage msg) {
		GetTSCStatus();
		m_bOnSyncAll = true;
	}

	/**
	 * 반송 경로 상태 Check 반송 경로 상태에 따라 반송명령의 상태를 변경(Suspended, TempHolded) 시킬 수 있음
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @param strSource
	 *            String
	 * @param strSourceType
	 *            String
	 * @param strDest
	 *            String
	 * @param strDestType
	 *            String
	 * @return int
	 */
	int CheckRouteStatus(String strMicroTrCmdID, String strSource, String strSourceType, String strDest, String strDestType) {
		String strAltPort;
		String strAltSTK;

		// SKTFull인 Shelf로 반송하는 경우에 대체 STK로 반송한다.
		if (IsSTKFull(strDestType, strMicroTrCmdID) || IsSTKError(strDest)) {
			// 대체 STK로 반송
			if ((strAltSTK = GetAltSTK(strDest)) != null && !strAltSTK.equals("")) {
				return ALT_STK_TRANSFER;
			}
			// TSC의 대체 STK로 반송
			else if ((strAltSTK = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltSTK.equals("")) {
				return ALT_STK_TRANSFER;
			}
			// 대체 STK가 없으면 Hold
			else {
				return HOLD_TRANSFER;
			}
		}

		// 이동할 Port가 에러가 발생한 경우에 대체 Port로 반송한다.
		// 대체Port가 없으면 대체STK로 반송한다.
		if (IsPortError(strDest)) {
			// 대체 Port로 반송
			if ((strAltPort = GetAltSTKPort(strDest)) != null && !strAltPort.equals("")) {
				return ALT_PORT_TRANSFER;
			}
			// 대체 STK로 반송
			else if ((strAltSTK = GetAltSTK(strDest)) != null && !strAltSTK.equals("")) {
				return ALT_STK_TRANSFER;
			}
			// TSC의 대체 STK로 반송
			else if ((strAltSTK = GetAltSTK_TSC(m_strTSCID, true)) != null && !strAltSTK.equals("")) {
				return ALT_STK_TRANSFER;
			}
			// 대체 STK가 없으면 Hold
			else {
				return HOLD_TRANSFER;
			}
		}

		// 이동할 Port가 점유되어 있는 경우(동일Port당 최대반송수 관리)에 대체 Port로 반송한다.
		// 대체Port가 없으면 TempHold를 건다.
		if (IsPortFull(strDest)) {
			// 대체 Port로 반송
			if ((strAltPort = GetAltSTKPort(strDest)) != null && !strAltPort.equals("")) {
				return ALT_PORT_TRANSFER;
			}
			// 대체 Port가 없으면 TempHold
			else {
				return TEMPHOLD_TRANSFER;
			}
		}
		return NORMAL_TRANSFER;
	}

	/**
	 * 반송 TSC의 상태 점검
	 * 
	 * @return boolean
	 */
	boolean IsTSCAuto() {
		MyHashtable tscInfo = new MyHashtable();
		GetTSCInfo(tscInfo);

		if (tscInfo.toString("ControlStatus", 0).equals("ONLINE") && tscInfo.toString("TSCStatus", 0).equals("AUTO")) {
			return true;
		}

		return false;
	}

	/**
	 * STK의 Full 상태 점검
	 * 
	 * @param strDestType
	 *            String
	 * @param strMicroTrCmdID
	 *            String
	 * @return boolean
	 */
	boolean IsSTKFull(String strDestType, String strMicroTrCmdID) {
		if (strDestType.equals("AUTO_IN") || strDestType.equals("AUTO_INOUT")) {
			// DB조회해서 Shelf로의 반송여부를 확인
			MyHashtable nextTrCmdInfo = new MyHashtable();
			GetNextTrCmdInfo(strMicroTrCmdID, nextTrCmdInfo);
			String strCarrierLoc = "";
			String strCapa = "";
			strCarrierLoc = nextTrCmdInfo.toString("Dest", 0);
			if (GetCarrierLocType(strCarrierLoc).equals("SHELF")) {
				// Stocker Full 점검
				strCapa = GetCapaStatus(strCarrierLoc);
				if (strCapa != null && strCapa.equals("FULL")) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * STK의 Error 상태 점검
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return boolean
	 */
	boolean IsSTKError(String strCarrierLocID) {
		if (GetCarrierLocType(strCarrierLocID).equals("STOCKERPORT")) {
			// TSC 유효성
			String strTSCStatus = "";
			String strSql = "SELECT TSCSTATUS FROM TSC WHERE TSCID IN";
			strSql += " (SELECT TSC FROM Device WHERE DeviceID IN";
			strSql += " (SELECT Owner FROM CarrierLoc WHERE CarrierLocID='" + strCarrierLocID + "'))";

			ResultSet rs = null;
			try {
				rs = m_dbFrame.GetRecord(strSql);
				if ((rs != null) && (rs.next())) {
					strTSCStatus = rs.getString("TSCStatus");
				}
			} catch (SQLException e) {
				String strLog = "IsSTKError() TSCSTATUS 유효성 - Exception: " + e.getMessage();
				WriteLog(strLog);
			} finally {
				if (rs != null) {
					m_dbFrame.CloseRecord(rs);
				}
			}
			if (!strTSCStatus.equals("AUTO")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Port의 Error 상태 점검
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return boolean
	 */
	boolean IsPortError(String strCarrierLocID) {
		// Port의 방향성 고려
		MyHashtable locInfo = new MyHashtable();
		GetCarrierLocInfo(strCarrierLocID, locInfo);
		if (locInfo.toString("InOutMode", 0).equals("OUT"))
			return true;

		// STK LP PortID or EQ PortID
		String strAlarmList = "";
		strAlarmList = GetAlarmList(strCarrierLocID);
		if (strAlarmList != null) {
			return true;
		}

		// STK Port의 경우에는 OP도 확인
		if (GetCarrierLocType(strCarrierLocID).equals("STOCKERPORT")) {
			// OP PortID
			int nPos = strCarrierLocID.indexOf(".");
			if (nPos > -1) {
				String strOPPort = "";
				strOPPort = strCarrierLocID.substring(0, nPos) + ".OP";
				strAlarmList = GetAlarmList(strOPPort);
				if (strAlarmList != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Port의 Full 상태 점검
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return boolean
	 */
	boolean IsPortFull(String strCarrierLocID) {
		// Port에 반송예정된 Carrier의 수 확인
		int nWorkQCapa;
		nWorkQCapa = GetTSCWorkQCapa();
		if (nWorkQCapa == -1 || GetCarrierQtyInPort(strCarrierLocID) <= nWorkQCapa) {
			return false;
		}
		return true;
	}

	/**
	 * Tr. Cmd Status 변경
	 * 
	 * @param strMicroTrCmdID
	 *            MyHashtable
	 * @param strMacroTrCmdID
	 *            String
	 * @param strStatus
	 *            String
	 */
	void UpdateTrCmdStatus(String strMicroTrCmdID, String strMacroTrCmdID, String strStatus) {
		String strSql = "UPDATE MicroTrCmd SET Status='" + strStatus + "', StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		strSql += " WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateTrCmdStatus() 1 - Exception: " + e.getMessage();
			WriteLog(strLog);
		}

		// Macro Tr. Cmd Status 변경 : Transferring
		if (strStatus.equals("TransferInitiated")) {
			strSql = "UPDATE MacroTrCmd SET Status='TRANSFERRING', StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
			strSql += " WHERE MacroTrCmdID='" + strMacroTrCmdID + "' AND Status<>'Transferring'";
			try {
				m_dbFrame.ExecSQL(strSql);
			} catch (SQLException e) {
				String strLog = "UpdateTrCmdStatus() 2 - Exception: " + e.getMessage();
				WriteLog(strLog);
			}
		}
	}

	/**
	 * Dest가 AUTO_OUT인 반송명령의 경우 다음 Step의 반송경로 상태를 점검한다. 1. Dest가 Stocker인 경우
	 * CapaStatus 및 Stocker Input Port 상태 점검 2. Dest가 EQ인 경우 정상반송실행
	 * 
	 * @param trCmdInfo
	 *            MyHashtable
	 * @return boolean
	 */
	boolean CheckNextStepRouteStatus(MyHashtable trCmdInfo) {
		boolean bRet = false;
		MyHashtable nextTrCmdInfo = new MyHashtable();

		// 현재 반송명령 정보
		int nCurrCmdIndex = trCmdInfo.toInt("CmdIndex", 0);
		String strMacroTrCmdID = trCmdInfo.toString("MacroTrCmdID", 0);

		// 2단계 후의 반송명령
		String strSql = "SELECT MicroTrCmdID, SourceLoc, DestLoc, TSC FROM MicroTrCmd WHERE MacroTrCmdID='" + strMacroTrCmdID + "' AND CmdIndex=" + String.valueOf(nCurrCmdIndex + 2);
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				nextTrCmdInfo.put("MicroTrCmdID", rs.getString("MicroTrCmdID"));
				nextTrCmdInfo.put("SourceLoc", rs.getString("SourceLoc"));
				nextTrCmdInfo.put("DestLoc", rs.getString("DestLoc"));
				nextTrCmdInfo.put("TSC", rs.getString("TSC"));
			} else {
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "CheckNextStepRouteStatus() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		// 다음 단계의 반송명령 Dest가 EQ인 경우 무조건 정상반송 실행
		if (bRet == true) {
			return true;
		}

		String strDestLoc = nextTrCmdInfo.toString("Dest", 0);
		// 2단계 후의 반송명령 Dest가 Shelf가 아닐 경우 경우 무조건 정상반송 실행
		if (IsPartition(strDestLoc) == false) {
			return true;
		}
		// 2단계 후의 반송명령 Dest가 Shelf일 경우 CapaFull check
		else {
			if (GetCapaStatus(strDestLoc).equals("FULL") == false) {
				// SourceLoc(Stocker AUTO_IN)의 상태 check
				if (CheckNextStockerInPortStatus(strDestLoc) == true) {
					return true;
				} else {
					// 현재 반송명령을 TempHolded 처리
					UpdateTrCmdStatus(trCmdInfo.toString("MicroTrCmdID", 0), trCmdInfo.toString("MacroTrCmdID", 0), "TempHolded");
					return false;
				}
			} else {
				// 현재 반송명령을 Suspended 처리
				UpdateTrCmdStatus(trCmdInfo.toString("MicroTrCmdID", 0), trCmdInfo.toString("MacroTrCmdID", 0), "Suspended");
				return false;
			}
		}
	}

	boolean CheckNextStockerInPortStatus(String strPartitionID) {
		Vector vtInPortList = new Vector();
		String strSql = "SELECT CarrierLocID FROM CarrierLoc WHERE Owner IN";
		strSql += " (SELECT Owner FROM Partition WHERE PartitionID='" + strPartitionID + "')";
		strSql += " AND Enabled=1 AND (AlarmList='' OR AlarmList is null)";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if (rs != null) {
				while (rs.next()) {
					vtInPortList.add(rs.getString("CarrierLocID"));
				}
			}
		} catch (SQLException e) {

		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		// Error 없이 유효한 Port가 없을 경우 반송불가
		int nInPortCnt = vtInPortList.size();
		if (nInPortCnt == 0) {
			return false;
		}

		// Error 없이 유효한 Port 중 비어 있는 Port가 있을 때 반송 가능
		strSql = "SELECT CarrierID FROM Carrier WHERE";
		for (int i = 0; i < nInPortCnt; i++) {
			if (i == 0) {
				strSql += " CurrLoc='" + (String) vtInPortList.get(i) + "'";
			} else {
				strSql += " OR CurrLoc='" + (String) vtInPortList.get(i) + "'";
			}
		}
		rs = null;
		boolean bRet = false;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.last())) {
				if (nInPortCnt < rs.getRow()) {
					// Carrier로 점유되어 있지 않은 Port가 존재함 -> 반송 가능
					bRet = true;
				}
			} else {
				// Carrier로 점유된 Port가 없음 -> 반송 가능
				bRet = true;
			}
		} catch (SQLException e) {

		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		if (bRet == true) {
			return true;
		}

		return false;
	}

	String GetErrorText(int nErrorCode) {
		String strErrorText = "";
		switch (nErrorCode) {
		case ERR_LOADCONFIG_FAIL:
			strErrorText = "LoadConfig 실패";
			break;
		case ERR_DBCONNECTION_FAIL:
			strErrorText = "DB 연결 실패";
			break;
		default:
			strErrorText = "Unknown";
			break;
		}
		return strErrorText;
	}

	/**
	 * Stocker와의 통신을 위한 XCom Config File Name 얻기
	 * 
	 * @return String
	 */
	String GetIBSEMConfigFileName() {
		String strCommCfgFile = "";
		String strSql = "SELECT CommCfgFile FROM TSC WHERE TSCID='" + m_strTSCID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strCommCfgFile = rs.getString("CommCfgFile");
			}
		} catch (SQLException e) {
			String strLog = "GetIBSEMConfigFileName() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strCommCfgFile;
	}

	String GetTrCmdStatusFromStateVal(int nTrState) {
		String strStatus = "NONE";
		switch (nTrState) {
		case 1:
			strStatus = "QUEUED";
			break;
		case 2:
			strStatus = "Transferring";
			break;
		case 3:
			strStatus = "TransferPaused";
			break;
		case 4:
			strStatus = "TransferCancelInitiated";
			break;
		case 5:
			strStatus = "TransferAbortInitiated";
			break;
		case 6:
			strStatus = "TransferInitiated";
			break;
		}
		return strStatus;
	}

	/**
	 * 현재 제어중인 Stocker의 PartitionNode 얻기
	 * 
	 * @return String
	 */
	String GetPartitionNode() {
		String strNode = null;
		String strSql = "SELECT Node FROM Partition WHERE Owner IN";
		strSql += " (SELECT DeviceID FROM Device WHERE TSC='" + m_strTSCID + "')";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strNode = rs.getString("Node");
			}
		} catch (SQLException e) {
			String strLog = "GetPartitionNode() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strNode;
	}

	/**
	 * 현재 제어중인 Stocker ID 얻기
	 * 
	 * @return String
	 */
	String GetStockerID() {
		String strStockerID = null;
		String strSql = "SELECT DeviceID FROM Device WHERE TSC='" + m_strTSCID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strStockerID = rs.getString("DeviceID");
			}
		} catch (SQLException e) {
			String strLog = "GetStockerID() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strStockerID;
	}

	/**
	 * 현재 제어중인 Stocker에 대한 대체 Stocker의 Partition Node 정보 얻기
	 * 
	 * @return String
	 */
	String GetValidAltStocker() {
		Vector vtDevList = new Vector();

		// AltDevListName
		String strSql = "SELECT DeviceName FROM AlternateDeviceList WHERE AltListName IN";
		strSql += " (SELECT AltDevListName FROM Device WHERE TSC='" + m_strTSCID + "') ORDER BY Priority ASC";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);

			// Device List 중 Priority 순에 따라 유효한 Device를 검색함.
			if (rs != null) {
				while (rs.next()) {
					vtDevList.add(rs.getString("DeviceName"));
				}
			}
		} catch (SQLException e) {
			String strLog = "GetValidAltNode_Stocker() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		if (vtDevList.size() > 0) {
			int i = 0;
			String strDeviceName = null;
			for (i = 0; i < vtDevList.size(); i++) {
				strDeviceName = (String) vtDevList.get(i);
				if (IsValidStocker(strDeviceName)) {
					return strDeviceName;
				}
			}
		}

		return null;
	}

	/**
	 * Stocker가 반송 가능한 상태인지 여부 확인 TSC 유효성, Partition 유효성 확인
	 * 
	 * @param strStockerName
	 *            String
	 * @return boolean
	 */
	boolean IsValidStocker(String strStockerName) {
		boolean bRet = false;
		String strTSCID = "";

		// Stocker 유효성
		// Enabled=true, Stocker Alarm 발생 여부
		String strSql = "SELECT TSC FROM Device WHERE (DeviceID='" + strStockerName + "') AND (Enabled=1) AND (AlarmList='' OR AlarmList is null)";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
				strTSCID = rs.getString("TSC");
			}
		} catch (SQLException e) {
			String strLog = "IsValidStocker() Stocker 유효성 - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		if (bRet == false) {
			return false;
		}

		// TSC 유효성
		// Enabled=true, TSCStatus=Auto
		bRet = false;
		strSql = "SELECT TSCID FROM TSC WHERE (TSCID='" + strTSCID + "') AND (TSCStatus='AUTO') AND (Enabled=1)";
		rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "IsValidStocker() TSC 유효성 - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		if (bRet == false) {
			return false;
		}

		// Partition 유효성(Manual 출고가 아닌 경우 -> Storage 저장)
		// Enabled=true, CapaStatus<>Full
		bRet = false;
		strSql = "SELECT PartitionID FROM Partition WHERE (Owner='" + strStockerName + "') AND (Enabled=1) AND (CapaStatus<>'FULL')";
		rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "IsValidStocker() Partition 유효성 - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return bRet;
	}

	/**
	 * Host I/F가 반송경로를 재설정하도록 요청함.
	 * 
	 * @param trCmdInfo
	 *            MyHashtable
	 * @param strAltDevice
	 *            String
	 * @param strDestLoc
	 *            String
	 * @param nByWayOf
	 *            int
	 */
	void ReqRouteResearch(MyHashtable trCmdInfo, String strAltDevice, int nByWayOf) {
		// Micro Tr. Cmd Status 변경 -> TempHolded
		String strSql = "UPDATE MicroTrCmd SET Status='TempHolded', StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')";
		strSql += " WHERE MicroTrCmdID='" + trCmdInfo.toString("MicroTrCmdID", 0) + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "ReqRouteResearch() - Exception: " + e.getMessage();
			WriteLog(strLog);
		}

		// 경로 재설정 요청 정보 등록 - UserOperation Table
		CMessage msg = new CMessage();
		msg.SetMessageName("ReqRouteResearch");
		msg.SetMessageItem("MacroTrCmdID", trCmdInfo.toString("MacroTrCmdID", 0), false);
		msg.SetMessageItem("CmdIndex", trCmdInfo.toInt("CmdIndex", 0), false);
		msg.SetMessageItem("AlternateDevice", strAltDevice, false);

		// 2005.12.27 대체 경로가 최종 dest인지 중간경유인지 여부 flag 추가
		msg.SetMessageItem("ByWayOf", nByWayOf, false);
		String strMsg = msg.ToMessage();
		if (IsAlreadyRegisteredUserOperation(strMsg))
			return;

		strSql = "INSERT INTO UserOperation (MsgSender, MsgReceiver, MsgString, CommandTime)";
		strSql += " VALUES ('" + m_strTSCID + "', 'HostIF', '" + msg.ToMessage() + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'))";
		try {
			m_dbFrame.ExecSQL(strSql);
			String strLog = "ReqRouteResearch - " + strSql;
			WriteLog(strLog);
		} catch (SQLException e) {
			String strLog = "ReqRouteResearch() - Exception: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 특정 Stocker에 대해 해당 Stocker로의 반송이 불가할 경우 대체 반송을 하게 되는데, 해당 Stocker가 다시
	 * 반송가능한 상태로 전환되었을 때 대체 Stocker에서 원래 Dest로 재반송을 하는지에 대한 flag값을 얻는 함수
	 * 
	 * @param strDevice
	 *            String
	 * @return int
	 */
	int GetReturnFromAltFlag(String strDevice) {
		int nReturnFromAltFlag = 0;
		String strSql = "SELECT ReturnFromAltFlag FROM Device WHERE DeviceID='" + strDevice + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				nReturnFromAltFlag = rs.getInt("ReturnFromAltFlag");
			}
		} catch (SQLException ex) {
			String strLog = "GetReturnFromAltFlag() - Exception: " + ex.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return nReturnFromAltFlag;
	}

	/**
	 * 1단 이상의 Stocker Port의 인접 Port 명 얻기 LP Port에 대해서는 OP Port명, OP Port에 대해서는
	 * LP Port 명, 1단 Port일 경우 동일명
	 * 
	 * @param strPort
	 *            String
	 * @param bInPort
	 *            boolean
	 * @return String
	 */
	String GetOPPort(String strPort, boolean bInPort) {
		String strOPPort = strPort;
		int nPos = strPort.indexOf(".");
		if (nPos > -1) {
			strOPPort = strPort.substring(0, nPos);
			strOPPort = GetPortName(strOPPort, "OP");
		}
		return strOPPort;
	}

	String GetPortName(String strSubName, String strPortType) {
		String strPortName = "";
		String strSql = "SELECT CarrierLocID FROM CarrierLoc WHERE (CarrierLocID LIKE '%" + strSubName + "%') AND (PortType='" + strPortType + "')";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strPortName = rs.getString("CarrierLocID");
			}
		} catch (SQLException e) {
			String strLog = "GetPortName() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strPortName;
	}

	/**
	 * 1단 이상의 Stocker Port의 인접 Port 명 얻기 LP Port에 대해서는 OP Port명, OP Port에 대해서는
	 * LP Port 명, 1단 Port일 경우 동일명
	 * 
	 * @param strPort
	 *            String
	 * @param bInput
	 *            boolean
	 * @return String
	 */
	String GetLPPort(String strPort, boolean bInput) {
		String strLPPort = strPort;
		int nPos = strPort.indexOf(".");
		if (nPos > -1) {
			strLPPort = strPort.substring(0, nPos);
			strLPPort = GetPortName(strLPPort, "LP");
		}
		return strLPPort;
	}

	/**
	 * 반송 DestLoc의 반송가능 상태 점검
	 * 
	 * @param strDest
	 *            String
	 * @param trCmdInfo
	 *            MyHashtable
	 * @return boolean
	 */
	boolean IsDestPortAvailable(String strDest, MyHashtable trCmdInfo) {
		// OP PortID
		// Stocker Port가 이중 Port일 경우 OP Port의 상태도 확인해야 한다.
		MyHashtable locInfo = new MyHashtable();
		GetCarrierLocInfo(strDest, locInfo);
		boolean bInputPort = false;
		if (locInfo.toString("InOutMode", 0).equals("_IN"))
			bInputPort = true;

		String strOPPort = GetOPPort(strDest, bInputPort);

		// OP, LP Port Full 상태 점검
		if (IsPortFull(strOPPort) || IsPortError(strOPPort) || IsPortError(strDest)) {
			String strAltPort = GetAltSTKPort(strDest);
			if (strAltPort != null && !strAltPort.equals("")) {
				locInfo.clear();
				GetCarrierLocInfo(strAltPort, locInfo);
				bInputPort = false;
				if (locInfo.toString("InOutMode", 0).equals("_IN"))
					bInputPort = true;

				String strAltOPPort = GetOPPort(strAltPort, bInputPort);
				if (IsPortFull(strAltOPPort) || IsPortError(strAltOPPort) || IsPortError(strAltPort)) {
					return false;
				} else {
					// 반송명령 갱신 - DestPort를 변경
					trCmdInfo.put("Dest", strAltPort);
					ChangeDestLoc(trCmdInfo, strAltPort);
					return true;
				}
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	/**
	 * 특정 Micro Tr. Cmd의 DestLoc 및 다음 CmdIndex를 가지는 Micro Tr. Cmd의 SourceLoc을 바꿈
	 * 
	 * @param trCmdInfo
	 *            MyHashtable
	 * @param strNewDest
	 *            String
	 */
	void ChangeDestLoc(MyHashtable trCmdInfo, String strNewDest) {
		String strSql = "UPDATE MicroTrCmd SET DestLoc='" + strNewDest + "'";
		strSql += " WHERE MicroTrCmdID='" + trCmdInfo.toString("MicroTrCmdID", 0) + "'";
		try {
			m_dbFrame.AddBatch(strSql);
		} catch (SQLException e) {
			String strLog = "ChangeDestLoc() - Exception: " + e.getMessage();
			WriteLog(strLog);
		}

		strSql = "UPDATE MicroTrCmd SET SourceLoc='" + strNewDest + "' WHERE MacroTrCmdID='" + trCmdInfo.toString("MacroTrCmdID", 0) + "' AND CmdIndex="
				+ String.valueOf(trCmdInfo.toInt("CmdIndex", 0) + 1);
		try {
			m_dbFrame.AddBatch(strSql);
		} catch (SQLException e) {
			String strLog = "ChangeDestLoc() - Exception: " + e.getMessage();
			WriteLog(strLog);
		}

		try {
			m_dbFrame.ExecBatch();
		} catch (SQLException e) {
			String strLog = "ChangeDestLoc() ExecBatch - Exception: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 유효한 대체 Port 정보 얻기
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return String
	 */
	String GetAltSTKPort(String strCarrierLocID) {
		String strAltLocID = "";

		if (IsValidStocker(GetOwnerDevice(strCarrierLocID))) {
			String strSql = "SELECT * FROM CarrierLoc WHERE OWNER IN";
			strSql += " (SELECT Owner FROM CarrierLoc WHERE CarrierLocID LIKE '%" + strCarrierLocID + "')";

			ResultSet rs = null;
			try {
				rs = m_dbFrame.GetRecord(strSql);
				if (rs != null) {
					while (rs.next()) {
						if (rs.getString("InOutMode").equals("AUTO_IN") && rs.getString("PortType").equals("LP") && rs.getInt("Enabled") == 1 && rs.getString("AlarmList") == null
								&& !rs.getString("CarrierLocID").equals(strCarrierLocID)) {
							strAltLocID = rs.getString("CarrierLocID");
							return strAltLocID;
						}
					}
				}
			} catch (SQLException e) {
				String strLog = "GetAltSTKPort() - Exception: " + e.getMessage();
				WriteLog(strLog);
			} finally {
				if (rs != null) {
					m_dbFrame.CloseRecord(rs);
				}
			}
		}
		return strAltLocID;
	}

	/**
	 * 유효한 대체 Port 정보 얻기
	 * 
	 * @param strTSCID
	 *            String
	 * @param bPartition
	 *            boolean
	 * @return String
	 */
	String GetAltSTK_TSC(String strTSCID, boolean bPartition) {
		String strAltLoc = "";
		String strSql = "";

		Vector vtDevList = new Vector();

		// 2005.09.09 bPartition여부에 상관없이 Priority 순으로 정보를 얻어 처리하도록 변경
		strSql = "SELECT DeviceName FROM AlternateDeviceList WHERE AltListName IN";
		strSql += " (SELECT AltDevListName FROM TSC WHERE TSCID='" + strTSCID + "') ORDER BY Priority ASC";

		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);

			// Device List 중 Priority 순에 따라 유효한 Device를 검색함.
			if (rs != null) {
				while (rs.next()) {
					vtDevList.add(rs.getString("DeviceName"));
				}
			}
		} catch (SQLException e) {
			String strLog = "GetAltSTK_TSC() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		if (vtDevList.size() > 0) {
			int i = 0;
			String strDeviceName = null;
			for (i = 0; i < vtDevList.size(); i++) {
				strDeviceName = (String) vtDevList.get(i);
				if (IsValidStocker(strDeviceName)) {
					if (bPartition) {
						return GetSTKPartition(strDeviceName);
					} else {
						return strDeviceName;
					}
				} else {
					WriteLog("IsValidStocker Fail!");
				}
			}
			WriteLog("GetAltSTK_TSC Fail!");
			return "";
		}

		return strAltLoc;
	}

	/**
	 * Partition의 Capa Status 정보 : NORMAL, EMPTY, FULL
	 * 
	 * @param strPartition
	 *            String
	 * @return String
	 */
	String GetCapaStatus(String strPartition) {
		String strCapaStatus = "NORMAL";
		String strSql = "SELECT CapaStatus FROM Partition WHERE PartitionID='" + strPartition + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strCapaStatus = rs.getString("CapaStatus");
			}
		} catch (SQLException e) {
			String strLog = "GetCapaStatus() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strCapaStatus;
	}

	/**
	 * CarrierLoc Type 정보 : SHELF, AUTO_IN, AUTO_OUT, MANUAL_IN, MANUAL_OUT
	 * 
	 * @param strLoc
	 *            String
	 * @return String
	 */
	String GetLocType(String strLoc) {
		// Loc Type : SHELF, AUTO_IN, AUTO_OUT, MANUAL_IN, MANUAL_OUT
		String strLocType = "";
		String strSql = "SELECT InOutMode FROM CarrierLoc WHERE CarrierLocID='" + strLoc + "' AND Type<>'SHELF'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strLocType = rs.getString("InOutMode");
			}
		} catch (SQLException e) {
			String strLog = "GetLocType() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strLocType;
	}

	/**
	 * Carrier의 현재 위치 정보
	 * 
	 * @param strCarrierID
	 *            String
	 * @return String
	 */
	String GetCurrLoc(String strCarrierID) {
		String strCurrLoc = "";
		String strSql = "SELECT CurrLoc FROM Carrier WHERE CarrierID='" + strCarrierID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strCurrLoc = rs.getString("CurrLoc");
			}
		} catch (SQLException e) {
			String strLog = "GetCurrLoc() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strCurrLoc;
	}

	/**
	 * Carrier의 현재 위치 정보
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return String
	 */
	String GetNode(String strCarrierLocID) {
		String strNode = "";
		String strSql = "SELECT Node FROM CarrierLoc WHERE CarrierLocID='" + strCarrierLocID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strNode = rs.getString("Node");
			}
		} catch (SQLException e) {
			String strLog = "GetNode() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strNode;
	}

	/**
	 * 반송명령의 SourceLoc 또는 DestLoc이 Partition인지 여부
	 * 
	 * @param strLoc
	 *            String
	 * @return boolean
	 */
	boolean IsPartition(String strLoc) {
		boolean bRet = false;
		String strSql = "SELECT PartitionID FROM Partition WHERE PartitionID='" + strLoc + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				bRet = true;
			}
		} catch (SQLException e) {
			String strLog = "IsPartition() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return bRet;
	}

	/**
	 * 현재 OCS가 반송 진행 중인지 여부 판단
	 * 
	 * @return boolean
	 */
	boolean IsOnTransferring() {
		String strSql = "SELECT MicroTrCmdID FROM MicroTrCmd WHERE TSC='" + m_strTSCID + "'";
		strSql += " AND (Status<>'NONE' AND Status<>'READY' AND Status<>'FAILED'";
		strSql += " AND Status<>'TransferCompleted' AND Status<>'TransferCancelCompleted'";
		strSql += " AND Status<>'TransferAbortCompleted' AND Status<>'Suspended' AND Status<>'TempHolded')";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				return true; // 현재 진행중인 반송명령이 있음.
			}
		} catch (SQLException e) {
			String strLog = "IsOnTransferring() - SQLException : " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return false;
	}

	/**
	 * 경로 Port변경을 위해 DB Table에 정보 변경
	 * 
	 * @param msg
	 *            MyHashtable
	 * @param strAlt
	 *            String
	 */
	void ReqRoutePortChange(MyHashtable msg, String strAlt) {
		String strMicroTrCmdID = msg.toString("MicroTrCmdID", 0);
		String strSql = "UPDATE MicroTrCmd SET Status='READY', DestLoc='" + strAlt + "'";
		strSql += " WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "ReqRoutePortChange() - Exception: " + e.getMessage();
			WriteLog(strLog);
		}

		String strMacroTrCmdID = msg.toString("MacroTrCmdID", 0);
		int nCmdIndex = msg.toInt("CmdIndex", 0);
		strSql = "UPDATE MicroTrCmd SET SourceLoc='" + GetOPName(strAlt) + "'";
		strSql += " WHERE MacroTrCmdID='" + strMacroTrCmdID + "' AND CmdIndex=" + String.valueOf(nCmdIndex + 1);

		try {
			m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "ReqRoutePortChange() - Exception: " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * 경로 Port변경을 위해 DB Table에 정보 변경
	 * 
	 * @param strAlt
	 *            String
	 * @return String
	 */
	String GetOPName(String strAlt) {
		String strOPName = strAlt;
		int nPos = strAlt.indexOf(".LP");
		if (nPos > -1) {
			strOPName = strAlt.substring(0, nPos);
		}

		String strSql = "SELECT CarrierLocID FROM CarrierLoc WHERE CarrierLocID LIKE '%";
		strSql += strOPName + "%' AND PortType='OP'";
		ResultSet rs = null;

		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strOPName = rs.getString("CarrierLocID");
			}
		} catch (SQLException e) {
			String strLog = "GetOPName() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
		return strOPName;
	}

	/**
	 * 반송명령의 대체 반송 STK 정보
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return String
	 */
	String GetAltSTK(String strCarrierLocID) {
		// CarrierLocID로 부터 대체 STK 정보를 찾는다.
		String strAltSTK = "";
		String strSql = "SELECT DeviceName FROM AlternateDeviceList WHERE AltListName IN";
		strSql += " (SELECT AltDevListName FROM Device WHERE DeviceID IN";
		strSql += " (SELECT Owner FROM CarrierLoc WHERE CarrierLocID LIKE '%" + strCarrierLocID + "')) ORDER BY Priority ASC";

		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if (rs != null) {
				while (rs.next()) {
					strAltSTK = rs.getString("DeviceName");
					if (IsValidStocker(strAltSTK) == true) {
						return strAltSTK;
					}
				}
			}
		} catch (SQLException e) {
			String strLog = "GetAltSTK() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strAltSTK;
	}

	/**
	 * 반송명령의 Cmd Index 정보
	 * 
	 * @param strMicroTrCmdID
	 *            String
	 * @return int
	 */
	int GetCmdIndex(String strMicroTrCmdID) {
		int nCmdIndex = -1;
		String strSql = "SELECT CmdIndex FROM MicroTrCmd WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.last())) {
				nCmdIndex = rs.getInt("CmdIndex");
			}
		} catch (SQLException e) {
			String strLog = "GetCmdIndex() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return nCmdIndex;
	}

	/**
	 * Port에 예약된 Carrier 개수 정보
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return String
	 */
	int GetCarrierQtyInPort(String strCarrierLocID) {
		int nCarrierQty = 0;
		String strSql = "SELECT CarrierID FROM Carrier WHERE DestLoc='" + strCarrierLocID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.last())) {
				nCarrierQty = rs.getRow();
			}
		} catch (SQLException e) {
			String strLog = "GetCarrierQtyInPort() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return nCarrierQty;
	}

	/**
	 * TSC에 예약된 Carrier 개수 정보
	 * 
	 * @return String
	 */
	int GetTSCWorkQCapa() {
		int nWorkQCapa = 0;
		String strSql = "SELECT WorkQCapa FROM TSC WHERE TSCID='" + m_strTSCID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.last())) {
				nWorkQCapa = rs.getInt("WorkQCapa");
			}
		} catch (SQLException e) {
			String strLog = "GetTSCWorkQCapa() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return nWorkQCapa;
	}

	/**
	 * CarrierLocID의 Type의 정보
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return String
	 */
	String GetCarrierLocType(String strCarrierLocID) {
		String strType = "";
		String strSql = "SELECT Type FROM CarrierLoc WHERE CarrierLocID='" + strCarrierLocID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strType = rs.getString("TYPE");
			} else if (strCarrierLocID.indexOf("STORAGE") > -1) {
				strType = "SHELF";
			}
		} catch (SQLException e) {
			String strLog = "GetCarrierLocType() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strType;
	}

	/**
	 * Dest의 AlarmList 정보
	 * 
	 * @param strCarrierLocID
	 *            String
	 * @return String
	 */
	String GetAlarmList(String strCarrierLocID) {
		String strAlarmList = "";
		String strSql = "SELECT AlarmList FROM CarrierLoc WHERE CarrierLocID='" + strCarrierLocID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strAlarmList = rs.getString("AlarmList");
			}
		} catch (SQLException e) {
			String strLog = "GetAlarmList() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strAlarmList;
	}

	/**
	 * Carrier의 DestLoc 다음의 이동 위치 정보
	 * 
	 * @param strCarrierID
	 *            String
	 * @return String
	 */
	String GetNextLoc(String strCarrierID) {
		String strNextLoc = "";
		String strSql = "SELECT NextLoc FROM Carrier WHERE CarrierID='" + strCarrierID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strNextLoc = rs.getString("NextLoc");
			}
		} catch (SQLException e) {
			String strLog = "GetNextLoc() - Exception: " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}

		return strNextLoc;
	}

	void GetTSCInfo(MyHashtable tscInfo) {
		String strSql = "SELECT * FROM TSC WHERE TSCID='" + m_strTSCID + "'";
		ResultSet rs = null;
		try {
			rs = m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				tscInfo.put("CommunicationStatus", rs.getString("CommunicationStatus"));
				tscInfo.put("ControlStatus", rs.getString("ControlStatus"));
				tscInfo.put("TSCStatus", rs.getString("TSCStatus"));
			}
		} catch (SQLException e) {
			String strLog = "GetTSCInfo() - SQLException : " + e.getMessage();
			WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_dbFrame.CloseRecord(rs);
			}
		}
	}

	/**
	 * 비정상 반송 처리
	 */
	void MakeTempHoldedCmdToReady() {
		if (m_nTempHoldedTimeout == 0) {
			m_nTempHoldedTimeout = 60;
		}

		// TempHolded 후 TempHoldedTimeout 이상 소요된 반송명령 상태 변경 -> READY
		String strTempHoldedTimeout = String.valueOf(m_nTempHoldedTimeout) + "/24/60/60";
		String strPrevTime = "TO_CHAR(SYSDATE-" + strTempHoldedTimeout + ", 'YYYYMMDDHH24MISS')";
		String strSql = "UPDATE MicroTrCmd SET Status='READY', StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), TrReadyTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE (Status='TempHolded') AND (StatusChangedTime<"
				+ strPrevTime + ") AND (TSC='" + m_strTSCID + "')";

		try {
			m_dbFrame.ExecSQL_NotFailGuaranteed(strSql);
		} catch (SQLException e) {
			String strLog = "MakeTempHoldedCmdToReady() - SQLException : " + e.getMessage();
			WriteLog(strLog);
		}
	}

	void MakeAbnormalTrCmdToReady() {
		// FAIL 또는 IFQUEUED 후 1분 이상 소요된 반송명령 상태 변경 -> READY
		String strTimeout = String.valueOf(60) + "/24/60/60";
		String strPrevTime = "TO_CHAR(SYSDATE-" + strTimeout + ", 'YYYYMMDDHH24MISS')";
		// 2005.10.08 CANCELQUEUED, ABORTQUEUED상태인 경우는 제외시킴(CANCELQUEUED는 애매하지만 수정했고 ABORTQUEUED는 제거필요)
		//    String strSql = "UPDATE MicroTrCmd SET Status='READY', StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), TrReadyTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE (Status='FAIL' OR Status='IFQUEUED' OR Status='ABORTQUEUED' OR Status='CANCELQUEUED') AND (StatusChangedTime<" + strPrevTime + ") AND (TSC='" + m_strTSCID + "')";
		String strSql = "UPDATE MicroTrCmd SET Status='READY', StatusChangedTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), TrReadyTime=TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS') WHERE (Status='FAIL' OR Status='IFQUEUED') AND (StatusChangedTime<"
				+ strPrevTime + ") AND (TSC='" + m_strTSCID + "')";
		try {
			m_dbFrame.ExecSQL_NotFailGuaranteed(strSql);
		} catch (SQLException e) {
			String strLog = "MakeAbnormalTrCmdToReady() - SQLException : " + e.getMessage();
			WriteLog(strLog);
		}
	}

	/**
	 * Log 기록
	 * 
	 * @param strLog
	 *            String
	 */
	void WriteLog(String strLog) {
		//System.out.println(strLog);
		//    Util.WriteReturnLog("OCSManager", strLog, "", true);
	}

	public FileLongRunManager getFileLongRunManager() {
		return this.flrm;
	}

	public LongRunManager getLongRunManager() {
		return this.lrm;
	}

	public void DisplayUserLongRunLogInText(String strLog) {
		if (m_Owner.jUserLongrunText.getLineCount() > 1000)
			m_Owner.jUserLongrunText.setText("");
		m_Owner.jUserLongrunText.append(strLog + "\n");
	}

	/**
	 * 2014.03.19 by MYM
	 * 
	 * @return
	 */
	public boolean isAutoDestChange() {
		return m_Owner.jAutoDestChange.isSelected();
	}
}
