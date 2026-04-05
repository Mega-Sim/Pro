package com.samsung.ocs.stbc.rfc.comm;

import java.util.Map;

import com.samsung.ocs.common.udpu.UDPServer;
import com.samsung.ocs.stbc.manager.RFCDataManager;
import com.samsung.ocs.stbc.rfc.comm.job.PowerCommJob;
import com.samsung.ocs.stbc.rfc.config.RfcConfig;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant;
import com.samsung.ocs.stbc.rfc.model.RFCData;

/**
 * PowerComm Class, OCS 3.0 for Unified FAB
 * 
 * RFC Power On 관련 메시지를 처리하는 클래스 
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

public class PowerComm extends CommonComm  {
	private byte[] stbcUnicastMessage = new byte[9];
	private byte[] stbcBroadcastMessage = new byte[9];
	private UDPServer server = null;
	
	private RFCDataManager rfcDataManager;
	
	/**
	 * Constructor of PowerComm class.
	 * 
	 * @param rfcMap
	 * @param broadcastAddress
	 * @param sendPort
	 */
	public PowerComm(RfcConfig config, Map<String, RFCData> rfcMap, String broadcastAddress, int sendPort, RFCDataManager rfcDataManager) {
		super(config, rfcMap, broadcastAddress, sendPort);
		this.rfcDataManager = rfcDataManager;
		initialize();
		sendBroadcast(stbcBroadcastMessage);
	}

	/**
	 * Initialize 
	 */
	private void initialize() {
		// stbc message 만들기
		System.arraycopy(RfcConstant.FLAG_UNICAST.getBytes(), 0, stbcUnicastMessage, 0, 1);
		System.arraycopy(config.getStbcMachineCode().getBytes(), 0, stbcUnicastMessage, 1, 2);
		System.arraycopy(config.getStbcMachineId().getBytes(), 0, stbcUnicastMessage, 3, 6);
		
		System.arraycopy(RfcConstant.FLAG_BROADCAST.getBytes(), 0, stbcBroadcastMessage, 0, 1);
		System.arraycopy(config.getStbcMachineCode().getBytes(), 0, stbcBroadcastMessage, 1, 2);
		System.arraycopy(config.getStbcMachineId().getBytes(), 0, stbcBroadcastMessage, 3, 6);
		
		// udp server 만들어두기
		server = new UDPServer(new PowerCommJob(this, rfcDataManager), sendPort);
		server.setRecieveBufferSize(50);
		server.start();
	}

	public byte[] getStbcUnicastMessage() {
		return stbcUnicastMessage;
	}

	public byte[] getStbcBroadcastMessage() {
		return stbcBroadcastMessage;
	}
	
	/* (non-Javadoc)
	 * @see com.samsung.ocs.stbc.rfc.comm.CommonComm#close()
	 */
	/**
	 * Close
	 */
	@Override
	public void close() {
		super.close();
		if (server != null) {
			server.stop();
		}
	}
}
