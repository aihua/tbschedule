
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleTaskType"%>
<%@page import="com.taobao.pamirs.schedule.taskmanager.ScheduleServer"%>
<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@page import="java.util.List"%>
<%@ page contentType="text/html; charset=GB2312" %>
<html>
<head>
<title>
������������ʷ��Ϣ
</title>
<STYLE type=text/css>


TH{height:20px;color:#5371BA;font-weight:bold;font-size:12px;text-align:center;border:#8CB2E3 solid;border-width:0 1 1 0;background-color:#E4EFF1;white-space:nowrap;overflow:hidden;}
TD{background-color: ;border:#8CB2E3 1px solid;border-width:0 1 1 0;font-size:12px;}
table{border-collapse:collapse}
</STYLE>

</head>
<body style="font-size:12px;">

<%
String managerFactoryUUID = request.getParameter("managerFactoryUUID");
String baseTaskType =  request.getParameter("baseTaskType");
String ownSign =  request.getParameter("ownSign");
String ip =  request.getParameter("ip");
String orderStr =  request.getParameter("orderStr");
%>

<%
if(managerFactoryUUID == null || managerFactoryUUID.trim().length() == 0){
%>
<table border="0">
  <tr>
  	<td>�������ͣ�</td><td><input type="text" id="baseTaskType" value="<%=baseTaskType==null?"":baseTaskType%>"> </td>
  	<td>������</td><td><input type="text" id="ownSign" value="<%=ownSign==null?"":ownSign%>"> </td>
  	<td>IP��</td><td><input type="text" id="ip" value="<%=ip==null?"":ip%>"> </td>
  	<td>����</td><td><input type="text" id="orderStr" value="<%=orderStr==null?"":orderStr%>"> </td>
  	<td><input type="button"  onclick="query()" value="��ѯ" style="width:100;"></td>
  </tr>  
</table>
<%
}
%>
   <table id="list" border="1" style=";border-COLLAPSE: collapse;display:block;">
   <tr >
   <th nowrap>���</th>
   <th>��������<BR/>[TASK_TYPE]</th>
   <th>��<BR/>[OWN_SIGN]</th>
   <th>IP��ַ<BR/>[IP]</th>
   <th>��������[HOST_NAME]</th>
   <th nowrap>�߳�<BR/>[THREAD_NUM]</th>
   <th>ע��ʱ��<BR/>[REGISTER_TIME]</th>
   <th>����ʱ��<BR/>[HEARTBEAT_TIME]</th>
   <th>ȡ��ʱ��<BR/>[LAST_FETCH_DATA_TIME]</th>
   <th nowrap>�汾<BR/>[VERSION]</th>
   <th nowrap>�´ο�ʼ<BR/>[NEXT_RUN_START_TIME]</th>
   <th nowrap>�´ν���<BR/>[NEXT_RUN_END_TIME]</th>
   <th>������<BR/>[MANAGER_FACTORY]</th>
   <th>��������</th>   
   </tr>
   <%
   List<ScheduleServer> serverList = null;
   if(managerFactoryUUID != null && managerFactoryUUID.trim().length() >0){
	   serverList = ConsoleManager.getScheduleDataManager().selectScheduleServerByManagerFactoryUUID(managerFactoryUUID);
   }else{
	   serverList = ConsoleManager.getScheduleDataManager()
       .selectScheduleServer(baseTaskType,ownSign,ip,orderStr);
   }
   
   for(int j =0;j<serverList.size();j++){
	   String bgColor="";
	   ScheduleTaskType base = ConsoleManager.getScheduleDataManager().loadTaskTypeBaseInfo(serverList.get(j).getBaseTaskType());
	   if(serverList.get(j).getCenterServerTime().getTime() - serverList.get(j).getHeartBeatTime().getTime() > base.getJudgeDeadInterval()){
		   bgColor = "BGCOLOR='#A9A9A9'";
	   }else if(serverList.get(j).getLastFetchDataTime() == null || serverList.get(j).getCenterServerTime().getTime() - serverList.get(j).getLastFetchDataTime().getTime() > base.getHeartBeatRate()*20){
		   bgColor = "BGCOLOR='#FF0000'";
	   }
   %>
	   <tr onclick="openDetail(this)" <%=bgColor%>>
	   <td><%=(j+1) %></td>
	   <td><%=serverList.get(j).getBaseTaskType()%></td>	  
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
	   <td nowrap><%=serverList.get(j).getManagerFactoryUUID()%></td>	
	   <td nowrap><%=serverList.get(j).getDealInfoDesc()%></td>	
	   </tr>      
   <%
   }
   %>
   </table>
</body>
</html>

<script>

function query(){
	 var baseTaskType=document.all("baseTaskType").value;
	 var ownSign=document.all("ownSign").value;
	 var ip=document.all("ip").value;
	 var orderStr=document.all("orderStr").value;	 
	 var url =  "serverList.jsp?a=1";
	 if(baseTaskType != null && baseTaskType != ""){
		 url = url + "&baseTaskType=" + baseTaskType;
	 }
	 if(ownSign != null&& ownSign != ""){
		 url = url + "&ownSign=" + ownSign;
	 }
	 if(ip != null&& ip != ""){
		 url = url + "&ip=" + ip;
	 }
	 if(orderStr != null&& orderStr != ""){
		 url = url + "&orderStr=" + orderStr;
	 }
	 window.location.href =  url;
}


var oldSelectRow = null;
var oldBgColor=null;
function openDetail(obj){
	if(oldSelectRow != null){
		oldSelectRow.bgColor=oldBgColor;
	}
	oldBgColor=obj.bgColor;
	obj.bgColor="#FFD700";
	oldSelectRow = obj;
}

</script>
