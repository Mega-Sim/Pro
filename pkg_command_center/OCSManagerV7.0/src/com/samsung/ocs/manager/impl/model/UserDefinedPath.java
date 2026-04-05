package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UserDefinedPath Class, OCS 3.0 for Unified FAB
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

public class UserDefinedPath {
	protected String name;
	protected List<String> fromLocationList = Collections.synchronizedList(new ArrayList<String>());
	protected List<String> toLocationList = Collections.synchronizedList(new ArrayList<String>());
	protected List<String> nodeList = Collections.synchronizedList(new ArrayList<String>());
	protected int vehicleLimit;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean containsFromLocation(String location) {
		return fromLocationList.contains(location);
	}
	public void setFromLocation(String fromLocation) {
		List<String> removeLocationList = new ArrayList<String>(fromLocationList);
		String[] location = fromLocation.split(",");
		for (int i = 0; i < location.length; i++) {
			if(fromLocationList.contains(location[i]) == false) {
				fromLocationList.add(location[i]);
			}
			removeLocationList.remove(location[i]);
		}
		
		for (int i = 0; i < removeLocationList.size(); i++) {
			fromLocationList.remove(removeLocationList.get(i));
		}
	}	
	public boolean containsToLocation(String location) {
		return toLocationList.contains(location);
	}
	public void setToLocation(String toLocation) {
		List<String> removeLocationList = new ArrayList<String>(toLocationList);
		String[] location = toLocation.split(",");
		for (int i = 0; i < location.length; i++) {
			if(toLocationList.contains(location[i]) == false) {
				toLocationList.add(location[i]);
			}
			removeLocationList.remove(location[i]);
		}
		
		for (int i = 0; i < removeLocationList.size(); i++) {
			toLocationList.remove(removeLocationList.get(i));
		}
	}
	public List<String> getNodeList() {
		return nodeList;
	}
	public void setNodeList(String nodeIdList) {
		List<String> removeNodeList = new ArrayList<String>(nodeList);		
		String[] nodes = nodeIdList.split(",");
		for (int i = 0; i < nodes.length; i++) {
			if(nodeList.contains(nodes[i]) == false) {
				this.nodeList.add(nodes[i]);
			}
			removeNodeList.remove(nodes[i]);
		}
		
		for (int i = 0; i < removeNodeList.size(); i++) {
			nodeList.remove(removeNodeList.get(i));
		}
	}
	public int getVehicleLimit() {
		return vehicleLimit;
	}
	public void setVehicleLimit(int vehicleLimit) {
		this.vehicleLimit = vehicleLimit;
	}
}
