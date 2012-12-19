package com.taobao.pamirs.schedule.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.taobao.pamirs.schedule.ConsoleManager;
import com.taobao.pamirs.schedule.IScheduleTaskDeal;
import com.taobao.pamirs.schedule.ScheduleUtil;
import com.taobao.pamirs.schedule.taskmanager.IScheduleDataManager;
import com.taobao.pamirs.schedule.taskmanager.TBScheduleManagerStatic;
import com.taobao.pamirs.schedule.zk.ScheduleDataManager4ZK;
import com.taobao.pamirs.schedule.zk.ScheduleStrategyDataManager4ZK;
import com.taobao.pamirs.schedule.zk.ZKManager;

/**
 * ���ȷ�����������
 * 
 * @author xuannan
 * 
 */
public class TBScheduleManagerFactory implements ApplicationContextAware {
	protected static transient Log logger = LogFactory.getLog(TBScheduleManagerFactory.class);
	
	private Map<String,String> zkConfig;
	
	protected ZKManager zkManager;

	/**
	 * �Ƿ��������ȹ������ֻ����ϵͳ����Ӧ������Ϊfalse
	 */
	public boolean start = true;
	private int timerInterval = 2000;
	
	/**
	 * �����������Ŀͷ���
	 */
	private IScheduleDataManager	scheduleDataManager;
	private ScheduleStrategyDataManager4ZK scheduleStrategyManager;
	
	private Map<String,List<IStrategyTask>> managerMap = new ConcurrentHashMap<String, List<IStrategyTask>>();
	
	private ApplicationContext			applicationcontext;	
	private String uuid;
	private String ip;
	private String hostName;

	private Timer timer;
	protected Lock  lock = new ReentrantLock();
    
	volatile String  errorMessage ="No config Zookeeper connect infomation";
	private InitialThread initialThread;
	
	public TBScheduleManagerFactory() {
		this.ip = ScheduleUtil.getLocalIP();
		this.hostName = ScheduleUtil.getLocalHostName();
	}

	public void init() throws Exception {
		Properties properties = new Properties();
		for(Map.Entry<String,String> e: this.zkConfig.entrySet()){
			properties.put(e.getKey(),e.getValue());
		}
		this.init(properties);
	}
	
	public void reInit(Properties p) throws Exception{
		if(this.start == true || this.timer != null || this.managerMap.size() >0){
			throw new Exception("���������������������³�ʼ��");
		}
		this.init(p);
	}
	
	public void init(Properties p) throws Exception {
	    if(this.initialThread != null){
	    	this.initialThread.stopThread();
	    }
		this.lock.lock();
		try{
			this.scheduleDataManager = null;
			this.scheduleStrategyManager = null;
		    ConsoleManager.setScheduleManagerFactory(this);
		    if(this.zkManager != null){
				this.zkManager.close();
			}
			this.zkManager = new ZKManager(p);
			this.errorMessage = "Zookeeper connecting ......" + this.zkManager.getConnectStr();
			initialThread = new InitialThread(this);
			initialThread.setName("TBScheduleManagerFactory-initialThread");
			initialThread.start();
		}finally{
			this.lock.unlock();
		}
	}
    
    /**
     * ��Zk״̬������ص����ݳ�ʼ��
     * @throws Exception
     */
	public void initialData() throws Exception{
			this.zkManager.initial();
			this.scheduleDataManager = new ScheduleDataManager4ZK(this.zkManager);
			this.scheduleStrategyManager  = new ScheduleStrategyDataManager4ZK(this.zkManager);
			if (this.start == true) {
				// ע����ȹ�����
				this.scheduleStrategyManager.registerManagerFactory(this);
				timer = new Timer("TBScheduleManagerFactory-Timer");
				timer.schedule(new ManagerFactoryTimerTask(this), 2000,
						this.timerInterval);
			}
	}

