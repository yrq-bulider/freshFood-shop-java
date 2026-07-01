package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminStatusDTO {

    @NotNull(message = "状态不能为空")
    @Pattern(regexp = "^[01]$", message = "状态必须为 0 或 1")
    private Integer status;
}
