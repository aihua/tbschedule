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
	private boolean isCheckParentPath = true;

	public enum keys {
		zkConnectString, rootPath, userName, password, zkSessionTimeout, isCheckParentPath, zkAuthCharset
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
		
		this.isCheckParentPath = Boolean.parseBoolean(this.properties.getProperty(keys.isCheckParentPath.toString(),"true"));
		String charset = this.properties.getProperty(keys.zkAuthCharset.toString());
		if (charset == null) {
			zk.addAuthInfo("digest", authString.getBytes());
		} else {
			zk.addAuthInfo("digest", authString.getBytes(charset));
		}
		acl.add(new ACL(ZooDefs.Perms.ALL, new Id("digest",
				DigestAuthenticationProvider.generateDigest(authString))));
		acl.add(new ACL(ZooDefs.Perms.READ, Ids.ANYONE_ID_UNSAFE));
	}
	
	/**
	 * �����B��zookeeper
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
			log.info("�յ�ZK���ӳɹ��¼���");
		} else if (event.getState() == KeeperState.Expired) {
			log.error("�Ự��ʱ���ȴ����½���ZK����...");
			try {
				reConnection();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}else{
			log.info("�Ѿ�������" + event.getType() + ":"+ event.getState() + "�¼���" + event.getPath());
		}
	}
	public void close() throws InterruptedException {
		log.info("�ر�zookeeper����");
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
		return zk.getState() == States.CONNECTED;
	}
	public void initial() throws Exception {
		//��zk״̬�����ܵ���
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
					throw new Exception("Pamirs-Schedule����汾 "+ Version.getVersion() +" ������Zookeeper�е���ݰ汾 " + dataVersion );
				}
				log.info("��ǰ�ĳ���汾:" + Version.getVersion() + " ��ݰ汾: " + dataVersion);
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
