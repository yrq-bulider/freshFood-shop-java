package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.service.ReviewService;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户端-评价", description = "订单完成后对商品的评价与查看")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/orders/{orderId}/reviewable-items")
    @Operation(summary = "订单内可评价的商品", description = "返回订单中尚未评价的明细项")
    public R<List<OrderItemVO>> reviewable(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        return R.ok(reviewService.listReviewableItems(orderId));
    }

    @PostMapping
    @Operation(summary = "发表评价")
    public R<Long> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return R.ok(reviewService.create(dto));
    }

    @PostMapping("/{id}/append")
    @Operation(summary = "追评", description = "请求体 { content: \"...\", images: [\"url1\", ...] }")
    public R<Void> append(@Parameter(description = "评价 ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) body.get("images");
        reviewService.append(id, (String) body.get("content"), images);
        return R.ok();
    }

    @SaIgnore
    @GetMapping("/{id}")
    @Operation(summary = "评价详情")
    public R<ReviewVO> detail(@Parameter(description = "评价 ID") @PathVariable Long id) {
        return R.ok(reviewService.detail(id));
    }
}