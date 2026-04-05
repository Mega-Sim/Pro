package com.samsung.ocs.failover.thread.tcp;

import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.samsung.ocs.failover.model.HeartBeat;
import com.samsung.ocs.failover.util.log.LogWrapper;

public class TCPHeartBeatCheckThread extends AbstractWorkThread {
	private HeartBeat heartBeat;
	private LogWrapper log;
	private Socket socket;
	private long timeout;
	private TCPHeartBeatReadThread read;
	private TCPHeartBeatWriteThread write;
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public TCPHeartBeatCheckThread(HeartBeat heartBeat, Socket socket, long timeoutMillies, TCPHeartBeatReadThread read, TCPHeartBeatWriteThread write, LogWrapper log) throws SocketException, UnknownHostException {
		super(1000);
		this.heartBeat = heartBeat;
		this.log = log;
		this.timeout = timeoutMillies;
		this.read = read;
		this.write = write;
		this.socket = socket;
	}
	
	protected void initialize() {
	}

	protected void stopProcessing() {
		
	}
	
	protected void mainProcessing() {
		if (socket != null && socket.isConnected()) {
			long lastRecvTimestamp = read.getLastRecvTimestamp();
			if (lastRecvTimestamp > 0 && System.currentTimeMillis() - lastRecvTimestamp > timeout) {
				setRunFlag(false);
				close("Timeout");
			}
		} else if (socket == null) {
			setRunFlag(false);
			close("socket is null");
		} else if (socket.isConnected() == false) {
			setRunFlag(false);
			close("isConnected is false");
		}
	}
	
	public String getThreadId() {
		return this.getClass().getName();
	}
	
	private void close(String message) {
		if (read != null) {
			try { read.stopThread(); } catch (Exception ignore) {}
		}
		if (write != null) {
			try { write.stopThread(); } catch (Exception ignore) {}
		}
		String info = null;
		if (socket != null) {
			info = socket.toString();
			try { socket.close(); } catch (Exception ignore) {}
		}
		log.write(String.format("[%s][SERVER] Disconnect(%s) [%s].", sdf.format(new Date(System.currentTimeMillis())), message, info == null ? heartBeat.getIpAddress()+"/"+ heartBeat.getPort() : info));
	}
	
}
