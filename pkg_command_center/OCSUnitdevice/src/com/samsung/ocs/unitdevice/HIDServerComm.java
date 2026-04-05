package com.samsung.ocs.unitdevice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * <p>Title: UnifiedOCS 1.0 for JAVA</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: SAMSUNG ELECTRONICS</p>
 * @author 조영일 책임
 * @version 1.0
 */

/**
 * HIDServerComm.java는 HID와의 통신을 담당하는 모듈로 관리하는 각각의 HID에 대한
 * Power On/Off, Status에 대한 메시지 및 세부 Status에 대한 정보를 수신하는 기능을 수행한다.
 * 상태에 대한 메시지는 일정주기마다 전송한다.
 */
public class HIDServerComm
{
	private String m_strTargetName = "";
	private String m_strTargetIPAddress = "127.0.0.1";
	private int m_nTargetPort = 2100;
	private Socket m_ClientSocket = null;
	private BufferedReader m_Readbuffer = null;
	private PrintWriter m_Writebuffer = null;
	private ReadSocketThread m_ReadSocketThread = null;
	private CheckSocketThread m_CheckSocketThread = null;

	private boolean m_bActive = false;
	private boolean m_bSendFlag = false;
	private Vector<String> m_vtSendStringList = null;
	private String m_strSendString = "";
	private String m_strReceiveString = "";
	private String m_strLastReceivedString = "";
	private long m_lLastReceivedTime = 0;
	private long m_lSocketFailTime = 0;
	private long m_lSocketReconnectTimeout = 10000;
	private long m_lReplyTimeOut = 3000;
	private int m_nReconnectCnt = 0;
	private long m_lSendRequestTime = 0;
	private int m_nTimeoutCount = 0;

	private String m_strCRLF = "";
	private char[] m_cBuf = null;

	final char STX = 0x02;
	final char ETX = 0x03;
	final int MAX_BUFFER_SIZE = 1024;

	HIDServerOperation m_pOperation = null;
	
	private static final String HID_SERVER_COMM_TRACE = "HIDServerCommDebug";
	private static final String HID_SERVER_COMM_EXCEPTION_TRACE = "HIDServerCommException";
	private static Logger commTraceLog = Logger.getLogger(HID_SERVER_COMM_TRACE);
	private static Logger commExceptionTraceLog = Logger.getLogger(HID_SERVER_COMM_EXCEPTION_TRACE);

	/**
	 * HIDServerComm의 생성자이다.
	 */
	public HIDServerComm(HIDServerOperation pOperation)
	{
		m_pOperation = pOperation;

		m_vtSendStringList = new Vector<String>();
		m_lSocketFailTime = System.currentTimeMillis()-m_lSocketReconnectTimeout;
		m_lLastReceivedTime = System.currentTimeMillis();
		m_nReconnectCnt = 0;

		CharArrayWriter chArray = new CharArrayWriter(2);
		chArray.write(0x0D);
		chArray.write(0x0A);
		m_strCRLF = chArray.toString();
		m_cBuf = new char[MAX_BUFFER_SIZE];
	}

