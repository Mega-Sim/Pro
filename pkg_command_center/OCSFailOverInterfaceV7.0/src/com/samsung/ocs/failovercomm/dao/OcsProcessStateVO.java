package com.samsung.ocs.failovercomm.dao;

import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;

/**
 * OcsProcessStateVO Class, OCS 3.0 for Unified FAB
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

public class OcsProcessStateVO {
	private String inserviceHost;
	private MODULE_STATE hostState;
	private MODULE_STATE remoteState;
	public String getInserviceHost() {
		return inserviceHost;
	}
	public void setInserviceHost(String inserviceHost) {
		this.inserviceHost = inserviceHost;
	}
	public MODULE_STATE getHostState() {
		return hostState;
	}
	public void setHostState(MODULE_STATE hostState) {
		this.hostState = hostState;
	}
	public MODULE_STATE getRemoteState() {
		return remoteState;
	}
	public void setRemoteState(MODULE_STATE remoteState) {
		this.remoteState = remoteState;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("DB STATE : ");
		sb.append("inserviceHost [").append(inserviceHost).append("] hostState[" ).append(hostState).append("] remoteState [").append(remoteState).append("]");
		return sb.toString();
	}
}
