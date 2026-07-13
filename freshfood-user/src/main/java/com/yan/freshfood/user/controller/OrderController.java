package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.service.OrderService;
import com.yan.freshfood.user.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户端-订单", description = "订单创建、支付、确认收货、详情查询")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "提交订单", description = "body 必填 receiverName / receiverPhone / receiverAddress + 购物车项")
    public R<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.create(dto));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "支付订单", description = "请求体 { payMethod: \"MOCK\" }")
    public R<Void> pay(@Parameter(description = "订单 ID") @PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.pay(id, body == null ? "MOCK" : body.getOrDefault("payMethod", "MOCK"));
        return R.ok();
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认收货")
    public R<Void> confirm(@Parameter(description = "订单 ID") @PathVariable Long id) {
        orderService.confirmReceive(id);
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
}