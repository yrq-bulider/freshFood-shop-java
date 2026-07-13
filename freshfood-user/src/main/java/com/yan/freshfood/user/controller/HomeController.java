package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.service.HomeService;
import com.yan.freshfood.user.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户端-首页", description = "分类树")
@SaIgnore
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/categories")
    @Operation(summary = "分类树")
    public R<List<CategoryVO>> categories() {
        return R.ok(homeService.listCategories());
    }
}