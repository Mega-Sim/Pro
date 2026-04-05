package com.samsung.ocs.longrun.model;

import com.samsung.ocs.manager.impl.model.UserRequest;

public class VibrationRequest {
	UserRequest userRequest;
	String userRequestId = "";
	long overTime;
	long elapsedTime;
	
	public VibrationRequest(UserRequest userRequest, long overTime, long elapsedTime) {
		this.userRequest = userRequest;
		this.overTime = overTime;
		this.elapsedTime = elapsedTime;
		if (userRequest != null) {
			this.userRequestId = userRequest.getUserRequestId();
		} else {
			this.userRequestId = "";
		}
	}
	
	public UserRequest getUserRequest() {
		return userRequest;
	}
	public void setUserRequest(UserRequest userRequest) {
		this.userRequest = userRequest;
		if (userRequest != null) {
			this.userRequestId = userRequest.getUserRequestId();
		} else {
			this.userRequestId = "";
		}
	}
	public String getUserRequestId() {
		return userRequestId;
	}
	public long getOverTime() {
		return overTime;
	}
	public void setOverTime(long overTime) {
		this.overTime = overTime;
	}
	public long getElapsedTime() {
		return elapsedTime;
	}
	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
}
