package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.SearchService;
import com.yan.freshfood.user.vo.HotWordVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.SearchHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "用户端-搜索", description = "商品搜索与搜索历史")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @SaIgnore
    @GetMapping("/hot-words")
    @Operation(summary = "热门搜索词")
    public R<List<HotWordVO>> hotWords() {
        return R.ok(searchService.listHotWords());
    }

    @SaIgnore
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

    @GetMapping("/history")
    @Operation(summary = "我的搜索历史")
    public R<List<SearchHistoryVO>> history() {
        return R.ok(searchService.listMyHistory());
    }

    @DeleteMapping("/history")
    @Operation(summary = "清空搜索历史")
    public R<Void> clearHistory() {
        searchService.clearMyHistory();
        return R.ok();
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除单条搜索历史")
    public R<Void> deleteHistory(@Parameter(description = "历史 ID") @PathVariable Long id) {
        searchService.deleteHistory(id);
        return R.ok();
    }
}