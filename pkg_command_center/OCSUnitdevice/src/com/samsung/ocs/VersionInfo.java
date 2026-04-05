/**
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
 * Unitdevice의 버전을 가지는 클래스
 * 
 * @author LWG
 * @date   2011. 6. 28.
 * @version 3.0
 */
public class VersionInfo {
	
	private static HashMap<String,String> map = new HashMap<String,String>();
	private final static String MODULE_NAME = "OCSUNITDEVICE";
	private final static String BUILD_DATE = "20200602";
	private final static String VERSION_NUMBER = "3.7.0.1";
    
    static {
        map.put("BUILDID", MODULE_NAME + "_" + BUILD_DATE);
        map.put("VERSION", VERSION_NUMBER);
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
