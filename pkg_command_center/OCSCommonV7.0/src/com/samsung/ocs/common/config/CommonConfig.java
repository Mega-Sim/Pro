package com.samsung.ocs.common.config;

import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.constant.OcsConstant;

/**
 * CommonConfig Class, OCS 3.0 for Unified FAB
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

public class CommonConfig {
	private static CommonConfig config = null;
	private int vehicleSocketPort = 5001;
	private int stbReportSocketPort = 6001;
	private String dbUserName;
	private String dbPassWord;
	private String homePath = "";
	private String primaryUrl = null;
	private String secondaryUrl = null;
	private String fileSeparator = File.separator;
	private String remoteHostIpAddress = "127.0.0.1";
	private String hostServiceType = OcsConstant.PRIMARY;
	private int reconnectIntervalMillis = 1000;
	private int logInTimeoutSeconds = 10;
	
    /**
	 * Constructor of CommonConfig class.
	 * 
	 * @exception JDOMException
	 * @exception IOException
	 */
	private CommonConfig() throws JDOMException, IOException {
		homePath = System.getProperty(OcsConstant.HOMEDIR);
		fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		loadModuleConfig();
	}
	
	/**
	 * CommonConfig 클래스 인스턴스를 반환하는 메서드
	 * 
	 * @return 싱글턴인 CommonConfig Instance
	 */
	public synchronized static CommonConfig getInstance() {
		if (config == null) {
			try {
				config = new CommonConfig();
			} catch (JDOMException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return config;
	}
	
	/**
	 * commonConfig.xml 설정파일을 Load.
	 * 
	 * @exception JDOMException
	 * @exception IOException
	 */
	private void loadModuleConfig() throws JDOMException, IOException {
		String configFile = homePath + fileSeparator + "commonConfig.xml";
		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		File f = new File(configFile);
		doc = saxb.build(f);

		Element configElement = doc.getRootElement();

		Element dbConnElement = configElement.getChild(OcsConstant.DBCONNECTION);
		
		primaryUrl = dbConnElement.getChildTextTrim(OcsConstant.PRIMARYURL);
		secondaryUrl = dbConnElement.getChildTextTrim(OcsConstant.SECONDARYURL);
		
		String dbUrl = dbConnElement.getChildTextTrim(OcsConstant.URL);
		if (dbUrl != null && dbUrl.length() > 0) {
			primaryUrl = dbUrl;
			secondaryUrl = dbUrl;
		} 
		dbUserName = dbConnElement.getChildTextTrim(OcsConstant.USERNAME);
		dbPassWord = dbConnElement.getChildTextTrim(OcsConstant.PASSWORD);
		Element reconnectIntervalMillisElement = dbConnElement.getChild(OcsConstant.RECONNECTINTERVALMILLIS);
		if(reconnectIntervalMillisElement != null) {
			try { reconnectIntervalMillis = Integer.parseInt(reconnectIntervalMillisElement.getTextTrim()); } catch (NumberFormatException ignore) { }
		}
		Element logInTimeoutSecondsElement = dbConnElement.getChild(OcsConstant.LOGINTIMEOUTSECONDS);
		if(logInTimeoutSecondsElement != null) {
			try { logInTimeoutSeconds = Integer.parseInt(logInTimeoutSecondsElement.getTextTrim()); } catch (NumberFormatException ignore) { }
		}

		hostServiceType = configElement.getChildTextTrim(OcsConstant.HOSTSERVICETYPE);
		remoteHostIpAddress = configElement.getChildTextTrim(OcsConstant.REMOTEHOSTIPADDRESS);

		String socketPort = configElement.getChildTextTrim(OcsConstant.VEHICLESOCKETPORT);        
		vehicleSocketPort = Integer.parseInt(socketPort);        
		socketPort = configElement.getChildTextTrim(OcsConstant.STBREPORTSOCKETPORT);
		if (socketPort != null) {
			stbReportSocketPort = Integer.parseInt(socketPort);
		}
	}

	/**
	 * commonConfig.xml 설정파일상의 primaryUrl 값을 반환하는 메서드.
	 * @return String primaryUrl
	 */
	public String getPrimaryUrl() {
		return primaryUrl;
	}

	/**
	 * commonConfig.xml 설정파일상의 secondaryUrl 값을 반환하는 메서드.
	 * @return String secondaryUrl
	 */
	public String getSecondaryUrl() {
		return secondaryUrl;
	}

	/**
	 * commonConfig.xml 설정파일상의 dbUserName 값을 반환하는 메서드.
	 * @return String dbUserName
	 */
	public String getDbUserName() {
		return dbUserName;
	}


	/**
	 * commonConfig.xml 설정파일상의 dbPassWord 값을 반환하는 메서드.
	 * @return String dbPassWord
	 */
	public String getDbPassWord() {
		return dbPassWord;
	}
	
	/**
	 * commonConfig.xml 설정파일상의 reconnectIntervalMillis 값을 반환하는 메서드.
	 * @return int reconnectIntervalMillis
	 */
	public int getReconnectIntervalMillis() {
		return reconnectIntervalMillis;
	}
	
	/**
	 * commonConfig.xml 설정파일상의 logInTimeoutSeconds 값을 반환하는 메서드.
	 * @return int logInTimeoutSeconds
	 */
	public int getLogInTimeoutSeconds() {
		return logInTimeoutSeconds;
	}

	/**
	 * commonConfig.xml 설정파일상의 remoteHostIpAddress 값을 반환하는 메서드.
	 * @return String remoteHostIpAddress
	 */
	public String getRemoteHostIpAddress() {
		return remoteHostIpAddress;
	}

	/**
	 * commonConfig.xml 설정파일상의 hostServiceType 값을 반환하는 메서드.
	 * @return String hostServiceType
	 */
	public String getHostServiceType() {
		return hostServiceType;
	}

	/**
	 * commonConfig.xml 설정파일상의 vehiclePort 값을 반환하는 메서드.
	 * @return
	 */
	public int getVehicleSocketPort() {
		return vehicleSocketPort;
	}
	
	/**
	 * commonConfig.xml 설정파일상의 stbcReportPort 값을 반환하는 메서드.
	 * @return
	 */
	public int getSTBReportSocketPort() {
		return stbReportSocketPort;
	}
	
}