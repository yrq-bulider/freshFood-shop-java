package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.ProductService;
import com.yan.freshfood.user.vo.ProductDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-商品", description = "商品详情")
@SaIgnore
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "商品详情（响应体含 reviews 数组，前 10 条评价）")
    public R<ProductDetailVO> detail(@Parameter(description = "商品 ID") @PathVariable Long id) {
        return R.ok(productService.getDetail(id));
    }
}