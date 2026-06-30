package com.yan.freshfood.user.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderId;        // 业务订单号
    private Integer status;
    private String statusText;
    private String totalAmount;
    private String shippingFee;
    private String discountAmount;
    private String payableAmount;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private List<OrderItemVO> items;
    private AddressVO address;
}