package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "系统通知")
public class MessageVO {

    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "消息类型：ORDER/PROMO/SYSTEM")
    private String type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "关联业务 ID（如订单 ID）")
    private Long relatedId;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}