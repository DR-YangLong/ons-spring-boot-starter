package io.github.yanglong.ons.commons.factory;

/**
 * Description: ONS工厂父接口
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/9
 */
public interface OnsFactory {
    /**
     * 初始化实例
     */
    void init();

    /**
     * 关闭所有实例
     */
    void shutdown();
}
