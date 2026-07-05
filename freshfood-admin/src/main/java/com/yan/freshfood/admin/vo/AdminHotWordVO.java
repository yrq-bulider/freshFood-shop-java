package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "搜索热词信息")
public class AdminHotWordVO {
    @Schema(description = "热词 ID")
    private Long id;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "搜索次数")
    private Integer searchCount;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
