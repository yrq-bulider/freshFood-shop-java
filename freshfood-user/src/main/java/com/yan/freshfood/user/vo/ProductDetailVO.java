package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductDetailVO {
    private Long productId;
    private String name;
    private String mainImage;
    private Long categoryId;
    private Long merchantId;
    private String origin;
    private List<String> afterSalesTags;
    private String description;
    private List<SkuVO> skus;
    private List<SpecVO> specs;
    private RatingStatsVO ratingStats;
}