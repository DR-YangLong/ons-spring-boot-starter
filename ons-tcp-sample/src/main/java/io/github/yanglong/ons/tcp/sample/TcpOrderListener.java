package io.github.yanglong.ons.tcp.sample;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.order.ConsumeOrderContext;
import com.aliyun.openservices.ons.api.order.OrderAction;
import io.github.yanglong.ons.tcp.consumer.TcpOrderMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Description: TCP顺序消费者消息处理器
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/18
 */
@Slf4j
@Component
public class TcpOrderListener implements TcpOrderMessageListener {

    @Override
    public OrderAction consume(Message message, ConsumeOrderContext context) {
        log.info("receive order message: {}", message);
        return OrderAction.Success;
    }
}
