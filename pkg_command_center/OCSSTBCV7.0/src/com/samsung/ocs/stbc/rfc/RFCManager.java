package com.samsung.ocs.stbc.rfc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.connection.DBAccessManager;
import com.samsung.ocs.stbc.manager.RFCDataManager;
import com.samsung.ocs.stbc.rfc.comm.NormalComm;
import com.samsung.ocs.stbc.rfc.comm.PowerComm;
import com.samsung.ocs.stbc.rfc.config.RfcConfig;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.EVENT_STATE;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.NORMAL_COMMTYPE;
import com.samsung.ocs.stbc.rfc.constant.RfcConstant.RFC_COND;
import com.samsung.ocs.stbc.rfc.model.EventEntry;
import com.samsung.ocs.stbc.rfc.model.RFCData;
import com.samsung.ocs.stbc.rfc.model.STBData;
import com.samsung.ocs.stbc.rfc.model.commmsg.NormalCommResult;
import com.samsung.ocs.stbc.rfc.model.commmsg.Read;
import com.samsung.ocs.stbc.rfc.model.commmsg.Read2;
import com.samsung.ocs.stbc.rfc.model.commmsg.ReadAll;
import com.samsung.ocs.stbc.rfc.model.commmsg.ReadAll2;
import com.samsung.ocs.stbc.rfc.model.commmsg.Status;
import com.samsung.ocs.stbc.rfc.model.commmsg.Verify;
import com.samsung.ocs.stbc.rfc.model.commmsg.Verify2;
import com.samsung.ocs.stbc.rfc.thread.RFCManagerThread;