	/**
	 * �������ȷ�����
	 * @param baseTaskType
	 * @param ownSign
	 * @return
	 * @throws Exception
	 */
	public IStrategyTask createStrategyTask(ScheduleStrategy strategy)
			throws Exception {
		IStrategyTask result = null;
		if(ScheduleStrategy.Kind.Schedule == strategy.getKind()){
			String baseTaskType = ScheduleUtil.splitBaseTaskTypeFromTaskType(strategy.getTaskName());
			String ownSign =ScheduleUtil.splitOwnsignFromTaskType(strategy.getTaskName());
			result = new TBScheduleManagerStatic(this,baseTaskType,ownSign,scheduleDataManager);
		}else if(ScheduleStrategy.Kind.Java == strategy.getKind()){
		    result=(IStrategyTask)Class.forName(strategy.getTaskName()).newInstance();
		    result.initialTaskParameter(strategy.getTaskParameter());
		}
		return result;
	}

	public void refresh() throws Exception{
		this.lock.lock();
		try{
		//�ж�״̬�Ƿ���ֹ
		ManagerFactoryInfo stsInfo = this.getScheduleStrategyManager().loadManagerFactoryInfo(this.getUuid());
		if(stsInfo.isStart() == false){
			stopServer(null); //ֹͣ���еĵ�������
			this.getScheduleStrategyManager().unRregisterManagerFactory(this);
		}else{
				List<String> stopList = this.getScheduleStrategyManager()
						.registerManagerFactory(this);
				for (String strategyName : stopList) {
					this.stopServer(strategyName);
				}
				this.assignScheduleServer();
				this.reRunScheduleServer();
			}
		}finally{
			this.lock.unlock();
		}
	}
	/**
	 * ���ݲ������·����������Ļ���
	 * @throws Exception
	 */
	public void assignScheduleServer() throws Exception{
		for(ScheduleStrategyRunntime run: this.scheduleStrategyManager.loadAllScheduleStrategyRunntimeByUUID(this.uuid)){
			List<ScheduleStrategyRunntime> factoryList = this.scheduleStrategyManager.loadAllScheduleStrategyRunntimeByTaskType(run.getStrategyName());
			if(factoryList.size() == 0 || this.isLeader(this.uuid, factoryList) ==false){
				continue;
			}
			ScheduleStrategy scheduleStrategy =this.scheduleStrategyManager.loadStrategy(run.getStrategyName());
			
			int[] nums =  ScheduleUtil.assignTaskNumber(factoryList.size(), scheduleStrategy.getAssignNum(), scheduleStrategy.getNumOfSingleServer());
			for(int i=0;i<factoryList.size();i++){
				ScheduleStrategyRunntime factory = 	factoryList.get(i);
				//��������ķ���������
				this.scheduleStrategyManager.updateStrategyRunntimeReqestNum(run.getStrategyName(), 
						factory.getUuid(),nums[i]);
			}
		}
	}
	
	public boolean isLeader(String uuid, List<ScheduleStrategyRunntime> factoryList) {
		long no = Long.parseLong(uuid.substring(uuid.lastIndexOf("$") + 1));
		for (ScheduleStrategyRunntime server : factoryList) {
			if (no > Long.parseLong(server.getUuid().substring(
					server.getUuid().lastIndexOf("$") + 1))) {
				return false;
			}
		}
		return true;
	}	
	public void reRunScheduleServer() throws Exception{
		for (ScheduleStrategyRunntime run : this.scheduleStrategyManager.loadAllScheduleStrategyRunntimeByUUID(this.uuid)) {
			List<IStrategyTask> list = this.managerMap.get(run.getStrategyName());
			if(list == null){
				list = new ArrayList<IStrategyTask>();
				this.managerMap.put(run.getStrategyName(),list);
			}
			while(list.size() > run.getRequestNum() && list.size() >0){
				IStrategyTask task  =  list.remove(list.size() - 1);
					try {
						task.stop();
					} catch (Throwable e) {
						logger.error("ע���������" + e.getMessage(), e);
					}
				}
		   //���㣬���ӵ�����
		   ScheduleStrategy strategy = this.scheduleStrategyManager.loadStrategy(run.getStrategyName());
		   while(list.size() < run.getRequestNum()){
			   IStrategyTask result = this.createStrategyTask(strategy);
			   list.add(result);
		    }
		}
	}
	
