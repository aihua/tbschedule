package com.taobao.pamirs.schedule.taskmanager;

class LockObject {
	private int m_threadCount = 0;
	private Object m_waitOnObject = new Object();

	public LockObject() {
	}

	public void waitCurrentThread() throws Exception {
		synchronized (m_waitOnObject) {
			// System.out.println(Thread.currentThread().getName() + ":" +
			// "���ߵ�ǰ�߳�");
			this.m_waitOnObject.wait();
		}
	}

	public void notifyOtherThread() throws Exception {
		synchronized (m_waitOnObject) {
			// System.out.println(Thread.currentThread().getName() + ":" +
			// "�������еȴ��߳�");
			this.m_waitOnObject.notifyAll();
		}
	}

	public void addThread() {
		synchronized (this) {
			m_threadCount = m_threadCount + 1;
		}
	}

	public void realseThread() {
		synchronized (this) {
			m_threadCount = m_threadCount - 1;
		}
	}

	/**
	 * �����߳���������������һ���̣߳���������
	 * 
	 * @return boolean
	 */
	public boolean realseThreadButNotLast() {
		synchronized (this) {
			if (this.m_threadCount == 1) {
				return false;
			} else {
				m_threadCount = m_threadCount - 1;
				return true;
			}
		}
	}

	public int count() {
		synchronized (this) {
			return m_threadCount;
		}
	}
}
