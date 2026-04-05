package com.samsung.ocs.failover.thread;

import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.HeartBeat;
import com.samsung.ocs.failover.thread.tcp.TCPHeartBeatReadThread;
import com.samsung.ocs.failover.thread.tcp.TCPHeartBeatWriteThread;
import com.samsung.ocs.failover.util.log.LogWrapper;

public class TCPClientHeartBeatThread extends AbstractOcsThread {
	private ClusterState state;
	private HeartBeat heartBeat;
	private LogWrapper log;
	private Socket socket = null;
	private TCPHeartBeatReadThread read;
	private TCPHeartBeatWriteThread write;
	private long timeout;
	
	public TCPClientHeartBeatThread(HeartBeat heartBeat, ClusterState state, long timeoutMillies) {
		super(1000);
		this.state = state;
		this.heartBeat = heartBeat;
		this.log = new LogWrapper(ClusterConstant.TCPLOGGER, 100);
		this.timeout = timeoutMillies;
		reConnect();
	}
	
	protected void initialize() {
	}

	protected void stopProcessing() {
		
	}

	protected void mainProcessing() {
//		if(socket == null || socket.isConnected() == false) {
//			close();
//			reConnect();
//		}
		
		if (socket != null && socket.isConnected()) {
			long lastRecvTimestamp = read.getLastRecvTimestamp();
			if (lastRecvTimestamp > 0 && System.currentTimeMillis() - lastRecvTimestamp > timeout) {
				close("Timeout");
				reConnect();
			}
		} else if (socket == null) {
			close("socket is null");
			reConnect();
		} else if (socket.isConnected() == false) {
			close("isConnected is false");
			reConnect();
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
		log.write(String.format("[%s][SERVER] Disconnect(%s) [%s].", sdf.format(new Date(System.currentTimeMillis())), message, info == null ? heartBeat.getIpAddress() + "/" + heartBeat.getPort() : info));
		socket = null;
	}
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private void reConnect() {
		log.write(String.format("[%s][SENDER] Try connect to[%s/%d].", sdf.format(new Date(System.currentTimeMillis())), heartBeat.getIpAddress(), heartBeat.getPort()));
		try {
			socket = new Socket(InetAddress.getByName(heartBeat.getIpAddress()), heartBeat.getPort());
			read = new TCPHeartBeatReadThread(socket, heartBeat, 10);
			read.setRunFlag(true);
			read.start();
			write = new TCPHeartBeatWriteThread(socket, state, 500);
			write.setRunFlag(true);
			write.start();
		} catch (Exception e) {
			log.write(String.format("[%s][SENDER] Connect to[%s/%d] Fail. [%s]", sdf.format(new Date(System.currentTimeMillis())), heartBeat.getIpAddress(), heartBeat.getPort(), e.getMessage()));
			close(e.getMessage());
		}
	}
	
}
