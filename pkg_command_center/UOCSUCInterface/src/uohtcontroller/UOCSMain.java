package uohtcontroller;

import java.io.*;
import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.samsung.ocs.VersionInfo;

/*

01. UOCS_UCInterface_20100224
버전 : 2010.02.24로 검색할 것
변경 :

02. UOCS_UCInterface_20100412
버전 : 2010.04.12로 검색할 것
변경 :
      1) STB,UTB 적용 가능하도록 Type 조건 추가

03. UOCS_UCInterface_20100625
버전 : 2010.06.25로 검색할 것
변경 :
      1) DGR 업데이트시 버그 수정
         - LOCALGROUPID도 업데이트 하는 코드로 되어 있었음.

04. UOCS_UCInterface_20100713
버전 : 2010.07.13로 검색할 것
변경 :
      1) 고정형 베이 전용 OHT 기능 추가
         - [고정형 베이 전용 OHT] 로 검색

05. UOCS_UCInterface_20120724
버전 : 2010.07.20로 검색할 것
변경 :
      1) UDP 포트를 설정가능하도록 수정한 부분 패치, 현재는 4050 포트 고정
         - UDP 포트 설정

06. UOCS_UCInterface_20120817
버전 : 2012.08.17로 검색할 것
변경 :
      1) NODE.AREA -> NODE.BAY for OCS v3.1
      
07. UOCS_UCInterface_20131001
버전 : 2013.10.01로 검색할 것
변경 :
      1) 기존 RemoteServer에서 MCSmgr과 통신하는 Vehicle상태정보 부분을 옮겨옴       
 */



public class UOCSMain {
  // Version ID
//  public static String m_strVersionID = "OCSUCINTERFACE_20131001";
  public static String m_strVersionID = "";
	private static final String VERSION = "VERSION";
	private static final String BUILDID = "BUILDID";

  //2019.09.02 by JJW Process name 정의
  private static final String ucinterface = "ucinterface";

  private long m_lLastLogDeleteTime = 0;
  private long m_lStartTime = 0;
  private int m_nDeleteLogDay = 7;
  private String m_strLogPath = "";

  // 메시지 내용상 개별 항목을 구분하기 위한 구분자
  private String m_sDelimiter = " ";

  private int m_nNumOfTotalVHL = 0;
  private int m_nNumOfIdleVHL = 0;
  private int m_nUpdateIntervalCount = 0;
  private long m_lLastUpdateTimeOfExpiredTime = 0;
  private int m_nDGRMaxVHLLimit = 5; // 2010.02.22 by MYM : DGR(Define Group Request) MAXVHL Limit
  private int m_nUDPPort = 4050; //2012.07.20 by MYM : UDP 포트를 설정 가능하도록 수정
  private int m_nTCPPort = 4100; //2013.10.01 by IKY : TCP 포트를 설정 가능하도록 수정
  private long m_lLogDeleteTimeout = 3600000; // Log를 삭제하는 주기
  private int MAX_CLIENT = 100; // 2022.03.30 by JJW UCInterface Connection 연결 제한 기능 추가

  // Error Code
  final int OK = 0;
  final int ERR_USERDEFINED = -1000;
  final int ERR_LOADCONFIG_FAIL = ERR_USERDEFINED - 1;
  final int ERR_DBCONNECTION_FAIL = ERR_USERDEFINED - 2;

  final int GROUPINFO_NOT_EXIST = 101;
  final int EQINFO_NOT_EXIST = 102;
  final int EQINFO_OVERLAP = 103;
  final int MINVHL_OR_MAXVHL_NEGATIVE_VALUE = 104;
  final int MINVHL_MORE_THAN_MAXVHL = 105;
  final int MAXVHL_MORE_THAN_MAXVHLLIMIT = 106;

  utilLog m_MainLog = null; // Utility에 대한 객체
  DBAccessManager m_DBAccessManager = null; // DBAccessFrame에 대한 객체
  UDPComm m_UDPComm = null; // UDPComm에 대한 객체
  MainThread m_MainThread = null;

  // 2013.10.01 by IKY
  RemoteService m_pRemoteServer = null;
  
  class MainThread extends Thread
  {
    public void run()
    {
      try
      {
        while (true)
        {
          MainProcess();

          sleep(1000);
        }
      }
      catch (Exception e)
      {
        String strLog = "MainThread - Exception: \n" + e.getMessage();
        MainTrace(strLog, null);
      }
    }
  }

  public UOCSMain() {
  	m_strVersionID = com.samsung.ocs.VersionInfo.getString(VERSION) + " ("+ com.samsung.ocs.VersionInfo.getString(BUILDID) + ")";
  	
    int nReturn = Initialize();

    String strLog = "";
    strLog = "UCInterface Execution Start -----------------------------";
    MainTrace("", null);
    MainTrace(strLog, null);

    if (nReturn != OK) {
      String strErrorText = GetErrorText(nReturn);
      strLog = "UCInterface Initialized Fail by " + strErrorText;
      MainTrace(strLog, null);
      return;
    }
    strLog = "UCInterface Execution Start OK ---------------------------";
    MainTrace(strLog, null);

    // 2010.02.22 by MYM, IKY : 시작시 LocalGroup 정보를 초기화 함.
    // 2013.07.05 by IKY : OCS 3.0에서는 고정형 LocalOHT 기능이 없어지면서 VHLOPTION Column이 DB에서 Drop됨
    // OCS가 자체적으로 지정하거나 MCSmgr이 지정한 LocalGroup에 대한 모든 설정을 재시작이 되더라도 그대로 유지함
//    DeleteGroupInfoInDB(null);
//    ClearGroupIDToCarrierLoc(null);
//    ClearGroupIDInVehicleDB(null);

    m_UDPComm.ServerThreadStart();
    strLog = "UDPComm ServerThread Start OK ---------------------------";
    MainTrace(strLog, null);
    
    m_pRemoteServer.TCPServiceStart();
    strLog = "TCPComm ServerThread Start OK ---------------------------";
    MainTrace(strLog, null);

    // 2010.02.24 by MYM : 버전 정보 로그화 추가
    if(m_MainLog != null) {
      m_MainLog.WriteVersionHistory("UOHTController", m_strVersionID);
    }

    m_lLastUpdateTimeOfExpiredTime = System.currentTimeMillis();

    m_MainThread = new MainThread();
    m_MainThread.start();
  }

