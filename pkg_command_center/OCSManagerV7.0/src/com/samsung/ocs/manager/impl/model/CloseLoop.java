package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;

import com.samsung.ocs.manager.impl.NodeManager;

/**
 * CloseLoop Class, OCS 3.0 for Unified FAB
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

public class CloseLoop {
	protected String closeLoopId;
	protected int vehicleLimit;
	private LinkedList<Node> nodeList = new LinkedList<Node>();
	private Hashtable<VehicleData, Object> vehicleTable = new Hashtable<VehicleData, Object>();
	
	public String getCloseLoopId() {
		return closeLoopId;
	}
	public void setCloseLoopId(String closeLoopId) {
		this.closeLoopId = closeLoopId;
	}
	public int getVehicleLimit() {
		return vehicleLimit;
	}
	public void setVehicleLimit(int vehicleLimit) {
		this.vehicleLimit = vehicleLimit;
	}
	public void addNode(Node node) {
		nodeList.add(node);
		node.addCloseLoop(this);
	}
	public int getVehicleCount() {
		return vehicleTable.size();
	}
	
	/**
	 * 
	 */
	public void clear() {
		if (nodeList != null) {
			for (int i = 0; i < nodeList.size(); i++) {
				Node node = nodeList.get(i);
				if (node != null) {
					node.removeCloseLoop(this);
				}
			}
			nodeList.clear();
		}
		if (vehicleTable != null) {
			vehicleTable.clear();
		}
	}
	
	private void getConnectedCloseLoopNodeList(Node node, ArrayList<Node> connectedCloseLoopNodeList) {
		if (node != null && connectedCloseLoopNodeList != null) {
			int pos = 0;
			Node tempNode = null;
			Section section = null;
			if (nodeList.contains(node)) {
				if (connectedCloseLoopNodeList.contains(node) == false) {
					connectedCloseLoopNodeList.add(node);
				} else {
					return;
				}
			}
			for (int i = 0; i < node.getSectionCount(); i++) {
				section = node.getSection(i);
				if (section != null) {
					pos = section.getNodeIndex(node);
					for (int j = pos + 1; j < section.getNodeCount(); j++) {
						tempNode = section.getNode(j);
						if (tempNode != null) {
							if (nodeList.contains(tempNode)) {
								if (j == section.getNodeCount() - 1) {
									getConnectedCloseLoopNodeList(tempNode, connectedCloseLoopNodeList);
								}
								if (connectedCloseLoopNodeList.contains(tempNode) == false) {
									connectedCloseLoopNodeList.add(tempNode);
								}
							} else {
								break;
							}
						}
					}
					for (int j = pos - 1; j >= 0; j--) {
						tempNode = section.getNode(j);
						if (tempNode != null) {
							if (nodeList.contains(tempNode)) {
								if (j == 0) {
									getConnectedCloseLoopNodeList(tempNode, connectedCloseLoopNodeList);
								}
								if (connectedCloseLoopNodeList.contains(tempNode) == false) {
									connectedCloseLoopNodeList.add(tempNode);
								}
							} else {
								break;
							}
						}
					}
				}
			}
		}
	}
	
	public void buildConnectedCloseLoopNodeList(ArrayList<Node> connectedCloseLoopNodeList) {
		if (nodeList != null && connectedCloseLoopNodeList != null) {
			Node node = null;
			ArrayList<Node> tempConnectedCloseLoopNodeList = new ArrayList<Node>();
			for (int i = 0; i < nodeList.size(); i++) {
				node = nodeList.get(i);
				if (node != null) {
					if (connectedCloseLoopNodeList.contains(node)) {
						continue;
					}
					getConnectedCloseLoopNodeList(node, tempConnectedCloseLoopNodeList);
					if (tempConnectedCloseLoopNodeList.size() >= nodeList.size() * 0.5) {
						for (Node tempNode : tempConnectedCloseLoopNodeList) {
							if (tempNode != null) {
								if (connectedCloseLoopNodeList.contains(tempNode) == false) {
									connectedCloseLoopNodeList.add(tempNode);
								}
							}
						}
					}
					tempConnectedCloseLoopNodeList.clear();
				}
			}
		}
	}
	
	public void checkCloseLoopValidityIsolated(ArrayList<Node> connectedCloseLoopNodeList, ArrayList<Node> isolatedNodeList) {
		if (nodeList != null &&
				connectedCloseLoopNodeList != null &&
				isolatedNodeList != null) {
			Node node = null;
			boolean isCollisionNode = false;
			Collision collision = null;
			Node collisionNode = null;
			NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
			ArrayList<Node> tempConnectedCollisionNodeList = new ArrayList<Node>();
			
			for (int i = 0; i < nodeList.size(); i++) {
				node = nodeList.get(i);
				if (node != null) {
					if (connectedCloseLoopNodeList.contains(node) == false) {
						isCollisionNode = false;
						ListIterator<Collision> iterator = node.getCollisions().listIterator();
						while (iterator.hasNext()) {
							collision = (Collision)iterator.next();
							if (collision != null) {
								collisionNode = nodeManager.getNode(collision.getCollisionNodeId(node.getNodeId()));
								if (collisionNode != null) {
									if (connectedCloseLoopNodeList.contains(collisionNode)) {
										isCollisionNode = true;
										if (tempConnectedCollisionNodeList.contains(node) == false) {
											tempConnectedCollisionNodeList.add(node);
										}
										break;
									}
								}
							}
						}
						if (isCollisionNode == false && isolatedNodeList.contains(node) == false) {
							isolatedNodeList.add(node);
						}
					}
				}
			}
			
			for (Node tempNode : tempConnectedCollisionNodeList) {
				if (tempNode != null && isolatedNodeList.size() > 0) {
					refineCollisionConnected(tempNode, isolatedNodeList);
				}
			}
		}
	}
	
	private void refineCollisionConnected(Node node, ArrayList<Node> isolatedNodeList) {
		if (node != null && isolatedNodeList != null) {
			ArrayList<Node> tempConnectedCloseLoopNodeList = new ArrayList<Node>();
			getConnectedCloseLoopNodeList(node, tempConnectedCloseLoopNodeList);
			for (Node tempNode : tempConnectedCloseLoopNodeList) {
				if (tempNode != null) {
					if (isolatedNodeList.contains(tempNode)) {
						isolatedNodeList.remove(tempNode);
					}
				}
			}
		}
	}
	
	public void checkCloseLoopValidity(ArrayList<Node> isolatedNodeList, ArrayList<Node> missedNodeList) {
		if (nodeList != null) {
			Node node = null;
			for (int i = 0; i < nodeList.size(); i++) {
				node = nodeList.get(i);
				if (node != null) {
					if (isolatedNodeList.contains(node) == false) {
						checkCloseLoopValidityBackward(node, isolatedNodeList, missedNodeList);
					}
				}
			}
		}
	}
	
	private boolean isValidCloseLoopNode(Node node, ArrayList<Node> isolatedNodeList) {
		if (node != null) {
			if (isolatedNodeList != null) {
				if (isolatedNodeList.contains(node) == false) {
					if (nodeList.contains(node)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private void checkCloseLoopValidityBackward(Node node, ArrayList<Node> isolatedNodeList, ArrayList<Node> missedNodeList) {
		if (node != null) {
			int pos = 0;
			Section section = null;
			Node prevNode = null;
			boolean wasPrevNodeInCloseLoop = true;
			ArrayList<Node> tempNodeList = new ArrayList<Node>();
			for (int i = 0; i < node.getSectionCount(); i++) {
				tempNodeList.clear();
				wasPrevNodeInCloseLoop = true;
				section = node.getSection(i);
				if (section != null) {
					pos = section.getNodeIndex(node);
					for (int j = pos - 1; j >= 0; j--) {
						prevNode = section.getNode(j);
						if (prevNode != null) {
							if (j == 0) {
								if (isValidCloseLoopNode(prevNode, isolatedNodeList) == false) {
									if (checkCloseLoopValidityLastNode(prevNode, isolatedNodeList)) {
										if (missedNodeList.contains(prevNode) == false && tempNodeList.contains(prevNode) == false) {
											tempNodeList.add(prevNode);
										}
									} else {
										tempNodeList.clear();
									}
								} else {
									wasPrevNodeInCloseLoop = true;
								}
							} else {
								if (wasPrevNodeInCloseLoop) {
									if (isValidCloseLoopNode(prevNode, isolatedNodeList) == false) {
										if (missedNodeList.contains(prevNode) == false && tempNodeList.contains(prevNode) == false) {
											tempNodeList.add(prevNode);
										}
										wasPrevNodeInCloseLoop = false;
									} else {
										wasPrevNodeInCloseLoop = true;
									}
								} else {
									if (isValidCloseLoopNode(prevNode, isolatedNodeList) == false) {
										if (missedNodeList.contains(prevNode) == false && tempNodeList.contains(prevNode) == false) {
											tempNodeList.add(prevNode);
										}
										wasPrevNodeInCloseLoop = false;
									}
								}
							}
						}
					}
					for (Node tempNode : tempNodeList) {
						if (tempNode != null) {
							if (missedNodeList.contains(tempNode) == false && isolatedNodeList.contains(tempNode) == false) {
								missedNodeList.add(tempNode);
							}
						}
					}
				}
			}
		}
	}
	
	public boolean checkCloseLoopValidityLastNode(Node node, ArrayList<Node> isolatedNodeList) {
		if (node != null) {
			int pos = 0;
			Node prevNode = null;
			Section section = null;
			for (int i = 0; i < node.getSectionCount(); i++) {
				section = node.getSection(i);
				if (section != null) {
					pos = section.getNodeIndex(node);
					if (pos > 0) {
						prevNode = section.getNode(pos - 1);
						if (prevNode != null) {
							if (isValidCloseLoopNode(prevNode, isolatedNodeList)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
	
	public void checkCloseLoopValidityCollisions(ArrayList<Node> connectedCloseLoopNodeList, ArrayList<Node> isolatedNodeList, ArrayList<Node> missedNodeList, ArrayList<Node> missedCollisionsList) {
		if (nodeList != null) {
			Node lastNode = null;
			Node criticalNode = null;
			Node collisionNode = null;
			Collision collision = null;
			Section section = null;
			int pos = 0;
			ArrayList<Node> criticalNodeList = new ArrayList<Node>();
			NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
			
			ArrayList<Node> firstNodeList = new ArrayList<Node>();
			ArrayList<Node> lastNodeList = new ArrayList<Node>();
			ArrayList<Candidate> candidateList = new ArrayList<Candidate>();
			ArrayList<Node> ignoredList = new ArrayList<Node>();
			
			for (Node node : connectedCloseLoopNodeList) {
				if (node != null) {
					for (int i = 0; i < node.getSectionCount(); i++) {
						section = node.getSection(i);
						if (section != null) {
							pos = section.getNodeIndex(node);
							if (pos == 0) {
								lastNode = section.getNode(section.getNodeCount() - 1);
								if (lastNode != null) {
									if (connectedCloseLoopNodeList.contains(lastNode)) {
										firstNodeList.add(node);
										lastNodeList.add(lastNode);
										candidateList.add(new Candidate(node, lastNode, section));
										
										for (int j = 0; j < section.getNodeCount(); j++) {
											criticalNode = section.getNode(j);
											if (criticalNode != null) {
												if (connectedCloseLoopNodeList.contains(criticalNode)) {
													if (criticalNodeList.contains(criticalNode) == false) {
														criticalNodeList.add(criticalNode);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			Candidate candidate = null;
			Node firstNode = null;
			
			for (int i = candidateList.size() - 1; i >= 0; i--) {
				candidate = candidateList.get(i);
				if (candidate != null) {
					firstNode = candidate.getFirstNode();
					lastNode = candidate.getLastNode();
					if (firstNode != null && lastNode != null) {
						if (firstNodeList.contains(lastNode) == false) {
							ignoredList.add(lastNode);
						}
						if (lastNodeList.contains(firstNode) == false) {
							ignoredList.add(firstNode);
						}
					}
				}
			}
			for (int i = candidateList.size() - 1; i >= 0; i--) {
				candidate = candidateList.get(i);
				if (candidate != null) {
					firstNode = candidate.getFirstNode();
					lastNode = candidate.getLastNode();
					if (firstNode != null && lastNode != null) {
						if (firstNodeList.contains(lastNode) == false || lastNodeList.contains(firstNode) == false) {
							candidateList.remove(i);
							firstNodeList.remove(firstNode);
							lastNodeList.remove(lastNode);
						}
					}
				}
			}
			
			if (candidateList.size() > 0 &&
					firstNodeList.size() == candidateList.size()) {
				// Circle Exist
				criticalNodeList.clear();
				ignoredList.clear();
				for (Candidate circleCandidate : candidateList) {
					if (circleCandidate != null) {
						section = circleCandidate.getSection();
						if (section != null) {
							for (int j = 0; j < section.getNodeCount(); j++) {
								criticalNode = section.getNode(j);
								if (criticalNode != null) {
									if (connectedCloseLoopNodeList.contains(criticalNode)) {
										if (criticalNodeList.contains(criticalNode) == false) {
											criticalNodeList.add(criticalNode);
										}
									}
								}
							}
						}
					}
				}
			}
			
			for (Node node : criticalNodeList) {
				if (node != null) {
					if (ignoredList.contains(node) == false) {
						ListIterator<Collision> iterator = node.getCollisions().listIterator();
						while (iterator.hasNext()) {
							collision = (Collision)iterator.next();
							if (collision != null) {
								collisionNode = nodeManager.getNode(collision.getCollisionNodeId(node.getNodeId()));
								if (collisionNode != null) {
									if (isValidCloseLoopNode(collisionNode, isolatedNodeList) == false) {
										if (missedNodeList.contains(collisionNode) == false &&
												missedCollisionsList.contains(collisionNode) == false) {
											missedCollisionsList.add(collisionNode);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param vehicle VehicleData
	 * @param node Node
	 * @param isInitialized boolean
	 * @return result String
	 */
	@SuppressWarnings("unchecked")
	public boolean checkMoveIn(VehicleData vehicle, Node node, boolean isInitialized, int forwardVehicleCount) {
		Hashtable<Node, VehicleData> nodeTable = (Hashtable<Node, VehicleData>) vehicleTable.get(vehicle);
		if (nodeTable != null) {
			// 동일한 CloseLoop상에서 Drive 대상 Vehicle이 이동하려고 할때 이 루틴이 실행됨.
			nodeTable.put(node, vehicle);
			return false;
		} else if (isInitialized) {
			// 초기 구동 후 Vehicle이 처음으로 인지된 CurrentNode에 대해 아래의 코드가 실행됨.
			nodeTable = new Hashtable<Node, VehicleData>();
			nodeTable.put(node, vehicle);
			vehicleTable.put(vehicle, nodeTable);
			return false;
			// 2015.10.14 by MYM : 근접제어 CloseLoop 체크 추가 (전방 Vehicle 체크)
//		} else if (vehicleTable.size() < vehicleLimit) {
		} else if (forwardVehicleCount == 0 && vehicleTable.size() < vehicleLimit) {
			// 새로운 CloseLoop 정보를 가진 Node로 Drive를 시도할 때 이 루틴이 실행됨.
			nodeTable = new Hashtable<Node, VehicleData>();
			nodeTable.put(node, vehicle);
			vehicleTable.put(vehicle, nodeTable);
			return false;
		} else {
			StringBuffer log = new StringBuffer("Closeloop Capacity Full(");
			log.append(vehicleTable.size()).append("/").append(vehicleLimit).append(") ");
			vehicle.setReason(log.toString());
			
			registerDriveFailCausedVehicleList(vehicle);
			return true;
		}
	}
	
	/**
	 * 
	 * @param vehicle VehicleData
	 * @param node Node
	 * @return result boolean
	 */
	@SuppressWarnings("unchecked")
	public boolean checkMoveOut(VehicleData vehicle, Node node) {
		Hashtable<Node, VehicleData> NodeTable = (Hashtable<Node, VehicleData>) vehicleTable.get(vehicle);
		if (NodeTable != null) {
			NodeTable.remove(node);
			if (NodeTable.size() == 0) {
				vehicleTable.remove(vehicle);
			}
			debugString(vehicle, node, "MoveOut");
		}
		return true;
	}
	
	/**
	 * 
	 * @param vehicle VehicleData
	 * @param node Node
	 * @param type String
	 * @return debug String
	 */
	public String debugString(VehicleData vehicle, Node node, String type) {
		StringBuffer log = new StringBuffer();
		log.append(vehicle.getVehicleId()).append("> ").append(type).append("(").append(node.getNodeId()).append(",").append(vehicleTable.size());
		log.append("/").append(vehicleLimit).append("/").append(toVehicleString()).append(") :").append(closeLoopId);
//		System.out.println(log.toString());
		return log.toString();
	}
	
	/**
	 * 
	 * @return vehicles String
	 */
	public String toVehicleString() {
		StringBuffer vehicles = new StringBuffer();
		for (Enumeration<VehicleData> e = vehicleTable.keys(); e.hasMoreElements();) {
			if (vehicles.length() > 0) {
				vehicles.append(",");
			}
			VehicleData vehicle = e.nextElement();
			vehicles.append(vehicle.getVehicleId()).append("(").append(vehicle.getCurrNode()).append(")");
		}
		return vehicles.toString();
	}
	
	/**
	 * 
	 * @return nodes String
	 */
	public String toString() {
		StringBuffer nodes = new StringBuffer();
		for (int i = 0; i < nodeList.size(); i++) {
			nodes.append(nodeList.get(i).getNodeId());
			if (i < nodeList.size()-1) {
				nodes.append("/");
			}
		}
		
		return nodes.toString();
	}
	
	@SuppressWarnings("unchecked")
	private void registerDriveFailCausedVehicleList(VehicleData vehicle) {
		// 2011.12.27 by PMM
		if (vehicle != null &&
				vehicle.getDriveFailCausedVehicleList() != null) {
			VehicleData driveFailCausedVehicle;
			ArrayList<VehicleData> removeList = (ArrayList<VehicleData>)vehicle.getDriveFailCausedVehicleList().clone(); 
			for (Enumeration<VehicleData> e = vehicleTable.keys(); e.hasMoreElements();) {
				driveFailCausedVehicle = e.nextElement();
				if (driveFailCausedVehicle != null) {
					// 2012.02.06 by PMM
//					driveFailCausedVehicle.setYieldRequest(true);
//					driveFailCausedVehicle.setYieldRequestedVehicle(vehicle);
					driveFailCausedVehicle.requestYield(vehicle);
					vehicle.addVehicleToDriveFailCausedVehicleList(driveFailCausedVehicle);
					if (removeList.contains(driveFailCausedVehicle)) {
						removeList.remove(driveFailCausedVehicle);
					}
				}
			}
			ListIterator<VehicleData> it = removeList.listIterator();
			while (it.hasNext()) {
				vehicle.removeVehicleFromDriveFailCausedVehicleList(it.next());
			}
		}
	}
}


class Candidate {
	private Node firstNode = null;
	private Node lastNode = null;
	private Section section = null;
	
	public Candidate(Node firstNode, Node lastNode, Section section) {
		this.firstNode = firstNode;
		this.lastNode = lastNode;
		this.section = section;
	}

	public Node getFirstNode() {
		return firstNode;
	}
	public void setFirstNode(Node firstNode) {
		this.firstNode = firstNode;
	}
	public Node getLastNode() {
		return lastNode;
	}
	public void setLastNode(Node lastNode) {
		this.lastNode = lastNode;
	}
	public Section getSection() {
		return section;
	}
	public void setSection(Section section) {
		this.section = section;
	}
}