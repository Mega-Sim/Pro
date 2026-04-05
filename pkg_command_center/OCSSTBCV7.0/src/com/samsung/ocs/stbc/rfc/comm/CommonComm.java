package com.samsung.ocs.stbc.rfc.comm;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.udpu.UDPSender;
import com.samsung.ocs.stbc.rfc.config.RfcConfig;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant;
import com.samsung.ocs.stbc.rfc.model.RFCData;

/**
 * CommonComm Class, OCS 3.0 for Unified FAB
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

public class CommonComm {
	protected int sendPort = 9001;
	protected UDPSender broadcastSender;
	protected Map<String, RFCData> rfcMap;
	protected RfcConfig config;
	private static Logger powerCommLogger = Logger.getLogger(RfcConstant.POWERCOMMLOGGER);
	
	/**
	 * Constructor of CommonComm class.
	 * 
	 * @param config
	 * @param rfcMap
	 * @param broadcastAddress
	 * @param sendPort
	 */
	public CommonComm(RfcConfig config, Map<String, RFCData> rfcMap, String broadcastAddress, int sendPort) {
		this.sendPort = sendPort;
		this.rfcMap = rfcMap;
		this.config = config;
		try {
			this.broadcastSender = new UDPSender(broadcastAddress, sendPort);
		} catch (SocketException e) {
			e.printStackTrace();
			powerCommLogger.error(e);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			powerCommLogger.error(e);
		}
	}
	
	public boolean sendBroadcast(byte[] data) {
		boolean result = broadcastSender.send2(data);
		log(String.format("SEND BROADCAST : [%s]", new String(data)));
		List<String> additionalAddressList = config.getAdditionalbroadcastAddressList();
		if(additionalAddressList!=null && additionalAddressList.size() > 0) {
			for(String address : additionalAddressList) {
				UDPSender additionalSender = null;
				try {
					additionalSender = new UDPSender(address, sendPort);
					additionalSender.send2(data);
					additionalSender.close();
					log(String.format("SEND BROADCAST : [%s][%s]", address, new String(data)));
				} catch (Exception ignore) {
					ignore.printStackTrace();
					powerCommLogger.error(ignore);
				} finally {
					if (additionalSender != null) {
						try { additionalSender.close(); } catch (Exception ignore) {}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Send Unicast Message
	 * 
	 * @param rfcId
	 * @param data
	 * @return
	 */
	public boolean sendUnicast(String rfcId, byte[] data) {
		RFCData rfc = rfcMap.get(rfcId);
		boolean result = false;
		if (rfc != null) {
			UDPSender sender = null;
			try {
				sender = new UDPSender(rfc.getIpAddress(), sendPort);
				log(String.format("SEND UNICAST : [%s]", new String(data)));
				result = sender.send(data);
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} finally {
				if (sender != null) {
					try { sender.close(); } catch (Exception ignore) {}
				}
			}
		}
		return result;
	}
	
	public void close() {
		if (broadcastSender != null) {
			broadcastSender.close();
		}
	}

	public Map<String, RFCData> getRfcMap() {
		return rfcMap;
	}

	public RfcConfig getConfig() {
		return config;
	}
	
	private void log(String s) {
		powerCommLogger.debug(s);
	}
}
