package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.SearchService;
import com.yan.freshfood.user.vo.HotWordVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import com.yan.freshfood.user.vo.SearchHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @SaIgnore
    @GetMapping("/hot-words")
    public R<List<HotWordVO>> hotWords() {
        return R.ok(searchService.listHotWords());
    }

    @SaIgnore
    @GetMapping("/products")
    public R<PageR<ProductSimpleVO>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "sales_desc") String sort,
            @RequestParam(required = false, defaultValue = "1") int pageNum,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {
        return R.ok(searchService.searchProducts(keyword, categoryId, minPrice, maxPrice,
                sort, pageNum, pageSize));
    }

    @GetMapping("/history")
    public R<List<SearchHistoryVO>> history() {
        return R.ok(searchService.listMyHistory());
    }

    @DeleteMapping("/history")
    public R<Void> clearHistory() {
        searchService.clearMyHistory();
        return R.ok();
    }

    @DeleteMapping("/history/{id}")
    public R<Void> deleteHistory(@PathVariable Long id) {
        searchService.deleteHistory(id);
        return R.ok();
    }
}