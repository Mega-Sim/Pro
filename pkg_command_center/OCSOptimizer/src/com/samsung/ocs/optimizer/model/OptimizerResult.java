package com.samsung.ocs.optimizer.model;

public class OptimizerResult {
	private Candidate candidate;
	private Target target;
	private double modelingCost;
	private double distanceBasedCost;
	
	public OptimizerResult(Candidate candidate, Target target, double modelingCost, double distanceBasedCost) {
		this.candidate = candidate;
		this.target = target;
		this.modelingCost = modelingCost;
		this.distanceBasedCost = distanceBasedCost;
	}
	
	public Candidate getCandidate() {
		return candidate;
	}
	public void setCandidate(Candidate candidate) {
		this.candidate = candidate;
	}
	public Target getTarget() {
		return target;
	}
	public void setTarget(Target target) {
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
