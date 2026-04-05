package com.samsung.ocs.stbc.rfc.constant;

/**
 * RfcConstant Interface, OCS 3.0 for Unified FAB
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

public interface RfcConstant {
	public static String RFCREAD = "rfcRead";
	public static String BROADCASTADDRESS = "broadcastAddress";
	public static String POWERCOMMPORT = "powerCommPort";
	public static String NORMALCOMMPORT = "normalCommPort";
	public static String STATUSCOMMPORT = "statusCommPort";
	
	public static String STBCMACHINECODE = "stbcMachineCode";
	public static String RFCMACHINECODE = "rfcMachineCode";
	public static String STBCMACHINEID = "stbcMachineId";
	
	public static String MULTIPLEREADCOUNT = "multipleReadCount";
	
	public static String READRETRYCOUNT = "readRetryCount";
	public static String VERIFYRETRYCOUNT = "verifyRetryCount";
	
	public static String POWERCOMMLOGGER = "powercomm";
	public static String NORMALCOMMLOGGER = "normalcomm";
	public static String RFCMANAGERLOGGER = "RfcManager";
	
    public static String SENDSTATUSINTERVALMILLIS = "sendStatusIntervalMillis";
    public static String RFCTIMEOUTMILLIS = "rfcTimeoutMillis";
	
	public static String FLAG_BROADCAST = "0";
	public static String FLAG_UNICAST = "1";
	
	public static String READTIMEOUT = "readTimeout";
	public static String READALLTIMEOUT = "readAllTimeout";
	
	public static String READALLRETRYCOUNT = "readAllRetryCount";
	public static String READALLNORESPONSETIMEOUT = "readAllNoResponseTimeout";
	
	public static String ADDITIONALBROADCASTADDRESS = "addtionalBroadcastAddress";
	
	public static enum EVENT_STATE {REQUESTED, PROCESSING, COMPLETED, TIMEOUT, RETRYING};
	
	// 2014.01.30 by KBS : Read2, ReadAll2, Verify2 Ãß°¡ (for A-PJT EDS)
	public static enum NORMAL_COMMTYPE {STATUS, READ, READALL, VERIFY, READ2, READALL2, VERIFY2, UNKNOWN;
	
		/**
		 * Get Normal CommType in Byte
		 * 
		 * @return
		 */
		public byte toByte() {
			byte result = 0;
			switch (this) {
				case STATUS:
					result = 1;
					break;
				case READ:
					result = 21;
					break;
				case READALL:
					result = 25;
					break;
				case VERIFY:
					result = 31;
					break;
				case UNKNOWN:
				default:
					break;
			}
			return result;
		}
		
		/**
		 * Get NORMAL_COMMTYPE from type
		 * 
		 * @param type
		 * @return
		 */
		public static NORMAL_COMMTYPE toCommType(byte type) {
			switch (type) {
				case 3 :
					return NORMAL_COMMTYPE.STATUS;
				case 22:
					return NORMAL_COMMTYPE.READ;
				case 24:
					return NORMAL_COMMTYPE.READ2;
				case 26:
					return NORMAL_COMMTYPE.READALL;
				case 28:
					return NORMAL_COMMTYPE.READALL2;
				case 32:
					return NORMAL_COMMTYPE.VERIFY;
				case 34:
					return NORMAL_COMMTYPE.VERIFY2;
				default:
					return NORMAL_COMMTYPE.UNKNOWN;
			}
		}
	}
	
	public static enum READ_RESULT {OK, READERROR, NOTREGISTERD;
		/**
		 * Get ReadResult in Byte
		 * 
		 * @return
		 */
		public byte toByte() {
			byte result = 0;
			switch (this) {
				case OK:
					result = 0;
					break;
				case READERROR:
					result = 1;
					break;
				case NOTREGISTERD :
					result = 2;
					break;
				default :
					break;
			}
			return result;
		}
		
		/**
		 * Get READ_RESULT from byte
		 * 
		 * @param result
		 * @return
		 */
		public static READ_RESULT toReadRsult(byte result) {
			switch (result) {
				case 0:
					return READ_RESULT.OK;
				case 1:
					return READ_RESULT.READERROR;
				case 2:
					return READ_RESULT.NOTREGISTERD;
				default:
					return READ_RESULT.READERROR;
			}
		}
	}
	
	public static enum VERIFY_RESULT {OK, MISMATCH, STBDISABLED;
		/**
		 * Get VerifyResult in Byte
		 * 
		 * @return
		 */
		public byte toByte() {
			byte result = 0;
			switch (this) {
				case OK:
					result = 0;
					break;
				case MISMATCH:
					result = 1;
					break;
				case STBDISABLED :
					result = 9;
					break;
				default :
					break;
			}
			return result;
		}
		
		/**
		 * Get VERIFY_RESULT from Byte
		 * 
		 * @param result
		 * @return
		 */
		public static VERIFY_RESULT toVerifyRsult(byte result) {
			switch (result) {
				case 0:
					return VERIFY_RESULT.OK;
				case 1:
					return VERIFY_RESULT.MISMATCH;
				case 2:
					return VERIFY_RESULT.STBDISABLED;
				default:
					return VERIFY_RESULT.STBDISABLED;
			}
		}
	}
	
	public static String ON_LINE = "ONLINE";
	public static String OFF_LINE = "OFFLINE";
	public static enum RFC_COND {ONLINE, OFFLINE;
	
		/**
		 * Get RfcCondition in String
		 * 
		 * @return
		 */
		public String toConditionString() {
			switch (this) {
				case ONLINE:
					return ON_LINE;
				case OFFLINE:
				default:
					return OFF_LINE;
			}
		}
		
		/**
		 * Get RFC_COND From String
		 * 
		 * @param condition
		 * @return
		 */
		public static RFC_COND toRfcCondition(String condition) {
			if (ON_LINE.equalsIgnoreCase(condition)) {
				return ONLINE;
			} else {
				return OFFLINE;
			}
		}
	}
}