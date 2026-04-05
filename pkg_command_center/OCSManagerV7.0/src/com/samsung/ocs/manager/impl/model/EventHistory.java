package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_NAME;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REASON;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_REMOTEID;
import com.samsung.ocs.common.constant.EventHistoryConstant.EVENTHISTORY_TYPE;

/**
 * EventHistory Class, OCS 3.0 for Unified FAB
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

public class EventHistory {
	protected EVENTHISTORY_NAME name;
	protected EVENTHISTORY_TYPE type;
	protected String subType;
	protected String event;
	protected String setTime;
	protected String clearTime;
	protected EVENTHISTORY_REMOTEID remoteId;
	protected String remoteIp;
	protected EVENTHISTORY_REASON reason;
	
	/**
	 * Constructor of EventHistory class.
	 */
	public EventHistory() {
	}
	
	/**
	 * Constructor of EventHistory class.
	 */
	public EventHistory(EVENTHISTORY_NAME name, EVENTHISTORY_TYPE type, String subType, 
			String event, String setTime, String clearTime, EVENTHISTORY_REMOTEID remoteId, 
			String remoteIp, EVENTHISTORY_REASON reason) {
		this.name = name;
		this.type = type;
		this.subType = subType;
		this.event = event;
		this.setTime = setTime;
		this.clearTime = clearTime;
		this.remoteId = remoteId;
		this.remoteIp = remoteIp;
		this.reason = reason;
	}
	
	public EVENTHISTORY_NAME getName() {
		return name;
	}
	public void setName(EVENTHISTORY_NAME name) {
		this.name = name;
	}
	public EVENTHISTORY_TYPE getType() {
		return type;
	}
	public void setType(EVENTHISTORY_TYPE type) {
		this.type = type;
	}
	public String getSubType() {
		return subType;
	}
	public void setSubType(String subType) {
		this.subType = subType;
	}
	public String getEvent() {
		if (event == null) return "";
		else return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public String getSetTime() {
		return setTime;
	}
	public void setSetTime(String setTime) {
		this.setTime = setTime;
	}
	public String getClearTime() {
		return clearTime;
	}
	public void setClearTime(String clearTime) {
		this.clearTime = clearTime;
	}
	public EVENTHISTORY_REMOTEID getRemoteId() {
		return remoteId;
	}
	public void setRemoteId(EVENTHISTORY_REMOTEID remoteId) {
		this.remoteId = remoteId;
	}
	public String getRemoteIp() {
		return remoteIp;
	}
	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}
	public EVENTHISTORY_REASON getReason() {
		return reason;
	}
	public void setReason(EVENTHISTORY_REASON reason) {
		this.reason = reason;
	}
}
