package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class CartItemVO {
    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private String spec;
    private String price;
    private Integer quantity;
    private Boolean selected;
    private Boolean valid;
    private Integer stock;
    private String mainImage;
}