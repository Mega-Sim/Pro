package com.samsung.ocs.operation;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.manager.impl.EventHistoryManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
import com.samsung.ocs.manager.impl.TrCompletionHistoryManager;
import com.samsung.ocs.manager.impl.VehicleErrorHistoryManager;
import com.samsung.ocs.manager.impl.model.OCSInfo;

/**
 * HistoryManager Class, OCS 3.0 for Unified FAB
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

public class HistoryManager extends AbstractOcsThread {
	private OCSInfoManager ocsInfoManager = null;
	private EventHistoryManager eventHistoryManager = null;
	private TrCompletionHistoryManager trCompletionHistoryManager = null;
	private VehicleErrorHistoryManager vehicleErrorHistoryManager = null;
	private static Logger exceptionLog = Logger.getLogger("OperationException");
	private SimpleDateFormat formatter = null;
	// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화
	private long lastEventHistoryDeletedTime = System.currentTimeMillis();
	private long lastTrCompletionHistoryDeletedTime = System.currentTimeMillis();
	private long lastVehicleErrorHistoryDeletedTime = System.currentTimeMillis();
	
	/**
	 * Constructor of HistoryManager class.
	 */
	public HistoryManager() {
		this.ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, null, true, true, 200);
		this.eventHistoryManager = EventHistoryManager.getInstance(null, null, false, true, 0);
		this.trCompletionHistoryManager = TrCompletionHistoryManager.getInstance(null, null, false, false, 0);
		this.vehicleErrorHistoryManager = VehicleErrorHistoryManager.getInstance(null, null, false, false, 0);
		formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	}

	@Override
	public String getThreadId() {
		// 2012.03.07 by PMM
//		StringBuffer info = new StringBuffer();
//		info.append("ClassName:").append(this.getClass().getName()).append(", ThreadId:").append(getThreadId());
//		return info.toString();
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		interval = 1000;
		this.start();
	}

	/**
	 * Main Processing Method.
	 */
	@Override
	protected void mainProcessing() {
		try {
			// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화 
//			Calendar calendar = Calendar.getInstance();
//			if (calendar.get(Calendar.MINUTE) == 0 && calendar.get(Calendar.SECOND) == 0) {
//				calendar.add(Calendar.DATE, (-1) * ocsInfoManager.getHistoryHoldingPeriod());
//				String timeBefore = formatter.format(calendar.getTime());
//				vehicleErrorHistoryManager.deleteHistoryFromDB(timeBefore);
//				eventHistoryManager.deleteHistoryFromDB(timeBefore);
//				trCompletionHistoryManager.deleteHistoryFromDB(timeBefore);
//			}
			if (ocsInfoManager.isVehicleErrorHistoryDeleteUsed()){
				if (System.currentTimeMillis() - lastVehicleErrorHistoryDeletedTime > ocsInfoManager.getHistoryDeleteCheckPeriod()){
					try {
						Calendar calendar = Calendar.getInstance();
						calendar.add(Calendar.DATE, (-1) * ocsInfoManager.getHistoryHoldingPeriod());
						String timeBefore = formatter.format(calendar.getTime());
						vehicleErrorHistoryManager.deleteHistoryFromDB(timeBefore);
					} catch (Exception e){
						traceException("mainProcessing()", e);
					} finally {
						lastVehicleErrorHistoryDeletedTime = System.currentTimeMillis();
					}
				}
			}
			if (ocsInfoManager.isEventHistoryDeleteUsed()){
				if (System.currentTimeMillis() - lastEventHistoryDeletedTime > ocsInfoManager.getHistoryDeleteCheckPeriod()){
					try {
						Calendar calendar = Calendar.getInstance();
						calendar.add(Calendar.DATE, (-1) * ocsInfoManager.getHistoryHoldingPeriod());
						String timeBefore = formatter.format(calendar.getTime());
						eventHistoryManager.deleteHistoryFromDB(timeBefore);
					} catch (Exception e){
						traceException("mainProcessing()", e);
					} finally {
						lastEventHistoryDeletedTime = System.currentTimeMillis();
					}
				}
			}
			if (ocsInfoManager.isTrCompletionHistoryDeleteUsed()){
				if (System.currentTimeMillis() - lastTrCompletionHistoryDeletedTime > ocsInfoManager.getHistoryDeleteCheckPeriod()){
					try {
						Calendar calendar = Calendar.getInstance();
						calendar.add(Calendar.DATE, (-1) * ocsInfoManager.getHistoryHoldingPeriod());
						String timeBefore = formatter.format(calendar.getTime());
						trCompletionHistoryManager.deleteHistoryFromDB(timeBefore);
					} catch (Exception e){
						traceException("mainProcessing()", e);
					} finally {
						lastTrCompletionHistoryDeletedTime = System.currentTimeMillis();
					}
				}
			}
		} catch (Exception e) {
			traceException("mainProcessing()", e);
		}
	}

	@Override
	protected void stopProcessing() {
		
	}
	
	public void traceException(String message) {
		exceptionLog.error(String.format("%s", message));
	}
	public void traceException(String message, Throwable t) {
		exceptionLog.error(String.format("%s", message), t);
	}
}
