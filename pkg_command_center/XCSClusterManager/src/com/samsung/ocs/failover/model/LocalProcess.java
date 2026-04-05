package com.samsung.ocs.failover.model;

import com.samsung.ocs.common.constant.OcsConstant.FAILOVER_POLICY;
import com.samsung.ocs.common.constant.OcsConstant.WATCHDOC_POLICY;

/**
 * LocalProcess Class, OCS 3.0 for Unified FAB
 * 
 * Local Process에 관련된 Config를 저장하는 Bean객체
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

public class LocalProcess {
	private String processName;
	private boolean failoverUse = false;
	private boolean watchdogRunControlUse = true;
	private boolean publicNetCheckUse = false;
	private boolean localNetCheckUse = false;
	private WATCHDOC_POLICY watchdogPolicy = WATCHDOC_POLICY.ALWAYS_DOWN;
	private FAILOVER_POLICY policy = FAILOVER_POLICY.NETDBBASE;
	
	/**
	 * Constructor of LocalProcess class.
	 */
	public LocalProcess(String processName) {
		this.processName = processName;
	}

	/**
	 * processName getter
	 * @return String processName
	 */
	public String getProcessName() {
		return processName;
	}

	/**
	 * failoverUse getter
	 * @return boolean failoverUse
	 */
	public boolean isFailoverUse() {
		return failoverUse;
	}
	
	/**
	 * failoverUse setter
	 * @param failoverUse
	 */
	public void setFailoverUse(boolean failoverUse) {
		this.failoverUse = failoverUse;
	}

	/**
	 * watchdogRunControlUse getter
	 * @return boolean watchdogRunControlUse
	 */
	public boolean isWatchdogRunControlUse() {
		return watchdogRunControlUse;
	}

	/**
	 * watchdogRunControlUse setter
	 * @param watchdogRunControlUse
	 */
	public void setWatchdogRunControlUse(boolean watchdogRunControlUse) {
		this.watchdogRunControlUse = watchdogRunControlUse;
	}
	
	/**
	 * policy getter
	 * @return FAILOVER_POLICY
	 */
	public FAILOVER_POLICY getPolicy() {
		return policy;
	}
	
	/**
	 * policy setter
	 * @param policy
	 */
	public void setPolicy(FAILOVER_POLICY policy) {
		this.policy = policy;
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
	 * watchdogPolicy getter
	 * @return
	 */
	public WATCHDOC_POLICY getWatchdogPolicy() {
		return watchdogPolicy;
	}
	
	/**
	 * watchdogPolicy setter
	 * @param watchdogPolicy
	 */
	public void setWatchdogPolicy(WATCHDOC_POLICY watchdogPolicy) {
		this.watchdogPolicy = watchdogPolicy;
	}

	/**
	 * 
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[").append(processName).append("] failOverUse[").append(failoverUse).append("] watchdogRunControl[").append(watchdogRunControlUse).append("] watchdogPolicy[").append(watchdogPolicy).append("]\n");
		sb.append("  policy [").append(policy).append("] publicNetCheckUse [").append(publicNetCheckUse).append("] localNetCheckUse [").append(localNetCheckUse).append("]");
		return sb.toString();
	}
}
