package com.samsung.ocs.manager.impl.model;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collision Class, OCS 3.0 for Unified FAB
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

public class Collision {
	protected String nodeId1;
	protected String nodeId2;
	protected String collisionId;
	protected boolean isSystemCollision;
	protected VehicleData vehicle = null;
	protected Vector<Node> nodeList = new Vector<Node>();
	protected ConcurrentHashMap<Node, Node> nodeTable = new ConcurrentHashMap<Node, Node>();
	
	public void setNodeId(String node1, String node2) {
		if (node1.compareTo(node2) <= 0) {
			this.nodeId1 = node1;
			this.nodeId2 = node2;
		} else {
			this.nodeId1 = node2;
			this.nodeId2 = node1;
		}
		
		setCollisionId();
	}
	
	protected void setCollisionId() {
		collisionId = nodeId1 + "_" + nodeId2;
	}
	
	public String getNodeId1() {
		return nodeId1;
	}
	public String getNodeId2() {
		return nodeId2;
	}
	
	public String getCollisionNodeId(String nodeId) {
		if (this.nodeId1.equals(nodeId)) {
			return this.nodeId2;
		} else if (this.nodeId2.equals(nodeId)){
			return this.nodeId1;
		} else {
			return "";
		}
	}
	
	public String getCollisionId() {
		return collisionId;
	}
	
	public Vector<Node> getNodeList() {
		return nodeList;
	}
	
	public void addNode(Node node) {
		nodeList.add(node);
	}
	
	/**
	 * 
	 */
	public void clear() {
		for (int i = 0; i < nodeList.size(); i++) {
			Node node = nodeList.get(i);
			node.removeCollision(this);
		}
		nodeList.clear();
	}

	public boolean isSystemCollision() {
		return isSystemCollision;
	}

	public void setSystemCollision(boolean isSystemCollision) {
		this.isSystemCollision = isSystemCollision;
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	public boolean checkNode(Node node) {
		if (nodeList.indexOf(node) >= 0) {
			return true;
		} else {
			return false;
		}
	}

	synchronized public boolean setOccupied(VehicleData vehicle, Node node) {
		if (this.vehicle == null) {
			this.vehicle = vehicle;
			nodeTable.put(node, node);
			return true;
		} else if (this.vehicle == vehicle) {
			nodeTable.put(node, node);
			return true;
		} else {
			return false;
		}
	}

	synchronized public void setReleased(VehicleData vehicle, Node node) {
		if (this.vehicle == vehicle) {
			nodeTable.remove(node);
			if (nodeTable.size() == 0) {
				this.vehicle = null;
			}
		}
	}

	synchronized public boolean isOccupied(VehicleData vehicle) {
		if (this.vehicle == null || this.vehicle == vehicle) {
			return false;
		} else {
			return true;
		}
	}

	public VehicleData getVehicle() {
		if (vehicle == null) {
			return null;
		} else {
			return vehicle;
		}
	}
	
	public String getVehicleId() {
		if (vehicle == null) {
			return "";
		} else {
			return vehicle.getVehicleId();
		}
	}
	
	/**
	 * 
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (isSystemCollision) {
			sb.append("S : ");
		} else {
			sb.append("U : ");
		}

		for (int i = 0; i < nodeList.size(); i++) {
			Node node = nodeList.get(i);
			// 2012.03.02 by MYM : [NotNullCheck] Ãß°¡
			if (i == 0) {
				sb.append(node);
			} else {
				sb.append(", ").append(node);
			}
		}
		return sb.toString();
	}
	
	public Node getCollisionNode(Node node) {
		if (checkNode(node)) {
			if (nodeList.get(0) == node) {
				if (nodeList.size() > 1 &&
						nodeList.get(1) != node) {
					return nodeList.get(1);
				}
			} else {
				return nodeList.get(0);
			}
		}
		return null;
	}
}
