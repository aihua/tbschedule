<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType"%>
<%@page import="java.util.List"%>
<%@ page contentType="text/html; charset=GB2312" %>
<%
	if(ConsoleManager.isInitial() == false){
		response.sendRedirect("config.jsp");
	}
%>

<%
	String isManager = request.getParameter("manager");
%>
<html>
<head>
<title>
Schedule���ȹ���
</title>
<STYLE type=text/css>

TH{height:20px;color:#5371BA;font-weight:bold;font-size:12px;text-align:center;border:#8CB2E3 solid;border-width:0 1 1 0;background-color:#E4EFF1;}
TD{background-color: ;border:#8CB2E3 1px solid;border-width:0 1 1 0;font-size:12px;}
table{border-collapse:collapse}
</STYLE>

</head>
<body style="font-size:12px;">
<table id="list" border="1" >
<thead>
     <tr>
     	<th>���</th>
     	<%
     		if ("true".equals(isManager)) {
     	%>
     	<th >����</th>
		<%
			}
		%>
     	<th>��������</th>
     	<th>������Bean</th>
     	<th>����Ƶ��(��)</th>
     	<th>�������(��)</th>
     	<th>�߳���</th>
     	<th>ÿ�λ�ȡ������</th>
     	<th>ÿ��ִ������</th>
     	<th>û������ʱ����ʱ��(��)</th>
     	<th>����ģʽ</th>
     	<th>ÿ�δ��������ݺ�����ʱ��(��)</th>
    	<th>�����������Ϣʱ��(Сʱ)</th>
     	<th>ִ�п�ʼʱ��</th>
     	<th>ִ�н���ʱ��</th>
     	<th>���߳������������</th>
     	<th>�Զ������</th>
     	<th>������</th>
     </tr>
     </thead>
     <tbody>
<%
	List<ScheduleTaskType> taskTypes = ConsoleManager.getScheduleDataManager().getAllTaskTypeBaseInfo();
	String taskItems = "";
	for (int i = 0; i < taskTypes.size(); i++) {
		taskItems = "";
		String[] strs = taskTypes.get(i).getTaskItems() ;
		if (strs != null) {
			for (int j = 0; j < strs.length; j++) {
				if (j > 0) {
					taskItems = taskItems + ",";
				}
				taskItems = taskItems + strs[j];
			}
		}
%>
     <tr onclick="openDetail(this,'<%=taskTypes.get(i).getBaseTaskType()%>')">
     	<td><%=(i + 1)%></td>
     	<%
     		if ("true".equals(isManager)) {
     	%>
     	<td width="120" align="center">
     		<a target="taskDetail" href="taskTypeEdit.jsp?taskType=<%=taskTypes.get(i).getBaseTaskType()%>"  style="color:#0000CD">�༭</a>
     		<a target="taskDetail" href="taskTypeDeal.jsp?action=clearTaskType&taskType=<%=taskTypes.get(i).getBaseTaskType()%>"  style="color:#0000CD">����</a>
     		<a target="taskDetail" href="javascript:void(0)" onclick="validateDel('<%=taskTypes.get(i).getBaseTaskType()%>')" style="color:#0000CD">ɾ��</a>
     	</td>
		<%
			}
		%>
     	<td><%=taskTypes.get(i).getBaseTaskType()%></td>
     	<td><%=taskTypes.get(i).getDealBeanName()%></td>
     	<td><%=taskTypes.get(i).getHeartBeatRate() / 1000.0%></td>
     	<td><%=taskTypes.get(i).getJudgeDeadInterval() / 1000.0%></td>
     	<td><%=taskTypes.get(i).getThreadNumber()%></td>
     	<td><%=taskTypes.get(i).getFetchDataNumber()%></td>
     	<td><%=taskTypes.get(i).getExecuteNumber()%></td>
     	<td><%=taskTypes.get(i).getSleepTimeNoData() == 0 ? "--"
						: taskTypes.get(i).getSleepTimeNoData() / 1000.0%></td>
     	<td><%=taskTypes.get(i).getProcessorType()%></td>
     	<td><%=taskTypes.get(i).getSleepTimeInterval() == 0 ? "--"
						: taskTypes.get(i).getSleepTimeInterval() / 1000.0%></td>
    	<td><%=taskTypes.get(i).getExpireOwnSignInterval()%></td>
     	<td><%=taskTypes.get(i).getPermitRunStartTime() == null ? "--"
						: taskTypes.get(i).getPermitRunStartTime()%></td>
     	<td><%=taskTypes.get(i).getPermitRunEndTime() == null ? "--"
						: taskTypes.get(i).getPermitRunEndTime()%></td>   
						
    	<td><%=taskTypes.get(i).getMaxTaskItemsOfOneThreadGroup()%></td>   

		<td><%=taskTypes.get(i).getTaskParameter() == null ? "--"
						: taskTypes.get(i).getTaskParameter()%></td>   
		<td><%=taskItems%></td>
     </tr>
<%
	}
%>
</tbody>
</table>
<br/>

<%
	if ("true".equals(isManager)) {
%>
<a target="taskDetail" href="taskTypeEdit.jsp?taskType=-1"  style="color:#0000CD">����������...</a>
<%
	}
%>
��������Ϣ��<br/>
<iframe id="showTaskDetail" name="taskDetail"  height="80%" width="100%"></iframe>
</body>
</html>
<script>
var oldSelectRow = null;
function openDetail(obj,baseTaskType){
	if(oldSelectRow != null){
		oldSelectRow.bgColor="";
	}
	obj.bgColor="#FFD700";
	oldSelectRow = obj;
	document.getElementById("showTaskDetail").src = "taskTypeInfo.jsp?baseTaskType=" + baseTaskType;
}

if(list.rows.length >1){
	list.rows[1].click();
}

function deleteTaskType(baseTaskType){
	//return window.confirm("��ȷ�����еĵ��������Ѿ�ֹͣ������ᵼ�µ������쳣��");
		
}

function validateDel(str) {
    var flag = window.confirm("ȷ��ɾ������"+str+"?");
    if(flag) {
        window.location.href="taskTypeDeal.jsp?action=deleteTaskType&taskType="+str; 
    }
}
</script>