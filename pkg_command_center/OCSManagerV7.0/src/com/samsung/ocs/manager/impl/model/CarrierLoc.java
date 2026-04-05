package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.OcsConstant.CARRIERLOC_TYPE;

/**
 * CarrierLoc Class, OCS 3.0 for Unified FAB
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

public class CarrierLoc {
	protected String carrierLocId;
	protected CARRIERLOC_TYPE type;
	protected String owner;
	protected String node;
	protected boolean enabled;
	protected long left;
	protected long top;
	protected String engineerInfo;
	protected String userGroupId;
	protected String localGroupId;
	protected String material;
	protected int materialIndex;
	protected boolean multiPort;
	// 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
	protected String autoRetryGroupId;
	protected String userRequest;	// 2015.02.04 by MYM : 장애 지역 우회 기능 (PortInService/PortOutOfservice 대응)
	
	// 2013.02.01 by KYK 
	protected String stationId;
	protected int hoistPosition;
	protected int shiftPosition;
	protected int rotatePosition;
	protected int pioDirection;
	
	protected int pioTimeLevel;
	protected int lookDownLevel;
	protected int extraOption;
	protected int subType;
	// 2013.11.12 by KYK
	protected boolean isValid;
	// 2014.10.30 by KYK [DualOHT]
	private String rfPioId;
	private int rfPioCS;
	
	public boolean isValid() {
		return isValid;
	}
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}
	
	public String getCarrierLocId() {
		return carrierLocId;
	}
	public void setCarrierLocId(String carrierLocId) {
		this.carrierLocId = carrierLocId;
	}
	public CARRIERLOC_TYPE getType() {
		return type;
	}
	public void setType(CARRIERLOC_TYPE type) {
		this.type = type;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getNode() {
		return node;
	}
	public void setNode(String node) {
		this.node = node;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public long getLeft() {
		return left;
	}
	public void setLeft(long left) {
		this.left = left;
	}
	public long getTop() {
		return top;
	}
	public void setTop(long top) {
		this.top = top;
	}
	public String getEngineerInfo() {
		return engineerInfo;
	}
	public void setEngineerInfo(String engineerInfo) {
		this.engineerInfo = engineerInfo;
	}
	public String getUserGroupId() {
		return userGroupId;
	}
	public void setUserGroupId(String userGroupId) {
		this.userGroupId = userGroupId;
	}
	public String getLocalGroupId() {
		return localGroupId;
	}
	public void setLocalGroupId(String localGroupId) {
		this.localGroupId = localGroupId;
	}
	public String getMaterial() {
		return material;
	}
	public void setMaterial(String material) {
		this.material = material;
	}
	public int getMaterialIndex() {
		return materialIndex;
	}
	public void setMaterialIndex(int materialIndex) {
		this.materialIndex = materialIndex;
	}

	public boolean isMultiPort() {
		// 2013.04.24 by MYM : PortOption으로 MultiPort 확인하도록 변경
//		if (subType == 2) {
//			return true;
//		}
//		return false;
		
		// 2016.08.02 by KBS : V1 Protocol 사용의 경우 MultiPort 정보 사용
		return multiPort;
	}

	public void setMultiPort(boolean multiPort) {
		this.multiPort = multiPort;
	}

	/**
	 * // 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
	 * @return
	 */
	public String getAutoRetryGroupId() {
		return autoRetryGroupId;
	}
	/**
	 * // 2012.08.21 by MYM : AutoRetry Port 그룹별 설정
	 * @param autoRetryGroupId
	 */
	public void setAutoRetryGroupId(String autoRetryGroupId) {
		this.autoRetryGroupId = autoRetryGroupId;
	}
	public String getUserRequest() {
		return userRequest;
	}
	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
	}
	// 2013.02.01 by KYK
	public String getStationId() {
		return stationId;
	}
	public void setStationId(String stationId) {
		this.stationId = stationId;
	}
	public int getHoistPosition() {
		return hoistPosition;
	}
	public void setHoistPosition(int hoistPosition) {
		this.hoistPosition = hoistPosition;
	}
	public int getShiftPosition() {
		return shiftPosition;
	}
	public void setShiftPosition(int shiftPosition) {
		this.shiftPosition = shiftPosition;
	}
	public int getRotatePosition() {
		return rotatePosition;
	}
	public void setRotatePosition(int rotatePosition) {
		this.rotatePosition = rotatePosition;
	}
	public int getPioDirection() {
		return pioDirection;
	}
	public void setPioDirection(int pioDirection) {
		this.pioDirection = pioDirection;
	}
	public int getPioTimeLevel() {
		return pioTimeLevel;
	}
	public void setPioTimeLevel(int pioTimeLevel) {
		this.pioTimeLevel = pioTimeLevel;
		// 2013.09.11 by KYK
		if (this.pioTimeLevel > 10) {
			this.pioTimeLevel = 10;
		} else if (this.pioTimeLevel < 1) {
			this.pioTimeLevel = 1;
		}
	}
	public int getLookDownLevel() {
		return lookDownLevel;
	}
	public void setLookDownLevel(int lookDownLevel) {
		this.lookDownLevel = lookDownLevel;
		if (this.lookDownLevel > 15) {
			this.lookDownLevel = 15;
		} else if (this.lookDownLevel < 1) {
			this.lookDownLevel = 1;
		}
	}
	public int getExtraOption() {
		return extraOption;
	}
	public void setExtraOption(int extraOption) {
		this.extraOption = extraOption;
	}
	public int getSubType() {
		return subType;
	}
	public void setSubType(int subType) {
//		0x01 : EQ 작업
//		0x02 : Multi EQ 작업
//		0x03 : Stocker 작업
//		0x04 : Loader 작업
//		0x05 : Left STB 작업
//		0x06 : Right STB 작업
//		0x07 : UTB 작업
		this.subType = subType;
	}
	
	/**
	 * 2014.10.30 by KYK [DualOHT]
	 * @return
	 */
	public String getRfPioId() {
		return rfPioId;
	}
	public void setRfPioId(String rfPioId) {
		this.rfPioId = rfPioId;
	}
	public int getRfPioCS() {
		return rfPioCS;
	}
	public void setRfPioCS(int rfPioCS) {
		this.rfPioCS = rfPioCS;
	}	
}
