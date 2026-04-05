package uohtcontroller;

import java.net.*;


public class UDPComm {
  private UOCSMain m_UOCSMain = null;
  private UDPServerThread m_UDPServerThread = null;
  private int m_nPort = 4050;

  private utilLog m_MainLog = null;

  class UDPServerThread extends Thread {
    DatagramSocket m_Socket = null;
    byte[] inbuf = new byte[3072]; // Data를 수신할 버퍼
    byte[] outbuf = new byte[1024]; // Data를 송신할 버퍼
    DatagramPacket m_RcvPacket = new DatagramPacket(inbuf, inbuf.length); // Data 수신용 packet 선언 및 생성
    DatagramPacket m_SndPacket = null; // Data 송신용 packet 선언

    public boolean m_bRun = true;

    private String m_strRcvString = "";
    private String m_strSndString = "";
    private long m_lLastMsgReceivedTime = System.currentTimeMillis();

    // 2010.02.22 by MYM : Target IP, Port
    String m_strTargetIPAddress = "";
    int m_nTargetPort = 4050;


    public UDPServerThread() throws SocketException {
      // DatagramPacket을 받기 위한 Socket 생성
      if (m_Socket == null) {
        m_Socket = new DatagramSocket(m_nPort);
      }
    }

    public void run() {
      while (m_bRun) {
        try {
          m_Socket.receive(m_RcvPacket); // 데이터가 수신될 때까지 대기함(block 함수)
          m_strRcvString = new String(m_RcvPacket.getData(), 0, m_RcvPacket.getLength());
          UDPCommTrace("RCV> " + m_strRcvString, null);
          System.out.println("RCV> " + m_strRcvString);

          m_strSndString = UDPSocketReceived(m_strRcvString);
          m_lLastMsgReceivedTime = System.currentTimeMillis();

          if (m_strSndString.equals("") == false) {
            InetAddress IPAddress = m_RcvPacket.getAddress();
            int nPort = m_RcvPacket.getPort();
            System.out.println("Client IP: " + IPAddress + ", Client Port:" + nPort);

            // 2010.02.22 by MYM : ULR로 보내온 IP, Port로 Reply
            String strMsgName = GetMsgName(m_strRcvString);
            if(strMsgName.equals("ULR") || strMsgName.equals("DGR") ||
               strMsgName.equals("SVR") || strMsgName.equals("GVR"))
            {
              String strTargetIP = GetMsgItem(m_strRcvString, "IP=");
              String strTargetPort = GetMsgItem(m_strRcvString, "PORT=");
              if(strTargetIP.equals("") == false)
                m_strTargetIPAddress = strTargetIP;
              if(strTargetPort.equals("") == false)
                m_nTargetPort = Integer.parseInt(strTargetPort);
            }

            outbuf = m_strSndString.getBytes();

            // 2010.02.22 by MYM : ULR로 보내온 IP, Port로 Reply
//            m_SndPacket = new DatagramPacket(outbuf, outbuf.length, IPAddress, nPort); // 송신용 packet 생성
            m_SndPacket = new DatagramPacket(outbuf, outbuf.length, IPAddress, m_nTargetPort); // 송신용 packet 생성
            m_Socket.send(m_SndPacket); // 데이터 송신
            UDPCommTrace("SND> " + m_strSndString, null);
            System.out.println("SND> " + m_strSndString);
          }

          m_strRcvString = "";
        }
        catch (Exception e) {
          e.printStackTrace();
          String strLog = "UDPServerThread - Exception: " + e.getStackTrace();
          UDPCommTrace(strLog, null);
        }
      }
    }
  }

  public UDPComm(UOCSMain OCSMain, utilLog MainLog, int port) {
    m_UOCSMain = OCSMain;
    m_MainLog = MainLog;
    m_nPort = port;
  }

  /**
   * ServerThread를 시작시키는 함수이다.
   */
  public void ServerThreadStart() {
    try {
      m_UDPServerThread = new UDPServerThread();
      m_UDPServerThread.start();
    }
    catch (Exception e) {
      e.printStackTrace();
      String strLog = "ServerThreadStart - Exception: " + e.getStackTrace();
      UDPCommTrace(strLog, null);
    }
  }

  /**
   * ServerThread를 종료시키는 함수이다.
   */
  public void ServerThreadStop() {
    if (m_UDPServerThread != null) {
      m_UDPServerThread.m_bRun = false;
      m_UDPServerThread = null;
    }
  }

  /**
   * UDP Packet에서 데이터를 수신했을때 이를 처리하는 함수이다.
   * @param strRcvString String
   */
  public String UDPSocketReceived(String strRcvString) {
    String strSndString = m_UOCSMain.InterpretReceivedMsg(strRcvString);

    return strSndString;
  }

  public void UDPCommTrace(String strLog1, String strLog2) {
    m_MainLog.WriteReturnLog("UDPCommLog", strLog1, strLog2, true);
  }

  /**
   *
   * @param strMsgString String
   * @return String
   * @version 2010.02.22 by MYM
   */
  public String GetMsgName(String strMsgString)
  {
    if (strMsgString.length() <= 0)
      return "";

    String sDelimiter = " ";
    String strMsgName = "";
    int nPos = strMsgString.indexOf(sDelimiter);
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

  /**
   *
   * @param strMsgString String
   * @param strKey String
   * @return String
   * @version 2010.02.22 by MYM
   */
  public String GetMsgItem(String strMsgString, String strKey)
  {
    if (strMsgString.length() <= 0)
      return "";

    int nPos = 0;
    String sDelimiter = " ";
    String strMsgItem = "";
    nPos = strMsgString.indexOf(strKey);
    if (nPos > 0)
    {
      String strTempMsgString = strMsgString.substring(nPos);
      nPos = strTempMsgString.indexOf(sDelimiter);
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
}
