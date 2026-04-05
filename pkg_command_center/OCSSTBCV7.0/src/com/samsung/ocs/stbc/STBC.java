package com.samsung.ocs.stbc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.manager.impl.AlarmManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.STBCarrierLocManager;
import com.samsung.ocs.manager.impl.STBInfoManager;
import com.samsung.ocs.manager.impl.STBRfcDataManager;
import com.samsung.ocs.manager.impl.STBStateManager;
import com.samsung.ocs.manager.impl.model.Alarm;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.OCSInfo;
import com.samsung.ocs.manager.impl.model.STBCarrierLoc;
import com.samsung.ocs.manager.impl.model.STBInfo;
import com.samsung.ocs.manager.impl.model.STBRfcData;
import com.samsung.ocs.manager.impl.model.STBState;
import com.samsung.ocs.stbc.rfc.RFCManager;
import com.samsung.sem.SEMCommon;
import com.samsung.sem.items.CollectionEvent;
import com.samsung.sem.items.EquipmentConstant;
import com.samsung.sem.items.EventReport;
import com.samsung.sem.items.ReportItems;
import com.samsung.sem.ucominterface.UComMsg;

/**
 * STBC Class, OCS 3.0 for Unified FAB
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

public class STBC extends SEMCommon implements STBCConstant{
	public boolean isFirst = true;	
	private int NO_ERROR = 0;
	private int ERROR_BASE = 1000;
	private int ER_UNDEFINED_ECID = (ERROR_BASE + 1);
	private long errorStbSeqNo = 1; // ErrorSTB 발생시 붙일 연번
	private long errorStbFoupSeqNo = 1; // ErrorSTB Foup 발생시 붙일 연번
	private String semStatus = STBC_NONE;	
	private String driverInitFileName = "SECom.xml";

	// 2013.01.04 by KYK
	public static String STBBACKUPDATA = "STBBackupData";
	
	// 2012.03.10 by KYK : received IDREADLIST(S2F49) command Info list
	private ArrayList<HashMap> readAllRequestList = new ArrayList<HashMap>();
	
	// STBCMain  
	private STBCManager stbcManager;
	private RFCManager rfcManager;
	// DataManager
	private STBStateManager stbStateManager;
	private STBInfoManager stbInfoManager;
	private OCSInfoManager ocsInfoManager;
	private STBRfcDataManager stbRfcDataManager;	
//	private STBCHistoryManager stbcHistoryManager;
	private STBCarrierLocManager stbCarrierLocManager;
	private AlarmManager alarmManager; // 2013.01.04 by KYK
	
	/**
	 * Constructor of STBC class.
	 * 
	 * @param stbcManager
	 * @param rfcManager
	 */
	public STBC(STBCManager stbcManager, RFCManager rfcManager){
		super();
		this.stbcManager = stbcManager;
		this.rfcManager = rfcManager;		
		stbInfoManager = STBInfoManager.getInstance(STBInfo.class, null, false, false, 0);
		ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, null, false, false, 0);
		stbStateManager = STBStateManager.getInstance(STBState.class, null, false, false, 0);
		stbRfcDataManager = STBRfcDataManager.getInstance(STBRfcData.class, null, false, false, 0);
		// 2013.01.29 by KYK : DBdelay 발생 (delete 시) -> 사용안하도록함 (향후 formattedLog로 대체함)
