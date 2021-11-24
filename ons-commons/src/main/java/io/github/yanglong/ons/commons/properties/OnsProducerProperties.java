package io.github.yanglong.ons.commons.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description: ons生产者配置属性
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OnsProducerProperties extends OnsCommonProperties {
    /**
     * 发送超时时间，单位毫秒
     */
    private String timeout = "3000";
    /**
     * group-id，事务和顺序消息需要使用，且不同类型的消息，group不能混用
     */
    private String group;
}
