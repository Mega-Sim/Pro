package com.samsung.ocs.failover.model;

/**
 * ClusterInfoItem Class, OCS 3.0 for Unified FAB
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

public class ClusterInfoItem {
	public static enum MODE {UPDATE, INIT, FORCEUNKNOWN};
	private MODE mode;
	private boolean isPrimary;
	private String infoItem;
	private String isAlive;
	private long setTime;
	
	public MODE getMode() {
		return mode;
	}
	public void setMode(MODE mode) {
		this.mode = mode;
	}
	public boolean isPrimary() {
		return isPrimary;
	}
	public void setPrimary(boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
	public String getInfoItem() {
		return infoItem;
	}
	public void setInfoItem(String infoItem) {
		this.infoItem = infoItem;
	}
	public String getIsAlive() {
		return isAlive;
	}
	public void setIsAlive(String isAlive) {
		this.isAlive = isAlive;
	}
	public long getSetTime() {
		return setTime;
	}
	public void setSetTime(long setTime) {
		this.setTime = setTime;
	}
}
