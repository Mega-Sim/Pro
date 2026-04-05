package com.samsung.ocs.ziplogs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.constant.OcsConstant;
import com.samsung.ocs.common.constant.OcsConstant.MODULE_STATE;
import com.samsung.ocs.common.thread.AbstractOcsThread;
import com.samsung.ocs.ziplogs.config.ZipLogsConfig;
import com.samsung.ocs.ziplogs.constant.ZipLogsConstant;

/**
 * CommonConfig Class, OCS 3.0 for Unified FAB
 * 
 * @author Byoungsoo.Kim
 * 
 * @date   2014. 7. 01.
 * @version 3.0
 * 
 * Copyright 2014 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public class ZipLogsManager extends AbstractOcsThread {
	
	private MODULE_STATE serviceState = MODULE_STATE.OUTOFSERVICE;
	private MODULE_STATE requestedSerivceState = MODULE_STATE.REQOUTOFSERVICE;
	
	private static List<File> zippedFileList = null;
	private static List<File> zippedDirList = null;
	
	private static String fileSeparator = File.separator;
	private static String backupDir = "./backup";
	
	private static ZipLogsConfig config = null;
	
	private long currentTime = 0;
	private int outOfServiceCheckCount;
	
	public ZipLogsManager() {
		fileSeparator = System.getProperty(OcsConstant.FILESEPARATOR);
		new LogManager(ZipLogsConstant.ZIPLOGS);
	}
	
	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {
		config = ZipLogsConfig.getInstance();
		try {
			config.loadModuleConfig();
		} catch (Exception e) {
			traceZipLogsException("Can not load config file.", e);
		}
		interval = config.getSleepTime() * ZipLogsConstant.MSEC_OF_MINUTE;
		outOfServiceCheckCount = 0;
	}

	@Override
	protected void mainProcessing() {
		// serviceState에 상관없이 동작하도록 수정
/*		manageServiceState();
		
		if (serviceState != requestedSerivceState) {
			if (requestedSerivceState == MODULE_STATE.INSERVICE)
				return;
			
			serviceState = requestedSerivceState;
		}
		
		if (serviceState == MODULE_STATE.INSERVICE)	{
			manageZipLogs();
			traceZipLogsMain("Sleep " + config.getSleepTime() + " min from now.");
		}*/
		manageZipLogs();
		traceZipLogsMain("Sleep " + config.getSleepTime() + " min from now.");
	}

	@Override
	protected void stopProcessing() {
		
	}
	
	public void manageZipLogs() {
		String zipPath = "";
		String zipFile = "";
		String zipAlias = "";
		
		try {
			currentTime = System.currentTimeMillis();

			String timeFormat = getTimeFormat();
			String timeFormat2 = getTimeFormat2();
			int curTime = Integer.parseInt(timeFormat);
			traceZipLogsMain("Current Time : " + timeFormat);

			for (int i=0; i < config.getRunTimeList().size(); i++) {
				int runTime = Integer.parseInt(config.getRunTimeList().get(i));

				traceZipLogsMain("Run Time[" + i + "] : " + config.getRunTimeList().get(i));
				traceZipLogsMain("Time Condition : " + ((curTime-runTime) < config.getSleepTime() && (curTime-runTime) >= 0));

				if ((curTime-runTime) < config.getSleepTime() && (curTime-runTime) >= 0) {
					for (int j=0; j < config.getOcsModuleList().size(); j++) {
						try {
							zipPath = config.getOcsModuleList().get(j);
							zipAlias = zipPath.substring(zipPath.lastIndexOf(fileSeparator)+1, zipPath.length());
							zipPath = zipPath + fileSeparator + "log";
							zipFile = backupDir + fileSeparator + zipAlias+ "_" + timeFormat2 + ".zip";
							boolean isZipped = false;

							// zip log files
							if (config.getZipTime() > 0) { 
								traceZipLogsMain("Start zipping log files.");
								traceZipLogsMain("    Input = " + zipPath);
								traceZipLogsMain("    Output = " + zipFile);

								long startTime = System.currentTimeMillis();
								isZipped = zip(zipPath, zipFile);
								long endTime = System.currentTimeMillis();
								traceZipLogsMain("    Elapsed time : " + (endTime - startTime) +" msec");

								// delete zipped log files
								traceZipLogsMain("Start deleting zipped files.");
								if (isZipped) { 
									delZipped();
								}
								traceZipLogsMain("End deleting zipped files.");
								traceZipLogsMain("End zipping log files.");
							} else {
								traceZipLogsMain("Zip Time is invalid.");
							}
						} catch (Throwable e) {
							traceZipLogsMain("    " + zipPath + " has no log files.");
							traceZipLogsMain("    Check out config file.");
							checkEmpty(backupDir);
						}
					}
					// delete old log files
					if (config.getDelTime() > 0) {
						traceZipLogsMain("Start deleting old zip files.");
						checkPeriod(backupDir);
						traceZipLogsMain("End deleting old zip files.");
					} else {
						traceZipLogsMain("Del Time is invalid.");
					}

					// check zip files limit
					if (config.getZipLimit() > 0) { 
						traceZipLogsMain("Start checking size of zip files.");
						checkLimitSize(backupDir);
						traceZipLogsMain("End checking size of zip files.");
					} else {
						traceZipLogsMain("Zip Limit is invalid.");
					}
					traceZipLogsMain("======================================================================");
				}
			}
		} catch (Throwable e) {
			traceZipLogsException("Error ocurrs while zipping log files", e);
		}
	}
	
	public boolean zip(String sourcePath, String zipFile) throws Exception {
		File sourceFile = new File(sourcePath);

		// 압축 대상(sourcePath)이 디렉토리나 파일이 아니면 리턴한다.
		if (!sourceFile.isFile() && !sourceFile.isDirectory()) {
			traceZipLogsMain("    Can not read files or directories. - zip()");
			return false;
		}
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		ZipOutputStream zos = null;
		zippedFileList = new ArrayList<File>();

		try {
			fos = new FileOutputStream(zipFile); // FileOutputStream
			bos = new BufferedOutputStream(fos); // BufferedStream
			zos = new ZipOutputStream(bos); // ZipOutputStream
			zos.setLevel(ZipLogsConstant.COMPRESSION_LEVEL); // 압축 레벨 - 최대 압축률은 9, 디폴트 8
			zipEntry(sourceFile, sourcePath, zos); // Zip 파일 생성
			zos.finish(); // ZipOutputStream finish
		} finally {
			if (zos != null) {
				zos.close();
			}
			if (bos != null) {
				bos.close();
			}
			if (fos != null) {
				fos.close();
			}
		}
		return true;
	}

	private void zipEntry(File sourceFile, String sourcePath, ZipOutputStream zos) throws Exception {
		if (sourceFile.isDirectory()) { // sourceFile 이 디렉토리인 경우 하위 파일 리스트 가져와 재귀호출
			File[] fileArray = sourceFile.listFiles(); // sourceFile 의 하위 파일 리스트
			for (int i = 0; i < fileArray.length; i++) {
				zipEntry(fileArray[i], sourcePath, zos); // 재귀 호출
			}
		} else { // sourcehFile 이 디렉토리가 아닌 경우
			BufferedInputStream bis = null;
			try {
				String sFilePath = sourceFile.getPath();
				String zipEntryName = sFilePath.substring(sourcePath.length() + 1, sFilePath.length());
				
				long lastModifiedTime = sourceFile.lastModified(); // 파일의 마지막 수정 시간
				
				// 압축할 파일의 확장자 확인
				if (zipEntryName.endsWith(".zip") || zipEntryName.indexOf(".log") < 0) { 
					return;
				}

				if ((currentTime-lastModifiedTime) >= (ZipLogsConstant.MSEC_OF_HOUR*config.getZipTime())) {
					bis = new BufferedInputStream(new FileInputStream(sourceFile));
					ZipEntry zentry = new ZipEntry(zipEntryName);
					zentry.setTime(sourceFile.lastModified());
					zos.putNextEntry(zentry);
					zippedFileList.add(sourceFile);

					byte[] buffer = new byte[ZipLogsConstant.BUFFER_SIZE];
					int cnt = 0;
					long index = 0;
					while ((cnt = bis.read(buffer, 0, ZipLogsConstant.BUFFER_SIZE)) != -1) {
						if(++index > 10) {
							Thread.sleep(1);
							index = 0;
						}
						zos.write(buffer, 0, cnt);
					}
					zos.closeEntry();
				} else {
					return;
				}
			} finally {
				if (bis != null) {
					bis.close();
				}
			}
		}
	}

	public void delZipped() throws Exception {
		zippedDirList = new ArrayList<File>();

		// delete log files
		for (int i = 0; i < zippedFileList.size(); i++) {
			String path = zippedFileList.get(i).toString();
			File tmpDir = new File(path.substring(0, path.lastIndexOf(fileSeparator)));
			if(!zippedDirList.contains(tmpDir)); 
				zippedDirList.add(tmpDir);

			File tmpFile = (File) zippedFileList.get(i);
			// 2015.04.22 by KBS : 압축된 파일의 삭제 기준을 Modify 시간에서 File 시간으로 변경
			// tmpFile.delete();
			deleteLog(tmpFile);
		}

		// delete log directories
		for (int i = 0; i < zippedDirList.size(); i++) {
			File tmpDir = zippedDirList.get(i);
			if (!tmpDir.toString().endsWith(fileSeparator + "log")) {
				tmpDir.delete();
			}
		}
	}

	public void checkPeriod(String sourcePath) throws Exception {
		File sourceFile = new File(sourcePath);
		File[] fileArray = sourceFile.listFiles(); // sourceFile 의 하위 파일 리스트

		// 압축 대상(sourcePath)이 디렉토리나 파일이 아니면 리턴한다.
		if (!sourceFile.isFile() && !sourceFile.isDirectory()) {
			traceZipLogsMain("    Can not read files or directories. - checkPeriod()");
			return;
		}

		try {
			for (int i = 0; i < fileArray.length; i++) {
				if (fileArray[i].isFile()) {
					String sFilePath = fileArray[i].getPath();
					String delEntryName = sFilePath.substring(sourcePath.length() + 1, sFilePath.length());
					long lastModifiedTime = fileArray[i].lastModified(); // 파일의 마지막 수정 시간

					// 삭제할 파일의 확장자 확인
					if (delEntryName.endsWith(".zip")) {
						if((currentTime-lastModifiedTime) >= (ZipLogsConstant.MSEC_OF_HOUR*config.getDelTime())) {
							traceZipLogsMain("    Deleted : " + sourcePath + fileSeparator + delEntryName);
							fileArray[i].delete();
						} else if (fileArray[i].length() == 0) {
							traceZipLogsMain("    Deleted(0) : " + sourcePath + fileSeparator + delEntryName);
							fileArray[i].delete();
						}
					} else {
						return;
					}
				}
			}
		} finally {
		}
	}


	public void checkLimitSize(String sourcePath) throws Exception {
		File sourceFile = new File(sourcePath);
		File[] fileArray = sourceFile.listFiles(); // sourceFile 의 하위 파일 리스트
		List<File> zipFileList = new ArrayList<File>();
		long totalSize = 0;

		// 압축 대상(sourcePath)이 디렉토리나 파일이 아니면 리턴한다.
		if (!sourceFile.isFile() && !sourceFile.isDirectory()) {
			traceZipLogsMain("    Can not read files or directories. - checkLimitSize()");
			return;
		}

		try {
			for (int i = 0; i < fileArray.length; i++) {
				if (fileArray[i].isFile()) {
					String sFilePath = fileArray[i].getPath();

					// 삭제할 파일의 확장자 확인
					if (sFilePath.endsWith(".zip")) { 
						zipFileList.add(fileArray[i]);
						totalSize += fileArray[i].length();
					}
				}
			}
			Collections.sort(zipFileList, new Comparator<File>() {
				public int compare(File o1, File o2) {
					return (int)(o1.lastModified() - o2.lastModified());
				}
			});
			
			traceZipLogsMain("    Total size : " + totalSize + " Bytes");
			traceZipLogsMain("    Limit size : " + config.getZipLimit()*ZipLogsConstant.GIGA_SIZE + " Bytes");
			if (totalSize >= (config.getZipLimit()*ZipLogsConstant.GIGA_SIZE)) {
				for (File zipFile : zipFileList) {
					long zipSize = zipFile.length();
					if (zipFile.delete() == true) {
						totalSize -= zipSize;
						traceZipLogsMain("    Deleting old zip file : " + zipFile + " [" + zipSize + "] Bytes");
						if (totalSize < (config.getZipLimit()*ZipLogsConstant.GIGA_SIZE)) {
							return;
						}
					} else {
						traceZipLogsMain("    Can not delete old zip file :  " + zipFile);
						return;
					}
				}
			}
		} finally {
		}
	}

	public void checkEmpty(String sourcePath) throws Exception {
		File sourceFile = new File(sourcePath);
		File[] fileArray = sourceFile.listFiles(); // sourceFile 의 하위 파일 리스트
		List<File> zipFileList = new ArrayList<File>();

		// 압축 대상(sourcePath)이 디렉토리나 파일이 아니면 리턴한다.
		if (!sourceFile.isFile() && !sourceFile.isDirectory()) {
			traceZipLogsMain("    Can not delete empty zip files. - checkEmpty()");
			return;
		}

		try {
			for (int i = 0; i < fileArray.length; i++) {
				if (fileArray[i].isFile()) {
					String sFilePath = fileArray[i].getPath();

					// 삭제할 파일의 확장자 확인
					if (sFilePath.endsWith(".zip")) { 
						if (fileArray[i].length() == 0) {
							zipFileList.add(fileArray[i]);
						}
					}
				}
			}
			
			for (File zipFile : zipFileList) {
				if (zipFile.delete() == true) {
					traceZipLogsMain("    Deleting empty zip file : " + zipFile);
				}
			}
		} finally {
		}
	}
	
	public void requestChangeServiceState(MODULE_STATE requestedState) {
		this.requestedSerivceState = requestedState;
	}
	
	public MODULE_STATE getServiceState() {
		return this.serviceState;
	}
	
