package com.samsung.ocs.manager;

import java.util.HashMap;

/**
 * VersionInfo Class, OCS 3.0 for Unified FAB
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

public class VersionInfo {
	private static HashMap<String, String> map = new HashMap<String, String>();
	private static final String BUILDID = "BUILDID";
	private static final String VERSION = "VERSION";
    
    static {
    	map.put("BUILDID", "OCSMANAGER_20230220");
        map.put("VERSION", "3.7.4.0");
    }

    /**
	 * Constructor of VersionInfo class.
	 */
	private VersionInfo() {
		
	}

	/**
	 * 
	 * @param key String
	 * @return versionInfo String
	 */
	public static String getString(String key) {
		return map.get(key.toUpperCase()).toString();
	}
	
	/**
	 * main method.
	 * display Version, Includes, BuildId
	 */
	public static void main(String[] args) {
		System.out.println("VERSION	: [" + map.get(VERSION) + "]");
		System.out.println("BUILDID	: [" + map.get(BUILDID) + "]");
    }
}
