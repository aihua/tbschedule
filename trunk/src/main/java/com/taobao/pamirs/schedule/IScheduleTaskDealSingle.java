package com.taobao.pamirs.schedule;

/**
 * ����������Ľӿ�
 * @author xuannan
 *
 * @param <T>��������
 */
public interface IScheduleTaskDealSingle<T> extends IScheduleTaskDeal<T> {
  /**
   * ִ�е�������
   * @param task Object
   * @param ownSign ��ǰ��������
   * @throws Exception
   */
  public boolean execute(T task,String ownSign) throws Exception;
  
}
