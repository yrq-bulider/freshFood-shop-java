package com.yan.freshfood.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 审核待办数量 */
@Data
@AllArgsConstructor
@Schema(description = "审核待办数量")
public class AuditPendingVO {

    @Schema(description = "待审数量")
    private Long count;
}