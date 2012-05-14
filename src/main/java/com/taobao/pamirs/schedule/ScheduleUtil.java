package com.taobao.pamirs.schedule;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 调度处理工具类
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

	public static long getCurrentTimeMillis() {
		//return TBSysdateManager.getCurrentTimeMillis();
		return System.currentTimeMillis();
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
}
