# tbschedule

由原来的项目taobao-pamirs-schedule更名为TBSchedule。

Mavan POM文件：
```XML
<groupId>com.taobao.pamirs.schedule</groupId>
    <artifactId>tbschedule</artifactId>
<version>3.2.16</version>
```

此文档内部包括：

1.   设计目标说明；
2.   主要概念解释；
3.   涉及的数据对象说明；
4.   涉及的业务接口说明；
5.   Sleep模式和NotSleep模式的区别；
6.   使用过程中的注意事项。

## 1.调度器的设计目标
1.   tbschedule的目的是让一种批量任务或者不断变化的任务，能够被动态的分配到多个主机的JVM中，
    不同的线程组中并行执行。所有的任务能够被不重复，不遗漏的快速处理。
2.   调度的Manager可以动态的随意增加和停止。
3.   可以通过JMX控制调度服务的创建和停止。
4.   可以指定调度的时间区间：
  - `PERMIT_RUN_START_TIME`：允许执行时段的开始时间crontab的时间格式.以startrun:开始，则表示开机立即启动调度；
  - `PERMIT_RUN_END_TIME`：允许执行时段的结束时间crontab的时间格式,如果不设置，表示取不到数据就停止；
  - `PERMIT_RUN_START_TIME ='0 * * * * ?'`表示在每分钟的0秒开始；
  - `PERMIT_RUN_START_TIME ='20 * * * * ?'`表示在每分钟的20秒终止，就是每分钟的0-20秒执行，其它时间休眠。

## 2.主要概念

### 2.1`TaskType`(任务类型)

`TaskType`(任务类型)是任务调度分配处理的单位，例如：

1.   将一张表中的所有状态为`STS=’N’`的所有数据提取出来发送给其它系统，同时将修改状态`STS=’Y’`，就是一种任务。`TaskType=’DataDeal’`。
2.   将一个目录以所有子目录下的所有文件读取出来入库，同时把文件移到对应的备份目录中，也是一种任务。`TaskType=’FileDeal’`。
3.   可以为一个任务类型自定义一个字符串参数由应用自己解析。例如：`"AREA=杭州,YEAR>30"`。

### 2.2`ScheduleServer`(任务处理器)

1.   是由一组线程`1..n`个线程构成的任务处理单元，每一任务处理器有一个唯一的全局标识，一般以`IP$UUID`，
     例如`192.168.1.100$0C78F0C0FA084E54B6665F4D00FA73DC`的形式出现。 一个任务类型的数据可以由`1..n`个任务处理器同时进行。
2.   这些任务处理器可以在同一个JVM中，也可以分布在不同主机的JVM中。任务处理器内部有一个心跳线程，用于确定Server的状态和任务的动态分配，
     有一组工作线程，负责处理查询任务和具体的任务处理工作。
3.   目前版本所有的心跳信息都是存放在Zookeeper服务器中的，所有的Server都是对等的，当一个Server死亡后，其它Server会接管起拥有的任务队列，
     期间会有几个心跳周期的时延。后续可以用类似`ConfigerServer`类的存储。
4.   现有的工作线程模式分为`Sleep`模式和`NotSleep`模式。缺省是`NotSleep`模式。在通常模式下，在通常情况下用`Sleep`模式。
     在一些特殊情况需要用NotSleep模式。两者之间的差异在后续进行描述。

### 2.3`TaskItem`(任务项)

`TaskItem`(任务项)是对任务进行的分片划分。例如：

1.   将一个数据表中所有数据的ID按10取模，就将数据划分成了0、1、2、3、4、5、6、7、8、9供10个任务项。
2.   将一个目录下的所有文件按文件名称的首字母(不区分大小写)，
     就划分成了A、B、C、D、E、F、G、H、I、J、K、L、M、N、O、P、Q、R、S、T、U、V、W、X、Y、Z供26个队列。
3.   将一个数据表的数据ID哈希后按1000取模作为最后的Hashcode，我们就可以将数据按[0,100)、[100,200) 、[200,300)、[300,400) 、
     [400,500)、[500,600)、[600,700)、[700,800)、[800,900)、 [900,1000)划分为十个任务项，当然你也可以划分为100个任务项，
     最多是1000个任务项。
     任务项是进行任务分配的最小单位。一个任务项只能由一个`ScheduleServer`来进行处理。但一个Server可以处理任意数量的任务项。
     例如，任务被划分为了10个队列，可以只启动一个Server，所有的任务项都有这一个Server来处理；也可以启动两个Server，每个Sever处理5个任务项；
     但最多只能启动10个Server，每一个`ScheduleServer`只处理一个任务项。如果在多，则第11个及之后的Server将不起作用，处于休眠状态。
