package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.MerchantAuditDTO;
import com.yan.freshfood.admin.dto.MerchantStatusDTO;
import com.yan.freshfood.admin.service.MerchantAdminService;
import com.yan.freshfood.admin.vo.AdminMerchantVO;
import com.yan.freshfood.admin.vo.AuditPendingVO;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
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
    public R<PageR<AdminMerchantVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantAdminService.page(keyword, auditStatus, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<AdminMerchantVO> detail(@PathVariable Long id) {
        return R.ok(merchantAdminService.detail(id));
    }

    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody MerchantAuditDTO dto) {
        merchantAdminService.audit(id, dto.getAuditStatus());
        return R.ok();
    }

    @PostMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody MerchantStatusDTO dto) {
        merchantAdminService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @GetMapping("/audit-pending")
    public R<AuditPendingVO> auditPendingCount() {
        return R.ok(merchantAdminService.auditPendingCount());
    }
}