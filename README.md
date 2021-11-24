# 阿里云版RocketMQ（ONS）组件

* 支持多生产者&消费者配置
* 支持多类型消息生产者&消费者配置
* 支持HTTP模式和TCP模式

！！！建议生产环境使用TCP模式。

## 说明

阿里云ONS客户端分为TCP接入点模式和HTTP接入点模式，不同接入点模式使用的客户端不同。 TCP模式只有在公网实例可以在外网使用，否则只能在阿里云内网使用。 HTTP模式则在公网内网都可以使用。
消息分为普通，延时，定时，顺序和事务消息。不同的消息，在不同的接入点方式下，使用方式不同。 此组件依赖线程池，将使用Spring异步线程池，所以需要在项目中进行如下配置:

```text
    //必须指定Bean名称为taskExecutor或使用@Primary
    @Bean("taskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(32);
        executor.setMaxPoolSize(128);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("Async task:");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
```

在配置TCP和HTTP的生产者消费者时，可以配置单独的阿里云AK和SK，如果有单独配置的AK和SK，则优先使用这个子配置，如果没有，则使用default-access配置的AK和SK，但是注意这2个配置不能同时为空。

在配置订阅列表时，注意，在相同的group下订阅同一个topic时，不能对这个topic配置订阅多次，如配置了2条针对A topic的订阅，只是tags一个为a，一个为b，应该合并为一条tags为"a,b"的订阅。

只有在配置文件中设置enable为true时，组件才会自动配置。

## GROUP,TOPIC和TAG

GROUP既可以用于生产者，也可以用于消费者，作为业务分组。      
作为消费者时，同一个GROUP下的消费者订阅消息的行为必须一致，即保证同一个GROUP下的消费者订阅的TOPIC和TAG必须相同。          
作为生产者时，需要注意，事务消息的Group ID不能与其他类型消息的Group ID共用！顺序消息的GROUP ID最好也和普通消息不要共用。一个GROUP ID最好只对应一种消息类型的TOPIC。    
TAG可用于同一个TOPIC下的消息过滤。

## TCP模式

TCP模式下，SDK接入较为简单，可以直接使用SDK的能力。

使用HTTP模式进行消息发送和消费引入以下依赖：

```xml

<dependency>
    <groupId>io.github.yanglong</groupId>
    <artifactId>ons-tcp-spring-boot-starter</artifactId>
    <!-- 版本根据需要选择 -->
    <version>${version}</version>
</dependency>
```

### 生产者

生产者主要生成3种生产者实例：Producer，OrderProducer，TransactionProducer然后使用其进行对应的消息发送。
其中Producer可以进行普通消息，延时消息，定时消息的发送。OrderProducer可以进行顺序消息发送。TransactionProducer可以进行事务消息发送。      
启用TCP接入点发送者，进行如下配置：

```yaml
ali-ons:
  enable: true
  default-access:
    accessKey: ${ONS_AK}
    secretKey: ${ONS_SK}
  tcp:
    producer:
      enable: true
      producers:
        normal:
          instanceName: ${spring.application.name}
          timeout: 3000
          #消息类型，不同的消息类型生成不同的生产者
          msgType: NORMAL
          #TCP接入点
          nameServer: ${ONS_NAME_SERVER}
        order:
          instanceName: ${spring.application.name}
          timeout: 2000
          #消息类型，不同的消息类型生成不同的生产者
          msgType: ORDER
          #TCP接入点
          nameServer: ${ONS_NAME_SERVER}
          #group Id-必填
          group: GID_TCP_ORDER_DEV
        trans:
          instanceName: ${spring.application.name}
          timeout: 6000
          #消息类型，不同的消息类型生成不同的生产者
          msgType: TRANSACTION
          #TCP接入点
          nameServer: ${ONS_NAME_SERVER}
          #group Id-必填
          group: GID_TCP_TRANS_DEV
          transChecker: io.github.yanglong.ons.tcp.sample.TransactionCheckerImpl
```

使用以上配置，会生成3个消息生产者实例：

