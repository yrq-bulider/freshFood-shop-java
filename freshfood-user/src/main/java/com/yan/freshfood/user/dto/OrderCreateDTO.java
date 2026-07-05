package com.yan.freshfood.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "下单请求")
public class OrderCreateDTO {
    @NotEmpty
    @Schema(description = "要结算的购物车项 ID 列表", example = "[101, 102]")
    private List<Long> cartIds;

    @NotNull
    @Schema(description = "收货地址 ID", example = "1")
    private Long addressId;

    @Schema(description = "优惠券 ID（可选，未接入优惠时忽略）")
    private Long couponId;

    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;
}