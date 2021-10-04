package io.github.yanglong.ons.http;

import com.aliyun.mq.http.MQClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: HTTP模式下，相同ak,sk,host的客户端可以共享
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 21:02 下午
 */
public class OnsHttpClientCache {
    /**
     * HTTP客户端，producer和client通用，合并到一起，便于复用
     */
    private static final Map<String, MQClient> ONS_CLIENT_CACHE = new ConcurrentHashMap<>(64);

    /**
     * 获取缓存中的客户端
     *
     * @param key key
     * @return MQClient
     */
    public static MQClient getClient(String key) {
        return ONS_CLIENT_CACHE.get(key);
    }

    /**
     * 获取缓存本身
     *
     * @return ONS_CLIENT_CACHE
     */
    public static Map<String, MQClient> getCache() {
        return ONS_CLIENT_CACHE;
    }
}
