package com.taobao.pamirs.schedule;

/**
 * �����壬�ṩ�ؼ���Ϣ��ʹ����
 * @author xuannan
 *
 */
public class TaskItemDefine {
	/**
	 * ������ID
	 */
	private String taskItemId;
	/**
	 * �������Զ������
	 */
	private String parameter;
	
	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	public String getParameter() {
		return parameter;
	}
	public void setTaskItemId(String taskItemId) {
		this.taskItemId = taskItemId;
	}
	public String getTaskItemId() {
		return taskItemId;
	}
	@Override
	public String toString() {
		return "(t=" + taskItemId + ",p="
				+ parameter + ")";
	}
	

}
