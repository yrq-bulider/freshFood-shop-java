package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.HomeService;
import com.yan.freshfood.user.vo.BannerVO;
import com.yan.freshfood.user.vo.CategoryVO;
import com.yan.freshfood.user.vo.ProductSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SaIgnore
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/banners")
    public R<List<BannerVO>> banners() {
        return R.ok(homeService.listBanners());
    }

    @GetMapping("/categories")
    public R<List<CategoryVO>> categories() {
        return R.ok(homeService.listCategories());
    }

    @GetMapping("/recommendations")
    public R<List<ProductSimpleVO>> recommendations() {
        return R.ok(homeService.listRecommendations());
    }
}
