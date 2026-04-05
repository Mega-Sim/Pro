package com.samsung.ocs.longrun.model;

public class Path {
	private String startNodeId = "";
	private String endNodeId = "";
	
	public Path(String startNodeId, String endNodeId) {
		this.startNodeId = startNodeId;
		this.endNodeId = endNodeId;
	}
	
	public String getStartNodeId() {
		return startNodeId;
	}
	public void setStartNodeId(String startNodeId) {
		this.startNodeId = startNodeId;
	}
	public String getEndNodeId() {
		return endNodeId;
	}
	public void setEndNodeId(String endNodeId) {
		this.endNodeId = endNodeId;
	}
}
