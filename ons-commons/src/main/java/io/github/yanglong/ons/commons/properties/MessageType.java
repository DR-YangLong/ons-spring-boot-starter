package io.github.yanglong.ons.commons.properties;

/**
 * Description: 消息类型
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/2/23
 */
public enum MessageType {
    /**
     * 普通消息。
     * 用于生产者时包含普通消息，延时消息，定时消息；
     * 用于消费者时，包含普通消息，事务消息，延时消息，定时消息，批量消息。
     */
    NORMAL,
    /**
     * 顺序消息，用于生产者和消费者
     */
    ORDER,
    /**
     * 事务消息，用于生产者
     */
    TRANSACTION
}