```txt
normal->普通消息生产者
order->顺序消息生产者
trans->事务消息生产者
```

使用以上消息生产者进行消息发送：

```text
    @Autowired
    private TcpSender tcpSender;
    ...
    
    //normal->普通消息生产者 发送普通消息
    tcpSender.sendMsg("normal", "normal_topic", "", "id_1", "普通消息");
    tcpSender.sendAsyncMsg("normal", "normal_topic", "", "id_1", "普通消息", new SendCallback() {
        @Override
        public void onSuccess(SendResult sendResult) {
            log.info("normal async result:{}", sendResult);
        }

        @Override
        public void onException(OnExceptionContext context) {
            log.info("normal async send error!", context.getException());
        }
    });
    
    //order->顺序消息生产者 发送顺序消息
    tcpSender.sendOrderMsg("order","order_topic","","id_1","order_trade","顺序消息");
    
    //trans->事务消息生产者 发送事务消息
    tcpSender.sendTransactionMsg("trans", new LocalTransactionExecuter() {
        @Override
        public TransactionStatus execute(Message msg, Object arg) {
            //执行本地事务或进行本地事务执行状态检查，返回事务状态
            return TransactionStatus.CommitTransaction;
        }
    },"trans_topic","","order_1","事务消息","自定义参数");
    
    //normal->普通消息生产者 发送延迟消息
    tcpSender.sendDelayMsg("normal", "delay_topic", "", "id_1", "10秒延迟消息",10000);
    
    //normal->普通消息生产者 发送定时消息
    tcpSender.sendTimeMsg("normal", "time_topic", "", "id_1", "当前日期10后发送的消息",System.currentTimeMillis()+10000);
```

### 消费者

TCP接入方式消费模式可以分为3种方式，普通，顺序，批量，其中普通和批量配置相似，都是NORMAL消息类型，不同的是批量需要batchEnable为true。
TCP消费者启动后自动进行消息消费，使用者只需要实现OnsBatchMessageListener，OnsNormalMessageListener，OnsOrderMessageListener进行消息处理，并将实现类注入Spring容器，
然后在配置处将实现类全限定名配置到对应的listener处。一个消费者，可以订阅多个topic，配置时可以将同一个接入点的同种消费方式的订阅配置到一起。 示例配置如下：

```yaml
ali-ons:
  enable: true
  default-access:
    accessKey: ${ONS_AK}
    secretKey: ${ONS_SK}
  tcp:
    consumer:
      enable: true
      #消费者配置，同一个接入点的订阅，按NORMAL（包括了普通，事务，延时，定时，批量）和ORDER分别全部配置到一起，方便管理
      consumers:
        #TCP普通消费者，包括事务，定时，延时
        normal:
          #消息类型，消费端忽略
          msgType: NORMAL
          #消费者group
          group: GID_TCP_NORMAL_DEV
          #TCP
          nameServer: ${ONS_NAME_SERVER}
          #可选，同一个接入点的最好配置且同一个
          instanceName: ${spring.application.name}
          #消费线程数，使用默认4
          threadNums: "4"
          #订阅关系列表
          subscriptions:
            - topic: normal_dev
              tags: a
              #tcp模式下消息处理类
              listener: io.github.yanglong.ons.tcp.sample.TcpNormalListener
            - topic: time_dev
              #tcp模式下消息处理类
              listener: io.github.yanglong.ons.tcp.sample.TcpNormalListener
            - topic: trans_dev
              #tcp模式下消息处理类
              listener: io.github.yanglong.ons.tcp.sample.TcpNormalListener
        #TCP顺序消费者
        order:
          #消息类型，消费端忽略
          msgType: ORDER
          group: GID_TCP_ORDER_DEV
          #TCP接入点
          nameServer: ${ONS_NAME_SERVER}
          instanceName: ${spring.application.name}
          #消费线程数，使用默认4
          threadNums: "4"
          #订阅关系列表
          subscriptions:
            #分区顺序消息
            - topic: order_sharding_dev
              tags: s
              #tcp模式下消息处理类
              listener: io.github.yanglong.ons.tcp.sample.TcpOrderListener
            #全局顺序消息
            - topic: order_global_dev
              #tcp模式下消息处理类
              listener: io.github.yanglong.ons.tcp.sample.TcpOrderListener
        #TCP批量消费者
        batch:
          #消息类型，消费端忽略
          msgType: NORMAL
          #设置为批量
          batchEnable: true
          group: GID_TCP_BATCH_DEV
          #TCP接入点
          nameServer: ${ONS_NAME_SERVER}
          instanceId:
          instanceName: ${spring.application.name}
          #仅当批量消费生效
          batchConsumeMaxAwaitDurationInSeconds: 5
          #仅当批量消费生效
          consumeMessageBatchMaxSize: 3
          #消费线程数，使用默认4
          threadNums: "4"
          #订阅关系列表
          subscriptions:
            - topic: normal_dev
              tags: batch
              #tcp模式下消息处理类
              listener: io.github.yanglong.ons.tcp.sample.TcpBatchListener
```

