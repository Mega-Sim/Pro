package com.samsung.ocs.manager.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.manager.impl.model.MaterialControl;

//2019.10.14 by kw3711.kim : MaterialControl Dest Port Ãß°¡
public class MaterialControlManager extends AbstractManager {

	private static MaterialControlManager manager = null;
	private static String VEHICLEMATERIAL = "VEHICLEMATERIAL";
	private static String SOURCELOCMATERIAL = "SOURCELOCMATERIAL";
	private static String DESTLOCMATERIAL = "DESTLOCMATERIAL";
//	private static String ASSIGNALLOWANCE = "ASSIGNALLOWANCE";
	private static String KEY_SEPERATOR = "_";
	
	private ArrayList<String> materialAssignAllowedList;
	private ArrayList<String> sourceDestAssignAllowedList;
	
	private MaterialControlManager (Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		super(dbAccessManager, vOType, initializeAtStart, makeManagerThread, managerThreadInterval);
		LOGFILENAME = this.getClass().getName();

		if (vOType != null && vOType.getClass().isInstance(MaterialControl.class)) {
			if (managerThread != null) {
				managerThread.setRunFlag(true);
			}
		} else {
			writeExceptionLog(LOGFILENAME, "Object Type Not Supported");
		}
		
		if (materialAssignAllowedList == null) {
			materialAssignAllowedList = new ArrayList<String>();			
		}
		if (sourceDestAssignAllowedList == null) {
			sourceDestAssignAllowedList = new ArrayList<String>();			
		}
		isInitialized = true;
		init();
	}
	
	public synchronized static MaterialControlManager getInstance(Class<?> vOType, DBAccessManager dbAccessManager, boolean initializeAtStart, boolean makeManagerThread, long managerThreadInterval) {
		if (manager == null) {
			manager = new MaterialControlManager(vOType, dbAccessManager, initializeAtStart, makeManagerThread, managerThreadInterval);
		} 
		return manager;
	}

	public void initializeFromDB() {
		data.clear();
		init();
	}
	
	@Override
	protected void init() {
		initialize();
		isInitialized = true;
	}

	private boolean initialize() {
		boolean result = false;
		if (isInitialized) {
			result = initializeMaterialAssignAllowedKey();
			result &= initializeSourceDestAssignAllowedKey();
		}
		return result;
	}

	@Override
	protected boolean updateFromDB() {
		return false;
	}

	@Override
	protected boolean updateToDB() {
		return false;
	}

	private static final String SELECT_MATERIALCONTOL_ALLOWED_SQL = "SELECT DISTINCT VEHICLEMATERIAL, SOURCELOCMATERIAL, DESTLOCMATERIAL FROM MATERIALCONTROL WHERE ASSIGNALLOWANCE = 'TRUE'";
	private boolean initializeMaterialAssignAllowedKey() {
		boolean result = false;
		if (materialAssignAllowedList == null) {
			materialAssignAllowedList = new ArrayList<String>();			
		}
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String key = null;
		ArrayList<String> tempAssignList = new ArrayList<String>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_MATERIALCONTOL_ALLOWED_SQL);
			rs = pstmt.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					key = makeMeterialAssignAllowedKey(getString(rs.getString(VEHICLEMATERIAL)),
													   getString(rs.getString(SOURCELOCMATERIAL)),
													   getString(rs.getString(DESTLOCMATERIAL)) );
					if (materialAssignAllowedList.contains(key) == false) {
						materialAssignAllowedList.add(key);
					}
					tempAssignList.add(key);
				}
			}
			for (int i = materialAssignAllowedList.size(); i > 0; i--) {
				key = materialAssignAllowedList.get(i - 1);
				if (tempAssignList.contains(key) == false) {
					materialAssignAllowedList.remove(key);
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
		return result;
	}
	
	private static final String SELECT_SOURCE_DEST_SQL = "SELECT DISTINCT SOURCELOCMATERIAL, DESTLOCMATERIAL FROM MATERIALCONTROL WHERE ASSIGNALLOWANCE = 'TRUE'";
	private boolean initializeSourceDestAssignAllowedKey() {
		boolean result = false;
		if (sourceDestAssignAllowedList == null) {
			sourceDestAssignAllowedList = new ArrayList<String>();			
		}
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String key = null;
		ArrayList<String> tempAssignList = new ArrayList<String>();
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_SOURCE_DEST_SQL);
			rs = pstmt.executeQuery();
			if (rs != null) {
				while (rs.next()) {
					key = makeSourceDestAssignAllowedKey(getString(rs.getString(SOURCELOCMATERIAL)),
													     getString(rs.getString(DESTLOCMATERIAL)) );
					if (sourceDestAssignAllowedList.contains(key) == false) {
						sourceDestAssignAllowedList.add(key);
					}
					tempAssignList.add(key);
				}
			}
			for (int i = sourceDestAssignAllowedList.size(); i > 0; i--) {
				key = sourceDestAssignAllowedList.get(i - 1);
				if (tempAssignList.contains(key) == false) {
					sourceDestAssignAllowedList.remove(key);
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
		return result;
	}
	
//	private boolean isMaterialAssignAllowed(ResultSet rs) {
//		if (rs != null) {
//			try {
//				return getBoolean(rs.getString(ASSIGNALLOWANCE));
//			} catch (SQLException e) {
//				e.printStackTrace();
//				return false;
//			}
//		}
//		return false;
//	}
	
	public String makeMeterialAssignAllowedKey(String vehicleMaterial, String sourceLocMaterial, String destLocMaterial) {
		StringBuilder sb = new StringBuilder();
		sb.append(vehicleMaterial)
		  .append(KEY_SEPERATOR)
		  .append(sourceLocMaterial)
		  .append(KEY_SEPERATOR)
		  .append(destLocMaterial);
		return sb.toString();
	}
	
	public String makeSourceDestAssignAllowedKey(String sourceLocMaterial, String destLocMaterial) {
		StringBuilder sb = new StringBuilder();
		sb.append(sourceLocMaterial)
		  .append(KEY_SEPERATOR)
		  .append(destLocMaterial);
		return sb.toString();
	}

	public ArrayList<String> getMaterialAssignAllowedList() {
		return materialAssignAllowedList;
	}

	public ArrayList<String> getSourceDestAssignAllowedList() {
		return sourceDestAssignAllowedList;
	}
	
}
