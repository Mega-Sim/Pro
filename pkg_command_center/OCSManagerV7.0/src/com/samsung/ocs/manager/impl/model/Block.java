package com.samsung.ocs.manager.impl.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsAlarmConstant;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsInfoConstant.FLOW_CONTROL_TYPE;
import com.samsung.ocs.manager.impl.BlockManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;

public class Block {
	private static final String OK = "OK";
	
	private String blockId;         // Block Id
	private String blockType;		// Block Type(합류 or 분기, 합류/분기 동시의 속성은 합류)
	private boolean multiType;		// 합류(Converge)이면서 분기(Diverge) 여부
	private Node mainBlockNode;     // 분기/합류 노드

	private Vector<VehicleData> drivingVehicleList;	// Block을 통과하기 위해 점유한 Vehicle List
	private ConcurrentHashMap<VehicleData, Long> drivingVehicleTable;	// Block 진입전 Path의 DrivingQueue(대기노드) List

	private LinkedList<Node> nodeList;      // Block 참조하는 Node 리스트(합류/분기 노드, 직진구간의 합류/분기와의 거리가 1300이내의 노드)  
	private LinkedList<Node> systemNodeList;
	private ConcurrentHashMap<String, DrivingQueueList> drivingQueueListTable;	// Block 진입전 Path의 DrivingQueue(대기노드) List

	// 2013.04.12 by KYK
	private ConcurrentHashMap<VehicleData, Double> timeToArriveTable;
	private Vector<VehicleData> drivingVehicleInDQList;	// DQ범위안으로 진입한 Vehicle List
	private ConcurrentHashMap<VehicleData, Long> drivingVehicleInDQTable; // DQ범위안 Vehicle 진입시각
	// 2013.05.16 by KYK
	private ConcurrentHashMap<VehicleData, Node> vehicleDQNodeTable; // DQ범위안 Vehicle 진입시각
	
//	private ArrayList<DrivingQueue> drivingQueueList;
	
	private BlockManager blockInfoManager;
	private OCSInfoManager ocsInfoManager;
	
	private static final String BLOCK_TRACE = "BlockDebug";
	private static Logger traceLog = Logger.getLogger(BLOCK_TRACE);
	private String lastLogMessage = "";
	private long lastLogMessageTime = System.currentTimeMillis();

	/**
	 * 
	 * @param node
	 * @param blockType
	 * @param multiType
	 * @param blockInfoManager
	 * @param ocsInfoManager
	 */
	public Block(Node node, String blockType, boolean multiType,
			BlockManager blockInfoManager, OCSInfoManager ocsInfoManager) {
		this.blockId = node.getNodeId();
		this.blockType = blockType;
		this.multiType = multiType;
		this.blockInfoManager = blockInfoManager;
		this.ocsInfoManager = ocsInfoManager; 

		mainBlockNode = node;
		drivingVehicleList = new Vector<VehicleData>();
		drivingVehicleTable = new ConcurrentHashMap<VehicleData, Long>();

		nodeList = new LinkedList<Node>();
		systemNodeList = new LinkedList<Node>();
		drivingQueueListTable = new ConcurrentHashMap<String, DrivingQueueList>();
		// 2013.04.12 by KYK
		timeToArriveTable = new ConcurrentHashMap<VehicleData, Double>();
		drivingVehicleInDQList = new Vector<VehicleData>();
		drivingVehicleInDQTable = new ConcurrentHashMap<VehicleData, Long>();
		vehicleDQNodeTable = new ConcurrentHashMap<VehicleData, Node>();
		
//		drivingQueueList = new ArrayList<DrivingQueue>();
	}
	
	public void addNode(Node node) {
		if (nodeList.contains(node) == false) {
			nodeList.add(node);
			systemNodeList.add(node);
			
			// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
			node.addCollisionBlock(this);
		}
	}
	
	public String getBlockNodeListString() {
		StringBuffer nodeListString = new StringBuffer();
		for (Node node : nodeList) {
			if (nodeListString.length() > 0) {
				nodeListString.append(",");
			}
			nodeListString.append(node.getNodeId());
		}
		return nodeListString.toString();
	}
	
	public String getSystemBlockNodeListString() {
		StringBuffer nodeListString = new StringBuffer();
		for (Node node : systemNodeList) {
			if (nodeListString.length() > 0) {
				nodeListString.append(",");
			}
			nodeListString.append(node.getNodeId());
		}
		return nodeListString.toString();
	}
	
	public String updateUserNode(Vector<Node> userNodeList) {
		synchronized (nodeList) {
			// 1. 합류인 경우 사용자 노드가 DrivingQueue에 포함된 노드인지 확인한다.
			if (OcsConstant.CONVERGE.equals(blockType)) {
				for (Node node : userNodeList) {
					// 2013.11.11 by MYM : MultiBlock(합류이면서 분기)인 경우 전방 간섭(C) Node도 Block으로 추가할 수 있도록 수정
					// 배경 : M1B 근접제어 전환시 MultiBlock중 전방 Node가 간섭이 설정된 구간이 존재하여 대응 필요
//					if (getDrivingQueueList(node) == null) {
					if (getDrivingQueueList(node) == null && this.multiType == false) {
						StringBuffer log = new StringBuffer();
						// "Node(xxxxxx) is not in the DrivingQueueList(Converge)"
						//log.append("Not in the DrivingQueueList(Converge) :").append(node.getNodeId());						
						log.append("Node(").append(node.getNodeId()).append(") is not in the DrivingQueueList(Converge)");
						return log.toString();
					}
				}
			}
			
			// 2. 기존(old) userNode를 추출한다. 이때, new userNodeList에 존재하는 것은 제외
			//    systemNodeList : 1
			//    nodeList : 1,2
			//    old userNodeList : 2
			//    new userNodeList : 3
			//    rmNodeList : 2
			Vector<Node> rmNodeList = new Vector<Node>(nodeList);
			for (Node rmNode : systemNodeList) {
				rmNodeList.remove(rmNode);
			}
			for (Node rmNode : userNodeList) {
				rmNodeList.remove(rmNode);
			}
			
			if (OcsConstant.CONVERGE.equals(blockType)) {
				// Converge인 경우
				// 3. userBlock이 DQ에서 연속하는지 확인
				//    DQ1: 1,2,3,4
				//    DQ2: 5,6,7,8
				//    userBlock
				//    case1. 1,2 -- OK	case2. 1,3 -- NG
				//    case3. 1,5 -- OK  case4. 6 -- NG
				Vector<Node> newBlockNodeList = new Vector<Node>(nodeList);
				for (Node node : userNodeList) {
					if (newBlockNodeList.contains(node) == false) {
						newBlockNodeList.add(node);					
					}
				}
				for (Node rmNode : rmNodeList) {
					newBlockNodeList.remove(rmNode);				
				}
				Vector<Node> forwardNodeList = new Vector<Node>(newBlockNodeList);
				for (Enumeration<DrivingQueueList> e= drivingQueueListTable.elements(); e.hasMoreElements();) {
					Vector<Node> dqNodeList = e.nextElement().getNodeList();
					for (Node node : newBlockNodeList) {
						int pos = dqNodeList.indexOf(node);
						if (pos < 0) {
							continue;
						} else if (pos == 0) {
							forwardNodeList.remove(node);
							continue;
						} else {
							forwardNodeList.remove(node);
							Node dqNode = dqNodeList.get(pos-1);
							if (newBlockNodeList.contains(dqNode) == false) {
								StringBuffer log = new StringBuffer();
								// "Node(xxxxxx) is not consecutive(Converge)"
								log.append("Node(").append(node.getNodeId()).append(") is not consecutive(Converge)");
								return log.toString();
							}
						}
					}
				}
				
				// 2013.11.11 by MYM : MultiBlock(분기) 노드를 기준으로 userBlockNode가 연속하는지 확인
				// 변경 : MultiBlock(합류이면서 분기)인 경우 전방 간섭(C) Node도 Block으로 추가할 수 있도록 수정
				// 배경 : M1B 근접제어 전환시 MultiBlock중 전방 Node가 간섭이 설정된 구간이 존재하여 대응 필요  
				if (this.multiType) {
					forwardNodeList.remove(this.mainBlockNode);
					for (Node node : forwardNodeList) { 
						for (int i = 0; i < node.getSectionCount(); i++) {
							Section section = node.getSection(i);
							if (node.equals(section.getFirstNode()) == false) {
								Node prevNode = section.getNode(section.getNodeIndex(node) - 1);
								if(newBlockNodeList.contains(prevNode) == false) {
									StringBuffer log = new StringBuffer();
									// "Node(xxxxxx) is not consecutive(Converge_Multi)"
									log.append("Node(").append(node.getNodeId()).append(") is not consecutive(Converge_Multi)");
									return log.toString();
								}
							}
						}
					}
				}
				
				// 4. 사용자 Node를 추가한다.
				for (Node node : userNodeList) {
					if (nodeList.contains(node) == false) {
						nodeList.add(node);
						
						// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
						node.addCollisionBlock(this);
					}
				}
				
				// 5. 기존 userNode를 제거한다.
				for (Node rmNode : rmNodeList) {
					nodeList.remove(rmNode);
					
					// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
					rmNode.removeCollisionBlock(this);
				}
			} else if (OcsConstant.DIVERGE.equals(blockType)) {
				// Diverge인 경우
				// 3. userBlock이 DQ에서 연속하는지 확인
				Vector<Node> newBlockNodeList = new Vector<Node>(nodeList);
				for (Node node : userNodeList) {
					if (newBlockNodeList.contains(node) == false) {
						newBlockNodeList.add(node);					
					}
				}
				for (Node rmNode : rmNodeList) {
					newBlockNodeList.remove(rmNode);				
				}
				for (Node node : newBlockNodeList) {
					if (node.equals(this.mainBlockNode)) {
						// 분기 노드인 경우 조건 만족
						continue;
					} else if (node.isConverge() || node.isDiverge()) {
//					} else if (node.getSectionCount() > 1) {
						// 다른 합류/분기 노드인 경우는 적용 불가
						StringBuffer log = new StringBuffer();
						// "Node(xxxxxx) is the other's converge or diverge node(Diverge)"
						//log.append("The other's coverge or diverge node(Diverge) :").append(node.getNodeId());						
						log.append("Node(").append(node.getNodeId()).append(") is the other's converge or diverge node(Diverge)");
						return log.toString();
					}
					// 분기 노드 이후의 DrivingQueueList에 포함된 Node는 Block으로 적용 불가
					for (Enumeration<DrivingQueueList> e = drivingQueueListTable.elements(); e.hasMoreElements();) {
						Vector<Node> dqNodeList = e.nextElement().getNodeList();
						if (dqNodeList.indexOf(node) >= dqNodeList.indexOf(mainBlockNode)) {
							StringBuffer log = new StringBuffer();
							// "Node(" + invalidNode + ") can not be apply"
							//log.append("not able to apply drivingQueueNode :").append(node.getNodeId());
							log.append("Node(").append(node.getNodeId()).append(") can not be apply");
							return log.toString();
						}
					}
					// 분기 노드를 기준으로 userBlockNode가 연속하는지 확인 
					for (int i = 0; i < node.getSectionCount(); i++) {
						Section section = node.getSection(i);
						if (node.equals(section.getFirstNode()) == false) {
							Node prevNode = section.getNode(section.getNodeIndex(node) - 1);
							if(newBlockNodeList.contains(prevNode) == false) {
								StringBuffer log = new StringBuffer();
							  // "Node(xxxxxx) is not consecutive(Diverge)"
								//log.append("Not consecutive(Diverge) :").append(node.getNodeId());								
								log.append("Node(").append(node.getNodeId()).append(") is not consecutive(Diverge)");
								return log.toString();
							}
						}
					}
				}

				// 4. 사용자 Node를 추가한다.
				for (Node node : userNodeList) {
					if (nodeList.contains(node) == false) {
						// DrivingQueueList에 Node가 존재하지 않으면 추가
						if (getDrivingQueueList(node) == null) {
							for (Enumeration<DrivingQueueList> e= drivingQueueListTable.elements(); e.hasMoreElements();) {
								DrivingQueueList drivingQueueList = e.nextElement();
								drivingQueueList.addBlockNode(node);
							}
						}
						nodeList.add(node);
						
						// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
						node.addCollisionBlock(this);
					}
				}
				
				// 5. 기존 userNode를 제거한다.
				for (Node rmNode : rmNodeList) {
					nodeList.remove(rmNode);
					
					// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
					rmNode.removeCollisionBlock(this);
					
					// Diverge이면서 DrivingQueueList에 Node가 존재하면 제거
					DrivingQueueList drivingQueueList = getDrivingQueueList(rmNode);
					if (drivingQueueList != null) {
						drivingQueueList.removeBlockNode(rmNode);
					}
				}
			} else {
				StringBuffer log = new StringBuffer();
				log.append("BlockType is wrong(").append(blockType).append(")");
				return log.toString();
			}
		}
		
		return OK;
	}
	
