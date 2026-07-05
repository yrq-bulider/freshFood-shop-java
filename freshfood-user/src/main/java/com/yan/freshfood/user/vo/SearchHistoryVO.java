package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "搜索历史")
public class SearchHistoryVO {

    @Schema(description = "历史记录 ID")
    private Long id;

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "搜索时间")
    private LocalDateTime createTime;
}