/*	private void manageServiceState() {
		if (this.serviceState != MODULE_STATE.OUTOFSERVICE && 
				this.requestedSerivceState == MODULE_STATE.REQOUTOFSERVICE) {
			changeServiceState(MODULE_STATE.OUTOFSERVICE);
			traceZipLogsMain("Deactivated!");
		} else if (this.serviceState != MODULE_STATE.INSERVICE && 
				this.requestedSerivceState == MODULE_STATE.REQINSERVICE) {
			changeServiceState(MODULE_STATE.INSERVICE);
			traceZipLogsMain("Activated!");
		}
		
		if (this.serviceState == MODULE_STATE.OUTOFSERVICE) {
			if (++outOfServiceCheckCount >= 5) {
				traceZipLogsMain("      [OUTOFSERVICE]");
				outOfServiceCheckCount = 0;
			}
		}
	}
	
	private void changeServiceState(MODULE_STATE state) {
		this.requestedSerivceState = state;
		this.serviceState = state;
	}*/
	
	private static final String PAST_PATTERN = "^(([a-zA-Z0-9]|_|-)*[a-zA-Z]|([a-zA-Z0-9]|_|-)*[a-zA-Z][0-9]).log$";
	private static final Pattern pattern = Pattern.compile(PAST_PATTERN);
	 
	private boolean isInitFile(String fileName) {
		Matcher matcher = pattern.matcher(fileName);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}

	private static final String SECS_PATTERN = "^[0-9]{2}-hour.log$";
	private static final Pattern secs_pattern = Pattern.compile(SECS_PATTERN);
	 
	private boolean isSecsFile(String fileName) {
		Matcher matcher = secs_pattern.matcher(fileName);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
	
	private void deleteLog(File file) {
		if (isInitFile(file.getName()) == false) {
			file.delete();
		} else {
			// 2017.02.17 by KBS: SECS 로그 삭제 보완
			if (isSecsFile(file.getName()) == true) {
				file.delete();
			} else {
				// nothing to do: init log file
			}
		}
	}
	
	private String getTimeFormat() {
		SimpleDateFormat formatter = new SimpleDateFormat("HHmm");
		Date time = new Date();
		String dTime = formatter.format(time);
		
		return dTime;
	}

	private String getTimeFormat2() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmm");
		Date time = new Date(System.currentTimeMillis() - config.getZipTime()*ZipLogsConstant.MSEC_OF_HOUR);
		String dTime = formatter.format(time);

		return dTime;
	}
	
	private static Logger zipLogsMainLog = Logger.getLogger(ZipLogsConstant.ZIPLOGS_MAIN);
	private static Logger zipLogsExceptionLog = Logger.getLogger(ZipLogsConstant.ZIPLOGS_EXCEPTION);
	
	public void traceZipLogsMain(String message) {
		zipLogsMainLog.debug(message);
	}

	public void traceZipLogsException(String message, Throwable e) {
		zipLogsExceptionLog.error(message, e);
	}
	
}
