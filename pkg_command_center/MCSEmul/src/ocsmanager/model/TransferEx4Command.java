/**
 * Copyright 2012 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */
package ocsmanager.model;

/**
 * Ό³Έν
 * 
 * @author LWG
 * @date 2012. 1. 25.
 * @version 3.0
 */
public class TransferEx4Command {

	private String trCmdID = "";
	private int priority = 30;
	private int replace = 0;
	private String carrierID = "";
	private String sourceLoc = "";
	private String destLoc = "";
	private String deliveryType = "PREMOVE";
	private int expectedDeliveryTime = 0;
	private int deliveryWaitTimeout = 0;

	public String getTrCmdID() {
		return trCmdID;
	}

	public void setTrCmdID(String trCmdID) {
		this.trCmdID = trCmdID;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getCarrierID() {
		return carrierID;
	}

	public void setCarrierID(String carrierID) {
		this.carrierID = carrierID;
	}

	public String getSourceLoc() {
		return sourceLoc;
	}

	public void setSourceLoc(String sourceLoc) {
		this.sourceLoc = sourceLoc;
	}

	public String getDestLoc() {
		return destLoc;
	}

	public void setDestLoc(String destLoc) {
		this.destLoc = destLoc;
	}

	public int getReplace() {
		return replace;
	}

	public void setReplace(int replace) {
		this.replace = replace;
	}

	public String getDeliveryType() {
		return deliveryType;
	}

	public void setDeliveryType(String deliveryType) {
		this.deliveryType = deliveryType;
	}

	public int getExpectedDeliveryTime() {
		return expectedDeliveryTime;
	}

	public void setExpectedDeliveryTime(int expectedDeliveryTime) {
		this.expectedDeliveryTime = expectedDeliveryTime;
	}

	public int getDeliveryWaitTimeout() {
		return deliveryWaitTimeout;
	}

	public void setDeliveryWaitTimeout(int deliveryWaitTimeout) {
		this.deliveryWaitTimeout = deliveryWaitTimeout;
	}

}
