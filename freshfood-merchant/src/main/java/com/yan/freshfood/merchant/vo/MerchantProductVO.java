package com.yan.freshfood.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家端商品视图（列表 + 简版详情）。
 * 价格区间从 SKU 聚合；库存为该商品所有 SKU 之和。
 */
@Data
@Schema(description = "商品视图（商家端）")
public class MerchantProductVO {

    @Schema(description = "商品 ID")
    private Long id;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分类名")
    private String categoryName;

    @Schema(description = "主图 URL")
    private String mainImage;

    @Schema(description = "商品描述")
    private String description;

    @Schema(description = "产地")
    private String origin;

    @Schema(description = "价格区间，如 \"59.90\" 或 \"59.90~109.00\"；无 SKU 时为 null")
    private String priceRange;

    @Schema(description = "总库存（所有 SKU stock 之和）")
    private Integer stock;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "审核状态：0=待审 1=通过 2=拒绝")
    private Integer auditStatus;

    @Schema(description = "上下架状态：0=下架 1=上架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}