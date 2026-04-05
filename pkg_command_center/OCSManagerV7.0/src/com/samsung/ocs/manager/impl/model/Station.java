package com.samsung.ocs.manager.impl.model;

/**
 * Alarm Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2013. 2. 1.
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

public class Station {

	private String stationId;
	private int tagType;
	private Node parentNode;
	private Node nextNode;
	private int offset;

	public String getStationId() {
		return stationId;
	}
	public void setStationId(String stationId) {
		this.stationId = stationId;
	}
	public int getTagType() {
		return tagType;
	}
	public void setTagType(int tagType) {
		this.tagType = tagType;
	}
	public String getParentNodeId() {
		if (parentNode != null) {
			return parentNode.getNodeId();
		}
		return "";
	}
	public void setParentNode(Node parentNode) {
		this.parentNode = parentNode;
	}
	public String getNextNodeId() {
		if (nextNode != null) {
			return nextNode.getNodeId();
		}
		return "";
	}
	public void setNextNode(Node nextNode) {
		this.nextNode = nextNode;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String toString() {
		return stationId;
	}
	public Node getParentNode() {
		return parentNode;
	}
	public Node getNextNode() {
		return nextNode;
	}

}
