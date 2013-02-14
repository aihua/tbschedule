package com.taobao.pamirs.schedule.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.taobao.pamirs.schedule.strategy.IStrategyTask;

public class JavaTaskDemo implements IStrategyTask,Runnable {
	protected static transient Log log = LogFactory.getLog(JavaTaskDemo.class);


    private String parameter;
    private boolean stop = false;
	public void initialTaskParameter(String strategyName,String taskParameter) {
		parameter = taskParameter;
		new Thread(this).start();
	}

	@Override
	public void stop(String strategyName) throws Exception {
		this.stop = true;
	}

	@Override
	public void run() {
		while(stop == false){
			log.error("Ö´ÐÐÈÎÎñ£º"  + this.parameter);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
