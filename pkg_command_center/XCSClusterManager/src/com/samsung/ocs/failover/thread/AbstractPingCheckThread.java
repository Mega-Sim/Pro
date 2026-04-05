package com.samsung.ocs.failover.thread;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.thread.AbstractOcsThread;

/**
 * AbstractPingCheckThread Abstract Class, OCS 3.0 for Unified FAB
 * 
 * Network Ping Check하는 Thread에서 공통적으로 쓸 메서드를 정의한 Thread
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

public abstract class AbstractPingCheckThread extends AbstractOcsThread {
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	
	/**
	 * String ipAddress로의 ping check를 하는 메서드
	 * @param ipAddress : 체크할 ipaddress
	 * @param pingTimeout : ping check 타임아웃값
	 * @return boolean pingCheckResult
	 */
	protected boolean pingCheck(String ipAddress, int pingTimeout) {
		boolean result = false;
		try {
			InetAddress target = InetAddress.getByName(ipAddress);
			result = target.isReachable(pingTimeout);
		} catch (Exception ignore) {}
		return result;
	}
	
	protected void log(String log) {
		logger.debug(log);
	}
	
	protected void log(Throwable w) {
		logger.debug(w.getMessage(), w);
	}
}
