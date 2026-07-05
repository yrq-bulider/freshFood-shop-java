package com.yan.freshfood.app.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.app.service.UnifiedAuthService;
import com.yan.freshfood.app.vo.UnifiedLoginVO;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.LoginDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "统一登录", description = "用户/商家/管理员统一登录入口")
@SaIgnore
@RestController
@RequiredArgsConstructor
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "统一登录", description = "按 user → merchant → admin 顺序匹配；返回 token、role、profile。无需登录鉴权。")
    public R<UnifiedLoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(unifiedAuthService.login(dto.getUsername(), dto.getPassword()));
    }
}