package com.taobao.pamirs.schedule.taskmanager;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.pamirs.schedule.IScheduleTaskDeal;
import com.taobao.pamirs.schedule.IScheduleTaskDealMulti;
import com.taobao.pamirs.schedule.IScheduleTaskDealSingle;
import com.taobao.pamirs.schedule.TaskItemDefine;


/**
 * �������������TBScheduleManager�Ĺ�����ʵ�ֶ��߳����ݴ���
 * @author xuannan
 * @param <T>
 * �޸ļ�¼��
 * 	  Ϊ�˼򻯴����߼���ȥ���汾���ʣ����ӿ����ظ��������б�   by  ���� 20110310
 */
class TBScheduleProcessorNotSleep<T> implements IScheduleProcessor, Runnable {
	
	private static transient Logger logger = LoggerFactory.getLogger(TBScheduleProcessorNotSleep.class);
	
	List<Thread> threadList = new CopyOnWriteArrayList<Thread>();
	/**
	 * ���������
	 */
	protected TBScheduleManager scheduleManager;
	/**
	 * ��������
	 */
	ScheduleTaskType taskTypeInfo;
	
	
	/**
	 * ������Ľӿ���
	 */
	protected IScheduleTaskDeal<T> taskDealBean;
	
	/**
	 * ����Ƚ���
	 */
	Comparator<T> taskComparator;

	StatisticsInfo statisticsInfo;

	protected List<T> taskList =new CopyOnWriteArrayList<T>();
	/**
	 * ���ڴ����е��������
	 */
	protected List<Object> runningTaskList = new CopyOnWriteArrayList<Object>(); 
	/**
	 * ������ȡ���ݣ����ܻ��ظ������ݡ�������ȥ����ǰ����runningTaskList��������
	 */
	protected List<T> maybeRepeatTaskList = new CopyOnWriteArrayList<T>();

	Lock lockFetchID = new ReentrantLock();
	Lock lockFetchMutilID = new ReentrantLock();	
	Lock lockLoadData = new ReentrantLock();
	/**
	 * �Ƿ����������
	 */
	boolean isMutilTask = false;
	
	/**
	 * �Ƿ��Ѿ������ֹ�����ź�
	 */
	boolean isStopSchedule = false;// �û�ֹͣ���е���
	boolean isSleeping = false;
	
	/**
	 * ����һ�����ȴ�����
	 * @param aManager
	 * @param aTaskDealBean
	 * @param aStatisticsInfo
	 * @throws Exception
	 */
	public TBScheduleProcessorNotSleep(TBScheduleManager aManager,
			IScheduleTaskDeal<T> aTaskDealBean,StatisticsInfo aStatisticsInfo) throws Exception {
		this.scheduleManager = aManager;
		this.statisticsInfo = aStatisticsInfo;
		this.taskTypeInfo = this.scheduleManager.getTaskTypeInfo();
		this.taskDealBean = aTaskDealBean;
		this.taskComparator = new MYComparator(this.taskDealBean.getComparator());
		if (this.taskDealBean instanceof IScheduleTaskDealSingle<?>) {
			if (taskTypeInfo.getExecuteNumber() > 1) {
				taskTypeInfo.setExecuteNumber(1);
			}
			isMutilTask = false;
		} else {
			isMutilTask = true;
		}
		if (taskTypeInfo.getFetchDataNumber() < taskTypeInfo.getThreadNumber() * 10) {
			logger.warn("�������ò�����ϵͳ���ܲ��ѡ���ÿ�δ����ݿ��ȡ������fetchnum�� >= ���߳�����threadnum�� *������ѭ������10�� ");
		}
		for (int i = 0; i < taskTypeInfo.getThreadNumber(); i++) {
			this.startThread(i);
		}
	}

	/**
	 * ��Ҫע����ǣ����ȷ���������������ע���Ĺ����������������߳��˳�������²�����
	 * @throws Exception
	 */
	public void stopSchedule() throws Exception {
		// ����ֹͣ���ȵı�־,�����̷߳��������־��ִ���굱ǰ����󣬾��˳�����
		this.isStopSchedule = true;
		//�������δ��������,���Ѿ����봦����еģ���Ҫ�������
		this.taskList.clear();
	}

	private void startThread(int index) {
		Thread thread = new Thread(this);
		threadList.add(thread);
		String threadName = this.scheduleManager.getScheduleServer().getTaskType()+"-"
				+ this.scheduleManager.getCurrentSerialNumber() + "-exe"
				+ index;
		thread.setName(threadName);
		thread.start();
	}
	
	@SuppressWarnings("unchecked")
	protected boolean isDealing(T aTask) {
		if (this.maybeRepeatTaskList.size() == 0) {
			return false;
		}
		T[] tmpList = (T[]) this.maybeRepeatTaskList.toArray();
		for (int i = 0; i < tmpList.length; i++) {
			if(this.taskComparator.compare(aTask, tmpList[i]) == 0){
				this.maybeRepeatTaskList.remove(tmpList[i]);
				return true;
			}
		}
		return false;
	}

