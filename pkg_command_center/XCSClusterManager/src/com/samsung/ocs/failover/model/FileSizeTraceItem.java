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

import java.util.List;

/**
 * Ό³Έν
 * 
 * @author LWG
 * @date   2014. 6. 3.
 * @version 3.0
 */
public class FileSizeTraceItem {
	private String processName;
	private List<FileInfoItem> fileInfoList = null;
	private long fileSize = -1;
	
	public FileSizeTraceItem(String processName) {
		this.processName = processName;
	}

	public String getProcessName() {
		return processName;
	}

	public List<FileInfoItem> getFileInfoList() {
		return fileInfoList;
	}

	public void setFileInfoList(List<FileInfoItem> fileInfoList) {
		this.fileInfoList = fileInfoList;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

}
