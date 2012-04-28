package com.taobao.pamirs.schedule;

import java.util.Iterator;

import javax.management.DynamicMBean;
import javax.management.ReflectionException;
import javax.management.Attribute;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.AttributeList;
import javax.management.MBeanInfo;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;

public abstract class AbstractDynamicMBean implements DynamicMBean {
	protected MBeanInfo dMBeanInfo = null;

	public Object getAttribute(String attrName)
			throws AttributeNotFoundException, MBeanException,
			ReflectionException {
		if (attrName == null) {
			throw new AttributeNotFoundException("�������Ʋ���Ϊ��");
		}
		try {
			return PropertyUtils.getNestedProperty(this, attrName);
		} catch (Exception ex) {
			throw new AttributeNotFoundException(ex.getMessage());
		}
	}

	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException {
		if (attribute == null) {
			throw new AttributeNotFoundException("����������Ϣ����Ϊ��");
		}
		try {
			PropertyUtils.setNestedProperty(this, attribute.getName(),
					attribute.getValue());
		} catch (Exception ex) {
			throw new AttributeNotFoundException(ex.getMessage());

		}
	}

	public AttributeList getAttributes(String[] stringArray) {
		if (stringArray == null) {
			throw new RuntimeException("����������Ϣ����Ϊ��");
		}
		AttributeList resultList = new AttributeList();

		if (stringArray.length == 0)
			return resultList;
		for (int i = 0; i < stringArray.length; i++) {
			try {
				Object value = getAttribute((String) stringArray[i]);
				resultList.add(new Attribute(stringArray[i], value));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return resultList;
	}

	public AttributeList setAttributes(AttributeList attributes) {
		AttributeList resultList = new AttributeList();
		if (attributes.isEmpty()) {
			return resultList;
		}
		for (Iterator<Object> i = attributes.iterator(); i.hasNext();) {
			Attribute attr = (Attribute) i.next();
			try {
				setAttribute(attr);
				String name = attr.getName();
				Object value = getAttribute(name);
				resultList.add(new Attribute(name, value));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return resultList;
	}

	public Object invoke(String operationName, Object[] parameterArray,
			String[] signature) throws MBeanException, ReflectionException {
		if (operationName == null) {
			throw new MBeanException(new IllegalArgumentException("��������Ϊ��"),
					"��������Ϊ��");
		}
		try {
			return MethodUtils
					.invokeMethod(this, operationName, parameterArray);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public MBeanInfo getMBeanInfo() {
		if (dMBeanInfo == null) {
			buildDynamicMBeanInfo();
		}
		return dMBeanInfo;
	}

	protected abstract void buildDynamicMBeanInfo();
}