	/**
	 * Target으로부터 수신한 메시지를 운영모듈에서 처리할 수 있도록 한다.
	 * @param strTargetName String
	 * @param strMsg String
	 *
	 * @version Created by MYM : 2009.10.20
	 */
	public void ReceiveMessage(String strTargetName, String strMsg)
	{
		String strData = "";
		String strCmd = "";
		String strHID[] = null;
		String strStatus[] = null;
		String strVoltage[] = null;
		String strCurrent[] = null;
		String strTemperature[] = null;
		String strFrequency[] = null;
		String strErrorCode[] = null;
		// 2011.02.28 by LWG [Backup HID 관련] 
		String backupHids[] = null;
		int nHIDCnt = 0;
		char cReply = ' ';
		// 2011.02.28 by LWG [Backup HID 관련]     
		boolean hasBackupHIDInfo = false;
		
		//strMsg = "";
		
		try {
			if (strMsg.substring(0, 2).equals("$t"))
			{
				// TimeOut이 나지 않았을 때(시는 T를 덧붙임.)
				if (!strMsg.equals("$tT"))
				{
					// 2010.01.07 by MYM - S1의 신형(3자리) HID와 MFAB의 HID(Can통신 Unit) 프로토콜이 다름 (자리수 위치가 다름)
					// 2010.01.07 by MYM - HID Name 3자리 호환(Mfab과 기존라인 호환하도록 함.)
					//          if(strMsg.indexOf("[") == 6)
					//          {
					//            // $t303A[001R00][002R00][003R00]
					//            nHIDCnt = Integer.parseInt(strMsg.substring(3, 5));
					//          }
					if(strMsg.indexOf("A[") == 5)
					{
						// $t303A[001R00][002R00][003R00]
						nHIDCnt = Integer.parseInt(strMsg.substring(3, 5));
					}
					else
					{
						// $t03A[001R00][002R00][003R00]
						nHIDCnt = Integer.parseInt(strMsg.substring(2, 4));
					}

					// 2010.01.07 by MYM - S1의 신형(3자리) HID와 MFAB의 HID(Can통신 Unit) 프로토콜이 다름 (자리수 위치가 다름)
					// 2010.01.07 by MYM - HID Name 3자리 호환(Mfab과 기존라인 호환하도록 함.)
					int nHIDNameSize = 2;
					//          if(strMsg.indexOf("[") == 6)
					//          {
					//            // $t303A[001R00][002R00][003R00]
					//            nHIDNameSize = Integer.parseInt(strMsg.substring(2, 3));
					//          }
					if(strMsg.indexOf("A[") == 5)
					{
						// $t303A[001R00][002R00][003R00]
						nHIDNameSize = Integer.parseInt(strMsg.substring(2, 3));
					}
					else if(strMsg.indexOf("[") == 6)
					{
						// $t03A3[001R00][002R00][003R00]
						nHIDNameSize = Integer.parseInt(strMsg.substring(5, 6));
					}
					String strTmpMsg = strMsg.substring(strMsg.indexOf("["));
					String[] strHIDInfos = strTmpMsg.split("]");
					
					int nHIDInfosCnt = strHIDInfos.length;					
					if(nHIDCnt != nHIDInfosCnt) {
						commTrace("ReceivedMessage : " + strMsg + ": Size Mismatch from HID Count / Received HID CommData" , null);
					}
					strHID = new String[nHIDInfosCnt];
					strStatus = new String[nHIDInfosCnt];
					strErrorCode = new String[nHIDInfosCnt];
					backupHids = new String[nHIDInfosCnt];
					
					for (int i = 0; i < nHIDInfosCnt; i++)
					{
						// [001R00
						strHID[i] = strHIDInfos[i].substring(1, nHIDNameSize + 1);
						strHIDInfos[i] = strHIDInfos[i].substring(nHIDNameSize + 1);
						// R00
						// 2013.04.17 by MYM : 메시지 사이즈가 기준보다 작으면 Parsing 하지 않도록 처리
						// 배경 : 온양에서 Invalid한 메시지를 올려주는 경우가 있어 Parsing시 Exception 발생함.
						//       ex) [011?0]
						if (strHIDInfos[i].length() >= 3) {
							strStatus[i] = strHIDInfos[i].substring(0, 1);
							strErrorCode[i] = strHIDInfos[i].substring(1, 3);
							
							// 2011.02.28 by LWG [Backup HID 관련] 5 0 1O 0 0 0 0 1
							if( strHIDInfos[i].length() == 3 + nHIDNameSize ) {
								backupHids[i] = strHIDInfos[i].substring(3, 3 + nHIDNameSize);
								hasBackupHIDInfo = true;
							}
						}
					}
					// 2011.02.28 by LWG [Backup HID 관련] : backup hid를 가지고 잇으면 그 정보까지 저장하는 메서드로 실행한다.
					if (hasBackupHIDInfo) {
						m_pOperation.ReceivedStatusMsg(strTargetName, nHIDInfosCnt, strHID, strStatus, strErrorCode, backupHids);
					} else {
						// 2012.11.07 by MYM : BackupHid 업데이트 안되는 문제 수정
						m_pOperation.ReceivedStatusMsg(strTargetName, nHIDInfosCnt, strHID, strStatus, strErrorCode, null);
//						m_pOperation.ReceivedStatusMsg(strTargetName, nHIDCnt, strHID, strStatus, strErrorCode);
					}					
				}

				if (m_vtSendStringList.size() > 0)
				{
					strData = (String) m_vtSendStringList.get(0);
					if (strData.substring(1, 2).equals(strMsg.substring(1, 2).toUpperCase()))
					{
						m_vtSendStringList.removeElementAt(0);
						m_bSendFlag = false;
					}
				}
				else
				{
					m_bSendFlag = false;
				}
			}
			else if (strMsg.substring(0, 2).equals("$s"))
			{
				if (!strMsg.equals("$sT"))
				{
					// 2010.01.07 by MYM - S1의 신형(3자리) HID와 MFAB의 HID(Can통신 Unit) 프로토콜이 다름 (자리수 위치가 다름)
					// 2010.01.07 by MYM - HID Name 3자리 호환(Mfab과 기존라인 호환하도록 함.)
					//          if(strMsg.indexOf("[") == 6)
					//          {
					//            // $s303A[001R399781199200][002R398791199200][003R400771199200]
					//            nHIDCnt = Integer.parseInt(strMsg.substring(3, 5));
					//          }
					if(strMsg.indexOf("A[") == 5)
					{
						// $s303A[001R399781199200][002R398791199200][003R400771199200]
						nHIDCnt = Integer.parseInt(strMsg.substring(3, 5));
					}
					else
					{
						// $s03A[001R399781199200][002R398791199200][003R400771199200]
						nHIDCnt = Integer.parseInt(strMsg.substring(2, 4));
					}

					// 2010.01.07 by MYM - S1의 신형(3자리) HID와 MFAB의 HID(Can통신 Unit) 프로토콜이 다름 (자리수 위치가 다름)
					// 2010.01.07 by MYM - HID Name 3자리 호환(Mfab과 기존라인 호환하도록 함.)
					int nHIDNameSize = 2;
					//          if(strMsg.indexOf("[") == 6)
					//          {
					//            // $s303A[001R399781199200][002R398791199200][003R400771199200]
					//            nHIDNameSize = Integer.parseInt(strMsg.substring(2, 3));
					//          }
					if(strMsg.indexOf("A[") == 5)
					{
						// $s303A[001R399781199200][002R398791199200][003R400771199200]
						nHIDNameSize = Integer.parseInt(strMsg.substring(2, 3));
					}
					else if(strMsg.indexOf("[") == 6)
					{
						// $s03A3[001R399781199200][002R398791199200][003R400771199200]
						nHIDNameSize = Integer.parseInt(strMsg.substring(5, 6));
					}

					String strTmpMsg = strMsg.substring(strMsg.indexOf("["));
					String[] strHIDInfos = strTmpMsg.split("]");
					
					int nHIDInfosCnt = strHIDInfos.length;
					if(nHIDCnt != nHIDInfosCnt) {
						commTrace("ReceivedMessage : " + strMsg + ": Size Mismatch from HID Count / Received HID CommData" , null);
					}
					strHID = new String[nHIDInfosCnt];
					strStatus = new String[nHIDInfosCnt];
					strVoltage = new String[nHIDInfosCnt];
					strCurrent = new String[nHIDInfosCnt];
					strTemperature = new String[nHIDInfosCnt];
					strFrequency = new String[nHIDInfosCnt];
					strErrorCode = new String[nHIDInfosCnt];
					// 2011.02.28 by LWG [Backup HID 관련]
					backupHids = new String[nHIDCnt];
					for (int i = 0; i < nHIDInfosCnt; i++)
					{
						// [001R399781199200
						strHID[i] = strHIDInfos[i].substring(1, nHIDNameSize + 1);
						strHIDInfos[i] = strHIDInfos[i].substring(nHIDNameSize + 1);

						// R399781199200
						// 2013.04.17 by MYM : 메시지 사이즈가 기준보다 작으면 Parsing 하지 않도록 처리
						// 배경 : 온양에서 Invalid한 메시지를 올려주는 경우가 있어 Parsing시 Exception 발생함.
						//       ex) [016?59770099200]
						if (strHIDInfos[i].length() >= 13) {
							strStatus[i] = strHIDInfos[i].substring(0, 1);
							strVoltage[i] = strHIDInfos[i].substring(1, 4);
							strCurrent[i] = strHIDInfos[i].substring(4, 6);
							strTemperature[i] = strHIDInfos[i].substring(6, 8);
							strFrequency[i] = strHIDInfos[i].substring(8, 11);
							strErrorCode[i] = strHIDInfos[i].substring(11, 13);

							// 2011.02.28 by LWG [Backup HID 관련] 파싱
							if( strHIDInfos[i].length() == 13+nHIDNameSize ) {
								backupHids[i] = strHIDInfos[i].substring(13, 13+nHIDNameSize);
								hasBackupHIDInfo = true;
							}
						}
					}

					// 2011.02.28 by LWG [Backup HID 관련] : backup hid를 가지고 잇으면 그 정보까지 저장하는 메서드로 실행한다.
					if (hasBackupHIDInfo) {
						m_pOperation.ReceivedStatusMsg(strTargetName, nHIDInfosCnt, strHID,
								strStatus, strVoltage, strCurrent, strTemperature,
								strFrequency, strErrorCode, backupHids);
					} else {
						// 2012.11.07 by MYM : BackupHid 업데이트 안되는 문제 수정
//						m_pOperation.ReceivedStatusMsg(strTargetName, nHIDCnt, strHID,
//								strStatus, strVoltage, strCurrent, strTemperature,
//								strFrequency, strErrorCode);
						m_pOperation.ReceivedStatusMsg(strTargetName, nHIDInfosCnt, strHID,
								strStatus, strVoltage, strCurrent, strTemperature,
								strFrequency, strErrorCode, null);
					}
				}

				if (m_vtSendStringList.size() > 0)
				{
					strData = (String) m_vtSendStringList.get(0);
					if (strData.substring(1, 2).equals(strMsg.substring(1, 2).toUpperCase()))
					{
						m_vtSendStringList.removeElementAt(0);
						m_bSendFlag = false;
					}
				}
				else
				{
					m_bSendFlag = false;
				}
			}
			else
			{
				strHID = new String[1];
				strCmd = strMsg.substring(1, 2);
				// 2012.06.07 by MYM : 응답에 대해서
				if (strMsg.length() >= 5) {
					if (m_pOperation.getHIDCipher() == 3) {
						strHID[0] = strMsg.substring(2, 5);
						cReply = strMsg.charAt(5);
					} else {
						strHID[0] = strMsg.substring(2, 4);
						cReply = strMsg.charAt(4);
					}
					m_pOperation.ReceivedCommandReplyMsg(strTargetName, strHID[0], strCmd, cReply);
				} else {
					cReply = 'A';
				}

				if (m_vtSendStringList.size() > 0)
				{
					strData = (String) m_vtSendStringList.get(0);
					if (strData.substring(1, 2).equals(strMsg.substring(1, 2).toUpperCase()))
					{
						// Timeout은 Retry가 이뤄질 수 있도록 미삭제
						if (cReply != 'T')
						{
//							m_vtSendStringList.removeElementAt(0);
							m_vtSendStringList.remove(strData);
						}
						// 이전에 처리했으나 Timeout에 의해서 여러개 전송되었던 Reply가 아닌 경우에만 Reset
						m_bSendFlag = false;
					}
				}
				else
				{
					m_bSendFlag = false;
				}
			}
		} catch (Exception e) {
			commTrace("ReceiveMessage", e);
		}
	}


