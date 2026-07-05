package com.yan.freshfood.admin.controller;

import com.yan.freshfood.admin.dto.UserStatusDTO;
import com.yan.freshfood.admin.service.UserAdminService;
import com.yan.freshfood.admin.vo.AdminUserVO;
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

@Tag(name = "管理端-用户管理", description = "C 端用户分页查询、启停")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    @Operation(summary = "用户分页")
    public R<PageR<AdminUserVO>> page(
            @Parameter(description = "用户名/昵称/手机号模糊") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(userAdminService.page(keyword, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public R<AdminUserVO> detail(@Parameter(description = "用户 ID") @PathVariable Long id) {
        return R.ok(userAdminService.detail(id));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "启停用户账号")
    public R<Void> updateStatus(@Parameter(description = "用户 ID") @PathVariable Long id,
                                @Valid @RequestBody UserStatusDTO dto) {
        userAdminService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码", description = "重置为默认密码 123456")
    public R<Void> resetPassword(@Parameter(description = "用户 ID") @PathVariable Long id) {
        userAdminService.resetPassword(id);
        return R.ok();
    }
}