	/**
	 * ��ֹһ������
	 * 
	 * @param taskType
	 * @throws Exception
	 */
	public void stopServer(String strategyName) throws Exception {
		if(strategyName == null){
			String[] nameList = (String[])this.managerMap.keySet().toArray(new String[0]);
			for (String name : nameList) {
				for (IStrategyTask task : this.managerMap.get(name)) {
					try{
					  task.stop();
					}catch(Throwable e){
					  logger.error("ע���������"+e.getMessage(),e);
					}
				}
				this.managerMap.remove(name);
			}
		}else {
			List<IStrategyTask> list = this.managerMap.get(strategyName);
			if(list != null){
				for(IStrategyTask task:list){
					try {
						task.stop();
					} catch (Throwable e) {
						logger.error("ע���������" + e.getMessage(), e);
					}
				}
				this.managerMap.remove(strategyName);
			}
			
		}			
	}
	/**
	 * �������еķ���
	 * @throws Exception
	 */
	public void reStart() throws Exception {
		try {
			if (this.timer != null) {
				this.timer.cancel();
				this.timer = null;
			}
			this.stopServer(null);
			this.zkManager.close();
			this.uuid = null;
			this.init();
		} catch (Throwable e) {
			logger.error("��������ʧ�ܣ�" + e.getMessage(), e);
		}
    }
    public boolean isZookeeperInitialSucess() throws Exception{
    	return this.zkManager.checkZookeeperState();
    }
	public String[] getScheduleTaskDealList() {
		return applicationcontext.getBeanNamesForType(IScheduleTaskDeal.class);

	}
    
	public IScheduleDataManager getScheduleDataManager() {
		if(this.scheduleDataManager == null){
			throw new RuntimeException(this.errorMessage);
		}
		return scheduleDataManager;
	}
	public ScheduleStrategyDataManager4ZK getScheduleStrategyManager() {
		if(this.scheduleStrategyManager == null){
			throw new RuntimeException(this.errorMessage);
		}
		return scheduleStrategyManager;
	}

	public void setApplicationContext(ApplicationContext aApplicationcontext) throws BeansException {
		applicationcontext = aApplicationcontext;
	}

	public Object getBean(String beanName) {
		return applicationcontext.getBean(beanName);
	}
	public String getUuid() {
		return uuid;
	}

	public String getIp() {
		return ip;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getHostName() {
		return hostName;
	}

	public void setStart(boolean isStart) {
		this.start = isStart;
	}

	public void setTimerInterval(int timerInterval) {
		this.timerInterval = timerInterval;
	}
	public void setZkConfig(Map<String,String> zkConfig) {
		this.zkConfig = zkConfig;
	}

	public Map<String,String> getZkConfig() {
		return zkConfig;
	}
}

class ManagerFactoryTimerTask extends java.util.TimerTask {
	private static transient Log log = LogFactory.getLog(ManagerFactoryTimerTask.class);
	TBScheduleManagerFactory factory;
	int count =0;

	public ManagerFactoryTimerTask(TBScheduleManagerFactory aFactory) {
		this.factory = aFactory;
	}

	public void run() {
		try {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			if(this.factory.zkManager.checkZookeeperState() == false){
				if(count > 5){
				   log.error("Zookeeper����ʧ�ܣ��ر����е��������������Zookeeper������......");
				   this.factory.reStart();
				  
				}else{
				   count = count + 1;
				}
			}else{
				count = 0;
			    this.factory.refresh();
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		}
	}
}

class InitialThread extends Thread{
	private static transient Log log = LogFactory.getLog(InitialThread.class);
	TBScheduleManagerFactory facotry;
	boolean isStop = false;
	public InitialThread(TBScheduleManagerFactory aFactory){
		this.facotry = aFactory;
	}
	public void stopThread(){
		this.isStop = true;
	}
	@Override
	public void run() {
		facotry.lock.lock();
		try {
			int count =0;
			while(facotry.zkManager.checkZookeeperState() == false){
				count = count + 1;
				if(count % 50 == 0){
					facotry.errorMessage = "Zookeeper connecting ......" + facotry.zkManager.getConnectStr() + " spendTime:" + count * 20 +"(ms)";
					log.error(facotry.errorMessage);
				}
				Thread.sleep(20);
				if(this.isStop ==true){
					return;
				}
			}
			facotry.initialData();
		} catch (Throwable e) {
			 log.error(e.getMessage(),e);
		}finally{
			facotry.lock.unlock();
		}

	}
	
}