package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;

/**
 * Area Class, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2011. 6. 21.
 * @version 3.0
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class Area {
	private String areaId;
	private int minVehicleCount = 0;
	private int maxVehicleCount = 200;
	
	private int minIdleVehicleCount = 0;
	private int targetIdleVehicleCount = 0;
	private int maxIdleVehicleCount = 100;
	
	private int minVehicleCountLowestLimit = 0;
	private int minVehicleCountUpperLimit = 100;
	private int maxVehicleCountLowestLimit = 0;
	private int maxVehicleCountUpperLimit = 500;
	
	private int minIdleVehicleCountLowestLimit = 0;
	private int minIdleVehicleCountUpperLimit = 100;
	private int maxIdleVehicleCountLowestLimit = 0;
	private int maxIdleVehicleCountUpperLimit = 500;
	
	private boolean isMinVehicleAssignAllowed;
	private boolean isMinVehicleParkAllowed;
	
	private ArrayList<Vehicle> stayingVehicleList;
	private ArrayList<Vehicle> outGoingVehicleList;
	private ArrayList<Vehicle> inComingVehicleList;
	
	// LocalVHL은 제외.
	private ArrayList<Vehicle> stayingIdleVehicleList;
	private ArrayList<Vehicle> outGoingIdleVehicleList;
	private ArrayList<Vehicle> inComingIdleVehicleList;
	private ArrayList<Vehicle> loadVehicleList;
	
	private ArrayList<TrCmd> unassignedTrCmdList;
	private ArrayList<TrCmd> destTrCmdList;
	
//	protected String waitingNode;
	private int distributedIdleVehicleCount;
	
	public Area() {
		// Enabled VHL Count in the AREA (Manual, LocalOHT 포함)
		stayingVehicleList = new ArrayList<Vehicle>();	// Stop & Target Area가 This Area
		outGoingVehicleList = new ArrayList<Vehicle>();	// Stop: This Area, but Target: NOT This Area
		inComingVehicleList = new ArrayList<Vehicle>();	// Stop: NOT Thiss Area, but Target: This Area
		
		// Assignable VHL Count in the AREA (Auto, Idle, LocalOHT 제외, PausedVHL 제외)
		stayingIdleVehicleList = new ArrayList<Vehicle>();	// Stop & Target Area가 This Area
		outGoingIdleVehicleList = new ArrayList<Vehicle>();	// Stop: This Area, but Target: NOT This Area
		inComingIdleVehicleList = new ArrayList<Vehicle>();	// Stop: NOT Thiss Area, but Target: This Area
		
		loadVehicleList = new ArrayList<Vehicle>();	// Target: This Area & Load VHL
		
		unassignedTrCmdList = new ArrayList<TrCmd>();
		destTrCmdList = new ArrayList<TrCmd>();
	}
	
	public String getAreaId() {
		return areaId;
	}
	public void setAreaId(String areaId) {
		this.areaId = areaId;
	}
	public int getMinVehicleCount() {
		return minVehicleCount;
	}
	public void setMinVehicleCount(int minVehicleCount) {
		if (minVehicleCount < minVehicleCountLowestLimit) {
			this.minVehicleCount = minVehicleCountLowestLimit;
		} else if (minVehicleCount > minVehicleCountUpperLimit) {
			this.minVehicleCount = minVehicleCountUpperLimit;
		} else {
			this.minVehicleCount = minVehicleCount;
		}
	}
	public int getMaxVehicleCount() {
		return maxVehicleCount;
	}
	public void setMaxVehicleCount(int maxVehicleCount) {
		if (maxVehicleCount < this.minVehicleCount) {
			this.maxVehicleCount = this.minVehicleCount;
		} else {
			if (maxVehicleCount < maxVehicleCountLowestLimit) {
				this.maxVehicleCount = maxVehicleCountLowestLimit;
			} else if (maxVehicleCount > maxVehicleCountUpperLimit) {
				this.maxVehicleCount = maxVehicleCountUpperLimit;
			} else {
				this.maxVehicleCount = maxVehicleCount;
			}
		}
	}
	public int getMinIdleVehicleCount() {
		return minIdleVehicleCount;
	}
	public void setMinIdleVehicleCount(int minIdleVehicleCount) {
		if (minIdleVehicleCount < minIdleVehicleCountLowestLimit) {
			this.minIdleVehicleCount = minIdleVehicleCountLowestLimit;
		} else if (minIdleVehicleCount > minIdleVehicleCountUpperLimit) {
			this.minIdleVehicleCount = minIdleVehicleCountUpperLimit;
		} else {
			this.minIdleVehicleCount = minIdleVehicleCount;
		}
	}
	public int getMaxIdleVehicleCount() {
		return maxIdleVehicleCount;
	}
	public void setMaxIdleVehicleCount(int maxIdleVehicleCount) {
		if (maxIdleVehicleCount > this.maxVehicleCount) {
			this.maxIdleVehicleCount = this.maxVehicleCount;
		} else {
			if (maxIdleVehicleCount < maxIdleVehicleCountLowestLimit) {
				this.maxIdleVehicleCount = maxIdleVehicleCountLowestLimit;
			} else if (maxIdleVehicleCount > maxIdleVehicleCountUpperLimit) {
				this.maxIdleVehicleCount = maxIdleVehicleCountUpperLimit;
			} else {
				this.maxIdleVehicleCount = maxIdleVehicleCount;
			}
		}
		if (this.maxIdleVehicleCount < this.minIdleVehicleCount) {
			this.maxIdleVehicleCount = this.minIdleVehicleCount;
		}
	}
	public int getMinVehicleCountLowestLimit() {
		return minVehicleCountLowestLimit;
	}
	public void setMinVehicleCountLowestLimit(int minVehicleCountLowestLimit) {
		this.minVehicleCountLowestLimit = minVehicleCountLowestLimit;
	}
	public int getMinVehicleCountUpperLimit() {
		return minVehicleCountUpperLimit;
	}
	public void setMinVehicleCountUpperLimit(int minVehicleCountUpperLimit) {
		this.minVehicleCountUpperLimit = minVehicleCountUpperLimit;
	}
	public int getMaxVehicleCountLowestLimit() {
		return maxVehicleCountLowestLimit;
	}
	public void setMaxVehicleCountLowestLimit(int maxVehicleCountLowestLimit) {
		this.maxVehicleCountLowestLimit = maxVehicleCountLowestLimit;
	}
	public int getMaxVehicleCountUpperLimit() {
		return maxVehicleCountUpperLimit;
	}
	public void setMaxVehicleCountUpperLimit(int maxVehicleCountUpperLimit) {
		this.maxVehicleCountUpperLimit = maxVehicleCountUpperLimit;
	}
	public int getMinIdleVehicleCountLowestLimit() {
		return minIdleVehicleCountLowestLimit;
	}
	public void setMinIdleVehicleCountLowestLimit(int minIdleVehicleCountLowestLimit) {
		this.minIdleVehicleCountLowestLimit = minIdleVehicleCountLowestLimit;
	}
	public int getMinIdleVehicleCountUpperLimit() {
		return minIdleVehicleCountUpperLimit;
	}
	public void setMinIdleVehicleCountUpperLimit(int minIdleVehicleCountUpperLimit) {
		this.minIdleVehicleCountUpperLimit = minIdleVehicleCountUpperLimit;
	}
	public int getMaxIdleVehicleCountLowestLimit() {
		return maxIdleVehicleCountLowestLimit;
	}
	public void setMaxIdleVehicleCountLowestLimit(int maxIdleVehicleCountLowestLimit) {
		this.maxIdleVehicleCountLowestLimit = maxIdleVehicleCountLowestLimit;
	}
	public int getMaxIdleVehicleCountUpperLimit() {
		return maxIdleVehicleCountUpperLimit;
	}
	public void setMaxIdleVehicleCountUpperLimit(int maxIdleVehicleCountUpperLimit) {
		this.maxIdleVehicleCountUpperLimit = maxIdleVehicleCountUpperLimit;
	}
	
	public boolean isMinVehicleAssignAllowed() {
		return isMinVehicleAssignAllowed;
	}

	public void setMinVehicleAssignAllowed(boolean isMinVehicleAssignAllowed) {
		this.isMinVehicleAssignAllowed = isMinVehicleAssignAllowed;
	}

	public boolean isMinVehicleParkAllowed() {
		return isMinVehicleParkAllowed;
	}

	public void setMinVehicleParkAllowed(boolean isMinVehicleParkAllowed) {
		this.isMinVehicleParkAllowed = isMinVehicleParkAllowed;
	}
	
	public int getTargetIdleVehicleCount() {
		return targetIdleVehicleCount;
	}
	public void setTargetIdleVehicleCount(int targetIdleVehicleCount) {
		this.targetIdleVehicleCount = targetIdleVehicleCount;
	}
	public int getDistributedIdleVehicleCount() {
		return distributedIdleVehicleCount;
	}
	public void setDistributedIdleVehicleCount(int distributedIdleVehicleCount) {
		this.distributedIdleVehicleCount = distributedIdleVehicleCount;
	}
	
	public void initVehicleList() {
		stayingVehicleList.clear();
		outGoingVehicleList.clear();
		inComingVehicleList.clear();
		
		stayingIdleVehicleList.clear();
		outGoingIdleVehicleList.clear();
		inComingIdleVehicleList.clear();
		loadVehicleList.clear();
		
		unassignedTrCmdList.clear();
		destTrCmdList.clear();
	}
	
	public void addStayingVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			if (stayingVehicleList.contains(vehicle) == false) {
				stayingVehicleList.add(vehicle);
			}
		}
	}
	public void removeStayingVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			stayingVehicleList.remove(vehicle);
		}
	}
	
	public void addOutGoingVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			if (outGoingVehicleList.contains(vehicle) == false) {
				outGoingVehicleList.add(vehicle);
			}
		}
	}
	public void removeOutGoingVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			outGoingVehicleList.remove(vehicle);
		}
	}
	
	public void addInComingVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			if (inComingVehicleList.contains(vehicle) == false) {
				inComingVehicleList.add(vehicle);
			}
		}
	}
	public void removeInComingVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			inComingVehicleList.remove(vehicle);
		}
	}
	
	public void addStayingIdleVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			if (stayingIdleVehicleList.contains(vehicle) == false) {
				stayingIdleVehicleList.add(vehicle);
			}
		}
	}
	public void removeStayingIdleVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			stayingIdleVehicleList.remove(vehicle);
			stayingVehicleList.remove(vehicle);
			addOutGoingIdleVehicle(vehicle);
			addOutGoingVehicle(vehicle);
		}
	}
	
	public void addOutGoingIdleVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			if (outGoingIdleVehicleList.contains(vehicle) == false) {
				outGoingIdleVehicleList.add(vehicle);
			}
		}
	}
	public void removeOutGoingIdleVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			outGoingIdleVehicleList.remove(vehicle);
		}
	}
	
	public void addInComingIdleVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			if (inComingIdleVehicleList.contains(vehicle) == false) {
				inComingIdleVehicleList.add(vehicle);
			}
		}
	}
	public void removeInComingIdleVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			inComingIdleVehicleList.remove(vehicle);
		}
	}
	
	public void addLoadVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			if (loadVehicleList.contains(vehicle) == false) {
				loadVehicleList.add(vehicle);
			}
		}
	}
	public void addUnassignedTrCmd(TrCmd trCmd) {
		if (trCmd != null) {
			if (unassignedTrCmdList.contains(trCmd) == false) {
				unassignedTrCmdList.add(trCmd);
			}
		}
	}
	public void addDestTrCmd(TrCmd trCmd) {
		if (trCmd != null) {
			if (destTrCmdList.contains(trCmd) == false) {
				destTrCmdList.add(trCmd);
			}
		}
	}
	public int getCurrentVehicleCount() {
		return stayingVehicleList.size() + outGoingVehicleList.size();
	}
	public int getCurrentAutoVehicleCount() { // 23.02.10 by JJW area max 기능
		int count = 0;
		for (Vehicle vehicle : stayingVehicleList) {
			if (vehicle.isActionHold() == true || vehicle.getVehicleMode() == 'M') {
				count++;
			}
		}
		return stayingVehicleList.size() + outGoingVehicleList.size() - count;
	}
	public int getStayingVehicleCount() {
		return stayingVehicleList.size();
	}
	public int getExpectedVehicleCount() {
		int expectedVehicleCount =  stayingVehicleList.size() + inComingVehicleList.size();
		int assignedDestTrCmdCount = assignedDestTrCmdCount();// 23.02.10 by JJW area max 기능
		return (expectedVehicleCount > assignedDestTrCmdCount) ? expectedVehicleCount : assignedDestTrCmdCount; // 23.02.10 by JJW area max 기능
	}
    // 23.02.10 by JJW area max 기능
	public int getExpectedAutoVehicleCount() {
		int count = 0;
		for(Vehicle vehicle : stayingVehicleList ){
			if(vehicle.isActionHold()== true || vehicle.getVehicleMode() =='M'){
				count++;
			}
		}
		for(Vehicle vehicle : inComingVehicleList ){
			if(vehicle.isActionHold()== true || vehicle.getVehicleMode() =='M'){
				count++;
			}
		}
		int expectedVehicleCount =  stayingVehicleList.size() + inComingVehicleList.size() - count;
		int assignedDestTrCmdCount = assignedDestTrCmdCount();
		return (expectedVehicleCount > assignedDestTrCmdCount) ? expectedVehicleCount : assignedDestTrCmdCount;
	}
    // 23.02.10 by JJW area max 기능
	public int getStayingIdleVehicleCount() {
		return stayingIdleVehicleList.size();
	}
	public int getOutGoingIdleVehicleCount() {
		return outGoingIdleVehicleList.size();
	}
	public int getInComingIdleVehicleCount() {
		return inComingIdleVehicleList.size();
	}
	public int getCurrentIdleVehicleCount() {
		return stayingIdleVehicleList.size() + outGoingIdleVehicleList.size();
	}
	public int getExpectedIdleVehicleCount() {
		return stayingIdleVehicleList.size() + inComingIdleVehicleList.size() + loadVehicleList.size();
	}
	public int getUnassignedTrCmdCount() {
		return unassignedTrCmdList.size();
	}
	public ArrayList<Vehicle> getStayingIdleVehicleList() {
		return stayingIdleVehicleList;
	}
	public ArrayList<Vehicle> getInComingIdleVehicleList() {
		return inComingIdleVehicleList;
	}
	public ArrayList<TrCmd> getUnassignedTrCmdList() {
		return unassignedTrCmdList;
	}
	public ArrayList<TrCmd> getDestTrCmdList() {
		return destTrCmdList;
	}
	private int assignedDestTrCmdCount() {
		int count = 0;
		TrCmd trCmd = null;
		for (int i = 0; i < destTrCmdList.size(); i++) {
			trCmd = destTrCmdList.get(i);
			if (trCmd != null) {
				if (trCmd.getAssignedVehicleId() != null && trCmd.getAssignedVehicleId().length() > 0) {
					count++;
				}
			}
		}
		return count;
	}
}
