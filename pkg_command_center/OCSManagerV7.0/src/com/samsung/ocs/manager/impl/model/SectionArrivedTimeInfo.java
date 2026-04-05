package com.samsung.ocs.manager.impl.model;

/**
 * SectionArrivedTimeInfo Class, OCS 3.0 for Unified FAB
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

public class SectionArrivedTimeInfo extends ArrivedTimeInfo {
	Section prevSection = null;

	public Section getPrevSection() {
		return prevSection;
	}
	public void setPrevSection(Section prevSection) {
		this.prevSection = prevSection;
	}	
}