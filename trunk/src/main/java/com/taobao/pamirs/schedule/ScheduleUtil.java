package com.taobao.pamirs.schedule;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * ���ȴ�������
 * @author xuannan
 *
 */
public class ScheduleUtil {
	public static String OWN_SIGN_BASE ="BASE";

	public static String getLocalHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			return "";
		}
	}

	public static int getFreeSocketPort() {
		try {
			ServerSocket ss = new ServerSocket(0);
			int freePort = ss.getLocalPort();
			ss.close();
			return freePort;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String getLocalIP() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			return "";
		}
	}

	public static String transferDataToString(Date d){
		SimpleDateFormat DATA_FORMAT_yyyyMMddHHmmss = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return DATA_FORMAT_yyyyMMddHHmmss.format(d);
	}
	public static Date transferStringToDate(String d) throws ParseException{
		SimpleDateFormat DATA_FORMAT_yyyyMMddHHmmss = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return DATA_FORMAT_yyyyMMddHHmmss.parse(d);
	}
	public static Date transferStringToDate(String d,String formate) throws ParseException{
		SimpleDateFormat FORMAT = new SimpleDateFormat(formate);
        return FORMAT.parse(d);
	}
	public static String getTaskTypeByBaseAndOwnSign(String baseType,String ownSign){
		if(ownSign.equals(OWN_SIGN_BASE) == true){
			return baseType;
		}
		return baseType+"$" + ownSign;
	}
	public static String splitBaseTaskTypeFromTaskType(String taskType){
		 if(taskType.indexOf("$") >=0){
			 return taskType.substring(0,taskType.indexOf("$"));
		 }else{
			 return taskType;
		 }
		 
	}
	public static String splitOwnsignFromTaskType(String taskType){
		 if(taskType.indexOf("$") >=0){
			 return taskType.substring(taskType.indexOf("$")+1);
		 }else{
			 return OWN_SIGN_BASE;
		 }
	}	
	
	/**
	 * ������������
	 * @param serverNum �ܵķ���������
	 * @param taskItemNum ����������
	 * @param maxNumOfOneServer ÿ��server�����������Ŀ
	 * @param maxNum �ܵ���������
	 * @return
	 */
	public static int[] assignTaskNumber(int serverNum,int taskItemNum,int maxNumOfOneServer){
		int[] taskNums = new int[serverNum];
		int numOfSingle = taskItemNum / serverNum;
		int otherNum = taskItemNum % serverNum;
		//20150323 ɾ��, �����Ƭ��֤���䵽���е��߳������ϡ� ��ʼ
//		if (maxNumOfOneServer >0 && numOfSingle >= maxNumOfOneServer) {
//			numOfSingle = maxNumOfOneServer;
//			otherNum = 0;
//		}
		//20150323 ɾ��, �����Ƭ��֤���䵽���е��߳������ϡ� ����
		for (int i = 0; i < taskNums.length; i++) {
			if (i < otherNum) {
				taskNums[i] = numOfSingle + 1;
			} else {
				taskNums[i] = numOfSingle;
			}
		}
		return taskNums;
	}
	private static String printArray(int[] items){
		String s="";
		for(int i=0;i<items.length;i++){
			if(i >0){s = s +",";}
			s = s + items[i];
		}
		return s;
	}
	public static void main(String[] args) {
		System.out.println(printArray(assignTaskNumber(1,10,0)));
		System.out.println(printArray(assignTaskNumber(2,10,0)));
		System.out.println(printArray(assignTaskNumber(3,10,0)));
		System.out.println(printArray(assignTaskNumber(4,10,0)));
		System.out.println(printArray(assignTaskNumber(5,10,0)));
		System.out.println(printArray(assignTaskNumber(6,10,0)));
		System.out.println(printArray(assignTaskNumber(7,10,0)));
		System.out.println(printArray(assignTaskNumber(8,10,0)));		
		System.out.println(printArray(assignTaskNumber(9,10,0)));
		System.out.println(printArray(assignTaskNumber(10,10,0)));
		
		System.out.println("-----------------");
		
		System.out.println(printArray(assignTaskNumber(1,10,3)));
		System.out.println(printArray(assignTaskNumber(2,10,3)));
		System.out.println(printArray(assignTaskNumber(3,10,3)));
		System.out.println(printArray(assignTaskNumber(4,10,3)));
		System.out.println(printArray(assignTaskNumber(5,10,3)));
		System.out.println(printArray(assignTaskNumber(6,10,3)));
		System.out.println(printArray(assignTaskNumber(7,10,3)));
		System.out.println(printArray(assignTaskNumber(8,10,3)));		
		System.out.println(printArray(assignTaskNumber(9,10,3)));
		System.out.println(printArray(assignTaskNumber(10,10,3)));
		
	}
}
