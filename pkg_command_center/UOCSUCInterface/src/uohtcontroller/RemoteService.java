package uohtcontroller;

import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <p>
 * Company: SAMSUNG ELECTRONICS
 * </p> 
 */

public class RemoteService
{
	private DBAccessManager m_DBAccessManager = null;
	private utilLog m_MainLog = null;
	private ServerSocket m_RemoteServerSocket = null;
	
	// Timer Thread
	private TCPServiceThread m_TCPServiceThread = null;

	int m_nServerPort = 4000;
	private int m_nClockInterval = 1000;
	
	private int m_Client = 100; // 2022.03.30 by JJW UCInterface Connection 연결 제한 기능 추가
	
	Vector m_vClientList = null;

	// ////////////////////////////////////////////////////////////////////////
	int STX = 0x02;
	int ETX = 0x03;
	int MTX = 0x04;
	String m_sSTX;
	String m_sETX;
	String m_sMTX;

	// ////////////////////////////////////////////////////////////////////////
	// Runtime Vehicle 메시지 추가
	String m_strUnloadRetryLimitCnt = "0";
	String m_strLoadRetryLimitCnt = "0";
	String m_strRuntimeVehicleDataString = "";
	String m_strDiagnosisInfoDataString = "";

	// 2012.04.27 by MYM : OCS3.0에는 누락되어 추가함.
	String m_strRuntimeAutoRetryDataString = ""; // 2011.06.03 by MYM : MCSmgr Auto Retry 요청 메시지에 대한 응답 메시지

	private static final String TRUE = "TRUE";
	private static final String FALSE = "FALSE";
	private static final String NULL = "NULL";

	public RemoteService(DBAccessManager pDBAccessManager, utilLog MainLog, int nTCPPort, int maxClient) {
		m_nServerPort = nTCPPort;		
		m_DBAccessManager = pDBAccessManager;	
		pDBAccessManager.AddConnection();

		m_MainLog = MainLog;
		m_vClientList = new Vector();
		
		m_Client = maxClient; // 2022.03.30 by JJW UCInterface Connection 연결 제한 기능 추가

		// STX, ETX, MTX 초기화
		char c;

		m_sSTX = " ";
		m_sETX = " ";

		c = (char)STX;
		m_sSTX = m_sSTX.replace(' ', c);

		c = (char)ETX;
		m_sETX = m_sETX.replace(' ', c);
	}
	
	class TCPServiceThread extends Thread
	{
		public boolean m_bRun = true;

		public void run() {
			try {
				while(m_bRun) {
					TCPServiceProcess();
					sleep(m_nClockInterval);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
				StringBuffer sb = new StringBuffer();
				StackTraceElement[] trace = e.getStackTrace();

				int nTraceSize = trace.length;
				for(int k = 0; k < nTraceSize; k++)
					sb.append("\n " + trace[k]);

				String strLog = "TCPServiceThread - Exception: " + sb.toString();
				RemoteServiceTrace(strLog, null);
			}
		}
	}

	public void TCPServiceStart() {
		m_TCPServiceThread = new TCPServiceThread();
		m_TCPServiceThread.start();
		// ServerThread Active
		RemoteServerSocketActive();		
	}

	public void TCPServiceStop() {
		if(m_TCPServiceThread != null) {
			m_TCPServiceThread.m_bRun = false;
			m_TCPServiceThread = null;
			// ServerThread Deactive
			RemoteServerSocketDeactive();	
		}
	}

	private void TCPServiceProcess() {
		// MCSmgr이 요청하는 데이터를 실시간으로 CMessage 형태로 구성함.
		BuildRuntimeVehicleMsg();
		
		// 진단서버에서 요청하는 데이터를 실시간으로 CMessage 형태로 구성함.
		BuildDiagnosisInfoMsg();	
	}

	/**
	 * <p>Description: 특정 Remote Client의 연결을 끊는다.</p>
	 * <p>호출함수명: RemoteService </p>   (Optional)
	 * @param strIPAddress : 연결 해제를 할 Remote Client IP
	 */
	public synchronized void CloseConnection(String strIPAddress) {
		// strIPAddress 가 null 일때는 모든 Client 접속 해제
		// 1. RuntimeUpdate시 모든 Client 강제 접속 해제 (strIPAddress가 null)
		// 2. Client에서 특정 IP의 Client 강제 접속 해제
		if(strIPAddress == null) {
			for(int k = m_vClientList.size() - 1; k >= 0; k--) {
				ServerThread clientSocket = (ServerThread)m_vClientList.elementAt(k);
				clientSocket.CloseConnection();
			}
		} else {
			for(int k = m_vClientList.size() - 1; k >= 0; k--) {
				ServerThread svrThreadTemp = (ServerThread)m_vClientList.elementAt(k);
				String strGetIP = svrThreadTemp.m_ClientSock.getInetAddress().toString();
//				if(strGetIP.equals(strIPAddress)) {
				if(strGetIP.equals("/" + strIPAddress)) {
					svrThreadTemp.CloseConnection();
				}
			}
		}
	}
	
	/**
	 * 2012.09.24 by MYM : 접속한 Client ClientList에 추가를 동기(Sync) 함수화
	 * @param svrThread
	 * @param ClientSock
	 * @return
	 */
	public synchronized boolean addClientList(ServerThread svrThread, Socket ClientSock) {
		try {
			// 3. Client에게 서비스 하는 ServerThread 관리를 위해 List에 추가
			m_vClientList.add(svrThread);
			
			// 4. 접속된 Client IP 및 Port 정보 표시
			String strLog = "Connection Count=" + m_vClientList.size() + ": " + ClientSock.getInetAddress() + "(" + ClientSock.getPort() + ")";
			System.out.println(strLog);
			RemoteServiceTrace(strLog, null);
			
			// 5. 현재 연결된 모든 Client List를 로그에 기록
			ServerThread svrThreadTemp;
			
			for(int k = 0; k < m_vClientList.size(); k++) {
				svrThreadTemp = (ServerThread)m_vClientList.elementAt(k);
				strLog = "           Current Connection list[" + k + "]: " + svrThreadTemp.m_ClientSock.getInetAddress()
				+ "(" + svrThreadTemp.m_ClientSock.getPort() + ")";
				RemoteServiceTrace(strLog, null);
			}
		} catch(Exception e) {
			String strLog = "addClientList Exception" + e.toString() + m_MainLog.DisplayException(e);
			RemoteServiceException(strLog, null);
			return false;
		}
		return true;
	}
	
	/**
	 * 2012.09.24 by MYM : 해제된 Client ClientList에서 제거를 동기(Sync) 함수화  
	 * @param clientSock
	 * @return
	 */
	public synchronized void removeClientList(Socket clientSock) {
		for(int k = m_vClientList.size() - 1; k >= 0; k--) {
			try {
				ServerThread svrThreadTemp = (ServerThread)m_vClientList.elementAt(k);
				InetAddress inetRemtoeClientIP = svrThreadTemp.m_ClientSock.getInetAddress();
				int nPort = svrThreadTemp.m_ClientSock.getPort();
				if(inetRemtoeClientIP.equals(clientSock.getInetAddress()) && nPort == clientSock.getPort()) {
					m_vClientList.removeElementAt(k);
					RemoteServiceTrace("Current Connection Closed: " + inetRemtoeClientIP + "(" + nPort + ")", null);
					break;
				}
			} catch (Exception e) {
				String strLog = "removeClientList Exception " + e.toString() + m_MainLog.DisplayException(e);
				RemoteServiceException(strLog, null);
			}
		}
	}
	
