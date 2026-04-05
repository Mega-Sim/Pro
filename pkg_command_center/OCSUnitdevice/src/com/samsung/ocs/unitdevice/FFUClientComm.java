package com.samsung.ocs.unitdevice;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * FFU(Server)와 통신을 담당하는 Client Class
 * @author LSH
 * 
 */
public class FFUClientComm {
	private String strFFUServerId = ""; // FFU ID
	private String strIPAddress = "127.0.0.1";
	private int port = 7001;
	private Socket clientSocket = null;
	private BufferedReader readBuffer = null;
	
	private ReadSocketThread readSocketThread = null;
	private CheckSocketThread checkSocketThread = null;

	private boolean bActive = false;

	private String strReceiveString = "";
	private String strLastReceivedString = "";
	
	private long lLastReceivedTime = 0;
	private long lLastLogTime = 0;
	private long lLastTimeoutCheckTime = 0; // 마지막 Timeout 시간
	private long lSocketFailTime = 0;
	private long lSocketReconnectTimeout = 10000;
	private long lReceivedTimeOut = 3000;
	private int reconnectCount = 0;
	private int timeoutCount = 0;

	private String strCRLF = "";
	private char[] cBuf = null;

	final char STX = 0x02;
	final char ETX = 0x03;
	final int MAX_BUFFER_SIZE = 1024;

	FFUOperation ffuOperation = null;

	private static final String FFU_CLIENT_COMM_TRACE = "FFUCommDebug";
	private static final String FFU_CLIENT_COMM_EXCEPTION_TRACE = "FFUCommException";
	private static Logger commTraceLog = Logger.getLogger(FFU_CLIENT_COMM_TRACE);
	private static Logger commExceptionTraceLog = Logger.getLogger(FFU_CLIENT_COMM_EXCEPTION_TRACE);

	/**
	 * FFUServerComm 생성자이다.
	 */
	public FFUClientComm(FFUOperation operation) {
		ffuOperation = operation;

		lSocketFailTime = System.currentTimeMillis() - lSocketReconnectTimeout;
		lLastReceivedTime = System.currentTimeMillis();
		reconnectCount = 0;
		timeoutCount = 0;

		CharArrayWriter chArray = new CharArrayWriter(2);
		chArray.write(0x0D);
		chArray.write(0x0A);
		strCRLF = chArray.toString();
		cBuf = new char[MAX_BUFFER_SIZE];
	}
	
	class ReadSocketThread extends Thread {
		String strInString = null;
		String strReceiveString = null;
		int nCount = 0;
		boolean bRun = true;

		public void run() {
			while (bRun) {
				try {
					strReceiveString = "";
					nCount = readBuffer.read(cBuf);

					if (nCount >= 0) {
						if (nCount < MAX_BUFFER_SIZE) {
							strInString = new String(cBuf, 0, nCount);
						} else {
							strInString = new String(cBuf, 0, MAX_BUFFER_SIZE);
						}
						strReceiveString += strInString;
						strInString = null;

						SocketRead(strReceiveString);
					}
					// SJLEE - 통신 장애시 과부하 개선
					sleep(10);
				} catch (Exception e) {
					clientSocketClose();
					commTrace("\tException ReadSocketThread", null);
					commTrace("\tReadSocketThread", e);
				}
			}
			commTrace("\tReadSocketThread is stopped.", null);
		}
	}
	
	/**
	 * Socket을 체크하기 위한 Class(Thread)
	 * @author zzang9un
	 */
	class CheckSocketThread extends Thread {
		boolean bRun = true;

		public void run() {
			while (bRun) {
				try {
					SocketCheckProcess();

					// 2009.10.20 by MYM : 1000 -> 500ms 수정
					sleep(500);

				} catch (Exception e) {
					clientSocketClose();
					commTrace("\tException CheckSocketThread", null);
					commTrace("\tReadSocketThread", e);
				}
			}
			// 2012.06.07 by MYM : Thread 종료시 Socket Close 하도록
			clientSocketClose();
			checkSocketThread = null;
			commTrace("\tCheckSocketThread is stopped.", null);
		}
	}

	// 프로토콜 정보
	private static String STR_FFU_MESSAGE = "101";
	
