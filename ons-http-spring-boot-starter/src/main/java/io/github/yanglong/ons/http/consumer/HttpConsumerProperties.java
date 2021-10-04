package io.github.yanglong.ons.http.consumer;

import io.github.yanglong.ons.commons.properties.ClientType;
import io.github.yanglong.ons.commons.properties.OnsConsumerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description:
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 19:38 下午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HttpConsumerProperties extends OnsConsumerProperties {
    /**
     * 固定为HTTP
     */
    private final ClientType type = ClientType.HTTP;
}
