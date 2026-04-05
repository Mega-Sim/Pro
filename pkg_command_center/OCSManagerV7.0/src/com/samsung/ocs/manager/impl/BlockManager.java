package com.samsung.ocs.manager.impl;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.OcsInfoConstant.NEARBY_TYPE;
import com.samsung.ocs.common.constant.OcsInfoConstant.RUNTIME_UPDATE;
import com.samsung.ocs.manager.impl.model.Alarm;
import com.samsung.ocs.manager.impl.model.Block;
import com.samsung.ocs.manager.impl.model.DrivingQueueList;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Section;

/**
 * BlockInfoManager Class, OCS 3.0 for Unified FAB
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

public class BlockManager extends AbstractManager {
	private static BlockManager manager = null;
	Vector<String> updateDrivingQueue = new Vector<String>();
	
	private static final String CURVE = "CURVE";
	private static final String LINE = "LINE";
	private static final String CONVERGE = "CONVERGE";
	private static final String DIVERGE = "DIVERGE";
	private static final String MULTI = "MULTI"; // 2013.07.30 by KYK
	private static final String COLLISION = "C";
	private static final double BLOCKRANGE = 1300;
	private static final int REALNODEMINCOUNT = 2;
	
	private static final String BLOCKID = "BLOCKID";
	private static final String USERBLOCKNODES = "USERBLOCKNODES";
	private static final String MAPBLOCKNODES = "MAPBLOCKNODES";
	private static final String IGNOREBLOCKNODES = "IGNOREBLOCKNODES";
	private static final String DRIVINGQUEUEID = "DRIVINGQUEUEID";
	private static final String TYPE = "TYPE";
	private static final String NODES = "NODES";
	private static final String IGNORE = "IGNORE";
	private static final String ADD = "ADD";
	private static final String NONE = "NONE";
	private static final String OK = "OK";
	// 2013.04.19 by KYK
	private static final String DQLIMITTIME = "DQLIMITTIME";
	private ArrayList<DrivingQueueList> userDrivingQueueList = new ArrayList<DrivingQueueList>();

	// 2013.11.11 by MYM : UserBlock Update시 RuntimeUpdate 처럼 처리(Primary 진행 후 Secondary 진행)
	private static final String USERBLOCKUPDATE_START_INSERVICE = "UserBlock Update Started! - INSERVICE";
	private static final String USERBLOCKUPDATE_COMPLETED_INSERVICE = "UserBlock Update Completed! - INSERVICE";
	private static final String USERBLOCKUPDATE_START_OUTOFSERVICE = "UserBlock Update Started! - OUTOFSERVICE";
	private static final String USERBLOCKUPDATE_COMPLETED_OUTOFSERVICE = "UserBlock Update Completed! - OUTOFSERVICE";
	
	private ConcurrentHashMap<String, String> userBlockData;
	private ConcurrentHashMap<String, String> userDrivingQueueData;
	private Vector<Block> updateBlockInfo = new Vector<Block>();
	
	// 2014.10.15 by KYK
	private boolean isBlockUpdateToDB = false;	

	public void requestBlockUpdateToDB() {
		this.isBlockUpdateToDB = true;
	}

	// 2012.01.16 by MYM : BlockManager Logger 생성
	private static final String BLOCKMANAGER_TRACE = "BlockInfo";
	private static Logger blockManagerLogger = Logger.getLogger(BLOCKMANAGER_TRACE);
	
	/**
	 * Constructor of BlockInfoManager class.
	 */
	private BlockManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(Block.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}

	/**
	 * Constructor of BlockInfoManager class. (Singleton)
	 */
	public static synchronized BlockManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new BlockManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
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
		updateDrivingQueueToDB();
		
		// 2012.01.16 by MYM : UserBlock, DrivingQueue 변경에 따른 Block 정보 DB에 반영
		updateBlockInfoToDB();
		
		// 2014.10.15 by KYK
		if (isBlockUpdateToDB) {
			insertBlockInfoToDB();
			isBlockUpdateToDB = false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	@Override
	protected boolean updateFromDB() {
	  // 2012.01.16 by MYM : UserBlock, DrivingQueue를 Block에 반영
		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		
		if (ocsInfoManager != null) {
			RUNTIME_UPDATE userBlockUpdate = ocsInfoManager.getUserBlockUpdate();
			if (this.serviceState == MODULE_STATE.INSERVICE) {
				if (userBlockUpdate != RUNTIME_UPDATE.NO) {
					if (userBlockUpdate == RUNTIME_UPDATE.YES) {
						ocsInfoManager.setUserBlockUpdate(RUNTIME_UPDATE.DONE);
						registerAlarmTextWithLevel(USERBLOCKUPDATE_START_INSERVICE, ALARMLEVEL.INFORMATION, false);
						trace(USERBLOCKUPDATE_START_INSERVICE);

						if (NEARBY_TYPE.NEARBY_V3 == ocsInfoManager.getNearbyType()) {
							updateUserBlock();				
						} else if (NEARBY_TYPE.NEARBY_V7 == ocsInfoManager.getNearbyType()) {
							updateUserBlock7();
						}

						unregisterAlarmText(USERBLOCKUPDATE_START_INSERVICE);
						trace(USERBLOCKUPDATE_COMPLETED_INSERVICE);
					} else if (userBlockUpdate != RUNTIME_UPDATE.DONE) {
						ocsInfoManager.setUserBlockUpdate(RUNTIME_UPDATE.NO);
					}
				}
			} else if (userBlockUpdate == RUNTIME_UPDATE.DONE) {
				ocsInfoManager.setUserBlockUpdate(RUNTIME_UPDATE.NO);
				registerAlarmTextWithLevel(USERBLOCKUPDATE_START_OUTOFSERVICE, ALARMLEVEL.INFORMATION, false);
				trace(USERBLOCKUPDATE_START_OUTOFSERVICE);

				if (NEARBY_TYPE.NEARBY_V3 == ocsInfoManager.getNearbyType()) {
					updateUserBlock();				
				} else if (NEARBY_TYPE.NEARBY_V7 == ocsInfoManager.getNearbyType()) {
					updateUserBlock7();
				}

				unregisterAlarmText(USERBLOCKUPDATE_START_OUTOFSERVICE);
				trace(USERBLOCKUPDATE_COMPLETED_OUTOFSERVICE);
			}
		}
	
		return true;
	}
	
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB() {
		isInitialized = false;
		data.clear();
		// 2013.04.19 by KYK : V7 일 경우는 안만들어짐
		// 2012.02.03 by MYM
		if (userBlockData != null) {
			userBlockData.clear();			
		}
		if (userDrivingQueueData != null) {
			userDrivingQueueData.clear(); // 2012.02.17 by MYM : Runtime Update시 기존 userDrivingQueueData 삭제			
		}
		init();
	}
	
	private void updateUserBlock() {
		updateUserDrivingQueue();
		updateUserBlockNode();
	}
	
	private void updateUserBlock7() {
//		updateUserDrivingQueueRange();
		updateUserDrivingQueue7();
		updateUserBlockNode7();
	}
	
	/**
	 * 2013.11.13 by MYM : AlarmLevel 추가
	 * 
	 * @param alarmText
	 * @param alarmLevel
	 * @param isOnlyInserviceRegister
	 */
	private void registerAlarmTextWithLevel(String alarmText, ALARMLEVEL alarmLevel, boolean isOnlyInserviceRegister) {
		if (isOnlyInserviceRegister && serviceState == MODULE_STATE.OUTOFSERVICE) {
			return;
		}
		
		AlarmManager alarmManager = AlarmManager.getInstance(Alarm.class, null, true, true, 0);
		if (alarmManager != null) {
			alarmManager.registerAlarmTextWithLevel(BLOCKMANAGER_TRACE, alarmText, alarmLevel.toConstString());
		}
	}
	
	private void unregisterAlarmText(String alarmText) {
		AlarmManager alarmManager = AlarmManager.getInstance(Alarm.class, null, true, true, 0);
		if (alarmManager != null) {
			alarmManager.unregisterAlarmText(BLOCKMANAGER_TRACE, alarmText);
		}
	}
	
	private static final String SELECT_USERBLOCK_SQL = "SELECT * FROM USERBLOCK";
	private boolean updateUserBlockNode() {
		if (userBlockData == null) {
			userBlockData = new ConcurrentHashMap<String, String>();
		}

		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_USERBLOCK_SQL);
			rs = pstmt.executeQuery();
			
			Set<String> removeKeys = new HashSet<String>(userBlockData.keySet());
			
			while (rs.next()) {
				String blockId = rs.getString(BLOCKID);
				if (blockId == null) {continue;}
				String userBlockNodes = rs.getString(USERBLOCKNODES);
				String mapBlockNodes = rs.getString(MAPBLOCKNODES);
				String ignoreBlockNodes = rs.getString(IGNOREBLOCKNODES);
				if (userBlockNodes == null && mapBlockNodes == null) {
					continue;
				}
				
				if (userBlockNodes == null) {userBlockNodes = "";}
				if (mapBlockNodes == null) {mapBlockNodes = "";}
				if (ignoreBlockNodes == null) {ignoreBlockNodes = "";}
				
				StringBuffer newValue = new StringBuffer();
				newValue.append("User(").append(userBlockNodes).append(") ");
				newValue.append("map(").append(mapBlockNodes).append(") ");
				newValue.append("ignore(").append(ignoreBlockNodes).append(")");
				
				String value = userBlockData.get(blockId);
				if (value == null || (value.equals(newValue.toString()) == false)) {
					// 신규 추가 및 업데이트
					Block block = (Block)data.get(blockId);
					if (block != null) {
						// 1) OHT Map에 설정된 Block Node 체크 및 저장
						Vector<Node> userBlockNodeList = new Vector<Node>();
						if (mapBlockNodes.length() > 0) {
							String[] nodeArry = mapBlockNodes.split(",");
							StringBuffer invalidNodes = new StringBuffer(); 
							for (int i = 0; i < nodeArry.length; i++) {
								Node node = nodeManager.getNode(nodeArry[i]);
								if (node == null) {
									// userBlockNodeList clear 및 node invalid 로그 기록
									if(invalidNodes.length() > 0) {
										invalidNodes.append(",");
									}
									invalidNodes.append(nodeArry[i]);
									continue;
								}
								if (userBlockNodeList.contains(node) == false) {
									userBlockNodeList.add(node);
								}
							}
							if (invalidNodes.length() > 0) {
								StringBuffer log = new StringBuffer();
								log.append("UserBlock Error(").append(blockId).append(") - Invalid NodeId(").append(invalidNodes);
								log.append("), MapBlockNodes(").append(mapBlockNodes).append(")");
								registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
								trace(log.toString());
							}
						}
						
						// 2) OHT Map에 설정된 Block Node 중 제외 대상 노드 삭제
						if (userBlockNodeList.size() > 0 && ignoreBlockNodes.length() > 0) {
							String[] nodeArry = ignoreBlockNodes.split(",");
							StringBuffer invalidNodes = new StringBuffer();
							for (int i = 0; i < nodeArry.length; i++) {
								Node node = nodeManager.getNode(nodeArry[i]);
								if (node == null) {
									// node invalid 로그 기록
									if(invalidNodes.length() > 0) {
										invalidNodes.append(",");
									}
									invalidNodes.append(nodeArry[i]);
									continue;
								}
								userBlockNodeList.remove(node);
							}
							if (invalidNodes.length() > 0) {
								StringBuffer log = new StringBuffer();
								log.append("UserBlock Error(").append(blockId).append(") - Invalid NodeId(").append(invalidNodes);
								log.append("), IgnoreBlockNodes(").append(ignoreBlockNodes).append(")");
								registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
								trace(log.toString());
							}
						}
						
						// 3) 사용자가 설정한 Block Node 체크 및 저장
						if(userBlockNodes.length() > 0) {
							String[] nodeArry = userBlockNodes.split(",");
							Vector<Node> tmpNodeList = new Vector<Node>();
							StringBuffer invalidNodes = new StringBuffer();
							for (int i = 0; i < nodeArry.length; i++) {
								Node node = nodeManager.getNode(nodeArry[i]);
								if (node == null) {
									// tmpNodeList clear 및 node invalid 로그 기록
									if(invalidNodes.length() > 0) {
										invalidNodes.append(",");
									}
									invalidNodes.append(nodeArry[i]);
									continue;
								}
								if (tmpNodeList.contains(node) == false) {
									tmpNodeList.add(node);
								}
							}
							if (tmpNodeList.size() > 0) {
								userBlockNodeList.addAll(tmpNodeList);
							}
							if (invalidNodes.length() > 0) {
								StringBuffer log = new StringBuffer();
								log.append("UserBlock Error(").append(blockId).append(") - Invalid NodeId(").append(invalidNodes);
								log.append("), UserBlockNodes(").append(userBlockNodes).append(")");
								registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
								trace(log.toString());
							}
						}
						
						// 2012.03.02 by MYM : Exception 처리
						try {
							// 4) block의 userBlock 반영
							String retVal = block.updateUserNode(userBlockNodeList);
							StringBuffer log = new StringBuffer();
							if (OK.equals(retVal)) {
								log.append("Update UserBlock(").append(blockId).append(")");
								addBlockToUpdateBlockInfoList(block);
							} else {
								log.append("Fail To Update UserBlock(").append(blockId).append(") ").append(retVal);
								registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
							}
							log.append(" {").append(newValue).append(" UserBlockNodes").append(userBlockNodeList);
							log.append("SystemBlock[").append(block.getSystemBlockNodeListString()).append("] AppliedBlock[");
							log.append(block.getBlockNodeListString()).append("]}");
							trace(log.toString());
						} catch (Exception e) {
							writeExceptionLog(LOGFILENAME, e);
						}
						userBlockData.put(blockId, newValue.toString());
					}
				}
				
				removeKeys.remove(blockId);
			}
			
			for (String rmKey : removeKeys) {
				String value = userBlockData.remove(rmKey);
				Block block = (Block)data.get(rmKey);
				if (block != null) {
					String retVal = block.updateUserNode(new Vector<Node>());
					StringBuffer log = new StringBuffer();
					if(OK.equals(retVal)) {
						log.append("Delete UserBlock(").append(rmKey).append(") ").append(value);
						addBlockToUpdateBlockInfoList(block);
					} else {
						log.append("Fail To Update UserBlock(").append(rmKey).append(") ").append(value);
						registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
					}
					trace(log.toString());
				}
			}
			
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
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		
		return result;
	}
	
	/**
	 * 2013.05.10 by KYK
	 * @return
	 */
	private boolean updateUserBlockNode7() {
		if (userBlockData == null) {
			userBlockData = new ConcurrentHashMap<String, String>();
		}		
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_USERBLOCK_SQL);
			rs = pstmt.executeQuery();			
			Set<String> removeKeys = new HashSet<String>(userBlockData.keySet());
			
			while (rs.next()) {
				String blockId = rs.getString(BLOCKID);
				String userBlockNodes = rs.getString(USERBLOCKNODES);
				if (blockId == null || userBlockNodes == null) {
					continue;
				}
				StringBuffer newValue = new StringBuffer();
				newValue.append("User(").append(userBlockNodes).append(") ");
				
				String value = userBlockData.get(blockId);
				if (value == null || (value.equals(newValue.toString()) == false)) {
					// Add or Update
					Block block = (Block)data.get(blockId);
					if (block != null) {
						Vector<Node> userBlockNodeList = new Vector<Node>();						
						if(userBlockNodes.length() > 0) {
							String[] nodeArry = userBlockNodes.split(",");
							Vector<Node> tmpNodeList = new Vector<Node>();
							StringBuffer invalidNodes = new StringBuffer();
							for (int i = 0; i < nodeArry.length; i++) {
								Node node = nodeManager.getNode(nodeArry[i]);
								if (node == null) {
									if(invalidNodes.length() > 0) {
										invalidNodes.append(",");
									}
									invalidNodes.append(nodeArry[i]);
									continue;
								}
								if (tmpNodeList.contains(node) == false) {
									tmpNodeList.add(node);
								}
							}
							if (tmpNodeList.size() > 0) {
								userBlockNodeList.addAll(tmpNodeList);
							}
							if (invalidNodes.length() > 0) {
								StringBuffer log = new StringBuffer();
								log.append("UserBlock Error(").append(blockId).append(") - Invalid NodeId(").append(invalidNodes);
								log.append("), UserBlockNodes(").append(userBlockNodes).append(")");
								registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
								trace(log.toString());
							}
						}
						
						try {
							// 4) block의 userBlock 반영
							String retVal = block.updateUserNode7(userBlockNodeList);
							StringBuffer log = new StringBuffer();
							if (OK.equals(retVal)) {
								log.append("Update UserBlock(").append(blockId).append(")");
								addBlockToUpdateBlockInfoList(block);
							} else {
								log.append("Fail To Update UserBlock(").append(blockId).append(") ").append(retVal);
								registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
							}
							log.append(" {").append(newValue).append(" UserBlockNodes").append(userBlockNodeList);
							log.append("SystemBlock[").append(block.getSystemBlockNodeListString()).append("] AppliedBlock[");
							log.append(block.getBlockNodeListString()).append("]}");
							trace(log.toString());
						} catch (Exception e) {
							writeExceptionLog(LOGFILENAME, e);
						}
						userBlockData.put(blockId, newValue.toString());
					}
				}
				
				removeKeys.remove(blockId);
			}
			
			for (String rmKey : removeKeys) {
				String value = userBlockData.remove(rmKey);
				Block block = (Block)data.get(rmKey);
				if (block != null) {
					String retVal = block.updateUserNode7(new Vector<Node>());
					StringBuffer log = new StringBuffer();
					if(OK.equals(retVal)) {
						log.append("Delete UserBlock(").append(rmKey).append(") ").append(value);
						addBlockToUpdateBlockInfoList(block);
					} else {
						log.append("Fail To Update UserBlock(").append(rmKey).append(") ").append(value);
						registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
					}
					trace(log.toString());
				}
			}			
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
	
	private static final String SELECT_USERDRIVINGQUEUE_SQL = "SELECT * FROM USERDRIVINGQUEUE";
	private boolean updateUserDrivingQueue() {		
		if (userDrivingQueueData == null) {
			userDrivingQueueData = new ConcurrentHashMap<String, String>();
		}

		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_USERDRIVINGQUEUE_SQL);
			rs = pstmt.executeQuery();
			Set<String> removeKeys = new HashSet<String>(userDrivingQueueData.keySet());
			
			while (rs.next()) {
				String drivingQueueId = rs.getString(DRIVINGQUEUEID);
				if (drivingQueueId == null) {continue;}
				String blockId = rs.getString(BLOCKID);
				if (blockId == null) {continue;}
				String type = rs.getString(TYPE);
				if (type == null || (ADD.equals(type) == false && IGNORE.equals(type) == false)) {continue;}
				String nodes = rs.getString(NODES);
				if (nodes == null) {continue;}
				
				StringBuffer newValue = new StringBuffer();
				newValue.append("BlockId(").append(blockId).append(") ");
				newValue.append("Type(").append(type).append(") ");
				newValue.append("Nodes(").append(nodes).append(")");
				
				String value = userDrivingQueueData.get(drivingQueueId);
				if (value == null || (value.equals(newValue.toString()) == false)) {
					// 신규 추가 및 업데이트
					Block block = (Block)data.get(blockId);
					if (block != null) {
						DrivingQueueList dqList = block.getDrivingQueueList(drivingQueueId);
						if (dqList != null) {
							String[] nodeArry = nodes.split(",");
							Vector<Node> nodeList = new Vector<Node>();
							StringBuffer invalidNodes = new StringBuffer(); 
							for (int i = 0; i < nodeArry.length; i++) {
								Node node = nodeManager.getNode(nodeArry[i]);
								if (node == null) {
									// nodeList clear 및 node invalid 로그 기록
									if(invalidNodes.length() > 0) {
										invalidNodes.append(",");
									}
									invalidNodes.append(nodeArry[i]);									
									continue;
								}								
								if (nodeList.contains(node) == false) {
									nodeList.add(node);
								}
							}
							if(invalidNodes.length() > 0) {
								nodeList.clear();
								StringBuffer log = new StringBuffer();
								log.append("UserDrivingQueue Error(").append(drivingQueueId).append(")");
								log.append(" - Invalid NodeId(").append(invalidNodes).append("), Nodes(").append(nodes).append(")");									
								trace(log.toString());
								registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
							}
							
							if (nodeList.size() > 0) {
								String retVal = dqList.updateUserNode(type, nodeList);
								StringBuffer log = new StringBuffer();
								if (OK.equals(retVal)) {
									addBlockToUpdateBlockInfoList(block);
									log.append("Update UserDrivingQueue(").append(drivingQueueId).append(")");
								} else {
									log.append("Fail To Update UserDrivingQueue(").append(drivingQueueId).append(") ").append(retVal);
									if(retVal.indexOf("already have been applied") < 0) {
										registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
									}
								}
								log.append(" {").append(newValue).append(" SystemDQ").append(dqList.getSystemNodeListString());
								log.append(" AppliedDQ").append(dqList.getNodeListString()).append("}");
								trace(log.toString());
							}
							userDrivingQueueData.put(drivingQueueId, newValue.toString());
						}
					}
				}
				removeKeys.remove(drivingQueueId);
			}
			
			for (String rmKey : removeKeys) {
				String value = userDrivingQueueData.remove(rmKey);
				if (value == null) {continue;}
				
				String blockId = value.substring(value.indexOf("(") + 1, value.indexOf(")"));
				Block block = (Block)data.get(blockId);
				if (block != null) {
					DrivingQueueList dqList = block.getDrivingQueueList(rmKey);
					if (dqList != null) {
						String retVal = dqList.updateUserNode(NONE, new Vector<Node>());
						StringBuffer log = new StringBuffer();
						if (OK.equals(retVal)) {
							log.append("Delete UserDrivingQueue(").append(rmKey).append(") ").append(value);
							addBlockToUpdateBlockInfoList(block);
						} else {
							log.append("Fail To Delete UserDrivingQueue(").append(rmKey).append(") ").append(value);
							registerAlarmTextWithLevel(log.toString(), ALARMLEVEL.WARNING, true);
						}
						trace(log.toString());						
					}
				}
			}
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
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
				rs = null;
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {
				}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		
		return result;
	}
	
	/**
	 * 2013.04.19 by KYK
	 * @return
	 */
	private boolean updateUserDrivingQueueRange() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		if (userDrivingQueueList == null) {
			userDrivingQueueList = new ArrayList<DrivingQueueList>();
		}
		ArrayList<DrivingQueueList> tempUserDQList = new ArrayList<DrivingQueueList>(userDrivingQueueList);
		userDrivingQueueList.clear();
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_USERDRIVINGQUEUE_SQL);
			rs = pstmt.executeQuery();
			
			Block block = null;
			DrivingQueueList drivingQueue = null;
			while (rs.next()) {
				String drivingQueueId = rs.getString(DRIVINGQUEUEID);
				String blockId = rs.getString(BLOCKID);
				double dQLimitTime = rs.getDouble(DQLIMITTIME);
				
				block = (Block) data.get(blockId);
				if (block != null) {
					drivingQueue = block.getDrivingQueueList(drivingQueueId);
					if (drivingQueue != null) {
						// set UserDQLimitTime
						drivingQueue.setUserDQLimitTime(dQLimitTime);
						
						userDrivingQueueList.add(drivingQueue);
						if (tempUserDQList.contains(drivingQueue)) {
							tempUserDQList.remove(drivingQueue);
						}
					}
				}
			}
			// reset UserDQLimitTime 0
			for (DrivingQueueList dQ: tempUserDQList) {
				if (dQ != null) {
					dQ.setUserDQLimitTime(0);
				}
			}			
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
	 * 2013.07.30 by KYK
	 * @return
	 */
	private boolean updateUserDrivingQueue7() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		if (userDrivingQueueList == null) {
			userDrivingQueueList = new ArrayList<DrivingQueueList>();
		}
		ArrayList<DrivingQueueList> tempUserDQList = new ArrayList<DrivingQueueList>(userDrivingQueueList);
		userDrivingQueueList.clear();
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_USERDRIVINGQUEUE_SQL);
			rs = pstmt.executeQuery();
			
			Block block = null;
			DrivingQueueList drivingQueue = null;
			while (rs.next()) {
				String drivingQueueId = rs.getString(DRIVINGQUEUEID);
				String blockId = rs.getString(BLOCKID);
				double dQLimitTime = rs.getDouble(DQLIMITTIME);
				
				block = (Block) data.get(blockId);
				if (block != null) {
					drivingQueue = block.getDrivingQueueList(drivingQueueId);
					if (drivingQueue != null) {
						// set UserDQLimitTime
						drivingQueue.setUserDQLimitTime(dQLimitTime);
						
						userDrivingQueueList.add(drivingQueue);
						if (tempUserDQList.contains(drivingQueue)) {
							tempUserDQList.remove(drivingQueue);
						}
					}
				}
			}
			// reset UserDQLimitTime 0
			for (DrivingQueueList dQ: tempUserDQList) {
				if (dQ != null) {
					dQ.setUserDQLimitTime(0);
					// 2013.10.10 by KYK
					userDrivingQueueList.add(dQ);
				}
			}			
			// apply requestedDQLimit to DQLimit
			applyUserDQLimitToBlockDB(userDrivingQueueList);
			
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
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
	
	private static final String UPDATE_REQUESTED_DQLIMIT = "UPDATE BLOCKINFO SET USERDQLIMITTIME=? WHERE DRIVINGQUEUEID=? AND BLOCKID=?";
	/**
	 * 2013.07.30 by KYK
	 * @param userDQMap
	 * @return
	 */
	private boolean updateUserDQLimitToDB() {
		if (userDrivingQueueList == null || userDrivingQueueList.isEmpty()) {
			return true;
		}
		boolean result = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_REQUESTED_DQLIMIT);
			Block block = null;
			for (DrivingQueueList dQ : userDrivingQueueList) {
				if (dQ != null) {
					block = dQ.getBlock();
					if (block != null) {
						block.getBlockId();						
						pstmt.setDouble(1, dQ.getUserDQLimitTime());
						pstmt.setString(2, dQ.getId());
						pstmt.setString(3, block.getBlockId());
						pstmt.executeUpdate();
					}
				}
			}
			pstmt.close();
			result = true;
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	private boolean applyUserDQLimitToBlockDB(ArrayList<DrivingQueueList> userDQList) {
		if (userDQList == null || userDQList.isEmpty()) {
			return true;
		}
		boolean result = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_REQUESTED_DQLIMIT);
			Block block = null;
			for (DrivingQueueList dQ : userDQList) {
				if (dQ != null) {
					block = dQ.getBlock();
					if (block != null) {
						block.getBlockId();						
						pstmt.setDouble(1, dQ.getUserDQLimitTime());
						pstmt.setString(2, dQ.getId());
						pstmt.setString(3, block.getBlockId());
						pstmt.executeUpdate();
					}
				}
			}
			pstmt.close();
			result = true;
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	// 2013.08.01 by KYK : DrivingQueueId 가 unique 하지 않음
//	private static final String UPDATE_BLOCKINFO_SQL = "UPDATE BLOCKINFO SET BLOCKNODES=?, DRIVINGQUEUENODES=? WHERE DRIVINGQUEUEID=?";
	private static final String UPDATE_BLOCKINFO_SQL = "UPDATE BLOCKINFO SET BLOCKNODES=?, DRIVINGQUEUENODES=? WHERE DRIVINGQUEUEID=? AND BLOCKID=?";

	private boolean updateBlockInfoToDB() {
		if (updateBlockInfo == null) {
			return false;
		}
		if(updateBlockInfo.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_BLOCKINFO_SQL);
			StringBuffer log = new StringBuffer("===== Update BlockInfo =====\n");
			for (int i = updateBlockInfo.size() - 1; i >= 0; i--) {
				Block block = updateBlockInfo.get(i);
				ConcurrentHashMap<String, DrivingQueueList> dqMap = block.getDrivingQueueListTable();
				for (Enumeration<DrivingQueueList> e = dqMap.elements(); e.hasMoreElements();) {
					DrivingQueueList dqList = e.nextElement();
					String blockNodes = block.getBlockNodeListString();
					String dqNodes = dqList.getDrivingQueueListString();
					pstmt.setString(1, blockNodes);
					pstmt.setString(2, dqNodes);
					pstmt.setString(3, dqList.getId());
					// 2013.08.01 by KYK
					pstmt.setString(4, block.getBlockId());
					pstmt.executeUpdate();
					
					log.append("[DQID:").append(dqList.getId()).append("] ");
					log.append("[BlockNodes:").append(blockNodes).append("] ");
					log.append("[DQNodes:").append(dqNodes).append("]\n");
				}
				updateBlockInfo.remove(block);
			}
			trace(log.toString());			
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
	 * [Block 개선]
	 * Pattern 에 따라 DrivingQueue 범위 설정에서 Distance(거리) base로 변경
	 * 이후 DrivingQueue 변경은 사용자 정의 Block으로 하도록 함.
	 * 
	 * 2012.01.27 by MYM
	 */
	private void initialize() {
		try {
			OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
			NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);

			// 2013.04.05 by KYK
			NEARBY_TYPE nearbyType = ocsInfoManager.getNearbyType();
			if (nearbyType == NEARBY_TYPE.NEARBY_V7) {
				createBlock7(nodeManager, ocsInfoManager);
				updateUserBlock7();
			} else {
				createBlock(nodeManager, ocsInfoManager);
				// 사용자 Block 및 DrvingQueue 설정
				updateUserBlock();
			}
			
			// DB의 BlockInfo와 새로 만들어진 Block을 비교
			checkDiffBlockInfo();
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	/**
	 * 2013.04.05 by KYK
	 * @param nodeManager
	 * @param ocsInfoManager
	 */
	private void createBlock7(NodeManager nodeManager, OCSInfoManager ocsInfoManager) {
		StringBuffer initilizedBlockLog = new StringBuffer();
		for (Enumeration<Object> e = nodeManager.getData().elements(); e.hasMoreElements();) {
			Node node = (Node) e.nextElement();
			if (node != null) {
				if (node.getSectionCount() >= 3) {
					if (node.isConverge() && node.isDiverge()) {
						String result = createConvergeBlock7(node, true, ocsInfoManager);
						initilizedBlockLog.append(result);
					} else if (node.isConverge()) {
						String result = createConvergeBlock7(node, false, ocsInfoManager);
						initilizedBlockLog.append(result);
					} else if (node.isDiverge()) {
						String result = createDivergeBlock7(node, ocsInfoManager);
						initilizedBlockLog.append(result);
					} else {
						initilizedBlockLog.append("BK_UNK_").append(node.getNodeId()).append("> Unknown block").append("\n");
					}
				}
			} else {
				writeExceptionLog(LOGFILENAME, "initialize() - node is null.");
			}
		}		
		// System Block 정보를 로그에 기록
		writeInitializedBlockInfoLog(initilizedBlockLog.toString());	
	}

	/**
	 * 2013.04.05 by KYK
	 * @param nodeManager
	 * @param ocsInfoManager
	 */
	private void createBlock(NodeManager nodeManager, OCSInfoManager ocsInfoManager) {

		StringBuffer initilizedBlockLog = new StringBuffer();
		for (Enumeration<Object> e = nodeManager.getData().elements(); e.hasMoreElements();) {
			Node node = (Node) e.nextElement();
			// 2012.03.02 by MYM : [NotNullCheck] 추가
			if (node != null) {
				if (node.getSectionCount() >= 3) {
					if (node.isConverge() && node.isDiverge()) {
						String result = createConvergeBlock(node, true, ocsInfoManager);
						initilizedBlockLog.append("*").append(result);
					} else if (node.isConverge()) {
						String result = createConvergeBlock(node, false, ocsInfoManager);
						initilizedBlockLog.append(result);
					} else if (node.isDiverge()) {
						String result = createDivergeBlock(node, ocsInfoManager);
						initilizedBlockLog.append(result);
					} else {
						initilizedBlockLog.append("BK_UNK_").append(node.getNodeId()).append("> Unknown block").append("\n");
					}
				}
			} else {
				writeExceptionLog(LOGFILENAME, "initialize() - node is null.");
			}
		}		
		// System Block 정보를 로그에 기록
		writeInitializedBlockInfoLog(initilizedBlockLog.toString());
	}

	private void trace(String log) {
		blockManagerLogger.debug(log);
	}
	
	private String getLinkType(Node node1, Node node2) {
		if (Math.abs(node2.getLeft() - node1.getLeft()) <= 50 ||
				Math.abs(node2.getTop() - node1.getTop()) <= 50) {
			return LINE;
		}
		return CURVE;
	}
	
	/**
	 * @param node
	 * @param multiType
	 * @param ocsInfoManager
	 * @return
	 */
	private String createConvergeBlock(Node node, boolean multiType, OCSInfoManager ocsInfoManager) {
		/*
		 * [Block 및 DrivingQueueList 생성]
		 * 
		 * 1. Block 객체를 생성하고 합류 노드를 Block으로 설정한다.
		 * 2. 합류 노드의 Section중  진입하는 Section을 가져온다.
		 * 3. DrivingQueueList 생성
		 *    1) 현재의 Section, cost, distance을 큐에 저장한다.
		 *			 -- 큐에 값(Section)이 없을 때까지 Loop --        
		 *    2) 큐에서 첫번째 Section, cost, distance을 가져온다.
		 *    3) Section에서  이전노드(PrevNode)를 가져온다.
		 *    4) 현재노드(CurrNode) ~ 이전노드(PrevNode)의 cost, distance, linkType 를 가져온다.
		 *    5) DrivingQueueNode Count가 REALNODEMINCOUNT이상이고 distance값이 DQ_DISTANCE_RANGE보다 크거나 linkType이 "CURVE"이면 Loop를 빠져나온다.
		 *    6) linkType이 "LINE"이고 distance가 1300mm 이하이면 이전노드(PrevNode)를 Block으로 설정한다.
		 *       linkType이 "LINE"이 아니면 이전노드(PrevNode)를 DrivingQueueList에 저장한다.
		 *    9) 이전노드(PrevNode)를 현재노드(CurrNode)로 저장한다.
		 *    10) 현재노드(CurrNode)가 합류이면 Loop를 빠져나온다.
		 *    11) 현재노드가 Section의 마지막 노드이면 이전 Section을 찾아  Section, cost, distance을 큐에 저장한다.
		 *       -- Loop 탈출 후 Pattern 적용 --
		 */
		
		Block block = new Block(node, CONVERGE, multiType, this, ocsInfoManager);
		block.addNode(node);
		node.addBlock(block);
		data.put(block.getBlockId(), block);
		
		StringBuffer blockLog = new StringBuffer();
		for (int i = 0; i < node.getSectionCount(); i++) {
			Section section = node.getSection(i);
			if (node.equals(section.getFirstNode())) {
				continue;
			}

			String drivingQueueId = section.getNode(section.getNodeCount()-2).getNodeId();
			String drivingQueueType = section.getType();

			double DQ_DISTANCE_RANGE = 6000;
			if (LINE.equals(drivingQueueType)) {
				DQ_DISTANCE_RANGE = ocsInfoManager.getDrivingQueueLineDistance();
			} else {
				DQ_DISTANCE_RANGE = ocsInfoManager.getDrivingQueueCurveDistance();
			}
				
			// DrivingQueue를 생성
			DrivingQueueList drivingQueueList = new DrivingQueueList(drivingQueueId, drivingQueueType, block);
			blockLog.append("C_").append(block.getBlockId()).append("> ");
			blockLog.append("[").append(drivingQueueId).append("_").append(drivingQueueType).append("]");
			
			// Backward Search를 진행하면서 대기 노드를 찾고 DrivingNodeList에 등록한다.
			Node prevNode; 
			Node currNode = node;

			int distance = 0;
			double time = 0;
			int realNodeCount = 0;
			boolean isConsecutiveBlockNode = true;

			Vector<Section> queue = new Vector<Section>();
			queue.add(section);
			Vector<Integer> distanceQueue = new Vector<Integer>();
			distanceQueue.add(new Integer(distance));
			Vector<Double> timeCostQueue = new Vector<Double>();
			timeCostQueue.add(new Double(time));
			
			while (true) {
				if (queue.size() == 0) {
					break;
				}

				section = (Section) queue.remove(0);
				distance = ((Integer) distanceQueue.remove(0)).intValue();
				time = ((Double) timeCostQueue.remove(0)).doubleValue();
				currNode = section.getLastNode();
				for (int j = section.getNodeCount() - 2; j >= 0; j--) {
					// 거리 및 시간 계산
					prevNode = section.getNode(j);
					distance += currNode.getMoveInDistance(prevNode);
					time += currNode.getMoveInTime(prevNode);
					
					if (realNodeCount >= REALNODEMINCOUNT 
							&& (distance > DQ_DISTANCE_RANGE || 
									(LINE.equals(drivingQueueType) && 
									 CURVE.equals(getLinkType(currNode, prevNode))))) {
						break;
					}

					// DrivingQueue에 Node 추가
					drivingQueueList.addNode(prevNode);
					prevNode.addBlock(block);
					blockLog.append(" ").append(prevNode.getNodeId());

					// 2012.02.20 by MYM : realNodeCount 카운트 조건 변경
					// . DrivingQueue 설정 시 BlockRange 값 범위의 노드는 RealNode로 카운트하지 않도록 함.
//					if (distance <= BLOCKRANGE) {
//						block.addNode(prevNode);
//						blockLog.append("(B)");
//					}
					// 2012.03.09 by MYM : BlockRange 이내에서 타 Block의 MainBlock Node는 BlockNode에 포함되지 않도록 조건 추가
//					if (distance <= BLOCKRANGE) { 
					if (distance <= BLOCKRANGE 
							&& isConsecutiveBlockNode == true
							&& prevNode.isDiverge() == false
							&& prevNode.isConverge() == false) {
						// 합류 이전 Path가 Line인 경우는 1300mm 범위 이내에 노드는 Block 노드로 설정
						block.addNode(prevNode);
						blockLog.append("(B)");
					} else if (isConsecutiveBlockNode == true
							&& prevNode.isDiverge() == false
							&& prevNode.isConverge() == false
							&& COLLISION.equals(prevNode.getCollisionType())) {
						// OHTMap의 Block노드인 경우
						// Initialize에서는 SystemBlock만 적용하고 OHTMap의 Block은 UserBlock에서 적용하도록 함.
						;
					} else {
						isConsecutiveBlockNode = false;
						// System 및 OHTMap의 Block 노드가 아닌 노드인 경우만 realNode로 카운트하도록 함.
						if (prevNode.isVirtual() == false) {
							realNodeCount++;
						}
					}
					
					// 이전 노드를 현재노드로 저장
					currNode = prevNode;

					// 이전 검색할 Section 확인 후 저장
					if (j == 0) {
						for (int k = 0; k < currNode.getSectionCount(); k++) {
							section = currNode.getSection(k);
							if (currNode.equals(section.getLastNode())) {
								if (currNode.isConverge()) {
									// Converge이면 Line인 경우에는 추가 검색
									if(LINE.equals(section.getType())) {
										queue.add(section);
										distanceQueue.add(new Integer(distance));
										timeCostQueue.add(new Double(time));
										break;
									}
								} else {
									// Converge가 아니면 추가 검색 
									queue.add(section);
									distanceQueue.add(new Integer(distance));
									timeCostQueue.add(new Double(time));
									break;
								}
							}
						}
					}
				}
			}
			
			block.addDrivingQueueList(drivingQueueList);
			blockLog.append("\n");
		}
		
		return blockLog.toString();
	}
	
	/**
	 * 2013.02.08 by KYK
	 * @param node
	 * @param multiType
	 * @param ocsInfoManager
	 * @return
	 */
	private String createConvergeBlock7(Node node, boolean multiType, OCSInfoManager ocsInfoManager) {
		/*
		 * [Block 및 DrivingQueueList 생성]
		 * 
		 * 1. Block 객체를 생성하고 합류 노드를 Block으로 설정한다.
		 * 2. 합류 노드의 Section중  진입하는 Section을 가져온다.
		 * 3. DrivingQueueList 생성
		 *  BlockNode 의 이전노드를 가져온다. DQId
		 *  Section : Type, Distance, ArrivedTime
		 */
		
		// 2013.07.30 by KYK
//		Block block = new Block(node, CONVERGE, multiType, this, ocsInfoManager);
		Block block = null;
		if (multiType) {
			block = new Block(node, MULTI, multiType, this, ocsInfoManager);
		} else {
			block = new Block(node, CONVERGE, multiType, this, ocsInfoManager);			
		}
		block.addNode(node);
		node.addBlock(block);
		data.put(block.getBlockId(), block);
		
		StringBuffer blockLog = new StringBuffer();
		for (int i = 0; i < node.getSectionCount(); i++) {
			Section section = node.getSection(i);
			if (node.equals(section.getFirstNode())) {
				continue;
			}
			
			Node prevNode = section.getNode(section.getNodeCount()-2);
			
			String drivingQueueId = prevNode.getNodeId();
			String drivingQueueType = section.getType();
			
			// DrivingQueue를 생성
			DrivingQueueList drivingQueue = new DrivingQueueList(drivingQueueId, drivingQueueType, block);
			drivingQueue.addNode(prevNode);
			block.addDrivingQueueList(drivingQueue);
			// 2013.04.12 by KYK
			prevNode.addBlock(block); // DQNode 에서 Block 가져올 수 있도록 함
			
			double distance = node.getMoveInDistance(prevNode);
			double arrivedTime = node.getMoveInTime(prevNode);
			drivingQueue.setDistanceToBlock(distance);
			drivingQueue.setArrivedTimeToBlock(arrivedTime);
			
			blockLog.append("C_").append(block.getBlockId()).append("> ");
			blockLog.append("[").append(drivingQueueId).append("_").append(drivingQueueType).append("]");
			blockLog.append(" DistanceToBlock:").append(distance).append(", ArrivedTime:").append(arrivedTime);
			blockLog.append("\n");
		}		
		return blockLog.toString();
	}

	/**
	 * 
	 * @param node
	 * @param ocsInfoManager
	 * @return
	 */
	private String createDivergeBlock(Node node, OCSInfoManager ocsInfoManager) {
		/*
		 * [Block 및 DrivingQueueList 생성]
		 * 
		 * 1. Block 객체를 생성하고 분기 노드를 Block으로 설정한다.
		 * 2. 분기 노드의 Section중  진입하는 Section을 가져온다.
		 * 3. 분기 전방 직진구간의 1300mm 이내의 노드는 Block 및 DrivingQueueNode로 설정한다.
		 * 4. 분기 노드를 DrivingQueueNode로 설정한다.
		 * 5. DrivingQueueNode 설정
		 *    1) 현재의 Section, cost, distance을 큐에 저장한다.
		 *			 -- 큐에 값(Section)이 없을 때까지 Loop --        
		 *    2) 큐에서 첫번째 Section, cost, distance을 가져온다.
		 *    3) Section에서  이전노드(PrevNode)를 가져온다.
		 *    4) 현재노드(CurrNode) ~ 이전노드(PrevNode)의 cost, distance를 가져온다.
		 *    5) cost값이 DQ_DISTANCE_RANGE보다 크면 Loop를 빠져나온다.
		 *    6) 이전노드(PrevNode)를 DrvingQueue로 저장한다.
		 *    7) 이전노드(PrevNode)를 현재노드(CurrNode)로 저장한다.
		 *    8) 현재노드(CurrNode)가 합류이면 Loop를 빠져나온다.
		 *    9) 현재노드가 Section의 마지막 노드이면 이전 Section을 찾아  Section, cost, distance을 큐에 저장한다.
		 * 
		 */	
		Block block = new Block(node, DIVERGE, false, this, ocsInfoManager);
		block.addNode(node);
		node.addBlock(block);
		data.put(block.getBlockId(), block);
		
		StringBuffer blockLog = new StringBuffer();
		for (int i = 0; i < node.getSectionCount(); i++) {
			Section section = node.getSection(i);
			if (node.equals(section.getFirstNode())) {
				continue;
			}
			
			// Backward Search를 진행하면서 대기 노드를 찾고 DrivingNodeList에 등록한다.
			String drivingQueueId = section.getNode(section.getNodeCount()-2).getNodeId();
			String drivingQueueType = section.getType();
			
			// DrivingQueue를 생성
			DrivingQueueList drivingQueueList = new DrivingQueueList(drivingQueueId, drivingQueueType, block);
			blockLog.append("D_").append(block.getBlockId()).append("> ");
			blockLog.append("[").append(drivingQueueId).append("_").append(drivingQueueType).append("]");
			
			Node currNode = node;
			Node prevNode, nextNode;
			int distance = 0;
			double time = 0;

			// 분기 전방의 1300mm 이내의 노드는 Block 및 DrivingQueue로 설정
			for (int j = 0; j < node.getSectionCount(); j++) {
				Section forwardSection = node.getSection(j);
				if (node.equals(forwardSection.getLastNode())) {
					continue;
				}

				time = 0;
				distance = 0;
				currNode = node;
				for (int k = 1; k < forwardSection.getNodeCount(); k++) {
					nextNode = forwardSection.getNode(k);
					distance += nextNode.getMoveInDistance(currNode);
					time += nextNode.getMoveInTime(currNode);

					// 2011.12.02 by MYM : 다른 Block의 분기/합류 노드는 Block에 포함시키지 않도록 조건 추가
					if (nextNode.isDiverge() || nextNode.isConverge()) {
						break;
					} else if (distance > BLOCKRANGE) {
						break;
					}

					// Block 설정
					block.addNode(nextNode);
					nextNode.addBlock(block);
					
					// DrivingQueue 설정
					drivingQueueList.addNodeToFirst(nextNode);
					blockLog.append(" ").append(nextNode.getNodeId()).append("(B)");
					currNode = nextNode;
				}
			}

			time = 0;
			distance = 0;
			Vector<Section> queue = new Vector<Section>();
			queue.add(section);
			Vector<Integer> distanceQueue = new Vector<Integer>();
			distanceQueue.add(new Integer(distance));
			Vector<Double> timeCostQueue = new Vector<Double>();
			timeCostQueue.add(new Double(time));
			int realNodeCount = 0;

			drivingQueueList.addNode(node);
			blockLog.append(" ").append(node.getNodeId()).append("(B)");
			if (node.isVirtual() == false) {
				realNodeCount++;
			}

			while (true) {
				if (queue.size() == 0) {
					break;
				}

				section = (Section) queue.remove(0);
				distance = ((Integer) distanceQueue.remove(0)).intValue();
				time = ((Double) timeCostQueue.remove(0)).doubleValue();
				currNode = section.getLastNode();
				for (int j = section.getNodeCount() - 2; j >= 0; j--) {
					// 거리 계산
					prevNode = section.getNode(j);
					distance += currNode.getMoveInDistance(prevNode);
					time += currNode.getMoveInTime(prevNode);

					// DQ 범위를 초과하면 Stop
					if (realNodeCount >= REALNODEMINCOUNT
							&& (distance > ocsInfoManager.getDrivingQueueLineDistance() 
									|| CURVE.equals(getLinkType(currNode, prevNode)))) {
						break;
					}

					if (prevNode.isVirtual() == false) {
						realNodeCount++;
					}
					
					// DrivingQueue에 Node 추가
					drivingQueueList.addNode(prevNode);
					prevNode.addBlock(block);
					blockLog.append(" ").append(prevNode.getNodeId());

					// 이전 노드를 현재노드로 저장
					currNode = prevNode;

					// 이전 검색할 Section 확인
					if (j == 0) {
						for (int k = 0; k < currNode.getSectionCount(); k++) {
							section = currNode.getSection(k);
							if (currNode.equals(section.getLastNode())) {
								if (currNode.isConverge()) {
									// Converge이면 Line인 경우에는 추가 검색
									if(LINE.equals(section.getType())) {
										queue.add(section);
										distanceQueue.add(new Integer(distance));
										timeCostQueue.add(new Double(time));
										break;
									}
								} else {
									// Converge가 아니면 추가 검색 
									queue.add(section);
									distanceQueue.add(new Integer(distance));
									timeCostQueue.add(new Double(time));
									break;
								}
							}
						}
					}
				}
			}
			
			block.addDrivingQueueList(drivingQueueList);
			blockLog.append("\n");
		}
		
		return blockLog.toString();
	}
	
	/**
	 * 2013.02.08 by KYK
	 * @param node
	 * @param ocsInfoManager
	 * @return
	 */
	private String createDivergeBlock7(Node node, OCSInfoManager ocsInfoManager) {
		/*
		 * [Block 및 DrivingQueueList 생성]
		 * 
		 * 1. Block 객체를 생성하고 분기 노드를 Block으로 설정한다.
		 * 2. 분기 노드의 Section중  진입하는 Section을 가져온다.
		 */	
		Block block = new Block(node, DIVERGE, false, this, ocsInfoManager);
		block.addNode(node);
		node.addBlock(block);
		data.put(block.getBlockId(), block);
		
		StringBuffer blockLog = new StringBuffer();
		
		String drivingQueueId = node.getNodeId();
		for (int i = 0; i < node.getSectionCount(); i++) {
			Section section = node.getSection(i);
			if (node.equals(section.getFirstNode())) {
				continue;
			}
			// DrivingQueue를 생성
			
			DrivingQueueList drivingQueue = new DrivingQueueList(drivingQueueId, section.getType(), block);
			drivingQueue.addNode(node);
			block.addDrivingQueueList(drivingQueue);
			
			blockLog.append("D_").append(block.getBlockId()).append("> ");
			blockLog.append("[").append(drivingQueueId).append("_").append(section.getType()).append("]");
			blockLog.append("\n");
		}		
		return blockLog.toString();
	}
	
	// 2011.11.14 by MYM
	private static final String DELETE_SQL = "DELETE BLOCKINFO PURGE";
	// 2013.07.31 by KYK : UserDQLimitTime 추가
	private static final String INSERT_SQL = "INSERT INTO BLOCKINFO (DRIVINGQUEUEID, BLOCKID, BLOCKTYPE, BLOCKNODES, SYSTEMBLOCKNODES, DRIVINGQUEUENODES, SYSTEMDRIVINGQUEUENODES, UPDATIONTIME, USERDQLIMITTIME) VALUES (?,?,?,?,?,?,?,SYSDATE,?)";

	
	private boolean insertBlockInfoToDB() {
		boolean result = false;
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement pstmt = null;
		long batchStartTime = System.currentTimeMillis();
		
		writeLog(LOGFILENAME, " BLOCKINFO INSTALL START");
		
		// Block 데이터 DB에 저장
		try {
			conn = dbAccessManager.getNewConnection();
			conn.setAutoCommit(false);
			
			// 먼저 지우는거 : 같은 커넥션이니까 하다가 실패하면 원래꺼 안지워진다..
			stmt = conn.createStatement();
			stmt.executeUpdate(DELETE_SQL);
			stmt.close();
			
			pstmt = conn.prepareStatement(INSERT_SQL);

			int idx = 1;
			for (Iterator<Object> it = data.values().iterator(); it.hasNext();) {
				Block block = (Block) it.next();
				// 2012.03.02 by MYM : [NotNullCheck] 추가
				if (block != null) {
					ConcurrentHashMap<String, DrivingQueueList> drivingQueueListTable = block.getDrivingQueueListTable();
					if (drivingQueueListTable != null) {
						for (Iterator<?> keyIt = drivingQueueListTable.keySet().iterator(); keyIt.hasNext();) {
							String drivingQueueId = (String) keyIt.next();
							DrivingQueueList dqList = drivingQueueListTable.get(drivingQueueId);
							pstmt.setString(1, drivingQueueId);
							pstmt.setString(2, block.getBlockId());
							pstmt.setString(3, block.getBlockType());
							pstmt.setString(4, block.getBlockNodeListString());
							pstmt.setString(5, block.getSystemBlockNodeListString());
							StringBuffer dql = new StringBuffer();
							// 2012.03.02 by MYM : [NotNullCheck] 추가
							if(dqList != null) {
								for (int i = 0; i < dqList.size(); i++) {
									Node node = dqList.get(i);									
									if (node != null) {
										dql.append(node.getNodeId());
										if (i != dqList.size() - 1) {
											dql.append(",");
										}
									} else {
										writeExceptionLog(LOGFILENAME, "insertBlockInfoToDB() - node is null.");
									}
								}
								pstmt.setString(6, dql.toString());
								
								// 2012.01.16 by MYM : BlockInfo의 SystemDrivingQueueNodes 추가
								StringBuffer systemDQl = new StringBuffer();
								for (int i = 0; i < dqList.systemNodeSize(); i++) {
									Node node = dqList.getSystemNode(i);
									// 2012.03.02 by MYM : [NotNullCheck] 추가
									if (node != null) {
										systemDQl.append(node.getNodeId());
										if (i != dqList.systemNodeSize() - 1) {
											systemDQl.append(",");
										}
									} else {
										writeExceptionLog(LOGFILENAME, "insertBlockInfoToDB() - node is null.");
									}
								}					
								pstmt.setString(7, systemDQl.toString());
								
								// 2013.07.31 by KYK : ??
								if (dqList.getUserDQLimitTime() > 0) {
									pstmt.setDouble(8, dqList.getUserDQLimitTime());
								} else {
									pstmt.setString(8, "");
								}
								
								pstmt.addBatch();
								pstmt.clearParameters();
								idx++;
								if (idx % 200 == 0) {
									pstmt.executeBatch();
									idx = 1;
								}
							} else {
								writeExceptionLog(LOGFILENAME, "insertBlockInfoToDB() - dqList is null.");
							}							
						}
					} else {
						writeExceptionLog(LOGFILENAME, "insertBlockInfoToDB() - drivingQueueListTable is null.");
					}					
				} else {
					writeExceptionLog(LOGFILENAME, "insertBlockInfoToDB() - block is null.");
				}				
			}
			if (idx > 1) {
				pstmt.executeBatch();
			}
			conn.commit();
			result = true;
			
			writeLog(LOGFILENAME, " BLOCKINFO INSTALL END : ElapsedTime " + (System.currentTimeMillis() - batchStartTime) + "ms");
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (Exception e) {}
				stmt = null;
			}
			
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
			
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception ignore) {
					conn = null;
				}
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	private static final String UPDATE_DRIVINGQUEUE_SQL = "UPDATE BLOCKINFO SET PREEMPTIVEVEHICLE=?, DRIVINGQUEUEVEHICLES=?, UPDATIONTIME=SYSDATE WHERE DRIVINGQUEUEID=?";

	@SuppressWarnings("unchecked")
	public boolean updateDrivingQueueToDB() {
		if (updateDrivingQueue.size() == 0) {
			return true;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		Vector<String> updateDrivingQueueClone = null;
		try {
			String blockId = null;
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_DRIVINGQUEUE_SQL);
			updateDrivingQueueClone = (Vector<String>)updateDrivingQueue.clone();
			ListIterator<String> iterator = updateDrivingQueueClone.listIterator();
			while (iterator.hasNext()) {
				blockId = iterator.next();
				if (blockId != null) {
					Block block = (Block) data.get(blockId);
					if (block != null) {
						// 2012.06.09 by MYM : Block의 drivingVehicle를 List로 변경
						ConcurrentHashMap<String, DrivingQueueList> drivingQueueTable = block.getDrivingQueueListTable();
						for (Enumeration<String> e = drivingQueueTable.keys(); e.hasMoreElements();) {
							String drivingQueueId = e.nextElement();
							DrivingQueueList drivingQueueList = drivingQueueTable.get(drivingQueueId);
							StringBuffer drivingQueueVehicles = new StringBuffer();
							String drivingQueueVehiclesInfo = "";
							if (drivingQueueList != null) {
								for (int i = 0; i < drivingQueueList.size(); i++) {
									Node node = drivingQueueList.get(i);
									if (node != null) {
										drivingQueueVehicles.append(node.getNodeId());
										if (node.getDriveVehicleCount() <= 0) {
											drivingQueueVehicles.append("()");
										} else {
											drivingQueueVehicles.append("(");								
											String vehicles = node.toVehicleString();
											drivingQueueVehicles.append(vehicles);
											drivingQueueVehicles.append(")");
										}
										
										if (i != drivingQueueList.size() - 1) {
											drivingQueueVehicles.append(",");
										}
									} else {
										writeExceptionLog(LOGFILENAME, "updateDrivingQueueToDB() - drivingQueue is null.");
									}
								}
							}
							drivingQueueVehiclesInfo = drivingQueueVehicles.toString();
							if (drivingQueueVehiclesInfo.length() > 1024) {
								drivingQueueVehiclesInfo = drivingQueueVehiclesInfo.substring(0, 1024);
							}
							
							// 2012.06.12 by MYM : 분기는 DrivingVehicle이 N개가 될 수 있어 DB의 칼럼 사이즈로 제한하도록 수정 
							String drivingVehicles = block.getDrivingVehicles();
							if (drivingVehicles.length() > 32) {
								drivingVehicles = drivingVehicles.substring(0, 32);
							}
							
							pstmt.setString(1, drivingVehicles);							
							pstmt.setString(2, drivingQueueVehiclesInfo);
							pstmt.setString(3, drivingQueueId);
							// 2014.03.14 by KYK
//							pstmt.executeUpdate();
							pstmt.addBatch();
						}
					}
				}
				updateDrivingQueue.remove(blockId);
			}
			// 2014.03.14 by KYK
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
		}
		finally {
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
	 * @param blockId
	 */
	public void addBlockToUpdateList(String blockId) {
		// 2014.10.15 by KYK : Primary, Secondary 동시 업데이트 되는 문제
		if (serviceState == MODULE_STATE.INSERVICE) {
			if (blockId != null) {
				updateDrivingQueue.add(blockId);
			} else {
				writeExceptionLog(LOGFILENAME, "addBlockToUpdateList() - blockId is null.");
			}
		}
	}
	
	private void addBlockToUpdateBlockInfoList(Block block) {
		if (serviceState == MODULE_STATE.OUTOFSERVICE) {
			return;
		}
		
		if (block != null && updateBlockInfo != null) {
			updateBlockInfo.add(block);
		} else {
			writeExceptionLog(LOGFILENAME, "addBlockToUpdateBlockInfoList() - blockId is null.");
		}
	}
	
	/**
	 * 2011.12.05 by MYM
	 * 
	 * @param log
	 */
	private void writeInitializedBlockInfoLog(String log) {
		File file;
		FileWriter out = null;
		try {
			file = new File("InitializedBlockInfo.log");
			out = new FileWriter(file, false);
			out.write(log);
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static final String SELECT_BLOCKINFO_SQL = "SELECT * FROM BLOCKINFO";
	private void checkDiffBlockInfo() {
		try {
			// 2012.01.16 by MYM : BlockInfo -> Block, DrivingQueue -> DrivingQueueList 변경 
			ConcurrentHashMap<String, String> drivingQueueMap = new ConcurrentHashMap<String, String>();
			for (Enumeration<Object> e = data.elements(); e.hasMoreElements();) {
				Block block = (Block) e.nextElement();
				// 2012.03.02 by MYM : [NotNullCheck] 추가
				if(block != null) {
					ConcurrentHashMap<String, DrivingQueueList> drivingQueueListTable = block.getDrivingQueueListTable();
					if (drivingQueueListTable != null) {
						for (Enumeration<String> e2 = drivingQueueListTable.keys(); e2.hasMoreElements();) {
							String key = e2.nextElement();
							StringBuffer id = new StringBuffer();
							StringBuffer nodes = new StringBuffer(block.getBlockNodeListString()).append("[");
							id.append(block.getBlockId()).append("_").append(key);
							
							DrivingQueueList drivingQueueList = drivingQueueListTable.get(key);
							for (int i = 0; i < drivingQueueList.size(); i++) {
								Node node = drivingQueueList.get(i);
								// 2012.03.02 by MYM : [NotNullCheck] 추가
								if (node != null) {
									nodes.append(node.getNodeId());
									if (i < drivingQueueList.size() - 1) {
										nodes.append(",");
									}
								}
							}
							nodes.append("]");
							drivingQueueMap.put(id.toString(), nodes.toString());
						}
					} else {
						writeExceptionLog(LOGFILENAME, "checkDiffBlockInfo() - drivingQueueListTable is null.");
					}
				} else {
					writeExceptionLog(LOGFILENAME, "checkDiffBlockInfo() - block is null.");
				}
			}

			Connection conn = null;
			ResultSet rs = null;
			PreparedStatement pstmt = null;
			StringBuffer diffLog = new StringBuffer("\n");
			diffLog.append("==[ CheckBlockInfo ]===============================\n");
			try {
				conn = dbAccessManager.getConnection();
				pstmt = conn.prepareStatement(SELECT_BLOCKINFO_SQL);
				rs = pstmt.executeQuery();

				String key = "";
				String value = "";
				String blockId = "";
				String dqId = "";
				String blockNodes = "";
				String dqNodes;
				while (rs.next()) {
					blockId = rs.getString(BLOCKID);
					dqId = rs.getString("DRIVINGQUEUEID");
					key = blockId + "_" + dqId;
					blockNodes = rs.getString("BLOCKNODES");
					dqNodes = rs.getString("DRIVINGQUEUENODES");
					value = blockNodes + "[" + dqNodes + "]";

					String newDQNodes = drivingQueueMap.remove(key);
					if (newDQNodes != null) {
						// Block Modified
						if (checkBlockEqual(newDQNodes.trim(), value.trim()) == false) {
							diffLog.append("MOD [").append(key).append("] ").append(value).append("\n");
							for (int i = 0; i < key.length() + 4; i++) {
								diffLog.append(" ");
							}
							diffLog.append("→ ").append(newDQNodes).append("\n");
						}
					} else {
						// Block Deleted
						diffLog.append("DEL [").append(key).append("] ").append(value).append("\n");
					}
				}
				
				// Block Added
				for (Enumeration<String> e = drivingQueueMap.keys(); e.hasMoreElements();) {
					key = e.nextElement();
					value = drivingQueueMap.get(key);
					diffLog.append("ADD [").append(key).append("] ").append(value).append("\n");
				}
				diffLog.append("==[ End of CheckBlockInfo ]========================\n");
			} catch (SQLException se) {
				se.printStackTrace();
				writeExceptionLog(LOGFILENAME, se);
			} catch (Exception e) {
				e.printStackTrace();
				writeExceptionLog(LOGFILENAME, e);
			} finally {
				if (rs != null) {
					try {
						rs.close();
					} catch (Exception e) {
					}
					rs = null;
				}
				if (pstmt != null) {
					try {
						pstmt.close();
					} catch (Exception e) {
					}
					pstmt = null;
				}
			}
			
			trace(diffLog.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean checkBlockEqual(String one, String two) {
		boolean result = false;

		if (one.length() == two.length()) {
			String[] oneBNodes = one.substring(0, one.indexOf("[")).split(",");
			String[] oneDQNodes = one.substring(one.indexOf("[") + 1, one.indexOf("]")).split(",");
			String[] twoBNodes = one.substring(0, two.indexOf("[")).split(",");
			String[] twoDQNodes = one.substring(two.indexOf("[") + 1, one.indexOf("]")).split(",");

			if (checkBlockEqual(oneBNodes, twoBNodes) == true) {
				if (checkBlockEqual(oneDQNodes, twoDQNodes) == true) {
					result = true;
				}
			}
		}
		return result;
	}

	private boolean checkBlockEqual(String[] one, String[] two) {
		boolean result = true;
		if (one.length == two.length) {
			for (int i = 0; i < one.length; i++) {
				String oneString = one[i];
				if (include(oneString, two)) {
					continue;
				} else {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	private boolean include(String str, String[] strs) {
		boolean result = false;
		for (int i = 0; i < strs.length; i++) {
			if (str.equalsIgnoreCase(strs[i])) {
				return true;
			}
		}
		return result;
	}
}