//		stbcHistoryManager = STBCHistoryManager.getInstance(STBCHistory.class, null, false, false, 0);
		stbCarrierLocManager = STBCarrierLocManager.getInstance(CarrierLoc.class, null, false, false, 0);		
		alarmManager = AlarmManager.getInstance(Alarm.class, null, false, false, 0); // 2013.01.04 by KYK
		
		initializeSTBC();				
		startSTBC();
	}	

	/**
	 * STBC Service start : SECS 통신위한 SEComDriver 연결하고, RFC 와 통신을 연결한다.
	 * @return
	 */
	boolean startSTBC() {		
		uCom.setCommCfgFile(driverInitFileName);
		if (STBC_INIT.equals(semStatus) || STBC_STOP.equals(semStatus)){			
			// start SECom
			if (uCom.startService()) {
				semStatus = STBC_START;				
				writeSEMLog(" STBC(& SEComDriver)'s Started !!");
				// start RFCManager
				if (isRFCUsed()) {
					rfcManager.startRFCManager();
					writeSEMLog("RFCMANAGER's Started !!");									
				} else {
					writeSEMLog("RFCMANAGER's Not Started !! (RFCUSAGE=NO)");														
				}
				return true;
			} else {
				writeSEMLog("STBC Fail to Start, SECS Driver Not Started !!");
				return false;
			}			
		} else {
			//writeSEMLog("IBSEM's Already Started before !!");
			return false;
		}		
	}
	
	/**
	 * STBC Service stop : SECS 통신위한 SEComDriver 연결종료, RFC 와 통신을 연결종료
	 * @return
	 */
	boolean stopSTBC() {		
		if (uCom.stopService()) {			
			hsmsStatus = TCPIP_NOT_CONNECTED;
			commStatus = COMM_DISABLED;
			controlStatus = CONTROL_NONE;
			tscStatus = TSC_NONE;
//			tscAlarmStatus = "TSC_NO_ALARMS";	
			semStatus = STBC_STOP;
			setSEMStatusToDB();			
			writeSEMLog("[STBC's Stopped !!] ********************");
			// stop RFCManager
			if (isRFCUsed()) {
				rfcManager.stopRFCManager();
				writeSEMLog("RFCManager's Stopped !!");				
			}
			return true;
		} else {
			writeSEMLog("STBC Fail, SECS Driver Not Stopped !!");
			return false;
		}
	}

	/**
	 * status 초기화  및 VID,ReportData 기준정보 업로드
	 */
	private void initializeSTBC() {
		mdln = STBC;
		softRev = "v3.0.0";
		semStatus = STBC_INIT;				
		tscStatus = TSC_NONE;
		commStatus = COMM_DISABLED;
		controlStatus = EQ_OFFLINE;
		hsmsStatus = TCPIP_NOT_CONNECTED;
		setSEMStatusToDB();
		// Load Data
		loadEcVData();
		loadVidData();		
		loadDynamicReportData();		
		loadAlarmData();
	}

	@Override
	public boolean loadAlarmData() {
		// Do nothing here.
		return true;
	}	

	/**
	 * Load EcV Data
	 */
	@Override
	public boolean loadEcVData() {
		EquipmentConstant ec = new EquipmentConstant();		
		ec.setData(61, EQPNAME, mdln, "");
		ecIdOTable.put(61, ec);
		return true;
	}
	
	@Override
	public boolean loadCollectionEventData() {
		return loadCollectionEventDataFromXml();
	}

	@Override
	public boolean loadEventReportData() {
		return loadEventReportDataFromXml();
	}

	@Override
	public boolean loadVidData() {
		return loadVidDataFromXml();
	}

	/**
	 * VID (VID,NAME) 정보를 xml file 로부터 읽어온다.
	 */
	public boolean loadVidDataFromXml() {
		int vid;
		String strVid;
		String name;		
		Element vidItem;		

		String homePath = System.getProperty(HOMEDIR);
		String fileSeparator = System.getProperty(FILESEPARATOR);
		String fileName = "STBVID.xml";
		String configFile = homePath + fileSeparator + fileName;

		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		
		try {
			doc = saxb.build(configFile);
			// 2012.09.13 by KYK : 위치이동 Integer.parseInt 하다가 혹시 잘못 exception 발생할수도 있을거같아서
			Element vidElement = doc.getRootElement();
			List<Element> list = vidElement.getChildren(ITEM);		
			Iterator<Element> iter = list.iterator();		
			while (iter.hasNext()) {
				vidItem = (Element) iter.next();
				strVid = vidItem.getChildTextTrim(VID);
				vid = Integer.parseInt(strVid);			
				name = vidItem.getChildTextTrim(NAME);
				
				if (strVid != null && name != null) {
					vIdOTable.put(vid, name);				
					//writeSEMLog("VID,NAME ("+vid+","+name+")");
				}
			}			
		} catch (JDOMException e) {
			e.printStackTrace();
			traceException("loadVidDataFromXml()", e);
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			traceException("loadVidDataFromXml()", e);
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			traceException("loadVidDataFromXml()", e);	
			return false;
		}		
		writeSEMLog(" VID's Loaded from 'STBVID.xml' / VIDCount: " + vIdOTable.size());
		return true;
	}

	/**
	 * ReportData (CollectionEvent,Report) 를 읽어온다.
	 */
	public void loadDynamicReportData(){		
		// load Report (REPORTID,VIDS)
		loadEventReportDataFromXml();
		// load CollectionEvent (CEID,NAME,RPIDS,ENABLED)
		loadCollectionEventDataFromXml();		
	}
		
	/**
	 * Report Data (REPORTID, VIDS) 를 xml file 로 부터 읽어온다.
	 */
	public boolean loadEventReportDataFromXml() {
		int reportId;		
		String strReportId;
		String vids;
		Element reportItem;		
		
		String homePath = System.getProperty(OcsConstant.HOMEDIR);
		String fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		String fileName = "STBReport.xml";
		String configFile = homePath + fileSeparator + fileName;
		
		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		
		try {
			doc = saxb.build(configFile);
			//
			Element reportElement = doc.getRootElement();
			List<Element> list = reportElement.getChildren(REPORTITEM);
			Iterator<Element> iter = list.iterator();		
			
			while (iter.hasNext()) {
				reportItem = (Element) iter.next();
				strReportId = reportItem.getChildTextTrim(REPORTID);
				reportId = Integer.parseInt(strReportId);			
				vids = reportItem.getChildTextTrim(VID);
				
				if (strReportId != null && vids != null) {
					EventReport eReport = new EventReport();
					eReport.setReportId(reportId);
					
					int vidCnt = 0;
					if (vids.length() > 0) {
						String[] saVid = vids.split(",");
						vidCnt = saVid.length;
						for (int i = 0; i < vidCnt; i++){
							eReport.setVid(i, Integer.parseInt(saVid[i]));
						}
					}
					eReport.setVidQty(vidCnt);
					reportIdOTable.put(reportId, eReport);				
				}
			}					
		} catch (JDOMException e) {
			e.printStackTrace();
			traceException("loadEventReportDataFromXml()", e);
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			traceException("loadEventReportDataFromXml()", e);
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			traceException("loadEventReportDataFromXml()", e);
			return false;			
		}
		writeSEMLog(" EventReport's Loaded from 'STBReport.xml' / ReportCount: " + reportIdOTable.size());
		return true;
	}

	/**
	 * Load CollectionEvent Data from XML
	 * 
	 * @return
	 */
	public boolean loadCollectionEventDataFromXml() {
		int ceId;
		boolean isEnabled;
		String strCeId;
		String reportIds;
		String eventName;
		String enabled;
		Element ceIdItem;	
		
		String homePath = System.getProperty(HOMEDIR);
		String fileSeparator = System.getProperty("file.separator");
		String fileName = "STBCollectionEvent.xml";
		String configFile = homePath + fileSeparator + fileName;

		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		
		try {
			doc = saxb.build(configFile);
			//
			Element ceIdElement = doc.getRootElement();
			List<Element> list = ceIdElement.getChildren(ITEM);
			Iterator<Element> iter = list.iterator();
			
			while (iter.hasNext()) {
				ceIdItem = (Element) iter.next();
				strCeId = ceIdItem.getChildTextTrim(CEID);
				ceId = Integer.parseInt(strCeId);			
				eventName = ceIdItem.getChildTextTrim(NAME);
				reportIds = ceIdItem.getChildTextTrim(REPORTID);
				enabled = ceIdItem.getChildTextTrim(ENABLED);
				
				if (TRUE.equals(enabled)) {
					isEnabled = true;
				} else {
					isEnabled = false;
				}			
				if (strCeId != null && reportIds != null) {
					CollectionEvent cEvent = new CollectionEvent();
					cEvent.setCollectionEventId(ceId);
					cEvent.setEventName(eventName);
					cEvent.setEnabled(isEnabled);
					
					int reportIdCnt = 0;
					if (reportIds.length() > 0) {
						String[] saReportId = reportIds.split(",");
						reportIdCnt = saReportId.length;
						for (int i = 0; i < reportIdCnt; i++) {
							cEvent.setReportIdAt(i, Integer.parseInt(saReportId[i]));
						}
					}
					cEvent.setReportIdQty(reportIdCnt);
					ceIdOTable.put(ceId, cEvent);
					eventNameCeIdTable.put(eventName, ceId);				
					//writeSEMLog("CEID,EVENTNAME ("+ceId+","+eventName+")");
				}
			}
		} catch (JDOMException e) {
			e.printStackTrace();
			traceException("loadCollectionEventDataFromXml()", e);
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			traceException("loadCollectionEventDataFromXml()", e);
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			traceException("loadCollectionEventDataFromXml()", e);
			return false;			
		}
		writeSEMLog(" CollectionEvent's Loaded from 'STBCollectionEvent.xml' / ReportCount: " + ceIdOTable.size());
		return true;
	}

	/*d*********************************************
	 * Communication Scenarios (between MCS and STBC)
	 **********************************************
	 * 1. Startup Scenarios
	 * 
	 * MCS <- OCS	: S1F13	Communication Request
	 * MCS -> OCS	: S1F13	Communication Request
	 * MCS <- OCS	: S1F14 reply Ack
	 *
	 * MCS -> OCS	: S1F17	Online Request
	 * MCS <- OCS	: S6F11 ControlStatusRemote
	 * MCS <- OCS	: S6F11 TSCAutoInitiated
	 * MCS <- OCS	: S6F11 TSCPaused
	 * MCS <- OCS	: S1F18 reply Ack (S1F17)
	 * MCS -> OCS	: S6F12 reply Ack (ControlStatusRemote)
	 * MCS -> OCS	: S6F12 reply Ack (TSCAutoInitiated)
	 * MCS -> OCS	: S6F12 reply Ack (TSCPaused)
	 * 
	 * MCS -> OCS	: S2F31	Date & Time Set
	 * MCS <- OCS	: S2F32 reply Ack
	 * 
	 * MCS -> OCS	: S2F15	Equipment Constant
	 * MCS <- OCS	: S2F16 reply Ack
	 * 
	 * MCS -> OCS	: S2F37	Disable Event
	 * MCS <- OCS	: S2F38 reply Ack

	 * MCS -> OCS	: S2F33	Define Report (Reset)
	 * MCS <- OCS	: S2F34 reply Ack
	 * MCS -> OCS	: S2F33	Define Report
	 * MCS <- OCS	: S2F34 reply Ack
	 * 
	 * MCS -> OCS	: S2F35	Link EventReport
	 * MCS <- OCS	: S2F36 reply Ack
	 * 
	 * MCS -> OCS	: S2F37	Enable Event
	 * MCS <- OCS	: S2F38 reply Ack
	 * 
	 * MCS -> OCS	: S5F3	Disable Alarm 
	 * MCS <- OCS	: S5F4 reply Ack
	 * MCS -> OCS	: S5F3	Enable Alarm
	 * MCS <- OCS	: S5F4 reply Ack
	 * 
	 * - This part of STBC is Different from IBSEM -------------
	 * MCS -> OCS	: S1F3	[VID]=6 Report ControlState
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=64 Report STBState
	 * MCS <- OCS	: S1F4 reply Ack	 
	 * MCS -> OCS	: S1F3	[VID]=118 Report CurrentPortState
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=51 Report ActiveCarriers
	 * MCS <- OCS	: S1F4 reply Ack
	 * 
	 * MCS -> OCS	: S2F49	INSTALLLIST (One Message)
	 * MCS <- OCS	: S2F50 reply Ack
	 *
	 **/
	
	/** S1F19 : Require Particular AZFS List
	 * (Direction : H --> E ) */	
	public void receiveS1F19(UComMsg msg) {
		String strLog = " [RCV S1F19] Require Particular AZFS List";
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 1, 19, strLog);

		int itemCnt;
		int subItemCnt;
		int subItemCnt2;
		String portId;
		ArrayList<String> portIdList = new ArrayList<String>();
		StringBuilder sb = new StringBuilder("PORTLIST: ");
		itemCnt = msg.getListItem(); // L, 3
		String str = msg.getAsciiItem();
		if (itemCnt != 3 || AZFSLIST.equals(str) == false) {
			//
		}
		subItemCnt = msg.getListItem(); // L, n
		for (int i = 0; i < subItemCnt; i++) {
			portId = msg.getAsciiItem();
			portIdList.add(portId);
			sb.append(portId).append("/");
		}
		subItemCnt2 = msg.getListItem(); // L, 4
		str = msg.getAsciiItem();
		if (PORTID.equalsIgnoreCase(str) == false) {
			writeSEMLog("[RCV S1F19] Invalid CPName : PORTID");
		}
		str = msg.getAsciiItem();
		if (CARRIERID.equalsIgnoreCase(str) == false) {
			writeSEMLog("[RCV S1F19] Invalid CPName : CARRIERID");
		}
		str = msg.getAsciiItem();
		if (CARRIERSTATE.equalsIgnoreCase(str) == false) {
			writeSEMLog("[RCV S1F19] Invalid CPName : CARRIERSTATE");
		}
		str = msg.getAsciiItem();
		if (INSTALLTIME.equalsIgnoreCase(str) == false) {
			writeSEMLog("[RCV S1F19] Invalid CPName : INSTALLTIME");
		}				
		writeSEMLog(sb.toString());
		sendS1F20(msg, portIdList);
	}

	/** S1F20 : Require Particular AZFS List
	 * (Direction : H <-- E ) */	
	private void sendS1F20(UComMsg msg, ArrayList<String> portIdList) {
		// -------------------------------------------------------------------
		//		Structure	L,2
		//		1. L,m         [m = number of objects for which data is sent]
		//		   1.L,4     [n = number of attributes returned for OBJID1]
		//		          1. ＜PORTID＞A
		//		          1. ＜CARRIERID＞A
		//		          1. ＜CARRIERSTATE＞U2
		//		          1. ＜INSTALLTIME＞A
		//		       …
		//		   m.L,n     [n = number of attributes returned for OBJIDm]
		//		          1. ＜ATTRDATA1＞
		//		              …
		//		          n. ＜ATTRDATAn＞
		//		  2.L,p           [p = # errors reported] 
		//		       1. L,2
		//		          1. ＜ERRCODE1＞
		//		          2. ＜ERRTEXT1＞
		//		       …
		//		       p.L,2
		//		1. ＜ERRCODEp＞
		//		          2. ＜ERRTEXTp＞          
		// -------------------------------------------------------------------

		String portId;
		String carrierId;
		String carrierState;
		String installTime;
		STBCarrierLoc carrierLoc;
		ConcurrentHashMap<String, Object> carrierLocTable = stbCarrierLocManager.getData();
		int itemCnt = portIdList.size();
		UComMsg rsp = uCom.makeSecsMsg(2, 42, msg.getSysbytes());
		rsp.setListItem(2); // L, 2		
		rsp.setListItem(itemCnt); // L, itemCnt
		for (int i = 0; i < itemCnt; i++) {
			portId = portIdList.get(i);
			carrierLoc = (STBCarrierLoc) carrierLocTable.get(portId);
			carrierId = makeString(carrierLoc.getCarrierId());
			carrierState = makeString(carrierLoc.getCarrierState());
			installTime = makeString(carrierLoc.getInstalledTime());
			
			rsp.setAsciiItem(portId);
			rsp.setAsciiItem(carrierId);
			rsp.setU2Item(convertCarrierStateToU2(carrierState));
			rsp.setAsciiItem(installTime);			
		}
		rsp.setListItem(0); // error reported ?? 일단무시함
		sendUComMsg(rsp, "S1F20");		
		
		String strLog = "[SND S1F20]: Report Particular AZFS List";
		writeSEMLog(strLog);	
		updateSEMHistory(SENT, 1, 20, strLog);		
	}

	/**
	 * Convert CarrierState to U2
	 * 
	 * @param carrierState
	 * @return
	 */
	private int convertCarrierStateToU2(String carrierState) {
		if (INSTALLED.equals(carrierState)) {
			return 1;
		} else {
			return 0;
		}		
	}

	/**
	 * Receive S2F41
	 */
	@Override
	public void receiveS2F41(UComMsg msg) {		
		int hcAck = 4; // HCACK
		int itemCnt = msg.getListItem(); // L, 2
		String rCmd = msg.getAsciiItem();
		
		String strLog = "[MCS->STBC] S2F41 : RCMD = " + rCmd;
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 41, strLog);
				
		if (controlStatus.equals(REMOTE_ONLINE) == false) {
			hcAck = 2; // HCACK 2 : currently not able to execute
			strLog = "[MCS->STBC] S2F41 : CONTROLStatus is not Online/HCACK=" + hcAck;
			writeSEMLog(strLog);	
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			// 2012.03.09 by KYK : 응답순서맞춤 (S2F42 응답메시지 전,이벤트보고 방지)
			sendS2F42(msg, hcAck);
		} else {
			if (PAUSE.equals(rCmd)) {			
				receivePAUSECommand(msg);
			} else if (RESUME.equals(rCmd)) {			
				receiveRESUMECommand(msg);
			} else if (INSTALL.equals(rCmd)) {
				receiveINSTALLcommand(msg);
			} else if (INSTALL2.equals(rCmd)) {
				receiveINSTALL2command(msg);
			} else if (REMOVE.equals(rCmd)) {
				receiveREMOVEcommand(msg);
			} else if (IDREAD.equals(rCmd)) {
				receiveIDREADcommand(msg);
			} else {
				// Error - Undefined Remote (Host) Command..
				hcAck = 1; // command doesn't exist		
				strLog = "[MCS->STBC] S2F41 : Undefined RemoteCmd, RCMD: " + rCmd;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				// 2012.03.09 by KYK
				sendS2F42(msg, hcAck);
			}				
		}		
	}

	/**
	 * Send S2F42
	 * 
	 * @param msg
	 * @param hcAck
	 */
	private boolean sendS2F42(UComMsg msg, int hcAck) {		
		UComMsg rsp = uCom.makeSecsMsg(2, 42, msg.getSysbytes());
		rsp.setListItem(2); // L, 2
		rsp.setBinaryItem(hcAck); // HCACK
		rsp.setListItem(0);

		sendUComMsg(rsp, "S2F42");
		
		String strLog = "[STBC->MCS] S2F42 : HCACK=" + hcAck;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 42, strLog);
		return true;
	}

	/**
	 * Receive IDREAD Command
	 * 
	 * @param msg
	 * @return
	 */
	private boolean receiveIDREADcommand(UComMsg msg) {
		// --------------------------------------------------------------------------
		// AS-WAS : D사 UNKNOWNCarrier 발생시, 리더기 부착된 INVENTORY포트 (일부)로 보내 IDREAD 
		// 		생기연 OHT에 리더기 부착, SCAN명령으로 OHT RF-Reading, 
		// AS-IS : S1B신규라인부터 RFC IDREAD 기능으로 사용
		// --------------------------------------------------------------------------
		int hcAck = 4;
		int itemCnt;
		int subItemCnt;
		String str;
		String strLog;
		String carrierId;
		String portId;
	
		itemCnt = msg.getListItem(); // L, 2 [CARRIERID,CARRIERLOC]
		if (itemCnt != 2) {
			strLog = "[MCS->STBC] S2F41:IDREAD / Invalid CPNAME (L, 2) itemCnt=" + itemCnt;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			hcAck = 3; 
			return sendS2F42(msg, hcAck);
		} else {
			subItemCnt = msg.getListItem(); // L, 2
			str = msg.getAsciiItem(); //  CPNAME : "CARRIERID"
			if (CARRIERID.equals(str)) {
				carrierId = msg.getAsciiItem();
				writeSEMLog("          [CARRIERID]:" + carrierId);
			} else {				
				strLog = "[MCS->STBC] S2F41:IDREAD / Invalid CPNAME (CARRIERID)";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				hcAck = 3; 
				return sendS2F42(msg, hcAck);
			}
			subItemCnt = msg.getListItem(); // L, 2
			str = msg.getAsciiItem(); // CPNAME : "PORTID"
			if (PORTID.equals(str)) {
				portId = msg.getAsciiItem();
				writeSEMLog("          [PORTID]:" + portId);
			} else {			
				strLog = "[MCS->STBC] S2F41:IDREAD / Invalid CPNAME (PORTID)";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				hcAck = 3; 
				return sendS2F42(msg, hcAck);
			}					
		}
		
		// carrierLoc available check : IDREAD 가 가능한 포트인지 확인하는 부분 // 보완요
		if (isCarrierLocAvailable(portId) == false) {			
			strLog = "[MCS->STBC] S2F41:IDREAD / Invalid CPNAME, CarrierLoc is not available";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			hcAck = 3; 
			return sendS2F42(msg, hcAck);
		}
		
		writeSEMLog("[MCS->STBC] S2F41:IDREAD Normally Received / PortId:" + portId + " ,CarrierId:" + carrierId);
		// 2012.03.09 by KYK : 수신에 대한 응답 먼저
		sendS2F42(msg, hcAck); // ■ Normal Response ■	
		
		// RFC Reader 설치 사용
		// 2012.04.05 by KYK
//		if (ocsInfoManager.isRFCUsed() && isIdReaderAvailable(portId)) {
		if (isRFCUsed() && isIdReaderAvailable(portId)) {
			rfcManager.requestIdRead(portId, true);
			writeSEMLog("[STBC->RFC] REQ_IDREAD On Port:" + portId + " ,CarrierId:" + carrierId);
		} else {
			// RFC Reader NotInstalled or Disable 일때 , DB 값으로 보고함?
			reportIDReadWithDBData(portId);
		}
		return true;
	}

	/**
	 * Report IDRead With DB Data
	 * 
	 * @param portId
	 */
	private void reportIDReadWithDBData(String portId) {
		STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(portId);
		ReportItems rItems = new ReportItems();
		rItems.setCarrierLoc(portId);
		rItems.setCarrierId(carrierLoc.getCarrierId());
		rItems.setFoupId(carrierLoc.getFoupId());
		rItems.setIdReadStatus(SUCCESS);
		sendS6F11(CarrierIDRead, rItems);		
		
		String strLog = "	[Reply of IDREAD] Report DBData, not by RFC / PortId:" + portId + " carrierId:" + carrierLoc.getCarrierId() + " foupId:" + carrierLoc.getFoupId();
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 6, 11, strLog);
	}

	/**
	 * Receive INSTALL Command
	 * 
	 * @param msg
	 * @return
	 */
	private boolean receiveINSTALLcommand(UComMsg msg) {
		int hcAck = 4;
		int subItemCnt;
		String str;
		String strLog;
		String carrierId;
		String carrierLocId;
	
		int itemCnt = msg.getListItem(); // L, 2 [CARRIERID,CARRIERLOC]
		if (itemCnt != 2) {
			strLog = "[MCS->STBC] S2F41:INSTALL / Invalid CPNAME (L, 2) itemCnt=" + itemCnt;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			hcAck = 3; 
			return sendS2F42(msg, hcAck);
		} else {
			subItemCnt = msg.getListItem(); // L, 2
			str = msg.getAsciiItem(); //  CPNAME : "CARRIERID"
			if (CARRIERID.equals(str)) {
				carrierId = msg.getAsciiItem();
				writeSEMLog("          [CARRIERID]:" + carrierId);
			} else {				
				strLog = "[MCS->STBC] S2F41:INSTALL / Invalid CPNAME [CARRIERID]";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				hcAck = 3; 
				return sendS2F42(msg, hcAck);
			}

			subItemCnt = msg.getListItem(); // L,2
			str = msg.getAsciiItem(); // CPNAME : "CARRIERLOC"
			if (CARRIERLOC.equals(str)) {
				carrierLocId = msg.getAsciiItem();
				writeSEMLog("          [CARRIERLOC]:" + carrierLocId);
			} else {			
				strLog = "[MCS->STBC] S2F41:INSTALL / Invalid CPNAME [CARRIERLOC]";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				hcAck = 3; 
				return sendS2F42(msg, hcAck);
			}					
		}
		
		// carrierLoc available check
		if (isCarrierLocAvailable(carrierLocId) == false) {			
			strLog = "[MCS->STBC] S2F41:INSTALL / Invalid CPNAME, CarrierLoc is not available";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			hcAck = 3; 
			return sendS2F42(msg, hcAck);
		}
		
		// 2012.03.09 by KYK
		STBCarrierLoc carrierLocOnPort = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
		if (carrierLocOnPort == null) {
			writeSEMLog(" # carrierLocOnPort is null, CarrierLoc:" + carrierLocId);
			return sendS2F42(msg, hcAck);
		} 		
		// Same ID as the INSTALL command has already been registered.
		// sendS2F42 를 먼저 보내기 위해 아래서 하던거 위에서 먼저 체크
		if (carrierId.equals(carrierLocOnPort.getCarrierId())) {
			hcAck = 5;
			strLog = "[MCS->STBC] S2F41:INSTALL / Command Already Request";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			return sendS2F42(msg, hcAck);
		}					
		
		writeSEMLog("[MCS->STBC] S2F41:INSTALL Normally Received / PortId:" + carrierLocId + ", CarrierId:" + carrierId + ", FoupId:" + "");
		// 2012.03.09 by KYK : 수신에 대한 응답 먼저
		sendS2F42(msg, hcAck); // ■ Normal Response ■	
		
		// RFC Reader 설치 사용 : if IdReader Available, request 'VERIFY' to RFC
		// 2012.04.05 by KYK
//		if (ocsInfoManager.isRFCUsed() && isIdReaderAvailable(carrierLocId)) {
		if (isRFCUsed() && isIdReaderAvailable(carrierLocId)) {
			//rfcManager.requestIdRead(carrierLocId, true);	
			rfcManager.requestVerify(carrierLocId, getVerifyTimeout(), true);	
			writeSEMLog("[STBC->RFC] REQ_VERIFY (S2F41 INSTALL) On Port:" + carrierLocId + ", CarrierId:" + carrierId + ", FoupId:" + "");
		}
				
		// ABNORMAL CASE 처리 --------------------------------
		// 1. carrier duplicate
		// 2. mismatch
		// 3. carrier duplicate & mismatch
		// --------------------------------------------------

		// 작업처리 방식 -----------------------------------------
		// AS-WAS : (Carrier)INSTALL, REMOVE 시 RCMD 로 일단 등록 (insertRCMDToDB) 후
		// STBCMain 에서 RCMD 처리 /carrierLoc의 컬럼값을 업데이트함 (S6F11 보고도 이때진행)
		// AS-IS : 여기에서 바로 정리
		// ----------------------------------------------------

		// check abnormal		
		STBCarrierLoc dupCarrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(carrierId);
		ReportItems rItems;

		// Q1 : 동일한 carrierId 가 DB 상 존재하는가?
		if (dupCarrierLoc == null) { // 존재안함, 정상
			// Q2 : 현재 포트에 어떤 carrier 가 점유하고 있는가?
			if (isCarrierOccupiedAt(carrierLocId)) { // [MISMATCH]
				strLog = "[MCS->STBC] S2F41:INSTALL / CARRIER MISMATCH";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				
//				stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, "", REMOVE);
				stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, "", "", REMOVE, false); // true ,false 확인할것
				rItems = makeSTBReportItems(carrierLocId, carrierLocOnPort.getCarrierId(), "");
				sendS6F11(CarrierRemoved, rItems);

//				stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, INSTALL);
				stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, "", INSTALL, true);
				rItems = makeSTBReportItems(carrierLocId, carrierId, "");
				sendS6F11(CarrierInstalled, rItems);
				
			} else { // OK
//				stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, INSTALL);
				stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, "", INSTALL, true);
				rItems = makeSTBReportItems(carrierLocId, carrierId, "");
				sendS6F11(CarrierInstalled, rItems);
			}
		} else { // Duplicate Carrier 존재함
			// Q1 : 동일한 carrierId 가 DB 상 존재하는가? 
			String dupCarrierLocId = dupCarrierLoc.getCarrierLocId();
			// Q3 : 동일한 carrier 가 위치한 포트가 바로 여기인가?
			if (carrierLocId.equals(dupCarrierLocId)) { // YES,[ALREADY REQUESTED]
				// 2012.03.09 by KYK : 상단에서 처리함
			} else { // 동일 carrier 는 다른 곳에 위치				
				String unknownCarrierId = makeUnknownCarrierName(carrierId);
				// Q2 : 현재 포트에 어떤 carrier 가 점유하고 있는가? YES
				if (isCarrierOccupiedAt(carrierLocId)) { // [DUPLICATE & MISMATCH]
					strLog = "[MCS->STBC] S2F41:INSTALL / CARRIER DUPLICATE & MISMATCH";
					writeSEMLog(strLog);	
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					
					stbCarrierLocManager.updateCarrierLocStatus(dupCarrierLocId, carrierId, "", REMOVE, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, REMOVE);
					rItems = makeSTBReportItems(dupCarrierLocId, carrierId, "");
					sendS6F11(CarrierRemoved, rItems);							
					
					stbCarrierLocManager.updateCarrierLocStatus(dupCarrierLocId, unknownCarrierId, "", INSTALL, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, unknownCarrierId, INSTALL);
					rItems = makeSTBReportItems(dupCarrierLocId, unknownCarrierId, "");
					sendS6F11(CarrierInstalled, rItems);

					stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierLocOnPort.getCarrierId(), "", REMOVE, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierLocOnPort.getCarrierId(), REMOVE);
					rItems = makeSTBReportItems(carrierLocId, carrierLocOnPort.getCarrierId(), "");
					sendS6F11(CarrierRemoved, rItems);
					
					stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, "", INSTALL, true);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, INSTALL);
					rItems = makeSTBReportItems(carrierLocId, carrierId, "");
					sendS6F11(CarrierInstalled, rItems);
					
				} else { // DUPLICATE
					// Q2 : 현재 포트에 어떤 carrier 가 점유하고 있는가? NO
					strLog = "[MCS->STBC] S2F41:INSTALL / CARRIER DUPLICATE";
					writeSEMLog(strLog);	
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					
					stbCarrierLocManager.updateCarrierLocStatus(dupCarrierLocId, carrierId, "", REMOVE, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, REMOVE);
					rItems = makeSTBReportItems(dupCarrierLocId, carrierId, "");
					sendS6F11(CarrierRemoved, rItems);							
					
					stbCarrierLocManager.updateCarrierLocStatus(dupCarrierLocId, unknownCarrierId, "", INSTALL, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, unknownCarrierId, INSTALL);
					rItems = makeSTBReportItems(dupCarrierLocId, unknownCarrierId, "");
					sendS6F11(CarrierInstalled, rItems);							

					stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, "", INSTALL, true);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, INSTALL);
					rItems = makeSTBReportItems(carrierLocId, carrierId, "");
					sendS6F11(CarrierInstalled, rItems);

				}
			}
		}		
		return true;
	}	
	
	/**
	 * Receive INSTALL2 Command
	 * 
	 * @param msg
	 * @return
	 */
	private boolean receiveINSTALL2command(UComMsg msg) {
		int hcAck = 4;
		int subItemCnt;
		String str;
		String strLog;
		String carrierId;
		String carrierLocId;
		String foupId;
	
		int itemCnt = msg.getListItem(); // L, 3 [CARRIERID,CARRIERLOC,FOUPID]
		if (itemCnt != 3) {
			strLog = "[MCS->STBC] S2F41:INSTALL2 / Invalid CPNAME (L, 3) itemCnt=" + itemCnt;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			hcAck = 3; 
			return sendS2F42(msg, hcAck);
		} else {
			subItemCnt = msg.getListItem(); // L, 2
			str = msg.getAsciiItem(); //  CPNAME : "CARRIERID"
			if (CARRIERID.equals(str)) {
				carrierId = msg.getAsciiItem();
				writeSEMLog("          [CARRIERID]:" + carrierId);
			} else {				
				strLog = "[MCS->STBC] S2F41:INSTALL2 / Invalid CPNAME [CARRIERID]";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				hcAck = 3; 
				return sendS2F42(msg, hcAck);
			}

			subItemCnt = msg.getListItem(); // L,2
			str = msg.getAsciiItem(); // CPNAME : "CARRIERLOC"
			if (CARRIERLOC.equals(str)) {
				carrierLocId = msg.getAsciiItem();
				writeSEMLog("          [CARRIERLOC]:" + carrierLocId);
			} else {			
				strLog = "[MCS->STBC] S2F41:INSTALL2 / Invalid CPNAME [CARRIERLOC]";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				hcAck = 3; 
				return sendS2F42(msg, hcAck);
			}					

			subItemCnt = msg.getListItem(); // L,2
			str = msg.getAsciiItem(); // CPNAME : "FOUPID"
			if (FOUPID.equals(str)) {
				foupId = msg.getAsciiItem();
				writeSEMLog("          [FOUPID]:" + foupId);
			} else {			
				strLog = "[MCS->STBC] S2F41:INSTALL2 / Invalid CPNAME [FOUPID]";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				hcAck = 3; 
				return sendS2F42(msg, hcAck);
			}					
		}
		
		// carrierLoc available check
		if (isCarrierLocAvailable(carrierLocId) == false) {			
			strLog = "[MCS->STBC] S2F41:INSTALL2 / Invalid CPNAME, CarrierLoc is not available";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			hcAck = 3; 
			return sendS2F42(msg, hcAck);
		}
		
		// 2012.03.09 by KYK
		STBCarrierLoc carrierLocOnPort = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
		if (carrierLocOnPort == null) {
			writeSEMLog(" # carrierLocOnPort is null, CarrierLoc:" + carrierLocId);
			return sendS2F42(msg, hcAck);
		} 		
		// Same ID as the INSTALL2 command has already been registered.
		// sendS2F42 를 먼저 보내기 위해 아래서 하던거 위에서 먼저 체크
		if (carrierId.equals(carrierLocOnPort.getCarrierId())) {
			hcAck = 5;
			strLog = "[MCS->STBC] S2F41:INSTALL2 / Command Already Request";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			return sendS2F42(msg, hcAck);
		}					
		
		writeSEMLog("[MCS->STBC] S2F41:INSTALL2 Normally Received / PortId:" + carrierLocId + ", CarrierId:" + carrierId + ", FoupId:" + foupId);
		// 2012.03.09 by KYK : 수신에 대한 응답 먼저
		sendS2F42(msg, hcAck); // ■ Normal Response ■	
		
		// RFC Reader 설치 사용 : if IdReader Available, request 'VERIFY' to RFC
		// 2012.04.05 by KYK
//		if (ocsInfoManager.isRFCUsed() && isIdReaderAvailable(carrierLocId)) {
		if (isRFCUsed() && isIdReaderAvailable(carrierLocId)) {
			//rfcManager.requestIdRead(carrierLocId, true);	
			rfcManager.requestVerify(carrierLocId, getVerifyTimeout(), true);	
			writeSEMLog("[STBC->RFC] REQ_VERIFY (S2F41 INSTALL2) On Port:" + carrierLocId + ", CarrierId:" + carrierId + ", FoupId:" + foupId);
		}
				
		// ABNORMAL CASE 처리 --------------------------------
		// 1. carrier duplicate
		// 2. mismatch
		// 3. carrier duplicate & mismatch
		// --------------------------------------------------

		// 작업처리 방식 -----------------------------------------
		// AS-WAS : (Carrier)INSTALL2, REMOVE 시 RCMD 로 일단 등록 (insertRCMDToDB) 후
		// STBCMain 에서 RCMD 처리 /carrierLoc의 컬럼값을 업데이트함 (S6F11 보고도 이때진행)
		// AS-IS : 여기에서 바로 정리
		// ----------------------------------------------------

		// check abnormal		
		STBCarrierLoc dupCarrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(carrierId);
		ReportItems rItems;

		// Q1 : 동일한 carrierId 가 DB 상 존재하는가?
		if (dupCarrierLoc == null) { // 존재안함, 정상
			// Q2 : 현재 포트에 어떤 carrier 가 점유하고 있는가?
			if (isCarrierOccupiedAt(carrierLocId)) { // [MISMATCH]
				strLog = "[MCS->STBC] S2F41:INSTALL2 / CARRIER MISMATCH";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				
//				stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, "", REMOVE);
				stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, "", "", REMOVE, false); // true ,false 확인할것
				rItems = makeSTBReportItems(carrierLocId, carrierLocOnPort.getCarrierId(), "");
				sendS6F11(CarrierRemoved, rItems);

//				stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, INSTALL2);
				stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, foupId, INSTALL, true);
				rItems = makeSTBReportItems(carrierLocId, carrierId, foupId);
				sendS6F11(CarrierInstalled, rItems);
				
			} else { // OK
//				stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, INSTALL2);
				stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, foupId, INSTALL, true);
				rItems = makeSTBReportItems(carrierLocId, carrierId, foupId);
				sendS6F11(CarrierInstalled, rItems);
			}
		} else { // Duplicate Carrier 존재함
			// Q1 : 동일한 carrierId 가 DB 상 존재하는가? 
			String dupCarrierLocId = dupCarrierLoc.getCarrierLocId();
			// Q3 : 동일한 carrier 가 위치한 포트가 바로 여기인가?
			if (carrierLocId.equals(dupCarrierLocId)) { // YES,[ALREADY REQUESTED]
				// 2012.03.09 by KYK : 상단에서 처리함
			} else { // 동일 carrier 는 다른 곳에 위치				
				String unknownCarrierId = makeUnknownCarrierName(carrierId);
				String unknownFoupId = makeUnknownFoupName(foupId);
				// Q2 : 현재 포트에 어떤 carrier 가 점유하고 있는가? YES
				if (isCarrierOccupiedAt(carrierLocId)) { // [DUPLICATE & MISMATCH]
					strLog = "[MCS->STBC] S2F41:INSTALL2 / CARRIER DUPLICATE & MISMATCH";
					writeSEMLog(strLog);	
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					
					stbCarrierLocManager.updateCarrierLocStatus(dupCarrierLocId, carrierId, "", REMOVE, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, REMOVE);
					rItems = makeSTBReportItems(dupCarrierLocId, carrierId, "");
					sendS6F11(CarrierRemoved, rItems);							
					
					stbCarrierLocManager.updateCarrierLocStatus(dupCarrierLocId, unknownCarrierId, unknownFoupId, INSTALL, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, unknownCarrierId, INSTALL2);
					rItems = makeSTBReportItems(dupCarrierLocId, unknownCarrierId, unknownFoupId);
					sendS6F11(CarrierInstalled, rItems);

					stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierLocOnPort.getCarrierId(), "", REMOVE, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierLocOnPort.getCarrierId(), REMOVE);
					rItems = makeSTBReportItems(carrierLocId, carrierLocOnPort.getCarrierId(), "");
					sendS6F11(CarrierRemoved, rItems);
					
					stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, foupId, INSTALL, true);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, INSTALL2);
					rItems = makeSTBReportItems(carrierLocId, carrierId, foupId);
					sendS6F11(CarrierInstalled, rItems);
					
				} else { // DUPLICATE
					// Q2 : 현재 포트에 어떤 carrier 가 점유하고 있는가? NO
					strLog = "[MCS->STBC] S2F41:INSTALL2 / CARRIER DUPLICATE";
					writeSEMLog(strLog);	
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					
					stbCarrierLocManager.updateCarrierLocStatus(dupCarrierLocId, carrierId, "", REMOVE, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, REMOVE);
					rItems = makeSTBReportItems(dupCarrierLocId, carrierId, "");
					sendS6F11(CarrierRemoved, rItems);							
					
					stbCarrierLocManager.updateCarrierLocStatus(dupCarrierLocId, unknownCarrierId, unknownFoupId, INSTALL, false);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, unknownCarrierId, INSTALL2);
					rItems = makeSTBReportItems(dupCarrierLocId, unknownCarrierId, unknownFoupId);
					sendS6F11(CarrierInstalled, rItems);							

					stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, foupId, INSTALL, true);
