package com.sec.iesw.mcs.infra.common.ucom;

/** 
 * UCom에서 이벤트를 받기위한 인터페이스
 * <p> 
 * UCom생성시 parameter로 들어가며, UCom으로부터 메세지를
 * 받기위한 클래스는 이 인터페이스를 구현하여야 한다.
 *  
 * @author  Hyung Doo, Yoon
 * @version 1.0
 * @since 1.4
 * @see UCom, UComMsg
 * 
 */
public interface IUComEventListener {

	/** 
	 * UCom에서 SECS메세지 수신시 호출되는 Method
	 *
	 * @param umsg SECS메세지 
	 */
	public void OnSECSReceived(UComMsg umsg);

	/** 
	 * UCom에서 연결시 호출되는 Method
	 */
	public void OnSECSConnected();

	/** 
	 * UCom에서 연결이 끊겼을 때 호출되는 Method
	 */
	public void OnSECSDisConnected();

	/** 
	 * UCom에서 T3 Time Out이 발생시 호출되는 Method
	 */
	public void OnSECST3TimeOut();
}
