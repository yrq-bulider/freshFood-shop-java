package com.yan.freshfood.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "加入购物车请求")
public class CartAddDTO {
    @NotNull(message = "skuId 不能为空")
    @Schema(description = "SKU ID", example = "2001")
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少 1")
    @Schema(description = "数量", example = "2")
    private Integer quantity;
}