  public int Initialize() {
    // *.ini로부터의 Configuration 정보 얻기
    if (LoadConfig() == false) {
      return ERR_LOADCONFIG_FAIL;
    }

    // Log 인스턴스 생성
    m_MainLog = new utilLog(m_strLogPath, m_nDeleteLogDay);
    // DB Access 인스턴스 생성
    m_DBAccessManager = new DBAccessManager();    
    // DB 연결이 실패했을 경우 Initialize 실패 처리
    if (m_DBAccessManager.IsDBConnected() == false) {
      return ERR_DBCONNECTION_FAIL;
    }
    // UDPComm 인스턴스 생성
    // 2012.07.20 by MYM : UDP 포트를 설정 가능하도록 수정
    m_UDPComm = new UDPComm(this, m_MainLog, m_nUDPPort);
    
    // 2013.10.01 by IKY
    // RemoteService 인스턴스 생성 및 Thread 실행
//	m_pRemoteServer = new RemoteService(m_DBAccessManager, m_MainLog, m_nTCPPort);
    // 2022.03.30 by JJW UCInterface Connection 연결 제한 기능 추가로 메소드의 인자 추가
    m_pRemoteServer = new RemoteService(m_DBAccessManager, m_MainLog, m_nTCPPort, MAX_CLIENT);

    // 지정된 Log 기간 이전의 필요없는 Log 제거
    m_MainLog.DeleteAutoLogFiles(m_nDeleteLogDay);

    m_lStartTime = System.currentTimeMillis();
    m_lLastLogDeleteTime = System.currentTimeMillis();
    
    // 2019.09.02 by JJW : Version 정보 호출 및 클래스 실행
    OcsProcessVersionVO dbVersion = null;
    
	String version = com.samsung.ocs.VersionInfo.getString(VERSION);
 	String buildId = com.samsung.ocs.VersionInfo.getString(BUILDID);
	
	String oldVersion = null;
	String [] buildDateArray = null;
	String buildDate = null;
	buildDateArray = buildId.split("_");
	buildDate = buildDateArray[1];
	String moduleName = ucinterface;
	String newVersion = moduleName+ "_" +version +"_"+ buildDate;
 
 	String hostServiceType = m_DBAccessManager.m_HostType;
 	
 	dbVersion = OcsProcessVersionDAO.retrieveProcessVersion(moduleName, m_DBAccessManager);
	if(hostServiceType.equalsIgnoreCase("Primary")){
		oldVersion = moduleName+"_"+dbVersion.getPrimary_Version()+"_"+dbVersion.getPrimary_Bulid_Date();
	}else{
		oldVersion = moduleName+"_"+dbVersion.getSecondary_Version()+"_"+dbVersion.getSecondary_Bulid_Date();
	}
	if(!oldVersion.equalsIgnoreCase(newVersion)){
		OcsProcessVersionDAO.registerVersion(m_DBAccessManager, hostServiceType, moduleName, version, buildDate);
		OcsProcessVersionDAO.historyVersion(m_DBAccessManager, moduleName, hostServiceType, version, buildDate);
	}

    return OK;
  }

