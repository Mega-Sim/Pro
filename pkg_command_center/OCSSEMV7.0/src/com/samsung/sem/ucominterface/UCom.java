package com.samsung.sem.ucominterface;

/* [ SEComEnabler 3.1 version Upgrade ] 변경내역
 * 0. lib/ secomdriver-3.1.0.jar, xutil4j-1.0.jar
 * 1. import SEComEnabler.SEComPlugIn.SXPlugIn -> SEComEnabler.SEComPlugIn.SinglePlugin
 * 2. import SEComEnabler.SEComStructure.SEComPlugInEvent -> SEComEnabler.SEComDriver.SECSListener
 * 3. class UCom implements SEComPlugInEvent -> class UCom implements SECSListener
 * 4. SXPlugIn plugin -> SinglePlugin plugin
 * 5. plugin.initializeDriver -> plugin.initialize
 */

import SEComEnabler.SEComDriver.SECSListener; 
import SEComEnabler.SEComPlugIn.SinglePlugin;  
import SEComEnabler.SEComStructure.SEComError;
import SEComEnabler.SEComStructure.SXTransaction;
import SEComEnabler.SEComStructure.SEComError.SEComTimeout;

/**
 * UCom Class, OCS 3.0 for Unified FAB
 * 
 * UCom is used for SECS Communication.
 * SEComEnabler Driver 사용
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

public class UCom implements SECSListener  {
	private String commCfgFile;
	private UComEventListener uListener;	
	private SinglePlugin plugin; /** SECom을 제어하기 위한 클래스 */		
	private String driverId; /** SECom에서 동작시킬 드라이버 Id */

	/**
	 * UCom 생성자	 
	 * @param listener  통신모듈에서 발생한 이벤트를 받을 인터페이스
	 */
	public UCom(UComEventListener listener) {
		plugin = new SinglePlugin(this);
		driverId = "MCS";
		uListener = listener;		
	}

	/**
	 * SECom 연결시 호출
	 * @param driverId : 연결된 드라이버 Id
	 */
	public void OnSECSConnected(String driverId) {
		uListener.onSECSConnected();
	}

	/**
	 * SECom 연결이 끊겼을 경우 호출
	 * @param driverId : 연결이 끊긴 드라이버 Id
	 */
	public void OnSECSDisConnected(String driverId) {
		uListener.onSECSDisConnected();
	}

	/**
	 * SECS 메세지 수신시 호출
	 * @param driverId : 메세지를 송신한 드라이버 Id
	 * @param trx 수신된 SECS 메세지
	 */
	public void OnSECSReceived(String driverId, SXTransaction trx) {
		uListener.onSECSReceived(new UComMsg(trx));
	}

	/**
	 * SECom 통신중 TimeOut이 발생되었을 경우 호출
	 * @param driverId 메세지를 송신한 드라이버 Id
	 * @param trx 수신된 SECS 메세지
	 */
	public void OnSECSTimeOut(String driverId, SXTransaction trx) {
		if (trx.getErrorCode()== SEComTimeout.ERR_TIMEOUT_T3) {
			uListener.onSECST3TimeOut();
		}
	}

	public void OnSECSInvalidReceived(String driverId, SXTransaction trx) {	}
	public void OnSECSUnknownMessage(String driverId, SXTransaction trx) { }
	public void OnSECSAbortMessage(String driverId, SXTransaction trx) { }
	public void OnSECS1Log(String driverId, String aSECS1Log) { }
	public void OnSECS2Log(String driverId, String aSECS2Log) { }

	/**
	 * 통신서비스를 선택한 모듈에 따라 시작한다.
	 */
	public boolean startService() {
		int returnCode = 0;
		returnCode = plugin.initialize(driverId, commCfgFile);

		return (checkError(returnCode) == false);
	}

	/**
	 * 통신서비스를 선택한 모듈에 따라 재시작한다.
	 */
	public boolean restartService() {				
		boolean bResult = false;
		if (stopService()) {
			bResult = startService();
		}		
		return bResult;
	}

	/**
	 * 통신서비스를 선택한 모듈에 따라 중지시킨다.
	 */
	public boolean stopService() {		
		int nRtnCode = 0;
		plugin.terminate();
		return (checkError(nRtnCode) == false);
	}

	/**	  
	 * @param stream
	 * @param function
	 * @param waitBit
	 * @return
	 */
	private SXTransaction makeSendingSXMsg(int stream, int function, boolean waitBit) {
		SXTransaction rsp = new SXTransaction(driverId);
		rsp.setStream(stream);
		rsp.setFunction(function);
		rsp.setWait(waitBit);

		return rsp;
	}

	/**
	 * @param stream
	 * @param function
	 * @param systemBytes
	 * @return
	 */
	private SXTransaction makeReplySXMsg(int stream, int function, long systemBytes) {
		SXTransaction rsp = new SXTransaction(driverId);
		rsp.setStream(stream);
		rsp.setFunction(function);
		rsp.setSystemBytes(systemBytes);
		rsp.setWait(false);

		return rsp;
	}

	/**
	 * 요청 메세지 보내기		
	 * @param umsg   SECS 메세지
	 */
	int requestMsg(UComMsg msg) {
		return plugin.request(msg.getSEComMsg());
	}

	/**
	 * 응답 메세지 보내기	 
	 * @param umsg   SECS 메세지
	 */
	int replyMsg(UComMsg msg) {
		return plugin.reply(msg.getSEComMsg());
	}

	/**
	 * 통신설정파일을 Setting 한다.
	 * @param filename  통신파일명
	 */
	public void setCommCfgFile(String filename) {
		commCfgFile = filename;
	}

	/**
	 * 선택한 통신모듈에 따라 전송 메세지를 만든다.
	 *
	 * @param nStream   Stream 번호
	 * @param nFunction Function 번호
	 * @param bWaitBit  wait bit 설정여부
	 */
	public UComMsg makeSendingMsg(int stream, int function, boolean waitBit) {
		UComMsg rsp = new UComMsg(makeSendingSXMsg(stream, function, waitBit));
		return rsp;
	}

	public UComMsg makeSecsMsg(int stream, int function) {
		return makeSendingMsg(stream, function, true);
	}

	/**
	 * 선택한 통신모듈에 따라 응답 메세지를 만든다.
	 *
	 * @param nStream   Stream 번호
	 * @param nFunction Function 번호
	 * @param lSystemBytes  System Bytes
	 */

	public UComMsg makeReplyMsg(int stream, int function, long lSystemBytes) {
		UComMsg rsp = new UComMsg(makeReplySXMsg(stream, function, lSystemBytes));
		return rsp;
	}

	/**
	 * XCom과 컨버전을 용의하게 하기 위해서 추가된 함수
	 *
	 * @param umsg   전송메세지
	 * @param bIsRequestMsg 전송메세지 여부
	 */
	public UComMsg makeSecsMsg(int stream, int function, long lSystemBytes) {
		return makeReplyMsg(stream, function, lSystemBytes);
	}

	/**
	 * 선택한 통신모듈에 따라 메세지를 전송한다.
	 *
	 * @param umsg   전송메세지
	 * @param bIsRequestMsg 전송메세지 여부
	 */
	public boolean send(UComMsg umsg, boolean bIsRequestMsg) {
		int nReturn = 0;
		if (bIsRequestMsg == true) {
			nReturn = requestMsg(umsg);
		} else {
			nReturn = replyMsg(umsg);
		}

		return (checkError(nReturn) == false);
	}

	public void closeSecsMsg(UComMsg msg) { }

	/**
	 * 로그를 출력하는 함수
	 *
	 * @param strmsg   로그내용
	 */
	private void writeLog(String strmsg) {
		System.out.println(strmsg);
	}

	/**
	 * 선택한 모듈별로 에러를 체크하는 함수	 *
	 * @param nRtnCode   리턴코드
	 */

	public boolean checkError(int nRtnCode) {
		boolean bRet = false;

		if (nRtnCode != SEComError.ERR_NONE) {
			UComError.ErrCode = nRtnCode;
			UComError.ErrString = SEComError.getErrDescription(nRtnCode);
			writeLog("[ERROR] ErrCode:" + String.valueOf(nRtnCode) + " ErrMsg: "
					+ SEComError.getErrDescription(nRtnCode));
			bRet = true;
		} else {
			UComError.ErrCode = 0;
			UComError.ErrString = "";
			bRet = false;
		}
		return bRet;
	}
}
