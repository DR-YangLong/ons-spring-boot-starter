package io.github.yanglong.ons.tcp.producer;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.SendCallback;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.ons.api.order.OrderProducer;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter;
import com.aliyun.openservices.ons.api.transaction.TransactionProducer;
import io.github.yanglong.ons.tcp.AdminUtils;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotEmpty;
import java.nio.charset.StandardCharsets;

/**
 * Description: TC消息发送客户端封装
 *
 * 使用OnsTcpProducerFactory中的容器获取对应名称的producer实例，进行消息发送。
 * 没有对异步发送进行延时和定时消息封装。
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
@Slf4j
public class TcpSender {
    private final TcpProducerFactory tcpProducerFactory;

    public TcpSender(TcpProducerFactory tcpProducerFactory) {
        this.tcpProducerFactory = tcpProducerFactory;
    }

    /**
     * 创建消息发送msg
     *
     * @param topic topic
     * @param tag   标签
     * @param key   业务唯一键值
     * @param msg   消息
     * @return ons发送的msg
     */
    private Message createMsg(final String topic, final String tag, final String key, final String msg) {
        Message message = new Message(
                // Message所属的Topic。
                topic,
                // Message Tag，可理解为Gmail中的标签，对消息进行再归类，方便Consumer指定过滤条件在消息队列RocketMQ版的服务器过滤。
                tag,
                // Message Body，任何二进制形式的数据，消息队列RocketMQ版不做任何干预，需要Producer与Consumer协商好一致的序列化和反序列化方式。
                msg.getBytes(StandardCharsets.UTF_8));
        // 设置代表消息的业务关键属性，请尽可能全局唯一。以方便您在无法正常收到消息情况下，可通过控制台查询消息并补发。
        // 注意：不设置也不会影响消息正常收发。
        message.setKey(key);
        return message;
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @return msgId，失败返回NULL
     * @see #sendMsg(Producer, String, String, String, String)
     */
    public String sendMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg) {
        Producer producer = tcpProducerFactory.getNormalProducer(producerName);
        return this.sendMsg(producer, topic, tag, key, msg);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @param callback     消息回调接口实现
     * @see #sendAsyncMsg(Producer, String, String, String, String, SendCallback)
     */
    public void sendAsyncMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final SendCallback callback) {
        Producer producer = tcpProducerFactory.getNormalProducer(producerName);
        this.sendAsyncMsg(producer, topic, tag, key, msg, callback);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param shardingKey  分区标识
     * @param msg          消息
     * @return msgId，失败返回NULL
     * @see #sendOrderMsg(OrderProducer, String, String, String, String, String)
     */
    public String sendOrderMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String shardingKey, @NotEmpty final String msg) {
        OrderProducer producer = tcpProducerFactory.getOrderProducer(producerName);
        return this.sendOrderMsg(producer, topic, tag, key, shardingKey, msg);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param executer     本地事务执行器
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @param custom       自定义参数
     * @return msgId，失败返回NULL
     * @see #sendTransactionMsg(TransactionProducer, LocalTransactionExecuter, String, String, String, String, Object)
     */
    public String sendTransactionMsg(@NotEmpty final String producerName, LocalTransactionExecuter executer, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final Object custom) {
        TransactionProducer producer = tcpProducerFactory.getTransactionProducer(producerName);
        return this.sendTransactionMsg(producer, executer, topic, tag, key, msg, custom);
    }

    /**
     * @param producerName 消息生产者名字，在配置文件中配置
     * @param topic        topic
     * @param tag          标签
     * @param key          业务唯一键值
     * @param msg          消息
     * @param delayTime    延迟毫秒数
     * @return msgId，失败返回NULL
     */
    public String sendDelayMsg(@NotEmpty final String producerName, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final long delayTime) {
        Producer producer = tcpProducerFactory.getNormalProducer(producerName);
        return this.sendDelayMsg(producer, topic, tag, key, msg, delayTime);
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
        Producer producer = tcpProducerFactory.getNormalProducer(producerName);
        return this.sendTimeMsg(producer, topic, tag, key, msg, timestamp);
    }

    /**
     * 同步发送MQ普通消息
     *
     * @param producer 客户端
     * @param topic    topic
     * @param tag      标签
     * @param key      业务唯一键值
     * @param msg      消息
     * @return msgId，失败返回NULL
     */
    public String sendMsg(Producer producer, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg) {
        log.info("ONS send normal sync msg.topic={},tag={},key={},body={}.", topic, tag, key, msg);
        String msgId = null;
        if (AdminUtils.isInstanceReady(producer)) {
            Message message = createMsg(topic, tag, key, msg);
            try {
                SendResult sendResult = producer.send(message);
                if (null != sendResult) {
                    msgId = sendResult.getMessageId();
                    log.info("ONS TCP client send sync message success. topic={}, msgId={}", topic, msgId);
                }
            } catch (Exception e) {
                log.error("ONS TCP client can't send sync msg,the msg is [topic:{},tag:{},key:{},msg:{}]", topic, tag, key, msg, e);
            }
        } else {
            log.error("ONS TCP client can't send sync msg,the producer not ready,the msg is [topic:{},tag:{},key:{},msg:{}]", topic, tag, key, msg);
        }
        return msgId;
    }

    /**
     * 异步发送MQ普通消息
     *
     * @param producer 客户端
     * @param topic    topic
     * @param tag      标签
     * @param key      业务唯一键值
     * @param msg      消息
     * @param callback 回调
     */
    public void sendAsyncMsg(Producer producer, @NotEmpty final String topic, final String tag, final String key, @NotEmpty final String msg, final SendCallback callback) {
        log.info("ONS send normal async msg.topic={},tag={},key={},body={}.", topic, tag, key, msg);
        if (AdminUtils.isInstanceReady(producer)) {
            Message message = createMsg(topic, tag, key, msg);
            // 异步发送消息，发送结果通过callback返回给客户端。
            producer.sendAsync(message, callback);
            // 在callback返回之前即可取得msgId。
            log.info("ONS TCP client send async message success. topic={}, msgId={}", topic, message.getMsgID());
        } else {
            log.error("ONS TCP client can't send async msg,the producer not ready,the msg is [topic:{},tag:{},key:{},msg:{}]", topic, tag, key, msg);
        }
    }

    /**
     * 发送顺序消息,如果不传递shardingKey,将默认使用key作为shardingKey。
     * 消息发送失败，需要进行重试处理，可重新发送这条消息或持久化这条数据进行补偿处理。
     * <code>shardingKey</code>为分片key，分区顺序消息，不同分区设置不同的字段；全局顺序消息，该字段可以设置为任意非空字符串。在消费端需要指定一模一样的<code>shardingKey</code>。
     *
     * @param producer    客户端
     * @param topic       topic
     * @param tag         标签
     * @param key         业务唯一键值
     * @param shardingKey 分片key
     * @param msg         消息
     * @return msgId，失败返回NULL
     */
    public String sendOrderMsg(OrderProducer producer, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String shardingKey, @NotEmpty final String msg) {
        log.info("ONS send order msg.topic={},tag={},key={},shardingKey={},body={}.", topic, tag, key, shardingKey, msg);
        String msgId = null;
        if (AdminUtils.isInstanceReady(producer)) {
            Message message = createMsg(topic, tag, key, msg);
            try {
                SendResult sendResult = producer.send(message, shardingKey);
                if (sendResult != null) {
                    msgId = sendResult.getMessageId();
                    log.info("ONS TCP client send order message success. topic={}, msgId={}", topic, msgId);
                }
            } catch (Exception e) {
                log.error("ONS TCP client can't send order msg,the msg is [topic:{},tag:{},key:{},shardingKey:{},msg:{}]", topic, tag, key, shardingKey, msg, e);
            }
        } else {
            log.error("ONS TCP client can't send order msg,the producer not ready,the msg is [topic:{},tag:{},key:{},shardingKey:{},msg:{}]", topic, tag, key, shardingKey, msg);
        }
        return msgId;
    }

    /**
     * 发送事务消息
     *
     * @param producer 客户端
     * @param executer 本地事务执行器
     * @param topic    topic
     * @param tag      标签
     * @param key      业务唯一键值
     * @param msg      消息
     * @param custom   应用自定义参数，该参数可以传入本地事务执行器
     * @return msgId，失败返回NULL
     */
    public String sendTransactionMsg(TransactionProducer producer, LocalTransactionExecuter executer, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final Object custom) {
        log.info("ONS send transaction msg.topic={},tag={},key={},body={}.", topic, tag, key, msg);
        String msgId = null;
        if (AdminUtils.isInstanceReady(producer)) {
            Message message = createMsg(topic, tag, key, msg);
            try {
                SendResult sendResult = producer.send(message, executer, custom);
                if (sendResult != null) {
                    msgId = sendResult.getMessageId();
                    log.info("ONS TCP client send transaction message success. topic={}, msgId={}", topic, msgId);
                }
            } catch (Exception e) {
                log.error("ONS TCP client can't send transaction msg,the msg is [topic:{},tag:{},key:{},msg:{}]", topic, tag, key, msg, e);
            }
        } else {
            log.error("ONS TCP client can't send transaction msg,the producer not ready,the msg is [topic:{},tag:{},key:{},msg:{}]", topic, tag, key, msg);
        }
        return msgId;
    }

    /**
     * 发送延时消息，实际使用的是发送定时消息
     *
     * @param producer  客户端
     * @param topic     topic
     * @param tag       标签
     * @param key       业务唯一键值
     * @param msg       消息
     * @param delayTime 延时时间，单位毫秒
     * @return msgId，失败返回NULL
     * @see #sendTimeMsg
     */
    public String sendDelayMsg(Producer producer, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final long delayTime) {
        log.info("ONS send delay msg.topic={},tag={},key={},body={},delayTime={}.", topic, tag, key, msg, delayTime);
        long timestamp = System.currentTimeMillis() + delayTime;
        log.info("cast delayTime={} to timestamp={},topic={},tag={},key={},body={}.", delayTime, timestamp, topic, tag, key, msg);
        return sendTimeMsg(producer, topic, tag, key, msg, timestamp);
    }

    /**
     * 发送定时消息
     * 如果<code>timestamp</code>小于当前时间，消息会被立即投递给消费者；
     * 如果在当前时间之后，将会在<code>timestamp</code>之后投递给消费者。
     *
     * @param producer  客户端
     * @param topic     topic
     * @param tag       标签
     * @param key       业务唯一键值
     * @param msg       消息
     * @param timestamp 时间戳，时间的UNIX时间毫秒数
     * @return msgId，失败返回NULL
     */
    public String sendTimeMsg(Producer producer, @NotEmpty final String topic, final String tag, @NotEmpty final String key, @NotEmpty final String msg, final long timestamp) {
        log.info("ONS send timing msg.topic={},tag={},key={},body={},delayTime={}.", topic, tag, key, msg, timestamp);
        String msgId = null;
        if (AdminUtils.isInstanceReady(producer)) {
            Message message = createMsg(topic, tag, key, msg);
            message.setStartDeliverTime(timestamp);
            try {
                SendResult sendResult = producer.send(message);
                if (null != sendResult) {
                    msgId = sendResult.getMessageId();
                    log.info("ONS TCP client send timing message success. topic={}, msgId={}", topic, msgId);
                }
            } catch (Exception e) {
                log.error("ONS TCP client can't send timing msg,the msg is [topic:{},tag:{},key:{},msg:{},timestamp={}]", topic, tag, key, msg, timestamp, e);
            }
        } else {
            log.error("ONS TCP client can't send timing msg,the producer not ready,the msg is [topic:{},tag:{},key:{},msg:{},timestamp={}]", topic, tag, key, msg, timestamp);
        }
        return msgId;
    }

}
