package com.samsung.ocs.failover.thread.tcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import com.samsung.ocs.failover.model.HeartBeat;

public class TCPHeartBeatReadThread extends AbstractWorkThread {
	private HeartBeat heartBeat;
	private Socket socket;
	private InputStreamReader isr;
	private BufferedReader reader;
	private long lastRecvTimestamp = 0L;
	
	public TCPHeartBeatReadThread(Socket socket, HeartBeat heartBeat, int interval) {
		super(interval);
		this.socket = socket;
		this.heartBeat = heartBeat;
		try {
			isr = new InputStreamReader(socket.getInputStream());
			reader = new BufferedReader(isr);
		} catch (Exception e) {}
	}
	
	protected void initialize() {
	}

	protected void stopProcessing() {
		if (reader != null) {
			try { reader.close(); } catch (Exception ignore) {}
		}
		if (isr != null) {
			try { isr.close(); } catch (Exception ignore) {}
		}
	}

	protected void mainProcessing() {
		if (socket != null && socket.isConnected() && reader != null) {
			try {
				String str = reader.readLine();
				if (str == null) {
					return;
				}
				str = getHeartBeatString(str);
				long currentTimeMillis = System.currentTimeMillis();
				if (str != null) {
					synchronized (heartBeat) {
						heartBeat.setLastReportedString(str.substring(6));
						heartBeat.setLastReportedTime(currentTimeMillis);
						heartBeat.setReported(true);
					}
				}
				lastRecvTimestamp = currentTimeMillis;
			} catch (Exception e) {
			}
		}
	}
	
	private String getHeartBeatString(String str) {
		int oCnt = 0;
		int headerIdx = 0;
		int splitIdx = 0;
		for (int i = 0; i < str.length(); i++) {
			if ('O' == str.charAt(i)) {
				oCnt++;
				if (oCnt == 1) {
					headerIdx = i;
				} else if (oCnt == 2) {
					splitIdx = i;
				}
			}
		}
		if (oCnt == 1 && str.length() > 6) {
			if (headerIdx > 0) {
				return str.substring(headerIdx).trim();
			}
			return str;
		} else if (oCnt > 1) {
			return str.substring(headerIdx, splitIdx).trim();
		}
		return null;
	}

	public String getThreadId() {
		return this.getClass().getName();
	}

	public long getLastRecvTimestamp() {
		return lastRecvTimestamp;
	}
	
//	public static void main(String[] args) {
//		String str = "CSHB:false,false,false,true,yyyyMMddHHmmss OCSHB:false,false,false,true,yyyyMMddHHmmss";
//		
//		String heartBeatString = getHeartBeatString(str);
//		
//		System.out.println("["+heartBeatString+"]");
//	}

}
