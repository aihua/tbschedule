<%@page import="java.io.StringWriter"%>
<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@ page contentType="text/html; charset=GB2312" %>
<%
if(ConsoleManager.isInitial() == false){
		response.sendRedirect("config.jsp");
}
%>
<html>
<body style="font-size:12px;">

<%
  String path = request.getParameter("path");
  String action = request.getParameter("action");
  if(action == null){
	  action ="query";
  }
  if(path == null){
	  path = ConsoleManager.getScheduleStrategyManager().getRootPath();
	  action ="query";
  }
  StringWriter writer = new StringWriter();
  if(action.equals("delete")){
	  try{
	  	ConsoleManager.getScheduleStrategyManager().deleteTree(path);
	  	writer.write("ɾ��Ŀ¼��" + path + "�ɹ���");
	  }catch(Exception e){
		writer.write(e.getMessage());
	  }
  }else{
      ConsoleManager.getScheduleStrategyManager().printTree(path,writer,"<br/>");
  }
%>
 ����·����
<input id="path" type="text" size="80" value="<%=path%>"/>
<input type="button" value="��ѯ" onclick="queryPath()"/>
<input type="checkbox" id="openDeleteButton" onclick="openDelete()">
<input type="button" id ="deleteButton" disabled="disabled" value="ɾ��" onclick="deletePath()"/>
<LABEL  style="color:red"><b>��ע��,��ɾ����Ŀ¼����Ŀ¼���������ݣ����Ҳ��ɻָ���</b></LABEL >
<hr/>
<pre>
<%=writer.getBuffer().toString()%>
</pre>
</body>
</html>

<script>
function queryPath(){	
	var url = "deleteZookeeperData.jsp?action=query&path=" + document.all("path").value;
    location.href= url;
}
function deletePath(){	
    location.href="deleteZookeeperData.jsp?action=delete&path=" + document.all("path").value;
}
function openDelete(){	
    if(document.all("openDeleteButton").checked == false){
    	document.all("deleteButton").disabled=true;
    }else{
    	document.all("deleteButton").disabled=false;
    }
}
</script>
