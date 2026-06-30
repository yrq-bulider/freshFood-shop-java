package com.yan.freshfood.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductAuditDTO {
    /** 1 通过 / 2 拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Integer auditStatus;
}
