package com.samsung.ocs.unitdevice;

import java.util.*;
import java.sql.SQLException;
import java.net.*;
import java.sql.ResultSet;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.NetworkDevice;


/**
 * <p>Title: UnifiedOCS 1.0 for JAVA</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2010.05.??</p>
 * <p>Company: SAMSUNG ELECTRONICS</p>
 * @author 강연국
 * @version 1.0
 * 2010.06.01 by KYK
 */

/**
 * NetDeviceOperation.java는 NetworkDevice 동작을 관리하는 모듈로
 * AP,HUB,HIDMaster 등에 PingCheck를 하여 네트웤 연결 상태를 관리한다.
 */

public class NetDeviceOperation
{
	int m_nInterval = 100; // Thread Interval
	int m_nLimitCount = 5; // OneThread안에 관리하는 리스트개수

	NetDeviceManager m_pNetDeviceManager = null; 
	private DBAccessManager m_DBAccessManager = null;  

	Hashtable m_vtNetDeviceGroupTable = new Hashtable(); //ex) m_vtNetDeviceGroupTable.put("AP1", "G1");
	Hashtable m_htPingThreadTable = new Hashtable(); // 실행중인 PingCheckThread 객체를 테이블에 저장관리
	
	private static final String NET_DEVICE_OPERATION_TRACE = "NetDeviceOperationDebug";
	private static final String NET_DEVICE_OPERATION_EXCEPTION_TRACE = "NetDeviceOperationException";
	private static Logger operationTraceLog = Logger.getLogger(NET_DEVICE_OPERATION_TRACE);
	private static Logger operationExceptionTraceLog = Logger.getLogger(NET_DEVICE_OPERATION_EXCEPTION_TRACE);

	/**
	 * NetDeviceOperation의 생성자이다.
	 */
	public NetDeviceOperation(NetDeviceManager pNetDeviceManager, DBAccessManager pDBAccessManager) // UOCSMain pUOCSMain 추가
	{
		m_pNetDeviceManager = pNetDeviceManager;
		m_DBAccessManager = pDBAccessManager;
		pDBAccessManager.AddConnection();
	}
	/**
	 * NetDevice PingCheckThread interval 설정 from DB OCSInfo
	 * 한 Thread에서 관리하는 NetDevice 수도 고려해야 한다.
	 */
	public void SetThreadIntervalFromDB()
	{
		// DB에서 'Thread Interval' 값 가져오기 : OCSInfo
		String strSql = "SELECT * FROM OCSINFO WHERE NAME='NETDEVICE_THREAD_INTERVAL'";
		ResultSet rs = null;
		String strValue = null;

		try
		{
			rs = m_DBAccessManager.GetRecord(strSql);
			if (rs != null)
			{
				while (rs.next())
				{
					strValue = rs.getString("VALUE");
				}
				if(strValue != null)
					m_nInterval = Integer.parseInt(strValue);
			}
		}
		//+2010.12.?? by KYK : 예외처리 catch - finally 보완
		catch (Exception e)
		{
			operationTrace("DBException SetThreadIntervalFromDB", e);
		}
		finally
		{
			if (rs != null)
			{
				m_DBAccessManager.CloseRecord(rs);
				rs = null;
			}
		}//-2010.12.?? by KYK	  
	}

//	/**
//	 * 한 Thread에서 관리하는 NetDevice 개수 결정 from DB OCSInfo
//	 * NetDevice PingCheckThread interval 설정 from DB 
//	 */ 
//	public void SetLimitCountOneThreadFromDB()
//	{
//		// DB에서 'NETDEVICE_LIMIT_PER_THREAD' 값 가져오기 : OCSInfo
//		String strSql =
//			"SELECT * FROM OCSINFO WHERE NAME='NETDEVICE_COUNT_PER_THREAD'";
//		ResultSet rs = null;
//		String strValue = null;
//
//		try
//		{
//			rs = m_DBAccessManager.GetRecord(strSql);
//			if (rs != null)
//			{
//				while (rs.next())
//				{
//					strValue = rs.getString("VALUE");
//				}
//				if (strValue != null)
//					m_nLimitCount = Integer.parseInt(strValue);
//			}
//		}
//		//+2010.12.?? by KYK : 예외처리 catch - finally 보완
//		catch (Exception e)
//		{
//			operationTrace("DBException SetLimitCountOneThreadFromDB", e);
//		}
//		finally
//		{
//			if (rs != null)
//			{
//				m_DBAccessManager.CloseRecord(rs);
//				rs = null;
//			}
//		}//-2010.12.?? by KYK	  
//	}  

