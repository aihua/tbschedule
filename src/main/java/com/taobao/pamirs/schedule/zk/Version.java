package com.taobao.pamirs.schedule.zk;

public class Version {
	
   public final static String version="taobao-pamirs-schedule-3.0.0";
   
   public static String getVersion(){
	   return version;
   }
   public static boolean isCompatible(String dataVersion){
	  if(version.compareTo(dataVersion)>=0){
		  return true;
	  }else{
		  return false;
	  }
   }
   
}
