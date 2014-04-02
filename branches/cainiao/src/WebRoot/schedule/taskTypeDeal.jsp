<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType"%>
<%@page import="java.util.List"%>
<%@ page contentType="text/html; charset=GB2312" %>
<html>
<head>
<title>
������������
</title>
</head>
<body bgcolor="#ffffff">

<%
	String action = (String)request.getParameter("action");
	String result = "";
	boolean isRefreshParent = false;
	String baseTaskType = (String)request.getParameter("taskType");
	try {
		if (action.equalsIgnoreCase("createTaskType")||action.equalsIgnoreCase("editTaskType")) {
			isRefreshParent = false;
			ScheduleTaskType taskType = new ScheduleTaskType();
			taskType.setBaseTaskType(baseTaskType);
			taskType.setDealBeanName(request.getParameter("dealBean"));
			taskType.setHeartBeatRate(request.getParameter("heartBeatRate")==null?0: ((int)Double.parseDouble(request.getParameter("heartBeatRate"))*1000));
			taskType.setJudgeDeadInterval(request.getParameter("judgeDeadInterval")==null?0: ((int)Double.parseDouble(request.getParameter("judgeDeadInterval"))*1000));
			taskType.setThreadNumber(request.getParameter("threadNumber")==null?0: Integer.parseInt(request.getParameter("threadNumber")));
			taskType.setFetchDataNumber(request.getParameter("fetchNumber")==null?0: Integer.parseInt(request.getParameter("fetchNumber")));
			taskType.setExecuteNumber(request.getParameter("executeNumber")==null?0: Integer.parseInt(request.getParameter("executeNumber")));
			taskType.setSleepTimeNoData(request.getParameter("sleepTimeNoData")==null?0: (int)(Double.parseDouble(request.getParameter("sleepTimeNoData"))*1000));
			taskType.setSleepTimeInterval(request.getParameter("sleepTimeInterval")==null?0: ((int)Double.parseDouble(request.getParameter("sleepTimeInterval"))*1000));
			taskType.setProcessorType(request.getParameter("processType"));
			//taskType.setExpireOwnSignInterval(request.getParameter("expireOwnSignInterval")==null?0: Integer.parseInt(request.getParameter("threadNumber")));
			taskType.setPermitRunStartTime(request.getParameter("permitRunStartTime"));
			taskType.setPermitRunEndTime(request.getParameter("permitRunEndTime"));
			String s= request.getParameter("maxTaskItemsOfOneThreadGroup");
			taskType.setMaxTaskItemsOfOneThreadGroup(s==null || s.trim().length() ==0?0:Integer.parseInt(request.getParameter("maxTaskItemsOfOneThreadGroup")));		
			taskType.setTaskParameter(request.getParameter("taskParameter"));
			
			String itemDefines  =request.getParameter("taskItems");
			itemDefines = itemDefines.replaceAll("\r","");
			itemDefines = itemDefines.replaceAll("\n","");			
			taskType.setTaskItems(ScheduleTaskType.splitTaskItem(itemDefines));
			taskType.setSts(request.getParameter("sts"));
			if(action.equalsIgnoreCase("createTaskType")){
				ConsoleManager.getScheduleDataManager().createBaseTaskType(taskType);
				result = "����" + baseTaskType + "�����ɹ���������";
			}else{
				ConsoleManager.getScheduleDataManager().updateBaseTaskType(taskType);
				result = "����" + baseTaskType + "�޸ĳɹ���������";			
			}
			isRefreshParent = true;
			
		} else if (action.equalsIgnoreCase("clearTaskType")) {
			ConsoleManager.getScheduleDataManager().clearTaskType(
					baseTaskType);
			result = "����" + baseTaskType + "��������Ϣ����ɹ���������";
			isRefreshParent = false;
		} else if (action.equalsIgnoreCase("deleteTaskType")) {
			ConsoleManager.getScheduleDataManager().deleteTaskType(
					baseTaskType);
			result = "����" + baseTaskType + "ɾ���ɹ���������";
			isRefreshParent = true;
		} else if (action.equalsIgnoreCase("pauseTaskType")) {
			ConsoleManager.getScheduleDataManager().pauseAllServer(baseTaskType);
			isRefreshParent = true;
		} else if (action.equalsIgnoreCase("resumeTaskType")) {
			ConsoleManager.getScheduleDataManager().resumeAllServer(baseTaskType);
			isRefreshParent = true;
		}else{
			throw new Exception("��֧�ֵĲ�����" + action);
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