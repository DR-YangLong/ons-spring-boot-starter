package io.github.yanglong.ons.tcp.producer;

import com.aliyun.openservices.ons.api.Admin;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionProducer;
import io.github.yanglong.ons.commons.factory.OnsFactory;
import io.github.yanglong.ons.commons.properties.ClientType;
import io.github.yanglong.ons.commons.properties.MessageType;
import io.github.yanglong.ons.commons.properties.OnsAccessProperties;
import io.github.yanglong.ons.commons.utils.OnsContextAware;
import io.github.yanglong.ons.commons.utils.OnsStringUtils;
import io.github.yanglong.ons.tcp.AdminUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: 在此处通过配置，生成各类消息的生产者实例，并将对应实例放入容器中，提供给消息发送封装客户端使用。
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
@Slf4j
public class TcpProducerFactory implements OnsFactory {
    /**
     * ONS安全配置类
     */
    private final OnsAccessProperties defaultAccessProperties;

    /**
     * 上下文工具，用于获取listener对象
     */
    private final OnsContextAware onsContextAware;

    /**
     * TCP生产者名称及其对应配置map
     */
    private final Map<String, TcpProducerProperties> clientProperties;

    /**
     * 普通消息，延时消息，定时消息生产者实例容器
     */
    private final Map<String, Producer> normalProducerContainer = new ConcurrentHashMap<>(8);

    /**
     * 顺序消息生产者实例容器
     */
    private final Map<String, OrderProducer> orderProducerContainer = new ConcurrentHashMap<>(8);

    /**
     * 事务消息生产者实例容器
     */
    private final Map<String, TransactionProducer> transactionContainer = new ConcurrentHashMap<>(8);

    /**
     * 生产者名称-类型映射
     */
    private final Map<String, MessageType> nameMap = new HashMap<>(16);

    public TcpProducerFactory(OnsAccessProperties defaultAccessProperties, OnsContextAware onsContextAware, Map<String, TcpProducerProperties> clientProperties) {
        this.defaultAccessProperties = defaultAccessProperties;
        this.onsContextAware = onsContextAware;
        this.clientProperties = clientProperties;
    }

    /**
     * 初始化TCP ons生产者
     */
    @Override
    public void init() {
        if (CollectionUtils.isEmpty(clientProperties)) {
            log.error("this is no TCP ons producer created!");
            return;
        }
        synchronized (TcpProducerFactory.class) {
            clientProperties.forEach((name, property) -> {
                String accessKey = defaultAccessProperties.getAccessKey();
                String secretKey = defaultAccessProperties.getSecretKey();
                OnsAccessProperties custom = property.getAccess();
                if (OnsStringUtils.checkAccess(custom)) {
                    accessKey = custom.getAccessKey();
                    secretKey = custom.getSecretKey();
                }
                property.setConfigName(name);
                switch (property.getMsgType()) {
                    case NORMAL: {
                        Producer producer = createNormalProducer(accessKey, secretKey, property.getTimeout(), property.getNameServer());
                        if (null != producer) {
                            normalProducerContainer.put(name, producer);
                            nameMap.put(name, MessageType.NORMAL);
                        }
                    }
                    break;
                    case ORDER: {
                        OrderProducer producer = createOrderProducer(accessKey, secretKey, property.getTimeout(), property.getNameServer(), property.getGroup());
                        if (null != producer) {
                            orderProducerContainer.put(name, producer);
                            nameMap.put(name, MessageType.ORDER);
                        }
                    }
                    break;
                    case TRANSACTION: {
                        Class<LocalTransactionChecker> clazz = property.getTransChecker();
                        if (LocalTransactionChecker.class.isAssignableFrom(clazz)) {
                            LocalTransactionChecker checkListener = onsContextAware.getBean(clazz);
                            TransactionProducer producer = createTransactionProducer(accessKey, secretKey, property.getTimeout(), property.getNameServer(), property.getGroup(), checkListener);
                            if (null != producer) {
                                transactionContainer.put(name, producer);
                                nameMap.put(name, MessageType.TRANSACTION);
                            }
                        } else {
                            log.error("init error!the class of property[ons.producers.{}.transChecker] must be implement LocalTransactionChecker!", name);
                        }
                    }
                    break;
                    default:
                }
            });
        }
    }

