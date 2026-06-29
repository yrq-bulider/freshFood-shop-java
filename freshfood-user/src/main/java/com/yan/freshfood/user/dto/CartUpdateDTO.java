package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartUpdateDTO {
    @NotNull
    @Min(value = 1, message = "数量至少 1")
    private Integer quantity;
}