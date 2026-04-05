package com.samsung.ocs.jobassign.model;

import com.samsung.ocs.manager.impl.model.TrCmd;

public class ReservedAsNextTrCmd {
	private TrCmd trCmd = null;
	private long waitingTime = 0;
	private double reservingCost = 9999.0;
	
	public ReservedAsNextTrCmd(TrCmd trCmd, long waitingTime, double reservingCost) {
		this.trCmd = trCmd;
		this.waitingTime = waitingTime;
		this.reservingCost = reservingCost;
	}

	public TrCmd getTrCmd() {
		return trCmd;
	}

	public void setTrCmd(TrCmd trCmd) {
		this.trCmd = trCmd;
	}

	public long getWaitingTime() {
		return waitingTime;
	}

	public void setWaitingTime(long waitingTime) {
		this.waitingTime = waitingTime;
	}

	public double getReservingCost() {
		return reservingCost;
	}

	public void setReservingCost(double reservingCost) {
		this.reservingCost = reservingCost;
	}
}
