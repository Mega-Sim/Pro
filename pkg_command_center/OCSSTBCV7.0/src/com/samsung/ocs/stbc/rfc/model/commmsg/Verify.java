package com.samsung.ocs.stbc.rfc.model.commmsg;

import com.samsung.ocs.stbc.rfc.constant.RfcConstant.READ_RESULT;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.VERIFY_RESULT;

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

public class Verify extends NormalCommResult {
	/**
	 * Constructor of Verify class.
	 * 
	 * @param recieveMessage
	 */
	public Verify(byte[] recieveMessage) {
		super(recieveMessage);
	}

	//265
	private byte getStbNumberByte() {
		return recieveMessage[269];
	}
	
	public byte getCarrierDetectByte() {
		return recieveMessage[271];
	}
	
	public byte getVerifyResultByte() {
		return recieveMessage[272];
	}
	
	public byte getReadResultByte() {
		return recieveMessage[273];
	}
	
	public byte[] getIdDataBytes() {
		byte[] b = new byte[64];
		System.arraycopy(recieveMessage, 275, b, 0, 64);
		return b;
	}
	
	public int getStbNumber() {
		return getInt(getStbNumberByte());
	}
	
	public boolean isDetectedCarrier() {
		if (getCarrierDetectByte() == (byte)1) {
			return true;
		} else {
			return false;
		}
	}
	
	public READ_RESULT getReadResult() {
		return READ_RESULT.toReadRsult(getReadResultByte());
	}
	
	public VERIFY_RESULT getVerifyResult() {
		return VERIFY_RESULT.toVerifyRsult(getVerifyResultByte());
	}
	
	public String getIdData() {
		return new String(getIdDataBytes()).trim();
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.stbc.rfc.model.commmsg.NormalCommResult#getKey()
	 */
	@Override
	public String getKey() {
		return String.format("%s[%s:%d]", getMachineId(), getCommType().toString(), getStbNumber() );
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
		sb.append(" STBNO [").append(getStbNumber()).append("] CARRIERDETECT[").append(getCarrierDetectByte()).append("] VERIFYRESULT[").append(getVerifyResultByte()).append("] READRESULT[").append(getReadResultByte()).append("] IDDATA[").append(getIdData()).append("]");
		sb.append("\n");
		for (int i = 8; i < recieveMessage.length; i++) {
			sb.append(String.format("%2x", (byte)recieveMessage[i]));
		}
		return sb.toString();
	}
}
