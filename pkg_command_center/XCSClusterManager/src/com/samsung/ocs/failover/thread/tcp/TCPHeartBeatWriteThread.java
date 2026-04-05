package com.samsung.ocs.failover.thread.tcp;

import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.samsung.ocs.failover.model.ClusterState;

public class TCPHeartBeatWriteThread extends AbstractWorkThread {
	
	private ClusterState state;
	private Socket socket;
	private PrintWriter writer;
	
	public TCPHeartBeatWriteThread(Socket socket, ClusterState state, int interval) {
		super(interval);
		this.state = state;
		this.socket = socket;
		try { writer = new PrintWriter(socket.getOutputStream()); } catch (Exception e) { }
	}
	
	protected void initialize() {
	}

	protected void stopProcessing() {
		if (writer != null) {
			try { writer.close(); } catch (Exception ignore) {}
		}
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	protected void mainProcessing() {
		if (socket != null && socket.isConnected() && writer != null) {
			long currentTimeMillis = System.currentTimeMillis();
			StringBuffer reportMessage = new StringBuffer("OCSHB:");
			reportMessage.append(state.isPublicNetCheckFail() == false).append(",");
			reportMessage.append(state.isLocalNetCheckFail() == false).append(",");
			reportMessage.append(state.isDbConnCheckFail() == false).append(",");
			reportMessage.append(state.isCheckThreadInitialized()).append(",");
			reportMessage.append(sdf.format(new Date(currentTimeMillis)));
			writer.println(reportMessage.toString());
			writer.flush();
		}
	}
	
	public String getThreadId() {
		return this.getClass().getName();
	}
	
}
