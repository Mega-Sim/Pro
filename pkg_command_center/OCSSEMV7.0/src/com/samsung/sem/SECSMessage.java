package com.samsung.sem;

import com.samsung.sem.items.ReportItems;
import com.samsung.sem.ucominterface.UComMsg;

/**
 * SECSMessage Class, OCS 3.0 for Unified FAB
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

public class SECSMessage {
	protected String tscId;
	protected String messageType;
	protected String eventName;
	protected String commandId;
	protected String carrierId;
	protected String lotId;
	protected String sourceLoc;
	protected String destLoc;
	protected String currLoc;
	protected String resultCode;
	protected String deviceId;
	protected String vehicleId;
	protected String moveStatus;
	protected String fullMessage;
	
	private static final String ABORT = "ABORT";
	private static final String CANCEL = "CANCEL";
//	private static final String PAUSE = "PAUSE";
//	private static final String RESUME = "RESUME";
	private static final String STAGEDELETE = "STAGEDELETE";
	private static final String SCAN = "SCAN";
	private static final String STAGE = "STAGE";
	private static final String TRANSFER = "TRANSFER";
	private static final String TRANSFERUPDATE = "TRANSFERUPDATE"; // 2021.01.21 by JJW
//	private static final String DESTCHANGE = "DESTCHANGE";
//	private static final String STAGECHANGE = "STAGECHANGE";
	
	private static final String COMMANDINFO = "COMMANDINFO";
	private static final String COMMANDID = "COMMANDID";
	private static final String PRIORITY = "PRIORITY";
	private static final String REPLACE = "REPLACE";
	private static final String TRANSFERINFO = "TRANSFERINFO";
	private static final String CARRIERID = "CARRIERID";
	private static final String SOURCEPORT = "SOURCEPORT";
	private static final String DESTPORT = "DESTPORT";
	private static final String STAGEINFO = "STAGEINFO";
	private static final String STAGEID = "STAGEID";
	private static final String SCANINFO = "SCANINFO";
	private static final String CARRIERLOC = "CARRIERLOC";
	
	private static final String ABORT_TRANSACTION = "Abort Transaction";
	private static final String ALARM_REPORT_SEND = "Alarm Report Send";
	private static final String ALARM_REPORT_ACKNOWLEDGE = "Alarm Report Acknowledge";
	private static final String ARE_YOU_THERE = "Are You There?";
	private static final String DATE_AND_TIME_REQUEST = "Date & Time Request";
	private static final String DATE_AND_TIME_DATA = "Date & Time Data";
	private static final String DATE_AND_TIME_SET_REQUEST = "Date and Time Set Request";
	private static final String DATE_AND_TIME_SET_ACKNOWLEDGE = "Date and Time Acknowledge";
	private static final String DEFINE_REPORT = "Define Report";
	private static final String DEFINE_REPORT_ACKNOWLEDGE = "Define Report Acknowledge";
	private static final String ENABLE_DISABLE_ALARM_SEND = "Enable/Disable Alarm Send";
	private static final String ENABLE_DISABLE_ALARM_SEND_ACKNOWLEDGE = "Enable/Disable Alarm Send Acknowledge";
	private static final String ENABLE_DISABLE_EVENT_REPORT = "Enable/Disable Event Report";
	private static final String ENABLE_DISABLE_EVENT_REPORT_ACKNOWLEDGE = "Enable/Disable Event Report Acknowledge";
	private static final String ENHANCED_REMOTE_COMMAND = "Enhanced Remote Command";
	private static final String ENHANCED_REMOTE_COMMAND_ACKNOWLEDGE = "Enhanced Remote Command Acknowledge";
	private static final String ESTABLISH_COMMUNICATION_REQUEST = "Establish Communication Request";
	private static final String ESTABLISH_COMMUNICATION_REQUEST_ACKNOWLEDGE = "Establish Communication Request Acknowledge";
	private static final String EQUIPMENT_CONSTANT_REQUEST = "Equipment Constant Request";
	private static final String EQUIPMENT_CONSTANT_DATA = "Equipment Constant Data";
	private static final String EQUIPMENT_CONSTANT_NAMELIST_REQUEST = "Equipment Constant NameList Request";
	private static final String EQUIPMENT_CONSTANT_NAMELIST = "Equipment Constant NameList";
	private static final String EVENT_REPORT_DATA = "Event Report Data";
	private static final String EVENT_REPORT_REQUEST = "Event Report Request";
//	private static final String EVENT_REPORT_SEND = "Event Report Send";
	private static final String EVENT_REPORT_ACKNOWLEDGE = "Event Report Acknowledge";
	private static final String HOST_COMMAND_SEND = "Host Command Send";
	private static final String HOST_COMMAND_ACKNOWLEDGE = "Host Command Acknowledge";
	private static final String LINK_REPORT_AND_EVENT = "Link Report/Event";
	private static final String LINK_REPORT_AND_EVENT_ACKNOWLEDGE = "Link Report/Event Acknowledge";
	private static final String LIST_ALARM_REQUEST = "List Alarm Request";
	private static final String LIST_ALARM_DATA = "List Alarm Data";
	private static final String NEW_EQUIPMENT_CONSTANT_SEND = "New Equipment Constant Send";
	private static final String NEW_EQUIPMENT_CONSTANT_SEND_ACKNOWLEDGE = "New Equipment Constant Send Acknowledge";
	private static final String OFFLINE_ACKNOWLEDGE = "Off-Line Acknowledge";
	private static final String ONLINE_ACKNOWLEDGE = "On-Line Acknowledge";
	private static final String ONLINE_DATA_SEND = "On-Line Data Send";
	private static final String REPORT_PARTICULAR_AZFS_LIST = "Report Particular AZFS List";
	private static final String REQUEST_TO_OFFLINE = "Request to Off-Line";
	private static final String REQUEST_TO_ONLINE = "Request to On-Line";
	private static final String SELECTED_EQ_STATUS_REQUEST = "Selected EQ Status Request";
	private static final String SELECTED_EQ_STATUS_REQUEST_ACKNOWLEDGE = "Selected EQ Status Request Acknowledge";
	
	public SECSMessage() {
		tscId = "";
		messageType = "";
		eventName = "";
		commandId = "";
		carrierId = "";
		lotId = "";
		sourceLoc = "";
		destLoc = "";
		currLoc = "";
		resultCode = "";
		deviceId = "";
		vehicleId = "";
		moveStatus = "";
		fullMessage = "";
	}
	
	public SECSMessage(UComMsg uComMsg, String eventName, ReportItems reportItems, String messageType) {
		tscId = "";
		this.messageType = messageType;
		this.eventName = eventName;
		commandId = "";
		carrierId = "";
		lotId = "";
		sourceLoc = "";
		destLoc = "";
		currLoc = "";
		resultCode = "";
		deviceId = "";
		vehicleId = "";
		moveStatus = "";
		fullMessage = "";
		
		if (uComMsg != null) {
			if (uComMsg.getSEComMsg() != null) {
				uComMsg.getSEComMsg().reset();
				fullMessage = uComMsg.getSEComMsg().getSECS2Log();
				if (fullMessage != null) {
					// 2012.01.13 by PMM
//					fullMessage.replace("\r\n                   ", "\r\n");
					
					// 2012.01.16 by PMM
//					fullMessage.replace("                   ", "");
//					fullMessage.replace("    ", "\t");
					fullMessage = fullMessage.replace("                   <", "<");
					fullMessage = fullMessage.replace("                   >", ">");
					fullMessage = fullMessage.replace("    ", "\t");
				} else {
					fullMessage = "";
				}
			}
			
			if (reportItems != null) {
				commandId = makeString(reportItems.getCommandId());
				carrierId = makeString(reportItems.getCarrierId());
				sourceLoc = makeString(reportItems.getSourcePort());
				destLoc = makeString(reportItems.getDestPort());
				if (reportItems.getTransferPort() != null) {
					currLoc = makeString(reportItems.getTransferPort());
				} else {
					currLoc = makeString(reportItems.getCarrierLoc());
				}
				resultCode = String.valueOf(reportItems.getResultCode());
				vehicleId = makeString(reportItems.getVehicleId());
			}
			parseUComMessage(uComMsg);
		}
	}
	
	private void parseUComMessage(UComMsg uComMsg) {
		assert uComMsg != null;
		
		switch (uComMsg.getStream()) {
			case 1:
				parseS1Fy(uComMsg);
				break;
			case 2:
				parseS2Fy(uComMsg);
				break;
			case 5:
				parseS5Fy(uComMsg);
				break;
			case 6:
				parseS6Fy(uComMsg);
				break;
			default:
				break;
		}
	}
	
	private void parseS1Fy(UComMsg uComMsg) {
		assert uComMsg != null;
		
		switch (uComMsg.getFunction()) {
			case 0:
				eventName = ABORT_TRANSACTION;
				break;
			case 1:
				eventName = ARE_YOU_THERE;
				break;
			case 2:
				eventName = ONLINE_DATA_SEND;
				break;
			case 3:
				eventName = SELECTED_EQ_STATUS_REQUEST;
				break;
			case 4:
				eventName = SELECTED_EQ_STATUS_REQUEST_ACKNOWLEDGE;
				break;
			case 13:
				eventName = ESTABLISH_COMMUNICATION_REQUEST;
				break;
			case 14:
				eventName = ESTABLISH_COMMUNICATION_REQUEST_ACKNOWLEDGE;
				break;
			case 15:
				eventName = REQUEST_TO_OFFLINE;
				break;
			case 16:
				eventName = OFFLINE_ACKNOWLEDGE;
				break;
			case 17:
				eventName = REQUEST_TO_ONLINE;
				break;
			case 18:
				eventName = ONLINE_ACKNOWLEDGE;
				break;
			case 20:
				// STBC
				eventName = REPORT_PARTICULAR_AZFS_LIST;
				break;
			default:
				break;
		}
	}
	
	private void parseS2Fy(UComMsg uComMsg) {
		assert uComMsg != null;
		
		switch (uComMsg.getFunction()) {
			case 0:
				eventName = ABORT_TRANSACTION;
				break;
			case 13:
				eventName = EQUIPMENT_CONSTANT_REQUEST;
				break;
			case 14:
				eventName = EQUIPMENT_CONSTANT_DATA;
				break;
			case 15:
				eventName = NEW_EQUIPMENT_CONSTANT_SEND;
				break;
			case 16:
				eventName = NEW_EQUIPMENT_CONSTANT_SEND_ACKNOWLEDGE;
				break;
			case 17:
				eventName = DATE_AND_TIME_REQUEST;
				break;
			case 18:
				eventName = DATE_AND_TIME_DATA;
				break;
			case 29:
				eventName = EQUIPMENT_CONSTANT_NAMELIST_REQUEST;
				break;
			case 30:
				eventName = EQUIPMENT_CONSTANT_NAMELIST;
				break;
			case 31:
				eventName = DATE_AND_TIME_SET_REQUEST;
				break;
			case 32:
				eventName = DATE_AND_TIME_SET_ACKNOWLEDGE;
				break;
			case 33:
				eventName = DEFINE_REPORT;
				break;
			case 34:
				eventName = DEFINE_REPORT_ACKNOWLEDGE;
				break;
			case 35:
				eventName = LINK_REPORT_AND_EVENT;
				break;
			case 36:
				eventName = LINK_REPORT_AND_EVENT_ACKNOWLEDGE;
				break;
			case 37:
				eventName = ENABLE_DISABLE_EVENT_REPORT;
				break;
			case 38:
				eventName = ENABLE_DISABLE_EVENT_REPORT_ACKNOWLEDGE;
				break;
			case 41:
				eventName = HOST_COMMAND_SEND;
				parseS2F41(uComMsg);
				break;
			case 42:
				eventName = HOST_COMMAND_ACKNOWLEDGE;
				parseS2F42(uComMsg);
				break;
			case 49:
				eventName = ENHANCED_REMOTE_COMMAND;
				parseS2F49(uComMsg);
				break;
			case 50:
				eventName = ENHANCED_REMOTE_COMMAND_ACKNOWLEDGE;
				break;
			default:
				break;
		}
	}

	private void parseS5Fy(UComMsg uComMsg) {
		assert uComMsg != null;
		
		switch (uComMsg.getFunction()) {
			case 0:
				eventName = ABORT_TRANSACTION;
				break;
			case 1:
				eventName = ALARM_REPORT_SEND;
				break;
			case 2:
				eventName = ALARM_REPORT_ACKNOWLEDGE;
				break;
			case 3:
				eventName = ENABLE_DISABLE_ALARM_SEND;
				break;
			case 4:
				eventName = ENABLE_DISABLE_ALARM_SEND_ACKNOWLEDGE;
				break;
			case 5:
				eventName = LIST_ALARM_REQUEST;
				break;
			case 6:
				eventName = LIST_ALARM_DATA;
				break;
			default:
				break;
		}
	}
	
	private void parseS2F41(UComMsg uComMsg) {
		assert uComMsg != null;
		
		uComMsg.getListItem();
		String remoteCmd = uComMsg.getAsciiItem();
		
		eventName = "Host Command Send (" + remoteCmd + ")";
		
//		if (CANCEL.equals(remoteCmd) || ABORT.equals(remoteCmd) || STAGEDELETE.equals(remoteCmd)) { 
		if (CANCEL.equals(remoteCmd) || ABORT.equals(remoteCmd) || STAGEDELETE.equals(remoteCmd) || TRANSFERUPDATE.equals(remoteCmd)) { // 2021.01.21 by JJW
			uComMsg.getListItem(); // L, n
			uComMsg.getListItem(); // L, 2
			if (COMMANDID.equals(uComMsg.getAsciiItem())) {
				commandId = uComMsg.getAsciiItem();
			}
		}
	}
	
	private void parseS2F42(UComMsg uComMsg) {
		assert uComMsg != null;
		
		uComMsg.getListItem(); // L, n
		uComMsg.getListItem(); // L, 2
		if (COMMANDID.equals(uComMsg.getAsciiItem())) {
			commandId = uComMsg.getAsciiItem();
		}
	}
	
	private void parseS2F49(UComMsg uComMsg) {
		assert uComMsg != null;
		
		uComMsg.getListItem(); // L, 4
		uComMsg.getU2Item(); // DATAID = 0 (사양에 맞게 U4 -> U2 로 바꿈)
		uComMsg.getAsciiItem(); // OBJSPEC
		String remoteCmd = uComMsg.getAsciiItem();
		
		eventName = "Enhanced Remote Command (" + remoteCmd + ")";
		
		if (TRANSFER.equals(remoteCmd)) {
			parseS2F49Transfer(uComMsg);
		} else if (STAGE.equals(remoteCmd)) {
			parseS2F49Stage(uComMsg);
		} else if (SCAN.equals(remoteCmd)) {
			parseS2F49Scan(uComMsg);
		}
	}
	
	private void parseS2F49Transfer(UComMsg uComMsg) {
		int itemCount = 0;
		String commandParameterName;
		
		itemCount = uComMsg.getListItem(); // L, 2 [CommandInfo, TransferInfo]
		if (itemCount == 2 || itemCount == 3) {
			itemCount = uComMsg.getListItem(); // L, 2
			commandParameterName = uComMsg.getAsciiItem(); // CPNAME1 : "COMMANDIDINFO"
			itemCount = uComMsg.getListItem(); // L, 3 : CommandID, Priority, Replace
			
			if (COMMANDINFO.equals(commandParameterName) && itemCount == 3) {
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'COMMANDID'
				if (COMMANDID.equals(commandParameterName) && itemCount == 2) {
					commandId = uComMsg.getAsciiItem();
				} else {
					return;
				}
				
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'PRIORITY'
				if (PRIORITY.equals(commandParameterName) && itemCount == 2) {
					uComMsg.getU2Item();
				} else {
					return;
				}
				
				itemCount = uComMsg.getListItem();  // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'REPLACE'
				if (REPLACE.equals(commandParameterName) && itemCount == 2) {
					uComMsg.getU2Item();
				} else {
					return;
				}
			}
		} else {
			return;
		}
		
		itemCount = uComMsg.getListItem(); // L, 2
		commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'TRANSFERINFO'
		itemCount = uComMsg.getListItem(); // L, 3 : CarrierID, SourcePort, DestPort
		if (TRANSFERINFO.equals(commandParameterName) && itemCount == 3) {
			itemCount = uComMsg.getListItem(); // L, 2
			commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'CARRIERID'			
			if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
				carrierId = uComMsg.getAsciiItem();
			} else {
				return;
			}
			
			itemCount = uComMsg.getListItem(); // L, 2
			commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'SOURCEPORT'
			if (SOURCEPORT.equals(commandParameterName) && itemCount == 2) {
				sourceLoc = uComMsg.getAsciiItem(); // CPVAL : A SourcePort
			} else {
				return;
			}
			
			// Get destPort 
			itemCount = uComMsg.getListItem(); // L, 2
			commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'DESTPORT'
			if (DESTPORT.equals(commandParameterName) && itemCount == 2) {
				destLoc = uComMsg.getAsciiItem(); // CPVAL : A DestPort
			} else {
				return;
			}
		} else {
			return;
		}
	}
	
	private void parseS2F49Stage(UComMsg uComMsg) {
		int itemCount = 0;
		String commandParameterName;
		
		itemCount = uComMsg.getListItem(); // L, 2 [StageInfo, TransferInfo]	
		if (itemCount == 2) {
			itemCount = uComMsg.getListItem(); // L, 2
			commandParameterName = uComMsg.getAsciiItem(); // CPNAME1 : "STAGEINFO"
			itemCount = uComMsg.getListItem(); // L, 6 : StageId, Priority, Replace, ExpectedDuration, NoBlockingTime, WaitTimeout
			
			if (STAGEINFO.equals(commandParameterName) && itemCount == 6) {

				// Get StageId 
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'STAGEID'
				if (STAGEID.equals(commandParameterName) && itemCount == 2) {
					commandId = uComMsg.getAsciiItem();
				} else {
					return;
				}
			} else {
				return;
			}
			
			itemCount = uComMsg.getListItem(); // L, 2
			commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'TRANSFERINFO'
			itemCount = uComMsg.getListItem(); // L, 3 : CarrierID, SourcePort, DestPort
			
			if (TRANSFERINFO.equals(commandParameterName) && itemCount == 3) {
				// Get carrierId 
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'CARRIERID'			
				if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
					carrierId = uComMsg.getAsciiItem();
					
				} else {
					return;
				}
				
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'SOURCEPORT'
				if (SOURCEPORT.equals(commandParameterName) && itemCount == 2) {
					sourceLoc = uComMsg.getAsciiItem(); // CPVAL : A SourcePort
				} else {
					return;
				}
				
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'DESTPORT'
				if (DESTPORT.equals(commandParameterName) && itemCount == 2) {
					destLoc = uComMsg.getAsciiItem(); // CPVAL : A DestPort
				} else {
					return;
				}
			} else {
				return;
			}
		} else {
			return;
		}
	}
	
	private void parseS2F49Scan(UComMsg uComMsg) {
		int itemCount = 0;
		String commandParameterName;
		
		itemCount = uComMsg.getListItem(); // L, 2 [CommandInfo, ScanInfo]	
		if (itemCount == 2) {
			itemCount = uComMsg.getListItem(); // L, 2
			commandParameterName = uComMsg.getAsciiItem(); // CPNAME1 : "COMMANDIDINFO"
			itemCount = uComMsg.getListItem(); // L, 2 : CommandID, Priority
			
			if (COMMANDINFO.equals(commandParameterName) && itemCount == 2) {
				
				// Get commandId 
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'COMMANDID'
				if (COMMANDID.equals(commandParameterName) && itemCount == 2) {
					// Normal case
					commandId = uComMsg.getAsciiItem();
				} else {
					return;
				}
				
			} else {
				return;
			}
			
			itemCount = uComMsg.getListItem(); // L, 2
			commandParameterName = uComMsg.getAsciiItem(); // CPNAME2 "SCANINFO"
			itemCount = uComMsg.getListItem(); // L, 2 : carrierId,carrierLoc
			if (SCANINFO.equals(commandParameterName) && itemCount == 2) {
				// Get carrierId 
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'CARRIERID'			
				if (CARRIERID.equals(commandParameterName) && itemCount == 2) {
					carrierId = uComMsg.getAsciiItem();
				} else {
					return;
				}
				
				itemCount = uComMsg.getListItem(); // L, 2
				commandParameterName = uComMsg.getAsciiItem(); // CPNAME : 'SOURCEPORT'
				if (CARRIERLOC.equals(commandParameterName) && itemCount == 2) {
					sourceLoc = uComMsg.getAsciiItem(); // CPVAL : A SourcePort
				} else {
					return;
				}
			} else {
				return;
			}
		} else {
			return;
		}
	}
	
	private void parseS6Fy(UComMsg uComMsg) {
		assert uComMsg != null;
		
		switch (uComMsg.getFunction()) {
			case 0:
				eventName = ABORT_TRANSACTION;
				break;
			case 11:
//				eventName = EVENT_REPORT_SEND;
				break;
			case 12:
				eventName = EVENT_REPORT_ACKNOWLEDGE;
				break;
			case 15:
				eventName = EVENT_REPORT_REQUEST;
				break;
			case 16:
				eventName = EVENT_REPORT_DATA;
				break;
			default:
				break;
		}
	}
	
	public String getTscId() {
		return tscId;
	}

	public void setTscId(String tscId) {
		this.tscId = tscId;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getCommandId() {
		return commandId;
	}

	public void setCommandId(String commandId) {
		this.commandId = commandId;
	}

	public String getCarrierId() {
		return carrierId;
	}

	public void setCarrierId(String carrierId) {
		this.carrierId = carrierId;
	}

	public String getLotId() {
		return lotId;
	}

	public void setLotId(String lotId) {
		this.lotId = lotId;
	}

	public String getSourceLoc() {
		return sourceLoc;
	}

	public void setSourceLoc(String sourceLoc) {
		this.sourceLoc = sourceLoc;
	}

	public String getDestLoc() {
		return destLoc;
	}

	public void setDestLoc(String destLoc) {
		this.destLoc = destLoc;
	}

	public String getCurrLoc() {
		return currLoc;
	}

	public void setCurrLoc(String currLoc) {
		this.currLoc = currLoc;
	}

	public String getResultCode() {
		return resultCode;
	}

	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public String getMoveStatus() {
		return moveStatus;
	}

	public void setMoveStatus(String moveStatus) {
		this.moveStatus = moveStatus;
	}

	public String getFullMessage() {
		return fullMessage;
	}

	public void setFullMessage(String fullMessage) {
		this.fullMessage = fullMessage;
	}
	
	private String makeString(String string) {
		if (string == null){
			return "";
		} else {
			return string;
		}
	}
}