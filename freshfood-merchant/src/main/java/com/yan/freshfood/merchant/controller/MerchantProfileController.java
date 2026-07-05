package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.MerchantUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantProfileService;
import com.yan.freshfood.merchant.vo.MerchantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家端-店铺资料", description = "商家店铺信息维护、资质提交")
@RestController
@RequestMapping("/api/v1/merchant/profile")
@RequiredArgsConstructor
public class MerchantProfileController {

    private final MerchantProfileService merchantProfileService;

    @GetMapping
    @Operation(summary = "我的店铺资料")
    public R<MerchantVO> get() {
        return R.ok(merchantProfileService.getProfile());
    }

    @PutMapping
    @Operation(summary = "更新店铺资料")
    public R<MerchantVO> update(@Valid @RequestBody MerchantUpdateDTO dto) {
        return R.ok(merchantProfileService.updateProfile(dto));
    }
}