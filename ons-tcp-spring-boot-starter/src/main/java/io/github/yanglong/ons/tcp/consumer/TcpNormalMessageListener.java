package io.github.yanglong.ons.tcp.consumer;

import com.aliyun.openservices.ons.api.MessageListener;
import io.github.yanglong.ons.commons.listener.OnsMessageListener;

/**
 * Description: 普通消息处理监听器
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @since 2021/2/23
 */
public interface TcpNormalMessageListener extends OnsMessageListener, MessageListener {
}
