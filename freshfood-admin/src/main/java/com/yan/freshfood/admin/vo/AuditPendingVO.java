package com.yan.freshfood.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 审核待办数量 */
@Data
@AllArgsConstructor
public class AuditPendingVO {
    private Long count;
}