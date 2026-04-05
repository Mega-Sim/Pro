package ocsmanager;

//import javax.swing.*;
import java.text.*;
import java.util.*;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sec.iesw.mcs.infra.common.ucom.IUComEventListener;
import com.sec.iesw.mcs.infra.common.ucom.UCom;
import com.sec.iesw.mcs.infra.common.ucom.UComMsg;

/**
 * STBC Test를 위한 MCSEmulator 구현클래스
 * 
 * IBSEMIF 클래스를 참조함 (차이점)
 * 1. Startup Senario : CEID,VID,ReportID 부분 수정필요 (S1F3/4, S2F49)
 * 2. S2F41 / S2F49 명령수정필요 : 메시지구조, 내용
 * @author yk09.kang
 *
 */
public class STBSEMIF implements IUComEventListener, SEMIF {

	OCSManagerMain m_ocsMain = null;

	UCom m_UCom;

	// ///////////////////////////////////////
	String m_strTSCName = "";
	String m_strEqpName = "";
	String m_strHSMSState = "";
	String m_strCommState = "";
	String m_strSCState = "";
	String m_strAlarmState = "";
	String m_strSVID = "";
	String m_strCurrentTrCmdID = "";
	String m_strCurrentCarrierID = "";
	String m_strCurrentTrCmdType = "";
	String m_strGetEnhancedDataState = "";
	String m_strAlarmID = "";
	String m_strAlarmText = "";
	String m_strAlarmCode = "";
	boolean m_bAutoInitialize = true;

	// 2013.01.04 by KYK
	CarrierLocDAO carrierLocDao;

	// Related to DataManager Temp Memory
	public MyHashtable m_pTempRecordInfo = null;
	public MyHashtable m_pPortListInfo = null;
	public int m_nDeviceID = 0;
	public boolean m_bS2F33DeleteCompleted = false;
	public boolean m_bS2F37DisableCompleted = false;
	public boolean m_bS5F3EnableAllCompleted = false;
	public String m_strControlState = "";
	public String m_strOnlineSequenceState = "";
	public boolean m_bResponse = false;

	/** m_strSEMFormat - S6F11, S1F17의 포맷 Daifuku와 Brooks에 맞게 사용 */
	public String m_strSEMFormat = "";

	// SD Operation Multi 대응
	MsgVector m_pSDOperationList;

	// Initialize 재개
	String m_strConfigFileName = "";

	boolean m_bCheckDisConnection;

	// Timer
	java.util.Timer S1F13Timer = new java.util.Timer();
	S1F13TimerProc S1F13TimerTask = new S1F13TimerProc();

	// Timer
	java.util.Timer ConnectionCheckTimer = new java.util.Timer();

	ConnectionCheckTimerProc ConnectionCheckTimerTask = new ConnectionCheckTimerProc();

	public STBSEMIF(OCSManagerMain ocsMain) {

		m_ocsMain = (OCSManagerMain) ocsMain;
		m_strConfigFileName = ocsMain.m_strIBSEMConfigFileName;

		// 2013.01.04 by KYK : OCSDB에서 데이터를 가져온다.
		initializeOcsData();

		try {
			jbInit();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 2013.01.04 by KYK : OCSDB에서 데이터를 가져온다. OHTController.ini
	 */
	private void initializeOcsData() {
		carrierLocDao = new CarrierLocDAO();
	}

	public boolean isDBConnected() {
		if (carrierLocDao.getConnection() != null) {
			return true;
		}
		return false;
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
				if (sLine.indexOf("SEMFormat") == 0) {
					nPos = sLine.indexOf("=");
					m_strSEMFormat = sLine.substring(nPos + 1);
					m_strSEMFormat = m_strSEMFormat.trim();
				}
			}
			raf.close();
		} catch (IOException e) {
			String strLog = "LoadConfig - IOException: " + e.getMessage();
			// System.out.println(strLog);
			m_ocsMain.WriteLog(strLog);
			return false;
		}
		return true;
	}

	private void jbInit() throws Exception {

		m_strHSMSState = "TCPIP_NOT_CONNECTED";
		m_strCommState = "COMM_NONE";
		m_strControlState = "CONTROL_NONE";
		m_strSCState = "TSC_NONE";
		m_strAlarmState = "NO_ALARMS";

		SetSEMStatus();

		m_pTempRecordInfo = new MyHashtable();
		m_pPortListInfo = new MyHashtable();

		m_strOnlineSequenceState = "INIT";
		m_bS2F33DeleteCompleted = false;
		m_bS2F37DisableCompleted = false;
		m_bS5F3EnableAllCompleted = false;
		m_strSVID = "73";
		m_bResponse = true;

		// Read SEMFormat
		LoadConfig();

		Initialize();
	}

	/**
	 * 외부 Dialog 또는 Frame이 호출하는 Function. Msg 변수를 Parsing하여 기능별 Function 재호출
	 *
	 * @param msg
	 *            MyHashtable
	 * @return int
	 */
	public int CallProc(MyHashtable msg) {
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
			m_ocsMain.WriteLog(strMsg);
		}

		String strMessageName = msg.toString("MessageName", 0);

