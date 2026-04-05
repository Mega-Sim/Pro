package com.samsung.ocs.failover.thread.tcp;

public abstract class AbstractWorkThread extends Thread {
	
	public abstract String getThreadId();
	protected abstract void initialize();
	protected abstract void stopProcessing();
	protected abstract void mainProcessing();
	
	protected long interval = 1000;
	protected long sleepCount = 0;
	private boolean stopFlag = false;
	public boolean runFlag = false;
	
	private boolean hasInterval = false;

	/**
	 * Constructor of AbstractWorkThread abstract class.
	 */
	protected AbstractWorkThread() {
		initialize();
	}
	
	protected AbstractWorkThread(long interval) {
		this();
		this.interval = interval;
	}
	
	/**
	 * 
	 */
	public void run() {
		setName(getThreadId());
		System.out.println("Thread[" + getThreadId() + "] is started.");
		try {
			while (!Thread.currentThread().isInterrupted()) {
				hasInterval = false;
				while (runFlag) {
					try {
						mainProcessing();
					} catch (Throwable t) {
						traceException(t);
					}

					try {
						sleep(interval);
						hasInterval = true;
						sleepCount++;
					} catch (Throwable t) {
						traceException(t);
					}
					if (stopFlag) {
						break;
					}
				}
				if (stopFlag) {
					stopProcessing();
					break;
				}
				if (hasInterval == false) {
					try {
						sleep(interval);
						hasInterval = true;
					} catch (Throwable t) {
						traceException(t);
					}
				}
			}
		} catch(Throwable e) {
			traceException(e);
		}
		System.out.println("Thread[" + getThreadId() + "] is stopped.");
	}
	
	/**
	 * 
	 * @param runFlag
	 */
	public void setRunFlag(boolean runFlag) {
		this.runFlag = runFlag;
	}
	
	/**
	 * 
	 */
	public void stopThread() {
		this.stopFlag = true;
	}
	
	private void traceException(Throwable t) {
		t.printStackTrace();
	}
}
