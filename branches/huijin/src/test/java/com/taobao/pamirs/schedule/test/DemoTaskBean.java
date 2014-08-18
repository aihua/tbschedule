package com.taobao.pamirs.schedule.test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.pamirs.schedule.IScheduleTaskDealSingle;
import com.taobao.pamirs.schedule.TaskItemDefine;

/**
 * ����������ʵ��
 * 
 * @author xuannan
 * 
 */
public class DemoTaskBean implements IScheduleTaskDealSingle<Long> {
	protected static transient Logger log = LoggerFactory.getLogger(DemoTaskBean.class);

	public Comparator<Long> getComparator() {
		return new Comparator<Long>() {
			public int compare(Long o1, Long o2) {
				return o1.compareTo(o2);
			}

			public boolean equals(Object obj) {
				return this == obj;
			}
		};
	}

	public List<Long> selectTasks(String taskParameter,String ownSign, int taskItemNum,
			List<TaskItemDefine> queryCondition, int fetchNum) throws Exception {
		List<Long> result = new ArrayList<Long>();
		int num = fetchNum / queryCondition.size();
		Random random = new Random(System.currentTimeMillis());
		String message = "��ȡ����...[ownSign=" + ownSign + ",taskParameter=\"" + taskParameter +"\"]:";
		boolean isFirst = true;
		for (TaskItemDefine s : queryCondition) {
			long taskItem = Integer.parseInt(s.getTaskItemId()) * 10000000L;
			for (int i = 0; i < num; i++) {
				result.add(taskItem + random.nextLong()% 100000L);
			}
			if (isFirst == false) {
				message = message + ",";				
			}else{
				isFirst = false;
			}
			message = message + s.getTaskItemId() + "{" + s.getParameter() +"}";
		}
		log.info(message);
		return result;
	}

	public boolean execute(Long task, String ownSign) throws Exception {
		Thread.sleep(50);
		log.info("��������["+ownSign+"]:" + task);
		return true;
	}
}