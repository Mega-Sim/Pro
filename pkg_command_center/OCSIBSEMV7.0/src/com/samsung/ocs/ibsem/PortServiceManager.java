package com.samsung.ocs.ibsem;

import java.util.HashMap;
import java.util.Iterator;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.manager.impl.CarrierLocManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.model.CarrierLoc;

public class PortServiceManager extends AbstractOcsThread {
	private IBSEMManager ibsemManager;
	private CarrierLocManager carrierLocManager;
	private OCSInfoManager ocsInfoManager;
	
	public PortServiceManager(IBSEMManager ibsemManager, IBSEM ibsem,
			CarrierLocManager carrierLocManager, OCSInfoManager ocsInfoManager) {
		super();
		this.ibsemManager = ibsemManager;
		this.carrierLocManager = carrierLocManager;
		this.ocsInfoManager = ocsInfoManager;
	}

	@Override
	public String getThreadId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void initialize() {
		interval = 500;
	}

	@Override
	protected void stopProcessing() {
		
	}

	@Override
	protected void mainProcessing() {
		managePortService();
	}
	
	/**
	 * 2015.02.04 by MYM : 장애 지역 우회 기능 (PortInService/PortOutOfservice 대응)
	 */
	public void managePortService() {
		if (ibsemManager.isReportStarted() && ibsemManager.getIbsem() != null) {
			long startTime = System.nanoTime();
			
			// UserRequest와 Enabled가 불일치한 경우 UserRequest Reset
			// (TRUE,PortInService), (FALSE,PortOutOfService)인 경우는 USERREQUEST Reset
			carrierLocManager.resetUserRequestForMismatchWithEnabled();
			
			// UserRequest 처리(TRUE → PortOutOfService), (FALSE → PortInService)
			HashMap<String, CarrierLoc> requestedPortTable = carrierLocManager.getRequestedPortFromCarrierLocDB();
			int itemCount = requestedPortTable.size();

			if (itemCount > 0) {
				ibsemManager.traceIBSEMMain("[PortService:" + itemCount + "] ");
				StringBuffer notReportlog = new StringBuffer();
				Iterator<CarrierLoc> iter = requestedPortTable.values().iterator();
				int reportCount = 0, notReportCount = 0;
				HashMap<String, Boolean> carrierLocMap = new HashMap<String, Boolean>();
				while (iter.hasNext()) {
					CarrierLoc carrierloc = iter.next();
					if (carrierloc != null) {
						String carrierLocId = carrierloc.getCarrierLocId();
						String requestedType = carrierloc.getUserRequest();
						boolean isEnabled = carrierloc.isEnabled();
						if (isEnabled == false && OcsConstant.PORT_INSERVICE.equals(requestedType)) {
							ibsemManager.getIbsem().sendS6F11Port(OcsConstant.PORT_INSERVICE, carrierLocId);
							isEnabled = true;
							reportCount++;
						} else if (isEnabled == true && OcsConstant.PORT_OUTOFSERVICE.equals(requestedType)) {
							ibsemManager.getIbsem().sendS6F11Port(OcsConstant.PORT_OUTOFSERVICE, carrierLocId);
							isEnabled = false;
							reportCount++;
						} else {
							notReportCount++;
							notReportlog.append(carrierLocId).append(",").append(requestedType).append(",").append(isEnabled).append("/");
						}
						carrierLocMap.put(carrierLocId, isEnabled);
					}
					// 한번에 PortService 보고하는 것을 제한(반송 처리 이벤트 지연 방지)
					if (reportCount >= ocsInfoManager.getDetourPortServiceLimitCount()) {
						break;
					}
				}
				
				carrierLocManager.resetUserRequestFromDB(carrierLocMap);
				carrierLocMap.clear();
				
				StringBuffer log = new StringBuffer();
				log.append("[PortService - Report:").append(reportCount).append("/").append(itemCount);
				log.append(", elapsedTime:").append(((System.nanoTime() - startTime)/1000)/1000).append(" ms").append("]");
				ibsemManager.traceIBSEMMain(log.toString());
				if (notReportCount > 0) {
					ibsemManager.traceIBSEMMain("[PortService - NoteReport:" + notReportCount + ", " + notReportlog.toString());
				}
			}
		}
	}
}
