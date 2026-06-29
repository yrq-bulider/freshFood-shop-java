package com.yan.freshfood.user.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {
    private Long id;
    private String spec;
    private String price;
    private Integer stock;
    private Integer sales;
    private String image;
}