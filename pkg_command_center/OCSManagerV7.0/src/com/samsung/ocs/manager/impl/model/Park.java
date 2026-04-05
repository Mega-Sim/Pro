package com.samsung.ocs.manager.impl.model;

/**
 * Park Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2011. 6. 21.
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

public class Park {
	protected String name;
	protected Node node;
	protected String type;
	protected String vehicleZone;
	protected int vehicleZoneIndex;
	protected int capacity;
	protected int rank;
	protected boolean isEnabled;
	protected boolean isNodeType;
	// 2013.05.31 by KYK
	protected Station station;	
	protected boolean isStationType;

	public Park() {
		this.name = "";
		this.node = null;
		this.type = "";
		this.vehicleZone = "";
		this.vehicleZoneIndex = 9999;
		this.capacity = 1;
		this.rank = 9;
		this.isEnabled = false;
		this.isNodeType = true;
		//
		this.isStationType = false;
		this.station = null;
	}
	
	public Park(String name, Node node, String type, String vehicleZone, int vehicleZoneIndex, int capacity, int rank, boolean isEnabled) {
		setName(name);
		setNode(node);
		setType(type);
		setVehicleZone(vehicleZone);
		setVehicleZoneIndex(vehicleZoneIndex);
		setCapacity(capacity);
		setRank(rank);
		setEnabled(isEnabled);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
		setNodeType("NODE".equalsIgnoreCase(type));
		//
		setStationType("STATION".equalsIgnoreCase(type));
	}
	public String getVehicleZone() {
		return vehicleZone;
	}
	public void setVehicleZone(String vehicleZone) {
		this.vehicleZone = vehicleZone;
	}
	public int getVehicleZoneIndex() {
		return vehicleZoneIndex;
	}
	public void setVehicleZoneIndex(int vehicleZoneIndex) {
		this.vehicleZoneIndex = vehicleZoneIndex;
	}
	public int getCapacity() {
		return capacity;
	}
	public void setCapacity(int capacity) {
		if (capacity < 0) {
			this.capacity = 0;
		} else {
			this.capacity = capacity;
		}
	}
	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		if (rank < 1) {
			this.rank = 1;
		} else if (rank > 9) {
			this.rank = 9;
		} else {
			this.rank = rank;
		}
	}
	public boolean isEnabled() {
		return isEnabled;
	}
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	public boolean isNodeType() {
		return isNodeType;
	}
	public void setNodeType(boolean isNodeType) {
		this.isNodeType = isNodeType;
	}
	// 2013.05.31 by KYK
	public Station getStation() {
		return station;
	}
	public void setStation(Station station) {
		this.station = station;
	}
	public boolean isStationType() {
		return isStationType;
	}
	public void setStationType(boolean isStationType) {
		this.isStationType = isStationType;
	}

}
