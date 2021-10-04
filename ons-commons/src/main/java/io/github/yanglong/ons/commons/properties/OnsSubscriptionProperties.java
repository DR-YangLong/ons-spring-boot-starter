package io.github.yanglong.ons.commons.properties;

import io.github.yanglong.ons.commons.listener.OnsMessageListener;
import lombok.Data;

/**
 * Description: ons订阅关系配置
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
@Data
public class OnsSubscriptionProperties {
    /**
     * 消费topic
     */
    private String topic;
    /**
     * 可选，消费tag，同一个group，必须保证tag一致
     */
    private String tags = "*";
    /**
     * 业务消息处理listener全限定类名。
     * TCP模式下:OnsBatchMessageListener,OnsNormalMessageListener,OnsOrderMessageListener；
     * HTTP模式下：HttpMessageListener
     */
    private Class<OnsMessageListener> listener;
}
