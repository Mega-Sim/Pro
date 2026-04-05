package com.samsung.ocs.unitdevice;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.NetworkDevice;

/**
 * 
 * @author yk09.kang
 *
 */

public class NetworkDeviceManager extends Thread {
	
	private static NetworkDeviceManager manager;
	private DBAccessManager dbAccessManager;
	protected ConcurrentHashMap<String, NetworkDevice> data;

	private boolean isAlive;
	private boolean isPingCheckUsed;
	private int pingInterval = 200;
	private int threadInterval = 1000;
	
	private final String UNITID = "UNITID";
	private final String ENABLED = "ENABLED";
	private final String IPADDRESS = "IPADDRESS";
	private final String STATUS = "STATUS";
	private final String TYPE = "TYPE";
	private final String TRUE = "TRUE";
	private final String NAME = "NAME";
	private final String VALUE = "VALUE";
	private final String YES = "YES";
	private final String NETDEVICE_CHECK_USAGE = "NETDEVICE_CHECK_USAGE";
	private final String NETDEVICE_THREAD_INTERVAL = "NETDEVICE_THREAD_INTERVAL";
	
	public static synchronized NetworkDeviceManager getInstance() {
		if (manager == null) {
			manager = new NetworkDeviceManager();
		}
		return manager;
	}
	
	private NetworkDeviceManager() {
		dbAccessManager = new DBAccessManager();
		for (int i = 0; i < 5; i++) {
			if (dbAccessManager.IsDBConnected()) {
				break;
			}
			dbAccessManager.ReconnectToDB();
		}
		initialize();
	}
	
	private void initialize() {
		data = new ConcurrentHashMap<String, NetworkDevice>();
		updateParameterFromDB();
		updateNetDeviceFromDB();
	}
	
	public void run() {
		isAlive = true;
		
		while (isAlive) {
			try {
				updateFromDB();
				pingCheckProcess();
				//
				sleep(threadInterval);
			} catch (InterruptedException e) {
				operationTrace("NetworkDeviceManager mainProcess()",e);
				e.printStackTrace();
			}
		}
	}

	public boolean updateFromDB() {
		updateParameterFromDB();
		updateNetDeviceFromDB();
		return true;
	}
	
	private static final String SELECT_PARA_SQL = "SELECT * FROM OCSINFO WHERE NAME LIKE '%NETDEVICE%'";
	public boolean updateParameterFromDB() {
		ResultSet rs = null;
		String name = null;
		boolean result = false;
		try {
			rs = dbAccessManager.GetRecord(SELECT_PARA_SQL);
			if (rs != null) {
				while (rs.next()) {
					name = rs.getString(NAME);
					if (NETDEVICE_CHECK_USAGE.equals(name)) {
						setPingCheckUsed(YES.equals(rs.getString(VALUE)));
					} else if (NETDEVICE_THREAD_INTERVAL.equals(name)) {
						setPingInterval(rs.getString(VALUE));
					}
				}
			}
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			operationTrace("SQLException updateParameterFromDB()", e);
		} finally {
			if (rs != null) {
				dbAccessManager.CloseRecord(rs);
			}
		}
		return result;
	}

