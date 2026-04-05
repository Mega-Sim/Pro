package com.samsung.ocs.unitdevice;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.DockingStation;
import com.samsung.ocs.unitdevice.model.FireDoor;
import com.samsung.ocs.unitdevice.model.HID;
import com.samsung.ocs.unitdevice.model.OcsProcessVersionVO;
import com.samsung.ocs.unitdevice.model.PassDoor;
import com.samsung.ocs.unitdevice.model.FFU;

/**
 * UOCSMain.java는 Main 모듈이다.
 */
public class UnitDeviceMain {
	// Version ID
	static String m_strVersionID = "";

	// DBAccessManager m_DBAccessManager = null;
	public HIDManager m_HIDManager = null;

	// 2015.02.03 by zzang9un : PassDoor Manager 추가
	public PassDoorManager passDoorManager = null;

	ConcurrentHashMap<String, HIDServerOperation> hidServerOperationList = new ConcurrentHashMap<String, HIDServerOperation>();

	// 2015.02.03 by zzang9un : PassDoorOperation List 추가
	ConcurrentHashMap<String, PassDoorOperation> passDoorOperationMap = new ConcurrentHashMap<String, PassDoorOperation>();
	
	// 2016.03.25 by LSH : FFU Manager, FFUOperation List 추가
	public FFUManager ffuManager = null;
	ConcurrentHashMap<String, FFUOperation> ffuOperationMap = new ConcurrentHashMap<String, FFUOperation>();

	// 2014.01.21 by KYK
	public NetworkDeviceManager netDeviceManager;

	// +2013.03.26 by YBM : [DockingStation] 진동 모니터링 추가
	public DockingStationManager m_DockingStationManager = null;
	ConcurrentHashMap<String, DockingStationOperation> dockingStationOperationList = new ConcurrentHashMap<String, DockingStationOperation>();
	
	// 2018.11.12 by kw3711.kim : FireDoor 기능 추가 (FireDoorManager, FireDoorOpearation List)
	public FireDoorManager fireDoorManager = null;
	private ConcurrentHashMap<String, FireDoorOperation> fireDoorOperationMap = new ConcurrentHashMap<String, FireDoorOperation>();

	private MainThread m_MainThread = null;
	private int m_nClockInterval = 1000;

	// Error Code
	final int OK = 0;
	final int ERR_USERDEFINED = -1000;
	final int ERR_LOADCONFIG_FAIL = ERR_USERDEFINED - 1;
	final int ERR_DBCONNECTION_FAIL = ERR_USERDEFINED - 2;

	private static final String VERSION = "VERSION";
	private static final String BUILDID = "BUILDID";

	private static final String MAIN_TRACE = "HIDMainDebug";
	private static final String MAIN_EXCEPTION_TRACE = "HIDMainException";
	private static Logger mainTraceLog = Logger.getLogger(MAIN_TRACE);
	private static Logger mainExceptionTraceLog = Logger.getLogger(MAIN_EXCEPTION_TRACE);
	int hidCipher = 3;

	// 2012.06.14 by MYM : 로그 삭제
	// 2015.06.17 by zzang9un : 로그 폴더명 변경
	private String logPath = "";
	private String fileSeparator = File.separator;
	private SimpleDateFormat formatter = null;
	private static String EXCEPTION = "Exception";
	private long lastLogDeleteTime = System.currentTimeMillis();
	private long logDeleteCheckTime = 3600000;
	// 2019.09.02 by JJW Process name 정의
	private static final String unitdevice = "unitdevice";

	public static void main(String[] args) {
		String strArg = "";
		if (args.length > 0) {
			strArg = args[0].toUpperCase();
		}
		if (strArg.equals("-VERSION")) {
			System.out.println("Version : " + UnitDeviceMain.m_strVersionID);
		} else if (strArg.equals("") || strArg.equals("-CONSOLE")) {
			new UnitDeviceMain();
		}
	}

	/**
	 * UOCSMain의 생성자이다.
	 */
	public UnitDeviceMain() {
		m_strVersionID = com.samsung.ocs.VersionInfo.getString(VERSION) + " (" + com.samsung.ocs.VersionInfo.getString(BUILDID) + ")";

		int nRet = Initialize();
		if (nRet != OK) {
			String strErrorText = GetErrorText(nRet);
			String strLog = "Fail to run : " + strErrorText + "";
			mainTrace(strLog, null);
		}
		writeVersionHistory();
	}

