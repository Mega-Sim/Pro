package ocsmanager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import jxl.Sheet;
import jxl.Workbook;
import ocsmanager.model.TransferCommand;

/**
 *
 * <p>
 * Title: OCS Manager
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * <p>
 * Company: 삼성전자 기술총괄 메카트로닉스연구소
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public class LongRun {
	public class TransferThread extends Thread {
		public double m_dAccel = 1;
		public boolean m_bStart = false;
		public int m_nStageInterval = 600;
		// 2012.01.25 by LWG [Cancel/Abort 내리기]
		public int cancelInterval = 600;
		public int abortInterval = 600;

		public int m_nThreadInterval = 1000;

		public void run() {
			long lThreadStartTime = 0;
			long lThreadEndTime = 0;
			// 2012.01.25 by LWG [Cancel/Abort 내리기]
			long lastSendCancelCmd = 0;
			long lastSendAbortCmd = 0;

			while (true) {
				try {
					lThreadStartTime = System.currentTimeMillis();
					if (m_bStart == true) {
						m_dThreadClock += (m_nThreadInterval / 1000 * m_dAccel);
						if (m_bUseStageCmd == true) {
							SendStageCmdProces();
						}

						SendTransferCmdProces();

						// 2012.01.25 by LWG [Cancel/Abort 내리기]
						if (useCancelCmd == true && ((System.currentTimeMillis() - lastSendCancelCmd) / 1000) >= cancelInterval) {
							sendCancelCmdProcess();
							lastSendCancelCmd = System.currentTimeMillis();
						}
						if (useAbortCmd == true && ((System.currentTimeMillis() - lastSendAbortCmd) / 1000) >= abortInterval) {
							sendAbortCmdProcess();
							lastSendAbortCmd = System.currentTimeMillis();
						}

						longrunManager.removeOldTrCmd();
					}
					lThreadEndTime = System.currentTimeMillis();
					sleep(m_nThreadInterval - (lThreadEndTime - lThreadStartTime));
				} catch (Exception e) {
					UtilLog.WriteReturnLog("LongRunException", "TransferThread - Exception", "", false);
				}
			}
		}
	}

	Hashtable stagePortList = new Hashtable();
	Hashtable sendStageCmdList = new Hashtable();
	Vector stageCmdList = new Vector();

	Vector m_vtTransferCmdList = new Vector();
	Hashtable m_htStageCmdList = new Hashtable();
	int m_nIndex = 0;
	int stageIndex = 0;
	double m_dThreadClock = 0;
	boolean m_bUseStageCmd = false;
	//2012.01.25 by LWG [Cancel/Abort 내리기]
	private boolean useAbortCmd = false;
	private boolean useCancelCmd = false;
	private int expectedDuration = 80;
	TransferThread m_TransferThread = null;
	OCSManagerMain m_ocsMain = null;
	utilLog UtilLog = null;
	private LongRunManager longrunManager = null;

	/**
	 * 생성자
	 *
	 * @param utillog
	 *            utilLog
	 */
	public LongRun(OCSManagerMain ocsMain, utilLog utillog) {
		m_ocsMain = ocsMain;
		UtilLog = utillog;
		m_TransferThread = new TransferThread();
		m_TransferThread.start();
		this.longrunManager = ocsMain.getLongRunManager();
	}

	/**
	 * 
	 */
	public void sendAbortCmdProcess() {
		List list = longrunManager.getAbortCandidateList();
		if (list.size() == 0) {
			return;
		}
		Random random = new Random(System.currentTimeMillis());
		int idx = random.nextInt(list.size());
		String trcmdId = (String) list.get(idx);
		TransferCommand trcmd = longrunManager.getTrcmd(trcmdId);
		if (trcmd != null) {
			MyHashtable htCommandInfo = new MyHashtable();
			htCommandInfo.put("TSC", "TOCS411");
			htCommandInfo.put("MicroTrCmdType", "ABORT");
			htCommandInfo.put("MicroTrCmdID", trcmd.getTrCmdID());
			System.out.println("ABORT : " + trcmd.getTrCmdID());
			m_ocsMain.semIF.SendMicroTC(htCommandInfo);
		}
		longrunManager.transferCompleted(trcmdId);
	}

	/**
	 * 
	 */
	public void sendCancelCmdProcess() {
		List list = longrunManager.getCancelCandidateList();
		if (list.size() == 0) {
			return;
		}
		Random random = new Random(System.currentTimeMillis());
		int idx = random.nextInt(list.size());
		String trcmdId = (String) list.get(idx);
		TransferCommand trcmd = longrunManager.getTrcmd(trcmdId);
		if (trcmd != null) {
			MyHashtable htCommandInfo = new MyHashtable();
			htCommandInfo.put("TSC", "TOCS411");
			htCommandInfo.put("MicroTrCmdType", "CANCEL");
			htCommandInfo.put("MicroTrCmdID", trcmd.getTrCmdID());
			System.out.println("CANCEL : " + trcmd.getTrCmdID());
			m_ocsMain.semIF.SendMicroTC(htCommandInfo);
		}
		longrunManager.transferCompleted(trcmdId);

	}

	/**
   *
   */
	private void SendTransferCmdProces() {
		// 1. Transfer 명령을 가져와서 전송한다.
		if (m_nIndex >= m_vtTransferCmdList.size()) {
			m_nIndex = 0;
			m_dThreadClock = 0.;
			stageIndex = 0;
		}

		for (; m_nIndex < m_vtTransferCmdList.size();) {
			// 2012.01.25 by LWG [Cancel/Abort 내리기] : trcmd를 빈형태로 변경함
			TransferCommand trCmd = (TransferCommand) m_vtTransferCmdList.get(m_nIndex);

			// 바로 시작을 할 수 있도록 가장 첫번째 반송명령 시간 기준으로 설정
			if (m_nIndex == 0) {
				m_dThreadClock = trCmd.getTrQueuedTime();
			}

			if (m_dThreadClock >= trCmd.getTrQueuedTime()) {
				if (trCmd.getRemoteCmd().equals("TRANSFER") == true) {
					MyHashtable htCommandInfo = new MyHashtable();
					htCommandInfo.put("TSC", "TOCS411");
					htCommandInfo.put("MicroTrCmdID", trCmd.getTrCmdID());
					htCommandInfo.put("MicroTrCmdType", "TRANSFER");
					htCommandInfo.put("CarrierID", trCmd.getCarrierID());
					htCommandInfo.put("CarrierLocID", "");
					htCommandInfo.put("Source", trCmd.getSourceLoc());
					htCommandInfo.put("Dest", trCmd.getDestLoc());
					htCommandInfo.put("Priority", new Integer(trCmd.getPriority()));
					htCommandInfo.put("Replace", new Integer(0));
					htCommandInfo.put("EmptyCarrier", new Integer(0));
					htCommandInfo.put("LotID", "AAA");
					htCommandInfo.put("ErrorID", "");
					htCommandInfo.put("FLOORNUMBER", new Integer(0));
					// 명령 전송
					m_ocsMain.semIF.SendMicroTC(htCommandInfo);

					// 2012.01.25 by LWG [Cancel/Abort 내리기]
					longrunManager.transfer(trCmd);

					// Stage 정보를 추가하는 코드
					if (m_bUseStageCmd == true && m_htStageCmdList.size() > 0) {
						TransferCommand stageCmd = (TransferCommand) m_htStageCmdList.remove(trCmd.getCarrierID());
						if (stageCmd != null && trCmd.getCarrierID().equals(stageCmd.getCarrierID()) == true) {
							StringBuffer log = new StringBuffer("TRANSFER - ");
							log.append("TrCmdId:").append(trCmd.getTrCmdID());
							log.append(" CarrierID:").append(trCmd.getCarrierID());
							log.append(" SourceLoc:").append(trCmd.getSourceLoc());
							log.append(" ClockTime:").append(m_dThreadClock);

							Integer count = (Integer) sendStageCmdList.get(stageCmd.getSourceLoc());
							if (count != null && count.intValue() > 0) {
								if (count.intValue() == 1) {
									sendStageCmdList.remove(stageCmd.getSourceLoc());
									log.append(" Count:0");
								} else {
									sendStageCmdList.put(stageCmd.getSourceLoc(), new Integer(count.intValue() - 1));
									log.append(" Count:").append(count.intValue() - 1);
								}
							}

							UtilLog.WriteReturnLog("StageDebug", log.toString(), "", false);
						}
					}
				} else if (trCmd.getRemoteCmd().equals("STAGE") == true) {
					MyHashtable htCommandInfo = new MyHashtable();
					htCommandInfo.put("TSC", "TOCS411");
					htCommandInfo.put("MicroTrCmdID", trCmd.getCarrierID());
					htCommandInfo.put("MicroTrCmdType", "STAGE");
					htCommandInfo.put("CarrierID", trCmd.getCarrierID());
					htCommandInfo.put("CarrierLocID", "");
					htCommandInfo.put("Source", trCmd.getSourceLoc());
					htCommandInfo.put("Dest", "");
					htCommandInfo.put("Priority", new Integer(trCmd.getPriority()));
					htCommandInfo.put("Replace", new Integer(0));
					htCommandInfo.put("EmptyCarrier", new Integer(0));
					htCommandInfo.put("LotID", "AAA");
					htCommandInfo.put("ErrorID", "");
					htCommandInfo.put("FLOORNUMBER", new Integer(0));
					htCommandInfo.put("EXPECTEDDURATION", new Integer(trCmd.getExpectedDuration()));
					htCommandInfo.put("NOBLOCKINGTIME", new Integer(trCmd.getNoBlockingTime()));
					htCommandInfo.put("WAITTIMEOUT", new Integer(trCmd.getWaitTimeout()));
					m_ocsMain.semIF.SendMicroTC(htCommandInfo);
				}
			} else {
				break;
			}

			m_nIndex++;
		}
	}

	/**
   *
   */
	private void SendStageCmdProces() {
		if (stageCmdList.size() == 0) {
			return;
		}

		int EDT = expectedDuration;
		int NBT = 0;
		int WTO = 0;
		int limitCount = 3;
		while (stageIndex < stageCmdList.size()) {
			TransferCommand stageCmd = (TransferCommand) stageCmdList.get(stageIndex);
			StageParam stageParam = (StageParam) stagePortList.get(stageCmd.getSourceLoc());
			if (stageParam != null) {
				if (stageParam.isEnabled() == false) {
					StringBuffer log = new StringBuffer("STAGE(X) - ");
					log.append("TrCmdId:").append(stageCmd.getTrCmdID());
					log.append(" CarrierID:").append(stageCmd.getCarrierID());
					log.append(" SourceLoc:").append(stageCmd.getSourceLoc());
					log.append(" Enabled:FALSE");
					UtilLog.WriteReturnLog("StageDebug", "" + log.toString(), "", false);
					stageIndex++;
					break;
				}
				EDT = stageParam.getExpectedDuration();
				NBT = stageParam.getNoBlockingTime();
				if (NBT > EDT) {
					NBT = EDT;
				}
				if (NBT < 0) {
					NBT = 0;
				}
				WTO = stageParam.getWaitTimeOut();
				if (WTO < 0) {
					WTO = 0;
				}
				limitCount = stageParam.getLimitCount();
			}
			double gap = stageCmd.getTrQueuedTime() - (m_dThreadClock + EDT - 5);
			if (gap >= 0 && gap <= 5) {
				Integer count = (Integer) sendStageCmdList.get(stageCmd.getSourceLoc());
				if (count == null || (count != null && count.intValue() < limitCount)) {
					stageCmd.setExpectedDuration(EDT + (int) gap);
					stageCmd.setNoBlockingTime(NBT);
					stageCmd.setWaitTimeout(WTO);

					// 명령 전송
					MyHashtable htCommandInfo = new MyHashtable();
					htCommandInfo.put("TSC", "TOCS411");
					htCommandInfo.put("MicroTrCmdID", stageCmd.getCarrierID());
					htCommandInfo.put("MicroTrCmdType", "STAGE");
					htCommandInfo.put("CarrierID", stageCmd.getCarrierID());
					htCommandInfo.put("CarrierLocID", "");
					htCommandInfo.put("Source", stageCmd.getSourceLoc());
					htCommandInfo.put("Dest", "");
					htCommandInfo.put("Priority", new Integer(stageCmd.getPriority()));
					htCommandInfo.put("Replace", new Integer(0));
					htCommandInfo.put("EmptyCarrier", new Integer(0));
					htCommandInfo.put("LotID", "AAA");
					htCommandInfo.put("ErrorID", "");
					htCommandInfo.put("FLOORNUMBER", new Integer(0));
					htCommandInfo.put("EXPECTEDDURATION", new Integer(stageCmd.getExpectedDuration()));
					htCommandInfo.put("NOBLOCKINGTIME", new Integer(stageCmd.getNoBlockingTime()));
					htCommandInfo.put("WAITTIMEOUT", new Integer(stageCmd.getWaitTimeout()));
					m_ocsMain.semIF.SendMicroTC(htCommandInfo);

					m_htStageCmdList.put(stageCmd.getCarrierID(), stageCmd);
					int currCount = 1;
					if (count != null) {
						currCount = count.intValue() + 1;
					}
					sendStageCmdList.put(stageCmd.getSourceLoc(), new Integer(currCount));

					StringBuffer log = new StringBuffer("STAGE    - ");
					log.append("TrCmdId:").append(stageCmd.getTrCmdID());
					log.append(" CarrierID:").append(stageCmd.getCarrierID());
					log.append(" SourceLoc:").append(stageCmd.getSourceLoc());
					log.append(" ClockTime:").append(m_dThreadClock);
					log.append(" EDT:").append(stageCmd.getExpectedDuration());
					log.append(" NBT:").append(stageCmd.getNoBlockingTime());
					log.append(" WTO:").append(stageCmd.getWaitTimeout());
					log.append(" ExpectedTrCreateTime:").append(stageCmd.getTrQueuedTime() - m_dThreadClock);

					UtilLog.WriteReturnLog("StageDebug", log.toString(), "", false);
				} else {
					StringBuffer log = new StringBuffer("STAGE(X) - ");
					log.append("TrCmdId:").append(stageCmd.getTrCmdID());
					log.append(" CarrierID:").append(stageCmd.getCarrierID());
					log.append(" SourceLoc:").append(stageCmd.getSourceLoc());
					log.append(" LimitOver:").append(count.intValue()).append("/").append(limitCount);
					UtilLog.WriteReturnLog("StageDebug", "" + log.toString(), "", false);
				}
				stageIndex++;
				break;
			} else if (gap < 0) {
				stageIndex++;
			} else {
				break;
			}
		}
	}

	private void SendStageCmdProcesOld() {
		// 1. Stage 명령을 만들어 전송한다.
		int nIndex = m_nIndex;
		// 2012.01.25 by LWG [Cancel/Abort 내리기] : 린덤에 seed를 줘서 생성하도록 함.
		Random ran = new Random(System.currentTimeMillis());
		int nEDT = 50;

		Random rad = new Random(System.currentTimeMillis());
		int nCreateCount = rad.nextInt(2) + 1;

		while (nIndex < m_vtTransferCmdList.size() && nCreateCount > 0) {
			// 2012.01.25 by LWG [Cancel/Abort 내리기] : trcmd를 빈형태로 변경함 
			TransferCommand trCmd = (TransferCommand) m_vtTransferCmdList.get(nIndex);
			// 30 ~ 60초 후에 발생될 반송명령을 찾기 위한 조건
			nEDT = 30 + ran.nextInt(30);

			if ((m_dThreadClock + nEDT) <= trCmd.getTrQueuedTime()) {
				// Stage 명령은 ExpectedDuration을 20 ~ 80으로 생성
				// Transfer 명령은 30 ~ 60 사이에 올수 있도록 생성
				trCmd.setExpectedDuration((nEDT - ran.nextInt(10)) + ran.nextInt(50));

				// 명령 전송
				MyHashtable htCommandInfo = new MyHashtable();
				htCommandInfo.put("TSC", "TOCS411");
				htCommandInfo.put("MicroTrCmdID", trCmd.getCarrierID());
				htCommandInfo.put("MicroTrCmdType", "STAGE");
				htCommandInfo.put("CarrierID", trCmd.getCarrierID());
				htCommandInfo.put("CarrierLocID", "");
				htCommandInfo.put("Source", trCmd.getSourceLoc());
				htCommandInfo.put("Dest", "");
				htCommandInfo.put("Priority", new Integer(trCmd.getPriority()));
				htCommandInfo.put("Replace", new Integer(0));
				htCommandInfo.put("EmptyCarrier", new Integer(0));
				htCommandInfo.put("LotID", "AAA");
				htCommandInfo.put("ErrorID", "");
				htCommandInfo.put("FLOORNUMBER", new Integer(0));
				htCommandInfo.put("EXPECTEDDURATION", new Integer(trCmd.getExpectedDuration()));
				htCommandInfo.put("NOBLOCKINGTIME", new Integer(trCmd.getNoBlockingTime()));
				htCommandInfo.put("WAITTIMEOUT", new Integer(trCmd.getWaitTimeout()));
				m_ocsMain.semIF.SendMicroTC(htCommandInfo);

				m_htStageCmdList.put(trCmd.getCarrierID(), trCmd);

				String strLog = "STAGE    - CarrierID: " + trCmd.getCarrierID() + ", ClockTime: " + m_dThreadClock + ", EDT: " + trCmd.getExpectedDuration() + ", ExpectedTrCreateTime: "
						+ (trCmd.getTrQueuedTime() - m_dThreadClock);
				UtilLog.WriteReturnLog("StageDebug", strLog, "", false);
				//        break;

				nCreateCount--;
			}

			nIndex++;
		}
	}

	/**
	 * Transfer.xls 파일을 로드한다.
	 *
	 * @version Created by MYM 2009.08.28
	 */
	public void LoadTransferCmdFromExcel(String strFileName) {
		// 2014.03.17 by MYM : Stage 적용 Port 읽어오기
		loadStageConfig();

		// 1. TrCmd Class 생성
		// 2. 각 TrCmd 객체를 리스트에 저장
		Workbook workbook = null;
		try {
			// Excel파일로부터 workbook 가져오기
			workbook = Workbook.getWorkbook(new File(strFileName));
			if (workbook != null) {
				// Sheet의 개수 얻기
				int nCount = workbook.getNumberOfSheets();
				System.out.println("Sheet 개수 : " + nCount);

				// 첫번째 Sheet 가져오기
				Sheet sheet = workbook.getSheet(0);

				if (sheet != null) {
					int nRowCount = sheet.getRows();
					System.out.println("Record 개수 : " + nRowCount);

					int nTrCmdIDColumn = sheet.findCell("TRCMDID").getColumn();
					int nPriorityColumn = sheet.findCell("PRIORITY").getColumn();
					int nCarrierIDColumn = sheet.findCell("CARRIERID").getColumn();
					int nSourceLocColumn = sheet.findCell("SOURCELOC").getColumn();
					int nDestLocColumn = sheet.findCell("DESTLOC").getColumn();
					int nTrQueuedTimeColumn = sheet.findCell("TRQUEUEDTIME").getColumn();
					int nRemoteCmdColumn = sheet.findCell("REMOTECMD").getColumn();

					String strTrQueuedTime, strHour, strMin, strSec;
					String strDay;

					m_vtTransferCmdList.clear();
					m_htStageCmdList.clear();
					sendStageCmdList.clear();
					stageCmdList.clear();
					m_dThreadClock = 0;
					m_nIndex = 0;
					stageIndex = 0;

					// 전체 Record 출력
					for (int i = 1; i < nRowCount; i++) {
						// 2012.01.25 by LWG [Cancel/Abort 내리기] : trcmd를 빈형태로 변경함
						TransferCommand trcmd = new TransferCommand();
						try {
							trcmd.setRemoteCmd(sheet.getCell(nRemoteCmdColumn, i).getContents());
							if (trcmd.getRemoteCmd().equals("TRANSFER") == false && trcmd.getRemoteCmd().startsWith("STAGE") == false)
								continue;

							if (trcmd.getRemoteCmd().startsWith("STAGE") == true) {
								trcmd.setRemoteCmd("STAGE");
							}

							trcmd.setTrCmdID(sheet.getCell(nTrCmdIDColumn, i).getContents());
							trcmd.setPriority(Integer.parseInt(sheet.getCell(nPriorityColumn, i).getContents()));
							trcmd.setCarrierID(sheet.getCell(nCarrierIDColumn, i).getContents());
							trcmd.setSourceLoc(sheet.getCell(nSourceLocColumn, i).getContents());
							trcmd.setDestLoc(sheet.getCell(nDestLocColumn, i).getContents());
							strTrQueuedTime = sheet.getCell(nTrQueuedTimeColumn, i).getContents();

							// 2010.03.24 by MYM
							//              try
							//              {
							//                int nYear = Integer.parseInt(strTrQueuedTime.substring(0, 4));
							//                int nMonth = Integer.parseInt(strTrQueuedTime.substring(4, 6)) - 1;
							//                int nDay = Integer.parseInt(strTrQueuedTime.substring(6, 8));
							//                int nHour = Integer.parseInt(strTrQueuedTime.substring(8, 10));
							//                int nMin = Integer.parseInt(strTrQueuedTime.substring(10, 12));
							//                int nSec = Integer.parseInt(strTrQueuedTime.substring(12));
							//                GregorianCalendar cal = new GregorianCalendar(nYear, nMonth, nDay, nHour, nMin, nSec);
							//                trcmd.lTrQueuedTime = System.currentTimeMillis()- cal.getTimeInMillis();
							//              }catch(Exception e) {trcmd.lTrQueuedTime = System.currentTimeMillis();}
							strDay = strTrQueuedTime.substring(6, 8);
							strHour = strTrQueuedTime.substring(8, 10);
							strMin = strTrQueuedTime.substring(10, 12);
							strSec = strTrQueuedTime.substring(12, 14);
							double trqueuedTime = Integer.parseInt(strDay) * 86400 + Integer.parseInt(strHour) * 3600 + Integer.parseInt(strMin) * 60 + Integer.parseInt(strSec)
									+ trcmd.getTrQueuedTime();
							trcmd.setTrQueuedTime(trqueuedTime);

							//              UtilLog.WriteReturnLog("Trnasfer", "TrCmdID:"+trcmd.strTrCmdID+",Time:"+trcmd.lTrQueuedTime+"("+strTrQueuedTime+")" ,"", false);

							// 2014.03.18 by MYM
							if (stagePortList.containsKey(trcmd.getSourceLoc())) {
								trcmd.setForStage(true);
								stageCmdList.add(trcmd);
							}

							m_vtTransferCmdList.add(trcmd);
						} catch (Exception e) {
							UtilLog.WriteReturnLog("LongRunException", "LoadTransferCmdFromExcel() - Exception", "", false);
						}
					}
				}
			}
		} catch (Exception e) {
			StringBuffer sb = new StringBuffer();
			StackTraceElement[] trace = e.getStackTrace();

			for (int i = 0; i < trace.length; i++)
				sb.append("\n " + trace[i]);

			System.out.println(sb.toString());
		}
	}

	/**
	 * Timer 주기로 반송명령을 생성한다.
	 *
	 * @version Created by MYM 2009.08.28
	 */
	public boolean LongRunStart() {
		if (m_vtTransferCmdList.size() <= 0)
			return false;

		m_TransferThread.m_bStart = true;
		return true;
	}

	/**
	 * 반송명령 일시 중지
	 *
	 * @version Created by MYM 2009.08.28
	 */
	public void LongRunPause() {
		m_TransferThread.m_bStart = false;
	}

	/**
	 * Stage 명령을 만들어 전송
	 *
	 * @version Created by MYM 2009.08.28
	 */
	public void SetUseStageCmd(boolean bUse) {
		m_bUseStageCmd = bUse;
	}

	/**
	 *
	 * @return boolean
	 */
	public boolean IsRunThread() {
		return m_TransferThread.m_bStart;
	}

	/**
	 *
	 * @param nInterval
	 *            int
	 */
	public void SetStageInterval(int nInterval) {
		m_TransferThread.m_nStageInterval = nInterval;
	}

	public void SetStageEDT(int expectedDuration) {
		this.expectedDuration = expectedDuration;
	}

	//2012.01.25 by LWG [Cancel/Abort 내리기]
	public void setCancelInterval(int interval) {
		m_TransferThread.cancelInterval = interval;
	}

	public void setAbortInterval(int interval) {
		m_TransferThread.abortInterval = interval;
	}

	/**
	 *
	 * @param nInterval
	 *            int
	 */
	public void SetThreadInterval(double dAccel) {
		m_TransferThread.m_dAccel = dAccel;
	}

	//2012.01.25 by LWG [Cancel/Abort 내리기]
	public void setUseAbortCmd(boolean useAbortCmd) {
		this.useAbortCmd = useAbortCmd;
	}

	//2012.01.25 by LWG [Cancel/Abort 내리기]
	public void setUseCancelCmd(boolean useCancelCmd) {
		this.useCancelCmd = useCancelCmd;
	}

	public class StageParam {
		private String port;
		private boolean enabled;
		private int limitCount;
		private int expectedDuration;
		private int noBlockingTime;
		private int waitTimeOut;

		public StageParam(String port, boolean enabled, int limitCount, int expectedDuration, int noBlockingTime, int waitTimeOut) {
			this.port = port;
			this.enabled = enabled;
			this.limitCount = limitCount;
			this.expectedDuration = expectedDuration;
			this.noBlockingTime = noBlockingTime;
			this.waitTimeOut = waitTimeOut;
		}

		public String getPort() {
			return port;
		}

		public void setPort(String port) {
			this.port = port;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getLimitCount() {
			return limitCount;
		}

		public void setLimitCount(int limitCount) {
			this.limitCount = limitCount;
		}

		public int getExpectedDuration() {
			return expectedDuration;
		}

		public void setExpectedDuration(int expectedDuration) {
			this.expectedDuration = expectedDuration;
		}

		public int getNoBlockingTime() {
			return noBlockingTime;
		}

		public void setNoBlockingTime(int noBlockingTime) {
			this.noBlockingTime = noBlockingTime;
		}

		public int getWaitTimeOut() {
			return waitTimeOut;
		}

		public void setWaitTimeOut(int waitTimeOut) {
			this.waitTimeOut = waitTimeOut;
		}
	}

	/**
	 * 2014.03.17 by MYM
	 * 
	 * @return
	 */
	boolean loadStageConfig() {

		StringBuffer FilePathName = new StringBuffer();
		String strFileName = "Stage.ini";

		// get current directory
		String Path = System.getProperty("user.dir");
		String Separator = System.getProperty("file.separator");
		FilePathName.append(Path).append(Separator).append(strFileName);

		File f;
		RandomAccessFile raf = null;
		boolean bReturn = true;

		try {
			f = new File(FilePathName.toString());
			raf = new RandomAccessFile(f, "r");
			int i;
			String line = "";
			int count = 0;
			while ((line = raf.readLine()) != null) {
				if (line.startsWith("#")) {
					continue;
				}
				String[] tmp = line.split(",");
				if (tmp.length == 6) {
					try {
						stagePortList
								.put(tmp[0], new StageParam(tmp[0], "TRUE".equals(tmp[1]), Integer.parseInt(tmp[2]), Integer.parseInt(tmp[3]), Integer.parseInt(tmp[4]), Integer.parseInt(tmp[5])));
					} catch (Exception e) {
					}
				}
			}
			bReturn = true;
		} catch (Exception e) {
			String strLog = "loadStageConfig - Exception: " + e.getMessage();
			bReturn = false;
		} finally {
			try {
				if (raf != null) {
					raf.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return bReturn;
	}

}
