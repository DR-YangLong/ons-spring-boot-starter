package io.github.yanglong.ons.http.consumer;

import com.aliyun.mq.http.model.Message;
import io.github.yanglong.ons.commons.listener.OnsMessageListener;

/**
 * Description: HTTP模式消费者消费消息接口。
 * 消费消息注意幂等，因为如果不在消费时限内消费完此条消息，会导致重复消费。
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/11
 */
public interface HttpMessageListener extends OnsMessageListener {
    /**
     * 消费消息，并返回消费状态。如果返回true，将会向MQ确认消费成功
     *
     * @param message 接收到的消息对象
     * @return true-消费成功，false-失败
     */
    boolean consumeMessage(Message message);
}