	private void writeVersionHistory() {
		Logger startupLogger = Logger.getLogger("StartupHistory");
		startupLogger.info(com.samsung.ocs.VersionInfo.getString(VERSION));
		startupLogger.info(com.samsung.ocs.VersionInfo.getString(BUILDID));
	}

	/**
	 * 환경값 설정 및 DB를 포함한 각 모듈 생성
	 */
	public int Initialize() {
		// DB Access Frame 생성
		DBAccessManager dbAccessManager = new DBAccessManager();
		for (int i = 0; i < 5; i++) {
			if (dbAccessManager.IsDBConnected()) {
				break;
			}
			dbAccessManager.ReconnectToDB();
		}
		if (dbAccessManager.IsDBConnected() == false) {
			return ERR_DBCONNECTION_FAIL;
		}

		// 2014.01.21 by KYK : NetworkDevice 실행
		netDeviceManager = NetworkDeviceManager.getInstance();
		netDeviceManager.start();

		// 2012.06.02 by MYM : HID PowerControl 기능 추가
		m_HIDManager = HIDManager.getInstance();
		m_HIDManager.resetAllRemoteCmd();
		hidCipher = m_HIDManager.getHIDCipher();

		// 2015.02.03 by zzang9un : PassDoor Manager 실행
		passDoorManager = PassDoorManager.getInstance();
		
		// 2013.03.26 by YBM : Docking station
		m_DockingStationManager = DockingStationManager.getInstance();
		
		// 2016.03.25 by LSH : FFU Manager 실행
		ffuManager = FFUManager.getInstance();
		
		// 2018.11.12 by kkw3711.kim : FireDoor Manager 실행
		fireDoorManager = FireDoorManager.getInstance();

		m_MainThread = new MainThread();
		m_MainThread.start();
		
		// 2019.09.02 by JJW : Version 정보 호출 및 클래스 실행
		String version = com.samsung.ocs.VersionInfo.getString(VERSION);
		String buildId = com.samsung.ocs.VersionInfo.getString(BUILDID);
		String hostServiceType = dbAccessManager.m_HostType;

		OcsProcessVersionVO dbVersion = null;

		String oldVersion = null;
		String[] buildDateArray = null;
		String buildDate = null;
		buildDateArray = buildId.split("_");
		buildDate = buildDateArray[1];
		String moduleName = unitdevice;
		String newVersion = moduleName + "_" + version + "_" + buildDate;
			 	
		dbVersion = OcsProcessVersionDAO.retrieveProcessVersion(moduleName, dbAccessManager);
		if(hostServiceType.equalsIgnoreCase("Primary")){
			oldVersion = moduleName+"_"+dbVersion.getPrimary_Version()+"_"+dbVersion.getPrimary_Bulid_Date();
		}else{
			oldVersion = moduleName+"_"+dbVersion.getSecondary_Version()+"_"+dbVersion.getSecondary_Bulid_Date();
		}
		if(!oldVersion.equalsIgnoreCase(newVersion)){
			OcsProcessVersionDAO.registerVersion(dbAccessManager, hostServiceType, moduleName, version, buildDate);
			OcsProcessVersionDAO.historyVersion(dbAccessManager, moduleName, hostServiceType, version, buildDate);
		}

		// 2012.06.14 by MYM : 로그 삭제 설정
		initDeleteLog();

		return OK;
	}

