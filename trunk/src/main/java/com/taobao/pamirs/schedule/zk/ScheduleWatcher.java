package com.taobao.pamirs.schedule.zk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class ScheduleWatcher implements Watcher {
	private static transient Log log = LogFactory.getLog(ScheduleWatcher.class);
	
	public void process(WatchedEvent event) {
		log.info("已经触发了" + event.getType() + "事件！" + event.getPath());
		try {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}