	/**
	 * <p>Title: acceptSocketThread</p>
	 * <p>Description: Client 접속 요청을 받아들이는 클래스</p>
	 * <p>Copyright: Copyright (c) 2008</p>
	 * <p>Company: 삼성전자 생산기술연구소</p>
	 * @version 2008.03.07 By MYM  Client accept 제한을 하지 않도록 변경<BR>
	 */
	class acceptSocketThread extends Thread
	{
		// Thread 실행 flag
		boolean bRun = true;
		int currentSize; // 현재 접속중인 client 개수

		public void run() {
			while(bRun) {
				try {
					if(m_RemoteServerSocket.isClosed() == false) {
						// 1. Client로 부터 연결 요청시 Accept
						//    ※ accept()는 Block 함수로 Client 요청이 있을 때까지 여기서 Block됨.
						Socket ClientSock = m_RemoteServerSocket.accept();
						// 2. Client에게 서비스 해줄 ServerThread 생성 및 실행
						ServerThread svrThread = new ServerThread(ClientSock);

						//  serverThread를 m_vClientList에 추가할 때는 한곳에서 접근하도록 함.
						//  m_vClientList에서 Remove or Add 될 때 Index가 참조 문제가 발생할 수 있음.
						addClientList(svrThread, ClientSock);
						svrThread.start();
						
						currentSize = m_vClientList.size();
						// 2022.03.30 by JJW UCInterface Connection 연결 제한 기능 추가
						if(currentSize > m_Client) {
							ServerThread svrThreadTemp = null; // 강제 종료를 위한 client
							svrThreadTemp = (ServerThread)m_vClientList.elementAt(0);
							svrThreadTemp.isExceedThreadMax = true;
							svrThreadTemp.CloseConnection();
							
							currentSize = m_vClientList.size();
							// 4. 접속된 Client IP 및 Port 정보 표시
							String strLog = "Connection Count=" + m_vClientList.size() + ": " + ClientSock.getInetAddress() + "(" + ClientSock.getPort() + ")";
							System.out.println(strLog);
							RemoteServiceTrace(strLog, null);
							
							for(int k = 0; k < m_vClientList.size(); k++) {
								svrThreadTemp = (ServerThread)m_vClientList.elementAt(k);
								strLog = "           Current Connection list[" + k + "]: " + svrThreadTemp.m_ClientSock.getInetAddress()
								+ "(" + svrThreadTemp.m_ClientSock.getPort() + ")";
								RemoteServiceTrace(strLog, null);
							}
						}
						
					}
				}
				catch(Exception e) {
					String strLog = "acceptSocketThread Exception - run(): " + e.toString() + m_MainLog.DisplayException(e);
					RemoteServiceException(strLog, null);
				}
			}
		}
	}

	/**
	 * <p>Title: ServerThread</p>
	 * <p>Description: Client와 메시지 송수신을 관리하는 클래스</p>
	 * <p>Copyright: Copyright (c) 2008</p>
	 * <p>Company: 삼성전자 생산기술연구소</p>
	 */
	class ServerThread extends Thread
	{
		// Socket, BufferedReader 메시지 수신, PrintWriter 메시지 전송
		Socket m_ClientSock = null;
		private BufferedReader m_InBuffer = null;
		private PrintWriter m_OutBuffer = null;
		private boolean isExceedThreadMax = false; // 2022.03.30 by JJW UCInterface Connection 연결 제한 기능 추가

		// 메시지 수신시 사용하는 버퍼
		String indata = "";
		String strReceiveString = "";
		public String receiveString = "";
		char[] buf = new char[1024];

		// 메시지 수신 사이즈
		int Count = 0;
		boolean m_bStart = false;

		public ServerThread(Socket sock) {
			m_ClientSock = sock;
			try {
				m_InBuffer = new BufferedReader(new InputStreamReader(m_ClientSock.getInputStream()));
				m_OutBuffer = new PrintWriter(m_ClientSock.getOutputStream(), true);

				m_bStart = true;

				String strLog = "ServerThread Started : " + m_ClientSock.getInetAddress() + "(" + m_ClientSock.getPort() +")";
				RemoteServiceTrace(strLog, null);

				String strRemoteIP = m_ClientSock.getInetAddress().toString().substring(1);
				strLog = strRemoteIP + "(" + m_ClientSock.getPort() +")";
				RemoteServiceTrace("CLIENT CONNECTED..", strLog);
			}
			catch(Exception e) {
				String strLog = "ServerThread Exception - ServerThread(): " + m_MainLog.DisplayException(e);
				RemoteServiceException(strLog, null);
				CloseConnection();
			}
		}

		public void run() {
			while(m_bStart) {
				try {
					Count = m_InBuffer.read(buf); // read는 Block 함수

					if(Count >= 0) {
						indata = new String(buf, 0, Count);

						strReceiveString += indata;
						if(Count < 1024) {
							indata = "";
						}

						RemoteServiceTrace(strReceiveString, "");
						if(strReceiveString.indexOf(m_sETX) > -1) {
							SocketReceived(strReceiveString, m_ClientSock.getInetAddress().getHostAddress());
							strReceiveString = "";
						}
					}
					else {
						String strLog = "Connection Closed(Disconnect or I/O Error): " 
							            + m_ClientSock.getInetAddress() + "(" + m_ClientSock.getPort()+")";
						RemoteServiceTrace(strLog, null);

						CloseConnection();
						break;
					}
				}
				catch(Exception e) {
					
					if (this.isExceedThreadMax == false){
						String strLog = "ServerThread Exception - run(): " + m_MainLog.DisplayException(e);
						RemoteServiceException(strLog, null);
	
						CloseConnection();
					} else {
						// 2022.03.30 by JJW UCInterface Connection 연결 제한 기능에 따른 로그 추가
						this.isExceedThreadMax = false;
						String strLog = "Connection Closed(Client max) / " + "Current Client Size: " + m_vClientList.size() + ", Max Client Size: " + m_Client;
						RemoteServiceTrace("CLIENT DISCONNECTED..", strLog);
					}
					break;
				}
			}
				
		}

		void SocketReceived(String strData, String ip) {
			RemoteServiceTrace("RCV> ", strData);	

			String strReturn = RemoteServerClientRead(this, strData, ip);
		
			m_OutBuffer.println(strReturn);
			m_OutBuffer.flush();
			RemoteServiceTrace("SND> ", strReturn);
		}

