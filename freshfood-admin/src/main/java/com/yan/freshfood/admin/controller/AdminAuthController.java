package com.yan.freshfood.admin.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.admin.dto.AdminLoginDTO;
import com.yan.freshfood.admin.service.AdminAuthService;
import com.yan.freshfood.admin.vo.AdminLoginVO;
import com.yan.freshfood.common.response.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaIgnore
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService authService;

    @PostMapping("/login")
    public R<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}