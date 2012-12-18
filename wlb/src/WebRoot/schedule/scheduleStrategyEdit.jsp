<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="com.taobao.pamirs.schedule.strategy.ScheduleStrategy"%>
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
		scheduleStrategy.setStrategyName("");
		scheduleStrategy.setKind(ScheduleStrategy.Kind.Schedule);
		scheduleStrategy.setTaskName("");
		scheduleStrategy.setTaskParameter("");
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
	<td><input type="text" id="strategyName" name="strategyName"  <%=editSts%> value="<%=scheduleStrategy.getStrategyName()%>" width="30"></td>
	<td>������д�����������ĺ������ַ�</td>
</tr>
<tr>
	<td>��������:</td>
	<td><input type="text" id="kind" name="kind"   value="<%=scheduleStrategy.getKind().toString()%>" width="30"></td>
	<td>��ѡ���ͣ�Schedule,Java,Bean ��Сд����</td>
</tr>
<tr>
	<td>��������:</td>
	<td><input type="text" id="taskName" name="taskName"  value="<%=scheduleStrategy.getTaskName()%>" width="30"></td>
	<td>����������ƥ����������磺1��������������õ���������(��ӦSchedule) 2��Class����(��Ӧjava) 3��Bean������(��ӦBean)</td>
</tr>
<tr>
	<td>�������:</td>
	<td><input type="text" id="taskParameter" name="taskParameter"   value="<%=scheduleStrategy.getTaskParameter()%>" width="30"></td>
	<td>���ŷָ���Key-Value�� ����������ΪSchedule����Ч����Ҫͨ��������������õ�</td>
</tr>

<tr>
	<td>��JVM����߳�������:</td>
	<td><input type="text" name="numOfSingleServer" value="<%=scheduleStrategy.getNumOfSingleServer() %>" width="30"></td>
	<td>��JVM����߳��������������0�����ʾû������.ÿ̨�������е��߳������� =����/������ </td>
</tr>
<tr>
	<td>����߳���������</td>
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
	var strategyName = document.all("strategyName").value;
	var reg = /.*[\u4e00-\u9fa5]+.*$/; 
	if(reg.test(strategyName)){
	   alert('�������Ͳ��ܺ�����');
	   return;
	}
	if(strategyName==null||strategyName==''||isContainSpace(strategyName)){
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