		void CloseConnection() {
			try {
				m_bStart = false;
				
				// 2022.03.30 by JJW UCInterface Connection 연결 제한 기능에 따른 Buffer 종료 순서 변경
				try {
					m_OutBuffer.close();
				} catch (Exception e) {}
				try {
					m_InBuffer.close();
				} catch (Exception e) {}
				
				try {
					String strRemoteIP = m_ClientSock.getInetAddress().toString().substring(1);
					String strLog = strRemoteIP + "(" + m_ClientSock.getPort() +")";
					RemoteServiceTrace("CLIENT DISCONNECTED..", strLog);

					removeClientList(m_ClientSock);
					m_ClientSock.close();
					this.interrupt(); // 2022.03.30 by JJW UCInterface Connection 연결 제한 기능 추가
				}
				catch(Exception e) {
					String strLog = "ServerThread Exception - CloseConnection(): " + m_MainLog.DisplayException(e);
					RemoteServiceException(strLog, null);
				} 
				finally {
					m_ClientSock = null;
					m_InBuffer = null;
					m_OutBuffer = null;
				}
			}
			catch(Exception e) {
				String strLog = "ServerThread Exception - CloseConnection(): " + m_MainLog.DisplayException(e);
				RemoteServiceException(strLog, null);
			}
		}
	} // End of ServerThread Class


	public void RemoteServerSocketActive() {
		if(m_RemoteServerSocket != null)
			return;

		System.out.println(System.currentTimeMillis() + " RemoteServerSocket Start");

		try {
			m_RemoteServerSocket = new ServerSocket(m_nServerPort);
			new acceptSocketThread().start();

			InetAddress addr = InetAddress.getLocalHost();
			StringBuffer log = new StringBuffer();
			log.append("[Host:").append(addr.getHostName());
			log.append(", IP:").append(addr.getHostAddress());
			log.append(", Port:").append(m_nServerPort).append("]");
			RemoteServiceTrace("RemoteServerSocket Start.. ", log.toString());
		}
		catch(Exception e) {
			StringBuffer sb = new StringBuffer();
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "ServerThread - IOException: " + sb.toString();
			RemoteServiceException(strLog, null);
		}
	}

	public void RemoteServerSocketDeactive() {
		if(m_RemoteServerSocket == null)
			return;

		try {
			m_RemoteServerSocket.close();
			m_RemoteServerSocket = null;
		}
		catch(Exception e) {
			StringBuffer sb = new StringBuffer();
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "ServerThread - IOException: " + sb.toString();
			RemoteServiceException(strLog, null);
		}
	}

	synchronized String RemoteServerClientRead(ServerThread th, String ReadString, String ip) {
		String MsgName;
		CMessage pReadMsg;
		boolean bAcceptReqRemoteMsg = true;
		pReadMsg = new CMessage();
		bAcceptReqRemoteMsg = true;

		// 메시지 모으는 부분은 각 ServerThread에서 하는 것으로 수정
		th.receiveString = ReadString;

		if(th.receiveString.indexOf(m_sETX) > -1) {
			int nSTX, nETX, nLength;
			nSTX = th.receiveString.indexOf(m_sSTX);
			nETX = th.receiveString.indexOf(m_sETX);
			nLength = th.receiveString.length();
			if(nETX < th.receiveString.length())
				th.receiveString = th.receiveString.substring(0, nETX + 1);
			if(nSTX == -1) {
				th.receiveString = "";
				return "";
			}
			else if(nSTX > -1) {
				th.receiveString = th.receiveString.substring(nSTX);
				if(th.receiveString.indexOf(m_sETX) == -1)
					return "";
			}

			ReadString = new String(th.receiveString);
			th.receiveString = "";

			pReadMsg.SetMessage(ReadString);
			MsgName = pReadMsg.GetMessageName();		
			if(bAcceptReqRemoteMsg && "ReqVehicleData".equals(MsgName)) {
				System.out.println(System.currentTimeMillis() + " Recv " + MsgName);
				if("".equals(m_strRuntimeVehicleDataString) == false) {
					System.out.println(System.currentTimeMillis() + " Send Rep" + MsgName);
					return m_strRuntimeVehicleDataString;
				}
			}
			else if(bAcceptReqRemoteMsg && "ReqVehicleErrorData".equals(MsgName)) {
				System.out.println(System.currentTimeMillis() + " Recv " + MsgName);
				if("".equals(m_strRuntimeAutoRetryDataString) == false) {
					System.out.println(System.currentTimeMillis() + " Send Rep" + MsgName);
					return m_strRuntimeAutoRetryDataString;
				}
			}
			else if(bAcceptReqRemoteMsg && "ReqDiagnosisInfoData".equals(MsgName)) {
				System.out.println(System.currentTimeMillis() + " Recv " + MsgName);
				if("".equals(m_strDiagnosisInfoDataString) == false) {
					System.out.println(System.currentTimeMillis() + " Send Rep" + MsgName);
					return m_strDiagnosisInfoDataString;
				}
			}			
		}
		return "";
	}