/**
 * RFCManager Class, OCS 3.0 for Unified FAB
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

public class RFCManager {
	private Map<String, RFCData> rfcMap;
	private Map<String, STBData> stbMap;
	private RfcConfig config;
	
	private List<EventEntry> requestEventList;
	private Map<String, EventEntry> processingEventMap;
	private List<EventEntry> completedEventList;
	private List<EventEntry> tempEventList;
	private List<String> tempStringList;
	
	//이건 udp로 normalComm 받은 결과..
	private Vector<NormalCommResult> recieveList;
	
	private PowerComm powerComm = null;
	private NormalComm normalComm = null;
	private RFCManagerThread managerThread = null;
	
	private DBAccessManager dbAccessManager = null;
	private RFCDataManager rfcDataManager = null;
	
	private static Logger rfcLogger = Logger.getLogger(RfcConstant.RFCMANAGERLOGGER);
	
	/**
	 * Constructor of RFCManager class.
	 */
	public RFCManager() {
		if (initializeConfig()) {
			initialize();
		}
	}
	
	/**
	 * Initilize Config
	 * 
	 * @return
	 */
	private boolean initializeConfig() {
		int i=0;
		while (true) {
			this.config = RfcConfig.getInstance();
			if (this.config != null) {
				break;
			}
			i++;
			if (i > 1000) {
				return false;
			} else {
				try {Thread.sleep(500);} catch (Exception ignore) {}
			}
		}
		return true;
	}
	
	/**
	 * Initialize
	 */
	private void initialize() {
		this.rfcMap = Collections.synchronizedMap(new HashMap<String, RFCData>());
		this.stbMap = Collections.synchronizedMap(new HashMap<String, STBData>());
		
		this.requestEventList = new Vector<EventEntry>();
		this.processingEventMap = new HashMap<String, EventEntry>();
		this.completedEventList = new Vector<EventEntry>();
		this.tempEventList = new ArrayList<EventEntry>();
		this.recieveList = new Vector<NormalCommResult>(); 
		this.tempStringList = new ArrayList<String>();
	}
	
	/**
	 * Start RFCManager
	 */
	public void startRFCManager() {
		this.rfcMap.clear();
		this.requestEventList.clear();
		this.processingEventMap.clear();
		this.completedEventList.clear();
		this.tempEventList.clear();
		this.recieveList.clear(); 
		this.tempStringList.clear();
		
		dbAccessManager = new DBAccessManager();
		try {
			rfcDataManager = new RFCDataManager(dbAccessManager, RFCData.class, true, true, 1000, rfcMap, stbMap, config);
		} catch (Exception ignore) {}
		
		powerComm = new PowerComm(config, rfcMap, config.getBroadcastAddress(), config.getPowerCommPort(), rfcDataManager);
		normalComm = new NormalComm(config, rfcMap, stbMap, recieveList, config.getBroadcastAddress(), config.getNormalCommPort(), rfcDataManager);
		
		managerThread = new RFCManagerThread(this);
		managerThread.start();
	}
	
	/**
	 * Stop RFCManager
	 */
	public void stopRFCManager() {
		// 2012.04.05 by KYK
		if (powerComm != null) {
			powerComm.close();
			powerComm = null;			
		}
		if (normalComm != null) {
			normalComm.close();
			normalComm = null;			
		}
		if (managerThread != null) {
			managerThread.stopThread();
			managerThread = null;			
		}
		if (rfcDataManager != null) {
			rfcDataManager.close();
			rfcDataManager = null;
		}
		if (dbAccessManager != null) {
			dbAccessManager.close();
			dbAccessManager = null;			
		}
		
//		powerComm.close();
//		powerComm = null;
//		normalComm.close();
//		normalComm = null;
//		managerThread.stopThread();
//		managerThread = null;
//		if (rfcDataManager != null) {
//			rfcDataManager.close();
//			rfcDataManager = null;
//		}
//		dbAccessManager.close();
//		dbAccessManager = null;
	}

	/*public Map<String, RFC> getRfcMap() {
		return rfcMap;
	}

	public List getRequestEventList() {
		return requestEventList;
	}

	public List getProcessingEventList() {
		return processingEventList;
	}*/

	/**
	 * Count ReadProcessing Event
	 */
	private int countReadProcessingEvent() {
		int count = 0;
		for (EventEntry e : processingEventMap.values()) {
			if (e.getEventType() == NORMAL_COMMTYPE.READ) {
				count++;
			}
		}
		return count;
	}
	
	
	// 2012.03.12 by LWG [Multiple Read Count 갯수대로 동작하게 적용]
	private boolean isAvailableAddReadProcessing() {
		if (config.getMultipleReadCount() > countReadProcessingEvent()) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * thread에서 주기적으로 돌릴꺼
	 */
	public void processingEvent() {
		
		// 기준시간을 하나로 잡고가자...
		long currentTime = System.currentTimeMillis();
		
		// request list 에서 읽어서 각 타입에 맞는 일을 시작해주면서 processingMap로 옮긴다.
		int requestListSize = requestEventList.size();
		
		// 10개.. 제한이 있으니까... temp list를 활용해서 유지를 해주자.
		tempEventList.clear();
		for (int idx = 0; idx < requestListSize; idx++) {
			EventEntry eventEntry = requestEventList.get(idx);
			
			// 2011.11.21 by KYK : Prevent NULL check
			if (eventEntry != null) {
				if (eventEntry.getEventType() != NORMAL_COMMTYPE.READ ||
						(eventEntry.getEventType() == NORMAL_COMMTYPE.READ && isAvailableAddReadProcessing())) {
					normalComm.sendEvent(eventEntry);
					// 2012.03.12 by LWG [ReadTimeOut 방식 변경]
					/*
					if (eventEntry.getEventType() == NORMAL_COMMTYPE.READ && eventEntry.getState() == EVENT_STATE.TIMEOUT) {
						eventEntry.setState(EVENT_STATE.RETRYING);
					} else {
						eventEntry.setState(EVENT_STATE.PROCESSING);
					}
					*/
					eventEntry.setState(EVENT_STATE.PROCESSING);
					eventEntry.setProcessingStartTime(currentTime);
					
					if (eventEntry.getEventType() != NORMAL_COMMTYPE.STATUS) {
						processingEventMap.put(eventEntry.getKey(), eventEntry);
					}
					
//					System.out.println("processingEventPut : [" + eventEntry.getKey() +"] ["+eventEntry.getRfcId()+"]["+eventEntry.getEventType()+"]");
				} else {
					tempEventList.add(eventEntry);
				}				
			}
		}
		// request event list에서는 지워주고
		for (int idx = 0; idx < requestListSize; idx++) {
			requestEventList.remove(0);
		}
		// tempList로 빼놨던거 다시 추가.. 
		requestEventList.addAll(0, tempEventList);
		
		// recieveList에서 읽어서 completed 여부를 확인해주고 processing map에서 completedList로 옮긴다.
		int recieveListSize = recieveList.size();
		for (int idx = 0; idx < recieveListSize; idx++) {
			NormalCommResult normalCommResult = recieveList.get(idx);
			
			RFCData rfc = rfcMap.get(normalCommResult.getMachineId().trim());
			
			if (normalCommResult.getCommType() == NORMAL_COMMTYPE.READALL) {
				EventEntry eventEntry = processingEventMap.get(normalCommResult.getKey());
				if (eventEntry != null) {
					ReadAll ra = (ReadAll) normalCommResult;
					Map<String, NormalCommResult> responseMap = eventEntry.getResponseMap();
					responseMap.put(String.valueOf(ra.getStbNumber()), ra);
					
					// last Message recieve
					if (ra.getDataCount() == ra.getDataMax()) {
						eventEntry.setState(EVENT_STATE.COMPLETED);
						eventEntry.setCompletedTime(currentTime);
						
						sendIdReadToNoResponseStb(eventEntry);
						
						// read all은 완료됐다 봄.
						processingEventMap.remove(eventEntry.getKey());
						completedEventList.add(eventEntry);
						
						// DB관련 처리를 해봅시다~
						if (rfc != null) {
							for (NormalCommResult n : eventEntry.getResponseMap().values()) {
								if ( n instanceof ReadAll ) {
									ReadAll raItem = (ReadAll) n;
									STBData stb = rfc.getStb(raItem.getStbNumber());
									if (stb != null) {
										if (stb.getReadResult() != (int)raItem.getReadResultByte() || 
											stb.getCarrierDetect() != (int)raItem.getCarrierDetectByte() ||
											stb.getIdData().equalsIgnoreCase(raItem.getIdData()) == false) {
											
											stb.setCarrierDetect((int)raItem.getCarrierDetectByte());
											stb.setReadResult((int)raItem.getReadResultByte());
											stb.setIdData(raItem.getIdData() == null ? "" : raItem.getIdData() );
											
											rfcDataManager.addUpdateStbResultDataList(stb);
										}
									}
								}
							}
						}
					}
				}
			} else if (normalCommResult.getCommType() == NORMAL_COMMTYPE.READALL2) {
				EventEntry eventEntry = processingEventMap.get(normalCommResult.getKey());
				if (eventEntry != null) {
					ReadAll2 ra = (ReadAll2) normalCommResult;
					Map<String, NormalCommResult> responseMap = eventEntry.getResponseMap();
					responseMap.put(String.valueOf(ra.getStbNumber()), ra);

					// last Message recieve
					if (ra.getDataCount() == ra.getDataMax()) {
						eventEntry.setState(EVENT_STATE.COMPLETED);
						eventEntry.setCompletedTime(currentTime);

						sendIdReadToNoResponseStb(eventEntry);

						// read all은 완료됐다 봄.
						processingEventMap.remove(eventEntry.getKey());
						completedEventList.add(eventEntry);

						// DB관련 처리를 해봅시다~
						if (rfc != null) {
							for (NormalCommResult n : eventEntry.getResponseMap().values()) {
								if ( n instanceof ReadAll ) {
									ReadAll2 raItem = (ReadAll2) n;
									STBData stb = rfc.getStb(raItem.getStbNumber());
									if (stb != null) {
										if (stb.getReadResult() != (int)raItem.getReadResultByte() || 
												stb.getCarrierDetect() != (int)raItem.getCarrierDetectByte() ||
												stb.getIdData().equalsIgnoreCase(raItem.getIdData()) == false) {

											stb.setCarrierDetect((int)raItem.getCarrierDetectByte());
											stb.setReadResult((int)raItem.getReadResultByte());
											stb.setIdData(raItem.getIdData() == null ? "" : raItem.getIdData() );
											stb.setFoupIdData(raItem.getFoupIdData() == null ? "" : raItem.getFoupIdData() );

											rfcDataManager.addUpdateStbResultDataList(stb);
										}
									}
								}
							}
						}
					}
				}
			} else if (normalCommResult.getCommType() == NORMAL_COMMTYPE.STATUS) {
				// state 읽는 로직이 필요하겟지.. 
				updateRFCStatusData((Status)normalCommResult);
				EventEntry eventEntry = processingEventMap.get(normalCommResult.getKey());
//				System.out.println(normalCommResult.getKey());
				if (eventEntry != null) {
					processingEventMap.remove(eventEntry.getKey());
				}
			} else {
//				System.out.println("recieve comm result : ["+normalCommResult.getKey()+"] ["+normalCommResult.getCommType()+"]");
				EventEntry eventEntry = processingEventMap.get(normalCommResult.getKey());
				if (eventEntry != null) {
					eventEntry.getResponseMap().put(normalCommResult.getKey(), normalCommResult);
					eventEntry.setState(EVENT_STATE.COMPLETED);
					eventEntry.setCompletedTime(currentTime);
					processingEventMap.remove(eventEntry.getKey());
					completedEventList.add(eventEntry);
					
					// DB관련 처리를 해봅시다~
					if (rfc != null) {
						if (normalCommResult.getCommType() == NORMAL_COMMTYPE.READ) {
							Read rItem = (Read) normalCommResult;
							STBData stb = rfc.getStb(rItem.getStbNumber());
							if (stb != null) {
								if (stb.getReadResult() != (int)rItem.getReadResultByte() || 
									stb.getCarrierDetect() != (int)rItem.getCarrierDetectByte() ||
									stb.getIdData().equalsIgnoreCase(rItem.getIdData()) == false) {
		
									stb.setCarrierDetect((int)rItem.getCarrierDetectByte());
									stb.setReadResult((int)rItem.getReadResultByte());
									stb.setIdData(rItem.getIdData() == null ? "" : rItem.getIdData() );
		
									rfcDataManager.addUpdateStbResultDataList(stb);
								}
							}
						} else if (normalCommResult.getCommType() == NORMAL_COMMTYPE.READ2) {
							Read2 rItem = (Read2) normalCommResult;
							STBData stb = rfc.getStb(rItem.getStbNumber());
							if (stb != null) {
								if (stb.getReadResult() != (int)rItem.getReadResultByte() || 
									stb.getCarrierDetect() != (int)rItem.getCarrierDetectByte() ||
									stb.getIdData().equalsIgnoreCase(rItem.getIdData()) == false || 
									stb.getFoupIdData().equalsIgnoreCase(rItem.getFoupIdData()) == false) {
		
									stb.setCarrierDetect((int)rItem.getCarrierDetectByte());
									stb.setReadResult((int)rItem.getReadResultByte());
									stb.setIdData(rItem.getIdData() == null ? "" : rItem.getIdData() );
									stb.setFoupIdData(rItem.getFoupIdData() == null ? "" : rItem.getFoupIdData() );
		
									rfcDataManager.addUpdateStbResultDataList(stb);
								}
							}
						} else if (normalCommResult.getCommType() == NORMAL_COMMTYPE.VERIFY) {
							Verify vItem = (Verify) normalCommResult;
							STBData stb = rfc.getStb(vItem.getStbNumber());
							if (stb != null) {
								if (stb.getReadResult() != (int)vItem.getVerifyResultByte() || 
										stb.getCarrierDetect() != (int)vItem.getCarrierDetectByte() ||
										stb.getReadResult() != (int)vItem.getReadResultByte() ||
										stb.getIdData().equalsIgnoreCase(vItem.getIdData()) == false) {
									
									stb.setCarrierDetect((int)vItem.getCarrierDetectByte());
									stb.setVerifyResult((int)vItem.getVerifyResultByte());
									stb.setReadResult((int)vItem.getReadResultByte());
									stb.setIdData(vItem.getIdData() == null ? "" : vItem.getIdData() );
									
									rfcDataManager.addUpdateStbResultDataList(stb);
								}
							}
						} else if (normalCommResult.getCommType() == NORMAL_COMMTYPE.VERIFY2) {
							Verify2 vItem = (Verify2) normalCommResult;
							STBData stb = rfc.getStb(vItem.getStbNumber());
							if (stb != null) {
								if (stb.getReadResult() != (int)vItem.getVerifyResultByte() || 
										stb.getCarrierDetect() != (int)vItem.getCarrierDetectByte() ||
										stb.getReadResult() != (int)vItem.getReadResultByte() ||
										stb.getIdData().equalsIgnoreCase(vItem.getIdData()) == false ||
										stb.getFoupIdData().equalsIgnoreCase(vItem.getFoupIdData()) == false) {
									
									stb.setCarrierDetect((int)vItem.getCarrierDetectByte());
									stb.setVerifyResult((int)vItem.getVerifyResultByte());
									stb.setReadResult((int)vItem.getReadResultByte());
									stb.setIdData(vItem.getIdData() == null ? "" : vItem.getIdData() );
									stb.setFoupIdData(vItem.getIdData() == null ? "" : vItem.getFoupIdData() );
									
									rfcDataManager.addUpdateStbResultDataList(stb);
								}
							}
						}
					}
				}
			}
		}
		// request event list에서는 지워주고
		for (int idx = 0; idx < recieveListSize; idx++) {
			recieveList.remove(0);
		}
		
		// timeout 처리 등등을 해본다.
		tempStringList.clear();
		for (EventEntry e : processingEventMap.values()) {
			long processingTime = 0;
			processingTime = currentTime - e.getProcessingStartTime();
			switch (e.getEventType()) {
				case READ:
					if (processingTime > config.getReadTimeout()) {
						// 2012.03.12 by LWG [ReadTimeOut 방식 변경]
//						e.setState(EVENT_STATE.TIMEOUT);
//						e.increaseRetryCount();
						// 2012.03.12 by LWG [ReadTimeOut 방식 변경] : 리트라이 카운트 현실적 반영
						if (config.getReadRetryCount() <= e.getRetryCount()) {
							e.setCanceledTime(currentTime);
							e.setState(EVENT_STATE.TIMEOUT);
							tempStringList.add(e.getKey());
							completedEventList.add(e);
							log(String.format("TIMEOUT EVENT [%s]", e.getKey()));
						} else {
							// 2012.03.12 by LWG [ReadTimeOut 방식 변경]
							normalComm.sendEvent(e);
							e.setProcessingStartTime(currentTime);
							e.increaseRetryCount();
							e.setState(EVENT_STATE.RETRYING);
							log(String.format("RETRY EVENT [%s]", e.getKey()));
						}
					}
					break;
				case READALL:
					//readall 후 무응답이면..
					if (e.getResponseMap().size() > 0) {
						if (processingTime > config.getReadAllTimeout()) {
							e.setState(EVENT_STATE.TIMEOUT);
							sendIdReadToNoResponseStb(e);
							e.setCanceledTime(currentTime);
							tempStringList.add(e.getKey());
							completedEventList.add(e);
						}
					} else {
						if (processingTime > config.getReadAllNoResponseTimeoutMillis()) {
							e.increaseRetryCount();
							// 2012.03.12 by LWG [ReadTimeOut 방식 변경] : 리트라이 카운트 현실적 반영
							if (config.getReadAllRetryCount() < e.getRetryCount()) {
								e.setState(EVENT_STATE.TIMEOUT);
								e.setCanceledTime(currentTime);
								tempStringList.add(e.getKey());
								completedEventList.add(e);
							} else {
								tempStringList.add(e.getKey());
								requestEventList.add(e);
							}
						}
					}
					break;
				case STATUS:
					break;
				case VERIFY:
					if (processingTime > (e.getTimeOut()*1000)) {
						e.setState(EVENT_STATE.TIMEOUT);
						e.increaseRetryCount();
						// 2012.03.12 by LWG [ReadTimeOut 방식 변경] : 리트라이 카운트 현실적 반영
						if (config.getVerifyRetryCount() < e.getRetryCount()) {
							e.setCanceledTime(currentTime);
							tempStringList.add(e.getKey());
							completedEventList.add(e);
							log(String.format("TIMEOUT EVENT [%s]", e.getKey()));
						} else {
							tempStringList.add(e.getKey());
							requestEventList.add(e);
							log(String.format("RETRY EVENT [%s]", e.getKey()));
						}
					}
					break;
			}
		}
		for (String s : tempStringList) {
			processingEventMap.remove(s);
		}
		
		for (RFCData r : rfcMap.values()) {
			// 다른 메시지랑은 관계없이 10초에 한번 보내라니까 이렇게 바꿔둔다.
			if (r.getCondition() == RFC_COND.ONLINE) {
				if (currentTime - r.getLastStatusSendTime() > config.getSendStatusIntervalMillis()) {
					requestStatus(r.getRfcId());
				}
			}
		}
		
		for (RFCData r : rfcMap.values()) {
			if (currentTime - r.getLastRecievedTime() > config.getRfcTimeoutMillis()) {
				if (r.getCondition() == RFC_COND.ONLINE) {
					// 2012.02.17 by LWG [RFC TIMEOUT] : Ready를 0으로 올리기.
					r.setCondition(RFC_COND.OFFLINE);
					rfcDataManager.addUpdateRfcCondition(r);
					r.setReady(0);
					r.setError(1);
					r.setErrorCode("1000");
					byte[] b = new byte[256];
					b[0] = 0x04;
					b[2] = 10;
					r.setStatusAndReturnEquals(b);
					rfcDataManager.addUpdateRfcStatusDataList(r);
				}
			} else {
				if (r.getCondition() == RFC_COND.OFFLINE) {
					r.setCondition(RFC_COND.ONLINE);
					rfcDataManager.addUpdateRfcCondition(r);
				}
			}
		}
	}
	
	/**
	 * Update RFC Status Data
	 * 
	 * @param s
	 */
	private void updateRFCStatusData(Status s) {
		long startTime = System.currentTimeMillis();
		
		String rfcId = s.getMachineId().trim();
		RFCData rfc = rfcMap.get(rfcId);
		
		StringBuffer r = new StringBuffer ("  READY         : ");
		StringBuffer cs = new StringBuffer("  CarrierSensor : ");
		StringBuffer sh = new StringBuffer("  STBHOMESensor : ");
		StringBuffer e1 = new StringBuffer("  ECAT1Conn     : ");
		StringBuffer e2 = new StringBuffer("  ECAT1Conn     : ");
		
		
		if (rfc != null) {
			byte[] status = s.getStatus();
			// 일단 rfc 객체에 저장을 하는걸로 하고... 만약 상태가 이전과 같으면 한번 쉴수있지.. 어?
			// 이전과 같으면 로그도 안찍을래!!!!
			if (rfc.setStatusAndReturnEquals(status) == false) {
				
				byte header = status[0];
				int rfcReady = header & 1;
				int rfcError = header >> 2 & 1;
				String errorCode = String.format("%2x%2x", status[2], status[3]);
				if (errorCode.equals(" 0 0")) {
					errorCode = "";
				}
				
				if (rfc.getReady() != rfcReady ||
					rfc.getError() != rfcError ||
					errorCode.equalsIgnoreCase(rfc.getErrorCode())== false) {
					rfc.setReady(rfcReady);
					rfc.setError(rfcError);
					rfc.setErrorCode(errorCode);
					rfcDataManager.addUpdateRfcStatusDataList(rfc);
				}
				
				int readyStart = 4;
				int carrierSensorStart = 36;
				int stbHomeSensorStart = 68;
				int E1ConnStart = 100;
				int E2ConnStart = 132;
				
				for (int i = 0; i < 32; i++) {
					byte readyByte = status[readyStart+i];
					byte carrierSensorByte = status[carrierSensorStart+i];
					byte stbHomeSensorByte = status[stbHomeSensorStart+i];
					byte E1ConnByte = status[E1ConnStart+i]; 
					byte E2ConnByte = status[E2ConnStart+i]; 
					for (int j = 0; j < 8; j++) {
						int stbIndex = i*8+j+1; 
						STBData stb = rfc.getStb(stbIndex);
						if (stb != null) {
							int ready = (readyByte >> j) & 1;
							int carrierSensor = (carrierSensorByte >> j) & 1;
							int stbHomeSensor = (stbHomeSensorByte >> j) & 1;
							int E1Conn = (E1ConnByte >> j) & 1;
							int E2Conn = (E2ConnByte >> j) & 1;
							
							r.append("STB-").append(stbIndex).append("[").append(ready).append("] ");
							cs.append("STB-").append(stbIndex).append("[").append(carrierSensor).append("] ");
							sh.append("STB-").append(stbIndex).append("[").append(stbHomeSensor).append("] ");
							e1.append("STB-").append(stbIndex).append("[").append(E1Conn).append("] ");
							e2.append("STB-").append(stbIndex).append("[").append(E2Conn).append("] ");
							
							if (stb.getReady() != ready ||
								stb.getCarrierSensor() != carrierSensor ||
								stb.getStbHomeSensor() != stbHomeSensor ||
								stb.getECAT1Conn() != E1Conn ||
								stb.getECAT2Conn() != E2Conn) {
								
								stb.setReady(ready);
								stb.setCarrierSensor(carrierSensor);
								stb.setStbHomeSensor(stbHomeSensor);
								stb.setECAT1Conn(E1Conn);
								stb.setECAT2Conn(E2Conn);
								
								rfcDataManager.addUpdateStbStatusDataList(stb);
							}
						}
					}
				}
				log(String.format("[%s] STATUS START-----------------------------------------------------------------------", rfc.getRfcId()) );
				log(String.format("Ready[%d], error[%d], errorCode[%s] ", rfcReady, rfcError, errorCode));
				log(r.toString());
				log(cs.toString());
				log(sh.toString());
				log(e1.toString());
				log(e2.toString());
				log(String.format("[%s] STATUS END-------------------------------------------------------------------------", rfc.getRfcId()) );
				
			}
		} else {
			// TODO : 미등록인데 응답하는애들을 어떻게할까나.... ! 버림!
			; /*NULL*/
		}
		
		long elapsedTime = System.currentTimeMillis() - startTime;
		log(String.format("parse status area. elapsedTime[%d]millis", elapsedTime));
	}
	
	/**
	 * Send IDReset to Not Responding STB
	 * 
	 * @param eventEntry
	 */
	private void sendIdReadToNoResponseStb(EventEntry eventEntry) {
		String rfcId = eventEntry.getRfcId().trim();
		ArrayList<String> idReadPortList = eventEntry.getIdReadPortList();
		
		if (idReadPortList == null) {
			RFCData rfcData = rfcMap.get(rfcId);
			Set<String> stbNumberSet = rfcData.getNewIndexSet();
			for (NormalCommResult ncr : eventEntry.getResponseMap().values()) {
				ReadAll ra = (ReadAll) ncr;
				stbNumberSet.remove(String.valueOf(ra.getStbNumber()));
			}
			for (String s : stbNumberSet) {
				requestIdRead(rfcId, Integer.parseInt(s), eventEntry.isIdRead());
			}
		} else {
			// 2015.06.08 by KBS : IDREADLIST 후 응답없는 port 처리 개선
			ArrayList<Integer> noResponsePortList = new ArrayList<Integer>();
			for (String s : idReadPortList) {
				STBData stbData = stbMap.get(s);
				ReadAll ra = (ReadAll) eventEntry.getResponseMap().get(String.valueOf(stbData.getRfcIndex()));
				if (ra == null) {
					noResponsePortList.add(stbData.getRfcIndex());
				}
			}
			for (Integer i : noResponsePortList) {
				requestIdRead(rfcId, i, eventEntry.isIdRead());
			}
		}
	}
	
	/**
	 * Request IDRead
	 * 
	 * @param carrierLocId
	 * @param idRead
	 */
	public void requestIdRead(String carrierLocId, boolean idRead) {
		STBData stbData = stbMap.get(carrierLocId);
		if(stbData != null) {
			EventEntry e = new EventEntry(stbData.getOwner().getRfcId(), NORMAL_COMMTYPE.READ, stbData.getRfcIndex(), idRead, 0, false, carrierLocId, null);
			requestEventList.add(e);
		} else {
			log(String.format("requestIdRead STBDATA IS NULL. carrierLocId[%s]idRead[%s] ", carrierLocId,idRead ) );
		}
	}
	
	/**
	 * Request Verify
	 * 
	 * @param carrierLocId
	 * @param timeoutSeconds
	 * @param hasCarrier
	 */
	public void requestVerify(String carrierLocId, int timeoutSeconds, boolean hasCarrier) {
		STBData stbData = stbMap.get(carrierLocId);
		if(stbData != null) {
			EventEntry e = new EventEntry(stbData.getOwner().getRfcId(), NORMAL_COMMTYPE.VERIFY, stbData.getRfcIndex(), false, timeoutSeconds, hasCarrier, carrierLocId, null);
			requestEventList.add(e);
		} else {
			log(String.format("requestVerify STBDATA IS NULL. carrierLocId[%s]timeoutSeconds[%d]hasCarrier[%s] ", carrierLocId,timeoutSeconds,hasCarrier ) );
		}
	}
	
	public void requestIdReadAll(String rfcId, boolean idRead, ArrayList<String> idReadPortList) {
		RFCData rfcData = rfcMap.get(rfcId);
		if(rfcData != null) {
			EventEntry e = new EventEntry(rfcId, NORMAL_COMMTYPE.READALL, -1, idRead, 0, false, null, idReadPortList);
			requestEventList.add(e);
		} else {
			log(String.format("requestIdReadAll RFCDATA IS NULL. rfcId[%s]idRead[%s] ", rfcId,idRead));
		}
	}
	
	/**
	 * Request IDRead
	 * 
	 * @param rfcId
	 * @param stbNumber
	 * @param idRead
	 */
	public void requestIdRead(String rfcId, int stbNumber, boolean idRead) {
		RFCData rfcData = rfcMap.get(rfcId);
		if(rfcData != null) {
			STBData stb = rfcData.getStb(stbNumber);
			if(stb != null) {
				EventEntry e = new EventEntry(rfcId, NORMAL_COMMTYPE.READ, stbNumber, idRead, 0, false, stb.getCarrierLocId(), null);
				requestEventList.add(e);
			} else {
				log(String.format("requestIdRead STBDATA IS NULL. rfcId[%s]stbNumber[%d]idRead[%s] ", rfcId,stbNumber,idRead) );
			}
		} else {
			log(String.format("requestIdRead RFCDATA IS NULL. rfcId[%s]stbNumber[%d]idRead[%s] ", rfcId,stbNumber,idRead) );
		}
	}
	
	/**
	 * Request Verify
	 * 
	 * @param rfcId
	 * @param stbNumber
	 * @param timeoutSeconds
	 * @param hasCarrier
	 */
	public void requestVerify(String rfcId, int stbNumber, int timeoutSeconds, boolean hasCarrier) {
		RFCData rfcData = rfcMap.get(rfcId);
		if(rfcData != null) {
			STBData stb = rfcData.getStb(stbNumber);
			if(stb != null) {
				EventEntry e = new EventEntry(rfcId, NORMAL_COMMTYPE.VERIFY, stbNumber, false, timeoutSeconds, hasCarrier, stb.getCarrierLocId(), null);
				requestEventList.add(e);
			} else {
				log(String.format("requestVerify STBDATA IS NULL. rfcId[%s]stbNumber[%d]timeoutSeconds[%d]hasCarrier[%s] ", rfcId ,stbNumber,timeoutSeconds,hasCarrier ) );
			}
		} else {
			log(String.format("requestVerify RFCDATA IS NULL. rfcId[%s]stbNumber[%d]timeoutSeconds[%d]hasCarrier[%s] ", rfcId ,stbNumber,timeoutSeconds,hasCarrier ) );
		}
	}
	
	public void requestStatus(String rfcId) {
		EventEntry e = new EventEntry(rfcId, NORMAL_COMMTYPE.STATUS, -1, false, 0, false, null, null);
		requestEventList.add(e);
	}
	
	public List<EventEntry> getCompletedEventList() {
		return completedEventList;
	}
	
	private void log(String s) {
		rfcLogger.debug(s);
	}
}
