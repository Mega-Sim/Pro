package com.samsung.ocs.unitdevice;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.samsung.ocs.unitdevice.model.FFU;

import org.apache.log4j.Logger;

public class FFUOperation
{
	FFUManager ffuManager = null;

	private FFUClientComm ffuServerComm = null;
	private ConcurrentHashMap<String, FFU> ffuData = null;
	
	private OperationThread FFUOperationThread = null;

	private String ffuServerId = ""; // FFU Server Id
	private String ipAddress = "127.0.0.1";
	
	private long lFFUGroupReportTimeout = 10000;
	
	private static final int FFU_PORT = 7001; // FFU Port Number
	private int socketPort = FFU_PORT;
	private int threadInterval = 1000; // OperationThread의 Interval
	
	private static final String FFU_STATUS_RUN = "R";
	private static final String FFU_STATUS_STOP = "S";
	private static final String FFU_STATUS_WARNING = "W";
	
	private static final String ERRORCODE_CLEAR = "0";
	private static final String ERRORCODE_OMISSION_STATUS_REPORT = "2000";
	private static final String ERRORCODE_FFUSERVER_COMFAIL = "1000";

	private static final String FFU_OPERATION_TRACE = "FFUOperationDebug";
	private static final String FFU_OPERATION_EXCEPTION_TRACE = "FFUOperationException";
	private static Logger operationTraceLog = Logger.getLogger(FFU_OPERATION_TRACE);
	private static Logger operationExceptionTraceLog = Logger.getLogger(FFU_OPERATION_EXCEPTION_TRACE);
	
	
	public FFUOperation(FFUManager ffuManager) {
		this.ffuManager = ffuManager;

		this.ffuServerComm = new FFUClientComm(this);
		this.ffuData = new ConcurrentHashMap<String, FFU>();
	}

	class OperationThread extends Thread {
		public boolean m_bRun = true;

		public void run() {
			while (m_bRun) {
				try {
					OperationProcess();
					sleep(threadInterval);
				} catch (Exception e) {
					operationTrace("OperationThread ", e);
				}
			}
			System.out.println(ipAddress + ": OprationThread is dead.");
			operationTrace(ipAddress + ": OprationThread is dead.", null);
		}
	}

	public void ReceivedStatusMsg(String name, int ffuCnt, String ffuGroupid[], String strStatus[], int nTotalFFU[], int nAbnormalFFU[], int nCommfailFFU[]) {
		
		lFFUGroupReportTimeout = ffuManager.getFFUGroupReportChecktime();
		
		// FFU 정보가 유효하지 않은 경우
		if (name.equals(ffuServerId) == false) {
			StringBuffer log = new StringBuffer(); 
			log.append("[ReceivedStatusMsg] FFUServerID mismatch: ").append(name);
			operationTrace(log.toString(), null);
			return;
		}
		
		// 16.04.26 LSH: DB에 등록되어 있지만, STATUS 보고받지 못한 FFU GROUP은 ERRORCODE SET
		for (String o: ffuData.keySet()){
			if(!Arrays.asList(ffuGroupid).contains(o)){
				FFU ffu = ffuData.get(o);
				if (ffu != null && (System.currentTimeMillis() - ffu.getLastReportedTime() > lFFUGroupReportTimeout)){
					ffu.setErrorCode(ERRORCODE_OMISSION_STATUS_REPORT);
					// 16.09.06 LSH: Report 누락 FFU Group도 Lane-Cut 차단 Rule 추가
					ffuManager.setNodeEnable(ffu.getNodeId(), false);
					ffuManager.addStatusToUpdateList(ffu);
				}
			}
		}
		
		for (int i = 0; i < ffuCnt; i++) {
			StringBuffer log = new StringBuffer(); 
			log.append(ffuGroupid[i]).append(",Status:").append(strStatus[i]);
			log.append(",Total_FFU:").append(nTotalFFU[i]);
			log.append(",Abnormal_FFU:").append(nAbnormalFFU[i]);
			log.append(",Commfail_FFU:").append(nCommfailFFU[i]);
			operationTrace(log.toString(), null);

			try {
				FFU ffu = ffuData.get(ffuGroupid[i]);
				if (ffu != null) {
					ffu.setPrevStatus(ffu.getStatus());
					ffu.setStatus(strStatus[i]);
					ffu.setTotalFFU(nTotalFFU[i]);
					ffu.setAbnormalFFU(nAbnormalFFU[i]);
					ffu.setCommfailFFU(nCommfailFFU[i]);
					ffu.setPrevErrorCode(ffu.getErrorCode());
					ffu.setErrorCode(ERRORCODE_CLEAR);
					ffu.setLastReportedTime(System.currentTimeMillis());
					ffuManager.addStatusToUpdateList(ffu);
				}
				// FFU 장애회피 제어
				// 16.04.26 LSH: 사용자가 이미 Disable한 Node는 막지 않고 있다가, 사용자가 Enable 한 경우 다시 막기 위해, Node Disable은 항상 시도
				// 이전과 status나 ErrorCode가 바뀌었을 경우만 node enable 제어
				if(ffu != null){
					if (!ffu.getStatus().equals(ffu.getPrevStatus()) || ffu.getErrorCode()!=ffu.getPrevErrorCode()) {
						if ((FFU_STATUS_RUN.equals(ffu.getStatus()) || FFU_STATUS_WARNING.equals(ffu.getStatus()))
								&& ERRORCODE_CLEAR.equals(String.valueOf(ffu.getErrorCode()))) {
							ffuManager.setNodeEnable(ffu.getNodeId(), true);
						} else {
							// nothing to do
						}
					}
					if (FFU_STATUS_STOP.equals(ffu.getStatus())) {
						ffuManager.setNodeEnable(ffu.getNodeId(), false);
					}
				}
			} catch (Exception e) {
				operationTrace("ReceivedStatusMsg ", e);
			}
		}
	}
	
	
	public boolean addFFU(FFU ffu) {
		if (ffuData.get(ffu.getFFUGroupId()) == null) {
			ffuData.put(ffu.getFFUGroupId(), ffu);
			return true;
		}
		return false;
	}

