package com.taobao.pamirs.schedule.zk;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZKManager{
	
	private static transient Logger log = LoggerFactory.getLogger(ZKManager.class);
	private ZooKeeper zk;
	private List<ACL> acl = new ArrayList<ACL>();
	private Properties properties;
	private boolean isCheckParentPath = true;
	
	public enum keys {
		zkConnectString, rootPath, userName, password, zkSessionTimeout, isCheckParentPath
	}

	public ZKManager(Properties aProperties) throws Exception{
		this.properties = aProperties;
		this.connect();
	}
	
	/**
	 * ����zookeeper
	 * @throws Exception
	 */
	public synchronized void  reConnection() throws Exception{
		if (this.zk != null) {
			this.zk.close();
			this.zk = null;
			this.connect() ;
		}
	}
	
	private void connect() throws Exception {
		CountDownLatch connectionLatch = new CountDownLatch(1);
		createZookeeper(connectionLatch);
		connectionLatch.await(10,TimeUnit.SECONDS);
	}
	
	private void createZookeeper(final CountDownLatch connectionLatch) throws Exception {
		zk = new ZooKeeper(this.properties.getProperty(keys.zkConnectString
				.toString()), Integer.parseInt(this.properties
				.getProperty(keys.zkSessionTimeout.toString())),
				new Watcher() {
					public void process(WatchedEvent event) {
						sessionEvent(connectionLatch, event);
					}
				});
		String authString = this.properties.getProperty(keys.userName.toString())
				+ ":"+ this.properties.getProperty(keys.password.toString());
		this.isCheckParentPath = Boolean.parseBoolean(this.properties.getProperty(keys.isCheckParentPath.toString(),"true"));
		zk.addAuthInfo("digest", authString.getBytes());
		acl.clear();
		acl.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",
				DigestAuthenticationProvider.generateDigest(authString))));
		acl.add(new ACL(ZooDefs.Perms.READ, Ids.ANYONE_ID_UNSAFE));
	}
	
	private void sessionEvent(CountDownLatch connectionLatch, WatchedEvent event) {
		if (event.getState() == KeeperState.SyncConnected) {
			log.info("�յ�ZK���ӳɹ��¼���");
			connectionLatch.countDown();
		} else if (event.getState() == KeeperState.Expired ) {
			log.error("�Ự��ʱ���ȴ����½���ZK����...");
			try {
				reConnection();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		} // Disconnected��Zookeeper���Զ�����Disconnected״̬����
		else if (event.getState() == KeeperState.Disconnected ) {
			log.info("tb_hj_schedule Disconnected���ȴ����½���ZK����...");
			try {
				reConnection();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
		else if (event.getState() == KeeperState.NoSyncConnected ) {
			log.info("tb_hj_schedule NoSyncConnected���ȴ����½���ZK����...");
			try {
				reConnection();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
		else{
			log.info("tb_hj_schedule �Ự������״̬��ֵ��event.getState() ="+event.getState() +", event  value="+event.toString());
			connectionLatch.countDown();
		}
	}
	
	public void close() throws InterruptedException {
		log.info("�ر�zookeeper����");
		if(zk == null) {
 		    return;
		}
		this.zk.close();
	}
	public static Properties createProperties(){
		Properties result = new Properties();
		result.setProperty(keys.zkConnectString.toString(),"localhost:2181");
		result.setProperty(keys.rootPath.toString(),"/taobao-pamirs-schedule/huijin");
		result.setProperty(keys.userName.toString(),"ScheduleAdmin");
		result.setProperty(keys.password.toString(),"password");
		result.setProperty(keys.zkSessionTimeout.toString(),"60000");
		result.setProperty(keys.isCheckParentPath.toString(),"true");
		
		return result;
	}
	public String getRootPath(){
		return this.properties.getProperty(keys.rootPath.toString());
	}
	public String getConnectStr(){
		return this.properties.getProperty(keys.zkConnectString.toString());
	}
	public boolean checkZookeeperState() throws Exception{
		return zk != null && zk.getState() == States.CONNECTED;
	}
	public void initial() throws Exception {
		//��zk״̬��������ܵ���
		if(zk.exists(this.getRootPath(), false) == null){
			ZKTools.createPath(zk, this.getRootPath(), CreateMode.PERSISTENT, acl);
			if(isCheckParentPath == true){
			  checkParent(zk,this.getRootPath());
			}
			//���ð汾��Ϣ
			zk.setData(this.getRootPath(),Version.getVersion().getBytes(),-1);
		}else{
			//��У�鸸�׽ڵ㣬�����Ƿ��Ѿ���schedule��Ŀ¼
			if(isCheckParentPath == true){
			   checkParent(zk,this.getRootPath());
			}
			byte[] value = zk.getData(this.getRootPath(), false, null);
			if(value == null){
				zk.setData(this.getRootPath(),Version.getVersion().getBytes(),-1);
			}else{
				String dataVersion = new String(value);
				if(Version.isCompatible(dataVersion)==false){
					throw new Exception("TBSchedule����汾 "+ Version.getVersion() +" ������Zookeeper�е����ݰ汾 " + dataVersion );
				}
				log.info("��ǰ�ĳ���汾:" + Version.getVersion() + " ���ݰ汾: " + dataVersion);
			}
		}
	}
	public static void checkParent(ZooKeeper zk, String path) throws Exception {
		String[] list = path.split("/");
		String zkPath = "";
		for (int i =0;i< list.length -1;i++){
			String str = list[i];
			if (str.equals("") == false) {
				zkPath = zkPath + "/" + str;
				if (zk.exists(zkPath, false) != null) {
					byte[] value = zk.getData(zkPath, false, null);
					if(value != null){
						String tmpVersion = new String(value);
					   if(tmpVersion.indexOf("taobao-pamirs-schedule-") >=0){
						throw new Exception("\"" + zkPath +"\"  is already a schedule instance's root directory, its any subdirectory cannot as the root directory of others");
					}
				}
			}
			}
		}
	}	
	
	public List<ACL> getAcl() {
		return acl;
	}
	public ZooKeeper getZooKeeper() throws Exception {
		if(this.checkZookeeperState()==false){
			reConnection();
		}
		return this.zk;
	}
	
}
