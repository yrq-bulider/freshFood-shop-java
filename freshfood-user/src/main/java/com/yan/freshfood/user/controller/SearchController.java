package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.SearchService;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "用户端-搜索", description = "商品搜索")
@SaIgnore
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/products")
    @Operation(summary = "搜索商品")
    public R<PageR<ProductSimpleVO>> searchProducts(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "最低价") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "排序方式：sales_desc / sales_asc / price_asc / price_desc / new") @RequestParam(required = false, defaultValue = "sales_desc") String sort,
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") int pageNum,
            @Parameter(description = "页大小") @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return R.ok(searchService.searchProducts(keyword, categoryId, minPrice, maxPrice,
                sort, pageNum, pageSize));
    }
}