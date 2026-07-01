package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.dto.CartUpdateDTO;
import com.yan.freshfood.user.service.CartService;
import com.yan.freshfood.user.vo.CartVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户端-购物车", description = "购物车商品增删改查、选中切换")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public R<CartVO> list() {
        return R.ok(cartService.listMyCart());
    }

    @PostMapping
    public R<Void> add(@Valid @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cartService.deleteOne(id);
        return R.ok();
    }

    @DeleteMapping
    public R<Void> deleteBatch(@RequestBody List<Long> ids) {
        cartService.deleteBatch(ids);
        return R.ok();
    }

    @PutMapping("/select")
    public R<Void> select(@RequestParam Long id, @RequestParam Boolean selected) {
        cartService.toggleSelect(id, selected);
        return R.ok();
    }

    @PutMapping("/select-all")
    public R<Void> selectAll(@RequestParam Boolean selected) {
        cartService.toggleSelectAll(selected);
        return R.ok();
    }
}