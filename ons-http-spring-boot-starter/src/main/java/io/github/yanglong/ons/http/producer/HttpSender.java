package io.github.yanglong.ons.http.producer;

import com.aliyun.mq.http.MQProducer;
import com.aliyun.mq.http.MQTransProducer;
import com.aliyun.mq.http.model.AsyncCallback;
import com.aliyun.mq.http.model.TopicMessage;
import io.github.yanglong.ons.http.OnsHttpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Description: 封装HTTP发送消息方法，不封装异步发送
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/9
 */
@Slf4j
public class HttpSender {
    private final HttpProducerFactory httpProducerFactory;

    public HttpSender(HttpProducerFactory httpProducerFactory) {
        this.httpProducerFactory = httpProducerFactory;
    }

    /**
     * 创建HTTP消息
     *
     * @param tag 标签
     * @param key 业务唯一键值
     * @param msg 消息
     * @return TopicMessage
     */
    private TopicMessage createMsg(String key, String tag, String msg) {
        TopicMessage topicMessage = new TopicMessage();
        topicMessage.setMessageBody(msg.getBytes(StandardCharsets.UTF_8));
        topicMessage.setMessageKey(key);
        topicMessage.setMessageTag(tag);
        return topicMessage;
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @return msgId，失败返回NULL
     * @see #sendMsg(MQProducer, String, String, String)
     */
    public String sendMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg) {
        log.info("send HTTP mq normal message,producer name is {},message is[topic:{},tag:{},key:{},msg:{}]", producerName, topic, tag, key, msg);
        MQProducer producer = httpProducerFactory.getProducer(producerName, topic);
        return this.sendMsg(producer, tag, key, msg);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @param callback     消息回调接口实现
     * @see #sendAsyncMsg(MQProducer, String, String, String, AsyncCallback)
     */
    public void sendAsyncMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final AsyncCallback<TopicMessage> callback) {
        log.info("send HTTP mq async message,producer name is {},message is[topic:{},tag:{},key:{},msg:{}]", producerName, topic, tag, key, msg);
        MQProducer producer = httpProducerFactory.getProducer(producerName, topic);
        this.sendAsyncMsg(producer, tag, key, msg, callback);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param shardingKey  分区标识
     * @param msg          消息
     * @return msgId，失败返回NULL
     * @see #sendOrderMsg(MQProducer, String, String, String, String)
     */
    public String sendOrderMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String shardingKey, @NotEmpty final String msg) {
        log.info("send HTTP mq order message,producer name is {},message is[topic:{},tag:{},key:{},shardingKey:{},msg:{}]", producerName, topic, tag, key, shardingKey, msg);
        MQProducer producer = httpProducerFactory.getProducer(producerName, topic);
        return this.sendOrderMsg(producer, tag, key, shardingKey, msg);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @param custom       自定义参数
     * @return receiptHandle，可用于提交回滚消息
     * @see #sendTransactionMsg(MQProducer, String, String, String, Map)
     */
    public String sendTransactionMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, Map<String, String> custom) {
        log.info("send HTTP mq transaction message,producer name is {},message is [topic:{},tag:{},key:{},msg:{}]", producerName, topic, tag, key, msg);
        MQProducer producer = httpProducerFactory.getProducer(producerName, topic);
        return this.sendTransactionMsg(producer, tag, key, msg, custom);
    }

    /**
     * 回滚事务消息
     *
     * @param producerName  消息生产者名称
     * @param receiptHandle 消息receiptHandle
     * @throws Exception 异常
     */
    public void commitMsg(@NotEmpty final String producerName, @NotEmpty final String topic, @NotEmpty final String receiptHandle) throws Exception {
        log.info("begin commit HTTP transaction msg,producerName is{},topic is {},receiptHandle is {}.", producerName, topic, receiptHandle);
        commitMsg(httpProducerFactory.getProducer(producerName, topic), receiptHandle);
        log.info("success commit HTTP transaction msg,producerName is{},topic is {},receiptHandle is {}.", producerName, topic, receiptHandle);
    }

