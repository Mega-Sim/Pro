package com.samsung.ocs.failovercomm.config;

import java.io.File;
import java.io.IOException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.samsung.ocs.common.OCSMain;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.FAILOVER_POLICY;

/**
 * ModuleConfig Class, OCS 3.0 for Unified FAB
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

public class ModuleConfig {
	private OCSMain main;
	
	//configuration variable
	private boolean failoverUse = false;
	private int autoTakeOverTimeoutCount = 3;
	
	private String homePath = "";
	private String fileSeparator = File.separator;
	
	private FAILOVER_POLICY failoverPolicy = FAILOVER_POLICY.NETDBBASE;
	
	/**
	 * Constructor of ModuleConfig class.
	 * 
	 * @exception JDOMException
	 * @exception IOException
	 */
	public ModuleConfig(OCSMain main) throws JDOMException, IOException {
		this.main = main;
		homePath = System.getProperty(OcsConstant.HOMEDIR);
		fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		loadModuleConfig();
		
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
		
		autoTakeOverTimeoutCount = Integer.parseInt(configElement.getChildTextTrim(OcsConstant.UNKNOWNWAITCOUNT));

		Element localProcessListElement = configElement.getChild(OcsConstant.LOCALPROCESSLIST);
		Element localProcessElement = localProcessListElement.getChild(main.getModuleName().toLowerCase());
		if (localProcessElement != null) {
			failoverUse = "TRUE".equalsIgnoreCase(localProcessElement.getAttributeValue(OcsConstant.FAILOVERUSE));
			failoverPolicy = FAILOVER_POLICY.toFailoverPolicy(localProcessElement.getChildTextTrim(OcsConstant.FAILOVERPOLICY));
		}
		
	}

	/**
	 * 
	 * @return
	 */
	public boolean isFailoverUse() {
		return failoverUse;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getAutoTakeOverTimeoutCount() {
		return autoTakeOverTimeoutCount;
	}

	/**
	 * 
	 * @return
	 */
	public FAILOVER_POLICY getFailoverPolicy() {
		return failoverPolicy;
	}
}