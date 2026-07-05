package com.yan.freshfood.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "商品审核请求")
public class ProductAuditDTO {
    /** 1 通过 / 2 拒绝 */
    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：1=通过 2=拒绝", example = "1")
    private Integer auditStatus;
}
