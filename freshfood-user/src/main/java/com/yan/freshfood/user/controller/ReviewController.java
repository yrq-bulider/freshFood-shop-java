package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-评价", description = "订单完成后对商品的评价")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "发表评价", description = "必填 orderId / productId / skuId / rating / content")
    public R<Long> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return R.ok(reviewService.create(dto));
    }
}