package com.samsung.ocs.optimizer.model;

import com.samsung.ocs.manager.impl.model.Area;
import com.samsung.ocs.manager.impl.model.Vehicle;

public class Candidate {
	private Area area;
	private Vehicle vehicle;
	
	public Candidate(Area area, Vehicle vehicle){
		this.area = area;
		this.vehicle = vehicle;
	}
	public Area getArea() {
		return area;
	}
	public void setArea(Area area) {
		this.area = area;
	}
	public Vehicle getVehicle() {
		return vehicle;
	}
	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}
}
