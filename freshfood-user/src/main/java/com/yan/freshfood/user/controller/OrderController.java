package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.dto.OrderPreviewDTO;
import com.yan.freshfood.user.service.OrderService;
import com.yan.freshfood.user.vo.LogisticsVO;
import com.yan.freshfood.user.vo.OrderPreviewVO;
import com.yan.freshfood.user.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户端-订单", description = "订单创建、支付、取消、确认收货、详情查询")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/preview")
    @Operation(summary = "下单预览", description = "传入购物车项 + 地址，返回金额/运费/明细预览")
    public R<OrderPreviewVO> preview(@Valid @RequestBody OrderPreviewDTO dto) {
        return R.ok(orderService.preview(dto));
    }

    @PostMapping
    @Operation(summary = "提交订单")
    public R<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.create(dto));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "支付订单", description = "请求体 { payMethod: \"MOCK\" }")
    public R<Void> pay(@Parameter(description = "订单 ID") @PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.pay(id, body == null ? "MOCK" : body.getOrDefault("payMethod", "MOCK"));
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "仅待付款订单可取消")
    public R<Void> cancel(@Parameter(description = "订单 ID") @PathVariable Long id) {
        orderService.cancel(id);
        return R.ok();
    }

    @GetMapping
    @Operation(summary = "我的订单分页")
    public R<PageR<OrderVO>> list(@Parameter(description = "订单状态，可选") @RequestParam(required = false) Integer status,
                                  @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
                                  @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(orderService.list(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public R<OrderVO> detail(@Parameter(description = "订单 ID") @PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    @GetMapping("/{id}/logistics")
    @Operation(summary = "物流轨迹")
    public R<LogisticsVO> logistics(@Parameter(description = "订单 ID") @PathVariable Long id) {
        return R.ok(orderService.logistics(id));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认收货")
    public R<Void> confirm(@Parameter(description = "订单 ID") @PathVariable Long id) {
        orderService.confirmReceive(id);
        return R.ok();
    }

    @PostMapping("/{id}/rebuy")
    @Operation(summary = "再次购买", description = "将订单内的商品加回购物车")
    public R<Void> rebuy(@Parameter(description = "订单 ID") @PathVariable Long id) {
        orderService.rebuy(id);
        return R.ok();
    }
}