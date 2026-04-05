package com.samsung.ocs.longrun.model;

public class PreviousVibrationRequest {
	private int index = 0;
	private String nodeId = "";
	private boolean isCostSearchChecked = false;
	
	public PreviousVibrationRequest(String nodeId) {
		this.index = 0;
		this.nodeId = nodeId;
	}
	
	public PreviousVibrationRequest(int index, String nodeId) {
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
	public void setNodeId(String tourId) {
		this.nodeId = tourId;
	}
	public boolean isCostSearchChecked() {
		return isCostSearchChecked;
	}
	public void setCostSearchChecked(boolean isCostSearchChecked) {
		this.isCostSearchChecked = isCostSearchChecked;
	}
}
