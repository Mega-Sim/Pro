package com.samsung.ocs.operation.model;

public class VehicleCommTeachingData {
	private int cmdId;
	private String nodeId;
	private String stationId;
	private int portType;
	private int pioDirection;
	private int hoistPosition;
	private int shiftPosition;
	private int rotatePosition;
	private int pioTimeLevel;
	private int lookDownLevel;
	
	public int getCmdId() {
		return cmdId;
	}
	public void setCmdId(int cmdId) {
		this.cmdId = cmdId;
	}
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public String getStationId() {
		return stationId;
	}
	public void setStationId(String stationId) {
		this.stationId = stationId;
	}
	public int getPortType() {
		return portType;
	}
	public void setPortType(int portType) {
		this.portType = portType;
	}
	public int getPIODirection() {
		return pioDirection;
	}
	public void setPIODirection(int pioDirection) {
		this.pioDirection = pioDirection;
	}
	public int getHoistPosition() {
		return hoistPosition;
	}
	public void setHoistPosition(int hoistPosition) {
		this.hoistPosition = hoistPosition;
	}
	public int getShiftPosition() {
		return shiftPosition;
	}
	public void setShiftPosition(int shiftPosition) {
		this.shiftPosition = shiftPosition;
	}
	public int getRotatePosition() {
		return rotatePosition;
	}
	public void setRotatePosition(int rotatePosition) {
		this.rotatePosition = rotatePosition;
	}
	public int getPIOTimeLevel() {
		return pioTimeLevel;
	}
	public void setPIOTimeLevel(int pioTimeLevel) {
		this.pioTimeLevel = pioTimeLevel;
	}
	public int getLookDownLevel() {
		return lookDownLevel;
	}
	public void setLookDownLevel(int lookDownLevel) {
		this.lookDownLevel = lookDownLevel;
	}
	public void reset() {
		cmdId = 0;
		stationId = "";
		portType = 0;
		pioDirection = 0;
		hoistPosition = 0;
		shiftPosition = 0;
		rotatePosition = 0;
		pioTimeLevel = 0;
		lookDownLevel = 0;
	}
	public String toString() {
		StringBuilder log = new StringBuilder();
		log.append("StationId:").append(stationId).append("/");
		log.append("PortType:").append(portType).append("/");
		log.append("PIODir:").append(pioDirection).append("/");
		log.append("PIOTimeLevel:").append(pioTimeLevel).append("/");
		log.append("LookDownLevel:").append(lookDownLevel).append("/");
		log.append("Hoist:").append(hoistPosition).append("/");
		log.append("Shift:").append(shiftPosition).append("/");
		log.append("Rotate:").append(rotatePosition);
		return log.toString();
	}
}
