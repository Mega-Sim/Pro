package com.samsung.sem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.samsung.sem.items.AlarmItem;
import com.samsung.sem.items.CollectionEvent;
import com.samsung.sem.items.EquipmentConstant;
import com.samsung.sem.items.EventReport;
import com.samsung.sem.items.ReportItems;
import com.samsung.sem.ucominterface.UCom;
import com.samsung.sem.ucominterface.UComEventListener;
import com.samsung.sem.ucominterface.UComItem;
import com.samsung.sem.ucominterface.UComMsg;

/**
 * SEMCommon Abstract Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date 2011. 6. 21.
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

public abstract class SEMCommon implements UComEventListener, SEMConstant {	
	/** <vid, name> */
	protected HashMap<Integer, String> vIdOTable; 
	/** <reportId, eReportO> : reportId 와 해당 EventReport 객체 */
	protected HashMap<Integer, EventReport> reportIdOTable; 	
	/** <ceid, cEventO> : ceId 와 해당 CollectionEvent 객체 */
	protected HashMap<Integer, CollectionEvent> ceIdOTable; 	
	/** <ecId, ECO> : ecId 와 해당 EquipmentConstant 객체 */
	protected HashMap<Integer, EquipmentConstant> ecIdOTable; 	
	/** <ecId, ECO> : alarmId 와 해당 AlarmItem 객체 */
	protected HashMap<Long, AlarmItem> alarmIdOTable; 		

	/** <name, ceid> : eventname 를 키로 ceid 를 매칭하는 테이블 */
	protected HashMap<String, Integer> eventNameCeIdTable; 		
	protected ArrayList<AlarmItem> currentAlarmList;
	
	protected String hsmsStatus;
	protected String commStatus;
	protected String controlStatus;
	protected String tscStatus;
	protected String mdln, softRev;
	
	protected UCom uCom;
	protected EstCommThread estCommThread;
	protected long clockInterval = 3000;

	// 2013.06.28 by KYK
	protected boolean isAllAlarmEnabled = false;
	
	/*b***************************************************************************
	 *  abstract methods
	 *  SEMStandard 클래스를 상속받는 모듈에서 구현해야함
	 *****************************************************************************/

	/** initializeData */	
	public abstract boolean loadVidData();
	public abstract boolean loadEventReportData();
	public abstract boolean loadCollectionEventData();
	public abstract boolean loadAlarmData();
	public abstract boolean loadEcVData();
		
	/** receiveSxFy */
	public abstract void receiveS2F41(UComMsg msg);	
	public abstract void receiveS2F49(UComMsg msg);		
	
	/** set DB status */
	public abstract void setSEMStatusToDB();
	/** SECS History DB 化 */
	public abstract void updateSEMHistory(String received, int stream, int function, String strLog);

	/** write Log */
	public abstract void writeSEMLog(String log);	
	public abstract void traceS6F11History(String eventName, ReportItems reportItems);	
	public abstract void respondS1F17Status();
	/** setDataToSecsMsg */
	public abstract UComMsg setActiveCarriersToSecsMsg(UComMsg response);
	public abstract UComMsg setActiveCarriers2ToSecsMsg(UComMsg response);
	public abstract UComMsg setActiveTransfersToSecsMsg(UComMsg response);
	public abstract UComMsg setActiveVehiclesToSecsMsg(UComMsg response);
	public abstract UComMsg setCarrierInfoToSecsMsg(UComMsg response, String carrierId);
	public abstract UComMsg setEnhancedCarriersToSecsMsg(UComMsg response);
	public abstract UComMsg setEnhancedTransfersToSecsMsg(UComMsg response);
	public abstract UComMsg setEnhancedTransferCommandToSecsMsg(UComMsg response, String commandId);
	public abstract UComMsg setEnhancedCarrierInfoToSecsMsg(UComMsg response, String carrierId);
	public abstract UComMsg setTSCStatusToSecsMsg(UComMsg response);
	public abstract UComMsg setCurrentPortStateToSecsMsg(UComMsg response);
	public abstract UComMsg setTransferStateToSecsMsg(UComMsg response, String commandId);
	public abstract UComMsg setTransferInfoToSecsMsg(UComMsg response, String carrierId);
	public abstract UComMsg setInstallTimeToSecsMsg(UComMsg response, ReportItems reportItems);
	//public abstract UComMsg setCommandInfoToSecsMsg(UComMsg response, String commandId);
	public abstract UComMsg setCommandInfoToSecsMsg(UComMsg response, ReportItems reportItems);
	protected abstract int setEcVData(int vId, UComMsg message);
	
	// 2012.01.06 by PMM
	private static final String FORMAT_SECSMESSAGE_TRACE = "SECSLog";
	private static Logger SECSMessageTraceFormatLog = Logger.getLogger(FORMAT_SECSMESSAGE_TRACE);
	private SimpleDateFormat sdf2;
	private static final String FOUR_ZEROS = "0000";
	
	private static final String RECEIVE_SECS = "Recv SECS";
	private static final String SEND_SECS = "Send SECS";
	
	private boolean isFormattedLogUsed = false;
	
	/** Constructor of SEMCommon 
	 * SEMCommon 을 상속한 하위 클래스가 생성될 때 호출
	 * abstract class 로 단독 객체가 생성될 수 없음
	 */
	public SEMCommon() {			
		hsmsStatus = TCPIP_NOT_CONNECTED;
		commStatus = COMM_DISABLED;
		controlStatus = CONTROL_NONE;
		tscStatus = TSC_NONE;		

		vIdOTable = new HashMap<Integer, String>();
		ceIdOTable = new HashMap<Integer, CollectionEvent>(); 
		ecIdOTable = new  HashMap<Integer, EquipmentConstant>(); 
		reportIdOTable = new HashMap<Integer, EventReport>(); 
		alarmIdOTable = new HashMap<Long, AlarmItem>(); 	
		currentAlarmList = new ArrayList<AlarmItem>();
		eventNameCeIdTable = new HashMap<String, Integer>(); 

		uCom = new UCom(this);

		// 2012.01.06 by PMM
		sdf2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
	}

	/**
	 * EstablishCommunication
	 * OnSECS Connected (SECS Driver에 최초 연결시) Thread 실행
	 * MCS와 통신불가능상태이거나 이미 연결되었으면 Thread종료
	 * 통신가능상태인경우, S1F13 메시지 전송
	 * @author yk09.kang
	 * INNER CLASS FOR THREAD
	 */
	class EstCommThread extends Thread {
		public boolean isAlive = true;		
		public void isAlive(boolean alive) {
			isAlive = alive;
		}

		/**
		 * 
		 */
		public void run() {
			try {
				while (isAlive) {
					runProcess();
					sleep(clockInterval);
				}
			} catch (Exception e) {
				String strLog = "MainThread - Exception: " + e.getMessage();
				writeSEMLog(strLog);
			}
		}
	}

	/**
	 * 
	 */
	public void runProcess() {
		if (hsmsStatus.equals(TCPIP_NOT_CONNECTED) ||
				commStatus.equals(COMM_DISABLED) ||
				commStatus.equals(COMMUNICATING)) {
			stopEstCommTimer();
		} else {
			sendS1F13();
		}		
	}

	/**
	 * 
	 */
	public void startEstCommTimer() {
		estCommThread = new EstCommThread();
		estCommThread.start();
	}

	/**
	 * 
	 */
	public void stopEstCommTimer() {
		if (estCommThread != null) {
			estCommThread.isAlive(false);
			writeSEMLog("Establish Communication Timeout Stop");
		}
	}
	
	/*b***************************************
	 * Implements UComEventListner methods 
	 *****************************************/
	/**
	 * 
	 */
	public void onSECSConnected() {
//		manageOnSECSConnected();
		System.out.println("OnSECS Connected. ..");		
		writeSEMLog("[ALARM] HSMS connected alarm happens...");
		hsmsStatus = HSMS_SELECTED;
		commStatus = NOT_COMMUNICATING;
		setSEMStatusToDB();
		
		//Establish Communication Timeout Start
		startEstCommTimer();
	}
	
	/**
	 * 
	 */
	public void onSECSDisConnected() {
//		manageOnSECSDisConnected();
		System.out.println("OnSECS Disconnected. ..");	
		writeSEMLog("[ALARM] HSMS Not connected alarm happens...");
		
		hsmsStatus = TCPIP_NOT_CONNECTED;
		commStatus = NOT_COMMUNICATING;
		controlStatus = CONTROL_NONE;
		tscStatus = TSC_NONE;
		setSEMStatusToDB();
	}

	/**
	 * 
	 */
	public void onSECST3TimeOut() {
//		manageOnSECST3TimeOut();
		// 2013.06.28 by KYK
		writeSEMLog("[ALARM] HSMS T3TimeOut...");
	}

	/**
	 * 
	 * 
	 * @param message UComMsg
	 */
	public void onSECSReceived(UComMsg message) {
		// Receive UComMsg 
		receiveHSMSMessage(message);
	}

	/**
	 * Receive HSMS Message 
	 * UComMsg (SEComMsg 를 사용자재정의) Type 으로 수신  
	 * stream 계열별로 메소드 처리 
	 * @param message UComMsg
	 */
	public void receiveHSMSMessage(UComMsg message) {
		
		int stream = message.getStream();
		// 0. Check Communication Status
		checkCommStatus(message);		
		// 1. SxFy Message Received	
		switch (stream){			
			case 1:
				receiveS1Fy(message);
				break;
			case 2:
				receiveS2Fy(message);
				break;
			case 5:
				receiveS5Fy(message);
				break;
			case 6:
				receiveS6Fy(message);
				break;
			case 9:
				receiveS9Fy(message);
				break;
			default:
				//Abnormal
				break;
		}
		
		// 2012.01.06 by PMM
		if (isFormattedLogUsed) {
			traceFormattedSECSMessage(new SECSMessage(message, "", null, RECEIVE_SECS));
		}
	} 	
	
	/*********************************************
	 * STREAM 1 EQUIPMENT STATE
	 * @param message
	 ********************************************/	

	public void receiveS1Fy(UComMsg message) {
		int function = message.getFunction();
		switch (function){
			case 0:
				receiveS1F0(message);
				break;
			case 1:
				receiveS1F1(message);
				break;
			case 2:
				receiveS1F2(message);
				break;
			case 3:
				receiveS1F3(message);
				break;
			case 4:
				receiveS1F4(message);
				break;
			case 13:
				receiveS1F13(message);
				break;
			case 14:
				receiveS1F14(message);
				break;
			case 15:
				receiveS1F15(message);
				break;
			case 16:
				receiveS1F16(message);
				break;
			case 17:
				receiveS1F17(message);
				break;
			case 18:
				receiveS1F18(message);
				break;			
			case 19:
				receiveS1F19(message);
				break;
			default:
				//Abnormal
				break;
		}	
	}

	private void receiveS1F19(UComMsg message) {
		// TODO Auto-generated method stub
		
	}
	/**********************************************
	 * STREAM 2 EQUIPMENT CONTROL 
	 * @param message
	 ********************************************/

	public void receiveS2Fy(UComMsg message) {
		int function = message.getFunction();
		switch (function) {
			case 0:
				receiveS2F0(message);
				break;
			case 13:
				receiveS2F13(message);
				break;
			case 14:
				receiveS2F14(message);
				break;
			case 15:
				receiveS2F15(message);
				break;
			case 16:
				receiveS2F16(message);
				break;
			case 17:
				receiveS2F17(message);
				break;
			case 18:
				receiveS2F18(message);
				break;
			case 29:
				receiveS2F29(message);
				break;
			case 30:
				receiveS2F30(message);
				break;
			case 31:
				receiveS2F31(message);
				break;
			case 32:
				receiveS2F32(message);
				break;
			case 33:
				receiveS2F33(message);
				break;
			case 34:
				receiveS2F34(message);
				break;
			case 35:
				receiveS2F35(message);
				break;
			case 36:
				receiveS2F36(message);
				break;
			case 37:
				receiveS2F37(message);
				break;
			case 38:
				receiveS2F38(message);
				break;
			case 41:
				receiveS2F41(message);
				break;
			case 42:
				receiveS2F42(message);
				break;
			case 49:
				receiveS2F49(message);
				break;
			case 50:
				receiveS2F50(message);
				break;
			default:
				//Abnormal
				break;
		}
	}

	/**********************************************
	 * STREAM 5 EXCEPTION REPORTING
	 * @param message
	 ********************************************/
	public void receiveS5Fy(UComMsg message) {
		int function = message.getFunction();
		switch (function) {
			case 0:
				receiveS5F0(message);
				break;
			case 1:
				receiveS5F1(message);
				break;				
			case 2:
				receiveS5F2(message);
				break;
			case 3:
				receiveS5F3(message);
				break;		
			case 4:
				receiveS5F4(message);
				break;				
			case 5:
				receiveS5F5(message);
				break;
			case 6:
				receiveS5F6(message);
				break;
			default:
				//Abnormal
				break;
		}
	}

	/**********************************************
	 * STREAM 6 DATA COLLECTION
	 * @param message
	 ********************************************/
	public void receiveS6Fy(UComMsg message) {
		int function = message.getFunction();
		switch (function) {
			case 0:
				receiveS6F0(message);
				break;
			case 11:
				receiveS6F11(message);
				break;					
			case 12:
				receiveS6F12(message);
				break;
			case 15:
				receiveS6F15(message);
				break;			
			case 16:
				receiveS6F15(message);
				break;
			default:
				//Abnormal
				break;
		}			
	}
	
	/**********************************************
	 * STREAM 9 SYSTEM ERRORS
	 * @param message
	 ********************************************/

	public void receiveS9Fy(UComMsg message) {
		int function = message.getFunction();
		switch (function) {
			case 0:
				receiveS9F0(message);
				break;
			case 1:
				receiveS9F1(message);
				break;					
			case 3:
				receiveS9F3(message);
				break;
			case 5:
				receiveS9F5(message);
				break;
			case 7:
				receiveS9F7(message);
				break;
			case 9:
				receiveS9F9(message);
				break;
			case 11:
				receiveS9F11(message);
				break;
			case 13:
				receiveS9F13(message);
				break;
			default:
				//Abnormal
				break;
		}		
	}

	/**
	 * Communication check 
	 * Message 수신시  hsmsStatus, commStatus, controlStatus 체크
	 * @param message
	 */
	public void checkCommStatus(UComMsg message) {
		String strLog;
		int stream = message.getStream();
		int function = message.getFunction();
		
		if (TCPIP_NOT_CONNECTED.equals(hsmsStatus) || COMM_DISABLED.equals(commStatus)) {			
			strLog = "In Not-Connected Status, SECS Message was received. Pls, check the communication.";
			writeSEMLog(strLog);
			// SECS History DB 化
			updateSEMHistory(RECEIVED, stream, function, strLog);			
		} else if (NOT_COMMUNICATING.equals(commStatus)) {
			if ((stream != 1 && stream != 6 && stream != 9)
					|| (stream == 1 && function != 13 && function != 14)
					|| (stream == 6 && function != 12)) {
				strLog = "[RCV S" + stream + "F" + function + "] CommStatus is NOT_COMMUNICATING. Transaction is Aborted.";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, stream, function, strLog);			
				// Ignore .. send SnF0
				sendAbortTransaction(stream, message.getSysbytes());
				return;
			}
		} else if (COMMUNICATING.equals(commStatus) && EQ_OFFLINE.equals(controlStatus)
				|| HOST_OFFLINE.equals(controlStatus)) {
			if ((stream != 1 && stream != 6 && stream != 9)
					|| (stream == 1 && function != 13 && function != 14 && function != 17 && function != 18)
					|| (stream == 6 && function != 12)) {
				strLog = "[RCV S" + stream + "F" + function + "] ControlStatus is EQ_OFFLINE or HOST_OFFLINE. Transaction is Aborted.";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, stream, function, strLog);			
				// Ignore .. send SnF0
				sendAbortTransaction(stream, message.getSysbytes());
				return;				
			}						
		}
	}
	
	/** S1F0 : Abort Transaction 
	 * (Direction : H <--> E ) */
	public void receiveS1F0(UComMsg message) {		
		if (COMMUNICATING.equals(commStatus) && ATTEMPT_ONLINE.equals(commStatus)) {
			controlStatus = HOST_OFFLINE;
			tscStatus = TSC_NONE;			
			setSEMStatusToDB();
			String strLog = "[RCV S1F0] Abort Transaction : controlStatus is HOST_OFFLINE / tscStatus is TSC_NONE";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 1, 0, strLog);			
		}		
	}
	
	/** S1F1 : Are you there ? 
	 * (Direction : H <--> E ) */
	public void receiveS1F1(UComMsg message) {
		UComMsg response;
		String strLog;
		if (COMMUNICATING.equals(commStatus)) {
			mdln = makeLength(mdln, 6);
			softRev = makeLength(softRev, 6);
			response = uCom.makeSecsMsg(1, 2, message.getSysbytes());
			response.setListItem(2);
			response.setAsciiItem(mdln);
			response.setAsciiItem(softRev);
			
			strLog = "[RCV S1F1] Are you there?";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 1, 1, strLog);						
			//Send S1F2
			sendUComMsg(response, "S1F2");
			strLog = "[SND S1F2] MDLN: " + mdln + ", softRev: " + softRev;
			writeSEMLog(strLog);
			updateSEMHistory(SENT, 1, 1, strLog);						

			// Change Control State to 'REMOTE_ONLINE'
			if (ATTEMPT_ONLINE.equals(controlStatus)|| LOCAL_ONLINE.equals(controlStatus)) {
				controlStatus = REMOTE_ONLINE;
				
				sendS6F11(ControlStatusRemote, "", "", 0);				
				if (TSC_NONE.equals(tscStatus)) {
					tscStatus = TSC_INIT;
				}
				setSEMStatusToDB();
			}
		} else {
			// Ignore... Send S1F0
			sendAbortTransaction(1, message.getSysbytes());
			strLog = "[SND S1F0] Invalid Comm. Status : NOT_COMMUNICATING";
			writeSEMLog(strLog);
			updateSEMHistory(SENT, 1, 0, strLog);	
		}		
	}
	
	/** S1F2 : Reply for " Are Your There? " 
	 * (Direction : H <--> E ) */
	public void receiveS1F2(UComMsg message) {
		String strLog;
		if (COMMUNICATING.equals(commStatus) && ATTEMPT_ONLINE.equals(controlStatus)) {
			// Change Control State to 'REMOTE_ONLINE'
			controlStatus = REMOTE_ONLINE;
			if (TSC_NONE.equals(tscStatus)) {
				tscStatus = TSC_INIT;
			}				
			sendS6F11(ControlStatusRemote, "", "", 0);
			// Set IBSEM Status to DB
			setSEMStatusToDB();
			strLog = "[RCV S1F2] Reply for S1F1 'Are Your There?'";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 1, 2, strLog);	
			
		} else {
			// Error
			strLog = "[RCV S1F2] Invalid Comm. Status : NOT_COMMUNICATING";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 1, 2, strLog);				
		}		
	}
	
	/** S1F3 : Specified TSC status Request
	 * (Direction : H --> E ) */
	public void receiveS1F3(UComMsg message) {
		String strLog;
		int vId;

		UComMsg response = uCom.makeSecsMsg(1, 4, message.getSysbytes());
		if (response != null) {
			int itemCount = message.getListItem();
			if (itemCount > 0){
				response.setListItem(itemCount);
				for (int i = 0; i < itemCount; i++) {
					vId = message.getU2Item();
					strLog = "[RCV S1F3] : Specified TSC status Request, VID:" + vId;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 1, 3, strLog);				

					response = setVariablesToSecsMsg(response, vId, "", "", "", 0);
					if (response == null) {
						writeSEMLog("[RCV S1F3] : Reconcile Fail (DB connection fail)");
						return;
					}
				}
				sendUComMsg(response, "S1F4");
				strLog = "[SND S1F4]";
				writeSEMLog(strLog);
				updateSEMHistory(SENT, 1, 4, strLog);
				
			} else if (itemCount == 0) {
				strLog = "[RCV S1F3] : Zero length list - report all SVIDs";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 1, 3, strLog);				
				
				// Zero length list or item means report all SVIDs
				response = setAllVariablesToSecsMsg(response);
				sendUComMsg(response, "S1F4");
				strLog = "[SND S1F4]";
				writeSEMLog(strLog);
				updateSEMHistory(SENT, 1, 4, strLog);										
				
			} else {
				strLog = "[RCV S1F3] S1F3 Msg is received, but it's ListItemCount is zero.";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 1, 3, strLog);
			}
		}
	}

	/** S1F13 : Establish Communication Request
	 * (Direction : H <--> E ) */
	public void receiveS1F13(UComMsg message) {
		int commAck;
		UComMsg response;
		
		if (NOT_COMMUNICATING.equals(commStatus) || COMMUNICATING.equals(commStatus)) {
			commAck = 0; // COMACK, Bin, 1 -> ACK
		} else {
			commAck = 1; // COMACK, Bin, 0 -> NAK
		}
		
		String strLog = "[RCV S1F13] Establish Communication Request";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 1, 13, strLog);		
		
		response = uCom.makeSecsMsg(1, 14, message.getSysbytes());
		response.setListItem(2);
		response.setBinaryItem(commAck);
		response.setListItem(2);
		response.setAsciiItem(makeLength(mdln, 6));
		response.setAsciiItem(makeLength(softRev, 6));
		
		if (sendUComMsg(response, "S1F14") == OK) {
			commStatus = COMMUNICATING;
			controlStatus = EQ_OFFLINE;			
			if (REMOTE_ONLINE.equals(controlStatus)) {
				controlStatus = EQ_OFFLINE;
				tscStatus = TSC_NONE;
			}
			setSEMStatusToDB();
			stopEstCommTimer();
			
			strLog = "[SND S1F14] commStatus is COMMUNICATING";
			writeSEMLog(strLog);
			updateSEMHistory(SENT, 1, 14, strLog);				
		} else {
			sendAbortTransaction(1, message.getSysbytes());
			strLog = "[SND S1F0] sendAbortTransaction";
			writeSEMLog(strLog);
			updateSEMHistory(SENT, 1, 0, strLog);		
		}
	}

	/** S1F14 : Establish communication request ACK 
	 * (Direction : H <--> E ) */
	public void receiveS1F14(UComMsg message) {
		int itemCount = message.getListItem();
		int commAck = message.getBinaryItem();
		String strLog;
		
		if ((commAck == 0x00) &&
				(NOT_COMMUNICATING.equals(commStatus) || COMMUNICATING.equals(commStatus))) {
			commStatus = COMMUNICATING;
			controlStatus = EQ_OFFLINE;
			tscStatus = TSC_NONE;

			setSEMStatusToDB();
			stopEstCommTimer();
			strLog = "[RCV S1F14] COMM_DIABLED -> COMMUNICATING ";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 1, 14, strLog);			
		} else {
			strLog = "[RCV S1F14] Invalid ACK response or Invalid Communication Status";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 1, 14, strLog);
		}		
	}

	/** S1F15 : Request to Offline 
	 * (Direction : H --> E ) */
	public void receiveS1F15(UComMsg message) {
		int oflAck;
		UComMsg response;
		
		if (COMMUNICATING.equals(commStatus)) {
			oflAck = 0; // OFLACK, Bin, 1 -> ACK
		} else {
			oflAck = 1; // OFLACK, Bin, 1 -> NAK
		}

		String strLog = "[RCV S1F15] Request to Offline";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 1, 15, strLog);			
		
		if (oflAck == 0) {
			if (REMOTE_ONLINE.equals(controlStatus)) {
				controlStatus = HOST_OFFLINE;
				tscStatus = TSC_NONE;
				// Set IBSEM Status to DB
				setSEMStatusToDB();
				sendS6F11(EquipmentOffline, "", "", 0);
			}
			// Send S1F16
			response = uCom.makeSecsMsg(1, 16, message.getSysbytes());
			response.setBinaryItem(oflAck);
			sendUComMsg(response, "S1F16");
			
			strLog = "[SND S1F16] Reply for S1F15 ACK";
			writeSEMLog(strLog);
			updateSEMHistory(SENT, 1, 16, strLog);		
		}		
	}

	/** S1F17 : Request Online 
	 * (Direction : H --> E ) */
	public void receiveS1F17(UComMsg message) {
		String strLog;
		int onlAck;		
		if (COMMUNICATING.equals(commStatus) &&
				(HOST_OFFLINE.equals(controlStatus) || EQ_OFFLINE.equals(controlStatus))) {
			onlAck = 0; // ONLACK, Bin, 0 -> ACK
			strLog = "[RCV S1F17] ONLACK, Bin, 0 -> ACK";
		} else if (COMMUNICATING.equals(commStatus) && REMOTE_ONLINE.equals(controlStatus)) {
			onlAck = 2; // ONLACK, Bin, 2 -> Already Online
			strLog = "[RCV S1F17] ONLACK, Bin, 2 -> Already Online";
		} else {
			onlAck = 1; // ONLACK, Bin, 1 -> NAK
			strLog = "[RCV S1F17] ONLACK, Bin, 1 -> NAK";
		}				
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 1, 17, strLog);		
		
		// Send S1F18
		UComMsg response = uCom.makeSecsMsg(1, 18, message.getSysbytes());
		response.setBinaryItem(onlAck);
		sendUComMsg(response, "S1F18");
		
		strLog = "[SND S1F18] Reply for S1F17";
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 18, strLog);		

		// 2013.10.15 by KYK : reply 후 online 처리하도록 함
		if (onlAck == 0) {
			// TSCAutoInitiated, TSCPaused (앞부분이 TSC별 다름) 
			respondS1F17Status();
			// Set IBSEM Status to DB
			setSEMStatusToDB();			
		}	
	}

	/** S2F13 : Equipment Constant Request 
	 * (Direction : H --> E ) */
	public void receiveS2F13(UComMsg message) {
		int itemCount;
		int equipmentConstantId;
		EquipmentConstant equipmentConstant;
		
		String strLog = "[RCV S2F13] Equipment Constant Request";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 13, strLog);		

		UComMsg response = uCom.makeSecsMsg(2, 14, message.getSysbytes());		
		itemCount = message.getListItem();

		String strLog2 = null;
		if (itemCount > 0) {
			response.setListItem(itemCount);
			for (int i = 0; i < itemCount; i++) {
				equipmentConstantId = message.getU2Item(); // ECID		
				if (ecIdOTable.containsKey(equipmentConstantId)) {					
					equipmentConstant = ecIdOTable.get(equipmentConstantId);
					//
					response = setEcVTypeItem(response, equipmentConstant);					
				} else {
					strLog2 = "ERROR: UNDEFINED_ECID. VID:" + equipmentConstantId;
					writeSEMLog(strLog2);
				}				
			}
			sendUComMsg(response, "S2F14");
		} else if (itemCount == 0) {
			// if ListCount = 0, list up allItems.
			itemCount = ecIdOTable.size();
			response.setListItem(itemCount);
			
			Iterator<EquipmentConstant> iter = ecIdOTable.values().iterator();
			while (iter.hasNext()) {
				equipmentConstant = (EquipmentConstant) iter.next();
				//
				response = setEcVTypeItem(response, equipmentConstant);					
			}
			sendUComMsg(response, "S2F14");
		} else {
			strLog2 = "[SND S2F13] Msg's received, but ecIdOTable is Empty.";
			writeSEMLog(strLog2);
		}	
		
		if (strLog2 == null) {
			strLog2 = "[SND S2F14] Reply for S2F14";
		}		 
		writeSEMLog(strLog2);
		updateSEMHistory(SENT, 2, 14, strLog2);	
	}	

	private UComMsg setEcVTypeItem(UComMsg response, EquipmentConstant equipmentConstant) {
		SECSMSG_TYPE valueType = equipmentConstant.getValueType();
		switch (valueType) {
			case A:
				response.setAsciiItem(equipmentConstant.getStrValue());
				break;
			case U2:
				response.setU2Item(equipmentConstant.getNumValue());
				break;
			case U4:
				response.setU4Item(equipmentConstant.getNumValue());
				break;
			default:
				writeSEMLog(" ValueType of EquipmentConstant is invalid.");
				break;
		}
		return response;
	}

	/**
	 * S2F15 : New Equipment Constant Send
	 * (Direction : H --> E )
	 * EAC  0 , ACK
	 *	    1 , denied, there isn't at least one constant
	 *	    2 , denied, busy
	 *	    3 , denied at least constant is beyond the allowed range..
	 *	    4 , undefined ECID
	 *	    5 , fail to extract ECV value from S2F15
	 *
	 * @param message
	 */
	public void receiveS2F15(UComMsg message) {
		int vId;
		int subItemCount;
		int eAc = 0;
		int itemCnt = message.getListItem();

		String strLog = "[RCV S2F15] New Equipment Constant Send";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 15, strLog);		

		if (itemCnt > 0) {
			for (int i = 0; i < itemCnt; i++) {
				subItemCount = message.getListItem(); // subItemCnt = 2 (ECID, ECV)
				vId = message.getU2Item(); // ECID : U2
				eAc = setEcVData(vId, message); // userDefinesMethod
			}
		} else {
			eAc = 1;
		}
		UComMsg rsp = uCom.makeSecsMsg(2, 16, message.getSysbytes());
		rsp.setBinaryItem(eAc); // EAC
		sendUComMsg(rsp, "S2F16");		
		
		strLog = "[SND S2F16] Reply for S2F15";
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 16, strLog);	
	}	

	/** S2F17 : Date & Time Request 
	 * (Direction : H <-- E ) */
	public void receiveS2F17(UComMsg message) {
		
		String strLog = "[RCV S2F17] Date & Time Request ";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 17, strLog);		

		UComMsg response = uCom.makeSecsMsg(2, 18, message.getSysbytes());
		response.setAsciiItem(getCurrentTime());
		sendUComMsg(response, "S2F18");
		
		strLog = "[SND S2F18] send Date & Time " + getCurrentTime();
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 18, strLog);	
	}
	
	/** S2F18 : Date & Time Data Send 
	 * (Direction : H --> E ) */
	public void receiveS2F18(UComMsg message) {
		// 2015.08.31 by KBS : getAsciiItem()을 2회하면 null 또는 잘못된 값을 읽을 수 있음
		String dateNtime = message.getAsciiItem();
		setCurrentTime(dateNtime);
		
		String strLog = "[RCV S2F18] Date & Time Data Send " + dateNtime;
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 18, strLog);		
	}
	
	/** S2F29 : Equipment Constant Namelist Request
	 * (Direction : H --> E ) */
	public void receiveS2F29(UComMsg message) {
		String log = null;
		int equipmentConstantId;
		
		String strLog = "[RCV S2F29] Equipment Constant Namelist Request";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 29, strLog);		

		int itemCount = message.getListItem();
		UComMsg response = uCom.makeSecsMsg(2, 30, message.getSysbytes());
		if (response != null) {
			if (itemCount > 0){
				response.setListItem(itemCount);
				for (int i = 0; i < itemCount; i++){
					// 2015.08.31 by KBS : message에서 ECID를 읽어와야 함
					equipmentConstantId = message.getU2Item(); // ECID
					response = setEcvNamelistToSecsMsg(response, equipmentConstantId);
					// 2011.11.10 by KYK : response 는 not null 대신 setEcvNamelistToSecsMsg 메소드 내 조건추가
//					if (response == null){					
//						response.setListItem(0);				
//						// Error
//						log = "[RCV S2F29] fail to make ECVNameList for ECID = " + equipmentConstantId;
//						writeSEMLog(log);
//						return;
//					}
				}
				sendUComMsg(response, "S2F30");					
				if (log == null) {
					log = "[SND S2F30] Send Equipment Constant Namelist";
				}
				writeSEMLog(log);
				updateSEMHistory(SENT, 2, 30, log);			
				
			} else if (itemCount == 0) {
				// A zero length list means report all ECIDs according to predefined order.			
				int subItemCount = ecIdOTable.size();			
				response.setListItem(subItemCount);
				
				Iterator<Integer> iter = ecIdOTable.keySet().iterator();
				while (iter.hasNext()) {
					equipmentConstantId = (Integer) iter.next();
					response = setEcvNamelistToSecsMsg(response, equipmentConstantId);
				}			
				sendUComMsg(response, "S2F30");
				log = "[SND S2F30] Send Equipment Constant Namelist";
				writeSEMLog(log);
				updateSEMHistory(SENT, 2, 30, log);				
			} else {
				log = "[RCV S2F29] Msg is received, but it's ListItemCount is zero.";
				writeSEMLog(log);
			}		
		}
	}	

	/** S2F31 : Date and Time Set Request
	 * (Direction : H --> E ) */
	public void receiveS2F31(UComMsg message) {
		int tiAck;
		String strLog;
		String dateTime = message.getAsciiItem();			
		if (setCurrentTime(dateTime)) {
			tiAck = 0;
			strLog = "[RCV S2F31] Set time & date - " + dateTime;
		} else {
			tiAck = 1;
			strLog = "[RCV S2F31] fail to set time & date - " + dateTime;
		}				
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 31, strLog);					
		
		UComMsg response = uCom.makeSecsMsg(2, 32, message.getSysbytes());
		response.setBinaryItem(tiAck); // TIACK
		sendUComMsg(response, "S2F32");

		strLog = "[SND S2F32] Reply for S2F31,TIACK: " + tiAck;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 32, strLog);			
	}
	
	/**
	 * S2F33 : Define Report
	 * (Direction : H --> E )
	 * DRACK    1  = denied, insufficient space
	 *          2  = denied, incorrect format
	 *          3  = denied, at least one ReportID is already defined
	 *          4  = denied, at least one VID is already defined
	 *          >4 = some other error
	 * If an error condition is detected the entire message is rejected, i. e., partial changes are not allowed
	 * // Exception 1. A list of zero-length following <DATAID> deletes all report definitions and associated links.
	 * 2. A list of zero-length following <ReportID> deletes report type ReportID. All CEID links to this ReportID are also deleted.
	 * @param message
	 */
	public void receiveS2F33(UComMsg message) {
		String strLog;
		int drAck = 0;		
		int itemCount = message.getListItem(); // L, 2
		long dataId = message.getU4Item(); // Always DATAID = 0
		itemCount = message.getListItem(); // number of ReportIds to be defined.

		// A list of zero-length  
		// deletes all report definitions and associated links.
		if (itemCount == 0){
			deleteAllReportDefinition();
			resetAllCEIDLink();
			strLog = "[RCV S2F33] Define Report, (A list of zero-length) All Report Define Reset";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 33, strLog);
		} else {						
			int subItemCount;
			int vIdCount;
			int reportId;
			int vId;
			String vids = null;
			EventReport eventReport;
			StringBuilder sb;
			
			for (int i = 0; i < itemCount; i++) {			
				subItemCount = message.getListItem(); // L, 2
				reportId = message.getU2Item(); 
				
				if (reportIdOTable.containsKey(reportId)) {
					drAck = 3; // denied, at least one ReportID is already defined.
					break;
				}
				eventReport = new EventReport();
				vIdCount = message.getListItem(); // number of the VIDs to be linked.
				for (int j = 0; j < vIdCount; j++) {
					vId = message.getU2Item();
					eventReport.setVid(j, vId);
					if (j == 0) {
						vids = "" + vId;
					} else {
						vids += "," + vId ;
					}
				}
				eventReport.setReportId(reportId);
				eventReport.setVidQty(vIdCount);
				reportIdOTable.put(reportId, eventReport);
					
				/* DB Update if necessary */
				sb = new StringBuilder();
				sb.append("	Define Report> ReportId:").append(reportId).append(" VIDs:").append(vids);
				writeSEMLog(sb.toString());
				updateSEMHistory(RECEIVED, 2, 33, sb.toString());
			}
			/* DB Update if necessary */
			strLog = "[RCV S2F33] Define Report Completed";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 33, strLog);
		}
		
		// If there are some errors, delete all the ReportIDs.
		if (drAck != 0){
			deleteAllReportDefinition();
			writeSEMLog("DRACK is not OK. All Report Definition is deleted");
		}			

		UComMsg response = uCom.makeSecsMsg(2, 34, message.getSysbytes());
		response.setBinaryItem(drAck); // DRACK
		sendUComMsg(response, "S2F34");		
		
		strLog = "[SND S2F34] Reply for S2F33,DRACK: " + drAck;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 34, strLog);		
	}
	
	public void receiveS2F34(UComMsg message) {
		// Do nothing
	}

	/**
	 * S2F35 : Link Event Report
	 * (Direction : H --> E )
	 * LRACK    1  = denied, insufficient space
	 *          2  = denied, incorrect format
	 *          3  = denied, at least one CEID link is already defined         
	 *          4  = denied, there isn't at least one CEID         
	 *          5  = denied, there isn't at least one ReportID
	 *          6  = denied, Undefined CEID         
	 *          7  = denied, duplicated ReportID         
	 *          >7 = some other error
	 * If an error condition is detected the entire message is rejected, i. e., partial changes are not allowed         
	 * Exception : A list of zero-length following CEID deletes all report links to that event.                  
	 * @param message
	 */	
	public void receiveS2F35(UComMsg message) {
		int lrAck;
		int itemCount;
		int subItemCount;
		int reportIdCount;
		int collectionEventId;
		int reportId;
		long dataId;
		String reportIds = null;
		String strLog;
		StringBuilder sb;
		
		lrAck = 0;
		itemCount = message.getListItem(); // L, 2
		dataId = message.getU4Item(); // Always DATAID = 0
		itemCount = message.getListItem(); // number of reportIds to be defined.

		// 2013.01.04 by KYK
		strLog = "[RCV S2F35] Link Event Report ";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 35, strLog);			

		if (itemCount == 0){
			lrAck = 4; //  denied, there isn't at least one CEID
			resetAllCEIDLink();
			strLog = "	Link Event Report (A list of zero-length) All report links were deleted";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 35, strLog);			
		} else {
			// Normal Case
			for (int i = 0; i < itemCount; i++) {
				subItemCount = message.getListItem(); // L, 2
				collectionEventId = message.getU2Item();
				reportIdCount = message.getListItem();

				if (reportIdCount == 0) {
					resetCEIDLink(collectionEventId);
					continue;
				} else {
					/* update memory */
					CollectionEvent collectionEvent = ceIdOTable.get(collectionEventId);
					if (collectionEvent == null) {
						collectionEvent = new CollectionEvent();
					}
					ArrayList<Integer> reportIdsOnCeid = new ArrayList<Integer>();
					
					for (int j = 0; j < reportIdCount; j++) {
						reportId = message.getU2Item();
						reportIdsOnCeid.add(reportId);
						collectionEvent.setReportIdAt(j, reportId);

						if (j == 0) {
							reportIds = "" + reportId;
						} else {
							reportIds += "," + reportId ;
						}
					}
					collectionEvent.setReportIdQty(reportIdCount);			
					ceIdOTable.put(collectionEventId, collectionEvent);
				}
				sb = new StringBuilder();
				sb.append("	Link Event Report> collectionEventId:").append(collectionEventId).append(" ReportIds:").append(reportIds);
				writeSEMLog(sb.toString());
				updateSEMHistory(RECEIVED, 2, 35, sb.toString());
			}
			/* update DB if necessary */
		}
		if (lrAck != 0) {
			resetAllCEIDLink(); // Cancel
			writeSEMLog("LRACK is not OK. All Report Links were deleted");
		}

		UComMsg response = uCom.makeSecsMsg(2, 36, message.getSysbytes());
		response.setBinaryItem(lrAck); // LRACK		
		sendUComMsg(response, "S2F36");			
		
		strLog = "[SND S2F36] Reply for S2F36,LRACK: " + lrAck;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 36, strLog);		
	}

	/**
	 * S2F37 : Enable/Disable Event Report
	 * (Direction : H --> E )
	 * ERACK    0 = ACK
	 *          1 = denied, there isn't at least one CEID
	 *          2 = denied, Undefined CEID
	 * If an error condition is detected the entire message is rejected, 
	 * i. e., partial changes are not allowed         
	 * @param message
	 */
	public void receiveS2F37(UComMsg message) {
		int erAck;
		int itemCount;
		boolean isEnabled;
		String strLog;
		erAck = 0;
		itemCount = message.getListItem(); // L, 2
		isEnabled = message.getBoolItem(); // CEED
		
		itemCount = message.getListItem(); // number of the Collection Events
		
		// Abnormal Case
		if (itemCount < 0) {
			erAck = 1;
			enableAllCollectionEvent(false);
			strLog = "[RCV S2F37] All CollectionEvent Disabled (itemCount < 0)";
		} else if (itemCount == 0) {
			// Normal Case
			enableAllCollectionEvent(isEnabled);
			strLog = "[RCV S2F37] All Event Abled:" + isEnabled;			
		} else {
			// enable itemCnt's msg
			enableCollectionEvent(message, itemCount, isEnabled);	
			strLog = "[RCV S2F37] " + itemCount + " Event Abled:" + isEnabled;			
		}
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 37, strLog);		

		UComMsg response = uCom.makeSecsMsg(2, 38, message.getSysbytes());
		response.setBinaryItem(erAck); // ERACK
		sendUComMsg(response, "S2F38");		
		
		strLog = "[SND S2F38] Reply for S2F38,ERACK: " + erAck;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 38, strLog);		
	}

	/** S5F3 : Enable/Disable Alarm Send
	 * (Direction : H --> E ) */
	public void receiveS5F3(UComMsg message) {
		String strLog;
		int ackc5;
		long alarmId = 0;
		boolean isEnabled = false;
		int itemCount = message.getListItem(); // L, 2
		int alarmEnabled = message.getBinaryItem(); 

		UComItem uItem = message.getCurrentItemAndMoveNext();
		
		// 2011.11.08 by PMM
		// uItem != null 조건 추가
//		if ("U4".equals(uItem.getTypeName())) {
		if (uItem != null && "U4".equals(uItem.getTypeName())) {
			alarmId = uItem.getU4Item();			
		}		
		// 테스트 필요 
		//alId = msg.getU4Item();
		
		// ALED (bit8 = 1 alarm enabled / bit8 = 0 alarm disabled)
		if (alarmEnabled == 0x80 || alarmEnabled == -128) {
			isEnabled = true;
		}		
		// ALID = 0, enable/disable all alarms.
		if (alarmId == 0) {
			if (enableAlarmStatus(isEnabled)) {
				ackc5 = 0; // ACK
				strLog = "[RCV S5F3] All alarms set " + isEnabled;
				// 2013.06.28 by KYK
				if (isEnabled) {
					isAllAlarmEnabled = true;
				}
			} else {
				ackc5 = 1; // Error
				strLog = "[RCV S5F3] (Error) All alarms set " + isEnabled;
			}			
		} else {
			if (enableAlarmStatus(alarmId, isEnabled)) {
				ackc5 = 0; // ACK
				strLog = "[RCV S5F3] AlarmId : " + alarmId + "/" + isEnabled;				
			} else {
				ackc5 = 1; // Error
				strLog = "[RCV S5F3] (Error) AlarmId : " + alarmId + "/" + isEnabled;								
			}
		}		
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 5, 3, strLog);		

		UComMsg response = uCom.makeSecsMsg(5, 4, message.getSysbytes());
		response.setBinaryItem(ackc5); // ACK5
		sendUComMsg(response, "S5F4");
		
		strLog = "[SND S5F4] Reply for S5F4,ACK5: " + ackc5;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 5, 4, strLog);		
	}	
	
	/** S5F5 : List Alarms Request
	 * (Direction : H --> E ) */
	// ??	
	public void receiveS5F5(UComMsg message) {

		// ALCD : bit8=1 (0x80) generated alarm status
		// ALCD	: bit8=0 (0x00) clear alarm status	
		int alcd;
		int itemCount = 0;
		long alarmId;
		AlarmItem alarmItem;
		
		String strLog = "[RCV S5F5] List Alarms Request";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 5, 5, strLog);		

		UComMsg response = uCom.makeSecsMsg(5, 6, message.getSysbytes());
		UComItem uItem = message.getCurrentItemAndMoveNext(); //??
		
		// 2011.11.08 by PMM
		// uItem에 대한 not null 체크 추가
