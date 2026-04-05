package com.samsung.ocs.manager.impl.model;

/**
 * STBCarrierLoc Class, OCS 3.0 for Unified FAB
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

public class STBCarrierLoc {
	protected String carrierLocId;
	protected boolean enabled;
	protected String carrierId;
	protected String installedTime;
	protected String removedTime;
	protected String carrierState;
	protected String idReader;
	protected String commandName;
	protected String mcsCarrierId;
	protected String ocsCarrierId;
	protected String rfcId;
	protected String rfcIndex;
	protected String ready;
	protected String carrierSensor;
	protected String stbHomeSensor;
	protected String ecat1Conn;
	protected String ecat2Conn;
	protected String carrierDetect;
	protected String readResult;
	protected String verifyResult;
	protected String idData;
	protected String rfcReaderId;
	// 2012.09.10 by KYK
	protected int mismatchCount;
	// 2013.01.04 by KYK
	protected String reason;
	// 2014.01.02 by KBS : FoupID Ãß°¡ (for A-PJT EDS)
	protected String foupId;
	protected String mcsFoupId;

	public String getCarrierLocId() {
		return carrierLocId;
	}
	public void setCarrierLocId(String carrierLocId) {
		this.carrierLocId = carrierLocId;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getCarrierId() {
		return carrierId;
	}
	public void setCarrierId(String carrierId) {
		this.carrierId = carrierId;
	}
	public String getInstalledTime() {
		return installedTime;
	}
	public void setInstalledTime(String installedTime) {
		this.installedTime = installedTime;
	}
	public String getRemovedTime() {
		return removedTime;
	}
	public void setRemovedTime(String removedTime) {
		this.removedTime = removedTime;
	}
	public String getCarrierState() {
		return carrierState;
	}
	public void setCarrierState(String carrierState) {
		this.carrierState = carrierState;
	}
	public String getIdReader() {
		return idReader;
	}
	public void setIdReader(String idReader) {
		this.idReader = idReader;
	}
	public String getCommandName() {
		return commandName;
	}
	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}
	public String getMcsCarrierId() {
		return mcsCarrierId;
	}
	public void setMcsCarrierId(String mcsCarrierId) {
		this.mcsCarrierId = mcsCarrierId;
	}
	public String getOcsCarrierId() {
		return ocsCarrierId;
	}
	public void setOcsCarrierId(String ocsCarrierId) {
		this.ocsCarrierId = ocsCarrierId;
	}
	public String getRfcId() {
		return rfcId;
	}
	public void setRfcId(String rfcId) {
		this.rfcId = rfcId;
	}
	public String getRfcIndex() {
		return rfcIndex;
	}
	public void setRfcIndex(String rfcIndex) {
		this.rfcIndex = rfcIndex;
	}
	public String getReady() {
		return ready;
	}
	public void setReady(String ready) {
		this.ready = ready;
	}
	public String getCarrierSensor() {
		return carrierSensor;
	}
	public void setCarrierSensor(String carrierSensor) {
		this.carrierSensor = carrierSensor;
	}
	public String getStbHomeSensor() {
		return stbHomeSensor;
	}
	public void setStbHomeSensor(String stbHomeSensor) {
		this.stbHomeSensor = stbHomeSensor;
	}
	public String getEcat1Conn() {
		return ecat1Conn;
	}
	public void setEcat1Conn(String ecat1Conn) {
		this.ecat1Conn = ecat1Conn;
	}
	public String getEcat2Conn() {
		return ecat2Conn;
	}
	public void setEcat2Conn(String ecat2Conn) {
		this.ecat2Conn = ecat2Conn;
	}
	public String getCarrierDetect() {
		return carrierDetect;
	}
	public void setCarrierDetect(String carrierDetect) {
		this.carrierDetect = carrierDetect;
	}
	public String getReadResult() {
		return readResult;
	}
	public void setReadResult(String readResult) {
		this.readResult = readResult;
	}
	public String getVerifyResult() {
		return verifyResult;
	}
	public void setVerifyResult(String verifyResult) {
		this.verifyResult = verifyResult;
	}
	public String getIdData() {
		return idData;
	}
	public void setIdData(String idData) {
		this.idData = idData;
	}
	public String getRfcReaderId() {
		return rfcReaderId;
	}
	public void setRfcReaderId(String rfcReaderId) {
		this.rfcReaderId = rfcReaderId;
	}
	// 2012.09.10 by KYK
	public int getMismatchCount() {
		return mismatchCount;
	}
	public int setMismatchCount(int mismatchCount) {
		return this.mismatchCount = mismatchCount;
	}
	// 2013.01.04 by KYK
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public String getFoupId() {
		return foupId;
	}
	
	public void setFoupId(String foupId) {
		this.foupId = foupId;
	}
	
	public String getMcsFoupId() {
		return mcsFoupId;
	}
	
	public void setMcsFoupId(String mcsFoupId) {
		this.mcsFoupId = mcsFoupId;
	}
	
}