	/**
	 * Main을 담당하는 Timer이다.
	 */
	void mainProcess() {
		try {
			// 1. DB의 HID정보를 Manager로 업데이트, HID Status를 DB로 업데이트
			m_HIDManager.update();

			// 2. HID 추가 및 삭제
			manageHIDServerOperationInstance();

			// +2013.03.26 by YBM : [Docking Station] 진동 모니터링
			// 4. DB의 docking station 정보를 Manager로 업데이트, DockingStation Status를
			// DB로 업데이트 (thread로 주기적으로 확인)
			m_DockingStationManager.update();
			// 5. DockingStationOperation 추가 및 삭제
			manageDockingStationOperationInstance();

			// 2015.02.03 by zzang9un : PassDoor 업데이트 추가
			passDoorManager.update();
			managePassDoorClientOperationInstance();
			
			// 2016.03.25 by LSH : FFU 업데이트 추가
			ffuManager.update();
			manageFFUClientOperationInstance();
			
			// 2018.11.12 by kw3711.kim : FireDoor 업데이트 추가
			fireDoorManager.update();
			manageFireDoorClientOperationInstance();

			// 6. 로그 삭제 (ExecutionTimeout msec 간격으로 실행)
			manageLogDelete();
		} catch (Exception e) {
			mainTrace("Exception mainProcess", e);
		}
	}

	/**
	 * Error 문자열을 얻는다.
	 */
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
	 * OCSMain의 이력을 저장한다.
	 */
	void mainTrace(String message, Throwable e) {
		if (e == null) {
			mainTraceLog.debug(message);
		} else {
			mainExceptionTraceLog.error(message, e);
		}
	}

	/**
	 * HIDServerOperation 객체를 생성하여 HIDServerOperationList에 추가한다.
	 */
	private HIDServerOperation createHIDServerOperation(String hidServerId, String ipAddress, int hidCipher) {
		HIDServerOperation hidOperation = null;
		hidOperation = new HIDServerOperation(m_HIDManager, hidCipher);
		hidOperation.SetOperationInfo(hidServerId, ipAddress);
		return hidOperation;
	}

	/**
	 * HIDServerOperation Unit을 시작한다.
	 */
	private void manageHIDServerOperationInstance() {
		int hidServerIndex = 0;
		// 2013.05.08 by KYK : try-catch 추가
		try {
			ConcurrentHashMap<String, HID> mapData = m_HIDManager.getData();
			Set<String> hidList = mapData.keySet();
			HIDServerOperation hidOperation = null;
			for (String hidName : hidList) {
				HID hid = mapData.get(hidName);
				if (hid != null) {
					hidOperation = hidServerOperationList.get(hid.getIpAddress());
					if (hidOperation == null) {
						hidOperation = createHIDServerOperation("HIDServer" + String.valueOf(++hidServerIndex), hid.getIpAddress(), hidCipher);
						hidOperation.addHID(hid);
						hidServerOperationList.put(hid.getIpAddress(), hidOperation);
						hidOperation.OperationStart();
					} else {
						hidOperation.addHID(hid);
					}
				}
			}

			for (Enumeration<HIDServerOperation> e = hidServerOperationList.elements(); e.hasMoreElements();) {
				hidOperation = e.nextElement();
				ConcurrentHashMap<String, HID> hidData = hidOperation.getHidList();
				for (Enumeration<HID> e2 = hidData.elements(); e2.hasMoreElements();) {
					HID hid = e2.nextElement();
					if (hid != null) {
						if (hidList.contains(hid.getHid()) == false) {
							hidOperation.removeHID(hid.getHid());
						}
					}
				}
				if (hidOperation.getHIDCount() == 0) {
					hidOperation.OperationStop();
					hidServerOperationList.remove(hidOperation.getIpAddress());
				}
			}
		} catch (Exception e) {
			mainTrace("Exception manageHIDServerOperationInstance", e);
		}

	}

	/**
	 * PassDoorManager의 data를 기준으로 PassDoorOperation을 시작하거나 종료한다.
	 * @author zzang9un
	 * @date 2015. 2. 11.
	 */
	private void managePassDoorClientOperationInstance() {
		try {
			ConcurrentHashMap<String, PassDoor> mapData = passDoorManager.getData();
			Set<String> passDoorList = mapData.keySet();
			PassDoorOperation passDoorOperation = null;
			for (String passDoorId : passDoorList) {
				PassDoor passDoor = mapData.get(passDoorId);
				if (passDoor != null) {
					passDoorOperation = passDoorOperationMap.get(passDoor.getIpAddress());
					if (passDoor.isEnabled()) {
						if (passDoorOperation == null) {
							passDoorOperation = createPassDoorOperation(passDoorId, passDoor.getIpAddress(), passDoor);
							passDoorOperationMap.put(passDoor.getIpAddress(), passDoorOperation);
							passDoorOperation.OperationStart();
						} else {
							passDoorOperation.setPassDoor(passDoor);
						}
					} else {
						if (passDoorOperation != null) {
							passDoorOperation.removePassDoor(passDoor.getPassDoorId());
							passDoorOperationMap.remove(passDoor.getIpAddress());
							passDoorOperation.OperationStop();
						}
					}	
				}
			}			
		} catch (Exception e) {
			mainTrace("Exception managePassDoorClientOperationInstance", e);
		}
	}

