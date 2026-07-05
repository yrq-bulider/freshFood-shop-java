package com.yan.freshfood.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新购物车数量请求")
public class CartUpdateDTO {
    @NotNull
    @Min(value = 1, message = "数量至少 1")
    @Schema(description = "新的数量", example = "3")
    private Integer quantity;
}