	private static final String SELECT_ND_SQL = "SELECT * FROM NETWORKDEVICE ORDER BY TYPE, UNITID";
	public boolean updateNetDeviceFromDB() {
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		boolean result = false;
		HashSet<String> removedKeys = new HashSet<String>(data.keySet());
		
		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(SELECT_ND_SQL);
			rs = pstmt.executeQuery();

			String unitId = null;
			NetworkDevice netDevice = null;
			while (rs.next()) {
				unitId = makeString(rs.getString(UNITID));
				netDevice = data.get(unitId);
				if (netDevice == null) {
					netDevice = new NetworkDevice(unitId);
					data.put(unitId, netDevice);
				}
				setNetDeviceInfo(netDevice, rs);
				removedKeys.remove(unitId);
			}
			for (String key: removedKeys) {
				data.remove(key);
			}
			result = true;
		} catch (SQLException e) {
			result = false;
			e.printStackTrace();
			operationTrace("DBException updateNetDeviceFromDB()",e);
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {}
			try {
				rs.close();
			} catch (SQLException e) {}
		}
		return result;
	}
	
	private void setNetDeviceInfo(NetworkDevice netDevice, ResultSet rs) throws SQLException {
		if (netDevice != null && rs != null) {
			netDevice.setIpAddress(makeString(rs.getString(IPADDRESS)));
			netDevice.setType(makeString(rs.getString(TYPE)));
			netDevice.setStatus(makeString(rs.getString(STATUS)));
			netDevice.setEnabled(makeString(rs.getString(ENABLED)).equals(TRUE));
		}		
	}	

	public void pingCheckProcess() {
		long startTime = System.currentTimeMillis();
		if (isPingCheckUsed) {
			NetworkDevice netDevice = null;
			for (String key: data.keySet()) {
				netDevice = data.get(key);
				if (netDevice != null) {
					if (netDevice.isEnabled()) {
						pingCheckProcess(key, netDevice.getIpAddress());
						try {
							Thread.sleep(pingInterval);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}						
					}
				}
			}
		}
		System.out.println("pingCheckTime:" + (System.currentTimeMillis() - startTime));
	}

	private void pingCheckProcess(String name, String ipAddress){		
		boolean isReachable = false;
		int timeout = 2000;
		String status = "FAIL";
		
		try {
			InetAddress target = InetAddress.getByName(ipAddress);
			isReachable = target.isReachable(timeout);
			if(isReachable) {
				status = "ALIVE";
			}

			StringBuffer sb = new StringBuffer();
			sb.append(name).append("> IP: ").append(ipAddress).append(" , status : ").append(status);				
			operationTrace(sb.toString(), null);
			System.out.println(sb.toString());

			updatePingStatusToDB(status, name);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(new Date() + " Unknown host " + ipAddress);
			operationTrace("Exception pingCheckProcess()", e);
		}
	}

	private static String UPDATE_STATUS = "UPDATE NETWORKDEVICE SET STATUS=? WHERE UNITID=?";
	public void updatePingStatusToDB(String status, String name) {
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = dbAccessManager.getConnection();
			pstmt = conn.prepareStatement(UPDATE_STATUS);
			pstmt.setString(1, status);
			pstmt.setString(2, name);
			pstmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			operationTrace("SQLException updatePingStatusToDB()", e);
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (Exception e) {}
				pstmt = null;
			}
		}
	}
	
	private String makeString(String value) {
		if (value == null) {
			return "";			
		} else {
			return value;			
		}
	}
	
	public ConcurrentHashMap<String, NetworkDevice> getData() {
		return data;
	}

	public void setData(ConcurrentHashMap<String, NetworkDevice> data) {
		this.data = data;
	}

	public boolean isPingCheckUsed() {
		return isPingCheckUsed;
	}

	public void setPingCheckUsed(boolean isPingCheckUsed) {
		this.isPingCheckUsed = isPingCheckUsed;
	}
	public int getPingInterval() {
		return pingInterval;
	}

	public void setPingInterval(int pingInterval) {
		this.pingInterval = pingInterval;
	}
	
	public void setPingInterval(String value) {
		try {
			pingInterval = Integer.parseInt(value);			
		} catch (Exception e) {
			operationTrace("Exception parsing netdevice_thread_interval",e);
		}
	}	
	
	private static final String NET_DEVICE_OPERATION_TRACE = "NetDeviceOperationDebug";
	private static final String NET_DEVICE_OPERATION_EXCEPTION_TRACE = "NetDeviceOperationException";
	private static Logger operationTraceLog = Logger.getLogger(NET_DEVICE_OPERATION_TRACE);
	private static Logger operationExceptionTraceLog = Logger.getLogger(NET_DEVICE_OPERATION_EXCEPTION_TRACE);
	
	private void operationTrace(String message, Throwable e) {
		if (e == null) {
			operationTraceLog.debug(message);
		} else {
			operationExceptionTraceLog.error(message, e);
		}
	}

}