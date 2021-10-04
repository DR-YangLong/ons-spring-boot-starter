package io.github.yanglong.ons.tcp.consumer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Map;

/**
 * Description:
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 21:03 下午
 */
@Data
@ConfigurationProperties(prefix = "ali-ons.tcp.consumer")
public class TcpConsumerConfig {
    /**
     * 是否启用TCP Consumer
     */
    private boolean enable;
    /**
     * 消费者配置，可以配置多个
     */
    @NestedConfigurationProperty
    private Map<String, TcpConsumerProperties> consumers;
}
