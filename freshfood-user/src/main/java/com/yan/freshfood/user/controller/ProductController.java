package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.ProductService;
import com.yan.freshfood.user.vo.ProductDetailVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaIgnore
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public R<ProductDetailVO> detail(@PathVariable Long id) {
        return R.ok(productService.getDetail(id));
    }

    @GetMapping("/{id}/reviews")
    public R<PageR<ReviewVO>> reviews(@PathVariable Long id,
                                      @RequestParam(defaultValue = "1") int pageNum,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(productService.listReviews(id, pageNum, pageSize));
    }

    @GetMapping("/{id}/recommendations")
    public R<List<ProductSimpleVO>> recommendations(@PathVariable Long id) {
        return R.ok(productService.listRecommendations(id));
    }
}