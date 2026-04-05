package com.samsung.ocs.failover.thread;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.failover.config.FailoverConfig;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.DatabaseConnCheckConfig;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DatabaseConnCheckThread Class, OCS 3.0 for Unified FAB
 * 
 * Database의 Connection을 주기적으로 체크하여 그 결과로 ClusterState 객체를 갱신하는 Thread
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

public class DatabaseConnCheckThread extends AbstractPingCheckThread {
	private ClusterState state;
	private boolean[] checkArray;
	private int checkArrayCount = 1;
	private int checkArrayIndex = 0;
	private DBAccessManager dbAccessmanager;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of DatabaseConnCheckThread class.
	 */
	public DatabaseConnCheckThread(DatabaseConnCheckConfig config, DBAccessManager dbAccessmanager, ClusterState state) {
		this.state = state;
		this.checkArrayCount = config.getTimeoutCount();
		this.checkArray = new boolean[checkArrayCount];
		this.dbAccessmanager = dbAccessmanager; 
		for (int i=0 ; i < checkArrayCount ; i++) {
			checkArray[i] = true;
		}
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
		interval = FailoverConfig.getInstance().getDatabaseConnCheckConfig().getIntervalMillis();
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
		log(String.format("DBConnCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		//db Conn 확인한다..
		checkArray[checkArrayIndex] = dbAccessmanager.isDBConnected();
		//연속 false면 db conn이 안되는것으로 한다...  한번 안될때 
		checkArrayIndex = (checkArrayIndex+1) % checkArrayCount;
		
		int dbConnFailCnt=0;
		for (int i = 0; i < checkArrayCount; i++) {
			if (checkArray[i] == false) {
				dbConnFailCnt++;
			}
		}
		if (dbConnFailCnt == checkArrayCount) {
			// db conn 이상함.
			state.setDbConnCheckFail(true);
		} else {
			state.setDbConnCheckFail(false);
		}
		
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("DBConnCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
	}
}