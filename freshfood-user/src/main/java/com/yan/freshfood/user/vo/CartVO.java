package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class CartVO {
    private List<CartItemVO> list;
    private String totalAmount;       // 全部商品金额（不含运费）
    private String selectedAmount;    // 已选商品金额
    private String shippingFee;
    private Integer invalidCount;
    private Integer selectedCount;
}