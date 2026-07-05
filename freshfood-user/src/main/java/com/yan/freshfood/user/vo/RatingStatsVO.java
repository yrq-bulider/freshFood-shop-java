package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "评价统计汇总")
public class RatingStatsVO {

    @Schema(description = "平均评分")
    private BigDecimal average;

    @Schema(description = "评价总数")
    private Integer total;

    @Schema(description = "5 星评价数")
    private Integer five;

    @Schema(description = "4 星评价数")
    private Integer four;

    @Schema(description = "3 星评价数")
    private Integer three;

    @Schema(description = "2 星评价数")
    private Integer two;

    @Schema(description = "1 星评价数")
    private Integer one;
}