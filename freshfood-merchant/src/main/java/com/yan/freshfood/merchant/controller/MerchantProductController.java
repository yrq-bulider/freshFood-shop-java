package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.ProductCreateDTO;
import com.yan.freshfood.merchant.dto.ProductUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantProductService;
import com.yan.freshfood.merchant.vo.MerchantProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(summary = "商品分页")
    public R<PageR<MerchantProductVO>> page(
            @Parameter(description = "上下架状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantProductService.page(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public R<MerchantProductVO> detail(@Parameter(description = "商品 ID") @PathVariable Long id) {
        return R.ok(merchantProductService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新建商品", description = "新建后默认待审 + 下架")
    public R<MerchantProductVO> create(@Valid @RequestBody ProductCreateDTO dto) {
        return R.ok(merchantProductService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑商品", description = "审核/上下架/销量/评分/所属商家不会被本接口改动")
    public R<MerchantProductVO> update(@Parameter(description = "商品 ID") @PathVariable Long id,
                                       @Valid @RequestBody ProductUpdateDTO dto) {
        dto.setId(id);
        return R.ok(merchantProductService.update(dto));
    }

    @PostMapping("/{id}/on-shelf")
    @Operation(summary = "上架", description = "仅审核通过（auditStatus=1）的商品可上架")
    public R<Void> onShelf(@Parameter(description = "商品 ID") @PathVariable Long id) {
        merchantProductService.onShelf(id);
        return R.ok();
    }

    @PostMapping("/{id}/off-shelf")
    @Operation(summary = "下架")
    public R<Void> offShelf(@Parameter(description = "商品 ID") @PathVariable Long id) {
        merchantProductService.offShelf(id);
        return R.ok();
    }
}