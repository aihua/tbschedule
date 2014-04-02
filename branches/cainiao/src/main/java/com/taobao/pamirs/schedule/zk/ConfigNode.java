package com.taobao.pamirs.schedule.zk;

/**
 * ������Ϣ
 * 
 * @author gjavac@gmail.com
 * @since 2012-2-12
 * @version 1.0
 */
public class ConfigNode {

	private String rootPath;

	private String configType;

	private String name;

	private String value;

	public ConfigNode() {

	}

	public ConfigNode(String rootPath, String configType, String name) {
		this.rootPath = rootPath;
		this.configType = configType;
		this.name = name;
	}

	public String getRootPath() {
		return rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public String getConfigType() {
		return configType;
	}

	public void setConfigType(String configType) {
		this.configType = configType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("���ø�Ŀ¼��").append(rootPath).append("\n");
		buffer.append("�������ͣ�").append(configType).append("\n");
		buffer.append("�������ƣ�").append(name).append("\n");
		buffer.append("���õ�ֵ��").append(value).append("\n");
		return buffer.toString();
	}
}
