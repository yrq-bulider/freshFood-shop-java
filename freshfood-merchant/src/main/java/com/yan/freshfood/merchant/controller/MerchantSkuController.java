package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.SkuCreateDTO;
import com.yan.freshfood.merchant.dto.SkuUpdateDTO;
import com.yan.freshfood.merchant.service.MerchantSkuService;
import com.yan.freshfood.merchant.vo.SkuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "商家端-SKU", description = "商品 SKU 规格与库存管理")
@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
public class MerchantSkuController {

    private final MerchantSkuService merchantSkuService;

    @GetMapping("/products/{productId}/skus")
    @Operation(summary = "查询商品下的 SKU 列表")
    public R<List<SkuVO>> list(@Parameter(description = "商品 ID") @PathVariable Long productId) {
        return R.ok(merchantSkuService.list(productId));
    }

    @PostMapping("/products/{productId}/skus")
    @Operation(summary = "新增 SKU")
    public R<SkuVO> create(@Parameter(description = "商品 ID") @PathVariable Long productId,
                           @Valid @RequestBody SkuCreateDTO dto) {
        return R.ok(merchantSkuService.create(productId, dto));
    }

    @PutMapping("/skus/{id}")
    @Operation(summary = "更新 SKU", description = "至少传 1 个非空字段")
    public R<SkuVO> update(@Parameter(description = "SKU ID") @PathVariable Long id,
                           @Valid @RequestBody SkuUpdateDTO dto) {
        return R.ok(merchantSkuService.update(id, dto));
    }

    @DeleteMapping("/skus/{id}")
    @Operation(summary = "删除 SKU", description = "已售 SKU 不能删除")
    public R<Void> delete(@Parameter(description = "SKU ID") @PathVariable Long id) {
        merchantSkuService.delete(id);
        return R.ok();
    }
}