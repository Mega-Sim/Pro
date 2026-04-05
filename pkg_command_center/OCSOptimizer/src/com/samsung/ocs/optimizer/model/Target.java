package com.samsung.ocs.optimizer.model;

import com.samsung.ocs.manager.impl.model.Area;

public class Target {
	private Area area;
	private String nodeId;
	
	public Target(Area area, String nodeId){
		this.area = area;
		this.nodeId = nodeId;
	}
	public Area getArea() {
		return area;
	}
	public void setArea(Area area) {
		this.area = area;
	}
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
}
