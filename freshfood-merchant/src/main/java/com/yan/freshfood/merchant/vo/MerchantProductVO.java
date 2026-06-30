package com.yan.freshfood.merchant.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家端商品视图（列表 + 简版详情）。
 * 价格区间从 SKU 聚合；库存为该商品所有 SKU 之和。
 */
@Data
public class MerchantProductVO {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String mainImage;
    private String description;
    private String origin;
    /** 价格区间，如 "59.90~109.00"；无 SKU 时为 null */
    private String priceRange;
    /** 总库存（所有 SKU stock 之和） */
    private Integer stock;
    private Integer sales;
    private Integer auditStatus;
    private Integer status;
    private LocalDateTime createTime;
}
