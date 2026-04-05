package com.samsung.ocs.stbc.rfc.comm.job;

import java.net.DatagramPacket;
import java.util.Map;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.common.udpu.UDPServerJob;
import com.samsung.ocs.stbc.manager.RFCDataManager;
import com.samsung.ocs.stbc.rfc.comm.PowerComm;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant;
import com.samsung.ocs.stbc.rfc.model.RFCData;

/**
 * PowerCommJob Class, OCS 3.0 for Unified FAB
 * 
 * Power Comm UDP 서버가 할일!
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

public class PowerCommJob implements UDPServerJob {
	private static Logger powerCommLogger = Logger.getLogger(RfcConstant.POWERCOMMLOGGER);
	private static Logger rfcLogger = Logger.getLogger(RfcConstant.RFCMANAGERLOGGER);

	private PowerComm comm;
	private Map<String, RFCData> rfcMap;
	private RFCDataManager rfcDataManager;
	
	/**
	 * Constructor of PowerCommJob class.
	 * 
	 * @param comm
	 * @param rfcDataManager
	 */
	public PowerCommJob(PowerComm comm, RFCDataManager rfcDataManager) {
		this.comm = comm;
		this.rfcDataManager = rfcDataManager;
		rfcMap = comm.getRfcMap();
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
		byte[] recieveMessage = packet.getData();

		//System.out.println(new String(recieveMessage));
		
		// RFC로부터 오는게 아니면 버리자.
		if (recieveMessage[1] != '9' && recieveMessage[2] != '0') {
			return;
		}
		
		String sourceAddress = packet.getAddress().getHostAddress();
		
		log(String.format("RECV[FROM:%s] : [%s]", sourceAddress,  new String(recieveMessage)));
		
		if (recieveMessage[0] == '0') {
			// rfc power on notice = register RFC and send response
			registerRFC(recieveMessage, sourceAddress );
			byte[] machineIdbyte = new byte[6];
			System.arraycopy(recieveMessage, 3, machineIdbyte, 0, 6);
			String machineId = new String(machineIdbyte);
			machineId = machineId.trim();
			comm.sendUnicast(machineId, comm.getStbcUnicastMessage());
			log(String.format("SEND[TO:%s] UNICAST : [%s]", sourceAddress, new String(comm.getStbcUnicastMessage())));
		} else if (recieveMessage[0] == '1') {
			// stbc power on response = register RFC
			registerRFC(recieveMessage, sourceAddress);
		}
	}
	
	/**
	 * @param recieveMessage
	 */
	private void registerRFC(byte[] recieveMessage, String sourceAddress) {
		byte[] machineCodeByte = new byte[2];
		byte[] machineIdbyte = new byte[6];
		System.arraycopy(recieveMessage, 1, machineCodeByte, 0, 2);
		System.arraycopy(recieveMessage, 3, machineIdbyte, 0, 6);
		
		String machineCode = new String(machineCodeByte);
		String machineId = new String(machineIdbyte);
		machineCode = machineCode.trim();
		machineId = machineId.trim();
		
		RFCData rfc = rfcMap.get(machineId);
		if (rfc != null) {
			if (sourceAddress.equals(rfc.getIpAddress()) == false) {
				rfc.getSender().close();
				UDPSender sender = null;
				try {
					sender = new UDPSender(sourceAddress, comm.getConfig().getNormalCommPort());
				} catch (Exception ignore) {}
				rfc.setSender(sender);
				rfc.setIpAddress(sourceAddress);
				rfc.setMachineCode(machineCode);
				rfcDataManager.addUpdateRfcMetaDataList(rfc);
			} else if (machineCode.equalsIgnoreCase(rfc.getMachineCode()) == false) {
				rfc.setMachineCode(machineCode);
				rfcDataManager.addUpdateRfcMetaDataList(rfc);
			}
		} else {
			// 통신은 되도록 추가는 해주지만... 이거 어쩌지..
			rfc = new RFCData(machineId);
			rfc.setMachineId(machineId);
			rfc.setMachineCode(machineCode);
			rfc.setIpAddress(sourceAddress);
			UDPSender sender = null;
			try {
				sender = new UDPSender(sourceAddress, comm.getConfig().getNormalCommPort());
			} catch (Exception ignore) {}
			rfc.setSender(sender);
			rfcLog(String.format("Unregisted RFC[%s] - response from [%s]", machineId, sourceAddress));
			rfcMap.put(machineId, rfc);
		}
	}

	/* (non-Javadoc)
	 * @see com.samsung.ocs.common.udpu.UDPServerJob#getRelpyMessage()
	 */
	public byte[] getRelpyMessage() {
		return null;
	}
	
	private void log(String s) {
		powerCommLogger.debug(s);
	}
	
	private void rfcLog(String s) {
		rfcLogger.debug(s);
	}
}
