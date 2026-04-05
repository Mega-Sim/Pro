package com.samsung.ocs.longrun.model;

public class PreviousMoveRequest {
	private int index = 0;
	private String nodeId = "";
	
	public PreviousMoveRequest(String nodeId) {
		this.index = 0;
		this.nodeId = nodeId;
	}
	public PreviousMoveRequest(int index, String nodeId) {
		this.index = index;
		this.nodeId = nodeId;
	}
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
}
