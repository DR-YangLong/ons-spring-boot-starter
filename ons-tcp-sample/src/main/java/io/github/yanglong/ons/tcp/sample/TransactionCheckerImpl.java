package io.github.yanglong.ons.tcp.sample;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import org.springframework.stereotype.Component;

/**
 * Description: 测试TCP发送者事务LocalTransactionChecker
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/18
 */
@Component
public class TransactionCheckerImpl implements LocalTransactionChecker {

    @Override
    public TransactionStatus check(Message msg) {
        return TransactionStatus.CommitTransaction;
    }
}
