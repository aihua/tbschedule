<%@page import="com.taobao.pamirs.schedule.ScheduleStrategy"%>
<%@page import="com.taobao.pamirs.schedule.TBScheduleManager"%>
<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="java.util.List"%>
<%@ page contentType="text/html; charset=GB2312" %>
<%
    String isManager= request.getParameter("manager");
%>
<html>
<head>
<title>
调度策略管理
</title>
<STYLE type=text/css>

TH{height:20px;color:#5371BA;font-weight:bold;font-size:12px;text-align:center;border:#8CB2E3 solid;border-width:0 1 1 0;background-color:#E4EFF1;white-space:nowrap;overflow:hidden;}
TD{background-color: ;border:#8CB2E3 1px solid;border-width:0 1 1 0;font-size:12px;}
table{border-collapse:collapse}

</STYLE>

</head>
<body style="font-size:12px;">

<table id="contentTable" border="1">
     <tr>
     	<th>序号</th>
     	<%if("true".equals(isManager)){%>
     	<th>管理</th>
		<%}%>
     	<th>任务类型</th>
     	<th>单JVM最大线程组数量</th>
    	<th>最大线程组数量</th>
     	<th>IP地址(逗号分隔)</th>
     </tr>
<%
List<ScheduleStrategy> scheduleStrategyList =  ConsoleManager.getScheduleStrategyManager().loadAllScheduleStrategy();
String ipIds ="";
for(int i=0;i<scheduleStrategyList.size();i++){
	ScheduleStrategy scheduleStrategy = scheduleStrategyList.get(i);
	String[] ipList =scheduleStrategy.getIPList();
	ipIds ="";
	for(int j=0;ipList!=null&& j<ipList.length;j++){
		if(j>0){
			ipIds = ipIds+ ",";
		}
		ipIds = ipIds + ipList[j];
	}
   String baseTaskType = TBScheduleManager.splitBaseTaskTypeFromTaskType(scheduleStrategy.getTaskType());
   String ownSign = TBScheduleManager.splitOwnsignFromTaskType(scheduleStrategy.getTaskType());   

%>
     <tr onclick="openDetail(this,'<%=baseTaskType%>','<%=ownSign%>','<%=scheduleStrategy.getTaskType()%>')">
     	<td><%=(i+1)%></td>
     	<%if("true".equals(isManager)){%>
     	<td width="100" align="center">
     	    <a target="scheduleStrategyDetail" href="scheduleStrategyEdit.jsp?taskType=<%=scheduleStrategy.getTaskType()%>" style="color:#0000CD">编辑</a>
     	    <a target="scheduleStrategyDetail" href="scheduleStrategyDeal.jsp?action=deleteScheduleStrategy&taskType=<%=scheduleStrategy.getTaskType()%>" style="color:#0000CD">删除</a>
     	</td>
		<%}%>
     	<td><%=scheduleStrategy.getTaskType()%></td>
     	<td align="center"><%=scheduleStrategy.getNumOfSingleServer()%></td>
     	<td align="center"><%=scheduleStrategy.getAssignNum()%></td>
		<td><%=ipIds%></td>
     </tr>
<%
}
%>
</table>
<br/>
<%if("true".equals(isManager)){%>
<a target="scheduleStrategyDetail" href="scheduleStrategyEdit.jsp?taskType=-1" style="color:#0000CD">创建新策略...</a>
<%}%>
任务运行情况：
<iframe  name="scheduleStrategyDetail" height="250" width="100%"></iframe>
任务在各个机器上的分配情况：
<iframe  name="scheduleStrategyRuntime" height="120" width="100%"></iframe>
</body>
</html>
<script>

var oldSelectRow = null;
function openDetail(obj,baseTaskType,ownSign,taskType){
	if(oldSelectRow != null){
		oldSelectRow.bgColor="";
	}
	obj.bgColor="#FFD700";
	oldSelectRow = obj;
    document.all("scheduleStrategyDetail").src = "taskTypeInfo.jsp?baseTaskType=" + baseTaskType +"&ownSign=" + ownSign;
    document.all("scheduleStrategyRuntime").src = "scheduleStrategyRuntime.jsp?taskType=" + taskType;
}
if(contentTable.rows.length >1){
	contentTable.rows[1].click();
}

</script>