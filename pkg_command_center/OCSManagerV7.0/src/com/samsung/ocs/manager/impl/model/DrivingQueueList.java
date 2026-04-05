package com.samsung.ocs.manager.impl.model;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.constant.OcsConstant;

public class DrivingQueueList {
	private String id;
	private String type;
	private Block block;
	// 2013.02.08 by KYK
	private double distanceToBlock;
	private double arrivedTimeToBlock;
	private double userDQLimitTime;
	
	private Vector<Node> nodeList;
	private Vector<Node> systemNodeList;
	private ConcurrentHashMap<Node, DrivingQueueList> nodeTable;
	private String userNodeType;
	private static final String ADD = "ADD";
	private static final String IGNORE = "IGNORE";
	private static final String NONE = "NONE";
	private static final String OK = "OK";

	public DrivingQueueList(String drivingQueue, String Type, Block block) {
		this.id = drivingQueue;
		this.type = Type;
		this.block = block;
		this.nodeList = new Vector<Node>();
		this.systemNodeList = new Vector<Node>();
		this.nodeTable = new ConcurrentHashMap<Node, DrivingQueueList> ();
		this.userNodeType = NONE;
		// 2013.02.08 by KYK
		this.distanceToBlock = 0.0;
		this.arrivedTimeToBlock = 0.0;
		this.userDQLimitTime = 0.0;
	}

	// 2013.02.08 by KYK
	public double getDistanceToBlock() {
		return distanceToBlock;
	}
	public void setDistanceToBlock(double distanceToBlock) {
		this.distanceToBlock = distanceToBlock;
	}
	public double getArrivedTimeToBlock() {
		return arrivedTimeToBlock;
	}
	public void setArrivedTimeToBlock(double arrivedTimeToBlock) {
		this.arrivedTimeToBlock = arrivedTimeToBlock;
	}
	// 2013.04.19 by KYK
	public double getUserDQLimitTime() {
		return userDQLimitTime;
	}
	public void setUserDQLimitTime(double userDQLimitTime) {
		this.userDQLimitTime = userDQLimitTime;
	}
	// 2013.05.10 by KYK
	public Node getFirstDQNode() {
		return get(0);
	}
	public Node getLastDQNode() {
		if (size() > 0) {
			return get(size()-1);			
		} else {
			return null;
		}
	}

