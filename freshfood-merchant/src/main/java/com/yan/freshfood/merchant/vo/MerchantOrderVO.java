package com.yan.freshfood.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "订单详情（商家端）")
public class MerchantOrderVO {

    @Schema(description = "订单 ID")
    private Long id;

    @Schema(description = "业务订单号")
    private String orderNo;

    @Schema(description = "订单状态：1=待付款 2=待发货 3=待收货 4=已完成 5=已取消")
    private Integer status;

    @Schema(description = "状态文字描述")
    private String statusText;

    @Schema(description = "订单总额（元，字符串）")
    private String totalAmount;

    @Schema(description = "应付金额（元，字符串）")
    private String payableAmount;

    @Schema(description = "订单明细列表")
    private List<MerchantOrderItemVO> items;

    @Schema(description = "收货人姓名")
    private String receiverName;

    @Schema(description = "收货人手机号")
    private String receiverPhone;

    @Schema(description = "收货地址")
    private String receiverAddress;

    @Schema(description = "买家备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "发货时间")
    private LocalDateTime shipTime;

    @Schema(description = "物流单号")
    private String trackingNo;

    @Schema(description = "物流公司")
    private String carrier;
}