package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.manager.impl.model.Alarm;
import com.samsung.ocs.manager.impl.model.CloseLoop;
import com.samsung.ocs.manager.impl.model.Node;

/**
 * CloseLoopManager Class, OCS 3.0 for Unified FAB
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

public class CloseLoopManager extends AbstractManager {
	private static CloseLoopManager manager = null;
	private static final String NODELIST = "NODELIST";
	private static final String VEHICLELIMIT = "VEHICLELIMIT";
	
	private static final String CLOSELOOP = "CloseLoop";
	
	private boolean isRuntimeUpdateRequested = false;
	private boolean isRuntimeUpdatable = false;
	
	// 2015.04.20 by MYM : Validation 체크를 요청 방식으로 변경
	private boolean requestValidationCheck = false;	
	
	/**
	 * Constructor of CloseLoopManager class.
	 */
	private CloseLoopManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(CloseLoop.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
//			writeLog(LOGFILENAME, "Object Type Not Supported", true);
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of CloseLoopManager class. (Singleton)
	 */
	public static synchronized CloseLoopManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new CloseLoopManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		initialize();
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
				updateFromDBImpl();
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
	
	/**
	 * 
	 * @return
	 */
	private boolean initialize() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				StringBuffer closeLoopId = new StringBuffer();
				closeLoopId.append(rs.getInt(VEHICLELIMIT)).append("_").append(rs.getString(NODELIST));
				CloseLoop closeLoop = (CloseLoop) data.get(closeLoopId.toString());
				if (closeLoop == null) {
					closeLoop = (CloseLoop) vOType.newInstance();
					data.put(closeLoopId.toString(), closeLoop);
					setCloseLoop(closeLoop, rs);
				}
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			writeExceptionLog(LOGFILENAME, ie);
			ie.printStackTrace();
		} catch (InstantiationException e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		} catch (NullPointerException e) {			
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		} catch (Exception e) {			
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
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
	
	private static final String SELECT_SQL = "SELECT * FROM CLOSELOOP";
	
	/**
	 * 
	 * @return
	 */
	protected boolean updateFromDBImpl() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		Set<CloseLoop> newCloseLoopSet = new HashSet<CloseLoop>(); 

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				StringBuffer closeLoopId = new StringBuffer();
				closeLoopId.append(rs.getInt(VEHICLELIMIT)).append("_").append(rs.getString(NODELIST));
				CloseLoop closeLoop = (CloseLoop) data.get(closeLoopId.toString());
				if (closeLoop == null) {
					closeLoop = (CloseLoop) vOType.newInstance();
					data.put(closeLoopId.toString(), closeLoop);
					setCloseLoop(closeLoop, rs);
					newCloseLoopSet.add(closeLoop);
				} else {
					setChangedCloseLoopInfo(closeLoop, rs);
				}
				removeKeys.remove(closeLoopId.toString());
			}
			for (String rmKey : removeKeys) {
				CloseLoop closeLoop = (CloseLoop) data.remove(rmKey);
				closeLoop.clear();
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException ie) {
			result = false;
			writeExceptionLog(LOGFILENAME, ie);
			ie.printStackTrace();
		} catch (InstantiationException e) {
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		} catch (NullPointerException e) {			
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
		} catch (Exception e) {			
			result = false;
			writeExceptionLog(LOGFILENAME, e);
			e.printStackTrace();
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
		
		// 2015.04.20 by MYM : Runtime Update 중 CloseLoop Validation 체크가 늦어지는 경우가 발생
		//                     Runtime Update 완료시 체크하도록 변경
		if (requestValidationCheck) {
			// Runtime Update 완료시 Validation 체크 (OperationManager에서 요청)
			checkCloseLoopValidationAll();
			requestValidationCheck = false;
		} else if(isRuntimeUpdateRequested == false) {
			// Runtime Update 중에는 Validation 체크 X
			checkCloseLoopValidation(newCloseLoopSet);			
		}
		
		return result;
	}

	/**
	 * 
	 * @param closeLoop
	 * @param rs
	 * @exception SQLException
	 */
	private void setCloseLoop(CloseLoop closeLoop, ResultSet rs ) throws SQLException {
//		closeLoop.setCloseLoopId(rs.getString("CLOSELOOPID"));	//CLOSELOOPID 추가 필요.
		String nodeList = rs.getString(NODELIST);
		int vehicleLimit = rs.getInt(VEHICLELIMIT);
		
		StringBuffer closeLoopId = new StringBuffer();
		closeLoopId.append(vehicleLimit).append("_").append(nodeList);
		closeLoop.setCloseLoopId(closeLoopId.toString());
		closeLoop.setVehicleLimit(vehicleLimit);
		
		// 2015.04.20 by MYM : Runtime Update 중 CloseLoop Validation 체크가 늦어지는 경우가 발생
		//                     Runtime Update 완료시 체크하도록 변경
//		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
//		String[] arrayOfnodeList = nodeList.split("/");	
//		StringBuffer invalidNodes = new StringBuffer();
//		int invalidNodeCount = 0;
//		for (int i = 0; i < arrayOfnodeList.length; i++) {
//			Node node = nodeManager.getNode(arrayOfnodeList[i]);
//			if (node != null) {
//				closeLoop.addNode(node);
//			} else {
//				if (invalidNodes.length() > 0) {
//					invalidNodes.append(",");
//				}
//				invalidNodes.append(arrayOfnodeList[i]);
//				invalidNodeCount++;
//			}
//		}
//		
//		checkCloseLoopOutdated(closeLoop, invalidNodes.toString(), invalidNodeCount, arrayOfnodeList);
//
//		checkCloseLoopValidities(closeLoop, arrayOfnodeList);
	}
	
	/**
	 * 2015.04.20 by MYM : Runtime Update 중 CloseLoop Validation 체크가 늦어지는 경우가 발생 Runtime Update 완료시 체크하도록 변경
	 */
	public void requestValidationCheck() {
		this.requestValidationCheck = true;
	}
	
	/**
	 * 2015.04.20 by MYM : Runtime Update 중 CloseLoop Validation 체크가 늦어지는 경우가 발생 Runtime Update 완료시 체크하도록 변경
	 */
	private void checkCloseLoopValidationAll() {
		if (data.size() > 0) {
			long startTime = System.currentTimeMillis();
			this.writeLog(LOGFILENAME, "ALL CLOSELOOP VALIDATION CHECK START");
			for (Object obj : data.values()) {
				CloseLoop closeLoop = (CloseLoop) obj;
				checkCloseLoopValidation(closeLoop);
			}
			this.writeLog(LOGFILENAME, "ALL CLOSELOOP VALIDATION CHECK END : ElapsedTime " + (System.currentTimeMillis() - startTime) + "ms");
		}
	}
	
	/**
	 * 2015.04.20 by MYM : Runtime Update 중 CloseLoop Validation 체크가 늦어지는 경우가 발생 Runtime Update 완료시 체크하도록 변경
	 * 
	 * @param closeLoopList
	 */
	private void checkCloseLoopValidation(Set<CloseLoop> closeLoopList) {
		if (closeLoopList.size() > 0) {
			long startTime = System.currentTimeMillis();
			this.writeLog(LOGFILENAME, "CLOSELOOP VALIDATION CHECK START");
			for (CloseLoop closeLoop : closeLoopList) {
				checkCloseLoopValidation(closeLoop);
			}
			this.writeLog(LOGFILENAME, "CLOSELOOP VALIDATION CHECK END : ElapsedTime " + (System.currentTimeMillis() - startTime) + "ms");
		}
	}
	
	/**
	 * 2015.04.20 by MYM : Runtime Update 중 CloseLoop Validation 체크가 늦어지는 경우가 발생 Runtime Update 완료시 체크하도록 변경
	 * 
	 * @param closeLoop
	 */
	private void checkCloseLoopValidation(CloseLoop closeLoop) {
		if (closeLoop != null) {
			NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
			String nodeList = closeLoop.getCloseLoopId();
			String[] arrayOfnodeList = nodeList.substring(nodeList.indexOf('_')+1).split("/");	
			StringBuffer invalidNodes = new StringBuffer();
			int invalidNodeCount = 0;
			for (int i = 0; i < arrayOfnodeList.length; i++) {
				Node node = nodeManager.getNode(arrayOfnodeList[i]);
				if (node != null) {
					closeLoop.addNode(node);
				} else {
					if (invalidNodes.length() > 0) {
						invalidNodes.append(",");
					}
					invalidNodes.append(arrayOfnodeList[i]);
					invalidNodeCount++;
				}
			}
			checkCloseLoopOutdated(closeLoop, invalidNodes.toString(), invalidNodeCount, arrayOfnodeList);
			checkCloseLoopValidities(closeLoop, arrayOfnodeList);
		}
	}
	
	private void checkCloseLoopOutdated(CloseLoop closeLoop, String invalidNodes, int invalidNodeCount, String[] arrayOfnodeList) {
		try {
			if (invalidNodes.length() > 0) {
				StringBuffer log = new StringBuffer("\n");
				log.append("CloseLoopId  :").append(closeLoop.getCloseLoopId()).append("\n");
				log.append("Invalid Node :").append(invalidNodes);
				this.writeLog(LOGFILENAME, log.toString());
				
				StringBuilder message = new StringBuilder();
				message.append("CloseLoop Setting Outdated! ");
				message.append(invalidNodeCount);
				message.append(" Invalid Nodes in ");
				message.append(getCloseLoopInfo(arrayOfnodeList));
				
				registerAlarmText(message.toString());
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private void checkCloseLoopValidities(CloseLoop closeLoop, String[] arrayOfnodeList) {
		ArrayList<Node> connectedCloseLoopNodeList = new ArrayList<Node>();
		ArrayList<Node> isolatedNodeList = new ArrayList<Node>();
		ArrayList<Node> missedNodeList = new ArrayList<Node>();

		try {
			closeLoop.buildConnectedCloseLoopNodeList(connectedCloseLoopNodeList);
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		
		try {
			closeLoop.checkCloseLoopValidityIsolated(connectedCloseLoopNodeList, isolatedNodeList);
			if (isolatedNodeList.size() > 0) {
				StringBuilder message = new StringBuilder();
				message.append("Invalid CloseLoop Detected! Isolated Node");
				if (isolatedNodeList.size() > 1) {
					message.append("s:");
				} else {
					message.append(":");
				}
				message.append(getNodeList(isolatedNodeList));
				message.append(" in ");
				message.append(getCloseLoopInfo(arrayOfnodeList));
				
				registerAlarmText(message.toString());
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		
		try {
			if (missedNodeList != null) {
				closeLoop.checkCloseLoopValidity(isolatedNodeList, missedNodeList);
				if (missedNodeList.size() > 0) {
					StringBuilder message = new StringBuilder();
					message.append("Invalid CloseLoop Detected! Missed Node");
					if (missedNodeList.size() > 1) {
						message.append("s:");
					} else {
						message.append(":");
					}
					message.append(getNodeList(missedNodeList));
					message.append(" out of ");
					message.append(getCloseLoopInfo(arrayOfnodeList));
					
					registerAlarmText(message.toString());
				}
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		
		try {
			ArrayList<Node> missedCollisionsList = new ArrayList<Node>();
			closeLoop.checkCloseLoopValidityCollisions(connectedCloseLoopNodeList, isolatedNodeList, missedNodeList, missedCollisionsList);
			if (missedCollisionsList.size() > 0) {
				StringBuilder message = new StringBuilder();
				message.append("Invalid CloseLoop Detected! Missed Collision");
				if (missedCollisionsList.size() > 1) {
					message.append("s:");
				} else {
					message.append(":");
				}
				message.append(getNodeList(missedCollisionsList));
				message.append(" out of ");
				message.append(getCloseLoopInfo(arrayOfnodeList));
				
				registerAlarmText(message.toString());
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private String getNodeList(ArrayList<Node> missedNodeList) {
		try {
			if (missedNodeList != null) {
				StringBuilder message = new StringBuilder();
				Node node = null;
				for (int i = 0; i < missedNodeList.size(); i++) {
					if (i > 0) {
						message.append(",");
					}
					node = missedNodeList.get(i);
					if (node != null) {
						message.append(node.getNodeId());
					}
				}
				return message.toString();
			}			
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		return "";
	}
	
	private String getCloseLoopInfo(String[] arrayOfnodeList) {
		try {
			StringBuilder message = new StringBuilder();
			for (int i = 0; i < arrayOfnodeList.length; i++) {
				if (i > 0) {
					message.append("/");
				}
				if (i < 3) {
					message.append(arrayOfnodeList[i]);
				} else {
					message.append("...");
					break;
				}
			}
			return message.toString();
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
		return "";
	}
	
	private void registerAlarmText(String alarmText) {
		// 2015.10.14 by MYM : InService인 경우만 Alamr 발생
		if (this.serviceState != MODULE_STATE.INSERVICE) {
			return;
		}
		
		try {
			StringBuilder message = new StringBuilder();
			message.append("AlarmRegistered. ");
			message.append(CLOSELOOP);
			message.append(":");
			message.append(alarmText);
			this.writeLog(LOGFILENAME, message.toString());
			
			AlarmManager alarmManager = AlarmManager.getInstance(Alarm.class, dbAccessManager, true, true, 200);
			if (alarmManager != null) {
				if (alarmText.length() > 160) {
					alarmText = alarmText.substring(0, 160);
				}
				alarmManager.registerAlarmText(CLOSELOOP, alarmText);
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}

	/**
	 * 
	 * @param closeLoop
	 * @param rs
	 * @exception SQLException
	 */
	private void setChangedCloseLoopInfo(CloseLoop closeLoop, ResultSet rs) throws SQLException {
		closeLoop.setVehicleLimit(rs.getInt(VEHICLELIMIT));
	}
	
	public int getSize() {
		if (data != null) {
			return data.size();
		}
		return 0;
	}
}
