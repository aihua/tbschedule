<%@page import="java.io.*"%>
<%@page import="com.taobao.pamirs.schedule.ConsoleManager"%>
<%@ page contentType="text/html; charset=GB2312"%>
<%
	if (ConsoleManager.isInitial() == false) {
		response.sendRedirect("config.jsp");
	}
	StringWriter confWriter = new StringWriter();
	StringWriter errWriter = new StringWriter();
	String rootPath = ConsoleManager.getScheduleStrategyManager().getRootPath(); 
	String type = (String) request.getParameter("type");
	if ("POST".equals(request.getMethod())) {
		StringWriter tmpWriter = new StringWriter();
		try {
			StringBuffer buffer = null;
			if (rootPath != null && rootPath.length() > 0) {
				buffer = ConsoleManager.getScheduleStrategyManager()
						.exportConfig(rootPath, confWriter);
			} else {
				tmpWriter.write("û�����õ���������Ϣ��·��");
			}
			// �����ļ�
			if (type != null && type.equals("1")) {
				// �������б���
				if (buffer != null) {
					response.setContentType("text/plain;charset=GBK");
					response.setHeader("Content-disposition",
							"attachment; filename=config.txt");
					PrintWriter out_=response.getWriter();
					out_.print(buffer.toString());
					out_.close();
				}
			}
			if (tmpWriter != null && tmpWriter.toString().length() > 0) {
				errWriter.write("<font color=\"red\">������Ϣ��</font>\n\t");
				errWriter.write(tmpWriter.toString());
			}
		} catch (Exception e) {
			if (tmpWriter != null && tmpWriter.toString().length() > 0) {
				errWriter.write("<font color=\"red\">������Ϣ��</font>\n\t");
				errWriter.write(tmpWriter.toString());
			}
			StringWriter strWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(strWriter);
			e.printStackTrace(printWriter);
			errWriter.write("<font color=\"red\">����Ķ�ջ��Ϣ:</font>\n\t" + e.getMessage()+"\n"+strWriter);
		}
	}
%>
<html>
<body style="font-size: 12px;">
<form id="taskTypeForm" method="post" name="taskTypeForm"
	action="exportConfig.jsp"><input type="hidden" id="type"
	name="type" value="1" /> �����ļ�·����<input type="text" name="rootPath"
	value="<%=rootPath == null ? "" : rootPath%>" style="width:330px;" />
<input type="button" onclick="viewConfig();" value="�鿴" /> <input
	type="button" onclick="saveConfig();" value="����" />
<pre>
<%if(errWriter==null || errWriter.getBuffer().length() == 0) { %>
<%=confWriter%>
<%} %><h3>
<%=errWriter == null ? "" : errWriter%>
</h3>
</pre>
</form>
<script>
	// �鿴�����ļ� 
	function viewConfig() {
		document.getElementById("type").value = "0";
		document.getElementById("taskTypeForm").submit();
	}
	// ���������ļ�
	function saveConfig() {
		document.getElementById("type").value = "1";
		document.getElementById("taskTypeForm").submit();
	}
</script>
</body>
</html>
