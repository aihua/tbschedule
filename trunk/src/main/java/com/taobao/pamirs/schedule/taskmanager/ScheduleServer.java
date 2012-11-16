package com.taobao.pamirs.schedule.taskmanager;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import com.taobao.pamirs.schedule.ScheduleUtil;

/**
 * ���ȷ�������Ϣ����
 * @author xuannan
 *
 */
public class ScheduleServer {
	/**
	 * ȫ��Ψһ���
	 */
	private String uuid;
	private long id;
	/**
	 * ��������
	 */
	private String taskType;

	/**
	 * ԭʼ��������
	 */
	private String baseTaskType;

	private String ownSign;
	/**
	 * ����IP��ַ
	 */
	private String ip;

	/**
	 * ��������
	 */
	private String hostName;

	/**
	 * ���ݴ����߳�����
	 */
	private int threadNum;
	/**
	 * ����ʼʱ��
	 */
	private Timestamp registerTime;
	/**
	 * ���һ������֪ͨʱ��
	 */
	private Timestamp heartBeatTime;
	/**
	 * ���һ��ȡ����ʱ��
	 */
	private Timestamp lastFetchDataTime;
	/**
	 * ����������Ϣ�������ȡ����������������ɹ�����������������ʧ�ܵ������������ʱ
	 * FetchDataCount=4430,FetcheDataNum=438570,DealDataSucess=438570,DealDataFail=0,DealSpendTime=651066
	 */
	private String dealInfoDesc;

	private String nextRunStartTime;

	private String nextRunEndTime;
	/**
	 * �������ĵĵ�ǰʱ��
	 */
	private Timestamp centerServerTime;

	/**
	 * ���ݰ汾��
	 */
	private long version;
	
	private boolean isRegister;
	
	private String managerFactoryUUID;

	public ScheduleServer() {

	}

	public static ScheduleServer createScheduleServer(IScheduleDataManager aScheduleCenter,String aBaseTaskType,
			String aOwnSign, int aThreadNum)
			throws Exception {
		ScheduleServer result = new ScheduleServer();
		result.baseTaskType = aBaseTaskType;
		result.ownSign = aOwnSign;
		result.taskType = ScheduleUtil.getTaskTypeByBaseAndOwnSign(
				aBaseTaskType, aOwnSign);
		result.ip = ScheduleUtil.getLocalIP();
		result.hostName = ScheduleUtil.getLocalHostName();
		result.registerTime = new Timestamp(aScheduleCenter.getSystemTime());
		result.threadNum = aThreadNum;
		result.heartBeatTime = null;
		result.dealInfoDesc = "���ȳ�ʼ��";
		result.version = 0;
		result.uuid = result.ip
				+ "$"
				+ (UUID.randomUUID().toString().replaceAll("-", "")
						.toUpperCase());
		SimpleDateFormat DATA_FORMAT_yyyyMMdd = new SimpleDateFormat("yyMMdd");
		String s = DATA_FORMAT_yyyyMMdd.format(new Date(aScheduleCenter.getSystemTime()));
		result.id = Long.parseLong(s) * 100000000
				+ Math.abs(result.uuid.hashCode() % 100000000);
		return result;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public int getThreadNum() {
		return threadNum;
	}

	public void setThreadNum(int threadNum) {
		this.threadNum = threadNum;
	}

	public Timestamp getRegisterTime() {
		return registerTime;
	}

	public void setRegisterTime(Timestamp registerTime) {
		this.registerTime = registerTime;
	}

	public Timestamp getHeartBeatTime() {
		return heartBeatTime;
	}

	public void setHeartBeatTime(Timestamp heartBeatTime) {
		this.heartBeatTime = heartBeatTime;
	}

	public Timestamp getLastFetchDataTime() {
		return lastFetchDataTime;
	}

	public void setLastFetchDataTime(Timestamp lastFetchDataTime) {
		this.lastFetchDataTime = lastFetchDataTime;
	}

	public String getDealInfoDesc() {
		return dealInfoDesc;
	}

	public void setDealInfoDesc(String dealInfoDesc) {
		this.dealInfoDesc = dealInfoDesc;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}


	public Timestamp getCenterServerTime() {
		return centerServerTime;
	}

	public void setCenterServerTime(Timestamp centerServerTime) {
		this.centerServerTime = centerServerTime;
	}

	public String getNextRunStartTime() {
		return nextRunStartTime;
	}

	public void setNextRunStartTime(String nextRunStartTime) {
		this.nextRunStartTime = nextRunStartTime;
	}

	public String getNextRunEndTime() {
		return nextRunEndTime;
	}

	public void setNextRunEndTime(String nextRunEndTime) {
		this.nextRunEndTime = nextRunEndTime;
	}

	public String getOwnSign() {
		return ownSign;
	}

	public void setOwnSign(String ownSign) {
		this.ownSign = ownSign;
	}

	public String getBaseTaskType() {
		return baseTaskType;
	}

	public void setBaseTaskType(String baseTaskType) {
		this.baseTaskType = baseTaskType;
	}

	public long getId() {
		return id;
	}

	public void setRegister(boolean isRegister) {
		this.isRegister = isRegister;
	}

	public boolean isRegister() {
		return isRegister;
	}

	public void setManagerFactoryUUID(String managerFactoryUUID) {
		this.managerFactoryUUID = managerFactoryUUID;
	}

	public String getManagerFactoryUUID() {
		return managerFactoryUUID;
	}

}
