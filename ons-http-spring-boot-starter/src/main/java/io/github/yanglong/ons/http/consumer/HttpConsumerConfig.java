package io.github.yanglong.ons.http.consumer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Map;

/**
 * Description: ONS HTTP消费者配置
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 20:05 下午
 */
@Data
@ConfigurationProperties(prefix = "ali-ons.http.consumer")
public class HttpConsumerConfig {
    /**
     * 是否启用HTTP Consumer
     */
    private boolean enable;
    /**
     * 消费者配置，可以配置多个
     */
    @NestedConfigurationProperty
    private Map<String, HttpConsumerProperties> consumers;
}