		if (strMessageName == "ReqTSCControlStatusChange") {
			ReqTSCControlStatusChange(msg); //
		} else if (strMessageName == "ReqTSCStatusChange") {
			ReqTSCStatusChange(msg);
		} else if (strMessageName == "SendMicroTC") {
			SendMicroTC(msg);
		} else if (strMessageName == "ReqSDOperation") {
			ReqSDOperation(msg);
		} else if (strMessageName == "ReqPortModeChange") {
			ReqPortModeChange(msg);
		} else if (strMessageName == "GetTSCStatus") {
			GetTSCStatus(msg);
		} else if (strMessageName == "GetEnhancedCarrierInfo") {
			GetEnhancedCarrierInfo(msg);
		} else if (strMessageName == "GetEnhancedTrCmd") {
			GetEnhancedTrCmd(msg);
		} else if (strMessageName == "GetEnhancedStockerUnit") {
			GetEnhancedStockerUnit(msg);
		} else if (strMessageName == "GetVehicleInfo") {
			GetVehicleInfo(msg);
		} else {
			Display("[Error         ] Undefined MessageName");
		}
		return nRet;
	}

	void ReqTSCControlStatusChange(MyHashtable pMessageInfo) {
		String strReceivedMessage, strTSC, strControlStatus, strSFMsg;
		boolean b_TSCExist = false;
		int nReturn;

		// 1. Get Items
		strTSC = pMessageInfo.toString("TSC", 0);
		strControlStatus = pMessageInfo.toString("ControlStatus", 0);

		// ///////////////////////////// func-import from operation timer
		// 2. Send Message To related SCS
		if (strControlStatus.equals("ONLINE") == true) {
			nReturn = SendMsg("S1F13");
			if (nReturn == 0)
				SendMsg("S1F17");
		} else if (strControlStatus.equals("OFFLINE") == true) {
			SendMsg("S1F15");
		} else {
			Display("Undefined ControlStatus!!");
		}

		// ///////////////////////////// sub-import end
		// 2. Set Log
		Display("[SEM I/F <- D.M] ReqTSCControlStatusChange(" + strTSC + "/" + strControlStatus + ")");
		// // Log("SEM I/F <- D.M><P> " + pMessageInfo.ToMessage());

	}

	// ---------------------------------------------------------------------------
	void ReqTSCStatusChange(MyHashtable pMessageInfo) {

		String strReceivedMessage, strTSC, strTSCStatus, strSFMsg;
		MyHashtable pMessage = new MyHashtable();

		// 1. Get Items
		strTSC = pMessageInfo.toString("TSC", 0);
		strTSCStatus = pMessageInfo.toString("TSCStatus", 0);

		if (strTSCStatus.equals("AUTO") == true) {

			pMessage.put("RCMD", "RESUME");
			SendS2F41(pMessage);
		} else if (strTSCStatus.equals("PAUSE") == true) {
			pMessage.put("RCMD", "PAUSE");
			SendS2F41(pMessage);
		} else {
			Display("Undefined TSCStatus!!");
		}

		// 2. Set Log
		strReceivedMessage = "TSC:" + strTSC;
		strReceivedMessage += "/ TSCStatus:" + strTSCStatus;

		Display("[SEM I/F <- D.M] ReqTSCStatusChange(" + strTSC + "/" + strTSCStatus + ")");

	}

	// ---------------------------------------------------------------------------
	public void SendMicroTC(MyHashtable pMessageInfo) {

		String strReceivedMessage, strTSC, strMicroTrCmdID, strMicroTrCmdType, strCarrierID, strCarrierLocID, strSource, strDest, strLotID, strErrorID, strInstallTime;
		int nPriority, nReplace, nEmptyCarrier, nFLOORNUMBER = 0, nReturnCode = 0;

		// 1. Get MicroTrCmd Items
		strTSC = pMessageInfo.toString("TSC", 0);
		strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);
		strMicroTrCmdType = pMessageInfo.toString("MicroTrCmdType", 0);
		strCarrierID = pMessageInfo.toString("CarrierID", 0);
		strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);
		strSource = pMessageInfo.toString("Source", 0);
		strDest = pMessageInfo.toString("Dest", 0);
		nPriority = pMessageInfo.toInt("Priority", 0);
		nReplace = pMessageInfo.toInt("Replace", 0);
		nEmptyCarrier = pMessageInfo.toInt("EmptyCarrier", 0);
		strLotID = pMessageInfo.toString("LotID", 0);
		strErrorID = pMessageInfo.toString("ErrorID", 0);
		nFLOORNUMBER = pMessageInfo.toInt("FLOORNUMBER", 0);

		// ///////////////////////////////////////////////////////
		MyHashtable pMessage = new MyHashtable();

		if (strMicroTrCmdType.equals("TRANSFER") == true) {
			// 1. Get TRCMD Item
			strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);
			strCarrierID = pMessageInfo.toString("CarrierID", 0);
			strSource = pMessageInfo.toString("Source", 0);
			strDest = pMessageInfo.toString("Dest", 0);
			nPriority = pMessageInfo.toInt("Priority", 0);
			nReplace = pMessageInfo.toInt("Replace", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", "TRANSFER");

			pMessage.put("COMMANDID", strMicroTrCmdID);
			pMessage.put("PRIORITY", new Integer(nPriority));
			pMessage.put("REPLACE", new Integer(nReplace));
			pMessage.put("CARRIERID", strCarrierID);
			pMessage.put("LOTID", strLotID);

			// 2008.12.04 by MYM : Source or Dest Port를 바로 사용할 수 있도록 수정(OCS IBSEM Emul용)
			//      pMessage.put("SOURCE", ChangeToSEMFormat(strSource));
			//      pMessage.put("DEST", ChangeToSEMFormat(strDest));
			pMessage.put("SOURCE", strSource);
			pMessage.put("DEST", strDest);

			// 3. Send to SCS
			m_strCurrentTrCmdID = strMicroTrCmdID;
			m_strCurrentCarrierID = strCarrierID;
			SendS2F49(pMessage);

			// SendTRCMDTimer.stop(); //.Enabled = false;
		}
		// 2009.07.21 by MYM : Stage 명령 전송
		else if (strMicroTrCmdType.equals("STAGE") == true) {
			strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);
			strCarrierID = pMessageInfo.toString("CarrierID", 0);
			strSource = pMessageInfo.toString("Source", 0);
			strDest = pMessageInfo.toString("Dest", 0);
			nPriority = pMessageInfo.toInt("Priority", 0);
			nReplace = pMessageInfo.toInt("Replace", 0);

			int nExpectedDuration = pMessageInfo.toInt("EXPECTEDDURATION", 0);
			int nNoBlockingTime = pMessageInfo.toInt("NOBLOCKINGTIME", 0);
			int nWaitTimeOut = pMessageInfo.toInt("WAITTIMEOUT", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", "STAGE");
			pMessage.put("STAGEID", strMicroTrCmdID);
			pMessage.put("PRIORITY", new Integer(nPriority));
			pMessage.put("REPLACE", new Integer(nReplace));
			pMessage.put("EXPECTEDDURATION", new Integer(nExpectedDuration));
			pMessage.put("NOBLOCKINGTIME", new Integer(nNoBlockingTime));
			pMessage.put("WAITTIMEOUT", new Integer(nWaitTimeOut));

			pMessage.put("CARRIERID", strCarrierID);
			pMessage.put("SOURCE", strSource);
			pMessage.put("DEST", strDest);

			SendS2F49ForStage(pMessage);
		} else if (strMicroTrCmdType.equals("SCAN") == true) // 2009.10.12 by IKY : Scan 명령
		{
			// 1. Get Scan Item
			strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);
			strCarrierID = pMessageInfo.toString("CarrierID", 0);
			strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);
			nReplace = pMessageInfo.toInt("Replace", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", "SCAN");

			pMessage.put("COMMANDID", strMicroTrCmdID);
			pMessage.put("PRIORITY", new Integer(nPriority));
			pMessage.put("CARRIERID", strCarrierID);
			pMessage.put("CARRIERLOCID", strCarrierLocID);

			// 3. Send
			SendS2F49ForScan(pMessage);
		} else if ((strMicroTrCmdType.equals("CANCEL") == true) || (strMicroTrCmdType.equals("ABORT") == true)) {
			// 1. Get TRCMD Item
			strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", strMicroTrCmdType);

			pMessage.put("COMMANDID", strMicroTrCmdID);

			// 3. Send to SCS
			m_strCurrentTrCmdID = strMicroTrCmdID;
			m_strCurrentTrCmdType = strMicroTrCmdType;
			SendS2F41(pMessage);
			// SendTRCMDTimer.stop(); //.Enabled = false;
		} else if (strMicroTrCmdType.equals("RETRY") == true) {
			// 1. Get TRCMD Item
			strErrorID = pMessageInfo.toString("ErrorID", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", strMicroTrCmdType);

			pMessage.put("ERRORID", strErrorID);

			// 3. Send to SCS
			SendS2F41(pMessage);
			m_strCurrentTrCmdType = strMicroTrCmdType;
			// SendTRCMDTimer.stop(); //);.Enabled = false;
		}
		// 2021.03.30 dahye : Transfer_Ex4
		else if (strMicroTrCmdType.equals("TRANSFER_Ex4") == true) {
			// 1. Get TRCMD Item
			strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);
			nPriority = pMessageInfo.toInt("Priority", 0);
			nReplace = pMessageInfo.toInt("Replace", 0);
			strCarrierID = pMessageInfo.toString("CarrierID", 0);
			strSource = pMessageInfo.toString("Source", 0);
			strDest = pMessageInfo.toString("Dest", 0);
			String strDeliveryType = pMessageInfo.toString("DELIVERYTYPE", 0);
			int nExpectedDeliveryTime = pMessageInfo.toInt("EXPECTEDDELIVERYTIME", 0);
			int nDeliveryWaitTimeout = pMessageInfo.toInt("DELIVERYWAITTIMEOUT", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", "TRANSFER_EX4");

			pMessage.put("COMMANDID", strMicroTrCmdID);
			pMessage.put("PRIORITY", new Integer(nPriority));
			pMessage.put("REPLACE", new Integer(nReplace));

			pMessage.put("CARRIERID", strCarrierID);
			pMessage.put("SOURCE", strSource);
			pMessage.put("DEST", strDest);

			pMessage.put("DELIVERYTYPE", strDeliveryType);
			pMessage.put("EXPECTEDDELIVERYTIME", new Integer(nExpectedDeliveryTime));
			pMessage.put("DELIVERYWAITTIMEOUT", new Integer(nDeliveryWaitTimeout));

			// 3. Send
			SendS2F49ForEx4(pMessage);
		} else {
			nReturnCode = -1;
			Display("Undefined TRCMD!! ReturnCode : " + nReturnCode);
		}

		// 2004.01.08 Added by N.Y.K

		// 8. Set Log
		Display("[SEM I/F <- D.M] SendMicroTC(" + strTSC + "/" + strMicroTrCmdType + "/" + strCarrierID + "/" + strSource + "/" + strDest + ")");
	}

	/**
	 * Stage 전송 후 이에 대한 Transfer 명령 전송시 사용
	 *
	 * @param pMessageInfo
	 *            MyHashtable
	 * @version Created by MYM 2009.09.10
	 */
	public void SendMicroTCForStage(MyHashtable pMessageInfo) {

		String strReceivedMessage, strTSC, strMicroTrCmdID, strMicroTrCmdType, strCarrierID, strCarrierLocID, strSource, strDest, strLotID, strErrorID, strInstallTime;
		int nPriority, nReplace, nEmptyCarrier, nFLOORNUMBER = 0, nReturnCode = 0;

		// 1. Get MicroTrCmd Items
		strTSC = pMessageInfo.toString("TSC", 0);
		strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);
		strMicroTrCmdType = pMessageInfo.toString("MicroTrCmdType", 0);
		strCarrierID = pMessageInfo.toString("CarrierID", 0);
		strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);
		strSource = pMessageInfo.toString("Source", 0);
		strDest = pMessageInfo.toString("Dest", 0);
		nPriority = pMessageInfo.toInt("Priority", 0);
		nReplace = pMessageInfo.toInt("Replace", 0);
		nEmptyCarrier = pMessageInfo.toInt("EmptyCarrier", 0);
		strLotID = pMessageInfo.toString("LotID", 0);
		strErrorID = pMessageInfo.toString("ErrorID", 0);
		nFLOORNUMBER = pMessageInfo.toInt("FLOORNUMBER", 0);

		// ///////////////////////////////////////////////////////
		MyHashtable pMessage = new MyHashtable();

		if (strMicroTrCmdType.equals("TRANSFER") == true) {
			// 1. Get TRCMD Item
			strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);
			strCarrierID = pMessageInfo.toString("CarrierID", 0);
			strSource = pMessageInfo.toString("Source", 0);
			strDest = pMessageInfo.toString("Dest", 0);
			nPriority = pMessageInfo.toInt("Priority", 0);
			nReplace = pMessageInfo.toInt("Replace", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", "TRANSFER");

			pMessage.put("COMMANDID", strMicroTrCmdID);
			pMessage.put("PRIORITY", new Integer(nPriority));
			pMessage.put("REPLACE", new Integer(nReplace));
			pMessage.put("CARRIERID", strCarrierID);
			pMessage.put("LOTID", strLotID);

			// 2008.12.04 by MYM : Source or Dest Port를 바로 사용할 수 있도록 수정(OCS IBSEM Emul용)
			//      pMessage.put("SOURCE", ChangeToSEMFormat(strSource));
			//      pMessage.put("DEST", ChangeToSEMFormat(strDest));
			pMessage.put("SOURCE", strSource);
			pMessage.put("DEST", strDest);

			// 3. Send to SCS
			m_strCurrentTrCmdID = strMicroTrCmdID;
			m_strCurrentCarrierID = strCarrierID;
			SendS2F49TrForStage(pMessage);

			// SendTRCMDTimer.stop(); //.Enabled = false;
		}
		// 2009.07.21 by MYM : Stage 명령 전송
		else if (strMicroTrCmdType.equals("STAGE") == true) {
			strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);
			strCarrierID = pMessageInfo.toString("CarrierID", 0);
			strSource = pMessageInfo.toString("Source", 0);
			strDest = pMessageInfo.toString("Dest", 0);
			nPriority = pMessageInfo.toInt("Priority", 0);
			nReplace = pMessageInfo.toInt("Replace", 0);

			int nExpectedDuration = pMessageInfo.toInt("EXPECTEDDURATION", 0);
			int nNoBlockingTime = pMessageInfo.toInt("NOBLOCKINGTIME", 0);
			int nWaitTimeOut = pMessageInfo.toInt("WAITTIMEOUT", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", "STAGE");
			pMessage.put("STAGEID", strMicroTrCmdID);
			pMessage.put("PRIORITY", new Integer(nPriority));
			pMessage.put("REPLACE", new Integer(nReplace));
			pMessage.put("EXPECTEDDURATION", new Integer(nExpectedDuration));
			pMessage.put("NOBLOCKINGTIME", new Integer(nNoBlockingTime));
			pMessage.put("WAITTIMEOUT", new Integer(nWaitTimeOut));

			pMessage.put("CARRIERID", strCarrierID);
			pMessage.put("SOURCE", strSource);
			pMessage.put("DEST", strDest);

			SendS2F49ForStage(pMessage);
		} else if ((strMicroTrCmdType.equals("CANCEL") == true) || (strMicroTrCmdType.equals("ABORT") == true)) {
			// 1. Get TRCMD Item
			strMicroTrCmdID = pMessageInfo.toString("MicroTrCmdID", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", strMicroTrCmdType);

			pMessage.put("COMMANDID", strMicroTrCmdID);

			// 3. Send to SCS
			m_strCurrentTrCmdID = strMicroTrCmdID;
			m_strCurrentTrCmdType = strMicroTrCmdType;
			SendS2F41(pMessage);
			// SendTRCMDTimer.stop(); //.Enabled = false;
		} else if (strMicroTrCmdType.equals("RETRY") == true) {
			// 1. Get TRCMD Item
			strErrorID = pMessageInfo.toString("ErrorID", 0);

			// 2. Change to IBSEM Form
			pMessage.put("RCMD", strMicroTrCmdType);

			pMessage.put("ERRORID", strErrorID);

			// 3. Send to SCS
			SendS2F41(pMessage);
			m_strCurrentTrCmdType = strMicroTrCmdType;
			// SendTRCMDTimer.stop(); //);.Enabled = false;
		} else {
			nReturnCode = -1;
			Display("Undefined TRCMD!! ReturnCode : " + nReturnCode);
		}

		// 2004.01.08 Added by N.Y.K

		// 8. Set Log
		Display("[SEM I/F <- D.M] SendMicroTC(" + strTSC + "/" + strMicroTrCmdType + "/" + strCarrierID + "/" + strSource + "/" + strDest + ")");
	}

	// ---------------------------------------------------------------------------
	void ReqSDOperation(MyHashtable pMessageInfo) {
		String strReceivedMessage, strTSC, strOperationType, strCarrierID, strLotID, strCarrierLocID, strEmptyFlag;
		int nPriority;

		boolean b_TSCExist = false;

		// 1. Get MicroTrCmd Items
		strTSC = pMessageInfo.toString("TSC", 0);
		strOperationType = pMessageInfo.toString("OperationType", 0);
		strCarrierID = pMessageInfo.toString("CarrierID", 0);
		strLotID = pMessageInfo.toString("LotID", 0);
		strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);
		strEmptyFlag = pMessageInfo.toString("EmptyFlag", 0);

		// ///////////////////////////

		MyHashtable pMessage = new MyHashtable();

		if (strOperationType.equals("INSTALL") == true) {
			// Get Install Info
			strCarrierID = pMessageInfo.toString("CarrierID", 0);
			strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);

			// Set Install Info
			pMessage.put("RCMD", "INSTALL");
			pMessage.put("CARRIERID", strCarrierID);
			pMessage.put("CARRIERLOC", ChangeToSEMFormat(strCarrierLocID));

			SendS2F41(pMessage);
		} else if (strOperationType.equals("REMOVE") == true) {
			// Get Remove Info
			strCarrierID = pMessageInfo.toString("CarrierID", 0);

			// Set Remove Info
			pMessage.put("RCMD", "REMOVE");
			pMessage.put("CARRIERID", strCarrierID);

			SendS2F41(pMessage);

		} else if (strOperationType.equals("LOCATE") == true) {
			// Get Locate Info
			strCarrierID = pMessageInfo.toString("CarrierID", 0);

			// Set Locate Info
			pMessage.put("RCMD", "LOCATE");
			pMessage.put("CARRIERID", strCarrierID);

			SendS2F41(pMessage);

		} else if (strOperationType.equals("UPDATE") == true) {
			// Get Update Info
			strCarrierID = pMessageInfo.toString("CarrierID", 0);
			strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);

			// Set Update Info
			pMessage.put("RCMD", "UPDATE");
			pMessage.put("CARRIERID", strCarrierID);
			pMessage.put("PRIORITY", new Integer(99));

			pMessage.put("DEST", ChangeToSEMFormat(strCarrierLocID));
			pMessage.put("STOCKERCRANEID", "Crane1");
			SendS2F41(pMessage);
		} else {
			Display("Undefined OperationType!!");
		}

		// ///////////////////////////
		// 2. Set Log
		strReceivedMessage = "OperationType:" + strOperationType;
		strReceivedMessage += "/ CarrierID:" + strCarrierID;
		strReceivedMessage += "/ CarrierLocID:" + strCarrierLocID;

		Display("[SEM I/F <- D.M] ReqSDOperation(" + strOperationType + "/" + strCarrierID + "/" + strCarrierLocID + ")");

	}

	// ---------------------------------------------------------------------------
	void ReqPortModeChange(MyHashtable pMessageInfo) {
		String strReceivedMessage, strTSC, strPortMode, strCarrierLocID;
		boolean b_TSCExist = false;

		// 1. Get MicroTrCmd Items
		strTSC = pMessageInfo.toString("TSC", 0);
		strPortMode = pMessageInfo.toString("PortMode", 0);
		strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);

		// ///////////////////

		MyHashtable pMessage = new MyHashtable();

		// Get OperationType

		if (strPortMode.equals("INPUT") == true) {
			// Get Install Info
			strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);

			// Set Install Info
			pMessage.put("RCMD", "INPUT_MODE");
			pMessage.put("CARRIERLOC", strCarrierLocID);

			SendS2F41(pMessage);
		} else if (strPortMode.equals("OUTPUT") == true) {
			// Get Install Info
			strCarrierLocID = pMessageInfo.toString("CarrierLocID", 0);

			// Set Install Info
			pMessage.put("RCMD", "OUTPUT_MODE");
			pMessage.put("CARRIERLOC", ChangeToSEMFormat(strCarrierLocID));

			SendS2F41(pMessage);
		} else {
			Display("Undefined PortMode!!");
		}

		// ///////////////////
		// 2. Set Log
		strReceivedMessage = "TSC:" + strTSC;
		strReceivedMessage += "/ PortMode:" + strPortMode;
		strReceivedMessage += "/ CarrierLocID:" + strCarrierLocID;

		Display("[SEM I/F <- D.M] ReqPortModeChange(" + strTSC + "/" + strPortMode + "/" + strCarrierLocID + ")");

	}

	// ---------------------------------------------------------------------------
	void GetTSCStatus(MyHashtable pMessageInfo) {
		String strReceivedMessage, strTSC, strCfgFile, strMessageList;
		int nReturnCode;

		// 1. Get TSC
		strTSC = pMessageInfo.toString("TSC", 0);

		// 2. Set Log
		strReceivedMessage = "TSC:" + strTSC;

		Display("[SEM I/F <- D.M] GetTSCStatus(" + strTSC + ")");

		MyHashtable pMessage = new MyHashtable();
		pMessage.put("TSCName", strTSC);

		if (m_strControlState.equals("REMOTE_ONLINE") && m_strOnlineSequenceState.equals("DONE")) {
			SendS1F3(73);
		}
	}

	// ---------------------------------------------------------------------------
	void GetEnhancedCarrierInfo(MyHashtable pMessageInfo) {

		String strReceivedMessage, strTSC, strCfgFile, strMessageList;
		int nReturnCode;

		// 1. Get TSC
		strTSC = pMessageInfo.toString("TSC", 0);

		// 2. Set Log
		strReceivedMessage = "TSC:" + strTSC;

		Display("[SEM I/F <- D.M] GetEnhancedCarrierInfo(" + strTSC + ")");

		MyHashtable pMessage = new MyHashtable();
		pMessage.put("TSCName", strTSC);

		SendS1F3(91);
	}

	// ---------------------------------------------------------------------------
	void GetEnhancedTrCmd(MyHashtable pMessageInfo) {
		String strReceivedMessage, strTSC, strCfgFile, strMessageList;
		int nReturnCode;

		// 1. Get TSC
		strTSC = pMessageInfo.toString("TSC", 0);

		// 2. Set Log
		strReceivedMessage = "TSC:" + strTSC;

		Display("[SEM I/F <- D.M] GetEnhancedTrCmd(" + strTSC + ")");

		MyHashtable pMessage = new MyHashtable();
		pMessage.put("TSCName", strTSC);

		SendS1F3(76);
	}

	// ---------------------------------------------------------------------------
	void GetEnhancedStockerUnit(MyHashtable pMessageInfo) {

		String strReceivedMessage, strTSC, strCfgFile, strMessageList;
		int nReturnCode;

		// 1. Get TSC
		strTSC = pMessageInfo.toString("TSC", 0);

		// 2. Set Log
		strReceivedMessage = "TSC:" + strTSC;

		Display("[SEM I/F <- D.M] GetEnhancedStockerUnit(" + strTSC + ")");

		MyHashtable pMessage = new MyHashtable();
		pMessage.put("TSCName", strTSC);

		Display("****** This Message is only available in PNPSEM ******");
	}

	// ---------------------------------------------------------------------------
	void GetVehicleInfo(MyHashtable pMessageInfo) {
		String strReceivedMessage, strTSC, strMessageList;
		int nReturnCode;

		// 1. Get TSC
		strTSC = pMessageInfo.toString("TSC", 0);

		// 2. Set Log
		strReceivedMessage = "TSC:" + strTSC;

		Display("[SEM I/F <- D.M] GetVehicleInfo(" + strTSC + ")");

		MyHashtable pMessage = new MyHashtable();
		pMessage.put("TSCName", strTSC);

		SendS1F3(53);

	}

	// ///////////////////////////////////////////////////////// import end from
	// MainUnit.cpp
	// ////////////////////////////// import from IBSEMModule.java
	// 기타
	public void Display(String strMsg) {
		m_ocsMain.Display(" " + m_strTSCName + ":: " + strMsg);
		// System.out.println(" " + m_strTSCName + ":: " +strMsg);
		m_ocsMain.Util.WriteReturnLog("IBSEM_", strMsg, "", true);

	}

	// IBSEM 운영관련
	public void Stop() {
		// Modified By Yoon(2006.7.19 )
		m_UCom.stopService();
		/*
		 * XCom1.Stop(); // Stop the XCom control. XCom1.Close();
		 */
		Display("XCom1 Stop successfully");
	}

	public int Initialize() {
		return Initialize(m_strConfigFileName);
	}

	int Initialize(String strConfigFileName) {
		int nRtnCode; // = IB_NO_ERROR;
		// String wsData;
		String strTemp, strCurrentDir;

		if (m_strConfigFileName != null && m_strConfigFileName.equals("") == false)
			strConfigFileName = m_strConfigFileName;
		else
			m_strConfigFileName = strConfigFileName;

		// Initialize the XCom1 control.
		/*
		 * XCom1 = new MyXCom(); if ( (nRtnCode =
		 * XCom1.Initialize(strConfigFileName)) == 0) {
		 * Display("XCom Initialied"); } else {
		 * Display("Fail to Initialize XCom (" + nRtnCode + ") : " +
		 * XComErr.getText(nRtnCode)); return nRtnCode; } // Start the XCom1
		 * control. if ( (nRtnCode = XCom1.Start()) == 0) {
		 * Display("XCom Started "); } else { Display("Fail to Start XCom (" +
		 * nRtnCode + ") : " + XComErr.getText(nRtnCode)); return nRtnCode; }
		 */

		// UCom 생성 및 초기화
		m_UCom = new UCom(UCom.COM_TYPE_SECOM, this);

		m_UCom.setCommCfgFile(strConfigFileName);

		// Start the XCom1 control.
		if (m_UCom.startService()) {
			Display("XCom Started ");
			nRtnCode = 0;
		} else {
			nRtnCode = -1;
			return nRtnCode;
		}

		// Status Info Initialize
		m_strHSMSState = "TCPIP_NOT_CONNECTED";
		m_strCommState = "COMM_DISABLED";
		m_strControlState = "EQ_OFFLINE";
		m_strSCState = "TSC_NONE";
		m_strAlarmState = "NO_ALARMS";
		SetSEMStatus();

		RepLocalStatus();

		// nRtnCode==0 이면 OK
		return nRtnCode;
	}

	// /////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////
	// /////////////////////////////////////////////////////////////////////////
	/*
	 * class MyXCom extends XCom { public void OnSecsMsg(UComMsg msg) { //
	 * MainForm.Memo에 받은 메세지를 출력... String strSFMsg = "S" + msg.GetStream() +
	 * "F" + msg.GetFunc(); if (strSFMsg.equals("S1F2") == false) {
	 * Display("Received> " + strSFMsg); } // Receive Fuction ReceivedMSG(msg);
	 * }
	 * 
	 * public void OnSecsEvent(int nEventId, UComMsg msg) { if (nEventId == 203)
	 * { // ALARM_T3_TIMEOUT... Display("[ALARM] T3 timeout alarm occurs..."); }
	 * else if (nEventId == 103) { // ALARM_CONNECT... Display("[ALARM] HSMS
	 * connected alarm happens..."); m_strHSMSState = "HSMS_SELECTED";
	 * m_strCommState = "NOT_COMMUNICATING";
	 * 
	 * SetSEMStatus(); //DB에 저장하고 Display정보변경 //S1F13TimerRestart(); } else if
	 * (nEventId == 101) { // ALARM_NOT_CONNECT Display("[ALARM] HSMS not
	 * connected alarm happens..."); m_strHSMSState = "TCPIP_NOT_CONNECTED";
	 * m_strCommState = "COMM_NONE"; m_strControlState = "CONTROL_NONE";
	 * m_strSCState = "TSC_NONE"; m_strAlarmState = "NO_ALARMS"; SetSEMStatus();
	 * //DB에 저장하고 Display정보변경
	 * 
	 * //MCS로 Event를 날린다. MyHashtable pReportInfo = new MyHashtable();
	 * pReportInfo.put("EventName", "EquipmentOffline");
	 * SendReportToDM(pReportInfo); //////delete pReportInfo;
	 * 
	 * m_strGetEnhancedDataState = "INIT"; m_bS2F33DeleteCompleted = false;
	 * ConnectionCheckTimer.cancel(); m_bResponse = true;
	 * 
	 * //2004.0604 N.Y.K m_bCheckDisConnection = true; m_strOnlineSequenceState
	 * = "INIT"; } else if (nEventId == 225) //2006.01.23 by N.Y.K { //
	 * ALARM_XI_CLOSE
	 * 
	 * //1. Status Save Display("[ALARM] XI was killed..."); m_strHSMSState =
	 * "TCPIP_NOT_CONNECTED"; m_strCommState = "COMM_NONE"; m_strControlState =
	 * "CONTROL_NONE"; m_strSCState = "TSC_NONE"; m_strAlarmState = "NO_ALARMS";
	 * SetSEMStatus(); //DB에 저장하고 Display정보변경
	 * 
	 * //2. MCS로 Event를 날린다. MyHashtable pReportInfo = new MyHashtable();
	 * pReportInfo.put("EventName", "EquipmentOffline");
	 * SendReportToDM(pReportInfo);
	 * 
	 * //3. 기존에 관련 Alarm이 있는지 확인 // AlarmText == "XCOM 225 XI_CLOSE" 인 놈이 있는지 확인
	 * int nAlarmCount = CheckExistXcomXIAlarm(); //4. 없으면 Alarm 등록 if
	 * (nAlarmCount == 0) { // AlarmID : nReturn(225) // AlarmEventType : XCOM
	 * // AlarmText : XCOM 225 XI_CLOSE // ErrorID | Alarm Count
	 * RegisterAlarm("", "", 225, "1", "["+m_ocsMain.m_strTSCID
	 * +"] XCOM 225 XI CLOSE", "XCOM", ""); //4.1 Alarm Text Update
	 * UpdateXcomXIAlarmText(); //4.2 System Shut down System.exit(1); } } }
	 * 
	 * boolean ReceivedMSG(UComMsg msg) { String sStr, sJis8, strBuff,
	 * strEventName = ""; //String wsData; String BSTRBuff; // char szMsg[255];
	 * String strMsg; int nList; short nValue = 0; int nSubPartNo; short nBuff;
	 * int nSubPartNo_2; short nErrorCode, nSize, nWbit; int i, nReturn = 0,
	 * nReportNo, nRPTID, nShelfNo; int lValue; long lReplyMsgId, lCount, lSize;
	 * long rValue; // struct time TempTime; // struct date TempDate;
	 * 
	 * String strSFMsg = "S" + msg.GetStream() + "F" + msg.GetFunc();
	 * 
	 * //#######################################################
	 * //####################################################### //## STREAM 1
	 * //#######################################################
	 * //#######################################################
	 * 
	 * if (strSFMsg.equals("S1F1") == true) { if
	 * (m_strCommState.equals("COMMUNICATING") == true) { // Send a reply
	 * message. UComMsg rsp = this.MakeSecsMsg(1, 2, msg.GetSysbytes());
	 * rsp.SetListItem(0); XComSend(rsp, "S1F2"); } else { // Ignore... Send
	 * S1F0 SendAbortTransaction(msg); } } else if (strSFMsg.equals("S1F2") ==
	 * true) { m_strControlState = "REMOTE_ONLINE";
	 * 
	 * if (m_strSCState.equals("TSC_NONE") == true) { m_strSCState = "TSC_INIT";
	 * } SetSEMStatus(); m_bResponse = true; } else if (strSFMsg.equals("S1F4")
	 * == true) { //구현할것 if (m_strSVID.equals("73") == true) { //Get SCStatus
	 * nList = msg.GetListItem(); //SVID갯수 lValue = msg.GetU2Item()[0];
	 * 
	 * //Send Report TSC Status To DataManager MyHashtable pReportInfo = new
	 * MyHashtable(); if (lValue == 1) //INIT { pReportInfo.put("EventName",
	 * "TSCAutoInitiated"); } else if (lValue == 2) //PAUSED {
	 * pReportInfo.put("EventName", "TSCPaused"); } else if (lValue == 3) //AUTO
	 * { if (m_strOnlineSequenceState.equals("INIT") == true) {
	 * pReportInfo.put("EventName", "TSCPaused"); } else {
	 * pReportInfo.put("EventName", "TSCAutoCompleted"); } } else if (lValue ==
	 * 4) //PAUSING { pReportInfo.put("EventName", "TSCPauseInitiated"); }
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * SendReportToDM(pReportInfo);
	 * 
	 * //Set Flag for OlineSequence if (m_strOnlineSequenceState.equals("INIT")
	 * == true) { SendS1F3(91); } } else if (m_strSVID.equals("91") == true) {
	 * int nTotalCarrierCount = 0;
	 * 
	 * //Get/Set Carrier Data MyHashtable pReportInfo = new MyHashtable(); nList
	 * = msg.GetListItem(); //SVID갯수 nList = msg.GetListItem(); //Carrier갯수
	 * nTotalCarrierCount = nList; pReportInfo.put("EventName",
	 * "RepEnhancedCarrierInfo"); pReportInfo.put("CarrierQty", new
	 * Integer(nTotalCarrierCount)); Vector vCarrierID = new Vector(); Vector
	 * vVehicleID = new Vector(); Vector vCarrierLocID = new Vector(); Vector
	 * vInstallTime = new Vector(); for (int k = 0; k < nTotalCarrierCount; k++)
	 * { nList = msg.GetListItem(); //L,4 BSTRBuff = msg.GetAsciiItem();
	 * vCarrierID.addElement(BSTRBuff); BSTRBuff = msg.GetAsciiItem();
	 * vVehicleID.addElement(ChangeToDMFormat(BSTRBuff)); BSTRBuff =
	 * msg.GetAsciiItem(); vCarrierLocID.addElement(ChangeToDMFormat(BSTRBuff));
	 * BSTRBuff = msg.GetAsciiItem(); vInstallTime.addElement(BSTRBuff); }
	 * 
	 * pReportInfo.put("CarrierID", vCarrierID); pReportInfo.put("VehicleID",
	 * vVehicleID); pReportInfo.put("CarrierLocID", vCarrierLocID);
	 * pReportInfo.put("InstallTime", vInstallTime);
	 * 
	 * 
	 * //Send Report Enhanced Carrier Data To DataManager
	 * SendReportToDM(pReportInfo);
	 * 
	 * //Set Flag for OlineSequence if (m_strOnlineSequenceState.equals("INIT")
	 * == true) { SendS1F3(76); } } else if (m_strSVID.equals("76") == true) {
	 * int nTotalCommandCount = 0; int nTransferInfoCnt = 0; //Get/Set Carrier
	 * Data MyHashtable pReportInfo = new MyHashtable(); nList =
	 * msg.GetListItem(); //SVID갯수 nList = msg.GetListItem(); //Command갯수
	 * nTotalCommandCount = nList; pReportInfo.put("EventName",
	 * "RepEnhancedTrCmd"); pReportInfo.put("CommandQty", new
	 * Integer(nTotalCommandCount));
	 * 
	 * Vector vMicroTrCmdID = new Vector(); Vector vPriority = new Vector();
	 * Vector vReplace = new Vector(); Vector vTransferState = new Vector();
	 * Vector vCarrierID = new Vector(); Vector vCarrierLocID = new Vector();
	 * Vector vDest = new Vector();
	 * 
	 * for (int k = 0; k < nTotalCommandCount; k++) { nList = msg.GetListItem();
	 * //L,3 nList = msg.GetListItem(); //L,3 BSTRBuff = msg.GetAsciiItem();
	 * vMicroTrCmdID.addElement(BSTRBuff); lValue = msg.GetU2Item()[0];
	 * vPriority.addElement(new Integer(lValue)); lValue = msg.GetU2Item()[0];
	 * vReplace.addElement(new Integer(lValue)); lValue = msg.GetU2Item()[0];
	 * vTransferState.addElement(new Integer(lValue)); nList =
	 * msg.GetListItem(); //L,n nTransferInfoCnt = nList; for (int j = 0; j <
	 * nTransferInfoCnt; j++) { nList = msg.GetListItem(); //L,3 BSTRBuff =
	 * msg.GetAsciiItem(); vCarrierID.addElement(BSTRBuff); BSTRBuff =
	 * msg.GetAsciiItem(); vCarrierLocID.addElement(ChangeToDMFormat(BSTRBuff));
	 * BSTRBuff = msg.GetAsciiItem();
	 * vDest.addElement(ChangeToDMFormat(BSTRBuff)); } }
	 * pReportInfo.put("MicroTrCmdID", vMicroTrCmdID);
	 * pReportInfo.put("Priority", vPriority); pReportInfo.put("Replace",
	 * vReplace); pReportInfo.put("TransferState", vTransferState);
	 * pReportInfo.put("CarrierID", vCarrierID); pReportInfo.put("CarrierLocID",
	 * vCarrierLocID); pReportInfo.put("Dest", vDest);
	 * 
	 * 
	 * //Send Report Enhanced Carrier Data To DataManager
	 * SendReportToDM(pReportInfo);
	 * 
	 * //Set Flag for OlineSequence if (m_strOnlineSequenceState.equals("INIT")
	 * == true) { SendS1F3(53); } } else if (m_strSVID.equals("53") == true) {
	 * int nTotalCarrierCount = 0;
	 * 
	 * //Get/Set Carrier Data MyHashtable pReportInfo = new MyHashtable(); nList
	 * = msg.GetListItem(); //SVID갯수 nList = msg.GetListItem(); //Carrier갯수
	 * nTotalCarrierCount = nList; pReportInfo.put("EventName",
	 * "RepVehicleInfo"); pReportInfo.put("VehicleQty", new
	 * Integer(nTotalCarrierCount));
	 * 
	 * Vector vVehicleID = new Vector(); Vector vVehicleState = new Vector();
	 * 
	 * for (int k = 0; k < nTotalCarrierCount; k++) { nList = msg.GetListItem();
	 * //L,2 BSTRBuff = msg.GetAsciiItem(); vVehicleID.addElement(BSTRBuff);
	 * lValue = msg.GetU2Item()[0]; vVehicleState.addElement(new
	 * Integer(lValue)); } pReportInfo.put("VehicleID", vVehicleID);
	 * pReportInfo.put("VehicleState", vVehicleState);
	 * 
	 * //Send Report Enhanced Carrier Data To DataManager
	 * SendReportToDM(pReportInfo);
	 * 
	 * //Set Flag for OlineSequence if (m_strOnlineSequenceState.equals("INIT")
	 * == true) { m_strOnlineSequenceState = "DONE"; MyHashtable pRESUMEMsg =
	 * new MyHashtable(); pRESUMEMsg.put("RCMD", "RESUME");
	 * SendS2F41(pRESUMEMsg); } } }
	 * /////////////////////////////////////////////
	 * /////////////////////////////// // S1F13 : Establish Communication
	 * Request else if (strSFMsg.equals("S1F13") == true) { // Send Response
	 * message as S1F14 UComMsg rsp = this.MakeSecsMsg(1, 14,
	 * msg.GetSysbytes()); nValue = 0; // COMACK, Bin, 0 -> ACK
	 * rsp.SetListItem(2); rsp.SetBinaryItem(nValue); rsp.SetListItem(0);
	 * XComSend(rsp, "S1F14");
	 * 
	 * m_strCommState = "COMMUNICATING"; m_strSCState = "TSC_NONE";
	 * m_strControlState = "EQ_OFFLINE"; SetSEMStatus(); Display("COMM_DIABLED
	 * -> COMMUNICATING ");
	 * 
	 * SendMsg("S1F17"); }
	 * //////////////////////////////////////////////////////
	 * ////////////////////// // S1F14 : Establish Communication Request ACK
	 * else if (strSFMsg.equals("S1F14") == true) { nList = msg.GetListItem();
	 * nValue = msg.GetBinaryItem()[0]; // COMACK
	 * 
	 * if (nValue == 0 && (m_strCommState.equals("NOT_COMMUNICATING") == true)
	 * || (m_strCommState.equals("COMMUNICATING") == true)) { m_strCommState =
	 * "COMMUNICATING"; m_strSCState = "TSC_NONE"; m_strControlState =
	 * "EQ_OFFLINE"; SetSEMStatus(); Display("COMM_DIABLED -> COMMUNICATING ");
	 * 
	 * MyHashtable pReportInfo = new MyHashtable(); pReportInfo.put("EventName",
	 * "CommunicationEvent"); pReportInfo.put("StatusChangedTime",
	 * GetCurrentTime()); pReportInfo.put("ONOFFLINEACK", new Integer(nValue));
	 * pReportInfo.put("CommunicationStatus", m_strCommState);
	 * SendReportToDM(pReportInfo); SendMsg("S1F17"); } else { // ERROR
	 * Display("Invalid ACK response or Invalid Communication Status"); //Send
	 * RepTSCControlStatus(FAIL); MyHashtable pReportInfo = new MyHashtable();
	 * pReportInfo.put("EventName", "TSCControlStatusFAIL");
	 * pReportInfo.put("ONOFFLINEACK", new Integer(3));
	 * pReportInfo.put("ControlStatus", "ONLINEFAIL");
	 * pReportInfo.put("CommunicationStatus", m_strCommState);
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * SendReportToDM(pReportInfo); } }
	 * /////////////////////////////////////////
	 * /////////////////////////////////// // S1F16 : OFFLINE Request ACK else
	 * if (strSFMsg.equals("S1F16") == true) { nValue = msg.GetBinaryItem()[0];
	 * // OFLACK
	 * 
	 * if ( (nValue == 0) && ( (m_strCommState.equals("NOT_COMMUNICATING") ==
	 * true) || (m_strCommState.equals("COMMUNICATING") == true))) { // Change
	 * Communicaion Status as COMMUNICATING m_strCommState = "COMMUNICATING"; //
	 * Change TSCStatus into NONE m_strSCState = "TSC_NONE"; // Chabge Control
	 * Status as EQ_OFFLINE State m_strControlState = "EQ_OFFLINE"; // Set SEM
	 * Status SetSEMStatus(); Display("REMOTE_ONLINE -> EQ_OFFLINE "); } else {
	 * if (nValue == 1) { // ERROR Display("Invalid Communication Status"); }
	 * else if (nValue == 2) { Display("Equipment Already Offline!!"); } else {
	 * Display("Undefined OFLACK!!"); } //Send RepTSCControlStatus(FAIL);
	 * MyHashtable pReportInfo = new MyHashtable(); pReportInfo.put("EventName",
	 * "TSCControlStatusFAIL"); pReportInfo.put("ONOFFLINEACK", new
	 * Integer(nValue)); pReportInfo.put("ControlStatus", "OFFLINEFAIL");
	 * pReportInfo.put("CommunicationStatus", m_strCommState);
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * SendReportToDM(pReportInfo); } }
	 * /////////////////////////////////////////
	 * /////////////////////////////////// // S1F18 : ONLINE Request ACK else if
	 * (strSFMsg.equals("S1F18") == true) { nValue = msg.GetBinaryItem()[0]; //
	 * OFLACK
	 * 
	 * if ( (nValue == 0) || (nValue == 2)) { if (nValue == 2) {
	 * Display("Equipment Already Online!!"); } else { Display("EQ_OFFLINE ->
	 * REMOTE_ONLINE"); }
	 * 
	 * m_strCommState = "COMMUNICATING"; m_strSCState = "TSC_INIT";
	 * m_strControlState = "REMOTE_ONLINE"; SetSEMStatus();
	 * 
	 * MyHashtable pReportInfo = new MyHashtable(); pReportInfo.put("EventName",
	 * "ControlStatusRemote"); pReportInfo.put("StatusChangedTime",
	 * GetCurrentTime()); pReportInfo.put("ONOFFLINEACK", new Integer(nValue));
	 * pReportInfo.put("CommunicationStatus", m_strCommState);
	 * SendReportToDM(pReportInfo);
	 * 
	 * SendMsg("S2F31"); } else { if (nValue == 1) { Display("Invalid
	 * Communication Status"); } else { Display("Undefined OLACK!!"); } //Send
	 * RepTSCControlStatus(FAIL); MyHashtable pReportInfo = new MyHashtable();
	 * pReportInfo.put("EventName", "TSCControlStatusFAIL");
	 * pReportInfo.put("ONOFFLINEACK", new Integer(nValue));
	 * pReportInfo.put("ControlStatus", "ONLINEFAIL");
	 * pReportInfo.put("CommunicationStatus", m_strCommState);
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * SendReportToDM(pReportInfo); } } else if (strSFMsg.equals("S2F16") ==
	 * true) { SendMsg("S5F3EnableAll"); }
	 * //////////////////////////////////////
	 * ////////////////////////////////////// // S2F17 : Date & Time Request
	 * else if (strSFMsg.equals("S2F17")==true) { // Send a reply message.
	 * UComMsg rsp = m_UCom.MakeSecsMsg(2, 18, msg.GetSysbytes());
	 * 
	 * SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS"); Date
	 * curTime = new Date(); String dateString =
	 * format.format(curTime).substring(0, 16);
	 * 
	 * rsp.SetAsciiItem(dateString); if ( (nReturn = XComSend(rsp, "S2F18")) !=
	 * 0) { Display("XCom Send() Fails..[S2F18]"); } else { Display("Reply S2F18
	 * successfully"); } } else if (strSFMsg.equals("S2F25") == true) { BSTRBuff
	 * = msg.GetAsciiItem(); strBuff = BSTRBuff;
	 * 
	 * Display("Received Loopback Diagnostic Request: String: " + strBuff); //
	 * Send Response message as S2F26 UComMsg rsp = this.MakeSecsMsg(2, 26,
	 * msg.GetSysbytes()); String strLoopback = "Received Loopback ACK";
	 * rsp.SetAsciiItem(strLoopback);
	 * 
	 * if ( (nReturn = XComSend(rsp, "S2F26")) == 0) { Display("Reply S2F26
	 * successfully"); } else { // Send S1F0 Display("XCom Send() Fails.. So
	 * send S2F0 [S2F26]"); SendAbortTransaction(msg); } } else if
	 * (strSFMsg.equals("S2F30") == true) { nSubPartNo = msg.GetListItem();
	 * //ECID 수 for (i = 0; i < nSubPartNo; i++) { nSubPartNo_2 =
	 * msg.GetListItem(); //ECID 수 lValue = msg.GetU2Item()[0]; Display("ECID: "
	 * + lValue); BSTRBuff = msg.GetAsciiItem(); Display("ECNAME: " + BSTRBuff);
	 * lValue = msg.GetU2Item()[0]; Display("ECMIN: " + lValue); lValue =
	 * msg.GetU2Item()[0]; Display("ECMAX: " + lValue); lValue =
	 * msg.GetU2Item()[0]; Display("ECDEF: " + lValue); BSTRBuff =
	 * msg.GetAsciiItem(); Display("UNITID: " + BSTRBuff); } } else if
	 * (strSFMsg.equals("S2F32") == true) { SendMsg("S2F15"); } else if
	 * (strSFMsg.equals("S2F34") ==true) { nValue = msg.GetBinaryItem()[0]; if
	 * (nValue == 0) {
	 * 
	 * if (m_bS2F33DeleteCompleted == false) { SendMsg("S2F33");
	 * m_bS2F33DeleteCompleted = true; } else { SendMsg("S2F35"); } } else {
	 * Display("Error >> DRACK(Define Report ACK) = " + nValue); Display("1:
	 * denied, insufficient space"); Display("2: denied, incorrect
	 * format"); Display("3: denied, at least one RPTID is already
	 * defined"); Display("4: denied, at least one VID is already
	 * defined"); } } else if (strSFMsg.equals("S2F36") == true) { nValue =
	 * msg.GetBinaryItem()[0]; if (nValue == 0) { SendMsg("S2F37"); } else {
	 * Display("Error >> LRACK(Link Report ACK) = " + nValue); Display("1:
	 * denied, insufficient space"); Display("2: denied, incorrect
	 * format"); Display("3: denied, at least one CEID link is already
	 * defined"); Display("4: denied, there isn't at least one
	 * CEID"); Display("5: denied, there isn't at least one
	 * RPTID"); } } else if (strSFMsg.equals("S2F38") == true) { nValue =
	 * msg.GetBinaryItem()[0]; if (nValue == 0) { SendS1F3(73); } else {
	 * Display("Error >> ERACK(Enalbed Report ACK) = " + nValue); Display("1:
	 * denied, there isn't at least one CEID"); } } else if
	 * (strSFMsg.equals("S2F42") == true) { nList = msg.GetListItem(); //L,2
	 * 
	 * nValue = msg.GetBinaryItem()[0];
	 * 
	 * //if( Display("HCACK:" + nValue); if ( (nValue == 5) &&
	 * (m_strCurrentTrCmdType.equals("RESUME") == true)) { m_strSCState =
	 * "TSC_AUTO"; SetSEMStatus(); Display(" FAIL : already
	 * requested!!(nValue==5)"); Display(" m_strCurrentTrCmdType" +
	 * m_strCurrentTrCmdType);
	 * 
	 * MyHashtable pReportInfo = new MyHashtable(); pReportInfo.put("EventName",
	 * "TSCAutoCompleted"); pReportInfo.put("StatusChangedTime",
	 * GetCurrentTime()); SendReportToDM(pReportInfo); } else if (nValue != 4) {
	 * if (nValue == 1) { Display(" FAIL : command doesn't exist!!"); } else if
	 * (nValue == 2) { Display(" FAIL : currently not able to execute!!"); }
	 * else if (nValue == 3) {
	 * Display(" FAIL : at least one parameter isn't valid!!"); } else if
	 * (nValue == 5) { Display(" FAIL : already requested!!"); } else if (nValue
	 * == 6) { Display(" FAIL : object doesn't exist!!"); } else if (nValue ==
	 * 65) { Display(" FAIL : Unrecognized CarrierId!!"); } else if (nValue ==
	 * 66) { Display(" FAIL : Double CarrierId!!"); } else if ( (nValue == 67)
	 * || (nValue == 68)) { Display(" FAIL : Source is NG!!"); } else if (
	 * (nValue == 69) || (nValue == 70)) { Display(" FAIL : Dest is NG!!"); }
	 * else if ( (nValue == 71) || (nValue == 72)) {
	 * Display(" FAIL : Shelf is NG!!"); } else { Display("RCV S2F42 : Undefined
	 * HCACK!!"); }
	 * 
	 * //Get Send MessageName String strMessageName; strMessageName =
	 * m_pTempRecordInfo.toString("MessageName", 0);
	 * 
	 * if ( (m_strCurrentTrCmdType.equals("CANCEL") == true) ||
	 * (m_strCurrentTrCmdType.equals("ABORT") == true) ||
	 * (m_strCurrentTrCmdType.equals("RETRY") == true)) { //Send
	 * RepMicroTCStatus(FAIL); MyHashtable pReportInfo = new MyHashtable();
	 * pReportInfo.put("EventName", "MicroTCStatusFAIL");
	 * pReportInfo.put("HCACK", new Integer(nValue));
	 * pReportInfo.put("MicroTrCmdID", m_strCurrentTrCmdID);
	 * 
	 * //2005.10.28 by N.Y.K 수정 String strCarrierID =
	 * GetCarrierIDFromMircoTrCmdId(m_strCurrentTrCmdID);
	 * pReportInfo.put("CarrierID", strCarrierID.trim());
	 * 
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime()); if
	 * (m_strCurrentTrCmdType == "CANCEL") { pReportInfo.put("MicroTCStatus",
	 * "CANCELFAIL"); } else if (m_strCurrentTrCmdType.equals("ABORT") == true)
	 * { pReportInfo.put("MicroTCStatus", "ABORTFAIL"); }
	 * SendReportToDM(pReportInfo); } else if
	 * (strMessageName.equals("ReqTSCStatusChange") == true) { //Send
	 * RepTSCStatus(FAIL); MyHashtable pReportInfo = new MyHashtable();
	 * pReportInfo.put("EventName", "TSCStatusFAIL"); pReportInfo.put("HCACK",
	 * new Integer(nValue)); pReportInfo.put("TSCStatus", "FAIL");
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * SendReportToDM(pReportInfo); } else if
	 * (strMessageName.equals("ReqSDOperation") == true) { String
	 * strOperationType, strCarrierLocID, strCarrierID;
	 * 
	 * strOperationType = m_pTempRecordInfo.toString("OperationType", 0);
	 * strCarrierLocID = m_pTempRecordInfo.toString("CarrierLocID", 0);
	 * strCarrierID = m_pTempRecordInfo.toString("CarrierID", 0);
	 * 
	 * //Send RepSDChanged(FAIL); MyHashtable pReportInfo = new MyHashtable();
	 * pReportInfo.put("EventName", "SDChangedFAIL");
	 * pReportInfo.put("SDStatus", "FAIL"); pReportInfo.put("CarrierID",
	 * strCarrierID); pReportInfo.put("OperationType", strOperationType);
	 * pReportInfo.put("CarrierLocID", strCarrierLocID);
	 * pReportInfo.put("HCACK", new Integer(nValue));
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * SendReportToDM(pReportInfo); } else if
	 * (strMessageName.equals("ReqPortModeChange") == true) { //Send
	 * RepPortModeChanged(FAIL); MyHashtable pReportInfo = new MyHashtable();
	 * pReportInfo.put("EventName", "PortModeChangedFAIL");
	 * pReportInfo.put("HCACK", new Integer(nValue));
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * SendReportToDM(pReportInfo); } } else //HCACK==4인경우 정상 { if (
	 * (m_strCurrentTrCmdType.equals("CANCEL") == true) ||
	 * (m_strCurrentTrCmdType.equals("ABORT") == true) ||
	 * (m_strCurrentTrCmdType.equals("RETRY") == true)) {
	 * 
	 * //Send RepMicroTCStatus(QUEUED); MyHashtable pReportInfo = new
	 * MyHashtable(); pReportInfo.put("EventName", "MicroTCQUEUED");
	 * pReportInfo.put("HCACK", new Integer(nValue));
	 * pReportInfo.put("MicroTrCmdID", m_strCurrentTrCmdID); //2005.10.28 by
	 * N.Y.K 수정 String strCarrierID =
	 * GetCarrierIDFromMircoTrCmdId(m_strCurrentTrCmdID);
	 * pReportInfo.put("CarrierID", strCarrierID.trim());
	 * 
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime()); if
	 * (m_strCurrentTrCmdType.equals("CANCEL") == true) {
	 * pReportInfo.put("MicroTCStatus", "CANCELQUEUED"); } else if
	 * (m_strCurrentTrCmdType.equals("ABORT") == true) {
	 * pReportInfo.put("MicroTCStatus", "ABORTQUEUED"); }
	 * SendReportToDM(pReportInfo);
	 * 
	 * m_strCurrentTrCmdType = ""; //SendTRCMDTimer.start(); } } } else if
	 * (strSFMsg.equals("S2F50") == true) { MyHashtable pReportInfo = new
	 * MyHashtable(); nList = msg.GetListItem(); //L,2 nValue =
	 * msg.GetBinaryItem()[0]; Display("HCACK:" + nValue);
	 * 
	 * if (nValue != 4) { if (nValue == 1) { Display(" FAIL : command doesn't
	 * exist!!"); } else if (nValue == 2) { Display(" FAIL : currently not able
	 * to execute!!"); } else if (nValue == 3) { Display(" FAIL : at least one
	 * parameter isn't valid!!"); } else if (nValue == 5) { Display(" FAIL :
	 * already requested!!"); } else if (nValue == 6) { Display(" FAIL : object
	 * doesn't exist!!"); } else if (nValue == 65) { Display(" FAIL :
	 * Unrecognized CarrierId!!"); } else if (nValue == 66) { Display(" FAIL :
	 * Double
	 * CarrierId!!"); } else if ( (nValue == 67) || (nValue == 68)) { Display("
	 * FAIL : Source is
	 * NG!!"); } else if ( (nValue == 69) || (nValue == 70)) { Display(" FAIL :
	 * Dest is NG!!"); } else if ( (nValue == 71) || (nValue == 72)) {
	 * Display(" FAIL : Shelf is NG!!"); } else { Display("RCV S2F50 : Undefined
	 * HCACK!!"); }
	 * 
	 * //Send RepMicroTCStatus(FAIL); pReportInfo.put("EventName",
	 * "MicroTCStatusFAIL"); pReportInfo.put("HCACK", new Integer(nValue));
	 * pReportInfo.put("MicroTCStatus", "FAIL"); pReportInfo.put("MicroTrCmdID",
	 * m_strCurrentTrCmdID); pReportInfo.put("CarrierID",
	 * m_strCurrentCarrierID); pReportInfo.put("StatusChangedTime",
	 * GetCurrentTime()); } else { //Send RepMicroTCStatus(QUEUED);
	 * pReportInfo.put("EventName", "MicroTCQUEUED"); pReportInfo.put("HCACK",
	 * new Integer(nValue)); pReportInfo.put("MicroTCStatus", "QUEUED");
	 * pReportInfo.put("MicroTrCmdID", m_strCurrentTrCmdID);
	 * pReportInfo.put("CarrierID", m_strCurrentCarrierID);
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime()); }
	 * 
	 * SendReportToDM(pReportInfo); }
	 * //#######################################################
	 * //####################################################### //## STREAM 5
	 * //#######################################################
	 * //####################################################### else if
	 * (strSFMsg.equals("S5F0") == true) { } else if (strSFMsg.equals("S5F1") ==
	 * true) { nList = msg.GetListItem();
	 * 
	 * byte[] nALCD = msg.GetBinaryItem(); // ALCD rValue = msg.GetU4Item()[0];
	 * BSTRBuff = msg.GetAsciiItem(); strBuff = BSTRBuff;
	 * 
	 * if(nALCD[0] == (byte) 0x0086) { nValue = 134; } else if(nALCD[0] ==
	 * (byte) 0x0080) { nValue = 128; } else if(nALCD[0] == (byte) 0x0000) {
	 * nValue = 0; }
	 * 
	 * m_strAlarmCode = Integer.toString(nValue); m_strAlarmID =
	 * Integer.toString((int)rValue); m_strAlarmText = strBuff;
	 * 
	 * Display("Alarm(" + nValue + "/" + rValue + "/" + strBuff + ")"); // Send
	 * a reply message. UComMsg rsp = m_UCom.MakeSecsMsg(5, 2,
	 * msg.GetSysbytes()); nValue = 0; rsp.SetBinaryItem(nValue); // B1 in TEL
	 * if ( (nReturn = XComSend(rsp, "SF")) == 0) {
	 * Display("Reply S5F2 successfully"); } else {
	 * Display("Fail to reply S5F2 (" + nReturn + ")"); }
	 * 
	 * //Alarm Cleared if (m_strAlarmCode.equals("0") == true) { //Send Report
	 * AlarmCleared Report to DataManager MyHashtable pReportInfo = new
	 * MyHashtable(); pReportInfo.put("EventName", "AlarmCleared");
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * 
	 * SendReportToDM(pReportInfo); } } else if (strSFMsg.equals("S5F4") ==
	 * true) { SendMsg("S2F33Delete"); //ConnectionCheckTimerRestart(); }
	 * //#######################################################
	 * //####################################################### //## STREAM 6
	 * //#######################################################
	 * //####################################################### else if
	 * (strSFMsg.equals("S6F0") == true) { } else if (strSFMsg.equals("S6F11")
	 * == true) { MyHashtable pReportInfo = new MyHashtable();
	 * 
	 * if (msg.IsEOF() == false) { nSubPartNo = msg.GetListItem(); rValue =
	 * msg.GetU4Item()[0]; // DATAID
	 * 
	 * if (m_strSEMFormat.equals("Daifuku") == true) { lValue =
	 * msg.GetU2Item()[0]; // CEID } else if (m_strSEMFormat.equals("Brooks") ==
	 * true) { lValue = (int) msg.GetU4Item()[0]; // CEID } else { // 기본 형식.
	 * lValue = (int) msg.GetU2Item()[0]; // CEID }
	 * 
	 * strEventName = GetCEventName(lValue); Display(" EventName: <" +
	 * strEventName + ">");
	 * 
	 * pReportInfo.put("EventName", strEventName);
	 * 
	 * UpdateStatus(strEventName);
	 * 
	 * nSubPartNo = msg.GetListItem(); // L,n nReportNo = nSubPartNo;
	 * //nReportNo = 1;
	 * 
	 * for (int k = 0; k < nReportNo; k++) { nSubPartNo = msg.GetListItem(); //
	 * L,2
	 * 
	 * lValue = msg.GetU2Item()[0]; // RPTID nRPTID = lValue;
	 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
	 * 
	 * if (nRPTID == 1) { nSubPartNo = msg.GetListItem(); // L,1 BSTRBuff =
	 * msg.GetAsciiItem(); m_strEqpName = BSTRBuff.trim();
	 * pReportInfo.put("EqpName", BSTRBuff); } else if (nRPTID == 2) { int
	 * nTRCMD = 0; nSubPartNo = msg.GetListItem(); // L,4 BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("EqpName", BSTRBuff); nSubPartNo =
	 * msg.GetListItem(); // L,3 BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("MicroTrCmdID", BSTRBuff); lValue = msg.GetU2Item()[0];
	 * pReportInfo.put("Priority", new Integer(lValue)); lValue =
	 * msg.GetU2Item()[0]; pReportInfo.put("Replace", new Integer(lValue));
	 * nSubPartNo = msg.GetListItem(); // L,n nTRCMD = nSubPartNo; for (int k2 =
	 * 0; k2 < nTRCMD; k2++) { nSubPartNo = msg.GetListItem(); // L,2 nSubPartNo
	 * = msg.GetListItem(); // L,3 BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("CarrierID", BSTRBuff); BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("Source", ChangeToDMFormat(BSTRBuff)); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("Dest", ChangeToDMFormat(BSTRBuff));
	 * BSTRBuff = msg.GetAsciiItem(); pReportInfo.put("CarrierLocID",
	 * ChangeToDMFormat(BSTRBuff)); } lValue = msg.GetU2Item()[0];
	 * pReportInfo.put("ResultCode", new Integer(lValue)); } else if (nRPTID ==
	 * 3) { int nTRCMD = 0; nSubPartNo = msg.GetListItem(); // L,3 BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("EqpName", BSTRBuff); nSubPartNo =
	 * msg.GetListItem(); // L,3 BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("MicroTrCmdID", BSTRBuff); lValue = msg.GetU2Item()[0];
	 * pReportInfo.put("Priority", new Integer(lValue)); lValue =
	 * msg.GetU2Item()[0]; pReportInfo.put("Replace", new Integer(lValue));
	 * nSubPartNo = msg.GetListItem(); // L,n nTRCMD = nSubPartNo; for (int k2 =
	 * 0; k2 < nTRCMD; k2++) { nSubPartNo = msg.GetListItem(); // L,2 nSubPartNo
	 * = msg.GetListItem(); // L,3 BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("CarrierID", BSTRBuff); BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("Source", ChangeToDMFormat(BSTRBuff)); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("Dest", ChangeToDMFormat(BSTRBuff));
	 * BSTRBuff = msg.GetAsciiItem(); pReportInfo.put("CarrierLocID",
	 * ChangeToDMFormat(BSTRBuff)); } } else if (nRPTID == 4) { nSubPartNo =
	 * msg.GetListItem(); // L,3 BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("EqpName", BSTRBuff); BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("CarrierID", BSTRBuff); BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("CarrierLocID", ChangeToDMFormat(BSTRBuff)); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("VehicleID", BSTRBuff); } else if
	 * (nRPTID == 5) { nSubPartNo = msg.GetListItem(); // L,3 BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("EqpName", BSTRBuff); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("VehicleID", BSTRBuff); nSubPartNo =
	 * msg.GetListItem(); // L,n for (int k2 = 0; k2 < nSubPartNo; k2++) {
	 * BSTRBuff = msg.GetAsciiItem(); pReportInfo.put("CarrierLocID",
	 * ChangeToDMFormat(BSTRBuff)); //PortList } } else if (nRPTID == 6) {
	 * nSubPartNo = msg.GetListItem(); // L,4 BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("EqpName", BSTRBuff); BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("VehicleID", BSTRBuff); BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("CarrierID", BSTRBuff); BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("CarrierLocID", ChangeToDMFormat(BSTRBuff)); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("MicroTrCmdID", BSTRBuff); } else if
	 * (nRPTID == 7) { nSubPartNo = msg.GetListItem(); // L,3 BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("EqpName", BSTRBuff); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("VehicleID", BSTRBuff); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("MicroTrCmdID", BSTRBuff); } else if
	 * (nRPTID == 8) { nSubPartNo = msg.GetListItem(); // L,2 BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("EqpName", BSTRBuff); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("VehicleID", BSTRBuff); } else if
	 * (nRPTID == 9) { nSubPartNo = msg.GetListItem(); // L,4 BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("MicroTrCmdID", BSTRBuff); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("MicroTrCmdType", BSTRBuff); BSTRBuff
	 * = msg.GetAsciiItem(); pReportInfo.put("CarrierID", BSTRBuff); BSTRBuff =
	 * msg.GetAsciiItem(); pReportInfo.put("Source",
	 * ChangeToDMFormat(BSTRBuff)); BSTRBuff = msg.GetAsciiItem();
	 * pReportInfo.put("Dest", ChangeToDMFormat(BSTRBuff)); lValue =
	 * msg.GetU2Item()[0]; pReportInfo.put("Priority", new Integer(lValue)); } }
	 * } // Send a reply message. UComMsg rsp = m_UCom.MakeSecsMsg(6, 12,
	 * msg.GetSysbytes()); nValue = 0; rsp.SetBinaryItem(nValue); // B1 in TEL
	 * if ( (nReturn = XComSend(rsp, "S6F12")) == 0) { //Display("Reply S6F12
	 * successfully"); } else { Display("Fail to reply S6F12
	 * (" + nReturn + ")"); }
	 * 
	 * //2005.09.29. by N.Y.K S6F12보내는것과 순서를 바꿈 //Send Report To DATAMANAGER
	 * SendReportToDM(pReportInfo);
	 * 
	 * } else if (strSFMsg.equals("S6F16") == true) { }
	 * 
	 * m_UCom.CloseSecsMsg(msg);
	 * 
	 * return true; } }
	 */
	String GetCarrierIDFromMircoTrCmdId(String strMicroTrCmdID) {
		String strCarrierId = "";
		String strSql = "SELECT CarrierId FROM MicroTrCmd WHERE MicroTrCmdID='" + strMicroTrCmdID + "'";
		ResultSet rs = null;
		try {
			rs = m_ocsMain.m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				strCarrierId = rs.getString("CarrierId");
			}
		} catch (SQLException e) {
			String strLog = "GetCarrierIdFromMicroTrCmd() - SQLException: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_ocsMain.m_dbFrame.CloseRecord(rs);
			}
		}

		return strCarrierId;
	}

	// Multi-SEM 관련
	public boolean SetTSCName(String strTSCName) {
		m_strTSCName = strTSCName;
		m_strEqpName = strTSCName.substring(1);
		return true;
	}

	public boolean CheckTSCName(String strTSCName) {
		if (m_strTSCName.equals(strTSCName) == true) {
			return true;
		} else {
			return false;
		}

	}

	public String GetTSCName() {
		return m_strTSCName;
	}

	// Remote Command
	// S2F41: RCMD
	public short SendS2F41(MyHashtable pMessage) {
		long lMsgId, lCount, lValue;
		int nNumCP;
		short nReturn = 0; // , nShort;
		String wsData;

		String strRCMD, strCOMMANDID, strCARRIERID, strSOURCE, strDEST, strLOTID, strSTOCKERCRANEID, strDESTLOC, strCARRIERLOC, strLOCTYPE, strSTKUNITID, strSTKUNITTYPE, strERRORID, strCARRIERLOCID;

		int nPRIORITY, nFLOORNUMBER, nEMPTYCARRIER, nGLASSCHECKFLAG, nSTKUNITTYPE;

		strRCMD = pMessage.toString("RCMD", 0);

		UComMsg rsp = m_UCom.MakeSecsMsg(2, 41);
		rsp.SetListItem(2); // L m
		rsp.SetAsciiItem(strRCMD);

		if (strRCMD.equals("INSTALL") == true) {

			// Get INSTALL Info.
			strCARRIERID = pMessage.toString("CARRIERID", 0);
			strCARRIERLOC = pMessage.toString("CARRIERLOC", 0);

			rsp.SetListItem(2); // L 2
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERID");
			rsp.SetAsciiItem(strCARRIERID);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERLOC");
			rsp.SetAsciiItem(strCARRIERLOC);
		} else if (strRCMD.equals("LOCATE") == true) {
			// Get LOCATE Info.
			strCARRIERID = pMessage.toString("CARRIERID", 0);

			rsp.SetListItem(1); // L 1
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERID");
			rsp.SetAsciiItem(strCARRIERID);
		} else if (strRCMD.equals("REMOVE") == true) {
			// Get REMOVE Info.
			strCARRIERID = pMessage.toString("CARRIERID", 0);

			rsp.SetListItem(1); // L 1
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERID");
			rsp.SetAsciiItem(strCARRIERID);
		} else if (strRCMD.equals("UPDATE") == true) {
			// Get UPDATE Info.
			strCARRIERID = pMessage.toString("CARRIERID", 0);
			nPRIORITY = pMessage.toInt("PRIORITY", 0);
			strDEST = pMessage.toString("DEST", 0);
			strSTOCKERCRANEID = pMessage.toString("STOCKERCRANEID", 0);
			strDESTLOC = pMessage.toString("DESTLOC", 0);

			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("PRIORITY");
			lValue = nPRIORITY;
			rsp.SetU2Item((int) lValue);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("DEST");
			rsp.SetAsciiItem(strDEST);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("STOCKERCRANEID");
			rsp.SetAsciiItem(strSTOCKERCRANEID);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("DESTLOC");
			rsp.SetAsciiItem(strDESTLOC);
		} else if ((strRCMD.equals("CANCEL") == true) || (strRCMD.equals("ABORT") == true)) {
			// Get CANCEL/ABORT Info.
			strCOMMANDID = pMessage.toString("COMMANDID", 0);

			rsp.SetListItem(1); // L 1
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("COMMANDID");
			rsp.SetAsciiItem(strCOMMANDID);
		}
		// 2009.07.21 by MYM : Stage Delete 명령 전송
		else if (strRCMD.equals("STAGEDELETE") == true) {
			// Get CANCEL/ABORT Info.
			strCOMMANDID = pMessage.toString("COMMANDID", 0);

			rsp.SetListItem(1); // L 1
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("STAGEID");
			rsp.SetAsciiItem(strCOMMANDID);
		} else if ((strRCMD.equals("RESUME") == true) || (strRCMD.equals("PAUSE") == true)) {
			rsp.SetListItem(0); // L 0
			m_strCurrentTrCmdType = "RESUME";
		}

		else if (strRCMD.equals("RETRY") == true) {
			// Get RETRY Info.
			strERRORID = pMessage.toString("ERRORID", 0);

			rsp.SetListItem(1); // L 1
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("ERRORID");
			rsp.SetAsciiItem(strERRORID);
		} else if ((strRCMD.equals("INPUT_MODE") == true) || (strRCMD.equals("OUTPUT_MODE") == true)) {
			// Get RETRY Info.
			strCARRIERLOCID = pMessage.toString("CARRIERLOCID", 0);

			rsp.SetListItem(2); // L 2
			rsp.SetListItem(1); // L 1
			rsp.SetAsciiItem("SOURCE");
			rsp.SetAsciiItem(strCARRIERLOCID);
		}
		// STBC IDREAD 추가
		else if (strRCMD.equals("IDREAD") == true) {

			// Get INSTALL Info.
			strCARRIERID = pMessage.toString("CARRIERID", 0);
			strCARRIERLOC = pMessage.toString("PORTID", 0);

			rsp.SetListItem(2); // L 2
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERID");
			rsp.SetAsciiItem(strCARRIERID);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("PORTID");
			rsp.SetAsciiItem(strCARRIERLOC);
		}

		XComSend(rsp, "S2F41(" + strRCMD + ")");
		return nReturn;

	}

	// S2F49: Enhanced RCMD
	public short SendS2F49(MyHashtable pMessage) {
		long lMsgId, lCount;
		int lValue;
		long rValue;
		short nReturn = 0; // , nShort;
		String wsData = "";
		String strRCMD, strCOMMANDID, strCARRIERID, strLOTID, strSOURCE, strDEST, strSTOCKERCRANEID, strDESTLOC, strLOCTYPE, strCARRIERLOC, strERRORID;
		int nPRIORITY = 0, nFLOORNUMBER = 0, nGLASSCHECKFLAG = 0, nREPLACE = 0;

		// Get COMMANDINFO Info.
		strRCMD = pMessage.toString("RCMD", 0);
		strCOMMANDID = pMessage.toString("COMMANDID", 0);
		nPRIORITY = pMessage.toInt("PRIORITY", 0);
		nREPLACE = pMessage.toInt("REPLACE", 0);

		UComMsg rsp = m_UCom.MakeSecsMsg(2, 49);
		rsp.SetListItem(4); // L m

		rValue = 0;
		rsp.SetU4Item(rValue); // DataID
		rsp.SetAsciiItem(wsData); // OBJSPEC
		rsp.SetAsciiItem(strRCMD); // RCMD
		rsp.SetListItem(2); // L n => 2:SingleCarrierTransfer
		// 3:MultipleCarrierTransfer
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("COMMANDINFO");
		rsp.SetListItem(3); // L 3
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("COMMANDID");
		rsp.SetAsciiItem(strCOMMANDID);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("PRIORITY");
		lValue = nPRIORITY;
		rsp.SetU2Item(lValue);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("REPLACE");
		lValue = nREPLACE;
		rsp.SetU2Item(lValue);

		// Carrier개수만큼의 전송 명렁을 수행한다.
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("TRANSFERINFO");

		if (strRCMD.equals("TRANSFER") == true) {
			// Get TRANSFER Info.
			strCARRIERID = pMessage.toString("CARRIERID", 0);
			strSOURCE = pMessage.toString("SOURCE", 0);
			strDEST = pMessage.toString("DEST", 0);

			rsp.SetListItem(3); // L 3
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERID");
			rsp.SetAsciiItem(strCARRIERID);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("SOURCEPORT");
			rsp.SetAsciiItem(strSOURCE);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("DESTPORT");
			rsp.SetAsciiItem(strDEST);
		}

		XComSend(rsp, "S2F49(" + strRCMD + ")");
		return nReturn;
	}

	/**
	 * S2F49: Enhanced RCMD
	 *
	 * @param pMessage
	 *            MyHashtable
	 * @return short
	 * @version Created by MYM 2009.09.10
	 */
	public short SendS2F49TrForStage(MyHashtable pMessage) {
		long lMsgId, lCount;
		int lValue;
		long rValue;
		short nReturn = 0; // , nShort;
		String wsData = "";
		String strRCMD, strCOMMANDID, strCARRIERID, strLOTID, strSOURCE, strDEST, strSTOCKERCRANEID, strDESTLOC, strLOCTYPE, strCARRIERLOC, strERRORID;
		int nPRIORITY = 0, nFLOORNUMBER = 0, nGLASSCHECKFLAG = 0, nREPLACE = 0;

		// Get COMMANDINFO Info.
		strRCMD = pMessage.toString("RCMD", 0);
		strCOMMANDID = pMessage.toString("COMMANDID", 0);
		nPRIORITY = pMessage.toInt("PRIORITY", 0);
		nREPLACE = pMessage.toInt("REPLACE", 0);

		UComMsg rsp = m_UCom.MakeSecsMsg(2, 49);
		rsp.SetListItem(4); // L m

		rValue = 0;
		rsp.SetU4Item(rValue); // DataID
		rsp.SetAsciiItem(wsData); // OBJSPEC
		rsp.SetAsciiItem(strRCMD); // RCMD
		rsp.SetListItem(3); // L n => 2:SingleCarrierTransfer
		// 3:MultipleCarrierTransfer
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("COMMANDINFO");
		rsp.SetListItem(3); // L 3
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("COMMANDID");
		rsp.SetAsciiItem(strCOMMANDID);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("PRIORITY");
		lValue = nPRIORITY;
		rsp.SetU2Item(lValue);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("REPLACE");
		lValue = nREPLACE;
		rsp.SetU2Item(lValue);

		// Carrier개수만큼의 전송 명렁을 수행한다.
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("TRANSFERINFO");

		if (strRCMD.equals("TRANSFER") == true) {
			// Get TRANSFER Info.
			strCARRIERID = pMessage.toString("CARRIERID", 0);
			strSOURCE = pMessage.toString("SOURCE", 0);
			strDEST = pMessage.toString("DEST", 0);

			rsp.SetListItem(3); // L 3
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERID");
			rsp.SetAsciiItem(strCARRIERID);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("SOURCEPORT");
			rsp.SetAsciiItem(strSOURCE);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("DESTPORT");
			rsp.SetAsciiItem(strDEST);
		}

		// 2009.09.10 by MYM : STAGEIDLIST
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("STAGEIDLIST");
		rsp.SetListItem(1); // L 3
		strCARRIERID = pMessage.toString("CARRIERID", 0);
		rsp.SetAsciiItem(strCARRIERID);

		XComSend(rsp, "S2F49(" + strRCMD + ")");
		return nReturn;
	}

	// S2F49: Enhanced RCMD
	public short SendS2F49ForStage(MyHashtable pMessage) {
		long lMsgId, lCount;
		int lValue;
		long rValue;
		short nReturn = 0; // , nShort;
		String wsData = "";
		String strRCMD, strCOMMANDID, strCARRIERID, strLOTID, strSOURCE, strDEST, strSTOCKERCRANEID, strDESTLOC, strLOCTYPE, strCARRIERLOC, strERRORID;
		int nPRIORITY = 0, nFLOORNUMBER = 0, nGLASSCHECKFLAG = 0, nREPLACE = 0;

		int nExpectedDuration = 0, nNoBlockingTime = 0, nWaitTimeOut = 0;

		// Get COMMANDINFO Info.
		strRCMD = pMessage.toString("RCMD", 0);
		strCOMMANDID = pMessage.toString("STAGEID", 0);
		nPRIORITY = pMessage.toInt("PRIORITY", 0);
		nREPLACE = pMessage.toInt("REPLACE", 0);

		nExpectedDuration = pMessage.toInt("EXPECTEDDURATION", 0);
		nNoBlockingTime = pMessage.toInt("NOBLOCKINGTIME", 0);
		nWaitTimeOut = pMessage.toInt("WAITTIMEOUT", 0);

		UComMsg rsp = m_UCom.MakeSecsMsg(2, 49);
		rsp.SetListItem(4); // L m

		rValue = 0;
		rsp.SetU4Item(rValue); // DataID
		rsp.SetAsciiItem(wsData); // OBJSPEC
		rsp.SetAsciiItem(strRCMD); // RCMD
		rsp.SetListItem(2); // L n => 2:SingleCarrierTransfer
		// 3:MultipleCarrierTransfer
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("STAGEINFO");
		rsp.SetListItem(6); // L 3
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("STAGEID");
		rsp.SetAsciiItem(strCOMMANDID);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("PRIORITY");
		lValue = nPRIORITY;
		rsp.SetU2Item(lValue);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("REPLACE");
		lValue = nREPLACE;
		rsp.SetU2Item(lValue);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("EXPECTEDDURATION");
		lValue = nExpectedDuration;
		rsp.SetU2Item(lValue);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("NOBLOCKINGTIME");
		lValue = nNoBlockingTime;
		rsp.SetU2Item(lValue);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("WAITTIMEOUT");
		lValue = nWaitTimeOut;
		rsp.SetU2Item(lValue);

		// Carrier개수만큼의 전송 명렁을 수행한다.
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("TRANSFERINFO");

		if (strRCMD.equals("STAGE") == true) {
			// Get TRANSFER Info.
			strCARRIERID = pMessage.toString("CARRIERID", 0);
			strSOURCE = pMessage.toString("SOURCE", 0);
			strDEST = pMessage.toString("DEST", 0);

			rsp.SetListItem(3); // L 3
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERID");
			rsp.SetAsciiItem(strCARRIERID);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("SOURCEPORT");
			rsp.SetAsciiItem(strSOURCE);
			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("DESTPORT");
			rsp.SetAsciiItem(strDEST);
		}

		XComSend(rsp, "S2F49(" + strRCMD + ")");
		return nReturn;
	}

	// S2F49: Enhanced RCMD For Scan Command
	public short SendS2F49ForScan(MyHashtable pMessage) {
		long lMsgId, lCount;
		int lValue;
		long rValue;
		short nReturn = 0; //
		String wsData = "";
		String strRCMD, strCOMMANDID, strCARRIERID, strCARRIERLOCID;
		int nPRIORITY = 0;

		// Get COMMANDINFO Info.
		strRCMD = pMessage.toString("RCMD", 0);
		strCOMMANDID = pMessage.toString("COMMANDID", 0);
		nPRIORITY = pMessage.toInt("PRIORITY", 0);
		strCARRIERID = pMessage.toString("CARRIERID", 0);
		strCARRIERLOCID = pMessage.toString("CARRIERLOCID", 0);

		UComMsg rsp = m_UCom.MakeSecsMsg(2, 49);
		rsp.SetListItem(4); // L m

		rValue = 0;
		rsp.SetU4Item(rValue); // DataID
		rsp.SetAsciiItem(wsData); // OBJSPEC
		rsp.SetAsciiItem(strRCMD); // RCMD
		rsp.SetListItem(2); // L n => 2:SingleCarrierTransfer

		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("COMMANDINFO");
		rsp.SetListItem(2); // L 2
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("COMMANDID");
		rsp.SetAsciiItem(strCOMMANDID);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("PRIORITY");
		lValue = nPRIORITY;
		rsp.SetU2Item(lValue);

		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("SCANINFO");
		rsp.SetListItem(2); // L 2
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("CARRIERID");
		rsp.SetAsciiItem(strCARRIERID);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("CARRIERLOC");
		rsp.SetAsciiItem(strCARRIERLOCID);

		XComSend(rsp, "S2F49(" + strRCMD + ")");
		return nReturn;
	}

	/**
	 * 2021.03.30 dahye Transfer_Ex4
	 */
	//S2F49: Enhanced RCMD For TransferEx4 Command
	public short SendS2F49ForEx4(MyHashtable pMessage) {
		long lMsgId, lCount;
		int lValue;
		long rValue;
		short nReturn = 0; // , nShort;
		String wsData = "";
		String strRCMD, strCOMMANDID, strCARRIERID, strSOURCE, strDEST, strCARRIERLOC, strDELIVERYTYPE;
		int nPRIORITY = 0, nREPLACE = 0, nEXPECTEDDELIVERYTIME = 0, nDELIVERYWAITTIMEOUT = 0;

		// Get COMMANDINFO Info.
		strRCMD = pMessage.toString("RCMD", 0); // 'TRANSFER_EX4'
		strCOMMANDID = pMessage.toString("COMMANDID", 0);
		nPRIORITY = pMessage.toInt("PRIORITY", 0);
		nREPLACE = pMessage.toInt("REPLACE", 0);

		UComMsg rsp = m_UCom.MakeSecsMsg(2, 49);
		rsp.SetListItem(4); // L m

		rValue = 0;
		rsp.SetU4Item(rValue); // DataID
		rsp.SetAsciiItem(wsData); // OBJSPEC
		rsp.SetAsciiItem(strRCMD); // RCMD
		rsp.SetListItem(2); // L 2
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("COMMANDINFO");
		rsp.SetListItem(3); // L 3
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("COMMANDID");
		rsp.SetAsciiItem(strCOMMANDID);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("PRIORITY");
		lValue = nPRIORITY;
		rsp.SetU2Item(lValue);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("REPLACE");
		lValue = nREPLACE;
		rsp.SetU2Item(lValue);

		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("TRANSFERINFO");
		// Get TRANSFER Info.
		strCARRIERID = pMessage.toString("CARRIERID", 0);
		strSOURCE = pMessage.toString("SOURCE", 0);
		strDEST = pMessage.toString("DEST", 0);

		rsp.SetListItem(3); // L 3
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("CARRIERID");
		rsp.SetAsciiItem(strCARRIERID);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("SOURCEPORT");
		rsp.SetAsciiItem(strSOURCE);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("DESTPORT");
		rsp.SetAsciiItem(strDEST);

		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("DELIVERYINFO");
		// Get DELIVERY Info.
		strDELIVERYTYPE = pMessage.toString("DELIVERYTYPE", 0);
		nEXPECTEDDELIVERYTIME = pMessage.toInt("EXPECTEDDELIVERYTIME", 0);
		nDELIVERYWAITTIMEOUT = pMessage.toInt("DELIVERYWAITTIMEOUT", 0);

		rsp.SetListItem(3); // L 3
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("DELIVERYTYPE");
		rsp.SetAsciiItem(strDELIVERYTYPE);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("EXPECTEDDELIVERYTIME");
		lValue = nEXPECTEDDELIVERYTIME;
		rsp.SetU2Item(lValue);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem("DESTPORT");
		lValue = nDELIVERYWAITTIMEOUT;
		rsp.SetU2Item(lValue);

		XComSend(rsp, "S2F49(" + strRCMD + ")");
		return nReturn;
	}

	// Send Messages
	public short SendMsg(String strSFMsg) {
		String szBuff;
		long lMsgId, lCount;
		int lValue;
		short nReturn = 0, nValue, nShort;
		String wsData;
		long rValue;
		String strTemp;

		// /////////////////////////////////////////////////////////////////////////////////////
		// STREAM 1: Equipment Status
		// /////////////////////////////////////////////////////////////////////////////////////
		if (strSFMsg.equals("S1F0") == true) // Abort Transaction
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(1, 0); // (Message Id,
			// S,F,byte,deviceId)
			XComSend(rsp, "S1F0");
		} else if (strSFMsg.equals("S1F1") == true) // Are you there Request
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(1, 1); // (Message Id,
			// S,F,byte,deviceId)
			XComSend(rsp, "S1F1");
		} else if (strSFMsg.equals("S1F13") == true) // Establish Communications
		// Request
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(1, 13);
			rsp.SetListItem(0);
			nReturn = (short) XComSend(rsp, "S1F13");
			return nReturn;
		} else if (strSFMsg.equals("S1F15") == true) //
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(1, 15);
			XComSend(rsp, "S1F15");
		} else if (strSFMsg.equals("S1F17") == true) {
			UComMsg rsp = m_UCom.MakeSecsMsg(1, 17);

			if (m_strSEMFormat.equals("Daifuku") == true) {
				int nTempValue = 1;
				rsp.SetU2Item(nTempValue);
			} else if (m_strSEMFormat.equals("Brooks") == true) {
				;
			} else { // 기본 형식.
				;
			}

			XComSend(rsp, "S1F17");
		}
		// /////////////////////////////////////////////////////////////////////////////////////
		// STREAM 2: Equipment Control and Diagnostics
		// /////////////////////////////////////////////////////////////////////////////////////
		else if (strSFMsg.equals("S2F0") == true) // Abort Transaction
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 0); // (Message Id,
			// S,F,byte,deviceId)
			XComSend(rsp, "S2F0");
		} else if (strSFMsg.equals("S2F13") == true) // Equipment Constant Request
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 13); // (Message Id,
			// S,F,byte,deviceId)
			rsp.SetListItem(2);
			lValue = 2;
			rsp.SetU2Item(lValue);
			lValue = 61;
			rsp.SetU2Item(lValue);

			XComSend(rsp, "S2F13");
		} else if (strSFMsg.equals("S2F15") == true) // New Equipment Send
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 15); // (Message Id,
			// S,F,byte,deviceId)
			rsp.SetListItem(1);
			rsp.SetListItem(2);
			lValue = 61;
			rsp.SetU2Item(lValue);
			rsp.SetAsciiItem(m_strEqpName);
			XComSend(rsp, "S2F15");
		} else if (strSFMsg.equals("S2F17") == true) // Date and Time Req. (DTR)
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 17);
			XComSend(rsp, "S2F17");
		} else if (strSFMsg.equals("S2F29") == true) // Equipment Constant Request
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 29); // (Message Id,
			// S,F,byte,deviceId)
			rsp.SetListItem(1);
			lValue = 2;
			rsp.SetU2Item(lValue);
			XComSend(rsp, "S1F29");
		} else if (strSFMsg.equals("S2F31") == true) // Date & Time Set Req
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 31);

			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			Date curTime = new Date();
			String dateString = format.format(curTime).substring(0, 16);

			wsData = dateString;
			lCount = (long) dateString.length();
			//nReturn = (short) rsp.SetAsciiItem(wsData);
			rsp.SetAsciiItem(wsData);

			XComSend(rsp, "S2F31");

		} else if (strSFMsg.equals("S2F33") == true) // Define Report
		{
			MyHashtable pMessage = new MyHashtable();
			// 2009.11.19 by MYM : Scan 위한 Report 추가
			int nTotalReportCnt = 7;

			// [6][] =
			// {ReportID, Num_related VIDs, VIDs......)
			/*
			 * 1 - 54,56 2 - 54,56 10- 6,5 11- 54,56,63 12- 115 13- 115 19- 69
			 */

			int naReportData[][] = { { 1, 2, 54, 56 }, { 2, 2, 54, 56 }, { 10, 2, 6, 5 }, { 11, 3, 54, 56, 63 }, { 12, 1, 115 }, { 13, 1, 115 }, { 19, 1, 69 } };

			// 2015.01.30 by KYK : STBReport.ini 에서 설정하도록
			if (loadReportConfig(pMessage) == false) {
				Vector lstReportID = new Vector();
				pMessage.put("ReportCount", new Integer(nTotalReportCnt));
				for (int k = 0; k < nTotalReportCnt; k++) {
					lstReportID.addElement(new Integer(naReportData[k][0]));
					Vector lstVID = new Vector();
					for (int j = 0; j < naReportData[k][1] + 1; j++) {
						lstVID.addElement(new Integer(naReportData[k][j + 1]));
					}
					pMessage.put(Integer.toString(naReportData[k][0]), lstVID);
				}
				pMessage.put("ReportIDList", lstReportID);
			}
			SendS2F33(pMessage);
		} else if (strSFMsg.equals("S2F33Delete") == true) // Define Report
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 33);
			rsp.SetListItem(2);
			rValue = 0;
			rsp.SetU4Item(rValue); // DataID 0 Fixed
			rsp.SetListItem(0); // L m
			XComSend(rsp, "S2F33(Delete)");
		} else if (strSFMsg.equals("S2F35") == true) // Link Event Report
		{
			MyHashtable pMessage = new MyHashtable();
			int nTotalCEIDCnt = 15;
			// new int[36][10] =
			// {CEID, Num_related Reports, Reports....)
			int naEventLinkData[][] = { { 1, 1, 10 }, { 2, 1, 10 }, { 3, 1, 10 }, { 51, 0 }, { 52, 0 }, { 53, 0 }, { 54, 0 }, { 55, 0 }, { 56, 0 }, { 57, 0 }, { 151, 1, 1 }, { 152, 1, 2 },
					{ 251, 1, 11 }, { 260, 1, 13 }, { 261, 1, 12 } };

			// 2015.02.02 by KYK
			if (loadLinkEventReportConfig(pMessage) == false) {
				Vector lstCEID = new Vector();
				pMessage.put("CEIDCount", new Integer(nTotalCEIDCnt));
				for (int k = 0; k < nTotalCEIDCnt; k++) {
					lstCEID.addElement(new Integer(naEventLinkData[k][0]));
					Vector lstReportID = new Vector();
					for (int j = 0; j < naEventLinkData[k][1] + 1; j++) {
						lstReportID.addElement(new Integer(naEventLinkData[k][j + 1]));
					}
					pMessage.put(Integer.toString(naEventLinkData[k][0]), lstReportID);
				}
				pMessage.put("CEIDList", lstCEID);
			}
			SendS2F35(pMessage);
		} else if (strSFMsg.equals("S2F35Delete") == true) // Link Event Report
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 35);
			rsp.SetListItem(2);
			rValue = 0;
			rsp.SetU4Item(rValue); // DataID
			rsp.SetListItem(0); // L n
			XComSend(rsp, "S2F35(Delete)");
		} else if (strSFMsg.equals("S2F37") == true) {
			MyHashtable pMessage = new MyHashtable();
			int nTotalCEIDCnt = 15;

			int naCEIDData[] = { 1, 2, 3, 51, 52, 53, 54, 55, 56, 57, 151, 152, 251, 261, 260 };

			// 2015.02.02 by KYK
			if (loadEnabledCEIDConfig(pMessage) == false) {
				Vector lstCEID = new Vector();
				pMessage.put("CEIDCount", new Integer(nTotalCEIDCnt));
				for (int k = 0; k < nTotalCEIDCnt; k++) {
					lstCEID.addElement(new Integer(naCEIDData[k]));
				}
				pMessage.put("CEIDList", lstCEID);
				pMessage.put("SubMsg", "Enable");
			}
			SendS2F37(pMessage);
		} else if (strSFMsg.equals("S2F37Enable(All)") == true) {
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 37);
			rsp.SetListItem(2); // L 2
			nValue = 1;
			rsp.SetBoolItem(nValue == 1); // CEED
			rsp.SetListItem(0); // L m

			XComSend(rsp, "S2F37(Enable)");
		} else if (strSFMsg.equals("S2F37Disable(All)") == true) {
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 37);
			rsp.SetListItem(2); // L 2
			nValue = 0;
			rsp.SetBoolItem(nValue == 1); // CEED
			rsp.SetListItem(0); // L m

			XComSend(rsp, "S2F37(Disable)");
		} else if (strSFMsg.equals("S2F41(PAUSE)") == true) {
			MyHashtable pMessage = new MyHashtable();
			pMessage.put("RCMD", "PAUSE");
			SendS2F41(pMessage);
		} else if (strSFMsg.equals("S2F41(RESUME)") == true) {
			MyHashtable pMessage = new MyHashtable();
			pMessage.put("RCMD", "RESUME");
			SendS2F41(pMessage);
		}
		// /////////////////////////////////////////////////////////////////////////////////////
		// STREAM 5: Exception(Alarm) Reporting
		// /////////////////////////////////////////////////////////////////////////////////////
		else if (strSFMsg.equals("S5F0") == true) // Abort Transaction
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(5, 0); // (Message Id,
			// S,F,byte,deviceId)
			XComSend(rsp, "S5F0");
		} else if (strSFMsg.equals("S5F3") == true) // Alarm Enabled
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(5, 3); // (Message Id,
			// S,F,byte,deviceId)

			rsp.SetListItem(2);
			nShort = 1;
			rsp.SetBinaryItem(nShort);
			rValue = 1;
			rsp.SetU4Item(rValue);
			XComSend(rsp, "S5F3");
		} else if (strSFMsg.equals("S5F3EnableAll") == true) // Alarm Enabled
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(5, 3); // (Message Id,
			// S,F,byte,deviceId)

			rsp.SetListItem(2);
			nShort = 128;
			rsp.SetBinaryItem(nShort);
			rValue = 0;
			rsp.SetU4Item(rValue);
			XComSend(rsp, "S5F3");
		} else if (strSFMsg.equals("S5F3DisableAll") == true) // Alarm Disabled
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(5, 3); // (Message Id,
			// S,F,byte,deviceId)

			rsp.SetListItem(2);
			nShort = 0;
			rsp.SetBinaryItem(nShort);
			rValue = 0;
			rsp.SetU4Item(rValue);
			XComSend(rsp, "S5F3");
		} else if (strSFMsg.equals("S5F5") == true) // Alarm List Request
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(5, 5); // (Message Id,
			// S,F,byte,deviceId)

			rsp.SetListItem(2);
			rValue = 1;
			rsp.SetU4Item(rValue);
			rValue = 2;
			rsp.SetU4Item(rValue);

			XComSend(rsp, "S5F5");
		}
		// /////////////////////////////////////////////////////////////////////////////////////
		// STREAM 6: Data Collection
		// /////////////////////////////////////////////////////////////////////////////////////
		else if (strSFMsg.equals("S6F0") == true) // Abort Transaction
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(6, 0); // (Message Id,
			// S,F,byte,deviceId)
			XComSend(rsp, "S6F0");
		}
		// /////////////////////////////////////////////////////////////////////////////////////
		// STREAM 9: System Errors
		// /////////////////////////////////////////////////////////////////////////////////////
		else if (strSFMsg.equals("S9F0") == true) // Abort Transaction
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(9, 0); // (Message Id,
			// S,F,byte,deviceId)
			XComSend(rsp, "S9F0");
		}
		// /////////////////////////////////////////////////////////////////////////////////////
		// STREAM 10: Terminal Service
		// /////////////////////////////////////////////////////////////////////////////////////
		else if (strSFMsg.equals("S10F0") == true) // Abort Transaction
		{
			UComMsg rsp = m_UCom.MakeSecsMsg(10, 0); // (Message Id,
			// S,F,byte,deviceId)
			XComSend(rsp, "S10F0");
		}
		// /////////////////////////////////////////////////////////////////////////////////////
		// Else other
		// /////////////////////////////////////////////////////////////////////////////////////
		else {
			Display("Undefined Msg ");
			nReturn = 1;
		}

		return nReturn;
	}

	// SXF0: Abort Transaction
	public short SendAbortTransaction(UComMsg msg) {
		short nReturn = 0;
		long lReplyMsgId;
		String strBuff;

		UComMsg rsp = m_UCom.MakeSecsMsg(msg.GetStream(), 0, msg.GetSysbytes());
		XComSend(rsp, "S" + msg.GetStream() + "F0");

		return 0;

	}

	// S1F3 : Selected SC Status Request(SSR)
	public short SendS1F3(MyHashtable pMessage) {
		String strTemp;
		MsgVector pSVIDList = new MsgVector();
		short nReturn = 0;
		long lMsgId;
		int lValue;

		pSVIDList = (MsgVector) pMessage.get("SVID");
		// naSVID[]={Num_Total SVIDs, SVIDs.......)
		UComMsg rsp = m_UCom.MakeSecsMsg(1, 3);

		rsp.SetListItem(pSVIDList.size()); // L m
		for (int i = 0; i < pSVIDList.size(); i++) {
			lValue = pSVIDList.toInt(i);
			rsp.SetU2Item(lValue); // SVIDs
		}

		XComSend(rsp, "S1F3");

		// 모든 내용을 삭제함
		pSVIDList.removeAllElements();

		return nReturn;
	}

	// S1F3 : Selected SC Status Request(SSR)
	public short SendS1F3(int nSVID) {
		String strTemp;
		short nReturn = 0;
		long lMsgId;
		int lValue;

		// naSVID[]={Num_Total SVIDs, SVIDs.......)
		UComMsg rsp = m_UCom.MakeSecsMsg(1, 3);

		rsp.SetListItem(1); // L m
		lValue = nSVID;
		rsp.SetU2Item(lValue); // SVIDs

		XComSend(rsp, "S1F3");

		m_strSVID = Integer.toString(nSVID);

		return nReturn;
	}

	// ---------------------------------------------------------------------------
	// /////////////////////////////////////////////
	// S2F33: Define Report
	// /////////////////////////////////////////////
	short SendS2F33(MyHashtable pMessage) {

		short nReturn = 0;
		long lMsgId;
		int lValue;
		long rValue;

		int nReportCount = pMessage.toInt("ReportCount", 0);
		UComMsg rsp = m_UCom.MakeSecsMsg(2, 33);
		rsp.SetListItem(2);
		rValue = 0;
		rsp.SetU4Item(rValue); // DataID 0 Fixed
		rsp.SetListItem(nReportCount); // L m Number of Reports

		for (int k = 0; k < nReportCount; k++) {
			rsp.SetListItem(2); // L 2
			lValue = pMessage.toInt("ReportIDList", k);
			rsp.SetU2Item(lValue); // RPTID

			String sReportID = String.valueOf(lValue);
			int nVIDCount = pMessage.toInt(sReportID, 0);

			rsp.SetListItem(nVIDCount); // L m VID Count
			for (int j = 0; j < nVIDCount; j++) {
				lValue = pMessage.toInt(sReportID, j + 1);
				rsp.SetU2Item(lValue);
			}
		}

		XComSend(rsp, "S2F33");
		return nReturn;
	}

	// ---------------------------------------------------------------------------
	// /////////////////////////////////////////////
	// S2F35: Link Event Report
	// /////////////////////////////////////////////
	short SendS2F35(MyHashtable pMessage) {
		short nReturn = 0;
		long lMsgId;
		int lValue;
		long rValue;

		int nCEIDCount = pMessage.toInt("CEIDCount", 0);
		UComMsg rsp = m_UCom.MakeSecsMsg(2, 35);
		rsp.SetListItem(2);
		rValue = 0;
		rsp.SetU4Item(rValue); // DataID

		rsp.SetListItem(nCEIDCount); // L m Number of CEIDs
		for (int k = 0; k < nCEIDCount; k++) {
			rsp.SetListItem(2); // L 2
			lValue = pMessage.toInt("CEIDList", k);
			rsp.SetU2Item(lValue); // CEID

			String sCEID = String.valueOf(lValue);
			int nReportCount = pMessage.toInt(sCEID, 0);

			rsp.SetListItem(nReportCount); // L m
			for (int j = 0; j < nReportCount; j++) {
				lValue = pMessage.toInt(sCEID, j + 1);
				rsp.SetU2Item(lValue);
			}
		}

		XComSend(rsp, "S2F35");

		return nReturn;
	}

	// ---------------------------------------------------------------------------
	// ////////////////////////////////////////////////
	// S2F37: Event Enable/Disable Setting Acknowledge
	// ////////////////////////////////////////////////
	short SendS2F37(MyHashtable pMessage) {
		String strSubMsg;
		short nReturn = 0, nValue;
		long lMsgId;
		int lValue;
		long rValue;
		UComMsg rsp = m_UCom.MakeSecsMsg(2, 37);
		rsp.SetListItem(2); // L 2

		int nCEIDCount = pMessage.toInt("CEIDCount", 0);
		strSubMsg = pMessage.toString("SubMsg", 0);

		if (strSubMsg.equals("Enable") == true) {
			nValue = 1;
			rsp.SetBoolItem(nValue == 1); // CEED 1:Enable
		} else {
			nValue = 0;
			rsp.SetBoolItem(nValue == 1); // CEED 0:Disable
		}

		rsp.SetListItem(nCEIDCount); // L m

		for (int k = 0; k < nCEIDCount; k++) {
			lValue = pMessage.toInt("CEIDList", k);
			rsp.SetU2Item(lValue); // CEID
		}

		XComSend(rsp, "S2F37");
		return nReturn;
	}

	// S6F15: Event Report Request
	public short SendS6F15(MyHashtable pMessage) {
		long lMsgId;
		int lValue;
		short nReturn = 0;
		int nCEID;

		nCEID = pMessage.toInt("CEID", 0);

		UComMsg rsp = m_UCom.MakeSecsMsg(6, 15);
		lValue = nCEID;
		rsp.SetU2Item(lValue); // CEID

		XComSend(rsp, "S6F15");

		return nReturn;
	}

	// Send Other Messages
	// S2F13: Equipment Constant Request(ECR)
	public short SendS2F13(int naECID[]) {
		long lMsgId;
		int lValue;
		short nReturn = 0;

		// naECID[]={Num_Total ECIDs, ECIDs.......)
		UComMsg rsp = m_UCom.MakeSecsMsg(2, 13);
		rsp.SetListItem(naECID[0]); // L m

		for (int k = 0; k < naECID[0]; k++) {
			lValue = naECID[k];
			rsp.SetU2Item(lValue); // ECIDs
		}

		XComSend(rsp, "S2F13");

		return nReturn;
	}

	// S2F15: New Equipment Constant Send
	public short SendS2F15(int nNumECID, int naECID[][]) // int naECID[][2]
	{
		long lMsgId;
		int lValue;
		short nReturn = 0;

		// naECID[]={Num_Total ECIDs, ECIDs.......)
		UComMsg rsp = m_UCom.MakeSecsMsg(2, 15);
		rsp.SetListItem(nNumECID); // L m

		for (int k = 0; k < nNumECID; k++) {
			rsp.SetListItem(2); // L 2
			lValue = naECID[k][0];
			rsp.SetU2Item(lValue); // ECID
			lValue = naECID[k][1];
			rsp.SetU2Item(lValue); // ECV
		}

		XComSend(rsp, "S2F15");
		return nReturn;

	}

	// S2F29: Equipment Constant Namelist Request
	public short SendS2F29(int naECID[]) {
		long lMsgId;
		int lValue;
		short nReturn = 0;

		// naECID[]={Num_Total ECIDs, ECIDs.......)
		UComMsg rsp = m_UCom.MakeSecsMsg(2, 29);
		rsp.SetListItem(naECID[0]); // L m

		for (int k = 0; k < naECID[0]; k++) {
			lValue = naECID[k];
			rsp.SetU2Item(lValue); // ECIDs
		}

		XComSend(rsp, "S2F29");
		return nReturn;
	}

	public int XComSend(UComMsg msg, String strMessage) {
		int nReturn = 0;

		boolean bRequest = true;

		if (msg.GetSystemBytes() == 0)
			bRequest = true;
		else
			bRequest = false;

		if (m_UCom.Send(msg, bRequest)) {
			if (strMessage.equals("S1F1") == false)
				Display("Send " + strMessage + " successfully");

			nReturn = 0;
		} else {
			Display("Send " + strMessage + " failed");

			//Display("Fail to send " + (String) strMessage + "  (" + nReturn + ") : " + XComErr.getText(nReturn));
			//2005.10.10 by N.Y.K
			//Send 실패시

			/*
			 * if(nReturn==-10047) { //1. 기존에 관련 Alarm이 있는지 확인 // AlarmText ==
			 * "XCOM -10047 IPC ERROR" 인 놈이 있는지 확인 int nAlarmCount =
			 * CheckExistXcomIPCAlarm(); //2. 없으면 Alarm 등록 if (nAlarmCount == 0)
			 * { // AlarmID : nReturn(-10047) // AlarmEventType : XCOM //
			 * AlarmText : XCOM -10047 IPC ERROR // ErrorID | Alarm Count
			 * RegisterAlarm("", "", nReturn, "1", "["+m_scsMain.m_strTSCID
			 * +"] XCOM -10047 IPC ERROR", "XCOM", ""); //3.1.1 Alarm Text
			 * Update UpdateXcomIPCAlarmText(); //3.1.2 System Shut down
			 * System.exit(1);
			 * 
			 * }
			 * 
			 * //3. 있으면 Alarm Update OR System Shutdown(); // else if
			 * (nAlarmCount > 0) // { // //3.1 Alarm이 2개이상 존재하면 Alarm Text를
			 * "XCOM -10047 IPC ERROR RESTARTED"으로 바꾸고 // // System Shut down //
			 * if (nAlarmCount >= 2) // { // //3.1.1 Alarm Text Update //
			 * UpdateXcomIPCAlarmText(); // //3.1.2 System Shut down //
			 * System.exit(1); // } // //3.2 Alarm이 1개이면 Alarm Count 증가 // else
			 * // { // //3.2.1 Update Alarm Count //
			 * UpdateXcomIPCAlarmCount(nAlarmCount); // } // } }
			 */

			// 2005.09.29 by N.Y.K 라인에서 문제가 되어 막기로 함
			//Send RepTSCControlStatus(FAIL);
			/*
			 * MyHashtable pReportInfo = new MyHashtable();
			 * pReportInfo.put("EventName", "TSCControlStatusFAIL");
			 * pReportInfo.put("ONOFFLINEACK", new Integer(nReturn));
			 * pReportInfo.put("ControlStatus", "ONLINEFAIL");
			 * pReportInfo.put("CommunicationStatus", m_strCommState);
			 * pReportInfo.put("StatusChangedTime", GetCurrentTime());
			 * SendReportToDM(pReportInfo);
			 */

			nReturn = -1;
		}
		return nReturn;
	}

	public int XComSendTest(boolean bSendSuccess) {
		int nReturn = 0;

		if (bSendSuccess == true) {
			Display("Send successfully");
		} else {
			Display("Fail to send -10047 ");
			nReturn = -10047;
			// 2005.10.10 by N.Y.K
			// Send 실패시
			if (nReturn == -10047) {
				// 1. 기존에 관련 Alarm이 있는지 확인
				// AlarmText == "XCOM -10047 IPC ERROR" 인 놈이 있는지 확인
				int nAlarmCount = CheckExistXcomIPCAlarm();
				// 2. 없으면 Alarm 등록
				if (nAlarmCount == 0) {
					// AlarmID : nReturn(-10047)
					// AlarmEventType : XCOM
					// AlarmText : XCOM -10047 IPC ERROR
					// ErrorID | Alarm Count
					RegisterAlarm("", "", nReturn, "1", "[" + m_ocsMain.m_strTSCID + "] XCOM -10047 IPC ERROR", "XCOM", "");
					// 3.1.1 Alarm Text Update
					UpdateXcomIPCAlarmText();
					// 3.1.2 System Shut down
					System.exit(1);

				}
				/*
				 * //3. 있으면 Alarm Update OR System Shutdown(); else
				 * if(nAlarmCount>0) { //3.1 Alarm이 3번 이상 발생했으면 Alarm Text를
				 * "XCOM -10047 IPC ERROR RESTARTED"으로 바꾸고 // System Shut down
				 * if(nAlarmCount>=2) { //3.1.1 Alarm Text Update
				 * UpdateXcomIPCAlarmText(); //3.1.2 System Shut down
				 * System.exit(1); } //3.2 Alarm 1개 or 2개이면 Alarm Count 증가 else
				 * { //3.2.1 Update Alarm Count
				 * UpdateXcomIPCAlarmCount(nAlarmCount); } }
				 */
			}
		}
		return nReturn;
	}

	public int XComSendTest2(boolean bSendSuccess) {
		int nReturn = 0;
		// 3. 기존에 관련 Alarm이 있는지 확인
		// AlarmText == "XCOM 225 XI_CLOSE" 인 놈이 있는지 확인
		int nAlarmCount = CheckExistXcomXIAlarm();
		// 4. 없으면 Alarm 등록
		if (nAlarmCount == 0) {
			// AlarmID : nReturn(225)
			// AlarmEventType : XCOM
			// AlarmText : XCOM 225 XI_CLOSE
			// ErrorID | Alarm Count
			RegisterAlarm("", "", 225, "1", "[" + m_ocsMain.m_strTSCID + "] XCOM 225 XI CLOSE", "XCOM", "");
			// 4.1 Alarm Text Update
			UpdateXcomXIAlarmText();
			// 4.2 System Shut down
			System.exit(1);
		}

		return nReturn;
	}

	public int CheckExistXcomIPCAlarm() {
		int nReturn = 0;
		// XcomIPC Alarm 이 존재하는지 확인하고
		// 존재하면 AlarmCount(ErrorID)의 값이 몇인지를 반환한다.

		String strSql = "SELECT * FROM Alarm Where AlarmText='[" + m_ocsMain.m_strTSCID + "] XCOM -10047 IPC ERROR'";
		ResultSet rs = null;
		try {
			rs = m_ocsMain.m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				// 존재하면 AlarmCount(ErrorID)의 값이 몇인지를 반환한다.
				int nAlarmCount = Integer.parseInt(rs.getString("ERRORID"));
				return nAlarmCount;
			}
		} catch (SQLException e) {
			String strLog = "CheckExistXcomIPCAlarm() - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_ocsMain.m_dbFrame.CloseRecord(rs);
			}
		}

		return nReturn;
	}

	public void UpdateXcomIPCAlarmText() {
		// 3.1.1 Alarm Text Update
		String strSql = "UPDATE Alarm SET AlarmText='[" + m_ocsMain.m_strTSCID + "]";
		strSql += " XCOM -10047 RESTARTED' WHERE AlarmText='[" + m_ocsMain.m_strTSCID + "] XCOM -10047 IPC ERROR'";
		try {
			m_ocsMain.m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateXcomIPCAlarmText() - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		}
	}

	public void UpdateXcomIPCAlarmCount(int nAlarmCount) {
		String sAlarmCount = "";
		sAlarmCount = String.valueOf(nAlarmCount + 1);
		// 3.2.1 Update Alarm Count
		String strSql = "UPDATE Alarm SET ErrorID='" + sAlarmCount + "' WHERE AlarmText='[" + m_ocsMain.m_strTSCID + "] XCOM -10047 IPC ERROR'";
		try {
			m_ocsMain.m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateXcomIPCAlarmText() - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		}
	}

	public int CheckExistXcomXIAlarm() {

		int nReturn = 0;
		// XcomIPC Alarm 이 존재하는지 확인하고
		// 존재하면 AlarmCount(ErrorID)의 값이 몇인지를 반환한다.

		String strSql = "SELECT * FROM Alarm Where AlarmText='[" + m_ocsMain.m_strTSCID + "] XCOM 225 XI CLOSE'";
		ResultSet rs = null;
		try {
			rs = m_ocsMain.m_dbFrame.GetRecord(strSql);
			if ((rs != null) && (rs.next())) {
				// 존재하면 AlarmCount(ErrorID)의 값이 몇인지를 반환한다.
				int nAlarmCount = Integer.parseInt(rs.getString("ERRORID"));
				return nAlarmCount;
			}
		} catch (SQLException e) {
			String strLog = "CheckExistXcomXIAlarm() - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		} finally {
			if (rs != null) {
				m_ocsMain.m_dbFrame.CloseRecord(rs);
			}
		}

		return nReturn;
	}

	public void UpdateXcomXIAlarmText() {
		// 3.1.1 Alarm Text Update
		String strSql = "UPDATE Alarm SET AlarmText='[" + m_ocsMain.m_strTSCID + "]";
		strSql += " XCOM 225 RESTARTED' WHERE AlarmText='[" + m_ocsMain.m_strTSCID + "] XCOM 225 XI CLOSE'";
		try {
			m_ocsMain.m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "UpdateXcomXIAlarmText() - Exception: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		}
	}

	void RegisterAlarm(String strUnitType, String strUnitID, int nAlarmID, String strErrorID, String strAlarmText, String strAlarmEventType, String strMicroTrCmdID) {

		// Alarm Table에 등록
		String strSql = "INSERT INTO Alarm (AlarmID, AlarmText, AlarmSetTime, ErrorID, AlarmEventType)";
		strSql += " VALUES (" + String.valueOf(nAlarmID) + ", '";
		strSql += strAlarmText + "', TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS'), '";
		strSql += strErrorID + "', '";
		strSql += strAlarmEventType + "')";
		try {
			m_ocsMain.m_dbFrame.ExecSQL(strSql);
		} catch (SQLException e) {
			String strLog = "RegisterAlarm - SQLException: " + e.getMessage();
			m_ocsMain.WriteLog(strLog);
		}
	}

	public int SetSEMStatus() {
		int nRtnCode = 1;
		int nRtn;
		MyHashtable pRecordInfo = new MyHashtable();

		// DB에 Status 저장
		// DB 모듈 미완성

		// Display정보변경
		pRecordInfo.put("TSC", m_strTSCName);
		pRecordInfo.put("HSMSState", m_strHSMSState);
		pRecordInfo.put("CommState", m_strCommState);
		pRecordInfo.put("ControlState", m_strControlState);
		pRecordInfo.put("SCState", m_strSCState);
		pRecordInfo.put("AlarmState", m_strAlarmState);

		return nRtnCode;
	}

	public void UpdateStatus(String strEventName) {
		String strMessage;

		if (strEventName.equals("EquipmentOffline") == true) {
			m_strCommState = "COMMUNICATING";
			m_strSCState = "TSC_NONE";
			m_strControlState = "EQ_OFFLINE";
			Display("REMOTE_ONLINE -> EQ_OFFLINE ");
		} else if (strEventName.equals("ControlStatusLocal") == true) {
			m_strCommState = "COMMUNICATING";
			m_strSCState = "TSC_NONE";
			m_strControlState = "LOCAL_ONLINE";
			Display("REMOTE_ONLINE -> LOCAL_ONLINE ");
		} else if (strEventName.equals("ControlStatusRemote") == true) {
			m_strCommState = "COMMUNICATING";
			m_strSCState = "TSC_INIT";
			m_strControlState = "REMOTE_ONLINE";

		} else if (strEventName.equals("AlarmCleared") == true) {
			m_strAlarmState = "NO_ALARMS";
			Display("ALARMS -> NO_ALARMS ");
		} else if (strEventName.equals("Alarmset") == true) {
			m_strAlarmState = "ALARMS";
			Display("NO_ALARMS -> ALARMS ");
		} else if (strEventName.equals("TSCAutoCompleted") == true) {
			if (m_strSCState.equals("TSC_PAUSING") == true) {
				Display("TSC_PAUSING -> TSC_AUTO ");
			} else if (m_strSCState.equals("TSC_PAUSED") == true) {
				Display("TSC_PAUSING -> TSC_AUTO ");
			}
			m_strSCState = "TSC_AUTO";
			m_strCommState = "COMMUNICATING";
			m_strControlState = "REMOTE_ONLINE";
		} else if (strEventName.equals("TSCAutoInitiated") == true) {
			Display("TSC_NONE -> TSC_INIT ");
			m_strSCState = "TSC_INIT";
		} else if (strEventName.equals("TSCPauseCompleted") == true) {
			Display("TSC_PAUSING -> TSC_PAUSED ");
			m_strSCState = "TSC_PAUSED";
		} else if (strEventName.equals("TSCPaused") == true) {
			Display("TSC_INIT -> TSC_PAUSED ");
			m_strSCState = "TSC_PAUSED";
		} else if (strEventName.equals("TSCPauseInitiated") == true) {
			Display("TSC_AUTO -> TSC_PAUSING ");
			m_strSCState = "TSC_PAUSING";
		}

		SetSEMStatus();
	}

	// Port List 관련
	public void SetPortList(MyHashtable pMessageInfo) {
		String strCarrierLocID, strSEMCarrierLoc;
		int nTotalCarrierLoc;

		nTotalCarrierLoc = pMessageInfo.toInt("NumOfCarrierLoc", 0);

		m_pPortListInfo.put("TotalCarrierLoc", new Integer(nTotalCarrierLoc));
		for (int k = 0; k < nTotalCarrierLoc; k++) {
			strCarrierLocID = pMessageInfo.toString("CarrierLocID", k);
			strSEMCarrierLoc = GetSEMCarrierLoc(strCarrierLocID);

			// 양쪽에서 모두 찾을 수 있도록
			m_pPortListInfo.put(strCarrierLocID, strSEMCarrierLoc);
			m_pPortListInfo.put(strSEMCarrierLoc, strCarrierLocID);

			m_pPortListInfo.put("CarrierLocID", strCarrierLocID);
			m_pPortListInfo.put("SEMCarrierLoc", strSEMCarrierLoc);

			// Display Port List To Memo
		}
		Display("Set PortList Successfully!!");
	}

	public void ShowPortList() {
		// 추가작업-ViewPortListForm - > Show();
		m_pPortListInfo.put("TSC", m_strTSCName);
		// 추가작업-ViewPortListForm - > ShowPortList(m_pPortListInfo);
	}

	public String GetSEMCarrierLoc(String strCarrierLocID) {
		String strSEMCarrierLoc;

		// Parsing
		String strTemp = "";
		String strItems[] = new String[2];
		int nItemCount = 0;

		for (int k = 1; k <= 2; k++) {
			strItems[k] = "";
		}

		// 1. Get MessageName and Items From sData
		for (int k = 0; k < strCarrierLocID.length(); k++) {
			if (strCarrierLocID.charAt(k) == ':') {
				strItems[nItemCount++] = strTemp;
				strTemp = "";
			} else {
				strTemp += strCarrierLocID.charAt(k);
			}

			if (k == strCarrierLocID.length() - 1) {
				strItems[nItemCount++] = strTemp;
			}
		}

		// SEMCarrierLoc
		strSEMCarrierLoc = strItems[1];

		// Remove []
		for (int k = 0; k < strSEMCarrierLoc.length(); k++) {
			if (strSEMCarrierLoc.charAt(k) == '[') {
				strSEMCarrierLoc = strSEMCarrierLoc.substring(0, k - 1);
				break;
			}
		}
		return strSEMCarrierLoc;
	}

	String ChangeToDMFormat(String strCarrierLocID) {
		if (strCarrierLocID == null) {
			return "";
		} else if (strCarrierLocID.equals("") == true) {
			return strCarrierLocID;
		} else {
			String strDMCarrierLoc = "";

			/*
			 * //CarrierLocID Type이 Port이면 if (strCarrierLocID.substring(0,
			 * m_strEqpName.length()) == m_strEqpName) strDMCarrierLoc =
			 * m_pPortListInfo.toString(strCarrierLocID, 0);
			 */

			// if (strDMCarrierLoc.equals("") == true)
			// int nPos = strCarrierLocID.indexOf("_");
			// strDMCarrierLoc = m_strEqpName + ":" + strCarrierLocID;
			// strDMCarrierLoc = strCarrierLocID.substring(0, nPos) + ":" + strCarrierLocID;
			strDMCarrierLoc = m_ocsMain.GetOwnerDevice2(strCarrierLocID) + ":" + strCarrierLocID;
			return strDMCarrierLoc;
		}
	}

	public String ChangeToSEMFormat(String strCarrierLocID) {
		String strSEMCarrierLoc = "";
		String strEqpName = "";

		for (int k = 0; k < strCarrierLocID.length(); k++) {
			if (strCarrierLocID.charAt(k) == ':') {
				strEqpName = strCarrierLocID.substring(0, k);
				break;
			}
		}

		strSEMCarrierLoc = m_pPortListInfo.toString(strCarrierLocID, 0);

		if (strSEMCarrierLoc.equals("") == true) {
			strSEMCarrierLoc = strCarrierLocID.substring(strEqpName.length() + 1);
		}

		return strSEMCarrierLoc;
	}

	// Send To DataManager
	public void SendReportToDM(MyHashtable pReportInfo) {
		String strMessage;
		String strEventName;

		// 추가작업-if (GUIForm - > m_strDisplay.equals("Display1") == true)
		{
			strEventName = pReportInfo.toString("EventName", 0);
			// Send RepTSCControlStatus(ONLINE/OFFLINE/FAIL)
			if ((strEventName.equals("ControlStatusRemote") == true) || (strEventName.equals("EquipmentOffline") == true) || (strEventName.equals("TSCControlStatusFAIL") == true)
					|| (strEventName.equals("CommunicationEvent") == true)) {
				pReportInfo.put("MessageName", "RepTSCControlStatus");
				pReportInfo.put("TSC", m_strTSCName);

				// 각 이벤트를 올리는 부분에서 control status 를 세팅할것
				if (m_strCommState.equals("COMMUNICATING") == false) {
					pReportInfo.put("CommunicationStatus", "NOT_COMMUNICATING");
				} else {
					pReportInfo.put("CommunicationStatus", m_strCommState);
				}

				if (strEventName.equals("ControlStatusRemote") == true) {
					pReportInfo.put("ControlStatus", "ONLINE");
				} else if (strEventName.equals("EquipmentOffline") == true) {
					pReportInfo.put("ControlStatus", "OFFLINE");
				} else if (strEventName.equals("CommunicationEvent") == true) {
					pReportInfo.put("ControlStatus", "");
				}

				if (strEventName.equals("TSCControlStatusFAIL") == false) {
					pReportInfo.put("ONOFFLINEACK", new Integer(1));
				}

				String strControlStatus, strCommStatus;
				strControlStatus = pReportInfo.toString("ControlStatus", 0);
				strCommStatus = pReportInfo.toString("CommunicationStatus", 0);
				Display("[SEM I/F -> D.M] RepTSCControlStatus(" + strControlStatus + "/" + strCommStatus + ")");
				m_ocsMain.CallProc(pReportInfo);
			}
			// Send RepTSCStatus(INIT/PAUSING/PAUSED/AUTO)
			else if ((strEventName.equals("TSCAutoCompleted") == true) || (strEventName.equals("TSCAutoInitiated") == true) || (strEventName.equals("TSCPauseCompleted") == true)
					|| (strEventName.equals("TSCPaused") == true) || (strEventName.equals("TSCPauseInitiated") == true) || (strEventName.equals("TSCStatusFAIL") == true)
					|| (strEventName.equals("TSCNone") == true)) {
				pReportInfo.put("MessageName", "RepTSCStatus");
				pReportInfo.put("TSC", m_strTSCName);

				if (strEventName.equals("TSCAutoCompleted") == true) {
					pReportInfo.put("TSCStatus", "AUTO");
				} else if (strEventName.equals("TSCAutoInitiated") == true) {
					pReportInfo.put("TSCStatus", "INIT");
				} else if (strEventName.equals("TSCPauseCompleted") == true) {
					pReportInfo.put("TSCStatus", "PAUSED");
				} else if (strEventName.equals("TSCPaused") == true) {
					pReportInfo.put("TSCStatus", "PAUSED");
				} else if (strEventName.equals("TSCPauseInitiated") == true) {
					pReportInfo.put("TSCStatus", "PAUSING");
				} else if (strEventName.equals("TSCNone") == true) {
					pReportInfo.put("TSCStatus", "NONE");
				}

				if (strEventName.equals("TSCStatusFAIL") == false) {
					pReportInfo.put("HCACK", new Integer(4));
				}

				String strTSCStatus;
				strTSCStatus = pReportInfo.toString("TSCStatus", 0);
				Display("[SEM I/F -> D.M] RepTSCStatus(" + strTSCStatus + ")");
				m_ocsMain.CallProc(pReportInfo);

			}
			// Send
			// RepMicroTCStatus(QUEUED/TRANSFERRING/COMPLETED/FAIL/ABORTING/ABORTED/CANCELING/CANCELED)
			else if ((strEventName.equals("TransferAbortCompleted") == true) || (strEventName.equals("TransferAbortFailed") == true) || (strEventName.equals("TransferAbortInitiated") == true)
					|| (strEventName.equals("TransferCancelCompleted") == true) || (strEventName.equals("TransferCancelFailed") == true) || (strEventName.equals("TransferCancelInitiated") == true)
					|| (strEventName.equals("TransferCompleted") == true) || (strEventName.equals("TransferInitiated") == true) || (strEventName.equals("TransferPaused") == true)
					|| (strEventName.equals("TransferResumed") == true) || (strEventName.equals("MicroTCStatusFAIL") == true) || (strEventName.equals("MicroTCQUEUED") == true)
					|| (strEventName.equals("Transferring") == true)) {
				pReportInfo.put("MessageName", "RepMicroTCStatus");
				pReportInfo.put("TSC", m_strTSCName);

				if ((strEventName.equals("MicroTCStatusFAIL") == false) && (strEventName.equals("MicroTCQUEUED") == false)) {
					pReportInfo.put("MicroTCStatus", strEventName);
				}

				if (strEventName.equals("MicroTCStatusFAIL") == false) {
					pReportInfo.put("HCACK", new Integer(4));
				}

				String strMicroTCStatus, strMicroTrCmdID;
				strMicroTCStatus = pReportInfo.toString("MicroTCStatus", 0);
				strMicroTrCmdID = pReportInfo.toString("MicroTrCmdID", 0);
				Display("[SEM I/F -> D.M] RepMicroTCStatus(" + strMicroTCStatus + "/" + strMicroTrCmdID + ")");
				m_ocsMain.CallProc(pReportInfo);

			}
			// Send RepCarrierStatus(CarrierRemoved/CarrierInstalled)
			else if ((strEventName.equals("CarrierRemoved") == true) || (strEventName.equals("CarrierInstalled") == true)) {
				pReportInfo.put("MessageName", "RepCarrierStatus");
				pReportInfo.put("TSC", m_strTSCName);
				pReportInfo.put("CarrierStatus", strEventName);

				String strCarrierStatus;
				strCarrierStatus = pReportInfo.toString("CarrierStatus", 0);
				Display("[SEM I/F -> D.M] RepCarrierStatus(" + strCarrierStatus + ")");

				m_ocsMain.CallProc(pReportInfo);
			}
			// Send
			// RepVehicleStatus(VehicleArrived/VehicleAcquireStarted/VehicleAcquireCompleted/
			// VehicleAssigned/VehicleDepositStarted/VehicleDepositCompleted/
			// VehicleInstalled/VehicleRemoved/VehicleUnassigned,VehicleDeparted)
			else if ((strEventName.equals("VehicleArrived") == true) || (strEventName.equals("VehicleAcquireStarted") == true) || (strEventName.equals("VehicleAcquireCompleted") == true)
					|| (strEventName.equals("VehicleAssigned") == true) || (strEventName.equals("VehicleDepositStarted") == true) || (strEventName.equals("VehicleDepositCompleted") == true)
					|| (strEventName.equals("VehicleInstalled") == true) || (strEventName.equals("VehicleRemoved") == true) || (strEventName.equals("VehicleUnassigned") == true)
					|| (strEventName.equals("VehicleDeparted") == true)) {

				pReportInfo.put("MessageName", "RepVehicleStatus");
				pReportInfo.put("TSC", m_strTSCName);
				pReportInfo.put("VehicleStatus", strEventName);

				Display("[SEM I/F -> D.M] RepVehicleStatus(" + strEventName + ")");
				m_ocsMain.CallProc(pReportInfo);
			}
			// Send RepAlarm(Items: UnitID, AlarmStatus, AlarmID)
			else if ((strEventName.equals("AlarmCleared") == true) || (strEventName.equals("AlarmSet") == true)) {

				pReportInfo.put("MessageName", "RepAlarm");
				pReportInfo.put("TSC", m_strTSCName);
				pReportInfo.put("AlarmCode", m_strAlarmCode);
				pReportInfo.put("AlarmID", new Integer(Integer.parseInt(m_strAlarmID)));
				pReportInfo.put("AlarmText", m_strAlarmText);
				// pReportInfo.put("VehicleID", "");
				pReportInfo.put("VehicleState", new Integer(0));
				pReportInfo.put("ErrorID", m_strAlarmText);
				pReportInfo.put("MicroTrCmdID", "");

				if (strEventName.equals("AlarmCleared") == true) {
					pReportInfo.put("AlarmStatus", "AlarmCleared");
				} else if (strEventName.equals("AlarmSet") == true) {
					pReportInfo.put("AlarmStatus", "AlarmSet");
				}

				Display("[SEM I/F -> D.M] RepAlarm(" + strEventName + "/" + m_strAlarmID + "/" + m_strAlarmText + ")");

				m_ocsMain.CallProc(pReportInfo);
			}
			// Send RepEnhancedCarrierInfo(Items: CarrierID, VehicleID, CarrierLocID,
			// InstallTime)
			else if (strEventName.equals("RepEnhancedCarrierInfo") == true) {
				pReportInfo.put("MessageName", "RepEnhancedCarrierInfo");
				pReportInfo.put("TSC", m_strTSCName);

				String strCarrierID = "", strLog = "";
				int nCarrierQty;
				nCarrierQty = pReportInfo.toInt("CarrierQty", 0);
				for (int k = 0; k < nCarrierQty; k++) {
					strCarrierID = pReportInfo.toString("CarrierID", k);
					strLog = strLog + "/" + strCarrierID;
				}
				Display("[SEM I/F -> D.M] RepEnhancedCarrierInfo(" + strLog + ")");

				m_ocsMain.CallProc(pReportInfo);
			}
			// Send RepEnhancedTrCmd(Items: CarrierLocID)
			else if (strEventName.equals("RepEnhancedTrCmd") == true) {
				pReportInfo.put("MessageName", "RepEnhancedTrCmd");
				pReportInfo.put("TSC", m_strTSCName);

				String strCommandID = "", strLog = "";
				int nCommandQty;
				nCommandQty = pReportInfo.toInt("CommandQty", 0);
				for (int k = 0; k < nCommandQty; k++) {
					strCommandID = pReportInfo.toString("MicroTrCmdID", k);
					strLog = strLog + "/" + strCommandID;
				}
				Display("[SEM I/F -> D.M] RepEnhancedTrCmd(" + strLog + ")");
				m_ocsMain.CallProc(pReportInfo);

			}
			// Send RepVehicleInfo(Items: CarrierLocID)
			else if (strEventName.equals("RepVehicleInfo") == true) {
				pReportInfo.put("MessageName", "RepVehicleInfo");
				pReportInfo.put("TSC", m_strTSCName);

				String strVehicleID = "";
				String strLog = "";
				int nVehicleQty;
				nVehicleQty = pReportInfo.toInt("VehicleQty", 0);
				for (int k = 0; k < nVehicleQty; k++) {
					strVehicleID = pReportInfo.toString("VehicleID", k);
					strLog = strLog + "/" + strVehicleID;
				}
				Display("[SEM I/F -> D.M] RepVehicleInfo(" + strLog + ")");
				m_ocsMain.CallProc(pReportInfo);
			}

		}
	}

	// Others
	public String GetCEventName(long lValue) {
		String strReturn;

		if (lValue == 1) {
			strReturn = "EquipmentOffline";
		} else if (lValue == 2) {
			strReturn = "ControlStatusLocal";
		} else if (lValue == 3) {
			strReturn = "ControlStatusRemote";
		} else if (lValue == 51) {
			strReturn = "AlarmCleared";
		} else if (lValue == 52) {
			strReturn = "AlarmSet";
		} else if (lValue == 53) {
			strReturn = "TSCAutoCompleted";
		} else if (lValue == 54) {
			strReturn = "TSCAutoInitiated";
		} else if (lValue == 55) {
			strReturn = "TSCPauseCompleted";
		} else if (lValue == 56) {
			strReturn = "TSCPaused";
		} else if (lValue == 57) {
			strReturn = "TSCPauseInitiated";
		} else if (lValue == 101) {
			strReturn = "TransferAbortCompleted";
		} else if (lValue == 102) {
			strReturn = "TransferAbortFailed";
		} else if (lValue == 103) {
			strReturn = "TransferAbortInitiated";
		} else if (lValue == 104) {
			strReturn = "TransferCancelCompleted";
		} else if (lValue == 105) {
			strReturn = "TransferCancelFailed";
		} else if (lValue == 106) {
			strReturn = "TransferCancelInitiated";
		} else if (lValue == 107) {
			strReturn = "TransferCompleted";
		} else if (lValue == 108) {
			strReturn = "TransferInitiated";
		} else if (lValue == 109) {
			strReturn = "TransferPaused";
		} else if (lValue == 110) {
			strReturn = "TransferResumed";
		} else if (lValue == 111) {
			strReturn = "Transferring";
		} else if (lValue == 151) {
			strReturn = "CarrierInstalled";
		} else if (lValue == 152) {
			strReturn = "CarrierRemoved";
		} else if (lValue == 201) {
			strReturn = "VehicleArrived";
		} else if (lValue == 202) {
			strReturn = "VehicleAcquireStarted";
		} else if (lValue == 203) {
			strReturn = "VehicleAcquireCompleted";
		} else if (lValue == 204) {
			strReturn = "VehicleAssigned";
		} else if (lValue == 205) {
			strReturn = "VehicleDeparted";
		} else if (lValue == 206) {
			strReturn = "VehicleDepositStarted";
		} else if (lValue == 207) {
			strReturn = "VehicleDepositCompleted";
		} else if (lValue == 208) {
			strReturn = "VehicleInstalled";
		} else if (lValue == 209) {
			strReturn = "VehicleRemoved";
		} else if (lValue == 210) {
			strReturn = "VehicleUnassigned";
		} else if (lValue == 254) {
			strReturn = "OperatorInitiatedAction";
		} else if (lValue == 10) {
			strReturn = "MessageRecognition";
		} else if (lValue == 11) {
			strReturn = "EstablishIntervalTimeChange";
		} else if (lValue == 251) {
			strReturn = "CarrierIDRead";
		} else if (lValue == 255) {
			strReturn = "CarrierIDReadMulti";
		} else {
			strReturn = "Undefined CEID";
		}

		return strReturn;
	}

	public String GetCurrentTime() {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Date curTime = new Date();
		String dateString = format.format(curTime).substring(0, 16);
		return dateString;
	}

	public void RepLocalStatus() {
		// /////////////////////////////////////////////
		// 1. Send Report Control Status To DataManager
		MyHashtable pReportInfo = new MyHashtable();
		pReportInfo.put("EventName", "EquipmentOffline");
		pReportInfo.put("StatusChangedTime", GetCurrentTime());
		SendReportToDM(pReportInfo);

		// ////////////////////////////////////////////
		// 2. Send Report TSC Status To DataManager
		MyHashtable pReportInfo2 = new MyHashtable();
		pReportInfo2.put("EventName", "TSCNone");
		pReportInfo2.put("StatusChangedTime", GetCurrentTime());
		SendReportToDM(pReportInfo2);
	}

	/**
	 * S1F13TimerTimerProc : 수행
	 */
	class S1F13TimerProc extends TimerTask {
		public void run() {
			if ((m_strHSMSState.equals("HSMS_SELECTED") == true) && (m_strCommState.equals("COMMUNICATING") == false)) {
				S1F13Timer.cancel();
				SendMsg("S1F13");
			}

		}
	}

	/**
	 * ConnectionCheckTimerProc : 수행
	 */
	class ConnectionCheckTimerProc extends TimerTask {
		public void run() {
			// ResponseCheck
			if (m_bResponse == true) {
				m_bResponse = false;
				// Send S1F1 to SCS(MCP7)
				SendMsg("S1F1");
			} else {
				// Set Status
				// Status Info Initialize
				m_strCommState = "COMM_DISABLED";
				m_strControlState = "EQ_OFFLINE";
				m_strSCState = "TSC_NONE";
				m_strAlarmState = "NO_ALARMS";
				SetSEMStatus();

				// Send Offline Event To DM.
				RepLocalStatus();
				ConnectionCheckTimer.cancel(); // .Enabled = false;
				m_bResponse = true;
				m_strGetEnhancedDataState = "INIT";
				m_bS2F33DeleteCompleted = false;
				m_bS2F37DisableCompleted = false;
				m_bS5F3EnableAllCompleted = false;
			}
		}
	}

	void S1F13TimerRestart() {
		S1F13Timer.cancel();
		S1F13Timer = null;
		S1F13TimerTask = null;
		S1F13Timer = new java.util.Timer();
		S1F13TimerTask = new S1F13TimerProc();
		S1F13Timer.schedule(S1F13TimerTask, 0, 30000);
	}

	void ConnectionCheckTimerRestart() {
		ConnectionCheckTimer.cancel();
		ConnectionCheckTimer = null;
		ConnectionCheckTimerTask = null;
		ConnectionCheckTimer = new java.util.Timer();
		ConnectionCheckTimerTask = new ConnectionCheckTimerProc();
		ConnectionCheckTimer.schedule(ConnectionCheckTimerTask, 0, 15000);
	}

	boolean ReceivedMSG(UComMsg msg) {
		String sStr, sJis8, strBuff, strEventName = "";
		// String wsData;
		String BSTRBuff;
		// char szMsg[255];
		String strMsg;
		int nList;
		int nValue = 0;
		int nSubPartNo;
		short nBuff;
		int nSubPartNo_2;
		short nErrorCode, nSize, nWbit;
		int i, nReturn = 0, nReportNo, nRPTID, nShelfNo;
		int lValue;
		long lReplyMsgId, lCount, lSize;
		long rValue;
		// struct time TempTime;
		// struct date TempDate;

		String strSFMsg = "S" + msg.GetStream() + "F" + msg.GetFunc();

		// #######################################################
		// #######################################################
		// ## STREAM 1
		// #######################################################
		// #######################################################

		if (strSFMsg.equals("S1F1")) {
			if (m_strCommState.equals("COMMUNICATING")) {
				// Send a reply message.
				UComMsg rsp = m_UCom.MakeSecsMsg(1, 2, msg.GetSysbytes());
				rsp.SetListItem(0);
				XComSend(rsp, "S1F2");
			} else {
				// Ignore... Send S1F0
				SendAbortTransaction(msg);
			}
		} else if (strSFMsg.equals("S1F2")) {
			m_strControlState = "REMOTE_ONLINE";
			if (m_strSCState.equals("TSC_NONE")) {
				m_strSCState = "TSC_INIT";
			}
			SetSEMStatus();
			m_bResponse = true;
		}
		// 2011.07.25 by KYK : STBC 변경부
		else if (strSFMsg.equals("S1F4")) {
			// Report ControlState
			if (m_strSVID.equals("6")) {
				if (m_strOnlineSequenceState.equals("INIT")) {
					if (m_bAutoInitialize == true)
						SendS1F3(64);
				}
			}
			// Report STBState
			else if (m_strSVID.equals("64")) {
				// Get SCStatus
				nList = msg.GetListItem(); // SVID갯수
				lValue = msg.GetU2Item();

				// Send Report TSC Status To DataManager
				MyHashtable pReportInfo = new MyHashtable();
				if (lValue == 1) { // INIT        
					pReportInfo.put("EventName", "TSCAutoInitiated");
				} else if (lValue == 2) { // PAUSED        
					pReportInfo.put("EventName", "TSCPaused");
				} else if (lValue == 3) { // AUTO        
					if (m_strOnlineSequenceState.equals("INIT")) {
						pReportInfo.put("EventName", "TSCPaused");
					} else {
						pReportInfo.put("EventName", "TSCAutoCompleted");
					}
				} else if (lValue == 4) { // PAUSING        
					pReportInfo.put("EventName", "TSCPauseInitiated");
				}
				pReportInfo.put("StatusChangedTime", GetCurrentTime());
				SendReportToDM(pReportInfo);

				// Set Flag for OlineSequence
				if (m_strOnlineSequenceState.equals("INIT")) {

					if (m_bAutoInitialize == true)
						SendS1F3(118);
				}
			}
			// Report CurrentPortState
			else if (m_strSVID.equals("118")) {
				// Set Flag for OlineSequence
				if (m_strOnlineSequenceState.equals("INIT")) {
					if (m_bAutoInitialize == true)
						SendS1F3(51);

				}
			} else if (m_strSVID.equals("51")) {
				// 2013.01.04 by KYK : OCS로부터 수신된 데이터가 있을때와 없을때 구분  
				boolean isValid = false;
				msg.GetListItem(); // L 1
				if (msg.GetListItem() == 0) { // L 0 ?
					isValid = true;
				}
				if (m_strOnlineSequenceState.equals("INIT")) {
					// 2013.01.04 by KYK
					//            sendS2F49("INSTALLLIST");
					sendS2F49("INSTALLLIST", isValid);
					m_strSVID = "";
					//          m_strOnlineSequenceState = "DONE";
					//          MyHashtable pRESUMEMsg = new MyHashtable();
					//          pRESUMEMsg.put("RCMD", "RESUME");
					//          if (m_bAutoInitialize == true)
					//            SendS2F41(pRESUMEMsg);
				}
			}
		}
		// //////////////////////////////////////////////////////////////////////////
		// S1F13 : Establish Communication Request
		else if (strSFMsg.equals("S1F13") == true) {
			// Send Response message as S1F14
			UComMsg rsp = m_UCom.MakeSecsMsg(1, 14, msg.GetSysbytes());
			nValue = 0; // COMACK, Bin, 0 -> ACK
			rsp.SetListItem(2);
			rsp.SetBinaryItem(nValue);
			rsp.SetListItem(0);
			XComSend(rsp, "S1F14");

			m_strCommState = "COMMUNICATING";
			m_strSCState = "TSC_NONE";
			m_strControlState = "EQ_OFFLINE";
			SetSEMStatus();
			Display("COMM_DIABLED -> COMMUNICATING ");
			if (m_bAutoInitialize == true)
				SendMsg("S1F17");
		}
		// //////////////////////////////////////////////////////////////////////////
		// S1F14 : Establish Communication Request ACK
		else if (strSFMsg.equals("S1F14") == true) {
			nList = msg.GetListItem();
			nValue = msg.GetBinaryItem(); // COMACK

			if (nValue == 0 && (m_strCommState.equals("NOT_COMMUNICATING") == true) || (m_strCommState.equals("COMMUNICATING") == true)) {
				m_strCommState = "COMMUNICATING";
				m_strSCState = "TSC_NONE";
				m_strControlState = "EQ_OFFLINE";
				SetSEMStatus();
				Display("COMM_DIABLED -> COMMUNICATING ");

				MyHashtable pReportInfo = new MyHashtable();
				pReportInfo.put("EventName", "CommunicationEvent");
				pReportInfo.put("StatusChangedTime", GetCurrentTime());
				pReportInfo.put("ONOFFLINEACK", new Integer(nValue));
				pReportInfo.put("CommunicationStatus", m_strCommState);
				SendReportToDM(pReportInfo);
				if (m_bAutoInitialize == true)
					SendMsg("S1F17");
			} else {
				// ERROR
				Display("Invalid ACK response or Invalid Communication Status");
				// Send RepTSCControlStatus(FAIL);
				MyHashtable pReportInfo = new MyHashtable();
				pReportInfo.put("EventName", "TSCControlStatusFAIL");
				pReportInfo.put("ONOFFLINEACK", new Integer(3));
				pReportInfo.put("ControlStatus", "ONLINEFAIL");
				pReportInfo.put("CommunicationStatus", m_strCommState);
				pReportInfo.put("StatusChangedTime", GetCurrentTime());
				SendReportToDM(pReportInfo);
			}
		}
		// //////////////////////////////////////////////////////////////////////////
		// S1F16 : OFFLINE Request ACK
		else if (strSFMsg.equals("S1F16") == true) {
			nValue = msg.GetBinaryItem(); // OFLACK

			if ((nValue == 0) && ((m_strCommState.equals("NOT_COMMUNICATING") == true) || (m_strCommState.equals("COMMUNICATING") == true))) {
				// Change Communicaion Status as COMMUNICATING
				m_strCommState = "COMMUNICATING";
				// Change TSCStatus into NONE
				m_strSCState = "TSC_NONE";
				// Chabge Control Status as EQ_OFFLINE State
				m_strControlState = "EQ_OFFLINE";
				// Set SEM Status
				SetSEMStatus();
				Display("REMOTE_ONLINE -> EQ_OFFLINE ");
			} else {
				if (nValue == 1) {
					// ERROR
					Display("Invalid Communication Status");
				} else if (nValue == 2) {
					Display("Equipment Already Offline!!");
				} else {
					Display("Undefined OFLACK!!");
				}
				// Send RepTSCControlStatus(FAIL);
				MyHashtable pReportInfo = new MyHashtable();
				pReportInfo.put("EventName", "TSCControlStatusFAIL");
				pReportInfo.put("ONOFFLINEACK", new Integer(nValue));
				pReportInfo.put("ControlStatus", "OFFLINEFAIL");
				pReportInfo.put("CommunicationStatus", m_strCommState);
				pReportInfo.put("StatusChangedTime", GetCurrentTime());
				SendReportToDM(pReportInfo);
			}
		}
		// //////////////////////////////////////////////////////////////////////////
		// S1F18 : ONLINE Request ACK
		else if (strSFMsg.equals("S1F18") == true) {
			nValue = msg.GetBinaryItem(); // OFLACK

			if ((nValue == 0) || (nValue == 2)) {
				if (nValue == 2) {
					Display("Equipment Already Online!!");
				} else {
					Display("EQ_OFFLINE -> REMOTE_ONLINE");
				}

				m_strCommState = "COMMUNICATING";
				m_strSCState = "TSC_INIT";
				m_strControlState = "REMOTE_ONLINE";
				SetSEMStatus();

				MyHashtable pReportInfo = new MyHashtable();
				pReportInfo.put("EventName", "ControlStatusRemote");
				pReportInfo.put("StatusChangedTime", GetCurrentTime());
				pReportInfo.put("ONOFFLINEACK", new Integer(nValue));
				pReportInfo.put("CommunicationStatus", m_strCommState);
				SendReportToDM(pReportInfo);

				if (m_bAutoInitialize == true)
					SendMsg("S2F31");
			} else {
				if (nValue == 1) {
					Display("Invalid Communication Status");
				} else {
					Display("Undefined OLACK!!");
				}
				// Send RepTSCControlStatus(FAIL);
				MyHashtable pReportInfo = new MyHashtable();
				pReportInfo.put("EventName", "TSCControlStatusFAIL");
				pReportInfo.put("ONOFFLINEACK", new Integer(nValue));
				pReportInfo.put("ControlStatus", "ONLINEFAIL");
				pReportInfo.put("CommunicationStatus", m_strCommState);
				pReportInfo.put("StatusChangedTime", GetCurrentTime());
				SendReportToDM(pReportInfo);
			}
		} else if (strSFMsg.equals("S2F16") == true) {
			// 2012.03.21 by KYK : STBC2.0 Test 위해 아래두줄 주석처리
			if (m_bAutoInitialize == true)
				SendMsg("S2F37Disable(All)");
			//SendMsg("S5F3EnableAll");
		}
		// //////////////////////////////////////////////////////////////////////////
		// S2F17 : Date & Time Request
		else if (strSFMsg.equals("S2F17") == true) {
			// Send a reply message.
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 18, msg.GetSysbytes());

			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			Date curTime = new Date();
			String dateString = format.format(curTime).substring(0, 16);

			rsp.SetAsciiItem(dateString);
			if ((nReturn = XComSend(rsp, "S2F18")) != 0) {
				Display("XCom Send() Fails..[S2F18]");
			} else {
				Display("Reply S2F18 successfully");
			}
		} else if (strSFMsg.equals("S2F25") == true) {
			BSTRBuff = msg.GetAsciiItem();
			strBuff = BSTRBuff;

			Display("Received Loopback Diagnostic Request: String: " + strBuff);

			// Send Response message as S2F26
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 26, msg.GetSysbytes());
			String strLoopback = "Received Loopback ACK";
			rsp.SetAsciiItem(strLoopback);

			if ((nReturn = XComSend(rsp, "S2F26")) == 0) {
				Display("Reply S2F26 successfully");
			} else {
				// Send S1F0
				Display("XCom Send() Fails.. So send S2F0 [S2F26]");
				SendAbortTransaction(msg);
			}

		} else if (strSFMsg.equals("S2F30") == true) {
			nSubPartNo = msg.GetListItem(); // ECID 수
			for (i = 0; i < nSubPartNo; i++) {
				nSubPartNo_2 = msg.GetListItem(); // ECID 수
				lValue = msg.GetU2Item();
				Display("ECID: " + lValue);
				BSTRBuff = msg.GetAsciiItem();
				Display("ECNAME: " + BSTRBuff);
				lValue = msg.GetU2Item();
				Display("ECMIN: " + lValue);
				lValue = msg.GetU2Item();
				Display("ECMAX: " + lValue);
				lValue = msg.GetU2Item();
				Display("ECDEF: " + lValue);
				BSTRBuff = msg.GetAsciiItem();
				Display("UNITID: " + BSTRBuff);
			}

		} else if (strSFMsg.equals("S2F32") == true) {
			if (m_bAutoInitialize == true)
				SendMsg("S2F15");
		} else if (strSFMsg.equals("S2F34") == true) {
			nValue = msg.GetBinaryItem();
			if (nValue == 0) {

				if (m_bS2F33DeleteCompleted == false) {
					if (m_bAutoInitialize == true)
						SendMsg("S2F33");
					m_bS2F33DeleteCompleted = true;
				} else {
					if (m_bAutoInitialize == true)
						SendMsg("S2F35");
				}
			} else {
				Display("Error >> DRACK(Define Report ACK) = " + nValue);
				Display("1: denied, insufficient space");
				Display("2: denied, incorrect format");
				Display("3: denied, at least one RPTID is already defined");
				Display("4: denied, at least one VID is already defined");
			}
		} else if (strSFMsg.equals("S2F36") == true) {
			nValue = msg.GetBinaryItem();
			if (nValue == 0) {
				SendMsg("S2F37");
			} else {
				Display("Error >> LRACK(Link Report ACK) = " + nValue);
				Display("1: denied, insufficient space");
				Display("2: denied, incorrect format");
				Display("3: denied, at least one CEID link is already defined");
				Display("4: denied, there isn't at least one CEID");
				Display("5: denied, there isn't at least one RPTID");
			}
		} else if (strSFMsg.equals("S2F38") == true) {
			nValue = msg.GetBinaryItem();
			if (nValue == 0) {
				if (m_bS2F37DisableCompleted == false) {
					m_bS2F37DisableCompleted = true;
					SendMsg("S2F33Delete");
				} else if (m_bAutoInitialize == true)
					SendMsg("S5F3DisableAll");
				//SendS1F3(73);
			} else {
				Display("Error >> ERACK(Enalbed Report ACK) = " + nValue);
				Display("1: denied, there isn't at least one CEID");
			}
		} else if (strSFMsg.equals("S2F42") == true) {
			nList = msg.GetListItem(); // L,2

			nValue = msg.GetBinaryItem();

			// if(
			Display("HCACK:" + nValue);
			if ((nValue == 5) && (m_strCurrentTrCmdType.equals("RESUME") == true)) {
				m_strSCState = "TSC_AUTO";
				SetSEMStatus();
				Display("   FAIL : already requested!!(nValue==5)");
				Display("   m_strCurrentTrCmdType" + m_strCurrentTrCmdType);

				MyHashtable pReportInfo = new MyHashtable();
				pReportInfo.put("EventName", "TSCAutoCompleted");
				pReportInfo.put("StatusChangedTime", GetCurrentTime());
				SendReportToDM(pReportInfo);
			} else if (nValue != 4) {
				if (nValue == 1) {
					Display("   FAIL : command doesn't exist!!");
				} else if (nValue == 2) {
					Display("   FAIL : currently not able to execute!!");
				} else if (nValue == 3) {
					Display("   FAIL : at least one parameter isn't valid!!");
				} else if (nValue == 5) {
					Display("   FAIL : already requested!!");
				} else if (nValue == 6) {
					Display("   FAIL : object doesn't exist!!");
				} else if (nValue == 65) {
					Display("   FAIL : Unrecognized CarrierId!!");
				} else if (nValue == 66) {
					Display("   FAIL : Double CarrierId!!");
				} else if ((nValue == 67) || (nValue == 68)) {
					Display("   FAIL : Source is NG!!");
				} else if ((nValue == 69) || (nValue == 70)) {
					Display("   FAIL : Dest is NG!!");
				} else if ((nValue == 71) || (nValue == 72)) {
					Display("   FAIL : Shelf is NG!!");
				} else {
					Display("RCV S2F42 : Undefined HCACK!!");
				}

				// Get Send MessageName
				String strMessageName;
				strMessageName = m_pTempRecordInfo.toString("MessageName", 0);

				if ((m_strCurrentTrCmdType.equals("CANCEL") == true) || (m_strCurrentTrCmdType.equals("ABORT") == true) || (m_strCurrentTrCmdType.equals("RETRY") == true)) {
					// Send RepMicroTCStatus(FAIL);
					MyHashtable pReportInfo = new MyHashtable();
					pReportInfo.put("EventName", "MicroTCStatusFAIL");
					pReportInfo.put("HCACK", new Integer(nValue));
					pReportInfo.put("MicroTrCmdID", m_strCurrentTrCmdID);

					// 2005.10.28 by N.Y.K 수정
					String strCarrierID = GetCarrierIDFromMircoTrCmdId(m_strCurrentTrCmdID);
					pReportInfo.put("CarrierID", strCarrierID.trim());

					pReportInfo.put("StatusChangedTime", GetCurrentTime());
					if (m_strCurrentTrCmdType == "CANCEL") {
						pReportInfo.put("MicroTCStatus", "CANCELFAIL");
					} else if (m_strCurrentTrCmdType.equals("ABORT") == true) {
						pReportInfo.put("MicroTCStatus", "ABORTFAIL");
					}
					SendReportToDM(pReportInfo);

				} else if (strMessageName.equals("ReqTSCStatusChange") == true) {
					// Send RepTSCStatus(FAIL);
					MyHashtable pReportInfo = new MyHashtable();
					pReportInfo.put("EventName", "TSCStatusFAIL");
					pReportInfo.put("HCACK", new Integer(nValue));
					pReportInfo.put("TSCStatus", "FAIL");
					pReportInfo.put("StatusChangedTime", GetCurrentTime());
					SendReportToDM(pReportInfo);
				} else if (strMessageName.equals("ReqSDOperation") == true) {
					String strOperationType, strCarrierLocID, strCarrierID;

					strOperationType = m_pTempRecordInfo.toString("OperationType", 0);
					strCarrierLocID = m_pTempRecordInfo.toString("CarrierLocID", 0);
					strCarrierID = m_pTempRecordInfo.toString("CarrierID", 0);

					// Send RepSDChanged(FAIL);
					MyHashtable pReportInfo = new MyHashtable();
					pReportInfo.put("EventName", "SDChangedFAIL");
					pReportInfo.put("SDStatus", "FAIL");
					pReportInfo.put("CarrierID", strCarrierID);
					pReportInfo.put("OperationType", strOperationType);
					pReportInfo.put("CarrierLocID", strCarrierLocID);
					pReportInfo.put("HCACK", new Integer(nValue));
					pReportInfo.put("StatusChangedTime", GetCurrentTime());
					SendReportToDM(pReportInfo);
				} else if (strMessageName.equals("ReqPortModeChange") == true) {
					// Send RepPortModeChanged(FAIL);
					MyHashtable pReportInfo = new MyHashtable();
					pReportInfo.put("EventName", "PortModeChangedFAIL");
					pReportInfo.put("HCACK", new Integer(nValue));
					pReportInfo.put("StatusChangedTime", GetCurrentTime());
					SendReportToDM(pReportInfo);
				}
			} else // HCACK==4인경우 정상
			{
				if ((m_strCurrentTrCmdType.equals("CANCEL") == true) || (m_strCurrentTrCmdType.equals("ABORT") == true) || (m_strCurrentTrCmdType.equals("RETRY") == true)) {

					// Send RepMicroTCStatus(QUEUED);
					MyHashtable pReportInfo = new MyHashtable();
					pReportInfo.put("EventName", "MicroTCQUEUED");
					pReportInfo.put("HCACK", new Integer(nValue));
					pReportInfo.put("MicroTrCmdID", m_strCurrentTrCmdID);
					// 2005.10.28 by N.Y.K 수정
					String strCarrierID = GetCarrierIDFromMircoTrCmdId(m_strCurrentTrCmdID);
					pReportInfo.put("CarrierID", strCarrierID.trim());

					pReportInfo.put("StatusChangedTime", GetCurrentTime());
					if (m_strCurrentTrCmdType.equals("CANCEL") == true) {
						pReportInfo.put("MicroTCStatus", "CANCELQUEUED");
					} else if (m_strCurrentTrCmdType.equals("ABORT") == true) {
						pReportInfo.put("MicroTCStatus", "ABORTQUEUED");
					}
					SendReportToDM(pReportInfo);

					m_strCurrentTrCmdType = "";
					// SendTRCMDTimer.start();
				}
			}
		} else if (strSFMsg.equals("S2F50") == true) {
			MyHashtable pReportInfo = new MyHashtable();
			nList = msg.GetListItem(); // L,2
			nValue = msg.GetBinaryItem();
			Display("HCACK:" + nValue);

			if (nValue != 4) {
				if (nValue == 1) {
					Display("   FAIL : command doesn't exist!!");
				} else if (nValue == 2) {
					Display("   FAIL : currently not able to execute!!");
				} else if (nValue == 3) {
					Display("   FAIL : at least one parameter isn't valid!!");
				} else if (nValue == 5) {
					Display("   FAIL : already requested!!");
				} else if (nValue == 6) {
					Display("   FAIL : object doesn't exist!!");
				} else if (nValue == 65) {
					Display("   FAIL : Unrecognized CarrierId!!");
				} else if (nValue == 66) {
					Display("   FAIL : Double CarrierId!!");
				} else if ((nValue == 67) || (nValue == 68)) {
					Display("   FAIL : Source is NG!!");
				} else if ((nValue == 69) || (nValue == 70)) {
					Display("   FAIL : Dest is NG!!");
				} else if ((nValue == 71) || (nValue == 72)) {
					Display("   FAIL : Shelf is NG!!");
				} else {
					Display("RCV S2F50 : Undefined HCACK!!");
				}
				// Send RepMicroTCStatus(FAIL);
				pReportInfo.put("EventName", "MicroTCStatusFAIL");
				pReportInfo.put("HCACK", new Integer(nValue));
				pReportInfo.put("MicroTCStatus", "FAIL");
				pReportInfo.put("MicroTrCmdID", m_strCurrentTrCmdID);
				pReportInfo.put("CarrierID", m_strCurrentCarrierID);
				pReportInfo.put("StatusChangedTime", GetCurrentTime());
			} else {
				// Send RepMicroTCStatus(QUEUED);
				pReportInfo.put("EventName", "MicroTCQUEUED");
				pReportInfo.put("HCACK", new Integer(nValue));
				pReportInfo.put("MicroTCStatus", "QUEUED");
				pReportInfo.put("MicroTrCmdID", m_strCurrentTrCmdID);
				pReportInfo.put("CarrierID", m_strCurrentCarrierID);
				pReportInfo.put("StatusChangedTime", GetCurrentTime());

				// 2011.07.25 by KYK : STBC-MCS 수정
				m_strOnlineSequenceState = "DONE";
				MyHashtable pRESUMEMsg = new MyHashtable();
				pRESUMEMsg.put("RCMD", "RESUME");
				if (m_bAutoInitialize == true)
					SendS2F41(pRESUMEMsg);
			}
			SendReportToDM(pReportInfo);
		}
		// #######################################################
		// #######################################################
		// ## STREAM 5
		// #######################################################
		// #######################################################
		else if (strSFMsg.equals("S5F0") == true) {
		} else if (strSFMsg.equals("S5F1") == true) {
			nList = msg.GetListItem();
			int nALCD = msg.GetBinaryItem(); // ALCD

			rValue = msg.GetU4Item();
			BSTRBuff = msg.GetAsciiItem();
			strBuff = BSTRBuff;

			if (nALCD == (byte) 0x0086) {
				nValue = 134;
			} else if (nALCD == (byte) 0x0080) {
				nValue = 128;
			} else if (nALCD == (byte) 0x0000) {
				nValue = 0;
			}

			m_strAlarmCode = Integer.toString(nValue);
			m_strAlarmID = Integer.toString((int) rValue);
			m_strAlarmText = strBuff;

			Display("Alarm(" + nValue + "/" + rValue + "/" + strBuff + ")");

			// Send a reply message.
			UComMsg rsp = m_UCom.MakeSecsMsg(5, 2, msg.GetSysbytes());
			nValue = 0;
			rsp.SetBinaryItem(nValue); // B1 in TEL
			if ((nReturn = XComSend(rsp, "SF")) == 0) {
				Display("Reply S5F2 successfully");
			} else {
				Display("Fail to reply S5F2 (" + nReturn + ")");
			}

			// Alarm Cleared
			if (m_strAlarmCode.equals("0") == true) {
				// Send Report AlarmCleared Report to DataManager
				MyHashtable pReportInfo = new MyHashtable();
				pReportInfo.put("EventName", "AlarmCleared");
				pReportInfo.put("StatusChangedTime", GetCurrentTime());

				SendReportToDM(pReportInfo);
			}
		} else if (strSFMsg.equals("S5F4") == true) {
			if (m_bS5F3EnableAllCompleted == false) {
				SendMsg("S5F3EnableAll");
				m_bS5F3EnableAllCompleted = true;
			} else
				SendS1F3(6);
			// ConnectionCheckTimerRestart();
		}
		// #######################################################
		// #######################################################
		// ## STREAM 6
		// #######################################################
		// #######################################################
		else if (strSFMsg.equals("S6F0") == true) {

		} else if (strSFMsg.equals("S6F11")) {
			MyHashtable pReportInfo = new MyHashtable();

			if (msg.IsEOF() == false) {
				nSubPartNo = msg.GetListItem();
				rValue = msg.GetU4Item(); // DATAID

				if (m_strSEMFormat.equals("Daifuku") == true) {
					lValue = msg.GetU2Item(); // CEID
				} else if (m_strSEMFormat.equals("Brooks") == true) {
					lValue = (int) msg.GetU4Item(); // CEID
				} else { // 기본 형식.
					lValue = (int) msg.GetU2Item(); // CEID
				}

				strEventName = GetCEventName(lValue);
				Display("   EventName: <" + strEventName + ">");
				pReportInfo.put("EventName", strEventName);
				UpdateStatus(strEventName);
				nSubPartNo = msg.GetListItem(); // L,n
				nReportNo = nSubPartNo;
				// nReportNo = 1;

				for (int k = 0; k < nReportNo; k++) {
					nSubPartNo = msg.GetListItem(); // L,2
					lValue = msg.GetU2Item(); // RPTID
					nRPTID = lValue;
					pReportInfo.put("StatusChangedTime", GetCurrentTime());

					if (nRPTID == 1) {
						nSubPartNo = msg.GetListItem(); // L,1
						BSTRBuff = msg.GetAsciiItem();
						m_strEqpName = BSTRBuff.trim();
						pReportInfo.put("EqpName", BSTRBuff);
					} else if (nRPTID == 2) {
						int nTRCMD = 0;
						nSubPartNo = msg.GetListItem(); // L,4
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("EqpName", BSTRBuff);
						nSubPartNo = msg.GetListItem(); // L,3
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("MicroTrCmdID", BSTRBuff);
						lValue = msg.GetU2Item();
						pReportInfo.put("Priority", new Integer(lValue));
						lValue = msg.GetU2Item();
						pReportInfo.put("Replace", new Integer(lValue));
						nSubPartNo = msg.GetListItem(); // L,n
						nTRCMD = nSubPartNo;
						for (int k2 = 0; k2 < nTRCMD; k2++) {
							nSubPartNo = msg.GetListItem(); // L,2
							nSubPartNo = msg.GetListItem(); // L,3
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("CarrierID", BSTRBuff);
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("Source", ChangeToDMFormat(BSTRBuff));
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("Dest", ChangeToDMFormat(BSTRBuff));
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("CarrierLocID", ChangeToDMFormat(BSTRBuff));
						}
						lValue = msg.GetU2Item();
						pReportInfo.put("ResultCode", new Integer(lValue));
					} else if (nRPTID == 3) {
						int nTRCMD = 0;
						nSubPartNo = msg.GetListItem(); // L,3
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("EqpName", BSTRBuff);
						nSubPartNo = msg.GetListItem(); // L,3
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("MicroTrCmdID", BSTRBuff);
						lValue = msg.GetU2Item();
						pReportInfo.put("Priority", new Integer(lValue));
						lValue = msg.GetU2Item();
						pReportInfo.put("Replace", new Integer(lValue));
						nSubPartNo = msg.GetListItem(); // L,n
						nTRCMD = nSubPartNo;
						for (int k2 = 0; k2 < nTRCMD; k2++) {
							nSubPartNo = msg.GetListItem(); // L,2
							nSubPartNo = msg.GetListItem(); // L,3
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("CarrierID", BSTRBuff);
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("Source", ChangeToDMFormat(BSTRBuff));
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("Dest", ChangeToDMFormat(BSTRBuff));
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("CarrierLocID", ChangeToDMFormat(BSTRBuff));
						}
					} else if (nRPTID == 4) {
						nSubPartNo = msg.GetListItem(); // L,3
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("EqpName", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("CarrierID", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("CarrierLocID", ChangeToDMFormat(BSTRBuff));
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("VehicleID", BSTRBuff);
					} else if (nRPTID == 5) {
						nSubPartNo = msg.GetListItem(); // L,3
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("EqpName", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("VehicleID", BSTRBuff);
						nSubPartNo = msg.GetListItem(); // L,n
						for (int k2 = 0; k2 < nSubPartNo; k2++) {
							BSTRBuff = msg.GetAsciiItem();
							pReportInfo.put("CarrierLocID", ChangeToDMFormat(BSTRBuff));
							// PortList
						}
					} else if (nRPTID == 6) {
						nSubPartNo = msg.GetListItem(); // L,4
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("EqpName", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("VehicleID", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("CarrierID", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("CarrierLocID", ChangeToDMFormat(BSTRBuff));
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("MicroTrCmdID", BSTRBuff);
					} else if (nRPTID == 7) {
						nSubPartNo = msg.GetListItem(); // L,3
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("EqpName", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("VehicleID", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("MicroTrCmdID", BSTRBuff);
					} else if (nRPTID == 8) {
						nSubPartNo = msg.GetListItem(); // L,2
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("EqpName", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("VehicleID", BSTRBuff);
					} else if (nRPTID == 9) {
						nSubPartNo = msg.GetListItem(); // L,4
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("MicroTrCmdID", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("MicroTrCmdType", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("CarrierID", BSTRBuff);
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("Source", ChangeToDMFormat(BSTRBuff));
						BSTRBuff = msg.GetAsciiItem();
						pReportInfo.put("Dest", ChangeToDMFormat(BSTRBuff));
						lValue = msg.GetU2Item();
						pReportInfo.put("Priority", new Integer(lValue));

					}
				}
				// CarrierIDReadMulti 수신시 ActiveCarriers 요청
				if ("CarrierIDReadMulti".equals(strEventName)) {
					SendS1F3(51);
				}
			}

			// Send a reply message.
			UComMsg rsp = m_UCom.MakeSecsMsg(6, 12, msg.GetSysbytes());
			nValue = 0;
			rsp.SetBinaryItem(nValue); // B1 in TEL
			if ((nReturn = XComSend(rsp, "S6F12")) == 0) {
				// Display("Reply S6F12 successfully");
			} else {
				Display("Fail to reply S6F12 (" + nReturn + ")");
			}

			// 2005.09.29. by N.Y.K S6F12보내는것과 순서를 바꿈
			// Send Report To DATAMANAGER
			SendReportToDM(pReportInfo);

		} else if (strSFMsg.equals("S6F16") == true) {
		}

		m_UCom.CloseSecsMsg(msg);

		return true;
	}

	//2011.07.25 by KYK
	public int sendS2F41(String rCmd, String carrierId, String carrierLocId) {

		int result = 0;
		UComMsg rsp = m_UCom.MakeSecsMsg(2, 41);
		rsp.SetListItem(2); // L 2
		rsp.SetAsciiItem(rCmd);

		if ("INSTALL".equals(rCmd)) {
			if (carrierLocId == null) {
				return 1;
			} else {
				rsp.SetListItem(2); // L 2

				rsp.SetListItem(2); // L 2
				rsp.SetAsciiItem("CARRIERID");
				rsp.SetAsciiItem(carrierId);

				rsp.SetListItem(2); // L 2
				rsp.SetAsciiItem("CARRIERLOC");
				rsp.SetAsciiItem(carrierLocId);
			}
		} else if ("REMOVE".equals(rCmd)) {
			if (carrierLocId == null) {
				return 1;
			} else {
				rsp.SetListItem(1); // L 1

				rsp.SetListItem(2); // L 2
				rsp.SetAsciiItem("CARRIERID");
				rsp.SetAsciiItem(carrierId);
			}
		} else if ("IDREAD".equals(rCmd)) {
			if (carrierLocId == null) {
				return 1;
			} else {
				rsp.SetListItem(2); // L 2

				rsp.SetListItem(2); // L 2
				rsp.SetAsciiItem("CARRIERID");
				rsp.SetAsciiItem(carrierId);

				rsp.SetListItem(2); // L 2
				rsp.SetAsciiItem("PORTID");
				rsp.SetAsciiItem(carrierLocId);
			}
		} else {
			return 1;
		}

		XComSend(rsp, "S2F41(" + rCmd + ")");
		return result;
	}

	// 2013.01.04 by KYK : MCS->OCS 데이터 전송가능하도록
	// 2011.07.25 by KYK
	public int sendS2F49(String rCmd, boolean isValid) {

		int result = 0;
		int itemCnt = 0;
		HashMap<String, CarrierLoc> carrierLocMap = new HashMap<String, CarrierLoc>();

		if ("INSTALLLIST".equals(rCmd)) {
			// S1F3 (51:ActiveCarriers) 수신데이터 있을때는 itemCnt = 0 처리
			if (isValid) {
				carrierLocMap = carrierLocDao.getCarrierLocMap();
			}
			itemCnt = carrierLocMap.size();

			// Get COMMANDINFO Info.
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 49);
			rsp.SetListItem(4); // L m

			rsp.SetU2Item(0); // DataID
			rsp.SetAsciiItem(""); // OBJSPEC
			rsp.SetAsciiItem(rCmd); // RCMD
			rsp.SetListItem(1); // L 1

			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("CARRIERDATALIST");
			rsp.SetListItem(itemCnt); // L 0 (N)

			// Carrier개수만큼의 전송 명렁을 수행한다.
			for (CarrierLoc carrierLoc : carrierLocMap.values()) {
				if (carrierLoc == null) {
					continue;
				}
				rsp.SetListItem(2); // L 2
				rsp.SetAsciiItem("CARRIERDATA");

				rsp.SetListItem(2); // L 2

				rsp.SetListItem(2); // L 2
				rsp.SetAsciiItem("CARRIERID");
				rsp.SetAsciiItem(carrierLoc.getCarrierId());

				rsp.SetListItem(2); // L 2
				rsp.SetAsciiItem("CARRIERLOC");
				rsp.SetAsciiItem(carrierLoc.getCarrierLocId());
			}

			//		String carrierId = "";
			//		String carrierLocId = "";
			//	    for(int i =0; i < itemCnt; i++){
			//	    	rsp.SetListItem(2); // L 2
			//	    	rsp.SetAsciiItem("CARRIERDATA");
			//	    	
			//	    	rsp.SetListItem(2); // L 2
			//	    	
			//	    	rsp.SetListItem(2); // L 2
			//	    	rsp.SetAsciiItem("CARRIERID");
			//	    	rsp.SetAsciiItem(carrierId);        
			//	    	
			//	    	rsp.SetListItem(2); // L 2
			//	    	rsp.SetAsciiItem("CARRIERLOC");
			//	    	rsp.SetAsciiItem(carrierLocId);
			//	    }
			XComSend(rsp, "S2F49(" + rCmd + ")");
			return result;

		} else if ("IDREADLIST".equals(rCmd)) {
			// IDREADALL 만 하도록 함
			UComMsg rsp = m_UCom.MakeSecsMsg(2, 49);
			rsp.SetListItem(4); // L m

			rsp.SetU2Item(0); // DataID
			rsp.SetAsciiItem(""); // OBJSPEC
			rsp.SetAsciiItem(rCmd); // RCMD
			rsp.SetListItem(1); // L 1

			rsp.SetListItem(2); // L 2
			rsp.SetAsciiItem("PORTIDLIST"); // PORTLIST
			rsp.SetListItem(0); // L 0  all ports

			XComSend(rsp, "S2F49(" + rCmd + ")");
			return result;

		} else {
			result = 1;
			return result;
		}
	}

	public void OnSECSReceived(UComMsg umsg) {
		// MainForm.Memo에 받은 메세지를 출력...
		String strSFMsg = "S" + umsg.GetStream() + "F" + umsg.GetFunc();
		if (strSFMsg.equals("S1F2") == false) {
			Display("Received> " + strSFMsg);
		}
		// Receive Fuction
		ReceivedMSG(umsg);
	}

	public void OnSECSConnected() {
		// TODO Auto-generated method stub
		// ALARM_CONNECT...
		Display("[ALARM] HSMS connected alarm happens...");
		m_strHSMSState = "HSMS_SELECTED";
		m_strCommState = "NOT_COMMUNICATING";

		SetSEMStatus(); // DB에 저장하고 Display정보변경
		// S1F13TimerRestart();
	}

	public void OnSECSDisConnected() {
		// TODO Auto-generated method stub
		//	ALARM_NOT_CONNECT
		Display("[ALARM] HSMS not connected alarm happens...");
		m_strHSMSState = "TCPIP_NOT_CONNECTED";
		m_strCommState = "COMM_NONE";
		m_strControlState = "CONTROL_NONE";
		m_strSCState = "TSC_NONE";
		m_strAlarmState = "NO_ALARMS";
		SetSEMStatus(); //DB에 저장하고 Display정보변경

		//MCS로 Event를 날린다.
		MyHashtable pReportInfo = new MyHashtable();
		pReportInfo.put("EventName", "EquipmentOffline");
		SendReportToDM(pReportInfo);
		//////delete pReportInfo;

		m_strGetEnhancedDataState = "INIT";
		m_bS2F33DeleteCompleted = false;
		m_bS2F37DisableCompleted = false;
		m_bS5F3EnableAllCompleted = false;
		ConnectionCheckTimer.cancel();
		m_bResponse = true;

		//2004.0604 N.Y.K
		m_bCheckDisConnection = true;
		m_strOnlineSequenceState = "INIT";

	}

	public void OnSECST3TimeOut() {
		// TODO Auto-generated method stub
		// ALARM_T3_TIMEOUT...
		Display("[ALARM] T3 timeout alarm occurs...");
	}

	/**
	 * 2013.10.01
	 * 
	 * @param reportIdList
	 * @param vidList
	 * @return
	 */
	public boolean loadReportConfig(MyHashtable pMessage) {
		StringBuffer FilePathName = new StringBuffer();
		String strFileName = "STBReport.ini"; // 2015.01.30 by KYK : 이부분만 다르다

		// get current directory
		String Path = System.getProperty("user.dir");
		String Separator = System.getProperty("file.separator");
		FilePathName.append(Path).append(Separator).append(strFileName);

		File f;
		RandomAccessFile raf = null;
		boolean bReturn = true;

		try {
			f = new File(FilePathName.toString());
			raf = new RandomAccessFile(f, "r");
			int i;
			String line = "";
			int count = 0;
			Vector lstReportID = new Vector();
			while ((line = raf.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				String report[] = line.split(",");
				if (report.length > 0) {
					Vector lstVID = new Vector();
					String reportId = report[0].trim();
					lstReportID.addElement(new Integer(reportId));
					for (i = 1; i < report.length; i++) {
						lstVID.addElement(new Integer(report[i].trim()));
					}
					lstVID.insertElementAt(new Integer(report.length - 1), 0);
					pMessage.put(reportId, lstVID);
					count++;
				}
			}
			pMessage.put("ReportCount", new Integer(count));
			pMessage.put("ReportIDList", lstReportID);
			bReturn = true;
		} catch (Exception e) {
			e.printStackTrace();
			String strLog = "[DBAccessManager] LoadConfig - IOException: " + e.getMessage();
			bReturn = false;
			pMessage.clear();
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return bReturn;
	}

	/**
	 * 2013.10.01 by MYM
	 * 
	 * @param pMessage
	 * @return
	 */
	public boolean loadLinkEventReportConfig(MyHashtable pMessage) {
		StringBuffer FilePathName = new StringBuffer();
		String strFileName = "STBLinkEventReport.ini"; // 2015.01.30 by KYK : 이부분만 다르다

		// get current directory
		String Path = System.getProperty("user.dir");
		String Separator = System.getProperty("file.separator");
		FilePathName.append(Path).append(Separator).append(strFileName);

		File f;
		RandomAccessFile raf = null;
		boolean bReturn = true;

		try {
			f = new File(FilePathName.toString());
			raf = new RandomAccessFile(f, "r");
			int i;
			String line = "";
			int count = 0;
			Vector lstCEID = new Vector();
			while ((line = raf.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}

				String report[] = line.split(",");
				if (report.length > 0) {
					Vector lstReportID = new Vector();
					String ceId = report[0].trim();
					lstCEID.addElement(new Integer(ceId));
					for (i = 1; i < report.length; i++) {
						lstReportID.addElement(new Integer(report[i].trim()));
					}
					lstReportID.insertElementAt(new Integer(report.length - 1), 0);
					pMessage.put(ceId, lstReportID);
					count++;
				}
			}
			pMessage.put("CEIDCount", new Integer(count));
			pMessage.put("CEIDList", lstCEID);
			bReturn = true;
		} catch (Exception e) {
			String strLog = "[DBAccessManager] LoadConfig - IOException: " + e.getMessage();
			bReturn = false;
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return bReturn;
	}

	/**
	 * 2013.10.01 by MYM
	 * 
	 * @param pMessage
	 * @return
	 */
	public boolean loadEnabledCEIDConfig(MyHashtable pMessage) {
		StringBuffer FilePathName = new StringBuffer();
		String strFileName = "STBEnabledCEID.ini";

		// get current directory
		String Path = System.getProperty("user.dir");
		String Separator = System.getProperty("file.separator");
		FilePathName.append(Path).append(Separator).append(strFileName);

		File f;
		RandomAccessFile raf = null;
		boolean bReturn = true;

		try {
			f = new File(FilePathName.toString());
			raf = new RandomAccessFile(f, "r");
			int i;
			String line = "";
			int count = 0;
			Vector lstCEID = new Vector();
			while ((line = raf.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				String ceId[] = line.split(",");
				if (ceId.length > 0) {
					for (i = 0; i < ceId.length; i++) {
						lstCEID.addElement(new Integer(ceId[i].trim()));
						count++;
					}
				}
			}
			pMessage.put("CEIDCount", new Integer(count));
			pMessage.put("CEIDList", lstCEID);
			pMessage.put("SubMsg", "Enable");
			bReturn = true;
		} catch (Exception e) {
			String strLog = "loadEnabledCEIDConfig - Exception: " + e.getMessage();
			bReturn = false;
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return bReturn;
	}

}
