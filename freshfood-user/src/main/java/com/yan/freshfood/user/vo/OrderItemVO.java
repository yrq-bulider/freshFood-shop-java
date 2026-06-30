package com.yan.freshfood.user.vo;

import lombok.Data;

@Data
public class OrderItemVO {
    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private String spec;
    private String price;
    private Integer quantity;
    private String mainImage;
}