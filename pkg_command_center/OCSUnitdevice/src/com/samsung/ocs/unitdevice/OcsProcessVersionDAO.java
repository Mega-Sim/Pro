package com.samsung.ocs.unitdevice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.samsung.ocs.unitdevice.model.OcsProcessVersionVO;

//import com.samsung.ocs.manager.impl.model.Alarm;

/**
 * OcsProcessVersionVO Class, OCS 3.0 for Unified FAB
 * 
 * @author Jongwon.Jung
 * 
 * @date   2019. 07. 25.
 * @version 3.0
 * 
 * Copyright 2019 by SEMES.
 * 
 * This software is the confidential and proprietary information
 * of SEMES. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with SEMES.
 */

public class OcsProcessVersionDAO {
	
	private static final String PRIMARY_VERSION = "PRIMARY_VERSION";
	private static final String PRIMARY_BUILD_DATE = "PRIMARY_BUILD_DATE";
	private static final String SECONDARY_VERSION = "SECONDARY_VERSION";
	private static final String SECONDARY_BUILD_DATE = "SECONDARY_BUILD_DATE";
	
	private static final String RETRIEVE_PROCESS_VERSION = "SELECT primary_version, primary_build_date, primary_include_version, primary_updated_time,"
			+" secondary_version, secondary_build_date, secondary_include_version, secondary_updated_time FROM VERSION_INFO where process_name = ? and rownum=1";
	
	private static final String P_MERGE_VERSION_SQL ="MERGE INTO VERSION_INFO USING DUAL ON (PROCESS_NAME=?)" +					
									  "WHEN MATCHED THEN UPDATE SET PRIMARY_VERSION=?,PRIMARY_BUILD_DATE=?, " +
			                          "PRIMARY_UPDATED_TIME=TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISS')" +
			                          "WHEN NOT MATCHED THEN INSERT (PROCESS_NAME, PRIMARY_VERSION, PRIMARY_BUILD_DATE, PRIMARY_UPDATED_TIME)"+
			                          "VALUES(?,?,?,TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISS'))";
	private static final String S_MERGE_VERSION_SQL ="MERGE INTO VERSION_INFO USING DUAL ON (PROCESS_NAME=?)" +					
			  						  "WHEN MATCHED THEN UPDATE SET SECONDARY_VERSION=?,SECONDARY_BUILD_DATE=?, " +
			  						  "SECONDARY_UPDATED_TIME=TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISS')" +
			  						  "WHEN NOT MATCHED THEN INSERT (PROCESS_NAME, SECONDARY_VERSION, SECONDARY_BUILD_DATE, SECONDARY_UPDATED_TIME)"+
			  						  "VALUES(?,?,?,TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISS'))";
	
	private static final String INSERT_VERSION_INFO_HISTORY_SQL = "INSERT INTO VERSION_INFO_HISTORY (UPDATED_TIME, HOST, PROCESS_NAME, VERSION, BUILD_DATE) values(TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISS'),?,?,?,?) ";
	
	/**
	 * @param processName
	 * @param dbAccessManager
	 * @return
	 */
	public static OcsProcessVersionVO retrieveProcessVersion(String processName, DBAccessManager dbAccessManager) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		OcsProcessVersionVO result = null;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(RETRIEVE_PROCESS_VERSION);
			pstmt.setQueryTimeout(3);
			pstmt.setString(1, processName);
			
			rs = pstmt.executeQuery();
			
			result = new OcsProcessVersionVO();
			while (rs.next()) {
				result.setPrimary_Version(rs.getString(PRIMARY_VERSION));
				result.setPrimary_Bulid_Date(rs.getString(PRIMARY_BUILD_DATE));
				result.setSecondary_Version(rs.getString(SECONDARY_VERSION));
				result.setSecondary_Bulid_Date(rs.getString(SECONDARY_BUILD_DATE));
				break;
			}
		} catch (Exception ignore) {
			result = null;
			dbAccessManager.ReconnectToDB();
		} finally {
			if (rs != null) {
				try { rs.close(); } catch (Exception ignore) {}
			}
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore) {}
			}
		}
		return result;
	}
	
	/**
	 * @param main
	 * @param dbAccessManager
	 * @param hostServiceType
	 */
	public static void registerVersion(DBAccessManager dbAccessManager, String hostServiceType, String moduleName, String version, String buildDate) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = dbAccessManager.getConnection();
			if (hostServiceType.equalsIgnoreCase("Primary")) {
				pstmt = conn.prepareStatement(P_MERGE_VERSION_SQL);
				pstmt.setString(1, moduleName.toLowerCase());
				pstmt.setString(2, version);
				pstmt.setString(3, buildDate);
				pstmt.setString(4, moduleName.toLowerCase());
				pstmt.setString(5, version);
				pstmt.setString(6, buildDate);
			} else {
				pstmt = conn.prepareStatement(S_MERGE_VERSION_SQL);
				pstmt.setString(1, moduleName.toLowerCase());
				pstmt.setString(2, version);
				pstmt.setString(3, buildDate);
				pstmt.setString(4, moduleName.toLowerCase());
				pstmt.setString(5, version);
				pstmt.setString(6, buildDate);
			}

			pstmt.executeUpdate();
		} catch (SQLException se) {
				se.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (pstmt != null) {
					try {
						pstmt.close();
					} catch (Exception e) {
					}
					pstmt = null;
				}
			}
	}

	/**
	 * @param main
	 * @param dbAccessManager
	 * @param hostServiceType
	 */
	public static void historyVersion(DBAccessManager dbAccessManager, String moduleName, String hostServiceType, String version, String buildDate) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INSERT_VERSION_INFO_HISTORY_SQL);
			pstmt.setString(1, hostServiceType);
			pstmt.setString(2, moduleName.toLowerCase());
			pstmt.setString(3, version);
			pstmt.setString(4, buildDate);
			pstmt.executeUpdate();
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception e) {}
				pstmt = null;
			}
		}
	}
}
