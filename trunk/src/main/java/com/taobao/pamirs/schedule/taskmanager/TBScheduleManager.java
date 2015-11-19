package com.taobao.pamirs.schedule.taskmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.pamirs.schedule.CronExpression;
import com.taobao.pamirs.schedule.IScheduleTaskDeal;
import com.taobao.pamirs.schedule.ScheduleUtil;
import com.taobao.pamirs.schedule.TaskItemDefine;
import com.taobao.pamirs.schedule.strategy.IStrategyTask;
import com.taobao.pamirs.schedule.strategy.TBScheduleManagerFactory;




/**
 * 1��������ȷ�������Ŀ�꣺	�����е������ظ�������©�ı����ٴ���
 * 2��һ��Managerֻ����һ���������͵�һ�鹤���̡߳�
 * 3����һ��JVM������ܴ��ڶ��������ͬ�������͵�Manager��Ҳ���ܴ��ڴ���ͬ�������͵�Manager��
 * 4���ڲ�ͬ��JVM������Դ��ڴ�����ͬ�����Manager 
 * 5�����ȵ�Manager���Զ�̬���������Ӻ�ֹͣ
 * 
 * ��Ҫ��ְ��
 * 1����ʱ���е������������ĸ��µ�ǰ���ȷ�����������״̬
 * 2���������������Ļ�ȡ���з�������״̬�����¼�������ķ��䡣��ô����Ŀ���Ǳ��⼯������������ĵĵ������⡣
 * 3����ÿ���������ݴ�����Ϻ󣬼���Ƿ���������������������Լ��ѳֵ�������У�����У����ͷŸ���ش����������
 *  
 * ������
 * 	 �����ǰ�������ڴ���ǰ�����ʱ��ʱ����Ҫ�����ǰ���У����ͷ��Ѿ��ѳֵ����񡣲�������������ı�����
 * 
 * @author xuannan
 *
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
abstract class TBScheduleManager implements IStrategyTask {
	private static transient Logger log = LoggerFactory.getLogger(TBScheduleManager.class);
	/**
	 * �û���ʶ��ͬ�̵߳����
	 */
	private static int nextSerialNumber = 0;
 
	/**
	 * ��ǰ�߳�����
	 */
	protected int currentSerialNumber=0;
	/**
	 * ��������������Ϣ
	 */
	protected ScheduleTaskType taskTypeInfo;
	/**
	 * ��ǰ���ȷ������Ϣ
	 */
	protected ScheduleServer currenScheduleServer;
	/**
	 * ���д�����
	 */
	IScheduleTaskDeal  taskDealBean;
	
    /**
     * ���߳���������
     */
	IScheduleProcessor processor;
    StatisticsInfo statisticsInfo = new StatisticsInfo();
    
    boolean isPauseSchedule = true;
    String pauseMessage="";
    /**
     *  ��ǰ������������嵥
     *  ArrayListʵ�ֲ���ͬ���ġ�����̲߳����޸ĸ��б������ConcurrentModificationException
     */
    protected List<TaskItemDefine> currentTaskItemList = new CopyOnWriteArrayList<TaskItemDefine>();
    /**
     * ���һ������װ�ص��������ʱ�䡣
     * ��ǰʵ��  - �ϴ�װ��ʱ��  > intervalReloadTaskItemList���������������������µ�����������
     */
    protected long lastReloadTaskItemListTime=0;    
    protected boolean isNeedReloadTaskItem = true;

    
    private String mBeanName;
    /**
     * ���������ĸ�����Ϣ�Ķ�ʱ��
     */
    private Timer heartBeatTimer;

    protected IScheduleDataManager scheduleCenter;
    
    protected String startErrorInfo = null;
    
    protected boolean isStopSchedule = false;
    protected Lock registerLock = new ReentrantLock();
    
    /**
     * ��������Ϣ�Ƿ��ʼ���ɹ�
     */
    protected boolean isRuntimeInfoInitial = false;
    
    TBScheduleManagerFactory factory;
	TBScheduleManager(TBScheduleManagerFactory aFactory,String baseTaskType,String ownSign ,IScheduleDataManager aScheduleCenter) throws Exception{
		this.factory = aFactory;
		this.currentSerialNumber = serialNumber();
		this.scheduleCenter = aScheduleCenter;
		this.taskTypeInfo = this.scheduleCenter.loadTaskTypeBaseInfo(baseTaskType);
    	log.info("create TBScheduleManager for taskType:"+baseTaskType);
		//����Ѿ�����1���TASK,OWN_SIGN����ϡ�����һ��û�лserver����Ϊ����
		this.scheduleCenter.clearExpireTaskTypeRunningInfo(baseTaskType,ScheduleUtil.getLocalIP() + "�������OWN_SIGN��Ϣ",this.taskTypeInfo.getExpireOwnSignInterval());
		
		Object dealBean = aFactory.getBean(this.taskTypeInfo.getDealBeanName());
		if (dealBean == null) {
			throw new Exception( "SpringBean " + this.taskTypeInfo.getDealBeanName() + " ������");
		}
		if (dealBean instanceof IScheduleTaskDeal == false) {
			throw new Exception( "SpringBean " + this.taskTypeInfo.getDealBeanName() + " û��ʵ�� IScheduleTaskDeal�ӿ�");
		}
    	this.taskDealBean = (IScheduleTaskDeal)dealBean;

    	if(this.taskTypeInfo.getJudgeDeadInterval() < this.taskTypeInfo.getHeartBeatRate() * 5){
    		throw new Exception("�������ô������⣬������ʱ����������Ҫ���������̵߳�5������ǰ�������ݣ�JudgeDeadInterval = "
    				+ this.taskTypeInfo.getJudgeDeadInterval() 
    				+ ",HeartBeatRate = " + this.taskTypeInfo.getHeartBeatRate());
    	}
    	this.currenScheduleServer = ScheduleServer.createScheduleServer(this.scheduleCenter,baseTaskType,ownSign,this.taskTypeInfo.getThreadNumber());
    	this.currenScheduleServer.setManagerFactoryUUID(this.factory.getUuid());
    	scheduleCenter.registerScheduleServer(this.currenScheduleServer);
    	this.mBeanName = "pamirs:name=" + "schedule.ServerMananger." +this.currenScheduleServer.getUuid();
    	this.heartBeatTimer = new Timer(this.currenScheduleServer.getTaskType() +"-" + this.currentSerialNumber +"-HeartBeat");
    	this.heartBeatTimer.schedule(new HeartBeatTimerTask(this),
                new java.util.Date(System.currentTimeMillis() + 500),
                this.taskTypeInfo.getHeartBeatRate());
    	initial();
	}  
	/**
	 * ���󴴽�ʱ��Ҫ���ĳ�ʼ������
	 * 
	 * @throws Exception
	 */
	public abstract void initial() throws Exception;
	public abstract void refreshScheduleServerInfo() throws Exception;
	public abstract void assignScheduleTask() throws Exception;
	public abstract List<TaskItemDefine> getCurrentScheduleTaskItemList();
	public abstract int getTaskItemCount();
	public String getTaskType(){
		return this.currenScheduleServer.getTaskType();
	}
	
	public void initialTaskParameter(String strategyName,String taskParameter){
	   //û��ʵ�ֵķ�������Ҫ�Ĳ���ֱ�Ӵ����������ж�ȡ	
	}
	private static synchronized int serialNumber() {
	        return nextSerialNumber++;
	}	

	public int getCurrentSerialNumber(){
		return this.currentSerialNumber;
	}
	/**
	 * ����ڴ������е��Ѿ�ȡ�õ����ݺ��������,����̬����ʧ�ܣ����߷���ע�����ĵĵ�����Ϣ��ɾ��
	 */
	public void clearMemoInfo(){
		try {
			// ����ڴ������е��Ѿ�ȡ�õ����ݺ��������,����̬����ʧ�ܣ����߷���ע�����ĵĵ�����Ϣ��ɾ��
			this.currentTaskItemList.clear();
			if (this.processor != null) {
				this.processor.clearAllHasFetchData();
			}
		} finally {
			//�����ڴ����������������Ҫ����װ��
			this.isNeedReloadTaskItem = true;
		}

	}
	
	public void rewriteScheduleInfo() throws Exception{
		registerLock.lock();
		try{
			if (this.isStopSchedule == true) {
				if(log.isDebugEnabled()){
					log.debug("�ⲿ������ֹ����,����ע����ȷ��񣬱��������������ݣ�" + currenScheduleServer.getUuid());
				}
				return;
			}
		//�ȷ���������Ϣ
		if(startErrorInfo == null){
			this.currenScheduleServer.setDealInfoDesc(this.pauseMessage + ":" + this.statisticsInfo.getDealDescription());
		}else{
		    this.currenScheduleServer.setDealInfoDesc(startErrorInfo);
		}
		if(	this.scheduleCenter.refreshScheduleServer(this.currenScheduleServer) == false){
			//������Ϣʧ�ܣ�����ڴ����ݺ�����ע��
			this.clearMemoInfo();
			this.scheduleCenter.registerScheduleServer(this.currenScheduleServer);
		}
		}finally{
			registerLock.unlock();
		}
	}

	


	/**
	 * ��ʼ��ʱ�򣬼����һ��ִ��ʱ��
	 * @throws Exception
	 */
    public void computerStart() throws Exception{
    	//ֻ�е����ڿ�ִ�ж��к��ٿ�ʼ��������
   	
    	boolean isRunNow = false;
    	if(this.taskTypeInfo.getPermitRunStartTime() == null){
    		isRunNow = true;
    	}else{
    		String tmpStr = this.taskTypeInfo.getPermitRunStartTime();
			if(tmpStr.toLowerCase().startsWith("startrun:")){
				isRunNow = true;
				tmpStr = tmpStr.substring("startrun:".length());
	    	}
			CronExpression cexpStart = new CronExpression(tmpStr);
    		Date current = new Date( this.scheduleCenter.getSystemTime());
    		Date firstStartTime = cexpStart.getNextValidTimeAfter(current);
    		this.heartBeatTimer.schedule(
    				new PauseOrResumeScheduleTask(this,this.heartBeatTimer,
    						PauseOrResumeScheduleTask.TYPE_RESUME,tmpStr), 
    						firstStartTime);
			this.currenScheduleServer.setNextRunStartTime(ScheduleUtil.transferDataToString(firstStartTime));	
			if( this.taskTypeInfo.getPermitRunEndTime() == null
    		   || this.taskTypeInfo.getPermitRunEndTime().equals("-1")){
				this.currenScheduleServer.setNextRunEndTime("�����ܻ�ȡ�����ݵ�ʱ��pause");				
			}else{
				try {
					String tmpEndStr = this.taskTypeInfo.getPermitRunEndTime();
					CronExpression cexpEnd = new CronExpression(tmpEndStr);
					Date firstEndTime = cexpEnd.getNextValidTimeAfter(firstStartTime);
					Date nowEndTime = cexpEnd.getNextValidTimeAfter(current);
					if(!nowEndTime.equals(firstEndTime) && current.before(nowEndTime)){
						isRunNow = true;
						firstEndTime = nowEndTime;
					}
					this.heartBeatTimer.schedule(
		    				new PauseOrResumeScheduleTask(this,this.heartBeatTimer,
		    						PauseOrResumeScheduleTask.TYPE_PAUSE,tmpEndStr), 
		    						firstEndTime);
					this.currenScheduleServer.setNextRunEndTime(ScheduleUtil.transferDataToString(firstEndTime));
				} catch (Exception e) {
					log.error("�����һ��ִ��ʱ������쳣:" + currenScheduleServer.getUuid(), e);
					throw new Exception("�����һ��ִ��ʱ������쳣:" + currenScheduleServer.getUuid(), e);
				}
			}
    	}
    	if(isRunNow == true){
    		this.resume("����������������");
    	}
    	this.rewriteScheduleInfo();
    	
    }
	/**
	 * ��Processû�л�ȡ�����ݵ�ʱ����ã������Ƿ���ʱֹͣ������
	 * @throws Exception
	 */
	public boolean isContinueWhenData() throws Exception{
		if(isPauseWhenNoData() == true){
			this.pause("û������,��ͣ����");
			return false;
		}else{
			return true;
		}
	}
	public boolean isPauseWhenNoData(){
		//�����û�з��䵽������������˳�
		if(this.currentTaskItemList.size() >0 && this.taskTypeInfo.getPermitRunStartTime() != null){
			if(this.taskTypeInfo.getPermitRunEndTime() == null
		       || this.taskTypeInfo.getPermitRunEndTime().equals("-1")){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}	
	/**
	 * �������е�����ʱ�䣬��ʱֹͣ����
	 * @throws Exception 
	 */
	public void pause(String message) throws Exception{
		if (this.isPauseSchedule == false) {
			this.isPauseSchedule = true;
			this.pauseMessage = message;
			if (log.isDebugEnabled()) {
				log.debug("��ͣ���� ��" + this.currenScheduleServer.getUuid()+":" + this.statisticsInfo.getDealDescription());
			}
			if (this.processor != null) {
				this.processor.stopSchedule();
			}
			rewriteScheduleInfo();
		}
	}
	/**
	 * �����˿�ִ�е�ʱ�����䣬�ָ�����
	 * @throws Exception 
	 */
	public void resume(String message) throws Exception{
		if (this.isPauseSchedule == true) {
			if(log.isDebugEnabled()){
				log.debug("�ָ�����:" + this.currenScheduleServer.getUuid());
			}
			this.isPauseSchedule = false;
			this.pauseMessage = message;
			if (this.taskDealBean != null) {
				if (this.taskTypeInfo.getProcessorType() != null &&
					this.taskTypeInfo.getProcessorType().equalsIgnoreCase("NOTSLEEP")==true){
					this.taskTypeInfo.setProcessorType("NOTSLEEP");
					this.processor = new TBScheduleProcessorNotSleep(this,
							taskDealBean,this.statisticsInfo);
				}else{
					this.processor = new TBScheduleProcessorSleep(this,
							taskDealBean,this.statisticsInfo);
					this.taskTypeInfo.setProcessorType("SLEEP");
				}
			}
			rewriteScheduleInfo();
		}
	}	
	/**
	 * ��������ֹͣ��ʱ�򣬵��ô˷����������δ�������������������ע����Ϣ��
	 * Ҳ�����ǿ������ķ������ָֹ�
	 * ��Ҫע����ǣ�������������ڵ�ǰ��������Ϻ����ִ��
	 * @throws Exception 
	 */
	public void stop(String strategyName) throws Exception{
		if(log.isInfoEnabled()){
			log.info("ֹͣ������ ��" + this.currenScheduleServer.getUuid());
		}
		this.isPauseSchedule = false;
		if (this.processor != null) {
			this.processor.stopSchedule();
		} else {
			this.unRegisterScheduleServer();
		}
	}
	
	/**
	 * ֻӦ����Processor�е���
	 * @throws Exception
	 */
	protected void unRegisterScheduleServer() throws Exception{
		registerLock.lock();
		try {
			if (this.processor != null) {
				this.processor = null;
			}
			if (this.isPauseSchedule == true) {
				// ����ͣ���ȣ���ע��Manager�Լ�
				return;
			}
			if (log.isDebugEnabled()) {
				log.debug("ע�������� ��" + this.currenScheduleServer.getUuid());
			}
			this.isStopSchedule = true;
			// ȡ������TIMER
			this.heartBeatTimer.cancel();
			// ����������ע���Լ�
			this.scheduleCenter.unRegisterScheduleServer(
					this.currenScheduleServer.getTaskType(),
					this.currenScheduleServer.getUuid());
		} finally {
			registerLock.unlock();
		}
	}
	public ScheduleTaskType getTaskTypeInfo() {
		return taskTypeInfo;
	}	
	
	
	public StatisticsInfo getStatisticsInfo() {
		return statisticsInfo;
	}
	/**
	 * ��ӡ�����������͵�����������
	 * @param taskType
	 */
	public void printScheduleServerInfo(String taskType){
		
	}
	public ScheduleServer getScheduleServer(){
		return this.currenScheduleServer;
	}
	public String getmBeanName() {
		return mBeanName;
	}
}

class HeartBeatTimerTask extends java.util.TimerTask {
	private static transient Logger log = LoggerFactory
			.getLogger(HeartBeatTimerTask.class);
	TBScheduleManager manager;

	public HeartBeatTimerTask(TBScheduleManager aManager) {
		manager = aManager;
	}

	public void run() {
		try {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			manager.refreshScheduleServerInfo();
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}
}

class PauseOrResumeScheduleTask extends java.util.TimerTask {
	private static transient Logger log = LoggerFactory
			.getLogger(HeartBeatTimerTask.class);
	public static int TYPE_PAUSE  = 1;
	public static int TYPE_RESUME = 2;	
	TBScheduleManager manager;
	Timer timer;
	int type;
	String cronTabExpress;
	public PauseOrResumeScheduleTask(TBScheduleManager aManager,Timer aTimer,int aType,String aCronTabExpress) {
		this.manager = aManager;
		this.timer = aTimer;
		this.type = aType;
		this.cronTabExpress = aCronTabExpress;
	}
	public void run() {
		try {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			this.cancel();//ȡ����������
			Date current = new Date(System.currentTimeMillis());
			CronExpression cexp = new CronExpression(this.cronTabExpress);
			Date nextTime = cexp.getNextValidTimeAfter(current);
			if(this.type == TYPE_PAUSE){
				manager.pause("������ֹʱ��,pause����");
				this.manager.getScheduleServer().setNextRunEndTime(ScheduleUtil.transferDataToString(nextTime));
			}else{
				manager.resume("���￪ʼʱ��,resume����");
				this.manager.getScheduleServer().setNextRunStartTime(ScheduleUtil.transferDataToString(nextTime));
			}
			this.timer.schedule(new PauseOrResumeScheduleTask(this.manager,this.timer,this.type,this.cronTabExpress) , nextTime);
		} catch (Throwable ex) {
			log.error(ex.getMessage(), ex);
		}
	}
}

class StatisticsInfo{
	private AtomicLong fetchDataNum = new AtomicLong(0);//��ȡ����
	private AtomicLong fetchDataCount = new AtomicLong(0);//��ȡ��������
	private AtomicLong dealDataSucess = new AtomicLong(0);//����ɹ���������
	private AtomicLong dealDataFail = new AtomicLong(0);//����ʧ�ܵ�������
	private AtomicLong dealSpendTime = new AtomicLong(0);//�����ܺ�ʱ,û����ͬ�������ܴ���һ�������
	private AtomicLong otherCompareCount = new AtomicLong(0);//����ȽϵĴ���
	
	public void addFetchDataNum(long value){
		this.fetchDataNum.addAndGet(value);
	}
	public void addFetchDataCount(long value){
		this.fetchDataCount.addAndGet(value);
	}
	public void addDealDataSucess(long value){
		this.dealDataSucess.addAndGet(value);
	}
	public void addDealDataFail(long value){
		this.dealDataFail.addAndGet(value);
	}
	public void addDealSpendTime(long value){
		this.dealSpendTime.addAndGet(value);
	}
	public void addOtherCompareCount(long value){
		this.otherCompareCount.addAndGet(value);
	}
    public String getDealDescription(){
    	return "FetchDataCount=" + this.fetchDataCount 
    	  +",FetchDataNum=" + this.fetchDataNum
    	  +",DealDataSucess=" + this.dealDataSucess
    	  +",DealDataFail=" + this.dealDataFail
    	  +",DealSpendTime=" + this.dealSpendTime
    	  +",otherCompareCount=" + this.otherCompareCount;    	  
    }

}