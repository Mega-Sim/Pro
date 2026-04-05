package com.samsung.ocs.manager.impl;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.constant.TrCmdConstant.REQUESTEDTYPE;
import com.samsung.ocs.manager.impl.model.Vehicle;
import com.samsung.ocs.manager.index.MaterialTable;
import com.samsung.ocs.manager.index.ZoneTable;

/**
 * VehicleManager Class, OCS 3.0 for Unified FAB
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

public class VehicleManager extends AbstractManager {
	private static VehicleManager manager = null;
	private static final String VEHICLEID = "VEHICLEID";
	private static final String VEHICLEMODE = "VEHICLEMODE";
	private static final String STATE = "STATUS";
//	private static final String CARRIEREXIST = "CARRIEREXIST";
	private static final String CURRNODE = "CURRNODE";
	private static final String STOPNODE = "STOPNODE";
	private static final String TARGETNODE = "TARGETNODE";
	private static final String ERRORCODE = "ERRORCODE";
	private static final String ASSIGNHOLD = "JOBPAUSE";
	private static final String ACTIONHOLD = "ACTIONPAUSE";
	private static final String LOADINGBYPASS = "LOADINGBYPASS";
	private static final String ENABLED = "ENABLED";
	private static final String IPADDRESS = "IPADDRESS";
	private static final String REQUESTEDTYPE_ = "REQUESTEDTYPE";
	private static final String REQUESTEDDATA = "REQUESTEDDATA";
	private static final String REQUESTEDCOST = "REQUESTEDCOST";
	private static final String MATERIAL = "MATERIAL";
	private static final String ZONE = "ZONE";
//	private static final String LOCUS = "LOCUS";
	private static final String SEMIMANUAL = "SEMIMANUAL";
	private static final String REASON = "REASON";
	private static final String LOCALGROUPID = "LOCALGROUPID";
	private static final String VEHICLESPEED = "VEHICLESPEED";
	private static final String MAPVERSION = "MAPVERSION";
	
	private static final String CURVHL = "CURVHL";
	
	private static final String TRUE = "TRUE";
	private static final String FALSE = "FALSE";

	// 2013.02.15 by KYK
	private static final String CURRNODEOFFSET = "CURRNODEOFFSET";
	private static final String CURRSTATION = "CURRSTATION";
	private static final String STOPSTATION = "STOPSTATION";
	private static final String TARGETSTATION = "TARGETSTATION";
	// 2013.09.06 by KYK
	private static final String CARRIERTYPE = "CARRIERTYPE";
	
	private Vector<Vehicle> update = null;
	private Vector<Vehicle> locusUpdate = null; // 2015.08.13 by KYK
	private Vector<String> register = null;
	private HashMap<String, String> registerLocalGroupOfVehicle = null;
	private Vector<String> releaseLocalGroupOfVehicle = null;
	protected HashMap<String, Integer> localGroupCurrVHL = null;
	private boolean isAllLocalGroupInfoCleared = false;
	
	private DBAccessManager dbAccessManagerForBatch = null;
	private MaterialTable materialTable = null;
	private ZoneTable zoneTable = null;
	
	/**
	 * Constructor of VehicleManager class.
	 */
	private VehicleManager(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();
		
		dbAccessManagerForBatch = new DBAccessManager();
		update = new Vector<Vehicle>();
		locusUpdate = new Vector<Vehicle>();
		register = new Vector<String>();
		registerLocalGroupOfVehicle = new HashMap<String, String>();
		releaseLocalGroupOfVehicle = new Vector<String>();
		localGroupCurrVHL = new HashMap<String, Integer>();
		
		materialTable = MaterialTable.getInstance();
		zoneTable = ZoneTable.getInstance();
		
		if (vOType != null && vOType.getClass().isInstance(Vehicle.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
	}
	
	/**
	 * Constructor of VehicleManager class. (Singleton)
	 */
	public static synchronized VehicleManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new VehicleManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		}
		return manager;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#init()
	 */
	@Override
	protected void init() {
		materialTable = MaterialTable.getInstance();
		zoneTable = ZoneTable.getInstance();
		updateFromDB();
		isInitialized = true;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.manager.impl.AbstractManager#updateToDB()
	 */
	@Override
	protected boolean updateToDB() {
		updateVehicleInfoToDB();
		updateVehicleLocusToDB(); // 2015.08.13 by KYK
		return true;
	}
	
	private static final String SELECT_SQL = "SELECT * FROM VEHICLE ORDER BY VEHICLEID";
	@Override
	protected boolean updateFromDB() {
		MODULE_STATE serviceState = this.serviceState;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		String vehicleId;
		Vehicle vehicle;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				vehicleId = rs.getString(VEHICLEID);
				vehicle = (Vehicle)data.get(vehicleId);
				if (vehicle == null) {
					vehicle = (Vehicle) vOType.newInstance();
					vehicle.setVehicleId(vehicleId);
					setVehicle(vehicle, rs);
					data.put(vehicleId, vehicle);
				} else {
					if (isInitialized) {
						// 2015.04.09 by MYM : OutOfService → InService로 전환될 때만 DB의 전체 정보를 가져옴. 
						if (serviceState == MODULE_STATE.REQINSERVICE) {
							updateVehicle2(vehicle, rs);
						} else {
							updateVehicle(vehicle, rs);
						}
					} else {
						setVehicle(vehicle, rs);
					}
				}
				removeKeys.remove(vehicleId);
			}
			for (String rmKey : removeKeys) {
				data.remove(rmKey);
			}
			result = true;
		} catch (SQLException se) {
			result = false;
			se.printStackTrace();
			writeExceptionLog(LOGFILENAME, se);
		} catch (IllegalAccessException iae) {
			result = false;
			iae.printStackTrace();
			writeExceptionLog(LOGFILENAME, iae);
		} catch (InstantiationException ie) {
			result = false;
			ie.printStackTrace();
			writeExceptionLog(LOGFILENAME, ie);
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
	
	/**
	 * Request to RuntimeUpdate
	 */
	public void initializeFromDB(boolean clear) {
		isInitialized = false;
		
		// 2011.11.04 by PMM
		// OperationManager에서 VehicleManager는 data.clear()를 하지 않는다는 것을
		// 구분하기 위해 clear 인자를 추가함.
//		if (clear) {
//			data.clear();
//		}
		init();
	}
	
	// 2014.03.15 by KYK
//	private static final String UPDATE_SQL = "UPDATE VEHICLE SET VEHICLEMODE=?, REQUESTEDTYPE=?, REQUESTEDDATA=?, REQUESTEDCOST=?, STATUS=?, CARRIEREXIST=?, CURRNODE=?, STOPNODE=?, TARGETNODE=?, ERRORCODE=?, REASON=?, VEHICLESPEED=?, MAPVERSION=?, APSIGNAL=?, APMACADDRESS=? WHERE VEHICLEID=?";
	private static final String UPDATE_SQL = "UPDATE VEHICLE SET VEHICLEMODE=?, STATUS=?, CARRIEREXIST=?, CURRNODE=?, STOPNODE=?, TARGETNODE=?, ERRORCODE=?, REASON=?, VEHICLESPEED=?, MAPVERSION=?, APSIGNAL=?, APMACADDRESS=?," +
			" CURRNODEOFFSET=?, CURRSTATION=?, STOPSTATION=?, TARGETSTATION=?, PAUSESTATUS=?, STEERPOSITION=?, ORIGININFO=?, RFDATA=?, FORWARDPOSITION=?, HOISTPOSITION=?, SHIFTPOSITION=?, ROTATEPOSITION=?, HIDDATA=?, VEHICLETYPE=?, INPUTDATA=?, OUTPUTDATA=?, CARRIERTYPE=?, STATIONMAPVERSION=?, TEACHINGMAPVERSION=? WHERE VEHICLEID=?";
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean updateVehicleInfoToDB() {
		if (update.size() == 0) {
			return false;
		}
		int count = 0;
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		Vehicle vehicle;
		boolean result = false;
		Vector<Vehicle> updateTemp = null;
		Vector<Vehicle> updateClone = null;
		try {
			conn = dbAccessManagerForBatch.getConnection();
			// 2011.11.08 by PMM
			// conn에 대한 not null 체크
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_SQL);
			
			updateTemp = new Vector<Vehicle>();
			updateClone = (Vector<Vehicle>)update.clone();
			
			ListIterator<Vehicle> iteratorSnapshot = updateClone.listIterator();
			while (iteratorSnapshot.hasNext()) {
				vehicle = iteratorSnapshot.next();
				if (vehicle != null) {
					if (updateTemp.contains(vehicle) == false) {
						updateTemp.add(vehicle);
					}
				} else {
					writeExceptionLog(LOGFILENAME, "vehicle is null!!!" + count + "/" + updateClone.size());
					StringBuffer tempList = new StringBuffer();
					for (Vehicle tempKey : updateClone) {
						if (tempKey != null) {
							tempList.append(tempKey.getVehicleId()).append("/");
						} else {
							tempList.append("null/");
						}
					}
					writeExceptionLog(LOGFILENAME, "   Temp List: " + tempList.toString());
				}
				update.remove(0);
			}
			ListIterator<Vehicle> iterator = updateTemp.listIterator();
			while (iterator.hasNext()) {
				vehicle = iterator.next();
				if (vehicle != null) {
					// 2014.03.07 by MYM : [Stage Locate 기능] RequestedType, RequestedData는 업데이트 하지않도록 함. 
					//                                        JobAssign, LongRun 등에서 요청한 것을 다시 Operation이 DB에 업데이트 하면 StageCancel 요청 처리가 안됨
					pstmt.setString(1, String.valueOf(vehicle.getVehicleMode()));
					pstmt.setString(2, String.valueOf(vehicle.getState()));
					pstmt.setString(3, String.valueOf(vehicle.getCarrierExist()));
					pstmt.setString(4, vehicle.getCurrNode());
					pstmt.setString(5, vehicle.getStopNode());
					pstmt.setString(6, vehicle.getTargetNode());
					pstmt.setInt(7, vehicle.getErrorCode());

					// 2011.12.27 by PMM
					//pstmt.setString(11, vehicle.getReason());
					if (vehicle.getReason() == null ||
							vehicle.getReason().indexOf("driveLimitTime Over") >= 0) {
						pstmt.setString(8, "");
					} else {
						if (vehicle.getReason().length() < 128) {
							StringReader reader = new StringReader(vehicle.getReason());
							pstmt.setCharacterStream(8, reader, vehicle.getReason().length());
						} else {
							StringReader reader = new StringReader(vehicle.getReason().substring(0, 128));
							pstmt.setCharacterStream(8, reader, 128);
						}
					}
					pstmt.setDouble(9, vehicle.getVehicleSpeed());
					pstmt.setString(10, vehicle.getMapVersion());
					pstmt.setInt(11, vehicle.getAPSignal());
					pstmt.setString(12, vehicle.getAPMacAddress());
					// 2013.02.15 by KYK
					pstmt.setInt(13, vehicle.getCurrNodeOffset());
					pstmt.setString(14, vehicle.getCurrStation());
					pstmt.setString(15, vehicle.getStopStation());
					pstmt.setString(16, vehicle.getTargetStation());
					
					// 2013.05.29 by MYM : Vehicle Profile 업데이트 추가
					pstmt.setInt(17, vehicle.getPauseType());
					pstmt.setInt(18, vehicle.getSteerPosition());
					pstmt.setInt(19, vehicle.getOriginInfo());
					pstmt.setString(20, vehicle.getRfData());
					pstmt.setInt(21, vehicle.getMotorDrvFPosition());
					pstmt.setInt(22, vehicle.getMotorHoistPosition());
					pstmt.setInt(23, vehicle.getMotorShiftPosition());
					pstmt.setInt(24, vehicle.getMotorRotate());
					pstmt.setString(25, vehicle.getHidData());
					pstmt.setInt(26, vehicle.getVehicleType());
					pstmt.setString(27, vehicle.getInputData());
					pstmt.setString(28, vehicle.getOutputData());
					pstmt.setInt(29, vehicle.getCarrierType());
					pstmt.setString(30, vehicle.getStationMapVersion());
					pstmt.setString(31, vehicle.getTeachingMapVersion());
					
					pstmt.setString(32, vehicle.getVehicleId());
					
					pstmt.addBatch();
				} else {
					writeExceptionLog(LOGFILENAME, "vehicle is null!!!" + count + "/" + updateTemp.size());
					StringBuffer tempList = new StringBuffer();
					for (Vehicle tempKey : updateTemp) {
						tempList.append(tempKey.getVehicleId()).append("/");
					}
					writeExceptionLog(LOGFILENAME, "   Temp List: " + tempList.toString());
					updateTemp.remove(null);
				}
				count++;
			}
			pstmt.executeBatch();
			conn.commit();
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
		if (result == false) {
			dbAccessManagerForBatch.requestDBReconnect();
			
			// 2011.11.08 by PMM
			// updateStateListClone에 대한 not null 조건 추가
			if (updateTemp != null && updateTemp.size() > 0) {
				for (Vehicle tempKey : updateTemp) {
					update.add(tempKey);
				}
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		if (elapsedTime > 50) {
			writeLog(LOGFILENAME, "UpdateCount:" + count);
			writeLog(LOGFILENAME, "Time:" + (System.currentTimeMillis() - startTime) + " (over 50ms)");
		}
		return result;
	}
	
	private static final String UPDATE_LOCUS_SQL = "UPDATE VEHICLE SET LOCUS=? WHERE VEHICLEID=?";

	/**
	 * 2013.04.02 by MYM
	 * Update Vehicle Locus to DB
	 * 
	 * @param vehicleId
	 * @param locus
	 * @return
	 */
	@Deprecated
	public boolean updateVehicleLocusToDB(String vehicleId, String locus) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_LOCUS_SQL);
			if (locus.length() < 2000) {
				pstmt.setString(1, locus);
			} else if (locus.length() < 4000) {
				StringReader reader = new StringReader(locus);
				pstmt.setCharacterStream(1, reader, locus.length());
			} else {
				StringReader reader = new StringReader(locus);
				pstmt.setCharacterStream(1, reader, 4000);
			}
			pstmt.setString(2, vehicleId);
			pstmt.executeUpdate();
			result = true;
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;				
	}
	
	/**
	 * 2015.08.13 by KYK : vehicle Locus batch update
	 * @return
	 */
	private boolean updateVehicleLocusToDB() {
		if (locusUpdate.size() == 0) {
			return false;
		}
		int count = 0;
		long startTime = System.currentTimeMillis();
		Connection conn = null;
		PreparedStatement pstmt = null;
		Vehicle vehicle;
		boolean result = false;
		Vector<Vehicle> updateTemp = null;
		Vector<Vehicle> updateClone = null;
		try {
			conn = dbAccessManagerForBatch.getConnection();
			if (conn == null) {
				return false;
			}
			conn.setAutoCommit(false);
			pstmt = conn.prepareStatement(UPDATE_LOCUS_SQL);
			
			updateTemp = new Vector<Vehicle>();
			updateClone = (Vector<Vehicle>)locusUpdate.clone();
			
			ListIterator<Vehicle> iteratorSnapshot = updateClone.listIterator();
			while (iteratorSnapshot.hasNext()) {
				vehicle = iteratorSnapshot.next();
				if (vehicle != null) {
					if (updateTemp.contains(vehicle) == false) {
						updateTemp.add(vehicle);
					}
				} else {
					writeExceptionLog(LOGFILENAME, "vehicle is null!!!" + count + "/" + updateClone.size());
					StringBuffer tempList = new StringBuffer();
					for (Vehicle tempKey : updateClone) {
						if (tempKey != null) {
							tempList.append(tempKey.getVehicleId()).append("/");
						} else {
							tempList.append("null/");
						}
					}
					writeExceptionLog(LOGFILENAME, "   locus Temp List: " + tempList.toString());
				}
				locusUpdate.remove(0);
			}
			ListIterator<Vehicle> iterator = updateTemp.listIterator();
			while (iterator.hasNext()) {
				vehicle = iterator.next();
				if (vehicle != null) {
					String locus = vehicle.getLocus();
//					if (locus.length() < 2000) {
//						pstmt.setString(13, locus);
//					} else if (locus.length() < 4000) {
					if (locus.length() < 4000) {
						StringReader reader = new StringReader(locus);
						pstmt.setCharacterStream(1, reader, locus.length());
					} else {
						StringReader reader = new StringReader(locus);
						pstmt.setCharacterStream(1, reader, 4000);
					}
					pstmt.setString(2, vehicle.getVehicleId());
					
					pstmt.addBatch();
				} else {
					writeExceptionLog(LOGFILENAME, "vehicle is null!!!" + count + "/" + updateTemp.size());
					StringBuffer tempList = new StringBuffer();
					for (Vehicle tempKey : updateTemp) {
						tempList.append(tempKey.getVehicleId()).append("/");
					}
					writeExceptionLog(LOGFILENAME, "   Temp List: " + tempList.toString());
					updateTemp.remove(null);
				}
				count++;
			}
			pstmt.executeBatch();
			conn.commit();
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
		if (result == false) {
			dbAccessManagerForBatch.requestDBReconnect();
			if (updateTemp != null && updateTemp.size() > 0) {
				for (Vehicle tempKey : updateTemp) {
					locusUpdate.add(tempKey);
				}
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		if (elapsedTime > 50) {
			writeLog(LOGFILENAME, "LocusUpdateCount:" + count);
			writeLog(LOGFILENAME, "Time:" + (System.currentTimeMillis() - startTime) + " (over 50ms)");
		}
		return result;
	}
	
	/**
	 * 2015.08.13 by KYK : locus update
	 * @param vehicle
	 */
	public void addVehicleToLocusUpdateList(Vehicle vehicle) {
		assert vehicle != null;
		try {
			locusUpdate.add(vehicle);
			
			if (vehicle == null) {
				writeExceptionLog(LOGFILENAME, "vehicle is null!!!");
				StringBuffer tempList = new StringBuffer();
				for (Vehicle tempKey : locusUpdate) {
					if (tempKey != null) {
						tempList.append(tempKey.getVehicleId()).append("/");
					} else {
						tempList.append("null/");
					}
				}
				writeExceptionLog(LOGFILENAME, "   Locus Temp List: " + tempList.toString());
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param rs
	 * @exception SQLException
	 */
	private void setVehicle(Vehicle vehicle, ResultSet rs) throws SQLException {
		vehicle.setRequestedType(REQUESTEDTYPE.toVehicleRequestedType(getString(rs.getString(REQUESTEDTYPE_))));
		vehicle.setVehicleMode(getString(rs.getString(VEHICLEMODE)).charAt(0));
		vehicle.setState(getString(rs.getString(STATE)).charAt(0));
//		vehicle.setCarrierExist(getString(rs.getString(CARRIEREXIST)).charAt(0));		// Operation에서는 VHL로부터 올라오는 정보 우선. 현재 타 모듈에서는 사용 안함. 추후 사용 예정.
		vehicle.setCurrNode(getString(rs.getString(CURRNODE)));
		vehicle.setStopNode(getString(rs.getString(STOPNODE)));
		vehicle.setTargetNode(getString(rs.getString(TARGETNODE)));
		vehicle.setErrorCode(rs.getInt(ERRORCODE));
		vehicle.setAssignHold(getBoolean(rs.getString(ASSIGNHOLD)));
		vehicle.setActionHold(getBoolean(rs.getString(ACTIONHOLD)));
		vehicle.setLoadingByPass(getBoolean(rs.getString(LOADINGBYPASS)));
		vehicle.setEnabled(getBoolean(rs.getString(ENABLED)));
		vehicle.setIpAddress(getString(rs.getString(IPADDRESS)));
		vehicle.setRequestedData(getString(rs.getString(REQUESTEDDATA)));
		vehicle.setRequestedCost(rs.getDouble(REQUESTEDCOST));
//		vehicle.setLocus(rs.getString(LOCUS));
		setVehicleMaterial(vehicle, getString(rs.getString(MATERIAL)));
		setVehicleZone(vehicle, getString(rs.getString(ZONE)));
		vehicle.setSemiManual(getBoolean(rs.getString(SEMIMANUAL)));
		vehicle.setReason(getString(rs.getString(REASON)));
		vehicle.setLocalGroupId(getString(rs.getString(LOCALGROUPID)));
		vehicle.setVehicleSpeed(rs.getDouble(VEHICLESPEED));
		vehicle.setMapVersion(getString(rs.getString(MAPVERSION)));
		// 2013.02.15 by KYK
		vehicle.setCurrNodeOffset(rs.getInt(CURRNODEOFFSET));
		vehicle.setCurrStation(getString(rs.getString(CURRSTATION)));
		vehicle.setStopStation(getString(rs.getString(STOPSTATION)));
		vehicle.setTargetStation(getString(rs.getString(TARGETSTATION)));
		// 2013.09.06 by KYK
		vehicle.setCarrierType(rs.getInt(CARRIERTYPE));
	}
	
	private void setVehicleMaterial(Vehicle vehicle, String material) {
		vehicle.setMaterial(material);
		vehicle.setMaterialIndex(materialTable.getMaterialIndex(material));
	}
	
	private void setVehicleZone(Vehicle vehicle, String zone) {
		vehicle.setZone(zone);
		vehicle.setZoneIndex(zoneTable.getZoneIndex(zone));
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param rs
	 * @exception SQLException
	 */
	private void updateVehicle2(Vehicle vehicle, ResultSet rs) throws SQLException {
		vehicle.setRequestedType(REQUESTEDTYPE.toVehicleRequestedType(rs.getString(REQUESTEDTYPE_)));
		vehicle.setRequestedData(getString(rs.getString(REQUESTEDDATA)));
		vehicle.setRequestedCost(rs.getDouble(REQUESTEDCOST));
		
		// 2012.06.01 by PMM
		// Failover 시 DB 기준으로 정리하는 데 꼭 필요함.
		vehicle.setCurrNode(getString(rs.getString(CURRNODE)));
		
		vehicle.setStopNode(getString(rs.getString(STOPNODE)));
		vehicle.setTargetNode(getString(rs.getString(TARGETNODE)));
		vehicle.setAssignHold(getBoolean(rs.getString(ASSIGNHOLD)));
		vehicle.setActionHold(getBoolean(rs.getString(ACTIONHOLD)));
		vehicle.setEnabled(getBoolean(rs.getString(ENABLED)));
		vehicle.setIpAddress(getString(rs.getString(IPADDRESS)));
		setVehicleZone(vehicle, getString(rs.getString(ZONE)));
		vehicle.setSemiManual(getBoolean(rs.getString(SEMIMANUAL)));
		vehicle.setLocalGroupId(getString(rs.getString(LOCALGROUPID)));
		
		// 2013.02.15 by KYK
		vehicle.setCurrNodeOffset(rs.getInt(CURRNODEOFFSET));
		vehicle.setCurrStation(getString(rs.getString(CURRSTATION)));
		vehicle.setStopStation(getString(rs.getString(STOPSTATION)));
		vehicle.setTargetStation(getString(rs.getString(TARGETSTATION)));

		if (vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0) {
			isAllLocalGroupInfoCleared = false;
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 * @param rs
	 * @exception SQLException
	 */
	private void updateVehicle(Vehicle vehicle, ResultSet rs) throws SQLException {
		vehicle.setRequestedType(REQUESTEDTYPE.toVehicleRequestedType(rs.getString(REQUESTEDTYPE_)));
		vehicle.setRequestedData(getString(rs.getString(REQUESTEDDATA)));
		vehicle.setRequestedCost(rs.getDouble(REQUESTEDCOST));
		vehicle.setAssignHold(getBoolean(rs.getString(ASSIGNHOLD)));
		vehicle.setActionHold(getBoolean(rs.getString(ACTIONHOLD)));
		vehicle.setEnabled(getBoolean(rs.getString(ENABLED)));
		vehicle.setIpAddress(getString(rs.getString(IPADDRESS)));
		setVehicleZone(vehicle, getString(rs.getString(ZONE)));
		vehicle.setSemiManual(getBoolean(rs.getString(SEMIMANUAL)));
		vehicle.setLocalGroupId(getString(rs.getString(LOCALGROUPID)));
		
		if (vehicle.getLocalGroupId() != null && vehicle.getLocalGroupId().length() > 0) {
			isAllLocalGroupInfoCleared = false;
		}
	}
	
	/**
	 * 
	 * @param vehicle
	 */
	public void addVehicleToUpdateList(Vehicle vehicle) {
		assert vehicle != null;
		try {
			update.add(vehicle);
			
			if (vehicle == null) {
				writeExceptionLog(LOGFILENAME, "vehicle is null!!!");
				StringBuffer tempList = new StringBuffer();
				for (Vehicle tempKey : update) {
					if (tempKey != null) {
						tempList.append(tempKey.getVehicleId()).append("/");
					} else {
						tempList.append("null/");
					}
				}
				writeExceptionLog(LOGFILENAME, "   Temp List: " + tempList.toString());
			}
		} catch (Exception e) {
			writeExceptionLog(LOGFILENAME, e);
		}
	}
	
	// 2012.02.09 by PMM
	private static String RESET_VEHICLE_REQUESTEDINFO_SQL = "UPDATE VEHICLE SET REQUESTEDTYPE='',REQUESTEDDATA='',REQUESTEDCOST=0 WHERE VEHICLEID=?";
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public boolean resetVehicleRequestedInfoToDB(Vehicle vehicle) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(RESET_VEHICLE_REQUESTEDINFO_SQL);
			pstmt.setString(1, vehicle.getVehicleId());
			pstmt.execute();
			result = true;
			vehicle.setRequestedCost(0);
			vehicle.setRequestedType(REQUESTEDTYPE.NULL);
			vehicle.setRequestedData("");
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;				
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public Vehicle getVehicle(String vehicleId) {
		return (Vehicle) data.get(vehicleId);
	}
	
	// JobAssign
	/**
	 * 
	 */
	public boolean updateToDBForJobAssign() {
		registerRequestedDataToDB();
		releaseLocalGroupOfVehicle();
		registerLocalGroupOfVehicle();
		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean updateFromDBForJobAssign() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Set<String> removeKeys = new HashSet<String>(data.keySet());
		String vehicleId;
		Vehicle vehicle;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				vehicleId = rs.getString(VEHICLEID);
				vehicle = (Vehicle)data.get(vehicleId);
				if (vehicle == null) {
					vehicle = (Vehicle) vOType.newInstance();
					vehicle.setVehicleId(vehicleId);
					data.put(vehicleId, vehicle);
				}
				setVehicle(vehicle, rs);
				removeKeys.remove(vehicleId);
			}
			for (String rmKey : removeKeys) {
				data.remove(rmKey);
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

	// 2013.06.05 by KYK
//	private static final String SELECT_VEHICLELOCATION_SQL = "SELECT CURRNODE,STOPNODE,TARGETNODE,VEHICLEID FROM VEHICLE ORDER BY VEHICLEID";
	private static final String SELECT_VEHICLELOCATION_SQL = "SELECT CURRNODE,STOPNODE,TARGETNODE,CURRSTATION,STOPSTATION,TARGETSTATION,CURRNODEOFFSET,VEHICLEID FROM VEHICLE ORDER BY VEHICLEID";
	
	/**
	 * 
	 * @return
	 */
	public boolean updateVehicleLocationFromDBForJobAssign() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String vehicleId;
		Vehicle vehicle;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_VEHICLELOCATION_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				vehicleId = rs.getString(VEHICLEID);
				vehicle = (Vehicle)data.get(vehicleId);
				if (vehicle != null) {
					setVehicleLocation(vehicle, rs);
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
	 * @param vehicle
	 * @param rs
	 * @exception SQLException
	 */
	private void setVehicleLocation(Vehicle vehicle, ResultSet rs) throws SQLException {
		vehicle.setCurrNode(getString(rs.getString(CURRNODE)));
		vehicle.setStopNode(getString(rs.getString(STOPNODE)));
		vehicle.setTargetNode(getString(rs.getString(TARGETNODE)));

		// 2013.02.15 by KYK
		vehicle.setCurrNodeOffset(rs.getInt(CURRNODEOFFSET));
		vehicle.setCurrStation(getString(rs.getString(CURRSTATION)));
		vehicle.setStopStation(getString(rs.getString(STOPSTATION)));
		vehicle.setTargetStation(getString(rs.getString(TARGETSTATION)));

	}
	
	/**
	 * 
	 * @param vehicleId
	 */
	public void addVehicleToRequestedDataRegisterList(String vehicleId) {
		if (register.contains(vehicleId) == false) {
			register.add(vehicleId);
		}
	}
	
	private static final String REGISTER_REQUESTEDDATA_SQL = "UPDATE VEHICLE SET REQUESTEDDATA=?, REQUESTEDTYPE=? WHERE VEHICLEID=? AND REQUESTEDTYPE IS NULL AND VEHICLEMODE='A'";
	/**
	 * 
	 * @return
	 */
	public boolean registerRequestedDataToDB() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String vehicleId;
		Vehicle vehicle;
		boolean result = false;
		try {
			Vector<String> registerTemp = new Vector<String>(register);
			ListIterator<String> iterator = registerTemp.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(REGISTER_REQUESTEDDATA_SQL);
			while (iterator.hasNext()) {
				vehicleId = (String) iterator.next();
				vehicle = (Vehicle) data.get(vehicleId);
				if (vehicle != null) {
					pstmt.setString(1, vehicle.getRequestedData());
					pstmt.setString(2, vehicle.getRequestedType().toConstString());
					pstmt.setString(3, vehicleId);
					pstmt.execute();
				}
				register.remove(0);
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
	
	private static final String CLEAR_ALL_LOCALGROUPID_SQL = "UPDATE VEHICLE SET LOCALGROUPID='' WHERE LOCALGROUPID IS NOT NULL";
	public boolean clearAllLocalGroupId() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(CLEAR_ALL_LOCALGROUPID_SQL);
			pstmt.execute();
			result = true;
			isAllLocalGroupInfoCleared = true;
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
		}
		if (result == false) {
			dbAccessManager.requestDBReconnect();
		}
		return result;
	}
	
	private static final String CLEAR_LOCALGROUPID_SQL = "UPDATE VEHICLE SET LOCALGROUPID='' WHERE VEHICLEID=?";
	public boolean clearLocalGroupId(String vehicleId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(CLEAR_LOCALGROUPID_SQL);
			pstmt.setString(1, vehicleId);
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
	
	public boolean isAllLocalGroupInfoCleared() {
		return isAllLocalGroupInfoCleared;
	}
	
	// 2012.01.11 by PMM
//	private static final String SELECT_LOCALGROUP_CURRVHL_SQL = "SELECT L.LOCALGROUPID, COUNT(V.VEHICLEID) AS CURVHL FROM VEHICLE V, LOCALGROUPINFO L WHERE V.LOCALGROUPID(+)=L.LOCALGROUPID GROUP BY L.LOCALGROUPID";
	private static final String SELECT_LOCALGROUP_CURRVHL_SQL = "SELECT LOCALGROUPID, COUNT(VEHICLEID) AS CURVHL FROM VEHICLE WHERE LOCALGROUPID IS NOT NULL GROUP BY LOCALGROUPID";
	/**
	 * 
	 * @return
	 */
	public HashMap<String, Integer> getLocalGroupCurrVHL() {
		if (registerLocalGroupOfVehicle.size() > 0) {
			registerLocalGroupOfVehicle();
		}
		if (releaseLocalGroupOfVehicle.size() > 0) {
			releaseLocalGroupOfVehicle();
		}
		
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Set<String> removeKeys = new HashSet<String>(localGroupCurrVHL.keySet());
		String localGroupId;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_LOCALGROUP_CURRVHL_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				localGroupId = rs.getString(LOCALGROUPID);
				localGroupCurrVHL.put(localGroupId, rs.getInt(CURVHL));
				removeKeys.remove(localGroupId);
				
				// 2012.01.11 by PMM
				isAllLocalGroupInfoCleared = false;
			}
			for (String rmKey : removeKeys) {
				localGroupCurrVHL.remove(rmKey);
			}
			result = true;
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
		return localGroupCurrVHL;
	}
	
	/**
	 * 
	 * @param vehicleId
	 * @param localGroupId
	 */
	public void addVehicleToRegisterLocalGroupList(String vehicleId, String localGroupId) {
		registerLocalGroupOfVehicle.put(vehicleId, localGroupId);
	}
	
	private static final String REGISTER_LOCALGROUPOFVEHICLE_SQL = "UPDATE VEHICLE SET LOCALGROUPID=? WHERE VEHICLEID=? AND REQUESTEDTYPE IS NULL AND VEHICLEMODE='A'";
	/**
	 * 
	 * @return
	 */
	private boolean registerLocalGroupOfVehicle() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(REGISTER_LOCALGROUPOFVEHICLE_SQL);
			Set<String> registerKeys = new HashSet<String>(registerLocalGroupOfVehicle.keySet());
			for (String registerKey : registerKeys) {
				pstmt.setString(1, (String) registerLocalGroupOfVehicle.get(registerKey));
				pstmt.setString(2, registerKey);
				pstmt.executeUpdate();
				registerLocalGroupOfVehicle.remove(registerKey);
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
	 * @param vehicleId
	 */
	public void addVehicleToReleaseLocalGroupList(String vehicleId) {
		if (releaseLocalGroupOfVehicle.contains(vehicleId) == false) {
			releaseLocalGroupOfVehicle.add(vehicleId);
		}
	}
	
	private static final String RELEASE_LOCALGROUPOFVEHICLE_SQL = "UPDATE VEHICLE SET LOCALGROUPID='' WHERE VEHICLEID=?";
	/**
	 * 
	 * @return
	 */
	private boolean releaseLocalGroupOfVehicle() {
		Connection conn = null;
		PreparedStatement pstmt = null;
		String vehicleId;
		Vehicle vehicle;
		ListIterator<String> iterator;
		Vector<String> registerTemp;
		boolean result = false;
		try {
			registerTemp = new Vector<String>(releaseLocalGroupOfVehicle);
			iterator = registerTemp.listIterator();
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(RELEASE_LOCALGROUPOFVEHICLE_SQL);
			while (iterator.hasNext()) {
				vehicleId = (String) iterator.next();
				vehicle = (Vehicle) data.get(vehicleId);
				if (vehicle != null) {
					pstmt.setString(1, vehicleId);
					pstmt.execute();
				}
				releaseLocalGroupOfVehicle.remove(0);
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
	
	/*
	 * Add Methods For IBSEM ONLY
	 */
	private static final String SELECT_ACTIVEVEHICLE_SQL = "SELECT VEHICLEID,STATUS FROM VEHICLE WHERE ENABLED='TRUE'";
	/**
	 * 
	 * @return
	 */
	public HashMap<String, Vehicle> getActiveVehiclesFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		HashMap<String, Vehicle> vehicleTable = new HashMap<String, Vehicle>();
		Vehicle vehicle;
		String vehicleId;
		char status;

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_ACTIVEVEHICLE_SQL);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				vehicle = new Vehicle();
				vehicleId = getString(rs.getString(VEHICLEID));
				status = getString(rs.getString(STATE)).charAt(0);

				vehicle.setVehicleId(vehicleId);
				vehicle.setState(status);
				
				vehicleTable.put(vehicleId, vehicle);
			}			
		} catch (SQLException se) {
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} catch (Exception e) {
			e.printStackTrace();
			dbAccessManager.requestDBReconnect();
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
		return vehicleTable;
	}

	private static final String SELECT_STATEASSIGNEDVEHICLE_SQL = "SELECT VEHICLEID FROM VEHICLE WHERE REQUESTEDDATA=?";
	/**
	 * 
	 * @param carrierId
	 * @return
	 */
	public String getStageAssignedVehicleFromDB(String carrierId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String vehicleId = null;
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_STATEASSIGNEDVEHICLE_SQL);
			pstmt.setString(1, carrierId);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				vehicleId = getString(rs.getString(VEHICLEID));
			}
		} catch (SQLException se) {
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
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
		return vehicleId;
	}

	private static String UPDATE_VEHICLE_REQUESTEDINFO_SQL = "UPDATE VEHICLE SET REQUESTEDTYPE=?,REQUESTEDDATA=? WHERE VEHICLEID=?";
	/**
	 * 
	 * @param requestedtype
	 * @param requesteddata
	 * @param vehicleId
	 * @return
	 */
	public boolean updateVehicleRequestedInfoToDB(String requestedtype, String requesteddata, String vehicleId) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		
		try {
			conn = dbAccessManager.getConnection();			
			pstmt = conn.prepareStatement(UPDATE_VEHICLE_REQUESTEDINFO_SQL);
			pstmt.setString(1, requestedtype);
			pstmt.setString(2, requesteddata);
			pstmt.setString(3, vehicleId);
			pstmt.execute();
			result = true;
//			conn.commit();
		} catch (SQLException se) {
			se.printStackTrace();
			result = false;
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (Exception ignore2) {}
			}
		}
		return result;				
	}
	
	// 2011.11.02 by PMM
	// IBSEM 패치 시, 전체 VHL ActionHold 후, ibsem 모듈 재시작 -> TSC_PAUSING -> TSC_PAUSED -> TSC_AUTO 시 모든 VHL ActionHold 해제.
//	private static final String resumeAllEnabledVehicleActionHoldSql = "UPDATE VEHICLE SET ACTIONPAUSE='FALSE' WHERE ENABLED='TRUE'";
	private static final String RESUME_ALLVEHICLE_ACTIONHOLD_SQL = "UPDATE VEHICLE SET ACTIONPAUSE='FALSE'";
	/**
	 * 
	 * @return
	 */
	public boolean resumeAllVehicleActionHold() {
		boolean result = false;
		Connection conn = null;
		Statement statement = null;
		try {
			conn = dbAccessManager.getConnection();
			statement = conn.createStatement();
			statement.execute(RESUME_ALLVEHICLE_ACTIONHOLD_SQL);
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
		} else {
			result = updateFromDB();
		}
		return result;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isNothingUpdateToDB() {
		if (update.size() > 0) {
			writeLog(LOGFILENAME, "update: " + update.size());
			return false;
		}
		if (register.size() > 0) {
			writeLog(LOGFILENAME, "register: " + register.size());
			return false;
		}
		if (registerLocalGroupOfVehicle.size() > 0) {
			writeLog(LOGFILENAME, "registerLocalGroupOfVehicle: " + registerLocalGroupOfVehicle.size());
			return false;
		}
		if (releaseLocalGroupOfVehicle.size() > 0) {
			writeLog(LOGFILENAME, "releaseLocalGroupOfVehicle: " + releaseLocalGroupOfVehicle.size());
			return false;
		}
		return true;
	}
	
	private static final String SELECT_VEHICLEZONE_SQL = "SELECT ZONE, COUNT(VEHICLEID) FROM VEHICLE WHERE ENABLED='TRUE' AND JOBPAUSE='FALSE' AND VEHICLEMODE='A' GROUP BY ZONE ORDER BY 2 DESC";
	/**
	 * 
	 * @return
	 */
	public ArrayList<String> getVehicleZoneList() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String zone = null;
		ArrayList<String> vehicleZoneList = new ArrayList<String>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_VEHICLEZONE_SQL);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				zone = getString(rs.getString(ZONE));
				vehicleZoneList.add(zone);
			}
		} catch (SQLException se) {
			se.printStackTrace();
			dbAccessManager.requestDBReconnect();
			writeExceptionLog(LOGFILENAME, se);
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
		return vehicleZoneList;
	}
	
	private static String UPDATE_VEHICLE_ASSIGNHOLD_SQL = "UPDATE VEHICLE SET JOBPAUSE=? WHERE VEHICLEID=?";
	/**
	 * 
	 * @param vehicleId
	 * @return
	 */
	public boolean setAssignHoldToDB(Vehicle vehicle, boolean isAssignHold) {
		boolean result = false;
		if (vehicle != null) {
			Connection conn = null;
			PreparedStatement pstmt = null;
			
			try {
				conn = dbAccessManager.getConnection();			
				pstmt = conn.prepareStatement(UPDATE_VEHICLE_ASSIGNHOLD_SQL);
				if (isAssignHold) {
					pstmt.setString(1, TRUE);
				} else {
					pstmt.setString(1, FALSE);
				}
				pstmt.setString(2, vehicle.getVehicleId());
				pstmt.execute();
				vehicle.setActionHold(isAssignHold);
				result = true;
			} catch (SQLException se) {
				se.printStackTrace();
				result = false;
				dbAccessManager.requestDBReconnect();
				writeExceptionLog(LOGFILENAME, se);
			} finally {
				if (pstmt != null) {
					try { pstmt.close(); } catch (Exception ignore2) {}
				}
			}
		}
		return result;				
	}
	
	private final static String SELECT_VEHICLE_SQL = "SELECT * FROM VEHICLE WHERE VEHICLEID=?";
	/**
	 * 2013.08.30 by KYK
	 * @param vehicleId
	 * @return
	 */
	public Vehicle getVehicleFromDB(String vehicleId) {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		Vehicle vehicle = new Vehicle();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_VEHICLE_SQL);
			pstmt.setString(1, vehicleId);
			pstmt.setFetchSize(256);
			rs = pstmt.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					vehicle.setVehicleId(vehicleId);
					setVehicle(vehicle, rs);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (SQLException e) {}
			}
			if (rs != null) {
				try { rs.close(); } catch (SQLException e) {}
			}
		}		
		return vehicle;
	}

	private final static String SELECT_ERROR_VEHICLE = "SELECT * FROM VEHICLE WHERE ENABLED='TRUE' AND VEHICLEMODE='M' AND STATUS='E'";
	/**
	 * 2013.10.02 by KYK
	 * @return
	 */
	public HashMap<String,Vehicle> getErrorVehicleFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String vehicleId = null;
		HashMap<String,Vehicle> errorVehicleMap = new HashMap<String,Vehicle>();
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_ERROR_VEHICLE);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Vehicle vehicle = new Vehicle();
				vehicleId = getString(rs.getString(VEHICLEID));
				vehicle.setVehicleId(vehicleId);
				setVehicle(vehicle, rs);
				if (errorVehicleMap.containsKey(vehicleId) == false) {
					errorVehicleMap.put(vehicleId, vehicle);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			writeExceptionLog(LOGFILENAME, e);
		} finally {
			if (pstmt != null) {
				try { pstmt.close(); } catch (SQLException e) {}
			}
			if (rs != null) {
				try { rs.close(); } catch (SQLException e) {}
			}
		}		
		return errorVehicleMap;
	}

	public boolean checkCurrentDBStatus() {
		return super.checkDBStatus();
	}
	
}