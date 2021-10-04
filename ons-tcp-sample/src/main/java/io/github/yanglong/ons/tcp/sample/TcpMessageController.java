package io.github.yanglong.ons.tcp.sample;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import io.github.yanglong.ons.tcp.producer.TcpSender;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Description: TCP发送消息测试接口
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/18
 */
@Slf4j
@Api(tags = "TCP方式发送消息")
@RestController
@RequestMapping("msg/tcp")
public class TcpMessageController {
    private final String normalProducer = "normal";
    private final String orderProducer = "order";
    private final String transProducer = "trans";
    @Autowired
    private TcpSender tcpSender;

    @ApiOperation("普通消息")
    @GetMapping("normal")
    public String sendNormal(MessageVO message) {
        String id = tcpSender.sendMsg(normalProducer, message.getTopic(), message.getTags(), message.getKey(), message.getBody());
        return id;
    }

    @ApiOperation("顺序消息")
    @GetMapping("order")
    public String sendOrder(MessageVO message) {
        String id = tcpSender.sendOrderMsg(orderProducer, message.getTopic(), message.getTags(), message.getKey(), message.getShardingKey(), message.getBody());
        return id;
    }

    @ApiOperation("事务消息")
    @GetMapping("trans")
    public String sendTrans(MessageVO message) {
        String id = tcpSender.sendTransactionMsg(transProducer, new LocalTransactionExecuter() {
            @Override
            public TransactionStatus execute(Message msg, Object arg) {
                log.info("local transaction executor,msg:[msgId:{},topic:{},key:{},tags:{},arg:{},body:{}]", msg.getMsgID(), msg.getTopic(), msg.getKey(), msg.getTag(), arg, Arrays.toString(msg.getBody()));
                return TransactionStatus.CommitTransaction;
            }
        }, message.getTopic(), message.getTags(), message.getKey(), message.getBody(), "测试传递自定义参数");
        return id;
    }

    @ApiOperation("定时消息")
    @GetMapping("time")
    public String sendTime(MessageVO message) {
        String id = tcpSender.sendTimeMsg(normalProducer, message.getTopic(), message.getTags(), message.getKey(), message.getBody(), System.currentTimeMillis() + 10000);
        return id;
    }

    @ApiOperation("延迟消息")
    @GetMapping("delay")
    public String sendDelay(MessageVO message) {
        String id = tcpSender.sendDelayMsg(normalProducer, message.getTopic(), message.getTags(), message.getKey(), message.getBody(), message.getDelayTime());
        return id;
    }
}