	/**
	 * 상태관리 할  대상Unit(Enabled='TRUE') 대하여 PingCheck 쓰레드 생성 [1 Thread /5개NetDevice]
	 * 호출부 : 처음 실행시, Initialize()
	 */
	public void NetDeviceOperationStart()
	{
		Vector vtUnitIDList = new Vector();
		m_pNetDeviceManager.GetNameList(vtUnitIDList);

		// 2010.06.01 by KYK +
		SetThreadIntervalFromDB();
//		SetLimitCountOneThreadFromDB();

		//  1. UnitID 수에 맞게 PingCheck 쓰레드 생성
		String strUnitID, strIPAddress, strGroupID;
		boolean bEnabled = false;
		int nCount = 0;
		PingCheckThread pingCheckThread = null;
		strGroupID = "";

		for(int i=0; i<vtUnitIDList.size(); i++)
		{
			strUnitID = (String)vtUnitIDList.get(i);
			NetworkDevice nd = (NetworkDevice) m_pNetDeviceManager.getNetDeviceTable().get(strUnitID);
			bEnabled = nd.isEnabled();
			//      strIPAddress = nd.getIpAddress();
			//      bEnabled = m_pNetDeviceManager.GetBoolean(strUnitID + ".ENABLED");
			//      strIPAddress = m_pNetDeviceManager.GetString(strUnitID + ".IPADDRESS");

			if(bEnabled == false)
				continue;

			if( (nCount % m_nLimitCount) == 0)
			{
				strGroupID = "G" + m_htPingThreadTable.size();
				// PingCheckThread 생성
				pingCheckThread = new PingCheckThread(m_nLimitCount,m_nInterval);
				m_htPingThreadTable.put(strGroupID, pingCheckThread);
			}
			m_vtNetDeviceGroupTable.put(strUnitID, strGroupID);
			// PingCheckThread 에 관리 NetDevice 추가
			
			// 2011.01.07 by PMM
			//pingCheckThread.AddNetDevice(strUnitID, strIPAddress);
			//if(pingCheckThread != null) pingCheckThread.AddNetDevice(strUnitID, strIPAddress);
			if(pingCheckThread != null) pingCheckThread.AddNetDevice(strUnitID, nd.getIpAddress());
			nCount++;
		}

		// 2. 생성한 쓰레드 시작
		for(Enumeration e = m_htPingThreadTable.elements(); e.hasMoreElements();)
		{
			pingCheckThread = (PingCheckThread)e.nextElement();
			pingCheckThread.start();
		}
	}

