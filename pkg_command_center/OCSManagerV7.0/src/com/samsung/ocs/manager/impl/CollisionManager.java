package com.samsung.ocs.manager.impl;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsInfoConstant.SYSTEM_COLLISION_CRITERION;
import com.samsung.ocs.manager.impl.model.Collision;
import com.samsung.ocs.manager.impl.model.Link;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Section;

/**
 * CollisionManager Class, OCS 3.0 for Unified FAB
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

public class CollisionManager extends AbstractManager {
	private static CollisionManager manager = null;
	private static final String NODE1 = "NODE1";
	private static final String NODE2 = "NODE2";
	private static final String TYPE = "TYPE";
	private static final String SYSTEM = "SYSTEM";
//	private Vector<Collision> insertSystemCollisionList = new Vector<Collision>(); // 2011.11.11 by MYM : 미사용으로 주석 처리
	private boolean isRuntimeUpdateRequested = false;
	private boolean isRuntimeUpdatable = false;
	NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
	
	/**
	 * Constructor of CollisionManager class.
	 */
	private CollisionManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(Collision.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of CollisionManager class. (Singleton)
	 */
	public static synchronized CollisionManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new CollisionManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		// 2015.03.18 by MYM : System Collision 설정 기준 옵션 추가(좌표 or Distance)
		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		
		boolean result = false;
		if (nodeManager != null && ocsInfoManager != null && ocsInfoManager.isSystemCollisionUpdate()) {
			if (ocsInfoManager.getSystemCollisionCriterion() == SYSTEM_COLLISION_CRITERION.DISTANCE) {
				double collisionDistance = 1500;
				collisionDistance = ocsInfoManager.getVehicleLength();
				result = makeSystemCollisionDistanceBaseOnlyLine(collisionDistance);
			} else {
				int vehicleLength = ocsInfoManager.getVehicleLength();
				int vehicleWidth = ocsInfoManager.getVehicleWidth();
				result = makeSystemCollision(vehicleLength, vehicleWidth);
			}
		}		
		
		initialize(result);
		updateStopAllowed();
		isInitialized = true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		if (isRuntimeUpdateRequested == false) {
			isRuntimeUpdatable = false;
			if (isInitialized) {
				updateUserCollision();
			} else {
				// 어떤 경우?? 예방 차원
				init();
			}
		} 
		isRuntimeUpdatable = true;
		return true;
	}
	
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB() {
		isRuntimeUpdateRequested = true;
		isInitialized = false;
		int count = 0;
		while (true) {
			if (isRuntimeUpdatable || count > 200) {
				break;
			}
			count++;
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				writeExceptionLog(LOGFILENAME, e);
				break;
			}
		}
		data.clear();
		
		init();
		isRuntimeUpdateRequested = false;
	}
	
	// 2011.11.09 by PMM
