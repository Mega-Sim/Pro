package com.samsung.ocs.stbc.rfc.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant;

/**
 * RfcConfig Class, OCS 3.0 for Unified FAB
 * 
 * rfc read 모듈에서 사용할 Configuration 값을 파일로 부터 읽어오는 클래스
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

public class RfcConfig {
	private static RfcConfig config = null;
	
	private String homePath = "";
	private String fileSeparator = File.separator;
	
	private String broadcastAddress = "";
	private int powerCommPort = 9000;
	private int normalCommPort = 9001;

	private String stbcMachineCode = "08";
	private String rfcMachineCode = "90";
	private String stbcMachineId = "STBC  ";
	
	private int multipleReadCount = 10;
	private int readRetryCount = 1;
	private int verifyRetryCount = 1;
	
	private long sendStatusIntervalMillis = 20000L;
	private long rfcTimeoutMillis = 60000L;
	
	private long readTimeout = 3000L;
	private long readAllTimeout = 95000L;
	
	private int readAllRetryCount = 1;
	private long readAllNoResponseTimeoutMillis = 3000L;
	
	private List<String> additionalbroadcastAddressList = new ArrayList<String>();
	
	/**
	 * Constructor of RfcConfig class.
	 * 
	 * @exception JDOMException
	 * @exception IOException
	 */
	private RfcConfig() throws JDOMException, IOException {
		homePath = System.getProperty(OcsConstant.HOMEDIR);
		fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		loadModuleConfig();
		
	}
	
	/**
	 * CommonConfig 클래스 인스턴스를 반환하는 메서드 
	 * @return 싱글턴인 CommonConfig Instance
	 */
	public synchronized static RfcConfig getInstance() {
		if (config == null) {
			try {
				config = new RfcConfig();
			} catch (JDOMException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return config;
	}
	
	/**
	 * Load ModuleConfig
	 * 
	 * @exception JDOMException
	 * @exception IOException
	 */
	private void loadModuleConfig() throws JDOMException, IOException {
		String configFile = homePath + fileSeparator + "stbcConfig.xml";
		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		doc = saxb.build(configFile);

		Element configElement = doc.getRootElement();

		Element rfcReadElement = configElement.getChild(RfcConstant.RFCREAD);
		broadcastAddress = rfcReadElement.getChildTextTrim(RfcConstant.BROADCASTADDRESS);
        
		powerCommPort = Integer.parseInt(rfcReadElement.getChildTextTrim(RfcConstant.POWERCOMMPORT));
		normalCommPort = Integer.parseInt(rfcReadElement.getChildTextTrim(RfcConstant.NORMALCOMMPORT));
		
		stbcMachineCode = rfcReadElement.getChildTextTrim(RfcConstant.STBCMACHINECODE);
		rfcMachineCode = rfcReadElement.getChildTextTrim(RfcConstant.RFCMACHINECODE);
		stbcMachineId = rfcReadElement.getChildTextTrim(RfcConstant.STBCMACHINEID);

		multipleReadCount = Integer.parseInt(rfcReadElement.getChildTextTrim(RfcConstant.MULTIPLEREADCOUNT));
		readRetryCount = Integer.parseInt(rfcReadElement.getChildTextTrim(RfcConstant.READRETRYCOUNT));
		verifyRetryCount = Integer.parseInt(rfcReadElement.getChildTextTrim(RfcConstant.VERIFYRETRYCOUNT));
		
	    sendStatusIntervalMillis = Long.parseLong(rfcReadElement.getChildTextTrim(RfcConstant.SENDSTATUSINTERVALMILLIS));
	    rfcTimeoutMillis = Long.parseLong(rfcReadElement.getChildTextTrim(RfcConstant.RFCTIMEOUTMILLIS));
	    
	    readTimeout = Long.parseLong(rfcReadElement.getChildTextTrim(RfcConstant.READTIMEOUT));
	    readAllTimeout = Long.parseLong(rfcReadElement.getChildTextTrim(RfcConstant.READALLTIMEOUT));
	    
	    readAllRetryCount = Integer.parseInt(rfcReadElement.getChildTextTrim(RfcConstant.READALLRETRYCOUNT));
	    readAllNoResponseTimeoutMillis = Long.parseLong(rfcReadElement.getChildTextTrim(RfcConstant.READALLNORESPONSETIMEOUT));
	    
	    Element addtionalBroadCastAddressList = rfcReadElement.getChild(RfcConstant.ADDITIONALBROADCASTADDRESS);
	    if(addtionalBroadCastAddressList != null) {
	    	List<?> broadcastAddressList = addtionalBroadCastAddressList.getChildren(RfcConstant.BROADCASTADDRESS);
	    	for (Object obj : broadcastAddressList) {
				Element addressElement = (Element) obj;
				String address = addressElement.getTextTrim();
				additionalbroadcastAddressList.add(address);
	    	}
	    }
	}

	public String getBroadcastAddress() {
		return broadcastAddress;
	}

	public int getPowerCommPort() {
		return powerCommPort;
	}

	public int getNormalCommPort() {
		return normalCommPort;
	}

	public String getStbcMachineCode() {
		return stbcMachineCode;
	}

	public String getRfcMachineCode() {
		return rfcMachineCode;
	}

	public String getStbcMachineId() {
		return stbcMachineId;
	}

	public int getMultipleReadCount() {
		return multipleReadCount;
	}

	public int getReadRetryCount() {
		return readRetryCount;
	}
	
	public int getVerifyRetryCount() {
		return verifyRetryCount;
	}

	public long getSendStatusIntervalMillis() {
		return sendStatusIntervalMillis;
	}

	public long getRfcTimeoutMillis() {
		return rfcTimeoutMillis;
	}

	public long getReadTimeout() {
		return readTimeout;
	}

	public long getReadAllTimeout() {
		return readAllTimeout;
	}

	public int getReadAllRetryCount() {
		return readAllRetryCount;
	}

	public long getReadAllNoResponseTimeoutMillis() {
		return readAllNoResponseTimeoutMillis;
	}

	public List<String> getAdditionalbroadcastAddressList() {
		return additionalbroadcastAddressList;
	}
	
	
}