package io.github.yanglong.ons.tcp.consumer;

import io.github.yanglong.ons.commons.properties.ClientType;
import io.github.yanglong.ons.commons.properties.OnsConsumerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description: TCP模式消费者属性配置
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @since 2021/4/16 19:38 下午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TcpConsumerProperties extends OnsConsumerProperties {
    /**
     * 固定为HTTP
     */
    private final ClientType type = ClientType.TCP;
}
