package com.yan.freshfood.merchant.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.MerchantLoginDTO;
import com.yan.freshfood.merchant.service.MerchantAuthService;
import com.yan.freshfood.merchant.vo.MerchantLoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaIgnore
@RestController
@RequestMapping("/api/v1/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final MerchantAuthService authService;

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