package com.taobao.pamirs.schedule;


import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectorServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.JdkVersion;

public class MBeanManagerFactory {
	protected static transient Log logger = LogFactory.getLog(MBeanManagerFactory.class);
	private static MBeanServer mbeanServer;
	private static JMXConnectorServer jmxConnectorServer;
	
	public static MBeanServer getMbeanServer()
			throws MBeanRegistrationException {
		if (mbeanServer != null) {
			return mbeanServer;
		}
		synchronized (MBeanManagerFactory.class) {
			if (mbeanServer != null) {
				return mbeanServer;
			}
			String agentId = TBScheduleManagerFactory.getMbeanServerAgentId();
			List<MBeanServer> serverList = MBeanServerFactory
					.findMBeanServer(agentId);
			if (agentId == null || agentId.trim().length() == 0
					|| serverList == null || serverList.size() == 0) {
				if (JdkVersion.isAtLeastJava15()) {
					mbeanServer = ManagementFactory.getPlatformMBeanServer();
				} else {
					throw new MBeanRegistrationException(null,
							"在JVM中还没有注册MBeanServer of agentId =" + agentId);
				}
			} else if (serverList.size() > 1) {
				throw new MBeanRegistrationException(
						null,
						"在JVM中存在多个MBeanServer of agentId ="
								+ agentId
								+ ",请合理的设置TBScheduleManagerFactory的属性mbeanServerAgentId");
			}else{
			    mbeanServer = serverList.get(0);
			}
		}
		return mbeanServer;
	}
    public static boolean isRegistered(String name) throws MBeanRegistrationException, MalformedObjectNameException, NullPointerException{
    	return getMbeanServer().isRegistered(new ObjectName(name));
    }
	public static ObjectName registerMBean(Object object, String name)
			throws InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException, MalformedObjectNameException, NullPointerException {	
		ObjectName result = new ObjectName(name);
		getMbeanServer().registerMBean(object,result);
		return result;
	}

	public static void unregisterMBean(String name)
			throws InstanceNotFoundException, MBeanRegistrationException, MalformedObjectNameException, NullPointerException {
		getMbeanServer().unregisterMBean(new ObjectName(name));
	}

	public static int getHtmlAdaptorPort() throws AttributeNotFoundException,
			InstanceNotFoundException, MBeanException, ReflectionException,
			ClassNotFoundException {
		return -1;
	}

	public static String getManangerUrl() throws ClassNotFoundException,
			AttributeNotFoundException, InstanceNotFoundException,
			MBeanException, ReflectionException {
		String result = "没有找到jmxConnectorServer";
		if(jmxConnectorServer != null){
			result = jmxConnectorServer.getAddress().toString();
		}else{
		MBeanServer server = getMbeanServer();
		Set<ObjectInstance> sets = server.queryMBeans(null, null);
		
		try {
			for (ObjectInstance o : sets) {
				if (JMXConnectorServer.class.isAssignableFrom(Class.forName(o
						.getClassName()))) {
					result = server.getAttribute(o.getObjectName(), "Address")
							.toString();
				}
			}
		} catch (Throwable e) {
			logger.error("没有找到MBean的jmxConnectorServer");
		}
		}
		return result.replaceAll("localhost", ScheduleUtil.getLocalIP());
	}

	public void setJmxConnectorServer(JMXConnectorServer jmxConnectorServer) {
		MBeanManagerFactory.jmxConnectorServer = jmxConnectorServer;
	}
}