//					stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, INSTALL2);
					rItems = makeSTBReportItems(carrierLocId, carrierId, foupId);
					sendS6F11(CarrierInstalled, rItems);

				}
			}
		}		
		return true;
	}	

	public boolean isIdReaderAvailable(String carrierLocId) {
//		STBCarrierLoc stbCarrierLoc = (STBCarrierLoc) stbCarrierLocManager.getData().get(carrierLocId);
		if (stbCarrierLocManager.getData() != null) {
			STBCarrierLoc stbCarrierLoc = (STBCarrierLoc) stbCarrierLocManager.getData().get(carrierLocId);
			if (stbCarrierLoc != null) {
				// STBCarrierLoc 에 IDREADER='INSTALLED' (설치됨)
				if (INSTALLED.equalsIgnoreCase(stbCarrierLoc.getIdReader())) {
					String rfcId = stbCarrierLoc.getRfcId();
					if (stbRfcDataManager.getData() != null) {
						STBRfcData stbRfcData = (STBRfcData) stbRfcDataManager.getData().get(rfcId);			
						if (stbRfcData != null) {
							// 2012.02.14 by KYK : Port isEnabled() 조건추가 , 'FALSE'일때 RFCRead 안하도록 함(반도체요청)						
							if (stbRfcData.isEnabled() && stbCarrierLoc.isEnabled()) {
								return true;
							}
						} else {
							writeSEMLog("Unregistered RfcData:" + rfcId);
						}
					}
				}
			}
		}
		return false;
	}
	
	private String getRfcIdOfPort(String portId) {
		if (stbCarrierLocManager.getData() != null) {
			STBCarrierLoc stbCarrierLoc = (STBCarrierLoc) stbCarrierLocManager.getData().get(portId);
			if (stbCarrierLoc != null) {
				return stbCarrierLoc.getRfcId();
			}
		}
		return null;
	}

	private long getErrorStbSeqNo(){
		if (errorStbSeqNo == 10000) {
			errorStbSeqNo = 1;
		}
		return errorStbSeqNo++;
	}
	
	private long getErrorStbFoupSeqNo(){
		if (errorStbFoupSeqNo == 10000) {
			errorStbFoupSeqNo = 1;
		}
		return errorStbFoupSeqNo++;
	}

	// UNKNOWNDUP-CARRIER01-YYYYMMDDHHMMSS : 기존과 이름다름 확인 필요 ?
	/**
	 * Make Unknown Carrier Name
	 */
	public String makeUnknownCarrierName(String carrierId) {		
		Date currTime = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(currTime);
		StringBuilder sb = new StringBuilder();
		sb.append("UNKNOWNDUP-").append(carrierId).append("-").append(date);		
		return sb.toString();
	}
	
	// UNKNOWNDUP-FOUP01-YYYYMMDDHHMMSS : 기존과 이름다름 확인 필요 ?
	/**
	 * Make Unknown Foup Name
	 */
	public String makeUnknownFoupName(String foupId) {		
		Date currTime = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(currTime);
		StringBuilder sb = new StringBuilder();
		sb.append("UNKNOWNDUP-").append(foupId).append("-").append(date);		
		return sb.toString();
	}
	
	// RFC IDREAD 후, Duplicate 발생시 carrierId : 'ErrorSTBDUPxxx' 등록
	/**
	 * Make Error STB DupCarrier Name
	 */
	public String makeErrorSTBDUPCarrierName() {		
		Date currTime = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(currTime);
		StringBuilder sb = new StringBuilder();
		sb.append("ErrorSTBDUP").append(date).append("_").append(getErrorStbSeqNo());		
		return sb.toString();
	}

	// RFC IDREAD 후, Duplicate 발생시 foupId : 'ErrorSTBDUP-Foupxxx' 등록
	/**
	 * Make Error STB DupFoup Name
	 */
	public String makeErrorSTBDUPFoupName() {		
		Date currTime = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(currTime);
		StringBuilder sb = new StringBuilder();
		sb.append("ErrorSTBDUP-Foup").append(date).append("_").append(getErrorStbFoupSeqNo());		
		return sb.toString();
	}

	// RFC IDREAD 후, Read Error (Read Fail) : 'UNKNOWNxxx' 등록
	/**
	 * Make RFC Unknown Carrier Name
	 */
	public String makeRFCUnknownCarrierName() {		
		Date currTime = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(currTime);
		StringBuilder sb = new StringBuilder();
		// 2012.03.08 by KYK : UNKNOWN CarrierID 중간에 공백제거
		// 원인 : mdln 뒤에 공백이 붙어서 내려옴 (S1F1) 'UNKNOWNSTBC  20120306160736_112/S1BC06R_127003'
//		sb.append("UNKNOWN").append(mdln).append(date).append("_").append(getErrorStbSeqNo());		
		sb.append("UNKNOWN").append("STBC").append(date).append("_").append(getErrorStbSeqNo());		
		return sb.toString();
	}
	
	/**
	 * Make RFC Unknown Foup Name
	 */
	public String makeRFCUnknownFoupName() {		
		Date currTime = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(currTime);
		StringBuilder sb = new StringBuilder();	
		sb.append("UNKNOWN").append("STBC-Foup").append(date).append("_").append(getErrorStbSeqNo());		
		return sb.toString();
	}

	// RFC IDREAD 명령전송에 대한 응답이 없을경우 (Timeout) : 'ErrorSTBxxx' 등록
	/**
	 * Make Error STB Carrier Name
	 */
	public String makeErrorSTBCarrierName() {		
		Date currTime = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(currTime);
		StringBuilder sb = new StringBuilder();
		sb.append("ErrorSTB").append(date).append("_").append(getErrorStbSeqNo());		
		return sb.toString();
	}
	
	/**
	 * Make Error STB Foup Name
	 */
	public String makeErrorSTBFoupName() {		
		Date currTime = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String date = dateFormat.format(currTime);
		StringBuilder sb = new StringBuilder();
		sb.append("ErrorSTB-Foup").append(date).append("_").append(getErrorStbFoupSeqNo());		
		return sb.toString();
	}
	
	public boolean isCarrierOccupiedAt(String carrierLocId) {
		STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierLocId(carrierLocId);
		if (carrierLoc != null) {
			if (carrierLoc.getCarrierId() != null || carrierLoc.getCarrierId().length() > 0) {
				return true;
			}
		}
		return false;
	}

	private boolean isCarrierLocAvailable(String carrierLocId) {
		if (stbCarrierLocManager.getData() != null) {
			if (stbCarrierLocManager.getData().containsKey(carrierLocId)) {
				return true;
			}
		}
		return false;
	}
	
	// 2012.04.05 by KYK
	private boolean isRFCUsed() {
		return stbcManager.isRFCUsed();
	}

	/**
	 * Receive REMOVE Command
	 * 
	 * @param msg
	 * @return
	 */
	private boolean receiveREMOVEcommand(UComMsg msg) {		
		int hcAck = 4;
		int itemCnt;
		int subItemCnt;
		String str;
		String strLog;
		String carrierId;
		String carrierLocId;
	
		itemCnt = msg.getListItem(); // L,1
		subItemCnt = msg.getListItem(); // L, 2
		str = msg.getAsciiItem(); //  CPNAME : "CARRIERID"
		if (CARRIERID.equals(str)) {
			carrierId = msg.getAsciiItem();
			writeSEMLog("          [CARRIERID]:" + carrierId);
		} else {
			strLog = "[MCS->STBC] S2F41:REMOVE / Invalid CPNAME [CARRIERID]";
			writeSEMLog(strLog);			
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			hcAck = 3; 
			return sendS2F42(msg, hcAck);
		}		
		// carrierId Not Exist
		STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(carrierId);
		if (carrierLoc == null) {			
			strLog = "[MCS->STBC] S2F41:REMOVE / Invalid CPNAME / Carrier Not Exist";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			hcAck = 3; 
			return sendS2F42(msg, hcAck);			
		} else {
			carrierLocId = carrierLoc.getCarrierLocId();
		}		
		
		// 2012.03.09 by KYK : 수신에 대한 응답 먼저
		sendS2F42(msg, hcAck); // ■ Normal Response ■	
		
		// RFC Reader 설치 사용
		// 2012.04.05 by KYK
//		if (ocsInfoManager.isRFCUsed() && isIdReaderAvailable(carrierLocId)) {
		if (isRFCUsed() && isIdReaderAvailable(carrierLocId)) {
			//rfcManager.requestIdRead(carrierLocId, true);
			// 2015.02.10 by KYK : Remove 시 hasCarrier=true 이면 실질적으로 Remove 불가함 (Verify:NG일꺼라서)
//			rfcManager.requestVerify(carrierLocId, getVerifyTimeout(), true);
			rfcManager.requestVerify(carrierLocId, getVerifyTimeout(), false);
			writeSEMLog("[STBC->RFC] REQ_VERIFY (S2F41 REMOVE cmd) On Port:" + carrierLocId + " ,CarrierId:" + carrierId);
		}

		stbCarrierLocManager.updateCarrierLocStatus(carrierLocId, carrierId, "", REMOVE, true);		
//		stbCarrierLocManager.addSTBCarrierLocToUpdateStatusList(carrierLocId, carrierId, "REMOVE");
		ReportItems rItems = makeSTBReportItems(carrierLocId, carrierId, "");
		sendS6F11(CarrierRemoved, rItems);
		
		return true;
	}	

	/**
	 * S2F41 RESUME 명령 수신에 대한 처리
	 * @param msg
	 * @return
	 */
	private boolean receiveRESUMECommand(UComMsg msg) {
		// 2012.03.09 by KYK : 수신에 대한 응답 먼저
		int hcAck = 4;
		sendS2F42(msg, hcAck);

		boolean result = true;
		// 이것이 필요한지 논의 필요
		//result = updateOcsInfoToDB("OCSCONTROL", RESUME);
		if (TSC_AUTO.equals(tscStatus) == false) {
			tscStatus = TSC_AUTO;
			setSEMStatusToDB();
			sendS6F11(STBAutoCompleted, "", "", 0);			
		}
		return result;		
	}

	/**
	 * S2F41 PAUSE 명령 수신에 대한 처리
	 * @param msg
	 * @return
	 */
	private boolean receivePAUSECommand(UComMsg msg) {
		// 2012.03.09 by KYK : 수신에 대한 응답 먼저
		int hcAck = 4;
		sendS2F42(msg, hcAck);
		
		boolean result = true;
		// STBC Reconcile 시, OCS PAUSE 상태여야하는가?
		// 장점 : 재하틀어짐 방지
		// 단점 : 불필요한 운영제약 (STBC 없이도 라인운영큰 문제없는데 이문제 때문에 운영중단발생 / STBC Reconcile 시간 긴게 문제)
		//result = updateOcsInfoToDB("OCSCONTROL", PAUSE);
		if (TSC_PAUSED.equals(tscStatus) == false) {
			tscStatus = TSC_PAUSING;
			setSEMStatusToDB();
			sendS6F11(STBPauseInitiated, "", "", 0);				
		}
		tscStatus = TSC_PAUSED;
		setSEMStatusToDB();
		sendS6F11(STBPauseCompleted, "", "", 0);							
		return result;
	}

	/**	  
	 * HCACK (REMOTE COMMAND ERROR)
	 * COMMAND_DOES_NOT_EXIST = 1;
	 * NOT_EXECUTABLE = 2;
	 * PARAMETER_ERROR = 3;
	 * SUCCESS = 4;
	 * ALREADY_REQUESTED = 5;
	 */
	@Override
	public void receiveS2F49(UComMsg msg) {		
		// ------------------------------------------------------------------------
		// ※ 테스트 항목
		//  - normal test 01 : mcs(one-msg 전송)-stbc(restart, no-data)
		//    UPDATE OCSINFO SET VALUE='MCS' WHERE NAME='STBDATA_RECOVERY_OPTION';
		//  - normal test 02 : mcs(restart)-stbc(carrierlist 전송)
		//    UPDATE OCSINFO SET VALUE='OCS' WHERE NAME='STBDATA_RECOVERY_OPTION';
		//  - abnormal test 01 : mcs(stbc가 없는 정보 전송)-stbc
		//    UPDATE OCSINFO SET VALUE='MCS' WHERE NAME='STBDATA_RECOVERY_OPTION';
		//  - abnormal test 02 : mcs(one-msg 전송)-stbc(install중 DB error 발생)
		//    UPDATE OCSINFO SET VALUE='MCS' WHERE NAME='STBDATA_RECOVERY_OPTION';
		//--------------------------------------------------------------------------		
		int hcAck = 4;		
		int itemCnt = msg.getListItem(); // L,4
		int dataId = msg.getU2Item(); // DATAID
		String str = msg.getAsciiItem(); // OBJSPEC
		String rCmd = msg.getAsciiItem(); // RCMD		

		String strLog = "[MCS->STBC] S2F49 : RCMD = " + rCmd;
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 41, strLog);
		
		if (controlStatus.equals(REMOTE_ONLINE) == false) {
			hcAck = 2; // HCACK 2 : currently not able to execute
			strLog = "[MCS->STBC] S2F49 : CONTROLStatus is not Online/HCACK=" + hcAck;
			writeSEMLog(strLog);	
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			sendS2F50(msg, hcAck);
		} else {
			if (INSTALLLIST.equals(rCmd)) {
				receiveINSTALLLISTcommand(msg);
			} else if (INSTALLLIST2.equals(rCmd)) {
				receiveINSTALLLIST2command(msg);
			} else if (IDREADLIST.equals(rCmd)) {
				receiveIDREADLISTcommand(msg);
			} else {
				hcAck = 3; // at least one parameter isn't valid
				strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (Carrier Qty : " + itemCnt + ")";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F50(msg, hcAck);
			}			
		}			
	}

	/**
	 * Receive IDREADLIST Command
	 * 
	 * @param msg
	 * @return
	 */
	private boolean receiveIDREADLISTcommand(UComMsg msg) {		
		// RCMD : IDREADLIST --------------------------------
		// <L [4]
		//   <U2 0 >  /* DATAID */
		//   <A “>  /* OBJSPEC */
		//   <A ‘IDREADLIST’>  /* RCMD */
		//   <L [1]
		//     <L [2]
		//       <A ‘PORTLIST’>  /* CPNAME */
		//       <L [N]			// 0 이면 all ports
		//			<A 'PORT-00001'>
		//			...
		// > > > >
		// --------------------------------------------------		
		int hcAck = 4;
		int itemCnt;
		int dataCnt;
		String str;
		String strLog;
		String portId;
		String rfcId; // 2012.03.08 by KYK : [IDREADLIST]
		ArrayList<String> idReadPortList;
		HashMap<String, ArrayList> readAllRequestMap = new HashMap<String, ArrayList>();

		itemCnt = msg.getListItem(); // L, 1 
		if (itemCnt != 1) {
			hcAck = 3; // at least one parameter isn't valid
			strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 1> : " + itemCnt;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			return sendS2F50(msg, hcAck);
		} else {
			// Get 'PORTIDLIST' data from S2F49 
			itemCnt = msg.getListItem(); // L, 2			
			if (itemCnt != 2) {
				hcAck = 3; // at least one parameter isn't valid
				strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 2> : " + itemCnt;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				return sendS2F50(msg, hcAck);
			} else {
				str = msg.getAsciiItem(); // CPNAME1 : "PORTIDLIST"
				dataCnt = msg.getListItem(); // L, n 
				if (PORTIDLIST.equals(str) == false) {
					hcAck = 3; // at least one parameter isn't valid
					strLog = "[MCS->STBC] S2F49 : Invalid CPNAME [PORTIDLIST]";
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					return sendS2F50(msg, hcAck);
				} else {
					// 2012.04.05 by KYK
//					if (ocsInfoManager.isRFCUsed() == false) {
					if (isRFCUsed() == false) {
						writeSEMLog("RFC-SYSTEM is Not Available. [RFCUSAGE='false']");
						sendS2F50(msg, hcAck);
						sendS6F11(CarrierIDReadMulti, null);
						return false;
					}
					// ■ Normal Process ■
					// 2012.03.15 by KYK [IDREADLIST] 수신시 RFC READALL 명령으로 처리 (이전 IDREAD 로 처리함)
					if (0 == dataCnt) { // L[0] 일 경우, IDREAD ALL (전체 포트에 대해 READALL)						
						writeSEMLog("	[RCV S2F49] IDREADLIST : Request READALL (All STBPort)");
						setReadAllRequestMap(readAllRequestMap);
					} else {
						StringBuilder log = new StringBuilder();
						StringBuilder abnormalLog = new StringBuilder();						
						for (int i = 0; i < dataCnt; i++) {
							// Get 'PORTID' 
							portId = msg.getAsciiItem(); // CPNAME : 'PORTID'
							
							log.append(portId).append("/");
							if (isIdReaderAvailable(portId)) {
								rfcId = getRfcIdOfPort(portId);
								idReadPortList = readAllRequestMap.get(rfcId);
								// requested Ports 를 rfcId 기준으로 정리하여 map 에 넣음
								if (idReadPortList == null) {
									idReadPortList = new ArrayList<String>();		
									readAllRequestMap.put(rfcId, idReadPortList);
								}
								idReadPortList.add(portId);								
							} else {
								// 비사용 Port 에 대해서는 처리안함
								abnormalLog.append(portId).append("/");
								writeSEMLog("Ignore request of IdRead (Port or Rfc Disabled), PortId:" + portId);
							}
						}
						writeSEMLog("	[RCV S2F49] PortIdList: " + log.toString());
						if (abnormalLog.toString() != null && abnormalLog.toString().length() > 0) {
							writeSEMLog("   Ignored Ports (Port or Rfc Disabled), PortId:" + abnormalLog.toString());							
						}						
					}
					
					// RFC IDREAD (READALL) 을 요청					
					if (readAllRequestMap.isEmpty()) {
						writeSEMLog("[STBC->RFC] NOT REQ_IDREADALL , but There is no Available RFC");
						sendS6F11(CarrierIDReadMulti, null);
					} else {
						readAllRequestList.add(readAllRequestMap);
						for (String rfcId2 : readAllRequestMap.keySet()) {
							// 2015.06.08 by KBS : IDREADLIST 후 응답없는 port 처리 개선
							if (dataCnt == 0) {
								idReadPortList = null;
							} else {
								idReadPortList = readAllRequestMap.get(rfcId2);
							}
							rfcManager.requestIdReadAll(rfcId2, true, idReadPortList);
							writeSEMLog("[STBC->RFC] REQ_IDREADALL , RFCID (" + rfcId2 + ") to ReadAll");
						}
					}
				}
			}
		}		
		return sendS2F50(msg, hcAck);
	}

	/**
	 * 2012.03.10 by KYK
	 * @param requestMap
	 */
	private void setReadAllRequestMap(HashMap<String,ArrayList> requestMap) {
		// 1. 전체 포트에 대해 (rfc, portList) map 을 만든다. 
		stbCarrierLocManager.setReadAllRequestMap(requestMap);
		// 2. 비사용 RFC 는 제외한다.
		StringBuilder message = new StringBuilder();
		ArrayList<String> disabledRfcList = new ArrayList<String>(stbRfcDataManager.getDisabledRfcList());
		for (String rfcId: disabledRfcList) {
			requestMap.remove(rfcId);
			message.append(rfcId).append("/");
		}
		if (message.toString() != null && message.toString().length() > 0) {
			writeSEMLog("Disabled RFCID List [Enabled=false]: " + message.toString());
		}
	}

	/**
	 * Receive INSTALLLIST Command
	 * 
	 * @param msg
	 * @return
	 */
	private boolean receiveINSTALLLISTcommand(UComMsg msg) {		
		// RCMD : CARRIERDATALIST ----------------------------
		// <L [4]
		//   <U2 0 >  /* DATAID */
		//   <A “>  /* OBJSPEC */
		//   <A ‘INSTALLLIST’>  /* RCMD */
		//   <L [1]
		//     <L [2]
		//       <A ‘CARRIERDATALIST’>  /* CPNAME */
		//       <L [N]
		//         <L [2]
		//           <A ‘CARRIERDATA’>  /* CPNAME */
		//           <L [2]
		//             <L [2]
		//               <A ‘CARRIERID’>  /* CPNAME */
		//               <A CarrierID>  /* CEPVAL */
		//             >
		//             <L [2]
		//               <A ‘CARRIERLOC’>  /* CPNAME */
		//               <A CarrierLoc>  /* CEPVAL */
		//         > > >
		//		   ...
		//	 > > >
		// ----------------------------------------------------
		
		// 2012.05.18 by KYK : Time Check
		long elapsedTime = System.currentTimeMillis();
		
		int hcAck = 4;
		int itemCnt;
		int dataCnt;
		int resultCode = 0;
		String str;
		String strLog;
		String carrierId = null;
		String carrierLoc = null;
		HashMap<String, String> carrierDataTable = new HashMap<String, String>();
		
		boolean isReconcileFailed = false; // 2013.01.04 by KYK

		itemCnt = msg.getListItem(); // L, 1 
		if (itemCnt != 1) {
			hcAck = 3; // at least one parameter isn't valid
			strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 1> : " + itemCnt;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			return sendS2F50(msg, hcAck);
		} else {
			// Get 'CARRIERDATALIST' data from S2F49 
			itemCnt = msg.getListItem(); // L, 2
			if (itemCnt != 2) {
				hcAck = 3; // at least one parameter isn't valid
				strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 2> :  A point" + itemCnt;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				return sendS2F50(msg, hcAck);
			}			
			str = msg.getAsciiItem(); // CPNAME1 : "CARRIERDATALIST"
			dataCnt = msg.getListItem(); // L, n			
			if (CARRIERDATALIST.equals(str) == false) {
				hcAck = 3; // at least one parameter isn't valid
				strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<CARRIERDATALIST>";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				return sendS2F50(msg, hcAck);
			} else {				
				for (int i = 0; i < dataCnt; i++) {
					// Get 'CARRIERDATA' 
					itemCnt = msg.getListItem(); // L, 2
					if (itemCnt != 2) {
						hcAck = 3; // at least one parameter isn't valid
						strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (CARRIERDATA itemCnt<L, 2> : " + itemCnt;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						return sendS2F50(msg, hcAck);
					}					
					str = msg.getAsciiItem(); // CPNAME : 'CARRIERDATA'
					itemCnt = msg.getListItem(); // L, 2
					if (CARRIERDATA.equals(str) && itemCnt == 2) {
						itemCnt = msg.getListItem(); // L, 2
						if (itemCnt != 2) {
							hcAck = 3; // at least one parameter isn't valid
							strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 2> : " + itemCnt;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							return sendS2F50(msg, hcAck);
						} else {
							// Get 'CARRIERID' 
							str = msg.getAsciiItem(); // CPNAME : 'CARRIERID'
							if (CARRIERID.equals(str)) {
								carrierId = msg.getAsciiItem();
							} else {
								hcAck = 3;
								strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<CARRIERID> or CPVAL";
								writeSEMLog(strLog);
								updateSEMHistory(RECEIVED, 2, 49, strLog);
								return sendS2F50(msg, hcAck);
							}
						}
						
						itemCnt = msg.getListItem(); // L, 2
						if (itemCnt != 2) {
							hcAck = 3; // at least one parameter isn't valid
							strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 2> : " + itemCnt;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							return sendS2F50(msg, hcAck);
						} else {
							// Get 'CARRIERLOC'  							
							str = msg.getAsciiItem(); // CPNAME : 'CARRIERLOC'
							if (CARRIERLOC.equals(str)) {
								carrierLoc = msg.getAsciiItem();
							} else {
								hcAck = 3;
								strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<CARRIERLOC> or CPVAL";
								writeSEMLog(strLog);
								updateSEMHistory(RECEIVED, 2, 49, strLog);
								return sendS2F50(msg, hcAck);		
							}						
						}
					} else {
						hcAck = 3;
						strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<CARRIERDATA> or CPVAL";
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						return sendS2F50(msg, hcAck);		
					}						
					carrierDataTable.put(carrierLoc, carrierId);	
//					writeSEMLog("	INSTALLLIST itemNo." + i + "> CarrierId: " + carrierId + " ,CarrierLoc:" + carrierLoc);
				}					
			}
		}
		// REPLY
		sendS2F50(msg, hcAck);
		
		// 2013.01.04 by KYK : 위치올림
		writeSEMLog("	CARRIERDATALIST ItemCount:" + dataCnt);		
		
		if (hcAck == 4) {
//		if (hcAck == 4 && dataCnt > 0 && carrierDataTable.size() > 0) {			
			// Batch Update To DB (One Message)
			// 2013.01.04 by KYK
			if (stbCarrierLocManager.updateActiveCarriersToDB(carrierDataTable)) {
				// In Case MCS is down-up, STBC send OCSData even if Option is MCS.
				isFirst = false;
				writeSEMLog("	Completed to Update CarrierDataList(S2F49 INSTALLLIST)");
			} else {
				stbCarrierLocManager.clearAllSTBData();
				resultCode = 1;
				isFirst = true;
				writeSEMLog("	Failed to Update CarrierDataList. Check Unregisted-Port in OCS or DB Exception.");
	
				// clear ? : 사용자 알람강제 삭제
				// 2014.12.19 by KYK TODO
//				registerAlarmText("[Reconcile] Failed to Install STBData from MCS(S2F49 INSTALLLIST). ");
				registerAlarmTextWithLevel("[Reconcile] Failed to Install STBData from MCS(S2F49 INSTALLLIST). ",ALARMLEVEL.ERROR);
				// InCase Reconcile Fail, change STBCUSAGE=NO ??
				isReconcileFailed = true;
			}
			
			ReportItems rItems = new ReportItems();				
			rItems.setResultCode(resultCode);
			sendS6F11(CarrierDataListInstalled, rItems);
		} else {
			// MCS -> STBC 데이터 다시 받을수있도록함
			isFirst = true;
		}
		
		// 2012.05.18 by KYK
		writeSEMLog("	[INSTALLLIST] ProcessignTime (Received & DBUpdated) : "  + (System.currentTimeMillis() - elapsedTime) + "ms");
		
		// 2013.01.04 by KYK
		// InCase Reconcile Failed, Disable STBC 
		if (isReconcileFailed) {
			writeSEMLog("STBCUSAGE(YES->NO)'s Changed by Reconcile-Fail.");
			// 2014.12.19 by KYK TODO
//			registerAlarmText("STBCUSAGE(YES->NO)'s Changed by Reconcile-Fail.");
			registerAlarmTextWithLevel("STBCUSAGE(YES->NO)'s Changed by Reconcile-Fail.",ALARMLEVEL.ERROR);
			// change STBCUSAGE=NO
			updateOcsInfoToDB(STBCUSAGE, "NO");
		}
		return true;
	}

	/**
	 * Receive INSTALLLIST2 Command
	 * 
	 * @param msg
	 * @return
	 */
	private boolean receiveINSTALLLIST2command(UComMsg msg) {		
		// RCMD : CARRIERDATALIST ----------------------------
		// <L [4]
		//   <U2 0 >  /* DATAID */
		//   <A “>  /* OBJSPEC */
		//   <A ‘INSTALLLIST’>  /* RCMD */
		//   <L [1]
		//     <L [2]
		//       <A ‘CARRIERDATALIST’>  /* CPNAME */
		//       <L [N]
		//         <L [2]
		//           <A ‘CARRIERDATA’>  /* CPNAME */
		//           <L [3]
		//             <L [2]
		//               <A ‘CARRIERID’>  /* CPNAME */
		//               <A CarrierID>  /* CEPVAL */
		//             >
		//             <L [2]
		//               <A ‘CARRIERLOC’>  /* CPNAME */
		//               <A CarrierLoc>  /* CEPVAL */
		//             >
		//             <L [2]
		//               <A ‘FOUPID’>  /* CPNAME */
		//               <A FoupID>  /* CEPVAL */
		//         > > >
		//		   ...
		//	 > > >
		// ----------------------------------------------------
		
		// 2012.05.18 by KYK : Time Check
		long elapsedTime = System.currentTimeMillis();
		
		int hcAck = 4;
		int itemCnt;
		int dataCnt;
		int resultCode = 0;
		String str;
		String strLog;
		String carrierId = null;
		String carrierLoc = null;
		String foupId = null;
		HashMap<String, String> carrierDataTable = new HashMap<String, String>();
		
		boolean isReconcileFailed = false; // 2013.01.04 by KYK
		
		itemCnt = msg.getListItem(); // L, 1 
		if (itemCnt != 1) {
			hcAck = 3; // at least one parameter isn't valid
			strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 1> : " + itemCnt;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			return sendS2F50(msg, hcAck);
		} else {
			// Get 'CARRIERDATALIST' data from S2F49 
			itemCnt = msg.getListItem(); // L, 2
			if (itemCnt != 2) {
				hcAck = 3; // at least one parameter isn't valid
				strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 2> :  A point" + itemCnt;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				return sendS2F50(msg, hcAck);
			}			
			str = msg.getAsciiItem(); // CPNAME1 : "CARRIERDATALIST"
			dataCnt = msg.getListItem(); // L, n			
			if (CARRIERDATALIST.equals(str) == false) {
				hcAck = 3; // at least one parameter isn't valid
				strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<CARRIERDATALIST>";
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				return sendS2F50(msg, hcAck);
			} else {				
				for (int i = 0; i < dataCnt; i++) {
					// Get 'CARRIERDATA2' 
					itemCnt = msg.getListItem(); // L, 3
					if (itemCnt != 2) {
						hcAck = 3; // at least one parameter isn't valid
						strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (CARRIERDATA2 itemCnt<L, 3> : " + itemCnt;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						return sendS2F50(msg, hcAck);
					}					
					str = msg.getAsciiItem(); // CPNAME : 'CARRIERDATA2'
					itemCnt = msg.getListItem(); // L, 3
					if (CARRIERDATA2.equals(str) && itemCnt == 3) {
						itemCnt = msg.getListItem(); // L, 2
						if (itemCnt != 2) {
							hcAck = 3; // at least one parameter isn't valid
							strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 2> : " + itemCnt;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							return sendS2F50(msg, hcAck);
						} else {
							// Get 'CARRIERID' 
							str = msg.getAsciiItem(); // CPNAME : 'CARRIERID'
							if (CARRIERID.equals(str)) {
								carrierId = msg.getAsciiItem();
							} else {
								hcAck = 3;
								strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<CARRIERID> or CPVAL";
								writeSEMLog(strLog);
								updateSEMHistory(RECEIVED, 2, 49, strLog);
								return sendS2F50(msg, hcAck);
							}
						}
						
						itemCnt = msg.getListItem(); // L, 2
						if (itemCnt != 2) {
							hcAck = 3; // at least one parameter isn't valid
							strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 2> : " + itemCnt;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							return sendS2F50(msg, hcAck);
						} else {
							// Get 'CARRIERLOC'  							
							str = msg.getAsciiItem(); // CPNAME : 'CARRIERLOC'
							if (CARRIERLOC.equals(str)) {
								carrierLoc = msg.getAsciiItem();
							} else {
								hcAck = 3;
								strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<CARRIERLOC> or CPVAL";
								writeSEMLog(strLog);
								updateSEMHistory(RECEIVED, 2, 49, strLog);
								return sendS2F50(msg, hcAck);		
							}						
						}
						
						itemCnt = msg.getListItem(); // L, 2
						if (itemCnt != 2) {
							hcAck = 3; // at least one parameter isn't valid
							strLog = "[MCS->STBC] S2F49 : Invalid CPVAL (itemCnt<L, 2> : " + itemCnt;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							return sendS2F50(msg, hcAck);
						} else {
							// Get 'FOUPID'  							
							str = msg.getAsciiItem(); // CPNAME : 'FOUPID'
							if (FOUPID.equals(str)) {
								foupId = msg.getAsciiItem();
							} else {
								hcAck = 3;
								strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<FOUPID> or CPVAL";
								writeSEMLog(strLog);
								updateSEMHistory(RECEIVED, 2, 49, strLog);
								return sendS2F50(msg, hcAck);		
							}						
						}
					} else {
						hcAck = 3;
						strLog = "[MCS->STBC] S2F49 : Invalid CPNAME<CARRIERDATA2> or CPVAL";
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						return sendS2F50(msg, hcAck);		
					}						
					carrierDataTable.put(carrierLoc, carrierId);	
				}					
			}
		}
		// REPLY
		sendS2F50(msg, hcAck);
		
		// 2013.01.04 by KYK : 위치올림
		writeSEMLog("	CARRIERDATALIST2 ItemCount:" + dataCnt);		
		
		if (hcAck == 4) {
//		if (hcAck == 4 && dataCnt > 0 && carrierDataTable.size() > 0) {			
			// Batch Update To DB (One Message)
			// 2013.01.04 by KYK
			if (stbCarrierLocManager.updateActiveCarriersToDB(carrierDataTable)) {
				// In Case MCS is down-up, STBC send OCSData even if Option is MCS.
				isFirst = false;
				writeSEMLog("	Completed to Update CarrierDataList(S2F49 INSTALLLIST2)");
			} else {
				stbCarrierLocManager.clearAllSTBData();
				resultCode = 1;
				isFirst = true;
				writeSEMLog("	Failed to Update CarrierDataList. Check Unregisted-Port in OCS or DB Exception.");
	
				// clear ? : 사용자 알람강제 삭제
				// 2014.12.19 by KYK TODO
//				registerAlarmText("[Reconcile] Failed to Install STBData from MCS(S2F49 INSTALLLIST2). ");
				registerAlarmTextWithLevel("[Reconcile] Failed to Install STBData from MCS(S2F49 INSTALLLIST2). ",ALARMLEVEL.ERROR);
				// InCase Reconcile Fail, change STBCUSAGE=NO ??
				isReconcileFailed = true;
			}
			
			ReportItems rItems = new ReportItems();				
			rItems.setResultCode(resultCode);
			sendS6F11(CarrierDataListInstalled, rItems);
		} else {
			// MCS -> STBC 데이터 다시 받을수있도록함
			isFirst = true;
		}
		
		// 2012.05.18 by KYK
		writeSEMLog("	[INSTALLLIST2] ProcessignTime (Received & DBUpdated) : "  + (System.currentTimeMillis() - elapsedTime) + "ms");
		
		// 2013.01.04 by KYK
		// InCase Reconcile Failed, Disable STBC 
		if (isReconcileFailed) {
			writeSEMLog("STBCUSAGE(YES->NO)'s Changed by Reconcile-Fail.");
			// 2014.12.19 by KYK TODO
//			registerAlarmText("STBCUSAGE(YES->NO)'s Changed by Reconcile-Fail.");
			registerAlarmTextWithLevel("STBCUSAGE(YES->NO)'s Changed by Reconcile-Fail.",ALARMLEVEL.ERROR);
			// change STBCUSAGE=NO
			updateOcsInfoToDB(STBCUSAGE, "NO");
		}
		return true;
	}
	
	// 이거 사양서에는 L,2 (기존코드가 1이었음;)
	/**
	 * Send S2F50
	 */
	private boolean sendS2F50(UComMsg msg, int hcAck) {
		UComMsg rsp = uCom.makeSecsMsg(2, 50, msg.getSysbytes());
		rsp.setListItem(2);
		rsp.setBinaryItem(hcAck);
		rsp.setListItem(0);		
		sendUComMsg(rsp, "S2F50");	

		String strLog = "[STBC->MCS] S2F50 : HCACK=" + hcAck;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 41, strLog);
		
		if (hcAck == 4) {
			return true;			
		} else {
			return false;
		}
	}

	/**
	 * Make STB ReportItems
	 * 
	 * @param carrierLocId
	 * @param mcsCarrierId
	 * @param mcsFoupId
	 * @return
	 */
	private ReportItems makeSTBReportItems(String carrierLocId, String mcsCarrierId, String mcsFoupId) {		
		ReportItems rItems = new ReportItems();
		rItems.setCarrierLoc(carrierLocId);
		rItems.setCarrierId(mcsCarrierId);
		rItems.setFoupId(mcsFoupId);
		return rItems;
	}

	@Override
	public UComMsg setActiveCarriersToSecsMsg(UComMsg rsp) {		
		// 2010.07.21 by MYM : [ActiveCarriers 개선]
		// 'STBDATA_RECOVERY_OPTION' 설정값  OCS 이면 STBC 데이터를 MCS로 Install, 그 이외의 값이면 사양대로 처리
		// ※ 사양
		//    - STBC Down-Up : MCS 데이터를 STBC로 Install
		//    - MCS Down-Up  : STBC 데이터를 MCS로 Install
		// 2009.10.13 by MYM : ActiveCarriers Reconcile 시 isFirst값을 OCSINFO 파라미터의 설정에 따라 결정
		//                     true : STBC 데이터를 MCS로 Install, false : MCS 데이터를 STBC로 Install
		// 배경 : Reconcile 시 Operation 시간 참조하게 되는데 시간체크 주기와 타이밍이 맞지않아
		//       Operation 구동이 되고 있는데 flag 잘못 설정 될 수 있으므로 Operation 시간을 참조하는 것 의미 없음.		
		String strLog;
		String recoveryOption = getStbDataRecoveryOption();
		
		if (!stbCarrierLocManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying ActiveCarriers");
			writeSEMLog("STBC_USAGE(YES->NO)'s Changed by Reconcile-Fail.");
			registerAlarmTextWithLevel("STBC_USAGE(YES->NO)'s Changed by Reconcile-Fail.", ALARMLEVEL.ERROR);

			// If reconcile failed, disable STBC
			updateOcsInfoToDB(STBCUSAGE, "NO");
			return null;
		}
		
		// 2018.08.31 by LSH : Reconcile 전, STBCarrierloc에 쌓인 이벤트 일괄 처리 절차 추가
		int updateCarrierLocStatusCount = 0;
		updateCarrierLocStatusCount = updateAllCarrierLocStatus();
		writeSEMLog("Update All STB CarrierLoc Status. Count: " + updateCarrierLocStatusCount);
		
		// 2013.01.04 by KYK : Reconcile시 STBBackupData 남김
		saveSTBBackupDataToFile();
		
		// 여기에서 ActiveCarriers Data 받아옴
		int itemCnt = 0;
		HashMap<String, STBCarrierLoc> activecarriersTable = new HashMap<String, STBCarrierLoc>();
		writeSEMLog("	[OCSParameter] STBDataRecoveryOption: " + recoveryOption);		
		if (STBC.equalsIgnoreCase(recoveryOption) || OCS.equalsIgnoreCase(recoveryOption) || isFirst == false) {
			// Case1 : 데이터 업로드 (STBC -> MCS)
			try {
				activecarriersTable = stbCarrierLocManager.uploadActiveCarriersFromDB();
			} catch (Exception e) {
				e.printStackTrace();
				writeSEMLog("DB connection fail while querying ActiveCarriers");
				traceException("DB Exception(ActiveCarriers)", e);
				writeSEMLog("STBC_USAGE(YES->NO)'s Changed by Reconcile-Fail.");
				registerAlarmTextWithLevel("STBC_USAGE(YES->NO)'s Changed by Reconcile-Fail.", ALARMLEVEL.ERROR);
				updateOcsInfoToDB(STBCUSAGE, "NO");
				return null;
			}
			itemCnt = activecarriersTable.size();
			writeSEMLog("	STBC Uploads Data to MCS. (MCSDownUp or Option:STBC)");
		} else {
			// Case2 : 데이터 올리지 않음 (dataCnt:0 으로 올림), MCS 데이터를 수신함	
			writeSEMLog("	STBC Clears current data.");
			stbCarrierLocManager.updateCarrierIdCleared();
			itemCnt = 0;			
		}		
		// 2013.01.04 by KYK
		strLog = "	ActiveCarriers itemCnt:" + itemCnt;
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 1, 3, strLog);
		// 이후 MCS Down 발생시, STB Data 올리기 위함
		// 2018.08.31 by LSH : 데이터 동기화 옵션이 MCS 일 때
		// InstallList를 받기전에 MCS와 끊어졌다 붙는 경우, STBC→MCS 데이터 동기화가 발생할 수 있어서 주석 처리
		// 실제 InstallList를 MCS에게 받아서 처리까지 하고 난 뒤, receiveINSTALLLISTcommand(UComMsg msg)에서 isFirst=false 처리 
//		isFirst = false;
		
		rsp.setListItem(itemCnt);
		if (itemCnt > 0) {	
			for (STBCarrierLoc carrierLoc : activecarriersTable.values()) {
				rsp.setListItem(4);
				rsp.setAsciiItem(carrierLoc.getCarrierId());
				rsp.setAsciiItem(carrierLoc.getCarrierLocId());
				rsp.setU2Item(convertCarrierStateToU2(CarrierState, INSTALLED));
				rsp.setAsciiItem(carrierLoc.getInstalledTime());
			}
		}		
		return rsp;
	}
	
	@Override
	public UComMsg setActiveCarriers2ToSecsMsg(UComMsg rsp) {		
		// 2010.07.21 by MYM : [ActiveCarriers 개선]
		// 'STBDATA_RECOVERY_OPTION' 설정값  OCS 이면 STBC 데이터를 MCS로 Install, 그 이외의 값이면 사양대로 처리
		// ※ 사양
		//    - STBC Down-Up : MCS 데이터를 STBC로 Install
		//    - MCS Down-Up  : STBC 데이터를 MCS로 Install
		// 2009.10.13 by MYM : ActiveCarriers Reconcile 시 isFirst값을 OCSINFO 파라미터의 설정에 따라 결정
		//                     true : STBC 데이터를 MCS로 Install, false : MCS 데이터를 STBC로 Install
		// 배경 : Reconcile 시 Operation 시간 참조하게 되는데 시간체크 주기와 타이밍이 맞지않아
		//       Operation 구동이 되고 있는데 flag 잘못 설정 될 수 있으므로 Operation 시간을 참조하는 것 의미 없음.		
		String strLog;
		String recoveryOption = getStbDataRecoveryOption();
		
		if (!stbCarrierLocManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying ActiveCarriers");
			writeSEMLog("STBC_USAGE(YES->NO)'s Changed by Reconcile-Fail.");
			registerAlarmTextWithLevel("STBC_USAGE(YES->NO)'s Changed by Reconcile-Fail.", ALARMLEVEL.ERROR);

			// If reconcile failed, disable STBC
			updateOcsInfoToDB(STBCUSAGE, "NO");
			return null;
		}
		
		// 2018.08.31 by LSH : Reconcile 전, STBCarrierloc에 쌓인 이벤트 일괄 처리 절차 추가
		int updateCarrierLocStatusCount = 0;
		updateCarrierLocStatusCount = updateAllCarrierLocStatus();
		writeSEMLog("Update All STB CarrierLoc Status. Count: " + updateCarrierLocStatusCount);
		
		// 2013.01.04 by KYK : Reconcile시 STBBackupData 남김
		saveSTBBackupDataToFile();
		
		// 여기에서 ActiveCarriers2 Data 받아옴
		int itemCnt = 0;
		HashMap<String, STBCarrierLoc> activecarriers2Table = new HashMap<String, STBCarrierLoc>();
		writeSEMLog("	[OCSParameter] STBDataRecoveryOption: " + recoveryOption);		
		if (STBC.equalsIgnoreCase(recoveryOption) || OCS.equalsIgnoreCase(recoveryOption) || isFirst == false) {
			// Case1 : 데이터 업로드 (STBC -> MCS)
			try {
				activecarriers2Table = stbCarrierLocManager.uploadActiveCarriers2FromDB();
			} catch (Exception e) {
				e.printStackTrace();
				writeSEMLog("DB connection fail while querying ActiveCarriers2");
				traceException("DB Exception(ActiveCarriers2)", e);
				writeSEMLog("STBC_USAGE(YES->NO)'s Changed by Reconcile-Fail.");
				registerAlarmTextWithLevel("STBC_USAGE(YES->NO)'s Changed by Reconcile-Fail.", ALARMLEVEL.ERROR);
				updateOcsInfoToDB(STBCUSAGE, "NO");
				return null;
			}
			itemCnt = activecarriers2Table.size();
			writeSEMLog("	STBC Uploads Data to MCS. (MCSDownUp or Option:STBC)");
		} else {
			// Case2 : 데이터 올리지 않음 (dataCnt:0 으로 올림), MCS 데이터를 수신함	
			writeSEMLog("	STBC Clears current data.");
			stbCarrierLocManager.updateCarrierIdCleared();
			itemCnt = 0;			
		}		
		// 2013.01.04 by KYK
		strLog = "	ActiveCarriers2 itemCnt:" + itemCnt;
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 1, 3, strLog);
		// 이후 MCS Down 발생시, STB Data 올리기 위함
		// 2018.08.31 by LSH : 데이터 동기화 옵션이 MCS 일 때
		// InstallList를 받기전에 MCS와 끊어졌다 붙는 경우, STBC→MCS 데이터 동기화가 발생할 수 있어서 주석 처리
		// 실제 InstallList를 MCS에게 받아서 처리까지 하고 난 뒤, receiveINSTALLLISTcommand(UComMsg msg)에서 isFirst=false 처리 
