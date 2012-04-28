package com.taobao.pamirs.schedule;

import org.apache.commons.lang.builder.ToStringBuilder;

public class ScheduleStrategy {
	/**
	 * 任务类型
	 */
	private String taskType;

	private String[] IPList;

	private int numOfSingleServer;
	/**
	 * 指定需要执行调度的机器数量
	 */
	private int assignNum;
	
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}
	
	public int getAssignNum() {
		return assignNum;
	}

	public void setAssignNum(int assignNum) {
		this.assignNum = assignNum;
	}

	public String[] getIPList() {
		return IPList;
	}

	public void setIPList(String[] iPList) {
		IPList = iPList;
	}

	public void setNumOfSingleServer(int numOfSingleServer) {
		this.numOfSingleServer = numOfSingleServer;
	}

	public int getNumOfSingleServer() {
		return numOfSingleServer;
	}
}
