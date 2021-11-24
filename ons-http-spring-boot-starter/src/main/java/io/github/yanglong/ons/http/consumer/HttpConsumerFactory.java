package io.github.yanglong.ons.http.consumer;

import com.aliyun.mq.http.MQClient;
import com.aliyun.mq.http.MQConsumer;
import io.github.yanglong.ons.commons.listener.OnsMessageListener;
import io.github.yanglong.ons.commons.properties.MessageType;
import io.github.yanglong.ons.commons.properties.OnsAccessProperties;
import io.github.yanglong.ons.commons.properties.OnsSubscriptionProperties;
import io.github.yanglong.ons.commons.utils.OnsContextAware;
import io.github.yanglong.ons.commons.utils.OnsStringUtils;
import io.github.yanglong.ons.http.AbstractHttpClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: ONS HTTP消费者工厂实现
 *
 * ons消费者，订阅topic，使用listener进行处理，注意同时订阅多个topic时需要在listener中分开处理
 * 完成配置生成客户端与从配置文件生成消费者并配置，未完成消费者刷新配置，重新生成。
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
@Slf4j
public class HttpConsumerFactory extends AbstractHttpClientFactory {
    /**
     * 容器工具类，用于获取容器中listener，构造传入
     */
    private final OnsContextAware contextAware;
    /**
     * 线程池-用于提交消费线程，构造传入
     */
    private final TaskExecutor taskExecutor;
    /**
     * MQConsumer容器，用于复用
     */
    private final Map<String, MQConsumer> consumerContainer = new ConcurrentHashMap<>(32);
    /**
     * 消费者实例对应的消息处理器，由于可以设置多个线程同时消费消息，因此存在一对多
     */
    private final Map<String, List<HttpConsumerRunnable>> handlers = new ConcurrentHashMap<>(32);

