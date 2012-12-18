package com.taobao.pamirs.schedule.strategy;

public interface IStrategyTask {
   public void initialTaskParameter(String taskParameter);
   public void stop() throws Exception;
}
