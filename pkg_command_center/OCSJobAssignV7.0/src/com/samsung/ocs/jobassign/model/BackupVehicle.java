package com.samsung.ocs.jobassign.model;

import com.samsung.ocs.manager.impl.model.Vehicle;

public class BackupVehicle {
	private Vehicle vehicle = null;
	private double distanceCost = 9999.0;
	
	public BackupVehicle(Vehicle vehicle, double distanceCost) {
		this.vehicle = vehicle;
		this.distanceCost = distanceCost;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public double getDistanceCost() {
		return distanceCost;
	}

	public void setDistanceCost(double distanceCost) {
		this.distanceCost = distanceCost;
	}
}
