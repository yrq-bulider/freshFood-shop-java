package com.yan.freshfood.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class OrderPreviewVO {
    private List<OrderItemVO> items;
    private AddressVO address;
    private String totalAmount;
    private String shippingFee;
    private String discountAmount;
    private String payableAmount;
    private List<Object> availableCoupons;  // 简化为空数组
}