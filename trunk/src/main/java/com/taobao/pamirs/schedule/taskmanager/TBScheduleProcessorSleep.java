package com.taobao.pamirs.schedule.taskmanager;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.pamirs.schedule.IScheduleTaskDeal;
import com.taobao.pamirs.schedule.IScheduleTaskDealMulti;
import com.taobao.pamirs.schedule.IScheduleTaskDealSingle;
import com.taobao.pamirs.schedule.TaskItemDefine;

/**
 * �������������TBScheduleManager�Ĺ�����ʵ�ֶ��߳����ݴ���
 * @author xuannan
 *
 * @param <T>
 */
class TBScheduleProcessorSleep<T> implements IScheduleProcessor,Runnable {
	
	private static transient Logger logger = LoggerFactory.getLogger(TBScheduleProcessorSleep.class);
	final  LockObject   m_lockObject = new LockObject();
	List<Thread> threadList =  new CopyOnWriteArrayList<Thread>();
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
	 * ��ǰ������еİ汾��
	 */
	protected long taskListVersion = 0;
	final Object lockVersionObject = new Object();
	final Object lockRunningList = new Object();

	protected List<T> taskList = new CopyOnWriteArrayList<T>();

	/**
	 * �Ƿ����������
	 */
	boolean isMutilTask = false;
	
	/**
	 * �Ƿ��Ѿ������ֹ�����ź�
	 */
	boolean isStopSchedule = false;// �û�ֹͣ���е���
	boolean isSleeping = false;
	