	/**
	 * Runtime Vehicle 메시지 생성
	 *
	 * VEHICLEID	                Vehicle ID
	 * CARRIERID	                Carrier ID (null or CarrierID)
	 * TRQUEUEDTIME	        MCS에서 OCS로 반송명령 받은 시간 (null or 시간)
	 * SOURCELOC	                Source Port Location (null or Location)
	 * DESTLOC	                Destination Port Location (null or Location)
	 * ERRORCODE	                Vehicle Error Code (0 or Error Code)
	 * ERRORTIME	                Vehicle Error 발생 시간 (null or 시간)
	 * RETRYCOUNT	                Auto Retry시 Retry 카운트 (0, 1 이상이면 Retry 한 것임)
	 * RETRYLOC	                Auto Retry를 한 Port Location (null or Location)
	 * UNLOADRETRYLIMITCOUNT	Source Port Location에서 최대 Auto Retry 제한 횟수
	 * LOADRETRYLIMITCOUNT	Destination Port Location에서 최대 Auto Retry 제한 횟수
	 * STATUSCHANGEDTIME    
	 *
	 * @return boolean
	 */
	public boolean BuildRuntimeVehicleMsg_Old()
	{
		int nCount = 0;
		boolean bRetVal = true;
		CMessage pVehicleMsg = new CMessage();
		pVehicleMsg.SetMessageName("ReqVehicleData_Rep");

		String strVehicleID = "";
		String strCarrierID = "";
		String strCurrNode = "";
		String strRemoteCmd = "";
		String strTrQueuedTime = "";
		String strTrStatus = "";
		String strSourceNode = "";
		String strSourceLoc = "";
		String strDestNode = "";
		String strDestLoc = "";
		String strHWErrorCode = "";
		String strRetryErrorCode = "0";
		String strErrorTime = "0";
		String strRetryCount = "0";
		String strRetryLoc = "";
		String strJobPause = "";
		String strVehicleLocalGroupID= "";
		String strStatusChangedTime= "";
		
		String strSql = "SELECT C.VEHICLEID, C.REMOTECMD, C.CARRIERID, C.CURRNODE, C.TRQUEUEDTIME, C.DETAILSTATUS AS TRSTATUS, C.SOURCENODE, C.SOURCELOC, C.DESTNODE, C.DESTLOC, C.ERRORCODE AS HWERRORCODE, D.ALARMCODE AS RETRYERRORCODE, D.SETTIME AS ERRORTIME, NVL2(C.PAUSECOUNT,C.PAUSECOUNT,0) AS RETRYCOUNT, D.CARRIERLOC AS RETRYLOC, C.PAUSE AS JOBPAUSE, C.LOCALGROUPID AS LOCALGROUPID FROM "
			+ "(SELECT A.VEHICLEID, A.CURRNODE, A.ERRORCODE, A.LOCALGROUPID, B.REMOTECMD, B.CARRIERID, B.TRQUEUEDTIME, B.DETAILSTATUS, B.SOURCENODE, B.SOURCELOC, B.DESTNODE, B.DESTLOC, B.PAUSECOUNT, B.PAUSE FROM VEHICLE A, TrCmd B WHERE A.ENABLED='TRUE' AND B.VEHICLE (+)= A.VEHICLEID) C, "
			+ "(SELECT A.VEHICLE, A.ALARMCODE, A.SETTIME, A.CARRIERLOC FROM VEHICLEERRORHISTORY A, (SELECT VEHICLE, MAX(SETTIME)AS SETTIME FROM VEHICLEERRORHISTORY WHERE (SHOWMSG='TRUE' OR SHOWMSG IS NULL) AND TYPE = 'AUTO_RETRY' AND SETTIME > TO_CHAR(SYSDATE-1,'YYYYMMDDHH24MISS') AND (ALARMCODE BETWEEN 901 AND 919) GROUP BY VEHICLE) B WHERE A.VEHICLE = B.VEHICLE AND A.SETTIME= B.SETTIME ORDER BY A.VEHICLE) D "
			+ "WHERE D.VEHICLE (+)= C.VEHICLEID ORDER BY C.VEHICLEID";

		ResultSet rs = null;

		long lDBStatusTime = System.currentTimeMillis();
		try
		{
			rs = m_DBAccessManager.GetRecord(strSql);
			if(rs != null) {
				while(rs.next()) {
					strVehicleID = rs.getString("VEHICLEID");
					if(strVehicleID == null) continue;

					strRemoteCmd = rs.getString("REMOTECMD");
					if(strRemoteCmd == null) strRemoteCmd = "";

					strCarrierID = rs.getString("CARRIERID");
					if(strCarrierID == null) strCarrierID = "";

					strCurrNode = rs.getString("CURRNODE");
					if(strCurrNode == null) strCurrNode = "";

					strTrQueuedTime = rs.getString("TRQUEUEDTIME");
					if(strTrQueuedTime == null) strTrQueuedTime = "";

					strTrStatus = rs.getString("TRSTATUS");
					if(strTrStatus == null) strTrStatus = "";

					strSourceNode = rs.getString("SOURCENODE");
					if(strSourceNode == null) strSourceNode = "";

					strSourceLoc = rs.getString("SOURCELOC");
					if(strSourceLoc == null) strSourceLoc = "";

					strDestNode = rs.getString("DESTNODE");
					if(strDestNode == null) strDestNode = "";

					strDestLoc = rs.getString("DESTLOC");
					if(strDestLoc == null) strDestLoc = "";

					strHWErrorCode = rs.getString("HWERRORCODE");
					if(strHWErrorCode == null) strHWErrorCode = "0";

					strRetryErrorCode = rs.getString("RETRYERRORCODE");
					if(strRetryErrorCode == null) strRetryErrorCode = "0";

					strErrorTime = rs.getString("ERRORTIME");
					if(strErrorTime == null) strErrorTime = "0";

					strRetryCount = rs.getString("RETRYCOUNT");
					if(strRetryCount == null || strRetryErrorCode.equals("0")) strRetryCount = "0";

					strRetryLoc = rs.getString("RETRYLOC");
					if(strRetryLoc == null) strRetryLoc = "";

					strJobPause = rs.getString("JOBPAUSE");
					
					if(strJobPause == null || strJobPause.equals(FALSE)) strRetryLoc = "";

					strVehicleLocalGroupID = rs.getString("LOCALGROUPID");
					if(strVehicleLocalGroupID == null) strVehicleLocalGroupID = "";
					
					pVehicleMsg.SetMessageItem("VEHICLE.NAME", strVehicleID, true);
					pVehicleMsg.SetMessageItem("VEHICLE.REMOTECMD", strRemoteCmd, true);
					pVehicleMsg.SetMessageItem("VEHICLE.CARRIERID", strCarrierID, true);
					pVehicleMsg.SetMessageItem("VEHICLE.CURRNODE", strCurrNode, true);
					pVehicleMsg.SetMessageItem("VEHICLE.TRSTATUS", strTrStatus, true);
					pVehicleMsg.SetMessageItem("VEHICLE.SOURCENODE", strSourceNode, true);
					pVehicleMsg.SetMessageItem("VEHICLE.SOURCELOC", strSourceLoc, true);
					pVehicleMsg.SetMessageItem("VEHICLE.DESTNODE", strDestNode, true);
					pVehicleMsg.SetMessageItem("VEHICLE.DESTLOC", strDestLoc, true);
					pVehicleMsg.SetMessageItem("VEHICLE.HWERRORCODE", strHWErrorCode, true);
					pVehicleMsg.SetMessageItem("VEHICLE.RETRYERRORCODE", strRetryErrorCode, true);
					pVehicleMsg.SetMessageItem("VEHICLE.ERRORTIME", strErrorTime, true);
					pVehicleMsg.SetMessageItem("VEHICLE.RETRYCOUNT", strRetryCount, true);
					pVehicleMsg.SetMessageItem("VEHICLE.RETRYLOC", strRetryLoc, true);
					pVehicleMsg.SetMessageItem("VEHICLE.JOBPAUSE", strJobPause, true);
					pVehicleMsg.SetMessageItem("VEHICLE.LOCALGROUPID", strVehicleLocalGroupID, true);					
					
					nCount++;
				}
			}

			pVehicleMsg.SetMessageItem("VEHICLE.Count", nCount, false);
			m_strRuntimeVehicleDataString = pVehicleMsg.ToMessage();
			System.out.println(System.currentTimeMillis() + ":" + (System.currentTimeMillis()- lDBStatusTime) + " BuildRuntimeVehicle End");
		}
		catch(Exception e)
		{
			StringBuffer sb = new StringBuffer(e.getMessage()+"\n");
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "BuildRuntimeVehicleMsg() - SQLException: " + sb.toString();
			RemoteServiceException(strLog, "");			

			bRetVal = false;
		}
		finally {
			if(rs != null) {
				m_DBAccessManager.CloseRecord(rs);
			}
		}
		pVehicleMsg = null;

		
		// 2012.04.27 by MYM : OCS3.0에는 누락되어 추가함.
		lDBStatusTime = System.currentTimeMillis();
		// 2011.06.03 by MYM : MCSmgr Auto Retry 요청 메시지에 대한 응답 메시지
		nCount = 0;
		int alarmcode;
		String sourceloc, destloc, vehicle, node, carrierloc, alarmtext, settime, cleartime, type, trstatus, trcmdid, carrierid;
		CMessage repAutoRetryMGRMsg = new CMessage(); 
		repAutoRetryMGRMsg.SetMessageName("ReqVehicleErrorData_Rep"); 
		strSql = "SELECT * FROM VEHICLEERRORHISTORY WHERE " +
						 "(VEHICLE IN (SELECT VEHICLEID FROM VEHICLE WHERE ERRORCODE<>0 AND ENABLED='TRUE') AND TYPE='ERROR' AND SETTIME > TO_CHAR(SYSDATE-1/24/60,'YYYYMMDDHH24MISS')) OR" +
						 "(TYPE<>'ERROR' AND SHOWMSG IS NULL AND SETTIME > TO_CHAR(SYSDATE-1/24/60,'YYYYMMDDHH24MISS')) ORDER BY VEHICLE, SETTIME DESC";
		try
		{
			rs = m_DBAccessManager.GetRecord(strSql);
			if(rs != null) {
				while(rs.next()) {
					vehicle = rs.getString("VEHICLE");
					if(vehicle == null) {
						continue;
					}
					
					trcmdid = MakeString(rs.getString("TRCMDID"));
					if("".equals(trcmdid)) {
						trstatus = ""; carrierloc = ""; sourceloc = ""; destloc = ""; carrierloc = "";carrierid = "";
					}
					else {
						trstatus = MakeString(rs.getString("TRSTATUS"));
						carrierloc = MakeString(rs.getString("CARRIERLOC"));
						sourceloc = MakeString(rs.getString("SOURCELOC")); 
						destloc = MakeString(rs.getString("DESTLOC"));
						if(trstatus == null) carrierloc = ""; 
						else if(trstatus.startsWith("UNLOAD")) carrierloc = sourceloc;
						else if(trstatus.startsWith("LOAD")) carrierloc = destloc;
						else carrierloc = "";
						carrierid = MakeString(rs.getString("CARRIERID"));
					}
					alarmcode = rs.getInt("ALARMCODE");
					settime = MakeString(rs.getString("SETTIME"));
					// TYPE정의 : ERROR(H/W Error), ERROR_PIO(PIO Error), AUTO_RETRY(PIO Auto Retry), AUTO_POSITION(Going Auto Position)
					type = MakeString(rs.getString("TYPE"));
					if("ERROR".equals(type)) {
						if(alarmcode >= 901 && alarmcode <= 919) {
							type += "_PIO";
						}
					}
					else if("AUTO_POSITION".equals(type)){
						if(alarmcode >= 901 && alarmcode <= 916) {
							type = "AUTO_RETRY";
						}
					}
					alarmtext = MakeString(rs.getString("ALARMTEXT"));
					node = MakeString(rs.getString("NODE"));
					cleartime = MakeString(rs.getString("CLEARTIME"));
					
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.VEHICLE", vehicle, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.TRSTATUS", trstatus, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.CARRIERLOC", carrierloc, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.SOURCELOC", sourceloc, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.DESTLOC", destloc, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.ALARMCODE", alarmcode, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.SETTIME", settime, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.CLEARTIME", cleartime, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.TYPE", type, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.ALARMTEXT", alarmtext, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.NODE", node, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.CARRIERID", carrierid, true);
					repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.TRCMDID", trcmdid, true);
					nCount++;
				}
			}
			repAutoRetryMGRMsg.SetMessageItem("VHLERRORHISTORY.Count", nCount, false);
			m_strRuntimeAutoRetryDataString = repAutoRetryMGRMsg.ToMessage();
			System.out.println(System.currentTimeMillis() + ":" + (System.currentTimeMillis()- lDBStatusTime) + " BuildRuntimeVehicleAutoRetry End");
		}
		catch(Exception e)
		{
			StringBuffer sb = new StringBuffer();
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "BuildRuntimeVehicleMsg() - SQLException: " + sb.toString();
			RemoteServiceException("BuildRuntimeMsg()", strLog);

			bRetVal = false;
		}
		finally {
			if(rs != null) {
				m_DBAccessManager.CloseRecord(rs);
			}
		}
		repAutoRetryMGRMsg = null;
		
