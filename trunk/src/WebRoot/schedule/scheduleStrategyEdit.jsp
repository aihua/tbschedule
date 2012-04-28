<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="com.taobao.pamirs.schedule.ScheduleStrategy"%>
<%@ page contentType="text/html; charset=GB2312" %>
<%
    String isManager= request.getParameter("manager");
	String taskTypeName= request.getParameter("taskType");
	ScheduleStrategy scheduleStrategy =  ConsoleManager.getScheduleStrategyManager().loadStrategy(taskTypeName);
    boolean isNew = false;
    String actionName ="editScheduleStrategy";
    String editSts="";
	String ips ="";
	if(scheduleStrategy != null){
		String[] ipList =scheduleStrategy.getIPList();
		for(int i=0;ipList!=null&& i<ipList.length;i++){
			if(i>0){
				ips = ips+ ",";
			}
			ips = ips + ipList[i];
		}
		editSts="style=\"background-color: blue\" readonly=\"readonly\"";
	}else{
		scheduleStrategy = new ScheduleStrategy();
		scheduleStrategy.setTaskType("�������µ���������...");
		scheduleStrategy.setNumOfSingleServer(0);
		scheduleStrategy.setAssignNum(2);
		ips = "127.0.0.1";
		
		isNew = true;
		actionName ="createScheduleStrategy";
	}

%>
<html>
<head>
<STYLE type=text/css>

TH{color:#5371BA;font-weight:bold;font-size:12px;background-color:#E4EFF1;display:block;}
TD{font-size:12px;}

</STYLE>
</head>
<body>
<form id="scheduleStrategyForm" method="get" name="scheduleStrategyForm" action="scheduleStrategyDeal.jsp">
<input type="hidden" name="action" value="<%=actionName%>"/>
<table>
<tr>
	<td>��������:</td>
	<td><input type="text" id="taskType" name="taskType"  <%=editSts%> value="<%=scheduleStrategy.getTaskType()%>" width="30"></td>
	<td>��ʽ����������$������</td>
</tr>
<tr>
	<td>��JVM����߳�������:</td>
	<td><input type="text" name="numOfSingleServer" value="<%=scheduleStrategy.getNumOfSingleServer() %>" width="30"></td>
	<td>��JVM����߳��������������0�����ʾû������.ÿ̨�������е��߳������� =����/������ </td>
</tr>
<tr>
	<td>��Ҫ���������߳���������</td>
	<td><input type="text" name="assignNum" value="<%=scheduleStrategy.getAssignNum()%>"  width="30"></td>
	<td>���з������ܹ����е��������</td>
</tr>
<tr>
	<td>IP��ַ(���ŷָ�)��</td>
	<td><input type="text" name="ips" value="<%=ips%>" width="30"></td>
	<td>127.0.0.1����localhost�������л���������</td>
</tr>
</table>
<br/>
<input type="button" value="����" onclick="save();" style="width:100px" >

</form>

</body>
</html>

<script>
function save(){
	var taskType = document.all("taskType").value;
	var reg = /.*[\u4e00-\u9fa5]+.*$/; 
	if(reg.test(taskType)){
	   alert('�������Ͳ��ܺ�����');
	   return;
	}
	if(taskType==null||taskType==''||isContainSpace(taskType)){
		alert('�������Ͳ���Ϊ�ջ���ڿո�');
		return;
	}
    document.getElementById("scheduleStrategyForm").submit();
}
  
function isContainSpace(array) {   
	if(array.indexOf(' ')>=0){
		return true;
	}
    return false;
}
</script>