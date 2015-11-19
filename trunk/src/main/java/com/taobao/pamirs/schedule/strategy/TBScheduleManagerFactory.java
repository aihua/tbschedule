package com.taobao.pamirs.schedule.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	protected static transient Logger logger = LoggerFactory.getLogger(TBScheduleManagerFactory.class);
	
	private Map<String,String> zkConfig;
	
	protected ZKManager zkManager;



	/**
	 * �Ƿ��������ȹ������ֻ����ϵͳ����Ӧ������Ϊfalse
	 */
	public boolean start = true;
	private int timerInterval = 2000;
	/**
	 * ManagerFactoryTimerTask�ϴ�ִ�е�ʱ�����<br/>
	 * zk�������ȶ������ܵ�������task��ѭ����ʧ������ֹͣ��<br/>
	 * ���Ӧ�ã�ͨ��jmx��¶����ʱ�䣬������tbschedule����Ҫ�Ĵ�ѭ����<br/>
	 */
	public volatile long timerTaskHeartBeatTS = System.currentTimeMillis();
	
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
	private ManagerFactoryTimerTask timerTask;
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
				if(timer == null){
					timer = new Timer("TBScheduleManagerFactory-Timer");
				}
				if(timerTask == null){
					timerTask = new ManagerFactoryTimerTask(this);
					timer.schedule(timerTask, 2000,this.timerInterval);
				}
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
		try{
			if(ScheduleStrategy.Kind.Schedule == strategy.getKind()){
				String baseTaskType = ScheduleUtil.splitBaseTaskTypeFromTaskType(strategy.getTaskName());
				String ownSign =ScheduleUtil.splitOwnsignFromTaskType(strategy.getTaskName());
				result = new TBScheduleManagerStatic(this,baseTaskType,ownSign,scheduleDataManager);
			}else if(ScheduleStrategy.Kind.Java == strategy.getKind()){
			    result=(IStrategyTask)Class.forName(strategy.getTaskName()).newInstance();
			    result.initialTaskParameter(strategy.getStrategyName(),strategy.getTaskParameter());
			}else if(ScheduleStrategy.Kind.Bean == strategy.getKind()){
			    result=(IStrategyTask)this.getBean(strategy.getTaskName());
			    result.initialTaskParameter(strategy.getStrategyName(),strategy.getTaskParameter());
			}
		}catch(Exception e ){
			logger.error("strategy ��ȡ��Ӧ��java or bean ����,schedule��û�м��ظ�����,��ȷ��" +strategy.getStrategyName(),e);
		}
		return result;
	}

	public void refresh() throws Exception {
		this.lock.lock();
		try {
			// �ж�״̬�Ƿ���ֹ
			ManagerFactoryInfo stsInfo = null;
			boolean isException = false;
			try {
				stsInfo = this.getScheduleStrategyManager().loadManagerFactoryInfo(this.getUuid());
			} catch (Exception e) {
				isException = true;
				logger.error("��ȡ��������Ϣ����uuid="+this.getUuid(), e);
			}
			if (isException == true) {
				try {
					stopServer(null); // ֹͣ���еĵ�������
					this.getScheduleStrategyManager().unRregisterManagerFactory(this);
				} finally {
					reRegisterManagerFactory();
				}
			} else if (stsInfo.isStart() == false) {
				stopServer(null); // ֹͣ���еĵ�������
				this.getScheduleStrategyManager().unRregisterManagerFactory(
						this);
			} else {
				reRegisterManagerFactory();
			}
		} finally {
			this.lock.unlock();
		}
	}
	public void reRegisterManagerFactory() throws Exception{
		//���·��������
		List<String> stopList = this.getScheduleStrategyManager().registerManagerFactory(this);
		for (String strategyName : stopList) {
			this.stopServer(strategyName);
		}
		this.assignScheduleServer();
		this.reRunScheduleServer();
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
		try {
			long no = Long.parseLong(uuid.substring(uuid.lastIndexOf("$") + 1));
			for (ScheduleStrategyRunntime server : factoryList) {
				if (no > Long.parseLong(server.getUuid().substring(
						server.getUuid().lastIndexOf("$") + 1))) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			logger.error("�ж�Leader����uuif="+uuid, e);
			return true;
		}
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
						task.stop(run.getStrategyName());
					} catch (Throwable e) {
						logger.error("ע���������strategyName=" + run.getStrategyName(), e);
					}
				}
		   //���㣬���ӵ�����
		   ScheduleStrategy strategy = this.scheduleStrategyManager.loadStrategy(run.getStrategyName());
		   while(list.size() < run.getRequestNum()){
			   IStrategyTask result = this.createStrategyTask(strategy);
			   if(null==result){
				   logger.error("strategy ��Ӧ�����������⡣strategy name="+strategy.getStrategyName());
			   }
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
					  task.stop(strategyName);
					}catch(Throwable e){
					  logger.error("ע���������strategyName="+strategyName,e);
					}
				}
				this.managerMap.remove(name);
			}
		}else {
			List<IStrategyTask> list = this.managerMap.get(strategyName);
			if(list != null){
				for(IStrategyTask task:list){
					try {
						task.stop(strategyName);
					} catch (Throwable e) {
						logger.error("ע���������strategyName=" + strategyName, e);
					}
				}
				this.managerMap.remove(strategyName);
			}
			
		}			
	}
	/**
	 * ֹͣ���е�����Դ
	 */
	public void stopAll() throws Exception {
		try {
			lock.lock();
			this.start = false;
			if (this.initialThread != null) {
				this.initialThread.stopThread();
			}
			if (this.timer != null) {
				if (this.timerTask != null) {
					this.timerTask.cancel();
					this.timerTask = null;
				}
				this.timer.cancel();
				this.timer = null;
			}
			this.stopServer(null);
			if (this.zkManager != null) {
				this.zkManager.close();
			}
			if (this.scheduleStrategyManager != null) {
				try {
					ZooKeeper zk = this.scheduleStrategyManager.getZooKeeper();
					if (zk != null) {
						zk.close();
					}
				} catch (Exception e) {
					logger.error("stopAll zk getZooKeeper�쳣��",e);
				}
			}
			this.uuid = null;
			logger.info("stopAll ֹͣ����ɹ���");
		} catch (Throwable e) {
			logger.error("stopAll ֹͣ����ʧ�ܣ�" + e.getMessage(), e);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * �������еķ���
	 * @throws Exception
	 */
	public void reStart() throws Exception {
		try {
			if (this.timer != null) {
				if(this.timerTask != null){
					this.timerTask.cancel();
					this.timerTask = null;
				}
				this.timer.purge();
			}
			this.stopServer(null);
			if (this.zkManager != null) {
				this.zkManager.close();
			}
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
	public ZKManager getZkManager() {
		return this.zkManager;
	}
	public Map<String,String> getZkConfig() {
		return zkConfig;
	}
}

class ManagerFactoryTimerTask extends java.util.TimerTask {
	private static transient Logger log = LoggerFactory.getLogger(ManagerFactoryTimerTask.class);
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

		}  catch (Throwable ex) {
			log.error(ex.getMessage(), ex);
		} finally {
		    factory.timerTaskHeartBeatTS = System.currentTimeMillis();
		}
	}
}

class InitialThread extends Thread{
	private static transient Logger log = LoggerFactory.getLogger(InitialThread.class);
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