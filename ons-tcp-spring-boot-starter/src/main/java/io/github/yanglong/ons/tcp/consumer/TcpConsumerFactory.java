package io.github.yanglong.ons.tcp.consumer;

import com.aliyun.openservices.ons.api.Admin;
import com.aliyun.openservices.ons.api.Consumer;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.batch.BatchConsumer;
import com.aliyun.openservices.ons.api.order.OrderConsumer;
import io.github.yanglong.ons.commons.factory.OnsFactory;
import io.github.yanglong.ons.commons.listener.OnsMessageListener;
import io.github.yanglong.ons.commons.properties.MessageType;
import io.github.yanglong.ons.commons.properties.OnsAccessProperties;
import io.github.yanglong.ons.commons.properties.OnsConsumerProperties;
import io.github.yanglong.ons.commons.properties.OnsSubscriptionProperties;
import io.github.yanglong.ons.commons.utils.OnsContextAware;
import io.github.yanglong.ons.commons.utils.OnsStringUtils;
import io.github.yanglong.ons.tcp.AdminUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description:消费者实例则在此处进行启动消费，HTTP消费端线程使用TaskExecutor来执行。因此必须配置名称为taskExecutor的TaskExecutor！
 * 使用配置文件进行消息订阅的方式，同一个实例同一种消息类型的订阅合并，因此无法对单个订阅关系进行维护，实际可以通过instanceName进行底层共享连接。
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
@Slf4j
public class TcpConsumerFactory implements OnsFactory {
    /**
     * 容器工具类，用于获取容器中listener，构造传入
     */
    private final OnsContextAware onsContextAware;
    /**
     * 默认认证信息，构造传入
     */
    private final OnsAccessProperties defaultAccessProperties;
    /**
     * 消费者名称实例键值容器
     */
    private final Map<String, Admin> consumerContainer = new ConcurrentHashMap<>(16);
    /**
     * 维护一个消费者配置列表用于管理,构造传入
     */
    private final Map<String, TcpConsumerProperties> consumerProperties;

    public TcpConsumerFactory(OnsContextAware onsContextAware, OnsAccessProperties defaultAccessProperties, Map<String, TcpConsumerProperties> consumerProperties) {
        this.onsContextAware = onsContextAware;
        this.defaultAccessProperties = defaultAccessProperties;
        this.consumerProperties = consumerProperties;
    }

    /**
     * 初始化TCP消费者，并启动消费
     */
    @Override
    public void init() {
        if (CollectionUtils.isEmpty(consumerProperties)) {
            log.error("ons TCP consumer property is empty,can't init TCP consumers.");
            return;
        }
        //根据消息类型归类，分为普通，批量，还有顺序
        List<OnsConsumerProperties> normalList = new ArrayList<>();
        List<OnsConsumerProperties> batchList = new ArrayList<>();
        List<OnsConsumerProperties> orderList = new ArrayList<>();
        consumerProperties.forEach((name, prop) -> {
            prop.setConfigName(name);
            MessageType messageType = prop.getMsgType();
            if (MessageType.ORDER.equals(messageType)) {
                orderList.add(prop);
            } else {
                //除了顺序消息消费者，全部作为普通消费者
                if (prop.isBatchEnable()) {
                    //批量消息
                    batchList.add(prop);
                } else {
                    //普通，事务，延迟和定时消息
                    normalList.add(prop);
                }
            }
        });
        //创建各个消费者，启动并放入容器
        normalList.forEach(p -> {
            Properties mqProperties = getConsumerProperty(defaultAccessProperties, p);
            if (null != mqProperties) {
                Consumer consumer = createConsumer(mqProperties, p.getSubscriptions(), onsContextAware);
                consumer.start();
                consumerContainer.put(p.getConfigName(), consumer);
            } else {
                log.error("can't create normal consumer {},mq properties is null!", p.getConfigName());
            }
        });
        batchList.forEach(p -> {
            Properties mqProperties = getConsumerProperty(defaultAccessProperties, p);
            if (null != mqProperties) {
                BatchConsumer consumer = createBatchConsumer(mqProperties, p.getSubscriptions(), onsContextAware);
                consumer.start();
                consumerContainer.put(p.getConfigName(), consumer);
            } else {
                log.error("can't create batch consumer {},mq properties is null!", p.getConfigName());
            }
        });
        orderList.forEach(p -> {
            Properties mqProperties = getConsumerProperty(defaultAccessProperties, p);
            if (null != mqProperties) {
                OrderConsumer consumer = createOrderConsumer(mqProperties, p.getSubscriptions(), onsContextAware);
                consumer.start();
                consumerContainer.put(p.getConfigName(), consumer);
            } else {
                log.error("can't create order consumer {},mq properties is null!", p.getConfigName());
            }
        });
    }

