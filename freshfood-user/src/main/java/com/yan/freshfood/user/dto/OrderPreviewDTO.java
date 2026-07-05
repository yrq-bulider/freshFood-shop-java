package com.yan.freshfood.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "下单预览请求：计算金额/运费/可用优惠")
public class OrderPreviewDTO {
    @NotEmpty(message = "购物车项不能为空")
    @Schema(description = "要结算的购物车项 ID 列表", example = "[101, 102]")
    private List<Long> cartIds;

    @NotNull(message = "地址不能为空")
    @Schema(description = "收货地址 ID", example = "1")
    private Long addressId;
}