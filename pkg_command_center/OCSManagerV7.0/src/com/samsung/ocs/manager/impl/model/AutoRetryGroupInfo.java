package com.samsung.ocs.manager.impl.model;

/**
 * AutoRetryGroupInfo Class, OCS 3.1 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2012. 8. 21.
 * @version 3.1
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class AutoRetryGroupInfo {
	private String groupId;
	private boolean unloadEnabled;
	private boolean loadEnabled;
	private int unloadCount;
	private int loadCount;
	private int unloadPauseTime;
	private int loadPauseTime;
	// 2012.09.20 by KYK
	private boolean lastUnloadErrorEnabled;
	private boolean lastLoadErrorEnabled;

	public boolean isLastUnloadErrorEnabled() {
		return lastUnloadErrorEnabled;
	}
	public void setLastUnloadErrorEnabled(boolean lastUnloadErrorEnabled) {
		this.lastUnloadErrorEnabled = lastUnloadErrorEnabled;
	}
	public boolean isLastLoadErrorEnabled() {
		return lastLoadErrorEnabled;
	}
	public void setLastLoadErrorEnabled(boolean lastLoadErrorEnabled) {
		this.lastLoadErrorEnabled = lastLoadErrorEnabled;
	}
	
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public boolean isUnloadEnabled() {
		return unloadEnabled;
	}
	public void setUnloadEnabled(boolean unloadEnabled) {
		this.unloadEnabled = unloadEnabled;
	}
	public boolean isLoadEnabled() {
		return loadEnabled;
	}
	public void setLoadEnabled(boolean loadEnabled) {
		this.loadEnabled = loadEnabled;
	}
	public int getUnloadCount() {
		return unloadCount;
	}
	public void setUnloadCount(int unloadCount) {
		this.unloadCount = checkValidRetryCount(unloadCount);
	}
	public int getLoadCount() {
		return loadCount;
	}
	public void setLoadCount(int loadCount) {
		this.loadCount = checkValidRetryCount(loadCount);
	}
	public int getUnloadPauseTime() {
		return unloadPauseTime;
	}
	public void setUnloadPauseTime(int pauseTime) {
		this.unloadPauseTime =  checkValidPauseTime(pauseTime);
	}
	public int getLoadPauseTime() {
		return loadPauseTime;
	}
	public void setLoadPauseTime(int pauseTime) {
		this.loadPauseTime = checkValidPauseTime(pauseTime);
	}
	private int checkValidRetryCount(int retryCount) {
		if (retryCount < 0) retryCount = 0;
		else if (retryCount > 3) retryCount = 3;
		return retryCount;
	}
	private int checkValidPauseTime(int pauseTime) {
		if (pauseTime < 0) pauseTime = 0;
		else if (pauseTime > 60) pauseTime = 60;
		return pauseTime * 1000;
	}
}
