package com.samsung.ocs.ziplogs.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.ziplogs.constant.ZipLogsConstant;

/**
 * CommonConfig Class, OCS 3.0 for Unified FAB
 * 
 * @author Byoungsoo.Kim
 * 
 * @date   2014. 7. 01.
 * @version 3.0
 * 
 * Copyright 2014 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class ZipLogsConfig {

	private static String fileSeparator = File.separator;
	private static String homePath = "";
	private static String backupDir = "./backup";
	private static ZipLogsConfig config = null;

	// ziplogs.xml
	private static List<String> ocsModuleList = null;
	private static List<String> runTimeList = null;
	private static int sleepTime = 0;
	private static int zipTime = 0;
	private static int delTime = 0;
	private static long zipLimit = 0;
	
	private ZipLogsConfig() throws Exception {
		homePath = System.getProperty(OcsConstant.HOMEDIR);
		fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
	}
	
	/**
	 * ZipLogsConfig 클래스 인스턴스를 반환하는 메서드
	 * 
	 * @return 싱글턴인 CommonConfig Instance
	 */
	public synchronized static ZipLogsConfig getInstance() {
		if (config == null) {
			try {
				config = new ZipLogsConfig();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return config;
	}
	
	/**
	 * ziplogs.xml 설정파일을 Load.
	 * 
	 */
	public void loadModuleConfig() throws Exception {
		ocsModuleList = new ArrayList<String>();
		runTimeList = new ArrayList<String>();
		
		String configFile = homePath + fileSeparator + "ziplogs.xml";
		File xmlFile = new File(configFile);
		if (xmlFile.exists()) {
			SAXBuilder saxb = new SAXBuilder();
			Document doc = null;
			doc = saxb.build(xmlFile);

			Element root = doc.getRootElement();

			traceZipLogsMain("Load config file.");
			int i = 1;
			List<Element> moduleList = root.getChildren(ZipLogsConstant.OCSMODULE);
			for (Element element: moduleList) {
				String path = element.getChildTextTrim(ZipLogsConstant.PATH);
				if (path.startsWith("$")) {
					String envKey = path.substring(1, path.indexOf(fileSeparator));
					String envValue = System.getenv(envKey);
					if (envValue != null && envValue.length() >= 0) {
						String replacedModule = path.replace(envKey, envValue);
						ocsModuleList.add(replacedModule.substring(1, replacedModule.length()));
						traceZipLogsMain("    OCS Module Path[" + i + "] : " + replacedModule.subSequence(1, replacedModule.length()));
						i++;
					} else {
						traceZipLogsMain("    OCS Module Path[" + path + "] does not exist.");
					}
				} else {
					ocsModuleList.add(path);
					traceZipLogsMain("    OCS Module Path[" + i + "] : " + path);
					i++;
				}
			}

			int j = 1;
			List<Element> timeList = root.getChildren(ZipLogsConstant.RUNTIME);
			for (Element element: timeList) {
				String time = element.getChildTextTrim(ZipLogsConstant.TIME);
				runTimeList.add(time);
				traceZipLogsMain("    Run Time[" + j + "] : " + time);
				j++;
			}

			String tmp = "";

			tmp = root.getChildTextTrim(ZipLogsConstant.SLEEPTIME).substring(1);
			sleepTime = Integer.parseInt(tmp);
			traceZipLogsMain("    Sleep Interval : " + sleepTime);

			tmp =  root.getChildTextTrim(ZipLogsConstant.ZIPTIME).substring(1);
			zipTime = Integer.parseInt(tmp);
			if(root.getChildTextTrim(ZipLogsConstant.ZIPTIME).startsWith("D")) {
				zipTime = zipTime * 24; // Day to 24 Hours
			}
			traceZipLogsMain("    Zip Time : " + zipTime);

			tmp = root.getChildTextTrim(ZipLogsConstant.DELTIME).substring(1);
			delTime = Integer.parseInt(tmp);
			if(root.getChildTextTrim(ZipLogsConstant.DELTIME).startsWith("D")) {
				delTime = delTime * 24; // Day to 24 Hours
			}
			traceZipLogsMain("    Delete Time : " + delTime);

			tmp = root.getChildTextTrim(ZipLogsConstant.ZIPLIMIT);
			zipLimit = Integer.parseInt(tmp.substring(0, tmp.length()-1));
			traceZipLogsMain("    Zip Limit : " + zipLimit);
		} else {
			traceZipLogsMain("Config file[" + configFile + "] does not exist.");
		}

		File tmpDir = new File(backupDir);
		if (tmpDir.exists() == false) {
			tmpDir.mkdir();
		}
	}
	
	public List<String> getOcsModuleList() {
		return ocsModuleList;
	}

	public List<String> getRunTimeList() {
		return runTimeList;
	}

	public int getSleepTime() {
		return sleepTime;
	}
	
	public int getZipTime() {
		return zipTime;
	}

	public int getDelTime() {
		return delTime;
	}

	public long getZipLimit() {
		return zipLimit;
	}
	
	private static Logger zipLogsMainLog = Logger.getLogger(ZipLogsConstant.ZIPLOGS_MAIN);
	private static Logger zipLogsExceptionLog = Logger.getLogger(ZipLogsConstant.ZIPLOGS_EXCEPTION);
	
	public void traceZipLogsMain(String message) {
		zipLogsMainLog.debug(message);
	}

	public void traceZipLogsException(String message, Throwable e) {
		zipLogsExceptionLog.error(message, e);
	}

}