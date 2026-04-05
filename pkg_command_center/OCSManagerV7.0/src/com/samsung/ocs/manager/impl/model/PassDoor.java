package com.samsung.ocs.manager.impl.model;

import java.util.Hashtable;

import com.samsung.ocs.common.constant.OcsConstant;

/**
 * PassDoor Class, OCS 3.7
 * @author zzang9un
 * @date   2015. 2. 7
 */
public class PassDoor {
	private String passDoorId = "";
	private String nodeId = "";
	private String ipAddress = "127.0.0.1";
	private boolean enabled = false;
	private String mode = "M";
	private String status = "I";
	private String sensorData = "";
	private int errorCode = 0;
	private String pioData = "";
	
	private VehicleData occupiedVehicle; // PassDoor를 점유하고 있는 Vehicle
	private Hashtable<String, VehicleData> driveVehicleTable = new Hashtable<String, VehicleData>(); // PassDoor를 Drive하고 있는 Vehicle
	
	private static int ERRORCODE_PASSDOOR_COMFAIL = 9999;
	
	public PassDoor() {
		passDoorId = "";
		nodeId = "";
		ipAddress = "127.0.0.1";
		enabled = false;
		mode = "M";
		status = "I";
		sensorData = "";
		errorCode = 0;
		pioData = "";
		
		occupiedVehicle = null;
		driveVehicleTable.clear();
	}

	public String toString() {
		return getPassDoorId();
	}
	
	public String getPassDoorId() {
		return passDoorId;
	}

	public void setPassDoorId(String passDoorId) {
		this.passDoorId = passDoorId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	
	/**
	 * PassDoor가 Enable인지 Disable인지를 리턴
	 * @author zzang9un
	 * @date 2015. 2. 7.
	 * @return
	 */
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public void setEnabled(String enabled) {
		if (enabled.equals(OcsConstant.TRUE))
			this.enabled = true;
		else
			this.enabled = false;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSensorData() {
		return sensorData;
	}

	public void setSensorData(String sensorData) {
		this.sensorData = sensorData;
	}

	public int getErrorCode() {
		return errorCode;
	}
	
	public boolean isError() {
		if (getStatus().equals("E")) 
			return true;
		else
			return false;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getPioData() {
		return pioData;
	}

	public void setPioData(String pioData) {
		this.pioData = pioData;
	}

	public VehicleData getOccupiedVehicle() {
		return occupiedVehicle;
	}
	
	/**
	 * PassDoor를 점유하고 있는 Vehicle이 있는지를 리턴
	 * @author zzang9un
	 * @date 2015. 2. 7.
	 * @return
	 */
	public boolean isOccupiedVehicle() {
		if (occupiedVehicle != null)
			return true;
		else
			return false;
	}

	public void setOccupiedVehicle(VehicleData occupiedVehicle) {
		this.occupiedVehicle = occupiedVehicle;
	}
	
	public boolean isDriveVehicle() {
		if (driveVehicleTable.size() > 0)
			return true;
		else
			return false;		
	}
	
	public int countDriveVehicle() {
		return driveVehicleTable.size();
	}

	/**
	 * vehicle이 driveVehicleTable에 있는 지를 판단하는 함수
	 * @author zzang9un
	 * @date 2015. 2. 9.
	 * @param vehicle
	 * @return vehicle이 driveVehicleTable에 있는 경우 true, 그렇지 않은 경우 false를 리턴
	 */
	public boolean containDriveVehicle(VehicleData vehicle) {
		if (vehicle != null) {
			if (driveVehicleTable.contains(vehicle))
				return true;
			else
				return false;
		} else
			return false;		
	}
	
	/**
	 * PassDoor를 Drive하려는 Vehicle을 table에 등록하는 함수
	 * @author zzang9un
	 * @date 2015. 2. 7.
	 * @param vehicle
	 */
	public void addDriveVehicleTable(VehicleData vehicle) {
		if (vehicle != null) {
			if (!driveVehicleTable.containsKey(vehicle.getVehicleId())) {
				driveVehicleTable.put(vehicle.getVehicleId(), vehicle);
			}
		}			
	}

	public void setDriveVehicleTable(Hashtable<String, VehicleData> driveVehicleTable) {
		this.driveVehicleTable = driveVehicleTable;
	}
	
	/**
	 * 통과 가능한지 판단하는 함수<br>
	 * 1. Enable인 경우 <br>
	 * - Error, Manual인 경우 통과 못하도록 설정<br>
	 * - Auto이지만 통신 에러(9999)인 경우 통과 못하도록 설정<br>
	 * 2. Disable인 경우<br>
	 * - PassDoor의 상태에 관계없이 통과하도록 설정
	 * @author zzang9un
	 * @date 2015. 2. 11.
	 * @return 통과 가능한 경우 true, 그렇지 앟은 경우 false를 리턴
	 */
	public boolean checkPassable() {
		boolean checkPass = true;
	
		if (isEnabled()) {
			if (isError() || getMode().equals("M") || (getErrorCode() == ERRORCODE_PASSDOOR_COMFAIL))
				checkPass = false;
			else
				checkPass = true;
		} else {
			checkPass = true;
		}
		
		return checkPass;
	}
}