	/**
	 * PassDoorOperation 객체를 생성
	 * @author zzang9un
	 * @date 2015. 2. 3.
	 * @param string
	 * @param ipAddress
	 * @return
	 */
	private PassDoorOperation createPassDoorOperation(String passDoorId, String ipAddress, PassDoor passDoor) {
		PassDoorOperation passDoorServerOperation = null;
		passDoorServerOperation = new PassDoorOperation(passDoorManager);
		passDoorServerOperation.SetOperationInfo(passDoorId, ipAddress, passDoor);
		return passDoorServerOperation;
	}

	// + 2013.03.26 YBM. DockingStation
	private DockingStationOperation createDockingStationOperation(DockingStation dockingStation) {
		DockingStationOperation dockingStationOperation = null;
		dockingStationOperation = new DockingStationOperation(m_DockingStationManager);
		dockingStationOperation.setOperationData(dockingStation);
		return dockingStationOperation;
	}

	// + 2013.03.26 YBM. DockingStation
	private void manageDockingStationOperationInstance() {

		// 2013.05.08 by KYK : try-catch 추가
		try {
			// DockingStation usage가 켜져 있을 경우에만 operation instance 관리
			if (m_DockingStationManager.getDockingStationUsage()) {
				// DockingStationManager로부터 현재 db에 저장된 모든 docking station들의
				// data를 불러옴
				ConcurrentHashMap<String, DockingStation> dockingStationData = m_DockingStationManager.getData();
				// 현재 manager가 관장하고 있는 DockingStation의 ID를 모아 dockingStationID
				// List에 저장
				Set<String> dockingStationIDList = dockingStationData.keySet();

				DockingStationOperation dockingStationOperation = null;
				for (String dockingStationID : dockingStationIDList) {
					DockingStation dockingStation = dockingStationData.get(dockingStationID);
					// docking station operation list에 docking station ID에 해당하는
					// operation이 있는지 확인
					dockingStationOperation = dockingStationOperationList.get(dockingStationID);

					// Manager가 관장하는 docking station 중 operation이 없는 경우에는 해당
					// docking station에 대한 docking station operation을 생성
					if (dockingStationOperation == null) {
						dockingStationOperation = createDockingStationOperation(dockingStation);
						dockingStationOperationList.put(dockingStationID, dockingStationOperation);
						// docking station operation을 생성하고 나면, operation의 thread
						// 시작
						dockingStationOperation.operationStart();
					}

					// else {
					// // Manager가 관장하는 docking station의 operation이 있는 경우
					// dockingStationOperation.setOperationData(dockingStation);
					// }

				}

				// 현재 모듈에서 저장된 operationList에는 있으나, DB로 받은 docking station data
				// 가 삭제되어 사라진 경우 dockingStationOperationList 에서 제거하기 위한 코드
				for (String dockingStationId : dockingStationOperationList.keySet()) {
					if (!dockingStationIDList.contains(dockingStationId)) {
						dockingStationOperationList.get(dockingStationId).operationStop();
						dockingStationOperationList.remove(dockingStationId);
					}
				}
			} else {
				// DockingStation usage가 꺼져 있을 경우
				for (String dockingStationId : dockingStationOperationList.keySet()) {
					// 현재 docking station Operation list에 있는 모든 operation의
					// stop()
					dockingStationOperationList.get(dockingStationId).operationStop();
				}
				// dockingStationOperationList 삭제
				dockingStationOperationList.clear();
			}
		} catch (Exception e) {
			mainTrace("Exception manageDockingStationOperationInstance", e);
		}
	}
	
