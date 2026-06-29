package com.yan.freshfood.user.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSimpleVO {
    private Long productId;
    private String name;
    private String mainImage;
    private String origin;
    private BigDecimal minPrice;
    private Integer sales;
    private BigDecimal rating;
}
