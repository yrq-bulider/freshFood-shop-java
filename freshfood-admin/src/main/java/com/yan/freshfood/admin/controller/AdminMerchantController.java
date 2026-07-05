package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.MerchantAuditDTO;
import com.yan.freshfood.admin.dto.MerchantStatusDTO;
import com.yan.freshfood.admin.service.MerchantAdminService;
import com.yan.freshfood.admin.vo.AdminMerchantVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-商家审核", description = "商家入驻资质审核、商家管理")
@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantAdminService merchantAdminService;

    @GetMapping
    @Operation(summary = "商家分页")
    public R<PageR<AdminMerchantVO>> page(
            @Parameter(description = "店铺名/用户名模糊") @RequestParam(required = false) String keyword,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer auditStatus,
            @Parameter(description = "启用状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantAdminService.page(keyword, auditStatus, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商家详情")
    public R<AdminMerchantVO> detail(@Parameter(description = "商家 ID") @PathVariable Long id) {
        return R.ok(merchantAdminService.detail(id));
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "商家资质审核", description = "auditStatus=1 通过；=2 拒绝")
    public R<Void> audit(@Parameter(description = "商家 ID") @PathVariable Long id,
                         @Valid @RequestBody MerchantAuditDTO dto) {
        merchantAdminService.audit(id, dto.getAuditStatus());
        return R.ok();
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "启停商家账号")
    public R<Void> updateStatus(@Parameter(description = "商家 ID") @PathVariable Long id,
                                @Valid @RequestBody MerchantStatusDTO dto) {
        merchantAdminService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @GetMapping("/audit-pending")
    @Operation(summary = "待审核商家数量")
    public R<AuditPendingVO> auditPendingCount() {
        return R.ok(merchantAdminService.auditPendingCount());
    }
}