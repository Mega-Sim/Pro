package com.samsung.ocs.failover.job;

import java.net.DatagramPacket;

import com.samsung.ocs.common.udpu.UDPServerJob;
import com.samsung.ocs.failover.model.HeartBeat;

/**
 * HeartBeatListenJob Class, OCS 3.0 for Unified FAB
 * 
 * HeartBeat Listen시 처리를 담당하는 JOB 구현 클래스 (UDPServerJob).
 * HeartBeat 객체를 받아서 해당 HeartBeat Configuration에 맞는 처리를 한다.
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

public class HeartBeatListenJob implements UDPServerJob {
	private HeartBeat heartBeat;
	
	/**
	 * HeartBeat을 인자로 받는 생성자
	 * @param heartBeat
	 */
	public HeartBeatListenJob(HeartBeat heartBeat) {
		this.heartBeat = heartBeat;
	}
	
	/**
	 * UDP를 Listen하였을 때, 수행하는 콜백메서드.
	 */
	public void operation(DatagramPacket packet) {
		String recieveString = new String(packet.getData()).trim();
		if (recieveString.indexOf("OCSHB:") >= 0 && recieveString.length() > 6) {
			synchronized (heartBeat) {
				heartBeat.setLastReportedString(recieveString.substring(6));
				heartBeat.setLastReportedTime(System.currentTimeMillis());
				heartBeat.setReported(true);
			}
		}
	}

	/**
	 * UDP를 Listen하였을때 Replay내용을 반환하는 콜백메서드.
	 */
	public byte[] getRelpyMessage() {
		return null;
	}
}
