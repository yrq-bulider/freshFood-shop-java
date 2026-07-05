package com.yan.freshfood.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yan.freshfood.admin.dto.AdminCreateDTO;
import com.yan.freshfood.admin.dto.AdminResetPasswordDTO;
import com.yan.freshfood.admin.dto.AdminStatusDTO;
import com.yan.freshfood.admin.dto.AdminUpdateDTO;
import com.yan.freshfood.admin.service.AdminAccountService;
import com.yan.freshfood.admin.vo.AdminAccountVO;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-账号管理", description = "管理员账号 CRUD、启停、重置密码、删除")
@RestController
@RequestMapping("/api/v1/admin/admins")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    @Operation(summary = "管理员分页")
    public R<PageR<AdminAccountVO>> page(
            @Parameter(description = "用户名/昵称模糊") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "20") long pageSize) {
        IPage<AdminAccountVO> page = adminAccountService.page(keyword, status, pageNum, pageSize);
        return R.ok(PageR.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "管理员详情")
    public R<AdminAccountVO> detail(@Parameter(description = "管理员 ID") @PathVariable Long id) {
        return R.ok(adminAccountService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新建管理员")
    public R<AdminAccountVO> create(@Valid @RequestBody AdminCreateDTO dto) {
        return R.ok(adminAccountService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑管理员")
    public R<AdminAccountVO> update(@Parameter(description = "管理员 ID") @PathVariable Long id,
                                    @Valid @RequestBody AdminUpdateDTO dto) {
        return R.ok(adminAccountService.update(id, dto));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "启停管理员账号")
    public R<Void> updateStatus(@Parameter(description = "管理员 ID") @PathVariable Long id,
                                @Valid @RequestBody AdminStatusDTO dto) {
        adminAccountService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置管理员密码")
    public R<Void> resetPassword(@Parameter(description = "管理员 ID") @PathVariable Long id,
                                  @Valid @RequestBody AdminResetPasswordDTO dto) {
        adminAccountService.resetPassword(id, dto.getPassword());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除管理员", description = "id=1 的超级管理员不允许删除")
    public R<Void> delete(@Parameter(description = "管理员 ID") @PathVariable Long id) {
        adminAccountService.delete(id);
        return R.ok();
    }
}