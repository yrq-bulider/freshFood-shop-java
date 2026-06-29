package com.yan.freshfood.model.entity.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yan.freshfood.model.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class MessageDO extends BaseDO {
    private Long userId;
    /** SYSTEM/ORDER/PROMO */
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    /** 0 未读 / 1 已读 */
    private Integer isRead;
}