package com.yan.freshfood.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "分类启停请求")
public class CategoryStatusDTO {
    /** 0 禁用 / 1 启用 */
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0=禁用 1=启用", example = "1")
    private Integer status;
}
