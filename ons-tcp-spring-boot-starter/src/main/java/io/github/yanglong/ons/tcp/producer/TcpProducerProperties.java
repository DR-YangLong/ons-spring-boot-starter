package io.github.yanglong.ons.tcp.producer;

import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import io.github.yanglong.ons.commons.properties.ClientType;
import io.github.yanglong.ons.commons.properties.OnsProducerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description: TCP发送者属性配置
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 20:42 下午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TcpProducerProperties extends OnsProducerProperties {
    /**
     * 固定为HTTP
     */
    private final ClientType type = ClientType.TCP;

    /**
     * TCP模式下，如果是事务消息，需要设置事务确认接口实现类，配置文件中设置实现类的全限定名称
     */
    private Class<LocalTransactionChecker> transChecker;
}
