/**
 * VersionInfo Class, OCS 2.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2012. 9. 11.
 * @version 2.0
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

package com.samsung.ocs;

import java.util.HashMap;

/**
 * 
 * UCInterface의 버전을 가지는 클래스
 * 
 * @author 
 * @date   2012. 9. 11.
 * @version 2.1
 */
public class VersionInfo {
	private static HashMap<String,String> map = new HashMap<String,String>();

	static {
		map.put("BUILDID", "OCSUCINTERFACE_20220513");
		map.put("VERSION", "3.7.1.0");
	}

	private VersionInfo() {
		
	}

	public static String getString(String key) {
		return map.get(key.toUpperCase()).toString();
	}
	
	public static void main(String[] args) {
		System.out.println("VERSION	: [" + map.get("VERSION") + "]");
		System.out.println("BUILDID	: [" + map.get("BUILDID") + "]");
    }
}
