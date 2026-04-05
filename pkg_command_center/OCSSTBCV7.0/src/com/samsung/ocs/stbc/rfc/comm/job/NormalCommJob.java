/**
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */
package com.samsung.ocs.stbc.rfc.comm.job;

import java.net.DatagramPacket;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.udpu.UDPServerJob;
import com.samsung.ocs.stbc.manager.RFCDataManager;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.NORMAL_COMMTYPE;
import com.samsung.ocs.stbc.rfc.model.RFCData;
import com.samsung.ocs.stbc.rfc.model.STBData;
import com.samsung.ocs.stbc.rfc.model.commmsg.NormalCommResult;
import com.samsung.ocs.stbc.rfc.model.commmsg.Read;
import com.samsung.ocs.stbc.rfc.model.commmsg.Read2;
import com.samsung.ocs.stbc.rfc.model.commmsg.ReadAll;
import com.samsung.ocs.stbc.rfc.model.commmsg.ReadAll2;
import com.samsung.ocs.stbc.rfc.model.commmsg.Status;
import com.samsung.ocs.stbc.rfc.model.commmsg.Verify;
import com.samsung.ocs.stbc.rfc.model.commmsg.Verify2;

/**
 * NormalCommJob Class, OCS 3.0 for Unified FAB
 * 
 * RFC로 부터 수신한 정보를 processingList의 Event에 반영해주고, 완료여부를 판단해서 completedList에 넣어주는 로직을 다 넣으면..힘들어 할꺼 같기도 하지만 바로 처리를 해줘야 할꺼같기도 하고.. 한 클래스
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

public class NormalCommJob implements UDPServerJob {
	private static Logger normalCommLogger = Logger.getLogger(RfcConstant.NORMALCOMMLOGGER);
	private Vector<NormalCommResult> recieveList;
	private Map<String, RFCData> rfcMap;
	
	/**
	 * Constructor of NormalCommJob class.
	 * 
	 * @param recieveList
	 * @param rfcMap
	 * @param stbMap
	 * @param rfcDataManager
	 */
	public NormalCommJob(Vector<NormalCommResult> recieveList, Map<String, RFCData> rfcMap, Map<String, STBData> stbMap, RFCDataManager rfcDataManager) {
		this.recieveList = recieveList;
		this.rfcMap = rfcMap;
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.udpu.UDPServerJob#operation(java.net.DatagramPacket)
	 */
	/**
	 * Operation
	 * 
	 * @param packet
	 */
	public void operation(DatagramPacket packet) {
		// 일단 느려질꺼 같으니까 vector에 넣어주기할꺼다. rfcmanager에서 로직처리를 몰자.
		byte[] recieveMessage = packet.getData();
		
	if (recieveMessage[0] != '9' || recieveMessage[1] != '0') {
			return;
		}
		
		NormalCommResult ncr = null;
		int rfcIndex = 0;
		switch (NORMAL_COMMTYPE.toCommType(recieveMessage[264])) {
			case READ:
				ncr = new Read(recieveMessage);
				rfcIndex = ((Read)ncr).getStbNumber();
				break;
			case READ2:
				ncr = new Read2(recieveMessage);
				rfcIndex = ((Read2)ncr).getStbNumber();
				break;
			case READALL:
				ncr = new ReadAll(recieveMessage);
				rfcIndex = ((ReadAll)ncr).getStbNumber();
				break;
			case READALL2:
				ncr = new ReadAll2(recieveMessage);
				rfcIndex = ((ReadAll2)ncr).getStbNumber();
				break;
			case STATUS:
				ncr = new Status(recieveMessage);
				break;
			case VERIFY:
				ncr = new Verify(recieveMessage);
				rfcIndex = ((Verify)ncr).getStbNumber();
				break;
			case VERIFY2:
				ncr = new Verify2(recieveMessage);
				rfcIndex = ((Verify2)ncr).getStbNumber();
				break;
			default : 
				ncr = null;
		}
		if (ncr != null) {
			log(packet.getAddress().getHostAddress(), ncr);
			recieveList.add(ncr);
			String rfcId = ncr.getMachineId().trim();
			RFCData rfc = rfcMap.get(rfcId);
			if (rfc != null) {
				rfc.setLastRecievedTime(System.currentTimeMillis());
				if (rfcIndex > 0) {
					STBData stb = rfc.getStb(rfcIndex);
					if (stb != null) {
						ncr.setCarrierLocId(stb.getCarrierLocId());
					}
				}
				
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.udpu.UDPServerJob#getRelpyMessage()
	 */
	public byte[] getRelpyMessage() {
		return null;
	}
	
	private void log(String sourceIpAddress, NormalCommResult ncr) {
		normalCommLogger.debug(ncr.toLogString(sourceIpAddress));
	}
}
