package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusDTO {
    /** 0 禁用 / 1 启用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