    /**
     * 回滚事务消息
     *
     * @param producerName  消息生产者名称
     * @param receiptHandle 消息receiptHandle
     * @throws Exception 异常
     */
    public void rollbackMsg(@NotEmpty final String producerName, @NotEmpty final String topic, @NotEmpty final String receiptHandle) throws Exception {
        log.info("begin rollback HTTP transaction msg,producerName is{},topic is {},receiptHandle is {}.", producerName, topic, receiptHandle);
        rollbackMsg(httpProducerFactory.getProducer(producerName, topic), receiptHandle);
        log.info("success rollback HTTP transaction msg,producerName is{},topic is {},receiptHandle is {}.", producerName, topic, receiptHandle);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @param delayTime    延迟毫秒数
     * @return msgId，失败返回NULL
     * @
     */
    public String sendDelayMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final long delayTime) {
        log.info("send HTTP mq delay message,producer name is {},message is[topic:{},tag:{},key:{},msg:{},delayTime:{}]", producerName, topic, tag, key, msg, delayTime);
        MQProducer producer = httpProducerFactory.getProducer(producerName, topic);
        return this.sendDelayMsg(producer, tag, key, msg, delayTime);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @param timestamp    投递消息时间的UNIX时间戳
     * @return msgId，失败返回NULL
     */
    public String sendTimeMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final long timestamp) {
        log.info("send HTTP mq timing message,producer name is {},message is[topic:{},tag:{},key:{},msg:{},timestamp:{}]", producerName, topic, tag, key, msg, timestamp);
        MQProducer producer = httpProducerFactory.getProducer(producerName, topic);
        return this.sendTimeMsg(producer, tag, key, msg, timestamp);
    }

    /**
     * 同步发送MQ普通消息
     *
     * @param producer 客户端
     * @param tag      标签
     * @param key      业务唯一键值
     * @param msg      消息
     * @return msgId，失败返回NULL
     */
    public String sendMsg(MQProducer producer, final String tag, @NotEmpty final String key, @NotEmpty final String msg) {
        String msgId = null;
        if (null != producer) {
            TopicMessage message = createMsg(key, tag, msg);
            try {
                TopicMessage sendResult = producer.publishMessage(message);
                if (null != sendResult) {
                    msgId = sendResult.getMessageId();
                    log.info("ONS HTTP client send sync message success. topic={}, msgId={}", producer.getTopicName(), msgId);
                }
            } catch (Exception e) {
                log.error("ONS HTTP client can't send sync msg,the msg is [topic:{},tag:{},key:{},msg:{}]", producer.getTopicName(), tag, key, msg, e);
            }
        } else {
            log.error("ONS HTTP client can't send sync msg,the producer not ready,the msg is [tag:{},key:{},msg:{}]", tag, key, msg);
        }
        return msgId;
    }

    /**
     * 异步发送MQ普通消息
     *
     * @param producer 客户端
     * @param tag      标签
     * @param key      业务唯一键值
     * @param msg      消息
     * @param callback 回调
     */
    public void sendAsyncMsg(MQProducer producer, final String tag, final String key, @NotEmpty final String msg, final AsyncCallback<TopicMessage> callback) {
        if (null != producer) {
            TopicMessage message = createMsg(key, tag, msg);
            // 异步发送消息，发送结果通过callback返回给客户端。
            producer.asyncPublishMessage(message, callback);
            // 在callback返回之前即可取得msgId。
            log.info("ONS HTTP client send async message success. key={},msg={}, msgId={}", key, msg, message.getMessageId());
        } else {
            log.error("ONS HTTP client can't send async msg,the producer not ready,the msg is [tag:{},key:{},msg:{}]", tag, key, msg);
        }
    }

    /**
     * 发送顺序消息,如果不传递shardingKey,将默认使用key作为shardingKey。
     * 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理。
     * <code>shardingKey</code>为分片key，分区顺序消息，不同分区设置不同的字段；全局顺序消息，该字段可以设置为任意非空字符串。在消费端需要指定一模一样的<code>shardingKey</code>。
     *
     * @param producer    客户端
     * @param tag         标签
     * @param key         业务唯一键值
     * @param shardingKey 分片key
     * @param msg         消息
     * @return msgId，失败返回NULL
     */
    public String sendOrderMsg(MQProducer producer, final String tag, @NotEmpty final String key, @NotEmpty final String shardingKey, @NotEmpty final String msg) {
        String msgId = null;
        if (null != producer) {
            TopicMessage message = createMsg(key, tag, msg);
            message.setShardingKey(shardingKey);
            try {
                TopicMessage sendResult = producer.publishMessage(message);
                if (sendResult != null) {
                    msgId = sendResult.getMessageId();
                    log.info("ONS HTTP client send order message success. msgId={},msg={}", msgId, msg);
                }
            } catch (Exception e) {
                log.error("ONS HTTP client can't send order msg,the msg is [tag:{},key:{},shardingKey:{},msg:{}]", tag, key, shardingKey, msg, e);
            }
        } else {
            log.error("ONS HTTP client can't send order msg,the producer not ready,the msg is [tag:{},key:{},shardingKey:{},msg:{}]", tag, key, shardingKey, msg);
        }
        return msgId;
    }

