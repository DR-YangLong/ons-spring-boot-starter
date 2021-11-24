package io.github.yanglong.ons.http.consumer;

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
 * Description: ONS HTTP自动配置消费者
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 19:39 下午
 */
@Slf4j
@Data
@Configuration
@ConditionalOnProperty(prefix = "ali-ons", name = {"enable", "http.consumer.enable"}, havingValue = "true")
@ConditionalOnBean(name = {"taskExecutor", "onsContextAware"})
@EnableConfigurationProperties({HttpConsumerConfig.class, OnsBaseConfig.class})
public class HttpConsumerAutoConfiguration {
    @Autowired
    private OnsBaseConfig onsBaseConfig;
    @Autowired
    private HttpConsumerConfig httpConsumerConfig;
    @Autowired
    private OnsContextAware onsContextAware;
    @Autowired
    private TaskExecutor taskExecutor;

    /**
     * 生成ONS HTTP消费者工厂
     *
     * @return HttpConsumerFactory
     */
    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public HttpConsumerFactory onsHttpConsumerFactory() {
        log.debug("config HttpConsumerFactory.");
        HttpConsumerFactory consumerFactory = new HttpConsumerFactory(OnsHttpClientCache.getCache(), onsBaseConfig.getDefaultAccess(), httpConsumerConfig.getConsumers(), onsContextAware, taskExecutor);
        log.debug("config HttpConsumerFactory finished.");
        return consumerFactory;
    }
}
