package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.OrderCreateDTO;
import com.yan.freshfood.user.dto.OrderPreviewDTO;
import com.yan.freshfood.user.service.OrderService;
import com.yan.freshfood.user.vo.LogisticsVO;
import com.yan.freshfood.user.vo.OrderPreviewVO;
import com.yan.freshfood.user.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/preview")
    public R<OrderPreviewVO> preview(@Valid @RequestBody OrderPreviewDTO dto) {
        return R.ok(orderService.preview(dto));
    }

    @PostMapping
    public R<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return R.ok(orderService.create(dto));
    }

    @PostMapping("/{id}/pay")
    public R<Void> pay(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.pay(id, body == null ? "MOCK" : body.getOrDefault("payMethod", "MOCK"));
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return R.ok();
    }

    @GetMapping
    public R<PageR<OrderVO>> list(@RequestParam(required = false) Integer status,
                                  @RequestParam(defaultValue = "1") int pageNum,
                                  @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(orderService.list(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    @GetMapping("/{id}/logistics")
    public R<LogisticsVO> logistics(@PathVariable Long id) {
        return R.ok(orderService.logistics(id));
    }

    @PostMapping("/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        orderService.confirmReceive(id);
        return R.ok();
    }

    @PostMapping("/{id}/rebuy")
    public R<Void> rebuy(@PathVariable Long id) {
        orderService.rebuy(id);
        return R.ok();
    }
}