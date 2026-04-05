package com.samsung.ocs.failover.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.failover.constant.ClusterConstant;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.model.ClusterInfoItem;
import com.samsung.ocs.failover.model.ClusterInfoItem.MODE;
import com.samsung.ocs.failover.model.HistoryItem;
import com.samsung.ocs.failover.thread.OcsHistoryManagerThread;

/**
 * OcsHistoryManager Class, OCS 3.0 for Unified FAB
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

public class OcsHistoryManager {
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	private AbstractOcsThread thread;
	private DBAccessManager dbam = null;
	private Vector<HistoryItem> historyItemList;
	private Vector<ClusterInfoItem> infoItemList;
	private Vector<HistoryItem> checkDupHistoryItemList;
	private boolean initialized = false;
	
	// 2016.12.15 by KBS : List size 증가 개선 (배경: List에 add만 되고 remove가 되지 않아 size 증가로 GC 다발하는 현상 발생) 
	private static int MAX_ITEM_SIZE = 128;
	
	/**
	 * Constructor of OcsHistoryManager class.
	 */
	public OcsHistoryManager() {
		this.historyItemList = new Vector<HistoryItem>();
		this.infoItemList = new Vector<ClusterInfoItem>();
		this.checkDupHistoryItemList = new Vector<HistoryItem>();
		
		this.thread = new OcsHistoryManagerThread(this);
		thread.start();
	}
	
	/**
	 * 
	 */
	public boolean init() {
		this.dbam = new DBAccessManager();
		while (true) {
			if (dbam.isDBConnected()) {
				break;
			}
			try { Thread.sleep(500); } catch (Exception e) {
			}
		}
		return true;
	}
	
	/**
	 * @param type
	 * @param processName
	 * @param isPrimary
	 * @param alarmText
	 */
	public void addClusterHistory(EVENT_TYPE type, String processName, boolean isPrimary, String alarmText) {
		if (historyItemList.size() < MAX_ITEM_SIZE) {
			HistoryItem item = new HistoryItem();
			item.setAlarmText(alarmText);
			item.setPrimary(isPrimary);
			item.setProcessName(processName);
			item.setType(type);
			item.setSetTime(System.currentTimeMillis());
			historyItemList.add(item);
		} else {
			log("  - Can not add to ClusterHistory list: fulled.");
		}
	}
	
	/**
	 * @param type
	 * @param processName
	 * @param isPrimary
	 * @param alarmText
	 */
	public void addCheckDupClusterHistory(EVENT_TYPE type, String processName, boolean isPrimary, String alarmText) {
		if (checkDupHistoryItemList.size() < MAX_ITEM_SIZE) {
			HistoryItem item = new HistoryItem();
			item.setAlarmText(alarmText);
			item.setPrimary(isPrimary);
			item.setProcessName(processName);
			item.setType(type);
			item.setSetTime(System.currentTimeMillis());
			checkDupHistoryItemList.add(item);
		} else {
			log("  - Can not add to DupClusterHistory list: fulled.");
		}
	}
	
	/**
	 * @param mode
	 * @param isPrimary
	 * @param infoItem
	 * @param isAlive
	 */
	public void addClusterInfo(MODE mode, boolean isPrimary, String infoItem, boolean isAlive) {
		if (infoItemList.size() < MAX_ITEM_SIZE) {
			ClusterInfoItem item = new ClusterInfoItem();
			item.setMode(mode);
			item.setPrimary(isPrimary);
			item.setInfoItem(infoItem);
			item.setIsAlive(isAlive ? ClusterConstant.ALIVE : ClusterConstant.DEAD);
			item.setSetTime(System.currentTimeMillis());
			infoItemList.add(item);
		} else {
			log("  - Can not add to ClusterInfo list: fulled.");
		}
	}
	
	/**
	 * @param mode
	 * @param isPrimary
	 * @param infoItem
	 * @param isAlive
	 */
	public void addClusterInfo(MODE mode, boolean isPrimary, String infoItem, String isAlive) {
		if (infoItemList.size() < MAX_ITEM_SIZE)  {
			ClusterInfoItem item = new ClusterInfoItem();
			item.setMode(mode);
			item.setPrimary(isPrimary);
			item.setInfoItem(infoItem);
			item.setIsAlive(isAlive);
			item.setSetTime(System.currentTimeMillis());
			infoItemList.add(item);
		} else {
			log("  - Can not add to ClusterInfo list: fulled.");
		}
	}
	
	/**
	 * @param mode
	 * @param isPrimary
	 */
	public void addClusterInfo(MODE mode, boolean isPrimary) {
		if (infoItemList.size() < MAX_ITEM_SIZE) {
			ClusterInfoItem item = new ClusterInfoItem();
			item.setMode(mode);
			item.setPrimary(isPrimary);
			item.setSetTime(System.currentTimeMillis());
			infoItemList.add(item);
		} else {
			log("  - Can not add to ClusterInfo list: fulled.");
		}
	}
	
	private static final String CHECK_CLUSERT_HISTORY_SQL = "select host_name||process_name from ocs_cluster_history where alarmcode=? and process_name=? and host_name=? and alarmtext=? and (SHOWMSG is null OR SHOWMSG != 'FALSE')";
	
	private boolean isExistSameClusterHistory(Connection conn, HistoryItem historyItem) throws Exception {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		boolean result = false;
		try {
			pstmt = conn.prepareStatement(CHECK_CLUSERT_HISTORY_SQL);
			pstmt.setQueryTimeout(5);
			pstmt.setInt(1, historyItem.getType().toAlarmCode());
			pstmt.setString(2, historyItem.getProcessName());
			if (historyItem.isPrimary()) {
				pstmt.setString(3, OcsConstant.PRIMARY);
			} else {
				pstmt.setString(3, OcsConstant.SECONDARY);
			}
			pstmt.setString(4, historyItem.getAlarmText());
			
			rs = pstmt.executeQuery();
			if(rs != null && rs.next()) {
				result = true;
			}
		} catch(Exception e) {
			result = false;
			throw e;
		} finally {
			if(rs != null) {
				try { rs.close(); } catch (Exception e) {}
			}
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception e) { }
			}
		}
		return result;
	}
	
	private boolean insertClusterHistory(Connection conn, HistoryItem historyItem) throws Exception {
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			pstmt = conn.prepareStatement(INSERT_CLUSERT_HISTORY_SQL);
			pstmt.setQueryTimeout(5);
			pstmt.setString(1, historyItem.getType().toString() );
			pstmt.setString(2, historyItem.getProcessName());
			if (historyItem.isPrimary()) {
				pstmt.setString(3, OcsConstant.PRIMARY);
			} else {
				pstmt.setString(3, OcsConstant.SECONDARY);
			}
			pstmt.setTimestamp(4, new Timestamp(historyItem.getSetTime()));
			pstmt.setInt(5, historyItem.getType().toAlarmCode());
			pstmt.setString(6, historyItem.getAlarmText() == null ? "" : historyItem.getAlarmText() );
			pstmt.execute();
			result = true;
		} catch(Exception e) {
			result = false;
			throw e;
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception e) { }
			}
		}
		return result;
	}
	
	
	/**
	 * Insert ClusterHistory to DB
	 * 
	 * @return
	 */
	public boolean processCheckDupClusterHistory() {
		
		if(dbam == null) {
			return false;
		}
		
		Connection conn = null;
		boolean result = false;
		boolean oneMoreExecute = false;
		int index = 0;
		try {
			conn = dbam.getConnection();
			conn.setAutoCommit(false);
			
			for (int i = 0; i < checkDupHistoryItemList.size(); i++) {
				HistoryItem historyItem = checkDupHistoryItemList.get(i);
				boolean existSameClusterHistory = isExistSameClusterHistory(conn, historyItem);
				if(existSameClusterHistory == false) {
					insertClusterHistory(conn, historyItem);
					oneMoreExecute = true;
				}
				index++;
			}
			if(oneMoreExecute) {
				conn.commit();
			}
			result = true;
		} catch (Exception ignore) {
			try { conn.rollback(); } catch (Exception ii) {}
			log(ignore);
			result = false;
			dbam.requestDBReconnect();
		} finally {
		}
		if (result == true) {
			for (int i = 0; i < index; i++) {
				checkDupHistoryItemList.remove(0);
			}
		}
		return result;
	}
	
	private static final String INSERT_CLUSERT_HISTORY_SQL = "INSERT INTO ocs_cluster_history (event_type, process_name, host_name, settime, alarmcode, alarmtext) values(?,?,?,?,?,?) ";
	
	/**
	 * Insert ClusterHistory to DB
	 * 
	 * @return
	 */
	public boolean processClusterHistory() {
		
		if(dbam == null) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int index = 0;
		try {
			conn = dbam.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(INSERT_CLUSERT_HISTORY_SQL);
			pstmt.setQueryTimeout(5);
			
			for (int i = 0; i < historyItemList.size(); i++) {
				HistoryItem historyItem = historyItemList.get(i);
				pstmt.setString(1, historyItem.getType().toString() );
				pstmt.setString(2, historyItem.getProcessName());
				if (historyItem.isPrimary()) {
					pstmt.setString(3, OcsConstant.PRIMARY);
				} else {
					pstmt.setString(3, OcsConstant.SECONDARY);
				}
				pstmt.setTimestamp(4, new Timestamp(historyItem.getSetTime()));
				pstmt.setInt(5, historyItem.getType().toAlarmCode());
				pstmt.setString(6, historyItem.getAlarmText() == null ? "" : historyItem.getAlarmText() );
				pstmt.addBatch();
				index++;
			}
			pstmt.executeBatch();
			conn.commit();
			result = true;
		} catch (Exception ignore) {
			try { conn.rollback(); } catch (Exception ii) {}
			log(ignore);
			result = false;
			dbam.requestDBReconnect();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		if (result == true) {
			for (int i = 0; i < index; i++) {
				historyItemList.remove(0);
			}
		}
		return result;
	}
	
	private static final String INIT_CLUSTER_INFO_SQL= "UPDATE ocs_cluster_info SET state = 'DEAD', updation_time=? WHERE host = ?";
	private static final String UPDATE_CLUSTER_INFO_SQL= "UPDATE ocs_cluster_info SET state = ?, updation_time=? WHERE info_item = ? AND host = ?";
	private static final String UNKNOWN_CLUSTER_INFO_SQL= "UPDATE ocs_cluster_info SET state = 'UNKNOWN', updation_time=? WHERE host = ?";
	
	/**
	 * Process ClusterInfo
	 * 
	 * @return
	 */
	public boolean processClusterInfo() {
		
		if(dbam == null) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		int index = 0;
		try {
			conn = dbam.getConnection();
			conn.setAutoCommit(true);
			
			for (int i = 0; i < infoItemList.size(); i++) {
				ClusterInfoItem infoItem = infoItemList.get(i);
				switch (infoItem.getMode()) {
					case FORCEUNKNOWN :
						pstmt = conn.prepareStatement(UNKNOWN_CLUSTER_INFO_SQL);
						pstmt.setQueryTimeout(5);
						pstmt.setTimestamp(1, new Timestamp(infoItem.getSetTime()));
						pstmt.setString(2, infoItem.isPrimary() ?  OcsConstant.PRIMARY : OcsConstant.SECONDARY );
						break;
					case INIT : 
						pstmt = conn.prepareStatement(INIT_CLUSTER_INFO_SQL);
						pstmt.setQueryTimeout(5);
						pstmt.setTimestamp(1, new Timestamp(infoItem.getSetTime()));
						pstmt.setString(2, infoItem.isPrimary() ?  OcsConstant.PRIMARY : OcsConstant.SECONDARY );
						break;
					case UPDATE :
						pstmt = conn.prepareStatement(UPDATE_CLUSTER_INFO_SQL);
						pstmt.setQueryTimeout(5);
						pstmt.setString(1, infoItem.getIsAlive());
						pstmt.setTimestamp(2, new Timestamp(infoItem.getSetTime()));
						pstmt.setString(3, infoItem.getInfoItem() );
						pstmt.setString(4, infoItem.isPrimary() ?  OcsConstant.PRIMARY : OcsConstant.SECONDARY );
						break;
					default :
						break;
				}
				if (pstmt != null) {
					pstmt.execute();
					pstmt.close();
					pstmt = null;
				}
				index++;
			}
			result = true;
		} catch (Exception ignore) {
			log(ignore);
			result = false;
			dbam.requestDBReconnect();
		} finally {
			if (pstmt!=null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		if (result == true) {
			for (int i = 0; i < index ; i++) {
				infoItemList.remove(0);
			}
		}
		return result;
	}
	
	/**
	 * ocs_cluster_info 테이블에 info_item, host를 키로하여 state column 데이터를  "ALIVE", "DEAD" 로 업데이트 하는 메서드
	 * @param dbAccessManager : 접속할 Database의 Connection을 가지는 DBAccessManager 객체.
	 * @param isPrimary : 업데이트를 해당할 호스트가 Primary 인지 Secondary인지 여부.
	 * @param infoItem : 갱신할 item명
	 * @param isAlive : ALIVE 혹은 DEAD
	 * @return
	 */
	/*public boolean updateClusterInfo(DBAccessManager dbAccessManager, boolean isPrimary, String infoItem, boolean isAlive) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(updateClusterinfoSql);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, isAlive ?  ClusterConstant.ALIVE : ClusterConstant.DEAD );
			pstmt.setString(2, infoItem );
			pstmt.setString(3, isPrimary ?  OcsConstant.PRIMARY : OcsConstant.SECONDARY );
			pstmt.execute();
			result = true;
		} catch(Exception ignore) {
			log(ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if(pstmt!=null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}*/
	
	
	
	/**
	 * ocs_cluster_info 테이블에 host를 키로하여 state column 데이터를  "DEAD" 로 업데이트 하는 메서드
	 * @param dbAccessManager
	 * @param isPrimary : 갱신할 Host가 Primary인지 아닌지.
	 * @return
	 */
	/*public boolean initClusterInfo(DBAccessManager dbAccessManager, boolean isPrimary) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(initClusterinfoSql);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, isPrimary ?  OcsConstant.PRIMARY : OcsConstant.SECONDARY );
			pstmt.execute();
			result = true;
		} catch(Exception ignore) {
			log(ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if(pstmt!=null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}*/
	
	
	/**
	 * ocs_cluster_info 테이블에 host를 키로하여 state column 데이터를  "UNKNOWN" 로 업데이트 하는 메서드
	 * @param dbAccessManager
	 * @param isPrimary : 갱신할 Host가 Primary인지 아닌지.
	 * @return
	 */
	/*public boolean unknownClusterInfo(DBAccessManager dbAccessManager, boolean isPrimary) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(unknownClusterinfoSql);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, isPrimary ?  OcsConstant.PRIMARY : OcsConstant.SECONDARY );
			pstmt.execute();
			result = true;
		} catch(Exception ignore) {
			log(ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if(pstmt!=null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;
	}*/


	private static void log(String log) {
		logger.debug(log);
	}
	
	private static void log(Throwable w) {
		logger.debug(w.getMessage(), w);
	}
	
	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	public void close() {
		thread.stopThread();
	}
}