	/**
	 * HID Server로 Pause Command 메시지를 생성해서 전송한다.
	 */
	public boolean SendPauseCommand(String strHID)
	{
		if (m_bActive == false)
		{
			return false;
		}

		String strSendString = "";
		strSendString = "$P" + GetHIDString(strHID);

		return RegisterSendData(strSendString);
	}

	/**
	 * HID Server로 Resume Command 메시지를 생성해서 전송한다.
	 */
	public boolean SendResumeCommand(String strHID)
	{
		if (m_bActive == false)
		{
			return false;
		}

		String strSendString = "";
		strSendString = "$R" + GetHIDString(strHID);

		return RegisterSendData(strSendString);
	}

	/**
	 * HID Server로 Status Command 메시지를 생성해서 전송한다.
	 */
	public boolean SendStatusCommand(String strHID)
	{
		if (m_bActive == false)
		{
			return false;
		}

		String strSendString = "";
		strSendString = "$T" + GetHIDString(strHID);

		return RegisterSendData(strSendString);
	}

	/**
	 * HID Server로 DetailStatus Command 메시지를 생성해서 전송한다.
	 */
	public boolean SendDetailStatusCommand(String strHID)
	{
		if (m_bActive == false)
		{
			return false;
		}

		String strSendString = "";
		// 2013.05.08 by YB.MIN : HID date 전송 부분 추가
		strSendString = "$S" + GetHIDString(strHID); // + new SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis());

		return RegisterSendData(strSendString);
	}