    /**
     * 创建普通、事务、延时、定时消息消费者
     *
     * @param mqProperty             MQ消费客户端配置
     * @param subscriptionProperties 订阅关系配置
     * @return ConsumerBean
     */
    private Consumer createConsumer(Properties mqProperty, List<OnsSubscriptionProperties> subscriptionProperties, OnsContextAware contextAware) {
        Consumer consumer = ONSFactory.createConsumer(mqProperty);
        subscriptionProperties.forEach(s -> {
            Class<OnsMessageListener> clazz = s.getListener();
            String topic = s.getTopic();
            if (StringUtils.isNotEmpty(topic) && null != clazz && TcpNormalMessageListener.class.isAssignableFrom(clazz)) {
                TcpNormalMessageListener listener = (TcpNormalMessageListener) contextAware.getBean(clazz);
                String tags = s.getTags();
                tags = OnsStringUtils.stringReplace(tags, OnsStringUtils.COMMA, "||");
                consumer.subscribe(s.getTopic(), tags, listener);
            } else {
                log.error("can't create normal consumer,the topic or listener is empty!");
            }
        });
        return consumer;
    }

    /**
     * 创建批量消费者。条件时消息类型为NORMAL且batchEnable=true
     *
     * @param mqProperty             MQ消费客户端配置
     * @param subscriptionProperties 订阅关系配置
     * @return BatchConsumerBean
     */
    private BatchConsumer createBatchConsumer(Properties mqProperty, List<OnsSubscriptionProperties> subscriptionProperties, OnsContextAware contextAware) {
        BatchConsumer consumer = ONSFactory.createBatchConsumer(mqProperty);
        subscriptionProperties.forEach(s -> {
            Class<OnsMessageListener> clazz = s.getListener();
            String topic = s.getTopic();
            if (StringUtils.isNotEmpty(topic) && null != clazz && TcpBatchMessageListener.class.isAssignableFrom(clazz)) {
                TcpBatchMessageListener listener = (TcpBatchMessageListener) contextAware.getBean(clazz);
                String tags = s.getTags();
                tags = OnsStringUtils.stringReplace(tags, OnsStringUtils.COMMA, "||");
                consumer.subscribe(topic, tags, listener);
            } else {
                log.error("can't create batch consumer,the topic or listener is empty!");
            }
        });
        return consumer;
    }

    /**
     * 创建顺序消息消费者，如果mqProperty中tag为空，返回空
     *
     * @param mqProperty             MQ消费客户端配置
     * @param subscriptionProperties 订阅关系配置
     * @return OrderConsumerBean
     */
    private OrderConsumer createOrderConsumer(Properties mqProperty, List<OnsSubscriptionProperties> subscriptionProperties, OnsContextAware contextAware) {
        OrderConsumer consumer = ONSFactory.createOrderedConsumer(mqProperty);
        subscriptionProperties.forEach(s -> {
            Class<OnsMessageListener> clazz = s.getListener();
            String topic = s.getTopic();
            if (StringUtils.isNotEmpty(topic) && null != clazz && TcpOrderMessageListener.class.isAssignableFrom(clazz)) {
                TcpOrderMessageListener listener = (TcpOrderMessageListener) contextAware.getBean(clazz);
                String tags = s.getTags();
                tags = OnsStringUtils.stringReplace(tags, OnsStringUtils.COMMA, "||");
                consumer.subscribe(s.getTopic(), tags, listener);
            } else {
                log.error("can't create order consumer,the topic or listener is empty!");
            }
        });
        return consumer;
    }

