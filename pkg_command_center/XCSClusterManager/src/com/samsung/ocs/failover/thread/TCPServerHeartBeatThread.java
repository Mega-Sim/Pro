package com.samsung.ocs.failover.thread;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.failover.XCSClusterManager;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.HeartBeat;
import com.samsung.ocs.failover.thread.tcp.TCPHeartBeatCheckThread;
import com.samsung.ocs.failover.thread.tcp.TCPHeartBeatReadThread;
import com.samsung.ocs.failover.thread.tcp.TCPHeartBeatWriteThread;
import com.samsung.ocs.failover.util.log.LogWrapper;

public class TCPServerHeartBeatThread extends AbstractOcsThread {
	private ClusterState state;
	private HeartBeat heartBeat;
	private LogWrapper log;
	private ServerSocket ss = null;
	
	private XCSClusterManager xcsClusterManager;
	
	public TCPServerHeartBeatThread(XCSClusterManager xcsClusterManager, HeartBeat heartBeat, ClusterState state) {
		super(1000);
		this.xcsClusterManager = xcsClusterManager;
		this.state = state;
		this.heartBeat = heartBeat;
		this.log = new LogWrapper(ClusterConstant.TCPLOGGER, 100);
		init();
	}
	
	protected void initialize() {
	}

	protected void stopProcessing() {
		
	}

	protected void mainProcessing() {
		while (true) {
			try {
				Socket socket = ss.accept();
				log.write(String.format("[%s][SERVER] Connect from [%s].", sdf.format(new Date(System.currentTimeMillis())), socket.toString()));
				TCPHeartBeatReadThread read = new TCPHeartBeatReadThread(socket, heartBeat, 10);
				read.setRunFlag(true);
				read.start();
				TCPHeartBeatWriteThread write = new TCPHeartBeatWriteThread(socket, state, 500);
				write.setRunFlag(true);
				write.start();
				TCPHeartBeatCheckThread check = new TCPHeartBeatCheckThread(heartBeat, socket, FailoverConfig.getInstance().getTcpServerTimeoutMillis(), read, write, log);
				check.setRunFlag(true);
				check.start();
			} catch (Exception e) {
				traceException("", e);
			}
		}
	}
	
	public String getThreadId() {
		return this.getClass().getName();
	}
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private void init() {
		try {
			ss = new ServerSocket(heartBeat.getPort());
			log.write(String.format("[%s][SERVER] Create Server Socket[%d].", sdf.format(new Date(System.currentTimeMillis())), heartBeat.getPort()));
		} catch (Exception e) {
			log.write(String.format("[%s][SERVER] Create Server Socket[%d] Fail. [%s]", sdf.format(new Date(System.currentTimeMillis())), heartBeat.getPort(), e.getMessage()));
		}
	}
	
	private void traceException(String message, Throwable t) {
		xcsClusterManager.traceException(message, t);
	}
	
	private void traceException(String message) {
		xcsClusterManager.traceException(message);
	}
}