    /**
     * 重新生成生产者实例。会根据<code>properties</code>中的消息类型和<code>name</code>从已有容器中获取客户端实例。
     * 如果没有同名客户端，将生成新客户端。
     * 如果有同名客户端，根据override参数：如果为true，则用新配置生成客户端，然后替换旧客户端，同时停止旧客户端；如果为false，则不生成新客户端。
     *
     * @param properties producer配置
     * @param name       实例名
     * @return true-已重新生成，反之false
     */
    public boolean addOrReplaceProducer(@NotNull TcpProducerProperties properties, @NotEmpty String name, boolean override) {
        boolean build = false;
        ClientType type = properties.getType();
        if (ClientType.TCP.equals(type)) {
            OnsAccessProperties accessProperties = properties.getAccess();
            if (OnsStringUtils.checkAccess(accessProperties)) {
                MessageType msgType = properties.getMsgType();
                Admin admin = null;
                switch (msgType) {
                    case NORMAL: {
                        admin = normalProducerContainer.get(name);
                        if (null == admin || override) {
                            Producer producer = createNormalProducer(accessProperties.getAccessKey(), accessProperties.getSecretKey(), properties.getTimeout(), properties.getNameServer());
                            if (null != producer) {
                                normalProducerContainer.put(name, producer);
                                build = true;
                            }
                        }
                    }
                    break;
                    case ORDER: {
                        admin = orderProducerContainer.get(name);
                        if (null == admin || override) {
                            OrderProducer producer = createOrderProducer(accessProperties.getAccessKey(), accessProperties.getSecretKey(), properties.getTimeout(), properties.getNameServer(), properties.getGroup());
                            if (null != producer) {
                                orderProducerContainer.put(name, producer);
                                build = true;
                            }
                        }
                    }
                    break;
                    case TRANSACTION: {
                        admin = transactionContainer.get(name);
                        if (null == admin || override) {
                            LocalTransactionChecker checker = getTransactionCheckerBean(properties);
                            if (null != checker) {
                                TransactionProducer producer = createTransactionProducer(accessProperties.getAccessKey(), accessProperties.getSecretKey(), properties.getTimeout(), properties.getNameServer(), properties.getGroup(), checker);
                                if (null != producer) {
                                    transactionContainer.put(name, producer);
                                    build = true;
                                }
                            } else {
                                log.error("reCreate error!the class of property[ons.producers.{}.transChecker] must be implement LocalTransactionChecker!", name);
                            }
                        }
                    }
                    break;
                    default:
                        log.error("unknown producer msgType[{}] by name[{}].", msgType, name);
                }
                if (build) {
                    nameMap.put(name, msgType);
                    clientProperties.put(name, properties);
                    AdminUtils.closeInstance(admin);
                }
            } else {
                log.error("instance can't build.because access info is invalid!");
            }
        } else {
            log.error("HTTP instance can't build by TCP!");
        }
        return build;
    }

    /**
     * 获取生产者名称对应配置的消息类型
     *
     * @param name 生产者名称
     * @return 消息类型
     */
    public MessageType getClientMsgTypeByName(@NotEmpty String name) {
        return nameMap.get(name);
    }

    /**
     * 根据<code>name</code>获取普通消息，延时消息，定时消息，生产者实例
     *
     * @param name 名称，配置中map的key
     * @return 消息生产者/null
     */
    public Producer getNormalProducer(@NotEmpty String name) {
        Producer producer = null;
        if (MessageType.NORMAL.equals(this.getClientMsgTypeByName(name))) {
            producer = this.normalProducerContainer.get(name);
        }
        return producer;
    }

    /**
     * 根据<code>name</code>获取顺序消息生产者实例
     *
     * @param name 名称，配置中map的key
     * @return 顺序消息生产者/null
     */
    public OrderProducer getOrderProducer(@NotEmpty String name) {
        OrderProducer producer = null;
        if (MessageType.ORDER.equals(this.getClientMsgTypeByName(name))) {
            producer = this.orderProducerContainer.get(name);
        }
        return producer;
    }

    /**
     * 根据<code>name</code>获取事务消息生产者实例
     *
     * @param name 名称，配置中map的key
     * @return 事务消息生产者/null
     */
    public TransactionProducer getTransactionProducer(@NotEmpty String name) {
        TransactionProducer producer = null;
        if (MessageType.TRANSACTION.equals(this.getClientMsgTypeByName(name))) {
            producer = this.transactionContainer.get(name);
        }
        return producer;
    }

    /**
     * 总是创建新的client;创建普通消息生产者并启动，可用普通消息发送，延时，顺序消息发送。
     *
     * @param accessKey  身份验证标识
     * @param secretKey  身份验证密钥
     * @param timeout    发送超时时间
     * @param nameServer tcp连接点
     * @return tcp普通消息生产者实例
     */
    private Producer createNormalProducer(String accessKey, String secretKey, String timeout, String nameServer) {
        if (OnsStringUtils.isAnyEmpty(accessKey, secretKey, nameServer)) {
            log.error("MQ producer can't init,because init parameter can't resolved!");
            return null;
        }
        Properties properties = getClientProperties(accessKey, secretKey, timeout, nameServer);
        Producer producer;
        try {
            producer = ONSFactory.createProducer(properties);
            // 在发送消息前，必须调用start方法来启动Producer，只需调用一次即可。
            producer.start();
        } catch (Exception e) {
            producer = null;
            log.error("MQ producer can't init!", e);
        }
        return producer;
    }

