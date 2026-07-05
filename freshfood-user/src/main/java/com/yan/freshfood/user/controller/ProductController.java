package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.ProductService;
import com.yan.freshfood.user.vo.ProductDetailVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户端-商品", description = "商品列表、详情、分类筛选")
@SaIgnore
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "商品详情")
    public R<ProductDetailVO> detail(@Parameter(description = "商品 ID") @PathVariable Long id) {
        return R.ok(productService.getDetail(id));
    }

    @GetMapping("/{id}/reviews")
    @Operation(summary = "商品评价分页")
    public R<PageR<ReviewVO>> reviews(@Parameter(description = "商品 ID") @PathVariable Long id,
                                      @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
                                      @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(productService.listReviews(id, pageNum, pageSize));
    }

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "看了又看 / 同类推荐")
    public R<List<ProductSimpleVO>> recommendations(@Parameter(description = "商品 ID") @PathVariable Long id) {
        return R.ok(productService.listRecommendations(id));
    }
}