	/**
	 * 2013.05.10 by KYK
	 * @param userNodeList
	 * @return
	 */
	public String updateUserNode7(Vector<Node> userNodeList) {
		synchronized (nodeList) {			
			// Select old userBlockNodes (removeNodeList) 
			Vector<Node> rmNodeList = new Vector<Node>(nodeList);
			for (Node rmNode : systemNodeList) {
				rmNodeList.remove(rmNode);
			}
			for (Node rmNode : userNodeList) {
				rmNodeList.remove(rmNode);
			}
			// Create newBlockNodeList
			Vector<Node> newBlockNodeList = new Vector<Node>(nodeList);
			for (Node node : userNodeList) {
				if (newBlockNodeList.contains(node) == false) {
					newBlockNodeList.add(node);
				}
			}
			for (Node rmNode : rmNodeList) {
				newBlockNodeList.remove(rmNode);				
			}

			// 2013.07.30 by KYK
//			if (OcsConstant.CONVERGE.equals(blockType)) {
			if (isConvergeOrMultiType()) {
				Vector<Node> tempNodeList = new Vector<Node>();
				ConcurrentHashMap<Node, Vector<Node>> tempDQNodeMap = new ConcurrentHashMap<Node, Vector<Node>>();				
				for (DrivingQueueList dQ: drivingQueueListTable.values()) {
					Node dQNode = dQ.getFirstDQNode();
					Vector<Node> tempDQNodeList = new Vector<Node>();		
					if (newBlockNodeList.contains(dQNode)) {
						tempNodeList.add(dQNode);
						for (int i = 0; i < dQNode.getSectionCount(); i++) {
							Section section = dQNode.getSection(i);
							if (section != null) {
								int index = section.getNodeIndex(dQNode);
								while (index > 0) {
									Node node = section.getNode(index - 1);
									tempDQNodeList.add(node);
									if (newBlockNodeList.contains(node)) {
										// 2013.07.30 by KYK
										tempNodeList.add(node);
										index--;
									} else {
										break;
									}
								}
							}
						}
					}
					tempDQNodeMap.put(dQNode, tempDQNodeList);
				}
				// Consider multiType-Block
				if (this.isMultiType()) {
					for (int i = 0; i < mainBlockNode.getSectionCount(); i++) {
						Section section = mainBlockNode.getSection(i);
						if (section != null) {
							if (mainBlockNode.equals(section.getFirstNode())) {
								for (int j = 1; j < section.getNodeCount(); j++) {
									Node node = section.getNode(j);
									if (newBlockNodeList.contains(node)) {
										if (tempNodeList.contains(node) == false) {
											tempNodeList.add(node);											
										}
									} else {
										break;
									}
								}
							}
						}
					}
				}
				
				// Check if nodes is consecutive (validation).
				for (Node node: userNodeList) {
					if (tempNodeList.contains(node) == false) {
						StringBuffer log = new StringBuffer();
						log.append("Node(").append(node.getNodeId()).append(") is not consecutive(Converge)");
						return log.toString();
					}
				}
				// Update DrivingQueue : add New & remove Old
				for (DrivingQueueList dQ: drivingQueueListTable.values()) {
					Node dQNode = dQ.getFirstDQNode();
					if (tempDQNodeMap.containsKey(dQNode)) { // ??
						Vector<Node> tempDQNodeList = new Vector<Node>(tempDQNodeMap.get(dQNode));
						Vector<Node> removeDQNodeList = new Vector<Node>(dQ.getNodeList());
						removeDQNodeList.remove(dQNode);
						for (Node newDQNode: tempDQNodeList) {
							dQ.addUserNode(newDQNode);
							removeDQNodeList.remove(newDQNode);
						}
						for (Node removeNode: removeDQNodeList) {
							dQ.removeUserNode(removeNode);
						}
					}
				}				
				// Update UserBlock : add New & remove Old
				for (Node node : userNodeList) {
					if (nodeList.contains(node) == false) {
						nodeList.add(node);
						
						// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
						node.addCollisionBlock(this);
					}
				}
				for (Node rmNode : rmNodeList) {
					nodeList.remove(rmNode);
					
					// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
					rmNode.removeCollisionBlock(this);
				}
			// 2013.07.30 by KYK	
//			} else if (OcsConstant.DIVERGE.equals(blockType)) {
			} else if (isDivergeType()) {
				for (Node node : newBlockNodeList) {
					if (node.equals(this.mainBlockNode)) {
						continue;
					} else if (node.isConverge() || node.isDiverge()) {
						// Not Allowed : other converge or diverge
						StringBuffer log = new StringBuffer();
						log.append("Node(").append(node.getNodeId()).append(") is the other's converge or diverge node(Diverge)");
						return log.toString();
					}
				}
				
				Vector<Node> tempNodeList = new Vector<Node>(newBlockNodeList);
				for (int i = 0; i < mainBlockNode.getSectionCount(); i++) {
					Section section = mainBlockNode.getSection(i);
					if (mainBlockNode.equals(section.getLastNode())) {
						for (Node node: newBlockNodeList) {
							if (section.getNodeIndex(node) > 0) {
								if (node.equals(section.getLastNode()) == false) {
								StringBuffer log = new StringBuffer();
								log.append("Node(").append(node.getNodeId()).append(") can not be apply");
								return log.toString();
								}
							}
						}
					} else {
						Node tempNode = null;
						for (int j = 0; j < section.getNodeCount(); j++) {
							tempNode = section.getNode(j);
							if (tempNodeList.contains(tempNode) || mainBlockNode.equals(tempNode)) {
								tempNodeList.remove(tempNode);
							} else {
								break;
							}							
						}
					}
				}
				// Check if nodes is consecutive (validation).
				for (Node node : tempNodeList) {
					StringBuffer log = new StringBuffer();
					log.append("Node(").append(node.getNodeId()).append(") is not consecutive(Diverge)");
					return log.toString();
				}
				
				// Update UserBlock : add New & remove Old
				for (Node node : userNodeList) {
					if (nodeList.contains(node) == false) {
						nodeList.add(node);
						
						// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
						node.addCollisionBlock(this);
						
						if (getDrivingQueueList(node) == null) {
							for (DrivingQueueList dQ: drivingQueueListTable.values()) {
								dQ.addBlockNode(node);
							}
						}
					}
				}
				for (Node rmNode : rmNodeList) {
					nodeList.remove(rmNode);
					
					// 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
					rmNode.removeCollisionBlock(this);
					
					DrivingQueueList drivingQueueList = getDrivingQueueList(rmNode);
					if (drivingQueueList != null) {
						drivingQueueList.removeBlockNode(rmNode);
					}
				}
			} else {
				StringBuffer log = new StringBuffer();
				log.append("BlockType is wrong(").append(blockType).append(")");
				return log.toString();
			}
		}
		
		return OK;
	}
	
	/**
	 * 
	 */
	private String checkBlockResetForConverge(VehicleData drivingVehicle) {
		// DrivingVehicle(타Vehicle)이 일정시간동안 Block을 점유하여 Reset이 되지 않으면
		// DrivingVehicle이 DrivingQueueList에서 다음을 확인한다.
		// 1) Block Node를 통과 위한 Go 명령을 전송했는지 확인(Block을 이미 점유하였지만 장애로 인해 가지 못하고 다시 Auto로 조치가 된 경우 Reset)
		int blockResetTimeout = ocsInfoManager.getBlockResetTimeout();
		long vehicleOccupiedTime = getBlockOccupiedTime(drivingVehicle);
		if (Math.abs(System.currentTimeMillis() - vehicleOccupiedTime) < blockResetTimeout * 1000) {
			StringBuffer log = new StringBuffer();
			log.append("Block ResetTimeout is not over(").append(blockResetTimeout).append("sec)");
			return log.toString();
		}
		
		// Block 점유 Vehicle(drivingVehicle)이 Block을 지나는 Vehicle인지 확인
		if (checkVehicleInBlock(drivingVehicle)) {
			return "Waiting for occupied vehicle";
		}
		
		// Main BlockNode(ConvergeNode)를 경유하는 Vehicle이 존재하는지 확인
		for (int i = 0; i < mainBlockNode.getDriveVehicleCount(); i++) {
			VehicleData vehicle = mainBlockNode.getDriveVehicle(i);
			// 2012.07.13 by MYM : [NotNullCheck] 추가
			if (vehicle != null && vehicle.getDriveNodeIndex(mainBlockNode) > 0) {
				return "Waiting for occupied vehicle";
			}
		}
		
		return OcsConstant.OK;
	}
	
	/**
	 * 
	 * @param vehicle
	 * @return
	 */
	private boolean checkVehicleInBlock(VehicleData vehicle) {
		// Block을 점유 후 Manual -> Auto -> Manual일 경우 다른 Path에서 Vehicle 합류 미통과
		// DrivingQueue상에 존재한 경우는 체크하지 않도록 제거 : 테스트팀 Defect 발견 후 수정(2011.03.28)
		for (int i = 0; i < nodeList.size(); i++) {
			Node blockNode = nodeList.get(i);			
			for (int j = 0; j < blockNode.getDriveVehicleCount(); j++) {
				// 2012.03.02 by MYM : [NotNullCheck] 추가
				VehicleData blockVehicle = blockNode.getDriveVehicle(j);
				if (blockVehicle != null && vehicle.equals(blockVehicle))
					return true;
			}
		}
		// 2013.04.12 by KYK
		Node stopNode = vehicle.getDriveStopNode();
		if (stopNode != null) {
			if (getDrivingQueueList(stopNode) != null) {
				String stopStationId = vehicle.getStopStation();
				if (stopStationId != null && stopStationId.length() > 0) {
					return true;
				}
			}
		}
		return false;
	}
	
	private String getDrivingQueueListId(VehicleData vehicle) {
		// Vehicle의 위치해 있는 DrivingQueue의 Section 정보를 반환한다.
		Node currNode = vehicle.getDriveCurrNode();
		if (currNode == null) {
			return "";
		}
		DrivingQueueList drivingQueueList = getDrivingQueueList(currNode);
		if (drivingQueueList == null) {
			return "";
		}
		
		return drivingQueueList.getId();
	}
	
	public void setDrivingVehicle(VehicleData vehicle) {
		// 2012.06.11 by MYM : Function Sync에서 drivingVehicleList만 Sync 하도록 수정 
		//                     drivingVehicleList Vehicle 중복 등록 조건 추가
		synchronized (drivingVehicleList) {
			if (drivingVehicleTable.putIfAbsent(vehicle, new Long(System.currentTimeMillis())) == null) {
				drivingVehicleList.add(vehicle);
			}
		}
	}

	/**
	 * 2013.04.12 by KYK (2014.02.20 by KYK)
	 * @param firstVehicle
	 * @return
	 */
	private double getTimeToArriveAtDQNode(VehicleData firstVehicle) {
		if (firstVehicle != null) {
			Double timeToArrive = timeToArriveTable.get(firstVehicle);
			if (timeToArrive != null) {
				return timeToArrive.doubleValue();
			}
		}
		return 9999;
	}

	/**
	 * 2013.04.12 by KYK
	 * @param vehicle
	 * @param timeToArrive
	 */
	public void setTimeToArriveAtDQNode(VehicleData vehicle, double timeToArrive) {
		synchronized (drivingVehicleList) {
			timeToArriveTable.put(vehicle, timeToArrive);
		}
	}
	
	/**
	 * 2013.04.12 by KYK (2014.02.20 by KYK)
	 * @param firstVehicle
	 * @return
	 */
	private long getEnteredTime(VehicleData firstVehicle) {
		if (firstVehicle != null) {
			Long enteredTime = drivingVehicleInDQTable.get(firstVehicle);
			if (enteredTime != null) {
				return enteredTime.longValue();
			}
		}
		return System.currentTimeMillis();
	}
	
	/**
	 * 2013.05.16 by KYK
	 * @param vehicle
	 * @param dQNode
	 */
	public void setEnteringTimeInDQRange(VehicleData vehicle, Node dQNode) {
		synchronized (drivingVehicleInDQList) {
			if (drivingVehicleInDQTable.putIfAbsent(vehicle, new Long(System.currentTimeMillis())) == null) {
				drivingVehicleInDQList.add(vehicle);
				vehicleDQNodeTable.put(vehicle, dQNode);
			}
		}
	}

	/**
	 * 2014.02.20 by KYK
	 * @param vehicle
	 */
	public void resetDQEnteringInfo(VehicleData vehicle) {
		drivingVehicleInDQTable.remove(vehicle);
		drivingVehicleInDQList.remove(vehicle);
		timeToArriveTable.remove(vehicle);
		vehicleDQNodeTable.remove(vehicle);
		vehicle.resetEnteringBlockList(this);
	}

	public void resetDrivingVehicle(VehicleData vehicle) {
		// 2012.06.11 by MYM : Function Sync에서 drivingVehicleList만 Sync 하도록 수정
		synchronized (drivingVehicleList) {
			drivingVehicleTable.remove(vehicle);
			drivingVehicleList.remove(vehicle);
			// 2014.02.20 by KYK
			resetDQEnteringInfo(vehicle);
			
			// 2015.06.03 by MYM : Block Reset 로그 추가
			StringBuffer log = new StringBuffer();
			log.append(blockId).append("> Reset ").append(vehicle);
			trace(log.toString());
		}
		// 2013.07.31 by KYK
		vehicle.resetOccupiedBlockList(this);
	}

