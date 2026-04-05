package com.samsung.ocs.common;

import com.samsung.ocs.common.constant.OcsConstant.INIT_STATE;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;

/**
 * OCSMain Class, OCS 3.0 for Unified FAB
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

public interface OCSMain {
	
	/**
	 * make module OUTOFSERVICE
	 * @return result boolean
	 */
	public boolean deactivate();
	
	/**
	 * make module INSERVICE
	 * @return result boolean
	 */
	public boolean activate();
	
	/**
	 * return moduleName in String
	 * @return moduleName String
	 */
	public String getModuleName();
	
	/**
	 * return ModuleState in MODULE_STATE
	 * @return moduleState MODULE_STATE
	 */
	public MODULE_STATE getModuleState();
	
	/**
	 * return ModuleInitState in INIT_STATE
	 * @return moduleInitState INIT_STATE
	 */
	public INIT_STATE getModuleInitState();
	
	/**
	 * return versionInfo in String
	 * @return versionInfo String
	 */
	public String getVersion();
	
	/**
	 * return buildId in String
	 * @return buildId String
	 */
	public String getBuildId();
	
	/**
	 * return includeInfo in String
	 * @return includeInfo String
	 */
	public String getIncludeInfo();
}
