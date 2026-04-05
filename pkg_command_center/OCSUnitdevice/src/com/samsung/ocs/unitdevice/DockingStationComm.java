package com.samsung.ocs.unitdevice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.log4j.Logger;



public class DockingStationComm {
	private String m_strDockingStationID = "";
	private String m_IPAddress = "";
	private int m_intPort = 0;
	
	private Socket m_ClientSocket = null;
	private BufferedReader m_Readbuffer = null;
	private PrintWriter m_Writebuffer = null;
	
	private ReadSocketThread m_ReadSocketThread = null;
	private CheckSocketThread m_CheckSocketThread = null;
	
	private boolean m_bActive = false;
	private String m_strSendString = "";
	private String m_strReceiveString = "";
	
	private long m_lLastReceivedTime = 0;
	private long m_lSocketFailTime = 0;
	private long m_lSocketReconnectTimeout = 20000;
	private long m_lReplyTimeOut = 2000;
	private int m_nReconnectCnt = 0;
	private long m_lSendRequestTime = 0;
	
	private String m_strCRLF = "";
	private char[] m_cBuf = null;
	final int MAX_BUFFER_SIZE = 1024;
	
	DockingStationOperation m_pOperation = null;	
	
	private static final String DOCKINSTATION_COMM_TRACE = "DockingStationCommDebug";
	private static final String DOCKINSGSTATION_COMM_EXCEPTION_TRACE = "DockingStationCommException";
	private static Logger commTraceLog = Logger.getLogger(DOCKINSTATION_COMM_TRACE);
	private static Logger commExceptionTraceLog = Logger.getLogger(DOCKINSGSTATION_COMM_EXCEPTION_TRACE);

	/**
	 * DockingStationComm의 생성자이다.
	 */
	public DockingStationComm(DockingStationOperation pOperation) {
		//초기화
		m_pOperation = pOperation;
		m_strSendString = "";
		m_lSocketFailTime = System.currentTimeMillis() - m_lSocketReconnectTimeout;
		m_lLastReceivedTime = System.currentTimeMillis();
		m_nReconnectCnt = 0;
		
		CharArrayWriter chArray = new CharArrayWriter(2);
		chArray.write(0x0D);
		chArray.write(0x0A);
		m_strCRLF = chArray.toString();
		m_cBuf = new char[MAX_BUFFER_SIZE];
	}
	
	/**
	 * Timer를 시작한다.
	 */
	public void dockingStationCommStart() {		
		if (m_CheckSocketThread == null) {
			m_CheckSocketThread = new CheckSocketThread();
			m_CheckSocketThread.start();
		}
	}
	
	public void dockingStationCommStop() {
		if (m_CheckSocketThread != null) {
			m_CheckSocketThread.bRun = false;
			m_CheckSocketThread = null;
			clientSocketClose();			
		}
	}	
	
	class ReadSocketThread extends Thread {
		String strInString = null;
		String strReceiveString = null;
		int nCount = 0;
		boolean bRun = true;
		String strLog = "";

		public void run() {
			while (bRun) {
				try {
					strReceiveString = "";
					nCount = m_Readbuffer.read(m_cBuf);

					if (nCount > 0) {
						if (nCount < MAX_BUFFER_SIZE) {
							strInString = new String(m_cBuf, 0, nCount);
						} else {
							strInString = new String(m_cBuf, 0, MAX_BUFFER_SIZE);
						}
						strReceiveString += strInString;
						strInString = null;
						
						socketRead(strReceiveString);						
						m_lLastReceivedTime = System.currentTimeMillis();
						strLog = "RCV> " + strReceiveString;
						commTrace(strLog, null);
						System.out.println(strLog);
//						receiveMessage(m_strDockingStationID, strReceiveString);
						
					}					
					else {
						strLog = "No Status Message From Docking Station";
//						System.out.println(strLog);
						commTrace(strLog, null);
						clientSocketClose();
					}
					sleep(10);
				} catch (Exception e) {
					clientSocketClose();
					commTrace("Exception ReadSocketThread", null);
					commTrace("ReadSocketThread", e);
				} 
				//finally {				}
			}
			commTrace("ReadSocketThread is dead.", null);
		}
	}	
	
	class CheckSocketThread extends Thread {
		boolean bRun = true;		
		
		public void run() {
			while (bRun) {
				try {
					socketCheckProcess();
					sleep(1000);

				} catch (Exception e) {
					clientSocketClose();
					commTrace("Exception CheckSocketThread", null);
					commTrace("ReadSocketThread", e);
				}
			}
			clientSocketClose();
			m_CheckSocketThread = null;
			commTrace("CheckSocketThread is dead.", null);
		}
	}
	
