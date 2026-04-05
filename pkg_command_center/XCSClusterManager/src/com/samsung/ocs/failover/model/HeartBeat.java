package com.samsung.ocs.failover.model;

import com.samsung.ocs.failover.constant.ClusterConstant.HEARTBEATTYPE;

/**
 * HeartBeat Class, OCS 3.0 for Unified FAB
 * 
 * HeartBeat에 관련된 Config를 저장하는 Bean객체
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

public class HeartBeat {
	private String ipAddress;
	private int port = 0;
	private boolean reported = false;
	private long lastReportedTime = 0;
	private String lastReportedString = "";
	private HEARTBEATTYPE type = HEARTBEATTYPE.DIRECT;
	/**
	 * Constructor of HeartBeat class.
	 */
	public HeartBeat() {
	}
	
	/**
	 * Constructor of HeartBeat class.
	 * 
	 * @param ipAddress
	 * @param port
	 */
	public HeartBeat(String ipAddress, int port) {
		this.ipAddress = ipAddress;
		this.port = port;
	}
	
	/**
	 * ipAddress getter
	 * @return String ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}
	/**
	 * ipAddress setter
	 * @param ipAddress
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	/**
	 * port getter
	 * @return int port
	 */
	public int getPort() {
		return port;
	}
	/**
	 * port setter
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}
	/**
	 * reported getter
	 * @return boolean reported
	 */
	public boolean isReported() {
		return reported;
	}
	/**
	 * reported setter
	 * @param reported
	 */
	public synchronized void setReported(boolean reported) {
		this.reported = reported;
	}
	/**
	 * lastReportedTime getter
 	 * @return long lastReportedTime
	 */
	public long getLastReportedTime() {
		return lastReportedTime;
	}
	/**
	 * lastReportedTime setter
	 * @param lastReportedTime
	 */
	public void setLastReportedTime(long lastReportedTime) {
		this.lastReportedTime = lastReportedTime;
	}
	/**
	 * lastReportedString getter
	 * @return String lastReportedString 
	 */
	public String getLastReportedString() {
		return lastReportedString;
	}
	/**
	 * lastReportedString setter
	 * @param lastReportedString
	 */
	public void setLastReportedString(String lastReportedString) {
		this.lastReportedString = lastReportedString;
	}
	
	public HEARTBEATTYPE getType() {
		return type;
	}

	public void setType(HEARTBEATTYPE type) {
		this.type = type;
	}

	/**
	 * 
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("ipAddress [").append(ipAddress).append("] port [").append(port).append("] reported [").append(reported).append("]");
		sb.append(" lastReportedTime [").append(lastReportedTime).append("] lastReportedString [").append(lastReportedString).append("] type [").append(type.toString()).append("]");
		return sb.toString();
	}
	
	
}
