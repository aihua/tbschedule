package com.taobao.pamirs.schedule;

import java.util.List;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

public class TBScheduleManagerFactoryMBean extends AbstractDynamicMBean {
	TBScheduleManagerFactory tbScheduleManagerFactory;
	
	public TBScheduleManagerFactoryMBean(TBScheduleManagerFactory factory){
		this.tbScheduleManagerFactory=factory;		
	}
	public String createScheduleManager(String taskType,String dealBeanName,String ownSign,int num) {
		try {
			for (int i = 0; i < num; i++) {
				tbScheduleManagerFactory.createTBScheduleManager(taskType,
						dealBeanName, ownSign);
			}
			return "创建调度服务器" + num + " 组成功：TASK_TYPE=" + taskType;
		} catch (Exception e) {
			return e.toString();
		}
	}

	public String[] getScheduleTaskDealList() {
		return tbScheduleManagerFactory.getScheduleTaskDealList();
	}
	public String createTaskType(String baseTaskType,String dealBeanName,String taskItems) {
		ScheduleTaskType taskType = new ScheduleTaskType();	
		taskType.setBaseTaskType(baseTaskType);
		taskType.setDealBeanName(dealBeanName);
		taskType.setTaskItems(taskItems.split(","));
		try {
			tbScheduleManagerFactory.getScheduleDataManager().createBaseTaskType(taskType);
			return "创建成功";
		} catch (Exception e) {
			return e.toString();
		}
		
	}
	public String[] getTaskTypeList() throws Exception{
		List<ScheduleTaskType> taskTypes = tbScheduleManagerFactory.getScheduleDataManager().getAllTaskTypeBaseInfo();
		String[] result = new String[taskTypes.size()];
		for(int i=0;i<result.length;i++){
			result[i] = taskTypes.get(i).getBaseTaskType();
		}
		return result;
	}

	protected void buildDynamicMBeanInfo() {
		MBeanAttributeInfo[] dAttributes = new MBeanAttributeInfo[] {};
		MBeanOperationInfo[] dOperations = new MBeanOperationInfo[] {
				new MBeanOperationInfo("getTaskTypeList", "获取所有的任务类型", new MBeanParameterInfo[] {},
						"String[]", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("getScheduleTaskDealList", "获取所有存在的调度任务处理器", new MBeanParameterInfo[] {},
						"String[]", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("createTaskType", "创建调度服务",
						new MBeanParameterInfo[] {
								new MBeanParameterInfo("baseTaskType",
										"java.lang.String", "任务类型"),
								new MBeanParameterInfo("dealBeanName",
												"java.lang.String", "任务处理Bean"),
								new MBeanParameterInfo("taskItems",
										"java.lang.String",
										"任务队列ID,以逗号分隔")},
						"String", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("createScheduleManager", "创建调度服务", new MBeanParameterInfo[] {
						new MBeanParameterInfo("taskType", "java.lang.String", "任务类型"),
						new MBeanParameterInfo("dealBeanName", "java.lang.String", "任务bean标识"),
						new MBeanParameterInfo("ownSign", "java.lang.String", "环境,例如:BASE,DAILY,PRE..."),
						new MBeanParameterInfo("num", "int", "数量")}, "String",
						MBeanOperationInfo.ACTION), };
		dMBeanInfo = new MBeanInfo(this.getClass().getName(), "TaskItemDefine", dAttributes, null, dOperations, null);
	}
}
