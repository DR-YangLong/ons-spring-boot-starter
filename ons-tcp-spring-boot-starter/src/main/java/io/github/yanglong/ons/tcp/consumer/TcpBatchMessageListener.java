package io.github.yanglong.ons.tcp.consumer;

import com.aliyun.openservices.ons.api.batch.BatchMessageListener;
import io.github.yanglong.ons.commons.listener.OnsMessageListener;

/**
 * Description:ons批量消息处理监听器
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
public interface TcpBatchMessageListener extends OnsMessageListener, BatchMessageListener {
}
