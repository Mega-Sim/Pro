package com.samsung.ocs.failover.model;

import java.util.Date;

import com.samsung.ocs.common.constant.OcsConstant;

/**
 * OcsDBStateVO Class, OCS 3.0 for Unified FAB
 * 
 * ocs_cluster_state 테이블의 ROW에 해당하는 데이터를 가진 Value Object.
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

public class OcsDBStateVO {
	private String inserviceHost = "";
	private String primaryState = "";
	private String secondaryState = "";
	private String failoverAcceptance = OcsConstant.FALSE;
	private Date primaryUpdationTime = null;
	private Date secondaryUpdationTime = null;
	private Date lastUpdationTime = null; 
	private Date currentTime = null;
	private String userRequest = "";
	
	/**
	 * ocs_cluster_state 테이블의 INSERVICE_HOST 컬럼에 매칭되는 inserviceHost 값을 반환한다.
	 * @return String inserviceHost
	 */
	public String getInserviceHost() {
		return inserviceHost;
	}
	/**
	 * ocs_cluster_state 테이블의 INSERVICE_HOST 컬럼에 매칭되는 inserviceHost 값을 저장한다.
	 * @param inserviceHost
	 */
	public void setInserviceHost(String inserviceHost) {
		this.inserviceHost = inserviceHost;
	}
	/**
	 * ocs_cluster_state 테이블의 PRIMARY_STATE 컬럼에 매칭되는 primaryState 값을 반환한다.
	 * @return String primaryState
	 */
	public String getPrimaryState() {
		return primaryState;
	}
	/**
	 * ocs_cluster_state 테이블의 PRIMARY_STATE 컬럼에 매칭되는 primaryState 값을 저장한다.
	 * @param primaryState
	 */
	public void setPrimaryState(String primaryState) {
		this.primaryState = primaryState;
	}
	/**
	 * ocs_cluster_state 테이블의 SECONDARY_STATE 컬럼에 매칭되는 secondaryState 값을 반환한다.
	 * @return String secondaryState
	 */
	public String getSecondaryState() {
		return secondaryState;
	}
	/**
	 * ocs_cluster_state 테이블의 SECONDARY_STATE 컬럼에 매칭되는 secondaryState 값을 저장한다.
	 * @param secondaryState
	 */
	public void setSecondaryState(String secondaryState) {
		this.secondaryState = secondaryState;
	}
	/**
	 * ocs_cluster_state 테이블의 PRIMARY_UPDATION_TIME 컬럼에 매칭되는 primaryUpdationTime 값을 반환한다.
	 * @return Date primaryUpdationTime
	 */
	public Date getPrimaryUpdationTime() {
		return primaryUpdationTime;
	}
	/**
	 * ocs_cluster_state 테이블의 PRIMARY_UPDATION_TIME 컬럼에 매칭되는 primaryUpdationTime 값을 저장한다.
	 * @param primaryUpdationTime
	 */
	public void setPrimaryUpdationTime(Date primaryUpdationTime) {
		this.primaryUpdationTime = primaryUpdationTime;
	}
	/**
	 * ocs_cluster_state 테이블의 SECONDARY_UPDATION_TIME 컬럼에 매칭되는 secondaryUpdationTime 값을 반환한다.
	 * @return Date secondaryUpdationTime
	 */
	public Date getSecondaryUpdationTime() {
		return secondaryUpdationTime;
	}
	/**
	 * ocs_cluster_state 테이블의 SECONDARY_UPDATION_TIME 컬럼에 매칭되는 secondaryUpdationTime 값을 저장한다. 
	 * @param secondaryUpdationTime
	 */
	public void setSecondaryUpdationTime(Date secondaryUpdationTime) {
		this.secondaryUpdationTime = secondaryUpdationTime;
	}
	/**
	 * ocs_cluster_state 테이블의 LAST_UPDATION_TIME 컬럼에 매칭되는 lastUpdationTime 값을 반환한다.
	 * @deprecated
	 * @return Date lastUpdationTime
	 */
	public Date getLastUpdationTime() {
		return lastUpdationTime;
	}
	/**
	 * ocs_cluster_state 테이블의 LAST_UPDATION_TIME 컬럼에 매칭되는 lastUpdationTime 값을 저장한다.
	 * @deprecated
	 * @param lastUpdationTime
	 */
	public void setLastUpdationTime(Date lastUpdationTime) {
		this.lastUpdationTime = lastUpdationTime;
	}
	/**
	 * Database의 현재 시간에 해당하는 값에 매칭되는 currentTime 값을 반환한다. 
	 * @return Date currentTime
	 */
	public Date getCurrentTime() {
		return currentTime;
	}
	/**
	 * Database의 현재 시간에 해당하는 값에 매칭되는 currentTime 값을 저장한다.
	 * @param currentTime
	 */
	public void setCurrentTime(Date currentTime) {
		this.currentTime = currentTime;
	}
	/**
	 * ocs_cluster_state 테이블의 FAILOVER_ACCEPTANCE 컬럼에 매칭되는 failoverAcceptance 값을 반환한다.
	 * @return String failoverAcceptance
	 */
	public String getFailoverAcceptance() {
		return failoverAcceptance;
	}
	/**
	 * ocs_cluster_state 테이블의 FAILOVER_ACCEPTANCE 컬럼에 매칭되는 failoverAcceptance 값을 저장한다.
	 * @param failoverAcceptance
	 */
	public void setFailoverAcceptance(String failoverAcceptance) {
		this.failoverAcceptance = failoverAcceptance;
	}
	/**
	 * ocs_cluster_state 테이블의 USER_REQUEST 컬럼에 매칭되는 userRequest 값을 반환한다.
	 * @return
	 */
	public String getUserRequest() {
		return userRequest;
	}
	/**
	 * ocs_cluster_state 테이블의 USER_REQUEST 컬럼에 매칭되는 userRequest 값을 저장한다.
	 * @param userRequest
	 */
	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
	}
}
