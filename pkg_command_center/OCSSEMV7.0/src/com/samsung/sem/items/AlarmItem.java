package com.samsung.sem.items;

import java.util.ArrayList;

/**
 * AlarmItem Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date 2011. 6. 21.
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

public class AlarmItem {	
	/*****************************************************************************
	 *  AlarmItem Class
	 *  - SetData(), SetEnabled(), SetActivated(), SetVehicleName()
	 *  - GetALID(), GetEnabled(), GetActivated(), GetAlarmText(), GetVehicleName()
	 *****************************************************************************/

	/* variables */
	private long alarmId;
	private String alarmText;
	private String vehicleId;
	private boolean isEnabled;
	private boolean isActivated;
	private ArrayList<String> vehicleList;

	/**
	 * Constructor of AlarmItem class.
	 */
	public AlarmItem() {
		alarmId = 0;
		isEnabled = false;
		isActivated = false;
		// 2013.10.02 by KYK
		vehicleList = new ArrayList<String>();
	}

	/* methods : getter, setter */
	public boolean setData(long alid, String alarmText)	{
		this.alarmId = alid;
		this.alarmText = alarmText;
		return true;
	}

	public long getAlarmId() {
		return alarmId;
	}
	public void setAlarmId(long alarmId) {
		this.alarmId = alarmId;
	}
	public String getAlarmText() {
		return alarmText;
	}
	public void setAlarmText(String alarmText) {
		this.alarmText = alarmText;
	}
	public String getVehicleId() {
		return vehicleId;
	}
	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}
	public boolean isEnabled() {
		return isEnabled;
	}
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	public boolean isActivated() {
		return isActivated;
	}
	public void setActivated(boolean isActivated) {
		this.isActivated = isActivated;
	}
	
	// 2013.10.02 by KYK
	public ArrayList<String> getVehicleList() {
		return vehicleList;
	}
	public void addVehicleToList(String vehicleId) {
		if (vehicleList.contains(vehicleId) == false) {
			vehicleList.add(vehicleId);			
		}
	}
}
