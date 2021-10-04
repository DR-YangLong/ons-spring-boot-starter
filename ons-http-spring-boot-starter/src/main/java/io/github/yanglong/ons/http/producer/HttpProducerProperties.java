package io.github.yanglong.ons.http.producer;

import io.github.yanglong.ons.commons.properties.ClientType;
import io.github.yanglong.ons.commons.properties.OnsProducerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description: HTTP发送者属性配置
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 20:42 下午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HttpProducerProperties extends OnsProducerProperties {
    /**
     * 固定为HTTP
     */
    private final ClientType type = ClientType.HTTP;
    /**
     * HTTP模式下，发送者端事务消息检查接口实现类全限定类名，HTTP事务消息模式下必须配置
     * 可以使用自定义参数协助业务处理
     */
    private Class<HalfMsgStatusChecker> httpTransChecker;
}
