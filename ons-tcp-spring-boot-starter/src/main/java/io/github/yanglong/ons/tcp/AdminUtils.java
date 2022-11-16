package io.github.yanglong.ons.tcp;

import com.aliyun.openservices.ons.api.Admin;

/**
 * Description: 辅助工具类
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @since 2021/3/3
 */
public class AdminUtils {
    /**
     * 检查生产者/消费者是否已经就绪
     *
     * @param admin 生产者/消费者实例
     * @return true-就绪
     */
    public static boolean isInstanceReady(Admin admin) {
        return null != admin && admin.isStarted();
    }

    /**
     * 关闭生产者或消费者
     *
     * @param admin 产者/消费者实例
     */
    public static void closeInstance(Admin admin) {
        if (null != admin && !admin.isClosed()) {
            admin.shutdown();
        }
    }

    /**
     * 启动生产者或消费者
     *
     * @param admin 实例
     */
    public static void startInstance(Admin admin) {
        if (null != admin && !admin.isStarted()) {
            admin.start();
        }
    }
}
