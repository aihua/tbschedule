<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType"%>
<%@page import="java.util.List"%>
<%@ page contentType="text/html; charset=GB2312" %>
<%
    String isManager= request.getParameter("manager");
	String taskTypeName= request.getParameter("taskType");
    ScheduleTaskType taskType =  ConsoleManager.getScheduleDataManager().loadTaskTypeBaseInfo(taskTypeName);
    String taskItems ="";
    boolean isNew = false;
    String actionName ="editTaskType";
    String editSts ="";
	if(taskType != null){
		String[] taskItemList = taskType.getTaskItems();
		for(int j=0;j<taskItemList.length;j++){
			if(j>0){
				taskItems = taskItems+ ",";
			}
			taskItems = taskItems + taskItemList[j];
		}
		editSts="style=\"background-color: blue\" readonly=\"readonly\"";
	}else{
		taskType = new ScheduleTaskType();
		taskType.setBaseTaskType("�������µ���������...");
		taskType.setDealBeanName("");
		isNew = true;
		actionName ="createTaskType";
	}

%>
<html>
<head>

<STYLE type=text/css>

TH{color:#5371BA;font-weight:bold;font-size:12px;background-color:#E4EFF1;display:block;}
TD{font-size:12px;}

</STYLE>
</head>
<body style="font-size:12px;">
<form id="taskTypeForm" method="get" name="taskTypeForm" action="taskTypeDeal.jsp">
<input type="hidden" name="action" value="<%=actionName%>"/>
<input type="hidden" name="sts" value="<%=taskType.getSts()%>"/>

<table>
<tr>
	<td>��������:</td><td><input type="text" id="taskType" name="taskType"  <%=editSts%> value="<%=taskType.getBaseTaskType()%>" width="30"></td>
	<td>�������SpringBean:</td><td><input type="text" id="dealBean" name="dealBean" value="<%=taskType.getDealBeanName()%>" width="30"></td>
</tr>
<tr>
	<td>����Ƶ��(��):</td><td><input type="text" name="heartBeatRate" value="<%=taskType.getHeartBeatRate()/1000.0 %>" width="30"></td>
	<td>�ٶ������������(��):</td><td><input type="text" name="judgeDeadInterval" value="<%=taskType.getJudgeDeadInterval()/1000.0 %>" width="30"></td>
</tr>
<tr>
	<td>�߳�����</td><td><input type="text" name="threadNumber" value="<%=taskType.getThreadNumber()%>"  width="30"></td>
	<td>����ģʽ��</td><td><input type="text" name="processType" value="<%=taskType.getProcessorType()%>" width="30">
		SLEEP ��  NOTSLEEP</td>
</tr>
<tr>
	<td>ÿ�λ�ȡ��������</td><td><input type="text" name="fetchNumber" value="<%=taskType.getFetchDataNumber() %>" width="30"></td>
	<td>ÿ��ִ��������</td><td><input type="text" name="executeNumber" value="<%=taskType.getExecuteNumber() %>" width="30">
		ֻ��beanʵ��IScheduleTaskDealMulti����Ч</td>
</tr>
<tr>
	<td>û������ʱ����ʱ��(��)��</td><td><input type="text" name="sleepTimeNoData" value="<%=taskType.getSleepTimeNoData()/1000.0%>" width="30"></td>
	<td>ÿ�δ��������ݺ�����ʱ��(��)��</td><td><input type="text" name="sleepTimeInterval" value="<%= taskType.getSleepTimeInterval()/1000.0%>" width="30"></td>
</tr>
<tr>
	<td>ִ�п�ʼʱ�䣺</td><td><input type="text" name="permitRunStartTime" value="<%=taskType.getPermitRunStartTime()==null?"":taskType.getPermitRunStartTime()%>" width="30"></td>
	<td>ִ�н���ʱ�䣺</td><td><input type="text" name="permitRunEndTime" value="<%=taskType.getPermitRunEndTime()==null?"":taskType.getPermitRunEndTime()%>" width="30"></td>
</tr>
<tr>
	<td>���߳�����������</td><td><input type="text" name="maxTaskItemsOfOneThreadGroup" value="<%=taskType.getMaxTaskItemsOfOneThreadGroup()%>" width="30"></td>
	<td colspan="2">ÿһ���߳��ܷ��������������������������Ż����ļ��ٰ������ķ�����ѹ����0���߿ձ�ʾ������</td>
</tr>
<tr>
	<td>�Զ������(�ַ���):</td><td colspan="3"><input type="text" id="taskParameter" name="taskParameter" value="<%=taskType.getTaskParameter()==null?"":taskType.getTaskParameter()%>" style="width:657"></td>
</tr>
<tr>
	<td>������(","�ָ�):</td><td colspan="3"><TEXTAREA  type="textarea" rows="5" , id="taskItems" name="taskItems" style="width:657"><%=taskItems%> </TEXTAREA></td>
</tr>

</table>
<br/>
<input type="button" value="����" onclick="save();" style="width:100px" >

</form>
<b>ִ�п�ʼʱ��˵����</b><br/>
1.����ִ��ʱ�εĿ�ʼʱ��crontab��ʱ���ʽ.'0 * * * * ?'  ��ʾ��ÿ���ӵ�0�뿪ʼ<br/>
2.��startrun:��ʼ�����ʾ����������������.<br/>
3.��ʽ�μ��� http://dogstar.javaeye.com/blog/116130<br/><br/>
<b>ִ�н���ʱ��˵����</b><br/>
1.����ִ��ʱ�εĽ���ʱ��crontab��ʱ���ʽ,'20 * * * * ?'  ��ʾ��ÿ���ӵ�20����ֹ<br/>
2.��������ã���ʾȡ�������ݾ�ֹͣ <br/>
3.��ʽ�μ���http://dogstar.javaeye.com/blog/116130<br/><br/>
<b>�������˵����</b><br/>
1����һ�����ݱ����������ݵ�ID��10ȡģ���ͽ����ݻ��ֳ���0��1��2��3��4��5��6��7��8��9��10�������<br/>
2����һ��Ŀ¼�µ������ļ����ļ����Ƶ�����ĸ(�����ִ�Сд)�� �ͻ��ֳ���A��B��C��D��E��F��G��H��I��J��K��L��M��N��O��P��Q��R��S��T��U��V��W��X��Y��Z��26�������<br/>
3����һ�����ݱ������ID��ϣ��1000ȡģ��Ϊ����HASHCODE,���ǾͿ��Խ����ݰ�[0,100)��[100,200) ��[200,300)��[300,400) ��[400,500)��[500,600)��[600,700)��[700,800)��[800,900)�� [900,1000)����Ϊʮ�������
	��Ȼ��Ҳ���Ի���Ϊ100������������1000���<br/>
4���������ǽ�������������С��λ��һ���������ֻ����һ��ScheduleServer�����д�����һ��Server���Դ������������������
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
	var str = document.all("dealBean").value;
	if(str == null || str.length==0){
		alert("�����봦�������bean���ƣ���");
		return;
	}
	if(isContainSpace(str)){
		alert('���������bean���Ʋ��ܴ��ڿո�');
		return;
	}
	if(reg.test(str)){
	   alert('bean���Ʋ��ܺ�����');
	   return;
	}
    str = document.all("taskItems").value;
	if(str == null || str.length==0){
		alert("�������������");
		return;
	}
    document.getElementById("taskTypeForm").submit();
}

function isContainSpace(array) {   
	if(array.indexOf(' ')>=0){
		return true;
	}
    return false;
}
</script>