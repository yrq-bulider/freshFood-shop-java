package com.yan.freshfood.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.yan.freshfood.common.constant.CommonConstants;
import com.yan.freshfood.common.response.PageR;
import com.yan.freshfood.common.response.R;
import com.yan.freshfood.merchant.dto.ShipDTO;
import com.yan.freshfood.merchant.service.MerchantOrderService;
import com.yan.freshfood.merchant.vo.MerchantOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商家端-订单", description = "商家查看订单、发货（仅 role=MERCHANT 账号可访问）")
@SaCheckRole(CommonConstants.ROLE_MERCHANT)
@RestController
@RequestMapping("/api/v1/merchant/orders")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    @GetMapping
    @Operation(summary = "订单分页")
    public R<PageR<MerchantOrderVO>> page(
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(merchantOrderService.page(status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public R<MerchantOrderVO> detail(@Parameter(description = "订单 ID") @PathVariable Long id) {
        return R.ok(merchantOrderService.detail(id));
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "发货", description = "仅待发货（status=2）订单可发货，自动转为待收货；body 必填 trackingNo + carrier")
    public R<Void> ship(@Parameter(description = "订单 ID") @PathVariable Long id,
                        @Valid @RequestBody ShipDTO dto) {
        merchantOrderService.ship(id, dto);
        return R.ok();
    }
}