package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.SkuCreateDTO;
import com.yan.freshfood.merchant.dto.SkuUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantSkuService;
import com.yan.freshfood.merchant.vo.SkuVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
public class MerchantSkuController {

    private final MerchantSkuService merchantSkuService;

    @GetMapping("/products/{productId}/skus")
    public R<List<SkuVO>> list(@PathVariable Long productId) {
        return R.ok(merchantSkuService.list(productId));
    }

    @PostMapping("/products/{productId}/skus")
    public R<SkuVO> create(@PathVariable Long productId,
                           @Valid @RequestBody SkuCreateDTO dto) {
        return R.ok(merchantSkuService.create(productId, dto));
    }

    @PutMapping("/skus/{id}")
    public R<SkuVO> update(@PathVariable Long id,
                           @Valid @RequestBody SkuUpdateDTO dto) {
        return R.ok(merchantSkuService.update(id, dto));
    }

    @DeleteMapping("/skus/{id}")
    public R<Void> delete(@PathVariable Long id) {
        merchantSkuService.delete(id);
        return R.ok();
    }
}