	/**
	 * HID의 자리수를 맞춰서 String을 생성한다.
	 */
	public String GetHIDString(String strHID)
	{
		if(m_pOperation.getHIDCipher() == 3)
		{
			// 2009.10.20 by MYM : 3자리 호환
			if (strHID.length() == 2)
			{
				return ("0" + strHID);
			}
			else if (strHID.length() == 1)
			{
				return ("00" + strHID);
			}
			else if (strHID.length() == 0)
			{
				return ("000");
			}
		}
		else
		{
			if (strHID.length() == 1)
			{
				return ("0" + strHID);
			}
			else if (strHID.length() == 0)
			{
				return ("00" + strHID);
			}
		}

		return strHID;
	}

	/**
	 * Timeout이 발생한 경우에 Timeout에 해당되는 String을 생성한다.
	 */
	public String MakeTimeoutString(String strSendMsg)
	{
		return strSendMsg.substring(0, 2).toLowerCase() + "T";
	}

	/**
	 * HID의 통신을 담당하는 Timer이다.
	 */
	void SocketCheckProcess()
	{
		String strCmdTimeoutMsg = "";
		String strLog = "";

		if (m_ClientSocket == null || m_bActive == false)
		{
			if (System.currentTimeMillis() - m_lSocketFailTime > m_lSocketReconnectTimeout)
			{
				ClientSocketOpen();
				m_bSendFlag = false;
				m_lSocketFailTime = System.currentTimeMillis();
				m_nReconnectCnt++;
				strLog = "Connect[" + m_nReconnectCnt + "]...";
				commTrace(strLog, null);
			}
		}
		else
		{
			if (m_vtSendStringList.size() > 0)
			{
				if (m_bSendFlag == false)
				{
					m_strSendString = (String) m_vtSendStringList.get(0);
					SendMessage(m_strSendString);
					m_lSendRequestTime = System.currentTimeMillis();
					m_bSendFlag = true;
				}
				else if (System.currentTimeMillis() - m_lSendRequestTime > m_lReplyTimeOut)
				{
					// Timeout을 판단할 수 있는 String을 만들어 수신메시지 처리하는 함수를 호출한다.
					strCmdTimeoutMsg = MakeTimeoutString(m_strSendString);
					strLog = "RCV> " + strCmdTimeoutMsg + " Timeout";
					commTrace(strLog, null);
					ReceiveMessage(m_strTargetName, strCmdTimeoutMsg);

					m_lSendRequestTime = System.currentTimeMillis();
					if (m_nTimeoutCount++ > 20)
					{
						m_nTimeoutCount = 0;
						ClientSocketClose();
					}
				}
			}
			else
			{
				// 2012.06.12 by MYM : HIDServerOperation에서 하도록 이동
//				if (m_nStatusSendCount % 5 != 0)
//				{
//					// 2010.01.07 by MYM : 기존라인의 경우 "00"로 요청
//					//                     MFAB Can통신 유닛은 "000"로 요청한다.
//					if(m_pOperation.getHIDCipher() == 2)
//						SendStatusCommand("00");
//					else if(m_pOperation.getHIDCipher() == 3)
//						SendStatusCommand("000");
//				}
//				else
//				{
//					// 2010.01.07 by MYM : 기존라인의 경우 "00"로 요청
//					//                     MFAB Can통신 유닛은 "000"로 요청한다.
//					if(m_pOperation.getHIDCipher() == 2)
//						SendDetailStatusCommand("00");
//					else if(m_pOperation.getHIDCipher() == 3)
//						SendDetailStatusCommand("000");
//
//					m_nStatusSendCount = 0;
//				}
//				m_nStatusSendCount++;
			}

			if (System.currentTimeMillis()-m_lLastReceivedTime > m_lSocketReconnectTimeout)
			{
				ClientSocketClose();
				m_lLastReceivedTime = System.currentTimeMillis();
				m_lSocketFailTime = System.currentTimeMillis()-m_lSocketReconnectTimeout;
			}
		}
	}

