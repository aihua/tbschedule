package com.taobao.pamirs.schedule.zk;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

public class ZKManager implements Watcher{
	private static transient Log log = LogFactory.getLog(ZKManager.class);
	private ZooKeeper zk;
	private List<ACL> acl = new ArrayList<ACL>();
	private Properties properties;
	
	public enum keys {
		zkConnectString, rootPath, userName, password, zkSessionTimeout
	}

	public ZKManager(Properties aProperties) throws Exception{
		this.properties = aProperties;
		this.createZooKeeper();
	}
	
	private void createZooKeeper() throws Exception{
		String authString = this.properties.getProperty(keys.userName.toString())
				+ ":"+ this.properties.getProperty(keys.password.toString());
		zk = new ZooKeeper(this.properties.getProperty(keys.zkConnectString
				.toString()), Integer.parseInt(this.properties
				.getProperty(keys.zkSessionTimeout.toString())),
				this);
		zk.addAuthInfo("digest", authString.getBytes());
		acl.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",
				DigestAuthenticationProvider.generateDigest(authString))));
		acl.add(new ACL(ZooDefs.Perms.READ, Ids.ANYONE_ID_UNSAFE));
	}
	
	/**
	 * 重新連接zookeeper
	 * @throws Exception
	 */
	public synchronized void  reConnection() throws Exception{
		if (this.zk.getState() == States.CLOSED) {
			if (this.zk != null) {
				this.zk.close();
				this.zk = null;
			}
			this.createZooKeeper();
		}
	}
	
	public void process(WatchedEvent event) {
		if (event.getState() == KeeperState.SyncConnected) {
			log.info("收到ZK连接成功事件！");
		} else if (event.getState() == KeeperState.Expired) {
			log.error("会话超时，等待重新建立ZK连接...");
			try {
				reConnection();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}else{
			log.info("已经触发了" + event.getType() + "事件！" + event.getPath());
		}
	}
	public void close() throws InterruptedException {
		log.info("关闭zookeeper连接");
		this.zk.close();
	}
	public static Properties createProperties(){
		Properties result = new Properties();
		result.setProperty(keys.zkConnectString.toString(),"localhost:2181");
		result.setProperty(keys.rootPath.toString(),"/taobao-pamirs-schedule/huijin");
		result.setProperty(keys.userName.toString(),"ScheduleAdmin");
		result.setProperty(keys.password.toString(),"password");
		result.setProperty(keys.zkSessionTimeout.toString(),"3000");
		return result;
	}
	public String getRootPath(){
		return this.properties.getProperty(keys.rootPath.toString());
	}
	public String getConnectStr(){
		return this.properties.getProperty(keys.zkConnectString.toString());
	}
	public boolean checkZookeeperState() throws Exception{
		return zk.getState() == States.CONNECTED;
	}

	public void initial() throws Exception {
		//当zk状态正常后才能调用
		if(zk.exists(this.getRootPath(), false) == null){
			ZKTools.createPath(zk, this.getRootPath(), CreateMode.PERSISTENT, acl);
			checkParent(zk,this.getRootPath());
			//设置版本信息
			zk.setData(this.getRootPath(),Version.getVersion().getBytes(),-1);
		}else{
			//先校验父亲节点，本身是否已经是schedule的目录
			checkParent(zk,this.getRootPath());
			byte[] value = zk.getData(this.getRootPath(), false, null);
			if(value == null){
				zk.setData(this.getRootPath(),Version.getVersion().getBytes(),-1);
			}else{
				String dataVersion = new String(value);
				if(Version.isCompatible(dataVersion)==false){
					throw new Exception("Pamirs-Schedule程序版本 "+ Version.getVersion() +" 不兼容Zookeeper中的数据版本 " + dataVersion );
				}
				log.info("当前的程序版本:" + Version.getVersion() + " 数据版本: " + dataVersion);
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
			throw new Exception("Zookeeper["+ this.getConnectStr()+"] connect error :" + this.zk.getState() );
		}
		return this.zk;
	}
	
	}
