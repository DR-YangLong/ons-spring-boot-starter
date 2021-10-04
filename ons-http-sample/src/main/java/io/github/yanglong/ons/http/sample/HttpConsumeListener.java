package io.github.yanglong.ons.http.sample;

import com.aliyun.mq.http.model.Message;
import io.github.yanglong.ons.http.consumer.HttpMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Description: HTTP消息处理
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/18
 */
@Slf4j
@Component
public class HttpConsumeListener implements HttpMessageListener {

    @Override
    public boolean consumeMessage(Message message) {
        log.debug("received HTTP msg:{}", message);
        log.info("收到ONS消息：id={}，key={}，tag={}，body={}，发送时间={}", message.getMessageId(),
                message.getMessageKey(),
                message.getMessageTag(),
                message.getMessageBodyString(),
                message.getPublishTime());
        return true;
    }
}
