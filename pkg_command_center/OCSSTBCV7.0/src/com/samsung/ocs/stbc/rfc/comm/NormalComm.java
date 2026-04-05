/**
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */
package com.samsung.ocs.stbc.rfc.comm;

import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.common.udpu.UDPServer;
import com.samsung.ocs.stbc.manager.RFCDataManager;
import com.samsung.ocs.stbc.rfc.comm.job.NormalCommJob;
import com.samsung.ocs.stbc.rfc.config.RfcConfig;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.NORMAL_COMMTYPE;
import com.samsung.ocs.stbc.rfc.model.EventEntry;
import com.samsung.ocs.stbc.rfc.model.RFCData;
import com.samsung.ocs.stbc.rfc.model.STBData;
import com.samsung.ocs.stbc.rfc.model.commmsg.NormalCommResult;

/**
 * NormalComm Class, OCS 3.0 for Unified FAB
 * 
 * RFC Normal 메시지를 처리하는 클래스
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

public class NormalComm extends CommonComm {
	private UDPServer server = null;
	private Vector<NormalCommResult> recieveList;
	
	private static Logger logger = Logger.getLogger(RfcConstant.NORMALCOMMLOGGER);
	
	private Map<String, STBData> stbMap;

	private RFCDataManager rfcDataManager;
	
	/**
	 * Constructor of NormalComm class.
	 * 
	 * @param config
	 * @param rfcMap
	 * @param stbMap
	 * @param recieveList
	 * @param broadcastAddress
	 * @param sendPort
	 * @param rfcDataManager
	 */
	public NormalComm(RfcConfig config, Map<String, RFCData> rfcMap, Map<String, STBData> stbMap, Vector<NormalCommResult> recieveList, 
			String broadcastAddress, int sendPort, RFCDataManager rfcDataManager) {
		super(config, rfcMap, broadcastAddress, sendPort);
		this.recieveList = recieveList;
		this.rfcDataManager = rfcDataManager;
		this.stbMap = stbMap;
		initialize();
	}
	
	/**
	 * Initialize
	 */
	private void initialize() {
		// udp server 만들어두기
		server = new UDPServer(new NormalCommJob(recieveList , rfcMap, stbMap, rfcDataManager), sendPort);
		// 2014.02.24 by KBS : FoupID 추가 (for A-PJT EDS)
		server.setRecieveBufferSize(350+64);
		server.start();
	}
	
	/**
	 * Send Unicast Message
	 * 
	 * @param rfcId
	 * @param event
	 * @param data
	 * @return
	 */
	public boolean sendUnicast(String rfcId, EventEntry event, byte[] data) {
		RFCData rfc = rfcMap.get(rfcId);
		boolean result = false;
		if (rfc != null ) {
			switch (event.getEventType()) {
				case READ:
					log(String.format("SEND[TO:%s] [READ] [%s] RFCID[%s] STBNO[%d] IDREAD[%s]", rfc.getIpAddress(), event.getKey(), event.getRfcId(), event.getStbNumber(), event.isIdRead()));
					break;
				case READALL: 
					log(String.format("SEND[TO:%s] [READALL] [%s] RFCID[%s] IDREAD[%s]", rfc.getIpAddress(), event.getKey(), event.getRfcId(), event.isIdRead()));
					break;
				case VERIFY:
					log(String.format("SEND[TO:%s] [VERIFY] [%s] RFCID[%s] STBNO[%d] TIMEOUT[%d] CarrierStaus[%s] ", rfc.getIpAddress(), event.getKey(), event.getRfcId(), event.getStbNumber(), event.getTimeOut(), event.hasCarrier()));
					break;
				case STATUS:
				default:
					log(String.format("SEND[TO:%s] [%s] [%s]", rfc.getIpAddress(), event.getEventType(), event.getKey()));
					break;
			}
			UDPSender sender = rfc.getSender();
			result = sender.send(data);
			if (event.getEventType() == NORMAL_COMMTYPE.STATUS) {
				rfc.setLastStatusSendTime(System.currentTimeMillis());
			}
			rfc.setLastSendTime(System.currentTimeMillis());
		}
		return result;
	}
	
	public boolean sendEvent(EventEntry event) {
		if (event != null) {
			return sendUnicast(event.getRfcId(), event, makeMessage(event));
		} else {
			return false;
		}
	}
	
	/**
	 * Make Message
	 * 
	 * @param event
	 * @return
	 */
	private byte[] makeMessage(EventEntry event) {
		byte [] msg = null;
		switch (event.getEventType()) {
			case READ:
				msg = new byte[272];
				makeHeader(event, msg);
				msg[264] = event.getEventType().toByte();
				msg[269] = (byte) event.getStbNumber(); 
				msg[271] = event.isIdRead() ? (byte)1 : (byte)0;
				break;
			case READALL:
				msg = new byte[272];
				makeHeader(event, msg);
				msg[264] = event.getEventType().toByte();
				msg[271] = event.isIdRead() ? (byte)1 : (byte)0;
				break;
			case STATUS:
				msg = new byte[265];
				makeHeader(event, msg);
				msg[264] = event.getEventType().toByte();
				break;
			case VERIFY:
				msg = new byte[273];
				makeHeader(event, msg);
				msg[264] = event.getEventType().toByte();
				msg[269] = (byte) event.getStbNumber(); 
				msg[271] = (byte) event.getTimeOut();
				msg[272] = event.hasCarrier() ? (byte)1 : (byte) 0; 
				break;
		}
		if(msg == null) {
			msg = new byte[1];
		}
		return msg;
	}
	
	private void makeHeader(EventEntry event, byte[] msg) {
		System.arraycopy("90".getBytes(), 0, msg, 0, 2);
		System.arraycopy(event.getRfcIdForMessage().getBytes(), 0, msg, 2, 6);
	}
	
	/**
	 * Close
	 */
	public void close() {
		super.close();
		if (server != null) {
			server.stop();
		}
	}
	
	private void log(String s) {
		logger.debug(s);
	}
	
//	private void log(Throwable w) {
//		logger.debug(w.getMessage(), w);
//	}
}
