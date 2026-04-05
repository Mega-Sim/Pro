package com.samsung.ocs.failover.constant;

import com.samsung.ocs.common.constant.OcsConstant;

/**
 * ClusterConstant Interface, OCS 3.0 for Unified FAB
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

public interface ClusterConstant {
	
	public static enum EVENT_TYPE {DETECT, CM_STARTUP, HEARTBEATDELAY, USERTAKEOVERREQ, USERTAKEOVER, USERTAKEOVERFAIL, USERTAKEOVERIGNORE,
		REMOTESTATECHANGE, STATECHANGE, LOCALNET_UP, PUBLICNET_UP, VIP_UP, PROCESS_STARTUP, PROCESS_DOWN, 
		AUTOTAKEOVER, AUTOTAKEOVERFAIL, AUTOTAKEOVERIGNORE, LOCALNET_DOWN, PUBLICNET_DOWN, VIP_DOWN, ABNORMALLOGGEN;
	
		/**
		 * Get AlarmCode from EVENT_TYPE
		 * 
		 * @return
		 */
		
		/* 14.11.18 LSH
		 * Alarm Type, Code ¿Á¡§∏≥
		public int toAlarmCode() {
			switch (this) {
				case AUTOTAKEOVERFAIL:
					return 9500;
				case AUTOTAKEOVERIGNORE:
					return 9511;
				case USERTAKEOVERFAIL:
					return 9510;
				case AUTOTAKEOVER:
					return 9000;
				case USERTAKEOVERIGNORE:
					return 9011;
				case USERTAKEOVER:
					return 9010;
				case PROCESS_DOWN :
					return 7100;
				case PROCESS_STARTUP :
					return 7200;
				case CM_STARTUP :
					return 7000;
				case STATECHANGE :
					return 2000;
				case REMOTESTATECHANGE :
					return 1500;
				case DETECT :
					return 1000;
				case ABNORMALLOGGEN :
					return 7300;
				case HEARTBEATDELAY :
					return 1100;
				default :
					return 0;
			}
		}
		*/

		public int toAlarmCode() {
			switch (this) {
				case DETECT :
					return 1000;
				case CM_STARTUP :
					return 3000;
				case HEARTBEATDELAY :
					return 3050;
				case USERTAKEOVERREQ :
					return 3100;
				case USERTAKEOVER:
					return 3101;
				case USERTAKEOVERFAIL:
					return 3102;
				case USERTAKEOVERIGNORE:
					return 3103;
				case REMOTESTATECHANGE :
					return 3200;
				case STATECHANGE :
					return 3300;
				case LOCALNET_UP :
					return 3301;
				case PUBLICNET_UP :
					return 3302;
				case VIP_UP :
					return 3303;					
				case PROCESS_STARTUP :
					return 3500;
				case PROCESS_DOWN :
					return 3600;
				case AUTOTAKEOVER:
					return 4001;
				case AUTOTAKEOVERFAIL:
					return 4002;
				case AUTOTAKEOVERIGNORE:
					return 4003;
				case LOCALNET_DOWN:
					return 4301;
				case PUBLICNET_DOWN:
					return 4302;
				case VIP_DOWN:
					return 4303;
				case ABNORMALLOGGEN :
					return 5000;
				default :
					return 0;
			}
		}
	};
	
	public static enum HEARTBEATTYPE {
		PUBLIC, LOCAL, DIRECT;
		public static HEARTBEATTYPE toHeartBeatType(String str) {
			if (OcsConstant.PUBLIC.equalsIgnoreCase(str)) {
				return HEARTBEATTYPE.PUBLIC;
			} else if (OcsConstant.LOCAL.equalsIgnoreCase(str)) {
				return HEARTBEATTYPE.LOCAL;
			} else {
				return HEARTBEATTYPE.DIRECT;
			}
		}
	};
	
	public static String ALIVE = "ALIVE";
	public static String DEAD = "DEAD";
	public static String PUBLICNET = "PUBLIC_NET";
	public static String LOCALNET = "LOCAL_NET";
	public static String VIP = "VIP";
	public static String CLUSTERMANAGER = "CM";
	public static String PINGCHECK = "PINGCHECK";
	
	public static String TCPLOGGER = "TCPContent";
	
	public static String WRITEOUT = "WRITEOUT";
	public static String WRITEERR = "WRITEERR";
	
}
