package com.yan.freshfood.user.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.ReviewCreateDTO;
import com.yan.freshfood.user.service.ReviewService;
import com.yan.freshfood.user.vo.OrderItemVO;
import com.yan.freshfood.user.vo.ReviewVO;
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
    public R<List<OrderItemVO>> reviewable(@PathVariable Long orderId) {
        return R.ok(reviewService.listReviewableItems(orderId));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return R.ok(reviewService.create(dto));
    }

    @PostMapping("/{id}/append")
    public R<Void> append(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) body.get("images");
        reviewService.append(id, (String) body.get("content"), images);
        return R.ok();
    }

    @SaIgnore
    @GetMapping("/{id}")
    public R<ReviewVO> detail(@PathVariable Long id) {
        return R.ok(reviewService.detail(id));
    }
}