	/**
	 * 실시간 변경점 업데이트 (관리대상 추가,삭제)
	 * 호출부 : MainProcess()   
	 */
	public void UpdatePingCheckThread()
	{
		// 2013.05.08 by KYK : try-catch 추가
		try {		
			// 2010.10.25 by KYK : NetDevice + Usage 추가  
			if(m_htPingThreadTable.isEmpty())
			{
				// 모듈실행 중 Usage No -> YES 될 때 PingCheckThread 생성함수 호출
				NetDeviceOperationStart();
				return;
			}

			Vector vtUnitIDList = new Vector();
			m_pNetDeviceManager.GetNameList(vtUnitIDList);
			Vector vtAddUnitIDList = new Vector();
			Vector vtRemoveUnitIDList = new Vector();
			Vector refreshUnitIdList = new Vector();

			String strUnitID, strGroupID;
			boolean bEnabled;
			PingCheckThread pingCheckThread = null;
			strGroupID = "";
			int nCount = 0;

			// 1-1. DB에서 삭제 또는 Disabled 된  List 만들기
			for (Enumeration e = m_vtNetDeviceGroupTable.keys(); e.hasMoreElements(); )
			{
				strUnitID = (String) e.nextElement();
				//      bEnabled = m_pNetDeviceManager.GetBoolean(strUnitID + ".ENABLED");
				NetworkDevice nd = (NetworkDevice) m_pNetDeviceManager.getNetDeviceTable().get(strUnitID);
				// 2013.03.29 by KYK
				if (nd == null) {
					vtRemoveUnitIDList.add(strUnitID);
				} else {
					bEnabled = nd.isEnabled();
					if (bEnabled == false || vtUnitIDList.contains(strUnitID) == false) {
						vtRemoveUnitIDList.add(strUnitID);
					} else {
						refreshUnitIdList.add(strUnitID);
					}				
				}
			}

			// 1-2.PingCheckThread 에서 삭제대상  제거
			if(vtRemoveUnitIDList.size() > 0)
			{
				for (int i = 0; i < vtRemoveUnitIDList.size(); i++)
				{
					strUnitID = (String) vtRemoveUnitIDList.get(i);
					strGroupID = (String) m_vtNetDeviceGroupTable.remove(strUnitID);
					if (strGroupID != null)
					{
						pingCheckThread = (PingCheckThread) m_htPingThreadTable.get(strGroupID);
						pingCheckThread.RemoveNetDevice(strUnitID);

						// 삭제 후 해당쓰레드에 PingCheck 대상 아이템없으면 쓰레드 종료
						if(pingCheckThread.m_htNetDeviceTable.size()==0)
						{
							pingCheckThread.stopped();
						}
					}
				}
			}

			// 2-1. DB에서 Enabled 또는 추가대상 List 만들기
			for (int i = 0; i < vtUnitIDList.size(); i++)
			{
				strUnitID = (String) vtUnitIDList.get(i);
				NetworkDevice nd = (NetworkDevice) m_pNetDeviceManager.getNetDeviceTable().get(strUnitID);
				// 2013.03.29 by KYK
				if (nd != null) {
					bEnabled = nd.isEnabled();
					//      bEnabled = m_pNetDeviceManager.GetBoolean(strUnitID + ".ENABLED");

					if (bEnabled == true && m_vtNetDeviceGroupTable.containsKey(strUnitID) == false) {
						vtAddUnitIDList.add(strUnitID);
						refreshUnitIdList.remove(strUnitID);
					}				
				}
			}

			// 2-1-1 ip가 변경되엇을수도 잇으니까 갱신해주자.
			for(Iterator it = refreshUnitIdList.iterator(); it.hasNext(); ) {
				String unitId = (String) it.next();
				strGroupID = (String) m_vtNetDeviceGroupTable.get(unitId);
				if (strGroupID != null) {
					NetworkDevice nd = (NetworkDevice) m_pNetDeviceManager.getNetDeviceTable().get(unitId);
					pingCheckThread = (PingCheckThread) m_htPingThreadTable.get(strGroupID);
					// 2013.03.29 by KYK
					if (nd != null && pingCheckThread != null) {
						pingCheckThread.putNetDevice(unitId, nd.getIpAddress());					
					}
				}
			}

			// 2-2. PingCheckThread 아이템 추가 : 기존 테이블 Capa 허용되는 쓰레드 찾아서 아이템추가

			// 추가해야할 목록이 있을 경우,
			if (vtAddUnitIDList.size() > 0)
			{
				for (Enumeration e = m_vtNetDeviceGroupTable.elements(); e.hasMoreElements(); )
				{
					strGroupID = (String) e.nextElement();
					pingCheckThread = (PingCheckThread) m_htPingThreadTable.get(strGroupID);

					// 2011.01.07 by PMM
					if(pingCheckThread != null)
					{
						if (pingCheckThread.IsLimitCountOver() == true)
							continue;

						while (vtAddUnitIDList.size() > 0)
						{

							if (pingCheckThread.IsLimitCountOver() == false)
							{
								strUnitID = (String) vtAddUnitIDList.remove(0);
								NetworkDevice nd = (NetworkDevice) m_pNetDeviceManager.getNetDeviceTable().get(strUnitID);
								//            bEnabled = nd.isEnabled();
								//            strIPAddress = m_pNetDeviceManager.GetString(strUnitID +
								//                ".IPADDRESS");

								// 2013.03.29 by KYK
								if (nd != null) {
									pingCheckThread.AddNetDevice(strUnitID, nd.getIpAddress());
									//업데이트
									m_vtNetDeviceGroupTable.put(strUnitID, strGroupID);								
								}
							}
							else
							{
								// 다 채워서 더 채울 것 없음
								break;
							}
						}					
					}
				}
				// 추가할 것 채우다가, 기존 테이블에서 Capa가 다 차 있으면 새로 쓰레드 생성하여 실행
				if (vtAddUnitIDList.size() > 0)
				{
					Vector vtStartThreadList = new Vector();
					for (int i = 0; i < vtAddUnitIDList.size(); i++)
					{
						strUnitID = (String) vtAddUnitIDList.get(i);
						NetworkDevice nd = (NetworkDevice) m_pNetDeviceManager.getNetDeviceTable().get(strUnitID);
						// 2013.03.29 by KYK
						if (nd == null) {
							continue;
						}
						bEnabled = nd.isEnabled();
						//          bEnabled = m_pNetDeviceManager.GetBoolean(strUnitID + ".ENABLED");
						//          strIPAddress = m_pNetDeviceManager.GetString(strUnitID + ".IPADDRESS");

						if (bEnabled == false)
							continue;

						if ( (nCount % m_nLimitCount) == 0)
						{
							strGroupID = "G" + m_htPingThreadTable.size();
							pingCheckThread = new PingCheckThread(m_nLimitCount,m_nInterval);
							m_htPingThreadTable.put(strGroupID, pingCheckThread);
							vtStartThreadList.add(pingCheckThread);
						}
						m_vtNetDeviceGroupTable.put(strUnitID, strGroupID);
						// 2011.01.07 by PMM
						//pingCheckThread.AddNetDevice(strUnitID, strIPAddress);
						//if(pingCheckThread != null) pingCheckThread.AddNetDevice(strUnitID, strIPAddress);
						if(pingCheckThread != null) pingCheckThread.AddNetDevice(strUnitID, nd.getIpAddress());
						nCount++;
					}

					for (int i = 0; i < vtStartThreadList.size(); i++)
					{
						pingCheckThread = (PingCheckThread) vtStartThreadList.get(i);
						pingCheckThread.start();
					}
				}
			}
		} catch (Exception e) {
			operationTrace("Exception UpdatePingCheckThread", e);
		}

	}