    /**
     * 从配置对象中获取MQ实例对象所需参数
     *
     * @param accessProperties   验证配置
     * @param consumerProperties 订阅配置
     * @return 配置对象，NULL-未通过验证
     */
    private Properties getConsumerProperty(OnsAccessProperties accessProperties, OnsConsumerProperties consumerProperties) {
        String ak = accessProperties.getAccessKey();
        String sk = accessProperties.getSecretKey();
        OnsAccessProperties custom = consumerProperties.getAccess();
        if (OnsStringUtils.checkAccess(custom)) {
            ak = custom.getAccessKey();
            sk = custom.getSecretKey();
        }
        String nameServer = consumerProperties.getNameServer();
        String group = consumerProperties.getGroup();
        Properties properties = null;
        if (OnsStringUtils.isAnyEmpty(ak, sk, nameServer, group)) {
            log.error("ONS consumer config properties can't init.because init parameter can't resolved!");
        } else {
            properties = new Properties();
            //必要配置
            properties.put(PropertyKeyConst.GROUP_ID, group);
            properties.put(PropertyKeyConst.AccessKey, ak);
            properties.put(PropertyKeyConst.SecretKey, sk);
            properties.put(PropertyKeyConst.NAMESRV_ADDR, nameServer);
            properties.put(PropertyKeyConst.ConsumeTimeout, consumerProperties.getConsumeTimeout());
            //可选配置
            properties.put(PropertyKeyConst.ConsumeThreadNums, consumerProperties.getThreadNums());
            properties.put(PropertyKeyConst.INSTANCE_ID, consumerProperties.getInstanceId());
            properties.put(PropertyKeyConst.InstanceName, consumerProperties.getInstanceName());
            properties.put(PropertyKeyConst.MaxReconsumeTimes, consumerProperties.getMaxReconsumeTimes());
            //批量消费配置
            if (MessageType.NORMAL.equals(consumerProperties.getMsgType())) {
                properties.put(PropertyKeyConst.BatchConsumeMaxAwaitDurationInSeconds, consumerProperties.getBatchConsumeMaxAwaitDurationInSeconds());
                properties.put(PropertyKeyConst.ConsumeMessageBatchMaxSize, consumerProperties.getConsumeMessageBatchMaxSize());
            }
            if (MessageType.ORDER.equals(consumerProperties.getMsgType())) {
                //顺序消息配置
                properties.put(PropertyKeyConst.SuspendTimeMillis, consumerProperties.getSuspendTimeMillis());
            }
        }
        return properties;
    }

    /**
     * 关闭特定名称的consumer
     *
     * @param name 名称
     */
    private void closeConsumer(String name) {
        Admin admin = consumerContainer.get(name);
        AdminUtils.closeInstance(admin);
    }

    /**
     * 关闭所有consumer
     */
    @Override
    public void shutdown() {
        consumerContainer.values().forEach(AdminUtils::closeInstance);
    }

    /**
     * 创建并启动消费者
     *
     * @param accessProperties   验证配置
     * @param consumerProperties 订阅配置
     * @param override           true-覆盖已有消费者，false-不覆盖，如果已有，不建新的
     * @return true-执行了生成，false未执行
     */
    private boolean createAndStart(OnsAccessProperties accessProperties, OnsConsumerProperties consumerProperties, boolean override) {
        boolean created = false;
        //获取配置
        Properties properties = getConsumerProperty(accessProperties, consumerProperties);
        if (null != properties) {
            String name = consumerProperties.getConfigName();
            Admin old = consumerContainer.get(name);
            //判断消息类型-根据类型创建消费者放入容器并启动
            MessageType messageType = consumerProperties.getMsgType();
            if (null == old || override) {
                Admin fresh;
                List<OnsSubscriptionProperties> subscriptions = consumerProperties.getSubscriptions();
                if (MessageType.ORDER.equals(messageType)) {
                    fresh = createOrderConsumer(properties, subscriptions, onsContextAware);
                } else {
                    //除了顺序消息消费者，全部作为普通消费者
                    if (consumerProperties.isBatchEnable()) {
                        //批量消息
                        fresh = createBatchConsumer(properties, subscriptions, onsContextAware);
                    } else {
                        //普通，事务，延迟和定时消息
                        fresh = createConsumer(properties, subscriptions, onsContextAware);
                    }
                }
                AdminUtils.startInstance(fresh);
                consumerContainer.put(name, fresh);
                //新的成功以后才关闭旧的
                if (null != old) {
                    AdminUtils.closeInstance(old);
                }
                created = true;
            }
        }
        return created;
    }


}
