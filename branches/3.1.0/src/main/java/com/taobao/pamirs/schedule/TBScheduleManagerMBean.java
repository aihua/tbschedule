package com.taobao.pamirs.schedule;


import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;


public class TBScheduleManagerMBean extends AbstractDynamicMBean {
	private TBScheduleManager tbScheduleManager;
    public TBScheduleManagerMBean(TBScheduleManager aTBScheduleManager){
    	this.tbScheduleManager = aTBScheduleManager;
    }
	public String getName() {
		return tbScheduleManager.getScheduleServer().getIp() +"$"+ tbScheduleManager.getScheduleServer().getManagerPort()
		+"$"+	tbScheduleManager.getScheduleServer().getTaskType() + "$"
				+ tbScheduleManager.getCurrentSerialNumber();
	}
	public String getTaskType() {
		return tbScheduleManager.getScheduleServer().getTaskType();
	}
	public String getDealInfoDesc() {
		return tbScheduleManager.getScheduleServer().getDealInfoDesc();
	}
	public String getUuid() {
		return tbScheduleManager.getScheduleServer().getUuid();
	}
	public String getIp(){
		return this.tbScheduleManager.getScheduleServer().getIp();
	}
	public String getHostName(){
		return this.tbScheduleManager.getScheduleServer().getHostName();
	}
	public java.util.Date getRegisterTime() {
		return this.tbScheduleManager.getScheduleServer().getRegisterTime();
	}
	public java.util.Date getHeartBeatTime() {
		return this.tbScheduleManager.getScheduleServer().getHeartBeatTime();
	}
	public int getThreadNum() {
		return this.tbScheduleManager.getTaskTypeInfo().getThreadNumber();
	}
	
	public int getManagerPort() {
		return this.tbScheduleManager.getScheduleServer().getManagerPort();
	}
	
	public String stopSchedule() throws Exception {
		this.tbScheduleManager.stopScheduleServer();
		return "ֹͣ�������ɹ�";
	}
	public String pauseSchedule() throws Exception {
		this.tbScheduleManager.pause("����Աͨ���ⲿ����pause���ȷ�����");
		return "��ͣ�������ɹ�";
	}	
	public String resumeSchedule() throws Exception {
		this.tbScheduleManager.resume("����Աͨ���ⲿ����resume���ȷ�����");
		return "�ָ��������ɹ�";
	}
	protected void buildDynamicMBeanInfo() {
		MBeanAttributeInfo[] dAttributes = new MBeanAttributeInfo[] {
				new MBeanAttributeInfo("uuid", "java.lang.String", "Ψһ��ʶ",
						true, false, false),
				new MBeanAttributeInfo("name", "java.lang.String", "��������",
						true, false, false),
				new MBeanAttributeInfo("taskType", "java.lang.String", "��������",
						true, false, false),
				new MBeanAttributeInfo("ip", "java.lang.String", "IP��ַ", true,
						false, false),
				new MBeanAttributeInfo("hostName", "java.lang.String", "��������",
						true, false, false),
				new MBeanAttributeInfo("registerTime", "java.util.Date",
						"����������ʱ��", true, false, false),
				new MBeanAttributeInfo("heartBeatTime", "java.util.Date",
						"���һ������ʱ��", true, false, false),
				new MBeanAttributeInfo("threadNum", "int", "�����߳�����", true,
						false, false),
				new MBeanAttributeInfo("managerPort", "int", "����˿�", true,
						false, false),
				new MBeanAttributeInfo("dealInfoDesc", "String", "������Ϣ", true,
						false, false) };

		MBeanOperationInfo[] dOperations = new MBeanOperationInfo[] { 
				new MBeanOperationInfo("stopSchedule", "ֹͣ���ȷ���", new MBeanParameterInfo[] {},"String", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("pauseSchedule", "��ͣ���ȷ���", new MBeanParameterInfo[] {},"String", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("resumeSchedule", "�ָ����ȷ���", new MBeanParameterInfo[] {},"String", MBeanOperationInfo.ACTION)
				};
		dMBeanInfo = new MBeanInfo(this.getClass().getName(), "TaskItemDefine",
				dAttributes, null, dOperations, null);
	}
}
