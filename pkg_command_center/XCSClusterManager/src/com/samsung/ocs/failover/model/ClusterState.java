package com.samsung.ocs.failover.model;

import com.samsung.ocs.common.constant.OcsConstant.INIT_STATE;

/**
 * ClusterState Class, OCS 3.0 for Unified FAB
 * 
 * xCSClusterManager의 상태값을 저장하는 Bean객체
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

public class ClusterState {

	// FailOver 자체의 INIT state
	private INIT_STATE initState = INIT_STATE.INIT_BEGINED;
	
	// heart beat
	private long recentHeartBeatTime = 0;
	private boolean remoteDaemonAlive = true;
	private boolean remotePublicConnected = true;
	private boolean remoteLocalConnected = true;
	private boolean remoteDBConnected = true;
	private long recentPublicHeartBeatTime = 0;
	private long recentLocalHeartBeatTime = 0;
	private long recentDirectHeartBeatTime = 0;

	// public net check usage
	private boolean publicNetCheckUse = false;
	private boolean publicNetCheckFail = false;
	
	// local net check usage
	private boolean localNetCheckUse = false;
	private boolean localNetCheckFail = false;
	
	private boolean dbConnCheckFail = false;
	
	private boolean vipAssigned = false;
	
	private boolean publicNetCheckInitialized = false;
	private boolean localNetCheckInitialized = false;
	private boolean vipCheckInitialized = false;
	
	private boolean isRemoteInitialized = false;
	
	
	/**
	 * initState getter
	 * @return INIT_STATE initState
	 */
	public INIT_STATE getInitState() {
		return initState;
	}
	/**
	 * initState setter
	 * @param initState
	 */
	public void setInitState(INIT_STATE initState) {
		this.initState = initState;
	}
	/**
	 * publicNetCheckUse getter
	 * @return boolean publicNetCheckUse
	 */
	public boolean isPublicNetCheckUse() {
		return publicNetCheckUse;
	}
	/**
	 * publicNetCheckUse setter
	 * @param publicNetCheckUse
	 */
	public void setPublicNetCheckUse(boolean publicNetCheckUse) {
		this.publicNetCheckUse = publicNetCheckUse;
	}
	/**
	 * localNetCheckUse getter
	 * @return boolean localNetCheckUse
	 */
	public boolean isLocalNetCheckUse() {
		return localNetCheckUse;
	}
	/**
	 * localNetCheckUse setter
	 * @param localNetCheckUse
	 */
	public void setLocalNetCheckUse(boolean localNetCheckUse) {
		this.localNetCheckUse = localNetCheckUse;
	}
	/**
	 * publicNetCheckFail getter
	 * @return boolean publicNetCheckFail
	 */
	public boolean isPublicNetCheckFail() {
		return publicNetCheckFail;
	}
	/**
	 * publicNetCheckFail setter
	 * @param publicNetCheckFail
	 */
	public void setPublicNetCheckFail(boolean publicNetCheckFail) {
		this.publicNetCheckFail = publicNetCheckFail;
	}
	/**
	 * localNetCheckFail getter
	 * @return boolean localNetCheckFail
	 */
	public boolean isLocalNetCheckFail() {
		return localNetCheckFail;
	}
	/**
	 * localNetCheckFail setter
	 * @param localNetCheckFail
	 */
	public void setLocalNetCheckFail(boolean localNetCheckFail) {
		this.localNetCheckFail = localNetCheckFail;
	}
	/**
	 * recentHeartBeatTime getter
	 * @return long recentHeartBeatTime
	 */
	public long getRecentHeartBeatTime() {
		return recentHeartBeatTime;
	}
	/**
	 * recentHeartBeatTime setter
	 * @param recentHeartBeatTime
	 */
	public void setRecentHeartBeatTime(long recentHeartBeatTime) {
		this.recentHeartBeatTime = recentHeartBeatTime;
	}
	/**
	 * recentPublicHeartBeatTime getter
	 * @return long recentPublicHeartBeatTime
	 */
	public long getRecentPublicHeartBeatTime() {
		return recentPublicHeartBeatTime;
	}
	/**
	 * recentPublicHeartBeatTime setter
	 * @param recentPublicHeartBeatTime
	 */
	public void setRecentPublicHeartBeatTime(long recentPublicHeartBeatTime) {
		this.recentPublicHeartBeatTime = recentPublicHeartBeatTime;
	}
	/**
	 * recentLocalHeartBeatTime getter
	 * @return long recentLocalHeartBeatTime
	 */
	public long getRecentLocalHeartBeatTime() {
		return recentLocalHeartBeatTime;
	}
	/**
	 * recentLocalHeartBeatTime setter
	 * @param recentLocalHeartBeatTime
	 */
	public void setRecentLocalHeartBeatTime(long recentLocalHeartBeatTime) {
		this.recentLocalHeartBeatTime = recentLocalHeartBeatTime;
	}
	/**
	 * recentDirectHeartBeatTime getter
	 * @return long recentDirectHeartBeatTime
	 */
	public long getRecentDirectHeartBeatTime() {
		return recentDirectHeartBeatTime;
	}
	/**
	 * resentDirectHeartBeatTime setter
	 * @param resentDirectHeartBeatTime
	 */
	public void setRecentDirectHeartBeatTime(long recentDirectHeartBeatTime) {
		this.recentDirectHeartBeatTime = recentDirectHeartBeatTime;
	}
	/**
	 * remoteDaemonAlive getter
	 * @return boolean remoteDaemonAlive
	 */
	public boolean isRemoteDaemonAlive() {
		return remoteDaemonAlive;
	}
	/**
	 * remoteDaemonAlive setter
	 * @param remoteDaemonAlive
	 */
	public void setRemoteDaemonAlive(boolean remoteDaemonAlive) {
		this.remoteDaemonAlive = remoteDaemonAlive;
	}
	/**
	 * remotePublicConnected getter	
	 * @return boolean remotePublicConnected
	 */
	public boolean isRemotePublicConnected() {
		return remotePublicConnected;
	}
	/**
	 * remotePublicConnected setter
	 * @param remotePublicConnected
	 */
	public void setRemotePublicConnected(boolean remotePublicConnected) {
		this.remotePublicConnected = remotePublicConnected;
	}
	/**
	 * remoteLocalConnected getter 
	 * @return boolean remoteLocalConnected
	 */
	public boolean isRemoteLocalConnected() {
		return remoteLocalConnected;
	}
	/**
	 * remoteLocalConnected setter
	 * @param remoteLocalConnected
	 */
	public void setRemoteLocalConnected(boolean remoteLocalConnected) {
		this.remoteLocalConnected = remoteLocalConnected;
	}
	/**
	 * remoteDBConnected getter
	 * @return boolean remoteDBConnected
	 */
	public boolean isRemoteDBConnected() {
		return remoteDBConnected;
	}
	/**
	 * remoteDBConnected setter
	 * @param remoteDBConnected
	 */
	public void setRemoteDBConnected(boolean remoteDBConnected) {
		this.remoteDBConnected = remoteDBConnected;
	}
	/**
	 * dbConnCheckFail getter
	 * @return boolean dbConnCheckFail
	 */
	public boolean isDbConnCheckFail() {
		return dbConnCheckFail;
	}
	/**
	 * dbConnCheckFail setter
	 * @param dbConnCheckFail
	 */
	public void setDbConnCheckFail(boolean dbConnCheckFail) {
		this.dbConnCheckFail = dbConnCheckFail;
	}
	/**
	 * vipAssigned getter
	 * @return boolean vipAssigned
	 */
	public boolean isVipAssigned() {
		return vipAssigned;
	}
	
	/**
	 * vipAssigned setter
	 * @param vipAssigned
	 */
	public void setVipAssigned(boolean vipAssigned) {
		this.vipAssigned = vipAssigned;
	}
	
	public void setPublicNetCheckInitialized(boolean publicNetCheckInitialized) {
		this.publicNetCheckInitialized = publicNetCheckInitialized;
	}
	
	public void setLocalNetCheckInitialized(boolean localNetCheckInitialized) {
		this.localNetCheckInitialized = localNetCheckInitialized;
	}
	
	public void setVipCheckInitialized(boolean vipCheckInitialized) {
		this.vipCheckInitialized = vipCheckInitialized;
	}
	
	public boolean isCheckThreadInitialized() {
		return publicNetCheckInitialized && localNetCheckInitialized && vipCheckInitialized;
	}
	
	public boolean isVipCheckInitialized() {
		return vipCheckInitialized;
	}
	
	public boolean isRemoteInitialized() {
		return isRemoteInitialized;
	}
	
	public void setRemoteInitialized(boolean isRemoteInitialized) {
		this.isRemoteInitialized = isRemoteInitialized;
	}
}