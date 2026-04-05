package com.samsung.ocs.ibsem.stbc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.ibsem.stbc.model.SendMsg;
import com.samsung.ocs.ibsem.stbc.thread.STBReportCommSocketReadThread;
import com.samsung.ocs.ibsem.stbc.thread.STBReportCommSocketWriteThread;

public class STBReportComm {

	protected int port = 6001;
	protected ServerSocket serverSocket = null;
	protected Socket clientSocket = null;
	protected BufferedInputStream readBuffer = null;
	protected BufferedOutputStream writeBuffer = null;

	protected byte[] sendMessage = null;
	protected Vector<SendMsg> sendMessageList = new Vector<SendMsg>();
	
	protected STBReportCommSocketReadThread socketReadThread = null;
	protected STBReportCommSocketWriteThread socketWriteThread = null;
	
	protected boolean stbReportCommUse = true;

	protected static final String STB_REPORT_DEBUG_TRACE = "STBReportDebug";
	protected static final String STB_REPORT_COMM_TRACE = "STBReportComm";
	protected static final String STB_REPORT_EXCEPTION_TRACE = "STBReportException";
	protected static Logger stbReportDebugTraceLog = Logger.getLogger(STB_REPORT_DEBUG_TRACE);
	protected static Logger stbReportCommTraceLog = Logger.getLogger(STB_REPORT_COMM_TRACE);
	protected static Logger stbReportExceptionTraceLog = Logger.getLogger(STB_REPORT_EXCEPTION_TRACE);
	
	private String carriageReturnAndLineFeed = "";
	/**
	 * Constructor of STBCReportComm class.
	 */
	public STBReportComm(int port) {
		this.port = port;
		
		CharArrayWriter arrayWriter = new CharArrayWriter(2);
		arrayWriter.write(0x0D);
		arrayWriter.write(0x0A);
		carriageReturnAndLineFeed = arrayWriter.toString();
			
		initialize();
	}
	
	public void initialize() {
		sendMessage = null;
		sendMessageList.clear();
	}
	
	public void startSTBReport() {
		if (socketWriteThread == null) {
			initialize();
			socketWriteThread = new STBReportCommSocketWriteThread(this);
			socketWriteThread.start();
		}
	}
	
	public void stopSTBReport() {
		if (socketWriteThread != null) {
			socketWriteThread.stopThread();
			socketWriteThread = null;
			initialize();
		}
	}
	
	private void openServerSocket() {
		try {
			serverSocket = new ServerSocket(port);
			stbReportTrace("openServerSocket(): Open server socket.", null);
			
			socketReadThread = new STBReportCommSocketReadThread(this);
			socketReadThread.start();
		} catch(Exception e) {
			serverSocket = null;
			stbReportTrace("openServerSocket(): Fail to open server socket.", null);
			stbReportTrace("openServerSocket()", e);
		}
	}
	
	public void closeServerSocket() {
		if (serverSocket == null) {
			return;
		}
		if (socketReadThread != null) {
			socketReadThread.stopThread();
			socketReadThread = null;
		}
		try {
			if (serverSocket != null) {
				serverSocket.close();
				serverSocket = null;
				stbReportTrace("closeServerSocket(): Close server socket.", null);
			}
		} catch (Exception e) {
			stbReportTrace("closeServerSocket(): Fail to close server socket.", null);
			stbReportTrace("closeServerSocket()", e);
		}
	}
	
	public void closeClientSocket() {
		if (clientSocket == null) {
			return;
		}
		try {
			if (writeBuffer != null) {
				writeBuffer.close();
				writeBuffer = null;
			}
			if (readBuffer != null) {
				readBuffer.close();
				readBuffer = null;
			}
			if (clientSocket != null) {
				clientSocket.close();
				clientSocket = null;
				stbReportTrace("closeClientSocket(): Close client socket.", null);
			}
		} catch (Exception e) {
			stbReportTrace("closeClientSocket(): Fail to close client socket.", null);
			stbReportTrace("closeClientSocket()", e);
		}
	}
	
	public void registerSendData(String eventName, String vehicleId, String carrierLoc, String carrier) {
		// 2014.11.18 by KBS : Client 연결이 없을 경우 STB Event 버림
		if (clientSocket != null) {
			SendMsg msg = new SendMsg(eventName, vehicleId, carrierLoc, carrier);

			if (sendMessageList.contains(msg)) {
				return;
			}

			sendMessageList.add(msg);
		}
	}
	
	private boolean sendMessage(Socket socket, byte[] sendMessage) {
		if (socket == null) {
			return false;
		} else {
			try {
				writeBuffer.write(sendMessage);
				writeBuffer.flush();
			} catch (IOException e) {
				stbReportTrace("writeSocketProcess()", e);
				return false;
			}
			return true;
		}
	}
	
	public void writeSocketProcess() {
		boolean result = false;
		String msg = null;
		
		if (stbReportCommUse == false) {
			if (serverSocket != null) {
				serverSocket.isClosed();
				serverSocket = null;
			}
			return;
		}

		if (serverSocket == null || serverSocket.isClosed()) {
				openServerSocket();
		} else {
			try {
				if (clientSocket == null) {
					clientSocket = serverSocket.accept();
					writeBuffer = new BufferedOutputStream(clientSocket.getOutputStream());
					readBuffer = new BufferedInputStream(clientSocket.getInputStream());
					stbReportTrace("writeSocketProcess(): Client socket opened by client.(IP:" + clientSocket.getInetAddress() + ")", null);
				}

				if (sendMessageList.size() > 0) {
					msg = sendMessageList.remove(0).getReqMsg() + carriageReturnAndLineFeed;
					sendMessage = msg.getBytes();
					result = sendMessage(clientSocket, sendMessage);
					if (result) {
						String strLog = "SND> " + msg;
						stbReportCommTrace(strLog);
					}
				}
			} catch (IOException e) {
				closeClientSocket();
				stbReportTrace("writeSocketProcess(): Fail to write client socket.", null);
				stbReportTrace("writeSocketProcess()", e);
			}
		}
	}

	public void readSocketProcess() {
		try {
			if (readBuffer != null) {
				int length = readBuffer.read();
				if (length < 0) {
					stbReportTrace("readSocketProcess(): Client socket closed from client.(IP:" + clientSocket.getInetAddress() + ")", null);
					closeClientSocket();
				}
			}
		} catch (IOException e) {
			closeClientSocket();
			stbReportTrace("readSocketProcess(): Fail to read client socket.", null);
			stbReportTrace("readSocketProcess()", e);
		}
	}
	
	private void stbReportTrace(String message, Throwable e) {
		if (e == null) {
			stbReportDebugTraceLog.debug(String.format("%s", message));
		} else {
			stbReportExceptionTraceLog.error(String.format("%s", message), e);
		}
	}
	
	private void stbReportCommTrace(String message) {
		stbReportCommTraceLog.debug(String.format("%s", message));
	}

	public void setStbReportCommUse(boolean stbReportCommUse) {
		this.stbReportCommUse = stbReportCommUse;
	}
	
}
