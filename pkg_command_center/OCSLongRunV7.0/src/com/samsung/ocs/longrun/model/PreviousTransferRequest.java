package com.samsung.ocs.longrun.model;

import com.samsung.ocs.manager.impl.model.CarrierLoc;

public class PreviousTransferRequest {
	private int index = 0;
	private CarrierLoc sourceLoc = null;
	private CarrierLoc destLoc = null;
	
	public PreviousTransferRequest(CarrierLoc sourceLoc, CarrierLoc destLoc) {
		this.index = 0;
		this.sourceLoc = sourceLoc;
		this.destLoc = destLoc;
	}
	public PreviousTransferRequest(int index, CarrierLoc sourceLoc, CarrierLoc destLoc) {
		this.index = index;
		this.sourceLoc = sourceLoc;
		this.destLoc = destLoc;
	}
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public CarrierLoc getSourceLoc() {
		return sourceLoc;
	}
	public void setSourceLoc(CarrierLoc sourceLoc) {
		this.sourceLoc = sourceLoc;
	}
	public CarrierLoc getDestLoc() {
		return destLoc;
	}
	public void setDestLoc(CarrierLoc destLoc) {
		this.destLoc = destLoc;
	}
}