## HTTP模式

HTTP模式相对于TCP模式，消费时只有一种模式，就是主动拉取。HTTP模式下，可以共用连接，因此，当权限和接入点配置相同时，将只生成一个连接实例。

使用HTTP模式进行消息发送和消费引入以下依赖：

```xml

<dependency>
    <groupId>io.github.yanglong</groupId>
    <artifactId>ons-http-spring-boot-starter</artifactId>
    <!-- 版本根据需要选择 -->
    <version>${version}</version>
</dependency>
```

### 生产者

HTTP模式下，生产者消息类型分为NORMAL和TRANSACTION两种，前者包含普通，顺序，延时，定时消息。 配置如下:

```yaml
ali-ons:
  enable: true
  default-access:
    accessKey: ${ONS_AK}
    secretKey: ${ONS_SK}
  http:
    producer:
      enable: true
      producers:
        normal:
          access:
            accessKey: ${ONS_AK}
            secretKey: ${ONS_SK}
          instanceName: ${spring.application.name}
          timeout: 6000
          #消息类型，不同的消息类型生成不同的生产者
          msgType: NORMAL
          #接入点
          nameServer: ${ONS_NAME_SERVER}
        trans:
          instanceName: ${spring.application.name}
          timeout: 6000
          #消息类型，不同的消息类型生成不同的生产者
          msgType: TRANSACTION
          #接入点
          nameServer: ${ONS_NAME_SERVER}
          #group Id
          group: GID_HTTP_TRANS_DEV
          httpTransChecker: io.github.yanglong.ons.http.sample.HalfMsgStatusCheckerImpl
```

以上配置将会生成2个HTTP生产者实例，一个NORMAL消息类型的生产者normal，一个TRANSACTION消息类型的生产者trans。 使用如下：

```text
    @Autowired
    private HttpSender httpSender;
    ...
    
    //使用生产者normal发送普通类消息
    httpSender.sendMsg("normal","normal_topic","","id_1","普通消息");
    httpSender.sendOrderMsg("normal","order_topic","","1","id_1","顺序消息");
    httpSender.sendDelayMsg("normal","delay_topic","","id_1","延迟消息",10000);
    httpSender.sendTimeMsg("normal","time_topic","","id_1","定时消息",System.currentTimeMillis()+10000);
    //使用生产者trans发送事务消息
    String receiptHandle = httpSender.sendTransactionMsg("trans","trans_topic","","id_1","事务消息",null);
    httpSender.commitMsg("trans", "trans_topic", receiptHandle);
    
```

### 消费者

HTTP消费者使用拉模式进行消息消费，必须配置group和topic，对于消息处理，业务只需要实现HttpMessageListener，并将相关实现类的全限定名配置到订阅列表的listener处。消息消费在启动时即自动生成消费者进行消费。
关键参数为threadNums，msgType。threadNums参数决定了启用多少个线程进行消费，默认值为4；msgType决定了消费何种消息，此参数只能配置NORMAL或ORDER，默认值为NORMAL。 配置如下：