4.   可以为一个任务项自定义一个字符串参数由应用自己解析。例如，`"TYPE=A,KIND=1"`。

### 2.4`TaskDealBean`(任务处理类)

`TaskDealBean`(任务处理类)是业务系统进行数据处理的实现类。要求实现`Schedule`的接口`IScheduleTaskDealMulti`或者`IScheduleTaskDealSingle`。
接口主要包括两个方法。一个是根据调度器分配到的队列查询数据的接口，一个是进行数据处理的接口。

运行时间

1.   可以指定任务处理的时间间隔，例如每天的1:00-3:00执行，或者每个月的第一天执行、每一个小时的第一分钟执行等等。
     时间格式与`crontab`相同。如果不指定就表示一致运行。`PERMIT_RUN_START_TIME`,`PERMIT_RUN_END_TIME`。
2.   可以指定如果没有数据了，休眠的时间间隔。`SLEEP_TIME_NODATA`，单位：秒。
3.   可以指定每处理完一批数据后休眠的时间间隔。`SLEEP_TIME_INTERVAL`，单位：秒。

### 2.5`OwnSign`(环境区域)

`OwnSign`(环境区域)是对运行环境的划分，进行调度任务和数据隔离。例如：开发环境、测试环境、预发环境、生产环境。

不同的开发人员需要进行数据隔离也可以用`OwnSign`来实现，避免不同人员的数据冲突。缺省配置的环境区域`OwnSign='BASE'`。
例如：`TaskType='DataDeal'`，配置的队列是0、1、2、3、4、5、6、7、8、9。此时，如果再启动一个测试环境，
则`Schedule`会动态生成一个`TaskType='DataDeal-Test'`的任务类型，环境会作为一个变量传递给业务接口，
由业务接口的实现类，在读取数据和处理数据的时候进行确定。业务系统一种典型的做法就是在数据表中增加一个`OWN_SIGN`字段。
在创建数据的时候根据运行环境填入对应的环境名称，在`Schedule`中就可以环境的区分了。

### 2.6调度策略

**调度策略**是指某一个任务在调度集群上的分布策略，可以制定：

1.   可以指定任务的机器IP列表。`127.0.0.1`和`localhost`表示所有机器上都可以执行；
2.   可以指定每个机器上能启动的线程组数量，0表示没有限制；
3.   可以指定所有机器上运行的线程组总数。

## 3.业务接口说明

有以下三个业务接口：

1.   `IScheduleTaskDeal`调度器对外的基础接口，是一个基类，并不能被直接使用；
2.   `IScheduleTaskDealSingle`单任务处理的接口，继承`IScheduleTaskDeal`；
3.   `IScheduleTaskDealMulti`可批处理的任务接口，继承`IScheduleTaskDeal`。

### 3.1`IScheduleTaskDeal`(调度器对外的基础接口)

```java
public interface IScheduleTaskDeal<T> {
/**
 * 根据条件，查询当前调度服务器可处理的任务	
 * @param taskParameter 任务的自定义参数
 * @param ownSign 当前环境名称
 * @param taskQueueNum 当前任务类型的任务队列数量
 * @param taskQueueList 当前调度服务器，分配到的可处理队列
 * @param eachFetchDataNum 每次获取数据的数量
 * @return
 * @throws Exception
 */
public List<T> selectTasks(String taskParameter,String ownSign,int taskQueueNum,List<TaskItemDefine> taskItemList,int eachFetchDataNum) throws Exception;

/**
 * 获取任务的比较器,只有在NotSleep模式下需要用到
 * @return
 */
public Comparator<T> getComparator();
} 
```

### 3.2`IScheduleTaskDealSingle`(单任务处理的接口)

```java
public interface IScheduleTaskDealSingle<T> extends IScheduleTaskDeal<T> {
  /**
   * 执行单个任务
   * @param task Object
   * @param ownSign 当前环境名称
   * @throws Exception
   */
  public boolean execute(T task,String ownSign) throws Exception;
  
}                                 
```

### 3.3`IScheduleTaskDealMulti`(可批处理的任务接口)

```java
public interface IScheduleTaskDealMulti<T>  extends IScheduleTaskDeal<T> {
 
/**
 * 	执行给定的任务数组。因为泛型不支持new 数组，只能传递OBJECT[]
 * @param tasks 任务数组
 * @param ownSign 当前环境名称
 * @return
 * @throws Exception
 */
  public boolean execute(Object[] tasks,String ownSign) throws Exception;
}
```

## 4.`Sleep`模式和`NotSleep`模式的区别

