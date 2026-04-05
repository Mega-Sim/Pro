package com.samsung.ocs.unitdevice;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.unitdevice.model.NetworkDevice;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2006</p>
 * <p>Company: </p>
 * @author 2010.06.01 by KYK : 강연국
 * @version 1.0
 * DB NETWORKDEVICE 테이블에 있는 정보를 관리하는 매니저
 */

public class NetDeviceManager
{
	DBAccessManager m_DBAccessManager = null;
	private Hashtable m_htNetDeviceTable = new Hashtable();
	private Vector m_vtNetDeviceList = new Vector();
	private boolean m_bNetDeviceCheckUsage = false;
	
	private static final String NETDEVICE_MANAGER_TRACE = "NetDeviceManagerDebug";
	private static Logger netDeviceManagerTraceLog = Logger.getLogger(NETDEVICE_MANAGER_TRACE);

	public NetDeviceManager()
	{
	}

	public void SetAccessManager(DBAccessManager DBAccessManager)
	{
		m_DBAccessManager = DBAccessManager;

		UploadNetDeviceFromDB();
		UploadNetDeviceCheckUsageFromDB();
	}

	// 2010.06.01 by KYK :
	/**
	 * DB로부터 NetworkDevice를 쿼리해와서 메모리에 업로드 한다.
	 */
	public boolean UploadNetDeviceFromDB()
	{
		String strUnitID = "";
		String strIPAddress = "";
		boolean bEnabled = false;
		String strStatus = "";
		String strType = "";

		String strSql = "SELECT * FROM NETWORKDEVICE ORDER BY TYPE,UNITID";
		ResultSet rs = null;

		Vector vtRemoveNetDeviceList = new Vector(m_vtNetDeviceList);

		try
		{
			rs = m_DBAccessManager.GetRecord(strSql);
			if (rs != null)
			{
				while (rs.next())
				{
					strUnitID = rs.getString("UNITID");
					strIPAddress = MakeString(rs.getString("IPADDRESS"));
					bEnabled = MakeString(rs.getString("ENABLED")).equals("TRUE");
					strStatus = MakeString(rs.getString("STATUS"));
					strType = MakeString(rs.getString("TYPE"));

					//          m_htNetDeviceTable.put(strUnitID + ".UNITID", strUnitID);
					//          m_htNetDeviceTable.put(strUnitID + ".IPADDRESS", strIPAddress);
					//          m_htNetDeviceTable.put(strUnitID + ".ENABLED", new Boolean(bEnabled));
					//          m_htNetDeviceTable.put(strUnitID + ".STATUS", strStatus);
					//          m_htNetDeviceTable.put(strUnitID + ".TYPE", strType);
					// 2011.02.28 by LWG [Backup HID 관련]
					NetworkDevice nd = (NetworkDevice) m_htNetDeviceTable.get(strUnitID);
					if(nd == null) {
						nd = new NetworkDevice(strUnitID);
						m_htNetDeviceTable.put(strUnitID, nd);
					}
					nd.setDetailInfo(strIPAddress, new Boolean(bEnabled).booleanValue(), strStatus, strType);


					if(m_vtNetDeviceList.contains(strUnitID) == false)
						m_vtNetDeviceList.add(strUnitID);

					vtRemoveNetDeviceList.remove(strUnitID);
				}

				// DB에서 삭제된 Unit를 메모리에서 제거
				for(int i=0; i<vtRemoveNetDeviceList.size(); i++)
				{
					strUnitID = (String)vtRemoveNetDeviceList.get(i);
					m_vtNetDeviceList.remove(strUnitID);
					// 2011.02.28 by LWG [Backup HID 관련]
					m_htNetDeviceTable.remove(strUnitID);
					//          m_htNetDeviceTable.remove(strUnitID + ".UNITID");
					//          m_htNetDeviceTable.remove(strUnitID + ".IPADDRESS");
					//          m_htNetDeviceTable.remove(strUnitID + ".ENABLED");
					//          m_htNetDeviceTable.remove(strUnitID + ".STATUS");
					//          m_htNetDeviceTable.remove(strUnitID + ".TYPE");
				}
			}
		}
		catch (SQLException e)
		{
			netDeviceManagerTrace("DBException UploadSubDeviceFromDB", e);
		}
		finally
		{
			if (rs != null)
			{
				m_DBAccessManager.CloseRecord(rs);
			}
		}

		return true;
	}

	// 2011.02.28 by LWG [Backup HID 관련]
	public Hashtable getNetDeviceTable() {
		return this.m_htNetDeviceTable;
	}

	/**
	 * [NetworkDevice] : NetDeviceCheckUsage 를 DB UOCSInfo 에서 쿼리해 온다. 
	 * 쿼리결과 (YES/NO)에 따라 m_bNetDeviceCheckUsage = TRUE / FALSE 
	 * @return boolean
	 * @version 2010.06.01 by KYK +
	 */
	public void UploadNetDeviceCheckUsageFromDB()
	{
		// DB에서 'NETDEVICE_LIMIT_PER_THREAD' 값 가져오기 : OCSInfo
		String strSql =
			"SELECT * FROM OCSINFO WHERE NAME='NETDEVICE_CHECK_USAGE'";
		ResultSet rs = null;
		String strValue = null;

		try
		{
			rs = m_DBAccessManager.GetRecord(strSql);
			if (rs != null)
			{
				while (rs.next())
				{
					strValue = rs.getString("VALUE");
				}			
			}
		}
		catch (Exception e)
		{
			netDeviceManagerTrace("DBException NetDeviceCheckUsage", e);
		}
		finally
		{
			if (rs != null)
			{
				m_DBAccessManager.CloseRecord(rs);
				rs = null;
			}
		}	  

		if ("YES".equals(strValue))
			m_bNetDeviceCheckUsage = true;
		else
			m_bNetDeviceCheckUsage = false;

	}
	/**
	 * [NetworkDevice] : NetDeviceCheckUsage
	 * @return boolean
	 * @version 2010.06.01 by KYK +
	 */  
	public boolean getNetDeviceCheckUsage()
	{
		return m_bNetDeviceCheckUsage;
	}


	public int GetNameList(Vector pNameList)
	{
		for (int i = 0; i < m_vtNetDeviceList.size(); i++)
		{
			pNameList.add((String) m_vtNetDeviceList.get(i));
		}
		return pNameList.size();
	}

	// 2011.02.28 by LWG [Backup HID 관련]
	/*public boolean CheckName(String strName)
  {
    if (m_htNetDeviceTable.get(strName) == null)
      return false;
    else
      return true;
  }

  public boolean GetBoolean(String strName)
  {
    Boolean bValue = (Boolean) m_htNetDeviceTable.get(strName);
    if (bValue == null)
      return false;
    else
      return bValue.booleanValue();
  }

  public String GetString(String strName)
  {
    String strValue = (String) m_htNetDeviceTable.get(strName);
    if (strValue == null)
      return "";
    else
      return strValue;
  }

  public int GetInteger(String strName)
  {
    Integer nValue = (Integer) m_htNetDeviceTable.get(strName);
    if (nValue == null)
      return 0;
    else
      return nValue.intValue();
  }*/

	private String MakeString(String strValue)
	{
		if (strValue == null)
			return "";
		else
			return strValue;
	}
	
	private void netDeviceManagerTrace(String message, Throwable e) {
		netDeviceManagerTraceLog.error(message, e);
	}

} // End of NetDeviceManager ------------------------------------------
