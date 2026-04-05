package com.samsung.ocs.common.udpu;

import java.net.DatagramPacket;

/**
 * UDPServerJob Interface, OCS 3.0 for Unified FAB
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

public interface UDPServerJob {

	// recieveMessage를 받아서 어케 처리할껀지!!
	public void operation(DatagramPacket packet);
	
	//답장할게 있으면 답장할 메시지를 만들어서 리턴하도록 해바. 널이면 답장안해줌.
	public byte[] getRelpyMessage();
}