1.   `ScheduleServer`启动的工作线程组线程是共享一个任务池的。
2.   在`Sleep`的工作模式：当某一个线程任务处理完毕，从任务池中取不到任务的时候，检查其它线程是否处于活动状态。如果是，则自己休眠；
     如果其它线程都已经因为没有任务进入休眠，当前线程是最后一个活动线程的时候，就调用业务接口，获取需要处理的任务，放入任务池中，
     同时唤醒其它休眠线程开始工作。
3.   在`NotSleep`的工作模式：当一个线程任务处理完毕，从任务池中取不到任务的时候，立即调用业务接口获取需要处理的任务，放入任务池中。
4.   `Sleep`模式在实现逻辑上相对简单清晰，但存在一个大任务处理时间长，导致其它线程不工作的情况。
5.   在`NotSleep`模式下，减少了线程休眠的时间，避免大任务阻塞的情况，但为了避免数据被重复处理，增加了CPU在数据比较上的开销。
     同时要求业务接口实现对象的比较接口。
6.   如果对任务处理不允许停顿的情况下建议用`NotSleep`模式，其它情况建议用`Sleep`模式。

## 5.使用过程中的注意事项

1.   同一个jvm中，不同线程之间如何防止任务被重复执行？一个`ScheduleServer`的内部线程间如何进行任务分片？
答：数据分片是在不同的jvm，获知同一个jvm中不同的线程组间起作用。在同一个线程组内的10个线程，是通过一个同步的任务队列来实现的。
每个线程从队列中取任务执行，如果没有任务了，则由一个线程负责调用`selectTasks`方法再获取一批新的任务。

2.   任务项设置的意义和`selectTasks`方法的参数含义
答：任务项`（0,1,2,3,4,5,6,7,8,9）`就是任务分片的策略。这个配置就是把数据分成10片。可以表示ID的最后一位，也可以是一个独立的字段。根据你的业务来定。
如果只有1组线程，则所有的任务片都分配给他。这时`selectTasks`方法的参数：`taskItemNum=10`, `queryCondition`由10个元素，分别对应`0,1,2,3,4,5,6,7,8,9`。
如果只有2组线程，则任务片被分成两份。这时，一个线程组的`selectTasks`方法的参数：`taskItemNum=10`, `queryCondition`有5个元素`(0,2,4,6,8)`；
另一个线程组的`selectTasks`方法的参数：`taskItemNum=10`, `queryCondition`有5个元素（1,3,5,7,9）。
如果有10个线程组，则每组线程只会获取到1个任务片。这时`selectTasks`方法的参数：`taskItemNum=10`, `queryCondition`只有一个元素，对应0到9中的一个。

执行期间和时间的修改功能：

1.   在创建任务和修改任务的时候，有两个属性(执行开始时间，执行结算时间)用于控制任务的执行时间；
2.   时间格式遵循标准的cron格式，还增强了原来不支持的倒数第几天的能力；
3.   当时间到底开始时间的时候，就开始执行任务，到达结束时间则终止调度（不管是否所有的任务都处理完）。
     如果没有设置执行结束时间。则一直运行，直到`selectTasks`返回的记录数为0，就终止执行。等待下个开始运行时间在启动。
4.   如果要动态修改任务的执行时间区间，则先 点击“暂停”按钮，等所有的服务器都停止完毕（大概需要几秒时间）。
     当再次单击任务，出现如下情形表示停止完毕。然后修改执行开始时间，执行结算时间。在恢复任务调度，就可以实现调度时间的修改。
     
任务处理的问题：

1. `Schedule`主要是提供任务调度的分配管理。每一个任务是否执行成功，是通过业务方的bean来实现的。
2. 你需求的例子，我理解的解决方案如下：
  1. 你从云梯拉下来100万数据放到保险应用的数据库中。这个表中有两个关键字段`USER_ID`和`STS`(状态：0-未发送，1-已发送)
  2. 在bean的`selectTasks`方法的查询sql中除了根据任务进行分片外，还需要增加状态条件。例如，`USER_ID % 10 in( ?,?,?) AND sts =0`。
  3. 在bean的execute方法中，在发送完消息后，你还需要修改数据状态`update table STS =1 where USER_ID =? `。这样下次就不会取到这条数据了。
  4. 这样就可以保障机器重新启动后，也不会出现问题。可以参考`DBDemoSingle.java`的实现模式。使用的接口应该是`IScheduleTaskDealSingle`。
     如果旺旺的接口支持批量发送消息的时候，才需要使用`IScheduleTaskDealMulti`接口。

## 6.使用步骤

### 6.1.搭建zookeeper集群。

### 6.2.搭建tbschedule控制台。
修改`src/test/resources/schedule.xml`中的配置信息指向已经启动的zookeeper服务器。为了避免不同应用任务类型间冲突，`rootPath`尽量全局唯一。

