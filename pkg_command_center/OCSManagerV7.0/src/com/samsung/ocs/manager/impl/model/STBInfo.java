package com.samsung.ocs.manager.impl.model;

/**
 * STBInfo Class, OCS 3.0 for Unified FAB
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

public class STBInfo {
	private String name;
	private String value;
	
	/**
	 * Constructor of STBInfo class.
	 */
	public STBInfo() {
	}
	
	/**
	 * Constructor of STBInfo class.
	 */
	public STBInfo(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}