	protected boolean setStatusRequestCommand(String dockingStationID) {
		if(!m_bActive) {
			return false;
		} else {
			m_strSendString = "$R" + dockingStationID;
			return true;
		}
	}	
	
	private void socketRead(String strBuffer) {
		int nPos = 0;
		
		m_strReceiveString += strBuffer;		
		m_lLastReceivedTime = System.currentTimeMillis();
		
		while(true) {
			// "$"문자열이 없는 경우에 루프 탈출		
			nPos = m_strReceiveString.indexOf("$");
			if(nPos == -1) {
				break;
			}
			// $로 시작하는 메시지 파싱
			m_strReceiveString = m_strReceiveString.substring(nPos);		
			// message length로 정상적인 message 여부 판단 
			// 기본적으로 $r 이후 나오는 msgLength 값은 최소 14 자리이고, 가변적인 carrierID값은 64자리가 최대
			// 메시지 길이는 고정 응답 헤드 부분 2자리($r) 와 가변 메시지 길이 영역 14(14+0) ~ 78(14+64) 로 구성된다.
			// 따라서 전체 응답 메시지 길이는 16~80으로 고정이므로 msgLength 값은 메시지의 두 자리만 살피게 된다.
			// Exception 발생시, 무한 루프 탈출
			
			// 메시지 길이를 알 수 없는 경우(Ex. $, $r, $rx), 루프 탈출
			if(m_strReceiveString.length() < 4) {
				break;
			}
			nPos = Integer.parseInt(m_strReceiveString.substring(2, 4));
			// 메시지가 주어진 길이만큼 들어오지 못한 경우
			if(m_strReceiveString.length() < nPos + 4) {
				break;
			}
			// 정상 수신 메시지인 경우
			strBuffer = m_strReceiveString.substring(0, nPos+4);
			messageParsing(strBuffer);
			// operation으로 전송하고 남은 메시지 부분
			m_strReceiveString = m_strReceiveString.substring(nPos+4);	
		}
	}
	
	private void messageParsing(String strMsg) {
		String strDockingStationID = "";
		int strMsgLength = 0;
		String strMode = "";
		int strExist = 0;
		int strCharge = 0;
		int strAlarmID = 0;
		String strCarrierID = "";
		if(!m_strDockingStationID.equals(strMsg.substring(4, 10))) {			
			return;
		}
		
		if(strMsg.substring(0, 2).equals("$r")) {
			// $r17FRR501A1100000AAA
			strMsgLength = Integer.parseInt(strMsg.substring(2,4));
			if(strMsg.length() == 4 + strMsgLength) {
				// docking station id 는 6자리
				strDockingStationID = strMsg.substring(4,10);					
				strMode = strMsg.substring(10, 11);
				strExist = Integer.parseInt(strMsg.substring(11, 12));
				strCharge = Integer.parseInt(strMsg.substring(12, 13));
				strAlarmID = Integer.parseInt(strMsg.substring(13, 18));
				strCarrierID = strMsg.substring(18);										
			}
			m_pOperation.receivedStatusMsg(strMsg, strDockingStationID, strMode, strExist, strCharge, strAlarmID, strCarrierID);
		}		
	}
	/*
	private void receiveMessage(String strDockingStationID, String strMsg) {		
		
		int strMsgLength = 0;
		String strMode = "";
		int strExist = 0;
		int strCharge = 0;
		int strAlarmID = 0;
		String strCarrierID = "";
		
		try {
			if(!strDockingStationID.equals(strMsg.substring(4, 10))) {
				return;
			}
			
			if(strMsg.substring(0, 2).equals("$r")) {
				// $r17FRR501A1100000AAA
				strMsgLength = Integer.parseInt(strMsg.substring(2,4));
				if(strMsg.length() == "$r".length() + strMsg.substring(2,4).length() + strMsgLength) {
					// docking station id 는 6자리
					strDockingStationID = strMsg.substring(4,10);					
					strMode = strMsg.substring(10, 11);
					strExist = Integer.parseInt(strMsg.substring(11, 12));
					strCharge = Integer.parseInt(strMsg.substring(12, 13));
					strAlarmID = Integer.parseInt(strMsg.substring(13, 18));
					strCarrierID = strMsg.substring(18);										
				}
				m_pOperation.receivedStatusMsg(strMsg, strDockingStationID, strMode, strExist, strCharge, strAlarmID, strCarrierID);
			}
		} catch (Exception e){
			commTrace("ReceiveMessage", e);
		}		
	}
	*/
	/**
	 * Docking Station으로 Status request 메시지를 생성해서 전송한다.
	 */	
	private boolean sendMessage(String sendString) {
		//if ((m_ClientSocket == null) {
		if (m_bActive == false) {
			return false;
		}
		else {
			String strLog = "SND> " + sendString;
			System.out.println(strLog);
			
			// 2015.03.09 by zzang9un : Docking Station 프로토콜 변경으로 수정
//			m_Writebuffer.println(m_strSendString);
			m_Writebuffer.write(m_strSendString + "\r\n");
			m_Writebuffer.flush();
			commTrace(strLog, null);
			return true;
		}	
	}
	
