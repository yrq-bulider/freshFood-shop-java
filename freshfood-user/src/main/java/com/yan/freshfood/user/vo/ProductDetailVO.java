package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "商品详情（用户端）")
public class ProductDetailVO {

    @Schema(description = "商品 ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "主图 URL")
    private String mainImage;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "商家 ID")
    private Long merchantId;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "售后标签列表（如 \"坏品包赔\"、\"次日达\"）")
    private List<String> afterSalesTags;

    @Schema(description = "商品详细描述")
    private String description;

    @Schema(description = "SKU 列表（用于选规格）")
    private List<SkuVO> skus;

    @Schema(description = "规格维度列表（用于渲染规格选择器）")
    private List<SpecVO> specs;

    @Schema(description = "评价统计汇总")
    private RatingStatsVO ratingStats;
}