	int getFFUCount() {
		return ffuData.size();
	}
	
	public ConcurrentHashMap<String, FFU> getFFUList() {
		return ffuData;
	}
	
	public FFU removeFFU(String ffuGroupId) {
		return ffuData.remove(ffuGroupId);
	}
	
//	public boolean removeFFU(String ffuGroupId) {
//		if (this.ffu.getFFUGroupId().equals(ffuGroupId)) {
//			this.ffu = null;
//			return true;
//		} else
//			return false;		
//	}

	public String getIpAddress() {
		return ipAddress;
	}
	
	public void SetOperationInfo(String strName, String strIPAddress) {
		// Operation에서 FFUComm 모듈의 FFUServer정보를 설정
		this.ffuServerId = strName;		
		this.ipAddress = strIPAddress;
		ffuServerComm.SetTargetInfo(strName, strIPAddress, socketPort);
	}

	/**
	 * Operation을 관리하는 Timer를 시작한다.
	 */
	public void OperationStart()
	{
		// 16.09.06 LSH: Commfail 상태로 초기화 하지 않고, 실제 Commfail 발생 시 처리하도록 수정
//		UpdateCommFail();
		
		// Main Operation Timer Thread 생성 및 실행
		FFUOperationThread = new OperationThread();
		FFUOperationThread.start();
		
		ffuServerComm.ffuClientCommStart();
	}

	/**
	 * Operation을 관리하는 Timer를 종료한다.
	 */
	public void OperationStop()
	{
		if (FFUOperationThread != null)
		{
			// Operation 정리
			FFUOperationThread.m_bRun = false;
			ffuServerComm.ffuClientCommStop();

			FFUOperationThread = null;
			ffuServerComm = null;
			
			manageFFUMonitoringControl();
		}
	}

	/**
	 * Operation에서 처리할 Process를 관리한다.
	 */
	void OperationProcess()
	{
		manageRequestFFUStatus();
		manageFFUServerCommfailControl();
	}
	
	private void manageRequestFFUStatus() {
		
	}
	
	private void manageFFUMonitoringControl() {
		
		boolean isFFUMonitoringControlUsed = ffuManager.isFFUMonitoringControlUsed();
		
		if (!isFFUMonitoringControlUsed){
			for (Enumeration<FFU> e = ffuData.elements(); e.hasMoreElements();) {
				FFU ffu = e.nextElement();
				if (ffu != null) {
					// 16.09.06 LSH: FFU_MONITORING_CONTROL_USAGE가 NO로 변경 될 때, ErrorCode 정리
					ffu.setErrorCode(ERRORCODE_CLEAR);
					ffuManager.setNodeEnable(ffu.getNodeId(), true);
				}
			}
			// 16.09.06 LSH: FFU_MONITORING_CONTROL_USAGE가 NO로 변경 될 때, ErrorCode 정리
			ffuManager.updateErrorCodeClearToDB();
		}
	}
	
	private void manageFFUServerCommfailControl() {
		
		boolean isCommfailControlUsed = ffuManager.isFFUServerCommfailControlUsed();

		for (Enumeration<FFU> e = ffuData.elements(); e.hasMoreElements();) {
			FFU ffu = e.nextElement();
			if (ffu != null) {
				if (ERRORCODE_FFUSERVER_COMFAIL.equals(String.valueOf(ffu.getErrorCode()))) {
					// FFU Server와 통신 fail 일 때
					if (isCommfailControlUsed) {
						ffuManager.setNodeEnable(ffu.getNodeId(), false);
					} else{
						if (FFU_STATUS_RUN.equals(ffu.getStatus()) || FFU_STATUS_WARNING.equals(ffu.getStatus())) {
							// FFUSERVER_COMMFAIL_CONTROL_USAGE가 FALSE 이면서 DB에 저장된 마지막 Status가 STOP이 아닌 경우 Node Enable 전환
							ffuManager.setNodeEnable(ffu.getNodeId(), true);
						} else if (FFU_STATUS_STOP.equals(ffu.getStatus())) {
							// 16.09.06 LSH: FFUSERVER_COMMFAIL_CONTROL_USAGE가 FALSE 이면서 DB에 저장된 마지막 Status가 STOP인 경우 Node Disable 전환
							ffuManager.setNodeEnable(ffu.getNodeId(), false);
						}
					}
				}
			}
		}
	}
	
	public void UpdateCommFail() {
		// 16.09.06 LSH: FFU_MONITORING_CONTROL_USAGE가 YES일 때만, Commfail 처리하도록 수정
		boolean isFFUMonitoringControlUsed = ffuManager.isFFUMonitoringControlUsed();
		
		if (isFFUMonitoringControlUsed){
			ffuManager.addCommfailToUpdateList(ipAddress);
			for (Enumeration<FFU> e = ffuData.elements(); e.hasMoreElements();) {
				FFU ffu = e.nextElement();
				if (ffu != null) {
					ffu.setErrorCode(ERRORCODE_FFUSERVER_COMFAIL);
				}
			}
		}
	}

	/**
	 * 통신에 대한 이력을 저장한다.
	 */
	private void operationTrace(String message, Throwable e) {
		if (e == null) {
			operationTraceLog.debug(message);
		} else {
			operationExceptionTraceLog.error(message, e);
		}
	}
}
