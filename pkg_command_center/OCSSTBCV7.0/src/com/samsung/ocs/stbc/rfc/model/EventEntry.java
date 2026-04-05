package com.samsung.ocs.stbc.rfc.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.samsung.ocs.stbc.rfc.constant.RfcConstant.EVENT_STATE;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.NORMAL_COMMTYPE;
import com.samsung.ocs.stbc.rfc.model.commmsg.NormalCommResult;

/**
 * EventEntry Class, OCS 3.0 for Unified FAB
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

public class EventEntry {
	
	// 시작시 REQUESTED, 
	// processing에 최초 들어가면 PROCESSING , 완료되면 COMPLETED , 재시도중이면 RETRYING
	// 처리하다가 응답이 안오면 TIMEOUT, READ에 한해 retryCount 사용
	private EVENT_STATE state = EVENT_STATE.REQUESTED;

	private long waitingStartTime = 0;
	private long processingStartTime = 0;
	private long completedTime = 0;
	private long canceledTime = 0;
	
	private int retryCount = 0;
	
	//request Info
	private String rfcId;
	private NORMAL_COMMTYPE eventType = NORMAL_COMMTYPE.UNKNOWN;
	private int stbNumber = -1;
	private boolean idRead = false; // 0 : use keeping data, 1 : id read
	private int timeOut;
	private boolean hasCarrier = false; // 0 : no carrier , 1 : one carrier
	
	//response map : 
	private int maxCount;
	private Map<String, NormalCommResult> responseMap;
	private String carrierlocId;
	
	// 2015.06.08 by KBS : IDREADLIST 후 응답없는 port 처리 개선
	private ArrayList<String> idReadPortList = null;
	
	/**
	 * Constructor of EventEntry class.
	 * 
	 * @param rfcId
	 * @param type
	 * @param stbNumber
	 * @param idRead
	 * @param timeOut
	 * @param hasCarrier
	 * @param carrierlocId
	 * @param idReadPortList
	 */
	public EventEntry(String rfcId, NORMAL_COMMTYPE type, int stbNumber, boolean idRead, int timeOut, boolean hasCarrier, String carrierlocId, ArrayList<String> idReadPortList) {
		this.rfcId = rfcId;
		this.eventType = type;
		this.idRead = idRead;
		this.timeOut = timeOut;
		this.hasCarrier = hasCarrier;
		this.stbNumber = stbNumber;
		this.waitingStartTime = System.currentTimeMillis();
		this.responseMap = new HashMap<String, NormalCommResult>();
		this.carrierlocId = carrierlocId;
		this.idReadPortList = idReadPortList;
	}

	public long getWaitingStartTime() {
		return waitingStartTime;
	}

	public void setWaitingStartTime(long waitingStartTime) {
		this.waitingStartTime = waitingStartTime;
	}

	public long getProcessingStartTime() {
		return processingStartTime;
	}

	public void setProcessingStartTime(long processingStartTime) {
		this.processingStartTime = processingStartTime;
	}

	public long getCompletedTime() {
		return completedTime;
	}

	public void setCompletedTime(long completedTime) {
		this.completedTime = completedTime;
	}

	public long getCanceledTime() {
		return canceledTime;
	}

	public void setCanceledTime(long canceledTime) {
		this.canceledTime = canceledTime;
	}

	public int getMaxCount() {
		return maxCount;
	}

	public void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}

	public String getRfcId() {
		return rfcId;
	}
	
	public String getRfcIdForMessage() {
		String result = "";
		if (rfcId.length() < 6) {
			result = rfcId;
			for (int i = 0; i < 6 - rfcId.length(); i++) {
				result += " ";
			}
		} else if (rfcId.length() > 6) {
			result = rfcId.substring(0, 6);
		} else {
			result = rfcId;
		}
		return result;
	}

	public NORMAL_COMMTYPE getEventType() {
		return eventType;
	}

	public int getTimeOut() {
		return timeOut;
	}

	public Map<String, NormalCommResult> getResponseMap() {
		return responseMap;
	}

	public int getStbNumber() {
		return stbNumber;
	}

	public boolean isIdRead() {
		return idRead;
	}

	public boolean hasCarrier() {
		return hasCarrier;
	}
	
	public String getKey() {
		return String.format("%s[%s:%d]", rfcId, eventType.toString(), stbNumber );
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getKey();
	}

	public EVENT_STATE getState() {
		return state;
	}

	public void setState(EVENT_STATE state) {
		this.state = state;
	}

	public int getRetryCount() {
		return retryCount;
	}
	
	public void increaseRetryCount() {
		retryCount++;
	}

	public String getCarrierlocId() {
		return carrierlocId;
	}

	public ArrayList<String> getIdReadPortList() {
		return idReadPortList;
	}

}
