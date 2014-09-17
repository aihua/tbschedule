package com.taobao.pamirs.schedule.test;

import com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType;

import junit.framework.TestCase;

public class ScheduleTaskTypeTest  extends TestCase  {

	String s=":1,2 4 5}:a,b,c}";

	public void testSplit(){
		String[]  list=ScheduleTaskType.splitTaskItem(s);
		System.out.println(list.toString());
	}
}
