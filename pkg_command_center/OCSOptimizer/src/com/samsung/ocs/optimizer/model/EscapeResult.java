package com.samsung.ocs.optimizer.model;

import com.samsung.ocs.manager.impl.model.Vehicle;

public class EscapeResult {
	private Vehicle vehicle;
	private double cost;
	
	public EscapeResult(Vehicle vehicle, double cost){
		this.vehicle = vehicle;
		this.cost = cost;
	}
	
	public Vehicle getVehicle() {
		return vehicle;
	}
	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
}
