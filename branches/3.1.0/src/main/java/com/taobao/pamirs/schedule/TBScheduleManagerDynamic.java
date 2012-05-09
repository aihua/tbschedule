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
			// �ǵ�һ������������Ӧ��zkĿ¼�Ƿ����
			this.scheduleCenter.initialRunningInfo4Dynamic(	this.currenScheduleServer.getBaseTaskType(),
					this.currenScheduleServer.getOwnSign());
		}
		computerStart();
    }
	
	public void refreshScheduleServerInfo() throws Exception {
		throw new Exception("û��ʵ��");
	}

	public boolean isNeedReLoadTaskItemList() throws Exception {
		throw new Exception("û��ʵ��");
	}
	public void assignScheduleTask() throws Exception {
		throw new Exception("û��ʵ��");
		
	}
	public List<TaskItemDefine> getCurrentScheduleTaskItemList() {
		throw new RuntimeException("û��ʵ��");
	}
	public int getTaskItemCount() {
		throw new RuntimeException("û��ʵ��");
	}
}
