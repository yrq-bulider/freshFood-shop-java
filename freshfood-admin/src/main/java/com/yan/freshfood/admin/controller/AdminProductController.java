package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.ProductAuditDTO;
import com.yan.freshfood.admin.service.ProductAdminService;
import com.yan.freshfood.admin.vo.AdminProductVO;
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

@Tag(name = "管理端-商品审核", description = "商品上架审核、商品管理")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductAdminService productAdminService;

    @GetMapping
    @Operation(summary = "商品分页")
    public R<PageR<AdminProductVO>> page(
            @Parameter(description = "商品名模糊") @RequestParam(required = false) String keyword,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer auditStatus,
            @Parameter(description = "上下架状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "商家 ID") @RequestParam(required = false) Long merchantId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(productAdminService.page(keyword, auditStatus, status, merchantId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public R<AdminProductVO> detail(@Parameter(description = "商品 ID") @PathVariable Long id) {
        return R.ok(productAdminService.detail(id));
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "商品审核", description = "auditStatus=1 通过；=2 拒绝")
    public R<Void> audit(@Parameter(description = "商品 ID") @PathVariable Long id,
                         @Valid @RequestBody ProductAuditDTO dto) {
        productAdminService.audit(id, dto.getAuditStatus());
        return R.ok();
    }

    @PostMapping("/{id}/off-shelf")
    @Operation(summary = "强制下架")
    public R<Void> offShelf(@Parameter(description = "商品 ID") @PathVariable Long id) {
        productAdminService.offShelf(id);
        return R.ok();
    }

    @GetMapping("/audit-pending")
    @Operation(summary = "待审核商品数量")
    public R<AuditPendingVO> auditPendingCount() {
        return R.ok(productAdminService.auditPendingCount());
    }
}