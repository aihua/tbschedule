package com.taobao.pamirs.schedule;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.taobao.pamirs.schedule.zk.ScheduleDataManager4ZK;
import com.taobao.pamirs.schedule.zk.ScheduleStrategyDataManager4ZK;
import com.taobao.pamirs.schedule.zk.ZKManager;

/**
 * 调度服务器构造器
 * 
 * @author xuannan
 * 
 */
public class TBScheduleManagerFactory implements ApplicationContextAware {
	protected static transient Log logger = LogFactory.getLog(TBScheduleManagerFactory.class);
	
	private Map<String,String> zkConfig;
	
	protected ZKManager zkManager;

	/**
	 * 是否启动调度管理，如果只是做系统管理，应该设置为false
	 */
	boolean start = true;
	private int timerInterval = 2000;
	
	/**
	 * 调度配置中心客服端
	 */
	private IScheduleDataManager	scheduleDataManager;
	private ScheduleStrategyDataManager4ZK scheduleStrategyManager;
	
	private Map<String, TBScheduleManager> managerMap = new ConcurrentHashMap<String, TBScheduleManager>();
	
	protected Map<String,String> baseTaskList = new ConcurrentHashMap<String,String>();
	/**
	 * 临时终止的任务，方便后续恢复 baseType$ownSign
	 */
	private Map<String,AtomicInteger> pauseTaskType = new ConcurrentHashMap<String,AtomicInteger>();

	private ApplicationContext			applicationcontext;
		
