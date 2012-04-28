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
		return "停止服务器成功";
	}
	public String pauseSchedule() throws Exception {
		this.tbScheduleManager.pause("管理员通过外部控制pause调度服务器");
		return "暂停服务器成功";
	}	
	public String resumeSchedule() throws Exception {
		this.tbScheduleManager.resume("管理员通过外部控制resume调度服务器");
		return "恢复服务器成功";
	}
	protected void buildDynamicMBeanInfo() {
		MBeanAttributeInfo[] dAttributes = new MBeanAttributeInfo[] {
				new MBeanAttributeInfo("uuid", "java.lang.String", "唯一标识",
						true, false, false),
				new MBeanAttributeInfo("name", "java.lang.String", "调度名称",
						true, false, false),
				new MBeanAttributeInfo("taskType", "java.lang.String", "任务类型",
						true, false, false),
				new MBeanAttributeInfo("ip", "java.lang.String", "IP地址", true,
						false, false),
				new MBeanAttributeInfo("hostName", "java.lang.String", "主机名称",
						true, false, false),
				new MBeanAttributeInfo("registerTime", "java.util.Date",
						"服务器启动时间", true, false, false),
				new MBeanAttributeInfo("heartBeatTime", "java.util.Date",
						"最近一次心跳时间", true, false, false),
				new MBeanAttributeInfo("threadNum", "int", "处理线程数量", true,
						false, false),
				new MBeanAttributeInfo("managerPort", "int", "管理端口", true,
						false, false),
				new MBeanAttributeInfo("dealInfoDesc", "String", "处理信息", true,
						false, false) };

		MBeanOperationInfo[] dOperations = new MBeanOperationInfo[] { 
				new MBeanOperationInfo("stopSchedule", "停止调度服务", new MBeanParameterInfo[] {},"String", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("pauseSchedule", "暂停调度服务", new MBeanParameterInfo[] {},"String", MBeanOperationInfo.ACTION),
				new MBeanOperationInfo("resumeSchedule", "恢复调度服务", new MBeanParameterInfo[] {},"String", MBeanOperationInfo.ACTION)
				};
		dMBeanInfo = new MBeanInfo(this.getClass().getName(), "TaskItemDefine",
				dAttributes, null, dOperations, null);
	}
}
