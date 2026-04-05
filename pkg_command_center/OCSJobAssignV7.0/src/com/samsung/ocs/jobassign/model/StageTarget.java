package com.samsung.ocs.jobassign.model;

import com.samsung.ocs.manager.impl.model.TrCmd;

public class StageTarget {
	private String target = "";
	private boolean available = true;
	private TrCmd trCmd = null;
	
	public StageTarget(String target, boolean available, TrCmd trCmd) {
		this.target = target;
		this.available = available;
		this.trCmd = trCmd;
	}
	public boolean isAvailable() {
		return available;
	}
	public void setAvailable(boolean available) {
		this.available = available;
	}
	public TrCmd getTrCmd() {
		return trCmd;
	}
	public void setTrCmd(TrCmd trCmd) {
		this.trCmd = trCmd;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{").append(target).append("=");
		sb.append(available).append(",").append(trCmd).append("}");
		return sb.toString(); 
	}
}