	/**
	 * 통신에 대한 이력을 저장한다.
	 */
	public void commTrace(String message, Throwable e) {
		if (e == null) {
			commTraceLog.debug(String.format("%s> %s", m_strTargetIPAddress, message));
		} else {
			commExceptionTraceLog.error(message, e);
		}
	}
	
	/**
	 * Socket의 수신 Buffer에서 정보를 얻기위한 Class이다.
	 */
	class ReadSocketThread extends Thread {
		String strInString = null;
		String strReceiveString = null;
		int nCount = 0;
		boolean bRun = true;

		public void run() {
			while (bRun) {
				try {
					strReceiveString = "";
					nCount = m_Readbuffer.read(m_cBuf);

					if (nCount >= 0) {
						if (nCount < MAX_BUFFER_SIZE) {
							strInString = new String(m_cBuf, 0, nCount);
						} else {
							strInString = new String(m_cBuf, 0, MAX_BUFFER_SIZE);
						}
						strReceiveString += strInString;
						strInString = null;

						SocketRead(strReceiveString);
					}
					// SJLEE - 통신 장애시 과부하 개선
					sleep(10);
				} catch (Exception e) {
					ClientSocketClose();
					commTrace("Exception ReadSocketThread", null);
					commTrace("ReadSocketThread", e);
				}
			}
			commTrace("ReadSocketThread is dead.", null);
		}
	}

