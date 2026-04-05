package com.samsung.ocs.longrun.model;

public class PreviousPatrolRequest {
	private int index = 0;
	private Path path = null;
	private boolean isCostSearchChecked = false;

	public PreviousPatrolRequest(Path path) {
		this.index = 0;
		this.path = path;
	}

	public PreviousPatrolRequest(int index, Path path) {
		this.index = index;
		this.path = path;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Path getPath() {
		return path;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public boolean isCostSearchChecked() {
		return isCostSearchChecked;
	}

	public void setCostSearchChecked(boolean isCostSearchChecked) {
		this.isCostSearchChecked = isCostSearchChecked;
	}
}
