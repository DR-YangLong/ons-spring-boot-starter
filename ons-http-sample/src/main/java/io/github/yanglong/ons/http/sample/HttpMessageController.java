package io.github.yanglong.ons.http.sample;

import io.github.yanglong.ons.http.producer.HttpSender;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description: HTTP发送消息
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/17
 */
@Api(tags = "HTTP方式发送消息")
@RestController
@RequestMapping("msg/http")
public class HttpMessageController {
    private final String httpNormalProducer = "normal";
    @Autowired
    private HttpSender httpSender;

    @ApiOperation("普通消息")
    @GetMapping("normal")
    public String sendNormal(MessageVO message) {
        String id = httpSender.sendMsg(httpNormalProducer, message.getTopic(), message.getTags(), message.getKey(), message.getBody());
        return id;
    }

    @ApiOperation("顺序消息")
    @GetMapping("order")
    public String sendOrder(MessageVO message) {
        String id = httpSender.sendOrderMsg(httpNormalProducer, message.getTopic(), message.getTags(), message.getKey(), message.getShardingKey(), message.getBody());
        return id;
    }

    @ApiOperation("事务消息")
    @GetMapping("trans")
    public String sendTrans(MessageVO message) throws Exception {
        String id = httpSender.sendTransactionMsg("trans", message.getTopic(), message.getTags(), message.getKey(), message.getBody(), null);
        //直接根据半消息句柄提交事物消息
        httpSender.commitMsg("trans", message.getTopic(), id);
        return id;
    }

    @ApiOperation("定时消息")
    @GetMapping("time")
    public String sendTime(MessageVO message) {
        String id = httpSender.sendTimeMsg(httpNormalProducer, message.getTopic(), message.getTags(), message.getKey(), message.getBody(), System.currentTimeMillis() + 10000);
        return id;
    }

    @ApiOperation("延迟消息")
    @GetMapping("delay")
    public String sendDelay(MessageVO message) {
        String id = httpSender.sendDelayMsg(httpNormalProducer, message.getTopic(), message.getTags(), message.getKey(), message.getBody(), message.getDelayTime());
        return id;
    }
}
