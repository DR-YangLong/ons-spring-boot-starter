package io.github.yanglong.ons.http.producer;

import java.util.Map;

/**
 * Description: 事务状态检查接口，用于HTTP事务状态回查
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/10
 */
public interface HalfMsgStatusChecker {

    /**
     * 半事务消息检查方法
     *
     * @param key    发送消息时设置的key
     * @param custom 发送消息时设置的自定义参数
     * @return 1-commit,0-rollback,其他-什么也不做
     */
    int checkTransactionStatus(String key, Map<String, String> custom);
}
