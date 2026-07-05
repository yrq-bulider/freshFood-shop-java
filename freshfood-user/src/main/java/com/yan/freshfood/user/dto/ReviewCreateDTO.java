package com.yan.freshfood.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "发表评价请求")
public class ReviewCreateDTO {
    @NotNull
    @Schema(description = "订单 ID", example = "8888")
    private Long orderId;

    @NotNull
    @Schema(description = "订单明细 ID", example = "9001")
    private Long orderItemId;

    @NotNull
    @Min(1) @Max(5)
    @Schema(description = "综合评分 1-5", example = "5")
    private Integer rating;

    @Min(1) @Max(5)
    @Schema(description = "口味评分 1-5")
    private Integer tasteRating;

    @Min(1) @Max(5)
    @Schema(description = "新鲜度评分 1-5")
    private Integer freshnessRating;

    @Min(1) @Max(5)
    @Schema(description = "物流评分 1-5")
    private Integer logisticsRating;

    @NotBlank
    @Size(max = 1000)
    @Schema(description = "评价内容", example = "非常新鲜，配送及时")
    private String content;

    @Schema(description = "评价图片 URL 列表")
    private List<String> images;
}
