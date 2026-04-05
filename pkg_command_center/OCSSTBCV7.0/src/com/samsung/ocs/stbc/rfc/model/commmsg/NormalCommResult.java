package com.samsung.ocs.stbc.rfc.model.commmsg;

import com.samsung.ocs.stbc.rfc.constant.RfcConstant.NORMAL_COMMTYPE;

/**
 * NormalCommResult Abstract Class, OCS 3.0 for Unified FAB
 * 
 * NormalComm 관련 처리 클래스
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

public abstract class NormalCommResult {
	protected byte[] recieveMessage;
	private long recieveTime = 0;
	private String carrierLocId = null;

	/**
	 * Constructor of NormalCommResult class.
	 */
	public NormalCommResult(byte[] recieveMessage) {
		this.recieveMessage = recieveMessage;
		this.recieveTime = System.currentTimeMillis();
	}
	
	public String getMachineCode() {
		byte[] b = new byte[2];
		System.arraycopy(recieveMessage, 0, b, 0, 2);
		return new String(b).trim();
	}
	
	public String getMachineId() {
		byte[] b = new byte[6];
		System.arraycopy(recieveMessage, 2, b, 0, 6);
		return new String(b).trim();
	}
	
	public NORMAL_COMMTYPE getCommType() {
		return NORMAL_COMMTYPE.toCommType(getCommandId());
	}
	
	public byte getCommandId() {
		return recieveMessage[264];
	}
	
	public byte[] getStatus() {
		byte[] b = new byte[256];
		System.arraycopy(recieveMessage, 8, b, 0, 256);
		return b;
	}

	public long getRecieveTime() {
		return recieveTime;
	}

	public abstract String getKey();
	
	/**
	 * Get Log in String
	 * 
	 * @param sourceIpAddress
	 */
	public String toLogString(String sourceIpAddress) {
		StringBuffer sb = new StringBuffer();
		sb.append("RECV[FROM:").append(sourceIpAddress).append("] [").append(getCommType()).append("] ");
		sb.append(getMachineCode()).append(getMachineId()).append("\n");
		for (int i = 8; i < recieveMessage.length; i++) {
			sb.append(String.format("%2x", (byte)recieveMessage[i]));
		}
		return sb.toString();
	}

	public String getCarrierLocId() {
		return carrierLocId;
	}

	public void setCarrierLocId(String carrierLocId) {
		this.carrierLocId = carrierLocId;
	}
	
	// 2013.08.29 by LWG [byte max]
	public int getInt(byte b) {
		if(b<0) {
			return b+256;
		} else {
			return (int)b;
		}
	}
}
