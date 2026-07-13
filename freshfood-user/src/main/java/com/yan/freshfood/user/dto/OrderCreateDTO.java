package com.yan.freshfood.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "下单请求")
public class OrderCreateDTO {
    @NotEmpty
    @Schema(description = "要结算的购物车项 ID 列表", example = "[101, 102]")
    private List<Long> cartIds;

    @NotBlank
    @Schema(description = "收货人姓名", example = "张三")
    private String receiverName;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "收货人手机号", example = "13800138000")
    private String receiverPhone;

    @NotBlank
    @Schema(description = "收货地址", example = "北京市朝阳区某街道 1 号")
    private String receiverAddress;

    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;
}