	public VehicleData getDrivingVehicle() {
		synchronized (drivingVehicleList) {
			if (drivingVehicleList.size() > 0) {
				return drivingVehicleList.firstElement();
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public String getDrivingVehicles() {
		StringBuffer sb = new StringBuffer();
		Vector<VehicleData> drivingVehicleListClone = (Vector<VehicleData>) drivingVehicleList.clone();
		for (int i = 0; i < drivingVehicleListClone.size(); i++) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(drivingVehicleListClone.get(i).getVehicleId());
		}
		return sb.toString();
	}
	
	public boolean containsDrivingVehicle(VehicleData vehicle) {
		return drivingVehicleList.contains(vehicle);
	}
	
	private long getBlockOccupiedTime(VehicleData vehicle) {
		Long occupiedTime = drivingVehicleTable.get(vehicle);
		if (occupiedTime != null) {
			return occupiedTime.longValue();
		}
		return System.currentTimeMillis();
	}
	
	public String getBlockId() {
		return blockId;
	}
	
	public String getBlockType() {
		return blockType;
	}
	
	/**
	 * 2013.07.30 by KYK
	 * @return
	 */
	public boolean isConvergeOrMultiType() {
		if (OcsConstant.CONVERGE.equals(getBlockType()) || OcsConstant.MULTI.equals(getBlockType())) {
			return true;
		}
		return false;
	}
	
	/**
	 * 2013.07.30 by KYK
	 * @return
	 */
	public boolean isDivergeType() {
		if (OcsConstant.DIVERGE.equals(getBlockType())) {
			return true;
		}
		return false;
	}
	
	public boolean isMultiType() {
		return multiType;
	}
	
	public Node getMainBlockNode() {
		return mainBlockNode;
	}
	
	public boolean checkNodeInBlock(Node node) {
		return nodeList.contains(node);
	}

	public int getBlockNodeCount() {
		return nodeList.size();
	}
	
	public Node getBlockNode(int index) {
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (index < 0 || index >= nodeList.size()) {
			return null;
		}
		return nodeList.get(index);
	}
	
	/**
	 * 2015.09.16 by MYM : Block의 User/System Block 위치에 장애 Vehicle 고려 경로 탐색
	 */
	public LinkedList<Node> getNodeList() {
		return nodeList;
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	public boolean containsInBlockNode(Node node) {
		return nodeList.contains(node);
	}
	
	/**
	 * 
	 * @param drivingQueueList
	 */
	public void addDrivingQueueList(DrivingQueueList drivingQueueList) {
		// 2012.03.02 by MYM : [NotNullCheck] 추가
		if (drivingQueueList != null) {
			drivingQueueListTable.put(drivingQueueList.getId(), drivingQueueList);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public ConcurrentHashMap<String, DrivingQueueList> getDrivingQueueListTable() {
		return drivingQueueListTable;
	}
	
	/**
	 * 
	 * @param drivingQueueId
	 * @return
	 */
	public DrivingQueueList getDrivingQueueList(String drivingQueueId) {
		return drivingQueueListTable.get(drivingQueueId);
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	private DrivingQueueList getDrivingQueueList(Node node) {
		for (Enumeration<DrivingQueueList> e = drivingQueueListTable.elements(); e.hasMoreElements();) {
			DrivingQueueList drivingQueueList = e.nextElement();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if(drivingQueueList != null && drivingQueueList.getDrivingQueueList(node) != null) {
				return drivingQueueList;
			}
		}
		return null;
	}
	
	/**
	 * 2012.06.01 by MYM : Sync 제거 vehicle의 Drive에서 Block 단위로 Sync 하도록 수정</br>
	 * 2014.08.21 by zzang9un : Drive Fail 경우 Caused Vehicle을 추가하도록 수정
	 * @param vehicle
	 * @return "OK" if normal, reason(String) otherwise
	 */
	public String checkFirstVehicleInConverge(VehicleData vehicle) {
//		public synchronized String checkFirstVehicleInConverge(VehicleData vehicle) {
		// Vehicle이 DrivingQueueList에서 첫번째 위치하고 있는지 확인한다.
		
		// 1. Vehicle의 현재 위치가 DrivingQueueList에 위치하고 있는지 확인한다.
		Node currNode = vehicle.getDriveCurrNode();
		if (currNode == null) {
			return "Error (Vehicle CurrNode is null.)";
		}
		DrivingQueueList drivingQueueList = getDrivingQueueList(currNode);
		if (drivingQueueList == null) {
			// DrivingQueue에는 미존재시 Block 노드에 Vehicle이 존재하는 경우는 통과
			if (nodeList.contains(currNode)) {
				StringBuffer log = new StringBuffer();
				log.append(blockId).append("> NONE  [").append(blockId).append("] PASS : ").append(vehicle.getVehicleId()).append("(");
				log.append(vehicle.getDriveCurrNode().getNodeId()).append(") - in Block Node");
				trace(log.toString());
				
				// 2011.10.27 by PMM & MYM
				this.setDrivingVehicle(vehicle);
				return OcsConstant.OK;
			}
			
			// 2014.09.22 by zzang9un : Vehicle이 DrivingQueue 범위 밖인 경우는 
			// Drive한 DrivingQueue에 있는 첫번째 Vehicle을 caused vehicle로 등록한다.
			VehicleData firstVehicle = null;
			DrivingQueueList drvQueue = getDrivingQueueList(vehicle.getDriveStopNode());
			
			if (drvQueue != null) {
				for (int i = 0; i < drvQueue.size(); i++) {
					Node node = drvQueue.get(i);

					if (node != null) {
						firstVehicle = node.getArrivedVehicle();
						if (firstVehicle != null) {
							break;
						}
					} else {
						break;
					}
				}
				if (firstVehicle != null) {
					vehicle.addVehicleToDriveFailCausedVehicleList(firstVehicle, true);
					
					return "Vehicle is not in drivingQueue range."  + "(CausedVehicle:" + firstVehicle.toString() + ")"; 
				}
			}
			
			return "Vehicle is not in drivingQueue range.";
		}

		// 2. Vehicle이 위치한 DrivingQueueList에서 첫번째 통과 Vehicle을 찾는다.
		VehicleData firstVehicle = null;
		for (int i = 0; i < drivingQueueList.size(); i++) {
			Node node = drivingQueueList.get(i);
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (node != null) {
				firstVehicle = node.getArrivedVehicle();
				if (firstVehicle != null) {
					break;
				}
			} else {
				break;
			}
		}

		// 3. DrivingQueueList에 위치하고 있는 Vehicle이 존재하지 않으면 false를 리턴
		if (firstVehicle == null) {
			return "Error (Vehicle is not in DrivingQueue.)";
		}

		// 4. 첫번째 Vehicle이 아니면 통과 불가
		if (vehicle.equals(firstVehicle) == false) {
			// IDLE Vehicle이 존재하고 있으면 양보 요청
			if (firstVehicle.hasArrivedAtTargetNode()) {
				// 2012.02.06 by PMM
//				firstVehicle.setYieldRequest(true);
//				firstVehicle.setYieldRequestedVehicle(vehicle);
				firstVehicle.requestYield(vehicle);
			}
			
			// 2014.08.21 by zzang9un : Caused Vehicle로 추가
			vehicle.addVehicleToDriveFailCausedVehicleList(firstVehicle, true);
			
			// 2014.09.12 by zzang9un : Caused Vehicle 표시
			return "Vehicle is not first in drivingQueue(1)." + "(CausedVehicle:" + firstVehicle.toString() + ")";
		} else {
			// 다음과 같은 구간에서 Vehicle이 1번  커브를 돌아 가지 않고 2번 커브로 주행을 해서 합류를 지날 때
			// 분기를 통과하고 나서 1번 커브구간으로의 합류를 통과하는 조건을 볼 가능성이 있어 StopNode를 기준으로
			// 통과 여부를 확인하도록 조건을 추가
			//    |   |
			//  ↓ | 1 | ↑
			//    |\_/|
			//    |   |
			//  ↓ | 2 | ↑
			//     \_/
			Node stopNode = vehicle.getDriveStopNode();
			DrivingQueueList stopNodeDrivingQueueList = getDrivingQueueList(stopNode);
			
			if (drivingQueueList.equals(stopNodeDrivingQueueList) == false) {
				return "Vehicle is not first in drivingQueue(2)." + "(CausedVehicle:" + firstVehicle.toString() + ")";
			}
		}

		// 5. Block을 통과하는 Vehicle이 존재하는지 확인한다.
		// 2012.06.09 by MYM : Block의 drivingVehicle를 List로 변경
		VehicleData drivingVehicle = getDrivingVehicle();
		String result = "";
		if (drivingVehicle != null) {
			result = existDrivingVehicle(drivingVehicle, firstVehicle, drivingQueueList);
		} else {
			result = notExistDrivingVehicle(firstVehicle, drivingQueueList);
			
			// 2014.08.28 by zzang9un : "Waiting for other Path Vehicle"인 경우 other Path의 vehicle을 causedVehicle로 등록
			if (result.indexOf("Waiting for other Path Vehicle") >= 0) {
				VehicleData oppositeVehicle = getOppositeFirstVehicle(drivingQueueList);
				
				if (oppositeVehicle != null) {
					vehicle.addVehicleToDriveFailCausedVehicleList(oppositeVehicle, true);
					
					// 2014.09.12 by zzang9un : Caused Vehicle 표시
					result = result.concat("(CausedVehicle:" + oppositeVehicle.toString() + ")");
				}	
			}
		}
		if (OcsConstant.OK.equals(result)) {
			this.setDrivingVehicle(vehicle);
		}
		return result;
	}
	
	/**
	 * 2013.02.08 by KYK
	 * @param vehicle
	 * @return
	 */
//	public String checkFirstVehicleInConverge7(VehicleData vehicle, OCSInfoManager ocsInfoManager) {
	public String checkFirstVehicleInConverge7(VehicleData vehicle, OCSInfoManager ocsInfoManager, String stationId) {
		
		// 0. Get DrivingQueue Node in this Block		
		DrivingQueueList drivingQueue = getDrivingQueue(vehicle);
		if (drivingQueue == null) {
			return "DQ Not Exist";
		}
		// 2013.05.10 by KYK
		Node dQNode = drivingQueue.getFirstDQNode();
//		Node dQNode = drivingQueue.getLastDQNode();
		
		// 1. Check if Vehicle is in DQLimitTime.
		double dQLimitTime = 1;
		String linkType = drivingQueue.getType();
		if (OcsConstant.LINE.equals(linkType)) {
			dQLimitTime = ocsInfoManager.getDrivingQueueLineLimitTime();
		} else if (OcsConstant.CURVE.equals(linkType)) {
			dQLimitTime = ocsInfoManager.getDrivingQueueCurveLimitTime();
		}
		dQLimitTime = getDQLimitTime(drivingQueue, dQLimitTime);
		
		// 2014.12.02 by zzang9un : DQ의 First Vehicle을 찾는다.
		// 기존 : 아래 2번 항목에서만 first vehicle을 찾았지만
		// 변경 : DQ range가 아닌 경우도 first vehicle을 caused vehicle로 등록한다.
		VehicleData firstVehicle = vehicle.getDQFirstVehicle(dQNode);
		if (checkVehicleInDQLimit(vehicle, dQNode, dQLimitTime) == false) {
			if ((firstVehicle != null) && (firstVehicle != vehicle)) {
				vehicle.addVehicleToDriveFailCausedVehicleList(firstVehicle, true);
				
				return "Vehicle is not in DQ range(Converge), LimitTime:" + dQLimitTime +
						"(CausedVehicle:" + firstVehicle.toString() + ")";
			}
			return "Vehicle is not in DQ range(Converge), LimitTime:" + dQLimitTime;
		}
		
		// 2. Find First Vehicle in DrivingQueue (DriveNodeList)		
		firstVehicle = vehicle.getDQFirstVehicle(dQNode);
		if (firstVehicle == null) {
			return "Error (Vehicle is not in DrivingQueue.)";
		}
		
		// 3. Request Yield to First Vehicle
		if (vehicle.equals(firstVehicle) == false) {
			if (firstVehicle.hasArrivedAtTarget()) {
				firstVehicle.requestYield(vehicle);
			}
			
			// 2014.11.06 by zzang9un : First Vehicle을 Caused Vehicle로 등록한다.
			vehicle.addVehicleToDriveFailCausedVehicleList(firstVehicle, true);
			
			return "Vehicle is not first in drivingQueue(1)." + "(CausedVehicle:" + firstVehicle.toString() + ")";
		}
		
		// 4. Check if Vehicle in Block Exists.
		// 2013.07.03 by KYK : 충돌우려.. (한쪽은 통과목적 한쪽은 간섭구간 도착목적일때)
		String result = "";
		synchronized (this) {
			VehicleData drivingVehicle = getDrivingVehicle();
			if (drivingVehicle != null) {
//			result = existDrivingVehicle7(drivingVehicle, firstVehicle, dQNode);
				result = existDrivingVehicle7(drivingVehicle, firstVehicle, drivingQueue);
			} else {
//			result = notExistDrivingVehicle7(firstVehicle, dQNode, ocsInfoManager.getFlowControlType());
				result = notExistDrivingVehicle7(firstVehicle, drivingQueue, ocsInfoManager.getFlowControlType());
			}
			if (OcsConstant.OK.equals(result)) {
				this.setDrivingVehicle(vehicle);
				// 2013.07.31 by KYK
				vehicle.setOccupiedBlockList(this, stationId);
			}			
		}
		return result;
	}

	/**
	 * 2013.04.19 by KYK
	 * @param drivingQueue
	 * @param systemDQLimitTime
	 * @return
	 */
	private double getDQLimitTime(DrivingQueueList drivingQueue, double systemDQLimitTime) {
		double dQLimitTime = systemDQLimitTime;
		if (drivingQueue != null) {
			double userDQLimitTime = drivingQueue.getUserDQLimitTime();
			if (userDQLimitTime > 0) {
				dQLimitTime = userDQLimitTime;
			}
		}
		return dQLimitTime;
	}

	/**
	 * 2013.04.19 by KYK
	 * @param vehicle
	 * @return
	 */
	private DrivingQueueList getDrivingQueue(VehicleData vehicle) {
		DrivingQueueList drivingQueue = null;		
		for (DrivingQueueList dQ : drivingQueueListTable.values()) {
//			Node tempNode = dQ.getLastDQNode();
			Node tempNode = dQ.getFirstDQNode();
			if (vehicle.containsDriveNode(tempNode)) {
				drivingQueue = dQ;
				break;
			}
			if (vehicle.containsRoutedNode(tempNode)) {
				if (drivingQueue == null) {
					drivingQueue = dQ;
				} else {
					// (Rare but)In case vehicle has both DQNodes in routedNodeList. 
					int indexA = vehicle.getRoutedNodeIndex(tempNode);
					int indexB = vehicle.getRoutedNodeIndex(drivingQueue.getFirstDQNode());
					if (indexA < indexB) {
						drivingQueue = dQ;
					}
				}
			}
		}
		return drivingQueue;
	}
	
//	/**
//	 * 2013.04.05 by KYK
//	 * @param vehicle
//	 * @return
//	 */
//	private Node getDrivingQueueNodeInCovergeBlock(VehicleData vehicle) {
//		Node dQNode = null;		
//		for (Enumeration<DrivingQueueList> e = drivingQueueListTable.elements(); e.hasMoreElements();) {
//			DrivingQueueList dQList = e.nextElement();
//			Node tempNode = dQList.get(0);
//			if (vehicle.containsDriveNode(tempNode)) {
//				dQNode = tempNode;
//				break;
//			}
//			if (vehicle.containsRoutedNode(tempNode)) {
//				dQNode = tempNode;
//			}
//		}
//		return dQNode;
//	}
//
//	/**
//	 * 2013.04.05 by KYK
//	 * @param dQNode
//	 * @return
//	 */
//	private VehicleData getFirstVehicleInDrivingQueue(Node dQNode) {
//		if (dQNode == null) {
//			return null;
//		}
//		VehicleData firstVehicle = null;
//		VehicleData tempVehicle = null;
//		Node node = null;		
//		if (dQNode.getDriveVehicleCount() > 0) {
//			tempVehicle = dQNode.getDriveVehicle(0);
//			for (int j = tempVehicle.getDriveNodeCount()-1; j >= 0; j--) {
//				node = tempVehicle.getDriveNode(j);
//				for (int i = 0; i < node.getArrivedVehicleCount(); i++) {
//					tempVehicle = node.getArrivedVehicle(i);
//					if (firstVehicle == null) {
//						firstVehicle = tempVehicle;
//					} else {
//						if (firstVehicle.getCurrNodeOffset() < tempVehicle.getCurrNodeOffset()) {
//							firstVehicle = tempVehicle;		
//						}
//					}
//				}
//				if (firstVehicle != null) {
//					break;
//				}
//			}
//		}
//		return firstVehicle;
//	}
	
	/**
	 * 2013.05.16 by KYK
	 * @param dQNode
	 * @return
	 */
	private VehicleData getDQFirstVehicle_old(Node dQNode) {
		if (dQNode == null) {
			return null;
		}
		VehicleData firstVehicle = null;
		Node node = null;
		
		ArrayList<VehicleData> vehicleList = new ArrayList<VehicleData>();
		for (VehicleData vehicle : vehicleDQNodeTable.keySet()) {
			node = vehicleDQNodeTable.get(vehicle);
			if (dQNode.equals(node)) {
				vehicleList.add(vehicle);
			}
		}
		// Return if no vehicle's tried to pass block.
		if (vehicleList.isEmpty()) {
			return null;
		}
		double min = 9999;
		double remainingTime = 0.0;
		for (VehicleData vehicle : vehicleList) {
			remainingTime = timeToArriveTable.get(vehicle);
			if (min > remainingTime) {
				min = remainingTime;
				firstVehicle = vehicle;
			}
		}
		return firstVehicle;
	}
	
	/**
	 * 2013.06.25 by KYK (2014.02.14 Revised)
	 * @param dQNode
	 * @return
	 */	
	private VehicleData getDQFirstVehicle(Node dQNode) {
		VehicleData vehicle = null;
		Node node = null;
		
		for (VehicleData tempVehicle : vehicleDQNodeTable.keySet()) {
			node = vehicleDQNodeTable.get(tempVehicle);
			if (node != null) {
				if (node.equals(dQNode)) {
					vehicle = tempVehicle;
					break;
				}
			}
		}
		// Return if no vehicle's tried to pass block.
		if (vehicle == null) {
			return null;
		}
		// 2014.02.14 by KYK
		return vehicle.getDQFirstVehicle(dQNode);
	}
	
//	/**
//	 * 2013.05.10 by KYK
//	 * @param vehicle
//	 * @param dQNode
//	 * @return
//	 */
//	private VehicleData getFirstVehicleInDrivingQueue(VehicleData vehicle, Node dQNode) {
//		if (dQNode == null) {
//			return null;
//		}
//		VehicleData firstVehicle = null;
//		VehicleData tempVehicle = null;
//		Node node = null;		
//		
//		List<Node> nodeList = null;
//		nodeList = new ArrayList<Node>(vehicle.getDriveNodeList());
//		if (nodeList.contains(dQNode) == false) {
//			nodeList.addAll(new ArrayList<Node>(vehicle.getRoutedNodeList()));
//		}
//		int endIndex = nodeList.indexOf(dQNode);
//		int currIndex = nodeList.indexOf(vehicle.getDriveCurrNode());
//		
//		for (int i = endIndex; i >= currIndex; i--) {
//			node = nodeList.get(i);
//			for (int j = 0; j < node.getArrivedVehicleCount(); j++) {
//				tempVehicle = node.getArrivedVehicle(j);
//				if (firstVehicle == null) {
//					firstVehicle = tempVehicle;
//				} else {
//					if (firstVehicle.getCurrNodeOffset() < tempVehicle.getCurrNodeOffset()) {
//						firstVehicle = tempVehicle;		
//					}					
//				}
//			}
//			if (firstVehicle != null) {
//				break;
//			}
//		}
//		return firstVehicle;		
//	}

	/**
	 * 2012.06.01 by MYM : Sync 제거 vehicle의 Drive에서 Block 단위로 Sync 하도록 수정</br>
	 * 2014.08.21 by zzang9un : Error인 경우 Caused Vehicle을 추가하도록 수정
	 * @param vehicle
	 * @return "OK" if normal, reason(String) otherwise  
	 */
	public String checkFirstVehicleInDiverge(VehicleData vehicle) {
//		public synchronized String checkFirstVehicleInDiverge(VehicleData vehicle) {
		// Vehicle이 DrivingQueue List에서 첫번째 위치해 있는 경우 분기를 통과
		// Vehicle의 선행 Vehicle이 분기를 통과중에 동일 방향으로 주행 예정인 경우 분기 통과

		// 1. Vehicle의 현재 위치가 DrivingQueue 노드에 위치해 있는지 확인한다.
		Node currNode = vehicle.getDriveCurrNode();
		if (currNode == null) {
			return "Error (Vehicle CurrNode is null.)";
		}
		DrivingQueueList drivingQueueList = getDrivingQueueList(currNode);
		if (drivingQueueList == null) {
			// 2014.08.22 by zzang9un : drivingQueue 제일 앞 vehicle을 caused vehicle로 등록
			Node stopNode = vehicle.getDriveStopNode();
			VehicleData causedVehicle = null;
			if (stopNode != null) {
				for (int i = 0; i < stopNode.getArrivedVehicleCount(); i++) {
					causedVehicle = stopNode.getArrivedVehicle(i);
					
					// causedVehicle의 currNode가 stopNode와 같은지 확인
					if (causedVehicle.getCurrNode().equals(stopNode.getNodeId())) {
						break;
					}
				}
			}
			
			if (causedVehicle != null) {
				vehicle.addVehicleToDriveFailCausedVehicleList(causedVehicle, true);
				
				// 2014.09.12 by zzang9un : Caused Vehicle 표시
				return "Vehicle is not in drivingQueue range." + "(CausedVehicle:" + causedVehicle.toString() + ")";
			} 
			
			return "Vehicle is not in drivingQueue range.";	
		}

		// 2. Vehicle이 위치한 DrivingQueueList에서 First Vehicle을 기준으로
		// 선행 Vehicle과 동일 방향이면 분기 Block(DIVERGE)를 통과시킨다.
		VehicleData occupiedVehicle = null;
		VehicleData prevVehicle = null;
		for (int i = 0; i < drivingQueueList.size(); i++) {
			Node drivingQueueNode = drivingQueueList.get(i);
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (drivingQueueNode == null) {
				return "Error (drivingQueueNode is null.)";
			}
			
			for (int j = 0; j < drivingQueueNode.getArrivedVehicleCount(); j++) {
				occupiedVehicle = drivingQueueNode.getArrivedVehicle(j);
				// 2012.03.02 by MYM : [NotNullCheck] 추가
				if (occupiedVehicle == null) {
					return "Error (occupiedVehicle is null.)";
				}
				
				// 1. First Vehicle인 경우 통과
				if (prevVehicle == null && vehicle.equals(occupiedVehicle)) {
					StringBuffer log = new StringBuffer();
					log.append(blockId).append("> ").append(vehicle.getVehicleId()).append("[").append(drivingQueueList.getId()).append("_").append(drivingQueueList.getType());
					log.append("] PASS(").append(drivingQueueNode.getNodeId()).append(")").append(" - First Vehicle");
					trace(log.toString());
					return OcsConstant.OK;
				}

				// 2. 분기 통과가 확정된 Vehicle과 동일 방향인지 판단을 위해 해당 Vehicle의 주행방향(분기 다음 노드)를 찾는다.
				if (prevVehicle == null) {
					prevVehicle = occupiedVehicle;
				}
				Node nextMoveNode = null;
				for (int k = 0; k < nodeList.size(); k++) {
					Node node = (Node) nodeList.get(k);
					// 2012.03.02 by MYM : [NotNullCheck] 추가
					if (node == null) {
						return "Error (node is null.)";
					}
					
					// DriveNodeList에 block 노드 포함 확인
					int index = prevVehicle.getDriveNodeIndex(node);
					if (index >= 0) {
						if ((index + 1) < prevVehicle.getDriveNodeCount()) {
							nextMoveNode = prevVehicle.getDriveNode(index + 1);
							break;
						}

						if (mainBlockNode.equals(node) == false) {
							nextMoveNode = node;
						}
						break;
					}
				}
				
			// 2015.01.15 by zzang9un : 경로 동일 방향, 직선/곡선 여부 판단 조건문 통합
				if (vehicle.equals(occupiedVehicle)) {
					String reason = "";
					
					// 1. 동일방향인지 판단
					if (occupiedVehicle.containsRoutedNode(nextMoveNode)) {
						// 2. 직선/곡선 판단
						if (OcsConstant.LINE.equals(nextMoveNode.getLinkType(mainBlockNode))) {
							// 직선
							StringBuffer log = new StringBuffer();
							// BK_D_145054> OHT258[Link1812_LINE] PASS(145053) : OHT258 - Going in the same direction with PrevVehicle(OHT238), NextMoveNode(145055)
							log.append(blockId).append("> ").append(vehicle.getVehicleId()).append("[").append(drivingQueueList.getId()).append("_").append(drivingQueueList.getType());
							log.append("] PASS(").append(drivingQueueNode.getNodeId()).append(") - Going in the same direction with Previous Vehicle(").append(prevVehicle.getVehicleId());
							log.append("), Next Move Node(").append(nextMoveNode).append(")");
							trace(log.toString());
							
							reason = OcsConstant.OK; // Drive 
						} else {
							// 곡선
							reason = "Waiting for previous vehicle(same direction but curve)" + "(CausedVehicle:" + prevVehicle.toString() + ")";
						}
					} else {
						// 다른 방향
						reason = "Waiting for previous vehicle(No same direction)" + "(CausedVehicle:" + prevVehicle.toString() + ")";
					}
					
					if (reason.equals(OcsConstant.OK) == false) {
						// Drive fail이므로 prevVehicle을 CausedVehicle로 등록
						vehicle.addVehicleToDriveFailCausedVehicleList(prevVehicle, true);
						
						// (같은 방향 && 직선)인 경우를 제외하고 무조건 양보 요청
						// 선행 OHT가 IDLE이면 양보 요청 : 테스트팀 Defect 발견 후 수정(2011.03.28)
						if (prevVehicle.isYieldRequested() == false 
								&& prevVehicle.hasArrivedAtTargetNode()) {
							prevVehicle.requestYield(vehicle);
						}
						
						// 2015.05.27 by MYM : 선행 Vehicle로 인해 주행 불가시 현재 시작으로 업데이트
						// 배경 : 분기 - 합류로 구간을 통과하는 Vehicle이 분기에 위치해 있는 경우
						//       전방 선행 Vehicle로 인해서 주행 못하는 경우 합류의 다른쪽 Path의 Vehicle이
						//       먼저 통과할 수 있도록 함.
						reArrangeVehicleArrivedTime(vehicle);
					}
					
					return reason;
				}
				prevVehicle = occupiedVehicle;
			}
		}

		// 2018.08.21 by zzang9un : Caused Vehicle로 추가
		if (prevVehicle != null) {
			vehicle.addVehicleToDriveFailCausedVehicleList(prevVehicle, true);
			
			// 2014.09.12 by zzang9un : Caused Vehicle 표시
			return "Waiting for previous vehicle." + "(CausedVehicle:" + prevVehicle.toString() + ")";
		} 
		
		return "Waiting for previous vehicle.";
	}
	
	/**
	 * 2013.02.08 by KYK
	 * @param vehicle
	 * @return
	 */
	public String checkFirstVehicleInDiverge7(VehicleData vehicle, double systemDQLimitTime) {
		
		// 2013.04.05 by KYK
		DrivingQueueList drivingQueue = getDrivingQueueList(mainBlockNode);
		double dQLimitTime = getDQLimitTime(drivingQueue, systemDQLimitTime);
		
		// 2014.12.02 by zzang9un : DQ의 첫번째 vehicle을 찾는다.
		DrivingQueueList drivingQueueList = getDrivingQueueList(vehicle.getDriveCurrNode());
		VehicleData firstVehicle = null;
		if (drivingQueueList == null) {
			Node stopNode = vehicle.getDriveStopNode();
			
			if (stopNode != null) {
				for (int i = 0; i < stopNode.getArrivedVehicleCount(); i++) {
					firstVehicle = stopNode.getArrivedVehicle(i);
					
					// causedVehicle의 currNode가 stopNode와 같은지 확인
					if (firstVehicle.getCurrNode().equals(stopNode.getNodeId())) {
						break;
					}
				}
			}
		}
		
		//if (checkVehicleInDrivingQueueRange(vehicle, mainBlockNode, dQLimitTime) == false) {
		if (checkVehicleInDQLimit(vehicle, mainBlockNode, dQLimitTime) == false) {
			// 2014.12.02 by zzang9un : DQ에 있지 않더라도 DQ의 첫번째 vehicle을 causedVehicle로 등록한다.
			if ((firstVehicle != null) && (firstVehicle != vehicle)) {
				vehicle.addVehicleToDriveFailCausedVehicleList(firstVehicle, true);
				
				return "Vehicle is not in DQ range(Diverge) ,LimitTime:" + dQLimitTime +
						"(CausedVehicle:" + firstVehicle.toString() + ")";
			}			
			return "Vehicle is not in DQ range(Diverge) ,LimitTime:" + dQLimitTime;
		}
		
		return checkVehicleInDiverge(vehicle, drivingQueue);		
	}
	
//	/**
//	 * 2013.03.29 by KYK
//	 * @param vehicle
//	 * @return
//	 */
//	private String checkVehicleInDiverge(VehicleData vehicle) {
//		// if First Vehicle, Pass
//		// else if Same Direction as first vehicle, Pass
//		// Request Yield
//		VehicleData occupiedVehicle = null;
//		VehicleData prevVehicle = null;
//		ArrayList<VehicleData> arrivedVehicleList = new ArrayList<VehicleData>();
//		List<Node> driveNodeList = new ArrayList<Node>(vehicle.getDriveNodeList());
//		int currIndex = driveNodeList.indexOf(vehicle.getDriveCurrNode());
//
//		for (int i = driveNodeList.size()-1; i >= currIndex; i--) {
//			Node drivingQueueNode = driveNodeList.get(i);
//			if (drivingQueueNode == null) {
//				return "Error (drivingQueueNode is null.)";
//			}			
//			arrivedVehicleList.clear();
//			// 한노드에 복수대 VHL Arrive 가능함, 이때는 offset 으로 판단
//			for (int j = 0; j < drivingQueueNode.getArrivedVehicleCount(); j++) {
//				occupiedVehicle = drivingQueueNode.getArrivedVehicle(j);
//				if (occupiedVehicle == null) {
//					return "Error (occupiedVehicle is null.)";
//				}
//				// currNodeOffset 순으로 정렬 (오름차순)
//				sortArrivedVehicleList(occupiedVehicle, arrivedVehicleList);
//			}
//			
//			if (arrivedVehicleList.size() > 0) {
//				int indexOfMe = arrivedVehicleList.indexOf(vehicle);
//				if (indexOfMe < 0) {
//					prevVehicle = arrivedVehicleList.get(0);
//				} else {
//					if (indexOfMe == arrivedVehicleList.size() - 1) {
//						if (prevVehicle == null) {
//							StringBuffer log = new StringBuffer();
//							// log
//							trace(log.toString());							
//							return OcsConstant.OK;
//						}
//					} else {
//						prevVehicle = arrivedVehicleList.get(indexOfMe + 1);
//					}
//					
//					if (prevVehicle == null) {
//						return "Error (prevVehicle is null.)";
//					}
//					
//					Node nextMoveNode = getNextMoveNode(prevVehicle);
//					if (nextMoveNode == null) {
//						return "Waiting for Previous Vehicle";
//					}
//					if (vehicle.containsRoutedNode(nextMoveNode)
//							&& OcsConstant.LINE.equals(nextMoveNode.getLinkType(mainBlockNode))) {
//						StringBuffer log = new StringBuffer();
//						// log
//						trace(log.toString());
//						return OcsConstant.OK;
//					}
//					// request Yield : 불필요해보이는데 ?
//					if (prevVehicle.isYieldRequested() == false) {
//						if (prevVehicle.hasArrivedAtTarget()) {
//							prevVehicle.requestYield(vehicle);
//							return "Waiting for previous vehicle.";
//						}
//					}
//				} 		
//			}
//		}
//		return "Waiting for previous vehicle.";
//	}

	/**
	 * 2013.05.10 by KYK
	 * @param vehicle
	 * @param drivingQueue
	 * @return
	 */
	private String checkVehicleInDiverge(VehicleData vehicle, DrivingQueueList drivingQueue) {
		// Pass if First Vehicle
		// Pass if Same Direction as first vehicle
		// Or Request Yield
		if (drivingQueue == null || vehicle == null) {
			return "Error (vehicle or drivingQueue is null.)";
		}
		VehicleData occupiedVehicle = null;
		VehicleData prevVehicle = null;
		ArrayList<VehicleData> arrivedVehicleList = new ArrayList<VehicleData>();
		// Check UserBlock
		Node dQNode = drivingQueue.getLastDQNode();
		Vector<Node> dQList = new Vector<Node>(drivingQueue.getNodeList());
		if (dQList.size() > 1) {
			dQList.remove(dQNode);
			for (Node node : dQList) {
				occupiedVehicle = node.getArrivedVehicle();
				if (occupiedVehicle != null) {
					if (occupiedVehicle.equals(vehicle)) {
						traceBlockOK(vehicle, drivingQueue, dQNode);
						return OcsConstant.OK;
					} else {
						prevVehicle = occupiedVehicle;
					}
					break;
				}
			}
		}
		// Check from DQNode ~ currNode
		List<Node> nodeList = new ArrayList<Node>(vehicle.getDriveNodeList());
		if (nodeList.contains(dQNode) == false) {
			nodeList.addAll(new ArrayList<Node>(vehicle.getRoutedNodeList()));
		}
		int endIndex = nodeList.indexOf(dQNode);
		int currIndex = nodeList.indexOf(vehicle.getDriveCurrNode());
		for (int i = endIndex; i >= currIndex; i--) {
			Node node = nodeList.get(i);
			if (node == null) {
				return "Error (drivingQueueNode is null.)";
			}			
			arrivedVehicleList.clear();
			// Consider several vehicles Arrived at one node. 
			for (int j = 0; j < node.getArrivedVehicleCount(); j++) {
				occupiedVehicle = node.getArrivedVehicle(j);
				if (occupiedVehicle != null) {
					// Order by currNodeOffset (ascending)
					sortArrivedVehicleList(occupiedVehicle, arrivedVehicleList);
				}
			}
			if (arrivedVehicleList.size() > 0) {
				int indexOfMe = arrivedVehicleList.indexOf(vehicle);
				if (indexOfMe < 0) {
					prevVehicle = arrivedVehicleList.get(0);
				} else {
					if (indexOfMe == arrivedVehicleList.size() - 1) {
						if (prevVehicle == null) {
							traceBlockOK(vehicle, drivingQueue, dQNode);
							return OcsConstant.OK;
						}
					} else {
						prevVehicle = arrivedVehicleList.get(indexOfMe + 1);
					}
					// Pass if Same Direction as first vehicle
					Node nextMoveNode = getNextMoveNode(prevVehicle);
					if (nextMoveNode == null) {
						// 2014.11.06 by zzang9un : prevVehicle을 Caused Vehicle로 추가
						vehicle.addVehicleToDriveFailCausedVehicleList(prevVehicle, true);
						
						// 2014.11.06 by zzang9un : Caused Vehicle 표시
						return "Waiting for Previous Vehicle" + "(CausedVehicle:" + prevVehicle.toString() + ")";
					}
					if (vehicle.containsRoutedNode(nextMoveNode)) {
						// 2013.10.22 by KYK : Pass following vehicle even if Curve.
//							&& OcsConstant.LINE.equals(nextMoveNode.getLinkType(mainBlockNode))) {
						traceBlockOK2(vehicle, drivingQueue, dQNode, nextMoveNode, prevVehicle);
						return OcsConstant.OK;
					}
					// request Yield
					if (prevVehicle != null) {
						if (prevVehicle.isYieldRequested() == false) {
							if (prevVehicle.hasArrivedAtTarget()) {
								prevVehicle.requestYield(vehicle);
								
								// 2014.11.06 by zzang9un : prevVehicle을 Caused Vehicle로 추가
								vehicle.addVehicleToDriveFailCausedVehicleList(prevVehicle, true);
								
								// 2014.11.06 by zzang9un : Caused Vehicle 표시
								return "Waiting for previous vehicle." + "(CausedVehicle:" + prevVehicle.toString() + ")";
							}
						}
					}
				} 		
			}
		}
		
		// 2015.01.08 by zzang9un : prevVehicle을 Caused Vehicle에 추가
		vehicle.addVehicleToDriveFailCausedVehicleList(prevVehicle, true);
		
		return "Waiting for previous vehicle." + "(CausedVehicle:" + prevVehicle.toString() + ")";
	}

	/**
	 * 2013.04.05 by KYK
	 * @param prevVehicle
	 * @return
	 */
	private Node getNextMoveNode(VehicleData vehicle) {
		if (vehicle != null) {
			int index = 0;
			// 2013.11.20 by KYK : driveNodeList 변경 시 고려
			ArrayList<Node> driveNodeList;
			for (Node node : nodeList) {
				driveNodeList = new ArrayList<Node>(vehicle.getDriveNodeList());
				index = driveNodeList.indexOf(node);
				if (index >= 0) {
					if (index + 1 < driveNodeList.size()) {
						node = driveNodeList.get(index + 1);
						return node;
					}
					if (mainBlockNode.equals(node) == false) {
						return node;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 2013.04.05 by KYK
	 * @param vehicle
	 * @param arrivedVehicleList
	 */
	private void sortArrivedVehicleList(VehicleData vehicle, ArrayList<VehicleData> arrivedVehicleList) {
		int index = 0;
		VehicleData tempVehicle = null;
		for (int k = 0; k < arrivedVehicleList.size(); k++) {
			tempVehicle = arrivedVehicleList.get(k);
			if (vehicle.getCurrNodeOffset() < tempVehicle.getCurrNodeOffset()) {
				index = k;
				break;
			}
		}
		arrivedVehicleList.add(index, vehicle);
	}

//	/**
//	 * 2013.03.29 by KYK
//	 * @param vehicle
//	 * @return
//	 */
//	private boolean checkVehicleInDrivingQueueRange(VehicleData vehicle, Node dQNode, double drivingQueueRange) {
//
//		// New DrivingQueue
//		double timeToArrive = vehicle.getMoveTimeToStopNode(dQNode);
//
//		if (timeToArrive < drivingQueueRange) {
//			setTimeToArriveAtDQNode(vehicle, timeToArrive);
//			setEnteringTimeInDQRange(vehicle);
//			return true;
//		}
//		return false;
//	}
	
	/**
	 * 2013.05.10 by KYK
	 * @param vehicle
	 * @param drivingQueue
	 * @param drivingQueueRange
	 * @return
	 */
	private boolean checkVehicleInDQLimit(VehicleData vehicle, Node dQNode, double dQLimitTime) {
		
		// calculate moveTime to DQNode. (First or Last ??)
		double timeToArrive = vehicle.getMoveTimeToStopNode(dQNode);
		// set EnteredTime & remainingTime Only if vehicle in DQLimit.
		if (timeToArrive < dQLimitTime) {
			setTimeToArriveAtDQNode(vehicle, timeToArrive);
			setEnteringTimeInDQRange(vehicle, dQNode);
			// 2014.02.20 by KYK
			vehicle.setEnteringBlockList(this);
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param drivingVehicle
	 * @param firstVehicle
	 * @param drivingQueueList
	 * @return	"OK" if normal, reason(String) otherwise
	 */
	private String existDrivingVehicle(VehicleData drivingVehicle, VehicleData firstVehicle, DrivingQueueList drivingQueueList) {
		// CASE 1. DrivingVehicle이 존재 有

		// 1) Block을 점유한 Vehicle이 자신인 경우는 통과
		if (drivingVehicle.equals(firstVehicle)) {
			return OcsConstant.OK;
		}

		// 2) 동일 Path의 Block 통과 순서 뒤바뀜 확인
		// drivingVehicle(Block 점유 Vehicle)이 동일 Path(Section)에 존재하는 경우 First Vehicle은 통과를 시킨다.
		// (후행 Vehicle이 앞에 있다고 우선 업데이트 되어 통과 주행 명령이 전송된 상태에서 실제 선행 First Vehicle이 나중에 업데이트 되어 순서가 뒤바뀐 경우 Block 통과 주행 명령을 전송)
		Node currNode = drivingVehicle.getDriveCurrNode();
		if (currNode != null) {
			DrivingQueueList tmpDrivingQueueList = getDrivingQueueList(currNode);
			if (tmpDrivingQueueList != null) {
				if (drivingQueueList.equals(tmpDrivingQueueList)) {
					StringBuffer log = new StringBuffer();
					log.append(blockId).append("> ").append(drivingVehicle.getVehicleId()).append("[").append(tmpDrivingQueueList.getId()).append("_").append(tmpDrivingQueueList.getType());
					log.append("] PASS : ").append(firstVehicle.getVehicleId()).append("(").append(firstVehicle.getDriveCurrNode().getNodeId()).append(") - No DrivingVehicle, But First Vehicle in DrivingVehicle'Path.");
					trace(log.toString());
					
//					return OcsConstant.ABNORMAL_OK;
					return OcsConstant.OK;
				}
			}
		}

		// 3) Block을 점유한 Vehicle이 Commfail인지 확인
		if (drivingVehicle.getErrorCode() == OcsConstant.COMMUNICATION_FAIL) {
			StringBuffer log = new StringBuffer();
			log.append("Commfail Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block.(Occupied Vehicle)");
			return log.toString();
		}

		// 4) Block을 점유한 Vehicle이 무언정지인지 확인
		if (drivingVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER ||
				drivingVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER ||
				drivingVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
			StringBuffer log = new StringBuffer();
			log.append("NotRespond Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block.(Occupied Vehicle)");
			return log.toString();
		}

		// 5) Block을 점유한 Vehicle이 Manual인지 확인
		if (drivingVehicle.getVehicleMode() == 'M') {
			if (checkVehicleInBlock(drivingVehicle) == false) {
				StringBuffer log = new StringBuffer();
				log.append(blockId).append("> ").append(drivingVehicle.getVehicleId()).append("[").append(getDrivingQueueListId(drivingVehicle)).append("_").append(drivingQueueList.getType()).append("]");
				log.append(" Block Reset by DrivingVehicle is not in Block, Requested: ").append(firstVehicle.getVehicleId());
				resetDrivingVehicle(drivingVehicle);
				trace(log.toString());
				return OcsConstant.OK;
			} else {
				StringBuffer log = new StringBuffer();
				log.append("Manual Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block.(Occupied Vehicle)");
				return log.toString();
			}
		}

		// 6) Block Node 위치에 있는 Vehicle에게 양보 요청
		if (nodeList.contains(drivingVehicle.getDriveCurrNode())
				&& nodeList.contains(drivingVehicle.getDriveStopNode())) {
			if (drivingVehicle.isYieldRequested() == false) {
				// 2012.02.06 by PMM
//				drivingVehicle.setYieldRequest(true);
//				drivingVehicle.setYieldRequestedVehicle(firstVehicle);
				drivingVehicle.requestYield(firstVehicle);
			}
			
			//TODO : 2014.09.15 : 확인 필요
			StringBuffer log = new StringBuffer();			
			// 2014.09.12 by zzang9un : Caused Vehicle 표시
			log.append("IDLE Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block. Request yield.(Occupied Vehicle:" + firstVehicle.toString() + ")");
			
			// 2014.08.22 by zzang9un : 양보 요청한 vehicle을 causedVehicle로 등록
			drivingVehicle.addVehicleToDriveFailCausedVehicleList(firstVehicle, true);
			
			return log.toString();
		}

		// 7) Block ResetTimeout 체크 후 return
		StringBuffer log = new StringBuffer();
		String result = checkBlockResetForConverge(drivingVehicle);
		if (OcsConstant.OK.equals(result)) {
			log.append(blockId).append("> ").append(drivingVehicle.getVehicleId()).append("[").append(getDrivingQueueListId(drivingVehicle)).append("_").append(drivingQueueList.getType()).append("]");
			log.append(" Block Reset by Timeout(").append(ocsInfoManager.getBlockResetTimeout()).append("sec), Requested: ").append(firstVehicle.getVehicleId());
			resetDrivingVehicle(drivingVehicle);
			trace(log.toString());
		}
		
		log.delete(0, log.length());
		// 2014.09.12 by zzang9un : Caused Vehicle 표시
		log.append("Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block. Check BlockReset(").append(result).append(")" + "(CausedVehicle:" + drivingVehicle.toString() + ")");
		
		// 2014.09.04 by zzang9un : firstvehicle의 causedVehicle로 drivingVehicle을 등록
		firstVehicle.addVehicleToDriveFailCausedVehicleList(drivingVehicle, true);
		
		return log.toString();
	}
	
	/**
	 * 2013.04.05 by KYK
	 * @param firstVehicle
	 * @param drivingQueueList
	 * @return
	 */
	private String existDrivingVehicle7(VehicleData drivingVehicle, VehicleData firstVehicle, DrivingQueueList drivingQueue) {
		
		if (drivingVehicle == null || firstVehicle == null || drivingQueue == null) {
			return "Invalid value (null)";
		}
		
		// 1) DrivingVehicle in Block is same as FirstVehicle.
		if (drivingVehicle.equals(firstVehicle)) {
			return OcsConstant.OK;
		}
		Node dQNode = drivingQueue.getFirstDQNode();
		// 2) 동일 Path Block 통과 순서 뒤바뀜 확인
		// 후행호기 앞에 있는 것으로 우선 업데이트 되어 주행명령 전송, 실제 선행호기 나중에 업데이트 되어 순서 뒤바뀜  
//		if (drivingVehicle.containsDriveNode(dQNode)) {
		if (drivingVehicle.containsDriveNode(drivingQueue.getLastDQNode())) {
			return OcsConstant.OK;
		}
		
		// 3) Check Abnormal Case (Commfail, 무언정지, Manual)
		String abnormalResult = checkAbnoramlInBlock(drivingVehicle, firstVehicle, dQNode);
		if (abnormalResult.length() > 0) {
			return abnormalResult;
		}		
		
		// 4) Request Yield to Vehicle in Block ??
		// 2013.12.13 by KYK : 다른 위치에 정지에 있는 경우, blockList 포함여부를 확인해야 함
//		if (drivingVehicle.hasArrivedAtTarget()) {
		if (isStayingOnBlock(drivingVehicle)) {
			if (drivingVehicle.isYieldRequested() == false) {
				drivingVehicle.requestYield(firstVehicle);
			}
			StringBuffer log = new StringBuffer();
			// 2014.11.07 by zzang9un : Caused Vehicle 표시
			log.append("IDLE Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Main Block. Request yield.(Occupied Vehicle:" + firstVehicle.toString() + ")");
			
			// 2014.11.07 by zzang9un : 양보 요청한 vehicle을 causedVehicle로 등록
			drivingVehicle.addVehicleToDriveFailCausedVehicleList(firstVehicle, true);
			
			return log.toString();			
		}
		// Block 이전노드 간섭구간(Station) 위치시 양보요청
		Node stopNode = drivingVehicle.getDriveStopNode();
		Node oppositeDQNode = getOppositeDQNode(dQNode);
		if (stopNode != null) {
			if (stopNode.equals(oppositeDQNode)) {
				String stopStationId = drivingVehicle.getStopStation();
				if (stopStationId != null && stopStationId.length() > 0) {
					// Request Yield
					if (drivingVehicle.hasArrivedAtTarget()) {
						if (drivingVehicle.isYieldRequested() == false) {
							drivingVehicle.requestYield(firstVehicle);
						}
					}
					StringBuffer log = new StringBuffer();
					log.append("IDLE Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block. Request yield.(Occupied Vehicle)");
					return log.toString();
				}
			}
		}
		
		// 5) Block ResetTimeout 체크 후 return
		StringBuffer log = new StringBuffer();
		String result = checkBlockResetForConverge(drivingVehicle);
		if (OcsConstant.OK.equals(result)) {
			log.append(blockId).append("> ").append(drivingVehicle.getVehicleId()).append("[").append(dQNode.getNodeId()).append("]");
			log.append(" Block Reset by Timeout(").append(ocsInfoManager.getBlockResetTimeout()).append("sec), Requested: ").append(firstVehicle.getVehicleId());
			resetDrivingVehicle(drivingVehicle);
			trace(log.toString());
		}
		log.delete(0, log.length());
		log.append("Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block. Check BlockReset(").append(result).append(")");
		
		// 2015.01.07 by zzang9un : occupied vehicle을 caused vehicle로 등록한다.
		log.append("(CausedVehicle:" + drivingVehicle.toString() + ")");
		firstVehicle.addVehicleToDriveFailCausedVehicleList(drivingVehicle, true);
		
		return log.toString();
	}

	/**
	 * 2013.12.13 by KYK
	 * @param drivingVehicle
	 * @return
	 */
	private boolean isStayingOnBlock(VehicleData drivingVehicle) {
		if (drivingVehicle != null) {
			if (nodeList.contains(drivingVehicle.getDriveCurrNode())
					&& nodeList.contains(drivingVehicle.getDriveStopNode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 2013.04.05 by KYK
	 * @param drivingVehicle
	 * @param firstVehicle
	 * @return
	 */
	private String checkAbnoramlInBlock(VehicleData drivingVehicle, VehicleData firstVehicle, Node dQNode) {
		
		if (drivingVehicle == null || firstVehicle == null || dQNode == null) {
			return "Invalid value (null)";
		}

		// 1) Check if Vehicle is Commfail in Block
		if (drivingVehicle.getErrorCode() == OcsConstant.COMMUNICATION_FAIL) {
			StringBuffer log = new StringBuffer();
			log.append("Commfail Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block.(Occupied Vehicle)");
			return log.toString();
		}
		
		// 2) Check if Vehicle is NotResponding in Block		
		if (drivingVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER ||
				drivingVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER ||
				drivingVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
			StringBuffer log = new StringBuffer();
			log.append("NotRespond Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block.(Occupied Vehicle)");
			return log.toString();
		}
		
		// 3) Check if Vehicle is Manual in Block		
		if (drivingVehicle.getVehicleMode() == 'M') {
			if (checkVehicleInBlock(drivingVehicle) == false) {
				StringBuffer log = new StringBuffer();
				log.append(blockId).append("> ").append(drivingVehicle.getVehicleId()).append("[").append(dQNode.getNodeId()).append("]");
				log.append(" Block Reset by DrivingVehicle is not in Block, Requested: ").append(firstVehicle.getVehicleId());
				resetDrivingVehicle(drivingVehicle);
				trace(log.toString());
				return OcsConstant.OK;
			} else {
				StringBuffer log = new StringBuffer();
				log.append("Manual Vehicle(").append(drivingVehicle.getVehicleId()).append(") is in Block.(Occupied Vehicle)");
				return log.toString();
			}
		}
		return "";
	}

	private String checkAbnormalInBlockNode(VehicleData firstVehicle) {
		// 1. 합류 Block Node에 Vehicle 존재시 Abnormal(Commfail, 무언정지, Manual, MultiType) 확인
		for (int i = 0; i < nodeList.size(); i++) {
			Node blockNode = nodeList.get(i);
			for (int j = 0; j < blockNode.getDriveVehicleCount(); j++) {
				VehicleData blockVehicle = blockNode.getDriveVehicle(j);
				
				// 2012.07.13 by MYM : [NotNullCheck] 추가
				if (blockVehicle == null) {
					continue;
				}

				if (blockNode.equals(blockVehicle.getDriveCurrNode())) {
					// Block에 있는 Vehicle이 자신이면 통과
					if (firstVehicle.equals(blockVehicle)) {
						return OcsConstant.OK;
					}

					// 합류 Block 노드에 Manual Vehicle이 존재하는 경우
					if (blockVehicle.getVehicleMode() == 'M') {
						StringBuffer retLog = new StringBuffer();
						retLog.append("Manual Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block");
						return retLog.toString();
					}
					
					// 합류 Block 노드에 Commfail Vehicle이 존재하는 경우
					if (blockVehicle.getErrorCode() == OcsConstant.COMMUNICATION_FAIL) {
						StringBuffer retLog = new StringBuffer();
						retLog.append("Commfail Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block");
						return retLog.toString();
					}

					// 합류 Block 노드에 무언정지 Vehicle이 존재하는 경우
					if (blockVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER ||
							blockVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER ||
							blockVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
						StringBuffer retLog = new StringBuffer();
						retLog.append("NotRespond Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block");
						return retLog.toString();
					}

					// 2011.12.30 by MYM : MainBlock Node에 IDLE Vehicle이 있으면 양보 요청 조건 추가
					// 합류이면서 분기인(MultiType) 경우 MainBlock 노드에 Vehicle이 존재하고 있으면 양보 요청을 하고 대기함.
					// block Node에 Vehicle 존재하고 있으면 양보 요청을 하고 대기함.
					if (mainBlockNode.equals(blockNode) == false
							|| (mainBlockNode.equals(blockNode) && isMultiType() == true)
							|| (mainBlockNode.equals(blockNode) && blockVehicle.hasArrivedAtTargetNode()) ) {
						if (blockVehicle.isYieldRequested() == false) {
							blockVehicle.requestYield(firstVehicle);
						}
						
						StringBuffer retLog = new StringBuffer();
						retLog.append("IDLE Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block. Request yield.");
						return retLog.toString();							
					}
				}
			}
		}		
		return "";
	}
	
	/**
	 * 2013.04.05 by KYK
	 * @param firstVehicle
	 * @param drivingQueueList
	 * @return
	 */
	private String notExistDrivingVehicle7(VehicleData firstVehicle, DrivingQueueList drivingQueue, FLOW_CONTROL_TYPE blockControlType) {
		
		if (firstVehicle == null || drivingQueue == null) {
			return "firstVehicle or dQNode (null.)";
		}		
		Node dQNode = drivingQueue.getFirstDQNode();
		
		// check if Vehicle exists in Block
		for (Node blockNode: nodeList) {
			for (int j = 0; j < blockNode.getDriveVehicleCount(); j++) {
				VehicleData blockVehicle = blockNode.getDriveVehicle(j);				
				if (blockVehicle != null) {
					// 2015.06.04 by MYM : blockNode에 다른 후방 호기가 있는 경우 동일 DQ이면 통과 조건 추가
					// 배경 : 분기 ~ 합류 사이에 UserBlock이 2개 있으면서 동시에 Port가 있는 영역에서는 작업 Vehicle이 2대가 진입할 수 있음
					//       분기 통과시 Vehicle Node 업데이트 시간 타이밍상 순서 뒤바뀜 된 경우 발생.
					// Pass if vehicle in Block is me.
					if (firstVehicle.equals(blockVehicle)
							|| drivingQueue.getDrivingQueueList(blockNode) != null) {
						return OcsConstant.OK;
					}
					// Check if vehicle is abnormal (Commfail, NotResponding, Manual)
					String abnormalResult = checkAbnoramlInBlock(blockVehicle, firstVehicle, dQNode);
					if (abnormalResult != null && abnormalResult.length() > 0) {
						return abnormalResult;
					}
					// Request Yield to Vehicle in Block : multiType 필요한가?
					if (mainBlockNode.equals(blockNode) == false
							|| blockVehicle.hasArrivedAtTarget() || isMultiType()) {
						if (blockVehicle.isYieldRequested() == false) {
							blockVehicle.requestYield(firstVehicle);
						}					
						StringBuffer retLog = new StringBuffer();
						retLog.append("IDLE Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block. Request yield.");
						return retLog.toString();							
					}
				}
			}
		}
		// 2013.09.04 by KYK : DQNode 에서 VHL ME 인 경우? MI ?
		Node oppositeDQNode = getOppositeDQNode(dQNode);
		if (oppositeDQNode != null) {
			for (int i = 0; i < oppositeDQNode.getDriveVehicleCount(); i++) {
				VehicleData oppositeVehicle = oppositeDQNode.getDriveVehicle(i);
				if (oppositeVehicle != null) {
					if (oppositeDQNode.equals(oppositeVehicle.getDriveCurrNode())) {
						if (oppositeVehicle.getVehicleMode() == 'M') {
							if (oppositeVehicle.getState() == 'E' || 
									(oppositeVehicle.getCurrStation() != null && oppositeVehicle.getCurrStation().length() > 0)) {
							StringBuffer log = new StringBuffer();
							log.append("Error Vehicle(").append(oppositeVehicle.getVehicleId()).append(") is in Block.");
							return log.toString();
							}
						}
					}
				}
			}
		}
		
		// Flow Control : 통과순서제어
		return checkVehicleEnteringBlock(firstVehicle, drivingQueue, blockControlType);
	}
	

	/**
	 * 2013.04.05 by KYK
	 * @param firstVehicle
	 * @param dQNode
	 * @return
	 */
	private String checkVehicleEnteringBlock(VehicleData firstVehicle, DrivingQueueList drivingQueue, FLOW_CONTROL_TYPE flowControlType) {

		if (firstVehicle == null || drivingQueue == null) {
			return "Error (invalid value)";
		}
		Node currNode = firstVehicle.getDriveCurrNode();
		if (currNode == null) {
			return "Error (currNode is null.)";
		}
		Node dQNode = drivingQueue.getFirstDQNode();
		StringBuffer log = new StringBuffer(blockId);
		log.append("> NONE  [").append(dQNode.getNodeId()).append("]");
		
		// Already Driven in Block (previous DQNode Station)
		if (currNode.equals(dQNode)) {
			if (firstVehicle.getCurrStation() != null && firstVehicle.getCurrStation().length() > 0) {
				log.append(" PASS : ").append(firstVehicle.getVehicleId()).append(" - Block's Already Occupied.");
				trace(log.toString());
				return OcsConstant.OK;
			}
		}

		// Compare firstVhl to other path's first (Default: DQ EnteredTime)
		double oppositeFirstVehicleCost = getOppositeFirstVehicleCost(dQNode, firstVehicle, flowControlType);
		if (oppositeFirstVehicleCost >= 9999) {
			// Pass if Other path's Vhl not exist or in AbnormalState (Commfail or Manual or NotResponding). 
			log.append(" PASS : ").append(firstVehicle.getVehicleId()).append(" - Other Path Vehicle is not in DrivingQueue");
			trace(log.toString());
			return OcsConstant.OK;
		} else if (oppositeFirstVehicleCost < 0) {
			// Hold if Other path' Vhl is passing or occupied
			log.append(" ").append(firstVehicle.getVehicleId()).append("(").append(dQNode.getNodeId());
			log.append(" Already Driven To BlockNode:").append(mainBlockNode.getNodeId()).append(") - Waiting for other Path Vehicle.");
			trace(log.toString());
			return "Waiting for other Path Vehicle.";
		} else {
			// Decide by rule
			double firstVehicleCost = getFirstVehicleCost(firstVehicle, dQNode, flowControlType);
			if (firstVehicleCost <= oppositeFirstVehicleCost) {
				log.append(" PASS : ").append(firstVehicle.getVehicleId()).append("(").append(dQNode.getNodeId());
				log.append(",").append(firstVehicleCost).append(",").append(oppositeFirstVehicleCost).append(") - ArrivedTime is more fast than the other Path's ArrivedTime");
				trace(log.toString());
				return OcsConstant.OK;
			}
			log.append(" ").append(firstVehicle.getVehicleId()).append("(").append(dQNode.getNodeId());
			log.append(",").append(firstVehicleCost).append(",").append(oppositeFirstVehicleCost).append(") - Waiting for other Path Vehicle.");
			trace(log.toString());
			return "Waiting for other Path Vehicle.";
		}
	}

	/**
	 * 2013.04.12 by KYK
	 * @param firstVehicle
	 * @param dQNode
	 * @param flowControlType
	 * @return
	 */
	private double getFirstVehicleCost(VehicleData firstVehicle, Node dQNode, FLOW_CONTROL_TYPE flowControlType) {
		Node firstVehicleNode = firstVehicle.getDriveCurrNode();
		if (firstVehicleNode == null) {
			return -1;
		}
		double firstVehicleCost = 9999;
		double distanceCost = 0;
		double waitingTime = 0;
		double waitingCost = 0;
		
		switch (flowControlType) {
		case DQRANGE:
			// DQRANGE : EnteredTime in DQ Range
			waitingTime = (System.currentTimeMillis() - getEnteredTime(firstVehicle)) / 1000;
			waitingCost = 1 / (waitingTime + 1);
			firstVehicleCost = waitingCost;			
			break;
		case FIFO_DQ1:
			// DQNODE2 : Time to Arrive DQNode (Tie Break Rule : DQ Entered Time)
			// 양쪽 모두 도착인 경우 (TimeToArrive:0) 위해 waitingCost 추가
			distanceCost = getTimeToArriveAtDQNode(firstVehicle);
			waitingTime = (System.currentTimeMillis() - getEnteredTime(firstVehicle)) / 1000;
			waitingCost = 1 / (waitingTime + 100);
			firstVehicleCost = distanceCost + waitingCost;
			break;
		case FIFO_DQ2:
			// DQNODE1 : Time to Arrive DQNode (Tie Break Rule : Node Arrived Time)
			// 양쪽 모두 도착인 경우 (TimeToArrive:0) 위해 waitingCost 추가
			distanceCost = getTimeToArriveAtDQNode(firstVehicle);
			waitingTime = (System.currentTimeMillis() - firstVehicleNode.getVehicleArrivedTime(firstVehicle.getVehicleId())) / 1000;
			waitingCost = 1 / (waitingTime + 100);
			firstVehicleCost = distanceCost + waitingCost;
			break;
		default:
			// DQRANGE : EnteredTime in DQ Range
			waitingTime = (System.currentTimeMillis() - getEnteredTime(firstVehicle)) / 1000;
			waitingCost = 1 / (waitingTime + 1);
			firstVehicleCost = waitingCost;
		}
		return firstVehicleCost;
	}
	
	/**
	 * 2014.08.21 by zzang9un : Error인 경우 Caused Vehicle을 추가하도록 수정
	 * @param firstVehicle
	 * @param drivingQueueList
	 * @return	"OK" if normal, reason(String) otherwise
	 */
	private String notExistDrivingVehicle(VehicleData firstVehicle, DrivingQueueList drivingQueueList) {
		// CASE 2. DrivingVehicle이 존재 無
		StringBuffer log = new StringBuffer(blockId);
		log.append("> NONE  [").append(drivingQueueList.getId()).append("_").append(drivingQueueList.getType()).append("]");

		// 1. 합류 Block Node에 Vehicle 존재시 Abnormal(Commfail, 무언정지, Manual, MultiType) 확인
		for (int i = 0; i < nodeList.size(); i++) {
			Node blockNode = nodeList.get(i);
			for (int j = 0; j < blockNode.getDriveVehicleCount(); j++) {
				VehicleData blockVehicle = blockNode.getDriveVehicle(j);
				
				// 2012.07.13 by MYM : [NotNullCheck] 추가
				if (blockVehicle == null) {
					continue;
				}

				if (blockNode.equals(blockVehicle.getDriveCurrNode())) {
					// 2015.06.04 by MYM : blockNode에 다른 후방 호기가 있는 경우 동일 DQ이면 통과 조건 추가
					// 배경 : 분기 ~ 합류 사이에 UserBlock이 2개 있으면서 동시에 Port가 있는 영역에서는 작업 Vehicle이 2대가 진입할 수 있음
					//       분기 통과시 Vehicle Node 업데이트 시간 타이밍상 순서 뒤바뀜 된 경우 발생.
					// Block에 있는 Vehicle이 자신이면 통과
//					if (firstVehicle.equals(blockVehicle)) {
					if (firstVehicle.equals(blockVehicle)
							|| drivingQueueList.getDrivingQueueList(blockNode) != null) {
						return OcsConstant.OK;
					}

					// 합류 Block 노드에 Manual Vehicle이 존재하는 경우
					if (blockVehicle.getVehicleMode() == 'M') {
						StringBuffer retLog = new StringBuffer();
						retLog.append("Manual Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block");
						return retLog.toString();
					}
					
					// 합류 Block 노드에 Commfail Vehicle이 존재하는 경우
					if (blockVehicle.getErrorCode() == OcsConstant.COMMUNICATION_FAIL) {
						StringBuffer retLog = new StringBuffer();
						retLog.append("Commfail Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block");
						return retLog.toString();
					}

					// 합류 Block 노드에 무언정지 Vehicle이 존재하는 경우
					if (blockVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER ||
							blockVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER ||
							blockVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
						StringBuffer retLog = new StringBuffer();
						retLog.append("NotRespond Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block");
						return retLog.toString();
					}

					// 2011.12.30 by MYM : MainBlock Node에 IDLE Vehicle이 있으면 양보 요청 조건 추가
					// 합류이면서 분기인(MultiType) 경우 MainBlock 노드에 Vehicle이 존재하고 있으면 양보 요청을 하고 대기함.
					// block Node에 Vehicle 존재하고 있으면 양보 요청을 하고 대기함.
					if (mainBlockNode.equals(blockNode) == false
							|| (mainBlockNode.equals(blockNode) && isMultiType() == true)
							|| (mainBlockNode.equals(blockNode) && blockVehicle.hasArrivedAtTargetNode()) ) {
						if (blockVehicle.isYieldRequested() == false) {
							blockVehicle.requestYield(firstVehicle);
						}
						
						StringBuffer retLog = new StringBuffer();
						// 2014.09.12 by zzang9un : Caused Vehicle 표시
						retLog.append("IDLE Vehicle(").append(blockVehicle.getVehicleId()).append(") is in Block. Request yield." + "(CausedVehicle:" + blockVehicle.toString() + ")");
						
						// 2014.08.25 by zzang9un : 양보 요청한 vehicle을 causedVehicle로 등록
						firstVehicle.addVehicleToDriveFailCausedVehicleList(blockVehicle, true);
						
						return retLog.toString();							
					}
				}
			}
		}

		// 2. 상대편의 FirstVehicle보다 먼저 도착한 경우 통과
		// - 작업없이 정지해 있는 Vehicle 제외
		// - 도착시간 : (DrivingQueueList Index + 1) * ArrivedTime
		// 2015.05.31 by MYM : opposite FirstVehicle 정보(도착시간, Index, Reason)를 가져오도록 추가
		HashMap<String, Object> result = new HashMap<String, Object>();
		long oppositeFirstVehicleArrivalTime = getOppositeFirstVehicleArrivedTime(drivingQueueList, firstVehicle, result);
		if (oppositeFirstVehicleArrivalTime == 0) {
			// Vehicle이 존재 X, Commfail, Manual, 무언정지
			// 상대편 Vehicle이 미존재시 통과
//			log.append(" PASS : ").append(firstVehicle.getVehicleId()).append(" - Other Path Vehicle is not in DrivingQueue");
			log.append(" PASS : ").append(firstVehicle.getVehicleId());
			log.append(" - Other Path Vehicle ").append(result.get(REASON));
			trace(log.toString());
			return OcsConstant.OK;
		} else if (oppositeFirstVehicleArrivalTime < 0) {
			// Vehicle이 block을 통과중이거나 blockNode에 위치
			// Block을 통과중이 Vehicle이 존재시 통과 보류(Block을 점유한 Vehicle이 없는 상황에서 Block 통과중인 경우)
			// blockNode에 정지해 있는 Vehicle이 존재시 양보 요청 후 통과 보류
			log.append(" ").append(firstVehicle.getVehicleId()).append("(").append(drivingQueueList.getId()).append("_").append(drivingQueueList.getType());
//			log.append(" Already Drived To MainBlockNode:").append(mainBlockNode.getNodeId()).append(") - Waiting for other Path Vehicle.");
			log.append(" - Waiting for other Path Vehicle. ");
			log.append(result.get(REASON));
			trace(log.toString());
			return "Waiting for other Path Vehicle.";
		} else {
			// block을 통과 위해 대기 Vehicle이 존재
			// 도착시간이 상대편의 FirstVehicle보다 먼저인 경우 통과
			Node firstVehicleNode = firstVehicle.getDriveCurrNode();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (firstVehicleNode == null) {
				return "Error (firstVehicleNode is null.)";
			}
			
			// 2014.10.20 by MYM : 가상 및 UserBlock 노드인 경우 Index 스킵하도록 수정
//			int drivingQueueIndex = drivingQueueList.getIndex(firstVehicleNode) + 1;
			int drivingQueueIndex = drivingQueueList.getIndex(firstVehicleNode);
			long firstVehicleArrivalTime = firstVehicleNode.getVehicleArrivedTime(firstVehicle.getVehicleId());
			double fristVehicleTimeCost = firstVehicleArrivalTime * drivingQueueIndex;
			int oppositeFristVehicleDrivingQueueIndex = (Integer) result.get(DQINDEX);
			String oppositeFristVehicleId = (String) result.get(FIRSTVEHICLE);
			double oppositeFristVehicleTimeCost = oppositeFirstVehicleArrivalTime * oppositeFristVehicleDrivingQueueIndex;

			if (fristVehicleTimeCost <= oppositeFristVehicleTimeCost) {
				log.append(" PASS ").append(firstVehicle).append("(").append(drivingQueueList.getId()).append("_").append(drivingQueueList.getType());
				log.append(",").append(firstVehicle).append("_").append(firstVehicleArrivalTime).append("_").append(drivingQueueIndex);
				log.append(",").append(oppositeFristVehicleId).append("_").append(oppositeFirstVehicleArrivalTime).append("_").append(oppositeFristVehicleDrivingQueueIndex);
				log.append(") - ArrivedTime is more fast than the other Path's ArrivedTime");
				trace(log.toString());
				return OcsConstant.OK;
			}

			log.append(" WAIT ").append(firstVehicle).append("(").append(drivingQueueList.getId()).append("_").append(drivingQueueList.getType());
			log.append(",").append(firstVehicle).append("_").append(firstVehicleArrivalTime).append("_").append(drivingQueueIndex);
			log.append(",").append(oppositeFristVehicleId).append("_").append(oppositeFirstVehicleArrivalTime).append("_").append(oppositeFristVehicleDrivingQueueIndex);
			log.append(") - Waiting for other Path Vehicle.");
			
			// 2015.05.27 by MYM : 상대 Path FirstVehicle로 인해 대기하는 경우 도착시간을 감소시킴
			// 배경 : S1 Ph1 6F 근접전환 Simulation 테스트 중 발생한 현상으로
			//       상대 Path FirstVehicle이 HID Capacity Full 등으로 통과를 못하는 경우가 발생하여 Deadlock 발생
			//       → 상대적으로 나의 도착시간을 감소시켜서 통과 순서가 바뀔 수 있도록 함.
			if (firstVehicle.getCurrNode().equals(firstVehicle.getStopNode()) &&
					(System.currentTimeMillis() - firstVehicle.getStateChangedTime()) > 10000) {
				if ((System.currentTimeMillis() - firstVehicleArrivalTime) > 2000) {
					long changedArrivedTime = firstVehicleNode.getVehicleArrivedTime(firstVehicle.getVehicleId()) - ocsInfoManager.getBlockArrivalRearrangTime();
					firstVehicleNode.updateDriveVehicleInfo(firstVehicle, changedArrivedTime);
					log.append(" (ReArrange ArrivalTime_").append(firstVehicleNode).append(":").append(firstVehicleArrivalTime).append("→").append(changedArrivedTime).append(")");
				}
			}
			
			trace(log.toString());
			return "Waiting for other Path Vehicle.";
		}
	}
	
	/**
	 * 2015.05.27 by MYM : DriveFail로 인해 대기하는 경우 도착시간을 증가시킴
	 * 
	 * @param vehicle
	 */
	public void reArrangeVehicleArrivedTime(VehicleData vehicle) {
		if (vehicle.getCurrNode().equals(vehicle.getStopNode())
				&& (System.currentTimeMillis() - vehicle.getStateChangedTime()) > 10000) {
			Node currNode = vehicle.getDriveCurrNode();
			DrivingQueueList drivingQueueList = getDrivingQueueList(currNode);
			if (drivingQueueList != null) {
				long prevArrivedTime = currNode.getVehicleArrivedTime(vehicle.getVehicleId());
				if ((System.currentTimeMillis() - prevArrivedTime) > 2000) {
					long changedArrivedTime = System.currentTimeMillis();
					currNode.updateDriveVehicleInfo(vehicle, changedArrivedTime);
					StringBuffer log = new StringBuffer(blockId);				
					log.append("> NONE  [").append(drivingQueueList.getId()).append("_").append(drivingQueueList.getType()).append("] ").append(vehicle.getVehicleId());
					log.append(" ReArrange ArrivalTime_").append(currNode).append(":").append(prevArrivedTime).append(" → ").append(changedArrivedTime).append(")");
					trace(log.toString());
				}
			}
		}
	}
	
	/**
	 * 리턴값이 
	 * < 0 Vehicle이 block을 통과중이거나 blockNode에 위치
	 * > 0 block을 통과 위해 대기 Vehicle이 존재 
	 * = 0 Vehicle이 존재 X, Commfail, Manual, 무언정지
	 * 
	 * @param drivingQueueList
	 * @param vehicle
	 * @return 
	 */
//	private double getOppositeFirstVehicleArrivedTime(DrivingQueueList drivingQueueList, VehicleData vehicle) {
	private long getOppositeFirstVehicleArrivedTime(DrivingQueueList drivingQueueList, VehicleData vehicle, HashMap<String, Object> result) {
		// 다른쪽 DrivingQeueue에서 첫번째 Vehicle을 찾아 해당 위치의 Node를 반환한다.

		// 2015.05.31 by MYM : result map에 reason, firstVehicle, DQIndex 등록 및 Return하여 로그 기록 추가
		StringBuffer reason = new StringBuffer();
		String firstVehicleId = "";
		int firstVehicleDQIndex = 0;
		
		try {
			for (Enumeration<DrivingQueueList> e = drivingQueueListTable.elements(); e.hasMoreElements();) {
				DrivingQueueList oppositeDrivingQueueList = e.nextElement();
				if (drivingQueueList.equals(oppositeDrivingQueueList)) {
					continue;
				}

				for (int i = 0; i < oppositeDrivingQueueList.size(); i++) {
					Node node = oppositeDrivingQueueList.get(i);
					// 2012.03.02 by MYM : [NotNullCheck] 추가
					if (node == null || node.getArrivedVehicleCount() == 0) {
						continue;
					}

					VehicleData  oppositeFirstVehicle = node.getArrivedVehicle();
					if (oppositeFirstVehicle != null) {
						// 2012.06.01 by MYM : mainBlokcNode에 driveIn 했는지로 조건 변경
						// Vehicle의 driveNodeList에 mainBlockNode가 존재하는지 보는 것은 아직 Vehicle이 node로 drivein을 하고 
						// Vehicle의 driveNodeList에 저장되기 전에 타 Vehicle이 동일 블럭 통과체크를 할 수 있음.
						// Block을 지나고 있는지 확인한다. 
						//					if (oppositeFirstVehicle.containsDriveNode(mainBlockNode)) {
						//						return -1;
						//					}
						if (mainBlockNode.hasAlreadyDrived(oppositeFirstVehicle)) {
							reason.append(oppositeFirstVehicle).append(" already dirved main block node");
							return -1;
						}

						// 메뉴얼 Vehicle인지 확인한다.
						if (oppositeFirstVehicle.getVehicleMode() == 'M') {
							reason.append(oppositeFirstVehicle).append(" is Manual");
							return 0;
						}

						// 통신 Fail Vehicle인지 확인한다.
						if (oppositeFirstVehicle.getErrorCode() == OcsConstant.COMMUNICATION_FAIL) {
							reason.append(oppositeFirstVehicle).append(" is CommFail");
							return 0;
						}

						// 합류 Block 노드에 무언정지 Vehicle이 존재하는 경우
						if (oppositeFirstVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_GOCOMMAND_TIMEOVER ||
								oppositeFirstVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_UNLOADCOMMAND_TIMEOVER ||
								oppositeFirstVehicle.getAlarmCode() == OcsAlarmConstant.NOTRESPONDING_LOADCOMMAND_TIMEOVER) {
							reason.append(oppositeFirstVehicle).append(" not Respond");
							return 0;
						}

						// 작업이 없이 정지해 있는 Vehicle인지 확인한다.
						if (oppositeFirstVehicle.hasArrivedAtTargetNode()) {
							// BlockNode에 위치해 있으면 양보 요청을 한다.
							if (checkNodeInBlock(node) == true) {
								oppositeFirstVehicle.requestYield(vehicle);
								reason.append(oppositeFirstVehicle).append(" won't pass block, but is in block");
								return -1;
							}
							reason.append(oppositeFirstVehicle).append(" won't pass block");
							return 0;
						}

						// 해당 Block으로 지나갈 Vehicle인지 확인한다.
						boolean blockPassingVehicle = false;
						for (int j = 0; j < nodeList.size(); j++) {
							Node blockNode = nodeList.get(j);
							// Drived이면 routeNodeList에 해당 node가 없음.
							if (oppositeFirstVehicle.containsRoutedNode(blockNode)) {
								blockPassingVehicle = true;
								break;
							}
						}
						if (blockPassingVehicle == false) {
							reason.append(oppositeFirstVehicle).append(" won't pass block");
							return 0;
						}

						// 2013.11.11 by MYM : Hybrid 주행 제어(교차로 근접, Vehicle 간섭 및 충돌설정 고려 주행)
						if (oppositeFirstVehicle.getDriveFailCausedVehicleListSize() > 0
								&& oppositeFirstVehicle.getDriveFailCausedVehicleList().contains(vehicle)) {
							reason.append(oppositeFirstVehicle).append(" has caused vehicle");
							return 0;
						}

						// 2015.06.02 by MYM : 여기서는 순수 도착시간만 리턴하도록 하고 호출부에서 통과 Cost를 별도 계산하도록 변경 
						//                     (result map에 reason, firstVehicle, DQIndex 등록 및 Return하여 로그 기록 추가)
						// 2014.10.20 by MYM : 가상 및 UserBlock 노드인 경우 Index 스킵하도록 수정
						//					return node.getVehicleArrivedTime(oppositeFirstVehicle.getVehicleId()) * (i + 1);
						firstVehicleId = oppositeFirstVehicle.getVehicleId();
						firstVehicleDQIndex = oppositeDrivingQueueList.getIndex(node);
//						return node.getVehicleArrivedTime(oppositeFirstVehicle.getVehicleId()) * oppositeDrivingQueueList.getIndex(node);
						return node.getVehicleArrivedTime(oppositeFirstVehicle.getVehicleId());
					}
				}
			}
			reason.append("is not in DrivingQueue");
		} catch (Exception e) {
		} finally {
			result.put(REASON, reason.toString());
			result.put(FIRSTVEHICLE, firstVehicleId);
			result.put(DQINDEX, new Integer(firstVehicleDQIndex));
		}

		return 0;
	}

	/**
	 * 2013.02.08 by KYK
	 * @param drivingQueueList
	 * @param vehicle
	 * @return
	 */
	private double getOppositeFirstVehicleCost(Node dQNode, VehicleData vehicle, FLOW_CONTROL_TYPE flowControlType) {
		// 다른쪽 DrivingQeueue에서 첫번째 Vehicle을 찾아 해당 위치의 Node를 반환한다.
				
		// Find Opposite DrivingQueue Node
		Node oppositeDQNode = getOppositeDQNode(dQNode);
		if (oppositeDQNode == null) {
			return 9999;
		}		
		// Find First Vehicle in DrivingQueue (DriveNodeList)		
		VehicleData firstVehicle = getDQFirstVehicle(oppositeDQNode);
		if (firstVehicle == null) {
			return 9999;
		}
		// Already Driven in Block
		if (mainBlockNode.hasAlreadyDrived(firstVehicle)) {
			return -1;
		}

		// Already Driven in Block (previous DQNode Station)
		Node stopNode = firstVehicle.getDriveStopNode();
		if (stopNode != null) {
			if (stopNode.equals(oppositeDQNode)) {
				String stopStationId = firstVehicle.getStopStation();
				if (stopStationId != null && stopStationId.length() > 0) {
					// Request Yield
					if (firstVehicle.hasArrivedAtTarget()) {
						if (firstVehicle.isYieldRequested() == false) {
							firstVehicle.requestYield(vehicle);
						}						
					}
					return -1;
				}
			}
		}
		
		// Check Abnormal State (Manual or Commfail or NotResponding)
		if (firstVehicle.isAbnormalInBlock()) {
			return 9999;
		}

		if (firstVehicle.containsRoutedNode(mainBlockNode) == false) {
			return 9999;
		}
//		// 2015.07.02 by KYK : 상대편 호기 때문에 못가고, 상대방은 나때문에 못 갈 때 (Hid Full)
//		if (firstVehicle.getDriveFailCausedVehicleListSize() > 0
//				&& firstVehicle.getDriveFailCausedVehicleList().contains(vehicle)) {
//			return 9999;
//		}

		return getFirstVehicleCost(firstVehicle, dQNode, flowControlType);
	}
	
	/**
	 * 2013.04.05 by KYK
	 * @param dQNode
	 * @return
	 */
	private Node getOppositeDQNode(Node dQNode) {
		Node oppositeDQNode = null;
		for (DrivingQueueList dQList : drivingQueueListTable.values()) {
//			Node tempNode = dQList.getLastDQNode();
			Node tempNode = dQList.getFirstDQNode();
			if (dQNode.equals(tempNode) == false) {
				oppositeDQNode = tempNode;
			}
		}
		return oppositeDQNode;
	}
	
	/**
	 * 2013.05.10 by KYK
	 * @param dQNode
	 * @return
	 */
	private DrivingQueueList getOppositeDrivingQueue(Node dQNode) {
		if (dQNode == null) {
			return null;
		}
		DrivingQueueList drivingQueue = null;
		Vector<Node> nodeList = null;
		for (DrivingQueueList dQ : drivingQueueListTable.values()) {
			if (dQ != null) {
				nodeList = dQ.getNodeList();
				if (nodeList != null) {
					if (nodeList.contains(dQNode) == false) {
						drivingQueue = dQ;
					}
				}
//				Node tempNode = dQ.getLastDQNode();
//				if (dQNode.equals(tempNode) == false) {
//					drivingQueue = dQ;
//				}			
			}			
		}
		return drivingQueue;
	}
	
	private static final String REASON = "REASON"; 
	private static final String FIRSTVEHICLE = "VEHICLE"; 
	private static final String DQINDEX = "DQINDEX";
	
	public void writeUpdatedVehicle(Node node, boolean updateToDB) {
		if (updateToDB == true) {
			blockInfoManager.addBlockToUpdateList(blockId);
		}

		// 로그 기록
		DrivingQueueList drivingQueueList = getDrivingQueueList(node);
		if (drivingQueueList != null) {
			StringBuffer log = new StringBuffer(blockId).append("> ");
			if (drivingVehicleList.size() == 0) {
				log.append("NONE  [");
			} else {				
//				log.append(drivingVehicle.getVehicleId()).append("[");
				log.append(getDrivingVehicles()).append("[");
			}
			log.append(drivingQueueList.getId()).append("_").append(drivingQueueList.getType()).append("] ").append(node.getNodeId());
			for (int i = 0; i < drivingQueueList.size(); i++) {
				Node dqNode = drivingQueueList.get(i);
				// 2012.03.02 by MYM : [NotNullCheck] 추가
				if (dqNode != null) {
					log.append(" ").append(dqNode.getNodeId());
					log.append("(").append(dqNode.toVehicleString()).append(")");
				} else {
					log.append(" null");
				}
			}
			trace(log.toString());
		}
	}
	
	/**
	 * 2013.08.01 by KYK
	 * @param vehicle
	 * @param updateToDB
	 */
	public void addBlockToUpdatedVehicleList(VehicleData vehicle) {
		blockInfoManager.addBlockToUpdateList(blockId);

		if (vehicle != null) {
			StringBuffer log = new StringBuffer(vehicle.getVehicleId());
			log.append(" release occupation of block:").append(blockId);
			log.append(" / CurrNode:").append(vehicle.getCurrNode());
			if (drivingVehicleList.size() > 0) {
				log.append(",but BlockDrivingVHL:").append(getDrivingVehicles());
			}
			trace(log.toString());
		}
	}
	
	public String toString() {
		StringBuffer blockLog = new StringBuffer();
		// 2013.07.30 by KYK
//		if (OcsConstant.CONVERGE.equals(this.blockType)) {
		if (isConvergeOrMultiType()) {
			blockLog.append("C_");
		} else {
			blockLog.append("D_");
		}
		blockLog.append(blockId).append("> ");
		
		for (Enumeration<String> e = drivingQueueListTable.keys(); e.hasMoreElements();) {
			String drivingQueueId = e.nextElement();
			DrivingQueueList drivingQueueList = drivingQueueListTable.get(drivingQueueId);
			blockLog.append("[").append(drivingQueueId).append("_").append(drivingQueueList.getType()).append("]");

			for (int i = 0; i < drivingQueueList.size(); i++) {
				Node node = drivingQueueList.get(i);
				// 2012.03.02 by MYM : [NotNullCheck] 추가
				// node의 toString이 바로 호출되도록 함. node가 null이면 null로 기록됨.
				blockLog.append(" ").append(node);
				if(this.nodeList.contains(node)) {
					blockLog.append("(B)");					
				}
			}
			blockLog.append("\n");
		}
		return blockLog.toString();
	}
	
	public void trace(String message) {
		if (message.equals(lastLogMessage)
				&& Math.abs(System.currentTimeMillis() - lastLogMessageTime) < 5000) {
			return;
		}

		traceLog.debug(String.format("%s", message));
		lastLogMessage = message;
		lastLogMessageTime = System.currentTimeMillis();
	}
	
	/**
	 * 2013.05.16 by KYK
	 * @param message
	 */
	public void traceBlockOK(VehicleData vehicle, DrivingQueueList dQ, Node dQNode) {
		if (vehicle != null && dQ != null && dQNode != null) {
			StringBuffer log = new StringBuffer();
			log.append(blockId).append("> ").append(vehicle.getVehicleId()).append("[").append(dQ.getId()).append("_").append(dQ.getType());
			log.append("] PASS(").append(dQNode.getNodeId()).append(")").append(" - First Vehicle");
			trace(log.toString());			
		}
	}
	
	public void traceBlockOK2(VehicleData vehicle, DrivingQueueList dQ, Node dQNode, Node nextNode, VehicleData prevVehicle) {
		if (vehicle != null && dQ != null && dQNode != null && nextNode != null && prevVehicle != null) {
			StringBuffer log = new StringBuffer();
			// BK_D_145054> OHT258[Link1812_LINE] PASS(145053) : OHT258 - Going in the same direction with PrevVehicle(OHT238), NextMoveNode(145055)
			log.append(blockId).append("> ").append(vehicle.getVehicleId()).append("[").append(dQ.getId()).append("_").append(dQ.getType());
			log.append("] PASS(").append(dQNode.getNodeId()).append(") - Going in the same direction with Previous Vehicle(").append(prevVehicle.getVehicleId());
			log.append("), Next Move Node(").append(nextNode.getNodeId()).append(")");
			trace(log.toString());
		}
	}

//	/**
//	 * 2013.04.12 by KYK
//	 * @param drivingQueue
//	 */
//	public void addDrivingQueueList(DrivingQueue drivingQueue) {
//		if (drivingQueue != null) {
//			drivingQueueList.add(drivingQueue);
//		}
//	}
	
	/**
	 * vehicle이 위치한 반대편 drivingQueue의 첫번째 vehicle을 리턴하는 함수
	 * @author zzang9un
	 * @since	2014. 8. 28.
	 * @param drivingQueueList - vehicle이 위치한 drivingQueue
	 * @return 반대편 drivingQueue의 첫번째 vehicle을 리턴, 없는 경우 null
	 */
	public VehicleData getOppositeFirstVehicle(DrivingQueueList drivingQueueList) {
		for (Enumeration<DrivingQueueList> e = drivingQueueListTable.elements(); e.hasMoreElements();) {
			DrivingQueueList oppositeDrivingQueueList = e.nextElement();
			if (drivingQueueList.equals(oppositeDrivingQueueList)) {
				continue;
			}

			for (int i = 0; i < oppositeDrivingQueueList.size(); i++) {
				Node node = oppositeDrivingQueueList.get(i);
				// 2012.03.02 by MYM : [NotNullCheck] 추가
				if (node == null || node.getArrivedVehicleCount() == 0) {
					continue;
				}

				VehicleData oppositeFirstVehicle = node.getArrivedVehicle();
				if (oppositeFirstVehicle != null) {
					return oppositeFirstVehicle;
				}				
			}
		}

		return null;
	}
	
}