	/**
	 * @param strStatus
	 * @param strName
	 * UnitID와 Status를 전달받아 DB 업데이트 함
	 */
	public void UpdateNetDeviceStatusIntoDB(String strStatus, String strName)
	{

		String strSql = "UPDATE NETWORKDEVICE SET STATUS='" + strStatus +
		"' WHERE UNITID='" + strName + "'";

		try
		{
			m_DBAccessManager.ExecSQL(strSql);
		}
		catch (SQLException e)
		{
			operationTrace("SQLException UpdateNetDeviceStatusIntoDB", e);
		}
	}

	// PingCheckThread 종료 후 관리테이블 정리
	public void ClearPingThreadTable()
	{
		// 2013.05.08 by KYK : try-catch 추가
		try {
			if(m_htPingThreadTable.isEmpty()==false) {
				// 2013.03.29 by KYK
				for (Enumeration<PingCheckThread> e = m_htPingThreadTable.elements(); e.hasMoreElements();) {
					PingCheckThread thread = e.nextElement();
					thread.stopped();
				}
				m_htPingThreadTable.clear();
			}			
		} catch (Exception e) {
			operationTrace("Exception ClearPingThreadTable", e);
		}
	}

	/*****************************************************************
	 * @author yk09.kang
	 * PingCheckThread : 일정 주기로 서버에  Ping 보내 Alive 및 응답시간을 확인  
	 * ***************************************************************
	 */
	class PingCheckThread extends Thread
	{
		Hashtable m_htNetDeviceTable = null;
		int m_nItemIndex;
		boolean m_bAlive;
		int m_nLimitCount = 5;
		int m_nInterval = 100;

