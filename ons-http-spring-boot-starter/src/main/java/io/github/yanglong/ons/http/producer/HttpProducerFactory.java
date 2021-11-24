package io.github.yanglong.ons.http.producer;

import com.aliyun.mq.http.MQClient;
import com.aliyun.mq.http.MQProducer;
import com.aliyun.mq.http.MQTransProducer;
import io.github.yanglong.ons.commons.properties.MessageType;
import io.github.yanglong.ons.commons.properties.OnsAccessProperties;
import io.github.yanglong.ons.commons.utils.OnsContextAware;
import io.github.yanglong.ons.commons.utils.OnsStringUtils;
import io.github.yanglong.ons.http.AbstractHttpClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;

import javax.validation.constraints.NotEmpty;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: HTTP方式生产客户端生成
 *
 * 生成通用的MQClient和放入缓存，当发送消息时，用topic,group获取发送客户端，进行发送，同时缓存此客户端。
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
@Slf4j
public class HttpProducerFactory extends AbstractHttpClientFactory {
    /**
     * 容器工具类，用于获取容器中listener，构造传入
     */
    private final OnsContextAware onsContextAware;
    /**
     * 线程池-用于提交消费线程，构造传入
     */
    private final TaskExecutor taskExecutor;
    /**
     * 消息生产者缓存
     */
    private final Map<String, MQProducer> producerContainer = new ConcurrentHashMap<>(16);

    /**
     * 管理事务状态确认线程
     */
    private final Map<String, HalfMsgConfirmRunnable> checkerContainer = new ConcurrentHashMap<>(16);

    public HttpProducerFactory(Map<String, MQClient> clients, OnsAccessProperties accessProperties, Map<String, HttpProducerProperties> clientProperties, OnsContextAware onsContextAware, TaskExecutor taskExecutor) {
        super(clients, accessProperties, clientProperties);
        this.onsContextAware = onsContextAware;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void shutdown() {
        //关闭事务检查
        checkerContainer.forEach((name, checker) -> {
            log.info("stop transaction confirm thread for HTTP producer {}.", name);
            checker.setShutdown(true);
        });
        super.shutdown();
    }

    /**
     * 获取指定名称的消息生产者
     *
     * @param name  生产者名称
     * @param topic topic
     * @return MQProducer
     */
    public MQProducer getProducer(String name, @NotEmpty String topic) {
        MQProducer producer = null;
        if (OnsStringUtils.isAllNotEmpty(name, topic)) {
            synchronized (this) {
                producer = producerContainer.computeIfAbsent(OnsStringUtils.generateKey(name, topic), key -> {
                    MQProducer mqProducer = null;
                    HttpProducerProperties properties = (HttpProducerProperties) commonProperties.get(name);
                    if (null != properties) {
                        String ak;
                        String sk;
                        OnsAccessProperties accessProperties = properties.getAccess();
                        if (null == accessProperties) {
                            ak = defaultAccessProperties.getAccessKey();
                            sk = defaultAccessProperties.getSecretKey();
                        } else {
                            ak = accessProperties.getAccessKey();
                            sk = accessProperties.getSecretKey();
                        }
                        MessageType messageType = properties.getMsgType();
                        if (MessageType.TRANSACTION.equals(messageType)) {
                            mqProducer = createTransactionProducer(ak, sk, properties.getNameServer(), properties.getInstanceId(), properties.getGroup(), topic);
                            Class<HalfMsgStatusChecker> checkerClass = properties.getHttpTransChecker();
                            if (null != checkerClass && HalfMsgStatusChecker.class.isAssignableFrom(checkerClass)) {
                                HalfMsgStatusChecker checker = onsContextAware.getBean(checkerClass);
                                this.applyChecker((MQTransProducer) mqProducer, name, checker, taskExecutor);
                            } else {
                                log.error("the HTTP transaction client {}，can't resolve HalfMsgStatusChecker,please confirm status in main thread by return receiptHandle.", name);
                            }
                        } else {
                            mqProducer = createProducer(ak, sk, properties.getNameServer(), properties.getInstanceId(), topic);
                        }
                        log.info("create http MQProducer {}:{}", name, mqProducer.toString());
                    }
                    return mqProducer;
                });
            }
        } else {
            log.error("HTTP sender name and topic must not empty!");
        }
        return producer;
    }

    /**
     * 创建普通消息，顺序消息发送者
     *
     * @param accessKey  ak
     * @param secretKey  sk
     * @param nameServer 接入点
     * @param instanceId 实例id
     * @param topic      控制台配置
     * @return MQProducer
     */
    private MQProducer createProducer(@NotEmpty final String accessKey, @NotEmpty final String secretKey, @NotEmpty final String nameServer, final String instanceId, @NotEmpty final String topic) {
        MQClient client = getClient(accessKey, secretKey, nameServer);
        return createProducer(client, instanceId, topic);
    }

    /**
     * 创建普通消息，顺序消息发送者
     *
     * @param client MQClient
     * @param topic  控制台配置
     * @return MQProducer
     */
    private MQProducer createProducer(MQClient client, final String instanceId, @NotEmpty final String topic) {
        MQProducer producer;
        if (StringUtils.isEmpty(instanceId)) {
            producer = client.getProducer(instanceId, topic);
        } else {
            producer = client.getProducer(topic);
        }
        return producer;
    }


    /**
     * 创建事务消息发送者
     *
     * @param accessKey  ak
     * @param secretKey  sk
     * @param nameServer 接入点
     * @param instanceId 实例id
     * @param group      groupId，控制台配置
     * @param topic      控制台配置
     * @return MQTransProducer
     */
    private MQTransProducer createTransactionProducer(final String accessKey, final String secretKey, final String nameServer, final String instanceId, @NotEmpty final String group, @NotEmpty final String topic) {
        MQClient client = getClient(accessKey, secretKey, nameServer);
        return createTransactionProducer(client, instanceId, group, topic);
    }

    /**
     * 创建事务消息发送者
     *
     * @param client MQClient
     * @param group  groupId，控制台配置
     * @param topic  控制台配置
     * @return MQTransProducer
     */
    private MQTransProducer createTransactionProducer(MQClient client, final String instanceId, @NotEmpty final String group, @NotEmpty final String topic) {
        MQTransProducer transProducer;
        if (StringUtils.isEmpty(instanceId)) {
            transProducer = client.getTransProducer(topic, group);
        } else {
            transProducer = client.getTransProducer(instanceId, topic, group);
        }
        return transProducer;
    }

    /**
     * 事务消息发送者设置事务确认线程
     *
     * @param transProducer 消息生产者
     * @param checker       事务确认接口实现
     * @param name          名字
     * @param executor      线程池
     */
    private void applyChecker(MQTransProducer transProducer, final String name, HalfMsgStatusChecker checker, TaskExecutor executor) {
        if (null != transProducer && null != checker) {
            HalfMsgConfirmRunnable runnable = new HalfMsgConfirmRunnable(name, transProducer, checker);
            executor.execute(runnable);
            checkerContainer.put(name, runnable);
        }
    }
}