```yaml
ali-ons:
  enable: true
  default-access:
    accessKey: ${ONS_AK}
    secretKey: ${ONS_SK}
  http:
    consumer:
      enable: true
      #消费者配置，同一个接入点的订阅，按NORMAL（包括了普通，事务，延时，定时，批量）和ORDER分别全部配置到一起，方便管理
      consumers:
        #普通消费者，包括事务，定时，延时
        normal:
          #消息类型，消费端忽略
          msgType: NORMAL
          #消费者group
          group: GID_HTTP_NORMAL_DEV
          #TCP
          nameServer: ${ONS_NAME_SERVER}
          #可选，同一个接入点的最好配置且同一个
          instanceName: ${spring.application.name}
          #消费线程数，使用默认4
          threadNums: "4"
          #订阅关系列表
          subscriptions:
            - topic: normal_dev
              #OnsMessageListener实现类名
              listener: io.github.yanglong.ons.http.sample.HttpConsumeListener
            - topic: trans_dev
              #OnsMessageListener实现类名
              listener: io.github.yanglong.ons.http.sample.HttpConsumeListener
            - topic: time_dev
              tags: a
              #OnsMessageListener实现类名
              listener: io.github.yanglong.ons.http.sample.HttpConsumeListener
        #顺序消费者
        order:
          #消息类型，消费端忽略
          msgType: ORDER
          group: GID_HTTP_ORDER_DEV
          #接入点
          nameServer: ${ONS_NAME_SERVER}
          instanceName: ${spring.application.name}
          #消费线程数，使用默认4
          threadNums: "4"
          #订阅关系列表
          subscriptions:
            - topic: order_sharding_dev
              #OnsMessageListener实现类名
              listener: io.github.yanglong.ons.http.sample.HttpConsumeListener
            - topic: order_global_dev
              #OnsMessageListener实现类名
              listener: io.github.yanglong.ons.http.sample.HttpConsumeListener
```

注意HTTP模式下，顺序消息消费者会收到订阅的topic所有的消息，消息处理类需要自行获取消息中的shardingKey来进行选择性消费。

## sample示例项目说明

运行示例项目，需要先进行配置创建。注意TCP模式只能在阿里云内网运行，也就是只能在阿里云产品上运行。HTTP模式则无限制。

### topic创建

创建以下topic：

|名称|消息类型|
|:---|:---:|
|order_global_dev|全局顺序消息|
|order_sharding_dev|分区顺序消息|
|time_dev|定时/延时消息|
|trans_dev|事务消息|
|normal_dev|普通消息|

### group创建

TCP模式GROUP ID创建：

|名称|用途|
|:---|:---:|
|GID_TCP_BATCH_DEV|TCP批量消息测试|
|GID_TCP_NORMAL_DEV|TCP普通消息测试|
|GID_TCP_TRANS_DEV|TCP事务消息测试|
|GID_TCP_ORDER_DEV|TCP顺序消息测试|

HTTP模式GROUP ID创建：

|名称|用途|
|:---|:---:|
|GID_HTTP_NORMAL|普通消息测试|
|GID_HTTP_ORDER_DEV|顺序消息测试|
|GID_HTTP_NORMAL_DEV|普通消息测试|
|GID_HTTP_TRANS_DEV|事务消息测试|

### 环境变量配置

需要配置以下环境变量：

```txt
ONS_NAME_SERVER=TCP或HTTP接入点，在控制台实例详情，接入点处获取
ONS_AK=access_key 账号AccessKey管理处生成
ONS_SK=secret_key 账号AccessKey管理处生成
```

配置完成后即可部署运行。 项目使用knife4j作为API文档输出，启动成功后访问127.0.0.1:8080/doc.html即可访问消息发送页面。

## 部署maven central

教程: https://blog.csdn.net/qq_41973594/article/details/119791466

部署：https://central.sonatype.org/publish/publish-guide/#deployment

gpg插件使用：https://maven.apache.org/plugins/maven-gpg-plugin/usage.html