    public HttpConsumerFactory(Map<String, MQClient> clients, OnsAccessProperties defaultAccessProperties, Map<String, HttpConsumerProperties> consumerProperties, OnsContextAware contextAware, TaskExecutor taskExecutor) {
        super(clients, defaultAccessProperties, consumerProperties);
        this.contextAware = contextAware;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void init() {
        //先生成Client
        super.init();
        if (CollectionUtils.isEmpty(commonProperties)) {
            log.error("ons HTTP consumer property is empty,can't init HTTP consumers.");
            return;
        }
        //生成consumer
        commonProperties.forEach((name, properties) -> {
            HttpConsumerProperties consumerProperties = (HttpConsumerProperties) properties;
            OnsAccessProperties accessProperties = consumerProperties.getAccess();
            if (OnsStringUtils.checkAccess(defaultAccessProperties) || OnsStringUtils.checkAccess(accessProperties)) {
                String host = consumerProperties.getNameServer();
                String group = consumerProperties.getGroup();
                String instanceId = consumerProperties.getInstanceId();
                MessageType messageType = consumerProperties.getMsgType();
                String threadNum = consumerProperties.getThreadNums();
                String ak;
                String sk;
                if (!OnsStringUtils.checkAccess(accessProperties)) {
                    ak = defaultAccessProperties.getAccessKey();
                    sk = defaultAccessProperties.getSecretKey();
                } else {
                    ak = accessProperties.getAccessKey();
                    sk = accessProperties.getSecretKey();
                }
                //由于HTTP模式不能同时消费多个Topic，所以需要对每个订阅关系分开处理
                List<OnsSubscriptionProperties> subscriptionProperties = consumerProperties.getSubscriptions();
                for (OnsSubscriptionProperties subscribe : subscriptionProperties) {
                    Class<OnsMessageListener> listenerClass = subscribe.getListener();
                    String topic = subscribe.getTopic();
                    String tags = subscribe.getTags();
                    if (null != listenerClass && HttpMessageListener.class.isAssignableFrom(listenerClass)) {
                        MQConsumer consumer = createConsumer(ak, sk, host, instanceId, topic, group, tags);
                        if (null != consumer) {
                            //设置消费线程
                            OnsMessageListener messageListener = contextAware.getBean(listenerClass);
                            if (null != messageListener) {
                                String handlerName = getHandlerName(name, topic, group, tags);
                                applyListener(handlerName, name, messageType, threadNum, (HttpMessageListener) messageListener, consumer, taskExecutor);
                            } else {
                                log.error("can't find HttpMessageListener for consumer {}，topic is {},group is {}!", name, topic, group);
                            }
                        }
                    } else {
                        log.error("can't init HTTP consumer {},HttpMessageListener can't find.", name);
                    }
                }
            } else {
                log.error("can't get access properties for HTTP consumer {},can't init.", name);
            }
        });
    }

    @Override
    public void shutdown() {
        if (!CollectionUtils.isEmpty(handlers)) {
            handlers.values().stream().flatMap(Collection::stream).forEach(handler -> handler.setShutdown(true));
        }
        super.shutdown();
    }

    /**
     * 获取HTTP模式消费者实例
     *
     * @param accessKey  AK
     * @param secretKey  SK
     * @param nameServer HTTP接入点
     * @param instanceId namespace
     * @param topic      topic
     * @param group      groupId
     * @param tags       tag
     * @return 消费客户端
     */
    private MQConsumer createConsumer(final String accessKey, final String secretKey, final String nameServer, final String instanceId, @NotEmpty final String topic, @NotEmpty final String group, final String tags) {
        MQClient client = getClient(accessKey, secretKey, nameServer);
        return createConsumer(client, instanceId, topic, group, tags);
    }

    /**
     * @param client     http模式MQClient
     * @param instanceId namespace
     * @param topic      topic
     * @param group      groupId
     * @param tags       tag
     * @return 消费客户端
     */
    private MQConsumer createConsumer(MQClient client, final String instanceId, @NotEmpty final String topic, @NotEmpty final String group, final String tags) {
        MQConsumer consumer = null;
        if (OnsStringUtils.isAnyEmpty(topic, group)) {
            log.error("param error!can't create consumer for [topic:{},group:{},tags:{},instanceId:{}].", topic, group, tags, instanceId);
        } else {
            if (null != client) {
                String tag = OnsStringUtils.stringReplace(tags, OnsStringUtils.COMMA, "||");
                if (StringUtils.isNotEmpty(instanceId)) {
                    consumer = client.getConsumer(instanceId, topic, group, tag);
                } else {
                    consumer = client.getConsumer(topic, group, tag);
                }
            } else {
                log.error("MQClient is null,can't create consumer for [topic:{},group:{},tags:{},instanceId:{}].", topic, group, tags, instanceId);
            }
        }
        return consumer;
    }

    /**
     * 为consumer设置消息消费器
     *
     * @param handlerName handler名称
     * @param name        消费者名称
     * @param messageType 消息类型
     * @param threadNum   线程数
     * @param listener    消息处理实现
     * @param executor    线程池
     */
    private void applyListener(@NotNull final String handlerName, @NotNull final String name, @NotNull MessageType messageType, final String threadNum, @NotNull final HttpMessageListener listener, @NotNull MQConsumer consumer, @NotNull TaskExecutor executor) {
        int num = 1;
        if (StringUtils.isNumeric(threadNum)) {
            try {
                num = Integer.parseInt(threadNum);
            } catch (NumberFormatException e) {
                log.error("parameter threadNum can't cast to number.", e);
            }
        }
        List<HttpConsumerRunnable> runnables = handlers.get(name);
        if (CollectionUtils.isEmpty(runnables)) {
            runnables = new ArrayList<>();
        }
        for (int i = 0; i < num; i++) {
            HttpConsumerRunnable handler = new HttpConsumerRunnable(handlerName, listener, consumer, messageType);
            executor.execute(handler);
            runnables.add(handler);
        }
        handlers.put(name, runnables);
    }

    /**
     * 使用"_"连接订阅配置的名称，topic，group，tags，生成唯一订阅关系处理的handler的名称
     *
     * @param consumerName 配置名称
     * @param topic        topic
     * @param group        group
     * @param tags         tags
     * @return handler名称
     */
    private String getHandlerName(@NotEmpty final String consumerName, @NotEmpty final String topic, @NotEmpty final String group, String tags) {
        StringBuilder builder = new StringBuilder(consumerName)
                .append("_")
                .append(topic)
                .append("_")
                .append(group);
        if (StringUtils.isNotEmpty(tags)) {
            builder.append("_").append(tags);
        }
        return builder.toString();
    }
}
