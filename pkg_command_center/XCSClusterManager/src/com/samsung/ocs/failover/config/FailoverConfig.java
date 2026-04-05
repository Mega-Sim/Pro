package com.samsung.ocs.failover.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.FAILOVER_POLICY;
import com.samsung.ocs.common.constant.OcsConstant.WATCHDOC_POLICY;
import com.samsung.ocs.failover.constant.ClusterConstant.HEARTBEATTYPE;
import com.samsung.ocs.failover.model.DatabaseConnCheckConfig;
import com.samsung.ocs.failover.model.FileInfoItem;
import com.samsung.ocs.failover.model.FileSizeTraceItem;
import com.samsung.ocs.failover.model.HeartBeat;
import com.samsung.ocs.failover.model.LocalNetCheckConfig;
import com.samsung.ocs.failover.model.LocalProcess;
import com.samsung.ocs.failover.model.NetworkPingCheckConfig;
import com.samsung.ocs.failover.model.PublicNetCheckConfig;

/**
 * FailoverConfig Class, OCS 3.0 for Unified FAB
 * 
 * ClusterManager에서 사용할 Configuration 값을 파일로 부터 읽어오는 클래스
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

public class FailoverConfig {
	private static FailoverConfig config = null;
	
	//configuration variable
	private int deleteLogDay = 7;
	// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화
	private boolean deleteHistoryUse = true;
	private int deleteHistoryDay = 25;
	// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화
	private long deleteHistoryCheckInterval = 3600000;
	private boolean failoverUse = false;
	
	private List<HeartBeat> heartBeatList = new ArrayList<HeartBeat>();
	private Map<String, LocalProcess> localProcessMap = new HashMap<String, LocalProcess>();
	private long heartBeatInterval = 0;
	private int heartBeatListenInterval = 2;
	private int heartBeatTimeoutCount = 4;
	private int heartBeatHistoryInsertCount = 3;
	private int heartBeatDelayInformMillis = 3000;
	private PublicNetCheckConfig publicNetCheckConfig = new PublicNetCheckConfig();
	private LocalNetCheckConfig localNetCheckConfig = new LocalNetCheckConfig();
	private DatabaseConnCheckConfig databaseConnCheckConfig = new DatabaseConnCheckConfig();
	private NetworkPingCheckConfig networkPingCheckConfig = new NetworkPingCheckConfig();
	
	private Map<String, FileSizeTraceItem> fileSizeTraceItemMap = new HashMap<String, FileSizeTraceItem>();
	private long fileSizeTraceInterval = 10000;
	
	private long watchdogInterval = 500;
	
	private String homePath = "";
	private String fileSeparator = File.separator;
	
	private int inserviceRequestTimeoutCount = 20;
	private int remoteInitTimeOutCount = 20;
	private int autoTakeOverCount = 4;
	
	private String directHeartBeatIpaddress = "";
	private int directHeartBeatPort = 0;
	
	private HeartBeat tcpHeartBeat;
	private int tcpClientTimeoutMillis = 2000;
	private int tcpServerTimeoutMillis = 2000;
	
	/**
	 * Constructor of FailoverConfig class.
	 * 
	 * @exception JDOMException
	 * @exception IOException
	 */
	private FailoverConfig() throws JDOMException, IOException {
		homePath = System.getProperty(OcsConstant.HOMEDIR);
		fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		loadModuleConfig();
	}
	
	/**
	 * FailoverConfig 클래스 인스턴스를 반환하는 메서드 
	 * @return 싱글턴인 FailoverConfig Instance
	 */
	public synchronized static FailoverConfig getInstance() {
		if (config == null) {
			try {
				config = new FailoverConfig();
			} catch (JDOMException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return config;
	}
	
	/**
	 * 
	 * @exception JDOMException
	 * @exception IOException
	 */
	private void loadModuleConfig() throws JDOMException, IOException {
		String configFile = homePath + fileSeparator + "failoverConfig.xml";
		SAXBuilder saxb = new SAXBuilder();
		Document doc = null;
		File f = new File(configFile);
		doc = saxb.build(f);

		Element configElement = doc.getRootElement();
		
		// health
		Element heartBeatListElement = configElement.getChild(OcsConstant.HEARTBEATLIST);
		List<?> heartBeatElements = heartBeatListElement.getChildren(OcsConstant.HEARTBEAT);
		for (Object obj : heartBeatElements) {
			Element heartBeatElement = (Element) obj;
			HeartBeat hb = new HeartBeat();
			hb.setIpAddress(heartBeatElement.getChildTextTrim(OcsConstant.IPADDRESS));
			hb.setPort(Integer.parseInt(heartBeatElement.getChildTextTrim(OcsConstant.PORT)));
			Element typeElement = heartBeatElement.getChild("type");
			if(typeElement != null) {
				String type = typeElement.getText();
				if(type != null) {
					hb.setType(HEARTBEATTYPE.toHeartBeatType(type));
				}
			}
			heartBeatList.add(hb);
		}
		heartBeatInterval = Long.parseLong(heartBeatListElement.getChildTextTrim(OcsConstant.INTERVALMILLIS));
		heartBeatListenInterval = Integer.parseInt(heartBeatListElement.getChildTextTrim(OcsConstant.LISTENINTERVALCOUNT));
		heartBeatTimeoutCount = Integer.parseInt(heartBeatListElement.getChildTextTrim(OcsConstant.TIMEOUTCOUNT));
		
		Element historyInsertCountElement = heartBeatListElement.getChild("historyInsertCount");
		if(historyInsertCountElement != null) {
			try {
				heartBeatHistoryInsertCount = Integer.parseInt(historyInsertCountElement.getTextTrim());
			} catch (Exception e) {
				e.printStackTrace();
				heartBeatHistoryInsertCount = heartBeatTimeoutCount/2;
			}
		} else {
			heartBeatHistoryInsertCount = heartBeatTimeoutCount/2;
		}
		
		Element delayInformMillisElement = heartBeatListElement.getChild("delayInformMillis");
		if(delayInformMillisElement != null) {
			try {
				heartBeatDelayInformMillis = Integer.parseInt(delayInformMillisElement.getTextTrim());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		} 
		
		Element publicCheckElement = configElement.getChild(OcsConstant.PUBLICNETCHECK);
		publicNetCheckConfig.setUse("TRUE".equalsIgnoreCase(publicCheckElement.getAttributeValue(OcsConstant.USAGE)));
		publicNetCheckConfig.setGatewayIp(publicCheckElement.getChildTextTrim(OcsConstant.PUBLICNETCHECKGTIP));
		publicNetCheckConfig.setRemoteIp(publicCheckElement.getChildTextTrim(OcsConstant.PUBLICNETCHECKREMOTEIP));
		publicNetCheckConfig.setPingTimeoutMillis(Long.parseLong(publicCheckElement.getChildTextTrim(OcsConstant.PINGTIMEOUTMILLIS)));
		publicNetCheckConfig.setIntervalMillis(Long.parseLong(publicCheckElement.getChildTextTrim(OcsConstant.INTERVALMILLIS)));
		publicNetCheckConfig.setTimeoutCount(Integer.parseInt(publicCheckElement.getChildTextTrim(OcsConstant.TIMEOUTCOUNT)));
		
		
		Element localCheckElement = configElement.getChild(OcsConstant.LOCALNETCHECK);
		localNetCheckConfig.setUse("TRUE".equalsIgnoreCase(localCheckElement.getAttributeValue(OcsConstant.USAGE)));
		localNetCheckConfig.setGatewayIp(localCheckElement.getChildTextTrim(OcsConstant.PUBLICNETCHECKGTIP));
		localNetCheckConfig.setRemoteIp(localCheckElement.getChildTextTrim(OcsConstant.LOCALNETCHECKREMOTEIP));
		localNetCheckConfig.setPingTimeoutMillis(Long.parseLong(localCheckElement.getChildTextTrim(OcsConstant.PINGTIMEOUTMILLIS)));
		localNetCheckConfig.setIntervalMillis(Long.parseLong(localCheckElement.getChildTextTrim(OcsConstant.INTERVALMILLIS)));
		localNetCheckConfig.setTimeoutCount(Integer.parseInt(localCheckElement.getChildTextTrim(OcsConstant.TIMEOUTCOUNT)));
		
		Element networkPingCheckElement = configElement.getChild("networkPingCheck");
		if (networkPingCheckElement != null) {
			networkPingCheckConfig.setUse("TRUE".equalsIgnoreCase(networkPingCheckElement.getAttributeValue(OcsConstant.USAGE)));
			networkPingCheckConfig.setPingTimeoutMillis(Long.parseLong(networkPingCheckElement.getChildTextTrim(OcsConstant.PINGTIMEOUTMILLIS)));
			networkPingCheckConfig.setIntervalMillis(Long.parseLong(networkPingCheckElement.getChildTextTrim(OcsConstant.INTERVALMILLIS)));
			Element ipListElement = networkPingCheckElement.getChild("ipList");
			if (ipListElement != null) {
				List<?> ipAddressElements = ipListElement.getChildren(OcsConstant.IPADDRESS);
				for (Object obj : ipAddressElements) {
					Element ipaddressElement = (Element) obj;
					String ipAddress = ipaddressElement.getTextTrim();
					String name = ipAddress.substring(ipAddress.lastIndexOf(".")+1);
					Attribute nameAttr = ipaddressElement.getAttribute("name");
					if (nameAttr != null) {
						String nameValue = nameAttr.getValue();
						if (nameValue != null && nameValue.length()>0) {
							name = nameValue;
						}
					}
					if(name != null && name.trim().length() > 0 && ipAddress != null && ipAddress.trim().length() > 0) {
						networkPingCheckConfig.getIpListMap().put(name, ipAddress);
					}
				}
			}
			if(networkPingCheckConfig.getIpListMap().size() < 0) {
				networkPingCheckConfig.setUse(false);
			}
		}
		
		Element dbConnCheckElement = configElement.getChild(OcsConstant.DBCONNCHECK);
		databaseConnCheckConfig.setIntervalMillis(Long.parseLong(dbConnCheckElement.getChildTextTrim(OcsConstant.INTERVALMILLIS)));
		databaseConnCheckConfig.setTimeoutCount(Integer.parseInt(dbConnCheckElement.getChildTextTrim(OcsConstant.TIMEOUTCOUNT)));
		
		failoverUse = "TRUE".equalsIgnoreCase(configElement.getChildTextTrim(OcsConstant.FAILOVERUSE));

		String deleteLogDayStr = configElement.getChildTextTrim(OcsConstant.DELETELOGDAY);
		if (deleteLogDayStr != null && deleteLogDayStr.length() > 0) {
			deleteLogDay = Integer.parseInt(deleteLogDayStr);
		}
		
		// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화
		Element deleteHistoryElement = configElement.getChild(OcsConstant.DELETEHISTORY);
		deleteHistoryUse = "TRUE".equalsIgnoreCase(deleteHistoryElement.getAttributeValue(OcsConstant.USAGE));
		String deleteHistoryDayStr = deleteHistoryElement.getChildTextTrim(OcsConstant.DELETEHISTORYDAY);
		if (deleteHistoryDayStr != null && deleteHistoryDayStr.length() > 0) {
			deleteHistoryDay = Integer.parseInt(deleteHistoryDayStr);
		}
		String deleteHistoryCheckIntervalStr = deleteHistoryElement.getChildTextTrim(OcsConstant.DELETEHISTORYCHECKINTERVAL);
		if (deleteHistoryCheckIntervalStr != null && deleteHistoryCheckIntervalStr.length() > 0) {
			try {
				deleteHistoryCheckInterval = Long.parseLong(deleteHistoryCheckIntervalStr) * 1000;
				if (deleteHistoryCheckInterval < 60000) deleteHistoryCheckInterval = 60000;
				else if (deleteHistoryCheckInterval > 86400000) deleteHistoryCheckInterval = 86400000;
			} catch (Exception e) {
				deleteHistoryCheckInterval = 3600000;
				e.printStackTrace();
			}
		}
		
		watchdogInterval = Long.parseLong(configElement.getChildTextTrim(OcsConstant.WATCHDOGINTERVAL));
		remoteInitTimeOutCount = Integer.parseInt(configElement.getChildTextTrim(OcsConstant.REMOTEINITTIMEOUTCOUNT));
		autoTakeOverCount = Integer.parseInt(configElement.getChildTextTrim(OcsConstant.AUTOTAKEOVERCOUNT));
		inserviceRequestTimeoutCount = Integer.parseInt(configElement.getChildTextTrim(OcsConstant.INSERVICEREQUESTTIMEOUTCOUNT));
		
		Element fileSizeTraceIntervalMillisElement = configElement.getChild("fileSizeTraceIntervalMillis");
		if(fileSizeTraceIntervalMillisElement != null) {
			fileSizeTraceInterval = Long.parseLong(fileSizeTraceIntervalMillisElement.getTextTrim());
		}

		Element localProcessListElement = configElement.getChild(OcsConstant.LOCALPROCESSLIST);
		List<?> localProcessElements = localProcessListElement.getChildren();
		for (Object obj : localProcessElements) {
			Element localProcessElement = (Element) obj;
			String processName = localProcessElement.getName().toLowerCase();
			LocalProcess lp = new LocalProcess(processName);
			lp.setFailoverUse(OcsConstant.TRUE.equalsIgnoreCase(localProcessElement.getAttributeValue(OcsConstant.FAILOVERUSE)));
			Element watchDogElemet = localProcessElement.getChild(OcsConstant.WATCHDOGCONTROL);
			lp.setWatchdogRunControlUse(OcsConstant.TRUE.equalsIgnoreCase(watchDogElemet.getChildTextTrim(OcsConstant.RUN)));
			lp.setWatchdogPolicy(WATCHDOC_POLICY.toWatchdogPolicy(watchDogElemet.getChildTextTrim(OcsConstant.POLICY)));
			lp.setPolicy(FAILOVER_POLICY.toFailoverPolicy(localProcessElement.getChildTextTrim(OcsConstant.FAILOVERPOLICY)));
			
			Element netCheckUseage = localProcessElement.getChild(OcsConstant.NETCHECKUSAGE);
			if (netCheckUseage != null) {
				lp.setPublicNetCheckUse(OcsConstant.TRUE.equalsIgnoreCase(netCheckUseage.getChildTextTrim(OcsConstant.PUBLIC)));
				lp.setLocalNetCheckUse(OcsConstant.TRUE.equalsIgnoreCase(netCheckUseage.getChildTextTrim(OcsConstant.LOCAL)));
			}
			localProcessMap.put(processName, lp);
			
			Element fileSizeTraceElement = localProcessElement.getChild("fileSizeTrace");
			if(fileSizeTraceElement != null) {
				if("TRUE".equalsIgnoreCase(fileSizeTraceElement.getAttributeValue("usage"))) {
					List<FileInfoItem> infoItemList = new ArrayList<FileInfoItem>();
					List<?> fileNamePrefixElements = fileSizeTraceElement.getChildren("fileNamePrefix");
					if(fileNamePrefixElements != null && fileNamePrefixElements.size() > 0) {
						for(Object o : fileNamePrefixElements) {
							Element fileNamePrefixElement = (Element) o;
							String filePath = fileNamePrefixElement.getAttributeValue("filePath");
							String filePrefix = fileNamePrefixElement.getText();
							if(filePath != null && filePath.trim().length() > 0 && filePrefix != null && filePrefix.trim().length() > 0) {
								infoItemList.add(new FileInfoItem(filePath, filePrefix));
							}
						}
						if(infoItemList.size() > 0) {
							FileSizeTraceItem item = new FileSizeTraceItem(processName);
							item.setFileInfoList(infoItemList);
							fileSizeTraceItemMap.put(processName, item);
						}
					}
				}
			}
		}
		Element directHeartBeatElement = configElement.getChild(OcsConstant.DIRECTHEARTBEAT);
		directHeartBeatIpaddress = directHeartBeatElement.getChildTextTrim(OcsConstant.IPADDRESS);
		directHeartBeatPort= Integer.parseInt(directHeartBeatElement.getChildTextTrim(OcsConstant.PORT));
		
		Element tcpHeartBeatElement = configElement.getChild("tcpHeartBeat");
		if(tcpHeartBeatElement != null) {
			tcpHeartBeat = new HeartBeat();
			tcpHeartBeat.setIpAddress(tcpHeartBeatElement.getChildTextTrim(OcsConstant.IPADDRESS));
			tcpHeartBeat.setPort(Integer.parseInt(tcpHeartBeatElement.getChildTextTrim(OcsConstant.PORT)));
			
			tcpClientTimeoutMillis = Integer.parseInt(tcpHeartBeatElement.getChildTextTrim("clientTimeoutMillis"));
			tcpServerTimeoutMillis = Integer.parseInt(tcpHeartBeatElement.getChildTextTrim("serverTimeoutMillis"));
		}
	}

	/**
	 * failoverConfig.xml 설정파일상의 deleteLogDay 값을 반환하는 메서드.
	 * @return int deleteLogDay  
	 */
	public int getDeleteLogDay() {
		return deleteLogDay;
	}
	
	/**
	 * failoverConfig.xml 설정파일상의 deleteHistoryUse 값을 반환하는 메서드.
	 * @return deleteHistoryDay
	 */
	public boolean isDeleteHistoryUse() {
		return deleteHistoryUse;
	}
	
	/**
	 * failoverConfig.xml 설정파일상의 deleteHistoryDay 값을 반환하는 메서드.
	 * @return deleteHistoryDay
	 */
	public int getDeleteHistoryDay() {
		return deleteHistoryDay;
	}
	
	/**
	 * failoverConfig.xml 설정파일상의 deleteHistoryCheckInterval 값을 반환하는 메서드.
	 * @return deleteHistoryDay
	 */
	public long getDeleteHistoryCheckInterval() {
		return deleteHistoryCheckInterval;
	}

	/**
	 * failoverConfig.xml 설정파일상의 failoverUse 값을 반환하는 메서드.
	 * @return boolean failoverUse
	 */
	public boolean isFailoverUse() {
		return failoverUse;
	}
	
	/**
	 * failoverConfig.xml 설정파일상의 heartBeatList를 반환하는 메서드.
	 * @return List<HeartBeat> heartBeatList
	 */
	public List<HeartBeat> getHeartBeatList() {
		return heartBeatList;
	}

	/**
	 * failoverConfig.xml 설정파일상의 publicNetCheckConfig 값을 반환하는 메서드. 
	 * @return PublicNetCheckConfig publicNetCheckConfig
	 */
	public PublicNetCheckConfig getPublicNetCheckConfig() {
		return publicNetCheckConfig;
	}

	/**
	 * failoverConfig.xml 설정파일상의 localNetCheckConfig 값을 반환하는 메서드. 
	 * @return LocalNetCheckConfig localNetCheckConfig
	 */
	public LocalNetCheckConfig getLocalNetCheckConfig() {
		return localNetCheckConfig;
	}

	/**
	 * failoverConfig.xml 설정파일상의 heartBeatInterval 값을 반환하는 메서드. 
	 * @return long heartBeatInterval
	 */
	public long getHeartBeatInterval() {
		return heartBeatInterval;
	}

	/**
	 * failoverConfig.xml 설정파일상의 heartBeatListenInterval 값을 반환하는 메서드. 
	 * @return int heartBeatListenInterval
	 */
	public int getHeartBeatListenInterval() {
		return heartBeatListenInterval;
	}
	
	/**
	 * failoverConfig.xml 설정파일상의 heartBeatDelayInformMillis 값을 반환하는 메서드. 
	 * @return int heartBeatDelayInformMillis
	 */
	public int getHeartBeatDelayInformMillis() {
		return heartBeatDelayInformMillis;
	}

	/**
	 * failoverConfig.xml 설정파일상의 databaseConnCheckConfig 값을 반환하는 메서드. 
	 * @return DatabaseConnCheckConfig databaseConnCheckConfig
	 */
	public DatabaseConnCheckConfig getDatabaseConnCheckConfig() {
		return databaseConnCheckConfig;
	}

	/**
	 * failoverConfig.xml 설정파일상의 localProcessMap 값을 반환하는 메서드. 
	 * @return Map<String, LocalProcess> localProcessMap
	 */
	public Map<String, LocalProcess> getLocalProcessMap() {
		return localProcessMap;
	}

	/**
	 * failoverConfig.xml 설정파일상의 watchdogInterval 값을 반환하는 메서드. 
	 * @return long watchdogInterval
	 */
	public long getWatchdogInterval() {
		return watchdogInterval;
	}

	/**
	 * failoverConfig.xml 설정파일상의 remoteInitTimeOutCount 값을 반환하는 메서드. 
	 * @return int remoteInitTimeOutCount
	 */
	public int getRemoteInitTimeOutCount() {
		return remoteInitTimeOutCount;
	}

	/**
	 * failoverConfig.xml 설정파일상의 autoTakeOverCount 값을 반환하는 메서드. 
	 * @return int autoTakeOverCount
	 */
	public int getAutoTakeOverCount() {
		return autoTakeOverCount;
	}

	/**
	 * failoverConfig.xml 설정파일상의 inserviceRequestTimeoutCount 값을 반환하는 메서드. 
	 * @return int inserviceRequestTimeoutCount
	 */
	public int getInserviceRequestTimeoutCount() {
		return inserviceRequestTimeoutCount;
	}

	/**
	 * failoverConfig.xml 설정파일상의 heartBeatTimeoutCount 값을 반환하는 메서드. 
	 * @return int heartBeatTimeoutCount
	 */
	public int getHeartBeatTimeoutCount() {
		return heartBeatTimeoutCount;
	}

	/**
	 * failoverConfig.xml 설정파일상의 directHeartBeatIpaddress 값을 반환하는 메서드.
	 * @return
	 */
	public String getDirectHeartBeatIpaddress() {
		return directHeartBeatIpaddress;
	}

	/**
	 * failoverConfig.xml 설정파일상의 directHeartBeatPort 값을 반환하는 메서드.
	 * @return
	 */
	public int getDirectHeartBeatPort() {
		return directHeartBeatPort;
	}

	/**
	 * failoverConfig.xml 설정파일상의 networkPingCheckConfig 값을 반환하는 메서드.
	 * @return
	 */
	public NetworkPingCheckConfig getNetworkPingCheckConfig() {
		return networkPingCheckConfig;
	}

	/**
	 * failoverConfig.xml 설정파일상의 fileSizeTraceItem중에 usage가 True인 Item들을 반환하는 메서드.
	 * @return
	 */
	public Map<String, FileSizeTraceItem> getFileSizeTraceItemMap() {
		return fileSizeTraceItemMap;
	}

	/**
	 * failoverConfig.xml 설정파일상의 fileSizeTraceIntervalMillis 값을 반환하는 메서드.
	 * @return
	 */
	public long getFileSizeTraceInterval() {
		return fileSizeTraceInterval;
	}

	/**
	 * failoverConfig.xml 설정파일상의 HeartBeat historyInsertCount 값을 반환하는 메서드.
	 * @return
	 */
	public int getheartBeatHistoryInsertCount() {
		return heartBeatHistoryInsertCount;
	}

	/**
	 * failoverConfig.xml 설정파일상의 tcpHeartBeat을 반환하는 메서드.
	 * @return 
	 */
	public HeartBeat getTcpHeartBeat() {
		return tcpHeartBeat;
	}

	/**
	 * failoverConfig.xml 설정파일상의 tcpClientTimeoutMillis을 반환하는 메서드.
	 * @return 
	 */
	public int getTcpClientTimeoutMillis() {
		return tcpClientTimeoutMillis;
	}

	/**
	 * failoverConfig.xml 설정파일상의 tcpServerTimeoutMillis을 반환하는 메서드.
	 * @return 
	 */
	public int getTcpServerTimeoutMillis() {
		return tcpServerTimeoutMillis;
	}
	
	/**
	 * STD Out 을 stdout.log 파일로 남기게 하는 메서드
	 * @return 
	 */
	public void writeStdOut() {
		String outFile = homePath + fileSeparator + "log" + fileSeparator + "stdout.log";
		
		String dataTime = getDataTime();
		File out = new File(outFile);
		if(out.exists()) {
			File out2 = new File(outFile+"_"+dataTime);
			out.renameTo(out2);
		}
		
		try {
			FileOutputStream ofos = new FileOutputStream(outFile);
			System.setOut(new PrintStream(ofos));
		} catch (Exception ignore) {
		}
	}
	
	/**
	 * STD Err 을 stderr.log 파일로 남기게 하는 메서드
	 * @return 
	 */
	public void writeStdErr() {
		String errFile = homePath + fileSeparator + "log" + fileSeparator + "stderr.log";
		
		String dataTime = getDataTime();
		File err = new File(errFile);
		if(err.exists()) {
			File err2 = new File(errFile+"_"+dataTime);
			err.renameTo(err2);
		}
		
		try {
			FileOutputStream efos = new FileOutputStream(errFile);
			System.setErr(new PrintStream(efos));
		} catch (Exception ignore) {
		}
	}
	
	private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private static String getDataTime() {
		Date dt = new Date();
		return sdf2.format(dt);
	}
	
}