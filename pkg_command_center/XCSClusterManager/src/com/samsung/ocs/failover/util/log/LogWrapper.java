package com.samsung.ocs.failover.util.log;

import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.failover.thread.tcp.AbstractWorkThread;

public class LogWrapper {
	
	private Vector<String> dataList = null;
	private LogWrapperThread thread = null;
	private int maxQueueSize;
	
	public LogWrapper(String logger, int maxQueueSize) {
		dataList = new Vector<String>();
		this.maxQueueSize = maxQueueSize;
		thread = new LogWrapperThread(logger, dataList);
		thread.setRunFlag(true);
		thread.start();
	}
	
	public void write(String data) {
		if (dataList.size() < maxQueueSize) {
			dataList.add(data);
		}
	}
	
	public void close() {
		thread.stopThread();
	}
}

class LogWrapperThread extends AbstractWorkThread {

	private Vector<String> dataList = null;
	private Logger log;
	private String logger;

	public LogWrapperThread(String logger, Vector<String> sendDataList) {
		super(200);
		this.dataList = sendDataList;
		this.logger = logger;
		this.log = Logger.getLogger(logger);
	}

	@Override
	public String getThreadId() {
		return "LogWrapperThread[" + logger + "]";
	}

	protected void initialize() { }

	protected void stopProcessing() { }

	protected void mainProcessing() {
		int sendCnt = 0;
		int sendSize = dataList.size();
		for (int i = 0; i < sendSize; i++) {
			String data = dataList.get(i);
			log.debug(data);
			sendCnt++;
		}
		for (int i = 0; i < sendCnt; i++) {
			dataList.remove(0);
		}
	}
}