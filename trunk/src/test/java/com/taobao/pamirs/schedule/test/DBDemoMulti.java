package com.taobao.pamirs.schedule.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.pamirs.schedule.IScheduleTaskDealMulti;
import com.taobao.pamirs.schedule.TaskItemDefine;

/**
 * ������ʵ��
 * 
 * @author xuannan
 * 
 */
public class DBDemoMulti implements	IScheduleTaskDealMulti<Long> {

	private static transient Logger log = LoggerFactory.getLogger(DBDemoMulti.class);

	protected DataSource dataSource;

	public Comparator<Long> getComparator() {
		return new Comparator<Long>() {
			public int compare(Long o1, Long o2) {
				return o1.compareTo(o2);
			}

			public boolean equals(Object obj) {
				return this == obj;
			}
		};
	}

	public List<Long> selectTasks(String taskParameter,String ownSign, int taskItemNum,
			List<TaskItemDefine> queryCondition, int fetchNum) throws Exception {
		List<Long> result = new ArrayList<Long>();
		if (queryCondition.size() == 0) {
			return result;
		}

		StringBuffer condition = new StringBuffer();
		for (int i = 0; i < queryCondition.size(); i++) {
			if (i > 0) {
				condition.append(",");
			}
			condition.append(queryCondition.get(i).getTaskItemId());
		}

		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			String dbType = this.getDataBaseType(conn);
			String sql = null;
			if ("oracle".equalsIgnoreCase(dbType)) {
				sql = "select ID from SCHEDULE_TEST where OWN_SIGN = '"
						+ ownSign + "' and mod(id," + taskItemNum + ") in ("
						+ condition.toString()
						+ ") and sts ='N' and rownum <= " + fetchNum;
			} else if ("mysql".equalsIgnoreCase(dbType)) {
				sql = "select ID from SCHEDULE_TEST where OWN_SIGN = '"
						+ ownSign + "'  and mod(id," + taskItemNum + ") in ("
						+ condition.toString() + ") and sts ='N' LIMIT "
						+ fetchNum;
			} else {
				throw new Exception("��֧�ֵ����ݿ����ͣ�" + dbType);
			}
			PreparedStatement statement = conn.prepareStatement(sql);
			ResultSet set = statement.executeQuery();
			while (set.next()) {
				result.add(set.getLong("ID"));
			}
			set.close();
			statement.close();
			return result;
		} finally {
			if (conn != null)
				conn.close();
		}
	}



	public boolean execute(Long[] tasks, String ownSign) throws Exception {
		Connection conn = null;
		long id = 0;
		try {
			conn = dataSource.getConnection();
			for (int index = 0; index < tasks.length; index++) {
				id = ((Long) tasks[index]).longValue();
				log.debug("��������" + id + " �ɹ���");
				String sql = "update SCHEDULE_TEST SET STS ='Y' ,DEAL_COUNT = DEAL_COUNT + 1 WHERE ID = ? and STS ='N' ";
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setLong(1, id);
				statement.executeUpdate();
				statement.close();
			}
			conn.commit();
		} catch (Exception e) {
			log.error("ִ������" + id + "ʧ�ܣ�" + e.getMessage(), e);
			if (conn != null) {
				conn.rollback();
			}
			return false;
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
		// System.out.println("��������" + tasks.length);
		return true;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	public String getDataBaseType(Connection conn) throws SQLException {
		return conn.getMetaData().getDatabaseProductName();
	}
}