package io.github.yanglong.ons.http.producer;

import com.aliyun.mq.http.MQTransProducer;
import com.aliyun.mq.http.model.Message;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Description: 实现HTTP事务消费，用于确认事务消息事务状态,在事务消息发送客户端一侧进行事务提交，回滚。
 * 由于事务消息客户端是缓存在map中，因此，只要客户端不消除，则此线程存在，因此，需要对while(true)进行管理，
 * 在事务消息客户端关闭或刷新时，系统关闭时，中断循环，结束当前线程。
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/10
 */
@Slf4j
@Data
public class HalfMsgConfirmRunnable implements Runnable {
    /**
     * 生产者名字
     */
    private String producerName;
    /**
     * 生产者实例
     */
    private MQTransProducer mqTransProducer;
    /**
     * 半事务消息状态检查接口
     */
    private HalfMsgStatusChecker halfMsgStatusChecker;
    /**
     * 关闭状态，true-关闭，false-开启，默认开启
     */
    private boolean shutdown = false;

    public HalfMsgConfirmRunnable(String producerName, MQTransProducer mqTransProducer, HalfMsgStatusChecker halfMsgStatusChecker) {
        this.producerName = producerName;
        this.mqTransProducer = mqTransProducer;
        this.halfMsgStatusChecker = halfMsgStatusChecker;
    }

    @Override
    public void run() {
        //只要不关闭，就一直获取消息
        while (!shutdown) {
            try {
                //每3秒获取一次半事务消息，且最多获取3条
                List<Message> messages = mqTransProducer.consumeHalfMessage(3, 3);
                if (CollectionUtils.isEmpty(messages)) {
                    log.debug("No Half message!");
                    continue;
                }
                for (Message message : messages) {
                    String key = message.getMessageKey();
                    log.info("receive half message:[key:{},tag:{},msg:{},publishTime:{}]", key, message.getMessageTag(), message.getMessageBodyString(), message.getPublishTime());
                    Map<String, String> custom = message.getProperties();
                    try {
                        //检查事务状态，1提交，0回滚，其他不做任何操作
                        int status = halfMsgStatusChecker.checkTransactionStatus(key, custom);
                        switch (status) {
                            case 1: {
                                mqTransProducer.commit(message.getReceiptHandle());
                                log.info("producer {} commit message {}.", producerName, message.getMessageId());
                            }
                            break;
                            case 0: {
                                mqTransProducer.rollback(message.getReceiptHandle());
                                log.info("producer {} rollback message {}.", producerName, message.getMessageId());
                            }
                            break;
                            default:
                                log.info("producer {} received half message {},but checker return status is {},so do nothing!", producerName, message.getMessageId(), status);
                        }
                    } catch (Throwable e) {
                        // 如果Commit/Rollback时超过了TransCheckImmunityTime（针对发送事务消息的句柄）或者超过10s（针对consumeHalfMessage的句柄）则会失败
                        log.error("commit or rollback half message error! message id is {},the key is {}.", message.getMessageId(), key, e);
                    }
                }
            } catch (Throwable e) {
                log.error("pull half and check transaction status failed!", e);
            }
        }
    }
}