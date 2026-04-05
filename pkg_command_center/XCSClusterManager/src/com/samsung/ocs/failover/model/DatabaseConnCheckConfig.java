package com.samsung.ocs.failover.model;

/**
 * DatabaseConnCheckConfig Class, OCS 3.0 for Unified FAB
 * 
 * Database Connection Check에 관련된 Config를 저장하는 Bean객체
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

public class DatabaseConnCheckConfig {
	private long intervalMillis = 1000;
	private int timeoutCount = 5;
	
	/**
	 * intervalMillis getter
	 * @return long intervalMillis
	 */
	public long getIntervalMillis() {
		return intervalMillis;
	}
	/**
	 * intervalMillis setter
	 * @param intervalMillis
	 */
	public void setIntervalMillis(long intervalMillis) {
		this.intervalMillis = intervalMillis;
	}
	/**
	 * timeoutCount getter
	 * @return int timeoutCount
	 */
	public int getTimeoutCount() {
		return timeoutCount;
	}
	/**
	 * timeoutCount setter
	 * @param timeoutCount
	 */
	public void setTimeoutCount(int timeoutCount) {
		this.timeoutCount = timeoutCount;
	}
}
