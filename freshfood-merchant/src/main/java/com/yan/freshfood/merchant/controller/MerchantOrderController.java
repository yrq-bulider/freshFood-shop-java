package com.yan.freshfood.merchant.controller;

import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.service.MerchantOrderService;
import com.yan.freshfood.merchant.vo.MerchantOrderVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商家端-订单", description = "商家查看订单、发货、确认")
@RestController
@RequestMapping("/api/v1/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    @GetMapping
    public R<PageR<MerchantOrderVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantOrderService.page(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public R<MerchantOrderVO> detail(@PathVariable Long id) {
        return R.ok(merchantOrderService.detail(id));
    }

    @PostMapping("/{id}/ship")
    public R<Void> ship(@PathVariable Long id) {
        merchantOrderService.ship(id);
        return R.ok();
    }
}
