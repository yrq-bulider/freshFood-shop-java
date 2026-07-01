package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.ProductAuditDTO;
import com.yan.freshfood.admin.service.ProductAdminService;
import com.yan.freshfood.admin.vo.AdminProductVO;
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

@Tag(name = "管理端-商品审核", description = "商品上架审核、商品管理")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductAdminService productAdminService;

    @GetMapping
    public R<PageR<AdminProductVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(productAdminService.page(keyword, auditStatus, status, merchantId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<AdminProductVO> detail(@PathVariable Long id) {
        return R.ok(productAdminService.detail(id));
    }

    @PostMapping("/{id}/audit")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody ProductAuditDTO dto) {
        productAdminService.audit(id, dto.getAuditStatus());
        return R.ok();
    }

    @PostMapping("/{id}/off-shelf")
    public R<Void> offShelf(@PathVariable Long id) {
        productAdminService.offShelf(id);
        return R.ok();
    }

    @GetMapping("/audit-pending")
    public R<AuditPendingVO> auditPendingCount() {
        return R.ok(productAdminService.auditPendingCount());
    }
}
