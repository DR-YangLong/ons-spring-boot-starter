package io.github.yanglong.ons.commons.properties;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Description: 通用配置
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/27
 */
@Data
public class OnsCommonProperties {
    /**
     * 配置名称，不需要配置，由程序回写
     */
    private String configName;
    /**
     * 类型，HTTP或TCP，不同的方式生成客户端方式不同，默认TCP
     */
    private ClientType type;
    /**
     * 消息类型，不同的消息类型生成的不客户端不同，默认NORMAL
     */
    private MessageType msgType = MessageType.NORMAL;
    /**
     * TCP协议接入点或http接入点
     */
    private String nameServer;
    /**
     * 实例ID，默认"",充当命名空间的作用，没有命名空间不要使用
     */
    private String instanceId = "";
    /**
     * 设置实例名，注意：如果在一个进程中将多个Producer或者是多个Consumer设置相同的InstanceName，底层会共享连接
     */
    private String instanceName = "";
    /**
     * 个性化access配置
     */
    @NestedConfigurationProperty
    private OnsAccessProperties access;
}
