package com.taobao.pamirs.schedule;

public class ScheduleStrategyRunntime {
	
	/**
	 * ��������
	 */
	String taskType;
	String uuid;
	String ip;
	/**
	 * ��Ҫ����������
	 */
	int	requestNum;
	/**
	 * ��ǰ����������
	 */
	int currentNum;
	 
	String message;
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getTaskType() {
		return taskType;
	}
	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}
	public int getRequestNum() {
		return requestNum;
	}
	public void setRequestNum(int requestNum) {
		this.requestNum = requestNum;
	}
	public int getCurrentNum() {
		return currentNum;
	}
	public void setCurrentNum(int currentNum) {
		this.currentNum = currentNum;
	}

}