	/**
	 * ��ȡ��������ע��lock�Ǳ��룬
	 * ������maybeRepeatTaskList�����ݴ����ϻ���ֳ�ͻ
	 * @return
	 */
	public T getScheduleTaskId() {
		lockFetchID.lock();
		try {
			T result = null;
			while (true) {
				if (this.taskList.size() > 0) {
					result = this.taskList.remove(0); // ��������
				} else {
					return null;
				}
				if (this.isDealing(result) == false) {
					return result;
				}
			}
		} finally {
			lockFetchID.unlock();
		}
	}
	/**
	 * ��ȡ��������ע��lock�Ǳ��룬
	 * ������maybeRepeatTaskList�����ݴ����ϻ���ֳ�ͻ
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public T[] getScheduleTaskIdMulti() {
		lockFetchMutilID.lock();
		try {
			if (this.taskList.size() == 0) {
				return null;
			}
			int size = taskList.size() > taskTypeInfo.getExecuteNumber() ? taskTypeInfo
					.getExecuteNumber() : taskList.size();

			List<T> result = new ArrayList<T>();
			int point = 0;
			T tmpObject = null;
			while (point < size
					&& ((tmpObject = this.getScheduleTaskId()) != null)) {
				result.add(tmpObject);
				point = point + 1;
			}
			if (result.size() == 0) {
				return null;
			} else {
				return (T[]) result.toArray((T[]) Array.newInstance(result.get(0).getClass(),0));
			}
		} finally {
			lockFetchMutilID.unlock();
		}
	}
	
	public void clearAllHasFetchData(){
		this.taskList.clear();
	}
    public boolean isDealFinishAllData(){
    	return this.taskList.size() == 0 && this.runningTaskList.size() ==0;  
    }
    
    public boolean isSleeping(){
    	return this.isSleeping;
    }
    /**
     * װ������
     * @return
     */
	protected int loadScheduleData() {
		lockLoadData.lock();
		try {
			if (this.taskList.size() > 0 || this.isStopSchedule == true) { // �ж��Ƿ��б���߳��Ѿ�װ�ع��ˡ�
				return this.taskList.size();
			}
			// ��ÿ�����ݴ�����Ϻ����߹̶���ʱ��
			try {
				if (this.taskTypeInfo.getSleepTimeInterval() > 0) {
					if (logger.isTraceEnabled()) {
						logger.trace("������һ�����ݺ����ߣ�"
								+ this.taskTypeInfo.getSleepTimeInterval());
					}
					this.isSleeping = true;
					Thread.sleep(taskTypeInfo.getSleepTimeInterval());
					this.isSleeping = false;
					
					if (logger.isTraceEnabled()) {
						logger.trace("������һ�����ݺ����ߺ�ָ�");
					}
				}
			} catch (Throwable ex) {
				logger.error("����ʱ����", ex);
			}

			putLastRunningTaskList();// ��running���е����ݿ����������ظ��Ķ�����

			try {
				List<TaskItemDefine> taskItems = this.scheduleManager
						.getCurrentScheduleTaskItemList();
				// ���ݶ�����Ϣ��ѯ��Ҫ���ȵ����ݣ�Ȼ�����ӵ������б���
				if (taskItems.size() > 0) {
					List<TaskItemDefine> tmpTaskList= new ArrayList<TaskItemDefine>();
					synchronized(taskItems){
						for (TaskItemDefine taskItemDefine : taskItems) {
							tmpTaskList.add(taskItemDefine);
						}
					}
					List<T> tmpList = this.taskDealBean.selectTasks(
							taskTypeInfo.getTaskParameter(),
							scheduleManager.getScheduleServer()
									.getOwnSign(), this.scheduleManager.getTaskItemCount(), tmpTaskList,
							taskTypeInfo.getFetchDataNumber());
					scheduleManager.getScheduleServer().setLastFetchDataTime(new Timestamp(scheduleManager.scheduleCenter.getSystemTime()));
					if (tmpList != null) {
						this.taskList.addAll(tmpList);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("û���������");
					}
				}
				addFetchNum(taskList.size(),
						"TBScheduleProcessor.loadScheduleData");
				if (taskList.size() <= 0) {
					// �жϵ�û�����ݵ��Ƿ��Ƿ���Ҫ�˳�����
					if (this.scheduleManager.isContinueWhenData() == true) {
						if (taskTypeInfo.getSleepTimeNoData() > 0) {
							if (logger.isDebugEnabled()) {
								logger.debug("û�ж�ȡ����Ҫ���������,sleep "
										+ taskTypeInfo.getSleepTimeNoData());
							}
							this.isSleeping = true;
							Thread.sleep(taskTypeInfo.getSleepTimeNoData());
							this.isSleeping = false;							
						}
					}
				}
				return this.taskList.size();
			} catch (Throwable ex) {
				logger.error("��ȡ�������ݴ���", ex);
			}
			return 0;
		} finally {
			lockLoadData.unlock();
		}
	}
	/**
	 * ��running���е����ݿ����������ظ��Ķ�����
	 */
	@SuppressWarnings("unchecked")
	public void putLastRunningTaskList() {
		lockFetchID.lock();
		try {
			this.maybeRepeatTaskList.clear();
			if (this.runningTaskList.size() == 0) {
				return;
			}
			Object[] tmpList = this.runningTaskList.toArray();
			for (int i = 0; i < tmpList.length; i++) {
				if (this.isMutilTask == false) {
					this.maybeRepeatTaskList.add((T) tmpList[i]);
				} else {
					T[] aTasks = (T[]) tmpList[i];
					for (int j = 0; j < aTasks.length; j++) {
						this.maybeRepeatTaskList.add(aTasks[j]);
					}
				}
			}
		} finally {
			lockFetchID.unlock();
		}
	}
	
