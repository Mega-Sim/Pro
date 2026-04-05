/**
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */
package com.samsung.ocs.failover.model;

import java.util.HashMap;
import java.util.Map;

/**
 * NetworkPingCheckConfig Class, OCS 3.0 for Unified FAB
 * 
 * network ping check에 관련된 Config를 저장하는 Bean객체
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
public class NetworkPingCheckConfig {
	
	private boolean use = false;
	private Map<String, String> ipListMap = new HashMap<String,String>();
	private long pingTimeoutMillis = 1000;
	private long intervalMillis = 1000;
	public boolean isUse() {
		return use;
	}
	public void setUse(boolean use) {
		this.use = use;
	}
	public long getPingTimeoutMillis() {
		return pingTimeoutMillis;
	}
	public void setPingTimeoutMillis(long pingTimeoutMillis) {
		this.pingTimeoutMillis = pingTimeoutMillis;
	}
	public long getIntervalMillis() {
		return intervalMillis;
	}
	public void setIntervalMillis(long intervalMillis) {
		this.intervalMillis = intervalMillis;
	}
	public Map<String, String> getIpListMap() {
		return ipListMap;
	}
	public void setIpListMap(Map<String, String> ipListMap) {
		this.ipListMap = ipListMap;
	}
	
}
