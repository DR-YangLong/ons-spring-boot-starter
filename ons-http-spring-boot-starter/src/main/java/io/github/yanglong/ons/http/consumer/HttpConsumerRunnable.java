package io.github.yanglong.ons.http.consumer;

import com.aliyun.mq.http.MQConsumer;
import com.aliyun.mq.http.common.AckMessageException;
import com.aliyun.mq.http.model.Message;
import io.github.yanglong.ons.commons.properties.MessageType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * Description: HTTP消息消费者线程
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/11
 */
@Data
@Slf4j
public class HttpConsumerRunnable implements Runnable {
    /**
     * handler的名称
     */
    private final String name;
    /**
     * 消费者实例
     */
    private final MQConsumer mqConsumer;
    /**
     * 消息类型，仅区分顺序消息和其他消息
     */
    private final MessageType messageType;
    /**
     * 消费消息的处理器
     */
    private HttpMessageListener messageListener;
    /**
     * 用于控住消费是否停止
     */
    private volatile boolean shutdown = false;
    /**
     * 连续消费失败计数器
     */
    private int retry = 0;

    public HttpConsumerRunnable(String name, HttpMessageListener messageListener, MQConsumer mqConsumer, MessageType messageType) {
        this.name = name;
        this.messageListener = messageListener;
        this.mqConsumer = mqConsumer;
        this.messageType = messageType;
    }

    @Override
    public void run() {
        //不关闭的状态下永远进行消费
        while (!shutdown) {
            if (retry > 9) {
                log.error("HTTP consumer: handler [{}] can't connect to server!begin quit.", name);
                shutdown = true;
                break;
            }
            List<Message> messages = null;
            try {
                if (MessageType.ORDER.equals(messageType)) {
                    // 一次最多消费3条(最多可设置为16条)
                    // 长轮询时间5秒（最多可设置为30秒）,没有消息在服务端挂住5秒
                    messages = mqConsumer.consumeMessageOrderly(3, 5);
                } else {
                    messages = mqConsumer.consumeMessage(3, 5);
                }
                //执行成功后重置计数器
                if (retry > 0) {
                    retry = 0;
                }
            } catch (Throwable e) {
                log.error("HTTP consumer: handler [{}] pull ONS message error!", name, e);
                if (shutdown) {
                    log.debug("HTTP MQConsumer shutdown!");
                    break;
                }
                try {
                    retry++;
                    //15秒后重试
                    Thread.sleep(15000);
                } catch (InterruptedException interruptedException) {
                    log.error("HTTP consumer: handler [{}] consume thread interrupted!", name, e);
                }
            }
            if (messages == null || messages.isEmpty()) {
                log.info("thread {}: handler [{}] no new message, continue!", Thread.currentThread().getName(), name);
                continue;
            }
            for (Message message : messages) {
                boolean status = messageListener.consumeMessage(message);
                if (status) {
                    try {
                        mqConsumer.ackMessage(Collections.singletonList(message.getReceiptHandle()));
                    } catch (Throwable e) {
                        log.error("handler [{}],Ack message fail!", name, e);
                        // 某些消息的句柄可能超时了会导致确认不成功
                        if (e instanceof AckMessageException) {
                            AckMessageException errors = (AckMessageException) e;
                            log.error("Ack message fail, requestId is:{}", errors.getRequestId());
                            if (errors.getErrorMessages() != null) {
                                for (String errorHandle : errors.getErrorMessages().keySet()) {
                                    log.error("Ack message fail, fail Handle:{}, ErrorCode:{}, ErrorMsg:{}", errorHandle, errors.getErrorMessages().get(errorHandle).getErrorCode(), errors.getErrorMessages().get(errorHandle).getErrorMessage());
                                }
                            }
                        }
                    }
                } else {
                    log.info("handler [{}] consume message failed!message:{}", name, message.toString());
                }
            }
        }
    }
}
