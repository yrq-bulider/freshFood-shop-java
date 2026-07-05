package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "商品评价")
public class ReviewVO {

    @Schema(description = "评价 ID")
    private Long id;

    @Schema(description = "用户昵称（脱敏后）")
    private String username;

    @Schema(description = "用户头像 URL")
    private String avatar;

    @Schema(description = "综合评分 1-5")
    private Integer rating;

    @Schema(description = "口味评分 1-5")
    private Integer tasteRating;

    @Schema(description = "新鲜度评分 1-5")
    private Integer freshnessRating;

    @Schema(description = "物流评分 1-5")
    private Integer logisticsRating;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "评价图片 URL 列表")
    private List<String> images;

    @Schema(description = "商家回复")
    private String merchantReply;

    @Schema(description = "评价时间")
    private LocalDateTime createTime;
}