package com.samsung.ocs.operation.model;

public class VehicleCommMapData {
	private int cmdId;
	private String stationId;
	private int stationType;
	private String firstNodeId;
	private int firstStationOffset;
	private String firstNextNodeId;
	private String secondNodeId;
	private int secondStationOffset;
	private String secondNextNodeId;

	public int getCmdId() {
		return cmdId;
	}
	public void setCmdId(int cmdId) {
		this.cmdId = cmdId;
	}
	public String getStationId() {
		return stationId;
	}
	public void setStationId(String stationId) {
		this.stationId = stationId;
	}
	public int getStationType() {
		return stationType;
	}
	public void setStationType(int stationType) {
		this.stationType = stationType;
	}
	public String getFirstNodeId() {
		return firstNodeId;
	}
	public void setFirstNodeId(String firstNodeId) {
		this.firstNodeId = firstNodeId;
	}
	public int getFirstStationOffset() {
		return firstStationOffset;
	}
	public void setFirstStationOffset(int firstStationOffset) {
		this.firstStationOffset = firstStationOffset;
	}
	public String getFirstNextNodeId() {
		return firstNextNodeId;
	}
	public void setFirstNextNodeId(String firstNextNodeId) {
		this.firstNextNodeId = firstNextNodeId;
	}
	public String getSecondNodeId() {
		return secondNodeId;
	}
	public void setSecondNodeId(String secondNodeId) {
		this.secondNodeId = secondNodeId;
	}
	public int getSecondStationOffset() {
		return secondStationOffset;
	}
	public void setSecondStationOffset(int secondStationOffset) {
		this.secondStationOffset = secondStationOffset;
	}
	public String getSecondNextNodeId() {
		return secondNextNodeId;
	}
	public void setSecondNextNodeId(String secondNextNodeId) {
		this.secondNextNodeId = secondNextNodeId;
	}
	public void reset() {
		cmdId = 0;
		stationId = "";
		stationType = 0;
		firstNodeId = "";
		firstStationOffset = 0;
		firstNextNodeId = "";
		secondNodeId = "";
		secondStationOffset = 0;
		secondNextNodeId = "";
	}
	public String toString() {
		StringBuilder message = new StringBuilder();
		message.append("StationId:").append(stationId).append("/");
		message.append("StationType:").append(stationType).append("/");
		message.append("NodeId1:").append(firstNodeId).append("/");
		message.append("Offset1:").append(firstStationOffset).append("/");
		message.append("NextNodeId1:").append(firstNextNodeId).append("/");
		message.append("NodeId2:").append(secondNodeId).append("/");
		message.append("Offset2:").append(secondStationOffset).append("/");
		message.append("NextNodeId2:").append(secondNextNodeId);
		return message.toString();
	}
}
