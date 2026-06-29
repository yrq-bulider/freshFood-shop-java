package com.yan.freshfood.model.entity.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review")
public class ReviewDO extends BaseDO {
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long productId;
    private Long skuId;
    private Long merchantId;
    /** 1-5 */
    private Integer rating;
    private Integer tasteRating;
    private Integer freshnessRating;
    private Integer logisticsRating;
    private String content;
    private String images;
    private String merchantReply;
    private LocalDateTime replyTime;
    /** 0 首评 / 1 追评 */
    private Integer isAppend;
    /** 0 隐藏 / 1 显示 */
    private Integer status;
}