	StatisticsInfo statisticsInfo;
	/**
	 * ����һ�����ȴ����� 
	 * @param aManager
	 * @param aTaskDealBean
	 * @param aStatisticsInfo
	 * @throws Exception
	 */
	public TBScheduleProcessorSleep(TBScheduleManager aManager,
			IScheduleTaskDeal<T> aTaskDealBean,	StatisticsInfo aStatisticsInfo) throws Exception {
		this.scheduleManager = aManager;
		this.statisticsInfo = aStatisticsInfo;
		this.taskTypeInfo = this.scheduleManager.getTaskTypeInfo();
		this.taskDealBean = aTaskDealBean;
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

	   public synchronized Object getScheduleTaskId() {
		     if (this.taskList.size() > 0)
		    	 return this.taskList.remove(0);  // ��������
		     return null;
		   }

		   public synchronized Object[] getScheduleTaskIdMulti() {
		       if (this.taskList.size() == 0){
		         return null;
		       }
		       int size = taskList.size() > taskTypeInfo.getExecuteNumber() ? taskTypeInfo.getExecuteNumber()
						: taskList.size();
		       
		       Object[] result = null;
		       if(size >0){
		    	   result =(Object[])Array.newInstance(this.taskList.get(0).getClass(),size);
		       }
		       for(int i=0;i<size;i++){
		      	 result[i] = this.taskList.remove(0);  // ��������
		       }
		       return result;
		   }

	public void clearAllHasFetchData() {
		this.taskList.clear();
	}
	public boolean isDealFinishAllData() {
		return this.taskList.size() == 0 ;
	}
	
	public boolean isSleeping(){
    	return this.isSleeping;
    }
	protected int loadScheduleData() {
		try {
           //��ÿ�����ݴ�����Ϻ����߹̶���ʱ��
			if (this.taskTypeInfo.getSleepTimeInterval() > 0) {
				if(logger.isTraceEnabled()){
					logger.trace("������һ�����ݺ����ߣ�" + this.taskTypeInfo.getSleepTimeInterval());
				}
				this.isSleeping = true;
			    Thread.sleep(taskTypeInfo.getSleepTimeInterval());
			    this.isSleeping = false;
			    
				if(logger.isTraceEnabled()){
					logger.trace("������һ�����ݺ����ߺ�ָ�");
				}
			}
			
			List<TaskItemDefine> taskItems = this.scheduleManager.getCurrentScheduleTaskItemList();
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
						scheduleManager.getScheduleServer().getOwnSign(),
						this.scheduleManager.getTaskItemCount(), tmpTaskList,
						taskTypeInfo.getFetchDataNumber());
				scheduleManager.getScheduleServer().setLastFetchDataTime(new Timestamp(scheduleManager.scheduleCenter.getSystemTime()));
				if(tmpList != null){
				   this.taskList.addAll(tmpList);
				}
			} else {
				if(logger.isTraceEnabled()){
					   logger.trace("û�л�ȡ����Ҫ��������ݶ���");
				}
			}
			addFetchNum(taskList.size(),"TBScheduleProcessor.loadScheduleData");
			return this.taskList.size();
		} catch (Throwable ex) {
			logger.error("Get tasks error.", ex);
		}
		return 0;
	}

	@SuppressWarnings({ "rawtypes", "unchecked", "static-access" })
	public void run(){
	      try {
	        long startTime =0;
	        while(true){
	          this.m_lockObject.addThread();
	          Object executeTask;
	          while (true) {
	            if(this.isStopSchedule == true){//ֹͣ���е���
	              this.m_lockObject.realseThread();
	              this.m_lockObject.notifyOtherThread();//֪ͨ���е������߳�
				  synchronized (this.threadList) {			
					  this.threadList.remove(Thread.currentThread());
					  if(this.threadList.size()==0){
							this.scheduleManager.unRegisterScheduleServer();
					  }
				  }
				  return;
	            }
	            
	            //���ص�������
	            if(this.isMutilTask == false){
	              executeTask = this.getScheduleTaskId();
	            }else{
	              executeTask = this.getScheduleTaskIdMulti();
	            }
	            
	            if(executeTask == null){
	              break;
	            }
	            
	            try {//������صĳ���
	              startTime =scheduleManager.scheduleCenter.getSystemTime();
	              if (this.isMutilTask == false) {
						if (((IScheduleTaskDealSingle) this.taskDealBean).execute(executeTask,scheduleManager.getScheduleServer().getOwnSign()) == true) {
							addSuccessNum(1, scheduleManager.scheduleCenter.getSystemTime()
									- startTime,
									"com.taobao.pamirs.schedule.TBScheduleProcessorSleep.run");
						} else {
							addFailNum(1, scheduleManager.scheduleCenter.getSystemTime()
									- startTime,
									"com.taobao.pamirs.schedule.TBScheduleProcessorSleep.run");
						}
					} else {
						if (((IScheduleTaskDealMulti) this.taskDealBean)
								.execute((Object[]) executeTask,scheduleManager.getScheduleServer().getOwnSign()) == true) {
							addSuccessNum(((Object[]) executeTask).length,scheduleManager.scheduleCenter.getSystemTime()
									- startTime,
									"com.taobao.pamirs.schedule.TBScheduleProcessorSleep.run");
						} else {
							addFailNum(((Object[]) executeTask).length,scheduleManager.scheduleCenter.getSystemTime()
									- startTime,
									"com.taobao.pamirs.schedule.TBScheduleProcessorSleep.run");
						}
					} 
	            }catch (Throwable ex) {
					if (this.isMutilTask == false) {
						addFailNum(1,scheduleManager.scheduleCenter.getSystemTime()- startTime,
								"TBScheduleProcessor.run");
					} else {
						addFailNum(((Object[]) executeTask).length, scheduleManager.scheduleCenter.getSystemTime()
								- startTime,
								"TBScheduleProcessor.run");
					}
					logger.warn("Task :" + executeTask + " ����ʧ��", ex);				
	            }
	          }
	          //��ǰ���������е������Ѿ�����ˡ�
	            if(logger.isTraceEnabled()){
				   logger.trace(Thread.currentThread().getName() +"����ǰ�����߳�����:" +this.m_lockObject.count());
			    }
				if (this.m_lockObject.realseThreadButNotLast() == false) {
					int size = 0;
					Thread.currentThread().sleep(100);
					startTime =scheduleManager.scheduleCenter.getSystemTime();
					// װ������
					size = this.loadScheduleData();
					if (size > 0) {
						this.m_lockObject.notifyOtherThread();
					} else {
						//�жϵ�û�����ݵ��Ƿ��Ƿ���Ҫ�˳�����
						if (this.isStopSchedule == false && this.scheduleManager.isContinueWhenData()== true ){						 
							if(logger.isTraceEnabled()){
								   logger.trace("û��װ�ص����ݣ�start sleep");
							}
							this.isSleeping = true;
						    Thread.currentThread().sleep(this.scheduleManager.getTaskTypeInfo().getSleepTimeNoData());
						    this.isSleeping = false;
						    
						    if(logger.isTraceEnabled()){
								   logger.trace("Sleep end");
							}
						}else{
							//û�����ݣ��˳����ȣ��������г�˯�߳�
							this.m_lockObject.notifyOtherThread();
						}
					}
					this.m_lockObject.realseThread();
				} else {// ����ǰ�̷߳��õ��ȴ������С�ֱ�����߳�װ�ص����µ���������
					if(logger.isTraceEnabled()){
						   logger.trace("�������һ���̣߳�sleep");
					}
					this.m_lockObject.waitCurrentThread();
				}
	        }
	      }
	      catch (Throwable e) {
	    	  logger.error(e.getMessage(), e);
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
}
