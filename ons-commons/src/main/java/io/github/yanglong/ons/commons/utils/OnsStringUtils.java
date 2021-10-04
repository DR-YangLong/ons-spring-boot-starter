package io.github.yanglong.ons.commons.utils;

import io.github.yanglong.ons.commons.properties.OnsAccessProperties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotEmpty;
import java.util.Objects;

/**
 * Description: 辅助工具类
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/3
 */
public class OnsStringUtils {
    public static final String COMMA = ",";

    /**
     * 检查参数必要信息是否完整,即检查传入的字符数组是不是每一个都不为空。
     *
     * @param args 字符串数组
     * @return 只要参数其中一个为空，返回false
     */
    public static boolean isAllNotEmpty(String... args) {
        boolean result = true;
        if (null != args && args.length > 0) {
            for (String param : args) {
                boolean empty = StringUtils.isEmpty(param);
                if (empty) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 检查参数列表中是不是有空的
     *
     * @param args 字符串数组
     * @return 如果其中一个参数为空，返回true
     */
    public static boolean isAnyEmpty(String... args) {
        return !isAllNotEmpty(args);
    }

    /**
     * 使用参数拼接的方式生成存储Client的key
     *
     * @param args 参与hash的参数
     * @return hash
     */
    public static String generateKey(String... args) {
        StringBuilder source = new StringBuilder();
        for (String arg : args) {
            source.append(arg);
        }
        return DigestUtils.md5Hex(source.toString());
    }

    /**
     * 将source中的sourceChar全部替换为targetChar，eg:
     * splitAndJoin("a,b,c,d,e",",","||")==>"a||b||c||d||e"
     *
     * @param source     原字符串
     * @param sourceChar 要被替换的字符
     * @param targetChar 替换成字符
     * @return String
     */
    public static String stringReplace(final String source, @NotEmpty final String sourceChar, @NotEmpty final String targetChar) {
        String target;
        if (StringUtils.isNotEmpty(source) && source.indexOf(sourceChar) > 0) {
            target = source.replaceAll(sourceChar, targetChar);
        } else {
            target = source;
        }
        return target;
    }

    /**
     * 检查OnsAccessProperties不为NULL且属性均有值
     *
     * @param accessProperties OnsAccessProperties
     * @return true-access配置可用，false-不可用
     */
    public static boolean checkAccess(OnsAccessProperties accessProperties) {
        return Objects.nonNull(accessProperties) && isAllNotEmpty(accessProperties.getAccessKey(), accessProperties.getSecretKey());
    }
}
