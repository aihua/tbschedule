package com.taobao.pamirs.schedule;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TBScheduleManagerDynamic extends TBScheduleManager {
	private static transient Log log = LogFactory.getLog(TBScheduleManagerDynamic.class);
	
	TBScheduleManagerDynamic(TBScheduleManagerFactory aFactory,
			String baseTaskType, String ownSign, int managerPort,
			String jxmUrl, IScheduleDataManager aScheduleCenter,
			IScheduleTaskDeal<?> aTaskDealBean) throws Exception {
		super(aFactory, baseTaskType, ownSign, managerPort, jxmUrl, aScheduleCenter,
				aTaskDealBean);
	}
	public void initial() throws Exception{
		if (scheduleCenter.isLeader(this.currenScheduleServer.getUuid(),
				scheduleCenter.loadScheduleServerNames(this.currenScheduleServer.getTaskType()))) {
			// 是第一次启动，检查对应的zk目录是否存在
			this.scheduleCenter.initialRunningInfo4Dynamic(	this.currenScheduleServer.getBaseTaskType(),
					this.currenScheduleServer.getOwnSign());
		}
		computerStart();
    }
	
	public void refreshScheduleServerInfo() throws Exception {
		throw new Exception("没有实现");
	}

	public boolean isNeedReLoadTaskItemList() throws Exception {
		throw new Exception("没有实现");
	}
	public void assignScheduleTask() throws Exception {
		throw new Exception("没有实现");
		
	}
	public List<TaskItemDefine> getCurrentScheduleTaskItemList() {
		throw new RuntimeException("没有实现");
	}
	public int getTaskItemCount() {
		throw new RuntimeException("没有实现");
	}
}