	private void manageFFUClientOperationInstance() {
		int ffuServerIndex = 0;
		
		try {
			if (ffuManager.isFFUMonitoringControlUsed()){
				ConcurrentHashMap<String, FFU> mapData = ffuManager.getData();
				Set<String> ffuList = mapData.keySet();
				FFUOperation ffuOperation = null;
				for (String ffuGroupId : ffuList) {
					FFU ffu = mapData.get(ffuGroupId);
					if (ffu != null) {
						ffuOperation = ffuOperationMap.get(ffu.getIpAddress());
						if (ffuOperation == null) {
							ffuOperation = createFFUOperation("FFUServer" + String.valueOf(++ffuServerIndex), ffu.getIpAddress());
							ffuOperation.addFFU(ffu);
							ffuOperationMap.put(ffu.getIpAddress(), ffuOperation);
							ffuOperation.OperationStart();
						} else {
							ffuOperation.addFFU(ffu);
						}
					}
				}

				for (Enumeration<FFUOperation> e = ffuOperationMap.elements(); e.hasMoreElements();) {
					ffuOperation = e.nextElement();
					ConcurrentHashMap<String, FFU> ffuData = ffuOperation.getFFUList();
					for (Enumeration<FFU> e2 = ffuData.elements(); e2.hasMoreElements();) {
						FFU ffu = e2.nextElement();
						if (ffu != null) {
							if (ffuList.contains(ffu.getFFUGroupId()) == false) {
								ffuOperation.removeFFU(ffu.getFFUGroupId());
							}
						}
					}
					if (ffuOperation.getFFUCount() == 0) {
						ffuOperation.OperationStop();
						ffuOperationMap.remove(ffuOperation.getIpAddress());
					}
				}
			} else {
				for (String ffuServerIpAddress : ffuOperationMap.keySet()) {
					ffuOperationMap.get(ffuServerIpAddress).OperationStop();
				}
				ffuOperationMap.clear();
			}
		} catch (Exception e) {
			mainTrace("Exception manageFFUClientOperationInstance", e);
		}
	}
	
	private FFUOperation createFFUOperation(String ffuServerId, String ipAddress) {
		FFUOperation ffuServerOperation = null;
		ffuServerOperation = new FFUOperation(ffuManager);
		ffuServerOperation.SetOperationInfo(ffuServerId, ipAddress);
		return ffuServerOperation;
	}

	// 2018.11.12 by kw3711.kim : FireDoor Operation Instance 관리
	private void manageFireDoorClientOperationInstance() {
		try {
			if (fireDoorManager.isFireDoorMonitoringControlUsed()){
				ConcurrentHashMap<String, FireDoor> mapData = fireDoorManager.getData();
				Set<String> fireDoorIdList = mapData.keySet();
				FireDoorOperation fireDoorOperation = null;
				for (String fireDoorId : fireDoorIdList) {
					FireDoor fireDoor = mapData.get(fireDoorId);
					if (fireDoor != null) {
						fireDoorOperation = fireDoorOperationMap.get(fireDoor.getFireDoorId());
						if (fireDoor.isEnabled()) {
							if (fireDoorOperation == null) {
								fireDoorOperation = createFireDoorOperation(fireDoor.getFireDoorId(), fireDoor.getIpAddress());
								fireDoorOperation.setFireDoor(fireDoor);
								fireDoorOperationMap.put(fireDoor.getFireDoorId(), fireDoorOperation);
								fireDoorOperation.OperationStart();
							} else {
								fireDoorOperation.setFireDoor(fireDoor);
							}
						} else {
							if (fireDoorOperation != null) {
//								fireDoorOperation.removeFireDoor(fireDoor.getFireDoorId());
								fireDoorOperationMap.remove(fireDoor.getFireDoorId());
								fireDoorOperation.OperationStop();
							}
						}
					}
				}
				
				// 현재 모듈에서 저장된 operationList에는 있으나, DB로 받은 firedoor data
				// 가 삭제되어 사라진 경우 fireFoorOperationList 에서 제거하기 위한 코드
				for (String fireDoorId : fireDoorOperationMap.keySet()) {
					if (!fireDoorIdList.contains(fireDoorId)) {
//						fireDoorOperation.removeFireDoor(fireDoorId);
						fireDoorOperationMap.get(fireDoorId).OperationStop();
						fireDoorOperationMap.remove(fireDoorId);
					}
				}
			} else {
				for (String fireDoorId : fireDoorOperationMap.keySet()) {
					fireDoorOperationMap.get(fireDoorId).OperationStop();
				}
				fireDoorOperationMap.clear();
			}
		} catch (Exception e) {
			mainTrace("Exception manageFireDoorClientOperationInstance", e);
		}
	}
	