//		isFirst = false;
		
		rsp.setListItem(itemCnt);
		if (itemCnt > 0) {	
			for (STBCarrierLoc carrierLoc : activecarriers2Table.values()) {
				rsp.setListItem(5);
				rsp.setAsciiItem(carrierLoc.getCarrierId());
				rsp.setAsciiItem(carrierLoc.getCarrierLocId());
				rsp.setU2Item(convertCarrierStateToU2(CarrierState, INSTALLED));
				rsp.setAsciiItem(carrierLoc.getInstalledTime());
				rsp.setAsciiItem(carrierLoc.getFoupId());
			}
		}		
		return rsp;
	}

	// 2013.01.04 by KYK : 다시부활시킴
	/**
	 * Runtime 중 변경된 STBData를 실시간 업데이트 함 
	 * File : STBRuntimeData
	 */
	public void saveSTBBackupDataToFile(){
		if (ocsInfoManager.isSTBDataSaveUsed()) {
			// make BackupFileName
			String stbBackupFileName = makeFileRouteName(STBBACKUPDATA);
			saveSTBDataToFile(stbBackupFileName);
		}		
	}

	/**
	 * STBData(DB insert문 형태)를 File로  저장 
	 * @param stbDataFileName
	 */
	public void saveSTBDataToFile(String stbDataFileName) {
		long checkTime = Math.abs(System.currentTimeMillis());
		BufferedWriter out = null;			
		try {
			out = new BufferedWriter(new FileWriter(stbDataFileName, false));

			Iterator it = stbCarrierLocManager.getData().values().iterator();
			STBCarrierLoc carrierLoc;
			while (it.hasNext()) {
				carrierLoc = (STBCarrierLoc) it.next();
				StringBuilder sb = new StringBuilder("UPDATE STBCARRIERLOC SET CARRIERID='");
				sb.append(carrierLoc.getCarrierId());
				sb.append("', COMMANDNAME='', MCSCARRIERID='' WHERE CARRIERLOCID='");
				sb.append(carrierLoc.getCarrierLocId()).append("';");					

				out.write(sb.toString());
				out.newLine();
			}
//			out.write(" -- SaveTime : " + (System.currentTimeMillis() - checkTime/1000.));
			//System.out.println("STB SaveTime : " + (System.currentTimeMillis() - checkTime/1000.));
			out.newLine();
			out.flush();

//			stbDataSaveTime = System.currentTimeMillis();
		} catch (IOException e) {
			e.printStackTrace();
			traceException("DBException, SaveSTBDataToFile() - SQLException ", e);
		} catch (Exception e) {
			e.printStackTrace();
			traceException("Exception, SaveSTBDataToFile() - Exception ", e);
		} finally {
			try	{
				if (out != null) out.close();
			} catch (Exception e){}
		}					
	}

	/**
	 * File 생성시 Directory 경로 및 파일이름을 만들어주는 메소드
	 * @param filename
	 * @return
	 */
	public String makeFileRouteName(String filename) {
		String path = System.getProperty(HOMEDIR);
		String separator = System.getProperty(FILESEPARATOR);

		SimpleDateFormat dateForm = new SimpleDateFormat("yyyyMMddHHmmss");
		String dateString = dateForm.format(new Date());

		StringBuilder sb = new StringBuilder(path);
		sb.append(separator); sb.append("log"); sb.append(separator);
		sb.append(filename); sb.append(dateString); sb.append(".log");

		return sb.toString();
	}

	/**
	 * 2014.12.19 by KYK TODO alarmTextWithLevel 로 변경
	 * @param alarmText
	 * @param alarmLevel
	 */
	private void registerAlarmTextWithLevel(String alarmText, ALARMLEVEL alarmLevel) {
		stbcManager.registerAlarmTextWithLevel(alarmText, alarmLevel);
	}

	/**
	 * 2013.01.04 by KYK
	 * @param alarmText
	 */
	private void registerAlarmText(String alarmText) {
//		stbcManager.registerAlarmText(alarmText);
		alarmManager.registerAlarmText(STBC, alarmText);				
	}

