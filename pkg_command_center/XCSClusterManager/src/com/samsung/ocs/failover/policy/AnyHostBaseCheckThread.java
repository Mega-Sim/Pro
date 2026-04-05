package com.samsung.ocs.failover.policy;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.LocalProcess;

/**
 * AnyHostBaseCheckThread Class, OCS 3.0 for Unified FAB
 * 
 * Primary,Secondary HOST에서 서비스 되는 프로세스를 위한 Policy Thread. 
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

public class AnyHostBaseCheckThread extends AbstractPolicyThread {
	private LocalProcess process;
	private OcsHistoryManager historyManager = null;
	
	/**
	 * Constructor of AnyHostBaseCheckThread class.
	 */
	public AnyHostBaseCheckThread(DBAccessManager dbAccessManager, ClusterState clusterState, LocalProcess process, UDPSender sender, OcsHistoryManager historyManager ) {
		super(500, process, sender);
		this.process = process;
		this.historyManager = historyManager;
		stopThread();
	}

	/**
	 * 
	 */
	@Override
	public String getThreadId() {
		return "AnyHostBaseCheckThread [" + process.getProcessName() + "]";
	}

	/**
	 * 
	 */
	@Override
	protected void initialize() {

	}

	/**
	 * 
	 */
	@Override
	protected void stopProcessing() {

	}

	/**
	 * 
	 */
	@Override
	protected void mainProcessing() {
	}
}
