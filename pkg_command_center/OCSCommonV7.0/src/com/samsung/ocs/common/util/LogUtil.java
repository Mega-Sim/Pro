package com.samsung.ocs.common.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.samsung.xcs.commons.logger.XCSDailyRollingFileAppender;

/**
 * LogUtil Class, OCS 3.0 for Unified FAB
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

public class LogUtil {
	/**
	 * 
	 * @param fileName
	 * @param log
	 * @param hourly
	 */
	public static void writeLog(String fileName, String log, boolean hourly) {
		Logger logger = Logger.getLogger(fileName);
		// 아래꺼는 순구형꺼.. 복사해옴.
		// 등록된 Logger가 없는 경우 새로 생성
        if (logger.getAllAppenders().hasMoreElements() == false) {
            
            // Log File에 대한 패턴 정의
            String pattern = "%d{yyyy-MM-dd HH:mm:ss:SSS} %-5p - %m%n";
            PatternLayout layout = new PatternLayout(pattern);
            
            // 생성될 로그 파일의 이름(조건에 따라 로그 파일이름을 따라 다르게 ...)
//            fileName = "/log/" + fileName + "/";
            fileName = "/log/" + fileName;

            Appender appender = null;
            
            // 날짜 패턴에 따라 추가될 파일이름
            String datePattern = "yyyyMMddHH";
            try {
                appender = new XCSDailyRollingFileAppender(layout, fileName, datePattern, logger.getAdditivity());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            logger.addAppender(appender);
            
            // Console Appender 추가
            appender = new ConsoleAppender(layout);
            logger.addAppender(appender);
        }
		logger.debug(log);
	}
	
	/**
	 * 
	 * @param fileName
	 * @param e
	 * @param hourly
	 */
	public static void writeLog(String fileName, Throwable e, boolean hourly) {
		Logger logger = Logger.getLogger(fileName);
		// 아래꺼는 순구형꺼.. 복사해옴.
		// 등록된 Logger가 없는 경우 새로 생성
        if (logger.getAllAppenders().hasMoreElements() == false) {
            
            // Log File에 대한 패턴 정의
            String pattern = "%d{yyyy-MM-dd HH:mm:ss:SSS} %-5p - %m%n";
            PatternLayout layout = new PatternLayout(pattern);
            
            // 생성될 로그 파일의 이름(조건에 따라 로그 파일이름을 따라 다르게 ...)
//            fileName = "/log/" + fileName + "/";
            fileName = "/log/" + fileName;

            Appender appender = null;
            
            // 날짜 패턴에 따라 추가될 파일이름
            String datePattern = "yyyyMMddHH";
            try {
                appender = new XCSDailyRollingFileAppender(layout, fileName, datePattern, logger.getAdditivity());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            logger.addAppender(appender);
            
            // Console Appender 추가
            appender = new ConsoleAppender(layout);
            logger.addAppender(appender);
        }
//		logger.debug(e.getMessage(), e);
		logger.debug(stackTraceToString(e));
	}
	
	/**
	 * 
	 * @param e
	 * @return
	 */
	public static String stackTraceToString(Throwable e) {
		String message = null;
		StringWriter sw = null;
		PrintWriter pw = null;
		
		try {
			sw = new StringWriter();
			pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			message = sw.toString();
		} catch (Exception es) {
			message = e.getMessage();
		}
		finally {
			if (pw != null) {
				try { pw.close();} catch (Exception e1){}
				pw = null;
			}
			if (sw != null) {
				try { sw.close();} catch (Exception e2){}
				sw = null;
			}
		}
		return message;
	}
}
