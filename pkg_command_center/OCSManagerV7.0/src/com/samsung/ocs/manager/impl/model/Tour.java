package com.samsung.ocs.manager.impl.model;

import com.samsung.ocs.common.constant.OcsConstant.TOUR_TYPE;

/**
 * Tour Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 3. 7.
 * @version 3.0
 * 
 * Copyright 2013 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class Tour {
	protected String tourId;
	protected String typeString;
	protected TOUR_TYPE type;
	protected String tour;
	
	public String getTourId() {
		return tourId;
	}
	public void setTourId(String tourId) {
		this.tourId = tourId;
	}
	public String getTypeString() {
		return typeString;
	}
	public void setTypeString(String typeString) {
		this.typeString = typeString;
	}
	public TOUR_TYPE getType() {
		return type;
	}
	public void setType(TOUR_TYPE type) {
		this.type = type;
	}
	public String getTour() {
		return tour;
	}
	public void setTour(String tour) {
		this.tour = tour;
	}
}