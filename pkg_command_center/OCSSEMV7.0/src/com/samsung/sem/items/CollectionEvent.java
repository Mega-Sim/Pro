package com.samsung.sem.items;

import java.util.ArrayList;

/**
 * CollectionEvent Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date 2011. 6. 21.
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

public class CollectionEvent {
	/*****************************************************************************
	 *  CollectionEvent Class
	 *  - SetData(), SetReportData(), SetEnabled(), SetreportIdQty(), SetreportId(), SetLinkDefined()
	 *  - GetEventName(), GetCEID(), GetEnabled(), GetreportIdQty(), GetreportId(), GetLinkDefined()
	 *****************************************************************************/

	/* variables */
	private int collectionEventId;
	private String eventName;
	private int reportIdQty;
	private boolean enabled;
	private boolean isLinkDefined;
	private ArrayList<Integer> reportIdList;

	/** MAX_IBSEM_NAME_LEN */
	private int MAX_SEM_NAME_LEN = 100;

	/**
	 * Constructor of CollectionEvent class.
	 */
	public CollectionEvent() {

		collectionEventId = 0;
		reportIdQty = 0;
		enabled = false;
		isLinkDefined = false;
		reportIdList = new ArrayList<Integer>();		
	}

	/* methods : getter, setter */	
	public boolean setData(int collectionEventId, String eventName) {
		this.collectionEventId = collectionEventId;
		this.eventName = eventName;
		if (eventName.length() >= MAX_SEM_NAME_LEN) {
			return false;
		}
		return true;
	}

	public int getCollectionEventId() {
		return collectionEventId;
	}

	public void setCollectionEventId(int collectionEventId) {
		this.collectionEventId = collectionEventId;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public int getReportIdQty() {
		return reportIdQty;
	}

	public void setReportIdQty(int reportIdQty) {
		this.reportIdQty = reportIdQty;
	}

	public boolean isEnabled() {
		return enabled;
	}	

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isLinkDefined() {
		return isLinkDefined;
	}

	public void setLinkDefined(boolean isLinkDefined) {
		this.isLinkDefined = isLinkDefined;
	}

	public int getReportIdAt(int i) {
		return reportIdList.get(i);
	}

	public void setReportIdAt(int i, int reportId) {
		reportIdList.add(i, reportId);		
	}

	public ArrayList<Integer> getReportIdList() {
		return reportIdList;
	}

	public void setReportData(int count, ArrayList<Integer> reportIdList) {
		reportIdQty = count;		
		this.reportIdList = reportIdList;
	}
}
