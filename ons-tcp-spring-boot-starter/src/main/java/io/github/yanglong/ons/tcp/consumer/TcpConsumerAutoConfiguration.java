package io.github.yanglong.ons.tcp.consumer;

import io.github.yanglong.ons.commons.properties.OnsAccessProperties;
import io.github.yanglong.ons.commons.properties.OnsBaseConfig;
import io.github.yanglong.ons.commons.utils.OnsContextAware;
import io.github.yanglong.ons.commons.utils.OnsStringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Description: ONS TCP消费者自动配置类
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @since 2021/4/16 21:06 下午
 */
@Slf4j
@Data
@Configuration
@ConditionalOnProperty(prefix = "ali-ons", name = {"enable", "tcp.consumer.enable"}, havingValue = "true")
@ConditionalOnBean(name = {"onsContextAware"})
@EnableConfigurationProperties({TcpConsumerConfig.class, OnsBaseConfig.class})
public class TcpConsumerAutoConfiguration {
    @Autowired
    private OnsBaseConfig onsBaseConfig;
    @Autowired
    private TcpConsumerConfig tcpConsumerConfig;
    @Autowired
    private OnsContextAware onsContextAware;

    /**
     * 生成TCP消费者工厂
     *
     * @return TcpConsumerFactory
     */
    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public TcpConsumerFactory onsTcpConsumerFactory() {
        log.debug("config TcpConsumerFactory.");
        OnsAccessProperties defaultAccessProp = onsBaseConfig.getDefaultAccess();
        defaultAccessProp = OnsStringUtils.checkAccess(defaultAccessProp) ? defaultAccessProp : new OnsAccessProperties();
        TcpConsumerFactory factory = new TcpConsumerFactory(onsContextAware, defaultAccessProp, tcpConsumerConfig.getConsumers());
        log.debug("config TcpConsumerFactory finished.");
        return factory;
    }
}
