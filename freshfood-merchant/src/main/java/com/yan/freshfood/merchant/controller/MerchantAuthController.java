package com.yan.freshfood.merchant.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.dto.MerchantRegisterDTO;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import com.yan.freshfood.merchant.vo.MerchantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商家端-账号", description = "商家入驻、登录、登出")
@SaIgnore
@RestController
@RequestMapping("/api/v1/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "商家入驻申请", description = "注册成功后默认 auditStatus=0 待审核，平台审核通过后才能登录。无需登录")
    public R<MerchantVO> register(@Valid @RequestBody MerchantRegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    /**
     * @deprecated 请使用 {@code POST /api/v1/auth/login}（UnifiedAuthController 统一入口）。
     *             本端点保留以做向后兼容，下个版本移除。
     */
    @Deprecated
    @PostMapping("/login")
    @Operation(summary = "商家登录（兼容老端点）", description = "推荐使用统一登录接口 /api/v1/auth/login")
    public R<MerchantLoginVO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @PostMapping("/logout")
    @Operation(summary = "商家登出")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }
}