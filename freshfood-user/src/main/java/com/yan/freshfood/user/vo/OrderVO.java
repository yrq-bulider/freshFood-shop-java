package com.yan.freshfood.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "订单详情（用户端）")
public class OrderVO {

    @Schema(description = "订单 ID")
    private Long id;

    @Schema(description = "业务订单号")
    private String orderId;

    @Schema(description = "订单状态：1=待付款 2=待发货 3=待收货 4=待评价 5=已完成 6=售后中 7=已取消")
    private Integer status;

    @Schema(description = "状态文字描述")
    private String statusText;

    @Schema(description = "订单总额（元，字符串）")
    private String totalAmount;

    @Schema(description = "运费（元，字符串）")
    private String shippingFee;

    @Schema(description = "优惠金额（元，字符串）")
    private String discountAmount;

    @Schema(description = "应付金额（元，字符串）")
    private String payableAmount;

    @Schema(description = "支付截止时间（待付款状态时使用）")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "订单明细列表")
    private List<OrderItemVO> items;

    @Schema(description = "收货地址")
    private AddressVO address;
}