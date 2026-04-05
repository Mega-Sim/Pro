package com.samsung.ocs.stbc.rfc.model.commmsg;

import com.samsung.ocs.stbc.rfc.constant.RfcConstant.NORMAL_COMMTYPE;

/**
 * Verify Class, OCS 3.0 for Unified FAB
 * 
 * NormalComm¿« verify result
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

public class Verify2 extends Verify {
	/**
	 * Constructor of Verify2 class.
	 * 
	 * @param recieveMessage
	 */
	public Verify2(byte[] recieveMessage) {
		super(recieveMessage);
	}

	public byte[] getFoupIdDataBytes() {
		byte[] b = new byte[64];
		System.arraycopy(recieveMessage, 339, b, 0, 64);
		return b;
	}
	
	public String getFoupIdData() {
		return new String(getFoupIdDataBytes()).trim();
	}
	
	/**
	 * Get Key
	 */
	@Override
	public String getKey() {
		return String.format("%s[%s:%d]", getMachineId(), NORMAL_COMMTYPE.VERIFY, getStbNumber() );
	}
	
	/**
	 * Get Log in String
	 * 
	 * @param sourceIpAddress
	 */
	public String toLogString(String sourceIpAddress) {
		StringBuffer sb = new StringBuffer();
		sb.append("RECV[FROM:").append(sourceIpAddress).append("] [").append(getCommType()).append("] ");
		sb.append("[").append(getMachineCode()).append("][").append(getMachineId()).append("]");
		sb.append(" STBNO [").append(getStbNumber()).append("] CARRIERDETECT[").append(getCarrierDetectByte()).append("] VERIFYRESULT[").append(getVerifyResultByte()).append("] READRESULT[").append(getReadResultByte()).append("] IDDATA[").append(getIdData()).append("] FOUPIDDATA[").append(getFoupIdData()).append("]");
		sb.append("\n");
		for (int i = 8; i < recieveMessage.length; i++) {
			sb.append(String.format("%2x", (byte)recieveMessage[i]));
		}
		return sb.toString();
	}
}