	private static String mbeanServerAgentId;
	
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
			throw new Exception("调度器有任务处理，不能重新初始化");
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
			initialThread.start();
		}finally{
			this.lock.unlock();
		}
	}
    
    /**
     * 在Zk状态正常后回调数据初始化
     * @throws Exception
     */
	public void initialData() throws Exception{
			this.zkManager.initial();
			this.scheduleDataManager = new ScheduleDataManager4ZK(this.zkManager);
			this.scheduleStrategyManager  = new ScheduleStrategyDataManager4ZK(this.zkManager);
			if (this.start == true) {
				// 注册调度管理器
				this.scheduleStrategyManager.registerManagerFactory(this);
				timer = new Timer("TBScheduleManagerFactory-Timer");
				timer.schedule(new ManagerFactoryTimerTask(this), 2000,
						this.timerInterval);
			}
	}

	/**
	 * 创建调度服务器
	 * @param baseTaskType
	 * @param dealBeanName
	 * @param ownSign
	 * @return
	 * @throws Exception
	 */
	public TBScheduleManager createTBScheduleManager(String baseTaskType,String dealBeanName,String ownSign)
			throws Exception {
		if (scheduleDataManager == null) {
			throw new Exception("没有设置配置中心客户端的接口实现，请在Spring中配置，或者通过程序设置");
		}
		this.lock.lock();
		try{
			//清除已经过期1天的TASK,OWN_SIGN的组合。超过一天没有活动server的视为过期
			ScheduleTaskType baseTaskTypeInfo = scheduleDataManager.loadTaskTypeBaseInfo(baseTaskType);
		
			scheduleDataManager.clearExpireTaskTypeRunningInfo(baseTaskType,ScheduleUtil.getLocalIP() + "清除过期OWN_SIGN信息",baseTaskTypeInfo.getExpireOwnSignInterval());
			
			if(ScheduleTaskType.STS_PAUSE.equals(baseTaskTypeInfo.getSts())==true){
				this.baseTaskList.put(baseTaskType,baseTaskType);
				String taskType = TBScheduleManager.getTaskTypeByBaseAndOwnSign(baseTaskType, ownSign);
				AtomicInteger count= (AtomicInteger)this.pauseTaskType.get(taskType);
				if(count == null){
					count = new AtomicInteger(1);
				}else{
					count.incrementAndGet();
				}
				this.pauseTaskType.put(taskType,count);
				return null;
			}
			
			if(dealBeanName == null || dealBeanName.trim().length()==0){
				dealBeanName = baseTaskTypeInfo.getDealBeanName();
			}
			Object dealBean = applicationcontext.getBean(dealBeanName);
		
			if (dealBean == null) {
				throw new Exception( "SpringBean " + dealBeanName + " 不存在");
			}
			if (dealBean instanceof IScheduleTaskDeal == false) {
				throw new Exception( "SpringBean " + baseTaskTypeInfo.getDealBeanName() + " 没有实现 IScheduleTaskDeal接口");
			}
			TBScheduleManager result = new TBScheduleManagerStatic(this,baseTaskType,ownSign,scheduleDataManager,
				(IScheduleTaskDeal<?>)dealBean);
			String key = TBScheduleManager.getTaskTypeByBaseAndOwnSign(baseTaskType, ownSign) +"$"+result.hashCode();
			managerMap.put(key, result);
			this.baseTaskList.put(baseTaskType,baseTaskType);
			return result;
		}finally{
			this.lock.unlock();
		}
	}


	public void unregister(TBScheduleManager manager) throws Exception{
		String key = manager.getScheduleServer().getTaskType()+"$"+ manager.hashCode();
		managerMap.remove(key);
	}


	public void refresh() throws Exception{
		this.lock.lock();
		try{
		//判断状态是否终止
		ManagerFactoryInfo stsInfo = this.getScheduleStrategyManager().loadManagerFactoryInfo(this.getUuid());
		if(stsInfo.isStart() == false){
			stopServer(null); //停止所有的调度任务
			this.getScheduleStrategyManager().unRregisterManagerFactory(this);
		}else{
				for (String baseTaskType : this.baseTaskList.keySet()) {
					ScheduleTaskType taskType = this.getScheduleDataManager()
							.loadTaskTypeBaseInfo(baseTaskType);
					if (ScheduleTaskType.STS_PAUSE.equals(taskType.getSts())) {
						this.pauseServer(baseTaskType);
					} else if (ScheduleTaskType.STS_RESUME.equals(taskType
							.getSts())) {
						this.resumeServer(baseTaskType);
					}
				}
				List<String> stopList = this.getScheduleStrategyManager()
						.registerManagerFactory(this);
				for (String taskType : stopList) {
					this.stopServer(taskType);
				}
				this.assignScheduleServer();
				this.reRunScheduleServer();
			}
		}finally{
			this.lock.unlock();
		}
	}
	/**
	 * 根据策略重新分配调度任务的机器
	 * @throws Exception
	 */
	public void assignScheduleServer() throws Exception{
		for(ScheduleStrategyRunntime run: this.scheduleStrategyManager.loadAllScheduleStrategyRunntimeByUUID(this.uuid)){
			List<ScheduleStrategyRunntime> factoryList = this.scheduleStrategyManager.loadAllScheduleStrategyRunntimeByTaskType(run.getTaskType());
			if(factoryList.size() == 0 || this.isLeader(this.uuid, factoryList) ==false){
				continue;
			}
			ScheduleStrategy scheduleStrategy =this.scheduleStrategyManager.loadStrategy(run.getTaskType());
			
			int[] nums = new int[factoryList.size()];
			if(scheduleStrategy.getNumOfSingleServer() >0){
				int count =0;
				for(int i=0;i<factoryList.size();i++){
					if( scheduleStrategy.getAssignNum() - count > scheduleStrategy.getNumOfSingleServer()){
						nums[i] = scheduleStrategy.getNumOfSingleServer();
					}else{
						nums[i] = scheduleStrategy.getAssignNum() - count;
					}
					count = count + nums[i];
				}
			}else{
				int numOfSingle = scheduleStrategy.getAssignNum() / factoryList.size();
				int otherNum = scheduleStrategy.getAssignNum() % factoryList.size();
				for(int i=0;i<factoryList.size();i++){
					if(i<otherNum){
						nums[i] = numOfSingle + 1;
					}else{
						nums[i] = numOfSingle;
					}
				}
			}

			for(int i=0;i<factoryList.size();i++){
				ScheduleStrategyRunntime factory = 	factoryList.get(i);
				//更新请求的服务器数量
				this.scheduleStrategyManager.updateStrategyRunntimeReqestNum(run.getTaskType(), 
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
			for(ScheduleStrategyRunntime run:this.scheduleStrategyManager.loadAllScheduleStrategyRunntimeByUUID(this.uuid)){
				AtomicInteger num = this.pauseTaskType.get(run.getTaskType());
				if (num != null && num.get() > 0) {// 暂停状态
					num.set(run.getRequestNum());
				} else {// 是运行状态
					int count = 1;
					for (TBScheduleManager manager : this.managerMap.values()) {
						if (manager.getScheduleServer().getTaskType().equals(run.getTaskType())) {
							if(count > run.getRequestNum()){//多了，删除调度器
								this.unregister(manager);
								manager.stopScheduleServer();
							}else{
								count = count + 1;
							}
						}
					}
					//不足，增加调度器
					for (int i = count; i <= run.getRequestNum(); i++) {
						String baseTaskType = TBScheduleManager.splitBaseTaskTypeFromTaskType(run.getTaskType());
						if(this.scheduleDataManager.loadTaskTypeBaseInfo(baseTaskType)== null){
							String message ="不能识别的任务名称:" + baseTaskType;
							this.scheduleStrategyManager.updateStrategyRunntimeErrorMessage(run.getTaskType(),this.uuid, message);
							logger.error(message);
							continue;
						}
						this.createTBScheduleManager(baseTaskType,TBScheduleManager.splitOwnsignFromTaskType(run.getTaskType()));
					}
				}
			}	
	}
	
	/**
	 * 终止一类任务
	 * 
	 * @param taskType
	 * @throws Exception
	 */
	public void stopServer(String taskType) throws Exception {
			for (TBScheduleManager manager : this.managerMap.values()) {
				if (taskType == null || manager.getScheduleServer().getTaskType().equals(taskType)) {
					this.unregister(manager);
					manager.stopScheduleServer();
				}
			}
	}
	
	/**
	 * 在控制台临时终止调度服务
	 * @param taskType
	 * @throws Exception
	 */
	public void pauseServer(String baseTaskType) throws Exception{
			for (TBScheduleManager manager : this.managerMap.values()) {
				if (manager.getScheduleServer().getBaseTaskType()
						.equals(baseTaskType)) {
					AtomicInteger count = (AtomicInteger) this.pauseTaskType
							.get(manager.getScheduleServer().getTaskType());
					if (count == null) {
						count = new AtomicInteger(1);
					} else {
						count.incrementAndGet();
					}
					this.pauseTaskType.put(manager.getScheduleServer()
							.getTaskType(), count);
					this.unregister(manager);
					manager.stopScheduleServer();
				}
			}
	}
	/**
	 * 在控制台恢复暂停的调度服务
	 * @param baseTaskType
	 * @throws Exception
	 */
	public void resumeServer(String baseTaskType) throws Exception{
		for(String taskType :this.pauseTaskType.keySet()){
			if(TBScheduleManager.splitBaseTaskTypeFromTaskType(taskType).equals(baseTaskType)){
				String ownSign = TBScheduleManager.splitOwnsignFromTaskType(taskType);
				AtomicInteger count=this.pauseTaskType.remove(taskType);
				for(int i=0;i<count.get();i++){
					this.createTBScheduleManager(baseTaskType, ownSign);
				}
			}
		}
	}
		
	public TBScheduleManager createTBScheduleManager(String taskType,String ownSign) throws Exception {
		return createTBScheduleManager(taskType,null, ownSign );
	}
    public boolean isZookeeperInitialSucess(){
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
	public void setMbeanServerAgentId(String aMbeanServerAgentId) {
		mbeanServerAgentId = aMbeanServerAgentId;
	}
	public static String getMbeanServerAgentId() {
		return mbeanServerAgentId;
	}
	public Map<String, TBScheduleManager> getManagerMap() {
		return managerMap;
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

	public ManagerFactoryTimerTask(TBScheduleManagerFactory aFactory) {
		this.factory = aFactory;
	}

	public void run() {
		try {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			this.factory.refresh();
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
					log.warn(facotry.errorMessage);
				}
				Thread.sleep(20);
				if(this.isStop ==true){
					return;
				}
			}
			facotry.initialData();
		} catch (Exception e) {
			 log.error(e.getMessage(),e);
		}finally{
			facotry.lock.unlock();
		}

	}
	
}