package io.github.yanglong.ons.tcp.producer;

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
 * Description: ONS TCP 生产者自动配置类
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @since 2021/4/16 21:06 下午
 */
@Slf4j
@Data
@Configuration
@ConditionalOnProperty(prefix = "ali-ons", name = {"enable", "tcp.producer.enable"}, havingValue = "true")
@ConditionalOnBean(name = {"onsContextAware"})
@EnableConfigurationProperties({TcpProducerConfig.class, OnsBaseConfig.class})
public class TcpProducerAutoConfiguration {
    @Autowired
    private OnsBaseConfig onsBaseConfig;
    @Autowired
    private TcpProducerConfig producerConfig;
    @Autowired
    private OnsContextAware onsContextAware;


    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public TcpProducerFactory onsTcpProducerFactory() {
        log.debug("config TcpProducerFactory.");
        OnsAccessProperties defaultAccessProp = onsBaseConfig.getDefaultAccess();
        defaultAccessProp = OnsStringUtils.checkAccess(defaultAccessProp) ? defaultAccessProp : new OnsAccessProperties();
        TcpProducerFactory factory = new TcpProducerFactory(defaultAccessProp, onsContextAware, producerConfig.getProducers());
        log.debug("config TcpProducerFactory finished.");
        return factory;
    }

    @Bean
    public TcpSender onsTcpSender(TcpProducerFactory factory) {
        return new TcpSender(factory);
    }
}