		// 생성자
		PingCheckThread(int nLimitCount, int nInterval)
		{
			m_nLimitCount = nLimitCount;
			m_htNetDeviceTable = new Hashtable();
			m_nItemIndex = 0;
			m_bAlive = true;
			m_nInterval = nInterval;
		}

		// 해당 쓰레드에 NetDevice 추가
		public boolean AddNetDevice(String strUnitID, String ipAddress )
		{
			if(m_htNetDeviceTable.size() >= m_nLimitCount)
				return false;

			synchronized(m_htNetDeviceTable)
			{
				m_htNetDeviceTable.put(strUnitID, ipAddress);
			}

			return true;
		}

		public void putNetDevice(String unitId, String ipAddress) {
			if(m_htNetDeviceTable!=null) {
				synchronized(m_htNetDeviceTable) {
					if(m_htNetDeviceTable.get(unitId)!=null) {
						m_htNetDeviceTable.put(unitId, ipAddress);
					}
				}
			}
		}

		//    public boolean AddNetDevice(String strUnitID, String strIPAddress)
		//    {
		//    	if(m_htNetDeviceTable.size() >= m_nLimitCount)
		//    		return false;
		//    	
		//    	synchronized(m_htNetDeviceTable)
		//    	{
		//    		m_htNetDeviceTable.put(strUnitID, strIPAddress);
		//    	}
		//    	
		//    	return true;
		//    }

		// 해당 쓰레드에서 NetDevice 제거
		public void RemoveNetDevice(String strUnitID)
		{
			synchronized(m_htNetDeviceTable)
			{
				m_htNetDeviceTable.remove(strUnitID);
			}
		}

		public boolean IsLimitCountOver()
		{
			if(m_htNetDeviceTable.size() >= m_nLimitCount)
				return true;
			return false;
		}

		// 스레드 종료
		public void stopped()
		{
			m_bAlive = false;
			//System.out.println("m_bIsStop:"+m_bIsStop);
		}

		/**
		 * PingCheckProcess : PingCheck 하는 Main Logic 함수
		 */
		public void PingCheckProcess()
		{
			// Usage 추가
			boolean bNetDeviceCheckUsage = m_pNetDeviceManager.getNetDeviceCheckUsage();

			if(!bNetDeviceCheckUsage)
			{	  // Usage='NO' 이면 Thread 종료
				stopped();
				m_htNetDeviceTable.clear();      	  
				return;    	
			}

			Vector vtNetDeviceList = new Vector(m_htNetDeviceTable.keySet());
			if (vtNetDeviceList.size() > m_nItemIndex)
			{
				try{

					String strUnitID = (String) vtNetDeviceList.get(m_nItemIndex);
					String ipAddress = (String) m_htNetDeviceTable.get(strUnitID); // value 값을 가져온다. (Type)

					// 2013.12.10 by KYK
//					PingCheckProcess(strUnitID, ipAddress); // TargetIP에 ICMP를 송신하는 함수
					pingCheckProcess(strUnitID, ipAddress); // TargetIP에 ICMP를 송신하는 함수
					m_nItemIndex++;
				}
				catch(Exception e)
				{
					m_nItemIndex = 0;
				}
			}
			else
			{    	  
				m_nItemIndex = 0;    	
			}
		}