//	/**
//	 * 2013.01.04 by KYK
//	 * @param alarmText
//	 */
//	private void unregisterAlarmText(String alarmText) {
////		stbcManager.unregisterAlarmText(alarmText);
//		alarmManager.unregisterAlarmText(STBC, alarmText);						
//	}
//
//	/**
//	 * 2013.01.04 by KYK
//	 * @param eventHistory
//	 * @param duplicateCheck
//	 */
//	private void registerEventHistory(EventHistory eventHistory, boolean duplicateCheck) {
//		eventHistoryManager.addEventHistoryToRegisterList(eventHistory, duplicateCheck);		
//	}

	/**
	 * 2013.01.04 by KYK
	 * @param name
	 * @param value
	 * @return
	 */
	public boolean updateOcsInfoToDB(String name, String value) {
		ocsInfoManager.addOCSInfoToOCSInfoUpdateList(name, value);
		return true;
	}

	/**
	 * 2013.01.04 by KYK
	 * @return
	 */
	private String getStbDataRecoveryOption() {
		return ocsInfoManager.getStbDataRecoveryOption();
	}
	
	/**
	 * Convert CarrierState to U2
	 */
	private int convertCarrierStateToU2(String stateName, String state) {		
		STBState stbState = stbStateManager.getStbStateData(stateName);
		if (stateName.equalsIgnoreCase(stbState.getStateName()) &&
				state.equalsIgnoreCase(stbState.getState())) {
			return Integer.parseInt(stbState.getValue());
		}
		return 0;
	}
	
	private boolean updateStbInfoToDB(String name, String value) {
//		stbInfoManager.addOCSInfoToOCSInfoUpdateList(name, value);
		stbInfoManager.updateSTBInfoToDB(name, value);
		return true;
	}	

	@Override
	public UComMsg setCurrentPortStateToSecsMsg(UComMsg rsp) {
		// : L,n <PortInfo> 
		// 		L,3 <PortID>
		//			<PortTransferState> 1=OutOfService 2=InService
		//			<IDReader>			0=not installed 1=installed	

		int itemCnt = 0;
		STBCarrierLoc carrierLoc;
		ConcurrentHashMap<String, Object> carrierLocTable = stbCarrierLocManager.getData();
		
		itemCnt = carrierLocTable.size();
		rsp.setListItem(itemCnt);
		if (itemCnt > 0) {
			for (String carrierLocId : carrierLocTable.keySet()) {
				carrierLoc = (STBCarrierLoc) carrierLocTable.get(carrierLocId);				
				rsp.setListItem(3);
				rsp.setAsciiItem(carrierLocId);
				if (carrierLoc.isEnabled()) {					
					rsp.setU2Item(2); // InService
				} else {					
					rsp.setU2Item(1); // OutOfService
				}
				rsp.setU2Item(convertIdReaderValueToU2(carrierLoc.getIdReader()));
			}
		}		
		return rsp;
	}

	/**
	 * Convert IDReader Value to U2
	 * 
	 * @param idReader
	 * @return
	 */
	private int convertIdReaderValueToU2(String idReader) {
		if (INSTALLED.equalsIgnoreCase(idReader)) {
			return 1;
		} else {
			return 0;
		}		
	}

	@Override
	public UComMsg setCarrierInfoToSecsMsg(UComMsg rsp, String carrierId) {		
		// 이거 좀 이상함 원래소스가 잘못된건지 확인 필 : 원래소스가 이상함, 호출될일이 없어서 그랬던듯
		STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(carrierId);
		
		rsp.setListItem(4);
		if (carrierLoc == null) {
			rsp.setAsciiItem("");
			rsp.setAsciiItem("");
			rsp.setAsciiItem("");			
			rsp.setAsciiItem("");						
		} else {
			rsp.setAsciiItem(carrierLoc.getCarrierId());
			rsp.setAsciiItem(carrierLoc.getCarrierLocId());
			rsp.setAsciiItem(carrierLoc.getCarrierState());
			rsp.setAsciiItem(carrierLoc.getInstalledTime());			
		}		
		return rsp;
	}

	@Override
	protected int setEcVData(int vid, UComMsg msg) {
		if (ecIdOTable.containsKey(vid)) {
			int value, min, max;
			String name;
			EquipmentConstant ec = ecIdOTable.get(vid);			
			// Establish Communication Timeout  U2
			if (vid == 2) {
				value = msg.getU2Item();
				max = ec.getMax();
				min = ec.getMin();				
				if (value == 0) {
					return 5;
				} else {
					if (min <= value && value <= max) {
						ec.setNumValue(value);
						ecIdOTable.put(vid, ec);						
						updateStbInfoToDB(COMMESTTIMEOUT, String.valueOf(value));						
						return NO_ERROR;
					} else {
						return 3; // beyond the allowed range
					}
				}
			} else if (vid == 61) { // EqpName A[1~80]
				name = msg.getAsciiItem();
				if (name == null) {
					return 5;
				} else {
					ec.setStrValue(name);
					updateStbInfoToDB(OCSNAME, name);					
					return NO_ERROR;
				}
			}
		}
		return ER_UNDEFINED_ECID;
	}

	@Override
	public UComMsg setInstallTimeToSecsMsg(UComMsg rsp, ReportItems rItems) {
		String carrierId = rItems.getCarrierId();
		STBCarrierLoc carrierLoc = stbCarrierLocManager.getCarrierLocFromDBWhereCarrierId(carrierId);
		
		rsp.setListItem(1);
		if (carrierLoc == null) {
			rsp.setAsciiItem("");
		} else {
			rsp.setAsciiItem(carrierLoc.getInstalledTime());
		}		
		return rsp;
	}

	@Override
	public UComMsg setTSCStatusToSecsMsg(UComMsg rsp) {
		int stateResult;
		if ("".equals(tscStatus)) {
			rsp.setU2Item(0);
		} else {
			if (TSC_NONE.equals(tscStatus) || TSC_INIT.equals(tscStatus)) {
				stateResult = 1;
			} else if (TSC_PAUSED.equals(tscStatus)) {
				stateResult = 2;
			} else if (TSC_AUTO.equals(tscStatus)) {
				stateResult = 3;
			} else if (TSC_PAUSING.equals(tscStatus)) {
				stateResult = 4;
			} else {
				stateResult = 0;
			}
			rsp.setU2Item(stateResult);
		}		
		return rsp;
	}

	@Override
	public void setSEMStatusToDB() {
		if (!ocsInfoManager.checkCurrentDBStatus()) {
			writeSEMLog("DB connection fail while updating SEM Status");
		}
		updateStbInfoToDB(HSMSSTATUS, hsmsStatus);
		updateStbInfoToDB(COMMSTATUS, commStatus);
		updateStbInfoToDB(CONTROLSTATUS, controlStatus);
		updateStbInfoToDB(TSCSTATUS, tscStatus);
		
		StringBuilder sb = new StringBuilder();
		sb.append("STBC Status : ");
		sb.append(hsmsStatus).append("/");
		sb.append(commStatus).append("/");
		sb.append(controlStatus).append("/");
		sb.append(tscStatus);		
		writeSEMLog(sb.toString());
	}

	@Override
	public void writeSEMLog(String log) {
		stbcManager.traceSTBCMain(log);
	}
	
	private void traceException(String message, Throwable t){
		stbcManager.traceException(message, t);
	}

	/**
	 * Trace S6F11 History
	 */
	@Override
	public void traceS6F11History(String eventName, ReportItems rItems) {		
		StringBuilder sb = new StringBuilder();
		sb.append("[STBC->MCS] S6F11 : "); sb.append(eventName); sb.append("> "); 		

		if (ControlStatusRemote.equalsIgnoreCase(eventName) ||
				EquipmentOffline.equalsIgnoreCase(eventName)) {
			sb.append("("); sb.append(controlStatus); sb.append("/"); 		
			sb.append(commStatus); sb.append(")");

		} else if (STBAutoCompleted.equalsIgnoreCase(eventName)||
				STBAutoInitiated.equalsIgnoreCase(eventName)||
				STBPauseCompleted.equalsIgnoreCase(eventName)||
				STBPauseInitiated.equalsIgnoreCase(eventName)||
				STBPaused.equalsIgnoreCase(eventName)) {
			sb.append("("); sb.append(tscStatus); sb.append(")");
		} else if (CarrierIDRead.equalsIgnoreCase(eventName) && rItems != null){
			sb.append("("); sb.append(rItems.getCarrierId()).append("/"); 		
			sb.append(rItems.getFoupId()).append("/"); 		
			sb.append(getNameOfIdReadStatus(rItems.getIdReadStatus())).append(")");			
		} else if (CarrierInstalled.equalsIgnoreCase(eventName)||
				CarrierRemoved.equalsIgnoreCase(eventName)) {
			if (rItems != null) {
				sb.append("("); sb.append(rItems.getCarrierId()).append("/"); 		
				sb.append(rItems.getCarrierLoc()).append(")");								
			}
		} else if (CarrierDataListInstalled.equalsIgnoreCase(eventName) && rItems != null) {
			sb.append("("); sb.append("ResultCode:"); 
			sb.append(rItems.getResultCode()); sb.append(")"); 			
		} else if (PortInService.equalsIgnoreCase(eventName) || 
				PortOutOfService.equalsIgnoreCase(eventName)) {
			if (rItems != null) {
				sb.append(" PortId: ").append(rItems.getCarrierLoc());
			}
		}
		writeSEMLog(sb.toString());
		updateSEMHistory(SENT, 6, 11, sb.toString());	
	}

	private String getNameOfIdReadStatus(int idReadStatus) {
		// IDRead Status
		String name;
		switch (idReadStatus) {
			case SUCCESS:
				name = "SUCCESS";
				break;
			case FAILURE:
				name = "FAILURE";
				break;
			case DUPLICATE:
				name = "DUPLICATE";
				break;
			case MISMATCH:
				name = "MISMATCH";
				break;
			case NO_LOAD:
				name = "NO_LOAD";
				break;
			default:
				name = "SUCCESS";
				break;
		}
		return name;
	}

	/**
	 * Respond S1F17 Status
	 */
	@Override
	public void respondS1F17Status() {
		controlStatus = REMOTE_ONLINE;
		sendS6F11(ControlStatusRemote, "", "", 0);
		tscStatus = TSC_INIT;
		sendS6F11("STBAutoInitiated", "", "", 0);
		tscStatus = TSC_PAUSED;
		sendS6F11("STBPaused", "", "", 0);
	}

	/**
	 * Update SEM History
	 */
	@Override
	public void updateSEMHistory(String type, int stream, int function,	String strLog) {
		// 2013.01.29 by KYK : DBdelay 발생 (delete 시) -> 사용안하도록함 (향후 formattedLog로 대체함)
//		String msgType = "S" + stream + "F" + function;
//		String eventTime = getCurrentTime();
//		STBCHistory stbcHistory = new STBCHistory(type, msgType, strLog, eventTime);
//		stbcHistoryManager.addSTBCHistoryToRegisterDB(stbcHistory);
	}
	
	@Override
	public UComMsg setEnhancedCarrierInfoToSecsMsg(UComMsg rsp, String carrierId) {
		// Do nothing. Method Not for This Module.
		return rsp;
	}
	@Override
	public UComMsg setEnhancedCarriersToSecsMsg(UComMsg rsp) {
		// Not Used Now
		return rsp;
	}
	@Override
	public UComMsg setEnhancedTransferCommandToSecsMsg(UComMsg rsp,	String commandId) {
		// Do nothing. Method Not for This Module.
		return rsp;
	}

	@Override
	public UComMsg setEnhancedTransfersToSecsMsg(UComMsg rsp) {
		// Do nothing. Method Not for This Module.
		return rsp;
	}

	@Override
	public UComMsg setTransferInfoToSecsMsg(UComMsg rsp, String carrierId) {
		// Do nothing. Method Not for This Module.
		return rsp;
	}

	@Override
	public UComMsg setTransferStateToSecsMsg(UComMsg rsp, String commandId) {
		// Do nothing. Method Not for This Module.
		return rsp;
	}

	@Override
	public UComMsg setCommandInfoToSecsMsg(UComMsg rsp, ReportItems reportItems) {
		// Not Used Now
		return rsp;
	}
	
	@Override
	public UComMsg setActiveTransfersToSecsMsg(UComMsg rsp) {
		// Do nothing. Method Not for This Module.
		return rsp;
	}

	@Override
	public UComMsg setActiveVehiclesToSecsMsg(UComMsg rsp) {
		// Do nothing. Method Not for This Module.
		return rsp;
	}
	
	public String getTscStatus(){
		return tscStatus;
	}
	public String getEqpName(){
		return mdln;
	}
	
	// 2012.03.10 by KYK
	public ArrayList<HashMap> getReadAllRequestList() {
		return readAllRequestList;
	}
	
	// 2012.04.05 by KYK
	private int getVerifyTimeout() {
		return stbcManager.getVerifyTimeout();
	}
	
	public int updateAllCarrierLocStatus() {
		int count = 0;
		count = stbCarrierLocManager.updateAllCarrierLocStatus();
		return count;
	}
	
}
