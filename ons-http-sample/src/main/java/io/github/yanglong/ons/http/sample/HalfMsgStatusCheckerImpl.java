package io.github.yanglong.ons.http.sample;

import io.github.yanglong.ons.http.producer.HalfMsgStatusChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Description: HTTP发送端事务确认实现
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/18
 */
@Slf4j
@Component
public class HalfMsgStatusCheckerImpl implements HalfMsgStatusChecker {

    @Override
    public int checkTransactionStatus(String key, Map<String, String> custom) {
        log.info("receive msg,key={}", key);
        //模拟事务状态
        Long status = System.currentTimeMillis() % 2;
        log.info("the message key={},transaction status is {}.", key, status);
        return status.intValue();
    }
}
