package com.samsung.sem.ucominterface;
	
/**
 * UComEventListener Interface, OCS 3.0 for Unified FAB
 * 
 * UCom에서 이벤트를 받기위한 인터페이스
 * UCom생성시 parameter로 들어가며, UCom으로부터 메세지를
 * 받기위한 클래스는 이 인터페이스를 구현하여야 한다.
 * 
 * @see UCom, UComMsg
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date 2011. 6. 21.
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

public interface UComEventListener {

	/**
	 * UCom에서 SECS메세지 수신시 호출되는 Method		 
	 * @param umsg SECS메세지
	 */
	public void onSECSReceived(UComMsg umsg);

	/** UCom에서 연결시 호출되는 Method */
	public void onSECSConnected();

	/** UCom에서 연결이 끊겼을 때 호출되는 Method */
	public void onSECSDisConnected();

	/** UCom에서 T3 Time Out이 발생시 호출되는 Method	 */
	public void onSECST3TimeOut();	
}