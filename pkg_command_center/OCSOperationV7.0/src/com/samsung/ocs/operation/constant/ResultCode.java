package com.samsung.ocs.operation.constant;

/**
 * OperationConstant Interface, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2012. 11. 12.
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

public class ResultCode {

	// 모듈실행시 ResultCode.xml 에 정의된 값이 있으면 그 값으로 업데이트 함
	public static int RESULTCODE_PREMOVE_WAIT_TIMEOUT =1;	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	public static int RESULTCODE_UNLOADED_BUT_CARRIERNOTEXIST = 1;
	public static int RESULTCODE_STBUNLOAD_CARRIERMISMATCH = 1;
	public static int RESULTCODE_STB_LOADFAIL = 21; 
	public static int RESULTCODE_EQ_LOADFAIL = 1;
	public static int RESULTCODE_STB_UNLOADFAIL = 22; 
	public static int RESULTCODE_EQ_UNLOADFAIL = 1;
	public static int RESULTCODE_TRDELETED_BY_VEHICLEREMOVE = 1;
	public static int RESULTCODE_TRDELETED_BY_USER = 1;
	public static int RESULTCODE_SCANDELETED_BY_USER = 23;
	public static int RESULTCODE_STBPORT_OUTOFSERVICE = 1;
	public static int RESULTCODE_RAILDOWN = 75;
	public static int RESULTCODE_UNLOADING_VHL_ERROR_CARRIER_EXIST = 32;
	public static int RESULTCODE_UNLOADING_VHL_ERROR_CARRIER_NOT_EXIST = 44;
	public static int RESULTCODE_MISSED_CARRIER = 1;
	public static int RESULTCODE_DETOUR = 34;
}
