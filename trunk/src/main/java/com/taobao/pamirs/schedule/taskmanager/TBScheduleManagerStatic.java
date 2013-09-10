package com.taobao.pamirs.schedule.taskmanager;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.pamirs.schedule.TaskItemDefine;
import com.taobao.pamirs.schedule.strategy.TBScheduleManagerFactory;

public class TBScheduleManagerStatic extends TBScheduleManager {
	private static transient Log log = LogFactory.getLog(TBScheduleManagerStatic.class);
    /**
	 * �ܵ���������
	 */
    protected int taskItemCount =0;

    protected long lastFetchVersion = -1;
    
    private Object NeedReloadTaskItemLock;

	public TBScheduleManagerStatic(TBScheduleManagerFactory aFactory,
			String baseTaskType, String ownSign,IScheduleDataManager aScheduleCenter) throws Exception {
		super(aFactory, baseTaskType, ownSign, aScheduleCenter);
	}
	public void initialRunningInfo() throws Exception{
		scheduleCenter.clearExpireScheduleServer(this.currenScheduleServer.getTaskType(),this.taskTypeInfo.getJudgeDeadInterval());
		List<String> list = scheduleCenter.loadScheduleServerNames(this.currenScheduleServer.getTaskType());
		if(scheduleCenter.isLeader(this.currenScheduleServer.getUuid(),list)){
	    	//�ǵ�һ����������������е���������
			log.debug(this.currenScheduleServer.getUuid() + ":" + list.size());
	    	this.scheduleCenter.initialRunningInfo4Static(this.currenScheduleServer.getBaseTaskType(), this.currenScheduleServer.getOwnSign(),this.currenScheduleServer.getUuid());
	    }
	 }
	public void initial() throws Exception{
    	new Thread(this.currenScheduleServer.getTaskType()  +"-" + this.currentSerialNumber +"-StartProcess"){
    		@SuppressWarnings("static-access")
			public void run(){
    			try{
    			   log.info("��ʼ��ȡ�����������...... of " + currenScheduleServer.getUuid());
    			   while (isRuntimeInfoInitial == false) {
 				      if(isStopSchedule == true){
				    	  log.debug("�ⲿ������ֹ����,�˳����ȶ��л�ȡ��" + currenScheduleServer.getUuid());
				    	  return;
				      }
 				      //log.error("isRuntimeInfoInitial = " + isRuntimeInfoInitial);
 				      try{
					  initialRunningInfo();
					  isRuntimeInfoInitial = scheduleCenter.isInitialRunningInfoSucuss(
										currenScheduleServer.getBaseTaskType(),
										currenScheduleServer.getOwnSign());
 				      }catch(Throwable e){
 				    	  //���Գ�ʼ�����쳣
 				    	  log.error(e.getMessage(),e);
 				      }
					  if(isRuntimeInfoInitial == false){
    				      Thread.currentThread().sleep(1000);
					  }
					}
    			   int count =0;
    			   lastReloadTaskItemListTime = scheduleCenter.getSystemTime();
				   while(getCurrentScheduleTaskItemListNow().size() <= 0){
    				      if(isStopSchedule == true){
    				    	  log.debug("�ⲿ������ֹ����,�˳����ȶ��л�ȡ��" + currenScheduleServer.getUuid());
    				    	  return;
    				      }
    				      Thread.currentThread().sleep(1000);
        			      count = count + 1;
        			     // log.error("���Ի�ȡ���ȶ��У���" + count + "�� ") ;
    			   }
    			   String tmpStr ="";
    			   for(int i=0;i< currentTaskItemList.size();i++){
    				   if(i>0){
    					   tmpStr = tmpStr +",";    					   
    				   }
    				   tmpStr = tmpStr + currentTaskItemList.get(i);
    			   }
    			   log.info("��ȡ����������У���ʼ���ȣ�" + tmpStr +"  of  "+ currenScheduleServer.getUuid());
    			   
    		    	//��������
    		    	taskItemCount = scheduleCenter.loadAllTaskItem(currenScheduleServer.getTaskType()).size();
    		    	//ֻ�����Ѿ���ȡ����������к�ſ�ʼ������������    			   
    			   computerStart();
    			}catch(Exception e){
    				log.error(e.getMessage(),e);
    				String str = e.getMessage();
    				if(str.length() > 300){
    					str = str.substring(0,300);
    				}
    				startErrorInfo = "���������쳣��" + str;
    			}
    		}
    	}.start();
    }
	/**
	 * ��ʱ�������������ĸ��µ�ǰ��������������Ϣ��
	 * ������ֱ��θ��µ�ʱ������Ѿ������ˣ��������������������ڣ��������������������Ϣ��
	 * ��Ӧ�õ����µķ���������������ע�ᡣ
	 * @throws Exception 
	 */
	public void refreshScheduleServerInfo() throws Exception {
	  try{
		rewriteScheduleInfo();
		//���������Ϣû�г�ʼ���ɹ�������������صĴ���
		if(this.isRuntimeInfoInitial == false){
			return;
		}
		
        //���·�������
        this.assignScheduleTask();
        
        //�ж��Ƿ���Ҫ���¼���������У�������������̲���Ҫ�ļ��͵ȴ�
        boolean tmpBoolean = this.isNeedReLoadTaskItemList();
        if(tmpBoolean != this.isNeedReloadTaskItem){
        	//ֻҪ����ͬ����������Ҫ����װ�أ���Ϊ�������쳣��ʱ������������е����飬�ָ�����Ҫ����װ�ء�
        	synchronized (NeedReloadTaskItemLock) {
        		this.isNeedReloadTaskItem = true;
        	}
        	rewriteScheduleInfo();
        }
        
        if(this.isPauseSchedule  == true || this.processor != null && processor.isSleeping() == true){
            //��������Ѿ���ͣ�ˣ�����Ҫ���¶�ʱ���� cur_server �� req_server
            //�������û����ͣ��һ�����ܵ��õ�
               this.getCurrentScheduleTaskItemListNow();
          }
		}catch(Throwable e){
			//����ڴ������е��Ѿ�ȡ�õ����ݺ��������,���������߳�ʧ��ʱ���µ������ظ�
			this.clearMemoInfo();
			if(e instanceof Exception){
				throw (Exception)e;
			}else{
			   throw new Exception(e.getMessage(),e);
			}
		}
	}	
	/**
	 * ��leader���·���������ÿ��server�ͷ�ԭ��ռ�е�������ʱ�������޸�����汾��
	 * @return
	 * @throws Exception
	 */
	public boolean isNeedReLoadTaskItemList() throws Exception{
		return this.lastFetchVersion < this.scheduleCenter.getReloadTaskItemFlag(this.currenScheduleServer.getTaskType());
	}
	/**
	 * ���ݵ�ǰ���ȷ���������Ϣ�����¼���������еĵ�������
	 * ����ķ�������Ҫ�������������ݷ������Ϊ�˱��������������ĸ������ã�ͨ���汾�����ﵽ����Ŀ��
	 * 
	 * 1����ȡ����״̬�İ汾��
	 * 2����ȡ���еķ�����ע����Ϣ�����������Ϣ
	 * 3������Ѿ������������ڵķ�����ע����Ϣ
	 * 3�����¼����������
	 * 4����������״̬�İ汾�š��ֹ�����
	 * 5����ϵ������еķ�����Ϣ
	 * @throws Exception 
	 */
	public void assignScheduleTask() throws Exception {
		scheduleCenter.clearExpireScheduleServer(this.currenScheduleServer.getTaskType(),this.taskTypeInfo.getJudgeDeadInterval());
		List<String> serverList = scheduleCenter
				.loadScheduleServerNames(this.currenScheduleServer.getTaskType());
		
		if(scheduleCenter.isLeader(this.currenScheduleServer.getUuid(), serverList)==false){
			if(log.isDebugEnabled()){
			   log.debug(this.currenScheduleServer.getUuid() +":���Ǹ�����������Leader,ֱ�ӷ���");
			}
			return;
		}
		//���ó�ʼ���ɹ���׼��������leaderת����ʱ���������߳����ʼ��ʧ��
		scheduleCenter.setInitialRunningInfoSucuss(this.currenScheduleServer.getBaseTaskType(), this.currenScheduleServer.getTaskType(), this.currenScheduleServer.getUuid());
		scheduleCenter.clearTaskItem(this.currenScheduleServer.getTaskType(), serverList);
		scheduleCenter.assignTaskItem(this.currenScheduleServer.getTaskType(),this.currenScheduleServer.getUuid(),this.taskTypeInfo.getMaxTaskItemsOfOneThreadGroup(),serverList);
	}	
	/**
	 * ���¼��ص�ǰ���������������
	 * 1���ͷŵ�ǰ���������У�������������������������������
	 * 2�����»�ȡ��ǰ�������Ĵ������
	 * 
	 * Ϊ�˱���˲����Ĺ��ȣ��������������ݴ���������ϵͳ����һ������װ�ص�Ƶ�ʡ�����1����
	 * 
	 * �ر�ע�⣺
	 *   �˷����ĵ��ñ������ڵ�ǰ�������񶼴�����Ϻ���ܵ��ã������Ƿ�������к�������ݱ��ظ�����
	 */
	
