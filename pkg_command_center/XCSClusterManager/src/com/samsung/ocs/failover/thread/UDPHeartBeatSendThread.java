package com.samsung.ocs.failover.thread;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.common.udpu.UDPAsyncSender;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.HeartBeat;
import com.samsung.ocs.failover.util.log.LogWrapper;

public class UDPHeartBeatSendThread extends AbstractOcsThread {
	private ClusterState state;
	private List<UDPAsyncSender> senderList = new ArrayList<UDPAsyncSender>();
	private List<HeartBeat> heartBeatList;
	private LogWrapper log;
	
	
	/**
	 * Constructor of HeartBeatCheckThread class.
	 * 
	 * @param config
	 * @param state
	 * @exception SocketException
	 * @exception UnknownHostException
	 */
	public UDPHeartBeatSendThread(FailoverConfig config, ClusterState state) throws SocketException, UnknownHostException {
		super();
		this.state = state;
		this.heartBeatList = config.getHeartBeatList();
		for (HeartBeat hb : heartBeatList) {
			UDPAsyncSender sender = new UDPAsyncSender(hb.getIpAddress(), hb.getPort());
			senderList.add(sender);
		}
		log = new LogWrapper(CommonLogFileName.CLUSTERMANAGER, 100);
	}
	
	/**
	 * Initialize
	 */
	protected void initialize() {
		interval = FailoverConfig.getInstance().getHeartBeatInterval();
	}

	/**
	 * Stop Processing
	 */
	protected void stopProcessing() {
		
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	/**
	 * Main Processing Method
	 */
	protected void mainProcessing() {
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		long startTime = System.currentTimeMillis();
		String start = sdf.format(new Date(startTime));
		log.write(String.format("[%s]UDPHeartBeatSendThread process start. startTime[%s]millis", start, start));
		
		//heart beat
		long currentTimeMillis = System.currentTimeMillis();
		StringBuffer reportMessage = new StringBuffer("OCSHB:");
		reportMessage.append(state.isPublicNetCheckFail() == false).append(",");
		reportMessage.append(state.isLocalNetCheckFail() == false).append(",");
		reportMessage.append(state.isDbConnCheckFail() == false).append(",");
		reportMessage.append(state.isCheckThreadInitialized()).append(",");
		reportMessage.append(sdf.format(new Date(currentTimeMillis)));
		
		byte[] message = reportMessage.toString().getBytes();
		for (UDPAsyncSender sender : senderList) {
			sender.send(message);
		}
		
		long endTime = System.currentTimeMillis();
		long elapsedTime = endTime - startTime;
		String end = sdf.format(new Date(endTime));
		log.write(String.format("[%s]UDPHeartBeatSendThread process completed. elapsedTime[%d]millis", end, elapsedTime));
	}
	
	public String getThreadId() {
		return this.getClass().getName();
	}
	
}
