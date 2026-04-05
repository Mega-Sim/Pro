package com.samsung.ocs.operation;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.samsung.ocs.common.thread.AbstractOcsThread;

public class ThreadMonitorManager extends AbstractOcsThread {
	private ConcurrentHashMap<String, ? extends AbstractOcsThread> threadsMap = null;
	private static ThreadMonitorManager manager = null;
	private ThreadMXBean threadMXbean = null;

	/**
	 * @author zzang9un
	 * @date 2014. 11. 13.
	 * @param m 감시 대상이 되는 thread Map
	 * @param checkInterval 감시 주기
	 * @return
	 */
	static public ThreadMonitorManager getInstance(ConcurrentHashMap<String, ? extends AbstractOcsThread> m, long checkInterval) {
		if (manager == null) {
			manager = new ThreadMonitorManager(m, checkInterval);
			manager.start();
		}

		return manager;
	}

	private ThreadMonitorManager(ConcurrentHashMap<String, ? extends AbstractOcsThread> m, long checkInterval) {
		this.threadsMap = m;
		this.interval = checkInterval;
	}

	@Override
	public String getThreadId() {
		return this.getClass().getName();
	}

	@Override
	protected void initialize() {

	}

	@Override
	protected void stopProcessing() {

	}

	@Override
	protected void mainProcessing() {
		if (threadMXbean == null) {
			threadMXbean = ManagementFactory.getThreadMXBean();
		}

		// step 1. Deadlock된 Thread 감지
		checkDeadlockedThreads();
		
		// step 2. 특정 시간 이상 지연된 Thread 감지
		checkDelayedThreads();
	}
	
	/**
	 * Check deadlocked threads.
	 * @author zzang9un
	 * @date 2014.11.13
	 */
	public void checkDeadlockedThreads() {
		long[] ids = threadMXbean.findDeadlockedThreads();
		if (ids != null) {
			ThreadInfo[] tis = threadMXbean.getThreadInfo(ids, true, true);
			StringBuilder log = new StringBuilder();
			log.append("DeadLock Found!!!!!\n");

			for (ThreadInfo ti : tis) {
				ThreadInfo threadInfo = threadMXbean.getThreadInfo(ti.getThreadId(), Integer.MAX_VALUE);
				log.append(threadInfo.toString());
			}
			
			traceThreadDeadlock(log.toString());
		}
	}
	
	/**
	 * Check delayed threads.
	 * @author zzang9un
	 * @date 2014.11.13
	 */
	public void checkDelayedThreads() {
		Enumeration<? extends AbstractOcsThread> elements = this.threadsMap.elements();
		int loopCount = 0;
		int threadsMapSize = this.threadsMap.size();
		
		while (elements.hasMoreElements()) {
			if (loopCount++ >= threadsMapSize) {
				break;
			}
			
			AbstractOcsThread thread = elements.nextElement();
			if (thread == null) {
				continue;
			}
			
			if ((System.currentTimeMillis() - thread.getLastExecutedTime()) >= thread.getElapsedTimeLimit()) {
				ThreadInfo threadInfo = threadMXbean.getThreadInfo(thread.getId(), Integer.MAX_VALUE);
				
				StringBuilder log = new StringBuilder();
				log.append("Thread Hang Detect!!!!\n");
				log.append("Thread Name:" + threadInfo.getThreadName() + ", ElapsedTimeLimit:" + thread.getElapsedTimeLimit() + "ms");
				log.append(", DelayedTime:" + (System.currentTimeMillis() - thread.getLastExecutedTime()) + "ms\n");
				log.append(threadInfo);
				traceThreadHangup(log.toString());
			}
		}
	}

	private static final String THREAD_HANGUP_TRACE = "ThreadHangup";
	private static Logger trehadHangupTraceLog = Logger.getLogger(THREAD_HANGUP_TRACE);

	private void traceThreadHangup(String message) {
		trehadHangupTraceLog.debug(message);
	}

	private static final String THREAD_DEADLOCK_TRACE = "ThreadDeadlock";
	private static Logger threadDeadlockTraceLog = Logger.getLogger(THREAD_DEADLOCK_TRACE);

	private void traceThreadDeadlock(String message) {
		threadDeadlockTraceLog.debug(message);
	}

}
