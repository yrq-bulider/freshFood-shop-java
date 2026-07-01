package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.ProductCreateDTO;
import com.yan.freshfood.merchant.dto.ProductUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantProductService;
import com.yan.freshfood.merchant.vo.MerchantProductVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家端-商品", description = "商家发布、编辑、上下架商品")
@RestController
@RequestMapping("/api/v1/merchant/products")
@RequiredArgsConstructor
public class MerchantProductController {

    private final MerchantProductService merchantProductService;

    @GetMapping
    public R<PageR<MerchantProductVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantProductService.page(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<MerchantProductVO> detail(@PathVariable Long id) {
        return R.ok(merchantProductService.detail(id));
    }

    @PostMapping
    public R<MerchantProductVO> create(@Valid @RequestBody ProductCreateDTO dto) {
        return R.ok(merchantProductService.create(dto));
    }

    @PutMapping("/{id}")
    public R<MerchantProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateDTO dto) {
        dto.setId(id);
        return R.ok(merchantProductService.update(dto));
    }

    @PostMapping("/{id}/on-shelf")
    public R<Void> onShelf(@PathVariable Long id) {
        merchantProductService.onShelf(id);
        return R.ok();
    }

    @PostMapping("/{id}/off-shelf")
    public R<Void> offShelf(@PathVariable Long id) {
        merchantProductService.offShelf(id);
        return R.ok();
    }
}
