package io.github.yanglong.ons.tcp.producer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Map;

/**
 * Description: TCP模式生产者配置类
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 21:45 下午
 */
@Data
@ConfigurationProperties(prefix = "ali-ons.tcp.producer")
public class TcpProducerConfig {
    /**
     * 是否启用HTTP Producer
     */
    private boolean enable;
    /**
     * 生产者配置，可以配置多个，使用时可以根据名称获取消息生产者，进行消息发送
     */
    @NestedConfigurationProperty
    private Map<String, TcpProducerProperties> producers;
}
