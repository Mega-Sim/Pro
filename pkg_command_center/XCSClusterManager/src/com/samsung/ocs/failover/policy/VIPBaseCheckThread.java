package com.samsung.ocs.failover.policy;

import com.samsung.ocs.common.config.CommonConfig;
import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.failover.constant.ClusterConstant.EVENT_TYPE;
import com.samsung.ocs.failover.dao.OcsDBStateDAO;
import com.samsung.ocs.failover.dao.OcsHistoryManager;
import com.samsung.ocs.failover.model.ClusterState;
import com.samsung.ocs.failover.model.LocalProcess;
import com.samsung.ocs.failover.model.OcsDBStateVO;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * VIPBaseCheckThread Class, OCS 3.0 for Unified FAB
 * 
 * VIP상태에 따라 Inservice host를 판단하여 
 * Primary,Secondary HOST 중 한곳에서만 서비스 되는 프로세스를 위한 Policy Thread.
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

public class VIPBaseCheckThread extends AbstractPolicyThread  {
	private LocalProcess process;
	private String processName;
	private DBAccessManager dbAccessManager;
	private String serviceType;
	private boolean isPrimary = false;
	private ClusterState clusterState;
	private OcsHistoryManager historyManager = null;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/**
	 * Constructor of VIPBaseCheckThread class.
	 */
	public VIPBaseCheckThread(DBAccessManager dbAccessManager, ClusterState clusterState, LocalProcess process, UDPSender sender, OcsHistoryManager historyManager ) {
		super(500, process, sender);
		this.process = process;
		this.processName = process.getProcessName().toLowerCase();
		this.dbAccessManager = dbAccessManager;
		CommonConfig cc = CommonConfig.getInstance();
		this.serviceType = cc.getHostServiceType();
		this.clusterState = clusterState;
		this.isPrimary = OcsConstant.PRIMARY.equalsIgnoreCase(serviceType);
		this.historyManager = historyManager;
	}
	
	@Override
	public String getThreadId() {
		return "VIPBaseCheckThread [" + process.getProcessName() + "]";
	}

	/**
	 * Initialize
	 */
	@Override
	protected void initialize() {
	}

	/**
	 * Stop Processing
	 */
	@Override
	protected void stopProcessing() {
		
	}

	/**
	 * Main Processing Method
	 */
	@Override
	protected void mainProcessing() {
		
		//15.11.26 LSH
		//Thread 진/출입 시, Log 기록
		long startTime = System.currentTimeMillis();
		log(String.format("VIPBaseCheckThread process start. startTime[%s]millis", sdf.format(new Date(startTime))));
		
		// Step 0.1 내 상태를 체크하고 준비가 될 경우 시작함.
		if (clusterState.isCheckThreadInitialized() == false) {
			try { sleep(1000); } catch (Exception ignore) {}
			log("NOT START : LOCAL CEHCK NOT INITIALIZE.");
			long elapsedTime = System.currentTimeMillis() - startTime;
			log(String.format("VIPBaseCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
			return;
		}
		
		log("VIP INFO : ");
		if (clusterState.isVipAssigned()) {
			OcsDBStateVO vo = OcsDBStateDAO.retrieveProcessState(processName, dbAccessManager);
			if (vo != null) {
				String inServiceHost = vo.getInserviceHost();
				log(String.format("  - VIP Assigned[TRUE]. currentInserviceHost[%s] ", inServiceHost));
				if (serviceType.equalsIgnoreCase(inServiceHost) == false) {
					boolean updateInserviceHost = OcsDBStateDAO.updateInserviceHost(dbAccessManager, isPrimary, OcsConstant.VIAVIP, processName);
					if (updateInserviceHost) {
						remoteUocsDown(processName);
					}
					log(String.format("  - dbUpdate InserviceHost [%s] : RESULT [%s]", serviceType, updateInserviceHost));
					historyManager.addClusterHistory(EVENT_TYPE.AUTOTAKEOVER, processName, isPrimary, String.format("[System Failover] VIP Move Result: [%s]", updateInserviceHost));
				}
			} else {
				log("  - retrieveProcessState IS NULL");
			}
		} else {
			log("  - VIP Assigned[FALSE].");
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("VIPBaseCheckThread process completed. elapsedTime[%d]millis", elapsedTime));
	}
}