    /**
     * 创建顺序消息生产者并启动
     *
     * @param accessKey  身份验证标识
     * @param secretKey  身份验证密钥
     * @param timeout    发送超时时间
     * @param nameServer tcp连接点
     * @param group      发送者group，此group不能和其他类型消息混用
     * @return 顺序消息生产者
     */
    private OrderProducer createOrderProducer(String accessKey, String secretKey, String timeout, String nameServer, String group) {
        if (OnsStringUtils.isAnyEmpty(accessKey, secretKey, nameServer, group)) {
            log.error("MQ order producer can't init,because init parameter can't resolve!");
            return null;
        }
        Properties properties = getClientProperties(accessKey, secretKey, timeout, nameServer);
        properties.put(PropertyKeyConst.GROUP_ID, group);
        OrderProducer producer;
        try {
            producer = ONSFactory.createOrderProducer(properties);
            // 在发送消息前，必须调用start方法来启动Producer，只需调用一次即可。
            producer.start();
        } catch (Exception e) {
            producer = null;
            log.error("MQ order producer can't init!", e);
        }
        return producer;
    }

    /**
     * 创建事务消息生产者并启动
     *
     * @param accessKey          身份验证标识
     * @param secretKey          身份验证密钥
     * @param timeout            发送超时时间
     * @param nameServer         tcp连接点
     * @param group              发送者group，此group不能和其他类型消息混用
     * @param transactionChecker 事务状态检查接口实现类
     * @return 事务消息生产者
     */
    private TransactionProducer createTransactionProducer(String accessKey, String secretKey, String timeout, String nameServer, String group, LocalTransactionChecker transactionChecker) {
        if (null == transactionChecker && OnsStringUtils.isAnyEmpty(accessKey, secretKey, nameServer, group)) {
            log.error("MQ transaction producer can't init,because init parameter can't resolve!");
            return null;
        }
        Properties properties = getClientProperties(accessKey, secretKey, timeout, nameServer);
        properties.put(PropertyKeyConst.GROUP_ID, group);
        TransactionProducer producer;
        try {
            producer = ONSFactory.createTransactionProducer(properties, transactionChecker);
            // 在发送消息前，必须调用start方法来启动Producer，只需调用一次即可。
            producer.start();
        } catch (Exception e) {
            producer = null;
            log.error("MQ transaction producer can't init!", e);
        }
        return producer;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * 关闭所有
     */
    @Override
    public void shutdown() {
        //循环所有容器，对容器中生产者调用shutdown
        if (!CollectionUtils.isEmpty(normalProducerContainer)) {
            normalProducerContainer.values().forEach(AdminUtils::closeInstance);
        }
        if (!CollectionUtils.isEmpty(orderProducerContainer)) {
            orderProducerContainer.values().forEach(AdminUtils::closeInstance);
        }
        if (!CollectionUtils.isEmpty(transactionContainer)) {
            transactionContainer.values().forEach(AdminUtils::closeInstance);
        }
    }

    /**
     * 获取通用连接properties
     *
     * @param accessKey  身份验证标识
     * @param secretKey  身份验证密钥
     * @param timeout    发送超时时间
     * @param nameServer tcp连接点
     * @return 通用的properties
     */
    private Properties getClientProperties(String accessKey, String secretKey, String timeout, String nameServer) {
        Properties properties = new Properties();
        // AccessKey ID阿里云身份验证，在阿里云服务器管理控制台创建。
        properties.put(PropertyKeyConst.AccessKey, accessKey);
        // AccessKey Secret阿里云身份验证，在阿里云服务器管理控制台创建。
        properties.put(PropertyKeyConst.SecretKey, secretKey);
        //设置发送超时时间，单位毫秒。
        properties.setProperty(PropertyKeyConst.SendMsgTimeoutMillis, timeout);
        // 设置TCP协议接入点，进入控制台的实例详情页面的TCP协议客户端接入点区域查看。
        properties.put(PropertyKeyConst.NAMESRV_ADDR, nameServer);
        return properties;
    }

    /**
     * 使用配置中LocalTransactionChecker的实现类全限定名获取Spring容器中对应的实例
     *
     * @param properties 事务生产者配置
     * @return LocalTransactionChecker
     */
    private LocalTransactionChecker getTransactionCheckerBean(@NotNull TcpProducerProperties properties) {
        LocalTransactionChecker checker = null;
        Class<LocalTransactionChecker> clazz = properties.getTransChecker();
        if (null != clazz && LocalTransactionChecker.class.isAssignableFrom(clazz)) {
            checker = onsContextAware.getBean(clazz);
        }
        return checker;
    }
}