  boolean LoadConfig() {
    StringBuffer FilePathName = new StringBuffer();
    String strFileName = "UOHTController.ini";

    // get current directory
    String strPath = System.getProperty("user.dir");
    String Separator = System.getProperty("file.separator");
    FilePathName.append(strPath).append(Separator).append(strFileName);

    File file;
    RandomAccessFile raf;

    int nPos;
    String strLine = "";
    String strData = "";

    try {
      file = new File(FilePathName.toString());
      raf = new RandomAccessFile(file, "r");

      while ( (strLine = raf.readLine()) != null) {
        if (strLine.indexOf("Path") == 0) {
          nPos = strLine.indexOf("=");
          strData = strLine.substring(nPos + 1);
          m_strLogPath = strData.trim();
        }
        else if (strLine.indexOf("DeleteLogDay") == 0) {
          nPos = strLine.indexOf("=");
          strData = strLine.substring(nPos + 1);
          m_nDeleteLogDay = Integer.parseInt(strData);
        }
        // 2010.02.22 by MYM : DGR(Define Group Request) MAXVHL Limit값을 Init 파일에서 불러옴.
        else if (strLine.indexOf("DGR_MAXVHL_LIMIT") == 0) {
          nPos = strLine.indexOf("=");
          strData = strLine.substring(nPos + 1);
          m_nDGRMaxVHLLimit = Integer.parseInt(strData);
        }
        // 2012.07.20 by MYM : UDP 포트를 설정 가능하도록 수정
        else if (strLine.indexOf("UCInterfacePort") == 0) {
          nPos = strLine.indexOf("=");
          strData = strLine.substring(nPos + 1);
          m_nUDPPort = Integer.parseInt(strData);
        }
        // 2013.10.01 by IKY : TCP 포트를 설정 가능하도록 수정
        else if (strLine.indexOf("UCInterfaceTCPPort") == 0) {
          nPos = strLine.indexOf("=");
          strData = strLine.substring(nPos + 1);
          m_nTCPPort = Integer.parseInt(strData);
        }
        // 2022.03.30 by JJW UCInterface Connection 연결 제한 기능 추가
        else if (strLine.indexOf("MAX_CLIENT") == 0) {
          nPos = strLine.indexOf("=");
          strData = strLine.substring(nPos + 1);
          MAX_CLIENT = Integer.parseInt(strData);
        }
      }
      raf.close();
    }
    catch (IOException e) {
      String strLog = "LoadConfig() - IOException: " + e.getMessage();
      MainTrace("LoadConfig()", strLog);
      return false;
    }

    return true;
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

  void MainTrace(String strLog1, String strLog2) {
    if (strLog2 == null) {
      strLog2 = "";
    }
    m_MainLog.WriteReturnLog("UCInterfaceMainLog", strLog1, strLog2, true);
    System.out.println(strLog1 + strLog2);
  }

  void MessageTrace(String strLog1, String strLog2) {
    if (strLog2 == null) {
      strLog2 = "";
    }
    m_MainLog.WriteReturnLog("ReportMessageLog", strLog1, strLog2, true);
    System.out.println(strLog1 + strLog2);
  }

  public void MainProcess()
  {
    UpdateVehicleCount();
    UpdateGroupVHLInfoInDB();
    // 2010.02.23 by MYM : ExpiredTime을 JobAssign에서 관리하도록 함.
//    if (m_nUpdateIntervalCount++ > 5)
//    {
//      UpdateGroupExpiredTimeInfoInDB();
//      m_nUpdateIntervalCount = 0;
//    }

    // 2010.02.24 by MYM : 보존 기간이 지난 로그 삭제 기능 추가
    if (System.currentTimeMillis() - m_lLastLogDeleteTime > m_lLogDeleteTimeout)
    {
      m_MainLog.DeleteAutoLogFiles(m_nDeleteLogDay);
      m_lLastLogDeleteTime = System.currentTimeMillis();
    }
  }

  /**
   * GVR 메시지에 대한 응답에 대해서 TOTALVHL과 IDLEVHL 값을 업데이트 하는 함수이다.
   */
  public void UpdateVehicleCount()
  {
    String strLog;
    String strSql, strSql_1, strSql_2;
    strSql_1 = "(SELECT Count(*) AS COUNT FROM VEHICLE WHERE ENABLED='TRUE') E,";
    strSql_2 = "(SELECT Count(*) AS COUNT FROM VEHICLE A, TRCMD B WHERE A.VEHICLEID=B.VEHICLE) F";
    strSql = "SELECT E.COUNT AS TOTALVHL, (E.COUNT - F.COUNT) AS IDLEVHL FROM " + strSql_1 + strSql_2;

    ResultSet rs = null;
    try
    {
      rs = m_DBAccessManager.GetRecord(strSql);
      if ((rs != null) && (rs.next()))
      {
        m_nNumOfTotalVHL = rs.getInt("TOTALVHL");
        m_nNumOfIdleVHL = rs.getInt("IDLEVHL");
      }
    }
    catch (SQLException e)
    {
      strLog = "SQLException: " + e.getMessage();
      MainTrace("  UpdateVehicleCount()-Exception : ", strLog);
    }
    finally
    {
      if (rs != null)
      {
        m_DBAccessManager.CloseRecord(rs);
      }
    }
  }

  /**
   * Vehicle 테이블을 모든 Vehicle을 Group별로 CurrVHL, BAYVHL을 계산하여 DB에 반영하는 함수이다.
   * @return boolean
   */
  public void UpdateGroupVHLInfoInDB()
  {
	  String strGroupID = "";
	  int nBayVHL = 0, nCurrVHL = 0;
	  String strSql, strSql_1, strSql_2;
	  strSql_1 = "(SELECT A.LOCALGROUPID, Count(*) COUNT FROM ";
	  strSql_1 += "(SELECT VEHICLEID, CURRNODE, LOCALGROUPID, BAY FROM VEHICLE, NODE WHERE CURRNODE=NODEID AND LOCALGROUPID IS NOT NULL) A, LOCALGROUPINFO C ";
	  strSql_1 += "WHERE A.LOCALGROUPID IS NOT NULL AND C.ENABLED='TRUE' AND C.LOCALGROUPID=A.LOCALGROUPID AND A.BAY=C.BAY GROUP BY A.LOCALGROUPID) E RIGHT OUTER JOIN";
	  strSql_2 = "(SELECT LOCALGROUPID, Count(*) COUNT FROM VEHICLE WHERE LOCALGROUPID IS NOT NULL GROUP BY LOCALGROUPID) F ";

	  strSql = "SELECT F.LOCALGROUPID, E.COUNT AS BAYVHL, F.COUNT AS CURVHL FROM " + strSql_1 + strSql_2;
	  strSql += " ON F.LOCALGROUPID=E.LOCALGROUPID ORDER BY F.LOCALGROUPID";

	  ResultSet rs = null;
	  try {
		  rs = m_DBAccessManager.GetRecord(strSql);

		  // 모든 Group의 CurVHL과 BayVHL를 초기화 한 후 아래의 쿼리 결과를 업데이트 하도록 한다.
		  Vector<String> vtSqlList = new Vector<String>();
		  strSql = "UPDATE LOCALGROUPINFO SET CURVHL=0, BAYVHL=0";
		  vtSqlList.add(strSql);
		  while ((rs != null) && (rs.next())) {
			  strGroupID = rs.getString("LOCALGROUPID");
			  nBayVHL = rs.getInt("BAYVHL");
			  nCurrVHL = rs.getInt("CURVHL");

			  strSql = "UPDATE LOCALGROUPINFO SET CURVHL=" + nCurrVHL + ", BAYVHL=" + nBayVHL + " WHERE LOCALGROUPID='" + strGroupID + "'";
			  vtSqlList.add(strSql);
		  }
		  ExecSQLList(vtSqlList);
	  } catch (SQLException e) {
		  String strLog = "SQLException: " + e.getMessage();
		  MainTrace("  UpdateGroupVHLInfoInDB()-Exception : ", strLog);
	  } finally {
		  if (rs != null) {
			  m_DBAccessManager.CloseRecord(rs);
		  }
	  }
  }

  String InterpretReceivedMsg(String strReceivedMsg)
  {
    String strMsgName;
    String strRepMsgName = "";
    String strRepMsgItem = "";
    String strReturnMsg = "";

    strMsgName = GetMsgName(strReceivedMsg);
    strRepMsgName = strMsgName+"_REP";
    // USE_LOCAL_OHT_REQUEST (ULR)
    if (strMsgName.equals("ULR"))
    {
      strRepMsgItem = UpdateLocalOHTUsage(strReceivedMsg);
    }
    // DEFINE_GROUP_REQUEST (DGR)
    else if (strMsgName.equals("DGR"))
    {
      strRepMsgItem = UpdateGroupInfo(strReceivedMsg);
    }
    // SET_VEHICLE_REQUEST (SVR)
    else if (strMsgName.equals("SVR"))
    {
      strRepMsgItem = UpdateLocalOHTInfo(strReceivedMsg);
    }
    // GET_VEHICLE_REQUEST (SVR)
    else if (strMsgName.equals("GVR"))
    {
      strRepMsgItem = QueryLocalOHTInfo(strReceivedMsg);
    }
    else
    {
      strRepMsgName = "UNDEFINED_MSG_NAME";
    }

    strReturnMsg = strRepMsgName + strRepMsgItem;
    return strReturnMsg;
  }

  public String GetMsgName(String strMsgString)
  {
    if (strMsgString.length() <= 0)
      return "";

    String strMsgName = "";
    int nPos = strMsgString.indexOf(m_sDelimiter);
    if (nPos > 0)
    {
      strMsgName = strMsgString.substring(0, nPos);
      return strMsgName;
    }
    else
    {
      return strMsgName;
    }
  }

  public String GetMsgItem(String strMsgString, String strKey)
  {
    if (strMsgString.length() <= 0)
      return "";

    int nPos = 0;
    String strMsgItem = "";
    nPos = strMsgString.indexOf(strKey);
    if (nPos > 0)
    {
      String strTempMsgString = strMsgString.substring(nPos);
      nPos = strTempMsgString.indexOf(m_sDelimiter);
      if (nPos > 0) {
        strMsgItem = strTempMsgString.substring(strKey.length(), nPos);
      }
      else {
        strMsgItem = strTempMsgString.substring(strKey.length());  // Key값이 마지막 항목일 경우
      }
    }
    else
    {
      // Key를 갖는 MsgItem이 존재하지 않음...
    }
    return strMsgItem;
  }

  public String UpdateLocalOHTUsage(String strReceivedMsg)
  {
    String strRepMsgItem = "";
    String strHDR, strIPAddress, strPort, strUsage;
    String strResult = "RESULT=";

    strHDR = GetMsgItem(strReceivedMsg, "HDR=");
    strIPAddress = GetMsgItem(strReceivedMsg, "IP=");
    strPort = GetMsgItem(strReceivedMsg, "PORT=");
    strUsage = GetMsgItem(strReceivedMsg, "USE=");

    // Local OHT 기능 On/Off 설정
    String strSql = "UPDATE OCSINFO SET ";
    if (strUsage.equals("Y") == true)
      strSql += "VALUE='YES'";
    else
      strSql += "VALUE='NO'";
    strSql += " WHERE NAME='LOCALOHT_USAGE'";

    try
    {
      m_DBAccessManager.ExecSQL(strSql);
    }
    catch (SQLException e)
    {
      String strLog = "UpdateLocalOHTUsage() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
    }

    strHDR = "HDR=(iFACS,OCS)";
    strResult += "PASS";
    strRepMsgItem = m_sDelimiter+strHDR+m_sDelimiter+strResult;

    return strRepMsgItem;
  }

  public String UpdateGroupInfo(String strReceivedMsg)
  {
    String strRepMsgItem = "";
    String strHDR, strGroupID, strEQPID, strMinVHL, strMaxVHL, strType;
    String strResult = "RESULT=";
    String strReason = "REASON=";
    String strReturnValue;

    strHDR = GetMsgItem(strReceivedMsg, "HDR=");
    strGroupID = GetMsgItem(strReceivedMsg, "GROUPID=");
    strEQPID = GetMsgItem(strReceivedMsg, "EQPID=");
    strMinVHL = GetMsgItem(strReceivedMsg, "MINVHL=");
    strMaxVHL = GetMsgItem(strReceivedMsg, "MAXVHL=");
    strType = GetMsgItem(strReceivedMsg, "TYPE=");

    strReturnValue = CheckDGRMsgValidation(strGroupID, strEQPID, strMinVHL, strMaxVHL, strType);
    // Type별 Group 추가/갱신/삭제 처리
    if (strReturnValue.equals("ADD"))
    {
      CreateGroupInfoInDB(strGroupID, strEQPID, strMinVHL, strMaxVHL);
      strResult += "PASS";
    }
    else if (strReturnValue.equals("UPDATE"))
    {
      ModifyGroupInfoInDB(strGroupID, strEQPID, strMinVHL, strMaxVHL);
      strResult += "PASS";
    }
    else if (strReturnValue.equals("DEL"))
    {
      DeleteGroupInfoInDB(strGroupID);
      strResult += "PASS";
    }
    else
    {
      strReason += strReturnValue;
      strResult += "FAIL";
    }

    strHDR = "HDR=(iFACS,OCS)";
    strGroupID = "GROUPID="+strGroupID;
    strRepMsgItem = m_sDelimiter+strHDR+m_sDelimiter+strGroupID+m_sDelimiter+strResult+m_sDelimiter+strReason;
    return strRepMsgItem;
  }

  public String UpdateLocalOHTInfo(String strReceivedMsg)
  {
    String strRepMsgItem = "";
    String strHDR, strGroupID, strMinVHL, strSetVHL, strDistance, strExpiredTime;
    String strResult = "RESULT=";
    String strReason = "REASON=";
    String strReturnValue;

    strHDR = GetMsgItem(strReceivedMsg, "HDR=");
    strGroupID = GetMsgItem(strReceivedMsg, "GROUPID=");
    strMinVHL = GetMsgItem(strReceivedMsg, "MINVHL=");
    strSetVHL = GetMsgItem(strReceivedMsg, "SETVHL=");
    strDistance = GetMsgItem(strReceivedMsg, "DISTANCE=");
    strExpiredTime = GetMsgItem(strReceivedMsg, "EXPIREDTIME=");

    // Local OHT 사용 수량 등록 및 변경
    strReturnValue = CheckSVRMsgValidation(strGroupID, strSetVHL, strDistance, strExpiredTime);
    if (strReturnValue.equals("OK"))
    {
      ModifySetVHLValueInDB(strGroupID, strSetVHL, strDistance, strExpiredTime);
      strResult += "PASS";
    }
    else
    {
      strResult += "FAIL";
      strReason += strReturnValue;
    }

    strHDR = "HDR=(iFACS,OCS)";
    strGroupID = "GROUPID="+strGroupID;
    strRepMsgItem = m_sDelimiter+strHDR+m_sDelimiter+strGroupID+m_sDelimiter+strResult+m_sDelimiter+strReason;
    return strRepMsgItem;
  }

  public String QueryLocalOHTInfo(String strReceivedMsg)
  {
    String strRepMsgItem = "";
    String strHDR, strGroupID;
    String strResult = "RESULT=";
    String strReason = "REASON=";
    String strGVRRepMsgItem = "";
    int nMinVHL, nCurVHL, nBayVHL, nSetVHL;
    int nDistance, nExpiredTime;

    strHDR = GetMsgItem(strReceivedMsg, "HDR=");
    strGroupID = GetMsgItem(strReceivedMsg, "GROUPID=");

    if(GetLocalOHTUsage() == false)
      return "LOCALOHT_USAGE_OFF";

    String strReturnValue = CheckGroupID(strGroupID);
    if (strReturnValue.equals("OK") == false)
      return strReturnValue;

    // Local OHT 정보 조회
    String strSql = "SELECT * FROM LOCALGROUPINFO WHERE ";
    strSql += "LOCALGROUPID='" + strGroupID + "'";
    ResultSet rs = null;
    try
    {
      rs = m_DBAccessManager.GetRecord(strSql);
      while (rs != null && rs.next())
      {
        nMinVHL = rs.getInt("MINVHL");
        strGVRRepMsgItem = ("MINVHL=" + String.valueOf(nMinVHL) + m_sDelimiter);
        nCurVHL = rs.getInt("CURVHL");
        strGVRRepMsgItem += ("CURVHL=" + String.valueOf(nCurVHL) + m_sDelimiter);
        nBayVHL = rs.getInt("BAYVHL");
        strGVRRepMsgItem += "BAYVHL=" + String.valueOf(nBayVHL) + m_sDelimiter;
        nSetVHL = rs.getInt("SETVHL");
        strGVRRepMsgItem += "SETVHL=" + String.valueOf(nSetVHL) + m_sDelimiter;
        strGVRRepMsgItem += "TOTALVHL=" + String.valueOf(m_nNumOfTotalVHL) + m_sDelimiter;
        strGVRRepMsgItem += "IDLEVHL=" + String.valueOf(m_nNumOfIdleVHL) + m_sDelimiter;
        nDistance = rs.getInt("DISTANCE");
        strGVRRepMsgItem += "DISTANCE=" + String.valueOf(nDistance) + m_sDelimiter;
        nExpiredTime = rs.getInt("EXPIREDTIME");
        strGVRRepMsgItem += "EXPIREDTIME=" + String.valueOf(nExpiredTime);
      }
      strResult += "PASS";
    }
    catch (SQLException e)
    {
      strResult += "FAIL";
      String strLog = "QueryLocalOHTInfo() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
    }
    finally
    {
      if (rs != null)
        m_DBAccessManager.CloseRecord(rs);
    }

    strHDR = "HDR=(iFACS,OCS)";
    strGroupID = "GROUPID="+strGroupID;
    strRepMsgItem = m_sDelimiter+strHDR+m_sDelimiter+strGroupID+m_sDelimiter+
        strGVRRepMsgItem+m_sDelimiter+strResult+m_sDelimiter+strReason;
    return strRepMsgItem;
  }

  /**
   * 'DEFINE_GROUP_REQUEST' 메시지를 수신했을때 Validation을 체크하는 함수이다.
   * @param strGroupID String
   * @param strEQPID String
   * @param strMinVHL String
   * @param strMaxVHL String
   * @param strType String
   * @return String
   */
  private String CheckDGRMsgValidation(String strGroupID, String strEQPID,
                                       String strMinVHL, String strMaxVHL, String strType)
  {
    if(GetLocalOHTUsage() == false)
      return "LOCALOHT_USAGE_OFF";
    if(strGroupID.equals(""))
      return "GROUPID_EMPTY_VALUE";
    if(strType.equals("DEL") == false && strEQPID.equals(""))
      return "EQPID_EMPTY_VALUE " + strType;
    if(strType.equals("DEL") == false && strMinVHL.equals(""))
      return "MINVHL_EMPTY_VALUE " + strType;
    if(strType.equals("DEL") == false && strMaxVHL.equals(""))
      return "MAXVHL_EMPTY_VALUE " + strType;

    // MINVHL, MAXVHL 파라미터 Validation 체크
    int nMinVHL = 0;
    int nMaxVHL = 0;
    if (strMinVHL.equals("") == false)
      nMinVHL = Integer.parseInt(strMinVHL);
    if (strMaxVHL.equals("") == false)
      nMaxVHL = Integer.parseInt(strMaxVHL);
    if (nMinVHL < 0)
      return "MINVHL_NEGATIVE_VALUE " + strMinVHL;
    if (nMaxVHL < 0)
      return "MAXVHL_NEGATIVE_VALUE " + strMaxVHL;
    if (nMinVHL > nMaxVHL)
      return "MINVHL_MORE_THAN_MAXVHL " + strMinVHL + "," + strMaxVHL;
    // 2010.02.22 by MYM : MaxVHL 값이 Limit값 보다 큰 경우 NAK 처리함.
    if(nMaxVHL > m_nDGRMaxVHLLimit)
      return "MAXVHL_MORE_THAN_MAXVHLLIMIT " + strMaxVHL + "," + m_nDGRMaxVHLLimit;
    // TYPE Validation 체크
    if (!strType.equals("ADD") && !strType.equals("DEL"))
      return "UNDEFINED_TYPE "+strType;

    String strReturnValue = "OK";
    boolean bGroupInfoExist = false;
    String strSql = "SELECT * FROM LOCALGROUPINFO WHERE LOCALGROUPID='" + strGroupID + "'";
    ResultSet rs = null;
    try
    {
      rs = m_DBAccessManager.GetRecord(strSql);
      if (rs != null && rs.next())
        bGroupInfoExist = true;

      // EQINFO의 Validation 체크
      if (strEQPID.equals("") == false)
        strReturnValue = CheckEQInfoInDB(strGroupID, strEQPID);

      if (strReturnValue.equals("OK") == false)
        return strReturnValue;

      // TYPE별로 RETURN 값을 설정한다.
      if (strType.equals("ADD") && bGroupInfoExist == false)
        return "ADD";
      else if (strType.equals("ADD") && bGroupInfoExist == true)
        return "UPDATE";
      else if (strType.equals("DEL") && bGroupInfoExist == false)
        return "GROUPINFO_NOT_EXIST "+strGroupID;
      else if (strType.equals("DEL") && bGroupInfoExist == true)
        return "DEL";
      else
        return "UNKNOWN_ERR";
    }
    catch (SQLException e)
    {
      String strLog = "CheckDGRMsgValidation() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return "EXCEPTION_ERR";
    }
    finally
    {
      if (rs != null)
        m_DBAccessManager.CloseRecord(rs);
    }
  }

  /**
   * DB Carrierloc 테이블에서 주어진 EQPID상의 EQ의 Validation을 체크하는 함수이다.
   * @param strGroupID String
   * @return boolean
   */
  private String CheckEQInfoInDB(String strGroupID, String strEQPID)
  {
    String strEQName;
    Vector vtEQItemList = new Vector();
    Vector vtOwnerList = new Vector();
    String strSql = "SELECT * FROM CARRIERLOC WHERE (";

    GetEQItemFromEQPMsg(strEQPID, vtEQItemList);
    for ( int i = 0 ; i < vtEQItemList.size() ; i++ )
    {
      strEQName = (String)vtEQItemList.get(i);
      if (i == vtEQItemList.size()-1)
        strSql += "CARRIERLOCID LIKE '" + strEQName + "%')";
      else
        strSql += "CARRIERLOCID LIKE '" + strEQName + "%' OR ";
    }

    if (vtEQItemList.size() == 0)
      return "EQPID_NOT_EXIST";

    ResultSet rs = null;
    try
    {
      rs = m_DBAccessManager.GetRecord(strSql);
      while (rs != null && rs.next())
      {
        String carrierLocId = rs.getString("CARRIERLOCID");
        // 2014.02.17 by MYM : Type 구분없이 CarrierLocID로 검색하도록 변경
        // 배경 : M1B에서 동일 Stocker이지만 서로 다른 베이에 위치하고 있어서 LocalGroup을 분리 운영해야 함.
        // 2010.04.12 by MYM : STB,UTB 적용 가능하도록 Type 조건 추가
//        String strType = rs.getString("TYPE");
//        String strOwner = "";
//        if(strType.equals("UTBPORT") || strType.equals("STBPORT"))
//        {
//          strOwner = strCarrierLocID;
//        }
//        else
//        {
//          int nPos = strCarrierLocID.indexOf("_");
//          strOwner = strCarrierLocID.substring(0, nPos);
//        }

        // 2010.04.12 by MYM : 중복 추가를 제거 조건 추가
				if (carrierLocId != null
						&& vtOwnerList.contains(carrierLocId) == false) {
					vtOwnerList.add(carrierLocId);
				}

        String strDBGroupID = rs.getString("LOCALGROUPID");
        if (strDBGroupID != null && strDBGroupID.equals(strGroupID) == false)
          return "GROUPINFO_OVERLAP EQID:" + carrierLocId + "->DB:"+strDBGroupID+",REQ:"+strGroupID;
      }
      // OCS상에 등록되어 있는 EQ인지 체크
      for (int i = 0 ; i < vtEQItemList.size() ; i++ )
      {
        strEQName = (String)vtEQItemList.get(i);
        if (vtOwnerList.contains(strEQName) == false)
          return "EQINFO_NOT_EXIST "+strEQName;
      }
      return "OK";
    }
    catch (SQLException e)
    {
      String strLog = "CheckEQInfoInDB() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return "EXCEPTION_ERR";
    }
    finally
    {
      if (rs != null)
        m_DBAccessManager.CloseRecord(rs);
    }
  }

  /**
   * 'SET_VEHICLE_REQUEST' 메시지를 수신했을때 Validation을 체크하는 함수이다.
   * @param strGroupID String
   * @param strSETVHL String
   * @param strDistance String
   * @param strExpiredTime String
   * @return String
   */
  private String CheckSVRMsgValidation(String strGroupID, String strSETVHL,
                                       String strDistance, String strExpiredTime)
  {
    // 2010.02.22 by MYM : 설정값이 Empty이면 리턴 Fail
    if(GetLocalOHTUsage() == false)
      return "LOCALOHT_USAGE_OFF";
    if(strSETVHL.equals(""))
      return "SETVHL_EMPTY_VALUE";
    if(strDistance.equals(""))
      return "DISTANCE_EMPTY_VALUE";
    if(strExpiredTime.equals(""))
      return "EXPIREDTIME_EMPTY_VALUE";

    // strSETVHL, strDistance, strExpiredTime 파라미터 Validation 체크
    int nSetVHL = Integer.parseInt(strSETVHL);
    int nDistance = Integer.parseInt(strDistance);
    int nExpiredTime = Integer.parseInt(strExpiredTime);
    if (nSetVHL < 0)
      return "SETVHL_NEGATIVE_VALUE";
    if (nDistance <= 0)
      return "DISTANCE_NOT_POSITVE_VALUE";
    if (nExpiredTime <= 0)
      return "EXPIREDTIME_NOT_POSITVE_VALUE";

    String strReturnValue = CheckGroupID(strGroupID);
    if (strReturnValue.equals("OK") == false)
      return strReturnValue;

    String strSql = "SELECT * FROM LOCALGROUPINFO WHERE ";
    strSql += "LOCALGROUPID='" + strGroupID + "'";
    ResultSet rs = null;
    try
    {
      rs = m_DBAccessManager.GetRecord(strSql);
      while (rs != null && rs.next())
      {
        int nDBMinVHL = rs.getInt("MINVHL");
        int nDBMaxVHL = rs.getInt("MAXVHL");
        if (nSetVHL < nDBMinVHL)
          return "SETVHL_LESS_THAN_MINVHL";
        if (nSetVHL > nDBMaxVHL)
          return "SETVHL_MORE_THAN_MAXVHL";
      }
      return "OK";
    }
    catch (SQLException e)
    {
      String strLog = "CheckSVRMsgValidation() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return "EXCEPTION_ERR";
    }
    finally
    {
      if (rs != null)
        m_DBAccessManager.CloseRecord(rs);
    }
  }

  private boolean GetEQItemFromEQPMsg(String strEQPMsg, Vector vtEQItemList)
  {
    // EQPMSG: (EQ1,EQ2,EQ3) or EQ1
    int nPos;
    String strEQName;
    nPos = strEQPMsg.indexOf("(");
    strEQPMsg = strEQPMsg.substring(nPos+1);
    nPos = strEQPMsg.indexOf(")");
    if (nPos > 0)
      strEQPMsg = strEQPMsg.substring(0,nPos);

    while (strEQPMsg.length() > 0)
    {
      nPos = strEQPMsg.indexOf(",");
      if (nPos < 0)
      {
        strEQName = strEQPMsg;
        strEQPMsg = "";
      }
      else
      {
        strEQName = strEQPMsg.substring(0, nPos);
        strEQPMsg = strEQPMsg.substring(nPos+1);
      }
      vtEQItemList.add(strEQName);
    }
    return true;
  }

  /**
   * 신규 Group 정보를 DB에 생성하는 함수이다.
   * @param strGroupID String
   * @param strEQPID String
   * @param strMinVHL String
   * @param strMaxVHL String
   * @return boolean
   */
  private boolean CreateGroupInfoInDB(String strGroupID, String strEQPID, String strMinVHL, String strMaxVHL)
  {
    String strSql = "INSERT INTO LOCALGROUPINFO (LOCALGROUPID, ENABLED, MINVHL, MAXVHL) VALUES ('";
    strSql += strGroupID + "','TRUE',";
    strSql += Integer.parseInt(strMinVHL) + "," + Integer.parseInt(strMaxVHL) + ")";

    try
    {
      m_DBAccessManager.ExecSQL(strSql);
      ClearGroupIDToCarrierLoc(strGroupID);
      SetGroupIDToCarrierLoc(strEQPID, strGroupID);
      return true;
    }
    catch (SQLException e)
    {
      String strLog = "CreateGroupInfoInDB() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return false;
    }
  }

  /**
   * GroupID가 DB에 존재할 경우 데이터를 Overwrite하여 변경한다.
   * @param strGroupID String
   * @param strEQPID String
   * @param strMinVHL String
   * @param strMaxVHL String
   * @return boolean
   */
  private boolean ModifyGroupInfoInDB(String strGroupID, String strEQPID, String strMinVHL, String strMaxVHL)
  {
    // 2010.06.25 by MYM : 버그 수정
    // 배경 : 업데이트시 다른 Group 정보의 값도 모두 변경이 되는 현상 발생
    // 원인 : LOCALGROUPID를 업데이트 하는 코드로 되어 있었음.
    String strSql = "UPDATE LOCALGROUPINFO SET ";
    strSql += "MINVHL=" + Integer.parseInt(strMinVHL) + ",";
    strSql += "MAXVHL=" + Integer.parseInt(strMaxVHL) + " ";
    strSql += "WHERE LOCALGROUPID='" + strGroupID + "'";

    try
    {
      m_DBAccessManager.ExecSQL(strSql);
      ClearGroupIDToCarrierLoc(strGroupID);
      SetGroupIDToCarrierLoc(strEQPID, strGroupID);
      return true;
    }
    catch (SQLException e)
    {
      String strLog = "ModifyGroupInfoInDB() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return false;
    }
  }

  /**
   * 주어진 GroupID에 해당하는 정보를 DB상에서 삭제한다.
   * @param strGroupID String
   * @return boolean
   */
  private boolean DeleteGroupInfoInDB(String strGroupID)
  {
	// 2013.07.05 by IKY: 지정된 LocalGroup에 대해서 삭제하도록 함
    String strSql = "DELETE FROM LOCALGROUPINFO";
    if(strGroupID != null)
      strSql += " WHERE LOCALGROUPID='" + strGroupID + "'";

    // 2010.07.13 by MYM : [고정형 베이 전용 OHT] // 2013.07.05 by IKY : VHLOPTION Column이 DB 테이블에서 없어지면서 SQL 구문 변경함 
//	String strSql = "DELETE FROM LOCALGROUPINFO WHERE VHLOPTION IS NULL";
//    if(strGroupID != null)
//       strSql += " AND LOCALGROUPID='" + strGroupID + "'";
    try
    {
      m_DBAccessManager.ExecSQL(strSql);
      ClearGroupIDToCarrierLoc(strGroupID);
      ClearGroupIDInVehicleDB(strGroupID);
      return true;
    }
    catch (SQLException e)
    {
      String strLog = "DeleteGroupInfoInDB() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return false;
    }
  }

  private boolean SetGroupIDToCarrierLoc(String strEQPID, String strGroupID)
  {
    Vector vtSqlList = new Vector();
    Vector vtEQItemList = new Vector();
    String strEQName = "";

    String strSql = "UPDATE CARRIERLOC SET ";
    strSql += "LOCALGROUPID='" + strGroupID + "' WHERE (";

    GetEQItemFromEQPMsg(strEQPID, vtEQItemList);
    for ( int i = 0 ; i < vtEQItemList.size() ; i++ )
    {
      strEQName = (String)vtEQItemList.get(i);
      if (i == vtEQItemList.size()-1)
        strSql += "CARRIERLOCID LIKE '" + strEQName + "%')";
      else
        strSql += "CARRIERLOCID LIKE '" + strEQName + "%' OR ";
    }
    vtSqlList.add(strSql);

    strSql = "UPDATE LOCALGROUPINFO SET BAY=";
 // 2012.08.17 by PMM
    // NODE.AREA -> NODE.BAY for OCS v3.1
//    strSql += "(SELECT AREA FROM CARRIERLOC C, NODE N WHERE C.node=N.nodeid";
    strSql += "(SELECT BAY FROM CARRIERLOC C, NODE N WHERE C.node=N.nodeid";
    strSql += " and C.carrierlocid LIKE '"+strEQName+"%' and ROWNUM=1) WHERE LOCALGROUPID='" + strGroupID +"'";
    vtSqlList.add(strSql);

    try
    {
      ExecSQLList(vtSqlList);
      return true;
    }
    catch (Exception e)
    {
      String strLog = "SetGroupIDToCarrierLoc() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return false;
    }
  }

  private boolean ClearGroupIDToCarrierLoc(String strGroupID)
  {
    String strSql = "UPDATE CARRIERLOC SET LOCALGROUPID=''";
    if(strGroupID != null)
      strSql += " WHERE LOCALGROUPID='" + strGroupID + "'";

    try
    {
      m_DBAccessManager.ExecSQL(strSql);
      return true;
    }
    catch (SQLException e)
    {
      String strLog = "ClearGroupIDToCarrierLoc() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return false;
    }
  }

  private boolean ClearGroupIDInVehicleDB(String strGroupID)
  {
   // 2013.07.05 by IKY : 지정된 LocalGroup에 대한 Vehicle의 LocalGroupID Clear
   String strSql = "UPDATE VEHICLE SET LOCALGROUPID=''";
   if(strGroupID != null)
     strSql += " WHERE LOCALGROUPID='" + strGroupID + "'";

   // 2010.07.13 by MYM : [고정형 베이 전용 OHT]  // 2013.07.05 by IKY : VHLOPTION이 DB에서 없어지면서 SQL 구문 변경함   
//   String strSql = "UPDATE VEHICLE SET LOCALGROUPID='' WHERE LOCALGROUPID NOT IN (SELECT LOCALGROUPID FROM LOCALGROUPINFO WHERE VHLOPTION='FIXED')";
//   if(strGroupID != null)
//     strSql += " AND LOCALGROUPID='" + strGroupID + "'";

   try {
      m_DBAccessManager.ExecSQL(strSql);
      return true;
   }
   catch (SQLException e) {
     String strLog = "ClearGroupIDToCarrierLoc() - Exception:" + e.getMessage();
     MainTrace(strLog, null);
     return false;
   }
  }

  private boolean ModifySetVHLValueInDB(String strGroupID, String strSetVHL,
                                        String strDistance, String strExpiredTime)
  {
    String strSql = "UPDATE LOCALGROUPINFO SET ";
    strSql += "SETVHL=" + Integer.parseInt(strSetVHL) + ",";
    strSql += "DISTANCE=" + Integer.parseInt(strDistance) + ",";
    strSql += "EXPIREDTIME=" + Integer.parseInt(strExpiredTime) + ", ";
    // 2010.02.23 by MYM : SVR UPDATE TIME 기록
    strSql += "UPDATETIME=SYSDATE ";
    strSql += "WHERE LOCALGROUPID='" + strGroupID + "'";

    try
    {
      m_DBAccessManager.ExecSQL(strSql);
      return true;
    }
    catch (SQLException e)
    {
      String strLog = "ModifySetVHLValueInDB() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return false;
    }
  }

  /**
   *
   * @return boolean
   * @version 2010.02.23 by MYM, IKY
   */
  private boolean GetLocalOHTUsage()
  {
    String strSql = "SELECT VALUE FROM OCSINFO WHERE NAME='LOCALOHT_USAGE'";
    ResultSet rs = null;
    boolean bRetVal = false;
    try
    {
      rs = m_DBAccessManager.GetRecord(strSql);
      if (rs != null && rs.next())
      {
        String strValue = rs.getString("VALUE");
        if(strValue != null && strValue.equals("YES"))
          bRetVal = true;
      }
    }
    catch (SQLException e)
    {
      String strLog = "GetLocalOHTUsage() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return bRetVal;
    }
    finally
    {
      if (rs != null)
        m_DBAccessManager.CloseRecord(rs);
    }

    return bRetVal;
  }

  /**
   *
   * @param strGroupID String
   * @return String
   * @version 2010.02.23 by MYM, IKY
   */
  private String CheckGroupID(String strGroupID)
  {
    String strReturnValue = "GROUPINFO_NOT_EXIST "+strGroupID+"";
    String strSql = "SELECT LOCALGROUPID FROM LOCALGROUPINFO WHERE LOCALGROUPID='" + strGroupID + "'";
    ResultSet rs = null;
    try
    {
      rs = m_DBAccessManager.GetRecord(strSql);
      if (rs != null && rs.next())
      {
        strReturnValue = "OK";
      }
    }
    catch (SQLException e)
    {
      String strLog = "CheckGroupID() - Exception:" + e.getMessage();
      MainTrace(strLog, null);
      return "EXCEPTION_ERR";
    }
    finally
    {
      if (rs != null)
        m_DBAccessManager.CloseRecord(rs);
    }

    return strReturnValue;
  }

  private void ExecSQLList(Vector vtSqlList)
  {
    if (vtSqlList.size() > 0)
    {
      for (int i = 0; i < vtSqlList.size(); i++)
      {
        try
        {
          m_DBAccessManager.ExecSQL( (String)vtSqlList.get(i));
        }
        catch (SQLException e)
        {
          String strLog = "ExecSQL() - SQLException: " + e.getMessage();
          MainTrace(strLog, (String) vtSqlList.get(i));
        }
      }
      vtSqlList.clear();
    }
  }
}