    /**
     * 发送事务消息
     *
     * @param producer 客户端
     * @param tag      标签
     * @param key      业务唯一键值
     * @param msg      消息
     * @param custom   应用自定义参数，该参数可以传入本地事务执行器
     * @return receiptHandle，失败返回NULL
     */
    public String sendTransactionMsg(MQProducer producer, final String tag, @NotEmpty final String key, @NotEmpty final String msg, Map<String, String> custom) {
        String receiptHandle = null;
        if (null != producer) {
            TopicMessage message = createMsg(key, tag, msg);
            // 设置事务第一次回查的时间，为相对时间，单位：秒，范围为10~300s之间
            // 第一次事务回查后如果消息没有commit或者rollback，则之后每隔10s左右会回查一次，总共回查一天
            message.setTransCheckImmunityTime(10);
            if (!CollectionUtils.isEmpty(custom)) {
                message.setProperties(custom);
            }
            String msgId = null;
            try {
                TopicMessage sendResult = producer.publishMessage(message);
                if (sendResult != null) {
                    msgId = sendResult.getMessageId();
                    receiptHandle = sendResult.getReceiptHandle();
                    log.info("ONS HTTP client send transaction message success. msgId={},key={},receiptHandle={}", msgId, key, receiptHandle);
                }
            } catch (Exception e) {
                log.error("ONS HTTP client can't send transaction msg,the msg is [tag:{},key:{},msg:{}]", tag, key, msg, e);
            }
        } else {
            log.error("ONS HTTP client can't send transaction msg,the producer not ready,the msg is [tag:{},key:{},msg:{}]", tag, key, msg);
        }
        return receiptHandle;
    }

    /**
     * 提交事务消息
     *
     * @param producer      消息生产者
     * @param receiptHandle 消息receiptHandle
     * @throws Exception 异常
     */
    public void commitMsg(MQProducer producer, @NotEmpty final String receiptHandle) throws Exception {
        if (producer instanceof MQTransProducer) {
            ((MQTransProducer) producer).commit(receiptHandle);
        } else {
            throw new OnsHttpException("producer is null or not a MQTransProducer!");
        }
    }

    /**
     * 回滚事务消息
     *
     * @param producer      消息生产者
     * @param receiptHandle 消息receiptHandle
     * @throws Exception 异常
     */
    public void rollbackMsg(MQProducer producer, @NotEmpty final String receiptHandle) throws Exception {
        if (producer instanceof MQTransProducer) {
            ((MQTransProducer) producer).rollback(receiptHandle);
        } else {
            throw new OnsHttpException("producer is null or not a MQTransProducer!");
        }
    }

    /**
     * 发送延时消息，实际使用的是发送定时消息
     *
     * @param producer  客户端
     * @param tag       标签
     * @param key       业务唯一键值
     * @param msg       消息
     * @param delayTime 延时时间，单位毫秒
     * @return msgId，失败返回NULL
     * @see #sendTimeMsg
     */
    public String sendDelayMsg(MQProducer producer, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final long delayTime) {
        long timestamp = System.currentTimeMillis() + delayTime;
        log.info("cast delayTime={} to timestamp={},[tag:{},key:{},msg:{}].", delayTime, timestamp, tag, key, msg);
        return this.sendTimeMsg(producer, tag, key, msg, timestamp);
    }

    /**
     * 发送定时消息
     * 如果<code>timestamp</code>小于当前时间，消息会被立即投递给消费者；
     * 如果在当前时间之后，将会在<code>timestamp</code>之后投递给消费者。
     *
     * @param producer  客户端
     * @param tag       标签
     * @param key       业务唯一键值
     * @param msg       消息
     * @param timestamp 时间戳，时间的UNIX时间毫秒数
     * @return msgId，失败返回NULL
     */
    public String sendTimeMsg(MQProducer producer, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final long timestamp) {
        String msgId = null;
        if (null != producer) {
            TopicMessage message = createMsg(key, tag, msg);
            message.setStartDeliverTime(timestamp);
            try {
                TopicMessage sendResult = producer.publishMessage(message);
                if (null != sendResult) {
                    msgId = sendResult.getMessageId();
                    log.info("ONS HTTP client send timing message success. message is [msgId:{},tag:{},key:{},msg:{},timestamp:{}]", msgId, tag, key, msg, timestamp);
                }
            } catch (Exception e) {
                log.error("ONS HTTP client can't send timing msg,the msg is [tag:{},key:{},msg:{},timestamp={}]", tag, key, msg, timestamp, e);
            }
        } else {
            log.error("ONS HTTP client can't send timing msg,the producer not ready,the msg is [tag:{},key:{},msg:{},timestamp={}]", tag, key, msg, timestamp);
        }
        return msgId;
    }
}
