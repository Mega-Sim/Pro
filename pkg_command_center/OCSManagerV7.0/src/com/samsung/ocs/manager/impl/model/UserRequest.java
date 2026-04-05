package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;

import com.samsung.ocs.common.constant.OcsConstant.CYCLE_UNIT;
import com.samsung.ocs.common.constant.OcsConstant.USERREQUEST_TYPE;

/**
 * UserRequest Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 3. 7.
 * @version 3.0
 * 
 * Copyright 2013 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class UserRequest {
	protected String userRequestId = "";
	protected String typeString = "";
	protected USERREQUEST_TYPE Type = USERREQUEST_TYPE.UNDEFINED;
	protected String vehicleId = "";
	protected boolean isLoadingByPass = false;
	protected boolean isEnabled = true;
	protected String tourListString = "";
	protected int cycle = 0;
	protected String cycleUnitString = "";
	protected CYCLE_UNIT cycleUnit = CYCLE_UNIT.UNDEFINED;
	protected String dockingStationId = "";
	protected DockingStation dockingStation = null;
	protected String lastTouredTime = "";
	protected long lastTourListCheckedTime = 0;
	private int patrolMode = 0;

	protected ArrayList<Object> tourList = new ArrayList<Object>();
	protected String carrierId = "";

	public String getUserRequestId() {
		return userRequestId;
	}

	public void setUserRequestId(String userRequestId) {
		this.userRequestId = userRequestId;
	}

	public String getTypeString() {
		return typeString;
	}

	public void setTypeString(String typeString) {
		this.typeString = typeString;
	}

	public USERREQUEST_TYPE getType() {
		return Type;
	}

	public void setType(USERREQUEST_TYPE Type) {
		this.Type = Type;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public boolean isLoadingByPass() {
		return isLoadingByPass;
	}

	public void setLoadingByPass(boolean isLoadingByPass) {
		this.isLoadingByPass = isLoadingByPass;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public void disable() {
		this.isEnabled = false;
		if (tourList != null) {
			tourList.clear();
		}
	}

	public String getTourListString() {
		return tourListString;
	}

	public void setTourListString(String tourListString) {
		this.tourListString = tourListString;
	}

	public int getTourListSize() {
		if (tourList != null) {
			return tourList.size();
		}
		return 0;
	}

	public int getTourListIndexOf(Object tourItem) {
		if (tourList != null) {
			return tourList.indexOf(tourItem);
		}
		return -1;
	}

	public Object getTourListItem(int index) {
		if (tourList != null) {
			if (index <= tourList.size() - 1) {
				return tourList.get(index);
			}
		}
		return null;
	}

	public void addToTourList(Object tourItem) {
		if (tourList != null) {
			if (tourItem != null) {
				tourList.add(tourItem);
			}
		}
	}

	public int getCycle() {
		return cycle;
	}

	public void setCycle(int cycle) {
		this.cycle = cycle;
	}

	public String getCycleUnitString() {
		return cycleUnitString;
	}

	public void setCycleUnitString(String cycleUnitString) {
		this.cycleUnitString = cycleUnitString;
	}

	public CYCLE_UNIT getCycleUnit() {
		return cycleUnit;
	}

	public void setCycleUnit(CYCLE_UNIT cycleUnit) {
		this.cycleUnit = cycleUnit;
	}

	public String getDockingStationId() {
		return dockingStationId;
	}

	public void setDockingStationId(String dockingStationId) {
		this.dockingStationId = dockingStationId;
	}

	public DockingStation getDockingStation() {
		return dockingStation;
	}

	public void setDockingStation(DockingStation dockingStation) {
		this.dockingStation = dockingStation;
	}

	public String getLastTouredTime() {
		return lastTouredTime;
	}

	public void setLastTouredTime(String lastTouredTime) {
		this.lastTouredTime = lastTouredTime;
	}

	public long getLastTourListCheckedTime() {
		return lastTourListCheckedTime;
	}

	public void setLastTourListCheckedTime(long lastTourListCheckedTime) {
		this.lastTourListCheckedTime = lastTourListCheckedTime;
	}

	public String getCarrierId() {
		return carrierId;
	}

	public void setCarrierId(String carrierId) {
		this.carrierId = carrierId;
	}

	public int getPatrolMode() {
		return patrolMode;
	}

	public void setPatrolMode(int patrolMode) {
		this.patrolMode = patrolMode;
	}
	
}
