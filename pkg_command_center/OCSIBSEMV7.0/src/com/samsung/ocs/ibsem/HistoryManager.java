package com.samsung.ocs.ibsem;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.log4j.Logger;

import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.manager.impl.IBSEMHistoryManager;
import com.samsung.ocs.manager.impl.OCSInfoManager;
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

public class HistoryManager extends AbstractOcsThread{
	private IBSEMHistoryManager ibsemHistoryManager;
	private OCSInfoManager ocsInfoManager;
	private SimpleDateFormat formatter;
	// 2018.03.12 by LSH: HISTORY 데이터 삭제 여부/주기 파라미터화
	private long lastHistoryDeletedTime = System.currentTimeMillis();
	
	/**
	 * Constructor of HistoryManager class.
	 */
	public HistoryManager() {
		ibsemHistoryManager = IBSEMHistoryManager.getInstance(null, null, false, false, 0);
		ocsInfoManager = OCSInfoManager.getInstance(OCSInfo.class, null, true, true, 500);
		formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	}
	
	@Override
	public String getThreadId() {
		// 2012.03.05 by PMM
//		return null;
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
//				calendar.add(Calendar.DATE, (-1)* ocsInfoManager.getHistoryHoldingPeriod());
//				String timeBefore = formatter.format(calendar.getTime());
//				ibsemHistoryManager.deleteIBSEMHistoryFromDB(timeBefore);
//			}
			if (ocsInfoManager.isIBSEMHistoryDeleteUsed()){
				if (System.currentTimeMillis() - lastHistoryDeletedTime > ocsInfoManager.getHistoryDeleteCheckPeriod()){
					try {
						Calendar calendar = Calendar.getInstance();
						calendar.add(Calendar.DATE, (-1)* ocsInfoManager.getHistoryHoldingPeriod());
						String timeBefore = formatter.format(calendar.getTime());
						ibsemHistoryManager.deleteIBSEMHistoryFromDB(timeBefore);
					} catch (Exception e) {
						traceException("mainProcessing()", e);
					} finally {
						lastHistoryDeletedTime = System.currentTimeMillis();
					}
				}
			}
		} catch (Exception e){
			traceException("mainProcessing()", e);
		}		
	}

	@Override
	protected void stopProcessing() {
		
	}

	private static Logger exceptionLog = Logger.getLogger("IBSEMException");
	public void traceException(String message, Throwable t) {
		exceptionLog.error(String.format("%s", message), t);
	}
}
