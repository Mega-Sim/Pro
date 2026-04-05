package com.samsung.ocs.stbc.rfc.model;

/**
 * STBData Class, OCS 3.0 for Unified FAB
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

public class STBData {
	private RFCData owner;
	private String carrierLocId;
	private int rfcIndex;
	private int ready;
	private int carrierSensor;
	private int stbHomeSensor;
	private int ECAT1Conn;
	private int ECAT2Conn;
	private int carrierDetect;
	private int readResult;
	private int verifyResult;
	private String idData;
	private String foupIdData;
	private boolean enabled;
	
	/**
	 * Constructor of STBData class.
	 * 
	 * @param owner
	 */
	public STBData(RFCData owner) {
		this.owner = owner;
	}
	
	public RFCData getOwner() {
		return owner;
	}
	
	public String getCarrierLocId() {
		return carrierLocId;
	}
	
	public void setCarrierLocId(String carrierLocId) {
		this.carrierLocId = carrierLocId;
	}

	public int getRfcIndex() {
		return rfcIndex;
	}

	public void setRfcIndex(int rfcIndex) {
		this.rfcIndex = rfcIndex;
	}

	public String getIdData() {
		if (idData == null) {
			idData = "";
		}
		return idData;
	}

	public void setIdData(String idData) {
		this.idData = idData;
	}
	
	public String getFoupIdData() {
		if (foupIdData == null) {
			foupIdData = "";
		}
		return foupIdData;
	}

	public void setFoupIdData(String foupIdData) {
		this.foupIdData = foupIdData;
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setOwner(RFCData owner) {
		this.owner = owner;
	}

	public int getReady() {
		return ready;
	}

	public void setReady(int ready) {
		this.ready = ready;
	}

	public int getCarrierSensor() {
		return carrierSensor;
	}

	public void setCarrierSensor(int carrierSensor) {
		this.carrierSensor = carrierSensor;
	}

	public int getStbHomeSensor() {
		return stbHomeSensor;
	}

	public void setStbHomeSensor(int stbHomeSensor) {
		this.stbHomeSensor = stbHomeSensor;
	}

	public int getECAT1Conn() {
		return ECAT1Conn;
	}

	public void setECAT1Conn(int eCAT1Conn) {
		ECAT1Conn = eCAT1Conn;
	}

	public int getECAT2Conn() {
		return ECAT2Conn;
	}

	public void setECAT2Conn(int eCAT2Conn) {
		ECAT2Conn = eCAT2Conn;
	}

	public int getCarrierDetect() {
		return carrierDetect;
	}

	public void setCarrierDetect(int carrierDetect) {
		this.carrierDetect = carrierDetect;
	}

	public int getReadResult() {
		return readResult;
	}

	public void setReadResult(int readResult) {
		this.readResult = readResult;
	}

	public int getVerifyResult() {
		return verifyResult;
	}

	public void setVerifyResult(int verifyResult) {
		this.verifyResult = verifyResult;
	}
}
