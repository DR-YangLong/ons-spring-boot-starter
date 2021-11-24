package io.github.yanglong.ons.commons.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Description: ONS基础配置
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 2:39 下午
 */
@Data
@ConfigurationProperties(prefix = "ali-ons")
public class OnsBaseConfig {
    /**
     * 是否启用ons
     */
    private boolean enable;
    /**
     * 全局AK和SK配置
     */
    @NestedConfigurationProperty
    private OnsAccessProperties defaultAccess;
}