		return bRetVal;
	}
	
	public boolean BuildRuntimeVehicleMsg()
	{
		int nCount = 0;
		boolean bRetVal = true;
		CMessage pVehicleMsg = new CMessage();
		pVehicleMsg.SetMessageName("ReqVehicleData_Rep");

		String strVehicleID = "";
		String strCarrierID = "";
		String strCurrNode = "";
		String strCurrLoc = "";
		String strRemoteCmd = "";
//		String strTrQueuedTime = "";
		String strTrStatus = "";
		String strSourceNode = "";
		String strSourceLoc = "";
		String strDestNode = "";
		String strDestLoc = "";
		String strHWErrorCode = "";
//		String strRetryErrorCode = "0";
//		String strErrorTime = "0";
//		String strRetryCount = "0";
//		String strRetryLoc = "";
		String strJobPause = "";
		String strVehicleLocalGroupID= "";
		
//		String strSql = "SELECT C.VEHICLEID, C.REMOTECMD, C.CARRIERID, C.CURRNODE, C.TRQUEUEDTIME, C.DETAILSTATUS AS TRSTATUS, C.SOURCENODE, C.SOURCELOC, C.DESTNODE, C.DESTLOC, C.ERRORCODE AS HWERRORCODE, D.ALARMCODE AS RETRYERRORCODE, D.SETTIME AS ERRORTIME, NVL2(C.PAUSECOUNT,C.PAUSECOUNT,0) AS RETRYCOUNT, D.CARRIERLOC AS RETRYLOC, C.PAUSE AS JOBPAUSE, C.LOCALGROUPID AS LOCALGROUPID FROM "
//			+ "(SELECT A.VEHICLEID, A.CURRNODE, A.ERRORCODE, A.LOCALGROUPID, B.REMOTECMD, B.CARRIERID, B.TRQUEUEDTIME, B.DETAILSTATUS, B.SOURCENODE, B.SOURCELOC, B.DESTNODE, B.DESTLOC, B.PAUSECOUNT, B.PAUSE FROM VEHICLE A, TrCmd B WHERE A.ENABLED='TRUE' AND B.VEHICLE (+)= A.VEHICLEID) C, "
//			+ "(SELECT A.VEHICLE, A.ALARMCODE, A.SETTIME, A.CARRIERLOC FROM VEHICLEERRORHISTORY A, (SELECT VEHICLE, MAX(SETTIME)AS SETTIME FROM VEHICLEERRORHISTORY WHERE (SHOWMSG='TRUE' OR SHOWMSG IS NULL) AND TYPE = 'AUTO_RETRY' AND SETTIME > TO_CHAR(SYSDATE-1,'YYYYMMDDHH24MISS') AND (ALARMCODE BETWEEN 901 AND 919) GROUP BY VEHICLE) B WHERE A.VEHICLE = B.VEHICLE AND A.SETTIME= B.SETTIME ORDER BY A.VEHICLE) D "
//			+ "WHERE D.VEHICLE (+)= C.VEHICLEID ORDER BY C.VEHICLEID";
		String strSql = "SELECT A.VEHICLEID, A.ERRORCODE AS HWERRORCODE, B.REMOTECMD, B.CARRIERID, A.CURRNODE, B.CARRIERLOC AS CURRLOC, B.DETAILSTATUS AS TRSTATUS, B.SOURCENODE, B.SOURCELOC, B.DESTNODE, B.DESTLOC, B.PAUSE AS JOBPAUSE, A.LOCALGROUPID"
				+ " FROM VEHICLE A, TrCmd B WHERE A.ENABLED='TRUE' AND B.VEHICLE (+)= A.VEHICLEID ORDER BY VEHICLEID";

		ResultSet rs = null;

		long lDBStatusTime = System.currentTimeMillis();
		try
		{
			rs = m_DBAccessManager.GetRecord(strSql);
			if(rs != null) {
				while(rs.next()) {
					strVehicleID = rs.getString("VEHICLEID");
					if(strVehicleID == null) continue;

					strRemoteCmd = MakeString(rs.getString("REMOTECMD"));
					strCarrierID = MakeString(rs.getString("CARRIERID"));
					strCurrNode = MakeString(rs.getString("CURRNODE"));
					strCurrLoc = MakeString(rs.getString("CURRLOC"));
//					strTrQueuedTime = rs.getString("TRQUEUEDTIME");
//					if(strTrQueuedTime == null) strTrQueuedTime = "";
					strTrStatus = MakeString(rs.getString("TRSTATUS"));
					strSourceNode = MakeString(rs.getString("SOURCENODE"));
					strSourceLoc = MakeString(rs.getString("SOURCELOC"));
					strDestNode = MakeString(rs.getString("DESTNODE"));
					strDestLoc = MakeString(rs.getString("DESTLOC"));
					strHWErrorCode = MakeString(rs.getString("HWERRORCODE"));
//					if(strHWErrorCode == null) strHWErrorCode = "0";
//					strRetryErrorCode = rs.getString("RETRYERRORCODE");
//					if(strRetryErrorCode == null) strRetryErrorCode = "0";
//					strErrorTime = rs.getString("ERRORTIME");
//					if(strErrorTime == null) strErrorTime = "0";
//					strRetryCount = rs.getString("RETRYCOUNT");
//					if(strRetryCount == null || strRetryErrorCode.equals("0")) strRetryCount = "0";
//					strRetryLoc = rs.getString("RETRYLOC");
//					if(strRetryLoc == null) strRetryLoc = "";
					strJobPause = MakeString(rs.getString("JOBPAUSE"));
//					if(strJobPause == null || strJobPause.equals(FALSE)) strRetryLoc = "";
					strVehicleLocalGroupID = MakeString(rs.getString("LOCALGROUPID"));
					
					pVehicleMsg.SetMessageItem("VEHICLE.NAME", strVehicleID, true);
					pVehicleMsg.SetMessageItem("VEHICLE.REMOTECMD", strRemoteCmd, true);
					pVehicleMsg.SetMessageItem("VEHICLE.CARRIERID", strCarrierID, true);
					pVehicleMsg.SetMessageItem("VEHICLE.CURRNODE", strCurrNode, true);
					pVehicleMsg.SetMessageItem("VEHICLE.CURRLOC", strCurrLoc, true);
					pVehicleMsg.SetMessageItem("VEHICLE.TRSTATUS", strTrStatus, true);
					pVehicleMsg.SetMessageItem("VEHICLE.SOURCENODE", strSourceNode, true);
					pVehicleMsg.SetMessageItem("VEHICLE.SOURCELOC", strSourceLoc, true);
					pVehicleMsg.SetMessageItem("VEHICLE.DESTNODE", strDestNode, true);
					pVehicleMsg.SetMessageItem("VEHICLE.DESTLOC", strDestLoc, true);
					pVehicleMsg.SetMessageItem("VEHICLE.HWERRORCODE", strHWErrorCode, true);
//					pVehicleMsg.SetMessageItem("VEHICLE.RETRYERRORCODE", strRetryErrorCode, true);
//					pVehicleMsg.SetMessageItem("VEHICLE.ERRORTIME", strErrorTime, true);
//					pVehicleMsg.SetMessageItem("VEHICLE.RETRYCOUNT", strRetryCount, true);
//					pVehicleMsg.SetMessageItem("VEHICLE.RETRYLOC", strRetryLoc, true);
					pVehicleMsg.SetMessageItem("VEHICLE.JOBPAUSE", strJobPause, true);
					pVehicleMsg.SetMessageItem("VEHICLE.LOCALGROUPID", strVehicleLocalGroupID, true);					
					
					nCount++;
				}
				pVehicleMsg.SetMessageItem("VEHICLE.Count", nCount, false);
			}
		}
		catch(Exception e)
		{
			StringBuffer sb = new StringBuffer(e.getMessage()+"\n");
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "BuildRuntimeVehicleMsg(Vehicle) - SQLException: " + sb.toString();
			RemoteServiceException(strLog, "");			
			
			bRetVal = false;
		}
		finally {
			if(rs != null) {
				m_DBAccessManager.CloseRecord(rs);
			}
		}
		
		try
		{
			// 2012.04.27 by MYM : OCS3.0에는 누락되어 추가함.
			lDBStatusTime = System.currentTimeMillis();
			// 2011.06.03 by MYM : MCSmgr Auto Retry 요청 메시지에 대한 응답 메시지
			nCount = 0;
			int alarmcode;
			String sourceloc, destloc, vehicle, node, carrierloc, alarmtext, settime, cleartime, type, trstatus, trcmdid, carrierid;
			strSql = "SELECT * FROM VEHICLEERRORHISTORY WHERE " +
							 "(VEHICLE IN (SELECT VEHICLEID FROM VEHICLE WHERE ERRORCODE<>0 AND ENABLED='TRUE') AND TYPE='ERROR' AND SETTIME > TO_CHAR(SYSDATE-1/24/60,'YYYYMMDDHH24MISS')) OR" +
							 "(TYPE<>'ERROR' AND SHOWMSG IS NULL AND SETTIME > TO_CHAR(SYSDATE-1/24/60,'YYYYMMDDHH24MISS')) ORDER BY VEHICLE, SETTIME DESC";

			rs = m_DBAccessManager.GetRecord(strSql);
			if(rs != null) {
				while(rs.next()) {
					vehicle = rs.getString("VEHICLE");
					if(vehicle == null) {
						continue;
					}
					
					trcmdid = MakeString(rs.getString("TRCMDID"));
					if("".equals(trcmdid)) {
						trstatus = ""; carrierloc = ""; sourceloc = ""; destloc = ""; carrierloc = "";carrierid = "";
					}
					else {
						trstatus = MakeString(rs.getString("TRSTATUS"));
						carrierloc = MakeString(rs.getString("CARRIERLOC"));
						sourceloc = MakeString(rs.getString("SOURCELOC")); 
						destloc = MakeString(rs.getString("DESTLOC"));
						if(trstatus == null) carrierloc = ""; 
						else if(trstatus.startsWith("UNLOAD")) carrierloc = sourceloc;
						else if(trstatus.startsWith("LOAD")) carrierloc = destloc;
						else carrierloc = "";
						carrierid = MakeString(rs.getString("CARRIERID"));
					}
					alarmcode = rs.getInt("ALARMCODE");
					settime = MakeString(rs.getString("SETTIME"));
					// TYPE정의 : ERROR(H/W Error), ERROR_PIO(PIO Error), AUTO_RETRY(PIO Auto Retry), AUTO_POSITION(Going Auto Position)
					type = MakeString(rs.getString("TYPE"));
					if("ERROR".equals(type)) {
						if(alarmcode >= 901 && alarmcode <= 919) {
							type += "_PIO";
						}
					}
					else if("AUTO_POSITION".equals(type)){
						if(alarmcode >= 901 && alarmcode <= 916) {
							type = "AUTO_RETRY";
						}
					}
					alarmtext = MakeString(rs.getString("ALARMTEXT"));
					node = MakeString(rs.getString("NODE"));
					cleartime = MakeString(rs.getString("CLEARTIME"));
					
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.VEHICLE", vehicle, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.TRSTATUS", trstatus, true);
//					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.CARRIERLOC", carrierloc, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.CURRLOC", carrierloc, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.SOURCELOC", sourceloc, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.DESTLOC", destloc, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.ALARMCODE", alarmcode, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.SETTIME", settime, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.CLEARTIME", cleartime, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.TYPE", type, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.ALARMTEXT", alarmtext, true);
//					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.NODE", node, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.CURRNODE", node, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.CARRIERID", carrierid, true);
					pVehicleMsg.SetMessageItem("VHLERRORHISTORY.TRCMDID", trcmdid, true);
					nCount++;
				}
				pVehicleMsg.SetMessageItem("VHLERRORHISTORY.Count", nCount, false);
			}

			m_strRuntimeVehicleDataString = pVehicleMsg.ToMessage();
			System.out.println(System.currentTimeMillis() + ":" + (System.currentTimeMillis()- lDBStatusTime) + " BuildRuntimeVehicle End");
		}
		catch(Exception e)
		{
			StringBuffer sb = new StringBuffer(e.getMessage()+"\n");
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "BuildRuntimeVehicleMsg(VehicleErrorHistory) - SQLException: " + sb.toString();
			RemoteServiceException(strLog, "");			

			bRetVal = false;
		}
		finally {
			if(rs != null) {
				m_DBAccessManager.CloseRecord(rs);
			}
		}
		pVehicleMsg = null;
		