	class CheckSocketThread extends Thread {
		boolean bRun = true;

		public void run() {
			while (bRun) {
				try {
					SocketCheckProcess();

					// 2009.10.20 by MYM : 1000 -> 500ms 수정
					sleep(500);

				} catch (Exception e) {
					ClientSocketClose();
					commTrace("Exception CheckSocketThread", null);
					commTrace("ReadSocketThread", e);
				}
			}
			// 2012.06.07 by MYM : Thread 종료시 Socket Close 하도록
			// HIDServerCommStop()에서 여기로 옮김.
			ClientSocketClose();
			m_CheckSocketThread = null;
			commTrace("CheckSocketThread is dead.", null);
		}
	}

	/**
	 * Socket의 송신 Buffer에 정보를 기록한다.
	 */
	public boolean SendMessage(String strSendMsg)
	{
		if (m_ClientSocket == null)
		{
			ClientSocketClose();
			return false;
		}
		else
		{
			String sSndStr = "SND> " + strSendMsg;
			commTrace(sSndStr, null);
			// 2007.08.01 메시지에 CRLF문자열 추가
			//m_Writebuffer.println(strSendMsg);
			m_Writebuffer.println(strSendMsg + m_strCRLF);
			m_Writebuffer.flush();
			return true;
		}
	}

