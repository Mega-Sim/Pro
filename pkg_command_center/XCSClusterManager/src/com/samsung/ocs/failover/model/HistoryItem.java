package com.samsung.ocs.failover.model;

import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;

/**
 * HistoryItem Class, OCS 3.0 for Unified FAB
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

public class HistoryItem {
	private EVENT_TYPE type;
	private String processName;
	private boolean isPrimary;
	private String alarmText;
	private long setTime;
	
	public EVENT_TYPE getType() {
		return type;
	}
	public void setType(EVENT_TYPE type) {
		this.type = type;
	}
	public String getProcessName() {
		return processName;
	}
	public void setProcessName(String processName) {
		this.processName = processName;
	}
	public boolean isPrimary() {
		return isPrimary;
	}
	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
	public String getAlarmText() {
		return alarmText;
	}
	public void setAlarmText(String alarmText) {
		this.alarmText = alarmText;
	}
	public long getSetTime() {
		return setTime;
	}
	public void setSetTime(long setTime) {
		this.setTime = setTime;
	}
}
