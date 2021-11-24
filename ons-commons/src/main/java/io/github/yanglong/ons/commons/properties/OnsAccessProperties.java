package io.github.yanglong.ons.commons.properties;

import lombok.Data;

/**
 * Description: AK和SK配置
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/3
 */
@Data
public class OnsAccessProperties {
    /**
     * RocketMQ(ONS)的AccessKey
     */
    private String accessKey;
    /**
     * RocketMQ(ONS)的SecretKey
     */
    private String secretKey;
}
