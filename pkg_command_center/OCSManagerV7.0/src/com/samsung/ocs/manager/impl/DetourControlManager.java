package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_CONTROL_LEVEL;
import com.samsung.ocs.common.constant.OcsConstant.DETOUR_REASON;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.manager.impl.model.CarrierLoc;
import com.samsung.ocs.manager.impl.model.DetourControl;
import com.samsung.ocs.manager.impl.model.Hid;
import com.samsung.ocs.manager.impl.model.Link;
import com.samsung.ocs.manager.impl.model.Node;
import com.samsung.ocs.manager.impl.model.Section;
import com.samsung.ocs.manager.impl.model.VehicleData;

/**
 * DetourManager Class, OCS 3.1 for Unified FAB
 * 
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * 
 * @date   2015. 1. 23.
 * @version 3.1
 * 
 * Copyright 2012 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class DetourControlManager extends AbstractManager {
	private static DetourControlManager manager = null;
	private static final String DETOURID = "DETOURID";
	private static final String SECTIONID = "SECTIONID";
	private static final String ENABLED = "ENABLED";
	private static final String TRANSFERCANCELENABLED = "TRANSFERCANCELENABLED";
	private static final String TRANSFERABORTENABLED = "TRANSFERABORTENABLED";
	private static final String TRANSFERABORTTIMEOUT = "TRANSFERABORTTIMEOUT";
	private static final String PORTSERVICEENABLED = "PORTSERVICEENABLED";
	private static final String PENALTY = "PENALTY";
	private static final String TRUE = "TRUE";
	private static final String FALSE = "FALSE";
	private static final String DELIMITER = ",";
	
	private DetourControl vehicleManualDetour = null;
	private DetourControl vehicleErrorDetour = null;
	private DetourControl vehicleCommfailDetour = null;
	private DetourControl vehicleNotRespondDetour = null;
	private DetourControl nodeDisabledDetour = null;
	private DetourControl linkDisabledDetour = null;
	private DetourControl hidDownDetour = null;
	private DetourControl hidCapaFullDetour = null;
	private Vector<Section> updateSectionInfoList = new Vector<Section>();
	private Vector<String> updatePortServiceList = new Vector<String>();
	private Vector<String> userRequestList = new Vector<String>();
	private boolean requestUpdateSectionToDB = false;
	private boolean isDetourControlUsed = true;
	
	private DetourControlManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(DetourControl.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		init();
	}
	
	/**
	 * Constructor of DetourManager class. (Singleton)
	 */
	public static synchronized DetourControlManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			if (vOType == null) {
				return null;
			}
			manager = new DetourControlManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}

	@Override
	protected void init() {
		isDetourControlUsed = getDetourControlUsageParam();
		updateDetourControlFromDB();
		isInitialized = true;
	}

	@Override
	protected boolean updateFromDB() {
		// TODO Auto-generated method stub
		checkDetourUsed();
		updateDetourControlFromDB();
		updateUserRequestFromDB();
		return true;
	}

	@Override
	protected boolean updateToDB() {
		updateSectionToDB();
		updateChangedSectionToDB();
		updatePortServiceToDB();		
		return true;
	}
	
	private static final String SELECT_SQL = "SELECT * FROM DETOURCONTROL";
	/**
	 * 
	 * @return
	 */
	private boolean updateDetourControlFromDB() {
		boolean result = false;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String detourId = "";
		DetourControl detour = null;

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			
			ArrayList<String> removeDetour = new ArrayList<String>(data.keySet());
			while (rs.next()) {
				detour = (DetourControl) vOType.newInstance();
				data.put(detourId, detour);
				setDetour(detour, rs);
				
				checkDetourUsed(detour);
				removeDetour.remove(detourId);
			}
			
			for (String key : removeDetour) {
				data.remove(key);
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
		} catch (Throwable t) {
			result = false;
			t.printStackTrace();
			writeExceptionLog(LOGFILENAME, t);
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
	
	private static final String SELECT_SECTION_REQUESTED_SQL = "SELECT SECTIONID FROM SECTION WHERE ENABLED='FALSE' AND USERREQUEST = 'TRUE'";
	private boolean updateUserRequestFromDB() {
		boolean result = false;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SECTION_REQUESTED_SQL);
			rs = pstmt.executeQuery();
			
			while (rs.next()) {
				String sectionId = getString(rs.getString(SECTIONID));
				userRequestList.add(sectionId);
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
		} catch (Throwable t) {
			result = false;
			t.printStackTrace();
			writeExceptionLog(LOGFILENAME, t);
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
		
		if (userRequestList.size() > 0) {
			SectionManager sectionManager = SectionManager.getInstance(null, null, false, false, 0);
			if (sectionManager != null) {
				for (String sectionId : userRequestList) {
					Section section = (Section) sectionManager.getData().get(sectionId);
					if (section != null) {
						section.clearAllAbnormalItem();
					}
				}
			}
			userRequestList.clear();
		}
		
		return result;
	}
	
	private boolean getDetourControlUsageParam() {
		OCSInfoManager ocsInfoManager = OCSInfoManager.getInstance(null, null, false, false, 0);
		if (ocsInfoManager != null) {
			return ocsInfoManager.isDetourControlUsed();
		}
		return this.isDetourControlUsed;
	}
	
	private void checkDetourUsed() {
		// 전체 기능 On/Off 확인
		boolean isDetourControlUsed = getDetourControlUsageParam();
		if (this.isDetourControlUsed && isDetourControlUsed == false) {
			checkClearDetour(DETOUR_REASON.NONE);
		}
		this.isDetourControlUsed = isDetourControlUsed;
	}
	
	private void checkDetourUsed(DetourControl detour) {
		// 각 기능별 On/Off 확인
		switch (detour.getDetourReason()) {
			case NODE_DISABLED :
				if (nodeDisabledDetour != null && nodeDisabledDetour.isDetourUsed()) {
					if (detour.isDetourUsed() == false) {
						checkClearDetour(detour.getDetourReason());
					} else if(nodeDisabledDetour.isPortServiceUsed() && detour.isPortServiceUsed() == false) {
						checkClearDetourPortService(detour.getDetourReason());
					}
				}
				nodeDisabledDetour = detour;
				break;
			case LINK_DISABLED :
				if (linkDisabledDetour != null && linkDisabledDetour.isDetourUsed()) {
					if (detour.isDetourUsed() == false) {
						checkClearDetour(detour.getDetourReason());
					} else if(linkDisabledDetour.isPortServiceUsed() && detour.isPortServiceUsed() == false) {
						checkClearDetourPortService(detour.getDetourReason());
					}
				}
				linkDisabledDetour = detour;
				break;
			case VEHICLE_MANUAL:
				if (vehicleManualDetour != null && vehicleManualDetour.isDetourUsed()) {
					if (detour.isDetourUsed() == false) {
						checkClearDetour(detour.getDetourReason());
					} else if(vehicleManualDetour.isPortServiceUsed() && detour.isPortServiceUsed() == false) {
						checkClearDetourPortService(detour.getDetourReason());
					}
				}
				vehicleManualDetour = detour;
				break;
			case VEHICLE_ERROR:
				if (vehicleErrorDetour != null && vehicleErrorDetour.isDetourUsed()) {
					if (detour.isDetourUsed() == false) {
						checkClearDetour(detour.getDetourReason());
					} else if(vehicleErrorDetour.isPortServiceUsed() && detour.isPortServiceUsed() == false) {
						checkClearDetourPortService(detour.getDetourReason());
					}
				}
				vehicleErrorDetour = detour;
				break;
			case VEHICLE_COMMFAIL:
				if (vehicleCommfailDetour != null && vehicleCommfailDetour.isDetourUsed()) {
					if (detour.isDetourUsed() == false) {
						checkClearDetour(detour.getDetourReason());
					} else if(vehicleCommfailDetour.isPortServiceUsed() && detour.isPortServiceUsed() == false) {
						checkClearDetourPortService(detour.getDetourReason());
					}
				}
				vehicleCommfailDetour = detour;
				break;
			case VEHICLE_NOTRESPOND:
				if (vehicleNotRespondDetour != null && vehicleNotRespondDetour.isDetourUsed()) {
					if (detour.isDetourUsed() == false) {
						checkClearDetour(detour.getDetourReason());
					} else if(vehicleNotRespondDetour.isPortServiceUsed() && detour.isPortServiceUsed() == false) {
						checkClearDetourPortService(detour.getDetourReason());
					}
				}
				vehicleNotRespondDetour = detour;
				break;
			case HID_DOWN:
				if (hidDownDetour != null && hidDownDetour.isDetourUsed()) {
					if (detour.isDetourUsed() == false) {
						checkClearDetour(detour.getDetourReason());
					} else if(hidDownDetour.isPortServiceUsed() && detour.isPortServiceUsed() == false) {
						checkClearDetourPortService(detour.getDetourReason());
					}
				}
				hidDownDetour = detour;
				break;
			case HID_CAPACITY_FULL:
				if (hidCapaFullDetour != null && hidCapaFullDetour.isDetourUsed()) {
					if (detour.isDetourUsed() == false) {
						checkClearDetour(detour.getDetourReason());
					}
				}
				hidCapaFullDetour = detour;
				break;
			default:
		}
	}
	
	private void checkClearDetour(DETOUR_REASON reason) {
		SectionManager sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		if (sectionManager != null) {
			sectionManager.requestClearDetour(reason);
		}
	}
	
	private void checkClearDetourPortService(DETOUR_REASON reason) {
		SectionManager sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		if (sectionManager != null) {
			sectionManager.requestClearDetourPortService(reason);
		}
	}
	
	/**
	 * 2015.02.06 by MYM : 장애 지역 우회 기능
	 */
	private void updateSectionToDB() {
		if (requestUpdateSectionToDB) {
			// Step1. Link, Carrierloc Table에 SectionID 업데이트
			updateSectionIdToLinkTable();
			updateSectionIdToCarrierlocTable();
			
			// Step2. DetourSection Table 갱신(기존 삭제/신규 등록)
			deleteDetourSectionFromDB();
			insertDetourSectionToDB();
			
			// Step3. Node, Link, HID Abnormal 상태 반영
			checkChangedSectionEnabled();
			updateChangedSectionToDB();
			
			// Step4. abnormal 상태 반영 기준으로 정상이 된 경우 InService 보고
			//        ex) Carrierloc OutOfService(FALSE)된 상태에서 재시작, FailOver 중에 TRUE로 변경된 경우 기동 후 OutOfService(TRUE) 처리
			updateChangedSectionEnabledToDB();
			requestUpdateSectionToDB = false;
		}
	}
	
	private void checkChangedSectionEnabled() {
		NodeManager nodeManager = NodeManager.getInstance(null, null, false, false, 0);
		if (nodeManager != null) {
			nodeManager.checkChangedSectionEnabled();
		}
		
		HIDManager hidManager = HIDManager.getInstance(null, null, false, false, 0);
		if (hidManager != null) {
			hidManager.checkChangedSectionEnabled();
		}
		
		SectionManager sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		if (sectionManager != null) {
			sectionManager.checkChangedSectionEnabled();
		}
	}
	
	/**
	 * 2015.01.28 by MYM : 장애 지역 우회 기능
	 * @param section
	 */
	public void addUpdateSectionInfoList(Section section) {
		try {
			if (serviceState == MODULE_STATE.INSERVICE) {
				if (updateSectionInfoList != null && section != null) {
					updateSectionInfoList.add(section);
				}
			} else {
				StringBuffer log = new StringBuffer();
				log.append(serviceState).append(" ").append(section).append(" ").append(section.getEnabled());
				writeLog(LOGFILENAME, log.toString());
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	private static final String UPDATE_SECTION_ENABLED_SQL = "UPDATE SECTION SET ENABLED=?, DETOURPENALTY=?, DETOURREASON=?, USERREQUEST='' WHERE SECTIONID=?";
	private boolean updateChangedSectionToDB() {
		if (updateSectionInfoList == null || updateSectionInfoList.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_SECTION_ENABLED_SQL);
			int count = 0;
			int size = updateSectionInfoList.size();
			for (int i = 0; i < size; i++) {
				Section section = updateSectionInfoList.get(i);
				pstmt.setString(1, section.getEnabled() ? TRUE : FALSE);
				pstmt.setDouble(2, section.getDetourPenalty());
				pstmt.setString(3, section.getDetourReason());
				pstmt.setString(4, section.getSectionId());
				pstmt.addBatch();
				count++;
				
				if (count > 1000) {
					pstmt.executeBatch();
					count = 0;
				}
			}
			
			if (count > 0) {
				pstmt.executeBatch();
			}
			conn.commit();
			conn.setAutoCommit(true);
			for (int i = size - 1; i >= 0; i--) {
				updateSectionInfoList.remove(i);
			}
			result = true;
		} catch (ArrayIndexOutOfBoundsException e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (SQLException se) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {writeExceptionLog(LOGFILENAME, e);}
				pstmt = null;
			}
		}
		return result;
	}
	
	public void addDetourPortService(String sectionId, String requestedType) {
		try {
			if (serviceState == MODULE_STATE.INSERVICE) {
				if (updatePortServiceList != null) {
					updatePortServiceList.add((new StringBuffer(requestedType).append(DELIMITER).append(sectionId)).toString());
				}
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	// 2015.09.02 by MYM : batch Update → 개별 Update로 변경 (DB Deadlock 방지)
//	private static String UPDATE_PORTSERVICE_SQL = "UPDATE CARRIERLOC SET USERREQUEST=? WHERE SECTION=? AND TYPE<>'VEHICLEPORT'";
	private static String UPDATE_PORTINSERVICE_SQL = "UPDATE CARRIERLOC SET USERREQUEST='PortInService' WHERE SECTION=? AND ENABLED='FALSE' AND TYPE<>'VEHICLEPORT'";
	private static String UPDATE_PORTOUTOFSERVICE_SQL = "UPDATE CARRIERLOC SET USERREQUEST='PortOutOfService' WHERE SECTION=? AND ENABLED='TRUE' AND TYPE<>'VEHICLEPORT'";
	private boolean updatePortServiceToDB() {
		if (updatePortServiceList == null || updatePortServiceList.size() == 0) {
			return false;
		}
		
		Connection conn = null;
		// 2015.09.02 by MYM : batch Update → 개별 Update로 변경 (DB Deadlock 방지)
		PreparedStatement pstmt1 = null;
		PreparedStatement pstmt2 = null;
		boolean result = false;
		int size = updatePortServiceList.size();
		if (size > 0) {
			try {
				conn = dbAccessManager.getConnection();
				pstmt1 = conn.prepareStatement(UPDATE_PORTINSERVICE_SQL);
				pstmt2 = conn.prepareStatement(UPDATE_PORTOUTOFSERVICE_SQL);
				try {
					for (int i = 0; i < size; i++) {
						String [] values = updatePortServiceList.get(i).split(DELIMITER);
						if (values[0].equals(OcsConstant.PORT_INSERVICE)) {
							pstmt1.setString(1, values[1]);
							pstmt1.executeUpdate();
						} else {
							pstmt2.setString(1, values[1]);
							pstmt2.executeUpdate();
						}
					}
					result = true;
				} catch (ArrayIndexOutOfBoundsException ae) {
					result = false;
					writeExceptionLog(LOGFILENAME, ae);						
				} catch (PatternSyntaxException pe) {
					result = false;
					writeExceptionLog(LOGFILENAME, pe);
				} catch (SQLException se) {
					result = false;
					try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
					writeExceptionLog(LOGFILENAME, se);
				} catch (Exception e) {
					result = false;
					writeExceptionLog(LOGFILENAME, e);
				} finally {
					if (pstmt1 != null) {
						try { pstmt1.close(); } catch (Exception e) {}
						pstmt1 = null;
					}
					if (pstmt2 != null) {
						try { pstmt2.close(); } catch (Exception e) {}
						pstmt2 = null;
					}
				}
			} catch (SQLException e) {
				writeExceptionLog(LOGFILENAME, e);
			}
			
			for (int i = size - 1; i >= 0; i--) {
				updatePortServiceList.remove(i);
			}
		}
		
		return result;
		
	}
	
	private static final String UPDATE_SECTIONID_SQL = "UPDATE LINK SET SECTION=? WHERE LINKID=?";
	private boolean updateSectionIdToLinkTable() {
		SectionManager sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		if (sectionManager == null) {
			return false;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_SECTIONID_SQL);
			int count = 0;
			for (Enumeration<Object> e = sectionManager.getData().elements(); e.hasMoreElements();) {
				Section section = (Section) e.nextElement();
				for (Link link : section.getLinkMap().values()) {
					pstmt.setString(1, section.getSectionId());
					pstmt.setString(2, link.getLinkId());
					pstmt.addBatch();
					count++;
					
					if (count > 1000) {
						pstmt.executeBatch();
						count = 0;
					}
				}
			}
			if (count > 0) {
				pstmt.executeBatch();
			}
			conn.commit();
			conn.setAutoCommit(true);
			result = true;
		} catch (ArrayIndexOutOfBoundsException e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (SQLException se) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {writeExceptionLog(LOGFILENAME, e);}
				pstmt = null;
			}
		}
		return result;
	}
	private static String UPDATE_SECTION_SQL = "UPDATE CARRIERLOC SET SECTION=? WHERE CARRIERLOCID=?";
	private boolean updateSectionIdToCarrierlocTable() {
		CarrierLocManager carrierlocManager = CarrierLocManager.getInstance(null, null, false, false, 0);
		NodeManager nodeManager = NodeManager.getInstance(Node.class, null, false, false, 0); 
		if (carrierlocManager == null || nodeManager == null) {
			return false;
		}
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_SECTION_SQL);
			int count = 0;
			
			for (Enumeration<Object> e = carrierlocManager.getData().elements(); e.hasMoreElements();) {
				CarrierLoc carrierloc = (CarrierLoc) e.nextElement();
				if (carrierloc == null) {
					continue;
				}
				Node node = nodeManager.getNode(carrierloc.getNode());
				if (node == null) {
					continue;
				}
				String sectionId = "";
				if (node.isDiverge()) {
					for (Section section : node.getSection()) {
						if (section.getLastNode() == node) {
							sectionId = section.getSectionId(); 
							break;
						}
					}
				} else if(node.isConverge()){
					for (Section section : node.getSection()) {
						if (section.getFirstNode() == node) {
							sectionId = section.getSectionId(); 
							break;
						}
					}
				} else {
					Section section = node.getSection().get(0);
					sectionId = section.getSectionId();
				}
				pstmt.setString(1, sectionId);
				pstmt.setString(2, carrierloc.getCarrierLocId());
				pstmt.addBatch();
				count++;
				
				if (count > 1000) {
					pstmt.executeBatch();
					count = 0;
				}
			}
			
			if (count > 0) {
				pstmt.executeBatch();
			}
			conn.commit();
			conn.setAutoCommit(true);
			result = true;
		} catch (SQLException se) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
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
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	private static final String DELETE_SECTION_SQL = "DELETE FROM SECTION";
	private boolean deleteDetourSectionFromDB() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return result;
			}
			pstmt = conn.prepareStatement(DELETE_SECTION_SQL);
			pstmt.execute();
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
				} catch (Exception e) {writeExceptionLog(LOGFILENAME, e);}
				pstmt = null;
			}
		}
		return result;
	}
	
	private static final String INSERT_SECTION_SQL = "INSERT INTO SECTION(SECTIONID, ENABLED, PREVIOUSSECTION, NODELIST) VALUES(?,?,?,?)";
	private boolean insertDetourSectionToDB() {
		SectionManager sectionManager = SectionManager.getInstance(null, null, false, false, 0);
		if (sectionManager == null) {
			return false;
		}
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(INSERT_SECTION_SQL);
			int count = 0;
			for (Enumeration<Object> e = sectionManager.getData().elements(); e.hasMoreElements();) {
				Section section = (Section) e.nextElement();
				pstmt.setString(1, section.getSectionId());
				pstmt.setString(2, section.getEnabled() ? TRUE : FALSE);
				HashSet<Section> prevSectionSet = section.getDetourSectionSet();
				String prevSectionIdList = "";
				if (prevSectionSet != null && prevSectionSet.size() > 0) {
					prevSectionIdList = prevSectionSet.toString().replace("[", "").replace("]", "").replaceAll("\\p{Z}", "");
				}
				pstmt.setString(3, prevSectionIdList);
				ArrayList<Node> nodeList = section.getNodeList();
				String nodeIdList = "";
				if (nodeList != null && nodeList.size() > 0) {
					nodeIdList = nodeList.toString().replace("[", "").replace("]", "").replaceAll("\\p{Z}", "");
				}				
				pstmt.setString(4, nodeIdList);
				pstmt.addBatch();
				count++;
				
				if (count > 1000) {
					pstmt.executeBatch();
					count = 0;
				}
			}
			if (count > 0) {
				pstmt.executeBatch();
			}
			conn.commit();
			conn.setAutoCommit(true);
			result = true;
		} catch (ArrayIndexOutOfBoundsException e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} catch (SQLException se) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			try { conn.rollback(); } catch (Exception ignore) {writeExceptionLog(LOGFILENAME, ignore);}
			result = false;
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		}
		finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {writeExceptionLog(LOGFILENAME, e);}
				pstmt = null;
			}
		}
		return result;
	}
	
	// 2015.03.19 by MYM : PortOutOfService 중 장애 Clear시 PortInService 처리 되도록 보완 
