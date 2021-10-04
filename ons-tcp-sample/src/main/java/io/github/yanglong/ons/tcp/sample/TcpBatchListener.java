package io.github.yanglong.ons.tcp.sample;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import io.github.yanglong.ons.tcp.consumer.TcpBatchMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Description: TCP批量消费消息处理器
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/18
 */
@Slf4j
@Component
public class TcpBatchListener implements TcpBatchMessageListener {

    @Override
    public Action consume(List<Message> messages, ConsumeContext context) {
        for (Message msg : messages) {
            log.info("receive batch message: {}", msg);
        }
        return Action.CommitMessage;
    }
}
