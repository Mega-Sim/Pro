package com.samsung.ocs.jobassign.model;

import com.samsung.ocs.manager.impl.model.Vehicle;

public class JobAssignResult {
	private Vehicle vehicle;
	private Object target;
	private double modelingCost;
	private double distanceBasedCost;
	
	public JobAssignResult(Vehicle vehicle, Object target, double modelingCost, double distanceBasedCost) {
		this.vehicle = vehicle;
		this.target = target;
		this.modelingCost = modelingCost;
		this.distanceBasedCost = distanceBasedCost;
	}
	
	public Vehicle getVehicle() {
		return vehicle;
	}
	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}
	public Object getTarget() {
		return target;
	}
	public void setTarget(Object target) {
		this.target = target;
	}
	public double getModelingCost() {
		return modelingCost;
	}
	public void setModelingCost(double modelingCost) {
		this.modelingCost = modelingCost;
	}
	public double getDistanceBasedCost() {
		return distanceBasedCost;
	}
	public void setDistanceBasedCost(double distanceBasedCost) {
		this.distanceBasedCost = distanceBasedCost;
	}
}
