package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "搜索热词")
public class HotWordVO {

    @Schema(description = "热词 ID")
    private Long id;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "搜索次数（用于排序）")
    private Integer searchCount;
}