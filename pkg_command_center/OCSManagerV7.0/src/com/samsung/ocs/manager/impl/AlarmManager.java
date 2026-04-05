package com.samsung.ocs.manager.impl;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsAlarmConstant.ALARMLEVEL;
import com.samsung.ocs.manager.impl.model.Alarm;

/**
 * AlarmManager Class, OCS 3.0 for Unified FAB
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

public class AlarmManager extends AbstractManager {
	private static AlarmManager manager = null;
	private static final String ALARMID = "ALARMID";
	private static final String ALARMTEXT = "ALARMTEXT";
	private static final String VEHICLE = "VEHICLE";
	private static final String ALARMTIME = "ALARMTIME";
	private ConcurrentHashMap<String, Alarm> insertAlarmList = null;
	private ConcurrentHashMap<String, Integer> alarmRegisteredList = null;
	// 2020.08.25 by JJW : Byte 포함 문자열 자르기 함수
	private static final int BYTE_LENGTH = 160; // 문자열 길이
	private static final int BYTE_INDEX = 3; // 한글 문자열 사이즈 Java -> Oracle (UTF-8 방식은 3Byte)

	/**
	 * Constructor of AlarmManager class.
	 */
	private AlarmManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		if (vOType != null && vOType.getClass().isInstance(Alarm.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		
		// 2012.03.16 by PMM
		// 초기화 위치 변경.
		insertAlarmList = new ConcurrentHashMap<String, Alarm>();
		alarmRegisteredList = new ConcurrentHashMap<String, Integer>();
	}
	
	/**
	 * Constructor of AlarmManager class. (Singleton)
	 */
	public static synchronized AlarmManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new AlarmManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		clearAlarm();
		updateFromDB();
		isInitialized = true;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		registerAlarm();
		return true;
	}

	private static final String SELECT_SQL = "SELECT * FROM ALARM";
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateFromDB()
	 */
	
	/**
	 * 
	 */
	@Override
	protected boolean updateFromDB() {
		boolean result = false;
		if (isInitialized) {
			if (alarmRegisteredList != null) {
				Connection conn = null;
				ResultSet rs = null;
				PreparedStatement pstmt = null;
				Set<String> removeKeys = new HashSet<String>(data.keySet());
				String key = null;
				String vehicleId = null;
				String alarmId = null;
				int alarmCode = 0;
				
				// 2012.03.19 by PMM
				// alarmRegisteredList 관리 개선
				Set<String> removeAlarmRegisteredKeys = null;

				try {
					removeAlarmRegisteredKeys = new HashSet<String>(alarmRegisteredList.keySet());
					conn = dbAccessManager.getConnection();
					pstmt = conn.prepareStatement(SELECT_SQL);
					rs = pstmt.executeQuery();
					while (rs.next()) {
						vehicleId = rs.getString(VEHICLE);
						alarmId = rs.getString(ALARMID);
						
						key = vehicleId + alarmId;
						if (key == null || (key != null && key.length() == 0)) {
							continue;
						}

						Alarm alarm = (Alarm) data.get(key);
						if (alarm == null) {
							alarm = (Alarm) vOType.newInstance();
							data.put(key, alarm);
						}
						setAlarm(alarm, rs);
						removeKeys.remove(key);
						
						if (alarmId == null || (alarmId != null && alarmId.length() == 0)) {
							alarmCode = 0;
						} else {
							alarmCode = rs.getInt(ALARMID);
						}
						
						if (vehicleId != null) {
							alarmRegisteredList.put(vehicleId, alarmCode);
							if (alarmRegisteredList.containsKey(vehicleId)) {
								removeAlarmRegisteredKeys.remove(vehicleId);
							}
						}
					}
					for (String rmKey : removeKeys) {
						data.remove(rmKey);
					}
					
					for (String rmKey : removeAlarmRegisteredKeys) {
						alarmRegisteredList.remove(rmKey);
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
			}
		}
		return result;
	}

	/**
	 * 
	 * @param alarm
	 * @param rs
	 * @exception SQLException
	 */
	private void setAlarm(Alarm alarm, ResultSet rs) throws SQLException {
		alarm.setAlarmId(getString(rs.getString(ALARMID)));
		alarm.setAlarmText(getString(rs.getString(ALARMTEXT)));
		alarm.setVehicle(getString(rs.getString(VEHICLE)));
		alarm.setAlarmTime(getString(rs.getString(ALARMTIME)));
	}

	/**
	 * 
	 * @param vehicleId
	 * @param alarmCode
	 * @param alarmText
	 */
	public void registerAlarm(String vehicleId, int alarmCode, String alarmText, ALARMLEVEL alarmLevel) {
		// 2012.03.20 by PMM
		if (isInitialized) {
			if (insertAlarmList != null) {
				alarmText = subStringBytes(alarmText, BYTE_LENGTH, BYTE_INDEX);
				insertAlarmList.putIfAbsent(vehicleId + alarmText, new Alarm(vehicleId, getAlarmId(alarmCode), alarmText, alarmLevel));
			}
		}
	}
	
//	private static final String insertSql = "MERGE INTO ALARM DATA USING DUAL ON (DATA.VEHICLE=? AND DATA.ALARMID=? AND DATA.ALARMTEXT=?) WHEN NOT MATCHED THEN INSERT (VEHICLE, ALARMID, ALARMTEXT, ALARMTIME) VALUES (?,?,?,TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3'))";
	private static final String INSERT_SQL = "MERGE INTO ALARM DATA USING DUAL ON (DATA.VEHICLE=? AND DATA.ALARMID=?) " +
												" WHEN MATCHED THEN UPDATE SET ALARMTEXT=?, ALARMTIME=TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3') " +
												" WHEN NOT MATCHED THEN INSERT (VEHICLE, ALARMID, ALARMTEXT, ALARMTIME, ALARMLEVEL) VALUES (?,?,?,TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3'),?)";
	/**
	 * 
	 * @return
	 */
	private boolean registerAlarm() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
			
		// 2012.03.20 by PMM
		if (isInitialized) {
			if (insertAlarmList != null && insertAlarmList.size() > 0) {
				try {
					conn = dbAccessManager.getConnection();
					Set<String> dataKeys = new HashSet<String>(insertAlarmList.keySet());
					Vector<String> insertedAlarm = new Vector<String>();
					Iterator<String> iterator = dataKeys.iterator();
					String key;
					Alarm alarm = null;
					pstmt = conn.prepareStatement(INSERT_SQL);
					while (iterator.hasNext()) {
						key = iterator.next();
						if (key != null) {
							alarm = insertAlarmList.get(key);
							if (alarm != null) {
								pstmt.setString(1, alarm.getVehicle());
								pstmt.setString(2, alarm.getAlarmId());
								pstmt.setString(3, alarm.getAlarmText());
								pstmt.setString(4, alarm.getVehicle());
								pstmt.setString(5, alarm.getAlarmId());
								pstmt.setString(6, alarm.getAlarmText());
								pstmt.setString(7, alarm.getAlarmLevel().toConstString());
								pstmt.executeUpdate();
							}
							insertedAlarm.add(key);
						}
					}
					for (String rmKey: insertedAlarm) {
						insertAlarmList.remove(rmKey);
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
			}
		}
		return result;
	}
	
	// 2011.11.04 by PMM
	private static final String INSERT_TEXT_SQL = "MERGE INTO ALARM DATA USING DUAL ON (DATA.VEHICLE=? AND DATA.ALARMTEXT=?) " +
	" WHEN MATCHED THEN UPDATE SET ALARMTIME=TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3') " +
	" WHEN NOT MATCHED THEN INSERT (VEHICLE, ALARMID, ALARMTEXT, ALARMTIME) VALUES (?,'',?,TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3'))";
	/**
	 * 
	 * @param vehicleId
	 * @param alarmText
	 * @return
	 */
	public boolean registerAlarmText(String vehicleId, String alarmText) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INSERT_TEXT_SQL);
			/*if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}*/
			alarmText = subStringBytes(alarmText, BYTE_LENGTH, BYTE_INDEX);
			pstmt.setString(1, vehicleId);
			pstmt.setString(2, alarmText);
			pstmt.setString(3, vehicleId);
			pstmt.setString(4, alarmText);
			pstmt.executeUpdate();
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
	
	private static final String INSERT_TEXT_WITH_TYPE_SQL = "MERGE INTO ALARM DATA USING DUAL ON (DATA.VEHICLE=? AND DATA.ALARMTEXT=?) " +
	" WHEN MATCHED THEN UPDATE SET ALARMTIME=TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3') " +
	" WHEN NOT MATCHED THEN INSERT (VEHICLE, ALARMID, ALARMTEXT, ALARMTIME, ALARMLEVEL) VALUES (?,'',?,TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF3'),?)";
	/**
	 * 
	 * @param vehicleId
	 * @param alarmText
	 * @param alarmLevel
	 * @return
	 */
	public boolean registerAlarmTextWithLevel(String vehicleId, String alarmText, String alarmLevel) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(INSERT_TEXT_WITH_TYPE_SQL);
			/*if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}*/
			alarmText = subStringBytes(alarmText, BYTE_LENGTH, BYTE_INDEX);
			pstmt.setString(1, vehicleId);
			pstmt.setString(2, alarmText);
			pstmt.setString(3, vehicleId);
			pstmt.setString(4, alarmText);
			pstmt.setString(5, alarmLevel);
			pstmt.executeUpdate();
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
	
	private static final String DELETE_ALARMTEXT_SQL = "DELETE FROM ALARM WHERE VEHICLE=? AND ALARMTEXT=?";
	/**
	 * 
	 * @param vehicleId
	 * @param alarmText
	 * @return
	 */
	public boolean unregisterAlarmText(String vehicleId, String alarmText) {
		// 2012.03.20 by PMM
//		if (insertAlarmList.size() > 0) {
		if (isInitialized) {
			registerAlarm();
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(DELETE_ALARMTEXT_SQL);
			/*if (alarmText.length() > 160) {
				alarmText = alarmText.substring(0, 160);
			}*/
			alarmText = subStringBytes(alarmText, BYTE_LENGTH, BYTE_INDEX);
			pstmt.setString(1, vehicleId);
			pstmt.setString(2, alarmText);
			pstmt.executeUpdate();
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
	
	private static final String DELETE_ALARM_SQL = "DELETE FROM ALARM WHERE VEHICLE=? AND ALARMID=?";
	/**
	 * 
	 * @param vehicleId
	 * @param alarmCode
	 * @return
	 */
	public boolean unregisterAlarm(String vehicleId, int alarmCode) {
		// 2012.03.20 by PMM
//		if (insertAlarmList.size() > 0) {
		if (isInitialized) {
			registerAlarm();
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(DELETE_ALARM_SQL);
			pstmt.setString(1, vehicleId);
			pstmt.setString(2, getAlarmId(alarmCode));
			result = pstmt.executeUpdate() > 0;
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
				} catch (Exception e) {
				}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		} else {
			if (isInitialized) {
				alarmRegisteredList.remove(vehicleId);
			}
		}
		
		return result;
	}

	private static final String DELETE_SQL = "DELETE FROM ALARM WHERE VEHICLE=?";
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public boolean unregisterAllAlarm(String vehicleId) {
		// 2012.03.20 by PMM
//		if (insertAlarmList.size() > 0) {
		if (isInitialized) {
			registerAlarm();
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(DELETE_SQL);
			pstmt.setString(1, vehicleId);
			pstmt.executeUpdate();
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
				} catch (Exception e) {
				}
				pstmt = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		} else {
			if (isInitialized) {
				if (alarmRegisteredList != null) {
					alarmRegisteredList.remove(vehicleId);
				}
			}
		}
		return result;
	}

	private static final String DELETE_ALL_SQL = "DELETE FROM ALARM PURGE";
	/**
	 * 초기 생성시 Init에서 한번 수행됨
	 * 
	 * @return
	 */
	private boolean clearAlarm() {
		boolean result = false;
		Statement statement = null;
		try {
			Connection conn = dbAccessManager.getConnection();
			StringBuffer sql = new StringBuffer();
			sql.append(DELETE_ALL_SQL);
			statement = conn.createStatement();
			statement.execute(sql.toString());
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
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) {
				}
				statement = null;
			}
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		} else {
			if (isInitialized) {
				if (alarmRegisteredList != null) {
					alarmRegisteredList.clear();
				}
			}
		}
		return result;
	}
	
	private String getAlarmId(int alarmCode) {
		if (alarmCode <= 0) {
			return "";
		} else {
			return String.valueOf(alarmCode);
		}
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public boolean isAlarmRegistered(String vehicleId) {
		if (vehicleId != null && isInitialized) {
			if (alarmRegisteredList != null && alarmRegisteredList.containsKey(vehicleId)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public int getRegisteredAlarmCode(String vehicleId) {
		if (vehicleId != null && isInitialized) {
			if (alarmRegisteredList != null) {
				Integer value = alarmRegisteredList.get(vehicleId);
				if (value != null) {
					return value.intValue();
				}
			}
		}
		return 0;
	}
	
	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}
	
	// 2020.08.25 by JJW : Byte 포함 문자열 자르기 함수
	public static String subStringBytes(String str, int byteLength,	int byteIndex) {
		int retLength = 0;
		int tempSize = 0;
		int asc;
		
		if (str.getBytes().length > byteLength) {
			if (str == null || "".equals(str) || "null".equals(str)) {
				str = "";
			}
			int length = str.length();

			for (int i = 1; i <= length; i++) {
				asc = (int) str.charAt(i - 1);
				if (asc > 127) {
					if (byteLength >= tempSize + byteIndex) {
						tempSize += byteIndex;
						retLength++;
					}
				} else {
					if (byteLength > tempSize) {
						tempSize++;
						retLength++;
					}
				}
			}
			return str.substring(0, retLength);
		} else {
			return str;
		}
	}
	
}
