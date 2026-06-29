package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.CartAddDTO;
import com.yan.freshfood.user.dto.CartUpdateDTO;
import com.yan.freshfood.user.vo.CartVO;

import java.util.List;

public interface CartService {
    CartVO listMyCart();
    void add(CartAddDTO dto);
    void updateQuantity(Long cartId, CartUpdateDTO dto);
    void deleteOne(Long cartId);
    void deleteBatch(List<Long> cartIds);
    void toggleSelect(Long cartId, Boolean selected);
    void toggleSelectAll(Boolean selected);
}