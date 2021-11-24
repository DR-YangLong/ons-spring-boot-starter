package io.github.yanglong.ons.commons.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

/**
 * Description: ons消费者配置属性
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 * @see com.aliyun.openservices.ons.api.PropertyKeyConst
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnsConsumerProperties extends OnsCommonProperties {
    /**
     * 消费者所属group，不同类型的group不能混用,事务消息和顺序消息必须有
     */
    private String group;
    /**
     * 设置每条消息消费的最大超时时间,超过这个时间,这条消息将会被视为消费失败,等下次重新投递再次消费. 每个业务需要设置一个合理的值. 单位(分钟)
     */
    private String consumeTimeout = "1";
    /**
     * 消息消费失败时的最大重试次数
     */
    private String maxReconsumeTimes = "5";
    /**
     * 是否批量消息，如果为true，必须保证消息类型为NORMAL且订阅关系中listener为OnsBatchMessageListener
     */
    private boolean batchEnable = false;
    /**
     * 设置批量消费最大等待时长，当等待时间达到10秒，SDK立即执行回调进行消费。默认值：0，取值范围：0~450，单位：秒
     */
    private String batchConsumeMaxAwaitDurationInSeconds = "10";
    /**
     * BatchConsumer每次批量消费的最大消息数量, 默认值为1, 允许自定义范围为[1, 1024], 实际消费数量可能小于该值.
     */
    private String consumeMessageBatchMaxSize = "32";
    /**
     * 顺序消息消费失败进行重试前的等待时间 单位(毫秒)
     */
    private String suspendTimeMillis = "2000";
    /**
     * 处理时的消费者线程池大小，默认4线程。
     * 在TCP模式下为PropertyKeyConst.ConsumeThreadNums；
     * 在HTTP模式下为启动主动拉取的线程。
     */
    private String threadNums = "4";
    /**
     * 订阅关系列表
     */
    @NestedConfigurationProperty
    private List<OnsSubscriptionProperties> subscriptions;
}
