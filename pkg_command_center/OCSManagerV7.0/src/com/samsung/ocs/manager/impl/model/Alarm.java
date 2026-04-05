package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;

/**
 * Alarm Class, OCS 3.0 for Unified FAB
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

public class Alarm {
	protected String alarmId;
	protected String vehicle;
	protected String alarmText;
	protected String alarmTime;
	protected ALARMLEVEL alarmLevel;
	
	 /**
	 * Constructor of Alarm class.
	 */
	public Alarm() {
		this.vehicle = "";
		this.alarmId = "";
		this.alarmText = "";
		this.alarmTime = "";
		this.alarmLevel = ALARMLEVEL.ERROR;
	}
	
	 /**
	 * Constructor of Alarm class.
	 */
	public Alarm (String vehicle, String alarmId, String alarmText, ALARMLEVEL alarmLevel) {
		setVehicle(vehicle);
		setAlarmId(alarmId);
		setAlarmText(alarmText);
		setAlarmLevel(alarmLevel);
	}
	
	public String getAlarmId() {
		return alarmId;
	}

	public void setAlarmId(String alarmId) {
		this.alarmId = alarmId;
	}

	public String getVehicle() {
		return vehicle;
	}

	public void setVehicle(String vehicle) {
		this.vehicle = vehicle;
	}

	public String getAlarmText() {
		return alarmText;
	}

	public void setAlarmText(String alarmText) {
		this.alarmText = alarmText;
	}

	public String getAlarmTime() {
		return alarmTime;
	}

	public void setAlarmTime(String alarmTime) {
		this.alarmTime = alarmTime;
	}

	public ALARMLEVEL getAlarmLevel() {
		return alarmLevel;
	}

	public void setAlarmLevel(ALARMLEVEL alarmLevel) {
		this.alarmLevel = alarmLevel;
	}
	
}
