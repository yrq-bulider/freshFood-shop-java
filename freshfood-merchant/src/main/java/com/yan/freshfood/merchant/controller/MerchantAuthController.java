package com.yan.freshfood.merchant.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商家端-账号", description = "商家注册、登录、登出")
@SaIgnore
@RestController
@RequestMapping("/api/v1/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantAuthService authService;

    /**
     * @deprecated 请使用 {@code POST /api/v1/auth/login}（UnifiedAuthController 统一入口）。
     *             本端点保留以做向后兼容，下个版本移除。
     */
    @Deprecated
    @PostMapping("/login")
    public R<MerchantLoginVO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}