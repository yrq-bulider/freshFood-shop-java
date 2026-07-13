package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.dto.CartUpdateDTO;
import com.yan.freshfood.user.service.CartService;
import com.yan.freshfood.user.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-购物车", description = "购物车商品增删改查")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "我的购物车", description = "返回购物车汇总与明细")
    public R<CartVO> list() {
        return R.ok(cartService.listMyCart());
    }

    @PostMapping
    @Operation(summary = "加入购物车")
    public R<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return R.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新数量")
    public R<Void> update(@Parameter(description = "购物车项 ID") @PathVariable Long id,
                          @Valid @RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除单项")
    public R<Void> delete(@Parameter(description = "购物车项 ID") @PathVariable Long id) {
        cartService.deleteOne(id);
        return R.ok();
    }
}