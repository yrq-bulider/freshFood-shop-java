package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "下单预览响应")
public class OrderPreviewVO {

    @Schema(description = "订单明细预览")
    private List<OrderItemVO> items;

    @Schema(description = "收货地址（按 addressId 解析）")
    private AddressVO address;

    @Schema(description = "商品总额")
    private String totalAmount;

    @Schema(description = "运费")
    private String shippingFee;

    @Schema(description = "优惠金额")
    private String discountAmount;

    @Schema(description = "应付金额")
    private String payableAmount;

    @Schema(description = "可用优惠券（当前固定为空数组，预留）")
    private List<Object> availableCoupons;
}