	/**
	 * Name의 정보를 얻는다.
	 */
	public String GetTargetName()
	{
		return m_strTargetName;
	}

	/**
	 * IP Address의 정보를 얻는다.
	 */
	public String GetTargetIPAddress()
	{
		return m_strTargetIPAddress;
	}

	/**
	 * Target의 정보를 설정한다.
	 */
	public void SetTargetInfo(String strTargetName,
			String strTargetIPAddress, int nTargetPort)
	{
		m_strTargetName = strTargetName;
		m_strTargetIPAddress = strTargetIPAddress;
		m_nTargetPort = nTargetPort;

		String strLog = "";
		strLog = "TargetName:" + m_strTargetName + ", TargetIPAddress:" + m_strTargetIPAddress + ", TargetPort:"
				+ m_nTargetPort;
		commTrace(strLog, null);
	}

	/**
	 * Client Socket을 Open한다.
	 */
	public void ClientSocketOpen()
	{
		if (m_ClientSocket != null) {
			return;
		}

		try {
			System.out.println(System.currentTimeMillis() + " [" + m_strTargetIPAddress + "] ClientSocketOpen : " + m_nTargetPort);
			m_lLastReceivedTime = System.currentTimeMillis();

			m_ClientSocket = new Socket(m_strTargetIPAddress, m_nTargetPort);
			m_Readbuffer = new BufferedReader(new InputStreamReader(m_ClientSocket.getInputStream()));
			m_Writebuffer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_ClientSocket.getOutputStream())));

			m_ReadSocketThread = new ReadSocketThread();
			m_ReadSocketThread.start();

			m_bActive = true;
			
			// 2011.09.06 by KYK : if Reconnect , strBuffer Reset
			// (갑작스런)Network Fail 시 , 이전 미완료 packet 리셋되지 않은 상태에서 남아있음, 
			// 다음 메시지가 연결되어 msg parsing 시 문제발생할 수 있음 (S1L Operation 2011.09.02)
			m_strReceiveString = "";

			commTrace("Client Socket Connected...", null);
		} catch (Exception e) {
			m_lSocketFailTime = System.currentTimeMillis();
			m_bActive = false;
			commTrace("Exception ClientSocketOpen", null);
			commTrace("ReadSocketThread", e);
		}
	}

	/**
	 * Client Socket을 Close한다.
	 */
	public void ClientSocketClose()
	{
		if (m_ClientSocket == null) {
			return;
		}

		System.out.println(System.currentTimeMillis() + " [" + m_strTargetIPAddress + "] ClientSocketClose : " + m_nTargetPort);

		// 2009.03.11 by MYM : HID 마스터의 통신상태를 DB에서 조회할 수 있도록 ErrorCode값을 1000으로 변경
//		m_pOperation.HIDCommunicationFail();
		m_pOperation.updateCommFail();

		m_lSocketFailTime = System.currentTimeMillis();
		m_bActive = false;
		try {
			if (m_ReadSocketThread != null) {
				m_ReadSocketThread.bRun = false;
			}
			
			m_Writebuffer.close();
			m_Writebuffer = null;
			
			if (m_Readbuffer != null) {
				try {
					m_Readbuffer.close();
				} catch (IOException e) {
					commTrace("ClientSocketClose", e);
				}
				m_Readbuffer = null;
			}
			
			if (m_ClientSocket != null) {
				try {
					m_ClientSocket.close();
				} catch (IOException e) {
					commTrace("ClientSocketClose", e);
				}
				m_ClientSocket = null;
			}
			
			commTrace("Client Socket Disconnected", null);
		} catch (Exception e) {
			commTrace("Exception ClientSocketClose", null);
			commTrace("ClientSocketClose", e);
		}
	}

	/**
	 * 전송할 명령을 등록한다.
	 */
	public boolean RegisterSendData(String strSendData)
	{
		if (m_vtSendStringList.contains(strSendData)) {
			return false;
		}

		m_vtSendStringList.add(strSendData);

		return true;
	}

	/**
	 * Target으로부터 수신된 Reply나 Status정보에 대해서 운영모듈에서 처리할 수 있도록 한다.
	 */
	public void SocketRead(String strBuffer)
	{
		int nPos = 0;
		String strLog = "";
		int i;

		m_strReceiveString += strBuffer;

		Vector<String> vtReceiveList = new Vector<String>();

		// SJLEE - 통신두절 감지용 시간 설정
		m_lLastReceivedTime = System.currentTimeMillis();
		m_nTimeoutCount = 0;

		// 수신된 메시지를 모두 처리...
		while (true)
		{
			// "$"문자열 이후에 "CRLF"문자열을 포함한 정상적인 문자열이 아닌 경우에 루프 탈출
			nPos = m_strReceiveString.indexOf("$");
			if (nPos == -1)
			{
				break;
			}
			m_strReceiveString = m_strReceiveString.substring(nPos);
			nPos = m_strReceiveString.indexOf(m_strCRLF);
			if (nPos == -1)
			{
				break;
			}
			
			// 정상적으로 수신된 메시지가 있는 경우에 메시지 처리
			strBuffer = m_strReceiveString.substring(0, nPos);
			m_strReceiveString = m_strReceiveString.substring(nPos + 2);

			// SJLEE - 복수개의 메시지가 수신된 경우에 대한 대응
			// 명령에 대한 응답이 있는 경우에는 이전의 상태 보고는 모두 삭제
			if (strBuffer.indexOf("$s") == -1)
				vtReceiveList.clear();
			else
			{
				// 상태 확인 경우에는 이전의 상태 보고는 모두 삭제
				for (i = vtReceiveList.size()-1; i >= 0; i--)
				{
					if (((String)vtReceiveList.get(i)).indexOf("$s") >= 0)
					{
						vtReceiveList.remove(i);
					}
				}
			}
			vtReceiveList.add(strBuffer);
		}

		for (i = 0; i < vtReceiveList.size(); i++)
		{
			strBuffer = (String)vtReceiveList.get(i);
			// 중복되지 않은 메시지나 일정시간이 경과된 경우에만 처리
			if (m_strLastReceivedString.equals(strBuffer) == false)
			{
				strLog = "RCV> " + strBuffer;
				commTrace(strLog, null);
				try
				{
//					strBuffer = "$t315A[001T00000][002R00000][003S00000][004S00000][005R00000][006T00000][007T00000][008T00000][009T00000][010T00000][011T00000][012T00000][013T00000][014T00000][015T00000]";
//					strBuffer = "$p001A";
					ReceiveMessage(m_strTargetName, strBuffer);
				}
				catch (Exception e)
				{
					commTrace("Exception SocketRead", null);
					commTrace("ClientSocketClose", e);
				}
			}
		}
	}

	/**
	 * Timer를 시작한다.
	 */
	public void HIDServerCommStart()
	{
		if (m_CheckSocketThread == null)
		{
			m_vtSendStringList.clear();
			m_strReceiveString = "";
			m_bSendFlag = false;

			m_CheckSocketThread = new CheckSocketThread();
			m_CheckSocketThread.start();
		}
	}

	/**
	 * Timer를 종료한다.
	 */
	public void HIDServerCommStop()
	{
		if (m_CheckSocketThread != null)
		{
			m_CheckSocketThread.bRun = false;
			// 2012.06.07 by MYM : 쓰레드를 정지를 시키고 정지된 후 SocketClose 호출하도록 변경
//			ClientSocketClose();
//			m_CheckSocketThread = null;
		}
	}
}