	public void parsingMessage(String strTargetName, String strMsg) {
		int nFFUGroupCnt = 0;
		String strFFUGroupId[] = null;
		String strStatus[] = null;
		int nTotalFFU[] = null;
		int nAbnormalFFU[] = null;
		int nCommfailFFU[] = null;
		
		String [] strTmpMsg = null;

		try {
			strTmpMsg = strMsg.split("/");
			
			if (strTmpMsg[0].equals(STR_FFU_MESSAGE)) {
				nFFUGroupCnt = Integer.parseInt(strTmpMsg[1]);

				int nFFUGroupInfosCnt = strTmpMsg.length - 2;
				if(nFFUGroupCnt != nFFUGroupInfosCnt) {
					commTrace("ReceivedMessage : " + strMsg + ": Size Mismatch from FFUGroup Count / Received FFUGroup CommData" , null);
				}

				strFFUGroupId = new String[nFFUGroupInfosCnt];
				strStatus = new String[nFFUGroupInfosCnt];
				nTotalFFU = new int[nFFUGroupInfosCnt];
				nAbnormalFFU = new int [nFFUGroupInfosCnt];
				nCommfailFFU = new int [nFFUGroupInfosCnt];
				
				for (int i = 0; i < nFFUGroupInfosCnt; i++)
				{
					String [] strFFUGroupInfos = null;
					strFFUGroupInfos = strTmpMsg[i+2].split(",");
					
					strFFUGroupId[i] = strFFUGroupInfos[0];
					
					strStatus[i] = strFFUGroupInfos[1];

					nTotalFFU[i] = Integer.parseInt(strFFUGroupInfos[2]);

					nAbnormalFFU[i] = Integer.parseInt(strFFUGroupInfos[3]);
					
					nCommfailFFU[i] = Integer.parseInt(strFFUGroupInfos[4]);
				}

				// Parsing한 메세지를 operation으로 넘겨줌
				ffuOperation.ReceivedStatusMsg(strTargetName, nFFUGroupInfosCnt, strFFUGroupId, strStatus, nTotalFFU, nAbnormalFFU, nCommfailFFU);
			}
		} catch (Exception e) {
			// String strLog = "ReceiveMessage - IOException: " +
			// e.getMessage();
			// commTrace("ReceiveMessage()", strLog);
			commTrace("ReceiveMessage", e);
		}
	}

	void SocketCheckProcess() {
		String strLog = "";

		if (clientSocket == null || bActive == false) {
			if (System.currentTimeMillis() - lSocketFailTime > lSocketReconnectTimeout) {
				clientSocketOpen();
				lSocketFailTime = System.currentTimeMillis();
				reconnectCount++;
				strLog = "Connect[" + reconnectCount + "]";
				commTrace(strLog, null);
			}
		} else {
			if (System.currentTimeMillis() - lLastTimeoutCheckTime > lReceivedTimeOut) {
				// 정해진 time 동안 메세지를 받지 못한 경우
				timeoutCount++;
				strLog = "Timeout[" + timeoutCount + "]";
				commTrace(strLog, null);	
				lLastTimeoutCheckTime = System.currentTimeMillis();
			} 
			
			if (System.currentTimeMillis() - lLastReceivedTime > lSocketReconnectTimeout) {
				clientSocketClose();
				lLastReceivedTime = System.currentTimeMillis();
				lLastTimeoutCheckTime = System.currentTimeMillis();
				lSocketFailTime = System.currentTimeMillis() - lSocketReconnectTimeout;
			}
		}
	}

	/**
	 * 통신에 대한 이력을 저장한다.
	 */
	public void commTrace(String message, Throwable e) {
		if (e == null) {
			commTraceLog.debug(String.format("%s> %s", strIPAddress, message));
		} else {
			commExceptionTraceLog.error(message, e);
		}
	}

	/**
	 * FFU Group의 ID 정보를 얻는다.
	 */
	public String getFFUServerId() {
		return strFFUServerId;
	}

	/**
	 * IP Address의 정보를 얻는다.
	 */
	public String getIPAddress() {
		return strIPAddress;
	}

	/**
	 * Target의 정보를 설정한다.
	 */
	public void SetTargetInfo(String strTargetName, String strTargetIPAddress, int nTargetPort) {
		strFFUServerId = strTargetName;
		strIPAddress = strTargetIPAddress;
		port = nTargetPort;

		String strLog = "";
		strLog = "FFUServerID:" + strTargetName + ", IPAddress:" + strTargetIPAddress + ", Port:" + port;
		commTrace(strLog, null);
	}

	public void clientSocketOpen() {
		if (clientSocket != null) {
			return;
		}

		try {
			System.out.println(System.currentTimeMillis() + " [" + strIPAddress + "] ClientSocketOpen : " + port);
			lLastReceivedTime = System.currentTimeMillis();

			clientSocket = new Socket(strIPAddress, port);
			readBuffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			readSocketThread = new ReadSocketThread();
			readSocketThread.start();

			bActive = true;

			// 2011.09.06 by KYK : if Reconnect , strBuffer Reset
			// (갑작스런)Network Fail 시 , 이전 미완료 packet 리셋되지 않은 상태에서 남아있음,
			// 다음 메시지가 연결되어 msg parsing 시 문제발생할 수 있음 (S1L Operation 2011.09.02)
			strReceiveString = "";

			commTrace("Client Socket Connected...", null);
		}
		catch (Exception e) {
			// 16.09.06 LSH: Commfail 상태로 초기화 하지 않고, 실제 Commfail 발생 시 처리하도록 수정
			ffuOperation.UpdateCommFail();
			lSocketFailTime = System.currentTimeMillis();
			bActive = false;
			commTrace("\tException ClientSocketOpen", null);
			commTrace("\tReadSocketThread", e);
		}
	}