		return bRetVal;
	}
	
	/**
	 * (진단서버에서 요청하는 정보 생성)
	 * Vehicle 관련 정보 : Name, Type, Mode, Status, Enabled, Node, StopNode, TargetNode, ErrorCode, JobPause,
	 *                  APSignal, APMacAddress, Material, Zone, Area, Bay
	 * HID 관련 정보 : UnitID, Type, IPAddress, Enabled, Status, Voltage, ElectricCurrent, Temperature, Frequency, ErrorCode
	 * VehicleErrorHistory : Vehicle, TrStatus, CarrierLoc, AlarmCode, Settime, Type, AlarmText, Node, CarrierID             
	 * @author neospace
	 * @since 2013.10.01
	 */
	public boolean BuildDiagnosisInfoMsg()
	{
		int Count = 0;
		int intItem;
		boolean bRetVal = true;
		String strSql, strItem;
		CMessage pMessage = new CMessage();
		pMessage.SetMessageName("ReqDiagnosisInfoData_Rep");
		
		ResultSet rs = null;

		long lStartTime = System.currentTimeMillis();		
		// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Vehicle 정보 가져오기
		strSql = "SELECT * FROM VEHICLE";
		Count = 0;
		try {
			rs = m_DBAccessManager.GetRecord(strSql);
			if(rs != null) {
				while(rs.next()) {
					strItem = rs.getString("VehicleID");
					pMessage.SetMessageItem("VEHICLE.NAME", strItem, true);
					pMessage.SetMessageItem("VEHICLE.TYPE", "OHT", true);
					strItem = rs.getString("VehicleMode");
					if(strItem.equals("A"))
						strItem = "AutoMode";
					else
						strItem = "ManualMode";
					pMessage.SetMessageItem("VEHICLE.MODE", strItem, true);

					strItem = rs.getString("Status");
					if(strItem.equals("I"))
						strItem = "OHTInit";
					else if(strItem.equals("G"))
						strItem = "OHTGoing";
					else if(strItem.equals("A"))
						strItem = "OHTArrived";
					else if(strItem.equals("U"))
						strItem = "OHTUnloading";
					else if(strItem.equals("N"))
						strItem = "OHTUnloaded";
					else if(strItem.equals("L"))
						strItem = "OHTLoading";
					else if(strItem.equals("O"))
						strItem = "OHTLoaded";
					else if(strItem.equals("V"))
						strItem = "OHTAutoRecovery";
					else if(strItem.equals("W"))
						strItem = "OHTCompleteRecovery";
					else if(strItem.equals("E"))
						strItem = "OHTError";
					else
						strItem = "Other";
					pMessage.SetMessageItem("VEHICLE.STATUS", strItem, true);				
				
					strItem = rs.getString("Enabled");
					pMessage.SetMessageItem("VEHICLE.ENABLED", strItem, true);
					if(TRUE.equals(strItem)) {
						strItem = rs.getString("CurrNode");
						pMessage.SetMessageItem("VEHICLE.NODE", strItem, true);
						strItem = rs.getString("StopNode");
						pMessage.SetMessageItem("VEHICLE.STOPNODE", strItem, true);
						strItem = rs.getString("TargetNode");
						pMessage.SetMessageItem("VEHICLE.TARGETNODE", strItem, true);
					}
					else {
						strItem = rs.getString("CurrNode");
						pMessage.SetMessageItem("VEHICLE.NODE", strItem, true);
						pMessage.SetMessageItem("VEHICLE.STOPNODE", strItem, true);
						pMessage.SetMessageItem("VEHICLE.TARGETNODE", strItem, true);
					}

					intItem = rs.getInt("ErrorCode");
					pMessage.SetMessageItem("VEHICLE.ERRORCODE", intItem, true);
				
					try {
						strItem = rs.getString("APSIGNAL");
					} catch(Exception e) {strItem = "";}
					pMessage.SetMessageItem("VEHICLE.APSIGNAL", strItem, true);
					
					try {
						strItem = rs.getString("APMACADDRESS");
					} catch (Exception e) {strItem = "";}
					pMessage.SetMessageItem("VEHICLE.APMACADDRESS", strItem, true);
					
					try {
						strItem = rs.getString("MapVersion");
					} catch(Exception e) {strItem = "";}
					pMessage.SetMessageItem("VEHICLE.MAPVERSION", strItem, true);
					
					try {
						strItem = rs.getString("MATERIAL");
						pMessage.SetMessageItem("VEHICLE.MATERIAL", strItem, true);
					} catch (Exception e) {strItem = "";}

					try {
						strItem = rs.getString("IPADDRESS");
						pMessage.SetMessageItem("VEHICLE.IPADDRESS", strItem, true);
					} catch (Exception e) {strItem = "";}

					Count++;
				}
			}
		}
		catch(SQLException e) {
			StringBuffer sb = new StringBuffer(e.getMessage()+"["+e.getErrorCode()+"]\n");
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "BuildDiagnosisInfoMsg(Vehicle) - SQLException: " + sb.toString();
			RemoteServiceException("BuildDiagnosisInfoMsg()", strLog);
		}
		finally {
			if(rs != null) {
				m_DBAccessManager.CloseRecord(rs);
			}
		}
		pMessage.SetMessageItem("VEHICLE.Count", Count, false);
		
		// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// HID 정보 가져오기
		strSql = "SELECT A.*, B.TEMPERATURE AS TML_TEMPERATURE, B.VOLTAGE AS TML_VOLTAGE, B.ERRORCODE AS TML_ERRORCODE, "
			+ "B.IPADDRESS AS TML_IPADDRESS, B.ENABLED AS TML_ENABLED "
			+ "FROM (SELECT * FROM UNITDEVICE WHERE TYPE='HID') A, (SELECT * FROM UNITDEVICE WHERE TYPE='TERMINAL') B WHERE B.UNITID (+)= A.UNITID "
			+ "ORDER BY LPAD((CASE WHEN INSTR(A.UNITID, 'HID') = 1 THEN SUBSTR(A.UNITID, 4) ELSE A.UNITID END), 3, '0')";
		Count = 0;
		try {
			rs = m_DBAccessManager.GetRecord(strSql);
			if(rs != null) {
				while(rs.next()) {
					strItem = rs.getString("UNITID");
					pMessage.SetMessageItem("HID.UNITID", strItem, true);

					strItem = rs.getString("TYPE");
					pMessage.SetMessageItem("HID.TYPE", strItem, true);

					strItem = rs.getString("IPADDRESS");
					pMessage.SetMessageItem("HID.IPADDRESS", strItem, true);

					strItem = rs.getString("ENABLED");
					pMessage.SetMessageItem("HID.ENABLED", strItem, true);

					strItem = rs.getString("STATUS");
					pMessage.SetMessageItem("HID.STATUS", strItem, true);

					strItem = rs.getString("VOLTAGE");
					pMessage.SetMessageItem("HID.VOLTAGE", strItem, true);

					strItem = rs.getString("ELECTRICCURRENT");
					pMessage.SetMessageItem("HID.ELECTRICCURRENT", strItem, true);

					strItem = rs.getString("TEMPERATURE");
					pMessage.SetMessageItem("HID.TEMPERATURE", strItem, true);

					strItem = rs.getString("FREQUENCY");
					pMessage.SetMessageItem("HID.FREQUENCY", strItem, true);

					strItem = rs.getString("ERRORCODE");
					pMessage.SetMessageItem("HID.ERRORCODE", strItem, true);

					strItem = rs.getString("ALTHID");
					pMessage.SetMessageItem("HID.ALTHID", strItem, true);
					strItem = "";
					try { strItem = rs.getString("BACKUPHID"); } catch(Exception ignore) {}
					pMessage.SetMessageItem("HID.BACKUPHID", strItem, true);
					
					strItem = "";
					try { strItem = rs.getString("SYNCSTATUS"); } catch(Exception ignore) {}
					pMessage.SetMessageItem("HID.SYNCSTATUS", strItem, true);				

					Count++;
				}
			}
		}
		catch(SQLException e) {
			StringBuffer sb = new StringBuffer(e.getMessage()+"["+e.getErrorCode()+"]\n");
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "BuildDiagnosisInfoMsg(HID) - SQLException: " + sb.toString();
			RemoteServiceException("BuildDiagnosisInfoMsg()", strLog);
		}
		finally {
			if(rs != null) {
				m_DBAccessManager.CloseRecord(rs);
			}
		}
		pMessage.SetMessageItem("HID.Count", Count, false);
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// VehicleErrorHistory
//		strSql = "SELECT * FROM VEHICLEERRORHISTORY ORDER BY VEHICLE, SETTIME DESC"; // TEST용 임시 코드

		strSql = "SELECT * FROM VEHICLEERRORHISTORY WHERE " +
		 "(VEHICLE IN (SELECT VEHICLEID FROM VEHICLE WHERE ERRORCODE<>0 AND ENABLED='TRUE') AND TYPE='ERROR' AND SETTIME > TO_CHAR(SYSDATE-1/24/60,'YYYYMMDDHH24MISS')) OR" +
		 "(TYPE<>'ERROR' AND SHOWMSG IS NULL AND SETTIME > TO_CHAR(SYSDATE-1/24/60,'YYYYMMDDHH24MISS')) ORDER BY VEHICLE, SETTIME DESC";

		Count = 0;
		try {
			rs = m_DBAccessManager.GetRecord(strSql);
			if(rs != null) {
				String strVHL, strSourceLoc, strDestLoc, strType, strSetTime, strAlarmText, strTrStatus, strNode, strCarrieID;
				int nAlarmCode;
				while(rs.next()) {
					strVHL = rs.getString("VEHICLE");
					if(strVHL == null) continue;
					pMessage.SetMessageItem("VHLERRORHISTORY.VEHICLE", strVHL, true);

					strTrStatus = rs.getString("TRSTATUS");
					pMessage.SetMessageItem("VHLERRORHISTORY.TRSTATUS", strTrStatus, true);

					if(strTrStatus == null) strTrStatus = "";
					if(strTrStatus.startsWith("UNLOAD"))
					{
						strSourceLoc = rs.getString("SOURCELOC");
						pMessage.SetMessageItem("VHLERRORHISTORY.CARRIERLOC", strSourceLoc, true);
					}
					else if(strTrStatus.startsWith("LOAD"))
					{
						strDestLoc = rs.getString("DESTLOC");
						pMessage.SetMessageItem("VHLERRORHISTORY.CARRIERLOC", strDestLoc, true);
					}
					else
					{
						pMessage.SetMessageItem("VHLERRORHISTORY.CARRIERLOC", "", true);
					}
				
					nAlarmCode = rs.getInt("ALARMCODE");
					pMessage.SetMessageItem("VHLERRORHISTORY.ALARMCODE", nAlarmCode, true);

					strSetTime = rs.getString("SETTIME");
					pMessage.SetMessageItem("VHLERRORHISTORY.SETTIME", strSetTime, true);
					
					strType = rs.getString("TYPE");
					pMessage.SetMessageItem("VHLERRORHISTORY.TYPE", strType, true);

					strAlarmText = rs.getString("ALARMTEXT");
					pMessage.SetMessageItem("VHLERRORHISTORY.ALARMTEXT", strAlarmText, true);

					strNode = rs.getString("NODE");
					pMessage.SetMessageItem("VHLERRORHISTORY.NODE", strNode, true);

					strCarrieID = rs.getString("CARRIERID");
					pMessage.SetMessageItem("VHLERRORHISTORY.CARRIERID", strCarrieID, true);
					
					Count++;
				}
			}
		}
		catch(Exception e) {
			StringBuffer sb = new StringBuffer(e.getMessage()+"\n");
			StackTraceElement[] trace = e.getStackTrace();
			for(int k = 0; k < trace.length; k++)
				sb.append("\n " + trace[k]);
			String strLog = "BuildDiagnosisInfoMsg(AutoRetry,AutoPosition) - Exception: " + sb.toString();
			RemoteServiceException("BuildDiagnosisInfoMsg()", strLog);
		}
		finally {
			if(rs != null) {
				m_DBAccessManager.CloseRecord(rs);
			}
		}
		pMessage.SetMessageItem("VHLERRORHISTORY.Count", Count, false);
		
		m_strDiagnosisInfoDataString = pMessage.ToMessage();
		System.out.println(System.currentTimeMillis() + ":" + (System.currentTimeMillis() - lStartTime) + " BuildDiagnosisInfoMsg End");

		return true;	
	}
	
	private static final String EMPTY_STRING = "";
	public String MakeString(String string) {
		if(string == null)
			return EMPTY_STRING;
		return string;
	}
	
	void RemoteServiceTrace(String strLog1, String strLog2) {
		m_MainLog.WriteReturnLog("MCSmgrTCPComm", strLog1, strLog2, true);
	}
	
	void RemoteServiceException(String strLog1, String strLog2) {
		m_MainLog.WriteReturnLog("Exception", strLog1, strLog2, true);
	}
	
}