	/**
	 * ���к���
	 */
	@SuppressWarnings("unchecked")
	public void run() {
		long startTime = 0;
		long sequence = 0;
		Object executeTask = null;	
		while (true) {
			try {
				if (this.isStopSchedule == true) { // ֹͣ���е���
					synchronized (this.threadList) {
						this.threadList.remove(Thread.currentThread());
						if(this.threadList.size()==0){
							this.scheduleManager.unRegisterScheduleServer();
						}
					}
					return;
				}
				// ���ص�������
				if (this.isMutilTask == false) {
					executeTask = this.getScheduleTaskId();
				} else {
					executeTask = this.getScheduleTaskIdMulti();
				}
				if (executeTask == null ) {
					this.loadScheduleData();
					continue;
				}
				
				try { // ������صĳ���
					this.runningTaskList.add(executeTask);
					startTime = scheduleManager.scheduleCenter.getSystemTime();
					sequence = sequence + 1;
					if (this.isMutilTask == false) {
						if (((IScheduleTaskDealSingle<Object>) this.taskDealBean).execute(executeTask,scheduleManager.getScheduleServer().getOwnSign()) == true) {
							addSuccessNum(1, scheduleManager.scheduleCenter.getSystemTime()
									- startTime,
									"com.taobao.pamirs.schedule.TBScheduleProcessorNotSleep.run");
						} else {
							addFailNum(1,scheduleManager.scheduleCenter.getSystemTime()
									- startTime,
									"com.taobao.pamirs.schedule.TBScheduleProcessorNotSleep.run");
						}
					} else {
						if (((IScheduleTaskDealMulti<Object>) this.taskDealBean)
								.execute((Object[]) executeTask,scheduleManager.getScheduleServer().getOwnSign()) == true) {
							addSuccessNum(((Object[]) executeTask).length, scheduleManager.scheduleCenter.getSystemTime()
									- startTime,
									"com.taobao.pamirs.schedule.TBScheduleProcessorNotSleep.run");
						} else {
							addFailNum(((Object[]) executeTask).length, scheduleManager.scheduleCenter.getSystemTime()
									- startTime,
									"com.taobao.pamirs.schedule.TBScheduleProcessorNotSleep.run");
						}
					}
				} catch (Throwable ex) {
					if (this.isMutilTask == false) {
						addFailNum(1, scheduleManager.scheduleCenter.getSystemTime() - startTime,
								"TBScheduleProcessor.run");
					} else {
						addFailNum(((Object[]) executeTask).length, scheduleManager.scheduleCenter.getSystemTime()
								- startTime,
								"TBScheduleProcessor.run");
					}
					logger.error("Task :" + executeTask + " ����ʧ��", ex);
				} finally {
					this.runningTaskList.remove(executeTask);
				}
			} catch (Throwable e) {
				throw new RuntimeException(e);
				//log.error(e.getMessage(), e);
			}
		}
	}

	public void addFetchNum(long num, String addr) {
			this.statisticsInfo.addFetchDataCount(1);
			this.statisticsInfo.addFetchDataNum(num);
	}

	public void addSuccessNum(long num, long spendTime, String addr) {
			this.statisticsInfo.addDealDataSucess(num);
			this.statisticsInfo.addDealSpendTime(spendTime);
	}

	public void addFailNum(long num, long spendTime, String addr) {
			this.statisticsInfo.addDealDataFail(num);
			this.statisticsInfo.addDealSpendTime(spendTime);
	}
	
    class MYComparator implements Comparator<T>{
    	Comparator<T> comparator;
    	public MYComparator(Comparator<T> aComparator){
    		this.comparator = aComparator;
    	}

		public int compare(T o1, T o2) {
			statisticsInfo.addOtherCompareCount(1);
			return this.comparator.compare(o1, o2);
		}
    	public  boolean equals(Object obj){
    	 return this.comparator.equals(obj);
    	}
    }
    
}
