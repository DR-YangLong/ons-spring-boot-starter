package io.github.yanglong.ons.tcp.sample;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Description:
 *
 * @author YangLong [410357434@163.com]
 * @version V1.0
 * @date 2021/3/17
 */
@Data
@ApiModel("消息")
public class MessageVO {
    @ApiModelProperty(value = "topic", allowableValues = "normal_dev,trans_dev,time_dev,order_global_dev,order_sharding_dev", required = true)
    private String topic;
    @ApiModelProperty(value = "消息内容", required = true)
    private String body;
    @ApiModelProperty("标签")
    private String tags;
    @ApiModelProperty("业务key")
    private String key;
    @ApiModelProperty("顺序消息时的shardingKey")
    private String shardingKey;
    @ApiModelProperty("延迟消息的延迟时间")
    private Long delayTime;
}
