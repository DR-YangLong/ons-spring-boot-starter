package io.github.yanglong.ons.http;

import com.aliyun.mq.http.MQClient;
import io.github.yanglong.ons.commons.factory.OnsFactory;
import io.github.yanglong.ons.commons.properties.OnsAccessProperties;
import io.github.yanglong.ons.commons.properties.OnsCommonProperties;
import io.github.yanglong.ons.commons.utils.OnsStringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * Description: HTTP客户端工厂
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/11
 */
@Slf4j
@Data
public abstract class AbstractHttpClientFactory implements OnsFactory {
    /**
     * ONS安全配置类
     */
    protected final OnsAccessProperties defaultAccessProperties;
    /**
     * 连接配置map
     */
    protected final Map<String, ? extends OnsCommonProperties> commonProperties;
    /**
     * MQClient缓存
     */
    private final Map<String, MQClient> clientCache;


    public AbstractHttpClientFactory(Map<String, MQClient> clientCache, OnsAccessProperties defaultAccessProperties, Map<String, ? extends OnsCommonProperties> commonProperties) {
        this.clientCache = clientCache;
        this.defaultAccessProperties = defaultAccessProperties;
        this.commonProperties = commonProperties;
    }

    @Override
    public void init() {
        //生成所有HTTP的client，不生成producer,consumer
        if (CollectionUtils.isEmpty(commonProperties)) {
            log.error("this is no HTTP ons client created!");
            return;
        }
        synchronized (this) {
            commonProperties.forEach((name, properties) -> {
                properties.setConfigName(name);
                getClient(defaultAccessProperties.getAccessKey(), defaultAccessProperties.getSecretKey(), properties.getNameServer());
            });
        }
    }

    @Override
    public void shutdown() {
        //关闭客户端
        if (!CollectionUtils.isEmpty(clientCache)) {
            clientCache.forEach((name, client) -> {
                log.info("close HTTP MQClient {}.", name);
                client.close();
            });
        }
    }

    /**
     * 从clients缓存中获取MQClient，如果没有，生成一个并放入缓存
     *
     * @param accessKey  ak
     * @param secretKey  sk
     * @param nameServer 接入点
     * @return MQClient
     */
    protected MQClient getClient(final String accessKey, final String secretKey, final String nameServer) {
        if (OnsStringUtils.isAnyEmpty(accessKey, secretKey, nameServer)) {
            log.error("can't create MQClient,params error!he key is {},nameServer is {}.", accessKey, nameServer);
            return null;
        }
        String key = OnsStringUtils.generateKey(accessKey, secretKey, nameServer);
        MQClient client;
        synchronized (this) {
            //computeIfAbsent使用CAS操作，并发时不能避免创建多次，因此加个同步锁
            client = clientCache.computeIfAbsent(key, k -> new MQClient(nameServer, accessKey, secretKey));
        }
        log.info("create HTTP MQClient the key is {},nameServer is {}.", key, nameServer);
        return client;
    }
}
