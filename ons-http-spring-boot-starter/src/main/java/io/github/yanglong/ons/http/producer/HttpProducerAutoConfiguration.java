package io.github.yanglong.ons.http.producer;

import io.github.yanglong.ons.commons.properties.OnsBaseConfig;
import io.github.yanglong.ons.commons.utils.OnsContextAware;
import io.github.yanglong.ons.http.OnsHttpClientCache;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

/**
 * Description: ONS HTTP自动配置生产者
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 19:41 下午
 */
@Slf4j
@Data
@Configuration
@ConditionalOnProperty(prefix = "ali-ons", name = {"enable", "http.producer.enable"}, havingValue = "true")
@ConditionalOnBean(name = {"taskExecutor", "onsContextAware"})
@EnableConfigurationProperties({HttpProducerConfig.class, OnsBaseConfig.class})
public class HttpProducerAutoConfiguration {
    @Autowired
    private OnsBaseConfig onsBaseConfig;
    @Autowired
    private HttpProducerConfig httpProducerConfig;
    @Autowired
    private OnsContextAware onsContextAware;
    @Autowired
    private TaskExecutor taskExecutor;

    @Bean
    public HttpSender httpSender(HttpProducerFactory factory) {
        return new HttpSender(factory);
    }

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public HttpProducerFactory onsHttpProducerFactory() {
        log.debug("config HttpProducerFactory.");
        HttpProducerFactory factory = new HttpProducerFactory(OnsHttpClientCache.getCache(), onsBaseConfig.getDefaultAccess(), httpProducerConfig.getProducers(), onsContextAware, taskExecutor);
        log.debug("config HttpProducerFactory finished.");
        return factory;
    }
}