	public List<TaskItemDefine> getCurrentScheduleTaskItemList() {
		try{
		if (this.isNeedReloadTaskItem == true) {			
			//�ر�ע�⣺��Ҫ�ж����ݶ����Ƿ��Ѿ����ˣ���������ڶ����л���ʱ���������ظ�����
			//��Ҫ�����̲߳����߾ͼ������ݵ�ʱ��һ����Ҫ����ж�
			if (this.processor != null) {
					while (this.processor.isDealFinishAllData() == false) {
						Thread.sleep(50);
					}
			}
			//������ʼ��������
			synchronized (NeedReloadTaskItemLock) {
				this.getCurrentScheduleTaskItemListNow();
				this.isNeedReloadTaskItem = false;
			}
		}
		this.lastReloadTaskItemListTime = this.scheduleCenter.getSystemTime();		
		return this.currentTaskItemList;		
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	protected List<TaskItemDefine> getCurrentScheduleTaskItemListNow() throws Exception {
		//��ȡ���µİ汾��
		this.lastFetchVersion = this.scheduleCenter.getReloadTaskItemFlag(this.currenScheduleServer.getTaskType());
		try{
			//�Ƿ�������Ķ���
			this.scheduleCenter.releaseDealTaskItem(this.currenScheduleServer.getTaskType(), this.currenScheduleServer.getUuid());
			//���²�ѯ��ǰ�������ܹ�����Ķ���
			//Ϊ�˱����������л��Ĺ����г��ֶ���˲��Ĳ�һ�£�������ڴ��еĶ���
			this.currentTaskItemList.clear();
			this.currentTaskItemList = this.scheduleCenter.reloadDealTaskItem(
					this.currenScheduleServer.getTaskType(), this.currenScheduleServer.getUuid());
			
			//�������10���������ڻ�û�л�ȡ�����ȶ��У��򱨾�
			if(this.currentTaskItemList.size() ==0 && 
					scheduleCenter.getSystemTime() - this.lastReloadTaskItemListTime
					> this.taskTypeInfo.getHeartBeatRate() * 10){			
				String message ="���ȷ�����" + this.currenScheduleServer.getUuid() +"[TASK_TYPE=" + this.currenScheduleServer.getTaskType() + "]����������������10���������ڣ��� û�л�ȡ��������������";
				log.warn(message);
			}
			
			if(this.currentTaskItemList.size() >0){
				 //����ʱ���
				 this.lastReloadTaskItemListTime = scheduleCenter.getSystemTime();
			}
			
			return this.currentTaskItemList;
		}catch(Throwable e){
			this.lastFetchVersion = -1; //����ѰѰ汾������С�������������ʧ��
			if(e instanceof Exception ){
				throw (Exception)e;
			}else{
				throw new Exception(e);
			}
		}
	}	
	public int getTaskItemCount(){
		 return this.taskItemCount;
	}

}