		// 스레드 Loop
		public void run()
		{
			// 2010.10.25 by KYK : m_bStart -> m_bAlive 변경	
			while (m_bAlive)
			{
				try
				{
					PingCheckProcess();
					sleep(m_nInterval); // 스레드가 차지하는 리소스 양보(context switching)
				}
				catch (InterruptedException e)
				{
					operationTrace("InterruptedException PingCheckProcess", e);
				}
			}
			System.out.println("Thread End");
		}

//		/**
//		 * @param strAddress
//		 * @return String
//		 * Ping 보내서 응답메시지를 반환하는 함수
//		 */
//		// 
//		String SendPingCommand(String strAddress)
//		{
//			String strResponseMsg = "";
//			try
//			{
//				InputStream in = null;
//				int nCount = -1;
//
//				String osName = System.getProperty("os.name");
//				if (osName.indexOf("Windows") >= 0){
//					Runtime rt = Runtime.getRuntime();
//					Process p = rt.exec("ping -n 2 " + strAddress);
//
//					//          String strSendMsgLog2 = "SND_After> " + strAddress;
//					//          System.out.print(strSendMsgLog2);
//
//					in = p.getInputStream();
//					while ( (nCount = in.read()) != -1)
//					{
//						strResponseMsg = strResponseMsg +
//						new String(new Character( (char) nCount).toString());
//					}
//					in.close();
//				}
//				else if (osName.indexOf("Linux") >= 0){
//					Runtime rt = Runtime.getRuntime();
//					Process p = rt.exec("ping -c 2 " + strAddress);
//
//					in = p.getInputStream();
//					while ( (nCount = in.read()) != -1)
//					{
//						strResponseMsg = strResponseMsg +
//						new String(new Character( (char) nCount).toString());
//					}
//					in.close();
//				}
//				// 2011.09.15 by KYK : S1L Server 이설 (Linux -> UNIX 머신) 에 따른 
//				// UNIX Ping & Reply 변경
//				else if(osName.indexOf("SunOS") >= 0){
//					Runtime rt = Runtime.getRuntime();
//					Process p = rt.exec("ping " + strAddress + " 2");
//					
//					in = p.getInputStream();
//					while ( (nCount = in.read()) != -1){
//						strResponseMsg = strResponseMsg +
//						new String(new Character( (char) nCount).toString());
//					}
//					in.close();
//				}
//				else{
//					Runtime rt = Runtime.getRuntime();
//					Process p = rt.exec("ping " + strAddress);
//					
//					in = p.getInputStream();
//					while ( (nCount = in.read()) != -1){
//						strResponseMsg = strResponseMsg +
//						new String(new Character( (char) nCount).toString());
//					}
//					in.close();					
//				}
//				
//				return strResponseMsg;
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace();
//				operationTrace("Exception SendPingCommand - " + strResponseMsg, e);
//				return strResponseMsg;
//			}
//		}
		
//		private static final String WINDOWS = "Windows";
//		private static final String LINUX = "Linux";
//		private static final String SUN = "SunOS";
//		private static final String OSNAME = "os.name";
//		
//		/**
//		 * 2012.06.19 by MYM
//		 * 스크립트 실행한 Process를 destroy 하도록 수정 및 함수 정리
//		 * 
//		 * @param address
//		 * @return
//		 */
//		String sendPingCommand(String address) {
//			StringBuffer command = new StringBuffer("ping "); 
//			String osName = System.getProperty(OSNAME);
//			if (osName.indexOf(WINDOWS) >= 0){
//				command.append("-n 2 ").append(address);
//				return execScript(command.toString());
//			} else if (osName.indexOf(LINUX) >= 0){
//				command.append("-c 2 ").append(address);
//				return execScript(command.toString());
//			} else if (osName.indexOf(SUN) >= 0){
//				command.append(address).append(" 2");
//				return execScript(command.toString());
//			} else {
//				command.append(address);
//				return execScript(command.toString());
//			}
//		}

//		/**
//		 * 2012.06.19 by MYM
//		 * 스크립트 실행한 Process를 destroy 하도록 수정 및 함수 정리
//		 * 
//		 * @param command
//		 * @return
//		 */
//		private String execScript(String command) {
//			if (command == null || command.length() == 0) {
//				return "";
//			}
//			
//			String result = "";
//			Runtime rt = Runtime.getRuntime();
//			Process ps = null;
//			try {
//				ps = rt.exec(command);
//				BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
//				StringWriter writer = new StringWriter();
//				String cl = null;
//				while ((cl = reader.readLine()) != null) {
//					writer.write(cl.trim()+"\n");
//				}
//				result = writer.toString();
//				reader.close();
//			} catch (Exception e) {
//				operationTrace("Exception SendPingCommand - " + result, e);
//			} finally {
//				if (ps != null) {
//					ps.destroy();
//				}
//			}
//			return result;
//		}
		
//		/**
//		 * @param strRepMsg
//		 * @return boolean
//		 * Ping 보내서 응답받은 메시지로 Alive or Fail 여부 판단
//		 * Fail 판단기준 : "Unreachable" or "time out" or "100%" or ("TTL" & "ttl" 이 없는 경우)	
//		 */
//		boolean IsCommStatusOK(String strRepMsg)
//		{
//			// 2011.09.15 by KYK : S1L Server 이설 (Linux -> UNIX 머신) 에 따른  UNIX Ping & Reply 변경
//			if(strRepMsg == null || strRepMsg == ""){
//				return false;
//			}			
//			String osName = System.getProperty("os.name");
//			if(osName.indexOf("SunOS") >= 0 ){
//				if(strRepMsg.indexOf("is alive") > 0){
//					return true;
//				} else if(strRepMsg.indexOf("no answer") > 0){
//					return false;
//				} else {
//					return false;
//				}
//			}
//			else{
//				if (strRepMsg.indexOf("Unreachable") > 0 || strRepMsg.indexOf("timed out") > 0
//						|| strRepMsg.equals("")==true || ExtractPacketLoss(strRepMsg).equals("100%")==true
//						|| ((strRepMsg.indexOf("TTL") < 0) && (strRepMsg.indexOf("ttl") < 0))){
//					return false;
//				}					
//				else return true;				
//			}			
//		}

//		//
//		String ExtractPacketLoss(String strRepMsg)
//		{			
//			// Extract Packet Loss
//			String[] saTemp = strRepMsg.split("\n");
//			String strPacket = "";
//			String strPacketLoss = "";
//			for (int i = 0; i < saTemp.length; i++)
//			{
//				if (saTemp[i].indexOf("%") >= 0)
//				{
//					strPacket = saTemp[i];
//					break;
//				}
//			}
//			if (strPacket.equals("") == false)
//			{
//				String[] saPacket = strPacket.split(" ");
//				for (int i = 0; i < saPacket.length; i++)
//				{
//					if (saPacket[i].indexOf("%") >= 0)
//					{
//						if(saPacket[i].indexOf("(") >=0)
//							strPacketLoss = saPacket[i].substring(1);
//						else
//							strPacketLoss = saPacket[i];
//
//						//System.out.println(strPacketLoss);
//						break;
//					}
//				}
//			}
//			return strPacketLoss;
//		}

//		/**
//		 * TargetIP에 ICMP메시지를 보내어(PingCheck) 결과를 리턴하는 함수
//		 * 2011.09.15 by KYK
//		 * [JAVA 5.0 (InetAddress) isReachable method 대신 (Runtime) exec(ping ~) 사용이유]
//		 * 1. Invalid IP 예외처리 없이 Status='FAIL' 로 처리
//		 * 2. Ping 결과 파싱하여 PacketLoss 계산 (UNIX 는 안됨;)
//		 * 차후에는 아래 isReachable 이용한 pingCheckProcess() 로 변경고려해볼 필요 있음
//		 */		
//		void PingCheckProcess(String strName, String strAddress)
//		{
//			boolean bIsReach = false;
//			String strStatus = null;
//
//			try
//			{
//				// 2011.02.28 by LWG : 괜히 익셉션 내는거 같아서 뺌 
////				InetAddress address = InetAddress.getByName(strAddress);
////				long start, end, gap;
//
//				// 2011.06.19 by MYM : SendPingCommand -> sendPingCommand 변경 
////				String strRepMsg = SendPingCommand(strAddress); // Ping을 보내 응답메시지 받음
//				String strRepMsg = sendPingCommand(strAddress);
//				bIsReach = IsCommStatusOK(strRepMsg); // 응답메시지로 success/fail 구분				
//				String strPacketLoss = ExtractPacketLoss(strRepMsg); // 응답메시지로 PacketLoss(%) 추출
//
//				// 2011.09.15 by KYK
//				if(bIsReach) {
//					strStatus = "ALIVE";
//				} else {
//					strStatus = "FAIL";
//				}
//
//				String osName = System.getProperty("os.name");
//				if(osName.indexOf("SunOS") >= 0 ) {
//					if(bIsReach == true) {
//						strPacketLoss = "0%";
//					} else {
//						strPacketLoss = "100%";
//					}					
//				}				
//				
//				StringBuffer sb = new StringBuffer();
//				sb.append(strName); sb.append("> IP: "); 
//				sb.append(strAddress); sb.append(" , status : ");
//				sb.append(strStatus); sb.append(" (PacketLoss : "); 
//				sb.append(strPacketLoss); sb.append(")"); 
//				
////				m_MainLog.WriteReturnLog("NetDeviceStatus", sb.toString(), null, true);
//				operationTrace(sb.toString(), null);
////				String strLog1 = strName + "> IP: " + strAddress + " , status : ";
////				strLog1 += strStatus + " (PacketLoss : " + strPacketLoss + ")";
////				m_MainLog.WriteReturnLog("NetDeviceStatus", strLog1, null, true);
//
//				System.out.println(new Date() + " IP : " + strAddress +
//						"  status : " + strStatus + " (PacketLoss : " + strPacketLoss + ")");
//
//				// PingCheck 결과 Status DB에 업데이트
//				UpdateNetDeviceStatusIntoDB(strStatus, strName);
//			}
//			catch (Exception e)
//			{
//				System.err.println(new Date() + " Unknown host " + strAddress);
//				operationTrace("Exception PingCheckProcess", e);
//			}
//		}
		
