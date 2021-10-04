package io.github.yanglong.ons.tcp.sample;

import com.aliyun.openservices.ons.api.Action;
import com.aliyun.openservices.ons.api.ConsumeContext;
import com.aliyun.openservices.ons.api.Message;
import io.github.yanglong.ons.tcp.consumer.TcpNormalMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Description: TCP普通，延迟，顺序，事务消息处理器
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/18
 */
@Slf4j
@Component
public class TcpNormalListener implements TcpNormalMessageListener {
    @Override
    public Action consume(Message message, ConsumeContext context) {
        log.info("receive normal message: {}", message);
        return Action.CommitMessage;
    }
}