//		if ("U4".equals(uItem.getTypeName())) {
		if (uItem != null && "U4".equals(uItem.getTypeName())) {
			alarmId = uItem.getU4Item();
			// ??
			if (alarmId > 0) {
				itemCount = alarmIdOTable.size();
			}
			response.setListItem(itemCount);
			
			if (itemCount > 0) {
				Iterator<AlarmItem> iter = alarmIdOTable.values().iterator();
				while (iter.hasNext()) {
					alarmItem = (AlarmItem) iter.next();
					if (alarmId == alarmItem.getAlarmId()) {
						if (alarmItem.isActivated()) {
							alcd = 0x80;
						} else {
							alcd = 0x00;
						}
						response.setListItem(3); // L, 3
						response.setBinaryItem(alcd); // ALCD
						response.setU4Item(alarmItem.getAlarmId()); // ALID
						response.setAsciiItem(alarmItem.getAlarmText()); // ALTX					
					} else { // Unregistered ALID
						response.setListItem(3); // L, 3					
						response.setBinaryItem(0); // ALCD
						response.setU4Item(0); // ALID
						response.setAsciiItem(""); // ALTX
					}
				}				
			}
		} else {
			// A zero-length item means send all possible alarms regardless of the state of ALID.
			response.setListItem(alarmIdOTable.size());
			Iterator<AlarmItem> iter = alarmIdOTable.values().iterator();
			while (iter.hasNext()) {
				alarmItem = (AlarmItem) iter.next();
				if (alarmItem.isActivated()) {
					alcd = 0x80;
				} else {
					alcd = 0x00;
				}
				response.setListItem(3); // L, 3
				response.setBinaryItem(alcd); // ALCD
				response.setU4Item(alarmItem.getAlarmId()); // ALID
				response.setAsciiItem(alarmItem.getAlarmText()); // ALTX
			}
		}
		// 기존코드에 전송하는 부분누락됨
		sendUComMsg(response, "S5F6");
		
		strLog = "[SND S5F6] Reply for S5F5, Send Alarm List";
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 5, 6, strLog);		
	}

	/** S6F15 : Event Report Request
	 * (Direction : H --> E ) */
	public void receiveS6F15(UComMsg message) {
		int i;
		int j;
		int itemCount;
		int subItemCount;
		int collectionEventId;
		CollectionEvent collectionEvent;
		EventReport eventReport;
		
		String strLog = "[RCV S6F15] Event Report Request";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 6, 15, strLog);		
		
		UComMsg response = uCom.makeSecsMsg(6, 16, message.getSysbytes());
		// 2011.11.08 by KYK : response 에 대한 not null 조건 추가 (prevent)
		if (response != null) {
			collectionEventId = message.getU2Item();
			response.setListItem(3); // L, 3
			response.setU4Item(0); // DATAID
			response.setU2Item(collectionEventId); // CEID
			collectionEvent = getCollectionEventObject(collectionEventId);
			
			if (collectionEvent != null && collectionEvent.isLinkDefined()) {
				itemCount = collectionEvent.getReportIdQty();
				response.setListItem(itemCount); // L, n : # of ReportID
				
				for (i = 0; i < itemCount; i++) {
					response.setListItem(2); // L, 2
					response.setU2Item(collectionEvent.getReportIdAt(i)); // ReportID
					eventReport = getEventReportObject(collectionEvent.getReportIdAt(i));
					if (eventReport != null && eventReport.getEnabled() == true) {
						subItemCount = eventReport.getVidQty(); // L, n : # of VID
						for (j = 0; j < subItemCount; j++) {
							response = setVariablesToSecsMsg(response, eventReport.getVid(j), "", "", "", 0);
						}
					} else {
						response.setListItem(0);
					}
				}
			} else {
				response.setListItem(0);
			}
			sendUComMsg(response, "S6F16");						
		}		
		strLog = "[SND S6F16] Reply for S6F15, Event Report Sent";
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 5, 6, strLog);		
	}

	public void receiveS1F4(UComMsg message) {
		// Do nothing
	}
	public void receiveS1F16(UComMsg message) {
		// Do nothing
	}
	public void receiveS1F18(UComMsg message) {
		// Do nothing
	}
	public void receiveS2F0(UComMsg message) {
		// Do nothing
	}
	public void receiveS2F14(UComMsg message) {
		// Do nothing.
	}
	public void receiveS2F16(UComMsg message) {
		// Do nothing
	}
	public void receiveS2F30(UComMsg message) {
		// Do nothing
	}
	public void receiveS2F32(UComMsg message) {
		// Do nothing
	}
	public void receiveS2F36(UComMsg message) {
		// Do nothing
	}
	public void receiveS2F38(UComMsg message) {
		// Do nothing
	}
	public void receiveS2F42(UComMsg message) {
		// Do nothing
	}	
	public void receiveS2F50(UComMsg message) {
		// Do nothing
	}
	public void receiveS5F0(UComMsg message) {
		// Do nothing		
	}
	public void receiveS5F1(UComMsg message) {
		// Do nothing	
	}
	public void receiveS5F2(UComMsg message) {
		// Do nothing		
	}
	public void receiveS5F4(UComMsg message) {
		// Do nothing		
	}
	public void receiveS5F6(UComMsg message) {
		// Do nothing		
	}	
	public void receiveS6F0(UComMsg message) {
		// Do nothing		
	}
	public void receiveS6F11(UComMsg message) {
		// Do nothing	
	}
	public void receiveS6F12(UComMsg message) {
		// Do nothing
	}
	public void receiveS9F0(UComMsg message) {
		// Do nothing		
	}
	public void receiveS9F1(UComMsg message) {
		// Do nothing		
	}
	public void receiveS9F3(UComMsg message) {
		// Do nothing		
	}
	public void receiveS9F5(UComMsg message) {
		// Do nothing		
	}
	public void receiveS9F7(UComMsg message) {
		// Do nothing		
	}
	public void receiveS9F9(UComMsg message) {
		// Do nothing		
	}
	public void receiveS9F11(UComMsg message) {
		// Do nothing		
	}
	public void receiveS9F13(UComMsg message) {
		// Do nothing		
	}
	
	/**
	 * UComMsg (SEComMsg) 를 전송한다.
	 * @param uComMsg UComMsg
	 * @param strMessage String
	 * @return int
	 */
	public int sendUComMsg(UComMsg uComMsg, String message){
		// 2012.01.06 by PMM
		return sendUComMsg(uComMsg, message, "", null);
	}
	
	public int sendUComMsg(UComMsg uComMsg, String message, String eventName, ReportItems reportItems){
		int returnValue = 0;
		boolean isRequested = true;

		if (uComMsg.getSystemBytes() == 0) {
			isRequested = true;
		} else {
			isRequested = false;
		}
		if (uCom.send(uComMsg, isRequested)){
			// message 로그는 따로 ?
//			writeSEMLog(message);
			returnValue = OK;
			
			// 2012.01.06 by PMM
			if (isFormattedLogUsed) {
				traceFormattedSECSMessage(new SECSMessage(uComMsg, eventName, reportItems, SEND_SECS));
			}
		} else {
			String strLog = "[" + message + "] Fail to Send Msg";
			writeSEMLog(strLog);
			updateSEMHistory(SENT, 0, 0, strLog);
			returnValue = NOT_OK;
		}
		return returnValue;
	}	
	
	/**
	 * SxF0 : AbortTransaction message 전송
	 * @param stream
	 * @param sysbytes
	 * @return
	 */
	public int sendAbortTransaction(int stream, long sysbytes) {
		String message = "S" + stream + "F0";
		UComMsg response = uCom.makeSecsMsg(stream, 0, sysbytes);
		return sendUComMsg(response, message);		
	}	
	
	/**
	 * 
	 * @return
	 */
	private int sendS1F13() {
		UComMsg message = uCom.makeSecsMsg(1, 13);
		message.setListItem(2);
		message.setAsciiItem(makeLength(mdln, 6));
		message.setAsciiItem(makeLength(softRev, 6));	
		
		String strLog = "[SND S1F13] MDLN: " + makeLength(mdln, 6) + "SOFTREV: " + makeLength(softRev, 6);
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 13, strLog);		

		return sendUComMsg(message, "S1F13");
	}
	
	/**
	 * 
	 * @param eventName
	 * @param reportItems : String commandId, String vehicleId, String sourcePort, String destPort,
	 * 				String transferPort, String carrierId, String carrierLoc
	 * 				int alarmId, int vehicleState, int priority, int replace, int resultCode
	 * @return
	 */
	public int sendS6F11(String eventName, ReportItems reportItems) {
		int collectionEventId;
		int reportId;
		int vId;
		String strLog;
		CollectionEvent collectionEvent;
		EventReport eventReport;
		UComMsg message = uCom.makeSecsMsg(6, 11);
		
		// 2011.11.09 by KYK : message null check
		if (message == null) {
			writeSEMLog("UComMsg message = uCom.makeSecsMsg(6, 11); message is null");	
			return NOT_OK;
		}
		collectionEventId = getCollectionEventId(eventName);
		if (collectionEventId != NOT_EXIST) {
			
			collectionEvent = getCollectionEventObject(collectionEventId);
			if (collectionEvent != null && collectionEvent.isEnabled()) {
				message.setListItem(3); // L, 3
				message.setU4Item(0); // DATAID = 0 (Always sends DATAID = 0)
				message.setU2Item(collectionEventId); // CEID
				message.setListItem(collectionEvent.getReportIdQty());
				
				for (int i = 0; i < collectionEvent.getReportIdQty(); i++) {
					reportId = collectionEvent.getReportIdAt(i);
					message.setListItem(2);
					message.setU2Item(reportId);
					eventReport = getEventReportObject(reportId);
					
					if (eventReport != null) {
						message.setListItem(eventReport.getVidQty());
						for (int j = 0; j < eventReport.getVidQty(); j++) {
							vId = eventReport.getVid(j);
							/* */
							message = setVariablesToSecsMsg(message, vId, reportItems);
						}
					} else {
						message.setListItem(0);
						strLog = "	Undefined ReportID or Enabled property of the Report is false. ReportID:" + reportId;
						writeSEMLog(strLog);						
					}
				}
				// 2012.01.06 by PMM
//				sendUComMsg(message, "S6F11");
				sendUComMsg(message, "S6F11", eventName, reportItems);
				
				// eventReportMsg DB化 
				updateEventReportMsgToDB(eventName, reportItems);
			} else {
				message.setListItem(0);
				strLog = "	Undefined CEID or Enabled property of the Event is false. CEID:" + collectionEventId;
				writeSEMLog(strLog);
				return NOT_OK;				
			}
		} else {
			strLog = "	It can't be found CollectionEventID by using the givent EventName. EventName:";
			writeSEMLog(strLog + eventName);
			updateSEMHistory(SENT, 6, 11, strLog + eventName);	
			return NOT_OK;
		}		
		traceS6F11History(eventName, reportItems);		
		return OK;		
	}

	public void updateEventReportMsgToDB(String eventName, ReportItems reportItems) {
		// TODO Auto-generated method stub				
	}

	public int getCollectionEventId(String eventName) {		
		if (eventNameCeIdTable.containsKey(eventName)) {
			return eventNameCeIdTable.get(eventName);
		} else {
			return NOT_EXIST;
		}
	}		
	
	/**	 
	 * VID 에 해당하는 값을 SECS Msg (UComMsg) 형태로 변환한다. 
	 * @param response
	 * @param vId
	 * @param reportItems
	 * @return
	 */
	public UComMsg setVariablesToSecsMsg(UComMsg response, int vId, ReportItems reportItems) {
		String strLog;
		String name;
		name = vIdOTable.get(vId);
		// 2011.11.14 by KYK : 불필요한데 CRC null check 때문에 삽입함
		if (reportItems == null) {
			reportItems = new ReportItems();
		}
		
		if (ALARMID.equalsIgnoreCase(name)) {
			// VID:1 , AlarmID , CLASS:DVVAL , Format:U4
			response.setU4Item(reportItems.getAlarmId());	
		} else if (ESTABLISHCOMMUNICATIONSTIMEOUT.equalsIgnoreCase(name)) {
			// VID:2 , S1F13 sending interval for establishing communications , CLASS: ECV , Format:<U2>
			response = setEstablishCommunicationTimeoutToSecsMsg(response);
		} else if (ALARMSENABLED.equalsIgnoreCase(name)) {
			// VID:3 , enabled alarms list, CLASS:SV, Format:< L <ALID> ... >  
			response = setAlarmEnabledToSecsMsg(response);
		} else if (ALARMSSET.equalsIgnoreCase(name)) {
			// VID:4 , active alarm list, CLASS:SV, Format: <L  <ALID> ...>  
			//  Code 수정 필요 (서로 다른 Vehicle 동일 alarm 발생했을 경우 구별하여 저장하지 못함 ??
			response = setAlarmSetToSecsMsg(response);
		} else if (CLOCK.equalsIgnoreCase(name)) {
			// VID:5 , current time, CLASS:SV, Format:<A[16]>  "YYYYMMDDhhmmsscc"
			response.setAsciiItem(getCurrentTime());
		} else if (CONTROLSTATE.equalsIgnoreCase(name)) {
			// VID:6 , controlState, CLASS:SV, Format:B,U1
			response = setControlStateToSecsMsg(response);			
		} else if (EVENTSENABLED.equalsIgnoreCase(name)) {
			// VID:7 , VariableName:EventsEnabled , CLASS:SV, Format:<L  <CEID>    ...>
			response = setEventEnabledToSecsMsg(response);			
		} else if (ACTIVECARRIERS.equalsIgnoreCase(name)) {
			// VID:51 , List current status of all carrier Info, CLASS:SV, Format:<L[n] <CarrierInfo>...>
			response = setActiveCarriersToSecsMsg(response);
		} else if (ACTIVETRANSFERS.equalsIgnoreCase(name)){
			// VID:52 , List current status of all ACTIVE TRANSFER, CLASS:SV, Format:<L[n] <TransferCommand>...>
			response = setActiveTransfersToSecsMsg(response);
		} else if (ACTIVEVEHICLES.equalsIgnoreCase(name)){
			// VID:53 , List current status of all vehicles availble, CLASS:SV, Format:<L[n]  <VehicleInfo> ... >
			response = setActiveVehiclesToSecsMsg(response);
		} else if (CARRIERID.equalsIgnoreCase(name)){
			// VID:54 , CarrierID , CLASS:DVVAL or SV , Format:<A[1...64]>
			response.setAsciiItem(makeString(reportItems.getCarrierId()));
		} else if (CARRIERINFO.equalsIgnoreCase(name)){
			// VID:55 , All dbInfo / a carrier generating an event, CLASS:DVVAL or SV , 
			// Format:<L[3], 1.<CarrierID> 2.<VehicleID> 3.<CarrierLoc> >
			response = setCarrierInfoToSecsMsg(response, reportItems.getCarrierId());
		} else if (CARRIERLOC.equalsIgnoreCase(name)){
			// VID:56 , Unique location of the carrier within, CLASS:DVVAL or SV , Format:<A[1...64]> "P1"
			response.setAsciiItem(makeString(reportItems.getCarrierLoc()));
		} else if (COMMANDNAME.equalsIgnoreCase(name)){
			// VID:57, CommandName
		} else if (COMMANDID.equalsIgnoreCase(name)){
			// VID:58 , CommandID , CLASS:DVVAL , Format:<A[1...64]>
			response.setAsciiItem(makeString(reportItems.getCommandId()));
		} else if (COMMANDINFO.equalsIgnoreCase(name)){
			// VID:59 , CommandInfo , CLASS:DVVAL or SV , Format:<L[3], 1.<CommandID> 2.<Priority> 3.<Replace> >
			response = setCommandInfoToSecsMsg(response, reportItems);			
		} else if (CURRENTPORTSTATE.equalsIgnoreCase(name)){
			response = setCurrentPortStateToSecsMsg(response);
		} else if (DESTPORT.equalsIgnoreCase(name)){
			// VID:60 DestPort , CLASS:DVVAL or SV , Format:<A[1...64]>
			response.setAsciiItem(makeString(reportItems.getDestPort()));
		} else if (EQPNAME.equalsIgnoreCase(name)){
			// VID:61 EqpName			
			response = setEqpNameToSecsMsg(response);			
		} else if (PRIORITY.equalsIgnoreCase(name)){
			// VID:62 Priority , CLASS:DVVAL or SV , Format:U2
			response.setU2Item(reportItems.getPriority());
		} else if (PORTID.equalsIgnoreCase(name)){
			// VariableName:PORTID , CLASS:DVVAL or SV , Format:U2
			response.setAsciiItem(makeString(reportItems.getCarrierLoc()));
		} else if (REPLACE.equalsIgnoreCase(name)){
			// VID:63 Replace , CLASS:DVVAL or SV , Format:U2
			response.setU2Item(reportItems.getReplace());
		} else if (RESULTCODE.equalsIgnoreCase(name)){
			// ResultCode , CLASS:DVVAL , Format:<U2>
			response.setU2Item(reportItems.getResultCode());
		} else if (SOURCEPORT.equalsIgnoreCase(name)){
			// SourcePort , CLASS:DVVAL , Format:<A[1...64]>
			response.setAsciiItem(makeString(reportItems.getSourcePort()));
		} else if (SPECVERSION.equalsIgnoreCase(name)){
			// SPECVERSION , CLASS:DVVAL , Format:<A[1...64]>
			response.setAsciiItem("SEMI E82");
		} else if (TRANSFERCOMMAND.equalsIgnoreCase(name)){
			// TransferCommand , CLASS:DVVAL or SV , Format: <L <CommandInfo> <TransferInfo> >
			response = setTransferCommandToSecsMsg(response, reportItems);			
		} else if (TRANSFERINFO.equalsIgnoreCase(name)){
			// TransferInfo , CLASS:DVVAL or SV , Format:<L[3] <CarrierID> <Source> <Destination> >
			response = setTransferInfoToSecsMsg(response, reportItems);			
		} else if (TRANSFERPORT.equalsIgnoreCase(name)){
			// TransferPort , CLASS:DVVAL , Format:<A[1..64]>
			response.setAsciiItem(makeString(reportItems.getTransferPort()));
		} else if (TRANSFERPORTLIST.equalsIgnoreCase(name)){
			// TransferPortList , CLASS:DVVAL , <L <Transport> >
			response = setTransferPortListToSecsMsg(response, reportItems);			
		} else if (VEHICLEID.equalsIgnoreCase(name)){
			// VehicleID , CLASS:DVVAL or SV , Format:<A[1...32>]
			response.setAsciiItem(makeString(reportItems.getVehicleId()));
		} else if (VEHICLEINFO.equalsIgnoreCase(name)){
			// VehicleInfo , CLASS:DVVAL or SV , Format:<L[2] <VehicleID> <VehicleState> >
			response = setVehicleInfoToDB(response, reportItems);
		} else if (VEHICLESTATE.equalsIgnoreCase(name)){
			// VehicleState , CLASS:DVVAL or SV , Format:<U2>
			response.setU2Item(reportItems.getVehicleState());
		} else if (TSCSTATE.equalsIgnoreCase(name)){
			// TSCState , CLASS:SV , Format:<U2>			
			response = setTSCStatusToSecsMsg(response);
		} else if (REMOTECMD.equalsIgnoreCase(name)){
			// CommandType , CLASS:DVVAL , Format:<A[1...20]>
			response.setAsciiItem(TRANSFER);
		} else if (ENHANCEDCARRIERINFO.equalsIgnoreCase(name)){
			// EnhancedCarrierInfo , CLASS:DVVAL , Format:<L[4] <CarrierID> <VehicleID> <CarrierLoc> <InstallTime> >
			response = setEnhancedCarrierInfoToSecsMsg(response, reportItems.getCarrierId());
		} else if (ENHANCEDTRANSFERS.equalsIgnoreCase(name)){
			// EnhancedTransfers , CLASS:SV , Format:< L[n] <EnhancedTransferCommand> ...>
			response = setEnhancedTransfersToSecsMsg(response);
		} else if (TRANSFERCOMPLETEINFO.equalsIgnoreCase(name)){
			// TransferCompleteInfo , CLASS:DVVAL , Format:<L[n] L,2 <TransferInfo> <CarrierLoc> .......>
			response = setTransferCompleteInfoToSecsMsg(response, reportItems);
		} else if (ENHANCEDCARRIERS.equalsIgnoreCase(name)){
			// EnhancedCarriers , CLASS:SV , Format:<L[n] <EnhancedCarrierInfo>.....>
			response = setEnhancedCarriersToSecsMsg(response);
		} else if (TRANSFERSTATE.equalsIgnoreCase(name)){
			// TransferState , CLASS:DVVAL , Format:U2
			response = setTransferStateToSecsMsg(response, reportItems.getCommandId());
		} else if (INSTALLTIME.equalsIgnoreCase(name)){
			// InstallTime , CLASS:DVVAL , Format:TIME (A[16])
			response = setInstallTimeToSecsMsg(response, reportItems);
		} else if (ENHANCEDTRANSFERCOMMAND.equalsIgnoreCase(name)){
			// EnhancedTransferCommand , CLASS:SV , Format:< L[3] <CommmandInfo> <TransferState> <L,n <TransferInfo> ..... > >
			response = setEnhancedTransferCommandToSecsMsg(response, reportItems.getCommandId());
		}		
		/* STBC only comparing IBSEM */
		else if (CARRIERSTATE.equalsIgnoreCase(name)){
			response.setU2Item(reportItems.getCarrierState());
		} else if (IDREADSTATUS.equalsIgnoreCase(name)){
			response.setU2Item(reportItems.getIdReadStatus());
		} else if (STBSTATE.equalsIgnoreCase(name)){
			response = setTSCStatusToSecsMsg(response);
		} else if (VEHICLETYPE.equalsIgnoreCase(name)){
			// 2012.05.16 by MYM : VehicleType 추가
			// Rail-Down - S1a Foup, Reticle 통합 반송시 사양(IBSEM Spec for Conveyor usage in one OHT) 대응
			// 2013.01.04 by KYK : B -> U2 (반도체 VOC)
//			response.setBinaryItem(reportItems.getVehicleType());			
			response.setU2Item(reportItems.getVehicleType());			
		}	
		// 2012.08.08 by KYK : alarmText VID 추가 (TP VOC)
		else if (ALARMTEXT.equalsIgnoreCase(name)){
			// VID:? , CommandID , CLASS:DVVAL , Format:<A[1...64]>
			response.setAsciiItem(makeString(reportItems.getAlarmText()));
		}
		// 2013.10.01 by MYM : UnitAlarmSet, UnitAlarmCleared 추가
		else if (VEHICLECURRENTDOMAIN.equalsIgnoreCase(name)){
			// VID:251 , VehicleCurrentDomain , CLASS:DVVAL , Format:<A[1...32]>
			response.setAsciiItem(makeString(reportItems.getVehicleCurrentDomain()));
		}
		else if (VEHICLECURRENTPOSITION.equalsIgnoreCase(name)){
			// VID:251 , VehicleCurrentPosition , CLASS:DVVAL , Format:<U4>
			response.setU4Item(reportItems.getVehicleCurrentPosition());
		}
		else if (ACTIVECARRIERS2.equalsIgnoreCase(name)) {
			// VID:151 , List current status of all carrier InfoEx, CLASS:SV, Format:<L[n] <CarrierInfoEx>...>
			response = setActiveCarriers2ToSecsMsg(response);
		}
		else if (FOUPID.equalsIgnoreCase(name)){
			// VID:154 , FoupID, CLASS:DVVAL , Format:<A[1...64]>
			response.setAsciiItem(makeString(reportItems.getFoupId()));
		}
		else if (FLOORID.equalsIgnoreCase(name)){
			// VID:270 , FloorID
			response.setAsciiItem(makeString(reportItems.getFloorId()));
		}
		/* 조건문 추가 시 : 코드수정없이 삽입함수로 처리  */
		else if (isExtraVidUsed()){
			response = setExtraVariableToSecsMsg(response, name, reportItems);
		} else {			
			// Error : Undefined VID
			strLog = "Fail to set Variable Data : Invalid VID" + vId;
			writeSEMLog(strLog);
		}
		
		return response;						
	}
	
	public UComMsg setAllVariablesToSecsMsg(UComMsg response) {
		return response;
	}

	public UComMsg setVariablesToSecsMsg(UComMsg response, int vId, String commandId,
			String carrierId, String vehicleId, int alarmId) {
		ReportItems reportItems = new ReportItems();		
		reportItems.setAlarmId(alarmId);
		reportItems.setCommandId(commandId);
		reportItems.setCarrierId(carrierId);
		reportItems.setVehicleId(vehicleId);
		
		return setVariablesToSecsMsg(response, vId, reportItems);
	}
	
	/**
	 * 
	 * @param eventName
	 * @param commandId
	 * @param vehicleId
	 * @param alarmId
	 * @return
	 */
	public int sendS6F11(String eventName, String commandId, String vehicleId, int alarmId) {
		ReportItems reportItems = new ReportItems();		
		reportItems.setCommandId(commandId);
		reportItems.setVehicleId(vehicleId);
		reportItems.setAlarmId(alarmId);
		// 2012.08.08 by KYK : alarmText VID 추가 (TP VOC)
		if (alarmIdOTable != null) {
			AlarmItem alarm = alarmIdOTable.get(alarmId);
			if (alarm != null) {
				reportItems.setAlarmText(alarm.getAlarmText());				
			}
		}

		return sendS6F11(eventName, reportItems);
	}
	
	public UComMsg setEstablishCommunicationTimeoutToSecsMsg(UComMsg response) {
		EquipmentConstant equipmentConstant;
		// VID(ECID) : 2
		if (ecIdOTable.containsKey(2)) {
			equipmentConstant = ecIdOTable.get(2);
			response.setU2Item(equipmentConstant.getNumValue());			
		}
		return response;
	}
	
	public UComMsg setAlarmEnabledToSecsMsg(UComMsg response) {
		int	itemCount;
		AlarmItem alarmItem;
		itemCount = 0;
		/* Enabled Alarm count */
		Iterator<AlarmItem> iter = alarmIdOTable.values().iterator();
		while (iter.hasNext()) {			
			alarmItem = (AlarmItem) iter.next();
			if (alarmItem.isEnabled()) {
				itemCount++;
			}						
		}
		response.setListItem(itemCount);
		Iterator<AlarmItem> iter2 = alarmIdOTable.values().iterator();
		while (iter2.hasNext()) {			
			alarmItem = (AlarmItem) iter2.next();
			if (alarmItem.isEnabled()) {
				response.setU4Item(alarmItem.getAlarmId());
			}						
		}
		return response;
	}

	public UComMsg setTransferCompleteInfoToSecsMsg(UComMsg response, ReportItems reportItems) {
		
		if (reportItems.getCommandId() == null) {
			response.setListItem(0);
		} else {
			response.setListItem(1);
			response.setListItem(2); // TransferInfo, CarrierLoc
			// TransferInfo
			response.setListItem(3); 
			response.setAsciiItem(makeString(reportItems.getCarrierId()));
			response.setAsciiItem(makeString(reportItems.getSourcePort()));
			response.setAsciiItem(makeString(reportItems.getDestPort()));
			// CarrierLoc
			response.setAsciiItem(makeString(reportItems.getCarrierLoc()));			
		}
		return response;
	}

	public UComMsg setVehicleInfoToDB(UComMsg response, ReportItems reportItems) {
		response.setListItem(2); // L,2
		response.setAsciiItem(reportItems.getVehicleId());
		response.setU2Item(reportItems.getVehicleState());
		return response;
	}

	public UComMsg setTransferPortListToSecsMsg(UComMsg response, ReportItems reportItems) {		
		response.setListItem(1);
		response.setAsciiItem(reportItems.getTransferPort());
		return response;
	}

	public UComMsg setTransferInfoToSecsMsg(UComMsg response, ReportItems reportItems) {
		response.setListItem(3);
		response.setAsciiItem(reportItems.getCarrierId());
		response.setAsciiItem(reportItems.getSourcePort());
		response.setAsciiItem(reportItems.getDestPort());
		return response;
	}

	public UComMsg setTransferCommandToSecsMsg(UComMsg response, ReportItems reportItems) {
		response.setListItem(2);
		response.setListItem(3);
		response.setAsciiItem(reportItems.getCommandId());
		response.setU2Item(reportItems.getPriority());
		response.setU2Item(reportItems.getReplace());
		response.setListItem(1);
		response.setListItem(3);
		response.setAsciiItem(reportItems.getCarrierId());
		response.setAsciiItem(reportItems.getSourcePort());
		response.setAsciiItem(reportItems.getDestPort());		
		return response;
	}

	public UComMsg setEqpNameToSecsMsg(UComMsg response) {
		EquipmentConstant equipmentConstant;
		// VID(ECID) : 61
		if (ecIdOTable.containsKey(61)) {
			equipmentConstant = ecIdOTable.get(61);
			response.setAsciiItem(equipmentConstant.getStrValue());			
		}
		return response;
	}

	public UComMsg setEventEnabledToSecsMsg(UComMsg response) {		
		int itemCount = 0;
		CollectionEvent collectionEvent;
		
		Iterator<CollectionEvent> iter = ceIdOTable.values().iterator();		
		while (iter.hasNext()) {
			collectionEvent = (CollectionEvent)iter.next();
			if (collectionEvent.isEnabled()) {
				itemCount++;
			}
		}
		response.setListItem(itemCount);

		Iterator<CollectionEvent> iter2 = ceIdOTable.values().iterator();
		while (iter2.hasNext()) {
			collectionEvent = (CollectionEvent)iter.next();
			if (collectionEvent.isEnabled()) {
				response.setU2Item(collectionEvent.getCollectionEventId());
			}
		}			
		return response;
	}
	
	public UComMsg setControlStateToSecsMsg(UComMsg response) {
		int item;		
		if (EQ_OFFLINE.equals(controlStatus)) {
			item = 1;
		} else if (ATTEMPT_ONLINE.equals(controlStatus)) {
			item = 2;
		} else if (HOST_OFFLINE.equals(controlStatus)) {
			item = 3;
		} else if (LOCAL_ONLINE.equals(controlStatus)) {
			item = 4;
		} else if (REMOTE_ONLINE.equals(controlStatus)) {
			item = 5;
		} else {
			item = 0;
		}
		String strLog = "	controlStatus:" + controlStatus;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 4, strLog);

		response.setU1Item(item);
		return response;
	}

	public UComMsg setAlarmSetToSecsMsg(UComMsg response) {
		int itemCount = 0;
		AlarmItem alarmItem;

		/* Enabled Alarm count */
		Iterator<AlarmItem> iter = alarmIdOTable.values().iterator();
		while (iter.hasNext()) {			
			alarmItem = (AlarmItem) iter.next();
			if (alarmItem.isActivated()) {
				itemCount++;
			}
		}
		response.setListItem(itemCount);
		Iterator<AlarmItem> iter2 = alarmIdOTable.values().iterator();
		while (iter2.hasNext()) {
			alarmItem = (AlarmItem) iter2.next();
			if (alarmItem.isActivated()) {
				response.setU4Item(alarmItem.getAlarmId());
			}
		}		
		String strLog = "	Enabled Alarm count:" + itemCount;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 4, strLog);
		
		return response;
	}

	/**
	 * 
	 * @param response
	 * @param vId
	 * @return
	 */
	private UComMsg setEcvNamelistToSecsMsg(UComMsg response, int vId) {
		EquipmentConstant equipmentConstant;
		SECSMSG_TYPE valueType;
		Iterator<EquipmentConstant> iter = ecIdOTable.values().iterator();
		boolean result = false;
		
		while (iter.hasNext()) {
			equipmentConstant = (EquipmentConstant)iter.next();
			valueType = equipmentConstant.getValueType();
			
			if (vId == equipmentConstant.getEquipmentConstantId()) {
				if (SECSMSG_TYPE.A == valueType) {
					response = setAsciiEcVToSecsMsg(response, equipmentConstant);
				} else if (SECSMSG_TYPE.B == valueType) {
					response = setBinaryEcVToSecsMsg(response, equipmentConstant);
				} else if (SECSMSG_TYPE.U2 == valueType) {
					response = setU2EcVToSecsMsg(response, equipmentConstant);
				} else if (SECSMSG_TYPE.U4 == valueType) {
					response = setU4EcVToSecsMsg(response, equipmentConstant);
				} else if (SECSMSG_TYPE.BOOLEAN == valueType) {
					response = setAsciiEcVToSecsMsg(response, equipmentConstant);
				} else {
					writeSEMLog(" ValueType of EquipmentConstant is invalid. VID=" + vId);
				}
				result = true;
			}
		}
		if (result == false) {
			writeSEMLog("[RCV S2F29] EquipmentConstant Not Exist. VID:" + vId);			
		}
		return response;
	}

	private UComMsg setU4EcVToSecsMsg(UComMsg response, EquipmentConstant equipmentConstant) {
		response.setListItem(6);
		response.setU2Item(equipmentConstant.getEquipmentConstantId()); // ECID
		response.setAsciiItem(equipmentConstant.getEquipmentConstantName()); // ECNAME
		// U4
		response.setU4Item(equipmentConstant.getMin()); // ECMIN
		response.setU4Item(equipmentConstant.getMax()); // ECMAX
		response.setU4Item(equipmentConstant.getDefVal()); // ECDEF
		response.setAsciiItem(equipmentConstant.getUnit()); // UNITS
		return response;
	}
	
	private UComMsg setU2EcVToSecsMsg(UComMsg response, EquipmentConstant equipmentConstant) {
		response.setListItem(6);
		response.setU2Item(equipmentConstant.getEquipmentConstantId()); // ECID
		response.setAsciiItem(equipmentConstant.getEquipmentConstantName()); // ECNAME
		// U2
		response.setU2Item(equipmentConstant.getMin()); // ECMIN
		response.setU2Item(equipmentConstant.getMax()); // ECMAX
		response.setU2Item(equipmentConstant.getDefVal()); // ECDEF
		response.setAsciiItem(equipmentConstant.getUnit()); // UNITS
		return response;
	}
	
	private UComMsg setBinaryEcVToSecsMsg(UComMsg response, EquipmentConstant eqiupmentConstant) {
		response.setListItem(6);
		response.setU2Item(eqiupmentConstant.getEquipmentConstantId()); // ECID
		response.setAsciiItem(eqiupmentConstant.getEquipmentConstantName()); // ECNAME
		// Binary
		response.setBinaryItem(eqiupmentConstant.getMin()); // ECMIN
		response.setBinaryItem(eqiupmentConstant.getMax()); // ECMAX
		response.setBinaryItem(eqiupmentConstant.getDefVal()); // ECDEF
		response.setAsciiItem(eqiupmentConstant.getUnit()); // UNITS
		return response;
	}
	
	private UComMsg setAsciiEcVToSecsMsg(UComMsg response, EquipmentConstant equipmentConstant) {
		response.setListItem(6);
		response.setU2Item(equipmentConstant.getEquipmentConstantId()); // ECID
		response.setAsciiItem(equipmentConstant.getEquipmentConstantName()); // ECNAME
		// Ascii
		response.setAsciiItem(""); // ECMIN
		response.setAsciiItem(""); // ECMAX
		response.setAsciiItem(""); // ECDEF
		response.setAsciiItem(equipmentConstant.getUnit()); // UNITS
		return response;
	}

	/**
	 * VID 추가시 SECS Msg 변환처리하기 위한 메소드
	 * @param response
	 * @param name
	 * @param reportItems
	 * @return
	 */
	public UComMsg setExtraVariableToSecsMsg(UComMsg response, String name, ReportItems reportItems) {
		// TODO Auto-generated method stub
		return response;
	}

	/**
	 * VID 가 추가되었는지 여부 체크
	 * @return
	 */
	public boolean isExtraVidUsed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/** Get CurrentTime in System : yyyyMMddkkmmssSS
	 * @return
	 */
	public String getCurrentTime() {
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddkkmmssSS");
		return dateFormat.format(now);
	}
	
	/**
	 * Set CurrentTime : yyyyMMddkkmmssSS
	 * @param dateTime
	 * @return
	 */
	public boolean setCurrentTime(String dateTime) {
		String year = dateTime.substring(0, 4);
		String month = dateTime.substring(4, 6);
		String date = dateTime.substring(6, 8);
		String hour = dateTime.substring(8, 10);
		String minute = dateTime.substring(10, 12);
		String second = dateTime.substring(12, 14);

		StringBuilder windowsDate = new StringBuilder();
		windowsDate.append(year); windowsDate.append("-");
		windowsDate.append(month); windowsDate.append("-"); windowsDate.append(date);

		StringBuilder windowsTime = new StringBuilder();
		windowsTime.append(hour); windowsTime.append(":");
		windowsTime.append(minute); windowsTime.append(":"); windowsTime.append(second);

		StringBuilder unixDate = new StringBuilder();
		unixDate.append(month); unixDate.append(date);

		StringBuilder unixTime = new StringBuilder();
		unixTime.append(hour); unixTime.append(minute); 
		unixTime.append(year.substring(2, 4)); unixTime.append(".");unixTime.append(second);
		
		String os = System.getProperty("os.name").toLowerCase();
		
		if (os.indexOf("windows") > -1) {
			try {
				Runtime.getRuntime().exec("cmd.exe /c date " + windowsDate.toString());
				Runtime.getRuntime().exec("cmd.exe /c time " + windowsTime.toString());
			} catch (Exception e) {
				writeSEMLog("Can't change the date and time.");
				return false;
			}
		} else {
			try {
				Runtime.getRuntime().exec("date " + unixDate.toString() + unixTime.toString());
			} catch (Exception e) {
				writeSEMLog("Can't change the date and time.");
				return false;
			}
		}
		return true;
	}
	
	/**
	 * String 의 길이를 맞춤 
	 * @param str
	 * @param k
	 * @return
	 */
	public String makeLength(String str, int k) {
		if (str.length() == k) {
			return str;
		} else if (str.length() < k) {
			int nEmptyLength = k - str.length();
			for (int y = 0; y < nEmptyLength; y++)
				str = str + " ";
			return str;
		} else {
			return str.substring(0, k);
		}
	}

	/**
	 * change enabled/disabled status about All alarms. 
	 * @param isEnabled
	 * @return
	 */
	protected boolean enableAlarmStatus(boolean isEnabled) {
		long alarmId;
		AlarmItem alarmItem;
		String strLog = null;

		if (alarmIdOTable.isEmpty()) {
			strLog = " There's no defined AlarmList.";
			writeSEMLog("          [EnabledAllAlarmStatus()]:"+ isEnabled + strLog);
		}
		
		Iterator<Long> iter = alarmIdOTable.keySet().iterator();
		while (iter.hasNext()) {			
			alarmId = (Long) iter.next();
			alarmItem = (AlarmItem) alarmIdOTable.get(alarmId);
			alarmItem.setEnabled(isEnabled);
			alarmIdOTable.put(alarmId, alarmItem);
		}
		return true;
	}

	/**
	 * change enabled/disabled status On alId's alarm.
	 * @param alarmId
	 * @param isEnabled
	 * @return
	 */
	protected boolean enableAlarmStatus(long alarmId, boolean isEnabled) {
		AlarmItem alarmItem;
		String strLog = null;
		
		if (alarmIdOTable.containsKey(alarmId)) {
			alarmItem = (AlarmItem) alarmIdOTable.get(alarmId);
			alarmItem.setEnabled(isEnabled);
			alarmIdOTable.put(alarmId, alarmItem);
			return true;
		} else {
			strLog = "There's no defined AlarmList.";
			writeSEMLog("          [EnabledAllAlarmStatus()]:" + isEnabled + strLog);
			return false;			
		}		
	}

	/**
	 * reportIdOTable 에서 reportId 에 해당하는 EventReport 객체를 가져옴 
	 * @param reportId
	 * @return
	 */
	protected EventReport getEventReportObject(int reportId) {
		EventReport eReport = (EventReport) reportIdOTable.get(reportId);
		if (eReport == null) {
			writeSEMLog("GetEventReportObject-pReport is null ");
		}
		return eReport;
	}

	/**
	 * ceIdOTable 에서 ceId 에 해당하는 CollectionEvent 객체를 가져옴 
	 * @param collectionEventId
	 * @return
	 */
	protected CollectionEvent getCollectionEventObject(int collectionEventId) {
		CollectionEvent collectionEvent = null;
		if (ceIdOTable.containsKey(collectionEventId)) {
			collectionEvent = (CollectionEvent) ceIdOTable.get(new Integer(collectionEventId));	
		} else {
			writeSEMLog("CEID Not Exists in CollectionEventTable ");
		}
		return collectionEvent;		
	}
	
	protected void resetCEIDLink(int collectionEventId) {
		ceIdOTable.put(collectionEventId, null);		
	}
	
	/**
	 * 
	 */
	protected void resetAllCEIDLink() {
//		ceIdOTable.clear();		
		for (int ceId:ceIdOTable.keySet()) {
			ceIdOTable.put(ceId, null);	
		}
	}

	protected void deleteAllReportDefinition() {
		reportIdOTable.clear();
	}
	
	/**
	 * 
	 * @param message
	 * @param itemCount
	 * @param isEnabled
	 */
	protected void enableCollectionEvent(UComMsg message, int itemCount, boolean isEnabled) {
		int collectionEventId;		
		for (int i = 0; i < itemCount; i++) {
			collectionEventId = message.getU2Item(); // CEID			
			CollectionEvent collectionEvent = ceIdOTable.get(collectionEventId);
			if (collectionEvent == null) {
				collectionEvent = new CollectionEvent();
			}
			collectionEvent.setEnabled(isEnabled);			
			ceIdOTable.put(collectionEventId, collectionEvent);
		}		
		/* DB Update */
	}
	
	/**
	 * 
	 * @param isEnabled
	 */
	protected void enableAllCollectionEvent(boolean isEnabled) {
		for (int collectionEventId:ceIdOTable.keySet()){
			CollectionEvent collectionEvent = ceIdOTable.get(collectionEventId);
			if (collectionEvent == null) {
				System.out.println("cEvent is null");
				collectionEvent = new CollectionEvent();
			}
			collectionEvent.setEnabled(isEnabled);	
			ceIdOTable.put(collectionEventId, collectionEvent);			
		}
		/* DB Update */
	}	
	
	/**
	 * 
	 * @param string
	 * @return
	 */
	public String makeString(String string) {
		if (string == null){
			return "";
		} else {
			return string;
		}
	}
	
	/**
	 * 
	 * 2012.01.06 by PMM
	 */
	private void traceFormattedSECSMessage(SECSMessage secsMessage) {
		StringBuffer message = new StringBuffer();
		message.append("[\"").append(sdf2.format(new Date())).append("\",");
		message.append("\"").append(secsMessage.getTscId()).append("\",");
		message.append("\"").append(secsMessage.getMessageType()).append("\",");
		message.append("\"").append(secsMessage.getEventName()).append("\",");
		message.append("\"").append(secsMessage.getCommandId()).append("\",");
		message.append("\"").append(secsMessage.getCarrierId()).append("\",");
		message.append("\"").append(secsMessage.getLotId()).append("\",");
		message.append("\"").append(secsMessage.getSourceLoc()).append("\",");
		message.append("\"").append(secsMessage.getDestLoc()).append("\",");
		message.append("\"").append(secsMessage.getCurrLoc()).append("\",");
		message.append("\"").append(secsMessage.getResultCode()).append("\",");
		message.append("\"").append(secsMessage.getDeviceId()).append("\",");
		message.append("\"").append(secsMessage.getVehicleId()).append("\",");
		message.append("\"").append(secsMessage.getMoveStatus()).append("\",");
		message.append("\"").append(secsMessage.getFullMessage()).append("\"]");
		
		message.insert(0, getMessageSizeInfoForLogServer(message.toString()));
		SECSMessageTraceFormatLog.debug(message.toString());
	}
	
	/**
	 * 
	 * 2012.01.06 by PMM
	 */
	private String getMessageSizeInfoForLogServer(String message) {
		if (message != null) {
			String size = Integer.toString(message.length(), 36);
			if (size.length() < 5) {
				return FOUR_ZEROS.substring(0, 4 - size.length()) + size;
			} else {
				return size;
			}
		} else {
			return FOUR_ZEROS;
		}
	}
	
//	public boolean isFormattedLogUsed() {
//		return isFormattedLogUsed;
//	}
	
	/**
	 * 
	 * 2012.01.06 by PMM
	 */
	public void setFormattedLogUsed(boolean isFormattedLogUsed) {
		this.isFormattedLogUsed = isFormattedLogUsed;
	}
}