//	private static final String initializeSql = "SELECT * FROM COLLISION";
	private static final String INITIALIZE_SQL = "SELECT * FROM COLLISION WHERE ROWID IN (SELECT MIN(ROWID) FROM COLLISION GROUP BY NODE1, NODE2) ORDER BY NODE1, NODE2";
	/**
	 * 
	 * @return
	 */
	private boolean initialize(boolean madeSystemCollision) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			// 2011.11.11 by MYM : System Collision 생성 유무에 따라 DB에서 Collision 정보를 가져오도록 변경  
			if (madeSystemCollision) {
				pstmt = conn.prepareStatement(SELECT_USERCOLLISION_SQL);
			} else {
				pstmt = conn.prepareStatement(INITIALIZE_SQL);
			}
			rs = pstmt.executeQuery();
			String nodeId1 = "";
			String nodeId2 = "";
			String key;
			Node node1 = null;
			Node node2 = null;
			Collision collision;
			NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
			while (rs.next()) {
				nodeId1 = rs.getString(NODE1);
				nodeId2 = rs.getString(NODE2);
				if (nodeId1.compareTo(nodeId2) <= 0) {
					key = nodeId1 + "_" + nodeId2;
				} else {
					key = nodeId2 + "_" + nodeId1;
				}
				collision = (Collision) data.get(key);
				if (collision == null) {
					collision = (Collision) vOType.newInstance();
					setCollision(collision, rs);
					node1 = nodeManager.getNode(collision.getNodeId1());
					node2 = nodeManager.getNode(collision.getNodeId2());
					if (node1 != null && node2 != null) {
						collision.addNode(node1);
						collision.addNode(node2);
						node1.addCollision(collision);
						node2.addCollision(collision);
					}
					data.put(key, collision);
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	// 2011.11.09 by PMM
//	private static final String selectUserSql = "SELECT * FROM COLLISION WHERE TYPE != 'SYSTEM'";
	private static final String SELECT_USERCOLLISION_SQL = "SELECT * FROM COLLISION WHERE ROWID IN (SELECT MIN(ROWID) FROM COLLISION WHERE TYPE IS NULL OR TYPE != 'SYSTEM' GROUP BY NODE1, NODE2) ORDER BY NODE1, NODE2";
	/**
	 * 
	 * @return
	 */
	private boolean updateUserCollision() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		
		// 2012.03.27 by PMM 
		boolean isNearByDrive = ocsInfoManager.isNearByDrive();
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_USERCOLLISION_SQL);
			rs = pstmt.executeQuery();
			String nodeId1 = "";
			String nodeId2 = "";
			String key;
			Node node1 = null;
			Node node2 = null;
			Collision collision;
			while (rs.next()) {
				if (isRuntimeUpdateRequested) {
					return false;
				}
				nodeId1 = rs.getString(NODE1);
				nodeId2 = rs.getString(NODE2);
				if (nodeId1.compareTo(nodeId2) <= 0) {
					key = nodeId1 + "_" + nodeId2;
				} else {
					key = nodeId2 + "_" + nodeId1;
				}
				collision = (Collision) data.get(key);
				if (collision == null) {
					collision = (Collision) vOType.newInstance();
					setCollision(collision, rs);
					node1 = nodeManager.getNode(collision.getNodeId1());
					node2 = nodeManager.getNode(collision.getNodeId2());
					if (node1 != null && node2 != null) {
						collision.addNode(node1);
						collision.addNode(node2);
						node1.addCollision(collision);
						node2.addCollision(collision);

						// 2012.03.12 by PMM
//						if (ocsInfoManager.isNearByDrive() == false) {
						// 2012.03.27 by PMM 
						if (isNearByDrive == false) {
							node1.checkStopAllowed();
							node2.checkStopAllowed();
						}
					}
					data.put(key, collision);
				}
				removeKeys.remove(key);
			}
			for (String rmKey : removeKeys) {
				collision = (Collision) data.get(rmKey);
				if (collision != null) {
					
					// 2012.03.27 by PMM 
//					if (ocsInfoManager.isNearByDrive() == false) {
					if (isNearByDrive == false) {
						node1 = nodeManager.getNode(collision.getNodeId1());
						node2 = nodeManager.getNode(collision.getNodeId2());
						if (node1 != null && node2 != null) {
							node1.checkStopAllowed();
							node2.checkStopAllowed();
						}
					}
					if (collision.isSystemCollision() == false) {
						collision.clear();
						data.remove(rmKey);
					}
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
		} catch (InstantiationException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * 
	 */
	// 2015.03.18 by MYM : System Collision 설정 기준 옵션 추가(좌표 or Distance)
//	private boolean makeSystemCollision() {
	private boolean makeSystemCollision(int vehicleLength, int vehicleWidth) {
//		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
//		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0); // 2011.11.11 by MYM : nodeManager null이라서 로컬 함수에서 가져옴.
//		if (nodeManager == null || ocsInfoManager.isSystemCollisionUpdate() == false) {
//			return false;
//		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("\n").append("Initialize...").append("\n");		
		long processTime = System.currentTimeMillis();
		
		// 2015.03.18 by MYM : System Collision 설정 기준 옵션 추가(좌표 or Distance)
		// System Collision 설정
//		int vehicleLength = 1500;
//		int vehicleWidth = 100;
//		if (ocsInfoManager != null) {
//			vehicleLength = ocsInfoManager.getVehicleLength();
//			vehicleWidth = ocsInfoManager.getVehicleWidth();
//		}
		double collisionDistance = Math.sqrt(vehicleLength * vehicleLength + vehicleWidth * vehicleWidth);
		Vector<Object> nodeList = new Vector<Object>(nodeManager.getData().values());
		Node node1;
		Node node2;
		StringBuffer log = new StringBuffer();
		for (int i = 0; i < nodeList.size(); i++) {
			node1 = (Node) nodeList.get(i);
			for (int j = i + 1; j < nodeList.size(); j++) {
				node2 = (Node) nodeList.get(j);
				// 2012.06.22 by MYM : Rail이 다르면 System Collision 만들지 않도록 수정
				if (node1.getRail().equals(node2.getRail())) {
					Collision collision = checkCollision(node1, node2, collisionDistance, vehicleWidth);
					if (collision != null) {
						if (addCollision(collision) == null) {
							log.append(collision.toString()).append("\r\n");
						}
					}
				}
			}
		}
		
		// 2011.11.11 by MYM : 로그 기록 방법 변경
		registerSystemCollisionToFile("CollisionData.txt", log.toString());

		sb.append("  Make System Collision : ").append((System.currentTimeMillis()-processTime)).append(" ms");		
		writeLog(LOGFILENAME, sb.toString());
		
		return true;
	}
	/**
	 * 2014.11.28 by KYK [DualOHT]
	 * 1. 정확도 향상 : Map Distance 활용 (기존, 좌표 값 기준)
	 * 2. 불합리 제거 : 직선 만 설정 (기존, 곡선도 설정 - 곡선은 데이터 부정확하고 사용자가 항상 다시 설정 원칙이었음) 
	 * 3. 연산 시간 개선 (기존, 대형라인 시 초기화 시간 지연 5~10초)
	 * @return
	 */
	// 2015.03.18 by MYM : System Collision 설정 기준 옵션 추가(좌표 or Distance)
//	private boolean makeSystemCollisionDistanceBaseOnlyLine() {
	private boolean makeSystemCollisionDistanceBaseOnlyLine(double collisionDistance) {
//		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
//		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0); // 2011.11.11 by MYM : nodeManager null이라서 로컬 함수에서 가져옴.
//		if (nodeManager == null || ocsInfoManager.isSystemCollisionUpdate() == false) {
//			return false;
//		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("\n").append("Initialize...").append("\n");
		long processTime = System.currentTimeMillis();
		
		// 2015.03.18 by MYM : System Collision 설정 기준 옵션 추가(좌표 or Distance)
//		double collisionDistance = 1500;
//		if (ocsInfoManager != null) {
//			collisionDistance = ocsInfoManager.getVehicleLength();
//		}
//		
		StringBuffer log = new StringBuffer();
		Vector<Object> nodeList = new Vector<Object>(nodeManager.getData().values());
		Node node;
		for (int i = 0; i < nodeList.size(); i++) {
			node = (Node) nodeList.get(i);
			if (node != null) {
				checkCollision(node, node, 0, collisionDistance, log);
			}
		}
		
		registerSystemCollisionToFile("CollisionData2.txt", log.toString());
		
		sb.append("  Make System Collision : ").append((System.currentTimeMillis()-processTime)).append(" ms");		
		writeLog(LOGFILENAME, sb.toString());
		
		return true;
	}
	
	/**
	 * 2014.11.28 by KYK [DualOHT] TODO
	 * @param node
	 * @param checkNode
	 * @param sumOfDistance
	 * @param collisionDistance
	 */
	private void checkCollision(Node node, Node checkNode, double sumOfDistance, double collisionDistance, StringBuffer log) {
		Section section;
		Link link;
		int index;
		boolean isCollision = false;
		Node node2 = null;
		Node prevNode = null;
		Collision collision;
		if (node != null && checkNode != null) {
			for (int j = 0; j < checkNode.getSectionCount(); j++) {
				section = checkNode.getSection(j);
				index = section.getNodeIndex(checkNode);
				prevNode = checkNode;				
				for (int k = index + 1; k < section.getNodeCount(); k++) {
					isCollision = false;
					node2 = section.getNode(k);
					if (node2 != null) {
						// 2015.03.10 by KYK : section 변경에 따른 collision 설정 변경
						link = section.getLink(prevNode, node2);
						if (link != null && OcsConstant.LINE.equals(link.getType())) {
//						if (OcsConstant.LINE.equals(prevNode.getLinkType(node2))) {
							sumOfDistance += prevNode.getMoveInDistance(node2);
							if (sumOfDistance < collisionDistance) {
								prevNode = node2;
								isCollision = true;
								
								collision = new Collision();
								collision.setSystemCollision(true);
								collision.setNodeId(node.getNodeId(), node2.getNodeId());
								collision.addNode(node);
								collision.addNode(node2);
								if (addCollision(collision) == null ) {
									node.addCollision(collision);
									node2.addCollision(collision);									
								}
								log.append(collision.toString()).append("\r\n");
							}
						}
						if (isCollision == false) {
							break;
						}
					}
				}
				if (isCollision) {
					checkCollision(node, node2, sumOfDistance, collisionDistance, log);
				}
			}
		}
	}

	/**
	 * 
	 * @param log
	 */
	private void registerSystemCollisionToFile(String fileName, String log) {
		if (log.length() <= 0) {
			return;
		}

		File file;
		FileWriter out = null;
		try {
			file = new File(fileName);
			out = new FileWriter(file, false);
			out.write(log);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			try {
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
				writeExceptionLog(LOGFILENAME, e);
			}
		}
	}

	/**
	 * 
	 * @param collision
	 * @param rs
	 * @exception SQLException
	 */
	private void setCollision(Collision collision, ResultSet rs ) throws SQLException {
		collision.setNodeId(rs.getString(NODE1), rs.getString(NODE2));
		collision.setSystemCollision((SYSTEM.equals(rs.getString(TYPE))));
	}

	/**
	 * 
	 * @param collision
	 */
	public Collision addCollision(Collision collision) {
		return (Collision)data.put(collision.getCollisionId(), collision);
	}
	
	// 2011.11.11 by MYM : 미사용 주석처리
	/**
	 * 
	 * @param collision
	 */
//	private void registerSystemCollision(Collision collision) {
//		if (insertSystemCollisionList.contains(collision) == false) {
//			insertSystemCollisionList.add(collision);
//		}
//	}
	
	private static final String REGISTER_SYSTEMCOLLISION_SQL = "INSERT INTO COLLISION (NODE1, NODE2, TYPE) VALUES (?,?,'SYSTEM')";
	
	// 2011.11.11 by MYM : DB 저장 방식 변경으로 주석처리
	/**
	 * 
	 * @return
	 */
//	private boolean registerSystemCollisionToDB() {
//		deleteSystemCollision();
//		
//		Connection conn = null;
//		PreparedStatement pstmt = null;
//		boolean result = false;
//		
//		try {
//			conn = dbAccessManager.getNewConnection();
//			Vector<Collision> insertSystemCollisionListTemp = new Vector<Collision>(insertSystemCollisionList);
//			ListIterator<Collision> iterator = insertSystemCollisionListTemp.listIterator();
//			Collision collision;
//			
//			conn.setAutoCommit(false);
//			pstmt = conn.prepareStatement(REGISTER_SYSTEMCOLLISION_SQL);
//			int count = 0;
//			while (iterator.hasNext()) {
//				collision = iterator.next();
//				if (collision != null) {
//					pstmt.setString(1, collision.getNodeId1());
//					pstmt.setString(2, collision.getNodeId2());
//					pstmt.addBatch();
//					count++;
//				}
//				if (count % 1000 == 0) {
//					pstmt.executeBatch();
//				}
//				insertSystemCollisionList.remove(collision);
//			}
//			pstmt.executeBatch();
//			conn.commit();
//			
//			result = true;
//		} catch (SQLException se) {
//			result = false;
//			se.printStackTrace();
//			writeExceptionLog(LOGFILENAME, se);
//		} catch (Exception e) {
//			result = false;
//			e.printStackTrace();
//			writeExceptionLog(LOGFILENAME, e);
//		}
//		finally {
//			if (pstmt != null) {
//				try {
//					pstmt.close();
//				} catch (Exception e) {}
//				pstmt = null;
//			}
//			try {
//				conn.setAutoCommit(true);
//			} catch (Exception e) {}
//			if (conn != null) {
//				try {
//					conn.close();
//				} catch (Exception e) {}
//				conn = null;
//			}
//		}
//		if (result == false) {
//			dbAccessManager.requestDBReconnect();
//		}
//		return result;
//	}
	
	/**
	 * 
	 * @return
	 */
	public boolean registerSystemCollisionToDB() {
		if (data.size() <= 0) {
			return false;
		}
		
		// 기존 System Collision 정보를 DB에서 삭제
		deleteSystemCollision();

		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getNewConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(REGISTER_SYSTEMCOLLISION_SQL);
			int count = 0;

			Collision collision = null;
			for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
				collision = (Collision) e.nextElement();
				if (collision != null && collision.isSystemCollision()) {
					pstmt.setString(1, collision.getNodeId1());
					pstmt.setString(2, collision.getNodeId2());
					pstmt.addBatch();
					count++;
				} else {
					continue;
				}

				if (count % 1000 == 0) {
					pstmt.executeBatch();
				}
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
			try {
				conn.setAutoCommit(true);
			} catch (Exception e) {}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {}
				conn = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	private static final String DELETE_SYSTEMCOLLISION_SQL = "DELETE FROM COLLISION PURGE WHERE TYPE = 'SYSTEM'";
	/**
	 * 
	 * @return
	 */
	private boolean deleteSystemCollision() {
		boolean result = false;
		Connection conn = null;
		Statement statement = null;
		try {
			conn = dbAccessManager.getConnection();
			statement = conn.createStatement();
			statement.execute(DELETE_SYSTEMCOLLISION_SQL);
			statement.close();
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) {}
				statement = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	/**
	 * 
	 * @param node1
	 * @param node2
	 * @param collisionDistance
	 * @param vehicleWidth
	 * @return
	 */
	public Collision checkCollision(Node node1, Node node2, double collisionDistance, double vehicleWidth) {
		Collision collision;
		double length = node2.getLength(node1);
		boolean isCollision = false;
		double x1, y1, x2, y2, x3, y3, x4, y4;
		double x5, y5, x6, y6, x7, y7, x8, y8;

		if (length <= collisionDistance) {
			collisionDistance *= 0.5;
			vehicleWidth *= 0.5;
			if (node2.getAngle() < 0) {
				if (node1.getAngle() < 0) {
					// 정렬상태 확인 불가로 최대 간섭범위 수용
				} else {
					double sine = Math.sin(Math.toRadians(node1.getAngle()));
					double cosine = Math.cos(Math.toRadians(node1.getAngle()));
					x1 = (int) (node1.getLeft() + collisionDistance * cosine - vehicleWidth * sine);
					y1 = (int) (node1.getTop() + collisionDistance * sine + vehicleWidth * cosine);
					x2 = (int) (node1.getLeft() - collisionDistance * cosine - vehicleWidth * sine);
					y2 = (int) (node1.getTop() - collisionDistance * sine +vehicleWidth * cosine);
					x3 = (int) (node1.getLeft() - collisionDistance * cosine + vehicleWidth * sine);
					y3 = (int) (node1.getTop() - collisionDistance * sine - vehicleWidth * cosine);
					x4 = (int) (node1.getLeft() + collisionDistance * cosine + vehicleWidth * sine);
					y4 = (int) (node1.getTop() + collisionDistance * sine - vehicleWidth * cosine);

					// 원과 직선을 비교
					if (checkArcLineCollision(node2.getLeft(), node2.getTop(), collisionDistance, x1, y1, x2, y2))
						isCollision = true;
					else if (checkArcLineCollision(node2.getLeft(), node2.getTop(), collisionDistance, x2, y2, x3, y3))
						isCollision = true;
					else if (checkArcLineCollision(node2.getLeft(), node2.getTop(), collisionDistance, x3, y3, x4, y4))
						isCollision = true;
					else if (checkArcLineCollision(node2.getLeft(), node2.getTop(), collisionDistance, x4, y4, x1, y1))
						isCollision = true;
				}
			} else {
				double sine = Math.sin(Math.toRadians(node2.getAngle()));
				double cosine = Math.cos(Math.toRadians(node2.getAngle()));
				x5 = (int) (node2.getLeft() + collisionDistance * cosine - vehicleWidth * sine);
				y5 = (int) (node2.getTop() + collisionDistance * sine + vehicleWidth * cosine);
				x6 = (int) (node2.getLeft() - collisionDistance * cosine - vehicleWidth * sine);
				y6 = (int) (node2.getTop() - collisionDistance * sine + vehicleWidth * cosine);
				x7 = (int) (node2.getLeft() - collisionDistance * cosine + vehicleWidth * sine);
				y7 = (int) (node2.getTop() - collisionDistance * sine - vehicleWidth * cosine);
				x8 = (int) (node2.getLeft() + collisionDistance * cosine + vehicleWidth * sine);
				y8 = (int) (node2.getTop() + collisionDistance * sine - vehicleWidth * cosine);
				
				if (node1.getAngle() < 0) {
					// 원과 직선을 비교
					if (checkArcLineCollision(node1.getLeft(), node1.getTop(), collisionDistance, x5, y5, x6, y6))
						isCollision = true;
					else if (checkArcLineCollision(node1.getLeft(), node1.getTop(), collisionDistance, x6, y6, x7, y7))
						isCollision = true;
					else if (checkArcLineCollision(node1.getLeft(), node1.getTop(), collisionDistance, x7, y7, x8, y8))
						isCollision = true;
					else if (checkArcLineCollision(node1.getLeft(), node1.getTop(), collisionDistance, x8, y8, x5, y5))
						isCollision = true;
				} else {
					// 두위치 다 정렬상태 확인으로 정확한 간섭범위 확인
					sine = Math.sin(Math.toRadians(node1.getAngle()));
					cosine = Math.cos(Math.toRadians(node1.getAngle()));
					x1 = (int) (node1.getLeft() + collisionDistance * cosine - vehicleWidth * sine);
					y1 = (int) (node1.getTop() + collisionDistance * sine + vehicleWidth * cosine);
					x2 = (int) (node1.getLeft() - collisionDistance * cosine - vehicleWidth * sine);
					y2 = (int) (node1.getTop() - collisionDistance * sine + vehicleWidth * cosine);
					x3 = (int) (node1.getLeft() - collisionDistance * cosine + vehicleWidth * sine);
					y3 = (int) (node1.getTop() - collisionDistance * sine - vehicleWidth * cosine);
					x4 = (int) (node1.getLeft() + collisionDistance * cosine + vehicleWidth * sine);
					y4 = (int) (node1.getTop() + collisionDistance * sine - vehicleWidth * cosine);

					// 직선과 직선을 비교
					if (checkLineLineCollision(x1, y1, x2, y2, x5, y5, x6, y6))
						isCollision = true;
					else if (checkLineLineCollision(x1, y1, x2, y2, x6, y6, x7, y7))
						isCollision = true;
					else if (checkLineLineCollision(x1, y1, x2, y2, x7, y7, x8, y8))
						isCollision = true;
					else if (checkLineLineCollision(x1, y1, x2, y2, x8, y8, x5, y5))
						isCollision = true;
					else if (checkLineLineCollision(x2, y2, x3, y3, x5, y5, x6, y6))
						isCollision = true;
					else if (checkLineLineCollision(x2, y2, x3, y3, x6, y6, x7, y7))
						isCollision = true;
					else if (checkLineLineCollision(x2, y2, x3, y3, x7, y7, x8, y8))
						isCollision = true;
					else if (checkLineLineCollision(x2, y2, x3, y3, x8, y8, x5, y5))
						isCollision = true;
					else if (checkLineLineCollision(x3, y3, x4, y4, x5, y5, x6, y6))
						isCollision = true;
					else if (checkLineLineCollision(x3, y3, x4, y4, x6, y6, x7, y7))
						isCollision = true;
					else if (checkLineLineCollision(x3, y3, x4, y4, x7, y7, x8, y8))
						isCollision = true;
					else if (checkLineLineCollision(x3, y3, x4, y4, x8, y8, x5, y5))
						isCollision = true;
					else if (checkLineLineCollision(x4, y4, x1, y1, x5, y5, x6, y6))
						isCollision = true;
					else if (checkLineLineCollision(x4, y4, x1, y1, x6, y6, x7, y7))
						isCollision = true;
					else if (checkLineLineCollision(x4, y4, x1, y1, x7, y7, x8, y8))
						isCollision = true;
					else if (checkLineLineCollision(x4, y4, x1, y1, x8, y8, x5, y5))
						isCollision = true;
				}
			}
			
			if (isCollision) {
				collision = new Collision();
				collision.setSystemCollision(true);
				collision.setNodeId(node1.getNodeId(), node2.getNodeId());
				collision.addNode(node1);
				collision.addNode(node2);
				node1.addCollision(collision);
				node2.addCollision(collision);
				return collision;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @param xc
	 * @param yc
	 * @param radius
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	private boolean checkArcLineCollision(double xc, double yc, double radius, double x1, double y1, double x2, double y2) {
		// Arc와 라인간의 충돌 여부를 확인
		double xc_x1 = xc - x1;
		double yc_y1 = yc - y1;
		double x2_x1 = x2 - x1;
		double y2_y1 = y2 - y1;
		double xc_x2 = xc - x2;
		double yc_y2 = yc - y2;
		double x1_x2 = x1 - x2;
		double y1_y2 = y1 - y2;

		double LenPcP1 = Math.sqrt(xc_x1*xc_x1 + yc_y1*yc_y1);
		double LenPcP2 = Math.sqrt(xc_x2*xc_x2 + yc_y2*yc_y2);
		double LenP1P2 = Math.sqrt(x1_x2*x1_x2 + y1_y2*y1_y2);

		if ((LenPcP1 < radius) || (LenPcP2 < radius))
			return true;

		double Theta1 = Math.acos((xc_x1*x2_x1 + yc_y1*y2_y1)/(LenPcP1*LenP1P2));
		double Theta2 = Math.acos((-xc_x2*x2_x1 - yc_y2*y2_y1)/(LenPcP2*LenP1P2));
		double Distance = LenPcP1*Math.sin(Theta1);

		if ((Theta1 < Math.PI/2.0) && (Theta2 < Math.PI/2.0) && (Distance < radius))
			return true;
		else
			return false;
	}
	
	/**
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param x3
	 * @param y3
	 * @param x4
	 * @param y4
	 * @return
	 */
	private boolean checkLineLineCollision(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
		double A1, B1, C1, A2, B2, C2;
		double X, Y;
		double DET;
		// 두 라인간의 층돌 여부를 확인
		A1 = y2 - y1;
		B1 = - (x2 - x1);
		C1 = - (y2 - y1) * x1 + y1 * (x2 - x1);
		A2 = y4 - y3;
		B2 = - (x4 - x3);
		C2 = - (y4 - y3) * x3 + y3 * (x4 - x3);
		DET = A1 * B2 - B1 * A2;
		
		if (DET == 0) {
			// 동일한 직선
			if (A1 * C2 - C1 * A2 == 0) {
				X = x3;
				Y = y3;
				// 입력 좌표가 Path의 시작점과 최종점 사이에 있는지를 확인
				if (((X - x1) * (x2 - X) >= 0.0) && ((Y - y1) * (y2 - Y) >= 0.0))
					return true;
				X = x4;
				Y = y4;
				// 입력 좌표가 Path의 시작점과 최종점 사이에 있는지를 확인
				if (((X - x1) * (x2 - X) >= 0.0) && ((Y - y1) * (y2 - Y) >= 0.0))
					return true;
				X = x1;
				Y = y1;
				// 입력 좌표가 Path의 시작점과 최종점 사이에 있는지를 확인
				if (((X - x3) * (x4 - X) >= 0.0) && ((Y - y3) * (y4 - Y) >= 0.0))
					return true;
				X = x2;
				Y = y2;
				// 입력 좌표가 Path의 시작점과 최종점 사이에 있는지를 확인
				if (((X - x3) * (x4 - X) >= 0.0) && ((Y - y3) * (y4 - Y) >= 0.0))
					return true;
			} else {
				// 다른 직선
				return false;
			}
		} else {
			// 한점에서 만나는 경우
			X = ( -B2 * C1 + B1 * C2) / DET;
			Y = (A2 * C1 - A1 * C2) / DET;

			// 입력 좌표가 Path의 시작점과 최종점 사이에 있는지를 확인
			if (((X - x1) * (x2 - X) >= 0.0) && ((Y - y1) * (y2 - Y) >= 0.0)) {
				if (((X - x3) * (x4 - X) >= 0.0) && ((Y - y3) * (y4 - Y) >= 0.0))
					return true;
			}
		}
		return false;
	}
	
	// 2012.02.06 by PMM
	public boolean checkCollsion(String nodeId1, String nodeId2) {
		if (nodeId1.compareTo(nodeId2) <= 0) {
			return data.containsKey(nodeId1 + "_" + nodeId2); 
		} else {
			return data.containsKey(nodeId2 + "_" + nodeId1);
		}
	}
	
	private void updateStopAllowed() {
		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		if (nodeManager == null) {
			return;
		}
		StringBuffer sb = new StringBuffer();
		long processTime = System.currentTimeMillis();
		Vector<Object> nodeList = new Vector<Object>(nodeManager.getData().values());
		Node node = null;
		for (Enumeration<Object> e = nodeList.elements(); e.hasMoreElements();) {
			node = (Node) e.nextElement();
			if (node != null) {
				if (ocsInfoManager.isNearByDrive()) {
					node.setStopAllowed(true);
				} else {
					node.checkStopAllowed();
				}
			}
		}
		sb.append("  Update StopAllowed: ").append((System.currentTimeMillis() - processTime)).append("ms");		
		writeLog(LOGFILENAME, sb.toString());
	}
}
