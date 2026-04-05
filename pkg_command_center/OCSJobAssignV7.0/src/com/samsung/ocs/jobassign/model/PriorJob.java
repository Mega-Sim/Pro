package com.samsung.ocs.jobassign.model;

import com.samsung.ocs.manager.impl.model.TrCmd;

public class PriorJob {
	
	TrCmd trCmd;
	int index;
	double cost;
	
	public PriorJob(TrCmd trCmd, int index, double cost) {
		this.trCmd = trCmd;
		this.index = index;
		this.cost = cost;
	}

	public TrCmd getTrCmd() {
		return trCmd;
	}

	public void setTrCmd(TrCmd trCmd) {
		this.trCmd = trCmd;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

}