```xml
<bean id="scheduleManagerFactory" class="com.taobao.pamirs.schedule.TBScheduleManagerFactory" init-method="init">
    <property name="zkConfig">
        <map>
            <entry key="zkConnectString" value="your_ip:2181" />
            <entry key="rootPath" value="/your_app/app1" />
            <entry key="zkSessionTimeout" value="3000" />
            <entry key="userName" value="userName" />
            <entry key="password" value="password" />
        </map>
    </property>	
</bean>
```

配置Web服务器: 将`console/ScheduleConsole.war`拷贝到你自己的Web服务器中运行即可。因为没有做仔细的兼容性测试，建议使用IE8。

启动浏览器`http://localhost/index.jsp?manager=true`通过Console来检查配置数据是否正确。

### 6.3将自己的应用接入到tbschedule中

#### 添加zookeeper依赖
```xml
<dependency>
    <groupId>org.apache.zookeeper</groupId>
    <artifactId>zookeeper</artifactId>
    <version>3.4.6</version>
</dependency>
```

#### 添加tbschedule依赖
```xml
<dependency>
    <groupId>com.taobao.pamirs.schedule</groupId>
    <artifactId>tbschedule</artifactId>
    <version>3.2.18</version>
</dependency>
```

#### 将应用的bean列表加载到`tbscheduleManagerFactory`
1. 加载zk连接配置；
2. 新建`ScheduleInitUtil`类，并实现`InitializingBean`, `ApplicationContextAware`；
3. 将`bean`列表加载至`tbscheduleManagerFactory`；
```java
public void afterPropertiesSet() throws Exception {
    Properties p = getProperties(configInfo);
    tbscheduleManagerFactory = new TBScheduleManagerFactory();
    tbscheduleManagerFactory.setApplicationContext(applicationcontext);
    tbscheduleManagerFactory.init(p);
    tbscheduleManagerFactory.setZkConfig(convert(p));
    logger.warn("TBBPM 成功启动schedule调度引擎 ...");
}
```

#### 实现自己应用的任务示例
```java
@Component("demoTaskBean")
public class DemoTaskBean  extends IScheduleTaskDealSingle<SubDetailDO>{

  /*实现自己的业务查询*/
	public List<SubDetailDO> selectTasks(String taskParameter, String ownSign,
			int taskItemNum, List<TaskItemDefine> taskItemList,
			int eachFetchDataNum) throws Exception {
		try { 
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date()); 
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			List<SubDetailDO> details = null;
			
				details = subDetailDAO.selectForSchedule(
						getScopeByQueueCondition(taskItemNum, taskItemList),
						confirmTypes, DETAIL_STATUS_ONE, eachFetchDataNum);
			return details;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		}

	}
	
	/*处理自己的业务*/
	public boolean execute(SubDetailDO subDetail, String ownSign)
			throws Exception {
		try {
			yourProcess.process(subDetail);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e); 
			return false;
		}
	}
```

#### 向zookeeper添加配置调度任务数据,或者通过控制台添加任务和调度策略

#### 启动调度服务器。
如果看到类似日志信息，则表示成功：

```
[2012-01-30 16:50:33,098] [DemoTask$PRE-2-exe0] (DemoTaskBean.java:58) INFO  com.taobao.pamirs.schedule.test.DemoTaskBean - 处理任务[PRE]:39971971893
[2012-01-30 16:50:33,098] [DemoTask-0-exe0] (DemoTaskBean.java:58) INFO  com.taobao.pamirs.schedule.test.DemoTaskBean - 处理任务[BASE]:79970840269
[2012-01-30 16:50:33,098] [DemoTask$PRE-3-exe1] (DemoTaskBean.java:58) INFO  com.taobao.pamirs.schedule.test.DemoTaskBean - 处理任务[PRE]:49993262139
[2012-01-30 16:50:33,114] [DemoTask$TEST-4-exe0] (DemoTaskBean.java:58) INFO  com.taobao.pamirs.schedule.test.DemoTaskBean - 处理任务[TEST]:59954542534
[2012-01-30 16:50:33,114] [DemoTask$TEST-5-exe1] (DemoTaskBean.java:58) INFO  com.taobao.pamirs.schedule.test.DemoTaskBean - 处理任务[TEST]:70033832131
[2012-01-30 16:50:33,114] [DemoTask-1-exe1] (DemoTaskBean.java:58) INFO  com.taobao.pamirs.schedule.test.DemoTaskBean - 处理任务[BASE]:90016724177
```

#### 在Console中检查服务器运行情况。
可以通过Console来维护调度任务和调度策略的配置。
