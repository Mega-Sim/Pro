package com.samsung.ocs.failover.thread;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.dao.OcsDBHistoryDAO;
import com.samsung.ocs.failover.dao.OcsDBStateDAO;
import com.samsung.ocs.failover.model.OcsDBStateVO;

/**
 * LogAndHistoryManageThread Class, OCS 3.0 for Unified FAB
 * 
 * 히스토리와 로그를 지우기 위한 Thread class
 * 얘는 별도의 DBAccessManager를 가지도록 해보자.. 지우는데 오래걸림 메인실행에 지장을 줄듯하기도 하다.
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2011. 6. 21.
 * @version 3.0
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class LogAndHistoryManageThread extends AbstractOcsThread {
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	private DBAccessManager dbAccessManager = null;
	private SimpleDateFormat formatter = null;
	private FailoverConfig failoverConfig = null;
	private String homePath = null;
	private String logPath = null;
	private String fileSeparator = null;
	// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화
	private long lastHistoryDeletedTime = System.currentTimeMillis();
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of LogAndHistoryManageThread class.
	 * 
	 * @param fc
	 */
	public LogAndHistoryManageThread(FailoverConfig fc) {
		super(1000);
		this.failoverConfig = fc;
		init();
	}
	
	/**
	 * Init
	 */
	private void init() {
		long startTime = System.currentTimeMillis();
		while (true) {
			if (dbAccessManager == null) {
				dbAccessManager = new DBAccessManager();
				System.out.println("DBAccessManager instance created.");
				continue;
			} 
			if (dbAccessManager.isDBConnected() == false) {
				System.out.println("dbAccessManager.isDBConnected()==false");
			} else {
				// 빈거 아무거나 가져오겠지..
				OcsDBStateVO dbState = OcsDBStateDAO.retrieveProcessState(OcsConstant.INSERVICE, dbAccessManager);
				if (dbState == null) {
					System.out.println("retrieve dbState fail.");
				} else {
					break;
				}
			}
			try {
				Thread.sleep(500);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		homePath = System.getProperty(OcsConstant.HOMEDIR);
		fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		StringBuffer logPathInfo = new StringBuffer();
		logPathInfo.append(homePath).append(fileSeparator).append(OcsConstant.LOG);
		logPath = logPathInfo.toString();
		
		formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("LogAndHistoryManage Thread initialized completed. elapsedTime[%d]millis", elapsedTime));
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#getThreadId()
	 */
	@Override
	public String getThreadId() {
		return "LogAndHistoryManageThread";
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#initialize()
	 */
	@Override
	protected void initialize() {
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#stopProcessing()
	 */
	@Override
	protected void stopProcessing() {
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.thread.AbstractOcsThread#mainProcessing()
	 */
	/**
	 * Main Processing Method
	 */
	@Override
	protected void mainProcessing() {
		try {
			long startTime = System.currentTimeMillis();
			Calendar calendar = Calendar.getInstance();
			// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 failoverConfig.xml 파일 설정으로 변경
//			if (calendar.get(Calendar.MINUTE) == 0 && calendar.get(Calendar.SECOND) == 0) {
//				log(String.format("LogAndHistoryManageThread process(remote log and history) start. startTime[%s]millis", sdf.format(new Date(startTime))));
//
//				calendar.add(Calendar.DATE, (-1) * failoverConfig.getDeleteHistoryDay() );
//				String timeBefore = formatter.format(calendar.getTime());
//				OcsDBHistoryDAO.deleteClusterHistory(dbAccessManager, timeBefore);
//
//
//				calendar.add(Calendar.DATE, failoverConfig.getDeleteHistoryDay());
//				calendar.add(Calendar.DATE, (-1) * failoverConfig.getDeleteLogDay());
//				timeBefore = formatter.format(calendar.getTime());
//				deleteLog(logPath, failoverConfig.getDeleteLogDay(), timeBefore);
//
//				long elapsedTime = System.currentTimeMillis() - startTime;
//				log(String.format("LogAndHistoryManageThread process(remote log and history) completed. elapsedTime[%d]millis", elapsedTime));
//			}
			if (calendar.get(Calendar.MINUTE) == 0 && calendar.get(Calendar.SECOND) == 0) {
				log(String.format("LogAndHistoryManageThread process(Delete Log) start. startTime[%s]millis", sdf.format(new Date(startTime))));

				calendar.add(Calendar.DATE, (-1) * failoverConfig.getDeleteLogDay());
				String timeBefore = formatter.format(calendar.getTime());
				deleteLog(logPath, failoverConfig.getDeleteLogDay(), timeBefore);
				
				long elapsedTime = System.currentTimeMillis() - startTime;
				log(String.format("LogAndHistoryManageThread process(Delete Log) completed. elapsedTime[%d]millis", elapsedTime));
			}
			calendar.clear();
			if (failoverConfig.isDeleteHistoryUse()){
				startTime = System.currentTimeMillis();
				if (startTime - lastHistoryDeletedTime > failoverConfig.getDeleteHistoryCheckInterval()){
					try {
						log(String.format("LogAndHistoryManageThread process(Delete Cluster History) start. startTime[%s]millis", sdf.format(new Date(startTime))));
						calendar = Calendar.getInstance();
						calendar.add(Calendar.DATE, (-1) * failoverConfig.getDeleteHistoryDay());
						String timeBefore = formatter.format(calendar.getTime());
						OcsDBHistoryDAO.deleteClusterHistory(dbAccessManager, timeBefore);
						long elapsedTime = System.currentTimeMillis() - startTime;
						log(String.format("LogAndHistoryManageThread process(Delete Cluster History) completed. elapsedTime[%d]millis", elapsedTime));
					} catch (Exception e) {
						log(e);
					} finally {
						lastHistoryDeletedTime = System.currentTimeMillis();
					}
				}
			}
		} catch (Exception e) {
			log(e);
		}
	}
	
	protected void log(String log) {
		logger.debug(log);
	}
	
	protected void log(Throwable w) {
		logger.debug(w.getMessage(), w);
	}

	/**
	 * Delete Log
	 * 
	 * @param path
	 * @param logHoldingPeriod
	 * @param timeBefore
	 */
	private void deleteLog(String path, int logHoldingPeriod, String timeBefore) {
		long startTime = System.currentTimeMillis();
		File dir = new File(path);
		String[] files = dir.list();
		long lastModifiedTime;
		long storedPeriod;
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				File file = new File(dir, files[i]);
				lastModifiedTime = file.lastModified();
				storedPeriod = (System.currentTimeMillis() - lastModifiedTime)/24/3600/1000;
				try {
					if (file.isDirectory()) {
						StringBuffer logPathInfo = new StringBuffer();
						logPathInfo.append(path).append(fileSeparator).append(file.getName());
						deleteLog(logPathInfo.toString(), logHoldingPeriod, timeBefore);
					}
					if (file.isDirectory()) {
						if ((storedPeriod >= logHoldingPeriod || timeBefore.compareTo(file.getName()) > 0) &&
								OcsConstant.EXCEPTION.equals(file.getName()) == false) {
							file.delete();
						}
					} else {
						if (storedPeriod >= logHoldingPeriod) {
							if(isInitFile(file.getName()) == false) {
								file.delete();
							}
						}
					}
				} catch (Exception e) {
					log(e);
				}
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("LogAndHistoryManage deleteLog completed. [%s][%d][%s] elapsedTime[%d]millis", path, logHoldingPeriod, timeBefore, elapsedTime));
		
	}
	
	private static final String PAST_PATTERN = "^(([a-zA-Z0-9]|_|-)*[a-zA-Z]|([a-zA-Z0-9]|_|-)*[a-zA-Z][0-9]).log$";
	private static final Pattern p = Pattern.compile(PAST_PATTERN);
	
	public boolean isInitFile(String fileName) {
		Matcher matcher = p.matcher(fileName);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
}
