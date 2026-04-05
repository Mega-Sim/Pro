package com.samsung.sem.ucominterface;

import SEComEnabler.SEComStructure.SECSID;

/**
 * UComItem Class, OCS 3.0 for Unified FAB
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

public class UComItem {

	/** SEComMsg °´Ã¼  */
	private SECSID secomItem;

	/**
	 * Constructor of UComItem class.
	 */
	public UComItem(SECSID item) {
		secomItem = item;
	}

	public String getTypeName(){
		return secomItem.getSECSFormat();
	}

	public int getU2Item() {
		return Integer.parseInt(secomItem.getValue());
	}

	public long getU4Item() {
//		long nReturn = 0;
		String sItem = secomItem.getValue();
		
//		if (sItem.equals("") == true)
//			nReturn = 0;
//		else
//			nReturn = Long.parseLong(secomItem.getValue());
		if ("".equals(sItem)) {
			return 0;
		} else {
			try {
				return Long.parseLong(secomItem.getValue());
			} catch (Exception e) {
				return 0;
			}
		}
	}
}
