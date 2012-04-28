<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="com.taobao.pamirs.schedule.ScheduleStrategy"%>
<%@ page contentType="text/html; charset=GB2312" %>
<html>
<head>
<title>
创建调度任务
</title>
</head>
<body bgcolor="#ffffff">

<%
	String action = (String)request.getParameter("action");
	String result = "";
	boolean isRefreshParent = false;
	ScheduleStrategy scheduleStrategy = new ScheduleStrategy();
	scheduleStrategy.setTaskType(request.getParameter("taskType"));
	scheduleStrategy.setNumOfSingleServer(request.getParameter("numOfSingleServer")==null?0: Integer.parseInt(request.getParameter("numOfSingleServer")));
	scheduleStrategy.setAssignNum(request.getParameter("assignNum")==null?0: Integer.parseInt(request.getParameter("assignNum")));
	if(request.getParameter("ips") == null){
		scheduleStrategy.setIPList(new String[0]);
	}else{
	   scheduleStrategy.setIPList(request.getParameter("ips").split(","));
	}
	try {
 		if (action.equalsIgnoreCase("createScheduleStrategy")) {
 			ConsoleManager.getScheduleStrategyManager().createScheduleStrategy(scheduleStrategy);
			isRefreshParent = true;
		} else if (action.equalsIgnoreCase("editScheduleStrategy")) {
			ConsoleManager.getScheduleStrategyManager().updateScheduleStrategy(scheduleStrategy);
			isRefreshParent = true;
		} else if (action.equalsIgnoreCase("deleteScheduleStrategy")) {
			ConsoleManager.getScheduleStrategyManager().deleteMachineStrategy(scheduleStrategy.getTaskType());
			isRefreshParent = true;
		}else{
			throw new Exception("不支持的操作：" + action);
		}
	} catch (Throwable e) {
		e.printStackTrace();
		result ="ERROR:" + e.getMessage(); 
		isRefreshParent = false;
	}
%>
<%=result%>
</body>
</html>
<% if(isRefreshParent == true){ %>
<script>
 parent.location.reload();
</script>
<%}%>