	public String getUserNodeType() {
		return userNodeType;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public Block getBlock() {
		return block;
	}

	public void addNode(Node node) {
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (node != null && nodeList.contains(node) == false) {
			nodeList.add(node);
			systemNodeList.add(node);
			nodeTable.put(node, this);
		}
	}
	
	/**
	 * 2013.05.10 by KYK
	 * @param node
	 */
	public void addUserNode(Node node) {
		if (node != null && nodeList.contains(node) == false) {
			nodeList.add(node);
			nodeTable.put(node, this);
		}
	}
	
	public void removeUserNode(Node node) {
		if (node != null) {
			nodeList.remove(node);
			nodeTable.remove(node);
		}
	}
	
	public void addNodeToFirst(Node node) {
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (node != null && nodeList.contains(node) == false) {
			nodeList.add(0, node);
			systemNodeList.add(0, node);
			nodeTable.put(node, this);
		}
	}
	
	public void addBlockNode(Node node) {
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (node != null && nodeList.contains(node) == false) {
			nodeList.add(0, node);
			nodeTable.put(node, this);
		}
	}
	
	public DrivingQueueList getDrivingQueueList(Node node) {
		return nodeTable.get(node);
	}

	public int size() {
		return nodeList.size();
	}

	public Node get(int index) {
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (index < 0 || index >= nodeList.size()) {
			return null;
		}
		return nodeList.get(index);
	}
	
	public int systemNodeSize() {
		return systemNodeList.size();
	}
	
	public Node getSystemNode(int index) {
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (index < 0 || index >= systemNodeList.size()) {
			return null;
		}
		return systemNodeList.get(index);
	}
	
	public String getSystemNodeListString() {
		return systemNodeList.toString();
	}
	
	public String getNodeListString() {
		return nodeList.toString();
	}

	public Vector<Node> getNodeList() {
		return nodeList;
	}

	public int getIndex(Node node) {
//		return nodeList.indexOf(node);
		// 2014.10.20 by MYM : 가상 및 UserBlock 노드인 경우 Index 스킵하도록 수정
		int noStopNodeCount = 0;
		int index = 1;
		for (int i = 0; i < nodeList.size(); i++) {
			Node tmpNode = nodeList.get(i);
			if (tmpNode == node) {
				index = (i + 1) - noStopNodeCount;
				break;
			} else if (tmpNode.isVirtual() || block.containsInBlockNode(tmpNode)) {
				noStopNodeCount++;
			}
		}
		return index;
	}
	
	public boolean removeBlockNode(Node node) {
		int rmNodeIndex = nodeList.indexOf(node);
		int mainBlockNodeIndex = nodeList.indexOf(block.getMainBlockNode());
		if (rmNodeIndex < mainBlockNodeIndex) {
			nodeTable.remove(node);
			return nodeList.remove(node);
		}
		return false;
	}

	private String checkConsecutive(Node prevNode, Vector<Node> nodeList) {
		Vector<Node> tmpNodeList = null;
		int pos = nodeList.indexOf(prevNode);
		if (pos >= 0) {
			tmpNodeList = new Vector<Node>();
			for (int i = pos+1; i < nodeList.size(); i++) {
				tmpNodeList.add(nodeList.get(i));
			}
		} else {
			tmpNodeList = new Vector<Node>(nodeList);
		}
		
		if(isConsecutive(prevNode, tmpNodeList) == false) {
			StringBuffer log = new StringBuffer();
			for(Node node : tmpNodeList) {
				if(log.length() > 0) {
					log.append(",");
				}
				log.append(node.getNodeId());
			}
			return log.toString();
		}
		
		return OK;
	}
	
	private boolean isConsecutive(Node prevNode, Vector<Node> nodeList) {
		// Section : [101001,101002,101003,101004]
		// prevNode: 101003, currNode: 101004
		for (int i = 0; i < prevNode.getSectionCount(); i++) {
			Section section = prevNode.getSection(i);
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (section != null) {
				if (prevNode.equals(section.getFirstNode()) == false) {
					int pos = section.getNodeIndex(prevNode);
					Node node = section.getNode(pos - 1);
					if (nodeList.contains(node)) {
						nodeList.remove(node);
						if (nodeList.size() > 0) {
							isConsecutive(node, nodeList);
						}
						break;
					}
				}
			} else {
				/*Null*/
			}
		}
		return nodeList.size() == 0;
	}
	
	public String updateUserNode(String type, Vector<Node> userNodeList) {
		if (ADD.equals(type)) {
			// 1. SystemDrivingQueue 이후로 연결 가능한 Node인지 확인한다.
			Node prevNode = systemNodeList.lastElement();
			String retVal = checkConsecutive(prevNode, userNodeList);
			if (OK.equals(retVal) == false) {
				StringBuffer log = new StringBuffer();
				log.append("[").append(type).append("] Node(");
				log.append(retVal).append(") is not consecutive");
				return log.toString();
			}
			
			// 2. SystemDrivingQueue에 이후로 Node를 추가하여 사용자 Node를 적용한다.
			return applyUserNode(type, userNodeList);
		} else if (IGNORE.equals(type)) {
			// 1. 현재의 SystemDrivingQueue에서 사용자가 Node를 제거한 임시 리스트(tmpNodeList)를 생성한다.
			Vector<Node> tmpNodeList = new Vector<Node>(systemNodeList);			
			Node firstNode = null;
			if (OcsConstant.DIVERGE.equals(block.getBlockType())) {
				firstNode = block.getMainBlockNode();
			} else {
				firstNode = systemNodeList.firstElement();
			}
			for (Node node : userNodeList) {
				if (firstNode.equals(node) == false) {
					tmpNodeList.remove(node);
				}
			}
			// 사용자가 Ignore할 Node가 현재 적용중인 DrivingQueue에 없어서 UserDrivingQueue 반영 불가
 			if(nodeList.toString().equals(tmpNodeList.toString())) {
				StringBuffer log = new StringBuffer();
				log.append("[").append(type).append("] DrivingQueue already have been applied");
				return log.toString();
			}

			// 2. 임시 리스트의 노드간 연결이 이루어지는지 확인한다.
			String retVal = checkConsecutive(firstNode, tmpNodeList);
			if (OK.equals(retVal) == false) {
				StringBuffer log = new StringBuffer();
				log.append("[").append(type).append("] Node(");
				log.append(retVal).append(") is not consecutive");
				return log.toString();
			}
			
			// 3. 최소 DrivingQueue 개수 확인	
			int realDQCount = 0;
			for (Node node : tmpNodeList) {
				if (block.containsInBlockNode(node) == false
						&& node.isVirtual() == false) {
					realDQCount++;
				}
			}
			if (realDQCount < 2) {
				StringBuffer log = new StringBuffer();
				log.append("[").append(type).append("] Need at least 2 real node drivingQueue");				
				return log.toString();
			}

			// 4. SystemDQ에서 사용자가 제거한 Node를 제거한다.
			return applyUserNode(type, userNodeList);
		} else {
			// SystemDQ로 복구
			return applyUserNode(type, userNodeList);
		}
	}
	
	private String applyUserNode(String type, Vector<Node> userNodeList) {
		synchronized (nodeList) {
			// 1. nodeList(사용자 적용된 DrivingQueue)를 systemNodeList(systemDrivingQueue)로 원복한다.
			int pos = systemNodeList.indexOf(nodeList.lastElement());
			if (pos >= 0) {
				// 기존에 systemDrivingQueue에서 제외시킨(Ignore) 경우
				for (int i = pos + 1; i < systemNodeList.size(); i++) {
					Node node = systemNodeList.get(i);
					if (nodeList.contains(node) == false) {
						nodeList.add(node);
						nodeTable.put(node, this);
					}
				}
			} else {
				// 기존에 systemDrivingQueue에서 추가한(Add) 경우 
				Node firstNode = null;
				if (OcsConstant.DIVERGE.equals(block.getBlockType())) {
					firstNode = block.getMainBlockNode();
				} else {
					firstNode = systemNodeList.firstElement();
				}
				while(true) {
					Node lastNode = nodeList.lastElement();
					if (firstNode.equals(lastNode)) {
						break;
					}
					if(lastNode.equals(systemNodeList.lastElement())) {
						break;
					}
					nodeList.remove(lastNode);
					nodeTable.remove(lastNode);
				}
			}

			// 2. 신규 사용자 Node를 반영한다.
			if (ADD.equals(type)) {
				for (Node node : userNodeList) {
					nodeList.add(node);
					nodeTable.put(node, this);
				}			
			} else if (IGNORE.equals(type)) {
				for (int i = userNodeList.size() - 1; i >= 0; i--) {
					Node node = userNodeList.get(i);
					nodeList.remove(node);
					nodeTable.remove(node);
				}
			}
		}
		
		userNodeType = type;
		return OK;
	}
	
	public String getDrivingQueueListString() {
		StringBuffer nodes = new StringBuffer();
		for (Node node : nodeList) {
			if (nodes.length() > 0) {
				nodes.append(",");
			}
			nodes.append(node.getNodeId());
		}
		
		return nodes.toString();
	}
}