		/**
		 * 2011.09.15 by KYK
		 * pingCheckProcess() : java method 를 이용한 pingCheck
		 * AS-WAS : java method - isReachable() 사용했으나 기능동작에 한계로 바꿈
		 * AS-IS : rt.exec("ping -c 2 " + strAddress); 이용, but OS별 ping 방식다른문제/OS별추가필요;
		 * TO-BE : 다시 isReachable 로 구현 , but PackageLoss 계산이 안됨 
		 * 
		 */
		
		private void pingCheckProcess(String name, String ipAddress){
			
			boolean isReachable = false;
			int timeout = 2000;
			String status = "FAIL";
			
			try {
				InetAddress target = InetAddress.getByName(ipAddress);
				isReachable = target.isReachable(timeout);
				if(isReachable) {
					status = "ALIVE";
				}

				StringBuffer sb = new StringBuffer();
				sb.append(name).append("> IP: ").append(ipAddress).append(" , status : ").append(status);				
				operationTrace(sb.toString(), null);
//				System.out.println(sb.toString());

				// PingCheck 결과 Status DB에 업데이트
				UpdateNetDeviceStatusIntoDB(status, name);
				
			} catch (Exception e) {
				System.err.println(new Date() + " Unknown host " + ipAddress);
				operationTrace("Exception PingCheckProcess", e);
			}
		}

	} // End of PingCheckThread
	
	private void operationTrace(String message, Throwable e) {
		if (e == null) {
			operationTraceLog.debug(message);
		} else {
			operationExceptionTraceLog.error(message, e);
		}
	}

} // End of NetDeviceOperation 