//	private static final String UPDATE_CHANGED_SECTION_SQL = "UPDATE CARRIERLOC SET USERREQUEST='PortInService' WHERE SECTION IN (SELECT DISTINCT SECTION FROM CARRIERLOC A, SECTION B WHERE A.ENABLED='FALSE' AND B.ENABLED='TRUE' AND A.SECTION=B.SECTIONID)";
	private static final String UPDATE_CHANGED_SECTION_SQL = "UPDATE CARRIERLOC SET USERREQUEST='PortInService' WHERE SECTION IN (SELECT DISTINCT SECTION FROM CARRIERLOC A, SECTION B WHERE (((A.ENABLED='FALSE' AND B.ENABLED='TRUE') OR (A.USERREQUEST='PortOutOfService' AND A.ENABLED='TRUE' AND B.ENABLED='TRUE')) AND A.SECTION=B.SECTIONID))";
	private boolean updateChangedSectionEnabledToDB() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			if (conn == null) {
				return result;
			}
			pstmt = conn.prepareStatement(UPDATE_CHANGED_SECTION_SQL);
			pstmt.execute();
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
				} catch (Exception e) {writeExceptionLog(LOGFILENAME, e);}
				pstmt = null;
			}
		}
		return result;
	}
	
	/**
	 * 
	 * @param detour
	 * @param rs
	 * @throws SQLException
	 */
	private void setDetour(DetourControl detour, ResultSet rs) throws SQLException {
		detour.setDetourId(getString(rs.getString(DETOURID)));
		detour.setDetourUsed(getString(rs.getString(ENABLED)).equals(TRUE));
		detour.setTransferCancelUsed(getString(rs.getString(TRANSFERCANCELENABLED)).equals(TRUE));
		detour.setTransferAbortUsed(getString(rs.getString(TRANSFERABORTENABLED)).equals(TRUE));
		detour.setTransferAbortTimeout(rs.getInt(TRANSFERABORTTIMEOUT));
		detour.setPortServiceUsed(getString(rs.getString(PORTSERVICEENABLED)).equals(TRUE));
		detour.setPenalty(rs.getDouble(PENALTY));
	}
	
	/**
	 * 
	 * @param reason
	 * @return
	 */
	public boolean isDetourUsed(Object item, DETOUR_REASON reason) {
		switch (reason) {
			case NONE :
				return false;
			case NODE_DISABLED :
				return nodeDisabledDetour != null ? nodeDisabledDetour.isDetourUsed() : false;
			case LINK_DISABLED :
				return linkDisabledDetour != null ? linkDisabledDetour.isDetourUsed() : false;
			case VEHICLE_MANUAL:
				return vehicleManualDetour != null ? vehicleManualDetour.isDetourUsed() : false;
			case VEHICLE_ERROR:
				if (vehicleErrorDetour != null && vehicleErrorDetour.isDetourUsed()) {
					if (item instanceof VehicleData) {
						VehicleData vehicle = (VehicleData) item;
						VehicleErrorManager vehicleErrorManager = VehicleErrorManager.getInstance(null, null, false, false, 0);
						// 2015.11.10 by MYM : 장애 발생시의 ErrorCode를 가져와서 체크하도록 변경
						// 배경 : 이미 ErrorCode가 0으로 Reset되어 getErrorCode()로는 체크 불가 
						int errorCode = vehicle.getDetourErrorCode();
						if (errorCode != 0 && vehicleErrorManager != null) {
							DETOUR_CONTROL_LEVEL level = vehicleErrorManager.getDetourControlLevel(errorCode);
							if (level == DETOUR_CONTROL_LEVEL.LEVEL1 || level == DETOUR_CONTROL_LEVEL.LEVEL2) {
								return true;
							}
						}
					}
				}
				return false;
			case VEHICLE_COMMFAIL:
				return vehicleCommfailDetour != null ? vehicleCommfailDetour.isDetourUsed() : false;
			case VEHICLE_NOTRESPOND:
				return vehicleNotRespondDetour != null ? vehicleNotRespondDetour.isDetourUsed() : false;
			case HID_DOWN:
				if (hidDownDetour != null && hidDownDetour.isDetourUsed()) {
					if (item instanceof Hid) {
						Hid hid = (Hid) item;
						DETOUR_CONTROL_LEVEL level = hid.getDetourControlLevel();
						if (level == DETOUR_CONTROL_LEVEL.LEVEL1 || level == DETOUR_CONTROL_LEVEL.LEVEL2) {
							return true;
						}
					}
				}
				return false;
			case HID_CAPACITY_FULL:
				return hidCapaFullDetour != null ? hidCapaFullDetour.isDetourUsed() : false;
			default:
				return false;
		}
	}
	
	/**
	 * 
	 * @param item
	 * @return
	 */
	public boolean isPortServiceUsed(Object item, DETOUR_REASON reason) {
		switch (reason) {
			case NONE :
				return false;
			case NODE_DISABLED :
				return nodeDisabledDetour != null ? nodeDisabledDetour.isPortServiceUsed(): false;
			case LINK_DISABLED :
				return linkDisabledDetour != null ? linkDisabledDetour.isPortServiceUsed(): false;
			case VEHICLE_MANUAL:
				return vehicleManualDetour != null ? vehicleManualDetour.isPortServiceUsed() : false;
			case VEHICLE_ERROR:
				if (vehicleErrorDetour != null && vehicleErrorDetour.isPortServiceUsed()) {
					if (item instanceof VehicleData) {
						VehicleData vehicle = (VehicleData) item;
						VehicleErrorManager vehicleErrorManager = VehicleErrorManager.getInstance(null, null, false, false, 0);
						// 2015.11.10 by MYM : 장애 발생시의 ErrorCode를 가져와서 체크하도록 변경
						// 배경 : 이미 ErrorCode가 0으로 Reset되어 getErrorCode()로는 체크 불가
						int errorCode = vehicle.getDetourErrorCode();
						if (errorCode != 0 && vehicleErrorManager != null) {
							DETOUR_CONTROL_LEVEL level = vehicleErrorManager.getDetourControlLevel(errorCode);
							if (level == DETOUR_CONTROL_LEVEL.LEVEL2) {
								return true;
							}
						}
					}
				}
				return false;
			case VEHICLE_COMMFAIL:
				return vehicleCommfailDetour != null ? vehicleCommfailDetour.isPortServiceUsed() : false;
			case VEHICLE_NOTRESPOND:
				return vehicleNotRespondDetour != null ? vehicleNotRespondDetour.isPortServiceUsed() : false;
			case HID_DOWN:
				if (hidDownDetour != null && hidDownDetour.isDetourUsed()) {
					if (item instanceof Hid) {
						Hid hid = (Hid) item;
						DETOUR_CONTROL_LEVEL level = hid.getDetourControlLevel();
						if (level == DETOUR_CONTROL_LEVEL.LEVEL2) {
							return true;
						}
					}
				}
				return false;
			default:
				return false;
		}
	}
	
	public double getDetourPenalty(DETOUR_REASON reason) {
		switch (reason) {
		case NODE_DISABLED :
			return nodeDisabledDetour != null ? nodeDisabledDetour.getPenalty() : 0;
		case LINK_DISABLED :
			return linkDisabledDetour != null ? linkDisabledDetour.getPenalty() : 0;
		case VEHICLE_MANUAL:
			return vehicleManualDetour != null ? vehicleManualDetour.getPenalty() : 0;
		case VEHICLE_ERROR:
			return vehicleErrorDetour != null ? vehicleErrorDetour.getPenalty() : 0;
		case VEHICLE_COMMFAIL:
			return vehicleCommfailDetour != null ? vehicleCommfailDetour.getPenalty() : 0;
		case VEHICLE_NOTRESPOND:
			return vehicleNotRespondDetour != null ? vehicleNotRespondDetour.getPenalty() : 0;
		case HID_DOWN:
			return hidDownDetour != null ? hidDownDetour.getPenalty() : 0;
		case HID_CAPACITY_FULL:
			return hidCapaFullDetour != null ? hidCapaFullDetour.getPenalty() : 0;
		default:
			return 0;
		}
	}
	
	public DetourControl getVehicleManualDetour() {
		return vehicleManualDetour;
	}
	
	public DetourControl getVehicleErrorDetour() {
		return vehicleErrorDetour;
	}
	
	public DetourControl getVehicleCommfailDetour() {
		return vehicleCommfailDetour;
	}
	
	public DetourControl getVehicleNotRespondDetour() {
		return vehicleNotRespondDetour;
	}

	public DetourControl getNodeDisabledDetour() {
		return nodeDisabledDetour;
	}
	
	public DetourControl getLinkDisabledDetour() {
		return linkDisabledDetour;
	}

	public DetourControl getHidDownDetour() {
		return hidDownDetour;
	}
	
	public void requestUpdateSectionToDB() {
		this.requestUpdateSectionToDB = true;
	}
}
