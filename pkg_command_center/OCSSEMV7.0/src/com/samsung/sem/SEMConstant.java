package com.samsung.sem;

/**
 * SEMConstant Interface, OCS 3.0 for Unified FAB
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

public interface SEMConstant {
	public static final int NOT_EXIST = -1;
	public static final int NOT_OK = -1;
	public static final int OK = 0;
	
	public static String TRUE ="TRUE";
	public static String FALSE ="FALSE";
	
	// statusName
	public static String HSMSSTATUS = "HSMSSTATUS";
	public static String COMMSTATUS = "COMMSTATUS";
	public static String CONTROLSTATUS = "CONTROLSTATUS";
	public static String TSCSTATUS = "TSCSTATUS";
	
	// hsmsStatus
	public static String TCPIP_NOT_CONNECTED = "TCPIP_NOT_CONNECTED";
	public static String HSMS_SELECTED = "HSMS_SELECTED";
	public static String CONTROL_NONE = "CONTROL_NONE";
	
	// commStatus
	public static String COMM_DISABLED = "COMM_DISABLED";
	public static String NOT_COMMUNICATING = "NOT_COMMUNICATING";
	public static String COMMUNICATING = "COMMUNICATING";

	// controlStatus
	public static String EQ_OFFLINE = "EQ_OFFLINE";
	public static String HOST_OFFLINE = "HOST_OFFLINE";
	public static String ATTEMPT_ONLINE = "ATTEMPT_ONLINE";
	public static String LOCAL_ONLINE = "LOCAL_ONLINE";
	public static String REMOTE_ONLINE = "REMOTE_ONLINE";

	// tscStatus
	public static String TSC_NONE = "TSC_NONE";
	public static String TSC_INIT = "TSC_INIT";
	public static String TSC_PAUSED = "TSC_PAUSED";
	public static String TSC_AUTO = "TSC_AUTO";
	public static String TSC_PAUSING = "TSC_PAUSING";
	public static String TSC_ALARMS = "TSC_ALARMS";
	public static String TSC_NO_ALARMS = "TSC_NO_ALARMS";

	// VID (vid sequence)
	public static String ALARMID = "ALARMID";
	public static String ESTABLISHCOMMUNICATIONSTIMEOUT = "ESTABLISHCOMMUNICATIONSTIMEOUT";
	public static String ALARMSENABLED = "ALARMSENABLED";
	public static String ALARMSSET = "ALARMSSET";
	public static String CLOCK = "CLOCK";
	public static String CONTROLSTATE = "CONTROLSTATE";
	public static String EVENTSENABLED = "EVENTSENABLED";
	public static String ACTIVECARRIERS = "ACTIVECARRIERS";
	public static String ACTIVETRANSFERS = "ACTIVETRANSFERS";
	public static String ACTIVEVEHICLES = "ACTIVEVEHICLES";
	public static String CARRIERID = "CARRIERID";
	public static String CARRIERINFO = "CARRIERINFO";
	public static String CARRIERLOC = "CARRIERLOC";
	public static String COMMANDNAME = "COMMANDNAME";
	public static String COMMANDID = "COMMANDID";
	public static String COMMANDINFO = "COMMANDINFO";
	public static String CURRENTPORTSTATE = "CURRENTPORTSTATE";
	public static String DESTPORT = "DESTPORT";
	public static String EQPNAME = "EQPNAME";
	public static String PRIORITY = "PRIORITY";
	public static String PORTID = "PORTID";
	public static String REPLACE = "REPLACE";
	public static String RESULTCODE = "RESULTCODE";
	public static String SOURCEPORT = "SOURCEPORT";
	public static String SPECVERSION = "SPECVERSION";
	public static String TRANSFERCOMMAND = "TRANSFERCOMMAND";
	public static String TRANSFERINFO = "TRANSFERINFO";
	public static String TRANSFERPORT = "TRANSFERPORT";
	public static String TRANSFERPORTLIST = "TRANSFERPORTLIST";
	public static String VEHICLEID = "VEHICLEID";
	public static String VEHICLEINFO = "VEHICLEINFO";
	public static String VEHICLESTATE = "VEHICLESTATE";
	public static String TSCSTATE = "TSCSTATE";
	public static String REMOTECMD = "REMOTECMD";
	public static String ENHANCEDCARRIERINFO = "ENHANCEDCARRIERINFO";
	public static String ENHANCEDTRANSFERS = "ENHANCEDTRANSFERS";
	public static String TRANSFERCOMPLETEINFO = "TRANSFERCOMPLETEINFO";
	public static String ENHANCEDCARRIERS = "ENHANCEDCARRIERS";
	public static String TRANSFERSTATE = "TRANSFERSTATE";
	public static String INSTALLTIME = "INSTALLTIME";
	public static String ENHANCEDTRANSFERCOMMAND = "ENHANCEDTRANSFERCOMMAND";
	public static String CARRIERSTATE = "CARRIERSTATE";
	public static String IDREADSTATUS = "IDREADSTATUS";
	public static String STBSTATE = "STBSTATE";
	public static String AZFSLIST = "AZFSLIST";
	public static String VEHICLETYPE = "VEHICLETYPE";
	public static String ALARMTEXT = "ALARMTEXT"; // 2012.08.08 by KYK : alarmText추가 (TP VOC)
	public static String VEHICLECURRENTDOMAIN = "VEHICLECURRENTDOMAIN"; // 2013.10.01 : SetUnitAlram, ClearUnitAlram 추가
	public static String VEHICLECURRENTPOSITION = "VEHICLECURRENTPOSITION";
	// 2014.01.02 by KBS : FoupID 추가 (for A-PJT EDS)
	public static String TRANSFERINFO2 = "TRANSFERINFO2";
	public static String ACTIVECARRIERS2 = "ACTIVECARRIERS2";
	public static String FOUPID = "FOUPID"; 
	//2020.07.08 by JJW : TRANSFER UPDATE
	public static String FLOORID = "FLOORID";
	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	public static String DELIVERYINFO = "DELIVERYINFO";
	public static String DELIVERYTYPE = "DELIVERYTYPE";
	public static String EXPECTEDDELIVERYTIME = "EXPECTEDDELIVERYTIME";
	public static String DELIVERYWAITTIMEOUT = "DELIVERYWAITTIMEOUT";
	
	
	// remoteCmd
	public static String ABORT = "ABORT";
	public static String CANCEL = "CANCEL";
	public static String PAUSE = "PAUSE";
	public static String RESUME = "RESUME";
	public static String STAGEDELETE = "STAGEDELETE";
	public static String SCAN = "SCAN";
	public static String STAGE = "STAGE";
	public static String TRANSFER = "TRANSFER";
	public static String PREMOVE = "PREMOVE";	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	// 2014.01.02 by KBS : TRANSFER_EX3 처리 (for A-PJT EDS)
	public static String TRANSFER_EX3 = "TRANSFER_EX3";
	// 2020.07.08 by JJW : TRANSFER UPDATE
	public static String TRANSFERUPDATE = "TRANSFERUPDATE";
	// 2021.04.02 by JDH : Transfer Premove 사양 추가
	public static String TRANSFER_EX4 = "TRANSFER_EX4";
	
	// STBC remoteCmd
	public static String INSTALL = "INSTALL";
	// 2014.01.02 by KBS : INSTALL2 처리 (for A-PJT EDS)
	public static String INSTALL2 = "INSTALL2";
	public static String REMOVE = "REMOVE";
	public static String IDREAD = "IDREAD";
	public static String INSTALLLIST = "INSTALLLIST";
	public static String IDREADLIST = "IDREADLIST";
	public static String CARRIERDATALIST = "CARRIERDATALIST";
	public static String CARRIERDATA = "CARRIERDATA";
	public static String PORTIDLIST = "PORTIDLIST";
	// 2014.01.02 by KBS : INSTALLLIST2 처리 (for A-PJT EDS)
	public static String INSTALLLIST2 = "INSTALLLIST2";
	public static String CARRIERDATA2 = "CARRIERDATA2";

	public static String DESTCHANGE = "DESTCHANGE";
	public static String STAGECHANGE = "STAGECHANGE";
	public static String STAGEID = "STAGEID";
	public static String STAGEINFO = "STAGEINFO";
	public static String SCANINFO = "SCANINFO";
	public static String CMD_CANCELLING = "CMD_CANCELLING";
	public static String CMD_ABORTING = "CMD_ABORTING";
	public static String EXPECTEDDURATION = "EXPECTEDDURATION";
	public static String NOBLOCKINGTIME = "NOBLOCKINGTIME";
	public static String WAITTIMEOUT = "WAITTIMEOUT";
	
	// CEID
	public static String ControlStatusRemote = "ControlStatusRemote";
	public static String EquipmentOffline = "EquipmentOffline";
	public static String TSCAutoInitiated = "TSCAutoInitiated";
	public static String TSCPaused = "TSCPaused";	
	
	// etc
	public static String ACK = "ACK";	
	public static String NAK = "NAK";		
	public static String ITEM = "ITEM";
	public static String NAME = "NAME";
	public static String VID = "VID";
	public static String CEID = "CEID";
	public static String REPORTID = "REPORTID";
	public static String REPORTITEM = "REPORTITEM";
	public static String ENABLED = "ENABLED";
	public static String SENT = "SENT";	
	public static String RECEIVED = "RECEIVED";
	public static String OCSNAME = "OCSNAME";	
	public static String COMMESTTIMEOUT = "COMMESTTIMEOUT";			
	
	// SECS Message Type
	public static String A = "A";
	public static String U2 = "U2";
	public static String U4 = "U4";
	public static String B = "B";
	public static String BOOLEAN = "BOOLEAN";
	
	public static enum SECSMSG_TYPE {A, B, BOOLEAN, U2, U4;
		/**
		 * Get SECSMSG_TYPE in String
		 * 
		 * @return
		 */
		public String toConstString() {
			switch (this) {
				case A:
					return SEMConstant.A;
				case B:
					return SEMConstant.B;
				case BOOLEAN:
					return SEMConstant.BOOLEAN;
				case U2:
					return SEMConstant.U2;
				case U4:
					return SEMConstant.U4;	
				default:
					return SEMConstant.A;
			}
		}
	};
}
