package com.samsung.ocs.manager.impl.model;

/**
 * LocalGroupInfo Class, OCS 3.0 for Unified FAB
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

public class LocalGroupInfo {
	protected String localGroupId;
	protected String bay;
	protected boolean enabled;
	protected int minVHL;
	protected int maxVHL;
	protected int setVHL;
	protected int curVHL;
	protected int bayVHL;
	protected int distance;
	protected String expiredTime;
	protected String updateTime;
	protected String assignOption;
	protected boolean ocsRegistered;

	public String getLocalGroupId() {
		return localGroupId;
	}
	public void setLocalGroupId(String localGroupId) {
		this.localGroupId = localGroupId;
	}
	public String getBay() {
		return bay;
	}
	public void setBay(String bay) {
		this.bay = bay;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public int getMinVHL() {
		return minVHL;
	}
	public void setMinVHL(int minVHL) {
		this.minVHL = minVHL;
	}
	public int getMaxVHL() {
		return maxVHL;
	}
	public void setMaxVHL(int maxVHL) {
		this.maxVHL = maxVHL;
	}
	public int getSetVHL() {
		return setVHL;
	}
	public void setSetVHL(int setVHL) {
		this.setVHL = setVHL;
	}
	public int getCurVHL() {
		return curVHL;
	}
	public void setCurVHL(int curVHL) {
		this.curVHL = curVHL;
	}
	public int getBayVHL() {
		return bayVHL;
	}
	public void setBayVHL(int bayVHL) {
		this.bayVHL = bayVHL;
	}
	public int getDistance() {
		return distance;
	}
	public void setDistance(int distance) {
		this.distance = distance;
	}
	public String getExpiredTime() {
		return expiredTime;
	}
	public void setExpiredTime(String expiredTime) {
		this.expiredTime = expiredTime;
	}
	public String getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}
	public String getAssignOption() {
		return assignOption;
	}
	public void setAssignOption(String assignOption) {
		this.assignOption = assignOption;
	}
	public boolean isOcsRegistered() {
		return ocsRegistered;
	}
	public void setOcsRegistered(boolean ocsRegistered) {
		this.ocsRegistered = ocsRegistered;
	}
}
