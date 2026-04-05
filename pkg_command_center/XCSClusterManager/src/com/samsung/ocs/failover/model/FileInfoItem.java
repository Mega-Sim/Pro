/**
 * Copyright 2014 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */
package com.samsung.ocs.failover.model;


/**
 * Ό³Έν
 * 
 * @author LWG
 * @date   2014. 6. 3.
 * @version 3.0
 */
public class FileInfoItem {

	private String filePath;
	private String filePrefix;
	
	public FileInfoItem(String filePath, String filePrefix) {
		this.filePath = filePath;
		this.filePrefix = filePrefix;
	}
	
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getFilePrefix() {
		return filePrefix;
	}
	public void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}
	
	
}
