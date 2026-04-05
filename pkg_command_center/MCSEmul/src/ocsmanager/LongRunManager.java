/**
 * Copyright 2011 by Samsung Electronics, Inc.,
 *
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */
package ocsmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ocsmanager.model.TransferCommand;

/**
 * 설명
 *
 * @author LWG
 * @date 2011. 9. 5.
 * @version 3.0
 */
public class LongRunManager {

	private OCSManagerMain ocsMain;

	private Map transferMap;
	private List bufferList;

	private boolean run = false;
	private Random random = null;

	private List cancelCandidateList = null;
	private List abortCandidateList = null;

	public LongRunManager(OCSManagerMain ocsMain, utilLog utillog) {
		this.ocsMain = ocsMain;
		this.transferMap = new HashMap();
		this.bufferList = new ArrayList();
		this.cancelCandidateList = new ArrayList();
		this.abortCandidateList = new ArrayList();
		this.random = new Random(System.currentTimeMillis());
	}

	public void initLongRunManager(Set bufferSet_) {
		this.transferMap.clear();
		this.bufferList.clear();
		this.bufferList.addAll(bufferSet_);
	}

	public void transferCompleted(String trcmdId) {
		synchronized (transferMap) {
			transferMap.remove(trcmdId);
			cancelCandidateList.remove(trcmdId);
			abortCandidateList.remove(trcmdId);
		}
	}

	public void transfer(TransferCommand trcmd) {
		synchronized (transferMap) {
			trcmd.setEntryPutTime(System.currentTimeMillis());
			transferMap.put(trcmd.getTrCmdID(), trcmd);
			cancelCandidateList.add(trcmd.getTrCmdID());
		}
	}

	public TransferCommand getTrcmd(String trcmdId) {
		return (TransferCommand) transferMap.get(trcmdId);
	}

	public String getRandomBufferPort() {
		int idx = random.nextInt(bufferList.size());
		return (String) bufferList.get(idx);
	}

	public void removeOldTrCmd() {
		synchronized (transferMap) {
			List removeList = new ArrayList();
			long currentTime = System.currentTimeMillis();
			for (Iterator it = transferMap.values().iterator(); it.hasNext();) {
				TransferCommand trcmd = (TransferCommand) it.next();
				long durationTime = currentTime - trcmd.getEntryPutTime();
				//10분이상인건 지우자
				if (durationTime > 600000) {
					removeList.add(trcmd.getTrCmdID());
				}
			}
			for (Iterator it = removeList.iterator(); it.hasNext();) {
				String removeId = (String) it.next();
				transferMap.remove(removeId);
				cancelCandidateList.remove(removeId);
				abortCandidateList.remove(removeId);
			}
		}
	}

	/**
	 * @param trcmdId
	 */
	public void setTrcmdTransferring(String trcmdId) {
		synchronized (transferMap) {
			TransferCommand trcmd = getTrcmd(trcmdId);
			if (trcmd != null) {
				trcmd.setState("Transferring");
				cancelCandidateList.remove(trcmdId);
				abortCandidateList.add(trcmdId);
			}
		}
	}

	public List getCancelCandidateList() {
		return cancelCandidateList;
	}

	public List getAbortCandidateList() {
		return abortCandidateList;
	}

}
