package com.samsung.ocs.ibsem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.config.CarrierTypeConfig;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.CARRIERLOC_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.VEHICLECOMM_TYPE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_DETAILSTATE;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_REMOTECMD;
import com.samsung.ocs.common.constant.TrCmdConstant.TRCMD_STATE;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.IBSEMHistoryManager;
import com.samsung.ocs.manager.impl.MaterialControlManager;
import com.samsung.ocs.manager.impl.NodeManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.RailDownControlManager;
import com.samsung.ocs.manager.impl.StationManager;
import com.samsung.ocs.manager.impl.TrCmdManager;
import com.samsung.ocs.manager.impl.VehicleErrorManager;
import com.samsung.ocs.manager.impl.VehicleManager;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.RailDownControl;
import com.samsung.ocs.manager.impl.model.Station;
import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.manager.impl.model.VehicleError;
import com.samsung.sem.SEMCommon;
import com.samsung.sem.items.AlarmItem;
import com.samsung.sem.items.CollectionEvent;
import com.samsung.sem.items.EquipmentConstant;
import com.samsung.sem.items.EventReport;
import com.samsung.sem.items.ReportItems;
import com.samsung.sem.ucominterface.UComMsg;

/**
 * IBSEM Class, OCS 3.0 for Unified FAB
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


public class IBSEM extends SEMCommon {

	/****************************************************************************
	 * IBSEM Member Variables
	 ****************************************************************************/
	public static String HOMEDIR = "user.dir";
	public static String FILESEPARATOR = "file.separator";

	private static String IBSEM_NONE = "IBSEM_NONE";
	private static String IBSEM_INIT = "IBSEM_INIT";
	private static String IBSEM_STOP = "IBSEM_STOP";
	private static String IBSEM_START = "IBSEM_START";

	// 2012.09.12 by KYK : [HCACK 세분화]
	private static String HCACK = "HCACK";
	private static String TRCMDID_DUPLICATE = "TRCMDID_DUPLICATE";
	private static String CARRIERID_DUPLICATE = "CARRIERID_DUPLICATE";
	private static String UNREGISTERED_SOURCELOC = "UNREGISTERED_SOURCELOC";
	private static String UNREGISTERED_DESTLOC = "UNREGISTERED_DESTLOC";
	private static String SOURCELOC_NOT_VEHICLEPORT_IN_DESTCHANGE = "SOURCELOC_NOT_VEHICLEPORT_IN_DESTCHANGE";
	private static String DIFFERENT_VEHICLEPORT_IN_DESTCHANGE = "DIFFERENT_VEHICLEPORT_IN_DESTCHANGE";
	private static String SOURCELOC_IS_VEHICLEPORT_IN_NORMAL_TRANSFER = "SOURCELOC_IS_VEHICLEPORT_IN_NORMAL_TRANSFER";
	private static String TRZONE_NOT_ACCEPTABLE = "TRZONE_NOT_ACCEPTABLE";
	private static String DIFFERENT_MATERIALTYPE = "DIFFERENT_MATERIALTYPE";
	// 2013.07.12 by KYK
	private static String UNREGISTERED_SOURCESTATION = "UNREGISTERED_SOURCESTATION";
	private static String UNREGISTERED_DESTSTATION  = "UNREGISTERED_DESTSTATION";
	private static String UNREADY_SOURCESTATION  = "UNREADY_SOURCESTATION";
	private static String UNREADY_DESTSTATION  = "UNREADY_DESTSTATION";
	// 2013.10.22 by KYK
	private static String UNREADY_SOURCELOC  = "UNREADY_SOURCELOC";
	private static String UNREADY_DESTLOC  = "UNREADY_DESTLOC";

	private String driverInitFileName = "IBSEM.xml";
	private String semStatus = IBSEM_NONE;
	private String tscAlarmStatus;	
	
	private int IB_NO_ERROR = 0;
	private int IBSEM_ERROR_BASE = 1000;
	private int ER_UNDEFINED_ECID = (IBSEM_ERROR_BASE + 1);
	private int ER_UNDEFINED_ALARMID = (IBSEM_ERROR_BASE + 5);
	private int MIN_ESTCOMM_TIMEOUT = 5;
	private int MAX_ESTCOMM_TIMEOUT = 30;
	private int DEFAULT_ESTCOMM_TIMEOUT = 10;

	// 2012.09.12 by KYK : [HCACK 세분화]
	private int HCACK_TRCMDID_DUPLICATE = 3;
	private int HCACK_CARRIERID_DUPLICATE = 3;
	private int HCACK_UNREGISTERED_SOURCELOC = 3;
	private int HCACK_UNREGISTERED_DESTLOC = 3;
	private int HCACK_SOURCELOC_NOT_VEHICLEPORT_IN_DESTCHANGE = 3;
	private int HCACK_DIFFERENT_VEHICLEPORT_IN_DESTCHANGE = 3;
	private int HCACK_SOURCELOC_IS_VEHICLEPORT_IN_NORMAL_TRANSFER = 3;
	private int HCACK_TRZONE_NOT_ACCEPTABLE = 2;
	private int HCACK_DIFFERENT_MATERIALTYPE = 2;	
	// 2013.07.12 by KYK
	private int HCACK_UNREGISTERED_SOURCESTATION = 3;
	private int HCACK_UNREGISTERED_DESTSTATION = 3;
	private int HCACK_UNREADY_SOURCESTATION = 3;
	private int HCACK_UNREADY_DESTSTATION = 3;
	// 2013.10.22 by KYK
	private int HCACK_UNREADY_SOURCELOC = 3;
	private int HCACK_UNREADY_DESTLOC = 3;	
	private int INVALID_TEACHING = -9999;
	// 2013.09.06 by KYK
	private String FOUP = "Foup";
	private String RETICLE = "Reticle";
	private String MAC = "Mac";

	private boolean isIdReaderInstalledOnVehicle = false;
	private boolean isRailDownCheckUsed = false;
	private VEHICLECOMM_TYPE vehicleCommType = VEHICLECOMM_TYPE.VEHICLECOMM_BYTE;

	private IBSEMManager ibsemManager;
	
	// DataManager
	private TrCmdManager trCmdManager;
	private VehicleManager vehicleManager;
	private OCSInfoManager ocsInfoManager;
	private CarrierLocManager carrierLocManager;
	private IBSEMHistoryManager ibsemHistoryManager;
	private RailDownControlManager railDownControlManager;
	private NodeManager nodeManager;
	// 2013.06.28 by KYK
	private VehicleErrorManager vehicleErrorManager;
	private StationManager stationManager;
	
	private MaterialControlManager materialControlManager;	// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
	
	private ArrayList<String> materialAssignAllowedList;	// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
	private ArrayList<String> sourceDestAssignAllowedList;	// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
	
	/**
	 * IBSEM Constructor : IBSEM 객체 생성 시 호출 (new IBSEM())
	 * @param owner
	 * @param managerControl
	 */
	public IBSEM(IBSEMManager ibsemManager) {
		super(); // SEMCommon constructor 실행
		this.ibsemManager = ibsemManager;	

		trCmdManager = TrCmdManager.getInstance(null, null, false, false, 0);
		vehicleManager = VehicleManager.getInstance(null, null, false, false, 0);
		ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);		
		carrierLocManager = CarrierLocManager.getInstance(null, null, false, false, 0);
		ibsemHistoryManager = IBSEMHistoryManager.getInstance(null, null, false, false, 0);
		railDownControlManager = RailDownControlManager.getInstance(RailDownControl.class, null, true, false, 0);
		nodeManager = NodeManager.getInstance(Node.class, null, true, false, 0);
		vehicleErrorManager = VehicleErrorManager.getInstance(VehicleError.class, null, false, false, 0);	// 2013.06.28 by KYK
		stationManager = StationManager.getInstance(Station.class, null, false, false, 0);
		materialControlManager = MaterialControlManager.getInstance(null, null, false, false, 0);
		
		initializeIBSEM();	
		
		if (ocsInfoManager.isIBSEMUsed()) {
			setSEMStatusToDB(); // 초기화		
			startIBSEM();
		}			
	}

	/**
	 * start IBSEM running with SEComDriver
	 * @return
	 */
	public boolean startIBSEM() {
		uCom.setCommCfgFile(driverInitFileName);
		if (IBSEM_INIT.equals(semStatus) || IBSEM_STOP.equals(semStatus)) {
			// start SECom
			if (uCom.startService()) {
				semStatus = IBSEM_START;				
				writeSEMLog("IBSEM's Started !!");
				return true;
			} else {
				writeSEMLog("IBSEM Fail, SECS Driver Not Started !!");
				return false;
			}			
		} else {
			//writeSEMLog("IBSEM's Already Started before !!");
			return false;
		}		
	}
	
	/**
	 * stop IBSEM running with SEComDriver
	 * @return
	 */
	public boolean stopIBSEM() {
		//
		if (uCom.stopService()) {
			hsmsStatus = TCPIP_NOT_CONNECTED;
			commStatus = COMM_DISABLED;
			controlStatus = CONTROL_NONE;
			tscStatus = TSC_NONE;
			tscAlarmStatus = TSC_NO_ALARMS;	
			semStatus = IBSEM_STOP;
			setSEMStatusToDB();
			
			writeSEMLog("IBSEM's Stopped !!");
			return true;
		} else {
			writeSEMLog("IBSEM Fail, SECS Driver Not Stopped !!");
			return false;
		}
	}
	
	/**
	 * initialize IBSEM for start, loading data 
	 */
	public void initializeIBSEM() {		
		mdln = getMdlnFromDB();
		softRev = getSoftVersionFromDB();
		semStatus = IBSEM_INIT;
		
		isIdReaderInstalledOnVehicle = ocsInfoManager.isIdReaderInstalledOnVehicle();
		isRailDownCheckUsed = ocsInfoManager.isRailDownCheckUsed();
		vehicleCommType = ocsInfoManager.getVehicleCommType();
		
		// Load Data
		loadEcVData();
		loadVidData();		
		loadDynamicReportData();		
		
		// 2012.09.12 by KYK
		loadHostCommandAakDetail();

		// 2013.06.28 by KYK : Alarm 데이터 참조 일원화 (errorCode.txt -> DB:VehicleError)
//		loadAlarmData();
		loadAlarmDataFromDB();
		
		materialAssignAllowedList = materialControlManager.getMaterialAssignAllowedList();
		sourceDestAssignAllowedList = materialControlManager.getSourceDestAssignAllowedList();
	}

	/**
	 * 2012.09.12 by KYK : HCACK.xml 파일을 읽어들인다. (NAK Code 세분화함)
	 * @return
	 */
	private boolean loadHostCommandAakDetail() {		
		String homePath = System.getProperty(HOMEDIR);
		String fileSeperator = System.getProperty(FILESEPARATOR);
		String fileName = "HCACK.xml";
		String configFile = homePath + fileSeperator + fileName;

		boolean result = false;
		String name;
		String strHcAck;
		int hcAck;

		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		try {
			doc = saxb.build(configFile);			
			Element root = doc.getRootElement();
			List<Element> list = root.getChildren(ITEM);
			for (Element element: list) {
				name = element.getChildTextTrim(NAME);
				strHcAck = element.getChildTextTrim(HCACK);
				if (strHcAck != null && strHcAck.length() > 0) {
					hcAck = Integer.parseInt(strHcAck);				
					// 0 or 4 means Ack(ok) , Only NAK(not ok) case is defined 
					if (hcAck != 0 && hcAck != 4) {
						if (TRCMDID_DUPLICATE.equals(name)) {
							HCACK_TRCMDID_DUPLICATE = hcAck;
						} else if (CARRIERID_DUPLICATE.equals(name)) {
							HCACK_CARRIERID_DUPLICATE = hcAck;
						} else if (UNREGISTERED_SOURCELOC.equals(name)) {
							HCACK_UNREGISTERED_SOURCELOC = hcAck;
						} else if (UNREGISTERED_DESTLOC.equals(name)) {
							HCACK_UNREGISTERED_DESTLOC = hcAck;
						} else if (SOURCELOC_NOT_VEHICLEPORT_IN_DESTCHANGE.equals(name)) {
							HCACK_SOURCELOC_NOT_VEHICLEPORT_IN_DESTCHANGE = hcAck;
						} else if (DIFFERENT_VEHICLEPORT_IN_DESTCHANGE.equals(name)) {
							HCACK_DIFFERENT_VEHICLEPORT_IN_DESTCHANGE = hcAck;
						} else if (SOURCELOC_IS_VEHICLEPORT_IN_NORMAL_TRANSFER.equals(name)) {
							HCACK_SOURCELOC_IS_VEHICLEPORT_IN_NORMAL_TRANSFER = hcAck;
						} else if (TRZONE_NOT_ACCEPTABLE.equals(name)) {
							HCACK_TRZONE_NOT_ACCEPTABLE = hcAck;
						} else if (DIFFERENT_MATERIALTYPE.equals(name)) {
							HCACK_DIFFERENT_MATERIALTYPE = hcAck;
						} 
						// 2013.07.12 by KYK
						else if (UNREGISTERED_SOURCESTATION.equals(name)) {
							HCACK_UNREGISTERED_SOURCESTATION = hcAck;
						} else if (UNREGISTERED_DESTSTATION.equals(name)) {
							HCACK_UNREGISTERED_DESTSTATION = hcAck; 
						} else if (UNREADY_SOURCESTATION.equals(name)) {
							HCACK_UNREADY_SOURCESTATION = hcAck; 
						} else if (UNREADY_DESTSTATION.equals(name)) {
							HCACK_UNREADY_DESTSTATION = hcAck;
						}
						// 2013.10.22 by KYK
						else if (UNREADY_SOURCELOC.equals(name)) {
							HCACK_UNREADY_SOURCELOC = hcAck; 
						} else if (UNREADY_DESTLOC.equals(name)) {
							HCACK_UNREADY_DESTLOC = hcAck;
						}
					}
					System.out.println(name + ":" + hcAck);					
				}
			}
			result = true;
		} catch (JDOMException e) {
			traceException("loadHostCommandAakDetail()", e);
			result = false;
		} catch (IOException e) {
			traceException("loadHostCommandAakDetail()", e);
			result = false;
		} catch (Exception e) {
			traceException("loadHostCommandAakDetail()", e);
			result = false;			
		}
		return result;		
	}

	private String getMdlnFromDB() {
		String tscName = ocsInfoManager.getOCSInfoValue("MDLN");
		if (tscName == null) {
			tscName = "OCS3.0";			
		}
		return tscName;
	}

	private String getSoftVersionFromDB() {
		String version = ocsInfoManager.getOCSInfoValue("SOFTREV");
		if (version == null) {
			version = "v3.0.1.0";
		}
		return version;
	}

	/**
	 * load EquipmentConstant (ESTABLISHCOMMUNICATIONSTIMEOUT, EQPNAME)
	 */
	public boolean loadEcVData() {
		EquipmentConstant equipmentConstantO1 = new EquipmentConstant();
		EquipmentConstant equipmentConstantO2 = new EquipmentConstant();
		int establishCommunicationsTimeout = ocsInfoManager.getEstablishCommunicationsTimeout();
		equipmentConstantO1.setData(2, ESTABLISHCOMMUNICATIONSTIMEOUT, establishCommunicationsTimeout, MIN_ESTCOMM_TIMEOUT, MAX_ESTCOMM_TIMEOUT, DEFAULT_ESTCOMM_TIMEOUT, "s", SECSMSG_TYPE.U2);
		ecIdOTable.put(2, equipmentConstantO1);
		equipmentConstantO2.setData(61, EQPNAME, mdln, "");
		ecIdOTable.put(61, equipmentConstantO2);
		return true;
	}
	
	/**
	 * 2013.06.28 by KYK 
	 * load AlarmData from DB (VehicleError)
	 * @return
	 */
	public boolean loadAlarmDataFromDB() {
		boolean result = false;
		long alarmId;
		AlarmItem alarmItem;
		VehicleError vehicleError;
		ConcurrentHashMap<String, Object> data = vehicleErrorManager.getData();
		try {
			if (alarmIdOTable == null) {
				alarmIdOTable = new HashMap<Long, AlarmItem>();
			}
			if (data != null) {
				for (String errorCode: data.keySet()) {
					vehicleError = (VehicleError) data.get(errorCode);
					alarmId = Long.parseLong(errorCode);
					
					alarmItem = alarmIdOTable.get(alarmId);
					if (alarmItem == null) {
						alarmItem = new AlarmItem();
						alarmIdOTable.put(alarmId, alarmItem);
					}
					alarmItem.setAlarmId(alarmId);
					if (vehicleError != null) {
						alarmItem.setAlarmText(vehicleError.getErrorText());
					}
				}
				// 2013.10.02 by KYK : Reload errorVehicleData from DB (Vehicle)
				currentAlarmList.clear();
				HashMap<String,Vehicle> errorVehicleMap = vehicleManager.getErrorVehicleFromDB();
				Vehicle vehicle;
				long errorCode = 0;
				AlarmItem currAlarm;
				if (errorVehicleMap != null) {
					for (String vehicleId: errorVehicleMap.keySet()) {
						vehicle = errorVehicleMap.get(vehicleId);
						if (vehicle != null) {
							errorCode = vehicle.getErrorCode();
							alarmItem = alarmIdOTable.get(errorCode);
							if (alarmItem != null) {
								// Set Alarm
								alarmItem.setActivated(true);
								alarmItem.setVehicleId(vehicleId);
								alarmItem.addVehicleToList(vehicleId);
								// Add into CurrentAlarmList
								currAlarm = new AlarmItem();
								currAlarm.setAlarmId(errorCode);
								currAlarm.setAlarmText(alarmItem.getAlarmText());
								currAlarm.setEnabled(true);
								currAlarm.setActivated(true);
								currAlarm.setVehicleId(vehicleId);
								currentAlarmList.add(currAlarm);
							}
						}
					}					
				}				
				result = true;				
			}
		} catch (Exception e) {
			traceException("loadAlarmDataFromDB()", e);
			result = false;
		}
		return result;
	}
	
	/**
	 * 2013.06.28 by KYK
	 * @return
	 */
	public boolean enableAlarm() {
		if (isAllAlarmEnabled) {
			return enableAlarmStatus(true);			
		} else {
			return false;
		}
	}

	/**
	 * load AlarmData (ErrorCode.txt)
	 */
	public boolean loadAlarmData() {
		String path = System.getProperty(HOMEDIR);
		String separator = System.getProperty(FILESEPARATOR);		
		String fileName = "ErrorCode.txt";
		String filePathName = path + separator + fileName;
		String lineString = "";
		int lineCount = 0;
		long alarmId;
		String alarmText;
		AlarmItem alarmItem;
		RandomAccessFile raf = null;
		
		try {
			File file = new File(filePathName);
			raf = new RandomAccessFile(file, "r");
			while ((lineString = raf.readLine())!= null) {
				lineCount++;
				lineString = lineString.trim();
				// check Validation
				if (lineString.length() == 0) {
					continue;
				}
				if (lineString.substring(0, 2).equals("//")) {
					continue;
				}
				String[] errors = lineString.split(":", 2);
				if (errors.length != 2) {
					writeSEMLog("Check ErrorCode.txt, at line: " + lineCount);
					return false;
				}
				errors[0] = errors[0].trim();
				if ("E".equalsIgnoreCase(errors[0].substring(0, 1)) == false) {
					writeSEMLog("Check ErrorCode.txt, Not Defined ErrorCode at line :" + lineCount);
					return false;						
				}
				// Normal case
				alarmId = Long.parseLong(errors[0].substring(1));
				alarmText = errors[1].trim();
				
				alarmItem = new AlarmItem();
				alarmItem.setAlarmId(alarmId);
				alarmItem.setAlarmText(new String(alarmText.getBytes("ISO-8859-1"), "EUC-KR")); // 한글깨짐현상 때문
				alarmIdOTable.put(alarmId, alarmItem);
			}					
		} catch (FileNotFoundException e) {
			writeSEMLog("Not Found ErrorCode.txt");
			traceException("", e);
			return false;	
		} catch (IOException e) {
			traceException("", e);
			return false;	
		} finally {
			try	{
				if (raf != null) {
					raf.close();
				}
			}catch (Exception e) {
				traceException("", e);
            }
		}
		return true;
	}
	
	/**
	 * load VID data
	 */
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
		String fileName = "VID.xml";
		String configFile = homePath + fileSeparator + fileName;
		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		
		try {
			doc = saxb.build(configFile);
			
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
					// testCode
					System.out.println("VID,NAME (" + vid + "," + name + ")");
				}
			}			
		} catch (JDOMException e) {
			traceException("loadVidDataFromXml()", e);
			return false;
		} catch (IOException e) {
			traceException("loadVidDataFromXml()", e);
			return false;
		} catch (Exception e) {
			traceException("loadVidDataFromXml()", e);
			return false;			
		}
		return true;
	}

	/**
	 * ReportData (CollectionEvent,Report) 를 읽어온다.
	 */
	public void loadDynamicReportData() {
		// Report (REPORTID,VIDS)
		loadEventReportData();

		// CollectionEvent (CEID,NAME,RPIDS,ENABLED)
		loadCollectionEventData();		
	}
	
	public boolean loadEventReportData() {
		return loadEventReportDataFromXml();
	}
	
	/**
	 * Report Data (REPORTID, VIDS) 를 xml file 로 부터 읽어온다.
	 */
	public boolean loadEventReportDataFromXml() {
		int reportId;		
		String tempReportId;
		String vIds;
		Element reportItem;
		String homePath = System.getProperty(OcsConstant.HOMEDIR);
		String fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		String fileName = "Report.xml";
		String configFile = homePath + fileSeparator + fileName;
		
		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		
		try {
			doc = saxb.build(configFile);
			
			Element reportElement = doc.getRootElement();
			List<Element> list = reportElement.getChildren(REPORTITEM);
			Iterator<Element> iter = list.iterator();		
			
			while (iter.hasNext()) {
				reportItem = (Element) iter.next();
				tempReportId = reportItem.getChildTextTrim(REPORTID);
				reportId = Integer.parseInt(tempReportId);			
				vIds = reportItem.getChildTextTrim(VID);
				
				if (tempReportId != null && vIds != null) {
					EventReport eventReport = new EventReport();
					eventReport.setReportId(reportId);
					
					int vIdCount = 0;
					if (vIds.length() > 0) {
						String[] saVid = vIds.split(",");
						vIdCount = saVid.length;
						for (int i = 0; i < vIdCount; i++) {
							eventReport.setVid(i, Integer.parseInt(saVid[i]));
						}
					}
					eventReport.setVidQty(vIdCount);
					reportIdOTable.put(reportId, eventReport);				
				}
			}		
		} catch (JDOMException e) {
			traceException("loadEventReportDataFromXml()", e);
			return false;
		} catch (IOException e) {
			traceException("loadEventReportDataFromXml()", e);
			return false;
		} catch (Exception e) {
			traceException("loadEventReportDataFromXml()", e);
			return false;			
		}
		return true;
	}

	public boolean loadCollectionEventData() {
		return loadCollectionEventDataFromXml();
	}
	
	/**
	 * XML파일에서 CollectionEvent data 를 읽어온다. 
	 * @return
	 */
	public boolean loadCollectionEventDataFromXml() {
		int collectionEventId;
		String tempCollectionEventId;
		String eventName;
		String reportIds;
		boolean isEnabled;
//		String enabled;
		Element ceIdItem;
		
		String homePath = System.getProperty(HOMEDIR);
		String fileSeparator = System.getProperty("file.separator");
		String fileName = "CollectionEvent.xml";
		String configFile = homePath + fileSeparator + fileName;

		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		
		try {
			doc = saxb.build(configFile);
			
			Element ceIdElement = doc.getRootElement();
			List<Element> list = ceIdElement.getChildren(ITEM);
			Iterator<Element> iter = list.iterator();
			
			while (iter.hasNext()) {
				ceIdItem = (Element) iter.next();
				tempCollectionEventId = ceIdItem.getChildTextTrim(CEID);
				collectionEventId = Integer.parseInt(tempCollectionEventId);			
				eventName = ceIdItem.getChildTextTrim(NAME);
				reportIds = ceIdItem.getChildTextTrim(REPORTID);
				isEnabled = (TRUE.equals(ceIdItem.getChildTextTrim(ENABLED))) ? true : false;
				
				if (tempCollectionEventId != null && reportIds != null) {
					CollectionEvent collectionEvent = new CollectionEvent();
					collectionEvent.setCollectionEventId(collectionEventId);
					collectionEvent.setEventName(eventName);
					collectionEvent.setEnabled(isEnabled);
					
					int reportIdCount = 0;
					if (reportIds.length() > 0) {
						String[] saReportId = reportIds.split(",");
						reportIdCount = saReportId.length;
						for (int i = 0; i < reportIdCount; i++) {
							collectionEvent.setReportIdAt(i, Integer.parseInt(saReportId[i]));
						}
					}
					collectionEvent.setReportIdQty(reportIdCount);
					ceIdOTable.put(new Integer(collectionEventId), collectionEvent);
					eventNameCeIdTable.put(eventName, new Integer(collectionEventId));
				}
			}					
		} catch (JDOMException e) {
			traceException("loadCollectionEventDataFromXml()", e);
			return false;
		} catch (IOException e) {
			traceException("loadCollectionEventDataFromXml()", e);
			return false;
		} catch (Exception e) {
			traceException("loadCollectionEventDataFromXml()", e);
			return false;			
		}
		return true;
	}
	
	/**********************************************
	 * Communication Scenarios (between MCS and OCS)
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
	 * MCS -> OCS	: S1F3	[VID]=6 Report ControlState
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=73 Report TSCState
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=114 Report SpecVersion
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=91 Report EnhancedCarriers
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=76 Report EnhancedTransfers
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=53 Report ActiveVehicles
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=118 Report CurrentPortState
	 * MCS <- OCS	: S1F4 reply Ack
	 * MCS -> OCS	: S1F3	[VID]=4 Report AlarmsSet
	 * MCS <- OCS	: S1F4 reply Ack
	 *  
	 *
	 **/
	
	/********************************************
	 * @Override SPEC Used in IBSEM (Except Not Used)
	 * STREAM 1 EQUIPMENT STATE
	 * @param message
	 ********************************************/	

	public void receiveS1Fy(UComMsg message) {
		int function = message.getFunction();
		switch (function) {
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
			case 13:
				receiveS1F13(message);
				break;
			case 14:
				receiveS1F14(message);
				break;
			case 15:
				receiveS1F15(message);
				break;
			case 17:
				receiveS1F17(message);
				break;
			default:
				//Abnormal Case
				break;
		}
	}

	/********************************************
	 * @Override SPEC Used in IBSEM (Except Not Used)
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
			case 15:
				receiveS2F15(message);
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
			case 31:
				receiveS2F31(message);
				break;
			case 33:
				receiveS2F33(message);
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
			case 41:
				receiveS2F41(message);
				break;
			case 49:
				receiveS2F49(message);
				break;
			default:
				//Abnormal Case
				break;
		}
	}

	/********************************************
	 * @Override SPEC Used in IBSEM (Except Not Used)
	 * STREAM 5 EXCEPTION REPORTING
	 * @param message
	 ********************************************/
	public void receiveS5Fy(UComMsg message) {
		int function = message.getFunction();
		switch (function) {
			case 3:
				receiveS5F3(message);
				break;
			case 5:
				receiveS5F5(message);
				break;
			default:
				//Abnormal Case
				break;
		}
	}

	/********************************************
	 * @Override SPEC Used in IBSEM (Except Not Used)
	 * STREAM 6 DATA COLLECTION
	 * @param message
	 ********************************************/
	public void receiveS6Fy(UComMsg message) {
		int function = message.getFunction();
		switch (function) {
			case 12:
				receiveS6F12(message);
				break;
			case 15:
				receiveS6F15(message);
				break;
			default:
				//Abnormal Case
				break;
		}
	}
	
	/**********************************************
	 * @Override SPEC Used in IBSEM (Except Not Used)
	 * STREAM 9 SYSTEM ERRORS
	 * @param message
	 ********************************************/
	public void receiveS9Fy(UComMsg message) {		
		// Do nothing
	}
		
	/**
	 * S2F41 : Host Command Send (CANCEL,ABORT,STAGEDELETE,PAUSE,RESUME)
	 * HCACK    0 = confirmed, the command was executed 
	 * (The transport system doesn't use this value. Confirmation is made using the number 4 value.)
	 *          1 = command doesn't exist
	 *          2 = currently not able to execute
	 *          3 = at least one parameter isn't valid         
	 *          4 = confirmed, the command will be executed and completion will be notified by an event         
	 *          5 = rejected, already requested         
	 *          6 = object doesn't exist
	 *          7-63 = hold         
	 * CPACK    1 = CPNAME doesn't exist
	 *          2 = the incorrect value is specified in CPVAL 
	 *          3 = the incorrect format is specified in CPVAL                  
	 *          >4 = another equipment error
	 * @param message
	 */
	public void receiveS2F41(UComMsg message) {
		int hostCommandAck = 4; // HCACK
		int itemCount = message.getListItem();
		String remoteCmd = message.getAsciiItem();
		
		String strLog = "[RCV S2F41] RCMD:" + remoteCmd;
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 41, strLog);	
		
		if (controlStatus.equals(REMOTE_ONLINE) == false) {
			hostCommandAck = 2; // HCACK 2 : currently not able to execute			
			strLog = "[RCV S2F41] CONTROLStatus is not Online/HCACK="+ hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			sendS2F42(message, hostCommandAck);
			return;
		} else {
			if (CANCEL.equals(remoteCmd)) {
				receiveCANCELCommand(message);
			} else if (ABORT.equals(remoteCmd)) {
				receiveABORTCommand(message);
			} else if (STAGEDELETE.equals(remoteCmd)) {
				receiveSTAGEDELETEcommand(message);
			} else if (PAUSE.equals(remoteCmd)) {
				receivePAUSECommand(message);
			} else if (RESUME.equals(remoteCmd)) {
				receiveRESUMECommand(message);
			} else if (TRANSFERUPDATE.equals(remoteCmd)) {
				receiveTRANSFERUPDATECommand(message);
			} else {
				// Error - Undefined Remote (Host) Command..
				hostCommandAck = 1; // command doesn't exist	
				strLog = "[RCV S2F41] Undefined RemoteCmd, RCMD: " + remoteCmd;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck);				
			}
			return;
		}				
	}

	/**
	 * S2F41 : ABORT command
	 * @param message
	 */
	public void receiveABORTCommand(UComMsg message) {
		int itemCount = 0;
		int commandParameterAck = 0;
		int hostCommandAck = 4;
		String str;
		String strLog;
		String commandId;
		String remoteCmd = ABORT;
		
		itemCount = message.getListItem(); // L, n
		itemCount = message.getListItem(); // L, 2
		str = message.getAsciiItem(); //  CPNAME : "COMMANDID"
		if (COMMANDID.equals(str) == false) {
			hostCommandAck = 3;
			
			// 2011.11.18 by PMM
			commandParameterAck = 2;
			
			strLog = "[RCV S2F41] Invalid CPNAME";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			sendS2F42(message, hostCommandAck, commandParameterAck);
			return;
		} else {
			commandId = message.getAsciiItem();
			writeSEMLog("          [COMMANDID]:" + commandId);

			// 2013.07.01 by KYK
//			if (isTrCmdIdDuplicated(commandId, remoteCmd)) {
			if (isTrCmdIdDuplicated(commandId, remoteCmd, null)) {
				hostCommandAck = 5;
				commandParameterAck = 2;
				strLog = "[RCV S2F41] Cmd has already registered. Invalid CommandId:" + commandId;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck, commandParameterAck);
				return;
			} else {
				// Normal Case
				if (isAbleToABORTCmd(commandId)) {
					setAbortCommandToDB(commandId);
				} else {
					hostCommandAck = 2;	
					commandParameterAck = 2;
					strLog = "[RCV S2F41] fail to execute ABORT Command - Invalid Command Status/COMMANDID:" + commandId;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					sendS2F42(message, hostCommandAck, commandParameterAck);
					return;
				}
			}
		}
		sendS2F42(message, hostCommandAck, commandParameterAck);
	}
	
	/**
	 * S2F41 : CANCEL command
	 * @param message
	 */
	public void receiveCANCELCommand(UComMsg message) {
		String strLog;
		int itemCount = 0;
		int commandParameterAck = 0;		//CPACK
		int hostCommandAck = 4;				//HCACK
		String commandParameterName;		//CPNAME
		String commandId;
		String remoteCmd = CANCEL;
				
		itemCount = message.getListItem(); // L, n
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); //  CPNAME : "COMMANDID"
		if (COMMANDID.equals(commandParameterName) == false) {
			hostCommandAck = 3;
			
			// 2011.11.18 by PMM
			commandParameterAck = 2;
			
			strLog = "[RCV S2F41] Invalid CPNAME";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			sendS2F42(message, hostCommandAck, commandParameterAck);
			return;
		} else {
			commandId = message.getAsciiItem();
			writeSEMLog("          [COMMANDID]:" + commandId);
			
			// 2013.07.01 by KYK
//			if (isTrCmdIdDuplicated(commandId, remoteCmd)) {
			if (isTrCmdIdDuplicated(commandId, remoteCmd, null)) {
				hostCommandAck = 5;
				commandParameterAck = 2;
				strLog = "[RCV S2F41] Cmd has already registered. Invalid CommandId:" + commandId;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck, commandParameterAck);
				return;
			} else {	
				// Normal Case
				if (isAbleToCANCELCmd(commandId)) {					
					setCancelCommandToDB(commandId);
				} else {
					hostCommandAck = 2;
					commandParameterAck = 2;
					strLog = "[RCV S2F41] fail to execute CANCEL Command - Invalid Command Status/COMMANDID:" + commandId;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					sendS2F42(message, hostCommandAck, commandParameterAck);
					return;
				}
			}
		}		
		sendS2F42(message, hostCommandAck, commandParameterAck);
	}

	/**
	 * S2F42 : Reply for S2F41
	 */
	private void sendS2F42(UComMsg message, int hostCommandAck, int commandParameterAck) {
		UComMsg response = uCom.makeSecsMsg(2, 42, message.getSysbytes());
		response.setListItem(2); 							// L, 2
		response.setBinaryItem(hostCommandAck); 			// HCACK
		
		if (hostCommandAck == 4) {
			response.setListItem(0);			
		} else {
			response.setListItem(1);						// L, n
			response.setListItem(2);						// L, 2
			response.setAsciiItem(COMMANDID);				// CPNAME
			response.setBinaryItem(commandParameterAck);	// CPACK			
		}
		sendUComMsg(response, "S2F42");		
		
		StringBuilder sb = new StringBuilder();
		sb.append("[SND S2F42] HCACK=").append(hostCommandAck).append(" /CPACK=").append(commandParameterAck);
		writeSEMLog(sb.toString());
		updateSEMHistory(SENT, 2, 42, sb.toString());
	}

	/**
	 * S2F42 : Reply for S2F41
	 * @param message
	 * @param hostCommandAck
	 */
	private void sendS2F42(UComMsg message, int hostCommandAck) {
		UComMsg response = uCom.makeSecsMsg(2, 42, message.getSysbytes());
		response.setListItem(2); // L, 2
		response.setBinaryItem(hostCommandAck); // HCACK
		response.setListItem(0);

		sendUComMsg(response, "S2F42");
		
		String strLog = "[SND S2F42] HCACK=" + hostCommandAck;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 42, strLog);		
	}

	/**
	 * S2F49 : Enhanced Remote Command (TRANSFER,STAGE,SCAN)
	 * HCACK    0 = confirmed, the command was executed 
	 * (The transport system doesn't use this value.Confirmation is made using the number 4 value.)
	 *          1 = command doesn't exist
	 *          2 = currently not able to execute
	 *          3 = at least one parameter isn't valid
	 *          4 = confirmed, the command will be executed and completion will be notified by an event
	 *          5 = rejected, already requested                  
	 *          6 = object doesn't exist                 
	 *          7-63 = hold         
	 * CEPACK   1 = CPNAME doesn't exist
	 *          2 = the incorrect value is specified in CPVAL
	 *          3 = the incorrect format is specified in CPVAL
	 *          4 = the CPNAME usage isn't valid         
	 *          
	 * @param message
	 */
	public void receiveS2F49(UComMsg message) {
		int hostCommandAck = 4;
		
		int itemCount = message.getListItem(); // L, 4
		long dataId = message.getU2Item(); // DATAID = 0 (사양에 맞게 U4 -> U2 로 바꿈)
		String specifiedObjectInstance = message.getAsciiItem(); // OBJSPEC
		String remoteCmd = message.getAsciiItem();				
		
		String strLog = "[RCV S2F49] RCMD:" + remoteCmd;
		writeSEMLog(strLog);
		updateSEMHistory(RECEIVED, 2, 49, strLog);

		if (controlStatus.equals(REMOTE_ONLINE) == false) {
			hostCommandAck = 2; 				// HCACK 2 : currently not able to execute
			strLog = "[RCV S2F49] CONTROLStatus is not Online/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);	
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, hostCommandAck);
			return;
		} else {
			if (TRANSFER.equals(remoteCmd)) {
				receiveTRANSFERcommand(message);
				return;
			} else if (TRANSFER_EX3.equals(remoteCmd)) {
				// 2014.01.02 by KBS : TRANSFER_EX3 처리 (for A-PJT EDS)
				receiveTRANSFEREX3command(message);
				return;
			}else if (TRANSFER_EX4.equals(remoteCmd)) {
				// 2021.04.02 by JDH : Transfer Premove 사양 추가
				receiveTRANSFEREX4command(message);
				return;
			} else if (STAGE.equals(remoteCmd)) {
				receiveSTAGEcommand(message);
				return;
			} else if (SCAN.equals(remoteCmd)) {
				// 2012.02.10 by PMM
//				if (ocsInfoManager.isIdReaderInstalledOnVehicle()) {
				if (isIdReaderInstalledOnVehicle) {
					receiveSCANcommand(message);					
				} else {
					hostCommandAck = 2; 	// HCACK 2 : currently not able to execute
					strLog = "[RCV S2F49] IDREADER Is Not Installed ON Vehicle.(SCAN Unavailable) /HCACK=" + hostCommandAck;
					writeSEMLog(strLog);	
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, hostCommandAck);
				}
				return;
			} else {
				hostCommandAck = 1; 			// HCACK 1 : command doesn't exist.
				strLog = "[RCV S2F49] Undefined Remote (Host) Command. RCMD :" + remoteCmd;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, hostCommandAck);
				return;
			}			
		}
	}

	/**
	 * S2F50 : Reply for S2F49
	 * @param message
	 * @param hostCommandAck
	 */
	private void sendS2F50(UComMsg message, int hostCommandAck) {
		UComMsg response = uCom.makeSecsMsg(2, 50, message.getSysbytes());
		response.setListItem(2);
		response.setBinaryItem(hostCommandAck);
		// 2012.01.11 by KYK
//		response.setListItem(2);
		response.setListItem(0);
		sendUComMsg(response, "S2F50");
		
		String strLog = "[SND S2F50] HCACK=" + hostCommandAck;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 2, 50, strLog);
	}

	/**
	 * S2F50 : Reply for S2F49
	 * @param message
	 * @param remoteCmd
	 * @param hostCommandAck
	 * @param commandExtensionParameterAck1
	 * @param commandExtensionParameterAck2
	 */
	private void sendS2F50(UComMsg message, TRCMD_REMOTECMD remoteCmd, int hostCommandAck, int commandExtensionParameterAck1, int commandExtensionParameterAck2) {
		UComMsg response = uCom.makeSecsMsg(2, 50, message.getSysbytes());
		response.setListItem(2);
		response.setBinaryItem(hostCommandAck);
		
		if (hostCommandAck == 4) {
			response.setListItem(0);
		} else {
//			response.setListItem(2);			
			if (commandExtensionParameterAck1 == 0 && commandExtensionParameterAck2 == 0) {
				response.setListItem(0);
			} else {
				response.setListItem(2);
				switch (remoteCmd) {
					case TRANSFER:
					case PREMOVE:	// 2021.04.02 by JDH : Transfer Premove 사양 추가
						response.setListItem(2);			
						response.setAsciiItem(COMMANDINFO); // CPNAME
						response.setBinaryItem(commandExtensionParameterAck1); // CEPACK1				
						response.setListItem(2);			
						response.setAsciiItem(TRANSFERINFO); // CPNAME
						response.setBinaryItem(commandExtensionParameterAck2); // CEPACK2	
						break;
					case STAGE:
						response.setListItem(2);			
						response.setAsciiItem(STAGEINFO); // CPNAME
						response.setBinaryItem(commandExtensionParameterAck1); // CEPACK1				
						response.setListItem(2);			
						response.setAsciiItem(TRANSFERINFO); // CPNAME
						response.setBinaryItem(commandExtensionParameterAck2); // CEPACK2	
						break;
					case SCAN:
						response.setListItem(2);			
						response.setAsciiItem(COMMANDINFO); // CPNAME
						response.setBinaryItem(commandExtensionParameterAck1); // CEPACK1				
						response.setListItem(2);			
						response.setAsciiItem(SCANINFO); // CPNAME
						response.setBinaryItem(commandExtensionParameterAck2); // CEPACK2
						break;
					default:
						break;
				}
			}
		}
		sendUComMsg(response, "S2F50");		
		
		StringBuilder sb = new StringBuilder();
		sb.append("[SND S2F50] HCACK=").append(hostCommandAck).append(" CPACK1=");
		sb.append(commandExtensionParameterAck1).append(" CPACK2=").append(commandExtensionParameterAck2);
		writeSEMLog(sb.toString());
		updateSEMHistory(SENT, 2, 50, sb.toString());

		return;		
	}

	public void setCancelCommandToDB(String trCmdId) {
		// TRSTATE 상 CANCEL 가능함
//		trCmdManager.updateTrCmdStatusToDB(trCmdId, CMD_CANCELLING, null, CANCEL, null, null, -1, true);
		trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, CANCEL, "");
	}


	public void setAbortCommandToDB(String trCmdId) {
		// TRSTATE 상 ABORT 가능함
//		trCmdManager.updateTrCmdStatusToDB(trCmdId, CMD_ABORTING, null, ABORT, null, null, -1, true);
		trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, ABORT, "");
	}

	/**
	* @author : Jongwon Jung
	* @date : 2021. 1. 29.
	* @description : Update Command DB Set
	* @param trCmdId
	* @param registeredTrCmd
	* @param trcmd
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 1. 29. Jongwon 최초 생성 */
	public void setTransferUpdateCommandToDB(String trCmdId, TrCmd registeredTrCmd, TrCmd trcmd) {
		// TRSTATE 상 UPDATE 가능함
		trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, TRANSFERUPDATE, trcmd);
		// 2021.08.10 by JJW 기존 데이터를 Old 데이터로 DB 저장
		trCmdManager.updateOldTrCmdChangedInfoToDB(trCmdId, trcmd);
		
		String updateType = transferUpdateType(registeredTrCmd,trcmd);
		
		if(updateType.equals(PRIORITY)){
			writeSEMLog("UPDATE TrCmd SET CHANGEDREMOTECMD='TRANSFERUPDATE', PRIORITY='" + trcmd.getPriority() + "' WHERE TRCMDID='" + trCmdId + "'");
		} else if(updateType.equals(DESTPORT)){
			writeSEMLog("UPDATE TrCmd SET CHANGEDREMOTECMD='TRANSFERUPDATE', DESTLOC='" + trcmd.getDestLoc() + "' WHERE TRCMDID='" + trCmdId + "'");
		} else if(updateType.equals(CARRIERID)){
			writeSEMLog("UPDATE TrCmd SET CHANGEDREMOTECMD='TRANSFERUPDATE', CARRIERID='" + trcmd.getCarrierId() + "' WHERE TRCMDID='" + trCmdId + "'");
		} else if(updateType.equals("PRIORITYDEST")){
			writeSEMLog("UPDATE TrCmd SET CHANGEDREMOTECMD='TRANSFERUPDATE', PRIORITY='" + trcmd.getPriority() + "', DESTLOC='"
		    + trcmd.getDestLoc() + "' WHERE TRCMDID='" + trCmdId + "'");
		} else{
			writeSEMLog("UPDATE TrCmd SET CHANGEDREMOTECMD='TRANSFERUPDATE' WHERE TRCMDID='" + trCmdId + "'");
		}
	}
	
	private String transferUpdateType(TrCmd registeredTrCmd, TrCmd trcmd)
	{
		String updateType = "default";
		
		if(trcmd.getDestLoc().equals(registeredTrCmd.getDestLoc()) && trcmd.getCarrierId().equals(registeredTrCmd.getCarrierId())
			&& trcmd.getPriority() != registeredTrCmd.getPriority()){
			updateType = "PRIORITY";
			return updateType;
		} else if (!trcmd.getDestLoc().equals(registeredTrCmd.getDestLoc())	&& trcmd.getCarrierId().equals(registeredTrCmd.getCarrierId())
				&& trcmd.getPriority() == registeredTrCmd.getPriority()){
			updateType = "DESTPORT";
			return updateType;
		} else if (trcmd.getDestLoc().equals(registeredTrCmd.getDestLoc()) && !trcmd.getCarrierId().equals(registeredTrCmd.getCarrierId())
				&& trcmd.getPriority() == registeredTrCmd.getPriority()){
			updateType = "CARRIERID";
			return updateType;
		}  else if (!trcmd.getDestLoc().equals(registeredTrCmd.getDestLoc()) && trcmd.getCarrierId().equals(registeredTrCmd.getCarrierId())
				&& trcmd.getPriority() != registeredTrCmd.getPriority()){
			updateType = "PRIORITYDEST";
			return updateType;
		}  else{
			return updateType;
		}
		
	}
	/**
	 * S2F49 : SCAN
	 * @param message
	 */
	public void receiveSCANcommand(UComMsg message) {
		int hostCommandAck = 4;
		int itemCount;
		int priority = 0;
		
		//cEPACK1
		int commandExtensionParameterAck1 = 0;
		//cEPACK2
		int commandExtensionParameterAck2 = 0;
		String commandParameterName;
		String strLog;
		String commandId = null;
		String carrierId = null;
		String carrierLoc = null;
		TRCMD_REMOTECMD remoteCmd = TRCMD_REMOTECMD.SCAN;
		TrCmd trCmd = new TrCmd();	
		trCmd.setRemoteCmd(remoteCmd);

		itemCount = message.getListItem(); // L, 2 [CommandInfo, ScanInfo]	
		if (itemCount != 2) {
			hostCommandAck = 3; // at least one parameter isn't valid
			
			// 2011.11.18 by KYK
			commandExtensionParameterAck1 = 3;
			
			strLog = "[RCV S2F49] Invalid CPVAL (Carrier Qty : " + itemCount;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}		
		// Get 'CommandInfo' data from S2F49 
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME1 : "COMMANDIDINFO"
		itemCount = message.getListItem(); // L, 2 : CommandID, Priority
		
		if (COMMANDINFO.equals(commandParameterName) && itemCount == 2) {
			
			// Get commandId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'COMMANDID'
			if (COMMANDID.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				commandId = message.getAsciiItem();
				trCmd.setTrCmdId(commandId);
				
				// Check condition : TrCmd Duplicate 
				// 2013.07.01 by KYK
//				if (isTrCmdIdDuplicated(commandId)) {
				if (isTrCmdIdDuplicated(commandId, null, null)) {
					hostCommandAck = 3;
					commandExtensionParameterAck1 = 2;
					strLog = "[RCV S2F49] Invalid CommandID - Already registered ID=" + commandId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Priority 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'PRIORITY'
			if (PRIORITY.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				priority = message.getU2Item();
				trCmd.setPriority(priority);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid PRIORITY:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
		} else { // !(str.equals("COMMANDINFO") && itemCnt == 2)
			hostCommandAck = 3;
			
			// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
			//commandExtensionParameterAck1 = 2;
			
			strLog = "[RCV S2F49] Invalid CommandInfo:" + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}		

		// Get 'ScanInfo' data from S2F49 
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME2 "SCANINFO"
		itemCount = message.getListItem(); // L, 2 : carrierId,carrierLoc
		if (SCANINFO.equals(commandParameterName) && itemCount == 2) {
			// Get carrierId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'CARRIERID'			
			if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
				carrierId = message.getAsciiItem();
				trCmd.setCarrierId(carrierId);
				
				// Check condition : Carrier Duplicate 
				if (isScanCarrierIdDuplicated(carrierId)) {
					hostCommandAck = 3;
					
					// 2011.11.18 by PMM
//					commandExtensionParameterAck1 = 2;
					commandExtensionParameterAck2 = 2;
					strLog = "[RCV S2F49] Invalid CarrierID or Invalid TrCmdStatus/CarrierID=" + carrierId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid CARRIERID:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
			
			// Get carrierLoc : Scan Location 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'SOURCEPORT'
			if (CARRIERLOC.equals(commandParameterName) && itemCount == 2) {
				carrierLoc = message.getAsciiItem(); // CPVAL : A SourcePort
				trCmd.setSourceLoc(carrierLoc);
				trCmd.setDestLoc(carrierLoc);
				if (isCarrierLocAvailable(carrierLoc) == false) {
					hostCommandAck = 3; // at least one parameter isn't valid
					commandExtensionParameterAck2 = 2;	
					strLog = "[RCV S2F49] Invalid SourcePort=" + carrierLoc + "- Doesn't exist /HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				} else if (getPortType(carrierLoc) == CARRIERLOC_TYPE.VEHICLEPORT) {
					// Abnormal Check : Only In DestChange, sourcePortType == VEHICLEPORT 
					hostCommandAck = 3; // at least one parameter isn't valid
					commandExtensionParameterAck2 = 2;		
					strLog = "[RCV S2F49] Invalid SourcePort=" + carrierLoc + " is VEHICLEPORT. [CarrierID:" + carrierId + "] /HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;			
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid SOURCEPORT Info" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
		}

		// Register SCAN Command to DB 
		if (hostCommandAck == 4 && commandExtensionParameterAck1 == 0 && commandExtensionParameterAck2 == 0) {
			trCmd.setState(TRCMD_STATE.CMD_QUEUED);
			trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
			
			StringBuilder sb = new StringBuilder();
			if (createScanJobInfo(trCmd)) {
			// newTr : (rCmd, commandId, priority,carrierId, carrierLoc, trCmdStatus,detailTrCmdStatus)
				sb.append("Scan [");
			} else {		
				// 2013.07.01 by KYK
				hostCommandAck = 2;
				sb.append("Fail:").append(remoteCmd).append("[");
			}
			sb.append(carrierId).append("/");
			sb.append(commandId).append("/");
			sb.append(carrierLoc).append("]");
			writeSEMLog(sb.toString());
			updateSEMHistory(RECEIVED, 2, 49, sb.toString());
		}	
		sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
	}

	/**
	 * create SCAN command
	 * @param trCmd
	 * @return
	 */
	private boolean createScanJobInfo(TrCmd trCmd) {
		String sourceLocId = trCmd.getSourceLoc();
		CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(sourceLocId);
		String sourceNode = "";
		if (sourceLoc != null) {
			sourceNode = sourceLoc.getNode();
		}
		trCmd.setSourceNode(sourceNode);
		trCmd.setDestNode(sourceNode);
		
		if (sourceNode != null && sourceNode.length() == 0) {
			StringBuilder sb = new StringBuilder("Fail createScanJobInfo()-SourceNode is Null. ");
			sb.append("sourceLoc=").append(sourceLocId);
			sb.append("sourceNode=").append(sourceNode);
			writeSEMLog(sb.toString());
			return false;
		}						
		return trCmdManager.createSCANCmdToDB(trCmd);
	}

	/**
	 * S2F41 : STAGE command
	 * @param message
	 */
	public void receiveSTAGEcommand(UComMsg message) {
		int hostCommandAck = 4;
		int itemCount;
		int priority = 0;
		int replace = 0;
		int expectedDuration = 0;
		long noBlockingTime = 0;
		int waitTimeout = 0;
		
		//CEPACK1
		int commandExtensionParameterAck1 = 0;
		//CEPACK2
		int commandExtensionParameterAck2 = 0;
		String commandParameterName;
		String strLog;
		String trCmdId = null;
		String carrierId = null;
		String sourceLoc = null;
		String destLoc = null;
		//String rCmd = "STAGE";
		TRCMD_REMOTECMD remoteCmd = TRCMD_REMOTECMD.STAGE;
		
		TrCmd trCmd = new TrCmd();	
		trCmd.setRemoteCmd(remoteCmd);

		itemCount = message.getListItem(); // L, 2 [StageInfo, TransferInfo]	
		if (itemCount != 2) {
			hostCommandAck = 3; // at least one parameter isn't valid
			
			// 2011.11.18 by KYK
			commandExtensionParameterAck1 = 3;
			
			strLog = "[RCV S2F49] Invalid CPVAL (Carrier Qty : " + itemCount;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}
		
		// Get 'StageInfo' data from S2F49 
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME1 : "STAGEINFO"
		itemCount = message.getListItem(); // L, 6 : StageId, Priority, Replace, ExpectedDuration, NoBlockingTime, WaitTimeout
		
		if (STAGEINFO.equals(commandParameterName) && itemCount == 6) {

			// Get StageId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'STAGEID'
			if (STAGEID.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				trCmdId = message.getAsciiItem();
				trCmd.setTrCmdId(trCmdId);
				
				// Check condition : TrCmd Duplicate 
				// 2013.07.01 by KYK
//				if (isTrCmdIdDuplicated(trCmdId)) {
				if (isTrCmdIdDuplicated(trCmdId, null, null)) {
					hostCommandAck = 3;
					commandExtensionParameterAck1 = 2;
					strLog = "[RCV S2F49] Invalid CommandID - Already registered ID=" + trCmdId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				} else if (trCmdId != null && trCmdId.length() == 0) {
					hostCommandAck = 3;
					commandExtensionParameterAck1 = 2;
					strLog = "[RCV S2F49] Invalid CommandID - StageId is null. ID=" + trCmdId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Priority 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'PRIORITY'
			if (PRIORITY.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				priority = message.getU2Item();
				trCmd.setPriority(priority);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid PRIORITY:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Replace
			itemCount = message.getListItem();
			commandParameterName = message.getAsciiItem();
			if (REPLACE.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				replace = message.getU2Item();
				trCmd.setReplace(replace);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid REPLACE:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}

			// Get ExpectedDuration
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'EXPECTEDDURATION'
			if (EXPECTEDDURATION.equals(commandParameterName) && itemCount == 2) {
				expectedDuration = message.getU2Item(); // CPVAL : U2 ExpectedDuration
				trCmd.setExpectedDuration(expectedDuration);
			} else {
				hostCommandAck = 3; // at least one parameter isn't valid
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid ExpectedDuration Info=" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}

			// Get NoBlockingTime
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'NOBLOCKINGTIME'
			if (NOBLOCKINGTIME.equals(commandParameterName) && itemCount == 2) {
				noBlockingTime = message.getU2Item(); // CPVAL : U2 NoBlockingTime
				trCmd.setNoBlockingTime(noBlockingTime);
			} else {
				hostCommandAck = 3; // at least one parameter isn't valid
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid NoBlockingTime Info=" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}

			// Get WaitTimeout
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'WAITTIMEOUT'
			if (WAITTIMEOUT.equals(commandParameterName) && itemCount == 2) {
				waitTimeout = message.getU2Item(); // CPVAL : U2 WaitTimeout
				trCmd.setWaitTimeout(waitTimeout);
			} else {
				hostCommandAck = 3; // at least one parameter isn't valid
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid WaitTimeout Info=" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
	
			// 2011.11.18 by PMM
//			if (expectedDuration == 0 && noBlockingTime == 0 && waitTimeout == 0) {
			if (expectedDuration == 0) {
				hostCommandAck = 3; // at least one parameter isn't valid
				
				// 2011.11.18 by PMM
				commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid Parameters. ExpectedDuration=" + expectedDuration + ", NoBlockingTime=" + noBlockingTime + " and WaitTimeout=" + waitTimeout + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
		} else { // !(str.equals("STAGEINFO") && itemCnt == 6)
			hostCommandAck = 3;
			
			// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
			//commandExtensionParameterAck1 = 2;
			
			strLog = "[RCV S2F49] Invalid StageInfo:" + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}		
	
		// Get 'TransferInfo' data from S2F49 
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME : 'TRANSFERINFO'
		itemCount = message.getListItem(); // L, 3 : CarrierID, SourcePort, DestPort
		
		if (TRANSFERINFO.equals(commandParameterName) && itemCount == 3) {
			// Get carrierId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'CARRIERID'			
			if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
				carrierId = message.getAsciiItem();
				trCmd.setCarrierId(carrierId);
				
				// Check condition : Carrier Duplicate 
				if (isStageCarrierIdDuplicated(carrierId)) {
					hostCommandAck = 3;
					
					// 2011.11.18 by PMM
//					commandExtensionParameterAck1 = 2;
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] Invalid CarrierID or Invalid TrCmdStatus/CarrierID=" + carrierId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid CARRIERID:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
			
			// Get sourcePort 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'SOURCEPORT'
			if (SOURCEPORT.equals(commandParameterName) && itemCount == 2) {
				sourceLoc = message.getAsciiItem(); // CPVAL : A SourcePort
				trCmd.setSourceLoc(sourceLoc);
				
				if (isCarrierLocAvailable(sourceLoc) == false) {
					hostCommandAck = 3; // at least one parameter isn't valid
					commandExtensionParameterAck2 = 2;	
					strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc + "- Doesn't exist /HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				} else if (getPortType(sourceLoc) == CARRIERLOC_TYPE.VEHICLEPORT) {
					// Abnormal Check : Only In DestChange, sourcePortType == VEHICLEPORT 
					hostCommandAck = 3; // at least one parameter isn't valid
					commandExtensionParameterAck2 = 2;	
					strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc + " is VEHICLEPORT. [CarrierID:" + carrierId + "] /HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;			
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid SOURCEPORT Info" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}

			// Get destPort
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'DESTPORT'
			if (DESTPORT.equals(commandParameterName) && itemCount == 2) {
				destLoc = message.getAsciiItem(); // CPVAL : A DestPort
				trCmd.setDestLoc(destLoc);			
				// STAGE 명령 destPort = '' 임
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid DESTPORT Info=" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
		}
		// STAGE : TRCMDID = CARRIERID 로 부여함
		if (trCmdId == null || trCmdId.equals(carrierId) == false) {
			hostCommandAck = 3;
			
			// 2011.11.18 by PMM
			commandExtensionParameterAck2 = 2;
			
			strLog = "[RCV S2F49] Invalid STAGEID, stageId is different from carrierId /HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;			
		}
		
		// Register STAGE Command to DB 
		if (hostCommandAck == 4 && commandExtensionParameterAck1 == 0 && commandExtensionParameterAck2 == 0) {
			trCmd.setState(TRCMD_STATE.CMD_QUEUED);
			trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
			StringBuilder sb = new StringBuilder();
			if (createStageJobInfo(trCmd)) {
			// newTr : (rCmd, commandId, priority,replace,expectedDuration, noBlockingTime, waitTimeout,carrierId, sourcePort, destPort, trCmdStatus,detailTrCmdStatus)
				sb.append("Stage [");
			} else {						
				// 2013.07.01 by KYK
				hostCommandAck = 2;
				sb.append("Fail:").append(remoteCmd).append("[");
			}
			sb.append(carrierId).append("/");
			sb.append(trCmdId).append("/");
			sb.append(sourceLoc).append("]");
			writeSEMLog(sb.toString());
			updateSEMHistory(RECEIVED, 2, 49, sb.toString());
		}			
		sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
	}

	/**
	 * create STAGE command
	 * @param trCmd
	 * @return
	 */
	private boolean createStageJobInfo(TrCmd trCmd) {
		String sourceLocId = trCmd.getSourceLoc();
		CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(sourceLocId);
		String sourceNode = "";
		if (sourceLoc != null) {
			sourceNode = sourceLoc.getNode();
		}
		String destNode = "0";
		
		trCmd.setSourceNode(sourceNode);
		trCmd.setDestNode(destNode);
		
		if (sourceNode != null && sourceNode.length() == 0) {
			StringBuilder sb = new StringBuilder("Fail createStageJobInfo()-SourceNode is Null. ");
			sb.append("sourceLoc=").append(sourceLocId);
			sb.append("sourceNode=").append(sourceNode);
			writeSEMLog(sb.toString());
			return false;
		}						
		return trCmdManager.createSTAGECmdToDB(trCmd);
	}

	/**
	 * S2F49 : TRANSFER command
	 * @param message
	 */
	public void receiveTRANSFERcommand(UComMsg message) {
		int hostCommandAck = 4;
		int itemCount;
		int priority = 0;
		int replace = 0;
		int commandExtensionParameterAck1 = 0;
		int commandExtensionParameterAck2 = 0;
		String commandParameterName;
		String strLog;
		String trCmdId = null;
		String carrierId = null;
		String sourceLoc = null;
		String destLoc = null;
		//String rCmd = "TRANSFER";
		TrCmd registeredTrCmd = null;		
		TrCmd trCmd = new TrCmd();	
		TRCMD_REMOTECMD remoteCmd = TRCMD_REMOTECMD.TRANSFER;
		trCmd.setRemoteCmd(remoteCmd);

		itemCount = message.getListItem(); // L, 2 [CommandInfo, TransferInfo]	
		if (itemCount != 2 && itemCount != 3) {
			// OCS use the spec. with only one carrier. (nListItemCnt = 3 --> ACS)
			hostCommandAck = 3; // at least one parameter isn't valid
			
			// 2011.11.18 by KYK
			commandExtensionParameterAck1 = 3;
			
			strLog = "[RCV S2F49] Invalid CPVAL ,Carrier Qty : " + itemCount;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);		
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}
		
		/** Get 'CommandInfo' data from S2F49 */
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME1 : "COMMANDIDINFO"
		itemCount = message.getListItem(); // L, 3 : CommandID, Priority, Replace
		
		if (COMMANDINFO.equals(commandParameterName) && itemCount == 3) {
			// Get commandId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'COMMANDID'
			if (COMMANDID.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				trCmdId = message.getAsciiItem();
				if (trCmdId != null) {
					
					if (trCmdId.length() == 0) {
						hostCommandAck = 3;
						
						// 2011.11.18 by PMM
						commandExtensionParameterAck1 = 2;
						
						strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + " is null/HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					} else if (trCmdId.length() > 64) {
						hostCommandAck = 3;
						
						// 2011.11.18 by PMM
						commandExtensionParameterAck1 = 2;
						
						strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + " over 64byte/HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					}
				}
				trCmd.setTrCmdId(trCmdId);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Priority 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'PRIORITY'
			if (PRIORITY.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				priority = message.getU2Item();
				trCmd.setPriority(priority);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid PRIORITY:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Replace 
			itemCount = message.getListItem();
			commandParameterName = message.getAsciiItem();
			if (REPLACE.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				replace = message.getU2Item();
				trCmd.setReplace(replace);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid REPLACE:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
		} else { // !(str.equals("COMMANDINFO") && itemCnt == 3)
			hostCommandAck = 3;
			
			// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
			//commandExtensionParameterAck1 = 2;
			
			strLog = "[RCV S2F49]: Invalid CommandInfo:" + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}
		
		// Get 'TransferInfo' data from S2F49 
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME : 'TRANSFERINFO'
		itemCount = message.getListItem(); // L, 3 : CarrierID, SourcePort, DestPort
		if (TRANSFERINFO.equals(commandParameterName) && itemCount == 3) {
			// Get carrierId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'CARRIERID'			
			if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
				carrierId = message.getAsciiItem();
				if (carrierId != null && carrierId.length() > 64) {
					hostCommandAck = 3;
					
					// 2011.11.18 by PMM
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] Invalid CarrierID:" + carrierId + " over 64byte/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				trCmd.setCarrierId(carrierId);
				// Check condition : TrCmd Duplicate
				// 2013.07.01 by KYK
//				if (isTrCmdIdDuplicated(trCmdId, null)) {
				if (isTrCmdIdDuplicated(trCmdId, null, carrierId)) {
					// 2012.09.04 by KYK : S2F49 NAK 세분화
//					hostCommandAck = 3;
					hostCommandAck = HCACK_TRCMDID_DUPLICATE;
					
					// 2011.11.18 by PMM
//					commandExtensionParameterAck1 = 2;
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] Invalid CommandID - Already registered ID=" + trCmdId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				// Check condition : Carrier Duplicate 
				if (isCarrierIdDuplicated(carrierId)) {
					// 2012.09.04 by KYK : S2F49 NAK 세분화
//					hostCommandAck = 3;
					hostCommandAck = HCACK_CARRIERID_DUPLICATE;
					
					// 2011.11.18 by PMM
//					commandExtensionParameterAck1 = 2;
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49]: Invalid CarrierID or Invalid TrCmdStatus/CarrierID=" + carrierId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid CARRIERID:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
			
			// Get sourcePort 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'SOURCEPORT'
			if (SOURCEPORT.equals(commandParameterName) && itemCount == 2) {
				sourceLoc = message.getAsciiItem(); // CPVAL : A SourcePort
				trCmd.setSourceLoc(sourceLoc);
				
//				if (isCarrierLocAvailable(sourceLoc) == false) {
//					// 2012.09.04 by KYK : S2F49 NAK 세분화
////					hostCommandAck = 3; // at least one parameter isn't valid
//					hostCommandAck = HCACK_UNREGISTERED_SOURCELOC;
//					commandExtensionParameterAck2 = 2;	
//					strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc + "- Doesn't exist /HCACK=" + hostCommandAck;
//					writeSEMLog(strLog);
//					updateSEMHistory(RECEIVED, 2, 49, strLog);
//					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
//					return;
//				}
				
				// 2013.07.12 by KYK
				hostCommandAck = checkCarrierLocAvailable(sourceLoc, true);
				if (hostCommandAck != 4) {
					commandExtensionParameterAck2 = 2;
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				
				// 쿼리 : SELECT * FROM TRCMD WHERE CARRIERID='carrierId' 
				registeredTrCmd = trCmdManager.getTrCmdFromDBWhereCarrierId(carrierId);
				CARRIERLOC_TYPE sourcePortType = getPortType(sourceLoc);
				
				// Abnormal Check in DestChange Condition 
				if (isInDestChangeCondition(registeredTrCmd)) {
					String carrierLocInTrDB = getCarrierLocInTrDB(registeredTrCmd);
					if (sourcePortType != CARRIERLOC_TYPE.VEHICLEPORT) {
						// 2012.09.04 by KYK : S2F49 NAK 세분화
//						hostCommandAck = 3; // at least one parameter isn't valid
						hostCommandAck = HCACK_SOURCELOC_NOT_VEHICLEPORT_IN_DESTCHANGE;
						commandExtensionParameterAck2 = 2;		
						strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc + "(" + sourcePortType + ") - isn't VEHICLEPORT. [CarrierID:" + carrierId + "] /HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					} else if (sourceLoc.equals(carrierLocInTrDB) == false) {
						// 2012.09.04 by KYK : S2F49 NAK 세분화
//						hostCommandAck = 3; // at least one parameter isn't valid
						hostCommandAck = HCACK_DIFFERENT_VEHICLEPORT_IN_DESTCHANGE;
						commandExtensionParameterAck2 = 2;		
						strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc+ " - isn't Registered CarrierLoc(" + carrierLocInTrDB + "). [CarrierID:" + carrierId + "] /HCACK="+hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					}
				} else {
					// Abnormal Check : Not In DestChange, sourcePortType == VEHICLEPORT 
					if (sourcePortType == CARRIERLOC_TYPE.VEHICLEPORT) {
						// 2012.09.04 by KYK : S2F49 NAK 세분화
//						hostCommandAck = 3; // at least one parameter isn't valid
						hostCommandAck = HCACK_SOURCELOC_IS_VEHICLEPORT_IN_NORMAL_TRANSFER;
						commandExtensionParameterAck2 = 2;	
						strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc+ " is VEHICLEPORT. [CarrierID:" + carrierId + "] /HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;						
					}
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid SOURCEPORT Info" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}

			// Get destPort 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'DESTPORT'
			if (DESTPORT.equals(commandParameterName) && itemCount == 2) {
				destLoc = message.getAsciiItem(); // CPVAL : A DestPort
				trCmd.setDestLoc(destLoc);
//				if (isCarrierLocAvailable(destLoc) == false) {
//					// 2012.09.04 by KYK : S2F49 NAK 세분화
////					hostCommandAck = 3; // at least one parameter isn't valid
//					hostCommandAck = HCACK_UNREGISTERED_DESTLOC;
//					commandExtensionParameterAck2 = 2;		
//					strLog = "[RCV S2F49] Invalid DestPort=" + destLoc +	"- Doesn't exist /HCACK=" + hostCommandAck;
//					writeSEMLog(strLog);
//					updateSEMHistory(RECEIVED, 2, 49, strLog);
//					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
//					return;
//				}
				
				// 2013.07.12 by KYK
				hostCommandAck = checkCarrierLocAvailable(destLoc, false);
				if (hostCommandAck != 4) {
					commandExtensionParameterAck2 = 2;
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid DESTPORT Info=" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}

			// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가 (DestChange시 MaterialControl 참조해서 가능여부 확인)
			// 2013.08.15 by KYK : DestChange 에서는 기존 source 만 고려하도록 함 
			// 조치 : if , if else (destchange) 순서바꿈
			// zoneControlType Check 
			if (isInDestChangeCondition(registeredTrCmd)) {
				// 2011.10.21 by PMM 
				// DestChange 시, 기존 Source와 변경된 Dest 간 반송 허용 여부 확인
				
				if (registeredTrCmd != null) {
					if (registeredTrCmd.getSourceLoc() != null && registeredTrCmd.getSourceLoc().length() > 0) {
						
//						if (isTransferCommandAcceptable(registeredTrCmd.getSourceLoc(), destLoc) == false) {
						if (isTransferCommandAcceptable(registeredTrCmd, registeredTrCmd.getSourceLoc(), destLoc, true) == false) {
							hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
							commandExtensionParameterAck2 = 2;
							strLog = "[RCV S2F49] Invalid Material. - The Transfer from the previous SourceLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
							return;
						}
					} else {
						// 2011.11.01 by PMM
						// registeredTrCmd의 SourceLoc이 ""인 경우 (ex. UnknownTrCmd) carrierLoc으로 판단.
						// 2013.09.06 by KYK
//						if (isTransferCommandAcceptable(registeredTrCmd.getCarrierLoc(), destLoc) == false) {				
						// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가 (DestChange시 MaterialControl 참조해서 가능여부 확인)
						Vehicle vehicle = vehicleManager.getVehicleFromDB(registeredTrCmd.getVehicle());
//						int carrierType = vehicle.getCarrierType();
						CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(destLoc);
//						int destLocType = 100;
//						if (carrierLoc != null) {
//							destLocType = getCarrierType(carrierLoc.getMaterial());							
//						}
//
						String sourceLocMaterial = CarrierTypeConfig.getInstance().getMaterialType(vehicle.getCarrierType());
						String destLocMaterial = carrierLoc.getMaterial();
						if (!sourceLocMaterial.equals(destLocMaterial)) {
							if (ocsInfoManager.isIgnoreMaterialDifference()) {
								String material = materialControlManager.makeMeterialAssignAllowedKey(vehicle.getMaterial(), sourceLocMaterial, destLocMaterial);
								if (materialAssignAllowedList.contains(material)) {
									// MaterialControl 허용 설정
									StringBuilder sb = new StringBuilder();
									sb.append("[RCV S2F49] Different Material.");
									sb.append(" - Transfer Command (From:[").append(vehicle.getVehicleId()).append(":").append(sourceLocMaterial);
									sb.append("], To:[").append(carrierLoc.getCarrierLocId()).append(":").append(destLocMaterial);
									sb.append("]) is allowed by MaterialControl. ");
									writeSEMLog(sb.toString());
								} else {
									hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
									commandExtensionParameterAck2 = 2;
									strLog = "[RCV S2F49] Differenct Material. - The Transfer from the previous CarrierLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
									writeSEMLog(strLog);
									updateSEMHistory(RECEIVED, 2, 49, strLog);
									sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
									return;
								}
							} else {
								hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
								commandExtensionParameterAck2 = 2;
								strLog = "[RCV S2F49] Invalid Material. - The Transfer from the previous CarrierLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
								writeSEMLog(strLog);
								updateSEMHistory(RECEIVED, 2, 49, strLog);
								sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
								return;
							}
						}
					}
				}
//			} else if (isTransferCommandAcceptable(sourceLoc, destLoc) == false) {
			} else if (isTransferCommandAcceptable(registeredTrCmd, sourceLoc, destLoc, false) == false) {
					// 2012.09.04 by KYK : S2F49 NAK 세분화
//					hostCommandAck = 2;
					hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
					commandExtensionParameterAck2 = 2;
					strLog = "[RCV S2F49] Invalid Material. -The Transfer from the SourceLoc to the DestLoc is not Allowed/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				
			} else if (isRailDownCheckUsed && checkRailDown(sourceLoc, destLoc)) {
				// 2012.05.16 by MYM : Rail-Down
				// S1a Foup, Reticle 통합 반송시 사양 대응(IBSEM Spec for Conveyor Usage in OHT)
				hostCommandAck = 74; // Rail down
				commandExtensionParameterAck2 = 2;
				strLog = "[RCV S2F49] Rail down - SourceLoc=" + sourceLoc + "/DestLoc=" + destLoc + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
		} else {
			hostCommandAck = 3; // at least one parameter isn't valid
			
			// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
			//commandExtensionParameterAck2 = 2;
			
			strLog = "[RCV S2F49] Invalid TRANSFERINFO=" + commandParameterName + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);			
			return;
		}		
		
		// Register TransferCommand to DB
		if (hostCommandAck == 4 && commandExtensionParameterAck1 == 0 && commandExtensionParameterAck2 == 0) {
			trCmd.setState(TRCMD_STATE.CMD_QUEUED);
			trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
			
			StringBuilder sb = new StringBuilder();
			if (createJobInfo(trCmd, registeredTrCmd)) {
			// newTr : (rCmd, commandId, priority,replace,carrierId, sourcePort, destPort, trCmdStatus,detailTrCmdStatus)
				sb.append("Transfer [");
			} else {
				// 2013.07.01 by KYK
				hostCommandAck = 2;
				//strLog = "Fail:"+rCmd+"["+carrierId+"/" + commandId + "/" + sourcePort + "->"+ destPort+"]";
				sb.append("Fail:").append(remoteCmd).append("[");
			}
			sb.append(carrierId).append("/");
			sb.append(trCmdId).append("/");
			sb.append(sourceLoc).append("->");
			sb.append(destLoc).append("]");
			writeSEMLog(sb.toString());
			updateSEMHistory(RECEIVED, 2, 49, sb.toString());
		}		
		// Set Response Message 
		sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
	}
	
	/**
	 * S2F49 : TRANSFER_EX3 command
	 * @param message
	 */
	public void receiveTRANSFEREX3command(UComMsg message) {
		int hostCommandAck = 4;
		int itemCount;
		int priority = 0;
		int replace = 0;
		int commandExtensionParameterAck1 = 0;
		int commandExtensionParameterAck2 = 0;
		String commandParameterName;
		String strLog;
		String trCmdId = null;
		String carrierId = null;
		String sourceLoc = null;
		String destLoc = null;
		String foupId = null;
		//String rCmd = "TRANSFER_EX3";
		TrCmd registeredTrCmd = null;		
		TrCmd trCmd = new TrCmd();	
		TRCMD_REMOTECMD remoteCmd = TRCMD_REMOTECMD.TRANSFER;
		trCmd.setRemoteCmd(remoteCmd);

		itemCount = message.getListItem(); // L, 2 [CommandInfo, TransferInfo2]	
		if (itemCount != 2 && itemCount != 3) {
			// OCS use the spec. with only one carrier. (nListItemCnt = 3 --> ACS)
			hostCommandAck = 3; // at least one parameter isn't valid
			
			// 2011.11.18 by KYK
			commandExtensionParameterAck1 = 3;
			
			strLog = "[RCV S2F49] Invalid CPVAL ,Carrier Qty : " + itemCount;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);		
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}
		
		/** Get 'CommandInfo' data from S2F49 */
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME1 : "COMMANDIDINFO"
		itemCount = message.getListItem(); // L, 3 : CommandID, Priority, Replace
		
		if (COMMANDINFO.equals(commandParameterName) && itemCount == 3) {
			// Get commandId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'COMMANDID'
			if (COMMANDID.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				trCmdId = message.getAsciiItem();
				if (trCmdId != null) {
					
					if (trCmdId.length() == 0) {
						hostCommandAck = 3;
						
						// 2011.11.18 by PMM
						commandExtensionParameterAck1 = 2;
						
						strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + " is null/HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					} else if (trCmdId.length() > 64) {
						hostCommandAck = 3;
						
						// 2011.11.18 by PMM
						commandExtensionParameterAck1 = 2;
						
						strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + " over 64byte/HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					}
				}
				trCmd.setTrCmdId(trCmdId);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Priority 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'PRIORITY'
			if (PRIORITY.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				priority = message.getU2Item();
				trCmd.setPriority(priority);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid PRIORITY:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Replace 
			itemCount = message.getListItem();
			commandParameterName = message.getAsciiItem();
			if (REPLACE.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				replace = message.getU2Item();
				trCmd.setReplace(replace);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid REPLACE:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
		} else { // !(str.equals("COMMANDINFO") && itemCnt == 3)
			hostCommandAck = 3;
			
			// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
			//commandExtensionParameterAck1 = 2;
			
			strLog = "[RCV S2F49]: Invalid CommandInfo:" + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}		
		
		// Get 'TransferInfo2' data from S2F49 
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME : 'TRANSFERINFO2'
		itemCount = message.getListItem(); // L, 3 : CarrierID, SourcePort, DestPort, FoupID
		if (TRANSFERINFO2.equals(commandParameterName) && itemCount == 4) {
			// Get carrierId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'CARRIERID'			
			if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
				carrierId = message.getAsciiItem();
				if (carrierId != null && carrierId.length() > 64) {
					hostCommandAck = 3;
					
					// 2011.11.18 by PMM
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] Invalid CarrierID:" + carrierId + " over 64byte/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				trCmd.setCarrierId(carrierId);
				// Check condition : TrCmd Duplicate
				// 2013.07.01 by KYK
//				if (isTrCmdIdDuplicated(trCmdId, null)) {
				if (isTrCmdIdDuplicated(trCmdId, null, carrierId)) {
					// 2012.09.04 by KYK : S2F49 NAK 세분화
//					hostCommandAck = 3;
					hostCommandAck = HCACK_TRCMDID_DUPLICATE;
					
					// 2011.11.18 by PMM
//					commandExtensionParameterAck1 = 2;
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] Invalid CommandID - Already registered ID=" + trCmdId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				// Check condition : Carrier Duplicate 
				if (isCarrierIdDuplicated(carrierId)) {
					// 2012.09.04 by KYK : S2F49 NAK 세분화
//					hostCommandAck = 3;
					hostCommandAck = HCACK_CARRIERID_DUPLICATE;
					
					// 2011.11.18 by PMM
//					commandExtensionParameterAck1 = 2;
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49]: Invalid CarrierID or Invalid TrCmdStatus/CarrierID=" + carrierId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid CARRIERID:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
			
			// Get sourcePort 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'SOURCEPORT'
			if (SOURCEPORT.equals(commandParameterName) && itemCount == 2) {
				sourceLoc = message.getAsciiItem(); // CPVAL : A SourcePort
				trCmd.setSourceLoc(sourceLoc);
				
//				if (isCarrierLocAvailable(sourceLoc) == false) {
//					// 2012.09.04 by KYK : S2F49 NAK 세분화
////					hostCommandAck = 3; // at least one parameter isn't valid
//					hostCommandAck = HCACK_UNREGISTERED_SOURCELOC;
//					commandExtensionParameterAck2 = 2;	
//					strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc + "- Doesn't exist /HCACK=" + hostCommandAck;
//					writeSEMLog(strLog);
//					updateSEMHistory(RECEIVED, 2, 49, strLog);
//					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
//					return;
//				}
				
				// 2013.07.12 by KYK
				hostCommandAck = checkCarrierLocAvailable(sourceLoc, true);
				if (hostCommandAck != 4) {
					commandExtensionParameterAck2 = 2;
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				
				// 쿼리 : SELECT * FROM TRCMD WHERE CARRIERID='carrierId' 
				registeredTrCmd = trCmdManager.getTrCmdFromDBWhereCarrierId(carrierId);
				CARRIERLOC_TYPE sourcePortType = getPortType(sourceLoc);
				
				// Abnormal Check in DestChange Condition 
				if (isInDestChangeCondition(registeredTrCmd)) {
					String carrierLocInTrDB = getCarrierLocInTrDB(registeredTrCmd);
					if (sourcePortType != CARRIERLOC_TYPE.VEHICLEPORT) {
						// 2012.09.04 by KYK : S2F49 NAK 세분화
//						hostCommandAck = 3; // at least one parameter isn't valid
						hostCommandAck = HCACK_SOURCELOC_NOT_VEHICLEPORT_IN_DESTCHANGE;
						commandExtensionParameterAck2 = 2;		
						strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc + "(" + sourcePortType + ") - isn't VEHICLEPORT. [CarrierID:" + carrierId + "] /HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					} else if (sourceLoc.equals(carrierLocInTrDB) == false) {
						// 2012.09.04 by KYK : S2F49 NAK 세분화
//						hostCommandAck = 3; // at least one parameter isn't valid
						hostCommandAck = HCACK_DIFFERENT_VEHICLEPORT_IN_DESTCHANGE;
						commandExtensionParameterAck2 = 2;		
						strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc+ " - isn't Registered CarrierLoc(" + carrierLocInTrDB + "). [CarrierID:" + carrierId + "] /HCACK="+hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					}
				} else {
					// Abnormal Check : Not In DestChange, sourcePortType == VEHICLEPORT 
					if (sourcePortType == CARRIERLOC_TYPE.VEHICLEPORT) {
						// 2012.09.04 by KYK : S2F49 NAK 세분화
//						hostCommandAck = 3; // at least one parameter isn't valid
						hostCommandAck = HCACK_SOURCELOC_IS_VEHICLEPORT_IN_NORMAL_TRANSFER;
						commandExtensionParameterAck2 = 2;	
						strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc+ " is VEHICLEPORT. [CarrierID:" + carrierId + "] /HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;						
					}
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid SOURCEPORT Info" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}

			// Get destPort 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'DESTPORT'
			if (DESTPORT.equals(commandParameterName) && itemCount == 2) {
				destLoc = message.getAsciiItem(); // CPVAL : A DestPort
				trCmd.setDestLoc(destLoc);
//				if (isCarrierLocAvailable(destLoc) == false) {
//					// 2012.09.04 by KYK : S2F49 NAK 세분화
////					hostCommandAck = 3; // at least one parameter isn't valid
//					hostCommandAck = HCACK_UNREGISTERED_DESTLOC;
//					commandExtensionParameterAck2 = 2;		
//					strLog = "[RCV S2F49] Invalid DestPort=" + destLoc +	"- Doesn't exist /HCACK=" + hostCommandAck;
//					writeSEMLog(strLog);
//					updateSEMHistory(RECEIVED, 2, 49, strLog);
//					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
//					return;
//				}
				
				// 2013.07.12 by KYK
				hostCommandAck = checkCarrierLocAvailable(destLoc, false);
				if (hostCommandAck != 4) {
					commandExtensionParameterAck2 = 2;
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid DESTPORT Info=" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get foupId
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'FOUPID'			
			if (FOUPID.equals(commandParameterName) && itemCount == 2) {
				foupId = message.getAsciiItem();
				if (foupId != null && foupId .length() > 64) {
					hostCommandAck = 3;
					
					// 2011.11.18 by PMM
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] Invalid FoupID:" + foupId + " over 64byte/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				trCmd.setFoupId(foupId);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid FOUPID:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}	

			// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가 (DestChange시 MaterialControl 참조해서 가능여부 확인)
			// 2013.08.15 by KYK : DestChange 에서는 기존 source 만 고려하도록 함 
			// 조치 : if , if else (destchange) 순서바꿈
			// zoneControlType Check 
			if (isInDestChangeCondition(registeredTrCmd)) {
				// 2011.10.21 by PMM 
				// DestChange 시, 기존 Source와 변경된 Dest 간 반송 허용 여부 확인
				
				if (registeredTrCmd != null) {
					if (registeredTrCmd.getSourceLoc() != null && registeredTrCmd.getSourceLoc().length() > 0) {
//						if (isTransferCommandAcceptable(registeredTrCmd.getSourceLoc(), destLoc) == false) {
						if (isTransferCommandAcceptable(registeredTrCmd, registeredTrCmd.getSourceLoc(), destLoc, true) == false) {
							hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
							commandExtensionParameterAck2 = 2;
							strLog = "[RCV S2F49] Invalid Material. - The Transfer from the previous SourceLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
							return;
						}
					} else {
						// 2011.11.01 by PMM
						// registeredTrCmd의 SourceLoc이 ""인 경우 (ex. UnknownTrCmd) carrierLoc으로 판단.
						// 2013.09.06 by KYK
//						if (isTransferCommandAcceptable(registeredTrCmd.getCarrierLoc(), destLoc) == false) {				
						Vehicle vehicle = vehicleManager.getVehicleFromDB(registeredTrCmd.getVehicle());
//						int carrierType = vehicle.getCarrierType();
						CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(destLoc);
//						int destLocType = 100;
//						if (carrierLoc != null) {
//							destLocType = getCarrierType(carrierLoc.getMaterial());							
//						}

						String sourceLocMaterial = CarrierTypeConfig.getInstance().getMaterialType(vehicle.getCarrierType());
						String destLocMaterial = carrierLoc.getMaterial();
						if (ocsInfoManager.isIgnoreMaterialDifference()) {
							String material = materialControlManager.makeMeterialAssignAllowedKey(vehicle.getMaterial(), sourceLocMaterial, destLocMaterial);
							if (materialAssignAllowedList.contains(material)) {
								// MaterialControl 허용 설정
								StringBuilder sb = new StringBuilder();
								sb.append("[RCV S2F49] Different Material.");
								sb.append(" - Transfer Command (From:[").append(vehicle.getVehicleId()).append(":").append(sourceLocMaterial);
								sb.append("], To:[").append(carrierLoc.getCarrierLocId()).append(":").append(destLocMaterial);
								sb.append("]) is allowed by MaterialControl. ");

								writeSEMLog(sb.toString());
							} else {
								hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
								commandExtensionParameterAck2 = 2;
								strLog = "[RCV S2F49] Differenct Material. - The Transfer from the previous CarrierLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
								writeSEMLog(strLog);
								updateSEMHistory(RECEIVED, 2, 49, strLog);
								sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
								return;
							}
						} else {
							hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
							commandExtensionParameterAck2 = 2;
							strLog = "[RCV S2F49] Invalid Material. - The Transfer from the previous CarrierLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
							return;
						}
					}
				}
//			} else if (isTransferCommandAcceptable(sourceLoc, destLoc) == false) {
			} else if (isTransferCommandAcceptable(registeredTrCmd, sourceLoc, destLoc, false) == false) {
				// 2012.09.04 by KYK : S2F49 NAK 세분화
//					hostCommandAck = 2;
				hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
				commandExtensionParameterAck2 = 2;
				strLog = "[RCV S2F49] Invalid Material. -The Transfer from the SourceLoc to the DestLoc is not Allowed/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
				
			} else if (isRailDownCheckUsed && checkRailDown(sourceLoc, destLoc)) {
				// 2012.05.16 by MYM : Rail-Down
				// S1a Foup, Reticle 통합 반송시 사양 대응(IBSEM Spec for Conveyor Usage in OHT)
				hostCommandAck = 74; // Rail down
				commandExtensionParameterAck2 = 2;
				strLog = "[RCV S2F49] Rail down - SourceLoc=" + sourceLoc + "/DestLoc=" + destLoc + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
		} else {
			hostCommandAck = 3; // at least one parameter isn't valid
			
			// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
			//commandExtensionParameterAck2 = 2;
			
			strLog = "[RCV S2F49] Invalid TRANSFERINFO=" + commandParameterName + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);			
			return;
		}		
		
		// Register TransferCommand to DB
		if (hostCommandAck == 4 && commandExtensionParameterAck1 == 0 && commandExtensionParameterAck2 == 0) {
			trCmd.setState(TRCMD_STATE.CMD_QUEUED);
			trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
			
			StringBuilder sb = new StringBuilder();
			if (createJobInfo(trCmd, registeredTrCmd)) {
			// newTr : (rCmd, commandId, priority,replace,carrierId, sourcePort, destPort, trCmdStatus,detailTrCmdStatus)
				sb.append("Transfer [");
			} else {
				// 2013.07.01 by KYK
				hostCommandAck = 2;
				//strLog = "Fail:"+rCmd+"["+carrierId+"/" + commandId + "/" + sourcePort + "->"+ destPort+"]";
				sb.append("Fail:").append(remoteCmd).append("[");
			}
			sb.append(carrierId).append("/");
			sb.append(trCmdId).append("/");
			sb.append(sourceLoc).append("->");
			sb.append(destLoc).append("]");
			writeSEMLog(sb.toString());
			updateSEMHistory(RECEIVED, 2, 49, sb.toString());
		}		
		// Set Response Message 
		sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
	}
	
	/**
	 * S2F49 : TRANSFER_EX4 command
	 * 2021.04.02 by JDH : Transfer Premove 사양 추가
	 * @param message
	 */
	public void receiveTRANSFEREX4command(UComMsg message) {
		int hostCommandAck = 4;
		int itemCount;
		int priority = 0;
		int replace = 0;
		int commandExtensionParameterAck1 = 0;
		int commandExtensionParameterAck2 = 0;
		String commandParameterName;
		String strLog;
		String trCmdId = null;
		String carrierId = null;
		String sourceLoc = null;
		String destLoc = null;
		String deliveryType = null;
		int expectedDeliveryTime = 10;
		int deliveryWaitTimeOut = 10;
		TrCmd registeredTrCmd = null;		
		TrCmd trCmd = new TrCmd();	
		TRCMD_REMOTECMD remoteCmd = TRCMD_REMOTECMD.TRANSFER;
		trCmd.setRemoteCmd(remoteCmd);

		itemCount = message.getListItem(); // L, 3 [CommandInfo, TransferInfo, DeliveryInfo]	
		if (itemCount != 3) {
			// OCS use the spec. with only one carrier. (nListItemCnt = 3 --> ACS)
			hostCommandAck = 3; // at least one parameter isn't valid
			
			// 2011.11.18 by KYK
			commandExtensionParameterAck1 = 3;
			
			strLog = "[RCV S2F49] Invalid CPVAL ,Carrier Qty : " + itemCount;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);		
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}
		
		/** Get 'CommandInfo' data from S2F49 */
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME1 : "COMMANDIDINFO"
		itemCount = message.getListItem(); // L, 3 : CommandID, Priority, Replace
		
		if (COMMANDINFO.equals(commandParameterName) && itemCount == 3) {
			// Get commandId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'COMMANDID'
			if (COMMANDID.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				trCmdId = message.getAsciiItem();
				if (trCmdId != null) {
					
					if (trCmdId.length() == 0) {
						hostCommandAck = 3;
						
						// 2011.11.18 by PMM
						commandExtensionParameterAck1 = 2;
						
						strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + " is null/HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					} else if (trCmdId.length() > 64) {
						hostCommandAck = 3;
						
						// 2011.11.18 by PMM
						commandExtensionParameterAck1 = 2;
						
						strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + " over 64byte/HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					}
				}
				trCmd.setTrCmdId(trCmdId);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid CommandId:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Priority 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'PRIORITY'
			if (PRIORITY.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				priority = message.getU2Item();
				trCmd.setPriority(priority);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid PRIORITY:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get Replace 
			itemCount = message.getListItem();
			commandParameterName = message.getAsciiItem();
			if (REPLACE.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				replace = message.getU2Item();
				trCmd.setReplace(replace);
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck1 = 2;
				
				strLog = "[RCV S2F49] Invalid REPLACE:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
		} else { // !(str.equals("COMMANDINFO") && itemCnt == 3)
			hostCommandAck = 3;
			
			// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
			//commandExtensionParameterAck1 = 2;
			
			strLog = "[RCV S2F49]: Invalid CommandInfo:" + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}		
		
		// Get 'TransferInfo' data from S2F49 
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME : 'TRANSFERINFO'
		itemCount = message.getListItem(); // L, 3 : CarrierID, SourcePort, DestPort
		if (TRANSFERINFO.equals(commandParameterName) && itemCount == 3) {
			// Get carrierId 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'CARRIERID'			
			if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
				carrierId = message.getAsciiItem();
				if (carrierId != null && carrierId.length() > 64) {
					hostCommandAck = 3;
					
					// 2011.11.18 by PMM
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] Invalid CarrierID:" + carrierId + " over 64byte/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				trCmd.setCarrierId(carrierId);
			
				if (isTrCmdIdDuplicated(trCmdId, null, carrierId)) {
					// 2012.09.04 by KYK : S2F49 NAK 세분화
					hostCommandAck = HCACK_TRCMDID_DUPLICATE;
					
					// 2011.11.18 by PMM
//					commandExtensionParameterAck1 = 2;
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] Invalid CommandID - Already registered ID=" + trCmdId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				// Check condition : Carrier Duplicate
				if (isCarrierIdDuplicated(carrierId,remoteCmd.toConstString())) {	// 21.08.25 dahye : RCMD 기준으로 CarrierID DUP 판단(TRANSFER_EX4 ⊃ TRANSFER)
					// 2012.09.04 by KYK : S2F49 NAK 세분화
					hostCommandAck = HCACK_CARRIERID_DUPLICATE;
					
					// 2011.11.18 by PMM
//					commandExtensionParameterAck1 = 2;
					commandExtensionParameterAck2 = 2;
					
					strLog = "[RCV S2F49] : Invalid CarrierID or Invalid TrCmdStatus/CarrierID=" + carrierId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid CARRIERID:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}			
			
			// Get sourcePort 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'SOURCEPORT'
			if (SOURCEPORT.equals(commandParameterName) && itemCount == 2) {
				sourceLoc = message.getAsciiItem(); // CPVAL : A SourcePort
				trCmd.setSourceLoc(sourceLoc);
				
				// 2013.07.12 by KYK
				hostCommandAck = checkCarrierLocAvailable(sourceLoc, true);
				if (hostCommandAck != 4) {
					commandExtensionParameterAck2 = 2;
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
				
				// 쿼리 : SELECT * FROM TRCMD WHERE CARRIERID='carrierId' 
				registeredTrCmd = trCmdManager.getTrCmdFromDBWhereCarrierId(carrierId);
				CARRIERLOC_TYPE sourcePortType = getPortType(sourceLoc);
				
				// Abnormal Check in DestChange Condition 
				if (isInDestChangeCondition(registeredTrCmd)) {
					String carrierLocInTrDB = getCarrierLocInTrDB(registeredTrCmd);
					// 21.08.25 dahye : TRANSFER_EX4 ⊃ TRANSFER
					if (sourcePortType != CARRIERLOC_TYPE.VEHICLEPORT) {
						if (!registeredTrCmd.getDeliveryType().equals(PREMOVE)) {
							// 2012.09.04 by KYK : S2F49 NAK 세분화
//							hostCommandAck = 3; // at least one parameter isn't valid
							hostCommandAck = HCACK_SOURCELOC_NOT_VEHICLEPORT_IN_DESTCHANGE;
							commandExtensionParameterAck2 = 2;		
							strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc + "(" + sourcePortType + ") - isn't VEHICLEPORT. [CarrierID:" + carrierId + "] /HCACK=" + hostCommandAck;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
							return;
						}
					} else if (sourceLoc.equals(carrierLocInTrDB) == false) {
						// 2012.09.04 by KYK : S2F49 NAK 세분화
//							hostCommandAck = 3; // at least one parameter isn't valid
						hostCommandAck = HCACK_DIFFERENT_VEHICLEPORT_IN_DESTCHANGE;
						commandExtensionParameterAck2 = 2;		
						strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc+ " - isn't Registered CarrierLoc(" + carrierLocInTrDB + "). [CarrierID:" + carrierId + "] /HCACK="+hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					}
				} else {
					// Abnormal Check : Not In DestChange, sourcePortType == VEHICLEPORT 
					if (sourcePortType == CARRIERLOC_TYPE.VEHICLEPORT) {
						// 2012.09.04 by KYK : S2F49 NAK 세분화
//						hostCommandAck = 3; // at least one parameter isn't valid
						hostCommandAck = HCACK_SOURCELOC_IS_VEHICLEPORT_IN_NORMAL_TRANSFER;
						commandExtensionParameterAck2 = 2;	
						strLog = "[RCV S2F49] Invalid SourcePort=" + sourceLoc+ " is VEHICLEPORT. [CarrierID:" + carrierId + "] /HCACK=" + hostCommandAck;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 49, strLog);
						sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
						return;
					}
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid SOURCEPORT Info" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}

			// Get destPort 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'DESTPORT'
			if (DESTPORT.equals(commandParameterName) && itemCount == 2) {
				destLoc = message.getAsciiItem(); // CPVAL : A DestPort
				trCmd.setDestLoc(destLoc);
				
				hostCommandAck = checkCarrierLocAvailable(destLoc, false);
				if (hostCommandAck != 4) {
					commandExtensionParameterAck2 = 2;
					sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
					return;
				}
			} else {
				hostCommandAck = 3;
				
				// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
				//commandExtensionParameterAck2 = 2;
				
				strLog = "[RCV S2F49] Invalid DESTPORT Info=" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가 (DestChange시 MaterialControl 참조해서 가능여부 확인)
			// 2013.08.15 by KYK : DestChange 에서는 기존 source 만 고려하도록 함 
			// 조치 : if , if else (destchange) 순서바꿈
			// zoneControlType Check 
			if (isInDestChangeCondition(registeredTrCmd)) {
				// 2011.10.21 by PMM 
				// DestChange 시, 기존 Source와 변경된 Dest 간 반송 허용 여부 확인
				
				if (registeredTrCmd != null) {
					if (registeredTrCmd.getSourceLoc() != null && registeredTrCmd.getSourceLoc().length() > 0) {
						if (isTransferCommandAcceptable(registeredTrCmd, registeredTrCmd.getSourceLoc(), destLoc, true) == false) {
							hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
							commandExtensionParameterAck2 = 2;
							strLog = "[RCV S2F49] Invalid Material. - The Transfer from the previous SourceLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
							writeSEMLog(strLog);
							updateSEMHistory(RECEIVED, 2, 49, strLog);
							sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
							return;
						}
					} else {
						// 2011.11.01 by PMM
						// registeredTrCmd의 SourceLoc이 ""인 경우 (ex. UnknownTrCmd) carrierLoc으로 판단.
						Vehicle vehicle = vehicleManager.getVehicleFromDB(registeredTrCmd.getVehicle());
						CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(destLoc);

						String sourceLocMaterial = CarrierTypeConfig.getInstance().getMaterialType(vehicle.getCarrierType());
						String destLocMaterial = carrierLoc.getMaterial();
						
						if (!sourceLocMaterial.equals(destLocMaterial)) {
							if (ocsInfoManager.isIgnoreMaterialDifference()) {
								String material = materialControlManager.makeMeterialAssignAllowedKey(vehicle.getMaterial(), sourceLocMaterial, destLocMaterial);
								if (materialAssignAllowedList.contains(material)) {
									// MaterialControl 허용 설정
									StringBuilder sb = new StringBuilder();
									sb.append("[RCV S2F49] Different Material.");
									sb.append(" - Transfer_EX4 Command (From:[").append(vehicle.getVehicleId()).append(":").append(sourceLocMaterial);
									sb.append("], To:[").append(carrierLoc.getCarrierLocId()).append(":").append(destLocMaterial);
									sb.append("]) is allowed by MaterialControl. ");

									writeSEMLog(sb.toString());
								} else {
									hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
									commandExtensionParameterAck2 = 2;
									strLog = "[RCV S2F49] Differenct Material. - The Transfer from the previous CarrierLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
									writeSEMLog(strLog);
									updateSEMHistory(RECEIVED, 2, 49, strLog);
									sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
									return;
								}
							} else {
								hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
								commandExtensionParameterAck2 = 2;
								strLog = "[RCV S2F49] Invalid Material. - The Transfer from the previous CarrierLoc to the changed DestLoc is not Allowed/HCACK=" + hostCommandAck;
								writeSEMLog(strLog);
								updateSEMHistory(RECEIVED, 2, 49, strLog);
								sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
								return;
							}
						}
					}
				}
			} else if (isTransferCommandAcceptable(registeredTrCmd, sourceLoc, destLoc, false) == false) {
				// 2012.09.04 by KYK : S2F49 NAK 세분화
				hostCommandAck = HCACK_DIFFERENT_MATERIALTYPE;
				commandExtensionParameterAck2 = 2;
				strLog = "[RCV S2F49] Invalid Material. -The Transfer from the SourceLoc to the DestLoc is not Allowed/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
				
			} else if (isRailDownCheckUsed && checkRailDown(sourceLoc, destLoc)) {
				// 2012.05.16 by MYM : Rail-Down
				// S1a Foup, Reticle 통합 반송시 사양 대응(IBSEM Spec for Conveyor Usage in OHT)
				hostCommandAck = 74; // Rail down
				commandExtensionParameterAck2 = 2;
				strLog = "[RCV S2F49] Rail down - SourceLoc=" + sourceLoc + "/DestLoc=" + destLoc + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
		} else {
			hostCommandAck = 3; // at least one parameter isn't valid
			
			// 2011.11.18 by KYK : invalid Parameter Or Format : 그래서 표시 안해주는게 나을듯 (현재 MCS 값 의미있게 참조안함)
			//commandExtensionParameterAck2 = 2;
			
			strLog = "[RCV S2F49] Invalid TRANSFERINFO=" + commandParameterName + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);			
			return;
		}
		
		
		/** Get 'DELIVERYINFO' data from S2F49 */
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); // CPNAME1 : "DELIVERYINFO"
		itemCount = message.getListItem(); // L, 3 : DeliveryType, ExpectedDeliveryTime, DeliveryWaitTimeout
		
		if (DELIVERYINFO.equals(commandParameterName) && itemCount == 3) {
			// Get DeliveryType
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'DELIVERYTYPE'
			if (DELIVERYTYPE.equals(commandParameterName) && itemCount == 2) {
				deliveryType = message.getAsciiItem();
				if (deliveryType.equals("PREMOVE")) {
					trCmd.setRemoteCmd(TRCMD_REMOTECMD.PREMOVE);
					trCmd.setDeliveryType(deliveryType);
					trCmd.setWaitStartedTime(ocsInfoManager.getCurrDBTimeStr());	// 2022.03.14 dahye : Premove Logic Improve
					remoteCmd = TRCMD_REMOTECMD.PREMOVE;
				} else {
					trCmd.setDeliveryType("TRANSFER");
					remoteCmd = TRCMD_REMOTECMD.TRANSFER;
				}
			} else {
				hostCommandAck = 3;
				
				strLog = "[RCV S2F49] Invalid DeliveryType:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get ExpectedDeliveryTime
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'EXPECTEDDELIVERYTIME'
			if (EXPECTEDDELIVERYTIME.equals(commandParameterName) && itemCount == 2) {
				expectedDeliveryTime = message.getU2Item();
				trCmd.setExpectedDeliveryTime(expectedDeliveryTime);
			} else {
				hostCommandAck = 3;
				
				strLog = "[RCV S2F49] Invalid ExpectedDeliveryTime:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
			
			// Get DeliveryWaitTimeout
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'DELIVERYWAITTIMEOUT'
			if (DELIVERYWAITTIMEOUT.equals(commandParameterName) && itemCount == 2) {
				deliveryWaitTimeOut = message.getU2Item();
				trCmd.setDeliveryWaitTimeOut(deliveryWaitTimeOut);
			} else {
				hostCommandAck = 3;
				
				strLog = "[RCV S2F49] Invalid DeliveryWaitTimeout:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 49, strLog);
				sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
				return;
			}
		} else {
			hostCommandAck = 3;
			
			strLog = "[RCV S2F49]: Invalid DeliveryInfo:" + "/HCACK=" + hostCommandAck;
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 49, strLog);
			sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
			return;
		}
		
		// Register TransferCommand to DB
		if (hostCommandAck == 4 && commandExtensionParameterAck1 == 0 && commandExtensionParameterAck2 == 0) {
			trCmd.setState(TRCMD_STATE.CMD_QUEUED);
			trCmd.setDetailState(TRCMD_DETAILSTATE.NOT_ASSIGNED);
			
			StringBuilder sb = new StringBuilder();
			if (createJobInfo(trCmd, registeredTrCmd)) {
				sb.append(remoteCmd).append(" [");
			} else {
				// 2013.07.01 by KYK
				hostCommandAck = 2;
				sb.append("Fail:").append(remoteCmd).append("[");
			}
			sb.append(carrierId).append("/");
			sb.append(trCmdId).append("/");
			sb.append(sourceLoc).append("->");
			sb.append(destLoc).append("]");
			writeSEMLog(sb.toString());
			updateSEMHistory(RECEIVED, 2, 49, sb.toString());
		}
		// Set Response Message 
		sendS2F50(message, remoteCmd, hostCommandAck, commandExtensionParameterAck1, commandExtensionParameterAck2);
	}

	
	/**
	 * S1a Foup, Reticle 통합 반송시 사양 대응(IBSEM Spec for Conveyor Usage in OHT)
	 * 
	 * @param sourceLocId
	 * @param destLocId
	 * @return
	 */
	private boolean checkRailDown(String sourceLocId, String destLocId) {
		// 2012.05.16 by MYM : Rail-Down
		CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(sourceLocId);
		CarrierLoc destLoc = carrierLocManager.getCarrierLocData(destLocId);
		if (sourceLoc == null || destLoc == null) {
			return false;
		}
		
		Node sourceNode = nodeManager.getNode(sourceLoc.getNode());
		Node destNode = nodeManager.getNode(destLoc.getNode());
		if (sourceNode == null || destNode == null) {
			return false;
		}
		
		// 2012.08.22 by : MYM : Zone -> Area로 변경
		String sourceArea = sourceNode.getAreaId();
		String destArea = destNode.getAreaId();
		if (sourceArea.length() > 0 && destArea.length() > 0 && sourceArea.equals(destArea) == false) {
			if (railDownControlManager.isRailAvailable(sourceArea, OcsConstant.OUT) == false) {
				return true;
			}
			if (railDownControlManager.isRailAvailable(destArea, OcsConstant.IN) == false) {
				return true;
			}
		}
		
		return false;
	}
	
	private String getCarrierLocInTrDB(TrCmd trCmd) {
		if (trCmd != null) {
			return trCmd.getCarrierLoc();
		}
		return null;
	}

	private boolean isInDestChangeCondition(TrCmd trCmd) {
		if (trCmd != null && trCmd.getRemoteCmd() == TRCMD_REMOTECMD.ABORT) {
			return true;
		} else {
			return false;			
		}
	}

	/**
	 * TRCMD (반송의 허용여부 : SourceLoc과 DestLoc의 Material 비교.) 
	 * @param sourceLocId
	 * @param destLocId
	 * @return
	 */
	@Deprecated
	private boolean isTransferCommandAcceptable(String sourceLocId, String destLocId) {
		CarrierLoc sourceLoc;
		CarrierLoc destLoc;
		String sourceLocMaterial = "";
		String destLocMaterial = "";
		
		sourceLoc = carrierLocManager.getCarrierLocData(sourceLocId);
		if (sourceLoc != null) {
			sourceLocMaterial = sourceLoc.getMaterial();
		}
		
		destLoc = carrierLocManager.getCarrierLocData(destLocId);
		if (destLoc != null) {
			destLocMaterial = destLoc.getMaterial();
		}
		
		if (sourceLocMaterial != null && sourceLocMaterial.length() > 0) {
			if (sourceLocMaterial.equals(destLocMaterial)) {
				return true;
			}
		}
		StringBuilder sblog = new StringBuilder();
		sblog.append("[RCV S2F49] Invalid Material.");
		sblog.append("-Transfer Command (From:[").append(sourceLocId).append(":").append(sourceLocMaterial);
		sblog.append("], To:[").append(destLocId).append(":").append(destLocMaterial);
		sblog.append("]) is not allowed. ");
		
		writeSEMLog(sblog.toString());
		return false;
	}
	
	/** 2019.10.15 by kw3711.kim	: MaterialControl Dest 추가
	 * @param trcmd
	 * @param sourceLocId
	 * @param destLocId
	 * @param isDestChange
	 * @return
	 */
	private boolean isTransferCommandAcceptable(TrCmd trcmd, String sourceLocId, String destLocId, boolean isDestChange) {
		Vehicle vehicle;
		CarrierLoc sourceLoc;
		CarrierLoc destLoc;
		String vehicleMaterial = "";
		String sourceLocMaterial = "";
		String destLocMaterial = "";

		sourceLoc = carrierLocManager.getCarrierLocData(sourceLocId);
		if (sourceLoc != null) {
			sourceLocMaterial = sourceLoc.getMaterial();
		}
		
		destLoc = carrierLocManager.getCarrierLocData(destLocId);
		if (destLoc != null) {
			destLocMaterial = destLoc.getMaterial();
		}
		
		if (sourceLocMaterial != null && sourceLocMaterial.length() > 0) {
			if (sourceLocMaterial.equals(destLocMaterial)) {
				return true;
			} else {
				if (ocsInfoManager.isIgnoreMaterialDifference()) {
					if (isDestChange) {
						vehicle = vehicleManager.getVehicleFromDB(trcmd.getVehicle());	
						if (vehicle != null) {
							vehicleMaterial = vehicle.getMaterial();
						}

						String material = vehicleMaterial + "_" + sourceLocMaterial + "_" + destLocMaterial;
						if (materialAssignAllowedList.contains(material)) {
							// MaterialControl 허용 설정
							StringBuilder sblog = new StringBuilder();
							sblog.append("[RCV S2F49] Different Material.");
							sblog.append(" - Transfer Command (From:[").append(sourceLocId).append(":").append(sourceLocMaterial);
							sblog.append("], To:[").append(destLocId).append(":").append(destLocMaterial);
							sblog.append("]) is allowed by MaterialControl.");
							writeSEMLog(sblog.toString());
							return true;
						}
					} else {
						String material = sourceLocMaterial + "_" + destLocMaterial;
						if(sourceDestAssignAllowedList != null){
							if (sourceDestAssignAllowedList.contains(material)) {
								// MaterialControl 허용 설정
								StringBuilder sblog = new StringBuilder();
								sblog.append("[RCV S2F49] Different Material.");
								sblog.append(" - Transfer Command (From:[").append(sourceLocId).append(":").append(sourceLocMaterial);
								sblog.append("], To:[").append(destLocId).append(":").append(destLocMaterial);
								sblog.append("]) is allowed by MaterialControl.");
								writeSEMLog(sblog.toString());
								return true;
							}
						}
						else{
							return false;
						}
						
					}
				}
			}
		}
		return false;
	}

	/**
	 * S2F41 : STAGEDELETE (STAGE 반송 취소)
	 * @param message
	 */
	public void receiveSTAGEDELETEcommand(UComMsg message) {
		int itemCount;
		int hostCommandAck = 4;
		String commandParameterName;
		String trCmdId;		
		String vehicleId = null;
		String strLog;
		
		itemCount = message.getListItem(); // L, n
		itemCount = message.getListItem(); // L, 2
		commandParameterName = message.getAsciiItem(); //  CPNAME : "COMMANDID"
		if (STAGEID.equals(commandParameterName) == false) {
			hostCommandAck = 3; // at least one parameter isn't valid
			strLog = "[RCV S2F41] Invalid CPNAME";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			sendS2F42(message, hostCommandAck);
			return;
		} else {
			trCmdId = message.getAsciiItem(); // CPVAL : CommandID Value
			writeSEMLog("          [COMMANDID]:" + trCmdId);			
			/* if. commandId ""(Blank)이면, 모든 Stage 취소
			 * else. commandId ""(Blank) 아니면
			 *   2-1. commandId TrCmd에 존재하면 ACK
			 *     2-1-1. Stage 명령에 Vehicle 할당되지 않은  경우 삭제
			 *     2-1-2. Stage 명령에 Vehicle 할당된 경우  Vehicle RequestedType에 'STAGEDELETE'로 변경
			 *   2-2. commandId TrCmd에 존재하지 않으면 NACK(HCACK=6)			 *  
			 */
			HashMap<String, TrCmd> stageCmdTable;
			// commandId "" : 전체 STAGE Delete 옵션
			
			// 2011.11.05 by PMM
//			if ("".equals(trCmdId)) {
			if (trCmdId != null && trCmdId.length() == 0) {
				StringBuilder sb = new StringBuilder();
				sb.append("[RCV S2F41] [commandId=''] Delete All StageCmd / ");
				stageCmdTable = trCmdManager.getRegisteredStageCmd(null);
				
				// 2011.11.08 by PMM
//				if (stageCmdTable.isEmpty()) {
				if (stageCmdTable == null || (stageCmdTable != null && stageCmdTable.isEmpty())) {
					hostCommandAck = 6;
					strLog = "[RCV S2F41] Cmd does not exist. Invalid COMMANDID : " + trCmdId;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					sendS2F42(message, hostCommandAck);
					return;
				} else {
					hostCommandAck = 0;					
					Iterator<TrCmd> iter = stageCmdTable.values().iterator();
					while (iter.hasNext()) {
						TrCmd stageTr = iter.next();
						trCmdId = stageTr.getTrCmdId();
						vehicleId = stageTr.getVehicle();
						
						sb.append(trCmdId).append("/");
						// STAGEcmd has Not Assigned to VHL yet. 
						
						if (vehicleId == null || (vehicleId != null && vehicleId.length() == 0)) {
							// insert STAGEDELETE TrHistoryDB 
							trCmdManager.updateSTAGEDELETETrCompletionHistoryDB(stageTr);
							// delete STAGE TrCmdDB 
							trCmdManager.deleteSTAGECmdFromDB(trCmdId);
							writeSEMLog("	STAGE DELETE by Remote Command STAGEDELETE : " + trCmdId);
						} else {
							// STAGEcmd to VHL has Already Assigned to VHL. 
							trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, STAGEDELETE, "");
							writeSEMLog("	UPDATE TrCmd SET CHANGEDREMOTECMD='STAGEDELETE' WHERE TRCMDID=" + trCmdId);
						}
					}					
					writeSEMLog(sb.toString());
					updateSEMHistory(RECEIVED, 2, 41, sb.toString());					
					// ?? Batch 반송처리
				}
			} else {				
				// 정상 개별 STAGE 반송 Delete 
				stageCmdTable = trCmdManager.getRegisteredStageCmd(trCmdId);
				
				if (stageCmdTable == null || (stageCmdTable != null && stageCmdTable.isEmpty())) {
					hostCommandAck = 6;
					strLog = "[RCV S2F41] Cmd does not exist. Invalid COMMANDID : " + trCmdId;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					sendS2F42(message, hostCommandAck);
					return;
				} else {
					hostCommandAck = 0;
					vehicleId = stageCmdTable.get(trCmdId).getVehicle();					
					// STAGEcmd has Not Assigned to VHL yet. 
					
					if (vehicleId != null && vehicleId.length() == 0) {
						TrCmd trCmd = stageCmdTable.get(trCmdId);
						// insert STAGEDELETE TrHistoryDB
						trCmdManager.updateSTAGEDELETETrCompletionHistoryDB(trCmd);
						// delete STAGE TrCmdDB 
						trCmdManager.deleteSTAGECmdFromDB(trCmdId);
						strLog = "STAGE DELETE by Remote Command STAGEDELETE : " + trCmdId;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 41, strLog);
					} else {
						// STAGEcmd has Already Assigned to VHL. 
						trCmdManager.updateTrCmdChangedInfoToDB(trCmdId, STAGEDELETE, "");
						strLog = "UPDATE TrCmd SET CHANGEDREMOTECMD='STAGEDELETE' WHERE TRCMDID=" + trCmdId;
						writeSEMLog(strLog);
						updateSEMHistory(RECEIVED, 2, 41, strLog);
					}					
				}				
			}
		}
		sendS2F42(message, hostCommandAck);
	}

	public void setSEMStatusToDB() {
		if (!ocsInfoManager.checkCurrentDBStatus()) {
			writeSEMLog("DB connection fail while updating SEM Status");
		}
		
		updateOcsInfoToDB(HSMSSTATUS, hsmsStatus);
		updateOcsInfoToDB(COMMSTATUS, commStatus);
		updateOcsInfoToDB(CONTROLSTATUS, controlStatus);
		updateOcsInfoToDB(TSCSTATUS, tscStatus);
		
		StringBuilder sb = new StringBuilder();
		sb.append("IBSEM Status : ");
		sb.append(hsmsStatus).append("/");
		sb.append(commStatus).append("/");
		sb.append(controlStatus).append("/");
		sb.append(tscStatus);		
		writeSEMLog(sb.toString());
	}

	public boolean updateOcsInfoToDB(String name, String value) {
		ocsInfoManager.addOCSInfoToOCSInfoUpdateList(name, value);
		return true;
	}

	@Override
	public void writeSEMLog(String log) {
		ibsemManager.traceIBSEMMain(log);
	}
	
	private void traceException(String message, Throwable t) {
		ibsemManager.traceException(message, t);
	}
	
	private void traceException(String message) {
		ibsemManager.traceException(message);
	}

	
	/**
	 * 
	 * @param trCmd
	 * @param registeredTrCmd
	 * @return
	 */
	public boolean createJobInfo(TrCmd trCmd, TrCmd registeredTrCmd) {
		// newTr : (rCmd, commandId, priority,replace,carrierId, sourcePort, destPort, trCmdStatus,detailTrCmdStatus)
		
		/*
		 * Validation is Already Checked Previous Step.(TrCmdId, CarrierId Duplicate) 
		 * case 0 : carrierId NotExist, create TRANSFER
		 * case 1 : carrierId Exist, remoteCmd:'ABORT' -> 'TRANSFER' (requestedtype:'DESTCHANGE')
		 * case 2 : carrierId Exist, remoteCmd:'STAGE' -> 'TRANSFER' (requestedtype:'STAGECHANGE')
		 * DESTCHANGE, STAGECHANGE 후 이전 반송(ABORT,STAGE) 삭제는 Operation 에서 처리
		 */
		
		// variables from inputData 
		String carrierId = trCmd.getCarrierId();
		String trCmdId = trCmd.getTrCmdId();
		String sourceLocId = trCmd.getSourceLoc();
		String destLocId = trCmd.getDestLoc();
		// 2021.04.02 by JDH : Transfer Premove 사양 추가
//		String remoteCmd = TRANSFER; // 이거 정리좀 .
		String remoteCmd = trCmd.getRemoteCmd().toString();
		String deliveryType = trCmd.getDeliveryType();
		int expectedDeliveryTime = trCmd.getExpectedDeliveryTime();	// 2021.09.03 dahye : TRANSFER_EX4 사양 데이터 반영
		int deliveryWaitTimeOut = trCmd.getDeliveryWaitTimeOut();	// 2021.09.03 dahye : TRANSFER_EX4 사양 데이터 반영
		String waitStartedTime = trCmd.getWaitStartedTime();	// 2022.03.14 dahye : Premove Logic Improve

		// 2011.11.08 by PMM
//		String sourceNode = carrierLocManager.getCarrierLocData(sourceLocId).getNode();
//		String destNode = carrierLocManager.getCarrierLocData(destLocId).getNode();
		CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(sourceLocId);
		String sourceNode = "";
		if (sourceLoc != null) {
			sourceNode = sourceLoc.getNode();
		}
		CarrierLoc destLoc = carrierLocManager.getCarrierLocData(destLocId);
		String destNode = "";
		if (destLoc != null) {
			destNode = destLoc.getNode();
		}
		
		trCmd.setSourceNode(sourceNode);
		trCmd.setDestNode(destNode);
				
		// case1:(DESTCHANGE) or case2:(STAGECHANGE) 
		// case3:(TRANSFERCHANGE)
		if (registeredTrCmd != null) {
			TRCMD_REMOTECMD registeredRemoteCmd = registeredTrCmd.getRemoteCmd();
			String registeredTrCmdId = registeredTrCmd.getTrCmdId();
			String vehicleId = registeredTrCmd.getVehicle();

			// ABORT 작업 : DESTCHANGE 진행 
			if (registeredRemoteCmd == TRCMD_REMOTECMD.ABORT) {
				
				if (vehicleId != null && vehicleId.length() > 0) {
					// TRCMD 테이블 
					// 다른 commandId 로 대체반송 내려올 경우
					if (registeredTrCmdId.equals(trCmdId) == false) {
						trCmdManager.createDESTCHANGECmdCopyingToDB(registeredTrCmd, trCmd);
					} else {
						// 동일 commandId 로 대체반송 내려올 경우
						// 기존소스에 sourceLoc부분 잘못됨
						trCmdManager.updateTrCmdToDB(carrierId, trCmdId, destLocId, destNode);
						// 2021.04.02 by JDH : Transfer Premove 사양 추가
//						String remoteCmd = trCmd.getRemoteCmd().toString();
//						String deliveryType = trCmd.getDeliveryType();
//						trCmdManager.updateTrCmdStatusToDB(trCmdId, null, "UNLOADED", remoteCmd, "FALSE", "NOT ACTIVE", 0, false);
//						trCmdManager.updateTrCmdStatusToDB(trCmdId, null, "UNLOADED", remoteCmd, "FALSE", "NOT ACTIVE", 0, false, deliveryType);
						// 2021.09.03 dahye : TRANSFER_EX4 사양 데이터 반영(ED,DW 추가)
//						trCmdManager.updateTrCmdStatusToDB(trCmdId, null, "UNLOADED", remoteCmd, "FALSE", "NOT ACTIVE", 0, false, deliveryType, expectedDeliveryTime, deliveryWaitTimeOut);
						// 2022.03.14 dahye : Premove Logic Improve
						trCmdManager.updateTrCmdStatusToDB(trCmdId, null, "UNLOADED", remoteCmd, "FALSE", "NOT ACTIVE", 0, false, deliveryType, expectedDeliveryTime, deliveryWaitTimeOut, waitStartedTime);
						
					}
					// VEHICLE 테이블 REQUESTEDTYPE='DESTCHANGE', REQUESTEDDATA=COMMANDID 
					trCmdManager.updateTrCmdChangedInfoToDB(registeredTrCmdId, DESTCHANGE, trCmdId);
					writeSEMLog("UPDATE TrCmd SET CHANGEDREMOTECMD='DESTCHANGE', CHANGEDTRCMDID='" + trCmdId + "' WHERE TRCMDID='" + registeredTrCmdId + "'");
				}
				// 2012.12.28 by KYK : Operation 에서 정리하도록 함
//				sendS6F11("Transferring", trCmdId, "", 0);
				return true;				
			} else if (registeredRemoteCmd == TRCMD_REMOTECMD.STAGE) {
				// STAGE 작업 : STAGECHAGE 

				/*
				 * Case : Loading 시 JobAssign 에서 Requested 정보 기록함,BUT Operation Stage 명령을 아직 안가져간 경우
				 * 1)(상황) OHT Loading 작업 수행중 : 해당 호기의 RequestedType = 'STAGE', RequestedData = 'CarrierID001'
				 * TrCmd 테이블의 Carrier001에 대한 반송명령에는 Vehicle 정보가 없는 상태
				 * 2) 이때  MCS로부터 Carrier001 대한 Transfer 명령 수신시 : Stage 명령 삭제, Transfer 명령 DB 등록
				 */				
				
				// Abnormal Case
				if (vehicleId != null && vehicleId.length() == 0) {
					String stageVehicleId = vehicleManager.getStageAssignedVehicleFromDB(carrierId);
					writeSEMLog("STAGE_DEBUG - TRCMD's VEHICLE is null: " + carrierId + ", " + registeredTrCmdId + ", " + stageVehicleId + ", ");

					// 기존 STAGE 명령을 삭제하고  TRANSFER 명령이 생성되도록 함.
					trCmdManager.updateTrCompletionHistoryDB(registeredTrCmd, STAGEDELETE);
					trCmdManager.deleteSTAGECmdFromDB(registeredTrCmdId);

					writeSEMLog("STAGE CANCEL by Remote Command TRANSFER(Stage Notassigned): " + carrierId + ", " + vehicleId + ", ");					
				} else {
					// Normal Case
					if (registeredTrCmdId.equals(trCmdId) == false) {
						trCmdManager.createSTAGECHANGECmdCopyingToDB(registeredTrCmd, trCmd);
					} else {
						trCmdManager.updateTrCmdToDB(carrierId, trCmdId, destLocId, destNode);
						// 2022.03.14 dahye : Premove Logic Improve
						trCmdManager.updateTrCmdStatusToDB(trCmdId, null, null, remoteCmd, null, null, 0, false, deliveryType, expectedDeliveryTime, deliveryWaitTimeOut, waitStartedTime);
					}
					// Vehicle 테이블 REQUESTEDTYPE='STAGECHANGE', REQUESTEDDATA='TransferCommnadID'; /
//					vehicleManager.updateVehicleRequestedInfoToDB(STAGECHANGE, trCmdId, vehicleId);
//					writeSEMLog("UPDATE Vehicle SET REQUESTEDTYPE='STAGECHANGE'");
					trCmdManager.updateTrCmdChangedInfoToDB(registeredTrCmdId, STAGECHANGE, trCmdId);
					writeSEMLog("UPDATE TrCmd SET CHANGEDREMOTECMD='STAGECHANGE', CHANGEDTRCMDID='" + trCmdId + "' WHERE TRCMDID='" + registeredTrCmdId + "'");
					return true;
				}				
			}			
		}

		// 여기처 체크하는 이유 : DestChange 시 sourceLoc = vehiclePort 로 node = null 발생
		if (sourceNode == null || (sourceNode != null && sourceNode.length() == 0) ||
				destNode == null || (destNode != null && destNode.length() == 0)) {
			StringBuilder sb = new StringBuilder("Fail CreateJobInfo()-SourceNode or DestNode is Null. ");
			sb.append("sourceLoc=").append(sourceLocId);
			sb.append(",sourceNode=").append(sourceNode);
			sb.append("/destLoc=").append(destLocId);
			sb.append(",destNode=").append(destNode);			
			writeSEMLog(sb.toString());
			return false;
		}					
		
		// newTr : commandId, rCmd, trCmdStatus, detailTrCmdStatus,carrierId, sourcePort, 
		//  destPort, sourcePort, sourceNode, destNode, priority, replace
		return trCmdManager.createTRANSFERCmdToDB(trCmd);
	}

	public CARRIERLOC_TYPE getPortType(String carrierLocId) {
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(carrierLocId);		
		if (carrierLoc != null) {
			return carrierLoc.getType();
		} else {
			return CARRIERLOC_TYPE.NULL;
		}
	}

	public boolean isCarrierLocAvailable(String carrierLocId) {
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(carrierLocId);
		if (carrierLoc == null) {
			return false;
		}
		return true;		
	}

	/**
	 * 2013.07.12 by KYK
	 * @param carrierLocId
	 * @param isSourceLoc
	 * @return
	 */
	public int checkCarrierLocAvailable(String carrierLocId, boolean isSourceLoc) {
		int checkCode = 4;
		StringBuilder sb = new StringBuilder();
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(carrierLocId);
		if (carrierLoc == null) {
			// Unregistered CarrierLoc
			if (isSourceLoc) {
				sb.append("[RCV S2F49] Invalid (Unregistered) SourcePort=").append(carrierLocId);
				writeSEMLog(sb.toString());
				return HCACK_UNREGISTERED_SOURCELOC;				
			} else {
				sb.append("[RCV S2F49] Invalid (Unregistered) DestPort=").append(carrierLocId);
				writeSEMLog(sb.toString());
				return HCACK_UNREGISTERED_DESTLOC;
			}
		} else {
			// Unready CarrierLoc
			// 2016.06.01 by KBS : V7인 경우 Invalid Teaching Port 체크하도록
			if (carrierLoc.isValid() == false && vehicleCommType == VEHICLECOMM_TYPE.VEHICLECOMM_BYTE) {
				if (isSourceLoc) {
					sb.append("[RCV S2F49] Invalid (Teaching value) SourcePort=").append(carrierLocId);
					writeSEMLog(sb.toString());
					return HCACK_UNREADY_SOURCELOC;				
				} else {
					sb.append("[RCV S2F49] Invalid (Teaching value) DestPort=").append(carrierLocId);
					writeSEMLog(sb.toString());
					return HCACK_UNREADY_DESTLOC;
				}
			}			
		}
		
		String stationId = carrierLoc.getStationId();
		if (stationId != null && stationId.length() > 0) {
			Station station = stationManager.getStation(stationId);
			if (station == null) {
				// Unregistered Station
				if (isSourceLoc) {
					sb.append("[RCV S2F49] Invalid (Unregistered) SourceStation=").append(stationId);
					sb.append(" /Port=").append(carrierLocId);
					writeSEMLog(sb.toString());
					return HCACK_UNREGISTERED_SOURCESTATION;				
				} else {
					sb.append("[RCV S2F49] Invalid (Unregistered) DestStation=").append(stationId);
					sb.append(" /Port=").append(carrierLocId);
					writeSEMLog(sb.toString());
					return HCACK_UNREGISTERED_DESTSTATION;
				}
			} else {
				// Unready Station
				if (station.getOffset() < 0) {
					if (isSourceLoc) {
						sb.append("[RCV S2F49] Invalid (offset<0) SourceStation=").append(stationId);
						sb.append(" /Port=").append(carrierLocId);
						writeSEMLog(sb.toString());
						return HCACK_UNREADY_SOURCESTATION;
					} else {
						sb.append("[RCV S2F49] Invalid (offset<0) DestStation=").append(stationId);
						sb.append(" /Port=").append(carrierLocId);
						writeSEMLog(sb.toString());
						return HCACK_UNREADY_DESTSTATION;
					}
				}
			}
		}
		return checkCode;
	}

	/**
	* @author : Jongwon Jung
	* @date : 2021. 3. 3.
	* @description : TrUpdate CarrierLoc 체크
	* @param carrierLocId
	* @param isSourceLoc
	* @return
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 3. 3. Jongwon 최초 생성 */
	public int checkTrUpdateCarrierLocAvailable(String carrierLocId, boolean isSourceLoc) {
		int checkCode = 4;
		StringBuilder sb = new StringBuilder();
		CarrierLoc carrierLoc = carrierLocManager.getCarrierLocData(carrierLocId);
		if (carrierLoc == null) {
			// Unregistered CarrierLoc
			if (isSourceLoc) {
				sb.append("[RCV S2F41] Invalid (Unregistered) SourcePort=").append(carrierLocId);
				writeSEMLog(sb.toString());
				return HCACK_UNREGISTERED_SOURCELOC;				
			} else {
				sb.append("[RCV S2F41] Invalid (Unregistered) DestPort=").append(carrierLocId);
				writeSEMLog(sb.toString());
				return HCACK_UNREGISTERED_DESTLOC;
			}
		} else {
			// Unready CarrierLoc
			// 2016.06.01 by KBS : V7인 경우 Invalid Teaching Port 체크하도록
			if (carrierLoc.isValid() == false && vehicleCommType == VEHICLECOMM_TYPE.VEHICLECOMM_BYTE) {
				if (isSourceLoc) {
					sb.append("[RCV S2F41] Invalid (Teaching value) SourcePort=").append(carrierLocId);
					writeSEMLog(sb.toString());
					return HCACK_UNREADY_SOURCELOC;				
				} else {
					sb.append("[RCV S2F41] Invalid (Teaching value) DestPort=").append(carrierLocId);
					writeSEMLog(sb.toString());
					return HCACK_UNREADY_DESTLOC;
				}
			}			
		}
		
		String stationId = carrierLoc.getStationId();
		if (stationId != null && stationId.length() > 0) {
			Station station = stationManager.getStation(stationId);
			if (station == null) {
				// Unregistered Station
				if (isSourceLoc) {
					sb.append("[RCV S2F41] Invalid (Unregistered) SourceStation=").append(stationId);
					sb.append(" /Port=").append(carrierLocId);
					writeSEMLog(sb.toString());
					return HCACK_UNREGISTERED_SOURCESTATION;				
				} else {
					sb.append("[RCV S2F41] Invalid (Unregistered) DestStation=").append(stationId);
					sb.append(" /Port=").append(carrierLocId);
					writeSEMLog(sb.toString());
					return HCACK_UNREGISTERED_DESTSTATION;
				}
			} else {
				// Unready Station
				if (station.getOffset() < 0) {
					if (isSourceLoc) {
						sb.append("[RCV S2F41] Invalid (offset<0) SourceStation=").append(stationId);
						sb.append(" /Port=").append(carrierLocId);
						writeSEMLog(sb.toString());
						return HCACK_UNREADY_SOURCESTATION;
					} else {
						sb.append("[RCV S2F41] Invalid (offset<0) DestStation=").append(stationId);
						sb.append(" /Port=").append(carrierLocId);
						writeSEMLog(sb.toString());
						return HCACK_UNREADY_DESTSTATION;
					}
				}
			}
		}
		return checkCode;
	}

	/**
	 * S2F41 : RESUME 명령 수신
	 * @param message
	 */
	public void receiveRESUMECommand(UComMsg message) {
		int hostCommandAck = 4;
		boolean result;
		// "UPDATE OCSINFO SET VALUE='RESUME' WHERE NAME='OCSCONTROL'"		
		result = updateOcsInfoToDB("OCSCONTROL", RESUME);

		if (result == true) {
			tscStatus = TSC_AUTO;
			ibsemManager.startReport();
		} else {
			hostCommandAck = 2; // 2 = currently not able to execute
			String strLog = "[RCV S2F41] fail to execute RESUME Command.";
			writeSEMLog(strLog);	
			updateSEMHistory(RECEIVED, 2, 41, strLog);
		}		
		sendS2F42(message, hostCommandAck);
	}

	/**
	 * S2F41 : PAUSE 명령 수신
	 * @param message
	 */
	public void receivePAUSECommand(UComMsg message) {
		String strLog;
		int hostCommandAck = 4;
		boolean result;
		// "UPDATE OCSINFO SET VALUE='PAUSE' WHERE NAME='OCSCONTROL'"	
		result = updateOcsInfoToDB("OCSCONTROL", PAUSE);
			
		if (result == true) {
			strLog = "[RCV S2F41] execute PAUSE Command.";
		} else {
			hostCommandAck = 2; // 2 = currently not able to execute
			strLog = "[RCV S2F41] fail to execute PAUSE Command.";
		}		
		writeSEMLog(strLog);	
		updateSEMHistory(RECEIVED, 2, 41, strLog);
		
		sendS2F42(message, hostCommandAck);
	}

	/**
	* @author : Jongwon Jung
	* @date : 2021. 1. 29.
	* @description : TRANSFER UPDATE Cmd 명령 수신
	* @param message
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 1. 29. Jongwon 최초 생성 */
	public void receiveTRANSFERUPDATECommand(UComMsg message) {
		int itemCount = 0;
		int commandParameterAck = 0;
		int hostCommandAck = 4;
		int priority = 0;
		int registeredPriority = 0;
		String commandParameterName;
		String str;
		String strLog;
		String carrierId = null;
		String commandId;
		String destLocId = null;
		String floorId = null;
		
		String registeredCarrierId;
		String registeredDestport;
		String registeredDestnode;
		
		TrCmd trCmd = new TrCmd();
		TRCMD_REMOTECMD remoteCmd = TRCMD_REMOTECMD.TRANSFERUPDATE;
		trCmd.setRemoteCmd(remoteCmd);
		
		TrCmd registeredTrCmd = null;
		
//		itemCount = message.getListItem(); // L, 2
//		commandParameterName = message.getAsciiItem(); // CPNAME1 : "TRANSFERUPDATE"
		itemCount = message.getListItem(); // L, n
		itemCount = message.getListItem(); // L, 2
		str = message.getAsciiItem(); //  CPNAME : "COMMANDID"
		if (COMMANDID.equals(str) == false) {
			hostCommandAck = 3;
			// 2011.11.18 by PMM
			commandParameterAck = 2;
			strLog = "[RCV S2F41] Invalid CPNAME";
			writeSEMLog(strLog);
			updateSEMHistory(RECEIVED, 2, 41, strLog);
			sendS2F42(message, hostCommandAck, commandParameterAck);
			return;
		} else {
			// Get CommandId 
			commandId = message.getAsciiItem();
			// SELECT * FROM TRCMD WHERE TRCMDID=?
 			writeSEMLog("          [COMMANDID]:" + commandId);
			trCmd.setTrCmdId(commandId);
			// Get Priority 
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'PRIORITY'
			if (PRIORITY.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				priority = message.getU2Item();
				trCmd.setPriority(priority);
			} else {
				hostCommandAck = 3;
				strLog = "[RCV S2F41] Invalid PRIORITY:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck, commandParameterAck);
				return;
			}
			
			// Get DestPort 
			itemCount = message.getListItem();
			commandParameterName = message.getAsciiItem();
			if (DESTPORT.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				destLocId = message.getAsciiItem();
				trCmd.setDestLoc(destLocId);
				if(!commandId.equals("") && destLocId.equals("")){
					hostCommandAck = 4;
				}else{
					hostCommandAck = checkTrUpdateCarrierLocAvailable(destLocId, false);
				}
				if (hostCommandAck != 4) {
					sendS2F42(message, hostCommandAck, commandParameterAck);
					return;
				}
			} else {
				hostCommandAck = 3;
				strLog = "[RCV S2F41] Invalid DESTPORT:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck, commandParameterAck);
				return;
			}
			
			// Get CarrierId
			itemCount = message.getListItem(); // L, 2
			commandParameterName = message.getAsciiItem(); // CPNAME : 'CARRIERID'			
			if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
				carrierId = message.getAsciiItem();
				if (carrierId != null && carrierId.length() > 64) {
					hostCommandAck = 3;
					
					strLog = "[RCV S2F41] Invalid CarrierID:" + carrierId + " over 64byte/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 41, strLog);
					sendS2F42(message, hostCommandAck, commandParameterAck);
					return;
				}
				
				if (isTransferUpdateCarrierIdDuplicated(commandId,carrierId)) {
					hostCommandAck = HCACK_CARRIERID_DUPLICATE;
					
					strLog = "[RCV S2F41]: Invalid CarrierID or Invalid TrCmdStatus/CarrierID=" + carrierId + "/HCACK=" + hostCommandAck;
					writeSEMLog(strLog);
					updateSEMHistory(RECEIVED, 2, 49, strLog);
					sendS2F42(message, hostCommandAck, commandParameterAck);
					return;
				}
				trCmd.setCarrierId(carrierId);
			} else {
				hostCommandAck = 3;
				strLog = "[RCV S2F41] Invalid CARRIERID:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck, commandParameterAck);
				return;
			}
			
			// Get FloorId
			itemCount = message.getListItem();
			commandParameterName = message.getAsciiItem();
			if (FLOORID.equals(commandParameterName) && itemCount == 2) {
				// Normal case
				floorId = message.getAsciiItem();
//				trCmd.setDestLoc(floorId);
			} else {
				hostCommandAck = 3;
				strLog = "[RCV S2F41] Invalid FLOORID:" + commandParameterName + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck, commandParameterAck);
				return;
			}
			
			// 쿼리 : SELECT * FROM TRCMD WHERE CARRIERID='carrierId' 
//			registeredTrCmd = trCmdManager.getTrCmdFromDBWhereCarrierId(carrierId);
			registeredTrCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(commandId);
			
			
			if (registeredTrCmd != null) {
				// Normal case
				registeredDestport = registeredTrCmd.getDestLoc();
				registeredDestnode = registeredTrCmd.getDestNode();
				registeredPriority = registeredTrCmd.getPriority();
				registeredCarrierId = registeredTrCmd.getCarrierId();
			} else {
				hostCommandAck = 3;
				strLog = "[RCV S2F41] Update Command ID is null" + "[COMMANDID]:" + commandId + "/HCACK=" + hostCommandAck;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck, commandParameterAck);
				return;
			}
			
			// 2021.08.10 by JJW 기존 데이터를 Old 데이터로 Set
			trCmd.setOldDestLoc(registeredDestport);
			trCmd.setOldDestNode(registeredDestnode);
			trCmd.setOldPriority(registeredPriority);
			trCmd.setOldCarrierId(registeredCarrierId);
			
			// 명령 받은 destLoc에 해당하는 destNode 값을 가져옴
			CarrierLoc destLoc = carrierLocManager.getCarrierLocData(destLocId);
			String destNode = "";
			if (destLoc != null) {
				destNode = destLoc.getNode();
			}
			
			if(priority == 0){ // 명령 받은 priority 값이 0일 경우에는 DB에 등록 된 priority 값으로 사용한다.
				trCmd.setPriority(registeredPriority);
			} else{ // 명령 받은 priority 값이 0이 아닐 경우
				if(priority != registeredPriority){
					trCmd.setPriority(priority);
				} else {
					trCmd.setPriority(registeredPriority);
				}
			}
			
			if(destLocId.equals("")){ // 명령 받은 destLocId 값이 null일 경우에는 DB에 등록 된 destLocId 값으로 사용한다.
				trCmd.setDestLoc(registeredDestport);
				trCmd.setDestNode(registeredDestnode);
			} else{ // 명령 받은 destLocId 값이 null이 아닐 경우
				if(destLocId.equals(registeredDestport)){ // 기존과 동일하면 기존 값으로 set
					trCmd.setDestLoc(registeredDestport);
					trCmd.setDestNode(registeredDestnode);
				} else { // 기존과 다르면 명령 받은 값으로 set
					trCmd.setDestLoc(destLocId);
					trCmd.setDestNode(destNode);
				}
			}
			
			if(carrierId.equals("")){ // 명령 받은 carrierId 값이 null일 경우에는 DB에 등록 된 carrierId 값으로 사용한다. 
				trCmd.setCarrierId(registeredCarrierId);
			} else{ // 명령 받은 carrierId 값이 null이 아닐 경우
				if(carrierId.equals(registeredCarrierId)){ // 기존과 동일하면 기존 값으로 set
					trCmd.setCarrierId(registeredCarrierId);
				} else { // 기존과 다르면 명령 받은 값으로 set
					trCmd.setCarrierId(carrierId);
				}
			}
			
			// Normal Case
			if (isAbleToTRANSFERUPDATECmd(commandId)) {
				setTransferUpdateCommandToDB(commandId, registeredTrCmd, trCmd);
			} else {
				hostCommandAck = 2;	
				commandParameterAck = 2;
				strLog = "[RCV S2F41] fail to execute TRANSFERUPDATE Command - Invalid Command Status/COMMANDID:" + commandId;
				writeSEMLog(strLog);
				updateSEMHistory(RECEIVED, 2, 41, strLog);
				sendS2F42(message, hostCommandAck, commandParameterAck);
				return;
			}
			
		}
		sendS2F42(message, hostCommandAck, commandParameterAck);
	}

	public boolean isAbleToCANCELCmd(String trCmdId) {
		TRCMD_STATE state;
		TRCMD_DETAILSTATE detailState;
		TRCMD_REMOTECMD changedRemoteCmd;
		TrCmd trCmd;

		if (REMOTE_ONLINE.equals(controlStatus) == false) {
			return false; //  HCACK : Currently not able to execute
		}

		//"SELECT * FROM TRCMD WHERE TRCMDID=?;
		trCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(trCmdId);
		if (trCmd == null) {
			return false;
		} else {
			state = trCmd.getState();
			detailState = trCmd.getDetailState();
			changedRemoteCmd = trCmd.getChangedRemoteCmd();
			// 2011.11.21 by KYK : NULL 이 아니면 이전 요청을 Operation 이 아직 처리완료하지 않은 상태로 추가수신 NAK
			// 배경 : STAGECHANGE 후 곧바로 CANCEL 수신, STAGECHANGE 정리후 NULL 리셋 시, CANCEL 삭제되는 케이스 발생 
			if (changedRemoteCmd != TRCMD_REMOTECMD.NULL){
				return false;
			}
			
			if (state == TRCMD_STATE.CMD_QUEUED 
					|| state == TRCMD_STATE.CMD_WAITING 
					|| state == TRCMD_STATE.CMD_TRANSFERRING) {
				if (detailState == TRCMD_DETAILSTATE.NOT_ASSIGNED 
						|| detailState == TRCMD_DETAILSTATE.UNLOAD_ASSIGNED) {
					return true; // ACK : Cancel Initiated
				} else if (detailState == TRCMD_DETAILSTATE.UNLOAD_SENT 
						|| detailState == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED) {
					// Going 중 Next Unload 전송 후 Vehicle Unload 위치 도착 유무를 체크하여 Cancel 수용 
					if (isVehicleArrivedAtSourceNode(trCmdId)) {
						return false; // CPACK    : currently not able to execute ( "Abort" cmd should be received at this time. )
					} else {
						return true; // ACK : Cancel Initiated						
					}
				} else {
					return false; 
				}
			} else {				
				return false; //Invalid Status		 		
			}
		}
	}

	private boolean isVehicleArrivedAtSourceNode(String trCmdId) {
		return trCmdManager.isVehicleArrivedAtSourceNode(trCmdId);
	}

	public boolean isAbleToABORTCmd(String trCmdId) {
		TRCMD_STATE state;
		TRCMD_DETAILSTATE detailState;
		TRCMD_REMOTECMD changedRemoteCmd;
		TrCmd trCmd;

		if (REMOTE_ONLINE.equals(controlStatus) == false) {
			return false; //  HCACK : Currently not able to execute
		}

		// SELECT * FROM TRCMD WHERE TRCMDID=?
		trCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(trCmdId);
		if (trCmd == null) {
			return false;
		} else {
			state = trCmd.getState();
			detailState = trCmd.getDetailState();
			changedRemoteCmd = trCmd.getChangedRemoteCmd();
			// 2011.11.21 by KYK : NULL 이 아니면 이전 요청을 Operation 이 아직 처리완료하지 않은 상태로 추가수신 NAK
			// 배경 : STAGECHANGE 후 곧바로 CANCEL 수신, STAGECHANGE 정리후 NULL 리셋 시, CANCEL 삭제되는 케이스 발생 
			if (changedRemoteCmd != TRCMD_REMOTECMD.NULL){
				return false;
			}
			
			if (state == TRCMD_STATE.CMD_QUEUED || state == TRCMD_STATE.CMD_WAITING) {
				return false;
//			} else if (state == TRCMD_STATE.CMD_TRANSFERRING) {
			} else if (state == TRCMD_STATE.CMD_TRANSFERRING || state == TRCMD_STATE.CMD_PREMOVE) {	// 2021.04.02 by JDH : Transfer Premove 사양 추가
				if (detailState == TRCMD_DETAILSTATE.UNLOAD_SENT 
						|| detailState == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED
						|| detailState == TRCMD_DETAILSTATE.UNLOADING
						|| detailState == TRCMD_DETAILSTATE.LOADING
						|| detailState == TRCMD_DETAILSTATE.LOADED) {
					return false;
				} else if (detailState == TRCMD_DETAILSTATE.LOAD_SENT || detailState == TRCMD_DETAILSTATE.LOAD_ACCEPTED) {
					// Going 중 Next Unload 전송 후 Vehicle Unload 위치 도착 유무를 체크하여 Cancel 수용/
					if (isVehicleArrivedAtDestNode(trCmdId)) {
						return false; // CPACK    : currently not able to execute ( "Abort" cmd should be received at this time. )
					}
				}
				return true; // ACK : Cancel Initiated	
			} else if (state == TRCMD_STATE.CMD_PAUSED) {
				return false;
			} else {
				return false; //Invalid Status
			}
		}
	}

	/**
	* @author : Jongwon Jung
	* @date : 2021. 1. 29.
	* @description : TRANSFER UPDATE Cmd 가능 여부 판단 
	* @param trCmdId
	* @return
	* ===========================================================
	* DATE AUTHOR NOTE
	* -----------------------------------------------------------
	* 2021. 1. 29. Jongwon 최초 생성 */
	public boolean isAbleToTRANSFERUPDATECmd(String trCmdId) {
		TRCMD_STATE state;
		TRCMD_DETAILSTATE detailState;
		TRCMD_REMOTECMD changedRemoteCmd;
		TrCmd trCmd;

		if (REMOTE_ONLINE.equals(controlStatus) == false) {
			return false; //  HCACK : Currently not able to execute
		}
		// SELECT * FROM TRCMD WHERE TRCMDID=?
		trCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(trCmdId);
		if (trCmd == null) {
			return false;
		} else {
			state = trCmd.getState();
			detailState = trCmd.getDetailState();
			changedRemoteCmd = trCmd.getChangedRemoteCmd();
			// 2011.11.21 by KYK : NULL 이 아니면 이전 요청을 Operation 이 아직 처리완료하지 않은 상태로 추가수신 NAK
			// 배경 : STAGECHANGE 후 곧바로 CANCEL 수신, STAGECHANGE 정리후 NULL 리셋 시, CANCEL 삭제되는 케이스 발생 
			if (changedRemoteCmd != TRCMD_REMOTECMD.NULL){
				return false;
			}
			
			if (state == TRCMD_STATE.CMD_QUEUED || state == TRCMD_STATE.CMD_WAITING) {
				return true;
			} else if (state == TRCMD_STATE.CMD_TRANSFERRING) {
				if (
//						detailState == TRCMD_DETAILSTATE.UNLOAD_SENT 
//						|| detailState == TRCMD_DETAILSTATE.UNLOAD_ACCEPTED
//						|| detailState == TRCMD_DETAILSTATE.UNLOADING || 
						detailState == TRCMD_DETAILSTATE.LOADING
						|| detailState == TRCMD_DETAILSTATE.LOADED) {
					return false;
				} else if (detailState == TRCMD_DETAILSTATE.LOAD_SENT || detailState == TRCMD_DETAILSTATE.LOAD_ACCEPTED) {
					// Going 중 Next Unload 전송 후 Vehicle Unload 위치 도착 유무를 체크하여 Cancel 수용/
					if (isVehicleArrivedAtDestNode(trCmdId)) {
						return false; // CPACK    : currently not able to execute ( "Abort" cmd should be received at this time. )
					}
				}
				return true; // ACK : Cancel Initiated	
			} else if (state == TRCMD_STATE.CMD_PAUSED) {
				return false;
			} else {
				return false; //Invalid Status
			}
		}
	}

	private boolean isVehicleArrivedAtDestNode(String trCmdId) {		
		return trCmdManager.isVehicleArrivedAtDestNode(trCmdId);
	}

	/**
	 * 2013.07.01 by KYK : TrCmdId Duplicated case 통합
	 * @param trCmdId
	 * @param remoteCmd
	 * @param carrierId
	 * @return
	 */
	public boolean isTrCmdIdDuplicated(String trCmdId, String remoteCmd, String carrierId) {
		return trCmdManager.checkDuplicatedTrCmdFromDB(trCmdId, remoteCmd, carrierId);
	}
	
//	public boolean isTrCmdIdDuplicated(String trCmdId, String remoteCmd) {
//		//SELECT * FROM TRCMD WHERE TRCMDID=?
//		//return trCmdManager.isTrCmdIdDuplicated(commandId,rCmd);
//		String duplicatedRemoteCmd = trCmdManager.getTrCmdFromDBWhere(trCmdId, remoteCmd);
//		if (duplicatedRemoteCmd == null) {
//			return false; // Not Duplicated
//		} else {
//			if (remoteCmd == null) {
//				if ("STAGE".equals(duplicatedRemoteCmd) || "ABORT".equals(duplicatedRemoteCmd)) {
//					return false; // Not Duplicated
//				} else {
//					return true; // Duplicated
//				}
//			} else {
//				return true; // Duplicated
//			}
//		}
//	}
//
//	public boolean isTrCmdIdDuplicated(String trCmdId) {
//		//SELECT * FROM TRCMD WHERE TRCMDID=?
//		//return trCmdManager.isTrCmdIdDuplicated(commandId);
//		TrCmd trCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(trCmdId);
//		if (trCmd == null) {
//			return false;
//		} else {
//			return true;
//		}
//	}
	
	
	public boolean isCarrierIdDuplicated(String carrierId) {		
		//SELECT * FROM TRCMD WHERE CARRIERID=? AND (REMOTECMD=? OR STATUS='CMD_CANCELLING' OR STATUS='CMD_ABORTING')
		return trCmdManager.isCarrierIdDuplicated(carrierId, TRANSFER);		
	}

	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	public boolean isCarrierIdDuplicated(String carrierId , String remotecmd) {		
		//SELECT * FROM TRCMD WHERE CARRIERID=? AND (REMOTECMD=? OR STATUS='CMD_CANCELLING' OR STATUS='CMD_ABORTING')
		return trCmdManager.isCarrierIdDuplicated(carrierId, remotecmd);		
	}

	private boolean isScanCarrierIdDuplicated(String carrierId) {
		return trCmdManager.isCarrierIdDuplicated(carrierId, SCAN);
	}

	private boolean isStageCarrierIdDuplicated(String carrierId) {
		return trCmdManager.isCarrierIdDuplicated(carrierId, STAGE);
	}

	public boolean isTransferUpdateCarrierIdDuplicated(String commandId, String carrierId) {		
		//SELECT * FROM TRCMD WHERE CARRIERID=? AND (REMOTECMD=? OR STATUS='CMD_CANCELLING' OR STATUS='CMD_ABORTING') AND TRCMDID !=? 
		return trCmdManager.isCarrierIdDuplicated(commandId, carrierId, TRANSFERUPDATE);		
	}

	@Override	
	public int setEcVData(int vId, UComMsg message) {
		if (ecIdOTable.containsKey(vId)) {
			int value;
			int min;
			int max;
			String name;
			EquipmentConstant equipmentConstant = ecIdOTable.get(vId);
			
			// Establish Communication Timeout  U2
			if (vId == 2) {
				value = message.getU2Item();
				max = equipmentConstant.getMax();
				min = equipmentConstant.getMin();
				
				if (value == 0) {
					return 5;
				} else {
					if (min <= value && value <= max) {
						equipmentConstant.setNumValue(value);
						ecIdOTable.put(vId, equipmentConstant);
						return IB_NO_ERROR;
					} else {
						return 3; // beyond the allowed range
					}
				}
			} else if (vId == 61) {
				// EqpName A[1~80]
				name = message.getAsciiItem();
				if (name == null) {
					return 5;
				} else {
					equipmentConstant.setStrValue(name);
					return IB_NO_ERROR;
				}
			}
		}
		return ER_UNDEFINED_ECID;
	}
	
	/**************************************************
	 * implements abstract methods from SEMStandard	   * 
	 ***************************************************/
	@Override
	public UComMsg setActiveCarriersToSecsMsg(UComMsg response) {
		// SELECT CARRIERID FROM TRCMD WHERE CARRIERID IS NOT NULL
		int itemCount = 0;
		TrCmd trCmd;
		
		if (!trCmdManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying ActiveCarriers");
			return null;
		}

		HashMap<String, TrCmd> trCmdTable = trCmdManager.getTrCmdFromDBCarrierIdNotNull();
		if (trCmdTable == null) {
			return response;
		}
				
		itemCount = trCmdTable.size();		
		response.setListItem(itemCount);
		
		if (itemCount > 0) {
			Iterator<TrCmd> iter = trCmdTable.values().iterator();
			while (iter.hasNext()) {
				trCmd = (TrCmd) iter.next();				
				response.setListItem(3);
				response.setAsciiItem(trCmd.getCarrierId());
				response.setAsciiItem(trCmd.getVehicle());
				response.setAsciiItem(trCmd.getCarrierLoc());
			}	
		}
		String strLog = "	ActiveCarriers Count: " + itemCount;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 4, strLog);
		writeSEMLog("	SELECT * FROM TRCMD WHERE CARRIERID IS NOT NULL");
		
		return response;
	}
	
	@Override
	public UComMsg setActiveCarriers2ToSecsMsg(UComMsg response) {
		// TODO Auto-generated method stub
		return null;
	}	
	
	@Override
	public UComMsg setActiveTransfersToSecsMsg(UComMsg response) {
		//"SELECT TRCMDID,PRIORITY,REPLACE,CARRIERID,SOURCELOC,DESTLOC FROM TRCMD WHERE TRCMDID IS NOT NULL"
		int itemCount = 0;
		TrCmd trCmd;
		
		if (!trCmdManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying ActiveTransfers");
			return null;
		}

		HashMap<String, TrCmd> trCmdTable = trCmdManager.getTrCmdFromDBTrCmdIdNotNull();
		if (trCmdTable == null) {
			return response;
		}
		
		itemCount = trCmdTable.size();		
		response.setListItem(itemCount);

		if (itemCount > 0) {
			Iterator<TrCmd> iter = trCmdTable.values().iterator();
			while (iter.hasNext()) {
				trCmd = iter.next();				
				response.setListItem(2);
				// CommandInfo : 59
				response.setListItem(3);
				response.setAsciiItem(trCmd.getTrCmdId());
				response.setU2Item(trCmd.getPriority());
				response.setU2Item(trCmd.getReplace());
				// TransferInfo : 67
				// 2015.08.31 by KBS : format에 맞춰 수정
				response.setListItem(3);
				response.setAsciiItem(trCmd.getCarrierId());
				response.setAsciiItem(trCmd.getSourceLoc());
				response.setAsciiItem(trCmd.getDestLoc());			
			}		
		}		
		String strLog = "	ActiveTransfers Count: " + itemCount;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 4, strLog);
		writeSEMLog("	SELECT * FROM TRCMD WHERE TRCMDID IS NOT NULL");
		
		return response;
	}

	@Override
	public UComMsg setActiveVehiclesToSecsMsg(UComMsg response) {
		// SELECT VEHICLEID,STATUS FROM VEHICLE WHERE ENABLED='TRUE'
		int itemCount = 0;
		String vehicleId;
		char state;
		Vehicle vehicle;
		
		if (!trCmdManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying ActiveVehicles");
			return null;
		}

		HashMap<String, Vehicle> vehicleTable = vehicleManager.getActiveVehiclesFromDB();
		itemCount = vehicleTable.size();		
		response.setListItem(itemCount);

		if (itemCount > 0) {
			Iterator<String> iter = vehicleTable.keySet().iterator();
			while (iter.hasNext()) {
				vehicleId = iter.next();
				vehicle = vehicleTable.get(vehicleId);			
				state = vehicle.getState();
				response.setListItem(2); // L,2
				response.setAsciiItem(vehicleId);
				response.setU2Item(getVehicleStateResult(state));			
			}			
		}
		String strLog = "	ActiveVehicles Count: " + itemCount;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 4, strLog);
		writeSEMLog("	SELECT VEHICLEID,STATUS FROM VEHICLE WHERE ENABLED='TRUE'");
		
		return response;
	}

	private int getVehicleStateResult(char state) {
		switch (state) {
			case 'I':
				return 2;
			case 'G':
				return 3;
			case 'A':
				return 4;
			case 'U':
			case 'N':
				return 5;
			case 'L':
			case 'O':
				return 6;
			default :
				return 1;
		}
	}

	@Override
	public UComMsg setCarrierInfoToSecsMsg(UComMsg response, String carrierId) {
		// SELECT CARRIERID, VEHICLE, CARRIERLOC FROM TRCMD WHERE CARRIERID=?
		TrCmd trCmd = trCmdManager.getTrCmdFromDBWhereCarrierId(carrierId);
		
		response.setListItem(3);
		if (trCmd == null) {
			response.setAsciiItem("");
			response.setAsciiItem("");
			response.setAsciiItem("");			
		} else {
			response.setAsciiItem(trCmd.getCarrierId());
			response.setAsciiItem(trCmd.getVehicle());
			response.setAsciiItem(trCmd.getCarrierLoc());			
		}		
		return response;
	}
	
	@Override
	public UComMsg setTransferInfoToSecsMsg(UComMsg response, String carrierId) {
		// SELECT CARRIERID, SOURCELOC, DESTLOC FROM TRCMD WHERE CARRIERID=?
		TrCmd trCmd = trCmdManager.getTrCmdFromDBWhereCarrierId(carrierId);
		response.setListItem(3);
		if (trCmd == null) {
			response.setAsciiItem("");
			response.setAsciiItem("");
			response.setAsciiItem("");
		} else {
			response.setAsciiItem(trCmd.getCarrierId());
			response.setAsciiItem(trCmd.getSourceLoc());
			response.setAsciiItem(trCmd.getDestLoc());			
		}
		return response;
	}
	
//	@Override
//	public UComMsg setCommandInfoToSecsMsg(UComMsg response, String trCmdId) {
//		// SELECT PRIORITY,REPLACE FROM TRCMD WHERE TRCMDID=?
//		TrCmd trCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(trCmdId);
//
//		response.setListItem(3);
//		if (trCmd == null) {
//			response.setAsciiItem("");
//			response.setU2Item(0);
//			response.setU2Item(0);			
//		} else {
//			response.setAsciiItem(trCmd.getTrCmdId());
//			response.setU2Item(trCmd.getPriority());
//			response.setU2Item(trCmd.getReplace());			
//		}				
//		return response;
//	}
	
	public UComMsg setCommandInfoToSecsMsg(UComMsg response, ReportItems reportItems) {
		response.setListItem(3);
		response.setAsciiItem(reportItems.getCommandId());
		response.setU2Item(reportItems.getPriority());
		response.setU2Item(reportItems.getReplace());			
						
		return response;
	}	
	
	@Override
	public UComMsg setInstallTimeToSecsMsg(UComMsg response, ReportItems reportItems) {
		// SELECT TRQUEUEDTIME FROM TRCMD WHERE TRCMDID=?
		TrCmd trCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(reportItems.getCommandId());
		if (trCmd == null) {
			response.setAsciiItem("");
		} else {
			response.setAsciiItem(trCmd.getTrQueuedTime());
		}
		return response;
	}

	@Override
	public UComMsg setTransferStateToSecsMsg(UComMsg response, String trCmdId) {
		// SELECT STATUS FROM TRCMD WHERE TRCMDID=?
		int stateResult = 0;
		TrCmd trCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(trCmdId);
		if (trCmd == null) {
			response.setListItem(1);
			response.setAsciiItem("");
		} else {
			stateResult = getTrCmdStatusResult(trCmd.getState());
			response.setListItem(1);
			response.setU2Item(stateResult);
		}		
		return response;
	}

	@Override
	public UComMsg setEnhancedCarrierInfoToSecsMsg(UComMsg response, String carrierId) {
		if (!trCmdManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying EnhancedCarrierInfo");
			return null;
		}

		// SELECT CARRIERID, VEHICLE, CARRIERLOC,TRQUEUEDTIME FROM TRCMD WHERE CARRIERID=?
		TrCmd trCmd = trCmdManager.getTrCmdFromDBWhereCarrierId(carrierId);

		response.setListItem(5);
		if (trCmd == null) {
			response.setAsciiItem(carrierId);
			response.setAsciiItem("");
			response.setAsciiItem("");
			response.setAsciiItem("");
			response.setU2Item(0);
		} else {
			response.setAsciiItem(trCmd.getCarrierId());
			// 2016.11.28 by KBS : carrier pick-up이 완료된 경우만 값 설정
			if (!trCmd.getCarrierLoc().equals(trCmd.getSourceLoc()) && !trCmd.getCarrierLoc().equals(trCmd.getDestLoc())) {
				// carrier pick-up completed
				response.setAsciiItem(trCmd.getVehicle());
			} else {
				response.setAsciiItem("");
			}
			response.setAsciiItem(trCmd.getCarrierLoc());
			response.setAsciiItem(trCmd.getTrQueuedTime());
			response.setU2Item(getCarrierType(trCmd));
		}	
		return response;
	}

	@Override
	public UComMsg setEnhancedCarriersToSecsMsg(UComMsg response) {
		int itemCount = 0;
		TrCmd trCmd;
		
		if (!trCmdManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying EnhancedCarriers");
			return null;
		}

		HashMap<String, TrCmd> trCmdTable = trCmdManager.getTrCmdFromDBCarrierIdNotNull();
		if (trCmdTable == null) {
			return response;
		}
		
		itemCount = trCmdTable.size();		
		response.setListItem(itemCount);
		if (itemCount > 0) {
			Iterator<TrCmd> iter = trCmdTable.values().iterator();
			while (iter.hasNext()) {
				trCmd = (TrCmd) iter.next();
				response.setListItem(5);
				response.setAsciiItem(trCmd.getCarrierId());
				// 2016.11.28 by KBS : carrier pick-up이 완료된 경우만 값 설정
				if (!trCmd.getCarrierLoc().equals(trCmd.getSourceLoc()) && !trCmd.getCarrierLoc().equals(trCmd.getDestLoc())) {
					// carrier pick-up completed
					response.setAsciiItem(trCmd.getVehicle());
				} else {
					response.setAsciiItem("");
				}
				response.setAsciiItem(trCmd.getCarrierLoc());
				response.setAsciiItem(trCmd.getTrQueuedTime());
				response.setU2Item(getCarrierType(trCmd));
			}	
		}
		String strLog = "	EnhancedCarriers Count: " + itemCount;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 4, strLog);
		writeSEMLog("	SELECT * FROM TRCMD WHERE CARRIERID IS NOT NULL");		

		return response;
	}

	/**
	 * 2013.09.06 by KYK
	 * @param trCmd
	 * @return
	 */
	private int getCarrierType(TrCmd trCmd) {
		int carrierType = 0;
		String materialType = "";
		if (trCmd != null) {
			CarrierLoc sourceLoc = carrierLocManager.getCarrierLocData(trCmd.getSourceLoc());
			if (sourceLoc != null && CARRIERLOC_TYPE.VEHICLEPORT != sourceLoc.getType()) {
				materialType = sourceLoc.getMaterial();
				carrierType = getCarrierType(materialType);
			} else {
				Vehicle vehicle = vehicleManager.getVehicleFromDB(trCmd.getVehicle());
				if (vehicle != null) {
					// 2016.06.01 by KBS : V1/V7의 CarrierType 정보 구분
					if (vehicleCommType == VEHICLECOMM_TYPE.VEHICLECOMM_BYTE) {
						carrierType = vehicle.getCarrierType();
					} else {
						carrierType = getCarrierType(vehicle.getMaterial());	
					}
				}
			}
		}
		return carrierType;
	}

	/**
	 * 2013.09.06 by KYK
	 * @param carrierType
	 * @return
	 */
	private int getCarrierType(String materialType) {
		// 2015.07.01 by MYM : CarrierTypeConfig에서 처리 변경(CarrierTypeConfig.xml 추가 및 관리)
//		int carrierType = 100;
//		// 0 = FOUP, 1 = POD, 3 = MAC 
//		if (FOUP.equalsIgnoreCase(materialType)) {
//			carrierType = 0;
//		} else if (RETICLE.equalsIgnoreCase(materialType)) {
//			carrierType = 1;
//		} else if (MAC.equalsIgnoreCase(materialType)) {
//			carrierType = 3;
//		}
//		return carrierType;
		return CarrierTypeConfig.getInstance().getCarrierType(materialType);
	}

	@Override
	public UComMsg setEnhancedTransfersToSecsMsg(UComMsg response) {
		int itemCount = 0;
		TrCmd trCmd;
		
		if (!trCmdManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying EnhancedTransfers");
			return null;
		}

		//"SELECT * FROM TRCMD WHERE TRCMDID IS NOT NULL AND REMOTECMD='TRANSFER'"
		HashMap<String, TrCmd> trCmdTable = trCmdManager.getEnhancedTransfersFromDB();
		itemCount = trCmdTable.size();		
		response.setListItem(itemCount);
		if (itemCount > 0) {
			Iterator<TrCmd> iter = trCmdTable.values().iterator();
			while (iter.hasNext()) {
				trCmd = iter.next();
				response.setListItem(3); // L, 3
				// CommandInfo : 59
				response.setListItem(3);
				response.setAsciiItem(trCmd.getTrCmdId());
				response.setU2Item(trCmd.getPriority());
				response.setU2Item(trCmd.getReplace());
				// TransferState
				response.setU2Item(getTrCmdStatusResult(trCmd.getState()));
				// TransferInfo : 67
				response.setListItem(1);
				response.setListItem(3);
				response.setAsciiItem(trCmd.getCarrierId());
				response.setAsciiItem(trCmd.getSourceLoc());
				response.setAsciiItem(trCmd.getDestLoc());		
			}							
		}
		String strLog = "	EnhancedTransfers Count: " + itemCount;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 1, 4, strLog);
		writeSEMLog("	SELECT * FROM TRCMD WHERE TRCMDID IS NOT NULL AND REMOTECMD='TRANSFER'");

		return response;
	}

	private int getTrCmdStatusResult(TRCMD_STATE state) {
		switch (state) {
			case CMD_QUEUED:
				return 1;
			case CMD_TRANSFERRING:
				return 2;
			case CMD_PAUSED:
				return 3;
			case CMD_CANCELLING:
				return 4;
			case CMD_ABORTING:
				return 5;
			case CMD_WAITING:
				return 6;
			case CMD_COMPLETED:
				return 7;
			default:
				return 0;
		}
	}

	// IBSEM 안쓰고 STBC에서 씀
	/** DB : CarrierLoc */
	@Override
	public UComMsg setCurrentPortStateToSecsMsg(UComMsg response) {
		int itemCount = 0;
		String carrierLocId;
		boolean isEnabled;
		
		if (!carrierLocManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying CurrentPortState");
			return null;
		}

		//"SELECT CARRIERLOCID,ENABLED FROM CarrierLoc WHERE (Type = 'STBPORT' OR Type = 'UTBPORT')";
		HashMap<String, CarrierLoc> portStateTable = carrierLocManager.getCurrentPortStateFromCarrierLocDB();
		itemCount = portStateTable.size();
		response.setListItem(itemCount);
		if (itemCount > 0) {
			Iterator<String> iter = portStateTable.keySet().iterator();
			while (iter.hasNext()) {
				carrierLocId = iter.next();
				isEnabled = portStateTable.get(carrierLocId).isEnabled();
				response.setListItem(2);
				response.setAsciiItem(carrierLocId);
				if (isEnabled) {
					response.setU2Item(2);
				} else {
					response.setU2Item(1);
				}
			}
		}		
		return response;
	}

	@Override
	public UComMsg setTSCStatusToSecsMsg(UComMsg response) {
		int stateResult;
		if (tscStatus != null && tscStatus.length() == 0) {
			response.setU2Item(0);
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
			response.setU2Item(stateResult);
		}		
		return response;
	}

	@Override
	public UComMsg setEnhancedTransferCommandToSecsMsg(UComMsg response, String trCmdId) {
		if (!trCmdManager.checkDBStatus()) {
			writeSEMLog("DB connection fail while querying EnhancedTransferCommand");
			return null;
		}

		// SELECT * FROM TRCMD WHERE TRCMDID=?
		TrCmd trCmd = trCmdManager.getTrCmdFromDBWhereTrCmdId(trCmdId);
		if (trCmd == null) {
			response.setListItem(3); // L, 3
			response.setListItem(3); // CommandInfo : 59
			response.setAsciiItem(""); // CommandId
			response.setU2Item(0); // Priority
			response.setU2Item(0); // Replace
			response.setU2Item(0); // TransferState
			response.setListItem(0);
		} else {
			response.setListItem(3); // L, 3
			response.setListItem(3); // CommandInfo : 59
			response.setAsciiItem(trCmd.getTrCmdId()); // CommandId
			response.setU2Item(trCmd.getPriority()); // Priority
			response.setU2Item(trCmd.getReplace()); // Replace
			
			int trCmdState = getTrCmdStatusResult(trCmd.getState());
			response.setU2Item(trCmdState); // TransferState
			response.setListItem(1);
			response.setListItem(3); // TransferInfo(67)
			response.setAsciiItem(trCmd.getCarrierId()); // CarrierID
			response.setAsciiItem(trCmd.getSourceLoc()); // Source
			response.setAsciiItem(trCmd.getDestLoc()); // Dest
		}
		return response;
	}

	/**
	 * 
	 * @param eventName
	 * @param trCmdId
	 * @param vehicleId
	 * @param transferPort
	 * @param carrierId
	 * @return
	 */
	public int sendS6F11Vehicle(String eventName, String trCmdId,
			String vehicleId, String transferPort, String carrierId) {
		ReportItems reportItems = new ReportItems();
		reportItems.setCommandId(trCmdId);
		reportItems.setVehicleId(vehicleId);
		reportItems.setTransferPort(transferPort);
		reportItems.setCarrierId(carrierId);
		
		return sendS6F11(eventName, reportItems);		
	}

	/**
	 * 
	 * @param eventName
	 * @param trCmdId
	 * @param vehicleId
	 * @param carrierId
	 * @param carrierLoc
	 * @return
	 */
	public int sendS6F11Carrier(String eventName, String trCmdId,
			String vehicleId, String carrierId, String carrierLoc, int vehicleType, String foupId) {
		ReportItems reportItems = new ReportItems();
		reportItems.setCommandId(trCmdId);
		reportItems.setVehicleId(vehicleId);
		reportItems.setCarrierId(carrierId);
		reportItems.setCarrierLoc(carrierLoc);
		// 2012.05.16 by MYM : VehicleType 추가
		// Rail-Down - S1a Foup, Reticle 통합 반송시 사양(IBSEM Spec for Conveyor usage in one OHT) 대응
		reportItems.setVehicleType(vehicleType);
		// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
		reportItems.setFoupId(foupId);
		
		return sendS6F11(eventName, reportItems);				
	}

	/**
	 * 
	 * @param eventName
	 * @param trCmdId
	 * @param sourceLoc
	 * @param destLoc
	 * @param carrierLoc
	 * @param carrierId
	 * @param priority
	 * @param replace
	 * @param resultCode
	 * @return
	 */
	public int sendS6F11TrCmd(String eventName, String trCmdId,
			String sourceLoc, String destLoc, String carrierLoc,
			String carrierId, int priority, int replace, int resultCode) {
		ReportItems reportItems = new ReportItems();
		reportItems.setCommandId(trCmdId);
		reportItems.setSourcePort(sourceLoc);
		reportItems.setDestPort(destLoc);
		reportItems.setCarrierLoc(carrierLoc);
		reportItems.setCarrierId(carrierId);
		reportItems.setPriority(priority);
		reportItems.setReplace(replace);
		reportItems.setResultCode(resultCode);
		
		return sendS6F11(eventName, reportItems);					
	}

	/**
	 * 2015.03.04 by MYM : 장애 지역 우회 기능 (PortOutOfService/PortInService)
	 * @param eventName
	 * @param carrierLoc
	 * @return
	 */
	public int sendS6F11Port(String eventName, String carrierLoc) {
		ReportItems reportItems = new ReportItems();
		reportItems.setCarrierLoc(carrierLoc);
		
		return sendS6F11(eventName, reportItems);				
	}
	
	/**
	 * 
	 * @param alarmId
	 * @param vehicleId
	 * @param vehicleState
	 * @param trCmdId
	 * @return
	 */
	public boolean setAlarmReport(long alarmId, String vehicleId, int vehicleState, String trCmdId, String vehicleCurrentDomain,
			int vehicleCurrentPosition, String sourcePort, String destPort) {
		AlarmItem alarmItem;
		if (alarmIdOTable != null) {
			AlarmItem alarm = alarmIdOTable.get(alarmId);
			if (alarm != null) {
				if (alarm.isEnabled()) {
					// Set Alarm
					alarm.setActivated(true);
					alarm.setVehicleId(vehicleId);
					// 2013.10.02 by KYK
					alarm.addVehicleToList(vehicleId);
					// Add into CurrentAlarmList
					alarmItem = new AlarmItem();
					alarmItem.setAlarmId(alarmId);
					alarmItem.setAlarmText(alarm.getAlarmText());
					alarmItem.setEnabled(true);
					alarmItem.setActivated(true);
					alarmItem.setVehicleId(vehicleId);
					currentAlarmList.add(alarmItem);
					// Send S5F1 & S6F11
					sendS5F1(alarmId);			
					sendS6F11Alarm("AlarmSet", trCmdId, vehicleId, vehicleState, (int) alarmId);
					// 2013.10.01 by MYM : UnitAlarmSet Event 추가
					sendS6F11UnitAlarm("UnitAlarmSet", trCmdId, vehicleId, vehicleState, (int) alarmId, vehicleCurrentDomain, vehicleCurrentPosition, sourcePort, destPort);

					// Set OCS AlarmStatus
					tscAlarmStatus = TSC_ALARMS;
					return true;
				} else {
					tscAlarmStatus = TSC_ALARMS;
					StringBuilder sb = new StringBuilder();
					sb.append("Alarm occurs but not enabled [Vehicle:").append(vehicleId);
					sb.append(", Alarm:").append(alarmId).append("]");
					writeSEMLog(sb.toString());
					// Exception 로그 추가
					traceException(sb.toString());
					return false;
				}
			}			
		}			
		// 2013.07.09 by KYK : Report even undefined Alarm
		if (isAllAlarmEnabled) {
			alarmItem = new AlarmItem();
			alarmItem.setAlarmId(alarmId);
			alarmItem.setAlarmText("Undefined Alarm");
			alarmItem.setEnabled(true);
			alarmItem.setActivated(true);
			alarmItem.setVehicleId(vehicleId);
			currentAlarmList.add(alarmItem);
			// Send S5F1 & S6F11
			sendS5F1(alarmId);
			sendS6F11Alarm("AlarmSet", trCmdId, vehicleId, vehicleState, (int) alarmId);
			// 2013.10.01 by MYM : UnitAlarmSet Event 추가
			sendS6F11UnitAlarm("UnitAlarmSet", trCmdId, vehicleId, vehicleState, (int) alarmId, vehicleCurrentDomain, vehicleCurrentPosition, sourcePort, destPort);
		}

		// Set OCS AlarmStatus : Error Undefined AlarmID			
		tscAlarmStatus = TSC_ALARMS;
		StringBuilder sb = new StringBuilder();
		sb.append("Alarm occurs but Undefined Alarm [Vehicle:").append(vehicleId);
		sb.append(", Alarm:").append(alarmId).append("]");
		writeSEMLog(sb.toString());
		// Exception 로그 추가
		traceException(sb.toString());
		return false;
	}
	
	/**
	 * 
	 * @param eventName
	 * @param trCmdId
	 * @param vehicleId
	 * @param vehicleState
	 * @param alarmId
	 */
	private void sendS6F11Alarm(String eventName, String trCmdId,
			String vehicleId, int vehicleState, int alarmId) {
		ReportItems reportItems = new ReportItems();
		reportItems.setCommandId(trCmdId);
		reportItems.setVehicleId(vehicleId);
		reportItems.setVehicleState(vehicleState);
		reportItems.setAlarmId(alarmId);
		// 2012.08.08 by KYK : alarmText VID 추가함
		String alarmText = null;
		if (alarmIdOTable != null) {
			AlarmItem alarm = alarmIdOTable.get((long)alarmId);
			if (alarm != null) {
				alarmText = alarm.getAlarmText();
			}
		}
		if (alarmText == null) {
			alarmText = "Undefined Alarm";
		}
		reportItems.setAlarmText(alarmText);
		
		sendS6F11(eventName, reportItems);
	}
	
	/**
	 * 2013.10.01 by MYM : UnitAlarmSet, UnitAlarmSetCleared Event 추가
	 * @param eventName
	 * @param trCmdId
	 * @param vehicleId
	 * @param vehicleState
	 * @param alarmId
	 * @param vehicleCurrentDomain
	 * @param vehicleCurrentPosition
	 * @param sourcePort
	 * @param destPort
	 */
	private void sendS6F11UnitAlarm(String eventName, String trCmdId,
			String vehicleId, int vehicleState, int alarmId,
			String vehicleCurrentDomain, int vehicleCurrentPosition,
			String sourcePort, String destPort) {
		ReportItems reportItems = new ReportItems();
		reportItems.setCommandId(trCmdId);
		reportItems.setVehicleId(vehicleId);
		reportItems.setVehicleState(vehicleState);
		reportItems.setAlarmId(alarmId);
		reportItems.setVehicleCurrentDomain(vehicleCurrentDomain);
		reportItems.setVehicleCurrentPosition(vehicleCurrentPosition);
		reportItems.setSourcePort(sourcePort);
		reportItems.setDestPort(destPort);
		
		String alarmText = null;
		if (alarmIdOTable != null) {
			AlarmItem alarm = alarmIdOTable.get((long)alarmId);
			if (alarm != null) {
				alarmText = alarm.getAlarmText();
			}
		}
		if (alarmText == null) {
			alarmText = "Undefined Alarm";
		}
		reportItems.setAlarmText(alarmText);
		
		sendS6F11(eventName, reportItems);
	}

	/**
	 * 2013.07.09 by KYK
	 * @param alarmId
	 * @return
	 */
	private int sendS5F1(long alarmId) {
		int alarmCode;
		String alarmText = "";
		boolean isOK = false;
		UComMsg message = uCom.makeSecsMsg(5, 1);
		message.setListItem(3);
		
		if (alarmIdOTable != null) {
			AlarmItem alarm = alarmIdOTable.get(alarmId);
			if (alarm != null) {
				alarmText = alarm.getAlarmText();
				if (alarm.isActivated()) {
					alarmCode = 0x80;					
				} else {
					alarmCode = 0x00;
				}
				message.setBinaryItem(alarmCode);
				message.setU4Item(alarmId);
				message.setAsciiItem(alarmText);
				isOK = true;
//				break;
			}
		}
		
		if (isOK == false) {
			if (isAllAlarmEnabled) {
				alarmText = "Undefined Alarm";
				message.setBinaryItem(0x80);
				message.setU4Item(alarmId);
				message.setAsciiItem(alarmText);
			} else {
				uCom.closeSecsMsg(message);
				return ER_UNDEFINED_ALARMID;
			}
		}
		String strLog = "[SND S5F1] Alarm Report Send: " + alarmText;
		writeSEMLog(strLog);
		updateSEMHistory(SENT, 5, 1, strLog);
		return sendUComMsg(message, "S5F1");
	}

	/**
	 * 2013.07.09 by KYK
	 * @param alarmId
	 * @param vehicleId
	 * @param vehicleState
	 * @param trCmdId
	 * @return
	 */
	public boolean clearAlarmReport(long alarmId, String vehicleId, int vehicleState, String trCmdId, String vehicleCurrentDomain,
			int vehicleCurrentPosition, String sourcePort, String destPort) {		
		long currAlarmId = 0;
		if (currentAlarmList != null) {
			ArrayList<AlarmItem> currAlarmList = new ArrayList<AlarmItem>(currentAlarmList);
			for (AlarmItem currAlarm: currAlarmList) {
				if (vehicleId != null && currAlarm != null) {
					if (vehicleId.equals(currAlarm.getVehicleId())) {
						currAlarmId = currAlarm.getAlarmId();
						// 2015.05.01 by KYK
						if ((alarmId == OcsConstant.COMMUNICATION_FAIL 
								|| currAlarmId == OcsConstant.COMMUNICATION_FAIL)) {
							if (alarmId != currAlarmId) {
								continue;
							}
						}
						
						// 2013.10.01 by MYM : UnitAlarmCleared Event 추가 
						sendS6F11UnitAlarm("UnitAlarmCleared", trCmdId, vehicleId, vehicleState, (int) currAlarmId, vehicleCurrentDomain, vehicleCurrentPosition, sourcePort, destPort);
						sendS6F11Alarm("AlarmCleared", trCmdId, vehicleId, vehicleState, (int) currAlarmId);
						sendS5F1(alarmId); // 0 (No_Error)
						currentAlarmList.remove(currAlarm);
						//
						if (alarmIdOTable != null) {
							AlarmItem alarm = alarmIdOTable.get(currAlarmId);
							if (alarm != null) {
								// 2013.10.02 by KYK
								if (alarm.getVehicleList() != null) {
									alarm.getVehicleList().remove(vehicleId);
									if (alarm.getVehicleList().isEmpty()) {
//									if (vehicleId.equals(alarm.getVehicleId())) {
										alarm.setActivated(false);
										alarm.setVehicleId("");
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		
		if (TSC_ALARMS.equals(tscAlarmStatus) && currentAlarmList.isEmpty()) {
			tscAlarmStatus = TSC_NO_ALARMS;
		}
		if (currAlarmId == 0) {
			StringBuffer sb = new StringBuffer();
			sb.append("Alarm cleared but can not find any activated alarm [Vehicle:");				
			sb.append(vehicleId).append("]");				
			writeSEMLog(sb.toString());			
		}
		return true;
	}
	
	/**
	 * Trace S6F11 History
	 */
	@Override
	public void traceS6F11History(String eventName, ReportItems reportItems) {
		StringBuffer sb = new StringBuffer();
		sb.append("[SND S6F11] ");
		sb.append(eventName).append("(");
		sb.append(makeString(reportItems.getCommandId())).append("/");
		sb.append(makeString(reportItems.getCarrierId())).append("/");
		sb.append(makeString(reportItems.getVehicleId())).append("/");
		sb.append(makeString(reportItems.getSourcePort())).append("->").append(makeString(reportItems.getDestPort())); 
		sb.append("/TranferPort=").append(makeString(reportItems.getTransferPort()));
		sb.append("/CurrLoc=").append(makeString(reportItems.getCarrierLoc()));
		sb.append("/ALARMID=").append(reportItems.getAlarmId());
		sb.append("/VehicleState=").append(reportItems.getVehicleState());
		sb.append("/Priority=").append(reportItems.getPriority());
		sb.append("/Replace=").append(reportItems.getReplace());
		sb.append("/ResultCode=").append(reportItems.getResultCode());
		// 2012.08.08 by KYK : alarmText log 추가
		if (reportItems.getAlarmText() != null && reportItems.getAlarmText().length() > 0) {
			sb.append("/AlarmText=").append(reportItems.getAlarmText());
		}
		// 2012.01.04 by KYK : VehicleType 추가
		if (reportItems.getVehicleType() != -1) {
			sb.append("/VehicleType=").append(reportItems.getVehicleType());
		}
		sb.append(")");
		
		writeSEMLog(sb.toString());		
		updateSEMHistory(SENT, 6, 11, sb.toString());
	}

	/**
	 * Respond S1F17 Status
	 */
	@Override
	public void respondS1F17Status() {
		controlStatus = REMOTE_ONLINE;
		sendS6F11(ControlStatusRemote, "", "", 0);
		tscStatus = TSC_INIT;
		sendS6F11(TSCAutoInitiated, "", "", 0);
		tscStatus = TSC_PAUSED;
		sendS6F11(TSCPaused, "", "", 0);
	}

	/**
	 * Update SEM History
	 */
	@Override
	public void updateSEMHistory(String type, int stream, int function,	String strLog) {
		// 2012.02.14 by PMM
		// FormattedLog로 대체, DB Transaction 부하 문제로 사용하지 않음.
//		String msgType = "S" + stream + "F" + function;
//		String eventTime = getCurrentTime();
//		
//		IBSEMHistory ibsemHistory = new IBSEMHistory(type, msgType, strLog, eventTime);
//		ibsemHistoryManager.addIBSEMHistoryToRegisterDB(ibsemHistory);
	}
}