	/**
	 * Docking Station의 정보를 설정한다.
	 */
	protected void setTargetInfo(String strDockingStationID, String strIPAddress, int intPort) {
		m_strDockingStationID = strDockingStationID;
		m_IPAddress = strIPAddress;
		m_intPort = intPort;

		String strLog = "";
		strLog = "DockingStationID :" + m_strDockingStationID + ", DockingStationIPAddress:"
		+ m_IPAddress + ", DockingStationPort:" + m_intPort;
		//commTrace(strLog, null);
	}
	
	
	/**
	 * Docking Station의 통신을 담당하는 Timer이다.
	 */
	private void socketCheckProcess() {
		String strLog = "";
		if(m_ClientSocket == null || m_ClientSocket.isClosed()) {
			clientSocketOpen();				
			m_lSocketFailTime = System.currentTimeMillis();
			m_nReconnectCnt++;
			strLog = "Connect [" + m_nReconnectCnt + "] ...";			
			commTrace(strLog, null);			
		}
		// 연결할 socket이 있고 활성화된 경우
		else{			
			if(System.currentTimeMillis() - m_lLastReceivedTime > m_lSocketReconnectTimeout) {
				strLog = "Socket Close. ReconnectTimeOut : " + m_lSocketReconnectTimeout + " ms";
//				System.out.println(strLog);
				commTrace(strLog, null);
				clientSocketClose();				
			}			
			else if(m_ClientSocket.isConnected() && m_strSendString.length() > 0) {				
				sendMessage(m_strSendString);
				m_lSendRequestTime = System.currentTimeMillis();				
			}			
		}
	}	
	/**
	 * Client Socket을 Open한다.
	 */
	private void clientSocketOpen() {
		String strLog = "";
		if(m_ClientSocket != null) {
			return;
		}
		try {
			strLog = "Client Socket Connected to Docking Station[IP: " + m_IPAddress + ", Port: " + m_intPort + "]";
			
			m_lLastReceivedTime = System.currentTimeMillis();
			
			m_ClientSocket = new Socket(m_IPAddress, m_intPort);
			m_Readbuffer = new BufferedReader(new InputStreamReader(m_ClientSocket.getInputStream()));	
			m_Writebuffer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(m_ClientSocket.getOutputStream())));
			m_ReadSocketThread = new ReadSocketThread();
			m_ReadSocketThread.start();

			m_bActive = true;
//			System.out.println(strLog);			
			commTrace(strLog, null);
			
		} catch(Exception e) {
			m_lSocketFailTime = System.currentTimeMillis();
			m_bActive = false;
			m_ClientSocket = null;			
			commTrace("Exception ClientSocketOpen", null);
			commTrace("ReadSocketThread", e);
		}		
	}
	
	/**
	 * Client Socket을 Close한다.
	 */
	private void clientSocketClose() {		
		
		if (m_ClientSocket == null) {
			return;
		}
		
		
		m_lSocketFailTime = System.currentTimeMillis();
		System.out.println(m_lSocketFailTime + " [" + m_IPAddress + "] Client Socket Is Closed : " + m_intPort);
		m_pOperation.updateCommFail();
		
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
//			System.out.println("Client Socket Disconnected");
			commTrace("Client Socket Disconnected", null);
		} catch (Exception e) {
			commTrace("Exception ClientSocketClose", null);
			commTrace("ClientSocketClose", e);
		}
	}
	
	
	private void commTrace(String message, Throwable e) {
		if (e == null) {
			commTraceLog.debug(String.format("%s> %s", m_IPAddress, message));
		} else {
			commExceptionTraceLog.error(message, e);
		}
	}	
}
