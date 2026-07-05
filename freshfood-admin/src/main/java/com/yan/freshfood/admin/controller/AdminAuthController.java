package com.yan.freshfood.admin.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.admin.dto.AdminLoginDTO;
import com.yan.freshfood.admin.service.AdminAuthService;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.common.response.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-认证", description = "管理员登录、登出")
@SaIgnore
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;

    /**
     * @deprecated 请使用 {@code POST /api/v1/auth/login}（UnifiedAuthController 统一入口）。
     *             本端点保留以做向后兼容，下个版本移除。
     */
    @Deprecated
    @PostMapping("/login")
    @Operation(summary = "管理员登录（兼容老端点）", description = "推荐使用统一登录接口 /api/v1/auth/login")
    public R<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "管理员登出")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}