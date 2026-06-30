package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.MerchantUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantProfileService;
import com.yan.freshfood.merchant.vo.MerchantVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/profile")
@RequiredArgsConstructor
public class MerchantProfileController {

    private final MerchantProfileService merchantProfileService;

    @GetMapping
    public R<MerchantVO> get() {
        return R.ok(merchantProfileService.getProfile());
    }

    @PutMapping
    public R<MerchantVO> update(@Valid @RequestBody MerchantUpdateDTO dto) {
        return R.ok(merchantProfileService.updateProfile(dto));
    }
}