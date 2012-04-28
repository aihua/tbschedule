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
			return "�������ȷ�����" + num + " ��ɹ���TASK_TYPE=" + taskType;
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
			return "�����ɹ�";
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
				new MBeanOperationInfo("getTaskTypeList", "��ȡ���е���������", new MBeanParameterInfo[] {},
						"String[]", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("getScheduleTaskDealList", "��ȡ���д��ڵĵ�����������", new MBeanParameterInfo[] {},
						"String[]", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("createTaskType", "�������ȷ���",
						new MBeanParameterInfo[] {
								new MBeanParameterInfo("baseTaskType",
										"java.lang.String", "��������"),
								new MBeanParameterInfo("dealBeanName",
												"java.lang.String", "������Bean"),
								new MBeanParameterInfo("taskItems",
										"java.lang.String",
										"�������ID,�Զ��ŷָ�")},
						"String", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("createScheduleManager", "�������ȷ���", new MBeanParameterInfo[] {
						new MBeanParameterInfo("taskType", "java.lang.String", "��������"),
						new MBeanParameterInfo("dealBeanName", "java.lang.String", "����bean��ʶ"),
						new MBeanParameterInfo("ownSign", "java.lang.String", "����,����:BASE,DAILY,PRE..."),
						new MBeanParameterInfo("num", "int", "����")}, "String",
						MBeanOperationInfo.ACTION), };
		dMBeanInfo = new MBeanInfo(this.getClass().getName(), "TaskItemDefine", dAttributes, null, dOperations, null);
	}
}
