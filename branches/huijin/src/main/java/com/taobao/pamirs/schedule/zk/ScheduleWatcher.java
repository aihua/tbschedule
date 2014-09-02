package com.taobao.pamirs.schedule.zk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleWatcher implements Watcher {
	private static transient Logger log = LoggerFactory.getLogger(ScheduleWatcher.class);
	private Map<String,Watcher> route = new ConcurrentHashMap<String,Watcher>();
	private ZKManager manager;
	public ScheduleWatcher(ZKManager aManager){
		this.manager = aManager;
	}
	public void registerChildrenChanged(String path,Watcher watcher) throws Exception{
		manager.getZooKeeper().getChildren(path, true);
		route.put(path,watcher);
	}
	public void process(WatchedEvent event) {
		if(log.isInfoEnabled()){
			log.info("�Ѿ�������" + event.getType() + ":"+ event.getState() + "�¼���" + event.getPath());
		}
		if(event.getType() == Event.EventType.NodeChildrenChanged){
			String path = event.getPath();
			Watcher watcher = route.get(path);
			  if( watcher != null ){
				  try{
					  watcher.process(event);
				  }finally{
					  try{
						  if(manager.getZooKeeper().exists(path,null) != null){
							  manager.getZooKeeper().getChildren(path, true);
						  }
					  }catch(Exception e){
						  log.error(path +":" + e.getMessage(),e);
					  }
				  }
			  }else{
				  log.info("�Ѿ�������" + event.getType() + ":"+ event.getState() + "�¼���" + event.getPath());
			  }
		}else if(event.getState()== KeeperState.AuthFailed){
			log.info("tb_hj_schedule zk status =KeeperState.AuthFailed��");
		}else if(event.getState()== KeeperState.ConnectedReadOnly){
			log.info("tb_hj_schedule zk status =KeeperState.ConnectedReadOnly��");
		}else if(event.getState()== KeeperState.Disconnected){
			log.info("tb_hj_schedule zk status =KeeperState.Disconnected��");
			try {
				manager.reConnection();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}else if(event.getState()== KeeperState.NoSyncConnected){
			log.info("tb_hj_schedule zk status =KeeperState.NoSyncConnected���ȴ����½���ZK����.. ");
			try {
				manager.reConnection();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}else if (event.getState()== KeeperState.SaslAuthenticated){
			log.info("tb_hj_schedule zk status =KeeperState.SaslAuthenticated��");
		}else if(event.getState() == KeeperState.Unknown){
			log.info("tb_hj_schedule zk status =KeeperState.Unknown��");
		}else if (event.getState() == KeeperState.SyncConnected) {
			log.info("�յ�ZK���ӳɹ��¼���");
		} else if (event.getState() == KeeperState.Expired) {
			log.error("�Ự��ʱ���ȴ����½���ZK����...");
			try {
				manager.reConnection();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
	}
}