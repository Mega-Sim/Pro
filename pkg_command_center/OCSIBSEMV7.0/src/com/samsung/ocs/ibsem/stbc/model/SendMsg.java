package com.samsung.ocs.ibsem.stbc.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SendMsg {

	//public static final String VEHICLE_ASSIGNED = "VehicleAssigned";
	//public static final String VEHICLE_UNASSIGNED = "VehicleUnassigned";
	public static final String CARRIER_INSTALLED = "CarrierInstalled";
	public static final String CARRIER_REMOVED = "CarrierRemoved";

	//public static final String ASSIGN = "ASSIGN"; 
	//public static final String UNASSIGN = "UNASSIGN"; 
	public static final String LOAD = "LOAD"; 
	public static final String UNLOAD = "UNLOAD"; 

	protected String time;
	protected String eventName;
	protected String vehicleId;
	protected String carrierLoc;
	protected String carrierId;

	public SendMsg(String eventName, String vehicleId, String carrierLoc, String carrier) {
		setTime();
		setEventName(eventName);
		setVehicleId(vehicleId);
		setCarrierLoc(carrierLoc);
		setCarrierId(carrier);
	}

	public void setTime() {
		// 2014.11.17 by KBS : IBSEM에서 CarrierInstall/CarrierRemove 시간을 알 수 없어, 현재 시간으로 설정
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
		time = dateFormat.format(now);
	}

	public void setEventName(String eventName) {
		switch (eventName.charAt(7)) {
		//case 'A': 
		//	this.eventName = ASSIGN;
		//	break;
		//case 'U': 
		//	this.eventName = UNASSIGN;
		//	break;
		case 'I': 
			this.eventName = UNLOAD; // EQ 관점에서 UNLOAD
			break;
		case 'R': 
			this.eventName = LOAD;	// EQ 관점에서 LOAD
			break;
		default:
		}
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public void setCarrierLoc(String carrierLoc) {
		this.carrierLoc = carrierLoc;
	}

	public void setCarrierId(String carrierId) {
		this.carrierId = carrierId;
	}

	public String getReqMsg() {
		return this.time + "," + this.eventName + "," + this.vehicleId + "," + this.carrierLoc + "," + this.carrierId;
	}

}