	// 2018.11.12 by kw3711.kim : FireDoor Operation Instance 생성
	private FireDoorOperation createFireDoorOperation(String fireDoorId, String ipAddress) {
		FireDoorOperation fireDoorOperation = null;
		fireDoorOperation = new FireDoorOperation(fireDoorManager);
		fireDoorOperation.SetOperationInfo(fireDoorId, ipAddress);
		return fireDoorOperation;
	}
	
	class MainThread extends Thread {
		public boolean m_bRun = true;

		public void run() {
			try {
				while (m_bRun) {
					mainProcess();

					sleep(m_nClockInterval);
				}
			} catch (Exception e) {
				mainTrace("Exception MainThread", e);
			}
		}
	}

	/**
	 * 
	 */
	private void initDeleteLog() {
		String homePath = System.getProperty("user.dir");
		String fileSeparator = System.getProperty("file.separator");
		StringBuffer path = new StringBuffer();
		path.append(homePath).append(fileSeparator).append("log");
		logPath = path.toString();
		formatter = new SimpleDateFormat("yyyy-MM-dd");
	}

	/**
	 * 
	 */
	private void manageLogDelete() {
		// 2013.05.08 by KYK : try-catch 추가
		try {
			if (Math.abs(System.currentTimeMillis() - lastLogDeleteTime) > logDeleteCheckTime) {
				Calendar calendar = Calendar.getInstance();
				int logHoldingPeriod = m_HIDManager.getLogHoldingPeriod();
				calendar.add(Calendar.DATE, (-1) * (logHoldingPeriod));
				String timeBefore = formatter.format(calendar.getTime());
				deleteLog(logPath, logHoldingPeriod, timeBefore);
				lastLogDeleteTime = System.currentTimeMillis();
			}
		} catch (Exception e) {
			mainTrace("Exception manageLogDelete", e);
		}
	}
	
	/**
	 * Delete Old Logs Except Exception Logs
	 * 
	 * @param path
	 * @param logHoldingPeriod
	 * @param timeBefore
	 */
	private void deleteLog(String path, int logHoldingPeriod, String timeBefore) {
		File dir = new File(path);
		String[] files = dir.list();
		long lastModifiedTime;
		long storedPeriod;
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = new File(dir, files[i]);
				lastModifiedTime = file.lastModified();
				storedPeriod = (System.currentTimeMillis() - lastModifiedTime) / 24 / 3600 / 1000;
				try {
					if (file.isDirectory()) {
						StringBuffer logPathInfo = new StringBuffer();
						logPathInfo.append(path).append(fileSeparator).append(file.getName());
						deleteLog(logPathInfo.toString(), logHoldingPeriod, timeBefore);
					}
					if (file.isDirectory()) {
						if ((storedPeriod >= logHoldingPeriod || timeBefore.compareTo(file.getName()) > 0) && EXCEPTION.equals(file.getName()) == false) {
							deleteLog(file);
						}
					} else {
						if (storedPeriod >= logHoldingPeriod) {
							deleteLog(file);
						}
					}
				} catch (Exception e) {
					mainTrace("deleteLog()", e);
				}
			}
		}
	}

	private static final String INITFILE_PATTERN = "^(([a-zA-Z0-9]|_|-)*[a-zA-Z]|([a-zA-Z0-9]|_|-)*[a-zA-Z][0-9]).log$";
	private static final Pattern initFilePattern = Pattern.compile(INITFILE_PATTERN);

	private boolean isInitFile(String fileName) {
		Matcher matcher = initFilePattern.matcher(fileName);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}

	private void deleteLog(File file) {
		if (isInitFile(file.getName()) == false) {
			file.delete();
		}
	}
}
