package com.samsung.ocs.failover.thread;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.FileInfoItem;
import com.samsung.ocs.failover.model.FileSizeTraceItem;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * FileSizeTraceThread Class, OCS 3.0 for Unified FAB
 * 
 * 현재 관리중인 Local Process들 중 FileSizeTrace를 설정한 목록을 Watchdog하여 파일사이즈가 증가하였을때 Alarm을 등록하는 Thread
 * 
 * @author Wongeun.Lee
 * 
 * @date   2014. 6. 3.
 * @version 3.1
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class FileSizeTraceThread extends AbstractOcsThread {
	private Map<String, FileSizeTraceItem> fileSizeTraceItemMap;
	private boolean isPrimary = false;
	private ClusterState state = null;
	private OcsHistoryManager historyManager = null;
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
 	
	/**
	 * Constructor of FileSizeTraceThread class.
	 * 
	 * @param fc
	 * @param state
	 * @param historyManager
	 */
	public FileSizeTraceThread(FailoverConfig fc, ClusterState state, OcsHistoryManager historyManager ) {
		super(fc.getFileSizeTraceInterval());
		this.fileSizeTraceItemMap = fc.getFileSizeTraceItemMap();
		CommonConfig cc = CommonConfig.getInstance();
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(cc.getHostServiceType());
		this.state = state;
		this.historyManager = historyManager;
	}

	/**
	 * Get Class Name
	 */
	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	/**
	 * Initialize
	 */
	@Override
	protected void initialize() {
	}

	/**
	 * Stop Processing
	 */
	@Override
	protected void stopProcessing() {

	}

	/**
	 * Main Processing Method
	 */
	@Override
	protected void mainProcessing() {
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		long startTime = System.currentTimeMillis();
		log(String.format("FileSizeCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		// VIP관련한 내용이 한번 돌때까지, 기다려야제
		// Step 0.1 내 상태를 체크하고 준비가 될 경우 시작함.
		if (state.isVipCheckInitialized() == false) {
			try { sleep(500); } catch (Exception ignore) {}
			log("NOT START : VIP CEHCK NOT INITIALIZE.");
			long elapsedTime = System.currentTimeMillis() - startTime;
			log(String.format("FileSizeCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
			return;
		}
		
		// 돌면서 관리해보자 관리..
		for(FileSizeTraceItem fsti : fileSizeTraceItemMap.values()) {
			long fileSize = 0;
			for(FileInfoItem item : fsti.getFileInfoList()) {
				File path = new File(item.getFilePath());
				File[] listFiles = path.listFiles();
				if(listFiles != null && listFiles.length > 0) {
					for(File child : listFiles) {
						if(child.isDirectory() == false && child.getName().startsWith(item.getFilePrefix())) {
							fileSize += child.length();
						}
					}
				}
			}
			long oldFileSize = fsti.getFileSize();
			if(oldFileSize >= 0) {
				if(fileSize > fsti.getFileSize()) {
					historyManager.addCheckDupClusterHistory(EVENT_TYPE.ABNORMALLOGGEN, fsti.getProcessName(), isPrimary, "Exception Log occurred.");
				}
			}
			fsti.setFileSize(fileSize);
			log(fsti.getProcessName()+" oldFileSize["+oldFileSize+"] curFileSize["+fileSize+"]");
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("FileSizeCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
	}
	
	protected void log(String log) {
		logger.debug(log);
	}
	
	protected void log(Throwable w) {
		logger.debug(w.getMessage(), w);
	}
}
