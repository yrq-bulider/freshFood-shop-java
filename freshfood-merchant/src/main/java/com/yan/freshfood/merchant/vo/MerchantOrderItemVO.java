package com.yan.freshfood.merchant.vo;

import lombok.Data;

/**
 * 商家视角的订单明细。
 */
@Data
public class MerchantOrderItemVO {
    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private String spec;
    /** 快照价, String */
    private String price;
    private Integer quantity;
}