	/**
	 * Client Socket을 Close한다.
	 */
	public void clientSocketClose() {
		if (clientSocket == null) {
			return;
		}

		System.out.println(System.currentTimeMillis() + " [" + strIPAddress + "] ClientSocketClose : " + port);

		// Socket을 close하기 전 해당 Operation을 CommFailList에 추가한다.
		ffuOperation.UpdateCommFail();

		lSocketFailTime = System.currentTimeMillis();
		bActive = false;
		try {
			if (readSocketThread != null) {
				readSocketThread.bRun = false;
				readSocketThread = null;
			}

//			if (readBuffer != null) {
//				try {
//					readBuffer.close();
//				} catch (IOException e) {
//					commTrace("ClientSocketClose", e);
//				}
//				readBuffer = null;
//			}

			if (clientSocket != null) {
				try {
					clientSocket.close();
				} catch (IOException e) {
					commTrace("ClientSocketClose", e);
				}
				clientSocket = null;
			}

			commTrace("Client Socket Disconnected", null);
		} catch (Exception e) {
			commTrace("Exception ClientSocketClose", null);
			commTrace("ClientSocketClose", e);
		} finally {
			if (readBuffer != null) {
				try {
					readBuffer.close();
				} catch (IOException e) {
					commTrace("ClientSocketClose", e);
				}
				readBuffer = null;
			}
			
			reconnectCount = 0;
			timeoutCount = 0;
		}
	}

	public void SocketRead(String strBuffer) {
		int nPos = 0;
		String strLog = "";
		int i;

		strReceiveString += strBuffer;

		Vector<String> vtReceiveList = new Vector<String>();

		// 수신된 메시지를 모두 처리...
		while (true) {
			// "101"문자열 이후에 "CRLF"문자열을 포함한 정상적인 문자열이 아닌 경우에 루프 탈출
			
			nPos = strReceiveString.indexOf(STR_FFU_MESSAGE);
			if (nPos == -1) {
				break;
			}
			
			strReceiveString = strReceiveString.substring(nPos);
			nPos = strReceiveString.indexOf(strCRLF);
			if (nPos == -1) {
				break;
			}

			// 정상적으로 수신된 메시지가 있는 경우에 메시지 처리
			strBuffer = strReceiveString.substring(0, nPos);
			strReceiveString = strReceiveString.substring(nPos + 2);

			// SJLEE - 복수개의 메시지가 수신된 경우에 대한 대응
			// 명령에 대한 응답이 있는 경우에는 이전의 상태 보고는 모두 삭제
			if (strBuffer.indexOf(STR_FFU_MESSAGE) == -1)
				vtReceiveList.clear();
			else {
				// 상태 확인 경우에는 이전의 상태 보고는 모두 삭제
				for (i = vtReceiveList.size() - 1; i >= 0; i--) {
					if (((String) vtReceiveList.get(i)).indexOf(STR_FFU_MESSAGE) >= 0) {
						vtReceiveList.remove(i);
					}
				}
			}
			vtReceiveList.add(strBuffer);
		}

		for (i = 0; i < vtReceiveList.size(); i++) {
			strBuffer = (String) vtReceiveList.get(i);
			
			try {
				// 받은 메세지(문자열)을 Parsing하기 위해 함수 호출
				parsingMessage(strFFUServerId, strBuffer);
				
				// 중복되지 않은 메시지, 중복되더라도 일정 시간이 지난 로그는 기록
				if ((strLastReceivedString.equals(strBuffer) == false) || (System.currentTimeMillis() - lLastLogTime > 1000)) {
					strLog = "RCV> " + strBuffer;
					commTrace(strLog, null); // 로그 기록
					lLastLogTime = System.currentTimeMillis();
				}
				
				strLastReceivedString = strBuffer;
				
				// SJLEE - 통신두절 감지용 시간 설정
				lLastReceivedTime = System.currentTimeMillis();
				lLastTimeoutCheckTime = System.currentTimeMillis();
			} catch (Exception e) {
				commTrace("\tException SocketRead", null);
				commTrace("\tClientSocketClose", e);
			}
			
			
		}
	}

	public void ffuClientCommStart() {
		if (checkSocketThread == null) {
			strReceiveString = "";

			checkSocketThread = new CheckSocketThread();
			checkSocketThread.start();
		}
	}

	/**
	 * Timer를 종료한다.
	 */
	public void ffuClientCommStop() {
		if (checkSocketThread != null) {
			checkSocketThread.bRun = false;
			// 2012.06.07 by MYM : 쓰레드를 정지를 시키고 정지된 후 SocketClose 호출하도록 변경
			// ClientSocketClose();
			// m_CheckSocketThread = null;
		}
	}
}
