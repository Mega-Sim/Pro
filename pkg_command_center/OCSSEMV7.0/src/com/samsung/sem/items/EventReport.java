package com.samsung.sem.items;

import java.util.ArrayList;

/**
 * EventReport Class, OCS 3.0 for Unified FAB
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

public class EventReport {
	
	/*****************************************************************************
	 *  EventReport Class
	 *  - SetreportId(), SetEnabled(), SetVIDQty(), SetVID()
	 *  - GetreportId(), GetEnabled(), GetVIDQty(), GetVID()
	 *****************************************************************************/

	/* variables */
	private int reportId;
	private int vidQty;
	private boolean enabled;
	private ArrayList<Integer> vidList;

	/**
	 * Constructor of EventReport class.
	 */
	public EventReport(){		
		reportId = 0;
		vidQty = 0;
		enabled = true;
		vidList = new ArrayList<Integer>();
	}

	/* methods : getter, setter */	
	public boolean setReportId(int reportId){
		this.reportId = reportId;
		return true;
	}

	public boolean setEnabled(boolean enabled){
		this.enabled = enabled;
		return true;
	}

	public boolean setVidQty(int vidQty){
		this.vidQty = vidQty;
		return true;
	}

	// ??
	public boolean setVid(int i, int vid){
		vidList.add(i, vid);
		return true;
	}

	public int getReportId(){
		return reportId;
	}

	public boolean getEnabled(){
		return enabled;
	}

	public int getVidQty(){
		return vidQty;
	}

	public int getVid(int j) {
		return vidList.get(j);
	}
}
