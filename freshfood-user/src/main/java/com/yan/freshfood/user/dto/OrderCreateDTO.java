package com.yan.freshfood.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {
    @NotEmpty
    private List<Long> cartIds;

    @NotNull
    private Long addressId;

    private Long couponId;   // 可选
    private String remark;
}