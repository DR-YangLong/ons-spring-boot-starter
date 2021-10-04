package io.github.yanglong.ons.http;

/**
 * Description: ons http客户端异常
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/4/16 1:46 下午
 */
public class OnsHttpException extends Exception {
    public OnsHttpException() {
        super();
    }

    public OnsHttpException(String message) {
        super(message);
    }
}
