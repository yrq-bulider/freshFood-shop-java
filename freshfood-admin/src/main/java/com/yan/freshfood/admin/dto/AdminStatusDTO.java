package com.yan.freshfood.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "管理员启停请求")
public class AdminStatusDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态必须为 0 或 1")
    @Max(value = 1, message = "状态必须为 0 或 1")
    @Schema(description = "状态：0=禁用 1=启用", example = "1")
    private Integer status;
}
