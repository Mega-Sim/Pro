package com.samsung.ocs.stbc.rfc.model.commmsg;

/**
 * Status Class, OCS 3.0 for Unified FAB
 * 
 * NormalComm¿« Status result
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

public class Status extends NormalCommResult {
	/**
	 * Constructor of Status class.
	 * 
	 * @param recieveMessage
	 */
	public Status(byte[] recieveMessage) {
		super(recieveMessage);
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.stbc.rfc.model.commmsg.NormalCommResult#getKey()
	 */
	/**
	 * Get Key
	 */
	@Override
	public String getKey() {
		return String.format("%s[%s:-1]", getMachineId(), getCommType().toString());
	}
}
