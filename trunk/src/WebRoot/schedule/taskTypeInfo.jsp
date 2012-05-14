
<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleTaskItem"%>
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleServer"%>
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleTaskTypeRunningInfo"%>
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType"%>
<%@page import="java.util.List"%>
<%@ page contentType="text/html; charset=GB2312" %>
<html>
<head>
<title>
����������ϸ��Ϣ
</title>
<STYLE type=text/css>
TH{height:20px;color:#5371BA;font-weight:bold;font-size:12px;text-align:center;border:#8CB2E3 solid;border-width:0 1 1 0;background-color:#E4EFF1;white-space:nowrap;overflow:hidden;}
TD{background-color: ;border:#8CB2E3 1px solid;border-width:0 1 1 0;font-size:12px;}
table{border-collapse:collapse}
</STYLE>

</head>
<body style="font-size:12px;">

<%
	String baseTaskType =  request.getParameter("baseTaskType");
String ownSign =  request.getParameter("ownSign");
List<ScheduleTaskTypeRunningInfo> taskTypeRunningInfoList = ConsoleManager.getScheduleDataManager().getAllTaskTypeRunningInfo(baseTaskType);
if(taskTypeRunningInfoList.size() ==0){
%>
���� <%=baseTaskType%>����û����������Ϣ
<%
	}else{
%>
<table border="1" >
<%
	for(int i=0;i<taskTypeRunningInfoList.size();i++){
	if(ownSign != null && taskTypeRunningInfoList.get(i).getOwnSign().equals(ownSign)==false){
		continue;
	}
%>
<tr style="background-color:#F3F5F8;color:#013299;">
<td style="font-size:14px;font-weight:bold">
	<%=taskTypeRunningInfoList.get(i).getTaskType()%> -- <%=taskTypeRunningInfoList.get(i).getOwnSign()%>   
</td>
</tr>
<tr>
<td>
   <table border="1" style="border-COLLAPSE: collapse;display:block;">
   <tr>
   <th nowrap>���</th>
   <th>�߳�����</th>
   <th>��</th>
   <th>IP��ַ</th>
   <th>��������</th>
   <th nowrap>�߳�</th>
   <th>ע��ʱ��</th>
   <th>����ʱ��</th>
   <th>ȡ��ʱ��</th>   
   <th nowrap>�汾</th>
   <th nowrap>�´ο�ʼ</th>
   <th nowrap>�´ν���</th>
   <th>��������</th>
   <th>�������</th>
   </tr>
   <%
   	List<ScheduleServer> serverList = ConsoleManager.getScheduleDataManager().selectAllValidScheduleServer(taskTypeRunningInfoList.get(i).getTaskType());
      for(int j =0;j<serverList.size();j++){
   	   String bgColor="";
   	   ScheduleTaskType base = ConsoleManager.getScheduleDataManager().loadTaskTypeBaseInfo(serverList.get(j).getBaseTaskType());
   	   if(serverList.get(j).getCenterServerTime().getTime() - serverList.get(j).getHeartBeatTime().getTime() > base.getJudgeDeadInterval()){
   		   bgColor = "BGCOLOR='#A9A9A9'";
   	   }else if(serverList.get(j).getLastFetchDataTime() == null || serverList.get(j).getCenterServerTime().getTime() - serverList.get(j).getLastFetchDataTime().getTime() > base.getHeartBeatRate()*20){
   		   bgColor = "BGCOLOR='#FF0000'";
   	   }
   %>
	   <tr <%=bgColor%>>
	   <td><%=(j+1)%></td>
	   <td nowrap><%=serverList.get(j).getUuid()%></td>
	   <td><%=serverList.get(j).getOwnSign()%></td>	  
	   <td nowrap><%=serverList.get(j).getIp()%></td>	  
	   <td nowrap><%=serverList.get(j).getHostName()%></td>	
	   <td><%=serverList.get(j).getThreadNum()%></td>	
	   <td nowrap><%=serverList.get(j).getRegisterTime()%></td>	
	   <td nowrap><%=serverList.get(j).getHeartBeatTime()%></td>	
	   <td nowrap><%=serverList.get(j).getLastFetchDataTime()== null ?"--":serverList.get(j).getLastFetchDataTime()%></td>		   
	   <td><%=serverList.get(j).getVersion()%></td>	
	   <td nowrap><%=serverList.get(j).getNextRunStartTime() == null?"--":serverList.get(j).getNextRunStartTime()%></td>	
	   <td nowrap><%=serverList.get(j).getNextRunEndTime()==null?"--":serverList.get(j).getNextRunEndTime()%></td>
	   <td nowrap><%=serverList.get(j).getDealInfoDesc()%></td>	
	   <td nowrap><%=serverList.get(j).getManagerFactoryUUID()%></td>	
	   </tr>      
   <%
         	}
         %>
   </table> 
</td>
</tr>
<!-- ������Ϣ -->
<tr>
<td>
   <table border="1" style="border-COLLAPSE: collapse;display:block;">
   <tr>
   <th>������</th>
   <th>��ǰ�߳���</th>
   <th>�����߳���</th>
   <th>����״̬</th>
   <th>�������</th>
   <th>��������</th>
   
   </tr>
   <%
   	List<ScheduleTaskItem> taskItemList =ConsoleManager.getScheduleDataManager().loadAllTaskItem(taskTypeRunningInfoList.get(i).getTaskType());
      for(int j =0;j<taskItemList.size();j++){
   %>
	   <tr>
	   <td><%=taskItemList.get(j).getTaskItem()%></td>
	   <td><%=taskItemList.get(j).getCurrentScheduleServer()==null?"--":taskItemList.get(j).getCurrentScheduleServer()%></td>	   
	   <td><%=taskItemList.get(j).getRequestScheduleServer()==null?"--":taskItemList.get(j).getRequestScheduleServer()%></td>	   
	   <td><%=taskItemList.get(j).getSts()%></td>
	   <td><%=taskItemList.get(j).getDealParameter()==null?"":taskItemList.get(j).getDealParameter()%></td>
	   <td><%=taskItemList.get(j).getDealDesc()==null?"":taskItemList.get(j).getDealDesc()%></td>
	   </tr>      
   <%
   }
   %>
   </table> 
</td>
</tr>
<%
}
%>
</table>
<%
}
%>
</body>
</html>
