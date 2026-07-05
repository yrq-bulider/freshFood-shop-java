package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.HomeService;
import com.yan.freshfood.user.vo.BannerVO;
import com.yan.freshfood.user.vo.CategoryVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户端-首页", description = "首页轮播图、分类树、热门搜索词")
@SaIgnore
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/banners")
    @Operation(summary = "首页轮播图")
    public R<List<BannerVO>> banners() {
        return R.ok(homeService.listBanners());
    }

    @GetMapping("/categories")
    @Operation(summary = "分类树")
    public R<List<CategoryVO>> categories() {
        return R.ok(homeService.listCategories());
    }

    @GetMapping("/recommendations")
    @Operation(summary = "首页推荐商品")
    public R<List<ProductSimpleVO>> recommendations() {
        return R.ok(homeService.listRecommendations());
    }
}