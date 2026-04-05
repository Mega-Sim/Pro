package com.samsung.ocs.failover.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.CommonLogFileName;

/**
 * OcsDBHistoryDAO Class, OCS 3.0 for Unified FAB
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

public class OcsDBHistoryDAO {
	private static Logger logger = Logger.getLogger(CommonLogFileName.CLUSTERMANAGER);
	private static String DELETE_SQL = "DELETE ocs_cluster_history WHERE TO_CHAR(SETTIME,'YYYYMMDDHH24MISS') < ? ";
	
	/**
	 * Delete ClusterHistory from DB
	 * 
	 * @param dbAccessManager
	 * @param timeBefore
	 * @return
	 */
	public static boolean deleteClusterHistory(DBAccessManager dbAccessManager, String timeBefore ) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		long startTime = System.currentTimeMillis();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(DELETE_SQL);
			pstmt.setQueryTimeout(60);
			pstmt.setString(1, timeBefore);
			pstmt.execute();
			result = true;
		} catch (Exception ignore) {
			log(ignore);
			result = false;
			dbAccessManager.requestDBReconnect();
		} finally {
			if (pstmt!=null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("OcsDBHistoryDAO : OCS_CLUSTER_HISTORY REMOVE SETTIME < [%s] result[%s] elapsedTime[%d]millis.", timeBefore, result, elapsedTime));
		return result;
	}
	
	private static void log(Throwable w) {
		logger.debug(w.getMessage(), w);
	}
	private static void log(String s) {
		logger.debug(s);
	}
}
